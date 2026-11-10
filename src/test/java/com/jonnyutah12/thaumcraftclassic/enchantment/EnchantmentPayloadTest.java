package com.jonnyutah12.thaumcraftclassic.enchantment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;

/**
 * TC-M6-05-s-e1b (enchantment registration only): the two datapack payloads
 * under {@code data/thaumcraft/enchantment/} must exist, keep the original
 * numbers, and the content-mapping rows must carry the now-true status.
 *
 * <p>Source of truth: {@code reference/decompiled/thaumcraft/thaumcraft/common/
 * lib/enchantment/EnchantmentRepair.java} and {@code EnchantmentHaste.java} —
 * the 1.7.10 cost functions are {@code func_77321_a} (Repair {@code 20 +
 * (level-1)*10}; Haste {@code 15 + (level-1)*9}) and {@code func_77317_b}
 * (always {@code minCost + 50}); the weights are {@code func_77325_b} (2 / 3)
 * and the enchantment types are {@code EnumEnchantmentType.all} (Repair) vs
 * {@code armor} (Haste, whose {@code func_92089_a} accepts {@code ItemArmor}
 * in slot 3 — boots — or {@code ItemHoverHarness} or {@code ItemBook}).
 * {@code max_level} is the port's ceiling for the original's level-capped
 * behaviour (Repair stops at II inside {@code doRepair}; Haste runs to III).
 * {@code anvil_cost} has no 1.7.10 counterpart — the WIP transcription on the
 * read-only {@code wip-salvage} branch kept 2 for both and this card ships the
 * same. Domain transcription: the original's {@code IRepairable}/{@code
 * ItemBook} and boots/hover-harness/{@code ItemBook} legs are carried as the
 * shipped {@code #thaumcraft:enchantments/*} item tags; the {@code IRepairable}
 * items themselves are TC-M6-02 and books/hover harness are not ported yet, so
 * the Repair tag carries the vanilla tool tags as its stand-in domain today.
 * The Repair conflict exclusion models the original's {@code func_77326_a}
 * rejection of one other enchantment ({@code Enchantment.field_77347_r}, which
 * {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv:3951} resolves
 * to {@code unbreaking}); Haste overrides {@code canCombine} in no way, so its
 * payload carries no {@code exclusive_set}.</p>
 *
 * <p>Schema authority for the key names: {@code net.minecraft.world.item.
 * enchantment.Enchantment.CODEC} in the pinned Minecraft 1.21.1 jar —
 * {@code description} (a {@code ComponentSerialization} value, required),
 * {@code exclusive_set} (optional, ids from {@code Registries.ENCHANTMENT})
 * and {@code effects} (optional) sit beside {@code Enchantment$
 * EnchantmentDefinition.CODEC}, whose keys ({@code supported_items}, optional
 * {@code primary_items}, {@code weight}, {@code max_level}, {@code min_cost},
 * {@code max_cost}, {@code anvil_cost}, {@code slots}) are merged into the same
 * JSON object. An unknown key is ignored by the codec, so a key this class
 * pins that the game did not read would be a silent no-op rather than an
 * error — hence the key names come from the jar, not from the 1.7.10 field
 * table.</p>
 */
class EnchantmentPayloadTest {

