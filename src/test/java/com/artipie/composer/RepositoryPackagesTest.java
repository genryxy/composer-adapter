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
import com.artipie.asto.memory.InMemoryStorage;
import java.io.ByteArrayInputStream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Repository#packages()} and {@link Repository#packages(Name)}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class RepositoryPackagesTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void shouldLoadEmptyPackages() throws Exception {
        final Name name = new Name("foo/bar");
        final Packages packages = new Repository(this.storage).packages(name);
        packages.save(this.storage, name.key()).get();
        MatcherAssert.assertThat(
            this.packages(name).keySet(),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void shouldLoadNonEmptyPackages() throws Exception {
        final Name name = new Name("foo/bar2");
        final byte[] bytes = "some data".getBytes();
        new BlockingStorage(this.storage).save(name.key(), bytes);
        final Packages packages = new Repository(this.storage).packages(name);
        packages.save(this.storage, name.key()).get();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(name.key()),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void shouldLoadEmptyAllPackages() throws Exception {
        final Packages packages = new Repository(this.storage).packages();
        packages.save(this.storage, new AllPackages()).get();
        MatcherAssert.assertThat(this.packages().keySet(), new IsEmptyCollection<>());
    }

    @Test
    void shouldLoadNonEmptyAllPackages() throws Exception {
        final byte[] bytes = "all packages".getBytes();
        new BlockingStorage(this.storage).save(new AllPackages(), bytes);
        final Packages packages = new Repository(this.storage).packages();
        packages.save(this.storage, new AllPackages()).get();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(new AllPackages()),
            new IsEqual<>(bytes)
        );
    }

    private JsonObject packages() throws Exception {
        return this.packages(new AllPackages());
    }

    private JsonObject packages(final Name name) throws Exception {
        return this.packages(name.key());
    }

    private JsonObject packages(final Key key) throws Exception {
        final JsonObject saved;
        final byte[] bytes = new BlockingStorage(this.storage).value(key);
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            saved = reader.readObject();
        }
        return saved.getJsonObject("packages");
    }
}
