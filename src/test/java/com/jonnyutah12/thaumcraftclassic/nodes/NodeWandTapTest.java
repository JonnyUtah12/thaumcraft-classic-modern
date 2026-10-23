package com.jonnyutah12.thaumcraftclassic.nodes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-02 acceptance: the wand right-click tap math of the original
 * {@code TileNode.onWandRightClick} server branch (TileNode L385-441) against
 * the TC-M4-02 vis pool. Pure math — the in-game plumbing is covered by
 * {@code NodeBehaviorGameTest}.
 */
class NodeWandTapTest {

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

    private static final java.util.function.Function<String, Aspect> RESOLVER = NodeWandTapTest::aspect;

    private static NodeRuntime node(final Map<String, Integer> aspects, final Map<String, Integer> base) {
        return NodeRuntime.fromRecord(new com.jonnyutah12.thaumcraftclassic.world.WorldMagicData.Node(
                0, 64, 0, "normal", null, 100, aspects, base, 0L), RESOLVER, System::currentTimeMillis);
    }

    // Stored capacity in centi-vis: greatwood rod capacity 50 -> maxVis 5000
    // (WandVis.maxVis(50, false) = capacity x 100, ItemWandCasting L82-84;
    // docs/balance-values.md §2.2). The tap argument stays in whole vis, as in
    // the original (addVis multiplies by 100 internally, L355-356).
    private static final int MAX_VIS = 5000;

    @Test
    void tapSizeIsOnePlusTapperResearch() {
        // TileNode L390-397.
        assertEquals(1, NodeWandTap.tapAmount(false, false));
        assertEquals(2, NodeWandTap.tapAmount(true, false));
        assertEquals(2, NodeWandTap.tapAmount(false, true));
        assertEquals(3, NodeWandTap.tapAmount(true, true));
    }

    @Test
    void researchBackedTapGrowsWithTheBankedTapperEntries() {
        // NODETAPPER1/2 ship in the research graph (data/thaumcraft/research):
        // an unknown player taps once; each completed entry adds one (TileNode
        // L390-397).
        final PlayerKnowledgeData knowledge = PlayerKnowledgeData.empty();
        assertEquals(1, NodeWandTap.tapAmount(knowledge));
        assertEquals(2, NodeWandTap.tapAmount(knowledge.withResearchCompleted("NODETAPPER1")));
        assertEquals(3, NodeWandTap.tapAmount(knowledge
                .withResearchCompleted("NODETAPPER1")
                .withResearchCompleted("NODETAPPER2")));
    }

    @Test
    void preserveNeedsResearchStandingAndNoCheapWand() {
        // TileNode L398-401: sneaking, wood rod, or iron cap each veto.
        assertTrue(NodeWandTap.preserveEligible(false, true, false, false));
        assertFalse(NodeWandTap.preserveEligible(true, true, false, false));
        assertFalse(NodeWandTap.preserveEligible(false, false, false, false));
        assertFalse(NodeWandTap.preserveEligible(false, true, true, false));
        assertFalse(NodeWandTap.preserveEligible(false, true, false, true));
    }

    @Test
    void tapMovesVisFromNodeToWandAndDrainsTheNode() {
        final NodeRuntime n = node(Map.of("aer", 10), Map.of("aer", 10));
        final NodeWandTap.Result r = NodeWandTap.tap(n, VisPool.EMPTY, MAX_VIS, 1, false, new java.util.Random(1));
        assertEquals(AER, r.aspect());
        assertEquals(1, r.taken());
        assertEquals(100, r.pool().getVis(AER)); // centi-vis (TC-M4-02)
        assertEquals(9, n.getAspects().getAmount(AER));
    }

    @Test
    void tapIsClampedToWhatTheNodeHolds() {
        // TileNode L405-408: tap > held -> tap = held.
        final NodeRuntime n = node(Map.of("aer", 2), Map.of("aer", 10));
        final NodeWandTap.Result r = NodeWandTap.tap(n, VisPool.EMPTY, MAX_VIS, 3, false, new java.util.Random(1));
        assertEquals(2, r.taken());
        assertEquals(0, n.getAspects().getAmount(AER));
    }

