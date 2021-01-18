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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.google.common.io.ByteSource;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<Optional<Packages>> packages() {
        return this.packages(Repository.ALL_PACKAGES);
    }

    /**
     * Reads packages description from storage.
     *
     * @param name Package name.
     * @return Packages found by name, might be empty.
     */
    public CompletionStage<Optional<Packages>> packages(final Name name) {
        return this.packages(name.key());
    }

    /**
     * Adds package described in JSON format from storage.
     *
     * @param content Package content.
     * @return Completion of adding package to repository.
     */
    public CompletableFuture<Void> add(final Content content) {
        final Key key = new Key.From(UUID.randomUUID().toString());
        return this.storage.save(key, content).thenCompose(
            saved -> this.storage.value(key)
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::bytes)
                .thenCompose(
                    bytes -> {
                        final Package pack = new JsonPackage(ByteSource.wrap(bytes));
                        final Name name = pack.name();
                        return CompletableFuture.allOf(
                            this.packages().thenCompose(
                                packages -> packages.orElse(new JsonPackages()).add(pack)
                                    .save(this.storage, Repository.ALL_PACKAGES)
                            ).toCompletableFuture(),
                            this.packages(name).thenCompose(
                                packages -> packages.orElse(new JsonPackages()).add(pack)
                                    .save(this.storage, name.key())
                            ).toCompletableFuture()
                        ).thenCompose(
                            ignored -> this.storage.delete(key)
                        );
                    }
                )
        );
    }

    /**
     * Reads packages description from storage.
     *
     * @param key Content location in storage.
     * @return Packages found by name, might be empty.
     */
    private CompletionStage<Optional<Packages>> packages(final Key key) {
        return this.storage.exists(key).thenCompose(
            exists -> {
                final CompletionStage<Optional<Packages>> packages;
                if (exists) {
                    packages = this.storage.value(key)
                        .thenApply(PublisherAs::new)
                        .thenCompose(PublisherAs::bytes)
                        .thenApply(bytes -> new JsonPackages(ByteSource.wrap(bytes)))
                        .thenApply(Optional::of);
                } else {
                    packages = CompletableFuture.completedFuture(Optional.empty());
                }
                return packages;
            }
        );
    }
}
