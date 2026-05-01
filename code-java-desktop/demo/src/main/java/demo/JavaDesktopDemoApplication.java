/*
 * Casciian Java Desktop add-on - demo
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

import casciian.TApplication;
import casciian.image.decoders.ImageDecoder;
import casciian.image.decoders.ImageDecoderRegistry;
import casciian.javadesktop.decoders.ImageIORGBDecoder;

import java.io.UnsupportedEncodingException;

/**
 * Demo TUI application showcasing the Casciian Java Desktop add-on.
 *
 * <p>The application registers an {@link ImageIORGBDecoder} so PNG and JPEG
 * files can be opened from File &gt; Open. This demonstrates how an
 * application running on a regular JVM (with full Java Desktop support) can
 * delegate image decoding to {@link javax.imageio.ImageIO} instead of the
 * pure-Java decoders bundled with core Casciian.</p>
 */
public final class JavaDesktopDemoApplication extends TApplication {

    /**
     * Public constructor.
     *
     * @param backendType the desired backend type
     * @throws UnsupportedEncodingException on backend errors
     */
    public JavaDesktopDemoApplication(final BackendType backendType) throws UnsupportedEncodingException {
        super(backendType);

        // Register the ImageIO-backed decoder so File > Open can handle PNG
        // and JPEG files. This is the whole point of the demo: it's the only
        // capability provided here that requires java.desktop.
        ImageDecoder decoder = new ImageIORGBDecoder();
        ImageDecoderRegistry.getInstance().registerDecoder(decoder);

        addToolMenu();
        addFileMenu();
        addWindowMenu();
        addHelpMenu();

        getBackend().setTitle("Casciian Java Desktop Add-on Demo");
    }
}
