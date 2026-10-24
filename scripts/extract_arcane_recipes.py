#!/usr/bin/env python3
"""Extract every original arcane workbench recipe into recipe JSON data.

TC-M4-03. Reads the decompiled original (kept outside the repository at
../reference/decompiled, see docs/licensing-and-attribution.md) — the
``initializeArcaneRecipes`` method of ``ConfigRecipes.java`` — and writes one
vanilla-loadable recipe JSON per recipe instance under

  src/main/resources/data/thaumcraft/recipe/

with ``"type": "thaumcraft:arcane_workbench"`` (custom recipe type,
ArcaneWorkbenchRecipeSerializer). File names are the TC-M0-04 inventory ids
from docs/content-mapping.csv (``recipe.NNN.arcane_shaped`` /
``recipe.NNN.arcane_shapeless``); the two loop-generated families (banners
x16, primal arrows x6) expand to ``<row-id>.<index>.json`` instances.

The item-id mapping is the content-mapping.csv contract (original_id ->
new_id); original damage metadata becomes the port's variant components
(thaumcraft:item_meta for item families, minecraft:block_state variant for
block items, thaumcraft:banner_color for banner colour). OreDict names
become thaumcraft:oredict.* item tags (created here when missing).

Every recipe carries neoforge:item_exists conditions for every thaumcraft
item it references, so the data loads before the owning catalog cards land.

Method notes: docs/feature-tracking.md TC-M4-03 coverage table. Run:
  python3 scripts/extract_arcane_recipes.py
"""

import csv
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO / "scripts"))

from extract_research_graph import (  # noqa: E402
    Resolver, match_brace, match_paren, parse_string_literal, split_args,
    strip_comments, SRC_RECIPES,
)

CONTENT_CSV = REPO / "docs" / "content-mapping.csv"
OUT_DIR = REPO / "src" / "main" / "resources" / "data" / "thaumcraft" / "recipe"
TAG_DIR = REPO / "src" / "main" / "resources" / "data" / "thaumcraft" / "tags" / "item"

RECIPE_TYPE = "thaumcraft:arcane_workbench"

problems = []

SOURCE_NOTE = ("decompiled thaumcraft/common/config/ConfigRecipes.java "
               "initializeArcaneRecipes (Thaumcraft 4.2.3.5); extracted "
               "mechanically by scripts/extract_arcane_recipes.py")


# ---------------------------------------------------------------------------
# content-mapping.csv contract
# ---------------------------------------------------------------------------

