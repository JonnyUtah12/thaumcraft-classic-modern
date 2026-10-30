#!/usr/bin/env python3
"""Generate the wand/sceptre assembly recipe rows (TC-M6-01-s-b).

The original never put these two recipes in a data file: `ArcaneWandRecipe` and
`ArcaneSceptreRecipe` were dynamic `IArcaneRecipe` objects registered directly
on `ThaumcraftApi.getCraftingRecipes()` (which is why docs/content-mapping.csv
has no recipe rows for them). The port enumerates the same combination space as
`thaumcraft:arcane_workbench` data rows, one per legal (cap, rod) pair:

    caps x rods = 6 x 18 = 108   (staff cores are rods: WandRod.rods includes them)
    wand rows   = 108 - 1        (ArcaneWandRecipe refuses rod "wood" + cap "iron")
    sceptre rows= 108            (the sceptre recipe has no such refusal)

Every value is read from the port's own tables, so the generator cannot drift:
the cap/rod tuples are parsed out of
`src/main/java/.../wands/WandComponents.java` (the transcription of
`ConfigItems.initializeWandComponents`), and the primal aspect tags out of
`aspect/AspectDefinitions.java` (definitions with no parents).

Layouts, from the originals:
    ArcaneWandRecipe:    caps (0,2)+(2,0), rod (1,1), the other six cells empty
    ArcaneSceptreRecipe: caps (1,0)+(2,1)+(0,2), rod (1,1), primal charm (2,0),
                         cells (0,0)/(0,1)/(1,2)/(2,2) empty

Cost model, from the originals: the vis price is every primal aspect at
`capCraftCost * rodCraftCost` (sceptre: `(int)(capCraftCost * rodCraftCost * 1.5F)`),
and the result's damage value is that same number (carried in
`thaumcraft:item_meta`, D-001).

Usage: python3 scripts/build_wand_recipes.py [--check]
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/jonnyutah12/thaumcraftclassic"
DATA = ROOT / "src/main/resources/data/thaumcraft/recipe"

ITEM_CONSTANTS = {
    "WAND_CAP_ITEM": "thaumcraft:wandcap",
    "WAND_ROD_ITEM": "thaumcraft:wandrod",
    "STICK_ITEM": "minecraft:stick",
}

CAP_RE = re.compile(
    r'^\s*cap\("(?P<tag>\w+)",\s*(?P<base>[\d.]+)F,\s*List\.of\((?P<special>[^)]*)\),'
    r'\s*(?P<mod>[\d.]+)F,\s*(?P<cost>\d+),\s*(?P<meta>\d+)\);')
ROD_RE = re.compile(
    r'^\s*rod\("(?P<tag>\w+)",\s*(?P<capacity>\d+),\s*(?P<cost>\d+),\s*(?P<item>\w+),'
    r'\s*(?P<meta>\d+),\s*(?P<staff>true|false),')
DEFINITION_RE = re.compile(
    r'new Definition\("(?P<tag>\w+)",\s*\d+,\s*(?:null|"[^"]*"),\s*\d+,\s*List\.of\((?P<parents>[^)]*)\)\)')


def caps():
    out = []
    for line in (JAVA / "wands/WandComponents.java").read_text().splitlines():
        m = CAP_RE.match(line)
        if m:
            out.append({"tag": m["tag"], "cost": int(m["cost"]), "meta": int(m["meta"]),
                        "item": "thaumcraft:wandcap"})
    return out


def rods():
    out = []
    for line in (JAVA / "wands/WandComponents.java").read_text().splitlines():
        m = ROD_RE.match(line)
        if m:
            item = ITEM_CONSTANTS.get(m["item"])
            if item is None:
                raise SystemExit("unknown rod item constant " + m["item"])
            out.append({"tag": m["tag"], "cost": int(m["cost"]), "meta": int(m["meta"]), "item": item})
    return out


def primal_aspects():
    tags = []
    for m in DEFINITION_RE.finditer((JAVA / "aspect/AspectDefinitions.java").read_text()):
        if not m["parents"].strip():
            tags.append(m["tag"])
    return tags


def ingredient(item, meta):
    # The ported form of ArcaneWandRecipe.checkItemEquals (item + original damage
    # value); meta 0 needs no thaumcraft:item_meta component, matching the
    # original's damage-0 stacks.
    return {"type": "thaumcraft:wand_component", "item": item, "meta": meta}


def row(cap, rod, sceptre, primals):
    cost = cap["cost"] * rod["cost"]
    if sceptre:
        cost = int(cost * 1.5)
    components = {
        "thaumcraft:wand_cap": cap["tag"],
        "thaumcraft:wand_rod": rod["tag"],
    }
    if sceptre:
        components["thaumcraft:wand_sceptre"] = True
    components["thaumcraft:item_meta"] = cost

    research = [cap_research(cap), rod_research(rod)]
    if sceptre:
        research.append("SCEPTRE")

    guards = [cap["item"], rod["item"], "thaumcraft:wandcasting"]
    key = {"C": ingredient(cap["item"], cap["meta"]), "R": ingredient(rod["item"], rod["meta"])}
    if sceptre:
        key["F"] = ingredient("thaumcraft:itemresource", 15)
        guards.append("thaumcraft:itemresource")

    body = {
        "type": "thaumcraft:arcane_workbench",
        "research_all": research,
        "aspects": [{"aspect": tag, "amount": cost} for tag in primals],
        "pattern": SCEPTRE_PATTERN if sceptre else WAND_PATTERN,
        "key": key,
        "result": {"id": "thaumcraft:wandcasting", "count": 1, "components": components},
        "neoforge:conditions": [
            {"type": "neoforge:item_exists", "item": item} for item in dict.fromkeys(guards)
        ],
    }
    name = ("sceptre." if sceptre else "wand.") + cap["tag"] + "_" + rod["tag"] + ".arcane_shaped.json"
    return name, body


def cap_research(cap):
    return "CAP_" + cap["tag"]


def rod_research(rod):
    return "ROD_" + rod["tag"]


# Row 0 = y, column = x, grid slot = x + y*3.
# ArcaneWandRecipe: cap1 (0,2), rod (1,1), cap2 (2,0).
WAND_PATTERN = ["  C", " R ", "C  "]
# ArcaneSceptreRecipe: cap1 (0,2), cap2 (1,0), rod (1,1), cap3 (2,1), charm (2,0).
SCEPTRE_PATTERN = ["  C", "CR ", "FC "]


def main():
    check = "--check" in sys.argv
    primals = primal_aspects()
    if len(primals) != 6:
        raise SystemExit("expected the 6 primal aspects, read " + str(primals))
    caps_, rods_ = caps(), rods()
    if (len(caps_), len(rods_)) != (6, 18):
        raise SystemExit("expected 6 caps x 18 rods, read %d x %d" % (len(caps_), len(rods_)))

    rows = []
    for cap in caps_:
        for rod in rods_:
            wand_refused = rod["tag"] == "wood" and cap["tag"] == "iron"
            if not wand_refused:
                rows.append(row(cap, rod, False, primals))
            rows.append(row(cap, rod, True, primals))

    expected = sorted(name for name, _ in rows)
    if check:
        on_disk = sorted(p.name for p in DATA.glob("wand.*.arcane_shaped.json"))
        on_disk += sorted(p.name for p in DATA.glob("sceptre.*.arcane_shaped.json"))
        missing = [n for n in expected if n not in on_disk]
        stale = [n for n in on_disk if n not in expected]
        mismatched = []
        for name, body in rows:
            path = DATA / name
            if path.exists() and json.loads(path.read_text()) != body:
                mismatched.append(name)
        if missing or stale or mismatched:
            raise SystemExit("wand recipe data is out of date: %d missing, %d stale, %d changed"
                             % (len(missing), len(stale), len(mismatched)))
        print("wand recipe data is current: %d rows (%d wand, %d sceptre)"
              % (len(rows), sum(1 for n, _ in rows if n.startswith("wand.")),
                 sum(1 for n, _ in rows if n.startswith("sceptre."))))
        return

    for name, body in rows:
        (DATA / name).write_text(json.dumps(body, indent=2) + "\n")
    print("wrote %d rows (%d wand, %d sceptre) to %s"
          % (len(rows), sum(1 for n, _ in rows if n.startswith("wand.")),
             sum(1 for n, _ in rows if n.startswith("sceptre.")), DATA))


if __name__ == "__main__":
    main()
