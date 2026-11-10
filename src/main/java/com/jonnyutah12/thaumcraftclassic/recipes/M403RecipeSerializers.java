package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe type + serializer for the arcane workbench (TC-M4-03).
 *
 * <p>Original: no registration existed — TC4 hard-coded its recipe manager
 * loops. 1.21.1 data-driven recipes need a registered type; the id follows
 * the extraction naming (docs/arcane-recipe-inventory.json, recipe JSON
 * {@code "type": "thaumcraft:arcane_workbench"}). Same pattern as
 * {@link M404RecipeSerializers} (TC-M4-04).</p>
 */
public final class M403RecipeSerializers {

    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ThaumcraftClassic.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThaumcraftClassic.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcaneWorkbenchRecipe>> ARCANE_WORKBENCH_TYPE =
            RECIPE_TYPES.register("arcane_workbench",
                    () -> RecipeType.simple(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    ThaumcraftClassic.MOD_ID, "arcane_workbench")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcaneWorkbenchRecipe>>
            ARCANE_WORKBENCH_SERIALIZER = RECIPE_SERIALIZERS.register("arcane_workbench",
            ArcaneWorkbenchRecipe.Serializer::new);

    public static void register(final net.neoforged.bus.api.IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    private M403RecipeSerializers() {
    }
}
