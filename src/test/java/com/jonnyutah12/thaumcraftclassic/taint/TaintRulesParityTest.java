package com.jonnyutah12.thaumcraftclassic.taint;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-04-s: every taint threshold in {@link TaintRules} is pinned to the
 * expression that ships in the decompiled 1.7.10 sources.
 *
 * <p>Each row below is (label, original source file, regex that must appear in
 * it). The regexes quote the obfuscated originals verbatim, so a threshold in
 * the port cannot drift without this test naming the original line it
 * contradicts — the requirement in the card is "spread/conversion unit tests
 * pinned to original thresholds", and the pin lives next to the rule it
 * guards, not in a comment.</p>
 *
 * <p>Skips when the reference tree is absent (CI), the same convention as
 * {@code LocalizationFileTest}.</p>
 */
class TaintRulesParityTest {

    private static final String BLOCKS = "thaumcraft/thaumcraft/common/blocks/";
    private static final String BIOMES = "thaumcraft/thaumcraft/common/lib/world/biomes/";

    /** (label, file, regex) triples, one per pinned constant. */
    private static final List<String[]> PINS = List.of(
            // ---- BlockTaint: decay, spread, gravity, step, properties ----
            new String[]{"crust decays on nextInt(20) == 0", BLOCKS + "BlockTaint.java",
                    "md == 0 && random\\.nextInt\\(20\\) == 0"},
            new String[]{"soil decays on nextInt(10) == 0", BLOCKS + "BlockTaint.java",
                    "md == 1 && random\\.nextInt\\(10\\) == 0"},
            new String[]{"crust decay target is flux goo", BLOCKS + "BlockTaint.java",
                    "func_147465_d\\(x, y, z, ConfigBlocks\\.blockFluxGoo"},
            new String[]{"soil decay target is dirt", BLOCKS + "BlockTaint.java",
                    "func_147465_d\\(x, y, z, Blocks\\.field_150346_d, 0, 3\\)"},
            new String[]{"crust spawns a spore swarmer on nextInt(200) == 0", BLOCKS + "BlockTaint.java",
                    // (?:...) — the alternation must be grouped: the test runs
                    // matches("(?s).*" + pin + ".*"), and a top-level `|` splits
                    // the whole pattern, so the right branch would then have to
                    // match from the very start of the file and could never pass.
                    "(?:Config\\.spawnTaintSpore && world\\.func_149674_a|spawnTaintSpore && world\\.func_147437_c\\(x, y \\+ 1, z\\) && random\\.nextInt\\(200\\) == 0)"},
            new String[]{"swarmer scan inflates the box by 16", BLOCKS + "BlockTaint.java",
                    "func_72314_b\\(16\\.0, 16\\.0, 16\\.0\\)"},
            new String[]{"player steps poison on nextInt(100) == 0", BLOCKS + "BlockTaint.java",
                    "entity instanceof EntityPlayer && world\\.field_73012_v\\.nextInt\\(100\\) == 0"},
            new String[]{"mobs step-poison on nextInt(20) == 0", BLOCKS + "BlockTaint.java",
                    "(!\\(entity instanceof EntityPlayer\\)) && world\\.field_73012_v\\.nextInt\\(20\\) == 0"},
            new String[]{"crust hardness 10.0", BLOCKS + "BlockTaint.java",
                    "func_149752_b\\(10\\.0F\\)"},
            new String[]{"crust resistance 2.0", BLOCKS + "BlockTaint.java",
                    "func_149711_c\\(2\\.0F\\)"},
            new String[]{"crust sound type is gore at 0.5/0.8", BLOCKS + "BlockTaint.java",
                    "CustomSoundType\\(\"gore\", 0\\.5F, 0\\.8F\\)"},
            new String[]{"crust gravity gate: canFallBelow below the target and y >= 0",
                    BLOCKS + "BlockTaint.java",
                    "canFallBelow\\(par1World, x2, y2 - 1, z2\\) && y2 >= 0"},
            new String[]{"crust tries to fall from its own cell", BLOCKS + "BlockTaint.java",
                    "tryToFall\\(world, x, y, z, x, y, z\\)"},
            new String[]{"flesh never ticks", BLOCKS + "BlockTaint.java", "if \\(md == 2\\) \\{\\s*return;"},
            new String[]{"crust step-hardness ladder 1.75 / 1.5 / 0.2 per meta",
                    BLOCKS + "BlockTaint.java",
                    // func_149712_f: the original writes it as an if/else ladder,
                    // not an array — the port's PLAYER_HARDNESS bag is its shape,
                    // so the pin quotes the original's own text.
                    "return 1\\.75F;\\s*\\} else if \\(md == 1\\) \\{\\s*return 1\\.5F;\\s*\\} else \\{\\s*return md == 2 \\? 0\\.2F"},
            // ---- BlockTaintFibres: conversion thresholds, ladder, death, scan box ----
            new String[]{"crust conversion needs adjacentTaint >= 2", BLOCKS + "BlockTaintFibres.java",
                    "adjacentTaint >= 2 && \\(Utils\\.isWoodLog"},
            new String[]{"crust conversion writes taint meta 0", BLOCKS + "BlockTaintFibres.java",
                    "func_147465_d\\(xx, yy, zz, ConfigBlocks\\.blockTaint, 0, 3\\)"},
            new String[]{"soil conversion needs adjacentTaint >= 3", BLOCKS + "BlockTaintFibres.java",
                    "adjacentTaint >= 3"},
            new String[]{"soil conversion excludes air", BLOCKS + "BlockTaintFibres.java",
                    "adjacentTaint >= 3\\s*&& bi != Blocks\\.field_150350_a"},
            new String[]{"soil conversion writes taint meta 1", BLOCKS + "BlockTaintFibres.java",
                    "func_147465_d\\(xx, yy, zz, ConfigBlocks\\.blockTaint, 1, 3\\)"},
            new String[]{"90% of seeded fibres are plain", BLOCKS + "BlockTaintFibres.java",
                    "nextInt\\(10\\) != 0 \\|\\| !world\\.func_147437_c\\(x, y \\+ 1, z\\) \\|\\| !world\\.isSideSolid"},
            new String[]{"grass-1 rung nextInt(10) < 9", BLOCKS + "BlockTaintFibres.java",
                    "nextInt\\(10\\) < 9"},
            new String[]{"grass-2 rung nextInt(12) < 10", BLOCKS + "BlockTaintFibres.java",
                    "nextInt\\(12\\) < 10"},
            new String[]{"the last rung is the spore stalk (meta 3)", BLOCKS + "BlockTaintFibres.java",
                    "func_147465_d\\(x, y, z, ConfigBlocks\\.blockTaintFibres, 3, 3\\)"},
            new String[]{"stalk spits a spore on nextInt(10) == 0 with air above",
                    BLOCKS + "BlockTaintFibres.java",
                    "md == 3 && Config\\.spawnTaintSpore && random\\.nextInt\\(10\\) == 0 && world\\.func_147437_c\\(x, y \\+ 1, z\\)"},
            new String[]{"a stalk with no spores left above reverts to meta 3",
                    BLOCKS + "BlockTaintFibres.java", "func_72921_c\\(x, y, z, 3, 3\\)"},
            new String[]{"fibres die to air outside the taint biome", BLOCKS + "BlockTaintFibres.java",
                    "func_147449_b\\(x, y, z, Blocks\\.field_150350_a\\)"},
            new String[]{"scan box x/z: nextInt(3) - 1", BLOCKS + "BlockTaintFibres.java",
                    "int xx = x \\+ random\\.nextInt\\(3\\) - 1"},
            new String[]{"scan box y: nextInt(5) - 3", BLOCKS + "BlockTaintFibres.java",
                    "int yy = y \\+ random\\.nextInt\\(5\\) - 3"},
            new String[]{"the redstone pulse plays roots at 0.1 / 0.9 + rand*0.2",
                    BLOCKS + "BlockTaintFibres.java",
                    "\"thaumcraft:roots\", 0\\.1F, 0\\.9F \\+ world\\.field_73012_v\\.nextFloat\\(\\) \\* 0\\.2F"},
            new String[]{"fibres hardness 5.0", BLOCKS + "BlockTaintFibres.java", "func_149752_b\\(5\\.0F\\)"},
            new String[]{"fibres resistance 1.0", BLOCKS + "BlockTaintFibres.java", "func_149711_c\\(1\\.0F\\)"},
            new String[]{"fibres are never side-solid", BLOCKS + "BlockTaintFibres.java",
                    "public boolean isSideSolid\\(IBlockAccess world, int x, int y, int z, ForgeDirection side\\) \\{\\s*return false;"},
            new String[]{"fibres seed gate: solid neighbour, not taint-only, not liquid, plantable cell",
                    BLOCKS + "BlockTaintFibres.java",
                    "isAdjacentToSolidBlock\\(world, x, y, z\\)\\s*&& !isOnlyAdjacentToTaint\\(world, x, y, z\\)\\s*&& !world\\.func_147439_a\\(x, y, z\\)\\.func_149688_o\\(\\)\\.func_76224_d\\(\\)\\s*&& \\(world\\.func_147437_c\\(x, y, z\\) \\|\\| bi\\.isReplaceable"},
            // ---- BlockFluxGoo: the flux -> taint branch ----
            new String[]{"goo converts at quanta >= 6 with air above", BLOCKS + "BlockFluxGoo.java",
                    "meta >= 6 && world\\.func_147437_c\\(x, y \\+ 1, z\\)"},
            new String[]{"goo-to-taint roll is nextInt(50) == 0 behind Config.taintFromFlux",
                    BLOCKS + "BlockFluxGoo.java", "Config\\.taintFromFlux && rand\\.nextInt\\(50\\) == 0"},
            new String[]{"the goo branch writes the biome then fibres meta 0", BLOCKS + "BlockFluxGoo.java",
                    "Utils\\.setBiomeAt\\(world, x, z, ThaumcraftWorldGenerator\\.biomeTaint\\)"},
            new String[]{"the large thaumic slime rolls nextInt(25)", BLOCKS + "BlockFluxGoo.java",
                    "rand\\.nextInt\\(25\\) == 0"},
            // ---- BiomeGenTaint: the colours and climate the biome JSON must carry ----
            new String[]{"sky/fog colour 7160201", BIOMES + "BiomeGenTaint.java", "7160201"},
            new String[]{"grass/foliage colour 8154503", BIOMES + "BiomeGenTaint.java", "8154503"},
            new String[]{"water colour 13373832", BIOMES + "BiomeGenTaint.java", "13373832"},
            new String[]{"sky-at-height colour 8144127", BIOMES + "BiomeGenTaint.java", "8144127"},
            new String[]{"biome display name is \"Tainted Land\"", BIOMES + "BiomeGenTaint.java",
                    "func_76735_a\\(\"Tainted Land\"\\)"},
            new String[]{"the biome's only spawn entry is the Taintacle (TC-M6-06's entity)",
                    BIOMES + "BiomeGenTaint.java",
                    "Config\\.spawnTaintacle\\) \\{\n\\s*this\\.field_76761_J\\.add\\(new SpawnListEntry\\(EntityTaintacle\\.class, 1, 1, 1\\)\\)"},
            // Temperature/downfall are NOT pinned here: BiomeGenTaint never sets
            // them, so 0.5/0.5 in the biome JSON is the BiomeGenBase default, and
            // BiomeGenBase is not in the reference tree (thaumcraft + baubles
            // only). The live 0.5 assertion is the gametest probe
            // TaintGameTest.taintContentRegistersWithOriginalIdsAndVariantRanges.
            new String[]{"the taint blob decoration is a WorldGenBlockBlob", BIOMES + "BiomeGenTaint.java",
                    "public static WorldGenBlockBlob blobs"},
            new String[]{"1-in-8 columns get the big magic tree", BIOMES + "BiomeGenTaint.java",
                    "par1Random\\.nextInt\\(8\\) == 0 \\? this\\.bigTree"},
            new String[]{"decorateSpecial: 10 fibre-scatter passes, meta 0", BIOMES + "BiomeGenTaint.java",
                    "for \\(int a = 0; a < 10; a\\+\\+\\) \\{\n.*ConfigBlocks\\.blockTaintFibres, 0, 2"},
            new String[]{"decorateSpecial: 8 surface-spread passes", BIOMES + "BiomeGenTaint.java",
                    "for \\(int ax = 0; ax < 8; ax\\+\\+\\) \\{\n\\s*int xx = x \\+ random\\.nextInt\\(16\\)"});

