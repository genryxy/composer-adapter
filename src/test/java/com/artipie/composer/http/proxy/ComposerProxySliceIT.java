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
package com.artipie.composer.http.proxy;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.test.ComposerSimple;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * Integration test for {@link ComposerProxySlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ComposerProxySliceIT {
    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Temporary directory.
     */
    private Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Server url.
     */
    private String url;

    @BeforeEach
    void setUp() throws Exception {
        this.tmp = Files.createTempDirectory("");
        this.client.start();
        this.storage = new FileStorage(this.tmp);
        this.server = new VertxSliceServer(
            ComposerProxySliceIT.VERTX,
            new LoggingSlice(
                new ComposerProxySlice(
                    this.client,
                    URI.create("https://packagist.org"),
                    new AstoRepository(this.storage),
                    Authenticator.ANONYMOUS,
                    Cache.NOP
                )
            )
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.url = String.format("http://host.testcontainers.internal:%s", port);
    }

    @AfterEach
    @SuppressWarnings("PMD.AvoidPrintStackTrace")
    void tearDown() throws Exception {
        this.server.close();
        this.client.stop();
        this.cntn.stop();
        try {
            FileUtils.cleanDirectory(this.tmp.toFile());
            Files.deleteIfExists(this.tmp);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    @AfterAll
    static void close() {
        ComposerProxySliceIT.VERTX.close();
    }

    @Test
    void installsPackageFromRemote() throws Exception {
        new ComposerSimple(this.url, "psr/log", "1.1.3")
            .writeTo(this.tmp.resolve("composer.json"));
        new TestResource("packages-remote.json")
            .saveTo(this.storage, new Key.From("packages.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(false, "Installs: psr/log:1.1.3"),
                    new StringContains(false, "- Downloading psr/log (1.1.3)"),
                    new StringContains(
                        false,
                        "- Installing psr/log (1.1.3): Extracting archive"
                    )
                )
            )
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s\n", String.join(" ", command));
        final Container.ExecResult res = this.cntn.execInContainer(command);
        final String log = String.format(
            "STDOUT:\n%s\nSTDERR:\n%s", res.getStdout(), res.getStderr()
        );
        Logger.debug(this, log);
        return log;
    }
}
