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
package casciian;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import casciian.backend.HeadlessBackend;
import casciian.backend.SystemProperties;
import casciian.bits.AnsiParser;
import casciian.bits.ArrayImageRGB;
import casciian.bits.CasciianMarkupParser;
import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.RichText;
import casciian.image.decoders.ImageDecoder;
import casciian.image.decoders.ImageDecoderRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Image blocks through the public rich-text, markup, and widget APIs.
 */
class RichTextImagesTest {
    private static final String IMAGE_PATH = "picture.rich-image-test";

    @Test
    void imageRunsRemainDistinctAndImmutable() {
        CellAttributes attributes = CellAttributes.builder().bold(true).build();
        RichText text = RichText.builder().append("before", attributes)
            .image(IMAGE_PATH, "", attributes)
            .image(IMAGE_PATH, "description", attributes)
            .append("after", attributes).build();
        attributes.setBold(false);

        assertThat(text.getRuns()).hasSize(4);
        assertThat(text.getRuns().get(1).isImage()).isTrue();
        assertThat(text.getRuns().get(1).getAttributes().isBold()).isTrue();
        CellAttributes copy = text.getRuns().get(1).getAttributes();
        copy.setBold(false);
        assertThat(text.getRuns().get(1).getAttributes().isBold()).isTrue();
        assertThat(text.getPlainText()).isEqualTo("beforedescriptionafter");
        assertThat(RichText.builder().image(IMAGE_PATH, "").build().isEmpty())
            .isFalse();
        assertThat(text).isNotEqualTo(RichText.builder()
            .append(text.getPlainText(), attributes).build());
    }

    @Test
    void markupRoundTripsEscapedPathsEmptyAltAndStylesWithoutOpeningFiles()
            throws IOException {
        ImageDecoder decoder = mock(ImageDecoder.class);
        when(decoder.getFileExtensionPattern()).thenReturn(".*rich-image-test");
        ImageDecoderRegistry registry = ImageDecoderRegistry.getInstance();
        registry.registerDecoder(decoder);
        try {
            RichText text = RichText.builder()
                .push(CellAttributes.builder().italic(true).build())
                .image("a [folder]/\"quoted\"\\picture.rich-image-test", "")
                .image(IMAGE_PATH, "[literal] and [/]")
                .append("tail").build();
            RichText parsed = CasciianMarkupParser.parse(
                CasciianMarkupParser.toMarkup(text));
            assertThat(parsed).isEqualTo(text);
            assertThat(CasciianMarkupParser.parse(
                "[bold][image=\"missing file\" ]Alt-text[/]tail[/]")
                .getRuns()).hasSize(2).allSatisfy(run ->
                    assertThat(run.getAttributes().isBold()).isTrue());
            verify(decoder, never()).decode(any(Path.class));
        } finally {
            registry.unregisterDecoder(decoder);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "before[image=x]alt[/]after|before,alt,after",
        "before\\n[image=x]alt[/]\\nafter|before,alt,after",
        "before\\r\\n[image=x]alt[/]\\r\\nafter|before,alt,after",
        "before\\n\\n[image=x]alt[/]\\n\\nafter|before,,alt,,after",
        "[image=x]a[/][image=y]b[/]|a,b",
        "[image=x][/][image=y]b[/]|,b",
        "[image=x]alt[/]\\n|alt",
        "[image=x]alt[/]|alt",
        "[image=x]1234567[/]tail|123456,7,tail"
    })
    void alternativeTextUsesStandaloneRows(final String markup,
            final String rows) {
        assertThat(AnsiParser.layout(CasciianMarkupParser.parse(
            markup.replace("\\n", "\n").replace("\\r", "\r")), 6)
            .stream().map(RichTextImagesTest::lineText).toList())
            .containsExactly(rows.split(",", -1));
    }

    @Test
    void customLayoutRendererPreservesImageCellsAndBlockBoundaries() {
        Cell cell = new Cell();
        cell.setImage(new ArrayImageRGB(2, 2));
        List<AnsiParser.Line> lines = AnsiParser.layout(
            CasciianMarkupParser.parse("a[image=x][/]\nb"), 4,
            run -> List.of(List.of(cell), List.of(cell)));
        assertThat(lines).hasSize(4);
        assertThat(lines.get(1).getCells().getFirst().isImage()).isTrue();
        assertThat(lineText(lines.getLast())).isEqualTo("b");
    }

