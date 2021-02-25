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

import com.artipie.http.auth.Authentication;

/**
 * Basic authentication for usage in tests. Alice is authenticated.
 * @since 0.4
 */
public final class TestAuthentication extends Authentication.Wrap {

    /**
     * Example Alice user.
     */
    public static final User ALICE = new User("Alice", "OpenSesame");

    /**
     * Example Bob user.
     */
    public static final User BOB = new User("Bob", "123");

    /**
     * Ctor.
     */
    public TestAuthentication() {
        super(
            new Authentication.Single(
                TestAuthentication.ALICE.name(),
                TestAuthentication.ALICE.password()
            )
        );
    }

    /**
     * User with name and password.
     * @since 0.4
     */
    public static final class User {

        /**
         * Username.
         */
        private final String username;

        /**
         * Password.
         */
        private final String pwd;

        /**
         * Ctor.
         * @param username Username
         * @param pwd Password
         */
        User(final String username, final String pwd) {
            this.username = username;
            this.pwd = pwd;
        }

        /**
         * Get username.
         * @return Username.
         */
        public String name() {
            return this.username;
        }

        /**
         * Get password.
         * @return Password.
         */
        public String password() {
            return this.pwd;
        }

        @Override
        public String toString() {
            return this.username;
        }
    }
}

