package com.jonnyutah12.thaumcraftclassic.entities.monster.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Crimson Portal world-free (TC-M6-06-s-c5-2) — no level, no entity
 * instance, no registry. Every expectation quotes the decompiled line it was read
 * from in
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/entities/monster/boss/EntityCultistPortal.java}
 * (313 lines), so "cleaning up" one of the original's figures — the 64/20/false
 * registration triple, the 1.5 x 3.0 box, the zero eye height, the 200-tick
 * countdown, the 160 banner value, the 13-period crate gate, the 0.33F knight roll,
 * the 32-block scan, the 15/60/210 counter formulas, the 3.0 contact square, the
 * 540 air, the 250-block drop lift, the 2.0F explosion — turns a test red. The
 * shipped wiring is proven by {@code BossMobGameTest}.
 */
class CultistPortalAiLogicTest {

    private static final float F = 1.0E-4F;

    // ------------------------------------------------------- registration figures

    @Test
    void theRegistrationRowIsTheOriginals() {
        // addMob(EntityCultistPortal.class, "EntityCultistPortal", "Crimson Portal",
        // 64, 20, false, …) — ConfigEntities.java:117. The 20 and the false are the
        // row that most often gets "helped" to the family's 3/true pair.
        assertEquals(64, CultistPortalAiLogic.TRACK_RANGE_BLOCKS);
        assertEquals(4, CultistPortalAiLogic.TRACK_RANGE_CHUNKS, "64 blocks = 4 chunks");
        assertEquals(20, CultistPortalAiLogic.UPDATE_INTERVAL);
        assertFalse(CultistPortalAiLogic.VELOCITY_UPDATES, ":117 — the false argument");
        assertEquals(1.5F, CultistPortalAiLogic.WIDTH, ":40 — func_70105_a(1.5F, 3.0F)");
        assertEquals(3.0F, CultistPortalAiLogic.HEIGHT, ":40");
        assertEquals(0.0F, CultistPortalAiLogic.EYE_HEIGHT, ":68-70 — func_70053_R() = 0.0F");
        assertTrue(CultistPortalAiLogic.FIRE_IMMUNE, ":38 — field_70178_ae = true");
        assertEquals(30, CultistPortalAiLogic.XP_REWARD, ":39 — field_70728_aV = 30");
    }

    @Test
    void theAttributeSetIsTheOriginalsAndCarriesNoMovementSpeed() {
        // func_110147_ax (:61-66) registers exactly three attributes: 500.0 health,
        // 0.0 attack, 1.0 knockback resistance. Armour 5 rides func_70658_aO (:43-45).
        assertEquals(500.0, CultistPortalAiLogic.MAX_HEALTH, ":63");
        assertEquals(0.0, CultistPortalAiLogic.ATTACK_DAMAGE, ":64 — the statue never swings");
        assertEquals(1.0, CultistPortalAiLogic.KNOCKBACK_RESISTANCE, ":65");
        assertEquals(5.0, CultistPortalAiLogic.ARMOR, ":43-45 — func_70658_aO() = 5");
    }

    @Test
    void thePinnedFiguresAreTheOriginals() {
        assertEquals(540, CultistPortalAiLogic.MAX_AIR, ":267-269 — func_70627_aG() = 540");
        assertEquals(0.75F, CultistPortalAiLogic.SOUND_VOLUME, ":263-265 — func_70599_aP() = 0.75F");
        assertEquals(15728880, CultistPortalAiLogic.BRIGHTNESS_FOR_RENDER, ":91-93");
        assertEquals(1.0F, CultistPortalAiLogic.BRIGHTNESS, ":95-97 — func_70013_c() = 1.0F");
        assertEquals(4096.0, CultistPortalAiLogic.RENDER_RANGE, ":87-89 — func_70112_a(p) = p < 4096.0");
        assertEquals(0, CultistPortalAiLogic.PICKUP_ITEM_ID, ":283-285 — Item.func_150899_d(0)");
    }

