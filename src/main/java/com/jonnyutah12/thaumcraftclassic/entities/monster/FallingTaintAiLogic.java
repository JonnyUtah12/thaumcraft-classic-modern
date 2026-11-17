package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Level-free physics of the falling taint block (TC-M6-06-s-b). Every
 * constant quotes {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * entities/EntityFallingTaint.java} (the tick body {@code func_70071_h_}
 * is lines 76-128; {@code canPlace} is lines 130-134).
 */
public final class FallingTaintAiLogic {

    /**
     * Gravity line 81 ({@code field_70181_x -= 0.04F}) and the drag on
     * lines 83-85: ALL THREE motion axes are then scaled by {@code 0.98F}
     * ({@code field_70159_w *= 0.98F; field_70181_x *= 0.98F;
     * field_70179_y *= 0.98F}).
     */
    public static final double GRAVITY = -0.04D;
    public static final double DRAG = 0.98D;

    /**
     * Lines 106-108: on landing the x/z motion scales by {@code 0.7F} and
     * the y motion is inverted at {@code -0.5} (line 108: {@code *= -0.5}).
     */
    public static final double LAND_XZ = 0.7D;
    public static final double LAND_Y = -0.5D;

    /** Lines 113-114: the landing sound is played at volume {@code 0.5F}. */
    public static final float LAND_SOUND_VOLUME = 0.5F;
    /** Lines 113-114: {@code (rand-rand) * 0.2F + 1.0F}, then scaled by 0.8F. */
    public static final float LAND_SOUND_PITCH_BASE = 1.0F;
    public static final float LAND_SOUND_PITCH_SPREAD = 0.2F;
    public static final float LAND_SOUND_PITCH_SCALE = 0.8F;

    /** Lines 113-114: {@code ((rand-rand) * 0.2F + 1.0F) * 0.8F}. */
    public static float landSoundPitch(final float rollA, final float rollB) {
        return ((rollA - rollB) * LAND_SOUND_PITCH_SPREAD + LAND_SOUND_PITCH_BASE)
                * LAND_SOUND_PITCH_SCALE;
    }

    /**
     * Lines 100-101: the flux-goo float rule. The original gates on the
     * INVERTED condition — when the block under the feet is flux goo whose
     * meta (quanta) is at least {@code 4} ({@code func_72805_g(...) < 4} is
     * the falling branch's test), the fall loop is SKIPPED: goo support
     * holds the taint up.
     */
    public static boolean heldByFluxGoo(final BlockState below, final int minQuanta) {
        return isFluxGoo(below) && gooQuanta(below) >= minQuanta;
    }

    /** The flux goo block row (TC-M5-03) is not registered yet: no support. */
    public static boolean isFluxGoo(final BlockState below) {
        return false;
    }

    public static int gooQuanta(final BlockState below) {
        return 0;
    }

    /**
     * Lines 102-104: discard after {@code 100} ticks spent outside the
     * hardcoded {@code [1, 256]} y window (the original's literal
     * {@code 256}, not the level's build height), and after {@code 600}
     * total ticks ({@code if (fallTime > 100 && (j < 1 || j > 256) ||
     * fallTime > 600) setDead();}).
     */
    public static final int MAX_OUT_OF_WORLD_TICKS = 100;
    public static final int MAX_FALL_TICKS = 600;
    /** Lines 102-104: the original's hardcoded upper y bound. */
    public static final int MAX_FALL_Y = 256;

    public static boolean discardByHeight(final int fallTime, final int y) {
        return fallTime > MAX_OUT_OF_WORLD_TICKS && (y < 1 || y > MAX_FALL_Y);
    }

    /**
     * Lines 91-97: the FIRST tick ({@code fallTime == 1}) verifies the
     * source cell still holds the falling block (92-95: otherwise the
     * entity dies WITHOUT clearing it) and then clears that source cell
     * (line 97, {@code func_147468_f} = setBlockToAir).
     */
    public static final int FIRST_TICK = 1;

    /**
     * Lines 110-112: the landing placement is refused when the target cell
     * is a piston, a piston extension or a piston head
     * ({@code Blocks.field_150331_J / field_150326_M / field_150332_K} per
     * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}) — the
     * original's gate is the piston family, NOT water/lava.
     */
    public static boolean isPistonFamily(final BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.PISTON)
                || state.is(net.minecraft.world.level.block.Blocks.MOVING_PISTON)
                || state.is(net.minecraft.world.level.block.Blocks.PISTON_HEAD);
    }

    /**
     * {@code canPlace} (lines 130-134): the target cell must be taint
     * fibres (line 132) or flux goo (line 133) — nothing else. Flux goo is
     * not registered yet (TC-M5-03), so only the fibres arm can fire; the
     * third arm of the landing gate is the {@code setBlock} success at
     * flags 3 (line 117, see {@link #PLACE_BLOCK_FLAGS}).
     */
    public static boolean canPlaceInto(final BlockState target, final boolean isFibres) {
        return isFibres || isFluxGoo(target);
    }

    /**
     * Line 117 ({@code func_147465_d(i, j, k, this.block, this.metadata,
     * 3)}) and line 97 ({@code func_147468_f} = setBlockToAir, the same
     * flags): setBlock flags {@code 3} = block update + neighbours —
     * 1.21.1's bit layout carries the same meaning at the same value.
     */
    public static final int PLACE_BLOCK_FLAGS = 3;

    private FallingTaintAiLogic() {
    }
}
