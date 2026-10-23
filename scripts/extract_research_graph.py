#!/usr/bin/env python3
"""Extract the complete Thaumcraft 4.2.3.5 research graph into data-driven JSON.

TC-M3-01. Reads the decompiled original (kept outside the repository at
../reference/decompiled, see docs/licensing-and-attribution.md) and writes:

  src/main/resources/data/thaumcraft/research_graph/categories.json
  src/main/resources/data/thaumcraft/research_graph/entries.json

Method and decisions: docs/research-graph-notes.md. The parser is a
brace/paren-balanced scanner over the decompiled ConfigResearch.java (the
class that registers the whole research graph in this decompilation; the
upstream source file was named Research.java). Every construct the file
uses is handled explicitly; anything unrecognised is reported, never
guessed. Run: python3 scripts/extract_research_graph.py
"""

import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
REF = REPO.parent / "reference"
DECOMPILED = REF / "decompiled" / "thaumcraft" / "thaumcraft" / "common" / "config"
SRC_RESEARCH = DECOMPILED / "ConfigResearch.java"
SRC_RECIPES = DECOMPILED / "ConfigRecipes.java"
SRC_ITEMS = DECOMPILED / "ConfigItems.java"
SRC_BLOCKS = DECOMPILED / "ConfigBlocks.java"
SRC_ASPECTS = REF / "decompiled" / "thaumcraft" / "thaumcraft" / "api" / "aspects" / "Aspect.java"
MCP_FIELDS = REF / "mcp-mappings" / "fields_1.7.10_mcp_stable_11.csv"
LANG = REF / "assets" / "thaumcraft" / "lang" / "en_US.lang"

OUT_DIR = REPO / "src" / "main" / "resources" / "data" / "thaumcraft" / "research_graph"

SOURCE_NOTE = (
    "decompiled thaumcraft/common/config/ConfigResearch.java (Thaumcraft "
    "4.2.3.5; upstream source file name: thaumcraft/common/lib/research/"
    "Research.java). Extracted mechanically by scripts/"
    "extract_research_graph.py."
)

problems = []   # hard extraction problems (unparsed constructs)
notes = []      # observations for docs/research-graph-notes.md


# --------------------------------------------------------------------------
# Java source scanning helpers
# --------------------------------------------------------------------------

def strip_comments(src: str) -> str:
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == '"':
                    break
                j += 1
            out.append(src[i:j + 1])
            i = j + 1
        elif c == "'":
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == "'":
                    break
                j += 1
            out.append(src[i:j + 1])
            i = j + 1
        elif src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j == -1 else j
        elif src.startswith("/*", i):
            j = src.find("*/", i)
            i = n if j == -1 else j + 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def match_paren(s: str, open_idx: int) -> int:
    """Index of the ')' matching the '(' at open_idx."""
    depth, i, n = 0, open_idx, len(s)
    while i < n:
        c = s[i]
        if c == '"':
            i += 1
            while i < n and s[i] != '"':
                i += 2 if s[i] == "\\" else 1
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced parens")


def split_args(s: str) -> list[str]:
    """Split a comma-separated argument list at paren/bracket/brace depth 0."""
    args, depth, cur, i, n = [], 0, [], 0, len(s)
    while i < n:
        c = s[i]
        if c == '"':
            j = i + 1
            while j < n:
                if s[j] == "\\":
                    j += 2
                    continue
                if s[j] == '"':
                    break
                j += 1
            cur.append(s[i:j + 1])
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    tail = "".join(cur).strip()
    if tail:
        args.append(tail)
    return args


def parse_string_literal(s: str) -> str:
    s = s.strip()
    assert s.startswith('"') and s.endswith('"'), f"not a string literal: {s!r}"
    body = s[1:-1]
    # Java escapes in the lang keys / entity names are limited to the usual set.
    return body.encode().decode("unicode_escape")


