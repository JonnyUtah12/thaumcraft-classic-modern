package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.network.payload.AspectDiscoveryPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.AspectPoolPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.BiomeChangePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.BoreDigPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.MiscEventPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.NotePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.ResearchCompletePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.RunicChargePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncAspectsPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncResearchPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedEntitiesPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedItemsPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncScannedPhenomenaPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWipePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.SyncWarpPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.WarpMessagePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBeamPulseGolemBossPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBeamPulsePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockArcPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockBubblePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockDigPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockSparklePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXBlockZapPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXEssentiaSourcePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXInfusionSourcePayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXShieldPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXSonicPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXVisDrainPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXWispZapPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXZapPayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Client handlers for every S2C payload. Each method runs on the client game
 * thread (the {@code enqueueWork} hop in {@link ModNetwork}) — the direct
 * equivalent of the original {@code @SideOnly(Side.CLIENT)} onMessage methods.
 *
 * <p>Knowledge updates land in {@link ClientNetworkData}; visual/audio
 * reactions go through {@link ClientEffectHooks}, which stays a stub until the
 * particle/GUI milestones provide the real client systems.</p>
 */
final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {
    }

    // --- player knowledge sync ---

    static void handleSyncWipe(final SyncWipePayload payload) {
        ClientNetworkData.wipe();
    }

    static void handleSyncResearch(final SyncResearchPayload payload) {
        ClientNetworkData.replaceResearch(payload.entries());
    }

    static void handleSyncScannedItems(final SyncScannedItemsPayload payload) {
        ClientNetworkData.replaceScannedItems(payload.entries());
    }

    static void handleSyncScannedEntities(final SyncScannedEntitiesPayload payload) {
        ClientNetworkData.replaceScannedEntities(payload.entries());
    }

    static void handleSyncScannedPhenomena(final SyncScannedPhenomenaPayload payload) {
        ClientNetworkData.replaceScannedPhenomena(payload.entries());
    }

    static void handleSyncAspects(final SyncAspectsPayload payload) {
        final Map<String, Integer> amounts = new HashMap<>();
        for (SyncAspectsPayload.AspectAmount aspect : payload.aspects()) {
            amounts.put(aspect.tag(), aspect.amount());
        }
        ClientNetworkData.replaceDiscoveredAspects(amounts);
    }

    static void handleSyncWarp(final SyncWarpPayload payload) {
        ClientNetworkData.setWarp(payload.warpKind(), payload.value());
    }

    static void handleWarpMessage(final WarpMessagePayload payload) {
        ClientEffectHooks.warpNotification(payload.change(), payload.warpKind());
    }

    static void handleAspectPool(final AspectPoolPayload payload) {
        ClientNetworkData.setAspectPool(payload.tag(), payload.total());
        ClientEffectHooks.aspectPoolNotification(payload.tag(), payload.amount());
    }

    static void handleAspectDiscovery(final AspectDiscoveryPayload payload) {
        ClientNetworkData.addDiscoveredAspect(payload.tag(), 1);
        ClientEffectHooks.aspectDiscoveryNotification(payload.tag());
    }

    static void handleResearchComplete(final ResearchCompletePayload payload) {
        ClientNetworkData.addCompletedResearch(payload.key());
        ClientEffectHooks.researchCompleteNotification(payload.key());
    }

    static void handleRunicCharge(final RunicChargePayload payload) {
        ClientNetworkData.setRunicCharge(payload.playerId(), payload.charge(), payload.maxCharge());
    }

    // --- misc ---

    static void handleBiomeChange(final BiomeChangePayload payload) {
        ClientEffectHooks.biomeChange(payload.x(), payload.z(), payload.biome());
    }

    static void handleMiscEvent(final MiscEventPayload payload) {
        ClientEffectHooks.miscEvent(payload.eventKind());
    }

    static void handleNote(final NotePayload payload) {
        ClientEffectHooks.noteUpdate(payload.pos(), payload.note());
    }

    static void handleBoreDig(final BoreDigPayload payload) {
        ClientEffectHooks.boreDig(payload.pos(), payload.digLocation());
    }

    // --- visual effects ---

    static void handleFxBlockBubble(final FXBlockBubblePayload payload) {
        ClientEffectHooks.fxBlockBubble(payload.pos(), payload.color());
    }

    static void handleFxBlockSparkle(final FXBlockSparklePayload payload) {
        ClientEffectHooks.fxBlockSparkle(payload.pos(), payload.color());
    }

    static void handleFxBlockArc(final FXBlockArcPayload payload) {
        ClientEffectHooks.fxBlockArc(payload.pos(), payload.sourceEntityId());
    }

    static void handleFxBlockZap(final FXBlockZapPayload payload) {
        ClientEffectHooks.fxBlockZap(payload.pos(), payload.direction());
    }

    static void handleFxBlockDig(final FXBlockDigPayload payload) {
        ClientEffectHooks.fxBlockDig(payload.targetPos(), payload.blockOrItemId(), payload.data(), payload.digFromPos());
    }

    static void handleFxEssentiaSource(final FXEssentiaSourcePayload payload) {
        ClientEffectHooks.fxEssentiaSource(payload.pos(), payload.offset(), payload.color());
    }

    static void handleFxInfusionSource(final FXInfusionSourcePayload payload) {
        ClientEffectHooks.fxInfusionSource(payload.pos(), payload.offset(), payload.color());
    }

    static void handleFxShield(final FXShieldPayload payload) {
        ClientEffectHooks.fxShield(payload.sourceEntityId(), payload.targetEntityId());
    }

    static void handleFxSonic(final FXSonicPayload payload) {
        ClientEffectHooks.fxSonic(payload.sourceEntityId());
    }

    static void handleFxVisDrain(final FXVisDrainPayload payload) {
        ClientEffectHooks.fxVisDrain(payload.pos(), payload.offset(), payload.color());
    }

    static void handleFxWispZap(final FXWispZapPayload payload) {
        ClientEffectHooks.fxWispZap(payload.sourceEntityId(), payload.targetEntityId());
    }

    static void handleFxZap(final FXZapPayload payload) {
        ClientEffectHooks.fxZap(payload.sourceEntityId(), payload.targetEntityId());
    }

    static void handleFxBeamPulse(final FXBeamPulsePayload payload) {
        ClientEffectHooks.fxBeamPulse(payload.sourceEntityId(), payload.targetEntityId(), payload.color());
    }

    static void handleFxBeamPulseGolemBoss(final FXBeamPulseGolemBossPayload payload) {
        ClientEffectHooks.fxBeamPulseGolemBoss(payload.sourceEntityId(), payload.targetEntityId());
    }
}
