package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-side connection lifecycle: the replacement for the original
 * {@code EventHandlerNetwork.clientLoggedIn/clientLogsOut}. The client's
 * knowledge mirror is dropped when a new connection starts so a stale mirror
 * from a previous server can never leak into the current session (the original
 * reset {@code GuiResearchBrowser.completedResearch} the same way). The
 * original logout handler restored client-side config values; that is not
 * needed here because server config sync is platform-provided.
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID, value = Dist.CLIENT)
public final class ClientNetworkEvents {

    private ClientNetworkEvents() {
    }

    @SubscribeEvent
    static void onLoggingIn(final ClientPlayerNetworkEvent.LoggingIn event) {
        ClientNetworkData.wipe();
    }
}
