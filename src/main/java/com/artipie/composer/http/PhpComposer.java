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

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * PHP Composer repository HTTP front end.
 *
 * @since 0.1
 */
public final class PhpComposer implements Slice {

    /**
     * Storage for packages.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage for packages.
     */
    public PhpComposer(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response response;
        final RequestLineFrom request = new RequestLineFrom(line);
        final String path = request.uri().getPath();
        final Resource resource = this.resource(path);
        final RqMethod method = request.method();
        if (method.equals(RqMethod.GET)) {
            response = resource.get();
        } else if (method.equals(RqMethod.PUT)) {
            response = resource.put(body);
        } else {
            response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }
        return response;
    }

    /**
     * Find resource by relative path.
     *
     * @param path Relative path.
     * @return Resource found by path.
     */
    private Resource resource(final String path) {
        final Resource resource;
        if (path.equals("/")) {
            resource = new Root(this.storage);
        } else {
            resource = new PackageMetadata(path, this.storage);
        }
        return resource;
    }
}
