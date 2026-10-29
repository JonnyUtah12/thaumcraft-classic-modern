package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

/**
 * The original FX senders' spawn loops, ported as pure code (TC-M7-02-s).
 *
 * <p>Every loop below is a line-for-line transcription of the original client
 * handler, with the same random-draw order so a seeded {@link RandomSource}
 * reproduces a burst exactly. Keeping them out of the client classes is what
 * makes the geometry testable on the game-test server, where no client exists
 * (the original's whole FX path sits behind {@code world.field_72995_K}, the
 * client-only gate).</p>
 *
 * <p>The random draw order is part of the fidelity claim: the original passes
 * these values as positional constructor arguments, and Java evaluates them
 * left to right, so changing the order here changes the burst.</p>
 */
public final class TcFxPlans {

    /**
     * The sparkle sender's magic colour: {@code PacketFXBlockSparkle} passes
     * {@code -9999} and {@code ClientProxy.blockSparkle} (ClientProxy.java:585-589)
     * then replaces the colour with a per-particle white-rainbow roll.
     */
    public static final int RAINBOW_COLOR = -9999;

    /** The base RGB of the original's white-rainbow sparkle branch. */
    public static final float RAINBOW_BASE = 0.33F;
    /** The span of the original's white-rainbow sparkle branch. */
    public static final float RAINBOW_SPAN = 0.67F;

    /** The original's sparkle jitter: colour ±0.2 (ClientProxy.java:597-599). */
    public static final float COLOR_JITTER = 0.2F;

    /** The original's sparkle spawn jitter: -0.1 .. +1.1 of the block (line 592-594). */
    public static final float SPARKLE_OFFSET = 0.1F;
    /** The original's sparkle spawn jitter span (line 592-594). */
    public static final float SPARKLE_OFFSET_SPAN = 1.2F;

    /** The original's sparkle upward drift (line 595). */
    public static final float SPARKLE_RISE = 0.02F;

    /** The original's sparkle alpha (line 600). */
    public static final float SPARKLE_ALPHA = 0.9F;

    /** The original's sparkle base age: 5 + rand(8) (line 603). */
    public static final int SPARKLE_AGE_BASE = 5;
    /** The original's sparkle age span (line 603). */
    public static final int SPARKLE_AGE_SPAN = 8;
    /** The original's sparkle render delay: rand(10) (line 604). */
    public static final int SPARKLE_DELAY_SPAN = 10;
    /** The original's sparkle scale: 0.7 + rand*0.4 (line 605). */
    public static final float SPARKLE_SCALE_BASE = 0.7F;
    /** The original's sparkle scale span (line 605). */
    public static final float SPARKLE_SCALE_SPAN = 0.4F;

    /** The base count every {@code blockSparkle} call passes (PacketFXBlockSparkle:7?). */
    public static final int SPARKLE_BASE_COUNT = 7;

    /** The bubble burst base count of {@code PacketFXBlockBubble} (particleCount(1)). */
    public static final int BUBBLE_BASE_COUNT = 1;
    /** The original spawns four bubbles per loop iteration (PacketFXBlockBubble:53-77). */
    public static final int BUBBLES_PER_LOOP = 4;

    /** The {@code age} the original's crucibleBubble passes to FXBubble. */
    public static final int BUBBLE_AGE = 1;
    /** {@code EntityFX.particleScale} default the original multiplies (FxBubble.java:21). */
    public static final float BUBBLE_SCALE_BASE = 0.1F;
    /** The original's bubble scale roll: rand*0.3 + 0.2 (FxBubble.java:21). */
    public static final float BUBBLE_SCALE_SPAN = 0.3F;
    public static final float BUBBLE_SCALE_MIN = 0.2F;
    /** The original's bubble jitter (FxBubble.java:22-24). */
    public static final float BUBBLE_JITTER = 0.02F;
    /** The original's bubble rise roll denominator (FxBubble.java:25). */
    public static final float BUBBLE_LIFETIME_SPAN = 0.8F;
    public static final float BUBBLE_LIFETIME_MIN = 0.2F;

    private TcFxPlans() {
    }

