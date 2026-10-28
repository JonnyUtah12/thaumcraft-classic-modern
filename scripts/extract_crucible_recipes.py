#!/usr/bin/env python3
"""Extract every original crucible recipe into recipe JSON data.

TC-M4-05. Reads the decompiled original (kept outside the repository at
../reference/decompiled, see docs/licensing-and-attribution.md) — the
``initializeAlchemyRecipes`` method of ``ConfigRecipes.java`` — and writes one
vanilla-loadable recipe JSON per recipe instance under

  src/main/resources/data/thaumcraft/recipe/

with ``"type": "thaumcraft:crucible"`` (custom recipe type, see
recipes/CrucibleRecipe.java). File names are the TC-M0-04 inventory ids from
docs/content-mapping.csv (``recipe.NNN.crucible``); the BalancedShard loop
family expands to ``<row-id>.<index>.json`` instances (49 rows, 54 instances).

The item-id mapping is the content-mapping.csv contract (original_id ->
new_id); original damage metadata becomes the port's variant components
(thaumcraft:item_meta for item families, minecraft:block_state variant for
block items). OreDict catalyst names become explicit member ingredients:
meta-bearing families (nuggetX) route through thaumcraft:item_meta
components so Trans* recipes cannot cross-match; ore/dust names become
thaumcraft:oredict.* item tags (created here when missing).

Faithfulness notes:
- The original gates Pure/Trans Copper/Tin/Silver/Lead on
  Config.foundXIngot (another mod registered ingotX). Vanilla 1.21 ships
  copper, so orect.orecopper gets vanilla members; tin/silver/lead tags are
  created empty — the recipe loads but never matches until a mod or datapack
  adds members, which is the same observable behaviour as foundXIngot=false.
- The Alumentum catalyst is any coal (OreDictionary.WILDCARD_VALUE on the
  1.7 coal item); 1.21 splits it into coal + charcoal, both listed.
- The BottleTaint catalyst is the ``bt`` local: an essentia jar (itemEssence
  meta 1) with TAINT 8 set via IEssentiaContainerItem. Catalyst matching in
  the original compares item+meta only (ItemUtils.itemMatches), so the jar's
  essentia contents are not part of the ingredient.

Every recipe carries a ``neoforge:conditions`` gate on every item it names
(both namespaces) and every tag it names, so a row whose content is not
ported is skipped by NeoForge before its body is decoded, and the data
loads clean before the owning catalog cards land (D-015). A
component-carrying catalyst is emitted in NeoForge’s registered
ingredient form ``{"type": "neoforge:components", "items": [...],
"components": {...}}`` — the only form ``Ingredient.CODEC_NONEMPTY``
resolves; see ``components_ingredient``.

Method notes: docs/feature-tracking.md TC-M4-05 coverage table. Run:
  python3 scripts/extract_crucible_recipes.py
"""

import csv
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO / "scripts"))

from extract_research_graph import (  # noqa: E402
    match_brace, match_paren, parse_string_literal, split_args,
    strip_comments, SRC_RECIPES,
)
from extract_arcane_recipes import (  # noqa: E402
    CONTENT_CSV, OUT_DIR, TAG_DIR, ITEM_META_FAMILIES, RecipeExtractor,
    problems,
)

RECIPE_TYPE = "thaumcraft:crucible"

SOURCE_NOTE = (
    "decompiled thaumcraft/common/config/ConfigRecipes.java "
    "initializeAlchemyRecipes (Thaumcraft 4.2.3.5); extracted mechanically by "
    "scripts/extract_crucible_recipes.py")

def components_ingredient(items, components=None):
    """The Ingredient form that actually loads (docs/porting-decisions.md D-015).

    A component-carrying ingredient must be NeoForge's *registered* ingredient:
    ``{"type": "neoforge:components", "items": [...], "components": {...}}``,
    which ``Ingredient.MAP_CODEC_NONEMPTY`` (the polymorphic map codec
    ``Ingredient.CODEC_NONEMPTY`` dispatches through) resolves. The bare
    ``{"neoforge:components": {...}}`` object the first extractor pass emitted
    has no ``type`` key, so it falls into vanilla's Ingredient codec, finds no
    ``item``/``tag``/``items`` entry, and every row using it fails the data
    pack. ``items`` is a list, never a bare string, for the same reason.
    """
    if isinstance(items, str):
        items = [items]
    return {"type": "neoforge:components",
            "items": list(items),
            "components": dict(components or {})}


