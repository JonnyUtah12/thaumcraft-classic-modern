# Original content inventory — Thaumcraft 4.2.3.5

Derived mechanically from the decompiled tree at `../reference/decompiled/thaumcraft`
(859 `.java` covering all 942 JAR `.class` entries) plus the JAR's embedded
`assets/thaumcraft/lang/en_US.lang` for display names. Registration entry points:
`common/config/ConfigBlocks.java`, `ConfigItems.java`, `ConfigEntities.java`,
`ConfigResearch.java`, `ConfigRecipes.java`, `Config.java` (config options,
enchantments, potions, biomes, dimension), `common/Thaumcraft.java` (worldgen,
dimension), `api/aspects/Aspect.java` (aspects), `api/nodes/NodeType.java` /
`NodeModifier.java` (node types), `client/ClientProxy.java` (GUI handler).
Extraction script: `extract.py` (kept outside the repo; output reconciled by hand).
Companion machine-readable form: `content-mapping.csv` (one row per entry).
Class-to-system map: `original-system-map.md`.

## Summary

| Category | Count | Source |
|---|---|---|
| Aspects | 48 | api/aspects/Aspect.java static finals |
| Blocks | 45 | ConfigBlocks.java GameRegistry.registerBlock |
| Fluids | 4 | ConfigBlocks.java Fluid registrations |
| Tile entities | 73 | ConfigBlocks.java registerTileEntity calls |
| Items | 108 | ConfigItems.java GameRegistry.registerItem |
| Entities (network-registered) | 50 | ConfigEntities.java EntityRegistry.registerModEntity |
| Spawn eggs | 29 | ConfigEntities.java ItemSpawnerEgg entries |
| Mob spawn entries | 5 | ConfigEntities.java addSpawn |
| Wand caps/rods/staffs | 24 | Config.java WAND_CAP_*/WAND_ROD_*/STAFF_ROD_* |
| Research categories | 6 | ConfigResearch.java registerCategory |
| Research entries | 201 | ConfigResearch.java new ResearchItem + registerResearchItem |
| Recipes | 279 | ConfigRecipes.java (all recipe types) |
| Wand block triggers | 11 | ConfigRecipes.java registerWandBlockTrigger |
| Config options | 62 | Config.java config.get calls (only file using config.get) |
| GUI ids | 17 | common/lib/RefGui.java + ClientProxy GuiHandler |
| Enchantments registered | 2 | Config.java (Haste, Repair) |
| Enchantment classes (unregistered) | 3 | common/lib/enchantment (Frugal, Potency, WandFortune — dead code) |
| Potion effects | 9 | Config.java <Class>.instance = new <Class>(...) |
| Biomes | 4 | Config.java BiomeGen* registration |
| Dimensions | 1 | Thaumcraft.java DimensionManager (Outer Lands, default -42) |
| Node types | 6 | api/nodes/NodeType enum |
| Node modifiers | 3 | api/nodes/NodeModifier enum |
| Worldgen features | 16 | ThaumcraftWorldGenerator.java + MazeHandler + Village*Manager |
| OreDict entries | 42 | ConfigRecipes.java OreDictionary.registerOre |

## 1. Aspects (48) — `api/aspects/Aspect.java`

Exact list of `public static final Aspect` fields. `tag` is the aspect id used in
essentia strings and icon filenames (`textures/aspects/<tag>.png`).
| Aspect | Tag | Components | Color |
|---|---|---|---|
| AIR | aer | (base) | #FFFF7E |
| EARTH | terra | (base) | #56C000 |
| FIRE | ignis | (base) | #FF5A01 |
| WATER | aqua | (base) | #3CD4FC |
| ORDER | ordo | (base) | #D5D4EC |
| ENTROPY | perditio | (base) | #404040 |
| VOID | vacuos | (base) | #888888 |
| LIGHT | lux | (base) | #FFF663 |
| WEATHER | tempestas | (base) | #FFFFFF |
| MOTION | motus | (base) | #CDCCF4 |
| COLD | gelum | (base) | #E1FFFF |
| CRYSTAL | vitreus | (base) | #80FFFF |
| LIFE | victus | (base) | #DE0005 |
| POISON | venenum | (base) | #89F000 |
| ENERGY | potentia | (base) | #C0FFFF |
| EXCHANGE | permutatio | (base) | #578357 |
| METAL | metallum | (base) | #B5B5CD |
| DEATH | mortuus | (base) | #887788 |
| FLIGHT | volatus | (base) | #E7E7D7 |
| DARKNESS | tenebrae | (base) | #222222 |
| SOUL | spiritus | (base) | #EBEBFB |
| HEAL | sano | (base) | #FF2F34 |
| TRAVEL | iter | (base) | #E0585B |
| ELDRITCH | alienis | (base) | #805080 |
| MAGIC | praecantatio | (base) | #9700C0 |
| AURA | auram | (base) | #FFC0FF |
| TAINT | vitium | (base) | #800080 |
| SLIME | limus | (base) | #01F800 |
| PLANT | herba | (base) | #01AC00 |
| TREE | arbor | (base) | #876531 |
| BEAST | bestia | (base) | #9F6409 |
| FLESH | corpus | (base) | #EE478D |
| UNDEAD | exanimis | (base) | #3A4000 |
| MIND | cognitio | (base) | #FFC2B3 |
| SENSES | sensus | (base) | #0FD9FF |
| MAN | humanus | (base) | #FFD7C0 |
| CROP | messis | (base) | #E1B371 |
| MINE | perfodio | (base) | #DCD2D8 |
| TOOL | instrumentum | (base) | #4040EE |
| HARVEST | meto | (base) | #EEAD82 |
| WEAPON | telum | (base) | #C05050 |
| ARMOR | tutamen | (base) | #00C0C0 |
| HUNGER | fames | (base) | #9A0305 |
| GREED | lucrum | (base) | #E6BE44 |
| CRAFT | fabrico | (base) | #809D80 |
| CLOTH | pannus | (base) | #EAEAC2 |
| MECHANISM | machina | (base) | #8080A0 |
| TRAP | vinculum | (base) | #9A8080 |

## 2. Blocks (45) — `ConfigBlocks.java`
`original_id` is the GameRegistry register name (namespace `Thaumcraft:`).
| Register name | Block class | Item class | Lang key |
|---|---|---|---|
| blockFluxGoo | BlockFluxGoo | BlockFluxGooItem | tile.blockFluxGoo.name |
| blockFluxGas | BlockFluxGas | BlockFluxGasItem | tile.blockFluxGas.name |
| blockFluidPure | BlockFluidPure | - | tile.blockFluidPure.name |
| blockFluidDeath | BlockFluidDeath | - | tile.blockFluidDeath.name |
| blockCustomOre | BlockCustomOre | BlockCustomOreItem | tile.blockCustomOre.name |
| blockMagicalLog | BlockMagicalLog | BlockMagicalLogItem | tile.blockMagicalLog.name |
| blockMagicalLeaves | BlockMagicalLeaves | BlockMagicalLeavesItem | tile.blockMagicalLeaves.name |
| blockCustomPlant | BlockCustomPlant | BlockCustomPlantItem | tile.blockCustomPlant.name |
| blockTaint | BlockTaint | BlockTaintItem | tile.blockTaint.name |
| blockTaintFibres | BlockTaintFibres | BlockTaintFibresItem | tile.blockTaintFibres.name |
| blockCosmeticOpaque | BlockCosmeticOpaque | BlockCosmeticOpaqueItem | tile.blockCosmeticOpaque.name |
| blockCosmeticSolid | BlockCosmeticSolid | BlockCosmeticSolidItem | tile.blockCosmeticSolid.name |
| blockCrystal | BlockCrystal | BlockCrystalItem | tile.blockCrystal.name |
| blockTube | BlockTube | BlockTubeItem | tile.blockTube.name |
| blockMetalDevice | BlockMetalDevice | BlockMetalDeviceItem | tile.blockMetalDevice.name |
| blockWoodenDevice | BlockWoodenDevice | BlockWoodenDeviceItem | tile.blockWoodenDevice.name |
| blockStoneDevice | BlockStoneDevice | BlockStoneDeviceItem | tile.blockStoneDevice.name |
| blockMirror | BlockMirror | BlockMirrorItem | tile.blockMirror.name |
| blockTable | BlockTable | BlockTableItem | tile.blockTable.name |
| blockChestHungry | BlockChestHungry | - | tile.blockChestHungry.name |
| blockArcaneDoor | BlockArcaneDoor | - | tile.blockArcaneDoor.name |
| blockLifter | BlockLifter | - | tile.blockLifter.name |
| blockMagicBox | BlockMagicBox | - | tile.blockMagicBox.name |
| blockAlchemyFurnace | BlockAlchemyFurnace | - | tile.blockAlchemyFurnace.name |
| blockJar | BlockJar | BlockJarItem | tile.blockJar.name |
| blockCandle | BlockCandle | BlockCandleItem | tile.blockCandle.name |
| blockEldritch | BlockEldritch | BlockEldritchItem | tile.blockEldritch.name |
| blockAiry | BlockAiry | BlockAiryItem | tile.blockAiry.name |
| blockManaPod | BlockManaPod | - | tile.blockManaPod.name |
| blockArcaneFurnace | BlockArcaneFurnace | - | tile.blockArcaneFurnace.name |
| blockWarded | BlockWarded | - | tile.blockWarded.name |
| blockHole | BlockHole | - | tile.blockHole.name |
| blockPortalEldritch | BlockEldritchPortal | - | tile.blockPortalEldritch.name |
| blockEssentiaReservoir | BlockEssentiaReservoir | BlockEssentiaReservoirItem | tile.blockEssentiaReservoir.name |
| blockEldritchNothing | BlockEldritchNothing | - | tile.blockPortalNothing.name |
| blockStairsArcaneStone | BlockCosmeticStairs | - | tile.blockStairsArcaneStone.name |
| blockStairsGreatwood | BlockCosmeticStairs | - | tile.blockStairsGreatwood.name |
| blockStairsSilverwood | BlockCosmeticStairs | - | tile.blockStairsSilverwood.name |
| blockStairsEldritch | BlockCosmeticStairs | - | tile.blockStairsEldritch.name |
| blockCosmeticSlabWood | BlockCosmeticWoodSlab | BlockCosmeticWoodSlabItem | tile.blockCosmeticSlabWood.name |
| blockCosmeticSlabStone | BlockCosmeticStoneSlab | BlockCosmeticStoneSlabItem | tile.blockCosmeticSlabStone.name |
| blockCosmeticDoubleSlabWood | BlockCosmeticWoodSlab | - | tile.blockCosmeticDoubleSlabWood.name |
| blockCosmeticDoubleSlabStone | BlockCosmeticStoneSlab | - | tile.blockCosmeticDoubleSlabStone.name |
| blockLootUrn | BlockLoot | BlockLootItem | tile.blockLootUrn.name |
| blockLootCrate | BlockLoot | BlockLootItem | tile.blockLootCrate.name |

## 3. Fluids (4)
| Name | Class |
|---|---|
| fluxGoo | Fluid |
| fluxGas | Fluid |
| fluidPure | Fluid |
| fluidDeath | Fluid |

