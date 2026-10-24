package com.jonnyutah12.thaumcraftclassic.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Hilltop stones — port of the original {@code WorldGenHilltopStones}
 * (reference/decompiled/thaumcraft/thaumcraft/common/lib/world/WorldGenHilltopStones.java).
 *
 * <p>A 7×7 obsidian-tile ring (blockCosmeticSolid meta 1, or plain obsidian
 * for 50% of cells) with stone-tile pillars (meta 0) rising 1-4 blocks along
 * the edge midpoints, a wisp spawner + double dungeon-chest at the center,
 * and vines on some pillar sides in non-snowy biomes. The ground under the
 * ring is filled in where it was snow/flowers/tallgrass/air.</p>
 *
 * <p>Adaptations, both documented: the original's {@code biome.topBlock}
 * (replaceBlock) is taken as the actual ground block at the validated center
 * cell — modern biomes expose no topBlock — and {@code !biome.getEnableSnow}
 * maps to {@code precipitation != SNOW}. The original's {@code replaceBlock
 * != null} guard covered only its snow-layer branch (operator precedence);
 * replaceBlock is never null in the port, so the guard is dropped.</p>
 */
final class WorldGenHilltopStonesGen {

    private WorldGenHilltopStonesGen() {
    }

    /** Original {@code func_76484_a}; returns whether the structure was placed. */
    static boolean generate(final ServerLevel level, final Random rand, final int i, final int j, final int k) {
        if (!locationIsValidSpawn(level, i - 2, j, k - 2)
                || !locationIsValidSpawn(level, i, j, k)
                || !locationIsValidSpawn(level, i + 2, j, k)
                || !locationIsValidSpawn(level, i + 2, j, k + 2)
                || !locationIsValidSpawn(level, i, j, k + 2)) {
            return false;
        }

        // Original: biome topBlock; port: the actual ground block at the center
        // (the up-scan validated ground sits at j-1 for a surface anchor).
        final BlockState replaceBlock = level.getBlockState(new BlockPos(i, j - 1, k));
        final boolean genVines = level.getBiome(new BlockPos(i, j, k)).value()
                .getPrecipitationAt(new BlockPos(i, j, k))
                != Biome.Precipitation.SNOW;

        final BlockState tile = StructureGen.tcBlockState("blockcosmeticsolid", 1);
        final BlockState stoneTile = StructureGen.tcBlockState("blockcosmeticsolid", 0);

        for (int x = i - 3; x <= i + 3; x++) {
            for (int z = k - 3; z <= k + 3; z++) {
                if (x != i - 3 && x != i + 3 || z != k - 3 && z != k + 3) {
                    // Base cell: obsidian tile, or plain obsidian half the time
                    // (the coin is always flipped; an unregistered tile block
                    // skips its branch without changing the RNG stream).
                    if (rand.nextBoolean()) {
                        if (tile != null) {
                            StructureGen.set(level, x, j, z, tile);
                        }
                    } else {
                        StructureGen.set(level, x, j, z, Blocks.OBSIDIAN.defaultBlockState());
                    }

                    boolean stop = false;

                    for (int y = 1; y < 5; y++) {
                        if (j - y >= 0) {
                            final BlockState blockId = level.getBlockState(new BlockPos(x, j - y, z));
                            if (blockId.is(Blocks.SNOW) || blockId.is(BlockTags.SMALL_FLOWERS)
                                    || blockId.is(Blocks.SHORT_GRASS) || blockId.is(Blocks.FERN)
                                    || blockId.isAir()) {
                                StructureGen.set(level, x, j - y, z, replaceBlock);
                            }

                            if (x == i && z == k && y == 1) {
                                if (tile != null) {
                                    StructureGen.set(level, x, j + y, z, tile);
                                }
                                StructureGen.set(level, x, j + y + 1, z, Blocks.CHEST.defaultBlockState());
                                StructureGen.fillDungeonChest(level, new BlockPos(x, j + y + 1, z));
                                StructureGen.fillDungeonChest(level, new BlockPos(x, j + y + 1, z));
                                StructureGen.set(level, x, j + y - 1, z, Blocks.TRIAL_SPAWNER.defaultBlockState());
                                StructureGen.setSpawnerEntity(level, new BlockPos(x, j + y - 1, z), "thaumcraft:wisp");
                            }

                            if (!stop && ((x == i - 3 || x == i + 3) && Math.abs((z - k) % 2) == 1
                                    || (z == k - 3 || z == k + 3) && Math.abs((x - i) % 2) == 1)) {
                                if (stoneTile != null) {
                                    StructureGen.set(level, x, j + y, z, stoneTile);
                                }
                                if (y >= 2 && rand.nextBoolean()) {
                                    stop = true;
                                    if (genVines) {
                                        if (rand.nextInt(3) == 0 && StructureGen.isAir(level, x - 1, j + y, z)) {
                                            StructureGen.growVines(level, x - 1, j + y, z, 8);
                                        }
                                        if (rand.nextInt(3) == 0 && StructureGen.isAir(level, x + 1, j + y, z)) {
                                            StructureGen.growVines(level, x + 1, j + y, z, 2);
                                        }
                                        if (rand.nextInt(3) == 0 && StructureGen.isAir(level, x, j + y, z - 1)) {
                                            StructureGen.growVines(level, x, j + y, z - 1, 1);
                                        }
                                        if (rand.nextInt(3) == 0 && StructureGen.isAir(level, x, j + y, z + 1)) {
                                            StructureGen.growVines(level, x, j + y, z + 1, 4);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /** The original's LocationIsValidSpawn, including its y >= 85 floor (hilltop only). */
    private static boolean locationIsValidSpawn(final ServerLevel level, final int x, final int y, final int z) {
        if (y < 85) {
            return false;
        }
        return StructureGen.locationIsValidSpawn(level, x, y, z, StructureGen::groundValidSpawn);
    }
}
