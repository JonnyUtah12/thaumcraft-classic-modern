package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
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
import java.util.Optional;

/**
 * Infusion altar recipe (TC-M4-04; content-mapping.csv rows
 * {@code Thaumcraft:recipe.<49..111>.infusion}).
 *
 * <p>Original: {@code thaumcraft.api.crafting.InfusionRecipe} — a code object
 * registered through {@code ThaumcraftApi.addInfusionCraftingRecipe} from
 * {@code ConfigRecipes.initializeInfusionRecipes}. The port loads the same
 * 63 recipes as data (extracted by scripts/extract_infusion_recipes.py into
 * {@code data/thaumcraft/recipe/recipe.<N>.infusion.json}); the runic matrix
 * matches them against its pedestals directly (the original's
 * {@code ThaumcraftCraftingManager.findMatchingInfusionRecipe} path), never
 * through the vanilla crafting grid.</p>
 *
 * <p>Matching is the original's exact-multiset rule: the central pedestal item
 * must match {@code input}, every component must be claimed by exactly one
 * pedestal stack, and no pedestal item may remain unclaimed. Research gating is
 * the original's {@code isResearchComplete} check against the player's
 * knowledge (TC-M3-02). The original's oreDictionary fuzziness is carried by
 * NeoForge ingredients (tags + {@code thaumcraft:item_meta} components,
 * docs/porting-decisions.md D-001).</p>
 */
public class InfusionCraftingRecipe implements Recipe<InfusionCraftingRecipe.InfusionInput> {

    /** The altar state fed to matching: central item, pedestal items, requester. */
    public record InfusionInput(ItemStack central, List<ItemStack> pedestals,
                                @Nullable ServerPlayer player) implements RecipeInput {
        @Override
        public ItemStack getItem(final int index) {
            return this.pedestals.get(index);
        }

        @Override
        public int size() {
            return this.pedestals.size();
        }

        @Override
        public boolean isEmpty() {
            return this.pedestals.isEmpty() && this.central.isEmpty();
        }
    }

    private final String originalKey;
    private final String research;
    private final int instability;
    private final AspectList aspects;
    private final Ingredient input;
    private final List<Ingredient> components;
    private final ItemStack output;
    @Nullable
    private final String outputTagLabel;
    @Nullable
    private final Tag outputTagValue;

    // The tag-output pair arrives as mojang's Optional form (see CODEC below);
    // the class keeps the original's plain @Nullable fields so every consumer
    // (TileInfusionMatrix, the recipe's own accessors) sees the same
    // "absent or present" answer it did in the original.
    public InfusionCraftingRecipe(final String originalKey, final String research, final int instability,
                                  final AspectList aspects, final Ingredient input, final List<Ingredient> components,
                                  final ItemStack output, @Nullable final Optional<String> outputTagLabelOpt,
                                  @Nullable final Optional<Tag> outputTagValueOpt) {
        this.originalKey = originalKey;
        this.research = research;
        this.instability = instability;
        this.aspects = aspects;
        this.input = input;
        this.components = List.copyOf(components);
        this.output = output;
        this.outputTagLabel = outputTagLabelOpt == null ? null : outputTagLabelOpt.orElse(null);
        this.outputTagValue = outputTagValueOpt == null ? null : outputTagValueOpt.orElse(null);
    }

    public String originalKey() {
        return this.originalKey;
    }

    public String research() {
        return this.research;
    }

    public int instability() {
        return this.instability;
    }

    public AspectList aspects() {
        return this.aspects.copy();
    }

    public Ingredient input() {
        return this.input;
    }

    public List<Ingredient> components() {
        return this.components;
    }

    /** Null for the tag-output recipes (golem placer, masks) — original Object[] output. */
    @Nullable
    public ItemStack concreteOutput() {
        return this.output.isEmpty() ? null : this.output.copy();
    }

    @Nullable
    public String outputTagLabel() {
        return this.outputTagLabel;
    }

    @Nullable
    public Tag outputTagValue() {
        return this.outputTagValue;
    }

