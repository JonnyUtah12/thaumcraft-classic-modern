package com.jonnyutah12.thaumcraftclassic.world.outerlands.gen;

import java.util.Random;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * The passage room of the Outer Lands maze — a verbatim port of the original
 * {@code thaumcraft.common.lib.world.dim.GenPassage}, reference
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/lib/world/dim/GenPassage.java}
 * (278 lines), cited below as "ref". Extends {@link GenCommon} exactly as the
 * original extends its GenCommon (ref L13).
 *
 * <p><b>Numbers verbatim.</b> The chunk origins {@code x = cx*16}/{@code z =
 * cz*16} (ref L15-16); the connection pass at {@code depth = 4} with
 * {@code justthetip = false} (ref L17); the four-way {@code mod} gate
 * (ref L18-21) and its two derived loop bounds {@code w,h in 2+mod .. 8-mod}
 * (ref L41-42, L83-84, L125-126, L167-168); the 7x7 core
 * {@code w,h in 1..7} at {@code x+4+w}/{@code z+4+h} with the floor/ceiling
 * pair at {@code y+2}/{@code y+8}, {@code y}/{@code y+10} and
 * {@code y+1}/{@code y+9} (ref L23-38); the glyphed-stone roll
 * {@code cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2} at every one
 * of its six call sites (ref L29, L30, L61, L103, L145, L187); the four wall
 * planes with the 11x11 {@code PAT_CONNECT[h][w]} pattern
 * (ref L43, L85, L127, L169 — the pattern table itself is ref GenCommon
 * L35-47) at {@code z+5}/{@code z+11}/{@code x+11}/{@code x+5} and elevation
 * {@code y+10-h}; the sealed-wall fallbacks at {@code z+4/z+3},
 * {@code z+12/z+13}, {@code x+12/x+13}, {@code x+4/x+3} with their
 * {@code h == 7}/{@code w == 7} caps (ref L59-79, L101-121, L143-163,
 * L185-205); the mid-door pairs at {@code y+3}/{@code y+7} with codes
 * 3/4/5/6 (ref L49-55, L91-97, L133-139, L175-181, L209-224); the crust sweep
 * {@code w,h,j in -4..4} at {@code y+4+h} (ref L227-242); the taint sweep
 * {@code w in -4..4, h in -3..3, jx in -4..4} (ref L244-258); and the spawner
 * sweep {@code w,h,jxx in -3..3} with the 0.35F web roll (ref L260-276).</p>
 *
 * <p><b>Random calls, in order.</b> The passage is the room that consumes the
 * chunk {@code random}: the four-way {@code nextBoolean} of the {@code mod}
 * gate (ref L19), then the glyphed-stone {@code nextInt(3)} rolls in wall
 * order — core, north, south, east, west (ref L29/30, L61, L103, L145, L187) —
 * then the crust {@code nextBoolean} per candidate cell (ref L236), the taint
 * {@code nextInt(3)} and {@code nextInt(4)} (ref L249-250) and the web
 * {@code nextFloat() < 0.35F} (ref L264). Every Java short-circuit of the
 * original is kept in the same operand order, so a skipped draw stays skipped
 * and the stream matches. The {@code placeBlock} codes draw the
 * <i>world</i> random (ref GenCommon L68-87, L240-246), not this one.</p>
 *
 * <h2>1.21.1 shape</h2>
 * <ul>
 *   <li><b>Entry contract.</b> The shipped call contract fixes the entry as
 *   {@code generatePassage(LevelWriter, Random, int i, int j, int k, int y,
 *   Cell)}. The reference entry is the six-parameter
 *   {@code generateDefaultPassage(world, random, cx, cz, y, cell)} (ref L14,
 *   called as {@code (world, random, cx, cz, y, cell)} from MazeHandler ref
 *   L178, behind the {@code random.nextInt(1)} switch at ref L176 — a
 *   single-arm switch that always picks the default passage). The reference
 *   body has no third chunk coordinate, so {@code k} has no consumer and is
 *   dropped at the seam; the original's own name and arity ship as
 *   {@link #generateDefaultPassage}, which is what the shipped passage slice
 *   of {@code MazeHandler} will call.</li>
 *   <li><b>Meta codes.</b> The passage is the heaviest user of the
 *   {@code placeBlock} code table (ref GenCommon L55-259): 1 = bedrock,
 *   2 = blockCosmeticSolid meta 11 (ancient stone), 3/4/5/6 =
 *   blockStairsEldritch with metas from the direction's <b>ForgeDirection
 *   ordinal</b> (ref GenCommon L86-145: ordinals 2/3 → 1/0/5/4 and 4/5 →
 *   3/2/7/6), 7 = blockEldritch meta 4, 8 = blockEldritchNothing, 9 = air +
 *   de-queue, 20 = blockEldritch meta 10 (the trap, the glyphed-stone roll),
 *   21 = crust (the code places nothing; its whole effect is
 *   {@code GenCommon}'s tail, ref L229-251). The codes pass through verbatim;
 *   the toolbox owns the table, the {@code blockEldritchNothing} gates and the
 *   decoration/crab-spawner queues.</li>
 *   <li><b>Direction ordinals (parity trap).</b> Codes 3-6 switch on
 *   {@code dir.ordinal()} with the 1.7.10 {@code ForgeDirection} encoding
 *   (UNKNOWN 0, DOWN 1, UP 2, NORTH 3, SOUTH 4, WEST 5, EAST 6); 1.21.1
 *   {@code Direction} orders DOWN 0, UP 1, NORTH 2, SOUTH 3, WEST 4, EAST 5,
 *   so a naive {@code Direction.ordinal()} shifts every stair meta by one. The
 *   toolbox must map through {@code OuterLands.forgeOrdinal} (that method is
 *   exactly this encoding); the direction <i>name</i> is what is handed over
 *   here, as in {@code GenPortal}.</li>
 *   <li><b>Unregistered TC blocks.</b> {@code blockTaintFibres}
 *   (content-mapping.csv row 77, {@code thaumcraft:blocktaintfibres}) and the
 *   Mind Spider (row 322, {@code thaumcraft:mindspider}) have no shipped
 *   catalog row. They are resolved through the registry at generation time
 *   (the {@code worldgen/StructureGen.tcBlockState} seam, package-private
 *   there, mirrored locally): the placement is skipped and the spawner left
 *   empty, while the random draws are taken unconditionally first so the RNG
 *   stream stays identical.</li>
 *   <li><b>Biome write.</b> {@code Utils.setBiomeAt(world, x, z, biomeTaint)}
 *   (ref L253) writes the chunk's 256-entry column biome array; 1.21.1 stores
 *   biomes in a 4x4x4 palette per section, so the write is the shipped
 *   palette-write pattern (as in {@code world/NodeTickDriver}), keyed by
 *   {@code BiomeAura.firstTaintBiome()} — the shipped taint-biome seam. No
 *   taint biome is registered yet, so the write is a no-op until its card
 *   lands; the original's {@code PacketBiomeChange} has no 1.21.1 caller
 *   (the chunk is marked unsaved, which is what the palette write needs).</li>
 *   <li><b>No Gen2x2.</b> The passage builds itself, as the original does: the
 *   original {@code GenPassage} never calls {@code Gen2x2}, and neither does
 *   this port. There is no tile-entity, {@code setDelay} or
 *   {@code markTileEntityForRenderUpdate} call anywhere in this file, matching
 *   the reference (its only tile touch is the mob spawner at ref L269-272),
 *   and no {@code MathHelper.floor_double} and no
 *   {@code Config.dimensionOuterId} read, so no {@code Mth.floor} and no
 *   {@code OuterLands.isOuterLands} are needed.</li>
 *   <li><b>Public entry points, and the wiring gap.</b> The 1.7.10 maze picks a
 *   room builder per {@code cell.feature}; features 9-14 and the default branch
 *   fall through to a private {@code generatePassage(world, random, cx, cz, 50,
 *   cell)} wrapper (reference MazeHandler L168) whose body is
 *   {@code switch (random.nextInt(1)) case 0: GenPassage.generateDefaultPassage(
 *   world, random, cx, cz, y, cell)} (reference MazeHandler L175-180) — a
 *   one-case switch, so the default passage always runs, and that is the maze's
 *   only use of this class. {@link #generateDefaultPassage} is that entry (it
 *   stamps {@code cell.feature = 13} as the original does) and
 *   {@link #generatePassage} is the per-room builder it calls. The shipped
 *   {@code MazeHandler.generateEldritch} (TC-M6-04f-2) wires
 *   {@code GenPortal.generatePortal}, {@code GenBossRoom.generateRoom} and
 *   {@code GenCommon.processDecorations} only — no passage branch — and the
 *   original's {@code GenCommon.generateConnections} carves connections without
 *   building these rooms, so the landing of the maze handler (or the toolbox)
 *   must route features 9-14 + default to {@code generateDefaultPassage}.
 *   Nothing else in the repo calls this class today.</li>
 * </ul>
 */
public class GenPassage extends GenCommon {

    /** The original's {@code ConfigBlocks.blockTaintFibres} (content-mapping.csv row 77). */
    private static final ResourceLocation TAINT_FIBRES =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blocktaintfibres");

    /** The original's spawner id {@code "Thaumcraft.MindSpider"} (content-mapping.csv row 322). */
    private static final ResourceLocation MIND_SPIDER =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "mindspider");

    /**
     * The shipped 7-argument seam. {@code i}/{@code j} are the cell's chunk
     * coordinates (original {@code cx}/{@code cz}, ref L14); {@code k} has no
     * consumer in the reference body and is dropped at the seam.
     */
    public static void generatePassage(final LevelWriter level, final Random random, final int i, final int j,
                                       final int k, final int y, final Cell cell) {
        generateDefaultPassage(level, random, i, j, y, cell);
    }

    /**
     * The passage room: connections, the 7x7 core, the four wall planes (open
     * via {@code PAT_CONNECT} or sealed), the four mid-door pairs, the centre
     * cross when all four sides are open, and the wall features — 12 crust,
     * 13 taint, 14 spawner.
     *
     * @param level the maze level writer (original {@code World}, ref L14)
     * @param random the maze random; the passage is its heavy consumer (see the class javadoc)
     * @param cx chunk X of the cell (ref L15)
     * @param cz chunk Z of the cell (ref L16)
     * @param y room floor Y (the caller's 50, MazeHandler ref L168/175)
     * @param cell the maze cell; {@code north/south/east/west} pick the wall
     *             planes, {@code feature} the rolls and sweeps (11 glyphed,
     *             12 crust, 13 taint, 14 spawner)
     */
    public static void generateDefaultPassage(final LevelWriter level, final Random random, final int cx,
                                              final int cz, final int y, final Cell cell) {
        final ServerLevel world = serverLevel(level);
        if (world == null) {
            ThaumcraftClassic.LOGGER.warn("[TC-M6-04f] GenPassage needs a ServerLevel (world reads, the world "
                    + "random, the biome palette and the spawner tile); the maze is a server-side path, "
                    + "passage at chunk (" + cx + ", " + cz + ") is left unbuilt");
            return;
        }

        final int x = cx * 16; // ref L15
        final int z = cz * 16; // ref L16

        // Ref L17: the connection stubs, depth 4, the whole tunnel.
        GenCommon.generateConnections(world, random, cx, cz, y, cell, 4, false);

        int mod = 0; // ref L18
        if (cell.north && cell.south && cell.west && cell.east && random.nextBoolean()) { // ref L19-21
            mod = 1;
        }

        // Ref L23-38: the 7x7 core — floor and ceiling, with the centre cell
        // open when the passage is a four-way cross.
        for (int w = 1; w < 8; w++) {
            for (int h = 1; h < 8; h++) {
                if (w == 4 && h == 4 && mod == 1) { // ref L25-27
                    place(world, new BlockPos(x + 4 + w, y + 2, z + 4 + h), 7, cell);
                    place(world, new BlockPos(x + 4 + w, y + 8, z + 4 + h), 7, cell);
                } else { // ref L29-30
                    place(world, new BlockPos(x + 4 + w, y + 2, z + 4 + h),
                            cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                    place(world, new BlockPos(x + 4 + w, y + 8, z + 4 + h),
                            cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                }

                place(world, new BlockPos(x + 4 + w, y, z + 4 + h), 1, cell);        // ref L33
                place(world, new BlockPos(x + 4 + w, y + 10, z + 4 + h), 1, cell);   // ref L34
                place(world, new BlockPos(x + 4 + w, y + 1, z + 4 + h), 8, cell);    // ref L35
                place(world, new BlockPos(x + 4 + w, y + 9, z + 4 + h), 8, cell);    // ref L36
            }
        }

        // Ref L40-80: the north wall, opened on the PAT_CONNECT pattern or
        // sealed with its own three-layer band.
        if (cell.north) {
            for (int w = 2 + mod; w < 9 - mod; w++) {
                for (int h = 2 + mod; h < 9 - mod; h++) {
                    place(world, new BlockPos(x + 3 + w, y + 10 - h, z + 5),
                            GenCommon.PAT_CONNECT[h][w], Direction.NORTH, cell);
                }
            }

            if (mod == 0) {
                if (cell.west) { // ref L48-51
                    place(world, new BlockPos(x + 6, y + 3, z + 6), 3, Direction.EAST, cell);
                    place(world, new BlockPos(x + 6, y + 7, z + 6), 5, Direction.EAST, cell);
                }

                if (cell.east) { // ref L53-56
                    place(world, new BlockPos(x + 10, y + 3, z + 6), 3, Direction.EAST, cell);
                    place(world, new BlockPos(x + 10, y + 7, z + 6), 5, Direction.EAST, cell);
                }
            }
        } else {
            for (int w = 1; w < 8; w++) {
                for (int h = 1; h < 8; h++) {
                    place(world, new BlockPos(x + 4 + w, y + 9 - h, z + 5),
                            cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell); // ref L61
                    place(world, new BlockPos(x + 4 + w, y + 9 - h, z + 4), 8, cell); // ref L62
                    place(world, new BlockPos(x + 4 + w, y + 9 - h, z + 3), 1, cell); // ref L63

                    if (h == 7) { // ref L64-67
                        place(world, new BlockPos(x + 4 + w, y + 1, z + 4), 1, cell);
                        place(world, new BlockPos(x + 4 + w, y + 9, z + 4), 1, cell);
                    }

                    if (w == 7) { // ref L69-72
                        place(world, new BlockPos(x + 4, y + 9 - h, z + 4), 1, cell);
                        place(world, new BlockPos(x + 12, y + 9 - h, z + 4), 1, cell);
                    }
                }
            }

            for (int w = 2; w < 7; w++) { // ref L76-79
                place(world, new BlockPos(x + 4 + w, y + 3, z + 6), 3, Direction.EAST, cell);
                place(world, new BlockPos(x + 4 + w, y + 7, z + 6), 5, Direction.EAST, cell);
            }
        }

        // Ref L82-122: the south wall.
        if (cell.south) {
            for (int w = 2 + mod; w < 9 - mod; w++) {
                for (int h = 2 + mod; h < 9 - mod; h++) {
                    place(world, new BlockPos(x + 3 + w, y + 10 - h, z + 11),
                            GenCommon.PAT_CONNECT[h][w], Direction.SOUTH, cell);
                }
            }

            if (mod == 0) {
                if (cell.west) { // ref L90-92
                    place(world, new BlockPos(x + 6, y + 3, z + 10), 4, Direction.EAST, cell);
                    place(world, new BlockPos(x + 6, y + 7, z + 10), 6, Direction.EAST, cell);
                }

                if (cell.east) { // ref L95-98
                    place(world, new BlockPos(x + 10, y + 3, z + 10), 4, Direction.EAST, cell);
                    place(world, new BlockPos(x + 10, y + 7, z + 10), 6, Direction.EAST, cell);
                }
            }
        } else {
            for (int w = 1; w < 8; w++) {
                for (int h = 1; h < 8; h++) {
                    place(world, new BlockPos(x + 4 + w, y + 9 - h, z + 11),
                            cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell); // ref L103
                    place(world, new BlockPos(x + 4 + w, y + 9 - h, z + 12), 8, cell); // ref L104
                    place(world, new BlockPos(x + 4 + w, y + 9 - h, z + 13), 1, cell); // ref L105

                    if (h == 7) { // ref L106-109
                        place(world, new BlockPos(x + 4 + w, y + 1, z + 12), 1, cell);
                        place(world, new BlockPos(x + 4 + w, y + 9, z + 12), 1, cell);
                    }

                    if (w == 7) { // ref L111-114
                        place(world, new BlockPos(x + 4, y + 9 - h, z + 12), 1, cell);
                        place(world, new BlockPos(x + 12, y + 9 - h, z + 12), 1, cell);
                    }
                }
            }

            for (int w = 2; w < 7; w++) { // ref L118-121
                place(world, new BlockPos(x + 4 + w, y + 3, z + 10), 4, Direction.EAST, cell);
                place(world, new BlockPos(x + 4 + w, y + 7, z + 10), 6, Direction.EAST, cell);
            }
        }

        // Ref L124-164: the east wall.
        if (cell.east) {
            for (int w = 2 + mod; w < 9 - mod; w++) {
                for (int h = 2 + mod; h < 9 - mod; h++) {
                    place(world, new BlockPos(x + 11, y + 10 - h, z + 3 + w),
                            GenCommon.PAT_CONNECT[h][w], Direction.EAST, cell);
                }
            }

            if (mod == 0) {
                if (cell.north) { // ref L132-134
                    place(world, new BlockPos(x + 10, y + 3, z + 6), 4, Direction.NORTH, cell);
                    place(world, new BlockPos(x + 10, y + 7, z + 6), 6, Direction.NORTH, cell);
                }

                if (cell.south) { // ref L137-140
                    place(world, new BlockPos(x + 10, y + 3, z + 10), 4, Direction.NORTH, cell);
                    place(world, new BlockPos(x + 10, y + 7, z + 10), 6, Direction.NORTH, cell);
                }
            }
        } else {
            for (int w = 1; w < 8; w++) {
                for (int h = 1; h < 8; h++) {
                    place(world, new BlockPos(x + 11, y + 9 - h, z + 4 + w),
                            cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell); // ref L145
                    place(world, new BlockPos(x + 12, y + 9 - h, z + 4 + w), 8, cell); // ref L146
                    place(world, new BlockPos(x + 13, y + 9 - h, z + 4 + w), 1, cell); // ref L147

                    if (h == 7) { // ref L148-151
                        place(world, new BlockPos(x + 12, y + 1, z + 4 + w), 1, cell);
                        place(world, new BlockPos(x + 12, y + 9, z + 4 + w), 1, cell);
                    }

                    if (w == 7) { // ref L153-156
                        place(world, new BlockPos(x + 12, y + 9 - h, z + 4), 1, cell);
                        place(world, new BlockPos(x + 12, y + 9 - h, z + 12), 1, cell);
                    }
                }
            }

            for (int w = 2; w < 7; w++) { // ref L160-163
                place(world, new BlockPos(x + 10, y + 3, z + 4 + w), 4, Direction.NORTH, cell);
                place(world, new BlockPos(x + 10, y + 7, z + 4 + w), 6, Direction.NORTH, cell);
            }
        }

        // Ref L166-206: the west wall.
        if (cell.west) {
            for (int w = 2 + mod; w < 9 - mod; w++) {
                for (int h = 2 + mod; h < 9 - mod; h++) {
                    place(world, new BlockPos(x + 5, y + 10 - h, z + 3 + w),
                            GenCommon.PAT_CONNECT[h][w], Direction.WEST, cell);
                }
            }

            if (mod == 0) {
                if (cell.north) { // ref L174-176
                    place(world, new BlockPos(x + 6, y + 3, z + 6), 3, Direction.NORTH, cell);
                    place(world, new BlockPos(x + 6, y + 7, z + 6), 5, Direction.NORTH, cell);
                }

                if (cell.south) { // ref L179-181
                    place(world, new BlockPos(x + 6, y + 3, z + 10), 3, Direction.NORTH, cell);
                    place(world, new BlockPos(x + 6, y + 7, z + 10), 5, Direction.NORTH, cell);
                }
            }
        } else {
            for (int w = 1; w < 8; w++) {
                for (int h = 1; h < 8; h++) {
                    place(world, new BlockPos(x + 5, y + 9 - h, z + 4 + w),
                            cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell); // ref L187
                    place(world, new BlockPos(x + 4, y + 9 - h, z + 4 + w), 8, cell); // ref L188
                    place(world, new BlockPos(x + 3, y + 9 - h, z + 4 + w), 1, cell); // ref L189

                    if (h == 7) { // ref L190-193
                        place(world, new BlockPos(x + 4, y + 1, z + 4 + w), 1, cell);
                        place(world, new BlockPos(x + 4, y + 9, z + 4 + w), 1, cell);
                    }

                    if (w == 7) { // ref L195-198
                        place(world, new BlockPos(x + 4, y + 9 - h, z + 4), 1, cell);
                        place(world, new BlockPos(x + 4, y + 9 - h, z + 12), 1, cell);
                    }
                }
            }

            for (int w = 2; w < 7; w++) { // ref L202-205
                place(world, new BlockPos(x + 6, y + 3, z + 4 + w), 3, Direction.NORTH, cell);
                place(world, new BlockPos(x + 6, y + 7, z + 4 + w), 5, Direction.NORTH, cell);
            }
        }

        // Ref L208-225: the four-way cross's corner doors.
        if (mod == 1) {
            place(world, new BlockPos(x + 5, y + 3, z + 5), 3, Direction.EAST, cell);    // ref L209
            place(world, new BlockPos(x + 5, y + 7, z + 5), 5, Direction.EAST, cell);    // ref L210
            place(world, new BlockPos(x + 5, y + 3, z + 6), 3, Direction.NORTH, cell);   // ref L211
            place(world, new BlockPos(x + 5, y + 7, z + 6), 5, Direction.NORTH, cell);   // ref L212
            place(world, new BlockPos(x + 11, y + 3, z + 5), 3, Direction.EAST, cell);   // ref L213
            place(world, new BlockPos(x + 11, y + 7, z + 5), 5, Direction.EAST, cell);   // ref L214
            place(world, new BlockPos(x + 11, y + 3, z + 6), 4, Direction.NORTH, cell);  // ref L215
            place(world, new BlockPos(x + 11, y + 7, z + 6), 6, Direction.NORTH, cell);  // ref L216
            place(world, new BlockPos(x + 5, y + 3, z + 11), 3, Direction.NORTH, cell);  // ref L217
            place(world, new BlockPos(x + 5, y + 7, z + 11), 5, Direction.NORTH, cell);  // ref L218
            place(world, new BlockPos(x + 6, y + 3, z + 11), 4, Direction.EAST, cell);   // ref L219
            place(world, new BlockPos(x + 6, y + 7, z + 11), 6, Direction.EAST, cell);   // ref L220
            place(world, new BlockPos(x + 11, y + 3, z + 11), 4, Direction.NORTH, cell); // ref L221
            place(world, new BlockPos(x + 11, y + 7, z + 11), 6, Direction.NORTH, cell); // ref L222
            place(world, new BlockPos(x + 10, y + 3, z + 11), 4, Direction.EAST, cell);  // ref L223
            place(world, new BlockPos(x + 10, y + 7, z + 11), 6, Direction.EAST, cell);  // ref L224
        }

        // Ref L227-242: feature 12, the ancient doorway — every air, cosmetic
        // solid or eldritch-stair cell in the 9x9x9 box crusts over on a coin
        // flip (the CRUST code's whole effect is GenCommon's tail, ref L237-251).
        if (cell.feature == 12) {
            for (int w = -4; w <= 4; w++) {
                for (int h = -4; h < 5; h++) {
                    for (int j = -4; j <= 4; j++) {
                        final BlockPos pos = new BlockPos(x + 8 + w, y + 4 + h, z + 8 + j);
                        final BlockState state = world.getBlockState(pos);
                        if ((state.isAir()
                                || state.is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get())
                                || state.is(CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get()))
                                && random.nextBoolean()) {
                            place(world, pos, 21, cell);
                        }
                    }
                }
            }
        }

        // Ref L244-258: feature 13, the ancient lock — taint fibres on the
        // exposed air cells and the taint biome stamped over the column.
        if (cell.feature == 13) {
            for (int w = -4; w <= 4; w++) {
                for (int h = -3; h <= 3; h++) {
                    for (int jx = -4; jx <= 4; jx++) {
                        final BlockPos pos = new BlockPos(x + 8 + w, y + 4 + h, z + 8 + jx);
                        if (world.getBlockState(pos).isAir() && isAdjacentToSolidBlock(world, pos)) {
                            if (random.nextInt(3) != 0) { // ref L249
                                // The meta draw (ref L250) is taken first: the
                                // taint-fibres block is not registered yet, and
                                // skipping the placement must not skip the roll.
                                final int meta = random.nextInt(4) == 0 ? 1 : 0;
                                final Block fibres = BuiltInRegistries.BLOCK.getOptional(TAINT_FIBRES).orElse(null);
                                if (fibres != null) {
                                    GenCommon.setBlock(pos, fibres, level, true, meta);
                                }
                            }

                            // Ref L253: Utils.setBiomeAt(world, x, z, biomeTaint).
                            setBiomeAt(world, pos.getX(), pos.getY(), pos.getZ(), BiomeAura.firstTaintBiome());
                        }
                    }
                }
            }
        }

        // Ref L260-276: feature 14, the mob spawner — cobweb in the air cells
        // of the 7x7x7 box, and the spawner at the centre. The spawner block
        // set and the tile touch sit inside the innermost loop in the
        // decompiled original (ref L268-272) and are kept there: they draw no
        // random, so the stream is untouched and the result is the same cell.
        if (cell.feature == 14) {
            for (int w = -3; w <= 3; w++) {
                for (int h = -3; h <= 3; h++) {
                    for (int jxx = -3; jxx <= 3; jxx++) {
                        final BlockPos pos = new BlockPos(x + 8 + w, y + 4 + h, z + 8 + jxx);
                        if (world.getBlockState(pos).isAir() && random.nextFloat() < 0.35F) {
                            GenCommon.setBlock(pos, Blocks.COBWEB, level, true, 0); // ref L265 Blocks.web
                        }

                        final BlockPos centre = new BlockPos(x + 8, y + 4, z + 8);
                        GenCommon.setBlock(centre, Blocks.SPAWNER, level, true, 0); // ref L268 Blocks.mob_spawner

                        final SpawnerBlockEntity spawner =
                                world.getBlockEntity(centre, BlockEntityType.MOB_SPAWNER).orElse(null); // ref L269
                        if (spawner != null) { // ref L270-272
                            // The original's setEntityId("Thaumcraft.MindSpider");
                            // the Mind Spider has no shipped entity row
                            // (content-mapping.csv row 322, not_started), so an
                            // unresolvable id leaves the spawner empty — the
                            // shipped behavior of the same seam in
                            // worldgen/StructureGen.setSpawnerEntity.
                            BuiltInRegistries.ENTITY_TYPE.getOptional(MIND_SPIDER)
                                    .ifPresent(id -> spawner.setEntityId(id, world.getRandom()));
                        }
                    }
                }
            }
        }
    }

    /**
     * The original's {@code placeBlock(world, x, y, z, code, cell)} (ref
     * GenCommon L49-51), kept as a code-dispatcher call so the code →
     * block/meta table, the {@code blockEldritchNothing} gates, the world
     * random draws and the decoration queues stay in {@link GenCommon}.
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
     * cell)} (ref GenCommon L55). The direction travels by name; the codes
     * that read it as an ordinal (3-6) need the ForgeDirection encoding on the
     * toolbox side — see the class javadoc.
     */
    private static void place(final ServerLevel level, final BlockPos pos, final int code,
                              final Direction dir, final Cell cell) {
        GenCommon.placeBlock(level, pos, code, dir, cell);
    }

    /**
     * The original's {@code BlockUtils.isAdjacentToSolidBlock(world, x, y, z)}
     * (reference BlockUtils L313-322): any of the six faces sturdy, checked
     * from the neighbour back toward the cell.
     */
    private static boolean isAdjacentToSolidBlock(final LevelReader level, final BlockPos pos) {
        for (final Direction d : Direction.values()) {
            final BlockPos side = pos.relative(d);
            if (level.getBlockState(side).isFaceSturdy(level, side, d.getOpposite())) {
                return true;
            }
        }

        return false;
    }

    /**
     * The original's {@code Utils.setBiomeAt(world, x, z, biome)} (reference
     * Utils L171-186): the 1.7.10 chunk biome array is one byte per column;
     * 1.21.1 keeps a 4x4x4 palette per section, so this writes the palette
     * cell that covers the column at the cell's own height, then marks the
     * chunk unsaved (the shipped pattern, as in {@code world/NodeTickDriver}).
     * The original's {@code PacketBiomeChange} has no 1.21.1 caller.
     */
    private static void setBiomeAt(final ServerLevel level, final int x, final int y, final int z,
                                   final ResourceKey<Biome> key) {
        if (key == null) {
            return;
        }

        final RegistryAccess access = level.registryAccess();
        final Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);
        final Holder<Biome> holder = registry.getHolder(key).orElse(null);
        if (holder == null) {
            return;
        }

        final LevelChunk chunk = level.getChunkAt(new BlockPos(x, y, z));
        final int section = Math.floorDiv(y, 16) - chunk.getMinSection();
        if (section < 0 || section >= chunk.getSections().length) {
            return;
        }

        @SuppressWarnings("unchecked")
        final PalettedContainer<Holder<Biome>> biomes =
                (PalettedContainer<Holder<Biome>>) chunk.getSections()[section].getBiomes();
        biomes.set(x & 3, y & 3, z & 3, holder);
        chunk.setUnsaved(true);
    }

    /**
     * The {@code LevelWriter} resolved to a {@code ServerLevel}, else null
     * (the reads, the biome palette and the spawner tile need it). The shipped
     * maze call hands a {@code ServerLevel}, which implements
     * {@code LevelWriter} through {@code Level}/{@code LevelAccessor}.
     */
    private static ServerLevel serverLevel(final LevelWriter level) {
        return level instanceof ServerLevel server ? server : null;
    }
}
