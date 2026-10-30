package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchLock;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;

/**
 * The boss room of the Outer Lands maze (the original's
 * {@code thaumcraft.common.lib.world.dim.GenBossRoom}, reference
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/world/dim/GenBossRoom.java},
 * cited below as "ref"). Extends {@link GenCommon} exactly as the original
 * extends its GenCommon (ref L9).
 *
 * <p><b>Numbers verbatim.</b> The 7x7 {@link #PAT_DOORWAY} matrix (ref L10-18,
 * rows {@code {0,2,2,2,2,2,0} / {2,2,9,9,9,2,2} / {2,9,9,9,9,9,2} /
 * {2,9,9,1,9,9,2} / {2,9,9,9,9,9,2} / {2,2,9,9,9,2,2} / {0,2,2,2,2,2,0}),
 * the chunk origins {@code x = cx*16}/{@code z = cz*16} (ref L21-22), the
 * feature switch 2→upper-left, 3→upper-right, 4→lower-left, 5→lower-right
 * (ref L23-35), the hard-coded {@code 50} y the corner rooms are built at
 * (ref L25/28/31/34), the 7x7 doorway sweep
 * {@code for a in 0..6, for b in 0..6} (ref L37-38), the per-opening offsets
 * north {@code (x+5+a, z+3)}, south {@code (x+5+a, z+13)}, east
 * {@code (x+13, z+5+a)}, west {@code (x+3, z+5+a)} (ref L42-64), the doorway
 * elevation {@code y + 2 + b} (ref L68/75/78) and the pattern codes
 * 1→blockEldritch meta 8 (Ancient Locking Mechanism, ref L68 via
 * {@code GenCommon} code 16, GenCommon ref L200-206), 2→blockEldritch meta 7
 * (gate, ref L75 via code 15, GenCommon ref L193-199) and 9→blockAiry meta 12
 * (void/eldritch-portal airy, ref L78 via code 17, GenCommon ref L207-210)
 * are all carried unchanged. The lock tile's {@code setFacing((byte)dir
 * .ordinal())} (ref L69-72) keeps the original ForgeDirection ordinal bytes
 * (UNKNOWN=0, NORTH=3, SOUTH=4, WEST=5, EAST=6) because that byte is the
 * shipped {@link TileEldritchLock#setFacing(byte)} wire format that drives
 * the original's face-specific icons.</p>
 *
 * <p><b>1.21.1 adaptation.</b> The shipped call contract
 * ({@code MazeHandler}) fixes the entry as
 * {@code generateRoom(LevelWriter, Random, int i, int j, int k, int y, int par5, Cell)}:
 * the reference's six-parameter {@code (world, random, cx, cz, y, cell)}
 * (ref L20, called as {@code (world, random, cx, cz, 50, cell)} from
 * MazeHandler ref L150) maps to {@code i=cx, j=cz, y=y, cell=cell}. The
 * reference body consumes no chunk {@code k} and no caller quadrant: the
 * corner is picked from {@code cell.feature} (ref L23-35), and the shipped
 * MazeGenerator assigns the corner cluster features 2..5 as the quadrant
 * encoding (MazeGenerator.java L109: "2x2 room (feature 2), boss room (3),
 * key room (4), nest room (5)"), so the passed {@code par5} (quadrant)
 * agrees with {@code cell.feature - 2} and is carried through to the Gen2x2
 * call as {@code k}/{@code par5} per the toolbox contract. The original's
 * package-private {@code Gen2x2.generateUpperLeft/UpperRight/LowerLeft/
 * LowerRight(world, random, cx, cz, 50, cell)} (Gen2x2 ref L8/L66/L126/L186)
 * are not on the shipped toolbox seam, so the four corners are reached
 * through the public {@link Gen2x2#generateRoom(LevelWriter, Random, int, int, int, int, int, Cell)}
 * with quadrant codes 0=upper-left, 1=upper-right, 2=lower-left, 3=lower-
 * right — the method order of the original's four corner builders.
 * {@code World.func_147438_o} (ref L69) becomes
 * {@code ServerLevel.getBlockEntity}; the entry takes a {@code LevelWriter},
 * so the tile touch and the meta-bearing placements run on the resolved
 * {@code ServerLevel} when the writer resolves to one, and fall back to
 * {@link GenCommon#setBlock(BlockPos, Block, LevelWriter, boolean, int)}
 * otherwise. The placements use the toolbox {@code flag=true}: the original's
 * final set passes update-flag 3 for blockEldritch and blockAiry (GenCommon
 * ref L254-257 — only eldritchNothing/bedrock/air get 0, none of which the
 * three doorway codes place). The original spawns no mobs here — the boss
 * spawn lives in the shipped tile seam ({@code TileEldritchLock.doBossSpawn}
 * handing off to {@code EldritchBossRoomHost}).</p>
 */
public class GenBossRoom extends GenCommon {

    /** The original's 7x7 doorway pattern (ref L10-18): 0 = nothing, 2 = gate, 9 = void airy, 1 = lock. */
    static final int[][] PAT_DOORWAY = new int[][]{
            {0, 2, 2, 2, 2, 2, 0},
            {2, 2, 9, 9, 9, 2, 2},
            {2, 9, 9, 9, 9, 9, 2},
            {2, 9, 9, 1, 9, 9, 2},
            {2, 9, 9, 9, 9, 9, 2},
            {2, 2, 9, 9, 9, 2, 2},
            {0, 2, 2, 2, 2, 2, 0}
    };

