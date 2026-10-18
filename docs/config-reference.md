# Configuration Reference (TC-M2-04)

Source of truth: `thaumcraft/common/config/Config.java` in
Thaumcraft-1.7.10-4.2.3.5 (decompiled tree at `../reference/decompiled/`,
workflow in docs/decompilation-notes.md). The original kept every option in
one Forge 1.7.10 `Configuration` file (`config/Thaumcraft.cfg`); the other
`Config*` classes in that package (ConfigBlocks, ConfigItems, ConfigRecipes,
…) are registration classes and read no config — verified: all 62
`Configuration.get` calls in the original JAR live in `Config.java`.

The port implements the same option set as a NeoForge 21.1 `ModConfigSpec`
registered as `ModConfig.Type.COMMON` → one file, `config/thaumcraft-common.toml`,
same categories and option names as the original single file. The
implementation is generated from the code-side inventory
(`TcConfigCatalog`); this table is generated from the same records.

Machine-checked parity (fails `./gradlew test` on any drift):

- `ConfigReferenceTableTest` parses every row below and compares it against
  `TcConfigCatalog` — name, type, default, status, notes, comment, source.
- `ConfigCatalogIntegrityTest` guards the catalog's structure and the
  original read-time value corrections.
- `ConfigParityGameTest` (runGameTestServer) compares the live spec with
  the catalog in-game and proves unknown options are tolerated.

## Reading the table

- **Type/Default/Original comment** are verbatim from the original config
  class; `Source` is the line in the decompiled `Config.java`.
- Defaults are the values passed to `Configuration.get` — the original
  re-read every option through `syncConfigurable()`, so the config-file
  default (not the Java field initializer) is the effective one. Example:
  `general.allow_cheat_sheet` has field initializer `true` but config
  default `false`; the table records `false`.
- **Status** `not ported` marks options that exist only to assign numeric
  registry ids (biome / enchantment / entity / dimension). NeoForge 1.21.1
  has no numeric registry ids — content is registry-keyed — so these
  options have no target-platform equivalent. They stay listed so the
  table remains a complete record of the original.
- Category and option order mirror the original generated `Thaumcraft.cfg`:
  built-in `general` first, then custom categories in the order the
  original registered their comments (Config.java L173-182); within a
  category, `config.get` call order.
- Out-of-range values: the original clamped in Java at read time and left
  the file untouched (`golem_delay` min 1, `golem_ignore_delay` min 1000,
  `golem_link_quality` <4 → 0, `special_node_rarity` min 3,
  `runic_recharge_speed` min 500, `runic_recharge_delay`/`runic_cost`
  min 0). The port reproduces these clamps in `ConfigNormalizers` at read
  time — the file keeps the user's value, every read is corrected, exactly
  like the original.
- Unknown options: the original Forge config ignored keys it did not know.
  NeoForge's spec correction removes unknown keys/categories from the file
  on load and keeps valid values; the mod never crashes on a config file
  written by another version. Guarded by `ConfigParityGameTest`.
- Gameplay wiring: options are consumed through `TcConfig`'s typed
  accessors as their systems land (TC-M2+ milestones); none are consumed
  yet.

## Not-ported options (9)

`general.thaumcraft_villager_id`, `general.thaumcraft_banker_id` (entity
ids), `Enchantments.ench_haste`, `Enchantments.ench_repair` (enchantment
ids), `Biomes.biome_taint`, `Biomes.biome_magical_forest`,
`Biomes.biome_eerie`, `Biomes.biome_eldritch` (biome ids),
`Biomes.outer_lands_dim` (dimension id). The `Enchantments` category has
no ported options and therefore does not appear in the generated TOML; the
`Biomes` category keeps its two weight options.

