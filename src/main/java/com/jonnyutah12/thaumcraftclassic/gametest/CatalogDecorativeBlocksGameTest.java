package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticOpaque;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticSolid;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStoneSlab;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticWoodSlab;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCandle;
import com.jonnyutah12.thaumcraftclassic.items.ItemShard;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M6-04b-s evidence: the card's "block registry + blockstate/model presence"
 * bar plus the drop semantics that moved to block loot tables.
 *
 * <ol>
 *   <li>{@link #decorativeFamiliesCarryTheirVariants} — each contract id
 *       resolves in the BLOCK and ITEM registries, is placeable in-world
 *       ({@code assertBlockPresent}: the block's shape/model path is usable),
 *       and declares the original's meta range as the {@code variant} block
 *       state property with a value-for-value round-trip (D-001 keeps the
 *       original meta numbers); the four stair families produce real stair
 *       states. The content-mapping.csv rows this card flipped to {@code done}
 *       are re-checked against the live registries, so id stability keeps being
 *       enforced after this commit.</li>
 *   <li>{@link #crystalClustersDropTheirShards} — original
 *       {@code BlockCrystal.getDrops} through the 1.21.x loot-table path.</li>
 *   <li>{@link #slabDropsKeepTheirVariant} — original
 *       {@code quantityFromDrops}: two variant-carrying singles from a merged
 *       double wood slab, one from a stone slab.</li>
 * </ol>
 *
 * <p>Needs the {@code gameTestServer} run configuration (build.gradle), which
 * sets {@code -Dtc.contentMappingFile}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatalogDecorativeBlocksGameTest {

    private CatalogDecorativeBlocksGameTest() {
    }

    /** Every block id this card owns: six variant families, four stairs, two legacy slabs. */
    private static List<String> contractIds() {
        return List.of("blockcosmeticopaque", "blockcosmeticsolid", "blockcrystal", "blockcandle",
                "blockcosmeticslabwood", "blockcosmeticslabstone",
                "blockstairsarcanestone", "blockstairsgreatwood", "blockstairssilverwood",
                "blockstairseldritch", "blockcosmeticdoubleslabwood", "blockcosmeticdoubleslabstone");
    }

    private static List<ContentMappingCsv.Row> readContract(final GameTestHelper helper) {
        final String path = System.getProperty("tc.contentMappingFile", "");
        helper.assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.contentMappingFile is not set or missing — the gameTestServer run config must provide it");
        try {
            return ContentMappingCsv.read(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkFamily(final GameTestHelper helper, final String id, final Block block,
                                   final IntegerProperty variant, final int originalMetaCount) {
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, id);
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), id + " is not registered in the block registry");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key),
                id + " has no block item under the same contract id");

        final BlockState base = block.defaultBlockState();
        helper.setBlock(new BlockPos(1, 1, 1), base);
        helper.assertBlockPresent(block, 1, 1, 1);
        helper.assertTrue(base.getValue(variant) == 0,
                id + "'s default state must carry the original's default meta 0");
        helper.assertTrue(variant.getPossibleValues().size() == originalMetaCount,
                id + " must expose the original's " + originalMetaCount + " meta values, got "
                        + variant.getPossibleValues().size());
        for (int value = 0; value < originalMetaCount; value++) {
            final BlockState state = base.setValue(variant, value);
            helper.assertTrue(state.getValue(variant) == value,
                    id + " variant " + value + " did not round-trip through the block state");
            // every meta must be placeable through the block's model path
            helper.setBlock(new BlockPos(1, 1, 1), state);
            helper.assertBlockPresent(block, 1, 1, 1);
        }
    }

    @GameTest(template = "empty")
    public static void decorativeFamiliesCarryTheirVariants(final GameTestHelper helper) {
        // original meta counts from the 1.7.10 jar (docs/content-mapping.csv
        // names column): 3 opaque, 16 solid, 8 crystal, 16 candle, 2 slabs.
        checkFamily(helper, "blockcosmeticopaque", CatalogDecorativeBlocks.BLOCK_COSMETIC_OPAQUE.get(),
                BlockCosmeticOpaque.VARIANT, 3);
        checkFamily(helper, "blockcosmeticsolid", CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get(),
                BlockCosmeticSolid.VARIANT, 16);
        checkFamily(helper, "blockcrystal", CatalogDecorativeBlocks.BLOCK_CRYSTAL.get(),
                BlockCrystal.VARIANT, 8);
        checkFamily(helper, "blockcandle", CatalogDecorativeBlocks.BLOCK_CANDLE.get(),
                BlockCandle.VARIANT, 16);
        checkFamily(helper, "blockcosmeticslabwood", CatalogDecorativeBlocks.BLOCK_COSMETIC_SLAB_WOOD.get(),
                BlockCosmeticWoodSlab.VARIANT, 2);
        checkFamily(helper, "blockcosmeticslabstone", CatalogDecorativeBlocks.BLOCK_COSMETIC_SLAB_STONE.get(),
                BlockCosmeticStoneSlab.VARIANT, 2);

        // The stairs family carries the meta in its base block, not a property,
        // so the presence proof is the stair state + placement.
        for (final var holder : List.of(CatalogDecorativeBlocks.BLOCK_STAIRS_ARCANE_STONE,
                CatalogDecorativeBlocks.BLOCK_STAIRS_GREATWOOD,
                CatalogDecorativeBlocks.BLOCK_STAIRS_SILVERWOOD,
                CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH)) {
            final Block block = holder.get();
            helper.assertTrue(StairBlock.isStairs(block.defaultBlockState()),
                    "the stairs family does not produce a stair state");
            helper.setBlock(new BlockPos(1, 1, 1), block.defaultBlockState());
            helper.assertBlockPresent(block, 1, 1, 1);
        }

        // The rows this card flipped to done must keep resolving (id stability).
        var flipped = 0;
        for (final ContentMappingCsv.Row row : readContract(helper)) {
            if (!row.kind().equals("block") || !row.newId().startsWith(ThaumcraftClassic.MOD_ID + ":")) {
                continue;
            }
            final String name = row.newId().substring((ThaumcraftClassic.MOD_ID + ":").length());
            if (!contractIds().contains(name)) {
                continue;
            }
            helper.assertTrue(row.implementationStatus().equals("done"),
                    "card-owned row " + row.newId() + " must be marked done in this commit");
            helper.assertTrue(BuiltInRegistries.BLOCK
                    .containsKey(ResourceLocation.parse(row.newId())),
                    "csv row " + row.newId() + " is marked done but does not resolve");
            flipped++;
        }
        helper.assertTrue(flipped == contractIds().size(),
                "expected " + contractIds().size() + " decorative block rows in the contract, got " + flipped);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crystalClustersDropTheirShards(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = new BlockPos(1, 1, 1);
        final Block crystal = CatalogDecorativeBlocks.BLOCK_CRYSTAL.get();
        final BlockState base = crystal.defaultBlockState();

        for (final int variant : List.of(BlockCrystal.VARIANT_AIR, BlockCrystal.VARIANT_FIRE,
                BlockCrystal.VARIANT_WATER, BlockCrystal.VARIANT_EARTH,
                BlockCrystal.VARIANT_ORDER, BlockCrystal.VARIANT_ENTROPY)) {
            final List<ItemStack> drops =
                    Block.getDrops(base.setValue(BlockCrystal.VARIANT, variant), level, pos, null);
            helper.assertTrue(drops.size() == 6,
                    "a primal cluster must drop six shards, got " + drops.size());
            for (final ItemStack shard : drops) {
                helper.assertTrue(shard.is(CatalogResources.ITEM_SHARD.get()),
                        "the cluster must drop primal shards, got " + shard);
                helper.assertTrue(shard.getOrDefault(ModDataComponents.ITEM_META.get(), -1) == variant,
                        "the " + variant + " cluster dropped a shard of the wrong meta");
            }
        }

        final List<ItemStack> mixed =
                Block.getDrops(base.setValue(BlockCrystal.VARIANT, BlockCrystal.VARIANT_MIXED), level, pos, null);
        helper.assertTrue(mixed.size() == 6,
                "the mixed cluster must drop one of each primal, got " + mixed.size());
        final int[] perMeta = new int[ItemShard.META_COUNT];
        for (final ItemStack shard : mixed) {
            helper.assertTrue(shard.is(CatalogResources.ITEM_SHARD.get()), "mixed cluster dropped " + shard);
            final int meta = shard.getOrDefault(ModDataComponents.ITEM_META.get(), -1);
            helper.assertTrue(meta >= 0 && meta < ItemShard.META_COUNT,
                    "mixed cluster dropped a shard with an out-of-range meta: " + meta);
            perMeta[meta]++;
        }
        for (int meta = 0; meta < ItemShard.META_COUNT - 1; meta++) {
            helper.assertTrue(perMeta[meta] == 1,
                    "the mixed cluster must drop exactly one shard of meta " + meta
                            + ", got " + perMeta[meta]);
        }

        final List<ItemStack> strange =
                Block.getDrops(base.setValue(BlockCrystal.VARIANT, BlockCrystal.VARIANT_STRANGE), level, pos, null);
        helper.assertTrue(strange.size() == 1
                        && strange.get(0).getOrDefault(ModDataComponents.ITEM_META.get(), -1) == ItemShard.META_COUNT - 1,
                "the strange cluster must drop one balanced shard (meta " + (ItemShard.META_COUNT - 1) + ")");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slabDropsKeepTheirVariant(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        final BlockPos pos = new BlockPos(1, 1, 1);

        final Block wood = CatalogDecorativeBlocks.BLOCK_COSMETIC_SLAB_WOOD.get();
        final BlockState baseWood = wood.defaultBlockState();
        final BlockState doubleWood = baseWood
                .setValue(SlabBlock.TYPE, SlabType.DOUBLE)
                .setValue(BlockCosmeticWoodSlab.VARIANT, BlockCosmeticWoodSlab.VARIANT_SILVERWOOD);
        final List<ItemStack> drops = Block.getDrops(doubleWood, level, pos, null);
        helper.assertTrue(drops.size() == 2,
                "a merged double wood slab must yield two singles, got " + drops.size());
        for (final ItemStack drop : drops) {
            helper.assertTrue(drop.is(CatalogDecorativeBlocks.ITEM_COSMETIC_SLAB_WOOD.get()),
                    "the double slab must drop its own slab, got " + drop);
            assertCarriesVariant(helper, drop, baseWood, BlockCosmeticWoodSlab.VARIANT,
                    BlockCosmeticWoodSlab.VARIANT_SILVERWOOD, "wood slab");
        }

        final Block stone = CatalogDecorativeBlocks.BLOCK_COSMETIC_SLAB_STONE.get();
        final BlockState baseStone = stone.defaultBlockState();
        final BlockState singleStone =
                baseStone.setValue(BlockCosmeticStoneSlab.VARIANT, BlockCosmeticStoneSlab.VARIANT_ELDRITCH);
        final List<ItemStack> stoneDrops = Block.getDrops(singleStone, level, pos, null);
        helper.assertTrue(stoneDrops.size() == 1,
                "a single stone slab must drop itself once, got " + stoneDrops.size());
        for (final ItemStack drop : stoneDrops) {
            helper.assertTrue(drop.is(CatalogDecorativeBlocks.ITEM_COSMETIC_SLAB_STONE.get()),
                    "the stone slab must drop its own slab, got " + drop);
            assertCarriesVariant(helper, drop, baseStone, BlockCosmeticStoneSlab.VARIANT,
                    BlockCosmeticStoneSlab.VARIANT_ELDRITCH, "stone slab");
        }
        helper.succeed();
    }

    /** The dropped stack must re-place as the same material: its block_state component carries the variant. */
    private static void assertCarriesVariant(final GameTestHelper helper, final ItemStack drop,
                                             final BlockState base, final IntegerProperty variant,
                                             final int expectedVariant, final String what) {
        final BlockItemStateProperties component = drop.get(DataComponents.BLOCK_STATE);
        helper.assertTrue(component != null && !component.isEmpty(),
                what + " drop lost its block_state component (variant " + expectedVariant + " not preserved)");
        final BlockState restored = component.apply(base);
        helper.assertTrue(restored.getValue(variant) == expectedVariant,
                what + " drop must re-place as variant " + expectedVariant + ", got " + restored);
    }
}
