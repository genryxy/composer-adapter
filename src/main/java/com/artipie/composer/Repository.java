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
import com.artipie.asto.Key;
import com.artipie.composer.http.Archive;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * PHP Composer repository.
 *
 * @since 0.3
 */
public interface Repository {

    /**
     * Reads packages description from storage.
     *
     * @return Packages found by name, might be empty.
     */
    CompletionStage<Optional<Packages>> packages();

    /**
     * Reads packages description from storage.
     *
     * @param name Package name.
     * @return Packages found by name, might be empty.
     */
    CompletionStage<Optional<Packages>> packages(Name name);

    /**
     * Adds package described in JSON format from storage.
     *
     * @param content Package content.
     * @param version Version in case of absence version in content with package. If package
     *  does not contain version, this value should be passed as a parameter.
     * @return Completion of adding package to repository.
     */
    CompletableFuture<Void> addJson(Content content, Optional<String> version);

    /**
     * Adds package described in archive with ZIP or TAR.GZ
     * format from storage.
     *
     * @param archive Archive with package content.
     * @param content Package content.
     * @return Completion of adding package to repository.
     */
    CompletableFuture<Void> addArchive(Archive archive, Content content);

    /**
     * Obtain bytes by key.
     * @param key The key
     * @return Bytes.
     */
    CompletableFuture<Content> value(Key key);

    /**
     * This file exists?
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise.
     */
    CompletableFuture<Boolean> exists(Key key);
}
