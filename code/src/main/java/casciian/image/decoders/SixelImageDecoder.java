/*
 * Casciian - Java Text User Interface
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
package casciian.image.decoders;

import casciian.bits.ImageRGB;
import casciian.terminal.SixelDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Image decoder for Sixel format files.
 */
public class SixelImageDecoder implements ImageDecoder {

    @Override
    public ImageRGB decode(Path path) throws IOException {
        String content = Files.readString(path);
        SixelDecoder decoder = new SixelDecoder(content, null, 0xFFFFFF, false);
        return decoder.getImage();
    }

    @Override
    public String getFileExtensionPattern() {
        return "^.*\\.[sS][iI][xX]$";
    }

    @Override
    public String getFormatDescription() {
        return "Sixel Image Files (*.six)";
    }
}
