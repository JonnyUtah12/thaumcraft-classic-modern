# Compatibility Matrix — feature-by-feature parity

Statuses (per the project brief):
**IT** implemented+tested · **IU** implemented, not fully tested ·
**PARTIAL** partially implemented · **NI** not yet implemented ·
**BL** blocked by a technical limitation · **AD** intentionally adapted
(reason + fidelity impact in `porting-decisions.md`).

Updated by the same change that alters a feature. No row may move to IT/IU
without test evidence linked in the Kanban task.

| System | Required verification | Status |
| --- | --- | --- |
| Build/load foundation | Gradle build, mod loads in game-test server, metadata correct | IT (unit + smoke gametest, CI) |
| Aspects | Original 25+7+… definitions, combination rules, discovery | NI |
| Research | Full original graph, prerequisites, unlock triggers, persistence | IT (graph 1:1 + progression/triggers/points/warp server-authoritative; 87 unit + 18 game tests, TC-M3-01/02; Thaumonomicon GUI is TC-M3-03) |
| Thaumonomicon | Pages, minigames, navigation, text | NI |
| Vis / aura / nodes | Generation, capacity, depletion, wand interaction, persistence | NI |
| Flux / taint | Spread, transformation, biome effects | NI |
| Arcane crafting | Workbench mechanics, vis costs, research gating | NI |
| Infusion | Pedestals, costs, instability, effects | NI |
| Alchemy | Crucible/table state machines, recipes | NI |
| Essentia | Extraction, transport, matching, containers | NI |
| Wands & foci | Cores/caps, capacities, per-focus behavior, targeting | NI |
| Equipment / accessories | Slot semantics, effects, persistence, sync | NI |
| Enchantments / potions | Original enchantments and potion effects | NI |
| Entities / AI | Original entities and behaviors | NI |
| World generation | Ores, nodes, structures, biome behavior | NI |
| Rendering / particles / sounds | Original visuals via modern APIs | NI |
| Persistence | Save/reload/restart/reconnect survival of all state | NI |
| Networking | Correct client/server sync | NI |
| ATM10 coexistence | Verified in disposable instance | NI |

Baseline counts (from the original JAR, for scale): 942 classes,
1,107 asset files, ~150 research entries, 25 basic + 7 primal + 8
supernatural… aspect counts to be re-derived exactly from
`Aspect.java` (task TC-M0-04), not quoted from memory.
