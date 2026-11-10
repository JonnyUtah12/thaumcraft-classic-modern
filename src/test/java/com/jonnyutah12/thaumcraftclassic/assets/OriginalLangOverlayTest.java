package com.jonnyutah12.thaumcraftclassic.assets;

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
 * Guards the language pipeline (docs/architecture/localization.md,
 * docs/asset-mapping.csv, build.gradle transformOriginalLangJson).
 *
 * <p>TC 4.2.3.5 ships its strings as 1.7.10 {@code lang/<locale>.lang} key=value
 * files. 1.21.1 does not read them at all: it loads
 * {@code assets/<ns>/lang/<locale>.json} keyed {@code <type>.<namespace>.<path>}
 * — the platform's own files show the form ({@code assets/minecraft/lang/en_us.json}
 * carries {@code block.minecraft.acacia_wood}, NeoForge's carries
 * {@code fml.menu.mods}). The mod shipped no language file, so every name fell
 * back to its raw registry key — the player-reported TC-M7-01 defect B, a tooltip
 * reading {@code block.thaumcraft.blockcustomore}.</p>
 *
 * <p>One file ships, two sources feed it: the user's own extracted {@code .lang}
 * files converted at build time (the strings are All Rights Reserved and stay
 * out of the repository, D-004) and the committed port-authored names under
 * {@code src/main/lang-port-authored/en_us.json} for ids the port invented. The
 * original wins when both define a key. As in OriginalSoundOverlayTest the CSV
 * contract is asserted on every build; the converted output is asserted only when
 * the reference tree exists, while the port-authored half is asserted always — a
 * code-only build must still resolve its own ids.</p>
 */
class OriginalLangOverlayTest {

    /** A key of a 1.21.1 language file. */
    private static final Pattern LANGUAGE_KEY =
            Pattern.compile("(block|item|entity|fluid)\\.[a-z][a-z0-9\\-]*\\.[a-z0-9._/\\-]+");
    /** A locale code, as the platform names language files (en_us, de_de). */
    private static final Pattern LOCALE = Pattern.compile("[a-z]{2}_[a-z]{2}");
    /** Key forms only the 1.7.10 `.lang` format used — must not survive. */
    private static final Pattern LEGACY_NAME_KEY =
            Pattern.compile("(tile|item|entity)\\.[A-Za-z0-9_.]*\\.name");

    private static Path languageDir() {
        final String dir = System.getProperty("tc.langPackDir", "");
        assertTrue(!dir.isEmpty(), "tc.langPackDir is not set — run the tests through ./gradlew test");
        return Path.of(dir);
    }

    private static Path portLangFile() {
        final String path = System.getProperty("tc.portLangFile", "");
        assertTrue(!path.isEmpty() && Files.isRegularFile(Path.of(path)),
                "tc.portLangFile is not set or missing — run the tests through ./gradlew test");
        return Path.of(path);
    }

    private static Path referenceLangFile() {
        return Path.of(System.getProperty("tc.originalLangFile", ""));
    }

