package com.jonnyutah12.thaumcraftclassic.entities.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the original {@code EntityFireBat} (404 lines) rules world-free
 * (TC-M6-06-s-c1) — no level, no entity instance. Every expectation quotes the
 * decompiled line it was read from, so "cleaning up" one of the original's
 * quirks (a goal, a rate, a bound, a bit) turns a test red.
 *
 * <p>The world side — the registry id, the hitbox, the attributes, the empty
 * goal selector, the fire/magic refusal, the save keys and the five flag bits
 * on a live instance — is proven by {@code FirebatMobGameTest}.</p>
 */
class FirebatAiLogicTest {

    // ------------------------------------------------------------- identity

    @Test
    void originalIdentityFigures() {
        // func_70105_a(0.5F, 0.9F) — :30
        assertEquals(0.5F, FirebatAiLogic.WIDTH);
        assertEquals(0.9F, FirebatAiLogic.HEIGHT);
        // field_70178_ae = true — :32
        assertTrue(FirebatAiLogic.FIRE_IMMUNE);
        // func_70013_c(float) -> 1.0F — :45-47 (renders at full brightness)
        assertEquals(1.0F, FirebatAiLogic.BRIGHTNESS);
        // func_70070_b(float) -> 15728880 (0xF03030) — :41-43
        assertEquals(15728880, FirebatAiLogic.TINT_RGB);
        // func_70599_aP() -> 0.1F — :49-51
        assertEquals(0.1F, FirebatAiLogic.SOUND_VOLUME);
        // func_70647_i() -> super * 0.95F — :53-55
        assertEquals(0.95F, FirebatAiLogic.SOUND_PITCH_FACTOR);
        // func_70639_aQ(): while hanging, nextInt(4) != 0 mutes the ambient — :57-59
        assertEquals(4, FirebatAiLogic.AMBIENT_WHILE_HANGING_ROLL);
        // The original plays three vanilla sounds by name — :58, :62, :66
        assertEquals("mob.bat.idle", FirebatAiLogic.SOUND_IDLE);
        assertEquals("mob.bat.hurt", FirebatAiLogic.SOUND_HURT);
        assertEquals("mob.bat.death", FirebatAiLogic.SOUND_DEATH);
        // func_70650_aV() -> false — :150-152 (no boss bar)
        assertFalse(FirebatAiLogic.IS_BOSS);
    }

    // ------------------------------------------------------------- attributes

    @Test
    void attributesMatchFunc_110147_ax() {
        // :75  health = getIsDevil() ? 15.0 : 5.0
        assertEquals(5.0F, FirebatAiLogic.healthFor(false));
        assertEquals(15.0F, FirebatAiLogic.healthFor(true));
        // :76  attack = getIsSummoned() ? (getIsDevil() ? 3 : 2) + damBonus : 1.0
        assertEquals(1.0F, FirebatAiLogic.attackDamageFor(false, false, 0));
        assertEquals(1.0F, FirebatAiLogic.attackDamageFor(false, true, 9),
                "the attack line tests the summon bit first; a non-summoned devil stays at 1.0");
        assertEquals(2.0F, FirebatAiLogic.attackDamageFor(true, false, 0));
        assertEquals(3.0F, FirebatAiLogic.attackDamageFor(true, true, 0));
        assertEquals(4.0F, FirebatAiLogic.attackDamageFor(true, false, 2));
        assertEquals(5.0F, FirebatAiLogic.attackDamageFor(true, true, 2));
        // setIsSummoned:104 / setIsDevil:133 write the same expressions
        assertEquals(3.0F, FirebatAiLogic.attackDamageFor(true, true, 0));
    }

    // ------------------------------------------------------------ flag byte

