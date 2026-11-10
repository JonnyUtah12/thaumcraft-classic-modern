package com.jonnyutah12.thaumcraftclassic.entities.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-06-s-b: pins the taint creeper's AI numbers to the decompiled
 * sources: {@code EntityTaintCreeper.java} (line numbers below) and
 * {@code ai/combat/AICreeperSwell.java} (file line numbers).
 */
class TaintCreeperAiLogicTest {

    @Test
    void attributesComeFromTheDecompiledFields() {
        // field fuseTime line 36 (init 30); func_110147_ax lines 52-58
        assertEquals(30, TaintCreeperAiLogic.FUSE_TICKS);
        assertEquals(30.0D, TaintCreeperAiLogic.HEALTH);
        assertEquals(2.0D, TaintCreeperAiLogic.ATTACK);
        assertEquals(0.25D, TaintCreeperAiLogic.MOVEMENT_SPEED);
    }

    @Test
    void swellGoalEngageFollowsAiCreeperSwellCanUse() {
        // AICreeperSwell.java lines 14-18 (canUse): state > 0, or a target
        // strictly inside squared distance 9.0
        assertTrue(TaintCreeperAiLogic.swellEngage(0, true, 8.0D));
        assertFalse(TaintCreeperAiLogic.swellEngage(0, true, 9.0D));
        assertFalse(TaintCreeperAiLogic.swellEngage(0, false, 0.0D));
        assertTrue(TaintCreeperAiLogic.swellEngage(1, false, 9999.0D));
    }

    @Test
    void swellGoalUpdateFollowsAiCreeperSwellUpdate() {
        // AICreeperSwell.java lines 29-38 (update): -1 when no target, the
        // squared distance exceeds 49.0, or the target is not visible; 1
        // otherwise
        assertEquals(-1, TaintCreeperAiLogic.swellUpdateState(false, 0.0D, true));
        assertEquals(-1, TaintCreeperAiLogic.swellUpdateState(true, 50.0D, true));
        assertEquals(1, TaintCreeperAiLogic.swellUpdateState(true, 49.0D, true));
        assertEquals(-1, TaintCreeperAiLogic.swellUpdateState(true, 1.0D, false));
        assertEquals(1, TaintCreeperAiLogic.swellUpdateState(true, 1.0D, true));
    }

    @Test
    void fuseAccumulatesByStateAndFloorsAtZero() {
        // func_70071_h_ lines 128-131; the cap is the fuseTime field
        // (line 36), which NBT can change (lines 93-95)
        assertEquals(3, TaintCreeperAiLogic.nextFuse(2, 1, 30));
        assertEquals(0, TaintCreeperAiLogic.nextFuse(-5, 3, 30));
        assertEquals(0, TaintCreeperAiLogic.nextFuse(10, -15, 30));
        assertEquals(0, TaintCreeperAiLogic.nextFuse(1, -2, 30));
        assertEquals(0, TaintCreeperAiLogic.nextFuse(5, -5, 30));
    }

    @Test
    void fuseStopsAtCap() {
        // func_70071_h_ lines 133-134: capped at fuseTime (30 default),
        // explode at >= fuseTime
        assertEquals(30, TaintCreeperAiLogic.nextFuse(29, 5, 30));
        assertEquals(30, TaintCreeperAiLogic.nextFuse(30, 1, 30));
        assertTrue(TaintCreeperAiLogic.explodedThisTick(30, 30));
        assertFalse(TaintCreeperAiLogic.explodedThisTick(29, 30));
    }

    @Test
    void fuseCapIsLoadedFromTheFuseNbtAndDefaultsToThirty() {
        // "Fuse" is the CAP (write line 86, containsKey-guarded load lines
        // 93-95); the field default is 30 (line 36). The entity wires
        // this.fuseTime into nextFuse/explodedThisTick.
        assertEquals(30, TaintCreeperAiLogic.FUSE_TICKS);
        assertEquals("Fuse", TaintCreeperAiLogic.TAG_FUSE);
        assertEquals("ExplosionRadius", TaintCreeperAiLogic.TAG_EXPLOSION_RADIUS);
        assertEquals("powered", TaintCreeperAiLogic.TAG_POWERED);
        assertEquals(60, TaintCreeperAiLogic.nextFuse(55, 10, 60));
        assertTrue(TaintCreeperAiLogic.explodedThisTick(60, 60));
    }

