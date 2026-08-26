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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Modal dialog for activating or closing an open modeless window.
 */
public class TWindowList extends TDialog {

    /**
     * Window list dialog result.
     */
    public enum Result {
        /**
         * A window was selected for activation.
         */
        OK,

        /**
         * The dialog was cancelled.
         */
        CANCEL
    }

    /**
     * Localization bundle name.
     */
    public static final String RESOURCE_BUNDLE_NAME =
        TWindowList.class.getName() + "Bundle";

    private final TList windowList;
    private final TButton closeButton;
    private final List<TWindow> windows = new ArrayList<>();
    private Result result = Result.CANCEL;

    /**
     * Public constructor.
     *
     * @param application application managing the open windows
     */
    @SuppressWarnings("this-escape")
    public TWindowList(final TApplication application) {
        super(application, "", 57, 15, MODAL);

        ResourceBundle i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME,
            getLocale());
        setTitle(i18n.getString("title"));

        int row = 1;
        windowList = addLabelFor(i18n.getString("windowsLabel"), 2, row,
            addList(new ArrayList<>(), 2, row + 1, 40, 11,
                new TAction() {
                    public void DO() {
                        doOk();
                    }
                })
        );
        windowList.getHorizontalScroller().setVisible(false);

        row++;
        TButton okButton = addButton(i18n.getString("okButton"), 43, row,
            this::doOk);
        setDefaultButton(okButton);
        row += 3;
        closeButton = addButton(i18n.getString("closeButton"), 43, row,
            this::closeSelected);
        row += 6;
        addButton(i18n.getString("cancelButton"), 43, row, this::doCancel);

        refreshWindows(findPreviouslyActiveWindow());
        activate(windowList);
    }

    private TWindow findPreviouslyActiveWindow() {
        return getApplication().getAllWindows().stream()
            .filter(window -> window != this && !window.isModal())
            .min(Comparator.comparingInt(TWindow::getZ))
            .orElse(null);
    }

    private void refreshWindows(final TWindow preferredWindow) {
        windows.clear();
        getApplication().getAllWindows().stream()
            .filter(window -> window != this && !window.isModal())
            .sorted(Comparator.comparingInt(TWindow::getWindowNumber))
            .forEach(windows::add);

        List<String> titles = windows.stream()
            .map(window -> window.getTitle() == null ? "" : window.getTitle())
            .toList();
        windowList.setList(titles);

        int selectedIndex = windows.indexOf(preferredWindow);
        if (selectedIndex < 0 && !windows.isEmpty()) {
            selectedIndex = 0;
        }
        windowList.setSelectedIndex(selectedIndex);
        closeButton.setEnabled(!windows.isEmpty());
    }

    private void doOk() {
        int selectedIndex = windowList.getSelectedIndex();
        TWindow selected = null;
        if (selectedIndex >= 0 && selectedIndex < windows.size()) {
            selected = windows.get(selectedIndex);
        }
        result = Result.OK;
        getApplication().closeWindow(this);
        if (selected != null) {
            getApplication().activateWindow(selected);
        }
    }

    private void closeSelected() {
        int selectedIndex = windowList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= windows.size()) {
            return;
        }

        TWindow selected = windows.get(selectedIndex);
        getApplication().closeWindow(selected);
        refreshWindows(null);
        if (!windows.isEmpty()) {
            windowList.setSelectedIndex(Math.min(selectedIndex,
                    windows.size() - 1));
        }
    }

    private void doCancel() {
        result = Result.CANCEL;
        getApplication().closeWindow(this);
    }

    /**
     * Get the dialog result.
     *
     * @return OK when a window was accepted, otherwise CANCEL
     */
    public final Result getResult() {
        return result;
    }

    /**
     * Get the window list control.
     *
     * @return the list control
     */
    public final TList getWindowList() {
        return windowList;
    }

    /**
     * Cancel without changing or deleting a modeless window.
     *
     * @return true
     */
    @Override
    protected boolean onCancel() {
        doCancel();
        return true;
    }
}
