# Original Baseline — verified facts about the source artifacts

Verified 2026-10-17 on this machine.

## Thaumcraft-1.7.10-4.2.3.5.jar

- SHA-256: `3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`
- Size: 12,565,807 bytes
- Contents: 942 `.class` entries; 1,107 files under `assets/thaumcraft`;
  `assets/minecraft` (15), `mcmod.info`, `dependancies.info`,
  `changelog.txt`, `pack.mcmeta`, `TCLogo.png`, embedded `truetyper/`
  (a bundled library), and `thaumcraft/codechicken/*` (CodeChickenLib
  pieces bundled by the author).
- Mod id: `Thaumcraft` (from mcmod.info / class structure).
- Package map (class counts): common/entities/monster 52,
  client/renderers/tile 52, client/renderers/models 48, common/entities/ai 45,
  client/renderers/entity 42, common/lib/network 40, common/lib/world 39,
  client/renderers/block 23, client/fx/particles 23,
  common/items/equipment 19, common/items/wands 17, common/entities/golems 16,
  common/items/armor 15, common/entities/projectile 12, common/lib/utils 12,
  common/lib/events 10, common/lib/potions 7, common/lib/research 6,
  common/items/baubles 6, common/lib/enchantment 5, api 11 top-level +
  api subpackages (aspects, research, wands, visnet, nodes, crafting,
  potions, entities, internal, damagesource).
- Full inventories: see `original-content-inventory.md` (built from the
  decompiled tree; task TC-M0-04).

## Baubles-1.7.10-1.0.1.10.jar

- SHA-256: `05ae8f11b8f9012ebebbf179227b010829739c6284e30da5dd3db220ee682f8a`
- Size: 95,415 bytes; 69 archive members (REA inventory `ai_04380c4e892a…`
  from the sibling project).
- API: `baubles.api` (BaublesApi, BaubleType RING/AMULET/BELT, IBauble),
  `baubles.common` (PlayerHandler, `.baub` persistence),
  `baubles.client` (ContainerPlayerExpanded, SlotBauble).
- License: CC BY-NC-SA 3.0 (repo README); JAR embeds no license text.

## neoforge-21.1.259-installer.jar

- SHA-256: `bb971287ab44fc43f27da77ac34e263a3885d73fb842ceeb20ee5c517ff4378c`
- Size: 6,972,025 bytes. Target loader identity confirmed against the
  ATM10 instance (docs/target-platform.md).

## Original API repositories (reference clones)

- `Azanor/thaumcraft-api` branch `1.7.10` @ `ced30ed35097d53cdb2a843d705477e52896e54b` — **MIT (c) 2013 Azanor**.
- `Azanor/Baubles` branch `1.7.10` @ `2ad0bd6ee34752ab0a81562ed59803ae804d9fec` — CC BY-NC-SA 3.0.

## What the API repo does and does not prove

The API repo defines extension points (IAspect, research classes, wand
interfaces, visnet interfaces). The **implementation** of every behavior
lives only in the JAR. Both are used; the JAR is authoritative.
