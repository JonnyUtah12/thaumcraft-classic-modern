package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

    // ---- TC-M7-02-b-s: world-rendered bolt/beam family constants ----

    /** {@code ClientProxy.nodeBolt}: duration 10, multiplier 4.0, speed 5 (ClientProxy.java:789-796). */
    public static final int BOLT_DURATION = 10;
    public static final float BOLT_MULTIPLIER = 4.0F;
    public static final int BOLT_INCREMENT = 5;
    /** The bolt's ribbon half-width: {@code setWidth(0.03F)} (FXLightningBolt sender). */
    public static final float BOLT_WIDTH = 0.03F;
    /** Pass 0 (body glow, {@code p_large.png}): colour (0.6, 0.3, 0.6). */
    public static final int BOLT_COLOR_GLOW = rgb(0.6F, 0.3F, 0.6F);
    /** Pass 1 (core, {@code p_small.png}): colour (1.0, 0.6, 1.0). */
    public static final int BOLT_COLOR_CORE = rgb(1.0F, 0.6F, 1.0F);
    /** Pass count of the original's {@code getRenderPass() == 2} two-pass draw. */
    public static final int BOLT_PASSES = 2;

    /** The beam pulse age the packet sets: 20 (PacketFXBeamPulse.java:52). */
    public static final int BEAM_AGE = 20;
    /** The beam pulse size: {@code field_70130_N = 2.5} (PacketFXBeamPulse.java:44). */
    public static final float BEAM_PULSE_SIZE = 2.5F;
    /** The beam pulse texture type: 1 → beam1.png (PacketFXBeamPulse.java:45). */
    public static final int BEAM_PULSE_TYPE = 1;

    /** The golem-boss beam colour (0.07, 0.376, 0.325) (PacketFXBeamPulseGolemBoss.java:44-45). */
    public static final int GOLEM_BEAM_COLOR = rgb(0.07F, 0.376F, 0.325F);
    public static final int GOLEM_BEAM_AGE = 20;
    /** The wide beam: {@code N = 3.0}, type 2 (PacketFXBeamPulseGolemBoss.java:46-50). */
    public static final float GOLEM_N_LARGE = 3.0F;
    public static final int GOLEM_TYPE_LARGE = 2;
    /** The narrow beam: {@code N = 1.5}, type 1 (PacketFXBeamPulseGolemBoss.java:52-56). */
    public static final float GOLEM_N_SMALL = 1.5F;
    public static final int GOLEM_TYPE_SMALL = 1;

    /** The arc's height gain: 0.5 (PacketFXBlockArc.java:64). */
    public static final float ARC_HEIGHT_GAIN = 0.5F;
    /** The arc's target point: block center, {@code pos.getY() + 1} (PacketFXBlockArc.java:61-63). */
    public static final double ARC_TARGET_Y_OFFSET = 1.0;

    /** The arc's glow sparkle: scale 3.0, age {@code 3 * m = 6} with multiplier 2 (ClientProxy.java:1302). */
    public static final float SPARKLE_GLOW_SCALE = 3.0F;
    public static final int SPARKLE_GLOW_AGE = 6;
    /** The type-6 sparkle constructor's three colour draws, overwritten by setColor (FXSparkle.java:81-84). */
    public static final int SPARKLE_GLOW_CTOR_DRAWS = 3;
    public static final float SPARKLE_GLOW_CTOR_BASE = 0.8F;
    public static final float SPARKLE_GLOW_CTOR_SPAN = 0.2F;
    /** The arc handler's colour rolls (PacketFXBlockArc.java:44-50). */
    public static final float ARC_COLOR_R_BASE = 0.3F;
    public static final float ARC_COLOR_R_SPAN = 0.1F;
    public static final float ARC_COLOR_B_BASE = 0.5F;
    public static final float ARC_COLOR_B_SPAN = 0.2F;
    public static final float ARC_PORTAL_COLOR_R_BASE = 0.5F;
    public static final float ARC_PORTAL_COLOR_R_SPAN = 0.2F;

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

    /**
     * {@code PacketFXBlockZap}'s client handler — {@code proxy.nodeBolt(world,
     * x, y, z, dx, dy, dz)} (PacketFXBlockZap.java:45, ClientProxy.java:789-796).
     *
     * <p>The original builds the fractal from {@code (start, end,
     * world.rand.nextLong(), 10, 4.0F, 5)} and draws it in two passes. The
     * port builds the same fractal here from the seed — proving the geometry
     * is reproducible where no client exists — and plans one particle per
     * pass carrying the seed; the particle rebuilds the fractal from it
     * (TcBoltParticle).</p>
     *
     * @param viewDistance the render view entity's distance to the start
     *                     point, for the original's per-frame 100/50 gate
     *                     (FXLightningBolt.java:155-161)
     */
    public static List<ParticleSpawn> blockZapBolt(final Vec3 start, final Vec3 end,
            final long seed, final double viewDistance, final boolean fancyGraphics) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        if (!TcBeamGeometry.boltVisible(viewDistance, fancyGraphics)) {
            return plan;
        }
        // The original geometry: seed-reproducible, seven-pass default fractal,
        // finalized once (FXLightningBolt.java:70-77 + FXLightningBoltCommon).
        final TcBoltGeometry geometry = new TcBoltGeometry(
                new TcFxVector3(start.x, start.y, start.z),
                new TcFxVector3(end.x, end.y, end.z), seed,
                BOLT_DURATION, BOLT_MULTIPLIER, BOLT_INCREMENT);
        geometry.defaultFractal();
        geometry.finalizeBolt();
        if (geometry.segments.isEmpty()) {
            return plan;
        }
        for (int pass = 0; pass < BOLT_PASSES; pass++) {
            plan.add(new ParticleSpawn(ModParticles.BOLT.get(),
                    new TcBoltOptions(end.x, end.y, end.z, seed,
                            pass == 0 ? BOLT_COLOR_GLOW : BOLT_COLOR_CORE, pass),
                    start.x, start.y, start.z, 0.0, 0.0, 0.0));
        }
        return plan;
    }

    /**
     * {@code PacketFXBlockArc}'s client handler — the colour draws, then
     * {@code proxy.arcLightning(world, source, target, r, g, b, 0.5F)}
     * (PacketFXBlockArc.java:41-66, ClientProxy.java:1301-1309).
     *
     * <p>The draw order is the handler's: the two colour rolls (portal
     * branch: one roll for red), the glow sparkle constructor's three
     * type-6 rolls (overwritten by {@code setColor}, but consumed), the
     * arc seed, then the arc jitter inside {@link TcArcGeometry} (two
     * draws per axis per point, x, y, z).</p>
     *
     * @param cultistPortal the original's {@code instanceof EntityCultistPortal}
     *                      colour branch; the port's boss family is on another
     *                      card, so the caller passes false and the branch is
     *                      kept plan-visible (docs/FEATURE_MATRIX.md note)
     */
    public static List<ParticleSpawn> blockArc(final Vec3 source, final double targetX,
            final double targetY, final double targetZ, final RandomSource rand,
            final boolean cultistPortal) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        float r = ARC_COLOR_R_BASE - rand.nextFloat() * ARC_COLOR_R_SPAN;
        final float g = 0.0F;
        float b = ARC_COLOR_B_BASE + rand.nextFloat() * ARC_COLOR_B_SPAN;
        if (cultistPortal) {
            r = ARC_PORTAL_COLOR_R_BASE + rand.nextFloat() * ARC_PORTAL_COLOR_R_SPAN;
            b = 0.0F;
        }
        // The FXSparkle constructor's type-6 rolls (FXSparkle.java:81-84),
        // consumed before the sender overwrites the colour (ClientProxy.java:1305).
        for (int draw = 0; draw < SPARKLE_GLOW_CTOR_DRAWS; draw++) {
            rand.nextFloat();
        }
        final int color = rgb(r, g, b);
        plan.add(new ParticleSpawn(ModParticles.SPARKLE_GLOW.get(),
                new TcGlowSparkleOptions(color, SPARKLE_GLOW_SCALE, SPARKLE_GLOW_AGE),
                targetX, targetY, targetZ, 0.0, 0.0, 0.0));
        final long seed = rand.nextLong();
        plan.add(new ParticleSpawn(ModParticles.ARC.get(),
                new TcArcOptions(targetX - source.x, targetY - source.y, targetZ - source.z,
                        ARC_HEIGHT_GAIN, color, seed),
                source.x, source.y, source.z, 0.0, 0.0, 0.0));
        return plan;
    }

    /**
     * {@code PacketFXBeamPulse}'s client handler — one {@code FXBeam} at the
     * source's eye position, aimed at the live target, size 2.5, type 1,
     * reverse, pulse, age 20 (PacketFXBeamPulse.java:38-58).
     *
     * @param viewDistance the render view entity's distance to the source,
     *                     for the 50/25 gate (FXBeam.java:66-73)
     */
    public static List<ParticleSpawn> beamPulse(final Vec3 sourceEye, final int targetEntityId,
            final int color, final double viewDistance, final boolean fancyGraphics) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        final int age = TcBeamGeometry.visibleAge(viewDistance, BEAM_AGE, fancyGraphics);
        if (age == 0) {
            return plan;
        }
        plan.add(new ParticleSpawn(ModParticles.BEAM.get(),
                new TcBeamOptions(targetEntityId, color, BEAM_PULSE_SIZE, BEAM_PULSE_TYPE, true, age),
                sourceEye.x, sourceEye.y, sourceEye.z, 0.0, 0.0, 0.0));
        return plan;
    }

    /**
     * {@code PacketFXBeamPulseGolemBoss}'s client handler — two
     * {@code FXBeamGolemBoss} beams from the boss-head anchor (N 3.0 type 2,
     * N 1.5 type 1, colour (0.07, 0.376, 0.325), pulse, age 20), each with
     * the impact sprite the original draws inside its render pass
     * (PacketFXBeamPulseGolemBoss.java:38-60, FXBeamGolemBoss.java:232-262).
     *
     * @param headAnchor the boss-head anchor from {@link TcBeamGeometry.headAnchor}
     */
    public static List<ParticleSpawn> beamPulseGolemBoss(final Vec3 headAnchor,
            final int bossEntityId, final int targetEntityId,
            final double viewDistance, final boolean fancyGraphics) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        final int age = TcBeamGeometry.visibleAge(viewDistance, GOLEM_BEAM_AGE, fancyGraphics);
        if (age == 0) {
            return plan;
        }
        final float[][] passes = {
            {GOLEM_N_LARGE, GOLEM_TYPE_LARGE},
            {GOLEM_N_SMALL, GOLEM_TYPE_SMALL},
        };
        for (final float[] beam : passes) {
            plan.add(new ParticleSpawn(ModParticles.BEAM_GOLEM_BOSS.get(),
                    new TcGolemBeamOptions(bossEntityId, targetEntityId, GOLEM_BEAM_COLOR,
                            beam[0], (int) beam[1], age),
                    headAnchor.x, headAnchor.y, headAnchor.z, 0.0, 0.0, 0.0));
            plan.add(new ParticleSpawn(ModParticles.BEAM_IMPACT.get(),
                    new TcBeamOptions(targetEntityId, GOLEM_BEAM_COLOR, beam[0],
                            (int) beam[1], false, age),
                    headAnchor.x, headAnchor.y, headAnchor.z, 0.0, 0.0, 0.0));
        }
        return plan;
    }

    // ---- TC-M7-02-c-s-f1: the wisp family's client senders ----

    /**
     * The scale of {@code FXWisp}'s entity-target constructor
     * ({@code FXWisp(world, d, d1, d2, tar, int type)} delegating to
     * {@code this(world, d, d1, d2, 0.4F, type)}, FXWisp.java:115-118).
     */
    public static final float WISP_TYPE_SCALE = 0.4F;

    /** The base count every wisp sender hands to {@code particleCount} (the wispFX family). */
    public static final int WISP_BASE_COUNT = 1;

    /** {@code ClientProxy.wispFX}'s gravity knob, the 0.02 the payload documents. */
    public static final float WISP_GRAVITY = 0.02F;

    /**
     * The wisp sender: one {@code FXWisp} per {@code particleCount(1)}
     * iteration from the sender's explicit colour triple — the original's
     * 8-argument constructor (FXWisp.java:20-60) and, when {@code target} is
     * not the spawn point, its 9-argument point-target sibling
     * (FXWisp.java:117-125), which divides the delta to the target by the
     * rolled lifetime and hands that in as the initial velocity.
     *
     * <p>An entity target ({@code targetEntityId != TcWispOptions.NO_TARGET})
     * switches the mote onto the original's homing branch ({@code target !=
     * null} in {@code onUpdate}, FXWisp.java:150-164) and leaves the velocity
     * at zero, which is what the entity-target constructor does.</p>
     *
     * @param spawn  the sender's spawn point (the mote's initial position)
     * @param target the point the mote travels to, or {@code spawn} for the
     *               colour constructor's free-drifting mote
     * @param culled true when the spawn point is past the sender's render
     *               distance: the original zeroes the lifetime there, so no
     *               mote appears (FXWisp.java:40-48)
     */
    public static List<ParticleSpawn> wisp(final Vec3 spawn, final Vec3 target, final float scale,
            final int color, final boolean additive, final int targetEntityId,
            final boolean shrink, final float gravity, final boolean clip, final boolean culled,
            final int particleSetting, final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        final boolean homing = targetEntityId != TcWispOptions.NO_TARGET;
        final int count = FxParticleCounts.particleCount(WISP_BASE_COUNT, particleSetting);
        for (int i = 0; i < count; i++) {
            // The lifetime rolls before the cull test in the original — the
            // constructor computes it and then zeroes it — so the draw is
            // consumed either way.
            final int lifetime = FxWispRules.wispLifetime(rand);
            if (culled) {
                continue;
            }
            final double vx = homing ? 0.0 : (target.x - spawn.x) / lifetime;
            final double vy = homing ? 0.0 : (target.y - spawn.y) / lifetime;
            final double vz = homing ? 0.0 : (target.z - spawn.z) / lifetime;
            plan.add(new ParticleSpawn(ModParticles.WISP.get(),
                    new TcWispOptions(fixZeroRed(color), scale, lifetime, targetEntityId, homing,
                            shrink, gravity, clip, additive),
                    spawn.x, spawn.y, spawn.z, vx, vy, vz));
        }
        return plan;
    }

    /**
     * The wisp sender's type branch: the original's entity-target constructor
     * {@code FXWisp(world, d, d1, d2, Entity tar, int type)} →
     * {@code this(world, d, d1, d2, 0.4F, type)} (FXWisp.java:115-118), whose
     * colour case also picks the blend layer (case 5 → additive,
     * FXWisp.java:100). The type is the sender's flavour: 0 wisp purple,
     * 1 amber, 2/3/4 the prim, 5 the additive dark mote, 6 white, 7 fiery
     * (FXWisp.java:66-115).
     */
    public static List<ParticleSpawn> wispByType(final Vec3 spawn, final int type,
            final int targetEntityId, final boolean shrink, final float gravity,
            final boolean clip, final boolean culled, final int particleSetting,
            final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        final boolean homing = targetEntityId != TcWispOptions.NO_TARGET;
        final int count = FxParticleCounts.particleCount(WISP_BASE_COUNT, particleSetting);
        for (int i = 0; i < count; i++) {
            final int lifetime = FxWispRules.wispLifetime(rand);
            // The colour switch runs after the delegated constructor, so its
            // rolls come after the lifetime's (FXWisp.java:63-115).
            final FxWispRules.WispColor rolled = FxWispRules.wispColor(type, rand);
            if (culled) {
                continue;
            }
            plan.add(new ParticleSpawn(ModParticles.WISP.get(),
                    new TcWispOptions(rgb(rolled.r(), rolled.g(), rolled.b()), WISP_TYPE_SCALE,
                            lifetime, targetEntityId, homing, shrink, gravity, clip,
                            rolled.additive()),
                    spawn.x, spawn.y, spawn.z, 0.0, 0.0, 0.0));
        }
        return plan;
    }

    /**
     * {@code ClientProxy.wispFXEG}: the dark additive EG mote that falls onto
     * an entity (FXWispEG.java:18-49). The constructor rolls its own x/z
     * gaussians, its lifetime and its colour, so the loop is the
     * {@code particleCount} gate around those rolls, in the constructor's
     * order (motion → lifetime → colour, FXWispEG.java:24-46).
     *
     * @param spawn          the spawn point the sender hands the constructor
     * @param targetEntityId the entity the mote falls onto and rides
     * @param culled         true past the sender's render distance
     *                       (FXWispEG.java:35-41)
     */
    public static List<ParticleSpawn> wispEg(final Vec3 spawn, final int targetEntityId,
            final boolean culled, final int particleSetting, final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        final int count = FxParticleCounts.particleCount(WISP_BASE_COUNT, particleSetting);
        for (int i = 0; i < count; i++) {
            final double vx = rand.nextGaussian() * FxWispRules.EG_SPREAD;
            final double vz = rand.nextGaussian() * FxWispRules.EG_SPREAD;
            final int lifetime = FxWispRules.wispEgLifetime(rand);
            final FxWispRules.WispColor rolled = FxWispRules.wispEgColor(rand);
            if (culled) {
                continue;
            }
            plan.add(new ParticleSpawn(ModParticles.WISP_EG.get(),
                    new TcWispEgOptions(rgb(rolled.r(), rolled.g(), rolled.b()), lifetime,
                            targetEntityId),
                    spawn.x, spawn.y, spawn.z, vx, FxWispRules.EG_RISE, vz));
        }
        return plan;
    }

    /**
     * {@code ClientProxy.sourceStreamFX}: the arcing wisp of an essentia
     * stream. The original's 11-argument constructor
     * {@code FXWispArcing(world, d, d1, d2, x, y, z, f, r, g, b)}
     * (FXWispArcing.java:51-59) leaves the mote's <em>anchor</em>
     * ({@code field_70568_aq/ar/as}, set by the delegated constructor) on the
     * first triple, repositions it to the second ({@code func_70012_b(x, y,
     * z)}), and puts {@code second - first} in its motion fields; the tick then
     * re-derives the position as {@code anchor + motion * (1 - age/lifetime)}
     * (FXWispArcing.java:96-103), so the mote starts at {@code start} and
     * converges on {@code anchor} as it ages. The port keeps that motion triple
     * in the payload ({@link TcWispArcingOptions}) because its particle writes
     * absolute positions, so the spawn velocity is zero.
     *
     * @param anchor the first triple: the point the mote converges on (the
     *               cull is measured against it, FXWispArcing.java:40-48)
     * @param start  the second triple: the mote's initial position
     * @param color  the sender's aspect-tag colour, packed 0x00RRGGBB
     * @param culled true past the sender's render distance
     */
    public static List<ParticleSpawn> wispArcing(final Vec3 anchor, final Vec3 start,
            final int color, final boolean culled, final int particleSetting,
            final RandomSource rand) {
        final List<ParticleSpawn> plan = new ArrayList<>();
        final int count = FxParticleCounts.particleCount(WISP_BASE_COUNT, particleSetting);
        for (int i = 0; i < count; i++) {
            final int lifetime = FxWispRules.wispLifetime(rand);
            if (culled) {
                continue;
            }
            plan.add(new ParticleSpawn(ModParticles.WISP_ARCING.get(),
                    new TcWispArcingOptions(fixZeroRed(color), FxWispRules.ARCING_SCALE, lifetime,
                            start.x - anchor.x, start.y - anchor.y, start.z - anchor.z),
                    anchor.x, anchor.y, anchor.z, 0.0, 0.0, 0.0));
        }
        return plan;
    }

    /**
     * The red-channel fix both {@code FXWisp} and {@code FXWispArcing} apply at
     * the top of their colour constructors ({@code if (red == 0.0F) red =
     * 1.0F;}, FXWisp.java:25-27 and FXWispArcing.java:22-24): a sender that
     * passes no red channel gets a fully red one rather than a black mote.
     * Packed form, so the test is against the red byte.
     */
    private static int fixZeroRed(final int color) {
        return (color >> 16 & 0xFF) == 0 ? color | 0xFF0000 : color;
    }

    /** Packs three 0..1 channels into the 0x00RRGGBB form the 1.21.1 tint uses. */
    public static int rgb(final float r, final float g, final float b) {
        return (Mth.clamp((int) (r * 255.0F), 0, 255) << 16)
                | (Mth.clamp((int) (g * 255.0F), 0, 255) << 8)
                | Mth.clamp((int) (b * 255.0F), 0, 255);
    }

    // ==================================================================
    // TC-M7-02-c-s-f5: the shared particle families of the original's
    // client/fx packages, mined from wt/m7-fx-wisps @ 535d816 and
    // re-grounded against the decompiled senders. Every loop keeps the
    // original's left-to-right random-draw order.
    // ==================================================================

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
     * 2D crumb has no 1.21.1 billboard equivalent (BD-18). The original's
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
                        new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, vx, vy, vz));
            } else {
                plan.add(new ParticleSpawn(ParticleTypes.ITEM,
                        new ItemParticleOption(ParticleTypes.ITEM, stack), x, y, z, vx, vy, vz));
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
     documented as the billboard approximation (BD-17, shared with
          * the shield runes).
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
                        new ItemParticleOption(ParticleTypes.ITEM, pedestalStack), x, y, z, vx, vy, vz));
            } else {
                plan.add(new ParticleSpawn(ParticleTypes.BLOCK,
                        new BlockParticleOption(ParticleTypes.BLOCK, pedestalBlockState), x, y, z, vx, vy, vz));
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


    // ----- UtilsFX.shootFire: the scorch fan -----

    /**
     * The scorch fan of {@code UtilsFX.shootFire}
     * (reference/decompiled/thaumcraft/thaumcraft/client/lib/UtilsFX.java:365-386):
     * three motes, each aimed 0.3 further along the throw vector, each handed the
     * same {@code vec3d}/{@code range}/{@code lance} the caller passed. The
     * {@code FXScorch} ctor (fx/particles/FXScorch.java:26-40) draws the six
     * jitter rolls first (two per axis, the 0.5 spread on a lance, the 1.0
     * spread on a mote), then the scale roll ({@code rand*0.5+2} lance,
     * {@code rand+3} mote) — and, as its velocity, divides the throw vector by
     * 100, which is the scale the plan's options carry.
     */
    public static List<ParticleSpawn> scorch(final double x, final double y, final double z,
            final double vx, final double vy, final double vz, final double range,
            final boolean lance, final RandomSource rand) {
        final List<ParticleSpawn> spawns = new ArrayList<>();
        // Lines 369-385: the loop advances the aim 0.3 per mote and re-draws the
        // ctor's rolls for each.
        double px = x;
        double py = y;
        double pz = z;
        for (int q = 0; q < 3; q++) {
            px += vx * 0.3;
            py += vy * 0.3;
            pz += vz * 0.3;
            final double jx = spread(vx, vy, vz, lance, 0.5F, 1.0F, rand);
            final double jy = spread(vx, vy, vz, lance, 0.5F, 1.0F, rand);
            final double jz = spread(vx, vy, vz, lance, 0.5F, 1.0F, rand);
            final double sx = spread(vx, vy, vz, lance, 0.5F, 0.5F, rand);
            final double sy = spread(vx, vy, vz, lance, 0.5F, 0.5F, rand);
            final double sz = spread(vx, vy, vz, lance, 0.5F, 0.5F, rand);
            final double scale = lance ? rand.nextDouble() * 0.5 + 2.0 : rand.nextDouble() + 3.0;
            // The ctor's jitter/scale rolls are consumed above in the original's
            // order; the payload carries the aim and the scale (the mote's own
            // velocity is the throw vector /100, which the particle re-derives
            // from the spawn vector). 1.21.1 seam: the jitter the ctor baked into
            // the mote's position is not carried per mote (BD-18).
            spawns.add(new ParticleSpawn(ModParticles.SCORCH.get(),
                    new TcScorchOptions(px + jx, py + jy, pz + jz, (float) scale),
                    px, py, pz, vx / 100.0, vy / 100.0, vz / 100.0));
        }
        return spawns;
    }

    private static double spread(final double vx, final double vy, final double vz,
            final boolean lance, final float lanceSpread, final float moteSpread,
            final RandomSource rand) {
        // FXScorch.java:36-38: (rand - rand) * spread, the lance spread tighter.
        final double s = lance ? lanceSpread : moteSpread;
        return (rand.nextDouble() - rand.nextDouble()) * s;
    }

    // ----- ClientProxy.smokeSpiral -----

    /**
     * The smoke spiral of {@code ClientProxy.smokeSpiral}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:644-649):
     * one mote at the caller's radius/height, with the ctor's lifetime roll
     * (FXSmokeSpiral.java:26-27, {@code 20 + rand(10)}). The start angle is
     * fixed at 0 — the spiral's wind-up comes from {@code angle += 720 * age /
     * lifetime}, not a random phase.
     */
    public static List<ParticleSpawn> smokeSpiral(final double x, final double y, final double z,
            final double radius, final double height, final RandomSource rand) {
        // FXSmokeSpiral.java:26-27: the ctor's only draw is the lifetime roll.
        final int lifetime = 20 + rand.nextInt(10);
        return List.of(new ParticleSpawn(ModParticles.SMOKE_SPIRAL.get(),
                new TcSmokeSpiralOptions((float) radius, 0, y + height, lifetime, 0xFFFFFF),
                x + radius, y + height, z, 0.0, 0.0, 0.0));
    }

    // ----- ClientProxy.soulTrail: the smoke trail -----

    /**
     * The soul trail of {@code ClientProxy.soulTrail}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:916-941):
     * two motes per entity tick (the original's {@code particleCount(2)}), each
     * after a 1-in-10 gate, each spawned at a random point in the entity's box.
     * The {@code FXSmokeTrail} ctor (fx/particles/FXSmokeTrail.java:24-47) then
     * draws, in order: the scale ({@code rand*0.5+0.5}), the distance-derived
     * base and its lifetime roll ({@code base/2 + rand(base)}), and the three
     * velocity rolls ({@code (rand-rand)*0.1}) — all carried in the payload.
     */
    public static List<ParticleSpawn> soulTrail(final int entityId, final double x, final double y,
            final double z, final double width, final double height, final RandomSource rand) {
        final List<ParticleSpawn> spawns = new ArrayList<>();
        for (int n = 0; n < FxParticleCounts.particleCount(2, 100); n++) {
            // Lines 918-921: a 1-in-10 gate on every iteration.
            if (rand.nextInt(10) != 0) {
                continue;
            }
            // Lines 922-926: the spawn point is a corner of the entity's box.
            final double sx = x - width / 2.0 + rand.nextDouble() * width;
            final double sy = y + rand.nextDouble() * height;
            final double sz = z - width / 2.0 + rand.nextDouble() * width;
            // FXSmokeTrail.java:25-26,40-42,44-46: scale, base, lifetime, velocity.
            final double scale = rand.nextDouble() * 0.5 + 0.5;
            final double d = Math.sqrt(sx * sx + sy * sy + sz * sz);
            final int base = (int) (d > 1.0E-4 ? d : 1.0);
            final int lifetime = base / 2 + rand.nextInt(base);
            // The ctor's three velocity rolls (FXSmokeTrail.java:44-46) are
            // consumed here in the original's order; the mote's drift is the
            // platform's own spawn velocity, and the payload keeps the target,
            // the colour, the lifetime and the scale.
            final double driftX = (rand.nextDouble() - rand.nextDouble()) * 0.1;
            final double driftY = (rand.nextDouble() - rand.nextDouble()) * 0.1;
            final double driftZ = (rand.nextDouble() - rand.nextDouble()) * 0.1;
            spawns.add(new ParticleSpawn(ModParticles.SMOKE_TRAIL.get(),
                    new TcSmokeTrailOptions(entityId, 0xBEBEBE, lifetime, (float) scale),
                            sx, sy, sz, driftX, driftY, driftZ));
        }
        return spawns;
    }

    // ----- ClientProxy.swarmParticleFX -----

    /**
     * The swarm mote of {@code ClientProxy.swarmParticleFX}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:987-1000):
     * six jitter rolls around the caller's point, then the colour rolls —
     * r {@code 0.8+rand*0.2}, g {@code rand*0.4}, b {@code 1.0-rand*0.2} — and
     * the {@code FXSwarm} ctor's own rolls (fx/particles/FXSwarm.java:28-32):
     * the scale {@code rand*0.5+1.0} and the three velocity rolls
     * {@code (rand-rand)*0.2}. The caller owns the 1-in-N spawn gate.
     */
    public static List<ParticleSpawn> swarm(final int entityId, final double x, final double y,
            final double z, final int color, final RandomSource rand) {
        final double sx = x + (rand.nextDouble() - rand.nextDouble()) * 2.0;
        final double sy = y + (rand.nextDouble() - rand.nextDouble()) * 2.0;
        final double sz = z + (rand.nextDouble() - rand.nextDouble()) * 2.0;
        final double r = 0.8 + rand.nextDouble() * 0.2;
        final double g = rand.nextDouble() * 0.4;
        final double b = 1.0 - rand.nextDouble() * 0.2;
        final double scale = rand.nextDouble() * 0.5 + 1.0;
        // The ctor's three velocity rolls (FXSwarm.java:30-32) are consumed here
        // in the original's order and ride the spawn velocity.
        final double driftX = (rand.nextDouble() - rand.nextDouble()) * 0.2;
        final double driftY = (rand.nextDouble() - rand.nextDouble()) * 0.2;
        final double driftZ = (rand.nextDouble() - rand.nextDouble()) * 0.2;
        return List.of(new ParticleSpawn(ModParticles.SWARM.get(),
                new TcSwarmOptions(entityId, rgb((float) r, (float) g, (float) b),
                        (float) scale, TcSwarmOptions.DEFAULT_SPEED, TcSwarmOptions.DEFAULT_TURN_SPEED,
                        TcSwarmOptions.DEFAULT_GRAVITY),
                        sx, sy, sz, driftX, driftY, driftZ));
    }

    // ----- ClientProxy.drawVentParticles: the steam puff -----

    /**
     * The steam vent puff of {@code ClientProxy.drawVentParticles}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:1215-1219):
     * one mote per block-face call with the caller's spread and
     * {@code steam=true}. The {@code FXVent} ctor (fx/particles/FXVent.java:24-31)
     * draws the scale roll {@code rand*0.1+0.05} first; {@code setHeading}
     * (lines 53-80) then draws, per axis, a gaussian and a sign flip — six
     * draws — at the 0.0075-per-spread rate.
     */
    public static List<ParticleSpawn> vent(final double x, final double y, final double z,
            final double nx, final double ny, final double nz, final double spread,
            final RandomSource rand) {
        // FXVent.java:35: the ctor's scale roll.
        final double scale = rand.nextDouble() * 0.1 + 0.05;
        // FXVent.java:55-57: the throw direction is normalised.
        final double len = Math.max(1.0E-4, Math.sqrt(nx * nx + ny * ny + nz * nz));
        final double ux = nx / len;
        final double uy = ny / len;
        final double uz = nz / len;
        final double wx = Math.abs(ux);
        final double wy = Math.abs(uy);
        final double wz = Math.abs(uz);
        // FXVent.java:58-62: a gaussian per axis, then a sign flip per axis.
        double vx = rand.nextGaussian() * wx * 0.0075 * spread + ux * 0.6;
        if (rand.nextBoolean()) {
            vx = -vx;
        }
        double vy = rand.nextGaussian() * wy * 0.0075 * spread + uy * 0.6;
        if (rand.nextBoolean()) {
            vy = -vy;
        }
        double vz = rand.nextGaussian() * wz * 0.0075 * spread + uz * 0.6;
        if (rand.nextBoolean()) {
            vz = -vz;
        }
        vz += (rand.nextDouble() - rand.nextDouble()) * 0.1;
        vx += (rand.nextDouble() - rand.nextDouble()) * 0.1;
        vy += (rand.nextDouble() - rand.nextDouble()) * 0.1;
        return List.of(new ParticleSpawn(ModParticles.VENT.get(),
                new TcVentOptions(0xFFFFFF, (float) scale,
                        (float) (rand.nextDouble() * 0.2 + 0.8), 1.0F),
                x, y, z, vx, vy, vz));
    }

    // ----- ClientProxy.dropletFX: the vis droplet -----

    /**
     * The vis droplet of {@code ClientProxy.dropletFX}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:1137-1141):
     * one mote carrying the caller's colour. The {@code FXDrop} ctor
     * (fx/particles/FXDrop.java:29) draws its lifetime from
     * {@code Math.random()} — the original's own source, not the world random —
     * so the caller passes the drawn value in.
     */
    public static List<ParticleSpawn> drop(final double x, final double y, final double z,
            final int color, final double lifetimeRoll, final RandomSource rand) {
        final int lifetime = (int) (TcDropOptions.LIFETIME_BASE
                / (lifetimeRoll * TcDropOptions.LIFETIME_SPAN + TcDropOptions.LIFETIME_MIN));
        return List.of(new ParticleSpawn(ModParticles.DROP.get(),
                new TcDropOptions(color, lifetime), x, y, z, 0.0, 0.0, 0.0));
    }

    // ----- ClientProxy.burst: the node burst -----

    /**
     * The node burst of {@code ClientProxy.burst}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:747-751):
     * one ring at the node's centre with the caller's size. The class draws
     * nothing (FXBurst.java:12-23) — every value is the ctor's constant.
     */
    public static List<ParticleSpawn> burst(final double x, final double y, final double z,
            final float size) {
        return List.of(new ParticleSpawn(ModParticles.BURST.get(),
                new TcBurstOptions(size), x + 0.5, y + 0.5, z + 0.5, 0.0, 0.0, 0.0));
    }

    // ----- ClientProxy.splooshFX: the breaking crumb -----

    /**
     * The breaking crumb of {@code ClientProxy.splooshFX(Entity)}
     * (reference/decompiled/thaumcraft/thaumcraft/client/ClientProxy.java:1006-1025)
     * and its (world,x,y,z) twin (lines 1027-1040): a single
     * {@code FXBreaking} (fx/particles/FXBreaking.java) — an item-stack crumb of
     * the iron pickaxe the sender passes, 0.1-scaled, that fades over its life.
     *
     * <p>1.21.1 mapping (BD-18): the crumb is a whole item icon, not a
     * billboard cell of the particle sheet, so the family maps to the
     * platform's own item crumb ({@link net.minecraft.core.particles.ParticleTypes#ITEM}),
     * which carries the stack. The draws below are the sender's, in order:
     * the angle, the radius factor, the height roll, the colour-variant coin
     * and the lifetime roll — the ctor draws nothing (FXBreaking.java:18-23).</p>
     */
    public static List<ParticleSpawn> blockBreaking(final double x, final double y, final double z,
            final double width, final double height, final RandomSource rand) {
        // Lines 1007-1010: the angle and the radius factor.
        final double angle = rand.nextDouble() * Math.PI * 2.0;
        final double radiusFactor = rand.nextDouble() * 0.5 + 0.5;
        final double ox = Math.cos(angle) * 2.0 * 0.5 * radiusFactor;
        final double oz = Math.sin(angle) * 2.0 * 0.5 * radiusFactor;
        // Line 1015: the height roll.
        final double sy = y + rand.nextDouble() * height;
        // Lines 1019-1024: the colour-variant coin (a tint the crumb carries).
        final boolean warm = rand.nextBoolean();
        // Line 1025: the lifetime roll 66 / (rand*0.9 + 0.1).
        final int lifetime = (int) (66.0 / (rand.nextDouble() * 0.9 + 0.1));
        return List.of(new ParticleSpawn(
                net.minecraft.core.particles.ParticleTypes.ITEM,
                new net.minecraft.core.particles.ItemParticleOption(
                        net.minecraft.core.particles.ParticleTypes.ITEM,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE)),
                x + ox, sy, z + oz, 0.0, 0.0, 0.0));
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