## 4. Tile entities (73) — registered via `ConfigBlocks.registerTileEntities`
| Tile id / class |
|---|
| TileHole |
| TileArcaneFurnace |
| TileArcaneFurnaceNozzle |
| TileCrucible |
| TileAlembic |
| TileJarFillable |
| TileJarFillableVoid |
| TileJarNode |
| TileJarBrain |
| TileResearchTable |
| TileTable |
| TileArcaneWorkbench |
| TileChestHungry |
| TileBellows |
| TileSensor |
| TileArcanePressurePlate |
| TileArcaneBore |
| TileArcaneBoreBase |
| TileArcaneLamp |
| TileArcaneLampGrowth |
| TileArcaneLampFertility |
| TileOwned |
| TileLifter |
| TileNitor |
| TileNode |
| TileCrystal |
| TileEldritchCrystal |
| TileMirror |
| TileMirrorEssentia |
| TilePedestal |
| TileWandPedestal |
| TileInfusionMatrix |
| TileInfusionPillar |
| TileAlchemyFurnace |
| TileAlchemyFurnaceAdvanced |
| TileAlchemyFurnaceAdvancedNozzle |
| TileDeconstructionTable |
| TileEtherealBloom |
| TileTube |
| TileTubeValve |
| TileCentrifuge |
| TileTubeFilter |
| TileTubeBuffer |
| TileTubeRestrict |
| TileTubeOneway |
| TileThaumatorium |
| TileThaumatoriumTop |
| TileBrainbox |
| TileWarded |
| TileGrate |
| TileWardingStone |
| TileWardingStoneFence |
| TileMagicBox |
| TileNodeStabilizer |
| TileVisRelay |
| TileNodeEnergized |
| TileNodeConverter |
| TileMagicWorkbenchCharger |
| TileManaPod |
| TileSpa |
| TileEldritchAltar |
| TileEldritchObelisk |
| TileEldritchCap |
| TileBanner |
| TileEldritchPortal |
| TileEssentiaReservoir |
| TileEssentiaCrystalizer |
| TileEldritchNothing |
| TileEldritchLock |
| TileEldritchCrabSpawner |
| TileFocalManipulator |
| TileFluxScrubber |
| TileEldritchTrap |

## 5. Items (108) — `ConfigItems.java`
| Register name | Class | Display name (en_US.lang) |
|---|---|---|
| WandCasting | ItemWandCasting |  |
| WandCap | ItemWandCap | Iron Cap / Gold Cap / Charged Thaumium Cap / Copper Cap / Charged Silver Cap / Inert Silver Cap / Inert Thaumium Cap / Charged Void metal Cap / Inert Void metal Cap |
| WandRod | ItemWandRod | Greatwood Rod / Obsidian Rod / Silverwood Rod / Icy Rod / Quartz Rod / Reed Rod / Blazing Rod / Bone Rod / Greatwood Staff Core / Obsidian Staff Core / Silverwood Staff Core / Icy Staff Core / Quartz Staff Core / Reed Staff Core / Blazing Staff Core / Bone Staff Core / Staff Core of the Primal |
| FocusPouch | ItemFocusPouchBauble | Focus Pouch |
| FocusFire | ItemFocusFire | Wand Focus: Fire |
| FocusShock | ItemFocusShock | Wand Focus: Shock |
| FocusHellbat | ItemFocusHellbat | Wand Focus: Nine Hells |
| FocusFrost | ItemFocusFrost | Wand Focus: Frost |
| FocusTrade | ItemFocusTrade | Wand Focus: Equal Trade |
| FocusExcavation | ItemFocusExcavation | Wand Focus: Excavation |
| FocusPortableHole | ItemFocusPortableHole | Wand Focus: Portable Hole |
| FocusPech | ItemFocusPech | Wand Focus: Pech's Curse |
| FocusWarding | ItemFocusWarding | Wand Focus: Warding |
| FocusPrimal | ItemFocusPrimal | Wand Focus: Primal |
| ItemEssence | ItemEssence | Glass Phial / Phial of Essentia |
| ItemManaBean | ItemManaBean | Mana Bean |
| ItemWispEssence | ItemWispEssence | Ethereal Essence |
| ItemResource | ItemResource | Alumentum / Nitor / Thaumium Ingot / Quicksilver / Magic Tallow / Zombie Brain (defunct) / Amber / Enchanted Fabric / Vis Filter / Knowledge Fragment / Mirrored Glass / Tainted Goo / Taint Tendril / Jar Label / Salis Mundus / Primal Charm / Void metal Ingot / Void Seed / Gold Coin |
| ItemShard | ItemShard | Air Shard / Fire Shard / Water Shard / Earth Shard / Order Shard / Entropy Shard / Balanced Shard |
| ItemResearchNotes | ItemResearchNotes | Research Notes |
| ItemInkwell | ItemInkwell | Scribing Tools |
| ItemThaumonomicon | ItemThaumonomicon | Thaumonomicon |
| ItemThaumometer | ItemThaumometer | Thaumometer |
| ItemGoggles | ItemGoggles | Goggles of Revealing |
| ItemHelmetThaumium | ItemThaumiumArmor | Thaumium Helm |
| ItemChestplateThaumium | ItemThaumiumArmor | Thaumium Chestplate |
| ItemLeggingsThaumium | ItemThaumiumArmor | Thaumium Leggings |
| ItemBootsThaumium | ItemThaumiumArmor | Thaumium Boots |
| ItemShovelThaumium | ItemThaumiumShovel | Thaumium Shovel |
| ItemPickThaumium | ItemThaumiumPickaxe | Thaumium Pickaxe |
| ItemAxeThaumium | ItemThaumiumAxe | Thaumium Axe |
| ItemSwordThaumium | ItemThaumiumSword | Thaumium Sword |
| ItemHoeThaumium | ItemThaumiumHoe | Thaumium Hoe |
| ItemArcaneDoor | ItemArcaneDoor | Arcane Door |
| ItemNugget | ItemNugget | Iron Nugget / Copper Nugget / Tin Nugget / Silver Nugget / Lead Nugget / Quicksilver Drop / Thaumium Nugget / Void metal Nugget / Native Iron Cluster / Native Copper Cluster / Native Tin Cluster / Native Silver Cluster / Native Lead Cluster / Native Cinnabar Cluster / Native Gold Cluster |
| BootsTraveller | ItemBootsTraveller | Boots of the Traveler |
| ItemNuggetChicken | ItemNuggetEdible | Chicken Nugget |
| ItemNuggetBeef | ItemNuggetEdible | Beef Nugget |
| ItemNuggetPork | ItemNuggetEdible | Pork Nugget |
| ItemNuggetFish | ItemNuggetEdible | Fish Nugget |
| TripleMeatTreat | ItemTripleMeatTreat | Triple Meat Treat |
| ItemSwordElemental | ItemElementalSword | Sword of the Zephyr |
| ItemShovelElemental | ItemElementalShovel | Shovel of the Earthmover |
| ItemPickaxeElemental | ItemElementalPickaxe | Pickaxe of the Core |
| ItemAxeElemental | ItemElementalAxe | Axe of the Stream |
| ItemHoeElemental | ItemElementalHoe | Hoe of Growth |
| ItemChestplateRobe | ItemRobeArmor | Thaumaturge's Robe |
| ItemLeggingsRobe | ItemRobeArmor | Thaumaturge's Leggings |
| ItemBootsRobe | ItemRobeArmor | Thaumaturge's Boots |
| ArcaneDoorKey | ItemKey | Iron Arcane Key / Gold Arcane Key |
| HandMirror | ItemHandMirror | Magic Hand Mirror |
| HoverHarness | ItemHoverHarness | Thaumostatic Harness |
| BlockJarFilledItem | ItemJarFilled | Jar of Essentia |
| BlockJarNodeItem | ItemJarNode | Node in a Jar |
| TrunkSpawner | ItemTrunkSpawner | Traveling Trunk |
| ItemGolemPlacer | ItemGolemPlacer | Straw Golem / Wood Golem / Tallow Golem / Clay Golem / Flesh Golem / Stone Golem / Iron Golem / Thaumium Golem |
| ItemGolemCore | ItemGolemCore | Golem Animation Core |
| ItemGolemUpgrade | ItemGolemUpgrade | Golem Upgrade: Air / Golem Upgrade: Earth / Golem Upgrade: Fire / Golem Upgrade: Water / Golem Upgrade: Order / Golem Upgrade: Entropy |
| GolemBell | ItemGolemBell | Golemancer's Bell |
| ItemGolemDecoration | ItemGolemDecoration | Accessory |
| ItemBowBone | ItemBowBone | Bow of Bone |
| PrimalArrow | ItemPrimalArrow | Air Arrow / Fire Arrow / Water Arrow / Earth Arrow / Order Arrow / Entropy Arrow |
| ItemResonator | ItemResonator | Essentia Resonator |
| ItemBaubleBlanks | ItemBaubleBlanks | Mundane Amulet / Mundane Ring / Mundane Belt / Apprentices Ring of %TYPE |
| ItemAmuletRunic | ItemAmuletRunic | Amulet of Runic Shielding / Amulet of Emergency Shielding |
| ItemRingRunic | ItemRingRunic | Ring of Protection / Ring of Runic Shielding / Charged Ring of Shielding / Revitalizing Ring of Shielding |
| ItemGirdleRunic | ItemGirdleRunic | Girdle of Runic Shielding / Kinetic Girdle of Shielding |
| ItemAmuletVis | ItemAmuletVis | Vis Stone / Amulet of Vis Storage |
| ItemGirdleHover | ItemGirdleHover | Thaumostatic Girdle |
| ItemSpawnerEgg | ItemSpawnerEgg |  |
| ItemZombieBrain | ItemZombieBrain | Zombie Brain |
| ItemBathSalts | ItemBathSalts | Purifying Bath Salts |
| ItemCrystalEssence | ItemCrystalEssence | Crystallized Essence |
| ItemBucketDeath | ItemBucketDeath | Bucket of Liquid Death |
| ItemBucketPure | ItemBucketPure | Bucket of Purifying Fluid |
| ItemHelmetFortress | ItemFortressArmor | Thaumium Fortress Helm |
| ItemChestplateFortress | ItemFortressArmor | Thaumium Fortress Cuirass |
| ItemLeggingsFortress | ItemFortressArmor | Thaumium Fortress Thigh Guards |
| ItemEldritchObject | ItemEldritchObject | Eldritch Eye / Crimson Rites / Runed Tablet / Primordial Pearl / Eldritch Obelisk Placer |
| ItemHelmetVoid | ItemVoidArmor | Void Helm |
| ItemChestplateVoid | ItemVoidArmor | Void Chestplate |
| ItemLeggingsVoid | ItemVoidArmor | Void Leggings |
| ItemBootsVoid | ItemVoidArmor | Void Boots |
| ItemShovelVoid | ItemVoidShovel | Void Shovel |
| ItemPickVoid | ItemVoidPickaxe | Void Pickaxe |
| ItemAxeVoid | ItemVoidAxe | Void Axe |
| ItemSwordVoid | ItemVoidSword | Void Sword |
| ItemHoeVoid | ItemVoidHoe | Void Hoe |
| ItemHelmetVoidFortress | ItemVoidRobeArmor |  |
| ItemChestplateVoidFortress | ItemVoidRobeArmor |  |
| ItemLeggingsVoidFortress | ItemVoidRobeArmor |  |
| ItemSanitySoap | ItemSanitySoap | Sanitizing Soap |
| ItemSanityChecker | ItemSanityChecker | Sanity Checker |
| ItemBottleTaint | ItemBottleTaint | Bottled Taint |
| ItemHelmetCultistRobe | ItemCultistRobeArmor | Crimson Cult Hood |
| ItemChestplateCultistRobe | ItemCultistRobeArmor | Crimson Cult Robe |
| ItemLeggingsCultistRobe | ItemCultistRobeArmor | Crimson Cult Leggings |
| ItemBootsCultist | ItemCultistBoots | Crimson Cult Boots |
| ItemHelmetCultistPlate | ItemCultistPlateArmor | Crimson Cult Helm |
| ItemChestplateCultistPlate | ItemCultistPlateArmor | Crimson Cult Chestplate |
| ItemLeggingsCultistPlate | ItemCultistPlateArmor | Crimson Cult Greaves |
| ItemHelmetCultistLeaderPlate | ItemCultistLeaderArmor | Crimson Praetor Helm |
| ItemChestplateCultistLeaderPlate | ItemCultistLeaderArmor | Crimson Praetor Chestplate |
| ItemLeggingsCultistLeaderPlate | ItemCultistLeaderArmor | Crimson Praetor Greaves |
| ItemSwordCrimson | ItemCrimsonSword | Crimson Blade |
| ItemLootBag | ItemLootBag | Common Treasure / Uncommon Treasure / Rare Treasure |
| ItemCompassStone | ItemCompassStone | Sinister Lodestone |
| ItemPrimalCrusher | ItemPrimalCrusher | Primal Crusher |

