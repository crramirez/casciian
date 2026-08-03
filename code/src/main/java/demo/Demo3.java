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

import java.io.*;

/**
 * This class is the main driver for a simple demonstration of Casciian's
 * capabilities.  This one passes separate Reader/Writer to TApplication,
 * which will behave quite badly due to System.in/out not being in raw mode.
 */
public class Demo3 {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo3() {}

    // ------------------------------------------------------------------------
    // Demo3 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            DemoApplication app = new DemoApplication(System.in,
                new InputStreamReader(System.in, "IBM437"),
                new PrintWriter(new OutputStreamWriter(System.out, "IBM437")),
                true);
            (new Thread(app)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