    @Test
    void theBodyIsStaticCollidableSilentAndStill() {
        assertTrue(CultistPortalAiLogic.CAN_BE_COLLIDED_WITH, ":72-74 — func_70067_L() = true");
        assertFalse(CultistPortalAiLogic.PUSHABLE, ":76-78 — func_70104_M() = false");
        assertTrue(CultistPortalAiLogic.NEVER_MOVES, ":80-81 — func_70091_d is an EMPTY override");
        assertTrue(CultistPortalAiLogic.CUSTOM_AI_STEP_EMPTY,
                ":83-84 — func_70626_be() is an EMPTY override: the goal selector has no ticker");
        assertFalse(CultistPortalAiLogic.DRAWS_EFFECT_PARTICLES,
                ":301-302 — func_70690_d is an EMPTY override (a VISUAL hook, not an immunity)");
        assertFalse(CultistPortalAiLogic.PLAYS_FOOTSTEPS, ":304-305 — func_70069_a is an EMPTY override");
    }

    // -------------------------------------------------------------- the fields

    @Test
    void theFieldInitialsAreTheOriginals() {
        assertEquals(0, CultistPortalAiLogic.STAGE_INITIAL, ":32 — int stage = 0");
        assertEquals(200, CultistPortalAiLogic.COUNTER_INITIAL, ":33 — int stagecounter = 200");
        assertEquals(0, CultistPortalAiLogic.PULSE_INITIAL, ":34 — public int pulse = 0");
        assertEquals(10, CultistPortalAiLogic.PULSE_TICKS, ":294 — pulse = 10 on the status byte");
        assertEquals(16, CultistPortalAiLogic.STATUS_PULSE, ":104, :138, :189 — (byte)16");
        assertEquals("stage", CultistPortalAiLogic.TAG_STAGE, ":53 — tag.putInt(\"stage\", stage)");
    }

    @Test
    void theCountersAreTheOriginalsAndTheStageHasNoWrap() {
        // The reference's machine has no cap and no wrap: :131 is a bare stage++, so
        // the stages run 0,1,2,… forever and the :121 default arm keeps summoning
        // minions, while :238 starts draining the portal.
        assertEquals(1, CultistPortalAiLogic.stageAfterFiring(0), ":131 — stage++");
        assertEquals(13, CultistPortalAiLogic.stageAfterFiring(12), ":131 — no cap at the boss stage");
        assertEquals(41, CultistPortalAiLogic.stageAfterFiring(40), ":131 — no wrap");
        assertTrue(CultistPortalAiLogic.STAGE_HAS_NO_WRAP);
        assertFalse(CultistPortalAiLogic.fires(1), ":102 — stagecounter <= 0");
        assertTrue(CultistPortalAiLogic.fires(0), ":102");
        assertTrue(CultistPortalAiLogic.fires(-1), ":102 — the arm fires on any non-positive value");
    }

    // ------------------------------------------------------- the firing decision

    @Test
    void theStagePicksTheArmTheOriginalSwitchPicks() {
        // switch (stage) — :105-129. Cases 0-4 (:106-110) and the default arm from 5
        // (:121-124) both call spawnMinions(); case 12 (:126-128) is the only
        // spawnBoss(); stage 13+ falls to the default and summons minions again.
        for (int stage = 0; stage <= 11; stage++) {
            assertEquals(CultistPortalAiLogic.Summon.MINIONS, CultistPortalAiLogic.actionFor(stage),
                    "stage " + stage + " summons the wave");
        }
        assertEquals(CultistPortalAiLogic.Summon.BOSS, CultistPortalAiLogic.actionFor(12), ":126-128");
        assertEquals(CultistPortalAiLogic.Summon.MINIONS, CultistPortalAiLogic.actionFor(13), ":121");
        assertEquals(CultistPortalAiLogic.Summon.MINIONS, CultistPortalAiLogic.actionFor(50), ":121");
        assertEquals(CultistPortalAiLogic.Summon.NONE, CultistPortalAiLogic.actionFor(-1),
                ":105 — the switch has no case below 0");
    }

    @Test
    void theShortCounterArmCoversExactlyStagesZeroToFour() {
        assertTrue(CultistPortalAiLogic.isShortCounterStage(0), ":106-110");
        assertTrue(CultistPortalAiLogic.isShortCounterStage(4), ":110");
        assertFalse(CultistPortalAiLogic.isShortCounterStage(5), ":114 — case 5 leaves the arm");
        assertFalse(CultistPortalAiLogic.isShortCounterStage(-1));
    }

