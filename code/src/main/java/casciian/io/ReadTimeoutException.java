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

/**
 * ReadTimeoutException is thrown by TimeoutInputStream.read() when bytes are
 * not available within the timeout specified.
 */
public class ReadTimeoutException extends IOException {

    /**
     * Serializable version.
     */
    private static final long serialVersionUID = 1;

    /**
     * Construct an instance with a message.
     *
     * @param msg exception text
     */
    public ReadTimeoutException(String msg) {
        super(msg);
    }
}
