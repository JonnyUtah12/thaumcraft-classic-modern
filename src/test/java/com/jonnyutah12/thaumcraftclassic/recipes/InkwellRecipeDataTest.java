package com.jonnyutah12.thaumcraftclassic.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.items.ItemPhial;
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
 * TC-M3-04-s-vi: the Scribing Tools acquisition rows
 * ({@code recipe.scribe1.crafting_shapeless.json} +
 * {@code recipe.scribe2.crafting_shapeless.json}) — the inkwell's survival
 * path, without which the Thaumonomicon's note arm (t_23829136) is
 * creative/give-only (BD-19).
 *
 * <p>Source of truth: {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * config/ConfigRecipes.java:3971-3982} — the three shapeless rows the original
 * registers for {@code itemInkwell}:</p>
 *
 * <pre>
 * "Scribe1": shapelessOreDictRecipe(new ItemStack(ConfigItems.itemInkwell),
 *              {new ItemStack(ConfigItems.itemEssence, 1, 0), Items.field_151008_G, "dyeBlack"})
 * "Scribe2": shapelessOreDictRecipe(new ItemStack(ConfigItems.itemInkwell),
 *              {Items.field_151069_bo, Items.field_151008_G, "dyeBlack"})
 * "Scribe3": shapelessOreDictRecipe(new ItemStack(ConfigItems.itemInkwell),
 *              {new ItemStack(ConfigItems.itemInkwell, 1, 32767), "dyeBlack"})
 * </pre>
 *
 * <p>Every MCP name the rows carry is resolved through
 * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv} (feather
 * :1610, glass_bottle :1671) — the tests compare the shipped ingredient ids
 * with the MCP-resolved modern names, not with a remembered table. The
 * in-game craft (grid matching, the crafted inkwell's durability, the
 * ingredient precision, the dyeBlack tag) is proven on the game-test server
 * by {@code InkwellRecipeGameTest}. This class pins the DATA.</p>
 */
class InkwellRecipeDataTest {

    private static final String ROW1 = "recipe.scribe1.crafting_shapeless.json";
    private static final String ROW2 = "recipe.scribe2.crafting_shapeless.json";
    private static final String ORIGINAL_RECIPES =
            "decompiled/thaumcraft/thaumcraft/common/config/ConfigRecipes.java";
    private static final String MCP_FIELDS =
            "mcp-mappings/fields_1.7.10_mcp_stable_11.csv";
    private static final String SHAPELESS = "minecraft:crafting_shapeless";
    private static final String SHAPE = "minecraft:crafting_shaped";

    /** The original's Scribe registration: key, output, ingredient array. */
    private static final Pattern SCRIBE = Pattern.compile(
            "\"(Scribe[123])\",\\s*shapelessOreDictRecipe\\(\\s*"
                    + "new ItemStack\\(ConfigItems\\.(\\w+)(?:,\\s*(\\d+),\\s*(\\d+))?\\),\\s*"
                    + "new Object\\[\\]\\{([^}]*)\\}", Pattern.DOTALL);

    /** Every id a shipped row names, in any ingredient/output position. */
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    // ------------------------------------------------------------------ shape

