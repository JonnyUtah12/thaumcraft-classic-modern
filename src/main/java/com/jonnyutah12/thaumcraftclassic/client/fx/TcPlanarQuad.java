package com.jonnyutah12.thaumcraftclassic.client.fx;

/**
 * A flat textured quad in WORLD axes, built from the rotation chain a 1.7.10
 * {@code EntityFX.render} layer applies.
 *
 * <p><b>Why this exists.</b> The rune decal is not a billboard. Its
 * {@code render} runs {@code glRotatef(rotation, 0, 1, 0)}, then
 * {@code glRotatef(90, 0, 0, 1)}, then {@code glTranslated(ofx, ofy, -0.51)}
 * and emits four vertices at ±0.5 of a rotated local square
 * (FXBlockRunes.java:48-50, 60-63). The chain is camera-independent — one of
 * four axis-aligned wall orientations — so the port reproduces the geometry
 * exactly and the BD-16 camera-basis adaptation does not apply to it. 1.21.1
 * builds the model-view matrix itself, so the same orientation is computed here
 * from the same angles and handed to {@code VertexConsumer} as plain
 * world-space vertices.</p>
 *
 * <p>The class is deliberately shared by the flat FX family: the warding-stone
 * shield (the original's {@code FXShieldRunes} / {@code FXBlockWard}, which
 * rotate about the camera and ARE subject to BD-16) lands on the same quad
 * shape with a camera-derived chain.</p>
 */
public final class TcPlanarQuad {

    /** The 90° Rz the rune chain applies after its yaw (FXBlockRunes.java:49). */
    public static final int RUNE_ROLL_DEGREES = 90;

    /** The fixed in-plane depth of the rune chain's translate (line 50). */
    public static final float RUNE_DEPTH = -0.51F;

    /**
     * The rune's quad for one particle: {@code yaw} is the ctor's
     * {@code rotation} (a multiple of 90°), {@code ofx}/{@code ofy} the in-plane
     * jitter, and {@code scale} the ctor's {@code particleScale}. The edge the
     * original uses is {@code var12 = 0.3F * particleScale}
     * (FXBlockRunes.java:52), so the half-extent is {@code 0.15 * scale}.
     */
    public static TcPlanarQuad runeQuad(final int yaw, final float ofx, final float ofy,
            final float scale) {
        return rotated(yaw, RUNE_ROLL_DEGREES, ofx, ofy, RUNE_DEPTH, 0.5F * 0.3F * scale);
    }

    /**
     * A quad whose local frame is {@code Ry(yaw) * Rz(roll)} and whose centre is
     * that same rotation applied to the translate vector — the exact order of
     * the GL matrix stack, which multiplies the later call on the right
     * (FXBlockRunes.java:48-50).
     */
    public static TcPlanarQuad rotated(final int yaw, final int roll, final double ofx,
            final double ofy, final double ofz, final double half) {
        // Columns of R = Ry(yaw) * Rz(roll), applied to the local basis vectors.
        final double[] e1 = rotY(yaw, rotZ(roll, 1.0, 0.0, 0.0));
        final double[] e2 = rotY(yaw, rotZ(roll, 0.0, 1.0, 0.0));
        final double[] e3 = rotY(yaw, rotZ(roll, 0.0, 0.0, 1.0));
        // glTranslated(ofx, ofy, ofz) runs through the same matrix (line 46).
        return new TcPlanarQuad(ofx * e1[0] + ofy * e2[0] + ofz * e3[0],
                ofx * e1[1] + ofy * e2[1] + ofz * e3[1],
                ofx * e1[2] + ofy * e2[2] + ofz * e3[2],
                e1, e2, e3, half);
    }

    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double ux;
    private final double uy;
    private final double uz;
    private final double vx;
    private final double vy;
    private final double vz;
    private final double nx;
    private final double ny;
    private final double nz;
    private final double half;

    private TcPlanarQuad(final double ox, final double oy, final double oz, final double[] u,
            final double[] v, final double[] normal, final double half) {
        this.offsetX = ox;
        this.offsetY = oy;
        this.offsetZ = oz;
        this.ux = u[0];
        this.uy = u[1];
        this.uz = u[2];
        this.vx = v[0];
        this.vy = v[1];
        this.vz = v[2];
        this.nx = normal[0];
        this.ny = normal[1];
        this.nz = normal[2];
        this.half = half;
    }

    /** The quad centre relative to the particle position, after the rotation. */
    public double[] offset() {
        return new double[] {offsetX, offsetY, offsetZ};
    }

    /** The sprite's horizontal axis in world space (the chain's first column). */
    public double[] u() {
        return new double[] {ux, uy, uz};
    }

    /** The sprite's vertical axis in world space (the chain's second column). */
    public double[] v() {
        return new double[] {vx, vy, vz};
    }

    /** The face normal, {@code u x v}. */
    public double[] normal() {
        return new double[] {nx, ny, nz};
    }

    /** Half the quad's edge length in world units. */
    public double half() {
        return half;
    }

    /**
     * A corner in world space. {@code s}/{@code t} are in units of the full
     * edge, matching the original's {@code ±0.5*var12} vertex arguments
     * (FXBlockRunes.java:55, 56-59) — so a unit step of the basis is scaled to
     * the quad's edge ({@code 2 * half}), not left at 1.0.
     */
    public double[] vertex(final double x, final double y, final double z, final double s,
            final double t) {
        final double edge = 2.0 * this.half;
        return new double[] {x + offsetX + s * edge * ux + t * edge * vx,
                y + offsetY + s * edge * uy + t * edge * vy,
                z + offsetZ + s * edge * uz + t * edge * vz};
    }

    /** {@code glRotatef(degrees, 0, 1, 0)} applied to a vector. */
    private static double[] rotY(final double degrees, final double[] v) {
        return rotY(degrees, v[0], v[1], v[2]);
    }

    private static double[] rotY(final double degrees, final double x, final double y, final double z) {
        final double a = Math.toRadians(degrees);
        final double c = Math.cos(a);
        final double s = Math.sin(a);
        return new double[] {c * x + s * z, y, -s * x + c * z};
    }

    /** {@code glRotatef(degrees, 0, 0, 1)} applied to a vector. */
    private static double[] rotZ(final double degrees, final double x, final double y, final double z) {
        final double a = Math.toRadians(degrees);
        final double c = Math.cos(a);
        final double s = Math.sin(a);
        return new double[] {c * x - s * y, s * x + c * y, z};
    }
}
