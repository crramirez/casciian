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

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.Deflater;

import casciian.bits.ImageRGB;

/**
 * Encoder for the kitty terminal graphics protocol.
 *
 * <p>Images are sent as raw 24-bit RGB ({@code f=24}) or 32-bit RGBA
 * ({@code f=32}) pixel data, optionally zlib-compressed ({@code o=z}), and
 * split into base64 chunks of at most {@value #CHUNK_SIZE} bytes as the
 * protocol requires.  Every command asks the terminal to stay quiet
 * ({@code q=2}) so that no responses pollute the input stream; only the
 * support query expects an answer.</p>
 *
 * <p>Image ids are unsigned 32-bit integers carried in a Java {@code int}.</p>
 *
 * @see <a href="https://sw.kovidgoyal.net/kitty/graphics-protocol/">kitty
 * graphics protocol</a>
 */
final class KittyImageEncoder {

    /**
     * Pixel format for transmitted image data.
     */
    enum Format {
        /** 24-bit RGB, 3 bytes per pixel.  Alpha is discarded. */
        RGB(24, 3),

        /** 32-bit RGBA, 4 bytes per pixel. */
        RGBA(32, 4);

        /** Value of the protocol's {@code f} key. */
        private final int protocolValue;

        /** Bytes per pixel. */
        private final int bytesPerPixel;

        Format(final int protocolValue, final int bytesPerPixel) {
            this.protocolValue = protocolValue;
            this.bytesPerPixel = bytesPerPixel;
        }

        /**
         * Parse a format name, case-insensitively.
         *
         * @param name "rgb" or "rgba"
         * @param defaultFormat the format to use for anything else
         * @return the format
         */
        static Format parse(final String name, final Format defaultFormat) {
            if (name == null) {
                return defaultFormat;
            }
            return switch (name.trim().toLowerCase()) {
                case "rgb" -> RGB;
                case "rgba" -> RGBA;
                default -> defaultFormat;
            };
        }
    }

    /**
     * Maximum number of base64 bytes in a single escape sequence.
     */
    static final int CHUNK_SIZE = 4096;

    /**
     * Image id used by the support query.
     */
    static final int QUERY_IMAGE_ID = 31;

    /**
     * Start of an APC graphics command.
     */
    private static final String APC_START = "\033_G";

    /**
     * String terminator.
     */
    private static final String ST = "\033\\";

    /**
     * The pixel format to transmit.
     */
    private final Format format;

    /**
     * If true, zlib-compress pixel data.
     */
    private final boolean compress;

    /**
     * Public constructor.
     *
     * @param format the pixel format to transmit
     * @param compress if true, zlib-compress the pixel data
     */
    KittyImageEncoder(final Format format, final boolean compress) {
        this.format = format;
        this.compress = compress;
    }

    /**
     * Get the pixel format.
     *
     * @return the format
     */
    Format getFormat() {
        return format;
    }

    /**
     * Whether pixel data is zlib-compressed.
     *
     * @return true if compressing
     */
    boolean isCompressed() {
        return compress;
    }

    /**
     * Transmit an image and display it at the cursor, scaled to fill the
     * given number of text cells.  The cursor does not move.
     *
     * @param image the image
     * @param imageId the image id, non-zero
     * @param placementId the placement id, non-zero
     * @param columns number of text columns to fill
     * @param rows number of text rows to fill
     * @return the escape sequence(s) to emit
     */
    String transmitAndPlace(final ImageRGB image, final int imageId,
        final int placementId, final int columns, final int rows) {

        String control = "a=T,f=" + format.protocolValue
            + ",s=" + image.getWidth() + ",v=" + image.getHeight()
            + (compress ? ",o=z" : "")
            + ",i=" + Integer.toUnsignedString(imageId)
            + ",p=" + Integer.toUnsignedString(placementId)
            + ",c=" + columns + ",r=" + rows + ",C=1,q=2";
        return chunked(control, encodePixels(image));
    }

    /**
     * Display an image previously transmitted, at the cursor, scaled to
     * fill the given number of text cells.  The cursor does not move.
     *
     * @param imageId the image id
     * @param placementId the placement id, non-zero
     * @param columns number of text columns to fill
     * @param rows number of text rows to fill
     * @return the escape sequence to emit
     */
    static String place(final int imageId, final int placementId,
        final int columns, final int rows) {

        return APC_START + "a=p,i=" + Integer.toUnsignedString(imageId)
            + ",p=" + Integer.toUnsignedString(placementId)
            + ",c=" + columns + ",r=" + rows + ",C=1,q=2" + ST;
    }

