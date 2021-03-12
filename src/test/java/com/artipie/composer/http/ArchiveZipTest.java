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
import com.artipie.asto.test.TestResource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Archive.Zip}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ArchiveZipTest {
    @Test
    void obtainingComposerJsonWorks() {
        MatcherAssert.assertThat(
            new Archive.Zip(
                new Archive.Name("name", "1.0.1")
            ).composerFrom(new Content.From(new TestResource("log-1.1.3.zip").asBytes()))
            .toCompletableFuture().join()
            .toString(),
            new IsEqual<>(
                String.join(
                    "",
                    "{",
                    "\"name\":\"psr/log\",",
                    "\"description\":\"Common interface for logging libraries\",",
                    "\"keywords\":[\"psr\",\"psr-3\",\"log\"],",
                    "\"homepage\":\"https://github.com/php-fig/log\",",
                    "\"license\":\"MIT\",",
                    "\"authors\":[{\"name\":\"PHP-FIG\",",
                    "\"homepage\":\"http://www.php-fig.org/\"}],",
                    "\"require\":{\"php\":\">=5.3.0\"},",
                    "\"autoload\":{\"psr-4\":{\"Psr\\\\Log\\\\\":\"Psr/Log/\"}},",
                    "\"extra\":{\"branch-alias\":{\"dev-master\":\"1.1.x-dev\"}}",
                    "}"
                )
            )
        );
    }

    @Test
    void replacesComposerWithAnotherOne() {
        final byte[] target = new TestResource("log-composer-with-version.json").asBytes();
        final String full = "log-1.1.3.zip";
        final Archive.Name name = new Archive.Name(full, "1.1.3");
        final Content updarch = new Archive.Zip(name)
            .replaceComposerWith(
                new Content.From(new TestResource(full).asBytes()),
                target
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new Archive.Zip(name)
                .composerFrom(updarch)
                .toCompletableFuture().join()
                .toString()
                .getBytes(StandardCharsets.UTF_8),
            new IsEqual<>(target)
        );
    }

    @Test
    void failsToObtainWhenFileIsAbsent() {
        final Exception exc = Assertions.assertThrows(
            CompletionException.class,
            () -> new Archive.Zip(
                new Archive.Name("some name", "1.0.2")
            ).composerFrom(new Content.From(ArchiveZipTest.emptyZip()))
            .toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            exc.getCause().getMessage(),
            new IsEqual<>("'composer.json' file was not found")
        );
    }

    private static byte[] emptyZip() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ZipOutputStream zos = new ZipOutputStream(bos);
        zos.putNextEntry(new ZipEntry("whatever"));
        zos.close();
        return bos.toByteArray();
    }
}
