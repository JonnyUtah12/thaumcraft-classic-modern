package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Shared tile emitter for the ported beam ribbons (TC-M7-02-b-s).
 *
 * <p>Vertex order follows {@code FXBeam.renderLayerEffect}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXBeam.java:236-239):
 * far-left (u=1), near-left (u=1), near-right (u=0), far-right (u=0). The
 * original renders with face culling disabled (FXBeam.java:199); the 1.21.1
 * particle sheet culls, so every quad is emitted twice with reversed winding
 * (docs/BEHAVIORAL_DEVIATIONS.md BD-16).</p>
 */
final class TcBeamRibbon {

    private TcBeamRibbon() {
    }

    static void emit(final VertexConsumer buffer, final PoseStack.Pose pose,
            final TcBeamGeometry.RibbonTile tile, final float red, final float green, final float blue,
            final float u0, final float u1, final float v0, final float v1,
            final int alpha, final int light) {
        final int r = (int) (red * 255.0F);
        final int g = (int) (green * 255.0F);
        final int b = (int) (blue * 255.0F);
        final float vNear = vOf(v0, v1, tile.vStart());
        final float vFar = vOf(v0, v1, tile.vEnd());
        // Front winding (FXBeam.java:236-239).
        buffer.addVertex(pose, -tile.halfEnd(), tile.yEnd(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u1, vFar).setLight(light);
        buffer.addVertex(pose, -tile.halfStart(), tile.yStart(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u1, vNear).setLight(light);
        buffer.addVertex(pose, tile.halfStart(), tile.yStart(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u0, vNear).setLight(light);
        buffer.addVertex(pose, tile.halfEnd(), tile.yEnd(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u0, vFar).setLight(light);
        // Back winding (the original's disabled culling).
        buffer.addVertex(pose, tile.halfEnd(), tile.yEnd(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u0, vFar).setLight(light);
        buffer.addVertex(pose, tile.halfStart(), tile.yStart(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u0, vNear).setLight(light);
        buffer.addVertex(pose, -tile.halfStart(), tile.yStart(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u1, vNear).setLight(light);
        buffer.addVertex(pose, -tile.halfEnd(), tile.yEnd(), 0.0F).setColor(r, g, b, alpha)
                .setUv(u1, vFar).setLight(light);
    }

    private static float vOf(final float v0, final float v1, final float v) {
        return v0 + (v1 - v0) * v;
    }
}