    @Test
    void theFiveFlagsAreBitsOfOneByte() {
        // :35-38 defines one byte at synched index 16; :79-148 reads and rewrites
        // it with Utils.getBit/setBit/clearBit, five bits in this order.
        assertEquals(0, FirebatAiLogic.FLAG_HANGING);    // getIsBatHanging :79-90
        assertEquals(1, FirebatAiLogic.FLAG_SUMMONED);   // :92-105
        assertEquals(2, FirebatAiLogic.FLAG_EXPLOSIVE);  // :107-118
        assertEquals(3, FirebatAiLogic.FLAG_DEVIL);      // :120-135
        assertEquals(4, FirebatAiLogic.FLAG_VAMPIRE);    // :137-148

        // Utils.getBit / setBit / clearBit
        assertTrue(FirebatAiLogic.getBit(0b10101, 2));
        assertFalse(FirebatAiLogic.getBit(0b10101, 1));
        assertEquals(8, FirebatAiLogic.setBit(0, 3));
        // 0b10111 has bits 0,1,2,4 set, so bit 3 is CLEAR in it: setBit writes it
        // (0b10111 -> 0b11111) and clearBit on a set bit drops it (0b10111 ->
        // 0b10110). The four rows below are the byte as Utils leaves it.
        assertEquals(0b11111, FirebatAiLogic.setBit(0b11111, 3), "setting a set bit is a no-op");
        assertEquals(0b10110, FirebatAiLogic.clearBit(0b10111, 0),
                "clearing bit 0 drops the hanging bit (:79-90)");
        assertEquals(0b01111, FirebatAiLogic.setBit(0b00111, 3),
                "setBit writes bit 3 and leaves the other four (:120-135)");
        assertEquals(0b10111, FirebatAiLogic.clearBit(0b11111, 3),
                "clearBit drops bit 3 and leaves the other four (:137-148)");
    }

    @Test
    void saveKeysAreTheOnesTheOriginalWrites() {
        // func_70014_b:384-385 writes "BatFlags" and "damBonus"; func_70037_a:378-379
        // reads the same two keys. A rename silently drops every explosive/devil/
        // vampire bat an existing save carries.
        assertEquals("BatFlags", FirebatAiLogic.NBT_FLAGS);
        assertEquals("damBonus", FirebatAiLogic.NBT_DAM_BONUS);
    }

    // -------------------------------------------------------------- tick body

    @Test
    void wetBatTakesOneDrowningDamageAndTheOrderIsBeforeSuper() {
        // func_70636_d (:154-160): func_70026_G() (isWet) -> DamageSource.field_76369_e
        // (drown) for 1.0F, and the hurt lands BEFORE super.onUpdate.
        assertEquals(1.0F, FirebatAiLogic.WET_DAMAGE);
        // func_70071_h_ (:188-193): hanging zeroes all three axes and snaps
        // y = floor(posY) + 1 - height; not hanging damps motionY by 0.6.
        assertEquals(0.6F, FirebatAiLogic.GRAVITY_DAMP, 1.0E-6);
        assertEquals(64.1, FirebatAiLogic.hangingY(64.3, 0.9F), 1.0E-5);
        assertEquals(64.1, FirebatAiLogic.hangingY(64.9, 0.9F), 1.0E-5, "floor, not round");
        // Math.floor(-3.5) = -4.0 (floor, not truncate), so the snap lands at
        // -4 + 1 - 0.9 = -3.9.
        assertEquals(-3.9, FirebatAiLogic.hangingY(-3.5, 0.9F), 1.0E-5, "floor floors negatives down");
        assertEquals(-0.3, FirebatAiLogic.flyingMotionY(-0.5), 1.0E-6);
        assertEquals(0.0, FirebatAiLogic.flyingMotionY(0.0), 1.0E-9);
    }

    @Test
    void hangingBodySupportTestAndUnhangDistance() {
        // func_70626_be:226-229 — the support test reads the block ONE above the
        // feet and a failing test fires level event 1015.
        assertEquals(1, FirebatAiLogic.SUPPORT_SCAN_Y_OFFSET);
        assertEquals(1015, FirebatAiLogic.HANGING_SUPPORT_EVENT);
        // :235 — a player found within 4.0 un-hangs the bat (and fires 1015 too).
        assertEquals(4.0F, FirebatAiLogic.UNHANG_PLAYER_DISTANCE);
        // :231-232 — nextInt(200) == 0 writes the head yaw from nextInt(360).
        assertEquals(200, FirebatAiLogic.IDLE_HEAD_YAW_ROLL);
        assertEquals(360, FirebatAiLogic.HEAD_YAW_SPAN);
        assertEquals(123, FirebatAiLogic.idleHeadYaw(0, 123));
        assertEquals(-1, FirebatAiLogic.idleHeadYaw(1, 123), "a failed roll keeps the current yaw");
    }

