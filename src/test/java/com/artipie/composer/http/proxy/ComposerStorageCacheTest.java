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
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.Repository;
import com.artipie.composer.misc.ContentAsJson;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComposerStorageCache}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ComposerStorageCacheTest {
    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Repository.
     */
    private Repository repo;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.repo = new AstoRepository(this.storage);
    }

    @Test
    void getsContentFromRemoteCachesItAndSaveKeyToCacheFile() {
        final byte[] body = "some info".getBytes();
        final String key = "vendor/package";
        MatcherAssert.assertThat(
            "Content was not obtained from remote",
            new PublisherAs(
                new ComposerStorageCache(this.repo).load(
                    new Key.From(key),
                    () -> CompletableFuture.completedFuture(Optional.of(new Content.From(body))),
                    CacheControl.Standard.ALWAYS
                ).toCompletableFuture().join().get()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat(
            "Item was not cached",
            this.storage.exists(
                new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key))
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Info about save time was not saved in cache file",
            new ContentAsJson(
                this.storage.value(CacheTimeControl.CACHE_FILE).join()
            ).value().toCompletableFuture().join()
            .keySet(),
            new IsEqual<>(new SetOf<>(key))
        );
    }

    @Test
    void getsContentFromCache() {
        final byte[] body = "some info".getBytes();
        final String key = "vendor/package";
        this.saveCacheFile(key);
        this.storage.save(
            new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key)),
            new Content.From(body)
        ).join();
        MatcherAssert.assertThat(
            "Content was not obtained from cache",
            new PublisherAs(
                new ComposerStorageCache(this.repo).load(
                    new Key.From(key),
                    () -> CompletableFuture.completedFuture(Optional.empty()),
                    new CacheTimeControl(this.storage)
                ).toCompletableFuture().join().get()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(body)
        );
    }

    @Test
    void getsContentFromRemoteForExpiredCacheAndOverwriteValues() {
        final byte[] body = "some info".getBytes();
        final byte[] updated = "updated some info".getBytes();
        final String key = "vendor/package";
        // @checkstyle MagicNumberCheck (2 line)
        final String expired = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            .minus(Duration.ofDays(100))
            .toString();
        this.saveCacheFile(key, expired);
        this.storage.save(
            new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key)),
            new Content.From(body)
        ).join();
        MatcherAssert.assertThat(
            "Content was not obtained from remote when cache is expired",
            new PublisherAs(
                new ComposerStorageCache(this.repo).load(
                    new Key.From(key),
                    () -> CompletableFuture.completedFuture(Optional.of(new Content.From(updated))),
                    new CacheTimeControl(this.storage)
                ).toCompletableFuture().join().get()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(updated)
        );
        MatcherAssert.assertThat(
            "Info about save time was not updated in cache file",
            new ContentAsJson(
                this.storage.value(CacheTimeControl.CACHE_FILE).join()
            ).value().toCompletableFuture().join()
            .getString(key),
            new IsNot<>(new IsEqual<>(expired))
        );
        MatcherAssert.assertThat(
            "Cached item was not overwritten",
            new PublisherAs(
                this.storage.value(
                    new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key))
                ).join()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(updated)
        );
    }

    @Test
    void returnsNotFoundOnRemoteErrorAndEmptyCache() {
        MatcherAssert.assertThat(
            "Was not empty for remote error and empty cache",
            new ComposerStorageCache(this.repo).load(
                new Key.From("anykey"),
                new Remote.WithErrorHandling(
                    () -> new FailedCompletionStage<>(
                        new IllegalStateException("Failed to obtain item from cache")
                    )
                ),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().join()
            .isPresent(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Cache storage is not empty",
            this.storage.list(Key.ROOT)
                .join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    private void saveCacheFile(final String key) {
        this.saveCacheFile(
            key,
            ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toString()
        );
    }

    private void saveCacheFile(final String key, final String expiration) {
        this.storage.save(
            CacheTimeControl.CACHE_FILE,
            new Content.From(
                Json.createObjectBuilder().add(key, expiration)
                    .build().toString()
                    .getBytes()
            )
        ).join();
    }
}
