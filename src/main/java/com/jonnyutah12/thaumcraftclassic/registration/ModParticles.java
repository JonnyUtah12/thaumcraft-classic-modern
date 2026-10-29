package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
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
 * {@code ClientProxy.crucibleBubble}, sent by {@code PacketFXBlockBubble}).
 * The bolt/beam family ({@code FXLightningBolt}, {@code FXBeam*}) and the wisp
 * family are not particles in the original — they are world-rendered beams —
 * and keep their own cards.</p>
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

    private ModParticles() {
    }
}
