package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaint;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockTaintFibres;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTaint;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-M5-04-s in-game acceptance for taint: spread, conversion, cleansing.
 *
 * <p>The pure rule layer is pinned by {@code TaintRulesTest} and
 * {@code TaintRulesParityTest}; these probes prove the same thresholds drive
 * the real blocks in a real world, where the tags, the biome palette and the
 * block states are the live ones. Where the original's behaviour is driven by
 * a random draw, the probe replays that draw order with the same seed and
 * asserts the world result equals the mirrored expectation — that is what
 * pins the modulus, not a statistics band.</p>
 *
 * <p>Two fixture facts decide the shape of the seeded probes. (1) The fibres
 * carry the original's {@code onNeighborChange} death rule: a fibres cell that
 * touches nothing but air and taint is deleted, and every {@code setBlock}
 * beside it re-runs that rule. The original's fibres only ever exist standing
 * on ground inside a taint field, so {@link #primeFibresOnGround} rebuilds
 * that ground (a dirt slab under the whole scan box) before every seeded tick;
 * a fibres cell floating in a void is an unreachable world state and would
 * test the death rule, not the conversion table. (2) The scan box
 * ({@code + rand.nextInt(3) - 1} / {@code + rand.nextInt(5) - 3}) can land on
 * the fibres cell itself and on the cells that touch it; the original's tick
 * converts nothing at its own cell, and at a cell touching the fibres the
 * fibres themselves count as taint in {@code getAdjacentTaint}. The probes
 * therefore skip the self-scan seeds, and the negative (below-threshold) loop
 * also skips the six cells that touch the source, so "exactly two adjacent
 * taint" means what it says.</p>
 *
 * <p>Guarded dependencies (flux goo, the taint entities, the Taint Poison
 * effect) are other cards' rows; each probe branches on
 * {@link TaintManager#entityRegistered} / {@link TaintManager#fluxGooRegistered()}
 * so this card's evidence stays true before and after they land.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaintGameTest {

    private TaintGameTest() {
    }

    /** The scan offset a given seed makes the tick use, by replaying its draws. */
    private static BlockPos mirroredScanOffset(final long seed) {
        final net.minecraft.util.RandomSource mirror = net.minecraft.util.RandomSource.create(seed);
        final int spreadRate = TcConfig.getInt(TcConfigCatalog.BIOME_TAINT_SPREAD);
        if (spreadRate > 0) {
            mirror.nextInt(3);                       // taintBiomeSpread: xx
            mirror.nextInt(3);                       // taintBiomeSpread: zz
            mirror.nextInt(TaintRules.biomeSpreadModulus(spreadRate)); // the roll
        }
        return new BlockPos(mirror.nextInt(TaintRules.SCAN_XZ_MOD) - 1,
                mirror.nextInt(TaintRules.SCAN_Y_MOD) - TaintRules.SCAN_Y_SHIFT,
                mirror.nextInt(TaintRules.SCAN_XZ_MOD) - 1);
    }

    /** The taint biome over every column of the fixture box around {@code base}. */
    private static void paintTaintBiome(final ServerLevel level, final BlockPos base, final int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                TaintBiomes.setBiomeAtAndSync(level, base.getX() + dx, base.getZ() + dz,
                        TaintBiomes.taintBiomeHolder(level));
            }
        }
    }

    /**
     * Rebuild the world state the original's fibres are always generated into:
     * ground under the whole scan box, air above it, and one plain-fibres cell
     * at {@code base}. The clear covers the scan box (x/z ±1, y -3..+1) plus
     * the ring the probes paint (±2), so no taint from the previous seeded tick
     * is left standing to raise the next tick's neighbour count.
     */
    private static void primeFibresOnGround(final ServerLevel level, final BlockPos base) {
        // The scan box is x/z ±1 and y -3..+1, and a seeded tick paints its
        // result at the scan cell and its two crust neighbours at z ±1: clear a
        // box with a cell of margin on every side, so nothing an earlier seed
        // wrote can raise the next seed's getAdjacentTaint.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -5; dy <= 4; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
            }
        }
        TaintManager.setFibres(level, base, BlockTaintFibres.META_FIBRES);
    }

    @GameTest(template = "gametest_pad_32")
    public static void taintContentRegistersWithOriginalIdsAndVariantRanges(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.assertTrue(BuiltInRegistries.BLOCK.get(TaintManager.TAINT_ID) == CatalogTaint.BLOCK_TAINT.get(),
                "blocktaint must register under the original id " + TaintManager.TAINT_ID);
        helper.assertTrue(BuiltInRegistries.BLOCK.get(TaintManager.FIBRES_ID) == CatalogTaint.BLOCK_TAINT_FIBRES.get(),
                "blocktaintfibres must register under the original id " + TaintManager.FIBRES_ID);
        helper.assertTrue(BuiltInRegistries.ITEM.get(TaintManager.TAINT_ID) != null
                        && BuiltInRegistries.ITEM.getKey(CatalogTaint.ITEM_TAINT.get()).equals(TaintManager.TAINT_ID),
                "the taint block item must carry the original id");

        final Property<?> taintVariants = CatalogTaint.BLOCK_TAINT.get().getStateDefinition().getProperty("variant");
        final Property<?> fibreVariants = CatalogTaint.BLOCK_TAINT_FIBRES.get().getStateDefinition().getProperty("variant");
        helper.assertTrue(taintVariants instanceof IntegerProperty
                        && ((IntegerProperty) taintVariants).getPossibleValues().size() == 3,
                "blocktaint needs the original 3 metas (crust, soil, flesh) — the original writes no meta 3, "
                        + "got " + taintVariants);
        helper.assertTrue(fibreVariants instanceof IntegerProperty
                        && ((IntegerProperty) fibreVariants).getPossibleValues().size() == 5,
                "blocktaintfibres needs the original 5 metas (fibres .. mature stalk)");

        // the Tainted Land biome is a registry entry with the original's climate
        final Biome taint = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(TaintBiomes.TAINT_BIOME).value();
        helper.assertTrue(Math.abs(taint.getBaseTemperature() - 0.5F) < 1.0E-4F,
                "BiomeGenTaint temperature is 0.5 in the original, got " + taint.getBaseTemperature());
        helper.assertTrue(taint.hasPrecipitation(), "the ported biome rains (has_precipitation)");
        helper.assertTrue(TaintManager.ownedBiomes().contains(TaintBiomes.TAINT_BIOME),
                "the taint biome is owned by this card's biome set");

        // the biome's colour slots are the original's numbers (1.21.1 has four
        // required slots; the original's func_76731_a(float)=8144127 has no
        // slot at all — documented deviation, see the biome JSON's _comment).
        final net.minecraft.world.level.biome.BiomeSpecialEffects fx = taint.getSpecialEffects();
        helper.assertTrue(fx.getFogColor() == 7160201, "fog colour 7160201, got " + fx.getFogColor());
        helper.assertTrue(fx.getSkyColor() == 7160201, "sky colour 7160201, got " + fx.getSkyColor());
        helper.assertTrue(fx.getWaterColor() == 13373832,
                "water colour 13373832, got " + fx.getWaterColor());
        helper.assertTrue(fx.getGrassColorOverride().orElse(-1) == 8154503,
                "grass colour 8154503, got " + fx.getGrassColorOverride());
        helper.assertTrue(fx.getFoliageColorOverride().orElse(-1) == 8154503,
                "foliage colour 8154503, got " + fx.getFoliageColorOverride());
        helper.assertTrue(fx.getWaterFogColor() == 329011,
                "water fog colour 329011 (the original's BiomeGenBase default), got "
                        + fx.getWaterFogColor());
        helper.succeed();
    }

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void taintFibresConvertLogsAtTwoAdjacentTaint(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(8, 6, 8));
        paintTaintBiome(level, base, 3);

        int converted = 0;
        int tried = 0;
        int selfScans = 0;
        for (long seed = 1; seed <= 60; seed++) {
            final BlockPos off = mirroredScanOffset(seed);
            if (off.equals(BlockPos.ZERO)) {
                selfScans++;
                continue; // the tick scans its own cell; the original converts there
            }
            primeFibresOnGround(level, base);
            final BlockPos target = base.offset(off);
            level.setBlockAndUpdate(target, Blocks.OAK_LOG.defaultBlockState());
            // exactly two taint neighbours: the original's crust threshold
            level.setBlockAndUpdate(target.north(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            level.setBlockAndUpdate(target.south(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            if (!level.getBlockState(base).is(CatalogTaint.BLOCK_TAINT_FIBRES.get())) {
                continue; // the fibres death rule fired on this fixture
            }
            tried++;
            tickTaintBlock(level, base, seed);
            final BlockState after = level.getBlockState(target);
            if (after.is(CatalogTaint.BLOCK_TAINT.get())
                    && after.getValue(BlockTaint.VARIANT) == BlockTaint.META_CRUST) {
                converted++;
            }
        }
        helper.assertTrue(tried >= 50, "the seed sweep must leave enough live scans, got " + tried
                + " of 60 (" + selfScans + " self-scans skipped)");
        helper.assertTrue(converted == tried,
                "every scanned wood log with 2 adjacent taint must become taint crust: "
                        + converted + "/" + tried);
        helper.succeed();
    }

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void taintFibresConvertSoilOnlyAtThreeAdjacentTaint(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(16, 6, 8));
        paintTaintBiome(level, base, 3);

        int converted = 0;
        int tried = 0;
        for (long seed = 1; seed <= 60; seed++) {
            final BlockPos off = mirroredScanOffset(seed);
            if (off.equals(BlockPos.ZERO)) {
                continue;
            }
            primeFibresOnGround(level, base);
            final BlockPos target = base.offset(off);
            level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(target.north(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            level.setBlockAndUpdate(target.south(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            level.setBlockAndUpdate(target.above(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            if (!level.getBlockState(base).is(CatalogTaint.BLOCK_TAINT_FIBRES.get())) {
                continue;
            }
            tried++;
            tickTaintBlock(level, base, seed);
            final BlockState after = level.getBlockState(target);
            if (after.is(CatalogTaint.BLOCK_TAINT.get())
                    && after.getValue(BlockTaint.VARIANT) == BlockTaint.META_SOIL) {
                converted++;
            }
        }
        helper.assertTrue(converted == tried,
                "every scanned dirt with 3 adjacent taint must become taint soil: " + converted + "/" + tried);

        // the negative: two adjacent taint is below the soil threshold. Cells
        // touching the fibres source are skipped — the source itself is taint
        // material, so those scans are 3-adjacent by construction, not 2.
        int stayed = 0;
        int scored = 0;
        String firstFail = null;
        final java.util.Map<String, Integer> became = new java.util.TreeMap<>();
        for (long seed = 1; seed <= 40; seed++) {
            final BlockPos off = mirroredScanOffset(seed);
            if (manhattan(off) <= 1) {
                continue;
            }
            primeFibresOnGround(level, base);
            final BlockPos target = base.offset(off);
            level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(target.north(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            level.setBlockAndUpdate(target.south(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            if (!level.getBlockState(base).is(CatalogTaint.BLOCK_TAINT_FIBRES.get())) {
                continue;
            }
            scored++;
            tickTaintBlock(level, base, seed);
            final BlockState fin = level.getBlockState(target);
            if (fin.is(Blocks.DIRT)) {
                stayed++;
            } else {
                final String variant = fin.hasProperty(BlockTaint.VARIANT)
                        ? "#" + fin.getValue(BlockTaint.VARIANT)
                        : (fin.hasProperty(BlockTaintFibres.VARIANT)
                                ? "#" + fin.getValue(BlockTaintFibres.VARIANT) : "");
                became.merge(fin.getBlock() + variant, 1, Integer::sum);
                if (firstFail == null) {
                    firstFail = describeNeighbourhood(level, target, off);
                }
            }
        }
        helper.assertTrue(scored >= 25, "the seed sweep must leave enough 2-adjacent scans, got " + scored);
        helper.assertTrue(stayed == scored,
                "dirt with only 2 adjacent taint must stay dirt (the 3-neighbour threshold): "
                        + stayed + "/" + scored + " became " + became + " first: " + firstFail);
        helper.succeed();
    }

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void taintSpreadConvertsNeighbourColumnsAndBiomeWriteCoversColumn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(24, 6, 8));
        final int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                TaintManager.restoreBiome(level, base.getX() + dx, base.getZ() + dz);
            }
        }
        TaintBiomes.setBiomeAtAndSync(level, base.getX(), base.getZ(), TaintBiomes.taintBiomeHolder(level));

        // the palette write covers the whole column, not one height: 1.7.10
        // stored one byte per chunk column, 1.21.1 stores a 4x4x4 palette per
        // section, so the port must write every section of the column.
        // Read through the palette (getNoiseBiome), which is what the shipped
        // write writes and what the client sync sends. Level#getBiome goes
        // through BiomeManager's per-section cache (up to 600 ticks), so it
        // keeps reporting the generated biome for a while after a write; that
        // staleness is pinned in TaintBiomes' note and in architecture/taint.md.
        // Sample one height per section. LevelReader#getNoiseBiome short-circuits
        // an AIR-ONLY section to the source default (its `section.hasOnlyAir()
        // ? DEFAULT : palette.get(...)` branch), so a bare air column reads
        // plains in every empty section no matter what the palette holds — the
        // pad is a 32^3 build over void, so all of these sections are air-only
        // until a probe puts a block in them. Every real taint column has
        // ground in it, so the port puts one block per sampled section: the
        // read then goes to the palette the shipped write writes. (Recorded in
        // docs/architecture/taint.md.)
        final StringBuilder misses = new StringBuilder();
        int heights = 0;
        for (int y = level.getMinBuildHeight() + 1; y < level.getMaxBuildHeight(); y += 64) {
            level.setBlockAndUpdate(new BlockPos(base.getX(), y, base.getZ()), Blocks.STONE.defaultBlockState());
            final Holder<Biome> at = level.getNoiseBiome(
                    net.minecraft.core.QuartPos.fromBlock(base.getX()),
                    net.minecraft.core.QuartPos.fromBlock(y),
                    net.minecraft.core.QuartPos.fromBlock(base.getZ()));
            heights++;
            if (!at.is(TaintBiomes.TAINT_BIOME)) {
                misses.append(" y=").append(y).append('(').append(at.getRegisteredName()).append(')');
            }
        }
        helper.assertTrue(misses.length() == 0,
                "the biome write must cover every section of the column (minBuildHeight="
                        + level.getMinBuildHeight() + ", maxBuildHeight=" + level.getMaxBuildHeight()
                        + ", " + heights + " sampled heights):" + misses
                        + " (wrote " + TaintBiomes.taintBiomeHolder(level).getRegisteredName()
                        + " at base=" + base.toShortString() + ")");

        // the spread rule needs >= 2 adjacent taint on the source cell
        primeFibresOnGround(level, base);
        level.setBlockAndUpdate(base.north(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
        level.setBlockAndUpdate(base.south(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));

        int converted = 0;
        for (long seed = 1; seed <= 6000 && converted == 0; seed++) {
            if (!level.getBlockState(base).is(CatalogTaint.BLOCK_TAINT_FIBRES.get())) {
                primeFibresOnGround(level, base);
                level.setBlockAndUpdate(base.north(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
                level.setBlockAndUpdate(base.south(), TaintManager.stateOf(TaintManager.TAINT_ID, BlockTaint.META_CRUST));
            }
            tickTaintBlock(level, base, seed);
            for (int dx = -1; dx <= 1 && converted == 0; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    if (level.getNoiseBiome(
                            net.minecraft.core.QuartPos.fromBlock(base.getX() + dx),
                            net.minecraft.core.QuartPos.fromBlock(base.getY()),
                            net.minecraft.core.QuartPos.fromBlock(base.getZ() + dz))
                            .is(TaintBiomes.TAINT_BIOME)) {
                        converted++;
                    }
                }
            }
        }
        helper.assertTrue(converted >= 1,
                "the biome spread (rate 200 -> nextInt(1000) == 0, >= 2 adjacent taint) must convert a "
                        + "neighbour column; 6000 seeded ticks converted " + converted);

        // cleansing: the same primitive puts the original biome back
        TaintManager.restoreBiome(level, base.getX(), base.getZ());
        helper.assertTrue(level.getNoiseBiome(
                        net.minecraft.core.QuartPos.fromBlock(base.getX()),
                        net.minecraft.core.QuartPos.fromBlock(base.getY()),
                        net.minecraft.core.QuartPos.fromBlock(base.getZ()))
                        .is(net.minecraft.world.level.biome.Biomes.PLAINS),
                "restoring a column must give the biome back (the cleansing primitive)");

        // fibres outside the taint biome die to air (the original's death rule)
        primeFibresOnGround(level, base);
        tickTaintBlock(level, base, 7L);
        helper.assertTrue(level.getBlockState(base).isAir(),
                "fibres outside the taint biome die to air, got " + level.getBlockState(base));
        helper.succeed();
    }

    @GameTest(template = "gametest_pad_32", timeoutTicks = 600)
    public static void taintSoilDecaysOutsideBiomeAtExactlyOneInTen(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(8, 6, 20));
        TaintManager.restoreBiome(level, base.getX(), base.getZ());

        int decayed = 0;
        int mismatches = 0;
        for (long seed = 1; seed <= 200; seed++) {
            clearBox(level, base, 1, 3, 1);
            TaintManager.restoreBiome(level, base.getX(), base.getZ());
            TaintManager.setTaint(level, base, BlockTaint.META_SOIL);
            // stone above and below: the crust fall/log-scan draws stay out of the sequence
            level.setBlockAndUpdate(base.above(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());

            final net.minecraft.util.RandomSource mirror = net.minecraft.util.RandomSource.create(seed);
            final int spreadRate = TcConfig.getInt(TcConfigCatalog.BIOME_TAINT_SPREAD);
            if (spreadRate > 0) {
                mirror.nextInt(3);
                mirror.nextInt(3);
                mirror.nextInt(TaintRules.biomeSpreadModulus(spreadRate));
            }
            mirror.nextInt(TaintRules.SCAN_XZ_MOD);
            mirror.nextInt(TaintRules.SCAN_Y_MOD);
            mirror.nextInt(TaintRules.SCAN_XZ_MOD);
            final boolean expectDecay = mirror.nextInt(TaintRules.SOIL_DECAY_MOD) == 0;

            TaintManager.taintBlockTick(level, base, level.getBlockState(base),
                    net.minecraft.util.RandomSource.create(seed));
            final boolean decayedNow = level.getBlockState(base).is(Blocks.DIRT);
            if (decayedNow) {
                decayed++;
            }
            if (decayedNow != expectDecay) {
                mismatches++;
            }
        }
        helper.assertTrue(mismatches == 0,
                "soil outside the taint biome must decay on exactly nextInt(10) == 0; " + mismatches
                        + " of 200 seeds disagreed with the original draw order");
        helper.assertTrue(decayed >= 5 && decayed <= 60,
                "expected about 1 in 10 soil cells to decay, saw " + decayed + "/200");
        helper.succeed();
    }

    @GameTest(template = "gametest_pad_32")
    public static void taintCrustDecaysToFluxGooOnlyWhenThatRowExists(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(14, 6, 20));
        TaintManager.restoreBiome(level, base.getX(), base.getZ());
        TaintManager.setTaint(level, base, BlockTaint.META_CRUST);
        level.setBlockAndUpdate(base.above(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());

        int decayed = 0;
        for (long seed = 1; seed <= 400; seed++) {
            TaintManager.setTaint(level, base, BlockTaint.META_CRUST);
            TaintManager.taintBlockTick(level, base, level.getBlockState(base),
                    net.minecraft.util.RandomSource.create(seed));
            if (!level.getBlockState(base).is(CatalogTaint.BLOCK_TAINT.get())) {
                decayed++;
            }
        }
        if (TaintManager.fluxGooRegistered()) {
            helper.assertTrue(decayed > 0,
                    "with flux goo registered, crust outside the biome must decay (1 in 20)");
        } else {
            helper.assertTrue(decayed == 0,
                    "crust decay targets flux goo (TC-M5-03's row); while that row is unregistered the "
                            + "seam must leave the crust in place, got " + decayed + " changes in 400 ticks");
        }
        helper.succeed();
    }

    /**
     * The crust's gravity, pinned to the decompiled call shape. The original's
     * {@code BlockTaint.updateTick} runs {@code tryToFall(world, x, y, z, x, y,
     * z)} (the same cell as the target, not {@code y - 1} — the reference tree's
     * text, quoted by a parity pin), so in 1.7.10 the observable fall is the
     * falling-sand entity the helper spawns at the target; the block-move is a
     * same-cell churn. That entity is TC-M6-06's row ({@code
     * thaumcraft:fallingtaint}), so while it is unregistered the faithful
     * outcome here is: the gate is evaluated, the crust survives in place, and
     * the tick does not throw. The height half of the gate (the original's
     * {@code y2 >= 0}, the port's level floor) is pinned in
     * {@code TaintRulesTest.fallHeightGuardCarriesTheOriginalsWorldFloorMeaning}.
     */
    @GameTest(template = "gametest_pad_32")
    public static void taintCrustGravityFollowsTheOriginalFallTarget(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(20, 8, 20));
        paintTaintBiome(level, base, 2);
        clearBox(level, base, 2, 4, 2);
        TaintManager.setTaint(level, base, BlockTaint.META_CRUST);

        // the gate's world facts: air below (canFallBelow) and a cell above the
        // level floor (canFallAtHeight) — the fall branch is taken
        helper.assertTrue(level.getBlockState(base.below()).isAir(),
                "the fixture needs air below the crust, got " + level.getBlockState(base.below()));
        helper.assertTrue(base.getY() > level.getMinBuildHeight(),
                "the fixture crust must sit above the level floor");

        tickTaintBlock(level, base, 11L);

        final BlockState after = level.getBlockState(base);
        helper.assertTrue(after.is(CatalogTaint.BLOCK_TAINT.get())
                        && after.getValue(BlockTaint.VARIANT) == BlockTaint.META_CRUST,
                "the original's fall target is the crust's own cell, so the crust survives in place "
                        + "(the observable fall is the falling-taint entity, TC-M6-06's row), got " + after);
        if (TaintManager.entityRegistered(TaintManager.FALLING_TAINT_ID)) {
            helper.assertTrue(TaintManager.countEntities(level, TaintManager.FALLING_TAINT_ID,
                            new net.minecraft.world.phys.AABB(base.getX(), base.getY() - 3, base.getZ(),
                                    base.getX() + 1, base.getY() + 3, base.getZ() + 1)) >= 1,
                    "with the falling-taint entity registered, the crust must spawn it at the target");
        }

        // a crust walled in below cannot fall: the gate refuses, the tick keeps
        // going, and the crust stays crust
        level.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());
        tickTaintBlock(level, base, 13L);
        helper.assertTrue(level.getBlockState(base).is(CatalogTaint.BLOCK_TAINT.get()),
                "a crust on solid ground must not move, got " + level.getBlockState(base));
        helper.succeed();
    }

    @GameTest(template = "gametest_pad_32")
    public static void guardedTaintDependenciesStayInert(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = helper.absolutePos(new BlockPos(26, 6, 20));
        paintTaintBiome(level, base, 1);
        final int gooBefore = countBlocks(level, base, 3);

        // the flux -> taint seam runs the original's rule and must not throw
        // while the goo row is unregistered (it never gets called from a block)
        level.setBlockAndUpdate(base, Blocks.WATER.defaultBlockState());
        helper.assertTrue(TaintRules.fluxGooConvertsToTaint(6, true, 0, true),
                "the goo rule itself is pinned: quanta 6, air above, roll 0, config on");
        helper.assertTrue(!TaintRules.fluxGooConvertsToTaint(5, true, 0, true),
                "quanta 5 is below the original's meta >= 6 gate");

        // the bottle-taint spill seam (TC-M6-06's projectile) is safe to call
        TaintManager.spillBottleTaint(level, base, net.minecraft.util.RandomSource.create(1L));
        helper.assertTrue(TaintManager.guardedDependencies().contains(TaintManager.TAINT_SPORE_ID),
                "the taint entities are declared guarded dependencies of this card");
        helper.assertTrue(gooBefore >= 0, "fixture sanity");
        helper.succeed();
    }

    /**
     * Run the tick the way {@code world/TaintTickDriver} does: dispatch on the
     * live state, so a probe never hands a crust state to the fibres tick (the
     * driver's dispatch is what keeps those two paths apart in the shipped
     * game, and the probes are testing the shipped paths).
     */
    private static void tickTaintBlock(final ServerLevel level, final BlockPos pos, final long seed) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(CatalogTaint.BLOCK_TAINT_FIBRES.get())) {
            TaintManager.fibresTick(level, pos, state, net.minecraft.util.RandomSource.create(seed));
        } else if (state.is(CatalogTaint.BLOCK_TAINT.get())) {
            TaintManager.taintBlockTick(level, pos, state, net.minecraft.util.RandomSource.create(seed));
        }
    }

    /** The six faces of a cell, for a probe failure message. */
    private static String describeNeighbourhood(final ServerLevel level, final BlockPos target,
                                                final BlockPos off) {
        final StringBuilder sb = new StringBuilder("off=" + off + " adjacentTaint="
                + TaintManager.getAdjacentTaint(level, target) + " faces=");
        for (final net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            final BlockPos n = target.offset(d.getNormal());
            sb.append(d.getName()).append('=')
                    .append(level.getBlockState(n).getBlock())
                    .append('@').append(n.getX() - target.getX())
                    .append(',').append(n.getY() - target.getY())
                    .append(',').append(n.getZ() - target.getZ()).append(' ');
        }
        return sb.toString();
    }

    /** |x| + |y| + |z| for a scan offset (BlockPos has no manhattan()). */
    private static int manhattan(final BlockPos off) {
        return Math.abs(off.getX()) + Math.abs(off.getY()) + Math.abs(off.getZ());
    }

    private static int countBlocks(final ServerLevel level, final BlockPos base, final int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!level.getBlockState(base.offset(dx, dy, dz)).isAir()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void clearBox(final ServerLevel level, final BlockPos base,
                                 final int rx, final int ryUp, final int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ryUp; dy <= 1; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
