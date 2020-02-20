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

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.maven.aether.ServiceLocatorFactory;
import com.artipie.maven.aether.SessionFactory;
import com.artipie.maven.aether.SimpleRemoteRepositories;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link Resolver}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (100 lines)
 */
public final class ResolverTest {

    /**
     * Random array size.
     */
    private static final int ARRAY_SIZE = 8192 * 10;

    /**
     * Test temporary directory.
     * By JUnit annotation contract it should not be private
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path temp;

    /**
     * Asto.
     */
    private BlockingStorage asto;

    /**
     * Class under test.
     */
    private Resolver resolver;

    @BeforeEach
    public void before() {
        final FileStorage files = new FileStorage(this.temp.resolve("asto"));
        this.asto = new BlockingStorage(files);
        final var locator = new ServiceLocatorFactory(files).serviceLocator();
        this.resolver = new Resolver(
            new SimpleRemoteRepositories(),
            locator.getService(RepositorySystem.class),
            new SessionFactory(
                new LocalRepository(this.temp.resolve("local").toFile()),
                locator
            ).newSession()
        );
    }

    @Test
    public void shouldResolveArtifact() {
        final var bytes = this.randomBytes();
        final String path = "org/example/artifact/1.0/artifact-1.0.jar";
        this.asto.save(new Key.From(path), bytes);
        MatcherAssert.assertThat(
            "should resolve artifact",
            Hex.encodeHexString(this.from(this.resolver.resolve(path))),
            new IsEqual<>(Hex.encodeHexString(bytes))
        );
    }

    @Test
    public void shouldResolveMetadata() {
        final var bytes = this.randomBytes();
        final String path = "org/example/artifact/maven-metadata.xml";
        this.asto.save(new Key.From(path), bytes);
        MatcherAssert.assertThat(
            "should resolve maven-metadata.xml",
            Hex.encodeHexString(this.from(this.resolver.resolve(path))),
            new IsEqual<>(Hex.encodeHexString(bytes))
        );
    }

    private byte[] randomBytes() {
        final byte[] bytes = new byte[ResolverTest.ARRAY_SIZE];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    private byte[] from(final Flowable<ByteBuffer> input) {
        final var baos = new ByteArrayOutputStream();
        input.blockingIterable().forEach(
            bb -> {
                try {
                    final var bytes = new byte[bb.remaining()];
                    bb.get(bytes);
                    baos.write(bytes);
                } catch (final IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        );
        return baos.toByteArray();
    }
}
