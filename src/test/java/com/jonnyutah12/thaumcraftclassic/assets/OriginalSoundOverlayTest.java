package com.jonnyutah12.thaumcraftclassic.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the original-asset sound overlay (docs/original-asset-inventory.md,
 * docs/asset-mapping.csv, build.gradle overlayOriginalAssets/transformOriginalSoundsJson).
 *
 * <p>The original TC 4.2.3.5 sounds.json is 1.7.10 format: its file
 * references carry no namespace, and 1.21.1 resolves unnamespaced references
 * against the {@code minecraft} namespace — the TC-M2-03 client smoke run
 * logged a caught ResourceLocationException for
 * {@code minecraft:runicShieldEffect}, discarding the whole file. The build
 * overlay prefixes references with {@code thaumcraft:}, keeps event keys
 * unnamespaced (1.21.1 derives the event namespace from the assets/&lt;ns&gt;/
 * directory), and normalizes the two camelCase ids to snake_case, renaming
 * their oggs to match. These tests pin both sides of the contract: the CSV
 * mapping (always) and the generated overlay output (local builds that have
 * the reference tree; skipped on CI, matching the pipeline design in
 * build.gradle).</p>
 */
class OriginalSoundOverlayTest {

    /** An event key as 1.21.1 requires it in sounds.json: unnamespaced, lowercase. */
    private static final Pattern EVENT_KEY = Pattern.compile("[a-z0-9/._-]+");
    /** A sound file reference 1.21.1 resolves to the overlaid oggs. */
    private static final Pattern NAMESPACED_SOUND_ID =
            Pattern.compile("thaumcraft:[a-z0-9/._-]+");
    private static final Pattern LOWERCASE_OGG_PATH =
            Pattern.compile("sounds/[a-z0-9/._-]+\\.ogg");

    /** The normalization the overlay applies to original camelCase sound ids. */
    private static String normalizeLegacySoundId(String id) {
        return id.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    @Test
    void csvRecordsNormalizedSoundAssetPaths() throws IOException {
        Path csv = Path.of(System.getProperty("tc.assetMappingFile", ""));
        assertTrue(Files.isRegularFile(csv),
                "asset-mapping.csv not wired — run tests through ./gradlew test");

        boolean registryRowFound = false;
        int oggRows = 0;
        for (String[] row : parseCsv(Files.readString(csv))) {
            if (row.length < 3) {
                continue;
            }
            String original = row[0];
            String newAsset = row[1];
            String usedBy = row[2];

            if (original.equals("sounds.json")) {
                registryRowFound = true;
                assertEquals("sounds.json", newAsset,
                        "the sound registry must be overlaid at assets/thaumcraft/sounds.json");
                continue;
            }
            if (!original.startsWith("sounds/") || !original.endsWith(".ogg")) {
                continue;
            }
            oggRows++;
            if (newAsset.isEmpty()) {
                assertEquals("UNUSED", usedBy,
                        "only unused assets may have an empty new_asset: " + original);
                continue;
            }
            String leaf = original.substring("sounds/".length(), original.length() - ".ogg".length());
            String expected = "sounds/" + normalizeLegacySoundId(leaf) + ".ogg";
            assertTrue(LOWERCASE_OGG_PATH.matcher(newAsset).matches(),
                    "new_asset must be a valid lowercase 1.21.1 path: " + original + " -> " + newAsset);
            assertEquals(expected, newAsset,
                    "CSV new_asset must equal the overlay's normalized path for " + original);
        }

        assertTrue(registryRowFound, "asset-mapping.csv must contain the sounds.json registry row");
        assertEquals(111, oggRows,
                "the original TC 4.2.3.5 jar ships 111 sound files (docs/original-asset-inventory.md)");

        // Explicit regression guard for the flagged incident (kanban t_b71391d7):
        // the two camelCase ids must be recorded with their snake_case renames.
        assertEquals("sounds/runic_shield_effect.ogg",
                newAssetFor(Files.readString(csv), "sounds/runicShieldEffect.ogg"));
        assertEquals("sounds/runic_shield_charge.ogg",
                newAssetFor(Files.readString(csv), "sounds/runicShieldCharge.ogg"));
    }

    @Test
    void generatedOverlaySoundsJsonIsPlatformValid() throws IOException {
        Path soundsJson = Path.of(System.getProperty("tc.originalSoundsJson", ""));
        Path overlayDir = Path.of(System.getProperty("tc.originalAssetsOverlayDir", ""));
        Assumptions.assumeTrue(Files.isRegularFile(soundsJson),
                "original-asset overlay not generated (no ../reference tree) — "
                        + "CI asserts the CSV mapping only");

        JsonObject root = JsonParser.parseString(Files.readString(soundsJson)).getAsJsonObject();
        assertEquals(66, root.size(),
                "the original TC 4.2.3.5 sounds.json defines 66 events (docs/original-asset-inventory.md)");

        for (String legacy : List.of("runicShieldEffect", "runicShieldCharge")) {
            assertFalse(root.has(legacy), "unnormalized event id in overlay output: " + legacy);
        }
        for (String renamed : List.of("runic_shield_effect", "runic_shield_charge")) {
            assertTrue(root.has(renamed), "renamed event missing from overlay output: " + renamed);
        }

        for (var entry : root.entrySet()) {
            String event = entry.getKey();
            assertTrue(EVENT_KEY.matcher(event).matches(),
                    "event key must be an unnamespaced, valid 1.21.1 id (the namespace comes "
                            + "from the assets/<ns>/ directory): " + event);

            JsonObject definition = entry.getValue().getAsJsonObject();
            assertFalse(definition.has("category"),
                    "1.7.10 per-event category field must be dropped: " + event);

            JsonArray sounds = definition.getAsJsonArray("sounds");
            assertFalse(sounds.isEmpty(), "event with no sound files: " + event);
            for (JsonElement element : sounds) {
                String reference = element.isJsonPrimitive()
                        ? element.getAsString()
                        : element.getAsJsonObject().get("name").getAsString();
                assertTrue(NAMESPACED_SOUND_ID.matcher(reference).matches(),
                        "sound file reference must be a valid namespaced 1.21.1 id: "
                                + event + " -> " + reference);
                Path ogg = overlayDir.resolve("thaumcraft/sounds/"
                        + reference.substring(reference.indexOf(':') + 1) + ".ogg");
                assertTrue(Files.isRegularFile(ogg),
                        "referenced sound file missing from overlay output: " + ogg);
            }
        }
    }

    private static String newAssetFor(String csvContent, String originalAsset) {
        for (String[] row : parseCsv(csvContent)) {
            if (row.length >= 2 && row[0].equals(originalAsset)) {
                return row[1];
            }
        }
        throw new AssertionError("asset-mapping.csv has no row for " + originalAsset);
    }

    /** Minimal CSV line parser: the first two columns are paths, later columns may quote commas. */
    private static List<String[]> parseCsv(String content) {
        List<String[]> rows = new ArrayList<>();
        for (String line : content.split("\r?\n")) {
            if (line.isBlank() || line.startsWith("original_asset,")) {
                continue;
            }
            List<String> fields = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    fields.add(field.toString());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
            fields.add(field.toString());
            rows.add(fields.toArray(new String[0]));
        }
        return rows;
    }
}
