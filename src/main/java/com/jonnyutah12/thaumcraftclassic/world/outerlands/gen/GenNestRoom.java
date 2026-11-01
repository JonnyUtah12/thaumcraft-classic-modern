package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;

import static com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon.placeBlock;
import static com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon.setBlockAndUpdate;

/**
 * The TC maze nest room — {@code MazeGenerator.FEATURE_NEST} (7), the crust
 * platform room the crab spawners colonise (content-mapping.csv row
 * {@code thaumcraft:worldgen.outer_lands_procedural_maze}).
 *
 * <p>Original: {@code thaumcraft.common.lib.world.dim.GenNestRoom.generateRoom}
 * (reference {@code GenNestRoom.java:10-131}), the {@code GenMaze} case 7
 * (reference {@code MazeGenerator.java:245}). Loop bounds, the crust-code
 * placements, the two ceiling rolls, the two stacked platform blobs, the
 * connection depth and the 11x11 loot sweep are verbatim from that method.</p>
 *
 * <p>Feature 7 is also the code dispatcher's crab-spawner magnet: {@code
 * GenCommon.placeBlock} case {@code CRUST} queues a spawner for every 1-in-25
 * crust placement unconditionally while {@code cell.feature == 7} (reference
 * {@code GenCommon.java:243-250}), and {@code GenCommon.processDecorations}
 * turns the queued positions that pass its exposure gate into
 * {@code blockEldritch} meta 9 tiles (reference {@code GenCommon.java:303-318})
 * — both shipped in the parent slice, so the nest room gets them for free.</p>
 */
public final class GenNestRoom {

    private GenNestRoom() {
    }

