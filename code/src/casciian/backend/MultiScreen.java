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
package casciian.backend;

import java.util.ArrayList;
import java.util.List;

import casciian.bits.BorderStyle;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.Clipboard;

/**
 * MultiScreen mirrors its I/O to several screens.
 */
public class MultiScreen extends LogicalScreen implements Screen {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of screens to use.
     */
    private List<Screen> screens = new ArrayList<Screen>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor provides a virtual screen at 80x25.
     */
    public MultiScreen() {
        super(80, 25);
    }

    /**
     * Public constructor takes the dimensions of the first screen.
     *
     * @param screen the screen to add
     */
    public MultiScreen(final Screen screen) {
        super(screen.getWidth(), screen.getHeight());
        synchronized (screens) {
            screens.add(screen);
        }
    }

    // ------------------------------------------------------------------------
    // LogicalScreen ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Change the width.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param width new screen width
     */
    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.setWidth(width);
            }
        }
    }

    /**
     * Change the height.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param height new screen height
     */
    @Override
    public void setHeight(final int height) {
        super.setHeight(height);
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.setHeight(height);
            }
        }
    }

    /**
     * Change the width and height.  Everything on-screen will be destroyed
     * and must be redrawn.
     *
     * @param width new screen width
     * @param height new screen height
     */
    @Override
    public void setDimensions(final int width, final int height) {
        super.setDimensions(width, height);
        synchronized (screens) {
            for (Screen screen: screens) {
                // Do not blindly call setDimension() on every screen.
                // Instead call it only on those screens that do not already
                // have the requested dimension.  With this very small check,
                // we have the ability for ANY screen in the MultiBackend to
                // resize ALL of the screens.
                if ((screen.getWidth() != width)
                    || (screen.getHeight() != height)
                ) {
                    screen.setDimensions(width, height);
                } else {
                    // The screen that didn't change is probably the one that
                    // prompted the resize.  Force it to repaint.
                    screen.clearPhysical();
                }
            }
        }
    }

    /**
     * Clear the physical screen.
     */
    @Override
    public void clearPhysical() {
        super.clearPhysical();
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.clearPhysical();
            }
        }
    }

    /**
     * Classes must provide an implementation to push the logical screen to
     * the physical device.
     */
    @Override
    public void flushPhysical() {
        List<Screen> screensToFlush = new ArrayList<Screen>();
        synchronized (screens) {
            screensToFlush.addAll(screens);
        }
        for (Screen screen: screensToFlush) {
            synchronized (screen) {
                screen.copyScreen(this);
            }
        }
    }

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    @Override
    public void setTitle(final String title) {
        super.setTitle(title);
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.setTitle(title);
            }
        }
    }

    // ------------------------------------------------------------------------
    // MultiScreen ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add a screen to the list.
     *
     * @param screen the screen to add
     */
    public void addScreen(final Screen screen) {
        synchronized (screens) {
            screens.add(screen);
        }
    }

    /**
     * Remove a screen from the list.
     *
     * @param screenToRemove the screen to remove
     */
    public void removeScreen(final Screen screenToRemove) {
        synchronized (screens) {
            if (screens.size() > 1) {
                screens.remove(screenToRemove);
            }
        }
    }

}