    @Test
    void appendingAnsiPreservesImagesIncludingEmptyAlt() {
        TTextAnsi widget = new TTextAnsi(null, "", 0, 0, 8, 6);
        widget.setMarkup("[image=\"missing\"][/]");
        widget.appendText("\033[1mtail");
        assertThat(widget.getRichText().getRuns()).hasSize(2);
        assertThat(widget.getRichText().getRuns().getFirst().isImage()).isTrue();
        assertThat(widget.getRichText().getRuns().getLast()
            .getAttributes().isBold()).isTrue();
        assertThat(widget.getRichText()).isEqualTo(CasciianMarkupParser.parse(
            CasciianMarkupParser.toMarkup(widget.getRichText())));
    }

    @Test
    void widgetFitsProportionallyAndReflowsForWidthAndPixelMetrics()
            throws IOException {
        ImageDecoder decoder = registerImage(24, 16);
        ImageDecoderRegistry registry = ImageDecoderRegistry.getInstance();
        try {
            PixelBackend backend = new PixelBackend();
            TTextAnsi widget = widget(backend, 4);
            widget.setMarkup("a[image=\"" + IMAGE_PATH + "\"]unused[/]b");
            widget.drawChildren();
            // Content is 3 cells * 4 pixels: 24x16 becomes 12x8 (one row).
            assertThat(cell(widget, 0, 0).getChar()).isEqualTo('a');
            assertThat(cell(widget, 2, 1).isImage()).isTrue();
            assertThat(cell(widget, 0, 2).getChar()).isEqualTo('b');
            assertThat(cell(widget, 2, 1).getImage().getWidth()).isEqualTo(4);

            widget.setWidth(7);
            widget.drawChildren();
            // 24x16 now fits without scaling: six columns, two rows.
            assertThat(cell(widget, 5, 2).isImage()).isTrue();
            assertThat(cell(widget, 0, 3).getChar()).isEqualTo('b');

            backend.pixelHeight = 4;
            widget.drawChildren();
            assertThat(cell(widget, 5, 4).isImage()).isTrue();
            assertThat(cell(widget, 0, 5).getChar()).isEqualTo('b');
            backend.pixelWidth = 2;
            widget.drawChildren();
            // The same six content columns now hold only 12 pixels, so shrink.
            assertThat(cell(widget, 5, 2).isImage()).isTrue();
            assertThat(cell(widget, 0, 3).getChar()).isEqualTo('b');
            verify(decoder, times(1)).decode(Path.of(IMAGE_PATH));
        } finally {
            registry.unregisterDecoder(decoder);
        }
    }

    @Test
    void smallImagesAreNotEnlargedAndPartialTilesArePadded()
            throws IOException {
        ImageDecoder decoder = registerImage(5, 9);
        try {
            TTextAnsi widget = widget(new PixelBackend(), 9);
            widget.setMarkup("[image=\"" + IMAGE_PATH + "\"]unused[/]end");
            widget.drawChildren();
            assertThat(cell(widget, 1, 1).isImage()).isTrue();
            assertThat(cell(widget, 2, 0).isImage()).isFalse();
            assertThat(cell(widget, 0, 2).getChar()).isEqualTo('e');
        } finally {
            ImageDecoderRegistry.getInstance().unregisterDecoder(decoder);
        }
    }

