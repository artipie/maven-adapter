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
package com.artipie.maven;

import com.artipie.asto.Key;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Valid upload to maven repository.
 * @since 0.5
 */
public interface ValidUpload {

    /**
     * Validate upload:
     * - validate upload checksums;
     * - validate metadata: check metadata group and id are the same as in
     * repository metadata, metadata versions are correct.
     * @param upload Uploading artifact location
     * @param artifact Artifact location
     * @return Completable validation action: true if uploaded maven-metadata.xml is valid,
     *  false otherwise
     */
    CompletionStage<Boolean> validate(Key upload, Key artifact);

    /**
     * Is the upload ready to be added to repository? The upload is considered to be ready if
     * at an artifact (any, nondeterministic) and maven-metadata.xml have the same set of checksums.
     * @param location Upload location to check
     * @return Completable action with the result
     */
    CompletionStage<Boolean> ready(Key location);

    /**
     * Dummy {@link ValidUpload} implementation.
     * @since 0.5
     */
    final class Dummy implements ValidUpload {

        /**
         * Validation result.
         */
        private final boolean valid;

        /**
         * Is upload ready?
         */
        private final boolean rdy;

        /**
         * Ctor.
         * @param valid Result of the validation
         * @param ready Is upload ready?
         */
        public Dummy(final boolean valid, final boolean ready) {
            this.valid = valid;
            this.rdy = ready;
        }

        /**
         * Ctor.
         * @param valid Result of the validation
         */
        public Dummy(final boolean valid) {
            this(valid, true);
        }

        /**
         * Ctor.
         */
        public Dummy() {
            this(true, true);
        }

        @Override
        public CompletionStage<Boolean> validate(final Key upload, final Key artifact) {
            return CompletableFuture.completedFuture(this.valid);
        }

        @Override
        public CompletionStage<Boolean> ready(final Key location) {
            return CompletableFuture.completedFuture(this.rdy);
        }

    }

}
