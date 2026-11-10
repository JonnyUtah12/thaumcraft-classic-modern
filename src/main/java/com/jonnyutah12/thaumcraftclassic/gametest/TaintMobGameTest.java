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
import net.minecraft.world.entity.ai.goal.WrappedGoal;
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

    /**
     * The fuse accumulates by the swell state, so the probe drives a real target — and takes
     * the swell goal OFF the live goal selector first, because a goal left on it is armed by
     * the ENGINE inside the probe's own {@code creeper.tick()}: {@code Entity#tick} →
     * {@code LivingEntity#tick} → {@code Mob#serverAiStep} → {@code goalSelector.tick()}, and
     * {@code GoalSelector#tick} (goalCleanup → goalUpdate starts eligible goals →
     * {@code tickRunningGoals(true)}) ticks the goal it has just started in the SAME call, so
     * {@code SwellGoal.tick()} writes the swell state the fuse is about to read. The
     * {@code (tickCount + id) % 2} gate at {@code Mob.java:779} shelters nothing here:
     * {@code tickCount} is advanced by the LEVEL ({@code ServerLevel#tickNonPassenger}), never
     * by {@code Entity#tick()}, so a hand-ticked probe entity sits at {@code tickCount=0}, the
     * {@code tickCount > 1} guard is false, and the FULL selector tick runs on every draw.
     *
     * Measured on 104ccce with a per-leg sampler at the asserted tick (56 legs, 26 with-Curios
     * / 30 -Ptc.noCurios): the axis that closed the fuse is the third input of the gate —
     * {@code getSensing().hasLineOfSight(attackTarget)} is a block clip, and the game-test grid
     * lands this probe's cell in generated terrain on some draws. sensingLos=true → 55 legs, 0
     * reds; sensingLos=false (clip=BLOCK cobblestone in the creeper's own eye cell) → 1 leg, 1
     * red, state=-1, fuse=0: "the fuse accumulates by the swell state, got 0". The parity
     * hypothesis this card inherited measured FALSE — 29 legs at parity 1 and 26 at parity 0,
     * 0 reds at parity 1, and the red leg sat at parity 0. Both arms draw terrain, which is why
     * the red showed in the with-Curios arm (1/26 here, the filer's 1/4) and in
     * -Ptc.noCurios (the reviewer's after_nc_3) alike.
     */
    @GameTest(template = "empty")
    public static void taintCreeperKeepsItsHandRolledFuseAndBlast(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // (1,2,1) feet + 1.53 eye height = the creeper's eye cell (1,3,1) — the cell the red
        // leg's clip hit: clip=BLOCK@6616291,-57,6353823 cobblestone, origin 6616290,-60,6353822
        final BlockPos eyeCell = helper.absolutePos(new BlockPos(1, 3, 1));
        final EntityTaintCreeper creeper = new EntityTaintCreeper(
                CatalogTaintMobs.TAINTED_CREEPER.get(), level);
        creeper.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        level.addFreshEntity(creeper);
        helper.assertTrue(creeper.getCreeperState() == -1,
                "the swell data byte starts at -1 (field_70713_aJ)");

        // MEASURE the swell row on the LIVE selector, then take it OFF the selector: left on
        // it, the engine arms it inside the probe's own tick (see the javadoc) and the goal's
        // update writes the state the fuse reads — on the draw, at a rate.
        final EntityTaintCreeper.SwellGoal swell = swellGoalOf(creeper);
        helper.assertTrue(swell != null,
                "the swell goal is on the live goal selector, rows:" + goalRows(creeper));
        helper.assertTrue(swellPriorityOf(creeper) == 2,
                "the swell goal is registered at priority 2 (the decompiled table), got "
                        + swellPriorityOf(creeper) + ", rows:" + goalRows(creeper));
        creeper.goalSelector.removeGoal(swell);
        // Goal has no isRunning() — the live flag is on WrappedGoal, so the rows string is the
        // witness: no swell row means no wrapper, and nothing can arm it.
        helper.assertTrue(swellGoalOf(creeper) == null,
                "the swell goal is OFF the live selector before the probe drives a tick (no row,"
                        + " so no wrapper to arm), rows:" + goalRows(creeper));

        // the original accumulates the fuse AFTER the goal selector has run
        // (func_70071_h_ calls super.tick first, lines 120-131), and
        // AICreeperSwell.update (29-38) keeps the state at 1 only while the
        // target is alive, visible and within squared distance 49.0 — with
        // no target the goal resets the state to -1 in the SAME tick, so the
        // faithful probe drives a real target
        final EntityTaintChicken prey = new EntityTaintChicken(
                CatalogTaintMobs.TAINTED_CHICKEN.get(), level);
        prey.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0.0F, 0.0F);
        level.addFreshEntity(prey);
        creeper.setTarget(prey);
        helper.assertTrue(creeper.getTarget() == prey && prey.isAlive()
                        && creeper.distanceToSqr(prey) < 49.0D,
                "the swell gate's target arm is open at the asserted tick, targetIsPrey="
                        + (creeper.getTarget() == prey) + " preyAlive=" + prey.isAlive()
                        + " health=" + prey.getHealth() + " distSqr="
                        + creeper.distanceToSqr(prey));

        // The draw that reddened the probe, stood in the pad as a constant: a solid block in
        // the creeper's own eye cell closes getSensing().hasLineOfSight(prey) — the same clip
        // that measured clip=BLOCK cobblestone on the red leg — so the LOS arm of the gate is
        // asserted here and never drawn by the grid.
        // GameTestHelper#setBlock applies absolutePos itself, so the pad cell goes in RELATIVE
        // (1,3,1); eyeCell above is the absolute witness for the messages and the reads.
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.COBBLESTONE.defaultBlockState());
        helper.assertTrue(!creeper.getSensing().hasLineOfSight(prey),
                "the pad occludes the creeper's eye cell so the swell gate's LOS arm is a"
                        + " constant and not a grid draw, eye=" + eyeCell + " block="
                        + level.getBlockState(eyeCell) + " los="
                        + creeper.getSensing().hasLineOfSight(prey) + " align16="
                        + Math.floorMod(eyeCell.getX(), 16) + ","
                        + Math.floorMod(eyeCell.getZ(), 16));

        creeper.setCreeperState(1);
        creeper.tick();
        helper.assertTrue(creeper.getTimeSinceIgnited() == 1,
                "the fuse accumulates by the swell state, got " + creeper.getTimeSinceIgnited()
                        + " with state=" + creeper.getCreeperState() + " targetIsPrey="
                        + (creeper.getTarget() == prey) + " preyAlive=" + prey.isAlive()
                        + " los=" + creeper.getSensing().hasLineOfSight(prey)
                        + " swellOnSelector=" + (swellGoalOf(creeper) != null)
                        + " eye=" + eyeCell + " block="
                        + level.getBlockState(eyeCell) + " tickCount=" + creeper.tickCount + " rows:"
                        + goalRows(creeper));

        // Off the live selector the probe drives the gate's two arms itself. WrappedGoal#start
        // and WrappedGoal#tick (GoalSelector#goalUpdate / tickRunningGoals) call exactly these,
        // so this is the same call the engine makes — it is only the engine's ARMING that the
        // probe has taken out of the draw.
        swell.start();                                       // start 20-23
        swell.tick();                                        // update 29-38
        helper.assertTrue(creeper.getCreeperState() == -1,
                "with the LOS arm closed the swell goal resets the state to -1 in the SAME tick"
                        + " (update 29-38), got " + creeper.getCreeperState() + " los="
                        + creeper.getSensing().hasLineOfSight(prey) + " preyAlive="
                        + prey.isAlive() + " health=" + prey.getHealth() + " distSqr="
                        + creeper.distanceToSqr(prey));
        helper.assertTrue(TaintCreeperAiLogic.nextFuse(0, creeper.getCreeperState(), 30) == 0,
                "a closed swell state decays a fresh fuse to 0 (the red leg's got 0), got "
                        + TaintCreeperAiLogic.nextFuse(0, creeper.getCreeperState(), 30));

        // the open arm. Mob#serverAiStep clears the per-id seen/unseen cache at the top of
        // every AI tick (Sensing#tick), so a pad change has to clear it the same way — a stale
        // "unseen" entry reads the block that is no longer there.
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.AIR.defaultBlockState());
        creeper.getSensing().tick();
        helper.assertTrue(creeper.getSensing().hasLineOfSight(prey),
                "with the pad cleared the LOS arm is open at " + eyeCell + ", block="
                        + level.getBlockState(eyeCell) + " preyAlive=" + prey.isAlive());
        swell.tick();
        helper.assertTrue(creeper.getCreeperState() == 1,
                "with the target alive, in range and visible the swell goal keeps the state at 1"
                        + " (update 29-38), got " + creeper.getCreeperState() + " preyAlive="
                        + prey.isAlive() + " health=" + prey.getHealth() + " distSqr="
                        + creeper.distanceToSqr(prey) + " los="
                        + creeper.getSensing().hasLineOfSight(prey));
        swell.stop();                                        // stop 25-28: target released
        helper.assertTrue(creeper.getCreeperState() == 1,
                "stop() releases the captured target and does not write the state, got "
                        + creeper.getCreeperState());
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

    /** The swell goal row of a live creeper's goal selector, or null when it has been removed. */
    private static EntityTaintCreeper.SwellGoal swellGoalOf(final EntityTaintCreeper creeper) {
        for (final WrappedGoal wrapped : creeper.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof EntityTaintCreeper.SwellGoal goal) {
                return goal;
            }
        }
        return null;
    }

    /** The priority the swell row is registered at on the live selector, -1 when it is absent. */
    private static int swellPriorityOf(final EntityTaintCreeper creeper) {
        for (final WrappedGoal wrapped : creeper.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof EntityTaintCreeper.SwellGoal) {
                return wrapped.getPriority();
            }
        }
        return -1;
    }

    /** The live goal selector as "priority=Goal*RUN" rows, for a failure that names its axis. */
    private static String goalRows(final EntityTaintCreeper creeper) {
        final StringBuilder rows = new StringBuilder();
        for (final WrappedGoal wrapped : creeper.goalSelector.getAvailableGoals()) {
            rows.append(' ').append(wrapped.getPriority()).append('=')
                    .append(wrapped.getGoal().getClass().getSimpleName());
            if (wrapped.isRunning()) {
                rows.append("*RUN");
            }
        }
        return rows.toString();
    }
}
