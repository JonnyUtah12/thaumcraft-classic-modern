package com.jonnyutah12.thaumcraftclassic.gametest;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-04-s-b2: the mutation guard for the bloom disc probe's ROLL STREAM.
 *
 * <p>The 2026-10-29 investigation on {@code wt/taint-b1} reported that 4 of the
 * 16 biome palette cells of the Ethereal Bloom's cleansing disc never cleansed,
 * and pointed at {@code TaintBiomes.setBiomeAt}'s palette-cell index. The write
 * path measured clean (the game probe
 * {@code TaintCleansingGameTest.diagnoseWhereASingleBiomeWriteLands}: 41/41
 * single-column writes land in their own column's 4-block footprint, 625/625
 * painted columns read back taint). What was actually missing was rolls:
 * {@code RandomSource.create(long)} wraps {@code java.util.Random}, whose
 * {@code seed = seed * 0x5DEECE66D + 0xB (mod 2^48)} step amplifies a seed
 * difference of one by only 2.3e10 per draw, so across the CONSECUTIVE seeds
 * 1..2500 the first {@code nextInt(8)} takes four values and the bloom's x
 * offset — first draw minus second — spans [-3, +7]. The x band -7..-5 is never
 * drawn, so the four cells sitting there get zero rolls and cannot fire. The
 * probe read that as an engine defect.</p>
 *
 * <p>Every assertion here is about the stream, so the stream cannot regress back
 * under the game probe: revert {@code TaintCleansingGameTest.rollSeed} to a
 * plain counter and this test goes red in seconds, in {@code ./gradlew test},
 * with no world and no 25-minute gate cycle.</p>
 */
class TaintCleansingRollCoverageTest {

    private static final int ROLLS = TaintCleansingGameTest.ROLLS;
    private static final int LIMIT = TaintRules.BLOOM_SCAN_MOD - 1;

    /** The cells of the disc, for a bloom whose origin is (bx mod 16, bz mod 16). */
    private static Set<String> discCells(final int bx, final int bz) {
        final Set<String> cells = new TreeSet<>();
        for (int dx = -LIMIT; dx <= LIMIT; dx++) {
            for (int dz = -LIMIT; dz <= LIMIT; dz++) {
                if (TaintRules.bloomDistanceSquared(dx, dz) <= TaintRules.BLOOM_RADIUS_SQUARED) {
                    cells.add(key(bx + dx, bz + dz));
                }
            }
        }
        return cells;
    }

    /** Chunk-local palette-cell key, matching the probe's own cell identity. */
    private static String key(final int x, final int z) {
        return (x >> 4) + ":" + ((x >> 2) & 3) + "|" + (z >> 4) + ":" + ((z >> 2) & 3);
    }

    /**
     * The stream must span the whole scan box on BOTH axes. This is the axis the
     * disc probe lost: x is drawn FIRST, so its offset inherits the frozen first
     * draw of consecutive seeds and never reaches -5..-7.
     */
    @Test
    public void theRollStreamSpansTheWholeScanBoxOnBothAxes() {
        int minTx = Integer.MAX_VALUE;
        int maxTx = Integer.MIN_VALUE;
        int minTz = Integer.MAX_VALUE;
        int maxTz = Integer.MIN_VALUE;
        final Map<Integer, Integer> countsX = new TreeMap<>();
        final Map<Integer, Integer> countsZ = new TreeMap<>();
        for (long index = 1; index <= ROLLS; index++) {
            final BlockPos off = TaintCleansingGameTest.bloomRollOffset(index);
            minTx = Math.min(minTx, off.getX());
            maxTx = Math.max(maxTx, off.getX());
            minTz = Math.min(minTz, off.getZ());
            maxTz = Math.max(maxTz, off.getZ());
            countsX.merge(off.getX(), 1, Integer::sum);
            countsZ.merge(off.getZ(), 1, Integer::sum);
        }
        assertEquals(-LIMIT, minTx, "the x offset stream must reach the -" + LIMIT + " edge of the"
                + " bloom's scan box; the 2026-10-29 miss was exactly the cells at x offsets"
                + " -7..-5 (consecutive seeds freeze java.util.Random's first draw)");
        assertEquals(LIMIT, maxTx, "the x offset stream must reach the +" + LIMIT + " edge");
        assertEquals(-LIMIT, minTz, "the z offset stream must reach the -" + LIMIT + " edge");
        assertEquals(LIMIT, maxTz, "the z offset stream must reach the +" + LIMIT + " edge");
        int minXCount = Integer.MAX_VALUE;
        for (int v = -LIMIT; v <= LIMIT; v++) {
            minXCount = Math.min(minXCount, countsX.getOrDefault(v, 0));
        }
        int minZCount = Integer.MAX_VALUE;
        for (int v = -LIMIT; v <= LIMIT; v++) {
            minZCount = Math.min(minZCount, countsZ.getOrDefault(v, 0));
        }
        assertTrue(minXCount >= 20 && minZCount >= 20,
                "every offset of the ±" + LIMIT + " box must be drawn often enough to cover a disc"
                        + " cell in one pass: rarest x value drawn " + minXCount + " times, rarest z"
                        + " value " + minZCount + " times, over " + ROLLS + " rolls");
    }