## 6. Entities (50) — `ConfigEntities.java` registerModEntity
| Registered name | Class | Display name | Mod entity index |
|---|---|---|---|
| SpecialItem | EntitySpecialItem | Magic Item | 0 |
| PermanentItem | EntityPermanentItem |  | 1 |
| FollowItem | EntityFollowingItem | Following Item | 2 |
| AspectOrb | EntityAspectOrb | Aspect Orb | 3 |
| FallingTaint | EntityFallingTaint | Tainted Crust | 4 |
| Alumentum | EntityAlumentum | Alumentum | 5 |
| PrimalOrb | EntityPrimalOrb | Orb of Primal Energy | 6 |
| FrostShard | EntityFrostShard | Frost Shard | 7 |
| Dart | EntityDart | Golem Dart | 8 |
| PrimalArrow | EntityPrimalArrow |  | 9 |
| PechBlast | EntityPechBlast |  | 10 |
| EldritchOrb | EntityEldritchOrb | Eldritch Orb | 11 |
| BottleTaint | EntityBottleTaint | Bottled Taint | 12 |
| GolemOrb | EntityGolemOrb | Energy Orb | 13 |
| ShockOrb | EntityShockOrb | Shocking Orb | 14 |
| ExplosiveOrb | EntityExplosiveOrb | Explosive Fireball | 15 |
| Ember | EntityEmber |  | 16 |
| Golem | EntityGolemBase | Golem | 17 |
| TravelingTrunk | EntityTravelingTrunk | Traveling Trunk | 18 |
| BrainyZombie | EntityBrainyZombie | Angry Zombie | 19 |
| GiantBrainyZombie | EntityGiantBrainyZombie | Furious Zombie | 20 |
| Wisp | EntityWisp | Wisp | 21 |
| Firebat | EntityFireBat | Firebat | 22 |
| Pech | EntityPech | Pech Forager | 23 |
| MindSpider | EntityMindSpider | Mind Spider | 24 |
| EldritchGuardian | EntityEldritchGuardian | Eldritch Guardian | 25 |
| EldritchWarden | EntityEldritchWarden | %s the %d | 26 |
| CultistKnight | EntityCultistKnight | Crimson Knight | 27 |
| CultistCleric | EntityCultistCleric | Crimson Cleric | 28 |
| CultistLeader | EntityCultistLeader | Praetor %s the %d | 29 |
| CultistPortal | EntityCultistPortal | Crimson Portal | 30 |
| EldritchGolem | EntityEldritchGolem | %s Eldritch Construct | 31 |
| EldritchCrab | EntityEldritchCrab | Eldritch Crab | 32 |
| InhabitedZombie | EntityInhabitedZombie | Shambling Husk | 33 |
| ThaumSlime | EntityThaumicSlime | Thaumic Slime | 34 |
| TaintSpider | EntityTaintSpider | Tainted Crawler | 35 |
| Taintacle | EntityTaintacle | Taintacle | 36 |
| TaintacleTiny | EntityTaintacleSmall | Taint Tendril | 37 |
| TaintSpore | EntityTaintSpore | Taint Spore | 38 |
| TaintSwarmer | EntityTaintSporeSwarmer | Taint Spore Swarmer | 39 |
| TaintSwarm | EntityTaintSwarm | Taint Swarm | 40 |
| TaintedChicken | EntityTaintChicken | Tainted Chicken | 41 |
| TaintedCow | EntityTaintCow | Tainted Cow | 42 |
| TaintedCreeper | EntityTaintCreeper | Tainted Creeper | 43 |
| TaintedPig | EntityTaintPig | Tainted Pig | 44 |
| TaintedSheep | EntityTaintSheep | Tainted Sheep | 45 |
| TaintedVillager | EntityTaintVillager | Tainted Villager | 46 |
| TaintacleGiant | EntityTaintacleGiant | Giant Taintacle | 47 |
| SpecialItemGrate | EntityItemGrate |  | 48 |
| GolemBobber | EntityGolemBobber | Golem Fishing Bobber | 49 |

### 6.1 Spawn eggs (29)
| Egg for | Egg colors |
|---|---|
| BrainyZombie | #FFC0FF / #008000 |
| GiantBrainyZombie | #FFC0FF / #004000 |
| Wisp | #FFC0FF / #FFFFFF |
| Firebat | #FFC0FF / #C00000 |
| Pech | #FFC0FF / #400040 |
| MindSpider | #AAAAAA / #404040 |
| EldritchGuardian | #222222 / #404040 |
| EldritchWarden | #552222 / #404040 |
| CultistKnight | #FF5055 / #000080 |
| CultistCleric | #FF5055 / #800000 |
| CultistLeader | #FF5055 / #505050 |
| CultistPortal | #FF5055 / #FF50FF |
| EldritchGolem | #555555 / #404040 |
| EldritchCrab | #555555 / #550000 |
| InhabitedZombie | #557755 / #550000 |
| ThaumSlime | #FFC0FF / #FF80FF |
| TaintSpider | #FFC0FF / #404040 |
| Taintacle | #FFC0FF / #800080 |
| TaintacleTiny | #FFC0FF / #800090 |
| TaintSpore | #FFC0FF / #800070 |
| TaintSwarmer | #FFC0FF / #800060 |
| TaintSwarm | #FFC0FF / #800050 |
| TaintedChicken | #FFC0FF / #C0C0C0 |
| TaintedCow | #FFC0FF / #7E3A3B |
| TaintedCreeper | #FFC0FF / #00FF00 |
| TaintedPig | #FFC0FF / #EF99EF |
| TaintedSheep | #FFC0FF / #808080 |
| TaintedVillager | #FFC0FF / #00FFFF |
| TaintacleGiant | #FFC0FF / #808080 |

### 6.2 Natural mob spawns (5)
| Class | Type | Weight | Min | Max |
|---|---|---|---|---|
| EntityBrainyZombie | monster | 10 | 1 | 1 |
| EntityPech | monster | 10 | 1 | 1 |
| EntityFireBat | monster | 10 | 1 | 2 |
| EntityFireBat | monster | 5 | 1 | 2 |
| EntityWisp | monster | 5 | 1 | 1 |

## 7. Wand components (24) — `Config.java`
| Kind | Constant | Component id |
|---|---|---|
| wand_cap | WAND_CAP_IRON | iron |
| wand_cap | WAND_CAP_GOLD | gold |
| wand_cap | WAND_CAP_THAUMIUM | thaumium |
| wand_cap | WAND_CAP_VOID | void |
| wand_cap | WAND_CAP_COPPER | copper |
| wand_cap | WAND_CAP_SILVER | silver |
| wand_rod | WAND_ROD_WOOD | wood |
| wand_rod | WAND_ROD_GREATWOOD | greatwood |
| wand_rod | WAND_ROD_OBSIDIAN | obsidian |
| wand_rod | WAND_ROD_BLAZE | blaze |
| wand_rod | WAND_ROD_ICE | ice |
| wand_rod | WAND_ROD_QUARTZ | quartz |
| wand_rod | WAND_ROD_BONE | bone |
| wand_rod | WAND_ROD_REED | reed |
| wand_rod | WAND_ROD_SILVERWOOD | silverwood |
| staff_rod | STAFF_ROD_GREATWOOD | greatwood |
| staff_rod | STAFF_ROD_OBSIDIAN | obsidian |
| staff_rod | STAFF_ROD_BLAZE | blaze |
| staff_rod | STAFF_ROD_ICE | ice |
| staff_rod | STAFF_ROD_QUARTZ | quartz |
| staff_rod | STAFF_ROD_BONE | bone |
| staff_rod | STAFF_ROD_REED | reed |
| staff_rod | STAFF_ROD_SILVERWOOD | silverwood |
| staff_rod | STAFF_ROD_PRIMAL | primal |

## 8. Research — `ConfigResearch.java`
Categories (6): BASICS, THAUMATURGY, ALCHEMY, ARTIFICE, GOLEMANCY, ELDRITCH