    /** The shipped payload directory (same handle build.gradle wires for the
     * recipe/data tests: the test JVM's cwd is the build directory, not the
     * repo root — see recipes/InfusionRecipeDataTest.java:41). */
    private static Path dataDir() {
        final String path = System.getProperty("tc.modDataDir");
        assertNotNull(path, "system property 'tc.modDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path).resolve("thaumcraft").resolve("enchantment");
        assertTrue(Files.isDirectory(dir), "enchantment payload dir not shipped at " + dir);
        return dir;
    }

    private static JsonObject payload(final String name) throws IOException {
        final Path file = dataDir().resolve(name);
        assertTrue(Files.isRegularFile(file), "payload " + name + " is not shipped");
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static JsonObject readJson(final Path file) throws IOException {
        assertTrue(Files.isRegularFile(file), file + " is not shipped");
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    /** The shipped ids of a field the codecs read as a list — the shipped
     * payloads and tags use the list form exactly where the game's own files do
     * (`slots` in data/minecraft/enchantment/*.json is a list; `supported_items`
     * and `exclusive_set` are single ids there, asserted through idOf below). */
    private static List<String> idsOf(final JsonObject object, final String key) {
        final List<String> out = new ArrayList<>();
        object.getAsJsonArray(key).forEach(element -> out.add(element.getAsString()));
        return out;
    }

    private static String idOf(final JsonObject object, final String key) {
        return object.get(key).getAsString();
    }

    /** Enumerate the shipped payloads — this card ships exactly two. */
    @Test
    void onlyTheTwoCardPayloadsAreShipped() throws IOException {
        final List<String> names = new ArrayList<>();
        try (var entries = Files.list(dataDir())) {
            entries.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> names.add(p.getFileName().toString()));
        }
        assertEquals(List.of("ench_haste.json", "ench_repair.json"), names.stream().sorted().toList(),
                "TC-M6-05-s-e1b ships exactly the two enchantments the original registers "
                        + "(the other three jar classes are dead code, TC-M0-04 triage)");
    }

    @Test
    void repairPayloadCarriesTheOriginalNumbers() throws IOException {
        final JsonObject payload = payload("ench_repair.json");
        assertEquals("enchantment.thaumcraft.ench_repair",
                payload.getAsJsonObject("description").get("translate").getAsString(),
                        "the translate key must be the 1.21.1 key the game derives for this id "
                                + "(Util.makeDescriptionId(\"enchantment\", id))");
        assertEquals(2, payload.get("weight").getAsInt(), "Repair weight is func_77325_b = 2");
        assertEquals(2, payload.get("max_level").getAsInt(),
                "the original caps Repair at II inside doRepair");
        final JsonObject minCost = payload.getAsJsonObject("min_cost");
        assertEquals(20, minCost.get("base").getAsInt(),
                "Repair minCost is func_77321_a = 20 + (level-1)*10");
        assertEquals(10, minCost.get("per_level_above_first").getAsInt(),
                "Repair minCost is func_77321_a = 20 + (level-1)*10");
        final JsonObject maxCost = payload.getAsJsonObject("max_cost");
        assertEquals(70, maxCost.get("base").getAsInt(), "func_77317_b: maxCost = minCost + 50");
        assertEquals(10, maxCost.get("per_level_above_first").getAsInt(),
                "func_77317_b keeps the same per-level term");
        assertEquals(List.of("any"), idsOf(payload, "slots"),
                "the original type is EnumEnchantmentType.all");
        assertEquals("#thaumcraft:enchantments/repair_applicable", idOf(payload, "supported_items"),
                "the applicability domain is carried by the shipped item tag");
        assertEquals("#thaumcraft:exclusive_set/repair", idOf(payload, "exclusive_set"),
                "the canCombine exclusion of Enchantment.field_77347_r = unbreaking");
    }

    @Test
    void hastePayloadCarriesTheOriginalNumbers() throws IOException {
        final JsonObject payload = payload("ench_haste.json");
        assertFalse(payload.has("exclusive_set"),
                "EnchantmentHaste overrides canCombine in no way, so it excludes nothing");
        assertEquals("enchantment.thaumcraft.ench_haste",
                payload.getAsJsonObject("description").get("translate").getAsString(),
                        "the translate key must be the 1.21.1 key the game derives for this id");
        assertEquals(3, payload.get("weight").getAsInt(), "Haste weight is func_77325_b = 3");
        assertEquals(3, payload.get("max_level").getAsInt(),
                "the original Haste runs to level III");
        final JsonObject minCost = payload.getAsJsonObject("min_cost");
        assertEquals(15, minCost.get("base").getAsInt(),
                "Haste minCost is func_77321_a = 15 + (level-1)*9");
        assertEquals(9, minCost.get("per_level_above_first").getAsInt(),
                "Haste minCost is func_77321_a = 15 + (level-1)*9");
        final JsonObject maxCost = payload.getAsJsonObject("max_cost");
        assertEquals(65, maxCost.get("base").getAsInt(), "func_77317_b: maxCost = minCost + 50");
        assertEquals(9, maxCost.get("per_level_above_first").getAsInt(),
                "func_77317_b keeps the same per-level term");
        assertEquals(List.of("feet"), idsOf(payload, "slots"),
                "the original type is armor and func_92089_a checks the boots slot");
        assertEquals("#thaumcraft:enchantments/haste_applicable", idOf(payload, "supported_items"),
                "the applicability domain is carried by the shipped item tag");
    }

    /** The applicability/exclusion tags the payloads reference must be shipped
     * and carry the transcribed domains. */
    @Test
    void applicabilityTagsAreShippedWithTheTranscribedDomains() throws IOException {
        final Path tagsDir = dataDir().getParent().resolve("tags");
        assertTrue(Files.isDirectory(tagsDir), "tag dir not shipped at " + tagsDir);
        final JsonObject repairTag = readJson(
                tagsDir.resolve("item").resolve("enchantments").resolve("repair_applicable.json"));
        assertEquals(List.of("#minecraft:axes", "#minecraft:pickaxes", "#minecraft:shovels",
                "#minecraft:hoes"), idsOf(repairTag, "values"),
                "the Repair stand-in domain is the vanilla tool tags until TC-M6-02 joins "
                        + "the original's IRepairable items");
        final JsonObject hasteTag = readJson(
                tagsDir.resolve("item").resolve("enchantments").resolve("haste_applicable.json"));
        assertEquals(List.of("#minecraft:enchantable/foot_armor"), idsOf(hasteTag, "values"),
                "the Haste domain is boots (the hover harness and ItemBook legs are unported "
                        + "and are not carried)");
        final JsonObject exclusiveTag = readJson(
                tagsDir.resolve("enchantment").resolve("exclusive_set").resolve("repair.json"));
        assertEquals(List.of("minecraft:unbreaking"), idsOf(exclusiveTag, "values"),
                "the enchantment field_77347_r resolves to (reference/mcp-mappings line 3951)");
    }

    /** The shipped display strings must match the original en_US.lang
     * verbatim (the card: take them from the reference lang file). */
    @Test
    void displayStringsAreVerbatimFromTheOriginalLang() throws IOException {
        final Path original = Path.of(System.getProperty("tc.originalLangFile", ""));
        assertTrue(Files.isRegularFile(original),
                "the reference tree (reference/assets/thaumcraft/lang/en_US.lang) is not present");
        final String text = Files.readString(original);
        final String repairName = originalValue(text, "enchantment.repair");
        final String hasteName = originalValue(text, "enchantment.haste");
        assertNotNull(repairName, "the original lang has no enchantment.repair key");
        assertNotNull(hasteName, "the original lang has no enchantment.haste key");
        assertEquals("Repair", repairName, "the original Repair name must survive verbatim");
        assertEquals("Haste", hasteName, "the original Haste name must survive verbatim");

        final JsonObject lang;
        try (InputStream in = EnchantmentPayloadTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "en_us.json is not on the classpath");
            lang = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertEquals(repairName, lang.get("enchantment.thaumcraft.ench_repair").getAsString(),
                "the shipped Repair display string must be verbatim from the original");
        assertEquals(hasteName, lang.get("enchantment.thaumcraft.ench_haste").getAsString(),
                "the shipped Haste display string must be verbatim from the original");
    }
    private static String originalValue(final String text, final String key) {
        for (final String line : text.split("\\R")) {
            if (line.startsWith(key + "=")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return null;
    }

    /** The two content-mapping rows must now carry done/tested — the payloads
     * resolving is asserted game-test-side (RegistrationGameTest resolves kind
     * =enchantment rows through the live registry, and EnchantmentGameTest
     * proves the two infusion rows match); the unit side keeps the contract
     * rows unambiguous and flips nothing else. */
    @Test
    void contractRowsCarryTheNewStatus() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired");
        final List<String> found = new ArrayList<>();
        for (final ContentMappingCsv.Row row : ContentMappingCsv.read(csv)) {
            if (row.kind().equals("enchantment")
                    && (row.newId().equals("thaumcraft:ench_haste")
                            || row.newId().equals("thaumcraft:ench_repair"))) {
                found.add(row.newId() + "/" + row.implementationStatus() + "/" + row.testStatus());
            }
        }
        assertEquals(List.of("thaumcraft:ench_haste/done/tested",
                "thaumcraft:ench_repair/done/tested"), found.stream().sorted().toList(),
                "TC-M6-05-s-e1b flips exactly the two live enchantment rows to done/tested "
                        + "(the three dead-code rows stay not_started)");
    }
}