    // ---------------------------------------------------------------- flight

    @Test
    void flightWaypointAndRepickWindows() {
        // :246-253 — the target is dropped when its block is not air or y < 1.
        assertEquals(1, FirebatAiLogic.FLIGHT_TARGET_MIN_Y);
        assertTrue(FirebatAiLogic.flightTargetInvalid(false, 64), "a non-air waypoint is dropped");
        assertTrue(FirebatAiLogic.flightTargetInvalid(true, 0), "a sub-world y is dropped");
        assertFalse(FirebatAiLogic.flightTargetInvalid(true, 1), "y exactly 1 is kept");
        assertFalse(FirebatAiLogic.flightTargetInvalid(true, 64));

        // :255-257 — re-pick when null, on nextInt(30) == 0, or when the current
        // waypoint is closer than 4.0.
        assertEquals(30, FirebatAiLogic.FLIGHT_TARGET_REPICK_ROLL);
        assertEquals(4.0F, FirebatAiLogic.FLIGHT_TARGET_REPICK_DISTANCE);
        assertTrue(FirebatAiLogic.repicksFlightTarget(true, 29, 999.0));
        assertTrue(FirebatAiLogic.repicksFlightTarget(false, 0, 999.0));
        assertTrue(FirebatAiLogic.repicksFlightTarget(false, 29, 3.999));
        assertFalse(FirebatAiLogic.repicksFlightTarget(false, 29, 4.0), "4.0 is not < 4.0");

        // :258-262 — x/z = (int)pos + nextInt(7) - nextInt(7), y = (int)pos +
        // nextInt(6) - 2, so x/z span -6..6 and y spans -2..3.
        assertEquals(7, FirebatAiLogic.WAYPOINT_AXIS_ROLL);
        assertEquals(6, FirebatAiLogic.WAYPOINT_Y_ROLL);
        assertEquals(2, FirebatAiLogic.WAYPOINT_Y_DROP);
        assertEquals(10, FirebatAiLogic.waypointAxis(10, 0, 0));
        assertEquals(4, FirebatAiLogic.waypointAxis(10, 0, 6), "the widest negative roll");
        assertEquals(16, FirebatAiLogic.waypointAxis(10, 6, 0), "the widest positive roll");
        assertEquals(8, FirebatAiLogic.waypointY(10, 0), "nextInt(6) == 0 drops the waypoint 2");
        assertEquals(13, FirebatAiLogic.waypointY(10, 5));

        // :264-266 — the aim point inside the block is +0.5 on x/z, +0.1 on y.
        assertEquals(0.5, FirebatAiLogic.WAYPOINT_XZ_CENTER);
        assertEquals(0.1, FirebatAiLogic.WAYPOINT_Y_CENTER);
    }

    @Test
    void steeringIsTheOriginalsLerpWithItsThreeBounds() {
        // :267-269 (and verbatim :283-285): motion + (signum(delta) * bound - motion) * 0.1
        assertEquals(0.5F, FirebatAiLogic.STEER_XZ);
        assertEquals(0.7F, FirebatAiLogic.STEER_Y);
        assertEquals(0.1F, FirebatAiLogic.STEER_LERP);
        assertEquals(0.05, FirebatAiLogic.steer(0.0F, 10.0, FirebatAiLogic.STEER_XZ), 1.0E-6);
        assertEquals(0.07, FirebatAiLogic.steer(0.0F, 10.0, FirebatAiLogic.STEER_Y), 1.0E-6);
        assertEquals(0.29, FirebatAiLogic.steer(0.4F, -5.0, FirebatAiLogic.STEER_Y), 1.0E-6);
        assertEquals(0.0, FirebatAiLogic.steer(0.0F, 0.0, FirebatAiLogic.STEER_XZ),
                1.0E-9, "signum(0) == 0, so a dead-centre delta steers nowhere");
        // :273 / :289 — field_70701_bs = 0.5F in both flight branches
        assertEquals(0.5F, FirebatAiLogic.WALK_VALUE);
        // :281-282 — the has-target branch aims at target.posY + getEyeHeight() * 0.66F
        assertEquals(0.66F, FirebatAiLogic.TARGET_EYE_HEIGHT_FACTOR);
    }

