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

import casciian.bits.ImageRGB;
import casciian.image.encoders.BmpImageEncoder;
import casciian.image.encoders.ImageEncoder;

/**
 * Encoder for the iTerm2 inline images protocol ({@code OSC 1337 File=}).
 *
 * <p>The pixels are packed into an image file by a pluggable
 * {@link ImageEncoder} (BMP by default, or e.g. PNG/JPEG from add-on
 * modules), base64-encoded, and displayed at the cursor stretched over a
 * given number of text cells.  {@code doNotMoveCursor=1} keeps the cursor
 * in place, so images on the bottom row never scroll the screen.</p>
 *
 * @see <a href="https://iterm2.com/documentation-images.html">iTerm2
 * inline images protocol</a>
 */
final class ITerm2ImageEncoder {

    /**
     * Start of an inline image command.
     */
    private static final String OSC_START = "\033]1337;File=";

    /**
     * String terminator.  iTerm2 documents BEL for this sequence.
     */
    private static final String BEL = "\007";

    /**
     * The file format encoder.
     */
    private final ImageEncoder fileEncoder;

    /**
     * If true, keep the alpha channel.
     */
    private final boolean alpha;

    /**
     * Public constructor.
     *
     * @param fileEncoder the file format encoder
     * @param alpha if true, keep the alpha channel when the file format
     *              supports it
     */
    ITerm2ImageEncoder(final ImageEncoder fileEncoder, final boolean alpha) {
        this.fileEncoder = fileEncoder;
        this.alpha = alpha;
    }

    /**
     * Get the file format encoder.
     *
     * @return the encoder
     */
    ImageEncoder getFileEncoder() {
        return fileEncoder;
    }

    /**
     * Whether the alpha channel is transmitted.
     *
     * @return true if the alpha channel is requested and the file format
     * supports it
     */
    boolean isAlpha() {
        return alpha && fileEncoder.supportsAlpha();
    }

    /**
     * Transmit an image and display it at the cursor, stretched to fill
     * the given number of text cells.  The cursor does not move.  If the
     * file encoder fails, the image is sent as a BMP instead.
     *
     * @param image the image
     * @param columns number of text columns to fill
     * @param rows number of text rows to fill
     * @return the escape sequence to emit
     */
    String transmitAndPlace(final ImageRGB image, final int columns,
        final int rows) {

        byte[] data;
        try {
            data = fileEncoder.encode(image, isAlpha());
        } catch (IOException | RuntimeException e) {
            data = new BmpImageEncoder().encode(image, alpha);
        }
        return inlineImage(data, columns, rows);
    }

    /**
     * Build the inline image escape sequence for already-encoded file
     * bytes.
     *
     * @param data the image file bytes
     * @param columns number of text columns to fill
     * @param rows number of text rows to fill
     * @return the escape sequence to emit
     */
    static String inlineImage(final byte[] data, final int columns,
        final int rows) {

        String base64 = Base64.getEncoder().encodeToString(data);
        StringBuilder sb = new StringBuilder(base64.length() + 128);
        sb.append(OSC_START)
            .append("inline=1;size=").append(data.length)
            .append(";width=").append(columns)
            .append(";height=").append(rows)
            .append(";preserveAspectRatio=0;doNotMoveCursor=1:")
            .append(base64)
            .append(BEL);
        return sb.toString();
    }
}
