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
import java.util.ArrayList;
import java.util.List;

import casciian.bits.StringUtils;

/**
 * TDirectoryList shows the files within a directory.
 */
public class TDirectoryList extends TList {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Files in the directory, with the same index as the TList strings
     * variable.
     */
    private List<File> files;

    /**
     * Root path containing files to display.
     */
    private File path;

    /**
     * The list of filters that a file must match in order to be displayed.
     */
    private List<String> filters;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param path directory path, must be a directory
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    public TDirectoryList(final TWidget parent, final String path, final int x,
        final int y, final int width, final int height) {

        this(parent, path, x, y, width, height, null, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param path directory path, must be a directory
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param action action to perform when an item is selected (enter or
     * double-click)
     */
    public TDirectoryList(final TWidget parent, final String path, final int x,
        final int y, final int width, final int height, final TAction action) {

        this(parent, path, x, y, width, height, action, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param path directory path, must be a directory
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param action action to perform when an item is selected (enter or
     * double-click)
     * @param singleClickAction action to perform when an item is selected
     * (single-click)
     */
    public TDirectoryList(final TWidget parent, final String path, final int x,
        final int y, final int width, final int height, final TAction action,
        final TAction singleClickAction) {

        this(parent, path, x, y, width, height, action, singleClickAction,
            null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param path directory path, must be a directory
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param action action to perform when an item is selected (enter or
     * double-click)
     * @param singleClickAction action to perform when an item is selected
     * (single-click)
     * @param filters a list of strings that files must match to be displayed
     */
    @SuppressWarnings("this-escape")
    public TDirectoryList(final TWidget parent, final String path, final int x,
        final int y, final int width, final int height, final TAction action,
        final TAction singleClickAction, final List<String> filters) {

        super(parent, null, x, y, width, height, action);
        files = new ArrayList<File>();
        this.filters = filters;
        this.singleClickAction = singleClickAction;

        setPath(path);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // TList ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Resize for a new width/height.
     */
    @Override
    public void reflowData() {
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                String displayName = renderFile(files.get(i));
                setListItem(i, displayName);
            }
        }
        super.reflowData();
    }

    // ------------------------------------------------------------------------
    // TDirectoryList ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the new path to display.
     *
     * @param path new path to list files for
     */
    public void setPath(final String path) {
        this.path = new File(path);

        List<String> newStrings = new ArrayList<String>();
        files.clear();

        // Build a list of files in this directory
        File [] newFiles = this.path.listFiles();
        if (newFiles != null) {
            for (int i = 0; i < newFiles.length; i++) {
                if (newFiles[i].getName().startsWith(".")) {
                    continue;
                }
                if (newFiles[i].isDirectory()) {
                    continue;
                }
                if (filters != null) {
                    for (String pattern: filters) {

                        /*
                        System.err.println("newFiles[i] " +
                            newFiles[i].getName() + " " + pattern +
                            " " + newFiles[i].getName().matches(pattern));
                        */

                        if (newFiles[i].getName().matches(pattern)) {
                            String str = renderFile(newFiles[i]);
                            files.add(newFiles[i]);
                            newStrings.add(str);
                            break;
                        }
                    }
                } else {
                    String str = renderFile(newFiles[i]);
                    files.add(newFiles[i]);
                    newStrings.add(str);
                }
            }
        }
        assert (newStrings.size() == files.size());
        setList(newStrings);

        // Select the first entry
        if (getMaxSelectedIndex() >= 0) {
            setSelectedIndex(0);
        }
    }

    /**
     * Get the path that is being displayed.
     *
     * @return the path
     */
    public File getPath() {
        path = files.get(getSelectedIndex());
        return path;
    }

    /**
     * Format one of the entries for drawing on the screen.
     *
     * @param file the File
     * @return the line to draw
     */
    private String renderFile(final File file) {
        String name = file.getName();
        int maxWidth = getWidth() - 8;
        if (StringUtils.width(name) > maxWidth) {
            name = name.substring(0, maxWidth - 3) + "...";
        }
        return String.format("%-" + maxWidth + "s %5dk", name,
            (file.length() / 1024));
    }

}
