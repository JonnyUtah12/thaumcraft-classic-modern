package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintChicken;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintCow;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintCreeper;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSheep;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintSpider;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintVillager;
import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityTaintedAnimalBase;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.Kind;
import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintCreeperAiLogic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaintMobs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for the taint mob family (TC-M6-06-s-b). These prove the
 * original classes are not merely compilable: every entity type is in the
 * live registry with the original's hitbox, an instance spawns alive with the
 * original's attributes and its decompiled goal table, and the preserved
 * quirks (the creeper's 1.5F blast, the falling taint's piston gate) hold in a
 * real level. The pure rules behind them are pinned world-free by
 * {@code TaintedAnimalAiLogicTest}, {@code TaintCreeperAiLogicTest} and
 * {@code FallingTaintAiLogicTest}.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaintMobGameTest {

    private TaintMobGameTest() {
    }

    /** The goal rows with no 1.21.1 goal class are skipped by the factory. */
    private static int installedGoals(final List<GoalSpec> table) {
        return (int) table.stream()
                .filter(spec -> spec.kind() != Kind.MOVE_INDOORS
                        && spec.kind() != Kind.RESTRICT_OPEN_DOOR)
                .count();
    }

    @GameTest(template = "empty")
    public static void taintMobFamilyIsRegisteredWithTheOriginalBoxes(final GameTestHelper helper) {
        assertBox(helper, "taintedchicken", TaintedAnimalAiLogic.CHICKEN.width(),
                TaintedAnimalAiLogic.CHICKEN.height());
        assertBox(helper, "taintedcow", TaintedAnimalAiLogic.COW.width(),
                TaintedAnimalAiLogic.COW.height());
        assertBox(helper, "taintedpig", TaintedAnimalAiLogic.PIG.width(),
                TaintedAnimalAiLogic.PIG.height());
        assertBox(helper, "taintedsheep", TaintedAnimalAiLogic.SHEEP.width(),
                TaintedAnimalAiLogic.SHEEP.height());
        assertBox(helper, "taintedcreeper", TaintedAnimalAiLogic.CREEPER.width(),
                TaintedAnimalAiLogic.CREEPER.height());
        assertBox(helper, "taintedvillager", TaintedAnimalAiLogic.VILLAGER.width(),
                TaintedAnimalAiLogic.VILLAGER.height());
        // EntityTaintSpider ctor line 17 / func_70053_R line 41-43
        assertBox(helper, "taintspider", 0.4D, 0.3D);
        // EntityFallingTaint ctor line 38
        assertBox(helper, "fallingtaint", 0.98D, 0.98D);
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
    public static void taintAnimalsCarryTheOriginalAttributes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityTaintChicken chicken = new EntityTaintChicken(
                CatalogTaintMobs.TAINTED_CHICKEN.get(), level);
        assertAttributes(helper, "taintedchicken", chicken, TaintedAnimalAiLogic.CHICKEN);

        final EntityTaintCow cow = new EntityTaintCow(CatalogTaintMobs.TAINTED_COW.get(), level);
        assertAttributes(helper, "taintedcow", cow, TaintedAnimalAiLogic.COW);

        final EntityTaintCow.Pig pig = new EntityTaintCow.Pig(
                CatalogTaintMobs.TAINTED_PIG.get(), level);
        assertAttributes(helper, "taintedpig", pig, TaintedAnimalAiLogic.PIG);

        final EntityTaintSheep sheep = new EntityTaintSheep(
                CatalogTaintMobs.TAINTED_SHEEP.get(), level);
        assertAttributes(helper, "taintedsheep", sheep, TaintedAnimalAiLogic.SHEEP);

        final EntityTaintCreeper creeper = new EntityTaintCreeper(
                CatalogTaintMobs.TAINTED_CREEPER.get(), level);
        assertAttributes(helper, "taintedcreeper", creeper, TaintedAnimalAiLogic.CREEPER);

        final EntityTaintVillager villager = new EntityTaintVillager(
                CatalogTaintMobs.TAINTED_VILLAGER.get(), level);
        assertAttributes(helper, "taintedvillager", villager, TaintedAnimalAiLogic.VILLAGER);

        // EntityTaintSpider func_110147_ax lines 21-25
        final EntityTaintSpider spider = new EntityTaintSpider(
                CatalogTaintMobs.TAINT_SPIDER.get(), level);
        helper.assertTrue(spider.getAttributeValue(Attributes.MAX_HEALTH) == 5.0D,
                "taint spider health is not the original 5.0");
        helper.assertTrue(spider.getAttributeValue(Attributes.ATTACK_DAMAGE) == 2.0D,
                "taint spider attack is not the original 2.0");
        helper.assertTrue(spider.taintSpiderXp() == 2,
                "the taint spider's field_70728_aV is 2");
        helper.succeed();
    }

    private static void assertAttributes(final GameTestHelper helper, final String path,
            final EntityTaintedAnimalBase mob, final TaintedAnimalAiLogic.Stats stats) {
        helper.assertTrue(mob.getAttributeValue(Attributes.MAX_HEALTH) == stats.health(),
                path + " health is not the original " + stats.health());
        helper.assertTrue(mob.getAttributeValue(Attributes.ATTACK_DAMAGE) == stats.attack(),
                path + " attack is not the original " + stats.attack());
        helper.assertTrue(mob.getAttributeValue(Attributes.ARMOR) == stats.armor(),
                path + " armor is not the original func_70658_aO value " + stats.armor());
        helper.assertTrue(mob.getVoicePitch() == 0.7F,
                path + " voice pitch is not the original func_70647_i 0.7");
    }

    @GameTest(template = "empty")
    public static void taintAnimalsInstallTheirDecompiledGoalTables(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        assertGoals(helper, "taintedchicken", new EntityTaintChicken(
                        CatalogTaintMobs.TAINTED_CHICKEN.get(), level),
                TaintedAnimalAiLogic.chickenGoals(), TaintedAnimalAiLogic.chickenTargetGoals());
        assertGoals(helper, "taintedcow", new EntityTaintCow(CatalogTaintMobs.TAINTED_COW.get(), level),
                TaintedAnimalAiLogic.cowGoals(), TaintedAnimalAiLogic.cowTargetGoals());
        assertGoals(helper, "taintedpig", new EntityTaintCow.Pig(
                        CatalogTaintMobs.TAINTED_PIG.get(), level),
                TaintedAnimalAiLogic.pigGoals(), TaintedAnimalAiLogic.pigTargetGoals());
        assertGoals(helper, "taintedsheep", new EntityTaintSheep(
                        CatalogTaintMobs.TAINTED_SHEEP.get(), level),
                TaintedAnimalAiLogic.sheepGoals(), TaintedAnimalAiLogic.sheepTargetGoals());
        assertGoals(helper, "taintedcreeper", new EntityTaintCreeper(
                        CatalogTaintMobs.TAINTED_CREEPER.get(), level),
                TaintedAnimalAiLogic.creeperGoals(), TaintedAnimalAiLogic.creeperTargetGoals());
        assertGoals(helper, "taintedvillager", new EntityTaintVillager(
                        CatalogTaintMobs.TAINTED_VILLAGER.get(), level),
                TaintedAnimalAiLogic.villagerGoals(), TaintedAnimalAiLogic.villagerTargetGoals());
        helper.succeed();
    }

    private static void assertGoals(final GameTestHelper helper, final String path,
            final EntityTaintedAnimalBase mob, final List<GoalSpec> goals,
            final List<GoalSpec> targets) {
        helper.assertTrue(mob.goalSelector.getAvailableGoals().size() == installedGoals(goals),
                path + " installed " + mob.goalSelector.getAvailableGoals().size()
                        + " goal rows, the decompiled table has " + installedGoals(goals)
                        + " buildable rows");
        helper.assertTrue(mob.targetSelector.getAvailableGoals().size() == targets.size(),
                path + " installed " + mob.targetSelector.getAvailableGoals().size()
                        + " target rows, the decompiled table has " + targets.size());
    }

    @GameTest(template = "empty")
    public static void spawnedTaintAnimalsAreAliveAndPushableLikeTheirOriginals(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityTaintChicken chicken = new EntityTaintChicken(
                CatalogTaintMobs.TAINTED_CHICKEN.get(), level);
        chicken.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        level.addFreshEntity(chicken);
        helper.assertTrue(chicken.isAlive(), "the spawned taint chicken is not alive");
        helper.assertTrue(chicken.getType() == CatalogTaintMobs.TAINTED_CHICKEN.get(),
                "the spawned chicken is not the registered entity type");
        helper.assertTrue(chicken instanceof com.jonnyutah12.thaumcraftclassic.api.entities
                        .ITaintedMob,
                "the taint chicken must implement ITaintedMob (the poison gate)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taintCreeperKeepsItsHandRolledFuseAndBlast(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final EntityTaintCreeper creeper = new EntityTaintCreeper(
                CatalogTaintMobs.TAINTED_CREEPER.get(), level);
        creeper.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        level.addFreshEntity(creeper);
        helper.assertTrue(creeper.getCreeperState() == -1,
                "the swell data byte starts at -1 (field_70713_aJ)");

        // The original accumulates the fuse AFTER the AI body has run
        // (func_70071_h_ calls super.tick first, lines 120-131), and
        // AICreeperSwell keeps the byte at 1 only while the target it CAPTURED
        // in start() is alive, visible and inside squared distance 49.0
        // (update 29-38); the moment it is not, the SAME tick writes -1 and the
        // fuse stops. The faithful probe therefore drives the byte THROUGH the
        // installed swell goal, on a target the probe owns.
        final EntityTaintChicken prey = new EntityTaintChicken(
                CatalogTaintMobs.TAINTED_CHICKEN.get(), level);
        prey.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        level.addFreshEntity(prey);

        // THE PRECONDITION THIS PROBE CLOSES: the engine's target rows come OFF
        // the live selector, so the probe owns the swell byte.
        //
        // Why the byte was not the probe's to own as written: the creeper's ctor
        // table (lines 48-49) installs EntityAINearestAttackableTarget(player,
        // mustSee) on the TARGET selector. 1.21.1's
        // NearestAttackableTargetGoal#canUse passes 1 in reducedTickDelay(10) = 5
        // (NearestAttackableTargetGoal.java:45-48), the scan is the mob's
        // FOLLOW_RANGE (Mob#createMobAttributes -> 16.0) and TargetGoal#start()
        // ends in mob.setTarget(scanned). That row is armed by the ENGINE, not the
        // probe: Mob#serverAiStep (Mob.java:779-794) runs the FULL target selector
        // on a fresh entity's first tick — the `(tickCount + id) % 2` branch needs
        // tickCount > 1, so entity-id parity never gates this probe (measured: 15
        // legs at id parity 0 and 9 at parity 1, every leg with both selectors
        // fully run, all green) — and on that 1-in-5 draw a hand-set target is
        // REPLACED by the nearest combat-valid Player inside 16 blocks. The
        // installed swell goal captures THAT player in start(), its update sees a
        // squared distance above the 49.0 cap — the packing grid puts a
        // neighbouring probe 6 to 7 blocks away, dist2 72+ — and writes -1 in the
        // SAME tick the fuse is read, so the fuse reads 0.
        //
        // Measured on the current tree (24 in-probe legs, -Ptc.noCurios, 318
        // required tests): state=1 and fuse=1 in 24/24, the goal captured the
        // probe's own prey in 24/24, and a 32-block scan found ZERO Players in
        // 24/24 — with no Player in the level the row cannot fire. 1.21.1's
        // GameTestHelper#makeMockPlayer is not one: it returns a bare Player at
        // BlockPos.ZERO that is never added to the level (measured: a mock player
        // carried to 12 blocks reads dist2=144.0 canAttack=true, the level scan
        // still counts 0 players, and the row's canUse() stays false across 60
        // draws). The red legs this card quotes are base-bound for exactly that
        // reason — the precondition is a Player in the level inside 16 blocks,
        // which the suite's packing and its neighbours decide, not the probe.
        //
        // So the probe drives the byte through the goal's own contract on a target
        // it owns, with the rows off the selector — asserted below. The goal's
        // distance/line-of-sight reset is asserted separately, off the selector.
        creeper.targetSelector.removeAllGoals(goal -> true);
        helper.assertTrue(creeper.targetSelector.getAvailableGoals().isEmpty(),
                "PRECONDITION (the closed flake): the probe owns the swell byte only "
                        + "with the engine's target rows OFF the live selector — a row "
                        + "left on it replaces the hand-set target on 1 draw in 5 and "
                        + "the swell goal then resets the byte for the distance of "
                        + "whatever it captured; "
                        + creeper.targetSelector.getAvailableGoals().size()
                        + " rows are armed");
        final List<net.minecraft.world.entity.ai.goal.Goal> installedSwell =
                creeper.goalSelector.getAvailableGoals().stream()
                        .map(wrapped -> wrapped.getGoal())
                        .filter(EntityTaintCreeper.SwellGoal.class::isInstance)
                        .toList();
        helper.assertTrue(installedSwell.size() == 1,
                "the factory installs the decompiled AICreeperSwell row (ctor line 42): "
                        + installedSwell.size() + " swell goals are on the live selector");

        creeper.setTarget(prey);
        creeper.setCreeperState(1);
        creeper.tick();
        helper.assertTrue(creeper.getCreeperState() == 1 && creeper.getTimeSinceIgnited() == 1,
                "the fuse accumulates by the swell state: one engine tick with the "
                        + "installed swell goal must hold the byte at 1 (target alive, "
                        + "squared distance " + fmt(creeper.distanceToSqr(prey))
                        + " against the 49.0 cap, line of sight "
                        + creeper.getSensing().hasLineOfSight(prey)
                        + ", target distance " + fmt(creeper.getTarget() == null ? -1.0
                                : creeper.distanceToSqr(creeper.getTarget()))
                        + " of that cap), got state="
                        + creeper.getCreeperState() + " fuse=" + creeper.getTimeSinceIgnited()
                        + " target=" + describeTarget(creeper)
                        + " selectors=" + describeSelectors(creeper));
        // nextFuse adds the byte, so the fuse steps 1/tick while the goal holds
        // the state (func_70071_h_ line 128, TaintCreeperAiLogic.nextFuse)
        creeper.tick();
        creeper.tick();
        helper.assertTrue(creeper.getCreeperState() == 1 && creeper.getTimeSinceIgnited() == 3,
                "the fuse steps by the swell byte every tick, got state="
                        + creeper.getCreeperState() + " fuse="
                        + creeper.getTimeSinceIgnited() + " target="
                        + describeTarget(creeper) + " selectors=" + describeSelectors(creeper));

        // AICreeperSwell.update (29-38) writes -1 the moment the target it
        // captured is gone, and the fuse then runs BACK DOWN, because the byte is
        // added, not clamped. Driven on the installed instance after taking it off
        // the selector, so no engine tick can re-arm it between the two draws.
        final EntityTaintCreeper.SwellGoal swell =
                (EntityTaintCreeper.SwellGoal) installedSwell.get(0);
        creeper.goalSelector.removeGoal(swell);
        swell.tick();
        helper.assertTrue(creeper.getCreeperState() == -1,
                "the swell goal resets the byte to -1 once its captured target is gone "
                        + "(AICreeperSwell.update 29-38), got " + creeper.getCreeperState());
        creeper.tick();
        helper.assertTrue(creeper.getTimeSinceIgnited() == 2,
                "the fuse runs back down by the -1 byte (nextFuse(3, -1, 30) = 2), got "
                        + creeper.getTimeSinceIgnited());
        // the blast is the hand-rolled 1.5F literal, never the loaded field
        helper.assertTrue(TaintCreeperAiLogic.EXPLOSION_RADIUS == 1.5F
                        && TaintCreeperAiLogic.LOADED_RADIUS_FIELD == 3.0F,
                "the taint creeper blast quirk drifted");
        // func_82143_as (lines 65-67): 3 without a target, 3 + (30-1) = 32
        // with one
        creeper.setTarget(null);
        helper.assertTrue(creeper.creeperExperiencePoints() == 3,
                "no-target experience is 3, got " + creeper.creeperExperiencePoints());
        creeper.setTarget(prey);
        helper.assertTrue(creeper.creeperExperiencePoints() == 32,
                "targeted experience is 32, got " + creeper.creeperExperiencePoints());
        helper.succeed();
    }

    /** The byte's driver, named in the failure message so a red leg reads from the log. */
    private static String describeTarget(final EntityTaintCreeper creeper) {
        final net.minecraft.world.entity.LivingEntity target = creeper.getTarget();
        return target == null ? "null"
                : target.getClass().getSimpleName() + "#" + target.getId();
    }

    private static String describeSelectors(final EntityTaintCreeper creeper) {
        final StringBuilder sb = new StringBuilder("goals=");
        for (final net.minecraft.world.entity.ai.goal.WrappedGoal wrapped
                : creeper.goalSelector.getAvailableGoals()) {
            sb.append(wrapped.getPriority()).append(':')
                    .append(wrapped.getGoal().getClass().getSimpleName())
                    .append(wrapped.isRunning() ? "R" : "-").append(' ');
        }
        sb.append("targets=").append(creeper.targetSelector.getAvailableGoals().size())
                .append(" rows");
        return sb.toString();
    }

    private static String fmt(final double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }


    @GameTest(template = "empty")
    public static void fallingTaintRefusesThePistonFamilyAndCarriesItsOriginalBox(
            final GameTestHelper helper) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(
                CatalogTaintMobs.FALLING_TAINT_ID);
        helper.assertTrue(type != null, "thaumcraft:fallingtaint is not registered");
        helper.assertTrue(type.getDimensions().width() == 0.98F
                        && type.getDimensions().height() == 0.98F,
                "the falling taint box is not the original 0.98 x 0.98");

        // EntityFallingTaint lines 109-111: the piston family refuses the landing
        helper.assertTrue(com.jonnyutah12.thaumcraftclassic.entities.monster.FallingTaintAiLogic
                        .isPistonFamily(Blocks.PISTON.defaultBlockState()),
                "a piston must refuse the falling-taint landing");
        helper.assertTrue(com.jonnyutah12.thaumcraftclassic.entities.monster.FallingTaintAiLogic
                        .isPistonFamily(Blocks.MOVING_PISTON.defaultBlockState()),
                "a moving piston must refuse the landing");
        helper.assertTrue(com.jonnyutah12.thaumcraftclassic.entities.monster.FallingTaintAiLogic
                        .isPistonFamily(Blocks.PISTON_HEAD.defaultBlockState()),
                "a piston head must refuse the landing");
        helper.assertTrue(!com.jonnyutah12.thaumcraftclassic.entities.monster.FallingTaintAiLogic
                        .isPistonFamily(Blocks.STONE.defaultBlockState()),
                "stone is not a piston");
        helper.succeed();
    }
}
