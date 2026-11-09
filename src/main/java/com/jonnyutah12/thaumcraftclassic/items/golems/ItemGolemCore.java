package com.jonnyutah12.thaumcraftclassic.items.golems;

import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Golem animation core (content-mapping.csv {@code thaumcraft:itemgolemcore}
 * "Golem Animation Core") — the original's
 * {@code thaumcraft.common.entities.golems.ItemGolemCore}.
 *
 * <p>The original's {@code icon[12]} array plus its separate blank icon gives
 * the damage-value domain: metas 0..11 are the twelve cores and meta 100 is
 * the blank core ({@code golem_core_blank}); {@code func_150895_a} lists meta
 * 100 first and then 0..11. The three static predicates below are the
 * original's verbatim switch sets and are the only behaviour this card ports —
 * they are pure, so they are pinned by unit tests rather than by a world. The
 * core's <em>effects</em> (the task switch in {@code EntityGolemBase.setupGolem}
 * and {@code GolemHelper.getItemsNeeded}) are TC-M6-08-s-b.</p>
 *
 * <p>Names come from the original's per-meta lang keys
 * ({@code item.ItemGolemCore.<meta>.name}: Fill, Empty, Gather, Harvest, Guard,
 * Decanting, Alchemy, Chop, Use, Butcher, Sorting, Fishing, and Blank for
 * meta 100); the port resolves them through its per-meta name key
 * {@code item.thaumcraft.itemgolemcore.<meta>} (the {@code ItemResource}
 * convention). Rarity is the original's {@code func_77613_e}: common for the
 * blank core, uncommon for every real core.</p>
 */
public class ItemGolemCore extends Item {

    /** The original's blank core metadata ({@code func_77617_a(100)} → {@code blankIcon}). */
    public static final int BLANK_META = 100;

    /**
     * The original's damage-value domain: {@code icon[12]} (metas 0..11) plus
     * the blank core at meta 100 — the set the localization audit walks.
     */
    public static final Set<Integer> VALID_METAS =
            Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, BLANK_META);

    public ItemGolemCore(final Properties properties) {
        super(properties);
    }

    /** Original {@code func_77667_c}: the per-meta name key ({@code ItemResource} convention). */
    @Override
    public Component getName(final ItemStack stack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath() + "." + storedMeta(stack));
    }

    public static int storedMeta(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_META.get(), 0);
    }

    /**
     * Original {@code ItemGolemCore.hasGUI(int)} — the cores whose golem has a
     * configuration GUI (0 fill, 1 empty, 2 gather, 4 guard, 5 decanting,
     * 8 use, 10 sorting). The GUI itself is TC-M6-08-s-c.
     */
    public static boolean hasGUI(final int core) {
        return switch (core) {
            case 0, 1, 2, 4, 5, 8, 10 -> true;
            default -> false;
        };
    }

    /** Original {@code ItemGolemCore.canSort(int)} — 0 fill, 1 empty, 2 gather, 8 use, 10 sorting. */
    public static boolean canSort(final int core) {
        return switch (core) {
            case 0, 1, 2, 8, 10 -> true;
            default -> false;
        };
    }

    /** Original {@code ItemGolemCore.hasInventory(int)} — 0 fill, 1 empty, 2 gather, 5 decanting, 8 use. */
    public static boolean hasInventory(final int core) {
        return switch (core) {
            case 0, 1, 2, 5, 8 -> true;
            default -> false;
        };
    }
}
