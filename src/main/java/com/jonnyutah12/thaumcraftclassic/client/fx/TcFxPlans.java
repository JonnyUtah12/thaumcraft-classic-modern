package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.jonnyutah12.thaumcraftclassic.registration.ModParticles;
import java.util.ArrayList;
import java.util.List;
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
     * stream. The original's 11-argument constructor parks the mote on the
     * TARGET point and stores the delta to the source in its motion fields
     * (FXWispArcing.java:51-59); {@code onUpdate} re-derives the position from
     * the spawn anchor plus that delta every tick, so the delta is payload and
     * the spawn velocity is zero.
     *
     * @param source the point the mote flies back toward (the stream's source)
     * @param target the spawn anchor, i.e. the target the stream runs to
     * @param color  the sender's aspect-tag colour, packed 0x00RRGGBB
     * @param culled true past the sender's render distance
     *               (FXWispArcing.java:40-48)
     */
    public static List<ParticleSpawn> wispArcing(final Vec3 source, final Vec3 target,
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
                            source.x - target.x, source.y - target.y, source.z - target.z),
                    target.x, target.y, target.z, 0.0, 0.0, 0.0));
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
