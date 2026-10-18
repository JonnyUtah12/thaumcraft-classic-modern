# Original system map — Thaumcraft 4.2.3.5

Every one of the 859 decompiled `.java` files (covering all 942 JAR class entries)
mapped to the gameplay/technical system it belongs to. Generated mechanically by
`classify.py` (package rules + class-name rules, first match wins) and reviewed by
hand; the rule script lives outside the repo. Companion content inventory:
`original-content-inventory.md`.

## Systems

| System | Files | Purpose |
|---|---|---|
| Rendering & client effects | 211 | Renderers, particle/effect classes, client helpers. |
| Alchemy, essentia & flux devices | 46 | Crucible, alembic, thaumatorium, reservoirs, jars, tubes, furnaces, flux scrubber, spa. |
| Mob AI | 43 | AI task classes for TC mobs and golems. |
| GUI & containers | 41 | All Gui* screens and Container*/Slot* classes. |
| Bundled: CodeChickenLib pieces | 41 | Bundled CodeChicken core/lib incl. DepLoader coremod. |
| Networking & packets | 40 | Packet pipeline and per-packet classes (S2C/C2S sync). |
| Decorative & utility blocks | 35 | Cosmetic/slab/stair blocks, devices blocks, lamps, lifters, jars step sounds, utility tiles. |
| Portals, mirrors & eldritch | 32 | Eldritch blocks/items, portals, arcane mirrors, loot, keys, guardians. |
| Mobs & monsters | 31 | Hostile/neutral mob entities (wisp, pech, firebat, cultists, watchers, faces...). |
| World generation (biomes, nodes, structures, villages) | 28 | World generator, biomes, ore/tree/flower gen, village houses, structures. |
| Taint system | 20 | Taint blocks, taint mobs, taint spread manager, taint items. |
| Golems & traveling trunk | 19 | Golem entities, golem devices/plates, traveling trunk. |
| Equipment: tools & weapons | 19 | Elemental/thaumium/void tools, bone bow, crimson sword, primal crusher. |
| Portals, mirrors & eldritch (Outer Lands) | 17 | Outer Lands dimension provider, procedural maze generation. |
| API: core (ThaumcraftApi, helpers) | 16 | Public API surface: ThaumcraftApi, ItemApi, interfaces, annotations. |
| Arcane crafting & devices | 13 | Arcane workbench, deconstruction table, bore, magic box, hungry chest, focal manipulator. |
| Equipment: armor | 13 | Robes, fortress/cultist/traveller armor, goggles, hover logic. |
| Vis nodes & aura | 12 | Node tiles, node jars, crystal blocks, compass stone, node devices. |
| Equipment: wand foci | 12 | Focus items (excavation, fire, frost, hellbat, pech, portable hole, primal, shock, trade, warding, pouch). |
| Research & scanning (research table & GUI) | 10 | Research table block/tile/GUI, inkwell. |
| API: wands | 10 | WandCap/WandRod/focus interfaces, WandTriggerRegistry. |
| Enchantments & runic shielding | 9 | Enchantment classes, runic armor handling, warded blocks. |
| Core support (tabs, refs, misc) | 9 | Creative tab, GUI id refs, sound types, misc helpers. |
| Potions, warp & effects | 9 | Potion effect classes and warp event handling. |
| Utilities & helpers | 9 | Block/inventory/entity/crop utils, vector helpers. |
| Bootstrap: registration & config | 9 | Config file handling and registration of every block, item, entity, research entry and recipe. |
| Entity support (special items, spawner eggs) | 9 | Special item entities, following items, mob inventory, spawner egg item. |
| Projectiles | 9 | Alumentum, primal arrow/orb, ember, dart entities and dispenser behavior. |
| Wands & vis | 8 | Wand items, WandManager vis tasks, resonator, vis relay, pedestal. |
| Materials & food items | 8 | Resource/nugget/shard/essence items, food items. |
| Events, commands & tick handlers | 7 | Forge event handlers, chat commands, tick loops (aura, warp, taint). |
| API: aspects | 7 | Aspect model, AspectList, essentia container interfaces. |
| API: crafting | 7 | Arcane/crucible/infusion recipe types and interfaces. |
| Equipment: baubles | 6 | Runic amulet/ring/girdle, vis amulet, hover girdle, blanks. |
| API: research | 6 | ResearchItem/ResearchPage/ResearchCategories model. |
| Crafting engine & recipe types | 5 | Recipe managers (arcane workbench, crucible, furnace) and special recipes. |
| Research & scanning | 4 | ResearchManager, PlayerKnowledge, scan handling. |
| API: vis nodes | 4 |  |
| Bundled: TrueType font renderer | 4 | Bundled truetyper font rendering library. |
| Mod core & lifecycle | 3 | @Mod entry point, proxies, pre/init/post lifecycle. |
| Research & scanning (items) | 3 | Thaumonomicon, research notes, thaumometer. |
| API: internal bridge | 3 | Internal method bridge used by the API to call implementation code. |
| Infusion crafting | 2 | Infusion matrix/pillar tiles. |
| Equipment: relics & curios | 2 | Relic items (hand mirror, sanity checker). |
| API: damage sources | 2 | Thaumcraft damage source classes. |
| API: potions | 2 | Potion base classes (PotionFluxTaint, PotionVisExhaust). |
| API: vis network | 2 | VisNetHandler, TileVisNode network model. |
| API: entities | 2 | Marker interfaces for eldritch and tainted mobs (IEldritchMob, ITaintedMob). |

Total: 859 files across 48 systems.

