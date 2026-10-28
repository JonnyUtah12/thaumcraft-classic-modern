package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-06-s evidence: the arcane furnace's <b>extraction rates</b>, pinned as
 * the original's numbers.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/TileArcaneFurnace.java}
 * ({@code calcCookTime}, {@code getBellows}, the {@code updateEntity} speed
 * block, {@code ejectItem}) and {@code TileArcaneFurnaceNozzle.java}
 * ({@code drawEssentia}, {@code getSuctionAmount}). The rules live in
 * {@link ArcaneFurnaceLogic}; {@link TileArcaneFurnace} applies them.</p>
 */
class ArcaneFurnaceLogicTest {

    // --- cook time: the rate the furnace smelts at -------------------------

    @Test
    void aColdFurnaceWithoutBellowsTakesOneHundredAndFortyTicks() {
        assertEquals(140, ArcaneFurnaceLogic.cookTime(0, 0),
                "TileArcaneFurnace.calcCookTime: (speedyTime > 0 ? 80 : 140) - 20 * bellows");
    }

    @Test
    void everyBellowsCutsTwentyTicks() {
        assertEquals(120, ArcaneFurnaceLogic.cookTime(0, 1));
        assertEquals(100, ArcaneFurnaceLogic.cookTime(0, 2));
        assertEquals(80, ArcaneFurnaceLogic.cookTime(0, 3));
        assertEquals(80, ArcaneFurnaceLogic.cookTime(0, 7),
                "the original caps the count at Math.min(3, bellows) — four bellows are three");
    }

    @Test
    void fireEssentiaHalvesTheCookTime() {
        assertEquals(80, ArcaneFurnaceLogic.cookTime(600, 0),
                "speedyTime > 0 (fed by the nozzle's FIRE draw) drops the base to 80");
        assertEquals(20, ArcaneFurnaceLogic.cookTime(1, 3),
                "the fastest reachable rate: 80 - 60 = 20 ticks");
    }

    @Test
    void speedyTimeDecaysOnePerTickWhileCooking() {
        assertEquals(1, ArcaneFurnaceLogic.SPEEDY_DECAY);
        assertEquals(5, ArcaneFurnaceLogic.VIS_DRAIN_REQUEST,
                "the original drains up to 5 FIRE from the vis net per refill");
    }

    @Test
    void theBellowsCountIsCappedAtThree() {
        assertEquals(0, ArcaneFurnaceLogic.bellows(0));
        assertEquals(3, ArcaneFurnaceLogic.bellows(3));
        assertEquals(3, ArcaneFurnaceLogic.bellows(99));
    }

    // --- the nozzle's essentia draw ---------------------------------------

    @Test
    void theNozzleDrawsOnceEveryFiveTicks() {
        assertTrue(ArcaneFurnaceLogic.nozzleDrawTick(5));
        assertTrue(ArcaneFurnaceLogic.nozzleDrawTick(10));
        assertFalse(ArcaneFurnaceLogic.nozzleDrawTick(4),
                "TileArcaneFurnaceNozzle.drawEssentia: ++drawDelay % 5 == 0");
    }

    @Test
    void aFurnaceOnlyDrawsWhileItIsStarved() {
        assertTrue(ArcaneFurnaceLogic.nozzleCanDraw(0));
        assertTrue(ArcaneFurnaceLogic.nozzleCanDraw(59));
        assertFalse(ArcaneFurnaceLogic.nozzleCanDraw(60),
                "the gate is speedyTime < 60, so a fed furnace stops pulling");
    }

    @Test
    void aStarvedNozzlePullsAtOneHundredTwentyEightAndAFedOneAtZero() {
        assertEquals(128, ArcaneFurnaceLogic.nozzleSuction(0));
        assertEquals(128, ArcaneFurnaceLogic.nozzleSuction(39));
        assertEquals(0, ArcaneFurnaceLogic.nozzleSuction(40),
                "getSuctionAmount: speedyTime < 40 ? 128 : 0 — the line is released once fed");
        assertEquals(0, ArcaneFurnaceLogic.nozzleSuction(600));
    }

