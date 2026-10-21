#!/usr/bin/env python3
"""Extract the complete Thaumcraft 4.2.3.5 essentia tag tables into data-driven JSON.

TC-M4-01. Reads the decompiled original (kept outside the repository at
../reference/decompiled, see docs/licensing-and-attribution.md) and writes:

  src/main/resources/data/thaumcraft/essentia/tags.json
      object tags (item + oredict) and entity tags registered by
      ConfigAspects.java, with the exact aspect amounts of the original.
  src/main/java/com/jonnyutah12/thaumcraftclassic/aspect/AspectDefinitions.java
      the 48 aspect definitions (tag, color, chatcolor, blend, components)
      from api/aspects/Aspect.java, in declaration order.

Method and decisions: docs/aspects.md. The parser is a brace/paren-balanced
scanner over the decompiled ConfigAspects.java. Every construct the file uses
is handled explicitly; anything unrecognised is reported, never guessed. Run:
python3 scripts/extract_essentia_tags.py
"""

import csv
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
REF = REPO.parent / "reference"
DECOMP = REF / "decompiled" / "thaumcraft" / "thaumcraft"
DECONFIG = DECOMP / "common" / "config"
SRC_CONFIG_ASPECTS = DECONFIG / "ConfigAspects.java"
SRC_CONFIG_ITEMS = DECONFIG / "ConfigItems.java"
SRC_CONFIG_BLOCKS = DECONFIG / "ConfigBlocks.java"
SRC_ASPECT = DECOMP / "api" / "aspects" / "Aspect.java"
MCP_FIELDS = REF / "mcp-mappings" / "fields_1.7.10_mcp_stable_11.csv"
ID_CSV = REPO / "docs" / "content-mapping.csv"

OUT_JSON = REPO / "src" / "main" / "resources" / "data" / "thaumcraft" / "essentia" / "tags.json"
OUT_JAVA = REPO / "src" / "main" / "java" / "com" / "jonnyutah12" / "thaumcraftclassic" / "aspect" / "AspectDefinitions.java"

SOURCE_NOTE = (
    "decompiled thaumcraft/common/config/ConfigAspects.java + "
    "thaumcraft/api/aspects/Aspect.java (Thaumcraft 4.2.3.5). Extracted "
    "mechanically by scripts/extract_essentia_tags.py."
)

problems = []   # hard extraction problems (unparsed constructs)
notes = []      # observations for docs/aspects.md

# 1.7.10 entity registry name -> 1.21.1 entity id. Only names actually used by
# ConfigAspects.java appear; the renames are the documented vanilla ones.
VANILLA_ENTITY_IDS = {
    "Bat": "bat", "Blaze": "blaze", "Boat": "boat", "CaveSpider": "cave_spider",
    "Chicken": "chicken", "Cow": "cow", "Creeper": "creeper",
    "EnderCrystal": "end_crystal", "EnderDragon": "ender_dragon",
    "Enderman": "enderman", "EntityHorse": "horse", "Ghast": "ghast",
    "Giant": "giant", "ItemFrame": "item_frame", "LavaSlime": "magma_cube",
    "MinecartChest": "chest_minecart", "MinecartFurnace": "furnace_minecart",
    "MinecartHopper": "hopper_minecart", "MinecartRideable": "minecart",
    "MinecartSpawner": "spawner_minecart", "MinecartTNT": "tnt_minecart",
    "MushroomCow": "mooshroom", "Ozelot": "ocelot", "Painting": "painting",
    "Pig": "pig", "PigZombie": "zombified_piglin", "Sheep": "sheep",
    "Silverfish": "silverfish", "Skeleton": "skeleton", "Slime": "slime",
    "SnowMan": "snow_golem", "Spider": "spider", "Squid": "squid",
    "Villager": "villager", "VillagerGolem": "iron_golem", "Witch": "witch",
    "WitherBoss": "wither", "Wolf": "wolf", "XPOrb": "experience_orb",
    "Zombie": "zombie",
}