def parse_chain(s: str) -> list[tuple[str, list[str]]]:
    """Parse `.name(args).name(args)...` at depth 0 into (name, args) calls."""
    calls, i, n = [], 0, len(s)
    while i < n:
        c = s[i]
        if c == '"':
            i += 1
            while i < n and s[i] != '"':
                i += 2 if s[i] == "\\" else 1
            i += 1
            continue
        if c == ".":
            m = re.match(r"\.([A-Za-z_]\w*)\s*\(", s[i:])
            if m:
                open_idx = i + m.end() - 1
                close = match_paren(s, open_idx)
                calls.append((m.group(1), split_args(s[open_idx + 1:close])))
                i = close + 1
                continue
        i += 1
    return calls


# --------------------------------------------------------------------------
# Reference maps (items, blocks, vanilla fields, aspects, lang, recipe keys)
# --------------------------------------------------------------------------

def load_field_maps():
    items_src = SRC_ITEMS.read_text(encoding="utf-8", errors="replace")
    blocks_src = SRC_BLOCKS.read_text(encoding="utf-8", errors="replace")
    item_fields = dict(re.findall(
        r"GameRegistry\.registerItem\((\w+),\s*\"([^\"]+)\"", items_src))
    block_fields = {}
    for m in re.finditer(
            r"GameRegistry\.registerBlock\((\w+)(?:,\s*[\w.]+\.class)?,\s*\"([^\"]+)\"\)",
            blocks_src):
        block_fields[m.group(1)] = m.group(2)

    vanilla = {}
    if MCP_FIELDS.is_file():
        for line in MCP_FIELDS.read_text(encoding="utf-8", errors="replace").splitlines()[1:]:
            parts = line.split(",")
            if len(parts) >= 2:
                vanilla[parts[0]] = parts[1]
    return item_fields, block_fields, vanilla


def load_aspect_tags():
    src = SRC_ASPECTS.read_text(encoding="utf-8", errors="replace")
    return dict(re.findall(r"public static final Aspect ([A-Z_0-9]+) = new Aspect\(\"([^\"]+)\"", src))


def load_component_costs():
    """WandCap/WandRod craftCost per ConfigItems constant (last int ctor arg)."""
    src = SRC_ITEMS.read_text(encoding="utf-8", errors="replace")
    costs = {}
    for m in re.finditer(r"WAND_(CAP|ROD)_(\w+) = new Wand(?:Cap|Rod)\((.*?)\);", src, re.S):
        ints = re.findall(r"(?<![\w.])-?\d+(?![\w.])", m.group(3))
        if ints:
            costs[f"WAND_{m.group(1)}_{m.group(2)}"] = int(ints[-1])
    return costs


def load_lang():
    entries = {}
    for raw in LANG.read_bytes().decode("utf-8").splitlines():
        raw = raw.strip()
        if not raw or raw.startswith("#") or "=" not in raw:
            continue
        key, value = raw.split("=", 1)
        entries[key.strip()] = value
    return entries


def load_recipe_keys():
    """All ConfigResearch.recipes map keys registered by ConfigRecipes.java,
    including loop-generated keys ("Banner_" + a, "JarLabel" + count)."""
    src = SRC_RECIPES.read_text(encoding="utf-8", errors="replace")
    aspect_count = len(load_aspect_tags())
    keys = set()
    for m in re.finditer(r"\.put\(\s*\"([^\"]+)\"\s*(?:\+\s*(\w+))?\s*,", src, re.S):
        key, var = m.group(1), m.group(2)
        if not var:
            keys.add(key)
            continue
        head = src[:m.start()]
        loop = None
        for fm in re.finditer(r"for \(int (\w+) = (\d+); \w+ ([<>]=?) ([\w.()]+); \w+\+\+\)", head):
            if fm.group(1) == var:
                loop = fm
        if loop:
            lo = int(loop.group(2))
            hi_raw = loop.group(4)
            if not hi_raw.isdigit():
                problems.append(f"unresolved recipe key loop bound: {key} + {var} ({hi_raw})")
                continue
            hi = int(hi_raw) + (0 if loop.group(3) == "<=" else -1)
            for i in range(lo, hi + 1):
                keys.add(f"{key}{i}")
        elif var == "count" and re.search(r"for \(Aspect \w+ : Aspect\.aspects\.values\(\)\)", head):
            for i in range(aspect_count):
                keys.add(f"{key}{i}")
        else:
            problems.append(f"unresolved recipe key loop: {key} + {var}")
    return keys


