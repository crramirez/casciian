/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
 * Copyright 2025 Carlos Rafael Ramirez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = Demo8.class.getName() + "Bundle";

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);

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
            Thread.ofVirtual().start(demoApp);
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
