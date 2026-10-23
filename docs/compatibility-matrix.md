# Compatibility Matrix — feature-by-feature parity

Statuses (per the project brief):
**IT** implemented+tested · **IU** implemented, not fully tested ·
**PARTIAL** partially implemented · **NI** not yet implemented ·
**BL** blocked by a technical limitation · **AD** intentionally adapted
(reason + fidelity impact in `porting-decisions.md`).

Updated by the same change that alters a feature. No row may move to IT/IU
without test evidence linked in the Kanban task.

Full regression pass 2026-10-22 at c44b219: 216 unit tests + 39 game
tests green in both the Curios and no-Curios configurations
(see `docs/regression-report-2026-10-22.md`). Statuses below describe the
committed state of `main`; work verified but not yet integrated is called
out explicitly.

| System | Required verification | Status |
| --- | --- | --- |
| Build/load foundation | Gradle build, mod loads in game-test server, metadata correct | IT (unit + smoke gametest, CI) |
| Aspects | Original 25+7+… definitions, combination rules, discovery | IT (48/48 definitions 1:1 vs Aspect.java, data-driven essentia tables 287 item / 56 oredict / 116 entity extracted 1:1 from ConfigAspects, TC4 combination/reduce/cull + discovery-on-knowledge rules — 4 unit suites + 5 game tests, TC-M4-01) |
| Research | Full original graph, prerequisites, unlock triggers, persistence | IT (graph 1:1 + progression/triggers/points/warp server-authoritative; 87 unit + 18 game tests, TC-M3-01/02; Thaumonomicon GUI is TC-M3-03) |
| Thaumonomicon | Pages, minigames, navigation, text | NI (TC-M3-03/04) |
| Vis / aura / nodes | Generation, capacity, depletion, wand interaction, persistence | PARTIAL (vis pool data component + centi-vis all-or-nothing payment + cost modifier chain + visnet node-conduit-device model + ambient vis reader IT — 44 unit + game tests, TC-M4-02; node type/modifier registries, 31-row biome aura table, seeded wild-node generation 1:1 with createRandomNodeAt, per-chunk aura records IT — 23 unit + 6 game tests, TC-M5-01; node behavior/depletion TC-M5-02 in progress; wand interaction TC-M6-01 NI) |
| Flux / taint | Spread, transformation, biome effects | PARTIAL (flux core IT: per-chunk flux state, generation from magic use, original-derived decay/diffusion, effect thresholds pinned — 44 unit + 3 game tests, TC-M5-03; taint transformation/biome effects NI, TC-M5-04 in progress) |
| Arcane crafting | Workbench mechanics, vis costs, research gating | NI (cost rules exist under Vis row; workbench TC-M4-03) |
| Infusion | Pedestals, costs, instability, effects | NI (TC-M4-04) |
| Alchemy | Crucible/table state machines, recipes | NI (TC-M4-05) |
| Essentia | Extraction, transport, matching, containers | NI (essentia *tables* are IT under Aspects; smelter/jars/tubes TC-M4-06/07) |
| Wands & foci | Cores/caps, capacities, per-focus behavior, targeting | NI (TC-M6-01..03; cost math guarded under Vis row) |
| Equipment / accessories | Slot semantics, effects, persistence, sync | PARTIAL (accessory provider layer IT: RING×2/AMULET/BELT/CHARM slot model, Curios 9.5.1 bridge + built-in fallback with identical contract, equip/unequip/worn-tick lifecycle, lenient persistence + keepInventory-aware death, dual-configuration game tests — 19 unit + 8 game tests, TC-M8-01; slot item effects TC-M8-02 and armor/equipment TC-M6-07 NI) |
| Enchantments / potions | Original enchantments and potion effects | NI (TC-M6-05 in progress) |
| Entities / AI | Original entities and behaviors | NI (TC-M6-06) |
| World generation | Ores, nodes, structures, biome behavior | PARTIAL (wild-node generation + biome aura behavior IT — TC-M5-01; ore generation — cinnabar/amber/infused stone at original frequencies via thaumcraft:tc_ore feature + biome modifier — IT — TC-M5-05; structures NI, TC-M5-05b) |
| Rendering / particles / sounds | Original visuals via modern APIs | PARTIAL (sound-event registry IT: 66 original TC events 1:1, per-event trigger-site map, parity + game-test guards, 3 wired play sites — TC-M7-05; asset playback depends on the local overlay tree, see D-004; rendering/particles TC-M7-01..03 NI) |
| Persistence | Save/reload/restart/reconnect survival of all state | PARTIAL (substrate IT: versioned WorldMagicData SavedData, lenient player-attachment codecs + copyOnDeath + S2C sync, chunk hooks — 17 unit + 3 game tests, TC-M2-02; research/vis-pool/accessory/aura/flux state persists with each feature; full-state coverage follows feature implementation) |
| Networking | Correct client/server sync | PARTIAL (payload layer IT: 37 payloads 1:1 with the original message set, both-dist registration — TC-M2-03; player-attachment S2C sync wired TC-M2-02; per-feature sync follows feature implementation) |
| ATM10 coexistence | Verified in disposable instance | NI (TC-M9-01/02 in progress) |

Baseline counts (from the original JAR, for scale): 942 classes,
1,107 asset files, ~150 research entries, 25 basic + 7 primal + 8
supernatural… aspect counts to be re-derived exactly from
`Aspect.java` (task TC-M0-04), not quoted from memory.
