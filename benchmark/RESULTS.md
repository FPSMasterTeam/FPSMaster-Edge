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

### Caching a whole model as one display list — mechanism wrong, reverted

**Verdict: entityModel −5.00% against a 4.04% band, nothing above it. Removed.**

Vanilla renders a model box by box: push, translate, up to three rotates, `glCallList`,
pop. That measured 8.66us per armour stand, about 2200 cycles per box for a trivial cube,
so the cost looked like the call sequence rather than the geometry. Recording a whole
model into one list, keyed by a hash of every box's pose, should have collapsed it.

The cache worked exactly as designed — **96.1 hits per frame out of 96.0 entities, zero
misses** — and the screenshot gate passed at 0.017-0.090%. The payoff did not follow:

| metric | paired | null band | verdict |
| --- | ---: | ---: | --- |
| `entityModel` | −5.00% | 4.04% | marginal, per-pair −7.6%, −5.6%, −1.8% |
| `entityRender` | −1.35% | 2.81% | within noise |
| frame p50 | +1.12% | 2.68% | within noise |

**The mechanism assumption was wrong.** A display list stores a nested `glCallList` as a
call, not as the contents of the list being called — legacy GL does not flatten. Replaying
the outer list still executes twelve nested list calls and all the matrix work on the
driver side; what is saved is only the client-side command submission, Java through JNI.
Hence 5% rather than 50%.

Five percent of a section that is 30% of the frame is about 1.5% overall, inside the
frame-level band, and it costs pose hashing, animation detection, a nested-`glNewList`
hazard and invalidation coupling to the batching setting. Removed on the same standard
applied to fast math and the visibility-list reuse.

Making this pay would mean not using display lists at all — one interleaved vertex buffer
per model with the transforms baked per pose — which is a different and much larger piece
of work.

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

## Recorded Hypixel matches: Bed Wars and The Pit

Two recordings replace the synthetic scenarios as the primary workload. `replay-pit` is
27.6s of Pit melee — around 800 packets a second of movement, damage, particles and
scoreboard updates with almost no chunk streaming. `replay-bedwars` is 162.8s of a Bed
Wars match, and the measured window is `t=16s..38.5s`, the heaviest stretch in it: island
travel with chunks arriving (peaks at 107 chunk packets and 1.9 MiB in one second) on top
of combat traffic.

Neither is played in full. `settleSeconds` skips to the interesting part, and the window
is 15s and 20s respectively. Playing all 162 seconds of the Bed Wars recording would cost
three minutes a run and measure mostly walking.

### What these workloads actually contain

Counters, per frame, with no optimisations enabled:

| | pit | bedwars |
| --- | ---: | ---: |
| entities reaching the renderer | 24.2 | 19.5 |
| armour slots examined | 41.7 | 76.1 |
| **armour glint model renders** | **0.01** | **0.00** |
| held-item layers | 10.4 | 19.0 |
| particles rendered | 63.8 | 45.0 |
| terrain draw calls | 331.5 | 74.1 |
| chunk rebuilds | 0.98 | 1.71 |
| **signs rendered** | **0.00** | **0.00** |

Section CPU p50, no optimisations:

| section | pit | bedwars |
| --- | ---: | ---: |
| entities | 620us | 492us |
| entity layers | 418us | 6us |
| **hud** | **221us** | **244us** |
| block entities | **0us** | **0us** |
| frame total | 1091us | 958us |

Two of these settle questions that were open:

- **The block-entity pass is not a target.** Signs, chests, enchanting tables, banners and
  skulls all render through `TileEntityRendererDispatcher`, which now has its own bracket.
  It reads a p50 of 0us on both recordings, 0.44us and 2.4us per frame in total, which is
  0.03% and 0.3% of the frame. Neither recording draws a single sign.
- **Enchantment glint is not a target either.** Vanilla draws an enchanted armour piece
  three times — the piece, then the glint twice, each pass reloading the texture matrix —
  so a fully enchanted set looked like the obvious cost in the layer stack. The counter
  says 0.01 glint model renders per frame on the Pit recording and zero on Bed Wars. The
  mechanism is real and the workload does not exercise it.

Both were measured before anything was built for them, which is the only reason neither
cost a day.

### The HUD is a third of the frame, and the feature aimed at it is off by default

`CustomHudFont` is the one setting that improved every metric in every series:

| | avg fps off | avg fps on | difference | hud section |
| --- | ---: | ---: | ---: | ---: |
| pit, 13-variant survey | 593.3 | 661.5 | **+11.5%** | −15.7% |
| pit, 4-variant confirmation | 576.2 | 715.6 | **+24.2%** | −15.7% |
| bedwars, 4-variant confirmation | 503.6 | 637.2 | **+26.5%** | −35.5% |

The Bed Wars series is the strongest form this evidence can take on a machine this noisy:
the three `hudfont` runs measured 637.2, 575.0 and 639.7 fps and the three `off` runs
measured 497.3, 248.0 and 509.8. **The worst run with the feature on beats the best run
with it off**, so the separation does not depend on any filtering decision.

It is off by default because it changes how text looks — the replacement face is narrower
than vanilla's. That is a product decision, not a measurement one, but it is worth knowing
it is the largest single frame-rate setting the client has on a real server.

### Fast Render, priced a third time

OptiFine's Fast Render is two changes. The first is `OpenGlHelper.isFramebufferEnabled`
returning false, so the world is drawn straight to the back buffer instead of into a
framebuffer that is then blitted; this client already does exactly that. The second is in
`Profiler.startSection`, which sets `GlStateManager.clearEnabled = false` on entering
`render` and back to true on entering `display` — the colour and depth clear is skipped
for the whole world pass. This client does not have that half.

Measured on the recordings, against the defaults rather than against nothing:

| | defaults | + FastRender | difference |
| --- | ---: | ---: | ---: |
| pit | 700.1 | 728.4 | +4.0% |
| bedwars | 526.4 | 530.1 | +0.7% |

The null band on the same series is ±2.5% on avg fps, so this is the third independent
attempt that has failed to find a benefit — after an integrated-GPU series and a
discrete-GPU series. The verdict stands: keep it off by default.

### What OptiFine does for signs, and why it does not apply here

`TileEntitySignRenderer` is one of only four block-entity renderers OptiFine touches at
all, and the change is a level of detail rather than an optimisation of the drawing:
`isRenderText` skips the text entirely past a distance derived from field of view and
window height, `max(1.5 * displayHeight / fov, 16)` blocks, recomputed once per frame.
At 1280x720 and the default field of view that is 16 blocks, where a sign glyph is about
two pixels tall. `RenderItemFrame` carries the same trick for the item inside a frame.

This is worth recording because it is the only per-frame text cost in block rendering —
every visible sign re-splits and re-draws its four lines through the font renderer, every
frame — but the counters above say both recordings draw zero signs, and the whole
block-entity pass is 0us. There is nothing to save.

The other renderers are untouched. The enchanting table, the beacon, the ender chest and
the end portal have no OptiFine change beyond shader and custom-colour hooks, and nothing
in OptiFine addresses glass or translucent blocks specifically. In 1.8.9 the translucent
layer's per-move re-sort is dispatched through `updateTransparencyLater` onto the chunk
builder thread, so it does not sit in the frame at all.

`RenderGlobal` does keep `renderInfosEntities` and `renderInfosTileEntities` as separate
pre-filtered lists rather than walking all visible chunks — which is the entity chunk
pre-filter this project implemented, measured at +0.5% avg fps, and deleted.

### Caching the armour texture path — built, and not measurable here

Forge's `LayerArmorBase.getArmorResource` builds the texture path with `String.format` —
four arguments, one boxed, plus a nested `String.format` for the overlay suffix — and only
then consults its map. The map spares the `ResourceLocation` allocation and nothing else:
the formatting, the boxing and the hash of a forty-character string are paid on every
call. Caching on the three things the path actually depends on (material, whether the slot
takes leggings, overlay or not) removes all of that and returns the same instance vanilla
would have.

The counter first: **13.8 calls per frame**, not the forty the armour-slot count suggested,
because only slots that actually hold armour reach it. At roughly 1.5us for a
`String.format` that is about 21us against a 1000us frame, or 2%.

The A/B confirms the switch works — 0 cache hits with the setting off, 13.8 with it on —
and says nothing else, because the in-series null band swamps it:

| | off | off2 | on |
| --- | ---: | ---: | ---: |
| avg fps | 594.3 | 571.6 (−3.8%) | 640.3 (+7.7%) |
| entity layers cpu p50 | 341us | 272us (**−20.1%**) | 193us (−43.4%) |

`off` and `off2` are the same configuration. Twenty percent apart on the section, and a
−43.4% reading for a change that can only account for about 6% of it, is the machine
talking, not the code. **This is not a result in either direction**, and the change should
be re-measured or removed rather than believed.

## The measurement environment stopped being usable

This has to be recorded because it invalidates part of the campaign above and will
invalidate the next one if it is not fixed.

Runs fall into two populations that no amount of repetition merges:

- clean, p50 1.33–1.53ms, zero or one frame over 20x the median;
- contaminated, either spiky — dozens of frames of 130–380ms — or uniformly slow, p50
  15.67ms and 19.52ms, which is 64 and 51 frames a second.

The uniformly slow ones are the dangerous kind: their frame times are *even*, so they
contain no frame over 20x their own median and the project's existing outlier guard passes
them. Frame times pinned near a display refresh interval are the desktop compositor pacing
the window, not the client rendering slowly. `matrix.py --clean` now rejects a run on p50
as well as on outliers.

