package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOptions;
import net.minecraft.core.particles.ItemParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

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

    // =================================================================================
    // TC-M7-02-c-s: the wisp family (ClientProxy.wispFX*, FXWisp*), the remaining
    // ClientEffectHooks senders (PacketFXBlockDig, PacketFXVisDrain,
    // PacketFXEssentiaSource, PacketFXInfusionSource) and the entity-space FX
    // (PacketFXShield, PacketFXSonic).
    //
    // As above, every random draw of the original path is hoisted here in the
    // original order: positional constructor args evaluate left-to-right, the
    // FX constructors draw their own values next (the 1.7.10 FX drew from the
    // non-seedable EntityFX java.util.Random / Math.random() — merging those
    // into the plan RandomSource is what makes the burst reproducible), and
    // post-constructor setter calls draw last.
    // =================================================================================

    /** {@code FXWisp}'s lifetime roll base: {@code (int)(36 / (rand*0.3 + 0.7))} (FXWisp.java:45). */
    public static final int WISP_LIFETIME_BASE = 36;
    /** {@code FXWispEG}'s lifetime roll base: {@code (int)(40 / (rand*0.3 + 0.7))} (FXWispEG.java:26). */
    public static final int WISP_EG_LIFETIME_BASE = 40;
    /** The wisp lifetime divisor: {@code rand * 0.3 + 0.7} (same lines). */
    public static final double WISP_LIFETIME_DIV_MIN = 0.7;
    /** The wisp lifetime divisor span (same lines). */
    public static final double WISP_LIFETIME_DIV_SPAN = 0.3;

    /** {@code wispFX}: fixed gravity of the plain wisp (ClientProxy.java:702). */
    public static final double WISP_GRAVITY = 0.02;
    /** {@code wispFX4}: the fixed wisp scale (FXWisp.java:118-121 delegates with 0.4F). */
    public static final float WISP_TARGET_SCALE = 0.4F;

    /** {@code FXWispEG} spawn drift: {@code nextGaussian() * 0.03} (FXWispEG.java:17,20). */
    public static final double WISP_EG_DRIFT = 0.03;
    /** {@code FXWispEG} fall: {@code -0.05} (FXWispEG.java:18). */
    public static final double WISP_EG_FALL = -0.05;
    /** {@code FXWispEG} colour roll: {@code rand * 0.05} per channel (FXWispEG.java:36-38). */
    public static final float WISP_EG_COLOR_SPAN = 0.05F;
    /** {@code wispFXEG} burst base: {@code particleCount(1)} (ClientProxy.java:709). */
    public static final int WISP_EG_BASE_COUNT = 1;

    /**
     * The wisp type colour roll of {@code FXWisp(world, x, y, z, scale, type)}
     * (FXWisp.java:64-111). Each rolled channel draws one float in r-g-b
     * order; a fixed channel draws nothing. Type 5 flips the blend to additive
     * (blendmode 771, line 96). An unknown type keeps the constructor's
     * (1, 0, 0) from the {@code red == 0 → 1} rule (line 32-34).
     */
    public static int wispTypeColor(final int type, final RandomSource rand) {
        final float r;
        final float g;
        final float b;
        switch (type) {
            case 0 -> {
                r = 0.75F + rand.nextFloat() * 0.25F;
                g = 0.25F + rand.nextFloat() * 0.25F;
                b = 0.75F + rand.nextFloat() * 0.25F;
            }
            case 1 -> {
                r = 0.5F + rand.nextFloat() * 0.3F;
                g = 0.5F + rand.nextFloat() * 0.3F;
                b = 0.2F;
            }
            case 2 -> {
                r = 0.2F;
                g = 0.2F;
                b = 0.7F + rand.nextFloat() * 0.3F;
            }
            case 3 -> {
                r = 0.2F;
                g = 0.7F + rand.nextFloat() * 0.3F;
                b = 0.2F;
            }
            case 4 -> {
                r = 0.7F + rand.nextFloat() * 0.3F;
                g = 0.2F;
                b = 0.2F;
            }
            case 5 -> {
                r = rand.nextFloat() * 0.1F;
                g = rand.nextFloat() * 0.1F;
                b = rand.nextFloat() * 0.1F;
            }
            case 6 -> {
                r = 0.8F + rand.nextFloat() * 0.2F;
                g = 0.8F + rand.nextFloat() * 0.2F;
                b = 0.8F + rand.nextFloat() * 0.2F;
            }
            case 7 -> {
                r = 0.7F + rand.nextFloat() * 0.3F;
                g = 0.5F + rand.nextFloat() * 0.2F;
                b = 0.3F + rand.nextFloat() * 0.1F;
            }
            default -> {
                r = 1.0F;
                g = 0.0F;
                b = 0.0F;
            }
        }
        return rgb(r, g, b);
    }

    /** The additive flag of a wisp type: only type 5 sets blendmode 771 (FXWisp.java:95-99). */
    public static boolean wispTypeAdditive(final int type) {
        return type == 5;
    }

    /** The wisp lifetime draw shared by the family: {@code (int)(36 / (rand*0.3 + 0.7))}. */
    public static int wispLifetime(final RandomSource rand) {
        return (int) (WISP_LIFETIME_BASE / (rand.nextDouble() * WISP_LIFETIME_DIV_SPAN + WISP_LIFETIME_DIV_MIN));
    }

    /**
     * {@code ClientProxy.wispFX} (ClientProxy.java:700-703): one plain wisp,
     * colour given straight, gravity 0.02. The constructor's
     * {@code red == 0 → 1} rule (FXWisp.java:32-34) is applied to the packed
     * colour; the lifetime draws first (constructor line 45).
     */
    public static List<ParticleSpawn> wispFx(final double x, final double y, final double z,
            final float scale, final float red, final float green, final float blue,
            final RandomSource rand) {
        final int lifetime = wispLifetime(rand);
        final float r = red == 0.0F ? 1.0F : red;
        return List.of(new ParticleSpawn(ModParticles.WISP.get(),
                new TcWispOptions(rgb(r, green, blue), scale, WISP_GRAVITY, lifetime,
                        0.0, 0.0, 0.0, TcWispOptions.NO_TARGET, false, false, false, false),
                x, y, z, 0.0, 0.0, 0.0));
    }

    /**
     * {@code ClientProxy.wispFX2} (ClientProxy.java:705-710): typed wisp,
     * {@code shrink}/{@code clip}/gravity from the caller. Draws: lifetime,
     * then the type colour roll.
     */
    public static List<ParticleSpawn> wispFx2(final double x, final double y, final double z,
            final float size, final int type, final boolean shrink, final boolean clip,
            final double gravity, final RandomSource rand) {
        final int lifetime = wispLifetime(rand);
        final int color = wispTypeColor(type, rand);
        return List.of(new ParticleSpawn(ModParticles.WISP.get(),
                new TcWispOptions(color, size, gravity, lifetime,
                        0.0, 0.0, 0.0, TcWispOptions.NO_TARGET, shrink, false,
                        wispTypeAdditive(type), clip),
                x, y, z, 0.0, 0.0, 0.0));
    }

    /**
     * {@code ClientProxy.wispFX3} (ClientProxy.java:719-724): typed wisp that
     * travels straight to the target: {@code mot = (target - spawn) / lifetime}
     * when the lifetime survived (FXWisp.java:112-118; a culled lifetime keeps
     * zero motion). Draws: lifetime, then the type colour roll.
     */
    public static List<ParticleSpawn> wispFx3(final double x, final double y, final double z,
            final double tx, final double ty, final double tz, final float size, final int type,
            final boolean shrink, final double gravity, final RandomSource rand) {
        final int lifetime = wispLifetime(rand);
        final int color = wispTypeColor(type, rand);
        final double motX;
        final double motY;
        final double motZ;
        if (lifetime > 0) {
            motX = (tx - x) / lifetime;
            motY = (ty - y) / lifetime;
            motZ = (tz - z) / lifetime;
        } else {
            motX = 0.0;
            motY = 0.0;
            motZ = 0.0;
        }
        return List.of(new ParticleSpawn(ModParticles.WISP.get(),
                new TcWispOptions(color, size, gravity, lifetime,
                        motX, motY, motZ, TcWispOptions.NO_TARGET, shrink, false,
                        wispTypeAdditive(type), false),
                x, y, z, motX, motY, motZ));
    }

    /**
     * {@code ClientProxy.wispFX4} (ClientProxy.java:726-731): typed wisp that
     * tracks an entity (fixed scale 0.4, FXWisp.java:118-121). Draws:
     * lifetime, then the type colour roll.
     */
    public static List<ParticleSpawn> wispFx4(final double x, final double y, final double z,
            final int targetEntityId, final int type, final boolean shrink,
            final double gravity, final RandomSource rand) {
        final int lifetime = wispLifetime(rand);
        final int color = wispTypeColor(type, rand);
        return List.of(new ParticleSpawn(ModParticles.WISP.get(),
                new TcWispOptions(color, WISP_TARGET_SCALE, gravity, lifetime,
                        0.0, 0.0, 0.0, targetEntityId, shrink, false,
                        wispTypeAdditive(type), false),
                x, y, z, 0.0, 0.0, 0.0));
    }

    /**
     * {@code ClientProxy.wispFXEG} (ClientProxy.java:712-716):
     * {@code particleCount(1)} EnderGoo wisps riding a target. Per spawn the
     * {@code FXWispEG} ctor draws (FXWispEG.java:15-38): x-drift gaussian,
     * z-drift gaussian, lifetime, then the three 0..0.05 colour rolls.
     */
    public static List<ParticleSpawn> wispFxEg(final double x, final double y, final double z,
            final int targetEntityId, final int particleSetting, final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        for (int a = 0; a < FxParticleCounts.particleCount(WISP_EG_BASE_COUNT, particleSetting); a++) {
            final double vx = rand.nextGaussian() * WISP_EG_DRIFT;
            final double vz = rand.nextGaussian() * WISP_EG_DRIFT;
            final int lifetime = (int) (WISP_EG_LIFETIME_BASE
                    / (rand.nextDouble() * WISP_LIFETIME_DIV_SPAN + WISP_LIFETIME_DIV_MIN));
            final int color = rgb(rand.nextFloat() * WISP_EG_COLOR_SPAN,
                    rand.nextFloat() * WISP_EG_COLOR_SPAN,
                    rand.nextFloat() * WISP_EG_COLOR_SPAN);
            plan.add(new ParticleSpawn(ModParticles.WISP_EG.get(),
                    new TcWispEgOptions(targetEntityId, lifetime, color),
                    x, y, z, vx, WISP_EG_FALL, vz));
        }
        return plan;
    }

    /**
     * {@code ClientProxy.sourceStreamFX} (ClientProxy.java:777-781): the
     * arcing stream wisp flying from the target param toward the spawn param
     * ({@code FXWispArcing(world, tx, ty, tz, sx, sy, sz, 0.1, r, g, b)} →
     * {@code mot = start - target}, FXWispArcing.java:44-46). The colour comes
     * straight from the tag colour; the only draw is the ctor lifetime roll
     * (FXWispArcing.java:38, same 36/(rand*0.3+0.7) shape).
     */
    public static List<ParticleSpawn> sourceStream(final double sx, final double sy, final double sz,
            final double tx, final double ty, final double tz, final int tagColor,
            final RandomSource rand) {
        final int lifetime = wispLifetime(rand);
        return List.of(new ParticleSpawn(ModParticles.WISP_ARCING.get(),
                new TcWispArcingOptions(tagColor, 0.1F, lifetime, sx - tx, sy - ty, sz - tz),
                tx, ty, tz, 0.0, 0.0, 0.0));
    }

    /**
     * {@code PacketFXVisDrain.onMessage} (PacketFXVisDrain.java:44-58): one
     * vis mote spawned at the source block corner + 0.4..0.6, homing at the
     * target block + a unit roll. Draws: the three spawn offsets, then the
     * three target offsets.
     */
    public static List<ParticleSpawn> visDrain(final double sx, final double sy, final double sz,
            final double tx, final double ty, final double tz, final int color,
            final RandomSource rand) {
        final double x = sx + 0.4 + rand.nextFloat() * 0.2;
        final double y = sy + 0.4 + rand.nextFloat() * 0.2;
        final double z = sz + 0.4 + rand.nextFloat() * 0.2;
        final double targetX = tx + rand.nextFloat();
        final double targetY = ty + rand.nextFloat();
        final double targetZ = tz + rand.nextFloat();
        return List.of(new ParticleSpawn(ModParticles.VIS_SPARKLE.get(),
                new TcVisSparkleOptions(color, targetX, targetY, targetZ, 0),
                x, y, z, 0.0, 0.0, 0.0));
    }

    // ----- PacketFXBlockDig -----

    /** {@code PacketFXBlockDig} burst base: {@code particleCount(20)} (PacketFXBlockDig.java:66). */
    public static final int BLOCK_DIG_BASE_COUNT = 20;
    /** The bore particle's lifetime base: {@code (int)(dist * 3)} min 1 (FXBoreParticles.java:44-47). */
    public static final float BORE_LIFETIME_DIST = 3.0F;
    /** The bore particle's spawn drift: {@code world rand gaussian * 0.01} (FXBoreParticles.java:51-53). */
    public static final float BORE_DRIFT = 0.01F;
    /** The bore particle's scale roll: {@code rand * 0.3 + 0.4} (FXBoreParticles.java:40). */
    public static final float BORE_SCALE_BASE = 0.4F;
    /** The bore particle's scale span (same line). */
    public static final float BORE_SCALE_SPAN = 0.3F;
    /** {@code drawInfusionParticles1}'s extra velocity: {@code gaussian * 0.03} (ClientProxy.java:650-652). */
    public static final float BORE_ITEM_DRIFT = 0.03F;

    /**
     * {@code PacketFXBlockDig.onMessage} (PacketFXBlockDig.java:66-94):
     * {@code particleCount(20)} bore crumbs from the dig-face block flying into
     * the target block centre. Per iteration the draws are: three spawn
     * offsets (positional args, evaluated first), the side roll
     * {@code nextInt(6)} (also a positional arg), then the
     * {@code FXBoreParticles} ctor: scale, lifetime, three drift gaussians.
     *
     * <p>1.21.1 mapping: block crumbs ride the vanilla {@code block}
     * particle ({@link ParticleTypes#BLOCK}), items the vanilla {@code item}
     * particle ({@link ParticleTypes#ITEM}) — the original's own texture-paged
     * 2D crumb has no 1.21.1 billboard equivalent (BD row). The original's
     * block metadata (1.7 side-tinted icons) selects the modern
     * {@link BlockState} directly.</p>
     */
    public static List<ParticleSpawn> blockDig(final BlockPos target, final BlockPos digFrom,
            final BlockState state, final ItemStack stack, final RandomSource rand,
            final int particleSetting) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        for (int a = 0; a < FxParticleCounts.particleCount(BLOCK_DIG_BASE_COUNT, particleSetting); a++) {
            final double x = digFrom.getX() + rand.nextFloat();
            final double y = digFrom.getY() + rand.nextFloat();
            final double z = digFrom.getZ() + rand.nextFloat();
            rand.nextInt(6); // the side roll: positional ctor arg in the original, unused on 1.21.1
            final double tx = target.getX() + 0.5;
            final double ty = target.getY() + 0.5;
            final double tz = target.getZ() + 0.5;
            final double dist = Mth.sqrt((float) ((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z)));
            int base = (int) (dist * BORE_LIFETIME_DIST);
            if (base < 1) {
                base = 1;
            }
            rand.nextFloat(); // ctor scale roll: the vanilla crumb sizes itself
            final int lifetime = base / 2 + rand.nextInt(base);
            final double vx = rand.nextGaussian() * BORE_DRIFT;
            final double vy = rand.nextGaussian() * BORE_DRIFT;
            final double vz = rand.nextGaussian() * BORE_DRIFT;
            if (stack == null || stack.isEmpty()) {
                plan.add(new ParticleSpawn(ParticleTypes.BLOCK,
                        new BlockParticleOptions(state), x, y, z, vx, vy, vz));
            } else {
                plan.add(new ParticleSpawn(ParticleTypes.ITEM,
                        new ItemParticleOptions(stack), x, y, z, vx, vy, vz));
            }
            if (lifetime < 0) {
                break; // unreachable; keeps the draw count honest
            }
        }
        return plan;
    }

    // ----- the client-tick source maps (PacketFXEssentiaSource / PacketFXInfusionSource) -----

    /** {@code FXEssentiaTrail} lifetime base: {@code (int)(dist * 30)} min 1 (FXEssentiaTrail.java:52-55). */
    public static final float TRAIL_LIFETIME_DIST = 30.0F;
    /** The trail mote's sine drift constants (FXEssentiaTrail.java:57-59). */
    public static final float TRAIL_DRIFT = 0.015F;
    /** The trail mote's gaussian jitter (FXEssentiaTrail.java:57-59). */
    public static final float TRAIL_JITTER = 0.002F;
    /** The trail mote's base rise (FXEssentiaTrail.java:58). */
    public static final float TRAIL_RISE = 0.1F;
    /** The trail colour jitter factor: {@code channel * 0.2} (FXEssentiaTrail.java:63-68). */
    public static final float TRAIL_COLOR_JITTER = 0.2F;
    /** {@code EssentiaSourceFX} tick budget when a packet lands (PacketFXEssentiaSource.java:52-62). */
    public static final int ESSENTIA_SOURCE_TICKS = 15;
    /** The trail's tail-off window: the last 5 ticks shrink the scale (ClientTickEventsFML.java:244-246). */
    public static final int ESSENTIA_TRAIL_TAIL = 5;

    /**
     * One tick of one {@code EssentiaHandler.sourceFX} entry
     * (ClientTickEventsFML.java:218-261 → ClientProxy.essentiaTrailFx:783-786):
     * one {@code FXEssentiaTrail} spawned at the source block centre flying to
     * the consumer block centre (consumer y shifted by -1 when the consumer is
     * an infusion matrix). The burst count param is the client tick count
     * ({@code this.tickCount}); in the last 5 ticks it becomes
     * {@code tickCount - (5 - ticks)} and the scale becomes
     * {@code ticks²/25}.
     *
     * <p>Per-spawn ctor draws (FXEssentiaTrail.java:56-68, hoisted in order):
     * lifetime {@code nextInt(base)}, x gaussian, z gaussian, then the three
     * colour jitter rolls.</p>
     */
    public static List<ParticleSpawn> essentiaTrailTick(final int sx, final int sy, final int sz,
            final int tx, final int ty, final int tz, final int color, final int ticks,
            final int tickCount, final boolean consumerIsMatrix, final RandomSource rand) {
        final int mod = consumerIsMatrix ? -1 : 0;
        final int count;
        final float scale;
        if (ticks > ESSENTIA_TRAIL_TAIL) {
            count = tickCount;
            scale = 1.0F;
        } else {
            count = tickCount - (ESSENTIA_TRAIL_TAIL - ticks);
            scale = (float) ticks * ticks / 25.0F;
        }
        final double x = sx + 0.5;
        final double y = sy + 0.5;
        final double z = sz + 0.5;
        final double targetX = tx + 0.5;
        final double targetY = ty + mod + 0.5;
        final double targetZ = tz + 0.5;
        final double dist = Mth.sqrt((float) ((targetX - x) * (targetX - x)
                + (targetY - y) * (targetY - y) + (targetZ - z) * (targetZ - z)));
        int base = (int) (dist * TRAIL_LIFETIME_DIST);
        if (base < 1) {
            base = 1;
        }
        final int lifetime = base / 2 + rand.nextInt(base);
        final double vx = Mth.sin(count / 4.0F) * TRAIL_DRIFT + rand.nextGaussian() * TRAIL_JITTER;
        final double vy = TRAIL_RISE + Mth.sin(count / 3.0F) * 0.01F;
        final double vz = Mth.sin(count / 2.0F) * TRAIL_DRIFT + rand.nextGaussian() * TRAIL_JITTER;
        final float pr = red(color);
        final float pg = green(color);
        final float pb = blue(color);
        final int jittered = rgb(
                pr - pr * TRAIL_COLOR_JITTER + rand.nextFloat() * pr * TRAIL_COLOR_JITTER,
                pg - pg * TRAIL_COLOR_JITTER + rand.nextFloat() * pg * TRAIL_COLOR_JITTER,
                pb - pb * TRAIL_COLOR_JITTER + rand.nextFloat() * pb * TRAIL_COLOR_JITTER);
        final float wobble = (Mth.sin(count / 2.0F) * 0.1F + 1.0F) * scale;
        return List.of(new ParticleSpawn(ModParticles.ESSENTIA_TRAIL.get(),
                new TcEssentiaTrailOptions(jittered, targetX, targetY, targetZ, count, wobble, lifetime),
                x, y, z, vx, vy, vz));
    }

    /** {@code particleCount(2)} — every infusion matrix draw loop uses base 2. */
    public static final int INFUSION_BASE_COUNT = 2;
    /** The pedestal sparkle guard: {@code world rand.nextInt(3) == 0} (TileInfusionMatrix.java:890). */
    public static final int INFUSION_SPARKLE_GUARD = 3;

    /**
     * One tick of one infusion matrix {@code SourceFX} entry, player branch
     * (TileInfusionMatrix.java:869-887 → ClientProxy.drawInfusionParticles4:664-668):
     * {@code particleCount(2)} {@code FXBoreSparkle} motes spawning on the
     * player's AABB (x/z jittered across the width, y across the body height,
     * from {@code bbMinY}) flying to {@code (mx + 0.5, my - 0.5, mz + 0.5)},
     * tinted {@code setColor(0.2, 0.6 + rand*0.3, 0.3)} over the ctor's
     * (0.2, 0.6+rand*0.3, 0.2).
     *
     * <p>Per-spawn draws in order (positional args, then ctor, then setColor):
     * 3 AABB draws (x jitter pair, y roll, z jitter pair — six floats), ctor
     * scale, lifetime, 3 drift gaussians, ctor green roll, setColor green
     * roll. The particle scale is 0.3 absolute via
     * {@code func_82338_g(0.3F)} — the 1.21.1 quad has no OBJ model,
     * documented as the billboard approximation (BD row, shared with the
     * shield runes).</p>
     */
    public static List<ParticleSpawn> infusionPlayerTick(final int mx, final int my, final int mz,
            final double px, final double pyMinY, final double pz, final double width,
            final double height, final int particleSetting, final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        for (int a = 0; a < FxParticleCounts.particleCount(INFUSION_BASE_COUNT, particleSetting); a++) {
            final double x = px + (rand.nextFloat() - rand.nextFloat()) * width;
            final double y = pyMinY + rand.nextFloat() * height;
            final double z = pz + (rand.nextFloat() - rand.nextFloat()) * width;
            final int[] bore = boreSparkleDraws(x, y, z, mx + 0.5, my - 0.5, mz + 0.5, rand);
            final int color = rgb(0.2F * 0.2F,
                    (0.6F + rand.nextFloat() * 0.3F) * 0.6F,
                    0.2F * 0.3F);
            plan.add(new ParticleSpawn(ModParticles.BORE_SPARKLE.get(),
                    new TcBoreSparkleOptions(color, mx + 0.5, my - 0.5, mz + 0.5,
                            bore[0], 0.3F),
                    x, y, z, bore[1] / 1000.0, bore[2] / 1000.0, bore[3] / 1000.0));
        }
        return plan;
    }

    /**
     * One tick of one infusion matrix {@code SourceFX} entry, pedestal branch
     * (TileInfusionMatrix.java:889-938): with a third chance a single
     * {@code FXBoreSparkle} (drawInfusionParticles3, blue-violet tint), else
     * {@code particleCount(2)} bore crumbs of the pedestal stack's block
     * (drawInfusionParticles2) or item (drawInfusionParticles1) flying into
     * the matrix head.
     */
    public static List<ParticleSpawn> infusionPedestalTick(final double sx, final double sy,
            final double sz, final int mx, final int my, final int mz,
            final BlockState pedestalBlockState, final ItemStack pedestalStack,
            final int particleSetting, final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        if (rand.nextInt(INFUSION_SPARKLE_GUARD) == 0) {
            final double x = sx + rand.nextFloat();
            final double y = sy + rand.nextFloat() + 1.0;
            final double z = sz + rand.nextFloat();
            final int[] bore = boreSparkleDraws(x, y, z, mx + 0.5, my - 0.5, mz + 0.5, rand);
            final int color = rgb(0.2F * (0.4F + rand.nextFloat() * 0.2F),
                    0.6F * 0.2F,
                    0.2F * (0.6F + rand.nextFloat() * 0.3F));
            plan.add(new ParticleSpawn(ModParticles.BORE_SPARKLE.get(),
                    new TcBoreSparkleOptions(color, mx + 0.5, my - 0.5, mz + 0.5,
                            bore[0], 1.0F),
                    x, y, z, bore[1] / 1000.0, bore[2] / 1000.0, bore[3] / 1000.0));
            return plan;
        }
        final boolean itemBranch = pedestalBlockState == null;
        for (int a = 0; a < FxParticleCounts.particleCount(INFUSION_BASE_COUNT, particleSetting); a++) {
            final double x;
            final double y;
            final double z;
            if (itemBranch) {
                x = sx + 0.4 + rand.nextFloat() * 0.2;
                y = sy + 1.23 + rand.nextFloat() * 0.2;
                z = sz + 0.4 + rand.nextFloat() * 0.2;
            } else {
                x = sx + rand.nextFloat();
                y = sy + rand.nextFloat() + 1.0;
                z = sz + rand.nextFloat();
            }
            rand.nextInt(6); // side roll (positional ctor arg; unused on 1.21.1)
            final double tx = mx + 0.5;
            final double ty = my - 0.5;
            final double tz = mz + 0.5;
            final double dist = Mth.sqrt((float) ((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z)));
            int base = (int) (dist * BORE_LIFETIME_DIST);
            if (base < 1) {
                base = 1;
            }
            rand.nextFloat(); // ctor scale roll (overridden to 0.3 by setParticleScale)
            final int lifetime = base / 2 + rand.nextInt(base);
            double vx = rand.nextGaussian() * BORE_DRIFT;
            double vy = rand.nextGaussian() * BORE_DRIFT;
            double vz = rand.nextGaussian() * BORE_DRIFT;
            if (itemBranch) {
                vx = rand.nextGaussian() * BORE_ITEM_DRIFT; // drawInfusionParticles1 velocity
                vy = rand.nextGaussian() * BORE_ITEM_DRIFT;
                vz = rand.nextGaussian() * BORE_ITEM_DRIFT;
                plan.add(new ParticleSpawn(ParticleTypes.ITEM,
                        new ItemParticleOptions(pedestalStack), x, y, z, vx, vy, vz));
            } else {
                plan.add(new ParticleSpawn(ParticleTypes.BLOCK,
                        new BlockParticleOptions(pedestalBlockState), x, y, z, vx, vy, vz));
            }
        }
        return plan;
    }

    /**
     * The {@code FXBoreSparkle} constructor draws shared by
     * drawInfusionParticles3/4 (FXBoreSparkle.java:33-45): scale roll
     * (unused — callers override), lifetime {@code nextInt(base)}, three
     * drift gaussians, and the ctor's green channel roll. Returns
     * {lifetime, vx*1000, vy*1000, vz*1000} so the caller can pack them into
     * the plan record.
     */
    private static int[] boreSparkleDraws(final double x, final double y, final double z,
            final double tx, final double ty, final double tz, final RandomSource rand) {
        rand.nextFloat(); // ctor scale roll
        final double dist = Mth.sqrt((float) ((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z)));
        int base = (int) (dist * BORE_LIFETIME_DIST);
        if (base < 1) {
            base = 1;
        }
        final int lifetime = base / 2 + rand.nextInt(base);
        final int vx = (int) (rand.nextGaussian() * 1000.0F);
        final int vy = (int) (rand.nextGaussian() * 1000.0F);
        final int vz = (int) (rand.nextGaussian() * 1000.0F);
        rand.nextFloat(); // ctor green roll (FXBoreSparkle.java:47-49)
        return new int[] {lifetime, vx, vy, vz};
    }

    /** Packs three 0..1 channels into the 0x00RRGGBB form the 1.21.1 tint uses. */
    public static int rgb(final float r, final float g, final float b) {
        return (Mth.clamp((int) (r * 255.0F), 0, 255) << 16)
                | (Mth.clamp((int) (g * 255.0F), 0, 255) << 8)
                | Mth.clamp((int) (b * 255.0F), 0, 255);
    }

    // ----- entity-space FX (PacketFXShield / PacketFXSonic) -----

    /** {@code PacketFXShield}: the rune lifetime, the literal {@code 8} of the ctor call. */
    public static final int SHIELD_LIFETIME = 8;
    /** {@code PacketFXSonic}: the pulse lifetime, the literal {@code 10} of the ctor call. */
    public static final int SONIC_LIFETIME = 10;

    /**
     * {@code PacketFXShield.onMessage} (PacketFXShield.java:63-72): one
     * {@code FXShieldRunes} riding the source entity (the packet's
     * {@code p}) with the yaw/pitch aimed from p at the target entity, at
     * the AABB centre. The particle re-anchors to the entity every tick, so
     * the spawn coordinates ride the entity, not the packet. No
     * {@code particleCount} gate — the original adds the effect directly.
     *
     * <p>The mob branch (ripple frames, b 0.5, FXShieldRunes.java:61-64) is
     * the {@code thaumcraft:shield_runes_mob} type; the 1.7.10 tree renders
     * the hemis/ripple frames on an OBJ shell (hemis.obj) — the billboard
     * quad is the documented approximation (BD row).</p>
     */
    public static List<ParticleSpawn> shieldRune(final int targetEntityId, final float yaw,
            final float pitch, final boolean mob) {
        return List.of(new ParticleSpawn(
                mob ? ModParticles.SHIELD_RUNES_MOB.get() : ModParticles.SHIELD_RUNES.get(),
                new TcShieldOptions(targetEntityId, yaw, pitch, SHIELD_LIFETIME, mob),
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
    }

    /**
     * {@code PacketFXSonic.onMessage} (PacketFXSonic.java:44-49): one
     * {@code FXSonic} riding the source entity at eye height for 10 ticks,
     * the 15-frame ripple strip. No count gate (direct {@code addEffect}).
     */
    public static List<ParticleSpawn> sonicPulse(final int targetEntityId) {
        return List.of(new ParticleSpawn(ModParticles.SONIC.get(),
                new TcSonicOptions(targetEntityId, SONIC_LIFETIME),
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
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
