#!/usr/bin/env python3
"""Build docs/feature-matrix.csv — the machine-readable feature matrix.

Single curated source of truth: the system table in docs/FEATURE_MATRIX.md.
This script parses that table and joins it with the content-level rollup from
docs/content-mapping.csv, so the CSV is a *derivative* and cannot drift from
the prose. It never rewrites FEATURE_MATRIX.md.

Run:  python3 scripts/build_feature_matrix.py
Exit: 0 on success; non-zero if the curated table cannot be parsed (so CI can
      catch a hand-edit that breaks the format).
"""
from __future__ import annotations

import csv
import io
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MATRIX_MD = REPO / "docs" / "FEATURE_MATRIX.md"
CONTENT_CSV = REPO / "docs" / "content-mapping.csv"
OUT_CSV = REPO / "docs" / "feature-matrix.csv"

# FEATURE_MATRIX.md system row -> content-mapping.csv `kind` token(s).
# Only rows with a 1:1 kind mapping get a discovered-count join; the rest
# rely on the count already written in the prose table.
SYSTEM_TO_KIND = {
    "Aspects": ["aspect"],
    "Research graph": ["research"],
    "Research progression": ["research"],
    "Thaumonomicon GUI": ["gui"],
    "Research minigames": [],
    "Vis core": [],
    "Aura + nodes": ["node_type", "node_modifier"],
    "Node behavior": [],
    "Flux": [],
    "Taint": [],
    "Worldgen": ["worldgen_feature"],
    "Arcane workbench": ["recipe"],
    "Crucible": ["recipe"],
    "Essentia": [],
    "Wands": ["wand_component"],
    "Blocks": ["block"],
    "Items": ["item"],
    "Tile entities": ["tile_entity"],
    "Enchantments": ["enchantment"],
    "Potions": ["potion"],
    "Entities": ["entity", "spawner_egg", "mob_spawn"],
    "Fluids": ["fluid"],
    "Biomes": ["biome", "dimension"],
    "Recipes": ["recipe", "oredict", "wand_trigger"],
}

ROW_RE = re.compile(r"^\|(?P<cells>.+)\|\s*$")


def parse_matrix() -> list[dict[str, str]]:
    """Parse the curated system table out of FEATURE_MATRIX.md."""
    text = MATRIX_MD.read_text(encoding="utf-8")
    rows: list[dict[str, str]] = []
    header: list[str] | None = None
    for line in text.splitlines():
        m = ROW_RE.match(line.strip())
        if not m:
            continue
        cells = [c.strip() for c in m.group("cells").split("|")]
        if cells and cells[0].lower() == "system":
            header = [c.lower() for c in cells]
            continue
        if header is None or set("".join(cells)) <= set("-: "):
            continue  # separator row
        if len(cells) != len(header):
            continue
        rows.append(dict(zip(header, cells)))
    if not rows:
        sys.exit("FATAL: could not parse the system table from "
                 f"{MATRIX_MD.relative_to(REPO)} — refusing to emit a stale CSV.")
    return rows


def load_content() -> tuple[Counter, list[str]]:
    """Content-level status rollup, skipping comment lines entirely.

    The CSV carries 18 leading comment lines; they are stripped explicitly
    (not left to the csv reader) and the remaining text is parsed from an
    in-memory buffer, so a line-ending quirk cannot silently truncate the
    parse. Any row with a blank kind/status is reported, never dropped
    silently.
    """
    counts: Counter = Counter()
    junk: list[str] = []
    text = CONTENT_CSV.read_text(encoding="utf-8")
    data_lines = [
        line for line in text.splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    reader = csv.DictReader(io.StringIO("\n".join(data_lines)))
    for r in reader:
        kind = (r.get("kind") or "").strip()
        status = (r.get("implementation_status") or "").strip()
        if not kind or not status:
            junk.append(str(r)[:120])
            continue
        counts[(kind, status)] += 1
    return counts, junk


def main() -> int:
    rows = parse_matrix()
    counts, junk = load_content()

    kinds = sorted({k for (k, _) in counts})
    out: list[dict[str, str]] = []
    by_kind_totals: dict[str, Counter] = defaultdict(Counter)
    for (kind, status), n in counts.items():
        by_kind_totals[kind][status] += n

    for r in rows:
        system = r.get("system", "")
        matched = []
        for key, klist in SYSTEM_TO_KIND.items():
            if system.startswith(key):
                matched = klist
                break
        content_total = sum(sum(by_kind_totals[k].values()) for k in matched)
        content_done = sum(by_kind_totals[k].get("done", 0) for k in matched)
        out.append({
            "system": system,
            "discovered": r.get("discovered (from inventory)", r.get("discovered", "")),
            "status": r.get("status", ""),
            "evidence": r.get("evidence (card → commit)", r.get("evidence", "")),
            "test_status": r.get("test status", ""),
            "deviation": r.get("deviation", ""),
            "next_action": r.get("next action", ""),
            "content_kinds": ";".join(matched),
            "content_entries": str(content_total) if matched else "",
            "content_done": str(content_done) if matched else "",
        })

    with OUT_CSV.open("w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=list(out[0].keys()))
        w.writeheader()
        w.writerows(out)

    print(f"wrote {OUT_CSV.relative_to(REPO)}: {len(out)} system rows")
    print(f"content kinds in CSV: {len(kinds)}")
    by_status: Counter = Counter()
    for (_kind, status), n in counts.items():
        by_status[status] += n
    print(f"content rows parsed: {sum(by_status.values())}")
    print("content status totals:", dict(by_status.most_common()))
    print("curated status totals:", dict(Counter(r["status"] for r in out).most_common()))
    if junk:
        print(f"WARNING: {len(junk)} unparsable content rows (kind/status blank):")
        for j in junk[:5]:
            print("   ", j)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
