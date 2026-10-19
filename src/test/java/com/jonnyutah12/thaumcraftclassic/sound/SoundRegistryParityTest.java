package com.jonnyutah12.thaumcraftclassic.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M7-05 parity contract: the committed sound-event registry
 * ({@link ModSounds}), the asset mapping (docs/asset-mapping.csv), and the
 * 1.21.1 sound definitions produced by the D-004 asset overlay must describe
 * exactly the same 66 events over the same 111 ogg files as the original
 * Thaumcraft 4.2.3.5 jar (docs/original-asset-inventory.md,
 * docs/sound-events.md).
 *
 * <p>The CSV half always runs (CI-safe). The overlay half is skipped when the
 * reference tree is absent, matching the pipeline design in build.gradle —
 * there the registry/CSV halves still pin the event-id set.</p>
 *
 * <p>The overlay file-existence walk is the static equivalent of
 * SoundManager's runtime check ("File ... does not exist, cannot add it to
 * event ..."): if this test passes, a client run cannot log a missing-sound
 * warning for any TC event.</p>
 */
class SoundRegistryParityTest {

    private static final Pattern SOUND_EVENT_NOTE = Pattern.compile("sound event: ([a-z_]+)");

    @Test
    void registryMatchesCsvEventSet() throws IOException {
        Set<String> csvEvents = csvSoundEvents();
        List<String> registryIds = ModSounds.eventIds().stream()
                .map(id -> {
                    assertEquals("thaumcraft", id.getNamespace(), "sound event outside the thaumcraft namespace: " + id);
                    return id.getPath();
                })
                .toList();

        assertEquals(66, registryIds.size(), "the original sounds.json defines 66 events");
        assertEquals(registryIds.size(), Set.copyOf(registryIds).size(), "duplicate event id in ModSounds");
        assertEquals(Set.copyOf(registryIds), csvEvents,
                "ModSounds event ids must equal the per-ogg 'sound event:' set in asset-mapping.csv");
    }

    @Test
    void generatedOverlaySoundDefinitionsAreComplete() throws IOException {
        Path overlayDir = overlayDir();
        String wiredJson = System.getProperty("tc.originalSoundsJson", "");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                !wiredJson.isEmpty() && Files.isRegularFile(Path.of(wiredJson)),
                "converted sounds.json not generated (no ../reference tree) — CI asserts the CSV half only");
        Path soundsJson = Path.of(wiredJson);

        JsonObject root = JsonParser.parseString(Files.readString(soundsJson)).getAsJsonObject();
        Set<String> expectedIds = new HashSet<>(ModSounds.eventIds().stream().map(id -> id.getPath()).toList());
        assertEquals(66, root.size(), "converted sounds.json must carry all 66 events");

        Set<String> referencedOggs = new HashSet<>();
        for (String event : root.keySet()) {
            // SoundManager namespaces keys by the sounds.json file namespace
            // (net/minecraft/client/sounds/SoundManager.load), and ':' is not
            // a valid path char — keys must be the bare event ids.
            assertTrue(expectedIds.contains(event), "unknown event id in converted sounds.json: " + event);

            JsonObject definition = root.get(event).getAsJsonObject();
            assertFalse(definition.has("category"), "1.7.10 category field must be dropped: " + event);

            JsonArray sounds = definition.getAsJsonArray("sounds");
            assertFalse(sounds.isEmpty(), "event with no sound files: " + event);
            for (JsonElement element : sounds) {
                String reference = element.isJsonPrimitive()
                        ? element.getAsString()
                        : element.getAsJsonObject().get("name").getAsString();
                assertTrue(reference.startsWith("thaumcraft:"),
                        "sound file reference must be namespaced (1.21.1 defaults bare names to minecraft): "
                                + event + " -> " + reference);
                String path = reference.substring("thaumcraft:".length());
                // Sound.getPath() resolves ids through FileToIdConverter("sounds", ".ogg")
                assertTrue(Files.isRegularFile(overlayDir.resolve("thaumcraft/sounds/" + path + ".ogg")),
                        "missing sound file for event " + event + ": thaumcraft/sounds/" + path + ".ogg");
                referencedOggs.add(path + ".ogg");
            }
        }
        assertEquals(expectedIds, root.keySet(), "converted sounds.json event set != ModSounds registry");

        // No orphan oggs: every overlaid sound file is referenced by an event.
        Set<String> onDisk = new HashSet<>();
        Path soundsDir = overlayDir.resolve("thaumcraft/sounds");
        try (Stream<Path> walk = Files.walk(soundsDir)) {
            walk.filter(p -> p.toString().endsWith(".ogg")).forEach(p -> onDisk.add(
                    soundsDir.relativize(p).toString().replace('\\', '/')));
        }
        assertEquals(111, onDisk.size(), "the original jar ships 111 sound files");
        assertEquals(onDisk, referencedOggs,
                "every overlaid ogg must be referenced by exactly one event (and vice versa)");
    }

    @Test
    void csvNewAssetPathsExistInOverlay() throws IOException {
        Path overlayDir = overlayDir();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(overlayDir.resolve("thaumcraft/sounds")),
                "original-asset overlay not generated — CI asserts the CSV half only");

        int checked = 0;
        for (String[] row : parseCsv(Files.readString(Path.of(csvFile())))) {
            if (row.length < 2 || !row[0].startsWith("sounds/") || !row[0].endsWith(".ogg") || row[1].isEmpty()) {
                continue;
            }
            assertTrue(Files.isRegularFile(overlayDir.resolve("thaumcraft").resolve(row[1])),
                    "CSV new_asset has no overlaid file: " + row[0] + " -> " + row[1]);
            checked++;
        }
        assertEquals(110, checked, "110 of the 111 ogg rows carry a new_asset (urnbreak.ogg is UNUSED)");
    }

    // --- helpers -------------------------------------------------------------

    private static Set<String> csvSoundEvents() throws IOException {
        Set<String> events = new HashSet<>();
        for (String[] row : parseCsv(Files.readString(Path.of(csvFile())))) {
            if (row.length < 5 || !row[0].startsWith("sounds/") || !row[0].endsWith(".ogg")) {
                continue;
            }
            Matcher m = SOUND_EVENT_NOTE.matcher(row[4]);
            assertTrue(m.find(), "sound row without 'sound event:' note: " + row[0]);
            events.add(m.group(1));
        }
        return events;
    }

    private static String csvFile() {
        String path = System.getProperty("tc.assetMappingFile", "");
        assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "asset-mapping.csv not wired — run tests through ./gradlew test");
        return path;
    }

    private static Path overlayDir() {
        String wired = System.getProperty("tc.originalAssetsOverlayDir", "");
        if (!wired.isEmpty()) {
            return Path.of(wired);
        }
        // Same output dir build.gradle's overlayOriginalAssets Sync writes to.
        return Path.of("build/generated/resources/original-assets/assets");
    }

    /** Minimal RFC-4180 CSV reader (quotes, doubled quotes, CRLF). */
    private static List<String[]> parseCsv(String text) {
        List<String[]> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                rows.add(fields.toArray(new String[0]));
                fields = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
            rows.add(fields.toArray(new String[0]));
        }
        return rows;
    }
}
