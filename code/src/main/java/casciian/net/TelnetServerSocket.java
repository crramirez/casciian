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
package casciian.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * This class provides a ServerSocket that return TelnetSocket's in accept().
 */
public class TelnetServerSocket extends ServerSocket {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Creates an unbound server socket.
     *
     * @throws IOException if an I/O error occurs
     */
    public TelnetServerSocket() throws IOException {
        super();
    }

    /**
     * Creates a server socket, bound to the specified port.
     *
     * @param port the port number, or 0 to use a port number that is
     * automatically allocated.
     * @throws IOException if an I/O error occurs
     */
    public TelnetServerSocket(final int port) throws IOException {
        super(port);
    }

    /**
     * Creates a server socket and binds it to the specified local port
     * number, with the specified backlog.
     *
     * @param port the port number, or 0 to use a port number that is
     * automatically allocated.
     * @param backlog requested maximum length of the queue of incoming
     * connections.
     * @throws IOException if an I/O error occurs
     */
    public TelnetServerSocket(final int port,
        final int backlog) throws IOException {

        super(port, backlog);
    }

    /**
     * Create a server with the specified port, listen backlog, and local IP
     * address to bind to.
     *
     * @param port the port number, or 0 to use a port number that is
     * automatically allocated.
     * @param backlog requested maximum length of the queue of incoming
     * connections.
     * @param bindAddr the local InetAddress the server will bind to
     * @throws IOException if an I/O error occurs
     */
    public TelnetServerSocket(final int port, final int backlog,
        final InetAddress bindAddr) throws IOException {

        super(port, backlog, bindAddr);
    }

    // ------------------------------------------------------------------------
    // ServerSocket -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Listens for a connection to be made to this socket and accepts it. The
     * method blocks until a connection is made.
     *
     * @return the new Socket
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Socket accept() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isBound()) {
            throw new SocketException("Socket is not bound");
        }

        Socket socket = new TelnetSocket();
        implAccept(socket);
        return socket;
    }

}
