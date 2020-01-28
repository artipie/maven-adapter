package com.artipie.maven.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class Assertions {

    public static void assertExists(Path file) {
        assertTrue(Files.exists(file), () -> "does not exist " + file);
    }

    public static void assertEmpty(Optional<?> o) {
        assertTrue(o.isEmpty(), () -> "should be empty, actual "+o.map(Object::toString).orElse(""));
    }

    public static <T> void assertPresent(Optional<T> o, Consumer<T> assertValue) {
        o.ifPresentOrElse(assertValue, () -> {
            fail("should be present, actual empty");
        });
    }

}
