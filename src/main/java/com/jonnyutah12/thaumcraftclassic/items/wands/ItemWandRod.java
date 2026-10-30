package com.jonnyutah12.thaumcraftclassic.items.wands;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogWands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The wand rod / staff core item (TC-M6-01; content-mapping.csv row
 * {@code thaumcraft:wandrod}, original {@code ItemWandRod}): the 1.7.10
 * damage-value variants of {@code common/items/wands/ItemWandRod.java:32-58}
 * —
 * <pre>
 * metas  0-7    wand rods : greatwood 0, obsidian 1, silverwood 2, ice 3,
 *                           quartz 4, reed 5, blaze 6, bone 7
 * metas 50-57   staff cores: greatwood 50, obsidian 51, silverwood 52, ice 53,
 *                           quartz 54, reed 55, blaze 56, bone 57
 * meta  100     staff core of the primal
 * </pre>
 *
 * <p>1.21.1 has no damage-value variants, so the meta travels on the stack in
 * {@code thaumcraft:item_meta} (see {@link ItemWandCap}); the original's
 * per-meta names are per-meta lang keys resolved by TC-M7-01
 * ({@code item.thaumcraft.wandrod.<meta>}).</p>
 *
 * <p>The original's plain wood rod is <em>not</em> a variant of this item: it
 * was registered on {@code minecraft:stick} ({@code ConfigItems.java:291}), so
 * {@code WandComponents} carries that rod on the vanilla stick and this item
 * has no meta for it.</p>
 *
 * <p>Original item properties: max stack 64, subtypes, no durability, TC tab
 * ({@code ItemWandRod.java:17-21}).</p>
 */
public class ItemWandRod extends Item {

    /** The primal staff core's original damage value (the original's sentinel for "primal"). */
    public static final int META_PRIMAL_STAFF = 100;

    /**
     * The sparse original damage-value domain of this item (the original's
     * {@code func_150895_a} list): 0-7 wand rods, 50-57 staff cores, 100 the
     * primal staff core. Declared in the shape the localization audit reads
     * ({@code LocalizationGameTest.metasOf} -> {@code VALID_METAS}), so every
     * variant's per-meta lang key is required rather than assumed.
     */
    public static final java.util.Set<Integer> VALID_METAS = java.util.Set.of(
            0, 1, 2, 3, 4, 5, 6, 7,
            50, 51, 52, 53, 54, 55, 56, 57,
            META_PRIMAL_STAFF);

    public ItemWandRod(final Properties props) {
        super(props);
    }

    /** Whether a damage value is one the original registered on this item. */
    public static boolean isValidMeta(final int meta) {
        return VALID_METAS.contains(meta);
    }

    /** A stack of one original-damage-value variant, the original {@code new ItemStack(item, 1, meta)}. */
    public static ItemStack stack(final int meta) {
        if (!isValidMeta(meta)) {
            throw new IllegalArgumentException("ItemWandRod meta is not an original wand rod variant: " + meta);
        }
        final ItemStack stack = new ItemStack(CatalogWands.WAND_ROD.get());
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
