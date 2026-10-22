# Thaumcraft Classic (Unofficial)

An independent, **unofficial** high-fidelity port of **Thaumcraft 4.2.3.5**
(Minecraft 1.7.10, by Azanor) to **Minecraft 1.21.1 / NeoForge 21.1.259**.

Not affiliated with, endorsed by, or approved by Azanor. Thaumcraft and all
original content, names, and (locally-built) assets are Azanor's work.

## What this project is

A preservation-focused native port: the original mod's systems — aspects,
research and the Thaumonomicon, vis/aura/nodes, flux and taint, arcane
crafting, infusion, alchemy, essentia, wands/foci, equipment, entities,
world generation, and their original balance values — reconstructed from the
original JAR (decompiled) and the MIT-licensed Thaumcraft API, and
implemented natively on NeoForge 21.1. No dependency on the 1.7.10 runtime,
Forge, the original JARs, or Baubles 1.7.10.

Original content ids are preserved as far as the platform permits
(`Thaumcraft:foo` → `thaumcraft:foo`; lowercase is a platform requirement).

## Status

**Mid-port — core magic systems implemented; not yet playable.** The
server-side systems are in and tested against the original: the frozen
registration/id contract, configuration and networking payloads, world +
player persistence substrate, the full aspect system (48 aspects, original
essentia tables, combination and discovery rules), research semantics over
the complete 201-entry graph (no book GUI yet), vis pools + visnet +
cost rules, aura and wild-node world data, the flux system, and the
accessory provider layer (Curios bridge + built-in fallback). The content
catalog (blocks/items/machines), wands and foci, alchemy/infusion/
essentia devices, entities, GUIs, structures, and taint are not
implemented yet, so there is no playable Thaumcraft content in a shipped
JAR. The authoritative feature-by-feature parity report is
[`docs/compatibility-matrix.md`](docs/compatibility-matrix.md) and
[`docs/feature-tracking.md`](docs/feature-tracking.md); the latest full
regression pass is recorded in `docs/regression-report-*.md`. The project
is tracked on the Hermes Kanban board `thaumcraft-classic-port`.

## Requirements

- Minecraft **1.21.1** with **NeoForge 21.1.259** (exact; pinned and
  asserted by tests).
- Java 21.
- Optional: Curios for 1.21.1 (`9.5.1+1.21.1`, the ATM10 8.3 version) for
  accessory-slot UI integration. The mod loads and runs without it.

## Building

```bash
JAVA_HOME=~/opt/jdk21 ./gradlew build          # unit tests + JAR
JAVA_HOME=~/opt/jdk21 ./gradlew runGameTestServer   # in-game tests
```

The build overlays original assets from the local reference tree
(`../reference/assets/`, extracted from the user's own Thaumcraft JAR; not
committed — see `docs/licensing-and-attribution.md`). Without that tree the
build produces a code-only JAR.

## Testing

Unit layer: `./gradlew test` (pure logic: aspects, research graph, recipe
data, costs, persistence codecs). In-game layer:
`./gradlew runGameTestServer` (NeoForge gametests). ATM10 integration is
tested in the disposable instance `~/minecraft mods/atm10-test`, never in
the user's primary instance. See `docs/testing.md`.

## Documentation map

| Document | Contents |
| --- | --- |
| `docs/original-baseline.md` | Verified facts about the original artifacts |
| `docs/original-content-inventory.md` | Full content inventory (blocks/items/research/aspects/…) |
| `docs/original-system-map.md` | Which original classes implement which systems |
| `docs/original-asset-inventory.md` | Asset inventory and mapping |
| `docs/decompilation-notes.md` | REA + Vineflower workflow and limits |
| `docs/target-platform.md` | Verified target environment facts |
| `docs/porting-decisions.md` | Every adaptation, with fidelity impact |
| `docs/compatibility-matrix.md` | Feature-by-feature parity status |
| `docs/feature-tracking.md` | Feature → Kanban task mapping |
| `docs/content-mapping.csv` | Original id → new id → status |
| `docs/kanban-workflow.md` | Board layout and workflow rules |
| `docs/licensing-and-attribution.md` | License findings and policy |
| `docs/atm10-compatibility.md` | ATM10 8.3 environment and test results |
| `docs/release-process.md` | Versioning, changelog, release rules |
| `docs/testing.md` | Test strategy and stages A–G |

## License

Our code: MIT (see `LICENSE`). Thaumcraft content: Azanor, All Rights
Reserved — not redistributed in this repository.
