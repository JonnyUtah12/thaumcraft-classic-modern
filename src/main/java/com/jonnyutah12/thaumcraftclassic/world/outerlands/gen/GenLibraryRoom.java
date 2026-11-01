package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticSolid;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStoneSlab;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon.placeBlock;
import static com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon.setBlockAndUpdate;

/**
 * The TC maze library room — {@code MazeGenerator.FEATURE_LIBRARY} (8), the
 * tall room whose four corner stacks and centre stack carry the glyphed
 * eldritch blockwork (content-mapping.csv row
 * {@code thaumcraft:worldgen.outer_lands_procedural_maze}).
 *
 * <p>Original: {@code thaumcraft.common.lib.world.dim.GenLibraryRoom.generateRoom}
 * (reference {@code GenLibraryRoom.java:9-103}), the {@code GenMaze} case 8
 * (reference {@code MazeGenerator.java:248}). The 13-deep shell, the void
 * lining with its door gaps, the ancient-stone lining, the four corner pads,
 * the twenty stair runs of code 10/11 around the walls, the eight stack
 * columns and the connection depth are verbatim from that method.</p>
 *
 * <p>The stacks are direct sets (no decoration queue): {@code
 * blockCosmeticSolid} meta 15 (the ancient pedestal), {@code blockEldritch}
 * meta 5 (glyphed) and {@code blockSlabStone} metas 1 / 9 (the eldritch slab
 * and its double form) — see {@link #setSlabStone} for the meta mapping.</p>
 */
public final class GenLibraryRoom {

    private GenLibraryRoom() {
    }