    @Test
    void yawComesFromMotionAndWrapsAt180() {
        // :271 — (atan2(motionZ, motionX) * 180 / PI) - 90
        assertEquals(0.0F, FirebatAiLogic.yawFromMotion(0.0, 1.0), 1.0E-5);
        assertEquals(-90.0F, FirebatAiLogic.yawFromMotion(1.0, 0.0), 1.0E-5);
        assertEquals(90.0F, FirebatAiLogic.yawFromMotion(-1.0, 0.0), 1.0E-5);
        // :272 / :288 — MathHelper.func_76142_g (wrapAngleTo180_float)
        assertEquals(-90.0F, FirebatAiLogic.wrapAngleTo180(270.0F), 1.0E-5);
        assertEquals(179.0F, FirebatAiLogic.wrapAngleTo180(-181.0F), 1.0E-5);
        assertEquals(-180.0F, FirebatAiLogic.wrapAngleTo180(180.0F), 1.0E-5, "180 is not < 180");
        assertEquals(90.0F, FirebatAiLogic.yawDelta(90.0F, 0.0F), 1.0E-5);
        assertEquals(90.0F, FirebatAiLogic.yawDelta(-180.0F, 90.0F), 1.0E-5,
                "wrapping -270 gives +90, not -270");
    }

    @Test
    void reHangNeedsTheRollAndTheSky() {
        // :275-279 — nextInt(100) == 0 AND the sky is visible above the feet.
        assertEquals(100, FirebatAiLogic.RE_HANG_ROLL);
        assertTrue(FirebatAiLogic.hangsAgain(0, true));
        assertFalse(FirebatAiLogic.hangsAgain(0, false), "no sky, no hanging");
        assertFalse(FirebatAiLogic.hangsAgain(1, true), "the roll gates the sky test");
    }

    @Test
    void targetIsTheClosestPlayerWithinTwelveAndSummonedBatsHaveNone() {
        // func_70782_k (:371-374): world.func_72856_b(this, 12.0), null while summoned.
        assertEquals(12.0F, FirebatAiLogic.TARGET_DISTANCE);
        // :293-295 — a creative player clears the target.
        assertTrue(FirebatAiLogic.clearsTarget(true, true));
        assertFalse(FirebatAiLogic.clearsTarget(true, false));
        assertFalse(FirebatAiLogic.clearsTarget(false, true), "the branch tests instanceof EntityPlayer first");
        // :241-243 — flying, summoned and targetless: 2.0 generic damage.
        assertEquals(2.0F, FirebatAiLogic.SUMMONED_NO_TARGET_DAMAGE);
    }

    // ------------------------------------------------------------------ melee

    @Test
    void meleeGateIsTheOriginalsFourConditions() {
        // func_70785_a:330-333 — attackTime <= 0, dist < max(2.5F, targetWidth * 1.1F),
        // and the two bounding-box height overlaps.
        assertEquals(20, FirebatAiLogic.ATTACK_COOLDOWN);
        assertEquals(2.5F, FirebatAiLogic.ATTACK_REACH_FLOOR);
        assertEquals(1.1F, FirebatAiLogic.ATTACK_REACH_WIDTH_FACTOR);
        assertEquals(2.5F, FirebatAiLogic.attackReach(2.0F), "the floor wins for a thin target");
        assertEquals(2.75F, FirebatAiLogic.attackReach(2.5F), "a wide target reaches further");

        assertTrue(FirebatAiLogic.canMelee(0, 2.4F, 2.5F, 3.0, 2.0, 2.0, 3.0));
        assertFalse(FirebatAiLogic.canMelee(1, 0.1F, 2.5F, 3.0, 2.0, 2.0, 3.0),
                "the attackTime written at :346 gates the next hit");
        assertFalse(FirebatAiLogic.canMelee(0, 2.75F, 2.5F, 3.0, 2.0, 2.0, 3.0), "2.75 is not < 2.75");
        assertFalse(FirebatAiLogic.canMelee(0, 1.0F, 2.5F, 2.0, 2.0, 2.0, 3.0),
                "target.maxY must sit above the bat's minY");
        assertFalse(FirebatAiLogic.canMelee(0, 1.0F, 2.5F, 3.0, 2.0, 3.0, 2.0),
                "target.minY must sit below the bat's maxY");
    }