    /**
     * Remove one placement of an image, keeping the image data so it can
     * be placed again later.
     *
     * @param imageId the image id
     * @param placementId the placement id
     * @return the escape sequence to emit
     */
    static String deletePlacement(final int imageId, final int placementId) {
        return APC_START + "a=d,d=i,i=" + Integer.toUnsignedString(imageId)
            + ",p=" + Integer.toUnsignedString(placementId) + ",q=2" + ST;
    }

    /**
     * Remove all placements of an image and free its data.
     *
     * @param imageId the image id
     * @return the escape sequence to emit
     */
    static String deleteImage(final int imageId) {
        return APC_START + "a=d,d=I,i=" + Integer.toUnsignedString(imageId)
            + ",q=2" + ST;
    }

    /**
     * Ask the terminal whether it supports the graphics protocol.  A
     * supporting terminal answers with an APC reply carrying
     * {@link #QUERY_IMAGE_ID}; others ignore it.
     *
     * @return the escape sequence to emit
     */
    static String query() {
        return APC_START + "i=" + QUERY_IMAGE_ID
            + ",s=1,v=1,a=q,t=d,f=24;AAAA" + ST;
    }

    /**
     * Check whether the body of an APC response (everything between
     * {@code ESC _} and {@code ESC \}) is a successful reply to
     * {@link #query()}.
     *
     * @param apc the APC body, e.g. "Gi=31;OK"
     * @return true if the terminal reported success
     */
    static boolean isQueryOk(final String apc) {
        if ((apc == null) || !apc.startsWith("G")) {
            return false;
        }
        int semicolon = apc.indexOf(';');
        if (semicolon < 0) {
            return false;
        }
        boolean idMatches = false;
        for (String key : apc.substring(1, semicolon).split(",")) {
            if (key.equals("i=" + QUERY_IMAGE_ID)) {
                idMatches = true;
                break;
            }
        }
        return idMatches && apc.substring(semicolon + 1).equals("OK");
    }

    /**
     * Number of bytes a terminal stores for an image: the protocol keeps
     * decoded images as RGBA regardless of the transmitted format.
     *
     * @param image the image
     * @return the approximate terminal-side storage cost
     */
    static long storageBytes(final ImageRGB image) {
        return (long) image.getWidth() * image.getHeight() * 4;
    }

    /**
     * Convert an image to the transmitted pixel bytes, compressing them if
     * requested.
     *
     * @param image the image
     * @return the bytes to base64-encode
     */
    byte[] encodePixels(final ImageRGB image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        int bpp = format.bytesPerPixel;
        byte[] bytes = new byte[argb.length * bpp];
        int i = 0;
        for (int pixel : argb) {
            bytes[i++] = (byte) (pixel >>> 16);
            bytes[i++] = (byte) (pixel >>> 8);
            bytes[i++] = (byte) pixel;
            if (bpp == 4) {
                bytes[i++] = (byte) (pixel >>> 24);
            }
        }
        if (!compress) {
            return bytes;
        }
        return zlib(bytes);
    }

    /**
     * Compress bytes in the zlib (RFC 1950) format.
     *
     * @param bytes the raw bytes
     * @return the compressed bytes
     */
    private static byte[] zlib(final byte[] bytes) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try {
            deflater.setInput(bytes);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(
                Math.max(64, bytes.length / 2));
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Wrap a payload in one or more APC commands, splitting it into chunks
     * as the protocol requires.  The control keys go on the first chunk
     * only.
     *
     * @param control the control keys
     * @param payload the raw payload bytes
     * @return the escape sequence(s) to emit
     */
    private static String chunked(final String control, final byte[] payload) {
        String base64 = Base64.getEncoder().encodeToString(payload);
        int length = base64.length();
        StringBuilder sb = new StringBuilder(length
            + ((length / CHUNK_SIZE) + 1) * 16 + control.length());
        if (length <= CHUNK_SIZE) {
            sb.append(APC_START).append(control).append(';')
                .append(base64).append(ST);
            return sb.toString();
        }
        for (int start = 0; start < length; start += CHUNK_SIZE) {
            int end = Math.min(length, start + CHUNK_SIZE);
            boolean more = end < length;
            sb.append(APC_START);
            if (start == 0) {
                sb.append(control).append(',');
            }
            sb.append(more ? "m=1" : "m=0");
            if (start != 0) {
                sb.append(",q=2");
            }
            sb.append(';').append(base64, start, end).append(ST);
        }
        return sb.toString();
    }
}
