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

/**
 * THScroller implements a simple horizontal scroll bar.
 *
 * <p>It is a thin {@link TScroller} specialization: the shared scroll logic
 * lives in the base class, and this class exists to default the orientation to
 * horizontal and to keep the historical left/right method names for backward
 * compatibility.</p>
 */
public class THScroller extends TScroller {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width height of scroll bar
     */
    public THScroller(final TWidget parent, final int x, final int y,
        final int width) {

        // Set parent and window
        super(Orientation.HORIZONTAL, parent, x, y, width, 1);
    }

    // ------------------------------------------------------------------------
    // THScroller -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the value that corresponds to being on the left edge of the scroll
     * bar.
     *
     * @return the scroll value
     */
    public int getLeftValue() {
        return getMinValue();
    }

    /**
     * Set the value that corresponds to being on the left edge of the
     * scroll bar.
     *
     * @param leftValue the new scroll value
     */
    public void setLeftValue(final int leftValue) {
        setMinValue(leftValue);
    }

    /**
     * Get the value that corresponds to being on the right edge of the
     * scroll bar.
     *
     * @return the scroll value
     */
    public int getRightValue() {
        return getMaxValue();
    }

    /**
     * Set the value that corresponds to being on the right edge of the
     * scroll bar.
     *
     * @param rightValue the new scroll value
     */
    public void setRightValue(final int rightValue) {
        setMaxValue(rightValue);
    }

    /**
     * Go to the left edge of the scroller.
     */
    public void toLeft() {
        toMin();
    }

    /**
     * Go to the right edge of the scroller.
     */
    public void toRight() {
        toMax();
    }

}
