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

import casciian.bits.StringUtils;
import casciian.event.TCommandEvent;
import casciian.texteditor.Word;
import static casciian.TCommand.*;

/**
 * TPasswordField implements an editable text field that displays
 * stars/asterisks instead of the text it holds.
 *
 * <p>
 * It behaves like {@link TField}: the text can be selected with the mouse or
 * with shift + the navigation keys, and text can be pasted into it.  As is
 * customary for password entry, the text is never copied to the clipboard:
 * cut and copy do nothing.
 * </p>
 */
public class TPasswordField extends TField {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     */
    public TPasswordField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed) {

        this(parent, x, y, width, fixed, "", null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     */
    public TPasswordField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text) {

        this(parent, x, y, width, fixed, text, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     * @param enterAction function to call when enter key is pressed
     * @param updateAction function to call when the text is updated
     */
    public TPasswordField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text,
        final TAction enterAction, final TAction updateAction) {

        // Set parent and window
        super(parent, x, y, width, fixed, text, enterAction, updateAction);
    }

    // ------------------------------------------------------------------------
    // TField -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The password is never copied to the clipboard.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmCopy) || command.equals(cmCut)) {
            // Do not expose the password to the clipboard.
            return;
        }

        super.onCommand(command);
    }

    // ------------------------------------------------------------------------
    // TTextBase --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw stars instead of the text.  One star is drawn per display cell,
     * so that the cursor and the selection line up with the real text.
     *
     * @param word the word being drawn
     * @return as many stars as the word is wide
     */
    @Override
    protected String getDisplayText(final Word word) {
        int width = StringUtils.width(word.getText());
        StringBuilder stars = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            stars.append('*');
        }
        return stars.toString();
    }

    // ------------------------------------------------------------------------
    // EditMenuUser -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The password cannot be cut to the clipboard.
     *
     * @return false
     */
    @Override
    public boolean isEditMenuCut() {
        return false;
    }

    /**
     * The password cannot be copied to the clipboard.
     *
     * @return false
     */
    @Override
    public boolean isEditMenuCopy() {
        return false;
    }

}
