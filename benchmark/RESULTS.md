# Edge performance benchmark results

Every entry records the noise band it was judged against. A change smaller than the
band is reported as "within noise", never as a win.

## Method

- One fresh JVM per run; variants alternate A/B/A/B rather than running in blocks,
  so thermal drift over a series is not charged to whichever variant ran second.
- The first pass of every series is discarded (cold OS page cache, GPU clock ramp).
- Phases are wall-clock: settle → warmup → 1s discard → measure. A faster build
  renders more frames of the same workload rather than covering it faster.
- The first second of every measurement window is discarded: `getDebugFPS()` reads 0
  until its first flush and feeds the per-frame chunk-upload budget in
  `EntityRenderer.updateCameraAndRender`, so those frames are not comparable.
- Video settings are pinned (`benchmark/options.benchmark.txt`). `maxFps` is 260,
  which is Unlimited in 1.8.9, so `Display.sync` — a `Thread.yield` busy-spin that
  also permanently raises the process timer resolution — never runs.
- `avg fps` is frames divided by elapsed time. `1% low` is the mean of the slowest
  1% of frames. `0.1% low` is only computed above 20k pooled frames.

## Environment

| | |
| --- | --- |
| CPU | AMD Ryzen 9 8940HX (16C/32T), laptop |
| GPU | NVIDIA GeForce RTX 5060 Laptop, driver 596.13 |
| GPU clocks | pinned to 1800 MHz for the session (`benchmark/clock-lock.ps1`) |
| Power plan | High performance for the session |
| RAM | 32 GB |
| OS | Windows 11, build 10.0.26200 |
| Client JVM | Temurin 8u472, `-Xms2G -Xmx2G -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+ForceTimeHighResolution` |
| Resolution | 1280x720 windowed |

A run is only comparable to others with the same `GL_RENDERER`; `compare.py` refuses
to compare across different ones. This machine has a second GPU (Radeon 610M) and two
virtual display adapters, so the check is not theoretical.

## Noise floor

### menu (no world, GPU-trivial) — 3 runs per side, same build both sides

| metric | spread between identical variants |
| --- | ---: |
| p50 frame time | 0.07% |
| p99 frame time | 2.21% |
| avg fps | 0.26% |
| 1% low | 3.31% |
| **0.1% low** | **16.05%** |

Measured before GPU clocks were pinned. The 0.1% low figure is the important one:
with 52k pooled frames and *no code difference at all*, it moved 16%. Any claim
resting on 0.1% low needs far more data than a normal series produces.

### flat-orbit (superflat, orbiting camera) — noise band used for in-world verdicts

Pending: see `benchmark/results/noise-flat-orbit/`.

## Touching the window destroys the tail statistics

The first in-world noise series looked unusable:

| metric | as measured | with the disturbed pass removed |
| --- | ---: | ---: |
| p50 | 6.19% | 3.18% |
| p99 | 14.25% | **1.88%** |
| avg fps | 7.26% | 2.95% |
| 1% low | 32.17% | **3.17%** |

Two of the six runs contained exactly one frame of 560ms and 230ms respectively.
Every other frame in those runs was under 8.3ms. The cause was the window being
dragged: Windows enters a modal move loop, the application stops pumping messages,
and the frame spanning the drag is recorded as one enormous sample.

One such frame in 60,000 was enough to move the 1% low by 32% and the p99 by 14%.
Judged against the contaminated band, no optimisation short of a rewrite could ever
have been called an improvement.

Two guards now exist, because an unattended series has no other way to notice:

- `DisplayWatch` excludes frames where the window moved, was resized, lost focus or
  became invisible, plus one recovery frame. The count is always reported, including
  when it is zero — a silently dropped frame is indistinguishable from a clean run.
- `noise-band.py` refuses to derive a band from any run containing a frame over 20x
  the median, and `compare.py` warns on one. A band measured from a disturbed run
  would hide every real regression underneath it.

## Runs

Pending.
