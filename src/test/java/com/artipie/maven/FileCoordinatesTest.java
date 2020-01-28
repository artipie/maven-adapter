package com.artipie.maven;

import static com.artipie.maven.test.Assertions.assertEmpty;
import static com.artipie.maven.test.Assertions.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileCoordinatesTest {

    @Test
    public void testEmptyClassifier() {
        assertEmpty(FileCoordinates.parse("group:artifact:1.0").getClassifier());
    }

    @Test
    public void testPresentClassifier() {
        assertPresent(FileCoordinates.parse("group:artifact:jar:sources:1.0").getClassifier(), c -> {
            assertEquals("sources", c);
        });
    }

    @Test
    public void testGetFileName() {
        assertEquals("artifact-2.0.jar", FileCoordinates.parse("org.group:artifact:jar:2.0").getFileName());
        assertEquals("artifact-1.0-sources.jar", FileCoordinates.parse("org.group:artifact:jar:sources:1.0").getFileName());
    }

    @Test
    public void testGetPath() {
        assertEquals("org/group/artifact/2.0/artifact-2.0.pom", FileCoordinates.parse("org.group:artifact:pom:2.0").getPath());
        assertEquals("org/group/artifact/1.0/artifact-1.0-sources.jar", FileCoordinates.parse("org.group:artifact:jar:sources:1.0").getPath());
    }


}
