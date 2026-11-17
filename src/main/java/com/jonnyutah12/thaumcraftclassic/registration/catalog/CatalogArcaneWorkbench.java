package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.menus.ArcaneWorkbenchMenu;
import com.jonnyutah12.thaumcraftclassic.recipes.ArcaneWorkbenchRecipe;
import com.jonnyutah12.thaumcraftclassic.recipes.SimpleRecipeSerializer;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneWorkbench;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;

/**
 * TC-M4-03-s catalog holder: the arcane workbench core.
 *
 * <p>Owns exactly the content-mapping.csv rows this card flips:
 * {@code thaumcraft:tilearcaneworkbench} (kind {@code tile_entity} — the
 * original had no block class, {@code GameRegistry.registerTileEntity(TileArcaneWorkbench.class,
 * "TileArcaneWorkbench")}; 1.21.1 needs a block to bind the entity to, so the
 * block reuses that row id and its same-id block item is the 1.21.1 placement
 * item, docs/porting-decisions.md) and the 89 recipe rows
 * {@code thaumcraft:recipe.<N>.arcane_shaped} / {@code .arcane_shapeless},
 * loaded as the 109 data files under {@code data/thaumcraft/recipe/} whose
 * {@code type} field resolves to {@code thaumcraft:arcane_workbench}.</p>
 *
 * <p>Out of this holder by design: {@code thaumcraft:blockarcanefurnace}
 * (TC-M6-04f). {@code gui_arcane_workbench} landed here with TC-M7-04-m2: the
 * {@link #MENU_ARCANE_WORKBENCH} type below, whose screen is wired in
 * {@code client/ModClientScreens}.</p>
 */
public final class CatalogArcaneWorkbench {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThaumcraftClassic.MOD_ID);

    // --- recipe type + serializer ------------------------------------------

    /** The recipe type the 109 data files name; only TileArcaneWorkbench reads it. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcaneWorkbenchRecipe>> ARCANE_WORKBENCH_TYPE =
            RECIPE_TYPES.register("arcane_workbench",
                    () -> RecipeType.simple(id("arcane_workbench")));

    /** Decodes the {@code research}/{@code aspects}/{@code pattern}+{@code key} or {@code ingredients} JSON. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcaneWorkbenchRecipe>> ARCANE_WORKBENCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("arcane_workbench",
                    () -> new SimpleRecipeSerializer<ArcaneWorkbenchRecipe>(
                            ArcaneWorkbenchRecipe.CODEC, ArcaneWorkbenchRecipe.STREAM_CODEC));

    // --- the workbench block + entity --------------------------------------

    public static final DeferredBlock<BlockArcaneWorkbench> ARCANE_WORKBENCH_BLOCK =
            BLOCKS.register("tilearcaneworkbench", () -> new BlockArcaneWorkbench());

    public static final DeferredItem<BlockItem> ARCANE_WORKBENCH_ITEM =
            ITEMS.register("tilearcaneworkbench",
                    () -> new BlockItem(ARCANE_WORKBENCH_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneWorkbench>> TILE_ARCANE_WORKBENCH =
            BLOCK_ENTITY_TYPES.register("tilearcaneworkbench",
                    () -> BlockEntityType.Builder.of(TileArcaneWorkbench::new,
                            ARCANE_WORKBENCH_BLOCK.get()).build(null));

    // --- menu (content-mapping.csv thaumcraft:gui_arcane_workbench, TC-M7-04-m2) --

    /**
     * {@code gui_arcane_workbench} — named after the csv row it closes. The
     * client-side factory reads the block pos, the same wire shape
     * {@code CatalogFurniture}'s five menu types use.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneWorkbenchMenu>> MENU_ARCANE_WORKBENCH =
            MENUS.register("gui_arcane_workbench", () -> IMenuTypeExtension.create(
                    (final int windowId, final Inventory inv, final RegistryFriendlyByteBuf buf) ->
                            new ArcaneWorkbenchMenu(windowId, inv,
                                    (TileArcaneWorkbench) inv.player.level().getBlockEntity(buf.readBlockPos()))));

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    public static void init(final IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        MENUS.register(bus);
    }

    private CatalogArcaneWorkbench() {
    }
}
