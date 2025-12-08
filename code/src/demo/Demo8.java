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

import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.backend.*;
import demo.DemoApplication;
import casciian.net.TelnetServerSocket;


/**
 * This class shows off the use of MultiBackend and MultiScreen.
 */
public class Demo8 {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Demo8.class.getName());

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo8() {}

    // ------------------------------------------------------------------------
    // Demo8 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        ServerSocket server = null;
        try {

            /*
             * In this demo we will create a headless application that anyone
             * can telnet to.
             */

            /*
             * Check the arguments for the port to listen on.
             */
            if (args.length == 0) {
                System.err.println(i18n.getString("usageString"));
                return;
            }
            int port = Integer.parseInt(args[0]);

            /*
             * We create a headless screen and use it to establish a
             * MultiBackend.
             */
            HeadlessBackend headlessBackend = new HeadlessBackend();
            MultiBackend multiBackend = new MultiBackend(headlessBackend);

            /*
             * Now we create the shared application (a standard demo) and
             * spin it up.
             */
            DemoApplication demoApp = new DemoApplication(multiBackend);
            (new Thread(demoApp)).start();
            multiBackend.setListener(demoApp);

            /*
             * Fire up the telnet server.
             */
            server = new TelnetServerSocket(port);
            while (demoApp.isRunning()) {
                Socket socket = server.accept();
                System.out.println(MessageFormat.
                    format(i18n.getString("newConnection"), socket));

                ECMA48Backend ecmaBackend = new ECMA48Backend(demoApp,
                    socket.getInputStream(),
                    socket.getOutputStream());

                /*
                 * Add this screen to the MultiBackend, and at this point we
                 * have the telnet client able to use the shared demo
                 * application.
                 */
                multiBackend.addBackend(ecmaBackend);

                /*
                 * Emit the connection information from telnet.
                 */
                Thread.sleep(500);
                System.out.println(MessageFormat.
                    format(i18n.getString("terminal"),
                    ((casciian.net.TelnetInputStream) socket.getInputStream()).
                        getTerminalType()));
                System.out.println(MessageFormat.
                    format(i18n.getString("username"),
                    ((casciian.net.TelnetInputStream) socket.getInputStream()).
                        getUsername()));
                System.out.println(MessageFormat.
                    format(i18n.getString("language"),
                    ((casciian.net.TelnetInputStream) socket.getInputStream()).
                        getLanguage()));

            } // while (demoApp.isRunning())

            /*
             * When the application exits, kill all of the connections too.
             */
            multiBackend.shutdown();
            server.close();

            System.out.println(i18n.getString("exitMain"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (Exception e) {
                    // SQUASH
                }
            }
        }
    }

}