    @Test
    void meleeSideEffectsCarryTheOriginalsNumbers() {
        // :334-335 — a summoned hit marks the target recently hit for 100.
        assertEquals(100, FirebatAiLogic.RECENTLY_HIT_TICKS);
        // :338-344 — vampire: owner gets regeneration 26 ticks amplifier 1 when it
        // does not already have it, and the bat heals 1.0.
        assertEquals(26, FirebatAiLogic.VAMPIRE_REGEN_DURATION);
        assertEquals(1, FirebatAiLogic.VAMPIRE_REGEN_AMPLIFIER);
        assertEquals(1.0F, FirebatAiLogic.VAMPIRE_HEAL);
        assertTrue(FirebatAiLogic.appliesVampireRegen(true, false));
        assertFalse(FirebatAiLogic.appliesVampireRegen(true, true), "the original checks hasPotionEffect");
        assertFalse(FirebatAiLogic.appliesVampireRegen(false, false), "no owner, no effect");
        // :346 — attackTime = 20 on a landed hit (EntityLivingBase decrements it
        // once per living update).
        assertEquals(20, FirebatAiLogic.ATTACK_COOLDOWN);
        // :367-368 — mob.bat.hurt at volume 0.5, pitch 0.9 + nextFloat() * 0.2
        assertEquals(0.5F, FirebatAiLogic.ATTACK_SOUND_VOLUME);
        assertEquals(0.9F, FirebatAiLogic.attackSoundPitch(0.0F), 1.0E-6);
        assertEquals(1.1F, FirebatAiLogic.attackSoundPitch(1.0F), 1.0E-6);
    }

    @Test
    void explosionBranchMatchesTheOriginalRollAndRadius() {
        // :347 — (explosive || level.random.nextInt(10) == 0) && !isClientSide && !devil
        assertEquals(10, FirebatAiLogic.EXPLOSION_ROLL);
        assertTrue(FirebatAiLogic.blowsUp(true, 9, false, false));
        assertTrue(FirebatAiLogic.blowsUp(false, 0, false, false));
        assertFalse(FirebatAiLogic.blowsUp(false, 1, false, false), "the roll gates a plain bat");
        assertFalse(FirebatAiLogic.blowsUp(true, 9, true, false), "a devil never blows up");
        assertFalse(FirebatAiLogic.blowsUp(true, 9, false, true), "the explosion is server-side only");
        // :351 — radius 1.5F + (explosive ? damBonus * 0.33F : 0F), isFlaming false,
        // isBreakingBlocks false
        assertEquals(1.5F, FirebatAiLogic.EXPLOSION_BASE);
        assertEquals(0.33F, FirebatAiLogic.EXPLOSION_DAM_BONUS_FACTOR);
        assertEquals(1.5, FirebatAiLogic.explosionRadius(false, 9), 1.0E-6, "a plain bat is 1.5");
        assertEquals(2.49, FirebatAiLogic.explosionRadius(true, 3), 1.0E-5);
        assertEquals(1.5, FirebatAiLogic.explosionRadius(true, 0), 1.0E-6);
    }

