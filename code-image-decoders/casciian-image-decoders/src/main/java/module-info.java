/*
 * Casciian Image Decoders add-on
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

/**
 * Casciian Image Decoders add-on.
 *
 * <p>Optional add-on that provides extra pure-Java
 * {@link casciian.image.decoders.ImageDecoder} implementations for
 * Casciian-based applications, on top of the Sixel decoder shipped with the
 * core library. It currently provides:</p>
 *
 * <ul>
 *   <li>{@link casciian.imagedecoders.decoders.BMP24ImageDecoder} — 24-bit
 *       uncompressed Windows Bitmap (BMP) files.</li>
 *   <li>{@link casciian.imagedecoders.decoders.XPMImageDecoder} — X PixMap
 *       (XPM) ASCII image files.</li>
 * </ul>
 *
 * <p>Because the decoders are implemented in pure Java (no {@code java.desktop}
 * dependency), this add-on remains compatible with a GraalVM native image.</p>
 */
module casciian.image.decoders {
    requires transitive casciian;

    exports casciian.imagedecoders.decoders;

    provides casciian.image.decoders.ImageDecoder
        with casciian.imagedecoders.decoders.BMP24ImageDecoder,
             casciian.imagedecoders.decoders.XPMImageDecoder;
}
