package com.jonnyutah12.thaumcraftclassic.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-08-s-c3 (t_6160b768): the five golem item model JSONs and the
 * eldritch golem's spawn-egg model.
 *
 * <p>The unit test set has no Minecraft bootstrap, so this test reads the
 * committed tree the way {@code scripts/check_asset_paths.py} does — the files
 * themselves — and pins three things the renderers' card cannot see:</p>
 *
 * <ol>
 *   <li><b>Every model the client will look for exists.</b> 1.21.1 resolves an
 *       item's model at {@code models/item/<path>.json}, so the file name is
 *       the registered id's path — including the spawn egg's dotted
 *       {@code spawnegg.eldritchgolem.json}.</li>
 *   <li><b>Every sprite reference resolves on a code-only jar.</b> The
 *       reference is the SINGULAR {@code textures/item/} layout (the 1.7.10
 *       plural layout is the defect {@code OriginalTextureOverlayTest} measures
 *       and {@code docs/asset-mapping.csv} renames), and it is one of the
 *       inventory's {@code new_asset} rows, which is exactly the set gate 4 of
 *       the asset-path checker accepts as "absent is fine, a resource-pack
 *       overlay ships it". A reference the CSV does not declare is a hard miss
 *       on the code-only jar.</li>
 *   <li><b>The base sprite is the original's icon 0</b>, so the single model
 *       1.21.1 can show for a meta-indexed item is the sprite the original
 *       showed for meta 0 — the meta 0 the port's per-meta lang keys label
 *       ("Straw Golem", "Golem Core: Fill", "Golem Upgrade: Air", "Top Hat").
 *       A model that picked an arbitrary meta would render an Air upgrade for
 *       an Air golem and a Fire core for a Fill core.</li>
 * </ol>
 */
class GolemItemModelContractTest {

    private static final String NAMESPACE = "thaumcraft";

    /** A committed golem item model and the sprite the original gave meta 0. */
    private record ModelCase(String path, String parent, String layer0, String originalIcon0, String citation) {
    }

    /**
     * The five registered golem items ({@code CatalogGolems.ITEMS}). The
     * icon-0 column is the original's own {@code registerItemIcons} line, i.e.
     * the sprite the 1.7.10 item showed at meta 0 — quoted with its file:line.
     */
    private static final List<ModelCase> GOLEM_ITEM_MODELS = List.of(
            new ModelCase("itemgolemplacer", "minecraft:item/generated", "thaumcraft:item/golem_straw",
                    "golem_straw", "ItemGolemPlacer.java:35 iconGolem[0] (meta 0 = straw; "
                            + "lang \"item.thaumcraft.itemgolemplacer.0\" = \"Straw Golem\")"),
            new ModelCase("itemgolemcore", "minecraft:item/generated", "thaumcraft:item/golem_core_fill",
                    "golem_core_fill", "ItemGolemCore.java:29 icon[0] (meta 0 = fill; "
                            + "lang \"item.thaumcraft.itemgolemcore.0\" = \"Golem Core: Fill\")"),
            new ModelCase("itemgolemupgrade", "minecraft:item/generated", "thaumcraft:item/golem_upgrade_air",
                    "golem_upgrade_air", "ItemGolemUpgrade.java:30 icon[0] (meta 0 = Air; "
                            + "lang \"item.thaumcraft.itemgolemupgrade.0\" = \"Golem Upgrade: Air\")"),
            new ModelCase("golembell", "minecraft:item/generated", "thaumcraft:item/ironbell",
                    "ironbell", "ItemGolemBell.java:40 icon (the bell has ONE icon, no meta array)"),
            new ModelCase("itemgolemdecoration", "minecraft:item/generated", "thaumcraft:item/golemdecotophat",
                    "golemdecotophat", "ItemGolemDecoration.java:26 icon[0] (meta 0 = Top Hat; "
                            + "lang \"item.thaumcraft.itemgolemdecoration.0\" = \"Top Hat\")"));

    /** The per-meta lang sets the models' items carry (TC-M6-08-s). */
    private record MetaSet(String path, int from, int to, int extraMeta) {
    }

    private static final List<MetaSet> META_SETS = List.of(
            new MetaSet("itemgolemplacer", 0, 7, -1),      // MATERIALS, ItemGolemPlacer.java:21-30
            new MetaSet("itemgolemcore", 0, 11, 100),      // VALID_METAS, ItemGolemCore.java:27-41
            new MetaSet("itemgolemupgrade", 0, 5, -1),     // PRIMAL_ORDER, ItemGolemUpgrade.java:21-28
            new MetaSet("itemgolemdecoration", 0, 7, -1)); // NAMES, ItemGolemDecoration.java:20-29

    private static Path modelRoot() {
        final String dir = System.getProperty("tc.modAssetsDir", "src/main/resources/assets");
        return Path.of(dir).resolve(NAMESPACE).resolve("models").resolve("item");
    }

    private static Path assetMappingFile() {
        final String path = System.getProperty("tc.assetMappingFile", "");
        return Path.of(path.isEmpty() ? "docs/asset-mapping.csv" : path);
    }