The rate rose through the session, from around 40% of runs to every run in the last Bed
Wars series. The GPU was not the cause: clocks held at the locked 1792 MHz throughout, 70C,
with no throttle reason set. The machine runs a remote-desktop agent with a virtual display
adapter (`GameViewer`, plus `OrayIddDriver` and a virtual display in the adapter list) and
a HIPS security daemon, either of which can take the desktop for long enough to do this.

Consequences for what is written above:

- **Frame-level medians on the clean subset are usable.** The `off` versus `base` null pair
  in the survey — the same code path, one through the module switch — came out at +2.5% on
  avg fps and −2.0% on p50, so that is the band the survey results are judged against.
- **Section-level attribution is not usable at these magnitudes.** The same series reports
  `terrain` +382% for a font change and `terrainSetup` +1420% for a chunk-update throttle,
  which are not effects. Only differences far larger than the section's own null spread —
  the HUD result — should be read from the section table.
- **Every per-feature figure smaller than about 5% is undecided**, including particle
  culling (which demonstrably skips 69% of particles yet moves avg fps +3.5%), entity
  culling, model batching and the armour texture cache.

The first attempt at this campaign was additionally polluted by this session's own tool
use — decompiling jars and extracting archives while runs were in flight — which produced
per-pass differences from −84% to −0.4% for the same variant. That part is fixed by not
doing it; the rest is not fixable from inside the benchmark.

## The interference was software, and removing it fixed the instrument

The desktop stalls described above came from `GameViewer`, a remote-desktop agent, whose
service was stopped for the session. An A-vs-A series immediately afterwards:

| | before | after |
| --- | ---: | ---: |
| runs containing a frame over 20x the median | ~40%, rising to 100% | 0 of 6 (each has exactly one, worst 42ms) |
| uniformly slow runs (p50 15-20ms) | several | none |
| p50 spread across six runs | 1.33-4.51ms | 1.275-1.346ms |
| **paired p50 band** | unusable | **2.74%** |

Frame-level p50 is usable again. avg fps and the tail metrics are not: the same six runs
give a 9.99% band on avg fps, 22.17% on p99 and 11.19% on the 1% low, because a single
30-40ms frame per run still moves them. **Verdicts below are taken on p50 and on section
p50, with avg fps quoted only for scale.**

## Block rendering: signs are the whole story, and the rest is not there

Three stress scenarios, because the recorded matches contain none of this: `sign-dense`
puts 2401 signs in a field reaching 34 blocks, `enchant-dense` 1089 enchanting tables,
`blockentity-dense` one block type per quadrant plus stained glass and panes.

They are stress scenarios in the literal sense — `sign-dense` runs at 36.9 fps with
**92.5% of the frame inside the block-entity pass**. That is the point: a cost that is
0.03% of a real match cannot be measured on a real match.

### What each block actually costs

Derived from the pass timings and the per-frame counters:

| | per block per frame |
| --- | ---: |
| **sign text** | **~12us** |
| sign model | ~2us |
| enchanting table | ~2us |
| stained glass / panes | **0** — chunk geometry, not a block entity |

The text is the outlier by six times. Everything else lands at the same ~2us, which is
what a matrix setup, a light lookup, a texture bind and a small model cost — the actual
drawing, with nothing recoverable in it.

### Sign text distance culling — shipped, default on

`SignTextCulling` implements OptiFine's cutoff, `max(1.5 * windowHeight / fov, 16)` blocks,
which is an angular threshold in disguise: it asks where a glyph falls below about a pixel.
At 1280x720 and the default field of view that is 16 blocks, where a sign character is
roughly two pixels tall.

The redirect is on the `signText` field read rather than on `drawString`. An empty array
makes the loop not execute at all, which skips the component splitting and the width
measurement as well; skipping only the draw would leave the layout work, which is the
larger half.

`sign-dense`, in-series duplicate baseline:

| | off | off2 | on |
| --- | ---: | ---: | ---: |
| **p50 frame** | 27.0ms | 27.2ms (+3.9%) | **15.1ms (−43.3%)** |
| blockEntities cpu p50 | 24285us | 24352us (+3.6%) | **12865us (−46.6%)** |
| avg fps | 36.9 | 32.5 | 66.0 |
| sign text culled per frame | 0 | 0 | **960 of 1737** |

Per-pass p50: −40.2%, −45.6%, −44.2%, against a 3.9% null band measured in the same
series. An order of magnitude clear of it, and the counter shows the mechanism doing what
it claims.

It is default on. It is a visible change in principle — text stops being drawn — but only
past the distance where it is already unreadable, which is the same trade OptiFine ships
on by default. On the recorded Hypixel matches it does nothing at all, because they
contain zero signs.

### Block-entity distance culling — shipped, default off

`BlockEntityCulling` plus `BlockEntityDistance` bring the cutoff in from vanilla's own
limit. This is a knob rather than an optimisation, and the reason is worth stating: there
was nothing invisible left to take. Forge already frustum-tests every block entity before
dispatching it — confirmed in the bytecode of the mapped jar, `getRenderBoundingBox`
followed by `isBoundingBoxInFrustum` at all three dispatch sites — and vanilla already
skips anything past `getMaxRenderDistanceSquared`. The remaining ~2us each is the drawing.

So the only lever is to draw fewer of them, and unlike sign text that is plainly visible:
a chest at forty blocks stops having a lid. Hence off by default, and hence a distance the
user chooses rather than one derived from the window.

`enchant-dense` at 12 blocks:

| | off | off2 | on |
| --- | ---: | ---: | ---: |
| **p50 frame** | 3.4ms | 3.2ms (−6.0%) | **1.9ms (−44.4%)** |
| blockEntities cpu p50 | 2348us | 2206us (−5.6%) | **942us (−59.4%)** |
| avg fps | 289.5 | 308.6 | 521.2 |
| culled per frame | 0 | 0 | **247 of 409** |

`blockentity-dense` at 24 blocks, with both features:

| | off | off2 | signs only | both |
| --- | ---: | ---: | ---: | ---: |
| **p50 frame** | 11.0ms | 12.0ms (+9.0%) | **6.4ms (−41.8%)** | **5.7ms (−48.3%)** |
| blockEntities cpu p50 | 9340us | 10156us (+8.8%) | 4980us (−46.7%) | 4298us (−54.0%) |
| terrain cpu p50 | 411us | 463us | 378us | 412us |
| avg fps | 90.3 | 82.8 | 154.4 | 174.5 |

Sign text culling alone recovers most of it; the distance cut adds another six points of
p50 on top.

### Glass and translucent blocks: nothing to optimise, and the reason

The `terrain` section does not move across any of these variants — 411us, 463us, 378us,
412us — while the block-entity pass halves. The stained glass and pane quadrant is 23x23x4
blocks of translucent geometry and it is simply part of the chunk mesh.

The premise that translucent blocks are expensive per frame does not hold in 1.8.9. The
one extra thing the translucent layer does is re-sort its quads by distance when the
player moves more than a block, and `RenderGlobal.renderBlockLayer` hands that to
`updateTransparencyLater`, which runs on the chunk builder thread. It is not in the frame.

OptiFine adds nothing here either: `ClearWater` changes water opacity, `ConnectedTextures`
and `CustomBlockLayers` are net costs, and no renderer for glass exists to optimise.

### The enchanting table specifically

No change made. `TileEntityEnchantmentTableRenderer` is a translate, two rotates, a texture
bind and a six-box book model, and OptiFine does not touch it. Measured at ~2us, the same
as every other block entity, which says the cost is the per-block overhead rather than
anything about the book. The only thing that moves it is not drawing it, which is what
`BlockEntityCulling` does for every block entity at once rather than for this one.

### The armour texture cache, re-measured with the interference gone

Supersedes the undecided result above. Same series shape, clean machine, p50 band 2.74%:

| | off | off2 | on |
| --- | ---: | ---: | ---: |
| p50 frame | 1.3ms | -0.8% | **+1.1%** |
| entityLayers cpu p50 | 351us | +9.4% | **+8.8%** |
| frameTotal cpu p50 | 848us | +0.2% | +1.0% |
| cache hits per frame | 0 | 0 | 14.7 |

Per-pass p50 for `on`: -0.0%, +7.3%, -3.9% -- mixed in sign, and the duplicate baseline
moves the section by more than the change does. **Within noise.**

The counter proves the mechanism: 14.7 `String.format` calls a frame removed, returning
the identical `ResourceLocation`. It is simply too small to see -- 14.7 calls at roughly
1.5us is 2% of the frame in theory and nothing in practice.

This is the same verdict, on the same evidence standard, that removed `ReuseRenderInfos`
(+2.2%) and `CacheModelLists` (+0.4%). By that standard `CacheArmorTextures` should be
deleted rather than shipped: an unmeasurable change still costs a mixin, a setting and two
language entries. It is left in place pending that call.

## Default set narrowed to what is invisible

Three settings that removed visible content were moved to off by default, leaving
`SignTextCulling`, `ParticlesLimit` and `LimitChunks` as the only defaults in that group:

| setting | was | now | what it was removing |
| --- | --- | --- | --- |
| `IgnoreStands` | on | **off** | the name label above every armour stand -- which is every hologram, shop label and floating kill feed a server puts in the world |
| `StaticParticleColor` | on | **off** | particle lighting; they rendered at full brightness regardless of the light around them |
| `LowAnimationTick` | on | **off** | nine tenths of the ambient particles, by cutting `doVoidFogParticles` from 1000 samples to 100 |
| `FPSLimit` | 30 | **0** | nothing visible, but it capped the game at 30 fps whenever the window lost focus |

The point is what they were worth. Priced individually on the pit recording against a
±2.5% avg fps band:

| | avg fps vs off | p50 | entities cpu p50 |
| --- | ---: | ---: | ---: |
| `IgnoreStands` alone | −5.3% | +2.3% | +2.6% |
| `StaticParticleColor` alone | +4.4% | −3.6% | −4.5% |
| `LowAnimationTick` alone | −1.0% | +1.5% | +0.2% |

