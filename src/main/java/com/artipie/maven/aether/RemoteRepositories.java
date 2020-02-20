/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.maven.aether;

import com.artipie.maven.RepositoryFile;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Maps artifact paths to remote repositories.
 * Configures remotes repositories in runtime like in Maven {@code settings.xml}
 * @since 0.1
 */
public interface RemoteRepositories {

    /**
     * Maps the artifact path to a remote repository to upload to.
     * It is assumed it should be an Asto instance.
     * @param path Uploading artifact
     * @return A remote repository. Should not be null
     */
    RemoteRepository uploading(RepositoryFile path);

    /**
     * Remote repositories to find the artifact in.
     * Artifacts are resolved in insertion order.
     * @param path Downloading artifact
     * @return Remote repositories to find the artifact in.
     */
    default List<RemoteRepository> downloading(RepositoryFile path) {
        return List.of(this.downloadingPrimary(path));
    }

    /**
     * A remote repository to find the artifact in.
     * Artifacts are resolved in insertion order.
     * @param path Downloading artifact
     * @return Remote repositories to find the artifact in.
     */
    RemoteRepository downloadingPrimary(RepositoryFile path);
}
