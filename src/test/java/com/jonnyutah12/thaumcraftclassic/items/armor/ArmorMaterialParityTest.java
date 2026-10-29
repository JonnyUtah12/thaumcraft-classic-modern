package com.jonnyutah12.thaumcraftclassic.items.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ArmorItem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-07-s parity evidence: the shipped armour tables, ids and display
 * names compared value-for-value against the 1.7.10 sources under
 * {@code ../reference} (skipped, not faked, when that tree is absent, per the
 * house rule in {@code worldgen/StructureParityTest}).
 *
 * <p>What is pinned, with the original's own lines:</p>
 * <ul>
 *   <li>{@code api/ThaumcraftApi.java} — the two {@code addArmorMaterial} rows
 *   (name, durability factor, defence array, enchantability) that
 *   {@code CatalogArmor} turns into registry entries;</li>
 *   <li>{@code common/config/ConfigItems.java} — how many pieces of each family
 *   the original registers, and on which material (this is what fixes the
 *   shipped scope at 8 ids: 4 thaumium + 3 robe + the goggles, no helmet for
 *   the robe);</li>
 *   <li>{@code common/items/armor/ItemGoggles.java} — {@code func_77656_e(350)}
 *   (= {@code setMaxDamage}),
 *   {@code getVisDiscount} returning 5, {@code getRunicCharge} returning 0;</li>
 *   <li>{@code common/items/armor/ItemRobeArmor.java} —
 *   {@code getVisDiscount} {@code armorType == 3 ? 1 : 2} and the
 *   {@code getColor} default 6961280;</li>
 *   <li>{@code assets/thaumcraft/lang/en_US.lang} — the eight display names and
 *   the {@code tc.visdiscount} label, against the shipped {@code en_us.json}.</li>
 * </ul>
 */
class ArmorMaterialParityTest {

    /**
     * The reference tree is a sibling of the checkout, so its depth depends on
     * where the tests run: {@code ../reference} from a normal checkout, one
     * level deeper from a {@code .worktrees/<task>} checkout. Every candidate
     * is tried, and the whole class skips (never fakes) when none exists — the
     * same rule {@code worldgen/StructureParityTest} follows.
     */
    private static final String[] REFERENCE_CANDIDATES = {
        "../reference", "../../reference", "../../../reference", "../../../../reference" };

    private static Path referenceRoot() {
        for (final String candidate : REFERENCE_CANDIDATES) {
            final Path path = Path.of(candidate);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of(REFERENCE_CANDIDATES[0]);
    }

    private static Path referenceSource(final String relative) {
        return referenceRoot().resolve("decompiled/thaumcraft/thaumcraft").resolve(relative);
    }

    private static Path referenceLang() {
        return referenceRoot().resolve("assets/thaumcraft/lang/en_US.lang");
    }

    /** The shipped file, off the test classpath (cwd-independent, as in LocalizationFileTest). */
    private static JsonObject shippedLang() throws IOException {
        try (var in = ArmorMaterialParityTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            return com.google.gson.JsonParser.parseString(
                    new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String source(final Path path) throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(path),
                "reference tree absent — parity is enforced where reference/ exists: " + path);
        return Files.readString(path);
    }

    /** {@code EnumHelper.addArmorMaterial("NAME", factor, new int[]{a, b, c, d}, ench)}. */
    private static final Pattern ADD_ARMOR_MATERIAL = Pattern.compile(
            "addArmorMaterial\\(\\s*\"([A-Z_]+)\"\\s*,\\s*(\\d+)\\s*,\\s*new int\\[\\]\\{\\s*"
                    + "(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\}\\s*,\\s*(\\d+)\\s*\\)");

    private static Map<String, int[]> originalMaterials() throws IOException {
        final Matcher matcher = ADD_ARMOR_MATERIAL.matcher(source(referenceSource("api/ThaumcraftApi.java")));
        final Map<String, int[]> found = new LinkedHashMap<>();
        while (matcher.find()) {
            // {durabilityFactor, helmet, chestplate, leggings, boots, enchantability}
            found.put(matcher.group(1), new int[] {
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)),
                Integer.parseInt(matcher.group(6)), Integer.parseInt(matcher.group(7)) });
        }
        return found;
    }

