package com.artipie.maven;

import com.artipie.maven.lib.ServiceLocatorFacade;
import com.artipie.maven.service.StagingFileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Repository {
    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    private final LocalRepositoryManagerFactory localRepositoryManagerFactory;

    private final LocalRepository localRepository;

    private final RepositorySystem repositorySystem;

    private final StagingFileStorage stagingFileStorage;

    public Repository(LocalRepositoryManagerFactory localRepositoryManagerFactory, LocalRepository localRepository, RepositorySystem repositorySystem, StagingFileStorage stagingFileStorage) {
        this.localRepositoryManagerFactory = localRepositoryManagerFactory;
        this.localRepository = localRepository;
        this.repositorySystem = repositorySystem;
        this.stagingFileStorage = stagingFileStorage;
    }

    public static Repository create(StagingFileStorage stagingFileStorage, ServiceLocatorFacade serviceLocatorFacade) {
        var lrmf = serviceLocatorFacade.getService(LocalRepositoryManagerFactory.class);
        var localRepository = serviceLocatorFacade.getLocalRepository();
        var repositorySystem = serviceLocatorFacade.getService(RepositorySystem.class);
        return new Repository(lrmf, localRepository, repositorySystem, stagingFileStorage);
    }

    public UploadResult upload(UploadRequest uploadRequest) {
        var stagingFile = stagingFileStorage.stage(uploadRequest);
        try {
            var session = ServiceLocatorFacade.createSession(localRepositoryManagerFactory, localRepository);
            var deployRequest = toDeployRequest(uploadRequest, stagingFile.getPath());
            var deployResult = repositorySystem.deploy(session, deployRequest);
            var result = adaptResult(deployResult);
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Artifact adaptArtifact(UploadRequest uploadRequest, Path path) {
        return new DefaultArtifact(uploadRequest.getCoordinates().toString())
            .setFile(path == null ? null : path.toFile());
    }

    private DeployRequest toDeployRequest(UploadRequest uploadRequest, Path path) {
        String url = localRepository.getBasedir().toURI().toString();
        var remoteRepository = new RemoteRepository.Builder("repository", "default", url)
            .build();
        var artifact = adaptArtifact(uploadRequest, path);
        var request = new DeployRequest();
        request.setRepository(remoteRepository);
        request.addArtifact(artifact);
        return request;
    }

    private UploadResult adaptResult(DeployResult deployResult) {
        var list = deployResult.getArtifacts().stream()
            .filter(a -> a.getFile() != null && a.getFile().canRead())
            .collect(Collectors.toList());
        if (list.size() != 1) {
            throw new IllegalStateException("expected ONE and only ONE artifact, got " + list);
        }
        var a = list.get(0);
        var coordinates = FileCoordinates.fromArtifact(a);
        Path path = a.getFile().toPath();
        try {
            var size = Files.size(path);
            var artifactChecksum = new ChecksumAttributeFile(path);
            var md5 = artifactChecksum.readMd5().orElse(null);
            var sha1 = artifactChecksum.readSha1().orElse(null);
            return new UploadResult(coordinates.toString(), coordinates.getPath(), size, md5, sha1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
