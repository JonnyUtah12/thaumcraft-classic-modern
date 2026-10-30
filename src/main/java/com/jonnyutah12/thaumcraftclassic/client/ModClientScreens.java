package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
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
 * registers has a screen here — the five gui rows of TC-M7-04-s
 * ({@code gui_trunk}, {@code gui_hand_mirror}, {@code gui_decon_table},
 * {@code gui_magic_box}, {@code gui_spa}) plus the two device machines of
 * TC-M7-04-m1 ({@code gui_alchemy_furnace} = gui_id 9,
 * {@code gui_arcane_bore} = gui_id 15). The crucible needs no screen:
 * the 1.7.10 crucible has no GUI at all (no {@code GuiCrucible} exists in
 * the reference {@code client/gui/}, and both {@code ClientProxy} and
 * {@code CommonProxy} return null for every crucible gui id) — it is a
 * direct-manipulation block, so its content-mapping {@code gui_crucible}
 * row closes on absence, proved by {@code CrucibleGuiParityTest}. The
 * remaining GUI ids have no menu type yet (arcane workbench,
 * thaumatorium, golem, pech, focus pouch, hover harness, focal
 * manipulator, research table) or are the Thaumonomicon (t_547e7ad8), so
 * they cannot be wired here; they are separate cards. A new menu type
 * added without a screen fails {@code MenuTypeScreenCoverageTest}.</p>
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
        // TC-M7-04-s, the four remaining menu types:
        event.register(CatalogTrinketsLoot.GUI_HAND_MIRROR.get(), HandMirrorScreen::new);
        event.register(CatalogFurniture.MENU_DECON_TABLE.get(), DeconstructionTableScreen::new);
        event.register(CatalogFurniture.MENU_MAGIC_BOX.get(), MagicBoxScreen::new);
        event.register(CatalogFurniture.MENU_SPA.get(), SpaScreen::new);
        // TC-M7-04-m1, the device machines (gui_id 9 and 15):
        event.register(CatalogEssentiaStorage.MENU_ALCHEMY_FURNACE.get(), AlchemyFurnaceScreen::new);
        event.register(CatalogDevices.MENU_ARCANE_BORE.get(), ArcaneBoreScreen::new);
    }

    private ModClientScreens() {
    }
}
