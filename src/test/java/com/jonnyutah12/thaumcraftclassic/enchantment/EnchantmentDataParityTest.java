package com.jonnyutah12.thaumcraftclassic.enchantment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.testsupport.ParityPaths;
import org.junit.jupiter.api.Test;

/**
 * TC-M6-05-s: the enchantment data contract. 1.21.1 enchantments are
 * datapack JSON, so the shipped payload is the behaviour: applicability,
 * slots, levels, weights, costs and the exclusivity set are asserted here, and
 * the id/lang pair is pinned so an enchantment can never ship named by its key.
 *
 * <p>Every file is read from the classpath (the shipped resource tree), so the
 * pins run identically on the code-only CI build — see
 * {@link ParityPaths} for why these tests do not open relative paths.</p>
 */
class EnchantmentDataParityTest {

    private static JsonObject json(final String classpathPath) {
        return JsonParser.parseString(ParityPaths.classpathResource(classpathPath)).getAsJsonObject();
    }

    @Test
    void hasteMatchesTheOriginalsArcaneBootsEnchantment() {
        final JsonObject haste = json("data/thaumcraft/enchantment/ench_haste.json");
        assertEquals("enchantment.thaumcraft.ench_haste",
                haste.get("description").getAsJsonObject().get("translate").getAsString());
        assertEquals(3, haste.get("max_level").getAsInt(), "the original's maxLevel 3");
        assertEquals(3, haste.get("weight").getAsInt(), "the original's enchantment frequency 3");
        assertEquals("feet", haste.get("slots").getAsJsonArray().get(0).getAsString(),
                "the original's Type.armorFeet");
        assertEquals("#thaumcraft:enchantments/haste_applicable",
                haste.get("supported_items").getAsString(),
                "the 1.7.10 isApplicable() predicate is a tag in 1.21.1 (# = tag reference)");
        assertFalse(haste.has("exclusive_set"), "the original's Haste excludes nothing");
        assertEquals(2, haste.get("anvil_cost").getAsInt());

        // Cost tables are the vanilla 1.21.1 enchanting-table shape the payload
        // ships with; pin them so a silent edit shows up.
        assertEquals(15, haste.getAsJsonObject("min_cost").get("base").getAsInt());
        assertEquals(9, haste.getAsJsonObject("min_cost").get("per_level_above_first").getAsInt());
        assertEquals(65, haste.getAsJsonObject("max_cost").get("base").getAsInt());
    }

