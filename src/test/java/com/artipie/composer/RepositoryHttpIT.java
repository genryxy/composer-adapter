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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.http.PhpComposer;
import com.artipie.files.FilesSlice;
import com.artipie.vertx.VertxSliceServer;
import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for PHP Composer repository.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class RepositoryHttpIT {

    // @checkstyle VisibilityModifierCheck (5 lines)
    /**
     * Temporary directory.
     */
    @TempDir
    Path temp;

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
     * Repository URL.
     */
    private String url;

    @BeforeEach
    void setUp() throws Exception {
        this.vertx = Vertx.vertx();
        this.project = this.temp.resolve("project");
        this.project.toFile().mkdirs();
        this.ensureComposerInstalled();
        this.server = new VertxSliceServer(
            this.vertx,
            new PhpComposer(new AstoRepository(new InMemoryStorage()))
        );
        final int port = this.server.start();
        this.url = String.format("http://localhost:%s", port);
    }

    @AfterEach
    void tearDown() {
        if (this.server != null) {
            this.server.stop();
        }
        if (this.vertx != null) {
            this.vertx.close();
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
                        .add("url", this.upload(RepositoryHttpIT.emptyZip()))
                        .add("type", "zip")
                )
                .build()
                .toString()
        );
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
        MatcherAssert.assertThat(
            this.run("install"),
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
            conn = (HttpURLConnection) new URL(this.url).openConnection();
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

    private String upload(final byte[] content) throws Exception {
        final InMemoryStorage files = new InMemoryStorage();
        final String name = UUID.randomUUID().toString();
        new BlockingStorage(files).save(new Key.From(name), content);
        final int port = new VertxSliceServer(this.vertx, new FilesSlice(files)).start();
        return String.format("http://localhost:%d/%s", port, name);
    }

    private void ensureComposerInstalled() throws Exception {
        final String output = this.run("--version");
        if (!output.startsWith("Composer version")) {
            throw new IllegalStateException("Composer not installed");
        }
    }

    private String run(final String... args) throws Exception {
        final Path stdout = this.temp.resolve(
            String.format("%s-stdout.txt", UUID.randomUUID().toString())
        );
        final int code = new ProcessBuilder()
            .directory(this.project.toFile())
            .command(
                ImmutableList.<String>builder()
                    .add(RepositoryHttpIT.command())
                    .add(args)
                    .add("--verbose")
                    .add("--no-cache")
                    .build()
            )
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        if (code != 0) {
            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
        }
        return log;
    }

    private static String command() {
        final String cmd;
        if (System.getProperty("os.name").startsWith("Windows")) {
            cmd = "composer.bat";
        } else {
            cmd = "composer";
        }
        return cmd;
    }

    private static byte[] emptyZip() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ZipOutputStream zip = new ZipOutputStream(bos);
        zip.putNextEntry(new ZipEntry("whatever"));
        zip.close();
        return bos.toByteArray();
    }
}
