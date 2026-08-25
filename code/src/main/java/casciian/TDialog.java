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

/**
 * TDialog is a {@link TWindow} whose default behaviour is suited to dialog
 * interaction.
 *
 * <h2>Relationship to TWindow</h2>
 * TDialog extends TWindow and adds two things:
 * <ul>
 *   <li>A default flag set of {@link #DEFAULT_FLAGS} ({@code MODAL}), so a
 *       plain {@code new TDialog(app, "Title", 40, 10)} is modal without
 *       requiring the caller to supply flags.</li>
 *   <li>A default {@link #onCancel()} implementation that closes the window,
 *       giving every modal TDialog standard Escape-to-close behaviour through
 *       the existing {@code TWindow} Escape-routing mechanism.</li>
 * </ul>
 *
 * <h2>Explicit flags always win</h2>
 * When flags are supplied explicitly they are passed through unchanged:
 * <pre>
 *     new TDialog(app, "Login", 40, 10, MODAL | NOCLOSEBOX);
 * </pre>
 * This creates a modal dialog <em>without</em> a close box – the user must
 * interact with the dialog's buttons.  Escape still triggers
 * {@link #onCancel()} as long as the window is modal.
 *
 * <h2>Modeless dialogs</h2>
 * Although unusual, a modeless TDialog is valid:
 * <pre>
 *     new TDialog(app, "Tool Palette", 40, 10, RESIZABLE);
 * </pre>
 * A modeless dialog does not acquire the modal Escape-cancellation behaviour
 * (the existing {@code TWindow} guard prevents it).
 *
 * <h2>Default buttons</h2>
 * Default button support is unchanged and provided by {@link TWindow}:
 * <pre>
 *     TButton login = addButton("Login", 20, 6, this::login);
 *     setDefaultButton(login);
 * </pre>
 *
 * <h2>Overriding cancellation</h2>
 * Subclasses can customise or suppress Escape cancellation:
 * <pre>
 *     {@literal @}Override
 *     protected boolean onCancel() {
 *         // Cancel additional state before closing.
 *         myResult = null;
 *         return super.onCancel();
 *     }
 * </pre>
 * A dialog that must not be dismissed with Escape can return {@code false}:
 * <pre>
 *     {@literal @}Override
 *     protected boolean onCancel() {
 *         return false;   // Escape ignored; user must press a button
 *     }
 * </pre>
 *
 * <h2>Typical usage</h2>
 * <pre>
 * public class LoginDialog extends TDialog {
 *
 *     public LoginDialog(final TApplication application) {
 *         super(application, "Login", 40, 10, MODAL | NOCLOSEBOX);
 *
 *         TButton login = addButton("Login", 20, 6, this::login);
 *         addButton("Cancel", 8, 6,
 *             () -&gt; getApplication().closeWindow(this));
 *
 *         setDefaultButton(login);
 *     }
 * }
 * </pre>
 */
public class TDialog extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default window flags used by constructors that do not take an explicit
     * {@code flags} argument.  Equal to {@link TWindow#MODAL}.
     */
    public static final int DEFAULT_FLAGS = MODAL;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The dialog will be modal and centered.
     *
     * @param application TApplication that manages this window
     * @param title       window title, centered along the top border
     * @param width       width of dialog
     * @param height      height of dialog
     */
    public TDialog(final TApplication application, final String title,
        final int width, final int height) {

        this(application, title, 0, 0, width, height, DEFAULT_FLAGS);
    }

    /**
     * Public constructor with explicit flags.
     *
     * @param application TApplication that manages this window
     * @param title       window title, centered along the top border
     * @param width       width of dialog
     * @param height      height of dialog
     * @param flags       bitmask of {@code RESIZABLE}, {@code MODAL},
     *                    {@code CENTERED}, {@code NOCLOSEBOX}, etc.
     *                    Passed through unchanged; {@code MODAL} is
     *                    <em>not</em> added automatically.
     */
    public TDialog(final TApplication application, final String title,
        final int width, final int height, final int flags) {

        this(application, title, 0, 0, width, height, flags);
    }

    /**
     * Public constructor.  The dialog will be modal and centered.
     *
     * @param application TApplication that manages this window
     * @param title       window title, centered along the top border
     * @param x           column relative to parent
     * @param y           row relative to parent
     * @param width       width of dialog
     * @param height      height of dialog
     */
    public TDialog(final TApplication application, final String title,
        final int x, final int y, final int width, final int height) {

        this(application, title, x, y, width, height, DEFAULT_FLAGS);
    }

    /**
     * Public constructor with explicit flags.
     *
     * @param application TApplication that manages this window
     * @param title       window title, centered along the top border
     * @param x           column relative to parent
     * @param y           row relative to parent
     * @param width       width of dialog
     * @param height      height of dialog
     * @param flags       bitmask of {@code RESIZABLE}, {@code MODAL},
     *                    {@code CENTERED}, {@code NOCLOSEBOX}, etc.
     *                    Passed through unchanged; {@code MODAL} is
     *                    <em>not</em> added automatically.
     */
    public TDialog(final TApplication application, final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        super(application, title, x, y, width, height, flags);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Standard dialog cancellation: closes the dialog window.
     *
     * <p>This implementation is invoked by the modal Escape-routing
     * mechanism in {@code TWindow} when no focused widget claims the Escape
     * key first.  Subclasses may override this method to add state cleanup
     * before closing, or return {@code false} to prevent Escape from
     * dismissing the dialog.</p>
     *
     * @return {@code true}, indicating the cancellation was handled
     */
    @Override
    protected boolean onCancel() {
        getApplication().closeWindow(this);
        return true;
    }

}
