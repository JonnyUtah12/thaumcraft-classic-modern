package com.jonnyutah12.thaumcraftclassic.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Shrine mound — port of the original {@code WorldGenMound}
 * (reference/decompiled/thaumcraft/thaumcraft/common/lib/world/WorldGenMound.java).
 *
 * <p>The 19×16×19 block table is generated verbatim from the original's
 * sequential setBlock calls by {@code scripts/extract_mound_structure.py}
 * ({@link MoundStructure}); transcription parity is pinned by
 * MoundStructureParityTest. The tile/loot tail (loot crate/urn pair, chest
 * or trapped-chest+TNT trap, two mob spawners) is hand-ported below, as in
 * the original.</p>
 *
 * <p>Adaptations: the original picks crate-vs-urn with {@code world.rand}
 * (not the chunk random) — the port uses {@code level.getRandom()}, the
 * modern equivalent, keeping the chunk random stream untouched; the original
 * fetches the {@code pyramidJungleChest} and {@code strongholdLibrary} chest
 * hooks but never uses them (dead code, not ported); the chest is filled
 * from the modern dungeon loot table (see {@link StructureGen#fillDungeonChest}).
 * Stairs/ladder metadata maps 1.7.10 enumFacing (0 east, 1 west, 2 south,
 * 3 north, +4 upside-down) onto the modern FACING/HALF properties directly.</p>
 */
final class WorldGenMoundGen {

    private static final Map<BlockPos, BlockState> TABLE = decode();

    private WorldGenMoundGen() {
    }

    /** Original {@code func_76484_a}; returns whether the structure was placed. */
    static boolean generate(final ServerLevel level, final Random rand, final int i, final int j, final int k) {
        if (!StructureGen.locationIsValidSpawn(level, i + 9, j + 9, k + 9, StructureGen::groundValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i, j + 9, k, StructureGen::groundValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i + 18, j + 9, k, StructureGen::groundValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i + 18, j + 9, k + 18, StructureGen::groundValidSpawn)
                || !StructureGen.locationIsValidSpawn(level, i, j + 9, k + 18, StructureGen::groundValidSpawn)) {
            return false;
        }

        for (final Map.Entry<BlockPos, BlockState> entry : TABLE.entrySet()) {
            final BlockPos off = entry.getKey();
            StructureGen.set(level, i + off.getX(), j + off.getY(), k + off.getZ(), entry.getValue());
        }

        // --- tile/loot tail (WorldGenMound.java:2508-2545) ---
        // The original also fetches pyramidJungleChest/strongholdLibrary hooks
        // here and never uses them; not ported.
        final float rr1 = rand.nextFloat();
        final int md1 = rr1 < 0.1F ? 2 : (rr1 < 0.33F ? 1 : 0);
        placeLootContainer(level, i + 9, j + 1, k + 7, md1, rand);
        final float rr2 = rand.nextFloat();
        final int md2 = rr2 < 0.1F ? 2 : (rr2 < 0.33F ? 1 : 0);
        placeLootContainer(level, i + 9, j + 1, k + 11, md2, rand);

        if (rand.nextInt(3) == 0) {
            StructureGen.set(level, i + 10, j + 1, k + 9, Blocks.TRAPPED_CHEST.defaultBlockState());
            StructureGen.set(level, i + 10, j - 1, k + 9, Blocks.TNT.defaultBlockState());
        } else {
            StructureGen.set(level, i + 10, j + 1, k + 9, Blocks.CHEST.defaultBlockState());
        }
        final BlockPos chestPos = new BlockPos(i + 10, j + 1, k + 9);
        if (level.getBlockEntity(chestPos) != null) {
            StructureGen.fillDungeonChest(level, chestPos);
            if (rand.nextInt(5) == 0) {
                StructureGen.fillDungeonChest(level, chestPos);
            }
        }

        StructureGen.set(level, i + 4, j + 5, k + 4, Blocks.TRIAL_SPAWNER.defaultBlockState());
        StructureGen.set(level, i + 4, j + 5, k + 14, Blocks.TRIAL_SPAWNER.defaultBlockState());
        StructureGen.setSpawnerEntity(level, new BlockPos(i + 4, j + 5, k + 4), "minecraft:skeleton");
        StructureGen.setSpawnerEntity(level, new BlockPos(i + 4, j + 5, k + 14), "minecraft:zombie");

        return true;
    }

    /**
     * The original's {@code world.rand.nextFloat() < 0.3F ? blockLootCrate :
     * blockLootUrn} with the rolled meta. Unregistered (M6-04e) → skip.
     */
    private static void placeLootContainer(final ServerLevel level, final int x, final int y, final int z,
                                          final int meta, final Random rand) {
        final boolean crate = level.getRandom().nextFloat() < 0.3F;
        final BlockState state = StructureGen.tcBlockState(crate ? "blocklootcrate" : "blocklooturn", meta);
        if (state != null) {
            StructureGen.set(level, x, y, z, state);
        }
    }

    private static Map<BlockPos, BlockState> decode() {
        final Map<BlockPos, BlockState> table = new HashMap<>();
        for (final String row : MoundStructure.ROWS) {
            final int bar = row.indexOf('|');
            final String[] yx = row.substring(0, bar).split(",");
            final int y = Integer.parseInt(yx[0]);
            final int x = Integer.parseInt(yx[1]);
            final String cells = row.substring(bar + 1);
            for (int z = 0; z < cells.length(); z++) {
                final BlockState state = stateFor(cells.charAt(z));
                if (state != null) {
                    table.put(new BlockPos(x, y, z + MoundStructure.Z_MIN), state);
                }
            }
        }
        return table;
    }

    private static BlockState stateFor(final char ch) {
        return switch (ch) {
            case '.' -> null;
            case 'a' -> Blocks.AIR.defaultBlockState();
            case 'd' -> Blocks.DIRT.defaultBlockState();
            case 'g' -> Blocks.GRASS_BLOCK.defaultBlockState();
            case 'c' -> Blocks.COBBLESTONE.defaultBlockState();
            case 'm' -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 'n' -> Blocks.STONE_BRICKS.defaultBlockState();
            case 'o' -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case 'p' -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case 'q' -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case 'r' -> Blocks.IRON_BARS.defaultBlockState();
            case 'l' -> Blocks.LADDER.defaultBlockState();
            case 'L' -> setDeclaredFacing(Blocks.LADDER.defaultBlockState(),
                    net.minecraft.core.Direction.WEST);
            case 's', 'u', 'v', 'w', 'x', 'y', 'z', 'A' -> stairs("suvwxyzA".indexOf(ch));
            case 't' -> Blocks.SHORT_GRASS.defaultBlockState();
            case 'f' -> Blocks.FERN.defaultBlockState();
            default -> throw new IllegalStateException("unknown mound table char: " + ch);
        };
    }

    /** 1.7.10 stone_stairs enumFacing 0..7 -> modern cobblestone stairs: the
     * four horizontal facings plus the +4 inversion bit mapped onto the HALF
     * property. {@code facing} is set through the block's own declared property
     * instance because stairs/ladders restrict that property's Direction domain
     * to the four horizontal directions, and the shared
     * BlockStateProperties.FACING constant (full six-value domain) is rejected
     * by the state-property matcher. */
    private static BlockState stairs(final int meta) {
        final net.minecraft.core.Direction facing =
                new net.minecraft.core.Direction[]{net.minecraft.core.Direction.EAST,
                        net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.SOUTH,
                        net.minecraft.core.Direction.NORTH}[meta & 3];
        final BlockState base = Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM)
                .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
        return setDeclaredFacing(base, facing);
    }

    private static BlockState setDeclaredFacing(final BlockState base,
            final net.minecraft.core.Direction facing) {
        for (final net.minecraft.world.level.block.state.properties.Property p : base.getProperties()) {
            if (p.toString().startsWith("DirectionProperty{name=facing,")) {
                return base.setValue(p, facing);
            }
        }
        return base;
    }

    /** Decoded table size, for the parity test. */
    static List<Map.Entry<BlockPos, BlockState>> tableEntries() {
        return new ArrayList<>(TABLE.entrySet());
    }
}