## Rendering & client effects (211)
Renderers, particle/effect classes, client helpers.
- `thaumcraft/client/ThaumcraftGuiFactory.java`
- `thaumcraft/client/fx/ParticleEngine.java`
- `thaumcraft/client/fx/WRMat4.java`
- `thaumcraft/client/fx/WRVector3.java`
- `thaumcraft/client/fx/beams/FXArc.java`
- `thaumcraft/client/fx/beams/FXBeam.java`
- `thaumcraft/client/fx/beams/FXBeamBore.java`
- `thaumcraft/client/fx/beams/FXBeamGolemBoss.java`
- `thaumcraft/client/fx/beams/FXBeamPower.java`
- `thaumcraft/client/fx/beams/FXBeamWand.java`
- `thaumcraft/client/fx/bolt/FXLightningBolt.java`
- `thaumcraft/client/fx/bolt/FXLightningBoltCommon.java`
- `thaumcraft/client/fx/other/FXBlockWard.java`
- `thaumcraft/client/fx/other/FXShieldRunes.java`
- `thaumcraft/client/fx/other/FXSonic.java`
- `thaumcraft/client/fx/particles/FXBlockRunes.java`
- `thaumcraft/client/fx/particles/FXBoreParticles.java`
- `thaumcraft/client/fx/particles/FXBoreSparkle.java`
- `thaumcraft/client/fx/particles/FXBreaking.java`
- `thaumcraft/client/fx/particles/FXBubble.java`
- `thaumcraft/client/fx/particles/FXBubbleAlt.java`
- `thaumcraft/client/fx/particles/FXBurst.java`
- `thaumcraft/client/fx/particles/FXDrop.java`
- `thaumcraft/client/fx/particles/FXEssentiaTrail.java`
- `thaumcraft/client/fx/particles/FXGeneric.java`
- `thaumcraft/client/fx/particles/FXScorch.java`
- `thaumcraft/client/fx/particles/FXSlimyBubble.java`
- `thaumcraft/client/fx/particles/FXSmokeSpiral.java`
- `thaumcraft/client/fx/particles/FXSmokeTrail.java`
- `thaumcraft/client/fx/particles/FXSpark.java`
- `thaumcraft/client/fx/particles/FXSparkle.java`
- `thaumcraft/client/fx/particles/FXSparkleTrail.java`
- `thaumcraft/client/fx/particles/FXSwarm.java`
- `thaumcraft/client/fx/particles/FXVent.java`
- `thaumcraft/client/fx/particles/FXVisSparkle.java`
- `thaumcraft/client/fx/particles/FXWisp.java`
- `thaumcraft/client/fx/particles/FXWispArcing.java`
- `thaumcraft/client/fx/particles/FXWispEG.java`
- `thaumcraft/client/lib/ClientTickEventsFML.java`
- `thaumcraft/client/lib/PlayerNotifications.java`
- `thaumcraft/client/lib/QuadHelper.java`
- `thaumcraft/client/lib/REHNotifyHandler.java`
- `thaumcraft/client/lib/REHWandHandler.java`
- `thaumcraft/client/lib/RenderEventHandler.java`
- `thaumcraft/client/lib/TCFontRenderer.java`
- `thaumcraft/client/lib/UtilsFX.java`
- `thaumcraft/client/renderers/block/BlockArcaneFurnaceRenderer.java`
- `thaumcraft/client/renderers/block/BlockCandleRenderer.java`
- `thaumcraft/client/renderers/block/BlockChestHungryRenderer.java`
- `thaumcraft/client/renderers/block/BlockCosmeticOpaqueRenderer.java`
- `thaumcraft/client/renderers/block/BlockCrystalRenderer.java`
- `thaumcraft/client/renderers/block/BlockCustomOreRenderer.java`
- `thaumcraft/client/renderers/block/BlockEldritchRenderer.java`
- `thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java`
- `thaumcraft/client/renderers/block/BlockGasRenderer.java`
- `thaumcraft/client/renderers/block/BlockJarRenderer.java`
- `thaumcraft/client/renderers/block/BlockLifterRenderer.java`
- `thaumcraft/client/renderers/block/BlockLootCrateRenderer.java`
- `thaumcraft/client/renderers/block/BlockLootUrnRenderer.java`
- `thaumcraft/client/renderers/block/BlockMetalDeviceRenderer.java`
- `thaumcraft/client/renderers/block/BlockRenderer.java`
- `thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java`
- `thaumcraft/client/renderers/block/BlockTableRenderer.java`
- `thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java`
- `thaumcraft/client/renderers/block/BlockTaintRenderer.java`
- `thaumcraft/client/renderers/block/BlockTubeRenderer.java`
- `thaumcraft/client/renderers/block/BlockWardedRenderer.java`
- `thaumcraft/client/renderers/block/BlockWoodenDeviceRenderer.java`
- `thaumcraft/client/renderers/entity/RenderAlumentum.java`
- `thaumcraft/client/renderers/entity/RenderAspectOrb.java`
- `thaumcraft/client/renderers/entity/RenderBrainyZombie.java`
- `thaumcraft/client/renderers/entity/RenderCultist.java`
- `thaumcraft/client/renderers/entity/RenderCultistPortal.java`
- `thaumcraft/client/renderers/entity/RenderDart.java`
- `thaumcraft/client/renderers/entity/RenderEldritchCrab.java`
- `thaumcraft/client/renderers/entity/RenderEldritchGolem.java`
- `thaumcraft/client/renderers/entity/RenderEldritchGuardian.java`
- `thaumcraft/client/renderers/entity/RenderEldritchOrb.java`
- `thaumcraft/client/renderers/entity/RenderElectricOrb.java`
- `thaumcraft/client/renderers/entity/RenderEmber.java`
- `thaumcraft/client/renderers/entity/RenderExplosiveOrb.java`
- `thaumcraft/client/renderers/entity/RenderFallingTaint.java`
- `thaumcraft/client/renderers/entity/RenderFireBat.java`
- `thaumcraft/client/renderers/entity/RenderFollowingItem.java`
- `thaumcraft/client/renderers/entity/RenderFrostShard.java`
- `thaumcraft/client/renderers/entity/RenderGolemBase.java`
- `thaumcraft/client/renderers/entity/RenderGolemBobber.java`
- `thaumcraft/client/renderers/entity/RenderInhabitedZombie.java`
- `thaumcraft/client/renderers/entity/RenderMindSpider.java`
- `thaumcraft/client/renderers/entity/RenderPech.java`
- `thaumcraft/client/renderers/entity/RenderPechBlast.java`
- `thaumcraft/client/renderers/entity/RenderPrimalArrow.java`
- `thaumcraft/client/renderers/entity/RenderPrimalOrb.java`
- `thaumcraft/client/renderers/entity/RenderSpecialItem.java`
- `thaumcraft/client/renderers/entity/RenderTaintChicken.java`
- `thaumcraft/client/renderers/entity/RenderTaintCow.java`
- `thaumcraft/client/renderers/entity/RenderTaintCreeper.java`
- `thaumcraft/client/renderers/entity/RenderTaintPig.java`
- `thaumcraft/client/renderers/entity/RenderTaintSheep.java`
- `thaumcraft/client/renderers/entity/RenderTaintSpider.java`
- `thaumcraft/client/renderers/entity/RenderTaintSpore.java`
- `thaumcraft/client/renderers/entity/RenderTaintSporeSwarmer.java`
- `thaumcraft/client/renderers/entity/RenderTaintSwarm.java`
- `thaumcraft/client/renderers/entity/RenderTaintVillager.java`
- `thaumcraft/client/renderers/entity/RenderTaintacle.java`
- `thaumcraft/client/renderers/entity/RenderThaumicSlime.java`
- `thaumcraft/client/renderers/entity/RenderTravelingTrunk.java`
- `thaumcraft/client/renderers/entity/RenderWatcher.java`
- `thaumcraft/client/renderers/entity/RenderWisp.java`
- `thaumcraft/client/renderers/item/ItemBannerRenderer.java`
- `thaumcraft/client/renderers/item/ItemBowBoneRenderer.java`
- `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java`
- `thaumcraft/client/renderers/item/ItemTrunkSpawnerRenderer.java`
- `thaumcraft/client/renderers/item/ItemWandRenderer.java`
- `thaumcraft/client/renderers/models/ModelArcaneWorkbench.java`
- `thaumcraft/client/renderers/models/ModelBanner.java`
- `thaumcraft/client/renderers/models/ModelBellows.java`
- `thaumcraft/client/renderers/models/ModelBore.java`
- `thaumcraft/client/renderers/models/ModelBoreBase.java`
- `thaumcraft/client/renderers/models/ModelBoreEmit.java`
- `thaumcraft/client/renderers/models/ModelBrain.java`
- `thaumcraft/client/renderers/models/ModelCentrifuge.java`
- `thaumcraft/client/renderers/models/ModelCrystal.java`
- `thaumcraft/client/renderers/models/ModelCube.java`
- `thaumcraft/client/renderers/models/ModelFireBat.java`
- `thaumcraft/client/renderers/models/ModelGolem.java`
- `thaumcraft/client/renderers/models/ModelGolemAccessories.java`
- `thaumcraft/client/renderers/models/ModelJar.java`
- `thaumcraft/client/renderers/models/ModelManaPod.java`
- `thaumcraft/client/renderers/models/ModelPech.java`
- `thaumcraft/client/renderers/models/ModelRendererTaintacle.java`
- `thaumcraft/client/renderers/models/ModelResearchTable.java`
- `thaumcraft/client/renderers/models/ModelTable.java`
- `thaumcraft/client/renderers/models/ModelTaintSheep1.java`
- `thaumcraft/client/renderers/models/ModelTaintSheep2.java`
- `thaumcraft/client/renderers/models/ModelTaintSpore.java`
- `thaumcraft/client/renderers/models/ModelTaintSporeSwarmer.java`
- `thaumcraft/client/renderers/models/ModelTaintacle.java`
- `thaumcraft/client/renderers/models/ModelTrunk.java`
- `thaumcraft/client/renderers/models/ModelTubeValve.java`
- `thaumcraft/client/renderers/models/ModelVoidRobe.java`
- `thaumcraft/client/renderers/models/entities/ModelEldritchCrab.java`
- `thaumcraft/client/renderers/models/entities/ModelEldritchGolem.java`
- `thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java`
- `thaumcraft/client/renderers/models/entities/ModelFireBat.java`
- `thaumcraft/client/renderers/models/entities/ModelGolem.java`
- `thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java`
- `thaumcraft/client/renderers/models/entities/ModelPech.java`
- `thaumcraft/client/renderers/models/entities/ModelRendererTaintacle.java`
- `thaumcraft/client/renderers/models/entities/ModelTaintSheep1.java`
- `thaumcraft/client/renderers/models/entities/ModelTaintSheep2.java`
- `thaumcraft/client/renderers/models/entities/ModelTaintSpore.java`
- `thaumcraft/client/renderers/models/entities/ModelTaintSporeSwarmer.java`
- `thaumcraft/client/renderers/models/entities/ModelTaintacle.java`
- `thaumcraft/client/renderers/models/entities/ModelTrunk.java`
- `thaumcraft/client/renderers/models/entities/ModelWatcher.java`
- `thaumcraft/client/renderers/models/gear/ModelFortressArmor.java`
- `thaumcraft/client/renderers/models/gear/ModelHoverHarness.java`
- `thaumcraft/client/renderers/models/gear/ModelKnightArmor.java`
- `thaumcraft/client/renderers/models/gear/ModelLeaderArmor.java`
- `thaumcraft/client/renderers/models/gear/ModelRobe.java`
- `thaumcraft/client/renderers/models/gear/ModelWand.java`
- `thaumcraft/client/renderers/tile/ItemJarFilledRenderer.java`
- `thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java`
- `thaumcraft/client/renderers/tile/ItemNodeRenderer.java`
- `thaumcraft/client/renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java`
- `thaumcraft/client/renderers/tile/TileAlembicRenderer.java`
- `thaumcraft/client/renderers/tile/TileArcaneBoreBaseRenderer.java`
- `thaumcraft/client/renderers/tile/TileArcaneBoreRenderer.java`
- `thaumcraft/client/renderers/tile/TileArcaneLampRenderer.java`
- `thaumcraft/client/renderers/tile/TileArcaneWorkbenchRenderer.java`
- `thaumcraft/client/renderers/tile/TileBannerRenderer.java`
- `thaumcraft/client/renderers/tile/TileBellowsRenderer.java`
- `thaumcraft/client/renderers/tile/TileCentrifugeRenderer.java`
- `thaumcraft/client/renderers/tile/TileChestHungryRenderer.java`
- `thaumcraft/client/renderers/tile/TileCrucibleRenderer.java`
- `thaumcraft/client/renderers/tile/TileCrystalRenderer.java`
- `thaumcraft/client/renderers/tile/TileDeconstructionTableRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchCapRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchCrabSpawnerRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchLockRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchObeliskRenderer.java`
- `thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java`
- `thaumcraft/client/renderers/tile/TileEssentiaCrystalizerRenderer.java`
- `thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java`
- `thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java`
- `thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java`
- `thaumcraft/client/renderers/tile/TileFocalManipulatorRenderer.java`
- `thaumcraft/client/renderers/tile/TileHoleRenderer.java`
- `thaumcraft/client/renderers/tile/TileInfusionPillarRenderer.java`
- `thaumcraft/client/renderers/tile/TileJarRenderer.java`
- `thaumcraft/client/renderers/tile/TileMagicWorkbenchChargerRenderer.java`
- `thaumcraft/client/renderers/tile/TileManaPodRenderer.java`
- `thaumcraft/client/renderers/tile/TileMirrorRenderer.java`
- `thaumcraft/client/renderers/tile/TileNodeConverterRenderer.java`
- `thaumcraft/client/renderers/tile/TileNodeEnergizedRenderer.java`
- `thaumcraft/client/renderers/tile/TileNodeRenderer.java`
- `thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java`
- `thaumcraft/client/renderers/tile/TilePedestalRenderer.java`
- `thaumcraft/client/renderers/tile/TileResearchTableRenderer.java`
- `thaumcraft/client/renderers/tile/TileRunicMatrixRenderer.java`
- `thaumcraft/client/renderers/tile/TileTableRenderer.java`
- `thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java`
- `thaumcraft/client/renderers/tile/TileTubeBufferRenderer.java`
- `thaumcraft/client/renderers/tile/TileTubeOnewayRenderer.java`
- `thaumcraft/client/renderers/tile/TileTubeValveRenderer.java`
- `thaumcraft/client/renderers/tile/TileVisRelayRenderer.java`
- `thaumcraft/client/renderers/tile/TileWandPedestalRenderer.java`
- `thaumcraft/client/renderers/tile/TileWardedRenderer.java`

