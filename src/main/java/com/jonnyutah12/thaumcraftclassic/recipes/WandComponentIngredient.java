package com.jonnyutah12.thaumcraftclassic.recipes;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWandRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An ingredient that matches one <em>original damage-value variant</em> of an
 * item: same item, and the original's damage value equal to {@link #meta()}
 * (TC-M6-01-s-b; {@code thaumcraft:wand_component} ingredient type).
 *
 * <p>Original: the private {@code ArcaneWandRecipe.checkItemEquals}
 * ({@code common/lib/crafting/ArcaneWandRecipe.java}) compares
 * {@code target.getItem() == input.getItem()} plus
 * {@code target.getItemDamage() == input.getItemDamage()} (with the 32767
 * wildcard and an optional ore-dictionary clause). The wand/sceptre assembly
 * rows need exactly that: two caps on the grid only count as a match when they
 * are the <em>same</em> cap, and "same cap" in 1.7.10 means same damage value.
 * 1.21.1 has no damage-value variants, so the port carries the original value
 * in {@link ModDataComponents#ITEM_META} (D-001, the TC-M6-04a multi-meta
 * convention) and a plain {@code neoforge:components} ingredient cannot express
 * it: {@code DataComponentPredicate} requires the component to be
 * <em>present</em>, while meta-0 variants — the iron cap, the greatwood rod,
 * the wood rod's stick — legitimately carry no component at all, exactly like
 * the original's damage 0.</p>
 *
 * <p>{@link #test(ItemStack)} is therefore the original comparison:
 * the item id resolves (an unregistered id resolves to air and never matches a
 * real stack, which is what keeps a row gated on unported content inert), and
 * the stack's stored meta — absent component reads 0, the vanilla meta 0 —
 * equals {@code meta}.</p>
 */
public final class WandComponentIngredient implements ICustomIngredient {

    public static final MapCodec<WandComponentIngredient> CODEC =
            RecordCodecBuilder.mapCodec(g -> g.group(
                    ResourceLocation.CODEC.fieldOf("item").forGetter(WandComponentIngredient::item),
                    Codec.intRange(0, 1023).optionalFieldOf("meta", 0).forGetter(WandComponentIngredient::meta))
                    .apply(g, WandComponentIngredient::new));

    private final ResourceLocation item;
    private final int meta;

    public WandComponentIngredient(final ResourceLocation item, final int meta) {
        this.item = item;
        this.meta = meta;
    }

    /** The item id whose original damage-value variant this ingredient claims. */
    public ResourceLocation item() {
        return this.item;
    }

    /** The original damage value ({@code ItemStack.getItemDamage()}) required. */
    public int meta() {
        return this.meta;
    }

    @Override
    public boolean test(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final Item expected = BuiltInRegistries.ITEM.get(this.item);
        return stack.getItem() == expected
                && stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0).intValue() == this.meta;
    }

    @Override
    public Stream<ItemStack> getItems() {
        final Item item = BuiltInRegistries.ITEM.get(this.item);
        final ItemStack stack = new ItemStack(item);
        if (this.meta != 0) {
            stack.set(ModDataComponents.ITEM_META.get(), this.meta);
        }
        return Stream.of(stack);
    }

    /** Direct stack testing is required: the match is on a component, not on the item alone. */
    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return CatalogWandRecipes.WAND_COMPONENT_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof WandComponentIngredient that
                && this.meta == that.meta && this.item.equals(that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.item, this.meta);
    }

    @Override
    public String toString() {
        return "wand_component(" + this.item + (this.meta == 0 ? "" : "#" + this.meta) + ")";
    }
}
