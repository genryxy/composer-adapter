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
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.composer.Repository;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice for adding a package to the repository.
 *
 * @since 0.3
 */
public final class AddSlice implements Slice {

    /**
     * RegEx pattern for matching path.
     */
    public static final Pattern PATH_PATTERN = Pattern.compile("^/$");

    /**
     * Storage to put content into.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage to read content from.
     */
    public AddSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> new Key.From(UUID.randomUUID().toString())
            ).thenCompose(
                key -> this.storage.save(key, new Content.From(body)).thenCompose(
                    nothing -> new Repository(this.storage).add(key)
                ).thenApply(
                    nothing -> new RsWithStatus(RsStatus.CREATED)
                )
            )
        );
    }
}
