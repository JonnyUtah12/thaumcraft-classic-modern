package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Blocks;

/**
 * The portal room of the Outer Lands maze — a verbatim port of the original
 * {@code thaumcraft.common.lib.world.dim.GenPortal}, reference
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/world/dim/GenPortal.java}
 * (122 lines), cited below as "ref". Extends {@link GenCommon} exactly as the
 * original extends its GenCommon (ref L9).
 *
 * <p><b>Numbers verbatim.</b> The chunk origins {@code x = cx*16} /
 * {@code z = cz*16} (ref L11-12) — the room is built on the cell's chunk with
 * its centre at {@code x+8}/{@code z+8}, the shipped equivalent of the
 * original's {@code i*16+8}/{@code j*16+8} portal centre. The sweep bounds of
 * every shell are carried unchanged: the 15x15 bedrock wall
 * {@code a,b in 1..15}, {@code c in 0..12} over the edge ring only
 * (ref L14-22); the 13x13 void band {@code a,b in 2..14}, {@code cxx in 1..11}
 * gated by the cell's four exits with the {@code b<=3 || b>=12} corner
 * exemption and the {@code cxx>=10} tip rule (ref L24-36); the 11x11 ancient
 * stone ring {@code a,b in 3..13}, {@code cxxx in 2..10} with the
 * {@code (a>4||b>4) && (a>4||b<12) && (a<12||b>4) && (a<12||b<12)} corner
 * carve (ref L38-46); the floor/ceiling stack at {@code y-1}/{@code y}/
 * {@code y+1}/{@code y+11}/{@code y+12}/{@code y+13} (ref L48-55); the two
 * pyramidal fills driven by {@code q = min(|8-a|, |8-b|)} — {@code g < q-1}
 * up from {@code y+1} and {@code g < q} down from {@code y+11} (ref L56-70);
 * the five-step stair arms {@code x+6+g, z+4}/{@code x+6+g, z+12}/
 * {@code x+12, z+6+g}/{@code x+4, z+6+g} at {@code y+2} (ref L74-79); the
 * corner air columns {@code a,b in 3..13}, {@code cxxxx in 1..11} gated to the
 * four 3x3 corner blocks (ref L83-92); the 24 named stair placements
 * (ref L94-117); and the {@code y+2}/{@code y+3}/{@code y+4} centre stack
 * (ref L118-120).</p>
 *
 * <p><b>Random calls.</b> The portal body itself draws no random: every RNG
 * draw in the room is inside {@link GenCommon#placeBlock} (the original reads
 * the <i>world</i> random, {@code world.field_73012_v}, e.g. ref GenCommon
 * L68/73/74/87/240/243/246 — never the chunk {@code random}) and inside
 * {@link GenCommon#generateConnections}, which the original calls with
 * {@code (world, random, cx, cz, y, cell, depth = 3, justthetip = true)}
 * (ref L81). Keeping that split is what preserves the original's RNG stream:
 * the chunk random advances only in the connection pass.</p>
 *
 * <h2>1.21.1 shape</h2>
 * <ul>
 *   <li><b>Entry contract.</b> The shipped caller {@code MazeHandler} passes
 *   {@code (ServerLevel, Random, chunkX, chunkZ, OuterLands.ROOM_Y, cell)} —
 *   the original's {@code (world, random, cx, cz, 50, cell)} (ref L10, called
 *   from MazeHandler ref L144), so the parameter order is kept, and
 *   {@code OuterLands.ROOM_Y} is the shipped form of the original's hard-coded
 *   50. The declared type is {@code LevelWriter} per the shipped contract; the
 *   code-table placements read the world, draw the world random and queue
 *   decorations, all of which need a {@code ServerLevel}, so the writer is
 *   narrowed once (see {@link #serverLevel}). Maze generation is a server-only
 *   path in both versions (the original ran it from
 *   {@code ThaumcraftWorldGenerator.worldGeneration}); a writer that does not
 *   resolve is reported and skipped rather than half-built.</li>
 *   <li><b>Meta codes.</b> {@code placeBlock} in the original is a code
 *   dispatcher (ref GenCommon L55-259): code 1 = bedrock, 2 =
 *   blockCosmeticSolid meta 11 (ancient stone), 8 = blockEldritchNothing,
 *   9 = air + de-queue, 10/11 = blockStairsEldritch with the directional metas
 *   NORTH 3 / SOUTH 2 / EAST 0 / WEST 1 (10) and NORTH 7 / SOUTH 6 / EAST 4 /
 *   WEST 5 (11), 19 = blockCosmeticSolid meta 13. The codes are passed through
 *   verbatim; the toolbox owns the code → block/meta table, the
 *   {@code blockEldritchNothing} gates and the decoration queues, exactly as
 *   the original's {@code GenCommon} did (the shipped MazeHandler flushes them
 *   with {@code GenCommon.processDecorations}).</li>
 *   <li><b>Directions.</b> {@code ForgeDirection.NORTH/SOUTH/EAST/WEST} map
 *   1:1 to {@code Direction} by name (ref L75-78, L94-117). The codes used
 *   here (10/11) switch on the direction <i>name</i> (ref GenCommon L159-192),
 *   so the 1.7.10 ordinal encoding does not matter for this room; the
 *   ordinal-coded codes 3-6 are the passage's, and there the toolbox must read
 *   the direction through the ForgeDirection encoding
 *   ({@code OuterLands.forgeOrdinal}) — see {@code GenPassage}.</li>
 *   <li><b>Tile touch.</b> The portal builder touches no tile entity: the
 *   original's whole portal-head placement is the two block sets at
 *   ref L118-119 plus {@code genObelisk} (ref L120). The {@code setDelay(450)}
 *   render-open delay and {@code markTileEntityForRenderUpdate} of the
 *   source-based variant have no call site in this reference file, so nothing
 *   is invented here: the shipped {@code TileEldritchPortal} keeps
 *   {@code opencount}/{@code count} (its render cadence ships on TC-M7-02) and
 *   this builder leaves both at their constructed values. The original's
 *   render-mark equivalent is a 1.21.1 no-op; the neighbour fact it feeds
 *   ({@code blockEldritch} above and below) is answered by the shipped
 *   {@code TileEldritchPortal.hasEldritchFrame()}, which reads the block
 *   states placed at ref L118 and the obelisk stack.</li>
 *   <li><b>No dimension test, no floor math.</b> The original portal builder
 *   reads no {@code Config.dimensionOuterId} and calls no
 *   {@code MathHelper.floor_double}, so neither {@code OuterLands.isOuterLands}
 *   nor {@code Mth.floor} is needed; the plane is fixed by the shipped caller
 *   ({@code OuterLands.OUTER_LANDS}).</li>
 * </ul>
 */
