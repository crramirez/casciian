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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import casciian.bits.ArrayImageRGB;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.ImageRGB;
import casciian.event.TInputEvent;
import casciian.event.TKeypressEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static casciian.backend.KittyImageEncoderTest.parse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that ECMA48Terminal detects the kitty graphics protocol and draws
 * image cells with it.
 */
@DisplayName("ECMA48Terminal kitty graphics protocol")
class ECMA48TerminalKittyGraphicsTest {

    private static final long TIMEOUT_MILLIS = 5000;

    /** A kitty reply to the support query, then Device Attributes. */
    private static final String KITTY_REPLY = "\033_Gi=31;OK\033\\\033[?62;4c";

    private ECMA48Terminal terminal;
    private ByteArrayOutputStream outputStream;

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
        clearProperties();
    }

    @Test
    @DisplayName("Startup queries kitty graphics before Device Attributes")
    void startupSendsQuery() {
        terminal = createTerminal("");

        String written = written();
        assertThat(written).contains(KittyImageEncoder.query());
        assertThat(written.indexOf(KittyImageEncoder.query()))
            .isLessThan(written.indexOf("\033[c"));
    }

    @Test
    @DisplayName("The query can be turned off by system property")
    void queryCanBeDisabled() {
        System.setProperty("casciian.ECMA48.kittyImages", "false");

        terminal = createTerminal(KITTY_REPLY + "z");

        assertThat(written()).doesNotContain(KittyImageEncoder.query());
        // Once "z" arrives, the reply and Device Attributes were processed.
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        List<TInputEvent> events = new ArrayList<>();
        while (events.stream().noneMatch(e -> (e instanceof TKeypressEvent k)
                && (k.getKey().getChar() == 'z'))
        ) {
            assertThat(System.currentTimeMillis()).isLessThan(deadline);
            terminal.getEvents(events);
            sleep();
        }
        assertThat(terminal.hasKittyImages()).isFalse();
    }

    @Test
    @DisplayName("An OK reply enables kitty images and is not seen as input")
    void replyEnablesKittyImages() {
        terminal = createTerminal(KITTY_REPLY);

        waitForKitty();
        assertThat(terminal.isImageProtocolSupported()).isTrue();
        assertThat(drainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Query replies with a placement id are accepted")
    void replyWithPlacementId() {
        terminal = createTerminal("\033_Gi=31,p=0;OK\033\\\033[?62;4c");
        waitForKitty();
        assertThat(drainEvents()).isEmpty();
    }

    @Test
    @DisplayName("A negative query response does not become keyboard input")
    void negativeQueryReply() {
        terminal = createTerminal("\033_Gi=31;EINVAL:bad\033\\\033[?62;4cz");
        List<TInputEvent> events = waitForEvents(1);
        assertThat(terminal.hasKittyImages()).isFalse();
        assertThat(events).hasSize(1);
        assertThat(((TKeypressEvent) events.getFirst()).getKey().getChar())
            .isEqualTo('z');
    }

    @Test
    @DisplayName("A partial query response times out and restores input")
    void partialQueryReplyTimesOut() {
        terminal = createTerminal("\033_Gi=31;");
        List<TInputEvent> events = waitForEvents(7);
        assertThat(((TKeypressEvent) events.getFirst()).getKey().isAlt()).isTrue();
        assertThat(events).hasSize(7);
    }

    @Test
    @DisplayName("Without a reply, kitty images stay off and Alt-_ still works")
    void noReplyMeansUnsupported() {
        terminal = createTerminal("\033[?62;4c\033_");

        List<TInputEvent> events = waitForEvents(1);
        assertThat(terminal.hasKittyImages()).isFalse();
        TKeypressEvent key = (TKeypressEvent) events.getFirst();
        assertThat(key.getKey().getChar()).isEqualTo('_');
        assertThat(key.getKey().isAlt()).isTrue();
    }

    @Test
    @DisplayName("An incomplete APC returns Alt-_ and reparses buffered input")
    void incompleteQueryReplyRecoversInput() {
        terminal = createTerminal("\033_G\033[?62;4cz");

        List<TInputEvent> events = waitForEvents(3);

        assertThat(events).hasSize(3);
        assertThat(((TKeypressEvent) events.get(0)).getKey().getChar())
            .isEqualTo('_');
        assertThat(((TKeypressEvent) events.get(0)).getKey().isAlt())
            .isTrue();
        assertThat(((TKeypressEvent) events.get(1)).getKey().getChar())
            .isEqualTo('G');
        assertThat(((TKeypressEvent) events.get(2)).getKey().getChar())
            .isEqualTo('z');
    }

    @Test
    @DisplayName("Image cells are transmitted once and then only re-placed")
    void imageCellsUseImageIdCache() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.flushPhysical();

        terminal.putCharXY(2, 1, imageCell(500));
        outputStream.reset();
        terminal.flushPhysical();

        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).hasSize(1);
        assertThat(commands.getFirst().keys()).containsEntry("a", "T")
            .containsEntry("i", "500");
        assertThat(written()).contains("\033[2;3H\033_G");

        // Move the image: delete the old placement, re-place, no pixels.
        terminal.putCharXY(2, 1, new Cell('x', new CellAttributes()));
        terminal.putCharXY(5, 3, imageCell(500));
        outputStream.reset();
        terminal.flushPhysical();

        commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("a"))
            .containsExactlyInAnyOrder("d", "p");
        assertThat(commands).allSatisfy(c ->
            assertThat(c.payload()).isEmpty());
        assertThat(written()).contains("x");

        // Nothing changed: nothing sent.
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).isEmpty();
    }

    @Test
    @DisplayName("Colliding image ids trigger a redraw at the same position")
    void collidingImageAtSamePosition() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        Cell first = imageCell(500);
        first.setChar(' ');
        terminal.putCharXY(2, 1, first);
        terminal.flushPhysical();

        Cell second = imageCell(500);
        second.setChar(' ');
        second.getImage().setRGB(0, 0, 0xFF998877);
        terminal.putCharXY(2, 1, second);
        assertThat(terminal.isDirty()).isTrue();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "T");
    }

    @Test
    @DisplayName("Clearing physical cells re-places cached kitty images")
    void physicalClearReplacesImageWithoutRetransmitting() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.putCharXY(2, 1, imageCell(500));
        terminal.flushPhysical();
        outputStream.reset();

        terminal.clearPhysical();
        terminal.flushPhysical();

        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("a", "p")
                .containsEntry("i", "500"));
        assertThat(commands.getFirst().payload()).isEmpty();

        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).isEmpty();
    }

    @Test
    @DisplayName("Resizing deletes cached Kitty images")
    void resizeResetsKittyImageCache() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.putCharXY(2, 1, imageCell(500));
        terminal.flushPhysical();
        outputStream.reset();

        terminal.setDimensions(terminal.getWidth() + 1, terminal.getHeight());
        terminal.flushPhysical();

        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("d", "I")
                .containsEntry("i", "500"));
    }

    @Test
    @DisplayName("A new terminal cell size invalidates and rescales cached tiles")
    void changedCellPixelSizeReuploadsTiles() throws Exception {
        try (PipedOutputStream input = new PipedOutputStream();
             PipedInputStream source = new PipedInputStream(input)) {
            terminal = new ECMA48Terminal(Mockito.mock(Backend.class), null,
                source, outputStream);
            input.write((KITTY_REPLY + "\033[6;16;12tz")
                .getBytes(StandardCharsets.UTF_8));
            input.flush();
            waitForKey('z');
            waitForKitty();
            terminal.putCharXY(2, 1, imageCell(500));
            terminal.flushPhysical();
            assertThat(parse(written())).anySatisfy(c ->
                assertThat(c.keys()).containsEntry("a", "T")
                    .containsEntry("s", "12").containsEntry("v", "16"));
            outputStream.reset();

            input.write("\033[6;16;8ty".getBytes(StandardCharsets.UTF_8));
            input.flush();
            waitForKey('y');
            assertThat(terminal.isDirty()).isTrue();
            terminal.flushPhysical();
            List<KittyImageEncoderTest.Command> commands = parse(written());
            assertThat(commands).anySatisfy(c ->
                assertThat(c.keys()).containsEntry("d", "I")
                    .containsEntry("i", "500"));
            assertThat(commands).anySatisfy(c ->
                assertThat(c.keys()).containsEntry("a", "T")
                    .containsEntry("s", "8").containsEntry("v", "16"));
        }
    }

    @Test
    @DisplayName("Turning kitty images off frees the terminal's images")
    void disablingFreesImages() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.putCharXY(0, 0, imageCell(9));
        terminal.flushPhysical();
        outputStream.reset();

        terminal.setHasKittyImages(false);

        assertThat(terminal.hasKittyImages()).isFalse();
        assertThat(parse(written())).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("d", "I")
                .containsEntry("i", "9"));
    }

    @Test
    @DisplayName("RGBA format and disabled compression are honored")
    void formatOptions() {
        System.setProperty("casciian.ECMA48.kittyImageFormat", "rgba");
        System.setProperty("casciian.ECMA48.kittyImageCompression", "false");
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.flushPhysical();

        terminal.putCharXY(0, 0, imageCell(3));
        outputStream.reset();
        terminal.flushPhysical();

        assertThat(parse(written())).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("f", "32")
                .doesNotContainKey("o"));
    }

    @Test
    @DisplayName("RGBA backing spaces repaint changed backgrounds with reset SGR")
    void rgbaBackgroundChangeRepaintsBackingCell() {
        System.setProperty("casciian.ECMA48.kittyImageFormat", "rgba");
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.putCharXY(1, 1, imageCell(3));

        CellAttributes attributes = new CellAttributes();
        attributes.setBackColorRGB(0x123456);
        attributes.setReverse(true);
        attributes.setUnderlineStyle(CellAttributes.UNDERLINE_STYLE_SINGLE);
        terminal.putAttrXY(1, 1, attributes);
        terminal.flushPhysical();
        outputStream.reset();
        assertThat(terminal.isDirty()).isFalse();

        attributes = new CellAttributes();
        attributes.setBackColorRGB(0xABCDEF);
        terminal.putAttrXY(1, 1, attributes);
        assertThat(terminal.isDirty()).isTrue();
        terminal.flushPhysical();
        assertThat(terminal.isDirty()).isFalse();

        String written = written();
        List<KittyImageEncoderTest.Command> commands = parse(written);
        assertThat(commands).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("a", "p")
                .containsEntry("i", "3"));
        assertThat(commands.getFirst().payload()).isEmpty();
        assertThat(written).contains("\033[0m")
            .contains("\033[48;2;171;205;239m ")
            .doesNotContain("\033[4m", "\033[7m");
        assertThat(written.indexOf("\033[0m"))
            .isLessThan(written.indexOf("\033[48;2;171;205;239m "));
        assertThat(written.indexOf("\033[48;2;171;205;239m "))
            .isLessThan(written.indexOf("\033_Ga=p"));
    }

    @Test
    @DisplayName("Horizontal tiles form one scaled image, reused on move and clear")
    void horizontalRunGroupsPixelsAndReusesCache() {
        System.setProperty("casciian.ECMA48.kittyImageCompression", "false");
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.flushPhysical();
        Cell left = solidImageCell(501, 1, 2, 0x80112233);
        Cell right = solidImageCell(502, 3, 1, 0xFF445566);
        terminal.putCharXY(2, 1, left);
        terminal.putCharXY(3, 1, right);
        outputStream.reset();
        terminal.flushPhysical();

        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).hasSize(1);
        KittyImageEncoderTest.Command upload = commands.getFirst();
        assertThat(upload.keys()).containsEntry("a", "T")
            .containsEntry("c", "2").containsEntry("r", "1")
            .containsEntry("C", "1")
            .containsEntry("s", Integer.toString(2 * terminal.getTextWidth()))
            .containsEntry("v", Integer.toString(terminal.getTextHeight()));
        assertRunPixels(upload, 3, 0x80112233, 0xFF445566);
        String imageId = upload.keys().get("i");
        assertThat(terminal.isDirty()).isFalse();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).isEmpty();

        terminal.clearPhysical();
        terminal.flushPhysical();
        assertThat(parse(written())).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("a", "p")
                .containsEntry("i", imageId).containsEntry("c", "2"));
        outputStream.reset();

        terminal.putCharXY(2, 1, new Cell('L'));
        terminal.putCharXY(3, 1, new Cell('R'));
        terminal.putCharXY(5, 3, left);
        terminal.putCharXY(6, 3, right);
        terminal.flushPhysical();
        commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "p");
        assertThat(commands.getLast().keys()).containsEntry("i", imageId)
            .containsEntry("c", "2");
        assertThat(commands).allSatisfy(c -> assertThat(c.payload()).isEmpty());
        assertThat(terminal.isDirty()).isFalse();
    }

    @Test
    @DisplayName("A tile pixel change inside a run replaces it and restores from cache")
    void changedInteriorTileReplacesWholeRun() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        for (int x = 1; x <= 3; x++) {
            terminal.putCharXY(x, 1, imageCell(500 + x));
        }
        terminal.flushPhysical();
        outputStream.reset();
        Cell changed = imageCell(502);
        changed.getImage().setRGB(0, 0, 0xFF998877);
        terminal.putCharXY(2, 1, changed);
        assertThat(terminal.isDirty()).isTrue();
        terminal.flushPhysical();
        assertThat(parse(written())).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "T");
        assertThat(parse(written()).getLast().keys()).containsEntry("c", "3");
        assertThat(terminal.isDirty()).isFalse();

        outputStream.reset();
        terminal.putCharXY(2, 1, imageCell(502));
        terminal.flushPhysical();
        assertThat(parse(written())).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "p");
    }

    @Test
    @DisplayName("Text splits and shrinks shared runs without deleting replacements")
    void textSplitsShrinksAndMergesRuns() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        for (int x = 1; x <= 5; x++) {
            terminal.putCharXY(x, 1, imageCell(500 + x));
        }
        terminal.flushPhysical();
        outputStream.reset();
        terminal.putCharXY(3, 1, new Cell('X'));
        terminal.flushPhysical();
        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "T", "T");
        assertThat(commands.subList(1, 3)).allSatisfy(c ->
            assertThat(c.keys()).containsEntry("c", "2"));
        assertThat(written().indexOf("\033_Ga=d"))
            .isLessThan(written().indexOf("\033_Ga=T"));
        assertThat(written()).contains("X");
        assertThat(terminal.isDirty()).isFalse();

        outputStream.reset();
        terminal.putCharXY(2, 1, new Cell('Y'));
        terminal.putCharXY(5, 1, new Cell('Z'));
        terminal.flushPhysical();
        commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "d", "T", "T");
        assertThat(commands.subList(2, 4)).allSatisfy(c ->
            assertThat(c.keys()).containsEntry("c", "1"));
        assertThat(terminal.isDirty()).isFalse();

        outputStream.reset();
        for (int x = 1; x <= 5; x++) {
            terminal.putCharXY(x, 1, imageCell(500 + x));
        }
        terminal.flushPhysical();
        commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "d", "p");
        assertThat(commands.getLast().keys()).containsEntry("c", "5");
        assertThat(terminal.isDirty()).isFalse();
    }

    @Test
    @DisplayName("RGBA runs preserve alpha and paint each backing cell before placement")
    void rgbaRunBackgroundChangeReplacesWithoutPixels() {
        System.setProperty("casciian.ECMA48.kittyImageFormat", "rgba");
        System.setProperty("casciian.ECMA48.kittyImageCompression", "false");
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.flushPhysical();
        terminal.putCharXY(1, 1, solidImageCell(501, 1, 1, 0x00112233));
        terminal.putCharXY(2, 1, solidImageCell(502, 1, 1, 0x80445566));
        CellAttributes background = new CellAttributes();
        background.setBackColorRGB(0x123456);
        terminal.putAttrXY(1, 1, background);
        background.setBackColorRGB(0xABCDEF);
        terminal.putAttrXY(2, 1, background);
        outputStream.reset();
        terminal.flushPhysical();
        KittyImageEncoderTest.Command upload = parse(written()).getFirst();
        assertRunPixels(upload, 4, 0x00112233, 0x80445566);
        assertThat(written().indexOf("\033[48;2;18;52;86m "))
            .isLessThan(written().indexOf("\033_Ga=T"));
        assertThat(written().indexOf("\033[48;2;171;205;239m "))
            .isLessThan(written().indexOf("\033_Ga=T"));
        outputStream.reset();

        background.setBackColorRGB(0x998877);
        terminal.putAttrXY(2, 1, background);
        assertThat(terminal.isDirty()).isTrue();
        terminal.flushPhysical();
        assertThat(parse(written())).singleElement().satisfies(c ->
            assertThat(c.keys()).containsEntry("a", "p")
                .containsEntry("c", "2").containsEntry("i", upload.keys().get("i")));
        assertThat(written()).contains("\033[48;2;153;136;119m ");
        assertThat(terminal.isDirty()).isFalse();
    }

    @Test
    @DisplayName("Runs stop at text and row boundaries, including the bottom right")
    void runsRespectScreenBoundariesAndResize() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.flushPhysical();
        int lastColumn = terminal.getWidth() - 1;
        int lastRow = terminal.getHeight() - 1;
        terminal.putCharXY(lastColumn - 1, lastRow - 1, imageCell(501));
        terminal.putCharXY(lastColumn, lastRow - 1, imageCell(502));
        terminal.putCharXY(0, lastRow, imageCell(503));
        terminal.putCharXY(lastColumn - 1, lastRow, imageCell(504));
        terminal.putCharXY(lastColumn, lastRow, imageCell(505));
        outputStream.reset();
        terminal.flushPhysical();
        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("c"))
            .containsExactly("2", "1", "2");
        assertThat(commands).allSatisfy(c ->
            assertThat(c.keys()).containsEntry("r", "1").containsEntry("C", "1"));
        assertThat(terminal.isDirty()).isFalse();
        outputStream.reset();
        terminal.setDimensions(terminal.getWidth() - 1, terminal.getHeight() - 1);
        terminal.flushPhysical();
        assertThat(parse(written())).hasSize(3).allSatisfy(c ->
            assertThat(c.keys()).containsEntry("a", "d").containsEntry("d", "I"));
    }

    @Test
    @DisplayName("Inversion inside a run cannot reuse normal pixels")
    void invertedRunIsFreedAndNormalRunIsReused() {
        terminal = createTerminal(KITTY_REPLY);
        waitForKitty();
        terminal.putCharXY(1, 1, imageCell(501));
        terminal.putCharXY(2, 1, imageCell(502));
        terminal.flushPhysical();
        outputStream.reset();
        Cell inverted = imageCell(502);
        inverted.invertImage();
        terminal.putCharXY(2, 1, inverted);
        assertThat(terminal.isDirty()).isTrue();
        terminal.flushPhysical();
        assertThat(parse(written())).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "T");
        assertThat(terminal.isDirty()).isFalse();
        outputStream.reset();
        terminal.flushPhysical();
        assertThat(parse(written())).isEmpty();

        terminal.putCharXY(2, 1, imageCell(502));
        terminal.flushPhysical();
        List<KittyImageEncoderTest.Command> commands = parse(written());
        assertThat(commands).extracting(c -> c.keys().get("a"))
            .containsExactly("d", "p");
        assertThat(commands.getFirst().keys()).containsEntry("d", "I");
        assertThat(terminal.isDirty()).isFalse();
    }

    // ------------------------------------------------------------------------
    // Helpers ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static void clearProperties() {
        System.clearProperty("casciian.ECMA48.kittyImages");
        System.clearProperty("casciian.ECMA48.kittyImageFormat");
        System.clearProperty("casciian.ECMA48.kittyImageCompression");
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

    private static Cell imageCell(final int imageId) {
        ImageRGB image = new ArrayImageRGB(8, 16);
        image.fillRect(0, 0, 8, 16, 0xFF102030);
        Cell cell = new Cell();
        cell.setImage(image, imageId);
        return cell;
    }

    private static Cell solidImageCell(final int imageId, final int width,
        final int height, final int argb) {
        ImageRGB image = new ArrayImageRGB(width, height);
        image.fillRect(0, 0, width, height, argb);
        Cell cell = new Cell();
        cell.setImage(image, imageId);
        return cell;
    }

    private void assertRunPixels(final KittyImageEncoderTest.Command command,
        final int bytesPerPixel, final int left, final int right) {
        byte[] pixels = Base64.getDecoder().decode(command.payload());
        int tileWidth = terminal.getTextWidth();
        int tileHeight = terminal.getTextHeight();
        assertThat(pixels).hasSize(2 * tileWidth * tileHeight * bytesPerPixel);
        for (int y = 0; y < tileHeight; y++) {
            for (int x = 0; x < 2 * tileWidth; x++) {
                int argb = x < tileWidth ? left : right;
                int offset = (y * 2 * tileWidth + x) * bytesPerPixel;
                assertThat(pixels[offset]).isEqualTo((byte) (argb >>> 16));
                assertThat(pixels[offset + 1]).isEqualTo((byte) (argb >>> 8));
                assertThat(pixels[offset + 2]).isEqualTo((byte) argb);
                if (bytesPerPixel == 4) {
                    assertThat(pixels[offset + 3]).isEqualTo((byte) (argb >>> 24));
                }
            }
        }
    }

    private void waitForKitty() {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!terminal.hasKittyImages()) {
            assertThat(System.currentTimeMillis()).as("kitty detection")
                .isLessThan(deadline);
            sleep();
        }
    }

    private List<TInputEvent> drainEvents() {
        List<TInputEvent> events = new ArrayList<>();
        terminal.getEvents(events);
        return events;
    }

    private List<TInputEvent> waitForEvents(final int count) {
        List<TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (events.size() < count) {
            assertThat(System.currentTimeMillis()).as("events")
                .isLessThan(deadline);
            terminal.getEvents(events);
            sleep();
        }
        return events;
    }

    private void waitForKey(final char key) {
        List<TInputEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (events.stream().noneMatch(e -> e instanceof TKeypressEvent k
            && k.getKey().getChar() == key)) {
            assertThat(System.currentTimeMillis()).as("key " + key)
                .isLessThan(deadline);
            terminal.getEvents(events);
            sleep();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
