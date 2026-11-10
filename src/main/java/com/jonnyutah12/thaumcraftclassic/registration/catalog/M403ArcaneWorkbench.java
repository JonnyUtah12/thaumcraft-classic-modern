package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTable;
import com.jonnyutah12.thaumcraftclassic.blocks.DeviceTileFactory;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import com.jonnyutah12.thaumcraftclassic.recipes.M403RecipeSerializers;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M4-03 registration holder: the arcane workbench.
 *
 * <p>Owns the {@code thaumcraft:blocktable} block (content-mapping.csv block
 * row — the row itself stays {@code not_started}: the BlockTableItem variant
 * picker and the remaining table variants belong to TC-M6-04d, which was
 * abandoned before wiring any of it; the block lands here because the
 * workbench is its only implemented consumer, same precedent as
 * {@link M404Infusion} wiring blockStoneDevice tiles). Plus the
 * {@code tilearcaneworkbench} block entity (tile_entity row), the
 * {@code gui_arcane_workbench} menu (gui row), the {@code arcane_workbench}
 * recipe type/serializer, and the (blocktable, variant 15) → tile binding —
 * the original {@code BlockTable.createTileEntity} meta 15.</p>
 *
 * <p>No BlockItem: the CSV has no item row for blocktable (the original
 * BlockTableItem shows the variant picker and belongs to TC-M6-04d).</p>
 */
public final class M403ArcaneWorkbench {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.createBlockEntities(ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    public static final DeferredHolder<Block, BlockTable> BLOCK_TABLE =
            BLOCKS.register("blocktable", BlockTable::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneWorkbench>>
            TILE_ARCANE_WORKBENCH = BLOCK_ENTITY_TYPES.register("tilearcaneworkbench",
                    () -> BlockEntityType.Builder.of(TileArcaneWorkbench::new, BLOCK_TABLE.get())
                            .build(null));

    /** Menu type id = the CSV gui row (original GUI_ARCANE_WORKBENCH, gui_id 13). */
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneWorkbenchMenu>> MENU_TYPE =
            MENUS.register("gui_arcane_workbench",
                    () -> IMenuTypeExtension.create(ArcaneWorkbenchMenu::new));

    public static void register(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        M403RecipeSerializers.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                // blockTable meta 15 = arcane workbench (original createTileEntity).
                DeviceTileFactory.register(BLOCK_TABLE.get(), BlockTable.VARIANT_ARCANE_WORKBENCH,
                        TileArcaneWorkbench::new);
            }
        });
    }

    private M403ArcaneWorkbench() {
    }
}
