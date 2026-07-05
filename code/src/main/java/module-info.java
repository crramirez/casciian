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

/**
 * Casciian - Java Text User Interface library
 *
 * <p>
 * This library is a text-based windowing system loosely reminiscent of
 * Borland's <a href="http://en.wikipedia.org/wiki/Turbo_Vision">Turbo
 * Vision</a> library.  Casciian's goal is to enable people to get up and
 * running with minimum hassle and lots of polish.
 * </p>
 */
module casciian {
    requires java.base;
    requires transitive java.xml;
    requires org.jline.terminal;
    requires org.jline.terminal.jni;

    exports casciian;
    exports casciian.backend;
    exports casciian.backend.terminal;
    exports casciian.bits;
    exports casciian.effect;
    exports casciian.event;
    exports casciian.help;
    exports casciian.image.decoders;
    exports casciian.io;
    exports casciian.layout;
    exports casciian.menu;
    exports casciian.net;
    exports casciian.texteditor;

    uses casciian.image.decoders.ImageDecoder;

    provides casciian.image.decoders.ImageDecoder
        with casciian.image.decoders.SixelImageDecoder;
}
