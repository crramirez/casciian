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
 * Incremental, allocation-free UTF-8 decoder for terminal input, fed one
 * byte at a time.
 *
 * <p>Terminal input is decoded byte by byte (rather than through a
 * {@link java.io.Reader}) so that the input parser can switch to raw byte
 * interpretation in the middle of a buffer, which is required for legacy X10
 * mouse reports.  The decoder keeps its partial state between calls, so
 * multibyte sequences split across reads are handled transparently.
 *
 * <p>Malformed input is replaced with U+FFFD following the "maximal subpart"
 * practice used by {@link java.nio.charset.CharsetDecoder}: overlong forms,
 * surrogate code points and values above U+10FFFF are rejected, and a byte
 * that interrupts an incomplete sequence is reported via
 * {@link #MALFORMED_RETRY} so the caller can emit U+FFFD and then feed that
 * byte again.
 */
final class Utf8InputDecoder {

    /**
     * Returned by {@link #decode(int)} when more bytes are needed to complete
     * the current code point.
     */
    static final int NEED_MORE = -1;

    /**
     * Returned by {@link #decode(int)} when the byte just fed terminated an
     * incomplete sequence.  The caller should emit U+FFFD for the incomplete
     * sequence and then call {@link #decode(int)} again with the same byte,
     * which is guaranteed not to return MALFORMED_RETRY a second time.
     */
    static final int MALFORMED_RETRY = -2;

    /**
     * The Unicode replacement character.
     */
    static final int REPLACEMENT = 0xFFFD;

    /**
     * The number of continuation bytes still expected.
     */
    private int needed = 0;

    /**
     * The code point accumulated so far.
     */
    private int codePoint = 0;

    /**
     * The smallest acceptable value of the next continuation byte.
     */
    private int lower = 0x80;

    /**
     * The largest acceptable value of the next continuation byte.
     */
    private int upper = 0xBF;

    /**
     * Feed one byte.
     *
     * @param b the byte, as an unsigned value 0-255
     * @return a complete code point (U+FFFD for an invalid byte), or
     * {@link #NEED_MORE}, or {@link #MALFORMED_RETRY}
     */
    int decode(final int b) {
        if (needed == 0) {
            if (b < 0x80) {
                return b;
            }
            if ((b >= 0xC2) && (b <= 0xDF)) {
                needed = 1;
                codePoint = b & 0x1F;
            } else if ((b >= 0xE0) && (b <= 0xEF)) {
                if (b == 0xE0) {
                    // Reject overlong forms.
                    lower = 0xA0;
                } else if (b == 0xED) {
                    // Reject UTF-16 surrogates.
                    upper = 0x9F;
                }
                needed = 2;
                codePoint = b & 0x0F;
            } else if ((b >= 0xF0) && (b <= 0xF4)) {
                if (b == 0xF0) {
                    // Reject overlong forms.
                    lower = 0x90;
                } else if (b == 0xF4) {
                    // Reject code points above U+10FFFF.
                    upper = 0x8F;
                }
                needed = 3;
                codePoint = b & 0x07;
            } else {
                // Stray continuation byte, or a byte that never appears in
                // UTF-8 (0xC0, 0xC1, 0xF5-0xFF).
                return REPLACEMENT;
            }
            return NEED_MORE;
        }

        if ((b < lower) || (b > upper)) {
            reset();
            return MALFORMED_RETRY;
        }
        lower = 0x80;
        upper = 0xBF;
        codePoint = (codePoint << 6) | (b & 0x3F);
        needed--;
        if (needed == 0) {
            int result = codePoint;
            codePoint = 0;
            return result;
        }
        return NEED_MORE;
    }

    /**
     * Check whether an incomplete sequence is pending.
     *
     * @return true if at least one byte of an incomplete sequence was fed
     */
    boolean hasPending() {
        return needed != 0;
    }

    /**
     * Discard any incomplete sequence.
     */
    void reset() {
        needed = 0;
        codePoint = 0;
        lower = 0x80;
        upper = 0xBF;
    }
}
