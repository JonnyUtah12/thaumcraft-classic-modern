package com.jonnyutah12.thaumcraftclassic.warp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * TC-M6-05-s: the warp accumulation math, pinned as pure arithmetic
 * ({@link WarpLogic} is the logic half of {@code WarpEvents.checkWarpEvent}).
 * WarpParityTest pins these numbers against the decompiled original; this class
 * pins the behaviour of the ported formulae, including the accumulation loop
 * across repeated events.
 */
class WarpLogicTest {

    // ------------------------------------------------------------- roll gate

    @Test
    void rollGateMatchesOriginal() {
        // warpCounter > 0 && warp > 0 && nextInt(100) <= sqrt(warpCounter)
        assertFalse(WarpLogic.fires(10, 0, 0), "a drained counter never rolls");
        assertFalse(WarpLogic.fires(0, 100, 0), "zero warp never rolls");
        assertFalse(WarpLogic.fires(-5, 100, 0), "negative warp never rolls");
        assertTrue(WarpLogic.fires(10, 1, 1), "sqrt(1) = 1: a draw of 1 fires");
        assertFalse(WarpLogic.fires(10, 1, 2), "sqrt(1) = 1: a draw of 2 does not");
        assertTrue(WarpLogic.fires(10, 4, 2), "sqrt(4) = 2");
        assertFalse(WarpLogic.fires(10, 4, 3));
        assertTrue(WarpLogic.fires(10, 100, 10), "sqrt(100) = 10, inclusive");
        assertFalse(WarpLogic.fires(10, 100, 11));
    }

    @Test
    void rollWarpIsTwoThirdsOfWarpPlusAThirdOfTheCounter() {
        assertEquals(20, WarpLogic.rollWarp(10, 40), "(10 + 10 + 40) / 3");
        assertEquals(100, WarpLogic.rollWarp(300, 0), "capped at 100");
        assertEquals(0, WarpLogic.rollWarp(0, 0));
        assertEquals(1, WarpLogic.rollWarp(1, 1), "truncating integer division");
    }

    @Test
    void counterIsSetToWarpCounterMinusTheDrain() {
        // (int)(warpCounter - max(5.0, sqrt(warpCounter) * 2.0)) - a SET, not a
        // decrement by an independent amount, and the 5.0 floor dominates early.
        assertEquals(3, WarpLogic.rollCounter(10), "10 - max(5, sqrt(10)*2 = 6.32) = 3.67 -> 3");
        assertEquals(80, WarpLogic.rollCounter(100));
        assertEquals(0, WarpLogic.rollCounter(5), "5 - max(5, 4.47) = 0");
        assertEquals(-1, WarpLogic.rollCounter(4),
                "4 - 5 = -1: the counter goes negative and fires() closes until a grant refills it");
        assertEquals(74, WarpLogic.rollCounter(94), "94 - 19.38 -> 74");
    }

    @Test
    void counterAccumulatesDownOverRepeatedEvents() {
        // The original's counter is a pure drain: each fired event rewrites it
        // with rollCounter(), so a long run marches to zero and the roll gate
        // then closes (fires() needs counter > 0).
        int counter = 100;
        int fired = 0;
        for (int i = 0; i < 50 && WarpLogic.fires(60, counter, 0); i++) {
            counter = WarpLogic.rollCounter(counter);
            fired++;
            assertTrue(counter < 100 || i == 0, "the counter must strictly drain");
        }
        assertTrue(fired >= 6, "a 100 counter must keep firing for several events, fired " + fired);
        assertTrue(counter <= 0, "the run must drain the counter, left " + counter);
    }

    @Test
    void amplifierIsCappedAtThree() {
        assertEquals(0, WarpLogic.effectAmplifier(14));
        assertEquals(1, WarpLogic.effectAmplifier(15));
        assertEquals(2, WarpLogic.effectAmplifier(30));
        assertEquals(3, WarpLogic.effectAmplifier(45));
        assertEquals(3, WarpLogic.effectAmplifier(1000), "Math.min(3, ...)");
    }

    // ----------------------------------------------------------- outcome table

