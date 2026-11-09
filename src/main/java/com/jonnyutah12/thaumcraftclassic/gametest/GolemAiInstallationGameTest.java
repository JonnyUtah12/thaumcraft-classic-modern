package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemCoreTask;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TC-M6-08-s-b2 probes: {@code setupGolem()} installs the goals the core table
 * promises, at the original's selector indices, and nothing at all for a
 * core-less golem.
 *
 * <p>The observable state here is the live goal list on a real spawned golem
 * ({@code goalSelector.getAvailableGoals()} / {@code targetSelector}
 * {@code .getAvailableGoals()}), compared against {@link
 * GolemCoreTask#goalNames()} + {@link GolemCoreTask#commonGoalNames()} — the
 * table the parent card pinned from {@code EntityGolemBase.setupGolem()}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/golems/
 * EntityGolemBase.java:214-296}). A drift between the table and the
 * installation fails here, which is what keeps the table honest after b2.</p>
 *
 * <p>The two names the installation is allowed to omit are the two goals gated
 * on sibling-card entities: {@code AIDartAttack} (needs {@code EntityGolemDart})
 * and {@code AIFish} (needs {@code EntityGolemBobber}). The probes assert the
 * omissions explicitly, so a later card that ships those entities has to update
 * the expectation on purpose.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemAiInstallationGameTest {

    /** The face the probe golems are "placed against" (see GolemRuntimeGameTest). */
    private static final int HOME_FACING = Direction.NORTH.get3DDataValue();

    /** The original's vanilla names for the two vanilla goals in its common tail. */
    private static final String WATCH_CLOSEST = "EntityAIWatchClosest";
    private static final String LOOK_IDLE = "EntityAILookIdle";

    private GolemAiInstallationGameTest() {
    }

    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    private static void clearGolemCaches() {
// NOTE: do NOT call GolemHelper.clearJarListForTest() here. The jar list is a
// process-wide static and game tests in a batch run CONCURRENTLY, so clearing it
// starves other probes' jar lookups (it broke InfusionAltarGameTest's potentia drain).
// Golem positions are per-template cells, so a stale entry from another probe's
// template can never resolve to a block in this one; item timeouts are keyed per
// golem, and each probe spawns a fresh one, so clearing only those is safe.
        // GolemHelper.clearJarListForTest(); // deliberately not called, see above
        GolemHelper.clearItemTimeoutsForTest();
    }

    private static EntityGolemBase spawn(final ServerLevel level, final BlockPos home,
                                         final EnumGolemType material, final int core) {
        final EntityGolemBase golem = CatalogGolems.GOLEM.get().create(level);
        golem.setMaterial(material);
        golem.setCore(core);
        golem.setOwnerName("probe-owner");
        golem.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
        golem.setup(HOME_FACING);
        golem.restrictTo(home, EntityGolemBase.HOME_RANGE);
        level.addFreshEntity(golem);
        return golem;
    }

    /** The original's vanilla goal names mapped to the classes this port installs. */
    private static String modernName(final String originalName) {
        return switch (originalName) {
            case WATCH_CLOSEST -> "LookAtPlayerGoal";
            case LOOK_IDLE -> "RandomLookAroundGoal";
            default -> originalName;
        };
    }

    /** The goal class names this port must install for one core, sorted. */
    private static List<String> expectedFor(final GolemCoreTask task) {
        final List<String> names = new ArrayList<>();
        for (final String name : task.goalNames()) {
            if (name.equals("AIFish")) {
                continue; // gated on EntityGolemBobber (TC-M6-08-s-c)
            }
            names.add(modernName(name));
        }
        for (final String name : GolemCoreTask.commonGoalNames()) {
            names.add(modernName(name));
        }
        Collections.sort(names);
        return names;
    }

    /** The simple class names of everything installed on both selectors, sorted. */
    private static List<String> installed(final EntityGolemBase golem) {
        final List<String> names = new ArrayList<>();
        for (final WrappedGoal goal : golem.goalSelector.getAvailableGoals()) {
            names.add(goal.getGoal().getClass().getSimpleName());
        }
        for (final WrappedGoal goal : golem.targetSelector.getAvailableGoals()) {
            names.add(goal.getGoal().getClass().getSimpleName());
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Every real core gets exactly the goals its table row names, installed by
     * {@code setupGolem()} on a live golem, with the two sibling-card gates
     * (AIDartAttack, AIFish) asserted as deliberate omissions.
     */
    @GameTest(template = "empty_24")
    public static void installation_everyCoreInstallsExactlyTheGoalsItsTableNames(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        for (final GolemCoreTask task : GolemCoreTask.values()) {
            final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 3), EnumGolemType.IRON, task.id());
            final List<String> installedNames = installed(golem);
            final List<String> expectedNames = expectedFor(task);
            helper.assertTrue(installedNames.equals(expectedNames),
                    "core " + task.id() + " (" + task.coreName() + ") installs " + installedNames
                            + ", expected " + expectedNames);
            helper.assertTrue(!installedNames.contains("AIFish") || task != GolemCoreTask.FISHING,
                    "AIFish is gated on EntityGolemBobber, so the Fishing core installs no task goal");
            helper.assertTrue(installedNames.contains("AIAvoidCreeperSwell"),
                    "the common AIAvoidCreeperSwell (EntityGolemBase.java:217-219) is installed for core " + task.id());
            helper.assertTrue(!installedNames.contains("AIDartAttack"),
                    "AIDartAttack is gated on EntityGolemDart, so no core installs it");
            golem.discard();
        }
        helper.succeed();
    }

    /**
     * Guard with the dart decoration ("R") and Butcher keep the same goal set:
     * the original's {@code if (this.decoration.contains("R"))} arm adds only the
     * gated dart goal, so the seam must not change the installed set.
     */
    @GameTest(template = "empty_24")
    public static void installation_theRdecorationAddsNothingWhileTheDartIsGated(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final EntityGolemBase plain = spawn(level, at(helper, 2, 1, 3), EnumGolemType.IRON, 4);
        final EntityGolemBase darty = spawn(level, at(helper, 4, 1, 3), EnumGolemType.IRON, 4);
        darty.setDecoration("R");
        darty.setupGolem();
        helper.assertTrue(installed(plain).equals(installed(darty)),
                "the decoration 'R' arm installs only the gated AIDartAttack, so the live set is unchanged: "
                        + installed(darty));
        helper.assertTrue(installed(plain).containsAll(
                        List.of("AIGolemAttackOnCollide", "AIHurtByTarget", "AINearestAttackableTarget")),
                "the Guard core's melee and target goals are the table's three names");
        plain.discard();
        darty.discard();
        helper.succeed();
    }

    /**
     * The installed goals are not dead weight: a Guard golem (core 4) acquires a
     * hostile and runs a goal with NOTHING hand-driven — no probe calls
     * {@code canUse/start/tick} and no probe touches the selectors. That is the
     * evidence that 1.21.1's {@code Mob.serverAiStep()} — which ticks
     * {@code targetSelector} then {@code goalSelector} — is reached through this
     * entity's own tick, so the {@code setupGolem()} installation is live in
     * production, not only under a probe's direct drive.
     *
     * <p>Geometry is measured, not assumed: a zombie sealed in a GLASS shell five
     * blocks away is NEVER acquired, because 1.21.1's {@code hasLineOfSight} clip
     * stops on glass (the first version of this probe asserted through the shell
     * and read {@code target == null}). So the zombie stands in the open, 3 blocks
     * away, {@code NoAi} so it cannot wander out of the 10-block home box, and
     * mortal: {@code TargetingConditions.forCombat()} rejects a witness marked
     * invulnerable, so invulnerability would make the claim unfalsifiable
     * (measured: {@code target == null} forever). The probe therefore samples the
     * arming window tick by tick and reads its facts at the arming tick, which is
     * where the golem is a watcher and not yet a killer.</p>
     */
    @GameTest(template = "empty_24", timeoutTicks = 400)
    public static void installation_theInstalledGoalsAndTargetsTickOnTheirOwn(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final EntityGolemBase golem = spawn(level, at(helper, 12, 1, 12), EnumGolemType.IRON, 4);
        // A Guard golem is a WORKING golem: the port models the original's
        // "!wandering && !busy" gate as paused/inactive (the GUI pause toggle and the
        // inert-fetter check at EntityGolemBase:1268). A freshly placed golem is neither,
        // so assert that instead of hand-setting fields the 1.7.10 entity had.
        golem.setPaused(false);
        helper.assertTrue(!golem.isPaused() && !golem.isInactive(),
                "the probe golem is a working golem: not paused, not standing on an inert fetter");
        // MEASURED (diag run): helper.spawn(EntityType, BlockPos) TRANSLATES its position by
        // the template origin, so the cell handed to it must stay template-RELATIVE. Passing
        // at(helper, ...) double-translates: the witness landed 1.08e14 squared blocks from
        // the golem's home (isWithinRange false, search box empty, target null). The golem
        // side is the opposite: it is moveTo/restrictTo'd with absolute cells, so the two
        // spaces must be mixed deliberately, not by accident.
        final net.minecraft.world.entity.monster.Zombie zombie =
                helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(15, 1, 12));
        helper.assertTrue(zombie != null, "the witness zombie spawned");
        // Deliberately NOT invulnerable: 1.21.1's TargetingConditions.forCombat() rejects
        // targets that are invulnerable to the mob's damage source, so a setInvulnerable(true)
        // witness is untargetable and the Guard goal would never acquire it.
        zombie.setPersistenceRequired();
        zombie.setNoAi(true); // a witness, not a combatant: it must not wander out of range

        // Sample the acquisition window and read the facts at the tick the target
        // goal ARMS. A fixed late tick is not usable: an IRON golem (strength 4, so
        // ~4 damage a hit) kills an armour-free 20 HP zombie inside a dozen landed
        // hits, and the melee goal starts landing as soon as the target goal arms.
        // Measured: at tick 90 the witness was already dead and removed (and at 200
        // too), which is the installed goals WORKING, not a fault. Vanilla runs the
        // target selector every 2nd tick (TargetSelector.reduceTickFreq), so the
        // acquisition lands in the first tens of ticks and the probe reads it there.
        final long[] armedTick = new long[1]; // helper.getTick() is a long
        final boolean[] aliveAtArming = new boolean[1];
        final boolean[] selectorRunning = new boolean[1];
        for (int t = 10; t <= 90; t++) {
            helper.runAfterDelay(t, () -> {
                if (armedTick[0] != 0) {
                    return;
                }
                if (golem.getTarget() == zombie) {
                    armedTick[0] = helper.getTick();
                    aliveAtArming[0] = zombie.isAlive() && !zombie.isRemoved();
                    selectorRunning[0] = golem.goalSelector.getAvailableGoals().stream()
                                    .anyMatch(goal -> goal.isRunning())
                            || golem.targetSelector.getAvailableGoals().stream()
                                    .anyMatch(goal -> goal.isRunning());
                }
            });
        }

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(golem.isAlive() && !golem.isRemoved(),
                    "the golem is alive and not removed at the assertion tick");
            helper.assertTrue(armedTick[0] != 0,
                    "the Guard core's target goal acquired the hostile on its own tick, inside the"
                            + " sampled window 10-90 (target=" + golem.getTarget() + ", goals:"
                            + installed(golem) + ")");
            helper.assertTrue(aliveAtArming[0],
                    "the witness was alive at the acquisition tick " + armedTick[0]
                            + ", so the acquisition is the goal's own work and not a post-mortem"
                            + " lock-on (zombie alive " + zombie.isAlive() + ", removed "
                            + zombie.isRemoved() + ")");
            helper.assertTrue(selectorRunning[0],
                    "a goal was RUNNING at the acquisition tick " + armedTick[0]
                            + ", so goalSelector.tick()/targetSelector.tick() are reached from the"
                            + " entity's own tick, not from a probe's direct drive");
            golem.discard();
            zombie.discard();
            helper.succeed();
        });
    }

    /**
     * A core-less golem — what every other golem probe spawns — installs
     * nothing, so the 116 inherited probes cannot regress.
     */
    @GameTest(template = "empty_24")
    public static void installation_aGolemWithNoCoreInstallsNoGoals(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 3), EnumGolemType.WOOD,
                GolemCoreTask.NO_CORE);
        helper.assertTrue(golem.goalSelector.getAvailableGoals().isEmpty(),
                "core -1 installs no task goal (EntityGolemBase.java:217)");
        helper.assertTrue(golem.targetSelector.getAvailableGoals().isEmpty(),
                "core -1 installs no target goal");
        helper.assertTrue(installed(golem).isEmpty(), "both selectors are empty, got " + installed(golem));
        golem.discard();
        helper.succeed();
    }

    /**
     * The load path re-runs {@code setupGolem()} (the original's :693-694), and
     * BD-31's clear-both-selectors rule must keep the set from stacking: a
     * save → fresh entity → load round trip ends with exactly the same goal set
     * as a fresh install, not two copies of each Guard goal.
     */
    @GameTest(template = "empty_24")
    public static void installation_reloadReinstallsGoalsWithoutDuplicates(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final EntityGolemBase golem = spawn(level, at(helper, 2, 1, 3), EnumGolemType.IRON, 4);
        final List<String> fresh = installed(golem);
        helper.assertTrue(fresh.contains("AIHurtByTarget"), "the Guard core starts with its target goals");

        final CompoundTag tag = new CompoundTag();
        golem.addAdditionalSaveData(tag);
        final EntityGolemBase reloaded = CatalogGolems.GOLEM.get().create(level);
        reloaded.load(tag);
        reloaded.setup(HOME_FACING);
        // setup() installs once; the load path installs again — the BD-31 clear
        // has to keep the second pass idempotent.
        final List<String> afterLoad = installed(reloaded);
        helper.assertTrue(fresh.size() == afterLoad.size(),
                "reload does not stack goals: fresh " + fresh.size() + " vs reloaded " + afterLoad.size()
                        + " (" + afterLoad + ")");
        helper.assertTrue(fresh.equals(afterLoad), "and the sets are identical");
        golem.discard();
        reloaded.discard();
        helper.succeed();
    }
}