    /**
     * Build the library room in one 16x16 maze cell.
     *
     * @param level the maze level writer (original {@code World}, ref L9)
     * @param random the maze random; this room draws only for the connection
     *               stubs (ref L102), so its layout is seed-independent
     * @param cx chunk X of the cell (ref L10: {@code cx * 16})
     * @param cz chunk Z of the cell (ref L11)
     * @param y room floor Y (the caller's 50, {@code OuterLands.ROOM_Y})
     * @param cell the maze cell; {@code north/south/east/west} open the wall
     *             ring and the connections, {@code feature} tags the blocks
     */
    public static void generateRoom(final LevelWriter level, final Random random, final int cx, final int cz,
                                    final int y, final Cell cell) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f-m2] GenLibraryRoom needs a ServerLevel (the maze block "
                    + "setting); the maze is a server-side path, library room at chunk (" + cx + ", " + cz
                    + ") is left unbuilt");
            return;
        }

        final int x = cx * 16; // ref L10
        final int z = cz * 16; // ref L11

        // Ref L13-21: the shell — PAT_WALL, 13 deep (the tallest maze room).
        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 1 || a == 15 || b == 1 || b == 15) {
                        placeBlock(world, new BlockPos(x + a, y + c, z + b), 1, cell); // ref L17
                    }
                }
            }
        }

        // Ref L23-35: the void lining with the door gaps on open sides.
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                for (int cxx = 1; cxx < 12; cxx++) {
                    if ((a == 2 || a == 14 || b == 2 || b == 14)
                            && (a != 2 || b <= 3 || b >= 12 || !cell.west || cxx >= 10)
                            && (a != 14 || b <= 3 || b >= 12 || !cell.east || cxx >= 10)
                            && (b != 2 || a <= 3 || a >= 12 || !cell.north || cxx >= 10)
                            && (b != 14 || a <= 3 || a >= 12 || !cell.south || cxx >= 10)) {
                        placeBlock(world, new BlockPos(x + a, y + cxx, z + b), 8, cell); // ref L31
                    }
                }
            }
        }

        // Ref L37-45: the ancient-stone lining (code 2).
        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int cxxx = 2; cxxx < 11; cxxx++) {
                    if (a == 3 || a == 13 || b == 3 || b == 13) {
                        placeBlock(world, new BlockPos(x + a, y + cxxx, z + b), 2, cell); // ref L41
                    }
                }
            }
        }

        // Ref L47-67: the floor/ceiling slabs and the four corner pads.
        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                placeBlock(world, new BlockPos(x + a, y - 1, z + b), 1, cell);  // ref L49
                placeBlock(world, new BlockPos(x + a, y, z + b), 8, cell);      // ref L50
                placeBlock(world, new BlockPos(x + a, y + 1, z + b), 2, cell);  // ref L51
                placeBlock(world, new BlockPos(x + a, y + 12, z + b), 1, cell); // ref L52
                placeBlock(world, new BlockPos(x + a, y + 11, z + b), 8, cell); // ref L53
                placeBlock(world, new BlockPos(x + a, y + 10, z + b), 2, cell); // ref L54
                if (a > 3 && a < 13 && b > 3 && b < 13) {
                    if (a <= 5 && b <= 5 || a <= 5 && b >= 11 || a >= 11 && b <= 5 || a >= 11 && b >= 11) {
                        placeBlock(world, new BlockPos(x + a, y + 2, z + b), 2, cell); // ref L57
                        placeBlock(world, new BlockPos(x + a, y + 9, z + b), 2, cell); // ref L58
                    }
                    if (a == 5 && b == 5 || a == 5 && b == 11 || a == 11 && b == 5 || a == 11 && b == 11) {
                        setBlockAndUpdate(new BlockPos(x + a, y + 3, z + b),
                                CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(), world,
                                BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL); // ref L62: meta 15
                        setBlockAndUpdate(new BlockPos(x + a, y + 8, z + b),
                                CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(), world,
                                BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL); // ref L63: meta 15
                    }
                }
            }
        }

        // Ref L69-78: the twenty stair runs hugging the walls, lower row code
        // 10 (STAIR_DIRECTIONAL) at y+2, upper row code 11 (inverted) at y+9.
        for (int g = 0; g < 5; g++) {
            placeBlock(world, new BlockPos(x + 6 + g, y + 2, z + 4), 10, Direction.NORTH, cell);  // ref L70
            placeBlock(world, new BlockPos(x + 6 + g, y + 2, z + 12), 10, Direction.SOUTH, cell); // ref L71
            placeBlock(world, new BlockPos(x + 12, y + 2, z + 6 + g), 10, Direction.EAST, cell);  // ref L72
            placeBlock(world, new BlockPos(x + 4, y + 2, z + 6 + g), 10, Direction.WEST, cell);   // ref L73
            placeBlock(world, new BlockPos(x + 6 + g, y + 9, z + 4), 11, Direction.NORTH, cell);  // ref L74
            placeBlock(world, new BlockPos(x + 6 + g, y + 9, z + 12), 11, Direction.SOUTH, cell); // ref L75
            placeBlock(world, new BlockPos(x + 12, y + 9, z + 6 + g), 11, Direction.EAST, cell);  // ref L76
            placeBlock(world, new BlockPos(x + 4, y + 9, z + 6 + g), 11, Direction.WEST, cell);   // ref L77
        }

        // Ref L80-101: the eight stacks. Each is a glyphed eldritch block
        // (meta 5), a slab (meta 1) above it, and — for the lower pair of
        // every column — a glyphed block with a double slab (meta 9) under it.
        for (final int[] stack : STACKS) { // ref L80-95, the eight (x, z, y) triples
            setBlockAndUpdate(new BlockPos(x + stack[0], y + stack[1], z + stack[2]),
                    CatalogEldritchRealm.BLOCK_ELDRITCH.get(), world, EldritchRealmValues.ELDRITCH_GLYPHED);
            setSlabStone(new BlockPos(x + stack[0], y + stack[3], z + stack[2]), world, stack[4]);
        }

        // Ref L96-101: the centre stack.
        setBlockAndUpdate(new BlockPos(x + 8, y + 2, z + 8), CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(),
                world, BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL); // ref L96: meta 15
        setBlockAndUpdate(new BlockPos(x + 8, y + 3, z + 8), CatalogEldritchRealm.BLOCK_ELDRITCH.get(), world,
                EldritchRealmValues.ELDRITCH_GLYPHED); // ref L97: meta 5
        setSlabStone(new BlockPos(x + 8, y + 4, z + 8), world, 1); // ref L98: blockSlabStone meta 1
        setBlockAndUpdate(new BlockPos(x + 8, y + 9, z + 8), CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(),
                world, BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL); // ref L99: meta 15
        setBlockAndUpdate(new BlockPos(x + 8, y + 8, z + 8), CatalogEldritchRealm.BLOCK_ELDRITCH.get(), world,
                EldritchRealmValues.ELDRITCH_GLYPHED); // ref L100: meta 5
        setSlabStone(new BlockPos(x + 8, y + 7, z + 8), world, 9); // ref L101: meta 9

        // Ref L102: the tunnel stubs, depth 3, sealed-end form.
        GenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);
    }

    /**
     * The eight corner stacks of ref L80-95 as {@code (x, y(glyphed), z,
     * y(slab), slabMeta)}: the upper four at y+3/y+5 with slab meta 1, the
     * lower four at y+7/y+6 with slab meta 9 (the double slab, below the
     * glyphed block).
     */
    private static final int[][] STACKS = {
            {5, 4, 5, 5, 1},   // ref L80-81
            {5, 4, 11, 5, 1},  // ref L82-83
            {11, 4, 5, 5, 1},  // ref L84-85
            {11, 4, 11, 5, 1}, // ref L86-87
            {5, 7, 5, 6, 9},   // ref L88-89
            {5, 7, 11, 6, 9},  // ref L90-91
            {11, 7, 5, 6, 9},  // ref L92-93
            {11, 7, 11, 6, 9}, // ref L94-95
    };

    /**
     * The original's {@code blockSlabStone} meta on a set: 1.7.10
     * {@code BlockSlab} packs the type in {@code meta & 7} (the reference's own
     * {@code BlockCosmeticStoneSlab} reads {@code (meta & 7) == 0 ? arcane :
     * eldritch}, reference {@code BlockCosmeticStoneSlab.java:26-29}, and drops
     * {@code meta & 7}) and the full-block bit in {@code meta & 8}. 1.21.1 has
     * one slab class carrying both forms ({@code variant} for the type, vanilla
     * {@code type} for the double), and the legacy double id is registered as a
     * second instance (TC-M6-04b), so the port maps meta 1 to the half slab
     * with the eldritch variant and meta 9 to the double-slab id — the pair the
     * original's meta 1 / 9 render as.
     *
     * <p>{@code GenCommon.stateFor} cannot express meta 9: its slab branch
     * writes the {@code variant} property, whose domain is 0..1 (the type), and
     * falls to the default state for out-of-domain metas. That is why this
     * builder maps the slab metas itself.</p>
     */
    private static void setSlabStone(final BlockPos pos, final ServerLevel world, final int meta) {
        final int type = meta & 7; // the arcane/eldritch pair value
        final boolean full = (meta & 8) != 0;
        final BlockState state = (full
                ? CatalogDecorativeBlocks.BLOCK_COSMETIC_DOUBLE_SLAB_STONE.get().defaultBlockState()
                : CatalogDecorativeBlocks.BLOCK_COSMETIC_SLAB_STONE.get().defaultBlockState())
                .setValue(BlockCosmeticStoneSlab.VARIANT,
                        BlockCosmeticStoneSlab.VARIANT.getPossibleValues().contains(type) ? type
                                : BlockCosmeticStoneSlab.VARIANT_ARCANE)
                .setValue(SlabBlock.TYPE, full ? SlabType.DOUBLE : SlabType.BOTTOM);
        world.setBlock(pos, state, 3); // the original's setBlock flags 3
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
