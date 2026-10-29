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
 * Payload of the ported wisp particle (TC-M7-02-c-s-f1).
 *
 * <p>The original's shape is {@code FXWisp}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXWisp.java).
 * Its constructor takes the spawn colour (or a {@code type} branch that rolls
 * it), the scale, a rolled lifetime, an optional target, and the senders set
 * three more knobs: {@code shrink}, {@code ignorePhysics}
 * ({@code field_70145_X}, the original's "clip" flag) and
 * {@code setGravity}. Every one of those rides this record so a spawn reaches
 * the client with the same values the 1.7.10 constructor received; the rolls
 * themselves stay server-side-plan-side in {@link TcFxPlans}
 * ({@link FxWispRules}), which is what keeps them testable.</p>
 *
 * @param color          packed 0x00RRGGBB spawn colour (the original's three
 *                       {@code particleRed/Green/Blue} floats)
 * @param scale          the original's {@code particleScale} after the
 *                       sender's {@code *= f} (FXWisp.java:38)
 * @param lifetime       {@code (int)(36 / (rand*0.3 + 0.7))} (FXWisp.java:39)
 * @param targetEntityId the homing target's entity id, or {@code -1} for a
 *                       free wisp (FXWisp.java:127-131, the {@code target}
 *                       constructor {@code wispFX4} uses)
 * @param homing         true when the original has a {@code target}: the tick
 *                       then drags by 0.985 and pulls toward the entity
 *                       (FXWisp.java:150-164); false runs the free branch
 * @param shrink         the original's {@code shrink} flag: the scale envelope
 *                       runs from 1 down to 0 instead of the up-then-down
 *                       triangle (FXWisp.java:176-183)
 * @param gravity        the senders' {@code setGravity} value (0.02 from
 *                       {@code wispFX}, 0 from {@code wispFX2} callers);
 *                       1.21.1 integrates the same {@code yd -= 0.04 * gravity}
 * @param clip           the original's {@code !ignorePhysics}: true lets the
 *                       wisp collide and get pushed out of blocks
 *                       (FXWisp.java:146-148)
 * @param additive       true when the original switches to GL blendmode 771
 *                       (type 5, FXWisp.java:100) — 1.21.1 picks the render
 *                       layer per particle, so the blend branch rides the
 *                       payload instead of a second particle type
 */
public record TcWispOptions(int color, float scale, int lifetime, int targetEntityId,
        boolean homing, boolean shrink, float gravity, boolean clip,
        boolean additive) implements ParticleOptions {

    /** The "no target" sentinel for {@link #targetEntityId()}. */
    public static final int NO_TARGET = -1;

    public static final MapCodec<TcWispOptions> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(TcWispOptions::color),
                    Codec.FLOAT.fieldOf("scale").forGetter(TcWispOptions::scale),
                    Codec.INT.fieldOf("lifetime").forGetter(TcWispOptions::lifetime),
                    Codec.INT.fieldOf("target").forGetter(TcWispOptions::targetEntityId),
                    Codec.BOOL.fieldOf("homing").forGetter(TcWispOptions::homing),
                    Codec.BOOL.fieldOf("shrink").forGetter(TcWispOptions::shrink),
                    Codec.FLOAT.fieldOf("gravity").forGetter(TcWispOptions::gravity),
                    Codec.BOOL.fieldOf("clip").forGetter(TcWispOptions::clip),
                    Codec.BOOL.fieldOf("additive").forGetter(TcWispOptions::additive))
                    .apply(instance, TcWispOptions::new));

    /**
     * The first six fields as a nested wire group. 1.21.1's
     * {@code StreamCodec.composite} overloads cap at SIX fields (javap over
     * the mapped {@code StreamCodec}: arities 1..6), and this payload has
     * nine, so the wire shape composites this group with the last three
     * fields. The byte order stays the {@link #MAP_CODEC} field order.
     */
    private record Core(int color, float scale, int lifetime, int targetEntityId,
            boolean homing, boolean shrink) {
    }

    private Core core() {
        return new Core(this.color, this.scale, this.lifetime, this.targetEntityId,
                this.homing, this.shrink);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Core> CORE_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, Core::color,
                    ByteBufCodecs.FLOAT, Core::scale,
                    ByteBufCodecs.VAR_INT, Core::lifetime,
                    ByteBufCodecs.VAR_INT, Core::targetEntityId,
                    ByteBufCodecs.BOOL, Core::homing,
                    ByteBufCodecs.BOOL, Core::shrink,
                    Core::new);

    /** The wire shape of the spawn, in the same field order as {@link #MAP_CODEC}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TcWispOptions> STREAM_CODEC =
            StreamCodec.composite(
                    CORE_STREAM_CODEC, TcWispOptions::core,
                    ByteBufCodecs.FLOAT, TcWispOptions::gravity,
                    ByteBufCodecs.BOOL, TcWispOptions::clip,
                    ByteBufCodecs.BOOL, TcWispOptions::additive,
                    (core, gravity, clip, additive) -> new TcWispOptions(
                            core.color(), core.scale(), core.lifetime(), core.targetEntityId(),
                            core.homing(), core.shrink(), gravity, clip, additive));

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WISP.get();
    }
}
