# Edge performance benchmark results

Every entry records the noise band it was judged against. A change smaller than the
band is reported as "within noise", never as a win.

## Method

- One fresh JVM per run. Repeated measurements inside one VM invocation are not a
  substitute for repeated invocations: JIT state, heap layout and OS page cache all
  differ on a cold start.
- Variants alternate within a pass (A/B/A/B), and the comparison is on the **paired**
  difference. The A-vs-A series drifts across passes — 636.7, 642.9, 668.8 fps, with
  both variants moving together — and pairing cancels that. It halves the smallest
  detectable effect: p50 band 4.67% unpaired vs 1.98% paired.
- Series carry a duplicate baseline variant (`off` and `off2`) where practical, so the
  null band and the effect are measured under the same conditions in the same series.
- The first pass of every series is discarded (cold page cache, GPU clock ramp).
- Phases are wall-clock: settle → warmup → 1s discard → measure. A faster build renders
  more frames of the same workload rather than covering it faster.
- Measurement windows are a whole number of camera-path loops, so no run averages in a
  different fraction of the path.
- The first second of every measurement window is discarded: `getDebugFPS()` reads 0
  until its first flush and feeds the per-frame chunk-upload budget in
  `EntityRenderer.updateCameraAndRender`, so those frames are not comparable.
- Video settings are pinned (`benchmark/options.benchmark.txt`). `maxFps` is 260, which
  is Unlimited in 1.8.9, so `Display.sync` — a `Thread.yield` busy-spin that also
  permanently raises the process timer resolution — never runs.
- Scenarios pin time, weather, mob spawning and random ticks. Left alone a world drifts
  and two runs half an hour apart are not rendering the same thing.
- `avg fps` is frames divided by elapsed time. `1% low` is the mean of the slowest 1%
  of frames. `0.1% low` is only computed above 20k pooled frames.

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

A run is only comparable to others with the same `GL_RENDERER`; `compare.py` refuses to
compare across different ones. This machine has a second GPU (Radeon 610M) and two
virtual display adapters, so the check is not theoretical.

**The power plan matters more than expected.** Switching from Balanced to High
performance roughly doubled frame rate on the menu scenario (1732 to 3478 fps). Any
measurement taken before that change is not comparable to any taken after.

## Noise floor

### flat-orbit (superflat, orbiting camera), 6 runs / 3 pairs, same build both sides

| metric | paired band | per-pair differences |
| --- | ---: | --- |
| p50 frame time | 1.98% | −1.38%, −0.27%, +0.60% |
| p99 frame time | 4.97% | +0.71%, −4.26%, −0.37% |
| avg fps | 1.59% | +1.12%, +0.69%, −0.47% |
| 1% low | 4.13% | −2.51%, +1.63%, +1.15% |

Clean series: 0 disturbed and 0 unfocused frames in all six runs. Median 1.502ms p50,
645.6 avg fps.

## Touching the window destroys the tail statistics

The first in-world noise series looked unusable:

| metric | as measured | with the disturbed pass removed |
| --- | ---: | ---: |
| p50 | 6.19% | 3.18% |
| p99 | 14.25% | **1.88%** |
| avg fps | 7.26% | 2.95% |
| 1% low | 32.17% | **3.17%** |

Two of the six runs contained exactly one frame of 560ms and 230ms respectively. Every
other frame in those runs was under 8.3ms. The cause was the window being dragged:
Windows enters a modal move loop, the application stops pumping messages, and the frame
spanning the drag is recorded as one enormous sample.

One such frame in 60,000 was enough to move the 1% low by 32% and the p99 by 14%.
Judged against the contaminated band, no optimisation short of a rewrite could ever have
been called an improvement.

Two guards now exist, because an unattended series has no other way to notice:

- `DisplayWatch` excludes frames where the window moved, was resized or changed focus
  state, plus one recovery frame. The count is always reported, including when it is
  zero — a silently dropped frame is indistinguishable from a clean run. Being unfocused
  for a whole run is a steady state rather than a perturbation, so it is counted and
  reported but not excluded; an entity-dense run that lost focus halfway through showed
  no step change in frame time across the transition.
- `noise-band.py` refuses to derive a band from any run containing a frame over 20x the
  median, and `compare.py` warns on one. A band measured from a disturbed run would hide
  every real regression underneath it.

## Scenario authoring notes

Things that silently produced empty or wrong workloads during development:

- The superflat preset in use puts the top solid block at **y=6**. Scenarios originally
  placed props at y=64, leaving everything floating 58 blocks in the air.
- **Chat is capped at 100 characters**, so `/summon` with an NBT tag sent through
  `sendChatMessage` arrives truncated and the server rejects it with "unbalanced
  brackets" — the scenario comes up empty with no error anywhere obvious. Setup commands
  now go to the integrated server's command manager directly.
- Ambient block particles (lava, torches) come from `WorldClient.doVoidFogParticles`,
  which samples ~100 random positions per tick out of a 32³ volume, and `LowAnimationTick`
  cuts that tenfold. Far too sparse to build a particle workload from; potion particles
  emitted per entity per tick are dense and continuous instead.
- Counters must not be reset when the measurement window opens. Display-list compilation
  and resource-pack loading fire only during startup, so a windowed counter reads zero
  for a feature that is demonstrably working. Reports carry both the run total and the
  window delta.

## Runs

### R1 — Performance module semantics and dead settings

Correctness change, no timing claim. Verified with `switch-matrix.ps1` on
`LowAnimationTick` and `BatchModelRendering`: the counter reads zero with the module
off, zero with the sub-feature off, and non-zero with both on. Before the change, four
sub-features stayed active after the user disabled the module.

### R4 — particle frustum culling

**Verdict: within noise on every metric. Kept as a correctness-neutral change, not
claimed as a win.**

Scenario `particle-dense`, ~144 particles/frame of which culling skips ~11%.
Series carried a duplicate baseline (`off`, `off2`, `on`), so the band below was
measured in the same series under the same conditions as the effect.

| metric | off | on | paired diff | band (off vs off2) | verdict |
| --- | ---: | ---: | ---: | ---: | --- |
| p50 frame ms | 1.318 | 1.314 | −0.65% | 2.00% | within noise |
| p99 frame ms | 1.837 | 1.829 | −0.12% | 4.42% | within noise |
| avg fps | 743.9 | 747.5 | +0.55% | 2.54% | within noise |
| 1% low fps | 468.9 | 470.8 | +0.01% | 9.45% | within noise |

The in-series band lands within a few tenths of a percent of the independently
measured flat-orbit band (p50 2.00% vs 1.98%, p99 4.42% vs 4.97%), which is a useful
cross-check on the method.

Every metric moves in the favourable direction, but by far less than the band, so
none of it is distinguishable from noise. That is the expected outcome for this
change: a particle is four vertices, and only 11% of them are being skipped.
Whether it pays off on a workload with a much larger off-screen fraction is untested.

Screenshot gate: **pass**, 0.70% and 0.81% of pixels differing.

**The gate needed calibrating, and that is worth recording.** With a fixed 2% limit
the null control failed — two runs of the *same* configuration differed by 2.5% and
2.1% of pixels, purely from particle RNG, while the actual change differed by 0.7%.
A fixed threshold would have reported identical builds as a visual regression. The
limit is now derived from the null control the same way the timing band is, and the
tool warns when a scenario's own variation is large enough to make the gate weak.
A static scene gives a far stronger check than one full of random particles.
