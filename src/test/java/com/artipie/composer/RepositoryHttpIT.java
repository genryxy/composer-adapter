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

import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for PHP Composer repository.
 *
 * @since 0.1
 */
class RepositoryHttpIT {

    // @checkstyle VisibilityModifierCheck (5 lines)
    /**
     * Temporary directory.
     */
    @TempDir
    Path temp;

    /**
     * Path to PHP project directory.
     */
    private Path project;

    @BeforeEach
    void setUp() throws Exception {
        this.project = this.temp.resolve("project");
        this.project.toFile().mkdirs();
        this.ensureComposerInstalled();
    }

    @Test
    @Disabled("Not implemented")
    void shouldInstallAddedPackage() throws Exception {
        Files.write(
            this.project.resolve("composer.json"),
            String.join(
                "",
                "{",
                "\"config\":{ \"secure-http\": false },",
                "\"repositories\": [",
                "{\"type\": \"composer\", \"url\": \"http://localhost:8080/myrepo/\"},",
                "{\"packagist.org\": false} ",
                "],",
                "\"require\": { \"vendor/package\": \"1.1.2\" }",
                "}"
            ).getBytes()
        );
        MatcherAssert.assertThat(
            this.run("install"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(false, "Installs: vendor/package:1.1.2"),
                    new StringContains(false, "100%")
                )
            )
        );
    }

    private void ensureComposerInstalled() throws Exception {
        final String output = this.run("--version");
        if (!output.startsWith("Composer version")) {
            throw new IllegalStateException("Composer not installed");
        }
    }

    private String run(final String... args) throws Exception {
        final Path stdout = this.temp.resolve(
            String.format("%s-stdout.txt", UUID.randomUUID().toString())
        );
        final int code = new ProcessBuilder()
            .directory(this.project.toFile())
            .command(
                ImmutableList.<String>builder()
                    .add("composer")
                    .add(args)
                    .add("--verbose")
                    .add("--no-cache")
                    .build()
            )
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        if (code != 0) {
            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
        }
        return log;
    }
}
