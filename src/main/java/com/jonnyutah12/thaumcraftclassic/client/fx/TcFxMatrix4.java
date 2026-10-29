package com.jonnyutah12.thaumcraftclassic.client.fx;

/**
 * Port of the reference tree's {@code WRMat4}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/WRMat4.java), the 4×4
 * matrix {@link TcFxVector3#rotate(float, TcFxVector3)} applies (TC-M7-02-b-s).
 *
 * <p>The column-major float[16] layout, the uninitialised {@code mat[3/7/11/12]}
 * slots and the {@code translate} arithmetic are kept cell-for-cell: the bolt
 * fractal's rotation results are bit-reproducible against the original, which
 * the seed-parity tests pin.</p>
 */
public final class TcFxMatrix4 {

    float[] mat;

    public TcFxMatrix4() {
        this.loadIdentity();
    }

    public TcFxMatrix4 loadIdentity() {
        this.mat = new float[16];
        this.mat[0] = this.mat[5] = this.mat[10] = this.mat[15] = 1.0F;
        return this;
    }

    public TcFxVector3 translate(final TcFxVector3 vec) {
        final float x = vec.x * this.mat[0] + vec.y * this.mat[1] + vec.z * this.mat[2] + this.mat[3];
        final float y = vec.x * this.mat[4] + vec.y * this.mat[5] + vec.z * this.mat[6] + this.mat[7];
        final float z = vec.x * this.mat[8] + vec.y * this.mat[9] + vec.z * this.mat[10] + this.mat[11];
        vec.x = x;
        vec.y = y;
        vec.z = z;
        return vec;
    }

    public static TcFxMatrix4 rotationMat(final double angle, final TcFxVector3 axis) {
        final TcFxVector3 norm = axis.copy().normalize();
        final float x = norm.x;
        final float y = norm.y;
        final float z = norm.z;
        double angleRad = angle * 0.0174532925;
        final float cos = (float) Math.cos(angleRad);
        final float ocos = 1.0F - cos;
        final float sin = (float) Math.sin(angleRad);
        final TcFxMatrix4 rotmat = new TcFxMatrix4();
        rotmat.mat[0] = x * x * ocos + cos;
        rotmat.mat[1] = y * x * ocos + z * sin;
        rotmat.mat[2] = x * z * ocos - y * sin;
        rotmat.mat[4] = x * y * ocos - z * sin;
        rotmat.mat[5] = y * y * ocos + cos;
        rotmat.mat[6] = y * z * ocos + x * sin;
        rotmat.mat[8] = x * z * ocos + y * sin;
        rotmat.mat[9] = y * z * ocos - x * sin;
        rotmat.mat[10] = z * z * ocos + cos;
        rotmat.mat[15] = 1.0F;
        return rotmat;
    }
}