# --------------------------------------------------------------------------
# Java source scanning helpers (same approach as extract_research_graph.py)
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
            i = n if j < 0 else j
        elif src.startswith("/*", i):
            j = src.find("*/", i)
            i = n if j < 0 else j + 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def match_paren(src: str, open_idx: int) -> int:
    """Return index of the ')' matching the '(' at open_idx (string-aware)."""
    depth, i, n = 0, open_idx, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                i += 2 if src[i] == "\\" else 1
        elif c == "'":
            i += 1
            while i < n and src[i] != "'":
                i += 2 if src[i] == "\\" else 1
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced parens")


def match_brace(src: str, open_idx: int) -> int:
    depth, i, n = 0, open_idx, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                i += 2 if src[i] == "\\" else 1
        elif c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced braces")


def split_args(s: str):
    """Split a top-level argument list on commas (paren/brace/string-aware)."""
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
    if cur:
        args.append("".join(cur).strip())
    return args


# --------------------------------------------------------------------------
# Reference maps
# --------------------------------------------------------------------------

def load_field_maps():
    items_src = SRC_CONFIG_ITEMS.read_text(encoding="utf-8", errors="replace")
    blocks_src = SRC_CONFIG_BLOCKS.read_text(encoding="utf-8", errors="replace")
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


def load_aspect_definitions():
    """Parse the 48 `new Aspect(...)` static initializers of Aspect.java.

    Returns a list of dicts in declaration order:
      {constant, tag, color, chatcolor, blend, components: [constants]}
    """
    src = strip_comments(SRC_ASPECT.read_text(encoding="utf-8", errors="replace"))
    defs = []
    pat = re.compile(
        r"public static final Aspect (\w+) = new Aspect\((.*?)\);", re.S)
    for m in pat.finditer(src):
        const, argstr = m.group(1), m.group(2)
        args = split_args(argstr)
        tag = json.loads(args[0])
        color = int(args[1])
        chatcolor = None
        blend = 1
        components = []
        # forms: (tag,color,chatcolor,blend) | (tag,color,Aspect[],blend)
        #        (tag,color,Aspect[])
        for a in args[2:]:
            a = a.strip()
            am = re.fullmatch(r"\"([^\"]*)\"", a)
            if am:
                chatcolor = am.group(1)
                continue
            cm = re.fullmatch(r"new Aspect\[\]\s*\{([^}]*)\}", a)
            if cm:
                components = [c.strip() for c in cm.group(1).split(",") if c.strip()]
                continue
            if re.fullmatch(r"-?\d+", a):
                blend = int(a)
                continue
            if a == "(Aspect[])null":
                continue
            problems.append(f"unparsed Aspect ctor arg for {const}: {a!r}")
        defs.append({"constant": const, "tag": tag, "color": color,
                     "chatcolor": chatcolor, "blend": blend,
                     "components": components})
    if len(defs) != 48:
        problems.append(f"expected 48 aspect definitions, found {len(defs)}")
    return defs


def load_id_csv():
    """content-mapping.csv: (kind, original_id) -> new_id, plus id sets."""
    rows, tc_ids, tc_entities = [], set(), {}
    with ID_CSV.open(encoding="utf-8") as fh:
        for line in fh:
            line = line.rstrip("\r\n")
            if not line or line.startswith("#"):
                continue
            parts = next(csv.reader([line]))
            if len(parts) < 4:
                continue
            orig, new_id, kind = parts[0], parts[1], parts[2]
            rows.append((kind, orig, new_id))
            if kind in ("item", "block"):
                tc_ids.add(new_id)
            if kind == "entity":
                tc_entities[orig] = new_id
    return rows, tc_ids, tc_entities


# --------------------------------------------------------------------------
# ConfigAspects.java parsing
# --------------------------------------------------------------------------

class ItemRef:
    def __init__(self, item, meta, srg_field=None):
        self.item = item
        self.meta = meta
        self.srg = srg_field

    def key(self):
        return (self.item, self.meta)

    def target(self):
        t = {"kind": "item", "id": self.item, "meta": self.meta}
        if self.srg:
            t["srgField"] = self.srg
        return t


