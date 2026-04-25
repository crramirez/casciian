/*
 * Copyright 2026 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.crramirez.casciian.demo.shop.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.crramirez.casciian.demo.shop.ProductRepository;
import io.github.crramirez.casciian.spring.CasciianTApplicationFactory;

/**
 * Wires the demo's admin TUI into the casciian-spring-boot-starter.
 *
 * <p>The {@link CasciianTApplicationFactory} bean is invoked once per
 * incoming SSH connection. We instantiate a new {@link AdminTApplication}
 * per session because Casciian {@code TApplication} instances own mutable
 * UI state and cannot be shared across terminals.</p>
 */
@Configuration
public class AdminTuiConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminTuiConfig.class);

    @Bean
    CasciianTApplicationFactory adminTApplicationFactory(final ProductRepository products) {
        return (input, output, session) -> {
            LOGGER.info("Casciian admin TUI session opened for user '{}' from {} ({}x{} {})",
                    session.username(), session.remoteAddress(),
                    session.columns(), session.rows(), session.terminalType());
            return new AdminTApplication(input, output, products);
        };
    }
}
