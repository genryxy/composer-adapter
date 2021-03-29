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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.misc.ContentAsJson;
import java.util.Optional;
import javax.json.JsonObject;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonPackages}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (6 lines)
 */
class JsonPackagesTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Package created from 'minimal-package.json' resource.
     */
    private Package pack;

    /**
     * Package name.
     */
    private Name name;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.pack = new JsonPackage(
            new Content.From(
                new TestResource("minimal-package.json").asBytes()
            )
        );
        this.name = this.pack.name().toCompletableFuture().join();
    }

    @Test
    void shouldSaveEmpty() throws Exception {
        new JsonPackages().save(this.storage, this.name.key())
            .toCompletableFuture().get();
        MatcherAssert.assertThat(
            this.versions(this.json(this.name.key())),
            new IsNull<>()
        );
    }

    @Test
    void shouldSaveNotEmpty() {
        final byte[] pkg = new TestResource("packages.json").asBytes();
        final Key key = this.name.key();
        new JsonPackages(new Content.From(pkg))
            .save(this.storage, key)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(key),
            new IsEqual<>(pkg)
        );
    }

    @Test
    void shouldAddPackageWhenEmpty() {
        final JsonObject json = this.addPackageTo("{\"packages\":{}}");
        MatcherAssert.assertThat(
            this.versions(json).getJsonObject(
                this.pack.version(Optional.empty())
                    .toCompletableFuture().join()
                    .get()
            ),
            new IsNot<>(new IsNull<>())
        );
    }

    @Test
    void shouldAddPackageWhenNotEmpty() {
        final JsonObject json = this.addPackageTo(
            "{\"packages\":{\"vendor/package\":{\"1.1.0\":{}}}}"
        );
        final JsonObject versions = this.versions(json);
        MatcherAssert.assertThat(
            versions.keySet(),
            new IsEqual<>(
                new SetOf<>(
                    "1.1.0",
                    this.pack.version(Optional.empty()).toCompletableFuture().join().get()
                )
            )
        );
    }

    private JsonObject addPackageTo(final String original) {
        final Key key = this.name.key();
        new JsonPackages(new Content.From(original.getBytes()))
            .add(this.pack, Optional.empty())
            .toCompletableFuture().join()
            .save(this.storage, key)
            .toCompletableFuture().join();
        return this.json(key);
    }

    private JsonObject json(final Key key) {
        return new ContentAsJson(
            this.storage.value(key)
                .toCompletableFuture().join()
        ).value().toCompletableFuture().join();
    }

    private JsonObject versions(final JsonObject json) {
        return json.getJsonObject("packages")
            .getJsonObject(this.name.string());
    }
}