    @Test
    void everyShortCounterValueIsTheFormulasInterval() {
        // :111 — 15 + nextInt(10 - stage) - stage. The drawn value is a parameter, so
        // the whole interval is enumerable: stage 0 gives 15..24, stage 4 gives 15..16.
        for (int stage = 0; stage <= 4; stage++) {
            final int spread = CultistPortalAiLogic.SHORT_COUNTER_SPREAD - stage;
            for (int drawn = 0; drawn < spread; drawn++) {
                assertEquals(15 + drawn - stage,
                        CultistPortalAiLogic.shortCounterFor(stage, drawn),
                        ":111 stage=" + stage + " drawn=" + drawn);
            }
            assertEquals(15 - stage + spread - 1,
                    CultistPortalAiLogic.shortCounterFor(stage, spread - 1));
        }
        assertEquals(24, CultistPortalAiLogic.shortCounterFor(0, 9), ":111 — the top of stage 0");
        assertEquals(16, CultistPortalAiLogic.shortCounterFor(4, 5), ":111 — the top of stage 4");
    }

    @Test
    void theTimedCounterUsesIntegerDivisionOnTheTiming() {
        // :122-123 — int t = getTiming(); stagecounter = t + nextInt(5 + t / 3).
        assertEquals(0, CultistPortalAiLogic.timedCounterFor(0, 0), ":123 — an empty scan fires fast");
        assertEquals(4, CultistPortalAiLogic.timedCounterFor(0, 4), ":123");
        assertEquals(5, CultistPortalAiLogic.timedCounterRollSpread(0), ":123 — 5 + 0/3");
        assertEquals(6, CultistPortalAiLogic.timedCounterRollSpread(5), ":123 — 5 + 5/3, integer");
        assertEquals(25, CultistPortalAiLogic.timedCounterRollSpread(60), ":123 — 5 + 60/3");
        assertEquals(84, CultistPortalAiLogic.timedCounterFor(60, 24), ":123 — 60 + 24");
    }

    @Test
    void theBossAndNoPlayerCountersAreTheOriginals() {
        // :127 — 50 + getTiming() * 2 + nextInt(50); :133 — 30 + nextInt(30).
        assertEquals(50, CultistPortalAiLogic.bossCounterFor(0, 0), ":127");
        assertEquals(219, CultistPortalAiLogic.bossCounterFor(60, 49), ":127 — 50 + 120 + 49");
        assertEquals(50, CultistPortalAiLogic.BOSS_COUNTER_ROLL, ":127 — nextInt(50)");
        assertEquals(30, CultistPortalAiLogic.noPlayerCounterFor(0), ":133");
        assertEquals(59, CultistPortalAiLogic.noPlayerCounterFor(29), ":133");
        assertEquals(30, CultistPortalAiLogic.NO_PLAYER_COUNTER_ROLL, ":133 — nextInt(30)");
    }

    @Test
    void theTimingIsTheScanSizeTimesTwenty() {
        assertEquals(0, CultistPortalAiLogic.timingFor(0), ":217 — an empty scan is 0, and 0 is used");
        assertEquals(20, CultistPortalAiLogic.timingFor(1), ":217 — l.size() * 20");
        assertEquals(140, CultistPortalAiLogic.timingFor(7), ":217");
        assertEquals(32.0, CultistPortalAiLogic.SUMMON_SCAN_RADIUS, ":215 — the 32.0 cube");
        assertEquals(48.0, CultistPortalAiLogic.PLAYER_SCAN, ":103 — func_72890_a(this, 48.0)");
    }

    // ---------------------------------------------------------------- the legs

    @Test
    void theBannerLegFiresOnTheOneValueInStageZero() {
        assertTrue(CultistPortalAiLogic.bannerGate(160, 0), ":137 — stagecounter == 160 && stage == 0");
        assertFalse(CultistPortalAiLogic.bannerGate(159, 0), ":137");
        assertFalse(CultistPortalAiLogic.bannerGate(161, 0), ":137");
        assertFalse(CultistPortalAiLogic.bannerGate(160, 1), ":137 — the wall is stage 0 only");
        assertEquals(160, CultistPortalAiLogic.BANNER_COUNTER, ":137");
        assertEquals(0, CultistPortalAiLogic.BANNER_STAGE, ":137");
    }

