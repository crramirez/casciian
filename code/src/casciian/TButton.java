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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import casciian.bits.BorderStyle;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.Color;
import casciian.bits.GraphicsChars;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;
import casciian.event.TKeypressEvent;
import casciian.event.TMouseEvent;
import static casciian.TKeypress.kbEnter;
import static casciian.TKeypress.kbSpace;

/**
 * TButton implements a simple button.  To make the button do something, pass
 * a TAction class to its constructor.
 *
 * @see TAction#DO()
 */
public class TButton extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available styles of the button.
     */
    public static enum Style {

        /**
         * A fully-text square button.  The default style.
         */
        SQUARE,

        /**
         * A button with round semi-circle edges.
         */
        ROUND,

        /**
         * A button with diamond end points.
         */
        DIAMOND,

        /**
         * A button arrow pointing left.
         */
        ARROW_LEFT,

        /**
         * A button arrow pointing right.
         */
        ARROW_RIGHT,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The shortcut and button text.
     */
    private MnemonicString mnemonic;

    /**
     * Remember mouse state.
     */
    private TMouseEvent mouse;

    /**
     * True when the button is being pressed and held down.
     */
    private boolean inButtonPress = false;

    /**
     * The action to perform when the button is clicked.
     */
    private TAction action;

    /**
     * If true, suppress the button shadow.
     */
    private boolean noButtonShadow = false;

    /**
     * The style of button to draw.
     */
    private Style style = Style.SQUARE;

    /**
     * A cache of generated button end decorations.
     */
    private HashMap<Integer, Cell> endsCache = new HashMap<Integer, Cell>();

    /**
     * A cache of generated button shadow decorations.
     */
    private HashMap<Integer, Cell> shadowCache = new HashMap<Integer, Cell>();

    /**
     * X location on-screen where endsCache and shadowCache remain valid.
     */
    private int cacheX = -1;

    /**
     * Y location on-screen where endsCache and shadowCache remain valid.
     */
    private int cacheY = -1;

    /**
     * Button style for which endsCache and shadowCache remain valid.
     */
    private Style cacheStyle = style;

    /**
     * Background color for which endsCache and shadowCache remain valid.
     */
    private CellAttributes cacheBackground = new CellAttributes();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor.
     *
     * @param parent parent widget
     * @param text label on the button
     * @param x column relative to parent
     * @param y row relative to parent
     */
    @SuppressWarnings("this-escape")
    private TButton(final TWidget parent, final String text,
        final int x, final int y) {

        // Set parent and window
        super(parent);

        mnemonic = new MnemonicString(text);

        setX(x);
        setY(y);
        super.setHeight(2);
        super.setWidth(StringUtils.width(mnemonic.getRawLabel()) + 3);

        setStyle((String) null);

        // Since we set dimensions after TWidget's constructor, we need to
        // update the layout manager.
        if (getParent().getLayoutManager() != null) {
            getParent().getLayoutManager().remove(this);
            getParent().getLayoutManager().add(this);
        }
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the button
     * @param x column relative to parent
     * @param y row relative to parent
     * @param action to call when button is pressed
     */
    public TButton(final TWidget parent, final String text,
        final int x, final int y, final TAction action) {

        this(parent, text, x, y);
        this.action = action;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the button.
     *
     * @return if true the mouse is currently on the button
     */
    private boolean mouseOnButton() {
        int rightEdge = getWidth() - 1;
        if (inButtonPress) {
            rightEdge++;
        }
        if ((mouse != null)
            && (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < rightEdge)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        if ((mouseOnButton()) && (mouse.isMouse1())) {
            // Begin button press
            inButtonPress = true;
            endsCache.clear();
            shadowCache.clear();
        }
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (inButtonPress && mouse.isMouse1()) {
            // Dispatch the event
            dispatch();

            endsCache.clear();
            shadowCache.clear();
        }

    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (!mouseOnButton()) {
            inButtonPress = false;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbEnter)
            || keypress.equals(kbSpace)
        ) {
            // Dispatch
            dispatch();
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget.setActive() so that the button ends are redrawn.
     *
     * @param enabled if true, this widget can be tabbed to or receive events
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
    }

    /**
     * Override TWidget's width: we can only set width at construction time.
     *
     * @param width new widget width (ignored)
     */
    @Override
    public void setWidth(final int width) {
        // Do nothing
    }

    /**
     * Override TWidget's height: we can only set height at construction
     * time.
     *
     * @param height new widget height (ignored)
     */
    @Override
    public void setHeight(final int height) {
        // Do nothing
    }

    /**
     * Draw a button with a shadow.
     */
    @Override
    public void draw() {
        CellAttributes cacheAttr = getAttrXY(getAbsoluteX(), getAbsoluteY(),
            true);
        if ((cacheX != getAbsoluteX())
            || (cacheY != getAbsoluteY())
            || (cacheStyle != style)
            || !cacheBackground.equals(cacheAttr)
        ) {
            endsCache.clear();
            shadowCache.clear();
        }
        cacheX = getAbsoluteX();
        cacheY = getAbsoluteY();
        cacheStyle = style;
        cacheBackground = new CellAttributes(cacheAttr);

        CellAttributes buttonColor;
        CellAttributes mnemonicColor;

        if (!isEnabled()) {
            buttonColor = getTheme().getColor("tbutton.disabled");
            mnemonicColor = getTheme().getColor("tbutton.disabled");
        } else if (isAbsoluteActive()) {
            buttonColor = getTheme().getColor("tbutton.active");
            mnemonicColor = getTheme().getColor("tbutton.mnemonic.highlighted");
        } else {
            buttonColor = getTheme().getColor("tbutton.inactive");
            mnemonicColor = getTheme().getColor("tbutton.mnemonic");
        }

        buttonColor = new CellAttributes(buttonColor);
        buttonColor.setForeColorRGB(getScreen().getBackend().
            attrToForegroundColor(buttonColor).getRGB());
        mnemonicColor = new CellAttributes(mnemonicColor);
        mnemonicColor.setForeColorRGB(getScreen().getBackend().
            attrToForegroundColor(mnemonicColor).getRGB());

        // Pulse colors.
        if (isActive() && getWindow().isActive()
            && getApplication().hasAnimations()
        ) {
            buttonColor.setPulse(true, false, 0);
            buttonColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor(
                    "tbutton.pulse")).getRGB());
            mnemonicColor.setPulse(true, false, 0);
            mnemonicColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor(
                    "tbutton.mnemonic.pulse")).getRGB());
        }

        Cell leftEdge = getLeftEdge(buttonColor);
        Cell rightEdge = getRightEdge(buttonColor);

        /*
        leftEdge = new Cell('\\', buttonColor);
        rightEdge = new Cell('/', buttonColor);
         */

        if (inButtonPress) {
            putCharXY(1, 0, leftEdge);
            putStringXY(2, 0, mnemonic.getRawLabel(), buttonColor);
            putCharXY(getWidth() - 1, 0, rightEdge);
        } else {
            putCharXY(0, 0, leftEdge);
            putStringXY(1, 0, mnemonic.getRawLabel(), buttonColor);
            putCharXY(getWidth() - 2, 0, rightEdge);

            if (!noButtonShadow) {
                Cell leftBottomShadow = getLeftBottomShadow();
                Cell rightTopShadow = getRightTopShadow();
                Cell rightBottomShadow = getRightBottomShadow();
                putCharXY(1, 1, leftBottomShadow);
                for (int i = 0; i < getWidth() - 3; i++) {
                    Cell bottomShadow = getBottomShadow(buttonColor, i + 2);
                    putCharXY(i + 2, 1, bottomShadow);
                }
                putCharXY(getWidth() - 1, 0, rightTopShadow);
                putCharXY(getWidth() - 1, 1, rightBottomShadow);
            }
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (inButtonPress) {
                putCharXY(2 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            } else {
                putCharXY(1 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TButton ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the mnemonic string for this button.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Act as though the button was pressed.  This is useful for other UI
     * elements to get the same action as if the user clicked the button.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
            inButtonPress = false;
        }
    }

    /**
     * Set the button style.
     *
     * @param style SQUARE, ROUND, etc.
     */
    public void setStyle(final Style style) {
        this.style = style;
    }

    /**
     * Get the button end on the left side.
     *
     * @param buttonColor the button foreground color
     * @return the left button end
     */
    private Cell getLeftEdge(final CellAttributes buttonColor) {
        if (style == Style.SQUARE) {
            return new Cell(buttonColor);
        }

        int screenX = getAbsoluteX();
        if (endsCache.containsKey(screenX)) {
            return endsCache.get(screenX);
        }

        int screenY = getAbsoluteY();
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();

        BufferedImage image = getButtonEndsImage(buttonColor, shadowChar);

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        // Left edge: left half of image
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2 = cellImage.createGraphics();
        gr2.drawImage(image.getSubimage(0, 0, cellWidth, cellHeight),
            0, 0, null);
        gr2.dispose();
        imageId += 1;

        Cell leftEdgeChar = new Cell();
        leftEdgeChar.setImage(cellImage, imageId & 0x7FFFFFFF);
        leftEdgeChar.setOpaqueImage();

        endsCache.put(screenX, leftEdgeChar);

        return leftEdgeChar;
    }

    /**
     * Get the button end on the right side.
     *
     * @param buttonColor the button foreground color
     * @return the right button end
     */
    private Cell getRightEdge(final CellAttributes buttonColor) {
        if (style == Style.SQUARE) {
            return new Cell(buttonColor);
        }

        int screenX = getAbsoluteX() + getWidth() - 2;
        if (inButtonPress) {
            screenX++;
        }
        if (endsCache.containsKey(screenX)) {
            return endsCache.get(screenX);
        }

        int screenY = getAbsoluteY();
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();

        BufferedImage image = getButtonEndsImage(buttonColor, shadowChar);

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        // Right edge: right half of image
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2 = cellImage.createGraphics();
        gr2.drawImage(image.getSubimage(cellWidth, 0, cellWidth, cellHeight),
            0, 0, null);
        gr2.dispose();
        imageId += 3;

        Cell rightEdgeChar = new Cell();
        rightEdgeChar.setImage(cellImage, imageId & 0x7FFFFFFF);
        rightEdgeChar.setOpaqueImage();

        endsCache.put(screenX, rightEdgeChar);
        return rightEdgeChar;
    }

    /**
     * Create the image that contains the pieces of the button ends.
     *
     * @param buttonAttr background is the button color
     * @param shadowAttr foreground is the shadow color (usually black),
     * background matches the container.
     * @return the image
     */
    private BufferedImage getButtonEndsImage(final CellAttributes buttonAttr,
        final CellAttributes shadowAttr) {

        java.awt.Color buttonRgb = getApplication().getBackend().attrToBackgroundColor(buttonAttr);
        java.awt.Color shadowRgb = getApplication().getBackend().attrToForegroundColor(shadowAttr);
        java.awt.Color rectangleRgb = getApplication().getBackend().attrToBackgroundColor(shadowAttr);

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();
        BufferedImage buttonEndsImage = new BufferedImage(cellWidth * 2,
            cellHeight * 2, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gr2 = buttonEndsImage.createGraphics();
        gr2.setColor(rectangleRgb);
        gr2.fillRect(0, 0, cellWidth * 2, cellHeight);
        if (!inButtonPress) {
            gr2.setColor(shadowRgb);
            gr2.fillRect(cellWidth, cellHeight / 2, cellWidth,
                cellHeight - (cellHeight / 2));
        }
        gr2.setColor(buttonRgb);

        int [] xPoints;
        int [] yPoints;
        switch (style) {
        case ROUND:
            gr2.fillOval(0, 0, cellWidth * 2, cellHeight);
            break;
        case DIAMOND:
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            yPoints[0] = cellHeight / 2;
            yPoints[1] = 0;
            yPoints[2] = cellHeight / 2;
            yPoints[3] = cellHeight;
            gr2.fillPolygon(xPoints, yPoints, 4);
            break;
        case ARROW_LEFT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            xPoints[4] = 2 * cellWidth;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight / 2;
            yPoints[1] = 0;
            yPoints[2] = 0;
            yPoints[3] = cellHeight / 2;
            yPoints[4] = cellHeight;
            yPoints[5] = cellHeight;
            gr2.fillPolygon(xPoints, yPoints, 6);
            break;
        case ARROW_RIGHT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 2 * cellWidth;
            xPoints[1] = cellWidth;
            xPoints[2] = 0;
            xPoints[3] = cellWidth;
            xPoints[4] = 0;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight / 2;
            yPoints[1] = 0;
            yPoints[2] = 0;
            yPoints[3] = cellHeight / 2;
            yPoints[4] = cellHeight;
            yPoints[5] = cellHeight;
            gr2.fillPolygon(xPoints, yPoints, 6);
            break;
        case SQUARE:
            // Not possible.
            throw new IllegalArgumentException("Programming bug!");
        }
        gr2.dispose();
        // gr2 now has the foreground ends, on both halves.

        return buttonEndsImage;
    }

    /**
     * Create the image that contains the pieces of shadow.
     *
     * @param shadowAttr foreground is the shadow color (usually black),
     * background matches the container.
     * @return the image
     */
    private BufferedImage getShadowImage(final CellAttributes shadowAttr) {

        java.awt.Color shadowRgb = getApplication().getBackend().attrToForegroundColor(shadowAttr);
        java.awt.Color rectangleRgb = getApplication().getBackend().attrToBackgroundColor(shadowAttr);

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();
        BufferedImage shadowImage = new BufferedImage(cellWidth * 2,
            cellHeight * 2, BufferedImage.TYPE_INT_ARGB);

        // Draw the shadow first, so that it be underneath the right edge.
        Graphics2D gr2s = shadowImage.createGraphics();
        gr2s.setColor(rectangleRgb);
        gr2s.fillRect(0, 0, cellWidth * 2, cellHeight * 2);
        gr2s.setColor(shadowRgb);

        int [] xPoints;
        int [] yPoints;
        switch (style) {
        case ROUND:
            gr2s.fillOval(0, cellHeight / 2, cellWidth * 2, cellHeight);
            break;
        case DIAMOND:
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            yPoints[0] = cellHeight;
            yPoints[1] = cellHeight / 2;
            yPoints[2] = cellHeight;
            yPoints[3] = cellHeight + cellHeight / 2;
            gr2s.fillPolygon(xPoints, yPoints, 4);
            break;
        case ARROW_LEFT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            xPoints[4] = 2 * cellWidth;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight;
            yPoints[1] = cellHeight / 2;
            yPoints[2] = cellHeight / 2;
            yPoints[3] = cellHeight;
            yPoints[4] = cellHeight + cellHeight / 2;
            yPoints[5] = cellHeight + cellHeight / 2;
            gr2s.fillPolygon(xPoints, yPoints, 6);
            break;
        case ARROW_RIGHT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 2 * cellWidth;
            xPoints[1] = cellWidth;
            xPoints[2] = 0;
            xPoints[3] = cellWidth;
            xPoints[4] = 0;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight;
            yPoints[1] = cellHeight / 2;
            yPoints[2] = cellHeight / 2;
            yPoints[3] = cellHeight;
            yPoints[4] = cellHeight + cellHeight / 2;
            yPoints[5] = cellHeight + cellHeight / 2;
            gr2s.fillPolygon(xPoints, yPoints, 6);
            break;
        case SQUARE:
            // Not possible.
            throw new IllegalArgumentException("Programming bug!");
        }
        gr2s.dispose();
        // gr2s now has the shadow bits, shifted half a cell down from 0.

        return shadowImage;
    }

    /**
     * Get the shadowed cell for the button end on the leftt side.
     *
     * @return the right button end
     */
    private Cell getLeftBottomShadow() {
        int screenX = getAbsoluteX() + 1;
        if (shadowCache.containsKey(screenX)) {
            return shadowCache.get(screenX);
        }

        int screenY = getAbsoluteY() + 1;
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDF], shadowChar);
        }

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();

        BufferedImage image = new BufferedImage(cellWidth * 2, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        BufferedImage shadowImage = getShadowImage(shadowChar);

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        // Left shadow edge: bottom-left half of shadowImage
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2s = cellImage.createGraphics();
        gr2s.drawImage(shadowImage.getSubimage(0, cellHeight,
                cellWidth, cellHeight), 0, 0, null);
        gr2s.dispose();
        imageId += 2;

        Cell leftEdgeShadowChar = new Cell();
        leftEdgeShadowChar.setImage(cellImage, imageId & 0x7FFFFFFF);
        leftEdgeShadowChar.setOpaqueImage();

        shadowCache.put(screenX, leftEdgeShadowChar);
        return leftEdgeShadowChar;
    }

    /**
     * Get the shadowed cell for the button end on the right side.
     *
     * @return the right button end's shadow
     */
    private Cell getRightTopShadow() {
        int screenX = getAbsoluteX() + getWidth() - 1;
        if (endsCache.containsKey(screenX)) {
            return endsCache.get(screenX);
        }

        int screenY = getAbsoluteY();
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDC], shadowChar);
        }

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();

        BufferedImage image = new BufferedImage(cellWidth * 2, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        BufferedImage shadowImage = getShadowImage(shadowChar);

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        // Right shadow edge top: top-right half of shadowImage
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2s = cellImage.createGraphics();
        gr2s.drawImage(shadowImage.getSubimage(cellWidth, 0,
                cellWidth, cellHeight), 0, 0, null);
        gr2s.dispose();
        imageId += 8;

        Cell rightEdgeShadowCharTop = new Cell();
        rightEdgeShadowCharTop.setImage(cellImage, imageId & 0x7FFFFFFF);
        rightEdgeShadowCharTop.setOpaqueImage();

        endsCache.put(screenX, rightEdgeShadowCharTop);
        return rightEdgeShadowCharTop;
    }

    /**
     * Get the shadowed cell below the button end on the right side.
     *
     * @return the right button end's shadow
     */
    private Cell getRightBottomShadow() {
        int screenX = getAbsoluteX() + getWidth() - 1;
        if (shadowCache.containsKey(screenX)) {
            return shadowCache.get(screenX);
        }

        int screenY = getAbsoluteY() + 1;
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDF], shadowChar);
        }

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();

        BufferedImage image = new BufferedImage(cellWidth * 2, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        BufferedImage shadowImage = getShadowImage(shadowChar);

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        // Right shadow edge bottom: bottom-right half of shadowImage
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2s = cellImage.createGraphics();
        gr2s.drawImage(shadowImage.getSubimage(cellWidth, cellHeight,
                cellWidth, cellHeight), 0, 0, null);
        gr2s.dispose();
        imageId += 4;

        Cell rightEdgeShadowCharBottom = new Cell();
        rightEdgeShadowCharBottom.setImage(cellImage, imageId & 0x7FFFFFFF);
        rightEdgeShadowCharBottom.setOpaqueImage();

        shadowCache.put(screenX, rightEdgeShadowCharBottom);
        return rightEdgeShadowCharBottom;
    }

    /**
     * Get the shadowed cell below the button text.
     *
     * @param buttonColor the button foreground color
     * @param x the widget x location for this shadowed cell
     * @return the right button end's shadow
     */
    private Cell getBottomShadow(final CellAttributes buttonColor,
        final int x) {

        int screenX = getAbsoluteX() + x;
        if (shadowCache.containsKey(screenX)) {
            return shadowCache.get(screenX);
        }

        int screenY = getAbsoluteY() + 1;
        CellAttributes shadowChar = getAttrXY(screenX, screenY, true);
        shadowChar.setForeColorRGB(0x00);

        if (style == Style.SQUARE) {
            return new Cell(GraphicsChars.CP437[0xDF], shadowChar);
        }

        java.awt.Color shadowRgb = getApplication().getBackend().attrToForegroundColor(shadowChar);

        java.awt.Color rectangleRgb = getApplication().getBackend().attrToBackgroundColor(shadowChar);

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2s = cellImage.createGraphics();
        gr2s.setColor(rectangleRgb);
        gr2s.fillRect(0, 0, cellWidth, cellHeight);
        gr2s.setColor(shadowRgb);
        gr2s.fillRect(0, 0, cellWidth, cellHeight / 2);
        gr2s.dispose();

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();
        imageId += 6;

        Cell shadowCharBottom = new Cell();
        shadowCharBottom.setImage(cellImage, imageId & 0x7FFFFFFF);
        shadowCharBottom.setOpaqueImage();

        shadowCache.put(screenX, shadowCharBottom);
        return shadowCharBottom;
    }

    /**
     * Set the button style.
     *
     * @param buttonStyle the button style string, one of: "square", "round",
     * "diamond", "leftArrow", or "rightArrow"; or null to use the value from
     * casciian.TButton.style.
     */
    public void setStyle(final String buttonStyle) {
        String styleString = System.getProperty("casciian.TButton.style",
            "square");
        if (buttonStyle != null) {
            styleString = buttonStyle.toLowerCase();
        }
        if (styleString.equals("square")) {
            style = Style.SQUARE;
        } else if (styleString.equals("round")) {
            style = Style.ROUND;
        } else if (styleString.equals("diamond")) {
            style = Style.DIAMOND;
        } else if (styleString.equals("arrowleft")
            || styleString.equals("leftarrow")
        ) {
            style = Style.ARROW_LEFT;
        } else if (styleString.equals("arrowright")
            || styleString.equals("rightarrow")
        ) {
            style = Style.ARROW_RIGHT;
        } else {
            style = Style.SQUARE;
        }
    }

}
