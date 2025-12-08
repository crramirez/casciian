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
package casciian.terminal;

import java.util.List;

import casciian.bits.Clipboard;

/**
 * TerminalListener is used to callback into external UI when the terminal
 * state has changed.
 */
public interface TerminalListener {

    /**
     * Function to call when the terminal state has updated.
     *
     * @param terminalState the new (now current) terminal state
     */
    public void postUpdate(final TerminalState terminalState);

    /**
     * Function to call to obtain the external UI display width.
     *
     * @return the number of columns in the display
     */
    public int getDisplayWidth();

    /**
     * Function to call to obtain the external UI display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight();

    /**
     * Get the system clipboard to use for OSC 52.
     *
     * @return the clipboard
     */
    public Clipboard getClipboard();

}
