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
 * held BEFORE taint overwrote it ({@link TaintBiomes#takeGeneratedBiome},
 * BD-16); plains is the fallback for a column with no record, which is exactly
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
 * squared radius are pinned exactly, not as a statistics band; the coverage
 * floor uses {@link #ROLLS} rolls, where the rarest core offset
 * ((±3, ±3), P = (5/64)²) has ~15 expected draws, i.e. a 1-in-10⁶ chance of a
 * miss. (2) The 4-block palette cell: one restore rewrites the whole cell of
 * the column it fired on (BD-15), so the disc is a set of cells, a cell is
 * cleansed at most once, and "never cleansed beyond the disc" is asserted for
 * columns whose CELL no ±7 offset can reach.</p>
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
    private static final int ROLLS = 2500;

    /**
     * The offset the bloom picks on this seed, by replaying the original's draw
     * order: x's pair first, then z's pair, each {@code nextInt(8) -
     * nextInt(8)}. Java evaluates constructor arguments left to right, so this
     * draws in exactly that order.
     */
    private static BlockPos mirroredBloomOffset(final long seed) {
        final RandomSource mirror = RandomSource.create(seed);
        return new BlockPos(
                TaintRules.bloomOffset(mirror.nextInt(TaintRules.BLOOM_SCAN_MOD),
                        mirror.nextInt(TaintRules.BLOOM_SCAN_MOD)),
                0,
                TaintRules.bloomOffset(mirror.nextInt(TaintRules.BLOOM_SCAN_MOD),
                        mirror.nextInt(TaintRules.BLOOM_SCAN_MOD)));
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

    /** The palette cell key a column's write lands in — {@code (coord >> 2) & 3}. */
    private static String cellKeyOf(final int x, final int z) {
        return ((x >> 2) & 3) + "_" + ((z >> 2) & 3);
    }

    /**
     * The distinct palette cells the bloom CAN reach: the cells of the offsets
     * that pass its own gate. One restore rewrites a whole cell, so this is also
     * the number of rolls that can fire — the disc, counted in 4×4 cells.
     */
    /** The derived cell set, for the failure message (a parity pin should name cells). */
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
        final java.util.Set<String> cells = new java.util.TreeSet<>();
        final int limit = TaintRules.BLOOM_SCAN_MOD - 1;
        for (int dx = -limit; dx <= limit; dx++) {
            for (int dz = -limit; dz <= limit; dz++) {
                if (TaintRules.bloomDistanceSquared(dx, dz) <= TaintRules.BLOOM_RADIUS_SQUARED) {
                    cells.add(cellKeyOf(bloom.getX() + dx, bloom.getZ() + dz));
                }
            }
        }
        return cells.size();
    }

    // ------------------------------------------------------------ 1. the disc gate

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void bloomCleansesInsideTheDiscAndNeverBeyondIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The bloom sits on a palette-cell origin (16 is a multiple of 4) so the
        // "must stay tainted" ring at |offset| >= 8 is a whole cell away from the
        // farthest cell a ±7 offset can touch: the 4-block granularity of a
        // biome write (BD-15) cannot blur the gate.
        final BlockPos origin = helper.absolutePos(new BlockPos(16, 8, 16));
        // The template origin is NOT 4-aligned in world space (observed: 6183709,
        // 8526245 -> both ≡1 mod 4), so snap to the palette-cell grid instead of
        // assuming the structure placement is aligned.
        final BlockPos bloom = new BlockPos(origin.getX() & ~3, origin.getY(), origin.getZ() & ~3);
        helper.assertTrue((bloom.getX() & 3) == 0 && (bloom.getZ() & 3) == 0,
                "the fixture bloom must sit on a biome palette cell origin, got " + bloom.toShortString());

        // Raw write: no pre-taint record, so every restore lands on the plains
        // fallback and "cleansed" reads unambiguously as "no longer taint".
        paintBiome(level, bloom, PAINT_RADIUS, TaintBiomes.taintBiomeHolder(level), false);
        helper.assertTrue(TaintBiomes.isTaintBiome(TaintBiomes.biomeAt(level, bloom)),
                "fixture: the bloom's own column must be the taint biome, got "
                        + TaintBiomes.biomeAt(level, bloom).getRegisteredName());

        int fired = 0;
        int beyondScanBox = 0;
        int beyondRadius = 0;
        final java.util.Set<String> firedCells = new java.util.TreeSet<>();
        // TC-M5-04-s-b2 per-cell telemetry: how many rolls LANDED on each cell,
        // how many of those reads were not the taint biome (so the engine's
        // biome gate refused them), and how many fired. The probe reads the
        // same palette the engine is about to read, on the same offset, so a
        // cell that shows rolls > 0 and fires == 0 is a gate story, not luck.
        final java.util.Map<String, int[]> perCell = new java.util.TreeMap<>();
        final java.util.Map<String, StringBuilder> detail = new java.util.TreeMap<>();
        for (long seed = 1; seed <= ROLLS; seed++) {
            final BlockPos off = mirroredBloomOffset(seed);
            final String cell = cellKeyOf(bloom.getX() + off.getX(), bloom.getZ() + off.getZ());
            final Holder<Biome> read = TaintBiomes.biomeAt(level, bloom.offset(off.getX(), 0, off.getZ()));
            final boolean rollFired = TaintManager.etherealBloomCleanse(level, bloom, RandomSource.create(seed));
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
                d.append(" r").append(seed).append('(').append(off.getX()).append(',').append(off.getZ())
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
        System.out.println("TC-B2-DIAG disc bloom=" + bloom.toShortString()
                + " x&15=" + (bloom.getX() & 15) + " z&15=" + (bloom.getZ() & 15)
                + " cell=" + cellKeyOf(bloom.getX(), bloom.getZ()));
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
        final int cells = reachableCellCount(bloom);
        int trimmedCorners = 0;
        final int limit = TaintRules.BLOOM_SCAN_MOD - 1;
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
        helper.assertTrue(fired == cells && firedCells.size() == cells,
                "the disc is " + cells + " biome palette cells (the scan box is " + limit
                        + ", so 4x4 cells of 4x4 blocks, and one restore cleanses a whole cell): "
                        + "the bloom fired " + fired + " times over " + firedCells.size()
                        + " distinct cells in " + ROLLS + " rolls; FIRED=" + firedCells
                        + " DERIVED=" + reachableCells(bloom)
                        + " MISSING=" + missing(reachableCells(bloom), firedCells)
                        + " (each derived cell has >=6 qualifying offsets of 225, so a miss"
                        + " over " + ROLLS + " rolls is not a coverage accident)");

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

        // (4) NEVER at |offset| > 8: every painted column whose palette cell no
        // ±7 offset can reach is still the taint biome.
        int ring = 0;
        int ringCleansed = 0;
        final StringBuilder leak = new StringBuilder();
        for (int dx = -PAINT_RADIUS; dx <= PAINT_RADIUS; dx++) {
            for (int dz = -PAINT_RADIUS; dz <= PAINT_RADIUS; dz++) {
                if (Math.abs(dx) < TaintRules.BLOOM_SCAN_MOD && Math.abs(dz) < TaintRules.BLOOM_SCAN_MOD) {
                    continue; // inside the scan box — not this assertion
                }
                ring++;
                final Holder<Biome> at = TaintBiomes.biomeAt(level, bloom.offset(dx, 0, dz));
                if (!TaintBiomes.isTaintBiome(at)) {
                    ringCleansed++;
                    if (leak.length() < 400) {
                        leak.append(' ').append(dx).append(',').append(dz).append('=').append(at.getRegisteredName());
                    }
                }
            }
        }
        helper.assertTrue(ringCleansed == 0,
                "the bloom must never cleanse a column at |offset| >= " + TaintRules.BLOOM_SCAN_MOD
                        + ": " + ringCleansed + " of " + ring + " ring columns were rewritten to" + leak);
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
        for (long seed = 1; seed <= ROLLS; seed++) {
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
        for (long seed = 1; seed <= ROLLS; seed++) {
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
    // Where does ONE biome write land, in palette-cell space?
    //
    // The disc probe reports 12 of its 16 disc cells firing, with the whole x
    // column of cells at key 2 (the bloom's negative x side) missing, while
    // its core assertion — every column at |offset| <= 3 reads cleansed, which
    // INCLUDES the key-2 columns x offsets -3..-1 — passes. Those two facts
    // are only compatible if the write path and the read path do not agree on
    // which palette cell a column lives in. So measure it: reset a strip to
    // plains, write FOREST to ONE column, read the strip back, and print the
    // run of columns that answer forest. That run IS the footprint of the
    // write, and the palette cell of the source column is `(x >> 2) & 3` —
    // a correct write answers for exactly the 4 columns x & ~3 .. x & ~3 + 3.
    // Swept across a chunk boundary (dx -20..20) so a negative-offset,
    // section-base-disagreeing case cannot hide.
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
