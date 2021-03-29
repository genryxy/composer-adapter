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
import com.artipie.composer.misc.ContentAsJson;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.JsonObject;

/**
 * PHP Composer package built from JSON.
 *
 * @since 0.1
 */
public final class JsonPackage implements Package {

    /**
     * Package binary content.
     */
    private final Content content;

    /**
     * Ctor.
     *
     * @param content Package binary content.
     */
    public JsonPackage(final Content content) {
        this.content = content;
    }

    @Override
    public CompletionStage<Name> name() {
        return this.mandatoryString("name")
            .thenApply(Name::new);
    }

    @Override
    public CompletionStage<Optional<String>> version(final Optional<String> value) {
        final String version = value.orElse(null);
        return this.optString("version")
            .thenApply(opt -> opt.orElse(version))
            .thenApply(Optional::ofNullable);
    }

    @Override
    public CompletionStage<JsonObject> json() {
        return new ContentAsJson(this.content).value();
    }

    /**
     * Reads string value from package JSON root. Throws exception if value not found.
     *
     * @param name Attribute value.
     * @return String value.
     */
    private CompletionStage<String> mandatoryString(final String name) {
        return this.json()
            .thenApply(jsn -> jsn.getString(name))
            .thenCompose(
                val -> {
                    final CompletionStage<String> res;
                    if (val == null) {
                        res = new CompletableFuture<String>()
                            .exceptionally(
                                ignore -> {
                                    throw new IllegalStateException(
                                        String.format("Bad package, no '%s' found.", name)
                                    );
                                }
                        );
                    } else {
                        res = CompletableFuture.completedFuture(val);
                    }
                    return res;
                }
            );
    }

    /**
     * Reads string value from package JSON root. Empty in case of absence.
     * @param name Attribute value
     * @return String value, otherwise empty.
     */
    private CompletionStage<Optional<String>> optString(final String name) {
        return this.json()
            .thenApply(json -> json.getString(name, null))
            .thenApply(Optional::ofNullable);
    }
}
