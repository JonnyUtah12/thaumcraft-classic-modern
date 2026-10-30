package com.jonnyutah12.thaumcraftclassic.items.wands;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The wand cap item (TC-M6-01; content-mapping.csv row {@code thaumcraft:wandcap},
 * original {@code ItemWandCap}): the 1.7.10 damage-value variants iron 0,
 * gold 1, thaumium 2, copper 3, silver 4, inert silver 5, inert thaumium 6,
 * void 7, inert void 8 ({@code common/items/wands/ItemWandCap.java:30-38}).
 *
 * <p>1.21.1 has no damage-value variants, so the meta travels on the stack in
 * {@code thaumcraft:item_meta} (the TC-M6-04a multi-meta convention,
 * {@link ModDataComponents#ITEM_META}); {@link #stack(int)} builds a variant.
 * The original's per-meta names ({@code unlocalizedName + "." + damage},
 * {@code func_77667_c}) are per-meta lang keys resolved by TC-M7-01, in the
 * port's {@code item.thaumcraft.wandcap.<meta>} shape (matching
 * {@code MetaBaubleItem#perMetaName}).</p>
 *
 * <p>The six registerable caps (metas 0-4 + 7) are the
 * {@code wand_component} entries of {@code wands.WandComponents}; metas 5, 6
 * and 8 are the "inert" caps the original never registered as components
 * (they exist only as crafting leftovers) — the original's
 * {@code WandCap.caps} map has no entry for them.</p>
 *
 * <p>Original item properties: max stack 64, subtypes, no durability, TC tab
 * ({@code ItemWandCap.java:23-27}).</p>
 */
public class ItemWandCap extends Item {

    public static final int META_IRON = 0;
    public static final int META_GOLD = 1;
    public static final int META_THAUMIUM = 2;
    public static final int META_COPPER = 3;
    public static final int META_SILVER = 4;
    public static final int META_SILVER_INERT = 5;
    public static final int META_THAUMIUM_INERT = 6;
    public static final int META_VOID = 7;
    public static final int META_VOID_INERT = 8;
    public static final int META_COUNT = 9;

    public ItemWandCap(final Properties props) {
        super(props);
    }

    /** A stack of one original-damage-value variant, the original {@code new ItemStack(item, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        if (meta < 0 || meta >= META_COUNT) {
            throw new IllegalArgumentException("ItemWandCap meta out of range: " + meta);
        }
        final ItemStack stack = new ItemStack(com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands.WAND_CAP.get());
        if (meta != 0) {
            stack.set(ModDataComponents.ITEM_META.get(), meta);
        }
        return stack;
    }

    /** The stack's original damage value; an absent component reads 0 (vanilla meta 0). */
    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }
}
