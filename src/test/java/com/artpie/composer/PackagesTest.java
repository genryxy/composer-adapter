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
import com.google.common.io.ByteStreams;
import java.nio.file.Path;
import org.cactoos.io.ResourceOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Packages}.
 *
 * @since 0.1
 */
class PackagesTest {

    /**
     * Resource 'packages.json'.
     */
    private ResourceOf resource;

    /**
     * Example packages registry read from 'packages.json'.
     */
    private Packages packages;

    @BeforeEach
    void init() throws Exception {
        this.resource = new ResourceOf("packages.json");
        this.packages = new Packages(
            new Name("vendor/package"),
            ByteSource.wrap(ByteStreams.toByteArray(this.resource.stream()))
        );
    }

    @Test
    void shouldSave(final @TempDir Path temp) throws Exception {
        final FileStorage storage = new FileStorage(temp);
        this.packages.save(storage).get();
        final Key.From key = new Key.From("vendor", "package.json");
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            Matchers.equalTo(ByteStreams.toByteArray(this.resource.stream()))
        );
    }
}
