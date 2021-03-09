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

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.util.regex.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link AddArchiveSlice}.
 *
 * @since 0.4
 */
final class AddArchiveSliceTest {
    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @CsvSource({
        "/log-1.1.3.zip,log,1.1.3",
        "/log-bad.1.3.zip,,",
        "/path/name-2.1.3.zip,,",
        "/name-prefix-0.10.321.zip,name-prefix,0.10.321",
        "/name.suffix-1.2.2-patch.zip,name.suffix,1.2.2-patch",
        "/name-2.3.1-beta1.zip,name,2.3.1-beta1"
    })
    void patternExtractsNameAndVersionCorrectly(
        final String url, final String name, final String vers
    ) {
        final Matcher matcher = AddArchiveSlice.PATH.matcher(url);
        final String cname;
        final String cvers;
        if (matcher.matches()) {
            cname = matcher.group("name");
            cvers = matcher.group("version");
        } else {
            cname = null;
            cvers = null;
        }
        MatcherAssert.assertThat(
            "Name is correct",
            cname,
            new IsEqual<>(name)
        );
        MatcherAssert.assertThat(
            "Version is correct",
            cvers,
            new IsEqual<>(vers)
        );
    }

    @Test
    void returnsBadRequest() {
        MatcherAssert.assertThat(
            new AddArchiveSlice(new AstoRepository(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.GET, "/bad/request")
            )
        );
    }
}
