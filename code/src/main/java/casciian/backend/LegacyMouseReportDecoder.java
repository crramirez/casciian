/*
 * Casciian - Java Text User Interface
 *
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
package casciian.backend;

/**
 * Decoder for the raw bytes that follow {@code ESC [ M} in a mouse report.
 *
 * <p>Two xterm mouse encodings share the {@code ESC [ M Cb Cx Cy} prefix:
 * <ul>
 *   <li><b>Legacy X10 / normal encoding</b> (no extended mode): each of Cb,
 *       Cx and Cy is a single byte holding {@code value + 32}, so positions
 *       1-223 map to bytes 0x21-0xFF.  Bytes 0x80-0xFF are not valid
 *       standalone UTF-8, which is why they must be read before any UTF-8
 *       decoding.  xterm sends a NUL byte for a position beyond 223.</li>
 *   <li><b>UTF-8 extended encoding (mode 1005)</b>: the same values are
 *       encoded as UTF-8 characters, so values of 128 and above take two
 *       bytes.  xterm limits coordinates to 2015 (value 2047, the largest
 *       two-byte UTF-8 character), and also sends a NUL for positions beyond
 *       that.</li>
 * </ul>
 * The SGR encoding (mode 1006, {@code ESC [ < Cb ; Cx ; Cy M/m}) uses
 * decimal parameters and a different prefix, so it is not handled here.
 *
 * <p>For any report whose bytes are all below 0x80 both encodings agree.
 * Otherwise the encoding cannot always be determined from the bytes alone:
 * for example {@code 0xC3 0xA0} is either two X10 coordinates (163, 128) or
 * one 1005 coordinate (192).  The decoder therefore works with an
 * {@link Encoding} that is ideally learned from the terminal's answer to a
 * DECRQM query for mode 1005 (see {@link #setEncoding(Encoding)}).  While the
 * encoding is {@link Encoding#UNKNOWN} (terminal does not answer DECRQM, or
 * the answer has not arrived yet), each report is resolved structurally:
 * <ul>
 *   <li>If the bytes are not a valid 1005 report (a byte 0x80-0xC1 or
 *       0xE0-0xFF where a value starts, or a lead byte not followed by a
 *       continuation byte) it can only be X10.  This proves the terminal is
 *       using X10, so the encoding is locked to X10.</li>
 *   <li>If the bytes form a complete 1005 report longer than three bytes it
 *       is decoded as 1005.  The X10 reading would require the bytes after
 *       the report to be unrelated input that arrived in the same burst; if
 *       the first such byte is a UTF-8 continuation byte, which can never
 *       start new input, the encoding is locked to 1005.</li>
 *   <li>If a complete X10 report is a valid but incomplete 1005 report, the
 *       decoder waits for more bytes.  Terminals write each report
 *       atomically, so if nothing completes the 1005 reading before
 *       {@link #timeout()} is called the report is decoded as X10.</li>
 * </ul>
 * Once the encoding is known it is authoritative: with {@link Encoding#UTF8}
 * only a complete 1005 report is accepted, and with {@link Encoding#X10}
 * only a complete X10 report.
 * Malformed or incomplete reports are aborted, and the collected bytes are
 * handed back to the caller to be processed as ordinary input.
 *
 * <p>The decoder does not allocate on the normal path; it only allocates a
 * small array when bytes must be handed back to the caller.
 */
final class LegacyMouseReportDecoder {

    /**
     * The encoding of {@code ESC [ M} mouse reports.
     */
    enum Encoding {
        /**
         * Not yet known: resolve each report structurally.
         */
        UNKNOWN,

        /**
         * Legacy X10 single-byte values.
         */
        X10,

        /**
         * UTF-8 encoded values (mode 1005).
         */
        UTF8
    }

    /**
     * Result: more bytes are needed.
     */
    static final int NEED_MORE = 0;

    /**
     * Result: a report was decoded; see {@link #getButtons()},
     * {@link #getX()}, {@link #getY()} and {@link #takeLeftovers()}.
     */
    static final int COMPLETE = 1;

    /**
     * Result: the bytes are not a mouse report; see
     * {@link #takeLeftovers()}.
     */
    static final int ABORT = 2;

