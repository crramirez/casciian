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
import java.util.ArrayList;
import java.util.List;

import casciian.backend.SystemProperties;
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
    private final List<File> files;

    /**
     * Root path containing files to display.
     */
    private File path;

    /**
     * The list of filters that a file must match in order to be displayed.
     */
    private final List<String> filters;

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
        files = new ArrayList<>();
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
        File pathFile = new File(path);
        if (!pathFile.isAbsolute()) {
            pathFile = new File(SystemProperties.getUserDir(), path);
        }
        this.path = pathFile;

        files.clear();

        // Build a list of files in this directory
        File [] newFiles = this.path.listFiles();
        if (newFiles != null) {
            for (File newFile : newFiles) {
                if (newFile.getName().startsWith(".")) {
                    continue;
                }
                if (newFile.isDirectory()) {
                    continue;
                }
                if (filters != null) {
                    for (String pattern : filters) {

                        /*
                        System.err.println("newFiles[i] " +
                            newFiles[i].getName() + " " + pattern +
                            " " + newFiles[i].getName().matches(pattern));
                        */

                        if (newFile.getName().matches(pattern)) {
                            files.add(newFile);
                            break;
                        }
                    }
                } else {
                    files.add(newFile);
                }
            }
        }

        files.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        List<String> newStrings = new ArrayList<>();
        for (File file: files) {
            newStrings.add(renderFile(file));
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
