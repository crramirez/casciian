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

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.github.crramirez.casciian.demo.shop.Product;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box test for the small piece of logic in {@link AdminTApplication}
 * that does not depend on a live terminal: row rendering. The
 * keyboard/mouse driven CRUD flows are exercised manually via SSH and are
 * not unit tested because they require a real {@code ECMA48Backend}.
 */
class AdminTApplicationFormatRowTest {

    @Test
    void rendersAllFixedWidthFields() {
        final Product p = new Product("SKU-1", "Widget", "desc", new BigDecimal("9.99"), 42);

        final String row = AdminTApplication.formatRow(p);

        assertThat(row)
                .contains("SKU-1")
                .contains("Widget")
                .contains("$9.99")
                .contains("stock=42");
    }

    @Test
    void truncatesOverlongFieldsToKeepRowsAligned() {
        final Product p = new Product(
                "SKU-VERY-LONG-CODE-EXTRA",
                "An extremely long product name that overflows the column",
                "",
                new BigDecimal("1.00"),
                1);

        final String row = AdminTApplication.formatRow(p);

        assertThat(row).contains("$1.00").contains("stock=1");
        // The SKU column is exactly 14 chars wide, so the very-long SKU is
        // truncated to that width.
        assertThat(row).startsWith("SKU-VERY-LONG-");
    }

    @Test
    void handlesNullStringFields() {
        final Product p = new Product();
        p.setSku(null);
        p.setName(null);
        p.setPrice(null);
        p.setStock(0);

        final String row = AdminTApplication.formatRow(p);

        assertThat(row).contains("$0.00").contains("stock=0");
    }

    @Test
    void priceAlwaysRendersWithExactlyTwoDecimals() {
        // BigDecimal scale varies depending on how a value was constructed;
        // the row must normalize so columns align in the TUI.
        final Product whole = new Product("S", "W", "", new BigDecimal("3"), 1);
        final Product oneDecimal = new Product("S", "W", "", new BigDecimal("3.5"), 1);
        final Product manyDecimals = new Product("S", "W", "", new BigDecimal("3.456"), 1);

        assertThat(AdminTApplication.formatRow(whole)).contains("$3.00");
        assertThat(AdminTApplication.formatRow(oneDecimal)).contains("$3.50");
        // 3.456 rounds half-up to 3.46.
        assertThat(AdminTApplication.formatRow(manyDecimals)).contains("$3.46");
    }
}
