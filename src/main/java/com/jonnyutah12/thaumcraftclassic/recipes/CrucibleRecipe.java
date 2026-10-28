package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogCrucible;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Crucible (alchemy) recipe (TC-M4-05; content-mapping.csv rows
 * {@code Thaumcraft:recipe.<N>.crucible}, 49 rows / 54 instances).
 *
 * <p>Original: {@code thaumcraft.api.crafting.CrucibleRecipe} — a code object
 * registered through {@code ThaumcraftApi.addCrucibleRecipe} from
 * {@code ConfigRecipes.initializeAlchemyRecipes}. The port loads the same
 * recipes as data (extracted by scripts/extract_crucible_recipes.py into
 * {@code data/thaumcraft/recipe/recipe.<N>.crucible[.<idx>].json}); the
 * crucible matches them against its aspect pool plus the thrown catalyst item
 * directly (the original's {@code ThaumcraftCraftingManager
 * .findMatchingCrucibleRecipe} path), never through the vanilla crafting grid.</p>
 *
 * <p>Matching is the original's rule: the thrown item must match the
 * {@code catalyst} (original {@code itemMatches} / OreDict
 * {@code containsMatch}; carried here by NeoForge ingredients — tags +
 * {@code thaumcraft:item_meta} components, docs/porting-decisions.md D-001),
 * and the crucible's current aspect pool must contain at least every recipe
 * aspect amount (a subset test, not exact-multiset — unlike the infusion
 * altar). Research gating is the original's {@code isResearchComplete} check
 * against the thrower's knowledge (TC-M3-02). {@link #removeMatching} is the
 * original's exact subtraction of the recipe cost from the pool.</p>
 */
public class CrucibleRecipe implements Recipe<CrucibleRecipe.CrucibleInput> {

    /** The crucible state fed to matching: the thrown item, the pool, the thrower. */
    public record CrucibleInput(ItemStack catalyst, AspectList pool,
                                @Nullable ServerPlayer thrower) implements RecipeInput {
        @Override
        public ItemStack getItem(final int index) {
            return this.catalyst;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.catalyst.isEmpty();
        }
    }

    private final String originalKey;
    private final String research;
    private final Ingredient catalyst;
    private final AspectList aspects;
    private final ItemStack output;

    public CrucibleRecipe(final String originalKey, final String research, final Ingredient catalyst,
                          final AspectList aspects, final ItemStack output) {
        this.originalKey = originalKey;
        this.research = research;
        this.catalyst = catalyst;
        this.aspects = aspects;
        this.output = output;
    }

    public String originalKey() {
        return this.originalKey;
    }

    public String research() {
        return this.research;
    }

    public Ingredient catalyst() {
        return this.catalyst;
    }

    public AspectList aspects() {
        return this.aspects.copy();
    }

    /**
     * Original {@code findMatchingCrucibleRecipe} tie-break: the candidate
     * with the most distinct aspect tags wins ({@code recipe.aspects.size()}
     * — distinct keys, not total vis).
     */
    public int aspectKeyCount() {
        return this.aspects.size();
    }

    /** Original {@code ResearchManager.isResearchComplete(username, key)} gate. */
    public boolean researchUnlocked(@Nullable final ServerPlayer thrower) {
        if (this.research.isEmpty()) {
            return true;
        }
        if (thrower == null) {
            return false;
        }
        final PlayerKnowledgeData knowledge = thrower.getData(ModAttachments.PLAYER_KNOWLEDGE);
        return ResearchManager.isResearchComplete(knowledge, this.research);
    }

    /** Original {@code CrucibleRecipe.matches}: catalyst match + pool superset. */
    @Override
    public boolean matches(final CrucibleInput in, final Level level) {
        if (in.catalyst().isEmpty() || in.catalyst().getCount() != 1) {
            // Original copies the stack and forces count 1 before matching.
            return false;
        }
        if (!this.catalyst.test(in.catalyst())) {
            return false;
        }
        for (final Aspect aspect : this.aspects.getAspects()) {
            if (in.pool().getAmount(aspect) < this.aspects.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Original {@code CrucibleRecipe.removeMatching}: subtract exactly the
     * recipe cost from the pool (the pool keeps any surplus aspects).
     */
    public AspectList removeMatching(final AspectList pool) {
        final AspectList out = pool.copy();
        for (final Aspect aspect : this.aspects.getAspects()) {
            out.remove(aspect, this.aspects.getAmount(aspect));
        }
        return out;
    }

    @Override
    public ItemStack assemble(final CrucibleInput in, final net.minecraft.core.HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(final net.minecraft.core.HolderLookup.Provider registries) {
        return this.output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CatalogCrucible.CRUCIBLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CatalogCrucible.CRUCIBLE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    // ------------------------------------------------------------------ codec

    private record AspectAmount(String aspect, int amount) {
        // The sibling infusion recipe's shape: the record owns its Codec, so
        // group() gets Codecs (fieldOf returns a MapCodec, which only groups
        // inside a RecordCodecBuilder of its own).
        public static final Codec<AspectAmount> CODEC =
                RecordCodecBuilder.create(g -> g.group(
                        Codec.STRING.fieldOf("aspect").forGetter(AspectAmount::aspect),
                        Codec.INT.fieldOf("amount").forGetter(AspectAmount::amount))
                        .apply(g, AspectAmount::new));
    }

    private static final Codec<AspectList> ASPECTS =
            AspectAmount.CODEC.listOf()
                    .xmap(entries -> {
                        final AspectList list = new AspectList();
                        for (final AspectAmount aa : entries) {
                            final Aspect aspect = Aspect.getAspect(aa.aspect());
                            if (aspect != null) {
                                list.add(aspect, aa.amount());
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

    public static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(g -> g.group(
            Codec.STRING.optionalFieldOf("original_key", "").forGetter(r -> r.originalKey),
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(r -> r.catalyst),
            ASPECTS.fieldOf("aspects").forGetter(r -> r.aspects),
            ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output))
            .apply(g, CrucibleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
