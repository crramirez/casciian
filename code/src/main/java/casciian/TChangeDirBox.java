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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.layout.AnchoredLayoutManager;
import static casciian.TKeypress.*;

/**
 * TChangeDirBox is a system-modal dialog for selecting a new working
 * directory.  It shows a combobox with previously visited directories
 * at the top, a directory tree view below, and OK / Chdir / Revert
 * buttons on the right.
 */
public class TChangeDirBox extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME =
        TChangeDirBox.class.getName() + "Bundle";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * The combobox showing the selected directory path and history.
     */
    private TComboBox dirComboBox;

    /**
     * The directory tree view.
     */
    private TTreeViewScrollable treeView;

    /**
     * The data behind treeView.
     */
    private TDirectoryTreeItem treeViewRoot;

    /**
     * The directory path when the dialog was opened (for Revert).
     */
    private String originalDir;

    /**
     * Session history of directory changes.
     */
    private static List<String> dirHistory = new ArrayList<>();

    /**
     * The resulting directory path, or null if the user cancelled.
     */
    private String result = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The change dir box will be centered on screen.
     *
     * @param application the TApplication that manages this window
     * @param path the initial directory path
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TChangeDirBox(final TApplication application,
        final String path) throws IOException {

        // Register with the TApplication
        super(application, "", 0, 0, 64, 18, MODAL | RESIZABLE);
        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME,
            getLocale());
        setTitle(i18n.getString("title"));

        setMinimumWindowWidth(getWidth());
        setMinimumWindowHeight(getHeight());
        AnchoredLayoutManager layout = new AnchoredLayoutManager(
            getWidth() - 2, getHeight() - 2);
        setLayoutManager(layout);

        originalDir = (new File(path)).getCanonicalPath();

        // Initialize the history list if it doesn't already contain the
        // current path
        if (!dirHistory.contains(originalDir)) {
            dirHistory.add(originalDir);
        }

        // Add combobox at the top with directory history
        dirComboBox = addComboBox(1, 1, getWidth() - 4,
            new ArrayList<>(dirHistory),
            dirHistory.indexOf(originalDir), 5, null);

        // Add directory tree view
        treeView = addTreeViewWidget(1, 3, getWidth() - 16,
            getHeight() - 6,
            new TAction() {
                public void DO() {
                    TTreeItem item = treeView.getSelected();
                    File selectedDir =
                        ((TDirectoryTreeItem) item).getFile();
                    try {
                        dirComboBox.setText(
                            selectedDir.getCanonicalPath());
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            }
        );
        treeViewRoot = new TDirectoryTreeItem(treeView, path, true);

        // Setup buttons on the right
        String okLabel = i18n.getString("okButton");
        String chdirLabel = i18n.getString("chdirButton");
        String revertLabel = i18n.getString("revertButton");

        int buttonX = getWidth() - StringUtils.width(okLabel) - 5;

        // OK button: accept the current combobox value as the result
        TButton okButton = addButton(okLabel, buttonX, 3,
            new TAction() {
                public void DO() {
                    result = dirComboBox.getText();
                    try {
                        changeToDirectory(result);
                    } catch (IOException e) {
                        // SQUASH
                    }
                    getApplication().closeWindow(TChangeDirBox.this);
                }
            }
        );
        layout.setAnchor(okButton, null,
            AnchoredLayoutManager.Anchor.TOP_RIGHT);

        // Chdir button: change to the selected directory, update combo
        TButton chdirButton = addButton(chdirLabel, buttonX, 6,
            new TAction() {
                public void DO() {
                    String dirPath = dirComboBox.getText();
                    try {
                        changeToDirectory(dirPath);
                        updateComboBox(dirPath);
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            }
        );
        layout.setAnchor(chdirButton, null,
            AnchoredLayoutManager.Anchor.TOP_RIGHT);

        // Revert button: revert to the directory when dialog was opened
        TButton revertButton = addButton(revertLabel, buttonX, 9,
            new TAction() {
                public void DO() {
                    try {
                        changeToDirectory(originalDir);
                        dirComboBox.setText(originalDir);
                        treeViewRoot = new TDirectoryTreeItem(treeView,
                            originalDir, true);
                        treeView.setTreeRoot(treeViewRoot, true);
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            }
        );
        layout.setAnchor(revertButton, null,
            AnchoredLayoutManager.Anchor.TOP_RIGHT);

        // Default: activate the tree view
        activate(treeView);

        // Set the secondaryFiber to run me
        getApplication().enableSecondaryEventReceiver(this);

        // Yield to the secondary thread.  When I come back from the
        // constructor response will already be set.
        getApplication().yield();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Escape - behave like cancel
        if (keypress.equals(kbEsc)) {
            result = null;
            getApplication().closeWindow(this);
            return;
        }

        if (treeView.isActive()) {
            if ((keypress.equals(kbEnter))
                || (keypress.equals(kbUp))
                || (keypress.equals(kbDown))
                || (keypress.equals(kbPgUp))
                || (keypress.equals(kbPgDn))
                || (keypress.equals(kbHome))
                || (keypress.equals(kbEnd))
            ) {
                // Tree view will be changing, update the combobox.
                super.onKeypress(keypress);

                TTreeItem item = treeView.getSelected();
                File selectedDir =
                    ((TDirectoryTreeItem) item).getFile();
                try {
                    dirComboBox.setText(
                        selectedDir.getCanonicalPath());
                } catch (IOException e) {
                    // SQUASH
                }
                return;
            }
        }

        // Pass to my parent
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Disable window closing effect for this dialog.
     *
     * @return true if the window close effect should be disabled
     */
    @Override
    public boolean disableCloseEffect() {
        return true;
    }

    // ------------------------------------------------------------------------
    // TChangeDirBox ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the return string.
     *
     * @return the directory the user selected, or null if they cancelled.
     */
    public String getResult() {
        return result;
    }

    /**
     * Change the current working directory to the given path.
     *
     * @param dirPath the directory to change to
     * @throws IOException if the path is not a valid directory
     */
    private void changeToDirectory(final String dirPath)
        throws IOException {

        File dir = new File(dirPath).getCanonicalFile();
        if (dir.isDirectory()) {
            System.setProperty("user.dir", dir.getCanonicalPath());
        }
    }

    /**
     * Update the combobox with a new directory path and add it to the
     * session history.
     *
     * @param dirPath the new directory path
     * @throws IOException if a java.io operation throws
     */
    private void updateComboBox(final String dirPath) throws IOException {
        String canonical = new File(dirPath).getCanonicalPath();
        if (!dirHistory.contains(canonical)) {
            dirHistory.add(canonical);
        }
        dirComboBox.setList(new ArrayList<>(dirHistory));
        dirComboBox.setText(canonical);
    }

}
