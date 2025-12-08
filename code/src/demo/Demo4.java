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
 * This class is the main driver for a simple demonstration of Jexer's
 * capabilities.  This one shows TDesktop and TWindow API details.
 */
public class Demo4 {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo4() {}

    // ------------------------------------------------------------------------
    // Demo4 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            // Swing is the default backend on Windows unless explicitly
            // overridden by casciian.Swing.
            TApplication.BackendType backendType = TApplication.BackendType.XTERM;
            if (System.getProperty("os.name").startsWith("Windows")) {
                backendType = TApplication.BackendType.SWING;
            }
            if (System.getProperty("os.name").startsWith("Mac")) {
                backendType = TApplication.BackendType.SWING;
            }
            if (System.getProperty("casciian.Swing") != null) {
                if (System.getProperty("casciian.Swing", "false").equals("true")) {
                    backendType = TApplication.BackendType.SWING;
                } else {
                    backendType = TApplication.BackendType.XTERM;
                }
            }
            DesktopDemoApplication app = new DesktopDemoApplication(backendType);
            (new Thread(app)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
