package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.util.Mth;

/**
 * The orbiting shield runes' measured rules — {@code FXShieldRunes.java} line
 * by line, plus its packet handler {@code PacketFXShield.onMessage}
 * (TC-M7-02-c-s-f4, D-017).
 *
 * <p>Split out of the client particle for the same reason as
 * {@link FxWardRules}: a dedicated server (the GameTest server) and a plain
 * unit test can pin the roll, the frame ramp, the camera yaw and the sprite
 * branch without a client load.</p>
 *
 * <h2>The frame family, stated in files</h2>
 * The original binds {@code textures/models/hemis<frame>.png}, or
 * {@code textures/models/ripple<frame>.png} when the tracked entity is a mob
 * that is not a cultist (FXShieldRunes.java:61-66). The frame number is
 * {@code min(15, (int)(14 * fade) + 1)} (line 60), so it runs over 1..15 and
 * the hemis/ripple files 1..15 are what the family consumes — 15 sprites, not
 * 16 (hemis0 belongs to the ward's strip, {@link FxWardRules}). The mob branch
 * also halves the colour to 0.5 (line 63), which the port ships as the second
 * particle type: 1.21.1 has no per-particle GL blend/tint layer switch, and the
 * frame list and the tint change together, so
 * {@code thaumcraft:shield_runes_mob} carries both.
 *
 * <h2>The seam</h2>
 * The original draws the loaded {@code hemis.obj} hemisphere
 * (FXShieldRunes.java:50-52, 76) at a fixed GL basis with
 * {@code glRotatef(180 - yaw, 0, 1, 0)} then {@code glRotatef(-pitch, 1, 0, 0)}
 * (lines 72-73). A 1.21.1 particle is one billboard quad in the camera basis,
 * so the port draws one sprite with the same yaw/pitch composed on top of the
 * camera rotation. That is the seam BD-16 declares for the whole FX lane, restated
 * for this family in BD-46; the card's "camera basis is NOT chased" rule applies.
 */
public final class FxShieldRules {

    /**
     * The age the senders hand in: {@code new FXShieldRunes(..., 8, yaw, pitch)}
     * (PacketFXShield.java:60, 63, 65, 68, 71) and the constructor's
     * {@code particleMaxAge = age + rand.nextInt(age / 2)}
     * (FXShieldRunes.java:31) — with age 8 that roll is {@code 8 + [0,3]}.
     */
    public static final int AGE_BASE = 8;
    public static final int AGE_ROLL_DIVISOR = 2;

    /**
     * The frame ramp's sprite count: the located draw binds
     * {@code hemis<frame>.png} / {@code ripple<frame>.png} with
     * {@code frame = min(15, (int)(14F * fade) + 1)} (FXShieldRunes.java:60),
     * so the frames are 1..15 — fifteen sprites, not sixteen (hemis0 belongs to
     * the ward's strip, {@link FxWardRules}). There is no texture-index line in
     * this file at all: the frame is chosen in the draw, every render, which is
     * why the port picks the sprite in {@code render} too.
     */
    public static final int FRAME_CLAMP = 15;
    public static final float FRAME_SCALE = 14.0F;

    /** The fade the frame ramp, the alpha and the size all read. */
    public static float fade(final int age, final float partialTick, final int lifetime) {
        return (age + partialTick) / Math.max(1.0F, (float) lifetime);
    }

    /** The 1-based frame number (1..15). */
    public static int frame(final float fade) {
        return Math.min(FRAME_CLAMP, (int)(FRAME_SCALE * fade) + 1);
    }

    /**
     * The index into the 15-sprite frame list: frame 1 is the first sprite,
     * so {@code frame - 1}. The ward and the runes share the hemis files and
     * start at different frames (hemis0 vs hemis1), which is why the sprite
     * names carry the source frame number.
     */
    public static int spriteIndex(final int frame) {
        return Math.max(0, Math.min(FRAME_CLAMP - 1, frame - 1));
    }

    /** {@code glColor4f(b, b, b, min(1, (1 - fade) * 3))} alpha (line 75). */
    public static float alpha(final float fade) {
        return Math.min(1.0F, (1.0F - fade) * 3.0F);
    }