def parse_item_stack(expr, maps):
    """`new ItemStack(X)` / `new ItemStack(X, 1, meta)` -> ItemRef."""
    expr = expr.strip()
    m = re.match(r"new ItemStack\((.*)\)$", expr, re.S)
    if not m:
        problems.append(f"not an ItemStack expression: {expr!r}")
        return None
    args = split_args(m.group(1))
    field = args[0].strip()
    meta = 0
    if len(args) > 2:
        raw = args[2].strip()
        if not re.fullmatch(r"-?\d+", raw):
            problems.append(f"non-integer meta in ItemStack: {expr!r}")
            return None
        meta = int(raw)
    item_fields, block_fields, vanilla = maps
    if field.startswith("ConfigItems."):
        name = item_fields.get(field.split(".", 1)[1])
        if name is None:
            problems.append(f"unmapped ConfigItems field: {field}")
            return None
        return ItemRef(f"thaumcraft:{name.lower()}", meta)
    if field.startswith("ConfigBlocks."):
        name = block_fields.get(field.split(".", 1)[1])
        if name is None:
            problems.append(f"unmapped ConfigBlocks field: {field}")
            return None
        return ItemRef(f"thaumcraft:{name.lower()}", meta)
    if field.startswith("Items.") or field.startswith("Blocks."):
        srg = field.split(".", 1)[1]
        name = vanilla.get(srg)
        if name is None:
            problems.append(f"unmapped vanilla field: {field}")
            return None
        return ItemRef(f"minecraft:{name}", meta, srg)
    problems.append(f"unresolved ItemStack field/variable: {expr!r}")
    return None


def parse_aspect_list(expr, maps, table, aspect_tags):
    """Evaluate an AspectList expression to a final {tag: amount} dict.

    Handles `new AspectList()`, inheritance `new AspectList(new ItemStack(..))`
    resolved against the table built so far (same semantics as the original at
    registration time), and the .add/.remove/.merge chains (add accumulates,
    merge keeps the max, remove deletes at <= 0).
    """
    expr = expr.strip()
    result = {}
    inherit_ref = None
    m = re.match(r"new AspectList\(\s*(new ItemStack\(.*\))?\s*\)", expr, re.S)
    if not m:
        problems.append(f"not an AspectList expression: {expr!r}")
        return result, None
    if m.group(1):
        inherit_ref = parse_item_stack(m.group(1), maps)
        if inherit_ref is not None:
            entry = table.get(inherit_ref.key())
            if entry is None:
                entry = table.get((inherit_ref.item, 32767))
            if entry is not None:
                result = dict(entry["aspects"])
    body = expr[m.end():]
    ops = re.findall(r"\.(add|remove|merge)\(Aspect\.([A-Z_0-9]+)(?:\s*,\s*(-?\d+))?\)", body)
    for op, const, amount in ops:
        tag = aspect_tags.get(const)
        if tag is None:
            problems.append(f"unknown Aspect constant: Aspect.{const}")
            continue
        if not amount:
            if op == "remove":
                result.pop(tag, None)  # single-arg remove: drop the aspect
            else:
                problems.append(f"aspect op without amount in: {expr!r}")
            continue
        amount = int(amount)
        if op in ("add", "merge"):
            if op == "add":
                result[tag] = result.get(tag, 0) + amount
            else:
                result[tag] = max(result.get(tag, 0), amount)
        else:
            na = result.get(tag, 0) - amount
            if na <= 0:
                result.pop(tag, None)
            else:
                result[tag] = na
    # verify nothing else is chained
    rest = re.sub(r"\.(add|remove|merge)\(Aspect\.[A-Z_0-9]+(?:\s*,\s*-?\d+)?\)", "", body)
    if rest.strip():
        problems.append(f"unparsed remainder in AspectList expression: {rest!r}")
    return result, inherit_ref


