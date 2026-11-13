package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogArcaneWorkbench;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogThaumatorium;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client screen wiring (TC-M7-04-s; first landing TC-M7-04-s-cr1). NeoForge 21.1
 * replaced the 1.7.10 {@code GuiOpenEvent}/{@code clientSideInit} pattern with
 * the mod-bus {@link RegisterMenuScreensEvent}: every {@code MenuType} whose GUI
 * is implemented gets its screen class put into the map the event carries, and
 * vanilla's {@code MenuScreens} then builds the screen from the open-screen
 * packet.
 *
 * <p>This is the port's only screen wiring. The mod shipped zero
 * {@code AbstractContainerScreen} subclasses before TC-M7-04, and the
 * {@code wip-salvage} tree contains no screen/GUI sources to port
 * ({@code git ls-tree -r --name-only wip-salvage | grep -icE "screen|gui"} is
 * 0), so every pair below is authored, not salvaged.</p>
 *
 * <p>{@code value = Dist.CLIENT} keeps the class out of the mod bus on a
 * dedicated server: {@code RegisterMenuScreensEvent} only fires on the client,
 * and the screen classes are client-only, so loading them on the server side
 * ({@code runServer}, {@code runGameTestServer}) would be a classloading
 * hazard.</p>
 *
 * <p><b>Coverage.</b> As of this commit every {@code MenuType} the mod
 * registers has a screen here: twelve registrations in
 * {@link #registerScreens} — {@code gui_trunk} (TC-M7-04-s-cr1),
 * {@code gui_hand_mirror}, {@code gui_decon_table}, {@code gui_magic_box} and
 * {@code gui_spa} (TC-M7-04-s, carried into main by the golem-visual lane,
 * t_17686882), {@code gui_golem} (TC-M7-04-m3-a), {@code gui_research_note} —
 * the hex-grid research-note window (TC-M3-04-s-iii: menu contract BD-44,
 * hex-grid presentation BD-45, NOTE-SCREEN-2 t_32a29c93) —
 * {@code gui_arcane_workbench} (TC-M7-04-m2, the first crafting-table GUI, on
 * the GUI fleet's shared base {@code d6d50e3}), {@code gui_thaumatorium}
 * (TC-M7-04-m2c), {@code gui_research_table} (TC-M7-04-m2b) and the two device
 * machines of TC-M7-04-m1, {@code gui_alchemy_furnace} (the original's
 * {@code gui_id} 9, registered from the essentia-storage catalog) and
 * {@code gui_arcane_bore} (the original's {@code gui_id} 15, from the devices
 * catalog). The crucible needs no screen: the 1.7.10 crucible ships no GUI at
 * all — no {@code GuiCrucible} in the reference {@code client/gui/}, and both
 * {@code ClientProxy} and {@code CommonProxy} return null for every crucible
 * gui id — so it is a direct-manipulation block and its
 * {@code docs/content-mapping.csv} {@code gui_crucible} row closes on absence,
 * proved by {@code CrucibleGuiParityTest}. The GUI ids still without a menu
 * type (pech, focus pouch, hover harness, focal manipulator) are separate
 * cards, and one — the book's containerless browser (t_547e7ad8) — is opened
 * by a payload and must never be paired with a menu factory here (BD-22). A
 * new menu type added without a screen fails
 * {@code MenuTypeScreenCoverageTest}.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientScreens {

    /**
     * Register every implemented screen. Each method reference matches 1.21.1's
     * screen-constructor shape {@code (M, Inventory, Component) -> Screen}, so
     * the compiler proves each pair is type-correct: the screen is a
     * {@code Screen} implementing {@code MenuAccess<M>} for the menu the
     * {@code MenuType} carries.
     */
    @SubscribeEvent
    public static void onRegisterScreens(final RegisterMenuScreensEvent event) {
        // TC-M7-04-s-cr1: the traveling trunk (gui_id 2, GuiTravelingTrunk).
        event.register(CatalogTrinketsLoot.GUI_TRUNK.get(), TrunkScreen::new);
        // TC-M3-04-s-iii: the research-note window over thaumcraft:gui_research_note
        // — the hex grid, aspect pool, drag machine and copy plate (BD-45) over
        // the menu contract (BD-44). The MenuType must be screen-complete, so the
        // pairing lives here (the hex grid landed in NOTE-SCREEN-2 t_32a29c93).
        event.register(M3ResearchContent.GUI_RESEARCH_NOTE.get(), ResearchNoteScreen::new);
        // TC-M7-04-s, the four remaining menu types:
        event.register(CatalogTrinketsLoot.GUI_HAND_MIRROR.get(), HandMirrorScreen::new);
        event.register(CatalogFurniture.MENU_DECON_TABLE.get(), DeconstructionTableScreen::new);
        event.register(CatalogFurniture.MENU_MAGIC_BOX.get(), MagicBoxScreen::new);
        event.register(CatalogFurniture.MENU_SPA.get(), SpaScreen::new);
        // TC-M7-04-m3-a: the golem (gui_id 0, GuiGolem) over the golem-content branch.
        event.register(CatalogGolems.GUI_GOLEM.get(), GolemScreen::new);
        // TC-M7-04-m2: the first crafting-table GUI, gui_arcane_workbench.
        event.register(CatalogArcaneWorkbench.MENU_ARCANE_WORKBENCH.get(), ArcaneWorkbenchScreen::new);
        // TC-M7-04-m2c: the thaumatorium queue window, gui_thaumatorium.
        event.register(CatalogThaumatorium.MENU_THAUMATORIUM.get(), ThaumatoriumScreen::new);
        // TC-M7-04-m2b: the research table, gui_research_table (original gui_id 10).
        event.register(CatalogFurniture.MENU_RESEARCH_TABLE.get(), ResearchTableScreen::new);
        // TC-M7-04-m1, the device machines (gui_id 9 and 15):
        event.register(CatalogEssentiaStorage.MENU_ALCHEMY_FURNACE.get(), AlchemyFurnaceScreen::new);
        event.register(CatalogDevices.MENU_ARCANE_BORE.get(), ArcaneBoreScreen::new);
    }

    private ModClientScreens() {
    }
}
