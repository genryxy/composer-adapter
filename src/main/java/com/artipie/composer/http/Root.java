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
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Root resource. Used as endpoint to add a package.
 *
 * @since 0.1
 */
public final class Root implements Resource {

    /**
     * Storage to put content into.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage to read content from.
     */
    public Root(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response get() {
        return new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    public Response put(final Publisher<ByteBuffer> body) {
        return connection -> CompletableFuture
            .supplyAsync(() -> new Key.From(UUID.randomUUID().toString()))
            .thenCompose(
                key -> this.storage.save(key, new Content.From(body)).thenCompose(
                    ignored -> new Repository(this.storage).add(key)
                ).thenCompose(
                    ignored -> new RsWithStatus(RsStatus.CREATED).send(connection)
                )
            );
    }
}