def parse_nbt_condition(expr):
    m = re.match(r"new ThaumcraftApi\.EntityTagsNBT\((.*)\)$", expr.strip(), re.S)
    if not m:
        problems.append(f"not an EntityTagsNBT expression: {expr!r}")
        return None
    args = split_args(m.group(1))
    name = json.loads(args[0])
    value = args[1].strip()
    vm = re.fullmatch(r"\(byte\)\s*(-?\d+)", value)
    if vm:
        return {"nbt": name, "type": "byte", "value": int(vm.group(1))}
    if re.fullmatch(r"-?\d+", value):
        return {"nbt": name, "type": "int", "value": int(value)}
    if value.startswith("tag.getTag()"):
        return None  # handled by the wisp loop expansion
    problems.append(f"unparsed EntityTagsNBT value: {value!r}")
    return None


def entity_target(name, tc_entities):
    if name.startswith("Thaumcraft."):
        orig = "Thaumcraft:" + name.split(".", 1)[1]
        new_id = tc_entities.get(orig)
        if new_id is None:
            problems.append(f"entity not in content-mapping.csv: {orig}")
            return None
        return new_id
    modern = VANILLA_ENTITY_IDS.get(name)
    if modern is None:
        problems.append(f"unmapped vanilla entity name: {name}")
        return None
    return f"minecraft:{modern}"