## general (25 options)

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| allow_mirrors | boolean | true | ported |  | Setting this to false will disable arcane mirror research and crafting recipes. | Config.java:403 |
| color_blind | boolean | false | ported |  | Setting this to true will make certain colors higher contrast or darker to prevent them from being 'invisible' to color blind people. | Config.java:406 |
| shaders | boolean | true | ported |  | This setting will disable certain thaumcraft shaders for those who experience FPS drops. | Config.java:409 |
| crooked | boolean | true | ported |  | Hate crooked labels, kittens, puppies and all things awesome? If yes, set this to false. | Config.java:412 |
| hard_mode_nodes | boolean | true | ported |  | Negative nodes like hungry, tainted or dark nodes will have additional, much nastier, effects. | Config.java:415 |
| wuss_mode | boolean | false | ported |  | Setting this to true disables Warp and similar mechanics. You wuss. | Config.java:418 |
| wand_dial_bottom | boolean | false | ported |  | Set to true to have the wand dial display in the bottom left instead of the top left. | Config.java:421 |
| golem_delay | int | 5 | ported |  | How many ticks a golem waits between checking for tasks. Setting it higher will save server ticks, but will make the golems slower to react. | Config.java:424 |
| golem_ignore_delay | int | 10000 | ported |  | How many milliseconds a golem will ignore an item after it has failed to find a destination or use for it. Min value 1000 | Config.java:431 |
| golem_link_quality | int | 16 | ported |  | The fx quality of the line connecting golems to marked blocks. Setting this below 4 deactives the effect entirely. | Config.java:438 |
| notification_delay | int | 5000 | ported |  | Determines how fast notifications scroll downwards. | Config.java:445 |
| notification_max | int | 15 | ported |  | The maximum amount of notifications that are displayed onscreen. | Config.java:448 |
| node_rarity | int | 36 | ported |  | How rare nodes are in the world. The number means there will be (on average) one node per N chunks. | Config.java:451 |
| special_node_rarity | int | 18 | ported |  | The chance of a node being special (pure, dark, unstable, etc.). The number means roughly 1 in N nodes will be special, so setting the number to 5 will mean 1 in 5 nodes may be special. | Config.java:454 |
| display_aspects | boolean | false | ported |  | Item aspects are hidden by default and pressing shift reveals them. Changing this setting to 'true' will reverse this behaviour and always display aspects unless shift is pressed. | Config.java:461 |
| allow_cheat_sheet | boolean | false | ported |  | Enables a version of the Thauminomicon in creative mode that grants you all the research when you first use it. | Config.java:464 |
| allow_warded_stone | boolean | true | ported |  | If set to false, warded stone, doors and glass will just be cosmetic in nature and not have its hardened properties (everyone will be able to break it with equal ease). | Config.java:467 |
| thaumcraft_villager_id | int | 190 | not ported | Numeric entity ids do not exist on NeoForge 1.21.1; entity types are registry-keyed. | Thaumcraft wizard villager id | Config.java:470 |
| thaumcraft_banker_id | int | 191 | not ported | Numeric entity ids do not exist on NeoForge 1.21.1; entity types are registry-keyed. | Thaumcraft banker villager id | Config.java:473 |
| golem_chest_interact | boolean | true | ported |  | If set to true golems will attempt to play the chest opening animations and sounds whenever they interact with them. | Config.java:476 |
| portablehole_blacklist | string | iron_door | ported |  | This is a comma-delimited list of any block names the portable hole is not allowed to pass through. | Config.java:479 |
| blue_magical_forest | boolean | false | ported |  | Set this to true to get the old blue magical forest back. | Config.java:494 |
| biome_taint_from_flux | boolean | true | ported |  | Can Taint be caused by flux effects. | Config.java:497 |
| biome_taint_spread | int | 200 | ported |  | The chance per block update (1 in n) of the Taint biome spreading. Setting it to 0 prevents the spread of Taint biomes. | Config.java:500 |
| glowing_taint | boolean | true | ported |  | Setting this to false will remove the glowing purple nodules from taint fibres. This might prevent crashes some people experience and improve performance. | Config.java:503 |

## Enchantments (2 options)

Category comment (verbatim): Custom enchantments

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| ench_haste | int | 150 | not ported | Numeric enchantment ids do not exist on NeoForge 1.21.1; enchantments are registry-keyed. |  | Config.java:250 |
| ench_repair | int | 151 | not ported | Numeric enchantment ids do not exist on NeoForge 1.21.1; enchantments are registry-keyed. |  | Config.java:254 |

## Monster_Spawning (8 options)

Category comment (verbatim): Will these mobs spawn

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| spawn_angry_zombies | boolean | true | ported |  |  | Config.java:393 |
| spawn_fire_bats | boolean | true | ported |  |  | Config.java:394 |
| spawn_wisps | boolean | true | ported |  |  | Config.java:395 |
| spawn_taintacles | boolean | true | ported |  |  | Config.java:396 |
| spawn_taint_spores | boolean | true | ported |  |  | Config.java:397 |
| spawn_pechs | boolean | true | ported |  |  | Config.java:398 |
| spawn_eldercreatures | boolean | true | ported |  |  | Config.java:399 |
| champion_mobs | boolean | true | ported |  | Setting this to false will disable spawning champion mobs. Even when false they will still have a greatly reduced chance of spawning in certain dangerous places. | Config.java:400 |

