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
package com.artipie.maven.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.maven.ValidUpload;
import com.artipie.maven.metadata.ArtifactsMetadata;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

/**
 * Asto {@link ValidUpload} implementation validates upload from abstract storage. Validation
 * process includes:
 * - check maven-metadata.xml: group and artifact ids from uploaded xml are the same as in
 *  existing xml, checksums (if there are any) are valid
 * - artifacts checksums are correct
 * @since 0.5
 * @checkstyle MagicNumberCheck (500 lines)
 */
public final class AstoValidUpload implements ValidUpload {

    /**
     * All supported Maven artifacts according to
     * <a href="https://maven.apache.org/ref/3.6.3/maven-core/artifact-handlers.html">Artifact
     * handlers</a> by maven-core, and additionally {@code xml} metadata files are
     * also artifacts.
     */
    private static final Pattern PTN_ARTIFACT = Pattern.compile(".+\\.(?:pom|jar|war|ear|rar|aar)");

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    public AstoValidUpload(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key upload, final Key artifact) {
        return this.validateMetadata(upload, artifact)
            .thenCompose(
                valid -> {
                    CompletionStage<Boolean> res = CompletableFuture.completedStage(valid);
                    if (valid) {
                        res = this.validateChecksums(upload);
                    }
                    return res;
                }
            );
    }

    /**
     * Validates uploaded and existing metadata by comparing group and artifact ids.
     * @param upload Uploaded artifacts location
     * @param artifact Artifact location
     * @return Completable validation action: true if group and artifact ids are equal,
     *  false otherwise.
     */
    private CompletionStage<Boolean> validateMetadata(final Key upload, final Key artifact) {
        final ArtifactsMetadata metadata = new ArtifactsMetadata(this.storage);
        final String meta = "maven-metadata.xml";
        return this.storage.exists(new Key.From(artifact, meta))
            .thenCompose(
                exists -> {
                    final CompletionStage<Boolean> res;
                    if (exists) {
                        res = metadata.groupAndArtifact(upload).thenCompose(
                            existing -> metadata.groupAndArtifact(artifact).thenApply(
                                uploaded -> uploaded.equals(existing)
                            )
                        );
                    } else {
                        res = CompletableFuture.completedStage(true);
                    }
                    return res;
                }
            ).thenCompose(
                same -> {
                    final CompletionStage<Boolean> res;
                    if (same) {
                        res = this.validateArtifactChecksums(
                            new Key.From(upload, meta)
                        ).to(SingleInterop.get());
                    } else {
                        res = CompletableFuture.completedStage(false);
                    }
                    return res;
                }
            );
    }

    /**
     * Validate artifact checksums.
     * @param upload Artifact location
     * @return Completable validation action: true if checksums are correct, false otherwise
     */
    private CompletionStage<Boolean> validateChecksums(final Key upload) {
        final RxStorage rxsto = new RxStorageWrapper(this.storage);
        return new ArtifactsMetadata(this.storage).maxVersion(upload).thenCompose(
            version -> {
                final Key pckg = new Key.From(upload, version);
                return rxsto.list(pckg)
                    .flatMapObservable(Observable::fromIterable)
                    .filter(key -> PTN_ARTIFACT.matcher(key.string()).matches())
                    .flatMapSingle(
                        this::validateArtifactChecksums
                    ).reduce(
                        new ArrayList<>(5),
                        (list, res) -> {
                            list.add(res);
                            return list;
                        }
                    ).map(
                        array -> !array.isEmpty() && !array.contains(false)
                    ).to(SingleInterop.get());
            }
        );
    }

    /**
     * Validates artifact checksums.
     * @param artifact Artifact key
     * @return Validation result: false if at least one checksum is invalid, true if all are valid
     *  or if no checksums exists.
     */
    private Single<Boolean> validateArtifactChecksums(final Key artifact) {
        return SingleInterop.fromFuture(
            new RepositoryChecksums(this.storage).checksums(artifact)
        ).map(Map::entrySet)
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                entry ->
                    SingleInterop.fromFuture(
                        this.storage.value(artifact).thenCompose(
                            content -> new ContentDigest(
                                content,
                                Digests.valueOf(
                                    entry.getKey().toUpperCase(Locale.US)
                                )
                            ).hex().thenApply(
                                hex -> hex.equals(entry.getValue())
                            )
                    )
                )
            ).all(equal -> equal);
    }
}
