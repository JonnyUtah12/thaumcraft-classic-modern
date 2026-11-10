package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.recipes.M404RecipeSerializers;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionMatrix;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionPillar;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M4-04 registration holder: the infusion altar's blocks and block entities
 * (content-mapping.csv tile_entity rows {@code tileinfusionmatrix},
 * {@code tileinfusionpillar}, {@code tilepedestal}) and the (block, variant)
 * → tile bindings for blockStoneDevice 1 (arcane pedestal), 2 (runic matrix)
 * and 3/4 (infusion pillars), matching the original
 * {@code BlockStoneDevice.createTileEntity}.
 *
 * <h2>Provisional ownership (docs/catalog-triage.md)</h2>
 * blockStoneDevice is owned by TC-M6-04c and the pedestal BE id by TC-M6-04d,
 * but both cards were abandoned mid-implementation and the infusion altar is
 * the only consumer of variants 1-4 — the original's arcane pedestal exists
 * solely for the altar. Until those cards resume, this holder registers the
 * device block, its block item and the pedestal BE type, and binds only the
 * four altar variants. When TC-M6-04c lands its own {@code M604cBlocks}
 * registration, drop the block/item rows here and keep only the tile
 * bindings; when TC-M6-04d resumes, move the TILE_PEDESTAL holder there. The
 * remaining variants (0, 5-14) intentionally have no tile binding yet — the
 * original's {@code createTileEntity} returns null for unimplemented ones,
 * and the other feature cards bind theirs from their own holders through
 * {@link DeviceTileFactory}. The recipe types/serializers live in
 * {@link M404RecipeSerializers}.</p>
 */
public final class M404Infusion {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    ThaumcraftClassic.MOD_ID);

    public static final DeferredBlock<BlockStoneDevice> BLOCK_STONE_DEVICE =
            BLOCKS.register("blockstonedevice", BlockStoneDevice::new);
    public static final DeferredItem<BlockItem> ITEM_STONE_DEVICE =
            ITEMS.register("blockstonedevice",
                    () -> new BlockItem(BLOCK_STONE_DEVICE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePedestal>> TILE_PEDESTAL =
            BLOCK_ENTITY_TYPES.register("tilepedestal",
                    () -> BlockEntityType.Builder.of(TilePedestal::new, BLOCK_STONE_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionMatrix>> TILE_INFUSION_MATRIX =
            BLOCK_ENTITY_TYPES.register("tileinfusionmatrix",
                    () -> BlockEntityType.Builder.of(TileInfusionMatrix::new, BLOCK_STONE_DEVICE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionPillar>> TILE_INFUSION_PILLAR =
            BLOCK_ENTITY_TYPES.register("tileinfusionpillar",
                    () -> BlockEntityType.Builder.of(TileInfusionPillar::new, BLOCK_STONE_DEVICE.get()).build(null));

    public static void register(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        M404RecipeSerializers.RECIPE_TYPES.register(modEventBus);
        M404RecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE)) {
                bindDeviceTiles();
            }
        });
    }

    /** Called after all blocks are registered (blocks flush before block entities). */
    public static void bindDeviceTiles() {
        final BlockStoneDevice stone = BLOCK_STONE_DEVICE.get();
        // blockStoneDevice 1 = arcane pedestal (original createTileEntity meta 1;
        // BlockStoneDevice has no named constant for it).
        DeviceTileFactory.register(stone, 1, TilePedestal::new);
        DeviceTileFactory.register(stone, BlockStoneDevice.VARIANT_RUNIC_MATRIX, TileInfusionMatrix::new);
        DeviceTileFactory.register(stone, BlockStoneDevice.VARIANT_PILLAR_A, TileInfusionPillar::new);
        DeviceTileFactory.register(stone, BlockStoneDevice.VARIANT_PILLAR_B, TileInfusionPillar::new);
    }

    private M404Infusion() {
    }
}
