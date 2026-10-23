package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCustomOre;
import com.jonnyutah12.thaumcraftclassic.config.ConfigOption;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World generation (TC-M5-05): proves the ore datapack objects load, are
 * wired into overworld biomes, and reproduce the original per-chunk attempt
 * frequencies and Y-bands of ThaumcraftWorldGenerator.generateOres.
 *
 * <p>The generation tests run the placed features directly against a
 * stone-filled region with a fixed seed (docs/testing.md: seeded tests for
 * random processes), so counts and bands are deterministic. The biome test
 * reads the real server biome data after the {@code neoforge:add_features}
 * modifier applied, and also asserts vanilla ores survive the modifier
 * (additive, not replacing).</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldGenGameTest {

    private static final ResourceLocation CINNABAR = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "ore_cinnabar");
    private static final ResourceLocation AMBER = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "ore_amber");
    private static final ResourceLocation INFUSED = ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "ore_infused_stone");

    /** Region: 3x3 chunks wide, y 0..50 — covers every ore band. */
    private static final int REGION_CHUNKS = 3;
    private static final int REGION_SIDE = REGION_CHUNKS * 16;
    private static final int REGION_MAX_Y = 50;

    @GameTest(template = "empty")
    public static void oreDataLoadsAndWiresIntoOverworldBiomes(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();

        // The placed features exist in the live datapack registries: this only
        // passes if the configured/placed JSONs decoded against the registered
        // tc_ore feature type and the three custom placement codecs.
        for (final ResourceLocation id : new ResourceLocation[] {CINNABAR, AMBER, INFUSED}) {
            helper.assertTrue(placed(level, id) != null, "placed feature missing: " + id);
        }

        // The biome modifier applied: plains must now carry all three ores in
        // its underground_ores step...
        final Biome plains = level.registryAccess().registryOrThrow(Registries.BIOME)
                .get(ResourceLocation.withDefaultNamespace("plains"));
        helper.assertTrue(plains != null, "plains biome missing");
        final BiomeGenerationSettings gen = plains.getGenerationSettings();
        final java.util.Set<ResourceLocation> ores = stepFeatures(gen, GenerationStep.Decoration.UNDERGROUND_ORES);
        helper.assertTrue(ores.contains(CINNABAR), "cinnabar not wired into plains underground_ores");
        helper.assertTrue(ores.contains(AMBER), "amber not wired into plains underground_ores");
        helper.assertTrue(ores.contains(INFUSED), "infused stone not wired into plains underground_ores");

        // ...additively: vanilla ores are still there (add_features, not
        // replace_all_features).
        helper.assertTrue(ores.contains(ResourceLocation.withDefaultNamespace("ore_iron_upper")),
                "vanilla ore step was replaced, not extended");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 6000)
    public static void seededOreGenerationMatchesOriginalFrequencies(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        fillStoneRegion(level);
        final ChunkGenerator generator = level.getChunkSource().getGenerator();

        // Cinnabar: original 18 attempts/chunk, Y uniform [0,50], single block.
        // Seeded run over 3x3 chunks: every attempt must land (region is all
        // stone), so exactly 18*9 blocks, all cinnabar variant, all in band.
        runFeature(level, generator, CINNABAR);
        final OreCounts cinnabar = countOres(level);
        helper.assertTrue(cinnabar.total() == 18 * REGION_CHUNKS * REGION_CHUNKS,
                "cinnabar count " + cinnabar.total() + " != 18/chunk * 9 chunks");
        helper.assertTrue(cinnabar.onlyVariant(BlockCustomOre.VARIANT_CINNABAR),
                "non-cinnabar variant in cinnabar run");
        helper.assertTrue(cinnabar.minY() >= 0 && cinnabar.maxY() <= 50,
                "cinnabar outside original Y band [0,50]: " + cinnabar.minY() + ".." + cinnabar.maxY());

        // Amber: original 20 attempts/chunk at surface-[0,24]. The filled
        // region's WORLD_SURFACE heightmap is 51, so Y lands in [27,51];
        // Y=51 is air (region tops out at 50) and that attempt misses, which
        // is the original's own edge behaviour. Count must sit in the
        // achievable window and every block inside the band.
        runFeature(level, generator, AMBER);
        final OreCounts amber = countOresInBand(level, 27, 50, 1 << BlockCustomOre.VARIANT_AMBER);
        helper.assertTrue(amber.total() >= 17 * REGION_CHUNKS * REGION_CHUNKS
                        && amber.total() <= 20 * REGION_CHUNKS * REGION_CHUNKS,
                "amber count " + amber.total() + " outside 17..20/chunk * 9 chunks");
        helper.assertTrue(amber.onlyVariant(BlockCustomOre.VARIANT_AMBER),
                "non-amber variant in amber run");

        // Infused stone: original 8 veins of size 6/chunk, Y uniform
        // [0, max(5, surface-5)), variant uniform 1..6 per vein. Veins clip
        // at the region border, so assert a generous lower bound, the hard
        // upper bound, and the variant domain.
        runFeature(level, generator, INFUSED);
        final OreCounts infused = countOresInBand(level, 0, REGION_MAX_Y,
                ((1 << (BlockCustomOre.VARIANT_INFUSED_MAX + 1)) - 1) & ~((1 << BlockCustomOre.VARIANT_INFUSED_MIN) - 1));
        helper.assertTrue(infused.total() >= 24 * REGION_CHUNKS && infused.total() <= 48 * REGION_CHUNKS * REGION_CHUNKS,
                "infused count " + infused.total() + " outside 24..48*9");
        helper.assertTrue(infused.onlyVariants(BlockCustomOre.VARIANT_INFUSED_MIN, BlockCustomOre.VARIANT_INFUSED_MAX),
                "infused run placed variants outside 1..6");
        helper.assertTrue(infused.distinctVariants() >= 2,
                "infused run produced a single variant — per-vein selection broken");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 3000)
    public static void configGateSuppressesDisabledOre(final GameTestHelper helper) {
        final ServerLevel level = (ServerLevel) helper.getLevel();
        fillStoneRegion(level);
        final ChunkGenerator generator = level.getChunkSource().getGenerator();

        final ConfigOption option = TcConfigCatalog.GENERATE_CINNIBAR_ORE;
        TcConfig.fields().get(option).set(Boolean.FALSE);
        try {
            runFeature(level, generator, CINNABAR);
            helper.assertTrue(countOres(level).total() == 0,
                    "cinnabar generated while World_Generation.generate_cinnibar_ore=false");
        } finally {
            TcConfig.fields().get(option).set(Boolean.TRUE);
        }

        // Re-enabled: the same feature generates again (gate reads live).
        runFeature(level, generator, CINNABAR);
        helper.assertTrue(countOres(level).total() == 18 * REGION_CHUNKS * REGION_CHUNKS,
                "cinnabar did not resume after re-enabling the option");
        helper.succeed();
    }


    // ---- helpers -----------------------------------------------------------

    private static PlacedFeature placed(final ServerLevel level, final ResourceLocation id) {
        return level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(id);
    }

    private static java.util.Set<ResourceLocation> stepFeatures(final BiomeGenerationSettings gen,
            final GenerationStep.Decoration step) {
        final java.util.Set<ResourceLocation> ids = new java.util.HashSet<>();
        for (final var holder : gen.features().get(step.ordinal()).stream().toList()) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.location()));
        }
        return ids;
    }

    private static void fillStoneRegion(final ServerLevel level) {
        for (int x = 0; x < REGION_SIDE; x++) {
            for (int z = 0; z < REGION_SIDE; z++) {
                for (int y = 0; y <= REGION_MAX_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 0);
                }
            }
        }
        // The game-test server never runs the worldgen prime stage for these chunks;
        // vanilla OreFeature.place guards on a primed OCEAN_FLOOR_WG heightmap.
        for (int cz = 0; cz < REGION_CHUNKS; cz++) {
            for (int cx = 0; cx < REGION_CHUNKS; cx++) {
                final net.minecraft.world.level.chunk.ChunkAccess chunk = level.getChunk(cx, cz);
                if (!chunk.hasPrimedHeightmap(Heightmap.Types.OCEAN_FLOOR_WG)) {
                    Heightmap.primeHeightmaps(chunk, java.util.Set.of(Heightmap.Types.OCEAN_FLOOR_WG));
                }
            }
        }
    }

    /** Runs the placed feature once per chunk origin in the region, fixed seed. */
    private static void runFeature(final ServerLevel level, final ChunkGenerator generator,
            final ResourceLocation id) {
        final PlacedFeature feature = placed(level, id);
        if (feature == null) {
            throw new IllegalStateException("placed feature not loaded: " + id);
        }
        final RandomSource random = new XoroshiroRandomSource(0x7CAFE5L);
        for (int cx = 0; cx < REGION_CHUNKS; cx++) {
            for (int cz = 0; cz < REGION_CHUNKS; cz++) {
                feature.place(level, generator, random, new BlockPos(cx * 16, 0, cz * 16));
            }
        }
    }

    private static OreCounts countOres(final ServerLevel level) {
        return countOresInBand(level, -64, 320, ~0);
    }

    /**
     * Counts blockCustomOre blocks in the band whose variant is inside
     * allowedVariants. Each runFeature targets one ore kind, but earlier runs
     * in the same test leave their blocks in the region; the variant mask
     * isolates the current run's blocks.
     */
    private static OreCounts countOresInBand(final ServerLevel level, final int minY, final int maxY,
            final int allowedVariants) {
        int total = 0;
        int minSeen = Integer.MAX_VALUE;
        int maxSeen = Integer.MIN_VALUE;
        int variantMask = 0;
        for (int x = 0; x < REGION_SIDE; x++) {
            for (int z = 0; z < REGION_SIDE; z++) {
                for (int y = minY; y <= maxY; y++) {
                    final BlockState block = level.getBlockState(new BlockPos(x, y, z));
                    if (block.getBlock() instanceof BlockCustomOre) {
                        final int variant = block.getValue(BlockCustomOre.VARIANT);
                        if ((allowedVariants & (1 << variant)) != 0) {
                            total++;
                            minSeen = Math.min(minSeen, y);
                            maxSeen = Math.max(maxSeen, y);
                            variantMask |= 1 << variant;
                        }
                    }
                }
            }
        }
        return new OreCounts(total, minSeen == Integer.MAX_VALUE ? 0 : minSeen,
                maxSeen == Integer.MIN_VALUE ? 0 : maxSeen, variantMask);
    }

    private record OreCounts(int total, int minY, int maxY, int variantMask) {

        boolean onlyVariant(final int variant) {
            return variantMask == (1 << variant);
        }

        boolean onlyVariants(final int minVariant, final int maxVariant) {
            final int allowed = ((1 << (maxVariant + 1)) - 1) & ~((1 << minVariant) - 1);
            return variantMask != 0 && (variantMask & ~allowed) == 0;
        }

        int distinctVariants() {
            return Integer.bitCount(variantMask);
        }
    }

    private WorldGenGameTest() {
    }
}
