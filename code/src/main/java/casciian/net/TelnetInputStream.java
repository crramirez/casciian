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

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import casciian.backend.SessionInfo;
import static casciian.net.TelnetSocket.*;

/**
 * TelnetInputStream works with TelnetSocket to perform the telnet protocol.
 */
public class TelnetInputStream extends InputStream implements SessionInfo {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The root TelnetSocket that has my telnet protocol state.
     */
    private TelnetSocket master;

    /**
     * The raw socket's InputStream.
     */
    private InputStream input;

    /**
     * The telnet-aware OutputStream.
     */
    private TelnetOutputStream output;

    /**
     * Persistent read buffer.  In practice this will only be used if the
     * single-byte read() is called sometime.
     */
    private byte [] readBuffer;

    /**
     * Current writing position in readBuffer - what is passed into
     * input.read().
     */
    private int readBufferEnd;

    /**
     * Current read position in readBuffer - what is passed to the client in
     * response to this.read().
     */
    private int readBufferStart;

    /**
     * User name.
     */
    private String username = "";

    /**
     * Language.
     */
    private String language = "en_US";

    /**
     * Text window width.
     */
    private int windowWidth = 80;

    /**
     * Text window height.
     */
    private int windowHeight = 24;

    /**
     * When true, the last read byte from the remote side was IAC.
     */
    private boolean iac = false;

    /**
     * When true, we are in the middle of a DO/DONT/WILL/WONT negotiation.
     */
    private boolean dowill = false;

    /**
     * The telnet option being negotiated.
     */
    private int dowillType = 0;

    /**
     * When true, we are waiting to see the end of the sub-negotiation
     * sequence.
     */
    private boolean subnegEnd = false;

    /**
     * When true, the last byte read from the remote side was CR.
     */
    private boolean readCR = false;

    /**
     * The subnegotiation buffer.
     */
    private ArrayList<Byte> subnegBuffer;

    /**
     * The time this session was started.
     */
    private long startTime = System.currentTimeMillis();

    /**
     * The number of seconds since the last user input event from this
     * session.
     */
    private int idleTime = Integer.MAX_VALUE;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param master the master TelnetSocket
     * @param input the underlying socket's InputStream
     * @param output the telnet-aware OutputStream
     */
    TelnetInputStream(final TelnetSocket master, final InputStream input,
        final TelnetOutputStream output) {

        this.master = master;
        this.input  = input;
        this.output = output;

        // Setup new read buffer
        readBuffer      = new byte[1024];
        readBufferStart = 0;
        readBufferEnd   = 0;
        subnegBuffer    = new ArrayList<Byte>();
    }

    // ------------------------------------------------------------------------
    // SessionInfo ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the time this session was started.
     *
     * @return the number of millis since midnight, January 1, 1970 UTC
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the time this session was idle.
     *
     * @return the number of seconds since the last user input event from
     * this session
     */
    public int getIdleTime() {
        return idleTime;
    }

    /**
     * Set the time this session was idle.
     *
     * @param seconds the number of seconds since the last user input event
     * from this session
     */
    public void setIdleTime(final int seconds) {
        idleTime = seconds;
    }

    /**
     * Username getter.
     *
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Username setter.
     *
     * @param username the value
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Language getter.
     *
     * @return the language
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Language setter.
     *
     * @param language the value
     */
    public void setLanguage(final String language) {
        this.language = language;
    }

    /**
     * Get the terminal type as reported by the telnet Terminal Type option.
     *
     * @return the terminal type
     */
    public String getTerminalType() {
        return master.terminalType;
    }

    /**
     * Text window width getter.
     *
     * @return the window width
     */
    public int getWindowWidth() {
        return windowWidth;
    }

    /**
     * Text window height getter.
     *
     * @return the window height
     */
    public int getWindowHeight() {
        return windowHeight;
    }

