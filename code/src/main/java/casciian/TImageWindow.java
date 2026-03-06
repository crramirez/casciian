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
import java.util.ResourceBundle;

import casciian.bits.ImageRGB;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import casciian.event.TResizeEvent;
import casciian.image.decoders.ImageDecoderRegistry;

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

    /**
     * If true, the user is dragging to pan the image.
     */
    private boolean inImagePan = false;

    /**
     * Starting absolute X position of the mouse when panning began.
     */
    private int panMouseX;

    /**
     * Starting absolute Y position of the mouse when panning began.
     */
    private int panMouseY;

    /**
     * Image left offset when panning began.
     */
    private int panStartLeft;

    /**
     * Image top offset when panning began.
     */
    private int panStartTop;

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

        // Use the image decoder registry to decode the file
        ImageDecoderRegistry registry = ImageDecoderRegistry.getInstance();
        try {
            ImageRGB image = registry.decodeImage(file.toPath());

            imageField = addImage(0, 0, getWidth() - 2, getHeight() - 2,
                image, 0, 0);

            setTitle(file.getName());

            setupAfterImage();
        } catch (IOException e) {
            close();

            throw e;
        }
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
        } else if (mouse.isMouseWheelLeft()) {
            imageField.setLeft(imageField.getLeft() + wheelScrollSize);
        } else if (mouse.isMouseWheelRight()) {
            imageField.setLeft(imageField.getLeft() - wheelScrollSize);
        } else if (mouse.isMouse1()
            && !inMovements()
            && !mouseOnVerticalScroller(mouse)
            && !mouseOnHorizontalScroller(mouse)) {

            // Begin panning the image (hand/grab drag)
            inImagePan = true;
            panMouseX = mouse.getAbsoluteX();
            panMouseY = mouse.getAbsoluteY();
            panStartLeft = imageField.getLeft();
            panStartTop = imageField.getTop();
        }
        setVerticalValue(imageField.getTop());
        setHorizontalValue(imageField.getLeft());
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        inImagePan = false;

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
        if (inImagePan && mouse.isMouse1()) {
            // Pan the image: drag direction moves the visible content
            int deltaX = mouse.getAbsoluteX() - panMouseX;
            int deltaY = mouse.getAbsoluteY() - panMouseY;
            imageField.setLeft(panStartLeft - deltaX);
            imageField.setTop(panStartTop - deltaY);
            setVerticalValue(imageField.getTop());
            setHorizontalValue(imageField.getLeft());
            return;
        }

        // Not panning, stop any stale pan state
        inImagePan = false;

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
        if (kbUp.equals(keypress.getKey())) {
            verticalDecrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (kbDown.equals(keypress.getKey())) {
            verticalIncrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (kbPgUp.equals(keypress.getKey())) {
            bigVerticalDecrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (kbPgDn.equals(keypress.getKey())) {
            bigVerticalIncrement();
            imageField.setTop(getVerticalValue());
            return;
        }
        if (kbRight.equals(keypress.getKey())) {
            horizontalIncrement();
            imageField.setLeft(getHorizontalValue());
            return;
        }
        if (kbLeft.equals(keypress.getKey())) {
            horizontalDecrement();
            imageField.setLeft(getHorizontalValue());
            return;
        }

        if (!keypress.getKey().isFnKey()) {
            if (keypress.getKey().getChar() == '+') {
                // Make the image bigger.
                imageField.setScaleFactor(imageField.getScaleFactor() * 1.25);
                return;
            }
            if (keypress.getKey().getChar() == '-') {
                // Make the image smaller.
                imageField.setScaleFactor(imageField.getScaleFactor() * 0.80);
                return;
            }
        }
        if (keypress.matchesKey(kbAltUp)) {
            // Make the image bigger.
            imageField.setScaleFactor(imageField.getScaleFactor() * 1.25);
            return;
        }
        if (keypress.matchesKey(kbAltDown)) {
            // Make the image smaller.
            imageField.setScaleFactor(imageField.getScaleFactor() * 0.80);
            return;
        }
        if (keypress.matchesKey(kbAltRight)) {
            // Rotate clockwise.
            int angle = imageField.getRotation() + 90;
            if (angle == 360) {
                angle = 0;
            }
            imageField.setRotation(angle);
            return;
        }
        if (keypress.matchesKey(kbAltLeft)) {
            // Rotate counter-clockwise.
            int angle = imageField.getRotation() - 90;
            if (angle < 0) {
                angle += 360;
            }
            imageField.setRotation(angle);
            return;
        }

        if (keypress.matchesKey(kbShiftLeft)) {
            switch (imageField.getScaleType()) {
                case NONE:
                    imageField.setScaleType(TImage.Scale.SCALE);
                    return;
                case STRETCH:
                    imageField.setScaleType(TImage.Scale.NONE);
                    return;
                case SCALE:
                    imageField.setScaleType(TImage.Scale.STRETCH);
                    return;
            }
        }
        if (keypress.matchesKey(kbShiftRight)) {
            switch (imageField.getScaleType()) {
                case NONE:
                    imageField.setScaleType(TImage.Scale.STRETCH);
                    return;
                case STRETCH:
                    imageField.setScaleType(TImage.Scale.SCALE);
                    return;
                case SCALE:
                    imageField.setScaleType(TImage.Scale.NONE);
                    return;
            }
        }
        if (keypress.matchesKey(kbCtrlLeft)) {
            switch (imageField.getDisplayMode()) {
                case BITMAP:
                    imageField.setDisplayMode(TImage.DisplayMode.UNICODE_HALVES);
                    return;
                case BLOCKS:
                    imageField.setDisplayMode(TImage.DisplayMode.BITMAP);
                    return;
                case UNICODE_HALVES:
                    imageField.setDisplayMode(TImage.DisplayMode.BLOCKS);
                    return;
            }
        }
        if (keypress.matchesKey(kbCtrlRight)) {
            switch (imageField.getDisplayMode()) {
                case BITMAP:
                    imageField.setDisplayMode(TImage.DisplayMode.BLOCKS);
                    return;
                case BLOCKS:
                    imageField.setDisplayMode(TImage.DisplayMode.UNICODE_HALVES);
                    return;
                case UNICODE_HALVES:
                    imageField.setDisplayMode(TImage.DisplayMode.BITMAP);
                    return;
            }
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
