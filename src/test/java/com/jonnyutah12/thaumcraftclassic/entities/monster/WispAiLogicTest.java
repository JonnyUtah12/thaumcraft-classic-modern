package com.jonnyutah12.thaumcraftclassic.entities.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original {@code EntityWisp} AI/navigation rules (TC-M6-06-s)
 * without a world. Every expectation below quotes the decompiled source it was
 * read from; if someone "cleans up" one of the original's quirks these go red.
 */
class WispAiLogicTest {

    // ------------------------------------------------------------ attributes

    @Test
    void originalAttributes() {
        // func_110147_ax: health 22.0, attack damage 3.0
        assertEquals(22.0, WispAiLogic.MAX_HEALTH);
        assertEquals(3.0, WispAiLogic.ATTACK_DAMAGE);
        // func_70105_a(0.9F, 0.9F)
        assertEquals(0.9F, WispAiLogic.WIDTH);
        assertEquals(0.9F, WispAiLogic.HEIGHT);
        // func_70641_bl() -> 2
        assertEquals(2, WispAiLogic.EXPERIENCE_REWARD);
        // func_70599_aP() -> 0.25F
        assertEquals(0.25F, WispAiLogic.SOUND_VOLUME);
        // func_70628_a -> 2 units of the wisp's own aspect
        assertEquals(2, WispAiLogic.ESSENCE_DROP);
    }

    // ------------------------------------------------------------- waypoints

    @Test
    void waypointRePickWindowIsOneToSixtyBlocks() {
        // the original rejects d3 < 1.0 and d3 > 3600.0
        assertFalse(WispAiLogic.waypointInRange(0.0, 0.5, 0.0), "distance^2 below 1.0 re-picks");
        assertTrue(WispAiLogic.waypointInRange(1.0, 0.0, 0.0), "distance^2 exactly 1.0 is usable");
        assertTrue(WispAiLogic.waypointInRange(0.0, 0.0, 60.0), "distance^2 exactly 3600.0 is usable");
        assertFalse(WispAiLogic.waypointInRange(0.0, 0.0, 60.1), "beyond 3600.0 re-picks");
    }

    @Test
    void waypointOffsetIsSixteenBlocksScaledByTheSignedRoll() {
        assertEquals(16.0, WispAiLogic.waypointOffset(1.0F));
        assertEquals(-16.0, WispAiLogic.waypointOffset(-1.0F));
        assertEquals(0.0, WispAiLogic.waypointOffset(0.0F));
        assertEquals(8.0, WispAiLogic.waypointOffset(0.5F));
    }

    @Test
    void courseChangeCooldownIsTwoPlusUpToFour() {
        // courseChangeCooldown += rand.nextInt(5) + 2 -> 2..6
        assertEquals(2, WispAiLogic.nextCourseChangeCooldown(0));
        assertEquals(6, WispAiLogic.nextCourseChangeCooldown(4));
    }

    @Test
    void steeringAccelerationIsTenPercentOfTheUnitDelta() {
        // motion += delta / d3 * 0.1
        assertEquals(0.1, WispAiLogic.steerComponent(10.0, 10.0), 1.0E-9);
        assertEquals(-0.05, WispAiLogic.steerComponent(-10.0, 20.0), 1.0E-9);
        assertEquals(0.0, WispAiLogic.steerComponent(0.0, 10.0), 1.0E-9);
    }

    @Test
    void courseTraversabilityNeedsAClearRayAndASolidBelowAndNoLiquidWaypoint() {
        assertTrue(WispAiLogic.isCourseTraversable(0, false, 0), "solid right at the waypoint");
        assertTrue(WispAiLogic.isCourseTraversable(0, false, 10), "last sample of the 11-block column");
        assertFalse(WispAiLogic.isCourseTraversable(1, false, 0), "a collision along the ray refuses");
        assertFalse(WispAiLogic.isCourseTraversable(0, true, 0), "a liquid waypoint refuses");
        assertFalse(WispAiLogic.isCourseTraversable(0, false, -1), "no solid in 11 samples refuses");
        assertFalse(WispAiLogic.isCourseTraversable(0, false, 11), "the 12th sample is out of range");
        assertEquals(11, WispAiLogic.COURSE_DROP_SAMPLES);
        assertEquals(10, WispAiLogic.COURSE_DROP_MAX_INDEX);
    }

    // ----------------------------------------------------------------- aggro

    @Test
    void aggroCooldownTicksDownEveryTick() {
        // this.aggroCooldown-- runs unconditionally before the roll block
        final WispAiLogic.Aggro quiet = WispAiLogic.aggroTick(10, 999, false);
        assertEquals(9, quiet.cooldown());
        assertFalse(quiet.attempt());
    }

    @Test
    void aggroRollZeroWithNoTargetSearchesWithoutASecondDecrement() {
        // rand==0 && targetedEntity == null short-circuits before the inner --
        final WispAiLogic.Aggro hit = WispAiLogic.aggroTick(10, 0, false);
        assertEquals(9, hit.cooldown());
        assertTrue(hit.attempt());
    }

    @Test
    void aggroRollZeroWithATargetDoubleDecrementsInTheOriginal() {
        // rand==0 && targetedEntity != null -> the inner aggroCooldown-- also runs,
        // and the value it compares is the post-outer-decrement one.
        final WispAiLogic.Aggro warm = WispAiLogic.aggroTick(10, 0, true);
        assertEquals(8, warm.cooldown(), "both decrements apply");
        assertFalse(warm.attempt(), "10 -> 9 -> 8, still above zero");

        final WispAiLogic.Aggro due = WispAiLogic.aggroTick(1, 0, true);
        assertEquals(-1, due.cooldown(), "1 -> 0 (compared) -> -1 (stored)");
        assertTrue(due.attempt(), "the compared value was 0");
    }

