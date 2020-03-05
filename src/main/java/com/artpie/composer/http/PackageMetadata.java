/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artpie.composer.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artpie.composer.Name;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Package metadata resource.
 *
 * @since 0.1
 */
public final class PackageMetadata implements Resource {

    /**
     * RegEx pattern for matching path.
     */
    private static final Pattern PATH_PATTERN = Pattern.compile(
        "/p/(?<vendor>[^/]+)/(?<package>[^/]+)\\.json$"
    );

    /**
     * Resource path.
     */
    private final String path;

    /**
     * Storage to read content from.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param path Resource path.
     * @param storage Storage to read content from.
     */
    public PackageMetadata(final String path, final Storage storage) {
        this.path = path;
        this.storage = storage;
    }

    @Override
    public Response put() {
        return new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    public Response get() {
        return connection -> CompletableFuture.supplyAsync(PackageMetadata.this::key)
            .thenCompose(
                key -> this.storage.exists(key).thenCompose(
                    exists -> {
                        final CompletionStage<Void> sent;
                        if (exists) {
                            sent = this.storage.value(key).thenCompose(
                                data -> connection.accept(
                                    RsStatus.OK,
                                    Collections.emptyList(),
                                    data
                                )
                            );
                        } else {
                            sent = new RsWithStatus(RsStatus.NOT_FOUND).send(connection);
                        }
                        return sent;
                    }
                )
            );
    }

    /**
     * Builds key to storage value from path.
     *
     * @return Key to storage value.
     */
    private Key key() {
        final Matcher matcher = PATH_PATTERN.matcher(this.path);
        if (!matcher.find()) {
            throw new IllegalStateException(
                String.format("Unexpected path: %s", this.path)
            );
        }
        return new Name(
            String.format("%s/%s", matcher.group("vendor"), matcher.group("package"))
        ).key();
    }
}
