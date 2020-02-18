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

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link AstoTransporter}.
 * @since 0.1
 */
@Disabled("debug")
public final class AstoTransporterTest {

    /**
     * Test temporary directory.
     * By JUnit annotation contract it should not be private
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path temp;

    /**
     * Test class instance.
     */
    private AstoTransporter transporter;

    @BeforeEach
    public void before() {
        this.transporter = new AstoTransporter(
            new BlockingStorage(new FileStorage(this.temp))
        );
    }

    @Test
    public void implPeekShouldThrow() throws Exception {
        Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> this.transporter.implPeek(
                new PeekTask(
                    URI.create(this.randomString())
                )
            )
        );
    }

    @Test
    public void implPeekShouldNotThrow() throws Exception {
        final var name = this.randomString();
        Files.write(this.temp.resolve(name), new byte[0]);
        Assertions.assertDoesNotThrow(
            () -> this.transporter.implPeek(new PeekTask(URI.create(name)))
        );
    }

    @Test
    public void implGetShouldRead() throws Exception {
        final var name = this.randomString();
        final var content = this.randomString();
        Files.write(this.temp.resolve(name), List.of(content));
        final var task = new GetTask(URI.create(name));
        this.transporter.implGet(task);
        MatcherAssert.assertThat(
            task.getDataString().trim(),
            CoreMatchers.is(content)
        );
    }

    @Test
    public void implPutShouldWrite() throws Exception {
        final var name = this.randomString();
        final var content = this.randomString();
        this.transporter.implPut(
            new PutTask(URI.create(name))
                .setDataString(content)
        );
        MatcherAssert.assertThat(
            Files.readString(this.temp.resolve(name)),
            CoreMatchers.is(content)
        );
    }

    /**
     * Generates random string.
     * @return Random string.
     */
    private String randomString() {
        return UUID.randomUUID().toString();
    }
}
