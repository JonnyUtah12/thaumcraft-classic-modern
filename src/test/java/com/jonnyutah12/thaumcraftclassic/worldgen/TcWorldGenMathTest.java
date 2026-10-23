package com.jonnyutah12.thaumcraftclassic.worldgen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math parity tests for the original ore Y-bands (TC-M5-05).
 * Transcription source: ThaumcraftWorldGenerator.generateOres L515-565.
 * No Minecraft types — see TcWorldGenMath for the band definitions.
 */
class TcWorldGenMathTest {

    @Test
    void cinnabarBandIsHeightOverFive() {
        // Original: random.nextInt(world.getHeight() / 5) with height 256
        // -> exclusive bound 51, i.e. Y 0..50.
        assertEquals(51, TcWorldGenMath.cinnabarYBound(TcWorldGenMath.ORIGINAL_WORLD_HEIGHT));
        assertEquals(51, TcWorldGenMath.cinnabarYBound(256));
        assertEquals(10, TcWorldGenMath.cinnabarYBound(50));
    }

    @Test
    void amberBandIsSurfaceMinusZeroToTwentyFour() {
        // Original: heightValue(x,z) - random.nextInt(25).
        for (int roll = 0; roll <= 24; roll++) {
            assertEquals(70 - roll, TcWorldGenMath.surfaceBelowY(70, 0, 24, roll));
        }
        // The roll domain the placement modifier draws from.
        Random random = new Random(0L);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            int roll = random.nextInt(24 - 0 + 1);
            seen.add(TcWorldGenMath.surfaceBelowY(70, 0, 24, roll));
        }
        assertEquals(25, seen.size());
        assertTrue(seen.contains(70) && seen.contains(46));
    }

    @Test
    void infusedBandIsBottomAnchoredWithFloorFive() {
        // Original: random.nextInt(Math.max(5, heightValue(x,z) - 5)).
        assertEquals(45, TcWorldGenMath.surfaceCappedBound(5, -5, 50));   // normal surface
        assertEquals(5, TcWorldGenMath.surfaceCappedBound(5, -5, 10));    // low surface -> floor wins
        assertEquals(5, TcWorldGenMath.surfaceCappedBound(5, -5, 3));     // surface at/below floor
        assertEquals(0, TcWorldGenMath.surfaceCappedBound(0, 0, 0));      // degenerate
    }

    @Test
    void seededBandDistributionsMatchOriginalDomains() {
        // Seeded sanity: every drawn Y must sit inside the original domain,
        // and the domain must be fully reachable (docs/testing.md seeded rule).
        Random random = new Random(20261018L);

        Set<Integer> cinnabar = new HashSet<>();
        for (int i = 0; i < 20000; i++) {
            int y = random.nextInt(TcWorldGenMath.cinnabarYBound(TcWorldGenMath.ORIGINAL_WORLD_HEIGHT));
            assertTrue(y >= 0 && y <= 50, "cinnabar Y out of band: " + y);
            cinnabar.add(y);
        }
        assertEquals(51, cinnabar.size());

        Set<Integer> infused = new HashSet<>();
        for (int i = 0; i < 20000; i++) {
            int surface = 40 + random.nextInt(80);
            int bound = TcWorldGenMath.surfaceCappedBound(5, -5, surface);
            int y = random.nextInt(bound);
            assertTrue(y >= 0 && y < bound, "infused Y out of band: " + y + " bound " + bound);
            infused.add(y);
        }
        // Union of all reachable bands (surface 40..119 -> bound 35..114)
        // covers Y 0..113; all of them must be hit.
        assertEquals(114, infused.size());
    }
}
