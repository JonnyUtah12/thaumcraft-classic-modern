package com.jonnyutah12.thaumcraftclassic.entities.golems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TC-M6-08-s-b: pins every pure golem formula to the decompiled source, value
 * for value. Each test names the original method and quotes the expression it
 * mirrors, so a future edit that "tidies" a balance figure fails here instead of
 * shipping silently.
 *
 * <p>These are the formulas the card calls load-bearing — in particular
 * {@code getCarryLimit()}, which derives from {@code getUpgradeAmount(1)} and
 * drives the carrier task's capacity.</p>
 */
class GolemRuntimeFormulaTest {

    private static final float EPS = 1.0E-5F;

    // ------------------------------------------------------------- carryLimit ---

    /**
     * Original {@code getCarryLimit()} ({@code EntityGolemBase.java:302-310}):
     * {@code base + Math.min(16, Math.max(4, base)) * getUpgradeAmount(1)}.
     * The per-upgrade term is the <b>clamped</b> base, so IRON (carry 32) gains
     * 16 per Earth upgrade, not 32.
     */
    @Test
    void carryLimitPerMaterialFollowsTheClampedBaseFormula() {
        // The eight EnumGolemType rows, with the Earth-upgrade term the formula derives.
        // STRAW carry 1: clamp(1) = max(4, 1) = 4, so +4 per Earth upgrade.
        assertEquals(1, EntityGolemBase.carryLimit(1, 0), "STRAW with no Earth upgrade carries its base 1");
        assertEquals(5, EntityGolemBase.carryLimit(1, 1), "STRAW + 1 Earth = 1 + 4 = 5");
        // WOOD carry 4, 1 slot: clamp(4) = 4.
        assertEquals(4, EntityGolemBase.carryLimit(4, 0), "WOOD base carry 4");
        assertEquals(8, EntityGolemBase.carryLimit(4, 1), "WOOD + 1 Earth = 4 + 4 = 8");
        // TALLOW carry 8, 2 slots: clamp(8) = 8.
        assertEquals(8, EntityGolemBase.carryLimit(8, 0), "TALLOW base carry 8");
        assertEquals(24, EntityGolemBase.carryLimit(8, 2), "TALLOW + 2 Earth = 8 + 8*2 = 24");
        // STONE carry 16, 1 slot: clamp(16) = 16.
        assertEquals(32, EntityGolemBase.carryLimit(16, 1), "STONE + 1 Earth = 16 + 16 = 32");
        // IRON carry 32, 1 slot: min(16, max(4, 32)) = 16, NOT 32.
        assertEquals(32, EntityGolemBase.carryLimit(32, 0), "IRON base carry 32");
        assertEquals(48, EntityGolemBase.carryLimit(32, 1),
                "IRON + 1 Earth = 32 + min(16,32)=16, the clamped base — not 32 + 32");
        // THAUMIUM carry 32, 2 slots.
        assertEquals(64, EntityGolemBase.carryLimit(32, 2), "THAUMIUM + 2 Earth = 32 + 16*2 = 64");
    }

    /**
     * The two boundaries of the clamp, {@code max(4, base)} and {@code min(16, base)}.
     * Neither base below is a material carry figure — {@code EnumGolemType.carry} only
     * carries 1/4/8/16/32 — so these pin the clamp itself rather than a shipped row.
     */
    @Test
    void carryLimitClampsThePerUpgradeTermAtBothEnds() {
        assertEquals(3 + 4, EntityGolemBase.carryLimit(3, 1),
                "a base below 4 is raised to 4 for the upgrade term (max(4, 3) = 4)");
        assertEquals(17 + 16, EntityGolemBase.carryLimit(17, 1),
                "a base above 16 is cut to 16 for the upgrade term (min(16, 17) = 16)");
        assertEquals(0, EntityGolemBase.carryLimit(0, 0), "no upgrades means the raw base is returned");
    }

    /**
     * Original {@code getFluidCarryLimit()} ({@code EntityGolemBase.java:311-313}):
     * {@code MathHelper.floor(Math.sqrt(getCarryLimit())) * 1000} — whole buckets.
     */
    @Test
    void fluidCarryLimitIsWholeBucketsOfTheSquareRoot() {
        assertEquals(1000, EntityGolemBase.fluidCarryLimit(1), "floor(sqrt(1)) = 1 bucket");
        assertEquals(2000, EntityGolemBase.fluidCarryLimit(8), "floor(sqrt(8)=2.828) = 2 buckets");
        assertEquals(4000, EntityGolemBase.fluidCarryLimit(16), "floor(sqrt(16)) = 4 buckets");
        assertEquals(4000, EntityGolemBase.fluidCarryLimit(20), "floor(sqrt(20)=4.47) = 4 buckets, rounded down");
        assertEquals(4000, EntityGolemBase.fluidCarryLimit(24), "floor(sqrt(24)=4.89) = 4 buckets, rounded down");
        assertEquals(5000, EntityGolemBase.fluidCarryLimit(32), "floor(sqrt(32)=5.657) = 5 buckets");
        assertEquals(8000, EntityGolemBase.fluidCarryLimit(64), "floor(sqrt(64)) = 8 buckets");
    }

