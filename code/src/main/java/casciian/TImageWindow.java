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
import java.nio.file.Files;
import java.util.ResourceBundle;

import casciian.bits.ImageRGB;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.terminal.SixelDecoder;

import static casciian.TKeypress.*;

/**
 * TImageWindow shows an image with scrollbars.
 */
public class TImageWindow extends TScrollableWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = TImageWindow.class.getName() + "Bundle";

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    private static final int wheelScrollSize = 3;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Translated strings.
     */
    private ResourceBundle i18n = null;

    /**
     * Hang onto the TImage so I can resize it with the window.
     */
    private TImage imageField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor opens a file.
     *
     * @param parent the main application
     * @param file the file to open
     * @throws IOException if a java.io operation throws
     */
    public TImageWindow(final TApplication parent,
        final File file) throws IOException {

        this(parent, file, 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop());
    }

    /**
     * Public constructor opens a file.
     *
     * @param parent the main application
     * @param file the file to open
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @throws IOException if a java.io operation throws
     */
    @SuppressWarnings("this-escape")
    public TImageWindow(final TApplication parent, final File file,
        final int x, final int y, final int width,
        final int height) throws IOException {

        super(parent, file.getName(), x, y, width, height, RESIZABLE);
        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, getLocale());

        ImageRGB image = null;
        if (file.getName().toLowerCase().endsWith(".six")) {
            image = new SixelDecoder(Files.readString(file.toPath()), null, 0xFFFFFF, true).getImage();
            imageField = addImage(0, 0, getWidth() - 2, getHeight() - 2,
                image, 0, 0);
        }

        setTitle(file.getName());

        setupAfterImage();
    }

    /**
     * Setup other fields after the image is created.
     */
    private void setupAfterImage() {
        if (imageField.getRows() < getHeight() - 2) {
            imageField.setHeight(imageField.getRows());
            setHeight(imageField.getRows() + 2);
        }
        if (imageField.getColumns() < getWidth() - 2) {
            imageField.setWidth(imageField.getColumns());
            setWidth(imageField.getColumns() + 2);
        }

        hScroller = new THScroller(this,
            Math.min(Math.max(0, getWidth() - 17), 17),
            getHeight() - 2,
            getWidth() - Math.min(Math.max(0, getWidth() - 17), 17) - 3);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        setTopValue(0);
        setBottomValue(imageField.getRows() - imageField.getHeight());
        setLeftValue(0);
        setRightValue(imageField.getColumns() - imageField.getWidth());

        statusBar = newStatusBar(i18n.getString("statusBar"));
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseDown(mouse);

        if (mouse.isMouseWheelUp()) {
            imageField.setTop(imageField.getTop() - wheelScrollSize);
        } else if (mouse.isMouseWheelDown()) {
            imageField.setTop(imageField.getTop() + wheelScrollSize);
        }
        setVerticalValue(imageField.getTop());
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseUp(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar
            imageField.setTop(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked/dragged on horizontal scrollbar
            imageField.setLeft(getHorizontalValue());
        }
    }

    /**
     * Method that subclasses can override to handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseMotion(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar
            imageField.setTop(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked/dragged on horizontal scrollbar
            imageField.setLeft(getHorizontalValue());
        }
    }

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the image field
            TResizeEvent imageSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2);
            imageField.onResize(imageSize);

            // Have TScrollableWindow handle the scrollbars
            super.onResize(event);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbUp)) {
            verticalDecrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (keypress.equals(kbDown)) {
            verticalIncrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (keypress.equals(kbPgUp)) {
            bigVerticalDecrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (keypress.equals(kbPgDn)) {
            bigVerticalIncrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (keypress.equals(kbRight)) {
            horizontalIncrement();
            imageField.setLeft(getHorizontalValue());
            return;
        }
        if (keypress.equals(kbLeft)) {
            horizontalDecrement();
            imageField.setLeft(getHorizontalValue());
            return;
        }

        // We did not take it, let the TImage instance see it.
        super.onKeypress(keypress);

        setVerticalValue(imageField.getTop());
        setBottomValue(imageField.getRows() - imageField.getHeight());
        setHorizontalValue(imageField.getLeft());
        setRightValue(imageField.getColumns() - imageField.getWidth());
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the window.
     */
    @Override
    public void draw() {
        // Draw as normal.
        super.draw();

        // We have to get the scrollbar values after we have let the image
        // try to draw.
        setBottomValue(imageField.getRows() - imageField.getHeight());
        setRightValue(imageField.getColumns() - imageField.getWidth());
    }

}
