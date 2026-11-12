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
import java.io.IOException;
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
import casciian.bits.Color;
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

    /** XTVERSION reply identifying an iTerm2 release before 3.5. */
    private static final String ITERM2_3_4 = "\033P>|iTerm2 3.4.0\033\\";

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
    @DisplayName("Reloading options frees kitty placements when iTerm2 takes over")
    void reloadOptionsResetsKittyPlacements() {
        System.setProperty("casciian.ECMA48.iterm2Images", "false");
        terminal = createTerminal(ITERM2 + KITTY_REPLY);
        waitFor(() -> terminal.hasKittyImages());
        waitFor(() -> terminal.getTerminalVersion().contains("iTerm2"));
        terminal.flushPhysical();

        terminal.putCharXY(0, 0, solidImageCell(7, 0xFF102030));
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(written()).contains("\033_Ga=T");

        System.setProperty("casciian.ECMA48.iterm2Images", "true");
        outputStream.reset();
        terminal.reloadOptions();

        assertThat(terminal.hasKittyImages()).isFalse();
        assertThat(written()).contains("\033_Ga=d");
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
    @DisplayName("iTerm2 versions before 3.5 do not enable inline images")
    void olderITerm2DoesNotEnableInlineImages() {
        terminal = createTerminal(ITERM2_3_4 + "\033[?62;4c");
        waitFor(() -> terminal.getTerminalVersion().contains("iTerm2"));

        terminal.putCharXY(0, 0, solidImageCell(7, 0xFF102030));
        terminal.flushPhysical();

        assertThat(terminal.hasITerm2Images()).isFalse();
        assertThat(parse(written())).isEmpty();
    }

    @Test
    @DisplayName("RGBA images composite each tile over its own cell background")
    void rgbaCompositesTilesOverTheirCellBackgrounds() {
        System.setProperty("casciian.ECMA48.iterm2ImageFormat", "rgba");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        Cell left = solidImageCell(3, 0x00000000);
        left.setBackColorRGB(0xFF0000);
        Cell right = solidImageCell(4, 0x00000000);
        right.setBackColorRGB(0x0000FF);
        terminal.putCharXY(1, 1, left);
        terminal.putCharXY(2, 1, right);
        outputStream.reset();
        terminal.flushPhysical();

        String written = written();
        assertThat(parse(written)).singleElement().satisfies(image -> {
            BmpImageEncoderTest.Bmp bmp = BmpImageEncoderTest.decode(
                image.data());
            assertThat(bmp.bitsPerPixel()).isEqualTo(32);
            assertThat(bmp.pixels()[0][0]).isEqualTo(0xFFFF0000);
            assertThat(bmp.pixels()[0][8]).isEqualTo(0xFF0000FF);
        });
        assertThat(terminal.isDirty()).isFalse();

        Cell changedRight = solidImageCell(4, 0x00000000);
        changedRight.setBackColorRGB(0x00FF00);
        terminal.putCharXY(2, 1, changedRight);
        assertThat(terminal.isDirty()).isTrue();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).singleElement().satisfies(image ->
            assertThat(BmpImageEncoderTest.decode(image.data()).pixels()[0][0])
                .isEqualTo(0xFF00FF00));
    }

    @Test
    @DisplayName("Runs with incompatible tile dimensions are sent separately")
    void incompatibleTileDimensionsSplitIntoSeparateRuns() {
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        terminal.putCharXY(0, 0, imageCell(10, 1, 2, 0xFF102030));
        terminal.putCharXY(1, 0, imageCell(11, 3, 1, 0xFFA0B0C0));
        outputStream.reset();
        terminal.flushPhysical();

        List<InlineImage> images = parse(written());
        assertThat(images).hasSize(2);
        assertThat(BmpImageEncoderTest.decode(images.get(0).data()))
            .satisfies(bmp -> {
                assertThat(bmp.width()).isEqualTo(1);
                assertThat(bmp.height()).isEqualTo(2);
            });
        assertThat(BmpImageEncoderTest.decode(images.get(1).data()))
            .satisfies(bmp -> {
                assertThat(bmp.width()).isEqualTo(3);
                assertThat(bmp.height()).isEqualTo(1);
            });
    }

    @Test
    @DisplayName("iTerm2 cache keys distinguish image tile dimensions")
    void imageCacheSeparatesTilesWithDifferentDimensions() {
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        Cell horizontal = imageCellWithPixels(12, 2, 1,
            0xFFFF0000, 0xFF0000FF);
        terminal.putCharXY(0, 0, horizontal);
        outputStream.reset();
        terminal.flushPhysical();

        Cell vertical = imageCellWithPixels(12, 1, 2,
            0xFFFF0000, 0xFF0000FF);
        terminal.putCharXY(0, 0, vertical);
        outputStream.reset();
        terminal.flushPhysical();

        assertThat(parse(written())).singleElement().satisfies(image -> {
            BmpImageEncoderTest.Bmp bmp = BmpImageEncoderTest.decode(
                image.data());
            assertThat(bmp.width()).isEqualTo(1);
            assertThat(bmp.height()).isEqualTo(2);
            assertThat(bmp.pixels()[0][0]).isEqualTo(0xFFFF0000);
            assertThat(bmp.pixels()[1][0]).isEqualTo(0xFF0000FF);
        });
    }

    @Test
    @DisplayName("Same-ID replacements and mutations redraw away from the cursor")
    void sameIdImageChangesRedraw() {
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        terminal.putCharXY(3, 5, imageCell(12, 2, 1, 0xFF102030));
        terminal.flushPhysical();
        assertThat(terminal.isDirty()).isFalse();

        Cell replacement = imageCell(12, 2, 1, 0xFFA0B0C0);
        terminal.putCharXY(3, 5, replacement);
        assertThat(terminal.isDirty()).isTrue();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(BmpImageEncoderTest.decode(parse(written()).getFirst().data())
            .pixels()[0][0]).isEqualTo(0xFFA0B0C0);

        terminal.putCharXY(3, 5, imageCell(12, 1, 2, 0xFFA0B0C0));
        assertThat(terminal.isDirty()).isTrue();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(BmpImageEncoderTest.decode(parse(written()).getFirst().data())
            .height()).isEqualTo(2);

        Cell mutable = imageCell(12, 1, 2, 0xFF112233);
        terminal.putCharXY(3, 5, mutable);
        terminal.flushPhysical();
        mutable.getImage().setRGB(0, 0, 0xFF445566);
        assertThat(terminal.isDirty()).isTrue();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(BmpImageEncoderTest.decode(parse(written()).getFirst().data())
            .pixels()[0][0]).isEqualTo(0xFF445566);
        assertThat(terminal.isDirty()).isFalse();
    }

    @Test
    @DisplayName("Hash collisions cannot reuse a different cached image")
    void imageCacheVerifiesPixels() {
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        Cell first = imageCellWithPixels(0, 2, 1, 0xFF000000, 0xFFFF0000);
        Cell second = imageCellWithPixels(0, 2, 1, 0xFF080000, 0xFF070000);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        terminal.putCharXY(3, 5, first);
        terminal.flushPhysical();
        terminal.putCharXY(3, 7, second);
        outputStream.reset();
        terminal.flushPhysical();

        assertThat(parse(written())).singleElement().satisfies(image ->
            assertThat(BmpImageEncoderTest.decode(image.data()).pixels()[0])
                .containsExactly(0xFF080000, 0xFF070000));
    }

    @Test
    @DisplayName("iTerm2 cache is bounded by encoded payload size")
    void imageCacheEvictsPayloadsBySize() {
        int[] encodeCount = {0};
        byte[] encodedImage = new byte[4 * 1024 * 1024];
        ImageEncoder largeEncoder = new ImageEncoder() {
            @Override
            public String getName() {
                return "large";
            }

            @Override
            public String getMimeType() {
                return "image/large";
            }

            @Override
            public int getPriority() {
                return 100;
            }

            @Override
            public byte[] encode(final ImageRGB image, final boolean alpha) {
                encodeCount[0]++;
                return encodedImage;
            }
        };
        ImageEncoderRegistry.getInstance().registerEncoder(largeEncoder);
        registered.add(largeEncoder);
        System.setProperty("casciian.ECMA48.iterm2ImageEncoder", "large");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        Cell first = solidImageCell(31, 0xFF102030);
        Cell second = solidImageCell(32, 0xFFA0B0C0);
        terminal.putCharXY(0, 0, first);
        terminal.flushPhysical();
        outputStream.reset();
        terminal.putCharXY(0, 0, second);
        terminal.flushPhysical();
        outputStream.reset();
        terminal.putCharXY(0, 0, first);
        terminal.flushPhysical();

        assertThat(encodeCount[0]).isEqualTo(3);
    }

    @Test
    @DisplayName("iTerm2 cache includes pixel snapshots in its memory budget")
    void imageCacheBoundsPixelSnapshots() {
        int[] encodeCount = {0};
        ImageEncoder encoder = new ImageEncoder() {
            @Override
            public String getName() {
                return "small-payload";
            }

            @Override
            public String getMimeType() {
                return "image/small-payload";
            }

            @Override
            public int getPriority() {
                return 100;
            }

            @Override
            public byte[] encode(final ImageRGB image, final boolean alpha) {
                encodeCount[0]++;
                return new byte[] {1};
            }
        };
        ImageEncoderRegistry.getInstance().registerEncoder(encoder);
        registered.add(encoder);
        System.setProperty("casciian.ECMA48.iterm2ImageEncoder", "small-payload");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();

        Cell large = imageCell(42, 1500, 1500, 0xFF102030);
        terminal.putCharXY(3, 5, large);
        terminal.flushPhysical();
        terminal.putCharXY(3, 7, large);
        terminal.flushPhysical();

        assertThat(encodeCount[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("OSC system color updates invalidate cached RGBA images")
    void oscColorUpdateInvalidatesCachedImages() {
        System.setProperty("casciian.ECMA48.iterm2ImageFormat", "rgba");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();
        Cell originalPaletteBlack = new Cell();
        originalPaletteBlack.setBackColor(Color.BLACK);
        int originalBlack = ECMA48Terminal.attrToBackgroundColor(
            originalPaletteBlack);
        terminal.oscResponse("4;0;rgb:0000/0000/0000");

        Cell cell = solidImageCell(33, 0x80000000);
        cell.setBackColor(Color.BLACK);
        terminal.putCharXY(0, 0, cell);
        outputStream.reset();
        terminal.flushPhysical();
        int before = BmpImageEncoderTest.decode(parse(written()).getFirst()
            .data()).pixels()[0][0];

        terminal.oscResponse("4;0;rgb:ffff/0000/0000");
        outputStream.reset();
        terminal.flushPhysical();

        assertThat(parse(written())).singleElement().satisfies(image ->
            assertThat(BmpImageEncoderTest.decode(image.data()).pixels()[0][0])
                .isNotEqualTo(before));
        terminal.oscResponse("4;0;rgb:%02x/%02x/%02x".formatted(
            (originalBlack >>> 16) & 0xFF, (originalBlack >>> 8) & 0xFF,
            originalBlack & 0xFF));
    }

    @Test
    @DisplayName("Reloading custom palette colors invalidates RGBA images")
    void customPaletteUpdateInvalidatesCachedImages() {
        System.setProperty("casciian.ECMA48.iterm2ImageFormat", "rgba");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        Cell black = new Cell();
        black.setBackColor(Color.BLACK);
        int originalBlack = ECMA48Terminal.attrToBackgroundColor(black);
        try {
            System.setProperty("casciian.ECMA48.color0", "000000");
            terminal.reloadOptions();
            Cell cell = solidImageCell(33, 0x80000000);
            cell.setBackColor(Color.BLACK);
            terminal.putCharXY(3, 5, cell);
            outputStream.reset();
            terminal.flushPhysical();
            int before = BmpImageEncoderTest.decode(parse(written()).getFirst()
                .data()).pixels()[0][0];

            System.setProperty("casciian.ECMA48.color0", "ff0000");
            terminal.reloadOptions();
            assertThat(terminal.isDirty()).isTrue();
            outputStream.reset();
            terminal.flushPhysical();
            assertThat(parse(written())).singleElement().satisfies(image ->
                assertThat(BmpImageEncoderTest.decode(image.data()).pixels()[0][0])
                    .isNotEqualTo(before));
        } finally {
            System.clearProperty("casciian.ECMA48.color0");
            terminal.oscResponse("4;0;rgb:%02x/%02x/%02x".formatted(
                (originalBlack >>> 16) & 0xFF, (originalBlack >>> 8) & 0xFF,
                originalBlack & 0xFF));
        }
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

    @Test
    @DisplayName("Reloading options installs the selected encoder instance")
    void reloadOptionsReplacesConfiguredEncoderInstance() {
        ImageEncoder first = new NamedEncoder("first", false);
        ImageEncoder second = new NamedEncoder("second", false);
        ImageEncoderRegistry.getInstance().registerEncoder(first);
        ImageEncoderRegistry.getInstance().registerEncoder(second);
        registered.add(first);
        registered.add(second);

        System.setProperty("casciian.ECMA48.iterm2ImageEncoder", "first");
        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();
        terminal.putCharXY(0, 0, solidImageCell(5, 0xFF102030));
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(new String(parse(written()).getFirst().data(),
            StandardCharsets.US_ASCII)).isEqualTo("first");

        System.setProperty("casciian.ECMA48.iterm2ImageEncoder", "second");
        outputStream.reset();
        terminal.reloadOptions();
        terminal.flushPhysical();
        assertThat(new String(parse(written()).getFirst().data(),
            StandardCharsets.US_ASCII)).isEqualTo("second");
    }

    @Test
    @DisplayName("BMP fallback uses the selected encoder's alpha behavior")
    void fallbackPreservesOpaqueEncoderBehavior() {
        System.setProperty("casciian.ECMA48.iterm2ImageFormat", "rgba");
        ImageEncoder failing = new ImageEncoder() {
            @Override
            public String getName() {
                return "failing";
            }

            @Override
            public String getMimeType() {
                return "image/failing";
            }

            @Override
            public int getPriority() {
                return 100;
            }

            @Override
            public byte[] encode(final ImageRGB image, final boolean alpha)
                throws IOException {
                throw new IOException("expected test failure");
            }
        };
        ImageEncoderRegistry.getInstance().registerEncoder(failing);
        registered.add(failing);
        System.setProperty("casciian.ECMA48.iterm2ImageEncoder", "failing");

        terminal = createTerminal(ITERM2 + "\033[?62;4c");
        waitForITerm2();
        terminal.flushPhysical();
        terminal.putCharXY(0, 0, solidImageCell(6, 0x40102030));
        outputStream.reset();
        terminal.flushPhysical();

        assertThat(parse(written())).singleElement().satisfies(image -> {
            BmpImageEncoderTest.Bmp bmp = BmpImageEncoderTest.decode(
                image.data());
            assertThat(bmp.bitsPerPixel()).isEqualTo(24);
            assertThat(bmp.pixels()[0][0]).isEqualTo(0xFF102030);
        });
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
        return imageCell(imageId, 8, 16, argb);
    }

    private static Cell imageCell(final int imageId, final int width,
        final int height, final int argb) {
        ImageRGB image = new ArrayImageRGB(width, height);
        image.fillRect(0, 0, width, height, argb);
        Cell cell = new Cell();
        cell.setImage(image, imageId);
        return cell;
    }

    private static Cell imageCellWithPixels(final int imageId,
        final int width, final int height, final int... pixels) {
        ImageRGB image = new ArrayImageRGB(width, height);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Cell cell = new Cell();
        if (imageId == 0) {
            cell.setImage(image);
        } else {
            cell.setImage(image, imageId);
        }
        return cell;
    }

    private static final class NamedEncoder implements ImageEncoder {
        private final String name;
        private final boolean alpha;

        private NamedEncoder(final String name, final boolean alpha) {
            this.name = name;
            this.alpha = alpha;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getMimeType() {
            return "image/" + name;
        }

        @Override
        public int getPriority() {
            return 10;
        }

        @Override
        public boolean supportsAlpha() {
            return alpha;
        }

        @Override
        public byte[] encode(final ImageRGB image, final boolean keepAlpha) {
            return name.getBytes(StandardCharsets.US_ASCII);
        }
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
