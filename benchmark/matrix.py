"""Summarise a many-variant series against one baseline.

compare.py answers "is this one change a win", which is the right shape for an A/B and the
wrong shape for a survey: pricing twelve features one pair at a time prints twelve headers
and no way to see which of them is worth pursuing. This prints one row per variant.

The comparison is still paired -- variant minus baseline within the same pass, averaged
across passes -- because that is what cancels the drift across a series. What it does not
do is supply a verdict: a band belongs to a metric, a scenario and a magnitude, so the
caller reads the null row (two variants that should be identical) and judges against that.

Sections are reported as CPU p50 microseconds. On a GPU-bound machine a CPU-side saving
just moves the wait into the next section that touches GL, so the frame-level metrics are
the ones to believe unless the run is known to be CPU-bound.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path

SUMMARY_METRICS = [
    ("avg fps", "avgFps", False),
    ("p50 ms", "p50FrameMs", True),
    ("p99 ms", "p99FrameMs", True),
    ("1% low", "onePercentLowFps", False),
]


def load(results_dir: Path) -> dict[str, dict[int, dict]]:
    out: dict[str, dict[int, dict]] = {}
    for f in sorted(results_dir.glob("*.json")):
        if f.name.startswith("noise-band") or f.name.startswith("band-"):
            continue
        variant, _, index = f.stem.rpartition("-")
        if not index.isdigit():
            continue
        out.setdefault(variant, {})[int(index)] = json.loads(f.read_text(encoding="utf-8"))
    return out


def contamination(run: dict) -> tuple[int, float]:
    """Frames over 20x the median, and the worst of them in milliseconds."""
    frames = sorted(run["frameNanos"])
    if not frames:
        return 0, 0.0
    median = frames[len(frames) // 2]
    bad = [f for f in frames if f > median * 20]
    return len(bad), (max(bad) / 1e6 if bad else 0.0)


def drop_contaminated(by_variant: dict[str, dict[int, dict]], max_bad: int,
                      max_worst_ms: float, max_p50_ms: float) -> tuple[dict[str, dict[int, dict]], dict[str, int]]:
    """Remove runs a desktop stall landed in.

    This machine runs a remote-desktop agent with a virtual display adapter, which
    intermittently costs a run dozens of frames of 130-250ms. It hits about 40% of runs at
    random, independent of the variant, and it moves avg fps and every tail metric far more
    than any change under test. The surviving runs are tightly grouped, so the split is real
    rather than a threshold invented to get a nicer answer: p50 is 1.33-1.53ms on one side and
    1.7-4.5ms on the other.

    The project already treats a frame over 20x the median as disqualifying when deriving a
    noise band; this applies the same rule to the comparison itself.

    Two different faults need catching, and the 20x rule only sees one of them. Some runs are
    not spiky but uniformly slow: p50 of 15.67ms and 19.52ms, which is 64 and 51 frames a
    second, against a clean population at 1.3-1.6ms. Those runs contain *no* frame over 20x
    their own median, because the median is the thing that moved. Frame times pinned near a
    display refresh interval are the compositor pacing the window rather than the client
    rendering slowly, so a p50 an order of magnitude off the clean population disqualifies a
    run regardless of how even it looks.
    """
    kept: dict[str, dict[int, dict]] = {}
    dropped: dict[str, int] = {}
    for name, runs in by_variant.items():
        for index, run in runs.items():
            count, worst = contamination(run)
            slow = run["summary"]["p50FrameMs"] > max_p50_ms
            if slow or count > max_bad or worst > max_worst_ms:
                dropped[name] = dropped.get(name, 0) + 1
            else:
                kept.setdefault(name, {})[index] = run
    return kept, dropped


def paired_pct(base: dict[int, dict], cand: dict[int, dict], pick) -> tuple[float, list[float]]:
    """Mean of the per-pass percentage differences, and the per-pass values behind it."""
    passes = sorted(set(base) & set(cand))
    diffs = []
    for p in passes:
        a, b = pick(base[p]), pick(cand[p])
        if a is None or b is None or a == 0:
            continue
        diffs.append((b - a) / a * 100.0)
    return (statistics.fmean(diffs) if diffs else float("nan")), diffs


def unpaired_pct(base: dict[int, dict], cand: dict[int, dict], pick) -> tuple[float, list[float]]:
    """Difference of the two medians. Used when contamination has broken the pairing."""
    a_vals = [v for v in (pick(r) for r in base.values()) if v is not None]
    b_vals = [v for v in (pick(r) for r in cand.values()) if v is not None]
    if not a_vals or not b_vals:
        return float("nan"), []
    a, b = statistics.median(a_vals), statistics.median(b_vals)
    return ((b - a) / a * 100.0 if a else float("nan")), b_vals


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results_dir", type=Path)
    parser.add_argument("--baseline", default="base")
    parser.add_argument("--sections", default="entities,entityRender,entityLayers,hud,terrainSetup,terrain,particles,frameTotal")
    parser.add_argument("--counters", default="")
    parser.add_argument("--per-pass", action="store_true")
    parser.add_argument("--clean", action="store_true",
                        help="drop runs a desktop stall landed in, and compare unpaired medians "
                             "of what is left (pairing needs both sides clean in the same pass, "
                             "which contamination this heavy rarely leaves)")
    parser.add_argument("--max-bad", type=int, default=2)
    parser.add_argument("--max-worst-ms", type=float, default=100.0)
    parser.add_argument("--max-p50-ms", type=float, default=5.0)
    args = parser.parse_args()

    # Pairing needs both sides clean in the same pass, which contamination this heavy rarely
    # leaves; comparing medians of the surviving runs keeps more of the series.
    compare = unpaired_pct if args.clean else paired_pct

    by_variant = load(args.results_dir)
    if args.clean:
        by_variant, dropped = drop_contaminated(by_variant, args.max_bad, args.max_worst_ms,
                                               args.max_p50_ms)
        total_dropped = sum(dropped.values())
        print(f"dropped {total_dropped} contaminated run(s): "
              + ", ".join(f"{k}x{v}" for k, v in sorted(dropped.items())))
        for name in sorted(by_variant):
            if len(by_variant[name]) < 2:
                print(f"  WARNING only {len(by_variant[name])} clean run(s) for '{name}'")
    if args.baseline not in by_variant:
        raise SystemExit(f"baseline '{args.baseline}' not in {sorted(by_variant)}")
    base = by_variant[args.baseline]

    renderers = {r["gl"]["renderer"] for runs in by_variant.values() for r in runs.values()}
    if len(renderers) != 1:
        raise SystemExit(f"runs span multiple GPUs, not comparable: {renderers}")
    print(f"GPU: {next(iter(renderers))}")
    print(f"baseline: {args.baseline}   variants: {len(by_variant)}\n")

    # Contamination first. A disturbed or unfocused run poisons the tail metrics, and an
    # unfocused one specifically would compare a frame-capped variant against an uncapped
    # one, which looks exactly like a catastrophic regression.
    for name, runs in sorted(by_variant.items()):
        for index, run in sorted(runs.items()):
            s = run["summary"]
            notes = []
            if s.get("disturbedFrames"):
                notes.append(f"{s['disturbedFrames']} disturbed")
            if s.get("unfocusedFrames"):
                notes.append(f"{s['unfocusedFrames']} unfocused")
            frames = sorted(run["frameNanos"])
            if frames:
                median = frames[len(frames) // 2]
                bad = [f for f in frames if f > median * 20]
                if bad:
                    notes.append(f"{len(bad)} over 20x median (worst {max(bad)/1e6:.0f}ms)")
            if notes:
                print(f"  note {name}-{index}: {', '.join(notes)}")

    header = f"{'variant':<11}" + "".join(f"{label:>20}" for label, _, _ in SUMMARY_METRICS)
    print("\n" + header)
    print("-" * len(header))
    for name in sorted(by_variant):
        row = f"{name:<11}"
        for _, key, _ in SUMMARY_METRICS:
            runs = by_variant[name]
            median = statistics.median([r["summary"][key] for r in runs.values()])
            if name == args.baseline:
                row += f"{median:>13.1f}{'':>7}"
            else:
                mean, _ = compare(base, runs, lambda r, k=key: r["summary"][k])
                row += f"{median:>13.1f}{mean:>+6.1f}%"
        print(row)
        if args.per_pass:
            for _, key, _ in SUMMARY_METRICS:
                if name == args.baseline:
                    continue
                _, diffs = compare(base, by_variant[name], lambda r, k=key: r["summary"][k])
                print(f"{'':<11}  {key}: {['%+.1f%%' % d for d in diffs]}")

    sections = [s for s in args.sections.split(",") if s]
    if sections:
        header = f"{'variant':<11}" + "".join(f"{s[:13]:>20}" for s in sections)
        print("\nsection cpu p50 (us)\n" + header)
        print("-" * len(header))
        for name in sorted(by_variant):
            row = f"{name:<11}"
            for section in sections:
                pick = lambda r, s=section: (r["sections"].get(s) or {}).get("cpuMicros", {}).get("p50")
                runs = by_variant[name]
                vals = [v for v in (pick(r) for r in runs.values()) if v is not None]
                median = statistics.median(vals) if vals else float("nan")
                if name == args.baseline:
                    row += f"{median:>13.0f}{'':>7}"
                else:
                    mean, _ = compare(base, runs, pick)
                    row += f"{median:>13.0f}{mean:>+6.1f}%"
            print(row)

    counters = [c for c in args.counters.split(",") if c]
    if counters:
        header = f"{'variant':<11}" + "".join(f"{c[:17]:>19}" for c in counters)
        print("\ncounters, per frame over the measured window\n" + header)
        print("-" * len(header))
        for name in sorted(by_variant):
            row = f"{name:<11}"
            for counter in counters:
                vals = []
                for r in by_variant[name].values():
                    total = r["counters"].get(counter)
                    frames = r["summary"]["frameCount"]
                    if total is not None and frames:
                        vals.append(total / frames)
                row += f"{statistics.median(vals) if vals else float('nan'):>19.2f}"
            print(row)


if __name__ == "__main__":
    main()
