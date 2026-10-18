package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncAspectsPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncResearchPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedEntitiesPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedItemsPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedPhenomenaPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWipePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Connection-lifecycle wiring: the replacement for the original
 * {@code EventHandlerNetwork.playerLoggedInEvent}, which pushed the full
 * knowledge sync set to every joining player.
 *
 * <p>The server-side knowledge store arrives with the persistence milestone
 * (TC-M2-02); until then the authoritative content is empty, but the
 * wipe-then-sync ordering itself is the contract the client mirror depends
 * on, so it is established now. The original also pushed {@code PacketConfig}
 * here — dropped on purpose: NeoForge syncs server configs to clients by
 * itself (see docs/architecture/networking.md).</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class ModNetworkEvents {

    private ModNetworkEvents() {
    }

    @SubscribeEvent
    static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new SyncWipePayload(),
                    new SyncResearchPayload(List.of()),
                    new SyncScannedItemsPayload(List.of()),
                    new SyncScannedEntitiesPayload(List.of()),
                    new SyncScannedPhenomenaPayload(List.of()),
                    new SyncAspectsPayload(List.of()),
                    new SyncWarpPayload(0, SyncWarpPayload.WARP_PERMANENT),
                    new SyncWarpPayload(0, SyncWarpPayload.WARP_STICKY),
                    new SyncWarpPayload(0, SyncWarpPayload.WARP_TEMPORARY));
        }
    }
}