    /** The {@code new_asset} targets the ledger declares (gate 4's accept set). */
    private static Set<String> declaredAssets() throws IOException {
        final Path file = assetMappingFile();
        assertTrue(Files.isRegularFile(file), file
                + " is missing - the path is the tc.assetMappingFile system property, wired by the test "
                + "task in build.gradle");
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        final String[] columns = lines.get(0).split(",", -1);
        final int newAsset = List.of(columns).indexOf("new_asset");
        assertTrue(newAsset >= 0, "asset-mapping has no new_asset column: " + List.of(columns));
        final Set<String> declared = new java.util.HashSet<>();
        lines.stream()
                .skip(1)
                .filter(l -> !l.isBlank() && !l.startsWith("#"))
                .map(l -> l.split(",", -1))
                .filter(c -> c.length > newAsset)
                .map(c -> c[newAsset].trim())
                .filter(v -> !v.isEmpty())
                .forEach(declared::add);
        assertFalse(declared.isEmpty(), "the ledger declares no new_asset targets in " + file);
        return declared;
    }

    private static JsonObject model(final String path) {
        final Path file = modelRoot().resolve(path + ".json");
        assertTrue(Files.isRegularFile(file), "the client looks for models/item/" + path
                + ".json for the registered id thaumcraft:" + path + "; " + file + " is not shipped");
        try {
            final var parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            assertTrue(parsed.isJsonObject(), file + " does not parse to a JSON object");
            return parsed.getAsJsonObject();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static JsonObject lang() {
        final Path file = Path.of(System.getProperty("tc.modAssetsDir", "src/main/resources/assets"))
                .resolve(NAMESPACE).resolve("lang").resolve("en_us.json");
        assertTrue(Files.isRegularFile(file), file + " is missing");
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Test
    void everyGolemItemModelIsShippedUnderItsRegisteredId() {
        for (final ModelCase modelCase : GOLEM_ITEM_MODELS) {
            final JsonObject model = model(modelCase.path());
            assertEquals(modelCase.parent(), model.get("parent").getAsString(),
                    modelCase.path() + " must parent the vanilla generated-item model; the parent supplies "
                            + "the 1.21.1 built-in item transforms the original got from its own renderer");
            assertEquals(modelCase.layer0(), model.getAsJsonObject("textures").get("layer0").getAsString(),
                    modelCase.path() + " sprite: the original's icon 0 is " + modelCase.originalIcon0()
                            + " (" + modelCase.citation() + ")");
        }
    }

    @Test
    void everySpriteReferenceIsSingularLayoutAndDeclaredByTheLedger() throws IOException {
        final Set<String> declared = declaredAssets();
        final List<String> misses = new ArrayList<>();
        for (final ModelCase modelCase : GOLEM_ITEM_MODELS) {
            final String ref = model(modelCase.path()).getAsJsonObject("textures").get("layer0").getAsString();
            final String target = "textures/" + ref.substring(NAMESPACE.length() + 1) + ".png";
            if (!ref.startsWith(NAMESPACE + ":item/")) {
                misses.add(modelCase.path() + " -> " + ref + ": 1.21.1 resolves item sprites at the SINGULAR "
                        + "textures/item/ layout; the 1.7.10 plural layout is the rename docs/asset-mapping.csv "
                        + "records (check_asset_paths gate 3)");
            }
            if (!declared.contains(target)) {
                misses.add(modelCase.path() + " -> " + ref + ": " + target + " is not a new_asset row of "
                        + "docs/asset-mapping.csv, so check_asset_paths gate 4 fails on the code-only jar");
            }
        }
        assertEquals(List.of(), misses, "unresolvable sprite references in the shipped golem item models");
    }

    @Test
    void theEldritchSpawnEggModelUsesTheVanillaTwoLayerTemplate() {
        final JsonObject model = model("spawnegg.eldritchgolem");
        assertEquals("minecraft:item/template_spawn_egg", model.get("parent").getAsString(),
                "the egg must parent the vanilla egg template, the shape every vanilla egg model has "
                        + "(measured: models/item/zombie_spawn_egg.json in the 1.21.1 client jar) - it is "
                        + "what applies SpawnEggItem#getColor to the shell and overlay layers");
        assertFalse(model.has("textures"),
                "the egg inherits both tinted layers from the template; overriding layer0/layer1 with "
                        + "thaumcraft sprites would drop the overlay the original's two-colour ItemSpawnerEgg drew");
    }

    @Test
    void everyModelledItemIsNamedByTheShippedLangFile() {
        final JsonObject lang = lang();
        final List<String> missing = new ArrayList<>();
        for (final ModelCase modelCase : GOLEM_ITEM_MODELS) {
            assertTrue(lang.has("item." + NAMESPACE + "." + modelCase.path()),
                    "item." + NAMESPACE + "." + modelCase.path() + " is missing from en_us.json, so "
                            + modelCase.path() + "'s model renders an untranslated item name");
            for (final MetaSet metaSet : META_SETS) {
                if (!metaSet.path().equals(modelCase.path())) {
                    continue;
                }
                for (int meta = metaSet.from(); meta <= metaSet.to(); meta++) {
                    if (!lang.has("item." + NAMESPACE + "." + modelCase.path() + "." + meta)) {
                        missing.add(modelCase.path() + "." + meta);
                    }
                }
                if (metaSet.extraMeta() >= 0
                        && !lang.has("item." + NAMESPACE + "." + modelCase.path() + "." + metaSet.extraMeta())) {
                    missing.add(modelCase.path() + "." + metaSet.extraMeta());
                }
            }
        }
        assertTrue(lang.has("item." + NAMESPACE + ".spawnegg.eldritchgolem"),
                "the spawn egg's lang key is the ledger's dotted id verbatim - the id contract "
                        + "LocalizationFileTest.everyRegistryKeyNamesInventoriedContent resolves by prefix");
        assertEquals(List.of(), missing, "modelled items are missing per-meta lang keys");
    }
}
