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

/**
 * Scrollable provides a public API for horizontal and vertical scrollbars.
 * Note that not all Scrollables support both horizontal and vertical
 * scrolling; for those that only support a subset, it is expected that the
 * methods corresponding to the missing scrollbar quietly succeed without
 * throwing any exceptions.
 */
public interface Scrollable {

    /**
     * Get the horizontal scrollbar, or null if this Viewport does not
     * support horizontal scrolling.
     *
     * @return the horizontal scrollbar
     */
    public THScroller getHorizontalScroller();

    /**
     * Get the vertical scrollbar, or null if this Viewport does not support
     * vertical scrolling.
     *
     * @return the vertical scrollbar
     */
    public TVScroller getVerticalScroller();

    /**
     * Get the value that corresponds to being on the top edge of the
     * vertical scroll bar.
     *
     * @return the scroll value
     */
    public int getTopValue();

    /**
     * Set the value that corresponds to being on the top edge of the
     * vertical scroll bar.
     *
     * @param topValue the new scroll value
     */
    public void setTopValue(final int topValue);

    /**
     * Get the value that corresponds to being on the bottom edge of the
     * vertical scroll bar.
     *
     * @return the scroll value
     */
    public int getBottomValue();

    /**
     * Set the value that corresponds to being on the bottom edge of the
     * vertical scroll bar.
     *
     * @param bottomValue the new scroll value
     */
    public void setBottomValue(final int bottomValue);

    /**
     * Get current value of the vertical scroll.
     *
     * @return the scroll value
     */
    public int getVerticalValue();

    /**
     * Set current value of the vertical scroll.
     *
     * @param value the new scroll value
     */
    public void setVerticalValue(final int value);

    /**
     * Get the increment for clicking on an arrow on the vertical scrollbar.
     *
     * @return the increment value
     */
    public int getVerticalSmallChange();

    /**
     * Set the increment for clicking on an arrow on the vertical scrollbar.
     *
     * @param smallChange the new increment value
     */
    public void setVerticalSmallChange(final int smallChange);

    /**
     * Get the increment for clicking in the bar between the box and an
     * arrow on the vertical scrollbar.
     *
     * @return the increment value
     */
    public int getVerticalBigChange();

    /**
     * Set the increment for clicking in the bar between the box and an
     * arrow on the vertical scrollbar.
     *
     * @param bigChange the new increment value
     */
    public void setVerticalBigChange(final int bigChange);

    /**
     * Perform a small step change up.
     */
    public void verticalDecrement();

    /**
     * Perform a small step change down.
     */
    public void verticalIncrement();

    /**
     * Perform a big step change up.
     */
    public void bigVerticalDecrement();

    /**
     * Perform a big step change down.
     */
    public void bigVerticalIncrement();

    /**
     * Go to the top edge of the vertical scroller.
     */
    public void toTop();

    /**
     * Go to the bottom edge of the vertical scroller.
     */
    public void toBottom();

    /**
     * Get the value that corresponds to being on the left edge of the
     * horizontal scroll bar.
     *
     * @return the scroll value
     */
    public int getLeftValue();

    /**
     * Set the value that corresponds to being on the left edge of the
     * horizontal scroll bar.
     *
     * @param leftValue the new scroll value
     */
    public void setLeftValue(final int leftValue);

    /**
     * Get the value that corresponds to being on the right edge of the
     * horizontal scroll bar.
     *
     * @return the scroll value
     */
    public int getRightValue();

    /**
     * Set the value that corresponds to being on the right edge of the
     * horizontal scroll bar.
     *
     * @param rightValue the new scroll value
     */
    public void setRightValue(final int rightValue);

    /**
     * Get current value of the horizontal scroll.
     *
     * @return the scroll value
     */
    public int getHorizontalValue();

    /**
     * Set current value of the horizontal scroll.
     *
     * @param value the new scroll value
     */
    public void setHorizontalValue(final int value);

    /**
     * Get the increment for clicking on an arrow on the horizontal
     * scrollbar.
     *
     * @return the increment value
     */
    public int getHorizontalSmallChange();

    /**
     * Set the increment for clicking on an arrow on the horizontal
     * scrollbar.
     *
     * @param smallChange the new increment value
     */
    public void setHorizontalSmallChange(final int smallChange);

    /**
     * Get the increment for clicking in the bar between the box and an
     * arrow on the horizontal scrollbar.
     *
     * @return the increment value
     */
    public int getHorizontalBigChange();

    /**
     * Set the increment for clicking in the bar between the box and an
     * arrow on the horizontal scrollbar.
     *
     * @param bigChange the new increment value
     */
    public void setHorizontalBigChange(final int bigChange);

    /**
     * Perform a small step change left.
     */
    public void horizontalDecrement();

    /**
     * Perform a small step change right.
     */
    public void horizontalIncrement();

    /**
     * Perform a big step change left.
     */
    public void bigHorizontalDecrement();

    /**
     * Perform a big step change right.
     */
    public void bigHorizontalIncrement();

    /**
     * Go to the left edge of the horizontal scroller.
     */
    public void toLeft();

    /**
     * Go to the right edge of the horizontal scroller.
     */
    public void toRight();

    /**
     * Go to the top-left edge of the horizontal and vertical scrollers.
     */
    public void toHome();

    /**
     * Go to the bottom-right edge of the horizontal and vertical scrollers.
     */
    public void toEnd();

}
