package com.jonnyutah12.thaumcraftclassic.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import org.junit.jupiter.api.Assumptions;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-04-s-iv: the KnowFrag recipe row
 * ({@code data/thaumcraft/recipe/recipe.256.crafting_shaped.json}) — the
 * research note's only survival acquisition path.
 *
 * <p>Source of truth: {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * config/ConfigRecipes.java:3860-3865}, the one and only
 * {@code itemResearchNotes} the original ever registers a recipe for:</p>
 *
 * <pre>
 * ConfigResearch.recipes.put("KnowFrag", GameRegistry.addShapedRecipe(
 *     new ItemStack(ConfigItems.itemResearchNotes, 1, 42),
 *     new Object[]{"KKK", "KKK", "KKK", 'K', new ItemStack(ConfigItems.itemResource, 1, 9)}));
 * </pre>
 *
 * <p>The in-game behaviour (the grid matching, the crafted blank's meta and its
 * EMPTY note state, the ingredient's meta precision) is proven on the game-test
 * server by {@code KnowledgeFragmentRecipeGameTest}. This class pins the DATA:
 * the shipped row is the original's row, it is a form the 1.21.1
 * {@code RecipeManager} can decode, and it stays gated so it can never take the
 * {@code runServer} smoke job down.</p>
 */
class KnowFragRecipeDataTest {

    private static final String ROW_FILE = "recipe.256.crafting_shaped.json";
    private static final String ORIGINAL_RECIPES =
            "decompiled/thaumcraft/thaumcraft/common/config/ConfigRecipes.java";
    private static final String SHAPE = "minecraft:crafting_shaped";

    /** The original's KnowFrag registration: output item/count/meta + grid + key. */
    private static final Pattern KNOW_FRAG = Pattern.compile(
            "\"KnowFrag\",\\s*GameRegistry\\.addShapedRecipe\\(\\s*"
                    + "new ItemStack\\(ConfigItems\\.(\\w+), (\\d+), (\\d+)\\),\\s*"
                    + "new Object\\[\\]\\{([^}]*)\\}", Pattern.DOTALL);

    /** The grid rows inside the original's {@code new Object[]{...}} array. */
    private static final Pattern GRID_ROW = Pattern.compile("\"([K ]+)\"");

    /** The original's key binding: {@code 'K', new ItemStack(ConfigItems.<item>, count, meta)}. */
    private static final Pattern KEY_ITEM = Pattern.compile(
            "'K',\\s*new ItemStack\\(ConfigItems\\.(\\w+), (\\d+), (\\d+)\\)");

    /** Every id a shipped row names, in any ingredient/output position. */
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    /** {@code thaumcraft:item_meta}'s persistent codec bound ({@code Codec.intRange(0, 63)}). */
    private static final int META_CARRIER_MAX = 63;

    // ------------------------------------------------------------------ shape

    @Test
    void theRowIsAWellFormedModernCraftingRow() throws IOException {
        final JsonObject json = row();

        assertEquals(SHAPE, json.get("type").getAsString(),
                "the original's row is GameRegistry.addShapedRecipe — a crafting table shaped "
                        + "recipe, not an arcane/infusion row");

        final JsonArray pattern = json.getAsJsonArray("pattern");
        assertEquals(3, pattern.size(), "the original's grid is 3 rows");
        for (final JsonElement row : pattern) {
            assertEquals("KKK", row.getAsString(), "the original's grid is a full 3x3 of K");
        }

        final JsonObject key = json.getAsJsonObject("key");
        assertEquals(Set.of("K"), key.asMap().keySet(), "the original binds one key, 'K'");
        final JsonObject fragment = key.getAsJsonObject("K");
        // D-015: the meta-carrying ingredient must be the REGISTERED
        // neoforge:components Ingredient, and the "type" key is mandatory —
        // without it the Ingredient codec fails and the whole row is dropped.
        assertEquals("neoforge:components", fragment.get("type").getAsString(),
                "the fragment ingredient is not the registered neoforge:components form");
        assertTrue(fragment.getAsJsonArray("items").isJsonArray(),
                "neoforge:components wants items as a list");
        assertEquals(1, fragment.getAsJsonArray("items").size());
        assertEquals("thaumcraft:itemresource",
                fragment.getAsJsonArray("items").get(0).getAsString(),
                "the key item is itemResource in the port's lowercase id");
        assertEquals(ItemResource.META_KNOWLEDGE_FRAGMENT,
                fragment.getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt(),
                "the key carries the knowledge fragment meta");

        final JsonObject result = json.getAsJsonObject("result");
        assertEquals("thaumcraft:itemresearchnotes", result.get("id").getAsString(),
                "the output is the note (ItemResearchNotes in the original)");
        assertEquals(1, result.get("count").getAsInt(), "the original crafts one note");
        assertEquals(ItemResearchNotes.META_BLANK_DISCOVERY_B,
                result.getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt(),
                "the output carries meta 42 — the blank discovery note the original crafts");

        final Set<String> gated = gatedItems(json);
        assertEquals(Set.of("thaumcraft:itemresource", "thaumcraft:itemresearchnotes"), gated,
                "every item the row names must carry a neoforge:item_exists gate, and only those");
    }

    @Test
    void theRowCarriesThePortsOwnMetaConstantsAndStaysInsideTheCarrier() {
        // The row hard-codes the metas the Java side names; drift between the
        // two is silent (a recipe can point at an unregistered variant), so pin
        // them to the constants the item classes expose.
        final JsonObject json = row();
        final int keyMeta = json.getAsJsonObject("key").getAsJsonObject("K")
                .getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt();
        final int outMeta = json.getAsJsonObject("result")
                .getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt();

        assertEquals(ItemResource.META_KNOWLEDGE_FRAGMENT, keyMeta,
                "the ingredient meta drifted from ItemResource.META_KNOWLEDGE_FRAGMENT");
        assertEquals(ItemResearchNotes.META_BLANK_DISCOVERY_B, outMeta,
                "the output meta drifted from ItemResearchNotes.META_BLANK_DISCOVERY_B");
        assertTrue(ItemResearchNotes.isBlankDiscoveryMeta(outMeta),
                "the crafted meta must be one of the original's blank discovery metas");
        assertTrue(ItemResearchNotes.META_BLANK_DISCOVERY_A != outMeta,
                "the original crafts meta 42 only — meta 24 (the other blank) has no recipe, "
                        + "which is why the note's acquisition path is this row");
        assertTrue(keyMeta >= 0 && keyMeta <= META_CARRIER_MAX,
                "thaumcraft:item_meta is bounded at " + META_CARRIER_MAX + " (Codec.intRange), "
                        + "so an out-of-range ingredient meta is a silent decode failure");
        assertTrue(outMeta >= 0 && outMeta <= META_CARRIER_MAX,
                "the crafted note's meta must fit the thaumcraft:item_meta carrier, which is why "
                        + "the discovery family (meta 64+) has no crafting path in the port");
    }

    // --------------------------------------------------------------- fidelity

    @Test
    void theShippedRowIsTheOriginalKnowFragRegistration() throws IOException {
        final String original = readReference(ORIGINAL_RECIPES);
        final Matcher knowFrag = KNOW_FRAG.matcher(original);
        assertTrue(knowFrag.find(), "the original's KnowFrag registration disappeared from "
                + ORIGINAL_RECIPES + " — the row this card ships has no source to copy");

        final String outputItem = knowFrag.group(1);
        final int outputCount = Integer.parseInt(knowFrag.group(2));
        final int outputMeta = Integer.parseInt(knowFrag.group(3));
        assertEquals("itemResearchNotes", outputItem, "the original's KnowFrag output item");
        assertEquals(1, outputCount, "the original's KnowFrag output count");
        assertEquals(ItemResearchNotes.META_BLANK_DISCOVERY_B, outputMeta,
                "the original's KnowFrag output meta is the blank discovery note");

        final String grid = knowFrag.group(4);
        final List<String> rows = new ArrayList<>();
        final Matcher gridRow = GRID_ROW.matcher(grid);
        while (gridRow.find()) {
            rows.add(gridRow.group(1));
        }
        assertEquals(List.of("KKK", "KKK", "KKK"), rows,
                "the original's KnowFrag grid is a full 3x3 of the K key");

        final Matcher key = KEY_ITEM.matcher(grid);
        assertTrue(key.find(), "the original's KnowFrag key binding disappeared");
        assertEquals("itemResource", key.group(1), "the original's KnowFrag key item");
        assertEquals(1, Integer.parseInt(key.group(2)), "one fragment per grid slot");
        assertEquals(ItemResource.META_KNOWLEDGE_FRAGMENT, Integer.parseInt(key.group(3)),
                "the original's KnowFrag key is the knowledge fragment meta");

        // And the shipped row agrees with the original on all four numbers.
        final JsonObject json = row();
        assertEquals(outputMeta, json.getAsJsonObject("result")
                        .getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt(),
                "the shipped row's output meta is not the original's");
        assertEquals(Integer.parseInt(key.group(3)), json.getAsJsonObject("key")
                        .getAsJsonObject("K").getAsJsonObject("components")
                        .get("thaumcraft:item_meta").getAsInt(),
                "the shipped row's ingredient meta is not the original's");
        assertEquals(rows.size(), json.getAsJsonArray("pattern").size(),
                "the shipped row's grid height is not the original's");
        assertEquals(rows.get(0), json.getAsJsonArray("pattern").get(0).getAsString(),
                "the shipped row's grid row is not the original's");
    }

    // ------------------------------------------------------- the smoke-gate family

    @Test
    void everyVanillaCraftingRowIsGatedAndDecodeable() throws IOException {
        // The CI runServer smoke job fails on any boot-log line pairing an
        // (ERROR|FATAL) marker or an Exception with 'thaumcraft'. A
        // minecraft:crafting_* row that cannot decode is dropped with
        // 'Parsing error loading recipe thaumcraft:...' — an ERROR naming
        // thaumcraft — so this whole family is what the gate reads, not just
        // this card's row. Pin the three rules the family lives by.
        final List<Path> rows = craftingRows();
        assertTrue(rows.size() >= 5, "only " + rows.size()
                + " vanilla crafting rows found — the family looks truncated");
        for (final Path file : rows) {
            final JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            final String type = json.get("type").getAsString();
            assertTrue(SHAPE.equals(type) || "minecraft:crafting_shapeless".equals(type),
                    file.getFileName() + ": a .crafting_ row must name a vanilla crafting type, "
                            + "found " + type);

            final List<JsonObject> ingredients = new ArrayList<>();
            if (SHAPE.equals(type)) {
                for (final JsonElement key : json.getAsJsonObject("key").asMap().values()) {
                    ingredients.add(key.getAsJsonObject());
                }
            } else {
                for (final JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                    ingredients.add(ingredient.getAsJsonObject());
                }
            }
            final List<String> ingredientIds = new ArrayList<>();
            boolean metaCarrying = false;
            for (final JsonObject ingredient : ingredients) {
                collectIds(ingredient, ingredientIds);
                if (ingredient.has("components") || ingredient.has("neoforge:components")) {
                    metaCarrying = true;
                    assertEquals("neoforge:components", ingredient.get("type").getAsString(),
                            file.getFileName() + ": a meta-carrying ingredient needs the "
                                    + "registered Ingredient type key (D-015)");
                    assertTrue(ingredient.getAsJsonArray("items").size() > 0,
                            file.getFileName() + ": empty items list");
                } else {
                    assertTrue(ingredient.has("item") || ingredient.has("tag"),
                            file.getFileName() + ": ingredient is neither item, tag nor typed");
                }
            }
            final List<String> resultIds = new ArrayList<>();
            collectIds(json.get("result"), resultIds);

            final Set<String> gated = gatedItems(json);
            for (final String id : ingredientIds) {
                assertTrue(VALID_ID.matcher(id).matches(),
                        file.getFileName() + ": '" + id + "' is not a valid resource location");
                assertTrue(gated.contains(id), file.getFileName() + ": item " + id
                        + " has no neoforge:item_exists gate — an unported id fails the row");
            }
            for (final String id : resultIds) {
                assertTrue(VALID_ID.matcher(id).matches(),
                        file.getFileName() + ": result '" + id
                                + "' is not a valid resource location");
            }
            // The rows the vanilla extractor emitted (264/266/274/275) gate
            // their input only — their itemresource result happens to be
            // registered, so they load. A meta-carrying row (this card's) gates
            // its output too: the 1.21.1 CraftingOutput codec resolves the item
            // at parse time, so an unregistered result id drops the row with a
            // 'Parsing error loading recipe' ERROR naming thaumcraft.
            if (metaCarrying) {
                for (final String id : resultIds) {
                    assertTrue(gated.contains(id), file.getFileName()
                            + ": a meta-carrying row must gate its result item as well");
                }
            }
        }
    }

    @Test
    void theContentMappingRowIsFlippedWithThisCard() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        Assumptions.assumeTrue(Files.isRegularFile(csv), "content mapping csv not wired into the test task");

        String row = null;
        for (final String line : Files.readAllLines(csv)) {
            if (line.startsWith("Thaumcraft:recipe.256.crafting_shaped,")) {
                row = line;
            }
        }
        assertNotNull(row, "the recipe.256.crafting_shaped row disappeared from content-mapping.csv");
        final List<String> fields = splitCsv(row);
        assertEquals(8, fields.size(), "the row must keep the 8-field contract, got " + fields.size()
                + ": " + row);
        assertEquals("thaumcraft:recipe.256.crafting_shaped", fields.get(1), "content id");
        assertEquals("recipe", fields.get(2), "kind");
        assertEquals("ItemResearchNotes, 1, 42", fields.get(3),
                "the display column is the original's output — the row this card ships");
        assertEquals("done", fields.get(4), "implementation status");
        assertEquals("tested", fields.get(5),
                "test status: KnowledgeFragmentRecipeGameTest + this class prove the row");
        assertFalse(fields.get(6).contains(","), "the note column must not carry a comma "
                + "or the row parses to 9 fields and ContentMappingCsv throws");
        assertTrue(fields.get(6).contains("256") || fields.get(6).contains("KnowFrag"),
                "the note names this card's row: " + fields.get(6));
        assertTrue(fields.get(7).startsWith("type=crafting_shaped"),
                "the extra column keeps the extractor's type tag: " + fields.get(7));
    }

    // ------------------------------------------------------------------ plumbing

    private static JsonObject row() {
        final Path file = recipeDir().resolve(ROW_FILE);
        assertTrue(Files.isRegularFile(file), "the KnowFrag row is missing: " + file);
        try (var reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static List<Path> craftingRows() throws IOException {
        try (Stream<Path> files = Files.list(recipeDir())) {
            final List<Path> out = new ArrayList<>();
            files.filter(p -> p.getFileName().toString().contains(".crafting_"))
                    .sorted()
                    .forEach(out::add);
            return out;
        }
    }

    private static Path recipeDir() {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path).resolve("thaumcraft").resolve("recipe");
        assertTrue(Files.isDirectory(dir), "recipe data dir not found at " + dir);
        return dir;
    }

    /** The items a row's neoforge:item_exists conditions name. */
    private static Set<String> gatedItems(final JsonObject json) {
        final Set<String> gated = new HashSet<>();
        final JsonElement conditions = json.get("neoforge:conditions");
        if (conditions != null && conditions.isJsonArray()) {
            for (final JsonElement condition : conditions.getAsJsonArray()) {
                final JsonObject entry = condition.getAsJsonObject();
                if ("neoforge:item_exists".equals(entry.get("type").getAsString())) {
                    gated.add(entry.get("item").getAsString());
                }
            }
        }
        return gated;
    }

    private static void collectIds(final JsonElement node, final List<String> ids) {
        if (node == null || node.isJsonNull()) {
            return;
        }
        if (node.isJsonArray()) {
            for (final JsonElement element : node.getAsJsonArray()) {
                collectIds(element, ids);
            }
            return;
        }
        if (!node.isJsonObject()) {
            return;
        }
        final JsonObject object = node.getAsJsonObject();
        for (final String key : List.of("item", "id")) {
            if (object.has(key)) {
                ids.add(object.get(key).getAsString());
            }
        }
        if (object.has("items")) {
            final JsonElement value = object.get("items");
            if (value.isJsonArray()) {
                for (final JsonElement element : value.getAsJsonArray()) {
                    ids.add(element.getAsString());
                }
            } else {
                ids.add(value.getAsString());
            }
        }
        for (final String key : List.of("key", "ingredients", "result", "components")) {
            collectIds(object.get(key), ids);
        }
    }

    /** Quote-aware CSV split, so a quoted display column with commas stays one field. */
    private static List<String> splitCsv(final String line) {
        final List<String> fields = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * The decompiled 1.7.10 tree, walking up from the test cwd: {@code ../reference}
     * is one level above the project dir, and a {@code .worktrees/<task>} checkout
     * sits one level deeper, so the bare idiom silently skips there. The tree is a
     * local decompile and is NOT in the repo, so the test skips when it is absent.
     */
    private static String readReference(final String relative) {
        Path base = Path.of(".").toAbsolutePath().normalize();
        for (int up = 0; up <= 4; up++) {
            final Path candidate = base.resolve("reference").resolve(relative);
            if (Files.isRegularFile(candidate)) {
                try {
                    return Files.readString(candidate);
                } catch (IOException failure) {
                    throw new java.io.UncheckedIOException(failure);
                }
            }
            base = base.getParent();
            if (base == null) {
                break;
            }
        }
        Assumptions.assumeTrue(false, "decompiled original not checked out: reference/" + relative);
        return null;
    }
}
