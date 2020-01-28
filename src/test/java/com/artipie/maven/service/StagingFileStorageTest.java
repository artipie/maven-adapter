package com.artipie.maven.service;

import static com.artipie.maven.test.Assertions.assertExists;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.artipie.maven.UploadRequest;
import com.artipie.maven.lib.ServiceLocatorFacade;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StagingFileStorageTest {

    @TempDir
    Path path;

    @Test
    void testStage() throws Exception {
        var helloWorld = "Hello world";
        var uploadRequest = UploadRequest.fromBytes("org.example:helloworld:pom:1.0", helloWorld.getBytes());

        var slf = ServiceLocatorFacade.create(path);
        var stagingFileStorage = new StagingFileStorage(slf.createLocalRepositoryManager());
        var file = stagingFileStorage.stage(uploadRequest);

        assertExists(file.getPath());
        assertEquals(helloWorld, Files.readString(file.getPath()));
    }
}
