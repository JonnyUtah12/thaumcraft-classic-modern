package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

    /** TODO(M6 world/magic plants): client biome view update, per original PacketBiomeChange. */
    static void biomeChange(final int x, final int z, final ResourceLocation biome) {
        LOGGER.debug("biome change at ({}, {}) -> {}", x, z, biome);
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
    // TC-M7-02-s wires the two block-position particle effects whose original
    // shape is a particle cloud (sparkle, crucible bubble). The beam/bolt
    // family below is not a particle effect in the original (FXLightningBolt,
    // FXBeam* and the wisp family are world-rendered beams), and the
    // dig/essentia/vis/infusion families ride the same plan+spawn seam, so
    // they keep their own follow-up cards.

    /**
     * TC-M7-02-s WIRED: the original's {@code PacketFXBlockBubble} client
     * handler runs four jittered {@code ClientProxy.crucibleBubble} calls per
     * {@code particleCount(1)} iteration and hands each to the particle engine
     * (PacketFXBlockBubble.java:44-79). The plan is
     * {@code TcFxPlans.blockBubble}, the spawn is {@code TcEffects}.
     */
    static void fxBlockBubble(final BlockPos pos, final int color) {
        LOGGER.debug("fx block bubble at {} color={}", pos, color);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcEffects.blockBubble(mc.level, pos, color, particleSetting(), RandomSource.create());
    }

    /**
     * TC-M7-02-s WIRED: the original's {@code PacketFXBlockSparkle} client
     * handler is one {@code ClientProxy.blockSparkle(world, x, y, z, c, 7)}
     * call (PacketFXBlockSparkle.java:40-42, ClientProxy.java:576-608) — the
     * generic sparkle every vis sender in the original routes through.
     */
    static void fxBlockSparkle(final BlockPos pos, final int color) {
        LOGGER.debug("fx block sparkle at {} color={}", pos, color);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcEffects.blockSparkle(mc.level, pos, color, particleSetting(), RandomSource.create());
    }

    /**
     * The vanilla particle setting the original reads as
     * {@code Minecraft.getMinecraft().gameSettings.field_74362_aa}
     * (ClientProxy.java:949-955): ALL=0, DECREASED=1, MINIMAL=2, same ids in
     * 1.21.1's {@code ParticleStatus}.
     */
    private static int particleSetting() {
        return Minecraft.getInstance().options.particles().get().getId();
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