Entries (201). Flags are the setter calls applied to the item.
| Research key | Category | Display name | Flags |
|---|---|---|---|
| BASICTHAUMATURGY | THAUMATURGY | Basic Wand Craft | setAutoUnlock |
| FOCUSFIRE | THAUMATURGY | Wand Foci | - |
| FOCUSFROST | THAUMATURGY | Wand Focus: Frost | - |
| FOCUSHELLBAT | THAUMATURGY | Wand Focus: Nine Hells | setHidden |
| FOCUSEXCAVATION | THAUMATURGY | Wand Focus: Excavation | - |
| FOCUSWARDING | THAUMATURGY | Wand Focus: Warding | - |
| FOCUSSHOCK | THAUMATURGY | Wand Focus: Shock | - |
| FOCUSTRADE | THAUMATURGY | Wand Focus: Equal Trade | - |
| FOCUSPORTABLEHOLE | THAUMATURGY | Wand Focus: Portable Hole | - |
| FOCUSPOUCH | THAUMATURGY | Focus Pouch | - |
| CAP_iron | THAUMATURGY |  | setAutoUnlock |
| CAP_gold | THAUMATURGY | Gold Wand Caps | - |
| CAP_thaumium | THAUMATURGY | Thaumium Wand Caps | - |
| CAP_copper | THAUMATURGY | Copper Wand Caps | - |
| CAP_silver | THAUMATURGY | Silver Wand Caps | - |
| ROD_wood | THAUMATURGY |  | setAutoUnlock |
| ROD_greatwood | THAUMATURGY | Greatwood Wand Core | - |
| ROD_reed | THAUMATURGY | Reed Wand Core | - |
| ROD_blaze | THAUMATURGY | Blaze Rod Wand Core | - |
| ROD_obsidian | THAUMATURGY | Obsidian Wand Core | - |
| ROD_ice | THAUMATURGY | Icy Wand Core | - |
| ROD_quartz | THAUMATURGY | Quartz Wand Core | - |
| ROD_bone | THAUMATURGY | Bone Wand Core | - |
| ROD_silverwood | THAUMATURGY | Silverwood Wand Core | - |
| SCEPTRE | THAUMATURGY | Crafting Scepters | - |
| ROD_greatwood_staff | THAUMATURGY | Magic Staves | - |
| ROD_reed_staff | THAUMATURGY | Reed Staff Core | - |
| ROD_blaze_staff | THAUMATURGY | Blaze Rod Staff Core | - |
| ROD_obsidian_staff | THAUMATURGY | Obsidian Staff Core | - |
| ROD_ice_staff | THAUMATURGY | Icy Staff Core | - |
| ROD_quartz_staff | THAUMATURGY | Quartz Staff Core | - |
| ROD_bone_staff | THAUMATURGY | Bone Staff Core | - |
| ROD_silverwood_staff | THAUMATURGY | Silverwood Staff Core | - |
| WANDPED | THAUMATURGY | Wand Recharge Pedestal | - |
| VISAMULET | THAUMATURGY | Vis Storage | - |
| WANDPEDFOC | THAUMATURGY | Compound Recharge Focus | - |
| NODESTABILIZER | THAUMATURGY | Node Stabilizer | - |
| NODESTABILIZERADV | THAUMATURGY | Advanced Node Stabilizer | - |
| VISPOWER | THAUMATURGY | Harnessing Vis | setSpecial |
| VISCHARGERELAY | THAUMATURGY | Vis Charging Relay | - |
| FOCALMANIPULATION | THAUMATURGY | Focal Manipulation | - |
| VAMPBAT | THAUMATURGY | Tier 5 Upgrade: Vampire Bats | - |
| BASICARTIFACE | ARTIFICE | Basic Artifacing | setAutoUnlock |
| ARCANESTONE | ARTIFICE | Arcane Stone | setAutoUnlock |
| GRATE | ARTIFICE | Item Grate | setAutoUnlock |
| TABLE | ARTIFICE | Table | setAutoUnlock |
| ARCTABLE | ARTIFICE | Arcane Worktable | setAutoUnlock |
| RESTABLE | ARTIFICE | Research Table | setAutoUnlock |
| THAUMOMETER | ARTIFICE | Thaumometer | setAutoUnlock |
| PAVETRAVEL | ARTIFICE | Paving Stone of Travel | - |
| PAVEWARD | ARTIFICE | Paving Stone of Warding | - |
| GOGGLES | ARTIFICE | Goggles of Revealing | - |
| ARCANEEAR | ARTIFICE | Arcane Ear | - |
| SINSTONE | ARTIFICE | Sinister Lodestone | - |
| LEVITATOR | ARTIFICE | Arcane Levitator | - |
| INFERNALFURNACE | ARTIFICE | Infernal Furnace | - |
| BELLOWS | ARTIFICE | Arcane Bellows | - |
| ENCHFABRIC | ARTIFICE | Enchanted Fabric | - |
| INFUSION | ARTIFICE | Infusion | - |
| FLUXSCRUB | ARTIFICE | Flux Scrubber | - |
| RUNICARMOR | ARTIFICE | Runic Shielding | - |
| RUNICCHARGED | ARTIFICE | Charged Ring of Shielding | - |
| RUNICHEALING | ARTIFICE | Revitalizing Ring of Shielding | - |
| RUNICKINETIC | ARTIFICE | Kinetic Girdle of Shielding | - |
| RUNICEMERGENCY | ARTIFICE | Amulet of Emergency Shielding | - |
| BANNERS | ARTIFICE | Banners | setHidden |
| RUNICAUGMENTATION | ARTIFICE | Runic Shielding Augmentation | - |
| BOOTSTRAVELLER | ARTIFICE | Boots of the Traveler | - |
| HOVERHARNESS | ARTIFICE | Thaumostatic Harness | - |
| HOVERGIRDLE | ARTIFICE | Thaumostatic Girdle | setHidden |
| MIRROR | ARTIFICE | Mirror Magic | setHidden |
| MIRRORHAND | ARTIFICE | Magical Hand Mirror | - |
| MIRRORESSENTIA | ARTIFICE | Essentia Mirrors | - |
| ARCANEBORE | ARTIFICE | Arcane Bore | - |
| ARCANELAMP | ARTIFICE | Arcane Lamp | - |
| LAMPGROWTH | ARTIFICE | Lamp of Growth | setHidden |
| LAMPFERTILITY | ARTIFICE | Lamp of Fertility | setHidden |
| BONEBOW | ARTIFICE | Bone Bow | setHidden |
| PRIMALARROW | ARTIFICE | Primal Arrows | - |
| ELEMENTALAXE | ARTIFICE | Axe of the Stream | - |
| ELEMENTALPICK | ARTIFICE | Pickaxe of the Core | - |
| ELEMENTALSWORD | ARTIFICE | Sword of the Zephyr | - |
| ELEMENTALSHOVEL | ARTIFICE | Shovel of the Earthmover | - |
| ELEMENTALHOE | ARTIFICE | Hoe of Growth | - |
| WARDEDARCANA | ARTIFICE | Warded Arcana | - |
| JARBRAIN | ARTIFICE | Brain in a Jar | setHidden |
| INFUSIONENCHANTMENT | ARTIFICE | Infusion Enchantment | - |
| ARMORFORTRESS | ARTIFICE | Thaumium Fortress Armor | setHidden |
| HELMGOGGLES | ARTIFICE | Helm of Revealing | - |
| MASKGRINNINGDEVIL | ARTIFICE | Grinning Devil Faceplate | - |
| MASKANGRYGHOST | ARTIFICE | Angry Ghost Faceplate | - |
| MASKSIPPINGFIEND | ARTIFICE | Sipping Fiend Faceplate | - |
| PHIAL | ALCHEMY | Glass Phial | setAutoUnlock |
| CRUCIBLE | ALCHEMY | Basic Alchemy | setAutoUnlock |
| NITOR | ALCHEMY | Nitor | - |
| ALUMENTUM | ALCHEMY | Alumentum | - |
| ALCHEMICALDUPLICATION | ALCHEMY | Alchemical Duplication | - |
| ALCHEMICALMANUFACTURE | ALCHEMY | Alchemical Manufacture | - |
| ENTROPICPROCESSING | ALCHEMY | Entropic Processing | - |
| LIQUIDDEATH | ALCHEMY | Liquid Death | setHidden |
| BOTTLETAINT | ALCHEMY | Bottled Taint | setHidden |
| THAUMIUM | ALCHEMY | Magical Metallurgy | setHidden |
| PUREIRON | ALCHEMY | Metal Purification | - |
| PUREGOLD | ALCHEMY | Gold Purification | - |
| PURECOPPER | ALCHEMY | Copper Purification | - |
| PURETIN | ALCHEMY | Tin Purification | - |
| PURESILVER | ALCHEMY | Silver Purification | - |
| PURELEAD | ALCHEMY | Lead Purification | - |
| TRANSIRON | ALCHEMY | Metal Transmutation | - |
| TRANSGOLD | ALCHEMY | Gold Transmutation | - |
| TRANSCOPPER | ALCHEMY | Copper Transmutation | - |
| TRANSTIN | ALCHEMY | Tin Transmutation | - |
| TRANSSILVER | ALCHEMY | Silver Transmutation | - |
| TRANSLEAD | ALCHEMY | Lead Transmutation | - |
| TALLOW | ALCHEMY | Magic Tallow | - |
| ETHEREALBLOOM | ALCHEMY | Ethereal Bloom | setHidden |
| BATHSALTS | ALCHEMY | Purifying Bath Salts | setHidden |
| SANESOAP | ALCHEMY | Sanitizing Soap | - |
| ARCANESPA | ALCHEMY | Arcane Spa | - |
| DISTILESSENTIA | ALCHEMY | Essentia Distillation | - |
| JARLABEL | ALCHEMY | Warded Jars and Labels | - |
| JARVOID | ALCHEMY | Void Jar | - |
| TUBES | ALCHEMY | Essentia Tubes | - |
| TUBEFILTER | ALCHEMY | Advanced Essentia Tubes | - |
| ESSENTIACRYSTAL | ALCHEMY | Essentia Crystallization | - |
| CENTRIFUGE | ALCHEMY | Alchemical Centrifuge | - |
| THAUMATORIUM | ALCHEMY | Automated Alchemy | - |
| HUNGRYCHEST | GOLEMANCY | Hungry Chest | - |
| GOLEMFETTER | GOLEMANCY | Golem Fetter | - |
| TRAVELTRUNK | GOLEMANCY | Traveling Trunk | - |
| GOLEMSTRAW | GOLEMANCY | Straw Golems | - |
| GOLEMWOOD | GOLEMANCY | Wooden Golems | - |
| GOLEMTALLOW | GOLEMANCY | Tallow Golems | - |
| GOLEMCLAY | GOLEMANCY | Clay Golems | - |
| GOLEMFLESH | GOLEMANCY | Flesh Golems | - |
| GOLEMSTONE | GOLEMANCY | Stone Golems | - |
| GOLEMIRON | GOLEMANCY | Iron Golems | - |
| GOLEMTHAUMIUM | GOLEMANCY | Thaumium Golems | - |
| GOLEMBELL | GOLEMANCY | Golemancer's Bell | - |
| COREGATHER | GOLEMANCY | Golem Core: Gather | - |
| COREFILL | GOLEMANCY | Golem Core: Fill | - |
| COREEMPTY | GOLEMANCY | Golem Core: Empty | - |
| CORESORTING | GOLEMANCY | Golem Core: Sorting | - |
| COREUSE | GOLEMANCY | Golem Core: Use | - |
| COREHARVEST | GOLEMANCY | Golem Core: Harvest | - |
| COREFISHING | GOLEMANCY | Golem Core: Fishing | - |
| CORELUMBER | GOLEMANCY | Golem Core: Chop | - |
| COREGUARD | GOLEMANCY | Golem Core: Guard | - |
| COREBUTCHER | GOLEMANCY | Golem Core: Butcher | - |
| CORELIQUID | GOLEMANCY | Golem Core: Decanting | - |
| COREALCHEMY | GOLEMANCY | Golem Core: Alchemy | - |
| UPGRADEAIR | GOLEMANCY | Golem Upgrade: Air | - |
| UPGRADEEARTH | GOLEMANCY | Golem Upgrade: Earth | - |
| UPGRADEFIRE | GOLEMANCY | Golem Upgrade: Fire | - |
| UPGRADEWATER | GOLEMANCY | Golem Upgrade: Water | - |
| UPGRADEORDER | GOLEMANCY | Golem Upgrade: Order | - |
| UPGRADEENTROPY | GOLEMANCY | Golem Upgrade: Entropy | - |
| ADVANCEDGOLEM | GOLEMANCY | Advanced Golems | - |
| TINYHAT | GOLEMANCY | Tiny Hats | setHidden |
| TINYGLASSES | GOLEMANCY | Tiny Spectacles | setHidden |
| TINYBOWTIE | GOLEMANCY | Tiny Bowties | setHidden |
| TINYFEZ | GOLEMANCY | Tiny Fezzes | setHidden |
| TINYDART | GOLEMANCY | Golem Dart Launcher | setHidden |
| TINYVISOR | GOLEMANCY | Golem Visor | setHidden |
| TINYARMOR | GOLEMANCY | Golem Iron Plating | setHidden |
| TINYHAMMER | GOLEMANCY | Golem Mace | setHidden |
| ASPECTS | BASICS | Aspects of Magic | setAutoUnlock |
| PECH | BASICS | The Pech | setAutoUnlock |
| NODES | BASICS | Auras and Nodes | setAutoUnlock |
| WARP | BASICS | Warp, Flux and all things bad | setAutoUnlock |
| RESEARCH | BASICS | Research | setAutoUnlock |
| KNOWFRAG | BASICS | Knowledge Fragments | setAutoUnlock |
| THAUMONOMICON | BASICS | Thaumonomicon | setAutoUnlock |
| ORE | BASICS | Ores | setAutoUnlock |
| PLANTS | BASICS | Plants & Trees | setAutoUnlock |
| ENCHANT | BASICS | Enchantments | setAutoUnlock |
| NODETAPPER1 | BASICS | Advanced Node Tapping | - |
| NODEPRESERVE | BASICS | Node Preserver | - |
| NODEJAR | BASICS | Node in a Jar | - |
| NODETAPPER2 | BASICS | Master Node Tapping | setSpecial |
| RESEARCHER1 | BASICS | Research Expertise | - |
| DECONSTRUCTOR | BASICS | Deconstruction table | - |
| RESEARCHER2 | BASICS | Research Mastery | setSpecial |
| RESEARCHDUPE | BASICS | Research Duplication | - |
| CRIMSON | BASICS | The Crimson Cult | setHidden, setSpecial |
| ELDRITCHMINOR | ELDRITCH | Eldritch Epiphany | setHidden, setSpecial |
| ELDRITCHMAJOR | ELDRITCH | Eldritch Revelation | setHidden, setSpecial |
| OCULUS | ELDRITCH | Opening the Eye | setSpecial |
| ENTEROUTER | ELDRITCH | The Outer Lands | setHidden |
| OUTERREV | ELDRITCH | Revelations in the Outer Lands | setSpecial |
| PRIMPEARL | ELDRITCH | Primordial Pearl | setSpecial |
| PRIMNODE | ELDRITCH | Primordial Node Manipulation | - |
| ADVALCHEMYFURNACE | ELDRITCH | Advanced Alchemical Furnace | - |
| PRIMALCRUSHER | ELDRITCH | Primal Crusher | - |
| SANITYCHECK | ELDRITCH | Sanity Check | - |
| VOIDMETAL | ELDRITCH | Void metal | - |
| ESSENTIARESERVOIR | ELDRITCH | Advanced Essentia Storage | - |
| CAP_void | ELDRITCH | Void metal Wand Caps | - |
| ARMORVOIDFORTRESS | ELDRITCH | Void Thaumaturge Robes | - |
| FOCUSPRIMAL | ELDRITCH | Wand Focus: Primal | - |
| ROD_primal_staff | ELDRITCH | Staff Core of the Primal | setHidden |