Two of the three do not even move in the right direction, and none of them clears the
band. They were removing things the player can see, in exchange for nothing that could be
measured. `IgnoreStands` was the worst of the three on both counts: the largest visible
loss and the least evidence of a gain.

`FPSLimit` additionally only ever took effect when the player's own frame rate limit was
already below Unlimited -- vanilla guards the `Display.sync` call with
`isFramerateLimitBelowMax`, which reads the game setting rather than this one.

**The cost of the change itself could not be measured.** A paired series against the old
default set was attempted on both recordings and has to be discarded: the duplicate
control (`newdef` against `newdef2`, the same configuration twice) came out at 1.583 vs
2.918ms and 2.158 vs 1.362ms on the pit recording, and the bedwars runs carried single
frames of 1.0 to 1.7 seconds with p50 at 14-22ms against the 1.245ms measured earlier the
same day. GPU clocks, temperature, power plan and free memory were all normal, and the
remote-desktop agent was still stopped, so the cause is unidentified. The per-feature
figures above, measured earlier on a better-behaved machine, are the evidence this change
rests on.

## HUD text: almost all of it is redrawn identically every frame

The HUD is rebuilt from scratch every frame while its contents change at most once a tick,
and these recordings run at 300-500 fps against 20 tps. That is fifteen to twenty-five
frames per tick in which nothing on the overlay can have changed. The question this probe
answers is how much of the text work is spent redoing identical geometry, and whether the
answer is large enough to justify caching it.

Counted with `-Dedge.exp.hudBreakdown=true`, `CustomHudFont` on, steady state, per frame.
Each row is one 600-frame report window, not a run average -- the first attempt reported
cumulative figures and the main menu, which draws two hundred static strings a frame at a
hundred per cent reuse, dragged the whole run towards a number no gameplay frame ever had.

| | pit | bedwars |
| --- | ---: | ---: |
| strings drawn | 40.0-47.4 | 51.4-70.6 |
| **identical to last frame** | **99.1-100%** | **69.8-74.7%** |
| obfuscated (cannot be cached) | 0.0 | 13.7-20.7 |
| glyph quads | 592-790 | 355-698 |
| `text:prewarm` | 11.2-12.1us | 14.1-16.4us |
| `text:setup` | 4.2-4.5us | 4.7-5.1us |
| **`text:emit`** | **118.7-131.0us** | **72.8-84.3us** |
| **`text:submit`** | **59.9-65.0us** | **68.9-75.1us** |
| all four | ~195-212us | ~161-181us |

Against a 1.83ms and 1.87ms p50 that is **11% and 9% of the frame**, and the reuse column
says most of it is recomputation. `emit` is the glyph loop that would not have to run;
`submit` is one draw call per string, 40 to 56 a frame, which is what batching is for.

The per-string costs held across runs that disagreed by 60% on frame rate -- `emit` measured
2.85us and 2.90us per string on two pit runs six minutes apart, one at 475 fps and one at
290 fps. The counts and the ratio are what this probe is for and neither depends on the
machine behaving.

Three things qualify the numbers before anything gets built on them:

- **The replay playback overlay was in the measurement, and has since been cut.** It
  bracketed at 59-64us a frame and exists only during benchmark playback. It was drawing two
  rounded rectangles -- four corner textures and three fills each, for a 3-pixel bar with a
  1-pixel radius -- and three centred strings, each of which is laid out twice, once to
  measure and once to draw. Rewritten to plain fills and one clock string rebuilt on the
  second rather than the frame, it now brackets at 15.3-16.5us and contributes 1 string
  rather than 3. So roughly 7% of the string count above was the measuring apparatus; the
  figures after this change are 33.9-40.2 strings a frame on the pit recording.
- **All of it is conditional on `CustomHudFont`.** With that setting off -- the default --
  `TextRenderer` draws 3.0 strings a frame and the same text goes through vanilla's renderer
  instead, where this cache cannot reach it.
- **Bed Wars is 70% reusable, not more.** Between a quarter and a third of its strings are
  obfuscated, and those re-scramble their glyphs every frame by definition. Hypixel's Bed
  Wars scoreboard is the source. A cache degrades to current behaviour on them rather than
  breaking, but they are not recoverable.

### Two things the probe seemed to find, and neither survived

Both were read off bracket names and both were wrong. Recorded because the misreadings are
easy to repeat.

- **Chat is not still on vanilla's font renderer.** `chat:drawText(vanillaBranch)` names the
  branch of `GuiNewChat` that runs when the `BetterChat` module is off, not the font it draws
  with. Chat goes through `FontRenderer.renderString` like everything else, which is exactly
  where `CustomHudFont` intercepts. The numbers say so plainly: the same bracket on the same
  recording measures 117.8us with `CustomHudFont` off and 63-80us with it on, a 32-46% drop
  from changing nothing else. It is an outer bracket containing part of the `text:*` costs
  below it, so it is not a separate target and must not be added to them.
- **`boss health` is a boss bar that is really there.** Hypixel drives one on The Pit.
  Vanilla's body returns immediately without one, and Forge's wrapper around it -- an event
  pre/post, a texture bind and blend toggles -- costs about 3us, which is what
  `renderCrosshairs` measures doing the same wrapper plus an actual draw. The remaining
  ~30us is one shadowed string, three textured rects and a `ScaledResolution` allocation.
  That also explains why it halved from 66-70us to 31-40us when `CustomHudFont` came on: a
  shadowed string is two `renderString` calls and dominates the element. Its cost is already
  inside the text path, and a text cache would cover it.

So the text figures above stand on their own, and there is no second target beside them.

## Caching HUD text geometry: the layout was never the cost

The reuse figures above said 99.7% of strings on the pit recording are identical to the
previous frame's, and `text:emit` was 125.6us a frame, so caching the laid-out geometry
looked like it would take most of that. It takes about a fifth of it, because the emit
bracket was not measuring what it looked like it was measuring.

`TextRenderer` now records each string's quads once, in coordinates local to its own origin
and with the caller's colour left out so a moved or re-coloured string still hits, keyed on
the text and the shadow pass, dropped when the atlas generation changes and never taken for
obfuscated strings. Pit recording, `CustomHudFont` on, 600-frame windows:

| | before | after |
| --- | ---: | ---: |
| `text:prewarm` | 11.0us | — |
| `text:hit` (cache lookup) | — | 9.1-9.8us |
| `text:setup` | 4.2us | 4.8-4.9us |
| `text:emit` | 125.6us | — (below reporting threshold) |
| `text:submit` | 62.8us | 164.8-181.0us |
| **per string** | **5.07us** | **4.14-4.32us** |

The layout genuinely stopped happening: `emit` fell off the report entirely, which at these
hit rates it should. But `submit` rose by about 102us, and that is not a regression -- it is
the same work under a different bracket. Pushing the vertices was always inside `emit`, and
moving it into the submission step is what the numbers are showing.

So the emit bracket was 102us of vertex writing and about 24us of actual layout. **The glyph
loop, the format parsing and the atlas lookups were a fifth of what they appeared to be**, and
the real cost is 600 to 800 quads a frame going through
`pos().tex().color().endVertex()` -- sixteen calls per quad, some twelve thousand a frame --
plus a `Tessellator.draw()` per string.

Net: 15-18% off the text path, roughly 32us a frame, **about 1.8% of the frame**. That is
below this project's own 3% entry threshold and inside the noise band, so it is not a result
on its own. What it is is the input the next step needs: geometry that is already laid out,
position-independent and stable across frames is exactly what can be uploaded once to a
buffer instead of rewritten every frame. Cutting the remaining ~165us means not rewriting
those vertices and not issuing a draw per string, and neither is reachable without that.

## The terrain visibility walk is forced by our own chunk throttle, not by the camera

`setupTerrain` is 14.9% of the frame and had never been looked at. The roadmap's plan for it
was to relax the threshold at which camera movement forces the visible-chunk list to be
rebuilt, on the theory that a PvP camera moves constantly and so rebuilds constantly. The
probe says the theory is right about the cost and wrong about the cause.

Timing the whole call and filing it under whether the walk ran that frame, rather than
bracketing inside a method whose walk, container class and dirty assignment share one body.
Forge already skips the walk when nothing changed, so both populations occur naturally and
the gap between their means is what the walk costs.

| | rebuilt | cost when it did | when it did not | amortised | rebuilds after camera movement |
| --- | ---: | ---: | ---: | ---: | ---: |
| pit replay, `LimitChunks` on | 90-100% | 392-576us | ~48us | 392-478us | **4-6%** |
| pit replay, `LimitChunks` off | 32-37% | 593-784us | 15-30us | 186-279us | 12-19% |
| flat-orbit, orbiting camera | 100% | 787-848us | — | 787-848us | **100%** |

Three things fall out of that.

**The walk is essentially all of `setupTerrain`.** 392us against 48us on the recording, and
787us on flat-orbit against a 2.16ms frame — **37% of that frame**. Whatever else the method
does, the visible-chunk walk is what it costs.

**On a real workload the camera is not what triggers it.** Only 4-6% of the rebuilds on the
pit recording followed the view entity moving. The other 94-96% were forced by
`!chunksToUpdate.isEmpty()`, which is the other half of Forge's dirty condition — and that
half is not optional, because a chunk that has just been rebuilt has to be walked again. The
relaxed-threshold prototype the roadmap scoped would have addressed one frame in twenty.
Flat-orbit is the control: with a camera that really does move every frame and no chunk
streaming, the attribution flips to 100%.