# OreDict catalyst names used by the originals -> ingredient objects.
# nuggetX families carry the thaumcraft:item_meta component so each Trans*
# recipe matches only its own metal (the original oredict split by meta).
# ore/dust names become thaumcraft:oredict.* tags (members below).
CRUCIBLE_OREDICT_INGREDIENTS = {
    "dustGlowstone": {"item": "minecraft:glowstone_dust"},
    "oreIron": {"tag": "thaumcraft:oredict.oreiron"},
    "oreGold": {"tag": "thaumcraft:oredict.oregold"},
    "oreCopper": {"tag": "thaumcraft:oredict.orecopper"},
    "oreTin": {"tag": "thaumcraft:oredict.oretin"},
    "oreSilver": {"tag": "thaumcraft:oredict.oresilver"},
    "oreLead": {"tag": "thaumcraft:oredict.orelead"},
    "nuggetIron": components_ingredient(
        "thaumcraft:itemnugget", {"thaumcraft:item_meta": 0}),
    "nuggetCopper": components_ingredient(
        "thaumcraft:itemnugget", {"thaumcraft:item_meta": 1}),
    "nuggetTin": components_ingredient(
        "thaumcraft:itemnugget", {"thaumcraft:item_meta": 2}),
    "nuggetSilver": components_ingredient(
        "thaumcraft:itemnugget", {"thaumcraft:item_meta": 3}),
    "nuggetLead": components_ingredient(
        "thaumcraft:itemnugget", {"thaumcraft:item_meta": 4}),
}


def collect_tags(node):
    """Every item-tag name an ingredient object references (oredict catalysts)."""
    found = set()
    stack = [node]
    while stack:
        current = stack.pop()
        if isinstance(current, dict):
            for key, value in current.items():
                if key == "tag" and isinstance(value, str):
                    found.add(value)
                else:
                    stack.append(value)
        elif isinstance(current, list):
            stack.extend(current)
    return found


def conditions_for(items, tags):
    """The D-015 data gate: every item and tag the row names.

    ``neoforge:conditions`` is evaluated before the recipe body is decoded, so
    a row whose items are not ported is skipped, not parsed — that is the only
    thing keeping a not-yet-ported ``thaumcraft:itemgolemplacer`` catalyst, or a
    1.7 id that no longer exists (``minecraft:snow``, ``minecraft:brick_block``),
    out of the RecipeManager parse-error count. Vanilla ids are gated too, for
    exactly that reason; conditions that are satisfied cost the row nothing.
    A tag catalyst gets the sibling infusion rows' ``not(tag_empty)`` gate so an
    empty ``thaumcraft:oredict.*`` tag (tin/silver/lead: the original's
    ``Config.foundXIngot``) skips the row the same way the original skipped
    registering it.
    """
    conditions = [{"type": "neoforge:item_exists", "item": item}
                  for item in sorted(items)]
    for tag in sorted(tags):
        conditions.append({"type": "neoforge:not", "value": {
            "type": "neoforge:tag_empty", "tag": tag}})
    return conditions


# Tag members for the ore/dust catalysts (vanilla members only; other mods
# contribute through the same tag file). tin/silver/lead stay empty: the
# original only registered those recipes when another mod provided the metal
# (Config.foundXIngot), and an empty tag reproduces that behaviour.
CRUCIBLE_OREDICT_MEMBERS = {
    "dustglowstone": ["minecraft:glowstone_dust"],
    "oreiron": ["minecraft:iron_ore", "minecraft:deepslate_iron_ore"],
    "oregold": ["minecraft:gold_ore", "minecraft:deepslate_gold_ore"],
    "orecopper": ["minecraft:copper_ore", "minecraft:deepslate_copper_ore"],
    "oretin": [],
    "oresilver": [],
    "orelead": [],
}

PRIMAL_ORDER = ("AIR", "FIRE", "WATER", "EARTH", "ORDER", "ENTROPY")


