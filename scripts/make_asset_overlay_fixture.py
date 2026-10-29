#!/usr/bin/env python3
"""Rebuild the 1.7.10 (plural) asset tree as a placeholder fixture (TC-M7-01-s-a).

CI has no `../reference/assets` tree: the original Thaumcraft assets are All
Rights Reserved and are never committed (D-004), so CI cannot build an overlay
jar and the asset path contract would never see a remapped texture.

This script reconstructs the *shape* of that tree from committed data alone:
every `docs/asset-mapping.csv` row whose `original_asset` is a texture file is
written to `<out>/thaumcraft/<original_asset>` as a 1x1 PNG (and its `.mcmeta`
as `{}`). No original bytes are involved, and the output stays under `build/`.

The result reproduces the defect's input: `textures/blocks/` + `textures/items/`
(plus the directories that keep their 1.7.10 name — gui/misc/models/aspects/foci,
measured per directory in build.gradle). Point the overlay at it with
`./gradlew -Ptc.referenceAssets=build/ci-fixture-assets` and the checker proves
the jar publishes the singular layout the CSV declares.
"""

from __future__ import annotations

import argparse
import base64
import csv
import json
import os

# A real 1x1 RGBA PNG. Placeholder content only: the gates care about paths.
PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)

# The sound transform reads this file; the fixture gives it the two camelCase
# ids the build normalizes so the rename path is exercised too.
SOUNDS_JSON = {
    "runicShieldEffect": {"category": "master", "sounds": ["runicShieldEffect"]},
    "runicShieldCharge": {"category": "master", "sounds": ["runicShieldCharge"]},
}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("out", help="fixture root to write (becomes referenceAssetsDir)")
    parser.add_argument("--asset-mapping", default="docs/asset-mapping.csv")
    args = parser.parse_args()

    root = os.path.join(args.out, "thaumcraft")
    written = {}
    with open(args.asset_mapping, newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            source = (row.get("original_asset") or "").strip()
            target = (row.get("new_asset") or "").strip()
            if not source.startswith("textures/") or not target:
                continue
            path = os.path.join(root, source.replace("/", os.sep))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            data = PNG_1X1 if path.endswith(".png") else (json.dumps({}) + "\n").encode()
            with open(path, "wb") as out:
                out.write(data)
            written[source] = target

    os.makedirs(root, exist_ok=True)
    with open(os.path.join(root, "sounds.json"), "w", encoding="utf-8") as out:
        json.dump(SOUNDS_JSON, out, indent=2)
    # The overlay excludes thaumcraft/sounds.json and the transform supplies the
    # converted one; placeholder oggs so a sound-driven build never 404s.
    for event in ("runicShieldEffect", "runicShieldCharge"):
        path = os.path.join(root, "sounds", f"{event}.ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as out:
            out.write(b"Oggs")

    dirs = {}
    for source in written:
        dirs[source.split("/")[1]] = dirs.get(source.split("/")[1], 0) + 1
    print(f"fixture: {args.out} — {len(written)} placeholder texture files over "
          f"{len(dirs)} directories: " + ", ".join(f"{k}={v}" for k, v in sorted(dirs.items())))


if __name__ == "__main__":
    main()
