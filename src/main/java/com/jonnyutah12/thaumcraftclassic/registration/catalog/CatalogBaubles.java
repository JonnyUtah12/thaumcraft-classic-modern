package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemAmuletRunic;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemAmuletVis;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemBaubleBlanks;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemGirdleHover;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemGirdleRunic;
import com.jonnyutah12.thaumcraftclassic.items.baubles.ItemRingRunic;
import com.jonnyutah12.thaumcraftclassic.registration.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bauble item registration (TC-M8-02): the six kind=item rows
 * docs/content-mapping.csv:253-258, ids exactly per the {@code new_id}
 * column (docs/architecture/registration.md, D-001 lowercasing).
 *
 * <p>Every original constructor set {@code maxStackSize = 1} and
 * {@code canRepair = false} (e.g. {@code ItemRingRunic.java:21-22}); the
 * port expresses both through {@link Item.Properties}.</p>
 *
 * <p>The Curios bridge attaches behavior automatically at common setup
 * (it scans {@code ModItems} for {@code AccessoryItem}s, TC-M8-01); slot
 * routing data lives in {@code data/curios/tags/item/*}. The focus pouch
 * ({@code thaumcraft:focuspouch}, content-mapping.csv:193) is wand/focus
 * work owned by TC-M6-02 (docs/catalog-triage.md, TC-M6-02 section) and is
 * deliberately not registered here.</p>
 */
public final class CatalogBaubles {

    /** Bauble blanks: amulet/ring/belt blanks + six apprentice rings (metas 0-8). */
    public static final DeferredItem<ItemBaubleBlanks> ITEM_BAUBLE_BLANKS =
            ModItems.ITEMS.register("itembaubleblanks",
                    () -> new ItemBaubleBlanks(baubleProps()));

    /** Runic rings: protection / greater / charged / revitalizing (metas 0-3). */
    public static final DeferredItem<ItemRingRunic> ITEM_RING_RUNIC =
            ModItems.ITEMS.register("itemringrunic",
                    () -> new ItemRingRunic(baubleProps(Rarity.UNCOMMON)));

    /** Runic amulets: avatar / emergency (metas 0-1). */
    public static final DeferredItem<ItemAmuletRunic> ITEM_AMULET_RUNIC =
            ModItems.ITEMS.register("itemamuletrunic",
                    () -> new ItemAmuletRunic(baubleProps(Rarity.RARE)));

    /** Runic girdles: vis / kinetic (metas 0-1). */
    public static final DeferredItem<ItemGirdleRunic> ITEM_GIRDLE_RUNIC =
            ModItems.ITEMS.register("itemgirdlerunic",
                    () -> new ItemGirdleRunic(baubleProps(Rarity.RARE)));

    /** Vis amulets: lesser / greater (metas 0-1). */
    public static final DeferredItem<ItemAmuletVis> ITEM_AMULET_VIS =
            ModItems.ITEMS.register("itemamuletvis",
                    () -> new ItemAmuletVis(baubleProps(Rarity.UNCOMMON)));

    /** Thaumostatic girdle (hover), single variant. */
    public static final DeferredItem<ItemGirdleHover> ITEM_GIRDLE_HOVER =
            ModItems.ITEMS.register("itemgirdlehover",
                    () -> new ItemGirdleHover(baubleProps(Rarity.RARE)));

    /** The original's universal bauble properties: unstackable, unrepairable. */
    private static Item.Properties baubleProps() {
        return new Item.Properties().stacksTo(1).setNoRepair();
    }

    /**
     * Default rarity per item = the original's meta-0 rarity
     * ({@code func_77613_e}); upgrade variants override via
     * {@code MetaBaubleItem.rarityForMeta}.
     */
    private static Item.Properties baubleProps(final Rarity defaultRarity) {
        return baubleProps().rarity(defaultRarity);
    }

    /** Forces class initialization (entry registration) from {@code ModRegistration}. */
    public static void init() {
    }

    private CatalogBaubles() {
    }
}
