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

package com.artipie.composer;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class representing PHP Composer repository.
 *
 * @since 0.1
 */
public class Repository {

    /**
     * Key to all packages.
     */
    public static final Key ALL_PACKAGES = new AllPackages();

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage to store all repository data.
     */
    public Repository(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Reads packages description from storage.
     *
     * @return Packages found by name, might be empty.
     */
    public Packages packages() {
        return this.packages(Repository.ALL_PACKAGES);
    }

    /**
     * Reads packages description from storage.
     *
     * @param name Package name.
     * @return Packages found by name, might be empty.
     */
    public Packages packages(final Name name) {
        return this.packages(name.key());
    }

    /**
     * Adds package described in JSON format from storage.
     *
     * @param key Key to find content of package JSON.
     * @return Completion of adding package to repository.
     * @throws IOException In case exception occurred on operations with storage.
     */
    public CompletableFuture<Void> add(final Key key) throws IOException {
        final ByteSource content = ByteSource.wrap(new BlockingStorage(this.storage).value(key));
        final Package pack = new JsonPackage(content);
        final Name name = pack.name();
        return CompletableFuture.allOf(
            this.packages()
                .add(pack)
                .save(this.storage, Repository.ALL_PACKAGES),
            this.packages(name)
                .add(pack)
                .save(this.storage, name.key())
        );
    }

    /**
     * Reads packages description from storage.
     *
     * @param key Content location in storage.
     * @return Packages found by name, might be empty.
     */
    private Packages packages(final Key key) {
        final BlockingStorage blocking = new BlockingStorage(this.storage);
        final JsonPackages packages;
        if (blocking.exists(key)) {
            final ByteSource content = ByteSource.wrap(blocking.value(key));
            packages = new JsonPackages(content);
        } else {
            packages = new JsonPackages();
        }
        return packages;
    }
}
