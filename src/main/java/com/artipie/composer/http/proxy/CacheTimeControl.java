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
import java.util.concurrent.CompletionStage;

/**
 * Checking does saved item expire by comparing time value.
 * @since 0.4
 */
final class CacheTimeControl implements CacheControl {
    /**
     * Time during which the file is valid.
     */
    static final Duration EXPIRATION = Duration.ofMinutes(10);

    /**
     * Name to file which contains info about cached items (e.g. when an item was saved).
     */
    static final Key CACHE_FILE = new Key.From("cache/cache-info.json");

    /**
     * Repository.
     */
    private final Repository repo;

    /**
     * Ctor.
     * @param repository Repository
     */
    CacheTimeControl(final Repository repository) {
        this.repo = repository;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key item, final Remote content) {
        return this.repo.value(CacheTimeControl.CACHE_FILE)
            .thenApply(ContentAsJson::new)
            .thenCompose(ContentAsJson::value)
            .thenApply(
                json -> {
                    final String key = item.string();
                    return json.containsKey(key)
                        && CacheTimeControl.notExpired(json.getString(key));
                }
            );
    }

    /**
     * Validate time by comparing difference with time of expiration.
     * @param time Time of uploading
     * @return True is valid as not expired yet, false otherwise.
     */
    private static boolean notExpired(final String time) {
        return !Duration.between(
            Instant.now().atZone(ZoneOffset.UTC),
            ZonedDateTime.parse(time)
        ).plus(CacheTimeControl.EXPIRATION)
        .isNegative();
    }
}
