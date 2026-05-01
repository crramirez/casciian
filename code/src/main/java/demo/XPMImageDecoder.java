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
    private static final Pattern PIXEL_PATTERN = Pattern.compile("\"(.+?)\"");

    /**
     * Public constructor.
     */
    public XPMImageDecoder() {
        // Explicit no arg constructor
    }

    @Override
    public ImageRGB decode(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            int[] header = readHeader(reader);
            int width = header[0];
            int height = header[1];
            int ncolors = header[2];
            int charsPerPixel = header[3];

            Map<String, Integer> colorMap = readColorMap(reader, ncolors,
                charsPerPixel);

            return readPixelData(reader, width, height, charsPerPixel,
                colorMap);
        }
    }

    /**
     * Read and parse the XPM header values line.
     *
     * @param reader the buffered reader
     * @return array of [width, height, ncolors, charsPerPixel]
     * @throws IOException if the header is not found or invalid
     */
    private int[] readHeader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = VALUES_PATTERN.matcher(line);
            if (matcher.find()) {
                return new int[] {
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4))
                };
            }
        }
        throw new IOException("Invalid XPM format: values line not found");
    }

    /**
     * Read the color map section of the XPM file.
     *
     * @param reader the buffered reader
     * @param nColors number of colors to read
     * @param charsPerPixel characters per pixel key
     * @return map from pixel key to RGB color value
     * @throws IOException if the file ends unexpectedly
     */
    private Map<String, Integer> readColorMap(BufferedReader reader,
        int nColors, int charsPerPixel) throws IOException {

        Map<String, Integer> colorMap = new HashMap<>();
        for (int i = 0; i < nColors; i++) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException(
                    "Unexpected end of file while reading colors");
            }
            parseColorEntry(line, charsPerPixel, colorMap);
        }
        return colorMap;
    }

    /**
     * Parse a single color entry line and add it to the color map.
     *
     * @param line the line to parse
     * @param charsPerPixel characters per pixel key
     * @param colorMap the map to add the parsed entry to
     */
    private void parseColorEntry(String line, int charsPerPixel,
        Map<String, Integer> colorMap) {

        Matcher matcher = PIXEL_PATTERN.matcher(line);
        if (!matcher.find()) {
            return;
        }
        String content = matcher.group(1);
        String key = content.substring(0, charsPerPixel);
        String colorPart = content.substring(charsPerPixel).trim();

        int colorStart = colorPart.indexOf("c ");
        if (colorStart == -1) {
            colorStart = colorPart.indexOf("c\t");
        }
        if (colorStart != -1) {
            String colorValue = colorPart.substring(colorStart + 2).trim();
            colorMap.put(key, parseColor(colorValue));
        }
    }

    /**
     * Read pixel data rows and build the image.
     *
     * @param reader the buffered reader
     * @param width image width
     * @param height image height
     * @param charsPerPixel characters per pixel key
     * @param colorMap map from pixel key to RGB color value
     * @return the decoded image
     * @throws IOException if the file ends unexpectedly or pixel data is invalid
     */
    private ImageRGB readPixelData(BufferedReader reader, int width,
        int height, int charsPerPixel,
        Map<String, Integer> colorMap) throws IOException {

        ImageRGB image = new ImageRGB(width, height);
        for (int y = 0; y < height; y++) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException(
                    "Unexpected end of file while reading pixels");
            }
            decodePixelRow(line, y, width, charsPerPixel, colorMap, image);
        }
        return image;
    }

    /**
     * Decode a single row of pixel data.
     *
     * @param line the line to decode
     * @param y the row index
     * @param width image width
     * @param charsPerPixel characters per pixel key
     * @param colorMap map from pixel key to RGB color value
     * @param image the image to write pixels to
     * @throws IOException if pixel data is invalid
     */
    private void decodePixelRow(String line, int y, int width,
        int charsPerPixel, Map<String, Integer> colorMap,
        ImageRGB image) throws IOException {

        Matcher matcher = PIXEL_PATTERN.matcher(line);
        if (!matcher.find()) {
            return;
        }
        String pixelLine = matcher.group(1);
        for (int x = 0; x < width; x++) {
            int start = x * charsPerPixel;
            int end = start + charsPerPixel;
            if (end > pixelLine.length()) {
                throw new IOException("Invalid pixel data at row " + y);
            }
            String key = pixelLine.substring(start, end);
            Integer rgb = colorMap.getOrDefault(key, 0x000000);
            image.setRGB(x, y, rgb);
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