    /**
     * Internal parse result: more bytes are needed.
     */
    private static final int PARSE_NEED_MORE = 0;

    /**
     * Internal parse result: the bytes form a complete report.
     */
    private static final int PARSE_COMPLETE = 1;

    /**
     * Internal parse result: the bytes cannot be a report.
     */
    private static final int PARSE_INVALID = 2;

    /**
     * The largest number of bytes a report can use (three two-byte UTF-8
     * values).
     */
    private static final int MAX_BYTES = 6;

    /**
     * Shared empty result for {@link #takeLeftovers()}.
     */
    private static final int[] NO_BYTES = new int[0];

    /**
     * Bytes collected after ESC [ M.
     */
    private final int[] pending = new int[MAX_BYTES];

    /**
     * Number of valid entries in pending.
     */
    private int count = 0;

    /**
     * Current encoding.
     */
    private Encoding encoding = Encoding.UNKNOWN;

    /**
     * Raw values (with the +32 offset) of the last decoded report.
     */
    private final int[] values = new int[3];

    /**
     * Values parsed by the UTF-8 interpretation.
     */
    private final int[] utf8Values = new int[3];

    /**
     * Bytes used by the UTF-8 interpretation.
     */
    private int utf8Consumed = 0;

    /**
     * Bytes used by the last decoded report; the rest are leftovers.
     */
    private int consumed = 0;

    /**
     * Get the current encoding.
     *
     * @return the encoding
     */
    Encoding getEncoding() {
        return encoding;
    }

    /**
     * Set the encoding, typically from the terminal's DECRQM response for
     * mode 1005.
     *
     * @param encoding the encoding
     */
    void setEncoding(final Encoding encoding) {
        this.encoding = encoding;
    }

    /**
     * Discard any collected bytes.  The encoding is kept.
     */
    void reset() {
        count = 0;
        consumed = 0;
    }

    /**
     * Check whether bytes have been collected for an undecided report.
     *
     * @return true if bytes are pending
     */
    boolean hasPending() {
        return count > 0;
    }

    /**
     * Feed the next raw byte following ESC [ M.
     *
     * @param b the byte, as an unsigned value 0-255
     * @return {@link #NEED_MORE}, {@link #COMPLETE} or {@link #ABORT}
     */
    int add(final int b) {
        if (count == MAX_BYTES) {
            // Cannot happen: a decision is always reached by MAX_BYTES.
            return abort();
        }
        pending[count++] = b;
        return decide(false);
    }

    /**
     * Resolve the collected bytes because no more input arrived in time.
     *
     * @return {@link #COMPLETE} or {@link #ABORT}
     */
    int timeout() {
        return decide(true);
    }

    /**
     * Get the raw button value Cb (including the +32 offset) of the last
     * decoded report.
     *
     * @return Cb
     */
    int getButtons() {
        return values[0];
    }

    /**
     * Get the raw column value Cx (position + 32, or 0 for "beyond the
     * encodable range") of the last decoded report.
     *
     * @return Cx
     */
    int getX() {
        return values[1];
    }

    /**
     * Get the raw row value Cy (position + 32, or 0 for "beyond the
     * encodable range") of the last decoded report.
     *
     * @return Cy
     */
    int getY() {
        return values[2];
    }

    /**
     * Return the bytes that were collected but are not part of the decoded
     * report (all collected bytes after {@link #ABORT}), and reset.  These
     * must be processed by the caller as ordinary input.
     *
     * @return the leftover bytes, possibly empty
     */
    int[] takeLeftovers() {
        int[] result = NO_BYTES;
        if (count > consumed) {
            result = new int[count - consumed];
            System.arraycopy(pending, consumed, result, 0, result.length);
        }
        reset();
        return result;
    }

