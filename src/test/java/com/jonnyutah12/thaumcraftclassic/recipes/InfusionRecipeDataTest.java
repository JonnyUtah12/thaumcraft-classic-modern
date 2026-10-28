package com.jonnyutah12.thaumcraftclassic.recipes;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-04: the extracted infusion recipe data must stay complete and
 * consistent with the content mapping (63 crafting + 24 enchantment recipes,
 * ids 49..111 / 201..224, research keys that exist in the research graph).
 *
 * <p>Source of truth: reference {@code ConfigRecipes.initializeInfusionRecipes}
 * / {@code initializeInfusionEnchantmentRecipes} via
 * scripts/extract_infusion_recipes.py.</p>
 */
class InfusionRecipeDataTest {

    // The test JVM's cwd is the build directory, not the repo root: the data
    // files come from the handles build.gradle wires into the test task
    // (research/ResearchGraphDataTest.java:56 and worldgen/WorldGenDataParityTest.java:208
    // read the same two properties; a relative Path.of would miss every file).
    private static Path recipeDir() {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path).resolve("thaumcraft").resolve("recipe");
        assertTrue(Files.isDirectory(dir), "recipe data dir not found at " + dir);
        return dir;
    }

    private static Path researchDir() {
        final String path = System.getProperty("tc.researchDataDir");
        assertNotNull(path, "system property 'tc.researchDataDir' is not set");
        final Path dir = Path.of(path);
        assertTrue(Files.isDirectory(dir), "research graph dir not found at " + dir);
        return dir;
    }

    private static final Pattern ID = Pattern.compile("recipe\\.(\\d+)\\.(infusion|infusion_enchantment)\\.json");

    private static List<Path> recipeFiles() throws IOException {
        try (Stream<Path> s = Files.list(recipeDir())) {
            final List<Path> out = new ArrayList<>();
            s.filter(p -> ID.matcher(p.getFileName().toString()).matches())
                    .forEach(out::add);
            return out;
        }
    }

    @Test
    void allOriginalInfusionRecipesArePresent() throws IOException {
        final List<Path> files = recipeFiles();
        final List<Integer> crafting = new ArrayList<>();
        final List<Integer> enchanting = new ArrayList<>();
        for (final Path p : files) {
            final Matcher m = ID.matcher(p.getFileName().toString());
            assertTrue(m.matches());
            (m.group(2).equals("infusion") ? crafting : enchanting).add(Integer.parseInt(m.group(1)));
        }
        // Original counts: 63 addInfusionCraftingRecipe + 24 addInfusionEnchantmentRecipe.
        assertEquals(63, crafting.size(), "infusion crafting recipe count");
        assertEquals(24, enchanting.size(), "infusion enchantment recipe count");
        for (int n = 49; n <= 111; n++) {
            assertTrue(crafting.contains(n), "missing recipe." + n + ".infusion.json");
        }
        for (int n = 201; n <= 224; n++) {
            assertTrue(enchanting.contains(n), "missing recipe." + n + ".infusion_enchantment.json");
        }
    }

    @Test
    void everyRecipeCarriesResearchGateAndCost() throws IOException {
        for (final Path p : recipeFiles()) {
            final JsonObject json = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            final String type = json.get("type").getAsString();
            assertTrue(type.equals("thaumcraft:infusion") || type.equals("thaumcraft:infusion_enchantment"),
                    p + ": bad type " + type);
            assertTrue(json.has("research") && !json.get("research").getAsString().isEmpty(),
                    p + ": missing research gate");
            assertTrue(json.has("aspects") && json.getAsJsonArray("aspects").size() > 0,
                    p + ": missing essentia cost");
            assertTrue(json.has("instability"), p + ": missing instability");
            assertTrue(json.has("components") && json.getAsJsonArray("components").size() > 0,
                    p + ": missing components");
        }
    }

    @Test
    void researchKeysExistInTheResearchGraph() throws IOException {
        final Path entries = researchDir().resolve("entries.json");
        assertTrue(Files.exists(entries), "research graph missing — TC-M3-02 data must be present");
        final String graph = Files.readString(entries);
        for (final Path p : recipeFiles()) {
            final String key = JsonParser.parseString(Files.readString(p))
                    .getAsJsonObject().get("research").getAsString();
            assertTrue(graph.contains("\"" + key + "\""),
                    p.getFileName() + ": research key " + key + " not in research graph");
        }
    }

    @Test
    void everyIngredientDecodesAsAModernIngredient() throws IOException {
        // A 1.21.1 Ingredient is either {"item": id}, {"tag": id} or an object
        // carrying a registered "type" (neoforge:components for the
        // meta-carrying form this data set needs). The extractor shipped the
        // components form WITHOUT its "type" key at first, which is a hard
        // parse error for every row that used it: RecipeManager logs
        // 'Parsing error loading recipe' and drops the row. Pin the shape here
        // so the data can never silently regress to a form that cannot load.
        for (final Path p : recipeFiles()) {
            final JsonObject json = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            final List<JsonObject> ingredients = new ArrayList<>();
            for (final JsonElement component : json.getAsJsonArray("components")) {
                ingredients.add(component.getAsJsonObject());
            }
            if (json.has("input")) {
                ingredients.add(json.getAsJsonObject("input"));
            }
            assertFalse(ingredients.isEmpty(), p + ": no ingredient to check");
            for (final JsonObject ingredient : ingredients) {
                if (ingredient.has("type")) {
                    assertEquals("neoforge:components", ingredient.get("type").getAsString(),
                            p + ": unknown ingredient type " + ingredient.get("type"));
                    assertTrue(ingredient.has("items") && ingredient.has("components"),
                            p + ": neoforge:components needs items + components");
                    assertFalse(ingredient.getAsJsonArray("items").isEmpty(),
                            p + ": empty items list");
                } else {
                    assertTrue(ingredient.has("item") || ingredient.has("tag"),
                            p + ": ingredient is neither item, tag nor typed: " + ingredient);
                }
            }
        }
    }

    @Test
    void everyReferencedIdIsALowercaseResourceLocationGatedByItemExists()
            throws IOException {
        // Two failure modes this pins: (1) an id that is not a valid resource
        // location (the MCP camelCase enchantment names were written verbatim,
        // e.g. minecraft:baneOfArthropods) is a hard parse error; (2) a row
        // naming an item that is not ported yet must be SKIPPED at load, not
        // fail — so every item a row names carries an item_exists condition
        // (the extractor adds one per referenced item).
        final Pattern valid = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
        for (final Path p : recipeFiles()) {
            final JsonObject json = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            final List<String> items = new ArrayList<>();
            final List<String> ids = new ArrayList<>();
            collectIds(json.has("input") ? json.get("input") : null, items, ids);
            collectIds(json.get("components"), items, ids);
            collectIds(json.has("output") ? json.get("output") : null, items, ids);
            if (json.has("enchantment")) {
                ids.add(json.get("enchantment").getAsString());
            }
            for (final String id : ids) {
                assertTrue(valid.matcher(id).matches(),
                        p.getFileName() + ": '" + id + "' is not a valid resource location");
            }
            final Set<String> gated = new HashSet<>();
            for (final JsonElement condition : json.getAsJsonArray("neoforge:conditions")) {
                final JsonObject entry = condition.getAsJsonObject();
                if ("neoforge:item_exists".equals(entry.get("type").getAsString())) {
                    gated.add(entry.get("item").getAsString());
                }
            }
            for (final String item : items) {
                assertTrue(gated.contains(item),
                        p.getFileName() + ": item " + item + " has no item_exists gate");
            }
        }
    }

    /** Item ids (and any other id) a row's ingredient/output objects name. */
    private static void collectIds(final JsonElement node, final List<String> items,
                                  final List<String> ids) {
        if (node == null) {
            return;
        }
        if (node.isJsonArray()) {
            for (final JsonElement element : node.getAsJsonArray()) {
                collectIds(element, items, ids);
            }
            return;
        }
        if (!node.isJsonObject()) {
            return;
        }
        final JsonObject object = node.getAsJsonObject();
        if (object.has("item")) {
            items.add(object.get("item").getAsString());
        }
        if (object.has("items")) {
            final JsonElement value = object.get("items");
            if (value.isJsonArray()) {
                for (final JsonElement element : value.getAsJsonArray()) {
                    items.add(element.getAsString());
                }
            } else {
                items.add(value.getAsString());
            }
        }
        if (object.has("id")) {
            ids.add(object.get("id").getAsString());
        }
        for (final String key : List.of("input", "components", "output")) {
            collectIds(object.get(key), items, ids);
        }
    }

    @Test
    void spotCheckSilverCapAndMendingMatchOriginalRegistration() throws IOException {
        // ConfigRecipes: addInfusionCraftingRecipe(new ItemStack(ConfigItems.itemWandCap, 1, 4),
        //   "CAP_silver", new AspectList().add(Aspect.getAspect("potentia"), 8)
        //       .add(Aspect.getAspect("auram"), 4), {itemResource meta 14 x2},
        //   new ItemStack(ConfigItems.itemWandCap, 1, 5), 4)
        final JsonObject cap = JsonParser.parseString(
                Files.readString(recipeDir().resolve("recipe.49.infusion.json"))).getAsJsonObject();
        assertEquals("WandCapSilver", cap.get("original_key").getAsString());
        assertEquals("CAP_silver", cap.get("research").getAsString());
        assertEquals(4, cap.get("instability").getAsInt());
        final JsonArray aspects = cap.getAsJsonArray("aspects");
        assertEquals(2, aspects.size());
        assertEquals("potentia", aspects.get(0).getAsJsonObject().get("aspect").getAsString());
        assertEquals(8, aspects.get(0).getAsJsonObject().get("amount").getAsInt());
        assertEquals("auram", aspects.get(1).getAsJsonObject().get("aspect").getAsString());
        assertEquals(4, aspects.get(1).getAsJsonObject().get("amount").getAsInt());
        assertEquals(2, cap.getAsJsonArray("components").size());

        // ConfigRecipes: addInfusionEnchantmentRecipe(ConfigEnchantments.enchantRepair,
        //   "INFUSIONENCHANTMENT", {anvil, itemResource meta 14}, inst 4,
        //   praecantatio 8 + fabrico 10 + ordo 10)
        final JsonObject mending = JsonParser.parseString(
                Files.readString(recipeDir().resolve("recipe.201.infusion_enchantment.json"))).getAsJsonObject();
        assertEquals("InfEnchRepair", mending.get("original_key").getAsString());
        assertEquals("thaumcraft:ench_repair", mending.get("enchantment").getAsString());
        assertEquals(4, mending.get("instability").getAsInt());
        assertEquals(3, mending.getAsJsonArray("aspects").size());
    }
}
