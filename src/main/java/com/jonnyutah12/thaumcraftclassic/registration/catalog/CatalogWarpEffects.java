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
 * TC-M6-05-s catalog holder: the warp-management items.
 *
 * <p>One {@code Catalog<Name>} unit per the registration architecture
 * (docs/architecture/registration.md): its own {@code DeferredRegister}s, one
 * {@code init(IEventBus)} line in
 * {@link com.jonnyutah12.thaumcraftclassic.registration.ModRegistration}, and
 * the ids exactly as docs/content-mapping.csv lists them.</p>
 *
 * <p>Owns exactly the content-mapping.csv rows {@code thaumcraft:itembathsalts},
 * {@code thaumcraft:itemsanitysoap} and {@code thaumcraft:itemsanitychecker}.
 * Per the catalog-triage rule the potion effects and the enchantments of the
 * same card do NOT live here: the effects register in {@code ModMobEffects}
 * (the shared TC-M2-01 holder whose javadoc pre-allocated this card) and the
 * enchantments are datapack payloads behind {@code ModEnchantments}.</p>
 */
public final class CatalogWarpEffects {

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

    public static void init(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private CatalogWarpEffects() {
    }
}
