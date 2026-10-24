package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlockEntity;
import net.minecraft.world.level.block.MobSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Shared helpers for the TC-M5-05b structure generators (golem totem, shrine
 * mound, eldritch ring, hilltop stones), transcribed from the original
 * {@code ThaumcraftWorldGenerator} inner generators
 * (reference/decompiled/thaumcraft/thaumcraft/common/lib/world/).
 *
 * <p>TC-owned blocks (blockCosmeticSolid, blockEldritch, blockWoodenDevice,
 * blockLoot) are owned by other catalog cards (M6-04b/f/e) and may not be
 * registered yet; {@link #tcBlockState} resolves them through the registry at
 * generation time and returns null when absent, which the callers treat as
 * "skip this placement but keep the RNG stream identical" (the original's
 * ConfigBlocks statics were always non-null, so this is a porting seam, not
 * an original behavior).</p>
 */
final class StructureGen {

    private StructureGen() {
    }

    /**
     * Resolves a Thaumcraft-Classic block by id with the original metadata
     * applied through its {@code variant} property (D-001 multi-meta blocks),
     * or null when the block is not registered (its catalog card has not
     * landed). Callers must null-check and skip the placement.
     */
    static BlockState tcBlockState(final String path, final int meta) {
        return BuiltInRegistries.BLOCK
                .getOptional(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path))
                .map(block -> withMeta(block, meta))
                .orElse(null);
    }

    private static BlockState withMeta(final Block block, final int meta) {
        final BlockState base = block.defaultBlockState();
        if (block.getStateDefinition().getProperty("variant") instanceof IntegerProperty property
                && property.getMin() <= meta && meta <= property.getMax()) {
            return base.setValue(property, meta);
        }
        return base;
    }

    /**
     * The original's {@code LocationIsValidSpawn} up-scan variant (mound,
     * eldritch ring, hilltop stones — identical bodies): walk up from (x,y,z)
     * to the first air cell; more than 2 blocks of solid above fails; the
     * ground block (last solid) must be in {@code valid}, or snow/tallgrass
     * over a valid block.
     */
    static boolean locationIsValidSpawn(final ServerLevel level, final int x, final int y, final int z,
                                       final Predicate<BlockState> valid) {
        int distanceToAir = 0;
        BlockState check = level.getBlockState(new BlockPos(x, y + distanceToAir, z));
        while (!check.isAir()) {
            distanceToAir++;
            check = level.getBlockState(new BlockPos(x, y + distanceToAir, z));
        }
        if (distanceToAir > 2) {
            return false;
        }
        final int ground = y + distanceToAir - 1;
        final BlockState blockId = level.getBlockState(new BlockPos(x, ground, z));
        if (!level.getBlockState(new BlockPos(x, ground + 1, z)).isAir()) {
            return false;
        }
        if (valid.test(blockId)) {
            return true;
        }
        final BlockState below = level.getBlockState(new BlockPos(x, ground - 1, z));
        return (blockId.is(Blocks.SNOW) || blockId.is(Blocks.SHORT_GRASS)) && valid.test(below);
    }

    /**
     * The original's {@code GetValidSpawnBlocks} for the eldritch ring:
     * stone, sand, packed ice, grass, gravel, dirt.
     */
    static boolean ringValidSpawn(final BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT);
    }

    /**
     * The original's {@code GetValidSpawnBlocks} for the mound and the
     * hilltop stones: stone, grass, dirt.
     */
    static boolean groundValidSpawn(final BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }

    /**
     * The original's {@code isFoliage} equivalence as used by NodeGeneration
     * (leaves, grass/fern and flowers); keep in sync with
     * {@code NodeGeneration.isFoliage}.
     */
    static boolean isFoliage(final BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN);
    }

    /** setBlock with update flags 3 (the original's setBlockWithNotify family). */
    static void set(final ServerLevel level, final int x, final int y, final int z, final BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    static void setAir(final ServerLevel level, final int x, final int y, final int z) {
        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    /** The original's {@code world.isAirBlock} (out-of-world reads are air, as modern getBlockState gives). */
    static boolean isAir(final ServerLevel level, final int x, final int y, final int z) {
        return level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    /** Snow layer or tallgrass — the original's {@code field_150431_aC}/{@code field_150329_H} pair. */
    static boolean isSnowOrTallgrass(final BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS);
    }

    /** The original's {@code isLeaves} (totem leaf-canopy descent). */
    static boolean isLeaves(final BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    /**
     * The original's {@code WeightedRandomChestContent.func_76293_a(rand,
     * ChestGenHooks.getInfo("dungeonChest").getItems(rand), chest,
     * .getCount(rand))}: fill one dungeon-chest roll into empty slots.
     * Deviation: the modern dungeon loot table rolls with its own source, so
     * the chunk {@link Random} stream is untouched by chest contents (the
     * original consumed it); structure frequencies and placement rules are
     * unaffected.
     */
    static void fillDungeonChest(final ServerLevel level, final BlockPos pos) {
        final ChestBlockEntity chest = level.getBlockEntity(pos, BlockEntityType.CHEST).orElse(null);
        if (chest == null) {
            return;
        }
        final LootTable table = level.getServer().getLootData().getLootTable(LootTables.CHESTS_DUNGEON);
        final LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        for (final ItemStack stack : table.getRandomItems(params)) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                if (chest.getItem(slot).isEmpty()) {
                    chest.setItem(slot, stack);
                    break;
                }
            }
        }
    }

    /**
     * The original's {@code TileEntityMobSpawner.func_145881_a().func_98272_a(id)}.
     * Unknown entity ids (e.g. {@code thaumcraft:wisp} before M6-06) leave the
     * spawner empty, matching the original's behavior for unresolvable ids.
     */
    static void setSpawnerEntity(final ServerLevel level, final BlockPos pos, final String entityId) {
        final MobSpawnerBlockEntity spawner = level.getBlockEntity(pos, BlockEntityType.MOB_SPAWNER).orElse(null);
        if (spawner == null) {
            return;
        }
        BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(entityId)).ifPresent(type ->
                spawner.getSpawner().setEntityId(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type)));
    }

    /**
     * The original's {@code growVines} (WorldGenHilltopStones.java:141-147):
     * place a vine (through the vanilla WorldGenerator.setBlockState
     * air/replaceable/leaves/foliage guard), then extend down while the cell
     * below is air, up to 4 more.
     *
     * <p>Metadata mapping: the original's 1.7.10 vine bits 1/2/4/8 attach
     * NORTH/EAST/SOUTH/WEST (the pre-flattening convention). TC's call sites
     * pass 8 for the cell west of the pillar, 2 east, 1 north, 4 south — i.e.
     * the vine attaches to the face away from the pillar; that quirk is
     * transcribed as-is.</p>
     */
    static void growVines(final ServerLevel level, final int x, final int y, final int z, final int meta) {
        setVine(level, x, y, z, meta);
        int remaining = 4;
        int yy = y;
        while (isAir(level, x, --yy, z) && remaining > 0) {
            setVine(level, x, yy, z, meta);
            remaining--;
        }
    }

    private static void setVine(final ServerLevel level, final int x, final int y, final int z, final int meta) {
        final BlockPos pos = new BlockPos(x, y, z);
        final BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced() || isLeaves(existing) || isFoliage(existing)) {
            final BlockState vine = Blocks.VINE.defaultBlockState()
                    .setValue(BlockStateProperties.NORTH, (meta & 1) != 0)
                    .setValue(BlockStateProperties.EAST, (meta & 2) != 0)
                    .setValue(BlockStateProperties.SOUTH, (meta & 4) != 0)
                    .setValue(BlockStateProperties.WEST, (meta & 8) != 0);
            level.setBlock(pos, vine, 2);
        }
    }
}
