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

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import casciian.TApplication;
import casciian.TButton;
import casciian.THScroller;
import casciian.TText;
import casciian.TVScroller;
import casciian.TWidget;
import casciian.TWindow;
import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;
import casciian.bits.Rgb;
import casciian.event.TResizeEvent;

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
     * Number of warmup iterations.
     */
    private static final int WARMUP_ITERATIONS = 4;

    /**
     * Number of measured iterations.
     */
    private static final int MEASURED_ITERATIONS = 10;

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

        BenchmarkResult alpha = benchmark("alphaBlendOver",
            this::benchmarkVectorAlphaBlend, this::benchmarkScalarAlphaBlend);
        BenchmarkResult distance = benchmark("distanceSquaredSum",
            this::benchmarkVectorDistanceSquared, this::benchmarkScalarDistanceSquared);

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
            nanosToMillis(result.vectorNanos), result.vectorChecksum));
        sb.append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("scalarLine"),
            nanosToMillis(result.scalarNanos), result.scalarChecksum));
        sb.append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("speedupLine"),
            result.speedup()));
        sb.append("\n");
        sb.append(String.format(Locale.ROOT, i18n.getString("equalLine"),
            result.vectorChecksum == result.scalarChecksum
                ? i18n.getString("equalYes")
                : i18n.getString("equalNo")));
    }

    private BenchmarkResult benchmark(final String name,
                                      final LongSupplier vectorRun,
                                      final LongSupplier scalarRun) {
        long vectorChecksum = 0;
        long scalarChecksum = 0;
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            vectorChecksum ^= vectorRun.getAsLong();
            scalarChecksum ^= scalarRun.getAsLong();
        }

        long vectorStart = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            vectorChecksum ^= vectorRun.getAsLong();
        }
        long vectorNanos = System.nanoTime() - vectorStart;

        long scalarStart = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            scalarChecksum ^= scalarRun.getAsLong();
        }
        long scalarNanos = System.nanoTime() - scalarStart;

        return new BenchmarkResult(name, vectorNanos, scalarNanos,
            vectorChecksum, scalarChecksum);
    }

    private long benchmarkVectorAlphaBlend() {
        ArrayImageRGB under = benchmarkData.newUnderImage();
        under.alphaBlendOver(benchmarkData.overlay, benchmarkData.alpha);
        return checksum(under);
    }

    private long benchmarkScalarAlphaBlend() {
        ArrayImageRGB under = benchmarkData.newUnderImage();
        scalarAlphaBlend(under, benchmarkData.overlay, benchmarkData.alpha);
        return checksum(under);
    }

    private long benchmarkVectorDistanceSquared() {
        return Rgb.distanceSquaredSum(benchmarkData.pixels,
            benchmarkData.pixels.length, benchmarkData.referenceColor);
    }

    private long benchmarkScalarDistanceSquared() {
        return scalarDistanceSquaredSum(benchmarkData.pixels,
            benchmarkData.pixels.length, benchmarkData.referenceColor);
    }

    private static double nanosToMillis(final long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long checksum(final ImageRGB image) {
        long sum = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                sum = (sum * 1_000_003L) ^ image.getRGB(x, y);
            }
        }
        return sum;
    }

    private static void scalarAlphaBlend(final ArrayImageRGB under,
                                         final ImageRGB over,
                                         final double alpha) {
        int width = under.getWidth();
        int height = under.getHeight();
        int a = (int) (alpha * 256);
        int oneMinusA = 256 - a;

        int[] underRow = new int[width];
        int[] overRow = new int[width];
        for (int y = 0; y < height; y++) {
            under.getRGB(0, y, width, 1, underRow, 0, width);
            over.getRGB(0, y, width, 1, overRow, 0, width);
            for (int x = 0; x < width; x++) {
                int underPixel = underRow[x];
                int overPixel = overRow[x];
                int red = (((underPixel >>> 16) & 0xFF) * oneMinusA
                    + ((overPixel >>> 16) & 0xFF) * a) >> 8;
                int green = (((underPixel >>> 8) & 0xFF) * oneMinusA
                    + ((overPixel >>> 8) & 0xFF) * a) >> 8;
                int blue = ((underPixel & 0xFF) * oneMinusA
                    + (overPixel & 0xFF) * a) >> 8;
                underRow[x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
            under.setRGB(0, y, width, 1, underRow, 0, width);
        }
    }

    private static long scalarDistanceSquaredSum(final int[] pixels,
                                                 final int count,
                                                 final int color) {
        long sum = 0;
        for (int i = 0; i < count; i++) {
            int px = pixels[i];
            int dr = ((px >>> 16) & 0xFF) - ((color >>> 16) & 0xFF);
            int dg = ((px >>> 8) & 0xFF) - ((color >>> 8) & 0xFF);
            int db = (px & 0xFF) - (color & 0xFF);
            sum += dr * dr + dg * dg + db * db;
        }
        return sum;
    }

    private static final class BenchmarkData {
        private final int[] underPixels;
        private final ArrayImageRGB overlay;
        private final int[] pixels;
        private final int referenceColor;
        private final double alpha;

        private BenchmarkData() {
            underPixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
            pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
            int[] overlayPixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
            for (int y = 0; y < IMAGE_HEIGHT; y++) {
                for (int x = 0; x < IMAGE_WIDTH; x++) {
                    int index = y * IMAGE_WIDTH + x;
                    underPixels[index] = 0xFF000000
                        | ((x * 255 / Math.max(1, IMAGE_WIDTH - 1)) << 16)
                        | ((y * 255 / Math.max(1, IMAGE_HEIGHT - 1)) << 8)
                        | ((x * 13 + y * 7) & 0xFF);
                    overlayPixels[index] = 0xFF000000
                        | (((x * 5 + y * 3) & 0xFF) << 16)
                        | (((x * 11 + y * 17) & 0xFF) << 8)
                        | ((x * 19 + y * 23) & 0xFF);
                    pixels[index] = overlayPixels[index] ^ 0x00112233;
                }
            }
            overlay = new ArrayImageRGB(IMAGE_WIDTH, IMAGE_HEIGHT);
            overlay.setRGB(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, overlayPixels, 0,
                IMAGE_WIDTH);
            referenceColor = 0x005A7FC3;
            alpha = 0.375;
        }

        private ArrayImageRGB newUnderImage() {
            ArrayImageRGB image = new ArrayImageRGB(IMAGE_WIDTH, IMAGE_HEIGHT);
            image.setRGB(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT,
                Arrays.copyOf(underPixels, underPixels.length), 0, IMAGE_WIDTH);
            return image;
        }
    }

    private record BenchmarkResult(String name, long vectorNanos,
                                   long scalarNanos, long vectorChecksum,
                                   long scalarChecksum) {
        private double speedup() {
            if (vectorNanos == 0) {
                return 0.0;
            }
            return (double) scalarNanos / (double) vectorNanos;
        }
    }

    @FunctionalInterface
    private interface LongSupplier {
        long getAsLong();
    }
}
