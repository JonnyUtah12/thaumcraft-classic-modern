package com.jonnyutah12.thaumcraftclassic.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the warp-event rules world-free (t_1a83f0db). Every expectation quotes
 * the decompiled line it was read from, in {@code thaumcraft.common.lib.
 * WarpEvents} (368 lines, {@code reference/decompiled/thaumcraft/thaumcraft/
 * common/lib/WarpEvents.java}) and its call site
 * {@code common/lib/events/EventHandlerEntity.java:235-237}.
 *
 * <p>The in-world half (the seam fires, a mob spawns, the biome scan stays
 * zero) is {@code gametest/WarpEventsGameTest}.</p>
 */
class WarpEventsLogicTest {

    @Test
    void theCadenceIsEvery2000thTickFromOne() {
        // EventHandlerEntity.java:235 — field_70173_aa > 0 && field_70173_aa % 2000 == 0
        assertEquals(2000, WarpEventsLogic.TICK_PERIOD);
        assertFalse(WarpEventsLogic.tickFires(0));
        assertFalse(WarpEventsLogic.tickFires(1));
        assertFalse(WarpEventsLogic.tickFires(1999));
        assertTrue(WarpEventsLogic.tickFires(2000));
        assertTrue(WarpEventsLogic.tickFires(4000));
        assertFalse(WarpEventsLogic.tickFires(2001));
    }

    @Test
    void theGateNeedsCounterWarpAndRollUnderTheSqrtOfTheCounter() {
        // :45-46 — nextInt(100); warpCounter > 0 && warp > 0 && r <= sqrt(warpCounter)
        assertEquals(100, WarpEventsLogic.ROLL_BOUND);
        assertFalse(WarpEventsLogic.gatePasses(10, 0, 0));    // counter 0 never fires
        assertFalse(WarpEventsLogic.gatePasses(0, 30, 0));    // warp 0 never fires
        assertTrue(WarpEventsLogic.gatePasses(10, 30, 5));    // 5 <= sqrt(30) = 5.477
        assertFalse(WarpEventsLogic.gatePasses(10, 30, 6));   // 6 > 5.477
        assertTrue(WarpEventsLogic.gatePasses(10, 25, 5));    // sqrt(25) = 5, <= passes
        assertFalse(WarpEventsLogic.gatePasses(10, 25, 6));
    }

    @Test
    void theEventWarpIsTheDoubledThirdWithCap100() {
        // :47 — warp = min(100, (warp + warp + warpCounter) / 3), int division
        assertEquals(70, WarpEventsLogic.eventWarp(90, 30));    // (90+90+30)/3 = 70
        assertEquals(90, WarpEventsLogic.eventWarp(120, 30));   // (120+120+30)/3 = 90 — the real-band probe (90 perm + 0 temp + 30 sticky)
        assertEquals(93, WarpEventsLogic.eventWarp(125, 30));   // (125+125+30)/3 = 93 — the illusion probe (90 + 5 temp + 30)
        assertEquals(100, WarpEventsLogic.eventWarp(100, 100)); // (100+100+100)/3 = 100, capped
        assertEquals(3, WarpEventsLogic.eventWarp(3, 3));       // 9/3 = 3
        assertEquals(8, WarpEventsLogic.eventWarp(10, 6));      // 26/3 = 8 (truncating)
        assertEquals(9, WarpEventsLogic.eventWarp(10, 7));      // 27/3 = 9
    }

    @Test
    void theCounterDecaysByAtLeastFivePerPass() {
        // :48 — warpCounter = (int)(warpCounter - max(5.0, sqrt(warpCounter) * 2.0))
        assertEquals(19, WarpEventsLogic.decayedCounter(30));  // 30 - 10.954 = 19.04 -> 19
        assertEquals(0, WarpEventsLogic.decayedCounter(5));    // 5 - max(5, 4.47) = 0
        assertEquals(-4, WarpEventsLogic.decayedCounter(1));   // 1 - 5 = -4 (the gate then rests)
        assertEquals(80, WarpEventsLogic.decayedCounter(100)); // 100 - 20
        assertEquals(15, WarpEventsLogic.decayedCounter(26));  // 26 - 10.198 = 15.80 -> 15
    }

    @Test
    void theFortressHelmDiscountsTwoPlusARollOfFour() {
        // :57 — eff -= 2 + rand.nextInt(4)
        assertEquals(2, WarpEventsLogic.fortressHelmDiscount(0));
        assertEquals(3, WarpEventsLogic.fortressHelmDiscount(1));
        assertEquals(5, WarpEventsLogic.fortressHelmDiscount(3));
    }

