package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCandle;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCandleItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticOpaque;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticOpaqueItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticSolid;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticSolidItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStairs;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStoneSlab;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStoneSlabItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticWoodSlab;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticWoodSlabItem;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystalItem;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrystal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M6-04b decorative block catalog (docs/catalog-triage.md shared
 * multi-meta block map): the cosmetic block family — blockCosmeticOpaque,
 * blockCosmeticSolid, blockCrystal, the four cosmetic stairs, the wood/stone
 * cosmetic slabs (plus their legacy double-slab ids) and the tallow candle —
 * with their block items and the crystal orientation tile.
 *
 * <p>Every id is the content-mapping.csv block_id verbatim lower-cased
 * (enforced by ContentMappingIdContractTest). Multi-meta originals become one
 * block with a {@code variant} property (D-001); variant-carrying items
 * round-trip the placed state through the vanilla {@code block_state}
 * component (see {@link VariantStateItems}).</p>
 *
 * <p>Cross-card tile hosts are wired through
 * {@link com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory}: the
 * warding stone (TC-M6-04d) and charged-totem node host (TC-M5-01) tiles are
 * registered by those cards; this class only registers this card's own
 * {@link TileCrystal}.</p>
 */
public final class CatalogDecorativeBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    ThaumcraftClassic.MOD_ID);

    // --- cosmetic opaque (amber block / amber bricks / warded glass) -------
    public static final DeferredHolder<Block, BlockCosmeticOpaque> BLOCK_COSMETIC_OPAQUE =
            BLOCKS.register("blockcosmeticopaque", BlockCosmeticOpaque::new);
    public static final DeferredHolder<Item, BlockCosmeticOpaqueItem> ITEM_COSMETIC_OPAQUE =
            ITEMS.registerBlockItem("blockcosmeticopaque", () -> new BlockCosmeticOpaqueItem(
                    BLOCK_COSMETIC_OPAQUE.get(), variantAwareItem()));

    // --- cosmetic solid (16 variants) --------------------------------------
    public static final DeferredHolder<Block, BlockCosmeticSolid> BLOCK_COSMETIC_SOLID =
            BLOCKS.register("blockcosmeticsolid", BlockCosmeticSolid::new);
    public static final DeferredHolder<Item, BlockCosmeticSolidItem> ITEM_COSMETIC_SOLID =
            ITEMS.registerBlockItem("blockcosmeticsolid", () -> new BlockCosmeticSolidItem(
                    BLOCK_COSMETIC_SOLID.get(), variantAwareItem()));

    // --- crystal clusters + orientation tile --------------------------------
    public static final DeferredHolder<Block, BlockCrystal> BLOCK_CRYSTAL =
            BLOCKS.register("blockcrystal", BlockCrystal::new);
    public static final DeferredHolder<Item, BlockCrystalItem> ITEM_CRYSTAL =
            ITEMS.registerBlockItem("blockcrystal", () -> new BlockCrystalItem(
                    BLOCK_CRYSTAL.get(), variantAwareItem()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrystal>> TILE_CRYSTAL =
            BLOCK_ENTITY_TYPES.register("tilecrystal", () -> BlockEntityType.Builder
                    .of(TileCrystal::new, BLOCK_CRYSTAL.get())
                    .build(null));

    // --- cosmetic stairs (four base blocks, one class) --------------------
    public static final DeferredHolder<Block, BlockCosmeticStairs> BLOCK_STAIRS_ARCANE_STONE =
            BLOCKS.register("blockstairsarcanestone", () -> new BlockCosmeticStairs(
                    () -> BLOCK_COSMETIC_SOLID.get().defaultBlockState()
                            .setValue(BlockCosmeticSolid.VARIANT, BlockCosmeticSolid.VARIANT_ARCANE_STONE_BRICKS),
                    30.0F));
    public static final DeferredHolder<Item, BlockItem> ITEM_STAIRS_ARCANE_STONE =
            ITEMS.registerSimpleBlockItem("blockstairsarcanestone", BLOCK_STAIRS_ARCANE_STONE);
    public static final DeferredHolder<Block, BlockCosmeticStairs> BLOCK_STAIRS_GREATWOOD =
            BLOCKS.register("blockstairsgreatwood", () -> new BlockCosmeticStairs(
                    BlockCosmeticStairs.woodenPlanks(6), 5.0F));
    public static final DeferredHolder<Item, BlockItem> ITEM_STAIRS_GREATWOOD =
            ITEMS.registerSimpleBlockItem("blockstairsgreatwood", BLOCK_STAIRS_GREATWOOD);
    public static final DeferredHolder<Block, BlockCosmeticStairs> BLOCK_STAIRS_SILVERWOOD =
            BLOCKS.register("blockstairssilverwood", () -> new BlockCosmeticStairs(
                    BlockCosmeticStairs.woodenPlanks(7), 5.0F));
    public static final DeferredHolder<Item, BlockItem> ITEM_STAIRS_SILVERWOOD =
            ITEMS.registerSimpleBlockItem("blockstairssilverwood", BLOCK_STAIRS_SILVERWOOD);
    public static final DeferredHolder<Block, BlockCosmeticStairs> BLOCK_STAIRS_ELDRITCH =
            BLOCKS.register("blockstairseldritch", () -> new BlockCosmeticStairs(
                    () -> BLOCK_COSMETIC_SOLID.get().defaultBlockState()
                            .setValue(BlockCosmeticSolid.VARIANT, BlockCosmeticSolid.VARIANT_ANCIENT_STONE),
                    30.0F));
    public static final DeferredHolder<Item, BlockItem> ITEM_STAIRS_ELDRITCH =
            ITEMS.registerSimpleBlockItem("blockstairseldritch", BLOCK_STAIRS_ELDRITCH);

    // --- cosmetic slabs (wood / stone + legacy double-slab ids) -----------
    public static final DeferredHolder<Block, BlockCosmeticWoodSlab> BLOCK_COSMETIC_SLAB_WOOD =
            BLOCKS.register("blockcosmeticslabwood", BlockCosmeticWoodSlab::new);
    public static final DeferredHolder<Item, BlockCosmeticWoodSlabItem> ITEM_COSMETIC_SLAB_WOOD =
            ITEMS.registerBlockItem("blockcosmeticslabwood", () -> new BlockCosmeticWoodSlabItem(
                    BLOCK_COSMETIC_SLAB_WOOD.get(), variantAwareItem()));
    public static final DeferredHolder<Block, BlockCosmeticWoodSlab> BLOCK_COSMETIC_DOUBLE_SLAB_WOOD =
            BLOCKS.register("blockcosmeticdoubleslabwood", BlockCosmeticWoodSlab::new);
    public static final DeferredHolder<Item, BlockItem> ITEM_COSMETIC_DOUBLE_SLAB_WOOD =
            ITEMS.registerSimpleBlockItem("blockcosmeticdoubleslabwood", BLOCK_COSMETIC_DOUBLE_SLAB_WOOD);
    public static final DeferredHolder<Block, BlockCosmeticStoneSlab> BLOCK_COSMETIC_SLAB_STONE =
            BLOCKS.register("blockcosmeticslabstone", BlockCosmeticStoneSlab::new);
    public static final DeferredHolder<Item, BlockCosmeticStoneSlabItem> ITEM_COSMETIC_SLAB_STONE =
            ITEMS.registerBlockItem("blockcosmeticslabstone", () -> new BlockCosmeticStoneSlabItem(
                    BLOCK_COSMETIC_SLAB_STONE.get(), variantAwareItem()));
    public static final DeferredHolder<Block, BlockCosmeticStoneSlab> BLOCK_COSMETIC_DOUBLE_SLAB_STONE =
            BLOCKS.register("blockcosmeticdoubleslabstone", BlockCosmeticStoneSlab::new);
    public static final DeferredHolder<Item, BlockItem> ITEM_COSMETIC_DOUBLE_SLAB_STONE =
            ITEMS.registerSimpleBlockItem("blockcosmeticdoubleslabstone", BLOCK_COSMETIC_DOUBLE_SLAB_STONE);

    // --- tallow candle (16 dye variants) -----------------------------------
    public static final DeferredHolder<Block, BlockCandle> BLOCK_CANDLE =
            BLOCKS.register("blockcandle", BlockCandle::new);
    public static final DeferredHolder<Item, BlockCandleItem> ITEM_CANDLE =
            ITEMS.registerBlockItem("blockcandle", () -> new BlockCandleItem(
                    BLOCK_CANDLE.get(), variantAwareItem()));

    private CatalogDecorativeBlocks() {
    }

    /**
     * Item properties for the variant-carrying block items. Variant stacks
     * carry the placed state through the vanilla {@code block_state}
     * component; the item classes apply it on placement and the block classes
     * re-attach it on drops/pick-block (see {@link VariantStateItems}).
     */
    private static Item.Properties variantAwareItem() {
        return new Item.Properties();
    }

    public static void init(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
