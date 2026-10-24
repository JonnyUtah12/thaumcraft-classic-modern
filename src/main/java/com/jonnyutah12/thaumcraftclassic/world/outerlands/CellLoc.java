package com.jonnyutah12.thaumcraftclassic.world.outerlands;

/**
 * Maze-grid coordinate, ported 1:1 from the original
 * {@code thaumcraft.common.lib.world.dim.CellLoc} (fields keep the original
 * x/z naming: z indexes the grid rows).
 */
public class CellLoc {

    public int x;
    public int z;

    public CellLoc(int x, int z) {
        this.x = x;
        this.z = z;
    }
}
