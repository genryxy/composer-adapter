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
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import org.cactoos.io.ResourceOf;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Repository#add(Key)}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class RepositoryAddTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Package pack;

    @BeforeEach
    void init() throws Exception {
        this.storage = new InMemoryStorage();
        this.pack = new JsonPackage(
            ByteSource.wrap(
                ByteStreams.toByteArray(new ResourceOf("minimal-package.json").stream())
            )
        );
    }

    @Test
    void shouldAddPackageToAll() throws Exception {
        new Repository(this.storage).add(this.savePackage()).get();
        final Name name = this.pack.name();
        MatcherAssert.assertThat(
            this.packages().getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>(this.pack.version()))
        );
    }

    @Test
    void shouldAddPackageToAllWhenOtherVersionExists() throws Exception {
        new BlockingStorage(this.storage).save(
            new AllPackages(),
            "{\"packages\":{\"vendor/package\":{\"2.0\":{}}}}".getBytes()
        );
        final Key.From key = this.savePackage();
        new Repository(this.storage).add(key).get();
        MatcherAssert.assertThat(
            this.packages().getJsonObject("vendor/package").keySet(),
            new IsEqual<>(new SetOf<>("2.0", this.pack.version()))
        );
    }

    @Test
    void shouldAddPackage() throws Exception {
        final Key.From key = this.savePackage();
        new Repository(this.storage).add(key).get();
        final Name name = this.pack.name();
        MatcherAssert.assertThat(
            "Package with correct version should present in packages after being added",
            this.packages(name).getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>(this.pack.version()))
        );
    }

    @Test
    void shouldAddPackageWhenOtherVersionExists() throws Exception {
        final Name name = this.pack.name();
        new BlockingStorage(this.storage).save(
            name.key(),
            "{\"packages\":{\"vendor/package\":{\"1.1.0\":{}}}}".getBytes()
        );
        final Key.From key = this.savePackage();
        new Repository(this.storage).add(key).get();
        MatcherAssert.assertThat(
            // @checkstyle LineLengthCheck (1 line)
            "Package with both new and old versions should present in packages after adding new version",
            this.packages(name).getJsonObject(name.string()).keySet(),
            new IsEqual<>(new SetOf<>("1.1.0", this.pack.version()))
        );
    }

    @Test
    void shouldDeleteSourceAfterAdding() throws Exception {
        final Key.From source = this.savePackage();
        new Repository(this.storage).add(source).get();
        MatcherAssert.assertThat(
            this.storage.exists(source).get(),
            new IsEqual<>(false)
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

    private Key.From savePackage() throws IOException {
        final byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonWriter writer = Json.createWriter(out)) {
            writer.writeObject(this.pack.json());
            out.flush();
            bytes = out.toByteArray();
        }
        final Key.From key = new Key.From("pack");
        new BlockingStorage(this.storage).save(key, bytes);
        return key;
    }
}