**And what keeps `chunksToUpdate` non-empty is our own throttle.** Turning `LimitChunks` off
takes the rebuild rate from 90-100% of frames to 32-37%, and the amortised cost from
392-478us to 186-279us. The throttle holds chunk rebuilds back, the pending set stays
occupied, and the pending set forces a full visibility walk every frame it is occupied. That
is roughly 200us a frame being paid for a setting whose purpose is to protect frame time.

That is not a verdict on the throttle — it was measured as a win before, and it is trading
chunk-build CPU for forced walks, which is a trade that can go either way. It is a verdict on
where to look: the lever on this section is how long the pending set stays occupied, not how
sensitive the camera check is. The dynamic budget in the same commit raises the allowance the
moment the player stops, which drains that set sooner, and now has a second and larger reason
to exist than clearing a backlog faster.

Single runs, and one of them measured 371.9 avg fps with the throttle off against 320-325 with
it on — the opposite direction to the 7.3% loss recorded for it earlier. That needs a paired
series before it means anything; the counts and the attribution above do not depend on it.

### `LimitChunks` priced properly: better median, worse everything else

Three interleaved pairs on the pit recording, first discarded.

| | on | off | off vs on |
| --- | ---: | ---: | ---: |
| avg fps | 308.3 | 325.9 | **+5.7%** |
| mean frame | 3.245ms | 3.069ms | −5.4% |
| **p50** | **2.425ms** | **2.704ms** | **+11.5% worse** |
| p95 | 7.460ms | 5.556ms | −25.5% |
| p99 | 11.685ms | 8.692ms | **−25.6%** |
| 1% low fps | 53.3 | 60.0 | +12.6% |

No metric's three-run range overlaps between the variants, so this is not the noise band
talking: on 302.9-317.7 avg fps against off 324.3-328.1, p50 2.342-2.527 against 2.666-2.728,
p99 10.660-12.360 against 8.428-9.160.

The throttle makes the typical frame cheaper and every other frame worse. That is what it is
built to do on the first half — holding chunk rebuilds back keeps them from competing with the
frame drawing what is already built. The second half is the cost measured above: the pending
set stays occupied, a full visibility walk is forced on every frame it is occupied, and the
work it deferred still has to happen, in bursts. p95 and p99 are a third worse and the average
is 5.7% down.

This project's standing rule is to take verdicts on p50 because the instrument's average and
tails are unreliable. That rule exists for noise, and this is not noise — p50 disagrees with
five other metrics, all of them separated, all in the same direction. It is a real trade
rather than a measurement artefact, and which side of it is right depends on whether a client
is being tuned for a smooth median or against stutter.

**One confound, stated rather than buried.** These `on` runs include the dynamic budget from
the same commit, and the pit recording's free camera does not move, so the budget spent the
run in its stationary branch — twice the configured allowance. This is therefore a comparison
of a doubled throttle against no throttle, not of the old fixed one. Separating the three
needs a switch that does not exist yet, and the earlier record of `LimitChunks` being worth
7.3% of frame rate was taken on a different build and workload and is not being overwritten
by this.

## Entity collision: 99% of the scan is waste, and the whole thing is 1% of wall clock

The plan for this was "vanilla scans entities linearly, add spatial partitioning". Vanilla
already partitions — `World` walks only the chunks the box touches and `Chunk` only the
16-block sections it touches — so the probe counts what is actually walked instead.

Bed Wars recording, three 200-tick windows:

| | | | |
| --- | ---: | ---: | ---: |
| queries per tick | 314.4 | 115.3 | 208.0 |
| repeats of a box already asked this tick | 2.8% | 10.5% | 5.0% |
| chunks per query | 2.28 | 2.02 | 2.07 |
| sections per query | 2.37 | 2.29 | 2.21 |
| **entities examined** | **82.1** | **25.7** | **50.7** |
| entities returned | 0.2 | 0.6 | 0.7 |
| **kept** | **0.3%** | **2.4%** | **1.3%** |
| per query | 1.87us | 1.35us | 1.13us |
| **per tick** | **589.3us** | **156.0us** | **234.3us** |

The waste is as bad as it could be: eighty-two entities examined to return one fifth of one.
Which is not a bug, it is the resolution of the index — a section is sixteen blocks tall, a
collision box is about one, and everything in the section is tested because there is nothing
finer to test against.

And it does not matter, because of the last row. 589us in the worst window, twenty ticks a
second, is **1.2% of wall clock**; the other two windows are 0.3% and 0.5%. Removing the
entire query — not optimising it, removing it — would buy about one per cent at its worst.
The two fixes actually available are smaller still: the duplicate rate caps a per-tick memo
at a tenth of that, and the per-query `ArrayList` is a few thousand small objects a second.

Under the 3% entry threshold on the best window and an order of magnitude under it on the
others, so this does not proceed. Recorded because "entity collision scans linearly" is an
argument that will be made again, and the answer to it is 82 examined against 0.2 returned
costing one per cent.

### Both objections to that were checked, and one of them found a real hole

**"The scenario is wrong."** It was a fair challenge — Bed Wars puts 19.5 entities in front of
the renderer, which is not an entity-dense workload, and the acceptance criteria ask for one.
Re-run on `entity-dense`, 120 armour stands: **145 queries a tick, 31 examined, 175-183us a
tick**. Milder than Bed Wars on every column. The stress scenario does not overturn the
recording; the recording was already the higher number.

**"The probe misses a path."** This one was right. `World` has two entity queries, not one:
`getEntitiesInAABBexcluding`, which funnels every collision form, and the typed
`getEntitiesWithinAABB`, which is a separate walk into a different chunk method. Only the
first was instrumented, so anything using the second was invisible. Both are now counted,
along with the sections each walks.

It changed nothing, for a structural reason worth writing down: **0.0% of queries on either
recording are the typed form.** The obvious client-side user of it would be item merging, and
`EntityItem.searchForOtherItemsNearby` is guarded by `!worldObj.isRemote` — it is server work.
The hole in the instrument was real and the room behind it was empty.

Which is the same reason the intuition that this should be expensive does not transfer. On a
server, entity collision is a known cost, because a server simulates entities. A client does
not: remote players are interpolated to the positions the server sends rather than moved by
physics, and mobs run no AI. What is left making three hundred queries a tick is dropped
items, projectiles and the push-apart check on living entities, and it costs what it costs.

The scenario that would change this is one neither recording has: a floor covered in dropped
items. That is untested and is the one honest gap left in this conclusion.

### The gap was bigger than that: half of collision had not been measured at all

Reading what Badlion's "Fast Entity Collision" actually does settles it. Its call site is
`Entity.moveEntity`, gated on the client world and four entity types, and it replaces the whole
method with an alternate engine. The adapter it hands that engine exposes
`addCollisionBoxesToList` — **block** collision boxes. The name means fast collision *for*
entities, against the world; not entity against entity, which is what the probe above measured.

`World.getCollidingBoundingBoxes` does both, in that order: a triple loop over every block
position the swept box touches, then the entity query. Only the second was instrumented.

Worse, no scenario here could have caught it. `BenchCamera` sets `noClip = true` and teleports
along its keyframes, which is what makes a path exactly reproducible and also means the player
never calls `moveEntity`. So a walked scenario was added — the player is steered at the same
keyframes under its own physics with collision on — and the block half instrumented.

Superflat, walking, per 200 ticks:

| | |
| --- | ---: |
| moves per tick | 36.1 |
| block positions per move | 4.6 |
| boxes returned per move | 1.41 |
| whole call | 75-81us/tick |
| the entity query nested inside it | ~40us/tick |
| **block walking** | **35-40us/tick** |

Bed Wars recording, now with both halves, three windows:

| | moves/tick | positions | whole call | nested entity | **block walking** |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 302.0 | 5.7 | 767.9us | 494.5us | **273.3us** |
| 2 | 52.6 | 4.5 | 58.7us | 28.3us | **30.4us** |
| 3 | 242.1 | 4.4 | 314.3us | 195.1us | **119.3us** |

So the earlier figures were about 60% of the real cost, and the worst window goes from 545us
to 768us a tick — **1.5% of wall clock** rather than 1.2%. Forty per cent more, same order of
magnitude, same side of the 3% threshold.

The correction stands on its own, though. The first pass measured one of two halves and did
not say so, which is the same mistake as the emit bracket in the HUD text cache: a name that
sounded like it covered the work, and did not. Badlion's own description of the feature —
"useful for Singleplayer" — is the other half of the answer, because in singleplayer the
integrated server simulates entities properly and there is real work there to speed up.

### FastCollision: skipping a query that only boats and minecarts could answer

The entity half of `getCollidingBoundingBoxes` is measured above at two thirds of the whole.
Its results are used in exactly two ways — the other entity's `getCollisionBoundingBox`, and
the mover's own `getCollisionBox(other)` — and reading every override of both in the 1.8.9
entity tree turns up two classes: `EntityBoat` and `EntityMinecart`. Nothing else can put a
box in that list. A Bed Wars map has neither, so several hundred queries a tick are asked and
answered with nothing for the length of a match.

So the skip is the two conditions under which the loop can produce output, checked directly: a
per-world count of loaded entities whose collision box is non-null, and whether the mover is
itself something that collides with others.

Bed Wars recording, same three windows, `FastCollision` off then on:

| window | moves/tick | whole call off | on | nested entity off | on | **boxes returned off / on** |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 302 | 767.9us | **218.6us** | 494.5us | 0.0us | 0.36 / 0.36 |
| 2 | 53 | 58.7us | **27.4us** | 28.3us | 0.0us | 0.14 / 0.14 |
| 3 | 242 | 314.3us | **110.2us** | 195.1us | 0.0us | 0.16 / 0.16 |

