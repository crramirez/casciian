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

import java.util.Locale;
import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.THScroller;
import casciian.TText;
import casciian.TVScroller;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.Rgb;
import casciian.event.TResizeEvent;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import static casciian.TCommand.*;
import static casciian.TKeypress.*;

/**
 * Demo window that compares vectorized and preserved scalar image kernels.
 */
public class DemoVectorPerformanceWindow extends TWindow {

    /**
     * The name of the resource bundle for this class.
     */
    public static final String RESOURCE_BUNDLE_NAME =
        DemoVectorPerformanceWindow.class.getName() + "Bundle";

    /**
     * Benchmark image width.
     */
    private static final int IMAGE_WIDTH = 320;

    /**
     * Benchmark image height.
     */
    private static final int IMAGE_HEIGHT = 180;

    /**
     * Minimum number of warmup iterations. C2 needs far more than a handful
     * of invocations to compile and stabilize these loops, so the warmup is
     * both iteration- and time-bounded.
     */
    private static final int MIN_WARMUP_ITERATIONS = 2_000;

    /**
     * Minimum warmup duration, in nanoseconds.
     */
    private static final long WARMUP_NANOS = 750_000_000L;

    /**
     * Number of measured iterations.
     */
    private static final int MEASURED_ITERATIONS = 200;

    /**
     * Localized strings.
     */
    private final ResourceBundle i18n;

    /**
     * Results view.
     */
    private final TText resultsText;

    /**
     * Shared benchmark dataset.
     */
    private final BenchmarkData benchmarkData;

    /**
     * Sink for kernel results, to keep the JIT from eliminating the work.
     */
    @SuppressWarnings("unused")
    private volatile long sink;

    /**
     * Construct the comparison window.
     *
     * @param parent the main application
     */
    @SuppressWarnings("this-escape")
    public DemoVectorPerformanceWindow(final TApplication parent) {
        super(parent, "", 0, 0, 76, 22, CENTERED | RESIZABLE);

        i18n = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, getLocale());
        setTitle(i18n.getString("windowTitle"));
        setMinimumWindowHeight(14);
        setMinimumWindowWidth(56);

        benchmarkData = new BenchmarkData();

        TWidget button = addButton(i18n.getString("runButton"), 2, 1,
            this::runBenchmarks);
        button = addButton(i18n.getString("closeButton"),
            button.getX() + button.getWidth() + 2, 1,
            () -> getApplication().closeWindow(this));

