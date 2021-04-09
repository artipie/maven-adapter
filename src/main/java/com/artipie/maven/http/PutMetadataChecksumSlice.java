/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.maven.Maven;
import com.artipie.maven.ValidUpload;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.reactivestreams.Publisher;

/**
 * This slice accepts PUT requests with maven-metadata.xml checksums, picks up corresponding
 * maven-metadata.xml from the package upload temp location and saves the checksum. If upload
 * is ready to be added in the repository (see {@link ValidUpload#ready(Key)}), this slice initiate
 * repository update.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class PutMetadataChecksumSlice implements Slice {

    /**
     * Metadata pattern.
     */
    static final Pattern PTN =
        Pattern.compile("^/(?<pkg>.+)/maven-metadata.xml.(?<alg>md5|sha1|sha256|sha512)");

    /**
     * Response with status BAD_REQUEST.
     */
    private static final Response BAD_REQUEST = new RsWithStatus(RsStatus.BAD_REQUEST);

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Upload validation.
     */
    private final ValidUpload valid;

    /**
     * Maven repository.
     */
    private final Maven mvn;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param valid Upload validation
     * @param mvn Maven repository
     */
    public PutMetadataChecksumSlice(final Storage asto, final ValidUpload valid, final Maven mvn) {
        this.asto = asto;
        this.valid = valid;
        this.mvn = mvn;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PutMetadataChecksumSlice.PTN
            .matcher(new RequestLineFrom(line).uri().getPath());
        final Response res;
        if (matcher.matches()) {
            final String alg = matcher.group("alg");
            final String pkg = matcher.group("pkg");
            res = new AsyncResponse(
                this.findAndSave(body, alg, pkg).thenCompose(
                    key -> {
                        final CompletionStage<Response> resp;
                        if (key.isPresent() && key.get().parent().isPresent()
                            && key.get().parent().get().parent().isPresent()) {
                            final Key location = key.get().parent().get().parent().get();
                            // @checkstyle NestedIfDepthCheck (10 lines)
                            resp = this.valid.ready(location).thenCompose(
                                ready -> {
                                    final CompletionStage<Response> action;
                                    if (ready) {
                                        action = this.validateAndUpdate(pkg, location);
                                    } else {
                                        action = CompletableFuture.completedFuture(
                                            new RsWithStatus(RsStatus.CREATED)
                                        );
                                    }
                                    return action;
                                }
                            );
                        } else {
                            resp = CompletableFuture.completedFuture(
                                PutMetadataChecksumSlice.BAD_REQUEST
                            );
                        }
                        return resp;
                    }
                )
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }

    /**
     * Validates and, if valid, starts update process.
     * @param pkg Package
     * @param location Temp upload location
     * @return Response: BAD_REQUEST if not valid, CREATED otherwise
     */
    private CompletionStage<Response> validateAndUpdate(final String pkg, final Key location) {
        return this.valid.validate(location, new Key.From(pkg)).thenCompose(
            correct -> {
                final CompletionStage<Response> upd;
                if (correct) {
                    upd = this.mvn.update(location, new Key.From(pkg))
                        .thenApply(ignored -> new RsWithStatus(RsStatus.CREATED));
                } else {
                    upd = CompletableFuture.completedFuture(PutMetadataChecksumSlice.BAD_REQUEST);
                }
                return upd;
            }
        );
    }

    /**
     * Searcher for the suitable maven-metadata.xml and saves checksum to the correct location,
     * returns suitable maven-metadata.xml key.
     * @param body Request body
     * @param alg Algorithm
     * @param pkg Package name
     * @return Completion action
     */
    private CompletionStage<Optional<Key>> findAndSave(final Publisher<ByteBuffer> body,
        final String alg, final String pkg) {
        return new PublisherAs(body).asciiString().thenCompose(
            sum -> new RxStorageWrapper(this.asto).list(
                new Key.From(UploadSlice.TEMP, pkg)
            ).flatMapObservable(Observable::fromIterable)
                .filter(item -> item.string().endsWith("maven-metadata.xml"))
                .flatMapSingle(
                    item -> Single.fromFuture(
                        this.asto.value(item).thenCompose(
                            pub -> new ContentDigest(
                                pub, Digests.valueOf(alg.toUpperCase(Locale.US))
                            ).hex()
                        ).thenApply(hex -> new ImmutablePair<>(item, hex))
                    )
                ).filter(pair -> pair.getValue().equals(sum))
                .singleOrError()
                .flatMap(
                    pair -> SingleInterop.fromFuture(
                        this.asto.save(
                            new Key.From(
                                String.format(
                                    "%s.%s", pair.getKey().string(), alg
                                )
                            ),
                            new Content.From(
                                sum.getBytes(StandardCharsets.US_ASCII)
                            )
                        ).thenApply(
                            nothing -> Optional.of(pair.getKey())
                        )
                    )
                )
                .onErrorReturn(ignored -> Optional.empty())
                .to(SingleInterop.get())
        );
    }
}
