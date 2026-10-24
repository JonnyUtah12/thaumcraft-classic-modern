package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe type + serializer for crucible recipes (TC-M4-05). The original had
 * no data recipe type at all — crucible recipes were code objects in
 * {@code ThaumcraftCraftingManager}; the port data-drives them following the
 * sibling infusion pattern ({@link M404RecipeSerializers}), with the JSON
 * shape fixed by the extractor output in {@code data/thaumcraft/recipe/}
 * ({@code thaumcraft:crucible}).
 */
public final class M405CrucibleContent {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.createRecipes(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.createRecipeSerializers(ThaumcraftClassic.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrucibleRecipe>> CRUCIBLE_TYPE =
            RECIPE_TYPES.register("crucible", () -> RecipeType.simple(id("crucible")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crucible",
                    () -> new SimpleRecipeSerializer<>(CrucibleRecipe.CODEC, CrucibleRecipe.STREAM_CODEC));

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    private M405CrucibleContent() {
    }
}
