package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSourceFx;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
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

    /**
     * TC-M7-02-c-s WIRED: the original's {@code PacketFXBlockDig} client
     * handler (PacketFXBlockDig.java:61-116): the numeric 1.7 block/item id
     * resolves to a modern registry object — an {@code ItemBlock} (block
     * branch) spawns block crumbs, everything else item crumbs — plus the
     * dig-face sounds. The reference tree has no server sender (the packet
     * is registered but never sent), so this is the client seam + probe.
     */
    static void fxBlockDig(final BlockPos targetPos, final ResourceLocation blockOrItemId, final int data,
            final BlockPos digFromPos) {
        LOGGER.debug("fx block dig at {} block={} data={} from={}", targetPos, blockOrItemId, data, digFromPos);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        final net.minecraft.world.level.block.Block block =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockOrItemId);
        final ItemStack stack;
        final net.minecraft.world.level.block.state.BlockState state;
        if (block != net.minecraft.world.level.block.Blocks.AIR) {
            // The original's `item instanceof ItemBlock && metadata == 0` branch.
            state = block.defaultBlockState();
            stack = null;
        } else {
            state = null;
            stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(blockOrItemId));
        }
        TcEffects.blockDig(mc.level, targetPos, digFromPos, state, stack,
                RandomSource.create(), particleSetting());
    }

    /**
     * TC-M7-02-c-s WIRED: the original's {@code PacketFXEssentiaSource}
     * client handler (PacketFXEssentiaSource.java:41-64) keeps a client-side
     * map of drain trails (source → consumer) that the client tick drains one
     * mote per tick ({@code ClientTickEventsFML.java:218-261}); a packet
     * lands/refreshes the entry with a 15-tick budget. The map lives in
     * {@link TcSourceFx}; the tick geometry is
     * {@link TcFxPlans#essentiaTrailTick}.
     */
    static void fxEssentiaSource(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx essentia source at {} offset={} color={}", pos, offset, color);
        TcSourceFx.onEssentiaSource(pos, offset, color);
    }

    /**
     * TC-M7-02-c-s WIRED: the original's {@code PacketFXInfusionSource}
     * client handler (PacketFXInfusionSource.java:41-70) registers a
     * per-matrix drain entry (15 ticks, 60 for a pedestal source) that the
     * matrix's own client tick draws from (TileInfusionMatrix.java:864-947).
     * Map + tick live in {@link TcSourceFx}.
     */
    static void fxInfusionSource(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx infusion source at {} offset={} color={}", pos, offset, color);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcSourceFx.onInfusionSource(mc.level, pos, offset, color);
    }

    /**
     * TC-M7-02-c-s WIRED: the original's {@code PacketFXShield} client
     * handler (PacketFXShield.java:44-90): runes ride the source entity
     * (the shielded one); {@code target >= 0} aims them from the source at
     * the target entity's AABB centre, {@code -1} spawns a pair (pitch 90
     * and 270), {@code -2}/{@code -3} one rune at 270/90. The mob branch
     * (ripple frames) is the packet entity being a mob; the 1.7.10
     * EntityCultist exception is pending the TC-M7-03 entity slice.
     */
    static void fxShield(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx shield {} -> {}", sourceEntityId, targetEntityId);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        final net.minecraft.world.entity.Entity p = mc.level.getEntity(sourceEntityId);
        if (p == null) {
            return; // the original bails when the source entity is absent
        }
        final boolean mob = p instanceof net.minecraft.world.entity.Mob;
        if (targetEntityId >= 0) {
            float yaw = 0.0F;
            float pitch = 90.0F;
            final net.minecraft.world.entity.Entity t = mc.level.getEntity(targetEntityId);
            if (t != null) {
                final double d0 = p.getX() - t.getX();
                final double d1 = (p.getBoundingBox().minY + p.getBoundingBox().maxY) / 2.0
                        - (t.getBoundingBox().minY + t.getBoundingBox().maxY) / 2.0;
                final double d2 = p.getZ() - t.getZ();
                final double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));
                yaw = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
                pitch = (float) (-Math.atan2(d1, d3) * 180.0 / Math.PI);
            }
            TcEffects.shieldRune(mc.level, sourceEntityId, yaw, pitch, mob);
        } else if (targetEntityId == -1) {
            TcEffects.shieldRune(mc.level, sourceEntityId, 0.0F, 90.0F, mob);
            TcEffects.shieldRune(mc.level, sourceEntityId, 0.0F, 270.0F, mob);
        } else if (targetEntityId == -2) {
            TcEffects.shieldRune(mc.level, sourceEntityId, 0.0F, 270.0F, mob);
        } else if (targetEntityId == -3) {
            TcEffects.shieldRune(mc.level, sourceEntityId, 0.0F, 90.0F, mob);
        }
    }

    /**
     * TC-M7-02-c-s WIRED: the original's {@code PacketFXSonic} client
     * handler (PacketFXSonic.java:44-49): one 10-tick ripple pulse riding
     * the source entity's head (the guardian/warden roar).
     */
    static void fxSonic(final int sourceEntityId) {
        LOGGER.debug("fx sonic at {}", sourceEntityId);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (mc.level.getEntity(sourceEntityId) == null) {
            return; // the original bails when the source entity is absent
        }
        TcEffects.sonicPulse(mc.level, sourceEntityId);
    }

    /**
     * TC-M7-02-c-s WIRED: the original's {@code PacketFXVisDrain} client
     * handler (PacketFXVisDrain.java:44-58): one vis mote from the source
     * block corner (the packet's dx/dy/dz) homing at the consumer block
     * (x/y/z) — the aura node vis siphon.
     */
    static void fxVisDrain(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx vis drain at {} offset={} color={}", pos, offset, color);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // The packet's offset carries the source (dx,dy,dz), pos the target.
        TcEffects.visDrain(mc.level, offset.getX(), offset.getY(), offset.getZ(),
                pos.getX(), pos.getY(), pos.getZ(), color, RandomSource.create());
    }

    /**
     * TC-M7-02-c-s WIRED (sender half): the original's
     * {@code PacketFXWispZap.onMessage} (PacketFXWispZap.java:41-46) resolves
     * the source and target entities and calls
     * {@code Thaumcraft.proxy.bolt(world, source, target)} — the same
     * {@code FXLightningBolt} the zap packet builds (PacketFXZap.java:44-62).
     * The bolt is world-rendered line-strip geometry, so the plan (endpoints +
     * bolt parameters, the reference-measurable half) goes to
     * {@link TcEffects#bolt} and the strip itself is TC-M7-02-b-s's renderer.
     */
    static void fxWispZap(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx wisp zap {} -> {}", sourceEntityId, targetEntityId);
        boltBetween(sourceEntityId, targetEntityId);
    }

    /**
     * TC-M7-02-c-s WIRED (sender half): {@code PacketFXZap.onMessage}
     * (PacketFXZap.java:44-62) — the entity-to-entity lightning bolt, the same
     * {@code proxy.bolt} call as the wisp zap. See {@link #fxWispZap}.
     */
    static void fxZap(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx zap {} -> {}", sourceEntityId, targetEntityId);
        boltBetween(sourceEntityId, targetEntityId);
    }

    /**
     * {@code Thaumcraft.proxy.bolt}: both entities' AABB centres
     * ({@code e.posX, e.getEntityBoundingBox().minY + e.height / 2, e.posZ})
     * plus a {@code world.rand.nextLong()} seed. The original bails when either
     * entity is missing.
     */
    private static void boltBetween(final int sourceEntityId, final int targetEntityId) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        final Entity source = mc.level.getEntity(sourceEntityId);
        final Entity target = mc.level.getEntity(targetEntityId);
        if (source == null || target == null) {
            return;
        }
        TcEffects.bolt(mc.level, TcFxPlans.boltPlan(
                source.getX(), source.getBoundingBox().minY + source.getBbHeight() / 2.0, source.getZ(),
                target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0, target.getZ(),
                mc.level.getRandom().nextLong()));
    }

    static void fxBeamPulse(final int sourceEntityId, final int targetEntityId, final int color) {
        LOGGER.debug("fx beam pulse {} -> {} color={}", sourceEntityId, targetEntityId, color);
    }

    static void fxBeamPulseGolemBoss(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx golem boss beam pulse {} -> {}", sourceEntityId, targetEntityId);
    }
}
