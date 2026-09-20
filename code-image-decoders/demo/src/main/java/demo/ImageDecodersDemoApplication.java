/*
 * Casciian Image Decoders add-on - demo
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

import java.io.UnsupportedEncodingException;

/**
 * Demo TUI application showcasing the Casciian Image Decoders add-on as a
 * simple image viewer.
 *
 * <p>The add-on registers its
 * {@link casciian.imagedecoders.decoders.BMP24ImageDecoder} and
 * {@link casciian.imagedecoders.decoders.XPMImageDecoder} as
 * {@link java.util.ServiceLoader} providers, and core casciian ships a
 * {@code SixelImageDecoder} provider. All of them are auto-discovered by
 * {@link TApplication}'s constructor via
 * {@link casciian.image.decoders.ImageDecoderRegistry#loadDecoders()}, so BMP,
 * XPM and Sixel files can all be opened from {@code Tool > Open image} without
 * any explicit registration here.</p>
 *
 * <p>Because these decoders are implemented in pure Java (no
 * {@code java.desktop}), the viewer stays compatible with a GraalVM native
 * image.</p>
 */
public final class ImageDecodersDemoApplication extends TApplication {

    /**
     * Public constructor.
     *
     * @param backendType the desired backend type
     * @throws UnsupportedEncodingException on backend errors
     */
    public ImageDecodersDemoApplication(final BackendType backendType) throws UnsupportedEncodingException {
        super(backendType);

        // The BMP, XPM and Sixel decoders are auto-discovered by the parent
        // constructor's ImageDecoderRegistry.loadDecoders() call, since the
        // casciian-image-decoders add-on and core casciian declare them as
        // ServiceLoader providers in their module-info.java and
        // META-INF/services files. "Tool > Open image" then offers all of
        // their file filters.

        addToolMenu();
        addFileMenu();
        addWindowMenu();
        addHelpMenu();

        getBackend().setTitle("Casciian Image Decoders Add-on Demo");
    }
}
