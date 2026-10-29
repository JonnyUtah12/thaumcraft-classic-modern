#!/usr/bin/env python3
"""Asset path contract for a built Thaumcraft jar (TC-M7-01-s-a).

Every block and item in the installed asset-included jar rendered the
magenta/black missing-texture icon: 1.21.1 resolves the model sprite
`thaumcraft:block/amberore` at `assets/thaumcraft/textures/block/amberore.png`
(SINGULAR — 1.14 dropped the 1.7.10 plural directories), while the asset
overlay published the original tree's `textures/blocks/` + `textures/items/`,
so every lookup missed.

This script asserts the published layout and every reference that reaches it.
It runs in CI twice: on the code-only jar the repository ships (the repo's own
committed assets must satisfy it) and on the overlay jar built from a
plural-layout fixture tree (scripts/make_asset_overlay_fixture.py).

Modes
  default (overlay jar)      every gate below runs against the jar contents.
  --code-only                the repository commits NO textures (the original
                             tree is All Rights Reserved, D-004), so the
                             gates that measure published textures are
                             reported as `skip:` and a reference that is not
                             in the jar is accepted only when
                             docs/asset-mapping.csv declares it, i.e. an
                             overlay build supplies it at exactly that path.
                             Any other dangling reference is a FAIL: no build
                             will ever render it.

Gates (one `ok:`/`FAIL:`/`skip:` line each)
  1 singular-texture-dirs            textures/block/*.png > 0 and
                                     textures/item/*.png > 0.
  2 no-legacy-plural-texture-dirs    zero entries under textures/blocks/ and
                                     textures/items/.
  3 modern-reference-layout          every thaumcraft model/texture reference
                                     in blockstates/*.json and models/**/*.json
                                     uses block/ or item/, never blocks/ or
                                     items/.
  4 references-resolve               blockstate -> model -> parent chain ->
                                     textures resolves; every thaumcraft model
                                     resolves to a models/<path>.json and every
                                     thaumcraft texture to a textures/<path>.png
                                     inside the jar (code-only: present OR
                                     declared in docs/asset-mapping.csv).
  5 csv-mapping-targets-present      every docs/asset-mapping.csv target under
                                     textures/ is an entry in the jar
                                     (overlay jar only).

Exit 0 when no gate FAILs.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import zipfile

NAMESPACE = "thaumcraft"
TEXTURE_ROOT = "assets/thaumcraft/textures"
MODEL_ROOT = "assets/thaumcraft/models"
# 1.14 renamed these; a jar entry under a plural name is the defect's signature.
LEGACY_TEXTURE_DIRS = ("blocks", "items")
MODERN_TEXTURE_DIRS = ("block", "item")


def parse_location(ref, default_namespace=NAMESPACE):
    """`thaumcraft:block/foo` / `block/foo` / `mymod:pack:name` -> (ns, path)."""
    if ":" in ref:
        ns, path = ref.split(":", 1)
    else:
        ns, path = default_namespace, ref
    return ns, path


def texture_entry(ns, path):
    return f"assets/{ns}/textures/{path}.png"


def model_entry(ns, path):
    return f"assets/{ns}/models/{path}.json"


def walk_json(node, texture_refs, model_refs, parents):
    """Collect texture + model locations out of a blockstate/model document.

    `#base`-style values are local aliases resolved inside the same document's
    `textures` map, so they are skipped; a face alias that the document does not
    define is reported through `texture_refs` as an empty path for gate 4.
    """
    if isinstance(node, dict):
        textures = node.get("textures")
        aliases = set()
        if isinstance(textures, dict):
            for value in textures.values():
                if isinstance(value, str):
                    if value.startswith("#"):
                        aliases.add(value[1:])
                    else:
                        texture_refs.add(value)
        for key, value in node.items():
            if key == "textures":
                continue
            if key in ("parent", "model"):
                if isinstance(value, str):
                    model_refs.add(value)
            elif key == "elements" and isinstance(value, list):
                for element in value:
                    faces = element.get("faces") if isinstance(element, dict) else None
                    if not isinstance(faces, dict):
                        continue
                    for face in faces.values():
                        tex = face.get("texture") if isinstance(face, dict) else None
                        if isinstance(tex, str) and tex.startswith("#"):
                            if tex[1:] not in aliases:
                                texture_refs.add("")  # dangling local alias
                        elif isinstance(tex, str):
                            texture_refs.add(tex)
            else:
                walk_json(value, texture_refs, model_refs, parents)
    elif isinstance(node, list):
        for item in node:
            walk_json(item, texture_refs, model_refs, parents)
    elif isinstance(node, str) and parents == "root_model_string":
        model_refs.add(node)


def read_csv_declared_targets(mapping_csv):
    """new_asset -> original_asset for every row that publishes a texture file."""
    declared = {}
    with open(mapping_csv, newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            target = (row.get("new_asset") or "").strip()
            if target.startswith("textures/"):
                declared[target] = (row.get("original_asset") or "").strip()
    return declared


SUMMARY = "Asset path contract for a built Thaumcraft jar (TC-M7-01-s-a)."


def main(argv=None):
    parser = argparse.ArgumentParser(description=SUMMARY)
    parser.add_argument("jar", help="built mod jar to inspect")
    parser.add_argument("--code-only", action="store_true",
                        help="the jar carries no overlay assets (public artifact policy)")
    parser.add_argument("--asset-mapping", default="docs/asset-mapping.csv",
                        help="docs/asset-mapping.csv, the published-path contract")
    args = parser.parse_args(argv)

    if not os.path.isfile(args.jar):
        print(f"FAIL: jar-not-found — {args.jar}")
        return 1

    results = []

    def gate(name, ok, detail, skipped=False):
        if skipped:
            results.append(("skip", name, detail))
        else:
            results.append(("ok" if ok else "FAIL", name, detail))
        return ok

    with zipfile.ZipFile(args.jar) as archive:
        entries = set(archive.namelist())
        # json.load needs the bytes; keep the documents we may traverse
        documents = {name: archive.read(name) for name in archive.namelist()
                     if name.endswith(".json") and name.startswith("assets/")}

    def is_annotated(name):
        return name.endswith(".png")

    singular = {}
    for sub in MODERN_TEXTURE_DIRS:
        singular[sub] = sorted(n for n in entries
                               if n.startswith(f"{TEXTURE_ROOT}/{sub}/")
                               and is_annotated(n))
    legacy = {}
    for sub in LEGACY_TEXTURE_DIRS:
        legacy[sub] = sorted(n for n in entries
                             if n.startswith(f"{TEXTURE_ROOT}/{sub}/"))

    # gate 1
    if args.code_only:
        gate("singular-texture-dirs", True,
             "code-only jar: the repository commits no textures (D-004); the "
             "overlay build is gated by the same script without --code-only",
             skipped=True)
    else:
        blocks, items = len(singular["block"]), len(singular["item"])
        gate("singular-texture-dirs", blocks > 0 and items > 0,
             f"textures/block/*.png = {blocks}, textures/item/*.png = {items}")

    # gate 2
    legacy_total = sum(len(v) for v in legacy.values())
    gate("no-legacy-plural-texture-dirs", legacy_total == 0,
         "textures/blocks/ = %d, textures/items/ = %d entries (1.7.10 layout, "
         "1.21.1 never resolves it)" % (len(legacy["blocks"]), len(legacy["items"])))

    # the committed model/blockstate documents in this jar
    json_docs = sorted(n for n in entries
                       if n.endswith(".json")
                       and (n.startswith("assets/thaumcraft/blockstates/")
                            or n.startswith(f"{MODEL_ROOT}/")))
    gate("model-documents-found", len(json_docs) > 0,
         f"{len(json_docs)} blockstates/models json documents in the jar")

    texture_refs = set()
    model_refs = set()
    broken = []
    for name in json_docs:
        try:
            document = json.loads(documents[name].decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            broken.append(f"{name}: {exc}")
            continue
        walk_json(document, texture_refs, model_refs, None)

    # gate 3 — reference layout
    plural_refs = sorted(r for r in sorted(texture_refs | model_refs)
                         if parse_location(r)[0] == NAMESPACE
                         and parse_location(r)[1].split("/")[0] in LEGACY_TEXTURE_DIRS)
    gate("modern-reference-layout", not plural_refs and not broken,
         f"{len(texture_refs)} texture + {len(model_refs)} model references over "
         f"{len(json_docs)} documents; plural-layout references = {len(plural_refs)}"
         + (f" (e.g. {plural_refs[:3]})" if plural_refs else "")
         + (f"; unparsable documents: {broken[:3]}" if broken else ""))

    # gate 4 — resolution, transitively through parent chains
    declared = read_csv_declared_targets(args.asset_mapping) \
        if os.path.isfile(args.asset_mapping) else None

    def accepted(entry):
        """A thaumcraft asset entry the jar resolves, in this build mode."""
        if entry in entries:
            return True
        if args.code_only and declared is not None:
            rel = entry[len("assets/thaumcraft/"):]
            if rel in declared:
                return True  # overlay build publishes it at exactly this path
        return False

    def resolve_texture(ref):
        ns, path = parse_location(ref)
        if ns != NAMESPACE:
            return "vanilla"
        return texture_entry(ns, path)

    missing_models = []
    missing_textures = []
    vanilla_skipped = 0
    seen_models = set()
    queue = sorted(model_refs)
    while queue:
        ref = queue.pop()
        entry = model_entry(*parse_location(ref))
        if entry in seen_models:
            continue
        seen_models.add(entry)
        ns, _ = parse_location(ref)
        if ns != NAMESPACE:
            vanilla_skipped += 1
            continue
        if entry not in entries:
            if not accepted(entry):
                missing_models.append(ref)
            continue
        document = json.loads(documents[entry].decode("utf-8"))
        local_textures, parent_refs = set(), set()
        walk_json(document, local_textures, parent_refs, None)
        for texture in local_textures:
            resolved = resolve_texture(texture)
            if resolved == "vanilla":
                vanilla_skipped += 1
            elif not accepted(resolved):
                missing_textures.append(texture)
        queue.extend(parent_refs)

    # textures referenced directly by blockstate documents (and any model doc
    # reached above) are covered; blockstates carry models only.
    for texture in sorted(texture_refs):
        resolved = resolve_texture(texture)
        if resolved == "vanilla":
            vanilla_skipped += 1
        elif not accepted(resolved):
            missing_textures.append(texture)

    detail = (f"{len(seen_models)} thaumcraft models walked, "
              f"{len(missing_models)} unresolved, "
              f"{len(set(missing_textures))} unresolved textures"
              f" (non-thaumcraft references skipped: {vanilla_skipped})")
    if missing_textures:
        detail += f"; unresolved texture refs: {sorted(set(missing_textures))[:5]}"
    if missing_models:
        detail += f"; unresolved model refs: {sorted(set(missing_models))[:5]}"
    if args.code_only:
        detail += "; code-only: absent refs accepted only when " \
                  "docs/asset-mapping.csv declares them"
    gate("references-resolve", not missing_models and not missing_textures, detail)

    # gate 5 — the published-path contract of docs/asset-mapping.csv
    if args.code_only:
        gate("csv-mapping-targets-present", True,
             "code-only jar carries no overlay assets; checked on the overlay jar",
             skipped=True)
    elif declared is None:
        gate("csv-mapping-targets-present", False,
             f"asset mapping not found: {args.asset_mapping}")
    else:
        absent = sorted(t for t in declared
                        if f"assets/thaumcraft/{t}" not in entries)
        gate("csv-mapping-targets-present", not absent,
             f"{len(declared)} texture targets in docs/asset-mapping.csv, "
             f"{len(absent)} missing from the jar"
             + (f": {absent[:5]}" if absent else ""))

    failed = [name for status, name, _ in results if status == "FAIL"]
    for status, name, detail in results:
        print(f"{status}: {name} — {detail}")
    print(f"{'FAIL' if failed else 'ok'}: asset path contract "
          f"{'FAILED: ' + ', '.join(failed) if failed else 'green'} "
          f"({args.jar}, mode={'code-only' if args.code_only else 'overlay'})")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
