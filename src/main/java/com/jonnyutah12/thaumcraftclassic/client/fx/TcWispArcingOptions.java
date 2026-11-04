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
 * Payload of the ported arcing wisp (TC-M7-02-c-s-f1).
 *
 * <p>The original's shape is {@code FXWispArcing}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWispArcing.java).
 * The 11-argument constructor (lines 51-59) parks the spawn on the <em>target</em>
 * point, stores the same triple as its anchor ({@code field_70568_aq/ar/as},
 * lines 30-32) and puts the delta to the source in the motion fields
 * ({@code xd = x - d} etc., lines 55-57) — it never integrates them; the tick
 * (lines 96-110) writes the position directly from anchor + delta + a
 * quadratic arc term. So the delta is payload, not velocity, and this record
 * carries it as such.</p>
 *
 * @param color    packed 0x00RRGGBB colour — {@code ClientProxy.sourceStreamFX}
 *                 (ClientProxy.java:752-757) passes the aspect tag colour's
 *                 {@code getRed()/255F} channels
 * @param scale    the sender's fixed {@code 0.1F} (ClientProxy.java:755),
 *                 multiplied into {@code EntityFX.particleScale} (line 37)
 * @param lifetime {@code (int)(36 / (rand*0.3 + 0.7))} (FXWispArcing.java:41)
 * @param dx       {@code sourceX - targetX} (line 55)
 * @param dy       {@code sourceY - targetY} (line 56)
 * @param dz       {@code sourceZ - targetZ} (line 57)
 */
public record TcWispArcingOptions(int color, float scale, int lifetime, double dx, double dy, double dz)
        implements ParticleOptions {

    public static final MapCodec<TcWispArcingOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcWispArcingOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcWispArcingOptions::scale),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWispArcingOptions::lifetime),
                    Codec.DOUBLE.fieldOf("dx").forGetter(TcWispArcingOptions::dx),
                    Codec.DOUBLE.fieldOf("dy").forGetter(TcWispArcingOptions::dy),
                    Codec.DOUBLE.fieldOf("dz").forGetter(TcWispArcingOptions::dz))
                    .apply(instance, TcWispArcingOptions::new));

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcWispArcingOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TcWispArcingOptions::color,
                    ByteBufCodecs.FLOAT, TcWispArcingOptions::scale,
                    ByteBufCodecs.VAR_INT, TcWispArcingOptions::lifetime,
                    ByteBufCodecs.DOUBLE, TcWispArcingOptions::dx,
                    ByteBufCodecs.DOUBLE, TcWispArcingOptions::dy,
                    ByteBufCodecs.DOUBLE, TcWispArcingOptions::dz,
                    TcWispArcingOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WISP_ARCING.get();
    }
}
