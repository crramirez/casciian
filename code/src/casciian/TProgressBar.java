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

import casciian.bits.CellAttributes;
import casciian.bits.GraphicsChars;
import casciian.bits.StringUtils;

/**
 * TProgressBar implements a simple progress bar.
 */
public class TProgressBar extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Value that corresponds to 0% progress.
     */
    private int minValue = 0;

    /**
     * Value that corresponds to 100% progress.
     */
    private int maxValue = 100;

    /**
     * Current value of the progress.
     */
    private int value = 0;

    /**
     * The left border character.
     */
    private int leftBorderChar = GraphicsChars.CP437[0xC3];

    /**
     * The filled-in part of the bar.
     */
    private int completedChar = GraphicsChars.BOX;

    /**
     * The remaining to be filled in part of the bar.
     */
    private int remainingChar = GraphicsChars.SINGLE_BAR;

    /**
     * The right border character.
     */
    private int rightBorderChar = GraphicsChars.CP437[0xB4];

    /**
     * If true, use the window's background color.
     */
    private boolean matchWindowBackground = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of progress bar
     * @param value initial value of percent complete
     */
    public TProgressBar(final TWidget parent, final int x, final int y,
        final int width, final int value) {

        this(parent, x, y, width, value, false);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of progress bar
     * @param value initial value of percent complete
     * @param matchWindowBackground if true, use the window's background
     * color
     */
    public TProgressBar(final TWidget parent, final int x, final int y,
        final int width, final int value, final boolean matchWindowBackground) {

        // Set parent and window
        super(parent, false, x, y, width, 1);

        this.value = value;
        this.matchWindowBackground = matchWindowBackground;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * Draw a static progress bar.
     */
    @Override
    public void draw() {

        if (getWidth() <= 2) {
            // Bail out, we are too narrow to draw anything.
            return;
        }

        CellAttributes completeColor = getTheme().getColor("tprogressbar.complete");
        CellAttributes incompleteColor = getTheme().getColor("tprogressbar.incomplete");

        float progress = ((float)value - minValue) / ((float)maxValue - minValue);
        int progressInt = (int)(progress * 100);
        int progressUnit = 100 / (getWidth() - 2);

        if (matchWindowBackground) {
            putForegroundCharXY(0, 0, leftBorderChar, incompleteColor);
        } else {
            putCharXY(0, 0, leftBorderChar, incompleteColor);
        }
        for (int i = StringUtils.width(leftBorderChar); i < getWidth() - 2;) {
            float iProgress = (float)i / (getWidth() - 2);
            int iProgressInt = (int)(iProgress * 100);
            if (iProgressInt <= progressInt - progressUnit) {
                if (matchWindowBackground) {
                    putForegroundCharXY(i, 0, completedChar, completeColor);
                } else {
                    putCharXY(i, 0, completedChar, completeColor);
                }
                i += StringUtils.width(completedChar);
            } else {
                if (matchWindowBackground) {
                    putForegroundCharXY(i, 0, remainingChar, incompleteColor);
                } else {
                    putCharXY(i, 0, remainingChar, incompleteColor);
                }
                i += StringUtils.width(remainingChar);
            }
        }
        if (value >= maxValue) {
            if (matchWindowBackground) {
                putForegroundCharXY(getWidth() -
                    StringUtils.width(leftBorderChar) -
                    StringUtils.width(rightBorderChar), 0, completedChar,
                    completeColor);
            } else {
                putCharXY(getWidth() - StringUtils.width(leftBorderChar) -
                    StringUtils.width(rightBorderChar), 0, completedChar,
                    completeColor);
            }
        } else {
            if (matchWindowBackground) {
                putForegroundCharXY(getWidth() -
                    StringUtils.width(leftBorderChar) -
                    StringUtils.width(rightBorderChar), 0, remainingChar,
                    incompleteColor);
            } else {
                putCharXY(getWidth() - StringUtils.width(leftBorderChar) -
                    StringUtils.width(rightBorderChar), 0, remainingChar,
                    incompleteColor);
            }
        }
        if (matchWindowBackground) {
            putForegroundCharXY(getWidth() - StringUtils.width(rightBorderChar),
                0, rightBorderChar, incompleteColor);
        } else {
            putCharXY(getWidth() - StringUtils.width(rightBorderChar), 0,
                rightBorderChar, incompleteColor);
        }
    }

    // ------------------------------------------------------------------------
    // TProgressBar -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the value that corresponds to 0% progress.
     *
     * @return the value that corresponds to 0% progress
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Set the value that corresponds to 0% progress.
     *
     * @param minValue the value that corresponds to 0% progress
     */
    public void setMinValue(final int minValue) {
        this.minValue = minValue;
    }

    /**
     * Get the value that corresponds to 100% progress.
     *
     * @return the value that corresponds to 100% progress
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Set the value that corresponds to 100% progress.
     *
     * @param maxValue the value that corresponds to 100% progress
     */
    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Get the current value of the progress.
     *
     * @return the current value of the progress
     */
    public int getValue() {
        return value;
    }

    /**
     * Set the current value of the progress.
     *
     * @param value the current value of the progress
     */
    public void setValue(final int value) {
        this.value = value;
    }

    /**
     * Set the left border character.
     *
     * @param ch the char to use
     */
    public void setLeftBorderChar(final int ch) {
        leftBorderChar = ch;
    }

    /**
     * Get the left border character.
     *
     * @return the char
     */
    public int getLeftBorderChar() {
        return leftBorderChar;
    }

    /**
     * Set the filled-in part of the bar.
     *
     * @param ch the char to use
     */
    public void setCompletedChar(final int ch) {
        completedChar = ch;
    }

    /**
     * Get the filled-in part of the bar.
     *
     * @return the char
     */
    public int getCompletedChar() {
        return completedChar;
    }

    /**
     * Set the remaining to be filled in part of the bar.
     *
     * @param ch the char to use
     */
    public void setRemainingChar(final int ch) {
        remainingChar = ch;
    }

    /**
     * Get the remaining to be filled in part of the bar.
     *
     * @return the char
     */
    public int getRemainingChar() {
        return remainingChar;
    }

    /**
     * Set the right border character.
     *
     * @param ch the char to use
     */
    public void setRightBorderChar(final int ch) {
        rightBorderChar = ch;
    }

    /**
     * Get the right border character.
     *
     * @return the char
     */
    public int getRightBorderChar() {
        return rightBorderChar;
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

}