# --------------------------------------------------------------------------
# Expression parsers
# --------------------------------------------------------------------------

class Resolver:
    def __init__(self):
        self.item_fields, self.block_fields, self.vanilla = load_field_maps()
        self.component_costs = load_component_costs()
        self.aspect_tags = load_aspect_tags()
        self.lang = load_lang()
        self.recipe_keys = load_recipe_keys()
        self.vars = {}          # local ItemStack variables (per init method)
        self.int_vars = {}      # local int variables (wand-component costs)
        self.lists = {}         # ArrayList<...> name -> resolved recipe keys

    def resolve_int(self, expr):
        expr = expr.strip()
        if expr.lstrip("-").isdigit():
            return int(expr)
        if expr in self.int_vars:
            return self.int_vars[expr]
        m = re.match(
            r"\(int\)\(\s*ConfigItems\.(WAND_\w+)\.getCraftCost\(\)\s*\*\s*"
            r"ConfigItems\.(WAND_\w+)\.getCraftCost\(\)\s*\*\s*([\d.]+)F?\s*\)", expr)
        if m:
            cap, rod, factor = m.group(1), m.group(2), float(m.group(3))
            if cap in self.component_costs and rod in self.component_costs:
                return int(self.component_costs[cap] * self.component_costs[rod] * factor)
        problems.append(f"unresolved int expression: {expr!r}")
        return None

    def itemstack(self, expr):
        expr = expr.strip()
        if expr in self.vars:
            return self.vars[expr]
        m = re.match(r"new ItemStack\((.*)\)$", expr, re.S)
        if not m:
            problems.append(f"unparsed ItemStack expression: {expr!r}")
            return {"kind": "unknown", "raw": expr}
        args = split_args(m.group(1))
        field = args[0].strip()
        meta = 0
        if len(args) > 2:
            meta = self.resolve_int(args[2])
            if meta is None:
                return {"kind": "unknown", "raw": expr}
        if field.startswith("ConfigItems."):
            name = self.item_fields.get(field.split(".", 1)[1])
            if name is None:
                problems.append(f"unmapped ConfigItems field: {field}")
                return {"kind": "unknown", "raw": expr}
            return {"item": f"Thaumcraft:{name}", "meta": meta}
        if field.startswith("ConfigBlocks."):
            name = self.block_fields.get(field.split(".", 1)[1])
            if name is None:
                problems.append(f"unmapped ConfigBlocks field: {field}")
                return {"kind": "unknown", "raw": expr}
            return {"item": f"Thaumcraft:{name}", "meta": meta}
        if field.startswith("Items.") or field.startswith("Blocks."):
            srg = field.split(".", 1)[1]
            name = self.vanilla.get(srg)
            if name is None:
                problems.append(f"unmapped vanilla field: {field}")
                return {"kind": "unknown", "raw": expr}
            return {"item": f"minecraft:{name}", "srgField": srg, "meta": meta}
        problems.append(f"unresolved ItemStack field/variable: {expr!r}")
        return {"kind": "unknown", "raw": expr}

    def aspect_list(self, expr):
        tags = []
        base = expr.strip()
        if not base.startswith("new AspectList()"):
            problems.append(f"expected AspectList expression: {expr!r}")
            return tags
        for m in re.finditer(r"\.add\(Aspect\.([A-Z_0-9]+)\s*,\s*(-?\d+)\)", base):
            const, amount = m.group(1), int(m.group(2))
            tag = self.aspect_tags.get(const)
            if tag is None:
                problems.append(f"unknown Aspect constant: Aspect.{const}")
                continue
            tags.append({"aspect": tag, "constant": const, "amount": amount})
        # verify nothing else is chained
        rest = re.sub(r"\.add\(Aspect\.[A-Z_0-9]+\s*,\s*-?\d+\)", "", base)
        if rest.strip() != "new AspectList()":
            problems.append(f"unparsed remainder in AspectList expression: {rest!r}")
        return tags

    def aspect_ref(self, expr):
        m = re.match(r"Aspect\.([A-Z_0-9]+)$", expr.strip())
        if not m or m.group(1) not in self.aspect_tags:
            problems.append(f"unparsed aspect reference: {expr!r}")
            return None
        return self.aspect_tags[m.group(1)]

    def resource_location(self, expr):
        m = re.match(r'new ResourceLocation\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)$', expr.strip())
        if not m:
            problems.append(f"unparsed ResourceLocation: {expr!r}")
            return None
        return f"{m.group(1)}:{m.group(2)}"

    def page(self, expr):
        expr = expr.strip()
        m = re.match(r"new ResearchPage\((.*)\)$", expr, re.S)
        if not m:
            problems.append(f"unparsed ResearchPage expression: {expr!r}")
            return {"type": "UNKNOWN"}
        args = split_args(m.group(1))
        if len(args) == 2:
            first, second = args
            if first.startswith('"'):
                # TEXT_CONCEALED: (researchKey, textKey)
                req = parse_string_literal(first)
                if req not in KNOWN_ENTRY_KEYS:
                    problems.append(f"page requires unknown research key: {req}")
                return self.text_page("text_concealed", parse_string_literal(second), requires=req)
            rl = self.resource_location(first)
            if rl:
                return {"type": "image", "image": rl,
                        **self.text_page("image", parse_string_literal(second))}
            problems.append(f"unparsed two-arg ResearchPage: {expr!r}")
            return {"type": "UNKNOWN"}
        if len(args) != 1:
            problems.append(f"unexpected ResearchPage arity: {expr!r}")
            return {"type": "UNKNOWN"}
        a = args[0]
        if a.startswith('"'):
            return self.text_page("text", parse_string_literal(a))
        m = re.match(r"\(([\w.]+)\)\s*recipes\.get\(\"([^\"]+)\"\)$", a)
        if m:
            cast, key = m.group(1), m.group(2)
            if key not in self.recipe_keys:
                problems.append(f"recipe key not found in ConfigRecipes.java: {key}")
            if cast == "List":
                return {"type": "compound_crafting", "recipeGroup": key}
            ptype = CAST_PAGE_TYPES.get(cast)
            if ptype is None:
                problems.append(f"unknown recipe page cast: {cast}")
                return {"type": "UNKNOWN"}
            return {"type": ptype, "recipe": key}
        m = re.match(r"(\w+)\.toArray\(new (\w+)\[0\]\)$", a)
        if m:
            list_name, elem = m.group(1), m.group(2)
            if list_name not in self.lists:
                problems.append(f"page references unknown recipe list: {list_name}")
                return {"type": "UNKNOWN"}
            ptype = CAST_PAGE_TYPES.get(elem)
            if ptype is None:
                problems.append(f"unknown array page element type: {elem}")
                return {"type": "UNKNOWN"}
            return {"type": ptype, "recipes": self.lists[list_name]}
        if a.startswith("new ItemStack("):
            return {"type": "smelting", "input": self.itemstack(a)}
        if a.startswith("new AspectList()"):
            return {"type": "aspects", "aspects": self.aspect_list(a)}
        problems.append(f"unparsed ResearchPage argument: {a!r}")
        return {"type": "UNKNOWN"}

    def text_page(self, ptype, text_key, requires=None):
        page = {"type": ptype, "textKey": text_key}
        if requires:
            page["requires"] = requires
        if text_key in self.lang:
            page["textEnUs"] = self.lang[text_key]
        else:
            page["textEnUs"] = None
            notes.append(f"lang key missing from original en_US.lang: {text_key}")
        return page


