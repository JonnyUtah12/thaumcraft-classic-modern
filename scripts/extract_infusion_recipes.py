#!/usr/bin/env python3
"""Extract the complete Thaumcraft 4.2.3.5 infusion recipe set into data-driven JSON.

TC-M4-04. Reads the decompiled original (kept outside the repository at
../reference/decompiled, see docs/licensing-and-attribution.md) and writes:

  src/main/resources/data/thaumcraft/recipe/recipe.<N>.infusion.json
  src/main/resources/data/thaumcraft/recipe/recipe.<N>.infusion_enchantment.json

The ids reproduce the content-mapping.csv numbering exactly: recipe indices
are assigned per recipe type in the CSV's type order, so infusion recipes are
numbered 49..111 (49 + call order of ThaumcraftApi.addInfusionCraftingRecipe
in ConfigRecipes.java) and infusion-enchantment recipes 201..224 (201 + call
order of ThaumcraftApi.addInfusionEnchantmentRecipe). The extractor asserts
the counts (63 + 24) and cross-checks every research key against the CSV.

Method: same brace/paren-balanced scanner + Resolver as
scripts/extract_research_graph.py (imported here); anything unrecognised is
reported, never guessed. Run: python3 scripts/extract_infusion_recipes.py
"""

import csv
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from extract_research_graph import (  # noqa: E402
    DECOMPILED, Resolver, split_args, problems,
)

SRC_RECIPES = DECOMPILED / "ConfigRecipes.java"
CSV_PATH = Path(__file__).resolve().parent.parent / "docs" / "content-mapping.csv"
OUT_DIR = (Path(__file__).resolve().parent.parent / "src" / "main" /
           "resources" / "data" / "thaumcraft" / "recipe")

INFUSION_BASE = 49          # content-mapping.csv numbering (crucible occupies 0..48)
INFUSION_ENCHANTMENT_BASE = 201

SOURCE_NOTE = (
    "decompiled thaumcraft/common/config/ConfigRecipes.java (Thaumcraft "
    "4.2.3.5), initializeInfusionRecipes + initializeInfusionEnchantment"
    "Recipes. Extracted mechanically by scripts/extract_infusion_recipes.py."
)

# Vanilla 1.7.10 meta items have no single modern item; map the ones the
# infusion set actually uses to their 1.21.1 equivalents. Anything not listed
# is reported as a problem instead of guessed.
VANILLA_META_ITEMS = {
    "dye": {15: "minecraft:bone_meal", 0: "minecraft:ink_sac", 1: "minecraft:red_dye",
            2: "minecraft:green_dye", 3: "minecraft:brown_dye", 4: "minecraft:blue_dye",
            5: "minecraft:purple_dye", 6: "minecraft:cyan_dye", 7: "minecraft:light_gray_dye",
            8: "minecraft:gray_dye", 9: "minecraft:pink_dye", 10: "minecraft:lime_dye",
            11: "minecraft:yellow_dye", 12: "minecraft:light_blue_dye", 13: "minecraft:magenta_dye",
            14: "minecraft:orange_dye", 15 + 16: "minecraft:bone_meal"},
    "log": {0: "minecraft:oak_log", 1: "minecraft:spruce_log", 2: "minecraft:birch_log",
            3: "minecraft:jungle_log"},
    "planks": {0: "minecraft:oak_planks", 1: "minecraft:spruce_planks",
               2: "minecraft:birch_planks", 3: "minecraft:jungle_planks"},
    "wool": {0: "minecraft:white_wool", 1: "minecraft:orange_wool", 2: "minecraft:magenta_wool",
             3: "minecraft:light_blue_wool", 4: "minecraft:yellow_wool", 5: "minecraft:lime_wool",
             6: "minecraft:pink_wool", 7: "minecraft:gray_wool", 8: "minecraft:light_gray_wool",
             9: "minecraft:cyan_wool", 10: "minecraft:purple_wool", 11: "minecraft:blue_wool",
             12: "minecraft:brown_wool", 13: "minecraft:green_wool", 14: "minecraft:red_wool",
             15: "minecraft:black_wool"},
    "sand": {0: "minecraft:sand", 1: "minecraft:red_sand"},
    "sandstone": {0: "minecraft:sandstone", 1: "minecraft:chiseled_sandstone",
                  2: "minecraft:cut_sandstone"},
    "stone": {0: "minecraft:stone", 1: "minecraft:granite", 2: "minecraft:polished_granite",
              3: "minecraft:diorite", 4: "minecraft:polished_diorite", 5: "minecraft:andesite",
              6: "minecraft:polished_andesite"},
    "leaves": {0: "minecraft:oak_leaves", 1: "minecraft:spruce_leaves",
               2: "minecraft:birch_leaves", 3: "minecraft:jungle_leaves"},
    "sapling": {0: "minecraft:oak_sapling", 1: "minecraft:spruce_sapling",
                2: "minecraft:birch_sapling", 3: "minecraft:jungle_sapling"},
    "fish": {0: "minecraft:cod", 1: "minecraft:salmon", 2: "minecraft:tropical_fish",
             3: "minecraft:pufferfish"},
    "skull": {0: "minecraft:skeleton_skull", 1: "minecraft:wither_skeleton_skull",
              2: "minecraft:zombie_head", 3: "minecraft:player_head",
              4: "minecraft:creeper_head", 5: "minecraft:dragon_head"},
}

