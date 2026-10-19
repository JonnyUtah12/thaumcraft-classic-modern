# Sound events — original trigger sites and 1.21.1 wiring (TC-M7-05)

Authoritative per-event map for the sound milestone: every sound event in
the original Thaumcraft 4.2.3.5 `assets/thaumcraft/sounds.json` (66 events
over 111 ogg files, docs/original-asset-inventory.md), where the original
code played it, and where the port plays it. Generated from the decompiled
tree (string-literal + entity sound-getter scan; icon-registration hits
excluded) — `../reference/decompiled` is evidence only, never committed.

## The contract

| half | lives in | guarded by |
| --- | --- | --- |
| event-id registry (66 ids) | `sound/ModSounds.java` (committed code) | `SoundRegistryGameTest` (runtime registry), `SoundRegistryParityTest` (id set) |
| event definitions (sounds.json, 1.21.1 format) | build output from `../reference/assets` via `transformOriginalSoundsJson` (ARR, not committed — D-004) | `SoundRegistryParityTest` + `OriginalSoundOverlayTest` |
| ogg files | same overlay (`overlayOriginalAssets`), 2 files renamed (below) | `SoundRegistryParityTest` (no orphan/missing files) |
| per-asset rows | `docs/asset-mapping.csv` (`used_by` = verified play sites) | `SoundRegistryParityTest` |

Conversion rules applied by the overlay transform:

- Event keys stay the bare original ids: `SoundManager` namespaces keys by
  the sounds.json file's namespace (`fromNamespaceAndPath(ns, key)`), and
  `:` is not a valid path char — prefixed or uppercase keys reject the
  whole file. Sound-file references inside entries must be explicitly
  `thaumcraft:`-qualified (1.21.1 defaults bare names to `minecraft`).
- Two ids contain uppercase letters, which 1.21.1 ids reject
  (`[a-z0-9/._-]` only): `runicShieldCharge -> runic_shield_charge`,
  `runicShieldEffect -> runic_shield_effect`; their oggs are renamed by
  the overlay and the CSV records both sides.
- The 1.7.10 per-event `category` field is dropped (1.21.1 has no
  per-event category). The original registered every event under
  `master`, so faithful play sites pass `SoundSource.MASTER`.
- Ranges: 1.7.10's fixed 16-block default == `createVariableRangeEvent`.
- `urnbreak` is registered for parity but has no trigger site in the
  original either (its step-sound name never resolves into the
  `thaumcraft` namespace — see Step sounds below).

## Trigger sites wired now (TC-M7-05)

The networking system exists in the port, so the three sounds the original
played inside packet handlers are live (volume/pitch copied exactly):

| event | original site | port site |
| --- | --- | --- |
| whispers | `PacketWarpMessage.onMessage` (client): player sound 0.5/1.0 when warp increases (sticky or temporary) | `ClientEffectHooks.warpNotification` when `change > 0` |
| heartbeat | `PacketMiscEvent.onMessage` (client) type 0: world sound at player 1.0/1.0 | `ClientEffectHooks.miscEvent` when `eventKind == 0` |
| zap | `PacketFXBlockZap.onMessage` (client): world sound at pos 0.1/(1.0+rand*0.2) | `ClientEffectHooks.fxBlockZap` |
| learn | `PacketResearchComplete.onMessage` (client): player sound 0.2/(1.0+rand*0.1) only for clue-type research; `PacketPlayerCompleteToServer` (server): world sound at player 0.75/1.0 on successful completion | deferred to TC-M3-02 — the clue condition needs the research graph; contract recorded in `ClientEffectHooks.researchCompleteNotification` |

All other events belong to systems that do not exist in the port yet
(machines, entities, wands, GUIs, taint). Their rows below name the
owning milestone; when that task lands, it plays the event at the listed
site with the original volume/pitch/category. `docs/asset-mapping.csv`
sound rows carry the same event ids; `ModSounds` constants are the only
legal way to reference them.

## Per-event map

