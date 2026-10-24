package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.recipes.M405CrucibleContent;
import com.jonnyutah12.thaumcraftclassic.tiles.TileCrucible;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M4-05 registration holder: the crucible block entity
 * (content-mapping.csv tile_entity row {@code tilecrucible}) and the
 * (blockMetalDevice, variant 0) → {@link TileCrucible} binding, matching the
 * original {@code BlockMetalDevice.createTileEntity} metadata 0
 * (docs/catalog-triage.md shared multi-meta block map). The crucible recipe
 * type/serializer live in {@link M405CrucibleContent}.
 */
public final class M405Crucible {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.createBlockEntities(ThaumcraftClassic.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrucible>> TILE_CRUCIBLE =
            BLOCK_ENTITY_TYPES.register("tilecrucible",
                    () -> BlockEntityType.Builder.of(TileCrucible::new,
                            M604cBlocks.BLOCK_METAL_DEVICE.get()).build(null));

    public static void register(final IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        M405CrucibleContent.RECIPE_TYPES.register(modEventBus);
        M405CrucibleContent.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                bindDeviceTiles();
            }
        });
    }

    /** Called after all blocks are registered (ModRegistration ordering). */
    public static void bindDeviceTiles() {
        // blockMetalDevice 0 = crucible (original createTileEntity metadata 0).
        DeviceTileFactory.register(M604cBlocks.BLOCK_METAL_DEVICE.get(), 0, TileCrucible::new);
    }

    private M405Crucible() {
    }
}