    /**
     * The colour multiplier {@code b}: 1.0 for the hemis form, 0.5 for the
     * mob's ripple form (FXShieldRunes.java:59-66).
     */
    public static float brightness(final boolean mob) {
        return mob ? 0.5F : 1.0F;
    }

    /**
     * The mob branch's condition: {@code target instanceof EntityMob &&
     * !(target instanceof EntityCultist)} (line 61). The cultist family is not
     * landed on this repo (no {@code EntityCultist} class), so the caller
     * passes the exclusion as a flag and the branch stays plan-visible — the
     * shape {@code TcFxPlans.blockArc}'s {@code cultistPortal} parameter uses.
     */
    public static boolean usesRippleFrames(final boolean mob, final boolean cultist) {
        return mob && !cultist;
    }

    /** {@code GL11.glScaled(0.4 * target.width, ...)} (line 74). */
    public static final float SCALE_FROM_WIDTH = 0.4F;

    /** The quad half-extent: the original's scale is a full-extent GL scale. */
    public static float scale(final double entityWidth) {
        return (float)(SCALE_FROM_WIDTH * entityWidth);
    }

    /**
     * The yaw aimed at the target: {@code (atan2(d2, d0) * 180 / PI) - 90}
     * where {@code d0 = source.x - target.x} and {@code d2 = source.z -
     * target.z} (PacketFXShield.java:46-54).
     */
    public static float yawTo(final double dx, final double dz) {
        return (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
    }

    /**
     * The pitch aimed at the target: {@code -(atan2(d1, d3) * 180 / PI)} where
     * {@code d1} is the AABB-centre height delta and {@code d3} the horizontal
     * distance (PacketFXShield.java:47-53).
     */
    public static float pitchTo(final double dy, final double horizontal) {
        return (float)-(Math.atan2(dy, horizontal) * 180.0 / Math.PI);
    }

    /** The horizontal distance {@code d3 = sqrt(d0*d0 + d2*d2)} (line 50). */
    public static double horizontal(final double dx, final double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * The handler's sentinel targets: {@code -1} fires the runes twice (pitch
     * 90 then 270), {@code -2} once at 270, {@code -3} once at 90, and a
     * target id that resolves to no entity draws at pitch 90
     * (PacketFXShield.java:55-72). Yaw stays 0 in every sentinel branch.
     */
    public static final int TARGET_ANY = -1;
    public static final int TARGET_BELOW = -2;
    public static final int TARGET_ABOVE = -3;
    public static final float SENTINEL_YAW = 0.0F;
    public static final float SENTINEL_PITCH_DOWN = 90.0F;
    public static final float SENTINEL_PITCH_UP = 270.0F;

    /**
     * The draw's light: {@code int i = 220; j = i % 65536; k = i / 65536;}
     * pushed through {@code OpenGlHelper.setLightmapTextureCoords} as
     * {@code (220, 0)} (FXShieldRunes.java:68-71) — block light 220 of a
     * possible 240, sky light 0. 1.21.1's particle has no numeric block light,
     * only a packed world light, so the port pins the block light at its
     * maximum and the sky light at 0, the located pair; BD-46 states the
     * mapping. The ward's {@code tessellator.setBrightness(240)}
     * (FXBlockWard.java:75) is the same arithmetic — 240 block, 0 sky.
     */
    public static final int FULL_BRIGHT_BLOCK_LIGHT = 220;
    public static final int SKY_LIGHT = 0;

    /** {@code EntityFX} defaults the constructor keeps: no gravity, no motion. */
    public static final float GRAVITY = 0.0F;

    /**
     * The AABB the constructor sizes: {@code setSize(0.01F, 0.01F)} around the
     * noClip flag (FXShieldRunes.java:32-34).
     */
    public static final float AABB_SIZE = 0.01F;

    private FxShieldRules() {
    }

    /**
     * The lifetime roll: {@code age + rand.nextInt(age / 2)}, expressed as the
     * rolled extra so the plan owns the random (FXShieldRunes.java:31).
     */
    public static int lifetime(final int baseAge, final int rolledExtra) {
        return baseAge + rolledExtra;
    }

    /** The rolled extra the constructor consumes: {@code rand.nextInt(age/2)}. */
    public static int rolledExtra(final int baseAge) {
        return baseAge / AGE_ROLL_DIVISOR;
    }

    /** Degrees to radians, shared by the particle's camera composition. */
    public static float toRadians(final float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }
}
