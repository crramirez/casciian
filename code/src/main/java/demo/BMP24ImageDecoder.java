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
package demo;

import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Image decoder for 24-bit BMP (Windows Bitmap) format files.
 * Supports uncompressed 24-bit RGB BMP images.
 */
public class BMP24ImageDecoder implements ImageDecoder {

    @Override
    public ImageRGB decode(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read BMP file header (14 bytes)
        short signature = buffer.getShort();
        if (signature != 0x4D42) { // "BM" in little-endian
            throw new IOException("Not a valid BMP file");
        }

        buffer.getInt(); // file size
        buffer.getInt(); // reserved
        int dataOffset = buffer.getInt();

        // Read DIB header (at least 40 bytes for BITMAPINFOHEADER)
        int headerSize = buffer.getInt();
        if (headerSize < 40) {
            throw new IOException("Unsupported BMP header format");
        }

        int width = buffer.getInt();
        int height = buffer.getInt();
        buffer.getShort(); // planes (must be 1)
        short bitsPerPixel = buffer.getShort();

        if (bitsPerPixel != 24) {
            throw new IOException("Only 24-bit BMP files are supported");
        }

        int compression = buffer.getInt();
        if (compression != 0) { // BI_RGB (no compression)
            throw new IOException("Only uncompressed BMP files are supported");
        }

        // Skip rest of header and any color table
        buffer.position(dataOffset);

        // Read pixel data
        // BMP stores rows bottom-to-top, and each row is padded to 4-byte boundary
        int rowSize = ((width * 3 + 3) / 4) * 4;
        boolean topDown = height < 0;
        int absHeight = Math.abs(height);

        ImageRGB image = new ImageRGB(width, absHeight);

        for (int y = 0; y < absHeight; y++) {
            int row = topDown ? y : (absHeight - 1 - y);
            for (int x = 0; x < width; x++) {
                // BMP stores pixels as BGR
                int blue = buffer.get() & 0xFF;
                int green = buffer.get() & 0xFF;
                int red = buffer.get() & 0xFF;

                int rgb = (red << 16) | (green << 8) | blue;
                image.setRGB(x, row, rgb);
            }

            // Skip row padding
            int padding = rowSize - (width * 3);
            buffer.position(buffer.position() + padding);
        }

        return image;
    }

    @Override
    public String getFileExtensionPattern() {
        return "^.*\\.[bB][mM][pP]$";
    }

    @Override
    public String getFormatDescription() {
        return "24-bit BMP Image Files (*.bmp)";
    }
}
