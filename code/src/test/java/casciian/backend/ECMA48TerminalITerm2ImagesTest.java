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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casciian.bits.ArrayImageRGB;
import casciian.bits.Cell;
import casciian.bits.ImageRGB;
import casciian.image.encoders.BmpImageEncoderTest;
import casciian.image.encoders.ImageEncoder;
import casciian.image.encoders.ImageEncoderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that ECMA48Terminal draws image cells with the iTerm2 inline images
 * protocol when the terminal is iTerm2.
 */
@DisplayName("ECMA48Terminal iTerm2 inline images protocol")
class ECMA48TerminalITerm2ImagesTest {

    private static final long TIMEOUT_MILLIS = 5000;

    /** XTVERSION reply identifying iTerm2. */
    private static final String ITERM2 = "\033P>|iTerm2 3.5.0\033\\";

    /** A kitty reply to the support query, then Device Attributes. */
    private static final String KITTY_REPLY = "\033_Gi=31;OK\033\\\033[?62;4c";

    private static final Pattern INLINE_IMAGE = Pattern.compile(
        "\033\\]1337;File=([^:]*):([A-Za-z0-9+/=]*)\007");

    private record InlineImage(Map<String, String> keys, byte[] data) {
    }

    private ECMA48Terminal terminal;
    private ByteArrayOutputStream outputStream;
    private final List<ImageEncoder> registered = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearProperties();
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() {
        if (terminal != null) {
            terminal.closeTerminal();
            terminal = null;
        }
        registered.forEach(ImageEncoderRegistry.getInstance()::unregisterEncoder);
        clearProperties();
    }

    @Test
    @DisplayName("iTerm2 image runs are sent as one stretched 24-bit BMP")
    void iterm2RunIsInlineBmp() {
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        assertThat(terminal.isImageProtocolSupported()).isTrue();
        terminal.flushPhysical();

        terminal.putCharXY(2, 1, solidImageCell(1, 0xFF102030));
        terminal.putCharXY(3, 1, solidImageCell(2, 0xFFA0B0C0));
        outputStream.reset();
        terminal.flushPhysical();

        String written = written();
        List<InlineImage> images = parse(written);
        assertThat(images).singleElement().satisfies(image -> {
            assertThat(image.keys()).containsEntry("inline", "1")
                .containsEntry("width", "2").containsEntry("height", "1")
                .containsEntry("preserveAspectRatio", "0")
                .containsEntry("doNotMoveCursor", "1")
                .containsEntry("size", String.valueOf(image.data().length));
            BmpImageEncoderTest.Bmp bmp = BmpImageEncoderTest.decode(
                image.data());
            assertThat(bmp.bitsPerPixel()).isEqualTo(24);
            assertThat(bmp.width()).isEqualTo(16);
            assertThat(bmp.height()).isEqualTo(16);
            assertThat(bmp.pixels()[5][3]).isEqualTo(0xFF102030);
            assertThat(bmp.pixels()[5][12]).isEqualTo(0xFFA0B0C0);
        });
        assertThat(written).contains("\033[02;03H\033]1337;File=");
    }

    @Test
    @DisplayName("iTerm2 inline images take priority over kitty graphics")
    void iterm2HasPriorityOverKitty() {
        terminal = createTerminal(ITERM2 + KITTY_REPLY);
        waitForITerm2();
        waitFor(() -> terminal.getTerminalVersion().contains("iTerm2"));
        terminal.flushPhysical();

        terminal.putCharXY(0, 0, solidImageCell(7, 0xFF102030));
        outputStream.reset();
        terminal.flushPhysical();

        assertThat(terminal.hasKittyImages()).isFalse();
        assertThat(written()).doesNotContain("\033_G");
        assertThat(parse(written())).hasSize(1);
    }

