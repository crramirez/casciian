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
package casciian.image.encoders;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import casciian.bits.ArrayImageRGB;
import casciian.bits.ImageRGB;

/**
 * Reproducible micro-benchmark for {@link PngImageEncoder} compression
 * levels and row filter strategies.  It is not a unit test; run it with
 * {@code ./gradlew pngBenchmark} (from the {@code code} directory).
 *
 * <p>All images are generated deterministically from fixed seeds, so the
 * output sizes are exactly reproducible; timings depend on the machine.
 * Each configuration is warmed up before measurement, and every encode is
 * timed individually to report the average, median, and 95th percentile.
 * Allocated bytes per encode are read from
 * {@code com.sun.management.ThreadMXBean} when the JVM supports it.</p>
 *
 * <p>Arguments (all optional):</p>
 * <ul>
 * <li>{@code --levels=1,3,6} compression levels</li>
 * <li>{@code --filters=PAETH,ADAPTIVE} filter strategies</li>
 * <li>{@code --images=flat-cell,photo-large} image names (default all)</li>
 * <li>{@code --warmup-ms=300} warm-up time per configuration</li>
 * <li>{@code --measure-ms=700} minimum measurement time per configuration</li>
 * <li>{@code --min-reps=30} minimum measured encodes per configuration</li>
 * <li>{@code --encoder-class=fqcn} benchmark another {@link ImageEncoder}
 *     (no-arg constructor) instead of the level/filter matrix</li>
 * </ul>
 */
public final class PngEncoderBenchmark {

    /**
     * A benchmark image.
     *
     * @param name the image name
     * @param description what the image represents
     * @param image the pixels
     * @param alpha whether to encode with alpha
     */
    private record Sample(String name, String description, ImageRGB image,
        boolean alpha) {
    }

    /**
     * One measured configuration.
     *
     * @param sample the image
     * @param config the encoder configuration label
     * @param size encoded bytes
     * @param avgNanos average time
     * @param medianNanos median time
     * @param p95Nanos 95th percentile time
     * @param allocBytes allocated bytes per encode, or -1
     * @param reps number of measured encodes
     */
    private record Result(Sample sample, String config, int size,
        double avgNanos, long medianNanos, long p95Nanos, long allocBytes,
        int reps) {
    }

    private PngEncoderBenchmark() {
    }

