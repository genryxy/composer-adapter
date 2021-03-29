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
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.Remote;
import com.artipie.composer.JsonPackages;
import com.artipie.composer.Packages;
import com.artipie.composer.Repository;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * Composer proxy slice with cache support.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
final class CachedProxySlice implements Slice {
    /**
     * Remote slice.
     */
    private final Slice remote;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Repository.
     */
    private final Repository repo;

    /**
     * Proxy slice without cache.
     * @param remote Remote slice
     * @param repo Repository
     */
    CachedProxySlice(final Slice remote, final Repository repo) {
        this(remote, repo, Cache.NOP);
    }

    /**
     * Ctor.
     * @param remote Remote slice
     * @param repo Repository
     * @param cache Cache
     */
    CachedProxySlice(final Slice remote, final Repository repo, final Cache cache) {
        this.remote = remote;
        this.cache = cache;
        this.repo = repo;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String name = new RequestLineFrom(line)
            .uri().getPath().replaceAll("^/p2?/", "")
            .replaceAll("~.*", "")
            .replaceAll("\\^.*", "")
            .replaceAll(".json$", "");
        return new AsyncResponse(
            this.repo.packages().thenApply(
                pckgs -> pckgs.orElse(new JsonPackages())
            ).thenCompose(Packages::content)
            .thenCombine(
                this.packageFromRemote(line),
                (lcl, rmt) -> new MergePackage.WithRemote(name, lcl).merge(rmt)
            ).thenCompose(Function.identity())
            .handle(
                (pkgs, throwable) -> {
                    final CompletableFuture<Response> res = new CompletableFuture<>();
                    if (throwable == null && pkgs.isPresent()) {
                        res.complete(
                            new RsWithBody(
                                StandardRs.OK,
                                pkgs.get()
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

    /**
     * Obtains info about package from remote.
     * @param line The request line (usually like this `GET /p2/vendor/package.json HTTP_1_1`)
     * @return Content from respond of remote. If there were some errors,
     *  empty will be returned.
     */
    private CompletionStage<Optional<? extends Content>> packageFromRemote(final String line) {
        return new Remote.WithErrorHandling(
            () -> {
                final CompletableFuture<Optional<? extends Content>> promise;
                promise = new CompletableFuture<>();
                this.remote.response(line, Headers.EMPTY, Content.EMPTY).send(
                    (rsstatus, rsheaders, rsbody) -> {
                        if (rsstatus.success()) {
                            promise.complete(Optional.of(new Content.From(rsbody)));
                        } else {
                            promise.complete(Optional.empty());
                        }
                        return CompletableFuture.allOf();
                    }
                );
                return promise;
            }
        ).get();
    }
}
