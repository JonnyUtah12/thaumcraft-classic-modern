package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 2x2 room of the Outer Lands maze — a verbatim port of the original
 * {@code thaumcraft.common.lib.world.dim.Gen2x2}, reference
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/world/dim/Gen2x2.java}
 * (245 lines), cited below as "ref". Extends {@link GenCommon} exactly as the
 * original extends its GenCommon (ref L7): every block set in the four
 * quadrant builders rides the inherited {@code placeBlock} code table, so the
 * code → block/meta table, the {@code blockEldritchNothing} gates, the world
 * random draws (ref GenCommon L68-87, L240-246) and the decoration queues
 * live in {@code GenCommon}, where the original had them.
 *
 * <p><b>Numbers verbatim.</b> The chunk origins {@code x = cx*16}/{@code z =
 * cz*16} (ref L9-10 and per quadrant); the four builders' sweep bounds
 * verbatim — upper-left: shell {@code a,b in 1..15}, {@code c in 0..13}, edge
 * gate {@code a==1||b==1} (ref L12-20), void band {@code a,b in 2..15},
 * {@code cxx in 1..12} with the {@code b<=4||b>=12}/{@code a<=4||a>=12} corner
 * exemption, the {@code !cell.west}/{@code !cell.north} exit gates and the
 * {@code cxx>=10} tip rule (ref L22-30), crust band {@code a,b in 3..15},
 * {@code cxxx in 2..11} at {@code a==3||b==3} (ref L32-40), floor/ceiling
 * stack {@code a in 2..15}, {@code b in 2..15} at {@code y-1}/{@code y}/
 * {@code y+1}/{@code y+11}/{@code y+12}/{@code y+13} with codes
 * 1/8/19/2/8/1 (ref L42-51), the stair rails {@code g in 4..15} at
 * {@code y+2}/{@code y+10} codes 10/11 on the {@code z+4} (NORTH) and
 * {@code x+4} (WEST) planes (ref L53-61); upper-right: shell {@code a in
 * 0..15}/{@code b in 1..15} at {@code a==15||b==1} (ref L70-78), void band
 * {@code a in 0..14}/{@code b in 2..15} gated to {@code a==14||b==2} with the
 * {@code cell.east}/{@code cell.north} rules (ref L80-90), crust {@code a in
 * 0..13}/{@code b in 3..15} at {@code a==13||b==3} (ref L92-100), stack
 * {@code a in 0..14}/{@code b in 2..15} (ref L102-111), rails {@code g in
 * 0..11} on {@code z+4} NORTH and {@code g in 4..15} on {@code x+12} EAST
 * (ref L113-121); lower-left: shell {@code a in 1..15}/{@code b in 0..15} at
 * {@code a==1||b==15} (ref L130-138), void band {@code a in 2..15}/{@code b
 * in 0..14} at {@code a==2||b==14} with the {@code cell.west}/{@code
 * cell.south} rules (ref L140-150), crust {@code a in 3..15}/{@code b in
 * 0..13} at {@code a==3||b==13} (ref L152-160), stack {@code a in
 * 2..15}/{@code b in 0..14} (ref L162-171), rails {@code g in 4..15} on
 * {@code z+12} SOUTH and {@code g in 0..11} on {@code x+4} WEST (ref
 * L173-181); lower-right: shell {@code a,b in 0..15} at {@code a==15||b==15}
 * (ref L190-198), void band {@code a,b in 0..14} at {@code a==14||b==14} with
 * the {@code cell.east}/{@code cell.south} rules (ref L200-210), crust
 * {@code a,b in 0..13} at {@code a==13||b==13} (ref L212-220), stack
 * {@code a in 0..14}/{@code b in 0..14} (ref L222-231), rails {@code g in
 * 0..11} on {@code z+12} SOUTH and {@code g in 0..12} on {@code x+12} EAST
 * (ref L233-241)}; and the closing connection pass of every builder —
 * {@code generateConnections(world, random, cx, cz, y, cell, depth = 3,
 * justthetip = true)} (ref L63, 123, 183, 243).</p>
 *
 * <p><b>Random calls.</b> The quadrant bodies draw no random of their own:
 * the chunk {@code random} is carried straight into
 * {@link GenCommon#generateConnections} (which draws none, ref GenCommon
 * L340-414), and every {@code placeBlock} draw rides the <i>world</i> random
 * inside {@link GenCommon#placeBlock}. The random-drawing seams below
 * ({@link #addTorch}, {@link #addSpawner}) document their draws per
 * generation.</p>
 *
 * <h2>1.21.1 adaptation</h2>
 * <ul>
 *   <li><b>Entry contract.</b> The shipped callers fix the entry as the
 *   8-parameter
 *   {@code generateRoom(LevelWriter, Random, int i, int j, int k, int y, int par5, Cell)}
 *   ({@code GenBossRoom} L114-133, the MazeHandler shape the task pins); the
 *   reference's four builders are 6-parameter
 *   {@code (world, random, cx, cz, y, cell)} (ref L8, 66, 126, 186), so the
 *   public entry maps {@code i = cx}, {@code j = cz}, carries {@code y} and
 *   {@code cell}, and routes {@code par5} to the quadrant: 0 → upper-left,
 *   1 → upper-right, 2 → lower-left, 3 → lower-right — the method order of
 *   the original's four builders. The boss room's corner feature → quadrant
 *   mapping (feature 2..5 → 0..3) is the caller's, kept there verbatim.
 *   {@code k} (the chunk-local coordinate of the toolbox contract) has no
 *   consumer in the reference body and is dropped at the seam. The declared
 *   writer type is {@code LevelWriter} (the shipped seam); the {@code
 *   placeBlock} table, the world-random draws and the decoration queues all
 *   need a {@code ServerLevel}, so the writer is narrowed once per entry — a
 *   writer that does not resolve is reported and the room is left unbuilt,
 *   matching the shipped {@code GenPassage}/{@code GenPortal} behaviour. The
 *   four builders keep the reference's names and parameter order
 *   (package-private static, {@code ServerLevel} first).</li>
 *   <li><b>Meta codes.</b> The quadrant bodies pass the original codes
 *   verbatim (1, 2, 8, 10, 11, 18, 19); the code → block/meta resolution,
 *   the {@code blockEldritchNothing} gates and the queue side effects live
 *   in {@link GenCommon#placeBlock} — see its javadoc for the meta mapping
 *   (variant properties, stair FACING/HALF).</li>
 *   <li><b>No tile touch.</b> The reference builders create no tile entity
 *   and call no {@code getTileEntity}; the only tile touch in this class is
 *   the mob spawner's entity id inside {@link #addSpawner} — and the original
 *   1.7.10 reference has no spawner/torch/chest seam at all: those three are
 *   the 1.9-generation {@code Gen2x2} toolbox methods the task pins (the
 *   maze rooms grew them between 1.7.10 and 1.9.4), kept here with the
 *   1.9.4 parameter order {@code (Random, World, BlockPos[, int])} and each
 *   one's documented draws.</li>
 * </ul>
 */
public class Gen2x2 extends GenCommon {

    /**
     * The shipped 8-parameter entry: the reference's quadrant builders,
     * routed by {@code par5}. See the class javadoc for the parameter
     * mapping.
     *
     * @param level the maze level writer (original {@code World}, ref L8)
     * @param random the maze random, carried into the connection pass (ref L63)
     * @param i chunk X of the cell (original {@code cx}, ref L9)
     * @param j chunk Z of the cell (original {@code cz}, ref L10)
     * @param k chunk-local coordinate per the shipped toolbox contract; the
     *          reference body has no consumer, so it is dropped at the seam
     * @param y room floor Y for the quadrant (the MazeHandler call passes the
     *          caller's 50 through {@code GenBossRoom}, ref L25/28/31/34 of
     *          that reference)
     * @param par5 quadrant selector: 0 upper-left, 1 upper-right, 2 lower-left,
     *             3 lower-right — the method order of the four builders
     * @param cell the maze cell (public fields north/south/east/west/feature)
     */
    public static void generateRoom(final LevelWriter level, final Random random, final int i, final int j,
                                    final int k, final int y, final int par5, final Cell cell) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f] Gen2x2 needs a ServerLevel (the world reads, the world "
                    + "random and the decoration queues); the maze is a server-side path, room at chunk ("
                    + i + ", " + j + ") is left unbuilt");
            return;
        }
        switch (par5) {
            case 0:
                generateUpperLeft(world, random, i, j, y, cell);
                break;
            case 1:
                generateUpperRight(world, random, i, j, y, cell);
                break;
            case 2:
                generateLowerLeft(world, random, i, j, y, cell);
                break;
            case 3:
                generateLowerRight(world, random, i, j, y, cell);
                break;
            default:
                break;
        }
    }

    /** The original's upper-left quadrant (ref L8-64), body verbatim. */
    static void generateUpperLeft(final ServerLevel world, final Random random, final int cx, final int cz,
                                  final int y, final Cell cell) {
        final int x = cx * 16; // ref L9
        final int z = cz * 16; // ref L10

        // Ref L12-20: the 15x15x13 shell, only the a==1/b==1 corner faces.
        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 1 || b == 1) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), BEDROCK, cell);
                    }
                }
            }
        }

        // Ref L22-30: the void band, opened at the west/north exits above the tip row.
        for (int a = 2; a <= 15; a++) {
            for (int b = 2; b <= 15; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 2 || b == 2) && (a != 2 || b <= 4 || b >= 12 || !cell.west || cxx >= 10)
                            && (b != 2 || a <= 4 || a >= 12 || !cell.north || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), VOID, cell);
                    }
                }
            }
        }

        // Ref L32-40: the crust band.
        for (int a = 3; a <= 15; a++) {
            for (int b = 3; b <= 15; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if (a == 3 || b == 3) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), ROCK, cell);
                    }
                }
            }
        }

        // Ref L42-51: floor, void, hardened floor, ceiling stack.
        for (int a = 2; a <= 15; a++) {
            for (int b = 2; b <= 15; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), STONE_NOSPAWN, cell);
                placeBlock(world, new BlockPos(x + a, y + 13, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y + 12, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), STONE, cell);
            }
        }

        // Ref L53-56: the north stair rail.
        for (int g = 4; g <= 15; g++) {
            placeBlock(world, new BlockPos(x + g, y + 2, z + 4), STAIR_DIRECTIONAL, Direction.NORTH, cell);
            placeBlock(world, new BlockPos(x + g, y + 10, z + 4), STAIR_DIRECTIONAL_INV, Direction.NORTH, cell);
        }

        // Ref L58-61: the west stair rail.
        for (int g = 4; g <= 15; g++) {
            placeBlock(world, new BlockPos(x + 4, y + 2, z + g), STAIR_DIRECTIONAL, Direction.WEST, cell);
            placeBlock(world, new BlockPos(x + 4, y + 10, z + g), STAIR_DIRECTIONAL_INV, Direction.WEST, cell);
        }

        // Ref L63: the tip-only connection stubs, depth 3.
        generateConnections(world, random, cx, cz, y, cell, 3, true);
    }

    /** The original's upper-right quadrant (ref L66-124), body verbatim. */
    static void generateUpperRight(final ServerLevel world, final Random random, final int cx, final int cz,
                                   final int y, final Cell cell) {
        final int x = cx * 16;
        final int z = cz * 16;

        // Ref L70-78.
        for (int a = 0; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 15 || b == 1) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), BEDROCK, cell);
                    }
                }
            }
        }

        // Ref L80-90.
        for (int a = 0; a <= 14; a++) {
            for (int b = 2; b <= 15; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 14 || b == 2)
                            && (a != 14 || b <= 4 || b >= 12 || !cell.east || cxx >= 10)
                            && (b != 2 || a <= 4 || a >= 12 || !cell.north || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), VOID, cell);
                    }
                }
            }
        }

        // Ref L92-100.
        for (int a = 0; a <= 13; a++) {
            for (int b = 3; b <= 15; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if (a == 13 || b == 3) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), ROCK, cell);
                    }
                }
            }
        }

        // Ref L102-111.
        for (int a = 0; a <= 14; a++) {
            for (int b = 2; b <= 15; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), STONE_NOSPAWN, cell);
                placeBlock(world, new BlockPos(x + a, y + 13, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y + 12, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), STONE, cell);
            }
        }

        // Ref L113-116: the north stair rail (the room's full x span).
        for (int g = 0; g <= 11; g++) {
            placeBlock(world, new BlockPos(x + g, y + 2, z + 4), STAIR_DIRECTIONAL, Direction.NORTH, cell);
            placeBlock(world, new BlockPos(x + g, y + 10, z + 4), STAIR_DIRECTIONAL_INV, Direction.NORTH, cell);
        }

        // Ref L118-121: the east stair rail.
        for (int g = 4; g <= 15; g++) {
            placeBlock(world, new BlockPos(x + 12, y + 2, z + g), STAIR_DIRECTIONAL, Direction.EAST, cell);
            placeBlock(world, new BlockPos(x + 12, y + 10, z + g), STAIR_DIRECTIONAL_INV, Direction.EAST, cell);
        }

        // Ref L123.
        generateConnections(world, random, cx, cz, y, cell, 3, true);
    }

    /** The original's lower-left quadrant (ref L126-184), body verbatim. */
    static void generateLowerLeft(final ServerLevel world, final Random random, final int cx, final int cz,
                                  final int y, final Cell cell) {
        final int x = cx * 16;
        final int z = cz * 16;

        // Ref L130-138.
        for (int a = 1; a <= 15; a++) {
            for (int b = 0; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 1 || b == 15) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), BEDROCK, cell);
                    }
                }
            }
        }

        // Ref L140-150.
        for (int a = 2; a <= 15; a++) {
            for (int b = 0; b <= 14; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 2 || b == 14)
                            && (a != 2 || b <= 4 || b >= 12 || !cell.west || cxx >= 10)
                            && (b != 14 || a <= 4 || a >= 12 || !cell.south || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), VOID, cell);
                    }
                }
            }
        }

        // Ref L152-160.
        for (int a = 3; a <= 15; a++) {
            for (int b = 0; b <= 13; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if (a == 3 || b == 13) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), ROCK, cell);
                    }
                }
            }
        }

        // Ref L162-171.
        for (int a = 2; a <= 15; a++) {
            for (int b = 0; b <= 14; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), STONE_NOSPAWN, cell);
                placeBlock(world, new BlockPos(x + a, y + 13, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y + 12, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), STONE, cell);
            }
        }

        // Ref L173-176: the south stair rail.
        for (int g = 4; g <= 15; g++) {
            placeBlock(world, new BlockPos(x + g, y + 2, z + 12), STAIR_DIRECTIONAL, Direction.SOUTH, cell);
            placeBlock(world, new BlockPos(x + g, y + 10, z + 12), STAIR_DIRECTIONAL_INV, Direction.SOUTH, cell);
        }

        // Ref L178-181: the west stair rail (the room's full z span).
        for (int g = 0; g <= 11; g++) {
            placeBlock(world, new BlockPos(x + 4, y + 2, z + g), STAIR_DIRECTIONAL, Direction.WEST, cell);
            placeBlock(world, new BlockPos(x + 4, y + 10, z + g), STAIR_DIRECTIONAL_INV, Direction.WEST, cell);
        }

        // Ref L183.
        generateConnections(world, random, cx, cz, y, cell, 3, true);
    }

    /** The original's lower-right quadrant (ref L186-244), body verbatim. */
    static void generateLowerRight(final ServerLevel world, final Random random, final int cx, final int cz,
                                   final int y, final Cell cell) {
        final int x = cx * 16;
        final int z = cz * 16;

        // Ref L190-198.
        for (int a = 0; a <= 15; a++) {
            for (int b = 0; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 15 || b == 15) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), BEDROCK, cell);
                    }
                }
            }
        }

        // Ref L200-210.
        for (int a = 0; a <= 14; a++) {
            for (int b = 0; b <= 14; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 14 || b == 14)
                            && (a != 14 || b <= 4 || b >= 12 || !cell.east || cxx >= 10)
                            && (b != 14 || a <= 4 || a >= 12 || !cell.south || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), VOID, cell);
                    }
                }
            }
        }

        // Ref L212-220.
        for (int a = 0; a <= 13; a++) {
            for (int b = 0; b <= 13; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if (a == 13 || b == 13) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), ROCK, cell);
                    }
                }
            }
        }

        // Ref L222-231.
        for (int a = 0; a <= 14; a++) {
            for (int b = 0; b <= 14; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), STONE_NOSPAWN, cell);
                placeBlock(world, new BlockPos(x + a, y + 13, z + b), BEDROCK, cell);
                placeBlock(world, new BlockPos(x + a, y + 12, z + b), VOID, cell);
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), STONE, cell);
            }
        }

        // Ref L233-236: the south stair rail (the room's full x span).
        for (int g = 0; g <= 11; g++) {
            placeBlock(world, new BlockPos(x + g, y + 2, z + 12), STAIR_DIRECTIONAL, Direction.SOUTH, cell);
            placeBlock(world, new BlockPos(x + g, y + 10, z + 12), STAIR_DIRECTIONAL_INV, Direction.SOUTH, cell);
        }

        // Ref L238-241: the east stair rail.
        for (int g = 0; g <= 12; g++) {
            placeBlock(world, new BlockPos(x + 12, y + 2, z + g), STAIR_DIRECTIONAL, Direction.EAST, cell);
            placeBlock(world, new BlockPos(x + 12, y + 10, z + g), STAIR_DIRECTIONAL_INV, Direction.EAST, cell);
        }

        // Ref L243.
        generateConnections(world, random, cx, cz, y, cell, 3, true);
    }

    // ------------------------------------------------------------------ toolbox seams (1.9-generation)

    /**
     * The 1.9-generation toolbox pillar: a crusted-stone column from the
     * cell up to {@code height} cells above the base (base cell and shaft
     * {@code blockEldritch} meta 4, the cap meta 3 on top) — the
     * {@code decorateBlockPillar} family's builder. Draws no random; the
     * callers' rolls gate placement, exactly as the maze queues demand. The
     * 1.7.10 reference has no pillar builder — the Outer Lands pillar block
     * is the eldritch crusted stone, and the metas ride the shipped variant
     * property.
     *
     * @param random the maze random (carried, unused: the builder draws none)
     * @param level  the level writer
     * @param pos    the pillar base
     * @param height the pillar height (the original's 1-5 column)
     */
    public static void generatePillar(final Random random, final LevelWriter level, final BlockPos pos,
                                      final int height) {
        for (int dy = 0; dy < height; dy++) {
            setBlockAndUpdate(pos.above(dy), CatalogEldritchRealm.BLOCK_ELDRITCH.get(), level,
                    EldritchRealmValues.ELDRITCH_CRUSTED);
        }
        setBlockAndUpdate(pos.above(height), CatalogEldritchRealm.BLOCK_ELDRITCH.get(), level,
                EldritchRealmValues.ELDRITCH_CAP);
    }

    /**
     * The 1.9-generation toolbox stairs: a {@code length}-step ascending eldritch
     * stair run from {@code pos} along {@code dir} (one cell up per cell along
     * the direction) with the run's directional metas — code 10's table,
     * EAST 0 / WEST 1 / SOUTH 2 / NORTH 3 (ref GenCommon L159-175), the
     * upside-down return rail rides code 11's metas (ref GenCommon L176-192)
     * at one above the run. Draws no random. The 1.7.10 reference has no stair
     * builder — the rooms place their stairs through the code table.
     *
     * @param random the maze random (carried, unused: the builder draws none)
     * @param level  the level writer
     * @param pos    the run's first step
     * @param dir    the run's direction (horizontal)
     * @param length the number of steps
     */
    public static void generateStairs(final Random random, final LevelWriter level, final BlockPos pos,
                                      final Direction dir, final int length) {
        final int meta = switch (dir) {
            case EAST -> 0;
            case WEST -> 1;
            case SOUTH -> 2;
            case NORTH -> 3;
            default -> 0;
        };
        for (int g = 0; g < length; g++) {
            setBlockAndUpdate(pos.relative(dir, g).above(g),
                    CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get(), level, meta);
        }
    }

    /**
     * The 1.9-generation {@code addTorch(Random, World, BlockPos)} seam: a
     * single {@code nextInt(2)} roll gates the torch (ref of this generation:
     * the 1.9.4 toolbox — the 1.7.10 rooms carry no torch pass); on a hit the
     * torch rides the first air cell in front of a sturdy face, the wall
     * torch facing the wall it hangs on. Vanilla torches: the shipped repo
     * registers no {@code blockTorch} row.
     *
     * @param random the maze random (one {@code nextInt(2)} draw)
     * @param level  the level writer
     * @param pos    the wall cell to hang a torch on
     */
    public static void addTorch(final Random random, final LevelWriter level, final BlockPos pos) {
        if (random.nextInt(2) != 0) {
            return;
        }
        final LevelReader reader = level instanceof LevelReader r ? r : null;
        for (final Direction dir : Direction.values()) {
            final BlockPos face = pos.relative(dir);
            if (reader.getBlockState(face).isAir() && faceSturdy(reader, pos, dir.getOpposite())) {
                level.setBlock(face, Blocks.WALL_TORCH.defaultBlockState()
                        .setValue(WallTorchBlock.FACING, dir.getOpposite()), 3);
                return;
            }
        }
    }

    /**
     * The 1.9-generation {@code addChest(Random, World, BlockPos)} seam: the
     * room chest. Draws no random (the 1.9.4 chest pass gates on the
     * <i>world</i> random inside the placement hook, not on the chunk
     * {@code random} carried here). The shipped repo has no
     * {@code TileThaumcraftChest}; the chest is the vanilla chest and the
     * original's loot 66 = {@code "ThaumVault"} rides the house loot-table
     * pattern — inert until that loot table ships (see
     * {@code GenCommon.CHEST_LOOT_THAUM_VAULT}).
     *
     * @param random the maze random (carried, unused: the seam draws none)
     * @param level  the level writer
     * @param pos    the chest cell
     */
    public static void addChest(final Random random, final LevelWriter level, final BlockPos pos) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            return;
        }
        setBlockAndUpdate(pos, Blocks.CHEST, world, 0);
        applyChestLoot(world, pos);
    }

    /**
     * The 1.9-generation {@code addSpawner(Random, World, BlockPos, int)}
     * seam: the room spawner. Draws no random of its own (the 1.9.4 mob pick
     * rides the <i>world</i> random); the {@code int} is the spawner type:
     * type 1 = the eldritch guardian spawner ({@code "EntityEldritchGuardian"},
     * kept verbatim and inert until TC-M6-06 registers the eldritch mobs),
     * every other type = the original's vanilla {@code "Silverfish"} id. The
     * spawner block is the vanilla mob spawner; the id apply follows the
     * shipped pattern, unresolvable ids fall back to the vanilla id (see
     * {@code GenCommon#applySpawnerId}).
     *
     * @param random the maze random (carried, unused: the seam draws none)
     * @param level  the level writer
     * @param pos    the spawner cell
     * @param type   the original's spawner type (1 = eldritch guardian, else vanilla)
     */
    public static void addSpawner(final Random random, final LevelWriter level, final BlockPos pos, final int type) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            return;
        }
        setBlockAndUpdate(pos, Blocks.SPAWNER, world, 0);
        applySpawnerId(world, pos, type == 1 ? SPAWNER_ID_ELDRITCH_GUARDIAN : FALLBACK_SPAWNER_ID);
    }

    /**
     * The original's {@code BlockUtils.isFaceSturdy} check (1.7.10 form): the
     * face of {@code pos} toward {@code dir} is sturdy (a torch can hang on
     * it).
     */
    private static boolean faceSturdy(final LevelReader reader, final BlockPos pos, final Direction dir) {
        final BlockState state = reader.getBlockState(pos);
        return state.isFaceSturdy(reader, pos, dir);
    }

    /** The {@code LevelWriter} resolved to a {@code ServerLevel}, else null (the shipped sibling pattern). */
    private static ServerLevel serverLevel(final LevelWriter level) {
        return level instanceof ServerLevel server ? server : null;
    }
}
