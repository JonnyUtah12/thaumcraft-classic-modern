#!/usr/bin/env python3
"""Generate BD-17 per-meta item models from the Java appearance table.

The single source of truth is
``src/main/java/.../items/ItemMetaAppearance.java`` — the table that
transcribes, per family, which sprite 4.2.3.5's item renderer showed for each
metadata (``Item.getIconFromDamage`` for item classes, ``Block.getIcon(0,
meta)`` for block items — ``RenderBlocks.renderBlockAsItem`` painted the side-0
icon on every face), and the box geometry of the two shape renderers.

This script derives the data files from that table:

* ``models/itemmeta/<id>_<meta>.json`` — one model per metadata. Flat families
  get the ``minecraft:item/generated`` layers; the shape replacements get the
  box elements of the model classes the original renderers instantiated
  (``ModelTrunk`` cube 0..16, ``ModelArcaneWorkbench`` box 1.5..14.5).
* ``models/item/<id>.json`` — gains an ``overrides`` array keyed on
  ``minecraft:custom_model_data`` (the int component 1.21.1's
  ``ItemProperties`` can read; an unregistered predicate name is dropped at
  load). The base sprite stays the SPINE-3 chain.
* The two custom-shape ids ship their shape model at ``models/item/<id>.json``.

Rules it enforces while it works (the in-CI twin of the game-test probes):

* every sprite it names is a **published** row of ``docs/asset-mapping.csv``
  (D-004: the original PNGs are overlay-only; D-005 layout ``items`` ->
  ``item``, ``blocks`` -> ``block``). No PNG is written.
* ids the table lists as measured per-meta exclusions get NO overrides.
* override values ascend (the int match picks the closest value at or below
  the key, so an unsorted array would pick the wrong meta).

Run with ``--check`` (the build gate calls it): fails if a committed model
file drifted from the table, writes nothing.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/thaumcraft"
MODELS_ITEM = ASSETS / "models/item"
MODELS_METAS = ASSETS / "models/itemmeta"
TABLE = ROOT / "src/main/java/com/jonnyutah12/thaumcraftclassic/items/ItemMetaAppearance.java"
CSV = ROOT / "docs/asset-mapping.csv"
LANG = ASSETS / "lang/en_us.json"
NS = "thaumcraft"
PREDICATE = "custom_model_data"
GENERATED = "minecraft:item/generated"


def published_sprites() -> set[str]:
    """Sprites the overlay publishes: the ``new_asset`` column (col 1) of
    docs/asset-mapping.csv, in the D-005 modern layout (``items`` -> ``item``,
    ``blocks`` -> ``block``). Col 0 is the 1.7.10 legacy source path."""
    out: set[str] = set()
    for line in CSV.read_text(encoding="utf-8").splitlines():
        cells = line.split(",")
        cell = cells[1].strip() if len(cells) > 1 else ""
        if cell.startswith("textures/") and cell.endswith(".png"):
            rest = cell[len("textures/"):-len(".png")]
            out.add(f"{NS}:{rest}")
    return out


def read_table() -> tuple[dict[str, list[dict]], dict[str, dict], set[str]]:
    """Parse FAMILIES, CUSTOM_SHAPES and the exclusion ids out of the Java table."""
    text = TABLE.read_text(encoding="utf-8")
    families: dict[str, list[dict]] = {}
    customs: dict[str, dict] = {}
    exclusions: set[str] = set()

    def strip_comment(tail: str) -> str:
        return tail.split("//", 1)[0]

    for m in re.finditer(r'FAMILIES\.put\("([a-z0-9_]+)",\s*List\.of\((.*?)\)\);', text, re.S):
        fam, body = m.group(1), strip_comment(m.group(2))
        metas: list[dict] = []
        for fm in re.finditer(r'two\("([^"]+)",\s*"([^"]+)",\s*(-?\d+)\)', body):
            metas.append({"meta": int(fm.group(3)), "shape": -1,
                          "sprites": [f"{NS}:item/{fm.group(1)}", f"{NS}:item/{fm.group(2)}"]})
        for fm in re.finditer(r'(flat|face)\("([^"]+)",\s*(-?\d+)\)', body):
            sprite = f"{NS}:item/{fm.group(2)}" if fm.group(1) == "flat" else f"{NS}:{fm.group(2)}"
            metas.append({"meta": int(fm.group(3)), "shape": -1, "sprites": [sprite]})
        metas.sort(key=lambda a: a["meta"])
        families[fam] = metas

    for m in re.finditer(
            r'CUSTOM_SHAPES\.put\("([a-z0-9_]+)",\s*new MetaArt\((\d+),\s*(SHAPE_[A-Z]+),\s*List\.of\(([^)]*)\)\)\);',
            text, re.S):
        sprites = [s.strip().strip('"') for s in m.group(4).split(",") if s.strip()]
        customs[m.group(1)] = {"meta": int(m.group(2)), "shape": m.group(3), "sprites": sprites}

    for m in re.finditer(r'NO_PER_META_APPEARANCE\.put\("([a-z0-9_]+)"', text):
        exclusions.add(m.group(1))

    assert families, "no FAMILIES rows parsed from the Java table"
    assert customs, "no CUSTOM_SHAPES rows parsed from the Java table"
    return families, customs, exclusions


def box_element(sprite: str, shape: str) -> dict:
    """The box geometry of the model class the original renderer drew."""
    lo, hi = (0.0, 16.0) if shape == "SHAPE_CUBE" else (1.5, 14.5)
    uv = [0.0, 0.0, 16.0, 16.0]
    faces = {side: {"uv": uv, "texture": f"#{side}"}
             for side in ("north", "south", "east", "west", "up", "down")}
    return {"from": [lo, lo, lo], "to": [hi, hi, hi], "faces": faces}


def model_for(art: dict, published: set[str]) -> dict:
    if art["shape"] == -1:
        layers = {"layer0": art["sprites"][0]}
        if len(art["sprites"]) > 1:
            layers["layer1"] = art["sprites"][1]
        for sprite in art["sprites"]:
            if sprite not in published:
                raise SystemExit(f"unpublished sprite {sprite} named by the table")
        return {"parent": GENERATED, "textures": layers}
    texture = art["sprites"][0]
    if texture not in published:
        raise SystemExit(f"unpublished sprite {texture} named by the table")
    return {"parent": GENERATED, "gui_light": "front",
            "textures": {"particle": texture, "north": texture, "south": texture,
                         "east": texture, "west": texture, "up": texture, "down": texture},
            "elements": [box_element(texture, art["shape"])]}


def main(check: bool) -> int:
    published = published_sprites()
    families, customs, exclusions = read_table()

    lang = json.loads(LANG.read_text(encoding="utf-8"))
    keyed = set()
    for key in lang:
        for prefix in (f"item.{NS}.", f"block.{NS}."):
            if key.startswith(prefix):
                tail = key[len(prefix):]
                parts = tail.rsplit(".", 1)
                if len(parts) == 2 and parts[1].isdigit() and lang[key]:
                    keyed.add(parts[0])

    wanted: dict[Path, dict] = {}
    for fam, metas in families.items():
        for art in metas:
            model = model_for(art, published)
            base = MODELS_ITEM / f"{fam}.json"
            if base.exists():
                base_json = json.loads(base.read_text(encoding="utf-8"))
            else:
                raise SystemExit(f"{fam}: no base item model to carry the overrides")
            # Keep any overrides the base model already carries under other
            # predicates; the custom_model_data set is derived wholly from the
            # table (so a dropped meta's entry disappears with it).
            overrides = [o for o in base_json.get("overrides", [])
                         if o.get("predicate") != PREDICATE]
            overrides += [{"predicate": PREDICATE, "value": art["meta"],
                           "model": f"{NS}:itemmeta/{fam}_{art['meta']}"}
                          for art in metas]
            values = [o["value"] for o in overrides if o.get("predicate") == PREDICATE]
            if sorted(values) != values:
                raise SystemExit(f"{fam}: override values must ascend, {values}")
            overrides = sorted(overrides, key=lambda o: (o.get("predicate", ""), o.get("value", 0)))
            base_json["overrides"] = overrides
            wanted[base] = base_json
            wanted[MODELS_METAS / f"{fam}_{art['meta']}.json"] = model
        if fam in exclusions:
            raise SystemExit(f"{fam}: in the family table and the exclusion table at once")
    for fam, art in customs.items():
        wanted[MODELS_ITEM / f"{fam}.json"] = model_for(art, published)

    for fam in keyed:
        if not (MODELS_ITEM / f"{fam}.json").exists():
            continue  # not shipped registered content (SPINE-3: every shipped item has a model)
        if fam not in families and fam not in exclusions:
            raise SystemExit(f"{fam}: lang names per-meta labels, the table has no row and no exclusion "
                             f"reason (add the art with the measured sprite, or the cited exclusion)")
        if (fam in families) == (fam in exclusions):
            raise SystemExit(f"{fam}: in the family table and the exclusion table at once")

    drift = {
        path: json.dumps(model, indent=2, ensure_ascii=False) + "\n"
        for path, model in wanted.items()
        if not path.exists()
        or path.read_text(encoding="utf-8").strip()
        != json.dumps(model, indent=2, ensure_ascii=False).strip()
    }
    for path in MODELS_METAS.glob("*.json") if MODELS_METAS.exists() else []:
        stem = path.name[: -len(".json")]
        fam, _, meta = stem.rpartition("_")
        if fam not in families or not any(m["meta"] == int(meta) for m in families[fam]):
            drift[path] = "<table has no row for it>"
    for fam in exclusions:
        base = MODELS_ITEM / f"{fam}.json"
        if base.exists():
            data = json.loads(base.read_text(encoding="utf-8"))
            if data.get("overrides"):
                drift[base] = "<the original draws this identically per meta — no overrides allowed>"

    if drift:
        if check:
            print(f"{len(drift)} model file(s) drifted from ItemMetaAppearance.java:", file=sys.stderr)
            for path, why in sorted(drift.items(), key=lambda kv: str(kv[0])):
                print(f"  {path.relative_to(ROOT)}: {why.splitlines()[0] if why.strip() else 'stale'}",
                      file=sys.stderr)
            return 1
        for path, content in drift.items():
            if content == "<table has no row for it>":
                path.unlink()
                continue
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
    print(f"{len(wanted)} models in sync, {len(drift)} written "
          f"({len(families)} per-meta families, {len(customs)} custom-renderer replacements, "
          f"{len(exclusions)} measured exclusions)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main("--check" in sys.argv[1:]))
