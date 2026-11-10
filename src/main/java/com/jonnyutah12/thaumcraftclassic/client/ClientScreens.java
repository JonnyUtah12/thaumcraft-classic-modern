package com.jonnyutah12.thaumcraftclassic.client;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.gui.ArcaneWorkbenchScreen;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M403ArcaneWorkbench;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side screen wiring (TC-M4-03). Screens bind to their menu types on
 * the mod bus; the menu type id is the CSV gui row
 * {@code thaumcraft:gui_arcane_workbench}.
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class ClientScreens {

    @SubscribeEvent
    public static void onRegisterScreens(final RegisterMenuScreensEvent event) {
        event.register(M403ArcaneWorkbench.MENU_TYPE.get(), ArcaneWorkbenchScreen::new);
    }

    private ClientScreens() {
    }
}
