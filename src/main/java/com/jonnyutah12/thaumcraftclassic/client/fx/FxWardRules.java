package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The ward hexagram's measured rules — {@code FXBlockWard.java} (113 lines)
 * line by line (TC-M7-02-c-s-f4, D-017): constructor 19-47, draw 49-87, tick
 * 89-108. The sender is {@code ClientProxy.blockWard}
 * ({@code ClientProxy.java:980-984}, which adds the block-centre
 * {@code + 0.5} itself) and its two callers are
 * {@code BlockWarded.addHitEffects} (BlockWarded.java:64-70) and
 * {@code BlockCosmeticOpaque.addHitEffects} (BlockCosmeticOpaque.java:68-82,
 * metadata 2 only).
 *
 * <p>Kept out of the client particle so a dedicated server (the GameTest
 * server) and a plain unit test can pin every constant, every roll and the
 * face geometry without a client load — the gate shape the FX lane runs on.</p>
 *
 * <h2>Three things the located file says that its summaries did not</h2>
 * <ul>
 *   <li>The alpha law is all floats: {@code threshold = particleMaxAge / 5.0F},
 *   alpha is {@code particleAge / threshold} up to the threshold and
 *   {@code (particleMaxAge - particleAge) / particleMaxAge} after it
 *   (lines 93-98). It is a fade-in then a fade-out, not a two-tick flash.</li>
 *   <li>The jitter zeroes the component <em>along the hit normal</em>
 *   ({@code if (side.offsetX != 0) sx = 0}, …, lines 36-46) — not the two
 *   plane components.</li>
 *   <li>The quad maps the whole bound frame (u 0..1, v 0..1, lines 77-80) of
 *   {@code hemis<frame>.png} (line 54). There is no {@code v = 0.25} strip in
 *   this file, and the frame index is
 *   {@code min(15, (int)(15.0F * fade))} over the fade that includes the
 *   partial tick (lines 52-53).</li>
 * </ul>
 *
 * <p>The 16 hemis frames are 128x128 renders of the OBJ hemisphere
 * {@code hemis.obj}; a 1.21.1 particle is one billboard quad, so the port
 * draws the frame art as one quad with the located rotation chain on top. The
 * OBJ surface is BD-46 in docs/BEHAVIORAL_DEVIATIONS.md.</p>
 */
public final class FxWardRules {

    /** {@code field_70545_g = 0.0F} — the mote never falls (FXBlockWard.java:22). */
    public static final float GRAVITY = 0.0F;

    /** {@code particleMaxAge = 12 + rand.nextInt(5)} (FXBlockWard.java:24). */
    public static final int LIFETIME_BASE = 12;
    public static final int LIFETIME_ROLLS = 5;

    /**
     * {@code particleScale = 1.4F + rand.nextGaussian() * 0.3F}
     * (FXBlockWard.java:31).
     */
    public static final double SCALE_BASE = 1.4;
    public static final double SCALE_GAUSSIAN = 0.3;

    /**
     * The draw's quad corners are {@code -0.5 * particleScale} to
     * {@code +0.5 * particleScale} (lines 72, 77-80), and 1.21.1's
     * {@code quadSize} <em>is</em> the half-extent
     * ({@code SingleQuadParticle.renderVertex}), so the half-extent is
     * {@code scale / 2}.
     */
    public static final double QUAD_HALF_EXTENT_DIVISOR = 2.0;

    /** {@code rotation = rand.nextInt(360)} (line 32), in degrees. */
    public static final int ROTATION_ROLLS = 360;

    /**
     * The jitter: {@code MathHelper.clamp(f - 0.6F + rand.nextFloat() * 0.2F,
     * -0.4F, 0.4F)} on each of the sender's three offsets (lines 33-35).
     */
    public static final double JITTER_FROM_HIT = -0.6;
    public static final double JITTER_SPAN = 0.2;
    public static final double JITTER_MIN = -0.4;
    public static final double JITTER_MAX = 0.4;

    /**
     * The constructor zeroes the jitter component that lies along the hit
     * normal: {@code if (side.offsetX != 0) sx = 0; if (side.offsetY != 0)
     * sy = 0; if (side.offsetZ != 0) sz = 0;} (lines 36-46), in X, Y, Z order.
     */
    public static boolean zeroesAxis(final Direction side, final Direction.Axis axis) {
        return switch (axis) {
            case X -> side.getStepX() != 0;
            case Y -> side.getStepY() != 0;
            case Z -> side.getStepZ() != 0;
        };
    }

    /** One jitter component, zeroed when it lies along the hit normal. */
    public static double zeroJitter(final double jitterX, final double jitterY,
            final double jitterZ, final Direction side, final Direction.Axis axis) {
        if (zeroesAxis(side, axis)) {
            return 0.0;
        }
        return switch (axis) {
            case X -> jitterX;
            case Y -> jitterY;
            case Z -> jitterZ;
        };
    }

    /**
     * The draw pushes the quad this far along its own z axis:
     * {@code +0.505F} when {@code side.offsetZ > 0}, {@code -0.505F} otherwise
     * (lines 65-70).
     */
    public static final float FACE_OFFSET = 0.505F;

    /** The draw's {@code glColor4f(1, 1, 1, particleAlpha / 2.0F)} (line 58, 76). */
    public static final float DRAW_ALPHA_DIVISOR = 2.0F;

    /**
     * EntityFX's default {@code particleAlpha = 1.0F}: the constructor never
     * touches it (lines 19-47), so the first render — before the first tick —
     * draws at half alpha (line 58).
     */
    public static final float INITIAL_ALPHA = 1.0F;

    /** The tick's fade window: {@code threshold = particleMaxAge / 5.0F} (line 93). */
    public static final float FADE_WINDOW_DIVISOR = 5.0F;