## Alchemy, essentia & flux devices (46)
Crucible, alembic, thaumatorium, reservoirs, jars, tubes, furnaces, flux scrubber, spa.
- `thaumcraft/common/blocks/BlockAlchemyFurnace.java`
- `thaumcraft/common/blocks/BlockArcaneFurnace.java`
- `thaumcraft/common/blocks/BlockEssentiaReservoir.java`
- `thaumcraft/common/blocks/BlockEssentiaReservoirItem.java`
- `thaumcraft/common/blocks/BlockFluidDeath.java`
- `thaumcraft/common/blocks/BlockFluidPure.java`
- `thaumcraft/common/blocks/BlockJar.java`
- `thaumcraft/common/blocks/BlockJarItem.java`
- `thaumcraft/common/blocks/BlockManaPod.java`
- `thaumcraft/common/blocks/BlockTube.java`
- `thaumcraft/common/blocks/BlockTubeItem.java`
- `thaumcraft/common/blocks/ItemJarFilled.java`
- `thaumcraft/common/items/ItemBathSalts.java`
- `thaumcraft/common/items/ItemBucketDeath.java`
- `thaumcraft/common/items/ItemBucketPure.java`
- `thaumcraft/common/items/ItemCrystalEssence.java`
- `thaumcraft/common/items/ItemEssence.java`
- `thaumcraft/common/items/ItemManaBean.java`
- `thaumcraft/common/tiles/TileAlchemyFurnace.java`
- `thaumcraft/common/tiles/TileAlchemyFurnaceAdvanced.java`
- `thaumcraft/common/tiles/TileAlchemyFurnaceAdvancedNozzle.java`
- `thaumcraft/common/tiles/TileAlembic.java`
- `thaumcraft/common/tiles/TileArcaneFurnace.java`
- `thaumcraft/common/tiles/TileArcaneFurnaceNozzle.java`
- `thaumcraft/common/tiles/TileBellows.java`
- `thaumcraft/common/tiles/TileCentrifuge.java`
- `thaumcraft/common/tiles/TileCrucible.java`
- `thaumcraft/common/tiles/TileEssentiaCrystalizer.java`
- `thaumcraft/common/tiles/TileEssentiaReservoir.java`
- `thaumcraft/common/tiles/TileEtherealBloom.java`
- `thaumcraft/common/tiles/TileFluxScrubber.java`
- `thaumcraft/common/tiles/TileJar.java`
- `thaumcraft/common/tiles/TileJarFillable.java`
- `thaumcraft/common/tiles/TileJarFillableVoid.java`
- `thaumcraft/common/tiles/TileManaPod.java`
- `thaumcraft/common/tiles/TileNitor.java`
- `thaumcraft/common/tiles/TileSensor.java`
- `thaumcraft/common/tiles/TileSpa.java`
- `thaumcraft/common/tiles/TileThaumatorium.java`
- `thaumcraft/common/tiles/TileThaumatoriumTop.java`
- `thaumcraft/common/tiles/TileTube.java`
- `thaumcraft/common/tiles/TileTubeBuffer.java`
- `thaumcraft/common/tiles/TileTubeFilter.java`
- `thaumcraft/common/tiles/TileTubeOneway.java`
- `thaumcraft/common/tiles/TileTubeRestrict.java`
- `thaumcraft/common/tiles/TileTubeValve.java`

## Mob AI (43)
AI task classes for TC mobs and golems.
- `thaumcraft/common/entities/ai/combat/AIAttackOnCollide.java`
- `thaumcraft/common/entities/ai/combat/AIAvoidCreeperSwell.java`
- `thaumcraft/common/entities/ai/combat/AICreeperSwell.java`
- `thaumcraft/common/entities/ai/combat/AICultistHurtByTarget.java`
- `thaumcraft/common/entities/ai/combat/AIDartAttack.java`
- `thaumcraft/common/entities/ai/combat/AIHurtByTarget.java`
- `thaumcraft/common/entities/ai/combat/AILongRangeAttack.java`
- `thaumcraft/common/entities/ai/combat/AINearestAttackableTarget.java`
- `thaumcraft/common/entities/ai/combat/AINearestAttackableTargetSorter.java`
- `thaumcraft/common/entities/ai/combat/AINearestButcherTarget.java`
- `thaumcraft/common/entities/ai/combat/AIOldestAttackableTargetSorter.java`
- `thaumcraft/common/entities/ai/combat/AITarget.java`
- `thaumcraft/common/entities/ai/fluid/AIEssentiaEmpty.java`
- `thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java`
- `thaumcraft/common/entities/ai/fluid/AIEssentiaGoto.java`
- `thaumcraft/common/entities/ai/fluid/AILiquidEmpty.java`
- `thaumcraft/common/entities/ai/fluid/AILiquidGather.java`
- `thaumcraft/common/entities/ai/fluid/AILiquidGoto.java`
- `thaumcraft/common/entities/ai/interact/AIFish.java`
- `thaumcraft/common/entities/ai/interact/AIHarvestCrops.java`
- `thaumcraft/common/entities/ai/interact/AIHarvestLogs.java`
- `thaumcraft/common/entities/ai/interact/AIUseItem.java`
- `thaumcraft/common/entities/ai/inventory/AIEmptyDrop.java`
- `thaumcraft/common/entities/ai/inventory/AIEmptyGoto.java`
- `thaumcraft/common/entities/ai/inventory/AIEmptyPlace.java`
- `thaumcraft/common/entities/ai/inventory/AIFillGoto.java`
- `thaumcraft/common/entities/ai/inventory/AIFillTake.java`
- `thaumcraft/common/entities/ai/inventory/AIHomeDrop.java`
- `thaumcraft/common/entities/ai/inventory/AIHomePlace.java`
- `thaumcraft/common/entities/ai/inventory/AIHomeReplace.java`
- `thaumcraft/common/entities/ai/inventory/AIHomeTake.java`
- `thaumcraft/common/entities/ai/inventory/AIHomeTakeSorting.java`
- `thaumcraft/common/entities/ai/inventory/AIItemPickup.java`
- `thaumcraft/common/entities/ai/inventory/AISortingGoto.java`
- `thaumcraft/common/entities/ai/inventory/AISortingPlace.java`
- `thaumcraft/common/entities/ai/misc/AIAltarFocus.java`
- `thaumcraft/common/entities/ai/misc/AIConvertGrass.java`
- `thaumcraft/common/entities/ai/misc/AIDoorInteract.java`
- `thaumcraft/common/entities/ai/misc/AIOpenDoor.java`
- `thaumcraft/common/entities/ai/misc/AIReturnHome.java`
- `thaumcraft/common/entities/ai/misc/AIWander.java`
- `thaumcraft/common/entities/ai/pech/AIPechItemEntityGoto.java`
- `thaumcraft/common/entities/ai/pech/AIPechTradePlayer.java`

