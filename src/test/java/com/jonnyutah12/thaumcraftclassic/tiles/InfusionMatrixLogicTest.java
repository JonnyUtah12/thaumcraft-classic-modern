package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-04: the runic matrix scan and instability math, ported unchanged from
 * {@code TileInfusionMatrix.getSurroundings}/{@code craftCycle} and pinned
 * against docs/balance-values.md §5.
 */
class InfusionMatrixLogicTest {

    private static final BlockPos MATRIX = new BlockPos(0, 10, 0);

    /** Set-membership probe: pedestals/pillars/stabilisers are explicit sets. */
    private static InfusionMatrixLogic.WorldProbe probe(final Set<BlockPos> pedestals,
                                                       final Set<BlockPos> filled,
                                                       final Set<BlockPos> pillars,
                                                       final Set<BlockPos> stabilisers) {
        return new InfusionMatrixLogic.WorldProbe() {
            @Override
            public boolean isPedestal(final BlockPos pos) {
                return pedestals.contains(pos);
            }

            @Override
            public boolean pedestalHasItem(final BlockPos pos) {
                return filled.contains(pos);
            }

            @Override
            public boolean isPillar(final BlockPos pos) {
                return pillars.contains(pos);
            }

            @Override
            public boolean isStabiliser(final BlockPos pos) {
                return stabilisers.contains(pos);
            }
        };
    }

    private static Set<BlockPos> altarPillars() {
        final Set<BlockPos> pillars = new HashSet<>();
        pillars.add(new BlockPos(1, 8, 1));
        pillars.add(new BlockPos(1, 8, -1));
        pillars.add(new BlockPos(-1, 8, 1));
        pillars.add(new BlockPos(-1, 8, -1));
        return pillars;
    }

    @Test
    void validAltarShapeMatchesOriginal() {
        final Set<BlockPos> centre = new HashSet<>();
        centre.add(new BlockPos(0, 8, 0));
        assertTrue(InfusionMatrixLogic.validLocation(MATRIX,
                probe(centre, Set.of(), altarPillars(), Set.of())));
    }

    @Test
    void missingPillarInvalidatesAltar() {
        final Set<BlockPos> centre = new HashSet<>();
        centre.add(new BlockPos(0, 8, 0));
        final Set<BlockPos> pillars = altarPillars();
        pillars.remove(new BlockPos(-1, 8, -1));
        assertFalse(InfusionMatrixLogic.validLocation(MATRIX,
                probe(centre, Set.of(), pillars, Set.of())));
    }

    @Test
    void missingCentrePedestalInvalidatesAltar() {
        assertFalse(InfusionMatrixLogic.validLocation(MATRIX,
                probe(Set.of(), Set.of(), altarPillars(), Set.of())));
    }

    @Test
    void symmetryCountsPedestalsAndItems() {
        // One pedestal with an item, no mirror: +2 (pedestal) +1 (item) = 3.
        final Set<BlockPos> ped = new HashSet<>();
        ped.add(new BlockPos(2, 8, 0));
        final Set<BlockPos> filled = new HashSet<>(ped);
        assertEquals(3, InfusionMatrixLogic.scan(MATRIX,
                probe(ped, filled, Set.of(), Set.of())).symmetry());
    }

    @Test
    void mirroredPedestalsWithItemsCancel() {
        // Point-mirrored pair, both holding items: +2+1-2-1 = 0.
        final Set<BlockPos> ped = new HashSet<>();
        ped.add(new BlockPos(2, 8, 0));
        ped.add(new BlockPos(-2, 8, 0));
        final Set<BlockPos> filled = new HashSet<>(ped);
        assertEquals(0, InfusionMatrixLogic.scan(MATRIX,
                probe(ped, filled, Set.of(), Set.of())).symmetry());
    }

    @Test
    void unmirroredPedestalsAddUp() {
        // Two pedestals on the same side, one filled: (2+1) + 2 = 5.
        final Set<BlockPos> ped = new HashSet<>();
        ped.add(new BlockPos(2, 8, 0));
        ped.add(new BlockPos(0, 8, 2));
        final Set<BlockPos> filled = Set.of(new BlockPos(2, 8, 0));
        assertEquals(5, InfusionMatrixLogic.scan(MATRIX,
                probe(ped, filled, Set.of(), Set.of())).symmetry());
    }

