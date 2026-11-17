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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
     * The crop fixture lands here, NOT at setup: the section the pad sits in relights from
     * queued data, and the engine applies the sky channel before the block channel has
     * propagated, so a crop planted at setup can be popped by the first neighbour update
     * inside that window (measured: 7 of 8 cells were Block{air} on the drive's first draw).
     */
    private static final int PLANT_TICK = 40;

    /**
     * Ticks the pad must be quiet before the drive's light gate runs: a glowstone the pad
     * had to re-place lands in the light engine's queue, and the raw read is the propagated
     * value only some ticks later (the same queueing that pops a crop planted at setup).
     */
    private static final int LIGHT_SETTLE = 10;

    /**
     * Sentinel for a crop cell the probe can no longer read as a crop. The age census
     * classifies the {@link BlockState} first and only then asks for
     * {@code CropBlock.AGE}: that property does not exist on {@code Block{minecraft:air}},
     * so the draws that feed the outcome asserts can never crash on a popped cell.
     */
    private static final int VANISHED = -2;

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
    //  The harvest pads: what the crop fixtures own, and who services it
    // ------------------------------------------------------------------

    /**
     * What a pad service call is asked to do. {@link #CLAIM} clears foreign blocks out
     * of the fixture's cells and pays the light, and plants NOTHING (it runs at setup,
     * before the fixture, so it can never eat a fixture — the rule at
     * docs/testing.md#sweep-hazard-set-is-scan-set: "Claim first, fixtures after").
     * {@link #PLANT} is the fixture itself (wet soil + crop at the pad's age).
     * {@link #SERVICE} keeps the light, the soil and the headroom paid during the drive
     * window and never touches a crop: a ripe crop the goal has just chopped must stay
     * chopped for the probe's own draws, so the crop arm belongs to the probe, not to
     * the pad.
     */
    private enum PadMode { CLAIM, PLANT, SERVICE }

    /**
     * The block cells a harvest crop patch owns: per crop column the source at
     * {@code y=-1}, the soil at {@code y=0}, the crop at {@code y=1} and its headroom at
     * {@code y=2}, plus the golem's walk cells at {@code y=0..2}. The counters keep every
     * write the pad made on the record, and {@link #lastWriteTick} says when the pad last
     * wrote, because a freshly placed light source needs ticks to propagate.
     *
     * <p><b>Why a claim is needed at all: a pad's BLOCK space is handed over DIRTY.</b>
     * The harness's 1x1x1 pads stride 6 cells ({@code StructureGridSpawner}: stride =
     * {@code template width + 5} / {@code depth + 6}), sixteen of them pack inside this
     * 24-wide footprint, their bodies run in the SAME tick as this probe's, and
     * {@code StructureUtils#clearSpaceForStructure} clears BLOCK space only — a
     * neighbour's {@code setBlock} lands on a scripted cell of a big pad. This lane's
     * scripted cells include the crop's LIGHT SOURCE, so the wave does not merely block a
     * walk cell: it deletes the light the fixture paid for. {@code CropBlock#canSurvive}
     * (CropBlock.java:164-171) is the {@code getRawBrightness(pos, 0) >= 8} gate, and
     * {@code BushBlock#updateShape} (BushBlock.java:30-35) pops the crop to
     * {@code Block{air}} on the FIRST neighbour update that fails it — and the wave's own
     * {@code setBlock} IS that neighbour update.</p>
     *
     * <p>Measured on the 529-probe grid at sha {@code 2eda098b}, CI run 43788415254
     * (job 133041135996, {@code -Ptc.noCurios}, attempt 1): the plant tick printed
     * {@code raw=0} in six of the eight crop cells — the glowstone under them gone and
     * the sky channel roofed — while the two cells whose source survived printed
     * {@code raw=13}, exactly the fixture's own depth-2 block channel (15 − 1 farmland
     * − 1). The drive's gate tick printed {@code repaired=5 ... raw=10 raw=9 raw=8 raw=7
     * raw=9} and reddened on cell {@code (5, 1, 6)} at raw 7. The sibling sample at the
     * same sha (land-ref run 43786472558, job 133035144147) printed {@code raw=15} in all
     * eight cells and never named this probe: that leg was handed a clean pad. So the
     * lane's rate is the handoff, and the cells the handoff dirties are the cells this
     * pad now claims (docs/testing.md#harvest-pad-claims-its-light-layer).</p>
     */
    private static final class HarvestPad {

        /** Crop columns as {@code {x, z}}; the pad owns them at y=-1..2. */
        private final int[][] crops;

        /** The golem's walk columns as {@code {x, z}}; owned at y=0..2 only. */
        private final int[][] walk;

        /** The age the fixture plants: 7 = ripe (the reap probe), 2 = unripe. */
        private final int age;

        private final AtomicInteger cleared = new AtomicInteger();
        private final AtomicInteger relit = new AtomicInteger();
        private final AtomicInteger soils = new AtomicInteger();
        private final AtomicInteger planted = new AtomicInteger();

        /** The last test tick on which the pad wrote a block (light needs ticks to spread). */
        private final AtomicLong lastWriteTick = new AtomicLong(Long.MIN_VALUE / 4);

        HarvestPad(final int[][] crops, final int[][] walk, final int age) {
            this.crops = crops;
            this.walk = walk;
            this.age = age;
        }

        /** One-line census for an assert message. */
        String census() {
            return "cleared=" + cleared.get() + " relit=" + relit.get() + " soil=" + soils.get()
                    + " planted=" + planted.get() + " lastWriteTick=" + lastWriteTick.get();
        }
    }

    /**
     * Claim / plant / service a harvest pad, and PRINT every write. The light layer is
     * the arm that matters: one glowstone directly under every crop cell keeps the
     * block channel at 13 in the crop cell permanently, so
     * {@code max(block, sky - skyDarken)} can never fall under the {@code >= 8} gate a
     * crop's {@code canSurvive} consults (docs/testing.md#crop-light-propagation-depth).
     * A foreign block in the source cell, the soil cell, the crop cell or the headroom
     * cell is named in the leg's log line, so a green leg says the pad was handed over
     * clean and a red leg says what arrived.
     */
    private static void serviceHarvestPad(final GameTestHelper helper, final HarvestPad pad,
                                          final PadMode mode) {
        final long tick = helper.getTick();
        final List<String> writes = new ArrayList<>();
        int cleared = 0;
        int relit = 0;
        int soils = 0;
        int planted = 0;
        for (final int[] cell : pad.crops) {
            final int x = cell[0];
            final int z = cell[1];
            // 1. Headroom: the crop cell must not be roofed. A block at y=2 kills the sky
            //    channel, and a cell whose source is gone has nothing left above 8.
            final BlockState head = helper.getBlockState(new BlockPos(x, 2, z));
            if (!head.isAir()) {
                helper.setBlock(x, 2, z, Blocks.AIR.defaultBlockState());
                writes.add(head + " @ rel (" + x + ", 2, " + z + ")");
                cleared++;
            }
            // 2. CLAIM also strips foreign blocks off the soil and crop cells; the fixture
            //    owns those cells and its writes come after the claim.
            if (mode == PadMode.CLAIM) {
                for (final int y : new int[] { 1, 0 }) {
                    final BlockState foreign = helper.getBlockState(new BlockPos(x, y, z));
                    if (!foreign.isAir()) {
                        helper.setBlock(x, y, z, Blocks.AIR.defaultBlockState());
                        writes.add(foreign + " @ rel (" + x + ", " + y + ", " + z + ")");
                        cleared++;
                    }
                }
            }
            // 3. The light the fixture paid for: y=-1, two steps under the crop, through
            //    the farmland (lightBlock 1) into the crop cell (lightBlock 0) = 13.
            final BlockState source = helper.getBlockState(new BlockPos(x, -1, z));
            if (!source.is(Blocks.GLOWSTONE)) {
                helper.setBlock(x, -1, z, Blocks.GLOWSTONE.defaultBlockState());
                writes.add("source " + source + " -> glowstone @ rel (" + x + ", -1, " + z + ")");
                relit++;
            }
            // 4. The soil: a crop above holds farmland via MAINTAINS_FARMLAND
            //    (FarmBlock.java:124-126) and FarmBlock.java:95-104 dries DRY farmland to
            //    dirt, so the pad pays it wet.
            final BlockState ground = helper.getBlockState(new BlockPos(x, 0, z));
            if (!ground.is(Blocks.FARMLAND)) {
                helper.setBlock(x, 0, z, Blocks.FARMLAND.defaultBlockState()
                        .setValue(BlockStateProperties.MOISTURE, 7));
                writes.add("soil " + ground + " -> farmland[moisture=7] @ rel (" + x + ", 0, " + z + ")");
                soils++;
            }
            // 5. The crop. PLANT plants the fixture; SERVICE touches the cell only where
            //    contamination is PROVEN — a foreign block sitting in the crop cell (the
            //    wave's own write, named in the census) or a source the pad had to re-place
            //    this tick — and restores the crop that write destroyed. Planting every tick
            //    would fake a reap the goal never did: the probe's reap arm needs a
            //    7 -> 0 transition, which only the goal's own replant produces, so a ripe
            //    crop planted here can never inflate it.
            final BlockState here = helper.getBlockState(new BlockPos(x, 1, z));
            if (mode == PadMode.PLANT && !here.hasProperty(CropBlock.AGE)) {
                helper.setBlock(x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, pad.age));
                writes.add("planted wheat[age=" + pad.age + "] @ rel (" + x + ", 1, " + z + ")");
                planted++;
            } else if (mode == PadMode.SERVICE && !here.isAir() && !here.hasProperty(CropBlock.AGE)) {
                helper.setBlock(x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, pad.age));
                writes.add("crop-repair cleared " + here + " -> wheat[age=" + pad.age + "] @ rel ("
                        + x + ", 1, " + z + ")");
                cleared++;
                planted++;
            } else if (mode == PadMode.SERVICE && relit > 0 && !here.hasProperty(CropBlock.AGE)) {
                helper.setBlock(x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, pad.age));
                writes.add("crop-repair " + here + " -> wheat[age=" + pad.age + "] @ rel ("
                        + x + ", 1, " + z + ")");
                planted++;
            } else if (mode == PadMode.CLAIM && !here.isAir() && !here.hasProperty(CropBlock.AGE)) {
                helper.setBlock(x, 1, z, Blocks.AIR.defaultBlockState());
                writes.add(here + " @ rel (" + x + ", 1, " + z + ")");
                cleared++;
            }
        }
        for (final int[] cell : pad.walk) {
            for (int y = 0; y <= 2; y++) {
                final BlockState foreign = helper.getBlockState(new BlockPos(cell[0], y, cell[1]));
                if (!foreign.isAir()) {
                    helper.setBlock(cell[0], y, cell[1], Blocks.AIR.defaultBlockState());
                    writes.add(foreign + " @ walk rel (" + cell[0] + ", " + y + ", " + cell[1] + ")");
                    cleared++;
                }
            }
        }
        if (!writes.isEmpty()) {
            pad.lastWriteTick.set(tick);
            pad.cleared.addAndGet(cleared);
            pad.relit.addAndGet(relit);
            pad.soils.addAndGet(soils);
            pad.planted.addAndGet(planted);
            System.out.println("HARVEST-PAD " + mode + " tick=" + tick + " cleared=" + cleared
                    + " relit=" + relit + " soil=" + soils + " planted=" + planted + " ["
                    + String.join(", ", writes) + "]");
        }
    }

    /**
     * The survival gate {@code CropBlock} itself consults, asserted on the drive's first
     * QUIET tick — a tick at least {@code LIGHT_SETTLE} ticks after the pad last wrote, so
     * a glowstone the pad had to re-place has had ticks to propagate — and in any case by
     * {@code SETTLE + 60}, where a pad under active attack reddens with its census naming
     * the source cell. The threshold is the fixture's own block channel (13), not the
     * vanilla floor (8): a cell reading between 8 and 13 has lost the source the fixture
     * paid for and is running on a neighbour's light.
     */
    private static void assertHarvestPadLight(final GameTestHelper helper, final HarvestPad pad,
                                              final int lightSettle) {
        for (final int[] cell : pad.crops) {
            final BlockPos crop = new BlockPos(cell[0], 1, cell[1]);
            final BlockPos abs = helper.absolutePos(crop);
            final int raw = helper.getLevel().getRawBrightness(abs, 0);
            helper.assertTrue(raw >= 13,
                    "the crop patch sits under CropBlock#hasSufficientLight"
                            + " (getRawBrightness < 13, the fixture's own depth-2 block channel: a"
                            + " glowstone at y=-1 under the soil, 15 - 1 farmland - 1 = 13) at " + crop
                            + " (abs " + abs + ", state " + helper.getLevel().getBlockState(abs)
                            + ", below " + helper.getLevel().getBlockState(abs.below())
                            + ", source " + helper.getLevel().getBlockState(abs.below(2))
                            + ", raw=" + raw + ", tick " + helper.getTick() + ", pad " + pad.census()
                            + "); BushBlock#updateShape pops such a crop to Block{air} on the next"
                            + " neighbour update (docs/testing.md#harvest-pad-claims-its-light-layer)");
        }
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
            crops[i] = new BlockPos(cells[i][0], 1, cells[i][1]);
        }
        // FIXTURE LIGHT, paid at setup, BEFORE the crop exists
        // (docs/testing.md#crop-light-propagation-depth). CropBlock#canSurvive
        // (CropBlock.java:164-171) is the light gate (hasSufficientLight:
        // getRawBrightness(pos, 0) >= 8) AND the soil gate, and BushBlock#updateShape
        // (BushBlock.java:30-35) turns the crop into Block{air} on the FIRST neighbour
        // update whose canSurvive fails. The section the pad sits in carries QUEUED light
        // data, and the engine relights the SKY channel before the block channel has
        // propagated to the far cells: in the dark half of the world clock that window is
        // raw = 15 - skyDarken(11) = 4, so a crop planted at setup pops on the first
        // neighbour update inside it and never comes back. Measured on the 515-probe grid:
        // the drive's FIRST draw found 7 of 8 cells already Block{air} with intact farmland
        // below and raw 10-12 read AFTER the fact — the block channel had arrived late
        // (15 - Manhattan), exactly the propagation-depth signature: the cells at depth 4-5
        // from the single source died, the one at depth 2 survived.
        // So the probe pays for light with a SHORT propagation depth: one source directly
        // under every crop cell (two steps to the crop, through the farmland) plus one above
        // the middle. The block channel then stands at 13-15 in every cell, permanently, and
        // max(block, sky - skyDarken) can never fall under the >= 8 gate — the sky transient
        // has nothing left to take away. Sources sit BELOW the soil, so the golem's walk
        // cells and the crops' headroom are untouched.
        // The pad claims its own cells BEFORE the fixture goes in, and keeps them paid for
        // the whole drive window: the grid hands BLOCK space over dirty (a neighbour pad's
        // write lands in these cells) and the cells this fixture owns include the crop's
        // light source, so an unclaimed handoff deletes the light the crop needs and
        // BushBlock#updateShape pops it. HarvestPad's javadoc carries the measured CI pair.
        final HarvestPad pad = new HarvestPad(cells, new int[][] { { 4, 4 } }, 7);
        serviceHarvestPad(helper, pad, PadMode.CLAIM);
        helper.setBlock(4, 3, 4, Blocks.GLOWSTONE.defaultBlockState());
        // The crop lands only after that light has settled: planting at setup plants into
        // the unlit window, which is where the pop comes from.
        helper.runAtTickTime(PLANT_TICK, () -> {
            serviceHarvestPad(helper, pad, PadMode.PLANT);
            final StringBuilder lit = new StringBuilder();
            for (int i = 0; i < crops.length; i++) {
                lit.append(" [").append(cells[i][0]).append(',').append(cells[i][1])
                        .append("] raw=")
                        .append(helper.getLevel().getRawBrightness(
                                helper.absolutePos(crops[i]), 0));
            }
            System.out.println("HARVEST-FIXTURE plant tick=" + helper.getTick() + ":" + lit
                    + " pad " + pad.census());
        });

        final AIHarvestCrops[] goal = new AIHarvestCrops[1];
        final EntityGolemBase[] golems = new EntityGolemBase[1];
        helper.runAtTickTime(6, () -> {
            golems[0] = spawn(helper.getLevel(), home, EnumGolemType.WOOD, CORE_HARVEST);
            assertAlive(golems[0]);
            goal[0] = new AIHarvestCrops(golems[0]);
        });

        final AtomicInteger harvested = new AtomicInteger(0);
        final AtomicBoolean vanished = new AtomicBoolean(false);
        final AtomicBoolean gateChecked = new AtomicBoolean(false);
        final AtomicInteger repaired = new AtomicInteger(0);
        final int[] ages = new int[crops.length];
        drive(helper, SETTLE + 460, () -> {
            if (goal[0] == null) {
                return;
            }
            final EntityGolemBase golem = require(golems, 0);
            if (golem.tickCount < MOB_SETTLE) {
                return;
            }
            serviceHarvestPad(helper, pad, PadMode.SERVICE);
            // The light gate runs on the drive's first QUIET tick: LIGHT_SETTLE ticks after
            // the pad last wrote, so a glowstone the pad had to re-place has had ticks to
            // propagate. In any case it runs by SETTLE + 60, where a pad under active attack
            // reddens with its census naming the source cell the grid ate.
            if (!gateChecked.get()
                    && (helper.getTick() - pad.lastWriteTick.get() >= LIGHT_SETTLE
                            || helper.getTick() >= SETTLE + 60)) {
                gateChecked.set(true);
                // The fixture owns these cells. If the settle window's light transient (or a
                // neighbour pad's block write) popped a cell before the drive opened, restore
                // the fixture and PRINT it — the drive's draws then measure the harvest goal's
                // work on a crop patch that exists, and a repaired cell is on the record. A pop
                // AFTER this tick is a real observation and stays a red arm below.
                final StringBuilder healed = new StringBuilder();
                for (int i = 0; i < crops.length; i++) {
                    final BlockPos abs = helper.absolutePos(crops[i]);
                    final BlockState here = helper.getLevel().getBlockState(abs);
                    if (here.hasProperty(CropBlock.AGE)) {
                        continue;
                    }
                    healed.append(" cell[").append(i).append("]=").append(crops[i])
                            .append(" was ").append(here).append(" below=")
                            .append(helper.getLevel().getBlockState(abs.below())).append(" raw=")
                            .append(helper.getLevel().getRawBrightness(abs, 0));
                    if (!helper.getLevel().getBlockState(abs.below()).is(Blocks.FARMLAND)) {
                        helper.setBlock(crops[i].getX(), 0, crops[i].getZ(),
                                Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                    }
                    helper.setBlock(crops[i].getX(), crops[i].getY(), crops[i].getZ(),
                            Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
                    repaired.incrementAndGet();
                    ages[i] = 7;
                }
                if (repaired.get() > 0) {
                    System.out.println("HARVEST-FIXTURE repaired=" + repaired.get()
                            + " at tick " + helper.getTick() + ":" + healed + " pad "
                            + pad.census());
                }
                // The survival gate CropBlock itself consults, asserted on the drive's first
                // quiet tick — before any draw depends on it — in the probe's own message,
                // at the fixture's own depth-2 block channel (13), not the vanilla floor (8).
                assertHarvestPadLight(helper, pad, LIGHT_SETTLE);
            }
            for (int i = 0; i < crops.length; i++) {
                // Classify the state, then read the property: IntegerProperty{name=age}
                // does not exist on Block{minecraft:air}, and asking for it there is the
                // disclosed crash (CI run 43675329931, job 132694099034, tip e50113cc).
                final BlockState state = helper.getBlockState(crops[i]);
                final int age = state.hasProperty(CropBlock.AGE) ? state.getValue(CropBlock.AGE) : VANISHED;
                if (age == VANISHED) {
                    vanished.set(true);
                }
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

        // The arm ends on the OBSERVED outcome, not on a tick budget: every draw classifies
        // the cell, and the poll asserts reaped >= 3 and replanted >= 3 off those draws.
        // A criterion that returns is success, so the asserts throw to keep polling — and the
        // two hard arms (a vanished cell, a deadline) throw a plain exception, which the
        // reporter prints as `<probe> failed at <coords>! <census>` instead of a bare
        // watchdog. No fixed verdict tick is asserted on.
        helper.succeedWhen(() -> {
            if (golems[0] == null || goal[0] == null || helper.getTick() < SETTLE) {
                throw new GameTestAssertException("the harvest drive is not open at tick "
                        + helper.getTick() + " (golem spawns at tick 6, drive opens at " + SETTLE + ")");
            }
            final EntityGolemBase golem = golems[0];
            int replanted = 0;
            int grown = 0;
            int vanishedCells = 0;
            for (final int age : ages) {
                if (age == VANISHED) {
                    vanishedCells++;
                } else if (age == 0) {
                    replanted++;
                } else if (age > 0) {
                    grown++;
                }
            }
            final int reaped = harvested.get();
            final String census = "harvested=" + reaped + ", replanted=" + replanted + ", stillRipe="
                    + grown + ", vanished=" + vanishedCells + " of " + crops.length + " cells, repaired="
                    + repaired.get() + "; " + state(golem);
            if (vanished.get() || vanishedCells > 0) {
                final StringBuilder lost = new StringBuilder();
                for (int i = 0; i < crops.length; i++) {
                    if (ages[i] == VANISHED) {
                        final BlockPos abs = helper.absolutePos(crops[i]);
                        lost.append(" cell[").append(i).append("]=").append(crops[i]).append(" (abs ")
                                .append(abs).append(") state=").append(helper.getLevel().getBlockState(abs))
                                .append(" below=").append(helper.getLevel().getBlockState(abs.below()))
                                .append(" raw=").append(helper.getLevel().getRawBrightness(abs, 0));
                    }
                }
                throw new IllegalStateException("the crop patch lost a cell to a non-crop state at tick "
                        + helper.getTick() + ": " + census + ";" + lost
                        + " — CropBlock#canSurvive (CropBlock.java:164-171) is the raw-brightness >= 8"
                        + " gate AND the soil gate, and BushBlock#updateShape (BushBlock.java:30-35) pops"
                        + " such a crop to Block{air} on the first neighbour update; AIHarvestCrops#harvest"
                        + " replants only when the cell still holds a crop at the chop tick, so a popped"
                        + " cell is never replanted and the goal parks on it. The probe pays for its"
                        + " light (glowstone at (4,3,4)) and asserts raw >= 8 on the drive's first tick.");
            }
            final boolean windowClosed = helper.getTick() >= SETTLE + 460 + 20;
            if (reaped < 3) {
                if (windowClosed) {
                    throw new IllegalStateException("the harvest goal never reaped the ripe wheat by the"
                            + " deadline at tick " + helper.getTick() + " (drive window closed at "
                            + (SETTLE + 460) + "): " + census);
                }
                throw new GameTestAssertException("waiting for the harvest goal to reap three crops: "
                        + census);
            }
            if (replanted < 3) {
                if (windowClosed) {
                    throw new IllegalStateException("the harvest goal reaped " + reaped
                            + " crops but replanted only " + replanted + " by the deadline at tick "
                            + helper.getTick() + " (the original sows a seed back onto the farmland): "
                            + census);
                }
                throw new GameTestAssertException("waiting for three crops to land as replanted"
                        + " (age 0) after the reap: " + census);
            }
            clearGolemCaches();
            assertAlive(golem);
            passed("interact_harvestCropsCoreReapsRipeWheatAndReplantsIt");
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
        // Same pad contract as the reap probe, and a harsher version of it: this crop is
        // light-gated for the whole 330-tick run, is asserted UNCHANGED at the end, and has
        // no light of its own at all — its raw 15 is the section's uncomputed sky value, so a
        // neighbour pad roofed over the cell pops it and the final assert reads Block{air}.
        // The pad claims the column, pays a source under the soil, and plants at PLANT_TICK
        // (the light engine needs ticks on a freshly placed source; docs/testing.md#
        // crop-light-propagation-depth).
        final HarvestPad pad = new HarvestPad(new int[][] { { 4, 5 } },
                new int[][] { { 4, 4 } }, 2);
        serviceHarvestPad(helper, pad, PadMode.CLAIM);
        helper.runAtTickTime(PLANT_TICK, () -> serviceHarvestPad(helper, pad, PadMode.PLANT));

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
            serviceHarvestPad(helper, pad, PadMode.SERVICE);
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
    // ------------------------------------------------------------------

    @GameTest(template = "empty_24", timeoutTicks = 700)
    public static void interact_returnHomeUsesTheHomeRadiusAndTheEntityTeleportsFromBeyond48(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 4, 0, 12);
        // Phase 1 drop-off: 8 blocks from home — past AIReturnHome's own
        // 3-block threshold (AIReturnHome.java:76) and well inside the entity's
        // 48-block leash teleport (EntityGolemBase.java:1110-1127), so the goal
        // is the only thing that can bring the golem back.
        final BlockPos strayedCell = at(helper, 4, 0, 4);
        helper.assertTrue(home.distSqr(strayedCell) > 3.0D && home.distSqr(strayedCell) < 2304.0D,
                "the phase-1 drop-off is not between the goal threshold and the leash radius");
        // Phase 2 drop-off: 56 blocks, past the 48-block leash. That is outside
        // the 24x24x24 template, so force-load the chunk and floor it.
        final BlockPos beyond = at(helper, 4, 0, 4 - 56);
        level.setChunkForced(beyond.getX() >> 4, beyond.getZ() >> 4, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(beyond.below().offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        helper.assertTrue(home.distSqr(beyond) >= 2304.0D,
                "the probe's phase-2 drop-off cell is not past the 48-block home radius");

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
        final AtomicInteger goalUses = new AtomicInteger(0);
        final AtomicBoolean arrivedWithGoal = new AtomicBoolean(false);
        final AtomicBoolean leashedHome = new AtomicBoolean(false);
        final StringBuilder trace = new StringBuilder();
        drive(helper, SETTLE + 320, () -> {
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
                if (goal[0].canUse()) {
                    goalUses.incrementAndGet();
                    goal[0].start();
                }
                if (goal[0].canContinueToUse()) {
                    goal[0].tick();
                } else {
                    goal[0].stop();
                }
                if (goalUses.get() >= 1 && homeDistance < 3.0D) {
                    arrivedWithGoal.set(true);
                    // Second phase: past the leash radius, where only the
                    // entity's own teleport can bring it back.
                    golem.setPos(beyond.getX() + 0.5D, beyond.getY(), beyond.getZ() + 0.5D);
                    golem.setDeltaMovement(Vec3.ZERO);
                    golem.fallDistance = 0.0F;
                    phase[0] = 2;
                    return;
                }
            } else if (phase[0] == 2) {
                if (golem.blockPosition().distSqr(home) < 256.0D) {
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

        helper.runAfterDelay(SETTLE + 320, () -> {
            clearGolemCaches();
            assertAlive(golems[0]);
            final EntityGolemBase golem = golems[0];
            helper.assertTrue(phase[0] >= 1, "the probe never moved the golem out of the home radius");
            helper.assertTrue(goalUses.get() >= 1,
                    "AIReturnHome never activated with the golem outside its home radius (" + state(golem)
                            + " strayedCell=" + strayedCell + " trace=" + trace + ")");
            helper.assertTrue(arrivedWithGoal.get(),
                    "the golem did not path back inside the goal's home threshold (feet="
                            + golem.blockPosition() + ", home=" + home + ", uses=" + goalUses.get()
                            + ", trace=" + trace + ", " + state(golem) + ")");
            helper.assertTrue(leashedHome.get(),
                    "the entity never came back from beyond the 48-block home radius (feet="
                            + golem.blockPosition() + ", home=" + home + ", beyond=" + beyond
                            + ", trace=" + trace + ", " + state(golem) + ")");
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
        // The wheat at (4, 1, 4) is asserted UNCHANGED at the end of a 310-tick run, so it is
        // a light-gated fixture too, with the same bare-sky exposure as probe 2's: claim the
        // column, pay the source under the soil, plant once the light has ticks to spread.
        final HarvestPad pad = new HarvestPad(new int[][] { { 4, 4 } },
                new int[][] { { 3, 4 }, { 10, 4 }, { 19, 4 } }, 7);
        serviceHarvestPad(helper, pad, PadMode.CLAIM);
        helper.runAtTickTime(PLANT_TICK, () -> serviceHarvestPad(helper, pad, PadMode.PLANT));
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
            serviceHarvestPad(helper, pad, PadMode.SERVICE);
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