    @Test
    void theScribeRowsAreWellFormedModernCraftingRows() {
        final JsonObject scribe1 = row(ROW1);
        final JsonObject scribe2 = row(ROW2);

        for (final JsonObject json : List.of(scribe1, scribe2)) {
            assertEquals(SHAPELESS, json.get("type").getAsString(),
                    "the original's rows are shapelessOreDictRecipe — crafting-table shapeless, "
                            + "not arcane/infusion rows");
            final JsonObject result = json.getAsJsonObject("result");
            assertEquals("thaumcraft:iteminkwell", result.get("id").getAsString(),
                    "the output is Scribing Tools (the original's itemInkwell, D-001 id case)");
            assertEquals(1, result.get("count").getAsInt(),
                    "the original's output is new ItemStack(ConfigItems.itemInkwell) — one");
            assertFalse(result.has("components"),
                    "the inkwell has no subtypes (ItemInkwell.java:23-29 setHasSubtypes(false)) "
                            + "so the output carries no thaumcraft:item_meta");
        }

        // Scribe1: empty phial (meta 0 on the component carrier, D-015) + feather + dyeBlack.
        final JsonArray in1 = scribe1.getAsJsonArray("ingredients");
        assertEquals(3, in1.size(), "the original's Scribe1 has three ingredients");
        final JsonObject phial = in1.get(0).getAsJsonObject();
        assertEquals("neoforge:components", phial.get("type").getAsString(),
                "the meta-carrying phial ingredient must be the REGISTERED "
                        + "neoforge:components Ingredient — the \"type\" key is mandatory (D-015)");
        assertEquals(List.of("thaumcraft:itemessence"),
                phial.getAsJsonArray("items").asList().stream()
                        .map(JsonElement::getAsString).toList(),
                "the original's ConfigItems.itemEssence is the port's thaumcraft:itemessence");
        assertEquals(ItemPhial.META_EMPTY,
                phial.getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt(),
                "the ingredient pins the EMPTY phial (the original's meta 0) — a filled phial "
                        + "must never be eaten");
        assertEquals("minecraft:feather", in1.get(1).getAsJsonObject().get("item").getAsString(),
                "Items.field_151008_G is feather (MCP fields :1610)");
        assertEquals("thaumcraft:oredict.dyeblack", in1.get(2).getAsJsonObject().get("tag").getAsString(),
                "the ore-dict name dyeBlack resolves through the port's oredict tag family");

        // Scribe2: glass bottle (MCP field_151069_bo, fields :1671) + feather + dyeBlack.
        final JsonArray in2 = scribe2.getAsJsonArray("ingredients");
        assertEquals(3, in2.size(), "the original's Scribe2 has three ingredients");
        assertEquals("minecraft:glass_bottle", in2.get(0).getAsJsonObject().get("item").getAsString(),
                "Items.field_151069_bo is glass_bottle (MCP fields :1671) — not bowl, not a "
                        + "phial: the original's second row stands the bottle in for the phial");
        assertEquals("minecraft:feather", in2.get(1).getAsJsonObject().get("item").getAsString(),
                "Items.field_151008_G is feather (MCP fields :1610)");
        assertEquals("thaumcraft:oredict.dyeblack", in2.get(2).getAsJsonObject().get("tag").getAsString(),
                "the ore-dict name dyeBlack resolves through the port's oredict tag family");

        for (final JsonObject json : List.of(scribe1, scribe2)) {
            final Set<String> gated = gatedItems(json);
            final List<String> ids = new ArrayList<>();
            for (final JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                collectIds(ingredient, ids);
            }
            collectIds(json.get("result"), ids);
            for (final String id : ids) {
                assertTrue(VALID_ID.matcher(id).matches(), "'" + id + "' is not a valid resource location");
                assertTrue(gated.contains(id), "item " + id
                        + " has no neoforge:item_exists gate — an unported id fails the row");
            }
        }
    }

    // --------------------------------------------------------------- fidelity