public class GenPortal extends GenCommon {

    /** The air set of ref L88 (the original's 4-arg {@code setBlock} overload, meta 0). */
    private static final int META_AIR = 0;

    /**
     * The portal room: shells, void band, ancient-stone ring, floor/ceiling
     * stack, the two pyramids, the four stair arms, the connection stubs, the
     * corner air columns, the 24 named stairs, and the centre
     * cap/portal/obelisk stack.
     *
     * @param level the maze level writer (original {@code World}, ref L10)
     * @param random the maze random, consumed only by the connection pass (ref L81)
     * @param cx chunk X of the cell (original {@code cx}, ref L11)
     * @param cz chunk Z of the cell (original {@code cz}, ref L12)
     * @param y room floor Y (the original passes 50 at its call site, MazeHandler ref L144)
     * @param cell the maze cell; its {@code north/south/east/west} exits gate
     *             the void band (ref L28-31)
     */
    public static void generatePortal(final LevelWriter level, final Random random, final int cx, final int cz,
                                      final int y, final Cell cell) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f] GenPortal needs a ServerLevel (world reads, the world "
                    + "random and the decoration queues); the maze is a server-side path, room at chunk ("
                    + cx + ", " + cz + ") is left unbuilt");
            return;
        }

        final int x = cx * 16; // ref L11
        final int z = cz * 16; // ref L12

        // Ref L14-22: the 15x15 bedrock wall, 13 high, over the edge ring only.
        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 1 || a == 15 || b == 1 || b == 15) {
                        place(world, new BlockPos(x + a, y + c, z + b), 1, cell);
                    }
                }
            }
        }

        // Ref L24-36: the 13x13 void band (blockEldritchNothing), opened only
        // where the cell has an exit, and only above the tip row (cxx >= 10).
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 2 || a == 14 || b == 2 || b == 14)
                            && (a != 2 || b <= 3 || b >= 12 || !cell.west || cxx >= 10)
                            && (a != 14 || b <= 3 || b >= 12 || !cell.east || cxx >= 10)
                            && (b != 2 || a <= 3 || a >= 12 || !cell.north || cxx >= 10)
                            && (b != 14 || a <= 3 || a >= 12 || !cell.south || cxx >= 10)) {
                        place(world, new BlockPos(x + a, y + cxx, z + b), 8, cell);
                    }
                }
            }
        }

        // Ref L38-46: the 11x11 ancient-stone ring, corners carved out.
        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if ((a == 3 || a == 13 || b == 3 || b == 13) && (a > 4 || b > 4) && (a > 4 || b < 12)
                            && (a < 12 || b > 4) && (a < 12 || b < 12)) {
                        place(world, new BlockPos(x + a, y + cxxx, z + b), 2, cell);
                    }
                }
            }
        }

        // Ref L48-72: the floor and ceiling stack, then the two pyramids from
        // q = min(|8-a|, |8-b|).
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                place(world, new BlockPos(x + a, y - 1, z + b), 1, cell);   // ref L50
                place(world, new BlockPos(x + a, y, z + b), 8, cell);       // ref L51
                place(world, new BlockPos(x + a, y + 1, z + b), 19, cell);  // ref L52
                place(world, new BlockPos(x + a, y + 13, z + b), 1, cell);  // ref L53
                place(world, new BlockPos(x + a, y + 12, z + b), 8, cell);  // ref L54
                place(world, new BlockPos(x + a, y + 11, z + b), 2, cell);  // ref L55

                if (a > 1 && a < 15 && b > 1 && b < 15) { // ref L56
                    final int q = Math.min(Math.abs(8 - a), Math.abs(8 - b)); // ref L57

                    for (int g = 0; g < q - 1; g++) {                         // ref L59-61
                        place(world, new BlockPos(x + a, y + 1 + g, z + b), 19, cell);
                    }
                }

                if (a > 3 && a < 13 && b > 3 && b < 13) { // ref L64
                    final int q = Math.min(Math.abs(8 - a), Math.abs(8 - b)); // ref L65

                    for (int g = 0; g < q; g++) {                             // ref L67-69
                        place(world, new BlockPos(x + a, y + 11 - g, z + b), 19, cell);
                    }
                }
            }
        }

        // Ref L74-79: the five-step directional-stair arms on all four faces.
        for (int g = 0; g < 5; g++) {
            place(world, new BlockPos(x + 6 + g, y + 2, z + 4), 10, Direction.NORTH, cell);
            place(world, new BlockPos(x + 6 + g, y + 2, z + 12), 10, Direction.SOUTH, cell);
            place(world, new BlockPos(x + 12, y + 2, z + 6 + g), 10, Direction.EAST, cell);
            place(world, new BlockPos(x + 4, y + 2, z + 6 + g), 10, Direction.WEST, cell);
        }

        // Ref L81: the connection stubs, depth 3, tip only.
        GenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);

        // Ref L83-92: the four 3x3 corner columns are hollowed: code 9 (air +
        // de-queue the decorations queued under it), then the original's own
        // air set, kept verbatim as the second write.
        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int cxxxx = 1; cxxxx < 12; cxxxx++) {
                    if (a <= 4 && b <= 4 || a <= 4 && b >= 12 || a >= 12 && b <= 4 || a >= 12 && b >= 12) {
                        final BlockPos pos = new BlockPos(x + a, y + cxxxx, z + b);
                        place(world, pos, 9, cell);                                        // ref L87
                        GenCommon.setBlock(pos, Blocks.AIR, level, true, META_AIR);              // ref L88
                    }
                }
            }
        }

        // Ref L94-117: the 24 named stair placements (codes 10 and 11).
        place(world, new BlockPos(x + 5, y + 3, z + 5), 10, Direction.NORTH, cell);    // ref L94
        place(world, new BlockPos(x + 4, y + 3, z + 5), 10, Direction.NORTH, cell);    // ref L95
        place(world, new BlockPos(x + 5, y + 3, z + 4), 10, Direction.WEST, cell);     // ref L96
        place(world, new BlockPos(x + 5, y + 8, z + 5), 11, Direction.NORTH, cell);    // ref L97
        place(world, new BlockPos(x + 4, y + 8, z + 5), 11, Direction.NORTH, cell);    // ref L98
        place(world, new BlockPos(x + 5, y + 8, z + 4), 11, Direction.WEST, cell);     // ref L99
        place(world, new BlockPos(x + 12, y + 3, z + 5), 10, Direction.NORTH, cell);   // ref L100
        place(world, new BlockPos(x + 11, y + 3, z + 5), 10, Direction.NORTH, cell);   // ref L101
        place(world, new BlockPos(x + 11, y + 3, z + 4), 10, Direction.EAST, cell);    // ref L102
        place(world, new BlockPos(x + 12, y + 8, z + 5), 11, Direction.NORTH, cell);   // ref L103
        place(world, new BlockPos(x + 11, y + 8, z + 5), 11, Direction.NORTH, cell);   // ref L104
        place(world, new BlockPos(x + 11, y + 8, z + 4), 11, Direction.EAST, cell);    // ref L105
        place(world, new BlockPos(x + 5, y + 3, z + 11), 10, Direction.SOUTH, cell);   // ref L106
        place(world, new BlockPos(x + 4, y + 3, z + 11), 10, Direction.SOUTH, cell);   // ref L107
        place(world, new BlockPos(x + 5, y + 3, z + 12), 10, Direction.WEST, cell);    // ref L108
        place(world, new BlockPos(x + 5, y + 8, z + 11), 11, Direction.SOUTH, cell);   // ref L109
        place(world, new BlockPos(x + 4, y + 8, z + 11), 11, Direction.SOUTH, cell);   // ref L110
        place(world, new BlockPos(x + 5, y + 8, z + 12), 11, Direction.WEST, cell);    // ref L111
        place(world, new BlockPos(x + 12, y + 3, z + 11), 10, Direction.SOUTH, cell);  // ref L112
        place(world, new BlockPos(x + 11, y + 3, z + 11), 10, Direction.SOUTH, cell);  // ref L113
        place(world, new BlockPos(x + 11, y + 3, z + 12), 10, Direction.EAST, cell);   // ref L114
        place(world, new BlockPos(x + 12, y + 8, z + 11), 11, Direction.SOUTH, cell);  // ref L115
        place(world, new BlockPos(x + 11, y + 8, z + 11), 11, Direction.SOUTH, cell);  // ref L116
        place(world, new BlockPos(x + 11, y + 8, z + 12), 11, Direction.EAST, cell);   // ref L117

        // Ref L118: the portal head base — blockEldritch meta 3 (the cap
        // variant) at the cell centre x+8/z+8, the original's set-with-meta
        // call with update flags 3 (flag true).
        GenCommon.setBlock(new BlockPos(x + 8, y + 2, z + 8), CatalogEldritchRealm.BLOCK_ELDRITCH.get(),
                level, true, EldritchRealmValues.ELDRITCH_CAP);

        // Ref L119: the portal itself, one above the base, at the original's
        // i*16+8 / j*16+8 centre. The shipped block carries its own tile
        // (TileEldritchPortal) and its own light 15; no tile field is written
        // here, matching the original's tile-free portal builder.
        GenCommon.setBlock(new BlockPos(x + 8, y + 3, z + 8), CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get(),
                level, true, 0);

        // Ref L120: the obelisk on top of the portal.
        GenCommon.genObelisk(world, new BlockPos(x + 8, y + 4, z + 8));
    }

    /**
     * The original's {@code placeBlock(world, x, y, z, code, cell)} (ref
     * GenCommon L49-51): the code dispatcher, kept as a dispatcher call so the
     * code → block/meta table, the {@code blockEldritchNothing} gates, the
     * {@code world.rand} draws and the decoration queues stay in
     * {@link GenCommon}, where the original had them.
     *
     * <p>Named {@code place} here, not {@code placeBlock}: a static with the
     * toolbox's exact signature in a subclass of {@code GenCommon} would be an
     * override of the inherited static with weaker (private) access, which javac
     * rejects. {@code GenBossRoom} uses the same {@code place(...)} name.
     */
    private static void place(final ServerLevel level, final BlockPos pos, final int code, final Cell cell) {
        GenCommon.placeBlock(level, pos, code, cell);
    }

    /**
     * The original's directional {@code placeBlock(world, x, y, z, code, dir,
     * cell)} (ref GenCommon L55): {@code ForgeDirection} → {@code Direction}
     * by name.
     */
    private static void place(final ServerLevel level, final BlockPos pos, final int code,
                              final Direction dir, final Cell cell) {
        GenCommon.placeBlock(level, pos, code, dir, cell);
    }

    /**
     * The {@code LevelWriter} resolved to a {@code ServerLevel}, else null.
     * {@code ServerLevel} implements {@code LevelWriter} through
     * {@code Level}/{@code LevelAccessor}, so the shipped maze call
     * ({@code MazeHandler.generateEldritch}) always resolves.
     */
    private static ServerLevel serverLevel(final LevelWriter level) {
        return level instanceof ServerLevel server ? server : null;
    }
}
