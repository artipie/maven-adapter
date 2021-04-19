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
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * Maven integration test.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
@EnabledOnOs({OS.LINUX, OS.MAC})
public final class MavenITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test user.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("Alladin", "openSesame");

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Vertx slice server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downloadsDependency(final boolean anonymous) throws Exception {
        this.init(this.auth(anonymous));
        this.settings(this.getUser(anonymous));
        this.addHellowordToArtipie();
        MatcherAssert.assertThat(
            this.exec(
                "mvn", "-s", "/home/settings.xml", "dependency:get",
                "-Dartifact=com.artipie:helloworld:0.1"
            ),
            new StringContainsInOrder(
                new ListOf<String>(
                    // @checkstyle LineLengthCheck (1 line)
                    String.format("Downloaded from my-repo: http://host.testcontainers.internal:%d/com/artipie/helloworld/0.1/helloworld-0.1.jar (11 B", this.port),
                    "BUILD SUCCESS"
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deploysArtifact(final boolean anonymous) throws Exception {
        this.init(this.auth(anonymous));
        this.settings(this.getUser(anonymous));
        this.copyHellowordSourceToContainer();
        MatcherAssert.assertThat(
            "Failed to deploy version 1.0",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml", "deploy"
            ),
            new StringContains("BUILD SUCCESS")
        );
        this.clean();
        this.verifyArtifactsAdded("1.0");
        MatcherAssert.assertThat(
            "Failed to set version 2.0",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml",
                "versions:set", "-DnewVersion=2.0"
            ),
            new StringContains("BUILD SUCCESS")
        );
        MatcherAssert.assertThat(
            "Failed to deploy version 2.0",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml", "deploy"
            ),
            new StringContains("BUILD SUCCESS")
        );
        this.clean();
        this.verifyArtifactsAdded("2.0");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deploysSnapshotAfterRelease(final boolean anonymous) throws Exception {
        this.init(this.auth(anonymous));
        this.settings(this.getUser(anonymous));
        this.copyHellowordSourceToContainer();
        MatcherAssert.assertThat(
            "Failed to deploy version 1.0",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml", "deploy"
            ),
            new StringContains("BUILD SUCCESS")
        );
        this.clean();
        this.verifyArtifactsAdded("1.0");
        MatcherAssert.assertThat(
            "Failed to set version 2.0-SNAPSHOT",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml",
                "versions:set", "-DnewVersion=2.0-SNAPSHOT"
            ),
            new StringContains("BUILD SUCCESS")
        );
        MatcherAssert.assertThat(
            "Failed to deploy version 2.0",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml", "deploy"
            ),
            new StringContains("BUILD SUCCESS")
        );
        this.clean();
        this.verifySnapshotAdded("2.0-SNAPSHOT");
        MatcherAssert.assertThat(
            "Maven metadata xml is not correct",
            new XMLDocument(
                this.storage.value(new Key.From("com/artipie/helloworld/maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '2.0-SNAPSHOT']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '1.0']")
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deploysSnapshot(final boolean anonymous) throws Exception {
        this.init(this.auth(anonymous));
        this.settings(this.getUser(anonymous));
        this.copyHellowordSourceToContainer();
        MatcherAssert.assertThat(
            "Failed to set version 1.0-SNAPSHOT",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml",
                "versions:set", "-DnewVersion=1.0-SNAPSHOT"
            ),
            new StringContains("BUILD SUCCESS")
        );
        MatcherAssert.assertThat(
            "Failed to deploy version 1.0-SNAPSHOT",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml", "deploy"
            ),
            new StringContains("BUILD SUCCESS")
        );
        this.clean();
        this.verifySnapshotAdded("1.0-SNAPSHOT");
        MatcherAssert.assertThat(
            "Maven metadata xml is not correct",
            new XMLDocument(
                this.storage.value(new Key.From("com/artipie/helloworld/maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '1.0-SNAPSHOT']")
                )
            )
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        MavenITCase.VERTX.close();
    }

    void init(final Pair<Permissions, Authentication> auth) {
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            MavenITCase.VERTX,
            new LoggingSlice(new MavenSlice(this.storage, auth.getKey(), auth.getValue()))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("maven:3.6.3-jdk-11")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    private String exec(final String... actions) throws Exception {
        return this.cntn.execInContainer(actions).getStdout().replaceAll("\n", "");
    }

    private void settings(final Optional<Pair<String, String>> user) throws IOException {
        final Path setting = this.tmp.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<String>(
                "<settings>",
                "   <servers>",
                "       <server>",
                "           <id>my-repo</id>",
                user.map(
                    data -> String.format(
                        "<username>%s</username>\n<password>%s</password>",
                        data.getKey(), data.getValue()
                    )
                ).orElse(""),
                "       </server>",
                "   </servers>",
                "    <profiles>",
                "        <profile>",
                "            <id>artipie</id>",
                "            <repositories>",
                "                <repository>",
                "                    <id>my-repo</id>",
                String.format("<url>http://host.testcontainers.internal:%d/</url>", this.port),
                "                </repository>",
                "            </repositories>",
                "        </profile>",
                "    </profiles>",
                "    <activeProfiles>",
                "        <activeProfile>artipie</activeProfile>",
                "    </activeProfiles>",
                "</settings>"
            )
        );
    }

    private void addHellowordToArtipie() {
        new TestResource("com/artipie/helloworld")
            .addFilesTo(this.storage, new Key.From("com", "artipie", "helloworld"));
    }

    private Pair<Permissions, Authentication> auth(final boolean anonymous) {
        final Pair<Permissions, Authentication> res;
        if (anonymous) {
            res = new ImmutablePair<>(Permissions.FREE, Authentication.ANONYMOUS);
        } else {
            res = new ImmutablePair<>(
                (user, action) -> MavenITCase.USER.getKey().equals(user.name())
                    && ("download".equals(action) || "upload".equals(action)),
                new Authentication.Single(
                    MavenITCase.USER.getKey(), MavenITCase.USER.getValue()
                )
            );
        }
        return res;
    }

    private Optional<Pair<String, String>> getUser(final boolean anonymous) {
        Optional<Pair<String, String>> res = Optional.empty();
        if (!anonymous) {
            res = Optional.of(MavenITCase.USER);
        }
        return res;
    }

    private void copyHellowordSourceToContainer() throws IOException {
        FileUtils.copyDirectory(
            new TestResource("helloworld-src").asPath().toFile(),
            this.tmp.resolve("helloworld-src").toFile()
        );
        Files.write(
            this.tmp.resolve("helloworld-src/pom.xml"),
            String.format(
                Files.readString(this.tmp.resolve("helloworld-src/pom.xml.template")), this.port
            ).getBytes()
        );
    }

    private void verifyArtifactsAdded(final String version) {
        MatcherAssert.assertThat(
            String.format("Artifacts with %s version were not added to storage", version),
            this.storage.list(new Key.From("com/artipie/helloworld"))
                .join().stream().map(Key::string).collect(Collectors.toList()),
            Matchers.hasItems(
                "com/artipie/helloworld/maven-metadata.xml",
                String.format("com/artipie/helloworld/%s/helloworld-%s.pom", version, version),
                String.format("com/artipie/helloworld/%s/helloworld-%s.jar", version, version)
            )
        );
    }

    private void verifySnapshotAdded(final String version) {
        MatcherAssert.assertThat(
            String.format("Artifacts with %s version were not added to storage", version),
            this.storage.list(new Key.From("com/artipie/helloworld", version))
                .join().stream().map(Key::string).collect(Collectors.toList()),
            Matchers.allOf(
                Matchers.hasItem(new StringContains(".jar")),
                Matchers.hasItem(new StringContains(".pom")),
                Matchers.hasItem(new StringContains("maven-metadata.xml"))
            )
        );
    }

    private void clean() throws Exception {
        this.exec("mvn", "-s", "/home/settings.xml", "-f", "/home/helloworld-src/pom.xml", "clean");
    }
}
