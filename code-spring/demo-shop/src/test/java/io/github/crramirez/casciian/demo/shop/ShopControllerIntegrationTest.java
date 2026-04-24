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

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-end test of the customer-facing shop. We boot the full Spring
 * context (H2 + JPA + MVC) and verify the catalogue page renders the
 * products that the {@link ProductRepository} actually contains. Casciian's
 * SSH server is disabled so the test does not bind to port 2222.
 */
@SpringBootTest(properties = {
        "casciian.ssh.enabled=false",
        // Suppress the auto-seeder so this test owns the database state.
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ShopControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository repository;

    @Test
    void cataloguePageListsAllProductsFromTheDatabase() throws Exception {
        repository.deleteAll();
        repository.saveAll(List.of(
                new Product("SKU-A", "Apple", "Red and crisp", new BigDecimal("1.50"), 10),
                new Product("SKU-B", "Banana", "Yellow and ripe", new BigDecimal("0.40"), 20)));

        final MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        final MvcResult result = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogue"))
                .andExpect(model().attributeExists("products"))
                .andReturn();

        @SuppressWarnings("unchecked")
        final List<Product> products = (List<Product>) result.getModelAndView()
                .getModel().get("products");
        assertThat(products).extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-A", "SKU-B");

        final String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Apple").contains("Banana").contains("$1.50");
    }
}
