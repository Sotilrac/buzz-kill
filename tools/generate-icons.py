#!/usr/bin/env python3
"""Regenerate Android launcher icon resources from tools/icon-1024.png.

Run after re-exporting the icon from tools/icon.html, or invoke via Gradle:

    ./gradlew :app:generateIcons

Reads:
    tools/icon-1024.png   (1024×1024 RGBA, transparent background)

Writes:
    app/src/main/res/mipmap-{mdpi..xxxhdpi}/ic_launcher.webp
    app/src/main/res/mipmap-{mdpi..xxxhdpi}/ic_launcher_round.webp
    app/src/main/res/drawable/ic_launcher_foreground.webp

Requires Pillow:  pip install --user Pillow
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.stderr.write(
        "Pillow is required. Install with: pip install --user Pillow\n",
    )
    sys.exit(2)


ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "tools" / "icon-1024.png"
RES = ROOT / "app" / "src" / "main" / "res"

# Density buckets per Android docs:
# mdpi=1×, hdpi=1.5×, xhdpi=2×, xxhdpi=3×, xxxhdpi=4×.
MIPMAP_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
# Adaptive icon foreground: 108dp × 4 (xxxhdpi).
FOREGROUND_SIZE = 432


def main() -> int:
    if not SRC.exists():
        sys.stderr.write(f"missing source: {SRC}\n")
        return 1

    src = Image.open(SRC).convert("RGBA")
    print(f"source: {SRC.relative_to(ROOT)} ({src.size[0]}×{src.size[1]})")

    for density, size in MIPMAP_SIZES.items():
        scaled = src.resize((size, size), Image.LANCZOS)
        for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
            out = RES / f"mipmap-{density}" / name
            out.parent.mkdir(parents=True, exist_ok=True)
            scaled.save(out, "WEBP", quality=92, method=6)
            print(f"  wrote {out.relative_to(ROOT)} ({size}×{size})")

    fg = src.resize((FOREGROUND_SIZE, FOREGROUND_SIZE), Image.LANCZOS)
    out = RES / "drawable" / "ic_launcher_foreground.webp"
    out.parent.mkdir(parents=True, exist_ok=True)
    fg.save(out, "WEBP", quality=92, method=6)
    print(f"  wrote {out.relative_to(ROOT)} ({FOREGROUND_SIZE}×{FOREGROUND_SIZE})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
