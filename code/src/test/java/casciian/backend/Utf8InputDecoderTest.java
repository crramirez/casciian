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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the incremental UTF-8 input decoder.
 */
@DisplayName("Utf8InputDecoder Tests")
class Utf8InputDecoderTest {

    /**
     * Decode bytes the way ECMA48Terminal does, collecting code points.
     */
    private static List<Integer> decode(final Utf8InputDecoder decoder,
                                        final int... bytes) {
        List<Integer> result = new ArrayList<>();
        for (int b : bytes) {
            int cp = decoder.decode(b);
            if (cp == Utf8InputDecoder.MALFORMED_RETRY) {
                result.add(Utf8InputDecoder.REPLACEMENT);
                cp = decoder.decode(b);
            }
            if (cp >= 0) {
                result.add(cp);
            }
        }
        return result;
    }

    private static int[] utf8(final String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i] & 0xFF;
        }
        return result;
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "é", "ñandú", "漢字", "😀", "a😀b€c"})
    @DisplayName("Decodes valid UTF-8 to the same code points as Java")
    void decodesValidUtf8(final String text) {
        List<Integer> expected = new ArrayList<>();
        text.codePoints().forEach(expected::add);
        assertEquals(expected, decode(new Utf8InputDecoder(), utf8(text)));
    }

    @Test
    @DisplayName("Keeps state across calls for sequences split between reads")
    void splitSequenceAcrossCalls() {
        Utf8InputDecoder decoder = new Utf8InputDecoder();
        int[] emoji = utf8("😀");
        assertEquals(List.of(), decode(decoder, emoji[0], emoji[1]));
        assertTrue(decoder.hasPending());
        assertEquals(List.of(0x1F600), decode(decoder, emoji[2], emoji[3]));
        assertFalse(decoder.hasPending());
    }

    @Test
    @DisplayName("Stray continuation and invalid bytes become U+FFFD")
    void invalidBytesBecomeReplacement() {
        assertEquals(List.of(0xFFFD, 0xFFFD, 0xFFFD, (int) 'a'),
            decode(new Utf8InputDecoder(), 0x80, 0xC0, 0xFF, 'a'));
    }

    @Test
    @DisplayName("Interrupted sequence becomes U+FFFD and the interrupting byte is kept")
    void interruptedSequenceKeepsNextByte() {
        assertEquals(List.of(0xFFFD, 0x1B, (int) '['),
            decode(new Utf8InputDecoder(), 0xE6, 0xBC, 0x1B, '['));
        assertEquals(List.of(0xFFFD, (int) 'é'),
            decode(new Utf8InputDecoder(), 0xC3, 0xC3, 0xA9));
    }

    @Test
    @DisplayName("Overlong forms, surrogates and values above U+10FFFF are rejected")
    void rejectsNonShortestAndOutOfRange() {
        assertEquals(List.of(0xFFFD, 0xFFFD, 0xFFFD),
            decode(new Utf8InputDecoder(), 0xE0, 0x80, 0x80));
        assertEquals(List.of(0xFFFD, 0xFFFD, 0xFFFD),
            decode(new Utf8InputDecoder(), 0xED, 0xA0, 0x80));
        assertEquals(List.of(0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD),
            decode(new Utf8InputDecoder(), 0xF4, 0x90, 0x80, 0x80));
    }
}
