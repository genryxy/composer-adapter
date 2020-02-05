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

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;

/**
 * Java resource read from class loader.
 *
 * @since 0.1
 */
final class Resource {

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param name Resource name.
     */
    Resource(final String name) {
        this.name = name;
    }

    /**
     * Reads binary data.
     *
     * @return Binary data.
     */
    byte[] bytes() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(this.name)) {
            if (stream == null) {
                throw new IllegalArgumentException(
                    String.format("Cannot find resource by name '%s'", this.name)
                );
            }
            return ByteStreams.toByteArray(stream);
        } catch (final IOException ex) {
            throw new IllegalArgumentException(String.format("Failed to read '%s'", this.name), ex);
        }
    }
}
