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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepository#add(Content)}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class AstoRepositoryAddTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Package pack;

    /**
     * Version of package.
     */
    private String version;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.pack = new JsonPackage(
            new Content.From(
                new TestResource("minimal-package.json").asBytes()
            )
        );
        this.version = this.pack.version().toCompletableFuture().join();
    }

    @Test
    void shouldAddPackageToAll() throws Exception {
        new AstoRepository(this.storage).add(this.packageJson()).get();
        final Name name = this.pack.name()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.packages().getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>(this.version))
        );
    }

    @Test
    void shouldAddPackageToAllWhenOtherVersionExists() throws Exception {
        new BlockingStorage(this.storage).save(
            new AllPackages(),
            "{\"packages\":{\"vendor/package\":{\"2.0\":{}}}}".getBytes()
        );
        new AstoRepository(this.storage).add(this.packageJson()).get();
        MatcherAssert.assertThat(
            this.packages().getJsonObject("vendor/package").keySet(),
            new IsEqual<>(new SetOf<>("2.0", this.version))
        );
    }

    @Test
    void shouldAddPackage() throws Exception {
        new AstoRepository(this.storage).add(this.packageJson()).get();
        final Name name = this.pack.name()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Package with correct version should present in packages after being added",
            this.packages(name).getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>(this.version))
        );
    }

    @Test
    void shouldAddPackageWhenOtherVersionExists() throws Exception {
        final Name name = this.pack.name()
            .toCompletableFuture().join();
        new BlockingStorage(this.storage).save(
            name.key(),
            "{\"packages\":{\"vendor/package\":{\"1.1.0\":{}}}}".getBytes()
        );
        new AstoRepository(this.storage).add(this.packageJson()).get();
        MatcherAssert.assertThat(
            // @checkstyle LineLengthCheck (1 line)
            "Package with both new and old versions should present in packages after adding new version",
            this.packages(name).getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>("1.1.0", this.version))
        );
    }

    @Test
    void shouldDeleteSourceAfterAdding() throws Exception {
        new AstoRepository(this.storage).add(this.packageJson()).get();
        MatcherAssert.assertThat(
            this.storage.list(Key.ROOT).join().stream()
                .map(Key::string)
                .collect(Collectors.toList()),
            Matchers.contains("packages.json", "vendor/package.json")
        );
    }

    private JsonObject packages() {
        return this.packages(new AllPackages());
    }

    private JsonObject packages(final Name name) {
        return this.packages(name.key());
    }

    private JsonObject packages(final Key key) {
        final JsonObject saved;
        final byte[] bytes = new BlockingStorage(this.storage).value(key);
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            saved = reader.readObject();
        }
        return saved.getJsonObject("packages");
    }

    private Content packageJson() throws Exception {
        final byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonWriter writer = Json.createWriter(out)) {
            writer.writeObject(this.pack.json().toCompletableFuture().join());
            out.flush();
            bytes = out.toByteArray();
        }
        return new Content.From(bytes);
    }
}
