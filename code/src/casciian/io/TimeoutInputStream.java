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
package casciian.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides an optional millisecond timeout on its read()
 * operations.  This permits callers to bail out rather than block.
 */
public class TimeoutInputStream extends InputStream {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The wrapped stream.
     */
    private InputStream stream;

    /**
     * The timeout value in millis.  If it takes longer than this for bytes
     * to be available for read then a ReadTimeoutException is thrown.  A
     * value of 0 means to block as a normal InputStream would.
     */
    private int timeoutMillis;

    /**
     * If true, the current read() will timeout soon.
     */
    private volatile boolean cancel = false;

    /**
     * If true, EOF was encountered.
     */
    private boolean eof = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor, at the default timeout of 10000 millis (10
     * seconds).
     *
     * @param stream the wrapped InputStream
     */
    public TimeoutInputStream(final InputStream stream) {
        this.stream             = stream;
        this.timeoutMillis      = 10000;
    }

    /**
     * Public constructor.
     *
     * @param stream the wrapped InputStream
     * @param timeoutMillis the timeout value in millis.  If it takes longer
     * than this for bytes to be available for read then a
     * ReadTimeoutException is thrown.  A value of 0 means to block as a
     * normal InputStream would.
     */
    public TimeoutInputStream(final InputStream stream,
        final int timeoutMillis) {

        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeoutMillis value, " +
                "must be >= 0");
        }

        this.stream             = stream;
        this.timeoutMillis      = timeoutMillis;
    }

    // ------------------------------------------------------------------------
    // InputStream ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return the next byte of data, or -1 if there is no more data because
     * the end of the stream has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {

        if (eof) {
            return -1;
        }

        if (timeoutMillis == 0) {
            // Block on the read().
            int rc = stream.read();
            if (rc == -1) {
                eof = true;
            }
            return rc;
        }

        if (stream.available() > 0) {
            // A byte is available now, return it.
            int rc = stream.read();
            if (rc == -1) {
                eof = true;
            }
            return rc;
        }

        // We will wait up to timeoutMillis to see if a byte is available.
        // If not, we throw ReadTimeoutException.
        long checkTime = System.currentTimeMillis();
        while (stream.available() == 0) {
            long now = System.currentTimeMillis();
            synchronized (this) {
                if ((now - checkTime > timeoutMillis) || (cancel == true)) {
                    if (cancel == true) {
                        cancel = false;
                    }
                    throw new ReadTimeoutException("Timeout on read(): " +
                        (int) (now - checkTime) + " millis and still no data");
                }
            }
            try {
                // How long do we sleep for, eh?  For now we will go with 2
                // millis.
                Thread.sleep(2);
            } catch (InterruptedException e) {
                // SQUASH
            }
        }

        if (stream.available() > 0) {
            // A byte is available now, return it.
            int rc = stream.read();
            if (rc == -1) {
                eof = true;
            }
            return rc;
        }

        throw new IOException("InputStream claimed a byte was available, but " +
            "now it is not.  What is going on?");
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

        if (eof) {
            return -1;
        }

        if (timeoutMillis == 0) {
            // Block on the read().
            int rc = stream.read(b);
            if (rc == -1) {
                eof = true;
            }
            return rc;
        }

        int remaining = b.length;

        if (stream.available() >= remaining) {
            // Enough bytes are available now, return them.
            int rc = stream.read(b);
            if (rc == -1) {
                eof = true;
            }
            return rc;
        }

        while (remaining > 0) {

            // We will wait up to timeoutMillis to see if a byte is
            // available.  If not, we throw ReadTimeoutException.
            long checkTime = System.currentTimeMillis();
            while (stream.available() == 0) {
                if ((remaining > 0) && (remaining != b.length)) {
                    return (b.length - remaining);
                }

                long now = System.currentTimeMillis();
                synchronized (this) {
                    if ((now - checkTime > timeoutMillis) || (cancel == true)) {
                        if (cancel == true) {
                            cancel = false;
                        }
                        throw new ReadTimeoutException("Timeout on read(): " +
                            (int) (now - checkTime) + " millis and still no " +
                            "data");
                    }
                }
                try {
                    // How long do we sleep for, eh?  For now we will go with
                    // 2 millis.
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    // SQUASH
                }
            }

            if (stream.available() > 0) {
                // At least one byte is available now, read it.
                int n = stream.available();
                if (remaining < n) {
                    n = remaining;
                }
                int rc = stream.read(b, b.length - remaining, n);
                if (rc == -1) {
                    eof = true;

                    // This shouldn't happen.
                    throw new IOException("InputStream claimed bytes were " +
                        "available, but read() returned -1.  What is going " +
                        "on?");
                }
                remaining -= rc;
                if (remaining == 0) {
                    return b.length;
                }
            }
        }

        throw new IOException("InputStream claimed all bytes were available, " +
            "but now it is not.  What is going on?");
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

        if (eof) {
            return -1;
        }

        if (timeoutMillis == 0) {
            // Block on the read().
            int rc = stream.read(b, off, len);
            if (rc == -1) {
                eof = true;
            }
            return rc;
        }

        int remaining = len;

        if (stream.available() >= remaining) {
            // Enough bytes are available now, return them.
            int rc = stream.read(b, off, remaining);
            if (rc <= 0) {
                eof = true;
            }
            return rc;
        }

        while (remaining > 0) {

            // We will wait up to timeoutMillis to see if a byte is
            // available.  If not, we throw ReadTimeoutException.
            long checkTime = System.currentTimeMillis();
            while (stream.available() == 0) {
                if ((remaining > 0) && (remaining != len)) {
                    return (len - remaining);
                }

                long now = System.currentTimeMillis();
                synchronized (this) {
                    if ((now - checkTime > timeoutMillis) || (cancel == true)) {
                        if (cancel == true) {
                            cancel = false;
                        }
                        throw new ReadTimeoutException("Timeout on read(): " +
                            (int) (now - checkTime) + " millis and still no " +
                            "data");
                    }
                }
                try {
                    // How long do we sleep for, eh?  For now we will go with
                    // 2 millis.
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    // SQUASH
                }
            }

            if (stream.available() > 0) {
                // At least one byte is available now, read it.
                int n = stream.available();
                if (remaining < n) {
                    n = remaining;
                }
                int rc = stream.read(b, off + len - remaining, n);
                if (rc <= 0) {
                    eof = true;

                    // This shouldn't happen.
                    throw new IOException("InputStream claimed bytes were " +
                        "available, but read() returned -1.  What is going " +
                        "on?");
                }
                remaining -= rc;
                if (remaining == 0) {
                    return len;
                }
            }
        }

        throw new IOException("InputStream claimed all bytes were available, " +
            "but now it is not.  What is going on?");
    }

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
        return stream.available();
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }

    /**
     * Marks the current position in this input stream.
     *
     * @param readLimit the maximum limit of bytes that can be read before
     * the mark position becomes invalid
     */
    @Override
    public void mark(final int readLimit) {
        stream.mark(readLimit);
    }

    /**
     * Tests if this input stream supports the mark and reset methods.
     *
     * @return true if this stream instance supports the mark and reset
     * methods; false otherwise
     */
    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

    /**
     * Repositions this stream to the position at the time the mark method
     * was last called on this input stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        stream.reset();
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
        return stream.skip(n);
    }

    // ------------------------------------------------------------------------
    // TimeoutInputStream -----------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Request that the current read() operation timeout immediately.
     */
    public synchronized void cancelRead() {
        cancel = true;
    }

    /**
     * Get the underlying stream.
     *
     * @return the stream
     */
    public InputStream getStream() {
        return stream;
    }

}
