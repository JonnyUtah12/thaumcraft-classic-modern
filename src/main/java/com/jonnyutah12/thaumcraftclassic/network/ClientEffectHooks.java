package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
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

    /**
     * TC-M7-05 sound half WIRED: the original PacketWarpMessage client handler
     * plays {@code thaumcraft:whispers} on the client player (volume 0.5,
     * pitch 1.0) whenever a warp gain (data &gt; 0) arrives, for both sticky
     * and temporary warp. The toast remains TODO(M4 research UI).
     */
    static void warpNotification(final int change, final int warpKind) {
        LOGGER.debug("warp notification: kind={} change={}", warpKind, change);
        if (change > 0) {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(ModSounds.WHISPERS.value(), 0.5F, 1.0F);
            }
        }
    }

    /** TODO(M4 research UI): aspect pool gain toast, per original PacketAspectPool. */
    static void aspectPoolNotification(final String tag, final int amount) {
        LOGGER.debug("aspect pool notification: {} +{}", tag, amount);
    }

    /** TODO(M4 research UI): aspect discovery toast, per original PacketAspectDiscovery. */
    static void aspectDiscoveryNotification(final String tag) {
        LOGGER.debug("aspect discovery notification: {}", tag);
    }

    /**
     * TODO(M4 research UI): research popup + book highlight, per original
     * PacketResearchComplete. TC-M7-05 contract note: the original also plays
     * {@code thaumcraft:learn} on the client player (volume 0.2, pitch
     * 1.0 + rand*0.1) but ONLY for clue-type research — the condition needs
     * the research graph (TC-M3-01/02), so the play site is deferred with it;
     * see docs/sound-events.md#learn.
     */
    static void researchCompleteNotification(final String key) {
        LOGGER.debug("research complete notification: {}", key);
    }

    // --- misc client effects ---

    /**
     * TC-M5-04-s WIRED: the client half of the original PacketBiomeChange. The
     * original's handler wrote the biome byte of the chunk column into the
     * client's chunk; the port applies the same palette write through
     * {@link TaintBiomes#applyBiomeChange} (1.21.1 keeps biomes in a
     * 4×4×4-section container), so biome fog/sky/grass tint follow a runtime
     * conversion the way they did in 1.7.10.
     */
    static void biomeChange(final int x, final int z, final ResourceLocation biome) {
        LOGGER.debug("biome change at ({}, {}) -> {}", x, z, biome);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            TaintBiomes.applyBiomeChange(mc.level, x, z, biome);
        }
    }

    /**
     * TC-M7-05 sound half WIRED: the original PacketMiscEvent client handler
     * plays {@code thaumcraft:heartbeat} at the client player's position
     * (volume 1.0, pitch 1.0) for event kind 0 (warp heartbeat); kinds 1-3
     * are fog/vignette only. The vignette itself remains TODO(M4 client
     * effects).
     */
    static void miscEvent(final int eventKind) {
        LOGGER.debug("misc client event: kind={}", eventKind);
        if (eventKind == 0) {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.HEARTBEAT.value(), SoundSource.MASTER, 1.0F, 1.0F, false);
            }
        }
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

    /**
     * TC-M7-05 sound half WIRED: the original PacketFXBlockZap client handler
     * plays {@code thaumcraft:zap} at the block position (volume 0.1, pitch
     * 1.0 + rand*0.2) alongside the node-bolt particle. The particle/beam
     * half remains TODO(M4 client effects → TC-M7-02).
     */
    static void fxBlockZap(final Vec3 pos, final Vec3 direction) {
        LOGGER.debug("fx block zap at {} dir={}", pos, direction);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.playLocalSound(pos.x, pos.y, pos.z,
                    ModSounds.ZAP.value(), SoundSource.MASTER,
                    0.1F, 1.0F + mc.level.getRandom().nextFloat() * 0.2F, false);
        }
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
