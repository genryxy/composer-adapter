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
import java.io.IOException;
import java.nio.file.Files;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.cactoos.io.ResourceOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Packages}.
 *
 * @since 0.1
 */
class PackagesTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Package created from 'minimal-package.json' resource.
     */
    private Package pack;

    @BeforeEach
    void init() throws Exception {
        this.storage = new FileStorage(
            Files.createTempDirectory(PackagesTest.class.getName()).resolve("repo")
        );
        this.pack = new Package(
            ByteSource.wrap(
                ByteStreams.toByteArray(new ResourceOf("minimal-package.json").stream())
            )
        );
    }

    @Test
    void shouldSave() throws Exception {
        final ResourceOf resource = new ResourceOf("packages.json");
        new Packages(
            this.pack.name(),
            ByteSource.wrap(
                ByteStreams.toByteArray(resource.stream())
            )
        ).save(this.storage).get();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(new Key.From("vendor", "package.json")),
            Matchers.equalTo(ByteStreams.toByteArray(resource.stream()))
        );
    }

    @Test
    void shouldAddPackageWhenEmpty() throws Exception {
        final JsonObject json = this.addPackageTo("{\"packages\":{}}");
        MatcherAssert.assertThat(
            this.versions(json).getJsonObject(this.pack.version()),
            Matchers.notNullValue()
        );
    }

    @Test
    void shouldAddPackageWhenNotEmpty() throws Exception {
        final JsonObject json = this.addPackageTo(
            "{\"packages\":{\"vendor/package\":{\"1.1.0\":{}}}}"
        );
        final JsonObject versions = this.versions(json);
        MatcherAssert.assertThat(
            versions.getJsonObject("1.1.0"),
            Matchers.notNullValue()
        );
        MatcherAssert.assertThat(
            versions.getJsonObject(this.pack.version()),
            Matchers.notNullValue()
        );
    }

    private JsonObject addPackageTo(final String original) throws Exception {
        new Packages(this.pack.name(), ByteSource.wrap(original.getBytes()))
            .add(this.pack)
            .save(this.storage)
            .get();
        final byte[] bytes = new BlockingStorage(this.storage).value(this.pack.name().key());
        final JsonObject json;
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            json = reader.readObject();
        }
        return json;
    }

    private JsonObject versions(final JsonObject json) throws IOException {
        return json.getJsonObject("packages").getJsonObject(this.pack.name().string());
    }
}
