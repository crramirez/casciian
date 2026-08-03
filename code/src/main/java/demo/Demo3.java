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

import java.io.*;

/**
 * This class is the main driver for a simple demonstration of Casciian's
 * capabilities.  This one passes separate Reader/Writer to TApplication,
 * which will behave quite badly due to System.in/out not being in raw mode.
 */
public class Demo3 {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Demo3() {}

    // ------------------------------------------------------------------------
    // Demo3 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            DemoApplication app = new DemoApplication(System.in,
                new InputStreamReader(System.in, "IBM437"),
                new PrintWriter(new OutputStreamWriter(System.out, "IBM437")),
                true);
            // Run the application on the main thread, so that the JVM stays
            // alive until the application exits.
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