CAST_PAGE_TYPES = {
    "IRecipe": "normal_crafting",
    "IArcaneRecipe": "arcane_crafting",
    "CrucibleRecipe": "crucible_crafting",
    "InfusionRecipe": "infusion_crafting",
    "InfusionEnchantmentRecipe": "infusion_enchantment",
}

KNOWN_ENTRY_KEYS = set()


# --------------------------------------------------------------------------
# ConfigResearch.java parsing
# --------------------------------------------------------------------------

def parse_recipe_lists(src, resolver):
    """Resolve the ArrayList-of-recipes blocks into concrete recipe keys.

    Returns definitions as (position, list-name, keys); the caller applies
    them in source order because the decompiled file reuses the variable
    names `rc` for two different lists in different methods.
    """
    definitions = []
    # inline ShapedArcaneRecipe variables (sceptre recipes)
    recipe_vars = {}
    for m in re.finditer(r"ShapedArcaneRecipe (\w+) = new ShapedArcaneRecipe\(\s*\"([^\"]+)\"", src):
        recipe_vars[m.group(1)] = m.group(2)

    for m in re.finditer(r"ArrayList<([\w.]+)>\s+(\w+)\s*=\s*new ArrayList<>\(\);", src):
        elem, name = m.group(1), m.group(2)
        keys = []
        # direct `name.add(expr);` statements before the next ResearchItem
        seg_end = src.find("new ResearchItem", m.end())
        seg_end = len(src) if seg_end == -1 else seg_end
        direct = [am for am in re.finditer(rf"\b{re.escape(name)}\.add\(([^;]*)\);", src[m.end():seg_end])]
        if direct and all(am.group(1).strip() in recipe_vars for am in direct):
            for am in direct:
                add_expr = am.group(1).strip()
                keys.append(f"inline:ShapedArcaneRecipe#{recipe_vars[add_expr]}#{len(keys) + 1}")
            notes.append(
                f"recipe list {name}: {len(keys)} ShapedArcaneRecipe instances constructed "
                "inline in ConfigResearch (no recipes-map key)")
            definitions.append((m.start(), name, keys))
            continue

        for_start = src.find("for (", m.end())
        if for_start == -1 or for_start > seg_end:
            problems.append(f"recipe list {name}: no following for-loop found")
            continue
        header_open = match_paren(src, for_start + 3)
        header = src[for_start + 4:header_open]
        brace_open = src.find("{", header_open)
        brace_close = match_brace(src, brace_open)
        body = src[brace_open + 1:brace_close]

        lo, hi = 0, None
        m_lo = re.search(r"int a = (\d+)", header)
        m_hi = re.search(r"a\s*([<>]=?)\s*([^;]+);", header)
        if not m_lo or not m_hi:
            problems.append(f"recipe list {name}: unparsed loop header {header!r}")
            continue
        lo = int(m_lo.group(1))
        hi_expr = m_hi.group(2).strip()
        inclusive = m_hi.group(1) == "<="
        if hi_expr.isdigit():
            hi = int(hi_expr) + (0 if inclusive else -1)
        elif hi_expr.startswith("Aspect.aspects"):
            hi = len(resolver.aspect_tags) - 1
        else:
            problems.append(f"recipe list {name}: unparsed loop bound {hi_expr!r}")
            continue

        add_m = re.search(r"\.add\((.*)\);", body, re.S)
        if not add_m:
            problems.append(f"recipe list {name}: no .add() in loop body")
            continue
        add_expr = add_m.group(1).strip()
        m_key = re.match(r"\(([\w.]+)\)\s*recipes\.get\(\"([^\"]+)\"\s*(?:\+\s*a)?\)$", add_expr)
        if m_key:
            prefix = m_key.group(2)
            if "+ a" in add_expr:
                keys = [f"{prefix}{i}" for i in range(lo, hi + 1)]
            else:
                keys = [prefix]
        elif add_expr.startswith("new InfusionRunicAugmentRecipe("):
            # Recipes constructed inline (not registered in the recipes map).
            keys = [f"inline:InfusionRunicAugmentRecipe#{i}" for i in range(lo, hi + 1)]
            notes.append(
                f"recipe list {name}: {len(keys)} InfusionRunicAugmentRecipe instances "
                "constructed inline in ConfigResearch (no recipes-map key)")
        else:
            problems.append(f"recipe list {name}: unparsed add expression {add_expr!r}")
            continue
        for k in keys:
            if not k.startswith("inline:") and k not in resolver.recipe_keys:
                problems.append(f"recipe list {name}: key not in ConfigRecipes.java: {k}")
        definitions.append((m.start(), name, keys))
    return definitions


