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
package casciian.javadesktop.encoders;

import casciian.bits.ImageRGB;
import casciian.image.encoders.ImageEncoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Lossy JPEG encoder backed by {@link javax.imageio.ImageIO}.  JPEG has no
 * alpha channel, so images are always encoded opaque.
 *
 * <p>JPEG is several times smaller than PNG for photographs, but blurs
 * sharp edges such as text, and is larger than PNG for flat UI content.
 * {@link ImageIOAdaptiveEncoder}, the preferred encoder, uses this encoder
 * only for photo-like images.  To always use JPEG, select it by name, e.g.
 * {@code casciian.ECMA48.iterm2ImageEncoder=jpeg}.</p>
 *
 * <p>A new {@link ImageWriter} is created for every call, so instances are
 * safe to use from several threads.  Output is buffered in memory; ImageIO's
 * disk cache is never used.</p>
 */
public class ImageIOJpegEncoder implements ImageEncoder {

    /**
     * The default compression quality.
     */
    public static final float DEFAULT_QUALITY = 0.85f;

    /**
     * The priority: below the built-in PNG encoder (10), so JPEG is only
     * used when selected by name or by {@link ImageIOAdaptiveEncoder}.
     */
    public static final int PRIORITY = 5;

    /**
     * The compression quality in [0, 1].
     */
    private final float quality;

    /**
     * The JPEG writer provider, looked up once.
     */
    private final ImageWriterSpi writerSpi;

    /**
     * Create an encoder with {@link #DEFAULT_QUALITY}.
     */
    public ImageIOJpegEncoder() {
        this(DEFAULT_QUALITY);
    }

    /**
     * Create an encoder with the given quality.
     *
     * @param quality the compression quality in [0, 1], where 1 is the
     * best quality and largest file
     * @throws IllegalArgumentException if quality is outside [0, 1]
     * @throws IllegalStateException if ImageIO has no JPEG writer
     */
    public ImageIOJpegEncoder(final float quality) {
        if (!(quality >= 0.0f && quality <= 1.0f)) {
            throw new IllegalArgumentException(
                "quality must be in [0, 1]: " + quality);
        }
        Iterator<ImageWriter> writers =
            ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("ImageIO has no JPEG writer");
        }
        ImageWriter writer = writers.next();
        ImageWriterSpi spi = writer.getOriginatingProvider();
        writer.dispose();
        if (spi == null) {
            throw new IllegalStateException("ImageIO JPEG writer has no "
                + "provider");
        }
        this.writerSpi = spi;
        this.quality = quality;
    }

    /**
     * Get the compression quality.
     *
     * @return the quality in [0, 1]
     */
    public float getQuality() {
        return quality;
    }

    @Override
    public String getName() {
        return "jpeg";
    }

    @Override
    public String getMimeType() {
        return "image/jpeg";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public byte[] encode(final ImageRGB image, final boolean alpha)
        throws IOException {

        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image is empty: "
                + width + "x" + height);
        }
        BufferedImage buffered = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_RGB);
        int[] dest = ((DataBufferInt) buffered.getRaster().getDataBuffer())
            .getData();
        // TYPE_INT_RGB ignores the top byte, so ARGB pixels copy as-is.
        image.getRGB(0, 0, width, height, dest, 0, width);

        ImageWriter writer = writerSpi.createWriterInstance();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(
            Math.max(1024, width * height / 4));
        try (ImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(buffered, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
