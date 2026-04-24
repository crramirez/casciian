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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Casciian SSH server.
 *
 * <p>Bind under the {@code casciian.ssh} prefix in {@code application.yml} or
 * {@code application.properties}.</p>
 */
@ConfigurationProperties(prefix = "casciian.ssh")
public class CasciianSshProperties {

    /** Default listen port (non-privileged so the application does not need root). */
    public static final int DEFAULT_PORT = 2222;

    /** Default listen address (all interfaces). */
    public static final String DEFAULT_HOST = "0.0.0.0";

    /** Default location of the persisted SSH host key, relative to the user home. */
    public static final String DEFAULT_HOST_KEY_PATH = "~/.casciian/ssh_host_key";

    /** Whether the SSH server should start. Defaults to {@code true}. */
    private boolean enabled = true;

    /** Interface to bind to. Defaults to {@link #DEFAULT_HOST}. */
    private String host = DEFAULT_HOST;

    /** TCP port to listen on. Defaults to {@link #DEFAULT_PORT}. */
    private int port = DEFAULT_PORT;

    /**
     * Single accepted username. If {@code null} or blank, password-based
     * authentication is disabled and the application must supply its own
     * {@link org.apache.sshd.server.auth.password.PasswordAuthenticator} bean.
     */
    private String username;

    /**
     * Password associated with {@link #username}. If {@code null} or blank,
     * password-based authentication is disabled.
     */
    private String password;

    /**
     * Path to the persistent SSH host key. If the file does not exist it will
     * be generated on first start. A leading {@code ~} is expanded to the user
     * home.
     */
    private String hostKeyPath = DEFAULT_HOST_KEY_PATH;

    /**
     * Optional banner text sent to clients before authentication. If
     * {@code null} no banner is sent.
     */
    private String banner;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getHostKeyPath() {
        return hostKeyPath;
    }

    public void setHostKeyPath(final String hostKeyPath) {
        this.hostKeyPath = hostKeyPath;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(final String banner) {
        this.banner = banner;
    }
}
