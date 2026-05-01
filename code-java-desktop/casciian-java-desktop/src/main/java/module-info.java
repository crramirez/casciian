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

/**
 * Casciian Java Desktop add-on.
 *
 * <p>Optional add-on that enables {@code java.desktop} capabilities for
 * Casciian-based applications. The add-on provides image decoders backed by
 * {@link javax.imageio.ImageIO} and other integrations that require a JDK
 * with full Java Desktop support.</p>
 *
 * <p>Use this module when you want to leverage Java Desktop on a regular JVM
 * and you are not building a GraalVM native image.</p>
 */
module casciian.java.desktop {
    requires transitive casciian;
    requires java.desktop;

    exports casciian.javadesktop.decoders;

    provides casciian.image.decoders.ImageDecoder
        with casciian.javadesktop.decoders.ImageIORGBDecoder;
}