    @Test
    void theBannerRingIsTheDirectionsOfTheLoop() {
        // :140-149 — for (a = 2; a < 6; a++) with ForgeDirection.getOrientation(a) and
        // the cell (int)x - dir.offsetX * 6, y, (int)z + dir.offsetZ * 6.
        assertEquals(2, CultistPortalAiLogic.BANNER_DIRECTION_FROM, ":140");
        assertEquals(6, CultistPortalAiLogic.BANNER_DIRECTION_TO, ":140");
        assertEquals(6, CultistPortalAiLogic.BANNER_OFFSET, ":144 — dir.offsetX * 6");
        assertEquals(8, CultistPortalAiLogic.BANNER_VARIANT, ":146-148 — meta 8, flags 3");
        assertEquals(0, CultistPortalAiLogic.bannerOffsetX(2), ":141 — NORTH");
        assertEquals(-1, CultistPortalAiLogic.bannerOffsetX(4), ":141 — WEST");
        assertEquals(1, CultistPortalAiLogic.bannerOffsetX(5), ":141 — EAST");
        assertEquals(-1, CultistPortalAiLogic.bannerOffsetZ(2), ":141 — NORTH");
        assertEquals(1, CultistPortalAiLogic.bannerOffsetZ(3), ":141 — SOUTH");
        // :155-167 — the facing switch.
        assertEquals(8, CultistPortalAiLogic.bannerFacing(2), ":157");
        assertEquals(0, CultistPortalAiLogic.bannerFacing(3), ":160");
        assertEquals(12, CultistPortalAiLogic.bannerFacing(4), ":163");
        assertEquals(4, CultistPortalAiLogic.bannerFacing(5), ":166");
        assertEquals(0, CultistPortalAiLogic.bannerFacing(9), ":154 — int face = 0, no case");
        assertEquals("8 0 12 4", java.util.Arrays.stream(
                CultistPortalAiLogic.bannerFacingsInLoopOrder())
                .mapToObj(String::valueOf).reduce((a, b) -> a + " " + b).orElse(""));
    }

    @Test
    void theCrateLegIsEveryThirteenthTickInsideTheWindow() {
        // :185 — stagecounter > 20 && stagecounter < 150 && stage == 0 && % 13 == 0.
        final int[] fires = {143, 130, 117, 104, 91, 78, 65, 52, 39, 26};
        for (final int counter : fires) {
            assertTrue(CultistPortalAiLogic.crateGate(counter, 0), ":185 — " + counter);
        }
        assertFalse(CultistPortalAiLogic.crateGate(150, 0), ":185 — the window is exclusive");
        assertFalse(CultistPortalAiLogic.crateGate(156, 0), ":185 — 13 divides it, the window does not");
        assertFalse(CultistPortalAiLogic.crateGate(26, 1), ":185 — stage 0 only");
        assertFalse(CultistPortalAiLogic.crateGate(13, 0), ":185 — below the window");
        // The whole sweep, counted: from the 200 start the leg lands exactly ten times.
        int hits = 0;
        for (int counter = 1; counter <= 200; counter++) {
            if (CultistPortalAiLogic.crateGate(counter, 0)) {
                hits++;
            }
        }
        assertEquals(10, hits, "the crate census of one 200→0 countdown");
    }

    @Test
    void theCrateRarityAndCellFollowTheOriginals() {
        // :190-191 — rr from world.rand, md = rr < 0.05F ? 2 : (rr < 0.2F ? 1 : 0).
        assertEquals(2, CultistPortalAiLogic.crateMetaFor(0.0F), ":191");
        assertEquals(2, CultistPortalAiLogic.crateMetaFor(0.049F), ":191");
        assertEquals(1, CultistPortalAiLogic.crateMetaFor(0.05F), ":191 — the bound is strict");
        assertEquals(1, CultistPortalAiLogic.crateMetaFor(0.199F), ":191");
        assertEquals(0, CultistPortalAiLogic.crateMetaFor(0.2F), ":191");
        assertEquals(0, CultistPortalAiLogic.crateMetaFor(0.999F), ":191");
        // :186-188 — nextInt(5) - nextInt(5) on x and z, both off the own column, cell filled.
        assertEquals(5, CultistPortalAiLogic.CRATE_SPREAD, ":186");
        assertTrue(CultistPortalAiLogic.crateCellValid(1, 0, 1, 0, true), ":188");
        assertFalse(CultistPortalAiLogic.crateCellValid(0, 0, 1, 0, true), ":188 — same x column");
        assertFalse(CultistPortalAiLogic.crateCellValid(1, 0, 0, 0, true), ":188 — same z column");
        assertFalse(CultistPortalAiLogic.crateCellValid(1, 0, 1, 0, false), ":188 — an air cell is skipped");
        assertEquals(0, CultistPortalAiLogic.CRATE_Y_OFFSET, ":192 — the crate lands on (int)y");
    }

