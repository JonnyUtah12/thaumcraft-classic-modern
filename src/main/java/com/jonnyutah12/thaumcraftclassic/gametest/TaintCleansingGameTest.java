package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomPlant;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEtherealBloom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M5-04-s-b1: the in-world acceptance for the taint CLEANSING slice — the
 * three things the cleansing half of TC-M5-04-s has to prove in a live world
 * (design record docs/architecture/taint.md, deviations BD-15 / BD-16).
 *
 * <ol><li><b>The disc gate.</b> The bloom cleanses the columns inside its
 * radius and NEVER one beyond it: the scan box is {@code nextInt(8) -
 * nextInt(8)} per axis (±7) and the radius gate is the original's SQUARED
 * {@code TileEntity#getDistance} ≤ 81 ({@link TaintRules#BLOOM_RADIUS_SQUARED}),
 * so the cleansed area is a disc of radius ~9 — not an 81-block field, and not
 * the square the scan box draws.</li>
 * <li><b>The pre-taint record.</b> The restore writes back the biome the column
 * held BEFORE taint overwrote it ({@link TaintBiomes#takeGeneratedBiome}, BD-16);
 * plains is the fallback for a column with no record, which is exactly
 * the case {@code TileEtherealBloom.java:37} documents.</li>
 * <li><b>Variant 4 only.</b> The plant family is one id with six metas, and only
 * the Ethereal Bloom (meta 4) gets a block entity and a ticker. In 1.21.1
 * {@code state.hasBlockEntity()} is true for EVERY state of an
 * {@code EntityBlock}, so {@code getTicker} is the only per-variant gate — the
 * probe asks it for all six metas.</li></ol>
 *
 * <p>Biome reads go through {@link TaintBiomes#biomeAt}, i.e.
 * {@code level.getNoiseBiome(QuartPos.fromBlock(...))}, and never through
 * {@code Level#getBiome}: that path runs on {@code BiomeManager}, which caches
 * one biome per section cell for up to 600 game ticks, so it keeps answering
 * with the GENERATED biome for ~30 seconds after a taint write. A cleansing
 * probe reads a column it rewrote a tick earlier, so the palette is the only
 * read that can see its own effect. {@code TaintGameTest
 * .taintSpreadConvertsNeighbourColumnsAndBiomeWriteCoversColumn} pins the same
 * staleness for the spread half.</p>
 *
 * <p>Two numbers shape the assertions. (1) The rolls: the probes replay the
 * original's draw order with the same seed ({@link #mirroredBloomOffset}) and
 * assert the geometry of every roll that fired, so the ±7 modulus and the
 * squared radius are pinned exactly, not as a statistics band. (2) The 4-block
 * palette cell: one restore rewrites the whole cell of the column it fired on
 * (BD-15), so the disc is a set of CELLS, a cell is cleansed at most once, and
 * "never cleansed beyond the disc" is a statement about cells, not columns —
 * one 4×4 cell spans the bloom's own column and a column 8 blocks out on the
 * other side, so the disc gate is evaluated on the cell, and the
 * {@link ScriptedRollRandom} sweep proves coverage by construction instead of
 * by luck.</p>
 *
 * <p><b>The roll stream is part of the fixture</b> (TC-M5-04-s-b2). The seeded
 * rolls used to run on the consecutive seeds 1..N, and Java's LCG — the
 * generator {@code RandomSource.create(long)} wraps — cannot span the bloom's
 * scan box from consecutive seeds. See {@link #rollSeed(long)}: the seeds are
 * decorrelated by an odd-constant multiply, and the disc probe additionally
 * sweeps every offset of the box explicitly
 * ({@link ScriptedRollRandom}) so "the disc is cleansed" never depends on which
 * offsets a seed sequence happens to draw. That split is what the 2026-10-29
 * investigation on {@code wt/taint-b1} came back with: four of sixteen disc
 * cells never fired, and the write path was clean.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaintCleansingGameTest {

    private TaintCleansingGameTest() {
    }

    /** Half the side of the biome field each probe paints around its bloom. */
    private static final int PAINT_RADIUS = 12;
    /** Columns this close to the bloom are drawn often enough to demand coverage. */
    private static final int CORE_RADIUS = 3;
    /**
     * Cleansing rolls per probe. Cheap (a palette read, and a palette write when
     * the gate passes) and generous: after every cell of the disc is cleansed a
     * roll just reads the biome and returns false.
     */
    public static final int ROLLS = 2500;

    /**
     * The seed of roll number {@code index}.
     *
     * <p>TC-M5-04-s-b2. Java's LCG — {@code RandomSource.create(long)} goes
     * {@code RandomSourceSupport.create -> LegacyRandomSource -> java.util.Random}
     * — sets {@code seed = (seed ^ 0x5DEECE66D) & 2^48-1} and steps it by
     * {@code seed = seed * 0x5DEECE66D + 0xB}, returning the TOP bits. A seed
     * difference of ONE therefore moves the first output by only
     * 0x5DEECE66D ≈ 2.3e10 in a 2^48 state: the first {@code nextInt(8)} changes
     * roughly every 1400 consecutive seeds. Measured over the seeds 1..2500 the
     * first draw takes four values (4,5,6,7) and the second draw is already
     * decorrelated. The bloom draws x's pair first, so
     * {@code tx = first - second} spans [-3, +7] and NEVER reaches -5..-7 —
     * the four biome palette cells at x offsets -7..-5 got ZERO of the 2500
     * rolls, at every bloom alignment, and the disc probe read that as an
     * engine bug ("4 of 16 cells never cleanse").</p>
     *
     * <p>Multiplying the roll index by an odd golden-ratio constant decorrelates
     * consecutive rolls: measured over the same 2500 rolls, both axes span the
     * full -7..+7, the rarest axis value is drawn 34 times, and the
     * least-covered disc cell gets 16 rolls. Pinned by
     * {@code TaintCleansingRollCoverageTest}, which goes red the moment this
     * multiplication is removed — the unit test is the mutation guard, the game
     * probe does not have to carry the statistics.</p>
     */
    public static long rollSeed(final long index) {
        return index * 0x9E3779B97F4A7C15L;
    }

    /**
     * The offset the bloom picks for a SEED, by replaying the original's draw
     * order: x's pair first, then z's pair, each {@code nextInt(8) - nextInt(8)}.
     * Java evaluates constructor arguments left to right, so this draws in
     * exactly that order. Public so the unit coverage test can sweep the stream;
     * callers must feed it {@link #rollSeed(long)}, not a raw counter.
     */
    public static BlockPos mirroredBloomOffset(final long seed) {
        final RandomSource mirror = RandomSource.create(seed);
        return new BlockPos(
                TaintRules.bloomOffset(mirror.nextInt(TaintRules.BLOOM_SCAN_MOD),
                        mirror.nextInt(TaintRules.BLOOM_SCAN_MOD)),
                0,
                TaintRules.bloomOffset(mirror.nextInt(TaintRules.BLOOM_SCAN_MOD),
                        mirror.nextInt(TaintRules.BLOOM_SCAN_MOD)));
    }

    /** The offset of roll {@code index}: the seed stream, decorrelated. */
    public static BlockPos bloomRollOffset(final long index) {
        return mirroredBloomOffset(rollSeed(index));
    }

    /**
     * A {@link RandomSource} whose {@code nextInt(8)} draws are scripted, so one
     * cleansing roll lands on an EXACT offset. The bloom draws x's pair, then z's
     * pair (the original's order), so the queue is supplied in that order:
     * {@code (max(tx,0), max(-tx,0))} then {@code (max(tz,0), max(-tz,0))}, which
     * makes {@code bloomOffset(a,b) = a - b} equal the requested offset.
     *
     * <p>Two properties come from the shape: the disc's cell coverage stops being
     * a property of the seed stream (it is a sweep of the whole ±7 box), and the
     * draw ORDER is pinned — a bloom that drew z's pair first would compute the
     * transposed offset, land in a different palette cell, and turn the probe
     * red.</p>
     */
    private static final class ScriptedRollRandom implements RandomSource {

        private final int[] draws;
        private int at;

        private ScriptedRollRandom(final int tx, final int tz) {
            draws = new int[] {Math.max(tx, 0), Math.max(-tx, 0), Math.max(tz, 0), Math.max(-tz, 0)};
        }

        @Override
        public int nextInt(final int bound) {
            if (at >= draws.length) {
                throw new IllegalStateException("the bloom drew past its scripted "
                        + draws.length + "-int roll: the roll consumes four nextInt("
                        + TaintRules.BLOOM_SCAN_MOD + ") draws (x pair, then z pair)");
            }
            final int value = draws[at++];
            if (value >= bound) {
                throw new IllegalStateException("the bloom asked nextInt(" + bound
                        + ") for a scripted draw of " + value + ": BLOOM_SCAN_MOD is "
                        + TaintRules.BLOOM_SCAN_MOD);
            }
            return value;
        }

        @Override
        public int nextInt() {
            throw new UnsupportedOperationException("the bloom's roll uses nextInt(bound) only");
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("the bloom's roll uses nextInt(bound) only");
        }

        @Override
        public boolean nextBoolean() {
            throw new UnsupportedOperationException("the bloom's roll uses nextInt(bound) only");
        }

        @Override
        public float nextFloat() {
            throw new UnsupportedOperationException("the bloom's roll uses nextInt(bound) only");
        }

        @Override
        public double nextDouble() {
            throw new UnsupportedOperationException("the bloom's roll uses nextInt(bound) only");
        }

        @Override
        public double nextGaussian() {
            throw new UnsupportedOperationException("the bloom's roll uses nextInt(bound) only");
        }

        @Override
        public void setSeed(final long seed) {
            at = 0;
        }

        @Override
        public RandomSource fork() {
            throw new UnsupportedOperationException("the bloom's roll must not fork its source");
        }

        @Override
        public net.minecraft.world.level.levelgen.PositionalRandomFactory forkPositional() {
            throw new UnsupportedOperationException("the bloom's roll must not fork its source");
        }
    }

    /**
     * Paint a square field of one biome around a centre.
     *
     * @param record true to go through {@code setBiomeAtAndSync} — the shipped
     *               conversion path, which records the biome the column had
     *               first (BD-16); false for the raw palette write, which leaves
     *               the column with NO record, the state the plains fallback is
     *               documented for.
     */
    private static void paintBiome(final ServerLevel level, final BlockPos centre, final int radius,
                                   final Holder<Biome> biome, final boolean record) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                final int x = centre.getX() + dx;
                final int z = centre.getZ() + dz;
                if (record) {
                    TaintBiomes.setBiomeAtAndSync(level, x, z, biome);
                } else {
                    TaintBiomes.setBiomeAt(level, x, z, biome);
                }
            }
        }
    }

    /**
     * The identity of the biome palette cell a column's write lands in.
     *
     * <p>The index inside the section's 4×4×4 biome palette is
     * {@code (coord >> 2) & 3}, and that index REPEATS every chunk (16 blocks),
     * so the index alone is not an identity: the cell at index 2 of chunk -848996
     * and the cell at index 2 of chunk -848995 are different cells that share a
     * name. The disc probe paints ±12 columns — two chunks — and a ±7 offset
     * reaches cells in the next chunk, so an index-only key merges them and made
     * the 2026-10-29 "missing cell" reading ambiguous. The chunk coordinate is
     * in the key, and the index stays visible in it.</p>
     */
    private static String cellKeyOf(final int x, final int z) {
        return (x >> 4) + ":" + ((x >> 2) & 3) + "|" + (z >> 4) + ":" + ((z >> 2) & 3);
    }

    /**
     * The distinct palette cells the bloom CAN reach: the cells of the offsets
     * that pass its own gate. One restore rewrites a whole cell, so this is also
     * the number of rolls that can fire — the disc, counted in 4×4 cells.
     */
    private static java.util.Set<String> reachableCells(final BlockPos bloom) {
        final java.util.Set<String> cells = new java.util.TreeSet<>();
        final int limit = TaintRules.BLOOM_SCAN_MOD - 1;
        for (int dx = -limit; dx <= limit; dx++) {
            for (int dz = -limit; dz <= limit; dz++) {
                if (TaintRules.bloomDistanceSquared(dx, dz) <= TaintRules.BLOOM_RADIUS_SQUARED) {
                    cells.add(cellKeyOf(bloom.getX() + dx, bloom.getZ() + dz));
                }
            }
        }
        return cells;
    }

    private static java.util.Set<String> missing(final java.util.Set<String> derived,
                                                 final java.util.Set<String> fired) {
        final java.util.Set<String> out = new java.util.TreeSet<>(derived);
        out.removeAll(fired);
        return out;
    }

    private static int reachableCellCount(final BlockPos bloom) {
        return reachableCells(bloom).size();
    }

    // ------------------------------------------------------------ 1. the disc gate

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void bloomCleansesInsideTheDiscAndNeverBeyondIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The bloom sits on a palette-cell origin (16 is a multiple of 4) so the
        // "must stay tainted" ring is a whole cell away from the farthest cell a
        // ±7 offset can touch: the 4-block granularity of a biome write (BD-15)
        // cannot blur the gate.
        final BlockPos origin = helper.absolutePos(new BlockPos(16, 8, 16));
        // The template origin is NOT 4-aligned in world space (observed: 6183709,
        // 8526245 -> both ≡1 mod 4), so snap to the palette-cell grid instead of
        // assuming the structure placement is aligned.
        final BlockPos bloom = new BlockPos(origin.getX() & ~3, origin.getY(), origin.getZ() & ~3);
        helper.assertTrue((bloom.getX() & 3) == 0 && (bloom.getZ() & 3) == 0,
                "the fixture bloom must sit on a biome palette cell origin, got " + bloom.toShortString());

        final int limit = TaintRules.BLOOM_SCAN_MOD - 1;
        final java.util.Set<String> derived = reachableCells(bloom);
        final int cells = derived.size();

        // ------------------------------------------------ (A) the exhaustive sweep
        // Every offset of the ±7 box, exactly once, with the roll forced
        // (ScriptedRollRandom). Coverage stops being a property of a seed stream:
        // each cell the gate admits is targeted while it is still the taint
        // biome, so it MUST fire, and the engine's fire decision is compared
        // against the rule at all 225 offsets.
        // Raw write: no pre-taint record, so every restore lands on the plains
        // fallback and "cleansed" reads unambiguously as "no longer taint".
        paintBiome(level, bloom, PAINT_RADIUS, TaintBiomes.taintBiomeHolder(level), false);
        helper.assertTrue(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, bloom)),
                "fixture: the bloom's own column must be the taint biome, got "
                        + TaintBiomes.biomeAt(level, bloom).getRegisteredName());

        final java.util.Set<String> sweptCells = new java.util.TreeSet<>();
        final StringBuilder gateMismatch = new StringBuilder();
        int sweptFires = 0;
        int sweptBeyondRadius = 0;
        for (int dx = -limit; dx <= limit; dx++) {
            for (int dz = -limit; dz <= limit; dz++) {
                final Holder<Biome> stored =
                        TaintBiomes.biomeAt(level, bloom.offset(dx, 0, dz));
                final double d2 = TaintRules.bloomDistanceSquared(dx, dz);
                final boolean expect =
                        TaintRules.bloomCleanses(TaintBiomes.isCleansableBiome(stored), d2);
                final boolean fired = TaintManager.etherealBloomCleanse(level, bloom,
                        new ScriptedRollRandom(dx, dz));
                if (fired != expect && gateMismatch.length() < 400) {
                    gateMismatch.append(' ').append(dx).append(',').append(dz)
                            .append("(d2=").append(String.format("%.2f", d2))
                            .append(",biome=").append(stored.getRegisteredName())
                            .append(fired ? ",engine=fired)" : ",engine=skip)");
                }
                if (!fired) {
                    continue;
                }
                sweptFires++;
                sweptCells.add(cellKeyOf(bloom.getX() + dx, bloom.getZ() + dz));
                if (d2 > TaintRules.BLOOM_RADIUS_SQUARED) {
                    sweptBeyondRadius++;
                }
            }
        }
        System.out.println("TC-B2-DIAG sweep bloom=" + bloom.toShortString()
                + " x&15=" + (bloom.getX() & 15) + " z&15=" + (bloom.getZ() & 15)
                + " cell=" + cellKeyOf(bloom.getX(), bloom.getZ())
                + " offsets=225 fires=" + sweptFires + " cellsFired=" + sweptCells.size()
                + " derived=" + cells + " MISSING=" + missing(derived, sweptCells)
                + " gateMismatches=" + (gateMismatch.length() == 0 ? 0 : gateMismatch));

        helper.assertTrue(gateMismatch.length() == 0,
                "the bloom's fire decision must equal TaintRules.bloomCleanses at EVERY offset"
                        + " of the ±" + limit + " box (biome gate + squared radius), mismatches:"
                        + gateMismatch);
        helper.assertTrue(sweptBeyondRadius == 0,
                "the sweep fired " + sweptBeyondRadius + " times on offsets past the squared"
                        + " radius " + TaintRules.BLOOM_RADIUS_SQUARED);
        helper.assertTrue(sweptCells.equals(derived),
                "the exhaustive sweep of all " + (2 * limit + 1) * (2 * limit + 1) + " offsets must"
                        + " cleanse every cell the disc admits (" + cells + " of them): fired"
                        + " " + sweptCells.size() + " cells; FIRED=" + sweptCells
                        + " DERIVED=" + derived + " MISSING=" + missing(derived, sweptCells));

        // ------------------------------------------------- (B) the seeded roll stream
        // The fixture the probe is known for, and the one the 2026-10-29 run
        // misread: the rolls are the bloom's own draws over ROLLS seeds, and the
        // per-cell telemetry below names the cells that fired. Seeds are
        // decorrelated (rollSeed) — with consecutive seeds the x offset never
        // reaches -5..-7 and this leg misses exactly four cells.
        paintBiome(level, bloom, PAINT_RADIUS, TaintBiomes.taintBiomeHolder(level), false);

        int fired = 0;
        int beyondScanBox = 0;
        int beyondRadius = 0;
        final java.util.Set<String> firedCells = new java.util.TreeSet<>();
        final java.util.Map<String, int[]> perCell = new java.util.TreeMap<>();
        final java.util.Map<String, StringBuilder> detail = new java.util.TreeMap<>();
        int minTx = Integer.MAX_VALUE;
        int maxTx = Integer.MIN_VALUE;
        int minTz = Integer.MAX_VALUE;
        int maxTz = Integer.MIN_VALUE;
        int negativeSide = 0;
        for (long index = 1; index <= ROLLS; index++) {
            final long seed = rollSeed(index);
            final BlockPos off = mirroredBloomOffset(seed);
            final String cell = cellKeyOf(bloom.getX() + off.getX(), bloom.getZ() + off.getZ());
            final Holder<Biome> read =
                    TaintBiomes.biomeAt(level, bloom.offset(off.getX(), 0, off.getZ()));
            final boolean rollFired = TaintManager.etherealBloomCleanse(level, bloom,
                    RandomSource.create(seed));
            minTx = Math.min(minTx, off.getX());
            maxTx = Math.max(maxTx, off.getX());
            minTz = Math.min(minTz, off.getZ());
            maxTz = Math.max(maxTz, off.getZ());
            if (off.getX() <= -(TaintRules.BLOOM_SCAN_MOD - 2)) {
                negativeSide++;
            }
            final int[] st = perCell.computeIfAbsent(cell, k -> new int[3]);
            st[0]++;
            if (!TaintBiomes.isTaintBiome(read)) {
                st[2]++;
            }
            if (rollFired) {
                st[1]++;
            }
            final StringBuilder d = detail.computeIfAbsent(cell, k -> new StringBuilder());
            if (d.length() < 260) {
                d.append(" r").append(index).append('(').append(off.getX()).append(',').append(off.getZ())
                        .append(")=").append(read.getRegisteredName()
                                .replace("minecraft:", "").replace("thaumcraft:biome.", ""))
                        .append(rollFired ? "*" : "");
            }
            if (!rollFired) {
                continue;
            }
            fired++;
            firedCells.add(cell);
            // The gate, measured on the offset the original's own draws picked.
            if (Math.max(Math.abs(off.getX()), Math.abs(off.getZ())) > TaintRules.BLOOM_SCAN_MOD - 1) {
                beyondScanBox++;
            }
            if (TaintRules.bloomDistanceSquared(off.getX(), off.getZ()) > TaintRules.BLOOM_RADIUS_SQUARED) {
                beyondRadius++;
            }
        }
        // The diagnostic that settles a "cell never cleanses" report: the span of
        // the stream decides coverage, so the log states it next to the cell list.
        System.out.println("TC-B2-DIAG seeded bloom=" + bloom.toShortString()
                + " x&15=" + (bloom.getX() & 15) + " z&15=" + (bloom.getZ() & 15)
                + " cell=" + cellKeyOf(bloom.getX(), bloom.getZ())
                + " rolls=" + ROLLS + " tx=[" + minTx + "," + maxTx + "] tz=[" + minTz + "," + maxTz + "]"
                + " rollsAtTx<=-5=" + negativeSide + " cellsExercised=" + perCell.size()
                + " fired=" + fired + " cellsFired=" + firedCells.size()
                + " MISSING=" + missing(derived, firedCells));
        for (final java.util.Map.Entry<String, int[]> e : perCell.entrySet()) {
            final int[] st = e.getValue();
            System.out.println("TC-B2-DIAG cell " + e.getKey() + " rolls=" + st[0]
                    + " fires=" + st[1] + " nonTaintReads=" + st[2] + " detail=" + detail.get(e.getKey()));
        }

        // (1) geometry of every roll that fired: |offset| > 8 is unreachable (the
        // scan box tops out at ±7) and so is anything past the squared radius.
        helper.assertTrue(beyondScanBox == 0 && beyondRadius == 0,
                "the bloom cleansed " + beyondScanBox + " column(s) outside its ±"
                        + (TaintRules.BLOOM_SCAN_MOD - 1) + " scan box and " + beyondRadius
                        + " outside its squared radius " + TaintRules.BLOOM_RADIUS_SQUARED
                        + ", over " + fired + " firing rolls: |offset| > 8 must be unreachable");

        // (2) the disc is a disc: the radius gate trims scan-box corners off it
        // ((7,7) is 98.25 > 81), and the cells it does admit all get cleansed —
        // exactly once each, because one restore rewrites a whole 4x4 cell.
        int trimmedCorners = 0;
        for (int dx = -limit; dx <= limit; dx++) {
            for (int dz = -limit; dz <= limit; dz++) {
                if (TaintRules.bloomDistanceSquared(dx, dz) > TaintRules.BLOOM_RADIUS_SQUARED) {
                    trimmedCorners++;
                }
            }
        }
        helper.assertTrue(trimmedCorners > 0,
                "the squared-radius gate must trim scan-box corners (7,7) off the disc, got "
                        + trimmedCorners + " trimmed offsets");
        helper.assertTrue(minTx == -limit && maxTx == limit && minTz == -limit && maxTz == limit,
                "the seeded roll stream must span the whole ±" + limit + " scan box on BOTH axes"
                        + " (a stream that does not cannot cover the disc): tx=[" + minTx + "," + maxTx
                        + "] tz=[" + minTz + "," + maxTz + "] over " + ROLLS
                        + " rolls; rolls with tx <= -5: " + negativeSide
                        + " — see rollSeed(), Java's LCG freezes the first draw across"
                        + " consecutive seeds");
        helper.assertTrue(fired == cells && firedCells.size() == cells,
                "the disc is " + cells + " biome palette cells (the scan box is " + limit
                        + ", so 4x4 cells of 4x4 blocks, and one restore cleanses a whole cell): "
                        + "the bloom fired " + fired + " times over " + firedCells.size()
                        + " distinct cells in " + ROLLS + " rolls; FIRED=" + firedCells
                        + " DERIVED=" + derived
                        + " MISSING=" + missing(derived, firedCells)
                        + " | tx=[" + minTx + "," + maxTx + "] tz=[" + minTz + "," + maxTz
                        + "] rollsAtTx<=-5=" + negativeSide + " cellsExercised=" + perCell.size()
                        + " (coverage of the stream, not of the engine: see rollSeed)");

        // (3) the core of the disc is genuinely cleansed: every column within
        // |offset| <= CORE_RADIUS reads something other than taint.
        final StringBuilder coreMisses = new StringBuilder();
        int core = 0;
        for (int dx = -CORE_RADIUS; dx <= CORE_RADIUS; dx++) {
            for (int dz = -CORE_RADIUS; dz <= CORE_RADIUS; dz++) {
                core++;
                if (TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, bloom.offset(dx, 0, dz)))) {
                    coreMisses.append(' ').append(dx).append(',').append(dz).append("(d2=")
                            .append(String.format("%.2f", TaintRules.bloomDistanceSquared(dx, dz)))
                            .append(')');
                }
            }
        }
        helper.assertTrue(coreMisses.length() == 0,
                "every column of the bloom's core (|offset| <= " + CORE_RADIUS + ", " + core
                        + " columns, " + ROLLS + " rolls) must be cleansed; still taint:" + coreMisses);

        // (4) NEVER beyond the disc, stated at CELL granularity: a painted
        // column whose palette cell no qualifying ±7 offset reaches must still be
        // the taint biome. Cell granularity is the honest unit because a biome
        // write is 4x4 blocks (BD-15): one 4x4 cell spans the bloom's negative
        // side (x offsets -7..-5, chunk-local index 1 of the bloom's chunk) AND
        // columns 8..11 blocks on its POSITIVE side (index 1 of the next chunk
        // shares the same palette index). Those columns share a cell with a
        // legitimate fire, so a fire there is the documented BD-15 footprint, not
        // a leak. Column-offset filtering hid that for a whole investigation.
        int ring = 0;
        int ringCleansed = 0;
        final StringBuilder leak = new StringBuilder();
        for (int dx = -PAINT_RADIUS; dx <= PAINT_RADIUS; dx++) {
            for (int dz = -PAINT_RADIUS; dz <= PAINT_RADIUS; dz++) {
                final String cell = cellKeyOf(bloom.getX() + dx, bloom.getZ() + dz);
                if (derived.contains(cell)) {
                    continue; // the disc admits this cell: cleansing it is correct
                }
                ring++;
                final Holder<Biome> at = TaintBiomes.biomeAt(level, bloom.offset(dx, 0, dz));
                if (!TaintBiomes.isTaintBiome(at)) {
                    ringCleansed++;
                    if (leak.length() < 400) {
                        leak.append(' ').append(dx).append(',').append(dz).append("(cell ").append(cell)
                                .append('=').append(at.getRegisteredName()).append(')');
                    }
                }
            }
        }
        helper.assertTrue(ringCleansed == 0,
                "the bloom must never cleanse a column whose palette cell NO qualifying ±"
                        + limit + " offset reaches: " + ringCleansed + " of " + ring
                        + " such columns were rewritten to" + leak);
        helper.succeed();
    }

    // ------------------------------------------------ 2. the pre-taint record (BD-16)

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void bloomRestoresThePreTaintBiomeFromTheRecord(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final net.minecraft.core.Registry<Biome> registry =
                level.registryAccess().registryOrThrow(Registries.BIOME);
        final Holder<Biome> taint = TaintBiomes.taintBiomeHolder(level);
        final Holder<Biome> forest = registry.getHolderOrThrow(Biomes.FOREST);
        final Holder<Biome> plains = registry.getHolderOrThrow(Biomes.PLAINS);
        helper.assertTrue(!forest.is(TaintBiomes.TAINT_BIOME) && !plains.is(TaintBiomes.TAINT_BIOME),
                "fixture: neither forest nor plains may be a cleansable biome, cleansable set "
                        + TaintBiomes.CLEANSABLE_BIOMES);

        // --- leg A: a column converted THROUGH the shipped conversion path has a
        // record, and the bloom gives THAT biome back — not plains.
        final BlockPos bloomA = helper.absolutePos(new BlockPos(8, 8, 8));
        paintBiome(level, bloomA, TaintRules.BLOOM_SCAN_MOD - 1, forest, false); // pre-taint: forest
        paintBiome(level, bloomA, TaintRules.BLOOM_SCAN_MOD - 1, taint, true);   // conversion records forest
        helper.assertTrue(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, bloomA)),
                "leg A fixture: the bloom column must read taint after the conversion, got "
                        + TaintBiomes.biomeAt(level, bloomA).getRegisteredName());

        int firedA = 0;
        int toForest = 0;
        final StringBuilder recordFails = new StringBuilder();
        for (long index = 1; index <= ROLLS; index++) {
            final long seed = rollSeed(index);
            final BlockPos off = mirroredBloomOffset(seed);
            if (!TaintManager.etherealBloomCleanse(level, bloomA, RandomSource.create(seed))) {
                continue;
            }
            firedA++;
            final Holder<Biome> after =
                    TaintBiomes.biomeAt(level, bloomA.offset(off.getX(), 0, off.getZ()));
            if (after.is(Biomes.FOREST)) {
                toForest++;
            } else if (recordFails.length() < 300) {
                recordFails.append(' ').append(off.getX()).append(',').append(off.getZ())
                        .append('=').append(after.getRegisteredName());
            }
        }
        helper.assertTrue(firedA > 0 && toForest == firedA,
                "BD-16: every cleansing roll must write back the biome recorded when taint converted "
                        + "the column (forest here), got " + toForest + '/' + firedA
                        + " columns restored to forest; the restore answered" + recordFails
                        + " (plains=" + plains.getRegisteredName() + ", taint=" + taint.getRegisteredName() + ")");

        // A restore USES UP the record: the next restore on that column answers
        // from the palette, which now holds the restored forest, so it keeps forest.
        TaintManager.restoreBiome(level, bloomA.getX(), bloomA.getY(), bloomA.getZ());
        helper.assertTrue(TaintBiomes.biomeAt(level, bloomA).is(Biomes.FOREST),
                "the record is consumed by the restore that used it, so a second restore answers from "
                        + "the palette; got " + TaintBiomes.biomeAt(level, bloomA).getRegisteredName());

        // --- leg B: a column with NO record falls back to plains, through the
        // bloom path. Raw writes only, so the conversion never ran, nothing was
        // recorded, and the palette answer IS the taint biome — the case the
        // original's `if (biome == biomeTaint) biome = Plains` line covers.
        final BlockPos bloomB = helper.absolutePos(new BlockPos(24, 8, 8));
        paintBiome(level, bloomB, PAINT_RADIUS, taint, false);
        int firedB = 0;
        int toPlains = 0;
        final StringBuilder fallbackFails = new StringBuilder();
        for (long index = 1; index <= ROLLS; index++) {
            final long seed = rollSeed(index);
            final BlockPos off = mirroredBloomOffset(seed);
            if (!TaintManager.etherealBloomCleanse(level, bloomB, RandomSource.create(seed))) {
                continue;
            }
            firedB++;
            final Holder<Biome> after =
                    TaintBiomes.biomeAt(level, bloomB.offset(off.getX(), 0, off.getZ()));
            if (after.is(Biomes.PLAINS)) {
                toPlains++;
            } else if (fallbackFails.length() < 300) {
                fallbackFails.append(' ').append(off.getX()).append(',').append(off.getZ())
                        .append('=').append(after.getRegisteredName());
            }
        }
        helper.assertTrue(firedB > 0 && toPlains == firedB,
                "the documented plains fallback: with no pre-taint record the restore writes plains, got "
                        + toPlains + '/' + firedB + " columns restored to plains; the restore answered"
                        + fallbackFails);

        // --- leg C: the primitive on one column, both halves read back at that
        // column. Recoded explicitly so the two cases cannot share a palette cell.
        final BlockPos recorded = helper.absolutePos(new BlockPos(8, 8, 24));
        final BlockPos bare = helper.absolutePos(new BlockPos(12, 8, 24));
        helper.assertTrue(!cellKeyOf(recorded.getX(), recorded.getZ()).equals(cellKeyOf(bare.getX(), bare.getZ())),
                "fixture: leg C's two columns must be in different palette cells, got "
                        + cellKeyOf(recorded.getX(), recorded.getZ()) + " and "
                        + cellKeyOf(bare.getX(), bare.getZ()));

        paintBiome(level, recorded, 0, forest, false);
        TaintBiomes.setBiomeAtAndSync(level, recorded.getX(), recorded.getZ(), taint); // records forest
        TaintManager.restoreBiome(level, recorded.getX(), recorded.getY(), recorded.getZ());
        helper.assertTrue(TaintBiomes.biomeAt(level, recorded).is(Biomes.FOREST),
                "restoreBiome on a recorded column must give the recorded biome back, got "
                        + TaintBiomes.biomeAt(level, recorded).getRegisteredName());

        TaintBiomes.setBiomeAt(level, bare.getX(), bare.getZ(), taint); // no record at all
        helper.assertTrue(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, bare)),
                "leg C fixture: the bare column must read taint, got "
                        + TaintBiomes.biomeAt(level, bare).getRegisteredName());
        TaintManager.restoreBiome(level, bare.getX(), bare.getY(), bare.getZ());
        helper.assertTrue(TaintBiomes.biomeAt(level, bare).is(Biomes.PLAINS),
                "restoreBiome on a column with no record must fall back to plains, got "
                        + TaintBiomes.biomeAt(level, bare).getRegisteredName());
        helper.succeed();
    }

    // ------------------------------------------ 3. only variant 4 gets a ticker

    @GameTest(template = "gametest_pad_32")
    public static void onlyTheBloomVariantGetsABlockEntityAndTicker(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockCustomPlant plant = CatalogTaint.BLOCK_CUSTOM_PLANT.get();
        final IntegerProperty variant =
                (IntegerProperty) plant.getStateDefinition().getProperty("variant");
        final java.util.List<Integer> metas = new java.util.ArrayList<>(
                variant == null ? java.util.List.<Integer>of() : variant.getPossibleValues());
        java.util.Collections.sort(metas);
        helper.assertTrue(metas.size() == 6 && metas.get(0) == 0
                        && metas.get(metas.size() - 1) == BlockCustomPlant.META_MANASHROOM,
                "the plant family must carry the original's six metas 0..5, got " + metas);

        final StringBuilder bloomInert = new StringBuilder();
        final StringBuilder othersLive = new StringBuilder();
        for (final int meta : metas) {
            final BlockState state = plant.defaultBlockState().setValue(BlockCustomPlant.VARIANT, meta);
            final BlockEntityTicker<?> ticker = plant.getTicker(level, state, CatalogTaint.BLOOM.get());
            final BlockEntity created = plant.newBlockEntity(helper.absolutePos(BlockPos.ZERO), state);
            if (meta == BlockCustomPlant.META_BLOOM) {
                if (ticker == null) {
                    bloomInert.append(" meta 4 answered getTicker = null (the bloom's ticker is the"
                            + " cleansing engine — TileEtherealBloom.tick)");
                }
                if (!(created instanceof TileEtherealBloom)) {
                    bloomInert.append(" meta 4 newBlockEntity -> ").append(created);
                }
            } else if (ticker != null || created != null) {
                othersLive.append(" meta ").append(meta).append(" ticker=").append(ticker)
                        .append(" tile=").append(created);
            }
        }
        helper.assertTrue(bloomInert.length() == 0, "the Ethereal Bloom (meta 4) needs both a ticker and"
                + " a tile;" + bloomInert);
        helper.assertTrue(othersLive.length() == 0,
                "only meta 4 may be ticked or carry a tile — the other five metas of blockcustomplant"
                        + " (greatwood / silverwood sapling, shimmerleaf, cinderpearl, manashroom) are"
                        + " inert plants. 1.21.1 reports hasBlockEntity() = true for EVERY state of an"
                        + " EntityBlock, so getTicker / newBlockEntity are the only per-variant gate:"
                        + othersLive);

        // The shipped world path: place the bloom on its own soil (the original's
        // Cave plant type — mycelium / podzol) and drive the ticker the level
        // drives. The tick seeds the counter from nextInt(100) and increments.
        final BlockPos pos = helper.absolutePos(new BlockPos(8, 8, 8));
        level.setBlockAndUpdate(pos.below(), Blocks.MYCELIUM.defaultBlockState());
        level.setBlockAndUpdate(pos, plant.defaultBlockState().setValue(BlockCustomPlant.VARIANT,
                BlockCustomPlant.META_BLOOM));
        final BlockEntity be = level.getBlockEntity(pos);
        helper.assertTrue(be instanceof TileEtherealBloom,
                "a placed bloom (meta 4) must carry a TileEtherealBloom, got " + be);
        helper.assertTrue(CatalogTaint.BLOOM.get().isValid(level.getBlockState(pos)),
                "the bloom's BlockEntityType must accept the plant's meta-4 state, got "
                        + level.getBlockState(pos));
        @SuppressWarnings("unchecked")
        final BlockEntityTicker<TileEtherealBloom> bloomTicker =
                (BlockEntityTicker<TileEtherealBloom>) plant.getTicker(level, level.getBlockState(pos),
                        CatalogTaint.BLOOM.get());
        helper.assertTrue(bloomTicker != null, "the placed bloom must answer getTicker");
        final int before = ((TileEtherealBloom) be).getCounter();
        for (int i = 0; i < TaintRules.BLOOM_PERIOD; i++) {
            bloomTicker.tick(level, pos, level.getBlockState(pos), (TileEtherealBloom) be);
        }
        final int after = ((TileEtherealBloom) be).getCounter();
        helper.assertTrue(after - before >= TaintRules.BLOOM_PERIOD && after > before,
                "the ticker must run TileEtherealBloom.tick — the counter seeds once from"
                        + " nextInt(" + TaintRules.BLOOM_PHASE_MOD + ") on the first tick and then"
                        + " increments every tick, so " + TaintRules.BLOOM_PERIOD + " ticker calls move"
                        + " it by at least " + TaintRules.BLOOM_PERIOD + "; it went " + before + " -> "
                        + after);

        // A plant of another meta, placed in the world, carries no tile and no
        // ticker, so the level's tick list never sees it.
        final BlockPos plain = helper.absolutePos(new BlockPos(12, 8, 8));
        level.setBlockAndUpdate(plain.below(), Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(plain, plant.defaultBlockState().setValue(BlockCustomPlant.VARIANT, 2));
        helper.assertTrue(level.getBlockState(plain).getValue(BlockCustomPlant.VARIANT) == 2,
                "fixture: meta 2 must be in the world at " + plain.toShortString() + ", got "
                        + level.getBlockState(plain));
        helper.assertTrue(level.getBlockEntity(plain) == null,
                "shimmerleaf (meta 2) must not carry a block entity, got " + level.getBlockEntity(plain));
        helper.assertTrue(plant.getTicker(level, level.getBlockState(plain), CatalogTaint.BLOOM.get()) == null,
                "shimmerleaf (meta 2) must not be ticked in the world");

        // The placed bloom's own column reads back through the palette, the read
        // the cleansing probes use (Level#getBiome is stale at this scale).
        final Holder<Biome> at = level.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
        helper.assertTrue(at.value() != null, "the palette read answers for the bloom column, got " + at);
        helper.succeed();
    }

    // ------------------------------------------------ TC-M5-04-s-b2 write cell
    //
    // VERDICT (kept as a standing probe, not scratch): where does ONE biome write
    // land, in palette-cell space?
    //
    // The disc probe reported 12 of 16 disc cells firing and blamed the write
    // path. Measured here, the write path is clean: every single-column write
    // answers for exactly the 4 columns (x & ~3)..(x & ~3)+3 of ITS OWN column
    // (41/41 offsets swept across two chunk boundaries, negative offsets
    // included), and painting the disc probe's field taints 625 of 625 columns.
    // The write and the read agree on the cell. The disc probe's missing cells
    // were its own roll stream (see rollSeed).
    //
    // This probe is the mutation guard for the OTHER side of the claim: the
    // quart-space cell index in TaintBiomes.setBiomeAt (BD-15). Revert that index
    // to the 1.7.10 `x & 3` byte write and the footprint lines go off-grid.
    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void diagnoseWhereASingleBiomeWriteLands(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(new BlockPos(16, 8, 16));
        final BlockPos bloom = new BlockPos(origin.getX() & ~3, origin.getY(), origin.getZ() & ~3);
        final net.minecraft.core.Registry<Biome> registry =
                level.registryAccess().registryOrThrow(Registries.BIOME);
        final Holder<Biome> forest = registry.getHolderOrThrow(Biomes.FOREST);
        final Holder<Biome> plains = registry.getHolderOrThrow(Biomes.PLAINS);
        final int y = bloom.getY();
        System.out.println("TC-B2-DIAG bloom=" + bloom.toShortString()
                + " x&15=" + (bloom.getX() & 15) + " z&15=" + (bloom.getZ() & 15)
                + " cell=" + cellKeyOf(bloom.getX(), bloom.getZ())
                + " build=" + level.getMinBuildHeight() + ".." + level.getMaxBuildHeight()
                + " sections=" + level.getChunk(bloom).getSections().length);

        final StringBuilder offGrid = new StringBuilder();
        for (int dx = -20; dx <= 20; dx++) {
            for (int rx = -24; rx <= 24; rx++) {
                for (int rz = -4; rz <= 4; rz++) {
                    TaintBiomes.setBiomeAt(level, bloom.getX() + rx, bloom.getZ() + rz, plains);
                }
            }
            final int x = bloom.getX() + dx;
            TaintBiomes.setBiomeAt(level, x, bloom.getZ(), forest);
            int first = Integer.MAX_VALUE;
            int last = Integer.MIN_VALUE;
            int n = 0;
            for (int rx = -24; rx <= 24; rx++) {
                if (TaintBiomes.biomeAt(level, new BlockPos(bloom.getX() + rx, y, bloom.getZ()))
                        .is(Biomes.FOREST)) {
                    if (first == Integer.MAX_VALUE) {
                        first = bloom.getX() + rx;
                    }
                    last = bloom.getX() + rx;
                    n++;
                }
            }
            final int expectFirst = x & ~3;
            System.out.println("TC-B2-DIAG single-write dx=" + dx + " x=" + x
                    + " chunk=" + (x >> 4) + " localx=" + (x & 15)
                    + " cellX=" + ((x >> 2) & 3) + " cellZ=" + ((bloom.getZ() >> 2) & 3)
                    + " expectFootprint=" + expectFirst + ".." + (expectFirst + 3)
                    + " footprint=" + (n == 0 ? "NONE" : first + ".." + last) + " n=" + n);
            if (n != 4 || first != expectFirst) {
                offGrid.append(" dx=").append(dx).append(" n=").append(n)
                        .append(" footprint=").append(n == 0 ? "NONE" : first + ".." + last)
                        .append(" expected ").append(expectFirst).append("..").append(expectFirst + 3);
            }
        }
        System.out.println("TC-B2-DIAG writes whose footprint is NOT the source column's own"
                + " 4-block cell:" + (offGrid.length() == 0 ? " none" : offGrid));

        // The disc probe's own paint, read back column by column: a column the
        // paint did not taint can never be a fire target, which is what a
        // missing disc cell looks like from the bloom's side.
        for (int rx = -24; rx <= 24; rx++) {
            for (int rz = -24; rz <= 24; rz++) {
                TaintBiomes.setBiomeAt(level, bloom.getX() + rx, bloom.getZ() + rz, plains);
            }
        }
        paintBiome(level, bloom, PAINT_RADIUS, TaintBiomes.taintBiomeHolder(level), false);
        final StringBuilder notTaint = new StringBuilder();
        int misses = 0;
        for (int dx = -PAINT_RADIUS; dx <= PAINT_RADIUS; dx++) {
            for (int dz = -PAINT_RADIUS; dz <= PAINT_RADIUS; dz++) {
                final Holder<Biome> read = TaintBiomes.biomeAt(level,
                        new BlockPos(bloom.getX() + dx, y, bloom.getZ() + dz));
                if (!TaintBiomes.isTaintBiome(read)) {
                    misses++;
                    if (notTaint.length() < 900) {
                        notTaint.append(' ').append(dx).append(',').append(dz)
                                .append("(cell ").append(cellKeyOf(bloom.getX() + dx, bloom.getZ() + dz))
                                .append('=').append(read.getRegisteredName()).append(')');
                    }
                }
            }
        }
        System.out.println("TC-B2-DIAG pad paint read-back: " + misses + " of "
                + (2 * PAINT_RADIUS + 1) * (2 * PAINT_RADIUS + 1)
                + " painted columns do NOT read taint;" + notTaint);

        helper.assertTrue(offGrid.length() == 0,
                "every single-column biome write must land in the palette cell the read reads — the"
                        + " 4 columns (x & ~3)..(x & ~3)+3 of the source column; off-grid writes:"
                        + offGrid);
        helper.assertTrue(misses == 0,
                "painting the disc probe's field must make every painted column read the taint"
                        + " biome (a column the paint missed can never be a bloom fire target): "
                        + misses + " of " + (2 * PAINT_RADIUS + 1) * (2 * PAINT_RADIUS + 1)
                        + " read something else;" + notTaint);
        helper.succeed();
    }
}
