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

package casciian.backend;

import casciian.bits.ImageRGB;
import casciian.bits.MathUtils;
import casciian.bits.Rgb;
import casciian.terminal.SixelDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HQSixelEncoder turns a ImageRGB into String of sixel image data,
 * using several strategies to produce a reasonably high quality image within
 * sixel's ~19.97 bit (101^3) color depth.
 *
 * <p>
 * Portions of this encoder were inspired / influenced by Hans Petter
 * Jansson's chafa project: https://hpjansson.org/chafa/ .  Please refer to
 * chafa's high-performance sixel encoder for far more advanced
 * implementations of principal component analysis color mapping, and sixel
 * row encoding.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe for concurrent encoding operations. Multiple
 * threads can safely call {@link #toSixel(ImageRGB)} concurrently on the
 * same encoder instance. Each encoding operation creates its own internal
 * Palette, ensuring no shared mutable state between concurrent calls.
 * </p>
 * <p>
 * Configuration methods such as {@link #setPaletteSize(int)} and
 * {@link #reloadOptions()} use volatile fields to ensure visibility of
 * changes across threads. However, for best results, configure the encoder
 * before beginning concurrent encoding operations.
 * </p>
 */
public class HQSixelEncoder implements SixelEncoder {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available custom palettes to use in HQSixelEncoder.
     */
    private enum CustomSixelPalette {
        NONE,
        VT340,
        CGA,
    }

    /**
     * Alpha value (0 - 255) above which to consider the pixel opaque.
     */
    private static final int ALPHA_OPAQUE = 102;        // ~40%

    /**
     * When fastAndDirty is set, the effective palette size for non-indexed
     * images.
     */
    private static final int FAST_AND_DIRTY = 64;

    /**
     * When run from the command line, we need both the image, and to know if
     * the image is transparent in order to set to correct sixel introducer.
     * So toSixel() returns a tuple now.
     */
    private class SixelResult {
        /**
         * The encoded image.
         */
        public String encodedImage;

        /**
         * If true, this image has transparent pixels.
         */
        public boolean transparent = false;

        /**
         * The palette used by this image.
         */
        public Palette palette;
    }

    /**
     * Palette is used to manage the conversion of images between 24-bit RGB
     * color and a palette of paletteSize colors.
     */
    private class Palette {

        /**
         * Timings records time points in the image generation cycle.
         */
        private class Timings {
            /**
             * Nanotime when the timings were begun.
             */
            public long startTime;

            /**
             * Nanotime after the image was scanned for color analysis.
             */
            public long scanImageTime;

            /**
             * Nanotime after the color map was produced.
             */
            public long buildColorMapTime;

            /**
             * Nanotime after which the RGB image was dithered into an
             * indexed image.
             */
            public long ditherImageTime;

            /**
             * Nanotime after which the dithered image was converted to sixel
             * and emitted.
             */
            public long emitSixelTime;

            /**
             * Nanotime when the timings were finished.
             */
            public long endTime;
        }

        /**
         * ColorIdx records a RGB color and its palette index.
         */
        private class ColorIdx {

            /**
             * The ~19.97-bit RGB color.  Each component has a value between
             * 0 and 100.
             */
            public int color;

            /**
             * The population count for this color.
             */
            public int count = 0;

            /**
             * The palette index for this color, only used for directMap().
             */
            public int directMapIndex = 0;

            /**
             * Public constructor.
             *
             * @param color the ~19.97-bit sixel color
             */
            public ColorIdx(final int color, final int index) {
                this.color = color;
                this.count = 0;
            }

            /**
             * Public constructor.  Count is set to 1, index to -1.
             *
             * @param color the ~19.97-bit sixel color
             */
            public ColorIdx(final int color) {
                this.color = color;
                this.count = 1;
            }

            /**
             * Hash only on color.
             *
             * @return the hash
             */
            @Override
            public int hashCode() {
                return color;
            }

            /**
             * Generate a human-readable string for this entry.
             *
             * @return a human-readable string
             */
            @Override
            public String toString() {
                return String.format("color %06x count %d", color, count);
            }
        }

        /**
         * A bucket contains colors that will all be mapped to the same
         * weighted average color value.
         */
        private class Bucket {

            /**
             * The colors in this bucket.
             */
            private List<ColorIdx> colors;

            /**
             * The palette index for this bucket.  For now this points to a
             * simple average of all the colors, or black if no colors are in
             * this bucket.
             */
            public int index = 0;

            // The minimum and maximum, and "total" component values in this
            // bucket.
            private int minRed = 0xFF;
            private int maxRed = 0;
            private int minGreen = 0xFF;
            private int maxGreen = 0;
            private int minBlue = 0xFF;
            private int maxBlue = 0;

            // The last computed average() value.
            private int lastAverage = -1;

            /**
             * Public constructor.
             *
             * @param n the expected number of colors that will be in this
             *          bucket
             */
            public Bucket(final int n) {
                reset(n);
            }

            /**
             * Reset the stats.
             *
             * @param n the expected number of colors that will be in this
             *          bucket
             */
            private void reset(final int n) {
                colors = new ArrayList<>(n);
                minRed = 0xFF;
                maxRed = 0;
                minGreen = 0xFF;
                maxGreen = 0;
                minBlue = 0xFF;
                maxBlue = 0;
                lastAverage = -1;
                index = 0;
            }

            /**
             * Get the index associated with all of the colors in this
             * bucket.
             *
             * @return the index
             */
            public int getIndex() {
                return index;
            }

            /**
             * Add a color to the bucket.
             *
             * @param color the color to add
             */
            public void add(final ColorIdx color) {
                colors.add(color);
                updateMinMaxBounds(color.color);
            }

            /**
             * Update min/max bounds for all color channels.
             *
             * @param rgb the color to update bounds with
             */
            private void updateMinMaxBounds(final int rgb) {
                int red = Rgb.getRed(rgb);
                int green = Rgb.getGreen(rgb);
                int blue = Rgb.getBlue(rgb);

                minRed = Math.min(minRed, red);
                maxRed = Math.max(maxRed, red);
                minGreen = Math.min(minGreen, green);
                maxGreen = Math.max(maxGreen, green);
                minBlue = Math.min(minBlue, blue);
                maxBlue = Math.max(maxBlue, blue);
            }

            /**
             * Partition this bucket into two buckets, split along the color
             * with the maximum range.
             *
             * @return the other bucket
             */
            public Bucket partition() {
                ColorChannel splitChannel = findMaxRangeChannel();
                sortByChannel(splitChannel);
                return splitIntoNewBucket();
            }

            /**
             * Find which color channel has the maximum range.
             */
            private ColorChannel findMaxRangeChannel() {
                int redDiff = Math.max(0, maxRed - minRed);
                int greenDiff = Math.max(0, maxGreen - minGreen);
                int blueDiff = Math.max(0, maxBlue - minBlue);

                if (verbosity >= 5) {
                    System.err.printf("partn colors %d Δr %d Δg %d Δb %d\n",
                        colors.size(), redDiff, greenDiff, blueDiff);
                }

                if (redDiff > greenDiff && redDiff > blueDiff) {
                    return ColorChannel.RED;
                } else if (greenDiff > blueDiff) {
                    return ColorChannel.GREEN;
                } else {
                    return ColorChannel.BLUE;
                }
            }

            /**
             * Sort colors by the specified channel.
             */
            private void sortByChannel(final ColorChannel channel) {
                if (verbosity >= 5) {
                    System.err.println("    " + channel);
                }
                Comparator<ColorIdx> comparator = switch (channel) {
                    case RED -> Comparator.comparingInt(c -> Rgb.getRed(c.color));
                    case GREEN -> Comparator.comparingInt(c -> Rgb.getGreen(c.color));
                    case BLUE -> Comparator.comparingInt(c -> Rgb.getBlue(c.color));
                };
                colors.sort(comparator);
            }

            /**
             * Split the bucket in half and return the new bucket with upper half.
             */
            private Bucket splitIntoNewBucket() {
                int oldN = colors.size();
                int splitPoint = oldN / 2;

                // Create new bucket from upper half (subList backed by original)
                List<ColorIdx> newBucketColors = colors.subList(splitPoint, oldN);
                Bucket newBucket = new Bucket(newBucketColors.size());
                for (ColorIdx color : newBucketColors) {
                    newBucket.add(color);
                }

                // Copy lower half before reset (required: reset() replaces colors list)
                List<ColorIdx> keepColors = new ArrayList<>(colors.subList(0, splitPoint));
                reset(keepColors.size());
                for (ColorIdx color : keepColors) {
                    add(color);
                }

                return newBucket;
            }

            /**
             * Color channel enumeration for partition decisions.
             */
            private enum ColorChannel {RED, GREEN, BLUE}

            /**
             * Average the colors in this bucket.
             *
             * @return an averaged RGB value
             */
            public int average() {
                if (lastAverage != -1) {
                    return lastAverage;
                }

                if (quantizationDone) {
                    lastAverage = computeQuantizedAverage();
                } else {
                    lastAverage = computeWeightedAverage();
                }
                return lastAverage;
            }

            /**
             * Compute average for quantized palette.
             */
            private int computeQuantizedAverage() {
                if (colors.isEmpty()) {
                    return Rgb.SIXEL_BLACK;
                }
                int sixelColor = sixelColors.get(index);
                if (sixelColor == Rgb.SIXEL_BLACK || sixelColor == Rgb.SIXEL_WHITE) {
                    return sixelColor;
                }
                return computeWeightedAverage();
            }

            /**
             * Compute weighted average of all colors in bucket.
             */
            private int computeWeightedAverage() {
                if (colors.isEmpty()) {
                    return Rgb.SIXEL_BLACK;
                }

                long totalRed = 0;
                long totalGreen = 0;
                long totalBlue = 0;
                long count = 0;
                for (ColorIdx color : colors) {
                    int rgb = color.color;
                    totalRed += (long) color.count * Rgb.getRed(rgb);
                    totalGreen += (long) color.count * Rgb.getGreen(rgb);
                    totalBlue += (long) color.count * Rgb.getBlue(rgb);
                    count += color.count;
                }

                if (count == 0) {
                    return Rgb.SIXEL_BLACK;
                }

                int avgRed = Rgb.clampSixelValue((int) (totalRed / count));
                int avgGreen = Rgb.clampSixelValue((int) (totalGreen / count));
                int avgBlue = Rgb.clampSixelValue((int) (totalBlue / count));

                return Rgb.combineRgb(avgRed, avgGreen, avgBlue);
            }

            /**
             * Generate a human-readable string for this entry.
             *
             * @return a human-readable string
             */
            @Override
            public String toString() {
                return String.format("bucket %d colors avg RGB %06x index %d",
                    colors.size(), average(), index);
            }
        }

        /**
         * A mapping of sixel color index to its first principal component.
         */
        private static class PcaColor implements Comparable<PcaColor> {

            /**
             * Index into sixelColors.
             */
            private int sixelIndex = 0;

            /**
             * The first principal component value.
             */
            private double firstPca = 0;

            /**
             * The second principal component value.
             */
            private double secondPca = 0;

            /**
             * The third principal component value.
             */
            private double thirdPca = 0;

            /**
             * Public constructor.
             *
             * @param sixelIndex the the index into sixelColors
             * @param firstPca   the first principal component
             * @param secondPca  the second principal component
             * @param thirdPca   the third principal component
             */
            public PcaColor(final int sixelIndex, final double firstPca,
                            final double secondPca, final double thirdPca) {

                this.sixelIndex = sixelIndex;
                this.firstPca = firstPca;
                this.secondPca = secondPca;
                this.thirdPca = thirdPca;
            }

            /**
             * Comparison operator puts the natural ordering along the first
             * principal component.
             *
             * @param that another PcaColor
             * @return difference between this.firstPca and that.firstPca
             */
            public int compareTo(final PcaColor that) {
                return Double.compare(this.firstPca, that.firstPca);
            }

            /**
             * Checks equality based only on firstPca.
             * <p>
             * Note: This intentionally only compares firstPca because PcaColor objects
             * are used as search keys in binary search operations where only the first
             * principal component determines the search position. The other fields
             * (sixelIndex, secondPca, thirdPca) are metadata associated with palette
             * entries, not part of the search key identity.
             */
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof PcaColor pcaColor)) return false;
                return Double.compare(firstPca, pcaColor.firstPca) == 0;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(firstPca);
            }
        }

        /**
         * Metadata regarding one sixel row.
         */
        private class SixelRow {

            /**
             * A set of colors that are present in this row.
             */
            private final BitSet colors;

            /**
             * Public constructor.
             */
            public SixelRow() {
                colors = new BitSet(sixelColors.size());
            }

        }

        /**
         * ColorMatchCache is a FIFO cache that hangs on to the matched
         * palette index color for a RGB color.
         * <p>
         * Uses primitive int arrays instead of wrapper classes for better performance.
         */
        private static class ColorMatchCache {

            /**
             * Maximum size of the cache.
             */
            private final int maxSize;

            /**
             * Keys stored in the cache (RGB colors). Uses -1 as empty marker.
             */
            private final int[] keys;

            /**
             * Values stored in the cache (palette indices).
             */
            private final int[] values;

            /**
             * Order of entries for FIFO eviction (stores indices into keys/values).
             */
            private final int[] order;

            /**
             * Current write position in the order array.
             */
            private int writePos = 0;

            /**
             * Current number of entries in cache.
             */
            private int size = 0;

            /**
             * Public constructor.
             *
             * @param maxSize the maximum size of the cache
             */
            public ColorMatchCache(final int maxSize) {
                this.maxSize = maxSize;
                this.keys = new int[maxSize];
                this.values = new int[maxSize];
                this.order = new int[maxSize];
                Arrays.fill(keys, -1);   // -1 indicates empty slot
                Arrays.fill(order, -1);  // -1 indicates uninitialized order entry
            }

            /**
             * Get an entry from the cache using linear probing.
             *
             * @param color the RGB color
             * @return the palette index, or -1 if not in cache
             */
            public int get(final int color) {
                int hash = (color & 0x7FFFFFFF) % maxSize;
                int probe = 0;
                while (probe < maxSize) {
                    int idx = (hash + probe) % maxSize;
                    if (keys[idx] == color) {
                        return values[idx];
                    }
                    if (keys[idx] == -1) {
                        return -1;  // Empty slot, not found
                    }
                    probe++;
                }
                return -1;
            }

            /**
             * Put an entry into the cache.
             *
             * @param color the RGB color
             * @param data  the palette index
             */
            public void put(final int color, final int data) {
                // Find slot using linear probing
                int hash = (color & 0x7FFFFFFF) % maxSize;
                int probe = 0;
                while (probe < maxSize) {
                    int idx = (hash + probe) % maxSize;
                    if (keys[idx] == color) {
                        // Update existing entry
                        values[idx] = data;
                        return;
                    }
                    if (keys[idx] == -1) {
                        // Check if eviction needed before adding new entry
                        if (size >= maxSize * 3 / 4) {
                            // Evict oldest entry (only if order entry is valid)
                            int evictIdx = order[writePos];
                            if (evictIdx >= 0 && evictIdx < maxSize && keys[evictIdx] != -1) {
                                keys[evictIdx] = -1;
                                size--;
                            }
                        }
                        // Add new entry
                        keys[idx] = color;
                        values[idx] = data;
                        order[writePos] = idx;
                        writePos = (writePos + 1) % maxSize;
                        size++;
                        return;
                    }
                    probe++;
                }
                // Cache is full and no empty slot was found. Evict the oldest entry
                // and insert the new color at that position.
                // Note: size is not changed because we're replacing an existing entry
                if (size > 0) {
                    int evictIdx = order[writePos];
                    if (evictIdx >= 0 && evictIdx < maxSize) {
                        keys[evictIdx] = color;
                        values[evictIdx] = data;
                        order[writePos] = evictIdx;
                        writePos = (writePos + 1) % maxSize;
                    }
                }
            }

        }

        /**
         * Number of colors in this palette.
         */
        private int paletteSize = 0;

        /**
         * Color palette for sixel output, sorted low to high.
         */
        private List<Integer> sixelColors = null;

        /**
         * The colors actually used in the image.
         */
        private BitSet usedColors = null;

        /**
         * Color palette for sixel output, sorted low to high by the first
         * principal component.
         */
        private List<PcaColor> pcaColors = null;

        /**
         * Comparator used for the first principal component search.
         */
        private final Comparator<PcaColor> nearby = Comparator.comparingDouble(c -> c.firstPca);

        /**
         * A map of recent matching colors.
         */
        private ColorMatchCache recentColorMatch;

        /**
         * The key used for binary search. Reused to avoid allocations in the
         * hot path of findNearestColor().
         * Note: This field is not thread-safe within a single Palette instance,
         * but thread safety is ensured by creating a new Palette for each
         * encoding operation.
         */
        private final PcaColor pcaKey = new PcaColor(0, 0, 0, 0);

        /**
         * The index into pcaColors last found by binary search.
         * Note: This field is used as a search optimization hint and is
         * intentionally not thread-safe within a single Palette instance.
         * Thread safety is ensured by creating a new Palette for each
         * encoding operation.
         */
        private int lastPcaSearchIndex = 0;

        /**
         * The distance along the first principal component axis at which two
         * colors are deemed close to each other.
         */
        private double pcaThreshold = 0;

        /**
         * The PCA change of basis matrix.
         */
        private double[][] PCA;

        /**
         * Map of colors used in the image by RGB.
         */
        private HashMap<Integer, ColorIdx> colorMap = null;

        /**
         * Type of color quantization used.
         * <p>
         * -1 = direct map indexed; 0 = direct map; 1 = median cut.
         */
        private int quantizationType = -1;

        /**
         * The image from the constructor, mapped to sixel color space with
         * transparent pixels removed.
         */
        private final int[] sixelImage;

        /**
         * The width of the image.
         */
        private final int sixelImageWidth;

        /**
         * The width of the image.
         */
        private final int sixelImageHeight;

        /**
         * If true, some pixels of the image are transparent.
         */
        private boolean transparent = false;

        /**
         * If true, sixelImage is already indexed and does not require
         * dithering.
         */
        @SuppressWarnings("FieldCanBeLocal")
        private boolean noDither = false;

        /**
         * The buckets produced by median cut.
         */
        private ArrayList<Bucket> buckets;

        /**
         * The sixel rows of this image.
         */
        private final SixelRow[] sixelRows;

        /**
         * If true, quantization is done.
         */
        private boolean quantizationDone = false;

        /**
         * Timings.
         */
        private Timings timings;

        /**
         * Public constructor.
         *
         * @param size             number of colors available for this palette
         * @param image            a bitmap image
         * @param allowTransparent if true, allow transparent pixels to be
         *                         specified
         * @param customPalette    if set, use a specific palette instead of
         *                         direct map or median cut
         */
        public Palette(final int size, final ImageRGB image,
                       final boolean allowTransparent,
                       final Map<Integer, Integer> customPalette) {

            assert (size >= 2);
            assert (image.getWidth() > 0);
            assert (image.getHeight() > 0);

            if (doTimings) {
                timings = new Timings();
                timings.startTime = System.nanoTime();
            }

            paletteSize = size;
            int numColors = paletteSize;
            if (fastAndDirty) {
                // Fast and dirty: use fewer colors.  Horizontal banding will
                // result, but might not be noticeable for fast-moving
                // scenes.
                numColors = Math.min(paletteSize, FAST_AND_DIRTY);
            }
            if (customPalette != null) {
                numColors = customPalette.size();
            }
            sixelColors = new ArrayList<>(numColors);
            usedColors = new BitSet(numColors);
            sixelRows = new SixelRow[(image.getHeight() / 6) + 1];
            Arrays.setAll(sixelRows, i -> new SixelRow());

            sixelImageWidth = image.getWidth();
            sixelImageHeight = image.getHeight();
            int totalPixels = sixelImageWidth * sixelImageHeight;

            // Sample the colors.  We will be taking SAMPLE_SIZE'd pixel
            // bands uniformly through the image data, with numColors bands.
            final int SAMPLE_SIZE = 4;
            int stride = 0;
            if (totalPixels > SAMPLE_SIZE * numColors) {
                stride = Math.max(0, totalPixels / numColors);
            }
            if (verbosity >= 1) {
                System.err.printf("Sampling %d pixels per color stride=%d\n",
                    SAMPLE_SIZE, stride);
            }

            int[] rgbArray = image.getRGB(0, 0,
                sixelImageWidth, sixelImageHeight, null, 0, sixelImageWidth);
            sixelImage = rgbArray;
            colorMap = HashMap.newHashMap(sixelImageWidth * sixelImageHeight);
            int transparentCount = 0;

            int strideI = 0;
            for (int i = 0; i < rgbArray.length; i++) {
                int colorRGB = rgbArray[i];
                if (transparent) {
                    int alpha = ((colorRGB >>> 24) & 0xFF);
                    if (alpha < ALPHA_OPAQUE) {
                        // This pixel is almost transparent, omit it.
                        transparentCount++;
                        if (allowTransparent) {
                            rgbArray[i] = 0x00f7a8b8;
                            continue;
                        } else {
                            rgbArray[i] = 0xFF000000;
                        }
                    }
                } else if ((colorRGB & 0xFF000000) != 0xFF000000) {
                    if (verbosity >= 10) {
                        System.err.printf("EH? color at %d is %08x\n", i,
                            colorRGB);
                    }
                    rgbArray[i] = 0xFF000000;
                }

                // Pull the 8-bit colors, and reduce them to 0-100 as per
                // sixel.
                int sixelRGB = toSixelColor(colorRGB, true);
                rgbArray[i] = sixelRGB;
                if ((stride == 0) || (strideI < SAMPLE_SIZE)) {
                    ColorIdx color = colorMap.get(sixelRGB & 0x00FFFFFF);
                    if (color == null) {
                        color = new ColorIdx(sixelRGB & 0x00FFFFFF);
                        colorMap.put(sixelRGB & 0x00FFFFFF, color);
                    } else {
                        color.count++;
                    }
                }
                if (strideI >= stride) {
                    strideI = 0;
                } else {
                    strideI++;
                }
            } // for (int i = 0; i < rgbArray.length; i++)

            /*
             * At this point:
             *
             * 1. The image data is mapped to the 101^3 sixel color space.
             *
             * 2. Any pixels with partial transparency below ALPHA_OPAQUE are
             *    fully transparent (and pink).
             *
             * 3. The sampled color map is populated with up to SAMPLE_SIZE *
             *    numColors samples.
             */

            if (verbosity >= 1) {
                System.err.printf("# colors in image (sampled): %d palette size %d\n",
                    colorMap.size(), numColors);
                System.err.printf("# transparent pixels: %d (%3.1f%%)\n",
                    transparentCount,
                    (double) transparentCount * 100.0 /
                        (sixelImageWidth * sixelImageHeight));
            }
            if ((transparentCount == 0) || !allowTransparent) {
                transparent = false;
            }

            assert (colorMap.size() > 0);

            if (timings != null) {
                timings.scanImageTime = System.nanoTime();
            }

            /*
             * Here we choose between several options:
             *
             * - If a custom palette was specified, use it.
             *
             * - If the palette size is big enough for the number of colors,
             *   and we know that because we sampled every pixel (stride ==
             *   0), then just do a straight 1-1 mapping.
             *
             * - Otherwise use median cut.
             */
            if (customPalette != null) {
                quantizationType = 1;
                List<Integer> keys = new ArrayList<Integer>(customPalette.keySet());
                Collections.sort(keys);
                for (Integer idx : keys) {
                    int colorRGB = customPalette.get(idx);
                    int sixelRGB = toSixelColor(colorRGB, true);
                    sixelColors.add(sixelRGB);
                }
                quantizationDone = true;
                if (verbosity >= 5) {
                    System.err.printf("COLOR MAP: %d entries\n",
                        sixelColors.size());
                    for (int i = 0; i < sixelColors.size(); i++) {
                        System.err.printf("   %03d %08x\n", i,
                            sixelColors.get(i));
                    }
                }

                if (timings != null) {
                    timings.buildColorMapTime = System.nanoTime();
                }

                // Now that colors have been established, build the search
                // structure for them.
                buildSearchMap();
            } else if ((stride == 0) && (colorMap.size() <= numColors)) {
                quantizationType = 0;
                directMap();
            } else if (true || (colorMap.size() <= numColors * 10)) {
                quantizationType = 1;
                medianCut();
            }
        }

        /**
         * Convert a 24-bit color to a 19.97-bit sixel color.
         *
         * @param rawColor the 24-bit color
         * @return the sixel color
         */
        public int toSixelColor(final int rawColor) {
            return Rgb.toSixelColor(rawColor);
        }

        /**
         * Convert a 24-bit color to a 19.97-bit sixel color.
         *
         * @param rawColor        the 24-bit color
         * @param checkBlackWhite if true, return pure black or pure white
         *                        for colors that are close to those
         * @return the sixel color
         */
        public int toSixelColor(final int rawColor, boolean checkBlackWhite) {
            if (!checkBlackWhite) {
                return Rgb.toSixelColor(rawColor);
            }

            Rgb color = Rgb.fromPackedRgb(rawColor).toSixelSpace();

            if (color.isNearBlack(10)) {
                logVerbose(10, "mapping to black: %08x%n", rawColor);
                return Rgb.SIXEL_BLACK;
            } else if (color.isNearWhite(0)) {
                logVerbose(10, "mapping to white: %08x%n", rawColor);
                return Rgb.SIXEL_WHITE;
            }

            return color.toPackedRgb();
        }

        /**
         * Log a verbose message if verbosity level is sufficient.
         */
        private void logVerbose(int level, String format, Object... args) {
            if (verbosity >= level) {
                System.err.printf(format, args);
            }
        }

        /**
         * Assign palette entries to the image colors.  This requires at
         * least as many palette colors as number of colors used in the
         * image.
         */
        public void directMap() {
            assert (quantizationType == 0);
            assert (paletteSize >= colorMap.size());

            if (verbosity >= 1) {
                System.err.println("Direct-map colors");
            }

            // The simplest thing: just put the used colors in RGB order.  We
            // don't _need_ an ordering, but it does make it nicer to look at
            // the generated output and understand what's going on.
            sixelColors = new ArrayList<Integer>(colorMap.size());
            usedColors = new BitSet(colorMap.size());
            for (ColorIdx color : colorMap.values()) {
                sixelColors.add(color.color);
            }
            if (verbosity >= 5) {
                Collections.sort(sixelColors);
            }
            assert (sixelColors.size() == colorMap.size());
            for (int i = 0; i < sixelColors.size(); i++) {
                colorMap.get(sixelColors.get(i)).directMapIndex = i;
            }

            quantizationDone = true;
            if (verbosity >= 1) {
                System.err.printf("colorMap size %d sixelColors size %d\n",
                    colorMap.size(), sixelColors.size());
                if (verbosity >= 5) {
                    System.err.printf("COLOR MAP:\n");
                    for (int i = 0; i < sixelColors.size(); i++) {
                        System.err.printf("   %03d %s\n", i,
                            colorMap.get(sixelColors.get(i)));
                    }
                }
            }
            if (timings != null) {
                timings.buildColorMapTime = System.nanoTime();
            }
        }

        /**
         * Perform median cut algorithm to generate a palette that fits
         * within the palette size.
         */
        public void medianCut() {
            assert (quantizationType == 1);

            // Populate the "total" bucket.
            Bucket bucket = new Bucket(colorMap.size());
            for (ColorIdx color : colorMap.values()) {
                bucket.add(color);

                int rgb = color.color;
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
            }

            int numColors = paletteSize;
            if (fastAndDirty) {
                // Fast and dirty: use fewer colors.  Horizontal banding will
                // result, but might not be noticeable for fast-moving
                // scenes.
                numColors = Math.min(paletteSize, FAST_AND_DIRTY);
            }

            // Find the number of buckets we can have based on the palette
            // size.
            int log2 = 31 - Integer.numberOfLeadingZeros(numColors);
            int totalBuckets = 1 << log2;
            if (verbosity >= 1) {
                System.err.println("Total buckets possible: " + totalBuckets);
            }

            buckets = new ArrayList<Bucket>(totalBuckets);
            buckets.add(bucket);
            while (buckets.size() < totalBuckets) {
                int n = buckets.size();
                for (int i = 0; i < n; i++) {
                    buckets.add(buckets.get(i).partition());
                }
            }
            assert (buckets.size() == totalBuckets);

            // Buckets are partitioned.  Now assign them to the sixel
            // palette, and also find the darkest and lightest buckets and
            // assign them to black and white.
            int idx = 0;
            int darkest = Integer.MAX_VALUE;
            int lightest = 0;
            int darkestIdx = -1;
            int lightestIdx = -1;
            final int diff = 1000;
            for (Bucket b : buckets) {
                int rgb = b.average();
                b.index = idx;
                Rgb color = Rgb.fromPackedRgb(rgb);
                int colorMagnitudeSq = color.r() * color.r() + color.g() * color.g() + color.b() * color.b();

                if (color.isNearBlack(diff)) {
                    // Black is a close match.
                    if (colorMagnitudeSq < darkest) {
                        darkest = colorMagnitudeSq;
                        darkestIdx = idx;
                    }
                } else if (color.isNearWhite(diff)) {
                    // White is a close match.
                    if (colorMagnitudeSq > lightest) {
                        lightest = colorMagnitudeSq;
                        lightestIdx = idx;
                    }
                }
                sixelColors.add(rgb);
                idx++;
            }
            if (darkestIdx != -1) {
                sixelColors.set(darkestIdx, Rgb.SIXEL_BLACK);
            }
            if (lightestIdx != -1) {
                sixelColors.set(lightestIdx, Rgb.SIXEL_WHITE);
            }

            quantizationDone = true;
            if (verbosity >= 5) {
                System.err.printf("COLOR MAP: %d entries\n",
                    sixelColors.size());
                for (int i = 0; i < sixelColors.size(); i++) {
                    System.err.printf("   %03d %08x\n", i,
                        sixelColors.get(i));
                }
            }

            // Now that colors have been established, build the search
            // structure for them.
            buildSearchMap();

            if (timings != null) {
                timings.buildColorMapTime = System.nanoTime();
            }
        }

        /**
         * Sort the palette colors by principal component so that they can be
         * located quickly in dither().  This approach was first brought to
         * open-source by Hans Petter Jansson's chafa project:
         * https://hpjansson.org/chafa/ .
         */
        private void buildSearchMap() {
            // Build the covariance matrix and find its eigenvalues.  These
            // will be the principal components.
            //
            // (The computational chemist in me is SO HAPPY that we finally
            // have an eigenvalue solver in Jexer. 💗)
            double[][] A = new double[3][3];

            double redMean = 0;
            double greenMean = 0;
            double blueMean = 0;
            int n = sixelColors.size();
            for (int rgbColor : sixelColors) {
                redMean += (rgbColor >>> 16) & 0xFF;
                greenMean += (rgbColor >>> 8) & 0xFF;
                blueMean += rgbColor & 0xFF;
            }
            redMean /= n;
            greenMean /= n;
            blueMean /= n;
            double covRedRed = 0;
            double covRedGreen = 0;
            double covRedBlue = 0;
            double covGreenGreen = 0;
            double covGreenBlue = 0;
            double covBlueBlue = 0;
            for (int rgbColor : sixelColors) {
                int red = (rgbColor >>> 16) & 0xFF;
                int green = (rgbColor >>> 8) & 0xFF;
                int blue = rgbColor & 0xFF;

                covRedRed += (red - redMean) * (red - redMean);
                covRedGreen += (red - redMean) * (green - greenMean);
                covRedBlue += (red - redMean) * (blue - blueMean);
                covGreenGreen += (green - greenMean) * (green - greenMean);
                covGreenBlue += (green - greenMean) * (blue - blueMean);
                covBlueBlue += (blue - blueMean) * (blue - blueMean);
            }
            covRedRed /= (n - 1);
            covRedGreen /= (n - 1);
            covRedBlue /= (n - 1);
            covGreenGreen /= (n - 1);
            covGreenBlue /= (n - 1);
            covBlueBlue /= (n - 1);

            A[0][0] = covRedRed;
            A[0][1] = covRedGreen;
            A[0][2] = covRedBlue;
            A[1][0] = covRedGreen;
            A[1][1] = covGreenGreen;
            A[1][2] = covGreenBlue;
            A[2][0] = covRedGreen;
            A[2][1] = covGreenBlue;
            A[2][2] = covBlueBlue;

            double[][] V = new double[3][3];
            double[] d = new double[3];

            MathUtils.eigen3(A, V, d);

            if (verbosity >= 1) {
                System.err.printf("PCA => eigenvalues: %8.4f %8.4f %8.4f\n",
                    d[0], d[1], d[2]);
                double pcaSum = d[0] + d[1] + d[2];
                System.err.printf("                   %7.2f%%  %7.2f%%  %7.2f%%\n",
                    Math.abs((d[0] / pcaSum) * 100.0),
                    Math.abs((d[1] / pcaSum) * 100.0),
                    Math.abs((d[2] / pcaSum) * 100.0));

                System.err.printf("PCA => [ %8.4f %8.4f %8.4f]\n       [ %8.4f %8.4f %8.4f]\n       [ %8.4f %8.4f %8.4f]\n",
                    V[0][0], V[0][1], V[0][2],
                    V[1][0], V[1][1], V[1][2],
                    V[2][0], V[2][1], V[2][2]
                );
            }

            // The principal component scalars are in d[3] (first), d[2]
            // (second), and d[1] (third).

            // Now sort sixelColors by first PCA.

            // This involves creation of a "change of basis matrix from RGB
            // to PCA".  If I am reading page 3 of
            // http://www.math.lsa.umich.edu/~kesmith/CoordinateChange.pdf
            // correctly, then V _is_ the change of basis matrix because we
            // know that all of its vectors are orthogonal.
            PCA = V;

            // Build PCA color list using IntStream for cleaner iteration
            pcaColors = java.util.stream.IntStream.range(0, sixelColors.size())
                .mapToObj(i -> {
                    int rgb = sixelColors.get(i);
                    return new PcaColor(i, firstPca(rgb), secondPca(rgb), thirdPca(rgb));
                })
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            // A first principal component difference within 8 indices will
            // be deemed in the same neighborhood.
            int n2 = pcaColors.size();
            pcaThreshold = ((pcaColors.get(n2 - 1).firstPca - pcaColors.get(0).firstPca) / n2) * 8.0;

            // Allow up the last 8192 colors to be re-used, or 10% of the
            // image size.
            recentColorMatch = new ColorMatchCache(Math.min(
                sixelImageWidth * sixelImageHeight / 10, 8192));
        }

        /**
         * Find the first principal component value of an RGB color.
         *
         * @param color the RGB color
         * @return the color's PCA1 coordinate in PCA space
         */
        private double firstPca(final int color) {
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;

            // Due to how MathUtils.eigen3() sorts the eigenvalues, the first
            // principal component is the last column in the PCA matrix.
            return (PCA[2][0] * red) + (PCA[2][1] * green) + (PCA[2][2] * blue);
        }

        /**
         * Find the second principal component value of an RGB color.
         *
         * @param color the RGB color
         * @return the color's PCA1 coordinate in PCA space
         */
        private double secondPca(final int color) {
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;

            // Due to how MathUtils.eigen3() sorts the eigenvalues, the second
            // principal component is the middle column in the PCA matrix.
            return (PCA[1][0] * red) + (PCA[1][1] * green) + (PCA[1][2] * blue);
        }

        /**
         * Find the third principal component value of an RGB color.
         *
         * @param color the RGB color
         * @return the color's PCA1 coordinate in PCA space
         */
        private double thirdPca(final int color) {
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;

            // Due to how MathUtils.eigen3() sorts the eigenvalues, the third
            // principal component is the first column in the PCA matrix.
            return (PCA[0][0] * red) + (PCA[0][1] * green) + (PCA[0][2] * blue);
        }

        /**
         * Search through the palette and find the best RGB match in the
         * sixel palette.  This particular approach was first done by Hans
         * Petter Jansson's chafa project: https://hpjansson.org/chafa/ .
         * The palette colors have been sorted by their principal component
         * (see principal component analysis), such that a binary search can
         * quickly find the region where the closest matching color resides.
         * One can then search forward and backward to find all nearby
         * colors.
         *
         * @param red   the red component, from 0-100
         * @param green the green component, from 0-100
         * @param blue  the blue component, from 0-100
         * @return the palette index of the nearest color in RGB space
         */
        private int findNearestColor(final int red, final int green,
                                     final int blue) {

            // Compute PCA coordinates inline to avoid array allocation
            final double pca1 = PCA[2][0] * red + PCA[2][1] * green + PCA[2][2] * blue;
            final double pca2 = PCA[1][0] * red + PCA[1][1] * green + PCA[1][2] * blue;
            final double pca3 = PCA[0][0] * red + PCA[0][1] * green + PCA[0][2] * blue;

            // Find search starting point
            PcaColor lastPcaColor = pcaColors.get(lastPcaSearchIndex);
            PcaColor centerPca;
            if (Math.abs(lastPcaColor.firstPca - pca1) < pcaThreshold) {
                centerPca = lastPcaColor;
            } else {
                pcaKey.firstPca = pca1;
                int pcaIndex = Math.abs(Collections.binarySearch(pcaColors, pcaKey, nearby));
                lastPcaSearchIndex = Math.clamp(pcaIndex, 0, sixelColors.size() - 1);
                centerPca = pcaColors.get(lastPcaSearchIndex);
            }

            int result = centerPca.sixelIndex;
            int sixelRgb = sixelColors.get(result);
            int bestRgbDistance = distanceSquaredInline(sixelRgb, red, green, blue);

            // Compute initial PCA distance
            double d1 = centerPca.firstPca - pca1;
            double d2 = centerPca.secondPca - pca2;
            double d3 = centerPca.thirdPca - pca3;
            double pcaDistance = d1 * d1 + d2 * d2 + d3 * d3;

            // Search up
            int idx = lastPcaSearchIndex;
            int n = pcaColors.size();
            while (idx + 1 < n) {
                idx++;
                PcaColor candidate = pcaColors.get(idx);
                d1 = candidate.firstPca - pca1;
                d2 = candidate.secondPca - pca2;
                d3 = candidate.thirdPca - pca3;
                double candidateDistance = d1 * d1 + d2 * d2 + d3 * d3;
                if (candidateDistance <= pcaDistance) {
                    int rgbDistance = distanceSquaredInline(
                        sixelColors.get(candidate.sixelIndex), red, green, blue);
                    if (rgbDistance < bestRgbDistance) {
                        result = candidate.sixelIndex;
                        bestRgbDistance = rgbDistance;
                    }
                    pcaDistance = candidateDistance;
                }
                if ((candidate.firstPca - pca1) > pcaDistance) {
                    break;
                }
            }

            // Search down
            idx = lastPcaSearchIndex;
            while (idx > 0) {
                idx--;
                PcaColor candidate = pcaColors.get(idx);
                d1 = candidate.firstPca - pca1;
                d2 = candidate.secondPca - pca2;
                d3 = candidate.thirdPca - pca3;
                double candidateDistance = d1 * d1 + d2 * d2 + d3 * d3;
                if (candidateDistance <= pcaDistance) {
                    int rgbDistance = distanceSquaredInline(
                        sixelColors.get(candidate.sixelIndex), red, green, blue);
                    if (rgbDistance < bestRgbDistance) {
                        result = candidate.sixelIndex;
                        bestRgbDistance = rgbDistance;
                    }
                    pcaDistance = candidateDistance;
                }
                if ((pca1 - candidate.firstPca) > pcaDistance) {
                    break;
                }
            }

            return result;
        }

        /**
         * Inline distance calculation to avoid method call overhead in hot path.
         */
        private static int distanceSquaredInline(final int rgb, final int red,
                                                 final int green, final int blue) {
            int dr = ((rgb >>> 16) & 0xFF) - red;
            int dg = ((rgb >>> 8) & 0xFF) - green;
            int db = (rgb & 0xFF) - blue;
            return dr * dr + dg * dg + db * db;
        }

        /**
         * Dither an image to a paletteSize palette.  The dithered
         * image cells will contain indexes into the palette.
         *
         * @return the dithered image rgb data.  Every pixel is an index into
         * the palette.
         */
        public int[] ditherImage() {
            int[] rgbArray = sixelImage;
            if (noDither) {
                return rgbArray;
            }

            int height = sixelImageHeight;
            int width = sixelImageWidth;
            for (int imageY = 0; imageY < height; imageY++) {
                SixelRow sixelRow = sixelRows[imageY / 6];
                for (int imageX = 0; imageX < width; imageX++) {
                    ditherPixel(rgbArray, width, imageX, imageY, sixelRow);
                }
            }
            return rgbArray;
        }

        /**
         * Dither a single pixel.
         */
        private void ditherPixel(int[] rgbArray, int width, int imageX, int imageY,
                                 SixelRow sixelRow) {
            int pixelIndex = imageX + width * imageY;
            int oldPixel = rgbArray[pixelIndex];

            if (!Rgb.isOpaque(oldPixel)) {
                logVerbose(10, "transparent oldPixel(%d, %d) %08x%n", imageX, imageY, oldPixel);
                rgbArray[pixelIndex] = -1;
                return;
            }

            logVerbose(10, "opaque oldPixel(%d, %d) %08x%n", imageX, imageY, oldPixel);

            int colorIdx = findColorIndex(oldPixel);
            int newPixel = sixelColors.get(colorIdx);
            rgbArray[pixelIndex] = colorIdx;
            sixelRow.colors.set(colorIdx);
            usedColors.set(colorIdx);

            if (quantizationType != 0) {
                propagateDitheringError(rgbArray, width, imageX, imageY, oldPixel, newPixel);
            }
        }

        /**
         * Find the palette index for a pixel color.
         * Inlined bit operations for performance.
         */
        private int findColorIndex(int pixel) {
            int color = pixel & 0x00FFFFFF;
            if (quantizationType == 0) {
                return colorMap.get(color).directMapIndex;
            }

            int colorIdx = recentColorMatch.get(color);
            if (colorIdx < 0) {
                // Inline bit extraction for performance
                colorIdx = findNearestColor(
                    (color >>> 16) & 0xFF,
                    (color >>> 8) & 0xFF,
                    color & 0xFF);
                recentColorMatch.put(color, colorIdx);
            }
            return colorIdx;
        }

        /**
         * Propagate dithering error to neighboring pixels using Floyd-Steinberg.
         * Sixel color space error divisors: 6 (main), 3 (right), 1 (bottom-left, bottom-right), 2 (bottom)
         * Inlined for performance.
         */
        private void propagateDitheringError(int[] rgbArray, int width, int imageX, int imageY,
                                             int oldPixel, int newPixel) {

            // Inline bit extraction for performance
            int redError = (((oldPixel >>> 16) & 0xFF) - ((newPixel >>> 16) & 0xFF)) / 6;
            int greenError = (((oldPixel >>> 8) & 0xFF) - ((newPixel >>> 8) & 0xFF)) / 6;
            int blueError = ((oldPixel & 0xFF) - (newPixel & 0xFF)) / 6;

            // Distribute error to neighboring pixels (inlined)
            int nextRow = imageY + 1;
            boolean hasRight = imageX < sixelImageWidth - 1;
            boolean hasBottom = nextRow < sixelImageHeight;

            if (hasRight) {
                applyErrorInline(rgbArray, imageX + 1 + width * imageY, redError * 3, greenError * 3, blueError * 3);
                if (hasBottom) {
                    applyErrorInline(rgbArray, imageX + 1 + width * nextRow, redError, greenError, blueError);
                }
            }

            if (hasBottom) {
                if (imageX > 0) {
                    applyErrorInline(rgbArray, imageX - 1 + width * nextRow, redError, greenError, blueError);
                }
                applyErrorInline(rgbArray, imageX + width * nextRow, redError * 2, greenError * 2, blueError * 2);
            }
        }

        /**
         * Apply error to a single pixel (inlined for performance).
         * Note: At this point, pixel values are in sixel color space (0-100), not RGB (0-255).
         */
        private void applyErrorInline(int[] rgbArray, int idx, int redError, int greenError, int blueError) {
            int pixel = rgbArray[idx];

            // Inline opacity check
            if ((pixel & 0xFF000000) != 0xFF000000) {
                rgbArray[idx] = 0;
                return;
            }

            // Extract, clamp to sixel range [0,100], and combine
            int red = Math.clamp(((pixel >>> 16) & 0xFF) + redError, 0, 100);
            int green = Math.clamp(((pixel >>> 8) & 0xFF) + greenError, 0, 100);
            int blue = Math.clamp((pixel & 0xFF) + blueError, 0, 100);
            rgbArray[idx] = (0xFF << 24) | (red << 16) | (green << 8) | blue;
        }

        /**
         * Emit the sixel palette.
         *
         * @param sb the StringBuilder to append to
         */
        public void emitPalette(final StringBuilder sb) {
            // Emit colors 1 to N-1 first, then 0 at the end (for hardware terminals)
            for (int i = 1; i < sixelColors.size(); i++) {
                emitColorIfUsed(sb, i);
            }
            emitColorIfUsed(sb, 0);
        }

        /**
         * Emit a single color definition if it's used.
         */
        private void emitColorIfUsed(final StringBuilder sb, final int index) {
            if (!usedColors.get(index)) {
                return;
            }
            int sixelColor = sixelColors.get(index);
            // Format: #<index>;2;<red>;<green>;<blue>
            sb.append('#').append(index).append(";2;")
                .append(Rgb.getRed(sixelColor)).append(';')
                .append(Rgb.getGreen(sixelColor)).append(';')
                .append(Rgb.getBlue(sixelColor));
        }
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Verbosity level for analysis mode.
     */
    private int verbosity = 0;

    /**
     * Number of colors in the sixel palette.  Xterm 335 defines the max as
     * 1024.  For HQ encoder the default is 128.
     * <p>
     * Marked volatile for thread-safe reads during encoding when another
     * thread may update the value via setPaletteSize().
     */
    private volatile int paletteSize = 128;

    /**
     * If true, record timings for the image.
     */
    private boolean doTimings = false;

    /**
     * If true, be fast and dirty.
     * <p>
     * Marked volatile for thread-safe reads during encoding when another
     * thread may update the value via reloadOptions().
     */
    private volatile boolean fastAndDirty = false;

    /**
     * Available custom palettes.
     * <p>
     * Marked volatile for thread-safe reads during encoding when another
     * thread may update the value via reloadOptions().
     */
    private volatile CustomSixelPalette customSixelPalette = CustomSixelPalette.NONE;

    /**
     * If true, don't emit palette colors.
     * <p>
     * Marked volatile for thread-safe reads during encoding when another
     * thread may update the value via reloadOptions().
     */
    private volatile boolean suppressEmitPalette = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    @SuppressWarnings("this-escape")
    public HQSixelEncoder() {
        reloadOptions();
    }

    // ------------------------------------------------------------------------
    // SixelEncoder -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap the bitmap data
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    @Override
    public String toSixel(final ImageRGB bitmap) {
        Map<Integer, Integer> customPalette = null;
        switch (customSixelPalette) {
            case CGA:
                customPalette = new HashMap<>(16);
                SixelDecoder.initializePaletteCGA(customPalette);
                break;
            case VT340:
                customPalette = new HashMap<>(16);
                SixelDecoder.initializePaletteVT340(customPalette);
                break;
            case NONE:
                break;
        }

        return toSixel(bitmap, false, customPalette, suppressEmitPalette);
    }

    // ------------------------------------------------------------------------
    // HQSixelEncoder ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reload options from System properties.
     */
    @Override
    public void reloadOptions() {
        // Palette size
        int paletteSize = 128;
        try {
            paletteSize = Integer.parseInt(System.getProperty(
                "casciian.ECMA48.sixelPaletteSize", "128"));
            switch (paletteSize) {
                case 2:
                case 4:
                case 8:
                case 16:
                case 32:
                case 64:
                case 128:
                case 256:
                case 512:
                case 1024:
                case 2048:
                    this.paletteSize = paletteSize;
                    break;
                default:
                    // Ignore value
                    break;
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
        if (System.getProperty("casciian.ECMA48.sixelFastAndDirty",
            "false").equals("true")
        ) {
            fastAndDirty = true;
        } else {
            fastAndDirty = false;
        }

        customSixelPalette = CustomSixelPalette.NONE;
        String customPaletteStr = System.getProperty("casciian.ECMA48.sixelCustomPalette",
            "none").toLowerCase();
        if (customPaletteStr.equals("cga")) {
            customSixelPalette = CustomSixelPalette.CGA;
        } else if (customPaletteStr.equals("vt340")) {
            customSixelPalette = CustomSixelPalette.VT340;
        }

        String emitPaletteStr = System.getProperty("casciian.ECMA48.sixelEmitPalette",
            "true").toLowerCase();
        if (emitPaletteStr.equals("false")) {
            suppressEmitPalette = true;
        }
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap           the bitmap data
     * @param allowTransparent if true, allow transparent pixels to be
     *                         specified
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    public String toSixel(final ImageRGB bitmap,
                          final boolean allowTransparent) {

        Map<Integer, Integer> customPalette = null;
        switch (customSixelPalette) {
            case CGA:
                customPalette = new HashMap<>(16);
                SixelDecoder.initializePaletteCGA(customPalette);
                break;
            case VT340:
                customPalette = new HashMap<>(16);
                SixelDecoder.initializePaletteVT340(customPalette);
                break;
            case NONE:
                break;
        }

        return toSixel(bitmap, allowTransparent, customPalette,
            suppressEmitPalette);
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap        the bitmap data
     * @param customPalette if set, use a specific palette instead of direct
     *                      map or median cut
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    public String toSixel(final ImageRGB bitmap,
                          final Map<Integer, Integer> customPalette) {

        return toSixel(bitmap, false, customPalette, suppressEmitPalette);
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap              the bitmap data
     * @param allowTransparent    if true, allow transparent pixels to be
     *                            specified
     * @param customPalette       if set, use a specific palette instead of direct
     *                            map or median cut
     * @param suppressEmitPalette if set, do not emit the sixel palette
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    public String toSixel(final ImageRGB bitmap,
                          final boolean allowTransparent,
                          final Map<Integer, Integer> customPalette,
                          final boolean suppressEmitPalette) {

        return toSixelResult(bitmap, allowTransparent,
            customPalette, suppressEmitPalette).encodedImage;
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap              the bitmap data
     * @param allowTransparent    if true, allow transparent pixels to be
     *                            specified
     * @param customPalette       if set, use a specific palette instead of direct
     *                            map or median cut
     * @param suppressEmitPalette if set, do not emit the sixel palette
     * @return the encoded string and transparency flag
     */
    private SixelResult toSixelResult(final ImageRGB bitmap,
                                      final boolean allowTransparent,
                                      final Map<Integer, Integer> customPalette,
                                      final boolean suppressEmitPalette) {

        // Start with 16k potential total output.
        StringBuilder sb = new StringBuilder(16384);

        assert (bitmap != null);
        int fullHeight = bitmap.getHeight();

        SixelResult result = new SixelResult();

        // Anaylze the picture and generate a palette.
        Palette palette = new Palette(paletteSize, bitmap, allowTransparent,
            customPalette);
        result.palette = palette;
        result.transparent = palette.transparent;

        // Dither the image.  We don't bother wrapping it in a ImageRGB.
        int[] rgbArray = palette.ditherImage();

        if (palette.timings != null) {
            palette.timings.ditherImageTime = System.nanoTime();
        }

        if (rgbArray == null) {
            if (palette.timings != null) {
                palette.timings.emitSixelTime = System.nanoTime();
                palette.timings.endTime = System.nanoTime();
            }
            result.encodedImage = "";
            return result;
        }

        if (!suppressEmitPalette) {
            // Emit the palette.
            palette.emitPalette(sb);
        }

        // Render the entire row of cells.
        int width = bitmap.getWidth();

        int colorsN = palette.sixelColors.size();
        // Reuse row array across colors to reduce allocations
        int[] row = new int[width];

        for (int currentRow = 0; currentRow < fullHeight; currentRow += 6) {
            Palette.SixelRow sixelRow = palette.sixelRows[currentRow / 6];

            for (int i = 0; i < colorsN; i++) {
                if (!sixelRow.colors.get(i)) {
                    continue;
                }

                // Clear the row array for reuse
                Arrays.fill(row, 0);

                // Calculate the number of rows to process (up to 6)
                int rowsToProcess = Math.min(6, fullHeight - currentRow);

                // Collect sixel bits for this color
                for (int j = 0; j < rowsToProcess; j++) {
                    int base = width * (currentRow + j);
                    int value = 1 << j;
                    for (int imageX = 0; imageX < width; imageX++) {
                        if (rgbArray[base + imageX] == i) {
                            row[imageX] += value;
                        }
                    }
                }

                // Set to the beginning of scan line for the next set of
                // colored pixels, and select the color.
                sb.append("$#").append(i);

                int oldData = -1;
                int oldDataCount = 0;
                for (int imageX = 0; imageX < width; imageX++) {
                    int data = row[imageX];

                    assert (data >= 0);
                    assert (data < 64);
                    data += 63;

                    if (data == oldData) {
                        oldDataCount++;
                    } else {
                        if (oldDataCount == 1) {
                            // assert (oldData != -1);
                            sb.append((char) oldData);
                        } else if (oldDataCount > 1) {
                            sb.append('!').append(oldDataCount).append((char) oldData);
                        }
                        oldDataCount = 1;
                        oldData = data;
                    }

                } // for (int imageX = 0; imageX < width; imageX++)

                // Emit the last sequence.
                if (oldDataCount == 1) {
                    // assert (oldData != -1);
                    sb.append((char) oldData);
                } else if (oldDataCount > 1) {
                    assert (oldData != -1);
                    sb.append('!').append(oldDataCount).append((char) oldData);
                }

            } // for (int i = 0; i < palette.sixelColors.size(); i++)

            // Advance to the next scan line.
            sb.append('-');

        } // for (int currentRow = 0; currentRow < imageHeight; currentRow += 6)

        // Kill the very last "-", because it is unnecessary.
        sb.deleteCharAt(sb.length() - 1);

        // Add the raster information.
        // Use StringBuilder for better performance than String.format
        StringBuilder header = new StringBuilder(20);
        header.append("\"1;1;").append(bitmap.getWidth())
            .append(';').append(bitmap.getHeight());
        sb.insert(0, header);

        if (palette.timings != null) {
            palette.timings.emitSixelTime = System.nanoTime();
            palette.timings.endTime = System.nanoTime();
        }
        result.encodedImage = sb.toString();
        return result;
    }

    /**
     * If the palette is shared for the entire terminal, emit it to a
     * StringBuilder.
     *
     * @param sb the StringBuilder to write the shared palette to
     */
    @Override
    public void emitPalette(final StringBuilder sb) {
        // NOP
    }

    /**
     * Get the sixel shared palette option.
     *
     * @return true if all sixel output is using the same palette that is set
     * in one DCS sequence and used in later sequences
     */
    public boolean hasSharedPalette() {
        return false;
    }

    /**
     * Set the sixel shared palette option.
     *
     * @param sharedPalette if true, then all sixel output will use the same
     *                      palette that is set in one DCS sequence and used in later sequences
     */
    @Override
    public void setSharedPalette(final boolean sharedPalette) {
        // NOP
    }

    /**
     * Get the number of colors in the sixel palette.
     *
     * @return the palette size
     */
    @Override
    public int getPaletteSize() {
        return paletteSize;
    }

    /**
     * Set the number of colors in the sixel palette.
     *
     * @param paletteSize the new palette size
     */
    @Override
    public void setPaletteSize(final int paletteSize) {
        if (this.paletteSize == paletteSize) {
            return;
        }

        switch (paletteSize) {
            case 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048:
                break;
            default:
                throw new IllegalArgumentException("Unsupported sixel palette " +
                    " size: " + paletteSize);
        }

        this.paletteSize = paletteSize;
    }

    /**
     * Clear the sixel palette.  It will be regenerated on the next image
     * encode.
     */
    @Override
    public void clearPalette() {
        // NOP
    }
}