## 9. Recipes (279) — `ConfigRecipes.java`
By type: arcane_shaped = 84, arcane_shapeless = 5, crafting_shaped = 22, crafting_shapeless = 1, crafting_special = 2, crucible = 49, infusion = 63, infusion_enchantment = 24, multiblock_structure = 10, smelting = 1, smelting_bonus = 18.
Outputs are shown with registered names resolved; `Blocks.field_*`/`Items.field_*` are obfuscated vanilla references.

### 9.arcane_shaped (84)
| # | Output | Research gate |
|---|---|---|
| 1 | Blocks.field_150325_L, 1, a | BANNERS |
| 2 | ItemResource, 1, 15 | BASICARTIFACE |
| 3 | ItemArcaneDoor | WARDEDARCANA |
| 4 | blockCosmeticOpaque, 8, 2 | WARDEDARCANA |
| 5 | ArcaneDoorKey, 2, 0 | WARDEDARCANA |
| 6 | blockStoneDevice, 1, 14 | FLUXSCRUB |
| 7 | ArcaneDoorKey, 2, 1 | WARDEDARCANA |
| 8 | blockWoodenDevice, 1, 2 | WARDEDARCANA |
| 9 | blockStoneDevice, 1, 9 | NODESTABILIZER |
| 10 | blockStoneDevice, 1, 11 | VISPOWER |
| 11 | blockMetalDevice, 2, 14 | VISPOWER |
| 12 | blockMetalDevice, 1, 2 | VISCHARGERELAY |
| 13 | blockStoneDevice, 1, 13 | FOCALMANIPULATION |
| 14 | blockCosmeticSolid, 1, 9 | GOLEMFETTER |
| 15 | blockCosmeticSolid, 9, 6 | ARCANESTONE |
| 16 | blockCosmeticSolid, 4, 2 | PAVETRAVEL |
| 17 | blockMetalDevice, 1, 7 | ARCANELAMP |
| 18 | blockStoneDevice, 1, 12 | ARCANESPA |
| 19 | blockCosmeticSolid, 4, 3 | PAVEWARD |
| 20 | blockLifter | LEVITATOR |
| 21 | blockWoodenDevice, 1, 1 | ARCANEEAR |
| 22 | ItemBowBone | BONEBOW |
| 23 | PrimalArrow, 8, a | PRIMALARROW |
| 24 | blockStoneDevice, 1, 2 | INFUSION |
| 25 | blockStoneDevice, 2, 1 | INFUSION |
| 26 | blockJar, 1, 0 | DISTILESSENTIA |
| 27 | blockJar, 1, 3 | JARVOID |
| 28 | WandCap, 1, 1 | CAP_gold |
| 29 | WandCap, 1, 3 | CAP_copper |
| 30 | WandCap, 1, 5 | CAP_silver |
| 31 | WandCap, 1, 6 | CAP_thaumium |
| 32 | WandCap, 1, 8 | CAP_void |
| 33 | WandRod, 1, 0 | ROD_greatwood |
| 34 | WandRod, 1, 50 | ROD_greatwood_staff |
| 35 | WandRod, 1, 51 | ROD_obsidian_staff |
| 36 | WandRod, 1, 52 | ROD_silverwood_staff |
| 37 | WandRod, 1, 53 | ROD_ice_staff |
| 38 | WandRod, 1, 54 | ROD_quartz_staff |
| 39 | WandRod, 1, 55 | ROD_reed_staff |
| 40 | WandRod, 1, 56 | ROD_blaze_staff |
| 41 | WandRod, 1, 57 | ROD_bone_staff |
| 42 | FocusFire | FOCUSFIRE |
| 43 | FocusFrost | FOCUSFROST |
| 44 | FocusShock | FOCUSSHOCK |
| 45 | FocusTrade | FOCUSTRADE |
| 46 | FocusExcavation | FOCUSEXCAVATION |
| 47 | FocusPrimal | FOCUSPRIMAL |
| 48 | FocusPouch | FOCUSPOUCH |
| 49 | blockTable, 1, 14 | DECONSTRUCTOR |
| 50 | blockWoodenDevice, 1, 4 | ARCANEBORE |
| 51 | ItemResource, 1, 7 | ENCHFABRIC |
| 52 | ItemChestplateRobe, 1 | ENCHFABRIC |
| 53 | ItemLeggingsRobe, 1 | ENCHFABRIC |
| 54 | ItemBootsRobe, 1 | ENCHFABRIC |
| 55 | ItemGoggles | GOGGLES |
| 56 | blockChestHungry | HUNGRYCHEST |
| 57 | GolemBell | GOLEMBELL |
| 58 | ItemGolemCore, 1, 100 | COREGATHER |
| 59 | ItemGolemUpgrade, 1, 0 | UPGRADEAIR |
| 60 | ItemGolemUpgrade, 1, 1 | UPGRADEEARTH |
| 61 | ItemGolemUpgrade, 1, 2 | UPGRADEFIRE |
| 62 | ItemGolemUpgrade, 1, 3 | UPGRADEWATER |
| 63 | ItemGolemUpgrade, 1, 4 | UPGRADEORDER |
| 64 | ItemGolemUpgrade, 1, 5 | UPGRADEENTROPY |
| 65 | ItemGolemDecoration, 1, 0 | TINYHAT |
| 66 | ItemGolemDecoration, 1, 3 | TINYFEZ |
| 67 | ItemGolemDecoration, 1, 2 | TINYBOWTIE |
| 68 | ItemGolemDecoration, 1, 1 | TINYGLASSES |
| 69 | ItemGolemDecoration, 1, 4 | TINYDART |
| 70 | ItemGolemDecoration, 1, 5 | TINYVISOR |
| 71 | ItemGolemDecoration, 1, 6 | TINYARMOR |
| 72 | ItemGolemDecoration, 1, 7 | TINYHAMMER |
| 73 | ItemResource, 2, 8 | DISTILESSENTIA |
| 74 | blockStoneDevice, 1, 0 | DISTILESSENTIA |
| 75 | blockMetalDevice, 1, 1 | DISTILESSENTIA |
| 76 | blockWoodenDevice, 1, 0 | BELLOWS |
| 77 | blockTube, 8, 0 | TUBES |
| 78 | ItemResonator | TUBES |
| 79 | blockTube, 1, 4 | CENTRIFUGE |
| 80 | blockMetalDevice, 1, 9 | DISTILESSENTIA |
| 81 | blockMetalDevice, 4, 3 | ADVALCHEMYFURNACE |
| 82 | blockTube, 1, 2 | CENTRIFUGE |
| 83 | blockTube, 1, 7 | ESSENTIACRYSTAL |
| 84 | blockMetalDevice, 1, 12 | THAUMATORIUM |

### 9.arcane_shapeless (5)
| # | Output | Research gate |
|---|---|---|
| 1 | ItemResource, 1, 10 | BASICARTIFACE |
| 2 | blockTube, 1, 1 | TUBES |
| 3 | blockTube, 1, 3 | TUBEFILTER |
| 4 | blockTube, 1, 5 | TUBEFILTER |
| 5 | blockTube, 1, 6 | TUBEFILTER |

