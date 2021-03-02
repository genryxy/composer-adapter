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
package com.artipie.composer.misc;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import java.io.StringReader;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Auxiliary class for converting content to json.
 * @since 0.4
 */
public final class ContentAsJson {
    /**
     * Source content.
     */
    private final Content source;

    /**
     * Ctor.
     * @param content Source content
     */
    public ContentAsJson(final Content content) {
        this.source = content;
    }

    /**
     * Converts content to json.
     * @return JSON object
     */
    public CompletionStage<JsonObject> value() {
        return new PublisherAs(this.source)
            .asciiString()
            .thenApply(
                str -> {
                    try (JsonReader reader = Json.createReader(new StringReader(str))) {
                        return reader.readObject();
                    }
                }
            );
    }
}