def match_brace(s, open_idx):
    depth, i, n = 0, open_idx, len(s)
    while i < n:
        c = s[i]
        if c == '"':
            i += 1
            while i < n and s[i] != '"':
                i += 2 if s[i] == "\\" else 1
            i += 1
            continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced braces")


def parse_categories(src, resolver):
    cats = []
    for m in re.finditer(r"ResearchCategories\.registerCategory\((.*?)\)\s*;", src, re.S):
        args = split_args(m.group(1))
        if len(args) != 3:
            problems.append(f"registerCategory arity: {args}")
            continue
        key = parse_string_literal(args[0])
        icon = resolver.resource_location(args[1])
        bg = resolver.resource_location(args[2])
        name_key = f"tc.research_category.{key}"
        cats.append({
            "key": key,
            "order": len(cats),
            "icon": icon,
            "background": bg,
            "nameKey": name_key,
            "nameEnUs": resolver.lang.get(name_key),
        })
        if name_key not in resolver.lang:
            notes.append(f"category lang key missing: {name_key}")
    return cats


def parse_warp(src):
    warp = {}
    for m in re.finditer(r"ThaumcraftApi\.addWarpToResearch\(\s*\"([^\"]+)\"\s*,\s*(\d+)\s*\)", src):
        warp[m.group(1)] = int(m.group(2))
    return warp


