package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Infusion enchantment recipe (TC-M4-04; content-mapping.csv rows
 * {@code Thaumcraft:recipe.<201..224>.infusion_enchantment}).
 *
 * <p>Original: {@code thaumcraft.api.crafting.InfusionEnchantmentRecipe}
 * registered through {@code ThaumcraftApi.addInfusionEnchantmentRecipe}. The
 * central pedestal item gains one level of the target enchantment; the essentia
 * cost scales with the levels already on the item ({@code getEssentiaMod}), the
 * instability with the total enchantment level ({@code calcInstability}), and
 * the XP drain with the target level ({@code calcXP}). All three formulas are
 * ported unchanged.</p>
 *
 * <p>The original referenced its own enchantments through
 * {@code Enchantment.field_77331_b[ThaumcraftApi.enchantRepair]} indirection;
 * the port references them by id ({@code thaumcraft:ench_repair},
 * {@code thaumcraft:ench_haste} — the content-mapping.csv contract,
 * registered as datapack payloads by TC-M6-05). Vanilla-referenced recipes
 * resolve directly.</p>
 */
public class InfusionEnchantmentRecipe implements Recipe<InfusionCraftingRecipe.InfusionInput> {

    private final String originalKey;
    private final String research;
    private final Holder<Enchantment> enchantment;
    private final int instability;
    private final AspectList aspects;
    private final List<Ingredient> components;

    public InfusionEnchantmentRecipe(final String originalKey, final String research,
                                     final Holder<Enchantment> enchantment, final int instability,
                                     final AspectList aspects, final List<Ingredient> components) {
        this.originalKey = originalKey;
        this.research = research;
        this.enchantment = enchantment;
        this.instability = instability;
        this.aspects = aspects;
        this.components = List.copyOf(components);
    }

    public String originalKey() {
        return this.originalKey;
    }

    public String research() {
        return this.research;
    }

    public Holder<Enchantment> enchantment() {
        return this.enchantment;
    }

    public AspectList aspects() {
        return this.aspects.copy();
    }

    public List<Ingredient> components() {
        return this.components;
    }

    /** Original {@code recipeXP = max(1, enchant.getMinCost(1) / 3)}. */
    public int calcBaseXp() {
        return Math.max(1, this.enchantment.value().getMinCost(1) / 3);
    }

    /** Original {@code calcInstability}: total enchantment levels / 2 + base. */
    public int calcInstability(final ItemStack central) {
        int i = 0;
        for (final ItemEnchantments.Entry entry : central.getEnchantments().entrySet()) {
            i += entry.level();
        }
        return i / 2 + this.instability;
    }

    /** Original {@code calcXP}: base * (1 + current level of the target). */
    public int calcXP(final ItemStack central) {
        return this.calcBaseXp() * (1 + EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, central));
    }

    /** Original {@code getEssentiaMod}: target level + 0.1 per other level. */
    public float getEssentiaMod(final ItemStack central) {
        float mod = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, central);
        for (final ItemEnchantments.Entry entry : central.getEnchantments().entrySet()) {
            if (entry.enchantment() != this.enchantment) {
                mod += entry.level() * 0.1F;
            }
        }
        return mod;
    }

    @Override
    public boolean matches(final InfusionCraftingRecipe.InfusionInput in, final Level level) {
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
        final ItemStack central = in.central();
        // Original: enchantment.canEnchant(central) && item.isEnchantable(central)
        if (!central.isEnchantable() || !this.enchantment.value().canEnchant(central)) {
            return false;
        }
        // Original: max-level and conflict checks against the item's enchantments
        for (final ItemEnchantments.Entry entry : central.getEnchantments().entrySet()) {
            if (entry.enchantment() == this.enchantment) {
                if (entry.level() >= this.enchantment.value().getMaxLevel()) {
                    return false;
                }
            } else if (this.enchantment.value().conflicts().test(entry.enchantment().value())
                    && entry.enchantment().value().conflicts().test(this.enchantment.value())) {
                return false;
            }
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
    public ItemStack assemble(final InfusionCraftingRecipe.InfusionInput in, final HolderLookup.Provider registries) {
        return in.central().copy();
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(final HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public List<Ingredient> getIngredients() {
        return this.components;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return M404RecipeSerializers.INFUSION_ENCHANTMENT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return M404RecipeSerializers.INFUSION_ENCHANTMENT_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    // ------------------------------------------------------------------ codec

    private record AspectAmount(String aspect, int amount) {
    }

    private static final Codec<AspectList> ASPECTS =
            Codec.list(RecordCodecBuilder.create(g -> g.group(
                            Codec.STRING.fieldOf("aspect"),
                            Codec.INT.fieldOf("amount"))
                    .apply(g, AspectAmount::new)))
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

    public static final MapCodec<InfusionEnchantmentRecipe> CODEC =
            RecordCodecBuilder.mapCodec(g -> g.group(
                    Codec.STRING.optionalFieldOf("original_key", "").forGetter(r -> r.originalKey),
                    Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research),
                    Enchantment.CODEC.fieldOf("enchantment").forGetter(r -> r.enchantment),
                    Codec.INT.optionalFieldOf("instability", 0).forGetter(r -> r.instability),
                    ASPECTS.fieldOf("aspects").forGetter(r -> r.aspects),
                    Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(r -> r.components))
                    .apply(g, InfusionEnchantmentRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
