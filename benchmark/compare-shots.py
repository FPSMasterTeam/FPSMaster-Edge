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

# Tolerances. A frame is not bit-identical between runs even without code changes:
# entity animation phase, particle RNG and cloud position all move slightly. What must
# not change is whether something is on screen at all, which shows up as a large number
# of differing pixels rather than as small per-pixel deltas.
CHANNEL_TOLERANCE = 8          # per-channel delta treated as "same pixel"
MAX_DIFFERING_FRACTION = 0.02  # 2% of pixels may differ beyond that


def compare(candidate: Path, reference: Path, diff_out: Path | None) -> tuple[bool, str]:
    a = Image.open(reference).convert("RGB")
    b = Image.open(candidate).convert("RGB")
    if a.size != b.size:
        return False, f"size {b.size} != reference {a.size}"

    diff = ImageChops.difference(a, b)
    # Collapse to a per-pixel maximum channel delta, then threshold.
    mono = diff.convert("L", (1.0, 0, 0, 0, 0, 1.0, 0, 0, 0, 0, 1.0, 0))
    mask = mono.point(lambda v: 255 if v > CHANNEL_TOLERANCE else 0)
    differing = sum(mask.histogram()[128:])
    total = a.size[0] * a.size[1]
    fraction = differing / total

    if diff_out is not None and fraction > MAX_DIFFERING_FRACTION:
        diff_out.parent.mkdir(parents=True, exist_ok=True)
        ImageChops.multiply(b, mask.convert("RGB")).save(diff_out)

    ok = fraction <= MAX_DIFFERING_FRACTION
    return ok, f"{fraction * 100:.3f}% of pixels differ (limit {MAX_DIFFERING_FRACTION * 100:.1f}%)"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("shots_dir", type=Path)
    parser.add_argument("--reference", type=Path, required=True)
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

    failures = 0
    for shot in shots:
        reference = args.reference / shot.name
        if not reference.exists():
            print(f"  {shot.name:<20} NO REFERENCE - run with --accept first")
            failures += 1
            continue
        ok, detail = compare(shot, reference, args.shots_dir / "diff" / shot.name)
        print(f"  {shot.name:<20} {'PASS' if ok else 'FAIL'}  {detail}")
        failures += 0 if ok else 1

    if failures:
        print(f"\n{failures} screenshot(s) regressed - a timing win here is not trustworthy")
        sys.exit(1)
    print(f"\nall {len(shots)} screenshot(s) match the reference")


if __name__ == "__main__":
    main()
