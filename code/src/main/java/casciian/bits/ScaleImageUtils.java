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
package casciian.bits;

import java.util.stream.IntStream;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Utility methods for scaling images using the Mitchell–Netravali
 * bicubic interpolation kernel (B = C = 1/3, support radius 2).
 *
 * <p>Scaling is implemented as two separable passes (horizontal then
 * vertical). Border pixels are handled by clamping coordinates at
 * the edges.</p>
 */
final class ScaleImageUtils {

    /**
     * Threshold for using parallel streams. Only compute-bound operations
     * benefit from parallelization.
     */
    private static final int PARALLEL_THRESHOLD = 10_000;

    /**
     * Preferred {@code double} vector species for SIMD convolution.
     * On x86 with AVX2 this is 4 lanes; on ARM NEON it is 2 lanes.
     * The channel layout used in the kernel is [R, G, B, W] — exactly
     * 4 doubles — so AVX2 processes one pixel's full kernel contribution
     * in a single fused-multiply-add step.
     */
    private static final VectorSpecies<Double> DOUBLE_SPECIES =
            DoubleVector.SPECIES_PREFERRED;

    private ScaleImageUtils() {
        // utility class – not instantiable
    }

    /**
     * Evaluates the Mitchell–Netravali kernel with B = C = 1/3.
     * Support radius is 2: the kernel is zero for |x| &gt;= 2.
     *
     * @param x the distance from the filter center
     * @return the filter weight
     */
    static double mitchellNetravali(double x) {
        double ax = Math.abs(x);
        if (ax >= 2.0) {
            return 0.0;
        }
        double ax2 = ax * ax;
        double ax3 = ax2 * ax;
        if (ax < 1.0) {
            return (7.0 / 6.0) * ax3 - 2.0 * ax2 + (8.0 / 9.0);
        }
        return (-7.0 / 18.0) * ax3 + 2.0 * ax2 - (10.0 / 3.0) * ax + (16.0 / 9.0);
    }

    /**
     * Clamps a floating-point value to the [0, 255] byte range and rounds it.
     */
    static int clampByte(double value) {
        int v = (int) Math.round(value);
        return Math.max(0, Math.min(255, v));
    }

    /**
     * Resamples each row from {@code srcWidth} to {@code dstWidth} pixels
     * using the Mitchell–Netravali filter.
     */
    static void resampleHorizontal(int[][] src, int[][] dst,
                                   int srcWidth, int dstWidth,
                                   int rows) {
        double ratio = (double) srcWidth / dstWidth;
        double filterScale = Math.max(1.0, ratio);
        double support = 2.0 * filterScale;

        IntStream rowStream = IntStream.range(0, rows);
        if ((long) dstWidth * rows > PARALLEL_THRESHOLD) {
            rowStream = rowStream.parallel();
        }

        rowStream.forEach(y -> {
            int[] srcRow = src[y];
            int[] dstRow = dst[y];
            for (int x = 0; x < dstWidth; x++) {
                double center = (x + 0.5) * ratio - 0.5;
                int left = (int) Math.floor(center - support);
                int right = (int) Math.ceil(center + support);

                double sumR = 0, sumG = 0, sumB = 0, sumW = 0;
                sumR = 0; sumG = 0; sumB = 0; sumW = 0;

                // Vectorized accumulation: pack [R, G, B, W] into a
                // DoubleVector so all four accumulators update in one FMA step.
                // Falls back to scalar on platforms with fewer than 4 lanes.
                final int laneCount = DOUBLE_SPECIES.length();
                if (laneCount >= 4) {
                    DoubleVector acc = DoubleVector.zero(DOUBLE_SPECIES);
                    for (int i = left; i <= right; i++) {
                        double weight = mitchellNetravali(
                                (i - center) / filterScale);
                        int clamped = Math.max(0, Math.min(i, srcWidth - 1));
                        int pixel = srcRow[clamped];
                        // [R, G, B, W] * weight  (pad with 0 for lanes > 4)
                        double[] contrib = new double[laneCount];
                        contrib[0] = ((pixel >>> 16) & 0xFF) * weight;
                        contrib[1] = ((pixel >>>  8) & 0xFF) * weight;
                        contrib[2] = ( pixel         & 0xFF) * weight;
                        contrib[3] = weight;
                        acc = acc.add(DoubleVector.fromArray(DOUBLE_SPECIES, contrib, 0));
                    }
                    double[] result = new double[laneCount];
                    acc.intoArray(result, 0);
                    sumR = result[0]; sumG = result[1]; sumB = result[2]; sumW = result[3];
                } else {
                    for (int i = left; i <= right; i++) {
                        double weight = mitchellNetravali(
                                (i - center) / filterScale);
                        int clamped = Math.max(0, Math.min(i, srcWidth - 1));
                        int pixel = srcRow[clamped];
                        sumR += ((pixel >>> 16) & 0xFF) * weight;
                        sumG += ((pixel >>>  8) & 0xFF) * weight;
                        sumB += (pixel & 0xFF) * weight;
                        sumW += weight;
                    }
                }

                int r = clampByte(sumR / sumW);
                int g = clampByte(sumG / sumW);
                int b = clampByte(sumB / sumW);
                dstRow[x] = (r << 16) | (g << 8) | b;
            }
        });
    }

