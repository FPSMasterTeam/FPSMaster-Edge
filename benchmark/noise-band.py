"""Derive the noise band from an A-vs-A series and write noise-band.json.

The band is the decision threshold every later comparison is judged against. It is
measured, not assumed: both "variants" in the input series are the same build with
no overrides, so any difference between them is pure measurement noise.

The band reported for a metric is the larger of
  - the peak-to-peak spread of that metric across all runs, and
  - the difference between the two nominal variants,
so it cannot be made to look tight by a lucky pairing.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path


SUMMARY_KEYS = {
    "p50": "p50FrameMs",
    "p99": "p99FrameMs",
    "avgFps": "avgFps",
    "onePercentLowFps": "onePercentLowFps",
}


def spread_pct(values: list[float]) -> float:
    if len(values) < 2:
        return 0.0
    return (max(values) - min(values)) / statistics.median(values) * 100.0


def paired_differences(runs_by_variant: dict[str, dict[int, dict]], key: str) -> list[float]:
    """Percent difference (b - a) for each pass where both variants ran.

    A and B alternate within a pass, so both sides of a pair see almost the same
    machine state. The A-vs-A series showed the unpaired spread is dominated by drift
    across passes -- 636 to 668 fps over three passes, with both variants moving
    together -- which pairing removes: p50 spread fell from 4.67% to 1.98%. Judging on
    the paired difference therefore more than halves the smallest detectable effect.
    """
    names = sorted(runs_by_variant)
    if len(names) != 2:
        raise SystemExit(f"pairing needs exactly two variants, got {names}")
    a_runs, b_runs = runs_by_variant[names[0]], runs_by_variant[names[1]]
    diffs = []
    for p in sorted(set(a_runs) & set(b_runs)):
        a, b = a_runs[p]["summary"][key], b_runs[p]["summary"][key]
        diffs.append((b - a) / a * 100.0)
    if not diffs:
        raise SystemExit("no passes have both variants; cannot pair")
    return diffs


def load_by_variant_and_pass(results_dir: Path) -> dict[str, dict[int, dict]]:
    out: dict[str, dict[int, dict]] = {}
    for f in sorted(results_dir.glob("*.json")):
        if f.name.startswith("noise-band"):
            continue
        variant, _, index = f.stem.rpartition("-")
        out.setdefault(variant, {})[int(index)] = json.loads(f.read_text(encoding="utf-8"))
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results_dir", type=Path, help="directory produced by run-series.ps1")
    parser.add_argument("--scenario", required=True)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()

    files = sorted(args.results_dir.glob("*.json"))
    files = [f for f in files if f.name != "noise-band.json"]
    if len(files) < 4:
        raise SystemExit(f"need at least 4 runs to estimate a band, found {len(files)}")

    runs = [json.loads(f.read_text(encoding="utf-8")) for f in files]
    renderers = {r["gl"]["renderer"] for r in runs}
    if len(renderers) != 1:
        raise SystemExit(f"runs span multiple GPUs: {renderers}")

    for run, f in zip(runs, files):
        frames = sorted(run["frameNanos"])
        median = frames[len(frames) // 2]
        bad = [x for x in frames if x > median * 20]
        disturbed = run["summary"].get("disturbedFrames", 0)
        # Two different things look alike in the tail and must not be treated alike. A dragged
        # window or an alt-tab stalls the process for hundreds of milliseconds and is not data. A
        # 20-40ms frame is a GC pause or a driver hitch — it is the client's own behaviour, and
        # discarding it would flatter the numbers. So rejection is on absolute duration, and the
        # relative check only warns.
        worst = max(frames) if frames else 0
        if worst > 100_000_000:
            raise SystemExit(
                f"{f.name} contains a {worst/1e6:.0f}ms frame. A stall that long is the desktop, "
                f"not the client, and a noise band measured from it would hide every real "
                f"regression. Rerun the series undisturbed."
            )
        if bad:
            print(f"  note: {f.name} has {len(bad)} frame(s) over 20x the median "
                  f"(worst {worst/1e6:.1f}ms) - kept, but the tail statistics rest on them")
        if disturbed:
            print(f"  note: {f.name} excluded {disturbed} disturbed frame(s) during capture")

    by_variant = load_by_variant_and_pass(args.results_dir)

    band = {}
    for name, key in SUMMARY_KEYS.items():
        diffs = paired_differences(by_variant, key)
        # Peak-to-peak of the paired null differences: how far apart two identical
        # variants landed in the same pass. Conservative with few pairs, which is the
        # right direction for a threshold that decides whether a change is real.
        band[f"{name}BandPct"] = max(diffs) - min(diffs)
        band[f"{name}PairedDiffs"] = [round(d, 3) for d in diffs]

    all_runs = [r for runs in by_variant.values() for r in runs.values()]
    band.update({
        "scenario": args.scenario,
        "paired": True,
        "pairs": len(next(iter(band[k] for k in band if k.endswith("PairedDiffs")))),
        "runs": len(all_runs),
        "glRenderer": next(iter(renderers)),
        "p50msMedian": statistics.median([r["summary"]["p50FrameMs"] for r in all_runs]),
        "avgFpsMedian": statistics.median([r["summary"]["avgFps"] for r in all_runs]),
    })

    out = args.out or (args.results_dir / "noise-band.json")
    out.write_text(json.dumps(band, indent=2), encoding="utf-8")

    print(f"scenario {args.scenario}: {band['runs']} runs, {band['pairs']} pairs, on {band['glRenderer']}")
    print(f"  median p50 {band['p50msMedian']:.3f}ms, median avg {band['avgFpsMedian']:.1f} fps")
    for name in SUMMARY_KEYS:
        print(f"  {name+'BandPct':<26}{band[name + 'BandPct']:6.2f}%   "
              f"paired diffs {band[name + 'PairedDiffs']}")
    print(f"\nwrote {out}")


def summary_key(metric: str) -> str:
    return {
        "p50": "p50FrameMs",
        "p99": "p99FrameMs",
        "avgFps": "avgFps",
        "onePercentLowFps": "onePercentLowFps",
    }[metric]


if __name__ == "__main__":
    main()
