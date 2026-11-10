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
 * The spawn payload of one {@code thaumcraft:drop} mote
 * (TC-M7-02-c-s-f5) — the vis droplet {@code ClientProxy.dropletFX}
 * (client/ClientProxy.java:1137-1141) puts in the world, and the drip of
 * {@code TileResearchNote}? no — the sender is {@code dropletFX}, which
 * forwards the caller's three channels straight into the ctor
 * (FXDrop.java:20-30).
 *
 * <p>The ctor's lifetime roll is {@code 64 / (Math.random() * 0.8 + 0.2)}
 * (FXDrop.java:29) — the original draws it from {@code Math.random()}, not the
 * world random, so the plan hands the value in as a parameter and the caller
 * (the client-tick drip) owns that draw. {@code bobTimer} is the ctor's own
 * constant 40 (line 28).</p>
 */
public record TcDropOptions(int color, int lifetime) implements ParticleOptions {

    public static final MapCodec<TcDropOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcDropOptions::color),
                    Codec.INT.fieldOf("lifetime").forGetter(TcDropOptions::lifetime))
                    .apply(instance, TcDropOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcDropOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcDropOptions::color,
                    ByteBufCodecs.VAR_INT, TcDropOptions::lifetime,
                    TcDropOptions::new);

    /** {@code particleGravity = 0.06F} (FXDrop.java:27). */
    public static final float GRAVITY = 0.06F;

    /** The bob window: {@code bobTimer = 40} (FXDrop.java:28). */
    public static final int BOB_TICKS = 40;

    /** The lifetime roll: {@code 64 / (rand * 0.8 + 0.2)} (FXDrop.java:29). */
    public static final double LIFETIME_BASE = 64.0;
    public static final double LIFETIME_SPAN = 0.8;
    public static final double LIFETIME_MIN = 0.2;

    /** The per-tick drag 0.98 (FXDrop.java:58-60). */
    public static final double DRAG = 0.98;

    /** The bob's horizontal/vertical crush 0.02 (FXDrop.java:46-48). */
    public static final double BOB_CRUSH = 0.02;

    /** The ground drag 0.7 (FXDrop.java:67-69). */
    public static final double GROUND_DRAG = 0.7;

    /** The three frames the class names: bobbing 113, spent 112, landed 114. */
    public static final int CELL_BOBBING = 113;
    public static final int CELL_SPENT = 112;
    public static final int CELL_LANDED = 114;
    /** The sliced strip is cells 112..114, so the first frame is 112. */
    public static final int FIRST_CELL = 112;
    public static final int FRAMES = 3;

    /** The render's flat brightness: {@code getBrightnessForRender = 257} (line 33). */
    public static final int LIGHT = 257;

    @Override
    public ParticleType<?> getType() {
        return ModParticles.DROP.get();
    }
}
