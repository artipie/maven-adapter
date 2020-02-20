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

import java.util.Arrays;

/**
 * Represents 'maven-metadata.xml'.
 * @since 0.1
 * @checkstyle AvoidFieldNameMatchingMethodName (50 lines)
 */
public final class MavenMetadata implements ArtifactCoordinates {

    /**
     * The name is a constant.
     */
    public static final String MAVEN_METADATA = "maven-metadata.xml";

    /**
     * Full file path.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final String path;

    /**
     * All args constructor.
     * @param path Full file path
     */
    public MavenMetadata(final String path) {
        this.path = path;
    }

    @Override
    public String groupId() {
        final String[] parts = this.path.split("/");
        return String.join(
            ".",
            Arrays.copyOfRange(parts, 0, parts.length - 2)
        );
    }

    @Override
    public String artifactId() {
        final String[] parts = this.path.split("/");
        return parts[parts.length - 2];
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public String name() {
        return MavenMetadata.MAVEN_METADATA;
    }
}
