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

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepository#packages()} and {@link AstoRepository#packages(Name)}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class AstoRepositoryPackagesTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void shouldLoadEmptyPackages() {
        final Name name = new Name("foo/bar");
        MatcherAssert.assertThat(
            new AstoRepository(this.storage).packages(name)
                .toCompletableFuture().join()
                .isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldLoadNonEmptyPackages() throws Exception {
        final Name name = new Name("foo/bar2");
        final byte[] bytes = "some data".getBytes();
        new BlockingStorage(this.storage).save(name.key(), bytes);
        new AstoRepository(this.storage).packages(name).toCompletableFuture().join().get()
            .save(this.storage, name.key())
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(name.key()),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void shouldLoadEmptyAllPackages() {
        MatcherAssert.assertThat(
            new AstoRepository(this.storage).packages().toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldLoadNonEmptyAllPackages() throws Exception {
        final byte[] bytes = "all packages".getBytes();
        new BlockingStorage(this.storage).save(new AllPackages(), bytes);
        new AstoRepository(this.storage).packages().toCompletableFuture().join().get()
            .save(this.storage, new AllPackages())
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(new AllPackages()),
            new IsEqual<>(bytes)
        );
    }
}