    /**
     * The draw's block light: {@code tessellator.setBrightness(240)} (line 75)
     * — full bright. 1.21.1's particle has no numeric block light, only a
     * packed world light, so the port pins the brightest block light (BD-46).
     */
    public static final int FULL_BRIGHT_BLOCK_LIGHT = 240;

    /** The frames the family ships: {@code hemis0..hemis15} (line 54). */
    public static final int FRAME_COUNT = 16;

    /** {@code fade = (particleAge + partialTick) / particleMaxAge} (line 52). */
    public static float fade(final int age, final float partialTick, final int lifetime) {
        return (age + partialTick) / Math.max(1.0F, (float) lifetime);
    }

    /** {@code frame = min(15, (int)(15.0F * fade))} (line 53). */
    public static int frame(final float fade) {
        return Math.min(FRAME_COUNT - 1, (int)(15.0F * fade));
    }

    private FxWardRules() {
    }

    /** {@code 12 + rand.nextInt(5)} — expressed as the rolled extra. */
    public static int lifetime(final int rolledExtra) {
        return LIFETIME_BASE + rolledExtra;
    }

    /** {@code 1.4F + nextGaussian() * 0.3F} — expressed as the rolled gaussian. */
    public static double scale(final double gaussian) {
        return SCALE_BASE + gaussian * SCALE_GAUSSIAN;
    }

    /**
     * One jitter component: {@code clamp(fromHit - 0.6 + rand * 0.2, -0.4, 0.4)}.
     *
     * @param fromHit the sender's offset for this axis
     * @param roll    the {@code rand.nextFloat()} this component consumes
     */
    public static double jitter(final double fromHit, final double roll) {
        return Math.max(JITTER_MIN, Math.min(JITTER_MAX,
                fromHit + JITTER_FROM_HIT + roll * JITTER_SPAN));
    }

    /**
     * The face rotation: {@code glRotatef(90, side.offsetY, -side.offsetX,
     * side.offsetZ)} (line 63) then {@code glRotatef(rotation, 0, 0, 1)}
     * (line 64). OpenGL post-multiplies as it descends the matrix stack, so a
     * vertex carries {@code face * spin * (offset + vertices)} — the order
     * below.
     */
    public static Quaternionf baseRotation(final Direction side, final double rotationDegrees) {
        // joml pitfall, measured: Quaternionf.rotationAxis(angle, x, y, z) SETS
        // this quaternion to that rotation (it is a setter, not a composition),
        // so the two GL calls cannot be chained with it — the second erased the
        // first and every face rendered with the identity rotation. The axis-
        // angle setter plus an explicit mul is the composition the GL matrix
        // stack does as it descends: M = face * spin.
        final Quaternionf face = new Quaternionf().fromAxisAngleRad(
                side.getStepY(), -side.getStepX(), side.getStepZ(),
                (float) Math.toRadians(90.0));
        final Quaternionf spin = new Quaternionf().fromAxisAngleRad(
                0.0F, 0.0F, 1.0F, (float) Math.toRadians(rotationDegrees));
        return face.mul(spin);
    }

    /**
     * The quad's rotation as 1.21.1's {@code renderRotatedQuad} wants it: the
     * face spin, plus the {@code side.offsetZ > 0} branch's extra
     * {@code glRotatef(180, 0, -1, 0)} (lines 65-67) applied to the vertices.
     */
    public static Quaternionf quadRotation(final Direction side, final double rotationDegrees) {
        final Quaternionf rotation = baseRotation(side, rotationDegrees);
        if (side.getStepZ() > 0) {
            // Post-multiplied, like the GL stack: the flip is the LAST matrix
            // applied to the vertices (line 66 follows the 0.505 translate at
            // line 65), so it turns the quad, not the push.
            rotation.mul(new Quaternionf().fromAxisAngleRad(
                    0.0F, -1.0F, 0.0F, (float) Math.toRadians(180.0)));
        }
        return rotation;
    }

    /**
     * The face push as a world offset: the base rotation applied to
     * {@code (0, 0, offsetZ > 0 ? +0.505 : -0.505)} (lines 65-70). The port
     * bakes it into the spawn position — the ward mote has no gravity and no
     * motion (lines 22-23), so a translate and a position are the same number.
     */
    public static Vec3 faceOffset(final Direction side, final double rotationDegrees) {
        final double along = side.getStepZ() > 0 ? FACE_OFFSET : -FACE_OFFSET;
        // Vec3 has no Quaternionf overload; the joml vector is what the
        // rotation acts on, and the port reads it back as the world offset.
        final org.joml.Vector3f pushed = new org.joml.Vector3f(0.0F, 0.0F, (float) along);
        pushed.rotate(baseRotation(side, rotationDegrees));
        return new Vec3(pushed.x, pushed.y, pushed.z);
    }

    /**
     * The tick's alpha (FXBlockWard.java:93-98), on the pre-increment age:
     * {@code particleAge / threshold} up to the threshold,
     * {@code (particleMaxAge - particleAge) / particleMaxAge} after it. Both
     * divisions are float divisions in the original, so the curve is a
     * fade-in to 1 at the threshold and a fade-out to 0 at the end of life.
     */
    public static float alpha(final int lifetime, final int age) {
        final float threshold = lifetime / FADE_WINDOW_DIVISOR;
        if (age <= threshold) {
            return age / threshold;
        }
        return (lifetime - age) / (float) lifetime;
    }

    /** The alpha the draw uses: the tick's alpha, halved (lines 58, 76). */
    public static float drawAlpha(final int lifetime, final int age) {
        return alpha(lifetime, age) / DRAW_ALPHA_DIVISOR;
    }
}
