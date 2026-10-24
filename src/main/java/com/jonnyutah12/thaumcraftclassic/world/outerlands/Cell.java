package com.jonnyutah12.thaumcraftclassic.world.outerlands;

/**
 * One maze cell of the Outer Lands procedural maze (TC-M6-04f), ported 1:1 from
 * the original {@code thaumcraft.common.lib.world.dim.Cell}.
 *
 * <p>A cell is packed into a {@code short}: the low byte holds the wall
 * connections (N=1, S=2, E=4, W=8, A=16, B=32 — see
 * {@link MazeGenerator}) and the high byte holds the feature id. Feature ids
 * (original values, frozen): 0 none, 1 portal, 2 2x2 room, 3 boss room,
 * 4 key room, 5 nest room, 6 library, 7 altar, 8 obelisk, 9 cap,
 * 10 crusted stone, 11 glyphed stone, 12 ancient doorway, 13 ancient lock,
 * 14 mob spawner, 99 temporary "carved extra" marker remapped to 0 after
 * generation.</p>
 */
public class Cell {

    public boolean north;
    public boolean south;
    public boolean east;
    public boolean west;
    public boolean a;
    public boolean b;
    public int feature;

    public Cell() {
    }

    public Cell(short in) {
        this.north = (in & MazeGenerator.N) != 0;
        this.south = (in & MazeGenerator.S) != 0;
        this.east = (in & MazeGenerator.E) != 0;
        this.west = (in & MazeGenerator.W) != 0;
        this.a = (in & MazeGenerator.A) != 0;
        this.b = (in & MazeGenerator.B) != 0;
        this.feature = in >> 8;
    }

    public short pack() {
        int out = 0;
        if (this.north) {
            out |= MazeGenerator.N;
        }
        if (this.south) {
            out |= MazeGenerator.S;
        }
        if (this.east) {
            out |= MazeGenerator.E;
        }
        if (this.west) {
            out |= MazeGenerator.W;
        }
        if (this.a) {
            out |= MazeGenerator.A;
        }
        if (this.b) {
            out |= MazeGenerator.B;
        }
        out |= this.feature << 8;
        return (short) out;
    }
}