    @Test
    void theShippedRowsAreTheOriginalScribeRegistrations() throws IOException {
        final String original = readReference(ORIGINAL_RECIPES);
        final Matcher scribes = SCRIBE.matcher(original);
        final List<String> keys = new ArrayList<>();
        final List<String> ingredients = new ArrayList<>();
        final List<String> outputs = new ArrayList<>();
        while (scribes.find()) {
            keys.add(scribes.group(1));
            outputs.add(scribes.group(2));
            ingredients.add(scribes.group(5));
        }
        assertEquals(List.of("Scribe1", "Scribe2", "Scribe3"), keys,
                "the original registers exactly three shapeless itemInkwell rows at "
                        + ORIGINAL_RECIPES + ":3971-3982");
        for (final String output : outputs) {
            assertEquals("itemInkwell", output, "the original's Scribe outputs are all itemInkwell");
        }

        // MCP-resolve the field names the rows carry, from the shipped CSV.
        final String mcp = readReference(MCP_FIELDS);
        assertEquals("feather", mcpName(mcp, "field_151008_G"),
                "Items.field_151008_G must resolve through the MCP fields CSV");
        assertEquals("glass_bottle", mcpName(mcp, "field_151069_bo"),
                "Items.field_151069_bo must resolve through the MCP fields CSV");

        // Scribe1: itemEssence meta 0 + feather + dyeBlack.
        assertTrue(ingredients.get(0).contains("new ItemStack(ConfigItems.itemEssence, 1, 0)"),
                "the original's Scribe1 starts with the EMPTY phial: " + ingredients.get(0));
        assertTrue(ingredients.get(0).contains("Items.field_151008_G"),
                "the original's Scribe1 carries the feather field: " + ingredients.get(0));
        assertTrue(ingredients.get(0).contains("\"dyeBlack\""),
                "the original's Scribe1 dye is the dyeBlack ore dict: " + ingredients.get(0));

        // Scribe2: glass_bottle field + feather + dyeBlack.
        assertTrue(ingredients.get(1).contains("Items.field_151069_bo"),
                "the original's Scribe2 starts with the glass_bottle field: " + ingredients.get(1));
        assertTrue(ingredients.get(1).contains("Items.field_151008_G"),
                "the original's Scribe2 carries the feather field: " + ingredients.get(1));
        assertTrue(ingredients.get(1).contains("\"dyeBlack\""),
                "the original's Scribe2 dye is the dyeBlack ore dict: " + ingredients.get(1));

        // The shipped rows agree with the MCP-resolved original.
        final JsonArray in1 = row(ROW1).getAsJsonArray("ingredients");
        assertEquals(ItemPhial.META_EMPTY,
                in1.get(0).getAsJsonObject().getAsJsonObject("components")
                        .get("thaumcraft:item_meta").getAsInt(),
                "the shipped Scribe1 phial meta is not the original's 0");
        assertEquals("minecraft:" + mcpName(mcp, "field_151008_G"),
                in1.get(1).getAsJsonObject().get("item").getAsString(),
                "the shipped feather id is not the MCP-resolved modern id");
        final JsonArray in2 = row(ROW2).getAsJsonArray("ingredients");
        assertEquals("minecraft:" + mcpName(mcp, "field_151069_bo"),
                in2.get(0).getAsJsonObject().get("item").getAsString(),
                "the shipped bottle id is not the MCP-resolved modern id");
        assertEquals("minecraft:" + mcpName(mcp, "field_151008_G"),
                in2.get(1).getAsJsonObject().get("item").getAsString(),
                "the shipped feather id is not the MCP-resolved modern id");
    }

    @Test
    void theInkwellHasNoRecolorRowUntilTheMetaFamilyLands() throws IOException {
        // Scribe3 is the 16-colour recolor row: its ingredient is the inkwell
        // ITSELF with the wildcard meta 32767. The port's ItemInkwell declares
        // no subtypes (BD-19), so there is no colour family to recolor — no
        // shipped row may name the inkwell as an INGREDIENT.
        final String original = readReference(ORIGINAL_RECIPES);
        final Matcher scribes = SCRIBE.matcher(original);
        String scribe3 = null;
        while (scribes.find()) {
            if ("Scribe3".equals(scribes.group(1))) {
                scribe3 = scribes.group(5);
            }
        }
        assertNotNull(scribe3, "the original's Scribe3 recolor row disappeared");
        assertTrue(scribe3.contains("new ItemStack(ConfigItems.itemInkwell, 1, 32767)"),
                "the original's Scribe3 is the inkwell wildcard recolor: " + scribe3);

        for (final Path file : craftingRows()) {
            final JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            final List<String> ingredientIds = new ArrayList<>();
            for (final JsonObject ingredient
                    : ingredientsOf(json, json.get("type").getAsString())) {
                collectIds(ingredient, ingredientIds);
            }
            assertFalse(ingredientIds.contains("thaumcraft:iteminkwell"),
                    file.getFileName() + " names thaumcraft:iteminkwell as an INGREDIENT — the "
                            + "Scribe3 recolor row needs the inkwell's 16-colour meta family "
                            + "(the original's meta 32767 wildcard), which the port's "
                            + "subtype-free ItemInkwell does not have (BD-19/BD-20)");
        }
    }

    // ------------------------------------------------------------------- data

    @Test
    void theDyeBlackTagCarriesTheOriginalsBlackDye() throws IOException {
        // 1.7.10's dyeBlack ore-dict entry was dye meta 0 — the item literally
        // named Ink Sac. 1.21.1 splits that identity: black_dye is the modern
        // dye, ink_sac the legacy item. The tag carries both so the original's
        // squid path (Scribe2 with an ink sac) survives.
        final Path tag = dataDir().resolve("tags").resolve("item").resolve("oredict.dyeblack.json");
        assertTrue(Files.isRegularFile(tag), "the dyeblack tag file is missing: " + tag);
        final JsonObject json = JsonParser.parseString(Files.readString(tag)).getAsJsonObject();
        assertTrue(json.get("required").getAsBoolean(), "the dyeblack tag must be required");
        final List<String> values = json.getAsJsonArray("values").asList().stream()
                .map(JsonElement::getAsString).toList();
        assertEquals(List.of("minecraft:black_dye", "minecraft:ink_sac"), values,
                "dyeBlack = 1.7.10 dye meta 0 (ink sac, the squid path the original had) + "
                        + "the modern black_dye — the tag carries exactly that pair");
    }