    @Test
    void deathAnimationAcceleratesTheFuseWithHeadroom() {
        // func_70069_a lines 70-77: (int)(t + par1 * 1.5F), cap fuseTime - 5
        assertEquals(1, TaintCreeperAiLogic.deathFuseStep(0, 1.0F));
        assertEquals(2, TaintCreeperAiLogic.deathFuseStep(1, 1.0F));
        assertEquals(11, TaintCreeperAiLogic.deathFuseStep(10, 1.0F));
        assertEquals(5, TaintCreeperAiLogic.DEATH_FUSE_HEADROOM);
        assertEquals(25, TaintCreeperAiLogic.FUSE_TICKS - TaintCreeperAiLogic.DEATH_FUSE_HEADROOM);
        assertFalse(TaintCreeperAiLogic.explodedThisTick(25, 30));
    }

    @Test
    void experienceFormulaDependsOnTheTarget() {
        // func_82143_as lines 65-67: target == null ? 3 : 3 + (int)(maxHealth - 1)
        assertEquals(3, TaintCreeperAiLogic.experiencePoints(false, 30.0D));
        assertEquals(32, TaintCreeperAiLogic.experiencePoints(true, 30.0D));
    }

    @Test
    void explosionRadiusIsFixedAtOneAndAHalf() {
        // explode() line 136: the blast call passes 1.5F
        assertEquals(1.5F, TaintCreeperAiLogic.EXPLOSION_RADIUS);
        // field explosionRadius (line 37, init 3) round-trips through NBT and
        // is NEVER read by the blast
        assertEquals(3.0F, TaintCreeperAiLogic.LOADED_RADIUS_FIELD);
    }

    @Test
    void flashIntensityDivisorIsPinnedEvenThoughTheRendererIsDeferred() {
        // getCreeperFlashIntensity line 192: (progress + partialTick) / 28.0F
        assertEquals(28.0F, TaintCreeperAiLogic.FLASH_INTENSITY_DIVISOR);
    }

    @Test
    void fuseSoundPitchMatchesTheDecompiledCall() {
        // line 125: "random.fuse", 1.0F, 0.5F
        assertEquals(0.5F, TaintCreeperAiLogic.FUSE_SOUND_PITCH);
    }

    @Test
    void poisonSplashSkipsTaintCreaturesAndWetTargets() {
        // explode() lines 143-152: position AABB inflated 6.0 on all axes,
        // 100 ticks at amplifier 0, gated on !tainted && !wet
        assertEquals(6.0D, TaintCreeperAiLogic.POISON_BOX_INFLATE);
        assertEquals(100, TaintCreeperAiLogic.POISON_DURATION);
        assertEquals(0, TaintCreeperAiLogic.POISON_AMPLIFIER);
        assertTrue(TaintCreeperAiLogic.poisonsTarget(false, false));
        assertFalse(TaintCreeperAiLogic.poisonsTarget(true, false));
        assertFalse(TaintCreeperAiLogic.poisonsTarget(false, true));
        assertFalse(TaintCreeperAiLogic.poisonsTarget(true, true));
    }

    @Test
    void attackBiasChainFollowsTheDecompiledOrder() {
        // explode() lines 156-168: 10 tries, (a - b) * 5.0F with two
        // independent draws, nextBoolean gates each attempt, fibres meta 0
        assertEquals(10, TaintCreeperAiLogic.BIAS_ATTEMPTS);
        assertEquals(5.0D, TaintCreeperAiLogic.BIAS_OFFSET);
        assertEquals(0, TaintCreeperAiLogic.FIBRES_PLACED_META);
        assertEquals(5, TaintCreeperAiLogic.biasOffset(2.0F, 1.0F));
        assertEquals(-5, TaintCreeperAiLogic.biasOffset(1.0F, 2.0F));
        // (int) truncates toward zero: (1.4F - 1.6F) * 5.0F = -1.0000001 -> -1
        assertEquals(-1, TaintCreeperAiLogic.biasOffset(1.4F, 1.6F));
        assertEquals(0, TaintCreeperAiLogic.biasOffset(1.05F, 1.0F));
        assertTrue(TaintCreeperAiLogic.biasPlacementRoll(true));
        assertFalse(TaintCreeperAiLogic.biasPlacementRoll(false));
    }
}
