#!/usr/bin/env python3
"""Regenerate the per-variant block-name keys of src/main/resources/assets/
thaumcraft/lang/en_us.json from the shipped declaration.

TC-M7-01b (t_f40bbc0d). The 1.7.10 mega-blocks carried one name per metadata
value in `tile.<id>.<meta>.name`; 1.21.1 has no per-state block display name,
so the port ships the same strings as `block.thaumcraft.<id>.<meta>` keys and
`VariantBlockItem` resolves them from the stack's `variant` value.

Single sources of truth:
  * WHICH (block, meta) pairs are named:
    src/main/java/.../blocks/BlockVariantNames.java (the shipped declaration,
    parsed here — it mirrors the original key set verbatim, sparse metas
    included).
  * The strings: `../reference/assets/thaumcraft/lang/en_US.lang`, the local
    reference tree extracted from the user's own JAR. Original lang is NOT
    committed to this repository (docs/licensing-and-attribution.md); only the
    derived keys land in en_us.json, exactly as TC-LOC-s (t_a2a74726) shipped
    the item names.

The same normalization LocalizationFileTest applies when it compares the two
files: legacy section-sign colour codes stripped, the `%TYPE` token turned into
a real `%s` argument, surrounding whitespace trimmed.

Usage: python3 scripts/build_variant_lang.py [--check]
  --check  compare the shipped file with the regenerated one and exit 1 on drift
           (the unit tests already pin the wording in both directions; this is
           the cheap regeneration guard).
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DECLARATION = (ROOT / "src/main/java/com/jonnyutah12/thaumcraftclassic"
               / "blocks/BlockVariantNames.java")
ORIGINAL_LANG = ROOT.parent / "reference/assets/thaumcraft/lang/en_US.lang"
SHIPPED = ROOT / "src/main/resources/assets/thaumcraft/lang/en_us.json"

PUT_RANGE = re.compile(r'put\(table,\s*"([a-z0-9_]+)",\s*"([A-Za-z0-9_]+)",\s*range\((\d+),\s*(\d+)\)\)')
PUT_ARRAY = re.compile(r'put\(table,\s*"([a-z0-9_]+)",\s*"([A-Za-z0-9_]+)",\s*new int\[\]\s*\{([^}]*)\}\)', re.S)
PUT_NAMED = re.compile(
    r'putNamed\(table,\s*"([a-z0-9_]+)",\s*"([A-Za-z0-9_]+)",\s*'
    r'new String\[\]\s*\{([^}]*)\}\)', re.S)
LEGACY = re.compile(r'^tile\.([A-Za-z0-9_]+)\.(.+)\.name=(.*)$')


def normalize(value: str) -> str:
    """The port's normalization of an original 1.7 lang string."""
    value = re.sub("\u00a7[0-9a-fk-orA-FK-OR]", "", value)
    value = value.replace("%TYPE", "%s")
    return value.strip()


def declared() -> list[tuple[str, int, str]]:
    """(block id path, meta, legacy lang key) triples, in declaration order."""
    text = DECLARATION.read_text(encoding="utf-8")
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)          # block comments
    text = re.sub(r"//[^\n]*", "", text)                        # line comments
    pairs: list[tuple[str, int, str]] = []
    for path, legacy, lo, hi in PUT_RANGE.findall(text):
        for meta in range(int(lo), int(hi) + 1):
            pairs.append((path, meta, f"tile.{legacy}.{meta}.name"))
    for path, legacy, metas in PUT_ARRAY.findall(text):
        for meta in (int(m) for m in metas.replace("\n", " ").split(",") if m.strip()):
            pairs.append((path, meta, f"tile.{legacy}.{meta}.name"))
    for path, legacy, keys in PUT_NAMED.findall(text):
        for meta, raw in enumerate(keys.replace('"', "").replace("\n", " ").split(",")):
            pairs.append((path, meta, f"tile.{legacy}.{raw.strip()}.name"))
    return pairs


def original_strings() -> dict[str, str]:
    by_key: dict[str, str] = {}
    for line in ORIGINAL_LANG.read_text(encoding="utf-8").splitlines():
        match = LEGACY.match(line.strip())
        if match:
            by_key[f"tile.{match.group(1)}.{match.group(2)}.name"] = normalize(match.group(3))
    return by_key


def main() -> int:
    check = "--check" in sys.argv
    if not DECLARATION.is_file():
        print(f"declaration missing: {DECLARATION}", file=sys.stderr)
        return 1
    if not ORIGINAL_LANG.is_file():
        print(f"reference tree missing: {ORIGINAL_LANG} (CI builds without it; "
              "the shipped file is regenerated on a checkout that has the "
              "user's own reference JAR)", file=sys.stderr)
        return 1

    legacy = original_strings()
    pairs = declared()
    missing = [key for _, _, key in pairs if key not in legacy]
    if missing:
        print(f"declared pairs with no original name key: {missing}", file=sys.stderr)
        return 1

    shipped = json.loads(SHIPPED.read_text(encoding="utf-8"))
    before = {k: v for k, v in shipped.items() if k.startswith("block.thaumcraft.")}
    added = {}
    for path, meta, key in pairs:
        added[f"block.thaumcraft.{path}.{meta}"] = legacy[key]
    merged = {**shipped, **added}

    families = len({path for path, _, _ in pairs})
    old_variant_keys = len([key for key in before if re.search(r"\.\d+$", key)])
    print(f"{len(pairs)} per-variant keys across {families} mega-block families; "
          f"en_us.json {len(shipped)} -> {len(merged)} keys "
          f"(new: {len(merged) - len(shipped)}, block-variant keys already shipped: "
          f"{old_variant_keys})")

    text = json.dumps(merged, indent=2, ensure_ascii=False, sort_keys=True) + "\n"
    if check:
        if text == SHIPPED.read_text(encoding="utf-8"):
            print(f"{SHIPPED} is up to date with the declaration")
            return 0
        print(f"{SHIPPED} has drifted from BlockVariantNames.java — run "
              "python3 scripts/build_variant_lang.py", file=sys.stderr)
        return 1
    SHIPPED.write_text(text, encoding="utf-8")
    print(f"wrote {SHIPPED}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