def parse_entries(src, resolver, list_defs):
    entries = []
    prev_end = 0
    def_idx = 0
    for m in re.finditer(r"new ResearchItem\(", src):
        # Apply recipe-list definitions in source order (the file reuses `rc`).
        while def_idx < len(list_defs) and list_defs[def_idx][0] < m.start():
            resolver.lists[list_defs[def_idx][1]] = list_defs[def_idx][2]
            def_idx += 1
        # Track local `ItemStack`/`int` assignments between statements; the
        # decompiled file uses `wand`, `is`, `sceptre3` as icon arguments and
        # `cost` as their computed metadata value.
        gap = src[prev_end:m.start()]
        assigns = []
        for gm in re.finditer(r"(?<![\w.])(\w+) = (new ItemStack\([^;]*\));", gap):
            assigns.append((gm.start(), gm.group(1), gm.group(2), "item"))
        for gm in re.finditer(r"int (\w+) = ([^;]+);", gap):
            assigns.append((gm.start(), gm.group(1), gm.group(2), "int"))
        # re-assignments like `cost = (int)(...);` (no `int` keyword)
        for gm in re.finditer(r"(?<![\w.])(\w+) = (\(int\)\([^;]+\));", gap):
            assigns.append((gm.start(), gm.group(1), gm.group(2), "int"))
        for _, var, expr, kind in sorted(assigns):
            if kind == "item":
                resolver.vars[var] = resolver.itemstack(expr)
            else:
                resolver.int_vars[var] = resolver.resolve_int(expr)
        prev_end = m.start()
        open_idx = m.end() - 1
        close = match_paren(src, open_idx)
        ctor = split_args(src[open_idx + 1:close])
        # chain until the terminating ';' at depth 0
        i = close + 1
        n = len(src)
        while i < n and src[i] != ";":
            i += 1
        chain_src = src[close + 1:i]
        calls = parse_chain(chain_src)

        e = {
            "key": parse_string_literal(ctor[0]),
            "category": parse_string_literal(ctor[1]),
            "column": 0, "row": 0,
            "complexity": None,
            "tags": [],
            "icon": {"kind": "none"},
            "flags": {},
            "parents": [], "parentsHidden": [], "siblings": [],
            "triggers": {"items": [], "entities": [], "aspects": []},
            "warp": 0,
            "nameKey": f"tc.research_name.{parse_string_literal(ctor[0])}",
            "textKey": f"tc.research_text.{parse_string_literal(ctor[0])}",
            "pages": [],
        }
        if len(ctor) == 2:
            e["flags"]["virtual"] = True
        elif len(ctor) == 7:
            e["tags"] = resolver.aspect_list(ctor[2])
            e["column"] = int(ctor[3])
            e["row"] = int(ctor[4])
            raw = int(ctor[5])
            e["complexity"] = min(3, max(1, raw))
            if raw != e["complexity"]:
                e["complexityRaw"] = raw
            icon_arg = ctor[6].strip()
            if icon_arg.startswith("new ResourceLocation("):
                e["icon"] = {"kind": "texture", "texture": resolver.resource_location(icon_arg)}
            else:
                ref = resolver.itemstack(icon_arg)
                if ref.get("kind") == "unknown":
                    e["icon"] = {"kind": "unknown", "raw": ref["raw"]}
                else:
                    e["icon"] = {"kind": "item", **ref}
        else:
            problems.append(f"unexpected ResearchItem constructor arity {len(ctor)} for {e['key']}")

        seen = {}
        for name, args in calls:
            if name in ("registerResearchItem",):
                continue
            if name in seen and name in ("setPages", "setParents", "setParentsHidden", "setSiblings"):
                notes.append(f"{e['key']}: {name} called twice; last call wins (original semantics)")
            seen[name] = args
        for name, args in seen.items():
            if name == "setPages":
                e["pages"] = [resolver.page(a) for a in args]
            elif name in ("setParents", "setParentsHidden", "setSiblings"):
                target = {"setParents": "parents", "setParentsHidden": "parentsHidden",
                          "setSiblings": "siblings"}[name]
                e[target] = [parse_string_literal(a) for a in args]
            elif name == "setItemTriggers":
                e["triggers"]["items"] = [resolver.itemstack(a) for a in args]
            elif name == "setEntityTriggers":
                e["triggers"]["entities"] = [parse_string_literal(a) for a in args]
            elif name == "setAspectTriggers":
                e["triggers"]["aspects"] = [r for r in (resolver.aspect_ref(a) for a in args) if r]
            elif name == "setComplexity":
                e["complexity"] = int(args[0])
            elif name in ("setSpecial", "setSecondary", "setRound", "setStub", "setVirtual",
                          "setConcealed", "setHidden", "setLost", "setAutoUnlock"):
                flag = {"setSpecial": "special", "setSecondary": "secondary", "setRound": "round",
                        "setStub": "stub", "setVirtual": "virtual", "setConcealed": "concealed",
                        "setHidden": "hidden", "setLost": "lost",
                        "setAutoUnlock": "autoUnlock"}[name]
                e["flags"][flag] = True
            else:
                problems.append(f"{e['key']}: unhandled chained call {name}")

        name_key, text_key = e["nameKey"], e["textKey"]
        e["nameEnUs"] = resolver.lang.get(name_key)
        e["textEnUs"] = resolver.lang.get(text_key)
        if e["nameEnUs"] is None:
            notes.append(f"no original lang name for entry {e['key']} (UNKNOWN display name)")
        if e["textEnUs"] is None:
            notes.append(f"no original lang text for entry {e['key']} (UNKNOWN text)")
        entries.append(e)
    return entries


# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------

def main():
    for p in (SRC_RESEARCH, SRC_RECIPES, SRC_ITEMS, SRC_BLOCKS, SRC_ASPECTS, LANG):
        if not p.is_file():
            sys.exit(f"missing reference file: {p}")
    raw = SRC_RESEARCH.read_text(encoding="utf-8", errors="replace")
    src = strip_comments(raw)

    resolver = Resolver()
    categories = parse_categories(src, resolver)
    list_defs = parse_recipe_lists(src, resolver)
    warp = parse_warp(src)

    # First pass: collect entry keys so TEXT_CONCEALED references can be checked.
    pre = re.findall(r"new ResearchItem\(\s*\"([^\"]+)\"", src)
    pre += re.findall(r'new ResearchItem\("([^"]+)", "[A-Z_]+"\)\.', src)
    KNOWN_ENTRY_KEYS.update(pre)

    entries = parse_entries(src, resolver, list_defs)
    for e in entries:
        if e["key"] in warp:
            e["warp"] = warp[e["key"]]

    # sanity: every entry key parsed from the constructor regex must appear once
    dupes = {k for k in (e["key"] for e in entries) if [x["key"] for x in entries].count(k) > 1}
    if dupes:
        problems.append(f"duplicate entry keys: {sorted(dupes)}")

    # graph checks (mirrored by the Java registry tests)
    keys = {e["key"] for e in entries}
    for e in entries:
        for p in e["parents"] + e["parentsHidden"] + e["siblings"]:
            if p not in keys:
                problems.append(f"{e['key']}: references unknown research key {p}")
        for pg in e["pages"]:
            if pg.get("requires") and pg["requires"] not in keys:
                problems.append(f"{e['key']}: page requires unknown key {pg['requires']}")
    # cycle detection over parents+parentsHidden
    adj = {e["key"]: [p for p in e["parents"] + e["parentsHidden"] if p in keys] for e in entries}
    state, cycles = {}, []

    def dfs(u, stack):
        state[u] = 1
        for v in adj.get(u, []):
            if state.get(v) == 1:
                cycles.append(stack + [v])
            elif state.get(v, 0) == 0:
                dfs(v, stack + [v])
        state[u] = 2

    for k in keys:
        if state.get(k, 0) == 0:
            dfs(k, [k])
    if cycles:
        problems.append(f"cycle in research graph: {cycles[0]}")

    # position collisions (the original addResearch drops non-virtual overlaps)
    placed = {}
    for e in entries:
        if e["flags"].get("virtual"):
            continue
        pos = (e["category"], e["column"], e["row"])
        if pos in placed:
            problems.append(f"position collision {pos}: {placed[pos]} vs {e['key']}")
        placed[pos] = e["key"]

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    cats_doc = {
        "formatVersion": 1,
        "source": SOURCE_NOTE,
        "categories": categories,
    }
    entries_doc = {
        "formatVersion": 1,
        "source": SOURCE_NOTE,
        "entryCount": len(entries),
        "entries": entries,
    }
    (OUT_DIR / "categories.json").write_text(
        json.dumps(cats_doc, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (OUT_DIR / "entries.json").write_text(
        json.dumps(entries_doc, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"categories: {len(categories)}")
    print(f"entries: {len(entries)}")
    print(f"pages: {sum(len(e['pages']) for e in entries)}")
    print(f"recipe lists resolved: { {name: len(keys) for _, name, keys in list_defs} }")
    print(f"warp entries: {len(warp)}")
    print(f"entries without pages: {[e['key'] for e in entries if not e['pages']]}")
    print(f"complexity clamped: {[ (e['key'], e.get('complexityRaw'), e['complexity']) for e in entries if 'complexityRaw' in e ]}")
    print(f"unknown display names: {[e['key'] for e in entries if e['nameEnUs'] is None]}")
    print(f"unknown texts: {[e['key'] for e in entries if e['textEnUs'] is None]}")
    print()
    if notes:
        print("NOTES:")
        for n in sorted(set(notes)):
            print("  -", n)
    if problems:
        print("PROBLEMS:")
        for p in sorted(set(problems)):
            print("  -", p)
        sys.exit(1)
    print("no extraction problems")


if __name__ == "__main__":
    main()