    @Test
    void thaumiumMaterialMatchesTheOriginalAddArmorMaterialRow() throws IOException {
        final int[] row = originalMaterials().get("THAUMIUM");
        assertNotNull(row, "the original's THAUMIUM armour material row");
        assertEquals(ArmorSetRules.THAUMIUM_DURABILITY_FACTOR, row[0]);
        assertEquals(ArmorSetRules.THAUMIUM_DEFENSE_HELMET, row[1]);
        assertEquals(ArmorSetRules.THAUMIUM_DEFENSE_CHESTPLATE, row[2]);
        assertEquals(ArmorSetRules.THAUMIUM_DEFENSE_LEGGINGS, row[3]);
        assertEquals(ArmorSetRules.THAUMIUM_DEFENSE_BOOTS, row[4]);
        assertEquals(ArmorSetRules.THAUMIUM_ENCHANTABILITY, row[5]);
    }

    @Test
    void specialMaterialMatchesTheOriginalAddArmorMaterialRow() throws IOException {
        final int[] row = originalMaterials().get("SPECIAL");
        assertNotNull(row, "the original's SPECIAL armour material row");
        assertEquals(ArmorSetRules.SPECIAL_DURABILITY_FACTOR, row[0]);
        assertEquals(ArmorSetRules.SPECIAL_DEFENSE_HELMET, row[1]);
        assertEquals(ArmorSetRules.SPECIAL_DEFENSE_CHESTPLATE, row[2]);
        assertEquals(ArmorSetRules.SPECIAL_DEFENSE_LEGGINGS, row[3]);
        assertEquals(ArmorSetRules.SPECIAL_DEFENSE_BOOTS, row[4]);
        assertEquals(ArmorSetRules.SPECIAL_ENCHANTABILITY, row[5]);
    }

    /**
     * The scope gate: the original registers exactly four thaumium pieces,
     * three robe pieces (no robe helmet) and one goggles item, on the two
     * materials above — which is why the shipped slice is 8 ids.
     */
    @Test
    void registeredFamiliesAreTheOriginals() throws IOException {
        final String configItems = source(referenceSource("common/config/ConfigItems.java"));
        assertEquals(4, count(configItems, "new ItemThaumiumArmor(ThaumcraftApi.armorMatThaumium"),
                "the original registers four thaumium pieces");
        assertEquals(3, count(configItems, "new ItemRobeArmor(ThaumcraftApi.armorMatSpecial"),
                "the original registers three robe pieces — the robe has no helmet");
        assertEquals(1, count(configItems, "new ItemGoggles(ThaumcraftApi.armorMatSpecial"),
                "the goggles ride armorMatSpecial");
        // The 1.7.10 ctor is (material, renderIndex, armourType-slot), so the
        // slot argument is the shipped piece list: thaumium 0..3, robe 1..3
        // (no robe helmet), goggles 0 in the head slot.
        assertEquals(4, regexCount(configItems,
                        "new ItemThaumiumArmor\\(ThaumcraftApi\\.armorMatThaumium, 2, [0-3]\\)"),
                "thaumium registers slots 0..3 with render index 2");
        assertEquals(3, regexCount(configItems,
                        "new ItemRobeArmor\\(ThaumcraftApi\\.armorMatSpecial, [12], [123]\\)"),
                "robe registers slots 1..3 (chest, legs, boots)");
        assertEquals(0, regexCount(configItems, "new ItemRobeArmor\\([^)]*, 0\\)"),
                "the 1.7.10 armourType slot 0 (helmet) is never registered for the robe");
        assertEquals(1, regexCount(configItems,
                        "new ItemGoggles\\(ThaumcraftApi\\.armorMatSpecial, 4, 0\\)"),
                "the goggles ride armorMatSpecial in the head slot (render index 4)");
    }

    /** {@code setMaxDamage(350)}, the flat 5 percent, and runic charge 0. */
    @Test
    void gogglesCarryTheirOriginalOverrides() throws IOException {
        final String goggles = source(referenceSource("common/items/armor/ItemGoggles.java"));
        assertTrue(goggles.contains("func_77656_e(350)"),
                "the original's goggles call func_77656_e (setMaxDamage) with 350 (line 31)");
        assertEquals(ArmorSetRules.GOGGLES_MAX_DAMAGE, 350);
        final Matcher discount = Pattern.compile(
                "public int getVisDiscount\\(.*?\\{.*?return (\\d+);", Pattern.DOTALL).matcher(goggles);
        assertTrue(discount.find(), "the original's goggles getVisDiscount");
        assertEquals(ArmorSetRules.GOGGLES_VIS_DISCOUNT, Integer.parseInt(discount.group(1)));
        final Matcher charge = Pattern.compile(
                "public int getRunicCharge\\(.*?\\{.*?return (\\d+);", Pattern.DOTALL).matcher(goggles);
        assertTrue(charge.find(), "the original's goggles getRunicCharge");
        assertEquals(ArmorSetRules.RUNIC_CHARGE_UNUPGRADED, Integer.parseInt(charge.group(1)));
    }

