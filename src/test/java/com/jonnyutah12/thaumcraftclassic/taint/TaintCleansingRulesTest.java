package com.jonnyutah12.thaumcraftclassic.taint;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomPlant;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-04-s-b: unit coverage for the cleansing rules, the aura/vis
 * interaction and the flux → taint thresholds — every pure formula in
 * {@link TaintRules} that half 2 added, plus the plant-family numbers the
 * cleansing row owns.
 *
 * <p>Each test states the original expression it is pinning (file:line of the
 * decompiled 1.7.10 source), because the card's rule is "read the decompiled
 * original for the exact formulas" — a threshold that only exists as a number
 * in the port is not evidence. {@code TaintRulesParityTest} is the mechanical
 * half of that pair: it greps the same expressions out of
 * {@code reference/decompiled}; this class is the behavioural half, so a
 * transcription that changes the arithmetic fails here and a change to the
 * original text fails there.</p>
 *
 * <p>These are the rules the live paths call: {@code TileEtherealBloom.tick} →
 * {@code TaintManager.etherealBloomCleanse}, {@code NodeRuntime.handleTaintNode}
 * and {@code NodeGeneration.planNode} (the aura multiplier). A rule that
 * nothing calls is theatre, so the constants that gate the node paths are
 * asserted equal to the node engine's own periods in
 * {@link #nodePeriodsAreTheNodeEnginesOwnConstants()}.</p>
 */
class TaintCleansingRulesTest {

    // ----------------------------------------------------- the bloom's clock

    /**
     * {@code TileEtherealBloom.java:19-24}: the counter is seeded from
     * {@code world.rand.nextInt(100)} when it reads 0, incremented, and the
     * roll runs on {@code counter % 20 == 0}.
     */
    @Test
    public void bloomSeedsItsPhaseFromAHundredAndRollsEveryTwenty() {
        assertEquals(100, TaintRules.BLOOM_PHASE_MOD);
        assertEquals(20, TaintRules.BLOOM_PERIOD);
        assertTrue(TaintRules.bloomCleansesThisTick(20), "counter 20 rolls");
        assertTrue(TaintRules.bloomCleansesThisTick(40), "every 20th tick rolls");
        assertFalse(TaintRules.bloomCleansesThisTick(21), "the tick between rolls does nothing");
        assertFalse(TaintRules.bloomCleansesThisTick(39), "one short of a roll");
    }

    /** {@code TileEtherealBloom.java:25-26}: {@code nextInt(8) - nextInt(8)} per axis. */
    @Test
    public void bloomScanOffsetSpansMinusSevenToSeven() {
        assertEquals(8, TaintRules.BLOOM_SCAN_MOD);
        assertEquals(-7, TaintRules.bloomOffset(0, 7));
        assertEquals(7, TaintRules.bloomOffset(7, 0));
        assertEquals(0, TaintRules.bloomOffset(3, 3));
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int a = 0; a < TaintRules.BLOOM_SCAN_MOD; a++) {
            for (int b = 0; b < TaintRules.BLOOM_SCAN_MOD; b++) {
                min = Math.min(min, TaintRules.bloomOffset(a, b));
                max = Math.max(max, TaintRules.bloomOffset(a, b));
            }
        }
        assertEquals(-7, min, "the offset box is -7..7 on each axis (15x15)");
        assertEquals(7, max, "the offset box is -7..7 on each axis (15x15)");
    }

    /**
     * The distance term, verbatim from the original's call shape
     * ({@code getDistance(x + tx + 0.5, this.y, z + tz + 0.5)}): the
     * {@code double} overload measures from the tile's centre, so the deltas
     * are {@code (-tx, 0.5, -tz)} — a y term of exactly 0.25, and a
     * SQUARE, not a distance.
     */
    @Test
    public void bloomDistanceIsSquaredAndCarriesTheHalfBlockYTerm() {
        assertEquals(0.25D, TaintRules.bloomDistanceSquared(0, 0), 0.0D);
        assertEquals(49.25D, TaintRules.bloomDistanceSquared(7, 0), 0.0D);
        assertEquals(98.25D, TaintRules.bloomDistanceSquared(7, 7), 0.0D);
        assertEquals(81.0D, TaintRules.BLOOM_RADIUS_SQUARED, 0.0D);
    }

    /**
     * The gate is {@code <= 81.0} (inclusive) on the squared distance, and it
     * needs the biome half as well: a column outside the three thaumic biomes
     * is never rewritten, and a column inside them at 98.25 is never rewritten.
     */
    @Test
    public void bloomGateNeedsTheBiomeAndTheRadiusAndIsInclusive() {
        assertFalse(TaintRules.bloomCleanses(false, 0.0D), "a non-taint column at the centre stays");
        assertTrue(TaintRules.bloomCleanses(true, 0.25D), "the bloom's own column cleanses");
        assertTrue(TaintRules.bloomCleanses(true, 81.0D), "the gate is <=, so exactly 81 cleanses");
        assertFalse(TaintRules.bloomCleanses(true, 81.25D), "81.25 is outside");
        assertFalse(TaintRules.bloomCleanses(true, 98.25D), "the (7,7) corner is outside the disc");
    }

    /**
     * The cleansed area is a DISC of radius ~9 trimmed by the ±7 box, not a
     * 15×15 square and not a radius-81 field. Of the 225 offsets the scan can
     * draw, exactly 213 pass the radius gate: the 12 that fail are the four
     * (±7,±7) corners (98.25) and the eight (±7,±6)/(±6,±7) corners (85.25).
     * That number is the geometry, so it pins 81.0, the 0.25 term and the box
     * width at once.
     */
    @Test
    public void bloomCleansesADiscOfTwoHundredThirteenColumnsNotASquare() {
        int inside = 0;
        for (int tx = -7; tx <= 7; tx++) {
            for (int tz = -7; tz <= 7; tz++) {
                if (TaintRules.bloomCleanses(true, TaintRules.bloomDistanceSquared(tx, tz))) {
                    inside++;
                }
            }
        }
        assertEquals(213, inside,
                "the ±7 scan box has 225 columns; the squared-radius gate keeps 213 (a disc)");
    }

    // ------------------------------------------------------- biome restore

    /**
     * {@code TileEtherealBloom.java:37-38}: the generated biome is written as
     * itself, and the plains substitution happens ONLY when the generated
     * answer is the taint biome — not for an Eerie / Magical Forest answer.
     */
    @Test
    public void restoreFallsBackToPlainsOnlyWhenTheGeneratedBiomeIsTaint() {
        assertTrue(TaintRules.cleansingFallsBackToPlains(true), "generated taint → plains");
        assertFalse(TaintRules.cleansingFallsBackToPlains(false), "any other generated biome is written");
    }

    /** {@code TileEtherealBloom.java:27-31}: a column outside the three biomes is skipped. */
    @Test
    public void restoreSkipsEveryColumnOutsideTheThaumicBiomes() {
        assertFalse(TaintRules.cleansingSkipsColumn(true), "a thaumic column is not skipped");
        assertTrue(TaintRules.cleansingSkipsColumn(false), "a plains column is left exactly as it is");
    }

    // ------------------------------------------------------------ aura / vis

    /**
     * {@code ThaumcraftWorldGenerator.java:272-279}: {@code baura = (int)(baura
     * * 1.5F)} for a non-PURE node in a taint biome, and the same cast again
     * when the coin flip makes it TAINTED. The cast TRUNCATES (150 → 225, not
     * 226; 99 → 148, not 149), which is what the vis budget downstream
     * ({@code nextInt(baura / 2) + baura / 2}) divides.
     */
    @Test
    public void taintBiomeAuraIsAHalfAgainAndTruncatesLikeTheOriginalCast() {
        assertEquals(1.5F, TaintRules.TAINT_AURA_MULTIPLIER, 0.0F);
        assertEquals(150, TaintRules.taintBiomeAura(100));
        assertEquals(225, TaintRules.taintBiomeAura(150), "the doubled case of a MAGICAL biome");
        assertEquals(120, TaintRules.taintBiomeAura(80), "the WASTELAND aura of the taint biome");
        assertEquals(148, TaintRules.taintBiomeAura(99), "(int) truncates: 148.5 → 148");
        assertEquals(1, TaintRules.taintBiomeAura(1), "(int) truncates: 1.5 → 1");
        assertEquals(0, TaintRules.taintBiomeAura(0), "an aura of nothing stays nothing");
    }

    /** {@code TileNode.java:774-783}: TAINTED node, {@code count % 50 == 0}, taint biome known. */
    @Test
    public void taintNodeRepaintsTheBiomeEveryFiftyTicksOnlyWithTheBiomeRegistered() {
        assertEquals(50, TaintRules.NODE_TAINT_PAINT_PERIOD);
        assertTrue(TaintRules.nodePaintsTaintBiome(50, true));
        assertTrue(TaintRules.nodePaintsTaintBiome(100, true));
        assertFalse(TaintRules.nodePaintsTaintBiome(51, true), "the period is 50, not 51");
        assertFalse(TaintRules.nodePaintsTaintBiome(50, false),
                "the original compares against biomeTaint by identity — with no taint biome registered "
                        + "the paint is inert");
    }

    /**
     * {@code TileNode.java:792-798}: the repaint branch is taken only for a node
     * that is neither PURE nor TAINTED, on {@code count % 100 == 0}, standing in
     * a taint biome, on {@code nextInt(500) == 0}.
     */
    @Test
    public void nonPureNodeFlipsToTaintedAtOneInFiveHundredEveryHundredTicks() {
        assertEquals(100, TaintRules.NODE_STABILITY_PERIOD);
        assertEquals(500, TaintRules.NODE_TAINT_FLIP_MOD);
        assertTrue(TaintRules.nodeBecomesTainted(100, false, true, true, 0));
        assertFalse(TaintRules.nodeBecomesTainted(100, false, true, true, 1), "one in five hundred");
        assertFalse(TaintRules.nodeBecomesTainted(99, false, true, true, 0), "the period is 100");
        assertFalse(TaintRules.nodeBecomesTainted(100, true, true, true, 0), "a PURE node never taints");
        assertFalse(TaintRules.nodeBecomesTainted(100, false, true, false, 0), "not in a taint biome");
        assertFalse(TaintRules.nodeBecomesTainted(100, false, false, true, 0), "no taint biome registered");
    }

    /**
     * The node engine's own periods are the same numbers, because
     * {@code NodeRuntime.handleTaintNode} calls these rules — this is the seam
     * that keeps the port from carrying two copies of 50 and 100.
     */
    @Test
    public void nodePeriodsAreTheNodeEnginesOwnConstants() {
        assertEquals(TaintRules.NODE_TAINT_PAINT_PERIOD, NodeRuntime.BIOME_EFFECT_TICKS);
        assertEquals(TaintRules.NODE_STABILITY_PERIOD, NodeRuntime.STABILITY_TICKS);
        assertEquals(8, TaintRules.NODE_TAINT_SCAN_MOD);
        assertEquals(-7, TaintRules.nodeTaintScanOffset(0, 7));
        assertEquals(7, TaintRules.nodeTaintScanOffset(7, 0));
    }

    // ------------------------------------------------- flux → taint thresholds

    /**
     * {@code BlockFluxGoo.java:88-96 and 110-118}: {@code meta >= 2 && meta < 6}
     * with air above on {@code nextInt(25) == 0} spawns a size-1 slime, {@code
     * meta >= 6} spawns the size-2 slime, and the goo decays one quanta on
     * {@code nextInt(30) == 0}, venting gas above on a coin flip.
     */
    @Test
    public void fluxGooQuantaBandsAndDecayAreTheOriginals() {
        assertEquals(2, TaintRules.FLUX_SLIME_SMALL_MIN_QUANTA);
        assertEquals(5, TaintRules.FLUX_SLIME_SMALL_MAX_QUANTA);
        assertEquals(25, TaintRules.FLUX_SLIME_SMALL_MOD);
        assertEquals(30, TaintRules.FLUX_DECAY_MOD);
        assertEquals(2, TaintRules.FLUX_GAS_ABOVE_CHANCES, "nextBoolean is one draw in two");

        assertTrue(TaintRules.fluxGooSpawnsSmallSlime(2, true, 0));
        assertTrue(TaintRules.fluxGooSpawnsSmallSlime(5, true, 0));
        assertFalse(TaintRules.fluxGooSpawnsSmallSlime(1, true, 0), "meta 1 is below the band");
        assertFalse(TaintRules.fluxGooSpawnsSmallSlime(6, true, 0), "meta 6 is the big slime");
        assertFalse(TaintRules.fluxGooSpawnsSmallSlime(3, false, 0), "needs air above");
        assertFalse(TaintRules.fluxGooSpawnsSmallSlime(3, true, 1), "one in twenty-five");

        assertTrue(TaintRules.fluxGooSpawnsLargeSlime(6, true, 0));
        assertFalse(TaintRules.fluxGooSpawnsLargeSlime(5, true, 0), "the size-2 band starts at 6");

        assertTrue(TaintRules.fluxGooDecays(0), "nextInt(30) == 0");
        assertFalse(TaintRules.fluxGooDecays(1));
    }

    // -------------------------------------------------------- plant family

    /**
     * {@code BlockCustomPlant.java:137-145}: {@code getLightValue} is
     * per-metadata — 1, 2, 3 and 5 glow 8, the bloom (4) glows 15, the
     * greatwood sapling (0) uses the block default (0).
     */
    @Test
    public void plantLightLevelsArePerMetaVerbatim() {
        assertEquals(0, BlockCustomPlant.lightFor(0));
        assertEquals(8, BlockCustomPlant.lightFor(1));
        assertEquals(8, BlockCustomPlant.lightFor(2));
        assertEquals(8, BlockCustomPlant.lightFor(3));
        assertEquals(15, BlockCustomPlant.lightFor(4), "the Ethereal Bloom glows 15");
        assertEquals(8, BlockCustomPlant.lightFor(5));
    }

    /**
     * The family's own numbers: six metas ({@code getSubItems} 0..5), meta 4 is
     * the tile-carrying bloom, and the fire pair is {@code getFlammability 100}
     * / {@code getFireSpreadSpeed 60} ({@code BlockCustomPlant.java:186-193}).
     */
    @Test
    public void plantFamilyCarriesSixMetasTheBloomMetaAndTheOriginalFirePair() {
        assertEquals(0, BlockCustomPlant.VARIANT.getMin());
        assertEquals(5, BlockCustomPlant.VARIANT.getMax());
        assertEquals(6, BlockCustomPlant.VARIANT.getPossibleValues().size());
        assertEquals(4, BlockCustomPlant.META_BLOOM, "meta 4 is the Ethereal Bloom (purifier seed)");
        assertEquals(100, BlockCustomPlant.FLAMMABILITY);
        assertEquals(60, BlockCustomPlant.FIRE_SPREAD_SPEED);
        assertEquals(1.0F, TaintRules.BLOOM_ROOTS_VOLUME, 0.0F);
        assertEquals(0.6F, TaintRules.BLOOM_ROOTS_PITCH, 0.0F);
    }
}
