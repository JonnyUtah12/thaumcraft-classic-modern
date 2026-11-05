package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client screen wiring (TC-M7-04-s-cr1). NeoForge 21.1 replaced the 1.7.10
 * {@code GuiOpenEvent}/{@code clientSideInit} pattern with the mod-bus
 * {@link RegisterMenuScreensEvent}: every {@code MenuType} whose GUI is
 * implemented gets its screen class put into the map the event carries, and
 * vanilla's {@code MenuScreens} then builds the screen from the open-screen
 * packet.
 *
 * <p>This is the first screen wiring the port has had: the mod shipped zero
 * {@code AbstractContainerScreen} subclasses before this card, and the
 * {@code wip-salvage} tree contains no screen/GUI sources to port
 * ({@code git ls-tree -r --name-only wip-salvage | grep -icE "screen|gui"} is
 * 0), so the pair below is authored, not salvaged.</p>
 *
 * <p>{@code value = Dist.CLIENT} keeps the class out of the mod bus on a
 * dedicated server: {@code RegisterMenuScreensEvent} only fires on the client,
 * and the screen class is client-only, so loading it on the server side
 * ({@code runServer}, {@code runGameTestServer}) would be a classloading
 * hazard. Two screens are registered today: {@link TrunkScreen} for
 * {@code CatalogTrinketsLoot.GUI_TRUNK}, and {@link ResearchNoteScreen} — the
 * hex-grid research-note window over {@code thaumcraft:gui_research_note}
 * (TC-M3-04-s-iii: menu contract BD-26, hex-grid presentation BD-27) — for
 * {@code M3ResearchContent.GUI_RESEARCH_NOTE}; the hand-mirror and
 * decon-table screens and every other GUI id are follow-up cards. The crucible
 * has no menu type at all, so its screen cannot be wired here.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientScreens {

    /**
     * Register the trunk screen for the trunk menu type. The method reference
     * matches 1.21.1's screen-constructor shape
     * {@code (TrunkMenu, Inventory, Component) -> TrunkScreen}, so the
     * compiler proves the pair is type-correct: {@link TrunkScreen} is a
     * {@code Screen} implementing {@code MenuAccess<TrunkMenu>}.
     */
    @SubscribeEvent
    public static void onRegisterScreens(final RegisterMenuScreensEvent event) {
        event.register(CatalogTrinketsLoot.GUI_TRUNK.get(), TrunkScreen::new);
        // TC-M3-04-s-iii: the research-note window over thaumcraft:gui_research_note
        // — the hex grid, aspect pool, drag machine and copy plate (BD-27) over
        // the menu contract (BD-26). The MenuType must be screen-complete, so the
        // pairing lives here (the hex grid landed in NOTE-SCREEN-2 t_32a29c93).
        event.register(M3ResearchContent.GUI_RESEARCH_NOTE.get(), ResearchNoteScreen::new);
    }

    private ModClientScreens() {
    }
}
