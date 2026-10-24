package com.jonnyutah12.thaumcraftclassic.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Pins the TC-M5-05b transcription of the original's structure machinery:
 *
 * <ul><li>the shrine-mound block table ({@link MoundStructure}) must equal a
 * replay of the original {@code WorldGenMound} setBlock sequence (the same
 * parse {@code scripts/extract_mound_structure.py} performs), and the runtime
 * table in {@link WorldGenMoundGen} must cover the same cells;</li>
 * <li>the chained else-if roll (1/150 mound, else 1/66 ring, else 1/40
 * hilltop) must be mutually exclusive, conditional (a mound hit never falls
 * through even when the mound fails), and select the same branch as the
 * original's raw draw sequence for the same seed.</li>
 * </ul>
 */
class StructureParityTest {

    private static final Path ORIGINAL_MOUND = Path.of(
            "../reference/decompiled/thaumcraft/thaumcraft/common/lib/world/WorldGenMound.java");

    // --------------------------------------------------------------------------------
    // mound table
    // --------------------------------------------------------------------------------

    @Test
    void moundTableMatchesOriginalSetBlockSequence() throws Exception {
        Assumptions.assumeTrue(Files.exists(ORIGINAL_MOUND), "original source not checked out");
        final String src = Files.readString(ORIGINAL_MOUND);

        // Replay the original's sequential setBlock calls: last write per cell wins.
        final Map<String, Character> fields = Map.ofEntries(
                Map.entry("field_150350_a", 'a'), Map.entry("field_150346_d", 'd'),
                Map.entry("field_150349_c", 'g'), Map.entry("field_150347_e", 'c'),
                Map.entry("field_150341_Y", 'm'), Map.entry("field_150417_aV", 'n'),
                Map.entry("field_150411_aY", 'r'), Map.entry("field_150329_H", 't'),
                Map.entry("field_150468_ap:5", 'L'), Map.entry("field_150468_ap:0", 'l'));
        final Map<Integer, Map<Integer, Map<Integer, Character>>> replay = new HashMap<>();

        final Matcher m = CALL.matcher(src);
        int calls = 0;
        while (m.find()) {
            final int x = Integer.parseInt(m.group(1).replace(" ", "").replace("+", ""));
            final int y = Integer.parseInt(m.group(2).replace(" ", "").replace("+", ""));
            final int z = Integer.parseInt(m.group(3).replace(" ", "").replace("+", ""));
            final String field = m.group(4);
            final String meta = m.group(5);
            final char ch;
            if (TAIL.contains(field)) {
                continue; // hand-ported tail, not table cells
            } else if ("field_150446_ar".equals(field)) {
                ch = "suvwxyzA".charAt(Integer.parseInt(meta)); // stairs metas 0..7
            } else {
                ch = fields.get(field + (meta == null ? "" : ":" + meta));
                if (ch == 0) {
                    ch = fields.getOrDefault(field, '?');
                }
            }
            if (ch == '?') {
                throw new AssertionError("unmapped block field in original mound: " + field);
            }
            replay.computeIfAbsent(y, yy -> new HashMap<>())
                    .computeIfAbsent(x, xx -> new HashMap<>()).put(z, ch);
            calls++;
        }
        assertTrue(calls > 1000, "expected the original mound to have >1000 setBlock calls, got " + calls);

        // Decode the ported table and compare cell by cell.
        final Map<Integer, Map<Integer, Map<Integer, Character>>> ported = new HashMap<>();
        for (final String row : MoundStructure.ROWS) {
            final int bar = row.indexOf('|');
            final String[] yx = row.substring(0, bar).split(",");
            final int y = Integer.parseInt(yx[0]);
            final int x = Integer.parseInt(yx[1]);
            final String cells = row.substring(bar + 1);
            for (int z = 0; z < cells.length(); z++) {
                final char c = cells.charAt(z);
                if (c != '.') {
                    ported.computeIfAbsent(y, yy -> new HashMap<>())
                            .computeIfAbsent(x, xx -> new HashMap<>()).put(z + MoundStructure.Z_MIN, c);
                }
            }
        }
        assertEquals(replay, ported, "ported mound table differs from the original setBlock replay");

        // The runtime table (what actually gets placed) must cover the same cells.
        final Map<Integer, Map<Integer, Map<Integer, Character>>> runtime = new HashMap<>();
        for (final Map.Entry<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> e
                : WorldGenMoundGen.tableEntries()) {
            final net.minecraft.core.BlockPos p = e.getKey();
            runtime.computeIfAbsent(p.getY(), yy -> new HashMap<>())
                    .computeIfAbsent(p.getX(), xx -> new HashMap<>()).put(p.getZ(), 'X');
        }
        final Map<Integer, Map<Integer, Map<Integer, Character>>> replayShape = new HashMap<>();
        replay.forEach((y, xs) -> xs.forEach((x, zs) -> zs.keySet()
                .forEach(z -> replayShape.computeIfAbsent(y, yy -> new HashMap<>())
                        .computeIfAbsent(x, xx -> new HashMap<>()).put(z, 'X'))));
        assertEquals(replayShape, runtime, "runtime mound table cells differ from the original");
    }

