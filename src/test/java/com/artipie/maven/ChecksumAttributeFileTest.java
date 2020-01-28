package com.artipie.maven;

import static com.artipie.maven.ChecksumAttributeFile.Type.MD5;
import static com.artipie.maven.ChecksumAttributeFile.Type.SHA1;
import static com.artipie.maven.ChecksumAttributeFile.getChecksumPath;
import static com.artipie.maven.ChecksumAttributeFile.readLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChecksumAttributeFileTest {

    @TempDir
    Path root;

    @Test
    void testGetChecksumPath() throws Exception {
        Path f = Files.createTempFile(root, "prefix", "suffix");
        assertEquals(f.toString()+".md5", getChecksumPath(f, MD5).toString());
        assertEquals(f.toString()+".sha1", getChecksumPath(f, SHA1).toString());
    }

    @Test
    void testReadLine() throws Exception {
        Path f = Files.createTempFile(root, "prefix", "suffix");

        String md5Content = UUID.randomUUID().toString().replace("-", "");
        Files.writeString(Paths.get(f.toString() + ".md5"), md5Content);
        assertEquals(md5Content, readLine(f, MD5).orElseThrow());

        String sha1Content = UUID.randomUUID().toString().replace("-", "");
        Files.writeString(Paths.get(f.toString() + ".sha1"), sha1Content);
        assertEquals(sha1Content, readLine(f, SHA1).orElseThrow());
    }
}