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

package com.artpie.composer;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import org.cactoos.io.ResourceOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// @checkstyle ClassDataAbstractionCouplingCheck (6 lines)
/**
 * Tests for {@link Repository}.
 *
 * @since 0.1
 */
class RepositoryTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Package pack;

    @BeforeEach
    void init(final @TempDir Path temp) throws Exception {
        this.storage = new FileStorage(temp);
        this.pack = new JsonPackage(
            ByteSource.wrap(
                ByteStreams.toByteArray(new ResourceOf("minimal-package.json").stream())
            )
        );
    }

    @Test
    void shouldLoadEmptyPackages() throws Exception {
        final Name name = new Name("foo/bar");
        final Packages packages = new Repository(this.storage).packages(name);
        packages.save(this.storage, name.key()).get();
        MatcherAssert.assertThat(
            "Packages loaded when no content is in storage should be empty",
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
            "Packages loaded and saved should preserve content without modification",
            new BlockingStorage(this.storage).value(name.key()),
            new IsEqual<>(bytes)
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
            new IsEqual<>(Collections.singleton(this.pack.version()))
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
            new IsEqual<>(new HashSet<>(Arrays.asList("1.1.0", this.pack.version())))
        );
    }

    private JsonObject packages(final Name name) {
        final JsonObject saved;
        final byte[] bytes = new BlockingStorage(this.storage).value(name.key());
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
