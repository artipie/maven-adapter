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
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.maven.MetadataXml;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link PutMetadataChecksumSlice}.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PutMetadataChecksumSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Test slice.
     */
    private Slice pmcs;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.pmcs = new PutMetadataChecksumSlice(this.asto);
    }

    @ParameterizedTest
    @ValueSource(strings = {"md5", "sha1", "sha256", "sha512"})
    void foundsCorrespondingMetadataAndSavesChecksum(final String alg) {
        final byte[] xml = new MetadataXml("com.example", "abc").get(
            new MetadataXml.VersionTags("0.1")
        ).getBytes(StandardCharsets.UTF_8);
        this.asto.save(
            new Key.From(UpdateMavenSlice.TEMP, "com/example/abc/0.1/maven-metadata.xml"),
            new Content.From(xml)
        ).join();
        this.asto.save(
            new Key.From(UpdateMavenSlice.TEMP, "com/example/abc/0.2/maven-metadata.xml"),
            new Content.From("any".getBytes())
        ).join();
        final byte[] mdfive = new ContentDigest(
            new Content.From(xml), Digests.valueOf(alg.toUpperCase(Locale.US))
        ).hex().toCompletableFuture().join().getBytes(StandardCharsets.US_ASCII);
        MatcherAssert.assertThat(
            "Incorrect response status, CREATED is expected",
            this.pmcs,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(
                    RqMethod.PUT, String.format("/com/example/abc/maven-metadata.xml.%s", alg)
                ),
                Headers.EMPTY,
                new Content.From(mdfive)
            )
        );
        MatcherAssert.assertThat(
            "Incorrect content was saved to storage",
            this.asto.value(
                new Key.From(
                    String.format(".upload/com/example/abc/0.1/maven-metadata.xml.%s", alg)
                )
            ).join(),
            new ContentIs(mdfive)
        );
    }

    @Test
    void returnsBadRequestIfSuitableMetadataFileNotFound() {
        this.asto.save(
            new Key.From(UpdateMavenSlice.TEMP, "com/example/xyz/0.1/maven-metadata.xml"),
            new Content.From("xml".getBytes())
        ).join();
        MatcherAssert.assertThat(
            "Incorrect response status, BAD REQUEST is expected",
            this.pmcs,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/com/example/xyz/maven-metadata.xml.sha1"),
                Headers.EMPTY,
                new Content.From("any".getBytes())
            )
        );
    }

    @Test
    void returnsBadRequestOnIncorrectRequest() {
        MatcherAssert.assertThat(
            this.pmcs,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/any/request/line")
            )
        );
    }

}