Sites are `class.method` in the original decompiled tree (kind: play =
direct playSound call, getter = entity living/death/hurt/jump sound
override — 1.21.1 equivalent is the entity's sound-type/`SoundEvent`
wiring). `func_*` names are 1.7.10 SRG identifiers; the decompiled tree
under `../reference/decompiled` is the evidence (never committed). Common
entity getters: `func_70639_aQ`=getLivingSound, `func_70621_aR`=getHurtSound,
`func_70673_aS`=getDeathSound, `func_85030_a`=Entity.playSound,
`func_72908_a`=World.playSound, `func_72905_a`=World.playSoundAtEntity.
Owners are the milestone that ports that class
(docs/feature-tracking.md; exact content-id join: docs/content-mapping.csv).

| event (thaumcraft:) | ModSounds | oggs | original trigger sites | port owner |
| --- | --- | --- | --- | --- |
| alembicknock | `ALEMBICKNOCK` | alembicknock.ogg | `common.blocks.BlockMetalDevice.func_149727_a`; `common.items.relics.ItemResonator.onItemUseFirst` | TC-M6-04 (block catalog); TC-M6-04 (item catalog) |
| brain | `BRAIN` | brain1.ogg, brain2.ogg, brain3.ogg, brain4.ogg | `common.tiles.TileJarBrain.func_145845_h` | TC-M4-06/07 (essentia storage/transport) |
| bubble | `BUBBLE` | bubble1.ogg, bubble2.ogg, bubble3.ogg, bubble4.ogg | `common.tiles.TileCrucible.attemptSmelt`; `common.blocks.BlockAlchemyFurnace.func_149670_a`; `common.items.equipment.ItemElementalAxe.onBlockStartBreak` | TC-M4-05 (alchemy/crucible); TC-M6-07 (equipment) |
| cameraclack | `CAMERACLACK` | cameraclack1.ogg, cameraclack2.ogg, cameraclack3.ogg | `client.gui.GuiThaumatorium.playButtonClick`; `client.gui.GuiResearchBrowser.playButtonClick`; `client.gui.GuiResearchTable.playButtonClick`; `client.gui.GuiSpa.playButtonClick`; `client.gui.GuiFocalManipulator.playButtonClick`; `common.entities.golems.EntityGolemBase.addDecoration`; `common.entities.golems.EntityGolemBase.func_70639_aQ`; `common.entities.golems.EntityGolemBase.func_70621_aR`; `common.entities.golems.EntityGolemBase.func_70673_aS` | TC-M3-03 (Thaumonomicon GUI); TC-M6-06 (entities/AI); TC-M7-04 (GUI parity) |
| cameraticks | `CAMERATICKS` | cameraticks.ogg | `common.items.relics.ItemThaumometer.onUsingTick`; `common.items.wands.WandManager.changeFocus`; `common.entities.golems.EntityGolemBase.func_70636_d` | TC-M6-01 (wands); TC-M6-04 (item catalog); TC-M6-06 (entities/AI) |
| chant | `CHANT` | chant1.ogg, chant2.ogg, chant3.ogg | `common.entities.monster.EntityCultistCleric.func_70639_aQ` | TC-M6-06 (entities/AI) |
| coins | `COINS` | coins.ogg | `common.items.ItemLootBag.func_77659_a` | TC-M6-04 (item catalog) |
| crabclaw | `CRABCLAW` | crabclaw.ogg | `common.entities.monster.EntityEldritchCrab.func_70652_k` | TC-M6-06 (entities/AI) |
| crabdeath | `CRABDEATH` | crabdeath.ogg | `common.entities.monster.EntityEldritchCrab.func_70673_aS` | TC-M6-06 (entities/AI) |
| crabtalk | `CRABTALK` | crabtalk1.ogg, crabtalk2.ogg, crabtalk3.ogg | `common.entities.monster.EntityEldritchCrab.func_70639_aQ`; `common.entities.monster.EntityInhabitedZombie.func_70645_a` | TC-M6-06 (entities/AI) |
| craftfail | `CRAFTFAIL` | craftfail.ogg | `common.tiles.TileFocalManipulator.func_145845_h`; `common.tiles.TileNodeConverter.func_145842_c`; `common.tiles.TileInfusionMatrix.craftCycle`; `common.blocks.BlockCosmeticSolid.addDestroyEffects`; `common.blocks.BlockMagicalLog.addDestroyEffects`; `common.blocks.BlockAiry.addDestroyEffects`; `common.container.ContainerFocalManipulator.func_75140_a` | TC-M4-04 (infusion altar); TC-M5-02 (node behavior); TC-M6-01 (focal manipulator); TC-M6-04 (block catalog); TC-M6-04 (container) |
| craftstart | `CRAFTSTART` | craftstart.ogg | `common.tiles.TileFocalManipulator.startCraft`; `common.tiles.TileInfusionMatrix.craftingStart`; `common.items.ItemSanitySoap.func_77615_a` | TC-M4-04 (infusion altar); TC-M6-01 (focal manipulator); TC-M6-04 (item catalog) |
| creak | `CREAK` | creak1.ogg, creak2.ogg | `common.tiles.TileTube.func_145842_c`; `common.tiles.TileEssentiaReservoir.func_145845_h` | TC-M4-06/07 (essentia storage/transport) |
| crystal | `CRYSTAL` | crystal.ogg | `common.tiles.TileVisRelay.onWandRightClick`; `common.blocks.BlockMetalDevice.func_149727_a`; `common.blocks.BlockEldritch.func_149727_a` | TC-M6-04 (block catalog); TC-M6-04 (tile catalog) |
| doorfail | `DOORFAIL` | doorfail.ogg | `common.blocks.BlockArcaneDoor.func_149699_a` | TC-M6-04 (block catalog) |
| egattack | `EGATTACK` | egattack.ogg | `common.entities.monster.EntityCultistCleric.func_82196_d`; `common.entities.monster.EntityEldritchGuardian.func_82196_d`; `common.entities.monster.boss.EntityEldritchGolem.func_82196_d`; `common.entities.monster.boss.EntityEldritchWarden.func_82196_d`; `common.entities.monster.boss.EntityCultistLeader.func_82196_d` | TC-M6-06 (entities/AI) |
| egdeath | `EGDEATH` | egdeath.ogg | `common.entities.monster.EntityEldritchGuardian.func_70673_aS`; `common.entities.monster.boss.EntityEldritchWarden.func_70673_aS` | TC-M6-06 (entities/AI) |
| egidle | `EGIDLE` | egidle1.ogg, egidle2.ogg | `common.entities.monster.EntityEldritchGuardian.func_70639_aQ`; `common.entities.monster.boss.EntityEldritchWarden.func_70639_aQ` | TC-M6-06 (entities/AI) |
| egscreech | `EGSCREECH` | egscreech.ogg | `common.entities.monster.EntityEldritchGuardian.func_82196_d`; `common.entities.monster.boss.EntityEldritchWarden.func_82196_d` | TC-M6-06 (entities/AI) |
| erase | `ERASE` | erase.ogg | `client.gui.GuiResearchTable.playButtonErase`; `common.items.ItemResearchNotes.func_77659_a` | TC-M3-03 (Thaumonomicon GUI); TC-M6-04 (item catalog) |
| evilportal | `EVILPORTAL` | evilportal.ogg | `common.tiles.TileEldritchPortal.func_145845_h` | TC-M6-04 (tile catalog) |
| fireloop | `FIRELOOP` | fireloop.ogg | `common.items.wands.foci.ItemFocusFire.onUsingFocusTick` | TC-M6-02/03 (foci + casting) |
| fly | `FLY` | fly1.ogg, fly2.ogg | `client.fx.particles.FXSwarm.func_70071_h_` | TC-M7-02 (particles/beams) |
| golemironshoot | `GOLEMIRONSHOOT` | golemironshoot.ogg | `common.entities.golems.EntityGolemBase.attackEntityWithRangedAttack` | TC-M6-06 (entities/AI) |
| gore | `GORE` | gore1.ogg, gore2.ogg | `common.tiles.TileEldritchCrabSpawner.func_145845_h`; `common.blocks.BlockFluxGoo.func_149674_a`; `common.entities.EntityFallingTaint.func_70071_h_`; `common.entities.monster.EntityTaintSporeSwarmer.func_70100_b_`; `common.entities.monster.EntityThaumicSlime.func_70626_be`; `common.entities.monster.EntityTaintSpore.spiderBurst`; `common.entities.monster.EntityTaintSpore.func_70621_aR`; `common.entities.monster.EntityTaintSpore.func_70673_aS`; `common.entities.monster.mods.ChampionModInfested.performEffect` | TC-M5-04 (taint); TC-M6-04 (tile catalog); TC-M6-06 (entities/AI) |
| heartbeat | `HEARTBEAT` | heartbeat.ogg | `common.lib.network.misc.PacketMiscEvent.onMessage` | WIRED — ClientEffectHooks.miscEvent |
| hhoff | `HHOFF` | hhoff.ogg | `client.gui.GuiDeconstructionTable.playButtonAspect`; `client.gui.GuiResearchTable.playButtonAspect`; `common.items.armor.Hover.toggleHover` | TC-M3-03 (Thaumonomicon GUI); TC-M6-07 (armor) |
| hhon | `HHON` | hhon.ogg | `client.gui.GuiThaumatorium.playButtonSelect`; `client.gui.GuiResearchTable.playButtonCombine`; `common.items.armor.Hover.toggleHover` | TC-M3-03 (Thaumonomicon GUI); TC-M6-07 (armor); TC-M7-04 (GUI parity) |
| ice | `ICE` | ice1.ogg, ice2.ogg, ice3.ogg | `common.tiles.TileEldritchLock.doBossSpawn`; `common.items.wands.foci.ItemFocusHellbat.onFocusRightClick`; `common.items.wands.foci.ItemFocusFrost.onFocusRightClick`; `common.items.wands.foci.ItemFocusPrimal.onFocusRightClick`; `common.items.wands.foci.ItemFocusPech.onFocusRightClick`; `common.entities.monster.EntityPech.func_82196_d` | TC-M6-02/03 (foci + casting); TC-M6-04 (tile catalog); TC-M6-06 (entities/AI) |
| infuser | `INFUSER` | infuser.ogg | `common.tiles.TileInfusionMatrix.onUsingWandTick` | TC-M4-04 (infusion altar) |
| infuserstart | `INFUSERSTART` | infuserstart.ogg | `common.tiles.TileInfusionMatrix.onUsingWandTick` | TC-M4-04 (infusion altar) |
| jacobs | `JACOBS` | jacobs.ogg | `common.blocks.BlockAiry.func_149734_b`; `common.items.armor.Hover.handleHoverArmor`; `common.entities.monster.boss.EntityEldritchGolem.func_70103_a` | TC-M6-04 (block catalog); TC-M6-06 (entities/AI); TC-M6-07 (armor) |
| jar | `JAR` | jar1.ogg, jar2.ogg, jar3.ogg, jar4.ogg | `common.blocks.BlockMirrorItem.onItemUseFirst`; `common.blocks.BlockJar.func_149727_a`; `common.items.relics.ItemHandMirror.onItemUseFirst` | TC-M6-04 (block catalog); TC-M6-04 (item catalog) |
| key | `KEY` | key.ogg | `client.gui.GuiResearchTable.playButtonScroll`; `common.items.ItemKey.onItemUseFirst` | TC-M3-03 (Thaumonomicon GUI); TC-M6-04 (item catalog) |
| learn | `LEARN` | learn.ogg | `common.tiles.TileResearchTable.func_145842_c`; `common.lib.network.playerdata.PacketPlayerCompleteToServer.onMessage`; `common.lib.network.playerdata.PacketResearchComplete.onMessage`; `common.items.ItemResearchNotes.func_77659_a`; `common.items.ItemEldritchObject.func_77659_a` | TC-M3-02 (research progression); TC-M3-02 (research progression; contract in ClientEffectHooks); TC-M6-04 (item catalog) |
| monolith | `MONOLITH` | monolith.ogg | `common.entities.monster.boss.EntityCultistPortal.func_70639_aQ` | TC-M6-06 (entities/AI) |
| page | `PAGE` | page1.ogg, page2.ogg | `client.gui.GuiResearchRecipe.func_73864_a`; `common.blocks.BlockMetalDevice.func_149727_a`; `common.blocks.BlockTube.func_149727_a`; `common.blocks.BlockJar.func_149727_a`; `common.items.relics.ItemThaumonomicon.func_77659_a` | TC-M3-03 (Thaumonomicon GUI); TC-M6-04 (block catalog); TC-M6-04 (item catalog) |
| pech_charge | `PECH_CHARGE` | pech_charge1.ogg, pech_charge2.ogg | `common.entities.monster.EntityPech.becomeAngryAt`; `common.entities.monster.EntityPech.func_70071_h_` | TC-M6-06 (entities/AI) |
| pech_death | `PECH_DEATH` | pech_death.ogg | `common.entities.monster.EntityPech.func_70673_aS` | TC-M6-06 (entities/AI) |
| pech_dice | `PECH_DICE` | pech_dice.ogg | `client.gui.GuiPech.playButton` | TC-M7-04 (GUI parity) |
| pech_hit | `PECH_HIT` | pech_hit1.ogg, pech_hit2.ogg | `common.entities.monster.EntityPech.func_70621_aR` | TC-M6-06 (entities/AI) |
| pech_idle | `PECH_IDLE` | pech_idle1.ogg, pech_idle2.ogg, pech_idle3.ogg | `common.entities.monster.EntityPech.func_70639_aQ` | TC-M6-06 (entities/AI) |
| pech_trade | `PECH_TRADE` | pech_trade.ogg | `common.entities.ContainerPech.generateContents`; `common.entities.monster.EntityPech.func_70642_aH` | TC-M6-06 (entities/AI) |
| pump | `PUMP` | pump1.ogg, pump2.ogg, pump3.ogg | `common.tiles.TileCentrifuge.func_145845_h`; `common.tiles.TileEldritchLock.func_145845_h` | TC-M4-06/07 (essentia storage/transport); TC-M6-04 (tile catalog) |
| roots | `ROOTS` | roots1.ogg, roots2.ogg, roots3.ogg | `common.tiles.TileEtherealBloom.func_145845_h`; `common.blocks.BlockTaint.func_149674_a`; `common.blocks.BlockTaint.func_149696_a`; `common.blocks.BlockTaintFibres.func_149696_a`; `common.items.ItemSanitySoap.onUsingTick`; `common.entities.monster.EntityTaintacle.func_70639_aQ`; `common.entities.monster.EntityTaintSporeSwarmer.func_70639_aQ` | TC-M5-04 (taint); TC-M6-04 (item catalog); TC-M6-04 (tile catalog); TC-M6-06 (entities/AI) |
| rumble | `RUMBLE` | rumble.ogg | `common.tiles.TileArcaneBore.dig`; `common.items.wands.foci.ItemFocusExcavation.onUsingFocusTick` | TC-M5-02 (node behavior); TC-M6-02/03 (foci + casting) |
| runic_shield_charge | `RUNIC_SHIELD_CHARGE` | runic_shield_charge.ogg | `common.lib.events.EventHandlerRunic.entityHurt`; `common.blocks.BlockEldritch.func_149727_a` | TC-M6-04 (block catalog); TC-M6-07 (runic armor events) |
| runic_shield_effect | `RUNIC_SHIELD_EFFECT` | runic_shield_effect.ogg | `common.lib.events.EventHandlerRunic.entityHurt` | TC-M6-07 (runic armor events) |
| shock | `SHOCK` | shock1.ogg, shock2.ogg | `common.items.wands.foci.ItemFocusShock.doLightningBolt`; `common.entities.projectile.EntityGolemOrb.func_70184_a`; `common.entities.projectile.EntityShockOrb.func_70184_a`; `common.entities.monster.boss.EntityCultistPortal.func_70673_aS` | TC-M6-02/03 (foci + casting); TC-M6-03 (focus behaviors/projectiles); TC-M6-06 (entities/AI) |
| spill | `SPILL` | spill.ogg | `client.ClientProxy.crucibleBoilSound` | TC-M4-05 (crucible boiling) |
| squeek | `SQUEEK` | squeek1.ogg, squeek2.ogg | `common.tiles.TileTubeValve.func_145845_h`; `common.tiles.TileTubeBuffer.onWandRightClick`; `common.blocks.BlockTube.func_149727_a` | TC-M4-06/07 (essentia storage/transport); TC-M6-04 (block catalog) |
| swarm | `SWARM` | swarm1.ogg, swarm2.ogg, swarm3.ogg | `common.entities.monster.EntityTaintSpore.func_70639_aQ` | TC-M6-06 (entities/AI) |
| swarmattack | `SWARMATTACK` | swarmattack.ogg | `common.entities.monster.EntityTaintSwarm.func_70621_aR`; `common.entities.monster.EntityTaintSwarm.func_70673_aS`; `common.entities.monster.EntityTaintSwarm.func_70785_a` | TC-M6-06 (entities/AI) |
| swing | `SWING` | swing1.ogg, swing2.ogg | `common.items.equipment.ItemElementalSword.onLeftClickEntity` | TC-M6-07 (equipment) |
| tentacle | `TENTACLE` | tentacle1.ogg, tentacle2.ogg, tentacle3.ogg | `common.entities.monster.EntityTaintacle.func_70785_a`; `common.entities.monster.EntityTaintacle.spawnTentacles`; `common.entities.monster.EntityTaintacle.func_70621_aR`; `common.entities.monster.EntityTaintacle.func_70673_aS` | TC-M6-06 (entities/AI) |
| tool | `TOOL` | tool1.ogg, tool2.ogg | `common.tiles.TileTube.onWandRightClick`; `common.tiles.TileTubeValve.onWandRightClick`; `common.tiles.TileArcaneBoreBase.onWandRightClick`; `common.tiles.TileArcaneBore.onWandRightClick`; `common.tiles.TileTubeBuffer.onWandRightClick` | TC-M4-06/07 (essentia storage/transport); TC-M5-02 (node behavior) |
| upgrade | `UPGRADE` | upgrade.ogg | `common.entities.golems.EntityTravelingTrunk.func_70085_c`; `common.entities.golems.EntityGolemBase.func_70085_c` | TC-M6-06 (entities/AI) |
| urnbreak | `URNBREAK` | urnbreak.ogg | — (never played in original) | — (no site; registered for parity only) |
| wand | `WAND` | wand1.ogg, wand2.ogg, wand3.ogg | `common.tiles.TileFocalManipulator.func_145845_h`; `common.lib.events.ServerTickEventsFML.addSwapper`; `common.items.equipment.ItemElementalHoe.func_77648_a`; `common.items.wands.WandManager.createCrucible`; `common.items.wands.WandManager.replaceInfusionAltar`; `common.items.wands.WandManager.createThaumatorium`; `common.items.wands.WandManager.replaceNodeJar`; `common.items.wands.WandManager.replaceArcaneFurnace`; `common.items.wands.WandManager.createThaumonomicon`; `common.items.wands.WandManager.createOculus`; `common.items.wands.WandManager.createAdvancedAlchemicalFurnace` | TC-M5-02 (node behavior); TC-M6-01 (focal manipulator); TC-M6-01 (wands); TC-M6-07 (equipment) |
| wandfail | `WANDFAIL` | wandfail.ogg | `common.items.equipment.ItemElementalPickaxe.func_77648_a`; `common.items.wands.foci.ItemFocusHellbat.onFocusRightClick`; `common.items.wands.foci.ItemFocusPortableHole.onFocusRightClick`; `common.entities.monster.boss.EntityCultistPortal.func_70071_h_` | TC-M6-02/03 (foci + casting); TC-M6-06 (entities/AI); TC-M6-07 (equipment) |
| whispers | `WHISPERS` | whispers.ogg | `common.lib.network.playerdata.PacketWarpMessage.onMessage` | WIRED — ClientEffectHooks.warpNotification |
| wind | `WIND` | wind1.ogg, wind2.ogg | `common.items.equipment.ItemElementalSword.onUsingTick` | TC-M6-07 (equipment) |
| wispdead | `WISPDEAD` | wispdead.ogg | `common.entities.monster.EntityWisp.func_70673_aS` | TC-M6-06 (entities/AI) |
| wisplive | `WISPLIVE` | wisplive1.ogg, wisplive2.ogg, wisplive3.ogg | `common.entities.monster.EntityWisp.func_70639_aQ` | TC-M6-06 (entities/AI) |
| write | `WRITE` | write1.ogg, write2.ogg | `client.gui.GuiResearchTable.playButtonWrite`; `common.items.ItemResearchNotes.func_77659_a` | TC-M3-03 (Thaumonomicon GUI); TC-M6-04 (item catalog) |
| zap | `ZAP` | zap1.ogg, zap2.ogg | `common.lib.network.fx.PacketFXBlockZap.onMessage`; `common.items.relics.ItemHandMirror.func_77659_a`; `common.items.relics.ItemHandMirror.transport`; `common.items.wands.foci.ItemFocusWarding.onFocusRightClick`; `common.items.wands.foci.ItemFocusShock.onFocusRightClick`; `common.entities.projectile.EntityGolemOrb.func_70097_a`; `common.entities.projectile.EntityShockOrb.func_70097_a`; `common.entities.monster.EntityWisp.func_70626_be`; `common.entities.monster.boss.EntityCultistPortal.func_70100_b_`; `common.entities.monster.boss.EntityCultistPortal.func_70621_aR`; `common.entities.monster.boss.EntityEldritchWarden.performFieldFrenzy`; `common.entities.golems.ItemGolemBell.onLeftClickEntity` | TC-M6-02/03 (foci + casting); TC-M6-03 (focus behaviors/projectiles); TC-M6-04 (item catalog); TC-M6-06 (entities/AI); WIRED — ClientEffectHooks.fxBlockZap |

