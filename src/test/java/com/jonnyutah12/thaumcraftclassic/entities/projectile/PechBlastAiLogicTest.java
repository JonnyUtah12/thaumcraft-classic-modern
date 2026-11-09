package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original Pech blast projectile world-free (TC-M6-06-s-c3). Every
 * expectation quotes the decompiled line it was read from, in
 * {@code thaumcraft.common.entities.projectile.EntityPechBlast} (182 lines),
 * plus the launch call site at {@code EntityPech.java:481-486} and the
 * registration triple at {@code ConfigEntities.java:86}.
 *
 * <p>The world side — the registry id, the live flight (gravity, no block
 * collision), the impact on a live entity, the age-out and the FX sends — is
 * proven by {@code PechMobGameTest}.</p>
 */
class PechBlastAiLogicTest {

    private static final float F = 1.0E-4F;
    private static final double D = 1.0E-4;

    @Test
    void registrationIsTheProjectilesOwnTriple() {
        // registerModEntity(..., "PechBlast", ..., 64, 20, true) —
        // ConfigEntities.java:86. The update interval is 20, NOT the 3 the
        // mobs use: the original relies on the tracker for its arc, which is
        // why 1.21.1's builder must call setShouldReceiveVelocityUpdates(true).
        assertEquals(64, PechBlastAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(20, PechBlastAiLogic.UPDATE_INTERVAL);
        assertTrue(PechBlastAiLogic.VELOCITY_UPDATES);
        // EntityThrowable's own box (func_70026_a(0.5F, 0.5F))
        assertEquals(0.5F, PechBlastAiLogic.WIDTH, F);
        assertEquals(0.5F, PechBlastAiLogic.HEIGHT, F);
    }

    @Test
    void flightKeepsTheOriginalPhysics() {
        // :38-40 — func_70185_h() { return 0.025F; } — half the vanilla
        // arrow's fall; 1.21.1's default gravity is 0.08, so the override is
        // required and not cosmetic.
        assertEquals(0.025F, PechBlastAiLogic.GRAVITY, F);
        // EntityPech.java:486 — func_70182_d(d0, d1 + f1*0.1F, d2, 1.5F, 4.0F)
        assertEquals(1.5F, PechBlastAiLogic.SPEED, F);
        assertEquals(4.0F, PechBlastAiLogic.AIM_INACCURACY, F);
        // :25 — the launch bias is target.y + eyeHeight - 0.1
        assertEquals(0.1F, PechBlastAiLogic.AIM_EYE_BIAS, F);
    }

    @Test
    void ageOutIsFiveHundredTicksAndStrictlyGreater() {
        // :79-81 — if (this.field_70173_aa > 500) this.func_70106_y();
        assertEquals(500, PechBlastAiLogic.MAX_AGE);
        assertFalse(PechBlastAiLogic.isExpired(0));
        assertFalse(PechBlastAiLogic.isExpired(500));
        assertTrue(PechBlastAiLogic.isExpired(501));
    }

    @Test
    void impactDamageIsStrengthPlusTwo() {
        // :147 — func_70097_a(source, this.strength + 2). The Pech's only
        // blast passes strength 1 (EntityPech.java:481), so 3.
        assertEquals(3, PechBlastAiLogic.damageFor(1));
        assertEquals(2, PechBlastAiLogic.damageFor(0));
        assertEquals(5, PechBlastAiLogic.damageFor(3));
    }

    @Test
    void impactSkipsItsOwnSpeciesAndNonLivingEntities() {
        // :142 — the scan box is func_72314_b(2.0, 2.0, 2.0); :144-146 —
        // `!(entity1 instanceof EntityPech) && entity1 instanceof
        // EntityLivingBase` gates both the damage and the effects.
        assertEquals(2.0, PechBlastAiLogic.SCAN_INFLATE, D);
        assertTrue(PechBlastAiLogic.canAffect(false, true));
        assertFalse(PechBlastAiLogic.canAffect(true, true));    // never a pech
        assertFalse(PechBlastAiLogic.canAffect(false, false));  // not an item entity
    }

    @Test
    void effectsAreTheNightshadeTripletOrOneOfThree() {
        // :149-171 — nightshade: all three potions, each behind its own
        // nextInt(3) == 0 draw in the non-nightshade branch (the original
        // wraps them in try/catch(Exception)). Duration 100 + duration*40;
        // the slowness leg carries amplifier + 1.
        assertEquals(3, PechBlastAiLogic.EFFECT_ROLL_BOUND);
        assertEquals(100, PechBlastAiLogic.EFFECT_BASE_DURATION);
        assertEquals(40, PechBlastAiLogic.EFFECT_DURATION_PER_DURATION_ARG);
        assertEquals(100, PechBlastAiLogic.effectDuration(0));    // the Pech's duration arg is 0
        assertEquals(140, PechBlastAiLogic.effectDuration(1));
        assertEquals(980, PechBlastAiLogic.effectDuration(22));   // 100 + 22*40
        assertEquals(1, PechBlastAiLogic.SLOWNESS_AMPLIFIER_OFFSET);
        assertEquals(0.1F, PechBlastAiLogic.NIGHTSHADE_CHANCE, F);
        // EntityPech.java:481 — `rand.nextFloat() < 0.1F`
        assertTrue(PechBlastAiLogic.nightshadeFor(0.0999F));
        assertFalse(PechBlastAiLogic.nightshadeFor(0.1F));
        assertFalse(PechBlastAiLogic.nightshadeFor(0.5F));
    }

    @Test
    void theClientFxCountsAreTheTickAndImpactLoops() {
        // :48 (3 trail rounds), :86 (9 impact rounds), :52-54 / :87-89 (the
        // per-axis jitter factors), :96-98 (the 8.0F target scale), the
        // wispFX2 size 0.3F / fade 0.02F pair
        assertEquals(3, PechBlastAiLogic.TRAIL_ROUNDS);
        assertEquals(9, PechBlastAiLogic.IMPACT_ROUNDS);
        assertEquals(0.2F, PechBlastAiLogic.TRAIL_POS_JITTER, F);
        assertEquals(0.1F, PechBlastAiLogic.SPARKLE_POS_JITTER, F);
        assertEquals(0.3F, PechBlastAiLogic.IMPACT_POS_JITTER, F);
        assertEquals(8.0F, PechBlastAiLogic.IMPACT_TARGET_SCALE, F);
        assertEquals(0.3F, PechBlastAiLogic.PARTICLE_SIZE, F);
        assertEquals(0.02F, PechBlastAiLogic.PARTICLE_FADE, F);
        assertEquals(3, PechBlastAiLogic.TRAIL_TYPE_AT_POSITION);   // :56
        assertEquals(2, PechBlastAiLogic.TRAIL_TYPE_AT_MIDPOINT);   // :67
        assertEquals(5, PechBlastAiLogic.TRAIL_SPARKLE_TYPE);       // :73
        assertEquals(3, PechBlastAiLogic.IMPACT_TYPES[0]);          // :90-98 types 3, 2, 0
        assertEquals(2, PechBlastAiLogic.IMPACT_TYPES[1]);
        assertEquals(0, PechBlastAiLogic.IMPACT_TYPES[2]);
        assertEquals(2.0, PechBlastAiLogic.MIDPOINT_DIVISOR, D);    // :61-66
    }

    @Test
    void fxCallGeometryFollowsTheDecompiledShapes() {
        final RandomSource random = RandomSource.create(20261104L);

        // :90-103 — the impact particle sits at pos + jitter(0.3F) and aims at
        // pos + jitter*8.0F, i.e. the target is the offset scaled, not a velocity
        final PechBlastAiLogic.BlastFxCall impact =
                PechBlastAiLogic.BlastFxCall.impact(random, 1.0, 2.0, 3.0, 0);
        final double fx = impact.dx() - 1.0;
        final double fy = impact.dy() - 2.0;
        final double fz = impact.dz() - 3.0;
        assertEquals(1.0 + fx * 8.0, impact.tx(), D);
        assertEquals(2.0 + fy * 8.0, impact.ty(), D);
        assertEquals(3.0 + fz * 8.0, impact.tz(), D);
        assertEquals(3, impact.type());
        // the jitter is (nextFloat() - nextFloat()) * 0.3F, so |offset| <= 0.3
        assertTrue(Math.abs(fx) <= 0.3 + 1.0E-6);
        assertTrue(Math.abs(fy) <= 0.3 + 1.0E-6);
        assertTrue(Math.abs(fz) <= 0.3 + 1.0E-6);

        // :49-60 — the trail particle is AT the blast's own position
        final PechBlastAiLogic.BlastFxCall trail =
                PechBlastAiLogic.BlastFxCall.atPosition(random, 5.0, 6.0, 7.0);
        assertEquals(5.0, trail.tx(), D);
        assertEquals(6.0, trail.ty(), D);
        assertEquals(7.0, trail.tz(), D);
        assertEquals(3, trail.type());
        assertEquals(0.3F, trail.size(), F);
        assertEquals(0.02F, trail.fade(), F);

        // :68-74 — the sparkle: jitter factor 0.1F, type 5, no size/fade
        final PechBlastAiLogic.BlastFxCall sparkle =
                PechBlastAiLogic.BlastFxCall.sparkle(random, 0.0, 0.0, 0.0);
        assertEquals(5, sparkle.type());
        assertEquals(0.0F, sparkle.size(), F);
        assertTrue(Math.abs(sparkle.dx()) <= 0.1 + 1.0E-6);
    }
}
