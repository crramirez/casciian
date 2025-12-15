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

import casciian.net.TelnetServerSocket;

/**
 * This class is the main driver for a simple demonstration of Casciian's
 * capabilities.  Rather than run locally, it serves a Casciian UI over a TCP
 * port.
 */
public class Demo2 {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Demo2.class.getName());

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
                (new Thread(app)).start();
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
