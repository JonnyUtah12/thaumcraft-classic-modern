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

D-016 option 2 (BOOK-3a) adds one more file: `thaumcraft/lang/en_US.lang`, a
deterministic 25-key subset of the research keys the shipped research graph
references, with placeholder values (never original strings). It exists so CI can
exercise `generateOriginalResearchLang` + `checkGeneratedResearchLang` on the same
fixture tree the asset path contract uses. The file count goes 911 -> 912.
"""

from __future__ import annotations

import argparse
import base64
import csv
import json
import os
import struct
import zlib

# A real 1x1 RGBA PNG. Placeholder content only: the gates care about paths.
PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)


def png_rgba(width, height):
    """A transparent RGBA PNG of the requested size, built with the stdlib only.

    The path gates only care that a texture exists, so every placeholder is 1x1
    — except the particle sheet: `sliceOriginalParticleSprites` (build.gradle)
    cuts 16x16 cells out of `textures/misc/particles.png`, and `getSubimage`
    throws `(x + width) is outside of Raster` on a 1x1 image. The real sheet is
    256x256 with a 16 px cell grid, so the fixture hands the slicer a sheet of
    the same size and the fixture overlay build exercises the slice for real.
    """
    raw = b"".join(b"\x00" + b"\x00\x00\x00\x00" * width for _ in range(height))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


# The one placeholder that has to carry real dimensions (see png_rgba).
PARTICLE_SHEET = "textures/misc/particles.png"
PARTICLE_SHEET_PX = 256

# The sound transform reads this file; the fixture gives it the two camelCase
# ids the build normalizes so the rename path is exercised too.
SOUNDS_JSON = {
    "runicShieldEffect": {"category": "master", "sounds": ["runicShieldEffect"]},
    "runicShieldCharge": {"category": "master", "sounds": ["runicShieldCharge"]},
}

# D-016 option 2 (BOOK-3a): the research lang fixture.
RESEARCH_ENTRIES = "src/main/resources/data/thaumcraft/research_graph/entries.json"
RESEARCH_LANG = "thaumcraft/lang/en_US.lang"
RESEARCH_KEY_COUNT = 25


def research_keys(entries_path, limit=RESEARCH_KEY_COUNT):
    """A deterministic mixture of research keys the shipped graph references.

    Derived from the graph, never a hardcoded list, with the generator's own
    structural rule: each row's `nameKey`/`textKey` plus a page `textKey`, no
    namespace filter (t_6799fd3f removed the `tc.` filter from the generator, and
    this fixture mirrored it). Round-robins name/text/page per entry so the
    fixture covers all three key shapes without becoming 712 keys long.

    Measured on the shipped graph: the first `limit` keys this yields are all
    `tc.`-prefixed, because the graph's single display key outside that namespace
    (`focus.upgrade.vampirebats.text`, on VAMPBAT — row 41) is out of reach at
    this limit. The fixture's lang is only an *input subset*, so what the gate
    asserts is graph-keys ∩ fixture-lang keys (25 today), measured — never a
    hardcoded expectation.
    """
    with open(entries_path, encoding="utf-8") as handle:
        graph = json.load(handle)
    keys = []
    for entry in graph["entries"]:
        candidates = [entry.get("nameKey"), entry.get("textKey")]
        pages = entry.get("pages") or []
        if pages:
            candidates.append(pages[0].get("textKey"))
        for key in candidates:
            if key and key not in keys:
                keys.append(key)
                if len(keys) >= limit:
                    return keys
    return keys


def research_lang_body(keys):
    """Placeholder values, CRLF, one `#` comment — and every parser edge case.

    The values are port-authored placeholders (no original string is involved).
    They deliberately carry what the parser and the writer must survive: a value
    containing `=`, a `\\n` escape, the measured 1.7.10 page markup (`<BR>`, a
    §-colour pair, an `&`-code), a leading space and a trailing space.
    """
    special = {
        0: "Fixture name with a trailing space ",
        1: "Fixture value containing = inside it, and a \\n escape\\nsecond line",
        2: "<BR>§5Fixture page markup§0<BR>a second line with the & code",
        3: " leading space is part of the value",
    }
    lines = ["# ci fixture: 25 research keys, placeholder values (D-004, no original strings)"]
    for index, key in enumerate(keys):
        lines.append(f"{key}={special.get(index, f'Fixture placeholder value {index}')}")
    return "\r\n".join(lines) + "\r\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("out", help="fixture root to write (becomes referenceAssetsDir)")
    parser.add_argument("--asset-mapping", default="docs/asset-mapping.csv")
    parser.add_argument("--research-entries", default=RESEARCH_ENTRIES,
                        help="shipped research graph the fixture lang keys come from")
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
            if not path.endswith(".png"):
                data = (json.dumps({}) + "\n").encode()
            elif source == PARTICLE_SHEET:
                data = png_rgba(PARTICLE_SHEET_PX, PARTICLE_SHEET_PX)
            else:
                data = PNG_1X1
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

    keys = research_keys(args.research_entries)
    lang_path = os.path.join(args.out, RESEARCH_LANG.replace("/", os.sep))
    os.makedirs(os.path.dirname(lang_path), exist_ok=True)
    with open(lang_path, "wb") as out:
        out.write(research_lang_body(keys).encode("utf-8"))

    dirs = {}
    for source in written:
        dirs[source.split("/")[1]] = dirs.get(source.split("/")[1], 0) + 1
    print(f"fixture: {args.out} — {len(written)} placeholder texture files over "
          f"{len(dirs)} directories: " + ", ".join(f"{k}={v}" for k, v in sorted(dirs.items())))
    files = sum(len(names) for _, _, names in os.walk(args.out))
    print(f"fixture: {files} files total ({len(written)} textures + sounds.json + "
          f"2 placeholder oggs + {RESEARCH_LANG}), research lang: {len(keys)} of the "
          f"graph-referenced keys")


if __name__ == "__main__":
    main()
