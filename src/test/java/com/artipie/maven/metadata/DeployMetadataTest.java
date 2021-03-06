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

import com.artipie.maven.MetadataXml;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DeployMetadata}.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DeployMetadataTest {

    @Test
    void readsReleaseFieldValue() {
        final String release = "1.098";
        MatcherAssert.assertThat(
            new DeployMetadata(
                new MetadataXml("com.artipie", "abc").get(
                    new MetadataXml.VersionTags(
                        "12", release, new ListOf<>(release, "0.3", "12", "0.1")
                    )
                )
            ).release(),
            new IsEqual<>(release)
        );
    }

    @Test
    void throwsExceptionIfMetadataInvalid() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new DeployMetadata(
                new MetadataXml("com.artipie", "abc").get(
                    new MetadataXml.VersionTags("0.3", "12", "0.1")
                )
            ).release()
        );
    }

    @Test
    void readsSnapshotVersions() {
        final String one = "0.1-SNAPSHOT";
        final String two = "0.2-SNAPSHOT";
        final String three = "3.1-SNAPSHOT";
        MatcherAssert.assertThat(
            new DeployMetadata(
                new MetadataXml("com.example", "logger").get(
                    new MetadataXml.VersionTags(one, "0.7", "13", two, "0.145", three)
                )
            ).snapshots(),
            Matchers.containsInAnyOrder(one, three, two)
        );
    }

}
