# Decompilation Notes — workflow and limits

## Tooling actually used

- **REA plugin** (Hermes `rea`, `rea-agents@6.4.0` MCP): archive validation
  and content-addressed artifact inventory (`open_binary`,
  `inspect_artifact`). Verified limitation (documented by the sibling
  project's TL-002 run): REA's archive route does **not** decompile Java
  `.class` files into source for these targets. Claims about decompiled
  source are never attributed to REA.
- **Vineflower 1.11.1** (`~/opt/tools/vineflower-1.11.1.jar`, Apache-2.0)
  under JDK 21: full Java decompilation of both JARs.
  - Thaumcraft: `-e` (inner-class merging) → ~859 Java files.
  - Baubles: 26 Java files.
- **javap** for bytecode-level checks where decompiler output is ambiguous.
- **Original API sources** (MIT): `../reference/api-repos/thaumcraft-api`
  @ `ced30ed35097d53cdb2a843d705477e52896e54b` (branch 1.7.10) and
  `../reference/api-repos/baubles-api` @ `2ad0bd6ee34752ab0a81562ed59803ae804d9fec`
  (branch 1.7.10).

## Reproduce

```bash
~/opt/jdk21/bin/java -jar ~/opt/tools/vineflower-1.11.1.jar -e \
  "/home/aaron/minecraft mods/Thaumcraft-1.7.10-4.2.3.5.jar" \
  ../reference/decompiled/thaumcraft
~/opt/jdk21/bin/java -jar ~/opt/tools/vineflower-1.11.1.jar \
  "/home/aaron/minecraft mods/Baubles-1.7.10-1.0.1.10.jar" \
  ../reference/decompiled/baubles
```

## Evidence discipline

- Decompiled output is an interpretation of bytecode; consequential claims are
  cross-checked against resource files (JSON/lang/config/TC's own
  `mcmod.info`), multiple classes, or javap.
- Decompiled sources and extracted assets stay outside the repository
  (All Rights Reserved). Docs cite class+method names, not pasted source.
- Neither original JAR is ever executed.

## Sibling-project RE corpus (reusable analysis, re-verify before relying)

`~/minecraft mods/thaumaturgical-legacy/docs/reverse-engineering/` contains
verified inventories (THAUMCRAFT_INVENTORY, CLASS_ARCHITECTURE,
MECHANICS_CATALOG, CORE_MECHANICS, BAUBLES_*, DEPENDENCY_GRAPH,
INTERACTION_ANALYSIS). This project re-derives its own inventories from the
decompiled tree (they must cover content the clean-room prototype ignored),
but those documents are a valuable cross-check.