    /**
     * The robe's discount body — {@code armorType == 3 ? 1 : 2}, where 1.7.10's
     * slot index 3 is the boots — and the colour default.
     */
    @Test
    void robeDiscountAndColourAreTheOriginals() throws IOException {
        final String robe = source(referenceSource("common/items/armor/ItemRobeArmor.java"));
        final Matcher discount = Pattern.compile(
                "getVisDiscount\\([^)]*\\)\\s*\\{\\s*return this\\.\\w+ == (\\d+) \\? (\\d+) : (\\d+);")
                .matcher(robe);
        assertTrue(discount.find(), "the original's robe getVisDiscount body");
        final int bootSlot = Integer.parseInt(discount.group(1));
        final int bootsDiscount = Integer.parseInt(discount.group(2));
        final int clothDiscount = Integer.parseInt(discount.group(3));
        assertEquals(3, bootSlot, "1.7.10 armour slot 3 is the boots");
        assertEquals(bootsDiscount, ArmorSetRules.robeVisDiscount(ArmorItem.Type.BOOTS));
        assertEquals(clothDiscount, ArmorSetRules.robeVisDiscount(ArmorItem.Type.CHESTPLATE));
        assertEquals(clothDiscount, ArmorSetRules.robeVisDiscount(ArmorItem.Type.LEGGINGS));
        final Matcher colour = Pattern.compile("return (\\d{6,8});").matcher(robe);
        assertTrue(colour.find(), "the original's robe colour default (func_82814_b, line 77)");
        assertEquals(ArmorSetRules.ROBE_DEFAULT_COLOR, Integer.parseInt(colour.group(1)),
                "the undyed robe colour must be the original's int");
    }

    /** The eight display names and the tooltip label, against the shipped file. */
    @Test
    void shippedNamesAreTheOriginalNames() throws IOException {
        final Map<String, String> original = new LinkedHashMap<>();
        final Path lang = referenceLang();
        Assumptions.assumeTrue(Files.isRegularFile(lang),
                "reference tree absent — parity is enforced where reference/ exists: " + lang);
        for (final String line : Files.readAllLines(lang)) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("item.Item") && trimmed.contains(".name=")) {
                original.put(trimmed.substring("item.".length(), trimmed.indexOf(".name=")),
                        trimmed.substring(trimmed.indexOf(".name=") + ".name=".length()));
            } else if (trimmed.startsWith("tc.visdiscount=")) {
                original.put("tc.visdiscount", trimmed.substring("tc.visdiscount=".length()));
            }
        }
        final JsonObject shipped = shippedLang();
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("ItemGoggles", "item.thaumcraft.itemgoggles");
        map.put("ItemHelmetThaumium", "item.thaumcraft.itemhelmetthaumium");
        map.put("ItemChestplateThaumium", "item.thaumcraft.itemchestplatethaumium");
        map.put("ItemLeggingsThaumium", "item.thaumcraft.itemleggingsthaumium");
        map.put("ItemBootsThaumium", "item.thaumcraft.itembootsthaumium");
        map.put("ItemChestplateRobe", "item.thaumcraft.itemchestplaterobe");
        map.put("ItemLeggingsRobe", "item.thaumcraft.itemleggingsrobe");
        map.put("ItemBootsRobe", "item.thaumcraft.itembootsrobe");
        map.put("tc.visdiscount", "tc.visdiscount");
        assertEquals(9, map.size(), "the eight items of this slice plus the tooltip label");
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            final String expected = original.get(entry.getKey());
            assertNotNull(expected, "the original lang has " + entry.getKey());
            assertTrue(shipped.has(entry.getValue()), entry.getValue() + " must ship in en_us.json");
            assertEquals(expected, shipped.get(entry.getValue()).getAsString(),
                    entry.getValue() + " must read exactly as the original's " + entry.getKey());
        }
    }

    private static int regexCount(final String haystack, final String regex) {
        final Matcher matcher = Pattern.compile(regex).matcher(haystack);
        int hits = 0;
        while (matcher.find()) {
            hits++;
        }
        return hits;
    }

    private static int count(final String haystack, final String needle) {
        int hits = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            hits++;
        }
        return hits;
    }
}
