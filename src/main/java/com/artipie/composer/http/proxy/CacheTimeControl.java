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

import com.artipie.asto.Key;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.composer.Repository;
import com.artipie.composer.misc.ContentAsJson;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Check if saved item is expired  by comparing time value.
 * @since 0.4
 * @todo #77:30min Move this class to asto.
 *  Move this class to asto module as soon as the implementation will
 *  be checked on convenience and rightness (e.g. this class will be used
 *  for implementation in this adapter and proper tests will be added).
 */
final class CacheTimeControl implements CacheControl {
    /**
     * Name to file which contains info about cached items (e.g. when an item was saved).
     */
    static final Key CACHE_FILE = new Key.From("cache/cache-info.json");

    /**
     * Time during which the file is valid.
     */
    private final Duration expiration;

    /**
     * Repository.
     */
    private final Repository repo;

    /**
     * Ctor with default value for time of expiration.
     * @param repository Repository
     * @checkstyle MagicNumberCheck (3 lines)
     */
    CacheTimeControl(final Repository repository) {
        this(repository, Duration.ofMinutes(10));
    }

    /**
     * Ctor.
     * @param repository Repository
     * @param expiration Time after which cached items are not valid
     */
    CacheTimeControl(final Repository repository, final Duration expiration) {
        this.repo = repository;
        this.expiration = expiration;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key item, final Remote content) {
        return this.repo.exists(CacheTimeControl.CACHE_FILE)
            .thenCompose(
                exists -> {
                    final CompletionStage<Boolean> res;
                    if (exists) {
                        res = this.repo.value(CacheTimeControl.CACHE_FILE)
                            .thenApply(ContentAsJson::new)
                            .thenCompose(ContentAsJson::value)
                            .thenApply(
                                json -> {
                                    final String key = item.string();
                                    return json.containsKey(key)
                                        && this.notExpired(json.getString(key));
                                }
                            );
                    } else {
                        res = CompletableFuture.completedFuture(false);
                    }
                    return res;
                }
            );
    }

    /**
     * Validate time by comparing difference with time of expiration.
     * @param time Time of uploading
     * @return True is valid as not expired yet, false otherwise.
     */
    private boolean notExpired(final String time) {
        return !Duration.between(
            Instant.now().atZone(ZoneOffset.UTC),
            ZonedDateTime.parse(time)
        ).plus(this.expiration)
        .isNegative();
    }
}
