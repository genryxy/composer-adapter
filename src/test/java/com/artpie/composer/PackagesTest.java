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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.google.common.io.ByteSource;
import java.nio.file.Files;
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
    private BlockingStorage storage;

    /**
     * Resource 'packages.json'.
     */
    private Resource resource;

    /**
     * Example packages registry read from 'packages.json'.
     */
    private Packages packages;

    @BeforeEach
    void init() throws Exception {
        this.storage = new BlockingStorage(
            new FileStorage(
                Files.createTempDirectory(PackagesTest.class.getName()).resolve("repo")
            )
        );
        this.resource = new Resource("packages.json");
        this.packages = new Packages(
            new Name("vendor/package"),
            ByteSource.wrap(this.resource.bytes())
        );
    }

    @Test
    void shouldSave() {
        this.packages.save(this.storage);
        final Key.From key = new Key.From("vendor", "package.json");
        MatcherAssert.assertThat(
            this.storage.value(key),
            Matchers.equalTo(this.resource.bytes())
        );
    }
}
