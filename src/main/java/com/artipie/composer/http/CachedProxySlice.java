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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.KeyFromPath;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * Composer proxy slice with cache support.
 * @since 0.4
 */
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
        final RequestLineFrom req = new RequestLineFrom(line);
        final Key key = new KeyFromPath(req.uri().getPath());
        return new AsyncResponse(
            this.cache.load(
                key,
                new Remote.WithErrorHandling(
                    () -> {
                        final CompletableFuture<Optional<? extends Content>> promise;
                        promise = new CompletableFuture<>();
                        this.origin.response(line, Headers.EMPTY, Content.EMPTY).send(
                            (rsstatus, rsheaders, rsbody) -> {
                                final CompletableFuture<Void> term = new CompletableFuture<>();
                                if (rsstatus.success()) {
                                    final Flowable<ByteBuffer> flow = Flowable.fromPublisher(rsbody)
                                        .doOnError(term::completeExceptionally)
                                        .doOnTerminate(() -> term.complete(null));
                                    promise.complete(Optional.of(new Content.From(flow)));
                                } else {
                                    promise.complete(Optional.empty());
                                }
                                return term;
                            }
                        );
                        return promise;
                    }
                ),
                CacheControl.Standard.ALWAYS
            ).handle(
                (content, throwable) -> {
                    final CompletableFuture<Response> res = new CompletableFuture<>();
                    if (throwable == null && content.isPresent()) {
                        res.complete(
                            new RsWithBody(
                                StandardRs.OK,
                                content.get()
                            )
                        );
                    } else {
                        res.complete(StandardRs.NOT_FOUND);
                    }
                    return res;
                }
            ).thenCompose(Function.identity())
        );
    }
}
