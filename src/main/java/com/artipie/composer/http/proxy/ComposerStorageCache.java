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
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.composer.Repository;
import com.artipie.composer.misc.ContentAsJson;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Cache implementation that tries to obtain items from storage cache,
 * validates it and returns if valid. If item is not present in storage or is not valid,
 * it is loaded from remote.
 * @since 0.4
 * @checkstyle ReturnCountCheck (500 lines)
 */
final class ComposerStorageCache implements Cache {
    /**
     * Folder for cached items.
     */
    private static final String CACHE_FOLDER = "cache";

    /**
     * Repository.
     */
    private final Repository repo;

    /**
     * Ctor.
     * @param repository Repository
     */
    ComposerStorageCache(final Repository repository) {
        this.repo = repository;
    }

    @Override
    public CompletionStage<Optional<? extends Content>> load(
        final Key name, final Remote remote, final CacheControl control
    ) {
        final Key cached = new Key.From(
            ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", name.string())
        );
        return this.repo.exists(cached)
            .thenCompose(
                exists -> {
                    final AtomicReference<Boolean> fromcache = new AtomicReference<>();
                    if (exists) {
                        return control.validate(
                            cached,
                            () -> CompletableFuture.completedFuture(Optional.empty())
                        ).thenCompose(
                            valid -> {
                                final CompletionStage<Optional<Content>> cacheval;
                                if (valid) {
                                    cacheval = this.repo.value(cached).thenApply(Optional::of);
                                    fromcache.set(true);
                                } else {
                                    cacheval = CompletableFuture.completedFuture(Optional.empty());
                                    fromcache.set(false);
                                }
                                return cacheval;
                            }
                        ).thenApply(Function.identity());
                    }
                    if (fromcache.get() == null || !fromcache.get()) {
                        return CompletableFuture.supplyAsync(() -> null)
                            .thenCombine(
                                remote.get(),
                                (nothing, content) -> {
                                    final CompletionStage<Optional<? extends Content>> res;
                                    if (content.isPresent()) {
                                        res = this.repo.save(cached, content.get())
                                            .thenCompose(noth -> this.updateCacheFile(cached, name))
                                            .thenCompose(ignore -> this.repo.value(cached))
                                            .thenApply(Optional::of);
                                    } else {
                                        res = CompletableFuture.completedFuture(Optional.empty());
                                    }
                                    return res;
                                }
                            ).thenCompose(Function.identity());
                    }
                    return CompletableFuture.completedFuture(Optional.empty());
                }
            );
    }

    /**
     * Update existed in storage cache file.
     * @param cached Key for obtaining cached package
     * @param name Name of cached item (usually like `vendor/package`)
     * @return Result of completion.
     */
    private CompletionStage<Void> updateCacheFile(final Key cached, final Key name) {
        final Key tmp = new Key.From(
            String.format("%s%s.json", cached, UUID.randomUUID().toString())
        );
        return this.repo.exists(CacheTimeControl.CACHE_FILE)
            .thenCompose(this::createCacheFileIfAbsent)
            .thenCompose(
                nothing -> this.repo.exclusively(
                    CacheTimeControl.CACHE_FILE,
                    nthng -> this.repo.value(CacheTimeControl.CACHE_FILE)
                        .thenApply(ContentAsJson::new)
                        .thenCompose(ContentAsJson::value)
                        .thenApply(json -> ComposerStorageCache.addTimeFor(json, name))
                        .thenCompose(json -> this.repo.save(tmp, new Content.From(json)))
                        .thenCompose(noth -> this.repo.delete(CacheTimeControl.CACHE_FILE))
                        .thenCompose(noth -> this.repo.move(tmp, CacheTimeControl.CACHE_FILE))
                )
            );
    }

    /**
     * Creates cache file in case of absent.
     * @param exists Does file exists?
     * @return Result of completion.
     */
    private CompletionStage<Void> createCacheFileIfAbsent(final boolean exists) {
        final CompletionStage<Void> res;
        if (exists) {
            res = CompletableFuture.allOf();
        } else {
            res = this.repo.save(
                CacheTimeControl.CACHE_FILE,
                new Content.From(
                    Json.createObjectBuilder().build()
                        .toString().getBytes()
                )
            );
        }
        return res;
    }

    /**
     * Add time in json for passed item.
     * @param json JSON file (e.g. which contains info for cached items)
     * @param name Item which should be added
     * @return Updated JSON with added info for passed key.
     */
    private static byte[] addTimeFor(final JsonObject json, final Key name) {
        return Json.createObjectBuilder(json)
            .add(
                name.string(),
                ZonedDateTime.ofInstant(
                    Instant.now(),
                    ZoneOffset.UTC
                ).toString()
            ).build().toString()
            .getBytes();
    }
}
