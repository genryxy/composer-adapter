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
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link CacheTimeControl}.
 * @since 0.4
 */
final class CacheTimeControlTest {
    @ParameterizedTest
    @CsvSource({"1,true", "12,false"})
    void verifiesTimeValueCorrectly(final long minutes, final boolean valid) {
        final Storage storage = new InMemoryStorage();
        final String pkg = "vendor/package";
        new BlockingStorage(storage).save(
            CacheTimeControl.CACHE_FILE,
            Json.createObjectBuilder()
                .add(
                    pkg,
                    ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneOffset.UTC
                    ).minusMinutes(minutes).toString()
                ).build().toString().getBytes()
        );
        MatcherAssert.assertThat(
            new CacheTimeControl(new AstoRepository(storage))
                .validate(new Key.From(pkg), Remote.EMPTY)
                .toCompletableFuture().join(),
            new IsEqual<>(valid)
        );
    }
}