    @Test
    @DisplayName("Turning iTerm2 images off falls back to kitty graphics")
    void disablingFallsBackToKitty() {
        System.setProperty("casciian.ECMA48.iterm2Images", "false");
        terminal = createTerminal(ITERM2 + KITTY_REPLY);
        waitFor(() -> terminal.hasKittyImages());
        waitFor(() -> terminal.getTerminalVersion().contains("iTerm2"));
        assertThat(terminal.hasITerm2Images()).isFalse();
        terminal.flushPhysical();

        terminal.putCharXY(0, 0, solidImageCell(7, 0xFF102030));
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(written()).contains("\033_G");
        assertThat(parse(written())).isEmpty();

        terminal.setHasITerm2Images(true);
        assertThat(terminal.hasITerm2Images()).isTrue();
        assertThat(terminal.hasKittyImages()).isFalse();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).hasSize(1);
    }

    @Test
    @DisplayName("Other terminals do not get iTerm2 inline images")
    void otherTerminalsAreNotDetected() {
        terminal = createTerminal("\033P>|XTerm(390)\033\\\033[?62;4c");
        waitFor(() -> terminal.getTerminalVersion().contains("XTerm"));

        terminal.putCharXY(0, 0, solidImageCell(7, 0xFF102030));
        terminal.flushPhysical();

        assertThat(terminal.hasITerm2Images()).isFalse();
        assertThat(parse(written())).isEmpty();
    }

    @Test
    @DisplayName("RGBA sends a 32-bit BMP over the cell background color")
    void rgbaKeepsAlphaAndPaintsBackground() {
        System.setProperty("casciian.ECMA48.iterm2ImageFormat", "rgba");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        Cell cell = solidImageCell(3, 0x40102030);
        cell.setBackColorRGB(0xABCDEF);
        terminal.putCharXY(1, 1, cell);
        outputStream.reset();
        terminal.flushPhysical();

        String written = written();
        assertThat(parse(written)).singleElement().satisfies(image -> {
            BmpImageEncoderTest.Bmp bmp = BmpImageEncoderTest.decode(
                image.data());
            assertThat(bmp.bitsPerPixel()).isEqualTo(32);
            assertThat(bmp.pixels()[0][0]).isEqualTo(0x40102030);
        });
        assertThat(written).contains("\033[0m\033[48;2;171;205;239m\033]1337;");
    }

    @Test
    @DisplayName("Add-on encoders are preferred unless one is configured")
    void addOnEncoderIsPreferred() {
        ImageEncoder png = new ImageEncoder() {
            @Override
            public String getName() {
                return "png";
            }

            @Override
            public String getMimeType() {
                return "image/png";
            }

            @Override
            public int getPriority() {
                return 10;
            }

            @Override
            public byte[] encode(final ImageRGB image, final boolean alpha) {
                return ("PNG" + image.getWidth()).getBytes(
                    StandardCharsets.US_ASCII);
            }
        };
        ImageEncoderRegistry.getInstance().registerEncoder(png);
        registered.add(png);

        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();
        terminal.putCharXY(0, 0, solidImageCell(5, 0xFF102030));
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).singleElement().satisfies(image ->
            assertThat(new String(image.data(), StandardCharsets.US_ASCII))
                .isEqualTo("PNG8"));

        terminal.closeTerminal();
        System.setProperty("casciian.ECMA48.iterm2ImageEncoder", "bmp");
        outputStream = new ByteArrayOutputStream();
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();
        terminal.putCharXY(0, 0, solidImageCell(5, 0xFF102030));
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).singleElement().satisfies(image ->
            assertThat(BmpImageEncoderTest.decode(image.data()).width())
                .isEqualTo(8));
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static void clearProperties() {
        System.clearProperty("casciian.ECMA48.iterm2Images");
        System.clearProperty("casciian.ECMA48.iterm2ImageFormat");
        System.clearProperty("casciian.ECMA48.iterm2ImageEncoder");
    }

    private static List<InlineImage> parse(final String written) {
        List<InlineImage> images = new ArrayList<>();
        Matcher matcher = INLINE_IMAGE.matcher(written);
        while (matcher.find()) {
            Map<String, String> keys = new HashMap<>();
            for (String pair : matcher.group(1).split(";")) {
                String[] kv = pair.split("=", 2);
                keys.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
            images.add(new InlineImage(keys,
                Base64.getDecoder().decode(matcher.group(2))));
        }
        return images;
    }

    private ECMA48Terminal createTerminal(final String input) {
        try {
            return new ECMA48Terminal(Mockito.mock(Backend.class), null,
                new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)), outputStream);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private String written() {
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static Cell solidImageCell(final int imageId, final int argb) {
        ImageRGB image = new ArrayImageRGB(8, 16);
        image.fillRect(0, 0, 8, 16, argb);
        Cell cell = new Cell();
        cell.setImage(image, imageId);
        return cell;
    }

    private void waitForITerm2() {
        waitFor(() -> terminal.hasITerm2Images());
    }

    private static void waitFor(final java.util.function.BooleanSupplier ok) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!ok.getAsBoolean()) {
            assertThat(System.currentTimeMillis()).as("condition")
                .isLessThan(deadline);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