        addLabel(i18n.getString("description"), 2, 3, "ttext", false);
        resultsText = addText(i18n.getString("initialText"), 2, 5,
            getWidth() - 4, getHeight() - 7);
        hideScrollbars(resultsText);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbF2, cmShell,
            i18n.getString("statusBarShell"));
        statusBar.addShortcutKeypress(kbF3, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbF10, cmExit,
            i18n.getString("statusBarExit"));
    }

    @Override
    public void onResize(final TResizeEvent event) {
        super.onResize(event);
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            TResizeEvent textSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, Math.max(10, event.getWidth() - 4),
                Math.max(4, event.getHeight() - 7));
            resultsText.onResize(textSize);
        }
    }

    private void hideScrollbars(final TWidget widget) {
        for (TWidget child: widget.getChildren()) {
            if ((child instanceof THScroller) || (child instanceof TVScroller)) {
                child.setVisible(false);
            }
        }
    }

    private void runBenchmarks() {
        StringBuilder sb = new StringBuilder();
        sb.append(i18n.getString("runningText"));
        resultsText.setText(sb.toString());
        getApplication().doRepaint();

        int alphaElements = IMAGE_WIDTH * IMAGE_HEIGHT;
        BenchmarkResult alpha = benchmark(alphaElements,
            new AlphaBlendKernel(true), new AlphaBlendKernel(false));
        BenchmarkResult distance = benchmark(benchmarkData.pixels.length,
            new DistanceKernel(true), new DistanceKernel(false));

        sb.setLength(0);
        appendBenchmark(sb, alpha, i18n.getString("alphaTitle"));
        sb.append("\n\n");
        appendBenchmark(sb, distance, i18n.getString("distanceTitle"));
        resultsText.setText(sb.toString());
    }

    private void appendBenchmark(final StringBuilder sb,
                                 final BenchmarkResult result,
                                 final String title) {
        sb.append(title).append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("vectorLine"),
            nanosToMillis(result.vectorNanos),
            result.nanosPerElement(result.vectorNanos)));
        sb.append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("scalarLine"),
            nanosToMillis(result.scalarNanos),
            result.nanosPerElement(result.scalarNanos)));
        sb.append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("speedupLine"),
            result.speedup()));
        sb.append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("equalLine"),
            result.vectorChecksum == result.scalarChecksum
                ? i18n.getString("equalYes")
                : i18n.getString("equalNo")));
    }

    /**
     * Run one kernel pair. Only the kernel itself is timed: buffer
     * preparation and result checksums happen outside the clock.
     *
     * @param elements the number of elements processed per iteration
     * @param vectorKernel the vectorized kernel
     * @param scalarKernel the scalar kernel
     * @return the measurement
     */
    private BenchmarkResult benchmark(final int elements,
                                      final Kernel vectorKernel,
                                      final Kernel scalarKernel) {

        warmup(vectorKernel);
        warmup(scalarKernel);

        long vectorNanos = measure(vectorKernel);
        long scalarNanos = measure(scalarKernel);

        return new BenchmarkResult(elements, vectorNanos, scalarNanos,
            vectorKernel.checksum(), scalarKernel.checksum());
    }

    private void warmup(final Kernel kernel) {
        long start = System.nanoTime();
        int iterations = 0;
        while ((iterations < MIN_WARMUP_ITERATIONS)
            || (System.nanoTime() - start < WARMUP_NANOS)
        ) {
            kernel.prepare();
            sink ^= kernel.run();
            iterations++;
        }
    }

    private long measure(final Kernel kernel) {
        long total = 0;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            kernel.prepare();
            long start = System.nanoTime();
            long value = kernel.run();
            total += System.nanoTime() - start;
            sink ^= value;
        }
        return total;
    }

    private static double nanosToMillis(final long nanos) {
        return nanos / 1_000_000.0;
    }

    /**
     * A benchmarkable kernel. {@link #prepare()} restores mutable state and
     * {@link #checksum()} verifies the result; neither is timed.
     */
    private interface Kernel {

        /**
         * Restore any state the kernel mutates. Called outside the clock.
         */
        void prepare();

        /**
         * Run the kernel once. This is the only timed call.
         *
         * @return a cheap value derived from the work, to prevent
         * dead-code elimination
         */
        long run();

        /**
         * Compute a checksum of the last result. Called outside the clock.
         *
         * @return the checksum
         */
        long checksum();
    }

    /**
     * Alpha blending kernel. Both variants use the same direct
     * {@code int[][]} access and the same (sequential) driver, so the only
     * difference measured is the row kernel itself.
     */
    private final class AlphaBlendKernel implements Kernel {

        private final boolean vectorized;
        private final int[][] under = new int[IMAGE_HEIGHT][IMAGE_WIDTH];

        private AlphaBlendKernel(final boolean vectorized) {
            this.vectorized = vectorized;
        }

        @Override
        public void prepare() {
            for (int y = 0; y < IMAGE_HEIGHT; y++) {
                System.arraycopy(benchmarkData.underRows[y], 0, under[y], 0,
                    IMAGE_WIDTH);
            }
        }

        @Override
        public long run() {
            int a = (int) (benchmarkData.alpha * 256);
            int oneMinusA = 256 - a;
            for (int y = 0; y < IMAGE_HEIGHT; y++) {
                if (vectorized) {
                    blendRowVector(under[y], benchmarkData.overlayRows[y],
                        IMAGE_WIDTH, a, oneMinusA);
                } else {
                    blendRowScalar(under[y], benchmarkData.overlayRows[y],
                        IMAGE_WIDTH, a, oneMinusA);
                }
            }
            return under[IMAGE_HEIGHT - 1][IMAGE_WIDTH - 1];
        }

        @Override
        public long checksum() {
            long sum = 0;
            for (int[] row: under) {
                for (int pixel: row) {
                    sum = (sum * 1_000_003L) ^ pixel;
                }
            }
            return sum;
        }
    }

    /**
     * Distance kernel. Both variants read the same array; neither mutates
     * state, so {@link #prepare()} is a no-op.
     */
    private final class DistanceKernel implements Kernel {

        private final boolean vectorized;
        private long lastResult;

        private DistanceKernel(final boolean vectorized) {
            this.vectorized = vectorized;
        }

        @Override
        public void prepare() {
            // Nothing to restore.
        }

        @Override
        public long run() {
            long result;
            if (vectorized) {
                result = Rgb.distanceSquaredSum(benchmarkData.pixels,
                    benchmarkData.pixels.length, benchmarkData.referenceColor);
            } else {
                result = scalarDistanceSquaredSum(benchmarkData.pixels,
                    benchmarkData.pixels.length, benchmarkData.referenceColor);
            }
            lastResult = result;
            return result;
        }

        @Override
        public long checksum() {
            return lastResult;
        }
    }

    /**
     * Alpha-blend one row with the Java Vector API.
     */
    private static void blendRowVector(final int[] thisRow,
                                       final int[] overRow,
                                       final int width,
                                       final int a,
                                       final int oneMinusA) {

        final VectorSpecies<Integer> species = IntVector.SPECIES_PREFERRED;
        final int laneCount = species.length();
        final IntVector vA = IntVector.broadcast(species, a);
        final IntVector vOneMinusA = IntVector.broadcast(species, oneMinusA);
        final IntVector vMask8 = IntVector.broadcast(species, 0xFF);
        final IntVector vOpaque = IntVector.broadcast(species, 0xFF000000);

        int x = 0;
        for (; x <= width - laneCount; x += laneCount) {
            IntVector under = IntVector.fromArray(species, thisRow, x);
            IntVector over = IntVector.fromArray(species, overRow, x);

            IntVector red = under.lanewise(VectorOperators.LSHR, 16).and(vMask8)
                .mul(vOneMinusA)
                .add(over.lanewise(VectorOperators.LSHR, 16).and(vMask8).mul(vA))
                .lanewise(VectorOperators.LSHR, 8);
            IntVector green = under.lanewise(VectorOperators.LSHR, 8).and(vMask8)
                .mul(vOneMinusA)
                .add(over.lanewise(VectorOperators.LSHR, 8).and(vMask8).mul(vA))
                .lanewise(VectorOperators.LSHR, 8);
            IntVector blue = under.and(vMask8).mul(vOneMinusA)
                .add(over.and(vMask8).mul(vA))
                .lanewise(VectorOperators.LSHR, 8);

            vOpaque.or(red.lanewise(VectorOperators.LSHL, 16))
                .or(green.lanewise(VectorOperators.LSHL, 8))
                .or(blue)
                .intoArray(thisRow, x);
        }
        blendRangeScalar(thisRow, overRow, x, width, a, oneMinusA);
    }

    /**
     * Alpha-blend one row with scalar arithmetic.
     */
    private static void blendRowScalar(final int[] thisRow,
                                       final int[] overRow,
                                       final int width,
                                       final int a,
                                       final int oneMinusA) {
        blendRangeScalar(thisRow, overRow, 0, width, a, oneMinusA);
    }

    private static void blendRangeScalar(final int[] thisRow,
                                         final int[] overRow,
                                         final int from,
                                         final int to,
                                         final int a,
                                         final int oneMinusA) {
        for (int x = from; x < to; x++) {
            int underPixel = thisRow[x];
            int overPixel = overRow[x];
            int red = (((underPixel >>> 16) & 0xFF) * oneMinusA
                + ((overPixel >>> 16) & 0xFF) * a) >> 8;
            int green = (((underPixel >>> 8) & 0xFF) * oneMinusA
                + ((overPixel >>> 8) & 0xFF) * a) >> 8;
            int blue = ((underPixel & 0xFF) * oneMinusA
                + (overPixel & 0xFF) * a) >> 8;
            thisRow[x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
    }

    private static long scalarDistanceSquaredSum(final int[] pixels,
                                                 final int count,
                                                 final int color) {
        long sum = 0;
        int refR = (color >>> 16) & 0xFF;
        int refG = (color >>> 8) & 0xFF;
        int refB = color & 0xFF;
        for (int i = 0; i < count; i++) {
            int px = pixels[i];
            int dr = ((px >>> 16) & 0xFF) - refR;
            int dg = ((px >>> 8) & 0xFF) - refG;
            int db = (px & 0xFF) - refB;
            sum += dr * dr + dg * dg + db * db;
        }
        return sum;
    }

    private static final class BenchmarkData {
        private final int[][] underRows;
        private final int[][] overlayRows;
        private final int[] pixels;
        private final int referenceColor;
        private final double alpha;

        private BenchmarkData() {
            underRows = new int[IMAGE_HEIGHT][IMAGE_WIDTH];
            overlayRows = new int[IMAGE_HEIGHT][IMAGE_WIDTH];
            pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
            for (int y = 0; y < IMAGE_HEIGHT; y++) {
                for (int x = 0; x < IMAGE_WIDTH; x++) {
                    underRows[y][x] = 0xFF000000
                        | ((x * 255 / Math.max(1, IMAGE_WIDTH - 1)) << 16)
                        | ((y * 255 / Math.max(1, IMAGE_HEIGHT - 1)) << 8)
                        | ((x * 13 + y * 7) & 0xFF);
                    overlayRows[y][x] = 0xFF000000
                        | (((x * 5 + y * 3) & 0xFF) << 16)
                        | (((x * 11 + y * 17) & 0xFF) << 8)
                        | ((x * 19 + y * 23) & 0xFF);
                    pixels[y * IMAGE_WIDTH + x] =
                        overlayRows[y][x] ^ 0x00112233;
                }
            }
            referenceColor = 0x005A7FC3;
            alpha = 0.375;
        }
    }

    private record BenchmarkResult(int elements, long vectorNanos,
                                   long scalarNanos, long vectorChecksum,
                                   long scalarChecksum) {
        private double speedup() {
            if (vectorNanos == 0) {
                return 0.0;
            }
            return (double) scalarNanos / (double) vectorNanos;
        }

        private double nanosPerElement(final long nanos) {
            long processed = (long) elements * MEASURED_ITERATIONS;
            if (processed == 0) {
                return 0.0;
            }
            return (double) nanos / processed;
        }
    }
}