    private static final Pattern CALL = Pattern.compile(
            "world\\.func_1474(?:49_b|65_d)\\(\\s*(?:i|par2\\d)\\s*([-+]\\s*\\d+)\\s*,\\s*"
                    + "(?:j|par2\\d)\\s*([-+]\\s*\\d+)\\s*,\\s*(?:k|par2\\d)\\s*([-+]\\s*\\d+)\\s*,\\s*"
                    + "Blocks\\.(field_\\w+)(?:, (\\d+), \\d+)?\\);");

    private static final java.util.Set<String> TAIL = java.util.Set.of(
            "field_150486_ae", "field_150447_bR", "field_150335_W", "field_150474_ac");

    // --------------------------------------------------------------------------------
    // chained roll
    // --------------------------------------------------------------------------------

    @Test
    void rollIsMutuallyExclusiveAndConditional() {
        // For every seed, at most one attempt runs, and a mound hit never falls
        // through to the ring/hilltop rolls even when the mound fails (the
        // original's else-if chain).
        for (long seed = 0; seed < 20000; seed++) {
            final int[] runs = new int[3];
            StructureChunkGen.rollStructures(new Random(seed), 60, 320,
                    rand -> {
                        runs[0]++;
                        return false; // every attempt fails: no fall-through allowed
                    },
                    rand -> {
                        runs[1]++;
                        return false;
                    },
                    rand -> {
                        runs[2]++;
                        return false;
                    });
            assertTrue(runs[0] + runs[1] + runs[2] <= 1,
                    "seed " + seed + " ran more than one structure attempt: " + java.util.Arrays.toString(runs));
        }
    }

    @Test
    void rollFrequenciesMatchOriginal() {
        // Chained conditional probabilities: mound 1/150, ring (149/150)/66,
        // hilltop (149/150)(65/66)/40.
        final int trials = 300000;
        int mound = 0;
        int ring = 0;
        int hilltop = 0;
        for (long seed = 0; seed < trials; seed++) {
            final int[] runs = new int[3];
            StructureChunkGen.rollStructures(new Random(seed), 60, 320,
                    rand -> {
                        runs[0]++;
                        return true;
                    },
                    rand -> {
                        runs[1]++;
                        return true;
                    },
                    rand -> {
                        runs[2]++;
                        return true;
                    });
            mound += runs[0];
            ring += runs[1];
            hilltop += runs[2];
        }
        assertClose(mound, trials / 150.0, "mound");
        assertClose(ring, trials * (149.0 / 150.0) / 66.0, "ring");
        assertClose(hilltop, trials * (149.0 / 150.0) * (65.0 / 66.0) / 40.0, "hilltop");
    }

    @Test
    void rollSelectsTheSameBranchAsTheOriginalDrawSequence() {
        // The original draws nextInt(150), then (else) nextInt(66), then (else)
        // nextInt(40). For a fixed seed the ported chain must land on the same
        // branch the raw draws indicate.
        for (final long seed : new long[]{1L, 42L, 12345L, 987654321L, 55L, 777L}) {
            final Random reference = new Random(seed);
            final int r1 = reference.nextInt(150);
            final int r2 = r1 == 0 ? -1 : reference.nextInt(66);
            final int r3 = (r1 == 0 || r2 == 0) ? -1 : reference.nextInt(40);
            final boolean expectedMound = r1 == 0;
            final boolean expectedRing = r1 != 0 && r2 == 0;
            final boolean expectedHilltop = r1 != 0 && r2 != 0 && r3 == 0;

            final boolean[] ran = new boolean[3];
            StructureChunkGen.rollStructures(new Random(seed), 60, 320,
                    rand -> {
                        ran[0] = true;
                        return true;
                    },
                    rand -> {
                        rand.nextInt(6); // ring w/h size draws happen before generating
                        rand.nextInt(6);
                        return true;
                    },
                    rand -> {
                        ran[2] = true;
                        return true;
                    });
            assertEquals(expectedMound, ran[0], "mound branch for seed " + seed);
            assertEquals(expectedRing, ran[1], "ring branch for seed " + seed);
            assertEquals(expectedHilltop, ran[2], "hilltop branch for seed " + seed);
        }
    }

    private static void assertClose(final int actual, final double expected, final String what) {
        final double tolerance = 6 * Math.sqrt(expected);
        assertTrue(Math.abs(actual - expected) < tolerance,
                what + " frequency off: got " + actual + ", expected ~" + expected);
    }
}
