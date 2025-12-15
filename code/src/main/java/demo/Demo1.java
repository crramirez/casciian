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
 * This class is the main driver for a simple demonstration of Casciian's
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
            DemoApplication app;
            app = new DemoApplication(TApplication.BackendType.XTERM);
            (new Thread(app)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
