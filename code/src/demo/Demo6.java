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

import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.backend.*;
import demo.DemoApplication;

/**
 * This class shows off the use of MultiBackend and MultiScreen.
 */
public class Demo6 {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Demo6.class.getName());

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo6() {}

    // ------------------------------------------------------------------------
    // Demo6 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {

            /*
             * In this demo we will create two applications spanning three
             * screens.  One of the applications will have both an ECMA48
             * screen and a Swing screen, with all I/O mirrored between them.
             * The second application will have a Swing screen containing a
             * window showing the first application, also mirroring I/O
             * between the window and the other two screens.
             */

            /*
             * We create the first screen and use it to establish a
             * MultiBackend.
             */
            ECMA48Backend ecmaBackend = new ECMA48Backend();
            MultiBackend multiBackend = new MultiBackend(ecmaBackend);

            /*
             * Now we create the first application (a standard demo).
             */
            DemoApplication demoApp = new DemoApplication(multiBackend);

            /*
             * We will need the width and height of the ECMA48 screen, so get
             * the Screen reference now.
             */
            Screen multiScreen = multiBackend.getScreen();

            /*
             * Now we create the second screen (backend) for the first
             * application.  It will be the same size as the ECMA48 screen,
             * with a font size of 16 points.
             */
            SwingBackend swingBackend = new SwingBackend(multiScreen.getWidth(),
                multiScreen.getHeight(), 16);

            /*
             * Add this screen to the MultiBackend, and at this point we have
             * one demo application spanning two physical screens.
             */
            multiBackend.addBackend(swingBackend);
            multiBackend.setListener(demoApp);

            /*
             * Time for the second application.  This one will have a single
             * window mirroring the contents of the first application.  Let's
             * make it a little larger than the first application's
             * width/height.
             */
            int width = multiScreen.getWidth();
            int height = multiScreen.getHeight();

            /*
             * Make a new Swing window for the second application.
             */
            SwingBackend monitorBackend = new SwingBackend(width + 5,
                height + 5, 20);

            /*
             * Setup the second application, give it the basic file and
             * window menus.
             */
            TApplication monitor = new TApplication(monitorBackend);
            monitor.addToolMenu();
            monitor.addFileMenu();
            monitor.addWindowMenu();

            /*
             * Now add the third screen to the first application.  We want to
             * change the object it locks on in its draw() method to the
             * MultiScreen, that will dramatically reduce (not totally
             * eliminate) screen tearing/artifacts.
             */
            TWindowBackend windowBackend = new TWindowBackend(demoApp,
                monitor, i18n.getString("monitorWindow"),
                width + 2, height + 2);
            windowBackend.setDrawLock(multiScreen);
            windowBackend.setOtherApplication(demoApp);
            multiBackend.addBackend(windowBackend, true);

            /*
             * Three screens, two applications: spin them up!
             */
            (new Thread(demoApp)).start();
            (new Thread(monitor)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
