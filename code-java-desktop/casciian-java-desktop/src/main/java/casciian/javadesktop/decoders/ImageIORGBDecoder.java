/*
 * Casciian Java Desktop add-on
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
package casciian.javadesktop.decoders;

import casciian.bits.ImageRGB;
import casciian.bits.ArrayImageRGB;
import casciian.image.decoders.ImageDecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Image decoder that uses {@link javax.imageio.ImageIO} from the
 * {@code java.desktop} module to decode common raster image formats
 * (PNG and JPEG by default) into Casciian's {@link ImageRGB}.
 *
 * <p>This decoder is part of the optional Casciian Java Desktop add-on:
 * it is only available on JVMs that ship the {@code java.desktop} module.
 * Applications targeting GraalVM native image without {@code java.desktop}
 * support should use one of the pure-Java decoders bundled with the core
 * Casciian library instead.</p>
 */
public class ImageIORGBDecoder implements ImageDecoder {

    /**
     * Default file extension regex covering the PNG and JPEG file
     * extensions handled by this decoder.
     */
    private static final String DEFAULT_EXTENSION_PATTERN =
            "^.*\\.([pP][nN][gG]|[jJ][pP][gG]|[jJ][pP][eE][gG])$";

    /**
     * Default human-readable description for the formats handled by this
     * decoder.
     */
    private static final String DEFAULT_FORMAT_DESCRIPTION =
            "PNG / JPEG Image Files (*.png, *.jpg, *.jpeg)";

    /**
     * Regex pattern matching file extensions this decoder should handle.
     */
    private final String extensionPattern;

    /**
     * Human-readable description of the formats handled by this decoder.
     */
    private final String formatDescription;

    /**
     * Default constructor.  Configures the decoder to handle PNG, JPG and
     * JPEG files.
     */
    public ImageIORGBDecoder() {
        this(DEFAULT_EXTENSION_PATTERN, DEFAULT_FORMAT_DESCRIPTION);
    }

    /**
     * Public constructor allowing callers to override the file extension
     * regex and human-readable description, e.g. to register the decoder
     * for additional ImageIO-supported formats such as BMP or GIF.
     *
     * @param extensionPattern  regex matching file names this decoder should
     *                          handle
     * @param formatDescription human-readable description of the formats
     */
    public ImageIORGBDecoder(final String extensionPattern,
                             final String formatDescription) {
        if (extensionPattern == null || extensionPattern.isEmpty()) {
            throw new IllegalArgumentException("extensionPattern must not be null or empty");
        }
        if (formatDescription == null || formatDescription.isEmpty()) {
            throw new IllegalArgumentException("formatDescription must not be null or empty");
        }
        this.extensionPattern = extensionPattern;
        this.formatDescription = formatDescription;
    }

    @Override
    public ImageRGB decode(final Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        BufferedImage buffered = ImageIO.read(path.toFile());
        if (buffered == null) {
            throw new IOException("ImageIO could not decode file: " + path);
        }

        int width = buffered.getWidth();
        int height = buffered.getHeight();

        // Read the entire image into a single int[] in TYPE_INT_ARGB layout
        // and then bulk-copy it into the ImageRGB. This is more efficient
        // than per-pixel BufferedImage#getRGB calls.
        int[] pixels = buffered.getRGB(0, 0, width, height, null, 0, width);

        ImageRGB image = new ArrayImageRGB(width, height);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    @Override
    public String getFileExtensionPattern() {
        return extensionPattern;
    }

    @Override
    public String getFormatDescription() {
        return formatDescription;
    }
}
