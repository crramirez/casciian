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

import casciian.net.TelnetServerSocket;

/**
 * This class is the main driver for a simple demonstration of Casciian's
 * capabilities.  Rather than run locally, it serves a Casciian UI over a TCP
 * port.
 */
public class Demo2 {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME = Demo2.class.getName() + "Bundle";

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
    public Demo2() {}

    // ------------------------------------------------------------------------
    // Demo2 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        ServerSocket server = null;
        try {
            if (args.length == 0) {
                System.err.println(i18n.getString("usageString"));
                return;
            }

            int port = Integer.parseInt(args[0]);
            server = new TelnetServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                System.out.println(MessageFormat.
                    format(i18n.getString("newConnection"), socket));
                DemoApplication app = new DemoApplication(socket.getInputStream(),
                    socket.getOutputStream());
                Thread.ofVirtual().start(app);
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
            }
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
