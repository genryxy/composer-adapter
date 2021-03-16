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
package com.artipie.composer.http;

import com.artipie.asto.cache.Cache;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.reactivestreams.Publisher;

/**
 * Composer proxy slice with cache support.
 * @since 0.4
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
final class CachedProxySlice implements Slice {
    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Ctor.
     * @param origin Origin slice
     * @param cache Cache
     */
    protected CachedProxySlice(final Slice origin, final Cache cache) {
        this.origin = origin;
        this.cache = cache;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        throw new NotImplementedException("not implemented yet");
    }
}
