package com.artipie.maven.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Path} that is being deleted on close. Catches and logs {@link IOException} but does not rethrow it.
 */
public class AutoCloseablePath implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AutoCloseablePath.class);

    private final Path path;

    public AutoCloseablePath(Path path) {
        this.path = path;
    }

    public boolean delete() {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("when deleting "+path, e);
            return false;
        }
    }

    @Override
    public void close() throws Exception {
        delete();
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
        AutoCloseablePath that = (AutoCloseablePath) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "AutoCloseablePath{" +
            "path=" + path +
            '}';
    }
}