    /**
     * Resamples each column from {@code srcHeight} to {@code dstHeight}
     * pixels using the Mitchell–Netravali filter.
     */
    static void resampleVertical(int[][] src, int[][] dst,
                                 int srcHeight, int dstHeight,
                                 int cols) {
        double ratio = (double) srcHeight / dstHeight;
        double filterScale = Math.max(1.0, ratio);
        double support = 2.0 * filterScale;

        IntStream rowStream = IntStream.range(0, dstHeight);
        if ((long) cols * dstHeight > PARALLEL_THRESHOLD) {
            rowStream = rowStream.parallel();
        }

        rowStream.forEach(y -> {
            double center = (y + 0.5) * ratio - 0.5;
            int top = (int) Math.floor(center - support);
            int bottom = (int) Math.ceil(center + support);

            int[] dstRow = dst[y];
            final int laneCount = DOUBLE_SPECIES.length();
            for (int x = 0; x < cols; x++) {
                double sumR = 0, sumG = 0, sumB = 0, sumW = 0;

                if (laneCount >= 4) {
                    DoubleVector acc = DoubleVector.zero(DOUBLE_SPECIES);
                    for (int i = top; i <= bottom; i++) {
                        double weight = mitchellNetravali(
                                (i - center) / filterScale);
                        int clamped = Math.max(0, Math.min(i, srcHeight - 1));
                        int pixel = src[clamped][x];
                        double[] contrib = new double[laneCount];
                        contrib[0] = ((pixel >>> 16) & 0xFF) * weight;
                        contrib[1] = ((pixel >>>  8) & 0xFF) * weight;
                        contrib[2] = ( pixel         & 0xFF) * weight;
                        contrib[3] = weight;
                        acc = acc.add(DoubleVector.fromArray(DOUBLE_SPECIES, contrib, 0));
                    }
                    double[] result = new double[laneCount];
                    acc.intoArray(result, 0);
                    sumR = result[0]; sumG = result[1]; sumB = result[2]; sumW = result[3];
                } else {
                    for (int i = top; i <= bottom; i++) {
                        double weight = mitchellNetravali(
                                (i - center) / filterScale);
                        int clamped = Math.max(0, Math.min(i, srcHeight - 1));
                        int pixel = src[clamped][x];
                        sumR += ((pixel >>> 16) & 0xFF) * weight;
                        sumG += ((pixel >>>  8) & 0xFF) * weight;
                        sumB += (pixel & 0xFF) * weight;
                        sumW += weight;
                    }
                }

                int r = clampByte(sumR / sumW);
                int g = clampByte(sumG / sumW);
                int b = clampByte(sumB / sumW);
                dstRow[x] = (r << 16) | (g << 8) | b;
            }
        });
    }
}
