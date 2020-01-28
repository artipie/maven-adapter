package com.artipie.maven.service;

import com.artipie.maven.UploadRequest;
import com.artipie.maven.util.AutoCloseablePath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A temporary intermediate storage as Maven is strongly coupled to file system
 * Ideally each file created should be one-shot only and deleted after the request was processed
 */
public class StagingFileStorage {

    private static final Logger log = LoggerFactory.getLogger(StagingFileStorage.class);

    private final LocalRepositoryManager localRepositoryManager;

    public StagingFileStorage(LocalRepositoryManager localRepositoryManager) {
        this.localRepositoryManager = localRepositoryManager;
    }

    public AutoCloseablePath stage(UploadRequest uploadRequest) {
        try {
            var a = new DefaultArtifact(uploadRequest.getCoordinates().toString());
            String relativePath = localRepositoryManager.getPathForLocalArtifact(a);
            Path localRepositoryRoot = localRepositoryManager.getRepository().getBasedir().toPath();
            var path = localRepositoryRoot.resolve(relativePath);

            Files.createDirectories(path.getParent());
            try (var is = uploadRequest.getInputStreamSupplier().get()) {
                try (var os = Files.newOutputStream(path)) {
                    var size = is.transferTo(os);
                    log.debug("stage {} written {} to {}", uploadRequest.getCoordinates(), size, path);
                    return new AutoCloseablePath(path);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
