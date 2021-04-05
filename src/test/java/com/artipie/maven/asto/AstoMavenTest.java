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

package com.artipie.maven.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.maven.MetadataXml;
import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AstoMaven} class.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AstoMavenTest {

    /**
     * Logger upload key.
     */
    public static final Key LGR_UPLOAD = new Key.From(".update/com/test/logger");

    /**
     * Logger package key.
     */
    public static final Key LGR = new Key.From("com/test/logger");

    /**
     * Asto artifact key.
     */
    private static final Key.From ASTO = new Key.From("com/artipie/asto");

    /**
     * Asto upload key.
     */
    private static final Key.From ASTO_UPLOAD = new Key.From(".upload/com/artipie/asto");

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void generatesMetadata() {
        final String latest = "0.20.2";
        this.addFilesToStorage(
            item -> !item.contains("1.0-SNAPSHOT") && !item.contains(latest),
            AstoMavenTest.ASTO
        );
        this.addFilesToStorage(
            item -> item.contains(latest),
            AstoMavenTest.ASTO_UPLOAD
        );
        this.metadataAndVersions(latest);
        new AstoMaven(this.storage)
            .update(AstoMavenTest.ASTO_UPLOAD, AstoMavenTest.ASTO)
            .toCompletableFuture()
            .join();
        MatcherAssert.assertThat(
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.ASTO_UPLOAD, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    // @checkstyle LineLengthCheck (20 lines)
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.artipie']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'asto']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.15']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.11.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.18']"),
                    XhtmlMatchers.hasXPath("metadata/versioning/versions[count(//version) = 5]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
    }

    @Test
    void generatesMetadataForFirstArtifact() {
        new TestResource("maven-metadata.xml.example")
            .saveTo(this.storage, new Key.From(AstoMavenTest.LGR_UPLOAD, "maven-metadata.xml"));
        new AstoMaven(this.storage).update(AstoMavenTest.LGR_UPLOAD, AstoMavenTest.LGR)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.LGR_UPLOAD, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.test']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'logger']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions[count(//version) = 1]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
    }

    @Test
    void addsMetadataChecksums() {
        new TestResource("maven-metadata.xml.example")
            .saveTo(this.storage, new Key.From(AstoMavenTest.LGR_UPLOAD, "maven-metadata.xml"));
        new AstoMaven(this.storage).update(AstoMavenTest.LGR_UPLOAD, AstoMavenTest.LGR)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.list(AstoMavenTest.LGR_UPLOAD).join().stream()
                .map(key -> new KeyLastPart(key).get())
                .filter(key -> key.contains("maven-metadata.xml"))
                .toArray(String[]::new),
            Matchers.arrayContainingInAnyOrder(
                "maven-metadata.xml", "maven-metadata.xml.sha1", "maven-metadata.xml.sha256",
                "maven-metadata.xml.sha512", "maven-metadata.xml.md5"
            )
        );
    }

    @Test
    void generatesWithSnapshotMetadata() throws Exception {
        final String snapshot = "1.0-SNAPSHOT";
        final Predicate<String> cond = item -> !item.contains(snapshot);
        this.addFilesToStorage(cond, AstoMavenTest.ASTO);
        this.addFilesToStorage(cond.negate(), AstoMavenTest.ASTO);
        this.metadataAndVersions(snapshot, "0.20.2");
        new AstoMaven(this.storage)
            .update(AstoMavenTest.ASTO_UPLOAD, AstoMavenTest.ASTO)
            .toCompletableFuture()
            .get();
        MatcherAssert.assertThat(
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.ASTO_UPLOAD, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    // @checkstyle LineLengthCheck (20 lines)
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.artipie']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'asto']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '1.0-SNAPSHOT']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.15']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.11.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.18']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '1.0-SNAPSHOT']"),
                    XhtmlMatchers.hasXPath("metadata/versioning/versions[count(//version) = 6]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
    }

    private void addFilesToStorage(final Predicate<String> condition, final Key base) {
        final Storage resources = new FileStorage(new TestResource("com/artipie/asto").asPath());
        final BlockingStorage bsto = new BlockingStorage(resources);
        bsto.list(Key.ROOT).stream()
            .map(Key::string)
            .filter(condition)
            .forEach(
                item -> new Unchecked<>(
                    () -> {
                        new BlockingStorage(this.storage).save(
                            new Key.From(base, item),
                            new Unchecked<>(() -> bsto.value(new Key.From(item))).value()
                        );
                        return true;
                    }
                ).value()
        );
    }

    private void metadataAndVersions(final String... versions) {
        new MetadataXml("com.artipie", "asto").addXmlToStorage(
            this.storage, new Key.From(AstoMavenTest.ASTO_UPLOAD, "maven-metadata.xml"),
            new MetadataXml.VersionTags(
                "0.20.2", "0.20.2",
                Stream.concat(
                    Stream.of("0.11.1", "0.15", "0.18", "0.20.1"),
                    Stream.of(versions)
                ).collect(Collectors.toList())
            )
        );
    }

}
