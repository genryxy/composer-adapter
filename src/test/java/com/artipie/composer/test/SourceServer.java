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
package com.artipie.composer.test;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.files.FilesSlice;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.Closeable;
import java.util.UUID;

/**
 * Source server for obtaining uploaded content by url. For using in test scope.
 * @since 0.4
 */
@SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
public final class SourceServer implements Closeable {
    /**
     * Free port for starting server.
     */
    private final int port;

    /**
     * HTTP server hosting repository.
     */
    private final VertxSliceServer server;

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param vertx Vert.x instance. It should be closed from outside
     * @param port Free port to start server
     */
    public SourceServer(final Vertx vertx, final int port) {
        this.port = port;
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            vertx, new LoggingSlice(new FilesSlice(this.storage)), port
        );
        this.server.start();
    }

    /**
     * Upload empty ZIP archive as a content.
     * @return Url for obtaining uploaded content.
     * @throws Exception In case of error during uploading
     */
    public String upload() throws Exception {
        return this.upload(new EmptyZip().value());
    }

    /**
     * Upload content.
     * @param content Content for uploading
     * @return Url for obtaining uploaded content.
     */
    public String upload(final byte[] content) {
        final String name = UUID.randomUUID().toString();
        new BlockingStorage(this.storage)
            .save(new Key.From(name), content);
        return String.format("http://host.testcontainers.internal:%d/%s", this.port, name);
    }

    @Override
    public void close() {
        this.server.stop();
    }
}
