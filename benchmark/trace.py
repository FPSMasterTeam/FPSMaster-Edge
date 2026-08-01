"""Compare two runs frame by frame instead of by their summaries.

Every result already carries `frameNanos`, the whole per-frame series, and nothing
has ever read it. A summary answers "which run was faster"; the series answers
"was it faster the whole way, or did one run hit something the other did not" --
and those have different fixes.

The question that prompted this: a three-way ceiling probe on `replay-pit`
reported a variant that *deleted* work as slower. The counters said the runs had
rendered 21.5, 23.0 and 24.9 entities a frame, so they had not been shown the
same scene. A summary cannot distinguish that from a real regression. A series
can: divergence confined to part of the run is a workload difference, divergence
spread evenly is an effect.

Usage:
    trace.py <a.json> <b.json>              compare two runs
    trace.py <a.json> <b.json> --plot out.txt   ASCII plot of both series
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path


def load(path: Path):
    data = json.loads(path.read_text(encoding="utf-8"))
    return {
        "name": f"{data.get('variant', '?')}",
        "frames": [n / 1e6 for n in data["frameNanos"]],
        "window": data.get("replayWindow"),
        "counters": data.get("counters", {}),
    }


def quantiles(series):
    ordered = sorted(series)
    n = len(ordered)
    pick = lambda q: ordered[min(n - 1, int(q * n))]
    return pick(0.50), pick(0.95), pick(0.99)


def segments(series, count=10):
    """Median of each tenth of the run, so drift and localised spikes show up."""
    size = max(1, len(series) // count)
    return [statistics.median(series[i:i + size])
            for i in range(0, size * count, size)][:count]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("a", type=Path)
    parser.add_argument("b", type=Path)
    parser.add_argument("--plot", type=Path)
    args = parser.parse_args()

    a, b = load(args.a), load(args.b)

    # Alignment first. On a replay, two runs are only comparable frame for frame if they
    # covered the same span of the recording; saying so is the whole point of recording it.
    for run in (a, b):
        window = run["window"]
        print(f"{run['name']:>12}: {len(run['frames'])} frames"
              + (f", replay {window['fromMillis']}..{window['toMillis']}ms" if window else ""))
    if a["window"] and b["window"]:
        drift = abs(a["window"]["fromMillis"] - b["window"]["fromMillis"])
        verdict = "aligned" if drift <= 50 else f"NOT ALIGNED — {drift}ms apart"
        print(f"{'':>12}  replay windows {verdict}")
    elif a["window"] or b["window"]:
        print(f"{'':>12}  one run has a replay window and the other does not")

    print()
    for run in (a, b):
        p50, p95, p99 = quantiles(run["frames"])
        print(f"{run['name']:>12}: p50={p50:6.3f}ms p95={p95:6.3f}ms p99={p99:6.3f}ms")

    # Workload counters that would invalidate the comparison if they disagree. These are the
    # ones a ceiling probe holds still by assumption and has to check by measurement.
    print()
    interesting = ("entitiesRendered", "armorLayerRenders", "heldItemLayerRenders",
                   "terrainDrawCalls", "chunkRebuilds")
    for key in interesting:
        av, bv = a["counters"].get(key), b["counters"].get(key)
        if not av and not bv:
            continue
        an, bn = av / len(a["frames"]), bv / len(b["frames"])
        skew = 0.0 if an == 0 and bn == 0 else abs(an - bn) / max(an, bn, 1e-9)
        flag = "  <-- workload differs" if skew > 0.05 else ""
        print(f"{key:>22}/frame: {an:8.1f} vs {bn:8.1f}{flag}")

    print()
    print("median per tenth of the run (ms):")
    for run in (a, b):
        print(f"{run['name']:>12}: " + " ".join(f"{v:6.3f}" for v in segments(run["frames"])))
    delta = [x - y for x, y in zip(segments(a["frames"]), segments(b["frames"]))]
    print(f"{'delta':>12}: " + " ".join(f"{v:+6.3f}" for v in delta))
    # Effect size against segment spread, not a count of signs.
    #
    # Unanimity was the first rule and it called a +26% frame rate change "not one effect" because
    # three tenths of a noisy replay went the other way. A 7-of-10 majority was the second, and it
    # blessed a pair already known to be invalid — the ceiling probe whose variants had rendered
    # 21.5 and 24.9 entities a frame. Both pairs sit at 7/10, so the sign count cannot separate
    # them at all.
    #
    # What does separate them is how large the typical difference is next to how much the
    # differences vary: the real effect runs +0.58ms against a spread of 0.33, the artefact runs
    # -0.13ms against a spread of 0.25. Median over mean absolute deviation, so one wild segment
    # moves neither term much.
    ordered = sorted(delta)
    mid = len(ordered) // 2
    median = ordered[mid] if len(ordered) % 2 else (ordered[mid - 1] + ordered[mid]) / 2
    spread = sum(abs(v - median) for v in delta) / len(delta)
    ratio = abs(median) / spread if spread else float("inf")
    if ratio >= 1.5:
        verdict = f"clear: median {median:+.3f}ms against spread {spread:.3f} (ratio {ratio:.2f})"
    elif ratio >= 1.0:
        verdict = f"weak: median {median:+.3f}ms against spread {spread:.3f} (ratio {ratio:.2f})"
    else:
        verdict = (f"NOT ONE EFFECT: median {median:+.3f}ms is smaller than the spread "
                   f"{spread:.3f} (ratio {ratio:.2f})")
    print(f"{'':>12}  " + verdict)

    if args.plot:
        rows = []
        for run in (a, b):
            seg = segments(run["frames"], 60)
            top = max(seg) or 1.0
            rows.append(run["name"])
            rows.append("".join("#" if v / top > 0.66 else
                                "+" if v / top > 0.33 else "." for v in seg))
        args.plot.write_text("\n".join(rows) + "\n", encoding="utf-8")
        print(f"\nwrote {args.plot}")


if __name__ == "__main__":
    main()
