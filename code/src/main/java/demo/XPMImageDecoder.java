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
package demo;

import casciian.bits.ImageRGB;
import casciian.image.decoders.ImageDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Image decoder for XPM (X PixMap) format files.
 * XPM is an ASCII-based image format used in X Window System.
 */
public class XPMImageDecoder implements ImageDecoder {

    private static final Pattern VALUES_PATTERN = Pattern.compile("\"(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+).*\"");
    private static final Pattern COLOR_PATTERN = Pattern.compile("\"(.+?)\\s+c\\s+(.+?)\"");
    private static final Pattern PIXEL_PATTERN = Pattern.compile("\"(.+?)\"");

    @Override
    public ImageRGB decode(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;

            // Skip until we find the values line (width height ncolors charsPerPixel)
            int width = 0, height = 0, ncolors = 0, charsPerPixel = 0;
            boolean foundValues = false;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = VALUES_PATTERN.matcher(line);
                if (matcher.find()) {
                    width = Integer.parseInt(matcher.group(1));
                    height = Integer.parseInt(matcher.group(2));
                    ncolors = Integer.parseInt(matcher.group(3));
                    charsPerPixel = Integer.parseInt(matcher.group(4));
                    foundValues = true;
                    break;
                }
            }

            if (!foundValues) {
                throw new IOException("Invalid XPM format: values line not found");
            }

            // Read color map
            Map<String, Integer> colorMap = new HashMap<>();
            for (int i = 0; i < ncolors; i++) {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("Unexpected end of file while reading colors");
                }

                Matcher matcher = COLOR_PATTERN.matcher(line);
                if (matcher.find()) {
                    String key = matcher.group(1);
                    String colorValue = matcher.group(2).trim();
                    int rgb = parseColor(colorValue);
                    colorMap.put(key, rgb);
                }
            }

            // Read pixel data
            ImageRGB image = new ImageRGB(width, height);
            for (int y = 0; y < height; y++) {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("Unexpected end of file while reading pixels");
                }

                Matcher matcher = PIXEL_PATTERN.matcher(line);
                if (matcher.find()) {
                    String pixelLine = matcher.group(1);
                    for (int x = 0; x < width; x++) {
                        int start = x * charsPerPixel;
                        int end = start + charsPerPixel;
                        if (end > pixelLine.length()) {
                            throw new IOException("Invalid pixel data at row " + y);
                        }
                        String key = pixelLine.substring(start, end);
                        Integer rgb = colorMap.get(key);
                        if (rgb == null) {
                            rgb = 0x000000; // Default to black if color not found
                        }
                        image.setRGB(x, y, rgb);
                    }
                }
            }

            return image;
        }
    }

    /**
     * Parse color value from XPM format.
     * Supports hex colors (#RRGGBB), named colors (basic set), and "None".
     */
    private int parseColor(String colorValue) {
        if (colorValue.equalsIgnoreCase("None")) {
            return 0x000000; // Treat transparent as black
        }

        if (colorValue.startsWith("#")) {
            try {
                String hex = colorValue.substring(1);
                // Handle both #RGB and #RRGGBB formats
                if (hex.length() == 3) {
                    int r = Integer.parseInt(hex.substring(0, 1), 16) * 17;
                    int g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
                    int b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
                    return (r << 16) | (g << 8) | b;
                } else if (hex.length() == 6) {
                    return Integer.parseInt(hex, 16);
                } else if (hex.length() == 12) {
                    // #RRRRGGGGBBBB format, take high byte of each component
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(4, 6), 16);
                    int b = Integer.parseInt(hex.substring(8, 10), 16);
                    return (r << 16) | (g << 8) | b;
                }
            } catch (NumberFormatException e) {
                return 0x000000;
            }
        }

        // Basic named colors
        return getNamedColor(colorValue.toLowerCase());
    }

    /**
     * Get RGB value for basic named colors.
     */
    private int getNamedColor(String name) {
        return switch (name) {
            case "black" -> 0x000000;
            case "white" -> 0xFFFFFF;
            case "red" -> 0xFF0000;
            case "green" -> 0x00FF00;
            case "blue" -> 0x0000FF;
            case "yellow" -> 0xFFFF00;
            case "cyan" -> 0x00FFFF;
            case "magenta" -> 0xFF00FF;
            case "gray", "grey" -> 0x808080;
            default -> 0x000000; // Default to black
        };
    }

    @Override
    public String getFileExtensionPattern() {
        return "^.*\\.[xX][pP][mM]$";
    }

    @Override
    public String getFormatDescription() {
        return "XPM Image Files (*.xpm)";
    }
}
