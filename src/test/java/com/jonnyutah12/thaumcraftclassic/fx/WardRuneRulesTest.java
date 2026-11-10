package com.jonnyutah12.thaumcraftclassic.fx;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jonnyutah12.thaumcraftclassic.client.fx.FxShieldRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.FxWardRules;
import com.jonnyutah12.thaumcraftclassic.client.fx.ParticleSpawn;

/**
 * The ward hexagram's and the shield runes' measured law, pinned without a
 * client (TC-M7-02-c-s-f4). {@link FxWardRules} and {@link FxShieldRules} carry
 * no client import, so this runs in the plain unit suite: every constant, the
 * roll order's ranges, the face geometry and the frame ramps. The
 * {@code FxParticleGameTest} pins the same law's plan side against a real
 * level; this pins the arithmetic the plan hands to it.
 *
 * <p>Each number cites the original: {@code FXBlockWard.java} (113 lines) and
 * {@code FXShieldRunes.java} (96 lines) under
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/fx/other/}.</p>
 */
class WardRuneRulesTest {

    // ---- FXBlockWard -------------------------------------------------------

    /** {@code particleMaxAge = 12 + rand.nextInt(5)} (FXBlockWard.java:24). */
    @Test
    void wardLifetimeRange() {
        for (int roll = 0; roll < FxWardRules.LIFETIME_ROLLS; roll++) {
            assertEquals(12 + roll, FxWardRules.lifetime(roll));
        }
        assertEquals(5, FxWardRules.LIFETIME_ROLLS, "the roll is nextInt(5), not nextInt(4)");
    }