    @Test
    void everyBranchOfTheOriginalChainIsReproduced() {
        assertEquals(WarpLogic.Kind.NONE, WarpLogic.outcome(0, 50).kind(), "eff 0 is silent");
        assertEquals(WarpLogic.Kind.RESEARCH, WarpLogic.outcome(1, 50).kind());
        assertEquals(1, WarpLogic.outcome(1, 50).times(), "grantResearch(1)");
        assertEquals(WarpLogic.Kind.NONE, WarpLogic.outcome(5, 50).kind(), "the 5-8 gap is silent");
        assertEquals(WarpLogic.Kind.NONE, WarpLogic.outcome(8, 50).kind());
        assertEquals(WarpLogic.Kind.CHAT, WarpLogic.outcome(9, 50).kind());
        assertEquals("warp.text.11", WarpLogic.outcome(9, 50).langKey());
        assertEquals(WarpLogic.Kind.VIS_EXHAUST, WarpLogic.outcome(13, 50).kind());
        assertEquals(5000, WarpLogic.outcome(13, 50).duration());
        assertEquals(WarpLogic.Kind.THAUMARHIA, WarpLogic.outcome(17, 50).kind());
        assertEquals(500, WarpLogic.outcome(17, 50).duration(), "10 * warp");
        assertEquals(WarpLogic.Kind.UNNATURAL_HUNGER, WarpLogic.outcome(21, 50).kind());
        assertEquals(WarpLogic.Kind.MIST, WarpLogic.outcome(29, 50).kind());
        assertEquals(1, WarpLogic.outcome(29, 50).guardians(), "eff <= 32 spawns one guardian");
        assertEquals(WarpLogic.Kind.BLURRED_VISION, WarpLogic.outcome(33, 50).kind());
        assertEquals(WarpLogic.Kind.SUN_SCORNED, WarpLogic.outcome(37, 50).kind());
        assertEquals(WarpLogic.Kind.DIG_SLOWDOWN, WarpLogic.outcome(41, 50).kind());
        assertEquals(1200, WarpLogic.outcome(41, 50).duration());
        assertEquals(WarpLogic.Kind.INFECTIOUS_VIS_EXHAUST, WarpLogic.outcome(45, 50).kind());
        assertEquals(6000, WarpLogic.outcome(45, 50).duration());
        assertEquals(WarpLogic.Kind.NIGHT_VISION, WarpLogic.outcome(49, 50).kind());
        assertEquals(2000, WarpLogic.outcome(49, 50).duration(), "40 * warp, capped at 6000");
        assertEquals(6000, WarpLogic.outcome(49, 300).duration(), "min(40 * warp, 6000)");
        assertEquals(WarpLogic.Kind.DEATH_GAZE, WarpLogic.outcome(53, 50).kind());
        assertEquals(WarpLogic.Kind.SPIDERS, WarpLogic.outcome(57, 50).kind());
        assertEquals(WarpLogic.Kind.CHAT, WarpLogic.outcome(61, 50).kind());
        assertEquals(1, WarpLogic.outcome(65, 50).guardians(), "warp / 30 guardians");
        assertEquals(WarpLogic.Kind.BLINDNESS, WarpLogic.outcome(69, 50).kind());
        assertEquals(WarpLogic.Kind.STICKY_RELIEF, WarpLogic.outcome(76, 50).kind());
        assertEquals(WarpLogic.Kind.UNNATURAL_HUNGER, WarpLogic.outcome(73, 50).kind(),
                "73-75 fall through to the eff <= 80 branch, as the original's if-chain does");
        assertEquals(WarpLogic.Kind.RESEARCH, WarpLogic.outcome(81, 50).kind());
        assertEquals(5, WarpLogic.outcome(81, 50).times(), "warp / 10");
        assertEquals(WarpLogic.Kind.NONE, WarpLogic.outcome(85, 50).kind(), "the 85-88 gap is silent");
        assertEquals(-1, WarpLogic.outcome(89, 50).guardians(), "mind spiders: the illusion marker");
        assertEquals(WarpLogic.Kind.MIST, WarpLogic.outcome(95, 50).kind());
        assertEquals(6, WarpLogic.outcome(95, 90).guardians(), "warp / 15 guardians");
    }

    @Test
    void everyChatKeyInTheTableIsShipped() {
        final var lang = com.jonnyutah12.thaumcraftclassic.testsupport.ParityPaths.lang();
        int keys = 0;
        for (int eff = 1; eff <= 100; eff++) {
            final String key = WarpLogic.outcome(eff, 50).langKey();
            if (key != null) {
                assertTrue(lang.contains("\"" + key + "\":"), key + " must ship in en_us.json");
                keys++;
            }
        }
        assertTrue(keys >= 15, "the table must exercise the original's warp text, got " + keys);
    }

    @Test
    void tempWarpDecaysByOnePerEvent() {
        assertEquals(4, WarpLogic.tempWarpAfterEvent(5));
        assertEquals(-1, WarpLogic.tempWarpAfterEvent(0), "the original does not clamp at 0");
    }

    // ------------------------------------------------------------ death gaze

    @Test
    void deathGazeGeometry() {
        assertEquals(11, WarpLogic.deathGazeRange(0), "the original's level 1 = 8 + 1*3");
        assertEquals(24, WarpLogic.deathGazeRange(10), "capped at 24");
        assertEquals(80, WarpLogic.DEATH_GAZE_WITHER_TICKS);

        final var apex = net.minecraft.world.phys.Vec3.ZERO;
        final var look = new net.minecraft.world.phys.Vec3(0, 0, 1);
        assertTrue(WarpLogic.coneVisible(apex, look, new net.minecraft.world.phys.Vec3(0, 0, 5),
                WarpLogic.DEATH_GAZE_APERTURE_RADIANS, 11));
        assertFalse(WarpLogic.coneVisible(apex, look, new net.minecraft.world.phys.Vec3(0, 0, -5),
                WarpLogic.DEATH_GAZE_APERTURE_RADIANS, 11), "behind the viewer");
        assertFalse(WarpLogic.coneVisible(apex, look, new net.minecraft.world.phys.Vec3(0, 0, 20),
                WarpLogic.DEATH_GAZE_APERTURE_RADIANS, 11), "out of range");
        assertFalse(WarpLogic.coneVisible(apex, look, new net.minecraft.world.phys.Vec3(9, 0, 1),
                WarpLogic.DEATH_GAZE_APERTURE_RADIANS, 11), "outside the 0.75 rad aperture");
        assertTrue(WarpLogic.coneVisible(apex, look, net.minecraft.world.phys.Vec3.ZERO,
                WarpLogic.DEATH_GAZE_APERTURE_RADIANS, 11) == false, "a zero-length target vector");
    }

    @Test
    void spawnCountsAreCapped() {
        assertEquals(8, WarpLogic.guardianCount(99));
        assertEquals(0, WarpLogic.guardianCount(-1));
        assertEquals(50, WarpLogic.spiderCount(500));
        assertEquals(30, WarpLogic.spiderCount(30));
    }
}
