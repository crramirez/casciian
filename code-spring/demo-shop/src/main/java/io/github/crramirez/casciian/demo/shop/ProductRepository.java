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

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Product}. The web layer uses
 * {@link #findAll()} for the customer-facing catalogue, and the admin TUI
 * uses the full CRUD surface inherited from {@link JpaRepository}.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
