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

import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import casciian.backend.SwingBackend;
import casciian.event.TMenuEvent;

/**
 * This class is the main driver for a simple demonstration of Jexer's
 * capabilities.  It shows two Swing demo applications running in the same
 * Swing UI.  The application in the left panel starts in English, and the
 * application in the right panel starts in Spanish.
 */
public class Demo5 implements WindowListener {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Demo5.class.getName());

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The first demo application instance.
     */
    DemoApplication app1 = null;

    /**
     * The second demo application instance.
     */
    DemoApplication app2 = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo5() {}

    // ------------------------------------------------------------------------
    // WindowListener ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowActivated(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowClosed(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowClosing(final WindowEvent event) {
        if (app1 != null) {
            app1.exit();
        }
        if (app2 != null) {
            app2.exit();
        }
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowDeactivated(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowDeiconified(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowIconified(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowOpened(final WindowEvent event) {
        // Ignore
    }

    // ------------------------------------------------------------------------
    // Demo5 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Run two demo applications in separate panes.
     */
    private void addApplications() {

        /*
         * In this demo we will create two swing panels with two
         * independently running applications, each with a different font
         * size.
         */

        /*
         * First we create a panel to put it on.  We need this to pass to
         * SwingBackend's constructor, so that it knows not to create a new
         * frame.
         */
        JPanel app1Panel = new JPanel();

        /*
         * Next, we create the Swing backend.  The "listener" (second
         * argument, set to null) is what the backend wakes up on every event
         * received.  Typically this is the TApplication.  TApplication sets
         * it in its constructor, so we can pass null here and be fine.
         */
        SwingBackend app1Backend = new SwingBackend(app1Panel, null,
            80, 25, 16);
        // Now that we have the backend, construct the TApplication.
        app1 = new DemoApplication(app1Backend);
        app1.onMenu(new TMenuEvent(app1Backend, 10005));

        /*
         * The second panel is the same sequence, except that we also change
         * the font from the default Terminus to JVM monospaced.
         */
        JPanel app2Panel = new JPanel();
        SwingBackend app2Backend = new SwingBackend(app2Panel, null,
            80, 25, 18);
        app2 = new DemoApplication(app2Backend);
        app2.onMenu(new TMenuEvent(app2Backend, 10006));
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 18);
        app2Backend.setFont(font);

        /*
         * Now that the applications are ready, spin them off on their
         * threads.
         */
        (new Thread(app1)).start();
        (new Thread(app2)).start();

        /*
         * The rest of this is standard Swing.  Set up a frame, a split pane,
         * put each of the panels on it, and make it visible.
         */
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            app1Panel, app2Panel);
        mainPane.setOneTouchExpandable(true);
        mainPane.setDividerLocation(500);
        mainPane.setDividerSize(6);
        mainPane.setBorder(null);
        frame.setContentPane(mainPane);

        frame.setTitle(i18n.getString("frameTitle"));
        frame.setSize(1000, 640);
        frame.setVisible(true);
    }

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            Demo5 demo = new Demo5();
            demo.addApplications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
