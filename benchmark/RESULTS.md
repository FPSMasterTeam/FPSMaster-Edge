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

## Run order carried a systematic bias

An A-vs-A series on the rebuilt profiler produced paired differences that were not noise:
p50 −7.2%, −1.7%, −4.5% and avg fps +8.5%, +2.4%, +8.2%, the same direction every pass.
Two identical variants cannot differ systematically, so the bias was positional —
whichever ran second was consistently faster by 5-6%.

The cause was not established. Focus state correlates loosely and heap at sampling time
was higher on the slower side, but neither explains every pass. Rather than guess, the
order now alternates per pass, so each variant occupies each position equally and the
bias cancels in the pairing. Re-measured, the p50 paired differences become +1.66%,
−1.13%, +1.43%, +4.35% — mixed in sign, as noise should be.

**This corrected a band that was too optimistic.** Under fixed order the p50 band read
1.98%; counterbalanced it reads 5.48%. The earlier figure was tighter precisely because
the bias shifted every pair the same way — biased but consistent, which looks like
precision.

## Noise bands do not transfer between scenarios

Section bands measured on `flat-orbit` under counterbalanced order:

| section | band | band excluding unfocused runs | magnitude here |
| --- | ---: | ---: | --- |
| terrain | 4.50% | 2.05% | 412us |
| terrain setup | 3.15% | 3.15% | 376us |
| sky | 10.82% | 2.57% | 135us |
| entities | **20.85%** | 17.08% | near zero on this scenario |
| entity model, hud | 0.00% | 0.00% | zero on this scenario |

The entities band is 20.85% here and 6.83% on `entity-dense`, for the same code. A
superflat world with mob spawning off barely renders an entity, so the section is near
zero and relative noise explodes; on the entity-dense arena it is over 1000us and stable.

So a band is only meaningful for the scenario and magnitude it was measured at. Judging
an entity-dense result against a flat-orbit band would be meaningless in either
direction. The in-series duplicate baseline used from R4 onward — `off` and `off2` in the
same series as `on` — is the only form of this that is sound, and it is what the R4 and
R5 verdicts rest on.

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

## The profiler was inflating the stutter it measured

Analysing where slow frames come from — the reduce-stutter goal — turned up something
about the harness rather than about the client. On an `entity-dense` run, frames slower
than 1.5x the median accounted for **12.05% of the window**, and clustering them showed
79% of that excess sitting in a single run of 328 consecutive frames at the very start
of the measurement window. 328 frames at 281 fps is 1.17 seconds, well past the
one-second discard.

The cause was `BenchProfiler`'s own storage. Per-frame samples were nanoseconds in a
`long`, two arrays per section, 262144 entries each — at sixteen sections that is 67 MB
reserved but never written until recording began, so the first frames of every
measurement window paid the page faults.

Samples are now microseconds in an `int`, which halves the footprint and is ample
resolution for values in the tens to hundreds of microseconds, and the arrays are
touched when recording starts so the faults land in the warmup window. Re-measured:

| | before | after |
| --- | ---: | ---: |
| leading ramp | 328 frames / 1.17s | **0 frames** |
| ramp share of all excess | 79% | 0% |
| excess over 1.5x median | **12.05%** | **2.45%** |

**This affects earlier numbers.** Percentile and low-percentile figures from rounds run
with the expanded profiler were inflated by this ramp; medians were not, since a median
is insensitive to a leading tail. That is consistent with what the noise bands showed all
along — p50 held at around 2% while p99 sat near 5% — and it is why only p50 and section
medians were quoted for verdicts. Earlier rounds measured with five or ten sections had a
0.05-0.24s ramp contributing 0.3-6% of excess, so they are affected far less.

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

## GL resource leak detection

Heap figures cannot see driver-side leaks: display lists, textures and framebuffers live
in GL memory, so a client can leak them steadily while the Java heap looks flat. The
measurement window makes a usable test, because the scene is settled and the camera
repeats a fixed loop — a net rise in live GL objects across it is a leak, not churn.

Counters sit on `GLAllocation.generateDisplayLists` / `deleteDisplayLists` (both
overloads) and `TextureUtil.glGenTextures` / `deleteTexture`.

