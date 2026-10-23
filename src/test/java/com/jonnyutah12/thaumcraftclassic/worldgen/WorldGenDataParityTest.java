package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Data-driven worldgen parity (TC-M5-05): the configured/placed feature and
 * biome-modifier JSONs must keep encoding the original
 * ThaumcraftWorldGenerator.generateOres parameters (attempt counts 18/20/8,
 * Y-bands, variant domains, config option names, additive biome wiring).
 *
 * <p>The GameTest (WorldGenGameTest) proves the same data decodes and runs;
 * this test pins the numbers as plain data so drift fails fast without the
 * game classpath (docs/testing.md).</p>
 */
class WorldGenDataParityTest {

    private static final String MOD = "thaumcraft";

    private static Path dataDir() {
        String dir = System.getProperty("tc.modDataDir", "");
        assertTrue(!dir.isEmpty() && Files.isDirectory(Path.of(dir)),
                "tc.modDataDir not wired — run tests through ./gradlew test");
        return Path.of(dir).resolve(MOD);
    }

    private static JsonObject readJson(String relative) throws IOException {
        Path path = dataDir().resolve(relative);
        assertTrue(Files.isRegularFile(path), "missing worldgen data file: " + relative);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static JsonObject configuredFeature(String name) throws IOException {
        return readJson("worldgen/configured_feature/" + name + ".json");
    }

    private static JsonArray placementOf(String name) throws IOException {
        return readJson("worldgen/placed_feature/" + name + ".json").getAsJsonArray("placement");
    }

    private static JsonObject placementStep(JsonArray placement, String type) {
        for (var element : placement) {
            JsonObject step = element.getAsJsonObject();
            if (type.equals(step.get("type").getAsString())) {
                return step;
            }
        }
        return fail("placement modifier missing: " + type);
    }

    // ---- configured features ----------------------------------------------

    @Test
    void cinnabarConfiguredFeatureMatchesOriginal() throws IOException {
        assertOreConfig(configuredFeature("ore_cinnabar"), 1, List.of("0"));
    }

    @Test
    void amberConfiguredFeatureMatchesOriginal() throws IOException {
        assertOreConfig(configuredFeature("ore_amber"), 1, List.of("7"));
    }

    @Test
    void infusedStoneConfiguredFeatureMatchesOriginal() throws IOException {
        // Original: WorldGenMinable(blockCustomOre, md, 6, stone), md = 1..6
        // per vein -> size 6, variant domain 1..6.
        assertOreConfig(configuredFeature("ore_infused_stone"), 6,
                List.of("1", "2", "3", "4", "5", "6"));
    }

    private static void assertOreConfig(JsonObject json, int size, List<String> variants) {
        assertEquals(MOD + ":tc_ore", json.get("type").getAsString());
        JsonObject config = json.getAsJsonObject("config");

        JsonArray rocks = config.getAsJsonArray("rocks");
        List<String> hosts = new ArrayList<>();
        for (var rock : rocks) {
            JsonObject predicate = rock.getAsJsonObject();
            assertEquals("minecraft:block_match", predicate.get("predicate_type").getAsString());
            hosts.add(predicate.get("block").getAsString());
        }
        // Original host: Blocks.stone only (1.7.10 had no deepslate). The port
        // adds deepslate as its below-transition equivalent so attempt
        // frequency is unchanged; nothing else may be replaceable.
        assertEquals(List.of("minecraft:stone", "minecraft:deepslate"), hosts);

        JsonArray states = config.getAsJsonArray("states");
        assertEquals(variants.size(), states.size());
        for (int i = 0; i < variants.size(); i++) {
            JsonObject state = states.get(i).getAsJsonObject();
            assertEquals(MOD + ":blockcustomore", state.get("Name").getAsString());
            assertEquals(variants.get(i),
                    state.getAsJsonObject("Properties").get("variant").getAsString());
        }

        assertEquals(size, config.get("size").getAsInt());
    }

    // ---- placed features ----------------------------------------------------

    @Test
    void cinnabarPlacementMatchesOriginalAttemptsAndBand() throws IOException {
        JsonArray placement = placementOf("ore_cinnabar");
        // Original: 18 attempts/chunk, Y = nextInt(height/5) = 0..50 absolute.
        assertGate(placement, "World_Generation.generate_cinnibar_ore"); // Config.java:370
        assertEquals(18, placementStep(placement, "minecraft:count").get("count").getAsInt());
        assertTrue(hasType(placement, "minecraft:in_square"));
        JsonObject height = placementStep(placement, "minecraft:height_range").getAsJsonObject("height");
        assertEquals("minecraft:uniform", height.get("type").getAsString());
        assertEquals(0, height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
        assertEquals(50, height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
    }

    @Test
    void amberPlacementMatchesOriginalAttemptsAndBand() throws IOException {
        JsonArray placement = placementOf("ore_amber");
        // Original: 20 attempts/chunk, Y = heightValue - nextInt(25).
        assertGate(placement, "World_Generation.generate_amber_ore"); // Config.java:371
        assertEquals(20, placementStep(placement, "minecraft:count").get("count").getAsInt());
        assertTrue(hasType(placement, "minecraft:in_square"));
        JsonObject band = placementStep(placement, MOD + ":surface_below");
        assertEquals(0, band.get("min_offset").getAsInt());
        assertEquals(24, band.get("max_offset").getAsInt());
    }

    @Test
    void infusedStonePlacementMatchesOriginalAttemptsAndBand() throws IOException {
        JsonArray placement = placementOf("ore_infused_stone");
        // Original: 8 attempts/chunk, Y = nextInt(max(5, heightValue - 5)).
        assertGate(placement, "World_Generation.generate_infused_stone"); // Config.java:372
        assertEquals(8, placementStep(placement, "minecraft:count").get("count").getAsInt());
        assertTrue(hasType(placement, "minecraft:in_square"));
        JsonObject band = placementStep(placement, MOD + ":surface_capped_uniform");
        assertEquals(0, band.get("min").getAsInt());
        assertEquals(5, band.get("floor").getAsInt());
        assertEquals(-5, band.get("surface_offset").getAsInt());
    }

    private static void assertGate(JsonArray placement, String optionPath) {
        // The gate must be first: with the option off nothing downstream runs.
        assertEquals(MOD + ":config_gate", placement.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(optionPath, placementStep(placement, MOD + ":config_gate").get("option").getAsString());
    }

    private static boolean hasType(JsonArray placement, String type) {
        for (var element : placement) {
            if (type.equals(element.getAsJsonObject().get("type").getAsString())) {
                return true;
            }
        }
        return false;
    }

    // ---- biome wiring --------------------------------------------------------

    @Test
    void biomeModifierAddsAllThreeOresToOverworldAdditively() throws IOException {
        JsonObject modifier = readJson("neoforge/biome_modifier/tc_ores.json");
        // Additive: the original generator never removed vanilla worldgen.
        assertEquals("neoforge:add_features", modifier.get("type").getAsString());
        // Original ran generateOres for every non-Nether, non-End dimension;
        // vanilla-side that is the overworld biome set.
        assertEquals("#minecraft:is_overworld", modifier.get("biomes").getAsString());
        assertEquals("underground_ores", modifier.get("step").getAsString());
        List<String> features = new ArrayList<>();
        modifier.getAsJsonArray("features").forEach(e -> features.add(e.getAsString()));
        assertEquals(List.of(MOD + ":ore_cinnabar", MOD + ":ore_amber", MOD + ":ore_infused_stone"),
                features);
    }

    // ---- block assets ---------------------------------------------------------

    @Test
    void blockstateCoversAllEightOriginalVariants() throws IOException {
        JsonObject blockstate = JsonParser.parseString(Files.readString(
                        modAsset("blockstates/blockcustomore.json")))
                .getAsJsonObject();
        JsonObject variants = blockstate.getAsJsonObject("variants");
        for (int variant = 0; variant <= 7; variant++) {
            assertTrue(variants.has("variant=" + variant), "blockstate missing variant " + variant);
        }
    }

    /** assets/<mod>/... — sibling of the data dir wired as tc.modDataDir. */
    private static Path resourceRoot() {
        return Path.of(System.getProperty("tc.modDataDir")).getParent();
    }

    private static Path modAsset(String relative) {
        return resourceRoot().resolve("assets").resolve(MOD).resolve(relative);
    }

    @Test
    void oreIsInPickaxeMineableTag() throws IOException {
        JsonObject tag = JsonParser.parseString(Files.readString(
                        Path.of(System.getProperty("tc.modDataDir"))
                                .resolve("minecraft/tags/block/mineable/pickaxe.json")))
                .getAsJsonObject();
        List<String> values = new ArrayList<>();
        tag.getAsJsonArray("values").forEach(e -> values.add(e.getAsString()));
        assertTrue(values.contains(MOD + ":blockcustomore"),
                "blockcustomore not in mineable/pickaxe (original required a pickaxe)");
    }
}