    // ----------------------------------------------------------------- range ---

    /**
     * Original {@code getRange()} ({@code EntityGolemBase.java:430-441}):
     * {@code 16 + getUpgradeAmount(3) * 4} (Water), then
     * {@code += max(dmod * 0.1F, 1.0F)} for the "G" spectacles and
     * {@code += max(dmod * 0.2F, 2.0F)} for an advanced golem — in that order,
     * compounding.
     */
    @Test
    void rangeStartsAtSixteenAndAddsFourPerWaterUpgrade() {
        assertEquals(16.0F, EntityGolemBase.range(0, false, false), EPS, "no Water upgrade is the base 16");
        assertEquals(20.0F, EntityGolemBase.range(1, false, false), EPS, "16 + 1*4");
        assertEquals(24.0F, EntityGolemBase.range(2, false, false), EPS, "16 + 2*4");
        assertEquals(32.0F, EntityGolemBase.range(4, false, false), EPS, "16 + 4*4");
    }

    @Test
    void rangeBonusesApplyInOrderAndCompound() {
        assertEquals(17.6F, EntityGolemBase.range(0, true, false), EPS,
                "spectacles add max(16*0.1, 1) = 1.6");
        assertEquals(19.2F, EntityGolemBase.range(0, false, true), EPS,
                "advanced adds max(16*0.2, 2) = 3.2");
        assertEquals(21.12F, EntityGolemBase.range(0, true, true), EPS,
                "advanced compounds on the spectacle-adjusted 17.6: 17.6 + max(3.52, 2) = 21.12");
        assertEquals(36.96F, EntityGolemBase.range(3, true, true), EPS,
                "16+12 = 28, +2.8 = 30.8, +6.16 = 36.96");
    }

    /**
     * The {@code max(..., floor)} guard is unreachable for a legal range: the
     * smallest legal {@code dmod} is 16, so the percentage is 1.6 / 3.2 and
     * always beats the 1.0 / 2.0 floors. Pinned as a fact so nobody "optimises"
     * the floors away and then meets them through a future negative path.
     */
    @Test
    void theRangeBonusFloorsAreUnreachableForALegalRange() {
        final float smallestLegal = EntityGolemBase.range(0, false, false);
        assertEquals(16.0F, smallestLegal, EPS, "the smallest legal range is 16");
        assertEquals(true, smallestLegal * 0.1F > 1.0F,
                "16 * 0.1 = 1.6 already beats the spectacle floor of 1.0");
        assertEquals(true, smallestLegal * 0.2F > 2.0F,
                "16 * 0.2 = 3.2 already beats the advanced floor of 2.0");
        // The floors do apply when a caller drives dmod below them, which is what
        // the source's Math.max is for: 10 + 0 = 10, 10*0.1 = 1.0 == floor, and
        // 4 + 0 = 4 gives 0.4 < 1.0, so the flat 1.0 wins.
        assertEquals(4.0F + 1.0F, EntityGolemBase.range(-3, true, false), EPS,
                "with dmod 4 the 0.4 percentage loses to the flat 1.0 floor");
    }

    // ------------------------------------------------------------ carrySpace ---

    /**
     * Original {@code getCarrySpace()} ({@code EntityGolemBase.java:812-816}):
     * {@code min(getCarryLimit() - carriedCount, carriedMaxStack - carriedCount)}.
     */
    @Test
    void carrySpaceIsTheSmallerOfTheLimitAndTheStackRoom() {
        assertEquals(3, EntityGolemBase.carrySpace(4, 1, 64),
                "WOOD's limit 4 with 1 carried and a 64 stack: min(3, 63) = 3");
        assertEquals(63, EntityGolemBase.carrySpace(64, 1, 64),
                "a 64 limit with 1 carried and a 64 stack: min(63, 63) = 63");
        assertEquals(0, EntityGolemBase.carrySpace(4, 4, 64),
                "a full carry limit leaves no space: min(0, 60) = 0");
        assertEquals(-1, EntityGolemBase.carrySpace(4, 5, 64),
                "the original does not guard over-carry, so the term goes negative");
    }

