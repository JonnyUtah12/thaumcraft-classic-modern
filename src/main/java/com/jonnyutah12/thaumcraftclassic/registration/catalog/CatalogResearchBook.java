package com.jonnyutah12.thaumcraftclassic.registration.catalog;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemThaumonomicon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * BOOK-1 (t_1e6b18cc) — the Thaumonomicon book item, the book's open path.
 *
 * <p>Owns exactly the content-mapping.csv row {@code Thaumcraft:ItemThaumonomicon}
 * ({@code thaumcraft:itemthaumonomicon}) and realises the open half of the gui
 * row {@code Thaumcraft:GUI_THAUMONOMICON} ({@code thaumcraft:gui_thaumonomicon})
 * — that gui row stays reference-only for the id harness, as
 * {@code CatalogTrinketsLoot}'s {@code gui_hand_mirror} / {@code gui_trunk} rows
 * are for their menus.</p>
 *
 * <p><b>No menu type is registered here, by decision.</b> The original's browser
 * is a {@code GuiScreen} with no container
 * ({@code GuiResearchBrowser.java:41}) and the port opens it through the S2C
 * {@code thaumcraft:open_thaumonomicon} payload, so the gui row's id has no
 * 1.21.1 menu realization (BD-22, and the reason is stated in
 * {@code OpenThaumonomiconPayload}). The game probe {@code bookRegistersNoMenuType}
 * pins that state, and the card's lang key is therefore
 * {@code container.thaumcraft.thaumonomicon} — the screen's own title key,
 * whitelisted in both localization audits — rather than a
 * menu-registry-derived {@code container.thaumcraft.gui_*} key.</p>
 *
 * <p>Per the catalog-triage rule (docs/catalog-triage.md rule 3) this card
 * registers through its OWN holder and appends exactly one wiring line in
 * {@code ModRegistration.init}; nothing here edits a sibling holder file.</p>
 */
public final class CatalogResearchBook {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ThaumcraftClassic.MOD_ID);

    /**
     * thaumcraft:itemthaumonomicon — the Thaumonomicon
     * (content-mapping row {@code Thaumcraft:ItemThaumonomicon}, original
     * {@code relics/ItemThaumonomicon}). Right-click opens the book screen:
     * the server arm completes the siblings of everything the player has
     * researched and sends the open payload, the client arm plays
     * {@code thaumcraft:page}.
     */
    public static final DeferredItem<ItemThaumonomicon> ITEM_THAUMONOMICON =
            ITEMS.register("itemthaumonomicon",
                    () -> new ItemThaumonomicon(new Item.Properties().rarity(Rarity.UNCOMMON)));

    private CatalogResearchBook() {
    }

    /** One wiring line from ModRegistration.init (catalog-triage pattern). */
    public static void init(final IEventBus bus) {
        ITEMS.register(bus);
    }
}
