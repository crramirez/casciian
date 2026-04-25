/*
 * Copyright 2026 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.crramirez.casciian.demo.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the demo shop. The application exposes:
 *
 * <ul>
 *   <li>A customer-facing product catalogue at <code>http://localhost:8080/</code>
 *       backed by an in-memory H2 database seeded with random products.</li>
 *   <li>A Casciian admin TUI reachable over SSH at port 2222 that allows
 *       full CRUD on the same product catalogue.</li>
 * </ul>
 *
 * <p>The whole point of this demo is to show that a single Spring Boot
 * application can serve both audiences from one JVM, sharing the same
 * Spring beans (repositories, services, security context).</p>
 */
@SpringBootApplication
public class DemoShopApplication {

    /**
     * Standard Spring Boot main method.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(DemoShopApplication.class, args);
    }
}
