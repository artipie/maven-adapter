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

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.reactivestreams.Publisher;

/**
 * This slice accepts PUT requests with maven-metadata.xml checksums, maven-metadata.xml.sha1 and
 * maven-metadata.xml.md5, picks up corresponding maven-metadata.xml from the package upload temp
 * location and saves the checksum. If two checksums (sha1 and md5) are present in the package
 * upload location, this slice initiate repository update.
 * @since 0.8
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class MetadataChecksumMavenSlice implements Slice {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public MetadataChecksumMavenSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        throw new NotImplementedException("Not yet implemented");
    }
}
