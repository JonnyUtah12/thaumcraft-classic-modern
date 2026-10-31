# ATM10 gameplay battery — evidence

- card: `t_d827c350` (TC-M9-02-s, replaces legacy `t_95c84692`)
- instance: `/home/aaron/minecraft mods/atm10-test` (disposable; primary Prism instance untouched)
- jar under test: `/home/aaron/Thaumcraft-Classic-Modern/thaumcraft-classic/.worktrees/t_d827c350/build/libs/thaumcraft-0.1.0-alpha.4.jar`
- world: `world-tc-m9-02-battery-i`, heap `-Xmx4G`
- started: 2026-10-30 20:03:39 EDT
- server ready: [20:06:01] [Server thread/INFO] [minecraft/DedicatedServer]: Done (26.674s)! For help, type "help"
- clean shutdown ('All dimensions are saved'): True
- pack jar backed up + restored on exit: ['thaumcraft-0.1.0-alpha.1.jar']

Data-safety rails enforced by the harness: it refuses any instance whose path contains `PrismLauncher` (the primary install) or whose directory is not the disposable `atm10-test` one; it backs up the pack's own copy of the mod and restores it on exit; and it deletes, disables or renames **no** other mod to make the port load. Conflicts below are recorded, not papered over.

## Mod load in the pack

- mod list: `Thaumcraft Classic (Unofficial) 0.1.0-alpha.4 (thaumcraft)`
- mod list: `[20:04:21] [modloading-worker-0/INFO] [co.jo.th.ThaumcraftClassic/]: Thaumcraft Classic (unofficial port of Thaumcraft 4.2.3.5) initializing; target MC 1.21.1 /`

## L1-blocks — Block + block entity instantiation  [PASS]

Every ported block is placed into the real pack world; the ones with a block entity are read back, which proves the tile instantiates and serialises next to 470 mods.

