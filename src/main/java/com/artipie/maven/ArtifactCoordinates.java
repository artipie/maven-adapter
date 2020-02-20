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

/**
 * Artifact coordinates parts split into meaningful parts.
 * All return values should not be null.
 * Should be effectively sealed.
 * @since 0.1
 */
public interface ArtifactCoordinates extends RepositoryFile {

    /**
     * Maven artifact groupId.
     *
     * @return Maven artifact groupId
     */
    String groupId();

    /**
     * Maven artifactId.
     * @return Maven artifactId
     */
    String artifactId();

    /**
     * Parses different ArtifactCoordinates from a path string.
     * @since 0.1
     */
    class Parser {

        /**
         * File path.
         */
        private final String path;

        /**
         * All args constructor.
         * @param path A path to parse from
         */
        public Parser(final String path) {
            this.path = path;
        }

        /**
         * Parses different ArtifactCoordinates from a path string.
         * @return ArtifactCoordinates instance
         */
        public ArtifactCoordinates parse() {
            final ArtifactCoordinates result;
            if (this.path.endsWith(MavenMetadata.MAVEN_METADATA)) {
                result = new MavenMetadata(this.path);
            } else {
                result = new FileCoordinates(this.path);
            }
            return result;
        }
    }
}
