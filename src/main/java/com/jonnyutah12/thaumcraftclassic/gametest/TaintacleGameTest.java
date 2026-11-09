package com.jonnyutah12.thaumcraftclassic.gametest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacle;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacleSmall;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityTaintacleGiant;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.TaintacleGiantAiLogic;
import com.jonnyutah12.thaumcraftclassic.taint.TaintHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for the taintacle family (TC-M6-06-s-e-ii):
 * {@code thaumcraft:taintacle}, {@code thaumcraft:taintacletiny} (the original's
 * display name is <em>Taint Tendril</em>) and {@code thaumcraft:taintaclegiant}.
 *
 * <p>What a world probe proves and a JUnit test cannot: the three ids are in the
 * live registry with the decompiled hitbox; every family member ships ZERO goal
 * selectors (the originals hand-roll their AI, and 1.21.1's
 * {@code Monster#registerGoals} would install rows the original never had); the
 * registered attributes and the ctor's xp reward are the decompiled numbers; the
 * giant caps a 100-damage blow to 35, enrages exactly once, never spawns
 * naturally, and narrows its anger watcher to a Short.</p>
 *
 * <p>The taint half of the AI is proven against the seam, not against the taint
 * engine, because the TC-M5-04-s taint engine is not on this branch (its probes
 * are on {@code wt/taint} with the documented 12-of-16-cell disc defect):
 * {@code theTaintacleSpawnGateAsksTheTaintSeam} installs a stub provider and
 * watches the spawn gate flip with the answers it gives, and
 * {@code theFarHitAsksTheSeamForItsGuardedTaintletSpawn} proves the burst hands
 * the taintlet spawn to the engine's guard. Both probes restore the provider
 * they found in a {@code finally}, and the ambient-count discipline (deltas over
 * a baseline, not absolutes) is the batch rule: the server packs the structures
 * of a batch side by side, so an absolute scan of the taintacle's 24×8×24
 * proximity box reads a neighbour probe's mobs.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaintacleGameTest {

    private TaintacleGameTest() {
    }

    // ----------------------------------------------------------- registration

    /** The three ids exist in the live registry with the original's boxes. */
    @GameTest(template = "empty")
    public static void familyIsRegisteredWithTheOriginalBoxes(final GameTestHelper helper) {
        assertBox(helper, "taintacle", TaintacleAiLogic.WIDTH, TaintacleAiLogic.HEIGHT);            // EntityTaintacle.java:30
        assertBox(helper, "taintacletiny", TaintacleAiLogic.SMALL_WIDTH, TaintacleAiLogic.SMALL_HEIGHT); // EntityTaintacleSmall.java:14
        assertBox(helper, "taintaclegiant", TaintacleGiantAiLogic.WIDTH, TaintacleGiantAiLogic.HEIGHT);  // EntityTaintacleGiant.java:24
        helper.succeed();
    }

    private static void assertBox(final GameTestHelper helper, final String path,
            final double width, final double height) {
        final EntityType<?> type = entityType(path);
        helper.assertTrue(Math.abs(type.getDimensions().width() - width) < 1.0E-4
                        && Math.abs(type.getDimensions().height() - height) < 1.0E-4,
                "thaumcraft:" + path + " hitbox is not the original " + width + " x " + height
                        + ", got " + type.getDimensions().width() + " x " + type.getDimensions().height());
    }

    /**
     * 1.7.10 set the box in the CONSTRUCTOR ({@code func_70105_a}), which is
     * per-instance, so the running entity must carry it, not just the builder:
     * the taintacle 0.66×3.0, the taintlet 0.22×1.0, the giant 1.1×6.0. All three
     * carry the family's 0.25 eye height — the taintacle's own
     * {@code func_70053_R} ({@code EntityTaintacle.java:54-56}) is inherited by
     * both subclasses, which override neither.
     */
    @GameTest(template = "empty")
    public static void familyCarriesTheOriginalBoxAndEyeHeightOnTheInstance(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        assertInstanceBox(helper, "taintacle", new EntityTaintacle(entityType("taintacle"), level),
                TaintacleAiLogic.WIDTH, TaintacleAiLogic.HEIGHT);
        assertInstanceBox(helper, "taintacletiny",
                new EntityTaintacleSmall(entityType("taintacletiny"), level),
                TaintacleAiLogic.SMALL_WIDTH, TaintacleAiLogic.SMALL_HEIGHT);
        assertInstanceBox(helper, "taintaclegiant",
                new EntityTaintacleGiant(entityType("taintaclegiant"), level),
                TaintacleGiantAiLogic.WIDTH, TaintacleGiantAiLogic.HEIGHT);
        helper.succeed();
    }

    private static void assertInstanceBox(final GameTestHelper helper, final String path,
            final EntityTaintacle mob, final double width, final double height) {
        final Pose pose = Pose.STANDING;
        helper.assertTrue(Math.abs(mob.getDimensions(pose).width() - width) < 1.0E-4
                        && Math.abs(mob.getDimensions(pose).height() - height) < 1.0E-4,
                "thaumcraft:" + path + " carries " + mob.getDimensions(pose).width() + " x "
                        + mob.getDimensions(pose).height() + " on the instance, the original ctor sets "
                        + width + " x " + height);
        helper.assertTrue(Math.abs(mob.getDimensions(pose).eyeHeight() - TaintacleAiLogic.EYE_HEIGHT) < 1.0E-4,
                "thaumcraft:" + path + " eye height is " + mob.getDimensions(pose).eyeHeight()
                        + ", EntityTaintacle.java:54-56 returns 0.25F and the subclasses override nothing");
    }

    /**
     * The originals install no goal selectors at all, so the port must not
     * inherit {@code Monster#registerGoals}'s look-at-player, random-look and
     * player-target rows.
     */
    @GameTest(template = "empty")
    public static void familyShipsZeroGoalSelectors(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        assertNoGoals(helper, "taintacle", new EntityTaintacle(entityType("taintacle"), level));
        assertNoGoals(helper, "taintacletiny",
                new EntityTaintacleSmall(entityType("taintacletiny"), level));
        assertNoGoals(helper, "taintaclegiant",
                new EntityTaintacleGiant(entityType("taintaclegiant"), level));
        helper.succeed();
    }

    private static void assertNoGoals(final GameTestHelper helper, final String path, final Monster mob) {
        helper.assertTrue(mob.goalSelector.getAvailableGoals().isEmpty(),
                path + " installed " + mob.goalSelector.getAvailableGoals().size()
                        + " goal rows; the original installs none");
        helper.assertTrue(mob.targetSelector.getAvailableGoals().isEmpty(),
                path + " installed " + mob.targetSelector.getAvailableGoals().size()
                        + " target rows; the original installs none");
    }

    /**
     * The attribute pair each original registers ({@code func_110147_ax}): 50/7,
     * 8/2, 125/9 — plus the ctor's {@code field_70728_aV} xp: 10, 0, 20. None of
     * them registers a movement speed, and a taintacle is rooted
     * ({@code EntityTaintacle.java:98-106}), so the port keeps 1.21.1's default.
     */
    @GameTest(template = "empty")
    public static void familyCarriesTheOriginalAttributesAndXp(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();

        final EntityTaintacle taintacle = new EntityTaintacle(entityType("taintacle"), level);
        helper.assertTrue(taintacle.getAttributeValue(Attributes.MAX_HEALTH) == TaintacleAiLogic.HEALTH
                        && taintacle.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintacleAiLogic.ATTACK,
                "EntityTaintacle.java:60-61 is 50 health / 7 attack, got "
                        + taintacle.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + taintacle.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(taintacle.xpReward() == TaintacleAiLogic.XP,
                "EntityTaintacle.java:31 sets field_70728_aV to 10, got " + taintacle.xpReward());
        helper.assertTrue(Math.abs(taintacle.getSoundVolume()) > 0.0F
                        && Math.abs(taintacle.getSoundVolume() - TaintacleAiLogic.soundVolume(50.0F)) < 1.0E-4,
                "EntityTaintacle.java:287-289 — volume is maxHealth/8 = 6.25, got "
                        + taintacle.getSoundVolume());
        helper.assertTrue(Math.abs(taintacle.getVoicePitch() + 3.7F) < 1.0E-4,
                "EntityTaintacle.java:283-285 — pitch is 1.3 - maxHealth/10 = -3.7 for a 50-heart "
                        + "taintacle; the negative pitch is the original's quirk, got "
                        + taintacle.getVoicePitch());

        final EntityTaintacleSmall tiny = new EntityTaintacleSmall(entityType("taintacletiny"), level);
        helper.assertTrue(tiny.getAttributeValue(Attributes.MAX_HEALTH) == TaintacleAiLogic.SMALL_HEALTH
                        && tiny.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintacleAiLogic.SMALL_ATTACK,
                "EntityTaintacleSmall.java:21-22 is 8 health / 2 attack, got "
                        + tiny.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + tiny.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(tiny.xpReward() == TaintacleAiLogic.SMALL_XP,
                "EntityTaintacleSmall.java:15 — a taintlet is worth no experience, got " + tiny.xpReward());
        helper.assertTrue(tiny.getLifetime() == TaintacleAiLogic.SMALL_LIFETIME_START,
                "EntityTaintacleSmall.java:10 — the lifetime counter starts at 200, got "
                        + tiny.getLifetime());

        final EntityTaintacleGiant giant = new EntityTaintacleGiant(entityType("taintaclegiant"), level);
        helper.assertTrue(giant.getAttributeValue(Attributes.MAX_HEALTH) == TaintacleGiantAiLogic.HEALTH
                        && giant.getAttributeValue(Attributes.ATTACK_DAMAGE) == TaintacleGiantAiLogic.ATTACK,
                "EntityTaintacleGiant.java:31-32 is 125 health / 9 attack, got "
                        + giant.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + giant.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(giant.xpReward() == TaintacleGiantAiLogic.XP,
                "EntityTaintacleGiant.java:25 sets field_70728_aV to 20, got " + giant.xpReward());
        helper.assertTrue(giant.getBossEvent() != null, "the boss bar exists (IBossDisplayData seam)");
        helper.succeed();
    }

    // ------------------------------------------------------------------ taint

    /**
     * The spawn gate ({@code EntityTaintacle.java:34-51}) asks the seam for both
     * taint answers, and the answers it gets decide the result: no taint anywhere
     * (this branch's world) refuses on every floor, taint biome + taint floor
     * hands the decision to the vanilla mob gate, and each half alone refuses.
     * A crowded 24×8×24 box refuses even with both halves true.
     */
    @GameTest(template = "thaumcraft:gametest_pad_32", timeoutTicks = 400)
    public static void theTaintacleSpawnGateAsksTheTaintSeam(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos floor = helper.absolutePos(new BlockPos(6, 5, 6));
        // EntityTaintacle.standingOnTaint reads the block AT blockPosition() — the
        // cell the mob occupies — so the floor the gate checks is floor.above().
        final BlockPos cell = floor.above();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());

        final EntityTaintacle probe = new EntityTaintacle(entityType("taintacle"), level);
        probe.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
        // The vanilla half of the gate measured on a plain monster, so the probe
        // asserts the taint half and not the gametest dimension's light level.
        final boolean vanillaGate = new Zombie(level).checkSpawnRules(level, MobSpawnType.COMMAND);

        helper.assertTrue(TaintHooks.current() == TaintHooks.NO_TAINT,
                "the seam must default to NO_TAINT on a branch with no taint engine; current() is "
                        + TaintHooks.current());
        helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                "EntityTaintacle.java:46-50 — with no taint underfoot the spawn rule refuses, even "
                        + "when the vanilla gate says " + vanillaGate);

        final TaintHooks.Provider previous = TaintHooks.current();
        final StubTaint stub = new StubTaint(true, Blocks.SOUL_SAND);
        try {
            TaintHooks.install(stub);
            helper.assertTrue(TaintHooks.current() == stub, "install() must publish the provider");
            level.setBlockAndUpdate(cell, Blocks.SOUL_SAND.defaultBlockState());
            helper.assertTrue(probe.checkSpawnRules(level, MobSpawnType.COMMAND) == vanillaGate,
                    "EntityTaintacle.java:39 + :46-50 — taint biome plus taint floor leaves the "
                            + "vanilla gate (" + vanillaGate + ") as the only decider");
            helper.assertTrue(stub.groundChecks.get() > 0 && stub.biomeChecks.get() > 0,
                    "the gate must ASK the seam: ground checks " + stub.groundChecks.get()
                            + ", biome checks " + stub.biomeChecks.get());

            // :39 — a taint floor in a NON-taint biome refuses.
            stub.inTaintBiome = false;
            helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                    "EntityTaintacle.java:39 — the biome test is required, not just the floor");

            // :46-50 — the taint biome on plain stone refuses.
            stub.inTaintBiome = true;
            level.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());
            helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                    "EntityTaintacle.java:46-50 — only fibres meta 0 / taint meta 1 count as the floor");

            // :42-44 — one taintacle inside the 24/8/24 box refuses the spawn.
            level.setBlockAndUpdate(cell, Blocks.SOUL_SAND.defaultBlockState());
            final int baseline = countFamily(level, probe);
            final EntityTaintacle neighbour = new EntityTaintacle(entityType("taintacle"), level);
            neighbour.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(neighbour);
            try {
                helper.assertTrue(countFamily(level, probe) == baseline + 1,
                        "EntityTaintacle.java:42-44 scans a 24x8x24 box (box "
                                + TaintacleAiLogic.spawnScanBox(probe.getX(), probe.getY(), probe.getZ())
                                + "): the delta over the ambient baseline must be exactly 1");
                helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                        "EntityTaintacle.java:44 requires ents.size() <= 0 — a crowded cell refuses");
            } finally {
                neighbour.discard();
            }
        } finally {
            TaintHooks.install(previous);
            level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(TaintHooks.current() == previous, "the probe must restore the provider it found");
        helper.succeed();
    }

    /**
     * {@code EntityTaintacle.java:240-249} — a hit from beyond 16 blocks bursts
     * taintlets AT THE ATTACKER, and the spawn goes through the engine's guarded
     * spawn ({@code :221-229}). Off taint the gate returns first and asks for
     * nothing; on taint the guard is called with the taintlet's id near the
     * attacker's feet, and the shared swing counter takes the 40 + rand(20)
     * cooldown ({@code :220}).
     */
    @GameTest(template = "thaumcraft:gametest_pad_32", timeoutTicks = 400)
    public static void theFarHitAsksTheSeamForItsGuardedTaintletSpawn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos floor = helper.absolutePos(new BlockPos(6, 5, 6));
        final BlockPos cell = floor.above();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());

        // The attacker sits past the 16-block line (EntityTaintacle.java:243) and
        // is a plain animal, so no mock player leaks into the batch's mob scans.
        final Cow attacker = new Cow(entityType("cow"), level);
        attacker.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 30.5, 0.0F, 0.0F);
        level.addFreshEntity(attacker);
        final EntityTaintacle probe = new EntityTaintacle(entityType("taintacle"), level);
        probe.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
        final DamageSource farHit = new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.GENERIC_KILL),
                attacker, attacker);

        final TaintHooks.Provider previous = TaintHooks.current();
        try {
            // Off taint: the gate (:213-219) returns before anything is asked for.
            probe.setAttackTime(0);
            probe.hurt(farHit, 1.0F);
            helper.assertTrue(probe.getAttackTime() == 0,
                    "EntityTaintacle.java:213-219 — with no taint biome and no taint ground the burst "
                            + "returns at the gate, so the swing counter stays 0, got "
                            + probe.getAttackTime());
            helper.assertTrue(!attacker.isRemoved() && attacker.getHealth() == 10.0F,
                    "the refused burst must not touch the attacker, health " + attacker.getHealth());

            // On taint: both taint answers true, so the guarded taintlet spawn runs.
            final StubTaint stub = new StubTaint(true, Blocks.SOUL_SAND);
            stub.taintEveryBlock = true;
            TaintHooks.install(stub);
            probe.hurt(farHit, 1.0F);
            helper.assertTrue(stub.spawnCalls.get() == 1,
                    "EntityTaintacle.java:221-229 — the burst hands the taintlet to the engine's spawn "
                            + "guard exactly once, got " + stub.spawnCalls.get() + " calls");
            helper.assertTrue(EntityTaintacle.TAINTLET_ID.equals(stub.lastSpawnId.get()),
                    "the spawned id must be the taintlet thaumcraft:taintacletiny, got "
                            + stub.lastSpawnId.get());
            final Vec3 last = stub.lastSpawnAt.get();
            helper.assertTrue(Math.abs(last.x - attacker.getX()) <= 1.0
                            && Math.abs(last.z - attacker.getZ()) <= 1.0
                            && Math.abs(last.y - attacker.getY()) < 1.0E-9,
                    "EntityTaintacle.java:221-229 — the taintlet lands on the ATTACKER's feet, jittered "
                            + "+/-1 in x/z only, got " + last);
            helper.assertTrue(probe.getAttackTime() >= TaintacleAiLogic.TENTACLE_COOLDOWN_BASE
                            && probe.getAttackTime() < TaintacleAiLogic.TENTACLE_COOLDOWN_BASE
                                    + TaintacleAiLogic.TENTACLE_COOLDOWN_JITTER,
                    "EntityTaintacle.java:220 — the cooldown is 40 + rand(20) on the shared swing "
                            + "counter, got " + probe.getAttackTime());

            // A taintlet never bursts (:160, :241) — isSmall switches the branch off.
            final StubTaint stub2 = new StubTaint(true, Blocks.SOUL_SAND);
            stub2.taintEveryBlock = true;
            TaintHooks.install(stub2);
            final EntityTaintacleSmall tiny = new EntityTaintacleSmall(entityType("taintacletiny"), level);
            tiny.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
            tiny.hurt(farHit, 1.0F);
            helper.assertTrue(stub2.spawnCalls.get() == 0,
                    "EntityTaintacle.java:241 — a taintlet is exempt from the far-hit burst, got "
                            + stub2.spawnCalls.get() + " spawn calls");
        } finally {
            TaintHooks.install(previous);
            attacker.discard();
        }
        helper.succeed();
    }

    /**
     * The two death rules of a taintlet, told apart by the seam's biome answer:
     * off taint the taintacle family's starve ({@code EntityTaintacle.java:116-121},
     * 1 heart every 20 ticks) kills an 8-health taintlet at tick 160, long before
     * its own 200-tick lifetime ({@code EntityTaintacleSmall.java:26-31}) can
     * fire; in the taint biome the starve is off, so the taintlet survives to
     * tick 201 and kills itself with 10 magic damage.
     */
    @GameTest(template = "thaumcraft:gametest_pad_32", timeoutTicks = 600)
    public static void theTaintletStarvesOffTaintAndBurnsAtItsLifetimeOnTaint(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos floor = helper.absolutePos(new BlockPos(4, 5, 4));
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(), Blocks.STONE.defaultBlockState());
        final TaintHooks.Provider previous = TaintHooks.current();
        final StubTaint stub = new StubTaint(true, Blocks.SOUL_SAND);
        try {
            // Off taint (the branch's own world): starve kills it at tick 160.
            final EntityTaintacleSmall offTaint =
                    new EntityTaintacleSmall(entityType("taintacletiny"), level);
            offTaint.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(offTaint);
            for (int tick = 0; tick < 150; tick++) {
                offTaint.tick();
            }
            helper.assertTrue(offTaint.isAlive()
                            && Math.abs(offTaint.getHealth() - 1.0F) < 1.0E-4,
                    "EntityTaintacle.java:116-121 — off taint a taintlet loses 1 heart every 20 ticks; "
                            + "after 150 ticks (7 starves) an 8-health taintlet must stand at 1, got "
                            + offTaint.getHealth() + " (tickCount " + offTaint.tickCount + ")");
            for (int tick = 0; tick < 15; tick++) {
                offTaint.tick();
            }
            helper.assertTrue(!offTaint.isAlive(),
                    "the 8th starve (tick 160) is the taintlet's death in a taint-free world, health "
                            + offTaint.getHealth());
            offTaint.discard();

            // In the taint biome: no starve, so the lifetime burn is what kills it.
            TaintHooks.install(stub);
            final BlockPos otherFloor = helper.absolutePos(new BlockPos(20, 5, 20));
            level.setBlockAndUpdate(otherFloor, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(otherFloor.above(), Blocks.STONE.defaultBlockState());
            final EntityTaintacleSmall onTaint =
                    new EntityTaintacleSmall(entityType("taintacletiny"), level);
            onTaint.moveTo(otherFloor.getX() + 0.5, otherFloor.getY() + 1.0, otherFloor.getZ() + 0.5,
                    0.0F, 0.0F);
            level.addFreshEntity(onTaint);
            for (int tick = 0; tick < 200; tick++) {
                onTaint.tick();
            }
            helper.assertTrue(onTaint.isAlive() && Math.abs(onTaint.getHealth() - 8.0F) < 1.0E-4,
                    "EntityTaintacle.java:116-121 — in the taint biome there is NO starve: 200 ticks "
                            + "must leave the taintlet at full 8 health, got " + onTaint.getHealth());
            helper.assertTrue(onTaint.getLifetime() == 0,
                    "EntityTaintacleSmall.java:10 — the lifetime counter has run out at tick 200, got "
                            + onTaint.getLifetime());
            for (int tick = 0; tick < 5; tick++) {
                onTaint.tick();
            }
            helper.assertTrue(!onTaint.isAlive(),
                    "EntityTaintacleSmall.java:26-31 — at lifetime-- <= 0 the taintlet takes 10 magic "
                            + "damage, which kills an 8-health taintlet; health " + onTaint.getHealth());
            onTaint.discard();
        } finally {
            TaintHooks.install(previous);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ giant

    /**
     * {@code EntityTaintacleGiant.java}: the giant never spawns naturally
     * (:80-84), caps a blow to 35 while it enrages (:110-126), enrages once
     * because the enrage needs anger 0 (:111), ticks the anger down 1 a tick
     * (:43-45), and narrows its anger watcher to a Short (:78).
     */
    @GameTest(template = "thaumcraft:gametest_pad_32", timeoutTicks = 400)
    public static void giantCapsTheBlowAtThirtyFiveAndEnragesExactlyOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos floor = helper.absolutePos(new BlockPos(22, 5, 6));
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(), Blocks.STONE.defaultBlockState());
        final EntityTaintacleGiant giant =
                new EntityTaintacleGiant(entityType("taintaclegiant"), level);
        giant.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(giant);
        try {
            helper.assertTrue(giant.getAnger() == 0, "EntityTaintacleGiant.java:70 — anger starts at 0");
            helper.assertTrue(!giant.checkSpawnRules(level, MobSpawnType.COMMAND),
                    "EntityTaintacleGiant.java:80-84 — getCanSpawnHere is false: the giant never "
                            + "spawns naturally, whatever the taint seam says");

            giant.hurt(level.damageSources().generic(), 100.0F);
            helper.assertTrue(giant.getAnger() == TaintacleGiantAiLogic.ANGER_ON_ENRAGE,
                    "EntityTaintacleGiant.java:116 — the enrage sets the anger to 200, got "
                            + giant.getAnger());
            helper.assertTrue(Math.abs(giant.getHealth()
                            - (TaintacleGiantAiLogic.HEALTH - TaintacleGiantAiLogic.ENRAGE_DAMAGE_CAP)) < 1.0E-4,
                    "EntityTaintacleGiant.java:126 caps the blow to 35, so 125 health must land on 90, got "
                            + giant.getHealth());

            giant.hurt(level.damageSources().generic(), 100.0F);
            helper.assertTrue(giant.getAnger() == TaintacleGiantAiLogic.ANGER_ON_ENRAGE,
                    "EntityTaintacleGiant.java:111 — the enrage needs anger == 0, so anger cannot re-arm");

            giant.tick();
            helper.assertTrue(giant.getAnger() == TaintacleGiantAiLogic.ANGER_ON_ENRAGE - 1,
                    "EntityTaintacleGiant.java:43-45 — the anger counter ticks down by 1 a tick, got "
                            + giant.getAnger());

            giant.setAnger(70000);
            helper.assertTrue(giant.getAnger() == 4464,
                    "EntityTaintacleGiant.java:78 narrows the setter to a Short: 70000 -> 4464, got "
                            + giant.getAnger());
        } finally {
            giant.discard();
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- helpers

    /**
     * The live registry is the id contract, so the probes resolve ids there.
     * The return is generic so a probe can hand the type straight to a ctor
     * ({@code new EntityTaintacle(entityType("taintacle"), level)}): the registry
     * hands out a wildcard, the ctor wants {@code EntityType<? extends X>}, and
     * the probe's id string is the thing that pins the class.
     */
    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entityType(final String path) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
        if (type == null || BuiltInRegistries.ENTITY_TYPE.getKey(type) == null
                || !ThaumcraftClassic.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
            throw new AssertionError("thaumcraft:" + path
                    + " is not in the live entity-type registry (CatalogTaintacles did not register it)");
        }
        return (EntityType<T>) type;
    }

    /** The family members inside the taintacle proximity box (the original scans the class). */
    private static int countFamily(final ServerLevel level, final EntityTaintacle probe) {
        return level.getEntitiesOfClass(EntityTaintacle.class,
                TaintacleAiLogic.spawnScanBox(probe.getX(), probe.getY(), probe.getZ())).size();
    }

    /**
     * A taint engine in a hat: the floor is taint when it is the marker block
     * (soul sand — no taint block exists on this branch to place), the biome
     * answer is a switch, every block answers as taint when
     * {@link #taintEveryBlock} is set, and the spawn/convert writes are recorded
     * instead of executed.
     */
    private static final class StubTaint implements TaintHooks.Provider {

        boolean inTaintBiome;
        boolean taintEveryBlock;
        private final Block taintFloorMarker;

        final AtomicInteger groundChecks = new AtomicInteger();
        final AtomicInteger biomeChecks = new AtomicInteger();
        final AtomicInteger spawnCalls = new AtomicInteger();
        final AtomicReference<ResourceLocation> lastSpawnId = new AtomicReference<>();
        final AtomicReference<Vec3> lastSpawnAt = new AtomicReference<>();

        StubTaint(final boolean inTaintBiome, final Block taintFloorMarker) {
            this.inTaintBiome = inTaintBiome;
            this.taintFloorMarker = taintFloorMarker;
        }

        @Override
        public boolean isTaintBlock(final BlockState state) {
            return this.taintEveryBlock || state.is(this.taintFloorMarker);
        }

        @Override
        public boolean isTaintGroundMeta(final BlockState state) {
            this.groundChecks.incrementAndGet();
            return state.is(this.taintFloorMarker);
        }

        @Override
        public boolean isTaintBiome(final net.minecraft.world.level.LevelAccessor level, final BlockPos pos) {
            this.biomeChecks.incrementAndGet();
            return this.inTaintBiome;
        }

        @Override
        public void spawnGuarded(final ServerLevel level, final ResourceLocation entityId, final Vec3 at) {
            this.spawnCalls.incrementAndGet();
            this.lastSpawnId.set(entityId);
            this.lastSpawnAt.set(at);
        }

        @Override
        public void convertToTaintFibres(final ServerLevel level, final BlockPos at, final int fibresMeta) {
            // Recorded as a spawn-free no-op: the eldritch branch is unreachable on
            // this branch (EntityTaintacle.NO_ELDRITCH_BIOME_CONTRACT).
        }

        @Override
        public String toString() {
            return "TaintacleGameTest.StubTaint(biome=" + this.inTaintBiome
                    + ", taintEveryBlock=" + this.taintEveryBlock + ")";
        }
    }
}
