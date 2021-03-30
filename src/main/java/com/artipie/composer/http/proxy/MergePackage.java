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
import com.artipie.composer.JsonPackage;
import com.artipie.composer.misc.ContentAsJson;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Merging info about different versions of packages.
 * @since 0.4
 */
public interface MergePackage {
    /**
     * Merges info about package from local packages file with info
     * about package which is obtained from remote package.
     * @param remote Remote data about package. Usually this file is not big because
     *  it contains info about versions for one package.
     * @return Merged data about one package.
     */
    CompletionStage<Optional<Content>> merge(Optional<? extends Content> remote);

    /**
     * Merging local data with data from remote.
     * @since 0.4
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    class WithRemote implements MergePackage {
        /**
         * Package name.
         */
        private final String name;

        /**
         * Data from local `packages.json` file.
         */
        private final Content local;

        /**
         * Ctor.
         * @param name Package name
         * @param local Data from local `packages.json` file
         */
        WithRemote(final String name, final Content local) {
            this.name = name;
            this.local = local;
        }

        @Override
        public CompletionStage<Optional<Content>> merge(
            final Optional<? extends Content> remote
        ) {
            return WithRemote.packageFrom(this.local)
                .thenApply(this::packageByNameFrom)
                .thenCombine(
                    WithRemote.packageFromOpt(remote),
                    (lcl, rmt) -> {
                        final JsonObjectBuilder bldr = Json.createObjectBuilder(lcl);
                        final Set<String> vrsns = lcl.keySet();
                        rmt.ifPresent(
                            json -> json.getJsonArray(this.name).stream()
                                .map(JsonValue::asJsonObject)
                                .forEach(
                                    entry -> {
                                        final String vers = entry.getString(JsonPackage.VRSN);
                                        if (!vrsns.contains(vers)) {
                                            final JsonObjectBuilder rmtblbdr;
                                            rmtblbdr = Json.createObjectBuilder(entry);
                                            if (!entry.containsKey("name")) {
                                                rmtblbdr.add("name", this.name);
                                            }
                                            rmtblbdr.add("uid", UUID.randomUUID().toString());
                                            bldr.add(vers, rmtblbdr.build());
                                        }
                                    }
                                )
                        );
                        final JsonObject builded = bldr.build();
                        final Optional<Content> res;
                        if (builded.keySet().isEmpty()) {
                            res = Optional.empty();
                        } else {
                            res = Optional.of(
                                new Content.From(
                                    Json.createObjectBuilder().add(
                                        "packages", Json.createObjectBuilder().add(
                                            this.name, builded
                                        ).build()
                                    ).build()
                                    .toString()
                                    .getBytes(StandardCharsets.UTF_8)
                                )
                            );
                        }
                        return res;
                    }
                );
        }

        /**
         * Obtains `packages` entry from file.
         * @param pkgs Content of `package.json` file
         * @return Packages entry from file.
         */
        private static CompletionStage<JsonObject> packageFrom(final Content pkgs) {
            return new ContentAsJson(pkgs).value()
                .thenApply(json -> json.getJsonObject("packages"));
        }

        /**
         * Obtains `packages` entry from file.
         * @param pkgs Optional content of `package.json` file
         * @return Packages entry from file if content is presented, otherwise empty..
         */
        private static CompletionStage<Optional<JsonObject>> packageFromOpt(
            final Optional<? extends Content> pkgs
        ) {
            final CompletionStage<Optional<JsonObject>> res;
            if (pkgs.isPresent()) {
                res = WithRemote.packageFrom(pkgs.get())
                    .thenApply(Optional::of);
            } else {
                res = CompletableFuture.completedFuture(Optional.empty());
            }
            return res;
        }

        /**
         * Obtains info about one package.
         * @param json Json object for `packages` entry
         * @return Info about one package. If passed json does not
         *  contain package, empty json will be returned.
         */
        private JsonObject packageByNameFrom(final JsonObject json) {
            final JsonObject res;
            if (json.containsKey(this.name)) {
                res = json.getJsonObject(this.name);
            } else {
                res = Json.createObjectBuilder().build();
            }
            return res;
        }
    }
}
