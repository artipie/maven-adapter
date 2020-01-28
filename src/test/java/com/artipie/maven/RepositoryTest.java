package com.artipie.maven;

import static com.artipie.maven.ChecksumAttributeFile.Type.MD5;
import static com.artipie.maven.ChecksumAttributeFile.Type.SHA1;
import static com.artipie.maven.ChecksumAttributeFile.getChecksumPath;
import static com.artipie.maven.test.Assertions.assertExists;

import com.artipie.maven.lib.ServiceLocatorFacade;
import com.artipie.maven.service.StagingFileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RepositoryTest {
    private static final Logger log = LoggerFactory.getLogger(RepositoryTest.class);

    @TempDir
    Path root;

    Path staging;

    Path local;

    @BeforeEach
    public void before(TestInfo testInfo) {
        staging = root.resolve("staging");
        local = root.resolve("repository");

        log.info("TestInfo {} staging {}", testInfo.getDisplayName(), staging);
        log.info("TestInfo {} local {}", testInfo.getDisplayName(), local);
    }

    @Test
    public void testUpload() throws Exception {
        var serviceLocatorFacade = ServiceLocatorFacade.create(local);

        var stagingFileStorage = new StagingFileStorage(serviceLocatorFacade.createLocalRepositoryManager());
        var repository = Repository.create(stagingFileStorage, serviceLocatorFacade);

        var coordinates = FileCoordinates.parse("org.example:empty:jar:1.0");
        var result = repository.upload(fromResourceEmpty(coordinates));
        log.info("{}", result);
        var path = local.resolve(result.getPath());
        var files = Files.walk(path)
            .filter(p -> !Files.isDirectory(p))
            .collect(Collectors.toList());
        log.info("{}", files);
        assertExists(path);
        assertExists(getChecksumPath(path, MD5));
        assertExists(getChecksumPath(path, SHA1));
        assertExists(path.getParent().resolveSibling("maven-metadata.xml"));
    }

    static UploadRequest fromResourceEmpty(FileCoordinates coordinates) {
        var resource = "empty/" + coordinates.getFileName();
        try (var is = RepositoryTest.class.getResourceAsStream("/" + resource)) {
            if (is == null) {
                throw new NullPointerException("no such resource " + resource);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("with " + resource, e);
        }
        return new UploadRequest(coordinates, () -> {
            InputStream is = RepositoryTest.class.getResourceAsStream("/" + resource);
            if (is == null) {
                throw new NullPointerException("no resource ");
            }
            return is;
        });
    }

}