    @Test
    void theHealCoversStagesUnderTwelveAndTheSelfDamageOnlyAboveIt() {
        for (int stage = 0; stage <= 11; stage++) {
            assertTrue(CultistPortalAiLogic.healsForStage(stage), ":203 — stage < 12");
        }
        assertFalse(CultistPortalAiLogic.healsForStage(12), ":203");
        assertFalse(CultistPortalAiLogic.healsForStage(30), ":203");
        assertEquals(1.0F, CultistPortalAiLogic.HEAL_AMOUNT, ":204 — func_70691_i(1.0F)");
        assertFalse(CultistPortalAiLogic.damagesSelf(12), ":238 — stage > 12");
        assertTrue(CultistPortalAiLogic.damagesSelf(13), ":238");
        assertEquals(5.0F, CultistPortalAiLogic.selfDamageFor(0), ":239 — 5 + nextInt(5)");
        assertEquals(9.0F, CultistPortalAiLogic.selfDamageFor(4), ":239");
        assertEquals(5, CultistPortalAiLogic.SELF_DAMAGE_ROLL, ":239 — nextInt(5)");
    }

    @Test
    void theCountdownTickPinsTheDecrementBeforeTheLegs() {
        // :136-137 — the decrement runs first, so the banner leg sees the value it
        // decremented TO, which is why the gate is == 160 and not >= 160.
        final CultistPortalAiLogic.CountdownTick fromStart =
                CultistPortalAiLogic.countdownTick(200, 0);
        assertEquals(199, fromStart.stageCounter(), ":136");
        assertFalse(fromStart.bannerLeg(), ":137");
        assertFalse(fromStart.crateLeg(), ":185");
        assertTrue(fromStart.heals(), ":203");
        final CultistPortalAiLogic.CountdownTick fromBanner =
                CultistPortalAiLogic.countdownTick(161, 0);
        assertEquals(160, fromBanner.stageCounter(), ":136");
        assertTrue(fromBanner.bannerLeg(), ":137 — the leg fires on the decremented value");
        final CultistPortalAiLogic.CountdownTick fromCrate =
                CultistPortalAiLogic.countdownTick(144, 0);
        assertTrue(fromCrate.crateLeg(), ":185 — 143 is the first crate of the sweep");
        assertFalse(CultistPortalAiLogic.countdownTick(145, 0).crateLeg(), ":185 — 144 is not");
        assertFalse(CultistPortalAiLogic.countdownTick(100, 12).heals(), ":203 — stage 12 stops healing");
    }

    // ------------------------------------------------------------- the summons

    @Test
    void theKnightRollIsStrictAndThePlacementIsTheOriginals() {
        assertTrue(CultistPortalAiLogic.summonsKnight(0.34F), ":222 — nextFloat() > 0.33");
        assertFalse(CultistPortalAiLogic.summonsKnight(0.33F), ":222 — the bound is strict");
        assertFalse(CultistPortalAiLogic.summonsKnight(0.0F), ":222");
        assertTrue(CultistPortalAiLogic.summonsKnight(1.0F), ":222");
        assertEquals(0.33F, CultistPortalAiLogic.MINION_KNIGHT_ROLL, ":222");
        assertEquals(0.25, CultistPortalAiLogic.MINION_Y_OFFSET, ":230 — (int)y + 0.25");
        assertEquals(32, CultistPortalAiLogic.MINION_HOME_RADIUS, ":235, :251 — func_110171_b(…, 32)");
        assertEquals("thaumcraft:cultistknight", CultistPortalAiLogic.CULTIST_KNIGHT.toString(), ":223");
        assertEquals("thaumcraft:cultistcleric", CultistPortalAiLogic.CULTIST_CLERIC.toString(), ":225");
        assertEquals("thaumcraft:cultistleader", CultistPortalAiLogic.CULTIST_LEADER.toString(), ":244");
    }

