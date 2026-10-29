package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * The ported block arc (TC-M7-02-b-s): one instance is one reference-tree
 * {@code FXArc}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXArc.java)
 * drawn as a 1.21.1 particle.
 *
 * <p>The point chain is built in the constructor from a seed
 * ({@link TcArcGeometry} — the original builds it from the world random in
 * its constructor, FXArc.java:41-63). The render (FXArc.java:83-107) draws
 * two crossed ribbon strips of half-width {@code size = 0.25F} along the
 * chain with {@code u = c / length} and {@code v = 0..1}, alpha 0.8,
 * additive, brightness 200 — the original draws triangle strips under
 * {@code GL_REPEAT} on {@code beamh.png}; the clamped particle atlas forces
 * per-tile quads, so each ribbon segment is split at integer {@code u}
 * boundaries (the BD-16 seam) and emitted with both windings for the
 * original's disabled culling.</p>
 */
public final class TcArcParticle extends TextureSheetParticle {

    private final TcArcOptions options;
    private final java.util.List<double[]> points;
    private final float length;
    private final float red;
    private final float green;
    private final float blue;

    TcArcParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcArcOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.points = TcArcGeometry.points(options.dx(), options.dy(), options.dz(),
                options.heightGain(), RandomSource.create(options.seed()));
        this.length = (float) Math.sqrt(
                options.dx() * options.dx() + options.dy() * options.dy() + options.dz() * options.dz());
        this.red = TcFxPlans.red(options.color());
        this.green = TcFxPlans.green(options.color());
        this.blue = TcFxPlans.blue(options.color());
        this.setLifetime(TcArcGeometry.LIFETIME);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 1));
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        final Vec3 cam = camera.getPosition();
        final float ox = (float) (this.x - cam.x);
        final float oy = (float) (this.y - cam.y);
        final float oz = (float) (this.z - cam.z);
        final int light = LightTexture.FULL_BRIGHT;
        final int alpha = (int) (TcArcGeometry.ALPHA * 255.0F);
        final int r = (int) (this.red * 255.0F);
        final int g = (int) (this.green * 255.0F);
        final int b = (int) (this.blue * 255.0F);
        final float size = TcArcGeometry.RIBBON_SIZE;
        final float su0 = this.getU0();
        final float su1 = this.getU1();
        final float sv0 = this.getV0();
        final float sv1 = this.getV1();

        // Two crossed strips (FXArc.java:95-106): the vertical strip offsets y
        // by +/- size, the horizontal strip offsets x and z.
        for (int strip = 0; strip < 2; strip++) {
            for (int c = 0; c + 1 < this.points.size(); c++) {
                final double[] a = this.points.get(c);
                final double[] b2 = this.points.get(c + 1);
                final float uA = this.length == 0.0F ? 0.0F : c / this.length;
                final float uB = this.length == 0.0F ? 1.0F : (c + 1) / this.length;
                // Split the quad at integer u boundaries (GL_REPEAT replaced by
                // clamped per-tile quads, BD-16).
                final float uStepA = (float) Math.ceil(uA);
                final boolean split = uStepA > uA && uStepA < uB;
                stripQuad(buffer, light, r, g, b, alpha, size, strip, su0, su1, sv0, sv1,
                        ox, oy, oz, a, b2, uA, split ? uStepA : uB);
                if (split) {
                    stripQuad(buffer, light, r, g, b, alpha, size, strip, su0, su1, sv0, sv1,
                            ox, oy, oz, a, b2, uStepA, uB);
                }
            }
        }
    }

    private static void stripQuad(final VertexConsumer buffer, final int light,
            final int r, final int g, final int b, final int alpha, final float size,
            final int strip, final float su0, final float su1, final float sv0, final float sv1,
            final float ox, final float oy, final float oz,
            final double[] a, final double[] b2, final float uA, final float uB) {
        // Edge 1 / edge 2 of the strip, matching FXArc.java:95-100:
        // vertical strip: (dx, dy - size, dz, u, 1) and (dx, dy + size, dz, u, 0);
        // horizontal strip: (dx - size, dy, dz - size, u, 1) and
        // (dx + size, dy, dz + size, u, 0).
        final float ax1 = strip == 0 ? ox + (float) a[0] : ox + (float) a[0] - size;
        final float ay1 = strip == 0 ? oy + (float) a[1] - size : oy + (float) a[1];
        final float az1 = strip == 0 ? oz + (float) a[2] : oz + (float) a[2] - size;
        final float ax2 = strip == 0 ? ox + (float) a[0] : ox + (float) a[0] + size;
        final float ay2 = strip == 0 ? oy + (float) a[1] + size : oy + (float) a[1];
        final float az2 = strip == 0 ? oz + (float) a[2] : oz + (float) a[2] + size;
        final float bx1 = strip == 0 ? ox + (float) b2[0] : ox + (float) b2[0] - size;
        final float by1 = strip == 0 ? oy + (float) b2[1] - size : oy + (float) b2[1];
        final float bz1 = strip == 0 ? oz + (float) b2[2] : oz + (float) b2[2] - size;
        final float bx2 = strip == 0 ? ox + (float) b2[0] : ox + (float) b2[0] + size;
        final float by2 = strip == 0 ? oy + (float) b2[1] + size : oy + (float) b2[1];
        final float bz2 = strip == 0 ? oz + (float) b2[2] : oz + (float) b2[2] + size;
        // (a edge1) (b edge1) (b edge2) (a edge2), then the reverse winding
        // (the original's glDisable(GL_CULL_FACE), FXArc.java:91).
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uA, 1.0F, ax1, ay1, az1);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uB, 1.0F, bx1, by1, bz1);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uB, 0.0F, bx2, by2, bz2);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uA, 0.0F, ax2, ay2, az2);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uA, 0.0F, ax2, ay2, az2);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uB, 0.0F, bx2, by2, bz2);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uB, 1.0F, bx1, by1, bz1);
        vertex(buffer, light, r, g, b, alpha, su0, su1, sv0, sv1, uA, 1.0F, ax1, ay1, az1);
    }

    private static void vertex(final VertexConsumer buffer, final int light,
            final int r, final int g, final int b, final int alpha,
            final float su0, final float su1, final float sv0, final float sv1,
            final float u, final float v,
            final float x, final float y, final float z) {
        buffer.addVertex(x, y, z)
                .setColor(r, g, b, alpha)
                .setUv(su0 + (su1 - su0) * u, sv0 + (sv1 - sv0) * v)
                .setLight(light);
    }

    @Override
    protected int getLightColor(final float partialTick) {
        return LightTexture.FULL_BRIGHT;   // tessellator setBrightness(200)
    }

    @Override
    public ParticleRenderType getRenderType() {
        // glBlendFunc(770, 1) (FXArc.java:89) — additive.
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} (arc.png full-texture copy). */
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcArcOptions> {

        @Override
        public Particle createParticle(final TcArcOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcArcParticle(level, x, y, z, options, this.sprites);
        }
    }
}