    /**
     * Re-query the text window size.
     */
    public void queryWindowSize() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // InputStream ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream.
     *
     * @return an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking or 0 when it
     * reaches the end of the input stream.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        if (readBuffer == null) {
            throw new IOException("InputStream is closed");
        }
        if (readBufferEnd - readBufferStart > 0) {
            return (readBufferEnd - readBufferStart);
        }
        return input.available();
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (readBuffer != null) {
            readBuffer = null;
            input.close();
        }
    }

    /**
     * Marks the current position in this input stream.
     *
     * @param readLimit the maximum limit of bytes that can be read before
     * the mark position becomes invalid
     */
    @Override
    public void mark(final int readLimit) {
        // Do nothing
    }

    /**
     * Tests if this input stream supports the mark and reset methods.
     *
     * @return true if this stream instance supports the mark and reset
     * methods; false otherwise
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return the next byte of data, or -1 if there is no more data because
     * the end of the stream has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {

        // If the post-processed buffer has bytes, use that.
        if (readBufferEnd - readBufferStart > 0) {
            readBufferStart++;
            return readBuffer[readBufferStart - 1];
        }

        // The buffer is empty, so reset the indexes to 0.
        readBufferStart = 0;
        readBufferEnd   = 0;

        // Read some fresh data and run it through the telnet protocol.
        int rc = readImpl(readBuffer, readBufferEnd,
            readBuffer.length - readBufferEnd);

        // If we got something, return it.
        if (rc > 0) {
            readBufferEnd += rc;
            readBufferStart++;
            return readBuffer[readBufferStart - 1];
        }
        // If we read 0, I screwed up big time.
        assert (rc != 0);

        // We read -1 (EOF).
        return rc;
    }

    /**
     * Reads some number of bytes from the input stream and stores them into
     * the buffer array b.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to len bytes of data from the input stream into an array of
     * bytes.
     *
     * @param b the buffer into which the data is read.
     * @param off the start offset in array b at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] b, final int off,
        final int len) throws IOException {

        // The only time we can return 0 is if len is 0, as per the
        // InputStream contract.
        if (len == 0) {
            return 0;
        }

        // If the post-processed buffer has bytes, use that.
        if (readBufferEnd - readBufferStart > 0) {
            int n = Math.min(len, readBufferEnd - readBufferStart);
            System.arraycopy(b, off, readBuffer, readBufferStart, n);
            readBufferStart += n;
            return n;
        }

        // The buffer is empty, so reset the indexes to 0.
        readBufferStart = 0;
        readBufferEnd   = 0;

        // The maximum number of bytes we will ask for will definitely be
        // within the bounds of what we can return in a single call.
        int n = Math.min(len, readBuffer.length);

        // Read some fresh data and run it through the telnet protocol.
        int rc = readImpl(readBuffer, readBufferEnd, n);

        // If we got something, return it.
        if (rc > 0) {
            System.arraycopy(readBuffer, 0, b, off, rc);
            return rc;
        }
        // If we read 0, I screwed up big time.
        assert (rc != 0);

        // We read -1 (EOF).
        return rc;
    }

    /**
     * Repositions this stream to the position at the time the mark method
     * was last called on this input stream.  This is not supported by
     * TelnetInputStream, so IOException is always thrown.
     *
     * @throws IOException if this function is used
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("InputStream does not support mark/reset");
    }

    /**
     * Skips over and discards n bytes of data from this input stream.
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n < 0) {
            return 0;
        }
        for (int i = 0; i < n; i++) {
            read();
        }
        return n;
    }

    // ------------------------------------------------------------------------
    // TelnetInputStream ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the socket associated with this input steram.
     *
     * @return the socket
     */
    public TelnetSocket getSocket() {
        return master;
    }

    /**
     * For debugging, return a descriptive string for this telnet option.
     * These are pulled from: http://www.iana.org/assignments/telnet-options
     *
     * @param option the telnet option byte
     * @return a string describing the telnet option code
     */
    @SuppressWarnings("unused")
    private String optionString(final int option) {
        switch (option) {
        case 0: return "Binary Transmission";
        case 1: return "Echo";
        case 2: return "Reconnection";
        case 3: return "Suppress Go Ahead";
        case 4: return "Approx Message Size Negotiation";
        case 5: return "Status";
        case 6: return "Timing Mark";
        case 7: return "Remote Controlled Trans and Echo";
        case 8: return "Output Line Width";
        case 9: return "Output Page Size";
        case 10: return "Output Carriage-Return Disposition";
        case 11: return "Output Horizontal Tab Stops";
        case 12: return "Output Horizontal Tab Disposition";
        case 13: return "Output Formfeed Disposition";
        case 14: return "Output Vertical Tabstops";
        case 15: return "Output Vertical Tab Disposition";
        case 16: return "Output Linefeed Disposition";
        case 17: return "Extended ASCII";
        case 18: return "Logout";
        case 19: return "Byte Macro";
        case 20: return "Data Entry Terminal";
        case 21: return "SUPDUP";
        case 22: return "SUPDUP Output";
        case 23: return "Send Location";
        case 24: return "Terminal Type";
        case 25: return "End of Record";
        case 26: return "TACACS User Identification";
        case 27: return "Output Marking";
        case 28: return "Terminal Location Number";
        case 29: return "Telnet 3270 Regime";
        case 30: return "X.3 PAD";
        case 31: return "Negotiate About Window Size";
        case 32: return "Terminal Speed";
        case 33: return "Remote Flow Control";
        case 34: return "Linemode";
        case 35: return "X Display Location";
        case 36: return "Environment Option";
        case 37: return "Authentication Option";
        case 38: return "Encryption Option";
        case 39: return "New Environment Option";
        case 40: return "TN3270E";
        case 41: return "XAUTH";
        case 42: return "CHARSET";
        case 43: return "Telnet Remote Serial Port (RSP)";
        case 44: return "Com Port Control Option";
        case 45: return "Telnet Suppress Local Echo";
        case 46: return "Telnet Start TLS";
        case 47: return "KERMIT";
        case 48: return "SEND-URL";
        case 49: return "FORWARD_X";
        case 138: return "TELOPT PRAGMA LOGON";
        case 139: return "TELOPT SSPI LOGON";
        case 140: return "TELOPT PRAGMA HEARTBEAT";
        case 255: return "Extended-Options-List";
        default:
            if ((option >= 50) && (option <= 137)) {
                return "Unassigned";
            }
            return "UNKNOWN - OTHER";
        }
    }

    /**
     * Send a DO/DON'T/WILL/WON'T response to the remote side.
     *
     * @param response a TELNET_DO/DONT/WILL/WONT byte
     * @param option telnet option byte (binary mode, term type, etc.)
     * @throws IOException if an I/O error occurs
     */
    private void respond(final int response,
        final int option) throws IOException {

        byte [] buffer = new byte[3];
        buffer[0] = (byte) TELNET_IAC;
        buffer[1] = (byte) response;
        buffer[2] = (byte) option;

        output.rawWrite(buffer);
    }

    /**
     * Tell the remote side we WILL support an option.
     *
     * @param option telnet option byte (binary mode, term type, etc.)
     * @throws IOException if an I/O error occurs
     */
    private void WILL(final int option) throws IOException {
        respond(TELNET_WILL, option);
    }

    /**
     * Tell the remote side we WON'T support an option.
     *
     * @param option telnet option byte (binary mode, term type, etc.)
     * @throws IOException if an I/O error occurs
     */
    private void WONT(final int option) throws IOException {
        respond(TELNET_WONT, option);
    }

    /**
     * Tell the remote side we DO support an option.
     *
     * @param option telnet option byte (binary mode, term type, etc.)
     * @throws IOException if an I/O error occurs
     */
    private void DO(final int option) throws IOException {
        respond(TELNET_DO, option);
    }

    /**
     * Tell the remote side we DON'T support an option.
     *
     * @param option telnet option byte (binary mode, term type, etc.)
     * @throws IOException if an I/O error occurs
     */
    private void DONT(final int option) throws IOException {
        respond(TELNET_DONT, option);
    }

    /**
     * Tell the remote side we WON't or DON'T support an option.
     *
     * @param remoteQuery a TELNET_DO/DONT/WILL/WONT byte
     * @param option telnet option byte (binary mode, term type, etc.)
     * @throws IOException if an I/O error occurs
     */
    private void refuse(final int remoteQuery,
        final int option) throws IOException {

        if (remoteQuery == TELNET_DO) {
            WONT(option);
        } else {
            DONT(option);
        }
    }

    /**
     * Build sub-negotiation packet (RFC 855).
     *
     * @param option telnet option
     * @param response output buffer of response bytes
     * @throws IOException if an I/O error occurs
     */
    private void telnetSendSubnegResponse(final int option,
        final byte [] response) throws IOException {

        byte [] buffer = new byte[response.length + 5];
        buffer[0] = (byte) TELNET_IAC;
        buffer[1] = (byte) TELNET_SB;
        buffer[2] = (byte) option;
        System.arraycopy(response, 0, buffer, 3, response.length);
        buffer[response.length + 3] = (byte) TELNET_IAC;
        buffer[response.length + 4] = (byte) TELNET_SE;
        output.rawWrite(buffer);
    }

    /**
     * Telnet option: Terminal Speed (RFC 1079).  Client side.
     *
     * @throws IOException if an I/O error occurs
     */
    private void telnetSendTerminalSpeed() throws IOException {
        byte [] response = {0, '3', '8', '4', '0', '0', ',',
                            '3', '8', '4', '0', '0'};
        telnetSendSubnegResponse(32, response);
    }

    /**
     * Telnet option: Terminal Type (RFC 1091).  Client side.
     *
     * @throws IOException if an I/O error occurs
     */
    private void telnetSendTerminalType() throws IOException {
        byte [] response = {0, 'v', 't', '1', '0', '0' };
        telnetSendSubnegResponse(24, response);
    }

    /**
     * Telnet option: Terminal Type (RFC 1091).  Server side.
     *
     * @throws IOException if an I/O error occurs
     */
    private void requestTerminalType() throws IOException {
        byte [] response = new byte[1];
        response[0] = 1;
        telnetSendSubnegResponse(24, response);
    }

    /**
     * Telnet option: Terminal Speed (RFC 1079).  Server side.
     *
     * @throws IOException if an I/O error occurs
     */
    private void requestTerminalSpeed() throws IOException {
        byte [] response = new byte[1];
        response[0] = 1;
        telnetSendSubnegResponse(32, response);
    }

    /**
     * Telnet option: New Environment (RFC 1572).  Server side.
     *
     * @throws IOException if an I/O error occurs
     */
    private void requestEnvironment() throws IOException {
        byte [] response = new byte[1];
        response[0] = 1;
        telnetSendSubnegResponse(39, response);
    }

    /**
     * Send the options we want to negotiate on.
     *
     * <p>The options we use are:
     *
     * <p>
     * <pre>
     *     Binary Transmission           RFC 856
     *     Suppress Go Ahead             RFC 858
     *     Negotiate About Window Size   RFC 1073
     *     Terminal Type                 RFC 1091
     *     Terminal Speed                RFC 1079
     *     New Environment               RFC 1572
     *
     * When run as a server:
     *     Echo                          RFC 857
     * </pre>
     *
     * @throws IOException if an I/O error occurs
     */
    void telnetSendOptions() throws IOException {
        if (master.binaryMode == false) {
            // Binary Transmission: must ask both do and will
            DO(0);
            WILL(0);
        }

        if (master.goAhead == true) {
            // Suppress Go Ahead
            DO(3);
            WILL(3);
        }

        // Server only options
        if (master.isServer == true) {
            // Enable Echo - I echo to them, they do not echo back to me.
            DONT(1);
            WILL(1);

            if (master.doTermType == true) {
                // Terminal type - request it
                DO(24);
            }

            if (master.doTermSpeed == true) {
                // Terminal speed - request it
                DO(32);
            }

            if (master.doNAWS == true) {
                // NAWS - request it
                DO(31);
            }

            if (master.doEnvironment == true) {
                // Environment - request it
                DO(39);
            }

        } else {

            if (master.doTermType == true) {
                // Terminal type - request it
                WILL(24);
            }

            if (master.doTermSpeed == true) {
                // Terminal speed - request it
                WILL(32);
            }

            if (master.doNAWS == true) {
                // NAWS - request it
                WILL(31);
            }

            if (master.doEnvironment == true) {
                // Environment - request it
                WILL(39);
            }
        }

        // Push it all out
        output.flush();
    }

    /**
     * New Environment parsing state.
     */
    private enum EnvState {
        INIT,
        TYPE,
        NAME,
        VALUE
    }

    /**
     * Handle the New Environment option.  Note that this implementation
     * fails to handle ESC as defined in RFC 1572.
     */
    private void handleNewEnvironment() {
        Map<String, String> newEnv = new TreeMap<String, String>();

        EnvState state = EnvState.INIT;
        StringBuilder name = new StringBuilder();
        StringBuilder value = new StringBuilder();

        /*
        System.err.printf("handleNewEnvironment() %d bytes\n",
            subnegBuffer.size());
         */

        for (int i = 1; i < subnegBuffer.size(); i++) {
            Byte b = subnegBuffer.get(i);
            /*
            System.err.printf("   b: %c %d 0x%02x\n", (char)b.byteValue(),
                b, b);
             */

            switch (state) {

            case INIT:
                // Looking for "IS"
                if (b == 0) {
                    state = EnvState.TYPE;
                } else {
                    // The other side isn't following the rules, see ya.
                    return;
                }
                break;

            case TYPE:
                // Looking for "VAR" or "USERVAR"
                if (b == 0) {
                    // VAR
                    state = EnvState.NAME;
                    name = new StringBuilder();
                } else if (b == 3) {
                    // USERVAR
                    state = EnvState.NAME;
                    name = new StringBuilder();
                } else {
                    // The other side isn't following the rules, see ya
                    return;
                }
                break;

            case NAME:
                // Looking for "VALUE" or a name byte
                if (b == 1) {
                    // VALUE
                    state = EnvState.VALUE;
                    value = new StringBuilder();
                } else {
                    // Take it as an environment variable name/key byte
                    name.append((char)b.byteValue());
                }

                break;

            case VALUE:
                // Looking for "VAR", "USERVAR", or a name byte, or the end
                if (b == 0) {
                    // VAR
                    state = EnvState.NAME;
                    if (value.length() > 0) {
                        /*
                        System.err.printf("NAME: '%s' VALUE: '%s'\n",
                            name, value);
                         */
                        newEnv.put(name.toString(), value.toString());
                    }
                    name = new StringBuilder();
                } else if (b == 3) {
                    // USERVAR
                    state = EnvState.NAME;
                    if (value.length() > 0) {
                        /*
                        System.err.printf("NAME: '%s' VALUE: '%s'\n",
                            name, value);
                         */
                        newEnv.put(name.toString(), value.toString());
                    }
                    name = new StringBuilder();
                } else {
                    // Take it as an environment variable value byte
                    value.append((char)b.byteValue());
                }
                break;

            default:
                throw new RuntimeException("Invalid state: " + state);

            }
        }

        if ((name.length() > 0) && (value.length() > 0)) {
            /*
            System.err.printf("NAME: '%s' VALUE: '%s'\n", name, value);
             */
            newEnv.put(name.toString(), value.toString());
        }

        for (String key: newEnv.keySet()) {
            if (key.equals("LANG")) {
                language = newEnv.get(key);
            }
            if (key.equals("LOGNAME")) {
                username = newEnv.get(key);
            }
            if (key.equals("USER")) {
                username = newEnv.get(key);
            }
        }
    }

    /**
     * Handle an option sub-negotiation.
     *
     * @throws IOException if an I/O error occurs
     */
    private void handleSubneg() throws IOException {
        Byte option;

        // Sanity check: there must be at least 1 byte in subnegBuffer
        if (subnegBuffer.size() < 1) {
            // Buffer too small: the other side is a broken telnetd, it did
            // not send the right sub-negotiation data.  Bail out now.
            return;
        }
        option = subnegBuffer.get(0);

        switch (option) {

        case 24:
            // Terminal Type
            if ((subnegBuffer.size() > 1) && (subnegBuffer.get(1) == 1)) {
                // Server sent "SEND", we say "IS"
                telnetSendTerminalType();
            }
            if ((subnegBuffer.size() > 1) && (subnegBuffer.get(1) == 0)) {
                // Client sent "IS", record it
                StringBuilder terminalString = new StringBuilder();
                for (int i = 2; i < subnegBuffer.size(); i++) {
                    terminalString.append((char)subnegBuffer.
                        get(i).byteValue());
                }
                master.terminalType = terminalString.toString();
                /*
                System.err.printf("terminal type: '%s'\n",
                    master.terminalType);
                 */
            }
            break;

        case 32:
            // Terminal Speed
            if ((subnegBuffer.size() > 1) && (subnegBuffer.get(1) == 1)) {
                // Server sent "SEND", we say "IS"
                telnetSendTerminalSpeed();
            }
            if ((subnegBuffer.size() > 1) && (subnegBuffer.get(1) == 0)) {
                // Client sent "IS", record it
                StringBuilder speedString = new StringBuilder();
                for (int i = 2; i < subnegBuffer.size(); i++) {
                    speedString.append((char)subnegBuffer.get(i).byteValue());
                }
                master.terminalSpeed = speedString.toString();
                /*
                System.err.printf("terminal speed: '%s'\n",
                    master.terminalSpeed);
                 */
            }
            break;

        case 31:
            // NAWS
            if (subnegBuffer.size() >= 5) {
                int i = 0;

                i++;
                if (subnegBuffer.get(i) == (byte) TELNET_IAC) {
                    i++;
                }
                int width = subnegBuffer.get(i);
                if (width < 0) {
                    width += 256;
                }
                windowWidth = width * 256;

                i++;
                if (subnegBuffer.get(i) == (byte) TELNET_IAC) {
                    i++;
                }
                width = subnegBuffer.get(i);
                windowWidth += width;
                if (width < 0) {
                    windowWidth += 256;
                }

                i++;
                if (subnegBuffer.get(i) == (byte) TELNET_IAC) {
                    i++;
                }
                int height = subnegBuffer.get(i);
                if (height < 0) {
                    height += 256;
                }
                windowHeight = height * 256;

                i++;
                if (subnegBuffer.get(i) == (byte) TELNET_IAC) {
                    i++;
                }
                height = subnegBuffer.get(i);
                windowHeight += height;
                if (height < 0) {
                    windowHeight += 256;
                }
            }
            break;

        case 39:
            // Environment
            handleNewEnvironment();
            break;

        default:
            // Ignore this one
            break;
        }
    }

    /**
     * Reads up to len bytes of data from the input stream into an array of
     * bytes.
     *
     * @param buf the buffer into which the data is read.
     * @param off the start offset in array b at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs
     */
    private int readImpl(final byte[] buf, final int off,
        final int len) throws IOException {

        assert (len > 0);

        // The current writing position in buf.
        int bufN = off;

        // We will keep trying to read() until we have something to return.
        do {

            byte [] buffer = null;
            if (master.binaryMode) {
                // Binary mode: read up to len bytes.  There will never be
                // more bytes to pass upstream than there are bytes on the
                // wire.
                buffer = new byte[len];
            } else {
                // ASCII mode: read up to len - 2 bytes.  There may have been
                // some combination of IAC, CR, and NUL from a previous
                // readImpl() that could result in more bytes to pass up than
                // are on the wire.
                buffer = new byte[len - 2];
            }

            int bufferN = 0;

            // Read some data from the other end
            int rc = input.read(buffer);

            // Check for EOF or error
            if (rc > 0) {
                // More data came in
                bufferN = rc;
            } else {
                // EOF, just return it.
                return rc;
            }

            // Loop through the read bytes
            for (int i = 0; i < bufferN; i++) {
                byte b = buffer[i];

                if (subnegEnd == true) {
                    // Looking for IAC SE to end this subnegotiation
                    if (b == (byte) TELNET_SE) {
                        if (iac == true) {
                            iac = false;
                            subnegEnd = false;
                            handleSubneg();
                        }
                    } else if (b == (byte) TELNET_IAC) {
                        if (iac == true) {
                            // An argument to the subnegotiation option
                            subnegBuffer.add((byte) TELNET_IAC);
                        } else {
                            iac = true;
                        }
                    } else {
                        // An argument to the subnegotiation option
                        subnegBuffer.add(b);
                    }
                    continue;
                }

                // Look for DO/DON'T/WILL/WON'T option
                if (dowill == true) {

                    // Look for option/
                    switch (b) {

                    case 0:
                        // Binary Transmission
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use binary transmission, yay.
                            master.binaryMode = true;
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for binary transmission.
                            WILL(b);
                            master.binaryMode = true;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // We're screwed, server won't do binary
                            // transmission.
                            master.binaryMode = false;
                        } else {
                            // Server demands NVT ASCII mode.
                            master.binaryMode = false;
                        }
                        break;

                    case 1:
                        // Echo
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use echo, yay.
                            master.echoMode = true;
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for echo.
                            WILL(b);
                            master.echoMode = true;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // We're screwed, server won't do echo.
                            master.echoMode = false;
                        } else {
                            // Server demands no echo.
                            master.echoMode = false;
                        }
                        break;

                    case 3:
                        // Suppress Go Ahead
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use suppress go-ahead, yay.
                            master.goAhead = false;
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for suppress go-ahead.
                            WILL(b);
                            master.goAhead = false;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // We're screwed, server won't do suppress
                            // go-ahead.
                            master.goAhead = true;
                        } else {
                            // Server demands Go-Ahead mode.
                            master.goAhead = true;
                        }
                        break;

                    case 24:
                        // Terminal Type - send what's in TERM
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use terminal type, yay.
                            if (master.isServer
                                && master.doTermType
                            ) {
                                requestTerminalType();
                                master.doTermType = false;
                            } else if (!master.isServer) {
                                master.doTermType = true;
                            }
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for terminal type.
                            WILL(b);
                            master.doTermType = true;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // We're screwed, server won't do terminal type.
                            master.doTermType = false;
                        } else {
                            // Server will not listen to terminal type.
                            master.doTermType = false;
                        }
                        break;

                    case 31:
                        // NAWS
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use NAWS, yay.
                            master.doNAWS = true;
                            // NAWS cannot be requested by the server, it is
                            // only sent by the client.
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for NAWS.
                            WILL(b);
                            master.doNAWS = true;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // Server won't do NAWS.
                            master.doNAWS = false;
                        } else {
                            // Server will not listen to NAWS.
                            master.doNAWS = false;
                        }
                        break;

                    case 32:
                        // Terminal Speed
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use terminal speed, yay.
                            if (master.isServer
                                && master.doTermSpeed
                            ) {
                                requestTerminalSpeed();
                                master.doTermSpeed = false;
                            } else if (!master.isServer) {
                                master.doTermSpeed = true;
                            }
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for terminal speed.
                            WILL(b);
                            master.doTermSpeed = true;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // We're screwed, server won't do terminal speed.
                            master.doTermSpeed = false;
                        } else {
                            // Server will not listen to terminal speed.
                            master.doTermSpeed = false;
                        }
                        break;

                    case 39:
                        // New Environment
                        if (dowillType == (byte) TELNET_WILL) {
                            // Server will use NewEnvironment, yay.
                            if (master.isServer
                                && master.doEnvironment
                            ) {
                                requestEnvironment();
                                master.doEnvironment = false;
                            } else if (!master.isServer) {
                                master.doEnvironment = true;
                            }
                        } else if (dowillType == (byte) TELNET_DO) {
                            // Server asks for NewEnvironment.
                            WILL(b);
                            master.doEnvironment = true;
                        } else if (dowillType == (byte) TELNET_WONT) {
                            // Server won't do NewEnvironment.
                            master.doEnvironment = false;
                        } else {
                            // Server will not listen to New Environment.
                            master.doEnvironment = false;
                        }
                        break;


                    default:
                        // Other side asked for something we don't
                        // understand.  Tell them we will not do this option.
                        refuse(dowillType, b);
                        break;
                    }

                    dowill = false;
                    continue;
                } // if (dowill == true)

                // Perform read processing
                if (b == (byte) TELNET_IAC) {

                    // Telnet command
                    if (iac == true) {
                        // IAC IAC -> IAC
                        buf[bufN++] = (byte) TELNET_IAC;
                        iac = false;
                    } else {
                        iac = true;
                    }
                    continue;
                } else {
                    if (iac == true) {

                        switch (b) {

                        case (byte) TELNET_SE:
                            // END Sub-Negotiation
                            break;
                        case (byte) TELNET_NOP:
                            // NOP
                            break;
                        case (byte) TELNET_DM:
                            // Data Mark
                            break;
                        case (byte) TELNET_BRK:
                            // Break
                            break;
                        case (byte) TELNET_IP:
                            // Interrupt Process
                            break;
                        case (byte) TELNET_AO:
                            // Abort Output
                            break;
                        case (byte) TELNET_AYT:
                            // Are You There?
                            break;
                        case (byte) TELNET_EC:
                            // Erase Character
                            break;
                        case (byte) TELNET_EL:
                            // Erase Line
                            break;
                        case (byte) TELNET_GA:
                            // Go Ahead
                            break;
                        case (byte) TELNET_SB:
                            // START Sub-Negotiation
                            // From here we wait for the IAC SE
                            subnegEnd = true;
                            subnegBuffer.clear();
                            break;
                        case (byte) TELNET_WILL:
                            // WILL
                            dowill = true;
                            dowillType = b;
                            break;
                        case (byte) TELNET_WONT:
                            // WON'T
                            dowill = true;
                            dowillType = b;
                            break;
                        case (byte) TELNET_DO:
                            // DO
                            dowill = true;
                            dowillType = b;
                            break;
                        case (byte) TELNET_DONT:
                            // DON'T
                            dowill = true;
                            dowillType = b;
                            break;
                        default:
                            // This should be equivalent to IAC NOP
                            break;
                        }
                        iac = false;
                        continue;

                    } // if (iac == true)

                    /*
                     * All of the regular IAC processing is completed at this
                     * point.  Now we need to handle the CR and CR LF cases.
                     *
                     * According to RFC 854, in NVT ASCII mode:
                     *     Bare CR -> CR NUL
                     *     CR LF -> CR LF
                     *
                     */
                    if (master.binaryMode == false) {

                        if (b == C_LF) {
                            if (readCR == true) {
                                // This is CR LF.  Send CR LF and turn the cr
                                // flag off.
                                buf[bufN++] = C_CR;
                                buf[bufN++] = C_LF;
                                readCR = false;
                                continue;
                            }
                            // This is bare LF.  Send LF.
                            buf[bufN++] = C_LF;
                            continue;
                        }

                        if (b == C_NUL) {
                            if (readCR == true) {
                                // This is CR NUL.  Send CR and turn the cr
                                // flag off.
                                buf[bufN++] = C_CR;
                                readCR = false;
                                continue;
                            }
                            // This is bare NUL.  Send NUL.
                            buf[bufN++] = C_NUL;
                            continue;
                        }

                        if (b == C_CR) {
                            if (readCR == true) {
                                // This is CR CR.  Send a CR NUL and leave
                                // the cr flag on.
                                buf[bufN++] = C_CR;
                                buf[bufN++] = C_NUL;
                                continue;
                            }
                            // This is the first CR.  Set the cr flag.
                            readCR = true;
                            continue;
                        }

                        if (readCR == true) {
                            // This was a bare CR in the stream.
                            buf[bufN++] = C_CR;
                            readCR = false;
                        }

                        // This is a regular character.  Pass it on.
                        buf[bufN++] = b;
                        continue;
                    }

                    /*
                     * This is the case for any of:
                     *
                     *     1) A NVT ASCII character that isn't CR, LF, or
                     *        NUL.
                     *
                     *     2) A NVT binary character.
                     *
                     * For all of these cases, we just pass the character on.
                     */
                    buf[bufN++] = b;

                } // if (b == TELNET_IAC)

            } // for (int i = 0; i < bufferN; i++)

        } while (bufN == 0);

        // Return bytes read
        return bufN;
    }

}
