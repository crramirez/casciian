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
 * TVScroller implements a simple vertical scroll bar.
 *
 * <p>It is a thin {@link TScroller} specialization: the shared scroll logic
 * lives in the base class, and this class exists to default the orientation to
 * vertical and to keep the historical top/bottom method names for backward
 * compatibility.</p>
 */
public class TVScroller extends TScroller {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param height height of scroll bar
     */
    public TVScroller(final TWidget parent, final int x, final int y,
        final int height) {

        // Set parent and window
        super(Orientation.VERTICAL, parent, x, y, 1, height);
    }

    // ------------------------------------------------------------------------
    // TVScroller -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the value that corresponds to being on the top edge of the scroll
     * bar.
     *
     * @return the scroll value
     */
    public int getTopValue() {
        return getMinValue();
    }

    /**
     * Set the value that corresponds to being on the top edge of the scroll
     * bar.
     *
     * @param topValue the new scroll value
     */
    public void setTopValue(final int topValue) {
        setMinValue(topValue);
    }

    /**
     * Get the value that corresponds to being on the bottom edge of the
     * scroll bar.
     *
     * @return the scroll value
     */
    public int getBottomValue() {
        return getMaxValue();
    }

    /**
     * Set the value that corresponds to being on the bottom edge of the
     * scroll bar.
     *
     * @param bottomValue the new scroll value
     */
    public void setBottomValue(final int bottomValue) {
        setMaxValue(bottomValue);
    }

    /**
     * Go to the top edge of the scroller.
     */
    public void toTop() {
        toMin();
    }

    /**
     * Go to the bottom edge of the scroller.
     */
    public void toBottom() {
        toMax();
    }

}
