package com.jonnyutah12.thaumcraftclassic.client.fx;

/**
 * Port of the reference tree's {@code WRVector3}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/WRVector3.java), the
 * vector math the bolt/fractal geometry runs on (TC-M7-02-b-s).
 *
 * <p>The port keeps the original's float components and its mutation-style
 * API (every operation returns {@code this}), because
 * {@link TcBoltGeometry}'s fractal walk assigns these vectors into segments
 * and reads them back in the original's exact order — the geometry is seed
 * reproducible, and a value-semantics rewrite would change the numbers.</p>
 *
 * <p>The two Minecraft-typed constructors of the original (from an Entity /
 * TileEntity position) are dropped: the vector layer stays free of world
 * types so it is unit-testable, and the callers in {@link TcFxPlans} pass
 * plain coordinates.</p>
 */
public final class TcFxVector3 {

    public float x;
    public float y;
    public float z;

    public TcFxVector3(final double x, final double y, final double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
    }

    public TcFxVector3 add(final TcFxVector3 vec) {
        this.x = this.x + vec.x;
        this.y = this.y + vec.y;
        this.z = this.z + vec.z;
        return this;
    }

    public TcFxVector3 sub(final TcFxVector3 vec) {
        this.x = this.x - vec.x;
        this.y = this.y - vec.y;
        this.z = this.z - vec.z;
        return this;
    }

    public TcFxVector3 scale(final float scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        return this;
    }

    public TcFxVector3 normalize() {
        final float length = this.length();
        this.x /= length;
        this.y /= length;
        this.z /= length;
        return this;
    }

    public float length() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public float lengthPow2() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public TcFxVector3 copy() {
        return new TcFxVector3(this.x, this.y, this.z);
    }

    public static TcFxVector3 crossProduct(final TcFxVector3 vec1, final TcFxVector3 vec2) {
        return new TcFxVector3(vec1.y * vec2.z - vec1.z * vec2.y,
                vec1.z * vec2.x - vec1.x * vec2.z,
                vec1.x * vec2.y - vec1.y * vec2.x);
    }

    public static TcFxVector3 xCrossProduct(final TcFxVector3 vec) {
        return new TcFxVector3(0.0, vec.z, -vec.y);
    }

    public static TcFxVector3 zCrossProduct(final TcFxVector3 vec) {
        return new TcFxVector3(-vec.y, vec.x, 0.0);
    }

    public static float dotProduct(final TcFxVector3 vec1, final TcFxVector3 vec2) {
        return vec1.x * vec2.x + vec1.y * vec2.y + vec1.z * vec2.z;
    }

    /** The original's acos of the (pre-normalised) dot product — see {@code anglePreNorm}. */
    public static float anglePreNorm(final TcFxVector3 vec1, final TcFxVector3 vec2) {
        return (float) Math.acos(dotProduct(vec1, vec2));
    }

    public TcFxVector3 rotate(final float angle, final TcFxVector3 axis) {
        return TcFxMatrix4.rotationMat(angle, axis).translate(this);
    }

    public static TcFxVector3 getPerpendicular(final TcFxVector3 vec) {
        return vec.z == 0.0F ? zCrossProduct(vec) : xCrossProduct(vec);
    }

    public boolean isZero() {
        return this.x == 0.0F && this.y == 0.0F && this.z == 0.0F;
    }

    @Override
    public String toString() {
        return "[" + this.x + "," + this.y + "," + this.z + "]";
    }
}
