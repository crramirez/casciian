/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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

import java.io.IOException;

import casciian.bits.CellAttributes;
import casciian.event.TResizeEvent;

/**
 * TEditor displays an editable text document.  It is unaware of
 * scrolling behavior, but can respond to mouse and keyboard events.
 *
 * <p>
 * The text model, cursor movement, selection, clipboard and undo behavior
 * are provided by {@link TTextBase}.
 * </p>
 */
public class TEditor extends TTextBase {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * An optional margin to display, or 0 for no margin.
     */
    private int margin = 0;

    /**
     * If true, automatically reflow text to fit the margin.
     */
    private boolean autoWrap = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text text on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    @SuppressWarnings("this-escape")
    public TEditor(final TWidget parent, final String text, final int x,
        final int y, final int width, final int height) {

        super(parent, text, x, y, width, height, "teditor");

        setCursorVisible(true);
        setMouseStyle("text");
    }

    // ------------------------------------------------------------------------
    // TTextBase --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * TEditor allows the text to be modified.
     *
     * @return true
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Method that subclasses can override to handle window/screen resize
     * events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        // Change my width/height, and pull the cursor in as needed.
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            setWidth(resize.getWidth());
            setHeight(resize.getHeight());
            // See if the cursor is now outside the window, and if so move
            // things.
            if (getCursorX() >= getWidth()) {
                setLeftColumn(getLeftColumn() + getCursorX()
                    - (getWidth() - 1));
                setCursorX(getWidth() - 1);
            }
            if (getCursorY() >= getHeight()) {
                setTopLine(getTopLine() + getCursorY() - (getHeight() - 1));
                setCursorY(getHeight() - 1);
            }
        } else {
            // Let superclass handle it
            super.onResize(resize);
        }
    }

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        drawDocument();

        if (margin > 0) {
            CellAttributes marginColor = getWidgetColor("teditor.margin");
            for (int i = 0; i < getHeight(); i++) {
                putAttrXY(margin - 1 - getLeftColumn(), i, marginColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TEditor ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the wrapping behavior.
     *
     * @return true if the editor automatically wraps text to fit in the
     * margin
     */
    public boolean isAutoWrap() {
        return autoWrap;
    }

    /**
     * Set the wrapping behavior.
     *
     * @param autoWrap if true, automatically wrap text to fit in the margin
     */
    public void setAutoWrap(final boolean autoWrap) {
        this.autoWrap = autoWrap;
    }

    /**
     * Set the right margin.
     *
     * @param margin column number, or 0 to disable
     */
    public void setMargin(final int margin) {
        this.margin = margin;
        if (autoWrap) {
            wrapText();
        }
    }

    /**
     * Get the dirty value.
     *
     * @return true if the buffer is dirty
     */
    public boolean isDirty() {
        return document.isDirty();
    }

    /**
     * Unset the dirty flag.
     */
    public void setNotDirty() {
        document.setNotDirty();
    }

    /**
     * Save contents to file.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToFilename(final String filename) throws IOException {
        document.saveToFilename(filename);
    }

    /**
     * Reflow the text to fit inside the margin.
     */
    public void wrapText() {
        if (margin > 0) {
            document.wrapText(margin);
            alignDocument(true);
        }
    }

    /**
     * Trim trailing whitespace from lines and trailing empty
     * lines from the document.
     */
    public void cleanWhitespace() {
        document.cleanWhitespace();
        setCursorY(document.getLineNumber() - getTopLine());
        alignCursor();
    }

    /**
     * Set keyword highlighting.
     *
     * @param enabled if true, enable keyword highlighting
     */
    public void setHighlighting(final boolean enabled) {
        document.setHighlighting(enabled);
    }

}
