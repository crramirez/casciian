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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for image decoder plugins.
 * Implementations can decode various image formats into ImageRGB objects.
 */
public interface ImageDecoder {

    /**
     * Decode an image from a file path.
     *
     * @param path the path to the image file
     * @return the decoded ImageRGB object
     * @throws IOException if an error occurs during decoding
     */
    ImageRGB decode(Path path) throws IOException;

    /**
     * Get the regex pattern that matches file extensions this decoder supports.
     * The pattern will be used to filter files in file open dialogs.
     *
     * @return a regex pattern string (e.g., "^.*\\.[sS][iI][xX]$" for .six files)
     */
    String getFileExtensionPattern();

    /**
     * Get a human-readable description of the supported format.
     *
     * @return a description string (e.g., "Sixel Image Files (*.six)")
     */
    String getFormatDescription();
}
