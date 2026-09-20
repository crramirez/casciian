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
package casciian.imagedecoders.decoders;

import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Black-box tests for {@link BMP24ImageDecoder}. Exercises the public
 * {@link ImageDecoder} API against synthetic 24-bit BMP data without inspecting
 * implementation details.
 */
class BMP24ImageDecoderTest {

    @Test
    void decodesBottomUp24BitBmpIntoMatchingImageRGB() throws IOException {
        // 2x2 image. BMP stores rows bottom-to-top, so the first stored row is
        // the bottom row of the final image.
        byte[] bmp = buildBmp(2, 2, new int[][]{
            // bottom row (y = 1): blue, white
            {0x0000FF, 0xFFFFFF},
            // top row (y = 0): red, green
            {0xFF0000, 0x00FF00},
        });

        ImageRGB image = new BMP24ImageDecoder().decode(new ByteArrayInputStream(bmp), "image/bmp");

        assertThat(image.getWidth()).isEqualTo(2);
        assertThat(image.getHeight()).isEqualTo(2);
        assertThat(image.getRGB(0, 0) & 0x00FFFFFF).isEqualTo(0xFF0000);
        assertThat(image.getRGB(1, 0) & 0x00FFFFFF).isEqualTo(0x00FF00);
        assertThat(image.getRGB(0, 1) & 0x00FFFFFF).isEqualTo(0x0000FF);
        assertThat(image.getRGB(1, 1) & 0x00FFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    void rejectsNonBmpData() {
        byte[] notBmp = "this is not a bitmap".getBytes();

        assertThatThrownBy(() ->
            new BMP24ImageDecoder().decode(new ByteArrayInputStream(notBmp), null))
            .isInstanceOf(IOException.class);
    }

    @Test
    void extensionPatternMatchesBmpFilesCaseInsensitively() {
        ImageDecoder decoder = new BMP24ImageDecoder();
        String pattern = decoder.getFileExtensionPattern();

        assertThat("picture.bmp").matches(pattern);
        assertThat("PICTURE.BMP").matches(pattern);
        assertThat("picture.png").doesNotMatch(pattern);
        assertThat(decoder.getSupportedMimeTypes()).contains("image/bmp");
        assertThat(decoder.getFormatDescription()).isNotBlank();
    }

    /**
     * Build a minimal uncompressed 24-bit BMP (BITMAPINFOHEADER) from rows of
     * 0xRRGGBB pixels, stored bottom-to-top as BMP requires.
     */
    private static byte[] buildBmp(int width, int height, int[][] rowsBottomUp) throws IOException {
        int rowSize = ((width * 3 + 3) / 4) * 4;
        int pixelDataSize = rowSize * height;
        int dataOffset = 14 + 40;
        int fileSize = dataOffset + pixelDataSize;

        ByteBuffer buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);

        // File header (14 bytes)
        buffer.putShort((short) 0x4D42); // "BM"
        buffer.putInt(fileSize);
        buffer.putInt(0); // reserved
        buffer.putInt(dataOffset);

        // DIB header (BITMAPINFOHEADER, 40 bytes)
        buffer.putInt(40);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.putShort((short) 1);  // planes
        buffer.putShort((short) 24); // bits per pixel
        buffer.putInt(0);            // BI_RGB
        buffer.putInt(pixelDataSize);
        buffer.putInt(2835);         // x pixels/meter
        buffer.putInt(2835);         // y pixels/meter
        buffer.putInt(0);            // colors used
        buffer.putInt(0);            // important colors

        // Pixel data, bottom-to-top, BGR, padded to 4-byte rows.
        ByteArrayOutputStream pixels = new ByteArrayOutputStream();
        for (int[] row : rowsBottomUp) {
            for (int rgb : row) {
                pixels.write(rgb & 0xFF);          // blue
                pixels.write((rgb >> 8) & 0xFF);   // green
                pixels.write((rgb >> 16) & 0xFF);  // red
            }
            for (int p = width * 3; p < rowSize; p++) {
                pixels.write(0);
            }
        }
        buffer.put(pixels.toByteArray());

        return buffer.array();
    }
}
