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

        attributes = new CellAttributes();
        attributes.setBackColorRGB(0xABCDEF);
        terminal.putAttrXY(1, 1, attributes);
        terminal.flushPhysical();

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
