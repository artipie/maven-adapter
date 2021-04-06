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
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.reactivestreams.Publisher;

/**
 * This slice accepts PUT requests with maven-metadata.xml checksums, picks up corresponding
 * maven-metadata.xml from the package upload temp location and saves the checksum. If at least
 * sha1 and md5 checksums are present in the package upload location, this slice initiate repository
 * update.
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
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public PutMetadataChecksumSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PutMetadataChecksumSlice.PTN
            .matcher(new RequestLineFrom(line).uri().getPath());
        final Response res;
        if (matcher.matches()) {
            res = new AsyncResponse(
                new PublisherAs(body).asciiString().thenCompose(
                    sum -> {
                        final String alg = matcher.group("alg");
                        final String pkg = matcher.group("pkg");
                        return new RxStorageWrapper(this.asto).list(
                            new Key.From(UpdateMavenSlice.TEMP, pkg)
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
                            .toList()
                            .flatMap(
                                list -> {
                                    final Single<Response> comp;
                                    if (list.isEmpty()) {
                                        comp = Single.just(new RsWithStatus(RsStatus.BAD_REQUEST));
                                    } else {
                                        comp = SingleInterop.fromFuture(
                                            this.asto.save(
                                                new Key.From(
                                                    String.format(
                                                        "%s.%s", list.get(0).getKey().string(), alg
                                                    )
                                                ),
                                                new Content.From(
                                                    sum.getBytes(StandardCharsets.US_ASCII)
                                                )
                                            ).thenApply(
                                                nothing -> new RsWithStatus(RsStatus.CREATED)
                                            )
                                        );
                                    }
                                    return comp;
                                }
                            ).to(SingleInterop.get());
                    }
                )
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }
}
