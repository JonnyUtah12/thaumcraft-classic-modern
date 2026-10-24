package com.jonnyutah12.thaumcraftclassic.recipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Plain {@link RecipeSerializer} pair holder. NeoForge 21.1.259 has no
 * {@code RecipeSerializer.of}/{@code simple} factory (verified against the
 * compiled jar: the interface only declares {@code codec()}/{@code streamCodec()}),
 * so the mod's data-driven recipe serializers build through this holder
 * (TC-M4-04 sibling fix, TC-M4-05).
 */
public record SimpleRecipeSerializer<T extends Recipe<?>>(
        MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec)
        implements RecipeSerializer<T> {
}