    @Test
    void preserveKeepsTheLastUnitOfAnAspect() {
        // TileNode L197-201: with preserve an aspect holding only one unit is not
        // even a candidate (amount > min=1 required) — the tap picks nothing.
        final NodeRuntime n = node(Map.of("aer", 1), Map.of("aer", 10));
        final NodeWandTap.Result r = NodeWandTap.tap(n, VisPool.EMPTY, MAX_VIS, 1, true, new java.util.Random(1));
        assertNull(r.aspect());
        assertEquals(0, r.taken());
        assertEquals(1, n.getAspects().getAmount(AER));
        assertEquals(VisPool.EMPTY, r.pool());

        // TileNode L405-411: tap clamped to held, then preserve && tap == held
        // -> tap--: from 2 held with a tap of 3, only 1 is taken.
        final NodeRuntime two = node(Map.of("aer", 2), Map.of("aer", 10));
        final NodeWandTap.Result r2 = NodeWandTap.tap(two, VisPool.EMPTY, MAX_VIS, 3, true, new java.util.Random(1));
        assertEquals(AER, r2.aspect());
        assertEquals(1, r2.taken());
        assertEquals(1, two.getAspects().getAmount(AER));
    }

    @Test
    void fullPoolTakesNothingFromTheNode() {
        // TileNode L414-416: addVis returns the leftover; rem == tap -> no drain.
        final NodeRuntime n = node(Map.of("aer", 10), Map.of("aer", 10));
        final VisPool full = WandVis.addVis(VisPool.EMPTY, AER,
                MAX_VIS / WandVis.CENTI_VIS_PER_VIS, MAX_VIS).pool();
        final NodeWandTap.Result r = NodeWandTap.tap(n, full, MAX_VIS, 1, false, new java.util.Random(1));
        assertEquals(0, r.taken());
        assertEquals(10, n.getAspects().getAmount(AER));
        assertEquals(full, r.pool());
    }

    @Test
    void partialRoomDrainsOnlyWhatWasStored() {
        // TileNode L415-420: rem < tap -> take (tap - rem) from the node.
        // Pool at 49 real vis of a 50-real capacity: exactly 1 vis of room.
        final NodeRuntime n = node(Map.of("aer", 10), Map.of("aer", 10));
        final VisPool nearlyFull = WandVis.addVis(VisPool.EMPTY, AER,
                MAX_VIS / WandVis.CENTI_VIS_PER_VIS - 1, MAX_VIS).pool();
        final NodeWandTap.Result r = NodeWandTap.tap(n, nearlyFull, MAX_VIS, 3, false, new java.util.Random(1));
        assertEquals(1, r.taken()); // only 1 vis of room
        assertEquals(9, n.getAspects().getAmount(AER));
        assertEquals(MAX_VIS, r.pool().getVis(AER)); // centi: capacity reached
    }

    @Test
    void emptyOrRoomlessWandGetsNothing() {
        final NodeRuntime empty = node(Map.of(), Map.of());
        assertNull(NodeWandTap.tap(empty, VisPool.EMPTY, MAX_VIS, 1, false, new java.util.Random(1)).aspect());
        final NodeRuntime n = node(Map.of("aer", 5), Map.of("aer", 5));
        final VisPool full = WandVis.addVis(VisPool.EMPTY, AER,
                MAX_VIS / WandVis.CENTI_VIS_PER_VIS, MAX_VIS).pool();
        // No room for aer; ignis is not in the node: no candidate at all.
        final NodeWandTap.Result r = NodeWandTap.tap(n, full, MAX_VIS, 1, false, new java.util.Random(1));
        assertNull(r.aspect());
        assertEquals(5, n.getAspects().getAmount(AER));
    }
}
