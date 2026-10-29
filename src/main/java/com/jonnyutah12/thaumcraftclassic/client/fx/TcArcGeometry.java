package com.jonnyutah12.thaumcraftclassic.client.fx;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * The arc trajectory of the reference tree's {@code FXArc}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXArc.java:41-63)
 * and the parabola solver of {@code Utils.calculateVelocity}
 * (common/lib/utils/Utils.java:316-338) — pure code (TC-M7-02-b-s).
 *
 * <p>The original built its point chain in the entity constructor from the
 * world random, so the arc is reproducible from a seed. The port uses the
 * plan's {@link RandomSource} (the parent slice's seam), jittering each axis
 * with the original's two-draw pattern {@code (nextDouble - nextDouble) * 0.25}
 * in the original's order: x, y, z per point.</p>
 */
public final class TcArcGeometry {

    /** The original's arc gravity (FXArc.java:44). */
    public static final double GRAVITY = 0.115;
    /** The original's arc jitter amplitude (FXArc.java:45). */
    public static final double NOISE = 0.25;
    /** The original's point cap: {@code c < 50} (FXArc.java:54), plus start and end = 52. */
    public static final int MAX_MID_POINTS = 50;
    /** The ribbon half width of the arc render: {@code size = 0.25F} (FXArc.java:88). */
    public static final float RIBBON_SIZE = 0.25F;
    /** The arc render's vertex alpha: 0.8 (FXArc.java:92). */
    public static final float ALPHA = 0.8F;
    /** The original's arc lifetime: {@code particleMaxAge = 1} (FXArc.java:39). */
    public static final int LIFETIME = 1;

    private TcArcGeometry() {
    }

    /**
     * {@code Utils.calculateVelocity(from, to, heightGain, gravity)} — the
     * launch velocity that carries a ballistic point chain from an apex of
     * {@code heightGain} over the 2D distance.
     */
    public static double[] calculateVelocity(final double fx, final double fy, final double fz,
            final double tx, final double ty, final double tz,
            final double heightGain, final double gravity) {
        final double endGain = ty - fy;
        final double dx = tx - fx;
        final double dz = tz - fz;
        final double horizDist = Math.sqrt(dx * dx + dz * dz);
        final double maxGain = Math.max(heightGain, endGain + heightGain);
        final double a = -horizDist * horizDist / (4.0 * maxGain);
        final double c = -endGain;
        final double slope = -horizDist / (2.0 * a) - Math.sqrt(horizDist * horizDist - 4.0 * a * c) / (2.0 * a);
        final double vy = Math.sqrt(maxGain * gravity);
        final double vh = vy / slope;
        final double mag = Math.sqrt(dx * dx + dz * dz);
        final double dirx = dx / mag;
        final double dirz = dz / mag;
        return new double[] {vh * dirx, vy, vh * dirz};
    }

    /**
     * The point chain of {@code FXArc}'s constructor (FXArc.java:41-63):
     * start at (0,0,0), walk by the launch velocity, jitter each axis with
     * {@code (rand - rand) * NOISE}, decay the vertical velocity by
     * {@code GRAVITY / 1.9} per step, stop within
     * {@code |velocity|²} of the target (the endpoint is added as the final
     * point). Coordinates are relative to the arc's spawn position.
     *
     * @return the point list, {@code [ {x,y,z}, ... ]} — first point (0,0,0),
     *         last point exactly (tX, tY, tZ)
     */
    public static List<double[]> points(final double tX, final double tY, final double tZ,
            final double heightGain, final RandomSource rand) {
        final List<double[]> points = new ArrayList<>();
        final double[] vv = calculateVelocity(0.0, 0.0, 0.0, tX, tY, tZ, heightGain, GRAVITY);
        final double limit = vv[0] * vv[0] + vv[1] * vv[1] + vv[2] * vv[2];
        points.add(new double[] {0.0, 0.0, 0.0});
        double cx = 0.0;
        double cy = 0.0;
        double cz = 0.0;
        for (int c = 0; distSq(tX - cx, tY - cy, tZ - cz) > limit && c < MAX_MID_POINTS; c++) {
            cx += vv[0];
            cy += vv[1];
            cz += vv[2];
            final double jx = cx + (rand.nextDouble() - rand.nextDouble()) * NOISE;
            final double jy = cy + (rand.nextDouble() - rand.nextDouble()) * NOISE;
            final double jz = cz + (rand.nextDouble() - rand.nextDouble()) * NOISE;
            cx = jx;
            cy = jy;
            cz = jz;
            points.add(new double[] {jx, jy, jz});
            vv[1] -= GRAVITY / 1.9;
        }
        points.add(new double[] {tX, tY, tZ});
        return points;
    }

    private static double distSq(final double dx, final double dy, final double dz) {
        return dx * dx + dy * dy + dz * dz;
    }
}
