package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Client-side reaction points for effect/notification payloads (particles,
 * sounds, research popups, biome view). The original handlers called directly
 * into the client FX system, {@code PlayerNotifications}, and
 * {@code GuiResearchBrowser}; those systems arrive with their own milestones,
 * so each hook is a stub that logs at debug level and carries a TODO naming
 * the milestone that replaces it. The payload plumbing around them is final —
 * only these bodies change.
 */
public final class ClientEffectHooks {

    private static final Logger LOGGER = ThaumcraftClassic.LOGGER;

    private ClientEffectHooks() {
    }

    // --- notifications (original PlayerNotifications / GuiResearchBrowser) ---

    /** TODO(M4 research UI): warp toast + whispers sound, per original PacketWarpMessage. */
    static void warpNotification(final int change, final int warpKind) {
        LOGGER.debug("warp notification: kind={} change={}", warpKind, change);
    }

    /** TODO(M4 research UI): aspect pool gain toast, per original PacketAspectPool. */
    static void aspectPoolNotification(final String tag, final int amount) {
        LOGGER.debug("aspect pool notification: {} +{}", tag, amount);
    }

    /** TODO(M4 research UI): aspect discovery toast, per original PacketAspectDiscovery. */
    static void aspectDiscoveryNotification(final String tag) {
        LOGGER.debug("aspect discovery notification: {}", tag);
    }

    /** TODO(M4 research UI): research popup + book highlight, per original PacketResearchComplete. */
    static void researchCompleteNotification(final String key) {
        LOGGER.debug("research complete notification: {}", key);
    }

    // --- misc client effects ---

    /** TODO(M6 world/magic plants): client biome view update, per original PacketBiomeChange. */
    static void biomeChange(final int x, final int z, final ResourceLocation biome) {
        LOGGER.debug("biome change at ({}, {}) -> {}", x, z, biome);
    }

    /** TODO(M4 client effects): warp vignette / mist fog, per original PacketMiscEvent. */
    static void miscEvent(final int eventKind) {
        LOGGER.debug("misc client event: kind={}", eventKind);
    }

    /** TODO(M5 tiles/sensors): note-block & sensor value mirror, per original PacketNote. */
    static void noteUpdate(final BlockPos pos, final int note) {
        LOGGER.debug("note update at {}: {}", pos, note);
    }

    /** TODO(M5 tiles/arcane bore): dig event on client tile, per original PacketBoreDig. */
    static void boreDig(final BlockPos pos, final int digLocation) {
        LOGGER.debug("bore dig at {} loc={}", pos, digLocation);
    }

    // --- visual effects (original common/lib/network/fx handlers) ---
    // TODO(M4 client effects): replace every stub with the ported particle/beam
    // system; the payload shapes below already carry everything the original
    // onMessage methods used.

    static void fxBlockBubble(final BlockPos pos, final int color) {
        LOGGER.debug("fx block bubble at {} color={}", pos, color);
    }

    static void fxBlockSparkle(final BlockPos pos, final int color) {
        LOGGER.debug("fx block sparkle at {} color={}", pos, color);
    }

    static void fxBlockArc(final BlockPos pos, final int sourceEntityId) {
        LOGGER.debug("fx block arc at {} source={}", pos, sourceEntityId);
    }

    static void fxBlockZap(final Vec3 pos, final Vec3 direction) {
        LOGGER.debug("fx block zap at {} dir={}", pos, direction);
    }

    static void fxBlockDig(final BlockPos targetPos, final ResourceLocation blockOrItemId, final int data,
            final BlockPos digFromPos) {
        LOGGER.debug("fx block dig at {} block={} data={} from={}", targetPos, blockOrItemId, data, digFromPos);
    }

    static void fxEssentiaSource(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx essentia source at {} offset={} color={}", pos, offset, color);
    }

    static void fxInfusionSource(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx infusion source at {} offset={} color={}", pos, offset, color);
    }

    static void fxShield(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx shield {} -> {}", sourceEntityId, targetEntityId);
    }

    static void fxSonic(final int sourceEntityId) {
        LOGGER.debug("fx sonic at {}", sourceEntityId);
    }

    static void fxVisDrain(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx vis drain at {} offset={} color={}", pos, offset, color);
    }

    static void fxWispZap(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx wisp zap {} -> {}", sourceEntityId, targetEntityId);
    }

    static void fxZap(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx zap {} -> {}", sourceEntityId, targetEntityId);
    }

    static void fxBeamPulse(final int sourceEntityId, final int targetEntityId, final int color) {
        LOGGER.debug("fx beam pulse {} -> {} color={}", sourceEntityId, targetEntityId, color);
    }

    static void fxBeamPulseGolemBoss(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx golem boss beam pulse {} -> {}", sourceEntityId, targetEntityId);
    }
}
