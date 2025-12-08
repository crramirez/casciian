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

import java.util.List;

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.ComplexCell;
import casciian.bits.MnemonicString;
import casciian.bits.StringUtils;

/**
 * TLabel implements a simple label, with an optional mnemonic hotkey action
 * associated with it.
 */
public class TLabel extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The shortcut and label.
     */
    private MnemonicString mnemonic;

    /**
     * A mnemonic raw text as complex cells.
     */
    private List<ComplexCell> cells = null;

    /**
     * The action to perform when the mnemonic shortcut is pressed.
     */
    private TAction action;

    /**
     * Label color.
     */
    private String colorKey;

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor, using the default "tlabel" for colorKey.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y) {

        this(parent, text, x, y, "tlabel");
    }

    /**
     * Public constructor, using the default "tlabel" for colorKey.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param action to call when shortcut is pressed
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y, final TAction action) {

        this(parent, text, x, y, "tlabel", action);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param colorKey ColorTheme key color to use for foreground text
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y, final String colorKey) {

        this(parent, text, x, y, colorKey, true);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param colorKey ColorTheme key color to use for foreground text
     * @param action to call when shortcut is pressed
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y, final String colorKey, final TAction action) {

        this(parent, text, x, y, colorKey, true, action);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param colorKey ColorTheme key color to use for foreground text
     * @param matchWindowBackground if true, use the window's background
     * color
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y, final String colorKey, final boolean matchWindowBackground) {

        this(parent, text, x, y, colorKey, matchWindowBackground, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param colorKey ColorTheme key color to use for foreground text
     * @param matchWindowBackground if true, use the window's background
     * color
     * @param action to call when shortcut is pressed
     */
    @SuppressWarnings("this-escape")
    public TLabel(final TWidget parent, final String text, final int x,
        final int y, final String colorKey, final boolean matchWindowBackground,
        final TAction action) {

        // Set parent and window
        super(parent, false, x, y, 0, 1);

        setLabel(text);
        this.colorKey = colorKey;
        this.matchWindowBackground = matchWindowBackground;
        this.action = action;
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * Draw a static label.
     */
    @Override
    public void draw() {
        // Setup my color
        CellAttributes color = new CellAttributes();
        CellAttributes mnemonicColor = new CellAttributes();
        color.setTo(getTheme().getColor(colorKey));
        mnemonicColor.setTo(getTheme().getColor("tlabel.mnemonic"));

        if (matchWindowBackground) {
            putForegroundStringXY(0, 0, mnemonic.getRawLabel(), color);
        } else {
            putStringXY(0, 0, mnemonic.getRawLabel(), color);
        }

        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (matchWindowBackground) {
                putForegroundCharXY(mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            } else {
                putCharXY(mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), mnemonicColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TLabel -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get label raw text.
     *
     * @return label text
     */
    public String getLabel() {
        return mnemonic.getRawLabel();
    }

    /**
     * Get the mnemonic string for this label.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Set label text.
     *
     * @param label new label text
     */
    public void setLabel(final String label) {
        mnemonic = new MnemonicString(label);
        super.setWidth(StringUtils.width(mnemonic.getRawLabel()));
    }

    /**
     * Get the label color.
     *
     * @return the ColorTheme key color to use for foreground text
     */
    public String getColorKey() {
        return colorKey;
    }

    /**
     * Set the label color.
     *
     * @param colorKey ColorTheme key color to use for foreground text
     */
    public void setColorKey(final String colorKey) {
        this.colorKey = colorKey;
    }

    /**
     * Get the window background option.
     *
     * @return true if the window's background color will be used
     */
    public boolean isMatchWindowBackground() {
        return matchWindowBackground;
    }

    /**
     * Set the window background option.
     *
     * @param matchWindowBackground if true, the window's background color
     * will be used
     */
    public void setMatchWindowBackground(final boolean matchWindowBackground) {
        this.matchWindowBackground = matchWindowBackground;
    }

    /**
     * Act as though the mnemonic shortcut was pressed.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
        }
    }

}
