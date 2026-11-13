package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-04f-m parity evidence (unit half): the shipped dimension data files
 * carry the original's flags verbatim, the lang names match the original's
 * {@code en_US.lang}, and the content-mapping rows this card owns are
 * flipped.
 *
 * <p>The originals are {@code WorldProviderOuter} (no skylight, no beds,
 * fog 0.15, height 128, coordinate scale 1, frozen noon),
 * {@code ChunkProviderOuter} (pure-air columns, Eldritch biome stamped over
 * every column — the flat generator's biome), {@code BiomeGenEldritch}
 * (black sky/fog, no weather) and the lang's
 * {@code tile.blockPortalEldritch.name = "Eldritch Portal"}; the biome's
 * display name is the biome name itself (the original ships no
 * {@code biome.Eldritch.name} key, so vanilla's fallback shows
 * {@code Eldritch}).</p>
 */
class OuterLandsParityTest {

    private static Path dataFile(final String... parts) {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        return Path.of(path).resolve("thaumcraft").resolve(Path.of(String.join("/", parts)));
    }

    private static JsonObject json(final Path file) throws IOException {
        assertTrue(Files.isRegularFile(file), file + " must ship");
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test
    void dimensionTypeCarriesTheWorldProviderOuterFlags() throws IOException {
        final JsonObject doc = json(dataFile("dimension_type", "outerlands.json"));
        // WorldProviderOuter verbatim: no daylight (isDaylight false), the
        // dimension never tracks the sun (no skylight), beds refuse (canRespawnHere
        // false), the outer coordinate scale is 1 (getTimeCoordinateScale /
        // getMovementCoordinateScale), the void fog (getFogDensity 0.1 -> the
        // ambient-light analogue vanilla names 0.15 for the End's blackout),
        // the 128 world height, frozen noon (calculateTimeOfDay 0? the
        // provider's getTimeOffset 0 + no daylight cycle: time_of_day 0).
        assertEquals(false, doc.get("ultrawarm").getAsBoolean(), "ultrawarm");
        assertEquals(false, doc.get("natural").getAsBoolean(), "natural");
        assertEquals(1.0, doc.get("coordinate_scale").getAsDouble(), 1.0e-9, "coordinate_scale");
        assertEquals(0.0, doc.get("time_of_day").getAsDouble(), 1.0e-9, "time_of_day (frozen noon)");
        assertEquals(false, doc.get("has_skylight").getAsBoolean(), "has_skylight");
        assertEquals(false, doc.get("has_ceiling").getAsBoolean(), "has_ceiling");
        assertEquals(0.15, doc.get("ambient_light").getAsDouble(), 1.0e-6, "ambient_light (void fog analogue)");
        assertEquals(false, doc.get("bed_works").getAsBoolean(), "bed_works");
        assertEquals(false, doc.get("respawn_anchor_works").getAsBoolean(), "respawn_anchor_works");
        assertEquals(false, doc.get("piglin_safe").getAsBoolean(), "piglin_safe");
        assertEquals(false, doc.get("has_raids").getAsBoolean(), "has_raids");
        assertEquals(0, doc.get("monster_spawn_light_level").getAsInt(), "monster_spawn_light_level");
        assertEquals(0, doc.get("monster_spawn_block_light_limit").getAsInt(), "monster_spawn_block_light_limit");
        assertEquals(128, doc.get("logical_height").getAsInt(), "logical_height (the 1.7.10 world height)");
        assertEquals(0, doc.get("min_y").getAsInt(), "min_y");
        assertEquals(128, doc.get("height").getAsInt(), "height");
        assertEquals("minecraft:the_end", doc.get("effects").getAsString(), "effects (the blackout sky profile)");
    }

    @Test
    void dimensionIsTheFlatVoidOverTheEldritchBiome() throws IOException {
        final JsonObject doc = json(dataFile("dimension", "dimension.-42.json"));
        assertEquals("thaumcraft:outerlands", doc.get("type").getAsString(), "level stem");
        final JsonObject generator = doc.getAsJsonObject("generator");
        assertEquals("minecraft:flat", generator.get("type").getAsString(),
                "ChunkProviderOuter analogue: the flat source");
        final JsonObject settings = generator.getAsJsonObject("settings");
        assertEquals("thaumcraft:biome.biomegeneldritch", settings.get("biome").getAsString(),
                "WorldChunkManagerHell(biomeEldritchLands) analogue: the flat biome");
        assertEquals(0, settings.getAsJsonArray("layers").size(),
                "ChunkProviderOuter.fill returns null: every column is air");
        assertEquals(0, settings.getAsJsonArray("structures").size(),
                "the Outer Lands runs no vanilla structures (func_73156_b)");
    }

    @Test
    void biomeCarriesTheBiomeGenEldritchFlags() throws IOException {
        // 1.21.1's datapack registry layout is data/<ns>/<registry>/<path>.json,
        // so the file name is the content-mapping id's path: the biome is
        // thaumcraft:biome.biomegeneldritch, hence biome.biomegeneldritch.json
        // (the game-test registry probe proved the ungapped name never loaded).
        final JsonObject doc = json(dataFile("worldgen", "biome", "biome.biomegeneldritch.json"));
        // BiomeGenEldritch: SPONGE palette on nothing (the void), no weather
        // (the outer world has no rain), the black sky/fog (setColor(0,0,0)).
        assertEquals(false, doc.get("has_precipitation").getAsBoolean(), "has_precipitation");
        assertEquals(0, doc.getAsJsonObject("effects").get("sky_color").getAsInt(), "sky_color");
        assertEquals(0, doc.getAsJsonObject("effects").get("fog_color").getAsInt(), "fog_color");
        // The original registers it at default id 195 with the name "Eldritch"
        // (content-mapping row default_id=195; the display name is the biome
        // name, the original ships no biome.Eldritch.name lang key).
        assertEquals(0.8, doc.get("temperature").getAsDouble(), 1.0e-6,
                "temperature (BiomeGenBase default 0.8, BiomeGenEldritch overrides nothing)");
        assertEquals(0.4, doc.get("downfall").getAsDouble(), 1.0e-6,
                "downfall (BiomeGenBase default 0.4)");
        final var spawners = doc.getAsJsonObject("spawners");
        for (final String category : List.of("monster", "creature", "ambient", "axolotls",
                "underground_water_creature", "water_creature", "water_ambient", "misc")) {
            assertEquals(0, spawners.getAsJsonArray(category).size(),
                    "the Eldritch biome spawns no " + category + " (no biome spawn list)");
        }
    }

    @Test
    void idsMatchTheFrozenContentMappingPaths() {
        assertEquals("thaumcraft:dimension.-42", OuterLands.OUTER_LANDS.location().toString(),
                "the content-mapping dimension new_id");
        assertEquals("thaumcraft:outerlands", OuterLands.DIMENSION_TYPE.location().toString());
        assertEquals("thaumcraft:biome.biomegeneldritch", OuterLands.BIOME_ELDRITCH.location().toString(),
                "the content-mapping biome new_id");
        assertEquals(50, OuterLands.ROOM_Y, "GenPortal/GenBossRoom build at y = 50 (the callers pass 50)");
    }

    @Test
    void langNamesMatchTheOriginal() throws IOException {
        final Path lang = Path.of(System.getProperty("tc.modAssetsDir")).resolve("thaumcraft/lang/en_us.json");
        final JsonObject entries = JsonParser.parseString(Files.readString(lang)).getAsJsonObject();
        // en_US.lang: tile.blockPortalEldritch.name = "Eldritch Portal"; the
        // biome name is the original's biome name "Eldritch" (no lang key in
        // the original — vanilla shows the name).
        assertEquals("Eldritch Portal", entries.get("block.thaumcraft.blockportaleldritch").getAsString());
        assertEquals("Eldritch", entries.get("biome.thaumcraft.biome.biomegeneldritch").getAsString());
    }

    @Test
    void contentMappingRowsAreFlippedToDone() throws IOException {
        final String path = System.getProperty("tc.contentMappingFile");
        final List<String> owned = List.of(
                "thaumcraft:blockportaleldritch", "thaumcraft:tileeldritchportal",
                "thaumcraft:dimension.-42", "thaumcraft:biome.biomegeneldritch",
                "thaumcraft:config.biomes.outer_lands_dim",
                "thaumcraft:worldgen.outer_lands_procedural_maze");
        final var rows = com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv.read(Path.of(path));
        for (final String id : owned) {
            final var hits = rows.stream().filter(r -> r.newId().equals(id)).toList();
            assertEquals(1, hits.size(), id + " must appear exactly once");
            final var row = hits.get(0);
            assertEquals("done", row.implementationStatus(), id + " implementationStatus");
            assertEquals("tested", row.testStatus(), id + " testStatus");
        }
    }
}
