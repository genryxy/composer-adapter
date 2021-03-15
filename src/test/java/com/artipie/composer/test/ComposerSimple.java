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
package com.artipie.composer.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple sample of composer for using in tests.
 * @since 0.4
 */
public final class ComposerSimple {
    /**
     * Repository url.
     */
    private final String url;

    /**
     * Package name.
     */
    private final String pkg;

    /**
     * Version of package for uploading.
     */
    private final String vers;

    /**
     * Ctor with default value for package name and version.
     * @param url Repository url
     */
    public ComposerSimple(final String url) {
        this(url, "vendor/package", "1.1.2");
    }

    /**
     * Ctor.
     * @param url Repository url
     * @param pkg Package name
     * @param vers Version of package for uploading
     */
    public ComposerSimple(final String url, final String pkg, final String vers) {
        this.url = url;
        this.pkg = pkg;
        this.vers = vers;
    }

    /**
     * Write composer to specified path.
     * @param path Path to save
     * @throws IOException In case of failure with writing.
     */
    public void writeTo(final Path path) throws IOException {
        Files.write(path, this.value());
    }

    /**
     * Bytes with composer json.
     * @return Composer sample.
     */
    private byte[] value() {
        return String.join(
            "",
            "{",
            "\"config\":{ \"secure-http\": false },",
            "\"repositories\": [",
            String.format("{\"type\": \"composer\", \"url\": \"%s\"},", this.url),
            "{\"packagist.org\": false} ",
            "],",
            String.format("\"require\": { \"%s\": \"%s\" }", this.pkg, this.vers),
            "}"
        ).getBytes();
    }
}