    /** The original's ForgeDirection ordinals used as the lock tile's facing byte (ref L41-71). */
    private static final byte DIR_UNKNOWN = 0;
    private static final byte DIR_NORTH = 3;
    private static final byte DIR_SOUTH = 4;
    private static final byte DIR_WEST = 5;
    private static final byte DIR_EAST = 6;

    /**
     * The room: the quadrant's 2x2 shell first (feature switch, ref L23-35),
     * then the 7x7 doorway wall across the cell's open sides (ref L37-81).
     *
     * @param level the maze level writer (original {@code World}, ref L20)
     * @param random the maze random, carried through to {@link Gen2x2} (ref L25-34)
     * @param i chunk X of the cell (original {@code cx}, ref L21)
     * @param j chunk Z of the cell (original {@code cz}, ref L22)
     * @param k chunk-local coordinate per the shipped 8-arg toolbox contract;
     *          the reference body (a 6-arg entry, ref L20) has no consumer
     *          for it, so it is passed straight to {@link Gen2x2#generateRoom}
     * @param y room floor Y for the doorway sweep (ref L68/75/78; the corner
     *          rooms keep the original's hard-coded 50, ref L25-34)
     * @param par5 caller quadrant per the shipped contract; the corner is
     *             selected from {@code cell.feature} verbatim (ref L23-35)
     * @param cell the maze cell (public fields north/south/east/west/feature,
     *             Cell.java L18-24; the task's exits[4] accessor pair is not
     *             on the shipped Cell)
     */
    public static void generateRoom(final LevelWriter level, final Random random, final int i, final int j,
                                    final int k, final int y, final int par5, final Cell cell) {
        final int x = i * 16; // ref L21
        final int z = j * 16; // ref L22

        // Ref L23-35: feature 2..5 -> upper-left, upper-right, lower-left,
        // lower-right; the original calls the corner builder at y=50.
        switch (cell.feature) {
            case 2:
                Gen2x2.generateRoom(level, random, i, j, k, 50, 0, cell);
                break;
            case 3:
                Gen2x2.generateRoom(level, random, i, j, k, 50, 1, cell);
                break;
            case 4:
                Gen2x2.generateRoom(level, random, i, j, k, 50, 2, cell);
                break;
            case 5:
                Gen2x2.generateRoom(level, random, i, j, k, 50, 3, cell);
                break;
            default:
                break;
        }

        // Ref L37-81: the 7x7 doorway sweep over every open side.
        for (int a = 0; a < 7; a++) {
            for (int b = 0; b < 7; b++) {
                int xx = 0;
                int zz = 0;
                byte dir = DIR_UNKNOWN; // ref L39-41
                if (cell.north) { // ref L42-46
                    xx = x + 5 + a;
                    zz = z + 3;
                    dir = DIR_NORTH;
                }
                if (cell.south) { // ref L48-52
                    xx = x + 5 + a;
                    zz = z + 13;
                    dir = DIR_SOUTH;
                }
                if (cell.east) { // ref L54-58
                    xx = x + 13;
                    zz = z + 5 + a;
                    dir = DIR_EAST;
                }
                if (cell.west) { // ref L60-64
                    xx = x + 3;
                    zz = z + 5 + a;
                    dir = DIR_WEST;
                }

                final BlockPos pos = new BlockPos(xx, y + 2 + b, zz); // ref L68/75/78
                switch (PAT_DOORWAY[a][b]) {
                    case 1:
                        // Code 16: blockEldritch meta 8, the Ancient Locking
                        // Mechanism (GenCommon ref L200-206); then the lock
                        // tile's facing (ref L69-72).
                        place(level, pos, CatalogEldritchRealm.BLOCK_ELDRITCH.get(),
                                EldritchRealmValues.ELDRITCH_LOCK);
                        final ServerLevel server = serverLevel(level);
                        if (server != null
                                && server.getBlockEntity(pos) instanceof TileEldritchLock lock) {
                            lock.setFacing(dir);
                        }
                        break;
                    case 2:
                        // Code 15: blockEldritch meta 7, the gate (GenCommon ref L193-199).
                        place(level, pos, CatalogEldritchRealm.BLOCK_ELDRITCH.get(),
                                EldritchRealmValues.ELDRITCH_GATE);
                        break;
                    case 9:
                        // Code 17: blockAiry meta 12, the void doorway
                        // (GenCommon ref L207-210; BlockAiry.MAX_VARIANT == 12).
                        place(level, pos, CatalogDevices.BLOCK_AIRY.get(), 12);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * The original's {@code placeBlock(world, x, y, z, code, cell)} tail:
     * a meta-bearing set with the update flag the original passes for these
     * blocks (3 → {@code true}, GenCommon ref L254-257). The b-code →
     * block/meta resolution is done at the call sites above against the
     * shipped catalog; the toolbox owns the set itself.
     */
    private static void place(final LevelWriter level, final BlockPos pos, final Block block, final int meta) {
        final ServerLevel server = serverLevel(level);
        if (server != null) {
            GenCommon.placeBlock(pos, block, server, true, meta);
        } else {
            GenCommon.setBlock(pos, block, level, true, meta);
        }
    }

    /** The {@code LevelWriter} resolved to a {@code ServerLevel}, else null (tile access needs it). */
    private static ServerLevel serverLevel(final LevelWriter level) {
        return level instanceof ServerLevel s ? s : null;
    }
}