# Thaumcraft's own enchantments (registered by id via config in the original;
# the port registers them under the content-mapping.csv contract ids —
# recipes simply do not match until the enchantments exist, same as for
# not-yet-ported items).
TC_ENCHANTMENTS = {"enchantRepair": "thaumcraft:ench_repair",
                  "enchantHaste": "thaumcraft:ench_haste"}

# Config-gated recipes (original: if (Config.foundSilverIngot) / if (Config.allowMirrors)
# around the registration). The port expresses these as datapack conditions:
# found* ore-dictionary probes become tag_nonempty on the conventional tag;
# allowMirrors becomes item_exists on the mirror block (the mirror feature
# registers that block only when allow_mirrors is enabled).
CONDITIONS = {
    "foundSilverIngot": [{"type": "neoforge:not",
                          "value": {"type": "neoforge:tag_empty",
                                    "tag": "c:ingots/silver"}}],
    "allowMirrors": [{"type": "neoforge:item_exists", "item": "thaumcraft:blockmirror"}],
}


def call_body(src, start):
    """Full argument text of the call whose '(' follows src[start:]."""
    i = src.index("(", start)
    depth = 0
    for j in range(i, len(src)):
        if src[j] == "(":
            depth += 1
        elif src[j] == ")":
            depth -= 1
            if depth == 0:
                return src[i + 1:j]
    raise AssertionError("unbalanced call at " + str(start))


def enclosing_condition(src, pos):
    """The innermost `if (Config.<flag>) {` block still open at pos."""
    best = None
    for m in re.finditer(r"if \(Config\.(\w+)\) \{", src):
        if m.start() < pos:
            # crude brace scan: the flag block must not have closed before pos
            depth = 0
            i = src.index("{", m.end() - 1)
            closed = False
            for j in range(i, pos):
                if src[j] == "{":
                    depth += 1
                elif src[j] == "}":
                    depth -= 1
                    if depth == 0:
                        closed = True
                        break
            if not closed:
                best = m.group(1)
    return best


