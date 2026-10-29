package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBoreSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcBubbleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcEssentiaTrailOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcParticleType;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcShieldOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSonicOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcSparkleOptions;
import com.jonnyutah12.thaumcraftclassic.client.fx.TcVisSparkleOptions;
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

    // ---- TC-M7-02-c-s: the wisp family ----

    /**
     * TC-M7-02-c-s: the wisp family member (FXWisp) — one type carries
     * wispFX/wispFX2/wispFX3/wispFX4 via
     * {@link TcWispOptions} (colour, gravity, lifetime, straight-line
     * motion, tracking target, shrink/tinkle/additive).
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWispOptions>> WISP =
            PARTICLE_TYPES.register("wisp",
                    () -> new TcParticleType<>(false, TcWispOptions.MAP_CODEC, TcWispOptions.STREAM_CODEC));

    /** TC-M7-02-c-s: the EnderGoo wisp (FXWispEG, rides its target's velocity). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWispEgOptions>> WISP_EG =
            PARTICLE_TYPES.register("wisp_eg",
                    () -> new TcParticleType<>(false, TcWispEgOptions.MAP_CODEC, TcWispEgOptions.STREAM_CODEC));

    /** TC-M7-02-c-s: the arcing stream wisp (FXWispArcing, parametric flight). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcWispArcingOptions>> WISP_ARCING =
            PARTICLE_TYPES.register("wisp_arcing",
                    () -> new TcParticleType<>(false, TcWispArcingOptions.MAP_CODEC,
                            TcWispArcingOptions.STREAM_CODEC));

    // ---- TC-M7-02-c-s: the sender-spawned motes ----

    /** TC-M7-02-c-s: the vis-drain mote (FXVisSparkle, homes at 0.1/tick). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcVisSparkleOptions>> VIS_SPARKLE =
            PARTICLE_TYPES.register("vis_sparkle",
                    () -> new TcParticleType<>(false, TcVisSparkleOptions.MAP_CODEC,
                            TcVisSparkleOptions.STREAM_CODEC));

    /** TC-M7-02-c-s: the infusion draw mote (FXBoreSparkle, homes at 0.3/0.6). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcBoreSparkleOptions>> BORE_SPARKLE =
            PARTICLE_TYPES.register("bore_sparkle",
                    () -> new TcParticleType<>(false, TcBoreSparkleOptions.MAP_CODEC,
                            TcBoreSparkleOptions.STREAM_CODEC));

    /** TC-M7-02-c-s: the essentia drain trail mote (FXEssentiaTrail, rises). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcEssentiaTrailOptions>> ESSENTIA_TRAIL =
            PARTICLE_TYPES.register("essentia_trail",
                    () -> new TcParticleType<>(false, TcEssentiaTrailOptions.MAP_CODEC,
                            TcEssentiaTrailOptions.STREAM_CODEC));

    // ---- TC-M7-02-c-s: the entity-space effects ----

    /**
     * TC-M7-02-c-s: the shield runes for a non-mob owner (hemis frames).
     * The original picked hemis vs ripple at render time
     * ({@code FXShieldRunes.java:61-66}); 1.21.1 binds a frame set at
     * registration, so the branch is two particle types.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcShieldOptions>> SHIELD_RUNES =
            PARTICLE_TYPES.register("shield_runes",
                    () -> new TcParticleType<>(false, TcShieldOptions.MAP_CODEC,
                            TcShieldOptions.STREAM_CODEC));

    /** TC-M7-02-c-s: the shield runes for a mob owner (ripple frames, b 0.5). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcShieldOptions>> SHIELD_RUNES_MOB =
            PARTICLE_TYPES.register("shield_runes_mob",
                    () -> new TcParticleType<>(false, TcShieldOptions.MAP_CODEC,
                            TcShieldOptions.STREAM_CODEC));

    /** TC-M7-02-c-s: the sonic pulse riding a mob's head (FXSonic). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<TcSonicOptions>> SONIC =
            PARTICLE_TYPES.register("sonic",
                    () -> new TcParticleType<>(false, TcSonicOptions.MAP_CODEC,
                            TcSonicOptions.STREAM_CODEC));

    private ModParticles() {
    }
}
