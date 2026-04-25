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
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box tests for {@link Product#equals(Object)} / {@link Product#hashCode()}.
 *
 * <p>Two transient (id-less) entities must never be considered equal,
 * otherwise hash-based collections (e.g. {@code Set}, {@code Map}) collapse
 * distinct unsaved products into a single entry. Once both sides have an
 * id, equality reduces to id comparison.</p>
 */
class ProductEqualsTest {

    @Test
    void transientProductsAreNotEqualEvenWithIdenticalFields() {
        final Product a = new Product("SKU", "Name", "desc", new BigDecimal("1.00"), 1);
        final Product b = new Product("SKU", "Name", "desc", new BigDecimal("1.00"), 1);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void transientProductIsEqualToItself() {
        final Product a = new Product("SKU", "Name", "desc", new BigDecimal("1.00"), 1);

        assertThat(a).isEqualTo(a);
    }

    @Test
    void persistedProductsAreEqualWhenIdsMatch() {
        final Product a = new Product("SKU-A", "A", "", new BigDecimal("1.00"), 1);
        final Product b = new Product("SKU-B", "B", "", new BigDecimal("9.00"), 9);
        a.setId(7L);
        b.setId(7L);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void persistedProductsAreNotEqualWhenIdsDiffer() {
        final Product a = new Product("SKU", "N", "", new BigDecimal("1.00"), 1);
        final Product b = new Product("SKU", "N", "", new BigDecimal("1.00"), 1);
        a.setId(1L);
        b.setId(2L);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void transientAndPersistedAreNotEqual() {
        final Product transientP = new Product("SKU", "N", "", new BigDecimal("1.00"), 1);
        final Product persisted = new Product("SKU", "N", "", new BigDecimal("1.00"), 1);
        persisted.setId(1L);

        assertThat(transientP).isNotEqualTo(persisted);
        assertThat(persisted).isNotEqualTo(transientP);
    }

    @Test
    void multipleTransientProductsCoexistInASet() {
        final Set<Product> set = new HashSet<>();
        set.add(new Product("A", "A", "", new BigDecimal("1.00"), 1));
        set.add(new Product("B", "B", "", new BigDecimal("2.00"), 2));
        set.add(new Product("C", "C", "", new BigDecimal("3.00"), 3));

        assertThat(set).hasSize(3);
    }
}
