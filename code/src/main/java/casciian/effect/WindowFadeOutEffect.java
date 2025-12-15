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
package casciian.effect;

import casciian.TWindow;
import casciian.backend.Screen;
import casciian.event.TInputEvent;

/**
 * Make the window fade out using translucence.
 */
public class WindowFadeOutEffect extends WindowTransitionEffect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The alpha value to set fakeWindow to.
     */
    private int alpha;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to fade out
     */
    public WindowFadeOutEffect(final TWindow window) {
        super(window);

        alpha = window.getAlpha();
    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the effect.
     */
    public void update() {
        if (dummyWindow == null) {
            return;
        }

        if (alpha > 0) {
            // Bump alpha a bit.  I have not yet figured out how this
            // converts to an actual time in practice.
            alpha = Math.max(alpha - 64, 0);
            dummyWindow.setAlpha(alpha);
        }
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        if (dummyWindow != null) {
            if (alpha == 0) {
                dummyWindow.close();
                dummyWindow = null;
                return true;
            }
        }
        return false;
    }

}