### 9.crafting_shaped (22)
| # | Output | Research gate |
|---|---|---|
| 1 | blockCosmeticSolid, 4, 7 | - |
| 2 | blockStairsArcaneStone, 4, 0 | - |
| 3 | blockCosmeticSlabStone, 6, 0 | - |
| 4 | ItemResearchNotes, 1, 42 | - |
| 5 | blockWoodenDevice, 4, 6 | - |
| 6 | blockWoodenDevice, 4, 7 | - |
| 7 | blockStairsGreatwood, 4, 0 | - |
| 8 | blockStairsSilverwood, 4, 0 | - |
| 9 | blockCosmeticSlabWood, 6, 0 | - |
| 10 | blockCosmeticSlabWood, 6, 1 | - |
| 11 | blockTaint, 1, 2 | - |
| 12 | ItemResource, 9, 2 | - |
| 13 | blockCosmeticSolid, 1, 5 | - |
| 14 | ItemResource, 9, 4 | - |
| 15 | blockMetalDevice, 1, 5 | - |
| 16 | ItemEssence, 8, 0 | - |
| 17 | blockCandle, 3, 0 | - |
| 18 | blockCosmeticOpaque, 1, 0 | - |
| 19 | blockCosmeticOpaque, 4, 1 | - |
| 20 | blockCosmeticSolid, 4, 1 | - |
| 21 | ItemResource, 4, 6 | - |
| 22 | ItemResource, 4, 6 | - |

### 9.crafting_shapeless (1)
| # | Output | Research gate |
|---|---|---|
| 1 | blockCandle, 1, 0 | - |

### 9.crafting_special (2)
| # | Recipe class |
|---|---|
| 1 | RecipesRobeArmorDyes |
| 2 | RecipesVoidRobeArmorDyes |

### 9.crucible (49)
| # | Output | Research gate |
|---|---|---|
| 1 | ItemShard, 1, 6 | CRUCIBLE |
| 2 | ItemResource, 1, 0 | ALUMENTUM |
| 3 | ItemResource, 1, 1 | NITOR |
| 4 | ItemResource, 1, 2 | THAUMIUM |
| 5 | ItemResource, 1, 16 | VOIDMETAL |
| 6 | ItemResource, 1, 17 | VOIDMETAL |
| 7 | ItemResource, 1, 4 | TALLOW |
| 8 | Items.field_151016_H, 2, 0 | ALCHEMICALDUPLICATION |
| 9 | Items.field_151123_aH, 2, 0 | ALCHEMICALDUPLICATION |
| 10 | Items.field_151119_aD, 2, 0 | ALCHEMICALDUPLICATION |
| 11 | Items.field_151114_aO, 2, 0 | ALCHEMICALDUPLICATION |
| 12 | Items.field_151100_aR, 2, 0 | ALCHEMICALDUPLICATION |
| 13 | Blocks.field_150321_G | ALCHEMICALMANUFACTURE |
| 14 | Blocks.field_150341_Y | ALCHEMICALMANUFACTURE |
| 15 | Blocks.field_150432_aD | ALCHEMICALMANUFACTURE |
| 16 | Blocks.field_150417_aV, 1, 2 | ENTROPICPROCESSING |
| 17 | Items.field_151100_aR, 4, 15 | ENTROPICPROCESSING |
| 18 | ItemNugget, 1, 16 | PUREIRON |
| 19 | ItemNugget, 1, 31 | PUREGOLD |
| 20 | ItemNugget, 1, 17 | PURECOPPER |
| 21 | ItemNugget, 1, 18 | PURETIN |
| 22 | ItemNugget, 1, 19 | PURESILVER |
| 23 | ItemNugget, 1, 20 | PURELEAD |
| 24 | ItemNugget, 3, 0 | TRANSIRON |
| 25 | Items.field_151074_bl, 3, 0 | TRANSGOLD |
| 26 | ItemNugget, 3, 1 | TRANSCOPPER |
| 27 | ItemNugget, 3, 2 | TRANSTIN |
| 28 | ItemNugget, 3, 3 | TRANSSILVER |
| 29 | ItemNugget, 3, 4 | TRANSLEAD |
| 30 | blockCustomPlant, 1, 4 | ETHEREALBLOOM |
| 31 | ItemBucketDeath | LIQUIDDEATH |
| 32 | ItemBottleTaint | BOTTLETAINT |
| 33 | ItemGolemPlacer, 1, 0 | GOLEMSTRAW |
| 34 | ItemGolemPlacer, 1, 1 | GOLEMWOOD |
| 35 | ItemGolemPlacer, 1, 2 | GOLEMTALLOW |
| 36 | ItemGolemPlacer, 1, 3 | GOLEMCLAY |
| 37 | ItemGolemPlacer, 1, 4 | GOLEMFLESH |
| 38 | ItemGolemPlacer, 1, 5 | GOLEMSTONE |
| 39 | ItemGolemPlacer, 1, 6 | GOLEMIRON |
| 40 | ItemGolemPlacer, 1, 7 | GOLEMTHAUMIUM |
| 41 | ItemGolemCore, 1, 2 | COREGATHER |
| 42 | ItemGolemCore, 1, 0 | COREFILL |
| 43 | ItemGolemCore, 1, 1 | COREEMPTY |
| 44 | ItemGolemCore, 1, 3 | COREHARVEST |
| 45 | ItemGolemCore, 1, 4 | COREGUARD |
| 46 | ItemGolemCore, 1, 9 | COREBUTCHER |
| 47 | ItemGolemCore, 1, 5 | CORELIQUID |
| 48 | ItemBathSalts | BATHSALTS |
| 49 | ItemSanitySoap | SANESOAP |

### 9.infusion (63)
| # | Output | Research gate |
|---|---|---|
| 1 | WandCap, 1, 4 | CAP_silver |
| 2 | WandCap, 1, 2 | CAP_thaumium |
| 3 | WandCap, 1, 7 | CAP_void |
| 4 | WandRod, 1, 1 | ROD_obsidian |
| 5 | WandRod, 1, 3 | ROD_ice |
| 6 | WandRod, 1, 4 | ROD_quartz |
| 7 | WandRod, 1, 5 | ROD_reed |
| 8 | WandRod, 1, 6 | ROD_blaze |
| 9 | WandRod, 1, 7 | ROD_bone |
| 10 | WandRod, 1, 2 | ROD_silverwood |
| 11 | WandRod, 1, 100 | ROD_primal_staff |
| 12 | FocusHellbat | FOCUSHELLBAT |
| 13 | FocusPortableHole | FOCUSPORTABLEHOLE |
| 14 | FocusWarding | FOCUSWARDING |
| 15 | blockStoneDevice, 1, 5 | WANDPED |
| 16 | blockStoneDevice, 1, 8 | WANDPEDFOC |
| 17 | blockStoneDevice, 1, 10 | NODESTABILIZERADV |
| 18 | blockJar, 1, 1 | JARBRAIN |
| 19 | ItemGolemPlacer, 1, 32767 | ADVANCEDGOLEM |
| 20 | HoverHarness | HOVERHARNESS |
| 21 | ItemGirdleHover | HOVERGIRDLE |
| 22 | ItemAmuletVis, 1, 1 | VISAMULET |
| 23 | ItemAmuletRunic, 1, 0 | RUNICARMOR |
| 24 | ItemAmuletRunic, 1, 1 | RUNICEMERGENCY |
| 25 | ItemRingRunic, 1, 1 | RUNICARMOR |
| 26 | ItemRingRunic, 1, 2 | RUNICCHARGED |
| 27 | ItemRingRunic, 1, 3 | RUNICHEALING |
| 28 | ItemGirdleRunic, 1, 0 | RUNICARMOR |
| 29 | ItemGirdleRunic, 1, 1 | RUNICKINETIC |
| 30 | ItemGirdleRunic, 1, 1 | RUNICKINETIC |
| 31 | blockMirror, 1, 0 | MIRROR |
| 32 | HandMirror | MIRRORHAND |
| 33 | blockMirror, 1, 6 | MIRRORESSENTIA |
| 34 | ItemAxeElemental | ELEMENTALAXE |
| 35 | ItemPickaxeElemental | ELEMENTALPICK |
| 36 | ItemSwordElemental | ELEMENTALSWORD |
| 37 | ItemShovelElemental | ELEMENTALSHOVEL |
| 38 | ItemHoeElemental | ELEMENTALHOE |
| 39 | BootsTraveller | BOOTSTRAVELLER |
| 40 | ItemGolemCore, 1, 6 | COREALCHEMY |
| 41 | ItemGolemCore, 1, 10 | CORESORTING |
| 42 | ItemGolemCore, 1, 7 | CORELUMBER |
| 43 | ItemGolemCore, 1, 11 | COREFISHING |
| 44 | ItemGolemCore, 1, 8 | COREUSE |
| 45 | blockWoodenDevice, 1, 5 | ARCANEBORE |
| 46 | TrunkSpawner | TRAVELTRUNK |
| 47 | blockMetalDevice, 1, 8 | LAMPGROWTH |
| 48 | blockMetalDevice, 1, 13 | LAMPFERTILITY |
| 49 | ItemHelmetFortress | ARMORFORTRESS |
| 50 | ItemChestplateFortress | ARMORFORTRESS |
| 51 | ItemLeggingsFortress | ARMORFORTRESS |
| 52 | ItemHelmetVoidRobe | ARMORVOIDFORTRESS |
| 53 | ItemChestplateVoidRobe | ARMORVOIDFORTRESS |
| 54 | ItemLeggingsVoidRobe | ARMORVOIDFORTRESS |
| 55 | ItemHelmetFortress, 1, 32767 | HELMGOGGLES |
| 56 | ItemHelmetFortress, 1, 32767 | MASKGRINNINGDEVIL |
| 57 | ItemHelmetFortress, 1, 32767 | MASKANGRYGHOST |
| 58 | ItemHelmetFortress, 1, 32767 | MASKSIPPINGFIEND |
| 59 | ItemSanityChecker | SANITYCHECK |
| 60 | blockEssentiaReservoir | ESSENTIARESERVOIR |
| 61 | ItemCompassStone | SINSTONE |
| 62 | ItemPrimalCrusher | PRIMALCRUSHER |
| 63 | ItemEldritchObject | OCULUS |