## GUI & containers (41)
All Gui* screens and Container*/Slot* classes.
- `thaumcraft/client/gui/GuiAlchemyFurnace.java`
- `thaumcraft/client/gui/GuiArcaneBore.java`
- `thaumcraft/client/gui/GuiArcaneWorkbench.java`
- `thaumcraft/client/gui/GuiDeconstructionTable.java`
- `thaumcraft/client/gui/GuiFocalManipulator.java`
- `thaumcraft/client/gui/GuiFocusPouch.java`
- `thaumcraft/client/gui/GuiGolem.java`
- `thaumcraft/client/gui/GuiHandMirror.java`
- `thaumcraft/client/gui/GuiHoverHarness.java`
- `thaumcraft/client/gui/GuiMagicBox.java`
- `thaumcraft/client/gui/GuiPech.java`
- `thaumcraft/client/gui/GuiSpa.java`
- `thaumcraft/client/gui/GuiThaumatorium.java`
- `thaumcraft/client/gui/GuiTravelingTrunk.java`
- `thaumcraft/client/gui/MappingThread.java`
- `thaumcraft/client/gui/ThaumcraftGuiConfig.java`
- `thaumcraft/common/container/ContainerAlchemyFurnace.java`
- `thaumcraft/common/container/ContainerArcaneBore.java`
- `thaumcraft/common/container/ContainerArcaneWorkbench.java`
- `thaumcraft/common/container/ContainerDeconstructionTable.java`
- `thaumcraft/common/container/ContainerDummy.java`
- `thaumcraft/common/container/ContainerFocalManipulator.java`
- `thaumcraft/common/container/ContainerFocusPouch.java`
- `thaumcraft/common/container/ContainerGhostSlots.java`
- `thaumcraft/common/container/ContainerHandMirror.java`
- `thaumcraft/common/container/ContainerHoverHarness.java`
- `thaumcraft/common/container/ContainerMagicBox.java`
- `thaumcraft/common/container/ContainerSpa.java`
- `thaumcraft/common/container/ContainerThaumatorium.java`
- `thaumcraft/common/container/InventoryFake.java`
- `thaumcraft/common/container/InventoryFocusPouch.java`
- `thaumcraft/common/container/InventoryHandMirror.java`
- `thaumcraft/common/container/InventoryHoverHarness.java`
- `thaumcraft/common/container/SlotCraftingArcaneWorkbench.java`
- `thaumcraft/common/container/SlotGhost.java`
- `thaumcraft/common/container/SlotGhostFluid.java`
- `thaumcraft/common/container/SlotLimitedByClass.java`
- `thaumcraft/common/container/SlotLimitedByItemstack.java`
- `thaumcraft/common/container/SlotLimitedByWand.java`
- `thaumcraft/common/container/SlotLimitedHasAspects.java`
- `thaumcraft/common/container/SlotOutput.java`

## Bundled: CodeChickenLib pieces (41)
Bundled CodeChicken core/lib incl. DepLoader coremod.
- `thaumcraft/codechicken/core/launch/DepLoader.java`
- `thaumcraft/codechicken/lib/colour/Colour.java`
- `thaumcraft/codechicken/lib/colour/ColourARGB.java`
- `thaumcraft/codechicken/lib/colour/ColourRGBA.java`
- `thaumcraft/codechicken/lib/lighting/LC.java`
- `thaumcraft/codechicken/lib/lighting/LightMatrix.java`
- `thaumcraft/codechicken/lib/lighting/LightModel.java`
- `thaumcraft/codechicken/lib/lighting/PlanarLightModel.java`
- `thaumcraft/codechicken/lib/math/MathHelper.java`
- `thaumcraft/codechicken/lib/raytracer/ExtendedMOP.java`
- `thaumcraft/codechicken/lib/raytracer/IndexedCuboid6.java`
- `thaumcraft/codechicken/lib/raytracer/RayTracer.java`
- `thaumcraft/codechicken/lib/render/CCModel.java`
- `thaumcraft/codechicken/lib/render/CCModelLibrary.java`
- `thaumcraft/codechicken/lib/render/CCRenderPipeline.java`
- `thaumcraft/codechicken/lib/render/CCRenderState.java`
- `thaumcraft/codechicken/lib/render/Vertex5.java`
- `thaumcraft/codechicken/lib/render/uv/UV.java`
- `thaumcraft/codechicken/lib/render/uv/UVRotation.java`
- `thaumcraft/codechicken/lib/render/uv/UVScale.java`
- `thaumcraft/codechicken/lib/render/uv/UVTransformation.java`
- `thaumcraft/codechicken/lib/render/uv/UVTransformationList.java`
- `thaumcraft/codechicken/lib/render/uv/UVTranslation.java`
- `thaumcraft/codechicken/lib/util/Copyable.java`
- `thaumcraft/codechicken/lib/vec/AxisCycle.java`
- `thaumcraft/codechicken/lib/vec/BlockCoord.java`
- `thaumcraft/codechicken/lib/vec/Cuboid6.java`
- `thaumcraft/codechicken/lib/vec/CuboidCoord.java`
- `thaumcraft/codechicken/lib/vec/ITransformation.java`
- `thaumcraft/codechicken/lib/vec/IrreversibleTransformationException.java`
- `thaumcraft/codechicken/lib/vec/Matrix4.java`
- `thaumcraft/codechicken/lib/vec/Quat.java`
- `thaumcraft/codechicken/lib/vec/RedundantTransformation.java`
- `thaumcraft/codechicken/lib/vec/Rotation.java`
- `thaumcraft/codechicken/lib/vec/Scale.java`
- `thaumcraft/codechicken/lib/vec/SwapYZ.java`
- `thaumcraft/codechicken/lib/vec/Transformation.java`
- `thaumcraft/codechicken/lib/vec/TransformationList.java`
- `thaumcraft/codechicken/lib/vec/Translation.java`
- `thaumcraft/codechicken/lib/vec/VariableTransformation.java`
- `thaumcraft/codechicken/lib/vec/Vector3.java`

## Networking & packets (40)
Packet pipeline and per-packet classes (S2C/C2S sync).
- `thaumcraft/common/lib/network/EventHandlerNetwork.java`
- `thaumcraft/common/lib/network/PacketHandler.java`
- `thaumcraft/common/lib/network/fx/PacketFXBeamPulse.java`
- `thaumcraft/common/lib/network/fx/PacketFXBeamPulseGolemBoss.java`
- `thaumcraft/common/lib/network/fx/PacketFXBlockArc.java`
- `thaumcraft/common/lib/network/fx/PacketFXBlockBubble.java`
- `thaumcraft/common/lib/network/fx/PacketFXBlockDig.java`
- `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java`
- `thaumcraft/common/lib/network/fx/PacketFXBlockZap.java`
- `thaumcraft/common/lib/network/fx/PacketFXEssentiaSource.java`
- `thaumcraft/common/lib/network/fx/PacketFXInfusionSource.java`
- `thaumcraft/common/lib/network/fx/PacketFXShield.java`
- `thaumcraft/common/lib/network/fx/PacketFXSonic.java`
- `thaumcraft/common/lib/network/fx/PacketFXVisDrain.java`
- `thaumcraft/common/lib/network/fx/PacketFXWispZap.java`
- `thaumcraft/common/lib/network/fx/PacketFXZap.java`
- `thaumcraft/common/lib/network/misc/PacketBiomeChange.java`
- `thaumcraft/common/lib/network/misc/PacketBoreDig.java`
- `thaumcraft/common/lib/network/misc/PacketConfig.java`
- `thaumcraft/common/lib/network/misc/PacketFlyToServer.java`
- `thaumcraft/common/lib/network/misc/PacketFocusChangeToServer.java`
- `thaumcraft/common/lib/network/misc/PacketItemKeyToServer.java`
- `thaumcraft/common/lib/network/misc/PacketMiscEvent.java`
- `thaumcraft/common/lib/network/misc/PacketNote.java`
- `thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer.java`
- `thaumcraft/common/lib/network/playerdata/PacketAspectDiscovery.java`
- `thaumcraft/common/lib/network/playerdata/PacketAspectPlaceToServer.java`
- `thaumcraft/common/lib/network/playerdata/PacketAspectPool.java`
- `thaumcraft/common/lib/network/playerdata/PacketPlayerCompleteToServer.java`
- `thaumcraft/common/lib/network/playerdata/PacketResearchComplete.java`
- `thaumcraft/common/lib/network/playerdata/PacketRunicCharge.java`
- `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncAspects.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncResearch.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncScannedEntities.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncScannedItems.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncScannedPhenomena.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncWarp.java`
- `thaumcraft/common/lib/network/playerdata/PacketSyncWipe.java`
- `thaumcraft/common/lib/network/playerdata/PacketWarpMessage.java`

