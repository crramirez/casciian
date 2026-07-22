/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package casciian;

import casciian.bits.CellAttributes;
import casciian.bits.ColorTheme;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbSpace;

/**
 * THyperLink implements a clickable hyperlink, based on {@link TLabel}.
 *
 * <p>
 * The visible text is tagged with an OSC 8 hyperlink URI so that terminals
 * that understand OSC 8 hyperlinks render it as a real clickable link.  The
 * widget also changes color depending on its state: a normal color, a hover
 * color used while the mouse is over the link, and a visited color used once
 * the link has been clicked.  Each state has a corresponding {@code .modal}
 * variant used when the containing window is modal.
 * </p>
 *
 * @see ColorTheme#THYPERLINK
 * @see ColorTheme#THYPERLINK_HOVER
 */
public class THyperLink extends TLabel<TWidget> {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The hyperlink target URI.
     */
    private String uri;

    /**
     * If true, the mouse is currently hovering over this link.
     */
    private boolean hover = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text visible text of the link
     * @param uri hyperlink target URI
     * @param x column relative to parent
     * @param y row relative to parent
     */
    public THyperLink(final TWidget parent, final String text,
        final String uri, final int x, final int y) {

        super(parent, text, x, y, ColorTheme.THYPERLINK, true, null, null);
        this.uri = uri;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the link.
     *
     * @param mouse mouse event
     * @return true if the mouse is over this link
     */
    private boolean mouseOnLink(final TMouseEvent mouse) {
        return (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < getWidth());
    }

    /**
     * Handle mouse movements to track the hover state.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        hover = mouseOnLink(mouse);
        super.onMouseMotion(mouse);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the hyperlink.
     */
    @Override
    public void draw() {
        String colorKey;
        if (hover || isActive()) {
            colorKey = ColorTheme.THYPERLINK_HOVER;
        } else {
            colorKey = ColorTheme.THYPERLINK;
        }

        String suffix = "";
        if ((getWindow() != null) && getWindow().isModal()) {
            suffix = ".modal";
        }

        CellAttributes themed = getTheme().getColor(colorKey + suffix);
        if (themed == null) {
            themed = getTheme().getColor(colorKey);
        }

        CellAttributes color = new CellAttributes();
        if (themed != null) {
            color.setTo(themed);
        }
        // Tag the drawn cells with the OSC 8 hyperlink URI so that the
        // terminal renders them as a real clickable link.
        color.setHyperlink(uri);

        String text = getLabel();
        if (isMatchWindowBackground()) {
            putForegroundStringXY(0, 0, text, color);
        } else {
            putStringXY(0, 0, text, color);
        }
    }

    // ------------------------------------------------------------------------
    // THyperLink -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the hyperlink target URI.
     *
     * @return the URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Set the hyperlink target URI.
     *
     * @param uri the new URI
     */
    public void setUri(final String uri) {
        this.uri = uri;
    }

}