    @Test
    void csvRecordsPlatformLanguagePaths() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.assetMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "asset-mapping.csv not wired — run tests through ./gradlew test");

        int localeRows = 0;
        for (String[] row : parseCsv(Files.readString(csv))) {
            if (row.length < 2 || !row[0].startsWith("lang/") || !row[0].endsWith(".lang")) {
                continue;
            }
            final String locale = row[0].substring("lang/".length(), row[0].length() - ".lang".length());
            assertTrue(LOCALE.matcher(locale.toUpperCase(Locale.ROOT)).matches(),
                    "not a locale asset: " + row[0]);
            assertEquals("lang/" + locale.toLowerCase(Locale.ROOT) + ".json", row[1],
                    row[0] + " must map to assets/thaumcraft/lang/<lowercase locale>.json — 1.21.1 reads"
                            + " assets/<ns>/lang/<locale>.json, there is no translations/ directory");
            localeRows++;
        }
        assertEquals(22, localeRows,
                "the original TC 4.2.3.5 jar ships 22 .lang locales (docs/original-asset-inventory.md)");
    }

    @Test
    void portAuthorNamesShipInACodeOnlyBuild() throws IOException {
        final JsonObject port = JsonParser.parseString(Files.readString(portLangFile())).getAsJsonObject();
        assertTrue(port.size() > 0, "the port-authored language file must name the port's own ids");

        final Path built = languageDir().resolve("en_us.json");
        assertTrue(Files.isRegularFile(built),
                "the built mod carries no assets/thaumcraft/lang/en_us.json — every name renders as a raw key");
        final JsonObject merged = JsonParser.parseString(Files.readString(built)).getAsJsonObject();

        for (var entry : merged.entrySet()) {
            final String key = entry.getKey();
            assertTrue(LANGUAGE_KEY.matcher(key).matches(),
                    "shipped language key " + key + " is not <type>.<namespace>.<path> as 1.21.1 requires");
            assertFalse(LEGACY_NAME_KEY.matcher(key).matches(), "legacy 1.7.10 key shipped: " + key);
            final String value = entry.getValue().getAsString();
            assertFalse(value.isEmpty(), "language key " + key + " maps to an empty name");
        }

        for (var entry : port.entrySet()) {
            assertTrue(merged.has(entry.getKey()),
                    "port-authored key " + entry.getKey() + " is missing from the shipped en_us.json");
        }
        for (final String probe : List.of("block.thaumcraft.test_probe_block", "item.thaumcraft.test_probe_item")) {
            assertTrue(merged.has(probe), "probe id " + probe + " has no display name in a code-only build");
        }
    }

    @Test
    void convertedOriginalLanguageIsPlatformValid() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(referenceLangFile()),
                "no ../reference tree (CI): the .lang conversion cannot be asserted here");

        final Path dir = languageDir();
        final Path enUs = dir.resolve("en_us.json");
        assertTrue(Files.isRegularFile(enUs), "the build produced no language file");
        final JsonObject language = JsonParser.parseString(Files.readString(enUs)).getAsJsonObject();

        for (final String locale : List.of("ca_es", "cs_cz", "de_de", "en_us", "fr_fr")) {
            assertTrue(Files.isRegularFile(dir.resolve(locale + ".json")),
                    "locale " + locale + " was not converted");
        }

        int legacy = 0;
        for (var entry : language.entrySet()) {
            final String key = entry.getKey();
            assertTrue(LANGUAGE_KEY.matcher(key).matches(), "key not in 1.21.1 form: " + key);
            if (LEGACY_NAME_KEY.matcher(key).matches()) {
                legacy++;
            }
        }
        assertEquals(0, legacy, "no key may remain in the 1.7.10 tile./item. .name form");

        // The reported defect, checked against the user's own original file: the
        // original string must survive byte for byte — the port rewords nothing.
        final String langText = Files.readString(referenceLangFile());
        final String original = originalValueFor(langText, "tile.blockCustomOre.0.name");
        assertNotNull(original, "en_US.lang has no tile.blockCustomOre.0.name row");
        assertTrue(language.has("block.thaumcraft.blockcustomore"),
                "no language key for the reported id block.thaumcraft.blockcustomore");
        assertEquals(original, language.get("block.thaumcraft.blockcustomore").getAsString(),
                "block.thaumcraft.blockcustomore must resolve to the original display string");

        // A port-authored name must never shadow an original one.
        final JsonObject port = JsonParser.parseString(Files.readString(portLangFile())).getAsJsonObject();
        for (var entry : port.entrySet()) {
            final String key = entry.getKey();
            final String legacyKey = legacyKeyForLanguageKey(key);
            if (legacyKey == null || !language.has(key)) {
                continue;
            }
            final String originalName = originalValueFor(langText, legacyKey);
            assertTrue(originalName == null || originalName.equals(language.get(key).getAsString()),
                    "the shipped value for " + key + " shadows the original string");
        }
    }

    /** The 1.7.10 lang key a shipped language key would have come from. */
    private static String legacyKeyForLanguageKey(final String languageKey) {
        final String[] parts = languageKey.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        final String legacyType = switch (parts[0]) {
            case "block" -> "tile";
            case "item", "entity" -> parts[0];
            default -> null;
        };
        return legacyType == null ? null : legacyType + "." + parts[2] + ".name";
    }

    /** Value of one `key=value` line of a 1.7.10 .lang file, or null. */
    private static String originalValueFor(final String langText, final String key) {
        for (final String line : langText.split("\\R")) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                continue;
            }
            final int equals = trimmed.indexOf('=');
            if (trimmed.substring(0, equals).trim().equals(key)) {
                return trimmed.substring(equals + 1);
            }
        }
        return null;
    }

    /** The same minimal CSV reader OriginalSoundOverlayTest uses. */
    private static List<String[]> parseCsv(final String content) {
        final List<String[]> rows = new ArrayList<>();
        for (String line : content.split("\\R")) {
            if (line.isBlank() || line.startsWith("original_asset,")) {
                continue;
            }
            final List<String> fields = new ArrayList<>();
            final StringBuilder field = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < line.length(); i++) {
                final char c = line.charAt(i);
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
