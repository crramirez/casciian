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

import casciian.TAction;
import casciian.TApplication;
import casciian.TList;
import casciian.TMessageBox;
import casciian.TWidget;
import casciian.TWindow;
import casciian.event.TMenuEvent;
import casciian.event.TResizeEvent;
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
        final ProductCatalogueWindow window = new ProductCatalogueWindow(this);
        // Place the list so its right/bottom scrollbars are drawn on top of
        // the window's right/bottom borders, matching the look of
        // {@code TEditorWindow}. The list spans from interior position
        // (1, 1) all the way to the border, so the scrollers (which live
        // in the list's last column/row) land exactly on the frame.
        productList = window.addList(new ArrayList<>(), 1, 1,
                window.getWidth() - 2, window.getHeight() - 2,
                new TAction() {
                    @Override
                    public void DO() {
                        // Triggered on Enter and on double click.
                        editSelectedProduct();
                    }
                });
        window.setProductList(productList);
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
        final String price = "$" + formatPrice(p.getPrice());
        return sku + "  " + name + "  " + pad(price, 10) + "  stock=" + p.getStock();
    }

    /**
     * Format a price as a fixed two-decimal string using a stable locale,
     * so that TUI rows align and the rendering matches the web view.
     *
     * @param price the price; may be null (treated as zero)
     * @return the formatted price (e.g. {@code "1.00"} or {@code "12.99"})
     */
    private static String formatPrice(final BigDecimal price) {
        if (price == null) {
            return "0.00";
        }
        return String.format(Locale.ROOT, "%.2f", price);
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
        new ProductFormWindow(this, "Create product", draft, new TAction() {
            @Override
            public void DO() {
                try {
                    products.save(draft);
                    refreshList();
                    messageBox("Product created",
                            "Created \"" + draft.getName() + "\".");
                } catch (final RuntimeException e) {
                    LOGGER.warn("Failed to create product", e);
                    messageBox("Error",
                            "Could not create product:\n" + e.getMessage());
                }
            }
        });
    }

    private void editSelectedProduct() {
        final Product target = selectedProduct();
        if (target == null) {
            messageBox("No selection", "Select a product in the list first.");
            return;
        }
        new ProductFormWindow(this, "Edit product", target, new TAction() {
            @Override
            public void DO() {
                try {
                    products.save(target);
                    refreshList();
                    messageBox("Saved",
                            "Updated \"" + target.getName() + "\".");
                } catch (final RuntimeException e) {
                    LOGGER.warn("Failed to update product {}", target.getId(), e);
                    messageBox("Error",
                            "Could not save product:\n" + e.getMessage());
                }
            }
        });
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
     * Main catalogue window. A small {@link TWindow} subclass exists so
     * that the embedded product list can be resized in lockstep with the
     * window itself, keeping the scrollbars glued to the window border
     * even after a resize.
     */
    private static final class ProductCatalogueWindow extends TWindow {

        /** The product list whose dimensions track this window's. */
        private TList list;

        ProductCatalogueWindow(final TApplication parent) {
            super(parent, "Product catalogue", 78, 20, RESIZABLE);
        }

        void setProductList(final TList productList) {
            this.list = productList;
        }

        @Override
        public void onResize(final TResizeEvent event) {
            if (event.getType() == TResizeEvent.Type.WIDGET && list != null) {
                // Resize the list to keep filling the window. The list
                // origin stays at (1, 1) and it spans up to the border
                // so the TList scrollbars overlay the window frame.
                final TResizeEvent listSize = new TResizeEvent(
                        event.getBackend(), TResizeEvent.Type.WIDGET,
                        event.getWidth() - 2, event.getHeight() - 2);
                list.onResize(listSize);
                return;
            }
            // Pass to children for SCREEN events.
            for (final TWidget widget : getChildren()) {
                widget.onResize(event);
            }
        }
    }
}