    @Test
    void tallImageRowsParticipateInScrollingWithoutHorizontalOverflow()
            throws IOException {
        ImageDecoder decoder = registerImage(8, 80);
        try {
            TTextAnsi widget = widget(new PixelBackend(), 5);
            widget.setMarkup("before[image=\"" + IMAGE_PATH
                + "\"]unused[/]after");
            // Both words wrap at four columns; the image occupies ten rows.
            assertThat(widget.getVerticalScroller().getBottomValue()).isEqualTo(5);
            assertThat(widget.getHorizontalScroller().getRightValue()).isZero();
            widget.setVerticalValue(5);
            widget.drawChildren();
            assertThat(cell(widget, 0, 0).isImage()).isTrue();
            assertThat(cell(widget, 0, 7).getChar()).isEqualTo('a');
            assertThat(cell(widget, 0, 8).getChar()).isEqualTo('r');
        } finally {
            ImageDecoderRegistry.getInstance().unregisterDecoder(decoder);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing.unknown-format", "broken.rich-image-test",
        "invalid\u0000path", "", "/"})
    void unavailableImagesDisplayAltOnSeparateRows(final String path)
            throws IOException {
        ImageDecoder decoder = registerImage(8, 8);
        when(decoder.decode(any(Path.class))).thenThrow(new IOException("bad"));
        try {
            TTextAnsi widget = widget(new PixelBackend(), 10);
            widget.setRichText(RichText.builder().append("before")
                .image(path, "ALT").append("after").build());
            widget.drawChildren();
            assertThat(cell(widget, 0, 0).getChar()).isEqualTo('b');
            assertThat(cell(widget, 0, 1).getChar()).isEqualTo('A');
            assertThat(cell(widget, 0, 2).getChar()).isEqualTo('a');
        } finally {
            ImageDecoderRegistry.getInstance().unregisterDecoder(decoder);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"halves", "solid"})
    void textFallbackMatchesTImageRendering(final String mode)
            throws IOException {
        String previousMode = SystemProperties.getImageFallbackDisplayMode();
        SystemProperties.setImageFallbackDisplayMode(mode);
        ImageDecoder decoder = registerImage(8, 8);
        try {
            PixelBackend backend = new PixelBackend();
            backend.bitmap = false;
            TTextAnsi widget = widget(backend, 5);
            widget.setMarkup("[image=\"" + IMAGE_PATH + "\"]unused[/]");
            widget.drawChildren();
            Cell rendered = new Cell(cell(widget, 0, 0));
            TImage image = new TImage(widget.getParent(), 0, 0, 4, 2,
                decoder.decode(Path.of(IMAGE_PATH)), 0, 0);
            image.drawChildren();
            Cell expected = image.getScreen().getCharXY(
                image.getAbsoluteX(), image.getAbsoluteY());
            assertThat(rendered.isImage()).isFalse();
            assertThat(rendered.getChar()).isEqualTo(expected.getChar());
            assertThat(rendered.getForeColorRGB())
                .isEqualTo(expected.getForeColorRGB());
            assertThat(rendered.getBackColorRGB())
                .isEqualTo(expected.getBackColorRGB());
        } finally {
            ImageDecoderRegistry.getInstance().unregisterDecoder(decoder);
            SystemProperties.setImageFallbackDisplayMode(previousMode);
        }
    }

    private static ImageDecoder registerImage(final int width, final int height)
            throws IOException {
        ArrayImageRGB image = new ArrayImageRGB(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, ((x + y) % 2 == 0) ? 0xFF0099FF : 0xFFFF3300);
            }
        }
        ImageDecoder decoder = mock(ImageDecoder.class);
        when(decoder.getFileExtensionPattern())
            .thenReturn(".*\\.rich-image-test");
        when(decoder.decode(any(Path.class))).thenReturn(image);
        ImageDecoderRegistry.getInstance().registerDecoder(decoder);
        return decoder;
    }

    private static TTextAnsi widget(final PixelBackend backend,
            final int width) {
        TApplication application = new TApplication(backend);
        TWindow window = new TWindow(application, "images", 0, 0, 40, 20);
        return new TTextAnsi(window, "", 0, 0, width, 10);
    }

    private static Cell cell(final TWidget widget, final int x, final int y) {
        return widget.getScreen().getCharXY(widget.getAbsoluteX() + x,
            widget.getAbsoluteY() + y);
    }

    private static String lineText(final AnsiParser.Line line) {
        StringBuilder text = new StringBuilder();
        for (Cell cell : line.getCells()) {
            text.appendCodePoint(cell.getChar());
        }
        return text.toString();
    }

    private static final class PixelBackend extends HeadlessBackend {
        private int pixelWidth = 4;
        private int pixelHeight = 8;
        private boolean bitmap = true;

        @Override
        public int getTextWidth() {
            return pixelWidth;
        }

        @Override
        public int getTextHeight() {
            return pixelHeight;
        }

        @Override
        public boolean isImageProtocolSupported() {
            return bitmap;
        }
    }
}
