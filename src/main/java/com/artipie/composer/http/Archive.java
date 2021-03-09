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
import com.artipie.asto.ext.PublisherAs;
import com.artipie.composer.misc.ContentAsJson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletionStage;
import javax.json.JsonObject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Interface for working with archive file. For example, obtaining
 * composer json file from archive.
 * @since 0.4
 */
public interface Archive {
    /**
     * Obtains composer json file from archive.
     * @return Composer json file from archive.
     */
    CompletionStage<JsonObject> composer();

    /**
     * Archive in ZIP format.
     * @since 0.4
     */
    class Zip implements Archive {
        /**
         * Content of ZIP archive.
         */
        private final Content content;

        /**
         * Ctor.
         * @param content Content of ZIP archive
         */
        public Zip(final Content content) {
            this.content = content;
        }

        @Override
        public CompletionStage<JsonObject> composer() {
            return this.file("composer.json")
                .thenApply(ContentAsJson::new)
                .thenCompose(ContentAsJson::value);
        }

        /**
         * Obtain file by name.
         * @param name The name of a file
         * @return The file content.
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private CompletionStage<Content> file(final String name) {
            return new PublisherAs(this.content).bytes()
                .thenApply(
                    bytes -> {
                        try (
                            ZipArchiveInputStream zip = new ZipArchiveInputStream(
                                new ByteArrayInputStream(bytes)
                            )
                        ) {
                            ArchiveEntry entry;
                            while ((entry = zip.getNextZipEntry()) != null) {
                                final String[] parts = entry.getName().split("/");
                                if (parts[parts.length - 1].equals(name)) {
                                    return new Content.From(IOUtils.toByteArray(zip));
                                }
                            }
                            throw new IllegalStateException(
                                String.format("'%s' file was not found", name)
                            );
                        } catch (final IOException exc) {
                            throw new UncheckedIOException(exc);
                        }
                    }
            );
        }
    }
}