First result, `entity-dense`, 80s window over 29,418 frames:

| resource | allocated | released | net |
| --- | ---: | ---: | ---: |
| display lists | 0 | 0 | **0** |
| textures | 0 | 0 | **0** |

No leak in this scenario. Whole-run totals including startup: 30 display lists
allocated and none released — sky and chunk-container lists held for the session, not a
leak — and 46 textures allocated against 45 released.

A settled scene never exercises the paths where leaks actually happen, so this result
only says the steady state is clean. `BenchStress` fixes that by toggling features on a
timer during the measurement window; the `leak-stress` scenario cycles MotionBlur (whose
`ShaderGroup` reload once leaked framebuffers here), FontOptimize, BlockOverlay and
EntityCulling once a second.

Result over 120 cycles in 120s, 52,127 frames:

| resource | allocated | released | net |
| --- | ---: | ---: | ---: |
| display lists | 0 | 0 | **0** |
| textures | 253 | 253 | **0** |

**No leak, and the instrument is demonstrably sensitive** — 253 texture allocations
against zero in the static scenario shows the churn is being reached. Heap 802MB, 69 GC
collections over the window.

Frame times from a stress run are meaningless by construction, since the workload changes
mid-window; only the resource counters should be read from it.

## Patcher licence remediation

Edge is GPL-3.0. Patcher is CC BY-NC-SA 4.0, which is incompatible: the NonCommercial
clause is an added restriction GPL forbids, and ShareAlike requires derivatives to carry
BY-NC-SA. Seven files carried a `patcher$` member prefix, `FontRendererHook` still had
commented-out `Patcher.instance` logging calls, and it loaded
`/assets/patcher/font_glyph_data.bin` — a Patcher asset which is not in this repository,
so `Objects.requireNonNull` threw an NPE that the surrounding `catch (Exception ignored)`
swallowed on every launch.

Handled in order of exposure, since default-on code ships to everyone:

- **Model batching** (default on) rewritten. The batch-ownership flag lived as a field on
  every `TexturedQuad`, but it describes the current draw: once the outer batch is open,
  `isDrawing()` is true regardless of who opened it. It now lives on a `ModelBatching`
  helper. Verified identical: 0.008–0.159% of pixels against a 0.500% limit.
- **Font optimisation** (default off) replaced. 858 lines removed —
  `FontRendererHook` (610), `EnhancedFontRenderer` (111), `font/optimize/*` (140) — for a
  190-line `StringRenderCache`.
- Prefixes renamed, Patcher remnants deleted, README attribution corrected to state that
  the ideas were referenced and the code is not derived.

The font replacement drops the part that was not worth keeping. The original fused two
optimisations: a string cache, and a 4096x4224 atlas merging all 256 unicode pages to
avoid texture binds. That atlas is roughly 69 MB of video memory to save at most two
binds per string, which is the wrong trade for a client whose goals include reducing
memory. Only the cache is kept, and it gained proper LRU eviction — the original emptied
itself wholesale at 5000 entries, collapsing the hit rate periodically for exactly the
steady text that benefits most, and it dropped display lists without releasing them.

Measured on `text-dense` (sidebar scoreboard, deterministic unlike the F3 overlay whose
FPS and memory lines change every frame):

| | hud cpu p50 | cache hits | misses |
| --- | ---: | ---: | ---: |
| off | 86.2us | — | — |
| off2 | 85.2us | — | — |
| **on** | **75.1us** | 929,070 | 0 |

**−12.9% on the HUD section** against a 1.2% null difference, rendering identical at
0.003–0.034% against a 0.500% limit.

### GlyphCache and StringCache — compatible, attribution was the gap

These 1865 lines are **BetterFonts** lineage (thvortex), not Patcher, and BetterFonts is
**LGPL-2.1**. Section 3 of LGPL-2.1 explicitly permits distributing a copy under the GNU
GPL instead, so combining it into a GPL-3.0 work is allowed. **This was never a
violation and rewriting it would have been waste.**