    // -------------------------------------------------------- healthPercent ---

    /** Original {@code getHealthPercentage()} ({@code EntityGolemBase.java:791-793}). */
    @Test
    void healthPercentageIsTheSyncedHealthOverMaxHealth() {
        assertEquals(1.0F, EntityGolemBase.healthPercentage(20, 20.0), EPS, "full health is 1.0");
        assertEquals(0.5F, EntityGolemBase.healthPercentage(10, 20.0), EPS, "half health is 0.5");
        assertEquals(0.0F, EntityGolemBase.healthPercentage(0, 20.0), EPS, "dead is 0.0");
        assertEquals(1.5F, EntityGolemBase.healthPercentage(30, 20.0), EPS,
                "the original divides without clamping, so an over-heal reads above 1.0");
        assertEquals(0.0F, EntityGolemBase.healthPercentage(20, 0.0), EPS,
                "a zero max health must not divide by zero (the port guards it)");
    }

    // --------------------------------------------------------- movementSpeed ---

    /**
     * Original {@code func_70689_ay()} ({@code EntityGolemBase.java:315-336}):
     * bowtie "B" ×1.1, plating "P" ×0.88, Air upgrade ×(1 + 0.15n), advanced
     * ×1.1, and ×2.0 in water for the water-affine materials.
     */
    @Test
    void movementSpeedAppliesEachModifierInTheSourceOrder() {
        assertEquals(0.35F, EntityGolemBase.movementSpeed(0.35F, false, false, 0, false, false, false), EPS,
                "WOOD's bare speed");
        assertEquals(0.35F * 1.1F, EntityGolemBase.movementSpeed(0.35F, true, false, 0, false, false, false), EPS,
                "the 'B' bowtie multiplies by 1.1");
        assertEquals(0.35F * 0.88F, EntityGolemBase.movementSpeed(0.35F, false, true, 0, false, false, false), EPS,
                "the 'P' plating multiplies by 0.88");
        assertEquals(0.35F * 1.3F, EntityGolemBase.movementSpeed(0.35F, false, false, 2, false, false, false), EPS,
                "2 Air upgrades multiply by 1 + 0.15*2 = 1.3");
        assertEquals(0.35F * 1.1F, EntityGolemBase.movementSpeed(0.35F, false, false, 0, true, false, false), EPS,
                "an advanced golem multiplies by 1.1");
        assertEquals(0.35F * 2.0F, EntityGolemBase.movementSpeed(0.35F, false, false, 0, false, true, true), EPS,
                "a water-affine golem in water doubles");
        assertEquals(0.35F, EntityGolemBase.movementSpeed(0.35F, false, false, 0, false, true, false), EPS,
                "a non-water-affine golem in water is unchanged");
        // Compounding order: bowtie, then plating, then Air, then advanced.
        assertEquals(0.35F * 1.1F * 0.88F * 1.3F * 1.1F,
                EntityGolemBase.movementSpeed(0.35F, true, true, 2, true, false, false), EPS,
                "all four multipliers compound in the source's order");
    }

    // ----------------------------------------------------------- actionTimer ---

    /** Original {@code getActionTimer()} ({@code EntityGolemBase.java:1047-1049}): a triangle wave. */
    @Test
    void actionTimerIsATriangleWaveOverSixTicks() {
        assertEquals(0, EntityGolemBase.actionTimer(0), "3 - |0-3| = 0");
        assertEquals(1, EntityGolemBase.actionTimer(1), "3 - |1-3| = 1");
        assertEquals(2, EntityGolemBase.actionTimer(2), "3 - |2-3| = 2");
        assertEquals(3, EntityGolemBase.actionTimer(3), "3 - |3-3| = 3, the peak");
        assertEquals(2, EntityGolemBase.actionTimer(4), "3 - |4-3| = 2");
        assertEquals(1, EntityGolemBase.actionTimer(5), "3 - |5-3| = 1");
        assertEquals(0, EntityGolemBase.actionTimer(6), "3 - |6-3| = 0, back to the start");
    }

    /** The action counter runs 6 ticks; the two arm counters 5 each ({@code EntityGolemBase.java:1051-1070}). */
    @Test
    void armAndActionCountersArePinned() {
        assertEquals(6, EntityGolemBase.ACTION_TICKS, "startActionTimer sets action = 6");
        assertEquals(5, EntityGolemBase.ARM_TICKS, "startLeftArmTimer / startRightArmTimer set 5");
    }
}
