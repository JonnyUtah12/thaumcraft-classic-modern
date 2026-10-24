package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockStoneDevice;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.recipes.M404RecipeSerializers;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionMatrix;
import com.jonnyutah12.thaumcraftclassic.tiles.TileInfusionPillar;
import com.jonnyutah12.thaumcraftclassic.tiles.TilePedestal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M4-04 registration holder: the infusion altar's block entities
 * (content-mapping.csv tile_entity rows {@code tileinfusionmatrix},
 * {@code tileinfusionpillar}) and the (block, variant) → tile bindings for
 * blockStoneDevice 1 (arcane pedestal), 2 (runic matrix) and 3/4 (infusion
 * pillars), matching the original {@code BlockStoneDevice.createTileEntity}.
 *
 * <p>The pedestal tile class and its BE type are owned by TC-M6-04d
 * ({@link M604dFurniture#TILE_PEDESTAL}, valid on blockCosmeticSolid 15); the
 * arcane-pedestal binding on blockStoneDevice 1 is wired here because the
 * infusion altar is its only consumer and TC-M6-04d was abandoned before
 * wiring it (docs/catalog-triage.md). The recipe types/serializers live in
 * {@link M404RecipeSerializers}.</p>
 */
public final class M404Infusion {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.createBlockEntities(ThaumcraftClassic.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionMatrix>> TILE_INFUSION_MATRIX =
            BLOCK_ENTITY_TYPES.register("tileinfusionmatrix",
                    () -> BlockEntityType.Builder.of(TileInfusionMatrix::new, stoneDevice()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionPillar>> TILE_INFUSION_PILLAR =
            BLOCK_ENTITY_TYPES.register("tileinfusionpillar",
                    () -> BlockEntityType.Builder.of(TileInfusionPillar::new, stoneDevice()).build(null));

    private static Block stoneDevice() {
        return BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "blockstonedevice"));
    }

    public static void register(final IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        M404RecipeSerializers.RECIPE_TYPES.register(modEventBus);
        M404RecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE)) {
                bindDeviceTiles();
            }
        });
    }

    /** Called after all blocks are registered (ModRegistration ordering). */
    public static void bindDeviceTiles() {
        final Block stone = stoneDevice();
        if (stone == net.minecraft.world.level.block.Blocks.AIR) {
            return; // blockstonedevice not registered (TC-M6-04c unwired) — no altar
        }
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
