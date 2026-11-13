package com.jonnyutah12.thaumcraftclassic.enchantment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enchantment id contract (TC-M6-05-s-e1c): the two enchantments the shipped
 * infusion rows name are registered, under their exact ids, and no shipped row
 * names a third one.
 *
 * <p>In 1.21.1 an enchantment is datapack registry data — a JSON payload under
 * {@code data/<namespace>/enchantment/<path>.json} ({@code minecraft:enchantment}
 * is a dynamic registry, not part of {@code BuiltInRegistries}, so a
 * DeferredRegister for it silently never populates). The payload path IS the
 * id, so this probe pins the path, the codec shape and the recipe references
 * without booting Minecraft. The live-registry half runs in
 * {@code EnchantmentRegistrationGameTest}.</p>
 *
 * <p>The required-field list is the field names of
 * {@code Enchantment.DIRECT_CODEC} plus the flattened
 * {@code Enchantment.EnchantmentDefinition.CODEC} in NeoForge 21.1.259
 * ({@code description}, {@code supported_items}, {@code weight},
 * {@code max_level}, {@code min_cost}, {@code max_cost}, {@code anvil_cost},
 * {@code slots}); {@code primary_items}, {@code exclusive_set} and
 * {@code effects} are optional. A payload missing one of the required fields
 * fails the {@code minecraft:enchantment} registry load — a datapack-wide
 * parse failure — so the shape is pinned here instead of being discovered in a
 * boot log.</p>
 */
class EnchantmentIdsTest {

    private static final String NAMESPACE = "thaumcraft";

    /** The two ids the contract ships (content-mapping.csv kind=enchantment rows). */
    private static final List<String> ENCHANTMENT_PATHS = List.of("ench_repair", "ench_haste");

    /** The contract ids, as {@code new_id} strings (namespace:path). */
    private static final List<String> CONTRACT_IDS = List.of("thaumcraft:ench_repair", "thaumcraft:ench_haste");

    private static final List<String> REQUIRED_CODEC_FIELDS = List.of(
            "description", "supported_items", "weight", "max_level",
            "min_cost", "max_cost", "anvil_cost", "slots");

    /** EquipmentSlotGroup serialised names — the only values the {@code slots} list accepts. */
    private static final List<String> SLOT_GROUPS = List.of(
            "any", "mainhand", "offhand", "hand", "feet", "legs", "chest", "head", "armor", "body");

    private static final List<String> COST_FIELDS = List.of("base", "per_level_above_first");

