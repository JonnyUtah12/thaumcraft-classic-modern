package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHarvestCrops;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIHarvestLogs;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIOpenDoor;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIReturnHome;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIUseItem;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Behaviour probes for the golem AI goals that touch the world: the core-7
 * chop, the core-3 harvest, the core-8 use, the door open/close and the
 * {@link AIReturnHome} leash.
 *
 * <p>Faithful to the original game tests
 * (thaumcraft.client.gui.blocks.gadgets.golem.blockTestedGolem
 * .registerGameTests): the crop probe needs three harvested crops plus
 * replanting (blockTestedGolem.java:178-186), the unripe probe needs the crop
 * untouched (blockTestedGolem.java:188-198), the chop probe needs felled logs
 * and a left-behind stone (blockTestedGolem.java:200-215), the use-item probe
 * needs the carried block placed on the marked target
 * (blockTestedGolem.java:217-235), and the door probe needs the door closed
 * again behind the golem (blockTestedGolem.java:237-250). The door and
 * return-home probes are extensions: the original suite never registered them
 * and {@code AIReturnHome} carries no {@code @GameTest} annotation there, so
 * those two are verified against the goal's own source.
 *
 * <p>Every probe drives a goal the way the golem's goal selector does
 * (canUse -&gt; start -&gt; tick, stop when canContinueToUse is false) inside a
 * window derived from the golem's observed {@code tickCount}: the golem is
 * added six ticks into the test and the template floor settles for ~180 ticks,
 * so no probe asserts an exact tick phase.
 *
 * <p><b>Geometry.</b> {@code empty_24} is all air and the framework drops the
 * structure block one cell above the settled floor, so a golem's FEET cell is
 * template-relative y=0 and its targets sit at y=0/y=1. Spawning the golem in
 * the same cell as a solid target suffocates it (one damage per tick) and that
 * death only surfaces as an unrelated assertion much later, so every probe
 * round runs {@link #assertAlive}. Driver windows are bounded above because
 * {@code helper.succeed()} removes every non-player entity inside the
 * structure bounds — a driver ticking on that same tick reports a liveness
 * failure for a test that just passed.
 *
 * <p>Coordinates: {@link GameTestHelper} block calls take template-relative
 * positions and entity/block-entity positions are absolute; the probes keep
 * the two in separate variables and only convert through
 * {@link #at(GameTestHelper, int, int, int)} when comparing against an entity.
 *
 * <p>Each probe also checks the per-core gate the original's
 * {@code setupGolem} applies when it installs the goal (the reference
 * behaviour arbiter, EntityGolemBase.java:554-683; the ports encode it as
 * {@code coreIs(...)}): a goal must refuse a golem with the wrong core.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemInteractAiGameTest {

    /** Thaumcraft {@code golemCoreHarvest} (reference EntityGolemBase.java:104). */
    private static final int CORE_HARVEST = 3;

    /** Thaumcraft {@code golemCoreChop} (reference EntityGolemBase.java:109). */
    private static final int CORE_CHOP = 7;

    /**
     * The core {@code setupGolem} installs {@link AIUseItem} for; the port
     * gates the goal on it (AIUseItem.java:94, {@code coreIs(8)}).
     */
    private static final int CORE_USE = 8;

    /** No core: every core-gated goal must refuse this golem. */
    private static final int CORE_NONE = -1;

    /**
     * The colour the probe marks its target with. A fresh golem's eighteen slot
     * colours are all 0, so {@link EntityGolemBase#getColorsMatching} offers 0
     * for an empty inventory and {@link AIUseItem#findSomething} stores it as
     * the goal's colour; {@link Marker#equalsFuzzy} wildcards only the QUERY
     * colour ({@code this.color == marker.color || this.color == -1}), so the
     * stored marker has to carry the slot colour 0, not the {@code -1} wildcard.
     */
    /**
     * A green suite is only auditable if a passing probe leaves a trace: the
     * framework logs failures alone, so each probe announces itself here.
     */
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final byte MARKER_COLOUR = 0;

    /** The home facing the runtime probes use. */
    private static final int HOME_FACING = Direction.NORTH.get3DDataValue();

    /**
     * Ticks the golem must have been ticking before a probe's drive window
     * opens: the 24x24x24 template floor settles for ~180 ticks and the golem
     * is added six ticks into the test, so the probes gate on the golem's own
     * {@code tickCount} and never on a hardcoded test-tick phase.
     */
    private static final int MOB_SETTLE = 180;

    /** The matching test-tick phase: {@code MOB_SETTLE} plus the 6-tick spawn delay. */
    private static final int SETTLE = MOB_SETTLE + 10;

    /**
     * Walk budget for the stray leg of
     * {@code interact_returnHomeUsesTheHomeRadiusAndTheEntityTeleportsFromBeyond48} — the 8-block
     * drop-off the goal has to walk back on its own. Measured on the game-test server (t_8dce1837,
     * 24 base-control legs on 7933db8): a driver that calls {@code start()} on every tick of an
     * open gate re-issues {@code moveTo} every tick, which rebuilds the path every tick, and that
     * walk ran between 30 and 315 ticks for the same 8 blocks (0.2 down to 0.013 blocks a tick);
     * the leg at the slow end went red 4 blocks out with {@code navDone=true}, the watchdog on the
     * whole test. Driven the way the goal selector drives it — one {@code start()}, then
     * {@code tick()} while {@code canContinueToUse} holds — the goal walks on its own 20-tick
     * attempt ladder (AIReturnHome.java:55-56, :100-121) at the golem's speed attribute: 8 blocks
     * at 0.1 (the shared fixture) is 80 ticks, at 0.0446 (the accessory-speed variant of the
     * dependency-present batch) 180, plus the goal-selector parity gate that costs the first 5
     * ticks ({@code (tickCount + getId()) % 2}, Mob.serverAiStep). The budget is that range with
     * margin; the test watchdog sits above the phases that use it.
     */
    private static final int WALK_BUDGET = 800;

    /**
     * The goal's own arrival threshold in squared blocks: {@code AIReturnHome.canUse} clears the
     * goal under 3 squared blocks from home (AIReturnHome.java:37). The probe states its arrival
     * in that unit rather than in a block count of its own.
     */
    private static final double HOME_THRESHOLD_SQUARED = 3.0D;

    /**
     * The entity's own leash in squared blocks: {@code EntityGolemBase} teleports a golem home
     * from 48 squared-blocks and beyond (the 2304.0D gate in its base tick), on the parity tick
     * {@code (tickCount + getId()) % 2}.
     */
    private static final double LEASH_SQUARED = 2304.0D;

    private GolemInteractAiGameTest() {
    }

    /**
     * {@code GolemHelper}'s jar list and item-timeout list are static, so they
     * leak between game tests; every probe clears them first.
     */
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

    /** Template-relative cell as an absolute world position (for entity math). */
    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /** The port's {@code GolemHelper.dimensionOf} (package-private) — stable {@code String.hashCode}. */
    private static int dim(final ServerLevel level) {
        return level.dimension().location().hashCode();
    }

    /**
     * Mark one face of a block for the golem, in the form
     * {@code AIUseItem.findSomething} accepts: a real face (side 0..5), not the
     * {@code side = 6} "no face" a position-only mark stores.
     */
    private static void mark(final EntityGolemBase golem, final BlockPos absolutePos, final Direction side) {
        golem.addMarker(new Marker(absolutePos.getX(), absolutePos.getY(), absolutePos.getZ(),
                dim((ServerLevel) golem.level()), (byte) side.get3DDataValue(), MARKER_COLOUR));
    }

    /**
     * Drive goals the way the golem's goal selector does, inside the window
     * {@code [SETTLE, endTick)} of test ticks; the probe's assertion step runs
     * at {@code endTick}.
     */
    private static void drive(final GameTestHelper helper, final int endTick, final Runnable... actions) {
        final AtomicInteger tick = new AtomicInteger(-1);
        helper.onEachTick(() -> {
            final int now = tick.incrementAndGet();
            if (now < SETTLE || now >= endTick) {
                return;
            }
            for (final Runnable action : actions) {
                action.run();
            }
        });
    }

    /**
     * Liveness gate. A golem that suffocates in a solid cell or drops into the
     * void dies mid-probe, and a dead goal driver surfaces as an unrelated
     * assertion much later, so fail loudly at the point of death.
     */
    private static void assertAlive(final EntityGolemBase golem) {
        if (golem == null || golem.isRemoved() || !golem.isAlive()) {
            throw new GameTestAssertException("the probe golem is no longer alive (removed="
                    + (golem == null ? "n/a" : golem.isRemoved()) + ", health="
                    + (golem == null ? "n/a" : golem.getHealth()) + ", feet="
                    + (golem == null ? "n/a" : golem.blockPosition()) + ")");
        }
    }

    /** Why a goal refused to activate, in one readable string. */
    private static String state(final EntityGolemBase golem) {
        return "core=" + golem.getCore() + " carried=" + golem.getCarried() + " carrySpace="
                + golem.getCarrySpace() + " toggles=" + golem.getTogglesValue() + " markers="
                + golem.getMarkers().size() + " home=" + golem.getRestrictCenter() + " feet="
                + golem.blockPosition() + " tickCount=" + golem.tickCount
                + " navDone=" + golem.getNavigation().isDone();
    }

    /** Spawn a golem standing in {@code feetCell} (the cell, not the block in it). */
    private static EntityGolemBase spawn(final ServerLevel level, final BlockPos feetCell,
            final EnumGolemType material, final int core) {
        final EntityGolemBase golem = CatalogGolems.GOLEM.get().create(level);
        golem.setMaterial(material);
        golem.setCore(core);
        golem.setOwnerName("probe-owner");
        golem.moveTo(feetCell.getX() + 0.5D, feetCell.getY(), feetCell.getZ() + 0.5D, 0.0F, 0.0F);
        golem.setup(HOME_FACING);
        // setup() runs setupGolem() (the original's load-path install, :693), which puts the
        // core's goals on the live entity. These probes DRIVE goals by hand, so the fixture
        // owns the goal set: drop the installation, then add exactly what the probe installs.
        // A live goal walks the golem out of the cell the probe asserts in, and a live
        // AIEssentiaGather drains the probe's jar before the probe's own draw.
        golem.goalSelector.removeAllGoals(goal -> true);
        golem.targetSelector.removeAllGoals(goal -> true);
        golem.restrictTo(feetCell, EntityGolemBase.HOME_RANGE);
        // Toggle 0 (pickup) off: AIUseItem.findSomething only accepts a
        // non-solid marker while pickup is off (AIUseItem.java:232-235).
        golem.setTogglesValue((byte) 0);
        level.addFreshEntity(golem);
        return golem;
    }

    private static EntityGolemBase require(final EntityGolemBase[] slot, final int index) {
        final EntityGolemBase golem = slot[index];
        assertAlive(golem);
        return golem;
    }

    // ------------------------------------------------------------------
    // 1. Harvest: a core-3 golem reaps ripe wheat and replants it
    // (blockTestedGolem.java:178-186)
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 700)
    public static void interact_harvestCropsCoreReapsRipeWheatAndReplantsIt(final GameTestHelper helper) {
        clearGolemCaches();
        final BlockPos home = at(helper, 4, 0, 4);
        // findGrownCrop() pops columns of the shuffled grid home.x +/-d, home.z
        // +/-d with d = sqrt(getRange()/4) = 2 (AIHarvestCrops.java:111,373-394),
        // scanning home.y +/-3, and tick() only chops within 2 blocks of the
        // golem (AIHarvestCrops.java:154). The crops therefore live in the x=5
        // column, z=3..6: inside the grid, adjacent to the golem's cell, and
        // leaving the golem's own cell (4,0,4) walkable.
        // findGrownCrop() pops ONE random column of the shuffled grid per
        // canUse attempt, and canUse only passes on golem-delay ticks, so the
        // discovery cost is grid-size over crop-count. Eight columns inside the
        // grid (and never in the golem's own cell) keep the first find cheap;
        // checkAdjacent() (±2 x/z, ±1 y) then chains the rest.
        final int[][] cells = {
                { 5, 3 }, { 5, 4 }, { 5, 5 }, { 5, 6 },
                { 4, 3 }, { 4, 5 }, { 3, 4 }, { 3, 5 },
        };
        final BlockPos[] crops = new BlockPos[cells.length];
        for (int i = 0; i < crops.length; i++) {
            final int x = cells[i][0];
            final int z = cells[i][1];
            crops[i] = new BlockPos(x, 1, z);
            helper.setBlock(x, 0, z, Blocks.FARMLAND.defaultBlockState());
            helper.setBlock(x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        }

        final AIHarvestCrops[] goal = new AIHarvestCrops[1];
        final EntityGolemBase[] golems = new EntityGolemBase[1];
        helper.runAtTickTime(6, () -> {
            golems[0] = spawn(helper.getLevel(), home, EnumGolemType.WOOD, CORE_HARVEST);
            assertAlive(golems[0]);
            goal[0] = new AIHarvestCrops(golems[0]);
        });

        final AtomicInteger harvested = new AtomicInteger(0);
        final int[] ages = new int[crops.length];
        drive(helper, SETTLE + 460, () -> {
            if (goal[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < MOB_SETTLE) {
                return;
            }
            for (int i = 0; i < crops.length; i++) {
                final int age = helper.getBlockState(crops[i]).getValue(CropBlock.AGE);
                if (ages[i] > 0 && age == 0) {
                    harvested.incrementAndGet();
                }
                ages[i] = age;
            }
            if (goal[0].canUse()) {
                goal[0].start();
            }
            if (goal[0].canContinueToUse()) {
                goal[0].tick();
            } else {
                goal[0].stop();
            }
        });

        helper.runAfterDelay(SETTLE + 460, () -> {
            clearGolemCaches();
            assertAlive(golems[0]);
            int replanted = 0;
            int grown = 0;
            for (final BlockPos crop : crops) {
                final BlockState state = helper.getBlockState(crop);
                if (state.is(Blocks.WHEAT)) {
                    if (state.getValue(CropBlock.AGE) == 0) {
                        replanted++;
                    } else {
                        grown++;
                    }
                }
            }
            helper.assertTrue(harvested.get() >= 3,
                    "the harvest goal never reaped the ripe wheat (harvested=" + harvested.get()
                            + ", replanted=" + replanted + ", still grown=" + grown + "; "
                            + state(golems[0]) + ")");
            helper.assertTrue(replanted >= 3,
                    "the harvest goal reaped " + harvested.get() + " crops but replanted only " + replanted
                            + " (the original sows a seed back onto the farmland)");
            passed("interact_harvestCropsCoreReapsRipeWheatAndReplantsIt");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------
    // 2. Harvest: unripe wheat is left alone (blockTestedGolem.java:188-198)
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 520)
    public static void interact_harvestCropsCoreLeavesUnripeWheatAlone(final GameTestHelper helper) {
        clearGolemCaches();
        final BlockPos home = at(helper, 4, 0, 4);
        final BlockPos crop = new BlockPos(4, 1, 5);
        helper.setBlock(4, 0, 5, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(4, 1, 5, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 2));

        final AIHarvestCrops[] goal = new AIHarvestCrops[1];
        final EntityGolemBase[] golems = new EntityGolemBase[1];
        helper.runAtTickTime(6, () -> {
            golems[0] = spawn(helper.getLevel(), home, EnumGolemType.WOOD, CORE_HARVEST);
            assertAlive(golems[0]);
            goal[0] = new AIHarvestCrops(golems[0]);
        });

        final AtomicInteger activations = new AtomicInteger(0);
        drive(helper, SETTLE + 140, () -> {
            if (goal[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < MOB_SETTLE) {
                return;
            }
            if (goal[0].canUse()) {
                activations.incrementAndGet();
                goal[0].start();
                goal[0].stop();
            }
        });

        helper.runAfterDelay(SETTLE + 140, () -> {
            clearGolemCaches();
            assertAlive(golems[0]);
            helper.assertTrue(activations.get() == 0,
                    "the harvest goal activated on unripe wheat " + activations.get() + " times ("
                            + state(golems[0]) + ")");
            helper.assertBlockProperty(crop, CropBlock.AGE, 2);
            passed("interact_harvestCropsCoreLeavesUnripeWheatAlone");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------
    // 3. Chop: a core-7 golem fells real logs and leaves stone alone
    // (blockTestedGolem.java:200-215)
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 620)
    public static void interact_chopCoreFellsRealLogsAndLeavesStoneAlone(final GameTestHelper helper) {
        clearGolemCaches();
        final BlockPos home = at(helper, 4, 0, 4);
        // A wood mass on the settled floor (y=0) with trunks above it: the log
        // tree AIHarvestLogs.harvest()/checkAdjacent() consumes.
        final int[][] mass = {
                { 2, 0, 2 }, { 2, 0, 3 }, { 2, 0, 4 }, { 2, 0, 5 },
                { 6, 0, 2 }, { 6, 0, 3 }, { 6, 0, 4 }, { 6, 0, 5 },
                { 4, 0, 6 }, { 2, 1, 4 }, { 6, 1, 4 }, { 4, 1, 6 },
                { 3, 2, 4 }, { 4, 2, 4 }, { 5, 2, 4 }, { 4, 2, 3 }, { 4, 2, 5 },
        };
        for (final int[] cell : mass) {
            helper.setBlock(cell[0], cell[1], cell[2], Blocks.OAK_LOG.defaultBlockState());
        }
        // Stone is never a target: the log search must skip it.
        helper.setBlock(2, 0, 6, Blocks.STONE.defaultBlockState());
        // A log column in the core-3 golem's own range, which nothing here may
        // touch (the chop goal is core-7's, and probe 7 proves it over a long run).
        helper.setBlock(12, 0, 4, Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(12, 1, 4, Blocks.OAK_LOG.defaultBlockState());

        final AIHarvestLogs[] chop = new AIHarvestLogs[1];
        final AIHarvestCrops[] cropsOnChopCore = new AIHarvestCrops[1];
        final AIUseItem[] useOnChopCore = new AIUseItem[1];
        final AIHarvestLogs[] logsOnHarvestCore = new AIHarvestLogs[1];
        final EntityGolemBase[] golems = new EntityGolemBase[3];
        helper.runAtTickTime(6, () -> {
            golems[0] = spawn(helper.getLevel(), home, EnumGolemType.WOOD, CORE_CHOP);
            assertAlive(golems[0]);
            chop[0] = new AIHarvestLogs(golems[0]);
            cropsOnChopCore[0] = new AIHarvestCrops(golems[0]);
            useOnChopCore[0] = new AIUseItem(golems[0]);
            // The core gate reads the golem's live core field, so the gate
            // checks run on goals built for golems of the other cores.
            golems[1] = spawn(helper.getLevel(), at(helper, 10, 0, 4), EnumGolemType.WOOD, CORE_HARVEST);
            assertAlive(golems[1]);
            logsOnHarvestCore[0] = new AIHarvestLogs(golems[1]);
            golems[2] = spawn(helper.getLevel(), at(helper, 16, 0, 4), EnumGolemType.WOOD, CORE_NONE);
            assertAlive(golems[2]);
        });

        final AtomicInteger activations = new AtomicInteger(0);
        final int planted = countLogs(helper, 0, 15);
        drive(helper, SETTLE + 400, () -> {
            if (chop[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < MOB_SETTLE) {
                return;
            }
            if (chop[0].canUse()) {
                activations.incrementAndGet();
                chop[0].start();
            }
            if (chop[0].canContinueToUse()) {
                chop[0].tick();
            } else {
                chop[0].stop();
            }
        });

        helper.runAfterDelay(SETTLE + 400, () -> {
            clearGolemCaches();
            for (final EntityGolemBase golem : golems) {
                assertAlive(golem);
            }
            helper.assertTrue(activations.get() >= 1,
                    "the chop goal never activated on the log mass (" + state(golems[0]) + ")");
            final int remaining = countLogs(helper, 0, 15);
            helper.assertTrue(remaining <= planted - 5,
                    "the core-7 chop goal felled only " + (planted - remaining) + " of " + planted
                            + " logs (the original requires five); activations=" + activations.get()
                            + " " + state(golems[0]));
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 0, 6)).is(Blocks.STONE),
                    "the chop goal broke the stone block at the mass edge");
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 0, 4)).is(Blocks.OAK_LOG),
                    "the core-7 golem chopped outside its own log mass");
            // Per-core gate: the harvest goal refuses the core-7 golem, the
            // chop goal refuses the core-3 golem, and the use goal refuses a
            // golem that is not its own core (setupGolem, EntityGolemBase.java:554-683).
            helper.assertTrue(!cropsOnChopCore[0].canUse(),
                    "the core-3 harvest goal activated on the core-7 (chop) golem: " + state(golems[0]));
            helper.assertTrue(!logsOnHarvestCore[0].canUse(),
                    "the core-7 chop goal activated on the core-3 (harvest) golem: " + state(golems[1]));
            helper.assertTrue(!useOnChopCore[0].canUse(),
                    "the core-8 use goal activated on the core-7 (chop) golem: " + state(golems[0]));
            passed("interact_chopCoreFellsRealLogsAndLeavesStoneAlone");
            helper.succeed();
        });
    }

    /** Log the probe's name once its assertions all hold. */
    private static void passed(final String probe) {
        LOGGER.info("[TC-M6-08-s-b] golem interact probe passed: {}", probe);
    }

    /** OPEN of a door state, as a compact tag for the failure message. */
    private static String open(final BlockState state) {
        return state.hasProperty(DoorBlock.OPEN) ? "[" + state.getValue(DoorBlock.OPEN) + "]" : "[n/a]";
    }

    /** Every non-air block in the 3x3x3 around a template-relative cell. */
    private static String around(final GameTestHelper helper, final BlockPos centre) {
        final StringBuilder out = new StringBuilder();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final BlockPos cell = centre.offset(dx, dy, dz);
                    final BlockState state = helper.getBlockState(cell);
                    if (!state.isAir()) {
                        out.append(cell.getX()).append(',').append(cell.getY()).append(',')
                                .append(cell.getZ()).append('=').append(state.getBlock()).append(' ');
                    }
                }
            }
        }
        return out.toString().trim();
    }

    private static int countLogs(final GameTestHelper helper, final int xFrom, final int xTo) {
        int n = 0;
        for (int x = xFrom; x <= xTo; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = 1; z <= 7; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.OAK_LOG)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // 4. Use item: a core-8 golem places its carried block on the marked
    // target (blockTestedGolem.java:217-235)
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 520)
    public static void interact_useItemCorePlacesItsCarriedBlockOnItsMarkedTarget(final GameTestHelper helper) {
        clearGolemCaches();
        final BlockPos home = at(helper, 4, 0, 4);
        final BlockPos spawnAt = at(helper, 5, 0, 4);
        final BlockPos target = at(helper, 7, 0, 4);
        // The placement target is an empty, non-solid cell over a solid floor:
        // with toggle 0 (pickup) off, findSomething() only accepts a marker on a
        // NON-solid block (the XOR at AIUseItem.java:233-235), the marked face is
        // UP so the cell under it is the solid anchor the marker needs
        // (AIUseItem.java:236-239), and the goal walks there — a cell covered in
        // a snow layer is not a path target (createPath returns null, so
        // canContinueToUse's "!navigation.isDone()" at AIUseItem.java:113 fails
        // the round after start()). Air is Blocks.Properties.replaceable, so the
        // click's BlockPlaceContext places into the clicked cell itself and the
        // carried block lands on the marked target.
        // The target sits two cells out from the spawn, not beside it: canUse's
        // reach is sqrt(range / 3) (AIUseItem.java:84) so the marker has to be
        // within ~2.3 cells, and 1.21.1's Path.isDone() is true the moment the
        // walker is on the last node — an adjacent target is a two-node path, so
        // canContinueToUse's "!navigation.isDone()" (AIUseItem.java:113) kills the
        // goal the round after start() and tick(), the only click site
        // (AIUseItem.java:123-127), never runs. Two cells out gives a three-node
        // path and a real walk, which is the shape the goal has in play.
        helper.setBlock(7, 0, 4, Blocks.AIR.defaultBlockState());
        helper.setBlock(6, 0, 4, Blocks.AIR.defaultBlockState());
        helper.setBlock(7, -1, 4, Blocks.STONE.defaultBlockState());

        final AIUseItem[] goal = new AIUseItem[1];
        final EntityGolemBase[] golems = new EntityGolemBase[1];
        helper.runAtTickTime(6, () -> {
            final EntityGolemBase golem = spawn(helper.getLevel(), spawnAt, EnumGolemType.WOOD, CORE_USE);
            assertAlive(golem);
            // The carried stack and the marked face: what the pickup step and
            // the player's right-click marking do in play.
            golem.setCarried(new ItemStack(Blocks.OAK_LOG));
            mark(golem, target, Direction.UP);
            helper.assertTrue(golem.getMarkers().size() == 1, "marking the target stored no marker");
            golems[0] = golem;
            goal[0] = new AIUseItem(golem);
        });

        final AtomicBoolean activated = new AtomicBoolean(false);
        final AtomicBoolean placed = new AtomicBoolean(false);
        // GoalSelector keeps the running bit outside the goal (1.21.1's Goal has
        // no isRunning()), so the probe tracks it the same way.
        final boolean[] running = new boolean[1];
        final int[] starts = new int[1];
        final int[] ticks = new int[1];
        final int[] stops2 = new int[1];
        final StringBuilder trace = new StringBuilder();
        drive(helper, SETTLE + 160, () -> {
            if (goal[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < MOB_SETTLE) {
                return;
            }
            if (helper.getBlockState(new BlockPos(7, 0, 4)).is(Blocks.OAK_LOG)) {
                placed.set(true);
            }
            final boolean gate = running[0] ? goal[0].canContinueToUse() : goal[0].canUse();
            if (placed.get() && !running[0] && starts[0] > 0) {
                return;
            }
            if (starts[0] + ticks[0] + stops2[0] < 45 && trace.length() < 1800) {
                trace.append("tc=").append(golem.tickCount)
                        .append(" run=").append(running[0])
                        .append(" gate=").append(gate)
                        .append(" nav=").append(golem.getNavigation().isDone())
                        .append(" carried=").append(golem.getCarried() == null ? "null"
                                : (golem.getCarried().isEmpty() ? "none" : "log"))
                        .append(" tgt=").append(helper.getBlockState(new BlockPos(7, 0, 4)).getBlock())
                        .append(" s=").append(starts[0]).append("/t=").append(ticks[0])
                        .append("/p=").append(placed.get()).append(" | ");
            }
            // Drive the goal exactly as GoalSelector does: canUse -> start while
            // it is idle, tick while canContinueToUse, stop when it lapses. The
            // goal has a real cooldown (canUse stamps nextTick, AIUseItem.java:99
            // -103) and canContinueToUse ends as soon as the walk finishes
            // (AIUseItem.java:111-113), so a latched "activated" flag would stop
            // the goal one tick after it starts and tick() - the only click site
            // (AIUseItem.java:123-127) - would never run.
            if (running[0]) {
                if (gate) {
                    ticks[0]++;
                    goal[0].tick();
                } else {
                    goal[0].stop();
                    running[0] = false;
                    stops2[0]++;
                }
            } else if (gate) {
                activated.set(true);
                starts[0]++;
                goal[0].start();
                running[0] = true;
            }
        });

        helper.runAfterDelay(SETTLE + 160, () -> {
            clearGolemCaches();
            assertAlive(golems[0]);
            helper.assertTrue(activated.get(),
                    "the Use goal never activated on the marked target (" + state(golems[0]) + ")");
            helper.assertTrue(placed.get(),
                    "the Use goal activated at the target cell " + target + " but never placed the "
                            + "carried block there (" + state(golems[0])
                            + " around=" + around(helper, new BlockPos(7, 0, 4))
                            + " trace=" + trace + ")");
            // The block lands on the marked target only: not in the cell the
            // golem walked through, not above the marked face, and the solid
            // anchor under the marker stays put.
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 0, 4)).isAir(),
                    "the carried block landed in the golem's own cell, not on the marked target");
            helper.assertTrue(helper.getBlockState(new BlockPos(6, 0, 4)).isAir(),
                    "the carried block landed on the way to the target, not on the marked cell");
            helper.assertTrue(helper.getBlockState(new BlockPos(7, 1, 4)).isAir(),
                    "the carried block landed above the marked face, not in the marked cell");
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 0, 4)).isAir(),
                    "the carried block landed past the target");
            helper.assertTrue(helper.getBlockState(new BlockPos(7, -1, 4)).is(Blocks.STONE),
                    "the solid anchor under the marked cell changed");
            passed("interact_useItemCorePlacesItsCarriedBlockOnItsMarkedTarget");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------
    // 5. Door: open it, pass through, close it behind (blockTestedGolem.java:237-250)
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 520)
    public static void interact_golemopensarealdoorandclosesitbehindit(final GameTestHelper helper) {
        clearGolemCaches();
        final BlockPos doorway = at(helper, 5, 0, 4);
        helper.setBlock(5, 0, 4, Blocks.OAK_DOOR.defaultBlockState());
        helper.setBlock(5, 1, 4, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(4, 0, 4, Blocks.AIR.defaultBlockState());
        helper.setBlock(6, 0, 4, Blocks.AIR.defaultBlockState());
        helper.setBlock(5, 0, 3, Blocks.AIR.defaultBlockState());
        helper.setBlock(5, 0, 5, Blocks.AIR.defaultBlockState());

        final BlockPos west = at(helper, 4, 0, 4);
        final BlockPos east = at(helper, 6, 0, 4);
        final AIOpenDoor[] goal = new AIOpenDoor[1];
        final EntityGolemBase[] golems = new EntityGolemBase[1];
        helper.runAtTickTime(6, () -> {
            // The golem starts west of the doorway and paths to the cell east
            // of it, so the door is a node on its path.
            golems[0] = spawn(helper.getLevel(), west, EnumGolemType.WOOD, CORE_USE);
            assertAlive(golems[0]);
            goal[0] = new AIOpenDoor(golems[0], true);
        });

        final AtomicBoolean opened = new AtomicBoolean(false);
        final AtomicBoolean activated = new AtomicBoolean(false);
        final AtomicBoolean closedBehind = new AtomicBoolean(false);
        final AtomicBoolean pathing = new AtomicBoolean(false);
        final int[] stops = new int[1];
        final boolean[] running = new boolean[1];
        final StringBuilder stopTrace = new StringBuilder();
        final StringBuilder trace = new StringBuilder();
        drive(helper, SETTLE + 200, () -> {
            if (goal[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < MOB_SETTLE) {
                return;
            }
            if (helper.getBlockState(new BlockPos(5, 0, 4)).getValue(DoorBlock.OPEN)) {
                opened.set(true);
            }
            if (golem.tickCount % 20 == 0 && trace.length() < 1400) {
                trace.append("tc=").append(golem.tickCount).append(" feet=").append(golem.blockPosition())
                        .append(" path=").append(golem.getNavigation().getPath() == null
                                ? "null" : golem.getNavigation().getPath().getNodeCount())
                        .append(" done=").append(golem.getNavigation().isDone())
                        .append(" open=").append(helper.getBlockState(new BlockPos(5, 0, 4)).getValue(DoorBlock.OPEN))
                        .append(" act=").append(activated.get())
                        .append(" spd=").append(golem.getSpeed())
                        .append(" mv=").append(golem.getDeltaMovement())
                        .append(" grnd=").append(golem.onGround())
                        .append(" node=").append(golem.getNavigation().getPath() == null
                                ? -1 : golem.getNavigation().getPath().getNextNodeIndex())
                        .append(" pos=").append(String.format("[%.1f,%.1f,%.1f]", golem.getX(), golem.getY(), golem.getZ()))
                        .append(" | ");
            }
            // AIDoorInteract.canUse() only fires while a path is in progress
            // (AIDoorInteract.java:85-86, the path-node scan) — the way the
            // goal is reached in play: a task goal walks the golem somewhere
            // and the door is on the way. The path is re-issued until the goal
            // takes over, so a finished path (isDone) never closes the window.
            // GoalSelector's loop, and the path is re-issued while the goal is
            // idle: AIDoorInteract.canUse() only fires while a path is in
            // progress (AIDoorInteract.java:85-86, the path-node scan), which is
            // how the goal is reached in play — a task goal walks the golem and
            // the door is on the way.
            if (running[0]) {
                if (goal[0].canContinueToUse()) {
                    goal[0].tick();
                } else {
                    // The goal's close-behind contract: stop() (closeDoor=true)
                    // shuts both halves (AIOpenDoor.java:84-96). Sample in this
                    // same round: the navigation re-opens any door still on the
                    // path (vanilla PathNavigation.doOpenDoors), which is not
                    // this goal's behaviour.
                    goal[0].stop();
                    running[0] = false;
                    stops[0]++;
                    final BlockState lower = helper.getBlockState(new BlockPos(5, 0, 4));
                    final BlockState upper = helper.getBlockState(new BlockPos(5, 1, 4));
                    if (stops[0] <= 8) {
                        stopTrace.append("#").append(stops[0]).append("(tc=").append(golem.tickCount)
                                .append(") L=").append(lower).append(open(lower)).append(" U=").append(upper)
                                .append(open(upper)).append(" | ");
                    }
                    if (opened.get()
                            && lower.is(Blocks.OAK_DOOR) && !lower.getValue(DoorBlock.OPEN)
                            && upper.is(Blocks.OAK_DOOR) && !upper.getValue(DoorBlock.OPEN)) {
                        closedBehind.set(true);
                    }
                }
            } else {
                // Absolute world coordinates: the navigator is not translated.
                golem.getNavigation().moveTo(east.getX() + 0.5D, east.getY(), east.getZ() + 0.5D,
                        golem.getSpeed());
                pathing.set(true);
                if (goal[0].canUse()) {
                    activated.set(true);
                    goal[0].start();
                    running[0] = true;
                }
            }
        });

        helper.runAfterDelay(SETTLE + 200, () -> {
            clearGolemCaches();
            assertAlive(golems[0]);
            helper.assertTrue(activated.get(),
                    "the door goal never activated at the doorway " + doorway + " (" + state(golems[0])
                            + " spawn=" + at(helper, 4, 0, 4) + " trace=" + trace + ")");
            helper.assertTrue(opened.get(),
                    "the golem never opened the oak door at " + doorway + " (" + state(golems[0])
                            + " trace=" + trace + ")");
            helper.assertTrue(closedBehind.get(),
                    "the door goal's stop() left the oak door open at " + doorway + " (" + state(golems[0])
                            + " stops=" + stops[0] + " stopTrace=" + stopTrace + " trace=" + trace + ")");
            passed("interact_golemopensarealdoorandclosesitbehindit");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------
    // 6. Return home: the 48-block leash brings the golem back
    // (EntityGolemBase.java:1110-1127, AIReturnHome; no @GameTest in the original)
    //
    // Deterministic design: the probe never asserts a tick phase. It gates on the
    // golem's own tickCount (MOB_SETTLE), drives AIReturnHome exactly as the goal
    // selector drives a goal (one start(), then tick() while canContinueToUse, stop()
    // when it turns false), and states both success bounds in the units the product
    // uses — the goal's 3.0 squared-block threshold and the entity's 2304 squared-block
    // leash. The only window is the WALK_BUDGET on the stray leg, sized from the
    // measured walk (see the constant), with the test watchdog above it.
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = SETTLE + WALK_BUDGET + 120)
    public static void interact_returnHomeUsesTheHomeRadiusAndTheEntityTeleportsFromBeyond48(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 4, 0, 12);
        // Phase 1 drop-off: 8 blocks from home — past AIReturnHome's own
        // 3-block threshold (AIReturnHome.java:76) and well inside the entity's
        // 48-block leash teleport (EntityGolemBase.java, the 2304.0 gate), so the goal
        // is the only thing that can bring the golem back.
        final BlockPos strayedCell = at(helper, 4, 0, 4);
        helper.assertTrue(home.distSqr(strayedCell) > HOME_THRESHOLD_SQUARED
                        && home.distSqr(strayedCell) < LEASH_SQUARED,
                "the phase-1 drop-off is not between the goal's " + HOME_THRESHOLD_SQUARED
                        + " squared-block threshold and the entity's " + LEASH_SQUARED
                        + " squared-block leash");
        // Phase 2 drop-off: 56 blocks, past the 48-block leash. That is outside
        // the 24x24x24 template, so force-load the chunk and floor it.
        final BlockPos beyond = at(helper, 4, 0, 4 - 56);
        level.setChunkForced(beyond.getX() >> 4, beyond.getZ() >> 4, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(beyond.below().offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        helper.assertTrue(home.distSqr(beyond) >= LEASH_SQUARED,
                "the probe's phase-2 drop-off cell is not past the entity's " + LEASH_SQUARED
                        + " squared-block leash (48 blocks)");

        final EntityGolemBase[] golems = new EntityGolemBase[1];
        final AIReturnHome[] goal = new AIReturnHome[1];
        final int[] observed = new int[1];
        helper.runAtTickTime(6, () -> {
            final EntityGolemBase golem = spawn(level, home, EnumGolemType.WOOD, CORE_USE);
            assertAlive(golem);
            golems[0] = golem;
            goal[0] = new AIReturnHome(golem);
            observed[0] = golem.tickCount; // phase baseline, read once, never asserted
        });

        final int[] phase = { 0 };
        final boolean[] running = { false };
        final AtomicInteger goalUses = new AtomicInteger(0);
        final AtomicBoolean arrivedWithGoal = new AtomicBoolean(false);
        final AtomicBoolean leashedHome = new AtomicBoolean(false);
        final StringBuilder trace = new StringBuilder();
        drive(helper, SETTLE + WALK_BUDGET, () -> {
            if (golems[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < observed[0] + MOB_SETTLE) {
                return;
            }
            if (phase[0] == 0) {
                // Stray the way a task-driven golem strays: walk out of the
                // home radius, then let the goal act.
                golem.setPos(strayedCell.getX() + 0.5D, strayedCell.getY(), strayedCell.getZ() + 0.5D);
                golem.setDeltaMovement(Vec3.ZERO);
                golem.fallDistance = 0.0F;
                phase[0] = 1;
                return;
            }
            final double homeDistance =
                    golem.distanceToSqr(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D);
            if (phase[0] == 1) {
                // Drive the goal the way the goal selector drives it (GoalSelector.tick): one
                // start() on canUse, then tick() while canContinueToUse holds, stop() when it
                // turns false. The first version of this probe called start() on every tick of an
                // open gate, and start() re-issues moveTo, which rebuilds the path every tick: the
                // 8-block walk then measured anywhere from 30 to 315 ticks (WALK_BUDGET), which is
                // the axis that reddened the leg. The goal's own cadence is the 20-tick attempt
                // ladder of AIReturnHome (count, prevX/Y/Z, the stuck escape at :102-120).
                if (!running[0] && goal[0].canUse()) {
                    goalUses.incrementAndGet();
                    goal[0].start();
                    running[0] = true;
                }
                if (running[0]) {
                    if (goal[0].canContinueToUse()) {
                        goal[0].tick();
                    } else {
                        goal[0].stop();
                        running[0] = false;
                    }
                }
                if (goalUses.get() >= 1 && homeDistance < HOME_THRESHOLD_SQUARED) {
                    arrivedWithGoal.set(true);
                    // Second phase: past the leash radius, where only the entity's own teleport
                    // can bring it back — the 2304.0 gate on getRestrictCenter() in
                    // EntityGolemBase.tick() (:1362), and the fixture set that centre to the home
                    // cell.
                    golem.setPos(beyond.getX() + 0.5D, beyond.getY(), beyond.getZ() + 0.5D);
                    golem.setDeltaMovement(Vec3.ZERO);
                    golem.fallDistance = 0.0F;
                    phase[0] = 2;
                    return;
                }
            } else if (phase[0] == 2) {
                // The teleport lands the golem in the passable cell the leash scan finds at the
                // home centre, so success is the goal's own arrival threshold, not a wider ring.
                if (golem.blockPosition().distSqr(home) < HOME_THRESHOLD_SQUARED) {
                    leashedHome.set(true);
                }
            }
            if (trace.length() < 700) {
                trace.append("p").append(phase[0]).append(" tc=").append(golem.tickCount)
                        .append(" feet=").append(golem.blockPosition())
                        .append(" d2=").append((long) homeDistance)
                        .append(" uses=").append(goalUses.get()).append(" | ");
            }
        });

        helper.runAfterDelay(SETTLE + WALK_BUDGET + 40, () -> {
            clearGolemCaches();
            assertAlive(golems[0]);
            final EntityGolemBase golem = golems[0];
            helper.assertTrue(phase[0] >= 1, "the probe never moved the golem out of the home radius");
            helper.assertTrue(goalUses.get() >= 1,
                    "AIReturnHome never activated with the golem outside its home radius (the goal's"
                            + " gate is " + HOME_THRESHOLD_SQUARED + " squared blocks; " + state(golem)
                            + " strayedCell=" + strayedCell + " trace=" + trace + ")");
            helper.assertTrue(arrivedWithGoal.get(),
                    "the golem did not walk back inside the goal's own threshold of "
                            + HOME_THRESHOLD_SQUARED + " squared blocks from home within the walk"
                            + " budget of " + WALK_BUDGET + " ticks (feet=" + golem.blockPosition()
                            + ", home=" + home + ", squared distance="
                            + (long) golem.distanceToSqr(home.getX() + 0.5D, home.getY() + 0.5D,
                                    home.getZ() + 0.5D)
                            + ", uses=" + goalUses.get() + ", trace=" + trace + ", " + state(golem) + ")");
            helper.assertTrue(leashedHome.get(),
                    "the entity never came back from beyond its own leash of " + LEASH_SQUARED
                            + " squared blocks (48 blocks; feet=" + golem.blockPosition() + ", home="
                            + home + ", beyond=" + beyond + ", trace=" + trace + ", " + state(golem) + ")");
            passed("interact_returnHomeUsesTheHomeRadiusAndTheEntityTeleportsFromBeyond48");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------
    // 7. Gate: a goal is inert for a golem whose core is not its own
    // (setupGolem installs each goal in exactly one core case, EntityGolemBase.java:554-683)
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 520)
    public static void interact_harvestGoalsAreInertForTheWrongCore(final GameTestHelper helper) {
        clearGolemCaches();
        // Ripe wheat in the core-7 golem's range, logs in the core-3 golem's
        // range, and a marked log target in the core-less golem's range: every
        // goal must refuse the golem that is not its own core, and the world
        // must be unchanged after a full run of the core task.
        helper.setBlock(4, 0, 4, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(4, 1, 4, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(12, 0, 4, Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(12, 1, 4, Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(20, 0, 4, Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(20, -1, 4, Blocks.STONE.defaultBlockState());
        helper.setBlock(21, 0, 4, Blocks.OAK_LOG.defaultBlockState());

        final AIHarvestCrops[] cropsOnChopCore = new AIHarvestCrops[1];
        final AIHarvestLogs[] logsOnHarvestCore = new AIHarvestLogs[1];
        final AIUseItem[] useOnCoreless = new AIUseItem[1];
        final EntityGolemBase[] golems = new EntityGolemBase[3];
        final BlockPos[] homes = { at(helper, 3, 0, 4), at(helper, 10, 0, 4), at(helper, 19, 0, 4) };
        helper.runAtTickTime(6, () -> {
            golems[0] = spawn(helper.getLevel(), homes[0], EnumGolemType.WOOD, CORE_CHOP);
            assertAlive(golems[0]);
            cropsOnChopCore[0] = new AIHarvestCrops(golems[0]);
            golems[1] = spawn(helper.getLevel(), homes[1], EnumGolemType.WOOD, CORE_HARVEST);
            assertAlive(golems[1]);
            logsOnHarvestCore[0] = new AIHarvestLogs(golems[1]);
            golems[2] = spawn(helper.getLevel(), homes[2], EnumGolemType.WOOD, CORE_NONE);
            assertAlive(golems[2]);
            useOnCoreless[0] = new AIUseItem(golems[2]);
            golems[2].setCarried(new ItemStack(Blocks.OAK_LOG));
            mark(golems[2], at(helper, 20, 0, 4), Direction.UP);
        });

        drive(helper, SETTLE + 120, () -> {
            for (final EntityGolemBase golem : golems) {
                assertAlive(golem);
                if (golem.tickCount >= MOB_SETTLE) {
                    golem.runCoreTaskStep();
                }
            }
        });

        helper.runAfterDelay(SETTLE + 120, () -> {
            clearGolemCaches();
            for (final EntityGolemBase golem : golems) {
                assertAlive(golem);
            }
            // 1. The gate: each goal refuses a golem whose core is not its own.
            helper.assertTrue(!cropsOnChopCore[0].canUse(),
                    "the core-3 harvest goal activated on the core-7 (chop) golem: " + state(golems[0]));
            helper.assertTrue(!logsOnHarvestCore[0].canUse(),
                    "the core-7 chop goal activated on the core-3 (harvest) golem: " + state(golems[1]));
            helper.assertTrue(!useOnCoreless[0].canUse(),
                    "the core-8 use goal activated on the core-less golem: " + state(golems[2]));
            // 2. The effect: a full run of the core task changed nothing.
            helper.assertBlockProperty(new BlockPos(4, 1, 4), CropBlock.AGE, 7);
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 0, 4)).is(Blocks.OAK_LOG)
                    && helper.getBlockState(new BlockPos(12, 1, 4)).is(Blocks.OAK_LOG),
                    "the wrong-core run chopped the log column in the harvest golem's range");
            helper.assertTrue(helper.getBlockState(new BlockPos(20, 0, 4)).is(Blocks.OAK_LOG)
                    && helper.getBlockState(new BlockPos(21, 0, 4)).is(Blocks.OAK_LOG),
                    "the wrong-core run touched the marked target column");
            passed("interact_harvestGoalsAreInertForTheWrongCore");
            helper.succeed();
        });
    }
}
