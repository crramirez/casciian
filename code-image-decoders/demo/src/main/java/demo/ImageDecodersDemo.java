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

/**
 * Main entry point for the Casciian Image Decoders add-on demo.
 *
 * <p>A minimal image-viewer TUI: it registers every available image decoder
 * (BMP and XPM from this add-on, plus the Sixel decoder shipped in core
 * casciian) and lets the user open any of those formats from
 * {@code Tool > Open image}.</p>
 *
 * <p>Run via the fat JAR produced by the {@code jarDemo} task:</p>
 * <pre>
 *     java -jar build/libs/casciian-image-decoders-demo-&lt;version&gt;.jar
 * </pre>
 */
public final class ImageDecodersDemo {

    private ImageDecodersDemo() {}

    /**
     * Launch the demo application.
     *
     * @param args command line arguments (ignored)
     */
    public static void main(final String[] args) {
        try {
            ImageDecodersDemoApplication app =
                    new ImageDecodersDemoApplication(TApplication.BackendType.XTERM);
            new Thread(app).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
