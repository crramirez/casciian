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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.datafaker.Faker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for {@link ProductFakerSeeder}. We exercise the contract
 * an operator cares about &mdash; idempotency, the right number of
 * products, basic data sanity &mdash; without spinning up Spring or H2.
 */
class ProductFakerSeederTest {

    private ProductRepository repository;
    private List<Product> stored;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        stored = new ArrayList<>();
        final AtomicLong ids = new AtomicLong();
        when(repository.count()).thenAnswer((InvocationOnMock i) -> (long) stored.size());
        when(repository.save(any(Product.class))).thenAnswer((InvocationOnMock i) -> {
            final Product p = i.getArgument(0);
            p.setId(ids.incrementAndGet());
            stored.add(p);
            return p;
        });
    }

    @Test
    void seedsRequestedNumberOfProductsWhenTableIsEmpty() {
        final ProductFakerSeeder seeder = new ProductFakerSeeder(repository,
                new Faker(Locale.ENGLISH, new Random(1L)),
                new Random(1L), 7);

        final List<Product> created = seeder.seed();

        assertThat(created).hasSize(7);
        assertThat(stored).hasSize(7);
        assertThat(created).allSatisfy(p -> {
            assertThat(p.getSku()).isNotBlank();
            assertThat(p.getName()).isNotBlank();
            assertThat(p.getPrice()).isNotNull()
                    .matches(price -> price.signum() >= 0,
                            "price must be non-negative");
            assertThat(p.getStock()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void doesNotSeedWhenTableIsAlreadyPopulated() {
        stored.add(new Product("EXISTING-1", "Existing", "", BigDecimal.ONE, 1));

        final ProductFakerSeeder seeder = new ProductFakerSeeder(repository,
                new Faker(Locale.ENGLISH, new Random(1L)),
                new Random(1L), 7);

        final List<Product> created = seeder.seed();

        assertThat(created).isEmpty();
        assertThat(stored).hasSize(1);
    }

    @Test
    void generatedSkusAreUniqueAcrossABatch() {
        final ProductFakerSeeder seeder = new ProductFakerSeeder(repository,
                new Faker(Locale.ENGLISH, new Random(1L)),
                new Random(1L), ProductFakerSeeder.DEFAULT_PRODUCT_COUNT);

        seeder.seed();

        assertThat(stored).extracting(Product::getSku).doesNotHaveDuplicates();
    }

    @Test
    void productPriceUsesTwoDecimalPlaces() {
        final ProductFakerSeeder seeder = new ProductFakerSeeder(repository,
                new Faker(Locale.ENGLISH, new Random(1L)),
                new Random(1L), 5);

        final List<Product> created = seeder.seed();

        assertThat(created).allSatisfy(p ->
                assertThat(p.getPrice().scale()).isEqualTo(2));
    }
}
