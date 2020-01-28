package com.artipie.maven;

import java.util.Objects;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Parses pattern {@code <groupId>:<artifactId>:<extension>[:<classifier>]:<version>}
 * like {@link DefaultArtifact} does but "extension" group is mandatory (and classifier is optional)
 */
public class FileCoordinates {
    private final String groupId;

    private final String artifactId;

    private final String baseVersion;

    // nullable
    private final String classifier;

    private final String extension;

    private FileCoordinates(String groupId, String artifactId, String baseVersion, String classifier, String extension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.baseVersion = baseVersion;
        this.classifier = classifier;
        this.extension = extension;
    }

    public static FileCoordinates fromArtifact(Artifact a) {
        return new FileCoordinates(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier(), a.getExtension());
    }

    public static FileCoordinates parse(String s) {
        return fromArtifact(new DefaultArtifact(s));
    }

    public Optional<String> getClassifier() {
        return Optional.ofNullable(classifier).filter(s -> !s.isBlank());
    }

    /**
     * @return colon-delimited string
     */
    @Override
    public String toString() {
        return new DefaultArtifact(groupId, artifactId, classifier, extension, baseVersion).toString();
    }

    /**
     * @return relative slash-delimited path
     */
    public String getPath() {
        return String.join("/", groupId.replace('.', '/'), artifactId, baseVersion, getFileName());
    }

    /**
     * @return only file name
     */
    public String getFileName() {
        var name = new StringBuilder()
            .append(artifactId).append("-").append(baseVersion);
        getClassifier().ifPresent(s -> {
            name.append("-").append(s);
        });
        name.append(".").append(extension);
        return name.toString();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileCoordinates that = (FileCoordinates) o;
        return Objects.equals(groupId, that.groupId) &&
            Objects.equals(artifactId, that.artifactId) &&
            Objects.equals(baseVersion, that.baseVersion) &&
            Objects.equals(classifier, that.classifier) &&
            Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, baseVersion, classifier, extension);
    }
}