    @Override
    public boolean matches(final InfusionInput in, final Level level) {
        if (in.central().isEmpty() || in.pedestals().isEmpty()) {
            return false;
        }
        if (!this.research.isEmpty()) {
            if (in.player() == null) {
                return false;
            }
            final PlayerKnowledgeData knowledge = in.player().getData(ModAttachments.PLAYER_KNOWLEDGE);
            if (!ResearchManager.isResearchComplete(knowledge, this.research)) {
                return false;
            }
        }
        if (!this.input.test(in.central())) {
            return false;
        }
        final List<ItemStack> remaining = new ArrayList<>(in.pedestals());
        for (final Ingredient component : this.components) {
            boolean claimed = false;
            for (int a = 0; a < remaining.size(); a++) {
                if (component.test(remaining.get(a))) {
                    remaining.remove(a);
                    claimed = true;
                    break;
                }
            }
            if (!claimed) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(final InfusionInput in, final net.minecraft.core.HolderLookup.Provider registries) {
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
    public NonNullList<Ingredient> getIngredients() {
        final NonNullList<Ingredient> all = NonNullList.createWithCapacity(this.components.size() + 1);
        all.add(all.size(), this.input);
        for (final Ingredient component : this.components) {
            all.add(all.size(), component);
        }
        return all;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CatalogInfusion.INFUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CatalogInfusion.INFUSION_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    // ------------------------------------------------------------------ codec

    private record AspectAmount(String aspect, int amount) {
        public static final Codec<AspectAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("aspect").forGetter(AspectAmount::aspect),
                Codec.INT.fieldOf("amount").forGetter(AspectAmount::amount)
        ).apply(instance, AspectAmount::new));
    }

    private static final Codec<AspectList> ASPECTS = AspectAmount.CODEC.listOf()
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
                final java.util.List<AspectAmount> out = new ArrayList<>();
                for (final Aspect aspect : list.getAspects()) {
                    out.add(new AspectAmount(aspect.getTag(), list.getAmount(aspect)));
                }
                return out;
            });

    /**
     * An NBT tag carried as SNBT text: MC 1.21.1 gives no {@code Codec<Tag>} for a bare tag
     * ({@code net.minecraft.nbt.CompoundTag.CODEC} covers compounds only), so the payload
     * round-trips through {@code net.minecraft.nbt.TagParser} and {@code Tag.toString}
     * exactly as {@code net.minecraft.nbt.TagParser.AS_CODEC} does for compounds.
     */
    private static final Codec<Tag> NBT_TAG = Codec.STRING.comapFlatMap(text -> {
        try {
            return DataResult.success(new TagParser(new StringReader(text)).readValue(), Lifecycle.stable());
        } catch (CommandSyntaxException exception) {
            return DataResult.error(exception::getMessage);
        }
    }, tag -> tag.toString());

    // The two tag-output rows (golem placer, masks) carry a String/Tag pair
    // instead of an ItemStack. 1.21.1 has no Codec<Tag> for a bare tag and no
    // null default for either field, so both are read through mojang's
    // no-default optionalFieldOf — which is Codec[java.util.Optional<…>] and
    // hands the constructor Optional.empty() for an absent key; the constructor
    // unwraps to the port's @Nullable fields unchanged, and the getters
    // re-wrap so the pair round-trips as "absent", never as "" / 0.
    public static final MapCodec<InfusionCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(g -> g.group(
            Codec.STRING.optionalFieldOf("original_key", "").forGetter(r -> r.originalKey),
            Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
            Codec.INT.optionalFieldOf("instability", 0).forGetter(r -> r.instability),
            ASPECTS.fieldOf("aspects").forGetter(r -> r.aspects),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(r -> r.input),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(r -> r.components),
            ItemStack.CODEC.optionalFieldOf("output", ItemStack.EMPTY).forGetter(r -> r.output),
            Codec.STRING.optionalFieldOf("output_tag_label")
                    .forGetter(r -> Optional.ofNullable(r.outputTagLabel)),
            NBT_TAG.optionalFieldOf("output_tag_value")
                    .forGetter(r -> Optional.ofNullable(r.outputTagValue)))
            .apply(g, InfusionCraftingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionCraftingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