**−53% to −71%** on the call, and per move 2.54us to 0.71us on the busiest window. 121,455
queries skipped across the run.

The last column is the correctness check and it is the important one: **the number of
collision boxes returned is identical to three decimal places in every window.** The skip
removed queries, not results. Had the reasoning about the entity tree been wrong, that column
would have fallen.

About 550us a tick at the worst moment, which is 1.1% of wall clock — the size the pricing
predicted, now actually removed rather than estimated.

**Off by default, and one path is untested.** Neither recording contains a boat or a minecart,
so the branch that must *not* skip has never been exercised. Its condition is one integer
comparison and one virtual call, but a wrong answer there is a player walking through a boat,
and that deserves a world with one in it before this is on by default.

### Three-way on the chunk budget: one finding survives, and one earlier claim does not

`LimitChunks` off, the fixed budget, and the adaptive one, three interleaved passes each on the
pit recording.

| | avg fps | range | p50 | range | p99 | range |
| --- | ---: | :---: | ---: | :---: | ---: | :---: |
| off | 336.9 | 314.8-380.0 | 2.630 | 2.257-2.822 | 8.977 | 7.535-9.764 |
| **fixed** | 380.4 | 327.5-411.2 | **2.130** | **2.046-2.235** | 8.016 | 6.570-10.454 |
| adaptive | 342.0 | 285.4-416.7 | 2.563 | 2.040-3.151 | 8.581 | 6.547-10.048 |

**The adaptive variant spans 285 to 417 fps for the same configuration.** A 46% spread within
one variant is the headline here, and it is about the machine rather than the setting. Two of
the three passes had the fixed budget far ahead of both others; the third reversed it
completely.

What survives: **the fixed throttle beats no throttle on p50, with separated ranges** — 2.130
against 2.630, and fixed's worst pass (2.235) is still better than off's best (2.257). That
agrees with the earlier paired series, so two independent series now say the same thing about
the median.

What does not survive: **the other half of that earlier series.** It reported the throttle
losing 5.7% of average fps and a third of p95 and p99, on three pairs whose ranges did not
overlap. This series shows a single variant covering a 46% range, which means that non-overlap
was not the evidence it looked like. The average and tail question is unresolved, and the
claim that the throttle trades the median against everything else is withdrawn to the part
that has been measured twice: it wins on the median.

The adaptive budget is off by default as of this commit. It has not beaten the fixed budget and
this instrument cannot say whether it could; the roadmap's acceptance for that item is to keep
the fixed budget unless it does. The switch stays so the question can be asked again.

And the standing instrument problem is still standing. The counters remain trustworthy — the
terrain probe's finding that the throttle takes visible-list rebuilds from 32-37% of frames to
90-100% is a count, and does not care how noisy the clock is.

## Where the noise comes from: two systematic artefacts, neither of them the machine

The three-way above spanned 285 to 417 fps for one configuration and was written off as the
instrument being unusable. It is not unusable; it has two identifiable faults, and with both
removed the same nine runs separate cleanly.

**One: the opening of every measured window is slower than the rest of it.** Median frame time
per tenth of each run:

| run | avg fps | 0 | 1 | 2 | 3-9 |
| --- | ---: | ---: | ---: | ---: | :---: |
| adaptive | 285.4 | **4.76** | **4.60** | **4.30** | 2.05-3.91 |
| fixed | 402.4 | 1.73 | 1.94 | 1.99 | 1.85-2.64 |
| off | 316.0 | **3.07** | 2.56 | **3.12** | 2.08-3.72 |
| fixed | 327.5 | **6.81** | **4.33** | 1.57 | 1.65-2.48 |
| adaptive | 416.7 | 1.77 | 2.05 | 1.86 | 1.76-2.51 |

Every slow run has a slow start and an ordinary middle. Dropping the first 30% of frames takes
the within-variant spread from 22.0% to 9.2% on the fixed budget and from 38.4% to 17.3% on the
adaptive one. `warmupMillis` was 2000 and `discardMillis` 500 on these scenarios; the transient
lasts several seconds.

**Two: the whole series drifts upward.** Steady-state fps by position in the run order, across
all nine: **+9.0 fps per run, +21.7% from first to last.** Each run is a fresh JVM, so this is
not JIT — the likeliest candidate is the OS page cache filling with the jar, the assets and the
recording. The harness discards one run per variant for exactly this reason and one is not
enough.

**With both removed, the variants separate and do not overlap.** Residuals after detrending:

| | mean | range |
| --- | ---: | :---: |
| fixed | **+25.7** | +19.1 to +36.5 |
| adaptive | +7.0 | −0.3 to +10.9 |
| off | **−32.7** | −44.6 to −22.2 |

Ordering: **fixed > adaptive > off**, by about 58 fps between the ends. Which reverses the
conclusion drawn from the raw numbers twice over — the earlier series said the throttle lost
5.7% of average fps, and this one raw said nothing at all.

Two things follow, and the second matters more than the first.

`warmupMillis` and `discardMillis` are raised to 10000 and 3000 on both recorded scenarios. The
series drift needs more discarded runs than one per variant, or a detrend, and until one of
those exists a nine-run series is not evidence on its own.

And **fps is the wrong instrument for this particular setting regardless.** The throttle draws
243 terrain chunks a frame against 330 with it off — 26% fewer — because deferring a rebuild
means the chunk is not drawn yet. It is not a same-picture optimisation, so a frame rate
comparison against no throttle is comparing two different pictures and will favour the throttle
whatever else is true. What it should be judged on is frame time at equal drawn-chunk counts,
plus how long the world takes to become complete.

### The steady-state gate, measured

Same three-way, same machine, with the discard phase now ending on a steady frame time instead
of a 500ms clock.

| | before | after |
| --- | ---: | ---: |
| within-variant spread, adaptive | 38.4% | **15.1%** |
| within-variant spread, fixed | 22.0% | **11.5%** |
| within-variant spread, off | 19.3% | 20.9% |
| opening penalty, mean across runs | +44.5% | **+19.1%** |
| opening penalty, worst run | +225.7% | +113.8% |
| series trend, first run to last | +15.5% | +11.2% |

Seven of the nine runs now start steady — openings within a few per cent of their own steady
state, several of them faster. **Two do not**, at +97% and +114%, and the detector passed both:
it declares steady on two consecutive flat 240-frame windows, and a run can plateau during a
quiet moment and then find more work to do. Those two are named by `benchmark/analyse.py`,
which is the point of it — a bad run that announces itself is a different thing from a bad run
that averages into the result.

**The series drift is untouched and is now the larger fault.** +4.8 fps per run, +11.2% across
nine. It is a cold start rather than a continuous slide: the first three runs average 366.8 and
the last six average 399.9, with the last three at 397.8. The harness discards one run per
variant, which is three runs, and three is not enough — the discard wants to be two passes, or
a priming run before the series.

**And the three-way is still not a result.** Raw, detrended or on p50, every variant's range
overlaps every other's: adaptive 373-434, fixed 363-407, off 333-414. The instrument is
better and still cannot separate these three. What that says about the earlier detrended
reading — fixed +25.7, adaptive +7.0, off −32.7, cleanly separated — is that it was a fit to
nine points, and this series does not reproduce it.

So the honest state of the chunk-throttle question is: unresolved, with two independent series
agreeing only that the throttle helps p50, and the counters still saying it draws 26% fewer
chunks while doing it.

## The noise was the setting under test

Two discarded passes instead of one, on top of the steady-state gate. The series trend goes
from +11.2% across nine runs to −4.7%, so the cold start is handled. What that reveals is not
a better instrument reading — it is which variant was carrying the noise.

| variant | avg fps spread | draws/frame | spread | chunk rebuilds | spread |
| --- | ---: | :---: | ---: | :---: | ---: |
| **off** | **0.9%** | 332.8 [330.9, 335.4] | **1.4%** | 7286 | 13.2% |
| fixed | 26.2% | 268.8 [243.8, 301.1] | 21.3% | 1114 [795, 1442] | 58.1% |
| adaptive | 27.2% | 315.1 [293.7, 328.8] | 11.1% | 2517 | 41.8% |

**With the throttle off, three runs land within 0.9% of each other on frame rate and 1.4% on
terrain draw calls.** That is not a machine that cannot be measured on. It is close to the best
this harness has ever produced.

With the throttle on, the same three runs scatter by 26%, and the reason is in the columns
beside it: how many chunks get rebuilt during a run varies by 58%, and how many end up being
drawn by 21%. The throttle defers rebuilds, how many it manages depends on timing, and what is
not rebuilt is not drawn. **Each run renders a different amount of world.**

So the instrument problem that has been in this document since the campaign started, and that
three separate series were written off against, was substantially this setting. Every series
that included a throttled variant inherited its nondeterminism and reported it as noise.

Two conclusions follow, and the second supersedes a lot of argument.

**On p50, the fixed throttle beats no throttle, and the ranges do not overlap** — 1.937-2.135
against 2.145-2.166. Three independent series now agree on that and nothing has ever disagreed.
The adaptive budget is separable from neither and stays off.

**But `LimitChunks` is not a same-picture optimisation and cannot be priced as one.** It draws
269 terrain chunks a frame against 333 with it off, 19% fewer, and that figure itself moves 21%
between runs. It belongs with the visual culling group in section five of the roadmap: a knob
that trades something the player can see — how complete the world is — for frame time. Its p50
win is real and is partly just drawing less. What its default should be is a product decision
about whether a smoother frame is worth a world that fills in more slowly, and no amount of
further benchmarking will answer that, because the two sides are in different units.

## Texture upload: sixteen megabytes per image, and a per-pixel colour model