    /**
     * Build the nest room in one 16x16 maze cell.
     *
     * @param level the maze level writer (original {@code World}, ref L10)
     * @param random the maze random — the wall-buttress roll, the two ceiling
     *               rolls per column, the platform blob rolls and the loot
     *               sweep all draw from it
     * @param cx chunk X of the cell (ref L11: {@code cx * 16})
     * @param cz chunk Z of the cell (ref L12)
     * @param y room floor Y (the caller's 50, {@code OuterLands.ROOM_Y})
     * @param cell the maze cell; {@code north/south/east/west} open the wall
     *             ring and the connections, {@code feature} tags the blocks
     *             and drives the crab-spawner queue
     */
    public static void generateRoom(final LevelWriter level, final Random random, final int cx, final int cz,
                                    final int y, final Cell cell) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f-m2] GenNestRoom needs a ServerLevel (the air checks, the "
                    + "crystal tile and the loot sets); the maze is a server-side path, nest room at chunk ("
                    + cx + ", " + cz + ") is left unbuilt");
            return;
        }

        final int x = cx * 16; // ref L11
        final int z = cz * 16; // ref L12

        // Ref L14-22: the shell — PAT_WALL, the full 11-deep column.
        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 11; c++) {
                    if (a == 1 || a == 15 || b == 1 || b == 15) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), 1, cell); // ref L18
                    }
                }
            }
        }

        // Ref L24-36: the void lining, with the door gaps on open sides.
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                for (int cxx = 1; cxx < 10; cxx++) {
                    if ((a == 2 || a == 14 || b == 2 || b == 14)
                            && (a != 2 || b <= 3 || b >= 12 || !cell.west || cxx >= 10)
                            && (a != 14 || b <= 3 || b >= 12 || !cell.east || cxx >= 10)
                            && (b != 2 || a <= 3 || a >= 12 || !cell.north || cxx >= 10)
                            && (b != 14 || a <= 3 || a >= 12 || !cell.south || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), 8, cell); // ref L32
                    }
                }
            }
        }

        // Ref L38-50: the crust lining, plus a crust buttress on an unlit side.
        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int cxxx = 2; cxxx < 9; cxxx++) {
                    if (a == 3 || a == 13 || b == 3 || b == 13) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), 21, cell); // ref L42
                    }
                    if ((a == 4 && !cell.west || a == 12 && !cell.east || b == 4 && !cell.north
                            || b == 12 && !cell.south) && random.nextBoolean()) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), 21, cell); // ref L46
                    }
                }
            }
        }

        // Ref L52-68: the floor and ceiling slabs. The ceiling draws its first
        // roll before the second, so a crystal only lands when the first roll
        // is false (ref L60-66); the crystal is a direct set (meta 7 = strange)
        // with its tile's orientation forced DOWN.
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), 1, cell);  // ref L54
                placeBlock(world, new BlockPos(x + a, y, z + b), 8, cell);      // ref L55
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), 21, cell); // ref L56
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), 1, cell); // ref L57
                placeBlock(world, new BlockPos(x + a, y + 10, z + b), 8, cell); // ref L58
                placeBlock(world, new BlockPos(x + a, y + 9, z + b), 21, cell); // ref L59
                if (random.nextBoolean()) {
                    placeBlock(world, new BlockPos(x + a, y + 8, z + b), 21, cell); // ref L61
                } else if (random.nextBoolean() && world.isEmptyBlock(new BlockPos(x + a, y + 8, z + b))) { // ref L62
                    final BlockPos crystalPos = new BlockPos(x + a, y + 8, z + b);
                    setBlockAndUpdate(crystalPos, CatalogDecorativeBlocks.BLOCK_CRYSTAL.get(),
                            world, BlockCrystal.VARIANT_STRANGE); // ref L63: blockCrystal meta 7
                    if (world.getBlockEntity(crystalPos) instanceof TileCrystal crystal) { // ref L64-65
                        crystal.setOrientation(Direction.DOWN); // orientation := DOWN ordinal
                    }
                }
            }
        }

        // Ref L70-95: the lower platform blob (crust, code 21), with the
        // optional side arms and the crust block (code 7) over the centre.
        placeBlock(world, new BlockPos(x + 8, y + 2, z + 8), 21, cell); // ref L70
        placeBlock(world, new BlockPos(x + 8, y + 3, z + 8), 21, cell); // ref L71
        placeBlock(world, new BlockPos(x + 8, y + 4, z + 8), 21, cell); // ref L72
        placeBlock(world, new BlockPos(x + 7, y + 2, z + 8), 21, cell); // ref L73
        placeBlock(world, new BlockPos(x + 8, y + 2, z + 7), 21, cell); // ref L74
        placeBlock(world, new BlockPos(x + 9, y + 2, z + 8), 21, cell); // ref L75
        placeBlock(world, new BlockPos(x + 8, y + 2, z + 9), 21, cell); // ref L76
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 7, y + 3, z + 8), 21, cell); // ref L78
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 8, y + 3, z + 7), 21, cell); // ref L82
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 9, y + 3, z + 8), 21, cell); // ref L86
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 8, y + 3, z + 9), 21, cell); // ref L90
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 8, y + 5, z + 8), 7, cell); // ref L94: blockEldritch meta 4
        }

        // Ref L97-118: the upper platform blob, the mirror of the lower one.
        placeBlock(world, new BlockPos(x + 8, y + 8, z + 8), 21, cell); // ref L97
        placeBlock(world, new BlockPos(x + 8, y + 7, z + 8), 21, cell); // ref L98
        placeBlock(world, new BlockPos(x + 8, y + 6, z + 8), 21, cell); // ref L99
        placeBlock(world, new BlockPos(x + 7, y + 8, z + 8), 21, cell); // ref L100
        placeBlock(world, new BlockPos(x + 8, y + 8, z + 7), 21, cell); // ref L101
        placeBlock(world, new BlockPos(x + 9, y + 8, z + 8), 21, cell); // ref L102
        placeBlock(world, new BlockPos(x + 8, y + 8, z + 9), 21, cell); // ref L103
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 7, y + 7, z + 8), 21, cell); // ref L105
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 8, y + 7, z + 7), 21, cell); // ref L109
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 9, y + 7, z + 8), 21, cell); // ref L113
        }
        if (random.nextBoolean()) {
            placeBlock(world, new BlockPos(x + 8, y + 7, z + 9), 21, cell); // ref L117
        }

        // Ref L120: the tunnel stubs, depth 3, sealed-end form.
        GenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);

        // Ref L122-130: the loot sweep. 15% of the 11x11 ring around the
        // platform centre, on an air cell, carries a container: the rarity is
        // the rr roll (rare < 0.15, normal < 0.4, plain) and the crate/urn
        // choice is a further 20% roll. Both are direct sets — the sweep is
        // not queued through the decoration lists.
        for (int a = -5; a <= 5; a++) {
            for (int bx = -5; bx <= 5; bx++) {
                if (random.nextFloat() < 0.15F
                        && world.isEmptyBlock(new BlockPos(x + 8 + a, y + 2, z + 8 + bx))) { // ref L124
                    final float rr = random.nextFloat(); // ref L125
                    final int md = rr < 0.15F ? 2 : (rr < 0.4F ? 1 : 0); // ref L126
                    final Block loot = random.nextFloat() < 0.2F // ref L127: blockLootCrate : blockLootUrn
                            ? CatalogTrinketsLoot.BLOCK_LOOT_CRATE.get()
                            : CatalogTrinketsLoot.BLOCK_LOOT_URN.get();
                    setBlockAndUpdate(new BlockPos(x + 8 + a, y + 2, z + 8 + bx), loot, world, md); // ref L127
                }
            }
        }
    }

    /**
     * The {@code LevelWriter} resolved to a {@code ServerLevel}, else null —
     * the maze call site ({@code MazeHandler.generateEldritch}) always
     * resolves (see {@code GenPortal.serverLevel}).
     */
    private static ServerLevel serverLevel(final LevelWriter level) {
        return level instanceof ServerLevel server ? server : null;
    }
}