    @Test
    void theContactAndDeathFiguresAreTheOriginals() {
        assertTrue(CultistPortalAiLogic.contactHits(2.9), ":258 — func_70068_e(p) < 3.0");
        assertFalse(CultistPortalAiLogic.contactHits(3.0), ":258 — the bound is strict");
        assertEquals(3.0, CultistPortalAiLogic.CONTACT_RANGE_SQ, ":258");
        assertEquals(8.0F, CultistPortalAiLogic.CONTACT_DAMAGE, ":258 — causeMobDamage, 8.0F");
        assertEquals(1.0F, CultistPortalAiLogic.contactPitchFor(0.0, 0.0), F, ":259");
        assertEquals(1.1F, CultistPortalAiLogic.contactPitchFor(1.0, 0.0), F, ":259");
        assertEquals(0.9F, CultistPortalAiLogic.contactPitchFor(0.0, 1.0), F, ":259");
        assertEquals(1.0F, CultistPortalAiLogic.CONTACT_VOLUME, ":198, :237, :254, :259");
        assertEquals(2.0F, CultistPortalAiLogic.DEATH_EXPLOSION_POWER, ":309 — 2.0F");
        assertFalse(CultistPortalAiLogic.DEATH_EXPLOSION_FIRE, ":309 — isFlaming false");
        assertFalse(CultistPortalAiLogic.DEATH_EXPLOSION_BREAKS_BLOCKS, ":309 — isBreaking false");
    }

    @Test
    void theDropIsTheRegisteredEldritchObjectAtHalfTheHealthPool() {
        // :287-288 — entityDropSpecialItem(this, new ItemStack(itemEldritchObject, 1, 3),
        // field_70131_O / 2.0F): a 500 HP statue throws the drop 250 blocks up.
        assertEquals(3, CultistPortalAiLogic.ELDRITCH_OBJECT_META, ":288");
        assertEquals("thaumcraft:itemeldritchobject",
                CultistPortalAiLogic.ITEM_ELDRITCH_OBJECT.toString(), ":288");
        assertEquals(250.0, CultistPortalAiLogic.dropLiftFor(500.0), ":288 — field_70131_O / 2.0F");
        assertEquals(2.0, CultistPortalAiLogic.DROP_LIFT_DIVISOR, ":288");
    }

    @Test
    void theWorldTargetsAreTheRegisteredOnes() {
        assertEquals("thaumcraft:blockwoodendevice",
                CultistPortalAiLogic.BLOCK_WOODEN_DEVICE.toString(),
                ":142-148 — blockWoodenDevice meta 8, the port's registered id");
        assertEquals("thaumcraft:blocklootcrate",
                CultistPortalAiLogic.BLOCK_LOOT_CRATE.toString(),
                ":192 — blockLootCrate md, the port's registered id");
        assertEquals("thaumcraft:wandfail", CultistPortalAiLogic.SOUND_WANDFAIL, ":198, :237, :254");
        assertEquals("thaumcraft:monolith", CultistPortalAiLogic.SOUND_AMBIENT, ":271-273");
        assertEquals("thaumcraft:zap", CultistPortalAiLogic.SOUND_ZAP, ":259, :275-277");
        assertEquals("thaumcraft:shock", CultistPortalAiLogic.SOUND_DEATH, ":279-281");
        assertEquals(32.0, CultistPortalAiLogic.FX_BROADCAST_RADIUS, ":196 — TargetPoint(…, 32.0)");
    }

    @Test
    void theBossBarSeamIsThePortedOne() {
        // :31 — IBossDisplayData has no 1.21.1 counterpart (the taintacle giant's
        // resolution), so the bar is a ServerBossEvent named by the mob's own key.
        assertEquals("entity.thaumcraft.cultistportal", CultistPortalAiLogic.BOSS_BAR_NAME_KEY);
        assertEquals("entity.thaumcraft.cultistportal", CultistPortalAiLogic.LANG_KEY);
        assertEquals(1.0F, CultistPortalAiLogic.bossHealthProgress(500.0F, 500.0F), F);
        assertEquals(0.5F, CultistPortalAiLogic.bossHealthProgress(250.0F, 500.0F), F);
        assertEquals(0.0F, CultistPortalAiLogic.bossHealthProgress(100.0F, 0.0F), F,
                "a zero pool draws an empty bar, never a NaN");
    }
}