    @Test
    void aggroReArmAndHurtFigures() {
        assertEquals(50, WispAiLogic.AGGRO_COOLDOWN);
        assertEquals(200, WispAiLogic.HURT_AGGRO_COOLDOWN);
        assertEquals(1000, WispAiLogic.AGGRO_ROLL_BOUND);
        assertEquals(16.0, WispAiLogic.AGGRO_RADIUS);
    }

    // ---------------------------------------------------------------- attack

    @Test
    void zapCadenceIsTwentyAndResetsBelowZero() {
        assertFalse(WispAiLogic.atZapCadence(19));
        assertTrue(WispAiLogic.atZapCadence(20));
        assertFalse(WispAiLogic.atZapCadence(21));
        // attackCounter = -20 + rand.nextInt(20) -> -20..-1
        assertEquals(-20, WispAiLogic.nextAttackCounterAfterZap(0));
        assertEquals(-1, WispAiLogic.nextAttackCounterAfterZap(19));
    }

    @Test
    void stationaryTargetIsHitOnSixtySixPercentForBonusDamage() {
        assertTrue(WispAiLogic.targetIsStationary(0.1, 0.1, 0.1), "0.1 is the inclusive bound");
        assertFalse(WispAiLogic.targetIsStationary(0.1, 0.1, 0.11));
        // nextFloat() < 0.66 -> damage + 1.0
        assertEquals(4.0, WispAiLogic.attackDamage(true, 0.65F, 3.0));
        assertEquals(4.0, WispAiLogic.attackDamage(true, 0.0F, 3.0));
        assertEquals(WispAiLogic.NO_HIT, WispAiLogic.attackDamage(true, 0.66F, 3.0));
        assertEquals(WispAiLogic.NO_HIT, WispAiLogic.attackDamage(true, 0.99F, 3.0));
    }

    @Test
    void movingTargetIsHitOnFortyPercentForPlainDamage() {
        // nextFloat() < 0.4 -> the plain attack-damage attribute, no bonus
        assertEquals(3.0, WispAiLogic.attackDamage(false, 0.39F, 3.0));
        assertEquals(WispAiLogic.NO_HIT, WispAiLogic.attackDamage(false, 0.4F, 3.0));
        assertEquals(WispAiLogic.NO_HIT, WispAiLogic.attackDamage(false, 0.9F, 3.0));
    }

    @Test
    void attackRangeIsSixteenBlocks() {
        assertEquals(16.0, WispAiLogic.ATTACK_RANGE);
        assertEquals(20, WispAiLogic.ATTACK_INTERVAL);
    }

    // ------------------------------------------------------- spawn and light

    @Test
    void lightGateIsSkyRollThenLightRoll() {
        // if (skyLight > rand(32)) return false; return lightLevel <= rand(8)
        assertTrue(WispAiLogic.isValidLightLevel(32, 32, 8, 8), "both rolls at the bound accept");
        assertFalse(WispAiLogic.isValidLightLevel(33, 32, 0, 8), "too bright under open sky");
        assertFalse(WispAiLogic.isValidLightLevel(0, 32, 9, 8), "too bright from block light");
        assertEquals(32, WispAiLogic.SKY_LIGHT_ROLL_BOUND);
        assertEquals(8, WispAiLogic.LIGHT_LEVEL_ROLL_BOUND);
    }

    @Test
    void spawnNeedsRoomDifficultyLightAndTheSuperCheck() {
        assertTrue(WispAiLogic.canSpawn(7, 1, true, true));
        assertFalse(WispAiLogic.canSpawn(8, 1, true, true), "8 nearby wisps is the cap");
        assertFalse(WispAiLogic.canSpawn(0, 0, true, true), "peaceful refuses");
        assertFalse(WispAiLogic.canSpawn(0, 1, false, true), "light refuses");
        assertFalse(WispAiLogic.canSpawn(0, 1, true, false), "the vanilla check refuses");
        assertEquals(8, WispAiLogic.SPAWN_MAX_NEARBY);
        assertEquals(16.0, WispAiLogic.SPAWN_SCAN_RADIUS);
    }

    // ----------------------------------------------------------- aspect pick

    @Test
    void eerieBiomePoolIsTheOriginalSwitchInOrder() {
        // switch (rand.nextInt(6)) case 0..5
        assertEquals("DARKNESS", WispAiLogic.eerieAspectForRoll(0));
        assertEquals("UNDEAD", WispAiLogic.eerieAspectForRoll(1));
        assertEquals("ENTROPY", WispAiLogic.eerieAspectForRoll(2));
        assertEquals("ELDRITCH", WispAiLogic.eerieAspectForRoll(3));
        assertEquals("POISON", WispAiLogic.eerieAspectForRoll(4));
        assertEquals("DEATH", WispAiLogic.eerieAspectForRoll(5));
    }

    @Test
    void nonEeriePickIsPrimalNineTimesInTen() {
        final String[] primal = {"AER", "IGNIS", "TERRA"};
        final String[] compound = {"MOTUS", "VITREUS"};
        // nextInt(10) != 0 -> a primal aspect
        assertEquals("IGNIS", WispAiLogic.pickAspectTag(false, 0, 1, 1, 0, primal, compound));
        assertEquals("AER", WispAiLogic.pickAspectTag(false, 0, 9, 0, 1, primal, compound));
        // nextInt(10) == 0 -> the one-in-ten compound pick
        assertEquals("VITREUS", WispAiLogic.pickAspectTag(false, 0, 0, 0, 1, primal, compound));
        // the eerie branch ignores both rolls and uses the switch
        assertEquals("ENTROPY", WispAiLogic.pickAspectTag(true, 2, 0, 0, 1, primal, compound));
    }
}