    /**
     * {@code ClientProxy.blockSparkle(world, x, y, z, c, 7)} — the loop that
     * every {@code PacketFXBlockSparkle} client handler runs
     * ({@code PacketFXBlockSparkle.java:41}, {@code ClientProxy.java:576-608}).
     *
     * @param pos    the block corner the original sends as (x, y, z) ints
     * @param color  the packed colour the packet carries (0xAARRGGBB signed int;
     *               {@link #RAINBOW_COLOR} selects the white-rainbow branch)
     * @param rand   the randomness source (the original uses {@code world.rand})
     * @param particleSetting the client's particle setting ordinal, gated
     *                        through {@link FxParticleCounts#particleCount}
     */
    public static List<ParticleSpawn> blockSparkle(final BlockPos pos, final int color,
            final RandomSource rand, final int particleSetting) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        // Color(c).getRed()/255F — the packet colour is a signed java.awt RGB int.
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        for (int a = 0; a < FxParticleCounts.particleCount(SPARKLE_BASE_COUNT, particleSetting); a++) {
            if (color == RAINBOW_COLOR) {
                // The original reassigns the running r/g/b inside the loop, so a
                // rainbow burst's later particles jitter off the previous roll.
                r = RAINBOW_BASE + rand.nextFloat() * RAINBOW_SPAN;
                g = RAINBOW_BASE + rand.nextFloat() * RAINBOW_SPAN;
                b = RAINBOW_BASE + rand.nextFloat() * RAINBOW_SPAN;
            }

            final double x = pos.getX() - SPARKLE_OFFSET + rand.nextFloat() * SPARKLE_OFFSET_SPAN;
            final double y = pos.getY() - SPARKLE_OFFSET + rand.nextFloat() * SPARKLE_OFFSET_SPAN;
            final double z = pos.getZ() - SPARKLE_OFFSET + rand.nextFloat() * SPARKLE_OFFSET_SPAN;
            final double vx = 0.0;
            final double vy = rand.nextFloat() * SPARKLE_RISE;
            final double vz = 0.0;
            final int jittered = rgb(r - COLOR_JITTER + rand.nextFloat() * (COLOR_JITTER * 2.0F),
                    g - COLOR_JITTER + rand.nextFloat() * (COLOR_JITTER * 2.0F),
                    b - COLOR_JITTER + rand.nextFloat() * (COLOR_JITTER * 2.0F));
            final int age = SPARKLE_AGE_BASE + rand.nextInt(SPARKLE_AGE_SPAN);
            final int delay = rand.nextInt(SPARKLE_DELAY_SPAN);
            final float scale = SPARKLE_SCALE_BASE + rand.nextFloat() * SPARKLE_SCALE_SPAN;

            plan.add(new ParticleSpawn(ModParticles.SPARCLE.get(),
                    new TcSparkleOptions(jittered, SPARKLE_ALPHA, scale, age, delay),
                    x, y, z, vx, vy, vz));
        }
        return plan;
    }

    /**
     * {@code PacketFXBlockBubble.onMessage} — four jittered
     * {@code ClientProxy.crucibleBubble} calls per {@code particleCount(1)}
     * iteration ({@code PacketFXBlockBubble.java:44-79},
     * {@code ClientProxy.java:682-687}, {@code FXBubble.java:14-38}).
     *
     * <p>The original's jitter pattern is literal: bubble 1 sits on the x face
     * with y/z jittered, bubble 2 on x+1, bubble 3 on the z face with x/y
     * jittered, bubble 4 on z+1. The y coordinate is jittered for every one of
     * them, which is why the burst reads as a cloud over the block rather than
     * four corners.</p>
     */
    public static List<ParticleSpawn> blockBubble(final BlockPos pos, final int color,
            final RandomSource rand, final int particleSetting) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        for (int a = 0; a < FxParticleCounts.particleCount(BUBBLE_BASE_COUNT, particleSetting); a++) {
            plan.add(crucibleBubble(pos.getX(), pos.getY() + rand.nextFloat(),
                    pos.getZ() + rand.nextFloat(), color, rand));
            plan.add(crucibleBubble(pos.getX() + 1.0, pos.getY() + rand.nextFloat(),
                    pos.getZ() + rand.nextFloat(), color, rand));
            plan.add(crucibleBubble(pos.getX() + rand.nextFloat(), pos.getY() + rand.nextFloat(),
                    pos.getZ(), color, rand));
            plan.add(crucibleBubble(pos.getX() + rand.nextFloat(), pos.getY() + rand.nextFloat(),
                    pos.getZ() + 1.0, color, rand));
        }
        return plan;
    }

    /**
     * The original's {@code new FXBubble(world, x, y, z, 0, 0, 0, 1)}
     * constructor, field by field (FxBubble.java:14-38). The passed motion is
     * zero, so the initial motion is pure jitter.
     */
    private static ParticleSpawn crucibleBubble(final double x, final double y, final double z,
            final int color, final RandomSource rand) {
        final float scale = BUBBLE_SCALE_BASE * (rand.nextFloat() * BUBBLE_SCALE_SPAN + BUBBLE_SCALE_MIN);
        final double vx = 0.0 * 0.2 + (rand.nextFloat() * 2.0 - 1.0) * BUBBLE_JITTER;
        final double vy = 0.0 * 0.2 + rand.nextFloat() * BUBBLE_JITTER;
        final double vz = 0.0 * 0.2 + (rand.nextFloat() * 2.0 - 1.0) * BUBBLE_JITTER;
        final int lifetime = (int) (BUBBLE_AGE + 2 + 8.0 / (rand.nextFloat() * BUBBLE_LIFETIME_SPAN + BUBBLE_LIFETIME_MIN));
        return new ParticleSpawn(ModParticles.BUBBLE.get(),
                new TcBubbleOptions(color, scale, TcBubbleOptions.DEFAULT_RISE, lifetime),
                x, y, z, vx, vy, vz);
    }

    /** Packs three 0..1 channels into the 0x00RRGGBB form the 1.21.1 tint uses. */
    public static int rgb(final float r, final float g, final float b) {
        return (Mth.clamp((int) (r * 255.0F), 0, 255) << 16)
                | (Mth.clamp((int) (g * 255.0F), 0, 255) << 8)
                | Mth.clamp((int) (b * 255.0F), 0, 255);
    }

    /** Extracts the 0..1 red channel of a packed colour. */
    public static float red(final int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    /** Extracts the 0..1 green channel of a packed colour. */
    public static float green(final int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    /** Extracts the 0..1 blue channel of a packed colour. */
    public static float blue(final int color) {
        return (color & 0xFF) / 255.0F;
    }
}