    /**
     * The jitter zeroes the component <em>along the hit normal</em>
     * (FXBlockWard.java:36-46) and keeps the other two, in X, Y, Z order.
     */
    @Test
    void wardJitterZeroesTheAxisAlongTheNormal() {
        assertEquals(0.0, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.UP, Direction.Y));
        assertEquals(0.3, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.UP, Direction.X));
        assertEquals(0.1, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.UP, Direction.Z));
        assertEquals(0.3, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.NORTH, Direction.X),
                "a north hit zeroes Z, not X");
        assertEquals(0.1, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.NORTH, Direction.Z));
        for (final Direction side : Direction.values()) {
            for (final Direction.Axis axis : Direction.Axis.values()) {
                assertEquals(side.getStepX() != 0 && axis == Direction.X
                                || side.getStepY() != 0 && axis == Direction.Y
                                || side.getStepZ() != 0 && axis == Direction.Z,
                        FxWardRules.zeroesAxis(side, axis),
                        side + "/" + axis + ": exactly one axis is zeroed");
            }
        }
    }

    /**
     * {@code clamp(f - 0.6F + rand.nextFloat() * 0.2F, -0.4F, 0.4F)}
     * (FXBlockWard.java:33-35): the span is 0.2 and the clamp is +-0.4, so a
     * centred hit ({@code f = 0.5}) lands in [-0.1, 0.1].
     */
    @Test
    void wardJitterSpanAndClamp() {
        assertEquals(-0.1, FxWardRules.jitter(0.5, 0.0), 1.0e-9);
        assertEquals(0.1, FxWardRules.jitter(0.5, 1.0), 1.0e-9);
        // The clamp is real: an extreme sender offset is pinned, not passed.
        assertEquals(0.4, FxWardRules.jitter(1.0, 1.0), 1.0e-9);
        assertEquals(-0.4, FxWardRules.jitter(-1.0, 0.0), 1.0e-9);
    }

    /**
     * The face push is {@code +0.505} when {@code side.offsetZ > 0} and
     * {@code -0.505} otherwise (FXBlockWard.java:65-70), rotated by the same
     * matrix as the quad. A north hit ({@code offsetZ = -1}) with no roll puts
     * it at {@code z - 0.505}; a south hit ({@code offsetZ = +1}) adds the
     * 180-degree flip, which maps the push to the same world side.
     */
    @Test
    void wardFaceOffsetSignAndRotation() {
        final Vec3 north = FxWardRules.faceOffset(Direction.NORTH, 0.0);
        assertEquals(0.0, north.x, 1.0e-6);
        assertEquals(0.0, north.y, 1.0e-6);
        assertEquals(-0.505, north.z, 1.0e-6);
        final Vec3 south = FxWardRules.faceOffset(Direction.SOUTH, 0.0);
        assertEquals(0.0, south.x, 1.0e-6);
        assertEquals(0.0, south.y, 1.0e-6);
        assertEquals(0.505, south.z, 1.0e-6, "the 180 flip lands the +0.505 push on +Z");
        final Vec3 up = FxWardRules.faceOffset(Direction.UP, 0.0);
        assertEquals(0.0, up.x, 1.0e-6, "an up hit pushes along -Z, which the 90-degree "
                + "X-axis rotation maps to -Y (FXBlockWard.java:63)");
        assertEquals(-0.505, up.y, 1.0e-6);
        assertEquals(0.0, up.z, 1.0e-6);
        assertEquals(0.505F, FxWardRules.FACE_OFFSET);
    }

    /**
     * The frame ramp: {@code fade = (age + partialTick) / particleMaxAge},
     * {@code frame = min(15, (int)(15.0F * fade))} over
     * {@code hemis<frame>.png} (FXBlockWard.java:52-54) — sixteen frames,
     * {@code hemis0} included, and the clamp is what keeps the last frame
     * indexable.
     */
    @Test
    void wardFrameRamp() {
        assertEquals(0, FxWardRules.frame(FxWardRules.fade(0, 0.0F, 15)));
        assertEquals(1, FxWardRules.frame(FxWardRules.fade(1, 0.0F, 15)),
                "the ramp is 15F * fade, so frame 1 arrives at age 1 of 15");
        assertEquals(15, FxWardRules.frame(FxWardRules.fade(15, 0.0F, 15)));
        assertEquals(15, FxWardRules.frame(FxWardRules.fade(99, 0.0F, 15)),
                "min(15, ...) is what keeps hemis15 the last frame");
        assertEquals(16, FxWardRules.FRAME_COUNT, "hemis0..hemis15 — 0 belongs to the ward");
    }

    /**
     * The alpha is a fade-in then a fade-out, all floats
     * (FXBlockWard.java:93-98): {@code threshold = life / 5.0F},
     * {@code age <= threshold ? age / threshold : (life - age) / life}, and the
     * draw halves it (line 58).
     */
    @Test
    void wardAlphaCurveIsAFadeInFadeOut() {
        // threshold = 15 / 5.0F = 3.0: a float, so the ramp is not integer-stepped.
        assertEquals(0.0F, FxWardRules.alpha(15, 0), 1.0e-6);
        assertEquals(1.0F, FxWardRules.alpha(15, 3), 1.0e-6,
                "alpha reaches 1.0 exactly at the threshold");
        assertEquals(6.0F / 15.0F, FxWardRules.alpha(15, 9), 1.0e-6, "the fade-out side");
        assertEquals(0.0F, FxWardRules.alpha(15, 15), 1.0e-6);
        assertTrue(FxWardRules.alpha(15, 6) > FxWardRules.alpha(15, 9), "monotone after the peak");
        // A lifetime the window does not divide evenly (12 / 5.0F = 2.4) keeps the
        // fraction: this is what separates the located law from an integer reading.
        assertEquals(2.0F / 2.4F, FxWardRules.alpha(12, 2), 1.0e-6);
        assertEquals(1.0F / 2.4F, FxWardRules.alpha(12, 1), 1.0e-6);
        assertEquals(9.0F / 12.0F, FxWardRules.alpha(12, 3), 1.0e-6);
    }

    /** The draw's alpha is the tick's, halved (FXBlockWard.java:58, 76). */
    @Test
    void wardDrawAlphaIsHalfTheTickAlpha() {
        assertEquals(FxWardRules.alpha(15, 7) / 2.0F, FxWardRules.drawAlpha(15, 7), 1.0e-6);
        // The constructor leaves particleAlpha at EntityFX's default 1.0F
        // (lines 19-47 touch it nowhere), so the first render draws at half.
        assertEquals(1.0F, FxWardRules.INITIAL_ALPHA);
        assertEquals(0.5F, FxWardRules.INITIAL_ALPHA / FxWardRules.DRAW_ALPHA_DIVISOR);
    }

    /** The quad's half-extent is {@code particleScale / 2} (lines 72, 77-80). */
    @Test
    void wardQuadHalfExtentIsHalfTheScale() {
        assertEquals(2.0, FxWardRules.QUAD_HALF_EXTENT_DIVISOR);
        assertEquals(1.4, FxWardRules.scale(0.0), 1.0e-9, "the gaussian's centre");
    }

    /** The mote never falls: {@code field_70545_g = 0.0F} (line 22). */
    @Test
    void wardMoteHasNoGravity() {
        assertEquals(0.0F, FxWardRules.GRAVITY);
        assertEquals(0.0F, FxShieldRules.GRAVITY, "FXShieldRunes.java:29");
    }

    // ---- FXShieldRunes -----------------------------------------------------

    /** {@code particleMaxAge = 8 + rand.nextInt(8 / 2)} (FXShieldRunes.java:31). */
    @Test
    void shieldLifetimeRange() {
        assertEquals(8, FxShieldRules.lifetime(0));
        assertEquals(12, FxShieldRules.lifetime(4));
        assertEquals(4, FxShieldRules.LIFETIME_ROLLS, "nextInt(8 / 2) is nextInt(4)");
    }

    /** {@code setSize(0.01F, 0.01F)} (FXShieldRunes.java:33). */
    @Test
    void shieldAabbSize() {
        assertEquals(0.01F, FxShieldRules.AABB_SIZE);
    }

    /**
     * The camera yaw is {@code 180 - camera.getYRot()} and the rune yaw
     * {@code 180 - yaw}, summed (PacketFXShield.java:47-49 +
     * FXShieldRunes.java:72); the pitch is {@code -camera.getXRot()} and
     * {@code -pitch}, summed (lines 56-58, 73).
     */
    @Test
    void shieldAnglesSumCameraAndPayload() {
        assertEquals(180.0F, FxShieldRules.cameraYaw(0.0F), "a camera looking south");
        assertEquals(0.0F, FxShieldRules.cameraYaw(180.0F));
        assertEquals(170.0F, FxShieldRules.runeYaw(10.0F));
        assertEquals(-20.0F, FxShieldRules.cameraPitch(20.0F));
        assertEquals(-7.0F, FxShieldRules.runePitch(7.0F));
        assertEquals(160.0F, FxShieldRules.yawDegrees(10.0F, 20.0F), "180-20 + 180-10, mod 360");
        assertEquals(-27.0F, FxShieldRules.pitchDegrees(7.0F, 20.0F));
    }

    /**
     * The frame ramp: {@code frame = min(15, (int)(14.0F * fade) + 1)}
     * (FXShieldRunes.java:60) — frames 1..15, so the family ships fifteen
     * sprites and {@code hemis0} belongs to the ward alone.
     */
    @Test
    void shieldFrameRampStartsAtOne() {
        assertEquals(1, FxShieldRules.frame(FxWardRules.fade(0, 0.0F, 8)));
        assertEquals(15, FxShieldRules.frame(FxWardRules.fade(8, 0.0F, 8)));
        assertEquals(15, FxShieldRules.frame(FxWardRules.fade(99, 0.0F, 8)));
        assertEquals(0, FxShieldRules.spriteIndex(1), "the frame IS the file suffix; the list is 0-based");
        assertEquals(14, FxShieldRules.spriteIndex(15));
    }

    /**
     * The alpha is the fade's tail: {@code min(1, (1 - fade) * 3)}
     * (FXShieldRunes.java:75).
     */
    @Test
    void shieldAlphaIsTheFadeTail() {
        assertEquals(1.0F, FxShieldRules.alpha(0.0F));
        assertEquals(1.0F, FxShieldRules.alpha(0.2F), "the clamp: (1-0.2)*3 = 2.4 -> 1.0");
        assertEquals(0.0F, FxShieldRules.alpha(1.0F));
    }

    /**
     * The mob branch (FXShieldRunes.java:61-66): the {@code 0.5} colour and the
     * {@code ripple} frames fire for a mob that is not a cultist — and for a
     * player they never do, which is why the player form keeps the {@code hemis}
     * frames.
     */
    @Test
    void shieldMobBranchHalvesColourAndSwapsFrames() {
        assertEquals(1.0F, FxShieldRules.brightness(false));
        assertEquals(0.5F, FxShieldRules.brightness(true));
        assertTrue(FxShieldRules.usesRippleFrames(true));
        assertFalse(FxShieldRules.usesRippleFrames(false));
    }

    /** The ward's frame list and the runes' two frame lists are disjoint sources. */
    @Test
    void wardAndRuneFramesComeFromThreeListsOfModelFiles() {
        List<String> wardFrames = new java.util.ArrayList<>();
        for (int frame = 0; frame < FxWardRules.FRAME_COUNT; frame++) {
            wardFrames.add("hemis" + frame);
        }
        List<String> runeFrames = new java.util.ArrayList<>();
        List<String> mobFrames = new java.util.ArrayList<>();
        for (int frame = 1; frame <= FxShieldRules.FRAME_CLAMP; frame++) {
            runeFrames.add("hemis" + frame);
            mobFrames.add("ripple" + frame);
        }
        assertEquals(16, wardFrames.size());
        assertEquals(15, runeFrames.size());
        assertEquals(15, mobFrames.size());
        assertFalse(wardFrames.contains("ripple0"));
        assertTrue(runeFrames.stream().noneMatch(f -> f.endsWith("0")),
                "the rune ramp is 1..15 — hemis0 is the ward's first frame");
    }
}
