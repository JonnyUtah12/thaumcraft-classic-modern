package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The spawn payload of one {@code thaumcraft:wisp} particle (TC-M7-02-c-s).
 *
 * <p>It carries the state {@code FXWisp}'s constructors settled before the
 * first tick, with every random draw hoisted into {@link TcFxPlans} so the
 * spawn is testable on the game-test server (the original drew its lifetime
 * from the FX's private {@code Math.random()} and its colours from
 * {@code world.rand}, both client-side):
 * <ul>
 *   <li>{@code color} — the packed RGB the wisp renders with. The
 *       {@code FXWisp(world, x, y, z, scale, red, green, blue)} ctor applies
 *       {@code if (red == 0) red = 1} (FXWisp.java:31-33) — the plan applies
 *       that rule before packing, exactly as the original ctor does;</li>
 *   <li>{@code scale} — {@code particleScale = moteParticleScale = 0.1 * f}
 *       is folded by the renderer ({@code f10 = 0.5 * particleScale});
 *       {@code scale} here is the original's {@code f} argument;</li>
 *   <li>{@code gravity} — {@code particleGravity}; {@code wispFX2} leaves it
 *       at 0, {@code wispFX3}/{@code wispFX4} pass -0.02 (rise) or the
 *       ±0.033 aura-node value;</li>
 *   <li>{@code lifetime} — {@code (int)(36.0 / (Math.random() * 0.3 + 0.7))}
 *       (FXWisp.java:45), drawn by the plan; range 36..51 ticks;</li>
 *   <li>{@code motX..motZ} — the wispFX3-style straight-line motion
 *       {@code (target - spawn) / lifetime} (FXWisp.java:121-128), computed
 *       by the plan from the drawn lifetime;</li>
 *   <li>{@code targetEntityId} — the wispFX4 tracking target
 *       ({@code -1} = none): with a target the particle drags at 0.985 and
 *       is pulled toward the entity's centre at 0.2/tick (FXWisp.java:185-202);
 *       without one it drags at 0.98, with the extra 0.7 ground friction.</li>
 * </ul>
 * {@code shrink} selects the two render scale curves (linear fade vs the
 * triangle pulse); {@code tinkle} gates the age-0 {@code random.orb} chirp.</p>
 */
public record TcWispOptions(int color, float scale, double gravity, int lifetime,
        double motX, double motY, double motZ, int targetEntityId,
        boolean shrink, boolean tinkle, boolean additive, boolean clip) implements ParticleOptions {

    public static final MapCodec<TcWispOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcWispOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcWispOptions::scale),
                    Codec.DOUBLE.fieldOf("gravity").forGetter(TcWispOptions::gravity),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWispOptions::lifetime),
                    Codec.DOUBLE.fieldOf("mot_x").forGetter(TcWispOptions::motX),
                    Codec.DOUBLE.fieldOf("mot_y").forGetter(TcWispOptions::motY),
                    Codec.DOUBLE.fieldOf("mot_z").forGetter(TcWispOptions::motZ),
                    Codec.INT.fieldOf("target").forGetter(TcWispOptions::targetEntityId),
                    Codec.BOOL.fieldOf("shrink").forGetter(TcWispOptions::shrink),
                    Codec.BOOL.fieldOf("tinkle").forGetter(TcWispOptions::tinkle),
                    Codec.BOOL.fieldOf("additive").forGetter(TcWispOptions::additive),
                    Codec.BOOL.fieldOf("clip").forGetter(TcWispOptions::clip))
                    .apply(instance, TcWispOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcWispOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcWispOptions::color,
                    ByteBufCodecs.FLOAT, TcWispOptions::scale,
                    ByteBufCodecs.DOUBLE, TcWispOptions::gravity,
                    ByteBufCodecs.VAR_INT, TcWispOptions::lifetime,
                    ByteBufCodecs.DOUBLE, TcWispOptions::motX,
                    ByteBufCodecs.DOUBLE, TcWispOptions::motY,
                    ByteBufCodecs.DOUBLE, TcWispOptions::motZ,
                    ByteBufCodecs.VAR_INT, TcWispOptions::targetEntityId,
                    ByteBufCodecs.BOOL, TcWispOptions::shrink,
                    ByteBufCodecs.BOOL, TcWispOptions::tinkle,
                    ByteBufCodecs.BOOL, TcWispOptions::additive,
                    ByteBufCodecs.BOOL, TcWispOptions::clip,
                    TcWispOptions::new);

    /** The 36-tick lifetime base of {@code FXWisp}'s ctor (FXWisp.java:45). */
    public static final int LIFETIME_BASE = 36;

    /** The {@code Math.random() * 0.3 + 0.7} divisor span of the same line. */
    public static final double LIFETIME_DIV_MIN = 0.7;
    public static final double LIFETIME_DIV_SPAN = 0.3;

    /**
     * The original's lifetime draw, factored out so the plan can pre-draw it
     * (the FX drew it from its own {@code Math.random()}).
     */
    public static int lifetime(final double random01) {
        return (int) (LIFETIME_BASE / (random01 * LIFETIME_DIV_SPAN + LIFETIME_DIV_MIN));
    }

    /** {@code wispFX4}: an untracked wisp. */
    public static final int NO_TARGET = -1;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WISP.get();
    }
}