def load_port_item_ids():
    """original_id -> new_id for items/blocks from content-mapping.csv
    (D-001: ids are preserved with case change only; the CSV is the
    authoritative mapping, plain lowercasing is the fallback)."""
    mapping = {}
    with CSV_PATH.open(newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row["kind"] in ("item", "block"):
                mapping[row["original_id"]] = row["new_id"]
    return mapping


PORT_ITEM_IDS = load_port_item_ids()


def port_item_id(item):
    if item.startswith("Thaumcraft:"):
        return PORT_ITEM_IDS.get(item, "thaumcraft:" + item[len("Thaumcraft:"):].lower())
    return item


# 1.7.10 MCP field names whose modern registry path differs. The field table
# (reference/mcp-mappings) carries the 1.7.10 MCP spelling verbatim, and an id
# that does not exist in 1.21.1 is a hard recipe parse error (RecipeManager
# drops the row), so every name this set uses is renamed explicitly here.
VANILLA_RENAMES = {
    "deadbush": "dead_bush",
    "reeds": "sugar_cane",
    "web": "cobweb",
    "potionitem": "potion",
}

# meta 32767 is the original's "any meta" wildcard; the faithful modern form is
# the tag that covers the family.
VANILLA_ANY_META = {
    "fish": {"tag": "minecraft:fishes"},
    "dye": {"tag": "c:dyes"},
}


def ingredient(resolver, stack):
    """Convert a resolved itemstack dict to a 1.21.1 Ingredient JSON object."""
    if stack.get("kind") == "unknown":
        problems.append(f"unknown item in ingredient: {stack.get('raw')!r}")
        return {"item": "minecraft:air"}
    item = port_item_id(stack["item"])
    meta = stack.get("meta", 0)
    if item.startswith("minecraft:"):
        base = item[len("minecraft:"):]
        base = VANILLA_RENAMES.get(base, base)
        item = f"minecraft:{base}"
        if base in ("potion", "splash_potion"):
            # 1.7.10's potionitem: meta 0 is the plain water bottle, 32767 any
            # potion, everything else the POTION_META_MAP set.
            if meta == 32767:
                return {"item": item}
            return potion_ingredient(meta)
        if base in VANILLA_META_ITEMS:
            modern = VANILLA_META_ITEMS[base].get(meta)
            if modern:
                return {"item": modern}
            if meta == 32767 and base in VANILLA_ANY_META:
                return dict(VANILLA_ANY_META[base])
            if meta in (0, 32767):
                return {"item": item}
            problems.append(f"unmapped vanilla meta item: {item} meta {meta}")
            return {"item": item}
        return {"item": item}
    # mod item: meta rides the thaumcraft:item_meta component (D-001);
    # meta 32767 is the original's "any meta" wildcard -> plain item ingredient.
    if meta == 32767:
        return {"item": item}
    if meta == 0:
        return {"item": item}
    return {"type": "neoforge:components", "items": [item],
            "components": {"thaumcraft:item_meta": meta}}


# 1.7.10 potion damage -> modern potion id. Flags: 0x4000 splash, 0x2000
# extended, 0x20 upgraded, 0x40 cured (level II of bad potions). Modern
# potions have no combined strong+long variants and no "cured" flag; the
# closest modern potion is used and the difference recorded in the recipe
# ("known_differences"). Only the metas the original set actually uses.
POTION_META_MAP = {
    0: ("water", None),          # the plain potionitem: a water bottle
    8233: ("strong_poison", "original: extended+upgraded poison; modern has no strong+long poison"),
    8226: ("strong_healing", "original: extended+upgraded healing; extended is a no-op for instant effects"),
    8257: ("long_regeneration", 'original: extended+"cured" regeneration; the cured flag has no modern equivalent'),
    16428: ("long_slowness", "original: splash upgraded slowness; modern has no strong slowness"),
    24620: ("long_slowness", "original: splash extended upgraded slowness; modern has no strong slowness"),
}


def potion_ingredient(meta):
    modern, note = POTION_META_MAP[meta]
    splash = bool(meta & 0x4000)
    item = "minecraft:splash_potion" if splash else "minecraft:potion"
    ingredient = {"type": "neoforge:components", "items": [item],
                  "components": {"minecraft:potion_contents": {"potion": f"minecraft:{modern}"}}}
    if note is not None:
        ingredient["_known_difference"] = note
    return ingredient


def result_stack(resolver, stack):
    """Convert a resolved itemstack dict to a 1.21.1 result object."""
    if stack.get("kind") == "unknown":
        problems.append(f"unknown item in result: {stack.get('raw')!r}")
        return {"id": "minecraft:air"}
    out = {"id": port_item_id(stack["item"]), "count": 1}
    meta = stack.get("meta", 0)
    if meta not in (0, 32767):
        if stack["item"].startswith("minecraft:"):
            base = stack["item"][len("minecraft:"):]
            modern = VANILLA_META_ITEMS.get(base, {}).get(meta)
            if modern:
                out["id"] = modern
                return out
            problems.append(f"unmapped vanilla meta result: {stack['item']} meta {meta}")
            return out
        out["components"] = {"thaumcraft:item_meta": meta}
    return out


def aspect_cost(resolver, expr):
    """WandCap.caps.get("silver").getCraftCost() [* N] -> int."""
    m = re.match(r'Wand(?:Cap|Rod)\.(?:caps|rods)\.get\("(\w+)"\)\.getCraftCost\(\)(?:\s*\*\s*(\d+))?$',
                 expr.strip())
    if not m:
        return None
    kind = "CAP" if "Cap" in expr else "ROD"
    key = f"WAND_{kind}_{m.group(1).upper()}"
    cost = resolver.component_costs.get(key)
    if cost is None:
        cost = STAFF_ROD_COSTS.get(m.group(1))
    if cost is None:
        problems.append(f"unknown wand component cost: {key}")
        return None
    return cost * (int(m.group(2)) if m.group(2) else 1)


# StaffRod constants (the shared loader only parses WAND_* constructors).
# StaffRod(tag, capacity, item, craftCost, ...) registers under tag+"_staff"
# with craftCost as the 4th constructor argument.
def load_staff_rod_costs():
    src = (DECOMPILED / "ConfigItems.java").read_text(encoding="utf-8", errors="replace")
    return {m.group(1) + "_staff": int(m.group(3))
            for m in re.finditer(
                r'new StaffRod\("(\w+)", (\d+), new ItemStack\([^)]*\), (\d+)', src)}


STAFF_ROD_COSTS = load_staff_rod_costs()


def aspect_list(resolver, expr):
    """AspectList with either literal amounts or wand-component costs."""
    tags = []
    base = expr.strip()
    if not base.startswith("new AspectList()"):
        problems.append(f"expected AspectList expression: {expr!r}")
        return tags
    pos = len("new AspectList()")
    while pos < len(base):
        m = re.match(r"\s*\.add\(Aspect\.([A-Z_0-9]+)\s*,", base[pos:])
        if not m:
            break
        const = m.group(1)
        i = pos + m.end()
        depth, j = 1, i
        while j < len(base) and depth:
            if base[j] == "(":
                depth += 1
            elif base[j] == ")":
                depth -= 1
            j += 1
        amount_expr = base[i:j - 1].strip()
        tag = resolver.aspect_tags.get(const)
        if tag is None:
            problems.append(f"unknown Aspect constant: Aspect.{const}")
        else:
            if amount_expr.lstrip("-").isdigit():
                amount = int(amount_expr)
            else:
                amount = aspect_cost(resolver, amount_expr)
            if amount is not None:
                tags.append({"aspect": tag, "constant": const, "amount": amount})
        pos = j
    if base[pos:].strip():
        problems.append(f"unparsed remainder in AspectList expression: {base[pos:]!r}")
    return tags


def parse_calls(src, api):
    """Yield (position, map-key-or-None, args) for every ThaumcraftApi.<api>(
    call, in source order. The map key is the ConfigResearch.recipes key the
    research graph uses to reference the recipe ("WandCapSilver" etc.)."""
    for m in re.finditer(r"ThaumcraftApi\." + api + r"\(", src):
        head = src[:m.start()]
        key = None
        pm = re.search(r'\.put\(\s*"([^"]+)"\s*,\s*$', head)
        if pm:
            key = pm.group(1)
        yield m.start(), key, split_args(call_body(src, m.start()))


def main():
    src = SRC_RECIPES.read_text(encoding="utf-8", errors="replace")
    resolver = Resolver()

    # expected research keys from content-mapping.csv, in id order
    expected = {}
    with CSV_PATH.open(newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            m = re.match(r"Thaumcraft:recipe\.(\d+)\.(infusion|infusion_enchantment)$",
                         row["original_id"])
            if m:
                rm = re.search(r"research=(\S+)", row["notes"] or "")
                expected[(m.group(2), int(m.group(1)))] = rm.group(1) if rm else None

    written = []

    # --- infusion crafting recipes ------------------------------------------
    infusion_calls = list(parse_calls(src, "addInfusionCraftingRecipe"))
    if len(infusion_calls) != 63:
        problems.append(f"expected 63 addInfusionCraftingRecipe calls, found {len(infusion_calls)}")
    for i, (pos, map_key, args) in enumerate(infusion_calls):
        n = INFUSION_BASE + i
        if len(args) != 6:
            problems.append(f"infusion recipe {n}: unexpected arity {len(args)}")
            continue
        if map_key is None:
            problems.append(f"infusion recipe {n}: not wrapped in a ConfigResearch.recipes.put")
        research = args[0].strip().strip('"')
        output_expr, instability, aspects_expr, input_expr, comps_expr = args[1:]
        expected_key = expected.get(("infusion", n))
        if expected_key != research:
            problems.append(f"infusion recipe {n}: research {research!r} != csv {expected_key!r}")

        recipe = {
            "type": "thaumcraft:infusion",
            "original_key": map_key or f"recipe.{n}.infusion",
            "research": research,
            "instability": int(instability.strip()),
            "aspects": aspect_list(resolver, aspects_expr),
            "input": ingredient(resolver, resolver.itemstack(input_expr)),
            "components": [],
            "_source": SOURCE_NOTE,
        }
        # components: new ItemStack[]{ ... }
        comps = comps_expr.strip()
        m = re.match(r"new ItemStack\[\]\s*\{(.*)\}$", comps, re.S)
        if not m:
            problems.append(f"infusion recipe {n}: unparsed components: {comps[:60]!r}")
        else:
            for comp in split_args(m.group(1)):
                comp = comp.strip()
                if comp.startswith('"'):  # ore-dictionary string component
                    problems.append(f"infusion recipe {n}: oredict string component {comp}")
                    continue
                recipe["components"].append(
                    ingredient(resolver, resolver.itemstack(comp)))

        # output: ItemStack or Object[]{label, NBTTag...}
        out_expr = output_expr.strip()
        if out_expr.startswith("new Object["):
            inner = split_args(re.match(r"new Object\[\]\s*\{(.*)\}$", out_expr, re.S).group(1))
            label = inner[0].strip().strip('"')
            tag = inner[1].strip()
            m2 = re.match(r"new NBTTagByte\(\(byte\)(-?\d+)\)", tag) or \
                 re.match(r"new NBTTagInt\((-?\d+)\)", tag) or \
                 re.match(r"new NBTTagShort\(\(short\)(-?\d+)\)", tag)
            if not m2:
                problems.append(f"infusion recipe {n}: unparsed output tag: {tag!r}")
                continue
            # the Object[] recipes (golem placer, masks) apply the tag to the
            # CENTRAL item on the pedestal; there is no concrete output stack.
            recipe["output_tag"] = {"label": label, "value": int(m2.group(1))}
        else:
            recipe["output"] = result_stack(resolver, resolver.itemstack(out_expr))

        cond = enclosing_condition(src, pos)
        if cond:
            if cond not in CONDITIONS:
                problems.append(f"infusion recipe {n}: unhandled config gate {cond}")
            else:
                recipe["neoforge:conditions"] = CONDITIONS[cond]

        write(recipe, f"recipe.{n}.infusion.json")
        written.append(f"recipe.{n}.infusion.json")

    # --- infusion enchantment recipes ---------------------------------------
    ench_calls = list(parse_calls(src, "addInfusionEnchantmentRecipe"))
    if len(ench_calls) != 24:
        problems.append(f"expected 24 addInfusionEnchantmentRecipe calls, found {len(ench_calls)}")
    for i, (pos, map_key, args) in enumerate(ench_calls):
        n = INFUSION_ENCHANTMENT_BASE + i
        if len(args) != 5:
            problems.append(f"infusion_enchantment recipe {n}: unexpected arity {len(args)}")
            continue
        if map_key is None:
            problems.append(f"infusion_enchantment recipe {n}: not wrapped in a ConfigResearch.recipes.put")
        research = args[0].strip().strip('"')
        ench_expr, instability, aspects_expr, comps_expr = args[1:]
        expected_key = expected.get(("infusion_enchantment", n))
        if expected_key != research:
            problems.append(f"infusion_enchantment recipe {n}: research {research!r} != csv {expected_key!r}")
        m = re.match(r"Enchantment\.field_\w+\[ThaumcraftApi\.(\w+)\]$", ench_expr.strip())
        if m:
            if m.group(1) not in TC_ENCHANTMENTS:
                problems.append(f"infusion_enchantment recipe {n}: unknown TC enchantment {m.group(1)}")
                continue
            ench_id = TC_ENCHANTMENTS[m.group(1)]
        else:
            m = re.match(r"Enchantment\.(\w+)$", ench_expr.strip())
            if not m:
                problems.append(f"infusion_enchantment recipe {n}: unparsed enchantment {ench_expr!r}")
                continue
            srg = m.group(1)
            ench_name = resolver.vanilla.get(srg)
            if ench_name is None:
                problems.append(f"infusion_enchantment recipe {n}: unmapped Enchantment field {srg}")
                continue
            # The mapping table carries the MCP camelCase name; the 1.21.1
            # registry path is its snake_case form (baneOfArthropods ->
            # minecraft:bane_of_arthropods). An id that is not a valid resource
            # location is a hard recipe parse error, so it is checked here.
            ench_path = re.sub(r"(?<!^)(?=[A-Z])", "_", ench_name).lower()
            if not re.fullmatch(r"[a-z0-9/._-]+", ench_path):
                problems.append(
                    f"infusion_enchantment recipe {n}: invalid enchantment path {ench_path!r}")
                continue
            ench_id = f"minecraft:{ench_path}"

        recipe = {
            "type": "thaumcraft:infusion_enchantment",
            "original_key": map_key or f"recipe.{n}.infusion_enchantment",
            "research": research,
            "enchantment": ench_id,
            "instability": int(instability.strip()),
            "aspects": aspect_list(resolver, aspects_expr),
            "components": [],
            "_source": SOURCE_NOTE,
        }
        mm = re.match(r"new ItemStack\[\]\s*\{(.*)\}$", comps_expr.strip(), re.S)
        if not mm:
            problems.append(f"infusion_enchantment recipe {n}: unparsed components")
        else:
            for comp in split_args(mm.group(1)):
                recipe["components"].append(
                    ingredient(resolver, resolver.itemstack(comp)))

        cond = enclosing_condition(src, pos)
        if cond:
            if cond not in CONDITIONS:
                problems.append(f"infusion_enchantment recipe {n}: unhandled config gate {cond}")
            else:
                recipe["neoforge:conditions"] = CONDITIONS[cond]

        write(recipe, f"recipe.{n}.infusion_enchantment.json")
        written.append(f"recipe.{n}.infusion_enchantment.json")

    print(f"wrote {len(written)} recipe files to {OUT_DIR}")
    if problems:
        print(f"{len(problems)} problems:")
        for p in problems:
            print("  -", p)
        return 1
    return 0


def item_ids(node):
    """Every item id a recipe's ingredient/output objects name."""
    found = []
    if isinstance(node, dict):
        for key in ("item", "id"):
            value = node.get(key)
            if isinstance(value, str):
                found.append(value)
        items = node.get("items")
        if isinstance(items, str):
            found.append(items)
        elif isinstance(items, list):
            found.extend(v for v in items if isinstance(v, str))
        for key in ("input", "components", "output"):
            value = node.get(key)
            if isinstance(value, (dict, list)):
                found.extend(item_ids(value))
    elif isinstance(node, list):
        for value in node:
            found.extend(item_ids(value))
    return found


def write(recipe, name):
    """Write one recipe, gated on the existence of every item it names.

    A row whose items are not ported yet must be SKIPPED at load, not fail:
    an unregistered item id inside an Ingredient is a hard parse error
    (RecipeManager logs 'Parsing error loading recipe' and drops the row), and
    the meta-carrying form cannot be decoded at all without the registered
    neoforge:components IngredientType. neoforge:item_exists is the platform's
    own gate for "ship the data now, activate it when the item lands" — the
    same idiom the Config-gated rows below already use. Duplicate ids collapse;
    the gate conditions (tag_nonempty / allowMirrors) keep their order first.
    """
    conditions = list(recipe.get("neoforge:conditions", []))
    seen = {c.get("item") for c in conditions
            if c.get("type") == "neoforge:item_exists"}
    for item in item_ids(recipe):
        if item == "minecraft:air" or item in seen:
            continue
        seen.add(item)
        conditions.append({"type": "neoforge:item_exists", "item": item})
    if conditions:
        recipe["neoforge:conditions"] = conditions
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / name
    path.write_text(json.dumps(recipe, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main())