## Decorative & utility blocks (35)
Cosmetic/slab/stair blocks, devices blocks, lamps, lifters, jars step sounds, utility tiles.
- `thaumcraft/common/blocks/BlockArcaneDoor.java`
- `thaumcraft/common/blocks/BlockCandle.java`
- `thaumcraft/common/blocks/BlockCandleItem.java`
- `thaumcraft/common/blocks/BlockCosmeticOpaque.java`
- `thaumcraft/common/blocks/BlockCosmeticOpaqueItem.java`
- `thaumcraft/common/blocks/BlockCosmeticSolid.java`
- `thaumcraft/common/blocks/BlockCosmeticSolidItem.java`
- `thaumcraft/common/blocks/BlockCosmeticStairs.java`
- `thaumcraft/common/blocks/BlockCosmeticStoneSlab.java`
- `thaumcraft/common/blocks/BlockCosmeticStoneSlabItem.java`
- `thaumcraft/common/blocks/BlockCosmeticWoodSlab.java`
- `thaumcraft/common/blocks/BlockCosmeticWoodSlabItem.java`
- `thaumcraft/common/blocks/BlockHole.java`
- `thaumcraft/common/blocks/BlockLifter.java`
- `thaumcraft/common/blocks/BlockMetalDevice.java`
- `thaumcraft/common/blocks/BlockMetalDeviceItem.java`
- `thaumcraft/common/blocks/BlockStoneDevice.java`
- `thaumcraft/common/blocks/BlockStoneDeviceItem.java`
- `thaumcraft/common/blocks/BlockWoodenDevice.java`
- `thaumcraft/common/blocks/BlockWoodenDeviceItem.java`
- `thaumcraft/common/blocks/CustomStepSound.java`
- `thaumcraft/common/blocks/ItemArcaneDoor.java`
- `thaumcraft/common/blocks/JarStepSound.java`
- `thaumcraft/common/tiles/TileArcaneLamp.java`
- `thaumcraft/common/tiles/TileArcaneLampFertility.java`
- `thaumcraft/common/tiles/TileArcaneLampGrowth.java`
- `thaumcraft/common/tiles/TileArcaneLampLight.java`
- `thaumcraft/common/tiles/TileArcanePressurePlate.java`
- `thaumcraft/common/tiles/TileGrate.java`
- `thaumcraft/common/tiles/TileHole.java`
- `thaumcraft/common/tiles/TileLifter.java`
- `thaumcraft/common/tiles/TileMemory.java`
- `thaumcraft/common/tiles/TileOwned.java`
- `thaumcraft/common/tiles/TilePedestal.java`
- `thaumcraft/common/tiles/TileThaumcraftInventory.java`

## Portals, mirrors & eldritch (32)
Eldritch blocks/items, portals, arcane mirrors, loot, keys, guardians.
- `thaumcraft/common/blocks/BlockEldritch.java`
- `thaumcraft/common/blocks/BlockEldritchItem.java`
- `thaumcraft/common/blocks/BlockEldritchNothing.java`
- `thaumcraft/common/blocks/BlockEldritchPortal.java`
- `thaumcraft/common/blocks/BlockFluxGas.java`
- `thaumcraft/common/blocks/BlockFluxGasItem.java`
- `thaumcraft/common/blocks/BlockFluxGoo.java`
- `thaumcraft/common/blocks/BlockFluxGooItem.java`
- `thaumcraft/common/blocks/BlockLoot.java`
- `thaumcraft/common/blocks/BlockLootItem.java`
- `thaumcraft/common/blocks/BlockMirror.java`
- `thaumcraft/common/blocks/BlockMirrorItem.java`
- `thaumcraft/common/entities/monster/EntityEldritchCrab.java`
- `thaumcraft/common/entities/monster/EntityEldritchGuardian.java`
- `thaumcraft/common/entities/monster/boss/EntityEldritchGolem.java`
- `thaumcraft/common/entities/monster/boss/EntityEldritchWarden.java`
- `thaumcraft/common/entities/projectile/EntityEldritchOrb.java`
- `thaumcraft/common/items/ItemEldritchObject.java`
- `thaumcraft/common/items/ItemKey.java`
- `thaumcraft/common/items/ItemLootBag.java`
- `thaumcraft/common/tiles/TileBanner.java`
- `thaumcraft/common/tiles/TileEldritchAltar.java`
- `thaumcraft/common/tiles/TileEldritchCap.java`
- `thaumcraft/common/tiles/TileEldritchCrabSpawner.java`
- `thaumcraft/common/tiles/TileEldritchCrystal.java`
- `thaumcraft/common/tiles/TileEldritchLock.java`
- `thaumcraft/common/tiles/TileEldritchNothing.java`
- `thaumcraft/common/tiles/TileEldritchObelisk.java`
- `thaumcraft/common/tiles/TileEldritchPortal.java`
- `thaumcraft/common/tiles/TileEldritchTrap.java`
- `thaumcraft/common/tiles/TileMirror.java`
- `thaumcraft/common/tiles/TileMirrorEssentia.java`

## Mobs & monsters (31)
Hostile/neutral mob entities (wisp, pech, firebat, cultists, watchers, faces...).
- `thaumcraft/common/entities/monster/EntityBrainyZombie.java`
- `thaumcraft/common/entities/monster/EntityCultist.java`
- `thaumcraft/common/entities/monster/EntityCultistCleric.java`
- `thaumcraft/common/entities/monster/EntityCultistKnight.java`
- `thaumcraft/common/entities/monster/EntityFireBat.java`
- `thaumcraft/common/entities/monster/EntityGiantBrainyZombie.java`
- `thaumcraft/common/entities/monster/EntityInhabitedZombie.java`
- `thaumcraft/common/entities/monster/EntityMindSpider.java`
- `thaumcraft/common/entities/monster/EntityPech.java`
- `thaumcraft/common/entities/monster/EntityThaumicSlime.java`
- `thaumcraft/common/entities/monster/EntityWatcher.java`
- `thaumcraft/common/entities/monster/EntityWisp.java`
- `thaumcraft/common/entities/monster/boss/EntityCultistLeader.java`
- `thaumcraft/common/entities/monster/boss/EntityCultistPortal.java`
- `thaumcraft/common/entities/monster/boss/EntityThaumcraftBoss.java`
- `thaumcraft/common/entities/monster/mods/ChampionModArmored.java`
- `thaumcraft/common/entities/monster/mods/ChampionModBold.java`
- `thaumcraft/common/entities/monster/mods/ChampionModDummy.java`
- `thaumcraft/common/entities/monster/mods/ChampionModFire.java`
- `thaumcraft/common/entities/monster/mods/ChampionModGrim.java`
- `thaumcraft/common/entities/monster/mods/ChampionModInfested.java`
- `thaumcraft/common/entities/monster/mods/ChampionModMighty.java`
- `thaumcraft/common/entities/monster/mods/ChampionModPoison.java`
- `thaumcraft/common/entities/monster/mods/ChampionModSickly.java`
- `thaumcraft/common/entities/monster/mods/ChampionModSpined.java`
- `thaumcraft/common/entities/monster/mods/ChampionModUndying.java`
- `thaumcraft/common/entities/monster/mods/ChampionModVampire.java`
- `thaumcraft/common/entities/monster/mods/ChampionModWarded.java`
- `thaumcraft/common/entities/monster/mods/ChampionModWarp.java`
- `thaumcraft/common/entities/monster/mods/ChampionModifier.java`
- `thaumcraft/common/entities/monster/mods/IChampionModifierEffect.java`

