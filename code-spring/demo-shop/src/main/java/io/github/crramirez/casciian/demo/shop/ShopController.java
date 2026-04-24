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

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Customer-facing storefront. Renders the catalogue from the H2 database
 * using the same {@link ProductRepository} the admin TUI mutates, so changes
 * made through the TUI show up here on the next page refresh.
 */
@Controller
public class ShopController {

    private final ProductRepository products;

    public ShopController(final ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/")
    public String catalogue(final Model model) {
        model.addAttribute("products", products.findAll(Sort.by("name").ascending()));
        return "catalogue";
    }
}
