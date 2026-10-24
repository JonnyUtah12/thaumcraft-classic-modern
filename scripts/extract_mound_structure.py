#!/usr/bin/env python3
"""Extract the WorldGenMound structure table from the decompiled original.

Parses every world.func_147449_b (setBlock, meta 0) and world.func_147465_d
(setBlockWithNotify, explicit meta) call in WorldGenMound.generate/generate2
and emits src/main/java/com/jonnyutah12/thaumcraftclassic/worldgen/
MoundStructure.java — a compact per-(y,x) row table the runtime generator
decodes. The tile/loot tail of generate2 (chests, spawners, loot crates) is
NOT part of the table; it is hand-ported in WorldGenMoundGen.

Usage: python3 scripts/extract_mound_structure.py [reference-java-path]
"""
import re
import sys

REF = sys.argv[1] if len(sys.argv) > 1 else \
    "../reference/decompiled/thaumcraft/thaumcraft/common/lib/world/WorldGenMound.java"
OUT = "src/main/java/com/jonnyutah12/thaumcraftclassic/worldgen/MoundStructure.java"

# SRG field -> MCP name (reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv)
FIELDS = {
    "field_150350_a": "air",
    "field_150346_d": "dirt",
    "field_150349_c": "grass",
    "field_150347_e": "cobblestone",
    "field_150341_Y": "mossy_cobblestone",
    "field_150417_aV": "stonebrick",
    "field_150411_aY": "iron_bars",
    "field_150468_ap": "ladder",
    "field_150446_ar": "stone_stairs",
    "field_150329_H": "tallgrass",
}
# table alphabet -> (mcp name, meta). Stairs metas 0..7 = 1.7.10
# enumFacing (0 east,1 west,2 south,3 north,+4 upside-down).
NAME_TO_CHAR = {("air", 0): "a", ("dirt", 0): "d", ("grass", 0): "g",
                ("cobblestone", 0): "c", ("mossy_cobblestone", 0): "m",
                ("stonebrick", 0): "n", ("stonebrick", 1): "o",
                ("stonebrick", 2): "p", ("stonebrick", 3): "q",
                ("iron_bars", 0): "r", ("ladder", 0): "l", ("ladder", 5): "L",
                ("stone_stairs", 0): "s", ("stone_stairs", 1): "u",
                ("stone_stairs", 2): "v", ("stone_stairs", 3): "w",
                ("stone_stairs", 4): "x", ("stone_stairs", 5): "y",
                ("stone_stairs", 6): "z", ("stone_stairs", 7): "A",
                ("tallgrass", 0): "t", ("tallgrass", 1): "f"}

CALL = re.compile(
    r"world\.func_(?:147449_b|147465_d)\("
    r"i ([-+] \d+|\d+), j ([-+] \d+|\d+), k ([-+] \d+|\d+), "
    r"Blocks\.(field_\w+)(?:, (\d+), \d+)?\);")

# Tile/loot tail blocks — hand-ported in WorldGenMoundGen, not table entries:
# chest, trapped_chest, tnt, mob_spawner.
TAIL = {"field_150486_ae", "field_150447_bR", "field_150335_W", "field_150474_ac"}


def off(text):
    t = text.replace(" ", "")
    return int(t)


def main():
    src = open(REF, encoding="utf-8").read()
    entries = []
    unknown = set()
    for m in CALL.finditer(src):
        dx, dy, dz = off(m.group(1)), off(m.group(2)), off(m.group(3))
        field, meta = m.group(4), int(m.group(5) or 0)
        if field in TAIL:
            continue
        if field not in FIELDS:
            unknown.add(field)
            continue
        name = FIELDS[field]
        ch = NAME_TO_CHAR.get((name, meta))
        if ch is None:
            unknown.add(f"{field} meta {meta}")
            continue
        entries.append((dx, dy, dz, ch))

    if unknown:
        sys.exit(f"unmapped blocks referenced (tail is hand-ported, this is a bug): {sorted(unknown)}")

    xs = [e[0] for e in entries]
    ys = [e[1] for e in entries]
    zs = [e[2] for e in entries]
    x0, x1, y0, y1, z0, z1 = min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)

    # last write wins (the original applies calls in source order)
    grid = {}
    for dx, dy, dz, ch in entries:
        grid[(dy, dx, dz)] = ch

    rows = []
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            row = "".join(grid.get((y, x, z), ".") for z in range(z0, z1 + 1))
            if "." not in row:
                rows.append(f"{y},{x}|{row}")
            else:
                rows.append(f"{y},{x}|{row}")

    counts = {}
    for ch in grid.values():
        counts[ch] = counts.get(ch, 0) + 1

    body = "\n".join(f'            "{r}",' for r in rows)
    java = f'''package com.jonnyutah12.thaumcraftclassic.worldgen;

/**
 * The shrine-mound block table, generated verbatim from the original
 * {{@code WorldGenMound.generate}}/{{@code generate2}} setBlock calls
 * (reference/decompiled/thaumcraft/thaumcraft/common/lib/world/WorldGenMound.java)
 * by {{@code scripts/extract_mound_structure.py}}. Do not edit by hand —
 * regenerate, and keep MoundStructureParityTest green.
 *
 * <p>Each row is {{@code "y,x|zzz..."}}: absolute offsets from the structure
 * anchor (the original's i/j/k), z from {z0} to {z1}, '.' = no placement.
 * Later source-order calls overwrite earlier ones; the table encodes the
 * final state per cell, which is what the original's sequential setBlock
 * calls produce (no call re-reads the world between writes).
 * Transcription parity is pinned by MoundStructureParityTest.</p>
 *
 * <p>Alphabet: a=air d=dirt g=grass block c=cobblestone m=mossy cobblestone
 * n/o/p/q=stonebrick meta 0..3 r=iron bars l=ladder
 * s/u/v/w/x/y/z/A=stone_stairs meta 0..7 (1.7.10 enumFacing; modern
 * cobblestone stairs) t=tallgrass meta 0 f=fern. The chest/trapped-chest+TNT trap, loot
 * crate/urn and mob-spawner tail of the original's generate2 is hand-ported
 * in WorldGenMoundGen (it is tile/loot logic, not a block table).</p>
 *
 * <p>Bounds: x {x0}..{x1}, y {y0}..{y1}, z {z0}..{z1}; {len(entries)} source
 * calls, {len(grid)} final cells. Per-block counts: {sorted(counts.items())}.</p>
 */
final class MoundStructure {{

    /** Row-major table; see class javadoc for the format. */
    static final String[] ROWS = {{
{body}
    }};

    static final int X_MIN = {x0};
    static final int X_MAX = {x1};
    static final int Y_MIN = {y0};
    static final int Y_MAX = {y1};
    static final int Z_MIN = {z0};
    static final int Z_MAX = {z1};

    private MoundStructure() {{
    }}
}}
'''
    open(OUT, "w", encoding="utf-8").write(java)
    print(f"wrote {OUT}: {len(rows)} rows, {len(grid)} cells, bounds x{x0}..{x1} y{y0}..{y1} z{z0}..{z1}")
    print("counts:", sorted(counts.items()))


if __name__ == "__main__":
    main()