    @Test
    void exhaustionBranchIsTheCoinFlipTheOriginalFlips() {
        // :354-355 — !vampire && !random.nextBoolean(): exhausted by 4 summoned, 2 otherwise.
        assertEquals(4, FirebatAiLogic.EXHAUSTION_SUMMONED);
        assertEquals(2, FirebatAiLogic.EXHAUSTION_NORMAL);
        assertTrue(FirebatAiLogic.usesExhaustion(false, false));
        assertFalse(FirebatAiLogic.usesExhaustion(true, false), "a vampire bat skips exhaustion");
        assertFalse(FirebatAiLogic.usesExhaustion(false, true), "the coin flip gates the branch");
        assertEquals(4, FirebatAiLogic.exhaustionFor(true));
        assertEquals(2, FirebatAiLogic.exhaustionFor(false));
    }

    // ----------------------------------------------------------------- damage

    @Test
    void fireAndMagicDamageAreRefused() {
        // func_70097_a:317-327 — refused while the bat is dead (func_85032_ar()),
        // from fire (func_76347_k) and from magic (func_94541_c).
        assertTrue(FirebatAiLogic.acceptsDamage(false, false, false));
        assertFalse(FirebatAiLogic.acceptsDamage(true, false, false), "a dead bat takes nothing");
        assertFalse(FirebatAiLogic.acceptsDamage(false, true, false), "fire is refused");
        assertFalse(FirebatAiLogic.acceptsDamage(false, false, true), "magic is refused");
        assertFalse(FirebatAiLogic.acceptsDamage(false, true, true));
    }

    // ------------------------------------------------------------------ spawn

    @Test
    void spawnLightGateIsTheOriginalsRoll() {
        // func_70601_bi:388-395 — light at the box's minY > nextInt(7) refuses.
        assertEquals(7, FirebatAiLogic.SPAWN_LIGHT_ROLL);
        assertTrue(FirebatAiLogic.canSpawnAtLight(0, 0));
        assertTrue(FirebatAiLogic.canSpawnAtLight(7, 7), "7 is not > 7");
        assertFalse(FirebatAiLogic.canSpawnAtLight(7, 6), "7 > 6 refuses the spawn");
        // :389 reads the light at floor_double(boundingBox.minY) — the 1.7.10
        // floor convention, not an (int) cast.
        assertEquals(64, FirebatAiLogic.spawnLightY(64.7));
        assertEquals(-4, FirebatAiLogic.spawnLightY(-3.2), "floor, not truncate");
    }

    @Test
    void theDropIsGunpowderOnlyForANonSummonedBat() {
        // func_146068_u:397-399 — Items.field_151016_H (gunpowder) when not
        // summoned, Item.func_150899_d(0) (id 0 = nothing) when summoned.
        // 1.7.10's vanilla dropAllLoot roll is NOT in this repo's reference tree,
        // so the port ships no drop body — the identity and the rule are pinned
        // here and the roll is a filed follow-up (see the card's
        // left_out_of_scope).
        assertEquals("minecraft:gunpowder", FirebatAiLogic.DROP_ITEM_ID);
        assertTrue(FirebatAiLogic.dropsItsItem(false));
        assertFalse(FirebatAiLogic.dropsItsItem(true), "a summoned bat's drop item is id 0 — nothing");
    }

    // ------------------------------------------------ documented non-shapes

    @Test
    void documentedBooleansAreTheOriginals() {
        // func_70104_M (:69-71) and func_70041_e_ (:303-305) both false -> isPushable()
        assertFalse(FirebatAiLogic.IS_PUSHABLE);
        // func_70814_o (:401-403) is TRUE: the goal system is on in 1.7.10 and the
        // class installs nothing, so the port registers an empty goal selector and
        // drives the target/melee itself from customServerAiStep.
        assertTrue(FirebatAiLogic.IS_AI_ENABLED);
        // func_145773_az (:313-315) is true (1.7.10 isNoAi) and is deliberately NOT
        // wired: 1.21.1's NoAi flag flips isEffectiveAi() and gates travel(), which
        // would switch off the hand-written flight motion this card is proving.
        assertTrue(FirebatAiLogic.ORIGINAL_NO_AI);
        // func_70064_a(double,boolean) empty body (:310-311) -> no fall damage
        assertTrue(FirebatAiLogic.NO_FALL_DAMAGE);
        // func_70069_a(float) empty body (:307-308) -> silent footsteps
        assertTrue(FirebatAiLogic.SILENT_FOOTSTEPS);
    }
}
