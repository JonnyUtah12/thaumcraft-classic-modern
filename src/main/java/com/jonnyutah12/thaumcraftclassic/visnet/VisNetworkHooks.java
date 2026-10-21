package com.jonnyutah12.thaumcraftclassic.visnet;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Vis-network lifecycle (TC-M4-02): clears the static {@link VisNetHandler}
 * networks when the server stops.
 *
 * <p>Parity note: the original left its static per-dimension registries in
 * place across world sessions and relied on {@code WeakReference} edges to
 * fade stale tiles out. The port holds explicit edges plus validity flags
 * (see {@link IVisNode}), so the registries must be dropped explicitly —
 * otherwise a second world in the same JVM would inherit the first world's
 * node graph.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class VisNetworkHooks {

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        VisNetHandler.clearNetworks();
    }

    private VisNetworkHooks() {
    }
}
