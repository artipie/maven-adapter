package com.artipie.maven;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads artifact checksum files
 */
public class ChecksumAttributeFile {
    private final Path path;

    public ChecksumAttributeFile(Path path) {
        this.path = path;
    }

    public Path getChecksumPath(Type type) {
        return getChecksumPath(path, type);
    }

    public Path getMD5Path() {
        return getChecksumPath(Type.MD5);
    }

    public Path getSha1Path() {
        return getChecksumPath(Type.SHA1);
    }

    public Optional<String> readLine(Type type) {
        return readLine(path, type);
    }

    public Optional<String> readMd5() {
        return readLine(Type.MD5);
    }

    public Optional<String> readSha1() {
        return readLine(Type.SHA1);
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChecksumAttributeFile that = (ChecksumAttributeFile) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "ArtifactChecksum{" +
            "path=" + path +
            '}';
    }

    /**
     * Simply appends checksum algorithms to a filenames as extension
     * Does not validate the resulting path
     */
    public static Path getChecksumPath(Path path, Type type) {
        return path.resolveSibling(path.getFileName().toString() + "." + type.getExtension());
    }

    /**
     *
     * @return empty if the file does not exist or it's empty, present otherwise
     * @throws UncheckedIOException in case of any problem
     */
    public static Optional<String> readLine(Path path, Type type) {
        var checksumPath = getChecksumPath(path, type);
        if (!Files.exists(checksumPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(checksumPath))
                .filter(s -> !s.isBlank());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Well known checksum algorithms
     */
    enum Type {
        MD5("md5"), SHA1("sha1");

        private final String extension;

        Type(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }
}
