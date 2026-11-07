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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the kitty graphics protocol encoding: pixel formats, zlib
 * compression, chunking, and the support query.
 */
@DisplayName("KittyImageEncoder")
class KittyImageEncoderTest {

    private static final Pattern APC =
        Pattern.compile("\033_G([^;\033]*)(?:;([^\033]*))?\033\\\\");

    /**
     * One parsed APC graphics command.
     */
    record Command(Map<String, String> keys, String payload) {
    }

    @Test
    @DisplayName("RGB transmits 3 bytes per pixel and drops alpha")
    void rgbPixels() throws Exception {
        ImageRGB image = image(2, 1, 0x80112233, 0xFF445566);
        KittyImageEncoder encoder = new KittyImageEncoder(
            KittyImageEncoder.Format.RGB, false);

        List<Command> commands = parse(encoder.transmitAndPlace(image, 7, 3,
            1, 1));

        assertThat(commands).hasSize(1);
        Map<String, String> keys = commands.getFirst().keys();
        assertThat(keys).containsEntry("a", "T").containsEntry("f", "24")
            .containsEntry("s", "2").containsEntry("v", "1")
            .containsEntry("i", "7").containsEntry("p", "3")
            .containsEntry("c", "1").containsEntry("r", "1")
            .containsEntry("C", "1").containsEntry("q", "2")
            .doesNotContainKey("o");
        assertThat(payload(commands, false)).containsExactly(
            0x11, 0x22, 0x33, 0x44, 0x55, 0x66);
    }

    @Test
    @DisplayName("RGBA transmits 4 bytes per pixel including alpha")
    void rgbaPixels() throws Exception {
        ImageRGB image = image(2, 1, 0x80112233, 0x00445566);
        KittyImageEncoder encoder = new KittyImageEncoder(
            KittyImageEncoder.Format.RGBA, false);

        List<Command> commands = parse(encoder.transmitAndPlace(image, 7, 3,
            1, 1));

        assertThat(commands.getFirst().keys()).containsEntry("f", "32");
        assertThat(payload(commands, false)).containsExactly(
            0x11, 0x22, 0x33, 0x80, 0x44, 0x55, 0x66, 0x00);
    }

    @Test
    @DisplayName("zlib compression round-trips and shrinks uniform images")
    void zlibCompression() throws Exception {
        ImageRGB image = new ArrayImageRGB(64, 64);
        image.fillRect(0, 0, 64, 64, 0xFF336699);
        KittyImageEncoder encoder = new KittyImageEncoder(
            KittyImageEncoder.Format.RGBA, true);

        String sequence = encoder.transmitAndPlace(image, 1, 1, 1, 1);
        List<Command> commands = parse(sequence);

        assertThat(commands.getFirst().keys()).containsEntry("o", "z");
        int[] pixels = payload(commands, true);
        assertThat(pixels).hasSize(64 * 64 * 4);
        assertThat(pixels).startsWith(0x33, 0x66, 0x99, 0xFF);
        assertThat(sequence.length()).isLessThan(64 * 64 * 4);
    }