`TextureUtil.uploadTextureImageSubImpl` sizes its staging array at `4194304 / width * width` —
four million ints regardless of the texture, so a **16MB heap allocation to upload a 16x16
icon**. It then fills that array with `BufferedImage.getRGB`, which resolves each pixel through
the image's colour model one at a time.

Neither is necessary. The array is now reused across uploads, held at the largest size any
upload has needed. And where the image is already in a layout the upload can read — 32-bit ARGB
backed by an int array, or the interleaved ABGR and BGR byte rasters `ImageIO` returns for PNG
— the pixels are taken directly instead.

| | |
| --- | ---: |
| uploads in one session | 195 |
| taken on a direct path | **145 (74%)** |
| pixels moved | 3,686,224 |
| staging allocation avoided | **~3.1 GB** |

The direct path was 25% before the byte rasters were handled; most PNGs do not load as
`TYPE_INT_ARGB`. Each of the three layouts is recognised only under exact conditions — one
bank, no offset, no per-pixel or per-row padding — and anything failing them falls back to
`getRGB` into the same reused array, so the allocation saving is unconditional and the read
saving is not.

**Verified by screenshot rather than by counter.** A wrong channel order here is every texture
in the game corrupted, and a counter cannot see that. `entity-dense` shot with the setting off
and on: 0.139%, 0.253% and 0.378% of pixels differ against a 0.5% limit, which is the scene's
own particle and entity motion. A swapped channel would be near total.

**No timing claim is made.** Uploads do not happen during steady rendering, so the frame rates
these runs reported say nothing about it, and load and reload times were not measured. What is
established is the mechanism, its coverage, and that it changes no pixels.

## Inside the visible-chunk walk: the allocation is 4% of it

The plan was to pool `ContainerLocalRenderInformation` and its `EnumSet`, on the reasoning that
one of each per visible chunk per frame is allocation churn on the hot path. Splitting the
method into its three segments first, on flat-orbit:

| | |
| --- | ---: |
| `setupTerrain` total | 886-891us |
| **the walk** | **836-838us (94%)** |
| the tail: dispatcher cleanup and dirty-chunk collection | 46-50us (5%) |
| containers built per frame | 2121-2239 |
| **per container** | **0.375-0.395us** |

At 437 fps that is 960,000 containers a second and, with the `EnumSet` each carries, about 1.9
million objects. Which sounds like the answer and is not: 0.38us is roughly 1100 cycles, and two
TLAB allocations are perhaps 40 of them. **Pooling would buy about 4% of the walk.** The idea is
dropped.

What the 1100 cycles actually are is six neighbour evaluations per chunk — a cached offset
lookup, two range checks, a view-frustum array index, an `EnumSet` membership test, a visibility
bitset check and a six-plane frustum test — at roughly 180 cycles each. There is no fat in that;
it is the walk doing its job.

**And 891us against a 2.17ms frame is 41% of it, the largest single item this campaign has
measured.**

### Which makes the idea dismissed earlier the right one after all

On the pit recording only 4-6% of rebuilds followed camera movement, and that was taken as
grounds for dropping the roadmap's plan to relax the movement threshold. That reading was wrong,
and the reason is in the workload: **a replay's free camera does not move unless a viewer moves
it, and nobody was moving it.** Flat-orbit, whose camera does move, attributes 100% of rebuilds
to it — and real play is flat-orbit, not the recording.

At 437 fps the camera travels a sub-pixel distance between frames, and the visible set is being
rebuilt 437 times a second to the same answer. That is the 838us, and reuse is what addresses
it.

So the roadmap's §4.1 stands, with one correction to how it should be judged: the recorded
matches cannot price it, because their cameras are stationary and the whole question is what
happens when a camera moves. Flat-orbit or a walked scenario is the instrument for this one.

## Reusing the visible-chunk list: the walk drops by a third

Forge rebuilds the visible list on any camera movement at all, tested by exact inequality, so a
sub-pixel frame counts. The reuse keeps the list until the camera has left a threshold measured
from where the list was last actually built — not from the previous frame, which is the version
that never fires because the anchor follows the camera and the movement never accumulates.

Thresholds: a quarter of a block, half a degree, any chunk crossing, any change to field of view
or window size, and a rebuild at least every 200ms regardless. That last one is not for any
invalidation named above; it is for the ones that are not, so a missed case is a fifth of a
second of stale visibility rather than a hole that stays until the player turns around.

flat-orbit, `terrainProbe`, off then on:

| | off | on |
| --- | ---: | ---: |
| frames that rebuilt | 100% | **56.3-62.5%** |
| **the walk** | 836-838us | **493-587us** |
| `setupTerrain` amortised | 886-891us | **500-596us** |
| containers built per frame | 2121-2239 | 1213-1371 |

**A third to two fifths off the walk**, which is roughly 300us a frame on this scenario.

The reuse rate is 37-44%, not the 89% the rotation rate and threshold predict — flat-orbit
moves as well as turns, and more to the point it is still streaming chunks, so
`chunksToUpdate` is often non-empty and that forces a rebuild the thresholds never see. On a
world that has finished loading the rate should be higher. The same thing limited the throttle
measurements and is the same fact each time: chunk work forces the walk, and only camera
movement is negotiable.

**Screenshots are identical** — 0.000%, 0.206% and 0.057% of pixels differ on entity-dense
against a 0.5% limit. This is the check that matters here more than any timing: an over-reused
visibility list is missing terrain, missing terrain makes frame times *better*, and a timing
report cannot tell this working from this broken.

**No frame rate claim.** These are single runs and one of them carried a p99 of 15.7ms against
3.9ms, so it was disturbed. The section timing is what is being reported.

**Off by default.** Three screenshots at fixed camera positions are not the same as watching a
world for holes while flying through it, and the failure mode is quiet.

## Entity model transforms: the assumption was backwards, and the fix is the small one

**The machine changed under this campaign.** The development machine's CPU scheduling was
corrected between the earlier sections of this document and this one. A model box measured at
720ns before and measures at **128ns** now — 5.6 times. Every timing baseline above this point
is from the slower machine and needs re-taking; the counts and ratios do not.

The plan for entity rendering rested on display lists being emulated, so twelve `callList` calls
an entity being twelve emulation costs. Two ceiling probes on `entity-dense-quick`, `entityModel`
section CPU p50:

| | entityModel | vs baseline |
| --- | ---: | ---: |
| baseline | 135us | — |
| `noModelCallList` — keep every transform, draw nothing | 99us | −36us (−27%) |
| `noModelTransforms` — keep every draw, transform nothing | 62us | **−73us (−54%)** |

**The transforms are twice what the list replay costs.** The hypothesis was wrong in its
direction, which makes it the fourth assumption about where cost lives this campaign has
overturned by measuring before building — and the first three each had code planned for them.

So the geometry stays exactly where it is. Vanilla places a box with a translate for its offset,
a translate for its rotation point and up to three rotates, then undoes the offset by hand: as
many as six fixed-function calls per box, about a thousand boxes a frame here, each `glRotatef`
building a rotation matrix from a sine and a cosine. `ComposedModelTransform` composes the whole
chain into one matrix and multiplies it in once, cached on the values it was built from so a box
whose pose has not changed does not recompute it — which is every box of every armour stand.

| entity-dense | off | on |
| --- | ---: | ---: |
| `entityModel` | 133us | **119us (−10.5%)** |
| `entityRender` | 345us | 337us |
| avg fps | 497.5 | 510.8 (+2.7%) |

**Screenshots identical**: 0.001%, 0.080% and 0.149% of pixels differ against a 0.5% limit,
which is the scene's own motion. That is the check that matters — a limb placed wrong or not
drawn makes frame time better.

Small here, and kept anyway. It deletes fixed per-frame CPU work — five GL calls per box across
a thousand boxes — on a machine whose bottleneck is elsewhere. On a CPU-bound machine that is
the whole of the saving rather than a tenth of a section, and this client's users are not all on
this hardware. The measurement bar for a change that draws the same picture, adds no failure
mode and reverts cleanly is that it is not negative, not that it is visible here.

## 原版字体批量绘制 — `BatchVanillaFont`

`CustomHudFont` 是单项收益最大的开关，默认关着，理由不是性能而是字形不是 Minecraft 的。
问题因此是：能不能在**不动画面**的前提下拿到同一份收益。原版的价格来自
`FontRenderer.renderDefaultChar` 每字符一次 `glBegin`/`glEnd`。

Badlion 这条路没走（见 `docs/performance-roadmap.md` §11.2）：他们的 `renderDefaultChar`
仍是每字符 `glBegin(5)`，自己那套是 FreeType 栅格化真实 TTF，与原版贴图无关。没有先例可抄。

### 尝试一：收集进 `WorldRenderer` —— 反向，作废

`text-dense`，三轮交错，`Performance.BatchVanillaFont` off/on：

| `text-dense` | off | on |
| --- | ---: | ---: |
| `hud` p50 | 50, 49, 50 us | **65, 65, 64 us（+30%）** |
| `frameTotal` p50 | 1322, 1306, 1292 us | 1292, 1337, 1297 us |
| avg fps | 537.4, 535.6, 538.6 | 535.9, 536.6, 541.4 |

三轮零重叠，方向确定。整帧看不出来是因为 `hud` 只占 1300us 里的 50us。

**为什么这个结果是干净的**：mixin 的逐字符注入回调在 off 侧同样执行、同样分配
`CallbackInfoReturnable`，所以那份开销**已经在基线里**。多出来的 15us 只能是收集本身 ——
`pos().tex().endVertex()` 每顶点做偏移算术加一次容量检查，四顶点一字形，比省掉的
`glBegin`/`glEnd` 还贵。这是本轮第五次"先测再写"推翻掉一个关于成本位置的假设，也是第一次
推翻的是**已经写完的代码**。

### 像素验证（两版顶点相同，结论通用）

