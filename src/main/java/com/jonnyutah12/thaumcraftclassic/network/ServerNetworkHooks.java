package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
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

    /**
     * TC-M7-04-m2b: the research table's aspect combination. The original ran the
     * rule in the packet handler
     * ({@code PacketAspectCombinationToServer.onMessage}, its :71-126); the port
     * keeps the rule where the original's research table keeps its own copy of it —
     * on the tile — so the table's {@code bonusAspects} pool is paid in the same
     * place it is stored. The tile method is the whole of the original's semantics:
     * the RESEARCHER1 gate, the RESEARCHER2 gate on a known result, one of each
     * aspect out of the player's pool (or out of this table's bonus pool when the
     * well was fed from the table), and the discovery bonus on a first find.
     */
    static void combineAspects(final AspectCombinationToServerPayload payload, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level().getBlockEntity(payload.tablePos())
                instanceof com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable table)) {
            LOGGER.debug("aspect combination from {} at {}: no research table at that position",
                    player.getGameProfile().getName(), payload.tablePos());
            return;
        }
        final Aspect first = Aspect.getAspect(payload.aspect1());
        final Aspect second = Aspect.getAspect(payload.aspect2());
        if (first == null || second == null) {
            return;
        }
        // The original verified the sender owns the window by matching the packet's
        // player id; 1.21.1 gives the payload the sending player, and the tile's
        // distance check is the container's own stillValid.
        if (!table.stillValid(serverPlayer)) {
            return;
        }
        table.combineAspects(serverPlayer, first, second, payload.bonus1(), payload.bonus2());
    }

    /**
     * The research table's hex-grid drop. The payload is decoded field for field,
     * and the tile's body is the documented refusal: every step of the original's
     * {@code placeAspect} writes into the note's {@code ResearchNoteData}
     * hex grid, and that component is the note card's unmerged lane
     * ({@code TcComponents.NOTE}; docs/BEHAVIORAL_DEVIATIONS.md BD-19).
     */
    static void placeAspect(final AspectPlaceToServerPayload payload, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level().getBlockEntity(payload.tablePos())
                instanceof com.jonnyutah12.thaumcraftclassic.tiles.TileResearchTable table)) {
            return;
        }
        table.placeAspect(payload.q(), payload.r(),
                payload.aspectTag().map(Aspect::getAspect).orElse(null), serverPlayer);
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
