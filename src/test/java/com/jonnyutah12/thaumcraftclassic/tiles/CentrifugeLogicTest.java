package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-07-m evidence: the alchemical centrifuge's conversion rules, pinned as
 * the original's numbers, without a {@code Level}.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/TileCentrifuge.java}
 * — the suction gate (lines 148-150: DOWN only, 128/64/0), the compound-only
 * intake (169-179), the draw pass (223-250), the two-component coin-flip split
 * (215-221), the idle-only 5-tick cadence (185-187) and the split gate
 * (191-195). The rules live in {@link CentrifugeLogic}; the tile applies them.
 * The container surface and the {@code aspectIn}/{@code aspectOut}/{@code
 * facing} NBT round-trip are pinned headlessly in
 * {@code EssentiaMachinesGameTest.machineSurfaceAndNbtHeadless} — unit JVMs
 * have no mod registries to construct a {@code BlockEntity} (the
 * {@code TileNBTProbe} lesson).</p>
 *
 * <p>Aspects are built from {@link AspectDefinitions} because unit tests run
 * without the game's {@code NewRegistryEvent} ({@code VisCostRulesTest}
 * pattern).</p>
 */
class CentrifugeLogicTest {

    private static final Aspect IGNIS = aspect("ignis");
    private static final Aspect ORDO = aspect("ordo");
    private static final Aspect POTENTLY = aspect("potentia");

    private static Aspect aspect(final String tag) {
        return new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(tag))
                .findFirst()
                .orElseThrow());
    }

    // --- suction (TileCentrifuge.java:148-150) -----------------------------

    @Test
    void suctionMatchesOriginal() {
        assertEquals(128, CentrifugeLogic.suction(Direction.DOWN, false, false), "free input (149)");
        assertEquals(64, CentrifugeLogic.suction(Direction.DOWN, false, true), "spin in progress (149)");
        assertEquals(0, CentrifugeLogic.suction(Direction.DOWN, true, true), "gettingPower (149)");
        assertEquals(0, CentrifugeLogic.suction(Direction.UP, false, false), "output face never sucks (149)");
        assertEquals(0, CentrifugeLogic.suction(Direction.NORTH, false, false), "side faces (149)");
    }

    // --- intake (TileCentrifuge.java:169-179) ------------------------------

    @Test
    void intakeIsCompoundOnlyOnAFreeSlot() {
        assertTrue(CentrifugeLogic.addEssentiaAllowed(null, POTENTLY), "free slot, compound (169)");
        assertFalse(CentrifugeLogic.addEssentiaAllowed(null, IGNIS), "primals have nothing to split (169)");
        assertFalse(CentrifugeLogic.addEssentiaAllowed(POTENTLY, POTENTLY), "slot busy (169)");
        assertFalse(CentrifugeLogic.addEssentiaAllowed(null, null), "no aspect (169)");
    }

    // --- the coin-flip split (TileCentrifuge.java:215-221) -----------------

    @Test
    void splitIsACoinFlipOverTheTwoComponents() {
        final List<Aspect> comps = POTENTLY.getComponents();
        assertEquals(2, comps.size(), "potentia has two components");
        assertSame(comps.get(0), CentrifugeLogic.split(comps, 0), "rand 0 picks the first (217)");
        assertSame(comps.get(1), CentrifugeLogic.split(comps, 1), "rand 1 picks the second (217)");
        assertTrue(
                comps.stream().anyMatch(a -> a.getTag().equals("ordo"))
                        && comps.stream().anyMatch(a -> a.getTag().equals("ignis")),
                "potentia is ordo + ignis (AspectDefinitions); components are the registry's own instances");
    }

    // --- the draw pass (TileCentrifuge.java:223-250) -----------------------

    @Test
    void drawTakesTheCompoundTheNeighbourOffers() {
        assertSame(POTENTLY, CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(POTENTLY, 1, 16, true, 0))), "offer wins (234-244)");
        assertSame(POTENTLY, CentrifugeLogic.planDraw(64,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(POTENTLY, 1, 16, true, 0))), "busy suction 64 still out-sucks 16 (236)");
    }

    @Test
    void drawRefusesTheOriginalRefusals() {
        assertNull(CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(IGNIS, 1, 16, true, 0))), "primal never (241)");
        assertNull(CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(null, 0, 16, true, 0))), "nothing on offer (234)");
        assertNull(CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(POTENTLY, 1, 128, true, 0))), "neighbour out-sucks: 128 !< 128 (236)");
        assertNull(CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(POTENTLY, 1, 16, false, 0))), "canOutputTo gate (228)");
        assertNull(CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN,
                        neighbour(POTENTLY, 1, 16, true, 129))), "minimumSuction (237)");
        assertNull(CentrifugeLogic.planDraw(128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, null)), "no neighbour (230)");
    }

    // --- the cadence (TileCentrifuge.java:185-195) -------------------------

    @Test
    void drawCadenceIsIdleOnlyEveryFifthTick() {
        assertTrue(CentrifugeLogic.drawPassRuns(false, false, 5), "both slots free, %5 (185)");
        assertFalse(CentrifugeLogic.drawPassRuns(false, false, 6), "not %5 (185)");
        assertFalse(CentrifugeLogic.drawPassRuns(true, false, 5), "input slot busy (185)");
        assertFalse(CentrifugeLogic.drawPassRuns(false, true, 5), "output slot in the way (185)");
        assertEquals(5, CentrifugeLogic.DRAW_INTERVAL_TICKS);
        assertEquals(39, CentrifugeLogic.PROCESS_TICKS, "process = 39 on draw (246)");
    }

    @Test
    void splitFiresAtZeroWithInputAndNoOutput() {
        assertTrue(CentrifugeLogic.splitRuns(true, false, 0), "spin over (191)");
        assertFalse(CentrifugeLogic.splitRuns(true, false, 1), "still spinning (189)");
        assertFalse(CentrifugeLogic.splitRuns(false, false, 0), "no input (191)");
        assertFalse(CentrifugeLogic.splitRuns(true, true, 0), "output slot in the way (191)");
    }

    // --- helpers -------------------------------------------------------------

    /** A stand-in neighbour: the transport contract's observable state. */
    private static IEssentiaTransport neighbour(@Nullable final Aspect essentiaType,
                                                final int essentiaAmount, final int suctionAmount,
                                                final boolean canOutput, final int minimumSuction) {
        return new IEssentiaTransport() {
            @Override
            public boolean isConnectable(final Direction face) {
                return true;
            }

            @Override
            public boolean canInputFrom(final Direction face) {
                return true;
            }

            @Override
            public boolean canOutputTo(final Direction face) {
                return canOutput;
            }

            @Override
            public void setSuction(@Nullable final Aspect aspect, final int amount) {
            }

            @Override
            @Nullable
            public Aspect getSuctionType(final Direction face) {
                return null;
            }

            @Override
            public int getSuctionAmount(final Direction face) {
                return suctionAmount;
            }

            @Override
            public int getMinimumSuction() {
                return minimumSuction;
            }

            @Override
            @Nullable
            public Aspect getEssentiaType(final Direction face) {
                return essentiaType;
            }

            @Override
            public int getEssentiaAmount(final Direction face) {
                return essentiaAmount;
            }

            @Override
            public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
                return 0;
            }

            @Override
            public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
                return 0;
            }

            @Override
            public boolean renderExtendedTube() {
                return false;
            }
        };
    }
}
