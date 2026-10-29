package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The wisp family's push-out-of-blocks decision (TC-M7-02-c-s-f1b).
 *
 * <p>1.7.10 {@code FXWisp}/{@code FXWispEG} each carry their OWN copy of
 * {@code pushOutOfBlocks(x, y, z)} (FXWisp.java:214-297, FXWispEG.java:99-183)
 * — it is Thaumcraft code, not an {@code EntityFX} API, so 1.21.1's removal of
 * the platform-side equivalent costs nothing: the method ports line for line.
 * A mote standing inside a solid, non-water cell picks the nearest open
 * neighbour face and gets a small outward velocity on that axis (the caller
 * owns the roll constants, exactly as the original's two local vars).</p>
 *
 * <p>The two simplifications from the 1.21.1 API are recorded in BD-17:
 * the water test reads the mote's own cell (the original sweeps its bounding
 * box, and these motes measure 0.1F/0.01F — sub-cell), and the solid test is
 * the collision-shape full-block check (the original's {@code isBlockSolid(…,
 * true)} is the all-faces-solid query).</p>
 */
@OnlyIn(Dist.CLIENT)
final class FxWispPush {

    /** No open face in the solid cell: no push. */
    static final int NONE = -1;
    /** Axis/direction codes, matching the original's var22 switch arms. */
    static final int WEST = 0;
    static final int EAST = 1;
    static final int DOWN = 2;
    static final int UP = 3;
    static final int NORTH = 4;
    static final int SOUTH = 5;

    private FxWispPush() {
    }

    /**
     * The mote's block cell, in the original's terms: the cell must be
     * non-air, solid, and not water (FXWisp.java:221-223); the push axis is
     * the nearest face whose neighbour cell is open (lines 224-260).
     *
     * @return one of the {@code WEST..SOUTH} codes, or {@link #NONE}
     */
    static int pushDirection(final ClientLevel level, final double x, final double y, final double z) {
        final int bx = Mth.floor(x);
        final int by = Mth.floor(y);
        final int bz = Mth.floor(z);
        final BlockPos pos = new BlockPos(bx, by, bz);
        final BlockState cell = level.getBlockState(pos);
        final double dx = x - bx;
        final double dy = y - by;
        final double dz = z - bz;
        // FXWisp.java:221-223: !isAirBlock && isBlockSolid(..., true) && !inWater.
        if (cell.isAir() || !isSolidCell(level, pos) || cell.is(Blocks.WATER)) {
            return NONE;
        }
        final boolean west = !isSolidCell(level, pos.west());
        final boolean east = !isSolidCell(level, pos.east());
        final boolean down = !isSolidCell(level, pos.below());
        final boolean up = !isSolidCell(level, pos.above());
        final boolean north = !isSolidCell(level, pos.north());
        final boolean south = !isSolidCell(level, pos.south());
        int direction = NONE;
        double nearest = 9999.0;
        // FXWisp.java:232-260, the same six-way nearest-open-face scan.
        if (west && dx < nearest) {
            nearest = dx;
            direction = WEST;
        }
        if (east && 1.0 - dx < nearest) {
            nearest = 1.0 - dx;
            direction = EAST;
        }
        if (down && dy < nearest) {
            nearest = dy;
            direction = DOWN;
        }
        if (up && 1.0 - dy < nearest) {
            nearest = 1.0 - dy;
            direction = UP;
        }
        if (north && dz < nearest) {
            nearest = dz;
            direction = NORTH;
        }
        if (south && 1.0 - dz < nearest) {
            nearest = 1.0 - dz;
            direction = SOUTH;
        }
        return direction;
    }

    private static boolean isSolidCell(final ClientLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.isCollisionShapeFullBlock(level, pos);
    }
}
