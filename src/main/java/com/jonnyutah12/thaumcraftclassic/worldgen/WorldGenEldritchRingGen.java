package com.jonnyutah12.thaumcraftclassic.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Random;

/**
 * Eldritch stone ring — port of the original {@code WorldGenEldritchRing}
 * (reference/decompiled/thaumcraft/thaumcraft/common/lib/world/WorldGenEldritchRing.java).
 *
 * <p>A 7×7 obsidian-tile floor (blockCosmeticSolid meta 1, or plain obsidian
 * for 25% of cells) carved through non-solid cells above ground, with an
 * eldritch altar (blockEldritch meta 0) + obelisk column (metas 1, 2×4) at
 * the center, eldritch stone pillars (meta 3) at the edge midpoints, and on
 * 40% of rolls four banners (blockWoodenDevice meta 8) facing inward at the
 * cardinal offsets. The original also flips a 10-sided die to arm the altar
 * spawner (types 0/1); {@code TileEldritchAltar} is TC-M6-04d's tile, so the
 * die is rolled (RNG parity) and the banner placement follows its cases, but
 * the altar's spawner state is applied by that card when the tile lands.</p>
 *
 * <p>Original quirks transcribed: for cells above ground the generator
 * places a block and then clears it (the placement's RNG draw still happens);
 * the {@code replaceBlock} biome lookup at L68 is dead code and is not
 * ported; the maze-overlap guard and the post-success {@code MazeThread} are
 * {@link StructureHooks} seams (TC-M6-04f).</p>
 */
final class WorldGenEldritchRingGen {

    /**
     * Soft link to TC-M6-04d's {@code TileBanner.setFacing(byte)}: resolved by
     * reflection so this class compiles and runs before that card lands (then
     * the banner keeps its default facing, exactly like an unhosted TE in the
     * original), and applies the original's facing values automatically once
     * the tile exists.
     */
    private static final Class<?> TILE_BANNER_CLASS;
    private static final MethodHandle TILE_BANNER_SET_FACING;

    static {
        Class<?> cls = null;
        MethodHandle setFacing = null;
        try {
            cls = Class.forName("com.jonnyutah12.thaumcraftclassic.tiles.TileBanner");
            setFacing = MethodHandles.lookup()
                    .findVirtual(cls, "setFacing", MethodType.methodType(void.class, byte.class));
        } catch (ReflectiveOperationException | RuntimeException e) {
            cls = null;
            setFacing = null;
        }
        TILE_BANNER_CLASS = cls;
        TILE_BANNER_SET_FACING = setFacing;
    }

    private WorldGenEldritchRingGen() {
    }

    /**
     * Original {@code func_76484_a} plus the caller-supplied maze fields
     * (chunkX/chunkZ/width/height were set on the instance by
     * generateSurface; only the maze guard reads them).
     */
    static boolean generate(final ServerLevel level, final Random rand, final int i, final int j, final int k,
                           final ChunkPos chunk, final int width, final int height) {
        if (!StructureGen.locationIsValidSpawn(level, i - 3, j, k - 3, StructureGen::ringValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i, j, k, StructureGen::ringValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i + 3, j, k, StructureGen::ringValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i + 3, j, k + 3, StructureGen::ringValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i, j, k + 3, StructureGen::ringValidSpawn)
                || StructureHooks.mazesInRange(chunk.x, chunk.z, width, height)) {
            return false;
        }

        final BlockState tile = StructureGen.tcBlockState("blockcosmeticsolid", 1);
        final BlockState altar = StructureGen.tcBlockState("blockeldritch", 0);
        final BlockState obelisk1 = StructureGen.tcBlockState("blockeldritch", 1);
        final BlockState obelisk2 = StructureGen.tcBlockState("blockeldritch", 2);
        final BlockState pillar = StructureGen.tcBlockState("blockeldritch", 3);
        final BlockState banner = StructureGen.tcBlockState("blockwoodendevice", 8);

        for (int x = i - 3; x <= i + 3; x++) {
            for (int z = k - 3; z <= k + 3; z++) {
                if (x != i - 3 && x != i + 3 || z != k - 3 && z != k + 3) {
                    for (int q = -4; q < 5; q++) {
                        final BlockState bb = level.getBlockState(new BlockPos(x, j + q, z));
                        if (q <= 0 || bb.canBeReplaced() || !bb.hasBlockCollision()
                                || StructureGen.isFoliage(bb)) {
                            if (rand.nextInt(4) == 0) {
                                StructureGen.set(level, x, j + q, z, Blocks.OBSIDIAN.defaultBlockState());
                            } else if (tile != null) {
                                StructureGen.set(level, x, j + q, z, tile);
                            }
                        }
                        if (q > 0) {
                            StructureGen.setAir(level, x, j + q, z);
                        }
                    }

                    if (x == i && z == k) {
                        if (altar != null) {
                            StructureGen.set(level, x, j + 1, z, altar);
                        }
                        if (tile != null) {
                            StructureGen.set(level, x, j, z, tile);
                        }
                        final int r = rand.nextInt(10);
                        // Original: TileEldritchAltar.setSpawner/setSpawnType per
                        // case (1-4 -> type 0 + banners, 6-7 -> type 1). The tile
                        // is TC-M6-04d's; the die and the banner placement ride
                        // here, the altar's spawner state lands with that card.
                        if (r >= 1 && r <= 4) {
                            for (int a = 0; a < 4; a++) {
                                // Original: x - dir.offsetX*3, z + dir.offsetZ*3 for
                                // ForgeDirection ordinals 2..5 (NORTH, SOUTH, WEST,
                                // EAST) — the minus sign puts the "west" banner on
                                // the east side; transcribed as-is.
                                final int bx = x + new int[]{0, 0, 3, -3}[a];
                                final int bz = z + new int[]{-3, 3, 0, 0}[a];
                                if (banner != null) {
                                    StructureGen.set(level, bx, j + 1, bz, banner);
                                    final BlockEntity te = level.getBlockEntity(new BlockPos(bx, j + 1, bz));
                                    if (te != null && TILE_BANNER_CLASS != null
                                            && TILE_BANNER_CLASS.isInstance(te)) {
                                        try {
                                            // Original face values: NORTH->8, SOUTH->0, WEST->12, EAST->4.
                                            TILE_BANNER_SET_FACING.invoke(te, (byte) new int[]{8, 0, 12, 4}[a]);
                                        } catch (Throwable ignored) {
                                            // decorative state only; never fail worldgen
                                        }
                                    }
                                }
                            }
                        }
                        if (obelisk1 != null) {
                            StructureGen.set(level, x, j + 3, z, obelisk1);
                        }
                        for (int y = 4; y <= 7; y++) {
                            if (obelisk2 != null) {
                                StructureGen.set(level, x, j + y, z, obelisk2);
                            }
                        }
                    } else if ((x == i - 3 || x == i + 3) && Math.abs((z - k) % 2) == 1
                            || (z == k - 3 || z == k + 3) && Math.abs((x - i) % 2) == 1) {
                        if (Math.abs(x - i) != Math.abs(z - k)) {
                            if (tile != null) {
                                StructureGen.set(level, x, j, z, tile);
                            }
                            if (pillar != null) {
                                StructureGen.set(level, x, j + 1, z, pillar);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}
