package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcArcOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBeamOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoltOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcGlowSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcGolemBeamOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcParticleType;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
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

    private ModParticles() {
    }
}