### 9.infusion_enchantment (24)
| # | Research gate | Enchantment | XP | Aspects |
|---|---|---|---|---|
| 1 | INFUSIONENCHANTMENT | Enchantment.field_77331_b[ThaumcraftApi.enchantRepair] | 4 | .add(Aspect.MAGIC, 8).add(Aspect.CRAFT, 10).add(Aspect.ORDER, 10), new ItemStack[]{new ItemStack(Blocks.field_150467_bQ), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 2 | INFUSIONENCHANTMENT | Enchantment.field_77331_b[ThaumcraftApi.enchantHaste] | 3 | .add(Aspect.MAGIC, 4).add(Aspect.TRAVEL, 8).add(Aspect.FLIGHT, 8), new ItemStack[]{new ItemStack(ConfigItems.itemResource, 1, 1), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 3 | INFUSIONENCHANTMENT | Enchantment.field_77332_c | 1 | .add(Aspect.MAGIC, 4).add(Aspect.ARMOR, 8), new ItemStack[]{new ItemStack(Items.field_151042_j), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 4 | INFUSIONENCHANTMENT | Enchantment.field_77329_d | 1 | .add(Aspect.MAGIC, 4).add(Aspect.ARMOR, 4).add(Aspect.FIRE, 4), new ItemStack[]{new ItemStack(Items.field_151042_j), new ItemStack(Items.field_151064_bs), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 5 | INFUSIONENCHANTMENT | Enchantment.field_77327_f | 1 | .add(Aspect.MAGIC, 4).add(Aspect.ARMOR, 4).add(Aspect.ENTROPY, 4), new ItemStack[]{new ItemStack(Items.field_151042_j), new ItemStack(Items.field_151016_H), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 6 | INFUSIONENCHANTMENT | Enchantment.field_77328_g | 1 | .add(Aspect.MAGIC, 4).add(Aspect.ARMOR, 4).add(Aspect.FLIGHT, 4), new ItemStack[]{new ItemStack(Items.field_151042_j), new ItemStack(Items.field_151032_g), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 7 | INFUSIONENCHANTMENT | Enchantment.field_77330_e | 1 | .add(Aspect.MAGIC, 4).add(Aspect.AIR, 4).add(Aspect.FLIGHT, 4), new ItemStack[]{new ItemStack(Items.field_151008_G), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 8 | INFUSIONENCHANTMENT | Enchantment.field_77340_h | 2 | .add(Aspect.MAGIC, 4).add(Aspect.AIR, 8).add(Aspect.WATER, 8), new ItemStack[]{new ItemStack(Items.field_151120_aE), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 9 | INFUSIONENCHANTMENT | Enchantment.field_77341_i | 2 | .add(Aspect.MAGIC, 4).add(Aspect.MOTION, 8).add(Aspect.WATER, 8), new ItemStack[]{new ItemStack(Items.field_151120_aE), new ItemStack(Items.field_151123_aH), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 10 | INFUSIONENCHANTMENT | Enchantment.field_92091_k | 2 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 8).add(Aspect.PLANT, 8), new ItemStack[]{new ItemStack(Blocks.field_150330_I), new ItemStack(Items.field_151128_bU), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 11 | INFUSIONENCHANTMENT | Enchantment.field_77338_j | 2 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 8), new ItemStack[]{new ItemStack(Items.field_151040_l), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 12 | INFUSIONENCHANTMENT | Enchantment.field_77339_k | 2 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 4).add(Aspect.UNDEAD, 4), new ItemStack[]{new ItemStack(Items.field_151040_l), new ItemStack(Items.field_151114_aO), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 13 | INFUSIONENCHANTMENT | Enchantment.field_77336_l | 2 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 4).add(Aspect.BEAST, 4), new ItemStack[]{ new ItemStack(Items.field_151040_l), new ItemStack(ConfigItems.itemResource, 1, 6), new ItemStack(ConfigItems.itemResource, 1, 14) } ) |
| 14 | INFUSIONENCHANTMENT | Enchantment.field_77337_m | 1 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 3).add(Aspect.MOTION, 3), new ItemStack[]{new ItemStack(Blocks.field_150331_J), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 15 | INFUSIONENCHANTMENT | Enchantment.field_77334_n | 3 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 4).add(Aspect.FIRE, 8), new ItemStack[]{new ItemStack(Items.field_151040_l), new ItemStack(Items.field_151065_br), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 16 | INFUSIONENCHANTMENT | Enchantment.field_77335_o | 3 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 4).add(Aspect.GREED, 8), new ItemStack[]{new ItemStack(Items.field_151040_l), new ItemStack(Items.field_151045_i), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 17 | INFUSIONENCHANTMENT | Enchantment.field_77349_p | 2 | .add(Aspect.MAGIC, 4).add(Aspect.TOOL, 4).add(Aspect.ORDER, 4), new ItemStack[]{new ItemStack(Items.field_151035_b), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 18 | INFUSIONENCHANTMENT | Enchantment.field_77348_q | 5 | .add(Aspect.MAGIC, 16).add(Aspect.TOOL, 16).add(Aspect.ORDER, 16).add(Aspect.HARVEST, 16).add(Aspect.MINE, 16), new ItemStack[]{new ItemStack(Items.field_151035_b), new ItemStack(Blocks.field_150321_G), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 19 | INFUSIONENCHANTMENT | Enchantment.field_77347_r | 2 | .add(Aspect.MAGIC, 4).add(Aspect.TOOL, 4).add(Aspect.ORDER, 8), new ItemStack[]{new ItemStack(Items.field_151035_b), new ItemStack(Blocks.field_150343_Z), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 20 | INFUSIONENCHANTMENT | Enchantment.field_77346_s | 3 | .add(Aspect.MAGIC, 4).add(Aspect.TOOL, 4).add(Aspect.GREED, 8), new ItemStack[]{new ItemStack(Items.field_151035_b), new ItemStack(Items.field_151045_i), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 21 | INFUSIONENCHANTMENT | Enchantment.field_77345_t | 2 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 8), new ItemStack[]{new ItemStack(Items.field_151031_f), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 22 | INFUSIONENCHANTMENT | Enchantment.field_77344_u | 2 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 3).add(Aspect.MOTION, 3), new ItemStack[]{new ItemStack(Blocks.field_150331_J), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 23 | INFUSIONENCHANTMENT | Enchantment.field_77343_v | 3 | .add(Aspect.MAGIC, 4).add(Aspect.WEAPON, 4).add(Aspect.FIRE, 8), new ItemStack[]{new ItemStack(Items.field_151031_f), new ItemStack(Items.field_151065_br), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |
| 24 | INFUSIONENCHANTMENT | Enchantment.field_77342_w | 5 | .add(Aspect.MAGIC, 8).add(Aspect.WEAPON, 16).add(Aspect.VOID, 16).add(Aspect.EXCHANGE, 16), new ItemStack[]{new ItemStack(Items.field_151031_f), new ItemStack(Items.field_151032_g), new ItemStack(ConfigItems.itemResource, 1, 14)} ) |

### 9.multiblock_structure (10)
| # | Structure gate |
|---|---|
| 1 | Thaumonomicon |
| 2 | ArcTable |
| 3 | Crucible |
| 4 | JarLabel |
| 5 | JarLabelNull |
| 6 | WandCapIron |
| 7 | VoidHelm |
| 8 | VoidChest |
| 9 | VoidLegs |
| 10 | VoidBoots |

### 9.smelting (1)
| # | Output | Research gate |
|---|---|---|
| 1 | blockMagicalLog, Items.field_151044_h, 1, 1), 0.5F | - |

### 9.smelting_bonus (18)
| # | Output | Research gate |
|---|---|---|
| 1 | Items.field_151074_bl, 0, 0) | - |
| 2 | ItemNugget, 0, 0) | - |
| 3 | ItemNugget, 0, 5) | - |
| 4 | ItemNugget, 0, 1) | - |
| 5 | ItemNugget, 0, 2) | - |
| 6 | ItemNugget, 0, 3) | - |
| 7 | ItemNugget, 0, 4) | - |
| 8 | Items.field_151074_bl, 0, 0) | - |
| 9 | ItemNugget, 0, 0) | - |
| 10 | ItemNugget, 0, 5) | - |
| 11 | ItemNugget, 0, 1) | - |
| 12 | ItemNugget, 0, 2) | - |
| 13 | ItemNugget, 0, 3) | - |
| 14 | ItemNugget, 0, 4) | - |
| 15 | ItemNuggetChicken) | - |
| 16 | ItemNuggetBeef) | - |
| 17 | ItemNuggetPork) | - |
| 18 | ItemNuggetFish) | - |

## 10. Wand block triggers (11) — `WandTriggerRegistry.registerWandBlockTrigger` in ConfigRecipes.java
Arguments: (manager, event, block, meta, modid). `Blocks.field_*` are obfuscated vanilla blocks.
| # | Call |
|---|---|
| 1 | `Thaumcraft.proxy.wandManager, 0, Blocks.field_150342_X, 0, "Thaumcraft"` |
| 2 | `Thaumcraft.proxy.wandManager, 1, Blocks.field_150383_bp, -1, "Thaumcraft"` |
| 3 | `Thaumcraft.proxy.wandManager, 2, Blocks.field_150343_Z, -1, "Thaumcraft"` |
| 4 | `Thaumcraft.proxy.wandManager, 2, Blocks.field_150385_bj, -1, "Thaumcraft"` |
| 5 | `Thaumcraft.proxy.wandManager, 2, Blocks.field_150411_aY, -1, "Thaumcraft"` |
| 6 | `Thaumcraft.proxy.wandManager, 3, ConfigBlocks.blockStoneDevice, 2, "Thaumcraft"` |
| 7 | `Thaumcraft.proxy.wandManager, 4, Blocks.field_150359_w, -1, "Thaumcraft"` |
| 8 | `Thaumcraft.proxy.wandManager, 5, ConfigBlocks.blockMetalDevice, 9, "Thaumcraft"` |
| 9 | `Thaumcraft.proxy.wandManager, 6, ConfigBlocks.blockEldritch, 0, "Thaumcraft"` |
| 10 | `Thaumcraft.proxy.wandManager, 7, ConfigBlocks.blockMetalDevice, 3, "Thaumcraft"` |
| 11 | `Thaumcraft.proxy.wandManager, 7, ConfigBlocks.blockMetalDevice, 9, "Thaumcraft_2"` |

## 11. Config options (62) — `Config.java` (the only file calling `config.get`)
| Category | Key | Default |
|---|---|---|
| Biomes | taint_biome_weight | 2 |
| Biomes | biome_taint | biomeTaintID |
| Biomes | magical_forest_biome_weight | 5 |
| Biomes | biome_magical_forest | biomeMagicalForestID |
| Biomes | biome_eerie | biomeEerieID |
| Biomes | biome_eldritch | biomeEldritchID |
| Biomes | outer_lands_dim | dimensionOuterId |
| Enchantments | ench_haste | encIdx++ |
| Enchantments | ench_repair | encIdx++ |
| World_Generation | generate_aura_nodes | true |
| World_Generation | generate_structures | true |
| World_Generation | generate_cinnibar_ore | true |
| World_Generation | generate_amber_ore | true |
| World_Generation | generate_infused_stone | true |
| World_Generation | generate_trees | true |
| World_Generation | generate_taint | genTaint |
| World_Regeneration | regen_key | "DEFAULT" |
| World_Regeneration | aura_nodes | false |
| World_Regeneration | structures | false |
| World_Regeneration | cinnibar_ore | false |
| World_Regeneration | amber_ore | false |
| World_Regeneration | infused_stone | false |
| World_Regeneration | trees | false |
| World_Regeneration | taint | false |
| Research | research_difficulty | 0 |
| Research | aspect_total_cap | 100 |
| Monster_Spawning | spawn_angry_zombies | true |
| Monster_Spawning | spawn_fire_bats | true |
| Monster_Spawning | spawn_wisps | true |
| Monster_Spawning | spawn_taintacles | true |
| Monster_Spawning | spawn_taint_spores | true |
| Monster_Spawning | spawn_pechs | true |
| Monster_Spawning | spawn_eldercreatures | true |
| Monster_Spawning | champion_mobs | championMobs |
| general | allow_mirrors | allowMirrors |
| general | color_blind | colorBlind |
| general | shaders | shaders |
| general | crooked | crooked |
| general | hard_mode_nodes | hardNode |
| general | wuss_mode | wuss |
| general | wand_dial_bottom | dialBottom |
| general | golem_delay | golemDelay |
| general | golem_ignore_delay | golemIgnoreDelay |
| general | golem_link_quality | golemLinkQuality |
| general | notification_delay | notificationDelay |
| general | notification_max | notificationMax |
| general | node_rarity | nodeRarity |
| general | special_node_rarity | specialNodeRarity |
| general | display_aspects | false |
| general | allow_cheat_sheet | false |
| general | allow_warded_stone | true |
| general | thaumcraft_villager_id | ConfigEntities.entWizardId |
| general | thaumcraft_banker_id | ConfigEntities.entBankerId |
| general | golem_chest_interact | true |
| general | portablehole_blacklist | "iron_door" |
| general | blue_magical_forest | blueBiome |
| general | biome_taint_from_flux | taintFromFlux |
| general | biome_taint_spread | taintSpreadRate |
| general | glowing_taint | glowyTaint |
| Runic_Shielding | runic_recharge_speed | shieldRecharge |
| Runic_Shielding | runic_recharge_delay | shieldWait |
| Runic_Shielding | runic_cost | shieldCost |

