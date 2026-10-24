package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemBathSalts;
import com.jonnyutah12.thaumcraftclassic.items.ItemSanityChecker;
import com.jonnyutah12.thaumcraftclassic.items.ItemSanitySoap;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TC-M6-05 catalog holder: the warp-management items.
 *
 * <p>Owns exactly the content-mapping.csv rows
 * {@code thaumcraft:itembathsalts}, {@code thaumcraft:itemsanitysoap} and
 * {@code thaumcraft:itemsanitychecker}. Per the catalog-triage rules this
 * card registers through its OWN holder; the potion effects and enchantments
 * of the same card live in {@code ModMobEffects} (shared TC-M2-01 holder,
 * its javadoc pre-allocates this card) and in the datapack enchantment
 * payloads behind {@code ModEnchantments}.</p>
 */
public final class M605WarpEffects {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);

    /** Thaumcraft:ItemBathSalts (Purifying Bath Salts). */
    public static final DeferredItem<ItemBathSalts> ITEM_BATH_SALTS =
            ITEMS.register("itembathsalts", ItemBathSalts::new);

    /** Thaumcraft:ItemSanitySoap (Sanitizing Soap). */
    public static final DeferredItem<ItemSanitySoap> ITEM_SANITY_SOAP =
            ITEMS.register("itemsanitysoap", ItemSanitySoap::new);

    /** Thaumcraft:ItemSanityChecker (Sanity Checker relic). */
    public static final DeferredItem<ItemSanityChecker> ITEM_SANITY_CHECKER =
            ITEMS.register("itemsanitychecker", ItemSanityChecker::new);

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private M605WarpEffects() {
    }
}
