package com.artipie.maven;


import java.util.Objects;

public class UploadResult {
    /**
     * colon-delimited string
     */
    private final String coordinates;
    /**
     * relative path
     */
    private final String path;
    private final long size;
    private final String md5; // nullable
    private final String sha1; // nullable

    public UploadResult(String coordinates, String path, long size, String md5, String sha1) {
        this.coordinates = coordinates;
        this.path = path;
        this.size = size;
        this.md5 = md5;
        this.sha1 = sha1;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getMd5() {
        return md5;
    }

    public String getSha1() {
        return sha1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UploadResult that = (UploadResult) o;
        return size == that.size &&
            Objects.equals(coordinates, that.coordinates) &&
            Objects.equals(path, that.path) &&
            Objects.equals(md5, that.md5) &&
            Objects.equals(sha1, that.sha1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates, path, size, md5, sha1);
    }

    @Override
    public String toString() {
        return "UploadResult{" +
            "coordinates='" + coordinates + '\'' +
            ", path='" + path + '\'' +
            ", size=" + size +
            ", md5='" + md5 + '\'' +
            ", sha1='" + sha1 + '\'' +
            '}';
    }
}
