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

import java.util.concurrent.atomic.AtomicInteger;

import casciian.bits.Cell;
import casciian.bits.CellAttributes;
import casciian.bits.ImageRGB;
import casciian.bits.ImageUtils;
import casciian.bits.UnicodeGlyphImage;

/**
 * Shared image fitting, padding, tiling, and terminal fallback rendering.
 */
final class ImageCellRenderer {
    private static final AtomicInteger IMAGE_IDS = new AtomicInteger();

    private ImageCellRenderer() {
    }

    static ImageRGB fit(final ImageRGB image, final int pixelWidth,
            final int pixelHeight, final boolean enlarge) {
        double widthFactor = (double) pixelWidth / image.getWidth();
        double heightFactor = (double) pixelHeight / image.getHeight();
        double factor = Math.min(widthFactor, heightFactor);
        if (!enlarge && factor >= 1.0) {
            return image;
        }
        int width = widthFactor <= heightFactor ? pixelWidth
            : Math.max(1, (int) (image.getWidth() * factor));
        int height = heightFactor <= widthFactor ? pixelHeight
            : Math.max(1, (int) (image.getHeight() * factor));
        return image.scale(width, height);
    }

    static ImageRGB pad(final ImageRGB image, final int textWidth,
            final int textHeight, final int background) {
        int width = ((image.getWidth() + textWidth - 1) / textWidth) * textWidth;
        int height = ((image.getHeight() + textHeight - 1) / textHeight) * textHeight;
        return width == image.getWidth() && height == image.getHeight()
            ? image : image.resizeCanvas(width, height, background);
    }

    static Cell[][] render(final ImageRGB image, final int textWidth,
            final int textHeight, final CellAttributes attributes,
            final TImage.DisplayMode mode, final boolean optimizeSolid) {
        int columns = image.getWidth() / textWidth;
        int rows = image.getHeight() / textHeight;
        Cell[][] cells = new Cell[columns][rows];
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                Cell cell = new Cell();
                cell.setTo(attributes);
                cell.setImage(image.getSubimage(x * textWidth, y * textHeight,
                    textWidth, textHeight));
                if (!optimizeSolid || !cell.checkForSingleColor(true)) {
                    cell.setImageId(IMAGE_IDS.incrementAndGet() & 0x7FFFFFFF);
                }
                if (mode == TImage.DisplayMode.BLOCKS && cell.isImage()) {
                    int rgb = ImageUtils.rgbAverage(cell.getImage(),
                        0, 0, textWidth, textHeight);
                    cell = new Cell(' ');
                    cell.setForeColorRGB(rgb);
                    cell.setBackColorRGB(rgb);
                } else if (mode == TImage.DisplayMode.UNICODE_HALVES
                        && cell.isImage()) {
                    cell = new UnicodeGlyphImage(cell).toHalfBlockGlyph();
                }
                cells[x][y] = cell;
            }
        }
        return cells;
    }
}