    @Test
    @DisplayName("Large payloads are split into chunks of at most 4096 bytes")
    void chunking() throws Exception {
        // Noise does not compress, so this needs several chunks.
        ImageRGB image = new ArrayImageRGB(40, 40);
        java.util.Random random = new java.util.Random(42);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                image.setRGB(x, y, 0xFF000000 | random.nextInt(0x1000000));
            }
        }
        KittyImageEncoder encoder = new KittyImageEncoder(
            KittyImageEncoder.Format.RGB, true);

        List<Command> commands = parse(encoder.transmitAndPlace(image,
            0x80000001, 5, 1, 1));

        assertThat(commands.size()).isGreaterThan(1);
        assertThat(commands.getFirst().keys()).containsEntry("a", "T")
            .containsEntry("i", "2147483649").containsEntry("m", "1");
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            assertThat(command.payload().length()).isLessThanOrEqualTo(
                KittyImageEncoder.CHUNK_SIZE);
            if (i > 0) {
                assertThat(command.keys()).doesNotContainKeys("a", "i", "f");
            }
            if (i < commands.size() - 1) {
                assertThat(command.payload().length() % 4).isZero();
                assertThat(command.keys()).containsEntry("m", "1");
            } else {
                assertThat(command.keys()).containsEntry("m", "0");
            }
        }
        int[] pixels = payload(commands, true);
        assertThat(pixels).hasSize(40 * 40 * 3);
        int first = image.getRGB(0, 0);
        assertThat(pixels).startsWith((first >>> 16) & 0xFF,
            (first >>> 8) & 0xFF, first & 0xFF);
    }

    @Test
    @DisplayName("Placement and deletion commands use unsigned ids")
    void placeAndDelete() {
        assertThat(parse(KittyImageEncoder.place(-1, 9, 1, 1)).getFirst()
            .keys()).containsEntry("a", "p").containsEntry("i", "4294967295")
            .containsEntry("p", "9").containsEntry("C", "1");
        assertThat(parse(KittyImageEncoder.deletePlacement(5, 9)).getFirst()
            .keys()).containsEntry("a", "d").containsEntry("d", "i")
            .containsEntry("i", "5").containsEntry("p", "9");
        assertThat(parse(KittyImageEncoder.deleteImage(5)).getFirst()
            .keys()).containsEntry("a", "d").containsEntry("d", "I")
            .containsEntry("i", "5").doesNotContainKey("p");
    }

    @Test
    @DisplayName("Only an OK reply for the query id counts as support")
    void queryReply() {
        assertThat(parse(KittyImageEncoder.query()).getFirst().keys())
            .containsEntry("a", "q").containsEntry("i", "31");
        assertThat(KittyImageEncoder.isQueryOk("Gi=31;OK")).isTrue();
        assertThat(KittyImageEncoder.isQueryOk("Gi=31,p=0;OK")).isTrue();
        assertThat(KittyImageEncoder.isQueryOk("Gi=31;EINVAL:bad")).isFalse();
        assertThat(KittyImageEncoder.isQueryOk("Gi=32;OK")).isFalse();
        assertThat(KittyImageEncoder.isQueryOk("Gi=310;OK")).isFalse();
        assertThat(KittyImageEncoder.isQueryOk("X")).isFalse();
    }

    @Test
    @DisplayName("Format names parse case-insensitively with a fallback")
    void formatParsing() {
        assertThat(KittyImageEncoder.Format.parse("RGBA",
            KittyImageEncoder.Format.RGB)).isEqualTo(
                KittyImageEncoder.Format.RGBA);
        assertThat(KittyImageEncoder.Format.parse("png",
            KittyImageEncoder.Format.RGB)).isEqualTo(
                KittyImageEncoder.Format.RGB);
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    static ImageRGB image(final int width, final int height,
        final int... argb) {

        ImageRGB image = new ArrayImageRGB(width, height);
        for (int i = 0; i < argb.length; i++) {
            image.setRGB(i % width, i / width, argb[i]);
        }
        return image;
    }

    /**
     * Parse every APC graphics command in a string.
     */
    static List<Command> parse(final String sequence) {
        List<Command> commands = new ArrayList<>();
        Matcher matcher = APC.matcher(sequence);
        while (matcher.find()) {
            Map<String, String> keys = new LinkedHashMap<>();
            for (String pair : matcher.group(1).split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    keys.put(pair.substring(0, eq), pair.substring(eq + 1));
                }
            }
            String payload = matcher.group(2) == null ? "" : matcher.group(2);
            commands.add(new Command(keys, payload));
        }
        return commands;
    }

    /**
     * Join, decode, and optionally inflate a transmission's payload.
     */
    private static int[] payload(final List<Command> commands,
        final boolean compressed) throws Exception {

        StringBuilder base64 = new StringBuilder();
        for (Command command : commands) {
            base64.append(command.payload());
        }
        byte[] bytes = Base64.getDecoder().decode(base64.toString());
        if (compressed) {
            Inflater inflater = new Inflater();
            inflater.setInput(bytes);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                out.write(buffer, 0, inflater.inflate(buffer));
            }
            inflater.end();
            bytes = out.toByteArray();
        }
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i] & 0xFF;
        }
        return result;
    }
}
