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


def spread_pct(values: list[float]) -> float:
    if len(values) < 2:
        return 0.0
    return (max(values) - min(values)) / statistics.median(values) * 100.0


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
        if bad:
            raise SystemExit(
                f"{f.name} contains {len(bad)} frame(s) over 20x the median "
                f"(worst {max(bad)/1e6:.1f}ms). A noise band measured from a disturbed run is "
                f"useless - it would hide every real regression. Rerun the series undisturbed."
            )
        if disturbed:
            print(f"  note: {f.name} excluded {disturbed} disturbed frame(s) during capture")

    by_variant: dict[str, list[dict]] = {}
    for run in runs:
        by_variant.setdefault(run["variant"], []).append(run)

    metrics = {
        "p50": [r["summary"]["p50FrameMs"] for r in runs],
        "p99": [r["summary"]["p99FrameMs"] for r in runs],
        "avgFps": [r["summary"]["avgFps"] for r in runs],
        "onePercentLowFps": [r["summary"]["onePercentLowFps"] for r in runs],
    }

    band = {}
    for name, values in metrics.items():
        overall = spread_pct(values)
        variant_medians = [
            statistics.median([r["summary"][summary_key(name)] for r in group])
            for group in by_variant.values()
        ]
        between = spread_pct(variant_medians) if len(variant_medians) > 1 else 0.0
        band[f"{name}BandPct"] = max(overall, between)

    band.update({
        "scenario": args.scenario,
        "runs": len(runs),
        "glRenderer": next(iter(renderers)),
        "frameCountMedian": statistics.median([r["summary"]["frameCount"] for r in runs]),
        "p50msMedian": statistics.median(metrics["p50"]),
        "avgFpsMedian": statistics.median(metrics["avgFps"]),
    })

    out = args.out or (args.results_dir / "noise-band.json")
    out.write_text(json.dumps(band, indent=2), encoding="utf-8")

    print(f"scenario {args.scenario}: {len(runs)} runs on {band['glRenderer']}")
    print(f"  median p50 {band['p50msMedian']:.3f}ms, median avg {band['avgFpsMedian']:.1f} fps")
    for key in ("p50BandPct", "p99BandPct", "avgFpsBandPct", "onePercentLowFpsBandPct"):
        print(f"  {key:<26}{band[key]:.2f}%")
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
