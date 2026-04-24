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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import casciian.TApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Black-box tests for {@link CasciianSshAutoConfiguration}. These verify the
 * public contract of the starter (which beans are created, when it stays
 * silent, what overriding produces) without exercising the real SSH server
 * network listener.
 */
class CasciianSshAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CasciianSshAutoConfiguration.class));

    @Test
    void failsFastWithoutATApplicationFactory() {
        runner.run(context -> {
            // No CasciianTApplicationFactory bean -> the ShellFactory bean
            // cannot be wired, so the context fails to start.
            assertThat(context).hasFailed();
            assertThat(context).getFailure()
                    .hasMessageContaining(CasciianTApplicationFactory.class.getName());
        });
    }

    @Test
    void doesNotLoadWhenDisabled() {
        runner.withUserConfiguration(FactoryConfig.class)
                .withPropertyValues("casciian.ssh.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CasciianSshServer.class);
                    assertThat(context).doesNotHaveBean(CasciianSshProperties.class);
                });
    }

    @Test
    void bindsDefaultPropertiesWhenEnabled() {
        runner.withUserConfiguration(FactoryConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CasciianSshProperties.class);
                    final CasciianSshProperties props = context.getBean(CasciianSshProperties.class);
                    assertThat(props.isEnabled()).isTrue();
                    assertThat(props.getPort()).isEqualTo(CasciianSshProperties.DEFAULT_PORT);
                    assertThat(props.getHost()).isEqualTo(CasciianSshProperties.DEFAULT_HOST);
                    assertThat(props.getHostKeyPath())
                            .isEqualTo(CasciianSshProperties.DEFAULT_HOST_KEY_PATH);
                });
    }

    @Test
    void bindsOverriddenProperties() {
        runner.withUserConfiguration(FactoryConfig.class)
                .withPropertyValues(
                        "casciian.ssh.port=3333",
                        "casciian.ssh.host=127.0.0.1",
                        "casciian.ssh.username=admin",
                        "casciian.ssh.password=hunter2",
                        "casciian.ssh.host-key-path=/tmp/key",
                        "casciian.ssh.banner=Hello")
                .run(context -> {
                    final CasciianSshProperties props = context.getBean(CasciianSshProperties.class);
                    assertThat(props.getPort()).isEqualTo(3333);
                    assertThat(props.getHost()).isEqualTo("127.0.0.1");
                    assertThat(props.getUsername()).isEqualTo("admin");
                    assertThat(props.getPassword()).isEqualTo("hunter2");
                    assertThat(props.getHostKeyPath()).isEqualTo("/tmp/key");
                    assertThat(props.getBanner()).isEqualTo("Hello");
                });
    }

    @Test
    void defaultAuthenticatorRejectsEverythingWhenCredentialsMissing() {
        runner.withUserConfiguration(FactoryConfig.class)
                .run(context -> {
                    final PasswordAuthenticator auth =
                            context.getBean(PasswordAuthenticator.class);
                    assertThat(auth).isSameAs(RejectAllPasswordAuthenticator.INSTANCE);
                });
    }

    @Test
    void defaultAuthenticatorAcceptsOnlyConfiguredCredentials() {
        runner.withUserConfiguration(FactoryConfig.class)
                .withPropertyValues(
                        "casciian.ssh.username=admin",
                        "casciian.ssh.password=hunter2")
                .run(context -> {
                    final PasswordAuthenticator auth =
                            context.getBean(PasswordAuthenticator.class);
                    assertThat(auth.authenticate("admin", "hunter2", null)).isTrue();
                    assertThat(auth.authenticate("admin", "wrong", null)).isFalse();
                    assertThat(auth.authenticate("root", "hunter2", null)).isFalse();
                });
    }

    @Test
    void userProvidedBeansOverrideDefaults() {
        final PasswordAuthenticator userAuth = mock(PasswordAuthenticator.class);
        final ShellFactory userShell = mock(ShellFactory.class);
        runner.withUserConfiguration(FactoryConfig.class)
                .withBean(PasswordAuthenticator.class, () -> userAuth)
                .withBean(ShellFactory.class, () -> userShell)
                .run(context -> {
                    assertThat(context.getBean(PasswordAuthenticator.class)).isSameAs(userAuth);
                    assertThat(context.getBean(ShellFactory.class)).isSameAs(userShell);
                    // The server is still created using whatever the user supplied.
                    assertThat(context).hasSingleBean(CasciianSshServer.class);
                });
    }

    @Configuration
    static class FactoryConfig {
        @Bean
        CasciianTApplicationFactory casciianTApplicationFactory() {
            return (in, out, session) -> mock(TApplication.class);
        }
    }
}
