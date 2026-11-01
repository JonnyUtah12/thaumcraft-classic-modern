package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSpore;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSporeSwarmer;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSwarm;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacle;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacleSmall;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityThaumicSlime;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintSporeAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintSwarmAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.ThaumicSlimeAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityTaintacleGiant;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.TaintacleGiantAiLogic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaintMobs;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for the taint MONSTER slice (TC-M6-06-s-e): thaumslime, the
 * taintacle family (taintacle, taintacletiny, taintaclegiant) and the
 * spore/swarmer/swarm chain.
 *
 * <p>What the probes prove, world-side, is the part a unit test cannot reach:
 * the seven ids are in the live registry with the decompiled hitbox; every mob
 * ships ZERO goal selectors (the originals hand-roll their AI, and
 * {@code Monster#registerGoals} would install vanilla rows the original never
 * had); the attributes and xp are the decompiled numbers; the spore's burst
 * really spawns taint spiders and spends the mature stalk; the swarmer's burst
 * really spawns a {@code thaumcraft:taintswarm} through
 * {@code TaintManager.spawnGuarded} — which is the seam that flips on with this
 * card, since the id was unregistered before it; the taintacle's spawn guard
 * demands taint underfoot plus the taint biome and refuses a crowded cell; and
 * the giant caps a 100-damage hit to 35 and enrages exactly once. The pure
 * rules behind them are pinned world-free by {@code TaintacleAiLogicTest},
 * {@code TaintacleGiantAiLogicTest}, {@code TaintSporeAiLogicTest},
 * {@code TaintSwarmAiLogicTest} and {@code ThaumicSlimeAiLogicTest}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaintMonsterGameTest {

    private TaintMonsterGameTest() {
    }

    // ------------------------------------------------------------- registration

    @GameTest(template = "empty")
    public static void monsterSliceIsRegisteredWithTheOriginalBoxes(final GameTestHelper helper) {
        // EntityThaumicSlime.java:84 — 0.25*sqrt(size)+0.25 at the ctor's size 1
        assertBox(helper, "thaumslime", 0.5D, 0.5D);
        assertBox(helper, "taintacle", TaintacleAiLogic.WIDTH, TaintacleAiLogic.HEIGHT);        // :30
        assertBox(helper, "taintacletiny", TaintacleAiLogic.SMALL_WIDTH, TaintacleAiLogic.SMALL_HEIGHT); // Small:14
        assertBox(helper, "taintaclegiant", TaintacleGiantAiLogic.WIDTH, TaintacleGiantAiLogic.HEIGHT);  // Giant:24
        assertBox(helper, "taintspore", 0.5D, 0.5D);   // Spore:31 + :51 (max(0.15*2, 0.5))
        assertBox(helper, "taintswarmer", 1.0D, 1.0D); // Swarmer:27 — its OWN fixed box
        assertBox(helper, "taintswarm", 2.0D, 2.0D);
        helper.succeed();
    }

    private static void assertBox(final GameTestHelper helper, final String path,
            final double width, final double height) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
        helper.assertTrue(type != null, "thaumcraft:" + path + " is not in the entity-type registry");
        helper.assertTrue(Math.abs(type.getDimensions().width() - width) < 1.0E-4
                        && Math.abs(type.getDimensions().height() - height) < 1.0E-4,
                "thaumcraft:" + path + " hitbox is not the original " + width + " x " + height
                        + ", got " + type.getDimensions().width() + " x " + type.getDimensions().height());
    }

    @GameTest(template = "empty")
    public static void monsterSliceShipsZeroGoalSelectors(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        assertNoGoals(helper, "thaumslime", new EntityThaumicSlime(CatalogTaintMobs.THAUMIC_SLIME.get(), level));
        assertNoGoals(helper, "taintacle", new EntityTaintacle(CatalogTaintMobs.TAINTACLE.get(), level));
        assertNoGoals(helper, "taintacletiny",
                new EntityTaintacleSmall(CatalogTaintMobs.TAINTACLE_TINY.get(), level));
        assertNoGoals(helper, "taintaclegiant",
                new EntityTaintacleGiant(CatalogTaintMobs.TAINTACLE_GIANT.get(), level));
        assertNoGoals(helper, "taintspore", new EntityTaintSpore(CatalogTaintMobs.TAINT_SPORE.get(), level));
        assertNoGoals(helper, "taintswarmer",
                new EntityTaintSporeSwarmer(CatalogTaintMobs.TAINT_SWARMER.get(), level));
        assertNoGoals(helper, "taintswarm", new EntityTaintSwarm(CatalogTaintMobs.TAINT_SWARM.get(), level));
        helper.succeed();
    }

    /**
     * Every original installs no {@code goalSelector}/{@code targetSelector} row
     * (none of the seven decompiled files calls the selector API), so the port
     * must not inherit {@code Monster#registerGoals}'s look-at-player,
     * random-look and player-target rows.
     */
    private static void assertNoGoals(final GameTestHelper helper, final String path,
            final net.minecraft.world.entity.monster.Monster mob) {
        helper.assertTrue(mob.goalSelector.getAvailableGoals().isEmpty(),
                path + " installed " + mob.goalSelector.getAvailableGoals().size()
                        + " goal rows; the original installs none");
        helper.assertTrue(mob.targetSelector.getAvailableGoals().isEmpty(),
                path + " installed " + mob.targetSelector.getAvailableGoals().size()
                        + " target rows; the original installs none");
    }

    @GameTest(template = "empty")
    public static void monsterSliceCarriesTheOriginalAttributesAndXp(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();

        final EntityTaintacle taintacle = new EntityTaintacle(CatalogTaintMobs.TAINTACLE.get(), level);
        helper.assertTrue(taintacle.getAttributeValue(Attributes.MAX_HEALTH) == TaintacleAiLogic.HEALTH
                        && taintacle.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintacleAiLogic.ATTACK,
                "EntityTaintacle.java:60-61 is 50 health / 7 attack, got "
                        + taintacle.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + taintacle.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(TaintacleAiLogic.XP == 10,
                "EntityTaintacle.java:31 sets field_70728_aV to 10 (xpReward is protected in 21.1.259, "
                        + "so the world probe pins the rule the ctor feeds it)");

        final EntityTaintacleSmall tiny = new EntityTaintacleSmall(CatalogTaintMobs.TAINTACLE_TINY.get(), level);
        helper.assertTrue(tiny.getAttributeValue(Attributes.MAX_HEALTH) == TaintacleAiLogic.SMALL_HEALTH
                        && tiny.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintacleAiLogic.SMALL_ATTACK,
                "EntityTaintacleSmall.java:21-22 is 8 health / 2 attack");
        helper.assertTrue(TaintacleAiLogic.SMALL_XP == 0,
                "EntityTaintacleSmall.java:15 sets the xp reward to 0 — a taintlet is worth nothing");

        final EntityTaintacleGiant giant = new EntityTaintacleGiant(CatalogTaintMobs.TAINTACLE_GIANT.get(), level);
        helper.assertTrue(giant.getAttributeValue(Attributes.MAX_HEALTH) == TaintacleGiantAiLogic.HEALTH
                        && giant.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintacleGiantAiLogic.ATTACK,
                "EntityTaintacleGiant.java:31-32 is 125 health / 9 attack");
        helper.assertTrue(TaintacleGiantAiLogic.XP == 20,
                "EntityTaintacleGiant.java:25 sets field_70728_aV to 20");

        final EntityTaintSwarm swarm = new EntityTaintSwarm(CatalogTaintMobs.TAINT_SWARM.get(), level);
        helper.assertTrue(swarm.getAttributeValue(Attributes.MAX_HEALTH) == TaintSwarmAiLogic.HEALTH
                        && swarm.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintSwarmAiLogic.ATTACK_BASE,
                "the taint swarm's registered attributes drifted (30 health / 2 attack)");
        helper.assertTrue(!swarm.isPushable(),
                "EntityTaintSwarm.java:70-72 and :180-182 both return false — the swarm is not pushable");

        final EntityTaintSporeSwarmer swarmer =
                new EntityTaintSporeSwarmer(CatalogTaintMobs.TAINT_SWARMER.get(), level);
        swarmer.setSporeSize(10);
        helper.assertTrue(TaintSporeAiLogic.xpForSize(10) == 10,
                "EntityTaintSpore.java:89 + Swarmer:29 — the xp reward is the SIZE, so a size-10 swarmer is 10");
        helper.assertTrue(Math.abs(swarmer.getDimensions(Pose.STANDING).width() - 1.0D) < 1.0E-4
                        && Math.abs(swarmer.getDimensions(Pose.STANDING).height() - 1.0D) < 1.0E-4,
                "EntityTaintSporeSwarmer.java:27 pins a FIXED 1x1 box: a size-10 swarmer must NOT take the "
                        + "size-10 spore's 1.5 box, got " + swarmer.getDimensions(Pose.STANDING).width());
        helper.succeed();
    }

    // ------------------------------------------------------------- thaumslime

    @GameTest(template = "empty")
    public static void thaumslimeSizeLadderIsTheSquareRootOfItsSize(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityThaumicSlime slime = new EntityThaumicSlime(CatalogTaintMobs.THAUMIC_SLIME.get(), level);
        slime.moveTo(helper.absolutePos(new BlockPos(2, 5, 2)), 0.0F, 0.0F);

        // EntityThaumicSlime.java:83-90 — box 0.25*sqrt(size)+0.25 on BOTH axes,
        // max-health attribute IS the size, xp is (int) sqrt(size).
        slime.setSlimeSize(4);
        helper.assertTrue(slime.getSlimeSize() == 4, "the size watcher (data 16) is the size");
        helper.assertTrue(Math.abs(slime.getDimensions(Pose.STANDING).width() - 0.75D) < 1.0E-4
                        && Math.abs(slime.getDimensions(Pose.STANDING).height() - 0.75D) < 1.0E-4,
                "EntityThaumicSlime.java:84 boxes size 4 as 0.25*2+0.25 = 0.75 x 0.75, got "
                        + slime.getDimensions(Pose.STANDING).width());
        helper.assertTrue(slime.getAttributeValue(Attributes.MAX_HEALTH) == 4.0D
                        && slime.getHealth() == 4.0D,
                "EntityThaumicSlime.java:86-87 — the MAX_HEALTH attribute IS the size and health refills to it");
        helper.assertTrue(ThaumicSlimeAiLogic.sizeToExperience(4) == 2,
                "EntityThaumicSlime.java:88 — xp is (int) sqrt(size), so size 4 is 2");

        slime.setSlimeSize(9);
        helper.assertTrue(Math.abs(slime.getDimensions(Pose.STANDING).width() - 1.0D) < 1.0E-4
                        && ThaumicSlimeAiLogic.sizeToExperience(9) == 3,
                "size 9 is the 1.0 x 1.0 box and 3 xp (sqrt(9))");

        // The ctor's own roll: 1 << rand(3) — only ever 1, 2 or 4 (ctor lines 31-34).
        for (int roll = 0; roll < 3; roll++) {
            final EntityThaumicSlime fresh =
                    new EntityThaumicSlime(CatalogTaintMobs.THAUMIC_SLIME.get(), level);
            helper.assertTrue(fresh.getSlimeSize() == 1 || fresh.getSlimeSize() == 2
                            || fresh.getSlimeSize() == 4,
                    "EntityThaumicSlime.java:31 rolls the size as 1 << rand(3) — got " + fresh.getSlimeSize());
        }
        helper.assertTrue(ThaumicSlimeAiLogic.initialSize(0) == 1
                        && ThaumicSlimeAiLogic.initialSize(1) == 2
                        && ThaumicSlimeAiLogic.initialSize(2) == 4,
                "the pinned initial-size rule is 1 << roll");
        helper.succeed();
    }

    // ------------------------------------------------------------- taintacle

    @GameTest(template = "empty")
    public static void taintacleSpawnGuardWantsTaintUnderfootAndACrowdedCellRefuses(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos feet = helper.absolutePos(new BlockPos(4, 5, 4));
        level.setBlockAndUpdate(feet, Blocks.STONE.defaultBlockState());
        TaintBiomes.setBiomeAtAndSync(level, feet.getX(), feet.getZ(), TaintBiomes.taintBiomeHolder(level));

        // The probe entity is deliberately NOT added to the level: EntityTaintacle.java:44
        // scans the box around itself and requires ents.size() <= 0, so a spawned
        // neighbour is the crowding case and the probing entity must stay outside.
        final EntityTaintacle probe = new EntityTaintacle(CatalogTaintMobs.TAINTACLE.get(), level);
        probe.moveTo(feet.getX() + 0.5, feet.getY() + 1.0, feet.getZ() + 0.5, 0.0F, 0.0F);

        helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                "EntityTaintacle.java:46-50: on plain stone the spawn rule refuses, even in a taint biome");

        // :46-50 second branch — taint SOIL (meta 1) under the mob.
        TaintManager.setTaint(level, feet, BlockTaint.META_SOIL);
        helper.assertTrue(probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                "EntityTaintacle.java:46-50: taint soil (meta 1) in a taint biome on an empty cell must spawn");

        // :42-44 — one taintacle already inside the 24/8/24 scan box refuses the spawn.
        final EntityTaintacle neighbour = new EntityTaintacle(CatalogTaintMobs.TAINTACLE.get(), level);
        neighbour.moveTo(feet.getX() + 0.5, feet.getY() + 1.0, feet.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(neighbour);
        helper.assertTrue(TaintManager.countEntities(level,
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintacle"),
                        TaintacleAiLogic.spawnScanBox(probe.getX(), probe.getY(), probe.getZ())) == 1,
                "EntityTaintacle.java:42-44 scans a 24/8/24 box for the class — the probe must see exactly 1");
        helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                "a crowded cell refuses: EntityTaintacle.java:44 requires ents.size() <= 0");

        // And fibres meta 0 (fibrous taint) is the other accepted floor (:46).
        neighbour.discard();
        TaintManager.setFibres(level, feet, BlockTaintFibres.META_FIBRES);
        helper.assertTrue(probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                "EntityTaintacle.java:46: fibrous taint (meta 0) under the mob is a valid spawn floor");
        helper.succeed();
    }

    // ------------------------------------------------------- spore / swarmer

    @GameTest(template = "empty")
    public static void sporeBurstLaunchesSpiderlingsAndSpendsTheMatureStalk(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos stalk = helper.absolutePos(new BlockPos(6, 5, 6));
        TaintManager.setFibres(level, stalk, BlockTaintFibres.META_MATURE_SPORE_STALK);
        TaintBiomes.setBiomeAtAndSync(level, stalk.getX(), stalk.getZ(), TaintBiomes.taintBiomeHolder(level));

        final EntityTaintSpore spore = new EntityTaintSpore(CatalogTaintMobs.TAINT_SPORE.get(), level);
        spore.setSporeSize(10);
        spore.moveTo(stalk.getX() + 0.5, stalk.getY() + 1.0, stalk.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(spore);

        // :145-152 — rooted on a MATURE stalk with no hurt flash means NO burst at all.
        spore.tick();
        helper.assertTrue(spiderCount(helper, spore) == 0,
                "EntityTaintSpore.java:145-152: a spore rooted on a mature stalk and unhurt must not burst");
        helper.assertTrue(!spore.isRemoved(), "the rooted spore is still alive");

        // :150 — the burst fires on the hurt DURATION (field_70725_aQ > 0), and
        // :162 makes size / 3 + rand(size / 2 + 1) spiderlings: at size 10 that is
        // 3 + [0..5], so at least 3 is the deterministic floor.
        spore.hurt(level.damageSources().magic(), 1.0F);
        spore.tick();
        final int spiders = spiderCount(helper, spore);
        helper.assertTrue(spiders >= TaintSporeAiLogic.burstCount(10, 0),
                "EntityTaintSpore.java:162 at size 10 spawns 10/3 = 3 spiderlings minimum, got " + spiders);
        helper.assertTrue(spore.isRemoved(), "EntityTaintSpore.java:183 — the burst discards the spore");
        helper.assertTrue(TaintManager.isTaintBlock(level.getBlockState(stalk))
                        && level.getBlockState(stalk).getValue(BlockTaintFibres.VARIANT)
                                == BlockTaintFibres.META_SPORE_STALK,
                "EntityTaintSpore.java:179-180 — the spent mature stalk drops back to meta 3 (spore stalk)");
        helper.succeed();
    }

    private static int spiderCount(final GameTestHelper helper, final Entity around) {
        return helper.getLevel().getEntitiesOfClass(EntityTaintSpider.class,
                new AABB(around.position().add(-16, -16, -16), around.position().add(16, 16, 16))).size();
    }

    @GameTest(template = "empty")
    public static void swarmerBurstSpawnsATaintSwarmThroughTheSpawnGuard(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos cell = helper.absolutePos(new BlockPos(10, 5, 10));
        TaintBiomes.setBiomeAtAndSync(level, cell.getX(), cell.getZ(), TaintBiomes.taintBiomeHolder(level));

        final EntityTaintSporeSwarmer swarmer =
                new EntityTaintSporeSwarmer(CatalogTaintMobs.TAINT_SWARMER.get(), level);
        swarmer.setSporeSize(10);
        swarmer.moveTo(cell.getX() + 0.5, cell.getY() + 1.0, cell.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(swarmer);

        // EntityTaintSporeSwarmer.java:72-74 — the burst fires on hurtDuration == 1
        // (1.7.10 field_70725_aQ), i.e. on the LAST flash tick of the hit.
        swarmer.hurt(level.damageSources().magic(), 1.0F);
        int ticks = 0;
        while (swarmCount(helper, swarmer) == 0 && ticks < 40) {
            swarmer.tick();
            ticks++;
        }
        helper.assertTrue(swarmCount(helper, swarmer) >= 1,
                "EntityTaintSporeSwarmer.java:72-74 + :86-88: the hurt-flash burst must spawn a "
                        + "thaumcraft:taintswarm through TaintManager.spawnGuarded (the id registered by "
                        + "this card); after " + ticks + " ticks the scan found "
                        + swarmCount(helper, swarmer));
        helper.succeed();
    }

    private static int swarmCount(final GameTestHelper helper, final Entity around) {
        return helper.getLevel().getEntitiesOfClass(EntityTaintSwarm.class,
                new AABB(around.position().add(-16, -16, -16), around.position().add(16, 16, 16))).size();
    }

    @GameTest(template = "empty")
    public static void spawnGuardIdsForThisSliceAreLive(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos cell = helper.absolutePos(new BlockPos(14, 5, 14));
        // The taint card's deferred seams: TaintManager.spawnGuarded resolves the id
        // from the registry at call time, so before this card it returned false.
        helper.assertTrue(TaintManager.spawnGuarded(level,
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintspore"),
                        cell.getCenter()),
                "thaumcraft:taintspore must resolve in TaintManager.spawnGuarded now that it is registered");
        helper.assertTrue(TaintManager.spawnGuarded(level,
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintswarmer"),
                        cell.getCenter()),
                "thaumcraft:taintswarmer must resolve in TaintManager.spawnGuarded");
        helper.assertTrue(TaintManager.spawnGuarded(level,
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintswarm"),
                        cell.getCenter()),
                "thaumcraft:taintswarm must resolve in TaintManager.spawnGuarded");
        helper.assertTrue(TaintManager.countEntities(level,
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taintspore"),
                        new AABB(net.minecraft.world.phys.Vec3.atBottomCenterOf(cell.offset(-8, -8, -8)),
                                net.minecraft.world.phys.Vec3.atBottomCenterOf(cell.offset(8, 8, 8)))) == 1,
                "the guarded spawn must land a live taintspore in the world");
        helper.succeed();
    }

    // ------------------------------------------------------------- taintswarm

    @GameTest(template = "empty")
    public static void unsummonedTaintSwarmDropsFiveHealthATick(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos cell = helper.absolutePos(new BlockPos(18, 6, 18));
        final EntityTaintSwarm swarm = new EntityTaintSwarm(CatalogTaintMobs.TAINT_SWARM.get(), level);
        swarm.moveTo(cell.getX() + 0.5, cell.getY() + 1.0, cell.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(swarm);
        helper.assertTrue(!swarm.getIsSummoned(),
                "EntityTaintSwarm.java:88-90 — the summoned flag (data 16) starts clear");

        // :121-123 — an unsummoned swarm with no target takes 5.0F generic damage per tick.
        final double before = swarm.getHealth();
        for (int i = 0; i < 3; i++) {
            swarm.tick();
        }
        helper.assertTrue(swarm.getHealth() < before,
                "EntityTaintSwarm.java:121-123: an unsummoned target-less swarm takes 5 damage a tick, "
                        + "health went from " + before + " to " + swarm.getHealth());

        // :89 — clearing the flag writes flags & -4; setting it stops the self-damage.
        swarm.setIsSummoned(true);
        helper.assertTrue(swarm.getIsSummoned(), "the summoned flag round-trips");
        final double summoned = swarm.getHealth();
        for (int i = 0; i < 3; i++) {
            swarm.tick();
        }
        helper.assertTrue(swarm.getHealth() == summoned,
                "a summoned swarm must not take the unsummoned self-damage (:121-123)");
        helper.succeed();
    }

    // ---------------------------------------------------------------- giant

    @GameTest(template = "empty")
    public static void giantCapsTheBlowAtThirtyFiveAndEnragesExactlyOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos cell = helper.absolutePos(new BlockPos(22, 7, 22));
        final EntityTaintacleGiant giant =
                new EntityTaintacleGiant(CatalogTaintMobs.TAINTACLE_GIANT.get(), level);
        giant.moveTo(cell.getX() + 0.5, cell.getY() + 1.0, cell.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(giant);
        helper.assertTrue(giant.getAnger() == 0, "EntityTaintacleGiant.java:70 — anger starts at 0");
        helper.assertTrue(giant.getBossEvent() != null, "the boss bar exists (IBossDisplayData seam)");
        helper.assertTrue(giant.checkSpawnRules(level, MobSpawnType.COMMAND) == false,
                "EntityTaintacleGiant.java:80-84 — getCanSpawnHere is false: the giant never spawns naturally");

        // :110-126 — a server hit over 35 enrages (anger 0) and the hit's damage is
        // then CAPPED to 35. A 100-damage blow leaves 125 - 35 = 90.
        giant.hurt(level.damageSources().generic(), 100.0F);
        helper.assertTrue(giant.getAnger() == TaintacleGiantAiLogic.ANGER_ON_ENRAGE,
                "EntityTaintacleGiant.java:116 sets the anger to 200 on the enrage, got " + giant.getAnger());
        helper.assertTrue(giant.getHealth() == TaintacleGiantAiLogic.HEALTH - TaintacleGiantAiLogic.ENRAGE_DAMAGE_CAP,
                "EntityTaintacleGiant.java:126 caps the blow to 35, so 125 health must land on 90, got "
                        + giant.getHealth());

        // :111 — the enrage needs anger == 0, so a second big blow cannot re-enrage.
        giant.hurt(level.damageSources().generic(), 100.0F);
        helper.assertTrue(giant.getAnger() == TaintacleGiantAiLogic.ANGER_ON_ENRAGE,
                "EntityTaintacleGiant.java:111 — anger must not re-arm while it is already 200");

        // :43-45 — anger ticks down 1 per tick, so the boss enrages at most once per 200 ticks.
        giant.tick();
        helper.assertTrue(giant.getAnger() == TaintacleGiantAiLogic.ANGER_ON_ENRAGE - 1,
                "EntityTaintacleGiant.java:43-45 — the anger counter ticks down by 1 a tick, got "
                        + giant.getAnger());

        // The watcher is a Short: 70000 narrows to 4464 (EntityTaintacleGiant.java:78).
        giant.setAnger(70000);
        helper.assertTrue(giant.getAnger() == 4464,
                "EntityTaintacleGiant.java:78 narrows the setter to a Short: 70000 -> 4464, got "
                        + giant.getAnger());
        helper.succeed();
    }
}
