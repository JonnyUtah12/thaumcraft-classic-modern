package com.jonnyutah12.thaumcraftclassic.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura.Entry;

/**
 * The biome aura table must stay a 1:1 transcription of the original
 * {@code Config.registerBiomes()} rows (reference/decompiled/thaumcraft/
 * thaumcraft/common/config/Config.java:837-867) plus the
 * {@code BiomeHandler.getBiomeAura}/{@code getRandomBiomeTag}/
 * {@code getBiomeSupportsGreatwood} rules
 * (common/lib/world/biomes/BiomeHandler.java). Values are pinned from
 * docs/balance-values.md §6.1.
 */
class BiomeAuraTableParityTest {

    /** (dictionaryType, aura, aspectTag, greatwood, greatwoodChance) exactly as registered by the original. */
    private static final Object[][] REFERENCE_ROWS = {
            {"WATER", 100, "aqua", false, 0.0F},
            {"OCEAN", 120, "aqua", false, 0.0F},
            {"RIVER", 100, "aqua", false, 0.0F},
            {"WET", 80, "aqua", false, 0.0F},
            {"HOT", 100, "ignis", false, 0.0F},
            {"DESERT", 100, "ignis", false, 0.0F},
            {"NETHER", 120, "ignis", false, 0.0F},
            {"MESA", 80, "ignis", false, 0.0F},
            {"DENSE", 100, "ordo", false, 0.0F},
            {"SNOWY", 80, "ordo", false, 0.0F},
            {"COLD", 80, "ordo", false, 0.0F},
            {"FROZEN", 100, "ordo", false, 0.0F},
            {"MUSHROOM", 140, "ordo", false, 0.0F},
            {"CONIFEROUS", 100, "terra", true, 0.2F},
            {"FOREST", 120, "terra", true, 1.0F},
            {"SANDY", 80, "terra", false, 0.0F},
            {"BEACH", 80, "terra", false, 0.0F},
            {"SAVANNA", 80, "aer", true, 0.2F},
            {"MOUNTAIN", 100, "aer", false, 0.0F},
            {"HILLS", 120, "aer", false, 0.0F},
            {"PLAINS", 80, "aer", true, 0.2F},
            {"DRY", 80, "perditio", false, 0.0F},
            {"SPARSE", 80, "perditio", false, 0.0F},
            {"SWAMP", 120, "perditio", true, 0.2F},
            {"WASTELAND", 80, "perditio", false, 0.0F},
            {"JUNGLE", 100, "herba", false, 0.0F},
            {"LUSH", 100, "herba", true, 0.5F},
            {"MAGICAL", 100, null, true, 1.0F},
            {"END", 80, "vacuos", false, 0.0F},
            {"SPOOKY", 80, "spiritus", false, 0.0F},
            {"DEAD", 50, "mortuus", false, 0.0F},
    };

    @Test
    void tableIsTheOriginalRowForRow() {
        assertEquals(REFERENCE_ROWS.length, BiomeAura.TABLE.size(),
                "row count drifted from Config.registerBiomes (31 rows)");
        for (int i = 0; i < REFERENCE_ROWS.length; i++) {
            final Object[] expected = REFERENCE_ROWS[i];
            final Entry actual = BiomeAura.TABLE.get(i);
            assertEquals(expected[0], actual.dictionaryType(), "row " + i + " type");
            assertEquals(expected[1], actual.aura(), "row " + i + " aura");
            assertEquals(expected[2], actual.aspectTag(), "row " + i + " aspect");
            assertEquals(expected[3], actual.greatwood(), "row " + i + " greatwood");
            assertEquals((Float) expected[4], actual.greatwoodChance(), "row " + i + " greatwood chance");
        }
    }

    @Test
    void everyRowCarriesTheConventionTag() {
        // The BiomeDictionary stand-in mapping must be complete: no null tags.
        for (final Entry entry : BiomeAura.TABLE) {
            assertTrue(entry.tag() != null, "missing biome tag for " + entry.dictionaryType());
            assertEquals("c", entry.tag().location().getNamespace(), entry.dictionaryType() + " must use the c: convention namespace");
        }
    }

    private static List<Entry> rows(final String... types) {
        return java.util.Arrays.stream(types)
                .map(t -> BiomeAura.TABLE.stream()
                        .filter(e -> e.dictionaryType().equals(t)).findFirst().orElseThrow())
                .toList();
    }

    @Test
    void averageOverMatchedTypesRule() {
        // Original: integer average of every matched type's aura level.
        assertEquals(120, BiomeAura.averageAura(rows("OCEAN")));
        assertEquals(110, BiomeAura.averageAura(rows("OCEAN", "WATER")));
        // Integer division, truncating, exactly like the original:
        // (120 + 100 + 100) / 3 = 106.
        assertEquals(106, BiomeAura.averageAura(rows("OCEAN", "WATER", "RIVER")));
    }

    @Test
    void unmatchedBiomesFallBackTo100() {
        // Original: exception path in getBiomeAura returns 100.
        assertEquals(100, BiomeAura.averageAura(List.of()));
        assertEquals(100, BiomeAura.FALLBACK_AURA);
    }

    @Test
    void randomTagIsUniformOverMatchedAndNullWhenUnmatched() {
        assertNull(BiomeAura.pickRandomTag(List.of(), new Random(0)));
        final List<Entry> matched = List.of(
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("MAGICAL")).findFirst().orElseThrow());
        // MAGICAL carries no aspect in the original — the pick returns null.
        assertNull(BiomeAura.pickRandomTag(matched, new Random(7)));
        final List<Entry> two = List.of(
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("WATER")).findFirst().orElseThrow(),
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("DESERT")).findFirst().orElseThrow());
        // Uniform over matched rows: both must appear across many draws.
        boolean sawAqua = false;
        boolean sawIgnis = false;
        final Random random = new Random(1);
        for (int i = 0; i < 200; i++) {
            final String tag = BiomeAura.pickRandomTag(two, random);
            sawAqua |= "aqua".equals(tag);
            sawIgnis |= "ignis".equals(tag);
        }
        assertTrue(sawAqua && sawIgnis, "pick is not uniform over matched rows");
    }

    @Test
    void greatwoodFlagsMatchTheOriginalTable() {
        // Original getBiomeSupportsGreatwood: first matched greatwood row wins.
        final List<Entry> forestThenPlains = List.of(
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("FOREST")).findFirst().orElseThrow(),
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("PLAINS")).findFirst().orElseThrow());
        assertEquals(1.0F, firstGreatwood(forestThenPlains));
        final List<Entry> plainsOnly = List.of(
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("PLAINS")).findFirst().orElseThrow());
        assertEquals(0.2F, firstGreatwood(plainsOnly));
        assertEquals(0.0F, firstGreatwood(List.of(
                BiomeAura.TABLE.stream().filter(e -> e.dictionaryType().equals("DESERT")).findFirst().orElseThrow())));
    }

    private static float firstGreatwood(final List<Entry> matched) {
        for (final Entry entry : matched) {
            if (entry.greatwood()) {
                return entry.greatwoodChance();
            }
        }
        return 0.0F;
    }
}
