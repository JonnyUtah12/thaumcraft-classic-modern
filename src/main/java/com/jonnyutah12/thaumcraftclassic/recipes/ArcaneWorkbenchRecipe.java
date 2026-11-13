package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Arcane workbench recipe (TC-M4-03-s; content-mapping.csv rows
 * {@code Thaumcraft:recipe.<N>.arcane_shaped} / {@code .arcane_shapeless}).
 *
 * <p>Original: {@code thaumcraft.api.crafting.IArcaneRecipe} realized by
 * {@code ShapedArcaneRecipe} / {@code ShapelessArcaneRecipe}, built as objects
 * by {@code ConfigRecipes.initializeArcaneRecipes} and held in
 * {@code ThaumcraftApi.getCraftingRecipes()}, which
 * {@code ThaumcraftCraftingManager.findMatchingArcaneRecipe} walks in
 * registration order. The port loads the same 89 recipes (109 instances,
 * docs/arcane-recipe-inventory.json) as data under
 * {@code data/thaumcraft/recipe/} with {@code "type": "thaumcraft:arcane_workbench"};
 * only {@link TileArcaneWorkbench} ever runs this match, never the vanilla
 * crafting grid.</p>
 *
 * <p>The match rule is the original verbatim: the research gate
 * ({@code ShapedArcaneRecipe.java:130} — {@code ThaumcraftApiHelper.isResearchComplete}
 * over the requester's knowledge, TC-M3-02) runs first, then the shaped kind
 * scans the 3x3 grid at every offset with the horizontal mirror variant
 * ({@code checkMatch}, {@code mirrored} defaulting to true as in the original
 * constructor) and the shapeless kind claims every grid item by exactly one
 * ingredient and requires nothing left over ({@code ShapelessArcaneRecipe.java:74}).
 * Item equality is the original's {@code checkItemEquals} (class + oredict tags +
 * damage meta) expressed as a NeoForge {@link Ingredient}: items, item tags and
 * {@code thaumcraft:item_meta} components (D-001).</p>
 *
 * <p>TC-M6-01-s-b adds the optional additive {@code research_all} list to the
 * codec and to {@link #matches}: the 215 enumerated wand/sceptre assembly rows
 * (the port of the two dynamic {@code IArcaneRecipe}s the original registered
 * on {@code ThaumcraftApi.getCraftingRecipes()}) gate on the cap's research,
 * the rod's research and, for a sceptre, {@code SCEPTRE} — three keys a single
 * {@code research} string cannot express. It is purely additive: every key must
 * be complete, the field defaults to empty, and the 109 shipped
 * {@code recipe.<N>.arcane_*} rows keep using {@code research} alone.</p>
 */
public final class ArcaneWorkbenchRecipe implements Recipe<ArcaneWorkbenchRecipe.WorkbenchInput> {

    /** The workbench state handed to {@link #matches}: the 3x3 grid + requester. */
    public record WorkbenchInput(List<ItemStack> grid, @Nullable Player player) implements RecipeInput {

        @Override
        public ItemStack getItem(final int index) {
            return this.grid.get(index);
        }

        @Override
        public int size() {
            return this.grid.size();
        }

        @Override
        public boolean isEmpty() {
            for (final ItemStack stack : this.grid) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private final String research;
    private final List<String> researchAll;
    private final AspectList aspects;
    private final List<String> pattern;
    private final Map<String, Ingredient> key;
    private final List<Ingredient> ingredients;
    private final ItemStack output;
    private boolean mirrored = true;

    public ArcaneWorkbenchRecipe(final String research, final List<String> researchAll,
                                final AspectList aspects, final List<String> pattern,
                                final Map<String, Ingredient> key, final List<Ingredient> ingredients,
                                final ItemStack output) {
        this.research = research;
        this.researchAll = List.copyOf(researchAll);
        this.aspects = aspects.copy();
        this.pattern = List.copyOf(pattern);
        this.key = Map.copyOf(key);
        this.ingredients = List.copyOf(ingredients);
        this.output = output;
        this.mirrored = !this.pattern.isEmpty();
        if (this.pattern.isEmpty() && this.ingredients.isEmpty()) {
            throw new IllegalArgumentException("an arcane recipe needs either a pattern or ingredients");
        }
        for (final String row : this.pattern) {
            if (row.length() > this.width() || this.height() == 0) {
                throw new IllegalArgumentException("arcane recipe pattern rows must be rectangular");
            }
        }
    }

    public String research() {
        return this.research;
    }

    /** The additive multi-key gate (TC-M6-01-s-b); empty on every shipped row. */
    public List<String> researchAll() {
        return this.researchAll;
    }

    public AspectList aspects() {
        return this.aspects.copy();
    }

    public List<String> pattern() {
        return this.pattern;
    }

    public Map<String, Ingredient> key() {
        return this.key;
    }

    public List<Ingredient> ingredients() {
        return this.ingredients;
    }

    public ItemStack output() {
        return this.output;
    }

    public int width() {
        return this.pattern.isEmpty() ? 0 : this.pattern.get(0).length();
    }

    public int height() {
        return this.pattern.size();
    }

    /** Original {@code ShapedArcaneRecipe.setMirrored}; the port keeps the original's true default. */
    public void setMirrored(final boolean mirror) {
        this.mirrored = mirror;
    }

    @Override
    public boolean matches(final WorkbenchInput in, final Level level) {
        if (!this.research.isEmpty() || !this.researchAll.isEmpty()) {
            // Original: ThaumcraftApiHelper.isResearchComplete(player.name, research).
            if (in.player() == null) {
                return false;
            }
            final PlayerKnowledgeData knowledge = in.player().getData(ModAttachments.PLAYER_KNOWLEDGE);
            if (!this.research.isEmpty() && !ResearchManager.isResearchComplete(knowledge, this.research)) {
                return false;
            }
            // TC-M6-01-s-b: the wand/sceptre assembly rows are the port of the
            // original's two dynamic IArcaneRecipes, whose matches() gates on the
            // cap's research AND the rod's research (ArcaneWandRecipe.checkMatch)
            // plus, for the sceptre, ItemWandCasting.RESEARCH_SCEPTRE — three keys
            // the single `research` field cannot carry. `research_all` is additive:
            // every key must be complete, and the 109 shipped rows leave it empty.
            for (final String key : this.researchAll) {
                if (!ResearchManager.isResearchComplete(knowledge, key)) {
                    return false;
                }
            }
        }
        if (in.size() != 9 || in.isEmpty()) {
            return false;
        }
        return this.pattern.isEmpty() ? matchesShapeless(in) : matchesShaped(in);
    }

    private boolean matchesShaped(final WorkbenchInput in) {
        for (int startY = 0; startY <= 3 - this.height(); startY++) {
            for (int startX = 0; startX <= 3 - this.width(); startX++) {
                if (checkMatch(in, startX, startY, false) || (this.mirrored && checkMatch(in, startX, startY, true))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Original {@code ShapedArcaneRecipe.checkMatch}: every cell must agree with the pattern. */
    private boolean checkMatch(final WorkbenchInput in, final int startX, final int startY, final boolean mirror) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                final int subX = x - startX;
                final int subY = y - startY;
                Ingredient target = null;
                if (subX >= 0 && subY >= 0 && subY < this.height() && subX < this.pattern.get(subY).length()) {
                    final String row = this.pattern.get(subY);
                    final char symbol = mirror
                            ? row.charAt(row.length() - subX - 1)
                            : row.charAt(subX);
                    if (symbol != ' ') {
                        target = this.key.get(String.valueOf(symbol));
                        if (target == null) {
                            throw new IllegalArgumentException("pattern symbol " + symbol + " has no key entry");
                        }
                    }
                }
                final ItemStack slot = in.getItem(x + y * 3);
                if (target == null) {
                    if (!slot.isEmpty()) {
                        return false;
                    }
                } else if (!target.test(slot)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Original {@code ShapelessArcaneRecipe.matches}: claim each grid item, nothing may remain. */
    private boolean matchesShapeless(final WorkbenchInput in) {
        final List<Ingredient> required = new ArrayList<>(this.ingredients);
        for (int x = 0; x < 9; x++) {
            final ItemStack slot = in.getItem(x);
            if (!slot.isEmpty()) {
                boolean claimed = false;
                for (int a = 0; a < required.size(); a++) {
                    if (required.get(a).test(slot)) {
                        required.remove(a);
                        claimed = true;
                        break;
                    }
                }
                if (!claimed) {
                    return false;
                }
            }
        }
        return required.isEmpty();
    }

    @Override
    public ItemStack assemble(final WorkbenchInput in, final HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return this.pattern.isEmpty() || width >= this.width() && height >= this.height();
    }

    @Override
    public ItemStack getResultItem(final HolderLookup.Provider registries) {
        return this.output;
    }

    @Override
    public RecipeType<?> getType() {
        return CatalogArcaneWorkbench.ARCANE_WORKBENCH_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CatalogArcaneWorkbench.ARCANE_WORKBENCH_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    // ------------------------------------------------------------------ codec

    public record AspectAmount(String aspect, int amount) {
        public static final Codec<AspectAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("aspect").forGetter(AspectAmount::aspect),
                Codec.INT.fieldOf("amount").forGetter(AspectAmount::amount)
        ).apply(instance, AspectAmount::new));
    }

    /** The original {@code initArcaneRecipeAspect} list format: [{key, amount}] in file order. */
    public static final Codec<AspectList> ASPECTS = AspectAmount.CODEC.listOf()
            .xmap(entries -> {
                final AspectList list = new AspectList();
                for (final AspectAmount entry : entries) {
                    final Aspect aspect = Aspect.getAspect(entry.aspect());
                    if (aspect != null) {
                        list.add(aspect, entry.amount());
                    }
                }
                return list;
            }, list -> {
                final List<AspectAmount> out = new ArrayList<>();
                for (final Aspect aspect : list.getAspects()) {
                    out.add(new AspectAmount(aspect.getTag(), list.getAmount(aspect)));
                }
                return out;
            });

    public static final MapCodec<ArcaneWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(g -> g.group(
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Codec.list(Codec.STRING).optionalFieldOf("research_all", List.of()).forGetter(r -> r.researchAll),
            ASPECTS.fieldOf("aspects").forGetter(r -> r.aspects),
            Codec.list(Codec.STRING).optionalFieldOf("pattern", List.of()).forGetter(r -> r.pattern),
            ExtraCodecs.strictUnboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY).optionalFieldOf("key", Map.of()).forGetter(r -> r.key),
            Ingredient.LIST_CODEC.optionalFieldOf("ingredients", List.of()).forGetter(r -> r.ingredients),
            ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output))
            .apply(g, ArcaneWorkbenchRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneWorkbenchRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    private ArcaneWorkbenchRecipe() {
        // Generics-erasure marker for the Recipe<Input> type-parameter on this class;
        // the compiler strips this constructor in the erased-generic jar, so the
        // neutral defaults below exist only to satisfy definite-assignment checks.
        this.research = "";
        this.researchAll = List.of();
        this.aspects = new AspectList();
        this.pattern = List.of();
        this.key = Map.of();
        this.ingredients = List.of();
        this.output = ItemStack.EMPTY;
    }
}
