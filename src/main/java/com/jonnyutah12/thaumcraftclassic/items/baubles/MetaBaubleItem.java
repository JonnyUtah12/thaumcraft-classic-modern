package com.jonnyutah12.thaumcraftclassic.items.baubles;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Shared base for the TC-M8-02 bauble items: original damage-value variants
 * carried in the {@code thaumcraft:item_meta} component (the TC-M6-04a
 * multi-meta convention, docs/architecture/registration.md) and the original's
 * per-meta display-name key shape ({@code func_77667_c} =
 * {@code unlocalizedName + "." + meta}, {@code common/items/baubles/
 * ItemBaubleBlanks.java:44-46} and siblings) — resolved per-meta lang keys
 * arrive with TC-M7-01.
 */
public abstract class MetaBaubleItem extends Item implements AccessoryItem {

    protected MetaBaubleItem(final Properties props) {
        super(props);
    }

    /** The stack's original damage value; absent component reads 0 (vanilla meta 0). */
    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /** Build a variant stack — the original's {@code new ItemStack(item, 1, meta)}. */
    protected static ItemStack metaStack(final Item item, final int meta) {
        final ItemStack stack = new ItemStack(item);
        if (meta != 0) {
            stack.set(ModDataComponents.ITEM_META.get(), meta);
        }
        if (item instanceof MetaBaubleItem bauble) {
            final Rarity rarity = bauble.rarityForMeta(meta);
            if (rarity != null) {
                // 1.21.1 moved per-item rarity to a data component
                // (Item.Properties.rarity sets the default; per-variant
                // overrides are written here — the original computed rarity
                // from the damage value, e.g. ItemRingRunic.java:33).
                stack.set(DataComponents.RARITY, rarity);
            }
        }
        return stack;
    }

    /**
     * Per-variant rarity override, null = keep the registered default. The
     * original's {@code func_77613_e} per-meta rarity tables live in the
     * concrete classes.
     */
    protected Rarity rarityForMeta(final int meta) {
        return null;
    }

    /**
     * The original's per-meta name key ({@code item.ItemBaubleBlanks.<meta>.name}
     * etc.), lowercased per D-001: {@code item.thaumcraft.<path>.<meta>}.
     * Multi-meta classes call this from their {@code getName} override;
     * single-variant items keep the plain {@code item.<ns>.<path>} key, as
     * the original did.
     */
    protected Component perMetaName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath()
                + "." + storedMeta(stack));
    }
}
