package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The ported fractal lightning bolt (TC-M7-02-b-s): one instance is one
 * render pass of a reference-tree {@code FXLightningBolt}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/bolt/FXLightningBolt.java)
 * drawn as a 1.21.1 particle.
 *
 * <p>The fractal itself is the seed-reproducible {@link TcBoltGeometry}
 * (port of {@code FXLightningBoltCommon}); this class is the render pass of
 * {@code renderBolt} (FXLightningBolt.java:106-170): the progressive reveal
 * length, the per-segment camera-facing width, the round caps on the first
 * and last segment, and the two-pass colours (pass 0 = {@code p_large.png}
 * body glow, pass 1 = {@code p_small.png} core; the sender spawns one
 * particle per pass with the original's pass colours). The per-segment
 * ribbon quads are emitted with both windings to replace the original's
 * disabled face culling — the BD-16 seam — and the camera basis comes from
 * the 1.21.1 camera look vector (TcBoltGeometry.playerVec).</p>
 */
public final class TcBoltParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final TcBoltOptions options;
    private final TcBoltGeometry geometry;
    private final float red;
    private final float green;
    private final float blue;
    private final double startX;
    private final double startY;
    private final double startZ;

    TcBoltParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcBoltOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.sprites = sprites;
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        // ClientProxy.nodeBolt seeds the geometry from world.rand.nextLong()
        // and passes duration 10, multiplier 4.0, speed 5 (ClientProxy.java:789-796).
        this.geometry = new TcBoltGeometry(new TcFxVector3(x, y, z),
                new TcFxVector3(options.endX(), options.endY(), options.endZ()),
                options.seed(),
                TcFxPlans.BOLT_DURATION, TcFxPlans.BOLT_MULTIPLIER, TcFxPlans.BOLT_INCREMENT);
        this.geometry.defaultFractal();
        this.geometry.finalizeBolt();
        this.red = TcFxPlans.red(options.color());
        this.green = TcFxPlans.green(options.color());
        this.blue = TcFxPlans.blue(options.color());
        // FXLightningBolt.onUpdate: dies when the geometry's age reaches its
        // maxAge; the geometry advances `increment` per tick (FXLightningBolt.java:101-105).
        this.setLifetime((this.geometry.particleMaxAge + this.geometry.increment - 1)
                / this.geometry.increment);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(options.pass(), 2));
    }

    @Override
    public void tick() {
        super.tick();
        // FXLightningBolt.onUpdate: main.onUpdate() advances the fractal age.
        this.geometry.onUpdate();
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        // The per-frame distance gate (FXLightningBolt.java:155-161).
        final Minecraft minecraft = Minecraft.getInstance();
        final Entity viewEntity = minecraft.getCameraEntity();
        if (viewEntity == null) {
            return;
        }
        final double viewDistance = Math.sqrt(Math.pow(viewEntity.getX() - this.startX, 2)
                + Math.pow(viewEntity.getY() - this.startY, 2)
                + Math.pow(viewEntity.getZ() - this.startZ, 2));
        if (!TcBeamGeometry.boltVisible(viewDistance, fancy())) {
            return;
        }

        final int renderLength = this.geometry.renderLength(partialTick);
        final float alpha = TcBoltGeometry.passAlpha(this.options.pass(),
                this.geometry.particleAge, this.geometry.particleMaxAge);

        // The camera basis expansion of the original's RenderGlobal arguments
        // (TcBoltGeometry.playerVec, BD-16).
        final Vector3f look = camera.getLookVector();
        final float cosPitch = TcBoltGeometry.lookCosPitch(look.x, look.y, look.z);
        final TcFxVector3 playervec = TcBoltGeometry.playerVec(
                TcBoltGeometry.lookSinYaw(look.x, look.z, cosPitch),
                TcBoltGeometry.lookCosYaw(look.z, cosPitch),
                cosPitch);

        final Vec3 cam = camera.getPosition();
        final int light = LightTexture.FULL_BRIGHT;
        // The bolt texture's vertical strip: u = 0.5 constant, v spans the
        // sprite (FXLightningBolt.java:134-137 uses raw GL uv (0.5, 0)..(0.5, 1)).
        final float u0 = this.getU0();
        final float uMid = u0 + (this.getU1() - u0) * 0.5F;
        final float v0 = this.getV0();
        final float v1 = this.getV1();

        for (final TcBoltGeometry.Segment segment : this.geometry.segments) {
            if (segment.segmentno > renderLength) {
                continue;
            }
            final TcFxVector3 start = segment.startpoint.point;
            final TcFxVector3 end = segment.endpoint.point;
            final float width = TcBoltGeometry.segmentWidth(TcFxPlans.BOLT_WIDTH,
                    distanceToPlayer(start.x, start.y, start.z), segment.light);
            final TcFxVector3 diff1 = TcFxVector3.crossProduct(playervec, segment.prevdiff)
                    .scale(width / segment.sinprev);
            final TcFxVector3 diff2 = TcFxVector3.crossProduct(playervec, segment.nextdiff)
                    .scale(width / segment.sinnext);
            final int segAlpha = (int) (Math.max(0.0F, Math.min(1.0F, alpha * segment.light)) * 255.0F);

            // Segment quad (FXLightningBolt.java:134-137): u = 0.5 line, v 0..1.
            quad(buffer, cam, light, segAlpha, v0, v1, uMid, uMid, uMid, uMid, 0.0F, 0.0F, 1.0F, 1.0F,
                    end.x - diff2.x, end.y - diff2.y, end.z - diff2.z,
                    start.x - diff1.x, start.y - diff1.y, start.z - diff1.z,
                    start.x + diff1.x, start.y + diff1.y, start.z + diff1.z,
                    end.x + diff2.x, end.y + diff2.y, end.z + diff2.z);

            if (segment.next == null) {
                // Round cap past the last segment (FXLightningBolt.java:139-147): u 0..0.5.
                final TcFxVector3 roundEnd = end.copy()
                        .add(segment.diff.copy().normalize().scale(width));
                quad(buffer, cam, light, segAlpha, v0, v1,
                        u0, uMid, uMid, u0, 0.0F, 0.0F, 1.0F, 1.0F,
                        roundEnd.x - diff2.x, roundEnd.y - diff2.y, roundEnd.z - diff2.z,
                        end.x - diff2.x, end.y - diff2.y, end.z - diff2.z,
                        end.x + diff2.x, end.y + diff2.y, end.z + diff2.z,
                        roundEnd.x + diff2.x, roundEnd.y + diff2.y, roundEnd.z + diff2.z);
            }
            if (segment.prev == null) {
                // Round cap behind the first segment (FXLightningBolt.java:149-156).
                final TcFxVector3 roundEnd = start.copy()
                        .sub(segment.diff.copy().normalize().scale(width));
                quad(buffer, cam, light, segAlpha, v0, v1,
                        uMid, u0, u0, uMid, 0.0F, 0.0F, 1.0F, 1.0F,
                        start.x - diff1.x, start.y - diff1.y, start.z - diff1.z,
                        roundEnd.x - diff1.x, roundEnd.y - diff1.y, roundEnd.z - diff1.z,
                        roundEnd.x + diff1.x, roundEnd.y + diff1.y, roundEnd.z + diff1.z,
                        start.x + diff1.x, start.y + diff1.y, start.z + diff1.z);
            }
        }
    }

    /** Emits one bolt quad (both windings) from four corners with per-corner uv. */
    private void quad(final VertexConsumer buffer, final Vec3 cam, final int light, final int alpha,
            final float v0, final float v1,
            final float ua, final float ub, final float uc, final float ud,
            final float va, final float vb, final float vc, final float vd,
            final float ax, final float ay, final float az,
            final float bx, final float by, final float bz,
            final float cx, final float cy, final float cz,
            final float dx, final float dy, final float dz) {
        final int r = (int) (this.red * 255.0F);
        final int g = (int) (this.green * 255.0F);
        final int b = (int) (this.blue * 255.0F);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, ua, va, ax, ay, az);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, ub, vb, bx, by, bz);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, uc, vc, cx, cy, cz);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, ud, vd, dx, dy, dz);
        // Both windings (the original draws under glDisable(GL_CULL_FACE)).
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, ud, vd, dx, dy, dz);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, uc, vc, cx, cy, cz);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, ub, vb, bx, by, bz);
        vertex(buffer, cam, light, r, g, b, alpha, this.getU0(), this.getU1(), v0, v1, ua, va, ax, ay, az);
    }

    private static void vertex(final VertexConsumer buffer, final Vec3 cam, final int light,
            final int r, final int g, final int b, final int alpha,
            final float su0, final float su1, final float sv0, final float sv1,
            final float u, final float v,
            final float x, final float y, final float z) {
        buffer.addVertex((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z))
                .setColor(r, g, b, alpha)
                .setUv(su0 + (su1 - su0) * u, sv0 + (sv1 - sv0) * v)
                .setLight(light);
    }

    private float distanceToPlayer(final float x, final float y, final float z) {
        // getRelativeViewVector (FXLightningBolt.java:90-93): the client
        // player's distance to the point, per vertex.
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0.0F;
        }
        final double dx = minecraft.player.getX() - x;
        final double dy = minecraft.player.getY() - y;
        final double dz = minecraft.player.getZ() - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static boolean fancy() {
        return Minecraft.getInstance().options.graphicsMode().get()
                != net.minecraft.client.GraphicsStatus.FAST;
    }

    @Override
    protected int getLightColor(final float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // FXLightningBolt renders the additive sheet (the original's 770/770
        // blend is closest to SRC_ALPHA, ONE — BD-16).
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /** Provider wired from {@link TcParticleProviders} (frames = sliced bolt_small/bolt_large). */
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcBoltOptions> {

        @Override
        public Particle createParticle(final TcBoltOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcBoltParticle(level, x, y, z, options, this.sprites);
        }
    }
}