    @Test
    void everyVanillaCraftingRowIsGatedAndDecodeable() throws IOException {
        // The CI runServer smoke job fails on any boot-log line pairing an
        // (ERROR|FATAL) marker or an Exception with 'thaumcraft'. A
        // minecraft:crafting_* row that cannot decode is dropped with
        // 'Parsing error loading recipe thaumcraft:...' — an ERROR naming
        // thaumcraft — so this whole family is what the gate reads, not just
        // this card's rows. Pin the three rules the family lives by.
        final List<Path> rows = craftingRows();
        assertTrue(rows.size() >= 6, "only " + rows.size()
                + " vanilla crafting rows found — the family looks truncated");
        for (final Path file : rows) {
            final JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            final String type = json.get("type").getAsString();
            assertTrue(SHAPE.equals(type) || SHAPELESS.equals(type),
                    file.getFileName() + ": a .crafting_ row must name a vanilla crafting type, "
                            + "found " + type);

            final List<JsonObject> ingredients = ingredientsOf(json, type);
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
            if (metaCarrying) {
                // The 1.21.1 CraftingOutput codec resolves the result item at
                // parse time, so a meta-carrying row's result must be gated.
                for (final String id : resultIds) {
                    assertTrue(gated.contains(id), file.getFileName()
                            + ": a meta-carrying row must gate its result item as well");
                }
            }
        }
    }

    @Test
    void theInkwellContentMappingRowRecordsTheAcquisitionPath() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        Assumptions.assumeTrue(Files.isRegularFile(csv),
                "content mapping csv not wired into the test task");

        String row = null;
        for (final String line : Files.readAllLines(csv)) {
            if (line.startsWith("Thaumcraft:ItemInkwell,")) {
                row = line;
            }
        }
        assertNotNull(row, "the ItemInkwell row disappeared from content-mapping.csv");
        final List<String> fields = splitCsv(row);
        assertEquals(8, fields.size(),
                "the row must keep the 8-field contract, got " + fields.size() + ": " + row);
        assertEquals("thaumcraft:iteminkwell", fields.get(1), "content id");
        assertEquals("item", fields.get(2), "kind");
        assertEquals("done", fields.get(4), "implementation status");
        assertEquals("tested", fields.get(5), "test status");
        assertTrue(fields.get(6).contains("scribe1") && fields.get(6).contains("scribe2"),
                "the row must record the acquisition rows this card ships: " + fields.get(6));
        assertFalse(fields.get(6).contains(",") && !row.contains("\"" + fields.get(6)),
                "a comma in the note column needs RFC4180 quoting, got " + row);
    }

    // ---------------------------------------------------------------- plumbing

    private static JsonObject row(final String name) {
        final Path file = recipeDir().resolve(name);
        assertTrue(Files.isRegularFile(file), "the row is missing: " + file);
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

    private static Path dataDir() {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path).resolve("thaumcraft");
        assertTrue(Files.isDirectory(dir), "mod data dir not found at " + dir);
        return dir;
    }

    private static Path recipeDir() {
        final Path dir = dataDir().resolve("recipe");
        assertTrue(Files.isDirectory(dir), "recipe data dir not found at " + dir);
        return dir;
    }

    /** The MCP deobfuscated name of an Items field, read from the shipped CSV. */
    private static String mcpName(final String csv, final String field) {
        final Pattern line = Pattern.compile("(?m)^" + field + ",([^,]+),");
        final Matcher matcher = line.matcher(csv);
        assertTrue(matcher.find(), MCP_FIELDS + " has no row for field " + field);
        return matcher.group(1);
    }

    /** Every ingredient object a vanilla crafting row names. */
    private static List<JsonObject> ingredientsOf(final JsonObject json, final String type) {
        final List<JsonObject> out = new ArrayList<>();
        if (SHAPE.equals(type)) {
            for (final JsonElement key : json.getAsJsonObject("key").asMap().values()) {
                out.add(key.getAsJsonObject());
            }
        } else {
            for (final JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                out.add(ingredient.getAsJsonObject());
            }
        }
        return out;
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

    /** Quote-aware CSV split, so a quoted note column with commas stays one field. */
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
