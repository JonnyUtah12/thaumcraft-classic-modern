package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Eldritch Golem world-free (TC-M6-06-s-c5-3) — no level, no entity
 * instance, no registry, no attribute build (the live pair rides the golem probes
 * of {@code BossMobGameTest}, which run on a bootstrapped server). Every
 * expectation quotes the decompiled line it was read from in
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityEldritchGolem.java}
 * (370 lines), so "cleaning up" one of the original's figures — a goal slot, a
 * constructor argument, the 1.75 x 3.5 box, the phase threshold, the 150-charge
 * ceiling, the 15-point spend, the blue flag, the hardness window, a status id, an
 * arc radius — turns a test red.
 */
class EldritchGolemAiLogicTest {

    private static final float F = 1.0E-4F;

    // ------------------------------------------------------- registration figures (:119-120, :54)

    @Test
    void theRegistrationRowIsTheOriginals() {
        // registerModEntity(EntityEldritchGolem.class, "EldritchGolem", 31, instance,
        // 64, 3, true) — ConfigEntities.java:119
        assertEquals(64, EldritchGolemAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(4, EldritchGolemAiLogic.TRACK_RANGE_CHUNKS);       // 64 blocks = 4 chunks
        assertEquals(3, EldritchGolemAiLogic.UPDATE_INTERVAL);
        assertTrue(EldritchGolemAiLogic.VELOCITY_UPDATES);
        // :54 func_70105_a(1.75F, 3.5F) — the box the renderer lane and GolemMenuGameTest bind to
        assertEquals(1.75F, EldritchGolemAiLogic.WIDTH, F);
        assertEquals(3.5F, EldritchGolemAiLogic.HEIGHT, F);
        // :120 addMapping("EldritchGolem", 5592405, 4210752)
        assertEquals(0x555555, EldritchGolemAiLogic.EGG_PRIMARY);
        assertEquals(0x404040, EldritchGolemAiLogic.EGG_SECONDARY);
        assertEquals(5592405, EldritchGolemAiLogic.EGG_PRIMARY);
        assertEquals(4210752, EldritchGolemAiLogic.EGG_SECONDARY);
    }

    @Test
    void theAiFlagOfTheConstructorIsThePlatformsStandingState() {
        // :55 this.field_70178_ae = true (setAIEnabled(true)) — 1.21.1's AI is always enabled,
        // which is why the constructor has no seam for the write.
        assertTrue(EldritchGolemAiLogic.AI_ENABLED);
    }

    // ------------------------------------------------------- attributes (:106-108, :100, :95-97)

    @Test
    void theAttributesAreTheOriginalsOverTheBossBasePair() {
        // :106-108 — speed 0.3, attack 10.0, max health 250.0
        assertEquals(0.3, EldritchGolemAiLogic.MOVEMENT_SPEED, 1.0E-9);
        assertEquals(10.0, EldritchGolemAiLogic.ATTACK_DAMAGE, 1.0E-9);
        assertEquals(250.0, EldritchGolemAiLogic.MAX_HEALTH, 1.0E-9);
        // :100 func_70658_aO() = super.getArmor() + 6
        assertEquals(6.0, EldritchGolemAiLogic.ARMOR_BONUS, 1.0E-9);
        // the super pass (:105) carries the boss base's pair — EntityThaumcraftBoss:53-54,
        // the follow-range parity fix this card ships: 16.0 (the stub's Monster default) -> 40.0
        assertEquals(0.95, ThaumcraftBossAiLogic.KNOCKBACK_RESISTANCE, 1.0E-9);
        assertEquals(40.0, ThaumcraftBossAiLogic.FOLLOW_RANGE, 1.0E-9);
        assertFalse(ThaumcraftBossAiLogic.FOLLOW_RANGE <= 16.0);
    }

    @Test
    void theEyeHeightPairIsTheOriginals() {
        // :95-97 func_70047_e(): 3.0F, and 3.33F once headless
        assertEquals(3.0F, EldritchGolemAiLogic.EYE_HEIGHT, F);
        assertEquals(3.33F, EldritchGolemAiLogic.EYE_HEIGHT_HEADLESS, F);
        assertEquals(3.0F, EldritchGolemAiLogic.eyeHeightFor(false), F);
        assertEquals(3.33F, EldritchGolemAiLogic.eyeHeightFor(true), F);
    }

    // ------------------------------------------------------- goal table (:46-53, :186)

    @Test
    void everyGoalSitsAtItsOriginalSlot() {
        assertEquals(0, EldritchGolemAiLogic.GOAL_SLOT_SWIM);           // :46
        assertEquals(3, EldritchGolemAiLogic.GOAL_SLOT_MELEE);          // :47
        assertEquals(6, EldritchGolemAiLogic.GOAL_SLOT_RESTRICTION);    // :48
        assertEquals(7, EldritchGolemAiLogic.GOAL_SLOT_WANDER);         // :49
        assertEquals(8, EldritchGolemAiLogic.GOAL_SLOT_WATCH);          // :50
        assertEquals(8, EldritchGolemAiLogic.GOAL_SLOT_LOOK_IDLE);      // :51
        assertEquals(1, EldritchGolemAiLogic.TARGET_SLOT_HURT);         // :52
        assertEquals(2, EldritchGolemAiLogic.TARGET_SLOT_PLAYER);       // :53
    }

    @Test
    void theRangedGoalIsNotInTheConstructorTable() {
        // :46-53 has eight writes and NONE of them is a ranged goal; the ONLY writer of the
        // slot-2 AILongRangeAttack is makeHeadless() (:185-187), reached by the phase rule.
        assertEquals(2, EldritchGolemAiLogic.GOAL_SLOT_RANGED);         // :186
        assertEquals(6, EldritchGolemAiLogic.SHIPPED_MOVEMENT_GOALS);   // :46-51, slot-8 pair kept
        assertEquals(2, EldritchGolemAiLogic.SHIPPED_TARGET_GOALS);     // :52-53
        assertNotEqualsSlot(EldritchGolemAiLogic.GOAL_SLOT_RANGED, EldritchGolemAiLogic.GOAL_SLOT_MELEE);
        assertNotEqualsSlot(EldritchGolemAiLogic.GOAL_SLOT_RANGED, EldritchGolemAiLogic.GOAL_SLOT_SWIM);
        assertNotEqualsSlot(EldritchGolemAiLogic.GOAL_SLOT_RANGED, EldritchGolemAiLogic.GOAL_SLOT_RESTRICTION);
        assertNotEqualsSlot(EldritchGolemAiLogic.GOAL_SLOT_RANGED, EldritchGolemAiLogic.GOAL_SLOT_WANDER);
        assertNotEqualsSlot(EldritchGolemAiLogic.GOAL_SLOT_RANGED, EldritchGolemAiLogic.GOAL_SLOT_WATCH);
        assertNotEqualsSlot(EldritchGolemAiLogic.GOAL_SLOT_RANGED, EldritchGolemAiLogic.GOAL_SLOT_LOOK_IDLE);
    }

    private static void assertNotEqualsSlot(final int a, final int b) {
        assertFalse(a == b, "the mid-fight goal collides with a constructor slot (" + a + ")");
    }

    @Test
    void theRangedGoalCarriesTheGolemsOwnConstructorArguments() {
        // :186 AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F) — NOT the praetor's 16.0/30/40 of :58
        assertEquals(3.0, EldritchGolemAiLogic.RANGED_MIN_DISTANCE, 1.0E-9);
        assertEquals(1.0, EldritchGolemAiLogic.RANGED_SPEED, 1.0E-9);
        assertEquals(5, EldritchGolemAiLogic.RANGED_INTERVAL_MIN);
        assertEquals(5, EldritchGolemAiLogic.RANGED_INTERVAL_MAX);
        assertEquals(24.0F, EldritchGolemAiLogic.RANGED_RANGE, F);
        assertEquals(16.0, CultistLeaderAiLogic.RANGED_MIN_DISTANCE, 1.0E-9);  // the pair differs
    }

    @Test
    void theMeleeRestrictionWanderWatchAndTargetFiguresAreTheOriginals() {
        // :47 AIAttackOnCollide(this, EntityLivingBase.class, 1.1, false)
        assertEquals(1.1, EldritchGolemAiLogic.MELEE_SPEED, 1.0E-9);
        assertFalse(EldritchGolemAiLogic.MELEE_LONG_MEMORY);
        // :48-49 — 0.8, 0.8
        assertEquals(0.8, EldritchGolemAiLogic.RESTRICTION_SPEED, 1.0E-9);
        assertEquals(0.8, EldritchGolemAiLogic.WANDER_SPEED, 1.0E-9);
        // :50 EntityAIWatchClosest(this, EntityPlayer.class, 8.0F)
        assertEquals(8.0F, EldritchGolemAiLogic.WATCH_PLAYER_RANGE, F);
        assertEquals(0.02F, EldritchGolemAiLogic.WATCH_PLAYER_PROBABILITY, F);
        // :52 EntityAIHurtByTarget(this, true) — the 1.7.10 vanilla class, so the port ships
        // vanilla's HurtByTargetGoal and NEVER calls setAlertOthers (the golem lane's reading)
        assertTrue(EldritchGolemAiLogic.HURT_ALERTS_SAME_TYPE);
        // :53 EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true)
        assertTrue(EldritchGolemAiLogic.TARGET_MUST_SEE);
    }

    // ------------------------------------------------------- phase rule (:170-187, :123-127)

    @Test
    void thePhaseRuleFiresAboveTheCurrentMaxHealthExactlyOnce() {
        // :172 if (!world.isRemote && damage > this.func_110143_aJ() && !isHeadless())
        assertEquals(1.0F, EldritchGolemAiLogic.PHASE_DAMAGE_MULTIPLIER, F);
        assertFalse(EldritchGolemAiLogic.shouldGoHeadless(250.0F, 250.0F, false)); // not >
        assertTrue(EldritchGolemAiLogic.shouldGoHeadless(250.5F, 250.0F, false));
        assertFalse(EldritchGolemAiLogic.shouldGoHeadless(250.5F, 250.0F, true));  // once only
        // a damaged max (the 1.7.10 threshold rides the CURRENT max health, func_110143_aJ)
        assertTrue(EldritchGolemAiLogic.shouldGoHeadless(120.0F, 100.0F, false));
    }

    @Test
    void thePhaseLegCarriesTheSpawnTimerAndBlastFigures() {
        // :125, :174 spawnTimer = 100; :283 the client's 150; :288 heal 2.0F per tick
        assertEquals(100, EldritchGolemAiLogic.SPAWN_TIMER_PHASE_TICKS);
        assertEquals(150, EldritchGolemAiLogic.SPAWN_TIMER_BROADCAST_TICKS);
        assertEquals(2.0F, EldritchGolemAiLogic.SPAWN_HEAL_AMOUNT, F);
        // :177 world.newExplosion(this, x + xx, y + getEyeHeight(), z + zz, 2.0F, false)
        assertEquals(2.0F, EldritchGolemAiLogic.EXPLOSION_STRENGTH, F);
        assertFalse(EldritchGolemAiLogic.EXPLOSION_DESTROY_BLOCKS);
        // :175-176 — cos(yaw % 360 / 180 * PI) * 0.75, sin(yaw % 360 / 180 * PI) * 0.75
        assertEquals(0.75, EldritchGolemAiLogic.explosionOffsetX(0.0F), 1.0E-9);
        assertEquals(0.0, EldritchGolemAiLogic.explosionOffsetZ(0.0F), 1.0E-9);
        assertEquals(0.0, EldritchGolemAiLogic.explosionOffsetX(90.0F), 1.0E-9);
        assertEquals(0.75, EldritchGolemAiLogic.explosionOffsetZ(90.0F), 1.0E-9);
        assertEquals(-0.75, EldritchGolemAiLogic.explosionOffsetX(180.0F), 1.0E-9);
    }

    // ------------------------------------------------------- melee leg (:189-211)

    @Test
    void theMeleeLegIsGatedOnTheTenTickTimer() {
        // :190-193 — if (attackTimer <= 0) { attackTimer = 10; broadcast(4); ... }
        assertEquals(10, EldritchGolemAiLogic.ATTACK_TIMER_TICKS);
        assertTrue(EldritchGolemAiLogic.meleeGate(0));
        assertFalse(EldritchGolemAiLogic.meleeGate(1));
        assertFalse(EldritchGolemAiLogic.meleeGate(10));
    }

    @Test
    void theMeleeDamageIsThreeQuartersWithALiftAndHeadlessKnockback() {
        // :196 target.attackEntityFrom(DamageSource.causeMobDamage, attackDamage * 0.75F)
        assertEquals(0.75F, EldritchGolemAiLogic.MELEE_DAMAGE_RATIO, F);
        assertEquals(7.5F, EldritchGolemAiLogic.meleeDamageFor(10.0F), F);
        // :199 target.motionY += 0.2
        assertEquals(0.2, EldritchGolemAiLogic.MELEE_LIFT, 1.0E-9);
        // :201-205 headless: addVelocity(-sin(yaw * PI / 180) * 1.5, 0.1, cos(yaw * PI / 180) * 1.5)
        assertEquals(1.5F, EldritchGolemAiLogic.HEADLESS_KNOCKBACK_FORCE, F);
        assertEquals(0.1, EldritchGolemAiLogic.HEADLESS_KNOCKBACK_LIFT, 1.0E-9);
        assertEquals(0.0, EldritchGolemAiLogic.headlessKnockbackX(0.0F), 1.0E-9);
        assertEquals(1.5, EldritchGolemAiLogic.headlessKnockbackZ(0.0F), 1.0E-9);
        assertEquals(-1.5, EldritchGolemAiLogic.headlessKnockbackX(90.0F), 1.0E-9);
        assertEquals(0.0, EldritchGolemAiLogic.headlessKnockbackZ(90.0F), 1.0E-9);
    }

    // ------------------------------------------------------- beam leg (:213-236, :346-358)

    @Test
    void theBeamGateIsTheTargetTwinPlusTheChargedState() {
        // :214 if (func_70685_l(target) && !chargingBeam && beamCharge > 0)
        assertTrue(EldritchGolemAiLogic.beamGate(true, false, 150));
        assertTrue(EldritchGolemAiLogic.beamGate(true, false, 1));
        assertFalse(EldritchGolemAiLogic.beamGate(true, false, 0));    // spent dry
        assertFalse(EldritchGolemAiLogic.beamGate(true, true, 150));   // charging
        assertFalse(EldritchGolemAiLogic.beamGate(false, false, 150)); // target twin
        assertEquals(1, EldritchGolemAiLogic.BEAM_MIN_CHARGE);
    }

    @Test
    void theBeamSpendIsFifteenPlusAJitterOfFive() {
        // :215 beamCharge -= 15 + random.nextInt(5)
        assertEquals(15, EldritchGolemAiLogic.beamSpendFor(0));
        assertEquals(19, EldritchGolemAiLogic.beamSpendFor(4));
        assertEquals(15, EldritchGolemAiLogic.BEAM_SPEND_BASE);
        assertEquals(5, EldritchGolemAiLogic.BEAM_SPEND_JITTER);
    }

    @Test
    void theChargeMachineStopsAtExactlyOneHundredFifty() {
        // :347-348 if (isHeadless() && beamCharge <= 0) chargingBeam = true
        assertTrue(EldritchGolemAiLogic.chargeStarts(true, 0));
        assertFalse(EldritchGolemAiLogic.chargeStarts(true, 1));
        assertFalse(EldritchGolemAiLogic.chargeStarts(false, 0));      // headed golems never charge
        // :351 if (isHeadless() && chargingBeam) beamCharge += 1; broadcast(19)
        assertTrue(EldritchGolemAiLogic.chargeAdvances(true, true));
        assertFalse(EldritchGolemAiLogic.chargeAdvances(false, true));
        assertFalse(EldritchGolemAiLogic.chargeAdvances(true, false));
        assertEquals(1, EldritchGolemAiLogic.CHARGE_STEP);
        // :354-356 if (beamCharge == 150) chargingBeam = false — the ceiling, not a cap
        assertEquals(150, EldritchGolemAiLogic.BEAM_CHARGE_TARGET);
        assertTrue(EldritchGolemAiLogic.chargeStops(150));
        assertFalse(EldritchGolemAiLogic.chargeStops(149));
        assertFalse(EldritchGolemAiLogic.chargeStops(151));
    }

    @Test
    void theAimLegPointsAtTheTargetsMiddleAtThirtyDegreesPerTick() {
        // :216-223 getLookHelper().setLookPosition(x, minY + height / 2, z, 30.0F, 30.0F)
        assertEquals(30.0F, EldritchGolemAiLogic.AIM_ROTATION_SPEED, F);
    }

    @Test
    void theOrbIsTheBlueFlagWithThePinnedAimAndSound() {
        // :225 new EntityGolemOrb(world, this, target, false) — the blue pair (BD-133's family
        // finding: the flag is (isRed, life, damage): false = 200 ticks / x1.25)
        assertFalse(EldritchGolemAiLogic.ORB_IS_RED);
        assertTrue(CultistLeaderAiLogic.ORB_IS_RED);
        // :224-228 the nudge rides the GOLEM's own look vector, x/z only, scale 1.0
        assertEquals(1.0F, EldritchGolemAiLogic.ORB_NUDGE_LOOK_SCALE, F);
        // :232 func_70186_c(d0, d1, d2, 0.66F, 5.0F) — no aim lift, unlike the praetor's +2.0
        assertEquals(0.66F, EldritchGolemAiLogic.ORB_SPEED, F);
        assertEquals(5.0F, EldritchGolemAiLogic.ORB_SPREAD, F);
        // :233 playSound("thaumcraft:egattack", 1.0F, 1.0F + rand * 0.1F)
        assertEquals(1.0F, EldritchGolemAiLogic.ORB_SOUND_VOLUME, F);
        assertEquals(1.0F, EldritchGolemAiLogic.ORB_SOUND_PITCH_BASE, F);
        assertEquals(0.1F, EldritchGolemAiLogic.ORB_SOUND_PITCH_JITTER, F);
    }

    // ------------------------------------------------------- footstep / crush legs (:135-167)

    @Test
    void theFootstepGateNeedsRealMotionAndARollOfZero() {
        // :135 motionX * motionX + motionZ * motionZ > 2.5000003E-7F && random.nextInt(5) == 0
        assertTrue(EldritchGolemAiLogic.footstepGate(0.001, 0.0, 0));
        assertFalse(EldritchGolemAiLogic.footstepGate(0.001, 0.0, 1));   // the roll
        assertFalse(EldritchGolemAiLogic.footstepGate(0.0, 0.0, 0));     // standing still
        assertEquals(2.5000003E-7F, EldritchGolemAiLogic.FOOTSTEP_MOTION_SQR, 1.0E-13F);
        assertEquals(5, EldritchGolemAiLogic.FOOTSTEP_CHANCE_DIVISOR);
        // :137 the block row read: floor(posY - 0.2F - height)
        assertEquals(0.2F, EldritchGolemAiLogic.FOOTSTEP_DEPTH_BELOW_FEET, F);
    }

    @Test
    void theCrushGateIsTheHardnessWindowOfTheOriginal() {
        // :164 if (h >= 0.0F && h <= 0.15F) — air / unbreakable is -1, stone is 1.5, snow 0.1
        assertTrue(EldritchGolemAiLogic.crushGate(0.0F));
        assertTrue(EldritchGolemAiLogic.crushGate(0.15F));
        assertTrue(EldritchGolemAiLogic.crushGate(0.1F));
        assertFalse(EldritchGolemAiLogic.crushGate(-1.0F));
        assertFalse(EldritchGolemAiLogic.crushGate(0.16F));
        assertFalse(EldritchGolemAiLogic.crushGate(1.5F));
        assertEquals(0.0F, EldritchGolemAiLogic.CRUSH_HARDNESS_MIN, F);
        assertEquals(0.15F, EldritchGolemAiLogic.CRUSH_HARDNESS_MAX, F);
    }

    // ------------------------------------------------------- client status ids (:238-279)

    @Test
    void theStatusIdsAndTheirClientEffectsAreTheOriginals() {
        // :194 broadcast(4) / :240-242 the client sets the 10-tick swing timer and plays
        // mob.irongolem.throw at 1.0F / 1.0F
        assertEquals((byte) 4, EldritchGolemAiLogic.STATUS_MELEE);
        assertEquals(10, EldritchGolemAiLogic.ATTACK_TIMER_TICKS);
        assertEquals(1.0F, EldritchGolemAiLogic.STATUS_MELEE_VOLUME, F);
        assertEquals(1.0F, EldritchGolemAiLogic.STATUS_MELEE_PITCH, F);
        // :283-284 broadcast(18) / :243-244 the client writes spawnTimer = 150
        assertEquals((byte) 18, EldritchGolemAiLogic.STATUS_SPAWN_PHASE);
        assertEquals(150, EldritchGolemAiLogic.SPAWN_TIMER_BROADCAST_TICKS);
        // :353 broadcast(19) / :245-275 the arc anchor pick
        assertEquals((byte) 19, EldritchGolemAiLogic.STATUS_CHARGE);
    }

    @Test
    void theArcAnchorPickIsTheOriginals() {
        // :247 2.0F + random * 2.0F; :255-257 five steps down; :263 8 + random.nextInt(5)
        assertEquals(2.0F, EldritchGolemAiLogic.arcRadiusFor(0.0F), F);
        assertEquals(4.0F, EldritchGolemAiLogic.arcRadiusFor(1.0F), F);
        assertEquals(3.0F, EldritchGolemAiLogic.arcRadiusFor(0.5F), F);
        assertEquals(8, EldritchGolemAiLogic.arcTicksFor(0));
        assertEquals(12, EldritchGolemAiLogic.arcTicksFor(4));
        assertEquals(5, EldritchGolemAiLogic.ARC_SCAN_STEPS);
        assertEquals(8, EldritchGolemAiLogic.ARC_TICKS_BASE);
        assertEquals(5, EldritchGolemAiLogic.ARC_TICKS_JITTER);
        // :264-273 thaumcraft:jacobs at 0.8F, pitch 1.0F + (r - r) * 0.05F
        assertEquals(0.8F, EldritchGolemAiLogic.ARC_SOUND_VOLUME, F);
        assertEquals(1.0F, EldritchGolemAiLogic.ARC_SOUND_PITCH_BASE, F);
        assertEquals(0.05F, EldritchGolemAiLogic.ARC_SOUND_PITCH_JITTER, F);
    }

    // ------------------------------------------------------- sound trio (:111-121)

    @Test
    void theSoundTrioIsTheIronGolemSetAtAFlatPair() {
        // :112-114 mob.irongolem.hit, :115-117 mob.irongolem.death, :118-121 mob.irongolem.walk,
        // every one of them at 1.0F volume and 1.0F pitch
        assertEquals(3, EldritchGolemAiLogic.SOUND_TRIO.length);
        assertEquals("mob.irongolem.hit", EldritchGolemAiLogic.SOUND_TRIO[0]);
        assertEquals("mob.irongolem.death", EldritchGolemAiLogic.SOUND_TRIO[1]);
        assertEquals("mob.irongolem.walk", EldritchGolemAiLogic.SOUND_TRIO[2]);
        assertEquals(1.0F, EldritchGolemAiLogic.SOUND_VOLUME, F);
        assertEquals(1.0F, EldritchGolemAiLogic.SOUND_PITCH, F);
    }
}