What was actually missing was attribution: the files carried no provenance note and the
README credited Patcher and eventbus but not BetterFonts. Both now state the origin,
the original licence and the basis for redistributing under GPL-3.0.

Worth separating from the Patcher case: one is an incompatible licence requiring the code
to go, the other is a compatible licence requiring the origin to be stated. Treating them
the same would have thrown away working code for no reason.

### Access transformer renamed

`patcher_at.cfg` became `fpsmaster_at.cfg`. The filename reaches the shipped jar through
the `FMLAT` manifest entry, and the manifest duplicated the literal rather than using the
`accessTransformerName` value declared directly above it, so both were corrected.
Verified end to end: `FMLAT: fpsmaster_at.cfg` in the built jar, the file at
`META-INF/fpsmaster_at.cfg`, and a client run with no mixin or access failures.

## Runs

### R1 — Performance module semantics and dead settings

Correctness change, no timing claim. Verified with `switch-matrix.ps1` on
`LowAnimationTick` and `BatchModelRendering`: the counter reads zero with the module
off, zero with the sub-feature off, and non-zero with both on. Before the change, four
sub-features stayed active after the user disabled the module.

### Where frame time actually goes

Full breakdown on `entity-dense`, 2530us p50 frame, culling off:

| section | CPU p50 | share of frame |
| --- | ---: | ---: |
| **entities** | 1187.2us | **46.9%** |
| terrain draw | 413.4us | 16.3% |
| **terrain setup** | **376.0us** | **14.9%** |
| sky | 134.8us | 5.3% |
| hud | 13.4us | 0.5% |
| hand | 8.1us | 0.3% |
| particles / clouds / chunk upload | ~1us | ~0% |
| outside the world render (blit, swap, GUI) | ~230us | 9.1% |
| unattributed inside the world render | ~167us | 6.6% |

Before these brackets existed, terrain and entities covered 58% of the frame and the
rest was simply unmeasured — picking the next target from that position would have been
guesswork. Two things the breakdown changes:

- **`setupTerrain` is 14.9% and had not been looked at.** It is the per-frame chunk
  visibility walk, rerun whenever the camera moves, which for a PvP client is always.
  Its bytecode allocates `ContainerLocalRenderInformation` at three sites — one per
  visible render chunk per frame, each carrying its own `setFacing` set — plus a fresh
  `renderInfos` list that grows to hundreds of entries. That is allocation churn on the
  hot path, which is both the memory goal and the stutter goal.
- **The entity pass splits 82/18.** Of 1120us, 918us is inside the per-entity render and
  202us is the list walk, frustum checks and tile-entity work around it. That is 9.61us
  per armour stand — roughly 30,000 cycles for a model of about a dozen boxes, so the
  cost is per-entity fixed overhead (GlStateManager sequences, brightness lookup,
  texture binds, layer iteration) rather than geometry. Occlusion culling removes that
  cost wholesale for hidden entities, which is why it worked; making the remaining
  entities cheaper is a separate target, broken down below.

  | per-entity phase | cpu p50 | share | per entity |
  | --- | ---: | ---: | ---: |
  | `renderModel` | 827.8us | 69.2% | 8.66us |
  | `renderLayers` | 186.4us | 15.6% | 1.95us |
  | `setBrightness` | 134.3us | 11.2% | 1.40us |
  | unbracketed | 47.2us | 3.9% | 0.49us |

  The model dominates: 8.66us across about a dozen boxes is 0.72us per box, roughly
  2200 cycles for a translate, a rotate and a `glCallList`. This exposes the limit of
  the existing BatchModelRendering, which batches vertices *inside*
  `compileDisplayList` but does not reduce the number of `glCallList` invocations — an
  entity still costs a dozen display-list calls and a dozen matrix setups. Collapsing a
  model into one display list is the real fix and needs `ModelBase` restructuring.

  `setBrightness` at 1.40us per entity is one `glMultiTexCoord2f` lightmap write.
  Caching it to skip redundant writes is the obvious move, but display lists executed
  by `glCallList` can contain texcoord commands that bypass such a cache and leave it
  stale, so it is not worth doing without a way to verify that first.
- **Sky is 134.8us** on a superflat scene with clouds disabled, which is more than
  expected and worth a look.