    @Test
    void repairIsExclusiveAgainstMendingAndCapsAtTwoLevels() {
        final JsonObject repair = json("data/thaumcraft/enchantment/ench_repair.json");
        assertEquals("enchantment.thaumcraft.ench_repair",
                repair.get("description").getAsJsonObject().get("translate").getAsString());
        assertEquals(2, repair.get("max_level").getAsInt(), "the original's maxLevel 2");
        assertEquals(2, repair.get("weight").getAsInt(), "the original's enchantment frequency 2");
        assertEquals("any", repair.get("slots").getAsJsonArray().get(0).getAsString(),
                "the original's Type.all");
        assertEquals("#thaumcraft:enchantments/repair_applicable",
                repair.get("supported_items").getAsString());
        assertEquals("#thaumcraft:exclusive_set/repair", repair.get("exclusive_set").getAsString());

        // The original's exclusion is Unbreaking, not Mending:
        // EnchantmentRepair.func_77326_a is
        // "super && par1Enchantment.field_77352_x != Enchantment.field_77347_r.field_77352_x",
        // and fields_1.7.10_mcp_stable_11.csv:3951 maps field_77347_r to
        // Enchantment.unbreaking. 1.21.1 measures compatibility as
        // "a.exclusiveSet.contains(b) || b.exclusiveSet.contains(a)"
        // (Enchantment.areCompatible, Enchantment.java:172), and vanilla
        // Unbreaking/Mending ship NO exclusive_set of their own
        // (data/minecraft/tags/enchantment/exclusive_set/ has only armor,
        // boots, bow, crossbow, damage, mining, riptide) — so naming
        // minecraft:unbreaking in OUR set is the only way to express the
        // original's exclusion, and Mending stays compatible exactly as the
        // original left it.
        final JsonObject set = json("data/thaumcraft/tags/enchantment/exclusive_set/repair.json");
        final var values = set.get("values").getAsJsonArray();
        assertEquals(2, values.size(), "the set is Repair + Unbreaking (vanilla convention lists self)");
        assertTrue(values.toString().contains("thaumcraft:ench_repair"));
        assertTrue(values.toString().contains("minecraft:unbreaking"));
        assertTrue(!values.toString().contains("minecraft:mending"),
                "the original excludes only Unbreaking; Mending must stay compatible");
        assertTrue(set.get("required").getAsBoolean(),
                "a dangling tag reference must fail the data pack, not resolve to empty");

        // The original's applicability is a predicate — EnchantmentRepair
        // .func_92089_a is "stack.isItemValid() && (item instanceof IRepairable
        // || item instanceof ItemBook)" over EnumEnchantmentType.all — and
        // 1.21.1 has no interface predicate in supported_items, only item sets.
        // An EMPTY tag makes the enchantment inert (it supports nothing), which
        // is exactly the shape the salvaged payload shipped, so the set must be
        // non-empty and carry the durability-wearing families the IRepairable
        // domain maps to. The probe item is the IRepairableExtended instance
        // the game test pays vis from.
        final JsonObject applicable = json("data/thaumcraft/tags/item/enchantments/repair_applicable.json");
        final String repairable = applicable.get("values").getAsJsonArray().toString();
        assertTrue(applicable.get("values").getAsJsonArray().size() >= 3,
                "an empty supported_items tag makes ench_repair inert: " + repairable);
        assertTrue(repairable.contains("thaumcraft:test_probe_repairable"),
                "the test probe item must be repairable so the game test can pay vis");
        assertTrue(repairable.contains("#minecraft:enchantable/durability"),
                "the tools family of the original's IRepairable domain: " + repairable);
        assertTrue(repairable.contains("#minecraft:enchantable/equippable"),
                "the worn-gear family of the original's IRepairable domain: " + repairable);
        assertTrue(applicable.get("required").getAsBoolean(),
                "a dangling tag reference must fail the data pack, not resolve to empty");
    }

    @Test
    void bothEnchantmentsCarryTheirLangEntries() {
        final String lang = ParityPaths.lang();
        assertTrue(lang.contains("\"enchantment.thaumcraft.ench_haste\": \"Haste\""),
                "the original names Haste plainly (en_US.lang:3 enchantment.haste=Haste)");
        assertTrue(lang.contains("\"enchantment.thaumcraft.ench_repair\": \"Repair\""),
                "the original's enchantment.repair=Repair");
    }

    @Test
    void hastesApplicableTagIsTheBootsFamily() {
        final JsonObject tag = json("data/thaumcraft/tags/item/enchantments/haste_applicable.json");
        final String values = tag.get("values").getAsJsonArray().toString();
        // The original's gate is EnumEnchantmentType.armor plus
        // "((ItemArmor) item).field_77881_a == 3", and
        // fields_1.7.10_mcp_stable_11.csv:4043 maps field_77881_a to armorType
        // with the documented range "0 is helmet, 1 is plate, 2 is legs and 3
        // is boots" — so Haste is the boots family. The 1.21.1 tag of that
        // family is #minecraft:enchantable/foot_armor (vanilla ships it as
        // #minecraft:foot_armor); there is NO minecraft:enchantable/footwear,
        // so naming one would leave the enchantment supporting nothing.
        assertTrue(values.contains("#minecraft:enchantable/foot_armor"),
                "haste is a boots enchantment: " + values);
        assertTrue(tag.get("required").getAsBoolean(),
                "a dangling tag reference must fail the data pack, not resolve to empty");
    }
}
