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

package com.artipie.maven.aether.repository;

import com.artipie.asto.fs.RxFile;
import com.artipie.maven.ArtifactCoordinates;
import com.artipie.maven.FileCoordinates;
import com.artipie.maven.aether.RemoteRepositories;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

/**
 * Performs artifact resolution.
 * @since 0.1
 */
final class Resolver {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Resolver.class);

    /**
     * Remote repositories to handle artifacts.
     */
    private final RemoteRepositories remotes;

    /**
     * RepositorySystem instance.
     */
    private final RepositorySystem repositories;

    /**
     * Ongoing session.
     */
    private final RepositorySystemSession session;

    /**
     * All args constructor.
     * @param remotes Remote repositories
     * @param repositories RepositorySystem instance
     * @param session Ongoing session
     */
    Resolver(
        final RemoteRepositories remotes,
        final RepositorySystem repositories,
        final RepositorySystemSession session
    ) {
        this.remotes = remotes;
        this.repositories = repositories;
        this.session = session;
    }

    /**
     * Resolves artifact file.
     * @param path Artifact path
     * @return Artifact file
     */
    public Flowable<ByteBuffer> resolve(final String path) {
        final var span = MarkerFactory.getMarker(UUID.randomUUID().toString());
        LOG.info(span, "resolving '{}'", path);
        return Single.fromCallable(
            () -> {
                final var coords = new ArtifactCoordinates.Parser(path).parse();
                LOG.debug(
                    span,
                    "ArtifactCoordinates '{}' ({})",
                    path,
                    coords.getClass().getSimpleName()
                );
                final Function<ArtifactCoordinates, File> handler;
                if (coords instanceof FileCoordinates) {
                    handler = this::artifact;
                } else {
                    handler = this::metadata;
                }
                final File file = handler.apply(coords);
                LOG.debug(span, "resolved '{}' file '{}'", path, file);
                return file;
            }
        ).map(File::toPath)
            .map(RxFile::new)
            .flatMapPublisher(RxFile::flow);
    }

    /**
     * Resolves an artifact.
     * @param coords Artifact coordinates
     * @return Artifact local file
     * @throws RepositoryException Failed to resolve the artifact
     */
    private File artifact(final ArtifactCoordinates coords) throws RepositoryException {
        return this.repositories.resolveArtifacts(
            this.session,
            List.of(
                new ArtifactRequest(
                    new DefaultArtifact(((FileCoordinates) coords).coords()),
                    this.remotes.downloading(coords),
                    null
                )
            )
        ).stream()
            .map(ArtifactResult::getArtifact)
            .findFirst()
            .map(Artifact::getFile)
            .orElseThrow(
                () -> new RepositoryException(this.buildMessage(coords))
            );
    }

    /**
     * Resolves a metadata.
     * @param coords Metadata coordinates
     * @return Metadata local file
     * @throws RepositoryException Failed to resolve the metadata
     */
    private File metadata(final ArtifactCoordinates coords) throws RepositoryException {
        final var meta = this.repositories.resolveMetadata(
            this.session,
            List.of(
                new MetadataRequest()
                    .setMetadata(
                        new DefaultMetadata(
                            coords.groupId(),
                            coords.artifactId(),
                            coords.name(),
                            Metadata.Nature.RELEASE_OR_SNAPSHOT
                        )
                    ).setRepository(this.remotes.downloadingPrimary(coords))
            )
        ).stream()
            .findFirst()
            .orElseThrow(
                () -> new RepositoryException(this.buildMessage(coords))
            );
        if (meta.getException() != null) {
            throw new RepositoryException(this.buildMessage(coords), meta.getException());
        }
        return meta.getMetadata().getFile();
    }

    /**
     * Builds exception message for an ArtifactCoordinates.
     * @param coords Failed ArtifactCoordinates
     * @return Exception message
     * @checkstyle NonStaticMethodCheck (2 lines)
     */
    private String buildMessage(final ArtifactCoordinates coords) {
        return String.format("Failed to resolve %s", coords.path());
    }
}
