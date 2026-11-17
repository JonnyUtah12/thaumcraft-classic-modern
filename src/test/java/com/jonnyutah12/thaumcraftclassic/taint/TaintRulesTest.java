package com.jonnyutah12.thaumcraftclassic.taint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-04-s: the spread / conversion / cleansing rule matrix.
 *
 * <p>{@link TaintRules} is deliberately pure arithmetic — every threshold is a
 * named constant and every rule takes the world facts it needs as plain
 * arguments, so the whole spread/conversion/cleansing contract is decidable
 * here, with no world, no tags and no game-test server. The companion
 * {@link TaintRulesParityTest} pins those constants against the decompiled
 * 1.7.10 sources; {@code TaintGameTest} proves the same rules drive the real
 * blocks in a real world.</p>
 */
class TaintRulesTest {

    @Test
    void fibreSeedGateMatchesOriginal() {
        // original lines 191-195: solid neighbour, not taint-only, not liquid,
        // and air / replaceable / flower / leaves
        assertTrue(TaintRules.canSeedFibres(true, false, false, true, false, false, false));
        assertTrue(TaintRules.canSeedFibres(true, false, false, false, true, false, false));
        assertTrue(TaintRules.canSeedFibres(true, false, false, false, false, true, false));
        assertTrue(TaintRules.canSeedFibres(true, false, false, false, false, false, true));
        assertFalse(TaintRules.canSeedFibres(false, false, false, true, false, false, false));
        assertFalse(TaintRules.canSeedFibres(true, true, false, true, false, false, false));
        assertFalse(TaintRules.canSeedFibres(true, false, true, true, false, false, false));
        assertFalse(TaintRules.canSeedFibres(true, false, false, false, false, false, false));
    }

    @Test
    void fibreMetaLadderIsTheOriginalRungs() {
        // 90% plain (nextInt(10) != 0), then the 1-in-10 branch needs air above
        // and a solid block below before the grass rungs are rolled at all
        assertEquals(0, TaintRules.fibresMeta(1, true, true, 0, 0));
        assertEquals(0, TaintRules.fibresMeta(0, false, true, 0, 0));
        assertEquals(0, TaintRules.fibresMeta(0, true, false, 0, 0));
        assertEquals(1, TaintRules.fibresMeta(0, true, true, 8, 0));
        assertEquals(2, TaintRules.fibresMeta(0, true, true, 9, 9));
        assertEquals(TaintRules.FIBRES_SPORE_STALK_META, TaintRules.fibresMeta(0, true, true, 9, 10));
        assertEquals(9, TaintRules.FIBRES_GRASS1_LIMIT);
        assertEquals(10, TaintRules.FIBRES_GRASS2_LIMIT);
    }

    @Test
    void crustNeedsTwoAdjacentTaintAndACrustTarget() {
        assertFalse(TaintRules.convertsToCrust(1, true));
        assertTrue(TaintRules.convertsToCrust(2, true));
        assertFalse(TaintRules.convertsToCrust(2, false));
        assertFalse(TaintRules.convertsToCrust(5, false));
        assertEquals(2, TaintRules.CRUST_ADJACENT_MIN);
    }

    @Test
    void soilNeedsThreeAdjacentTaintAndIsNeverAir() {
        assertFalse(TaintRules.convertsToSoil(2, false, true));
        assertFalse(TaintRules.convertsToSoil(3, true, true)); // the air guard
        assertTrue(TaintRules.convertsToSoil(3, false, true));
        assertFalse(TaintRules.convertsToSoil(3, false, false));
        assertEquals(3, TaintRules.SOIL_ADJACENT_MIN);
    }

    @Test
    void biomeSpreadUsesRateTimesFiveAndTwoAdjacentTaint() {
        assertEquals(1000, TaintRules.biomeSpreadModulus(200));
        assertEquals(0, TaintRules.biomeSpreadModulus(0));
        // a rate of 0 disables the rule (the original's config guard)
        assertFalse(TaintRules.spreadsBiome(0, false, 0, 5));
        assertFalse(TaintRules.spreadsBiome(200, true, 0, 5));  // already taint
        assertFalse(TaintRules.spreadsBiome(200, false, 1, 5)); // roll missed
        assertFalse(TaintRules.spreadsBiome(200, false, 0, 1)); // not enough taint
        assertTrue(TaintRules.spreadsBiome(200, false, 0, 2));
        assertTrue(TaintRules.spreadsBiome(200, false, 0, 9));
    }

    @Test
    void decayOutsideBiomeIsCrustOverTwentySoilOverTen() {
        assertTrue(TaintRules.crustDecaysOutsideBiome(0));
        assertFalse(TaintRules.crustDecaysOutsideBiome(1));
        assertFalse(TaintRules.crustDecaysOutsideBiome(19));
        assertTrue(TaintRules.soilDecaysOutsideBiome(0));
        assertFalse(TaintRules.soilDecaysOutsideBiome(9));
        assertEquals(20, TaintRules.CRUST_DECAY_MOD);
        assertEquals(10, TaintRules.SOIL_DECAY_MOD);
    }