    @Test
    void everyLadderBandBoundaryMatchesTheChain() {
        // :61-199 — the branch edges of the original's chain, both directions.
        assertEquals(WarpEventsLogic.Arm.NONE, WarpEventsLogic.armFor(0));            // :61 eff > 0 gate
        assertEquals(WarpEventsLogic.Arm.GRANT_RESEARCH, WarpEventsLogic.armFor(1));  // :62
        assertEquals(WarpEventsLogic.Arm.GRANT_RESEARCH, WarpEventsLogic.armFor(4));
        assertEquals(WarpEventsLogic.Arm.NONE, WarpEventsLogic.armFor(5));            // :65 eff > 8 gate
        assertEquals(WarpEventsLogic.Arm.NONE, WarpEventsLogic.armFor(8));
        assertEquals(WarpEventsLogic.Arm.CHAT_11, WarpEventsLogic.armFor(9));         // :66
        assertEquals(WarpEventsLogic.Arm.CHAT_11, WarpEventsLogic.armFor(12));
        assertEquals(WarpEventsLogic.Arm.VISEXHAUST, WarpEventsLogic.armFor(13));     // :69
        assertEquals(WarpEventsLogic.Arm.VISEXHAUST, WarpEventsLogic.armFor(16));
        assertEquals(WarpEventsLogic.Arm.THAUMARHIA, WarpEventsLogic.armFor(17));     // :80
        assertEquals(WarpEventsLogic.Arm.THAUMARHIA, WarpEventsLogic.armFor(20));
        assertEquals(WarpEventsLogic.Arm.UNHUNGER_5000, WarpEventsLogic.armFor(21));  // :91
        assertEquals(WarpEventsLogic.Arm.UNHUNGER_5000, WarpEventsLogic.armFor(24));
        assertEquals(WarpEventsLogic.Arm.CHAT_12, WarpEventsLogic.armFor(25));        // :104
        assertEquals(WarpEventsLogic.Arm.CHAT_12, WarpEventsLogic.armFor(28));
        assertEquals(WarpEventsLogic.Arm.MIST_ONE, WarpEventsLogic.armFor(29));       // :106
        assertEquals(WarpEventsLogic.Arm.MIST_ONE, WarpEventsLogic.armFor(32));
        assertEquals(WarpEventsLogic.Arm.BLURRED, WarpEventsLogic.armFor(33));       // :109
        assertEquals(WarpEventsLogic.Arm.BLURRED, WarpEventsLogic.armFor(36));
        assertEquals(WarpEventsLogic.Arm.SUNSCORNED, WarpEventsLogic.armFor(37));     // :114
        assertEquals(WarpEventsLogic.Arm.SUNSCORNED, WarpEventsLogic.armFor(40));
        assertEquals(WarpEventsLogic.Arm.STARVATION, WarpEventsLogic.armFor(41));     // :126
        assertEquals(WarpEventsLogic.Arm.STARVATION, WarpEventsLogic.armFor(44));
        assertEquals(WarpEventsLogic.Arm.INF_VISEXHAUST, WarpEventsLogic.armFor(45)); // :133
        assertEquals(WarpEventsLogic.Arm.INF_VISEXHAUST, WarpEventsLogic.armFor(48));
        assertEquals(WarpEventsLogic.Arm.WEAKNESS, WarpEventsLogic.armFor(49));       // :144
        assertEquals(WarpEventsLogic.Arm.WEAKNESS, WarpEventsLogic.armFor(52));
        assertEquals(WarpEventsLogic.Arm.DEATHGAZE, WarpEventsLogic.armFor(53));      // :147
        assertEquals(WarpEventsLogic.Arm.DEATHGAZE, WarpEventsLogic.armFor(56));
        assertEquals(WarpEventsLogic.Arm.SPIDERS_ILLUSION, WarpEventsLogic.armFor(57)); // :158
        assertEquals(WarpEventsLogic.Arm.SPIDERS_ILLUSION, WarpEventsLogic.armFor(60));
        assertEquals(WarpEventsLogic.Arm.CHAT_13, WarpEventsLogic.armFor(61));        // :160
        assertEquals(WarpEventsLogic.Arm.CHAT_13, WarpEventsLogic.armFor(64));
        assertEquals(WarpEventsLogic.Arm.MIST_W30, WarpEventsLogic.armFor(65));       // :162
        assertEquals(WarpEventsLogic.Arm.MIST_W30, WarpEventsLogic.armFor(68));
        assertEquals(WarpEventsLogic.Arm.SLOWNESS, WarpEventsLogic.armFor(69));       // :165
        assertEquals(WarpEventsLogic.Arm.SLOWNESS, WarpEventsLogic.armFor(72));
        assertEquals(WarpEventsLogic.Arm.UNHUNGER_6000, WarpEventsLogic.armFor(73));  // :177 — 73..75
        assertEquals(WarpEventsLogic.Arm.UNHUNGER_6000, WarpEventsLogic.armFor(75));  // fall past the
        assertEquals(WarpEventsLogic.Arm.STICKY_RELIEF, WarpEventsLogic.armFor(76));  // :169 equality arm
        assertEquals(WarpEventsLogic.Arm.UNHUNGER_6000, WarpEventsLogic.armFor(77));  // :177 — 77..80
        assertEquals(WarpEventsLogic.Arm.UNHUNGER_6000, WarpEventsLogic.armFor(80));
        assertEquals(WarpEventsLogic.Arm.GRANT_RESEARCH_TEN, WarpEventsLogic.armFor(81)); // :190
        assertEquals(WarpEventsLogic.Arm.GRANT_RESEARCH_TEN, WarpEventsLogic.armFor(84));
        assertEquals(WarpEventsLogic.Arm.NONE, WarpEventsLogic.armFor(85));           // :193 eff > 88 gate
        assertEquals(WarpEventsLogic.Arm.NONE, WarpEventsLogic.armFor(88));
        assertEquals(WarpEventsLogic.Arm.SPIDERS_REAL, WarpEventsLogic.armFor(89));   // :195
        assertEquals(WarpEventsLogic.Arm.SPIDERS_REAL, WarpEventsLogic.armFor(92));
        assertEquals(WarpEventsLogic.Arm.MIST_W15, WarpEventsLogic.armFor(93));       // :197
        assertEquals(WarpEventsLogic.Arm.MIST_W15, WarpEventsLogic.armFor(99));
    }

