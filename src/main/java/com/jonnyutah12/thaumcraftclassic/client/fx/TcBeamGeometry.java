package com.jonnyutah12.thaumcraftclassic.client.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * The axis math of the world-rendered beams (TC-M7-02-b-s), a pure port of the
 * geometry the reference tree's {@code FXBeam} / {@code FXBeamGolemBoss}
 * compute in their constructors and render passes
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/beams/FXBeam.java,
 * FXBeamGolemBoss.java).
 *
 * <p>The 1.7.10 originals drew these effects with immediate-mode GL: a fixed
 * transform chain ({@code glRotatef(90,1,0,0)}, a yaw rotation about the -Z
 * axis, a pitch rotation, a world-time spin about Y, then three ribbon passes
 * 60° apart) and a {@code GL_REPEAT}-wrapped beam texture. The port reproduces
 * the same chain through 1.21.1's pose stack; the seam choice and its two
 * documented approximations (the camera basis handed to the original by vanilla
 * {@code RenderGlobal}, and the clamped particle atlas forcing per-tile
 * geometry instead of texture repeat) are recorded as BD-16 in
 * docs/BEHAVIORAL_DEVIATIONS.md.</p>
 */
public final class TcBeamGeometry {

    /** Beam distance gate, fancy graphics: {@code visibleDistance = 50} (FXBeam.java:66-73). */
    public static final int VISIBLE_RANGE_FANCY = 50;
    /** Beam distance gate, fast graphics: halved (FXBeam.java:66-73). */
    public static final int VISIBLE_RANGE_FAST = 25;
    /** Bolt distance gate, fancy graphics: {@code visibleDistance = 100} (FXLightningBolt.java:155-159). */
    public static final int BOLT_RANGE_FANCY = 100;
    /** Bolt distance gate, fast graphics: {@code visibleDistance = 50} (FXLightningBolt.java:155-159). */
    public static final int BOLT_RANGE_FAST = 50;

    /** Ribbon half-width at the near end: {@code 0.15 * size} (FXBeam.java:222-223). */
    public static final float RIBBON_HALF_WIDTH = 0.15F;
    /** Base opacity while pulsing: {@code op = 0.4F} (FXBeam.java:161-165). */
    public static final float OPACITY = 0.4F;
    /** The pulse ramp: size grows as {@code min(age / 4F, N)} (FXBeam.java:157). */
    public static final float PULSE_RAMP_TICKS = 4.0F;
    /** The end-fade window and slope: {@code op = 0.4F - (4 - remaining) * 0.1F} (FXBeam.java:162-164). */
    public static final int FADE_WINDOW_TICKS = 4;
    public static final float FADE_SLOPE = 0.1F;
    /** Scroll speed constants: {@code var12 = -var11 * 0.2F - floor(-var11 * 0.1F)} (FXBeam.java:203-207). */
    public static final float SCROLL_SPEED = 0.2F;
    public static final float SCROLL_STEP = 0.1F;

    private TcBeamGeometry() {
    }

    /** The beam axis of the original constructors (FXBeam.java:44-53). */
    public record Axis(float length, float rotYaw, float rotPitch) {
    }

    /**
     * The axis math of {@code FXBeam}'s constructors, line for line:
     * {@code xd = x - tX}, {@code rotYaw = atan2(xd, zd)},
     * {@code rotPitch = atan2(yd, sqrt(xd² + zd²))} (degrees).
     */
    public static Axis axis(final double x, final double y, final double z,
            final double tx, final double ty, final double tz) {
        final float xd = (float) (x - tx);
        final float yd = (float) (y - ty);
        final float zd = (float) (z - tz);
        final float length = (float) Math.sqrt(xd * xd + yd * yd + zd * zd);
        final double horizon = (float) Math.sqrt(xd * xd + zd * zd);
        final float rotYaw = (float) (Math.atan2(xd, zd) * 180.0 / Math.PI);
        final float rotPitch = (float) (Math.atan2(yd, horizon) * 180.0 / Math.PI);
        return new Axis(length, rotYaw, rotPitch);
    }

    /**
     * The pulse size ramp (FXBeam.java:155-160):
     * {@code size = min(age / 4F, N)} interpolated from the previous tick's size.
     */
    public static float size(final boolean pulse, final int age, final float n,
            final float prevSize, final float partialTick) {
        if (!pulse) {
            return n;
        }
        final float target = Math.min(age / PULSE_RAMP_TICKS, n);
        return (float) (prevSize + (target - prevSize) * partialTick);
    }

    /** The end-fade opacity (FXBeam.java:161-165). */
    public static float opacity(final boolean pulse, final int maxAge, final int age) {
        float op = OPACITY;
        if (pulse && maxAge - age <= FADE_WINDOW_TICKS) {
            op = OPACITY - (FADE_WINDOW_TICKS - (maxAge - age)) * FADE_SLOPE;
        }
        return op;
    }

    /**
     * The texture scroll offset (FXBeam.java:203-207): the clock is the
     * client player's tick counter ({@code slide + f}, negated when the beam
     * is reversed) and {@code var12 = -var11 * 0.2F - floor(-var11 * 0.1F)}.
     */
    public static float scroll(final float slidePlusPartial, final boolean reverse) {
        final float var11 = reverse ? -slidePlusPartial : slidePlusPartial;
        return -var11 * SCROLL_SPEED - (float) Math.floor(-var11 * SCROLL_STEP);
    }

