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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import casciian.bits.ImageRGB;
import casciian.image.encoders.ImageEncoder;
import casciian.image.encoders.PngImageEncoder;

/**
 * Encoder for the iTerm2 inline images protocol ({@code OSC 1337 File=}).
 *
 * <p>The pixels are packed into an image file by a pluggable
 * {@link ImageEncoder} (the built-in {@link PngImageEncoder} by default,
 * or e.g. JPEG/WebP from add-on modules), base64-encoded, and displayed at
 * the cursor stretched over a given number of text cells.
 * {@code doNotMoveCursor=1} keeps the cursor in place, so images on the
 * bottom row never scroll the screen.</p>
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
    private static final byte BEL = 7;

    /**
     * The built-in encoder used when an add-on encoder fails.
     */
    private static final PngImageEncoder FALLBACK_ENCODER =
        new PngImageEncoder();

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
     * the given number of text cells.  The cursor does not move.
     *
     * <p>If an add-on file encoder throws or returns no data, the image is
     * encoded with the built-in PNG encoder instead, keeping the selected
     * encoder's alpha behavior.  If no valid file can be produced, null is
     * returned so that the caller can draw something else rather than
     * send a broken image.</p>
     *
     * @param image the image
     * @param columns number of text columns to fill
     * @param rows number of text rows to fill
     * @return the escape sequence to emit, or null if the image could not
     * be encoded
     */
    String transmitAndPlace(final ImageRGB image, final int columns,
        final int rows) {

        byte[] data = encode(fileEncoder, image);
        if ((data == null) && (fileEncoder.getClass()
                != PngImageEncoder.class)) {
            data = encode(FALLBACK_ENCODER, image);
        }
        return (data == null) ? null : inlineImage(data, columns, rows);
    }

    /**
     * Encode an image file, treating exceptions and empty results as
     * failure.
     *
     * @param encoder the file encoder
     * @param image the image
     * @return the file bytes, or null on failure
     */
    private byte[] encode(final ImageEncoder encoder, final ImageRGB image) {
        try {
            byte[] data = encoder.encode(image, isAlpha());
            return ((data == null) || (data.length == 0)) ? null : data;
        } catch (IOException | RuntimeException e) {
            return null;
        }
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

        byte[] header = (OSC_START + "inline=1;size=" + data.length
            + ";width=" + columns + ";height=" + rows
            + ";preserveAspectRatio=0;doNotMoveCursor=1:")
            .getBytes(StandardCharsets.US_ASCII);
        int base64Length = 4 * ((data.length + 2) / 3);
        // Base64-encode straight into the final sequence, which the
        // String constructor copies once, instead of building and copying
        // an intermediate base64 String.
        byte[] sequence = new byte[header.length + base64Length + 1];
        System.arraycopy(header, 0, sequence, 0, header.length);
        ArrayOutput out = new ArrayOutput(sequence, header.length);
        try (OutputStream base64 = Base64.getEncoder().wrap(out)) {
            base64.write(data);
        } catch (IOException e) {
            // ArrayOutput never throws.
            throw new IllegalStateException(e);
        }
        sequence[sequence.length - 1] = BEL;
        return new String(sequence, StandardCharsets.ISO_8859_1);
    }

    /**
     * An output stream over a preallocated array.
     */
    private static final class ArrayOutput extends OutputStream {

        /**
         * The destination.
         */
        private final byte[] buffer;

        /**
         * The next index to write.
         */
        private int position;

        /**
         * Create a stream writing from an offset.
         *
         * @param buffer the destination
         * @param position the first index to write
         */
        ArrayOutput(final byte[] buffer, final int position) {
            this.buffer = buffer;
            this.position = position;
        }

        @Override
        public void write(final int b) {
            buffer[position++] = (byte) b;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            System.arraycopy(b, off, buffer, position, len);
            position += len;
        }
    }
}
