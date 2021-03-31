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
import com.artipie.asto.test.TestResource;
import com.artipie.composer.misc.ContentAsJson;
import java.util.Optional;
import javax.json.JsonObject;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link MergePackage.WithRemote}.
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MergePackageWithRemoteTest {
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"packages\":{}}"})
    void returnsEmptyWhenLocalAndRemoteNotContainPackage(final String content) {
        final byte[] pkgs = content.getBytes();
        MatcherAssert.assertThat(
            this.mergedContent("not/exist", pkgs, pkgs).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsFromRemoteForEmptyLocal() {
        final String name = "psr/log";
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        final JsonObject pkgs = this.pkgsFromMerged(name, "{}".getBytes(), remote);
        MatcherAssert.assertThat(
            "Contains required package name",
            pkgs.keySet(),
            new IsEqual<>(new SetOf<>(name))
        );
        MatcherAssert.assertThat(
            "Contains all versions",
            pkgs.getJsonObject(name).keySet().toArray(),
            Matchers.arrayContainingInAnyOrder("1.1.2", "1.1.3")
        );
    }

    @Test
    void emptyWhenRemoteDoesNotContainsNameAndLocalIsEmpty() {
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        MatcherAssert.assertThat(
            this.mergedContent("not/exist", "{}".getBytes(), remote).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void mergesLocalWithRemote() {
        final String name = "psr/log";
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        final byte[] local = new TestResource("merge/local.json").asBytes();
        final JsonObject pkgs = this.pkgsFromMerged(name, local, remote);
        MatcherAssert.assertThat(
            "Contains required package name",
            pkgs.keySet(),
            new IsEqual<>(new SetOf<>(name))
        );
        MatcherAssert.assertThat(
            "Contains all versions",
            pkgs.getJsonObject(name).keySet(),
            Matchers.containsInAnyOrder("1.1.3", "1.1.4", "1.1.2")
        );
        for (final String vrsn: pkgs.getJsonObject(name).keySet()) {
            MatcherAssert.assertThat(
                "Each entry contains required fields",
                pkgs.getJsonObject(name)
                    .getJsonObject(vrsn)
                    .keySet(),
                Matchers.hasItems("version", "name", "dist", "uid")
            );
        }
    }

    @Test
    void returnsFromLocalForEmptyRemote() {
        final String name = "psr/log";
        final byte[] local = new TestResource("merge/local.json").asBytes();
        MatcherAssert.assertThat(
            this.pkgsFromMerged(name, local, "{}".getBytes()).keySet(),
            new IsEqual<>(new SetOf<>(name))
        );
    }

    @Test
    void returnsEmptyWhenLocalAndRemoteContainAnotherPackage() {
        final byte[] local = new TestResource("merge/local.json").asBytes();
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        MatcherAssert.assertThat(
            this.mergedContent("not/exist", local, remote).isPresent(),
            new IsEqual<>(false)
        );
    }

    private Optional<Content> mergedContent(
        final String name, final byte[] local, final byte[] remote
    ) {
        return new MergePackage.WithRemote(name, new Content.From(local))
            .merge(
                Optional.of(new Content.From(remote))
            ).toCompletableFuture().join();
    }

    private JsonObject pkgsFromMerged(final String name, final byte[] local, final byte[] remote) {
        return new ContentAsJson(
            this.mergedContent(name, local, remote).get()
        ).value()
        .toCompletableFuture().join()
        .getJsonObject("packages");
    }
}
