package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.NotePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Server-side action sinks for C2S payloads. The systems that own these
 * actions (ScanManager, ResearchManager, player knowledge, wand/equipment
 * behavior, sensor tiles) arrive with their feature milestones; until then
 * each hook is a stub that logs at debug level and carries a TODO naming the
 * milestone that implements the real behavior. The payload plumbing around
 * them is final — only these bodies change.
 */
public final class ServerNetworkHooks {

    private static final Logger LOGGER = ThaumcraftClassic.LOGGER;

    private ServerNetworkHooks() {
    }

    /** TODO(M4 research): ScanManager.completeScan equivalent, per original PacketScannedToServer. */
    static void completeScan(final ScannedToServerPayload payload, final Player player) {
        LOGGER.debug("scan request from {}: type={} id={} data={}", player.getGameProfile().getName(),
                payload.scanType(), payload.scannedId(), payload.data());
    }

    /** TODO(M4 research table): aspect combination in the research table, per original PacketAspectCombinationToServer. */
    static void combineAspects(final AspectCombinationToServerPayload payload, final Player player) {
        LOGGER.debug("aspect combination from {} at {}: {} + {}", player.getGameProfile().getName(),
                payload.tablePos(), payload.aspect1(), payload.aspect2());
    }

    /** TODO(M4 research table): place aspect on the research table grid, per original PacketAspectPlaceToServer. */
    static void placeAspect(final AspectPlaceToServerPayload payload, final Player player) {
        LOGGER.debug("aspect place from {} at {} ({},{}): {}", player.getGameProfile().getName(),
                payload.tablePos(), payload.q(), payload.r(), payload.aspectTag().orElse("(none)"));
    }

    /**
     * TC-M3-02: the Thaumonomicon completion request, per original
     * {@code PacketPlayerCompleteToServer.onMessage} — the research manager owns
     * the semantics (prerequisite gate, aspect-pool payment, completion, siblings).
     */
    static void completeResearchRequest(final PlayerCompleteToServerPayload payload, final Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchManager.handlePlayerCompleteRequest(payload, serverPlayer);
        }
    }

    /** TODO(M3 equipment/hover): hover flight toggle, per original PacketFlyToServer. */
    static void setHover(final boolean hover, final Player player) {
        LOGGER.debug("hover toggle from {}: {}", player.getGameProfile().getName(), hover);
    }

    /** TODO(M3 equipment/wands): wand focus change, per original PacketFocusChangeToServer. */
    static void changeFocus(final String focus, final Player player) {
        LOGGER.debug("focus change from {}: {}", player.getGameProfile().getName(), focus);
    }

    /** TODO(M3 equipment): item-bound key actions, per original PacketItemKeyToServer. */
    static void itemKey(final int key, final Player player) {
        LOGGER.debug("item key from {}: {}", player.getGameProfile().getName(), key);
    }

    /** TODO(M5 tiles/sensors): answer a sensor note query, per original PacketNote server side. */
    static void answerNoteQuery(final NotePayload payload, final Player player) {
        LOGGER.debug("note query from {} at {}", player.getGameProfile().getName(), payload.pos());
    }
}
