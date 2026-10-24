package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Golem totem generation — port of the original
 * {@code ThaumcraftWorldGenerator.generateTotem} (ThaumcraftWorldGenerator.java:112-183).
 *
 * <p>Gate transcription: {@code Config.genStructure} (gated by
 * {@link StructureChunkGen}), dimension {@code == 0 || == 1} — verified in
 * the original bytecode this is literally overworld-or-overworld, so the
 * {@code generateNether} call site is dead code and the netherrack ground
 * branch and {@code getFirstUncoveredY} branch are vestigial (kept for
 * fidelity: netherrack stays in the ground-block list, the overworld
 * heightmap is always used); {@code newGen} is always true in the modern
 * pipeline; {@code !auraGen} maps to "this chunk got no aura node from the
 * node pass" (checked through {@link WorldMagicData}); and
 * {@code random.nextInt(nodeRarity * 10) == 0}.</p>
 *
 * <p>Structure: an obsidian-tile base (blockCosmeticSolid meta 1) under a
 * 1-4 block obsidian-totem column (meta 0); with 25% chance (after height 2)
 * or on reaching height 5 without a node, a charged totem (meta 8) crowns it
 * and an eerie aura node is created at the crown.</p>
 */
final class TotemGen {

    private TotemGen() {
    }

    /**
     * One totem roll for a chunk. {@code auraGen} is the original's flag:
     * true when the chunk already produced an aura node (the node pass or an
     * earlier structure), which suppresses the totem entirely.
     */
    static void generate(final ServerLevel level, final ChunkPos pos, final Random random, final boolean auraGen) {
        if (auraGen) {
            return;
        }
        final int nodeRarity = TcConfig.getInt(TcConfigCatalog.NODE_RARITY);
        if (random.nextInt(Math.max(1, nodeRarity * 10)) != 0) {
            return;
        }
        final int x = pos.x * 16 + random.nextInt(16);
        final int z = pos.z * 16 + random.nextInt(16);
        // Original: nether uses Utils.getFirstUncoveredY, overworld the heightmap;
        // the dimension gate makes the nether branch unreachable, so the heightmap
        // (topmost non-air + 1, same convention as the original's func_72976_f)
        // minus 1 is the ground cell.
        int topy = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (topy > level.getMaxBuildHeight()) {
            return; // original's world-height guard (vestigial)
        }

        if (StructureGen.isLeaves(level.getBlockState(new BlockPos(x, topy, z)))) {
            do {
                topy--;
            } while (!level.getBlockState(new BlockPos(x, topy, z)).is(Blocks.GRASS_BLOCK) && topy > 40);
        }

        if (StructureGen.isSnowOrTallgrass(level.getBlockState(new BlockPos(x, topy, z)))) {
            topy--;
        }

        final BlockState ground = level.getBlockState(new BlockPos(x, topy, z));
        if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.SAND) || ground.is(Blocks.DIRT)
                || ground.is(Blocks.STONE) || ground.is(Blocks.NETHERRACK))) {
            return;
        }

        int count = 1;
        while ((StructureGen.isAir(level, x, topy + count, z)
                || StructureGen.isSnowOrTallgrass(level.getBlockState(new BlockPos(x, topy + count, z))))
                && count < 3) {
            count++;
        }
        if (count < 2) {
            return;
        }

        final BlockState tile = StructureGen.tcBlockState("blockcosmeticsolid", 1);
        final BlockState totem = StructureGen.tcBlockState("blockcosmeticsolid", 0);
        final BlockState charged = StructureGen.tcBlockState("blockcosmeticsolid", 8);
        if (tile == null) {
            return; // blockCosmeticSolid not registered (M6-04b); nothing to place
        }
        StructureGen.set(level, x, topy, z, tile);
        count = 1;
        boolean nodePlaced = false;
        while ((StructureGen.isAir(level, x, topy + count, z)
                || StructureGen.isSnowOrTallgrass(level.getBlockState(new BlockPos(x, topy + count, z))))
                && count < 5) {
            if (totem != null) {
                StructureGen.set(level, x, topy + count, z, totem);
            }
            if (count > 1 && random.nextInt(4) == 0) {
                if (charged != null) {
                    StructureGen.set(level, x, topy + count, z, charged);
                }
                NodeGeneration.createRandomNodeAt(level, x, topy + count, z, random, false, true, false);
                count = 5;
                nodePlaced = true;
            }
            if (++count >= 5 && !nodePlaced) {
                if (charged != null) {
                    StructureGen.set(level, x, topy + 5, z, charged);
                }
                NodeGeneration.createRandomNodeAt(level, x, topy + 5, z, random, false, true, false);
            }
        }
    }
}
