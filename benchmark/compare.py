"""Compare benchmark variants from a run-series result directory.

Standard library only, so it works with whatever Python happens to be installed.

Decision rule
-------------
With a handful of runs per variant there is no formal significance test worth
running: a 3-vs-3 permutation test cannot produce a p-value below 0.1 no matter
how large the effect. So the rule here is empirical instead. Phase 2 measures the
same build against itself and records how far apart two identical variants land;
that spread is the noise band, and a difference smaller than it is reported as
"within noise" rather than as a win.

Frame times are pooled across runs only for the deep tail (0.1% low), which needs
roughly 20k frames before it stops being a handful of samples.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path

NANOS_PER_MILLI = 1_000_000.0
NANOS_PER_SECOND = 1_000_000_000.0


def percentile(sorted_values: list[int], percent: float) -> float:
    index = max(0, min(len(sorted_values) - 1, int(len(sorted_values) * percent / 100.0 + 0.5) - 1))
    return sorted_values[index]


def slowest_tail_fps(sorted_values: list[int], fraction: float) -> float:
    tail = max(1, round(len(sorted_values) * fraction))
    return NANOS_PER_SECOND / statistics.fmean(sorted_values[-tail:])


def outlier_report(frames_sorted: list[int], label: str) -> str | None:
    """Flag samples far above the median.

    Belt and braces for the harness-side DisplayWatch: a disturbance it cannot see
    (a modal dialog, a driver hitch, a title-bar click that never moves the window)
    still shows up as a frame orders of magnitude above the median. These are never
    dropped automatically here — they are surfaced so a contaminated run can be
    rerun rather than quietly averaged in.
    """
    if not frames_sorted:
        return None
    median = frames_sorted[len(frames_sorted) // 2]
    bad = [f for f in frames_sorted if f > median * 20]
    if not bad:
        return None
    return f"{label}: {len(bad)} frame(s) over 20x median, worst {max(bad)/1e6:.1f}ms"


class Variant:
    def __init__(self, name: str, files: list[Path]) -> None:
        self.name = name
        self.runs = [json.loads(f.read_text(encoding="utf-8")) for f in sorted(files)]
        if not self.runs:
            raise SystemExit(f"no result files for variant '{name}'")
        self.frames: list[int] = [n for run in self.runs for n in run["frameNanos"]]
        self.frames.sort()

    @property
    def run_p50s(self) -> list[float]:
        return [run["summary"]["p50FrameMs"] for run in self.runs]

    @property
    def run_p99s(self) -> list[float]:
        return [run["summary"]["p99FrameMs"] for run in self.runs]

    @property
    def run_avg_fps(self) -> list[float]:
        return [run["summary"]["avgFps"] for run in self.runs]

    def summary(self) -> dict:
        return {
            "runs": len(self.runs),
            "pooledFrames": len(self.frames),
            "p50ms": statistics.median(self.run_p50s),
            "p50msSpreadPct": spread_pct(self.run_p50s),
            "p99ms": statistics.median(self.run_p99s),
            "p99msSpreadPct": spread_pct(self.run_p99s),
            "avgFps": statistics.median(self.run_avg_fps),
            "avgFpsSpreadPct": spread_pct(self.run_avg_fps),
            "pooledP99ms": percentile(self.frames, 99.0) / NANOS_PER_MILLI,
            "onePercentLowFps": slowest_tail_fps(self.frames, 0.01),
            "pointOnePercentLowFps": (
                slowest_tail_fps(self.frames, 0.001) if len(self.frames) >= 20_000 else None
            ),
            "glRenderer": self.runs[0]["gl"]["renderer"],
        }


def spread_pct(values: list[float]) -> float:
    """Peak-to-peak spread as a percentage of the median. This is what the noise band measures."""
    if len(values) < 2:
        return 0.0
    return (max(values) - min(values)) / statistics.median(values) * 100.0


def delta_pct(baseline: float, candidate: float) -> float:
    return (candidate - baseline) / baseline * 100.0


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results_dir", type=Path)
    parser.add_argument("--baseline", required=True, help="variant name treated as the A side")
    parser.add_argument("--candidate", required=True, help="variant name treated as the B side")
    parser.add_argument(
        "--noise-band",
        type=Path,
        help="noise-band.json from Phase 2; without it no verdict is printed",
    )
    args = parser.parse_args()

    variants = {}
    for name in (args.baseline, args.candidate):
        files = sorted(args.results_dir.glob(f"{name}-*.json"))
        variants[name] = Variant(name, files)

    a, b = variants[args.baseline], variants[args.candidate]
    sa, sb = a.summary(), b.summary()

    if sa["glRenderer"] != sb["glRenderer"]:
        raise SystemExit(
            f"GL renderer differs between variants ({sa['glRenderer']!r} vs {sb['glRenderer']!r}); "
            "the runs are not comparable"
        )

    print(f"GPU: {sa['glRenderer']}")
    for variant in (a, b):
        for run in variant.runs:
            disturbed = run["summary"].get("disturbedFrames", 0)
            if disturbed:
                print(f"  note: {variant.name} excluded {disturbed} disturbed frame(s)")
        warning = outlier_report(variant.frames, variant.name)
        if warning:
            print(f"  WARNING {warning} - rerun rather than trust the tail statistics")
    print(f"{'metric':<24}{args.baseline:>14}{args.candidate:>14}{'delta':>12}")
    rows = [
        ("p50 frame ms", sa["p50ms"], sb["p50ms"], True),
        ("p99 frame ms", sa["p99ms"], sb["p99ms"], True),
        ("pooled p99 ms", sa["pooledP99ms"], sb["pooledP99ms"], True),
        ("avg fps", sa["avgFps"], sb["avgFps"], False),
        ("1% low fps", sa["onePercentLowFps"], sb["onePercentLowFps"], False),
    ]
    if sa["pointOnePercentLowFps"] and sb["pointOnePercentLowFps"]:
        rows.append(("0.1% low fps", sa["pointOnePercentLowFps"], sb["pointOnePercentLowFps"], False))

    deltas = {}
    for label, va, vb, lower_is_better in rows:
        d = delta_pct(va, vb)
        deltas[label] = (d, lower_is_better)
        print(f"{label:<24}{va:>14.3f}{vb:>14.3f}{d:>11.2f}%")

    print(
        f"\nrun-to-run spread within variant: "
        f"{args.baseline} p50 {sa['p50msSpreadPct']:.2f}%, "
        f"{args.candidate} p50 {sb['p50msSpreadPct']:.2f}% "
        f"({sa['runs']} and {sb['runs']} runs, {sa['pooledFrames']} and {sb['pooledFrames']} frames)"
    )

    if not args.noise_band:
        print("\nNo noise band supplied: report these as candidate results only, not as a win.")
        return

    band = json.loads(args.noise_band.read_text(encoding="utf-8"))
    print(f"\nnoise band (A-vs-A, {band.get('scenario', '?')}): "
          f"p50 +/-{band['p50BandPct']:.2f}%, p99 +/-{band['p99BandPct']:.2f}%")
    for label, (d, lower_is_better) in deltas.items():
        threshold = band["p99BandPct"] if "p99" in label else band["p50BandPct"]
        if abs(d) <= threshold:
            verdict = "within noise"
        elif (d < 0) == lower_is_better:
            verdict = "IMPROVED"
        else:
            verdict = "REGRESSED"
        print(f"  {label:<24}{d:>8.2f}%  {verdict}")


if __name__ == "__main__":
    main()