    @Test
    void everyThresholdIsPresentInTheOriginalSources() throws IOException {
        final Path root = referenceRoot();
        Assumptions.assumeTrue(root != null,
                "the reference tree (reference/decompiled) is not present on this build");
        for (final String[] pin : PINS) {
            final String source = Files.readString(root.resolve(pin[1]), StandardCharsets.UTF_8);
            assertTrue(source.matches("(?s).*" + pin[2] + ".*"),
                    pin[0] + " — not found in " + pin[1] + " (port would drift from the original)");
        }
    }

    @Test
    void thePortedConstantsMatchTheOriginalNumbers() {
        // The numbers the regexes above pin; kept here so a rename in
        // TaintRules that changes a value fails even without the reference tree.
        assertEquals(2, TaintRules.CRUST_ADJACENT_MIN);
        assertEquals(3, TaintRules.SOIL_ADJACENT_MIN);
        assertEquals(10, TaintRules.FIBRES_PLAIN_MOD);
        assertEquals(10, TaintRules.FIBRES_GRASS1_MOD);
        assertEquals(9, TaintRules.FIBRES_GRASS1_LIMIT);
        assertEquals(12, TaintRules.FIBRES_GRASS2_MOD);
        assertEquals(10, TaintRules.FIBRES_GRASS2_LIMIT);
        assertEquals(3, TaintRules.FIBRES_SPORE_STALK_META);
        assertEquals(20, TaintRules.CRUST_DECAY_MOD);
        assertEquals(10, TaintRules.SOIL_DECAY_MOD);
        assertEquals(200, TaintRules.CRUST_SWARMER_MOD);
        assertEquals(10, TaintRules.SPORE_STALK_MOD);
        assertEquals(16.0D, TaintRules.SWARMER_SCAN_INFLATE);
        assertEquals(5, TaintRules.BIOME_SPREAD_RATE_MULTIPLIER);
        assertEquals(2, TaintRules.BIOME_SPREAD_ADJACENT_MIN);
        assertEquals(3, TaintRules.SCAN_XZ_MOD);
        assertEquals(5, TaintRules.SCAN_Y_MOD);
        assertEquals(3, TaintRules.SCAN_Y_SHIFT);
        assertEquals(100, TaintRules.STEP_PLAYER_CRUST_MOD);
        assertEquals(20, TaintRules.STEP_MOB_CRUST_MOD);
        assertEquals(1000, TaintRules.STEP_PLAYER_FIBRES_MOD);
        assertEquals(500, TaintRules.STEP_MOB_FIBRES_MOD);
        assertEquals(80, TaintRules.STEP_PLAYER_DURATION_TICKS);
        assertEquals(160, TaintRules.STEP_MOB_DURATION_TICKS);
        assertEquals(6, TaintRules.FLUX_TAINT_MIN_QUANTA);
        assertEquals(50, TaintRules.FLUX_TAINT_MOD);
        assertEquals(25, TaintRules.FLUX_SLIME_LARGE_MOD);
        assertEquals(4, TaintRules.FALL_BLOCKING_GOO_QUANTA);
        assertEquals(0, TaintRules.FALL_MIN_Y);
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertEquals(double expected, double actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, 0.0D);
    }

    /** {@code reference/decompiled} under the repo root, or null when absent. */
    private static Path referenceRoot() {
        final List<Path> candidates = new java.util.ArrayList<>();
        final String property = System.getProperty("tc.referenceDecompiledDir", "");
        if (!property.isBlank()) {
            candidates.add(Path.of(property));
        }
        candidates.add(Path.of("../reference/decompiled").toAbsolutePath().normalize());
        candidates.add(Path.of("../../reference/decompiled").toAbsolutePath().normalize());
        for (final Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static void requireReadable(final Path path) {
        if (!Files.isRegularFile(path)) {
            throw new UncheckedIOException(new IOException("missing reference source " + path));
        }
    }
}