    @Test
    void aSuccessfulDrawBuysSixHundredTicksOfFastCooking() {
        assertEquals(610, ArcaneFurnaceLogic.speedyAfterDraw(10));
        assertEquals(600, ArcaneFurnaceLogic.speedyAfterDraw(0));
    }

    // --- experience paid on eject ----------------------------------------

    @Test
    void aRecipeWithNoExperiencePaysNothing() {
        assertEquals(0, ArcaneFurnaceLogic.experience(5, 0.0F, RandomSource.create(1L)));
    }

    @Test
    void aResultCarryingOneOrMoreExperiencePaysItsCount() {
        assertEquals(5, ArcaneFurnaceLogic.experience(5, 1.0F, RandomSource.create(1L)));
        assertEquals(3, ArcaneFurnaceLogic.experience(3, 2.5F, RandomSource.create(1L)),
                "ejectItem pays result.getCount() once xp >= 1");
    }

    @Test
    void fractionalExperienceRoundsUpByTheFraction() {
        int ones = 0;
        final RandomSource random = RandomSource.create(20261028L);
        for (int i = 0; i < 4000; i++) {
            final int paid = ArcaneFurnaceLogic.experience(1, 0.7F, random);
            assertTrue(paid == 0 || paid == 1, "1 * 0.7 rounds to floor(0.7)=0 or 1");
            ones += paid;
        }
        final double mean = ones / 4000.0;
        assertTrue(mean > 0.6 && mean < 0.8,
                "the original's fractional branch pays the fraction as a probability, got " + mean);
    }

    @Test
    void theExperienceRollIsSeedStable() {
        final int a = ArcaneFurnaceLogic.experience(7, 0.42F, RandomSource.create(1066L));
        final int b = ArcaneFurnaceLogic.experience(7, 0.42F, RandomSource.create(1066L));
        assertEquals(a, b, "the same seed must give the same payout, which is what the gametest asserts against");
        assertTrue(a >= 2 && a <= 4, "7 * 0.42 = 2.94 → 2, 3 (2.94 rounds up with 94% weight), got " + a);
    }

    // --- where the result is thrown to ------------------------------------

    @Test
    void theResultIsEjectedOutOfTheNozzleSide() {
        assertEquals(1.7, ArcaneFurnaceLogic.ejectOffsetAxis(1), 1.0E-9);
        assertEquals(-0.7, ArcaneFurnaceLogic.ejectOffsetAxis(-1), 1.0E-9);
        assertEquals(0.5, ArcaneFurnaceLogic.ejectOffsetAxis(0), 1.0E-9,
                "the eject point is 1.2 blocks along the facing axis, centred on the other");
    }

    @Test
    void theEjectVelocityIsALongTheFacingWithJitterAcrossIt() {
        final RandomSource random = RandomSource.create(77L);
        assertEquals(0.13, ArcaneFurnaceLogic.ejectVelocity(1, random), 1.0E-12);
        assertEquals(-0.13, ArcaneFurnaceLogic.ejectVelocity(-1, random), 1.0E-12);
        for (int i = 0; i < 500; i++) {
            final double v = ArcaneFurnaceLogic.ejectVelocity(0, random);
            assertTrue(v >= -0.03D && v <= 0.03D,
                    "the cross-axis term is (nextFloat() - nextFloat()) * 0.03, got " + v);
        }
    }

    // --- the inventory the rates run on -----------------------------------

    @Test
    void theFurnaceInventoryIsTheOriginalsThirtyTwoSlotsAtSixtyFourEach() {
        assertEquals(32, ArcaneFurnaceLogic.SLOTS, "TileArcaneFurnace IInventory: 32 slots");
        assertEquals(64, ArcaneFurnaceLogic.STACK_LIMIT, "getInventoryStackLimit: 64");
    }
}
