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

package com.artpie.composer;

import com.artipie.asto.Key;

/**
 * Name of package consisting of vendor name and package name "[vendor]/[package]".
 *
 * @since 0.1
 */
public final class Name {

    /**
     * Name string.
     */
    private final String value;

    /**
     * Ctor.
     *
     * @param value Name string.
     */
    public Name(final String value) {
        this.value = value;
    }

    /**
     * Generates key for package in store.
     *
     * @return Key for package in store.
     */
    public Key key() {
        return new Key.From(this.vendorPart(), String.format("%s.json", this.packagePart()));
    }

    /**
     * Generates name string value.
     *
     * @return Name string value.
     */
    public String string() {
        return this.value;
    }

    /**
     * Extracts vendor part from name.
     *
     * @return Vendor part of name.
     */
    private String vendorPart() {
        return this.part(0);
    }

    /**
     * Extracts package part from name.
     *
     * @return Package part of name.
     */
    private String packagePart() {
        return this.part(1);
    }

    /**
     * Extracts part of name by index.
     *
     * @param index Part index.
     * @return Part of name by index.
     */
    private String part(final int index) {
        final String[] parts = this.value.split("/");
        if (parts.length != 2) {
            throw new IllegalStateException(
                String.format(
                    "Invalid name. Should be like '[vendor]/[package]': '%s'",
                    this.value
                )
            );
        }
        return parts[index];
    }
}
