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

import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.RejectAllPasswordAuthenticator;
import org.apache.sshd.server.shell.ShellFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import casciian.TApplication;

/**
 * Spring Boot auto-configuration that wires an embedded SSH server serving a
 * Casciian {@link TApplication} per connection.
 *
 * <p>The auto-configuration is active when:</p>
 * <ul>
 *   <li>{@link TApplication} is on the classpath (the starter depends on the
 *       Casciian library, so this is effectively always true but keeps the
 *       guard explicit).</li>
 *   <li>Property {@code casciian.ssh.enabled} is not set to {@code false}.</li>
 * </ul>
 *
 * <p>Applications <strong>must</strong> publish a
 * {@link CasciianTApplicationFactory} bean; the auto-configuration will fail
 * fast at bean-creation time if none is present, because there is no sensible
 * default TUI.</p>
 */
@AutoConfiguration
@ConditionalOnClass(TApplication.class)
@ConditionalOnProperty(prefix = "casciian.ssh", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CasciianSshProperties.class)
public class CasciianSshAutoConfiguration {

    /**
     * Default password authenticator backed by
     * {@link CasciianSshProperties#getUsername()} /
     * {@link CasciianSshProperties#getPassword()}.
     *
     * <p>If either is blank the returned authenticator rejects every attempt,
     * so a misconfigured starter never silently grants anonymous access.
     * Applications can override this by declaring their own
     * {@link PasswordAuthenticator} bean (e.g. one backed by Spring
     * Security).</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordAuthenticator casciianSshPasswordAuthenticator(
            final CasciianSshProperties properties) {
        final String expectedUser = properties.getUsername();
        final String expectedPassword = properties.getPassword();
        if (expectedUser == null || expectedUser.isBlank()
                || expectedPassword == null || expectedPassword.isBlank()) {
            return RejectAllPasswordAuthenticator.INSTANCE;
        }
        return (username, password, session) ->
                expectedUser.equals(username) && expectedPassword.equals(password);
    }

    @Bean
    @ConditionalOnMissingBean
    public ShellFactory casciianShellFactory(final CasciianTApplicationFactory factory) {
        return new CasciianShellFactory(factory);
    }

    @Bean
    @ConditionalOnMissingBean
    public CasciianSshServer casciianSshServer(final CasciianSshProperties properties,
                                               final ShellFactory shellFactory,
                                               final PasswordAuthenticator authenticator) {
        // Start/stop is driven by SmartLifecycle; Spring will invoke start()
        // after all beans are ready and stop() on context shutdown.
        return new CasciianSshServer(properties, shellFactory, authenticator);
    }
}
