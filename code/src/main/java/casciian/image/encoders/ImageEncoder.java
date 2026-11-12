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
package casciian.image.encoders;

import java.io.IOException;

import casciian.bits.ImageRGB;

/**
 * Interface for image file encoder plugins.  Implementations convert an
 * {@link ImageRGB} into the bytes of an image file format (BMP, PNG,
 * JPEG, ...).  They are used, for example, to send images to terminals
 * that understand the iTerm2 inline images protocol.
 *
 * <p>Implementations are discovered by {@link ImageEncoderRegistry} via
 * {@link java.util.ServiceLoader}.  Add-on modules declare them with
 * {@code provides casciian.image.encoders.ImageEncoder with ...} in their
 * {@code module-info.java}, or in
 * {@code META-INF/services/casciian.image.encoders.ImageEncoder}.</p>
 *
 * <p>Pixels are ARGB, as returned by {@link ImageRGB#getRGB(int, int)}.</p>
 */
public interface ImageEncoder {

    /**
     * Get the short name of the format, used to select this encoder by
     * configuration, e.g. "bmp" or "png".
     *
     * @return the format name, never {@code null}
     */
    String getName();

    /**
     * Get the MIME type of the produced bytes, e.g. "image/png".
     *
     * @return the MIME type, never {@code null}
     */
    String getMimeType();

    /**
     * Get the priority of this encoder.  When no encoder is explicitly
     * configured, the registered encoder with the highest priority is
     * used.  The built-in BMP encoder has priority 0, so any add-on encoder
     * with a positive priority is preferred over it.
     *
     * @return the priority, higher is preferred
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Whether this encoder can preserve the alpha channel.
     *
     * @return true if {@link #encode(ImageRGB, boolean)} honors alpha
     */
    default boolean supportsAlpha() {
        return false;
    }

    /**
     * Encode an image.
     *
     * @param image the image to encode
     * @param alpha if true, keep the alpha channel if the format supports
     *              it; if false, the image is treated as opaque
     * @return the encoded file bytes
     * @throws IOException if the image cannot be encoded
     */
    byte[] encode(ImageRGB image, boolean alpha) throws IOException;
}
