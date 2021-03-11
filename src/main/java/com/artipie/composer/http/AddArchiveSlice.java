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
import com.artipie.composer.Repository;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice for adding a package to the repository in ZIP format.
 * See <a href="https://getcomposer.org/doc/05-repositories.md#artifact">Artifact repository</a>.
 * @since 0.4
 */
@SuppressWarnings({"PMD.SingularField", "PMD.UnusedPrivateField"})
final class AddArchiveSlice implements Slice {
    /**
     * Composer HTTP for entry point.
     * See <a href="https://getcomposer.org/doc/04-schema.md#version">docs</a>.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/(?<full>(?<name>[a-z0-9_.\\-]*)-(?<version>v?\\d+.\\d+.\\d+[-\\w]*).zip)$"
    );

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Ctor.
     * @param repository Repository.
     */
    AddArchiveSlice(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rqline = new RequestLineFrom(line);
        final String uri = rqline.uri().getPath();
        final Matcher matcher = AddArchiveSlice.PATH.matcher(uri);
        final Response resp;
        if (matcher.matches()) {
            final Content content = new Content.From(body);
            resp = new AsyncResponse(
                this.repository
                    .addArchive(
                        new Archive.Zip(
                            new Archive.Name(matcher.group("full"), matcher.group("version")),
                            content
                        ),
                        content
                    )
                    .thenApply(nothing -> new RsWithStatus(RsStatus.CREATED))
            );
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