    /**
     * The world-time spin of the ribbon assembly (FXBeam.java:156):
     * {@code worldTime % (360 / rotSpeed) * rotSpeed + rotSpeed * f}.
     */
    public static float spin(final long worldTime, final int rotationSpeed, final float partialTick) {
        return (float) (worldTime % (360 / rotationSpeed) * rotationSpeed) + rotationSpeed * partialTick;
    }

    /**
     * One texture tile of a beam ribbon: the span of world length it covers
     * (local y, the ribbon's axis) and the v range inside the beam texture.
     * The near-end half width is {@code 0.15*size}, the far-end
     * {@code 0.15*size*endMod}; a tile interpolates both linearly along y.
     */
    public record RibbonTile(float yStart, float yEnd, float halfStart, float halfEnd,
            float vStart, float vEnd) {
    }

    /**
     * The three-pass ribbon of {@code FXBeam.renderLayerEffect} (FXBeam.java:225-243)
     * cut into per-texture-tile quads. The original drew ONE quad from
     * {@code y = 0 .. length*size/N} with {@code v} spanning the same numbers
     * under {@code GL_REPEAT} — one texture tile per world unit of ribbon. The
     * particle atlas is clamped, so the port emits one quad per tile boundary
     * and maps v 0..1 inside each tile; the geometry and texture phase match
     * the original tile for tile (BD-16).
     *
     * @param length  the beam length
     * @param size    the current (pulsed) beam size
     * @param n       the sender's {@code field_70130_N} (the size divisor)
     * @param endMod  the far-end taper
     * @param vStart  the scroll phase at y=0 ({@code -1 + var12 + t/3})
     */
    public static List<RibbonTile> ribbon(final float length, final float size, final float n,
            final float endMod, final float vStart) {
        final List<RibbonTile> tiles = new ArrayList<>();
        final float span = length * size / n;
        if (span <= 0.0F) {
            return tiles;
        }
        float y = 0.0F;
        while (y < span) {
            // The next v-integer boundary = the next tile edge. The epsilon
            // keeps an exact-integer phase (ceil(v) == v) from yielding a
            // zero-width tile, and converting the boundary back to y
            // (boundary − vStart) makes a fractional phase (vStart = −0.3)
            // advance to the real edge instead of crawling 1e-6 at a time.
            final float boundaryV = (float) Math.ceil(vStart + y + 1.0e-4F);
            final float yEnd = Math.min(span, boundaryV - vStart);
            tiles.add(new RibbonTile(y, yEnd, halfWidth(size, endMod, y, span),
                    halfWidth(size, endMod, yEnd, span), vStart + y, vStart + yEnd));
            y = yEnd;
        }
        return tiles;
    }

    /** Linear taper between the near width 0.15*size and the far width 0.15*size*endMod. */
    public static float halfWidth(final float size, final float endMod, final float y, final float span) {
        return RIBBON_HALF_WIDTH * size * (1.0F + (endMod - 1.0F) * (y / span));
    }

    /**
     * The spawn distance gate (FXBeam.java:66-73): the render view entity's
     * distance over 50 (fancy) / 25 (fast) forces {@code particleMaxAge = 0},
     * i.e. the beam never appears. The port gates the plan the same way.
     */
    public static int visibleAge(final double viewDistance, final int age, final boolean fancyGraphics) {
        return viewDistance > (fancyGraphics ? VISIBLE_RANGE_FANCY : VISIBLE_RANGE_FAST) ? 0 : age;
    }

    /** The bolt's per-frame render gate (FXLightningBolt.java:155-161). */
    public static boolean boltVisible(final double viewDistance, final boolean fancyGraphics) {
        return !(viewDistance > (fancyGraphics ? BOLT_RANGE_FANCY : BOLT_RANGE_FAST));
    }

    /**
     * The golem-boss head anchor (FXBeamGolemBoss.java:39-52): the boss's
     * position pushed 0.5 along the vanilla look basis
     * {@code (f2*f3, f4, f1*f3)} with
     * {@code f1 = cos(-yawRad - PI)}, {@code f2 = sin(-yawRad - PI)},
     * {@code f3 = -cos(-pitchRad)}, {@code f4 = sin(-pitchRad)}; the y anchor
     * is the boss's position plus its eye height.
     */
    public static double[] headAnchor(final double bossX, final double bossY, final double bossZ,
            final float yawDegrees, final float pitchDegrees, final float eyeHeight) {
        final float yawRad = -yawDegrees * (float) (Math.PI / 180.0) - (float) Math.PI;
        final float f1 = (float) Math.cos(yawRad);
        final float f2 = (float) Math.sin(yawRad);
        final float pitchRad = pitchDegrees * (float) (Math.PI / 180.0);
        final float f3 = -(float) Math.cos(pitchRad);
        final float f4 = -(float) Math.sin(pitchRad);
        return new double[] {bossX + f2 * f3 * 0.5, bossY + eyeHeight, bossZ + f1 * f3 * 0.5};
    }

    /**
     * The golem-boss target anchor (FXBeamGolemBoss.java:60-63, 95-98):
     * the target's interpolated (previous-tick) x/z and its bounding-box
     * bottom plus half its width — the original reads {@code prevPosX},
     * {@code boundingBox.minY} and {@code width} (1.21.1's {@code xo/zo},
     * {@code getBoundingBox().minY} and {@code getBbWidth()}).
     */
    public static double targetAnchorY(final double boundingBoxMinY, final float width) {
        return boundingBoxMinY + width / 2.0F;
    }
}
