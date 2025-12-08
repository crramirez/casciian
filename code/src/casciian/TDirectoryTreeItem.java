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
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

/**
 * TDirectoryTreeItem is a single item in a disk directory tree view.
 */
public class TDirectoryTreeItem extends TTreeItem {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * File corresponding to this list item.
     */
    private File file;

    /**
     * The TTreeViewScrollable containing this directory tree.
     */
    private TTreeViewScrollable treeViewWidget;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param view root TTreeViewScrollable
     * @param text text for this item
     * @param expanded if true, have it expanded immediately
     * @throws IOException if a java.io operation throws
     */
    public TDirectoryTreeItem(final TTreeViewScrollable view, final String text,
        final boolean expanded) throws IOException {

        this(view, text, expanded, true);
    }

    /**
     * Public constructor.
     *
     * @param view root TTreeViewScrollable
     * @param text text for this item
     * @param expanded if true, have it expanded immediately
     * @param openParents if true, expand all paths up the root path and
     * return the root path entry
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TDirectoryTreeItem(final TTreeViewScrollable view, final String text,
        final boolean expanded, final boolean openParents) throws IOException {

        super(view.getTreeView(), text, false);

        this.treeViewWidget = view;

        List<String> parentFiles = new LinkedList<String>();
        boolean oldExpanded = expanded;

        // Convert to canonical path
        File rootFile = new File(text);
        rootFile = rootFile.getCanonicalFile();

        if (openParents) {
            setExpanded(true);

            // Go up the directory tree
            File parent = rootFile.getParentFile();
            while (parent != null) {
                parentFiles.add(rootFile.getName());
                rootFile = rootFile.getParentFile();
                parent = rootFile.getParentFile();
            }
        }
        file = rootFile;
        if (rootFile.getParentFile() == null) {
            // This is a filesystem root, use its full name
            setText(rootFile.getCanonicalPath());
        } else {
            // This is a relative path.  We got here because openParents was
            // false.
            assert (!openParents);
            setText(rootFile.getName());
        }
        onExpand();

        if (openParents) {
            TDirectoryTreeItem childFile = this;
            Collections.reverse(parentFiles);
            for (String p: parentFiles) {
                for (TWidget widget: childFile.getChildren()) {
                    TDirectoryTreeItem child = (TDirectoryTreeItem) widget;
                    if (child.getText().equals(p)) {
                        childFile = child;
                        childFile.setExpanded(true);
                        childFile.onExpand();
                        break;
                    }
                }
            }
            unselect();
            getTreeView().setSelected(childFile, true);
            setExpanded(oldExpanded);
        }

        view.reflowData();
    }

    // ------------------------------------------------------------------------
    // TTreeItem --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the File corresponding to this list item.
     *
     * @return the File
     */
    public final File getFile() {
        return file;
    }

    /**
     * Called when this item is expanded or collapsed.  this.expanded will be
     * true if this item was just expanded from a mouse click or keypress.
     */
    @Override
    public final void onExpand() {
        // System.err.printf("onExpand() %s\n", file);

        if (file == null) {
            return;
        }
        getChildren().clear();

        // Make sure we can read it before trying to.
        if (file.canRead()) {
            setSelectable(true);
        } else {
            setSelectable(false);
        }
        assert (file.isDirectory());
        setExpandable(true);

        if (!isExpanded() || !isExpandable()) {
            return;
        }

        File [] listFiles = file.listFiles();
        if (listFiles != null) {
            for (File f: listFiles) {
                // System.err.printf("   -> file %s %s\n", file, file.getName());

                if (f.getName().startsWith(".")) {
                    // Hide dot-files
                    continue;
                }
                if (!f.isDirectory()) {
                    continue;
                }

                try {
                    TDirectoryTreeItem item = new TDirectoryTreeItem(treeViewWidget,
                        f.getCanonicalPath(), false, false);

                    item.level = this.level + 1;
                    getChildren().add(item);
                } catch (IOException e) {
                    continue;
                }
            }
        }
        Collections.sort(getChildren());
    }

}