同一系列内 on 的截图对 off 的截图：

| | 总差异像素 | 第一人称手臂区域之外 |
| --- | ---: | ---: |
| front-wide | 498 | **0** |
| front-angled | 2274 | **0** |
| elevated | 1313 | **0** |

全部差异集中在右下角 x[893..1124] y[509..719]，是第一人称手臂的动画相位，跑与跑之间本来就
不确定。**计分板、物品栏、所有文字像素逐位相同。** 顶点数学（`7.99F`/`0.01F`/`0.02F`/`15.98F`
这些溢出防护常量，以及三角带改四边形的绕序）验证无误。

### 尝试二：立即模式，只把 `glBegin`/`glEnd` 提到字符串级

保留原版逐顶点的 `glTexCoord2f`/`glVertex3f`，唯一的变化是括号移出循环。
`ResourceLocation.equals`（每字符两次字符串比较）同时换成引用比较。

| `text-dense` | off | on |
| --- | ---: | ---: |
| `hud` p50 | 49, 49, 50 us | **48, 48, 48 us** |
| `frameTotal` p50 | 1332, 1307, 1297 us | 1310, 1308, 1287 us |
| avg fps | 531.1, 533.4, 533.3 | 534.3, 531.3, 534.8 |

方向翻正，三轮区间不重叠（off [49,50]，on [48,48]），但幅度是 **−1.3us，占 `hud` 的 2.7%、
占整帧的 0.1%**。p50 的分辨率就是 1us，这个结果坐在分辨率地板上。

### 结论：这条路没有收益空间

把 −15us 变成 −1.3us 的唯一改动是顶点写去哪里。反过来读这两个数：**每字符一对
`glBegin`/`glEnd` 在这台 AMD 驱动上大约值 3ns/字形，几乎免费。** 原版字体的成本不在这里。

所以对"原版字体能不能优化到自定义字体的同等水平"这个问题，批量化的回答是**不能** ——
它要拆的那个开销本来就不存在。自定义字体的优势必然来自别处（自建图集、省掉逐字符宽度查表
与状态设置、整条字符串级的缓存），下一步要先给那个差值定价再谈实现。

`BatchVanillaFont` 保留、默认关。它是正收益、像素逐位一致、不引入新的失效模式（原版本身也
在逐字符 `glBegin`，嵌套风险没有变化），但 1.3us 不值得在没实机验证过聊天/GUI/告示牌/书本
这些同样走 `FontRenderer` 的路径之前默认打开。

### 盘子有多大：`CustomHudFont` off/on，`replay-pit`

| `replay-pit` | 原版字体 | 自定义字体 |
| --- | ---: | ---: |
| `hud` p50 | 224, 235, 229 us | **183, 177, 179 us** |
| `frameTotal` p50 | 763, 806, 906 us | 743, 758, 781 us |

`hud` 三轮区间不重叠（原版 [224,235]，自定义 [177,183]）：**−50us，−21.7%**，约占 763us 整帧的
6.5%。批量化拿到的 1.3us 只占其中 2.6%，剩下 48us 在别的地方。

**这一系列的其余数字不可用**：pass 0 有一次 `HARNESS_FAILED`，且原版字体侧逐轮劣化
（516 → 471 → 427 fps，p99 4.0 → 6.6 → 6.7ms），而同样交错的自定义字体侧稳定在 512–523。
`entities`、`terrainSetup`、`terrain` 在原版字体 pass 4 上一起变差，说明那一轮整机受了干扰。
`hud` 在三轮原版字体上是 224/235/229，稳定，所以只有这一行拿来用。

### 48us 在哪：每字符三次线性扫描

读原版源码找到的，不是猜的。`FontRenderer` 用 `indexOf` 在一个 **256 字符**的常量串里找字形位置，
**三个调用点全部是每字符一次**：

| 位置 | 频率 |
| --- | --- |
| `renderStringAtPos` | 每渲染字符一次（决定阴影偏移） |
| `renderChar` | 每渲染字符一次（选默认页还是 unicode 页），粗体再一次 |
| `getCharWidth` | 每测量字符一次 —— 而 `getStringWidth` 逐字符调它，HUD 为了居中/右对齐几乎对每条字符串都测一遍 |

所以画一个字符至少两次 256 长度的线性扫描，量一个字符再加一次。`FastGlyphLookup`
（`FontRendererMixin_GlyphIndex`）换成一次数组读。返回值与原版逐个相同，只是找法不同。

查表在运行时从接收者本身构建、按引用识别：那个常量在 `FontRenderer` 字节码里是内联字面量，
1.8.9 没有字段持有它（`ChatAllowedCharacters` 暴露的是另一个集合的 `char[]`），把 256 个制表符
和带音标拉丁字母抄进源文件是白担一份正确性风险。倒序填表，因为 `indexOf` 返回首次出现而那个
串里有重复的 NUL 填充。

### `FastGlyphLookup` off/on，`replay-pit`

| `replay-pit` | off | on |
| --- | ---: | ---: |
| `hud` p50 | 223, 226, 229 us | **202, 194, 201 us** |

三轮区间不重叠（off [223,229]，on [194,202]）：**−27us，−11.9%**。

对照上面 `CustomHudFont` 量出的 50us 差值：**查表一项就拿回 54%，而且画面不变。**
加上 `BatchVanillaFont` 的 1.3us，两项合计约 28us / 56%。

这一系列的 `frameTotal` 同样不可用（pass 0 又一次 `HARNESS_FAILED`，pass 3 off 掉到 421fps、
pass 4 on 掉到 481fps，p99 分别 7.1ms 和 6.7ms，是整机受扰不是变体差异）。`hud` 在两侧各三轮
都稳定，只用这一行。**这台机器在长系列后段反复出现被打断的运行，是仪器的已知缺口。**

### 两项合并，五分钟档，`text-dense-quick`

| `text-dense-quick` | 全关 | `FastGlyphLookup` + `BatchVanillaFont` |
| --- | ---: | ---: |
| `hud` p50 | 50 us | **38 us（−24%）** |

每变体一轮，是筛子不是判决。方向与 `replay-pit` 上的长系列一致。

**像素**：三张截图的差异像素**全部**落在第一人称手臂区域（x[894..1114] y[510..719]），
文字区域为 0。但 `front-wide` 的总差异 0.552% 越过了 0.5% 的门限 —— 那是我把截图放在
t=1000 造成的：手持物品的装备动画此时还在插值，是运行间差异最大的时刻（t=3000/5000 只有
288 和 435 像素）。截图已移到 t=3200/4500/5800，并跑同配置对同配置的空对照校准门限，
而不是把时间点调到通过为止。

### 五分钟档的噪声地板（空对照，`text-dense-quick`，同配置两轮）

| | a | b | 离散 |
| --- | ---: | ---: | ---: |
| avg fps | 474.7 | 374.0 | **21%** |
| `frameTotal` p50 | 1416 us | 1546 us | 9.2% |
| `terrainSetup` | 370 us | 489 us | **32%** |
| `entities` | 704 us | 736 us | 4.5% |
| `hud` | 53 us | 55 us | **3.8%（2us）** |

`hud` 的地板是 2us，字体两项合并的 −12us 是它的六倍，所以那个筛子结果读得出来。
`terrainSetup` 和整帧的地板比大多数候选效果还大，这一档对它们不可用。n=1，别当精确值。

**像素闸门校准**：截图移到 t=3200/4500/5800 后，空对照三张全过
（0.028% / 0.092% / 0.192%，门限 0.5%），最差的一张有 2.6 倍余量。

## 阴影合并 — `MergeTextShadow`

原版把阴影当作**另一次完整调用**发出来（`drawString` 里 `renderString(x+1,y+1,shadow)` 再
`renderString(x,y)`），而我们的钩子挂在 `renderString` 上，看到的是两条互不相关的字符串：两次录制、
两份**几何完全相同**的缓存条目、两次查找（其中一次 `"s"+text` 分配）、**两次 draw call**。

`record()` 里 `shadowPass` 只影响 `currentRgb`，`penX`／`baseline`／`shear`／`glyph`／`step`／
`advance` 全部不受影响；而阴影对颜色做的变换就是 `(rgb & 0xFCFCFC) >> 2`，可以推迟到提交时做。
所以 `Recorded` 的几何数组一份都不用加，改挂 `drawString`、一次录制、两遍写进同一批次。

### 结果

`text-dense-quick`（五分钟档，各一轮，`CustomHudFont` 开）：

| | off | on |
| --- | ---: | ---: |
| `drawString` 调用/帧 | 39.8 | 40.5 |
| 带阴影/帧 | 7.3 | 7.5 |
| 合并/帧 | 0 | 7.5 |
| `hud` p50（两轮独立） | 119, 125 us | **115, 119 us** |

**−4 到 −6us**，噪声地板 2us。每去掉一次 draw call 约 0.54–0.81us，与 §13 用两场景反解出的
0.88us/条落在同一区间 —— 一个独立的佐证。

`replay-pit` 上每帧 9.2 条带阴影（总 30.5），合并后总 draw call 少 23%。

### 计数器把"功能没跑"和"功能没用"分开了

第一次在 `text-dense` 上测，`mergedShadowDraws` = 0，像素闸门"干净通过"。加计数器才看清：
`drawStringCalls` = 32.9/帧说明**钩子生效**，`drawStringShadowed` = 0 说明**场景一条带阴影的
文字都没有**。这两种情况在时间和像素上长得一模一样。

修法是给 `text-dense-quick` 的物品栏加堆叠物品（数量是这个场景里唯一走 `drawStringWithShadow`
的东西），**选中槽位留空** —— 手持物品会摆动，两轮同构建曾因此差 17782 像素。

