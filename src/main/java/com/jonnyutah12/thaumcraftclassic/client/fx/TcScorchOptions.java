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
 * The spawn payload of one {@code thaumcraft:scorch} mote
 * (TC-M7-02-c-s-f5) — the fireblast scorch streak of {@code UtilsFX.shootFire}
 * (client/lib/UtilsFX.java:365-386) → {@code FXScorch}.
 *
 * <p>The ctor aims the mote at a point 100 units along the throw direction and
 * jitters it: by {@code (rand-rand) * spread} for a normal blast, by
 * {@code (rand-rand) * 0.5} for a lance (FXScorch.java:22-35). Both rolls are
 * drawn by {@link TcFxPlans#scorch}, which is the sender's loop, so the payload
 * carries the resolved target point plus the ctor's own scale roll
 * {@code lance ? rand*0.5+2.0 : rand+3.0} (FXScorch.java:37-40).</p>
 */
public record TcScorchOptions(double targetX, double targetY, double targetZ,
        float scale) implements ParticleOptions {

    public static final MapCodec<TcScorchOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("target_x").forGetter(TcScorchOptions::targetX),
                    Codec.DOUBLE.fieldOf("target_y").forGetter(TcScorchOptions::targetY),
                    Codec.DOUBLE.fieldOf("target_z").forGetter(TcScorchOptions::targetZ),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcScorchOptions::scale))
                    .apply(instance, TcScorchOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcScorchOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, TcScorchOptions::targetX,
                    ByteBufCodecs.DOUBLE, TcScorchOptions::targetY,
                    ByteBufCodecs.DOUBLE, TcScorchOptions::targetZ,
                    ByteBufCodecs.FLOAT, TcScorchOptions::scale,
                    TcScorchOptions::new);

    /** The ctor's aim distance: {@code pos + vec3d * 100.0} (FXScorch.java:22-24). */
    public static final double AIM_DISTANCE = 100.0;

    /** The non-lance jitter term: {@code (rand - rand) * range} (FXScorch.java:26-29). */
    public static final float JITTER_SPAN = 1.0F;

    /** The lance jitter term: {@code (rand - rand) * 0.5} (FXScorch.java:31-34). */
    public static final float LANCE_JITTER = 0.5F;

    /** {@code particleMaxAge = 50} (FXScorch.java:41). */
    public static final int LIFETIME = 50;

    /** {@code setParticleBounds(0.1F, 0.1F)} (FXScorch.java:42). */
    public static final float BOUND = 0.1F;

    /** {@code particleAlpha = 0.66F} (FXScorch.java:47). */
    public static final float ALPHA = 0.66F;

    /** The first frame of the strip: {@code setParticleTextureIndex(151)} (line 43). */
    public static final int FIRST_CELL = 151;

    /** The strip's frame count: the index climbs 151 → 159 (lines 100-104). */
    public static final int FRAMES = 9;

    /** The sender's lead-in: {@code pos += vec3d * 0.3F} twice (UtilsFX.java:377-385). */
    public static final double SENDER_LEAD = 0.3;

    /** The mote count of the original sender loop: {@code for (q = 0; q < 3; q++)}. */
    public static final int SENDER_COUNT = 3;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SCORCH.get();
    }
}