def extract():
    src = strip_comments(SRC_CONFIG_ASPECTS.read_text(encoding="utf-8", errors="replace"))
    maps = load_field_maps()
    aspect_defs = load_aspect_definitions()
    aspect_tags = {d["constant"]: d["tag"] for d in aspect_defs}
    _, tc_ids, tc_entities = load_id_csv()

    # Locate the three generated loops and the four compat-metal conditionals.
    loop_ranges, cond_ranges = [], []
    for m in re.finditer(r"for \(", src):
        header_close = match_paren(src, m.end() - 1)
        header = src[m.end():header_close]
        body_open = src.find("{", header_close)
        body_end = match_brace(src, body_open)
        if "int i = 0; i < 16" in header and "dyes" in src[body_open:body_end]:
            loop_ranges.append((m.start(), body_end, "dyes"))
        elif "Aspect tag : Aspect.aspects.values()" in header:
            loop_ranges.append((m.start(), body_end, "wisp"))
        elif "var4 = 1; var4 <= 32767" in header or "var7 : lhm.values()" in header:
            loop_ranges.append((m.start(), body_end, "potion"))
        else:
            problems.append(f"unhandled for-loop in ConfigAspects: {header!r}")
    for m in re.finditer(r"if \(Config\.(found\w+)\)", src):
        body_open = src.find("{", m.end())
        body_end = match_brace(src, body_open)
        cond_ranges.append((m.start(), body_end, m.group(1)))

    def in_range(ranges, pos):
        for lo, hi, tag in ranges:
            if lo <= pos <= hi:
                return tag
        return None

    item_tags, entity_tags = [], []
    table = {}  # (item, meta) -> entry, in registration order (inheritance)

    def add_item_entry(ref, aspects, complex_flag, condition, inherits):
        entry = {"target": ref.target(), "aspects": aspects}
        if complex_flag:
            entry["complex"] = True
        if condition:
            entry["condition"] = condition
        if inherits is not None:
            entry["inherits"] = {"id": inherits.item, "meta": inherits.meta}
        item_tags.append(entry)
        table[ref.key()] = entry

    # Generic scan: every ThaumcraftApi.register* call outside the loops.
    for m in re.finditer(r"ThaumcraftApi\.(registerObjectTag|registerComplexObjectTag|registerEntityTag)\(", src):
        if in_range(loop_ranges, m.start()):
            continue
        close = match_paren(src, m.end() - 1)
        args = split_args(src[m.end():close])
        kind, cond = m.group(1), in_range(cond_ranges, m.start())
        if kind == "registerEntityTag":
            name = json.loads(args[0])
            aspects, _ = parse_aspect_list(args[1], maps, table, aspect_tags)
            cond_nbt = parse_nbt_condition(args[2]) if len(args) > 2 else None
            target = entity_target(name, tc_entities)
            if target is None:
                continue
            entry = {"entity": target, "originalName": name, "aspects": aspects}
            if cond_nbt:
                entry["condition"] = cond_nbt
            entity_tags.append(entry)
            continue

        # object tags: (String oredict | ItemStack[, int[] metas], AspectList)
        if len(args) == 2:
            metas, rest = None, args
        elif len(args) == 3 and args[1].startswith("new int[]{"):
            metas = [int(x) for x in re.findall(r"-?\d+", args[1])]
            rest = [args[0], args[2]]
        else:
            problems.append(f"unhandled registerObjectTag arity: {args!r}")
            continue

        aspects, inherits = parse_aspect_list(rest[1], maps, table, aspect_tags)
        first = rest[0].strip()
        if first.startswith('"'):
            name = json.loads(first)
            entry = {"target": {"kind": "oredict", "name": name}, "aspects": aspects}
            if kind == "registerComplexObjectTag":
                entry["complex"] = True
            if cond:
                entry["condition"] = cond
            item_tags.append(entry)
            continue
        ref = parse_item_stack(first, maps)
        if ref is None:
            continue
        for meta in (metas if metas is not None else [ref.meta]):
            add_item_entry(ItemRef(ref.item, meta, ref.srg), dict(aspects),
                           kind == "registerComplexObjectTag", cond, inherits)

    # --- dyes loop: 16 oredict names from the dyes[] array literal ---------
    dm = re.search(r"public static String\[\] dyes = new String\[\]\s*\{(.*?)\}", src, re.S)
    dyes = [json.loads(x.strip().rstrip(",")) for x in dm.group(1).split("\n") if x.strip()]
    if len(dyes) != 16:
        problems.append(f"dyes array has {len(dyes)} entries")
    for name in dyes:
        item_tags.append({"target": {"kind": "oredict", "name": name},
                          "aspects": {"sensus": 1}})
    notes.append(f"dyes loop expanded to 16 oredict entries ({dyes[0]}..{dyes[-1]}).")

    # --- wisp loop: one entry per aspect, in Aspect.java declaration order --
    for d in aspect_defs:
        wisp_aspects = {}
        for tag, amt in ((d["tag"], 2), ("praecantatio", 1), ("aer", 1)):
            wisp_aspects[tag] = wisp_aspects.get(tag, 0) + amt  # add() accumulates
        entity_tags.append({
            "entity": entity_target("Thaumcraft.Wisp", tc_entities),
            "originalName": "Thaumcraft.Wisp",
            "condition": {"nbt": "Type", "type": "string", "value": d["tag"]},
            "aspects": wisp_aspects,
        })
    notes.append(f"wisp loop expanded to {len(aspect_defs)} entity entries keyed by NBT Type.")

    # --- potion loop: every valid PotionHelper meta inherited the base ------
    # potion tags: the base registration (Items.field_151068_bn) is in the
    # table; all meta variants inherited exactly those tags, so one wildcard
    # entry reproduces the original behaviour.
    base = None
    for entry in item_tags:
        t = entry["target"]
        if t.get("kind") == "item" and t.get("id") == "minecraft:potionitem" and t.get("meta") == 0:
            base = entry
            break
    if base is None:
        problems.append("base potion item tag not found for the potion-meta loop")
    else:
        item_tags.append({
            "target": {"kind": "item", "id": "minecraft:potionitem", "meta": 32767,
                       "srgField": base["target"].get("srgField")},
            "aspects": dict(base["aspects"]),
            "inherits": {"id": "minecraft:potionitem", "meta": 0},
            "note": "collapses the original per-PotionHelper-meta registrations "
                    "(every valid meta inherited the base potion tags)",
        })
    notes.append("potion-meta loop collapsed to one wildcard entry (all variants "
                 "inherited the base potion tags; 1.21.1 potions have no meta).")

    return aspect_defs, item_tags, entity_tags, tc_ids


# --------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------

