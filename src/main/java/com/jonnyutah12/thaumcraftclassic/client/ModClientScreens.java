package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
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
 * hazard. Only one screen is registered today — {@link TrunkScreen} for
 * {@code CatalogTrinketsLoot.GUI_TRUNK}; the hand-mirror and decon-table
 * screens, and every other GUI id, are follow-up TC-M7-04 cards. The crucible
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
    }

    private ModClientScreens() {
    }
}
