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
package com.artipie.maven.metadata;

import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.ext.PublisherAs;
import com.jcabi.xml.XMLDocument;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Extracts information from the metadata maven client sends on deploy.
 * @since 0.8
 */
public final class DeployMetadata {

    /**
     * Metadata.
     */
    private final Publisher<ByteBuffer> data;

    /**
     * Ctor.
     * @param data Metadata
     */
    public DeployMetadata(final Publisher<ByteBuffer> data) {
        this.data = data;
    }

    /**
     * Get versioning/release tag value.
     * @return Completion action
     */
    CompletionStage<String> release() {
        return new PublisherAs(this.data).asciiString().thenApply(XMLDocument::new)
            .thenApply(xml -> xml.xpath("//release/text()"))
            .thenCompose(
                release -> {
                    final CompletionStage<String> res;
                    if (release.isEmpty()) {
                        res = new FailedCompletionStage<>(
                            new IllegalArgumentException("Failed to read deploy maven metadata")
                        );
                    } else {
                        res = CompletableFuture.completedFuture(release.get(0));
                    }
                    return res;
                }
            );
    }
}
