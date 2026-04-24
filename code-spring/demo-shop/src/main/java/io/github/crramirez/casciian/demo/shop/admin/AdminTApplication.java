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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import casciian.TApplication;
import casciian.TInputBox;
import casciian.TList;
import casciian.TMessageBox;
import casciian.TWindow;
import casciian.event.TMenuEvent;
import casciian.menu.TMenu;

import io.github.crramirez.casciian.demo.shop.Product;
import io.github.crramirez.casciian.demo.shop.ProductRepository;

/**
 * Casciian {@link TApplication} that lets an operator CRUD the demo shop's
 * product catalogue from a terminal.
 *
 * <p>Each SSH connection gets its own instance because Casciian
 * {@code TApplication} owns mutable UI state. The {@link ProductRepository}
 * is shared (it's a singleton Spring bean) so all operators &mdash; and the
 * customer-facing web UI &mdash; see the same data.</p>
 */
public class AdminTApplication extends TApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminTApplication.class);

    /** Menu id for the "refresh product list" item. */
    static final int MID_PRODUCTS_REFRESH = 9001;
    /** Menu id for the "create new product" item. */
    static final int MID_PRODUCTS_NEW = 9002;
    /** Menu id for the "edit selected product" item. */
    static final int MID_PRODUCTS_EDIT = 9003;
    /** Menu id for the "delete selected product" item. */
    static final int MID_PRODUCTS_DELETE = 9004;

    private final ProductRepository products;

    /**
     * Cached snapshot of the products as last loaded into the list, indexed
     * the same way the {@link TList} indexes its items so we can map a
     * selection back to a {@link Product#getId()}.
     */
    private List<Product> currentSnapshot = new ArrayList<>();

    private TList productList;

    /**
     * Build a TUI bound to the given SSH channel streams.
     *
     * @param input    stream of bytes from the remote terminal
     * @param output   stream of bytes to the remote terminal
     * @param products shared product repository
     * @throws UnsupportedEncodingException if the underlying ECMA-48 backend
     *                                      cannot be created
     */
    public AdminTApplication(final InputStream input, final OutputStream output,
                             final ProductRepository products) throws UnsupportedEncodingException {
        super(input, output);
        this.products = products;
        buildMenus();
        buildMainWindow();
        refreshList();
    }

    private void buildMenus() {
        addFileMenu();

        final TMenu productsMenu = addMenu("&Products");
        productsMenu.addItem(MID_PRODUCTS_REFRESH, "&Refresh");
        productsMenu.addSeparator();
        productsMenu.addItem(MID_PRODUCTS_NEW, "&New product");
        productsMenu.addItem(MID_PRODUCTS_EDIT, "&Edit selected");
        productsMenu.addItem(MID_PRODUCTS_DELETE, "&Delete selected");

        addWindowMenu();
        addHelpMenu();
    }

    private void buildMainWindow() {
        final TWindow window = new TWindow(this, "Product catalogue", 78, 20);
        productList = window.addList(new ArrayList<>(), 1, 1,
                window.getWidth() - 4, window.getHeight() - 4);
    }

    @Override
    protected boolean onMenu(final TMenuEvent menu) {
        switch (menu.getId()) {
            case MID_PRODUCTS_REFRESH:
                refreshList();
                return true;
            case MID_PRODUCTS_NEW:
                createProduct();
                return true;
            case MID_PRODUCTS_EDIT:
                editSelectedProduct();
                return true;
            case MID_PRODUCTS_DELETE:
                deleteSelectedProduct();
                return true;
            default:
                return super.onMenu(menu);
        }
    }

    /**
     * Reload products from the repository and re-render the list.
     */
    void refreshList() {
        currentSnapshot = new ArrayList<>(products.findAll());
        currentSnapshot.sort((a, b) -> {
            final String an = a.getName() == null ? "" : a.getName();
            final String bn = b.getName() == null ? "" : b.getName();
            return an.compareToIgnoreCase(bn);
        });
        final List<String> rendered = new ArrayList<>(currentSnapshot.size());
        for (final Product p : currentSnapshot) {
            rendered.add(formatRow(p));
        }
        if (productList != null) {
            productList.setList(rendered);
        }
    }

    /**
     * Format a product as a single fixed-width row for the {@link TList}.
     *
     * <p>Package-private so it can be unit-tested without standing up a
     * terminal.</p>
     *
     * @param p the product to render; must not be null
     * @return a single-line representation suitable for a TUI list
     */
    static String formatRow(final Product p) {
        final String sku = pad(p.getSku() == null ? "" : p.getSku(), 14);
        final String name = pad(p.getName() == null ? "" : p.getName(), 32);
        final String price = "$" + (p.getPrice() == null ? "0.00" : p.getPrice().toPlainString());
        return sku + "  " + name + "  " + pad(price, 10) + "  stock=" + p.getStock();
    }

    private static String pad(final String value, final int width) {
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        final StringBuilder sb = new StringBuilder(width);
        sb.append(value);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private Product selectedProduct() {
        if (productList == null) {
            return null;
        }
        final int index = productList.getSelectedIndex();
        if (index < 0 || index >= currentSnapshot.size()) {
            return null;
        }
        return currentSnapshot.get(index);
    }

    private void createProduct() {
        final Product draft = new Product();
        draft.setSku("");
        draft.setName("");
        draft.setDescription("");
        draft.setPrice(BigDecimal.ZERO);
        draft.setStock(0);
        if (promptProductFields("Create product", draft)) {
            try {
                products.save(draft);
                refreshList();
                messageBox("Product created", "Created \"" + draft.getName() + "\".");
            } catch (final RuntimeException e) {
                LOGGER.warn("Failed to create product", e);
                messageBox("Error", "Could not create product:\n" + e.getMessage());
            }
        }
    }

    private void editSelectedProduct() {
        final Product target = selectedProduct();
        if (target == null) {
            messageBox("No selection", "Select a product in the list first.");
            return;
        }
        if (promptProductFields("Edit product", target)) {
            try {
                products.save(target);
                refreshList();
                messageBox("Saved", "Updated \"" + target.getName() + "\".");
            } catch (final RuntimeException e) {
                LOGGER.warn("Failed to update product {}", target.getId(), e);
                messageBox("Error", "Could not save product:\n" + e.getMessage());
            }
        }
    }

    private void deleteSelectedProduct() {
        final Product target = selectedProduct();
        if (target == null) {
            messageBox("No selection", "Select a product in the list first.");
            return;
        }
        final TMessageBox confirm = messageBox("Delete product",
                "Delete \"" + target.getName() + "\" (SKU " + target.getSku() + ")?",
                TMessageBox.Type.YESNO);
        if (!confirm.isYes()) {
            return;
        }
        try {
            products.delete(target);
            refreshList();
        } catch (final RuntimeException e) {
            LOGGER.warn("Failed to delete product {}", target.getId(), e);
            messageBox("Error", "Could not delete product:\n" + e.getMessage());
        }
    }

    /**
     * Walk the operator through SKU/name/description/price/stock prompts,
     * mutating {@code target} in place. Returns {@code false} if the
     * operator cancelled at any step or entered invalid data they declined
     * to retry.
     */
    private boolean promptProductFields(final String title, final Product target) {
        final TInputBox skuBox = inputBox(title, "SKU:",
                target.getSku() == null ? "" : target.getSku(), TInputBox.Type.OKCANCEL);
        if (!skuBox.isOk()) {
            return false;
        }
        final String sku = skuBox.getText().trim();
        if (sku.isEmpty()) {
            messageBox("Invalid SKU", "SKU must not be empty.");
            return false;
        }

        final TInputBox nameBox = inputBox(title, "Name:",
                target.getName() == null ? "" : target.getName(), TInputBox.Type.OKCANCEL);
        if (!nameBox.isOk()) {
            return false;
        }
        final String name = nameBox.getText().trim();
        if (name.isEmpty()) {
            messageBox("Invalid name", "Name must not be empty.");
            return false;
        }

        final TInputBox descBox = inputBox(title, "Description:",
                target.getDescription() == null ? "" : target.getDescription(),
                TInputBox.Type.OKCANCEL);
        if (!descBox.isOk()) {
            return false;
        }

        final TInputBox priceBox = inputBox(title, "Price (e.g. 12.99):",
                target.getPrice() == null ? "0.00" : target.getPrice().toPlainString(),
                TInputBox.Type.OKCANCEL);
        if (!priceBox.isOk()) {
            return false;
        }
        final BigDecimal price;
        try {
            price = new BigDecimal(priceBox.getText().trim());
            if (price.signum() < 0) {
                throw new NumberFormatException("price must be >= 0");
            }
        } catch (final NumberFormatException e) {
            messageBox("Invalid price", "Could not parse price: " + e.getMessage());
            return false;
        }

        final TInputBox stockBox = inputBox(title, "Stock (integer):",
                Integer.toString(target.getStock()), TInputBox.Type.OKCANCEL);
        if (!stockBox.isOk()) {
            return false;
        }
        final int stock;
        try {
            stock = Integer.parseInt(stockBox.getText().trim());
            if (stock < 0) {
                throw new NumberFormatException("stock must be >= 0");
            }
        } catch (final NumberFormatException e) {
            messageBox("Invalid stock", "Could not parse stock: " + e.getMessage());
            return false;
        }

        target.setSku(sku.toUpperCase(Locale.ROOT));
        target.setName(name);
        target.setDescription(descBox.getText());
        target.setPrice(price);
        target.setStock(stock);
        return true;
    }
}
