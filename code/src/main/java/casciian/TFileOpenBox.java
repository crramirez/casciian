/*
 * Casciian - Java Text User Interface
 *
 * Written 2013-2025 by Autumn Lamonte
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package casciian;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.layout.AnchoredLayoutManager;
import static casciian.TKeypress.*;

/**
 * TFileOpenBox is a system-modal dialog for selecting a file to open.  Call
 * it like:
 *
 * <pre>
 * {@code
 *     filename = fileOpenBox("/path/to/file.ext",
 *         TFileOpenBox.Type.OPEN);
 *     if (filename != null) {
 *         ... the user selected a file, go open it ...
 *     }
 * }
 * </pre>
 *
 */
public class TFileOpenBox extends TWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * TFileOpenBox can be called for either Open or Save actions.
     */
    public enum Type {
        /**
         * Button will be labeled "Open".
         */
        OPEN,

        /**
         * Button will be labeled "Save".
         */
        SAVE,

        /**
         * Button will be labeled "Select".
         */
        SELECT
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * String to return, or null if the user canceled.
     */
    private String filename = null;

    /**
     * The left-side tree view pane.
     */
    private TTreeViewScrollable treeView;

    /**
     * The data behind treeView.
     */
    private TDirectoryTreeItem treeViewRoot;

    /**
     * The right-side directory list pane.
     */
    private TDirectoryList directoryList;

    /**
     * The top row text field.
     */
    private TField entryField;

    /**
     * The Open or Save button.
     */
    private TButton openButton;

    /**
     * The type of box this is (OPEN, SAVE, or SELECT).
     */
    private Type type = Type.OPEN;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The file open box will be centered on screen.
     *
     * @param application the TApplication that manages this window
     * @param path path of selected file
     * @param type one of the Type constants
     * @throws IOException of a java.io operation throws
     */
    public TFileOpenBox(final TApplication application, final String path,
        final Type type) throws IOException {

        this(application, path, type, null);
    }

    /**
     * Public constructor.  The file open box will be centered on screen.
     *
     * @param application the TApplication that manages this window
     * @param path path of selected file
     * @param type one of the Type constants
     * @param filters a list of strings that files must match to be displayed
     * @throws IOException of a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TFileOpenBox(final TApplication application, final String path,
        final Type type, final List<String> filters) throws IOException {

        // Register with the TApplication
        super(application, "", 0, 0, 78, 22, MODAL | RESIZABLE);
        i18n = ResourceBundle.getBundle(TFileOpenBox.class.getName(),
            getLocale());

        setMinimumWindowWidth(getWidth());
        setMinimumWindowHeight(getHeight());
        AnchoredLayoutManager layout = new AnchoredLayoutManager(getWidth() - 2,
            getHeight() - 2);
        setLayoutManager(layout);

        TStatusBar statusBar = newStatusBar("");

        // Add text field
        entryField = addField(1, 1, getWidth() - 4, false,
            (new File(path)).getCanonicalPath(),
            new TAction() {
                public void DO() {
                    try {
                        checkFilename(entryField.getText());
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            }, null);
        entryField.end();

        // Add directory treeView
        treeView = addTreeViewWidget(1, 3, 30, getHeight() - 6,
            new TAction() {
                public void DO() {
                    TTreeItem item = treeView.getSelected();
                    File selectedDir = ((TDirectoryTreeItem) item).getFile();
                    try {
                        directoryList.setPath(selectedDir.getCanonicalPath());
                        entryField.setText(selectedDir.getCanonicalPath());
                        if (type == Type.OPEN) {
                            openButton.setEnabled(false);
                        }
                        activate(treeView);
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            }
        );
        treeViewRoot = new TDirectoryTreeItem(treeView, path, true);

        // Add directory files list
        directoryList = addDirectoryList(path, 35, 3, 27, getHeight() - 6,
            new TAction() {
                public void DO() {
                    try {
                        File newPath = directoryList.getPath();
                        entryField.setText(newPath.getCanonicalPath());
                        entryField.end();
                        openButton.setEnabled(true);
                        activate(entryField);
                        checkFilename(entryField.getText());
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            },
            new TAction() {
                public void DO() {
                    try {
                        File newPath = directoryList.getPath();
                        entryField.setText(newPath.getCanonicalPath());
                        entryField.end();
                        openButton.setEnabled(true);
                        activate(entryField);
                        getStatusBar().setText(entryField.getText());
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            },
            filters);

        layout.setAnchor(directoryList, treeView,
            AnchoredLayoutManager.Anchor.LEFT);

        String openLabel = "";
        switch (type) {
        case OPEN:
            openLabel = i18n.getString("openButton");
            setTitle(i18n.getString("openTitle"));
            break;
        case SAVE:
            openLabel = i18n.getString("saveButton");
            setTitle(i18n.getString("saveTitle"));
            break;
        case SELECT:
            openLabel = i18n.getString("selectButton");
            setTitle(i18n.getString("selectTitle"));
            break;
        default:
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        this.type = type;

        // Setup button actions
        int buttonX = this.getWidth() - StringUtils.width(openLabel) - 5;
        openButton = addButton(openLabel, buttonX, 3,
            new TAction() {
                public void DO() {
                    try {
                        checkFilename(entryField.getText());
                    } catch (IOException e) {
                        // SQUASH
                    }
                }
            }
        );
        if (type == Type.OPEN) {
            openButton.setEnabled(false);
        }
        layout.setAnchor(openButton, null,
            AnchoredLayoutManager.Anchor.TOP_RIGHT);

        TButton cancelButton = addButton(i18n.getString("cancelButton"),
            buttonX, 6,
            new TAction() {
                public void DO() {
                    filename = null;
                    getApplication().closeWindow(TFileOpenBox.this);
                }
            }
        );
        layout.setAnchor(cancelButton, null,
            AnchoredLayoutManager.Anchor.TOP_RIGHT);

        switch (type) {
        case SAVE:
            // Save dialog: activate the filename field.
            entryField.setText(entryField.getText() + File.separator);
            entryField.end();
            activate(entryField);
            break;

        default:
            // Default: activate the directory list.
            activate(directoryList);
            break;
        }

        // Set status bar text to first filename
        if (directoryList.getMaxSelectedIndex() > 0) {
            getStatusBar().setText(directoryList.getPath().
                getCanonicalPath());
        }

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
            // Close window
            filename = null;
            getApplication().closeWindow(this);
            return;
        }

        if (directoryList.isActive()) {
            if ((keypress.equals(kbUp))
                || (keypress.equals(kbDown))
                || (keypress.equals(kbPgUp))
                || (keypress.equals(kbPgDn))
                || (keypress.equals(kbHome))
                || (keypress.equals(kbEnd))
            ) {
                // Directory list will be changing, update the status bar.
                super.onKeypress(keypress);

                try {
                    getStatusBar().setText(directoryList.getPath().
                        getCanonicalPath());
                } catch (IOException e) {
                    getStatusBar().setText("");
                }
                return;
            }
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
                // Tree view will be changing, update the directory list.
                super.onKeypress(keypress);

                // This is the same action as treeView's enter.
                TTreeItem item = treeView.getSelected();
                File selectedDir = ((TDirectoryTreeItem) item).getFile();
                try {
                    directoryList.setPath(selectedDir.getCanonicalPath());
                    if (type == Type.OPEN) {
                        openButton.setEnabled(false);
                    }
                    activate(treeView);
                } catch (IOException e) {
                    // SQUASH
                }
                getStatusBar().setText("");
                return;
            }
        }

        // Pass to my parent
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw me on screen.
     */
    @Override
    public void draw() {
        super.draw();
        int columnX = treeView.getX() + treeView.getWidth() + 2;
        int columnY = treeView.getY() + 1;
        vLineXY(columnX, columnY, treeView.getHeight(),
            GraphicsChars.WINDOW_SIDE, getBackground());
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, disable any window closing effect.  This is used by the
     * window closing effects themselves so that they can be closed when
     * finished.
     *
     * @return true if the window close effect should be disabled
     */
    @Override
    public boolean disableCloseEffect() {
        // Let's let this particular dialog vanish immediately.
        return true;
    }

    // ------------------------------------------------------------------------
    // TFileOpenBox -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the return string.
     *
     * @return the filename the user selected, or null if they canceled.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * See if there is a valid filename to return.  If the filename is a
     * directory, then switch the tree view and directory list to that
     * directory.
     *
     * @param newFilename the filename to check and return
     * @throws IOException of a java.io operation throws
     */
    private void checkFilename(final String newFilename) throws IOException {
        File newFile = new File(newFilename);
        if (newFile.exists()) {
            if (newFile.isFile() || (type == Type.SELECT)) {
                filename = newFilename;
                getApplication().closeWindow(this);
                return;
            }
            if (newFile.isDirectory()) {
                treeViewRoot = new TDirectoryTreeItem(treeView,
                    newFilename, true);
                treeView.setTreeRoot(treeViewRoot, true);
                if (type == Type.OPEN) {
                    openButton.setEnabled(false);
                }
                directoryList.setPath(newFilename);
            }
        } else if (type != Type.OPEN) {
            filename = newFilename;
            getApplication().closeWindow(this);
            return;
        }
    }

}