| # | command | result |
| --- | --- | --- |
| 1 | `forceload add -16 -16 48 80` | ok — [20:06:02] [Server thread/INFO] [ne.ne.ne.se.pe.PermissionAPI/]: Successfully initialized permission handler neoforge:de |
| 2 | `setblock 4 120 4 thaumcraft:blockjar` | ok — [20:06:15] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 4, 120, 4 |
| 3 | `data get block 4 120 4` | ok — [20:06:16] [Server thread/INFO] [minecraft/MinecraftServer]: 4, 120, 4 has the following block data: {Amount: 0s, x: 4,  |
| 4 | `setblock 5 120 4 thaumcraft:blockarcanefurnace` | ok — [20:06:16] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 5, 120, 4 |
| 5 | `data get block 5 120 4` | ok — [20:06:16] [Server thread/INFO] [minecraft/MinecraftServer]: 5, 120, 4 has the following block data: {CookTime: 0s, x: 5 |
| 6 | `setblock 6 120 4 thaumcraft:blocktable` | ok — [20:06:17] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 6, 120, 4 |
| 7 | `data get block 6 120 4` | ok — [20:06:17] [Server thread/INFO] [minecraft/MinecraftServer]: 6, 120, 4 has the following block data: {x: 6, y: 120, z: 4 |
| 8 | `setblock 7 120 4 thaumcraft:blockchesthungry` | ok — [20:06:17] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 7, 120, 4 |
| 9 | `data get block 7 120 4` | ok — [20:06:17] [Server thread/INFO] [minecraft/MinecraftServer]: 7, 120, 4 has the following block data: {x: 7, y: 120, Item |
| 10 | `setblock 8 120 4 thaumcraft:blockmirror` | ok — [20:06:17] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 8, 120, 4 |
| 11 | `data get block 8 120 4` | ok — [20:06:17] [Server thread/INFO] [minecraft/MinecraftServer]: The target block is not a block entity |
| 12 | `setblock 9 120 4 thaumcraft:blockcrystal` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 9, 120, 4 |
| 13 | `data get block 9 120 4` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: 9, 120, 4 has the following block data: {orientation: 1, x: |
| 14 | `setblock 10 120 4 thaumcraft:blockmetaldevice` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 10, 120, 4 |
| 15 | `setblock 11 120 4 thaumcraft:blockwoodendevice` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 11, 120, 4 |
| 16 | `setblock 12 120 4 thaumcraft:blockstonedevice` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 12, 120, 4 |
| 17 | `setblock 13 120 4 thaumcraft:blocklifter` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 13, 120, 4 |
| 18 | `setblock 14 120 4 thaumcraft:blockmagicbox` | ok — [20:06:18] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 14, 120, 4 |
| 19 | `data get block 14 120 4` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: 14, 120, 4 has the following block data: {x: 14, y: 120, It |
| 20 | `setblock 15 120 4 thaumcraft:blockcandle` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 15, 120, 4 |
| 21 | `setblock 16 120 4 thaumcraft:blockmanapod` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 16, 120, 4 |
| 22 | `data get block 16 120 4` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: 16, 120, 4 has the following block data: {x: 16, y: 120, z: |
| 23 | `setblock 17 120 4 thaumcraft:blockmetaldevice[variant=0]` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 17, 120, 4 |
| 24 | `data get block 17 120 4` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: 17, 120, 4 has the following block data: {Heat: 0s, Aspects |
| 25 | `setblock 18 120 4 thaumcraft:blockstonedevice[variant=9]` | ok — [20:06:19] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 18, 120, 4 |
| 26 | `data get block 18 120 4` | ok — [20:06:20] [Server thread/INFO] [minecraft/MinecraftServer]: 18, 120, 4 has the following block data: {x: 18, y: 120, It |

## L2-items — Item registry + item-component resolution  [PASS]

Each ported item id is spawned as a real item entity and read back: an unregistered id degrades to minecraft:air, so the read-back is a genuine registry check inside the pack.

| # | command | result |
| --- | --- | --- |
| 1 | `summon minecraft:item 4 120 44 {Item:{id:"thaumcraft:itemessence",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:20] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemessence |
| 2 | `data get entity @e[type=minecraft:item,x=4,y=120,z=44,distance=..1,limit=1]` | ok — [20:06:20] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemessence has the following entity data:  |
| 3 | `summon minecraft:item 6 120 44 {Item:{id:"thaumcraft:itemthaumometer",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:20] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Thaumometer |
| 4 | `data get entity @e[type=minecraft:item,x=6,y=120,z=44,distance=..1,limit=1]` | ok — [20:06:20] [Server thread/INFO] [minecraft/MinecraftServer]: Thaumometer has the following entity data: {NoGravity: 1b,  |
| 5 | `summon minecraft:item 8 120 44 {Item:{id:"thaumcraft:itemshard",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:20] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemshard |
| 6 | `data get entity @e[type=minecraft:item,x=8,y=120,z=44,distance=..1,limit=1]` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemshard has the following entity data: {N |
| 7 | `summon minecraft:item 10 120 44 {Item:{id:"thaumcraft:itemresource",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemresource |
| 8 | `data get entity @e[type=minecraft:item,x=10,y=120,z=44,distance=..1,limit=1]` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemresource has the following entity data: |
| 9 | `summon minecraft:item 12 120 44 {Item:{id:"thaumcraft:handmirror",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Magic Hand Mirror |
| 10 | `data get entity @e[type=minecraft:item,x=12,y=120,z=44,distance=..1,limit=1]` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: Magic Hand Mirror has the following entity data: {NoGravity |
| 11 | `summon minecraft:item 14 120 44 {Item:{id:"thaumcraft:itemcrystalessence",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Crystallized Essence |
| 12 | `data get entity @e[type=minecraft:item,x=14,y=120,z=44,distance=..1,limit=1]` | ok — [20:06:21] [Server thread/INFO] [minecraft/MinecraftServer]: Crystallized Essence has the following entity data: {NoGrav |
| 13 | `summon minecraft:item 4 120 46 {Item:{id:"thaumcraft:itemlootbag",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemlootbag |
| 14 | `data get entity @e[type=minecraft:item,x=4,y=120,z=46,distance=..1,limit=1]` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemlootbag has the following entity data:  |
| 15 | `summon minecraft:item 6 120 46 {Item:{id:"thaumcraft:itemresonator",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Essentia Resonator |
| 16 | `data get entity @e[type=minecraft:item,x=6,y=120,z=46,distance=..1,limit=1]` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: Essentia Resonator has the following entity data: {NoGravit |
| 17 | `summon minecraft:item 8 120 46 {Item:{id:"thaumcraft:itembaubleblanks",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itembaubleblanks |
| 18 | `data get entity @e[type=minecraft:item,x=8,y=120,z=46,distance=..1,limit=1]` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itembaubleblanks has the following entity d |
| 19 | `summon minecraft:item 10 120 46 {Item:{id:"thaumcraft:trunkspawner",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:22] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Traveling Trunk |
| 20 | `data get entity @e[type=minecraft:item,x=10,y=120,z=46,distance=..1,limit=1]` | ok — [20:06:23] [Server thread/INFO] [minecraft/MinecraftServer]: Traveling Trunk has the following entity data: {NoGravity:  |
| 21 | `summon minecraft:item 12 120 46 {Item:{id:"thaumcraft:itemnugget",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:23] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemnugget |
| 22 | `data get entity @e[type=minecraft:item,x=12,y=120,z=46,distance=..1,limit=1]` | ok — [20:06:23] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemnugget has the following entity data: { |
| 23 | `summon minecraft:item 14 120 46 {Item:{id:"thaumcraft:itemwispessence",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:23] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Ethereal Essence |
| 24 | `data get entity @e[type=minecraft:item,x=14,y=120,z=46,distance=..1,limit=1]` | ok — [20:06:23] [Server thread/INFO] [minecraft/MinecraftServer]: Ethereal Essence has the following entity data: {NoGravity: |
| 25 | `summon minecraft:item 4 120 48 {Item:{id:"thaumcraft:blockjarfilleditem",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:23] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Jar of Essentia |
| 26 | `data get entity @e[type=minecraft:item,x=4,y=120,z=48,distance=..1,limit=1]` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Jar of Essentia has the following entity data: {NoGravity:  |
| 27 | `summon minecraft:item 6 120 48 {Item:{id:"thaumcraft:itemarcanedoor",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Arcane Door |
| 28 | `data get entity @e[type=minecraft:item,x=6,y=120,z=48,distance=..1,limit=1]` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Arcane Door has the following entity data: {NoGravity: 1b,  |
| 29 | `summon minecraft:item 8 120 48 {Item:{id:"thaumcraft:arcanedoorkey",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Arcane Key |
| 30 | `data get entity @e[type=minecraft:item,x=8,y=120,z=48,distance=..1,limit=1]` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Arcane Key has the following entity data: {NoGravity: 1b, M |
| 31 | `summon minecraft:item 10 120 48 {Item:{id:"thaumcraft:itemmanabean",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Mana Bean |
| 32 | `data get entity @e[type=minecraft:item,x=10,y=120,z=48,distance=..1,limit=1]` | ok — [20:06:24] [Server thread/INFO] [minecraft/MinecraftServer]: Mana Bean has the following entity data: {NoGravity: 1b, Mo |
| 33 | `summon minecraft:item 12 120 48 {Item:{id:"thaumcraft:itemamuletrunic",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemamuletrunic |
| 34 | `data get entity @e[type=minecraft:item,x=12,y=120,z=48,distance=..1,limit=1]` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemamuletrunic has the following entity da |
| 35 | `summon minecraft:item 14 120 48 {Item:{id:"thaumcraft:itemringrunic",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemringrunic |
| 36 | `data get entity @e[type=minecraft:item,x=14,y=120,z=48,distance=..1,limit=1]` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemringrunic has the following entity data |
| 37 | `summon minecraft:item 4 120 50 {Item:{id:"thaumcraft:itemgirdlerunic",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemgirdlerunic |
| 38 | `data get entity @e[type=minecraft:item,x=4,y=120,z=50,distance=..1,limit=1]` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemgirdlerunic has the following entity da |
| 39 | `summon minecraft:item 6 120 50 {Item:{id:"thaumcraft:itemamuletvis",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:25] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.itemamuletvis |
| 40 | `data get entity @e[type=minecraft:item,x=6,y=120,z=50,distance=..1,limit=1]` | ok — [20:06:26] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.itemamuletvis has the following entity data |
| 41 | `summon minecraft:item 8 120 50 {Item:{id:"thaumcraft:itemgirdlehover",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:26] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Thaumostatic Girdle |
| 42 | `data get entity @e[type=minecraft:item,x=8,y=120,z=50,distance=..1,limit=1]` | ok — [20:06:26] [Server thread/INFO] [minecraft/MinecraftServer]: Thaumostatic Girdle has the following entity data: {NoGravi |
| 43 | `summon minecraft:item 10 120 50 {Item:{id:"thaumcraft:itemcompassstone",count:1},Age:0s,NoGravity:1b}` | ok — [20:06:26] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Sinister Lodestone |
| 44 | `data get entity @e[type=minecraft:item,x=10,y=120,z=50,distance=..1,limit=1]` | ok — [20:06:26] [Server thread/INFO] [minecraft/MinecraftServer]: Sinister Lodestone has the following entity data: {NoGravit |

## L3-datapack — Mod-provided data pack loaded and re-loadable  [PASS]

The port's 199 recipe rows + research graph are datapack data; in ATM10 AlmostUnified and KubeJS also touch data, so load + a clean reload is the cheapest positive check that ours survived.

| # | command | result |
| --- | --- | --- |
| 1 | `datapack list enabled` | ok — [20:06:26] [Server thread/INFO] [minecraft/MinecraftServer]: There are 31 data pack(s) enabled: [create:dynamic_data (bu |
| 2 | `reload` | ok — [20:06:50] [Server thread/INFO] [minecraft/RecipeManager]: Loaded 95567 recipes |

## L4-wandfoci — Wand/foci registry + wanstead pedestal tile  [PASS]

The card depends on TC-M6-03-s; this places the wand pedestal and resolves all 14 wand/foci item ids the dependency registered against the real pack registries.

| # | command | result |
| --- | --- | --- |
| 1 | `summon minecraft:item 4 120 64 {Item:{id:"thaumcraft:wandcasting",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:02] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.wandcasting |
| 2 | `data get entity @e[type=minecraft:item,x=4,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:02] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.wandcasting has the following entity data:  |
| 3 | `summon minecraft:item 6 120 64 {Item:{id:"thaumcraft:wandrod",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:03] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.wandrod |
| 4 | `data get entity @e[type=minecraft:item,x=6,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:03] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.wandrod has the following entity data: {NoG |
| 5 | `summon minecraft:item 8 120 64 {Item:{id:"thaumcraft:wandcap",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:03] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new item.thaumcraft.wandcap |
| 6 | `data get entity @e[type=minecraft:item,x=8,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:03] [Server thread/INFO] [minecraft/MinecraftServer]: item.thaumcraft.wandcap has the following entity data: {NoG |
| 7 | `summon minecraft:item 10 120 64 {Item:{id:"thaumcraft:focusfire",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:04] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Fire |
| 8 | `data get entity @e[type=minecraft:item,x=10,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:04] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Fire has the following entity data: {NoGravity: |
| 9 | `summon minecraft:item 12 120 64 {Item:{id:"thaumcraft:focusshock",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:04] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Shock |
| 10 | `data get entity @e[type=minecraft:item,x=12,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:05] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Shock has the following entity data: {NoGravity |
| 11 | `summon minecraft:item 14 120 64 {Item:{id:"thaumcraft:focushellbat",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:05] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Nine Hells |
| 12 | `data get entity @e[type=minecraft:item,x=14,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:05] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Nine Hells has the following entity data: {NoGr |
| 13 | `summon minecraft:item 16 120 64 {Item:{id:"thaumcraft:focusfrost",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:05] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Frost |
| 14 | `data get entity @e[type=minecraft:item,x=16,y=120,z=64,distance=..1,limit=1]` | ok — [20:07:05] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Frost has the following entity data: {NoGravity |
| 15 | `summon minecraft:item 4 120 66 {Item:{id:"thaumcraft:focustrade",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:05] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Equal Trade |
| 16 | `data get entity @e[type=minecraft:item,x=4,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Equal Trade has the following entity data: {NoG |
| 17 | `summon minecraft:item 6 120 66 {Item:{id:"thaumcraft:focusexcavation",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Excavation |
| 18 | `data get entity @e[type=minecraft:item,x=6,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Excavation has the following entity data: {NoGr |
| 19 | `summon minecraft:item 8 120 66 {Item:{id:"thaumcraft:focusportablehole",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Portable Hole |
| 20 | `data get entity @e[type=minecraft:item,x=8,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Portable Hole has the following entity data: {N |
| 21 | `summon minecraft:item 10 120 66 {Item:{id:"thaumcraft:focuspech",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Pech's Curse |
| 22 | `data get entity @e[type=minecraft:item,x=10,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:06] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Pech's Curse has the following entity data: {No |
| 23 | `summon minecraft:item 12 120 66 {Item:{id:"thaumcraft:focuswarding",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Warding |
| 24 | `data get entity @e[type=minecraft:item,x=12,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Warding has the following entity data: {NoGravi |
| 25 | `summon minecraft:item 14 120 66 {Item:{id:"thaumcraft:focusprimal",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Wand Focus: Primal |
| 26 | `data get entity @e[type=minecraft:item,x=14,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Wand Focus: Primal has the following entity data: {NoGravit |
| 27 | `summon minecraft:item 16 120 66 {Item:{id:"thaumcraft:focuspouch",count:1},Age:0s,NoGravity:1b}` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Summoned new Focus Pouch |
| 28 | `data get entity @e[type=minecraft:item,x=16,y=120,z=66,distance=..1,limit=1]` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Focus Pouch has the following entity data: {NoGravity: 1b,  |
| 29 | `setblock 24 120 4 thaumcraft:blockstonedevice[variant=9]` | ok — [20:07:07] [Server thread/INFO] [minecraft/MinecraftServer]: Changed the block at 24, 120, 4 |
| 30 | `data get block 24 120 4` | ok — [20:07:08] [Server thread/INFO] [minecraft/MinecraftServer]: 24, 120, 4 has the following block data: {x: 24, y: 120, It |

## L5-command — Thaumcraft command surface + live research graph  [PASS]

`/thaumcraft help` proves the command registers next to 470 mods; `/thaumcraft help research list` prints ResearchManager.graph(), so the shipped 201-entry research graph is proven live in-instance, not just parsed. Step 3 PINS a defect: the research/aspect/warp commands are nested under `help`, so the `/thaumcraft research <...>` path the help text advertises is unreachable.

| # | command | result |
| --- | --- | --- |
| 1 | `thaumcraft help` | ok — [20:07:08] [Server thread/INFO] [minecraft/MinecraftServer]: You can also use /thaum or /tc instead of /thaumcraft. |
| 2 | `thaumcraft help research list` | ok — [20:07:08] [Server thread/INFO] [minecraft/MinecraftServer]: BASICTHAUMATURGY |
| 3 | `thaumcraft research list` | ok — [20:07:08] [Server thread/INFO] [minecraft/MinecraftServer]: Incorrect argument for command |

## Live research graph in-instance

- `thaumcraft help research list` printed 201 keys (201 distinct); sample ['ADVALCHEMYFURNACE', 'ADVANCEDGOLEM', 'ALCHEMICALDUPLICATION', 'ALCHEMICALMANUFACTURE', 'ALUMENTUM']
- shipped graph on disk: 6 categories / 201 entries (`src/main/resources/data/thaumcraft/research_graph/`)

## Conflict triage

No mod was deleted, disabled or renamed for this run: the pack was booted as shipped, and the counts below are the whole boot + battery log.

- ERROR/FATAL lines in the whole run: 229
- Exception lines: 146
- attributable to `thaumcraft`: 0
- `thaumcraft` recipe parse errors: 0
- mixin apply failures: 0

Boot-log ERROR/FATAL lines grouped by the mod that logged them — the conflict inventory. Every entry is another mod's own noise unless the `thaumcraft` line above is non-zero:

| mod | ERROR/FATAL lines |
| --- | --- |
| `Worker-Main-1` | 83 |
| `Worker-ResourceReload-2` | 83 |
| `modloading-sync-worker` | 32 |
| `main` | 16 |
| `Server thread` | 9 |
| `Worker-Main-3` | 2 |
| `Worker-ResourceReload-4` | 2 |
| `Worker-Main-7` | 1 |
| `Worker-ResourceReload-6` | 1 |
