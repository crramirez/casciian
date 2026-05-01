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

import java.io.UnsupportedEncodingException;

/**
 * Demo TUI application showcasing the Casciian Java Desktop add-on.
 *
 * <p>The Java Desktop add-on registers an
 * {@link casciian.javadesktop.decoders.ImageIORGBDecoder} as a
 * {@link java.util.ServiceLoader} provider, so PNG and JPEG files can be
 * opened from File &gt; Open without any explicit registration here:
 * {@link TApplication}'s constructor auto-discovers it via
 * {@link casciian.image.decoders.ImageDecoderRegistry#loadDecoders()}.</p>
 *
 * <p>This demonstrates how an application running on a regular JVM (with
 * full Java Desktop support) can delegate image decoding to
 * {@link javax.imageio.ImageIO} instead of the pure-Java decoders bundled
 * with core Casciian, simply by putting the add-on on the classpath /
 * module path.</p>
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

        // ImageIORGBDecoder is auto-discovered by the parent constructor's
        // ImageDecoderRegistry.loadDecoders() call, since the
        // casciian-java-desktop add-on declares it as a ServiceLoader
        // provider in its module-info.java and META-INF/services file.

        addToolMenu();
        addFileMenu();
        addWindowMenu();
        addHelpMenu();

        getBackend().setTitle("Casciian Java Desktop Add-on Demo");
    }
}
