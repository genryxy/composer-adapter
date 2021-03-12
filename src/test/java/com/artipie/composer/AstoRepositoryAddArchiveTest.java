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
import com.artipie.composer.http.Archive;
import com.artipie.composer.misc.ContentAsJson;
import javax.json.JsonObject;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepository#addArchive(Archive, Content)}.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AstoRepositoryAddArchiveTest {
    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Content archive;

    /**
     * Archive name.
     */
    private Archive.Name name;

    @BeforeEach
    void init() {
        final String zip = "log-1.1.3.zip";
        this.storage = new InMemoryStorage();
        this.archive = new Content.From(
            new TestResource(zip).asBytes()
        );
        this.name = new Archive.Name(zip, "1.1.3");
    }

    @Test
    void shouldAddPackageToAll() {
        this.saveZipArchive();
        MatcherAssert.assertThat(
            this.packages(new AllPackages())
                .getJsonObject("psr/log")
                .keySet(),
            new IsEqual<>(new SetOf<>(this.name.version()))
        );
    }

    @Test
    void shouldAddPackageToAllWhenOtherVersionExists() {
        new BlockingStorage(this.storage).save(
            new AllPackages(),
            "{\"packages\":{\"psr/log\":{\"1.1.2\":{}}}}".getBytes()
        );
        this.saveZipArchive();
        MatcherAssert.assertThat(
            this.packages(new AllPackages())
                .getJsonObject("psr/log")
                .keySet(),
            new IsEqual<>(new SetOf<>("1.1.2", this.name.version()))
        );
    }

    @Test
    void shouldAddArchive() {
        this.saveZipArchive();
        MatcherAssert.assertThat(
            this.storage.exists(new Key.From("artifacts", this.name.full()))
                .toCompletableFuture()
                .join(),
            new IsEqual<>(true)
        );
    }

    private void saveZipArchive() {
        new AstoRepository(this.storage)
            .addArchive(
                new Archive.Zip(this.name),
                this.archive
            ).join();
    }

    private JsonObject packages(final Key key) {
        return this.storage.value(key)
            .thenApply(ContentAsJson::new)
            .thenCompose(ContentAsJson::value)
            .toCompletableFuture().join()
            .getJsonObject("packages");
    }
}
