package com.jonnyutah12.thaumcraftclassic.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final Path RECIPE_DIR =
            Path.of("src/main/resources/data/thaumcraft/recipe");
    private static final Pattern ID = Pattern.compile("recipe\\.(\\d+)\\.(infusion|infusion_enchantment)\\.json");

    private static List<Path> recipeFiles() throws IOException {
        try (Stream<Path> s = Files.list(RECIPE_DIR)) {
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
        final Path entries = Path.of("src/main/resources/data/thaumcraft/research/entries.json");
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
    void spotCheckSilverCapAndMendingMatchOriginalRegistration() throws IOException {
        // ConfigRecipes: addInfusionCraftingRecipe(new ItemStack(ConfigItems.itemWandCap, 1, 4),
        //   "CAP_silver", new AspectList().add(Aspect.getAspect("potentia"), 8)
        //       .add(Aspect.getAspect("auram"), 4), {itemResource meta 14 x2},
        //   new ItemStack(ConfigItems.itemWandCap, 1, 5), 4)
        final JsonObject cap = JsonParser.parseString(
                Files.readString(RECIPE_DIR.resolve("recipe.49.infusion.json"))).getAsJsonObject();
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
                Files.readString(RECIPE_DIR.resolve("recipe.201.infusion_enchantment.json"))).getAsJsonObject();
        assertEquals("InfEnchRepair", mending.get("original_key").getAsString());
        assertEquals("thaumcraft:mending", mending.get("enchantment").getAsString());
        assertEquals(4, mending.get("instability").getAsInt());
        assertEquals(3, mending.getAsJsonArray("aspects").size());
    }
}
