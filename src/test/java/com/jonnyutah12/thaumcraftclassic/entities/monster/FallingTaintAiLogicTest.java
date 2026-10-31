package com.jonnyutah12.thaumcraftclassic.entities.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the falling-taint physics (TC-M6-06-s-b) without a world. Every
 * expectation quotes {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * entities/EntityFallingTaint.java} with its line number.
 */
class FallingTaintAiLogicTest {

    @Test
    void gravityAndDragQuoteTheTickBody() {
        // gravity line 81 (field_70181_x -= 0.04F), drag lines 83-85: all
        // three axes *= 0.98F
        assertEquals(-0.04D, FallingTaintAiLogic.GRAVITY);
        assertEquals(0.98D, FallingTaintAiLogic.DRAG);
    }

    @Test
    void landingMotionFactorsQuoteTheLandingBranch() {
        // lines 106-108
        assertEquals(0.7D, FallingTaintAiLogic.LAND_XZ);
        assertEquals(-0.5D, FallingTaintAiLogic.LAND_Y);
    }

    @Test
    void landingSoundIsHalfVolumeWithTheRandomizedPitch() {
        // lines 113-114: volume 0.5F, pitch ((rand-rand) * 0.2F + 1.0F) * 0.8F
        assertEquals(0.5F, FallingTaintAiLogic.LAND_SOUND_VOLUME);
        assertEquals(0.8F, FallingTaintAiLogic.landSoundPitch(0.0F, 0.0F), 1.0E-6);
        assertEquals(0.96F, FallingTaintAiLogic.landSoundPitch(1.0F, 0.0F), 1.0E-6);
        assertEquals(0.64F, FallingTaintAiLogic.landSoundPitch(0.0F, 1.0F), 1.0E-6);
    }

    @Test
    void discardRulesUseTheOriginalsHardcodedYWindow() {
        // lines 102-104: fallTime > 100 && (j < 1 || j > 256) || fallTime > 600
        assertEquals(100, FallingTaintAiLogic.MAX_OUT_OF_WORLD_TICKS);
        assertEquals(600, FallingTaintAiLogic.MAX_FALL_TICKS);
        assertEquals(256, FallingTaintAiLogic.MAX_FALL_Y);

        assertFalse(FallingTaintAiLogic.discardByHeight(100, 0), "tick 100 is not yet out of the world");
        assertTrue(FallingTaintAiLogic.discardByHeight(101, 0), "below y=1 after 100 ticks discards");
        assertTrue(FallingTaintAiLogic.discardByHeight(101, 257), "above the hardcoded 256 discards");
        assertFalse(FallingTaintAiLogic.discardByHeight(101, 256), "y=256 is still inside the window");
        assertFalse(FallingTaintAiLogic.discardByHeight(101, 1), "y=1 is still inside the window");
    }

    @Test
    void theFirstTickIsTickOneAndTheSourceCellIsClearedThen() {
        // lines 91-97: if (fallTime == 1) { ... func_147468_f(oldX, oldY, oldZ) }
        assertEquals(1, FallingTaintAiLogic.FIRST_TICK);
    }

    @Test
    void fluxGooSupportNeedsQuantaFour() {
        // line 100: blockFluxGoo with meta >= 4 skips the falling branch; the
        // goo row is not registered yet, so the rule is pinned as a predicate
        assertFalse(FallingTaintAiLogic.heldByFluxGoo(
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 4),
                "the flux-goo row is not registered, so nothing is held up");
        assertFalse(FallingTaintAiLogic.isFluxGoo(
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
        assertEquals(0, FallingTaintAiLogic.gooQuanta(
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
    }

    @Test
    void theLandingPlacementAcceptsOnlyFibresAndGoo() {
        // canPlace (lines 130-134): fibres (132) or flux goo (133) — nothing
        // else; the goo row (TC-M5-03) is unregistered so only fibres fires
        final var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        assertTrue(FallingTaintAiLogic.canPlaceInto(air, true), "fibrous taint accepts");
        assertFalse(FallingTaintAiLogic.canPlaceInto(air, false), "any other cell refuses");
        // the landing setBlock (line 117) and the source clear (line 97) run
        // at flags 3 = block update + neighbours
        assertEquals(3, FallingTaintAiLogic.PLACE_BLOCK_FLAGS);
    }
}
