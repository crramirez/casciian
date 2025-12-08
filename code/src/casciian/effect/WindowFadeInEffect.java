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

/**
 * Make the window fade in using tranlucence.
 */
public class WindowFadeInEffect implements Effect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The window to fade in.
     */
    private TWindow window;

    /**
     * The window's original alpha value we are ramping up to.
     */
    private int targetAlpha = -1;
    
    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to fade in
     */
    public WindowFadeInEffect(final TWindow window) {
        this.window = window;
    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the effect.
     */
    public void update() {
        if (!window.isShown()) {
            return;
        }
        if (!window.getApplication().hasTranslucence()) {
            return;
        }
        if (targetAlpha < 0) {
            targetAlpha = window.getAlpha();
            window.setAlpha(0);
        } else {
            int alpha = window.getAlpha();
            if (alpha < targetAlpha) {
                // Bump alpha a bit.  I have not yet figured out how this
                // converts to an actual time in practice.
                alpha = Math.min(alpha + 64, targetAlpha);
                window.setAlpha(alpha);
            }
        }
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        if (!window.getApplication().hasTranslucence()) {
            return true;
        }
        return ((targetAlpha > 0) && (window.getAlpha() >= targetAlpha));
    }

}
