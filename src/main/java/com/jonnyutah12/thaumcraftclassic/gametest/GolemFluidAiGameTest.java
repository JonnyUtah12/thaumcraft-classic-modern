package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemHelper;
import com.jonnyutah12.thaumcraftclassic.entities.golems.Marker;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIEssentiaEmpty;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIEssentiaGather;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AIEssentiaGoto;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AILiquidEmpty;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AILiquidGather;
import com.jonnyutah12.thaumcraftclassic.entities.golems.ai.AILiquidGoto;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.tiles.TileJarFillable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * TC-M6-08-s-b2 probes: the six ported fluid/essentia golem goals move real
 * essentia and real fluid.
 *
 * <p>Each probe asserts <b>observable world state</b> — the essentia total and
 * stored aspect of a real {@link TileJarFillable}, the {@code LEVEL} of a real
 * NeoForge water cauldron, a real water source block, the cauldron's own
 * {@link IFluidHandler} simulation result — plus the golem's own
 * {@code essentiaAmount} / {@code getFluidCarried()} and the liquid goals'
 * shared watch. The goals are driven <b>directly</b> ({@code canUse()} →
 * {@code start()} → {@code tick()}) with the golem parked next to its target,
 * so no probe depends on the vanilla goal selector (no ticking state runs it)
 * or on pathfinding — the constraint {@code GolemRuntimeGameTest} documents for
 * the core tasks.</p>
 *
 * <h2>Geometry this class pins (a pitfall, measured)</h2>
 * The golem's feet go in the cell <b>adjacent</b> to the container, never in
 * the container's own cell: {@code homeContainerPos} is
 * {@code restrictCenter.relative(homeFacing.getOpposite())}
 * ({@code GolemHelper.java:126-129}), so with {@code HOME_FACING = NORTH} the
 * container sits at {@code z + 1} from the golem. A golem whose feet share a
 * cell with a solid block takes suffocation damage every tick and dies in ~20
 * ticks, which is what a "the golem died mid-test" line means. The container is
 * also outside the golem's 0.4 × 0.95 hitbox, so nothing in a probe is
 * occluded by the entity.
 *
 * <h2>Other pitfalls this class guards against</h2>
 * <ul>
 *   <li>{@code GolemHelper}'s jar list and item-timeout lists are static, so
 *       every probe <em>and every arm</em> clears them
 *       ({@link #clearGolemCaches()}).</li>
 *   <li>The {@code *Goto} goals gate on {@code tickCount % Config.golemDelay}
 *       ({@code AIEssentiaGoto.java:31}, {@code AILiquidGoto.java:33}); a probe
 *       drives no mob ticks, so {@code tickCount} stays 0, i.e. <b>on</b> the
 *       cadence. Every "the goal is inert" assertion here is paired with a
 *       {@code tickCount % 5 == 0} assertion, so the refusal is the goal's own
 *       gate and never a cadence miss. Probe 1 drives the cadence both ways
 *       ({@code tickCount} 0 → accepts, 1 → refuses).</li>
 *   <li>1.21.1's {@code GoalSelector} throttles {@code tick()}; these probes
 *       call {@code tick()} themselves, so the goal's own counters (the gather
 *       wind-up at {@code AILiquidGather.java:111}) advance one per call.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GolemFluidAiGameTest {

    /**
     * The face the probe golems were "placed against". The original's home
     * container is {@code restrictCenter.relative(facing.getOpposite())}, so
     * NORTH puts the container one block to the golem's <b>south</b>
     * ({@code z + 1}) — never inside the golem's own cell.
     */
    private static final int HOME_FACING = Direction.NORTH.get3DDataValue();

    /** A concrete, non-wildcard marker colour, so the wildcard path is exercised separately. */
    private static final byte MARKER_COLOUR = 3;

    /** The jar face every probe marks: {@code TileJarFillable.java:42-47} → 1 = top. */
    private static final Direction JAR_FACE = Direction.UP;

    /**
     * NeoForge's water cauldron fills in <b>whole level cycles</b>, not per
     * layer: {@code CauldronWrapper.fill} steps by
     * {@code gcd(maxLevel = 3, totalAmount = 1000) = 1}, so the only accepted
     * increment is 1000 mB across all three {@code LEVEL}s. A cauldron at
     * LEVEL 1 or 2 therefore accepts <b>nothing</b> ({@code (3 - level) / 3 == 0}).
     */
    private static final int CAULDRON_CYCLE = 1000;

    /** The essentia every probe primes a jar with ({@code TileJarFillable.maxAmount() == 64}). */
    private static final int JAR_PRIME = 30;

    private GolemFluidAiGameTest() {
    }

    // ------------------------------------------------------------------ helpers ---

    private static BlockPos at(final GameTestHelper helper, final int x, final int y, final int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /** The port's {@code GolemHelper.dimensionOf} stand-in — the marker's dimension stamp. */
    private static int dim(final ServerLevel level) {
        return level.dimension().location().hashCode();
    }

    private static void clearGolemCaches() {
        GolemHelper.clearJarListForTest();
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

    /** Marks a block for the golem on one face with a concrete colour. */
    private static void mark(final EntityGolemBase golem, final BlockPos pos, final Direction side) {
        golem.addMarker(new Marker(pos.getX(), pos.getY(), pos.getZ(), dim((ServerLevel) golem.level()),
                (byte) side.get3DDataValue(), MARKER_COLOUR));
    }

    private static TileJarFillable jarAt(final ServerLevel level, final BlockPos pos,
                                         final GameTestHelper helper) {
        final BlockEntity tile = level.getBlockEntity(pos);
        helper.assertTrue(tile instanceof TileJarFillable, "blockjar must place a TileJarFillable");
        return (TileJarFillable) tile;
    }

    private static int jarTotal(final TileJarFillable jar) {
        return jar.getEssentiaAmount(JAR_FACE);
    }

    private static int cauldronLevel(final ServerLevel level, final BlockPos pos,
                                     final GameTestHelper helper) {
        final net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.hasProperty(LayeredCauldronBlock.LEVEL),
                "the cauldron at " + pos + " is a layered water cauldron, got " + state);
        return state.getValue(LayeredCauldronBlock.LEVEL);
    }

    private static boolean isWaterSource(final ServerLevel level, final BlockPos pos) {
        final FluidState state = level.getFluidState(pos);
        return state.isSource() && state.getType() == Fluids.WATER;
    }

    private static IFluidHandler cauldronHandler(final ServerLevel level, final BlockPos pos,
                                                 final GameTestHelper helper) {
        final IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos,
                Direction.from3DDataValue(HOME_FACING));
        helper.assertTrue(handler != null,
                "NeoForge registers a cauldron fluid handler on every face, including the home face");
        return handler;
    }

    // ------------------------------------------------------------------ probes ---

    /**
     * {@link AIEssentiaEmpty} (core 6, priority 1 —
     * {@code EntityGolemBase.java:262}) against a real jar, with
     * {@link AIEssentiaGoto} (core 6, priority 3 — {@code :264}) as its path half.
     *
     * <ol>
     *   <li>jar 30 of {@code aer}, golem 4 of {@code aer}: the jar is a target
     *       because {@code findJarWithRoom} only offers a non-empty jar whose
     *       stored aspect <b>equals the payload</b> (original
     *       {@code GolemHelper.java:283-458}, pass 4: {@code jar.aspect == essentia
     *       && jar.amount < jar.maxAmount()}), so this is the pair the original
     *       empties into. {@code canUse} accepts it inside the <b>squared 4.0</b>
     *       gate ({@code AIEssentiaEmpty.java:34}) and {@code start()} hands the
     *       whole charge over: the jar rises to <b>exactly 34</b> and keeps its
     *       own aspect, the golem's {@code essentiaAmount} falls to 0 with its
     *       aspect cleared ({@code AIEssentiaEmpty.java:55-58});</li>
     *   <li>the cadence, both ways: {@code AIEssentiaGoto} accepts on
     *       {@code tickCount % 5 == 0} and refuses on {@code tickCount % 5 == 1}
     *       ({@code AIEssentiaGoto.java:31}), and refuses an empty payload on a
     *       cadence tick — so the inertness assertions elsewhere in this file are
     *       never cadence artefacts;</li>
     *   <li>a jar with 2 units of room takes only 2: it tops out at
     *       {@code maxAmount()} (64) and the golem keeps <b>exactly 4 - 2 = 2</b>
     *       — the original's {@code essentiaAmount = jar.addToContainer(...)}
     *       remainder ({@code AIEssentiaEmpty.java:55});</li>
     *   <li>the full jar drops out of targeting and the marked jar three blocks
     *       out takes its place, so {@code findJarWithRoom} resolves it — the
     *       only reason {@code canUse} refuses is the <b>squared 4.0</b> proximity
     *       gate: the jar centre is squared 9.25 away. Nothing moves into it.</li>
     * </ol>
     */
    @GameTest(template = "empty_24")
    public static void essentiaEmptyPoursItsWholeChargeIntoTheMarkedJar(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 2, 1, 3);
        final BlockPos jarPos = at(helper, 2, 1, 4);
        helper.setBlock(2, 1, 4, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable jar = jarAt(level, jarPos, helper);

        final Aspect aer = Aspect.getAspect("aer");
        final Aspect ignis = Aspect.getAspect("ignis");
        helper.assertTrue(aer != null, "the shipped aspect graph defines aer");
        helper.assertTrue(ignis != null, "the shipped aspect graph defines a second aspect, ignis");

        final EntityGolemBase golem = spawn(level, home, EnumGolemType.WOOD, 6);
        mark(golem, jarPos, JAR_FACE);
        helper.assertTrue(jar.addToContainer(aer, JAR_PRIME) == 0, "the jar takes all 30 of the priming fill");
        helper.assertTrue(jarTotal(jar) == JAR_PRIME, "the jar holds 30 before the golem acts, got " + jarTotal(jar));
        helper.assertTrue(golem.getNavigation().isDone(), "an idle navigation is the original's first canUse clause");
        golem.setEssentia(aer, 4);
        helper.assertTrue(golem.getEssentiaAmount() == 4, "the golem carries four essentia");

        // AIEssentiaGoto: the cadence gate, driven in both directions, plus the
        // payload gate — all on the same goal instance, so nothing below is a
        // cadence artefact.
        final AIEssentiaGoto gotoGoal = new AIEssentiaGoto(golem);
        golem.tickCount = 0;
        helper.assertTrue(golem.tickCount % GolemConfig.GOLEM_DELAY == 0,
                "tickCount 0 is on the Config.golemDelay = " + GolemConfig.GOLEM_DELAY + " cadence");
        helper.assertTrue(gotoGoal.canUse(),
                "on the cadence, with a payload and a jar with room, AIEssentiaGoto accepts (AIEssentiaGoto.java:31-38)");
        golem.tickCount = 1;
        helper.assertTrue(golem.tickCount % GolemConfig.GOLEM_DELAY != 0,
                "tickCount 1 is off the cadence");
        helper.assertTrue(!gotoGoal.canUse(),
                "off the cadence the same goal refuses (AIEssentiaGoto.java:31)");
        golem.tickCount = 0;
        golem.setEssentia(aer, 0);
        helper.assertTrue(golem.getEssentia() == null && golem.getEssentiaAmount() == 0,
                "setEssentia with 0 clears the aspect (EntityGolemBase.setEssentia)");
        helper.assertTrue(!gotoGoal.canUse(),
                "on the cadence an empty payload is the second refusal of the same line (:31)");
        golem.setEssentia(aer, 4);
        helper.assertTrue(gotoGoal.canUse(), "and the goal accepts again with its payload back");
        helper.assertTrue(!gotoGoal.canContinueToUse(),
                "canContinueToUse is count > 0 && !navigation.isDone() — count is 0 before the walk starts (:47)");
        gotoGoal.stop();
        helper.assertTrue(!gotoGoal.canContinueToUse(), "resetTask zeroes the walk budget (:50-52)");

        final AIEssentiaEmpty goal = new AIEssentiaEmpty(golem);
        helper.assertTrue(GolemHelper.findJarWithRoom(golem) != null,
                "the marked jar of the carried aspect with room is a target before the gate is applied");
        helper.assertTrue(goal.canUse(), "AIEssentiaEmpty accepts a marked jar with room, one block away");
        goal.start();
        helper.assertTrue(jarTotal(jar) == JAR_PRIME + 4,
                "the jar took the whole charge: 30 + 4 = 34, got " + jarTotal(jar));
        helper.assertTrue(aer.equals(jar.getEssentiaType(JAR_FACE)),
                "the jar kept the aspect it already held, as TileJarFillable.addToContainer does");
        helper.assertTrue(golem.getEssentiaAmount() == 0 && golem.getEssentia() == null,
                "the golem's essentiaAmount dropped by exactly the jar's take, got " + golem.getEssentiaAmount());

        // (b) A jar with 2 units of room: the golem keeps the remainder.
        final int capacity = jar.maxAmount();
        helper.assertTrue(capacity == 64, "a plain jar's maxAmount is 64, got " + capacity);
        final int toCap = capacity - 2 - jarTotal(jar);
        helper.assertTrue(toCap > 0, "the jar has room to reach capacity - 2, needs " + toCap);
        helper.assertTrue(jar.addToContainer(aer, toCap) == 0, "topping the jar up leaves nothing over");
        helper.assertTrue(jarTotal(jar) == capacity - 2, "the jar holds capacity - 2 before the second feed");
        golem.setEssentia(aer, 4);
        helper.assertTrue(goal.canUse(), "the goal accepts a jar with 2 units of room");
        goal.start();
        helper.assertTrue(jarTotal(jar) == capacity, "the jar tops out at its capacity " + capacity
                + ", got " + jarTotal(jar));
        helper.assertTrue(golem.getEssentiaAmount() == 2,
                "the golem keeps exactly the 4 - 2 the jar refused, got " + golem.getEssentiaAmount());
        helper.assertTrue(golem.getEssentia() == aer, "and still carries its own aspect");

        // (c) The squared-4.0 radius gate. The jar beside the golem is now full,
        // so targeting hands over to the marked jar three blocks out: the refusal
        // below can only be proximity.
        clearGolemCaches();
        final BlockPos farJarPos = at(helper, 5, 1, 3);
        helper.setBlock(5, 1, 3, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable far = jarAt(level, farJarPos, helper);
        helper.assertTrue(far.addToContainer(aer, JAR_PRIME) == 0, "the far jar takes its 30-unit priming fill");
        mark(golem, farJarPos, JAR_FACE);
        golem.setEssentia(aer, 4);
        helper.assertTrue(farJarPos.equals(GolemHelper.findJarWithRoom(golem)),
                "the far jar is the jar with room the golem targets — the gate is proximity, not targeting, got "
                        + GolemHelper.findJarWithRoom(golem));
        helper.assertTrue(!goal.canUse(),
                "a jar 3 blocks away is squared 9.25 away, past the squared-4.0 gate (AIEssentiaEmpty.java:34)");
        helper.assertTrue(jarTotal(far) == JAR_PRIME, "and nothing moved into it, got " + jarTotal(far));
        helper.assertTrue(jarTotal(jar) == capacity, "the full jar beside the golem is untouched, got " + jarTotal(jar));
        helper.assertTrue(golem.getEssentiaAmount() == 4, "the golem still carries its 4");
        helper.succeed();
    }

    /**
     * {@link AIEssentiaGather} (core 6, priority 2 —
     * {@code EntityGolemBase.java:263}) at the jar its home face points at
     * ({@code GolemHelper.homeContainerPos}: {@code restrictCenter - homeFacing}).
     *
     * <ol>
     *   <li>jar 30 of {@code ignis}, an empty golem: the goal takes
     *       <b>min(30, getCarryLimit() - 0)</b> — the clamp at
     *       {@code AIEssentiaGather.java:136} — so the golem's
     *       {@code essentiaAmount} rises to exactly the WOOD carry limit (4,
     *       {@code EntityGolemBase.getCarryLimit} with no Earth upgrade), the jar
     *       drops to 26, and the aspect taken is the jar's own. The jar is read
     *       from the <b>top</b> face ({@code :102-104}) and a jar's
     *       {@code takeEssentia} hands back the whole request it accepted
     *       ({@code TileJarFillable.java:213-215}). There is no vis cost in the
     *       original golem path, so none is asserted here;</li>
     *   <li>the goal's own delay refuses a second draw in the same instant
     *       ({@code :56} arms now + 1000 ms, {@code :153} re-arms now + 100 ms);</li>
     *   <li>a golem <em>at</em> the carry limit refuses ({@code :54}), a golem
     *       carrying an aspect the jar does not hold refuses ({@code :47-54}:
     *       neither the face aspect nor the internal one matches), and an empty
     *       jar is not a source ({@code :46}).</li>
     * </ol>
     */
    @GameTest(template = "empty_24")
    public static void essentiaGatherTakesFromTheHomeFacingJarUpToTheCarryLimit(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 2, 1, 3);
        final BlockPos jarPos = at(helper, 2, 1, 4);
        helper.setBlock(2, 1, 4, CatalogEssentiaStorage.jarState(false));
        final TileJarFillable jar = jarAt(level, jarPos, helper);
        final Aspect aer = Aspect.getAspect("aer");
        final Aspect fire = Aspect.getAspect("ignis");
        helper.assertTrue(aer != null && fire != null, "the shipped aspect graph defines aer and ignis");

        final EntityGolemBase golem = spawn(level, home, EnumGolemType.WOOD, 6);
        mark(golem, jarPos, JAR_FACE);
        helper.assertTrue(GolemHelper.homeContainerPos(golem).equals(jarPos),
                "the home-facing container is the jar itself (restrictCenter - NORTH = z + 1), got "
                        + GolemHelper.homeContainerPos(golem));
        helper.assertTrue(jar.addToContainer(fire, JAR_PRIME) == 0, "the jar takes all 30 of the priming fill");
        helper.assertTrue(jarTotal(jar) == JAR_PRIME, "the jar holds 30 before the golem draws, got " + jarTotal(jar));

        golem.setEssentia(aer, 0);
        final int limit = golem.getCarryLimit();
        helper.assertTrue(limit == 4, "a WOOD golem's carry limit is its base 4, got " + limit);

        final AIEssentiaGather goal = new AIEssentiaGather(golem);
        helper.assertTrue(goal.canUse(), "AIEssentiaGather accepts the home-facing jar");
        goal.start();
        helper.assertTrue(golem.getEssentiaAmount() == limit,
                "the golem's essentiaAmount rose to the carry limit exactly, got " + golem.getEssentiaAmount());
        helper.assertTrue(golem.getEssentia() == fire, "the golem took the jar's aspect, not its own");
        helper.assertTrue(jarTotal(jar) == JAR_PRIME - limit,
                "the jar gave exactly that much: 30 - 4 = 26, got " + jarTotal(jar));
        helper.assertTrue(!goal.canUse(),
                "the goal's own delay (now + 1000 ms at AIEssentiaGather.java:56, re-armed to now + 100 ms at :153) "
                        + "refuses a second draw in the same instant");

        // A golem at its carry limit does not draw (:54) — a fresh goal, delay 0.
        final AIEssentiaGather full = new AIEssentiaGather(golem);
        helper.assertTrue(full.getDelayForTest() == 0L, "the fresh goal's own cooldown is unarmed");
        helper.assertTrue(!full.canUse(), "a golem at its carry limit refuses to draw (AIEssentiaGather.java:54)");
        helper.assertTrue(jarTotal(jar) == JAR_PRIME - limit, "and the jar kept what it had, got " + jarTotal(jar));

        // A golem carrying an aspect the jar does not hold does not draw (:47-54).
        golem.setEssentia(aer, 2);
        helper.assertTrue(golem.getEssentiaAmount() < limit, "this golem is under its carry limit, so the "
                + "refusal below is the aspect match and not the clamp");
        final AIEssentiaGather mismatch = new AIEssentiaGather(golem);
        helper.assertTrue(!mismatch.canUse(),
                "carrying aer against a jar of ignis fails both the face and the internal-aspect clause (AIEssentiaGather.java:47-54)");
        helper.assertTrue(jarTotal(jar) == JAR_PRIME - limit, "and the jar kept what it had, got " + jarTotal(jar));

        // An empty jar is not a source (:46).
        final int had = jarTotal(jar);
        helper.assertTrue(jar.takeEssentia(fire, had, JAR_FACE) == had,
                "the jar hands back the whole request it accepted, as TileJarFillable.takeEssentia does");
        helper.assertTrue(jarTotal(jar) == 0, "the jar is empty, got " + jarTotal(jar));
        golem.setEssentia(aer, 0);
        final AIEssentiaGather emptyJar = new AIEssentiaGather(golem);
        helper.assertTrue(!emptyJar.canUse(), "an empty jar is not a source (AIEssentiaGather.java:46)");
        emptyJar.start();
        helper.assertTrue(golem.getEssentiaAmount() == 0, "and nothing was taken from it");
        helper.succeed();
    }

    /**
     * The liquid family (core 5, priorities 1/2/3 —
     * {@code EntityGolemBase.java:257-259}) against the homes this tree ships.
     *
     * <ol>
     *   <li><b>The act works on a real cauldron.</b> {@link AILiquidEmpty}'s fill
     *       runs through {@code GolemHelper.homeContainerPos} + the NeoForge block
     *       capability, so with an <b>empty</b> cauldron as the home
     *       {@code start()} pours the belly in: the SIMULATE pass says exactly
     *       {@value #CAULDRON_CYCLE} mB — NeoForge steps cauldrons in
     *       {@code gcd(3, 1000) = 1}-increments, i.e. whole three-layer cycles
     *       ({@code CauldronWrapper.fill}) — the block becomes a real water
     *       cauldron at {@code LEVEL 3}, and the golem's belly goes to
     *       <b>null</b>, the original's {@code fluidCarried = null}
     *       ({@code AILiquidEmpty.java:66-69});</li>
     *   <li><b>The same rule measured from the other side.</b> On a LEVEL-1
     *       cauldron the accepted increment is 0 mB — {@code (3 - 1) / 3 == 0} —
     *       so {@code start()} takes nothing: the {@code LEVEL} and the belly are
     *       unchanged. This is the handler's arithmetic, not the goal's;</li>
     *   <li><b>The gate is inert in this tree.</b> All three liquid goals enter
     *       through {@link GolemHelper#getMissingLiquids}, which reads the home
     *       container's <em>block entity</em> ({@code GolemHelper.java:556-565}).
     *       A cauldron has no block entity, and a chest exposes no fluid handler,
     *       so the list is empty on both homes: {@code canUse()} stays false for
     *       all three goals — on the cauldron home the act above still ran, which
     *       is the proof the inertness is the gate and not the goal logic.
     *       {@link GolemHelper#findPossibleLiquid} does <b>not</b> go through that
     *       list (it walks the markers, {@code :585-608}), so the marked water
     *       source below really does resolve — and the goals still refuse, because
     *       the fluid they were told to fetch never made it onto the list. Fifteen
     *       driven ticks of {@link AILiquidGather} (past its ten-tick wind-up,
     *       {@code AILiquidGather.java:111}) leave the source standing, the belly
     *       unchanged and the watch intact.</li>
     * </ol>
     */
    @GameTest(template = "empty_24")
    public static void liquidEmptyFillsARealCauldronAndTheGatheringGoalsStayInertOnTheMissingFluidGate(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 2, 1, 3);
        final BlockPos cauldronPos = at(helper, 2, 1, 4);
        helper.setBlock(2, 1, 4, Blocks.CAULDRON.defaultBlockState());
        helper.assertTrue(level.getBlockState(cauldronPos).is(Blocks.CAULDRON),
                "the home container cell holds a real, empty cauldron");
        helper.assertTrue(level.getBlockEntity(cauldronPos) == null,
                "and a cauldron is a block state, not a block entity");

        final EntityGolemBase golem = spawn(level, home, EnumGolemType.WOOD, 5);
        helper.assertTrue(GolemHelper.homeContainerPos(golem).equals(cauldronPos),
                "the home-facing container is the cauldron, got " + GolemHelper.homeContainerPos(golem));
        final int bellyLimit = golem.getFluidCarryLimit();
        helper.assertTrue(bellyLimit == 2000,
                "a WOOD golem's belly is floor(sqrt(carryLimit 4)) * 1000 = 2000 mB, got " + bellyLimit);
        final IFluidHandler handler = cauldronHandler(level, cauldronPos, helper);

        // ---- (1) the act: an empty cauldron takes one whole three-layer cycle.
        golem.setFluidCarried(new FluidStack(Fluids.WATER, CAULDRON_CYCLE));
        AILiquidGather.watchFluid(golem, new FluidStack(Fluids.WATER, CAULDRON_CYCLE));
        final int predicted = handler.fill(new FluidStack(Fluids.WATER, CAULDRON_CYCLE),
                IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(predicted == CAULDRON_CYCLE,
                "an empty water cauldron takes one whole three-layer cycle (1000 mB), got " + predicted);

        final AILiquidEmpty empty = new AILiquidEmpty(golem);
        helper.assertTrue(golem.tickCount % GolemConfig.GOLEM_DELAY == 0,
                "tickCount " + golem.tickCount + " is on the golemDelay cadence, so the refusals below are gates");
        helper.assertTrue(!empty.canUse(),
                "the gate is inert on a cauldron home: getMissingLiquids reads the home block entity, "
                        + "and a cauldron has none (GolemHelper.java:556-561)");
        helper.assertTrue(GolemHelper.getMissingLiquids(golem).isEmpty(), "the missing-fluid list is empty there");
        empty.start();
        helper.assertTrue(level.getBlockState(cauldronPos).is(Blocks.WATER_CAULDRON)
                        && cauldronLevel(level, cauldronPos, helper) == 3,
                "the cauldron filled to LEVEL 3 of its own block state, got " + level.getBlockState(cauldronPos));
        helper.assertTrue(golem.getFluidCarried() == null,
                "the belly emptied to null, the original's fluidCarried = null (AILiquidEmpty.java:66-69), got "
                        + golem.getFluidCarried());
        helper.assertTrue(AILiquidGather.watchedFluid(golem) == null,
                "AILiquidEmpty clears the golem's watch (AILiquidEmpty.java:82)");

        // ---- (2) the same handler from the other side: LEVEL 1 takes nothing.
        helper.setBlock(2, 1, 4, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 1));
        helper.assertTrue(cauldronLevel(level, cauldronPos, helper) == 1, "the cauldron starts at LEVEL 1");
        golem.setFluidCarried(new FluidStack(Fluids.WATER, CAULDRON_CYCLE));
        AILiquidGather.watchFluid(golem, new FluidStack(Fluids.WATER, CAULDRON_CYCLE));
        final IFluidHandler half = cauldronHandler(level, cauldronPos, helper);
        helper.assertTrue(half.fill(new FluidStack(Fluids.WATER, CAULDRON_CYCLE),
                        IFluidHandler.FluidAction.SIMULATE) == 0,
                "a LEVEL-1 cauldron accepts 0 mB: the fill step is a whole three-layer cycle (CauldronWrapper.fill)");
        empty.start();
        helper.assertTrue(cauldronLevel(level, cauldronPos, helper) == 1,
                "so the LEVEL is unchanged, got " + cauldronLevel(level, cauldronPos, helper));
        helper.assertTrue(golem.getFluidCarried() != null
                        && golem.getFluidCarried().getAmount() == CAULDRON_CYCLE,
                "and the belly kept all 1000 mB, got "
                        + (golem.getFluidCarried() == null ? "null" : golem.getFluidCarried().getAmount()));

        // ---- (3) the gate, on a home this tree can build: a chest (block entity,
        // ---- no fluid capability registered anywhere in this tree).
        helper.setBlock(2, 1, 4, Blocks.CHEST);
        final BlockEntity chest = level.getBlockEntity(cauldronPos);
        helper.assertTrue(chest != null, "the chest home is a block entity");
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, cauldronPos, null) == null,
                "and this tree registers no block fluid-handler capability for it");
        golem.setFluidCarried(new FluidStack(Fluids.WATER, CAULDRON_CYCLE));
        helper.assertTrue(GolemHelper.getMissingLiquids(golem).isEmpty(),
                "so the home reports no missing fluid for any container this tree ships");
        helper.assertTrue(!empty.canUse(), "AILiquidEmpty canUse stays false behind that gate");

        final BlockPos waterPos = at(helper, 2, 1, 6);
        helper.setBlock(2, 1, 6, Blocks.WATER);
        mark(golem, waterPos, Direction.UP);
        helper.assertTrue(isWaterSource(level, waterPos), "the marked block is a real water source");
        helper.assertTrue(waterPos.equals(GolemHelper.findPossibleLiquid(new FluidStack(Fluids.WATER, CAULDRON_CYCLE),
                        golem)),
                "findPossibleLiquid does resolve the marked source — it walks the markers, not the "
                        + "missing-fluid list (GolemHelper.java:585-608), got "
                        + GolemHelper.findPossibleLiquid(new FluidStack(Fluids.WATER, CAULDRON_CYCLE), golem));

        final AILiquidGoto gotoGoal = new AILiquidGoto(golem);
        helper.assertTrue(golem.tickCount % GolemConfig.GOLEM_DELAY == 0,
                "AILiquidGoto is queried on the cadence (tickCount " + golem.tickCount + ")");
        helper.assertTrue(golem.getFluidCarried().getAmount() <= bellyLimit - 1000,
                "and the belly leaves the 1000 mB of headroom its own clause asks for (AILiquidGoto.java:34)");
        helper.assertTrue(!gotoGoal.canUse(),
                "so AILiquidGoto's refusal, with a marked source in range, is the empty missing-fluid list");

        AILiquidGather.watchFluid(golem, new FluidStack(Fluids.WATER, CAULDRON_CYCLE));
        final AILiquidGather gather = new AILiquidGather(golem);
        helper.assertTrue(AILiquidGather.watchedFluid(golem) != null, "the watch is set, the goal's first clause");
        helper.assertTrue(!gather.canUse(),
                "AILiquidGather canUse stays false with its watch set, behind the same gate (AILiquidGather.java:43-46)");
        final int carriedBefore = golem.getFluidCarried().getAmount();
        gather.start();
        for (int tick = 0; tick < 15; tick++) {
            gather.tick();
        }
        helper.assertTrue(isWaterSource(level, waterPos),
                "fifteen driven ticks — past the ten-tick wind-up — left the marked water source standing");
        helper.assertTrue(golem.getFluidCarried() != null && golem.getFluidCarried().getAmount() == carriedBefore,
                "and the belly unchanged, got " + golem.getFluidCarried().getAmount());
        helper.assertTrue(AILiquidGather.watchedFluid(golem) != null,
                "the watch survives: only a completed transfer clears it (:149, :204, :233)");
        helper.succeed();
    }

    /**
     * The liquid goals' shared state and {@link AILiquidGather}'s own timing,
     * which no other probe here touches.
     *
     * <ol>
     *   <li><b>The watch is per golem.</b> 1.7.10 stored it on the entity
     *       ({@code EntityGolemBase.itemWatched}, read at
     *       {@code AILiquidGather.java:46} and cleared at {@code :105}), so
     *       two golems never share one: setting A's watch leaves B's at the
     *       stack B set, and {@code resetTask} on B clears B alone.</li>
     *   <li><b>The gather's own window.</b> {@code canContinueToUse} is
     *       {@code count < 20 && itemWatched != null}
     *       ({@code AILiquidGather.java:91-93}) with {@code count} restarted by
     *       {@code startExecuting} ({@code :99-101}), so the goal is live for
     *       twenty of its own ticks and not one more; and
     *       {@code isInterruptable} is constant false ({@code :95-97}). The
     *       golem is never ticked here, so the counter moves exactly once per
     *       driven {@code tick()} — the ten-tick wind-up at {@code :111} runs
     *       harmlessly because the missing-fluid list is empty.</li>
     *   <li><b>Which arm a vanilla source can reach.</b> The gather's first arm
     *       queries marked <em>fluid handlers</em> adjacent to the golem
     *       ({@code :60-68}); {@code GolemHelper} skips a marker with no block
     *       entity ({@code GolemHelper.java:615-640}), so a marked water source
     *       — a {@code FluidState}, no tile — is invisible to that arm. It is the
     *       marked-<em>block</em> arm ({@code :70-82}) that sees it, and that arm
     *       is bounded by the squared {@code 4.0} adjacency
     *       ({@code GolemHelper.ADJACENT_RANGE}): the source one block out is in
     *       the list (squared 1.25), the one three blocks out is not (squared
     *       9.25). On this home — water, so no block entity, so an empty
     *       missing-fluid list — the goal stays inert and the source stands.</li>
     * </ol>
     */
    @GameTest(template = "empty_24")
    public static void theLiquidWatchIsPerGolemAndTheGatherRunsItsOwnTwentyTickWindow(
            final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        helper.setBlock(2, 1, 4, Blocks.WATER);
        helper.setBlock(5, 1, 3, Blocks.WATER);
        final BlockPos nearWater = at(helper, 2, 1, 4);
        final BlockPos farWater = at(helper, 5, 1, 3);
        helper.assertTrue(isWaterSource(level, nearWater) && isWaterSource(level, farWater),
                "both marked cells hold real water sources");

        final EntityGolemBase golemA = spawn(level, at(helper, 2, 1, 3), EnumGolemType.WOOD, 5);
        final EntityGolemBase golemB = spawn(level, at(helper, 6, 1, 3), EnumGolemType.WOOD, 5);
        mark(golemA, nearWater, Direction.UP);
        mark(golemA, farWater, Direction.UP);

        // ---- (1) the watch is the entity's own, not a global.
        AILiquidGather.watchFluid(golemA, new FluidStack(Fluids.WATER, 1000));
        AILiquidGather.watchFluid(golemB, new FluidStack(Fluids.LAVA, 500));
        helper.assertTrue(AILiquidGather.watchedFluid(golemA) != null
                        && AILiquidGather.watchedFluid(golemA).getFluid() == Fluids.WATER
                        && AILiquidGather.watchedFluid(golemA).getAmount() == 1000,
                "golem A watches the water stack it set out to fetch");
        helper.assertTrue(AILiquidGather.watchedFluid(golemB) != null
                        && AILiquidGather.watchedFluid(golemB).getFluid() == Fluids.LAVA
                        && AILiquidGather.watchedFluid(golemB).getAmount() == 500,
                "golem B keeps its own lava watch — the original's field lived on the entity");
        new AILiquidGather(golemB).stop();
        helper.assertTrue(AILiquidGather.watchedFluid(golemB) == null,
                "reset clears B's watch (AILiquidGather.java:103-107)");
        helper.assertTrue(AILiquidGather.watchedFluid(golemA) != null
                        && AILiquidGather.watchedFluid(golemA).getFluid() == Fluids.WATER,
                "and leaves A's watch alone");

        // ---- (2) the gather's own twenty-tick window and its interruptability.
        final AILiquidGather gather = new AILiquidGather(golemA);
        helper.assertTrue(!gather.isInterruptable(),
                "the gather is not interruptable, the original's constant false (:95-97)");
        gather.start(); // :99-101 — the counter restarts
        helper.assertTrue(gather.canContinueToUse(),
                "with the watch set and count 0, canContinueToUse is true (:91-93)");
        for (int tick = 0; tick < 19; tick++) {
            gather.tick();
        }
        helper.assertTrue(gather.canContinueToUse(), "nineteen driven ticks: still inside the window (:92)");
        gather.tick();
        helper.assertTrue(!gather.canContinueToUse(),
                "the twentieth tick closes the window — count < 20 is the original's own bound (:92)");

        // ---- (3) which arm a marked vanilla source can reach, and how far.
        helper.assertTrue(GolemHelper.getMarkedFluidHandlersAdjacentToGolem(
                        new FluidStack(Fluids.WATER, 1000), level, golemA).isEmpty(),
                "the handler arm is blind to a fluid block: a marker with no block entity is skipped "
                        + "(GolemHelper.java:615-640), so the gather's block arm is the only reachable one");
        final List<BlockPos> adjacent = GolemHelper.getMarkedBlocksAdjacentToGolem(level, golemA, (byte) -1);
        helper.assertTrue(adjacent.contains(nearWater),
                "the source one block out is in the block arm's squared-4.0 list, got " + adjacent);
        helper.assertTrue(!adjacent.contains(farWater),
                "the source three blocks out is squared 9.25 away, outside that same list, got " + adjacent);

        // ---- and the gate, on a home that is a fluid block: no block entity, so
        // ---- no missing-fluid list, so the goal is inert and the water stands.
        helper.assertTrue(GolemHelper.getMissingLiquids(golemA).isEmpty(),
                "the home cell is water: no block entity, so no missing fluid (GolemHelper.java:556-561)");
        helper.assertTrue(golemA.tickCount % GolemConfig.GOLEM_DELAY == 0,
                "tickCount " + golemA.tickCount + " is on the golemDelay cadence, so the refusals below are gates");
        golemA.setFluidCarried(new FluidStack(Fluids.WATER, 1000));
        helper.assertTrue(!gather.canUse(),
                "with the watch set and a source adjacent, canUse still refuses: the missing-fluid gate "
                        + "(AILiquidGather.java:43-46)");
        helper.assertTrue(isWaterSource(level, nearWater) && isWaterSource(level, farWater),
                "twenty driven ticks left both marked sources standing");
        helper.assertTrue(golemA.getFluidCarried() != null && golemA.getFluidCarried().getAmount() == 1000,
                "and the belly unchanged, got " + golemA.getFluidCarried().getAmount());
        helper.succeed();
    }

    /**
     * The watch is the liquid goals' shared state — 1.7.10's public
     * {@code EntityGolemBase.itemWatched}, ported as the {@link FluidStack} per
     * golem (the original stored {@code ItemStack(itemById(fluidID), 1, amount)}:
     * the fluid id as an item id and the amount in the damage value).
     *
     * <ol>
     *   <li>{@link AILiquidGather#stop()} clears it, exactly where the original's
     *       {@code resetTask} nulls {@code itemWatched}
     *       ({@code AILiquidGather.java:103-107});</li>
     *   <li>{@link AILiquidEmpty}'s act clears it too
     *       ({@code AILiquidEmpty.java:82}) — here with a <b>full</b> cauldron as
     *       the home, so the fill takes 0 mB and the clearing is the only
     *       effect: the belly is untouched, which proves the watch clear is the
     *       goal's own line and not a side effect of a transfer;</li>
     *   <li>and it does <b>not</b> clear when the home is not a fluid handler:
     *       the original's whole act — the fill, {@code updateCarried}, the block
     *       update and {@code itemWatched = null} — sits inside the
     *       {@code tile instanceof IFluidHandler} guard ({@code :62}, {@code :80-82}),
     *       so on an air home the watch and the belly both survive.</li>
     * </ol>
     */
    @GameTest(template = "empty_24")
    public static void liquidGoalsClearTheGolemWatchOnResetAndOnEmptying(final GameTestHelper helper) {
        clearGolemCaches();
        final ServerLevel level = helper.getLevel();
        final BlockPos home = at(helper, 2, 1, 3);
        final BlockPos cauldronPos = at(helper, 2, 1, 4);
        helper.setBlock(2, 1, 4, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3));
        final EntityGolemBase golem = spawn(level, home, EnumGolemType.WOOD, 5);
        helper.assertTrue(GolemHelper.homeContainerPos(golem).equals(cauldronPos),
                "the full cauldron is the home-facing container, got " + GolemHelper.homeContainerPos(golem));
        golem.setFluidCarried(new FluidStack(Fluids.WATER, 500));

        AILiquidGather.watchFluid(golem, new FluidStack(Fluids.LAVA, 1000));
        helper.assertTrue(AILiquidGather.watchedFluid(golem) != null
                        && AILiquidGather.watchedFluid(golem).getFluid() == Fluids.LAVA,
                "the watch holds the stack the golem set out to fetch");
        helper.assertTrue(AILiquidGather.watchedFluid(golem).getAmount() == 1000,
                "with the amount the 1.7.10 watch item carried in its damage value");

        new AILiquidGather(golem).stop();
        helper.assertTrue(AILiquidGather.watchedFluid(golem) == null,
                "reset clears the watch (AILiquidGather.java:105)");

        AILiquidGather.watchFluid(golem, new FluidStack(Fluids.WATER, 500));
        new AILiquidEmpty(golem).start();
        helper.assertTrue(AILiquidGather.watchedFluid(golem) == null,
                "AILiquidEmpty's act clears it as well (AILiquidEmpty.java:82)");
        helper.assertTrue(golem.getFluidCarried() != null && golem.getFluidCarried().getAmount() == 500,
                "the full cauldron took nothing, so the belly is the only proof the act ran, got "
                        + golem.getFluidCarried().getAmount());
        helper.assertTrue(cauldronLevel(level, cauldronPos, helper) == 3, "and the cauldron is still full");

        // The original's guard: no handler at the home, no act, no watch clear.
        AILiquidGather.watchFluid(golem, new FluidStack(Fluids.WATER, 500));
        helper.setBlock(2, 1, 4, Blocks.AIR);
        helper.assertTrue(level.getBlockEntity(cauldronPos) == null
                        && level.getCapability(Capabilities.FluidHandler.BLOCK, cauldronPos,
                        Direction.from3DDataValue(HOME_FACING)) == null,
                "the home cell is now air: no tile, no fluid handler");
        new AILiquidEmpty(golem).start();
        helper.assertTrue(AILiquidGather.watchedFluid(golem) != null
                        && AILiquidGather.watchedFluid(golem).getFluid() == Fluids.WATER,
                "with no handler the act never runs, so the watch survives (AILiquidEmpty.java:62, 82)");
        helper.assertTrue(golem.getFluidCarried() != null && golem.getFluidCarried().getAmount() == 500,
                "and the belly is untouched, got " + golem.getFluidCarried().getAmount());
        helper.succeed();
    }
}