## World generation (biomes, nodes, structures, villages) (28)
World generator, biomes, ore/tree/flower gen, village houses, structures.
- `thaumcraft/common/blocks/BlockCustomOre.java`
- `thaumcraft/common/blocks/BlockCustomOreItem.java`
- `thaumcraft/common/blocks/BlockCustomPlant.java`
- `thaumcraft/common/blocks/BlockCustomPlantItem.java`
- `thaumcraft/common/blocks/BlockMagicalLeaves.java`
- `thaumcraft/common/blocks/BlockMagicalLeavesItem.java`
- `thaumcraft/common/blocks/BlockMagicalLog.java`
- `thaumcraft/common/blocks/BlockMagicalLogItem.java`
- `thaumcraft/common/lib/world/ChunkLoc.java`
- `thaumcraft/common/lib/world/ComponentBankerHome.java`
- `thaumcraft/common/lib/world/ComponentWizardTower.java`
- `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java`
- `thaumcraft/common/lib/world/VillageBankerManager.java`
- `thaumcraft/common/lib/world/VillageWizardManager.java`
- `thaumcraft/common/lib/world/WorldGenBigMagicTree.java`
- `thaumcraft/common/lib/world/WorldGenCustomFlowers.java`
- `thaumcraft/common/lib/world/WorldGenEldritchRing.java`
- `thaumcraft/common/lib/world/WorldGenGreatwoodTrees.java`
- `thaumcraft/common/lib/world/WorldGenHilltopStones.java`
- `thaumcraft/common/lib/world/WorldGenManaPods.java`
- `thaumcraft/common/lib/world/WorldGenMound.java`
- `thaumcraft/common/lib/world/WorldGenSilverwoodTrees.java`
- `thaumcraft/common/lib/world/WorldGenSilverwoodTreesOld.java`
- `thaumcraft/common/lib/world/biomes/BiomeGenEerie.java`
- `thaumcraft/common/lib/world/biomes/BiomeGenEldritch.java`
- `thaumcraft/common/lib/world/biomes/BiomeGenMagicalForest.java`
- `thaumcraft/common/lib/world/biomes/BiomeGenTaint.java`
- `thaumcraft/common/lib/world/biomes/BiomeHandler.java`

## Taint system (20)
Taint blocks, taint mobs, taint spread manager, taint items.
- `thaumcraft/common/blocks/BlockTaint.java`
- `thaumcraft/common/blocks/BlockTaintFibres.java`
- `thaumcraft/common/blocks/BlockTaintFibresItem.java`
- `thaumcraft/common/blocks/BlockTaintItem.java`
- `thaumcraft/common/entities/EntityFallingTaint.java`
- `thaumcraft/common/entities/monster/EntityTaintChicken.java`
- `thaumcraft/common/entities/monster/EntityTaintCow.java`
- `thaumcraft/common/entities/monster/EntityTaintCreeper.java`
- `thaumcraft/common/entities/monster/EntityTaintPig.java`
- `thaumcraft/common/entities/monster/EntityTaintSheep.java`
- `thaumcraft/common/entities/monster/EntityTaintSpider.java`
- `thaumcraft/common/entities/monster/EntityTaintSpore.java`
- `thaumcraft/common/entities/monster/EntityTaintSporeSwarmer.java`
- `thaumcraft/common/entities/monster/EntityTaintSwarm.java`
- `thaumcraft/common/entities/monster/EntityTaintVillager.java`
- `thaumcraft/common/entities/monster/EntityTaintacle.java`
- `thaumcraft/common/entities/monster/EntityTaintacleSmall.java`
- `thaumcraft/common/entities/monster/boss/EntityTaintacleGiant.java`
- `thaumcraft/common/entities/projectile/EntityBottleTaint.java`
- `thaumcraft/common/items/ItemBottleTaint.java`

## Golems & traveling trunk (19)
Golem entities, golem devices/plates, traveling trunk.
- `thaumcraft/common/entities/ai/combat/AIGolemAttackOnCollide.java`
- `thaumcraft/common/entities/golems/ContainerGolem.java`
- `thaumcraft/common/entities/golems/ContainerTravelingTrunk.java`
- `thaumcraft/common/entities/golems/EntityGolemBase.java`
- `thaumcraft/common/entities/golems/EntityGolemBobber.java`
- `thaumcraft/common/entities/golems/EntityTravelingTrunk.java`
- `thaumcraft/common/entities/golems/EnumGolemType.java`
- `thaumcraft/common/entities/golems/GolemHelper.java`
- `thaumcraft/common/entities/golems/InventoryTrunk.java`
- `thaumcraft/common/entities/golems/ItemGolemBell.java`
- `thaumcraft/common/entities/golems/ItemGolemCore.java`
- `thaumcraft/common/entities/golems/ItemGolemDecoration.java`
- `thaumcraft/common/entities/golems/ItemGolemPlacer.java`
- `thaumcraft/common/entities/golems/ItemGolemUpgrade.java`
- `thaumcraft/common/entities/golems/ItemTrunkSpawner.java`
- `thaumcraft/common/entities/golems/Marker.java`
- `thaumcraft/common/entities/projectile/EntityGolemOrb.java`
- `thaumcraft/common/tiles/TileBrainbox.java`
- `thaumcraft/common/tiles/TileJarBrain.java`

## Equipment: tools & weapons (19)
Elemental/thaumium/void tools, bone bow, crimson sword, primal crusher.
- `thaumcraft/common/items/equipment/ItemBowBone.java`
- `thaumcraft/common/items/equipment/ItemCrimsonSword.java`
- `thaumcraft/common/items/equipment/ItemElementalAxe.java`
- `thaumcraft/common/items/equipment/ItemElementalHoe.java`
- `thaumcraft/common/items/equipment/ItemElementalPickaxe.java`
- `thaumcraft/common/items/equipment/ItemElementalShovel.java`
- `thaumcraft/common/items/equipment/ItemElementalSword.java`
- `thaumcraft/common/items/equipment/ItemPrimalArrow.java`
- `thaumcraft/common/items/equipment/ItemPrimalCrusher.java`
- `thaumcraft/common/items/equipment/ItemThaumiumAxe.java`
- `thaumcraft/common/items/equipment/ItemThaumiumHoe.java`
- `thaumcraft/common/items/equipment/ItemThaumiumPickaxe.java`
- `thaumcraft/common/items/equipment/ItemThaumiumShovel.java`
- `thaumcraft/common/items/equipment/ItemThaumiumSword.java`
- `thaumcraft/common/items/equipment/ItemVoidAxe.java`
- `thaumcraft/common/items/equipment/ItemVoidHoe.java`
- `thaumcraft/common/items/equipment/ItemVoidPickaxe.java`
- `thaumcraft/common/items/equipment/ItemVoidShovel.java`
- `thaumcraft/common/items/equipment/ItemVoidSword.java`

## Portals, mirrors & eldritch (Outer Lands) (17)
Outer Lands dimension provider, procedural maze generation.
- `thaumcraft/common/lib/world/dim/Cell.java`
- `thaumcraft/common/lib/world/dim/CellLoc.java`
- `thaumcraft/common/lib/world/dim/ChunkProviderOuter.java`
- `thaumcraft/common/lib/world/dim/Gen2x2.java`
- `thaumcraft/common/lib/world/dim/GenBossRoom.java`
- `thaumcraft/common/lib/world/dim/GenCommon.java`
- `thaumcraft/common/lib/world/dim/GenKeyRoom.java`
- `thaumcraft/common/lib/world/dim/GenLibraryRoom.java`
- `thaumcraft/common/lib/world/dim/GenNestRoom.java`
- `thaumcraft/common/lib/world/dim/GenPassage.java`
- `thaumcraft/common/lib/world/dim/GenPortal.java`
- `thaumcraft/common/lib/world/dim/MapBossData.java`
- `thaumcraft/common/lib/world/dim/MazeGenerator.java`
- `thaumcraft/common/lib/world/dim/MazeHandler.java`
- `thaumcraft/common/lib/world/dim/MazeThread.java`
- `thaumcraft/common/lib/world/dim/TeleporterThaumcraft.java`
- `thaumcraft/common/lib/world/dim/WorldProviderOuter.java`

## API: core (ThaumcraftApi, helpers) (16)
Public API surface: ThaumcraftApi, ItemApi, interfaces, annotations.
- `thaumcraft/api/BlockCoordinates.java`
- `thaumcraft/api/IArchitect.java`
- `thaumcraft/api/IGoggles.java`
- `thaumcraft/api/IRepairable.java`
- `thaumcraft/api/IRepairableExtended.java`
- `thaumcraft/api/IRunicArmor.java`
- `thaumcraft/api/IScribeTools.java`
- `thaumcraft/api/IVisDiscountGear.java`
- `thaumcraft/api/IWarpingGear.java`
- `thaumcraft/api/ItemApi.java`
- `thaumcraft/api/ItemRunic.java`
- `thaumcraft/api/ThaumcraftApi.java`
- `thaumcraft/api/ThaumcraftApiHelper.java`
- `thaumcraft/api/TileThaumcraft.java`
- `thaumcraft/api/WorldCoordinates.java`
- `thaumcraft/api/package-info.java`

