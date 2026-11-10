package com.jonnyutah12.thaumcraftclassic.network;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBeamGeometry;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBeamParticle;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEffects;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcFxPlans;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWispRules;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
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

    // --- the wisp family (original ClientProxy.wispFX / wispFXEG / sourceStreamFX) ---
    // TC-M7-02-c-s-f1c wires them. These three senders are NOT packet handlers
    // in the original: they are client proxy calls the aura-node renderer, the
    // essentia sources and the wisp entity make directly
    // (ClientProxy.java:715-757), so there is no payload to route and nothing
    // to receive. The hooks below are the seam those client-side senders call;
    // their plan is {@code TcFxPlans.wisp}/{@code wispByType}/{@code wispEg}/
    // {@code wispArcing} and the spawn is {@link TcEffects#spawn}.

    /**
     * TC-M7-02-c-s-f1c WIRED: the original's {@code ClientProxy.wispFX}
     * explicit-colour sender, i.e. {@code new FXWisp(world, x, y, z, scale, r,
     * g, b)} and its point-target sibling (FXWisp.java:20-60, 117-125). The
     * mote is spawned at {@code spawn}; passing {@code spawn} as {@code target}
     * gives the free-drifting form, a different point gives the original's
     * {@code (target - spawn) / lifetime} initial velocity, and a
     * {@code targetEntityId} other than {@code TcWispOptions.NO_TARGET} gives
     * the homing form.
     */
    static void wispFx(final Vec3 spawn, final Vec3 target, final float scale, final int color,
            final boolean additive, final int targetEntityId, final boolean shrink,
            final float gravity, final boolean clip) {
        LOGGER.debug("fx wisp at {} scale={} color={}", spawn, scale, Integer.toHexString(color));
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcEffects.spawn(mc.level, TcFxPlans.wisp(spawn, target, scale, color, additive,
                targetEntityId, shrink, gravity, clip, wispCulled(spawn), particleSetting(),
                RandomSource.create()));
    }

    /**
     * TC-M7-02-c-s-f1c WIRED: the original's typed wisp sender — the
     * {@code Entity}-target constructor {@code new FXWisp(world, x, y, z, tar,
     * type)} (FXWisp.java:115-118), whose type switch picks both the colour and
     * the blend layer. {@code type} is the sender's flavour (0 wisp purple …
     * 7 fiery); {@code TcWispOptions.NO_TARGET} spawns it without a homing
     * target.
     */
    static void wispFxType(final Vec3 spawn, final int type, final int targetEntityId,
            final boolean shrink, final float gravity, final boolean clip) {
        LOGGER.debug("fx typed wisp at {} type={} target={}", spawn, type, targetEntityId);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcEffects.spawn(mc.level, TcFxPlans.wispByType(spawn, type, targetEntityId, shrink, gravity,
                clip, wispCulled(spawn), particleSetting(), RandomSource.create()));
    }

    /**
     * TC-M7-02-c-s-f1c WIRED: the original's {@code ClientProxy.wispFXEG}
     * sender — the dark additive mote that falls onto an entity
     * ({@code new FXWispEG(world, x, y, z, target)}, FXWispEG.java:18-49).
     */
    static void wispFxEg(final Vec3 spawn, final int targetEntityId) {
        LOGGER.debug("fx EG wisp at {} target={}", spawn, targetEntityId);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcEffects.spawn(mc.level, TcFxPlans.wispEg(spawn, targetEntityId, wispCulled(spawn),
                particleSetting(), RandomSource.create()));
    }

    /**
     * TC-M7-02-c-s-f1c WIRED: the original's {@code ClientProxy.sourceStreamFX}
     * sender — the arcing wisp of an essentia stream, constructed from two
     * points and the aspect tag's colour (ClientProxy.java:752-757,
     * FXWispArcing.java:51-59). {@code from} is where the mote appears,
     * {@code to} the point it converges on as it ages; {@code color} is the
     * aspect tag's colour, packed 0x00RRGGBB.
     */
    static void sourceStreamFx(final Vec3 from, final Vec3 to, final int color) {
        LOGGER.debug("fx source stream {} -> {} color={}", from, to, Integer.toHexString(color));
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TcEffects.spawn(mc.level, TcFxPlans.wispArcing(to, from, color, wispCulled(to),
                particleSetting(), RandomSource.create()));
    }

    /**
     * The wisp family's spawn cull: the original reads the render view entity's
     * distance to the spawn point against a 50 (fancy) / 25 (fast) visible
     * distance and zeroes the lifetime beyond it, so nothing appears
     * (FXWisp.java:40-48, FXWispEG.java:35-41, FXWispArcing.java:40-48).
     */
    private static boolean wispCulled(final Vec3 spawn) {
        final Vec3 viewer = viewerPosition();
        return FxWispRules.culledAtSpawn(spawn.x, spawn.y, spawn.z, viewer.x, viewer.y, viewer.z,
                FxWispRules.visibleDistance(TcBeamParticle.fancyGraphics()));
    }

    /**
     * The render view entity's position — the original's {@code renderentity}
     * for the wisp cull and its {@code getDistance}-based gates
     * (FXWisp.java:41-47). 1.21.1's camera entity is the same view point.
     */
    private static Vec3 viewerPosition() {
        final Minecraft mc = Minecraft.getInstance();
        final Entity viewEntity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        return viewEntity == null
                ? Vec3.ZERO
                : new Vec3(viewEntity.getX(), viewEntity.getY(), viewEntity.getZ());
    }

    /**
     * TC-M7-02-b-s WIRED: the original's {@code PacketFXBlockArc} client
     * handler resolves the source entity, rolls the arc colour (the
     * {@code EntityCultistPortal} branch stays plan-visible; the boss family
     * is on another card, so the port passes false), and runs
     * {@code proxy.arcLightning} — the glow sparkle at the target plus the
     * jittered arc (PacketFXBlockArc.java:41-66, ClientProxy.java:1301-1309).
     */
    static void fxBlockArc(final BlockPos pos, final int sourceEntityId) {
        LOGGER.debug("fx block arc at {} source={}", pos, sourceEntityId);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        final Entity source = mc.level.getEntity(sourceEntityId);
        if (source == null) {
            return;
        }
        // Handler source point: entity x/z, bounding-box bottom + width/2
        // (PacketFXBlockArc.java:56-58).
        final Vec3 sourcePoint = new Vec3(source.getX(),
                source.getBoundingBox().minY + source.getBbWidth() / 2.0, source.getZ());
        // Target point: block center, pos.getY() + 1 (PacketFXBlockArc.java:61-63).
        TcEffects.spawn(mc.level, TcFxPlans.blockArc(sourcePoint,
                pos.getX() + 0.5, pos.getY() + ARC_TARGET_Y, pos.getZ() + 0.5,
                mc.level.getRandom(), false));
    }

    /** The arc target's Y offset of {@code PacketFXBlockArc} (message.y + 1). */
    private static final double ARC_TARGET_Y = 1.0;

    /**
     * TC-M7-05 sound half WIRED + TC-M7-02-b-s particle half WIRED: the
     * original PacketFXBlockZap client handler runs {@code proxy.nodeBolt}
     * (the seed draw) and then plays {@code thaumcraft:zap} at the block
     * position (volume 0.1, pitch 1.0 + rand*0.2) — the draw order of the
     * original handler (PacketFXBlockZap.java:45-50).
     */
    static void fxBlockZap(final Vec3 pos, final Vec3 direction) {
        LOGGER.debug("fx block zap at {} dir={}", pos, direction);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // nodeBolt first: world.rand.nextLong() seeds the fractal
        // (ClientProxy.java:789-796), before the sound's pitch draw.
        final long seed = mc.level.getRandom().nextLong();
        TcEffects.spawn(mc.level, TcFxPlans.blockZapBolt(pos,
                pos.add(direction.x, direction.y, direction.z), seed,
                viewDistance(pos), TcBeamParticle.fancyGraphics()));
        mc.level.playLocalSound(pos.x, pos.y, pos.z,
                ModSounds.ZAP.value(), SoundSource.MASTER,
                0.1F, 1.0F + mc.level.getRandom().nextFloat() * 0.2F, false);
    }

    /**
     * TC-M7-02-c-s-f5: the one seam every wired effect handler shares — the
     * plan's spawns go to the client level's particle engine, the way the
     * original's {@code FXHandler.onEffect} loop ends in {@code addEffect}. On a
     * dedicated server there is no client level and the payload never reaches
     * this handler: the original's whole FX path sits behind the client gate
     * ({@code world.field_72995_K}).
     */
    private static void spawnRaw(final net.minecraft.client.multiplayer.ClientLevel level,
            final com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn spawn) {
        level.addParticle(spawn.options(), spawn.x(), spawn.y(), spawn.z(),
                spawn.vx(), spawn.vy(), spawn.vz());
    }

    private static void spawnFx(final net.minecraft.client.multiplayer.ClientLevel level,
            final java.util.List<com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn> spawns) {
        if (level == null) {
            return;
        }
        for (com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn spawn : spawns) {
            spawnRaw(level, spawn);
        }
    }


    static void fxBlockDig(final BlockPos targetPos, final ResourceLocation blockOrItemId, final int data,
            final BlockPos digFromPos) {
        LOGGER.debug("fx block dig at {} block={} data={} from={}", targetPos, blockOrItemId, data, digFromPos);
        // TC-M7-02-c-s-f5 WIRED: the bore's dig dust. The original's
        // PacketFXBlockDig client handler (PacketFXBlockDig.java:44-59) builds
        // FXBoreParticles and FXBoreSparkle; 1.21.1 has no per-block face crumb,
        // so the crumb maps to the platform's own block dust and the sparkle
        // keeps thaumcraft:bore_sparkle (cells 64..67, FXBoreSparkle.java:56-59).
        final Minecraft digMc = Minecraft.getInstance();
        if (digMc.level != null) {
            spawnFx(digMc.level, TcFxPlans.blockDig(targetPos, digFromPos,
                    digMc.level.getBlockState(targetPos), ItemStack.EMPTY,
                    digMc.level.getRandom(), particleSetting()));
        }
    }

    static void fxEssentiaSource(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx essentia source at {} offset={} color={}", pos, offset, color);
        // TC-M7-02-c-s-f5 WIRED: the pedestal source mote (PacketFXEssentiaSource,
        // ClientProxy.java:925-941) — the trail into the amulet's cell plus the
        // bore sparkle on it. thaumcraft:essentia_trail carries the trail's single
        // sprite cell; the pedestal's aura stack is the tile's own, and EMPTY
        // stands in on a client that has not synced the tile.
        final Minecraft essMc = Minecraft.getInstance();
        if (essMc.level != null) {
            spawnFx(essMc.level, TcFxPlans.infusionPedestalTick(
                    pos.getX() + offset.getX(), pos.getY() + offset.getY(),
                    pos.getZ() + offset.getZ(), pos.getX(), pos.getY(), pos.getZ(),
                    essMc.level.getBlockState(pos), ItemStack.EMPTY,
                    particleSetting(), essMc.level.getRandom()));
        }
    }

    static void fxInfusionSource(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx infusion source at {} offset={} color={}", pos, offset, color);
        // TC-M7-02-c-s-f5 WIRED: the infusion source mote at the player's face
        // (PacketFXInfusionSource, ClientProxy.java:980-1000) — the trail from the
        // amulet cell to the receiver, with the bore sparkle at the cell.
        final Minecraft infMc = Minecraft.getInstance();
        if (infMc.level != null) {
            final net.minecraft.world.phys.Vec3 eye = infMc.player == null
                    ? pos.getCenter() : infMc.player.getEyePosition();
            spawnFx(infMc.level, TcFxPlans.infusionPlayerTick(
                    pos.getX() + offset.getX(), pos.getY() + offset.getY(),
                    pos.getZ() + offset.getZ(), eye.x, eye.y, eye.z,
                    infMc.player == null ? 1.0 : infMc.player.getBbWidth(),
                    infMc.player == null ? 2.0 : infMc.player.getBbHeight(),
                    particleSetting(), infMc.level.getRandom()));
        }
    }

    static void fxShield(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx shield {} -> {}", sourceEntityId, targetEntityId);
        // TC-M7-02-c-s-f5 WIRED: the shield's burst. The original's PacketFXShield
        // builds an FXBurst (ClientProxy.java:747-751), the node-burst ring;
        // thaumcraft:burst carries its 32-frame strip (FXBurst.java:31).
        final Minecraft shieldMc = Minecraft.getInstance();
        final Entity shieldSource = shieldMc.level == null ? null
                : shieldMc.level.getEntity(sourceEntityId);
        if (shieldSource != null) {
            spawnFx(shieldMc.level, TcFxPlans.burst(shieldSource.getX(), shieldSource.getY(),
                    shieldSource.getZ(), 1.0F));
        }
    }

    static void fxSonic(final int sourceEntityId) {
        LOGGER.debug("fx sonic at {}", sourceEntityId);
        // TC-M7-02-c-s-f5 PARTIAL: the family is landed — thaumcraft:sonic,
        // TcSonicParticle/Options, the 15 ripple frames (fx/other/FXSonic.java:
        // 34-36,62) and its sprite set. The sender seam stays open: the plan
        // builder (the boom's per-tick expansion, FXSonic.java:62) is the
        // follow-up; see card TC-M7-02-c-s-f5's handoff.
    }

    static void fxVisDrain(final BlockPos pos, final BlockPos offset, final int color) {
        LOGGER.debug("fx vis drain at {} offset={} color={}", pos, offset, color);
        // TC-M7-02-c-s-f5 PARTIAL: the family is landed — thaumcraft:vis_sparkle,
        // TcVisSparkleParticle/Options, cells 128..143 (FXVisSparkle.java:49-52)
        // and its sprite set. The drain sender's own loop (PacketFXVisDrain:26-32,
        // per ClientProxy.visSparkleFX) needs its plan builder; follow-up.
    }

    static void fxWispZap(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx wisp zap {} -> {}", sourceEntityId, targetEntityId);
        // TC-M7-02-c-s-f5 WIRED: the wisp zap's swarm mote (PacketFXWispZap,
        // ClientProxy.swarmParticleFX at ClientProxy.java:987-1000) — the buzzer
        // that homes on the target. thaumcraft:swarm carries its 8-frame strip
        // (cells 71..78, FXSwarm.java:56-59).
        final Minecraft wispMc = Minecraft.getInstance();
        final Entity wispSource = wispMc.level == null ? null
                : wispMc.level.getEntity(sourceEntityId);
        if (wispSource != null) {
            spawnFx(wispMc.level, TcFxPlans.swarm(targetEntityId, wispSource.getX(),
                    wispSource.getY(), wispSource.getZ(), 0x547FFF, wispMc.level.getRandom()));
        }
    }

    static void fxZap(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx zap {} -> {}", sourceEntityId, targetEntityId);
        // TC-M7-02-c-s-f5 WIRED: the zap's soul trail (PacketFXZap,
        // ClientProxy.soulTrail at ClientProxy.java:916-941) — the smoke motes
        // that stream off the source entity toward its target.
        // thaumcraft:smoke_trail carries cells 1..5 (FXSmokeTrail.java:52-55).
        final Minecraft zapMc = Minecraft.getInstance();
        final Entity zapSource = zapMc.level == null ? null
                : zapMc.level.getEntity(sourceEntityId);
        if (zapSource != null) {
            spawnFx(zapMc.level, TcFxPlans.soulTrail(targetEntityId, zapSource.getX(),
                    zapSource.getY(), zapSource.getZ(), zapSource.getBbWidth(),
                    zapSource.getBbHeight(), zapMc.level.getRandom()));
        }
    }

    /**
     * TC-M7-02-b-s WIRED: the original's {@code PacketFXBeamPulse} client
     * handler builds one {@code FXBeam} at the source entity's eye position
     * aimed at the live target (PacketFXBeamPulse.java:38-58). The 50/25
     * view-distance gate (FXBeam.java:66-73) is applied in the plan.
     */
    static void fxBeamPulse(final int sourceEntityId, final int targetEntityId, final int color) {
        LOGGER.debug("fx beam pulse {} -> {} color={}", sourceEntityId, targetEntityId, color);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        final Entity source = mc.level.getEntity(sourceEntityId);
        if (source == null || mc.level.getEntity(targetEntityId) == null) {
            // The original forces particleMaxAge = 0 for a dead target
            // (FXBeam.java:55-64); the beam never appears.
            return;
        }
        // Source eye position (PacketFXBeamPulse.java:40-42).
        final Vec3 sourceEye = new Vec3(source.getX(), source.getY() + source.getEyeHeight(),
                source.getZ());
        TcEffects.spawn(mc.level, TcFxPlans.beamPulse(sourceEye, targetEntityId, color,
                viewDistance(sourceEye), TcBeamParticle.fancyGraphics()));
    }

    /**
     * TC-M7-02-b-s WIRED: the original's {@code PacketFXBeamPulseGolemBoss}
     * client handler builds the two-boss-beam pair from the boss's head
     * anchor (PacketFXBeamPulseGolemBoss.java:38-60, FXBeamGolemBoss.java:39-52).
     */
    static void fxBeamPulseGolemBoss(final int sourceEntityId, final int targetEntityId) {
        LOGGER.debug("fx golem boss beam pulse {} -> {}", sourceEntityId, targetEntityId);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        final Entity boss = mc.level.getEntity(sourceEntityId);
        if (boss == null || mc.level.getEntity(targetEntityId) == null) {
            return;
        }
        final double[] head = TcBeamGeometry.headAnchor(boss.getX(), boss.getY(), boss.getZ(),
                boss.getYRot(), boss.getXRot(), boss.getEyeHeight());
        final Vec3 headAnchor = new Vec3(head[0], head[1], head[2]);
        TcEffects.spawn(mc.level, TcFxPlans.beamPulseGolemBoss(headAnchor, sourceEntityId,
                targetEntityId, viewDistance(headAnchor), TcBeamParticle.fancyGraphics()));
    }

    /**
     * The render view entity's distance to a point — the original reads
     * {@code renderentity.getDistance(...)} for the beam/bolt gates
     * (FXBeam.java:66-73, FXLightningBolt.java:155-159); 1.21.1's camera
     * entity is the same view point.
     */
    private static double viewDistance(final Vec3 point) {
        final Minecraft mc = Minecraft.getInstance();
        final Entity viewEntity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        if (viewEntity == null) {
            return 0.0;
        }
        return Math.sqrt(Math.pow(viewEntity.getX() - point.x, 2)
                + Math.pow(viewEntity.getY() - point.y, 2)
                + Math.pow(viewEntity.getZ() - point.z, 2));
    }
}
