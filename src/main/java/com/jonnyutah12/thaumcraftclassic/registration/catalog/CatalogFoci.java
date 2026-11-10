package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.foci.Focuses;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocusPouch;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * TC-M6-02 catalog holder: the wand-focus family — the focus pouch and the ten
 * foci.
 *
 * <p>Owns exactly the content-mapping.csv {@code item} rows this card flips:
 * {@code thaumcraft:focuspouch} ({@code class=ItemFocusPouchBauble}) and the ten
 * {@code thaumcraft:focus*} rows (fire, shock, hellbat, frost, trade,
 * excavation, portablehole, pech, warding, primal). Each row stays one
 * registered object, on the {@code new_id} column (D-001 lowercasing,
 * docs/architecture/registration.md); the ten original focus classes collapse
 * onto one generic {@link ItemFocus} whose identity is a {@link Focus} table
 * entry, because they differed only in declared values plus the TC-M6-03-s
 * spell bodies.</p>
 *
 * <p>Item shape, from the original constructors: {@code ItemFocusBasic} set
 * {@code maxStackSize = 1}, {@code canRepair = false}, {@code setMaxDamage(0)}
 * and reported {@code EnumRarity.rare}
 * ({@code api-repos/thaumcraft-api/wands/ItemFocusBasic.java:26-32,86-89});
 * {@code ItemFocusPouch} set stack size 1, no subtypes, no durability, and
 * {@code EnumRarity.rare} ({@code ItemFocusPouch.java:20-46}). Both become
 * {@code stacksTo(1).setNoRepair().rarity(Rarity.RARE)}.</p>
 *
 * <p>The arcane crafting rows for the foci already shipped with the arcane
 * workbench data (TC-M4-03-s,
 * {@code data/thaumcraft/recipe/recipe.153-159.arcane_shaped.json}) and are
 * gated on these ids through {@code neoforge:item_exists}, so they start
 * resolving as soon as these holders exist — the same effect registering the
 * wand caps/rods had. The three infusion-gated foci (hellbat, portablehole,
 * warding — {@code recipe.60-62}) stay {@code not_started}: those are infusion
 * recipe rows owned by the recipe milestones.</p>
 */
public final class CatalogFoci {

    /** The focus pouch (original {@code ItemFocusPouchBauble}). */
    public static final DeferredItem<ItemFocusPouch> FOCUS_POUCH =
            ModItems.ITEMS.register("focuspouch",
                    () -> new ItemFocusPouch(focusProps()));

    private static final Map<String, DeferredItem<ItemFocus>> FOCI = new LinkedHashMap<>();

    static {
        // Original registration order (ConfigItems.java:335-354) == CSV row order.
        register("fire");
        register("shock");
        register("hellbat");
        register("frost");
        register("trade");
        register("excavation");
        register("portablehole");
        register("pech");
        register("warding");
        register("primal");
    }

    private static void register(final String tag) {
        final Focus focus = Focuses.byTag(tag);
        if (focus == null) {
            throw new IllegalStateException("no focus table entry for tag: " + tag);
        }
        FOCI.put(tag, ModItems.ITEMS.register("focus" + tag, () -> new ItemFocus(focus, focusProps())));
    }

    /** The original's universal focus/pouch properties: unstackable, unrepairable, rare. */
    private static Item.Properties focusProps() {
        return new Item.Properties().stacksTo(1).setNoRepair().rarity(Rarity.RARE);
    }

    // ------------------------------------------------------------------ lookups

    /** The registered item for a focus tag, or null. */
    public static DeferredItem<ItemFocus> item(final String tag) {
        return FOCI.get(tag);
    }

    /** The registered focus items by tag, in registration order. */
    public static Map<String, DeferredItem<ItemFocus>> items() {
        return Collections.unmodifiableMap(FOCI);
    }

    /** Forces class initialization (entry registration) from {@code ModRegistration}. */
    public static void init() {
    }

    private CatalogFoci() {
    }
}
