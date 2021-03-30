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

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Empty ZIP archive for using in tests.
 * @since 0.4
 */
public final class EmptyZip {
    /**
     * Entry name. As archive should contains whatever.
     */
    private final String entry;

    /**
     * Ctor.
     */
    public EmptyZip() {
        this("whatever");
    }

    /**
     * Ctor.
     * @param entry Entry name
     */
    public EmptyZip(final String entry) {
        this.entry = entry;
    }

    /**
     * Obtains ZIP archive.
     * @return ZIP archive
     * @throws Exception In case of error during creating ZIP archive
     */
    public byte[] value() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ZipOutputStream zos = new ZipOutputStream(bos);
        zos.putNextEntry(new ZipEntry(this.entry));
        zos.close();
        return bos.toByteArray();
    }
}
