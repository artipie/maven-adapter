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

package com.artipie.maven.aether;

import com.artipie.maven.ArtifactMetadata;
import com.artipie.maven.ChecksumAttribute;
import com.artipie.maven.ChecksumType;
import com.artipie.maven.DetachedMetadata;
import com.artipie.maven.FileCoordinates;
import com.artipie.maven.Repository;
import com.artipie.maven.util.AutoCloseablePath;
import com.artipie.maven.util.FileCleanupException;
import com.google.common.collect.Iterables;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven library implementation for {@link Repository}.
 * @since 0.1
 * @todo #10:30min AetherRepository unit test.
 *  The implementation of the class is ongoing work.
 *  We should resolve other TODOs before coding AetherRepository unit test
 *  as AetherRepository is top-level class.
 */
public final class AetherRepository implements Repository {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AetherRepository.class);

    /**
     * Create request-level {@link ServiceLocator} instances.
     */
    private final ServiceLocatorFactory locators;

    /**
     * Staging files root.
     */
    private final AutoCloseablePath.Parent dir;

    /**
     * All args constructor.
     * @param locators Creates ServiceLocator instances
     * @param dir Staging files root
     */
    public AetherRepository(
        final ServiceLocatorFactory locators,
        final AutoCloseablePath.Parent dir
    ) {
        this.locators = locators;
        this.dir = dir;
    }

    @Override
    public ArtifactMetadata upload(final String path, final InputStream content) throws Exception {
        final var coords = new FileCoordinates(path);
        final var locator = this.locators.serviceLocator();
        final var session = new SessionFactory(locator).newSession();
        final Artifact deployed = Iterables.getOnlyElement(
            new Deployer(locator, session)
                .deploy(path, content)
                .getArtifacts()
        );
        final Path file = new LocalArtifactResolver(session)
            .resolve(deployed);
        final Map<ChecksumType, String> checksums = new ChecksumAttribute(file)
            .write();
        return new DetachedMetadata(
            coords,
            path,
            Files.size(file),
            checksums.get(ChecksumType.MD5),
            checksums.get(ChecksumType.SHA1)
        );
    }

    /**
     * Performs deployment lifecycle in terms of Maven libraries.
     * @since 0.1
     */
    private final class Deployer {
        /**
         * ServiceLocator instance.
         */
        private final ServiceLocator locator;

        /**
         * Ongoing session.
         */
        private final RepositorySystemSession session;

        /**
         * All args constructor.
         * @param locator ServiceLocator instance
         * @param session Ongoing session
         */
        private Deployer(final ServiceLocator locator, final RepositorySystemSession session) {
            this.locator = locator;
            this.session = session;
        }

        /**
         * Transfers given content to staging file and deploys it.
         * @param path Artifact URI path segment
         * @param content Artifact binary
         * @return DeployResult
         * @throws Exception Deployment failed
         */
        private DeployResult deploy(final String path, final InputStream content) throws Exception {
            final var repositories = this.locator.getService(RepositorySystem.class);
            if (repositories == null) {
                throw new IllegalStateException("RepositorySystem cannot be null");
            }
            DeployResult result = null;
            try (var staging = AetherRepository.this.dir.resolve(path)) {
                try (var file = Files.newOutputStream(staging.unwrap())) {
                    content.transferTo(file);
                }
                result = repositories.deploy(
                    this.session,
                    new DeployRequest()
                        .addArtifact(
                            new DefaultArtifact(
                                new FileCoordinates(path).coords()
                            ).setFile(
                                staging.unwrap().toFile()
                            )
                        )
                );
            } catch (final FileCleanupException ex) {
                LOG.warn("on AutoCloseablePath cleanup", ex);
            }
            return result;
        }
    }
}
