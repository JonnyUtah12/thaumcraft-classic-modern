package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectCombinations;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectKnowledge;
import com.jonnyutah12.thaumcraftclassic.menus.ResearchNoteMenu;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectCombinationToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPlaceToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.NotePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.PlayerCompleteToServerPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ScannedToServerPayload;
import com.jonnyutah12.thaumcraftclassic.player.ModAttachments;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
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
     * TC-M4-01: aspect combination in the research table, per original
     * {@code PacketAspectCombinationToServer.onMessage}. The player-identity
     * and world gates of the original ride the payload context (NeoForge
     * delivers the handler with the sending connection's player). The
     * tile-bonus branch ({@code TileResearchTable.bonusAspects}) belongs to
     * the research-table tile card; a bonus claim from the client that finds
     * an empty pool therefore pays nothing, exactly as the original does at a
     * tile whose bonus pool is empty.
     */
    static void combineAspects(final AspectCombinationToServerPayload payload, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final Aspect aspect1 = Aspect.getAspect(payload.aspect1());
        final Aspect aspect2 = Aspect.getAspect(payload.aspect2());
        if (aspect1 == null || aspect2 == null) {
            return;
        }
        final String tag1 = aspect1.getTag();
        final String tag2 = aspect2.getTag();
        PlayerKnowledgeData knowledge = serverPlayer.getData(ModAttachments.PLAYER_KNOWLEDGE);
        final int pool1 = knowledge.aspectAmount(tag1);
        final int pool2 = knowledge.aspectAmount(tag2);
        if (!((pool1 > 0 || payload.bonus1()) && (pool2 > 0 || payload.bonus2()))) {
            return;
        }
        final Aspect combo = AspectCombinations.getCombinationResult(aspect1, aspect2);
        if (pool1 > 0 || !payload.bonus1()) {
            final PlayerKnowledgeData paid = knowledge.withAspectPoolAdded(tag1, -1);
            if (paid != knowledge) {
                knowledge = paid;
                serverPlayer.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            }
            PacketDistributor.sendToPlayer(serverPlayer,
                    new AspectPoolPayload(tag1, 0, knowledge.aspectAmount(tag1)));
        }
        if (pool2 > 0 || !payload.bonus2()) {
            final PlayerKnowledgeData paid = knowledge.withAspectPoolAdded(tag2, -1);
            if (paid != knowledge) {
                knowledge = paid;
                serverPlayer.setData(ModAttachments.PLAYER_KNOWLEDGE, knowledge);
            }
            PacketDistributor.sendToPlayer(serverPlayer,
                    new AspectPoolPayload(tag2, 0, knowledge.aspectAmount(tag2)));
        }
        if (combo != null) {
            AspectKnowledge.checkAndSyncAspectKnowledge(serverPlayer, combo, 1);
        }
        // Original ResearchManager.scheduleSave: the port's attachment
        // autosaves with the player (BD-04).
    }

    /**
     * TC-M3-04-s-iii: the hex-grid write. The original's packet routed to the
     * {@code TileResearchTable} at the packet's position; this port routes to
     * the open research-note menu, which owns the grid (the tile-backed
     * research table ships on its own card).
     */
    static void placeAspect(final AspectPlaceToServerPayload payload, final Player player) {
        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.containerMenu instanceof ResearchNoteMenu menu) {
            menu.placeAspect(payload.q(), payload.r(), payload.aspectTag().orElse(null), serverPlayer);
            return;
        }
        LOGGER.debug("aspect place from {} at {} ({},{}): {} (no research-note menu open)",
                player.getGameProfile().getName(), payload.tablePos(), payload.q(), payload.r(),
                payload.aspectTag().orElse("(none)"));
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
