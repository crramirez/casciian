/*
 * Copyright 2026 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.crramirez.casciian.spring;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the host-key path resolution logic in {@link CasciianSshServer}.
 * The full start/stop lifecycle is covered by integration testing against a
 * real SSH client and is out of scope for these unit tests.
 *
 * <p>Every test that may exercise the {@code ~}-expansion codepath runs with
 * {@code user.home} redirected to a JUnit {@link TempDir} so the tests never
 * create files or directories under the real developer/CI user home.</p>
 */
class CasciianSshServerTest {

    @TempDir
    Path fakeHome;

    private String originalUserHome;

    @BeforeEach
    void redirectUserHome() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", fakeHome.toString());
    }

    @AfterEach
    void restoreUserHome() {
        if (originalUserHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void expandsTildeAgainstUserHome() {
        final CasciianSshProperties props = new CasciianSshProperties();
        props.setHostKeyPath("~/casciian-test-key/hostkey");
        final CasciianSshServer server = new CasciianSshServer(props, null, null);

        final Path resolved = server.resolveHostKeyPath();

        assertThat(resolved).isAbsolute();
        assertThat(resolved.toString()).startsWith(fakeHome.toString());
        assertThat(resolved.getFileName().toString()).isEqualTo("hostkey");
        // Parent directory was created as a side-effect, inside the fake home.
        assertThat(resolved.getParent()).exists();
    }

    @Test
    void acceptsAbsolutePathsUnchanged(@TempDir final Path tmp) {
        final Path target = tmp.resolve("sub/hostkey");
        final CasciianSshProperties props = new CasciianSshProperties();
        props.setHostKeyPath(target.toString());
        final CasciianSshServer server = new CasciianSshServer(props, null, null);

        final Path resolved = server.resolveHostKeyPath();

        assertThat(resolved).isEqualTo(target.toAbsolutePath());
        assertThat(resolved.getParent()).exists();
    }

    @Test
    void fallsBackToDefaultWhenNullConfigured() {
        final CasciianSshProperties props = new CasciianSshProperties();
        props.setHostKeyPath(null);
        final CasciianSshServer server = new CasciianSshServer(props, null, null);

        final Path resolved = server.resolveHostKeyPath();

        assertThat(resolved.toString()).startsWith(fakeHome.toString());
    }

    @Test
    void fallsBackToDefaultWhenBlankConfigured() {
        final CasciianSshProperties props = new CasciianSshProperties();
        props.setHostKeyPath("   ");
        final CasciianSshServer server = new CasciianSshServer(props, null, null);

        final Path resolved = server.resolveHostKeyPath();

        // A blank value must not resolve to the working directory; it should
        // behave like null and fall back to the documented default under home.
        assertThat(resolved.toString()).startsWith(fakeHome.toString());
    }
}
