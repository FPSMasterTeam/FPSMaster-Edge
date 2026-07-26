"""Compare two benchmark variants from a run-series result directory.

Decision rule
-------------
Variants alternate within a pass, so both sides of a pair see almost the same machine
state. That matters: in the A-vs-A series the unpaired spread was dominated by drift
across passes (636 to 668 fps over three passes, with both variants moving together),
and pairing removed it -- p50 spread fell from 4.67% to 1.98%.

So the comparison is on the mean paired difference, judged against a band measured the
same way from a series where both "variants" were the same build. With a handful of
pairs there is no significance test worth running; the empirically measured band is the
threshold instead.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path

NANOS_PER_MILLI = 1_000_000.0
NANOS_PER_SECOND = 1_000_000_000.0

# label -> (summary key, lower is better)
METRICS = {
    "p50 frame ms": ("p50FrameMs", True),
    "p99 frame ms": ("p99FrameMs", True),
    "avg fps": ("avgFps", False),
    "1% low fps": ("onePercentLowFps", False),
}
BAND_KEYS = {"p50FrameMs": "p50", "p99FrameMs": "p99",
             "avgFps": "avgFps", "onePercentLowFps": "onePercentLowFps"}


def load_by_variant_and_pass(results_dir: Path) -> dict[str, dict[int, dict]]:
    out: dict[str, dict[int, dict]] = {}
    for f in sorted(results_dir.glob("*.json")):
        if f.name.startswith("noise-band"):
            continue
        variant, _, index = f.stem.rpartition("-")
        out.setdefault(variant, {})[int(index)] = json.loads(f.read_text(encoding="utf-8"))
    return out


def check_contamination(runs: dict[int, dict], name: str) -> None:
    """Surface disturbed and outlier frames rather than averaging them in silently.

    A single frame spanning a dragged window moved a 60k-sample 1% low by 32% during
    development, so a contaminated run has to be visible and rerun, not absorbed.
    """
    for index, run in sorted(runs.items()):
        summary = run["summary"]
        if summary.get("disturbedFrames"):
            print(f"  note: {name}-{index} excluded {summary['disturbedFrames']} disturbed frame(s)")
        if summary.get("unfocusedFrames"):
            print(f"  note: {name}-{index} spent {summary['unfocusedFrames']} frame(s) unfocused")
        frames = sorted(run["frameNanos"])
        if not frames:
            print(f"  WARNING {name}-{index} recorded no frames")
            continue
        median = frames[len(frames) // 2]
        bad = [f for f in frames if f > median * 20]
        if bad:
            print(f"  WARNING {name}-{index} has {len(bad)} frame(s) over 20x median "
                  f"(worst {max(bad) / NANOS_PER_MILLI:.1f}ms) - rerun rather than trust the tail")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results_dir", type=Path)
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--candidate", required=True)
    parser.add_argument("--noise-band", type=Path,
                        help="noise-band.json; without it no verdict is printed")
    args = parser.parse_args()

    by_variant = load_by_variant_and_pass(args.results_dir)
    for name in (args.baseline, args.candidate):
        if name not in by_variant:
            raise SystemExit(f"no result files for variant '{name}' in {args.results_dir}")
    a_runs, b_runs = by_variant[args.baseline], by_variant[args.candidate]

    renderers = {r["gl"]["renderer"] for r in list(a_runs.values()) + list(b_runs.values())}
    if len(renderers) != 1:
        raise SystemExit(f"runs span multiple GPUs, not comparable: {renderers}")
    print(f"GPU: {next(iter(renderers))}")
    check_contamination(a_runs, args.baseline)
    check_contamination(b_runs, args.candidate)

    pairs = sorted(set(a_runs) & set(b_runs))
    if not pairs:
        raise SystemExit("no pass ran both variants; nothing to pair")
    print(f"\n{len(pairs)} pair(s): passes {pairs}\n")

    band = json.loads(args.noise_band.read_text(encoding="utf-8")) if args.noise_band else None
    print(f"{'metric':<16}{args.baseline:>12}{args.candidate:>12}{'paired':>10}{'band':>9}  verdict")

    for label, (key, lower_is_better) in METRICS.items():
        a_vals = [a_runs[p]["summary"][key] for p in pairs]
        b_vals = [b_runs[p]["summary"][key] for p in pairs]
        diffs = [(b - a) / a * 100.0 for a, b in zip(a_vals, b_vals)]
        mean_diff = statistics.fmean(diffs)

        verdict, threshold = "no band", float("nan")
        if band:
            threshold = band[f"{BAND_KEYS[key]}BandPct"]
            if abs(mean_diff) <= threshold:
                verdict = "within noise"
            elif (mean_diff < 0) == lower_is_better:
                verdict = "IMPROVED"
            else:
                verdict = "REGRESSED"
        print(f"{label:<16}{statistics.median(a_vals):>12.3f}{statistics.median(b_vals):>12.3f}"
              f"{mean_diff:>9.2f}%{threshold:>8.2f}%  {verdict}")
        print(f"{'':<16}per-pair: {['%+.2f%%' % d for d in diffs]}")

    if not band:
        print("\nNo noise band supplied: these are candidate results, not a win.")


if __name__ == "__main__":
    main()