This is also why judging a targeted change by whole-frame FPS is the wrong instrument —
see R4 below.

### Smart Animations — not implementable against evidence available here

**Verdict: deferred. Measured before building, and the measurement does not support it.**

Vanilla walks every animated sprite each tick and uploads the next frame whether or not
anything on screen uses it. Restricting that to sprites the visible chunks reference is
OptiFine's Smart Animations, and it needs sprite usage tracked through chunk compilation
— a substantial change. Measured first, on `entity-dense`:

| | |
| --- | ---: |
| `textureAnim` p50 | 0.0us (runs per tick, roughly 1 frame in 16 at this rate) |
| `textureAnim` p99 | 123.7us |
| total over an 80s window | 238.5ms, **0.3% of wall time** |
| animated sprites in the atlas | ~16 (vanilla water, lava, fire, portal) |

With vanilla textures the entire cost is 0.3%, so eliminating it completely would gain
0.3%. The technique targets **high-resolution resource packs**, where a 512x animated
sprite uploads 256 times the pixels — that is where OptiFine's win comes from, and there
is no such pack here to measure against. Building it blind is the mistake fast math
would have been.

**What the measurement did surface** is a stutter source rather than a throughput one:
p50 of 0 against a p99 of 123.7us is a spike once per tick, about 9us per sprite, which
is `glTexSubImage2D` call overhead rather than pixel volume. That is relevant to the
reduce-stutter goal and would remain relevant even at vanilla resolution.

### Fast math (BetterFps-style lookup tables) — not worth doing here

**Verdict: rejected on measurement. No code shipped beyond the benchmark itself.**

Vanilla's `MathHelper.SIN_TABLE` is 65536 floats (256 KB), far larger than L1, and the
standard suggestion is a smaller table with better locality. Microbenchmarked with each
implementation in its own JVM, best of 3 invocations, scattered access:

| implementation | ns/op | vs vanilla | max error |
| --- | ---: | ---: | ---: |
| vanilla 64K table | 1.2527 | — | 9.59e-05 |
| fast 16K table | 1.2939 | +3.3% | 3.84e-04 |
| fast 8K table | 1.1002 | −12.2% | 7.67e-04 |
| fast 4K table | 1.3107 | +4.6% | 1.53e-03 |
| `Math.sin` (double) | 46.5485 | +3616% | 2.98e-08 |

Then the question that decides it: **how often is it actually called?** Counting on
`entity-dense` gave **56 trig calls per frame** — about 0.07us of an 1867us frame, or
**0.004%**. Making sin and cos infinitely fast would gain four thousandths of one
percent.

Two things worth keeping from this:

- The first attempt measured all implementations in a single JVM and reported vanilla at
  3.27 ns/op with the 16K table 65% faster. Per-JVM isolation put vanilla at 1.25 ns/op
  and the ordering changed completely. A single dispatch site profiled across every
  implementation pollutes the JIT's view of the loop, and the result then depends on run
  order.
- Porting an optimisation because another mod has it, without first measuring the call
  volume in *this* codebase, would have spent a day on something worth 0.004%.

### R5 — entity occlusion culling

**Verdict: entity pass 40% faster. Judged on the section timer, not on frame rate.**

Scenario `entity-dense`: 120 armour stands, 60 in front of a stone wall and 60 behind
it. 95.6 entities reach the renderer per frame after vanilla frustum culling; 47.8 of
them are skipped.

| section cpu p50 | off | on | paired diff | null band | verdict |
| --- | ---: | ---: | ---: | ---: | --- |
| **entities** | **1209.7us** | **726.0us** | **−40.15%** | 13.56% | IMPROVED |
| terrain | 428.8us | 408.1us | −2.79% | 14.07% | within noise |
| particles | 1.0us | 0.9us | −10.0% | 20.0% | within noise |
| chunk upload | 2.3us | 2.1us | −8.45% | 12.5% | within noise |

**Re-verified under counterbalanced order** (4 pairs, in-series duplicate baseline), which
is the figure to quote:

| metric | off | on | paired | null band | verdict |
| --- | ---: | ---: | ---: | ---: | --- |
| `entities` cpu p50 | 1505us | 1062us | **−30.3%** | 5.84% | IMPROVED |
| `entityRender` cpu p50 | 1268us | 800.5us | **−37.2%** | 1.89% | IMPROVED |
| frame p50 | 2.881ms | 2.373ms | **−17.8%** | 6.07% | IMPROVED |
| avg fps | 338.3 | 406.9 | **+19.9%** | 9.73% | IMPROVED |

Per-pair on `entityRender`: −37.3%, −37.8%, −36.1%, −37.6%. Near-zero spread against a
1.89% band — a twenty-to-one ratio — and the effect concentrates in the section that was
changed. 35.7 of 96.0 entities per frame skipped.

This supersedes the earlier fixed-order figures. Those measured −40% and then −24% on the
entity pass across two builds and left the difference unexplained; under counterbalanced
order the four pairs agree to within 1.7 percentage points, which suggests the earlier
inconsistency was the ordering bias rather than anything in the code.

Whole-frame p50 moved −23.41%. The p99 and 1% low figures from this series are **not
trustworthy**: `off-2` contains a single 268ms frame that the outlier guard flagged, and
it is what produced an absurd +915% on one pair's 1% low. Only p50 is quoted.

Screenshot gate: pass, 0.65%–1.58% of pixels differing against a 7.29% calibrated limit.

**Memory regression found and fixed.** The first version raised heap from 480MB to
573MB and GC collections by ~30%. `Map<Integer, Probe>` boxed the entity id on every
lookup, twice per entity per frame — roughly 94,000 Integer allocations per second at
this frame rate, since ids above 127 miss the Integer cache. Moving the state onto the
entity via a duck interface removed the map, the hashing and the boxing together, and
removed the need for a periodic sweep of stale keys. After the fix, on a clean series:
heap 557MB off vs 539MB on, GC collections [29,26,27] off vs [28,30,31] on — level.

Re-measured after that fix, clean series with no outlier frames:

| section cpu p50 | off | on | paired diff | null band |
| --- | ---: | ---: | ---: | ---: |
| entities | 1142.8us | 863.2us | **−23.90%** | 6.83% |
| terrain | 403.4us | 421.9us | +4.50% | 3.45% |

Per-pair: −20.9%, −25.5%, −25.3%. avg fps 371.7 to 427.0.

**Two things this series leaves open.** Entities culled per frame dropped from 47.8 in
the first measurement to 35.5 in the second, and the entity-pass win moved with it,
from −40% to −24%. The two are consistent with each other, so the question is why fewer
entities are being culled; a review of the refactor found no behavioural difference, so
this is unexplained and **the conservative −24% is what is quoted**. Separately, terrain
came out +4.50% against a 3.45% band — marginal, and worth watching rather than
believing on one series.

### Reusing the chunk visibility list — no benefit, reverted

**Verdict: within noise on every metric. Change removed.**

`setupTerrain` rebuilds its visible-chunk walk whenever the camera moves and starts by
allocating a fresh `ArrayList`, which then grows to one entry per visible chunk —
several hundred at render distance 12. The hypothesis was that the growth copies were
worth removing. Reusing a cleared list with retained capacity:

| section cpu p50 | off | on | paired diff | null band |
| --- | ---: | ---: | ---: | ---: |
| terrainSetup | 334.4us | 335.7us | −0.24% | 1.76% |
| frameTotal | 2046.8us | 2022.8us | −0.89% | 1.32% |

GC collections identical, [29,30,29] against [29,29,29]. Heap at the sampling instant
read 855MB against 467MB, but with GC counts level that is sampling phase rather than a
change in allocation rate, and is not claimed as a memory win.

The estimate of roughly ten reallocations per rebuild was right; what was wrong was
assuming that mattered. `ArrayList` growth is amortised, so a few hundred reference
copies is well under a microsecond against a 334us walk. That walk's cost is the
breadth-first traversal and the per-chunk frustum tests, not the list.

Removed rather than kept: an unmeasurable change still costs a mixin, a setting and two
language entries, and the same standard was applied to fast math.

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