class CrucibleExtractor(RecipeExtractor):

    # ---------------------------------------------------------------- aspects
    def aspect_chain(self, expr):
        """AspectList chain accepting both .add() and .merge() (the crucible
        tables use both; they are equivalent when building a required list)."""
        tags = []
        base = expr.strip()
        if not base.startswith("new AspectList()"):
            problems.append(f"expected AspectList expression: {expr!r}")
            return tags
        i = len("new AspectList()")
        while i < len(base):
            m = re.compile(r"\s*\.(?:add|merge)\(").match(base, i)
            if not m:
                break
            open_paren = m.end() - 1
            close = match_paren(base, open_paren)
            args = split_args(base[open_paren + 1:close])
            if len(args) != 2:
                problems.append(f"bad chain arity in AspectList: {args!r}")
                i = close + 1
                continue
            ref = args[0].strip()
            tag = self.resolver.aspect_tags.get(ref.split(".", 1)[1])
            if tag is None:
                problems.append(f"unknown Aspect constant: {ref}")
            amount = self.resolve_int(args[1].strip())
            if tag is not None and amount is not None:
                tags.append((tag, amount))
            i = close + 1
        rest = base[i:].strip()
        if rest:
            problems.append(f"unparsed remainder in AspectList: {rest!r}")
        return tags

    # ------------------------------------------------------------- catalysts
    def tc_catalyst(self, expr):
        """Catalyst ItemStack -> ingredient. Unlike the base ingredient(), a
        thaumcraft meta-family item always carries an explicit
        thaumcraft:item_meta component (including meta 0): an Ingredient
        without components would match every meta of the family, while the
        original itemMatches compares the exact damage value."""
        ref = self.resolver.itemstack(expr)
        mapped = self.map_item(ref)
        if mapped is None:
            return None
        item_id, components = mapped
        if components:
            return components_ingredient(item_id, components)
        if item_id.startswith("thaumcraft:"):
            entry = self.contract.get(ref["item"])
            if entry and entry[2] in ITEM_META_FAMILIES:
                return components_ingredient(
                    item_id, {"thaumcraft:item_meta": 0})
            # explicit meta 0 on a multi-variant TC block (GolemWood's
            # greatwood catalyst): the port routes block metas through the
            # block_state variant property, so pin variant 0 explicitly.
            m = re.match(r"new ItemStack\([^,]+,\s*\d+\s*,\s*0\s*\)$", expr)
            if m and entry and entry[1] == "block":
                return components_ingredient(
                    item_id, {"minecraft:block_state": {"variant": "0"}})
        return {"item": item_id}

    def catalyst(self, expr):
        expr = expr.strip()
        if expr.startswith('"') and expr.endswith('"'):
            name = expr[1:-1]
            ing = CRUCIBLE_OREDICT_INGREDIENTS.get(name)
            if ing is None:
                problems.append(f"no catalyst table for oredict: {name}")
                return None
            return json.loads(json.dumps(ing))  # deep copy
        if expr == "bt":
            # BottleTaint catalyst: the local jar stack (itemEssence meta 1).
            return self.tc_catalyst("new ItemStack(ConfigItems.itemEssence, 1, 1)")
        if "32767" in expr:
            # OreDictionary.WILDCARD_VALUE: the Alumentum catalyst is any
            # coal (Items.field_151044_h = coal; meta 0 coal / 1 charcoal in
            # 1.7, split into two items in 1.21).
            ref = self.resolver.itemstack(expr)
            name = (ref.get("item") or "").split(":")[-1]
            if name == "coal":
                return components_ingredient(
                    ["minecraft:coal", "minecraft:charcoal"])
            problems.append(f"unhandled wildcard catalyst: {expr!r}")
            return None
        return self.tc_catalyst(expr)

    # ---------------------------------------------------------------- recipes
    def crucible_recipe(self, key_expr, result_expr, catalyst_expr,
                        aspects_expr, aspects_override=None):
        result_ref = self.resolver.itemstack(result_expr)
        result_mapped = self.map_item(result_ref)
        if result_mapped is None:
            return None
        result_id, result_components = result_mapped
        count = 1
        m = re.match(r"new ItemStack\(.*?,\s*(\d+)\s*(?:,.*)?\)$",
                     result_expr.strip(), re.S)
        if m:
            count = int(m.group(1))
        if aspects_override is not None:
            aspects = aspects_override
        else:
            aspects = self.aspect_chain(aspects_expr)
        catalyst_ing = self.catalyst(catalyst_expr)
        if catalyst_ing is None or not aspects:
            return None

        referenced = {result_id}
        for item_id in self._ingredient_items(catalyst_ing):
            referenced.add(item_id)
        tags = collect_tags(catalyst_ing)

        result = {"id": result_id, "count": count}
        if result_components:
            result["components"] = result_components
        out = {
            "type": RECIPE_TYPE,
            "original_key": parse_string_literal(key_expr),
            "research": parse_string_literal(key_expr),
            "catalyst": catalyst_ing,
            "aspects": [{"aspect": t, "amount": a} for t, a in aspects],
            "result": result,
        }
        conditions = conditions_for(referenced, tags)
        out["neoforge:conditions"] = conditions
        return out

    # ------------------------------------------------------------------ rows
    def inventory_rows(self):
        rows = []
        with CONTENT_CSV.open(newline="", encoding="utf-8") as fh:
            for row in csv.DictReader(fh):
                notes = row.get("notes") or ""
                m = re.search(r"type=crucible\b(?:.*?research=(\w+))?", notes)
                if not m:
                    continue
                rows.append({
                    "id": row["original_id"].split(":", 1)[1],
                    "research": m.group(1),
                    "result": self.norm_result(row["display_name"]),
                })
        return rows

    def pair_calls(self, body, call_sites, rows):
        pairs = []
        used = set()
        for pos in call_sites:
            open_paren = pos + re.match(
                r"ThaumcraftApi\s*\.\s*\w+\s*\(", body[pos:]).end() - 1
            args = split_args(body[open_paren + 1:match_paren(body, open_paren)])
            research = parse_string_literal(args[0])
            result_sig = self.norm_result(args[1])
            cands = [i for i, r in enumerate(rows)
                     if i not in used and r["research"] == research
                     and r["result"] == result_sig]
            if len(cands) != 1:
                problems.append(
                    f"call at {pos} research={research} result={result_sig} "
                    f"matched {len(cands)} inventory rows")
                continue
            used.add(cands[0])
            pairs.append((pos, rows[cands[0]]["id"]))
        return pairs

    # ------------------------------------------------------------------ main
    def run(self):
        src = strip_comments(SRC_RECIPES.read_text(encoding="utf-8",
                                                   errors="replace"))
        start = src.index("private static void initializeAlchemyRecipes")
        end = src.index("private static void initializeArcaneRecipes", start)
        body = src[start:end]

        call_sites = [m.start() for m in
                      re.finditer(r"ThaumcraftApi\s*\.\s*addCrucibleRecipe\s*\(",
                                  body)]
        rows = self.inventory_rows()
        pairs = self.pair_calls(body, call_sites, rows)
        if len(call_sites) != len(rows):
            problems.append(f"call sites {len(call_sites)} != inventory rows "
                            f"{len(rows)}")

        primals = [self.resolver.aspect_tags[c] for c in PRIMAL_ORDER]
        instances = []
        for pos, row_id in pairs:
            open_paren = pos + re.match(
                r"ThaumcraftApi\s*\.\s*\w+\s*\(", body[pos:]).end() - 1
            close = match_paren(body, open_paren)
            args = split_args(body[open_paren + 1:close])
            self.resolver.vars = {}
            self.resolver.int_vars = {}

            # BalancedShard family: the loop builds `al` = every primal except
            # aspect[a], amount 2; the call site sits inside the loop body.
            head = body[:pos]
            loop_start = head.rfind("for (int a = 0; a < 6; a++)")
            in_loop = (loop_start != -1
                       and pos < match_brace(body, body.index("{", loop_start)))
            if in_loop and args[3].strip() == "al":
                for idx in range(6):
                    self.resolver.int_vars["a"] = idx
                    override = [(p, 2) for p in primals if p != primals[idx]]
                    r = self.crucible_recipe(args[0], args[1], args[2], None,
                                             aspects_override=override)
                    if r is None:
                        continue
                    instances.append((f"{row_id}.{idx}", r))
                continue

            r = self.crucible_recipe(*args)
            if r is None:
                continue
            instances.append((row_id, r))

        grouped = {}
        for rid, _ in instances:
            base = re.sub(r"\.\d+$", "", rid)
            grouped.setdefault(base, []).append(rid)
        self.rows = grouped

        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for rid, data in instances:
            path = OUT_DIR / f"{rid}.json"
            path.write_text(json.dumps(data, indent=2) + "\n",
                            encoding="utf-8")

        for name, members in sorted(CRUCIBLE_OREDICT_MEMBERS.items()):
            path = TAG_DIR / f"oredict.{name}.json"
            if path.exists():
                continue
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(
                {"required": True, "values": members}, indent=2) + "\n",
                encoding="utf-8")

        summary = {
            "source": SOURCE_NOTE,
            "inventoryRows": len(self.rows),
            "instances": len(instances),
            "rows": {rid: ids for rid, ids in sorted(
                self.rows.items(),
                key=lambda kv: int(re.search(r"\.(\d+)\.", kv[0]).group(1)))},
            "oredictTags": sorted(CRUCIBLE_OREDICT_MEMBERS),
        }
        (REPO / "docs" / "crucible-recipe-inventory.json").write_text(
            json.dumps(summary, indent=2) + "\n", encoding="utf-8")

        print(f"rows={len(self.rows)} instances={len(instances)}")
        if problems:
            print("PROBLEMS:")
            for p in problems:
                print(" -", p)
            return 1
        return 0


def main():
    return CrucibleExtractor().run()


if __name__ == "__main__":
    sys.exit(main())
