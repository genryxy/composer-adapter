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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.http.PhpComposer;
import com.artipie.files.FilesSlice;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.json.Json;
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
 * Integration test for PHP Composer repository.
 *
 * @since 0.1
 * @todo #78:30 min Avoid cleaning directory in this method.
 *  Now method for cleaning directory is used because otherwise
 *  temporary directory could not be deleted on github actions (it doesn't
 *  happen locally on Win). Probably it related to the fact that
 *  not all resources working with this temporary directory were
 *  properly closed. The invocation of cleaning directory should
 *  be removed.
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@DisabledOnOs(OS.WINDOWS)
class RepositoryHttpIT {
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
     * Repository URL.
     */
    private String url;

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
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new PhpComposer(new AstoRepository(new InMemoryStorage())))
        );
        this.port = this.server.start();
        this.sourceport = new RandomFreePort().get();
        Testcontainers.exposeHostPorts(this.port, this.sourceport);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.project.toString(), "/home");
        this.cntn.start();
        this.url = String.format("http://host.testcontainers.internal:%s", this.port);
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
    void shouldInstallAddedPackage() throws Exception {
        this.addPackage(
            Json.createObjectBuilder()
                .add("name", "vendor/package")
                .add("version", "1.1.2")
                .add(
                    "dist",
                    Json.createObjectBuilder()
                        .add("url", this.upload(RepositoryHttpIT.emptyZip(), this.sourceport))
                        .add("type", "zip")
                )
                .build()
                .toString()
        );
        this.writeComposer();
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

    private void addPackage(final String pack) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(
                String.format("http://localhost:%s", this.port)
            ).openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.write(pack.getBytes());
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

    private String upload(final byte[] content, final int freeport) throws Exception {
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

    private void writeComposer() throws IOException {
        Files.write(
            this.project.resolve("composer.json"),
            String.join(
                "",
                "{",
                "\"config\":{ \"secure-http\": false },",
                "\"repositories\": [",
                String.format("{\"type\": \"composer\", \"url\": \"%s\"},", this.url),
                "{\"packagist.org\": false} ",
                "],",
                "\"require\": { \"vendor/package\": \"1.1.2\" }",
                "}"
            ).getBytes()
        );
    }

    private static byte[] emptyZip() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            zip.putNextEntry(new ZipEntry("whatever"));
        }
        return bos.toByteArray();
    }
}
