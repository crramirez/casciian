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
package demo;

import casciian.*;

/**
 * The modified desktop.
 */
public class DesktopDemo extends TDesktop {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, draw the hatch.  Note package private access.
     */
    boolean drawHatch = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent application
     */
    public DesktopDemo(final TApplication parent) {
        super(parent);
    }

    // ------------------------------------------------------------------------
    // TDesktop ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The default TDesktop draws a hatch character across everything.  This
     * version is selectable.
     */
    @Override
    public void draw() {
        if (drawHatch) {
            super.draw();
        }
    }

}