## Step sounds (never reach the thaumcraft namespace)

The original set custom step-sound names on blocks: `CustomStepSound`/
`CustomSoundType`/`JarStepSound("gore"|"jar"|"crystal"|"urnbreak")`
(BlockTaint, BlockTaintFibres, BlockFluxGoo, BlockJar, BlockMirror,
BlockCrystal, BlockLoot). 1.7.10 resolves step names through the vanilla
unnamespaced lookup (`step.*`/`dig.*`), so none of these ever played a
thaumcraft event — the surface was silent. 1.21.1 step sounds are
`SoundEvent`s on the block's `SoundSettings`; the owning content task
(TC-M5-04 taint, TC-M6-04 catalog) decides per block. `urnbreak` is the
only matching registered event; keeping it silent preserves original
behavior.

## Verification

- `./gradlew test` → `SoundRegistryParityTest`: registry ids == CSV event
  set == converted sounds.json keys; every referenced ogg exists in the
  overlay; no orphan oggs (static equivalent of SoundManager's
  "File ... does not exist, cannot add it to event ..." warning).
- `./gradlew runGameTestServer` → `SoundRegistryGameTest`: all 66 ids
  present in the SOUND_EVENT registry at runtime, no extras.
- client log check: a client run with the overlay must contain zero
  `Invalid sounds.json`, `cannot add it to event`, or missing-sound
  warnings (evidence recorded on the TC-M7-05 card).