    @Test
    void stabilisersContributeFractionalSymmetryTruncated() {
        // One stabiliser, no mirror: +0.1 -> truncated into the integer sum.
        final Set<BlockPos> ped = new HashSet<>();
        ped.add(new BlockPos(2, 8, 0)); // +2
        final Set<BlockPos> st = new HashSet<>();
        st.add(new BlockPos(5, 8, 5));  // +0.1
        assertEquals(2, InfusionMatrixLogic.scan(MATRIX,
                probe(ped, Set.of(), Set.of(), st)).symmetry());
    }

    @Test
    void onlyFirstPedestalPerColumnCounts() {
        // Two pedestals stacked in one (x,z) column: only the first (lowest y
        // scan order, yy from +10 down) counts — original skip flag.
        final Set<BlockPos> ped = new HashSet<>();
        ped.add(new BlockPos(2, 8, 0));
        ped.add(new BlockPos(2, 9, 0));
        assertEquals(2, InfusionMatrixLogic.scan(MATRIX,
                probe(ped, Set.of(), Set.of(), Set.of())).symmetry());
    }

    @Test
    void craftStartInstabilityIsSymmetryPlusRecipe() {
        assertEquals(-3, InfusionMatrixLogic.craftStartInstability(-7, 4));
        assertEquals(7, InfusionMatrixLogic.craftStartInstability(3, 4));
    }

    @Test
    void instabilityIncrementsCapAt25() {
        assertEquals(1, InfusionMatrixLogic.cappedIncrement(0));
        assertEquals(25, InfusionMatrixLogic.cappedIncrement(24));
        assertEquals(25, InfusionMatrixLogic.cappedIncrement(25));
    }

    @Test
    void eventFiresOnRollAtOrBelowInstability() {
        assertFalse(InfusionMatrixLogic.eventFires(0, 0));
        assertTrue(InfusionMatrixLogic.eventFires(4, 4));
        assertFalse(InfusionMatrixLogic.eventFires(4, 5));
    }

    @Test
    void eventRollTableMatchesOriginalSwitch() {
        // Original nextInt(21): cases 0,2,10,13 eject type 0; 1,11 type 2;
        // 3,8,14 zap; 4,15 type 5; 5,16 harm; 6,17 type 1; 7 type 4; 9
        // lightning; 12 all-zap; 18 all-harm; 19 type 3; 20 warp.
        assertEquals(InfusionMatrixLogic.EV_EJECT, InfusionMatrixLogic.eventForRoll(0));
        assertEquals(0, InfusionMatrixLogic.ejectType(InfusionMatrixLogic.eventForRoll(13)));
        assertEquals(2, InfusionMatrixLogic.ejectType(InfusionMatrixLogic.eventForRoll(1)));
        assertEquals(InfusionMatrixLogic.EV_ZAP, InfusionMatrixLogic.eventForRoll(3));
        assertTrue(InfusionMatrixLogic.affectsAll(InfusionMatrixLogic.eventForRoll(12)));
        assertFalse(InfusionMatrixLogic.affectsAll(InfusionMatrixLogic.eventForRoll(3)));
        assertEquals(InfusionMatrixLogic.EV_HARM, InfusionMatrixLogic.eventForRoll(5));
        assertTrue(InfusionMatrixLogic.affectsAll(InfusionMatrixLogic.eventForRoll(18)));
        assertEquals(InfusionMatrixLogic.EV_LIGHTNING, InfusionMatrixLogic.eventForRoll(9));
        assertEquals(InfusionMatrixLogic.EV_WARP, InfusionMatrixLogic.eventForRoll(20));
        assertEquals(3, InfusionMatrixLogic.ejectType(InfusionMatrixLogic.eventForRoll(19)));
        assertEquals(1, InfusionMatrixLogic.ejectType(InfusionMatrixLogic.eventForRoll(6)));
        assertEquals(4, InfusionMatrixLogic.ejectType(InfusionMatrixLogic.eventForRoll(7)));
        assertEquals(5, InfusionMatrixLogic.ejectType(InfusionMatrixLogic.eventForRoll(4)));
    }
}
