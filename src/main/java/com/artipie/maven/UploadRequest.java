package com.artipie.maven;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Supplier;

public class UploadRequest {
    private final FileCoordinates coordinates;
    private final Supplier<InputStream> inputStreamSupplier;

    public UploadRequest(FileCoordinates coordinates, Supplier<InputStream> inputStreamSupplier) {
        this.coordinates = coordinates;
        this.inputStreamSupplier = inputStreamSupplier;
    }

    public static UploadRequest fromBytes(String coordinates, byte[] bytes) {
        return new UploadRequest(FileCoordinates.parse(coordinates), () -> new ByteArrayInputStream(bytes));
    }

    public FileCoordinates getCoordinates() {
        return coordinates;
    }

    public Supplier<InputStream> getInputStreamSupplier() {
        return inputStreamSupplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UploadRequest that = (UploadRequest) o;
        return Objects.equals(coordinates, that.coordinates) &&
            Objects.equals(inputStreamSupplier, that.inputStreamSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates, inputStreamSupplier);
    }

    @Override
    public String toString() {
        return "UploadRequest{" +
            "coordinates=" + coordinates +
            ", inputStreamSupplier=" + inputStreamSupplier +
            '}';
    }
}
