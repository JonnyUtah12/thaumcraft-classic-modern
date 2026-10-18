# Feature Tracking — original feature → task → status

Authoritative parity status lives in `compatibility-matrix.md`; this file
maps each original feature area to the Kanban task (board
`thaumcraft-classic-port`) that implements it.
Statuses: NOT STARTED / IN PROGRESS / PARTIAL / DONE (DONE only with test
evidence). Task ids are Kanban ids on the board.

| Feature area (original) | Source evidence (TC 4.2.3.5) | Task (id) | Status |
| --- | --- | --- | --- |
| Environment/artifact verification | JARs, instance | TC-M0-01 (t_6ebc7418) | DONE |
| Decompilation workflow | REA + Vineflower | TC-M0-02 (t_22824a88) | DONE |
| API repo reference | api repos @ 1.7.10 | TC-M0-03 (t_cc8fc2e9) | DONE |
| Content + system inventory | decompiled tree | TC-M0-04 (t_83241bc7) | NOT STARTED |
| Asset inventory/mapping | assets (1079 files) | TC-M0-05 (t_66f308b2) | NOT STARTED |
| Balance values | decompiled constants | TC-M0-06 (t_b9cd76b8) | NOT STARTED |
| Build/load foundation | — | TC-M1-01 (t_e81a4a3b) | IN PROGRESS |
| Registration + id stability | common registration | TC-M2-01 (t_749e3134) | NOT STARTED |
| Persistence (world/player) | WorldMagicData, PlayerKnowledge | TC-M2-02 (t_0083a616) | NOT STARTED |
| Networking | common/lib/network (40 classes) | TC-M2-03 (t_cbc91aec) | NOT STARTED |
| Configuration | config class | TC-M2-04 (t_c87a23bc) | DONE |
| Research graph | Research.java | TC-M3-01 (t_eef90e8c) | NOT STARTED |
| Research progression | ResearchManager | TC-M3-02 (t_25f6eeb2) | NOT STARTED |
| Thaumonomicon GUI | client/gui | TC-M3-03 (t_38e57cb0) | NOT STARTED |
| Research minigames | note puzzle classes | TC-M3-04 (t_4ebb83af) | NOT STARTED |
| Aspects | api/aspects | TC-M4-01 (t_0cc0a0ca) | NOT STARTED |
| Vis core | api/visnet, wands | TC-M4-02 (t_d7272167) | NOT STARTED |
| Arcane workbench | RecipeArcaneWorkbench | TC-M4-03 (t_81c79f33) | NOT STARTED |
| Infusion altar | TileInfusion* | TC-M4-04 (t_30961958) | NOT STARTED |
| Alchemy/crucible | TileCrucible* | TC-M4-05 (t_cdece7cd) | NOT STARTED |
| Essentia storage | TileSmelter*, jars | TC-M4-06 (t_e9d23e1c) | NOT STARTED |
| Essentia transport | tube network | TC-M4-07 (t_2047bca5) | NOT STARTED |
| Aura + nodes | common/world, api/nodes | TC-M5-01 (t_71def828) | NOT STARTED |
| Node behavior | WorldMagicData tick | TC-M5-02 (t_d00335ab) | NOT STARTED |
| Flux | WorldMagicData flux | TC-M5-03 (t_c476f104) | NOT STARTED |
| Taint | common/taint | TC-M5-04 (t_66c37e97) | NOT STARTED |
| World gen (ores) | worldgen handlers | TC-M5-05 (t_de33def3) | NOT STARTED |
| Wands (rods/caps) | items/wands | TC-M6-01 (t_aaab7bef) | NOT STARTED |
| Foci + casting | focus classes | TC-M6-02 (t_ae471793) | NOT STARTED |
| Focus behaviors/combos | projectile classes | TC-M6-03 (t_a0113d55) | NOT STARTED |
| Block/item catalog | content-mapping.csv | TC-M6-04 (t_12f350af) | NOT STARTED |
| Enchantments/potions | lib/enchantment, api/potions | TC-M6-05 (t_a8f8a37c) | NOT STARTED |
| Entities/AI | common/entities (125 classes) | TC-M6-06 (t_cd287131) | NOT STARTED |
| Armor/equipment | items/armor | TC-M6-07 (t_1ba6e428) | NOT STARTED |
| Models/textures | assets + renderers | TC-M7-01 (t_1625375e) | NOT STARTED |
| Particles/beams | client/fx | TC-M7-02 (t_b1249368) | NOT STARTED |
| Entity renderers | renderers/entity | TC-M7-03 (t_6c2e5f60) | NOT STARTED |
| GUI parity | client/gui | TC-M7-04 (t_52cdd02c) | NOT STARTED |
| Sounds | sounds assets | TC-M7-05 (t_32cd081b) | NOT STARTED |
| Accessory layer | Baubles semantics | TC-M8-01 (t_76c2874f) | NOT STARTED |
| Baubles items | items/baubles | TC-M8-02 (t_0ca9e33b) | NOT STARTED |
| ATM10 startup | disposable instance | TC-M9-01 (t_bd0c30d2) | NOT STARTED |
| ATM10 gameplay | disposable instance | TC-M9-02 (t_95c84692) | NOT STARTED |
| Regression | all suites | TC-M10-01 (t_fa8b23a7) | NOT STARTED |
| Release | release gate | TC-M10-02 (t_596882c9) | NOT STARTED |
