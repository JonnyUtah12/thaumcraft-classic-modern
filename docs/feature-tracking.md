# Feature Tracking — original feature → task → status

Authoritative parity status lives in `compatibility-matrix.md`; this file
maps each original feature area to the Kanban task that implements it.
Statuses: NOT STARTED / IN PROGRESS / PARTIAL / DONE (DONE only with test
evidence).

| Feature area (original) | Source evidence (TC 4.2.3.5) | Task | Status |
| --- | --- | --- | --- |
| Aspects (definitions, combinations, essentia) | `thaumcraft.api.aspects.Aspect`, `AspectHelper`, `common/lib` | TC-M4-01 | NOT STARTED |
| Research graph + Thaumonomicon | `thaumcraft.common.lib.research.ResearchManager`, `api.research.*`, `client.gui.*` | TC-M3-01..04 | NOT STARTED |
| Vis network, aura, nodes | `api.visnet`, `common/lib/world`, `api.nodes` | TC-M5-01..03 | NOT STARTED |
| Flux + taint | `common/taint/*`, flux in `WorldMagicData` | TC-M5-04..05 | NOT STARTED |
| Wands (cores/caps/rods), foci, casting | `common/items/wands/*`, `api.wands.*`, `common/entities/projectile/*` | TC-M6-01..03 | NOT STARTED |
| Arcane workbench | `common/crafting/*`, `client/gui/*` | TC-M4-03 | NOT STARTED |
| Infusion altar (pedestals, instability) | `common/tile/TileInfusion*`, crafting | TC-M4-04 | NOT STARTED |
| Alchemy (crucible, table) | `common/tile/TileAlchemy*` | TC-M4-05 | NOT STARTED |
| Essentia (extraction, transport, jars) | `api/damagesource`? no: `common/tile/TileEssentia*`, `TileSmelter`, tube network | TC-M4-06..07 | NOT STARTED |
| Equipment + Baubles slots | `common/items/equipment/*`, `items/baubles/*`, Baubles repo | TC-M8-01..02 | NOT STARTED |
| Enchantments, potions/effects | `common/lib/enchantment`, `api/potions` | TC-M6-05 | NOT STARTED |
| Entities + AI (elementals, golems, wisp, taint mobs) | `common/entities/*` | TC-M6-06..07 | NOT STARTED |
| World gen (ores, nodes, structures, biome effects) | `common/world/*`, worldgen event handlers | TC-M5-06 | NOT STARTED |
| Particles, beams, rendering | `client/fx/*`, `client/renderers/*` | TC-M7-01..04 | NOT STARTED |
| Sounds | `assets/thaumcraft/sounds*` | TC-M7-05 | NOT STARTED |
| Networking/sync | `common/lib/network/*` | TC-M2-03 | NOT STARTED |
| Persistence (world magic data, player knowledge) | `WorldMagicData`, `PlayerKnowledge` | TC-M2-02 | NOT STARTED |
| Config | `Thaumcraft` config class | TC-M2-04 | NOT STARTED |
| ATM10 coexistence | isolated instance tests | TC-M9-01..02 | NOT STARTED |

Foundation (board/CI/build/docs): TC-M0/TC-M1 — see CHANGELOG for progress.