    /**
     * Coverage the disc probe actually asserts: for every bloom alignment on the
     * palette grid (the probe snaps its bloom to a 4-aligned origin, and the
     * template puts it at a different one every run), every cell of the disc has
     * to receive rolls. A cell with 0 rolls cannot fire — that is the whole
     * "4 of 16 cells never cleanse" report, in one number.
     */
    @Test
    public void everyDiscCellGetsRollsAtEveryBloomAlignment() {
        int worst = Integer.MAX_VALUE;
        String worstWhere = "";
        final StringBuilder zeroCells = new StringBuilder();
        for (int bx = 0; bx < 16; bx += 4) {
            for (int bz = 0; bz < 16; bz += 4) {
                final Map<String, Integer> perCell = new TreeMap<>();
                for (long index = 1; index <= ROLLS; index++) {
                    final BlockPos off = TaintCleansingGameTest.bloomRollOffset(index);
                    if (TaintRules.bloomDistanceSquared(off.getX(), off.getZ())
                            > TaintRules.BLOOM_RADIUS_SQUARED) {
                        continue; // the radius gate trims it: not a fire candidate
                    }
                    final String cell = key(bx + off.getX(), bz + off.getZ());
                    perCell.merge(cell, 1, Integer::sum);
                }
                for (final String cell : discCells(bx, bz)) {
                    final int n = perCell.getOrDefault(cell, 0);
                    if (n == 0) {
                        zeroCells.append(' ').append(bx).append('/').append(bz).append("->").append(cell);
                    }
                    if (n < worst) {
                        worst = n;
                        worstWhere = "bloom origin " + bx + "/" + bz + " cell " + cell;
                    }
                }
            }
        }
        assertTrue(zeroCells.length() == 0,
                "every cell of the cleansing disc must receive rolls, else the disc probe's"
                        + " \"the disc is cleansed\" assertion is a coverage accident and a real"
                        + " cleansing bug hides behind it; cells with ZERO rolls:" + zeroCells);
        assertTrue(worst >= 8,
                "the least-covered disc cell needs a margin over one roll, not exactly one: "
                        + worst + " rolls (" + worstWhere + " over " + ROLLS + " rolls)");
    }

    /**
     * The mechanism, pinned. The stream on CONSECUTIVE seeds is degenerate — this
     * is the fact the fix rests on, so the test states it: if a future mapping
     * change swaps {@code RandomSource.create(long)} for a generator that is NOT
     * degenerate this way, this test says so and the rollSeed() comment gets
     * revisited, instead of the probe quietly recovering and the reasoning
     * rotting.
     */
    @Test
    public void consecutiveSeedsFreezeTheFirstDrawThatIsTheRootCause() {
        int minTx = Integer.MAX_VALUE;
        int maxTx = Integer.MIN_VALUE;
        int negativeSide = 0;
        for (long seed = 1; seed <= ROLLS; seed++) {
            final BlockPos off = TaintCleansingGameTest.mirroredBloomOffset(seed);
            minTx = Math.min(minTx, off.getX());
            maxTx = Math.max(maxTx, off.getX());
            if (off.getX() <= -5) {
                negativeSide++;
            }
        }
        assertTrue(minTx > -LIMIT,
                "expected: on consecutive seeds the x offset does NOT reach -" + LIMIT
                        + " (measured minTx=" + minTx + "). If it now does, the RNG behind"
                        + " RandomSource.create(long) changed — re-measure before trusting the"
                        + " rollSeed() comment, and keep the decorrelated stream anyway");
        assertTrue(negativeSide == 0,
                "expected: consecutive seeds draw x <= -5 zero times over " + ROLLS
                        + " rolls (measured " + negativeSide + "); tx span was [" + minTx + ","
                        + maxTx + "]");
    }

    /** The decorated stream does draw the band the 2026-10-29 run lost. */
    @Test
    public void theDecorrelatedStreamDrawsTheBandConsecutiveSeedsNeverReach() {
        int negativeSide = 0;
        for (long index = 1; index <= ROLLS; index++) {
            if (TaintCleansingGameTest.bloomRollOffset(index).getX() <= -5) {
                negativeSide++;
            }
        }
        assertTrue(negativeSide >= 40,
                "the x band -7..-5 (the disc cells that were reported as \"never cleansing\")"
                        + " must be drawn by the roll stream: " + negativeSide + " of " + ROLLS
                        + " rolls");
    }

}
