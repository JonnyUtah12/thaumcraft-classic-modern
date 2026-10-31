# Registration Architecture (TC-M2-01)

One registration surface, one id contract, one harness that keeps them
aligned forever.

## The id contract

`docs/content-mapping.csv` is the authoritative id contract for all mod
content. For every row:

    new_id = "thaumcraft:" + lower(original id path)      (D-001 addendum)

Lowercasing is the only permitted transformation. Ids are frozen once
shipped — saves, scripts and addons may reference them forever.

Exceptions (documented, guarded by tests):

- **Sound events** are not content-mapping rows; the two original ids with
  uppercase letters were renamed to snake_case (see D-001 addendum,
  docs/asset-mapping.csv).
- **wand_trigger rows** carry the original `registerWandBlockTrigger` call
  signature as their original id; they share the pseudo-id
  `thaumcraft:wandtrigger` and are never registered as objects.
- **Shared ids within a kind** are allowed only for the documented
  multi-registration groups: wand rod/staff pairs (greatwood, silverwood,
  obsidian, blaze, ice, quartz, bone, reed), multi-use oredict names
  (logWood, plankWood, slabWood, treeSapling), multi-rule mob spawns
  (EntityFireBat), and the wand_trigger pseudo-id. Each group resolves to a
  single registered object (or is reference-only).

`implementation_status` vocabulary: `not_started | in_progress | done |
deferred`. Content milestones flip rows to `done` as they land; the harness
then enforces that the id resolves to a live registered object.

## Holders

All `DeferredRegister` holders live in `registration/` and are attached to
the mod event bus from the mod constructor via `ModRegistration.init()` —
before any `RegisterEvent` fires. Attaching a holder later throws
"DeferredRegister registered too late"; the constructor is the only safe
place, and `ModRegistration` is the only ordering point.

| Holder | Registry | content-mapping kinds | Filled by |
| --- | --- | --- | --- |
| `ModDataComponents` | `minecraft:data_component_type` | — (replaces ad-hoc item NBT) | M4+ features |
| `ModBlocks` | `minecraft:block` | block | M4-M6 |
| `ModItems` | `minecraft:item` | item, spawner_egg | M4-M6 |
| `ModBlockEntities` | `minecraft:block_entity_type` | tile_entity (keyed by owning block) | M4-M6 |
| `ModEntities` | `minecraft:entity_type` | entity | M6-06 |
| `ModMobEffects` | `minecraft:mob_effect` | potion | M6-05 |
| `ModParticles` | `minecraft:particle_type` | — (FX are network payloads) | M6/M7 |
| `ModCreativeTabs` | `minecraft:creative_mode_tab` | — | M6-04 |
| `ModAttachments` (TC-M2-02) | `neoforge:attachment_types` | — | done |
| `ModSounds` (TC-M7-05) | `minecraft:sound_event` | — (sound events, asset overlay) | done |

Ordering: NeoForge itself fires `RegisterEvent` per registry in a fixed
order (data components first, then vanilla order — blocks before items
before block entities — then modded registries). `ModRegistration.init()`
mirrors that dependency chain for readability; what matters is that every
holder is attached before startup registration begins.

### Enchantments are not a DeferredRegister

In 1.21.1 `minecraft:enchantment` is a **datapack registry**: it is not in
`BuiltInRegistries`, and NeoForge never fires a `RegisterEvent` for it
(verified against NeoForge 21.1.259 `GameData.postRegisterEvents`). The id
contract for kind=enchantment is `ModEnchantments` `ResourceKey` constants
plus JSON under `data/thaumcraft/enchantment/`. TC-M6-05 implements the
two enchantments the original actually registers (Haste, Repair).

### Modded registry keys (pinned)

Feature milestones create these via `NewRegistryEvent` + their own
DeferredRegister, wired from the mod constructor like everything else. The
registry keys are fixed here so the harness and every milestone agree:

| Registry key | kinds | Owner |
| --- | --- | --- |
| `thaumcraft:aspect` | aspect | TC-M4-01 |
| `thaumcraft:wand_component` | wand_component | TC-M6 (wand system) |
| `thaumcraft:node_type` | node_type | TC-M5 |
| `thaumcraft:node_modifier` | node_modifier | TC-M5 |

Entry ids inside `thaumcraft:aspect` carry the `aspect.<tag>` path
(`thaumcraft:aspect.aer`), exactly the frozen CSV contract ids; the same
prefix pattern `NodeType.registryPath()` uses for `thaumcraft:nodetype.*`.
The aspect tag stays the serialization key (NBT, payloads, essentia
tables); only the registry id carries the prefix (t_f00b7e29).

### Research content is graph data, not a registry (t_f00b7e29)

`kind=research` (201 rows) and `kind=research_category` (6 rows) have **no
modded registry**. The earlier pin table listed `thaumcraft:research` /
`thaumcraft:research_category` registries for TC-M3-01/02; those registries
were never created and will not be — the decision is recorded here. The
original registered research in the static `ResearchCategories` code keyed
by string, and the port ships the extracted graph as data:
`data/thaumcraft/research_graph/{categories,entries}.json`, loaded once by
`research/ResearchGraphLoader` and pinned by `ResearchGraphLoaderTest`
(201 entries / 6 categories, structural validation in `ResearchGraph`).
A registry would have no consumer — research keys are not saved as registry
ids, not synced by registry sync, and not datapack-referenced — and
registry population happens at `RegisterEvent` time, before data resources
exist, so data-driven graph nodes cannot live there.

The harness therefore resolves a `done` research row against the **live
graph**: `RegistrationGameTest` looks the CSV id path up in
`ResearchManager.graph()` (entry key for `research`, category key for
`research_category`), matching case-insensitively because D-001 lowercases
the original uppercase keys. The row is `done` only while the graph node
exists at runtime — the same forward guarantee the registry branches give.

Reference-only kinds (never standalone registered objects): `config_option`,
`gui`, `oredict` (→ item tags), `mob_spawn` (spawn placements), `dimension`,
`worldgen_feature`, `wand_trigger`, `tile_entity` (keyed by block),
`recipe` (instances; serializers are registered by their features).

## The harness

- **Unit** (`ContentMappingIdContractTest`, `./gradlew test`): parses the
  CSV; asserts every `new_id` is a legal `thaumcraft` resource location,
  equals the D-001 rule applied to the original id, and that shared ids are
  only the documented groups. Pure JDK — no Minecraft classpath.
- **Game** (`RegistrationGameTest`, `./gradlew runGameTestServer`):
  1. `registriesInitialize` — every holder's registry exists at runtime and
     the probe block/item are registered under their exact contract ids
     (proves constructor-time wiring; no late-registration errors).
  2. `probeRoundTripsThroughSaveData` — probe block state and item stack
     survive NBT save round-trips (the save-data id path).
  3. `implementedContentIdsResolve` — forward harness: every CSV row with
     status `done` must resolve in its target registry (builtin, datapack
     lookup, pinned modded registry) or in the live research graph (kind
     `research` / `research_category`, see above). Rows stay `not_started`
     until their milestone lands; from then on the same test enforces
     resolution forever.

Probe content (`TestProbeContent`) registers only under
`-Dthaumcraft.testprobe=true`, set by the `gameTestServer` run config —
the shipped mod contains exactly the CSV's ids, never test scaffolding.

## Adding content (checklist for feature milestones)

1. Register the object in the right holder with the CSV's `new_id` path.
2. Flip that row's `implementation_status` to `done` (and `test_status` as
   you add coverage).
3. `./gradlew test runGameTestServer` — the harness does the rest.