    /**
     * Decide what the collected bytes are.
     *
     * @param timedOut if true, no more bytes will be considered
     * @return {@link #NEED_MORE}, {@link #COMPLETE} or {@link #ABORT}
     */
    private int decide(final boolean timedOut) {
        int x10 = parseX10();

        if (encoding == Encoding.X10) {
            if (x10 == PARSE_COMPLETE) {
                return acceptX10();
            }
            if ((x10 == PARSE_INVALID) || timedOut) {
                return abort();
            }
            return NEED_MORE;
        }

        int utf8 = parseUtf8();
        if (encoding == Encoding.UTF8) {
            // Mode 1005 is confirmed: only a complete UTF-8 report counts.
            if (utf8 == PARSE_COMPLETE) {
                return acceptUtf8();
            }
            if ((utf8 == PARSE_INVALID) || timedOut) {
                return abort();
            }
            return NEED_MORE;
        }

        if (utf8 == PARSE_COMPLETE) {
            if ((encoding == Encoding.UNKNOWN)
                && (utf8Consumed > 3)
                && (pending[3] >= 0x80) && (pending[3] <= 0xBF)
            ) {
                // Under X10 this continuation byte would have to start new
                // input, which is impossible: the terminal uses 1005.
                encoding = Encoding.UTF8;
            }
            return acceptUtf8();
        }

        if (utf8 == PARSE_INVALID) {
            if (x10 == PARSE_COMPLETE) {
                if (encoding == Encoding.UNKNOWN) {
                    // Not valid 1005 but valid X10: the terminal uses X10.
                    encoding = Encoding.X10;
                }
                return acceptX10();
            }
            if ((x10 == PARSE_INVALID) || timedOut) {
                return abort();
            }
            return NEED_MORE;
        }

        // The UTF-8 interpretation needs more bytes.
        if (!timedOut) {
            return NEED_MORE;
        }
        if (x10 == PARSE_COMPLETE) {
            // The terminal sent a complete X10 report and nothing more.
            return acceptX10();
        }
        return abort();
    }

    /**
     * Check one value byte for validity.  Cb is always at least 32, while
     * coordinates are at least 33 or NUL for "out of range".
     *
     * @param index 0 for Cb, 1 for Cx, 2 for Cy
     * @param b the value
     * @return true if the value is acceptable
     */
    private static boolean isValidValue(final int index, final int b) {
        if (index == 0) {
            return b >= 0x20;
        }
        return (b == 0) || (b >= 0x21);
    }

    /**
     * Interpret the collected bytes as an X10 report.
     *
     * @return a PARSE_* result
     */
    private int parseX10() {
        int n = Math.min(count, 3);
        for (int i = 0; i < n; i++) {
            if (!isValidValue(i, pending[i])) {
                return PARSE_INVALID;
            }
        }
        return (count >= 3) ? PARSE_COMPLETE : PARSE_NEED_MORE;
    }

    /**
     * Interpret the collected bytes as a 1005 (UTF-8) report.
     *
     * @return a PARSE_* result
     */
    private int parseUtf8() {
        int pos = 0;
        for (int i = 0; i < 3; i++) {
            if (pos >= count) {
                return PARSE_NEED_MORE;
            }
            int b = pending[pos++];
            if (b < 0x80) {
                if (!isValidValue(i, b)) {
                    return PARSE_INVALID;
                }
                utf8Values[i] = b;
            } else if ((b >= 0xC2) && (b <= 0xDF)) {
                if (pos >= count) {
                    return PARSE_NEED_MORE;
                }
                int c = pending[pos++];
                if ((c < 0x80) || (c > 0xBF)) {
                    return PARSE_INVALID;
                }
                utf8Values[i] = ((b & 0x1F) << 6) | (c & 0x3F);
            } else {
                // Stray continuation, overlong lead, or a lead for a value
                // beyond the 1005 limit of 2047.
                return PARSE_INVALID;
            }
        }
        utf8Consumed = pos;
        return PARSE_COMPLETE;
    }

    /**
     * Accept the X10 interpretation.
     *
     * @return COMPLETE
     */
    private int acceptX10() {
        values[0] = pending[0];
        values[1] = pending[1];
        values[2] = pending[2];
        consumed = 3;
        return COMPLETE;
    }

    /**
     * Accept the UTF-8 interpretation.
     *
     * @return COMPLETE
     */
    private int acceptUtf8() {
        values[0] = utf8Values[0];
        values[1] = utf8Values[1];
        values[2] = utf8Values[2];
        consumed = utf8Consumed;
        return COMPLETE;
    }

    /**
     * Give up on the collected bytes.
     *
     * @return ABORT
     */
    private int abort() {
        consumed = 0;
        return ABORT;
    }
}
