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

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;

/**
 * HTTP URL connection for using in tests.
 * @since 0.4
 */
public class HttpUrlUpload {
    /**
     * Url for connection.
     */
    private final String url;

    /**
     * Content that should be uploaded by url.
     */
    private final byte[] content;

    /**
     * Ctor.
     * @param url Url for connection
     * @param content Content that should be uploaded by url
     */
    public HttpUrlUpload(final String url, final byte[] content) {
        this.url = url;
        this.content = Arrays.copyOf(content, content.length);
    }

    /**
     * Upload content to specified url with set permissions for user.
     * @param user User for basic authentication
     * @throws Exception In case of fail to upload
     */
    public void upload(final Optional<TestAuthentication.User> user) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(this.url).openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            if (user.isPresent()) {
                conn.addRequestProperty(
                    "Authorization",
                    String.format(
                        "Basic %s",
                        new String(
                            Base64.encodeBase64(
                                String.format(
                                    "%s:%s",
                                    user.get().name(),
                                    user.get().password()
                                ).getBytes(StandardCharsets.UTF_8)
                            )
                        )
                    )
                );
            }
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.write(this.content);
                dos.flush();
            }
            final int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_CREATED) {
                throw new IllegalStateException(
                    String.format("Failed to upload package: %d", status)
                );
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