def load_content_contract():
    """original_id -> (new_id, kind, class-name-from-notes)."""
    contract = {}
    with CONTENT_CSV.open(newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row["kind"] not in ("item", "block"):
                continue
            notes = row.get("notes") or ""
            cls = ""
            m = re.search(r"class=(\w+)", notes)
            if m:
                cls = m.group(1)
            contract[row["original_id"]] = (row["new_id"], row["kind"], cls)
    return contract


# Item families whose original damage value rides the thaumcraft:item_meta
# component (docs/architecture/registration.md; ModDataComponents.ITEM_META).
ITEM_META_FAMILIES = {
    "ItemResource", "ItemShard", "ItemNugget", "ItemPrimalArrow",
    "ItemWandCap", "ItemWandRod", "ItemWandCasting", "ItemEldritchObject",
    "ItemLootBag", "ItemNugget",
}

# Vanilla 1.7 blocks/items addressed by damage value in the originals, mapped
# to their modern split items. Only entries actually referenced are used; the
# extractor reports any meta it cannot map instead of guessing.
VANILLA_META_ITEMS = {
    "log": {0: "oak_log", 1: "spruce_log", 2: "birch_log", 3: "jungle_log"},
    "planks": {0: "oak_planks", 1: "spruce_planks", 2: "birch_planks",
               3: "jungle_planks"},
    "log2": {0: "acacia_log", 1: "dark_oak_log"},
    "planks2": {0: "acacia_planks", 1: "dark_oak_planks"},
    # Blocks.wool damage = colour (1.7 order); bone meal is the *item* form
    # (Items.field_151042_Z) and resolves through the SRG field map instead
    "wool": {0: "white_wool", 1: "orange_wool", 2: "magenta_wool",
             3: "light_blue_wool", 4: "yellow_wool", 5: "lime_wool",
             6: "pink_wool", 7: "gray_wool", 8: "light_gray_wool",
             9: "cyan_wool", 10: "purple_wool", 11: "blue_wool",
             12: "brown_wool", 13: "green_wool", 14: "red_wool",
             15: "black_wool"},
    # referenced by the crucible tables (TC-M4-05): cracked stone brick and
    # the 1.7 dye item's ink-sac / bone-meal metas
    "stonebrick": {0: "stone_bricks", 1: "mossy_stone_bricks",
                   2: "cracked_stone_bricks", 3: "chiseled_stone_bricks"},
    "dye": {0: "ink_sac", 15: "bone_meal"},
}

# vanilla items that never exist without a damage value in 1.21 (split by
# colour/variant): always route through the table above
VANILLA_SPLIT = {"log", "planks", "log2", "planks2", "wool", "stonebrick",
                 "dye"}

# OreDict names referenced by the originals -> tag membership. Vanilla
# members only; thaumcraft members are contributed by the owning catalog
# cards through the same tag file (append-only). Members verified against
# the 1.21.1 item registry by the companion unit test.
OREDICT_MEMBERS = {
    "stickwood": ["minecraft:stick"],
    "plankwood": ["minecraft:oak_planks", "minecraft:spruce_planks",
                  "minecraft:birch_planks", "minecraft:jungle_planks",
                  "minecraft:acacia_planks", "minecraft:dark_oak_planks"],
    "logwood": ["minecraft:oak_log", "minecraft:spruce_log",
                "minecraft:birch_log", "minecraft:jungle_log",
                "minecraft:acacia_log", "minecraft:dark_oak_log"],
    "slabwood": ["minecraft:oak_slab", "minecraft:spruce_slab",
                 "minecraft:birch_slab", "minecraft:jungle_slab",
                 "minecraft:acacia_slab", "minecraft:dark_oak_slab"],
    "treesapling": ["minecraft:oak_sapling", "minecraft:spruce_sapling",
                    "minecraft:birch_sapling", "minecraft:jungle_sapling",
                    "minecraft:acacia_sapling", "minecraft:dark_oak_sapling"],
    "dyeblue": ["minecraft:blue_dye"],
    "gememerald": ["minecraft:emerald"],
    "ingotiron": ["minecraft:iron_ingot"],
    "stone": ["minecraft:stone"],
}


# inventory display names that don't normalize to the source field name
RESULT_ALIASES = {
    "key|2|0": "arcanedoorkey|2|0",
    "key|2|1": "arcanedoorkey|2|1",
    "chestrobe|1|0": "chestplaterobe|1|0",
    "legsrobe|1|0": "leggingsrobe|1|0",
}


class RecipeExtractor:

    class _Resolver(Resolver):
        """Also understands OreDictionary.WILDCARD_VALUE (any damage)."""

        def resolve_int(self, expr):
            expr = expr.strip()
            if expr == "OreDictionary.WILDCARD_VALUE":
                return 32767
            return super().resolve_int(expr)

    def __init__(self):
        self.resolver = RecipeExtractor._Resolver()
        self.contract = load_content_contract()
        self.cap_costs = self._load_component_tag_costs()
        self.used_tags = set()
        self.rows = {}          # content-mapping row id -> instances
        self.oredict_seen = set()

    def _load_component_tag_costs(self):
        """WandCap.caps/WandRod.rods craftCost keyed by component tag."""
        from extract_research_graph import SRC_ITEMS
        src = SRC_ITEMS.read_text(encoding="utf-8", errors="replace")
        costs = {}
        for m in re.finditer(
                r"new Wand(?:Cap|Rod)\(\s*\"(\w+)\".*?,\s*(\d+)\s*\)", src, re.S):
            costs[m.group(1)] = int(m.group(2))
        return costs

    # ------------------------------------------------------------------ ints
    def resolve_int(self, expr):
        expr = expr.strip()
        if expr == "OreDictionary.WILDCARD_VALUE":
            return 32767
        m = re.match(r"^Wand(?:Cap|Rod)\.(?:caps|rods)\.get\(\"(\w+)\"\)"
                     r"(?:\s*\*\s*(\d+))?$", expr)
        if m:
            tag, mult = m.group(1), int(m.group(2) or 1)
            if tag in self.cap_costs:
                return self.cap_costs[tag] * mult
            problems.append(f"unknown wand component tag for cost: {tag}")
            return None
        return self.resolver.resolve_int(expr)

    # ----------------------------------------------------------------- aspects
    def aspect_list(self, expr, primal_override=None):
        """Parse an AspectList chain with a paren-balanced .add() scanner;
        pa[a] entries are resolved by the caller (primal-arrow loop)."""
        tags = []
        base = expr.strip()
        if not base.startswith("new AspectList()"):
            problems.append(f"expected AspectList expression: {expr!r}")
            return tags
        i = len("new AspectList()")
        while i < len(base):
            m = re.compile(r"\s*\.add\(").match(base, i)
            if not m:
                break
            open_paren = m.end() - 1
            close = match_paren(base, open_paren)
            args = split_args(base[open_paren + 1:close])
            if len(args) != 2:
                problems.append(f"bad .add() arity in AspectList: {args!r}")
                i = close + 1
                continue
            ref, amount_expr = args[0].strip(), args[1].strip()
            tag = None
            if ref == "pa[a]":
                if primal_override is None:
                    problems.append("pa[a] outside primal-arrow loop")
                else:
                    tag = primal_override
            else:
                tag = self.resolver.aspect_tags.get(ref.split(".", 1)[1])
                if tag is None:
                    problems.append(f"unknown Aspect constant: {ref}")
            amount = self.resolve_int(amount_expr)
            if tag is not None and amount is not None:
                tags.append((tag, amount))
            i = close + 1
        rest = base[i:].strip()
        if rest:
            problems.append(f"unparsed remainder in AspectList: {rest!r}")
        return tags

    # ------------------------------------------------------------- item refs
    def map_item(self, ref):
        """Resolver itemstack dict -> (new_id, component dict or None)."""
        if ref.get("kind") == "unknown":
            return None
        item, meta = ref["item"], ref.get("meta", 0)
        if meta == 32767:
            meta = 0   # OreDictionary.WILDCARD_VALUE: any damage
        if item.startswith("minecraft:"):
            name = item.split(":", 1)[1]
            table = VANILLA_META_ITEMS.get(name)
            if name in VANILLA_SPLIT:
                if meta not in (table or {}):
                    problems.append(f"unmapped vanilla meta: {item} meta {meta}")
                    return None
                return "minecraft:" + (table or {})[meta], None
            if meta:
                problems.append(f"unmapped vanilla meta: {item} meta {meta}")
                return None
            return item, None
        entry = self.contract.get(item)
        if entry is None:
            problems.append(f"no content-mapping row for {item}")
            return None
        new_id, kind, _cls = entry
        components = None
        if meta:
            # port convention (ModDataComponents.ITEM_META / VariantStateItems):
            # multi-meta blocks ride the block_state variant property,
            # multi-meta item families ride thaumcraft:item_meta
            if kind == "block":
                components = {"minecraft:block_state": {"variant": str(meta)}}
            else:
                components = {"thaumcraft:item_meta": meta}
        return new_id, components

    def ingredient(self, expr):
        """One ingredient argument -> JSON ingredient object."""
        expr = expr.strip()
        if expr.startswith('"') and expr.endswith('"') and " " not in expr[1:-1]:
            name = expr[1:-1]
            tag = "thaumcraft:oredict." + name.lower()
            self.oredict_seen.add(name.lower())
            self.used_tags.add(tag)
            return {"tag": tag}
        if expr.startswith("new ItemStack("):
            ref = self.resolver.itemstack(expr)
        elif expr.startswith("Items.") or expr.startswith("Blocks.") \
                or expr.startswith("ConfigItems.") or expr.startswith("ConfigBlocks."):
            # bare Item/Block reference == new ItemStack(x, 1, 0)
            ref = self.resolver.itemstack(f"new ItemStack({expr}, 1, 0)")
        elif expr in self.resolver.vars:
            ref = self.resolver.vars[expr]
        else:
            problems.append(f"unparsed ingredient expression: {expr!r}")
            return None
        mapped = self.map_item(ref)
        if mapped is None:
            return None
        item_id, components = mapped
        if components:
            self.used_tags  # no-op; keeps linters honest
            return {"neoforge:components": {
                "items": item_id, "components": components}}
        return {"item": item_id}

    # ---------------------------------------------------------------- recipes
    def recipe(self, research, result_expr, aspects_expr, rest_args,
               shapeless, primal_override=None):
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
        aspects = self.aspect_list(aspects_expr, primal_override)
        if not aspects and "new AspectList()" not in aspects_expr:
            return None

        out = {
            "type": RECIPE_TYPE,
            "research": parse_string_literal(research),
            "aspects": [{"aspect": t, "amount": a} for t, a in aspects],
        }
        referenced = {result_id}

        if shapeless:
            ingredients = []
            for arg in rest_args:
                ing = self.ingredient(arg)
                if ing is None:
                    return None
                ingredients.append(ing)
            out["ingredients"] = ingredients
        else:
            pattern = []
            keys = {}
            i = 0
            while i < len(rest_args) and rest_args[i].strip().startswith('"') \
                    and len(rest_args[i].strip()) >= 3:
                pattern.append(json.loads(rest_args[i].strip()))
                i += 1
            while i + 1 < len(rest_args):
                key = self.parse_char(rest_args[i].strip())
                ing = self.ingredient(rest_args[i + 1])
                if ing is None or len(key) != 1:
                    return None
                keys[key] = ing
                i += 2
            if not pattern or len(keys) * 2 != len(rest_args) - len(pattern):
                problems.append(f"unparsed shaped arguments: {rest_args!r}")
                return None
            out["pattern"] = pattern
            out["key"] = keys

        for ing in out.get("ingredients", []) + list(out.get("key", {}).values()):
            for item_id in self._ingredient_items(ing):
                referenced.add(item_id)

        result = {"id": result_id, "count": count}
        if result_components:
            result["components"] = result_components
        out["result"] = result

        conditions = [{"type": "neoforge:item_exists", "item": item}
                      for item in sorted(referenced)
                      if item.startswith("thaumcraft:")]
        if conditions:
            out["neoforge:conditions"] = conditions
        return out

    @staticmethod
    def parse_char(expr):
        m = re.match(r"^'(\\u[0-9a-fA-F]{4}|.)'$", expr)
        if not m:
            problems.append(f"unparsed char literal: {expr!r}")
            return None
        v = m.group(1)
        return chr(int(v[2:], 16)) if v.startswith("\\u") else v

    @staticmethod
    def _ingredient_items(ing):
        if "item" in ing:
            yield ing["item"]
        elif "neoforge:components" in ing:
            items = ing["neoforge:components"]["items"]
            yield from ([items] if isinstance(items, str) else items)

    # ------------------------------------------------------------------ main
    def run(self):
        src = strip_comments(SRC_RECIPES.read_text(encoding="utf-8", errors="replace"))
        start = src.index("private static void initializeArcaneRecipes")
        end = src.index("private static void initializeInfusionRecipes", start)
        body = src[start:end]

        call_sites = []
        for name, shapeless in (("addArcaneCraftingRecipe", False),
                                ("addShapelessArcaneCraftingRecipe", True)):
            # the originals break lines between ThaumcraftApi and the method
            for m in re.finditer(r"ThaumcraftApi\s*\.\s*" + name + r"\s*\(",
                                 body):
                call_sites.append((m.start(), name, shapeless))
        call_sites.sort()
        rows = self.inventory_rows()
        pairs = self.pair_calls(body, call_sites, rows)
        if len(call_sites) != len(rows):
            problems.append(f"call sites {len(call_sites)} != inventory rows "
                            f"{len(rows)}")

        instances = []
        for pos, _name, shapeless, row_id in pairs:
            open_paren = pos + re.match(
                r"ThaumcraftApi\s*\.\s*\w+\s*\(", body[pos:]).end() - 1
            close = match_paren(body, open_paren)
            args = split_args(body[open_paren + 1:close])
            loop = self.loop_context(body, pos)
            self.resolver.vars = {}
            self.resolver.int_vars = {}
            if loop:
                banner_result = args[1].strip() == "banner"
                for idx, (ivar, primal) in enumerate(loop):
                    self.resolver.int_vars["a"] = ivar
                    if banner_result:
                        self.resolver.vars["banner"] = {
                            "item": "Thaumcraft:blockWoodenDevice", "meta": 8,
                            "banner_color": ivar}
                    r = self.recipe(args[0], args[1], args[2], args[3:],
                                    shapeless=shapeless,
                                    primal_override=primal[0] if primal else None)
                    if r is None:
                        continue
                    if "banner_color" in (self.resolver.vars.get("banner") or {}):
                        r["result"].setdefault("components", {})[
                            "thaumcraft:banner_color"] = ivar
                    instances.append((f"{row_id}.{idx}", r))
            else:
                r = self.recipe(args[0], args[1], args[2], args[3:],
                                shapeless=shapeless)
                if r is None:
                    continue
                instances.append((row_id, r))

        # regroup instances by row for the coverage summary
        rows = {}
        for rid, _ in instances:
            base = re.sub(r"\.\d+$", "", rid)
            rows.setdefault(base, []).append(rid)
        self.rows = rows

        # write recipe files
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for rid, data in instances:
            path = OUT_DIR / f"{rid}.json"
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

        # write missing oredict tags
        for tag in sorted(self.used_tags):
            name = tag.split(".", 1)[1]
            # existing convention: flat file name oredict.NAME.json
            path = TAG_DIR / f"oredict.{name}.json"
            if path.exists():
                continue
            members = OREDICT_MEMBERS.get(name)
            if members is None:
                problems.append(f"no member table for oredict tag: {name}")
                continue
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(
                {"required": True, "values": members}, indent=2) + "\n",
                encoding="utf-8")

        # coverage summary for docs
        summary = {
            "source": SOURCE_NOTE,
            "inventoryRows": len(self.rows),
            "instances": len(instances),
            "rows": {rid: ids for rid, ids in sorted(
                self.rows.items(), key=lambda kv: int(re.search(r"\.(\d+)\.", kv[0]).group(1)))},
            "oredictTags": sorted(self.used_tags),
        }
        (REPO / "docs" / "arcane-recipe-inventory.json").write_text(
            json.dumps(summary, indent=2) + "\n", encoding="utf-8")

        print(f"wrote {len(instances)} recipe instances for "
              f"{len(self.rows)} inventory rows")
        print(f"oredict tags referenced: {len(self.used_tags)}")
        if problems:
            print("PROBLEMS:")
            for p in sorted(set(problems)):
                print("  -", p)
            sys.exit(1)
        print("no extraction problems")

    def loop_context(self, body, call_pos):
        """Loop expansion for the two known generated families.

        Returns [(loop_var, primal_override_or_None)] per iteration, or None.
        Banner loop: aspect-free loop var + banner var with colour.
        Primal arrow loop: primal aspect per iteration (pa[a] table).
        """
        head = body[:call_pos]
        loop_start = head.rfind("for (int a = 0; a < ")
        if loop_start == -1:
            return None
        # the call site must sit inside the loop body: match its braces
        brace = body.index("{", loop_start)
        if call_pos > match_brace(body, brace):
            return None
        m = re.match(r"for \(int a = 0; a < (\d+); a\+\+\)", body[loop_start:])
        if not m:
            problems.append(f"unrecognized loop: {body[loop_start:loop_start+60]!r}")
            return None
        n = int(m.group(1))
        primals = [self.resolver.aspect_tags[c] for c in
                   ("AIR", "FIRE", "WATER", "EARTH", "ORDER", "ENTROPY")]
        if n == 16:
            return [(i, None) for i in range(n)]
        if n == 6:
            return [(i, (primals[i],)) for i in range(n)]
        problems.append(f"unexpected loop size {n}")
        return None

    def inventory_rows(self):
        """content-mapping recipe rows of the two arcane kinds."""
        rows = []
        with CONTENT_CSV.open(newline="", encoding="utf-8") as fh:
            for row in csv.DictReader(fh):
                notes = row.get("notes") or ""
                m = re.search(r"type=(arcane_shaped|arcane_shapeless)\b"
                              r"(?:.*?research=(\w+))?", notes)
                if not m:
                    continue
                rows.append({
                    "id": row["original_id"].split(":", 1)[1],
                    "kind": m.group(1),
                    "research": m.group(2),
                    "result": self.norm_result(row["display_name"]),
                })
        return rows

    @staticmethod
    def norm_result(text):
        """Canonical form of a result signature for call<->row matching.

        The inventory display names are inconsistent about the item-prefix
        (ItemResource vs PrimalArrow for itemResource/itemPrimalArrow), so
        strip Config* receivers, lowercase, and drop a leading "item" from
        item-family field names.
        """
        t = re.sub(r"\s+", " ", text.strip())
        m0 = re.match(r"^new ItemStack\((.*)\)$", t, re.S)
        if m0:
            t = m0.group(1).strip()
        t = re.sub(r"^Config(?:Items|Blocks)\.", "", t)
        m = re.match(r"^([\w.]+)(?:,\s*(\d+))?(?:,\s*([\w]+))?$", t)
        if not m:
            return t.lower()
        field, count, meta = m.groups()
        field = field.split(".")[-1].lower()
        if field.startswith("item") and len(field) > 4:
            field = field[4:]
        return f"{field}|{count or '1'}|{meta or '0'}"

    def pair_calls(self, body, call_sites, rows):
        """Match each call site to its inventory row by (research, result).

        TC-M0-04 numbered rows in its own order, not source order, so
        positional pairing is unsafe; the (research, result-signature) pair
        is unique across the 89 rows (verified here).
        """
        pairs = []
        used = set()
        for pos, name, shapeless in call_sites:
            open_paren = pos + re.match(
                r"ThaumcraftApi\s*\.\s*\w+\s*\(", body[pos:]).end() - 1
            args = split_args(body[open_paren + 1:match_paren(body, open_paren)])
            research = parse_string_literal(args[0])
            result_sig = self.norm_result(args[1])
            if result_sig == "banner|1|0":
                result_sig = "banner"      # loop variable, matched by research
            result_sig = RESULT_ALIASES.get(result_sig, result_sig)
            cands = [i for i, r in enumerate(rows)
                     if i not in used and r["research"] == research
                     and (r["result"] == result_sig
                          or result_sig == "banner")]
            if len(cands) != 1:
                problems.append(
                    f"call at {pos} research={research} result={result_sig} "
                    f"matched {len(cands)} inventory rows")
                continue
            used.add(cands[0])
            pairs.append((pos, name, shapeless, rows[cands[0]]["id"]))
        if len(pairs) != len(rows) or len(pairs) != len(call_sites):
            problems.append(f"pairing incomplete: {len(pairs)} pairs, "
                            f"{len(rows)} rows, {len(call_sites)} calls")
        return pairs

if __name__ == "__main__":
    RecipeExtractor().run()
