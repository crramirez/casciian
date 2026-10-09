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

import casciian.backend.LegacyMouseReportDecoder.Encoding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static casciian.backend.LegacyMouseReportDecoder.ABORT;
import static casciian.backend.LegacyMouseReportDecoder.COMPLETE;
import static casciian.backend.LegacyMouseReportDecoder.NEED_MORE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for decoding the bytes after ESC [ M in X10 and 1005 encodings.
 */
@DisplayName("LegacyMouseReportDecoder Tests")
class LegacyMouseReportDecoderTest {

    /**
     * Feed bytes, returning the last result.
     */
    private static int feed(final LegacyMouseReportDecoder decoder,
                            final int... bytes) {
        int result = NEED_MORE;
        for (int b : bytes) {
            result = decoder.add(b);
        }
        return result;
    }

    /**
     * Encode one 1005 value as UTF-8 bytes.
     */
    private static int[] utf8Value(final int value) {
        if (value < 0x80) {
            return new int[] {value};
        }
        return new int[] {0xC0 | (value >> 6), 0x80 | (value & 0x3F)};
    }

    private static int[] concat(final int[]... parts) {
        int n = 0;
        for (int[] p : parts) {
            n += p.length;
        }
        int[] result = new int[n];
        int i = 0;
        for (int[] p : parts) {
            System.arraycopy(p, 0, result, i, p.length);
            i += p.length;
        }
        return result;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 95, 96, 127, 128, 160, 200, 223})
    @DisplayName("X10 reports decode every position 1-223 on both axes")
    void x10AllPositions(final int position) {
        for (Encoding encoding : new Encoding[] {Encoding.X10, Encoding.UNKNOWN}) {
            // Position on X, row 1.
            LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
            decoder.setEncoding(encoding);
            int result = feed(decoder, 32, position + 32, 33);
            if (result == NEED_MORE) {
                result = decoder.timeout();
            }
            assertEquals(COMPLETE, result, "x=" + position + " " + encoding);
            assertEquals(32, decoder.getButtons());
            assertEquals(position + 32, decoder.getX());
            assertEquals(33, decoder.getY());
            assertArrayEquals(new int[0], decoder.takeLeftovers());

            // Position on Y, column 1.
            decoder = new LegacyMouseReportDecoder();
            decoder.setEncoding(encoding);
            result = feed(decoder, 32, 33, position + 32);
            if (result == NEED_MORE) {
                result = decoder.timeout();
            }
            assertEquals(COMPLETE, result, "y=" + position + " " + encoding);
            assertEquals(33, decoder.getX());
            assertEquals(position + 32, decoder.getY());
            assertArrayEquals(new int[0], decoder.takeLeftovers());
        }
    }

    @Test
    @DisplayName("Invalid UTF-8 coordinate bytes prove X10 and lock the encoding")
    void invalidUtf8LocksX10() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        // 0x80 = position 96: a stray continuation byte in UTF-8.
        assertEquals(COMPLETE, feed(decoder, 32, 0x80, 0xFF));
        assertEquals(0x80, decoder.getX());
        assertEquals(0xFF, decoder.getY());
        decoder.takeLeftovers();
        assertEquals(Encoding.X10, decoder.getEncoding());

        // Now an otherwise ambiguous report completes immediately as X10.
        assertEquals(COMPLETE, feed(decoder, 32, 0xC3, 0xA0));
        assertEquals(0xC3, decoder.getX());
        assertEquals(0xA0, decoder.getY());
    }

    @Test
    @DisplayName("Lead byte followed by ESC resolves the previous report as X10")
    void x10FollowedByNextReport() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        assertEquals(NEED_MORE, feed(decoder, 32, 0x41, 0xC3));
        assertEquals(COMPLETE, decoder.add(0x1B));
        assertEquals(0x41, decoder.getX());
        assertEquals(0xC3, decoder.getY());
        assertArrayEquals(new int[] {0x1B}, decoder.takeLeftovers());
        assertEquals(Encoding.X10, decoder.getEncoding());
    }

    @Test
    @DisplayName("Ambiguous X10 report with no more input completes on timeout")
    void ambiguousX10ResolvedByTimeout() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        assertEquals(NEED_MORE, feed(decoder, 32, 0xC3, 0xA0));
        assertEquals(COMPLETE, decoder.timeout());
        assertEquals(0xC3, decoder.getX());
        assertEquals(0xA0, decoder.getY());
        // Not proven, so the encoding stays unknown.
        assertEquals(Encoding.UNKNOWN, decoder.getEncoding());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 95, 96, 127, 128, 223, 224, 500, 2015})
    @DisplayName("1005 reports decode multibyte coordinates on both axes")
    void utf8Positions(final int position) {
        for (Encoding encoding : new Encoding[] {Encoding.UTF8, Encoding.UNKNOWN}) {
            LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
            decoder.setEncoding(encoding);
            int result = feed(decoder, concat(utf8Value(32),
                    utf8Value(position + 32), utf8Value(40 + 32)));
            assertEquals(COMPLETE, result, "x=" + position + " " + encoding);
            assertEquals(position + 32, decoder.getX());
            assertEquals(72, decoder.getY());
            assertArrayEquals(new int[0], decoder.takeLeftovers());

            decoder = new LegacyMouseReportDecoder();
            decoder.setEncoding(encoding);
            result = feed(decoder, concat(utf8Value(32), utf8Value(40 + 32),
                    utf8Value(position + 32)));
            assertEquals(COMPLETE, result, "y=" + position + " " + encoding);
            assertEquals(72, decoder.getX());
            assertEquals(position + 32, decoder.getY());
            assertArrayEquals(new int[0], decoder.takeLeftovers());
        }
    }

    @Test
    @DisplayName("Trailing continuation byte proves 1005 and locks the encoding")
    void continuationAfterX10LengthLocksUtf8() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        // Cy = 200 + 32 = 232 = C3 A8: the X10 reading would leave 0xA8.
        assertEquals(COMPLETE, feed(decoder, 32, 72, 0xC3, 0xA8));
        assertEquals(232, decoder.getY());
        decoder.takeLeftovers();
        assertEquals(Encoding.UTF8, decoder.getEncoding());
    }

    @Test
    @DisplayName("Known X10 encoding never consumes extra bytes")
    void knownX10DoesNotConsumeExtraBytes() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        decoder.setEncoding(Encoding.X10);
        assertEquals(COMPLETE, feed(decoder, 32, 0xC3, 0xA8));
        assertEquals(0xC3, decoder.getX());
        assertEquals(0xA8, decoder.getY());
    }

    @Test
    @DisplayName("NUL coordinate (out of range) is accepted")
    void nulCoordinate() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        assertEquals(COMPLETE, feed(decoder, 32, 0, 0x21));
        assertEquals(0, decoder.getX());
    }

    @Test
    @DisplayName("Control bytes abort the report and are handed back")
    void malformedReportAborts() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        assertEquals(ABORT, feed(decoder, 32, 0x1B));
        assertArrayEquals(new int[] {32, 0x1B}, decoder.takeLeftovers());

        decoder.setEncoding(Encoding.X10);
        assertEquals(ABORT, decoder.add(0x0D));
        assertArrayEquals(new int[] {0x0D}, decoder.takeLeftovers());
    }

    @Test
    @DisplayName("Coordinate 0x20 (position 0) aborts the report")
    void coordinateBelowPositionOneAborts() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        assertEquals(ABORT, feed(decoder, 32, 0x20));
        assertArrayEquals(new int[] {32, 0x20}, decoder.takeLeftovers());

        decoder.setEncoding(Encoding.X10);
        assertEquals(ABORT, feed(decoder, 32, 0x21, 0x20));
        assertArrayEquals(new int[] {32, 0x21, 0x20}, decoder.takeLeftovers());
    }

    @Test
    @DisplayName("Confirmed 1005 encoding aborts an incomplete report on timeout")
    void confirmedUtf8IncompleteAbortsOnTimeout() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        decoder.setEncoding(Encoding.UTF8);
        assertEquals(NEED_MORE, feed(decoder, 32, 0xC3, 0xA0));
        assertEquals(ABORT, decoder.timeout());
        assertArrayEquals(new int[] {32, 0xC3, 0xA0}, decoder.takeLeftovers());
        assertEquals(Encoding.UTF8, decoder.getEncoding());
    }

    @Test
    @DisplayName("Confirmed 1005 encoding aborts invalid UTF-8 instead of using X10")
    void confirmedUtf8InvalidAborts() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        decoder.setEncoding(Encoding.UTF8);
        assertEquals(ABORT, feed(decoder, 32, 0x80));
        assertArrayEquals(new int[] {32, 0x80}, decoder.takeLeftovers());
        assertEquals(Encoding.UTF8, decoder.getEncoding());
    }

    @Test
    @DisplayName("Incomplete report aborts on timeout and is handed back")
    void incompleteReportAbortsOnTimeout() {
        LegacyMouseReportDecoder decoder = new LegacyMouseReportDecoder();
        assertEquals(NEED_MORE, feed(decoder, 32, 0x41));
        assertEquals(ABORT, decoder.timeout());
        assertArrayEquals(new int[] {32, 0x41}, decoder.takeLeftovers());
    }
}