    @Test
    void stepPoisonOddsAndDurationPerSurfaceAndActor() {
        assertEquals(100, TaintRules.stepOdds(true, false));
        assertEquals(20, TaintRules.stepOdds(false, false));
        assertEquals(1000, TaintRules.stepOdds(true, true));
        assertEquals(500, TaintRules.stepOdds(false, true));
        assertEquals(80, TaintRules.stepDuration(true));
        assertEquals(160, TaintRules.stepDuration(false));
    }

    @Test
    void scanBoxIsTheOriginalOffsets() {
        assertEquals(-1, TaintRules.scanDelta(0, 3, 1));
        assertEquals(0, TaintRules.scanDelta(1, 3, 1));
        assertEquals(1, TaintRules.scanDelta(2, 3, 1));
        assertEquals(-3, TaintRules.scanDelta(0, 5, 3));
        assertEquals(1, TaintRules.scanDelta(4, 5, 3));
    }

    @Test
    void sporeSwarmerAndStalkRules() {
        assertTrue(TaintRules.spawnsSporeSwarmer(0, true, true));
        assertFalse(TaintRules.spawnsSporeSwarmer(1, true, true));
        assertFalse(TaintRules.spawnsSporeSwarmer(0, false, true));
        assertFalse(TaintRules.spawnsSporeSwarmer(0, true, false));
        assertEquals(200, TaintRules.CRUST_SWARMER_MOD);

        assertTrue(TaintRules.maturesSporeStalk(3, 0, true, true));
        assertFalse(TaintRules.maturesSporeStalk(2, 0, true, true));
        assertFalse(TaintRules.maturesSporeStalk(3, 1, true, true));
        assertFalse(TaintRules.maturesSporeStalk(3, 0, false, true));
        assertEquals(10, TaintRules.SPORE_STALK_MOD);

        assertTrue(TaintRules.revertsMatureStalk(4, 0));
        assertFalse(TaintRules.revertsMatureStalk(4, 1));
        assertFalse(TaintRules.revertsMatureStalk(3, 0));
    }

    @Test
    void fluxGooBranchesPinQuantaAndOdds() {
        // meta >= 6 with air above: 1/25 thaumic slime (TC-M6-06), else
        // 1/50 biome-to-taint plus fibres
        assertTrue(TaintRules.fluxGooConvertsToTaint(6, true, 0, true));
        assertFalse(TaintRules.fluxGooConvertsToTaint(5, true, 0, true));
        assertFalse(TaintRules.fluxGooConvertsToTaint(6, false, 0, true));
        assertFalse(TaintRules.fluxGooConvertsToTaint(6, true, 1, true));
        assertFalse(TaintRules.fluxGooConvertsToTaint(6, true, 0, false));
        assertEquals(50, TaintRules.FLUX_TAINT_MOD);
        assertEquals(25, TaintRules.FLUX_SLIME_LARGE_MOD);

        assertFalse(TaintRules.gooBlocksFall(3));
        assertTrue(TaintRules.gooBlocksFall(4));
    }

    @Test
    void fallHeightGuardCarriesTheOriginalsWorldFloorMeaning() {
        // The original writes `y2 >= 0`, and in 1.7.10 y=0 IS the world's
        // bottom layer, so the guard means "the target cell stays in the world".
        assertEquals(0, TaintRules.FALL_MIN_Y);
        // 1.21.1 overworld (min build height -64): a crust at y=-52 falls —
        // the original's literal 0 would have frozen the whole bottom 64 layers.
        assertTrue(TaintRules.canFallAtHeight(-52, -64));
        assertTrue(TaintRules.canFallAtHeight(-63, -64));
        // ...and the crust may not fall out of the world. In 1.7.10 the block
        // below y=0 is bedrock (not air), so the same refusal was implicit;
        // in 1.21.1 out-of-world reads as void air and needs the explicit check.
        assertFalse(TaintRules.canFallAtHeight(-64, -64));
        assertFalse(TaintRules.canFallAtHeight(-100, -64));
        // A 1.7.10-shaped floor (0) keeps the original's numbers, one cell shy
        // of bedrock (the documented deviation in BD-14).
        assertTrue(TaintRules.canFallAtHeight(1, 0));
        assertFalse(TaintRules.canFallAtHeight(0, 0));
        assertFalse(TaintRules.canFallAtHeight(-1, 0));
    }

    @Test
    void cleansingAndBottleConstants() {
        assertEquals(10, TaintRules.BOTTLE_SPILL_ATTEMPTS);
        assertEquals(5.0F, TaintRules.BOTTLE_SPILL_SPREAD);
        assertEquals(100, TaintRules.BOTTLE_POISON_DURATION_TICKS);
        assertEquals(0.1F, TaintRules.ROOTS_VOLUME);
        assertEquals(0.9F, TaintRules.ROOTS_PITCH_BASE);
        assertEquals(0.2F, TaintRules.ROOTS_PITCH_SPREAD);
    }
}
