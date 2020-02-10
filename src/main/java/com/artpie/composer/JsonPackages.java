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

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.google.common.io.ByteSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

/**
 * PHP Composer packages registry built from JSON.
 *
 * @since 0.1
 */
public final class JsonPackages implements Packages {

    /**
     * Root attribute value for packages registry in JSON.
     */
    private static final String ATTRIBUTE = "packages";

    /**
     * Package name.
     */
    private final Name name;

    /**
     * Packages registry content.
     */
    private final ByteSource content;

    /**
     * Ctor.
     *
     * @param name Package name.
     * @param content Packages registry content.
     */
    public JsonPackages(final Name name, final ByteSource content) {
        this.name = name;
        this.content = content;
    }

    @Override
    public Packages add(final Package pack) throws IOException {
        final JsonObject json = this.json();
        if (json.isNull(JsonPackages.ATTRIBUTE)) {
            throw new IllegalStateException("Bad content, no 'packages' object found");
        }
        final JsonObject packages = json.getJsonObject(JsonPackages.ATTRIBUTE);
        final String pname = pack.name().string();
        final JsonObjectBuilder builder;
        if (packages.isNull(pname)) {
            builder = Json.createObjectBuilder();
        } else {
            builder = Json.createObjectBuilder(packages.getJsonObject(pname));
        }
        builder.add(pack.version(), pack.json());
        return new JsonPackages(
            this.name,
            bytes(
                Json.createObjectBuilder(json)
                    .add(
                        JsonPackages.ATTRIBUTE,
                        Json.createObjectBuilder(packages).add(pname, builder)
                    )
                    .build()
            )
        );
    }

    @Override
    public CompletableFuture<Void> save(final Storage storage) {
        return CompletableFuture.runAsync(() -> this.save(new BlockingStorage(storage)));
    }

    /**
     * Saves packages registry binary content to storage.
     *
     * @param storage Storage to use for saving.
     */
    private void save(final BlockingStorage storage) {
        final byte[] bytes;
        try {
            bytes = this.content.read();
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read content", ex);
        }
        storage.save(this.name.key(), bytes);
    }

    /**
     * Reads content as JSON object.
     *
     * @return JSON object.
     * @throws IOException In case exception occurred on reading content.
     */
    private JsonObject json() throws IOException {
        try (JsonReader reader = Json.createReader(this.content.openStream())) {
            return reader.readObject();
        }
    }

    /**
     * Serializes JSON object into bytes.
     *
     * @param json JSON object.
     * @return Serialized JSON object.
     * @throws IOException In case of any I/O problems.
     */
    private static ByteSource bytes(final JsonObject json) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonWriter writer = Json.createWriter(out)) {
            writer.writeObject(json);
            out.flush();
            return ByteSource.wrap(out.toByteArray());
        }
    }

}
