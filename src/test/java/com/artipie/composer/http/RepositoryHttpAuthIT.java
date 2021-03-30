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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.test.ComposerSimple;
import com.artipie.composer.test.EmptyZip;
import com.artipie.composer.test.HttpUrlUpload;
import com.artipie.composer.test.PackageSimple;
import com.artipie.composer.test.TestAuthentication;
import com.artipie.files.FilesSlice;
import com.artipie.http.Slice;
import com.artipie.http.auth.JoinedPermissions;
import com.artipie.http.auth.Permissions;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
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
 * Integration test for PHP Composer repository with auth.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RepositoryHttpAuthIT {
    /**
     * Temporary directory.
     */
    private Path temp;

    /**
     * Vert.x instance to use in tests.
     */
    private Vertx vertx;

    /**
     * Path to PHP project directory.
     */
    private Path project;

    /**
     * HTTP server hosting repository.
     */
    private VertxSliceServer server;

    /**
     * HTTP source server.
     */
    private VertxSliceServer sourceserver;

    /**
     * Test container.
     */
    private GenericContainer<?> cntn;

    /**
     * Server port.
     */
    private int port;

    /**
     * Source port for tgz archive.
     */
    private int sourceport;

    @BeforeEach
    void setUp() throws IOException {
        this.temp = Files.createTempDirectory("");
        this.vertx = Vertx.vertx();
        this.project = this.temp.resolve("project");
        this.project.toFile().mkdirs();
        this.port = new RandomFreePort().get();
        this.sourceport = new RandomFreePort().get();
        final Slice slice = new PhpComposer(
            new AstoRepository(new InMemoryStorage()),
            new JoinedPermissions(
                new Permissions.Single(TestAuthentication.ALICE.name(), "write"),
                new Permissions.Single(TestAuthentication.ALICE.name(), "read")
            ),
            new TestAuthentication()
        );
        this.server = new VertxSliceServer(this.vertx, new LoggingSlice(slice), this.port);
        this.server.start();
        Testcontainers.exposeHostPorts(this.port, this.sourceport);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.project.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    @SuppressWarnings("PMD.AvoidPrintStackTrace")
    void tearDown() {
        if (this.sourceserver != null) {
            this.sourceserver.stop();
        }
        this.server.stop();
        this.vertx.close();
        this.cntn.stop();
        try {
            FileUtils.cleanDirectory(this.temp.toFile());
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void shouldInstallAddedPackageWithAuth() throws Exception {
        this.addPackage();
        new ComposerSimple(this.url(TestAuthentication.ALICE))
            .writeTo(this.project.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(false, "Installs: vendor/package:1.1.2"),
                    new StringContains(false, "- Downloading vendor/package (1.1.2)"),
                    new StringContains(
                        false,
                        "- Installing vendor/package (1.1.2): Extracting archive"
                    )
                )
            )
        );
    }

    @Test
    void returnsUnauthorizedWhenUserIsUnknown() throws Exception {
        this.addPackage();
        new ComposerSimple(this.url(TestAuthentication.BOB))
            .writeTo(this.project.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new StringContains("URL required authentication")
        );
    }

    private void addPackage() throws Exception {
        new HttpUrlUpload(
            String.format("http://localhost:%s", this.port),
            new PackageSimple(
                this.upload(new EmptyZip().value(), this.sourceport)
            ).withSetVersion()
        ).upload(Optional.of(TestAuthentication.ALICE));
    }

    private String upload(final byte[] content, final int freeport) {
        final Storage files = new InMemoryStorage();
        final String name = UUID.randomUUID().toString();
        new BlockingStorage(files).save(new Key.From(name), content);
        this.sourceserver = new VertxSliceServer(
            this.vertx, new LoggingSlice(new FilesSlice(files)), freeport
        );
        this.sourceserver.start();
        return String.format("http://host.testcontainers.internal:%d/%s", freeport, name);
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

    private String url(final TestAuthentication.User user) {
        return String.format(
            "http://%s:%s@host.testcontainers.internal:%d",
            user.name(),
            user.password(),
            this.port
        );
    }
}
