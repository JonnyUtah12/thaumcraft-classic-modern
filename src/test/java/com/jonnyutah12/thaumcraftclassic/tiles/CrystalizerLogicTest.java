package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-07-m evidence: the essentia crystallizer's conversion rules, pinned as
 * the original's numbers, without a {@code Level}.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/TileEssentiaCrystalizer.java}
 * — the suction pair (175-177: {@code facing}-and-empty 128, else 64, powered
 * 0), the any-aspect intake (358-377), the every-fifth-tick pass (203), the vis
 * request clamp (215), the {@code progress + 1 + drained * 2} advance (217),
 * the completion gate (221) and the container arithmetic (82-95, 196). The
 * rules live in {@link CrystalizerLogic}; the tile's container surface and the
 * {@code Aspect}/{@code face} NBT round-trip are pinned headlessly in
 * {@code EssentiaMachinesGameTest.machineSurfaceAndNbtHeadless}.</p>
 *
 * <p>Aspects are built from {@link AspectDefinitions} (the
 * {@code VisCostRulesTest} pattern).</p>
 */
class CrystalizerLogicTest {

    private static final Aspect IGNIS = aspect("ignis");
    private static final Aspect POTENTLY = aspect("potentia");

    private static Aspect aspect(final String tag) {
        return new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(tag))
                .findFirst()
                .orElseThrow());
    }

    // --- suction (TileEssentiaCrystalizer.java:175-177) --------------------

    @Test
    void suctionMatchesOriginal() {
        assertEquals(128, CrystalizerLogic.suction(Direction.DOWN, Direction.DOWN, false, false),
                "facing and empty (176)");
        assertEquals(64, CrystalizerLogic.suction(Direction.DOWN, Direction.DOWN, false, true),
                "loaded (176)");
        assertEquals(64, CrystalizerLogic.suction(Direction.NORTH, Direction.DOWN, false, false),
                "every other face (176)");
        assertEquals(0, CrystalizerLogic.suction(Direction.DOWN, Direction.DOWN, true, false),
                "gettingPower (176)");
    }

    // --- intake (TileEssentiaCrystalizer.java:358-377) ---------------------

    @Test
    void intakeTakesAnythingThroughTheFacingFace() {
        // No primal gate: the crystalliser crystallises anything (367-372).
        assertSame(IGNIS, CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, neighbour(IGNIS, 1, 31, true, 0))));
        assertSame(POTENTLY, CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, neighbour(POTENTLY, 1, 31, true, 0))));
    }

    @Test
    void intakeRefusesTheOriginalRefusals() {
        assertNull(CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, neighbour(null, 0, 31, true, 0))),
                "nothing on offer (364)");
        assertNull(CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, neighbour(IGNIS, 1, 128, true, 0))),
                "neighbour out-sucks: 128 !< 128 (365)");
        assertNull(CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, neighbour(IGNIS, 1, 31, false, 0))),
                "canOutputTo gate (361)");
        assertNull(CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, neighbour(IGNIS, 1, 31, true, 129))),
                "minimumSuction (366)");
        assertNull(CrystalizerLogic.planFill(Direction.DOWN, 128,
                new TubeTransportLogic.Neighbour(Direction.DOWN, null)), "no neighbour (360)");
    }

    @Test
    void inputFaceIsTheStoredFacing() {
        assertTrue(CrystalizerLogic.inputFace(Direction.DOWN, Direction.DOWN), "facing (196)");
        assertFalse(CrystalizerLogic.inputFace(Direction.NORTH, Direction.DOWN), "other face (196)");
    }

    // --- the vis arithmetic (TileEssentiaCrystalizer.java:203-221) ---------

    @Test
    void visRequestIsTheOriginalClamp() {
        // Math.min(20, Math.max(1, (200 - progress) / 2)) — verbatim (215).
        assertEquals(20, CrystalizerLogic.visRequest(0));
        assertEquals(10, CrystalizerLogic.visRequest(180));
        assertEquals(1, CrystalizerLogic.visRequest(198));
        assertEquals(1, CrystalizerLogic.visRequest(199), "floor(0.5) = 0 -> max(1, 0) (215)");
        assertEquals(1, CrystalizerLogic.visRequest(250), "negative -> max(1, ..) (215)");
    }

    @Test
    void advanceAddsOnePlusTwiceTheVis() {
        // progress = progress + 1 + drainVis(...) * 2 — verbatim (217).
        assertEquals(1, CrystalizerLogic.advance(0, 0), "free tick without vis (217)");
        assertEquals(11, CrystalizerLogic.advance(0, 5));
        assertEquals(201, CrystalizerLogic.advance(198, 1), "no clamp — 198+1+1*2 (217)");
        assertEquals(231, CrystalizerLogic.advance(190, 20), "overshoot past 200 stays (217)");
    }

    @Test
    void completeAtTwoHundredOnlyWhenLoaded() {
        assertTrue(CrystalizerLogic.isComplete(IGNIS, 200), "progress >= 200 (221)");
        assertTrue(CrystalizerLogic.isComplete(IGNIS, 250));
        assertFalse(CrystalizerLogic.isComplete(IGNIS, 199));
        assertFalse(CrystalizerLogic.isComplete(null, 250), "aspect != null gate (221)");
    }

    @Test
    void constantsAreTheOriginalsNumbers() {
        assertEquals(5, CrystalizerLogic.PASS_INTERVAL_TICKS, "++count % 5 == 0 (203)");
        assertEquals(200, CrystalizerLogic.PROG_MAX, "progMax (215)");
    }

    // --- the container arithmetic (TileEssentiaCrystalizer.java:82-95, 196) --

    @Test
    void addLeftoverMatchesTheOneUnitContainer() {
        assertEquals(0, CrystalizerLogic.addLeftover(null, 0), "am == 0 no-op (84)");
        assertEquals(0, CrystalizerLogic.addLeftover(null, 1), "empty takes the unit (91)");
        assertEquals(1, CrystalizerLogic.addLeftover(null, 2), "leftover am-1 (91)");
        assertEquals(1, CrystalizerLogic.addLeftover(IGNIS, 1), "full: all back (94)");
        assertEquals(0, CrystalizerLogic.addLeftover(IGNIS, 0), "am == 0 no-op (84)");
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
