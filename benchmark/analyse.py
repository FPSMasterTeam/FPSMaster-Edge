"""Reads a results directory and says whether it can be believed before saying what it shows.

Two faults have produced wrong conclusions in this project and both are invisible in the summary
numbers a run reports.

The first is inside a run: the opening of the measured window can be much slower than the rest of
it, and the runs with the longest openings were exactly the ones that measured slowest. The harness
now waits for a steady frame time before measuring, which fixed most of it, but not all -- so every
run is checked here and the ones where it failed are named.

The second is across a series: frame rate climbs run over run, +11% to +22% from first to last in
the two series measured so far. Each run is a fresh JVM, so the likeliest cause is the OS page
cache. Comparing variants without accounting for it compares their positions in the queue.

    python benchmark/analyse.py chunkbudget-pit2
"""
import glob
import json
import os
import statistics as st
import sys

# A measured window whose first tenth is this much slower than its steady state did not start
# steady, whatever the harness decided.
OPENING_TOLERANCE = 0.15


def load(tag):
    runs = []
    for path in sorted(glob.glob(os.path.join("benchmark", "results", tag, "*.json"))):
        with open(path) as handle:
            data = json.load(handle)
        runs.append(data)
    runs.sort(key=lambda d: d.get("wallClockUtcMillis", 0))
    return runs


def opening_penalty(run):
    """How much slower the first tenth of the window was than its steady part, as a fraction."""
    frames = run.get("frameNanos") or []
    if len(frames) < 100:
        return 0.0
    tenth = len(frames) // 10
    opening = st.median(frames[:tenth])
    steady = st.median(frames[3 * tenth:])
    return (opening - steady) / steady if steady else 0.0


def trend(values):
    """Least-squares slope of a series against its run index."""
    xs = list(range(len(values)))
    mx, my = st.mean(xs), st.mean(values)
    denominator = sum((x - mx) ** 2 for x in xs)
    return sum((x - mx) * (y - my) for x, y in zip(xs, values)) / denominator if denominator else 0.0


def main(tag):
    runs = load(tag)
    if not runs:
        print("no runs in " + tag)
        return 1

    print("run order")
    suspect = []
    for index, run in enumerate(runs):
        penalty = opening_penalty(run)
        flag = ""
        if penalty > OPENING_TOLERANCE:
            flag = "  <- did not start steady"
            suspect.append(index)
        summary = run["summary"]
        print("  %d %-9s avg %6.1f  p50 %6.3f  p99 %7.3f  opening %+5.1f%%%s"
              % (index, run["variant"], summary["avgFps"], summary["p50FrameMs"],
                 summary["p99FrameMs"], 100 * penalty, flag))

    fps = [r["summary"]["avgFps"] for r in runs]
    slope = trend(fps)
    print("\nseries trend %+.1f fps per run (%+.1f%% across %d runs)"
          % (slope, 100 * slope * len(runs) / st.mean(fps), len(runs)))
    if suspect:
        print("%d of %d runs did not start steady: %s"
              % (len(suspect), len(runs), ", ".join(str(i) for i in suspect)))

    print("\nper variant, raw and with the series trend removed")
    mean_index = st.mean(range(len(runs)))
    mean_fps = st.mean(fps)
    by_variant = {}
    for index, run in enumerate(runs):
        detrended = run["summary"]["avgFps"] - (mean_fps + slope * (index - mean_index))
        by_variant.setdefault(run["variant"], []).append(
            (run["summary"]["avgFps"], detrended, run["summary"]["p50FrameMs"]))

    for variant, values in by_variant.items():
        raw = [v[0] for v in values]
        residual = [v[1] for v in values]
        p50 = [v[2] for v in values]
        print("  %-9s avg %6.1f [%6.1f,%6.1f]  residual %+6.1f [%+6.1f,%+6.1f]  p50 %.3f [%.3f,%.3f]"
              % (variant, st.mean(raw), min(raw), max(raw),
                 st.mean(residual), min(residual), max(residual),
                 st.mean(p50), min(p50), max(p50)))

    print("\nRanges that overlap between variants are not a result, whichever column they are in.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "chunkbudget-pit2"))
