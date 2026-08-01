"""Compare a run's screenshots against stored references.

The reason this exists: skipping geometry that should have been drawn makes frame
times *better*. A culling bug and a culling win look identical in a timing report —
faster, with no failing test. Rendered output is the only signal that separates them.

Usage:
    compare-shots.py <shots-dir> --reference benchmark/reference/<scenario>
    compare-shots.py <shots-dir> --reference <dir> --accept   # (re)establish references
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

from PIL import Image, ImageChops

# A frame is not bit-identical between runs even without code changes: particle RNG,
# entity animation phase and cloud position all move. How much they move is
# scenario-dependent and has to be measured, not guessed — on the particle-dense
# scenario two runs of the *same* configuration differ by 2.5% of pixels, so a fixed 2%
# limit would have failed identical builds. Pass --null to calibrate the limit from a
# duplicate-baseline run the same way the timing band is calibrated.
CHANNEL_TOLERANCE = 8            # per-channel delta treated as "same pixel"
DEFAULT_MAX_FRACTION = 0.005     # floor when no null control is supplied
NULL_SAFETY_FACTOR = 1.5


def differing_fraction(candidate: Path, reference: Path, diff_out: Path | None = None) -> float:
    a = Image.open(reference).convert("RGB")
    b = Image.open(candidate).convert("RGB")
    if a.size != b.size:
        raise SystemExit(f"{candidate.name}: size {b.size} != reference {a.size}")

    diff = ImageChops.difference(a, b)
    # Collapse to a per-pixel maximum channel delta, then threshold.
    mono = diff.convert("L", (1.0, 0, 0, 0, 0, 1.0, 0, 0, 0, 0, 1.0, 0))
    mask = mono.point(lambda v: 255 if v > CHANNEL_TOLERANCE else 0)
    differing = sum(mask.histogram()[128:])
    total = a.size[0] * a.size[1]
    fraction = differing / total

    if diff_out is not None:
        diff_out.parent.mkdir(parents=True, exist_ok=True)
        ImageChops.multiply(b, mask.convert("RGB")).save(diff_out)

    return fraction


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("shots_dir", type=Path)
    parser.add_argument("--reference", type=Path, required=True)
    parser.add_argument("--null", type=Path,
                        help="shots from a duplicate-baseline variant; the pass limit is "
                             "calibrated from how far these drift from the reference")
    parser.add_argument("--accept", action="store_true",
                        help="copy the shots in as the new reference instead of comparing")
    args = parser.parse_args()

    shots = sorted(args.shots_dir.glob("*.png"))
    if not shots:
        raise SystemExit(f"no screenshots in {args.shots_dir}")

    if args.accept:
        args.reference.mkdir(parents=True, exist_ok=True)
        for shot in shots:
            shutil.copy2(shot, args.reference / shot.name)
        print(f"accepted {len(shots)} reference image(s) into {args.reference}")
        return

    limit = DEFAULT_MAX_FRACTION
    if args.null:
        null_fractions = [
            differing_fraction(args.null / shot.name, args.reference / shot.name)
            for shot in shots
            if (args.null / shot.name).exists() and (args.reference / shot.name).exists()
        ]
        if null_fractions:
            limit = max(limit, max(null_fractions) * NULL_SAFETY_FACTOR)
            print(f"limit calibrated from null control: {max(null_fractions) * 100:.3f}% "
                  f"x{NULL_SAFETY_FACTOR} = {limit * 100:.3f}%")
            if limit > 0.05:
                print("  note: this scenario's own run-to-run variation is large, so the gate "
                      "here is weak; a static scene gives a much stronger check")

    failures = 0
    for shot in shots:
        reference = args.reference / shot.name
        if not reference.exists():
            print(f"  {shot.name:<20} NO REFERENCE - run with --accept first")
            failures += 1
            continue
        fraction = differing_fraction(shot, reference, args.shots_dir / "diff" / shot.name)
        ok = fraction <= limit
        print(f"  {shot.name:<20} {'PASS' if ok else 'FAIL'}  "
              f"{fraction * 100:.3f}% of pixels differ (limit {limit * 100:.3f}%)")
        failures += 0 if ok else 1

    if failures:
        print(f"\n{failures} screenshot(s) regressed - a timing win here is not trustworthy")
        sys.exit(1)
    print(f"\nall {len(shots)} screenshot(s) match the reference")


if __name__ == "__main__":
    main()
