package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacle;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintacleSmall;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintacleAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.EntityTaintacleGiant;
import com.jonnyutah12.thaumcraftclassic.entities.monster.boss.TaintacleGiantAiLogic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaintacles;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
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
 * <p>One gate arm is deliberately NOT asserted: with the biome taint and fibres
 * meta 0 under the mob, {@code EntityTaintacle.checkSpawnRules} still refuses, so
 * the taint-true hand-off to the vanilla half has no green probe on this card —
 * it is a product defect (the family would never pass its own gate) and is
 * filed as a follow-up. The pad's vanilla half is asserted OPEN, which is what
 * localises that refusal to the mob's own taint layer.
 *
 * <p>The taint half of the AI is proven against the REAL taint engine this
 * branch ships (TC-M5-04-s): {@code theTaintacleSpawnGateReadsTheRealTaintWorld}
 * places fibres meta 0 / taint meta 0 / taint meta 1 under the mob and writes
 * the taint biome on the column with the engine's own writers
 * ({@code TaintManager.setFibres}/{@code setTaint},
 * {@code TaintBiomes.setBiomeAtAndSync}), and watches the gate flip with them;
 * {@code theFarHitBurstsTaintletsAtTheAttackerOnTaint} puts real fibres under
 * the attacker and counts the {@code thaumcraft:taintacletiny} the burst
 * spawns. Every probe restores the world it touched in a {@code finally}
 * ({@code TaintManager.restoreBiome} plus the block back to air), and the
 * ambient-count discipline (deltas over a baseline, not absolutes) is the batch
 * rule: the server packs the structures of a batch side by side, so an absolute
 * scan of the taintacle's 24×8×24 proximity box reads a neighbour probe's
 * mobs.</p>
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
        // The expected pair is the ORIGINAL's literal, not the port's constant: an
        // expectation read from the code under test is a tautology and moves with
        // every mutation, so it measures nothing (measured: widening the builder
        // box 0.66 -> 0.75 left this probe green).
        assertBox(helper, "taintacle", 0.66, 3.0);        // EntityTaintacle.java:30 func_70105_a(0.66F, 3.0F)
        assertBox(helper, "taintacletiny", 0.22, 1.0);    // EntityTaintacleSmall.java:14 func_70105_a(0.22F, 1.0F)
        assertBox(helper, "taintaclegiant", 1.1, 6.0);    // EntityTaintacleGiant.java:24 func_70105_a(1.1F, 6.0F)
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
                0.66, 3.0);                                 // EntityTaintacle.java:30
        assertInstanceBox(helper, "taintacletiny",
                new EntityTaintacleSmall(entityType("taintacletiny"), level),
                0.22, 1.0);                                 // EntityTaintacleSmall.java:14
        assertInstanceBox(helper, "taintaclegiant",
                new EntityTaintacleGiant(entityType("taintaclegiant"), level),
                1.1, 6.0);                                  // EntityTaintacleGiant.java:24
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
        helper.assertTrue(Math.abs(mob.getDimensions(pose).eyeHeight() - 0.25) < 1.0E-4,
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
        helper.assertTrue(taintacle.getAttributeValue(Attributes.MAX_HEALTH) == 50.0
                        && taintacle.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7.0,
                "EntityTaintacle.java:60-61 is 50 health / 7 attack, got "
                        + taintacle.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + taintacle.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(taintacle.xpReward() == 10,
                "EntityTaintacle.java:31 sets field_70728_aV to 10, got " + taintacle.xpReward());
        helper.assertTrue(Math.abs(taintacle.getSoundVolume()) > 0.0F
                        && Math.abs(taintacle.getSoundVolume() - 6.25F) < 1.0E-4,
                "EntityTaintacle.java:287-289 — volume is maxHealth/8 = 6.25, got "
                        + taintacle.getSoundVolume());
        helper.assertTrue(Math.abs(taintacle.getVoicePitch() + 3.7F) < 1.0E-4,
                "EntityTaintacle.java:283-285 — pitch is 1.3 - maxHealth/10 = -3.7 for a 50-heart "
                        + "taintacle; the negative pitch is the original's quirk, got "
                        + taintacle.getVoicePitch());

        final EntityTaintacleSmall tiny = new EntityTaintacleSmall(entityType("taintacletiny"), level);
        helper.assertTrue(tiny.getAttributeValue(Attributes.MAX_HEALTH) == 8.0
                        && tiny.getAttributeValue(Attributes.ATTACK_DAMAGE) == 2.0,
                "EntityTaintacleSmall.java:21-22 is 8 health / 2 attack, got "
                        + tiny.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + tiny.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(tiny.xpReward() == 0,
                "EntityTaintacleSmall.java:15 — a taintlet is worth no experience, got " + tiny.xpReward());
        helper.assertTrue(tiny.getLifetime() == 200,
                "EntityTaintacleSmall.java:10 — the lifetime counter starts at 200, got "
                        + tiny.getLifetime());

        final EntityTaintacleGiant giant = new EntityTaintacleGiant(entityType("taintaclegiant"), level);
        helper.assertTrue(giant.getAttributeValue(Attributes.MAX_HEALTH) == 125.0
                        && giant.getAttributeValue(Attributes.ATTACK_DAMAGE) == 9.0,
                "EntityTaintacleGiant.java:31-32 is 125 health / 9 attack, got "
                        + giant.getAttributeValue(Attributes.MAX_HEALTH) + " / "
                        + giant.getAttributeValue(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(giant.xpReward() == 20,
                "EntityTaintacleGiant.java:25 sets field_70728_aV to 20, got " + giant.xpReward());
        helper.assertTrue(giant.getBossEvent() != null, "the boss bar exists (IBossDisplayData seam)");
        helper.succeed();
    }

    // ------------------------------------------------------------------ taint

        /**
 * The spawn gate ({@code EntityTaintacle.java:34-51}) reads the REAL taint world
 * this branch ships: stone underfoot in a non-taint biome refuses, fibres meta 0
 * underfoot in a NON-taint biome refuses (:39), fibres meta 0 plus the taint
 * biome hands the decision to the vanilla mob gate, the meta discipline is
 * exactly fibres-0 / taint-1 (fibres grass-1 and taint crust-0 refuse), and a
 * crowded 24x8x24 box refuses even with both taint answers true. Every arm runs
 * inside a sealed stone chamber against a same-category control, so the vanilla
 * darkness layer the product delegates to is OPEN and each verdict measures the
 * taint layer; the hand-off arm asserts the mob answers exactly what the control
 * answers once every taint leg is true.
 */
    @GameTest(template = "gametest_pad_32", timeoutTicks = 400)
    public static void theTaintacleSpawnGateReadsTheRealTaintWorld(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos floor = helper.absolutePos(new BlockPos(6, 4, 6));
        // EntityTaintacle.standingOnTaint reads the block AT blockPosition() — the
        // cell the mob occupies (:46-50 floors the mob's own feet cell).
        final BlockPos cell = floor.above();
        // A SEALED stone chamber, so the mob's own vanilla layer is OPEN on the
        // cell: 1.21.1 Monster#checkSpawnRules is the darkness gate and it is the
        // layer EntityTaintacle.java:210 delegates to. On an open sunlit pad that
        // layer refuses by itself, which makes every refusal arm pass for the
        // wrong reason — and it is exactly what the first version of this probe
        // read as "the taint hand-off is a product-side defect", the arm it then
        // declined to assert. Sealed walls put the light gate on the OPEN side, so
        // every verdict below can only be the taint layer, and the hand-off arm is
        // assertable. 1.21.1 computes sky light on a queue, so the chamber needs
        // ticks to go dark: the legs run on a delay, not in the build tick
        // (measured: raw brightness 15 at the cell in the build tick, and
        // draining the engine in-line trips the GameTest thread guard).
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(floor.offset(x, 6, z), Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 5; y++) {
                    if (x == -2 || x == 2 || z == -2 || z == 2) {
                        level.setBlockAndUpdate(floor.offset(x, y, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        level.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());
        // The legs run on a delay (the chamber needs ticks to go dark), so the
        // probe succeeds at the END of the delayed body, not here.
        helper.runAfterDelay(20, () -> runSpawnGateArms(helper, level, floor, cell));
    }

    /** The gate arms, driven once the chamber has gone dark. */
    private static void runSpawnGateArms(final GameTestHelper helper, final ServerLevel level,
            final BlockPos floor, final BlockPos cell) {
        final EntityTaintacle probe = new EntityTaintacle(entityType("taintacle"), level);
        probe.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);

        // The vanilla half, measured on a monster of the same category that
        // overrides nothing, on THIS cell — the layer :210 delegates to. Every
        // refusal arm asserts against it, so a refusal can only be the taint layer.
        final Zombie control = new Zombie(level);
        control.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
        final boolean vanillaHalf = control.checkSpawnRules(level, MobSpawnType.COMMAND);
        helper.assertTrue(vanillaHalf,
                "the sealed chamber must open the mob category's own vanilla layer (1.21.1 "
                        + "Monster#checkSpawnRules is the darkness gate, and it is the layer "
                        + "EntityTaintacle.java:210 delegates to), or the arms below measure the "
                        + "light gate: raw brightness at " + cell + " is "
                        + level.getRawBrightness(cell, 0));

        helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                "EntityTaintacle.java:46-50 — plain stone underfoot refuses while the vanilla "
                        + "half is OPEN, so the refusal is the taint layer");
        helper.assertTrue(!TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, cell)),
                "the pad starts outside the taint biome, got "
                        + TaintBiomes.biomeAt(level, cell).getRegisteredName());

        try {
            // :39 — the floor alone is not enough: fibres in a NON-taint biome refuse.
            TaintManager.setFibres(level, cell, BlockTaintFibres.META_FIBRES);
            helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                    "EntityTaintacle.java:39 — fibres meta 0 underfoot in a non-taint biome refuses "
                            + "while the vanilla half is OPEN");

            // :39 + :46-50 all true => the taint layer stops vetoing and the verdict
            // IS the vanilla half. This is the hand-off arm.
            TaintBiomes.setBiomeAtAndSync(level, floor.getX(), floor.getZ(),
                    TaintBiomes.taintBiomeHolder(level));
            helper.assertTrue(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, cell)),
                    "the biome write must land — biomeAt reads "
                            + TaintBiomes.biomeAt(level, cell).getRegisteredName());
            helper.assertTrue(probe.checkSpawnRules(level, MobSpawnType.COMMAND) == vanillaHalf,
                    "EntityTaintacle.java:51 — with the biome taint, fibres meta 0 underfoot and no "
                            + "neighbour the taint layer must stop vetoing and hand the decision to "
                            + "the vanilla half (vanilla " + vanillaHalf + " on " + cell
                            + ", mob " + probe.checkSpawnRules(level, MobSpawnType.COMMAND) + ")");

            // :46-48 — the meta discipline: only fibres meta 0 is a legal floor.
            TaintManager.setFibres(level, cell, BlockTaintFibres.META_GRASS_1);
            helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                    "EntityTaintacle.java:46-48 — only fibres meta 0 counts, grass-1 refuses with "
                            + "the biome taint on the column and the vanilla half OPEN");

            // :49-50 — taint meta 1 (Tainted Soil) is the other legal floor.
            TaintManager.setTaint(level, cell, BlockTaint.META_SOIL);
            helper.assertTrue(TaintManager.isTaintBlock(level.getBlockState(cell))
                            && probe.checkSpawnRules(level, MobSpawnType.COMMAND) == vanillaHalf,
                    "EntityTaintacle.java:49-50 — taint meta 1 (Tainted Soil) is the other legal "
                            + "floor: the engine reads the cell as taint ground ("
                            + level.getBlockState(cell) + ") and the verdict falls to the vanilla "
                            + "half (" + vanillaHalf + ")");

            // :49-50 — taint meta 0 (Crusted Taint) is NOT a legal floor.
            TaintManager.setTaint(level, cell, BlockTaint.META_CRUST);
            helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                    "EntityTaintacle.java:49-50 — taint meta 0 (Crusted Taint) is NOT a legal floor");

            // :42-44 — one taintacle inside the 24/8/24 box refuses the spawn.
            TaintManager.setFibres(level, cell, BlockTaintFibres.META_FIBRES);
            final int baseline = countFamily(level, probe);
            final EntityTaintacle neighbour = new EntityTaintacle(entityType("taintacle"), level);
            neighbour.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(neighbour);
            try {
                helper.assertTrue(countFamily(level, probe) == baseline + 1,
                        "the proximity scan must see the fresh neighbour (box "
                                + TaintacleAiLogic.spawnScanBox(probe.getX(), probe.getY(), probe.getZ())
                                + "): the delta over the ambient baseline must be exactly 1");
                helper.assertTrue(!probe.checkSpawnRules(level, MobSpawnType.COMMAND),
                        "EntityTaintacle.java:44 requires ents.size() <= 0 — a crowded cell refuses "
                                + "with every other leg true and the vanilla half OPEN");
            } finally {
                neighbour.discard();
            }
        } finally {
            level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
            TaintManager.restoreBiome(level, floor.getX(), floor.getZ());
        }
        helper.succeed();
    }

        /**
 * {@code EntityTaintacle.java:240-249} — a hit from beyond 16 blocks bursts
 * taintlets AT THE ATTACKER, through the engine's guarded spawn
 * ({@code TaintManager.spawnGuarded}, :221-229). Off taint the gate (:213-219)
 * returns first and spawns nothing; on real taint (fibres under the attacker
 * plus the taint biome on that column) a {@code thaumcraft:taintacletiny}
 * appears at the attacker's feet, jittered +/-1 in x/z only, and the shared
 * swing counter takes the 40 + rand(20) cooldown (:220). A taintlet is exempt
 * from the burst (:160, :241).
 */
    @GameTest(template = "gametest_pad_32", timeoutTicks = 400)
    public static void theFarHitBurstsTaintletsAtTheAttackerOnTaint(final GameTestHelper helper) {
    final ServerLevel level = helper.getLevel();
    final BlockPos floor = helper.absolutePos(new BlockPos(6, 5, 6));
    final BlockPos cell = floor.above();
    level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
    level.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());

    // The attacker sits past the 16-block line (EntityTaintacle.java:243) and
    // is a plain animal, so no mock player leaks into the batch's mob scans.
    final Cow attacker = new Cow(net.minecraft.world.entity.EntityType.COW, level);
    attacker.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 30.5, 0.0F, 0.0F);
    level.addFreshEntity(attacker);
    final EntityTaintacle probe = new EntityTaintacle(entityType("taintacle"), level);
    probe.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
    final DamageSource farHit = new DamageSource(
            level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.GENERIC_KILL),
            attacker, attacker);
    final BlockPos at = BlockPos.containing(attacker.getX(), attacker.getY(), attacker.getZ());

    try {
        // Off taint: the gate (:213-219) returns before anything is spawned.
        probe.setAttackTime(0);
        probe.hurt(farHit, 1.0F);
        helper.assertTrue(probe.getAttackTime() == 0,
                "EntityTaintacle.java:213-219 — with no taint biome and no taint ground the burst "
                        + "returns at the gate, so the swing counter stays 0, got "
                        + probe.getAttackTime());
        helper.assertTrue(countTaintlets(level, attacker.position()) == 0,
                "the refused burst must spawn no taintlet at the attacker");
        helper.assertTrue(!attacker.isRemoved() && attacker.getHealth() == 10.0F,
                "the refused burst must not touch the attacker, health " + attacker.getHealth());

        // On taint: fibres on the attacker's cell plus the taint biome on that
        // column, so the gate opens and the guarded spawn runs.
        TaintBiomes.setBiomeAtAndSync(level, at.getX(), at.getZ(), TaintBiomes.taintBiomeHolder(level));
        TaintManager.setFibres(level, at, BlockTaintFibres.META_FIBRES);
        helper.assertTrue(TaintManager.isTaintBlock(level.getBlockState(at))
                        && TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, at)),
                "the taint the probe placed must be visible to the engine's own predicates");
        final int baseline = countTaintlets(level, attacker.position());
        probe.hurt(farHit, 1.0F);
        final java.util.List<EntityTaintacleSmall> spawned = taintlets(level, attacker.position());
        helper.assertTrue(spawned.size() == baseline + 1,
                "EntityTaintacle.java:221-229 — the burst hands exactly one taintlet to the "
                        + "engine's guarded spawn, got " + (spawned.size() - baseline));
        final EntityTaintacleSmall let = spawned.get(spawned.size() - 1);
        helper.assertTrue(Math.abs(let.getX() - attacker.getX()) <= 1.0
                        && Math.abs(let.getZ() - attacker.getZ()) <= 1.0
                        && Math.abs(let.getY() - attacker.getY()) < 1.0,
                "EntityTaintacle.java:221-229 — the taintlet lands on the ATTACKER's feet, jittered "
                        + "+/-1 in x/z only, got " + let.position() + " vs attacker "
                        + attacker.position());
        helper.assertTrue(let.getMaxHealth() == 8.0 && let.xpReward() == 0,
                "the spawned mob is thaumcraft:taintacletiny — 8 health / 0 xp, got "
                        + let.getMaxHealth() + " / " + let.xpReward());
        helper.assertTrue(probe.getAttackTime() >= 40 && probe.getAttackTime() < 60,
                "EntityTaintacle.java:220 — the cooldown is 40 + rand(20) on the shared swing "
                        + "counter, got " + probe.getAttackTime());

        // A taintlet never bursts (:160, :241) — isSmall switches the branch off.
        final int before = countTaintlets(level, attacker.position());
        final EntityTaintacleSmall tiny = new EntityTaintacleSmall(entityType("taintacletiny"), level);
        tiny.moveTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0.0F, 0.0F);
        tiny.hurt(farHit, 1.0F);
        helper.assertTrue(countTaintlets(level, attacker.position()) == before,
                "EntityTaintacle.java:241 — a taintlet is exempt from the far-hit burst, got "
                        + (countTaintlets(level, attacker.position()) - before) + " extra taintlets");
        helper.assertTrue(tiny.getAttackTime() == 0,
                "EntityTaintacle.java:241 — the exempt taintlet takes no burst cooldown, got "
                        + tiny.getAttackTime());
    } finally {
        attacker.discard();
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        TaintManager.restoreBiome(level, at.getX(), at.getZ());
    }
    helper.succeed();
    }

            /**
     * The two death rules of a taintlet, told apart by the biome under it. Off
     * taint the taintacle family's starve ({@code EntityTaintacle.java:116-121})
     * fires and an 8-health taintlet is on its way to death; in the real taint
     * biome ({@code TaintBiomes.setBiomeAtAndSync}) the starve is OFF, so the
     * taintlet is untouched at 190 hand-fed ticks and its own 200-tick lifetime
     * ({@code EntityTaintacleSmall.java:26-31}) is the only thing that can kill
     * it, at {@code lifetime-- <= 0} with 10 magic damage.
     *
     * <p>The taintlet is hand-fed on purpose: 1.21.1 advances the entity's age
     * inside the level's own tick, so the exact 20-tick starve cadence
     * ({@code age % 20}) is proven as a pure function
     * ({@code TaintacleAiLogicTest.starveCadence});
     * what needs the mob class here is that the biome answer selects the starve
     * at all, which is a per-tick predicate on the live mob.</p>
     */
    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void theTaintletStarvesOffTaintAndBurnsAtItsLifetimeOnTaint(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos offFloor = helper.absolutePos(new BlockPos(4, 5, 4));
        final BlockPos onFloor = helper.absolutePos(new BlockPos(20, 5, 20));
        for (final BlockPos floor : new BlockPos[] {offFloor, onFloor}) {
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(floor.above(), Blocks.STONE.defaultBlockState());
        }
        try {
            helper.assertTrue(!TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, offFloor)),
                    "the starve arm needs a non-taint biome column, got "
                            + TaintBiomes.biomeAt(level, offFloor).getRegisteredName());
            final EntityTaintacleSmall offTaint = spawnTaintlet(level, offFloor);
            for (int tick = 0; tick < 8; tick++) {
                offTaint.tick();
            }
            helper.assertTrue(offTaint.getHealth() < 8.0,
                    "EntityTaintacle.java:116-121 — a taintlet in a taint-free world is starving: "
                            + "after 8 ticks its 8 health must be moving down, got "
                            + offTaint.getHealth());
            // Vanilla throttles repeated damage by invulnerableTime, so a 1-heart
            // starve lands about every 10 ticks; 8 fires is this mob's death.
            for (int tick = 0; tick < 120; tick++) {
                offTaint.tick();
            }
            helper.assertTrue(!offTaint.isAlive(),
                    "the starve of a taint-free world is a death sentence for an 8-health taintlet "
                            + "(1 heart per fire, throttled by invulnerableTime), health "
                            + offTaint.getHealth());
            offTaint.discard();

            // The shipped starve gate (EntityTaintacle.java:116-121) reads the taint
            // GROUND under the mob, so the starve-off arm needs real fibres meta 0
            // under its feet, with the taint biome on the column.
            TaintBiomes.setBiomeAtAndSync(level, onFloor.getX(), onFloor.getZ(),
                    TaintBiomes.taintBiomeHolder(level));
            TaintManager.setFibres(level, onFloor, BlockTaintFibres.META_FIBRES);
            helper.assertTrue(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, onFloor))
                            && TaintManager.isTaintBlock(level.getBlockState(onFloor)),
                    "the starve-off premise must land: taint biome on the column and taint ground "
                            + "under the feet, got biome "
                            + TaintBiomes.biomeAt(level, onFloor).getRegisteredName() + " / "
                            + level.getBlockState(onFloor));
            final EntityTaintacleSmall onTaint = spawnTaintlet(level, onFloor);
            for (int tick = 0; tick < 190; tick++) {
                onTaint.tick();
            }
            helper.assertTrue(onTaint.isAlive() && onTaint.getHealth() > 6.0F,
                    "EntityTaintacle.java:116-121 — on taint (fibres meta 0 under the feet plus the "
                            + "taint biome on the column) the starve gate is closed: 190 ticks leave "
                            + "this 8-health taintlet alive above 6 health while its taint-free twin "
                            + "is dead inside 48 ticks, got health " + onTaint.getHealth()
                            + " (lifetime " + onTaint.getLifetime() + ")");
            for (int tick = 0; tick < 15; tick++) {
                onTaint.tick();
            }
            helper.assertTrue(!onTaint.isAlive(),
                    "EntityTaintacleSmall.java:26-31 — at lifetime-- <= 0 the taintlet takes 10 magic "
                            + "damage, which kills an 8-health taintlet; health "
                            + onTaint.getHealth() + " at lifetime " + onTaint.getLifetime());
            helper.assertTrue(onTaint.getLifetime() <= 0,
                    "EntityTaintacleSmall.java:28 — the lifetime counter has run out, got "
                            + onTaint.getLifetime());
            onTaint.discard();
        } finally {
            level.setBlockAndUpdate(onFloor, Blocks.AIR.defaultBlockState());
            TaintManager.restoreBiome(level, onFloor.getX(), onFloor.getZ());
        }
        helper.succeed();
    }


    private static int otherColumnX(final GameTestHelper helper) {
    return helper.absolutePos(new BlockPos(20, 5, 20)).getX();
    }

    private static int otherColumnZ(final GameTestHelper helper) {
    return helper.absolutePos(new BlockPos(20, 5, 20)).getZ();
    }

    /** A taintlet dropped onto a pad floor cell, for the starve/lifetime probe. */
    private static EntityTaintacleSmall spawnTaintlet(final ServerLevel level, final BlockPos floor) {
        final EntityTaintacleSmall tiny = new EntityTaintacleSmall(entityType("taintacletiny"), level);
        tiny.setPos(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5);
        level.addFreshEntity(tiny);
        return tiny;
    }

    /** The taintlets within 2 blocks of {@code at} (the burst's spawn box). */
    private static java.util.List<EntityTaintacleSmall> taintlets(final ServerLevel level, final Vec3 at) {
    return level.getEntitiesOfClass(EntityTaintacleSmall.class,
            new net.minecraft.world.phys.AABB(at.x - 2.0, at.y - 2.0, at.z - 2.0,
                    at.x + 2.0, at.y + 2.0, at.z + 2.0));
    }

    private static int countTaintlets(final ServerLevel level, final Vec3 at) {
    return taintlets(level, at).size();
    }

    // ------------------------------------------------------------------ giant

    /**
     * {@code EntityTaintacleGiant.java}: the giant never spawns naturally
     * (:80-84), caps a blow to 35 while it enrages (:110-126), enrages once
     * because the enrage needs anger 0 (:111), ticks the anger down 1 a tick
     * (:43-45), and narrows its anger watcher to a Short (:78).
     */
    @GameTest(template = "gametest_pad_32", timeoutTicks = 400)
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
            helper.assertTrue(giant.getAnger() == 200,
                    "EntityTaintacleGiant.java:116 — the enrage sets the anger to 200, got "
                            + giant.getAnger());
            helper.assertTrue(Math.abs(giant.getHealth() - 90.0) < 1.0E-4,
                    "EntityTaintacleGiant.java:126 caps the blow to 35, so 125 health must land on 90, got "
                            + giant.getHealth());

            giant.hurt(level.damageSources().generic(), 100.0F);
            helper.assertTrue(giant.getAnger() == 200,
                    "EntityTaintacleGiant.java:111 — the enrage needs anger == 0, so anger cannot re-arm");

            giant.tick();
            helper.assertTrue(giant.getAnger() == 199,
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
}
