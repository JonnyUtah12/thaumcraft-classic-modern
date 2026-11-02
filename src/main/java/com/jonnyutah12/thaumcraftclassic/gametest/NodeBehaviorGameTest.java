package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeReveal;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeRuntime;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeWandTap;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.world.NodeTickDriver;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Node behaviour engine end-to-end (TC-M5-02; design record:
 * docs/architecture/node-behavior.md). Covers what the pure unit tests cannot:
 * the driver's record→runtime→record loop against the live store and aspect
 * registry, the production/degradation/death cycle observed over simulated
 * ticks, the wand tap persisting through the store, and the reveal gate against
 * a registered item.
 *
 * <p>Who owns the RNG, per probe (this is the whole difference between a green
 * suite and a flaky one):
 * <ul>
 *   <li>{@link #driverMaterialisesRuntimeAndTapPersistsToStore} draws from a
 *       {@code Random} the probe constructs, and asserts values that do not
 *       depend on a probabilistic branch at all.</li>
 *   <li>{@link #isolatedNodeDeathSpiralDegradesTheOneEmptyAspectToBaseNine} and
 *       {@link #dischargedNodeDeathSpiralDegradesBothEmptyAspectsToBaseEight}
 *       drive {@link NodeRuntime#tick} with {@link ScriptedRolls}: every roll
 *       site is pinned, so each assertion names the TileNode branch it
 *       documents.</li>
 *   <li>{@link #emptyNodeDiesAndIsRemovedFromStore} drives the driver and the
 *       store, and its outcome is RNG-independent by construction (its base is
 *       0, so the spiral's removal is forced by the {@code base <= 0} operand of
 *       TileNode L711, not by the 1/20 roll).</li>
 * </ul>
 *
 * <p>No probe here reads the level RNG. The node tick loop consumes the level's
 * shared stream in production ({@code NodeTickDriver} bridges {@code world.rand}
 * to {@code LevelRandomBridge(level.getRandom())}, TileNode L711), and a probe
 * that asserts one value from that stream is asserting a draw it does not own:
 * the value depends on how many other game tests consumed the stream in the
 * same tick. The death-spiral probe did exactly that — green at 125 tests, red
 * at 127 (docs/jei-integration.md § "Gate flakiness observed on this branch",
 * card t_7bf6a4d0). The engine's arithmetic is deterministic; the observation
 * was not. Probabilistic branch <em>mechanics</em> (which modifier a removal
 * picks, the roll formulas) stay pinned in {@code NodeBehaviorMathTest}.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NodeBehaviorGameTest {

    /** Fixed wall clock: the 75 ms catch-up math (TileNode L286) is not under test here. */
    private static final long PROBE_CLOCK_MS = 1_700_000_000_000L;

    private static int sum(final Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int baseOf(final WorldMagicData.Node record, final String tag) {
        return record.aspectsBase().getOrDefault(tag, -1);
    }

    private static int heldOf(final WorldMagicData.Node record, final String tag) {
        return record.aspects().getOrDefault(tag, -1);
    }

    private static WorldMagicData.Node findNode(final WorldMagicData data, final ChunkPos chunk,
            final BlockPos pos) {
        for (final WorldMagicData.Node node : data.getNodes(chunk)) {
            if (node.x() == pos.getX() && node.y() == pos.getY() && node.z() == pos.getZ()) {
                return node;
            }
        }
        return null;
    }

    /**
     * Insertion-ordered map for probes that pin WHICH aspect a uniform pick
     * takes. {@code Map.of}'s iteration order is unspecified by the JDK and was
     * observed to differ between JVM invocations — the spiral walks the
     * container in that order (TileNode L708) and the production pick indexes
     * into it (L765), so never rely on it (same rule as
     * {@code NodeBehaviorMathTest.orderedMap}).
     */
    private static Map<String, Integer> orderedMap(final Object... keyValue) {
        final Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValue.length; i += 2) {
            map.put((String) keyValue[i], (Integer) keyValue[i + 1]);
        }
        return map;
    }

    /**
     * A runtime built from a record through the LIVE aspect registry — the seam
     * a unit test cannot reach (there {@code Aspect.getAspect} is null, so
     * {@code NodeBehaviorMathTest} injects a definition-table resolver). That is
     * why the two spiral probes are game tests: they assert the engine's
     * arithmetic against the real {@link Aspect} instances the driver resolves.
     *
     * <p>The runtime is local to the probe: it is never registered with
     * {@code NodeTickDriver} and its record is never written to the store, so no
     * neighbouring probe's structure, chunk, tick counter or RNG stream can
     * reach it.
     */
    private static NodeRuntime localRuntime(final int x, final int y, final int z,
            final Map<String, Integer> container, final Map<String, Integer> visBase) {
        return NodeRuntime.fromRecord(new WorldMagicData.Node(x, y, z, "normal", null, 10,
                container, visBase, 0L), Aspect::getAspect, () -> PROBE_CLOCK_MS);
    }

    /**
     * The engine's world seams, answered locally. {@code neighborAt} reads a
     * registered runtime set the way the driver's own {@code WorldContext} reads
     * its live runtime map (TileNode L597-601 → the port's
     * {@code Context.neighborAt}): every node in scope is in the map, the node
     * under test included, because a discharge is made by the node that ROLLS
     * reading the node that gets READ. Every other probe stays at the interface
     * default, which is the inert reading — no stabilizer below the node (lock
     * 0, L949-957), no taint/eerie/magical-forest biome registered (painting
     * skipped, L773-801 / L830-910), no magical log, air everywhere, nothing
     * breakable, no aspect orb.
     */
    private static final class ProbeContext implements NodeRuntime.Context {

        private final Map<BlockPos, NodeRuntime> runtimes = new HashMap<>();

        /** Puts a runtime in scope at its own position. */
        ProbeContext add(final NodeRuntime node) {
            runtimes.put(new BlockPos(node.x(), node.y(), node.z()), node);
            return this;
        }

        @Override
        public NodeRuntime neighborAt(final int x, final int y, final int z) {
            return runtimes.get(new BlockPos(x, y, z));
        }
    }

    /**
     * Rolls scripted <b>per roll site</b>, keyed by the bound the site asks for.
     * The engine asks for a bounded int once per decision, and each bound is one
     * named site in the original: {@code nextInt(20)} is the spiral's 1/20
     * aspect-removal roll (TileNode L711), {@code nextInt(5)} is the discharge
     * offset (L590-592) and the 1/5 modifier roll (L713), {@code nextInt(2)} is
     * the uniform pick over this node's two aspects (L609, L765),
     * {@code nextInt(1)} is the pick over a one-aspect candidate list.
     *
     * <p>Pinning the bound pins the site no matter how many draws came before
     * it, which is exactly what the level RNG cannot offer: there, the value a
     * site receives depends on the draws every other probe took from the same
     * stream. The sequence for a bound cycles, so a probe of any tick count is
     * deterministic. A bound with no script, a scripted value outside
     * {@code [0, bound)}, {@code nextBoolean}/{@code nextDouble} — used only by
     * the PALE modifier paths (L593, L617) — all throw: a probe that walks into
     * an unscripted branch says so instead of silently changing branch. Game-test
     * scope of the approach {@code NodeBehaviorMathTest} uses as unit tests.
     */
    private static final class ScriptedRolls extends Random {

        private final Map<Integer, int[]> sites = new HashMap<>();
        private final Map<Integer, Integer> taken = new HashMap<>();

        /** The answers for one roll site, in order, cycling on exhaustion. */
        ScriptedRolls answer(final int bound, final int... values) {
            sites.put(bound, values.clone());
            return this;
        }

        @Override
        public int nextInt(final int bound) {
            final int[] sequence = sites.get(bound);
            if (sequence == null) {
                throw new IllegalArgumentException(
                        "unscripted roll site: nextInt(" + bound + ") — see ScriptedRolls");
            }
            final int draw = taken.merge(bound, 1, Integer::sum) - 1;
            final int value = sequence[draw % sequence.length];
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("scripted " + value + " is out of range for nextInt("
                        + bound + ")");
            }
            return value;
        }

        @Override
        public boolean nextBoolean() {
            throw new IllegalArgumentException("unscripted roll site: nextBoolean (PALE discharge, TileNode L593)");
        }

        @Override
        public double nextDouble() {
            throw new IllegalArgumentException("unscripted roll site: nextDouble");
        }
    }

    @GameTest(template = "empty")
    public static void driverMaterialisesRuntimeAndTapPersistsToStore(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final ChunkPos chunk = new ChunkPos(pos);
        final WorldMagicData data = WorldMagicData.get(level);

        data.addNode(chunk, new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                Map.of("aer", 10, "ignis", 10)));

        // Pass 1: the driver reconciles the record into a live runtime.
        NodeTickDriver.runNodePass(level);
        final NodeRuntime runtime = NodeTickDriver.runtime(level, pos);
        helper.assertTrue(runtime != null, "driver did not materialise a runtime for the stored record");
        helper.assertTrue(runtime.getAspects().visSize() == 20,
                "runtime container does not match the record");

        // Three taps of 1 vis each (TileNode L403-427 via NodeWandTap).
        VisPool pool = VisPool.EMPTY;
        final Random random = new Random(0x5EEDL);
        for (int i = 0; i < 3; i++) {
            pool = NodeWandTap.tap(runtime, pool, 5000, 1, false, random).pool();
        }
        helper.assertTrue(sum(pool.centiVis()) == 300,
                "three taps must store 300 centi-vis, got " + sum(pool.centiVis()));
        helper.assertTrue(runtime.getAspects().visSize() == 17,
                "node must have lost exactly 3 vis, got " + runtime.getAspects().visSize());

        // Pass 2: the dirty runtime writes back through replaceNode.
        NodeTickDriver.runNodePass(level);
        final WorldMagicData.Node stored = findNode(data, chunk, pos);
        helper.assertTrue(stored != null, "node record vanished from the store");
        helper.assertTrue(sum(stored.aspects()) == 17,
                "store did not receive the drained container: " + stored.aspects());
        helper.assertTrue(stored.aspectsBase() != null && sum(stored.aspectsBase()) == 20,
                "vis base must persist alongside the container: " + stored.aspectsBase());

        helper.succeed();
    }

    /**
     * Scenario (a) of the two the flaky probe collapsed: an isolated node —
     * nothing for the discharge to read — so exactly ONE container entry is
     * empty at the 1200-tick spiral, and the vis base falls by exactly one:
     * 5 + 5 → 5 + 4 = <b>9</b>. The number is the original probe's; the
     * difference is that every roll that produces it is pinned here, so the
     * value is a conclusion and not a drawn coincidence.
     *
     * <p>Every assertion names the TileNode branch it pins.
     */
    @GameTest(template = "empty")
    public static void isolatedNodeDeathSpiralDegradesTheOneEmptyAspectToBaseNine(
            final GameTestHelper helper) {
        final NodeRuntime node = localRuntime(0, 64, 0,
                orderedMap("aer", 0, "ignis", 0), orderedMap("aer", 5, "ignis", 5));
        // The node's own cell is in scope (as in the driver's runtime map) and
        // nothing sits at +x: every discharge roll looks outward, finds null, and
        // returns (TileNode L597-601). That is what makes this the isolated case.
        final ProbeContext world = new ProbeContext().add(node);
        final ScriptedRolls rolls = new ScriptedRolls()
                .answer(5, 2, 1, 1, 1, 1, 1) // discharge offsets → (1,0,0), TileNode L590-592
                .answer(2, 0)                // uniform pick over 2 entries → index 0 = aer, L609 / L765
                .answer(20, 1);              // spiral 1/20 removal roll MISSES, TileNode L711

        // Production: the first unit appears exactly at count 600 — the
        // regeneration period for a node with no modifier (TileNode L754-765,
        // period L661-674). The pick is scripted to index 0, so the unit is aer.
        for (int tick = 1; tick <= 600; tick++) {
            node.tick(world, rolls, () -> PROBE_CLOCK_MS);
        }
        WorldMagicData.Node record = node.toRecord();
        helper.assertTrue(sum(record.aspects()) == 1,
                "the first produced unit must appear exactly at count 600 (TileNode L754-765), got "
                        + record.aspects());
        helper.assertTrue(heldOf(record, "aer") == 1 && heldOf(record, "ignis") == 0,
                "the production pick (L765) is scripted to index 0 of the insertion-ordered "
                        + "candidate list, so aer holds the unit: " + record.aspects());

        // The spiral is period-gated (TileNode L707: count % 1200 == 0): one tick
        // short of 1200 the vis base is untouched.
        for (int tick = 601; tick <= 1199; tick++) {
            node.tick(world, rolls, () -> PROBE_CLOCK_MS);
        }
        record = node.toRecord();
        helper.assertTrue(sum(record.aspectsBase()) == 10,
                "count 1199 must not fire the death spiral (TileNode L707), got base "
                        + record.aspectsBase());

        // Count 1200 — the spiral (TileNode L707-748).
        node.tick(world, rolls, () -> PROBE_CLOCK_MS);
        record = node.toRecord();

        // L709-710: only the entry whose amount is <= 0 is degraded, by exactly
        // one; aer holds the unit, so its base stays 5 and ignis falls to 4.
        helper.assertTrue(baseOf(record, "aer") == 5 && baseOf(record, "ignis") == 4,
                "the spiral degrades the ONE empty aspect's base by exactly one (TileNode L709-710): "
                        + "aer 5 → 5, ignis 5 → 4, got " + record.aspectsBase());
        // The original probe's number, now derived from pinned rolls: 5 + 4.
        helper.assertTrue(sum(record.aspectsBase()) == 9,
                "death spiral must degrade the empty aspect's base by exactly one, got "
                        + sum(record.aspectsBase()) + " " + record.aspectsBase());

        // L711-712: the 1/20 removal roll is scripted to miss, so no entry is
        // removed and the base-1 aspect keeps its slot.
        helper.assertTrue(record.aspects().size() == 2,
                "the 1/20 removal roll (TileNode L711) is scripted to miss, so both entries stay: "
                        + record.aspects());

        // The old `held == 1 || held == 2` tolerance, made exact. Two branches
        // decide it and both are pinned:
        //  - the produced unit is never drained, because discharge returns at the
        //    neighbor lookup (TileNode L597-601: neighborAt answers null);
        //  - count 1200 produces nothing, because the spiral's nodeChange (L729)
        //    clears `regeneration` in the same tick and the production gate asks
        //    for `regeneration > 0` (L754).
        helper.assertTrue(sum(record.aspects()) == 1,
                "exactly the 600-tick unit is held: discharge has no neighbor (TileNode L597-601) and "
                        + "the spiral's nodeChange (L729) gates production off at the same count (L754), got "
                        + record.aspects());

        // L733: the death test is the container's ENTRY count, not its vis total.
        helper.assertTrue(!node.isDead(), "a node with two entries and 1 vis must not die (TileNode L733)");

        helper.succeed();
    }

    /**
     * Scenario (b) of the two the flaky probe collapsed: a richer node at -x
     * discharges the produced unit out of the node under test (TileNode
     * L608-613, L631), so at the 1200-tick spiral BOTH entries are empty and
     * BOTH bases fall: 5 + 5 → 4 + 4 = <b>8</b>. That 8 is the value the flaky
     * probe reported as a failure on CI's `game-test-no-curios` job; it was
     * never an engine fault, it was the other legal branch of the same TileNode
     * loop, reached whenever the level RNG happened to put a node within the
     * ±4-block discharge window.
     *
     * <p>Every assertion names the TileNode branch it pins.
     */
    @GameTest(template = "empty")
    public static void dischargedNodeDeathSpiralDegradesBothEmptyAspectsToBaseEight(
            final GameTestHelper helper) {
        final ScriptedRolls rolls = new ScriptedRolls()
                .answer(5, 2, 1, 1, 1, 1, 1) // discharge offsets → (1,0,0), TileNode L590-592
                .answer(2, 0)                // uniform pick over 2 entries → index 0 = aer, L609 / L765
                .answer(20, 1);              // spiral 1/20 removal roll MISSES, TileNode L711

        final NodeRuntime node = localRuntime(0, 64, 0,
                orderedMap("aer", 0, "ignis", 0), orderedMap("aer", 5, "ignis", 5));
        final ProbeContext world = new ProbeContext().add(node);

        // Phase 1, no neighbor in scope: the node reaches count 600 and produces
        // its first unit (TileNode L754-765), exactly as in the isolated probe.
        for (int tick = 1; tick <= 600; tick++) {
            node.tick(world, rolls, () -> PROBE_CLOCK_MS);
        }
        WorldMagicData.Node record = node.toRecord();
        helper.assertTrue(sum(record.aspects()) == 1 && heldOf(record, "aer") == 1,
                "the node must hold its 600-tick unit before anything can discharge it, got "
                        + record.aspects());

        // Phase 2 — the state the flaky probe hit at random: a node enters the
        // discharge window. The discharger sits at -x so the scripted offset
        // (1,0,0) points from IT at the node under test — discharge moves vis
        // from the read node `nd` into the node that ROLLS (TileNode L606-613),
        // so the loser is the node being read. It is richer (avg 10 vs the node's
        // 5, L606-607) and below its own base, so both gates pass.
        final NodeRuntime richer = localRuntime(-1, 64, 0, orderedMap("aer", 1), orderedMap("aer", 20));
        world.add(richer);

        // Count 602 — the discharger's first eligible tick (attempt interval 2 for
        // a node with no modifier, TileNode L586-587): it picks aer (index 0 of
        // the read node's two entries, L609), takes it, and pays it into its own
        // container because it is below its base (L611-613).
        for (int tick = 601; tick <= 610; tick++) {
            node.tick(world, rolls, () -> PROBE_CLOCK_MS);
            richer.tick(world, rolls, () -> PROBE_CLOCK_MS);
        }
        record = node.toRecord();
        helper.assertTrue(sum(record.aspects()) == 0,
                "the produced unit must have been discharged to the richer neighbor "
                        + "(TileNode L606-613, L631), got " + record.aspects());
        helper.assertTrue(richer.getAspects().getAmount(Aspect.getAspect("aer")) == 2,
                "the discharger paid the unit into its own container (TileNode L611-613): 1 it started "
                        + "with + the drained unit = 2, got " + richer.getAspects().visSize());
        helper.assertTrue(sum(record.aspectsBase()) == 10,
                "discharge branch 1 (TileNode L611-613) moves vis only: it must not touch the vis base, "
                        + "got " + record.aspectsBase());

        // The drain pauses the node's production for regeneration/2 = 300 ticks
        // (TileNode L631 → L760-762), so the container is still empty one tick
        // before the spiral.
        for (int tick = 611; tick <= 1199; tick++) {
            node.tick(world, rolls, () -> PROBE_CLOCK_MS);
            richer.tick(world, rolls, () -> PROBE_CLOCK_MS);
        }
        record = node.toRecord();
        helper.assertTrue(sum(record.aspectsBase()) == 10,
                "count 1199 must not fire the death spiral (TileNode L707), got base "
                        + record.aspectsBase());

        // Count 1200 — the spiral with two empty entries.
        node.tick(world, rolls, () -> PROBE_CLOCK_MS);
        record = node.toRecord();

        // L708-731: the loop walks EVERY entry and degrades each empty one, and
        // it only `break`s when the removal roll fires (L726) — scripted to miss,
        // so both entries degrade here. This is the branch the single-value probe
        // could not see.
        helper.assertTrue(baseOf(record, "aer") == 4 && baseOf(record, "ignis") == 4,
                "with both entries empty the spiral degrades BOTH by one (TileNode L708-710, the break "
                        + "at L726 is gated by the removal roll at L711): got " + record.aspectsBase());
        helper.assertTrue(sum(record.aspectsBase()) == 8,
                "two empty aspects, removal roll (TileNode L711) scripted to miss, so 5+5 → 4+4 = 8, got "
                        + sum(record.aspectsBase()) + " " + record.aspectsBase());
        helper.assertTrue(record.aspects().size() == 2,
                "the removal roll (TileNode L711-712) is scripted to miss on both entries: " + record.aspects());

        // The other half of the old `held == 1 || held == 2` tolerance: with a
        // discharge the honest value is 0, and it is exact — the drain (L611-613)
        // emptied the container, the 300-tick pause (L631 → L760-762) suppressed
        // the 1200 production, and the spiral's nodeChange (L729) gates
        // production off at the same count anyway (L754).
        helper.assertTrue(sum(record.aspects()) == 0,
                "an emptied container stays empty at the 1200 spiral (TileNode L611-613, L631, L729, "
                        + "L754), got " + record.aspects());

        // L733: an EMPTY CONTAINER does not kill a node that still has entries —
        // the original's death test is `getAspects().size() <= 0`, the number of
        // entries, not the vis total. The zero-base removal (L711's second
        // operand) is what eventually kills a node, pinned in
        // NodeBehaviorMathTest.deathSpiralRemovesAtZeroBaseAndKillsTheNode and
        // in-game by emptyNodeDiesAndIsRemovedFromStore.
        helper.assertTrue(!node.isDead(),
                "two zero-vis entries keep the node alive: the death test is the entry count (TileNode L733)");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyNodeDiesAndIsRemovedFromStore(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        final ChunkPos chunk = new ChunkPos(pos);
        final WorldMagicData data = WorldMagicData.get(level);

        // A node whose every aspect sits at base 0 with an empty container: the
        // 1200-tick spiral removes one zero-base aspect per pass (TileNode
        // L713-726, forced by the base <= 0 branch), and the emptied node dies
        // and loses its record (L749-758).
        data.addNode(chunk, new WorldMagicData.Node(pos.getX(), pos.getY(), pos.getZ(), "normal", null, 10,
                Map.of("aer", 0, "ignis", 0), Map.of("aer", 0, "ignis", 0), 0L));

        for (int tick = 1; tick <= 1200; tick++) {
            NodeTickDriver.runNodePass(level);
        }
        WorldMagicData.Node stored = findNode(data, chunk, pos);
        helper.assertTrue(stored != null && stored.aspects().size() == 1,
                "first zero-base aspect must be removed at the 1200 spiral: "
                        + (stored == null ? "already dead" : stored.aspects()));

        for (int tick = 1201; tick <= 2400; tick++) {
            NodeTickDriver.runNodePass(level);
        }
        helper.assertTrue(findNode(data, chunk, pos) == null,
                "emptied node must be removed from the store at the 2400 spiral");
        helper.assertTrue(NodeTickDriver.runtime(level, pos) == null,
                "dead node runtime must be dropped from the driver");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void revealGateFollowsTheOriginalSlots(final GameTestHelper helper) {
        try {
            final ItemStack probe = new ItemStack(TestProbeContent.PROBE_ITEM.get());
            final ItemStack empty = ItemStack.EMPTY;

            // Nothing registered: no reveal (TileNodeRenderer L187-197 default).
            helper.assertTrue(!NodeReveal.canSeeNodes(probe, empty, null),
                    "unregistered items must not reveal nodes");

            // Held thaumometer branch (TileNodeRenderer L191-196).
            NodeReveal.registerThaumometer(probe.getItem());
            helper.assertTrue(NodeReveal.canSeeNodes(probe, empty, null),
                    "held thaumometer must reveal nodes");
            helper.assertTrue(!NodeReveal.canSeeNodes(empty, probe, null),
                    "a thaumometer in the helmet slot must not reveal nodes");

            // Helmet IRevealer branch, honouring showNodes (TileNodeRenderer L187-190).
            NodeReveal.registerRevealer(probe.getItem(), (stack, wearer) -> true);
            helper.assertTrue(NodeReveal.canSeeNodes(empty, probe, null),
                    "revealing helmet must reveal nodes");
            NodeReveal.registerRevealer(probe.getItem(), (stack, wearer) -> false);
            helper.assertTrue(!NodeReveal.canSeeNodes(empty, probe, null),
                    "showNodes=false must not reveal nodes");
        } finally {
            NodeReveal.clear();
        }
        helper.succeed();
    }
}
