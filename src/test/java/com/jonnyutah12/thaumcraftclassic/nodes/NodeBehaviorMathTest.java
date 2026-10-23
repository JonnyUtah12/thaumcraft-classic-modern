package com.jonnyutah12.thaumcraftclassic.nodes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-02 acceptance: the node behaviour engine's rules, pinned line-for-line
 * against the reference {@code TileNode} (docs/architecture/node-behavior.md).
 * Pure unit coverage — the handlers are driven directly with a scripted RNG so
 * every probabilistic branch is deterministic; the full tick order and the
 * record write-back loop are covered in-game by {@code NodeBehaviorGameTest}.
 */
class NodeBehaviorMathTest {

    private static final long CLOCK = 1_700_000_000_000L;

    // Unit tests run without the game's NewRegistryEvent, so Aspect.getAspect is
    // null here; build one shared instance per tag (Aspect has no equals, so the
    // resolver and the test constants must be the same object).
    private static final java.util.Map<String, Aspect> ASPECT_CACHE = new java.util.HashMap<>();

    private static Aspect aspect(final String tag) {
        return ASPECT_CACHE.computeIfAbsent(tag, t -> new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(t))
                .findFirst()
                .orElseThrow()));
    }

    private static final Aspect AER = aspect("aer");
    private static final Aspect IGNIS = aspect("ignis");
    private static final Aspect AQUA = aspect("aqua");

    private static final java.util.function.Function<String, Aspect> RESOLVER = NodeBehaviorMathTest::aspect;

    /**
     * Insertion-ordered map for tests that pin WHICH aspect a uniform pick takes
     * (index 0). Map.of's iteration order is unspecified by the JDK and was
     * observed to differ between JVM invocations — never rely on it.
     */
    private static Map<String, Integer> orderedMap(final Object... kv) {
        final Map<String, Integer> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }

    private static NodeRuntime node(final int x, final int y, final int z, final String type,
            final String modifier, final Map<String, Integer> aspects, final Map<String, Integer> base,
            final long lastActive) {
        return NodeRuntime.fromRecord(new WorldMagicData.Node(x, y, z, type, modifier, 100,
                aspects, base, lastActive), RESOLVER, () -> CLOCK);
    }

    private static NodeRuntime node(final int x, final int y, final int z, final Map<String, Integer> aspects,
            final Map<String, Integer> base) {
        return node(x, y, z, "normal", null, aspects, base, 0L);
    }

    /** Deterministic RNG: scripted nextInt/nextBoolean answers, bounds recorded for formula pins. */
    private static final class ScriptedRandom extends Random {
        private final Deque<Integer> ints = new ArrayDeque<>();
        private final Deque<Boolean> bools = new ArrayDeque<>();
        final List<Integer> seenBounds = new ArrayList<>();

        /** nextInt answers come from {@code scripted} first, then 0. */
        ScriptedRandom(final Integer... scripted) {
            ints.addAll(List.of(scripted));
        }

        ScriptedRandom ints(final Integer... values) {
            ints.addAll(List.of(values));
            return this;
        }

        ScriptedRandom bools(final Boolean... values) {
            bools.addAll(List.of(values));
            return this;
        }

        @Override
        public int nextInt(final int bound) {
            seenBounds.add(bound);
            return ints.isEmpty() ? 0 : ints.poll();
        }

        @Override
        public boolean nextBoolean() {
            return bools.isEmpty() ? false : bools.poll();
        }
    }

    private static final class FakeContext implements NodeRuntime.Context {
        final Map<String, NodeRuntime> neighbors = new HashMap<>();
        int stabilizer;
        boolean hasTaint;
        boolean taintAt;
        final List<int[]> paintedTaint = new ArrayList<>();
        boolean hasEerie;
        final List<int[]> paintedEerie = new ArrayList<>();
        boolean hasMagicalForest;
        boolean magicalForestAt;
        final List<int[]> paintedMagicalForest = new ArrayList<>();
        boolean magicalLog;
        int surface = 100;
        int[] hit;
        float hardness = 1.0F;
        boolean air;
        final List<int[]> broken = new ArrayList<>();
        final List<Object[]> orbs = new ArrayList<>();

        @Override
        public NodeRuntime neighborAt(final int x, final int y, final int z) {
            return neighbors.get(x + ":" + y + ":" + z);
        }

        @Override
        public int stabilizerAt(final int x, final int y, final int z) {
            return stabilizer;
        }

        @Override
        public boolean isTaintBiomeAt(final int x, final int z) {
            return taintAt;
        }

        @Override
        public boolean hasTaintBiome() {
            return hasTaint;
        }

        @Override
        public void paintTaintBiome(final int x, final int y, final int z) {
            paintedTaint.add(new int[] {x, y, z});
        }

        @Override
        public boolean isEerieBiomeAt(final int x, final int z) {
            return false;
        }

        @Override
        public boolean hasEerieBiome() {
            return hasEerie;
        }

        @Override
        public void paintEerieBiome(final int x, final int y, final int z) {
            paintedEerie.add(new int[] {x, y, z});
        }

        @Override
        public boolean isMagicalForestBiomeAt(final int x, int z) {
            return magicalForestAt;
        }

        @Override
        public boolean hasMagicalForestBiome() {
            return hasMagicalForest;
        }

        @Override
        public void paintMagicalForestBiome(final int x, final int y, final int z) {
            paintedMagicalForest.add(new int[] {x, y, z});
        }

        @Override
        public boolean isMagicalLogAt(final int x, final int y, final int z) {
            return magicalLog;
        }

        @Override
        public int surfaceHeight(final int x, final int z) {
            return surface;
        }

        @Override
        public boolean isAir(final int x, final int y, final int z) {
            return air;
        }

        @Override
        public float blockHardness(final int x, final int y, final int z) {
            return hardness;
        }

        @Override
        public int[] rayTraceHit(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
            return hit;
        }

        @Override
        public void breakBlock(final int x, final int y, final int z) {
            broken.add(new int[] {x, y, z});
        }

        @Override
        public void spawnAspectOrb(final int x, final int y, final int z, final Aspect aspect, final int amount) {
            orbs.add(new Object[] {x, y, z, aspect, amount});
        }
    }

    private static int visSize(final NodeRuntime node) {
        return node.getAspects().visSize();
    }


    // ------------------------------------------------------------- regen periods

    @Test
    void regenPeriodsMatchTheModifierTable() {
        // TileNode L661-674: 600 base, 400 bright, 900 pale, 0 fading.
        assertEquals(600, node(0, 64, 0, "normal", null, Map.of(), null, 0L).regeneration());
        assertEquals(400, node(0, 64, 0, "normal", "bright", Map.of(), null, 0L).regeneration());
        assertEquals(900, node(0, 64, 0, "normal", "pale", Map.of(), null, 0L).regeneration());
        assertEquals(0, node(0, 64, 0, "normal", "fading", Map.of(), null, 0L).regeneration());
    }

    @Test
    void stabilizerLockMultipliesTheRegenPeriod() {
        // TileNode L676-682: lock 1 doubles, lock 2 x20; TileNode L959-961 forces
        // the recompute when the lock changes. count starts at 1 because the real
        // tick loop increments before the handlers (TileNode L111) — count 0 would
        // also fire the 1200-spiral gate (0 % 1200 == 0).
        final FakeContext ctx = new FakeContext();
        ctx.stabilizer = 1;
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 0), Map.of("aer", 5));
        n.setCount(1);
        n.checkLock(ctx);
        assertEquals((byte) 1, n.getLock());
        n.handleRecharge(ctx, new ScriptedRandom(), () -> CLOCK);
        assertEquals(1200, n.regeneration());

        ctx.stabilizer = 2;
        n.checkLock(ctx);
        n.handleRecharge(ctx, new ScriptedRandom(), () -> CLOCK);
        assertEquals(12000, n.regeneration());
    }

    // ------------------------------------------------------------- production

    @Test
    void productionAddsOneVisAtTheRegenPeriod() {
        // TileNode L763-770: count % regeneration == 0, one unit to a random
        // aspect below its base, lastActive stamped.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 0, "ignis", 0), Map.of("aer", 3, "ignis", 3));
        n.setCount(599);
        assertFalse(n.handleRecharge(new FakeContext(), new ScriptedRandom(), () -> CLOCK));
        n.setCount(600);
        assertTrue(n.handleRecharge(new FakeContext(), new ScriptedRandom(0), () -> CLOCK));
        assertEquals(1, visSize(n));
        assertEquals(CLOCK, nodeLastActive(n));
    }

    private static long nodeLastActive(final NodeRuntime n) {
        return n.toRecord().lastActive();
    }

    @Test
    void fadingNodesNeverProduce() {
        final NodeRuntime n = node(0, 64, 0, "normal", "fading", Map.of("aer", 0), Map.of("aer", 3), 0L);
        n.setCount(600);
        assertFalse(n.handleRecharge(new FakeContext(), new ScriptedRandom(), () -> CLOCK));
        n.setCount(60000);
        assertFalse(n.handleRecharge(new FakeContext(), new ScriptedRandom(), () -> CLOCK));
        assertEquals(0, visSize(n));
    }

    @Test
    void waitSuppressesProductionAndCountsDown() {
        // TileNode L760-762: wait-- before the production gate.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 0), Map.of("aer", 3));
        n.setWait(5);
        n.setCount(600);
        assertFalse(n.handleRecharge(new FakeContext(), new ScriptedRandom(), () -> CLOCK));
        assertEquals(4, n.getWait());
        assertEquals(0, visSize(n));
    }

    @Test
    void catchUpGrantsOwedProductionCappedAtTheVisBase() {
        // TileNode L685-703: amt = (now - lastActive) / (regen * 75ms), capped at
        // aspectsBase.visSize(); here 10 owed, base total 6 -> 6 produced.
        final long stale = CLOCK - 600L * 75L * 10L;
        final NodeRuntime n = node(0, 64, 0, "normal", null, Map.of("aer", 0, "ignis", 0),
                Map.of("aer", 3, "ignis", 3), stale);
        assertTrue(n.catchUp());
        // Quirk kept: the original's catch-up production never sets the change
        // flag (TileNode L685-703) — it mutates the container silently.
        assertFalse(n.handleRecharge(new FakeContext(), new ScriptedRandom(), () -> CLOCK));
        assertFalse(n.catchUp());
        assertEquals(6, visSize(n));
    }

    @Test
    void noCatchUpWithoutLastActiveOrInsideTheWindow() {
        // TileNode L285-289: lastActive <= 0 never catches up; a recent node neither.
        assertFalse(node(0, 64, 0, "normal", null, Map.of("aer", 0), Map.of("aer", 3), 0L).catchUp());
        final long recent = CLOCK - 600L * 75L;
        assertFalse(node(0, 64, 0, "normal", null, Map.of("aer", 0), Map.of("aer", 3), recent).catchUp());
    }

    // ----------------------------------------------------------- death spiral

    @Test
    void deathSpiralDegradesTheVisBaseOfEmptyAspects() {
        // TileNode L707-712: at count % 1200, an empty aspect loses one base; the
        // 1/20 removal roll (scripted to miss) keeps the entry.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 0), Map.of("aer", 3));
        n.setCount(1200);
        n.handleRecharge(new FakeContext(), new ScriptedRandom(1), () -> CLOCK);
        assertEquals(2, n.getNodeVisBase(AER));
        assertEquals(1, n.getAspects().size());
        assertFalse(n.isDead());
    }

    @Test
    void deathSpiralRemovesAtZeroBaseAndKillsTheNode() {
        // TileNode L713-748: base <= 0 forces removal; an empty container kills.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 0), Map.of("aer", 0));
        n.setCount(1200);
        n.handleRecharge(new FakeContext(), new ScriptedRandom(1, 1), () -> CLOCK);
        assertEquals(0, n.getAspects().size());
        assertTrue(n.isDead());
    }

    @Test
    void deathSpiralDowngradesModifiersBrightToNullToPaleToFading() {
        // TileNode L716-725: on removal, 1/5: BRIGHT -> none, none -> PALE;
        // then PALE -> FADING at another 1/5.
        final NodeRuntime bright = node(0, 64, 0, "normal", "bright", Map.of("aer", 0), Map.of("aer", 0), 0L);
        bright.setCount(1200);
        bright.handleRecharge(new FakeContext(), new ScriptedRandom(1, 0, 1), () -> CLOCK);
        assertNull(bright.getNodeModifier());

        final NodeRuntime plain = node(0, 64, 0, "normal", null, Map.of("aer", 0), Map.of("aer", 0), 0L);
        plain.setCount(1200);
        plain.handleRecharge(new FakeContext(), new ScriptedRandom(1, 0, 1), () -> CLOCK);
        assertEquals(NodeModifier.PALE, plain.getNodeModifier());

        final NodeRuntime pale = node(0, 64, 0, "normal", "pale", Map.of("aer", 0), Map.of("aer", 0), 0L);
        pale.setCount(1200);
        pale.handleRecharge(new FakeContext(), new ScriptedRandom(1, 0, 0, 0), () -> CLOCK);
        assertEquals(NodeModifier.FADING, pale.getNodeModifier());
    }

    // -------------------------------------------------------------- discharge

    @Test
    void dischargeMovesVisFromRicherToPoorerNeighbor() {
        // TileNode L611-620 branch 1: this below its base -> it receives from the
        // poorer neighbor, and the neighbor's regen is halved into wait.
        final FakeContext ctx = new FakeContext();
        final NodeRuntime self = node(0, 64, 0, Map.of("aer", 4), Map.of("aer", 10));
        final NodeRuntime neighbor = node(1, 64, 0, Map.of("aer", 1), Map.of("aer", 1));
        ctx.neighbors.put("1:64:0", neighbor);
        self.setCount(2);
        // offsets: dx=1 (5-4), dy=0 (2-2), dz=0 (3-3); pick index 0.
        assertTrue(self.handleDischarge(ctx, new ScriptedRandom(5, 4, 2, 2, 3, 3, 0)));
        assertEquals(5, self.getAspects().getAmount(AER));
        assertEquals(0, neighbor.getAspects().getAmount(AER));
        assertEquals(300, neighbor.getWait()); // neighbor regen 600 / 2 (TileNode L646)
    }

    @Test
    void dischargeAtBaseGrowsOwnBaseAndCanDrainNeighborBase() {
        // TileNode L621-640 branch 2: at/over base -> neighbor loses; base-add
        // roll hits (nextInt(1 + base/1.0) == 0); neighbor-base drain roll hits.
        final FakeContext ctx = new FakeContext();
        final NodeRuntime self = node(0, 64, 0, Map.of("aer", 5), Map.of("aer", 5));
        final NodeRuntime neighbor = node(1, 64, 0, Map.of("aer", 1), Map.of("aer", 1));
        ctx.neighbors.put("1:64:0", neighbor);
        self.setCount(2);
        final ScriptedRandom rand = new ScriptedRandom(5, 4, 2, 2, 3, 3, 0, 0, 0);
        assertTrue(self.handleDischarge(ctx, rand));
        assertEquals(6, self.getNodeVisBase(AER));
        assertEquals(0, neighbor.getNodeVisBase(AER));
        assertEquals(0, neighbor.getAspects().getAmount(AER));
        assertEquals(300, neighbor.getWait());
        // The base-add roll asked nextInt(1 + 5/1.0) = nextInt(6) (TileNode L623).
        assertTrue(rand.seenBounds.contains(6));
    }

    @Test
    void dischargeIsSuppressedByLockOneAndFading() {
        final FakeContext ctx = new FakeContext();
        ctx.stabilizer = 1;
        final NodeRuntime locked = node(0, 64, 0, Map.of("aer", 4), Map.of("aer", 10));
        locked.checkLock(ctx);
        locked.setCount(2);
        ctx.neighbors.put("1:64:0", node(1, 64, 0, Map.of("aer", 1), Map.of("aer", 1)));
        assertFalse(locked.handleDischarge(ctx, new ScriptedRandom(5, 4, 2, 2, 3, 3, 0)));

        final NodeRuntime fading = node(0, 64, 0, "normal", "fading", Map.of("aer", 4), Map.of("aer", 10), 0L);
        fading.setCount(2);
        assertFalse(fading.handleDischarge(ctx, new ScriptedRandom(5, 4, 2, 2, 3, 3, 0)));
    }

    @Test
    void paleDischargeRollsHalfTheTimeAtEveryThirdTick() {
        // TileNode L585 + L592: interval 3, then a coin flip skips.
        final FakeContext ctx = new FakeContext();
        ctx.neighbors.put("1:64:0", node(1, 64, 0, Map.of("aer", 1), Map.of("aer", 1)));
        final NodeRuntime pale = node(0, 64, 0, "normal", "pale", Map.of("aer", 4), Map.of("aer", 10), 0L);
        pale.setCount(1);
        assertFalse(pale.handleDischarge(ctx, new ScriptedRandom()));
        pale.setCount(2);
        assertFalse(pale.handleDischarge(ctx, new ScriptedRandom()));
        pale.setCount(3);
        assertFalse(pale.handleDischarge(ctx, new ScriptedRandom(5, 4, 2, 2, 3, 3).bools(true)));
        pale.setCount(6);
        assertTrue(pale.handleDischarge(ctx, new ScriptedRandom(5, 4, 2, 2, 3, 3, 0).bools(false)));
    }

    // -------------------------------------------------------------- stability

    @Test
    void unstableUnlockedNodeSpendsOnePrimalOnAnOrb() {
        // TileNode L806-818: coin flip, then takeRandomPrimalFromSource + orb.
        final FakeContext ctx = new FakeContext();
        final NodeRuntime n = node(0, 64, 0, "unstable", null,
                orderedMap("aer", 2, "ignis", 1), orderedMap("aer", 2, "ignis", 1), 0L);
        n.setCount(100);
        assertTrue(n.handleNodeStability(ctx, new ScriptedRandom(0).bools(true)));
        assertEquals(1, ctx.orbs.size());
        assertEquals(AER, ctx.orbs.get(0)[3]);
        assertEquals(1, n.getAspects().getAmount(AER));
        assertEquals(1, n.getAspects().getAmount(IGNIS));
    }

    @Test
    void lockedUnstableNodeConvertsToNormalAtTheLockScaledRoll() {
        // TileNode L819-822: nextInt(10000 / lock) == 42.
        final FakeContext ctx = new FakeContext();
        ctx.stabilizer = 1;
        final NodeRuntime n = node(0, 64, 0, "unstable", null, Map.of(), null, 0L);
        n.checkLock(ctx);
        n.setCount(100);
        final ScriptedRandom rand = new ScriptedRandom(42);
        assertTrue(n.handleNodeStability(ctx, rand.bools(true)));
        assertEquals(NodeType.NORMAL, n.getNodeType());
        assertTrue(rand.seenBounds.contains(10000));

        ctx.stabilizer = 2;
        final NodeRuntime n2 = node(0, 64, 0, "unstable", null, Map.of(), null, 0L);
        n2.checkLock(ctx);
        n2.setCount(100);
        final ScriptedRandom rand2 = new ScriptedRandom(42);
        n2.handleNodeStability(ctx, rand2.bools(true));
        assertTrue(rand2.seenBounds.contains(5000)); // 10000 / lock 2
    }

    @Test
    void lockedFadingNodeRestoresToPaleAtTheLockScaledRoll() {
        // TileNode L824-827: nextInt(12500 / lock) == 69.
        final FakeContext ctx = new FakeContext();
        ctx.stabilizer = 1;
        final NodeRuntime n = node(0, 64, 0, "normal", "fading", Map.of(), null, 0L);
        n.checkLock(ctx);
        n.setCount(100);
        final ScriptedRandom rand = new ScriptedRandom(69);
        assertTrue(n.handleNodeStability(ctx, rand));
        assertEquals(NodeModifier.PALE, n.getNodeModifier());
        assertTrue(rand.seenBounds.contains(12500));
    }

    // ----------------------------------------------------------------- taint

    @Test
    void taintBiomeConvertsForeignNodesAtOneInFiveHundred() {
        // TileNode L792-799: non-pure non-tainted node standing in taint.
        final FakeContext ctx = new FakeContext();
        ctx.hasTaint = true;
        ctx.taintAt = true;
        final NodeRuntime n = node(0, 64, 0, "normal", null, Map.of(), null, 0L);
        n.setCount(100);
        assertTrue(n.handleTaintNode(ctx, new ScriptedRandom(0)));
        assertEquals(NodeType.TAINTED, n.getNodeType());
        assertEquals(-1, n.regeneration()); // nodeChange (TileNode L798)
    }

    @Test
    void taintNodePaintsTaintBiomeAroundItself() {
        // TileNode L774-782: every 50 ticks, a random column in r8 becomes taint
        // unless it already is.
        final FakeContext ctx = new FakeContext();
        ctx.hasTaint = true;
        final NodeRuntime n = node(0, 64, 0, "tainted", null, Map.of(), null, 0L);
        n.setCount(50);
        n.handleTaintNode(ctx, new ScriptedRandom(7, 0, 3, 3));
        assertEquals(1, ctx.paintedTaint.size());
        assertEquals(7, ctx.paintedTaint.get(0)[0]);
        assertEquals(64, ctx.paintedTaint.get(0)[1]); // painted at the node's y

        ctx.taintAt = true;
        ctx.paintedTaint.clear();
        n.handleTaintNode(ctx, new ScriptedRandom(7, 0, 3, 3));
        assertTrue(ctx.paintedTaint.isEmpty());
    }

    @Test
    void biomePaintingIsInertUntilBiomesAreRegistered() {
        // The port has no taint/eerie/magical-forest biomes yet (TC-M5-04+);
        // the behaviour must be a no-op, not a crash.
        final FakeContext ctx = new FakeContext();
        final NodeRuntime dark = node(0, 64, 0, "dark", null, Map.of(), null, 0L);
        dark.setCount(50);
        assertFalse(dark.handleDarkNode(ctx, new ScriptedRandom()));
        assertTrue(ctx.paintedEerie.isEmpty());
        final NodeRuntime pure = node(0, 64, 0, "pure", null, Map.of(), null, 0L);
        pure.setCount(50);
        assertFalse(pure.handleTaintNode(ctx, new ScriptedRandom(0)));
        assertFalse(pure.handlePureNode(ctx, new ScriptedRandom()));
        assertTrue(ctx.paintedMagicalForest.isEmpty());
    }

    @Test
    void pureNodeConvertsTaintUnderMagicalLogToMagicalForest() {
        // TileNode L844-848: taint column, or the node standing on a magical log.
        final FakeContext ctx = new FakeContext();
        ctx.hasMagicalForest = true;
        ctx.taintAt = true;
        final NodeRuntime pure = node(0, 64, 0, "pure", null, Map.of(), null, 0L);
        pure.setCount(50);
        pure.handlePureNode(ctx, new ScriptedRandom(4, 0, 2, 2));
        assertEquals(1, ctx.paintedMagicalForest.size());

        ctx.taintAt = false;
        ctx.paintedMagicalForest.clear();
        pure.handlePureNode(ctx, new ScriptedRandom(4, 0, 2, 2));
        assertTrue(ctx.paintedMagicalForest.isEmpty());

        ctx.magicalLog = true;
        pure.handlePureNode(ctx, new ScriptedRandom(4, 0, 2, 2));
        assertEquals(1, ctx.paintedMagicalForest.size());
    }

    // ---------------------------------------------------------------- hungry

    @Test
    void hungryNodeBreaksSoftBlocksAlongTheRay() {
        // TileNode L913-938: r16 target clamped to surface, ray hit within
        // squared 256, non-air, hardness in [0,5).
        final FakeContext ctx = new FakeContext();
        ctx.hit = new int[] {3, 64, 0};
        ctx.hardness = 1.5F;
        final NodeRuntime n = node(0, 64, 0, "hungry", null, Map.of(), null, 0L);
        n.setCount(50);
        n.handleHungryNode(ctx, new ScriptedRandom(3, 0, 0, 0, 0, 0));
        assertEquals(1, ctx.broken.size());
        assertEquals(3, ctx.broken.get(0)[0]);
    }

    @Test
    void hungryNodeSkipsAirHardAndOutOfReachTargets() {
        final FakeContext ctx = new FakeContext();
        ctx.hit = new int[] {3, 64, 0};
        ctx.hardness = 50.0F; // obsidian-class: >= 5 never breaks (TileNode L932)
        final NodeRuntime n = node(0, 64, 0, "hungry", null, Map.of(), null, 0L);
        n.setCount(50);
        n.handleHungryNode(ctx, new ScriptedRandom(3, 0, 0, 0, 0, 0));
        assertTrue(ctx.broken.isEmpty());

        ctx.hardness = -1.0F; // unbreakable
        n.handleHungryNode(ctx, new ScriptedRandom(3, 0, 0, 0, 0, 0));
        assertTrue(ctx.broken.isEmpty());

        ctx.hardness = 1.0F;
        ctx.air = true;
        n.handleHungryNode(ctx, new ScriptedRandom(3, 0, 0, 0, 0, 0));
        assertTrue(ctx.broken.isEmpty());

        ctx.air = false;
        ctx.hit = new int[] {20, 64, 0}; // squared distance 400 >= 256
        n.handleHungryNode(ctx, new ScriptedRandom(3, 0, 0, 0, 0, 0));
        assertTrue(ctx.broken.isEmpty());
    }

    @Test
    void hungryTargetIsClampedToTheSurface() {
        // TileNode L919-921: ty above the column top is pulled down before the ray.
        final FakeContext ctx = new FakeContext();
        ctx.surface = 70;
        ctx.hit = null;
        final NodeRuntime n = node(0, 64, 0, "hungry", null, Map.of(), null, 0L);
        n.setCount(50);
        // Engine draw order is tx-pair, then ty-pair, then tz-pair: dy = 15 - 0
        // -> ty = 79 > surface 70 -> clamped; record the ray end.
        final int[][] captured = new int[1][];
        final NodeRuntime.Context recording = new NodeRuntime.Context() {
            @Override
            public NodeRuntime neighborAt(final int x, final int y, final int z) {
                return null;
            }

            @Override
            public int surfaceHeight(final int x, final int z) {
                return 70;
            }

            @Override
            public int[] rayTraceHit(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
                captured[0] = new int[] {x1, y1, z1};
                return null;
            }
        };
        n.handleHungryNode(recording, new ScriptedRandom(0, 0, 15, 0, 0, 0));
        assertEquals(70, captured[0][1]);
    }

    // ---------------------------------------------------------- container ops

    @Test
    void addToContainerClampsAtTheVisBase() {
        // TileNode L177-182.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 2), Map.of("aer", 5));
        assertEquals(0, n.addToContainer(AER, 3)); // 2+3 <= 5
        assertEquals(5, n.getAspects().getAmount(AER));
        assertEquals(3, n.addToContainer(AER, 3)); // full
        assertEquals(5, n.getAspects().getAmount(AER));
    }

    @Test
    void takeFromContainerIsAllOrNothing() {
        // TileNode L184-187 via AspectList.reduce.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 2), Map.of("aer", 5));
        assertFalse(n.takeVis(AER, 3));
        assertEquals(2, n.getAspects().getAmount(AER));
        assertTrue(n.takeVis(AER, 2));
        assertEquals(0, n.getAspects().getAmount(AER));
    }

    @Test
    void setNodeVisBaseMergesUpAndReducesDown() {
        // TileNode L237-245.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 0), Map.of("aer", 5));
        n.setNodeVisBase(AER, (short) 8);
        assertEquals(8, n.getNodeVisBase(AER));
        n.setNodeVisBase(AER, (short) 3);
        assertEquals(3, n.getNodeVisBase(AER));
        n.setNodeVisBase(AER, (short) 0);
        assertEquals(0, n.getNodeVisBase(AER));
    }

    @Test
    void chooseRandomFilteredHonoursRoomAndPreserve() {
        // TileNode L195-211: filter amount > 0, node amount > (preserve ? 1 : 0).
        final NodeRuntime n = node(0, 64, 0, orderedMap("aer", 1, "ignis", 3),
                orderedMap("aer", 5, "ignis", 5));
        final AspectList room = new AspectList();
        room.add(AER, 1);
        room.add(IGNIS, 1);
        final Aspect preserved = n.chooseRandomFilteredFromSource(room, true, new ScriptedRandom(0));
        assertEquals(IGNIS, preserved); // aer has exactly 1: preserved
        final Aspect any = n.chooseRandomFilteredFromSource(room, false, new ScriptedRandom(0));
        assertEquals(AER, any); // without preserve, aer (index 0) is eligible
        final AspectList noRoom = new AspectList();
        noRoom.add(AER, 0);
        noRoom.add(IGNIS, 0);
        assertNull(n.chooseRandomFilteredFromSource(noRoom, false, new ScriptedRandom()));
    }

    @Test
    void takeRandomPrimalFromSourceGuardsAgainstEmpty() {
        final NodeRuntime n = node(0, 64, 0, Map.of("lux", 3), Map.of("lux", 3));
        assertNull(n.takeRandomPrimalFromSource()); // compounds are not primals
        final NodeRuntime withPrimal = node(0, 64, 0, Map.of("aqua", 1), Map.of("aqua", 1));
        assertEquals(AQUA, withPrimal.takeRandomPrimalFromSource());
        assertEquals(0, withPrimal.getAspects().getAmount(AQUA));
    }

    // ------------------------------------------------------------- persistence

    @Test
    void recordRoundTripCarriesBaseLastActiveAndIdentity() {
        final NodeRuntime n = node(0, 64, 0, "dark", "pale", Map.of("aer", 2), Map.of("aer", 5), 12345L);
        final WorldMagicData.Node record = n.toRecord();
        assertEquals("dark", record.type());
        assertEquals("pale", record.modifier());
        assertEquals(Map.of("aer", 2), record.aspects());
        assertEquals(Map.of("aer", 5), record.aspectsBase());
        assertEquals(12345L, record.lastActive());

        final NodeRuntime back = NodeRuntime.fromRecord(record, RESOLVER, () -> CLOCK);
        assertEquals(2, back.getAspects().getAmount(AER));
        assertEquals(5, back.getNodeVisBase(AER));
        assertEquals(NodeType.DARK, back.getNodeType());
        assertEquals(NodeModifier.PALE, back.getNodeModifier());
        assertFalse(back.isDead());
    }

    @Test
    void missingBaseCopiesTheContainerAsTheOriginalSetAspectsDoes() {
        // TileNode L171-174: generation writes aspects only; base = aspects.
        final NodeRuntime n = node(0, 64, 0, Map.of("aer", 4), null);
        assertEquals(4, n.getNodeVisBase(AER));
    }
}