    /**
     * Run the benchmark.
     *
     * @param args see the class documentation
     * @throws Exception on failure
     */
    public static void main(final String[] args) throws Exception {
        Map<String, String> opts = new LinkedHashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] kv = arg.substring(2).split("=", 2);
                opts.put(kv[0], kv[1]);
            }
        }
        long warmupNanos = Long.parseLong(opts.getOrDefault("warmup-ms",
            "300")) * 1_000_000L;
        long measureNanos = Long.parseLong(opts.getOrDefault("measure-ms",
            "700")) * 1_000_000L;
        int minReps = Integer.parseInt(opts.getOrDefault("min-reps", "30"));

        List<Sample> samples = samples();
        if (opts.containsKey("images")) {
            List<String> wanted = Arrays.asList(opts.get("images").split(","));
            samples.removeIf(s -> !wanted.contains(s.name()));
        }

        Map<String, Supplier<ImageEncoder>> configs = new LinkedHashMap<>();
        if (opts.containsKey("encoder-class")) {
            String className = opts.get("encoder-class");
            Class<?> type = Class.forName(className);
            configs.put(type.getSimpleName(), () -> {
                try {
                    return (ImageEncoder) type.getDeclaredConstructor()
                        .newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            });
        } else {
            for (String level : opts.getOrDefault("levels", "1,3,6")
                     .split(",")) {
                for (String filter : opts.getOrDefault("filters",
                         "PAETH,ADAPTIVE").split(",")) {
                    int lvl = Integer.parseInt(level.trim());
                    PngImageEncoder.FilterStrategy strategy =
                        PngImageEncoder.FilterStrategy.valueOf(
                            filter.trim().toUpperCase(Locale.ROOT));
                    configs.put("L" + lvl + " " + strategy,
                        () -> new PngImageEncoder(lvl, strategy));
                }
            }
        }

        System.out.printf("# PNG encoder benchmark%n");
        System.out.printf("# %s %s, %s, %d CPUs%n",
            System.getProperty("java.vm.name"),
            System.getProperty("java.version"),
            System.getProperty("os.name") + " " + System.getProperty("os.arch"),
            Runtime.getRuntime().availableProcessors());
        System.out.printf("# warmup=%dms measure>=%dms minReps=%d%n%n",
            warmupNanos / 1_000_000, measureNanos / 1_000_000, minReps);
        System.out.println("| image | pixels | config | bytes | base64 "
            + "| avg us | median us | p95 us | MB/s | alloc KiB/op | reps |");
        System.out.println("|---|---|---|---:|---:|---:|---:|---:|---:|---:"
            + "|---:|");
        for (Sample sample : samples) {
            for (Map.Entry<String, Supplier<ImageEncoder>> config
                     : configs.entrySet()) {
                print(run(sample, config.getKey(), config.getValue().get(),
                    warmupNanos, measureNanos, minReps));
            }
        }
    }

    /**
     * Measure one configuration.
     */
    private static Result run(final Sample sample, final String config,
        final ImageEncoder encoder, final long warmupNanos,
        final long measureNanos, final int minReps) throws IOException {

        byte[] encoded = encoder.encode(sample.image(), sample.alpha());
        long sink = encoded.length;
        long end = System.nanoTime() + warmupNanos;
        while (System.nanoTime() < end) {
            sink += encoder.encode(sample.image(), sample.alpha()).length;
        }

        long[] times = new long[1024];
        int reps = 0;
        long allocBefore = allocatedBytes();
        long start = System.nanoTime();
        while ((reps < minReps) || (System.nanoTime() - start < measureNanos)) {
            long t0 = System.nanoTime();
            sink += encoder.encode(sample.image(), sample.alpha()).length;
            long t1 = System.nanoTime();
            if (reps == times.length) {
                times = Arrays.copyOf(times, reps * 2);
            }
            times[reps++] = t1 - t0;
        }
        long allocAfter = allocatedBytes();
        if (sink == 42) {
            System.out.print("");
        }
        long[] sorted = Arrays.copyOf(times, reps);
        Arrays.sort(sorted);
        double avg = Arrays.stream(sorted).average().orElse(0);
        long median = sorted[reps / 2];
        long p95 = sorted[Math.min(reps - 1, (int) Math.ceil(reps * 0.95) - 1)];
        long alloc = (allocBefore < 0) ? -1
            : (allocAfter - allocBefore) / reps;
        return new Result(sample, config, encoded.length, avg, median, p95,
            alloc, reps);
    }

    /**
     * Print one result row as Markdown.
     */
    private static void print(final Result r) {
        ImageRGB image = r.sample().image();
        long rawBytes = (long) image.getWidth() * image.getHeight()
            * (r.sample().alpha() ? 4 : 3);
        double mbPerSec = rawBytes / (r.medianNanos() / 1e9) / 1e6;
        System.out.printf(Locale.ROOT,
            "| %s | %dx%d%s | %s | %d | %d | %.1f | %.1f | %.1f | %.0f | %s "
                + "| %d |%n",
            r.sample().name(), image.getWidth(), image.getHeight(),
            r.sample().alpha() ? " RGBA" : "", r.config(), r.size(),
            base64Size(r.size()), r.avgNanos() / 1e3, r.medianNanos() / 1e3,
            r.p95Nanos() / 1e3, mbPerSec,
            r.allocBytes() < 0 ? "n/a"
                : String.format(Locale.ROOT, "%.1f", r.allocBytes() / 1024.0),
            r.reps());
    }

    /**
     * Size of the base64 text sent to the terminal.
     */
    private static long base64Size(final int bytes) {
        return 4L * ((bytes + 2) / 3);
    }

    /**
     * Bytes allocated by the current thread, or -1 if unsupported.
     */
    private static long allocatedBytes() {
        if (ManagementFactory.getThreadMXBean()
            instanceof com.sun.management.ThreadMXBean bean
            && bean.isThreadAllocatedMemorySupported()) {
            return bean.getCurrentThreadAllocatedBytes();
        }
        return -1;
    }

    // ------------------------------------------------------------------------
    // Sample images ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Build the deterministic sample images.  Cells are 8x16 pixels, the
     * default text cell size; a "run" is a horizontal row of image cells
     * as sent by the iTerm2 protocol.
     */
    private static List<Sample> samples() {
        List<Sample> list = new ArrayList<>();
        list.add(new Sample("flat-cell", "single flat-color cell",
            flat(8, 16), false));
        list.add(new Sample("flat-run", "160-cell flat-color run",
            flat(1280, 16), false));
        list.add(new Sample("text-cell", "one glyph-like cell with border",
            text(8, 16, 1), false));
        list.add(new Sample("text-run", "160-cell window border + text run",
            text(1280, 16, 2), false));
        list.add(new Sample("text-window", "640x400 window with text",
            text(640, 400, 3), false));
        list.add(new Sample("icon-alpha", "64x64 anti-aliased icon, RGBA",
            icon(64), true));
        list.add(new Sample("gradient-run", "160-cell gradient run",
            gradient(1280, 16), false));
        list.add(new Sample("gradient-large", "640x400 2-D gradient",
            gradient(640, 400), false));
        list.add(new Sample("photo-cell", "photo-like cell", photo(8, 16, 4),
            false));
        list.add(new Sample("photo-run", "160-cell photo-like run",
            photo(1280, 16, 5), false));
        list.add(new Sample("photo-large", "640x400 photo-like image",
            photo(640, 400, 6), false));
        list.add(new Sample("noise-large", "640x400 uniform noise",
            noise(640, 400, 7), false));
        return list;
    }

    private static ImageRGB flat(final int w, final int h) {
        ImageRGB image = new ArrayImageRGB(w, h);
        image.fillRect(0, 0, w, h, 0xFF0000AA);
        return image;
    }

    /**
     * A TUI window: flat background, a double-line border, and 8x16 cells
     * filled with pseudo-glyphs (random 1-pixel strokes in a 6x10 box).
     */
    private static ImageRGB text(final int w, final int h, final long seed) {
        Random random = new Random(seed);
        int[] px = new int[w * h];
        Arrays.fill(px, 0xFF0000AA);
        for (int cy = 0; cy + 16 <= h; cy += 16) {
            for (int cx = 0; cx + 8 <= w; cx += 8) {
                if (random.nextInt(4) == 0) {
                    continue;
                }
                int fg = random.nextBoolean() ? 0xFFFFFFFF : 0xFFFFFF55;
                for (int s = 0; s < 4; s++) {
                    boolean vertical = random.nextBoolean();
                    int x0 = cx + 1 + random.nextInt(6);
                    int y0 = cy + 3 + random.nextInt(10);
                    int len = 2 + random.nextInt(6);
                    for (int i = 0; i < len; i++) {
                        int x = vertical ? x0 : Math.min(cx + 6, x0 + i);
                        int y = vertical ? Math.min(cy + 12, y0 + i) : y0;
                        px[y * w + x] = fg;
                    }
                }
            }
        }
        for (int x = 0; x < w; x++) {
            px[x] = 0xFFC0C0C0;
            px[2 * w + x] = 0xFFC0C0C0;
            px[(h - 1) * w + x] = 0xFFC0C0C0;
        }
        for (int y = 0; y < h; y++) {
            px[y * w] = 0xFFC0C0C0;
            px[y * w + w - 1] = 0xFFC0C0C0;
        }
        return image(w, h, px);
    }

    private static ImageRGB icon(final int size) {
        int[] px = new int[size * size];
        double c = (size - 1) / 2.0;
        double radius = size * 0.42;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x - c, y - c);
                int a = (int) Math.round(255 * Math.max(0,
                    Math.min(1, radius - d + 0.5)));
                int r = 40 + (x * 200) / size;
                int g = 180 - (y * 120) / size;
                int b = (d < radius * 0.5) ? 240 : 90;
                px[y * size + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return image(size, size, px);
    }

    private static ImageRGB gradient(final int w, final int h) {
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = (x * 255) / Math.max(1, w - 1);
                int g = (y * 255) / Math.max(1, h - 1);
                int b = 255 - ((x + y) * 255) / Math.max(1, w + h - 2);
                px[y * w + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        return image(w, h, px);
    }

    /**
     * Photo-like content: smooth low-frequency structure plus fine
     * Gaussian sensor-like noise.  No real photograph is bundled with the
     * repository, so this is a synthetic stand-in.
     */
    private static ImageRGB photo(final int w, final int h, final long seed) {
        Random random = new Random(seed);
        double[] phase = new double[9];
        for (int i = 0; i < phase.length; i++) {
            phase[i] = random.nextDouble() * Math.PI * 2;
        }
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int[] rgb = new int[3];
                for (int ch = 0; ch < 3; ch++) {
                    double v = 128
                        + 60 * Math.sin(x / 37.0 + phase[ch * 3])
                        + 40 * Math.sin(y / 23.0 + phase[ch * 3 + 1])
                        + 25 * Math.sin((x + y) / 11.0 + phase[ch * 3 + 2])
                        + random.nextGaussian() * 4;
                    rgb[ch] = Math.max(0, Math.min(255, (int) v));
                }
                px[y * w + x] = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8)
                    | rgb[2];
            }
        }
        return image(w, h, px);
    }

    private static ImageRGB noise(final int w, final int h, final long seed) {
        Random random = new Random(seed);
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            px[i] = 0xFF000000 | random.nextInt(0x1000000);
        }
        return image(w, h, px);
    }

    private static ImageRGB image(final int w, final int h, final int[] px) {
        ImageRGB image = new ArrayImageRGB(w, h);
        image.setRGB(0, 0, w, h, px, 0, w);
        return image;
    }
}
