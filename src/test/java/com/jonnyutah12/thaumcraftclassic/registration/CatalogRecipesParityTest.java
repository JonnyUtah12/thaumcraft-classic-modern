package com.jonnyutah12.thaumcraftclassic.registration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-04a recipe guard: the four plain crafting recipes this card owns
 * (content-mapping.csv rows recipe.264/266/274/275.crafting_shaped, from
 * the original ConfigRecipes.java GameRegistry.addShapedRecipe calls) exist
 * as datapack recipes whose shape, input and result (item, count and
 * item_meta component) match the original, and whose rows are flipped to
 * done. The original's meta-specific inputs (blockCosmeticSolid 4/5,
 * blockCosmeticOpaque 0/1) are item-level here — the blocks' variant model
 * is TC-M6-04b — so each recipe is gated with neoforge:item_exists until
 * that card lands; the deviation is recorded in the rows'
 * known_differences column.
 */
final class CatalogRecipesParityTest {

    private record Spec(String id, String input, int count, int meta) {
    }

    // Original ConfigRecipes.java lines 3899/3907/3949/3950.
    private static final List<Spec> SPECS = List.of(
            new Spec("264", "thaumcraft:blockcosmeticsolid", 9, 2),
            new Spec("266", "thaumcraft:blockcosmeticsolid", 9, 4),
            new Spec("274", "thaumcraft:blockcosmeticopaque", 4, 6),
            new Spec("275", "thaumcraft:blockcosmeticopaque", 4, 6));

    @Test
    void craftingRecipesMatchTheOriginalAndTheContract() throws IOException {
        final Path dataDir = Path.of(System.getProperty("tc.modDataDir", ""));
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        Assumptions.assumeTrue(Files.isDirectory(dataDir), "tc.modDataDir not set");
        Assumptions.assumeTrue(Files.isRegularFile(csv), "tc.contentMappingFile not set");

        final List<ContentMappingCsv.Row> rows = ContentMappingCsv.read(csv);
        for (final Spec spec : SPECS) {
            final String newId = "thaumcraft:recipe." + spec.id() + ".crafting_shaped";
            final ContentMappingCsv.Row row = rows.stream()
                    .filter(r -> r.newId().equals(newId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no content-mapping row for " + newId));
            assertEquals("done", row.implementationStatus(), newId + " must be marked done");
            // The row's display_name column carries the original result spec
            // "ItemResource, <count>, <meta>" — cross-check it against the file.
            final String[] resultSpec = row.displayName().split(",");
            assertEquals(spec.count(), Integer.parseInt(resultSpec[1].trim()), newId + " count vs CSV");
            assertEquals(spec.meta(), Integer.parseInt(resultSpec[2].trim()), newId + " meta vs CSV");

            final Path file = dataDir.resolve("thaumcraft/recipe/recipe." + spec.id() + ".crafting_shaped.json");
            assertTrue(Files.isRegularFile(file), "missing recipe file " + file);
            final JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

            assertEquals("minecraft:crafting_shaped", json.get("type").getAsString(), newId + " type");
            final JsonArray pattern = json.getAsJsonArray("pattern");
            assertEquals(1, pattern.size(), newId + " must be a single-cell pattern (original {\"K\"})");
            assertEquals("K", pattern.get(0).getAsString(), newId + " pattern");
            assertEquals(spec.input(),
                    json.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString(),
                    newId + " input item");

            final JsonObject result = json.getAsJsonObject("result");
            assertEquals("thaumcraft:itemresource", result.get("id").getAsString(), newId + " result item");
            assertEquals(spec.count(), result.get("count").getAsInt(), newId + " result count");
            assertEquals(spec.meta(),
                    result.getAsJsonObject("components").get("thaumcraft:item_meta").getAsInt(),
                    newId + " result item_meta component");

            final JsonArray conditions = json.getAsJsonArray("neoforge:conditions");
            assertEquals("neoforge:item_exists", conditions.get(0).getAsJsonObject().get("type").getAsString(),
                    newId + " must be gated until its input block is registered (TC-M6-04b)");
            assertEquals(spec.input(), conditions.get(0).getAsJsonObject().get("item").getAsString(),
                    newId + " gate item");
        }
    }
}
