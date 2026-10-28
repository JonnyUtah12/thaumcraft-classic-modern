package com.jonnyutah12.thaumcraftclassic.tiles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-06-s evidence: the essentia <b>aspect matching</b> rules for jars,
 * pinned as the original's numbers.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/TileJarFillable.java}
 * (+ {@code TileJarFillableVoid.java}). Every assertion below cites the
 * original line it comes from; the rules themselves live in
 * {@link JarStorageLogic}, which is what the jar tiles apply.</p>
 */
class JarStorageLogicTest {

    private static final String AER = "aer";
    private static final String IGNIS = "ignis";
    private static final int MAX = JarStorageLogic.MAX_AMOUNT;

    // --- one aspect per jar, the original's add rule ----------------------

    @Test
    void anEmptyJarTakesTheFirstAspectItIsGiven() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(null, 0, MAX, AER, 1);
        assertEquals(0, add.leftover(), "an empty jar takes everything it is offered");
        assertEquals(AER, add.aspectTag(), "the jar locks onto the aspect it was filled with");
        assertEquals(1, add.amount());
    }

    @Test
    void aHeldAspectStacksIntoTheSameJar() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(AER, 30, MAX, AER, 10);
        assertEquals(0, add.leftover());
        assertEquals(AER, add.aspectTag());
        assertEquals(40, add.amount(), "TileJarFillable.addToContainer: amount += min(tt.amount, max - amount)");
    }

    @Test
    void aForeignAspectIsRefusedWhileTheJarIsNotEmpty() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(AER, 30, MAX, IGNIS, 10);
        assertEquals(10, add.leftover(), "one jar, one aspect: the foreign essentia is handed back");
        assertEquals(AER, add.aspectTag(), "the refusal must not rewrite the jar's aspect");
        assertEquals(30, add.amount());
    }

    @Test
    void aJarDrainedToZeroIsFreeToTakeAnyAspect() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(AER, 0, MAX, IGNIS, 5);
        assertEquals(0, add.leftover());
        assertEquals(IGNIS, add.aspectTag(),
                "the original's `|| amount == 0` clause: an emptied jar re-locks");
        assertEquals(5, add.amount());
    }

    @Test
    void aFullJarRefusesEvenItsOwnAspect() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(AER, MAX, MAX, AER, 4);
        assertEquals(4, add.leftover(), "amount < maxAmount is part of the original's writable test");
        assertEquals(MAX, add.amount());
    }

    @Test
    void aFillIsClippedToTheCapacityAndReportsTheRest() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(AER, 60, MAX, AER, 10);
        assertEquals(MAX, add.amount(), "maxAmount is 64 in TileJarFillable (original ConfigCore jarCapacity)");
        assertEquals(6, add.leftover(), "the leftover is what the transport keeps on the line");
    }

    // --- the void jar: same matching, clipped overflow, no full-stop -------

    @Test
    void theVoidJarClipsTheOverflowInsteadOfRefusingIt() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(IGNIS, 60, MAX, IGNIS, 10, true);
        assertEquals(0, add.leftover(), "TileJarFillableVoid.addToContainer has no room check, it clips");
        assertEquals(MAX, add.amount(), "and the overflow is discarded (void jar)");
    }

    @Test
    void theVoidJarStillHoldsOneAspectOnly() {
        final JarStorageLogic.Add add = JarStorageLogic.addToContainer(IGNIS, 5, MAX, AER, 3, true);
        assertEquals(3, add.leftover());
        assertEquals(IGNIS, add.aspectTag());
        assertEquals(5, add.amount());
    }

    // --- taking essentia out ----------------------------------------------

    @Test
    void takingTheWholeContentsClearsTheAspect() {
        final JarStorageLogic.Take take = JarStorageLogic.takeFromContainer(AER, 8, AER, 8);
        assertTrue(take.taken());
        assertEquals(0, take.amount());
        assertNull(take.aspectTag(), "the original nulls the aspect when the amount reaches 0");
    }

    @Test
    void takingAnAspectTheJarDoesNotHoldIsRefused() {
        final JarStorageLogic.Take take = JarStorageLogic.takeFromContainer(AER, 8, IGNIS, 1);
        assertFalse(take.taken());
        assertEquals(AER, take.aspectTag());
        assertEquals(8, take.amount());
    }

    @Test
    void takingMoreThanTheJarHoldsIsRefusedEntirely() {
        final JarStorageLogic.Take take = JarStorageLogic.takeFromContainer(AER, 5, AER, 6);
        assertFalse(take.taken(), "the original has no partial take");
        assertEquals(5, take.amount());
    }

    @Test
    void aPartialTakeKeepsTheAspect() {
        final JarStorageLogic.Take take = JarStorageLogic.takeFromContainer(AER, 8, AER, 3);
        assertTrue(take.taken());
        assertEquals(AER, take.aspectTag());
        assertEquals(5, take.amount());
    }

    // --- the label (aspect filter) ----------------------------------------

    @Test
    void anUnlabelledJarAcceptsAnyAspectAndALabelMatchesExactly() {
        assertTrue(JarStorageLogic.doesContainerAccept(null, IGNIS));
        assertTrue(JarStorageLogic.doesContainerAccept(AER, AER));
        assertFalse(JarStorageLogic.doesContainerAccept(AER, IGNIS),
                "TileJarFillable.doesContainerAccept compares the aspect tag, not the parents");
    }

    @Test
    void aLabelledJarReportsExactlyWhatItHolds() {
        assertTrue(JarStorageLogic.doesContainerContainAmount(AER, 8, AER, 8));
        assertFalse(JarStorageLogic.doesContainerContainAmount(AER, 7, AER, 8));
        assertFalse(JarStorageLogic.doesContainerContainAmount(AER, 8, IGNIS, 1));
    }

    // --- suction, i.e. how hard a jar pulls on the line -------------------

    @Test
    void aPlainJarPullsAt32AndALabelledJarAt64WhileTheyHaveRoom() {
        assertEquals(32, JarStorageLogic.suctionAmount(false, 0, MAX));
        assertEquals(64, JarStorageLogic.suctionAmount(true, 0, MAX));
        assertEquals(0, JarStorageLogic.suctionAmount(false, MAX, MAX),
                "a full plain jar advertises 0 so the line stops feeding it");
        assertEquals(0, JarStorageLogic.suctionAmount(true, MAX, MAX));
    }

    @Test
    void aVoidJarPullsAt32Or48AndNeverStops() {
        assertEquals(32, JarStorageLogic.voidSuctionAmount(false, 0, MAX));
        assertEquals(48, JarStorageLogic.voidSuctionAmount(true, 0, MAX));
        assertEquals(32, JarStorageLogic.voidSuctionAmount(false, MAX, MAX),
                "TileJarFillableVoid.getSuctionAmount has no full check: a void jar never lets go");
        assertEquals(32, JarStorageLogic.voidSuctionAmount(true, MAX, MAX),
                "labelled, the full void jar drops from 48 to 32, not to 0");
    }

    @Test
    void theMinimumSuctionMatchesTheOriginalPair() {
        assertEquals(32, JarStorageLogic.minimumSuction(false));
        assertEquals(64, JarStorageLogic.minimumSuction(true));
        assertEquals(32, JarStorageLogic.voidMinimumSuction(false));
        assertEquals(48, JarStorageLogic.voidMinimumSuction(true));
    }

    // --- the comparator read ----------------------------------------------

    @Test
    void theSignalIsFloorOfTheFillRatioTimesFourteenPlusOne() {
        assertEquals(0, JarStorageLogic.signalStrength(0, MAX), "an empty jar gives no signal");
        assertEquals(1, JarStorageLogic.signalStrength(1, MAX));
        assertEquals(2, JarStorageLogic.signalStrength(5, MAX));
        assertEquals(8, JarStorageLogic.signalStrength(32, MAX));
        assertEquals(15, JarStorageLogic.signalStrength(MAX, MAX));
        assertEquals(0, JarStorageLogic.signalStrength(4, 0), "a zero capacity must not divide");
    }

    // --- the fill cadence --------------------------------------------------

    @Test
    void aJarPullsOneUnitEveryFiveTicks() {
        assertTrue(JarStorageLogic.fillTick(5));
        assertTrue(JarStorageLogic.fillTick(10));
        assertFalse(JarStorageLogic.fillTick(4), "the original counts `++counter % 5 == 0`");
        assertEquals(1, JarStorageLogic.FILL_PER_CYCLE);
        assertEquals(8, JarStorageLogic.PHIAL_TRANSFER, "a phial moves 8 per fill/drain");
    }
}
