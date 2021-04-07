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

import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.maven.Maven;
import com.artipie.maven.metadata.ArtifactsMetadata;
import com.artipie.maven.metadata.MavenMetadata;
import com.jcabi.xml.XMLDocument;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.xembly.Directives;

/**
 * Maven front for artipie maven adaptor.
 *
 * @since 0.2
 */
public final class AstoMavenWithMove implements Maven {

    /**
     * Maven metadata xml name.
     */
    private static final String MAVEN_META = "maven-metadata.xml";

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     * @param storage Storage used by this class.
     */
    public AstoMavenWithMove(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Void> update(final Key upload, final Key artifact) {
        return this.storage.exclusively(
            artifact,
            target -> this.storage.value(new Key.From(upload, AstoMavenWithMove.MAVEN_META))
                .thenCompose(
                    pub -> new PublisherAs(pub).asciiString()
                )
                .thenApply(XMLDocument::new)
                .thenApply(doc -> new MavenMetadata(Directives.copyOf(doc.node())))
                .thenCompose(
                    doc -> target.list(artifact).thenApply(
                        items -> items.stream()
                            .map(
                                item -> item.string()
                                    .replaceAll(String.format("%s/", artifact.string()), "")
                                    .split("/")[0]
                            )
                            .filter(item -> !item.startsWith("maven-metadata"))
                            .collect(Collectors.toSet())
                    ).thenCompose(
                        versions -> new ArtifactsMetadata(this.storage).maxVersion(upload)
                            .thenApply(
                                latest -> {
                                    versions.add(latest);
                                    return doc.versions(versions);
                                }
                        )
                    )
                ).thenCompose(doc -> doc.save(this.storage, upload))
                .thenCompose(meta -> new RepositoryChecksums(this.storage).generate(meta))
                .thenCompose(nothing -> this.moveToTheRepository(upload, target, artifact))
                .thenCompose(nothing -> this.storage.list(upload).thenCompose(this::remove))
            );
    }

    /**
     * Moves artifacts from temp location to repository.
     * @param upload Upload temp location
     * @param target Repository
     * @param artifact Artifact repository location
     * @return Completion action
     */
    private CompletableFuture<Void> moveToTheRepository(
        final Key upload, final Storage target, final Key artifact
    ) {
        final Storage sub = new SubStorage(upload, this.storage);
        final Storage subversion = new SubStorage(upload.parent().get(), this.storage);
        return sub.list(Key.ROOT).thenCompose(
            list -> new Copy(
                sub,
                list.stream().filter(key -> key.string().contains(AstoMavenWithMove.MAVEN_META))
                    .collect(Collectors.toList())
            ).copy(new SubStorage(artifact, target))
        ).thenCompose(
            nothing -> subversion.list(Key.ROOT).thenCompose(
                list -> new Copy(
                    subversion,
                    list.stream()
                        .filter(key -> !key.string().contains(AstoMavenWithMove.MAVEN_META))
                        .collect(Collectors.toList())
                ).copy(new SubStorage(artifact, target))
            )
        );
    }

    /**
     * Delete items from storage.
     * @param items Keys to remove
     * @return Completable remove operation
     */
    private CompletableFuture<Void> remove(final Collection<Key> items) {
        return CompletableFuture.allOf(
            items.stream().map(this.storage::delete)
                .toArray(CompletableFuture[]::new)
        );
    }
}