### 闸门抓到一个真 bug，但方向是反的

给 `submit` 加 `shadow` 标志时，把 `shadowPass` 传给了**非偏移**的那次提交。旧的逐 pass 路径里
颜色已经被 mixin 压暗过、§ 颜色也已在 `record()` 时烘暗，于是压了两次：白字阴影出来是
`0x0F0F0F`（实测像素 19–34），而原版是 `0x3F3F3F`（合并路径实测 57–62，正确）。

**闸门报告的是 `on` regressed，坏的却是 `off`。** 闸门只做比较，不判断哪边是对的 —— 差一点
因此把正确的实现默认关掉。

修复后：三张截图在 x<820（含全部 HUD 文字）**差异像素为 0**，物品数量行为 0，两轮独立复现。
剩余差异全在第一人称手臂，与文字无关。

**这个 bug 从加标志那一刻就存在，前两次跑都"通过"了** —— 因为那两次场景里有 0 条带阴影文字。
一个覆盖不到被测功能的闸门，通过和失败都没有信息量。

### 像素闸门对性能改动的结构性缺陷

修复后的 off/on 与同配置空对照，`text-dense-quick`：

| front-wide | 空对照 | off vs on |
| --- | ---: | ---: |
| 总差异 | 363 px (0.039%) | 6102 px (0.573%) |
| x<820（全部 HUD 文字） | 0 | 79 |
| 物品数量行 | 0 | 0 |

差异是真的，但**不在文字上**：绝大部分在第一人称手臂，其余散布到 x=77 y=369 这类世界几何。

原因是**手臂旋转和实体动画都朝目标值平滑插值，插值步数取决于跑了多少帧**，而 off/on 的帧率不同
（475.8 vs 458.7 fps）。空对照干净是因为两轮配置相同、帧率接近、插值停在同一相位。

**所以任何改变帧率的优化都会移动带插值的内容 —— 而那就是性能优化的定义。** 这一档的像素判定
必须配合区域定位来读：只看 0.573% FAIL 的话，这次会连续两次得出错误结论（先以为合并是错的，
再以为修复没生效）。

**不给闸门加区域白名单。** 那会让它同时不再能抓缺地形，而那是它存在的首要理由 —— 缺geometry
会让帧时间**变好**，时间报告分辨不出裁剪做对了还是做漏了。判定归判定，定位归定位，两步都要做。

## 混淆降频 — `SlowObfuscation`（机制成立，收益未证实）

50ms 为一个 epoch 复用录制结果，让混淆串进入几何缓存（原本 `text.indexOf("§k") >= 0` 直接排除）。

`replay-bedwars`，五分钟档，各一轮，`CustomHudFont` 开：

| | off | on |
| --- | ---: | ---: |
| 混淆串/帧 | 8.8 | 9.2 |
| **缓存命中率** | **0%**（设计如此） | **98.0%** |
| `hud` p50 | 163 us | **168 us** |
| avg fps | 477.9 | 376.7 |

**机制完全成立**：98.0% 命中率，即每帧约 9.0 次 `record()` 被省掉。这是计数，扛得住噪声。

**时间不可用**：这一对的 avg fps 差 21%、p99 双双 13–14ms，是机器受扰的特征。`hud` 的 +5us 落在
一个已经不可信的配对里 —— 而按 §2.0，五分钟档本来就是筛子不是判决。

**两个数字互相矛盾，值得记下来**：按早前探针，bedwars 的 `emit` 是 72.8–84.3us / 约 60 条 ≈
1.25us 每条，省掉 9 条应当是 −11us；实测 +5us。要么混淆串的单条布局成本远低于平均（它们通常很短），
要么这次测量是坏的。**没有第三次运行来分辨，所以两种可能都留着。**

**这是四个字体开关里唯一改变画面的一个**（20Hz vs 帧率刷新）。在没有证实收益之前不应该默认开，
而它的天花板是随服务器的混淆文字量变的 —— 这段录像每帧 9.2 条，别的服务器可能多得多。

## 仪器：录像基准从此可比较

### 问题

回放场景的测量窗口是录像上的一段**挂钟时间**，而 discard 阶段**以帧时间稳定为结束条件**
（`discardMillis` 只是下限）。稳定得慢的一轮，窗口就落在录像的后面 —— 看到的是另一段战斗。

这不是理论风险。entityLayers 的第一次天花板探针在 `replay-pit` 上跑，三个变体分别渲染了
**21.5 / 23.0 / 24.9 个实体**、**231.7 / 279.1 次地形绘制**，而删掉全部盔甲的变体
`entityLayers` 反而从 108us 涨到 122us。整轮作废。

### 修法

`replayMeasureFromMillis` 把窗口两端都锚到录像位置。第一版只做对一半：锚点写成了下限，而两轮
达到稳态时录像已越过它，**真正决定起点的还是稳态时刻**（错位 249ms，视野内玩家数差 22%）。
第二版把锚点抬到最差稳态时刻之后，并在越过时明确警告而不是安静地测错位置。

结果里新增 `replayWindow: {fromMillis, toMillis}` —— **对齐从此可验证，而不是被假设**。

### 效果（同配置两轮，`replay-pit`）

| | 修之前 | 修之后 |
| --- | ---: | ---: |
| 录像窗口错位 | 249 ms | **20 ms** |
| `entitiesRendered`/帧 | 23.8 vs 22.1 | 22.2 vs 22.4 |
| `armorLayerRenders`/帧 | 9.6 vs 7.9（22%） | 9.7 vs 9.5（2%） |
| `terrainDrawCalls`/帧 | 268.5 vs 213.3（26%） | 289.9 vs 289.8（**0.03%**） |

**它修的是工作量的确定性，不是机器噪声。** 同配置两轮的 p50 仍然是 2.723 vs 2.915（7%）。
这两件事之前混在一起，任何差异都无法归因；现在只剩一个。

### `benchmark/trace.py`

`frameNanos`（逐帧序列）一直写在结果里，从来没人读过。摘要只能回答"哪轮更快"，序列能回答
"是全程更快，还是某一轮撞上了另一轮没撞上的东西" —— 这两者的修法不同。

工具做三件事：核对 `replayWindow` 是否对齐、核对天花板探针**假设不变**的负载计数是否真的没变、
把整轮切成十段看方向是否一致。拿它跑上面那次失败的探针，它自动报出了我手工才发现的问题，
还多找到一条（地形绘制差 20%）。

**阈值也踩过一次**：第一版对齐容差写了 250ms，恰好把 249ms 的错位判成 "aligned"。
容差是我定的，它替我掩盖了问题。已收到 50ms。

## 实体图层：手持物才是大头 —— `CacheItemModels`

### 定价（`armor-dense-quick`：103 个全套钻石装 + 持剑的盔甲架）

| | `entityLayers` | 删掉的部分 | 每件 |
| --- | ---: | ---: | ---: |
| base | 734 us | — | — |
| noarmor | 543 us | 盔甲 191 us（26%） | 0.46 us |
| nohelditem | 301 us | **手持物 433 us（59%）** | **4.2 us** |

**一把剑抵九件盔甲。** 十段方向一致。这与本项目此前的注意力方向相反 —— 之前的实体工作全在模型和
盔甲上。原因是 1.8.9 把 2D 贴图**挤出成实体**：正面、背面，加上轮廓每一条 texel 边一个侧面四边形，
而 `RenderItem.renderModel` 每个实体每帧把这上百个四边形重新拷进缓冲再 draw 一次。

### 结果（仪器修好后，两轮负载完全一致）

| | off | on |
| --- | ---: | ---: |
| `itemModelListHits`/帧 | 0 | 102.9（全部命中） |
| `entitiesRendered`/帧 | 102.9 | 102.9 |
| `entityLayers` | 721 us | **392 us（−46%）** |
| `frameTotal` CPU | 2236 us | **1935 us（−13.5%）** |
| `frameTotal` **GPU** | **2439 us** | **2442 us** |
| avg fps | 275.4 | 275.4 |

**CPU 少了 301us，帧率一点没动，而 GPU 计时解释了为什么**：GPU 每帧要 2.44ms，比 CPU 还长，
帧不可能快过它。省下的 CPU 时间无处兑现 —— **这是实测，不是推断**。

所以这一项的正确表述是：**它删除的是真实的 CPU 工作，在这台机器上不转化为帧率**。对 CPU 受限
的机器（弱 CPU 配尚可的显卡，或分辨率低的场景）才会转化。留存理由与 `ComposedModelTransform`
同一条，不把 `−46%` 当帧率收益报。

### 第一版一次都没生效

`itemModelListHits = 0`、`recorded = 0`。原因是我写了"有 tint index 就不缓存"，而
`ItemModelGenerator` 把**图层序号**写进 `tintIndex`（`new BlockPartFace(null, i, ...)`），于是每个
2D 物品模型都被拒。**有 tint ≠ 颜色会变** —— `getColorFromItemStack` 对钻石剑返回白色。改成把
解析后的颜色纳入缓存键，同一模型超过 8 种颜色才整体放弃（染色皮甲）。

**只看时间的话，第一版会显示 `entityLayers` 723 vs 726 —— 一个完美的"没有收益"，然后这条路
就被放弃了。** 是计数器立刻报了 0。本轮第三次「功能没跑」和「功能没用」长得一模一样。

### 仪器：镜头路径也要锚定

`entitiesRendered/帧` 曾是 102.6 vs 112.8 —— 一个静止盔甲架、路径驱动镜头的场景差 10%。
`pathStartMillis` 只在运行开始时设置，而窗口在稳态到达时才打开，稳态时刻每轮不同。
和回放窗口那个 bug 同类，上次只修了一半。进入测量时重置路径时钟后：**102.9 vs 102.9**。
