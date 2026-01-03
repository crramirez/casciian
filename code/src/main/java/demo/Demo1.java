/*
 * Casciian - Java Text User Interface
 *
 * Original work written 2013–2025 by Autumn Lamonte
 * and dedicated to the public domain via CC0.
 *
 * Modifications and maintenance:
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
import casciian.backend.SystemProperties;

import java.util.stream.Stream;

/**
 * This class is the main driver for a simple demonstration of Casciian's
 * capabilities.
 */
public final class Demo1 {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    private Demo1() {}

    // ------------------------------------------------------------------------
    // Demo1 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            boolean defaults;
            if (Stream.of(args).noneMatch(arg -> arg.equals("--defaults"))) {
                SystemProperties.setAnimations(true);
                SystemProperties.setTextMouse(true);
                SystemProperties.setTranslucence(true);
                SystemProperties.setMenuIcons(true);

                defaults = false;
            } else {
                defaults = true;
            }

            DemoApplication app;
            app = new DemoApplication(TApplication.BackendType.XTERM, defaults);
            (new Thread(app)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
