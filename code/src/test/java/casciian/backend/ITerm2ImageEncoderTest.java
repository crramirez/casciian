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

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import casciian.image.encoders.ImageEncoder;
import casciian.image.encoders.PngImageEncoder;
import casciian.image.encoders.PngImageEncoderTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the iTerm2 inline image sequence and encoder failure handling.
 */
@DisplayName("ITerm2ImageEncoder")
class ITerm2ImageEncoderTest {

    private static final Pattern SEQUENCE = Pattern.compile(
        "\033\\]1337;File=inline=1;size=(\\d+);width=(\\d+);height=(\\d+)"
            + ";preserveAspectRatio=0;doNotMoveCursor=1:([A-Za-z0-9+/=]*)\007");

    @ParameterizedTest(name = "{0} bytes")
    @ValueSource(ints = {1, 2, 3, 4, 5, 1000, 6144, 6145, 65537})
    @DisplayName("The payload is the complete base64 of the file")
    void payloadIsBase64OfFile(final int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i * 31 + 7);
        }

        Matcher m = SEQUENCE.matcher(ITerm2ImageEncoder.inlineImage(data, 3,
            1));

        assertThat(m.matches()).isTrue();
        assertThat(m.group(1)).isEqualTo(String.valueOf(length));
        assertThat(m.group(2)).isEqualTo("3");
        assertThat(m.group(3)).isEqualTo("1");
        assertThat(Base64.getDecoder().decode(m.group(4))).isEqualTo(data);
    }

    @Test
    @DisplayName("Failing add-on encoders fall back to a valid PNG")
    void failingEncoderFallsBackToPng() {
        ImageRGB image = new ArrayImageRGB(4, 2);
        image.fillRect(0, 0, 4, 2, 0x80102030);

        for (ImageEncoder broken : new ImageEncoder[] {
                encoder(new IOException("boom")),
                encoder(new IllegalStateException("boom")),
                returning(null), returning(new byte[0])}) {
            String sequence = new ITerm2ImageEncoder(broken, true)
                .transmitAndPlace(image, 2, 1);

            PngImageEncoderTest.Png png = PngImageEncoderTest.decode(
                payload(sequence));
            // The add-on does not support alpha, so neither does the
            // fallback.
            assertThat(png.bitsPerPixel()).isEqualTo(24);
            assertThat(png.pixels()[1][3]).isEqualTo(0xFF102030);
        }
    }

    @Test
    @DisplayName("No sequence is produced when no valid image can be made")
    void unencodableImageProducesNothing() {
        ImageRGB unreadable = Mockito.mock(ImageRGB.class);
        Mockito.when(unreadable.getWidth()).thenReturn(4);
        Mockito.when(unreadable.getHeight()).thenReturn(2);
        Mockito.when(unreadable.getRGB(Mockito.anyInt(), Mockito.anyInt(),
            Mockito.anyInt(), Mockito.anyInt(), Mockito.any(),
            Mockito.anyInt(), Mockito.anyInt()))
            .thenThrow(new IllegalStateException("unreadable"));

        assertThat(new ITerm2ImageEncoder(new PngImageEncoder(), true)
            .transmitAndPlace(unreadable, 2, 1)).isNull();
        assertThat(new ITerm2ImageEncoder(returning(null), false)
            .transmitAndPlace(unreadable, 2, 1)).isNull();
    }

    private static byte[] payload(final String sequence) {
        Matcher m = SEQUENCE.matcher(sequence);
        assertThat(m.matches()).isTrue();
        byte[] data = Base64.getDecoder().decode(m.group(4));
        assertThat(m.group(1)).isEqualTo(String.valueOf(data.length));
        return data;
    }

    private static ImageEncoder encoder(final Exception failure) {
        return new TestEncoder() {
            @Override
            public byte[] encode(final ImageRGB image, final boolean alpha)
                throws IOException {
                if (failure instanceof IOException e) {
                    throw e;
                }
                throw (RuntimeException) failure;
            }
        };
    }

    private static ImageEncoder returning(final byte[] data) {
        return new TestEncoder() {
            @Override
            public byte[] encode(final ImageRGB image, final boolean alpha) {
                return data;
            }
        };
    }

    private abstract static class TestEncoder implements ImageEncoder {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public String getMimeType() {
            return "image/x-test";
        }
    }
}