    // The test JVM's cwd is the build directory, so the shipped data comes from
    // the handle build.gradle wires into the test task (tc.modDataDir) and the
    // lang file comes from the classpath — see recipes/InfusionRecipeDataTest.java:36.
    private static Path modDataDir() {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path);
        assertTrue(Files.isDirectory(dir), "mod data dir not found at " + dir);
        return dir;
    }

    /** The singular {@code enchantment} folder: the 1.21.1 datapack layout. */
    private static Path enchantmentDir() {
        final Path dir = modDataDir().resolve(NAMESPACE).resolve("enchantment");
        assertTrue(Files.isDirectory(dir), "enchantment payload dir not found at " + dir);
        return dir;
    }

    private static Path recipeDir() {
        return modDataDir().resolve(NAMESPACE).resolve("recipe");
    }

    private static JsonObject payload(final String path) {
        final Path json = enchantmentDir().resolve(path + ".json");
        assertTrue(Files.isRegularFile(json), "no payload at " + json);
        return parse(json);
    }

    private static JsonObject parse(final Path file) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every shipped enchantment payload's path component (the id sans namespace). */
    private static Set<String> shippedPayloadPaths() throws IOException {
        try (Stream<Path> files = Files.list(enchantmentDir())) {
            final Set<String> out = new TreeSet<>();
            files.forEach(p -> {
                final String name = p.getFileName().toString();
                assertTrue(name.endsWith(".json"), p + " is not a .json payload");
                out.add(name.substring(0, name.length() - ".json".length()));
            });
            return out;
        }
    }

    @Test
    void thePayloadsAreTheContractIdsAndNothingElse() throws IOException {
        assertEquals(new TreeSet<>(ENCHANTMENT_PATHS), shippedPayloadPaths(),
                "data/" + NAMESPACE + "/enchantment must carry exactly the two contract payloads");
    }

    /**
     * The registry name is pinned through the id contract: a
     * content-mapping.csv row of {@code kind=enchantment} resolves through
     * {@code Registries.ENCHANTMENT} ({@code minecraft:enchantment}) in the
     * registration harness ({@code gametest/RegistrationGameTest.DATAPACK_KINDS}),
     * and in 1.21.1 that registry loads from the singular
     * {@code data/<ns>/enchantment/} folder, filename = path.
     */
    @Test
    void eachContractIdIsAnEnchantmentRowWhosePayloadPathEncodesIt() throws IOException {
        final Map<String, String> kindByNewId = new LinkedHashMap<>();
        for (final ContentMappingCsv.Row row : ContentMappingCsv.read(
                Path.of(System.getProperty("tc.contentMappingFile", "")))) {
            kindByNewId.put(row.newId(), row.kind());
        }

        for (final String id : CONTRACT_IDS) {
            assertEquals("enchantment", kindByNewId.get(id),
                    id + " must be inventoried as kind=enchantment, the registry the harness resolves it in");

            final int colon = id.indexOf(':');
            final String namespace = id.substring(0, colon);
            final String path = id.substring(colon + 1);
            final Path payload = modDataDir().resolve(namespace).resolve("enchantment").resolve(path + ".json");
            assertTrue(Files.isRegularFile(payload),
                    id + " has no payload on the registry path data/" + namespace + "/enchantment/" + path + ".json");
        }
    }

    @Test
    void everyPayloadCarriesThe1211EnchantmentCodecFields() {
        for (final String path : ENCHANTMENT_PATHS) {
            final JsonObject json = payload(path);
            for (final String field : REQUIRED_CODEC_FIELDS) {
                assertTrue(json.has(field), path + ".json is missing the required codec field '" + field + "'");
            }
            assertTrue(json.get("description").isJsonObject(), path + ".json description must be a component object");
            assertTrue(json.getAsJsonObject("description").has("translate"),
                    path + ".json description must be a translate component");
            assertTrue(json.get("supported_items").getAsString().startsWith("#"),
                    path + ".json supported_items must be an item-tag reference");

            final int weight = json.get("weight").getAsInt();
            assertTrue(weight >= 1 && weight <= 1024,
                    path + ".json weight out of ExtraCodecs.intRange(1, 1024): " + weight);
            final int maxLevel = json.get("max_level").getAsInt();
            assertTrue(maxLevel >= 1 && maxLevel <= 255,
                    path + ".json max_level out of ExtraCodecs.intRange(1, 255): " + maxLevel);
            assertTrue(json.get("anvil_cost").getAsInt() >= 0, path + ".json anvil_cost must be non-negative");

            for (final String cost : COST_FIELDS) {
                assertTrue(json.getAsJsonObject("min_cost").has(cost), path + ".json min_cost." + cost + " is required");
                assertTrue(json.getAsJsonObject("max_cost").has(cost), path + ".json max_cost." + cost + " is required");
            }
            assertTrue(json.getAsJsonObject("max_cost").get("base").getAsInt()
                            >= json.getAsJsonObject("min_cost").get("base").getAsInt(),
                    path + ".json max_cost.base must not sit below min_cost.base");

            final JsonArray slots = json.getAsJsonArray("slots");
            assertTrue(!slots.isEmpty(), path + ".json slots must not be empty (EquipmentSlotGroup list)");
            for (final var slot : slots) {
                assertTrue(SLOT_GROUPS.contains(slot.getAsString()),
                        path + ".json slots carries " + slot + ", not an EquipmentSlotGroup name");
            }
        }
    }

    @Test
    void eachPayloadNamesItsVanillaShapeKeyAndThatKeyShipsWithTheOriginalLabel() {
        final JsonObject lang = lang();
        for (final String path : ENCHANTMENT_PATHS) {
            final String key = payload(path).getAsJsonObject("description").get("translate").getAsString();
            assertEquals("enchantment." + NAMESPACE + "." + path, key,
                    path + " description key must be the 1.21.1 enchantment key shape");
            assertTrue(lang.has(key), key + " is not shipped in assets/thaumcraft/lang/en_us.json");
        }
        assertEquals("Repair", lang.get("enchantment.thaumcraft.ench_repair").getAsString(),
                "original en_US.lang: enchantment.repair = Repair");
        assertEquals("Haste", lang.get("enchantment.thaumcraft.ench_haste").getAsString(),
                "original en_US.lang: enchantment.haste = Haste");
    }

    @Test
    void theTwoInfusionRowsReferenceExactlyTheseTwoIds() {
        final JsonObject repair = recipe("recipe.201.infusion_enchantment.json");
        final JsonObject haste = recipe("recipe.202.infusion_enchantment.json");
        assertEquals("thaumcraft:ench_repair", repair.get("enchantment").getAsString(),
                "recipe.201 must name the Repair id");
        assertEquals("thaumcraft:ench_haste", haste.get("enchantment").getAsString(),
                "recipe.202 must name the Haste id");
        assertEquals("InfEnchRepair", repair.get("original_key").getAsString(),
                "recipe.201 keeps the original ConfigRecipes key");
        assertEquals("InfEnchHaste", haste.get("original_key").getAsString(),
                "recipe.202 keeps the original ConfigRecipes key");
    }

    /**
     * No shipped infusion_enchantment row may name a thaumcraft enchantment
     * this slice did not register: such a row loads and then never matches,
     * which is the defect this card closes.
     */
    @Test
    void noInfusionRowNamesAnUnregisteredThaumcraftEnchantment() throws IOException {
        final List<Path> rows = new ArrayList<>();
        try (Stream<Path> files = Files.list(recipeDir())) {
            files.filter(p -> p.getFileName().toString().endsWith(".infusion_enchantment.json")).forEach(rows::add);
        }
        assertEquals(24, rows.size(), "expected the 24 extracted infusion_enchantment rows (201..224)");

        final Set<String> named = new LinkedHashSet<>();
        for (final Path row : rows) {
            final String enchantment = parse(row).get("enchantment").getAsString();
            if (enchantment.startsWith(NAMESPACE + ":")) {
                named.add(enchantment);
            }
        }
        assertEquals(Set.of("thaumcraft:ench_repair", "thaumcraft:ench_haste"), named,
                "the infusion rows name thaumcraft enchantments beyond the registered pair");
    }

    private static JsonObject recipe(final String fileName) {
        final Path file = recipeDir().resolve(fileName);
        assertTrue(Files.isRegularFile(file), fileName + " is not shipped");
        return parse(file);
    }

    private static JsonObject lang() {
        try (InputStream in = EnchantmentIdsTest.class.getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
