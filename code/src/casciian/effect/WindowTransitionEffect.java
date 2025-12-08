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

import casciian.TApplication;
import casciian.TWindow;
import casciian.backend.Screen;
import casciian.event.TInputEvent;

/**
 * Make the window fade out using translucence.
 */
public abstract class WindowTransitionEffect implements Effect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The DummyWindow shows the new screen area contents, while also
     * ignoring events during the Effect execution.
     */
    public class DummyWindow extends TWindow {

        /**
         * The region of the screen showing what was behind the window (for
         * window open effects) of the window state before it closed (for
         * close effects).
         */
        private Screen screen;

        /**
         * The WindowTransitionEffect that will actually draw the screen.
         */
        private WindowTransitionEffect windowTransitionEffect;

        /**
         * The original window that the dummy window is covering up.
         */
        private TWindow window;

        /**
         * Public constructor.
         *
         * @param window the window that will be opened or closed
         * @param windowTransitionEffect the effect this dummy window is
         * placegolder for
         */
        public DummyWindow(final TWindow window,
            final WindowTransitionEffect windowTransitionEffect) {

            super(window.getApplication(), window.getTitle(),
                window.getX(), window.getY(),
                window.getWidth(), window.getHeight(),
                TWindow.MODAL);

            this.windowTransitionEffect = windowTransitionEffect;
            this.window = window;
        }

        // Disable all inputs.
        @Override
        public void handleEvent(final TInputEvent event) {
            // NOP
        }

        // Draw the old screen.
        @Override
        public void draw() {
            getScreen().resetClipping();
            getScreen().setOffsetX(getX());
            getScreen().setOffsetY(getY());
            windowTransitionEffect.drawEffect();
        }

        // Disable effects on the dummy screen itself.
        @Override
        public boolean disableOpenEffect() {
            return true;
        }

        // Disable effects on the dummy screen itself.
        @Override
        public boolean disableCloseEffect() {
            return true;
        }

        /**
         * Get the old screen data.
         *
         * @return the old screen data
         */
        public Screen getOldScreen() {
            return screen;
        }

        /**
         * Get the window this dummy window is convering up.
         *
         * @return the window
         */
        public TWindow getEffectedWindow() {
            return window;
        }

    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The dummy window that shows the new screen area contents while also
     * ignoring events while the effect is running.
     */
    protected DummyWindow dummyWindow;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to fade out
     */
    public WindowTransitionEffect(final TWindow window) {

        // We capture the screen in the idle thread, right *before* the
        // window has been drawn the first time (for window open effects) or
        // *after* it has been drawn for the last time (for window close
        // effects).
        final TApplication app = window.getApplication();

        app.invokeLater(new Runnable() {
            public void run() {
                if (app.isModalThreadRunning()) {
                    // This effect cannot run on modal windows.
                    return;
                }
                if ((window.getWidth() < 16) || (window.getHeight() < 6)) {
                    // Don't run this effect for small windows.
                    return;
                }

                dummyWindow = new DummyWindow(window,
                    WindowTransitionEffect.this);
                int width = window.getScreen().getWidth();
                int height = window.getScreen().getHeight();

                dummyWindow.screen = window.getScreen().snapshotPhysical(0, 0,
                    width, height);

                dummyWindow.setAlpha(window.getAlpha());
                }
            });

    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // WindowTransitionEffect -------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The draw function called by the DummyScreen.
     */
    protected void drawEffect() {
        assert (dummyWindow != null);
        assert (!dummyWindow.getApplication().isModalThreadRunning());

        Screen oldScreen = dummyWindow.screen;
        Screen screen = dummyWindow.getScreen();
        int width = dummyWindow.getWidth();
        int height = dummyWindow.getHeight();
        int x0 = dummyWindow.getEffectedWindow().getX();
        int y0 = dummyWindow.getEffectedWindow().getY();

        screen.resetClipping();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                screen.putCharXY(x + x0, y + y0,
                    oldScreen.getCharXY(x + x0, y + y0));
            }
        }
    }

}