    @Test
    void theWiredArmsAreTheMobMistAndReliefArms() {
        // t_1a83f0db scope: the mob-spawn seam (+ the mist FX + the relief arm
        // that rides warp state the port already ships).
        assertTrue(WarpEventsLogic.ships(WarpEventsLogic.Arm.SPIDERS_ILLUSION));
        assertTrue(WarpEventsLogic.ships(WarpEventsLogic.Arm.SPIDERS_REAL));
        assertTrue(WarpEventsLogic.ships(WarpEventsLogic.Arm.MIST_ONE));
        assertTrue(WarpEventsLogic.ships(WarpEventsLogic.Arm.MIST_W30));
        assertTrue(WarpEventsLogic.ships(WarpEventsLogic.Arm.MIST_W15));
        assertTrue(WarpEventsLogic.ships(WarpEventsLogic.Arm.STICKY_RELIEF));
        assertFalse(WarpEventsLogic.ships(WarpEventsLogic.Arm.NONE));
        assertFalse(WarpEventsLogic.ships(WarpEventsLogic.Arm.VISEXHAUST));
        assertFalse(WarpEventsLogic.ships(WarpEventsLogic.Arm.GRANT_RESEARCH));
        assertFalse(WarpEventsLogic.ships(WarpEventsLogic.Arm.CHAT_11));
    }

    @Test
    void guardianCountsAreTheArmRollsCappedAtEight() {
        // :106 (1), :162 (warp/30), :197 (warp/15), :231 min(8, guardian)
        assertEquals(1, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_ONE, 90));
        assertEquals(3, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_W30, 90));  // 90/30
        assertEquals(6, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_W15, 90));  // 90/15
        assertEquals(3, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_W15, 45));  // 45/15
        assertEquals(0, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_W30, 15));  // 15/30 = 0
        assertEquals(8, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_W15, 120)); // min(8, 8)
        assertEquals(8, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.MIST_W15, 150)); // min(8, 10)
        assertEquals(0, WarpEventsLogic.guardianCount(WarpEventsLogic.Arm.NONE, 90));
    }

    @Test
    void spiderSpawnsAreTheWarpCappedAtFifty() {
        // :282 — int spawns = Math.min(50, warp)
        assertEquals(50, WarpEventsLogic.MAX_SPAWNS);
        assertEquals(50, WarpEventsLogic.spiderSpawns(90));
        assertEquals(30, WarpEventsLogic.spiderSpawns(30));
        assertEquals(50, WarpEventsLogic.spiderSpawns(100));
        assertEquals(0, WarpEventsLogic.spiderSpawns(0));
    }

    @Test
    void theScanIsFiftyAttemptsOfSevenToTwentyFourTimesASign() {
        // :263/:291 (50 attempts), :264-266/:292-297 (func_76136_a(rand, 7, 24)
        // * func_76136_a(rand, -1, 1) per axis)
        assertEquals(50, WarpEventsLogic.SCAN_ATTEMPTS);
        assertEquals(7, WarpEventsLogic.OFFSET_MIN);
        assertEquals(24, WarpEventsLogic.OFFSET_MAX);
        assertEquals(-1, WarpEventsLogic.SIGN_MIN);
        assertEquals(1, WarpEventsLogic.SIGN_MAX);
    }

    @Test
    void theTailDecaysTempEveryCheckAndTheReliefOneSticky() {
        // :224 addWarpTemp(-1) on every check; :172 addWarpSticky(-1) at eff 76
        assertEquals(-1, WarpEventsLogic.TAIL_TEMP_DECAY);
        assertEquals(-1, WarpEventsLogic.STICKY_RELIEF_DELTA);
    }

    @Test
    void theChatKeysAreTheOriginals() {
        // :238 (warp.text.6), :321 (warp.text.7), :176 (warp.text.14) —
        // reference en_US.lang
        assertEquals("warp.text.6", WarpEventsLogic.KEY_MIST);
        assertEquals("warp.text.7", WarpEventsLogic.KEY_SPIDERS);
        assertEquals("warp.text.14", WarpEventsLogic.KEY_RELIEF);
    }
}
