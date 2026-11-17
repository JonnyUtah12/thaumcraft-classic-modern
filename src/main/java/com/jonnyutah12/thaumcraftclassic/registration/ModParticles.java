package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaTrailOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoreSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcVisSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSonicOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBurstOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcDropOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcVentOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSwarmOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSmokeTrailOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSmokeSpiralOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcScorchOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcArcOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBeamOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoltOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcGlowSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcGolemBeamOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcParticleType;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcRuneOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcShieldOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWardOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispArcingOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispEgOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcWispOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle type registration holder (TC-M2-01 registration architecture).
 *
 * <p>The original's visual effects were network-driven FX renderers
 * (TC-FX-*), not particle types. Where the port expresses them as vanilla
 * particles (M6/M7 visuals), the particle types register here; the FX
 * payloads themselves are already ported (TC-M2-03).</p>
 *
 * <p>Because the original created an {@code EntityFX} subclass on the client,
 * its effects had no registry ids — docs/content-mapping.csv has no particle
 * row for them. 1.21.1 requires a registered {@link ParticleType} for every
 * spawn, since the id is what {@code Level#addParticle} resolves, so the
 * ported effects register here and the spawn geometry stays in
 * {@code client.fx.TcFxPlans}.</p>
 *
 * <p>TC-M7-02-s lands the two block-position effects whose original shape is a
 * particle cloud: {@code thaumcraft:sparkle} (the generic sparkle of
 * {@code ClientProxy.blockSparkle}, sent by {@code PacketFXBlockSparkle}) and
 * {@code thaumcraft:bubble} (the crucible bubble of
 * {@code ClientProxy.crucibleBubble}, sent by {@code PacketFXBlockBubble}).</p>
 *
 * <p>TC-M7-02-b-s lands the world-rendered bolt/beam family: the originals
 * ({@code FXLightningBolt}, {@code FXBeam}, {@code FXBeamGolemBoss},
 * {@code FXArc}) drew immediate-mode GL in a vanilla {@code RenderGlobal}
 * pass, which 1.21.1 has no direct equivalent of; the port draws them as
 * custom-geometry particles on the particle sheet — the seam and its
 * constants are BD-16 in docs/BEHAVIORAL_DEVIATIONS.md. The registered types:
 * {@code thaumcraft:bolt} (the fractal bolt of {@code ClientProxy.nodeBolt},
 * sent by {@code PacketFXBlockZap}, one particle per render pass),
 * {@code thaumcraft:beam} (the entity-targeted beam of
 * {@code PacketFXBeamPulse}), {@code thaumcraft:beam_impact} (the
 * golem-boss impact sprite of {@code FXBeamGolemBoss.renderImpact}),
 * {@code thaumcraft:beam_golem_boss} (the boss-head beam of
 * {@code PacketFXBeamPulseGolemBoss}), {@code thaumcraft:arc} (the block arc
 * of {@code PacketFXBlockArc}) and {@code thaumcraft:sparkle_glow} (the
 * arc's companion glow sparkle of {@code ClientProxy.arcLightning}).</p>
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ThaumcraftClassic.MOD_ID);

    /**
     * TC-M7-02-c-s-f1: the wisp (the original's {@code FXWisp}, spawned by
     * {@code ClientProxy.wispFX/wispFX2/wispFX3/wispFX4}). Motion is fully
     * self-integrated: the original applies its own drags (0.985 homing /
     * 0.98 free / 0.7 ground) inside {@code onUpdate}, so the platform must
     * not add relative velocity on top.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWispOptions>> WISP =
            PARTICLE_TYPES.register("wisp",
                    () -> new TcParticleType<>(false, TcWispOptions.MAP_CODEC, TcWispOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f1: the EG (essentia-gas) wisp ({@code FXWispEG}, {@code ClientProxy.wispFXEG}). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWispEgOptions>> WISP_EG =
            PARTICLE_TYPES.register("wisp_eg",
                    () -> new TcParticleType<>(false, TcWispEgOptions.MAP_CODEC, TcWispEgOptions.STREAM_CODEC));

    /**
     * TC-M7-02-c-s-f1: the arcing wisp ({@code FXWispArcing},
     * {@code ClientProxy.sourceStreamFX}). The payload carries a delta, not a
     * velocity, so velocity relevance is off for the same reason.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWispArcingOptions>> WISP_ARCING =
            PARTICLE_TYPES.register("wisp_arcing",
                    () -> new TcParticleType<>(false, TcWispArcingOptions.MAP_CODEC,
                            TcWispArcingOptions.STREAM_CODEC));

    /**
     * TC-M7-02-s: the ported generic sparkle. {@code isVelocityRelevant} is
     * false because the original integrates the motion it was handed itself
     * (FXGeneric sets its own motion fields), so the platform must not add
     * relative velocity on top.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcSparkleOptions>> SPARCLE =
            PARTICLE_TYPES.register("sparkle",
                    () -> new TcParticleType<>(false, TcSparkleOptions.MAP_CODEC, TcSparkleOptions.STREAM_CODEC));

    /** TC-M7-02-s: the ported crucible bubble (motion is fully self-integrated). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBubbleOptions>> BUBBLE =
            PARTICLE_TYPES.register("bubble",
                    () -> new TcParticleType<>(false, TcBubbleOptions.MAP_CODEC, TcBubbleOptions.STREAM_CODEC));

    /**
     * TC-M7-02-b-s: the ported fractal bolt (one particle per render pass;
     * the geometry is rebuilt from the payload seed, TcBoltGeometry).
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBoltOptions>> BOLT =
            PARTICLE_TYPES.register("bolt",
                    () -> new TcParticleType<>(false, TcBoltOptions.MAP_CODEC, TcBoltOptions.STREAM_CODEC));

    /** TC-M7-02-b-s: the ported entity-targeted beam (FXBeam). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBeamOptions>> BEAM =
            PARTICLE_TYPES.register("beam",
                    () -> new TcParticleType<>(false, TcBeamOptions.MAP_CODEC, TcBeamOptions.STREAM_CODEC));

    /** TC-M7-02-b-s: the golem-boss impact sprite (FXBeamGolemBoss.renderImpact). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBeamOptions>> BEAM_IMPACT =
            PARTICLE_TYPES.register("beam_impact",
                    () -> new TcParticleType<>(false, TcBeamOptions.MAP_CODEC, TcBeamOptions.STREAM_CODEC));

    /** TC-M7-02-b-s: the boss-head beam pair (FXBeamGolemBoss). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcGolemBeamOptions>> BEAM_GOLEM_BOSS =
            PARTICLE_TYPES.register("beam_golem_boss",
                    () -> new TcParticleType<>(false, TcGolemBeamOptions.MAP_CODEC, TcGolemBeamOptions.STREAM_CODEC));

    /** TC-M7-02-b-s: the ported block arc (FXArc). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcArcOptions>> ARC =
            PARTICLE_TYPES.register("arc",
                    () -> new TcParticleType<>(false, TcArcOptions.MAP_CODEC, TcArcOptions.STREAM_CODEC));

    /** TC-M7-02-b-s: the arc's companion glow sparkle (FXSparkle type 6). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcGlowSparkleOptions>> SPARKLE_GLOW =
            PARTICLE_TYPES.register("sparkle_glow",
                    () -> new TcParticleType<>(false, TcGlowSparkleOptions.MAP_CODEC,
                            TcGlowSparkleOptions.STREAM_CODEC));

    /**
     * TC-M7-02-c-s-f4: the ward hexagram ({@code FXBlockWard}, sent by
     * {@code ClientProxy.blockWard} from the ward blocks' hit/destroy effects).
     * Velocity relevance is off: the mote has no gravity and no motion
     * (FXBlockWard.java:21-26), so the platform must not add relative velocity
     * on top of the baked-in face offset.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWardOptions>> WARD =
            PARTICLE_TYPES.register("ward",
                    () -> new TcParticleType<>(false, TcWardOptions.MAP_CODEC, TcWardOptions.STREAM_CODEC));

    /**
     * TC-M7-02-c-s-f4: the shield runes ({@code FXShieldRunes}, sent by
     * {@code PacketFXShield}) — the hemis form, frames hemis1..hemis15. The
     * runes integrate their own motion (the constructor zeroes it and the tick
     * overwrites the position from the entity), so velocity relevance is off.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcShieldOptions>> SHIELD_RUNES =
            PARTICLE_TYPES.register("shield_runes",
                    () -> new TcParticleType<>(false, TcShieldOptions.MAP_CODEC,
                            TcShieldOptions.STREAM_CODEC));

    /**
     * TC-M7-02-c-s-f4: the shield runes' mob form — the ripple frames and the
     * 0.5 colour the original switches on inside its own draw
     * ({@code instanceof EntityMob && !(instanceof EntityCultist)},
     * FXShieldRunes.java:61-66). 1.21.1 picks the frame list at construction,
     * so the branch is a second type; {@link TcShieldOptions#getType()} routes
     * it, and the plain type keeps the hemis frames.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcShieldOptions>> SHIELD_RUNES_MOB =
            PARTICLE_TYPES.register("shield_runes_mob",
                    () -> new TcParticleType<>(false, TcShieldOptions.MAP_CODEC,
                            TcShieldOptions.STREAM_CODEC));

    // ---- TC-M7-02-c-s-f5: the shared particle families of the original's
    // client/fx/particles + fx/other packages. Each id names the ORIGINAL class
    // (D-017): thaumcraft:scorch is FXScorch, thaumcraft:swarm is FXSwarm, and
    // so on. Every type is self-integrated (the first argument false): all of
    // them move by hand in onUpdate, so the platform must not integrate them
    // twice.

    /** TC-M7-02-c-s-f5: the fireblast scorch streak (FXScorch). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcScorchOptions>> SCORCH =
            PARTICLE_TYPES.register("scorch",
                    () -> new TcParticleType<>(false, TcScorchOptions.MAP_CODEC,
                            TcScorchOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the boiler smoke spiral (FXSmokeSpiral). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcSmokeSpiralOptions>> SMOKE_SPIRAL =
            PARTICLE_TYPES.register("smoke_spiral",
                    () -> new TcParticleType<>(false, TcSmokeSpiralOptions.MAP_CODEC,
                            TcSmokeSpiralOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the entity-chasing smoke trail (FXSmokeTrail). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcSmokeTrailOptions>> SMOKE_TRAIL =
            PARTICLE_TYPES.register("smoke_trail",
                    () -> new TcParticleType<>(false, TcSmokeTrailOptions.MAP_CODEC,
                            TcSmokeTrailOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the vis swarm attendant (FXSwarm). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcSwarmOptions>> SWARM =
            PARTICLE_TYPES.register("swarm",
                    () -> new TcParticleType<>(false, TcSwarmOptions.MAP_CODEC,
                            TcSwarmOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the vent/steam puff (FXVent). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcVentOptions>> VENT =
            PARTICLE_TYPES.register("vent",
                    () -> new TcParticleType<>(false, TcVentOptions.MAP_CODEC,
                            TcVentOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the bore drill sparkle (FXBoreSparkle). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBoreSparkleOptions>> BORE_SPARKLE =
            PARTICLE_TYPES.register("bore_sparkle",
                    () -> new TcParticleType<>(false, TcBoreSparkleOptions.MAP_CODEC,
                            TcBoreSparkleOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the generic vis sparkle (FXVisSparkle). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcVisSparkleOptions>> VIS_SPARKLE =
            PARTICLE_TYPES.register("vis_sparkle",
                    () -> new TcParticleType<>(false, TcVisSparkleOptions.MAP_CODEC,
                            TcVisSparkleOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the essence trail ribbon (FXEssentiaTrail). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcEssentiaTrailOptions>> ESSENTIA_TRAIL =
            PARTICLE_TYPES.register("essentia_trail",
                    () -> new TcParticleType<>(false, TcEssentiaTrailOptions.MAP_CODEC,
                            TcEssentiaTrailOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the sonic ripple (FXSonic). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcSonicOptions>> SONIC =
            PARTICLE_TYPES.register("sonic",
                    () -> new TcParticleType<>(false, TcSonicOptions.MAP_CODEC,
                            TcSonicOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the vis droplet (FXDrop). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcDropOptions>> DROP =
            PARTICLE_TYPES.register("drop",
                    () -> new TcParticleType<>(false, TcDropOptions.MAP_CODEC,
                            TcDropOptions.STREAM_CODEC));

    /** TC-M7-02-c-s-f5: the node burst ring (FXBurst). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBurstOptions>> BURST =
            PARTICLE_TYPES.register("burst",
                    () -> new TcParticleType<>(false, TcBurstOptions.MAP_CODEC,
                            TcBurstOptions.STREAM_CODEC));

    /**
     * TC-M7-02-c-s-f3: the rune decal (the original's {@code FXBlockRunes},
     * spawned through {@code ClientProxy.blockRunes:974-978}) — the effect the
     * thaumometer scan, the warding stone's ambient loop, the Eldritch trap and
     * the running infusion matrix all send. Motion is zeroed by the ctor
     * (FXBlockRunes.java:25) and integrated by its own {@code onUpdate}, so
     * velocity is irrelevant, and its 16 frames are the {@code runeIndex % 16}
     * columns of one sheet row (lines 51-54), sliced at build time (D-004).
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcRuneOptions>> RUNE =
            PARTICLE_TYPES.register("rune",
                    () -> new TcParticleType<>(false, TcRuneOptions.MAP_CODEC, TcRuneOptions.STREAM_CODEC));

    private ModParticles() {
    }
}
