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

import com.artipie.composer.Repository;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice for uploading archive by key from storage.
 * @since 0.4
 */
final class DownloadArchiveSlice implements Slice {
    /**
     * Repository.
     */
    private final Repository repos;

    /**
     * Slice by key from storage.
     * @param repository Repository
     */
    DownloadArchiveSlice(final Repository repository) {
        this.repos = repository;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String path = new RequestLineFrom(line).uri().getPath();
        return new AsyncResponse(
            this.repos.value(new KeyFromPath(path))
                .thenApply(RsWithBody::new)
                .thenApply(rsp -> new RsWithStatus(rsp, RsStatus.OK))
        );
    }
}
