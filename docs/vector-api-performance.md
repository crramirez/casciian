# Vector API kernel performance

This note records the baseline numbers for the SIMD kernels in
`casciian.bits`, so future changes to them can be compared against a known
starting point.

## What is vectorized

| Kernel | Status |
| ------ | ------ |
| `ArrayImageRGB.alphaBlendOver` (row blend) | Vector API (`IntVector`) |
| `Rgb.distanceSquaredSum` | Vector API (`IntVector`), single accumulator reduced once per block |
| `ScaleImageUtils.resampleHorizontal` / `resampleVertical` | Scalar |

`ScaleImageUtils` is deliberately **not** vectorized. Packing the four
`[R, G, B, W]` accumulators into one `DoubleVector` allocated two `double[]`
per innermost iteration and measured 2.1–2.7× *slower* than the scalar
accumulators. Vectorizing resampling usefully would require working across
the x axis with precomputed weight tables, which is a separate, larger piece
of work.

## Measuring

The demo application has a **Vector performance** window
(`demo.DemoVectorPerformanceWindow`) that compares each kernel against a
scalar baseline. The harness:

* prepares (restores) buffers and computes result checksums **outside** the
  timed region, so only the kernel is measured;
* runs the scalar baseline over the same `int[][]` buffers with the same
  driver as the vector path, so the ratio isolates SIMD rather than array
  copying or threading;
* warms up for at least 2 000 iterations and 750 ms before measuring;
* reports absolute ns/element for both paths in addition to the ratio, so a
  regression in either path is visible.

## Baseline

JVM: Temurin 25, `--add-modules jdk.incubator.vector`.
Host: AMD EPYC 9V74 (AVX2, no AVX-512), Linux x86_64.
Data: 320×180 pixels per iteration.

| Kernel | Vector | Scalar | Speedup |
| ------ | ------ | ------ | ------- |
| Alpha blend row kernel | 0.34 ns/px | 0.40 ns/px | 1.16× |
| `distanceSquaredSum` | 0.19 ns/px | 0.41 ns/px | 2.16× |

## Native image build flags

The GraalVM build no longer passes `-Os` or `-march=compatibility` by
default:

* `-Os` optimizes for binary size and costs a large amount of throughput in
  these kernels. Opt in with `-PnativeOptimizeForSize=true`.
* `-march=compatibility` caps generated code at SSE2, which throttles the
  Vector API kernels and makes native builds useless as a Vector API
  baseline. The native-image default (`x86-64-v3` on x86_64, i.e. AVX2) is
  used instead. For broad-hardware builds, pass an explicit target such as
  `-PnativeMarch=x86-64-v2` or `-PnativeMarch=compatibility` — and treat any
  performance numbers from such a build as an SSE-era baseline, not as
  evidence about the Vector API.

Native numbers should be re-measured with the same demo window on a machine
with GraalVM installed and recorded in the table above.
