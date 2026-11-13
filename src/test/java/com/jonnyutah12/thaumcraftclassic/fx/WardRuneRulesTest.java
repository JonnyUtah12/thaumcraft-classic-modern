package com.jonnyutah12.thaumcraftclassic.fx;

import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXShieldPayload;
import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXWardBlockPayload;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.EntityHitResult;
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
        assertEquals(0.0, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.UP, Direction.Axis.Y));
        assertEquals(0.3, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.UP, Direction.Axis.X));
        assertEquals(0.1, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.UP, Direction.Axis.Z));
        assertEquals(0.3, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.NORTH, Direction.Axis.X),
                "a north hit zeroes Z, not X");
        assertEquals(0.0, FxWardRules.zeroJitter(0.3, 0.2, 0.1, Direction.NORTH, Direction.Axis.Z),
                "NORTH's stepZ is -1, and the rule zeroes the component along the normal, "
                        + "whatever its sign (lines 36-46)");
        for (final Direction side : Direction.values()) {
            for (final Direction.Axis axis : Direction.Axis.values()) {
                assertEquals(side.getStepX() != 0 && axis == Direction.Axis.X
                                || side.getStepY() != 0 && axis == Direction.Axis.Y
                                || side.getStepZ() != 0 && axis == Direction.Axis.Z,
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
     * {@code -0.505} otherwise (FXBlockWard.java:64-69), carried by the same
     * 90-degree face rotation the quad carries (line 63). Measured per side:
     * every face pushes OUTWARD along its own normal, which is the physical
     * sanity check the original has.
     *
     * <p>The 180-degree flip (line 66) is a separate assertion: it follows the
     * translate in the GL stack, so it turns the quad and never the push.</p>
     */
    @Test
    void wardFaceOffsetSignAndRotation() {
        final Vec3 north = FxWardRules.faceOffset(Direction.NORTH, 0.0);
        assertEquals(0.0, north.x, 1.0e-6);
        assertEquals(0.0, north.y, 1.0e-6);
        assertEquals(-0.505, north.z, 1.0e-6,
                "a north hit takes the else-branch translate (0, 0, -0.505) (line 68)");
        final Vec3 south = FxWardRules.faceOffset(Direction.SOUTH, 0.0);
        assertEquals(0.0, south.x, 1.0e-6);
        assertEquals(0.0, south.y, 1.0e-6);
        assertEquals(0.505, south.z, 1.0e-6,
                "the offsetZ > 0 branch translates +0.505 (line 65); the 180-degree flip at "
                        + "line 66 comes after it, so it turns the quad, not the push");
        final Vec3 up = FxWardRules.faceOffset(Direction.UP, 0.0);
        assertEquals(0.0, up.x, 1.0e-6);
        assertEquals(0.505, up.y, 1.0e-6,
                "the +90-degree rotation about (offsetY, -offsetX, offsetZ) = (1, 0, 0) "
                        + "(line 63) maps the local -Z translate to +Y, so an up hit sits "
                        + "ABOVE the block centre");
        assertEquals(0.0, up.z, 1.0e-6);
        final Vec3 down = FxWardRules.faceOffset(Direction.DOWN, 0.0);
        assertEquals(0.0, down.x, 1.0e-6);
        assertEquals(-0.505, down.y, 1.0e-6, "the DOWN face's axis is (-1, 0, 0): -Z maps to -Y");
        assertEquals(0.0, down.z, 1.0e-6);
        final Vec3 west = FxWardRules.faceOffset(Direction.WEST, 0.0);
        assertEquals(-0.505, west.x, 1.0e-6, "a west hit pushes west (axis (0, 1, 0), line 63)");
        assertEquals(0.0, west.y, 1.0e-6);
        assertEquals(0.0, west.z, 1.0e-6);
        final Vec3 east = FxWardRules.faceOffset(Direction.EAST, 0.0);
        assertEquals(0.505, east.x, 1.0e-6, "an east hit pushes east (axis (0, -1, 0), line 63)");
        assertEquals(0.0, east.y, 1.0e-6);
        assertEquals(0.0, east.z, 1.0e-6);
        assertEquals(0.505F, FxWardRules.FACE_OFFSET);

        // The invariant that ties them together: a 0.505-long push along the
        // face normal, for every face.
        for (final Direction side : Direction.values()) {
            final Vec3 push = FxWardRules.faceOffset(side, 0.0);
            assertEquals(0.505, push.length(), 1.0e-5, side + ": the push is 0.505 long");
            assertEquals(side.getStepX() * 0.505, push.x, 1.0e-5,
                    side + ": the push lies on the face normal");
            assertEquals(side.getStepY() * 0.505, push.y, 1.0e-5, side + ": y");
            assertEquals(side.getStepZ() * 0.505, push.z, 1.0e-5, side + ": z");
        }

        // The spin is about (0, 0, 1) (line 64) and the push lies ON that axis
        // in the local frame, so no rotation roll moves the push at all.
        for (final Direction side : Direction.values()) {
            for (final int roll : new int[] {0, 90, 180, 270, 359}) {
                final Vec3 a = FxWardRules.faceOffset(side, 0.0);
                final Vec3 b = FxWardRules.faceOffset(side, roll);
                assertEquals(a.x, b.x, 1.0e-5, side + " roll " + roll + ": x");
                assertEquals(a.y, b.y, 1.0e-5, side + " roll " + roll + ": y");
                assertEquals(a.z, b.z, 1.0e-5, side + " roll " + roll + ": z");
            }
        }

        // The quad, not the push: an UP hit lays the quad flat on the top face
        // (its local +Y vertex lands on world +Z), and a SOUTH hit carries the
        // extra 180-degree flip (line 66) that a north hit does not.
        final org.joml.Vector3f upVertex = new org.joml.Vector3f(0.0F, 0.5F, 0.0F)
                .rotate(FxWardRules.quadRotation(Direction.UP, 0.0));
        assertEquals(0.0F, upVertex.x, 1.0e-5);
        assertEquals(0.0F, upVertex.y, 1.0e-5);
        assertEquals(0.5F, upVertex.z, 1.0e-5,
                "the hexagram lies flat on the top face: the quad's local up is world +Z");
        final org.joml.Vector3f flipped = new org.joml.Vector3f(0.5F, 0.0F, 0.0F)
                .rotate(FxWardRules.quadRotation(Direction.SOUTH, 0.0));
        assertEquals(0.0F, flipped.x, 1.0e-5);
        assertEquals(-0.5F, flipped.y, 1.0e-5, "the offsetZ > 0 flip turns the quad (line 66)");
        assertEquals(0.0F, flipped.z, 1.0e-5);
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
        assertEquals(8, FxShieldRules.lifetime(FxShieldRules.AGE_BASE, 0));
        assertEquals(11, FxShieldRules.lifetime(FxShieldRules.AGE_BASE, 3),
                "the roll is nextInt(8 / 2) = nextInt(4), so the top of the range is 8 + 3 = 11, "
                        + "not 12 (an earlier draft of this test read the roll as nextInt(4) + 1)");
        assertEquals(4, FxShieldRules.rolledExtra(FxShieldRules.AGE_BASE),
                "the rolled extra is age / AGE_ROLL_DIVISOR = 8 / 2 = 4");
        assertEquals(2, FxShieldRules.AGE_ROLL_DIVISOR);
    }

    /** {@code setSize(0.01F, 0.01F)} (FXShieldRunes.java:33). */
    @Test
    void shieldAabbSize() {
        assertEquals(0.01F, FxShieldRules.AABB_SIZE);
    }

    /**
     * The aim the packet handler computes: {@code yaw = (atan2(dz, dx) * 180 /
     * PI) - 90} and {@code pitch = -(atan2(dy, d3) * 180 / PI)}, where
     * {@code d0/d2} are {@code source - target} in x/z, {@code d3} is
     * {@code sqrt(d0*d0 + d2*d2)} and {@code d1} the AABB-centre height delta
     * (PacketFXShield.java:46-54).
     *
     * <p><b>The camera term is not a rule here.</b> The original's draw added
     * {@code glRotatef(180 - yaw, 0, 1, 0)} / {@code glRotatef(-pitch, 1, 0, 0)}
     * on top of a matrix stack the vanilla renderer had already camera-transformed
     * (FXShieldRunes.java:72-73), and the trig pair the 1.7.10 renderer fed it
     * lives in {@code RenderGlobal}, which is not in the reference tree — the
     * FX lane's BD-16. This card's definition of done closes the camera basis
     * out of scope, so {@link FxShieldRules} deliberately has no
     * {@code cameraYaw} / {@code cameraPitch} / {@code runeYaw} /
     * {@code runePitch} method, and an earlier draft of this test that asserted
     * them is replaced by the located handler arithmetic (D-017: name only what
     * the located code supports). The port composes the payload yaw/pitch on
     * top of 1.21.1's camera-facing mode, which is the BD-16 rule.</p>
     */
    @Test
    void shieldAimIsThePacketsAtan2Pair() {
        assertEquals(0.0F, FxShieldRules.yawTo(0.0, 4.0), 1.0e-6,
                "a target due south: atan2(4, 0) is 90 degrees, minus 90 is 0");
        assertEquals(90.0F, FxShieldRules.yawTo(-4.0, 0.0), 1.0e-6,
                "a target due east of the source: atan2(0, -4) is 180, minus 90 is 90");
        assertEquals(-180.0F, FxShieldRules.yawTo(0.0, -4.0), 1.0e-6,
                "a target due north: atan2(-4, 0) is -90, minus 90 is -180");
        assertEquals(0.0F, FxShieldRules.pitchTo(0.0, 4.0), 1.0e-6,
                "equal AABB-centre heights aim level");
        assertEquals(-45.0F, FxShieldRules.pitchTo(4.0, 4.0), 1.0e-6,
                "the pitch is the NEGATED atan2 of the height delta over the horizontal");
        assertEquals(45.0F, FxShieldRules.pitchTo(-4.0, 4.0), 1.0e-6);
        assertEquals(5.0, FxShieldRules.horizontal(3.0, 4.0), 1.0e-9, "d3 is the hypotenuse");
        assertEquals((float) Math.PI, FxShieldRules.toRadians(180.0F), 1.0e-6);
    }

    /**
     * The sentinel targets of {@code PacketFXShield.onMessage}
     * (PacketFXShield.java:55-72): {@code -1} fires the rune twice (pitch 90
     * then 270), {@code -2} once at 270, {@code -3} once at 90, and a target id
     * with no entity draws at pitch 90. The yaw stays 0 in every branch, which
     * is what makes the sentinels aim at the horizon rather than at anything.
     */
    @Test
    void shieldSentinelsAreThreePitchesAndOneYaw() {
        assertEquals(-1, FxShieldRules.TARGET_ANY);
        assertEquals(-2, FxShieldRules.TARGET_BELOW);
        assertEquals(-3, FxShieldRules.TARGET_ABOVE);
        assertEquals(0.0F, FxShieldRules.SENTINEL_YAW);
        assertEquals(90.0F, FxShieldRules.SENTINEL_PITCH_DOWN);
        assertEquals(270.0F, FxShieldRules.SENTINEL_PITCH_UP);
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
        assertTrue(FxShieldRules.usesRippleFrames(true, false),
                "a mob that is not a cultist takes the ripple frames (FXShieldRunes.java:61)");
        assertFalse(FxShieldRules.usesRippleFrames(false, false), "a player never does");
        assertFalse(FxShieldRules.usesRippleFrames(true, true),
                "EntityCultist is excluded from the branch — the class is not landed here, so the "
                        + "caller passes the exclusion as a flag and the rule stays plan-visible");
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
        assertTrue(runeFrames.stream().noneMatch(f -> f.equals("hemis0")),
                "the rune ramp is 1..15 — hemis0 is the ward's first frame (a suffix test "
                        + "would reject hemis10 for the wrong reason)");
    }

    /**
     * The two S2C halves of this slice, on the wire. {@code FXShieldPayload} is
     * the original's {@code PacketFXShield} pair of entity ids
     * (PacketFXShield.java:22-33); {@code FXWardBlockPayload} carries
     * {@code ClientProxy.blockWard}'s parameters verbatim — block, hit face, and
     * the {@code (hitVec - pos)} triple (ClientProxy.java:980-984).
     *
     * <p>The ward payload's face rides the 3D data value, so the round trip is
     * walked for all six faces: a swapped mapping is visible there and nowhere
     * else. The shield payload is two VAR_INTs, so it is sampled with a
     * multi-byte id and with 0, and the two fields carry distinct values so a
     * slot swap is visible.</p>
     */
    @Test
    void fxPayloadsRoundTripOnTheWire() {
        final FXShieldPayload[] shields = {
            new FXShieldPayload(1, 2),
            new FXShieldPayload(0, 0),
            new FXShieldPayload(8388609, 1),
            new FXShieldPayload(42, 7),
        };
        for (final FXShieldPayload sent : shields) {
            final io.netty.buffer.ByteBuf buffer = io.netty.buffer.Unpooled.buffer();
            try {
                FXShieldPayload.STREAM_CODEC.encode(buffer, sent);
                assertEquals(sent, FXShieldPayload.STREAM_CODEC.decode(buffer), "shield payload");
                assertEquals(0, buffer.readableBytes(), "trailing bytes for " + sent);
            } finally {
                buffer.release();
            }
        }

        // BlockPos.STREAM_CODEC is the packed long (x/z 26 bits, y 14 bits in
        // 1.21.1), measured: an out-of-range y wraps on the wire. The ward
        // blocks live in the overworld, so the probe samples a near-origin
        // pos and the signed extremes of the packed layout.
        final BlockPos[] positions = {
            new BlockPos(-7, 64, 13),
            new BlockPos(33554430, -64, -33554431),
        };
        for (final BlockPos pos : positions) {
            for (final Direction side : Direction.values()) {
                final FXWardBlockPayload sent =
                        new FXWardBlockPayload(pos, side, new Vec3(0.25, -0.5, 0.9999));
                final io.netty.buffer.ByteBuf buffer = io.netty.buffer.Unpooled.buffer();
                try {
                    FXWardBlockPayload.STREAM_CODEC.encode(buffer, sent);
                    final FXWardBlockPayload got = FXWardBlockPayload.STREAM_CODEC.decode(buffer);
                    assertEquals(sent.pos(), got.pos(), side + " " + pos + ": pos");
                    assertEquals(sent.side(), got.side(),
                            side + ": the face rides the 3D data value");
                    assertEquals(sent.hit().x, got.hit().x, 1.0e-9, side + ": f");
                    assertEquals(sent.hit().y, got.hit().y, 1.0e-9, side + ": f1");
                    assertEquals(sent.hit().z, got.hit().z, 1.0e-9, side + ": f2");
                    assertEquals(0, buffer.readableBytes(), "trailing bytes for " + side);
                } finally {
                    buffer.release();
                }
            }
        }
    }


}
