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

import casciian.TApplication;

/**
 * This class is the main driver for a simple demonstration of Jexer's
 * capabilities.
 */
public class Demo1 {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo1() {}

    // ------------------------------------------------------------------------
    // Demo1 ------------------------------------------------------------------
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
            DemoApplication app;
            if (backendType == TApplication.BackendType.SWING) {
                int fontSize = 20;
                try {
                    fontSize = Integer.parseInt(
                        System.getProperty("casciian.Swing.fontSize", "20"));
                    // Keep requested font size between 16 and 32 pt.
                    fontSize = Math.min(32, Math.max(16, fontSize));
                } catch (NumberFormatException e) {
                    // SQUASH
                }
                app = new DemoApplication(backendType, 90, 30, fontSize);
            } else {
                app = new DemoApplication(backendType);
            }
            (new Thread(app)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
