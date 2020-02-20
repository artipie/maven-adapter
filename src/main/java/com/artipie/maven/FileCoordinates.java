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
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Parses and validates given string.
 * @since 0.1
 */
public final class FileCoordinates implements ArtifactCoordinates {

    /**
     * Path parts count.
     */
    private static final int PARTS_COUNT = 4;

    /**
     * Path parts.
     */
    private final Supplier<String[]> splitter;

    /**
     * Creates a new instance, validating input string.
     * @param path An URI path
     * @throws IllegalArgumentException in case of invalid format
     */
    public FileCoordinates(final String path) {
        this.splitter = () -> {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("path should not be blank");
            }
            final var pts = StringUtils.removeStart(path, "/")
                .split("/");
            if (Arrays.stream(pts).anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("path should not contain blank parts");
            }
            if (pts.length < FileCoordinates.PARTS_COUNT) {
                throw new IllegalArgumentException(
                    String.format(
                        "path should contain at least %d slash-delimited parts",
                        FileCoordinates.PARTS_COUNT
                    )
                );
            }
            return pts;
        };
    }

    /**
     * Builds colon-delimited Gradle-style line.
     * groupId:artifactId:extension[:classifier]:version.
     * @return Artifact coordinates
     */
    public String coords() {
        final var builder = new StringBuilder(
            String.join(":", this.groupId(), this.artifactId(), this.extension())
        );
        if (!this.classifier().isBlank()) {
            builder.append(':').append(this.classifier());
        }
        if (!this.version().isBlank()) {
            builder.append(':').append(this.version());
        }
        return builder.toString();
    }

    @Override
    public String path() {
        return String.join(
            "/",
            this.groupId().replace('.', '/'),
            this.artifactId(),
            this.version(),
            this.name()
        );
    }

    // @checkstyle MagicNumberCheck (5 lines)
    @Override
    public String groupId() {
        return String.join(
            ".",
            Arrays.copyOfRange(this.splitter.get(), 0, this.splitter.get().length - 3)
        );
    }

    @Override
    public String name() {
        return this.splitter.get()[this.splitter.get().length - 1];
    }

    /**
     * Version.
     * @return Returns version part
     * @checkstyle MagicNumberCheck (3 lines)
     */
    public String version() {
        return this.splitter.get()[this.splitter.get().length - 2];
    }

    // @checkstyle MagicNumberCheck (3 lines)
    @Override
    public String artifactId() {
        return this.splitter.get()[this.splitter.get().length - 3];
    }

    /**
     * Classifier.
     * @return Returns classifier part
     */
    public String classifier() {
        final var names = FilenameUtils.removeExtension(this.name())
            .split("-");
        var classifier = "";
        // @checkstyle MagicNumberCheck (3 lines)
        if (names.length == 3) {
            classifier = names[2];
        }
        return classifier;
    }

    /**
     * Classifier as Optional.
     * @return Returns classifier part as Optional
     */
    public Optional<String> tryClassifier() {
        return Optional.of(this.classifier())
            .filter(s -> !s.isBlank());
    }

    /**
     * File extension.
     * @return Returns file extension part
     */
    public String extension() {
        return FilenameUtils.getExtension(this.name());
    }
}
