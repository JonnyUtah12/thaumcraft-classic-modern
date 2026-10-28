package com.jonnyutah12.thaumcraftclassic.assets;

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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the original-asset TEXTURE overlay (docs/original-asset-inventory.md,
 * docs/asset-mapping.csv, build.gradle overlayOriginalAssets).
 *
 * <p>The bug this pins (kanban t_2c24f643, player screenshot 2026-10-28): every
 * block and item rendered the missing-texture magenta. A 1.21.1 model asks for
 * the sprite {@code thaumcraft:block/amberore}, which resolves to
 * {@code assets/thaumcraft/textures/block/amberore.png} — SINGULAR. Minecraft
 * 1.14 dropped the 1.7.10 plural texture directory names: the 1.21.1 client jar
 * ships {@code assets/minecraft/textures/block/} (1061 files) and
 * {@code /item/} (605) and has no {@code blocks/} or {@code items/} at all. The
 * overlaid 1.7.10 tree only ever supplied {@code textures/blocks/} and
 * {@code textures/items/}, so not one original texture was reachable at the
 * path a model asks for.</p>
 *
 * <p>The fix is a copy-time directory rename in {@code overlayOriginalAssets}
 * (the ARR source tree is never modified — D-004) plus the matching correction
 * of the recorded contract, docs/asset-mapping.csv. Only {@code blocks} and
 * {@code items} rename: the 1.21.1 client jar keeps {@code gui}, {@code misc}
 * and {@code models} plural, and {@code aspects} / {@code foci} are Thaumcraft
 * directories addressed by literal path ({@code Aspect#iconTexture} builds
 * {@code thaumcraft:textures/aspects/<tag>.png}), so moving them would break
 * the one reference that is not model-driven.</p>
 *
 * <p>The CSV assertion runs everywhere, CI included (matching the pipeline
 * design in build.gradle). The overlay-output assertions are
 * assumption-guarded exactly like {@link OriginalSoundOverlayTest}: CI has no
 * reference tree, so CI asserts the contract; a local build asserts the bytes
 * that land in the jar.</p>
 */
class OriginalTextureOverlayTest {

    /**
     * 1.7.10 texture directory -> 1.21.1 texture directory, measured against
     * the 1.21.1 client jar ({@code assets/minecraft/textures/*}).
     */
    private static final Map<String, String> LEGACY_DIR_RENAMES = Map.of(
            "blocks", "block",
            "items", "item");

    /** 1.7.10 texture directories 1.21.1 keeps unchanged — verified, not assumed. */
    private static final Set<String> KEPT_DIRS = Set.of("gui", "misc", "models", "aspects", "foci");

    /**
     * Files per 1.7.10 texture directory in the reference tree
     * (docs/original-asset-inventory.md, "Inventory by category").
     */
    private static final Map<String, Integer> FILES_PER_LEGACY_DIR = Map.ofEntries(
            Map.entry("blocks", 262),
            Map.entry("items", 291),
            Map.entry("models", 216),
            Map.entry("misc", 77),
            Map.entry("aspects", 50),
            Map.entry("gui", 23),
            Map.entry("foci", 21));

    /** The 1.21.1 path an overlaid 1.7.10 texture must be published at. */
    private static String modernTexturePath(String legacyPath) {
        String[] parts = legacyPath.split("/");
        String renamed = LEGACY_DIR_RENAMES.get(parts[1]);
        if (renamed != null) {
            parts[1] = renamed;
        }
        return String.join("/", parts);
    }

    /**
     * The recorded contract: every texture row in docs/asset-mapping.csv must
     * target the 1.21.1 layout. This is the half a pipeline-only fix would
     * contradict, and it fails on the pre-fix identity mapping.
     */
    @Test
    void csvTextureTargetsUseTheModernLayout() throws IOException {
        Path csv = Path.of(System.getProperty("tc.assetMappingFile", ""));
        assertTrue(Files.isRegularFile(csv),
                "asset-mapping.csv not wired — run tests through ./gradlew test");

        String csvText = Files.readString(csv);
        Map<String, Integer> rowsPerLegacyDir = new TreeMap<>();
        int textureRows = 0;
        int unusedRows = 0;

        for (String[] row : parseCsv(csvText)) {
            if (row.length < 5 || !row[0].startsWith("textures/")) {
                continue;
            }
            String original = row[0];
            String newAsset = row[1];
            textureRows++;

            String legacyDir = original.split("/")[1];
            rowsPerLegacyDir.merge(legacyDir, 1, Integer::sum);
            assertTrue(LEGACY_DIR_RENAMES.containsKey(legacyDir) || KEPT_DIRS.contains(legacyDir),
                    "unexpected 1.7.10 texture directory in the CSV — measure it against the "
                            + "1.21.1 client jar before accepting it: " + original);

            if (newAsset.isEmpty()) {
                unusedRows++;
                assertEquals("UNUSED", row[2],
                        "only unused assets may have an empty new_asset: " + original);
                continue;
            }

            assertTrue(newAsset.startsWith("textures/"),
                    "a texture row must target a texture path: " + original + " -> " + newAsset);
            String targetDir = newAsset.split("/")[1];
            assertFalse(LEGACY_DIR_RENAMES.containsKey(targetDir),
                    "new_asset still names the 1.7.10 plural directory, which 1.21.1 never "
                            + "resolves for a block/item sprite: " + original + " -> " + newAsset);
            assertEquals(modernTexturePath(original), newAsset,
                    "new_asset must be the 1.21.1 path of the original texture: " + original);
        }

        assertEquals(940, textureRows,
                "the reference tree ships 940 texture files (docs/original-asset-inventory.md)");
        assertEquals(36, unusedRows,
                "42 assets are unused overall (docs/original-asset-inventory.md); 36 of them are textures");
        assertEquals(FILES_PER_LEGACY_DIR.keySet(), rowsPerLegacyDir.keySet(),
                "CSV texture directories (measured: " + rowsPerLegacyDir + ")");
        for (Map.Entry<String, Integer> expected : FILES_PER_LEGACY_DIR.entrySet()) {
            assertEquals(expected.getValue(), rowsPerLegacyDir.get(expected.getKey()),
                    "CSV row count for textures/" + expected.getKey() + "/");
        }

        // Regression guard for the reported incident: the ores from the
        // screenshot, and the animated pair — a .mcmeta must travel with its png.
        assertEquals("textures/block/amberore.png",
                newAssetFor(csvText, "textures/blocks/amberore.png"));
        assertEquals("textures/block/cinnibar.png",
                newAssetFor(csvText, "textures/blocks/cinnibar.png"));
        assertEquals("textures/block/infusedore.png",
                newAssetFor(csvText, "textures/blocks/infusedore.png"));
        assertEquals("textures/block/infusedore.png.mcmeta",
                newAssetFor(csvText, "textures/blocks/infusedore.png.mcmeta"));
        assertEquals("textures/item/shard.png",
                newAssetFor(csvText, "textures/items/shard.png"));
        assertEquals("textures/item/shard_balanced.png.mcmeta",
                newAssetFor(csvText, "textures/items/shard_balanced.png.mcmeta"));

        // Directories that must NOT move (1.21.1 keeps them, or the path is literal).
        assertEquals("textures/aspects/ignis.png",
                newAssetFor(csvText, "textures/aspects/ignis.png"));
        assertEquals("textures/gui/gui_researchbook.png",
                newAssetFor(csvText, "textures/gui/gui_researchbook.png"));
        assertEquals("textures/misc/beam.png",
                newAssetFor(csvText, "textures/misc/beam.png"));
        assertEquals("textures/foci/alchemistsfire.png",
                newAssetFor(csvText, "textures/foci/alchemistsfire.png"));
        assertEquals("textures/models/alembic.obj",
                newAssetFor(csvText, "textures/models/alembic.obj"));
    }

    /**
     * The pipeline: the overlaid tree the jar is built from. Files land under
     * the modern directories and nowhere under the plural ones — a plural
     * leftover is a sprite the game cannot resolve.
     */
    @Test
    void overlaidTexturesLandOnTheModernPaths() throws IOException {
        Path textures = overlayTexturesDir();

        Map<String, Integer> filesPerDir = new TreeMap<>();
        List<String> pluralLeftovers = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(textures)) {
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path file : files) {
            String relative = textures.relativize(file).toString().replace('\\', '/');
            String dir = relative.split("/")[0];
            filesPerDir.merge(dir, 1, Integer::sum);
            if (LEGACY_DIR_RENAMES.containsKey(dir)) {
                pluralLeftovers.add(relative);
            }
        }

        assertTrue(pluralLeftovers.isEmpty(),
                "the overlay carried 1.7.10 plural texture paths, which 1.21.1 never resolves for "
                        + "a block/item sprite — those textures render magenta. First 5 of "
                        + pluralLeftovers.size() + ": "
                        + pluralLeftovers.subList(0, Math.min(5, pluralLeftovers.size())));

        for (String legacyDir : LEGACY_DIR_RENAMES.keySet()) {
            assertFalse(Files.exists(textures.resolve(legacyDir)),
                    "the overlay output still carries the 1.7.10 directory textures/" + legacyDir
                            + "/ — even empty, it ships as a jar entry under a path 1.21.1 never resolves");
        }

        for (Map.Entry<String, Integer> expected : FILES_PER_LEGACY_DIR.entrySet()) {
            String targetDir = LEGACY_DIR_RENAMES.getOrDefault(expected.getKey(), expected.getKey());
            assertEquals(expected.getValue(), filesPerDir.get(targetDir),
                    "overlaid file count under textures/" + targetDir + "/ (whole tree: "
                            + filesPerDir + ")");
        }

        // The sprites the player saw as magenta.
        for (String required : List.of(
                "block/amberore.png",
                "block/cinnibar.png",
                "block/infusedore.png",
                "block/infusedore.png.mcmeta",
                "block/infusedorestone.png",
                "item/shard.png",
                "item/shard_balanced.png.mcmeta")) {
            assertTrue(Files.isRegularFile(textures.resolve(required)),
                    "overlay is missing textures/" + required);
        }
    }

    /**
     * End to end for the client: every sprite the committed models ask for must
     * exist in the overlay at exactly the path 1.21.1 will look for. This is
     * the check the screenshot failed — the model resolved, the sprite did not.
     */
    @Test
    void everyModelSpriteResolvesToAnOverlayTexture() throws IOException {
        Path textures = overlayTexturesDir();
        Path assets = Path.of(System.getProperty("tc.modAssetsDir", ""));
        assertTrue(Files.isDirectory(assets),
                "tc.modAssetsDir not wired — run tests through ./gradlew test");
        Path models = assets.resolve("thaumcraft/models");
        assertTrue(Files.isDirectory(models), "committed models missing: " + models);

        int spriteRefs = 0;
        List<String> unresolved = new ArrayList<>();
        List<Path> modelFiles;
        try (Stream<Path> walk = Files.walk(models)) {
            modelFiles = walk.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        for (Path model : modelFiles) {
            JsonObject root = JsonParser.parseString(Files.readString(model)).getAsJsonObject();

            // Sprites: values of the "textures" object. A "#..." value is a
            // local reference inside the same model, not a location.
            JsonElement texturesElement = root.get("textures");
            if (texturesElement instanceof JsonObject textureMap) {
                for (Map.Entry<String, JsonElement> sprite : textureMap.entrySet()) {
                    String value = sprite.getValue().getAsString();
                    if (value.startsWith("#")) {
                        continue;
                    }
                    String namespace = value.contains(":")
                            ? value.substring(0, value.indexOf(':'))
                            : "minecraft";
                    if (!namespace.equals("thaumcraft")) {
                        continue; // vanilla sprites ship in the vanilla jar
                    }
                    spriteRefs++;
                    // `thaumcraft:block/amberore` -> <overlay>/thaumcraft/textures/block/amberore.png
                    Path spriteTexture = textures.resolve(
                            value.substring(value.indexOf(':') + 1) + ".png");
                    if (!Files.isRegularFile(spriteTexture)) {
                        unresolved.add(models.getParent().relativize(model) + " ["
                                + sprite.getKey() + " = " + value + "] -> " + spriteTexture);
                    }
                }
            }

            // Parents are models, not textures: they must ship in the repo.
            if (root.has("parent")) {
                String parent = root.get("parent").getAsString();
                if (parent.startsWith("thaumcraft:")) {
                    Path parentModel = assets.resolve("thaumcraft/models/"
                            + parent.substring(parent.indexOf(':') + 1) + ".json");
                    assertTrue(Files.isRegularFile(parentModel),
                            "model parent missing from the repo: " + parent + " (from " + model + ")");
                }
            }
        }

        assertTrue(spriteRefs >= 4,
                "expected the committed models to reference the ore sprites, found " + spriteRefs);
        assertTrue(unresolved.isEmpty(),
                spriteRefs + " thaumcraft sprite references, unresolved: " + unresolved);
    }

    /**
     * Committed <em>structured</em> references — the research graph's
     * {@code texture}/{@code icon}/{@code background} locations — name texture
     * files directly, so they name the published 1.21.1 path and must resolve
     * in the overlay. Verbatim display text is the deliberate exception:
     * research page text stays byte-identical to the original
     * {@code en_US.lang} (guarded by
     * {@code ResearchGraphDataTest.langStringsAreVerbatimFromTheOriginalFile}),
     * so legacy {@code <IMG>} paths inside that text stay as authored and the
     * renderer that consumes them applies this same rename.
     */
    @Test
    void structuredResearchTextureLocationsArePublishedPaths() throws IOException {
        Path research = Path.of(System.getProperty("tc.researchDataDir", ""));
        assertTrue(Files.isDirectory(research),
                "tc.researchDataDir not wired — run tests through ./gradlew test");
        Path textures = overlayTexturesDir();

        Pattern location = Pattern.compile(
                "\"(?:texture|icon|background)\": *\"thaumcraft:textures/([a-z]+)/([^\"]+)\"");
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String name : List.of("categories.json", "entries.json")) {
            Path file = research.resolve(name);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            Matcher matcher = location.matcher(Files.readString(file));
            while (matcher.find()) {
                checked++;
                String dir = matcher.group(1);
                String locationPath = dir + "/" + matcher.group(2);
                if (LEGACY_DIR_RENAMES.containsKey(dir)) {
                    problems.add(name + ": " + locationPath
                            + " — a structured location must name the 1.21.1 directory");
                } else if (!Files.isRegularFile(textures.resolve(dir).resolve(matcher.group(2)))) {
                    problems.add(name + ": " + locationPath + " — not published by the overlay");
                }
            }
        }

        assertTrue(checked >= 30,
                "expected the research graph to carry its icon locations, found " + checked);
        assertTrue(problems.isEmpty(),
                checked + " structured research texture locations, problems: " + problems);
    }

    private static Path overlayTexturesDir() {
        Path overlayDir = Path.of(System.getProperty("tc.originalAssetsOverlayDir", ""));
        Path textures = overlayDir.resolve("thaumcraft/textures");
        Assumptions.assumeTrue(Files.isDirectory(textures),
                "original-asset overlay not generated (no ../reference tree) — "
                        + "CI asserts the CSV mapping only");
        return textures;
    }

    private static String newAssetFor(String csvContent, String originalAsset) {
        for (String[] row : parseCsv(csvContent)) {
            if (row.length >= 2 && row[0].equals(originalAsset)) {
                return row[1];
            }
        }
        throw new AssertionError("asset-mapping.csv has no row for " + originalAsset);
    }

    /** Minimal CSV line parser: paths are unquoted, later columns may quote commas. */
    private static List<String[]> parseCsv(String content) {
        List<String[]> rows = new ArrayList<>();
        for (String line : content.split("\\r?\\n")) {
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