## Arcane crafting & devices (13)
Arcane workbench, deconstruction table, bore, magic box, hungry chest, focal manipulator.
- `thaumcraft/common/blocks/BlockChestHungry.java`
- `thaumcraft/common/blocks/BlockMagicBox.java`
- `thaumcraft/common/items/armor/RecipesRobeArmorDyes.java`
- `thaumcraft/common/items/armor/RecipesVoidRobeArmorDyes.java`
- `thaumcraft/common/tiles/TileArcaneBore.java`
- `thaumcraft/common/tiles/TileArcaneBoreBase.java`
- `thaumcraft/common/tiles/TileArcaneWorkbench.java`
- `thaumcraft/common/tiles/TileChestHungry.java`
- `thaumcraft/common/tiles/TileDeconstructionTable.java`
- `thaumcraft/common/tiles/TileFocalManipulator.java`
- `thaumcraft/common/tiles/TileMagicBox.java`
- `thaumcraft/common/tiles/TileMagicWorkbench.java`
- `thaumcraft/common/tiles/TileMagicWorkbenchCharger.java`

## Equipment: armor (13)
Robes, fortress/cultist/traveller armor, goggles, hover logic.
- `thaumcraft/common/items/armor/Hover.java`
- `thaumcraft/common/items/armor/ItemBootsTraveller.java`
- `thaumcraft/common/items/armor/ItemCultistBoots.java`
- `thaumcraft/common/items/armor/ItemCultistLeaderArmor.java`
- `thaumcraft/common/items/armor/ItemCultistPlateArmor.java`
- `thaumcraft/common/items/armor/ItemCultistRobeArmor.java`
- `thaumcraft/common/items/armor/ItemFortressArmor.java`
- `thaumcraft/common/items/armor/ItemGoggles.java`
- `thaumcraft/common/items/armor/ItemHoverHarness.java`
- `thaumcraft/common/items/armor/ItemRobeArmor.java`
- `thaumcraft/common/items/armor/ItemThaumiumArmor.java`
- `thaumcraft/common/items/armor/ItemVoidArmor.java`
- `thaumcraft/common/items/armor/ItemVoidRobeArmor.java`

## Vis nodes & aura (12)
Node tiles, node jars, crystal blocks, compass stone, node devices.
- `thaumcraft/common/blocks/BlockAiry.java`
- `thaumcraft/common/blocks/BlockAiryItem.java`
- `thaumcraft/common/blocks/BlockCrystal.java`
- `thaumcraft/common/blocks/BlockCrystalItem.java`
- `thaumcraft/common/blocks/ItemJarNode.java`
- `thaumcraft/common/items/ItemCompassStone.java`
- `thaumcraft/common/tiles/TileCrystal.java`
- `thaumcraft/common/tiles/TileJarNode.java`
- `thaumcraft/common/tiles/TileNode.java`
- `thaumcraft/common/tiles/TileNodeConverter.java`
- `thaumcraft/common/tiles/TileNodeEnergized.java`
- `thaumcraft/common/tiles/TileNodeStabilizer.java`

## Equipment: wand foci (12)
Focus items (excavation, fire, frost, hellbat, pech, portable hole, primal, shock, trade, warding, pouch).
- `thaumcraft/common/items/wands/ItemFocusPouch.java`
- `thaumcraft/common/items/wands/ItemFocusPouchBauble.java`
- `thaumcraft/common/items/wands/foci/ItemFocusExcavation.java`
- `thaumcraft/common/items/wands/foci/ItemFocusFire.java`
- `thaumcraft/common/items/wands/foci/ItemFocusFrost.java`
- `thaumcraft/common/items/wands/foci/ItemFocusHellbat.java`
- `thaumcraft/common/items/wands/foci/ItemFocusPech.java`
- `thaumcraft/common/items/wands/foci/ItemFocusPortableHole.java`
- `thaumcraft/common/items/wands/foci/ItemFocusPrimal.java`
- `thaumcraft/common/items/wands/foci/ItemFocusShock.java`
- `thaumcraft/common/items/wands/foci/ItemFocusTrade.java`
- `thaumcraft/common/items/wands/foci/ItemFocusWarding.java`

## Research & scanning (research table & GUI) (10)
Research table block/tile/GUI, inkwell.
- `thaumcraft/client/gui/GuiResearchBrowser.java`
- `thaumcraft/client/gui/GuiResearchPopup.java`
- `thaumcraft/client/gui/GuiResearchRecipe.java`
- `thaumcraft/client/gui/GuiResearchTable.java`
- `thaumcraft/common/blocks/BlockTable.java`
- `thaumcraft/common/blocks/BlockTableItem.java`
- `thaumcraft/common/container/ContainerResearchTable.java`
- `thaumcraft/common/items/ItemInkwell.java`
- `thaumcraft/common/tiles/TileResearchTable.java`
- `thaumcraft/common/tiles/TileTable.java`

## API: wands (10)
WandCap/WandRod/focus interfaces, WandTriggerRegistry.
- `thaumcraft/api/wands/FocusUpgradeType.java`
- `thaumcraft/api/wands/IWandFocus.java`
- `thaumcraft/api/wands/IWandRodOnUpdate.java`
- `thaumcraft/api/wands/IWandTriggerManager.java`
- `thaumcraft/api/wands/IWandable.java`
- `thaumcraft/api/wands/ItemFocusBasic.java`
- `thaumcraft/api/wands/StaffRod.java`
- `thaumcraft/api/wands/WandCap.java`
- `thaumcraft/api/wands/WandRod.java`
- `thaumcraft/api/wands/WandTriggerRegistry.java`

## Enchantments & runic shielding (9)
Enchantment classes, runic armor handling, warded blocks.
- `thaumcraft/common/blocks/BlockWarded.java`
- `thaumcraft/common/lib/enchantment/EnchantmentFrugal.java`
- `thaumcraft/common/lib/enchantment/EnchantmentHaste.java`
- `thaumcraft/common/lib/enchantment/EnchantmentPotency.java`
- `thaumcraft/common/lib/enchantment/EnchantmentRepair.java`
- `thaumcraft/common/lib/enchantment/EnchantmentWandFortune.java`
- `thaumcraft/common/tiles/TileWarded.java`
- `thaumcraft/common/tiles/TileWardingStone.java`
- `thaumcraft/common/tiles/TileWardingStoneFence.java`

## Core support (tabs, refs, misc) (9)
Creative tab, GUI id refs, sound types, misc helpers.
- `thaumcraft/common/lib/CreativeTabThaumcraft.java`
- `thaumcraft/common/lib/CustomSoundType.java`
- `thaumcraft/common/lib/FakeThaumcraftPlayer.java`
- `thaumcraft/common/lib/HexUtils.java`
- `thaumcraft/common/lib/InternalMethodHandler.java`
- `thaumcraft/common/lib/RefGui.java`
- `thaumcraft/common/lib/TCVec3.java`
- `thaumcraft/common/lib/TCVec3Pool.java`
- `thaumcraft/common/lib/UUIDHelper.java`

## Potions, warp & effects (9)
Potion effect classes and warp event handling.
- `thaumcraft/common/items/ItemSanitySoap.java`
- `thaumcraft/common/lib/WarpEvents.java`
- `thaumcraft/common/lib/potions/PotionBlurredVision.java`
- `thaumcraft/common/lib/potions/PotionDeathGaze.java`
- `thaumcraft/common/lib/potions/PotionInfectiousVisExhaust.java`
- `thaumcraft/common/lib/potions/PotionSunScorned.java`
- `thaumcraft/common/lib/potions/PotionThaumarhia.java`
- `thaumcraft/common/lib/potions/PotionUnnaturalHunger.java`
- `thaumcraft/common/lib/potions/PotionWarpWard.java`

## Utilities & helpers (9)
Block/inventory/entity/crop utils, vector helpers.
- `thaumcraft/common/lib/utils/BlockUtils.java`
- `thaumcraft/common/lib/utils/CropUtils.java`
- `thaumcraft/common/lib/utils/EntityUtils.java`
- `thaumcraft/common/lib/utils/HexUtils.java`
- `thaumcraft/common/lib/utils/InventoryUtils.java`
- `thaumcraft/common/lib/utils/TCVec3.java`
- `thaumcraft/common/lib/utils/TCVec3Pool.java`
- `thaumcraft/common/lib/utils/UUIDHelper.java`
- `thaumcraft/common/lib/utils/Utils.java`

## Bootstrap: registration & config (9)
Config file handling and registration of every block, item, entity, research entry and recipe.
- `thaumcraft/common/config/Config.java`
- `thaumcraft/common/config/ConfigAspects.java`
- `thaumcraft/common/config/ConfigBlocks.java`
- `thaumcraft/common/config/ConfigEntities.java`
- `thaumcraft/common/config/ConfigItems.java`
- `thaumcraft/common/config/ConfigRecipes.java`
- `thaumcraft/common/config/ConfigResearch.java`
- `thaumcraft/common/config/MaterialAiry.java`
- `thaumcraft/common/config/MaterialTaint.java`