## Research (2 options)

Category comment (verbatim): Various research related things.

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| research_difficulty | int | 0 | ported |  | 0 = normal, -1 = easy (all research items are directly purchased with RP), 1 = Hard (all research items need to be solved via the research table) | Config.java:387 |
| aspect_total_cap | int | 100 | ported |  | The total amount of RP you can have in your pool per aspect before the scanning soft cap kicks in. | Config.java:390 |

## World_Generation (7 options)

Category comment (verbatim): Settings to turn certain world-gen on or off.

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| generate_aura_nodes | boolean | true | ported |  |  | Config.java:368 |
| generate_structures | boolean | true | ported |  |  | Config.java:369 |
| generate_cinnibar_ore | boolean | true | ported |  |  | Config.java:370 |
| generate_amber_ore | boolean | true | ported |  |  | Config.java:371 |
| generate_infused_stone | boolean | true | ported |  |  | Config.java:372 |
| generate_trees | boolean | true | ported |  |  | Config.java:373 |
| generate_taint | boolean | true | ported |  | Can taint biomes generate at worldgen | Config.java:374 |

## World_Regeneration (8 options)

Category comment (verbatim): If a chunk is encountered that skipped TC worldgen, then the game will attempt to regenerate certain world features if they are set to true. CAUTION: Best used for worlds created before you added this mod, and only if you know what you are doing. Backups are advised.

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| regen_key | string | DEFAULT | ported |  | This key is used to keep track of which chunk have been generated/regenerated. Changing it will cause the regeneration code to run again, so only change it if you want it to happen. Useful to regen only one world feature at a time. | Config.java:377 |
| aura_nodes | boolean | false | ported |  |  | Config.java:380 |
| structures | boolean | false | ported |  |  | Config.java:381 |
| cinnibar_ore | boolean | false | ported |  |  | Config.java:382 |
| amber_ore | boolean | false | ported |  |  | Config.java:383 |
| infused_stone | boolean | false | ported |  |  | Config.java:384 |
| trees | boolean | false | ported |  |  | Config.java:385 |
| taint | boolean | false | ported |  |  | Config.java:386 |

## Biomes (7 options)

Category comment (verbatim): Biomes and effects

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| taint_biome_weight | int | 2 | ported |  | higher values increases number of taint biomes. If you are using biome addon mods you probably want to increase this weight quite a bit | Config.java:185 |
| biome_taint | int | 192 | not ported | Numeric biome ids do not exist on NeoForge 1.21.1; biomes are registry-keyed. The biome weight options are ported. | Taint biome id | Config.java:188 |
| magical_forest_biome_weight | int | 5 | ported |  | higher values increases number of magical forest biomes. If you are using biome addon mods you probably want to increase this weight quite a bit | Config.java:202 |
| biome_magical_forest | int | 193 | not ported | Numeric biome ids do not exist on NeoForge 1.21.1; biomes are registry-keyed. The biome weight options are ported. | Magical Forest biome id | Config.java:205 |
| biome_eerie | int | 194 | not ported | Numeric biome ids do not exist on NeoForge 1.21.1; biomes are registry-keyed. The biome weight options are ported. | Eerie biome id | Config.java:219 |
| biome_eldritch | int | 195 | not ported | Numeric biome ids do not exist on NeoForge 1.21.1; biomes are registry-keyed. The biome weight options are ported. | Eldritch Lands biome id | Config.java:233 |
| outer_lands_dim | int | -42 | not ported | Numeric dimension ids do not exist on NeoForge 1.21.1; dimensions are registry-keyed. |  | Config.java:247 |

## Runic_Shielding (3 options)

Category comment (verbatim): Runic Shielding

| Option | Type | Default | Status | Notes | Original comment | Source |
|---|---|---|---|---|---|---|
| runic_recharge_speed | int | 2000 | ported |  | How many milliseconds pass between runic shielding recharge ticks. Lower values equals faster recharge. Minimum of 500. | Config.java:506 |
| runic_recharge_delay | int | 80 | ported |  | How many game ticks pass after a shield has been reduced to zero before it can start recharging again. Minimum of 0. | Config.java:509 |
| runic_cost | int | 50 | ported |  | How much aer and terra centi-vis (0.01 vis) it costs to reacharge a single unit of shielding. Minimum of 0. | Config.java:512 |