## 12. GUIs (17 ids) — `common/lib/RefGui.java` + `client/ClientProxy.java` GuiHandler
| Gui id | Constant | Gui class |
|---|---|---|
| 0 | GUI_GOLEM | GuiGolem |
| 1 | GUI_PECH | GuiPech |
| 2 | GUI_TRUNK | GuiTravelingTrunk |
| 3 | GUI_THAUMATORIUM | GuiThaumatorium |
| 5 | GUI_FOCUS_POUCH | GuiFocusPouch |
| 8 | GUI_DECON_TABLE | GuiDeconstructionTable |
| 9 | GUI_ALCHEMY_FURNACE | GuiAlchemyFurnace |
| 10 | GUI_RESEARCH_TABLE | GuiResearchTable |
| 11 | GUI_CHEST_HUNGRY | - |
| 12 | GUI_THAUMONOMICON | GuiResearchBrowser |
| 13 | GUI_ARCANE_WORKBENCH | GuiArcaneWorkbench |
| 15 | GUI_ARCANE_BORE | GuiArcaneBore |
| 16 | GUI_HAND_MIRROR | GuiHandMirror |
| 17 | GUI_HOVER_HARNESS | GuiHoverHarness |
| 18 | GUI_MAGIC_BOX | GuiMagicBox |
| 19 | GUI_SPA | GuiSpa |
| 20 | GUI_FOCAL_MANUPULATOR | GuiFocalManipulator |
Gui classes present in the jar (20): GuiAlchemyFurnace, GuiArcaneBore, GuiArcaneWorkbench, GuiDeconstructionTable, GuiFocalManipulator, GuiFocusPouch, GuiGolem, GuiHandMirror, GuiHoverHarness, GuiMagicBox, GuiPech, GuiResearchBrowser, GuiResearchPopup, GuiResearchRecipe, GuiResearchTable, GuiSpa, GuiThaumatorium, GuiTravelingTrunk, MappingThread, ThaumcraftGuiConfig

## 13. Enchantments
Registered (2, `Config.java`):
| Config key | Class | Max level | Lang name |
|---|---|---|---|
| ench_haste | EnchantmentHaste | 3 | Haste |
| ench_repair | EnchantmentRepair | 2 | Repair |
Present in the jar but NOT registered in 4.2.3.5 (dead code): EnchantmentFrugal, EnchantmentPotency, EnchantmentWandFortune.
The 24 infusion-enchantment recipes (§9.infusion_enchantment) apply vanilla enchantments via the infusion altar; they are not new enchantment definitions.

## 14. Potion effects (9) — `Config.java`
| Class | Lang key | Display name | Bad | Color |
|---|---|---|---|---|
| PotionFluxTaint | potion.fluxtaint | Taint Poison | true | #663377 |
| PotionVisExhaust | potion.visexhaust | Flux Flu | true | #664477 |
| PotionInfectiousVisExhaust | potion.infvisexhaust | Flux Phage | true | #665577 |
| PotionUnnaturalHunger | potion.unhunger | Unnatural Hunger | true | #446633 |
| PotionWarpWard | potion.warpward | Warp Ward | false | #E0F2F7 |
| PotionDeathGaze | potion.deathgaze | Deadly Gaze | true | #664433 |
| PotionBlurredVision | potion.blurred | Blurred Vision | true | #808080 |
| PotionSunScorned | potion.sunscorned | Sun Scorned | true | #F8D86A |
| PotionThaumarhia | potion.thaumarhia | Thaumarhia | true | #664477 |
Note: `potion.soulshatter` exists in en_US.lang but no PotionSoulShatter class exists in 4.2.3.5 — dead lang key.

## 15. Worldgen — `ThaumcraftWorldGenerator.java`, `Config.java`, `Thaumcraft.java`
Biomes (4):
| Class | Default id | Config key |
|---|---|---|
| BiomeGenTaint | 192 | taint_biome_id |
| BiomeGenMagicalForest | 193 | magical_forest_biome_id |
| BiomeGenEerie | 194 | eerie_biome_id |
| BiomeGenEldritch | 195 | eldritch_biome_id |
Dimension: Outer Lands, `WorldProviderOuter`, default id -42 (config `outer_lands_dim`), registered in `common/Thaumcraft.java`.
Node types (6): NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY, PURE. Node modifiers (3): BRIGHT, PALE, FADING.
Features (16):
| Feature | Implementation | Config gate |
|---|---|---|
| Silverwood trees | WorldGenSilverwoodTrees via generateVegetation | genTrees / regenTrees |
| Greatwood trees | WorldGenGreatwoodTrees via generateVegetation | genTrees / regenTrees |
| Shimmerleaf / flowers | WorldGenCustomFlowers (blockCustomPlant meta 3) | genTrees |
| Cinnabar ore | WorldGenMinable blockCustomOre meta 0 | genCinnibar / regenCinnibar |
| Infused stone (6 elemental variants) | WorldGenMinable blockCustomOre meta 1-6 | genInfusedStone / regenInfusedStone |
| Amber ore | WorldGenMinable blockCustomOre meta 7 | genAmber / regenAmber |
| Wild vis aura nodes | generateWildNodes (TileNode) | genAura / regenAura / nodeRarity |
| Golem totems | generateTotem | genStructure / regenStructure |
| Shrine mounds | WorldGenMound | genStructure |
| Eldritch stone rings | WorldGenEldritchRing | genStructure |
| Hilltop stones | WorldGenHilltopStones | genStructure |
| Nether: totems + wild nodes | generateNether | genAura / genStructure |
| Outer Lands procedural maze | MazeHandler.generateEldritch (dimension -42) | outer_lands_dim |
| Taint spread / conversion | TaintManager (chunk events, not chunk gen) | - |
| Wizard tower village house | ComponentWizardTower via VillageWizardManager | villager_wizard spawn |
| Banker house village house | ComponentBankerHome via VillageBankerManager | villager_banker spawn |

## 16. OreDict registrations (42) — `ConfigRecipes.java`
| Entry | Stack |
|---|---|
| oreCinnabar | blockCustomOre, 1, 0) |
| oreInfusedAir | blockCustomOre, 1, 1) |
| oreInfusedFire | blockCustomOre, 1, 2) |
| oreInfusedWater | blockCustomOre, 1, 3) |
| oreInfusedEarth | blockCustomOre, 1, 4) |
| oreInfusedOrder | blockCustomOre, 1, 5) |
| oreInfusedEntropy | blockCustomOre, 1, 6) |
| shardAir | ItemShard, 1, 0) |
| shardFire | ItemShard, 1, 1) |
| shardWater | ItemShard, 1, 2) |
| shardEarth | ItemShard, 1, 3) |
| shardOrder | ItemShard, 1, 4) |
| shardEntropy | ItemShard, 1, 5) |
| oreAmber | blockCustomOre, 1, 7) |
| quicksilver | ItemResource, 1, 3) |
| gemAmber | ItemResource, 1, 6) |
| nuggetIron | ItemNugget, 1, 0) |
| nuggetCopper | ItemNugget, 1, 1) |
| nuggetTin | ItemNugget, 1, 2) |
| nuggetSilver | ItemNugget, 1, 3) |
| nuggetLead | ItemNugget, 1, 4) |
| nuggetThaumium | ItemNugget, 1, 6) |
| nuggetVoid | ItemNugget, 1, 7) |
| nuggetQuicksilver | ItemResource, 1, 3) |
| nuggetGold | ItemResource, 1, 18) |
| ingotThaumium | ItemResource, 1, 2) |
| ingotVoid | ItemResource, 1, 16) |
| clusterIron | ItemNugget, 1, 16) |
| clusterCopper | ItemNugget, 1, 17) |
| clusterTin | ItemNugget, 1, 18) |
| clusterSilver | ItemNugget, 1, 19) |
| clusterLead | ItemNugget, 1, 20) |
| clusterCinnabar | ItemNugget, 1, 21) |
| clusterGold | ItemNugget, 1, 31) |
| logWood | blockMagicalLog, 1, 0) |
| logWood | blockMagicalLog, 1, 1) |
| plankWood | blockWoodenDevice, 1, 6) |
| plankWood | blockWoodenDevice, 1, 7) |
| slabWood | blockCosmeticSlabWood, 1, 0) |
| slabWood | blockCosmeticSlabWood, 1, 1) |
| treeSapling | blockCustomPlant, 1, 0) |
| treeSapling | blockCustomPlant, 1, 1) |

## Verification
- **Block lang-name coverage (entries with no name key in the original en_US.lang are listed)** — PASS. 39/45 have lang names; no lang name in original: blockMagicBox, blockManaPod, blockWarded, blockEldritchNothing, blockCosmeticDoubleSlabWood, blockCosmeticDoubleSlabStone
- **Item lang-name coverage (entries with no name key in the original en_US.lang are listed)** — PASS. 103/108 have lang names; no lang name in original: WandCasting, ItemSpawnerEgg, ItemHelmetVoidFortress, ItemChestplateVoidFortress, ItemLeggingsVoidFortress
- **Entity lang-name coverage (entries with no name key in the original en_US.lang are listed)** — PASS. 45/50 have lang names; no lang name in original: PermanentItem, PrimalArrow, PechBlast, Ember, SpecialItemGrate
- **Research lang-name coverage (entries with no name key in the original en_US.lang are listed)** — PASS. 199/201 have lang names; no lang name in original: CAP_iron, ROD_wood (stub auto-unlock entries registered without icon or name; display falls back to the raw key)
- **Every aspect tag has a textures/aspects/<tag>.png icon** — PASS. icons on disk: 50 (48 tags + _back + _unknown utility icons)
- **Research item count matches constructor and register calls** — PASS. new ResearchItem( = 201, .registerResearchItem() = 201, extracted = 201
- **Recipe count matches ConfigResearch.recipes.put count** — PASS. recipes.put = 279, extracted = 279
- **Decompiled tree matches JAR class entries** — PASS. 859 .java files covering all 942 .class entries (inner classes merged; verified by TC-M0-02, re-counted here)
- **JAR asset entries match baseline** — PASS. 1107 entries under assets/ (1091 under assets/thaumcraft + 16 under assets/minecraft shaders); 942 .class entries — re-verified with unzip -Z1
- Cross-check against sibling project `thaumaturgical-legacy` inventory (TL-003): package map,
  registration entry points, and resource counts agree (its "50 aspect icons" = 48 aspect tags
  + `_back` + `_unknown` utility icons; its catalog is representative, this document is exact).
- No enumeration in this document uses "etc." — every list is complete.
