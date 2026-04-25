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
import java.math.RoundingMode;
import java.util.Locale;

import casciian.TAction;
import casciian.TApplication;
import casciian.TField;
import casciian.TMessageBox;
import casciian.TWindow;

import io.github.crramirez.casciian.demo.shop.Product;

/**
 * Modal window that lets the operator create or edit a {@link Product}
 * using a labelled form (mirrors the {@code DemoTextFieldWindow} layout)
 * instead of a chain of {@link casciian.TInputBox} prompts.
 *
 * <p>The window is non-blocking: it is shown to the user, and on OK it
 * applies the changes to the supplied {@link Product} instance and invokes
 * the {@code onSave} callback so the caller can persist the entity and
 * refresh its view. On Cancel the window simply closes without mutating
 * anything.</p>
 */
final class ProductFormWindow extends TWindow {

    /** Width of the form's input fields, in cells. */
    private static final int FIELD_WIDTH = 32;

    /** Column at which the input fields begin. */
    private static final int FIELD_COLUMN = 16;

    private final Product target;
    private final TAction onSave;

    private final TField skuField;
    private final TField nameField;
    private final TField descriptionField;
    private final TField priceField;
    private final TField stockField;

    /**
     * Build a modal product form.
     *
     * @param parent the owning admin application
     * @param title  window title (e.g. {@code "Create product"})
     * @param target the product instance to edit; mutated in place on OK
     * @param onSave callback invoked after a successful OK validation;
     *               typically persists the product and refreshes the list
     */
    @SuppressWarnings("this-escape")
    ProductFormWindow(final TApplication parent, final String title,
                      final Product target, final TAction onSave) {
        super(parent, title, 0, 0, 56, 14, MODAL | CENTERED);
        this.target = target;
        this.onSave = onSave;

        int row = 1;

        addLabel("SKU:", 2, row);
        skuField = addField(FIELD_COLUMN, row++, FIELD_WIDTH, false,
                target.getSku() == null ? "" : target.getSku());

        addLabel("Name:", 2, row);
        nameField = addField(FIELD_COLUMN, row++, FIELD_WIDTH, false,
                target.getName() == null ? "" : target.getName());

        addLabel("Description:", 2, row);
        descriptionField = addField(FIELD_COLUMN, row++, FIELD_WIDTH, false,
                target.getDescription() == null ? "" : target.getDescription());

        addLabel("Price:", 2, row);
        priceField = addField(FIELD_COLUMN, row++, 12, false,
                target.getPrice() == null
                        ? "0.00" : target.getPrice().toPlainString());

        addLabel("Stock:", 2, row);
        stockField = addField(FIELD_COLUMN, row++, 8, false,
                Integer.toString(target.getStock()));

        // Buttons
        final int buttonRow = getHeight() - 4;
        addButton("&OK", getWidth() / 2 - 12, buttonRow, new TAction() {
            @Override
            public void DO() {
                applyAndClose();
            }
        });
        addButton("&Cancel", getWidth() / 2 + 2, buttonRow, new TAction() {
            @Override
            public void DO() {
                getApplication().closeWindow(ProductFormWindow.this);
            }
        });

        activate(skuField);
    }

    /**
     * Validate the form fields, copy them onto the {@link Product}, fire
     * the {@code onSave} callback and close the window. Validation errors
     * are reported via a message box and leave the form open so the
     * operator can correct them.
     */
    private void applyAndClose() {
        final String sku = skuField.getText().trim();
        if (sku.isEmpty()) {
            getApplication().messageBox("Invalid SKU",
                    "SKU must not be empty.", TMessageBox.Type.OK);
            activate(skuField);
            return;
        }

        final String name = nameField.getText().trim();
        if (name.isEmpty()) {
            getApplication().messageBox("Invalid name",
                    "Name must not be empty.", TMessageBox.Type.OK);
            activate(nameField);
            return;
        }

        final BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim())
                    .setScale(2, RoundingMode.HALF_UP);
            if (price.signum() < 0) {
                throw new NumberFormatException("price must be >= 0");
            }
        } catch (final NumberFormatException | ArithmeticException e) {
            getApplication().messageBox("Invalid price",
                    "Could not parse price: " + e.getMessage(),
                    TMessageBox.Type.OK);
            activate(priceField);
            return;
        }

        final int stock;
        try {
            stock = Integer.parseInt(stockField.getText().trim());
            if (stock < 0) {
                throw new NumberFormatException("stock must be >= 0");
            }
        } catch (final NumberFormatException e) {
            getApplication().messageBox("Invalid stock",
                    "Could not parse stock: " + e.getMessage(),
                    TMessageBox.Type.OK);
            activate(stockField);
            return;
        }

        target.setSku(sku.toUpperCase(Locale.ROOT));
        target.setName(name);
        target.setDescription(descriptionField.getText());
        target.setPrice(price);
        target.setStock(stock);

        getApplication().closeWindow(this);

        if (onSave != null) {
            onSave.DO();
        }
    }
}
