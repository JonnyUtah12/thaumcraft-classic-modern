package com.jonnyutah12.thaumcraftclassic.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Arcane workbench recipe (TC-M4-03; content-mapping.csv recipe rows
 * {@code recipe.*.arcane_shaped}/{@code recipe.*.arcane_shapeless}).
 *
 * <p>Original: {@code thaumcraft.api.crafting.ShapedArcaneRecipe} /
 * {@code ShapelessArcaneRecipe} (both implement {@code IArcaneRecipe}), built
 * by {@code ConfigRecipes.initializeArcaneRecipes} and matched by
 * {@code ThaumcraftCraftingManager.findMatchingArcaneRecipe}. The matching
 * rules are the vanilla shaped/shapeless rules verbatim (offset scan with
 * mirroring for shaped, multiset cover for shapeless); the arcane additions
 * are the per-aspect vis price ({@link #visCost()}) and the research gate
 * ({@link #research()}).</p>
 *
 * <p>The research gate is NOT evaluated in {@link #matches}: 1.21.1 recipe
 * matching has no player parameter (the original checked
 * {@code IArcaneRecipe.canCraft(player)} inside its own manager loop). The
 * gate is applied by {@code ArcaneWorkbenchMenu} at result-resolution time,
 * server-side, against the player's knowledge — same net behaviour, and the
 * only place a per-player check can live in this platform (D-006).</p>
 *
 * <p>Vis costs are whole vis (the original's units); payment scales them by
 * 100 into the centi-vis pool via {@code WandVis.consumeAllVisCrafting}.</p>
 */
public class ArcaneWorkbenchRecipe implements Recipe<CraftingInput> {

    /** Aspect price entry: tag + whole-vis amount (original AspectList units). */
    public record AspectPrice(Aspect aspect, int amount) {
    }

    private static final Codec<AspectPrice> PRICE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(Aspect::getAspect, Aspect::getTag).fieldOf("aspect").forGetter(AspectPrice::aspect),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(AspectPrice::amount)
    ).apply(i, AspectPrice::new));

    private final boolean shaped;
    private final int width;
    private final int height;
    /** Shaped: row-major width*height; shapeless: unordered multiset. */
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final String research;
    private final List<AspectPrice> visCost;

    public ArcaneWorkbenchRecipe(final boolean shaped, final int width, final int height,
                                 final NonNullList<Ingredient> ingredients, final ItemStack result,
                                 final String research, final List<AspectPrice> visCost) {
        this.shaped = shaped;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.research = research;
        this.visCost = List.copyOf(visCost);
    }

    /** The research key gating this recipe (original IArcaneRecipe.canCraft). */
    public String research() {
        return research;
    }

    /** The vis price in whole vis per aspect (original IArcaneRecipe.getAspects). */
    public AspectList visCost() {
        final AspectList list = new AspectList();
        for (final AspectPrice price : visCost) {
            list.add(price.aspect(), price.amount());
        }
        return list;
    }

    public boolean isShaped() {
        return shaped;
    }

    @Override
    public boolean matches(final CraftingInput input, final net.minecraft.world.level.Level level) {
        if (shaped) {
            return matchesShaped(input);
        }
        return matchesShapeless(input);
    }

    /** Vanilla ShapedRecipe.matches, ported: offset scan + horizontal mirror. */
    private boolean matchesShaped(final CraftingInput input) {
        if (input.width() < this.width || input.height() < this.height) {
            return false;
        }
        for (int x = 0; x <= input.width() - this.width; ++x) {
            for (int y = 0; y <= input.height() - this.height; ++y) {
                if (fits(input, x, y, false)) {
                    return true;
                }
                if (fits(input, x, y, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fits(final CraftingInput input, final int offsetX, final int offsetY, final boolean mirror) {
        for (int x = 0; x < input.width(); ++x) {
            for (int y = 0; y < input.height(); ++y) {
                final int px = x - offsetX;
                final int py = y - offsetY;
                final int recipeX;
                if (px >= 0 && px < this.width && py >= 0 && py < this.height) {
                    recipeX = mirror ? this.width - 1 - px : px;
                } else {
                    recipeX = -1;
                }
                final ItemStack grid = input.getItem(x + y * input.width());
                final Ingredient ingredient = recipeX < 0
                        ? Ingredient.EMPTY
                        : this.ingredients.get(recipeX + py * this.width);
                if (!ingredient.test(grid)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Vanilla ShapelessRecipe.matches, ported: every grid stack consumed once. */
    private boolean matchesShapeless(final CraftingInput input) {
        final List<Ingredient> remaining = new ArrayList<>();
        final List<Ingredient> remaining = new ArrayList<>();
        for (final Ingredient ingredient : this.ingredients) {
            if (!ingredient.isEmpty()) {
                remaining.add(ingredient);
            }
        }
        int used = 0;
        outer:
        for (final ItemStack grid : input.items()) {
            if (grid.isEmpty()) {
                continue;
            }
            used++;
            for (final java.util.Iterator<Ingredient> it = remaining.iterator(); it.hasNext();) {
                if (it.next().test(grid)) {
                    it.remove();
                    continue outer;
                }
            }
            return false;
        }
        return used == this.ingredients.size() && remaining.isEmpty();
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return shaped ? width >= this.width && height >= this.height : width * height >= this.ingredients.size();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public ItemStack assemble(final CraftingInput input,
                             final net.minecraft.core.HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public ItemStack getResultItem(final net.minecraft.core.HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return M403RecipeSerializers.ARCANE_WORKBENCH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return M403RecipeSerializers.ARCANE_WORKBENCH_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        // The original's arcane recipes never appeared in the recipe book.
        return true;
    }

    // ------------------------------------------------------------------ codecs

    /**
     * JSON shape exactly as scripts/extract_arcane_recipes.py emits it:
     * {@code research} + {@code aspects} always; either {@code pattern}/{@code key}
     * (shaped) or {@code ingredients} (shapeless); vanilla {@code result}.
     */
    public static final MapCodec<ArcaneWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("research").forGetter(r -> r.research),
            PRICE.listOf().fieldOf("aspects").forGetter(r -> r.visCost),
            Codec.STRING.listOf().optionalFieldOf("pattern"),
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC).optionalFieldOf("key"),
            Ingredient.CODEC.listOf().optionalFieldOf("ingredients"),
            ItemStack.CODEC.fieldOf("result")
    ).apply(i, ArcaneWorkbenchRecipe::fromFields));

    private static ArcaneWorkbenchRecipe fromFields(final String research, final List<AspectPrice> prices,
                                                    final List<String> pattern,
                                                    final java.util.Map<String, Ingredient> key,
                                                    final List<Ingredient> shapeless,
                                                    final ItemStack result) {
        if (pattern.isPresent() == shapeless.isPresent()) {
            throw new IllegalArgumentException(
                    "arcane_workbench recipe needs exactly one of pattern/key or ingredients");
        }
        if (pattern.isPresent()) {
            final List<String> rows = pattern.get();
            int width = 0;
            for (final String row : rows) {
                width = Math.max(width, row.length());
            }
            final NonNullList<Ingredient> grid = NonNullList.create();
            for (int y = 0; y < rows.size(); y++) {
                final String row = rows.get(y);
                for (int x = 0; x < width; x++) {
                    final char c = x < row.length() ? row.charAt(x) : ' ';
                    if (c == ' ') {
                        grid.add(Ingredient.EMPTY);
                    } else {
                        final Ingredient ingredient = key.get().get(String.valueOf(c));
                        if (ingredient == null) {
                            throw new IllegalArgumentException("Key '" + c + "' is not defined in the recipe key");
                        }
                        grid.add(ingredient);
                    }
                }
            }
            return new ArcaneWorkbenchRecipe(true, width, rows.size(), grid, result, research, prices);
        }
        final NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(shapeless.get());
        return new ArcaneWorkbenchRecipe(false, 0, 0, list, result, research, prices);
    }

    public static final class Serializer implements RecipeSerializer<ArcaneWorkbenchRecipe> {
        @Override
        public MapCodec<ArcaneWorkbenchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ArcaneWorkbenchRecipe> streamCodec() {
            return STREAM;
        }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, ArcaneWorkbenchRecipe> STREAM =
            StreamCodec.of(ArcaneWorkbenchRecipe::toBuffer, ArcaneWorkbenchRecipe::fromBuffer);

    private static void toBuffer(final RegistryFriendlyByteBuf buf, final ArcaneWorkbenchRecipe recipe) {
        buf.writeBoolean(recipe.shaped);
        buf.writeVarInt(recipe.width);
        buf.writeVarInt(recipe.height);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredients);
        ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        buf.writeUtf(recipe.research);
        buf.writeCollection(recipe.visCost, (b, price) -> {
            b.writeUtf(price.aspect().getTag());
            b.writeVarInt(price.amount());
        });
    }

    private static ArcaneWorkbenchRecipe fromBuffer(final RegistryFriendlyByteBuf buf) {
        final boolean shaped = buf.readBoolean();
        final int width = buf.readVarInt();
        final int height = buf.readVarInt();
        final NonNullList<Ingredient> ingredients = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        final ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
        final String research = buf.readUtf();
        final List<AspectPrice> prices = buf.readList(b ->
                new AspectPrice(Aspect.getAspect(b.readUtf()), b.readVarInt()));
        return new ArcaneWorkbenchRecipe(shaped, width, height, ingredients, result, research, prices);
    }
}
