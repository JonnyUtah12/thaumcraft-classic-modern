package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe type + serializer registration for the infusion altar (TC-M4-04).
 * The JSON {@code type} field resolves to the serializer id, so the ids here
 * are exactly {@code thaumcraft:infusion} and
 * {@code thaumcraft:infusion_enchantment} as emitted by
 * scripts/extract_infusion_recipes.py.
 */
public final class M404RecipeSerializers {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.createRecipes(ThaumcraftClassic.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.createRecipeSerializers(ThaumcraftClassic.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeType<?>, RecipeType<InfusionCraftingRecipe>> INFUSION_TYPE =
            RECIPE_TYPES.register("infusion", () -> RecipeType.simple(id("infusion")));
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeType<?>, RecipeType<InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT_TYPE =
            RECIPE_TYPES.register("infusion_enchantment", () -> RecipeType.simple(id("infusion_enchantment")));

    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InfusionCraftingRecipe>> INFUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("infusion",
                    () -> new SimpleRecipeSerializer<>(InfusionCraftingRecipe.CODEC, InfusionCraftingRecipe.STREAM_CODEC));
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("infusion_enchantment",
                    () -> new SimpleRecipeSerializer<>(InfusionEnchantmentRecipe.CODEC, InfusionEnchantmentRecipe.STREAM_CODEC));

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path);
    }

    private M404RecipeSerializers() {
    }
}