## Entity support (special items, spawner eggs) (9)
Special item entities, following items, mob inventory, spawner egg item.
- `thaumcraft/common/entities/ContainerPech.java`
- `thaumcraft/common/entities/EntityAspectOrb.java`
- `thaumcraft/common/entities/EntityFollowingItem.java`
- `thaumcraft/common/entities/EntityItemGrate.java`
- `thaumcraft/common/entities/EntityPermanentItem.java`
- `thaumcraft/common/entities/EntitySpecialItem.java`
- `thaumcraft/common/entities/InventoryMob.java`
- `thaumcraft/common/entities/InventoryPech.java`
- `thaumcraft/common/entities/ItemSpawnerEgg.java`

## Projectiles (9)
Alumentum, primal arrow/orb, ember, dart entities and dispenser behavior.
- `thaumcraft/common/entities/projectile/EntityAlumentum.java`
- `thaumcraft/common/entities/projectile/EntityDart.java`
- `thaumcraft/common/entities/projectile/EntityEmber.java`
- `thaumcraft/common/entities/projectile/EntityExplosiveOrb.java`
- `thaumcraft/common/entities/projectile/EntityFrostShard.java`
- `thaumcraft/common/entities/projectile/EntityPechBlast.java`
- `thaumcraft/common/entities/projectile/EntityPrimalArrow.java`
- `thaumcraft/common/entities/projectile/EntityPrimalOrb.java`
- `thaumcraft/common/entities/projectile/EntityShockOrb.java`

## Wands & vis (8)
Wand items, WandManager vis tasks, resonator, vis relay, pedestal.
- `thaumcraft/common/items/relics/ItemResonator.java`
- `thaumcraft/common/items/wands/ItemWandCap.java`
- `thaumcraft/common/items/wands/ItemWandCasting.java`
- `thaumcraft/common/items/wands/ItemWandRod.java`
- `thaumcraft/common/items/wands/WandManager.java`
- `thaumcraft/common/items/wands/WandRodPrimalOnUpdate.java`
- `thaumcraft/common/tiles/TileVisRelay.java`
- `thaumcraft/common/tiles/TileWandPedestal.java`

## Materials & food items (8)
Resource/nugget/shard/essence items, food items.
- `thaumcraft/common/items/BehaviorDispenseAlumetum.java`
- `thaumcraft/common/items/ItemNugget.java`
- `thaumcraft/common/items/ItemNuggetEdible.java`
- `thaumcraft/common/items/ItemResource.java`
- `thaumcraft/common/items/ItemShard.java`
- `thaumcraft/common/items/ItemTripleMeatTreat.java`
- `thaumcraft/common/items/ItemWispEssence.java`
- `thaumcraft/common/items/ItemZombieBrain.java`

## Events, commands & tick handlers (7)
Forge event handlers, chat commands, tick loops (aura, warp, taint).
- `thaumcraft/common/lib/events/CommandThaumcraft.java`
- `thaumcraft/common/lib/events/EssentiaHandler.java`
- `thaumcraft/common/lib/events/EventHandlerEntity.java`
- `thaumcraft/common/lib/events/EventHandlerRunic.java`
- `thaumcraft/common/lib/events/EventHandlerWorld.java`
- `thaumcraft/common/lib/events/KeyHandler.java`
- `thaumcraft/common/lib/events/ServerTickEventsFML.java`

## API: aspects (7)
Aspect model, AspectList, essentia container interfaces.
- `thaumcraft/api/aspects/Aspect.java`
- `thaumcraft/api/aspects/AspectList.java`
- `thaumcraft/api/aspects/AspectSourceHelper.java`
- `thaumcraft/api/aspects/IAspectContainer.java`
- `thaumcraft/api/aspects/IAspectSource.java`
- `thaumcraft/api/aspects/IEssentiaContainerItem.java`
- `thaumcraft/api/aspects/IEssentiaTransport.java`

## API: crafting (7)
Arcane/crucible/infusion recipe types and interfaces.
- `thaumcraft/api/crafting/CrucibleRecipe.java`
- `thaumcraft/api/crafting/IArcaneRecipe.java`
- `thaumcraft/api/crafting/IInfusionStabiliser.java`
- `thaumcraft/api/crafting/InfusionEnchantmentRecipe.java`
- `thaumcraft/api/crafting/InfusionRecipe.java`
- `thaumcraft/api/crafting/ShapedArcaneRecipe.java`
- `thaumcraft/api/crafting/ShapelessArcaneRecipe.java`

## Equipment: baubles (6)
Runic amulet/ring/girdle, vis amulet, hover girdle, blanks.
- `thaumcraft/common/items/baubles/ItemAmuletRunic.java`
- `thaumcraft/common/items/baubles/ItemAmuletVis.java`
- `thaumcraft/common/items/baubles/ItemBaubleBlanks.java`
- `thaumcraft/common/items/baubles/ItemGirdleHover.java`
- `thaumcraft/common/items/baubles/ItemGirdleRunic.java`
- `thaumcraft/common/items/baubles/ItemRingRunic.java`

## API: research (6)
ResearchItem/ResearchPage/ResearchCategories model.
- `thaumcraft/api/research/IScanEventHandler.java`
- `thaumcraft/api/research/ResearchCategories.java`
- `thaumcraft/api/research/ResearchCategoryList.java`
- `thaumcraft/api/research/ResearchItem.java`
- `thaumcraft/api/research/ResearchPage.java`
- `thaumcraft/api/research/ScanResult.java`

## Crafting engine & recipe types (5)
Recipe managers (arcane workbench, crucible, furnace) and special recipes.
- `thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java`
- `thaumcraft/common/lib/crafting/ArcaneWandRecipe.java`
- `thaumcraft/common/lib/crafting/InfusionRunicAugmentRecipe.java`
- `thaumcraft/common/lib/crafting/ShapelessNBTOreRecipe.java`
- `thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java`

## Research & scanning (4)
ResearchManager, PlayerKnowledge, scan handling.
- `thaumcraft/common/lib/research/PlayerKnowledge.java`
- `thaumcraft/common/lib/research/ResearchManager.java`
- `thaumcraft/common/lib/research/ResearchNoteData.java`
- `thaumcraft/common/lib/research/ScanManager.java`

## API: vis nodes (4)

- `thaumcraft/api/nodes/INode.java`
- `thaumcraft/api/nodes/IRevealer.java`
- `thaumcraft/api/nodes/NodeModifier.java`
- `thaumcraft/api/nodes/NodeType.java`

## Bundled: TrueType font renderer (4)
Bundled truetyper font rendering library.
- `truetyper/FontHelper.java`
- `truetyper/FontLoader.java`
- `truetyper/Formatter.java`
- `truetyper/TrueTypeFont.java`

## Mod core & lifecycle (3)
@Mod entry point, proxies, pre/init/post lifecycle.
- `thaumcraft/client/ClientProxy.java`
- `thaumcraft/common/CommonProxy.java`
- `thaumcraft/common/Thaumcraft.java`

## Research & scanning (items) (3)
Thaumonomicon, research notes, thaumometer.
- `thaumcraft/common/items/ItemResearchNotes.java`
- `thaumcraft/common/items/relics/ItemThaumometer.java`
- `thaumcraft/common/items/relics/ItemThaumonomicon.java`

## API: internal bridge (3)
Internal method bridge used by the API to call implementation code.
- `thaumcraft/api/internal/DummyInternalMethodHandler.java`
- `thaumcraft/api/internal/IInternalMethodHandler.java`
- `thaumcraft/api/internal/WeightedRandomLoot.java`

## Infusion crafting (2)
Infusion matrix/pillar tiles.
- `thaumcraft/common/tiles/TileInfusionMatrix.java`
- `thaumcraft/common/tiles/TileInfusionPillar.java`

## Equipment: relics & curios (2)
Relic items (hand mirror, sanity checker).
- `thaumcraft/common/items/relics/ItemHandMirror.java`
- `thaumcraft/common/items/relics/ItemSanityChecker.java`

## API: damage sources (2)
Thaumcraft damage source classes.
- `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java`
- `thaumcraft/api/damagesource/DamageSourceThaumcraft.java`

## API: potions (2)
Potion base classes (PotionFluxTaint, PotionVisExhaust).
- `thaumcraft/api/potions/PotionFluxTaint.java`
- `thaumcraft/api/potions/PotionVisExhaust.java`

## API: vis network (2)
VisNetHandler, TileVisNode network model.
- `thaumcraft/api/visnet/TileVisNode.java`
- `thaumcraft/api/visnet/VisNetHandler.java`

## API: entities (2)
Marker interfaces for eldritch and tainted mobs (IEldritchMob, ITaintedMob).
- `thaumcraft/api/entities/IEldritchMob.java`
- `thaumcraft/api/entities/ITaintedMob.java`