def emit_java(aspect_defs):
    lines = [
        "// Generated by scripts/extract_essentia_tags.py from the decompiled",
        "// original api/aspects/Aspect.java - do not edit by hand.",
        "// Provenance and semantics: docs/aspects.md.",
        "package com.jonnyutah12.thaumcraftclassic.aspect;",
        "",
        "import java.util.List;",
        "",
        "/**",
        " * The 48 original Thaumcraft 4.2.3.5 aspect definitions, in the original",
        " * declaration order (primal first; every compound's components are",
        " * declared before it). Minecraft-free on purpose: this is the single",
        " * source of truth for the thaumcraft:aspect registry, the combination",
        " * table and the docs tables (docs/aspects.md).",
        " */",
        "public final class AspectDefinitions {",
        "",
        "    /** One aspect: tag, display color, chat color code (null for",
        "     * compounds), blend mode, and component tags (empty = primal). */",
        "    public record Definition(String tag, int color, String chatcolor, int blend,",
        "                             List<String> components) {",
        "        public boolean primal() {",
        "            return components.isEmpty();",
        "        }",
        "    }",
        "",
        "    private static final List<Definition> DEFINITIONS = List.of(",
    ]
    for idx, d in enumerate(aspect_defs):
        comps = ", ".join(f'"{c}"' for c in
                          [cc["tag"] for cc in
                           [next(x for x in aspect_defs if x["constant"] == k)
                            for k in d["components"]]])
        chat = f'"{d["chatcolor"]}"' if d["chatcolor"] is not None else "null"
        comma = "," if idx != len(aspect_defs) - 1 else ""
        lines.append(f'        new Definition("{d["tag"]}", {d["color"]}, {chat}, '
                     f'{d["blend"]}, List.of({comps})){comma}')
    lines += [
        "    );",
        "",
        "    public static List<Definition> all() {",
        "        return DEFINITIONS;",
        "    }",
        "",
        "    private AspectDefinitions() {",
        "    }",
        "}",
        "",
    ]
    OUT_JAVA.parent.mkdir(parents=True, exist_ok=True)
    OUT_JAVA.write_text("\n".join(lines), encoding="utf-8")


def main():
    aspect_defs, item_tags, entity_tags, tc_ids = extract()

    # Validation: every TC item id must be a CSV row; every aspect tag valid.
    valid_tags = {d["tag"] for d in aspect_defs}
    for entry in item_tags:
        t = entry["target"]
        if t.get("kind") == "item" and t["id"].startswith("thaumcraft:") \
                and t["id"] not in tc_ids:
            problems.append(f"TC item id not in content-mapping.csv: {t['id']}")
        for tag in entry["aspects"]:
            if tag not in valid_tags:
                problems.append(f"unknown aspect tag in table: {tag}")
    for entry in entity_tags:
        if entry["entity"] is None:
            problems.append(f"unresolved entity: {entry['originalName']}")
        for tag in entry["aspects"]:
            if tag not in valid_tags:
                problems.append(f"unknown aspect tag on entity: {tag}")

    # Dependency order check: components must precede their compound.
    seen = set()
    for d in aspect_defs:
        for k in d["components"]:
            ctag = next(x["tag"] for x in aspect_defs if x["constant"] == k)
            if ctag not in seen:
                problems.append(f"component {ctag} declared after {d['tag']}")
        seen.add(d["tag"])

    payload = {
        "formatVersion": 1,
        "source": SOURCE_NOTE,
        "notes": notes,
        "counts": {
            "aspects": len(aspect_defs),
            "itemTags": sum(1 for e in item_tags if e["target"]["kind"] == "item"),
            "oredictTags": sum(1 for e in item_tags if e["target"]["kind"] == "oredict"),
            "entityTags": len(entity_tags),
        },
        "itemTags": item_tags,
        "entityTags": entity_tags,
    }

    emit_java(aspect_defs)
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
                        encoding="utf-8")

    c = payload["counts"]
    print(f"aspects={c['aspects']} itemTags={c['itemTags']} "
          f"oredictTags={c['oredictTags']} entityTags={c['entityTags']}")
    if problems:
        print(f"\n{len(problems)} PROBLEM(S):")
        for p in problems:
            print(" -", p)
        sys.exit(1)
    print("clean extraction")


if __name__ == "__main__":
    main()
