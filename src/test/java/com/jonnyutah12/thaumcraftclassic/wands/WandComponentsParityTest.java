package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-01 acceptance: the rod/cap tables against the original registration
 * block, value for value.
 *
 * <p>Source of truth: {@code thaumcraft.common.config.ConfigItems}
 * {@code initializeWandComponents} + {@code postInit}
 * ({@code common/config/ConfigItems.java:287-322}) of the decompiled
 * Thaumcraft 4.2.3.5 tree in {@code reference/decompiled}. Every literal below
 * is transcribed from those lines, so a wrong capacity, cost, modifier or meta
 * fails here rather than in game.</p>
 *
 * <p>The wand and cap item classes are not exercised (they need a live item
 * registry and the {@code thaumcraft:aspect} registry); the wand item's rules
 * that are pure arithmetic — the original recipe damage value and the
 * cap×rod capacity product — are, and the pool/registration half runs in
 * {@code gametest/WandGameTest}.</p>
 */
class WandComponentsParityTest {

    // ---------------------------------------------------------------- caps

    @Test
    void capTableMatchesTheOriginalRegistrationBlock() {
        final List<WandCap> caps = new ArrayList<>(WandComponents.caps());
        assertEquals(6, caps.size());
        assertEquals(List.of("iron", "gold", "thaumium", "void", "copper", "silver"),
                caps.stream().map(WandCap::tag).toList(),
                "ConfigItems.java:287-290 registers iron/gold/thaumium/void, "
                        + "postInit (:314-322) then copper/silver");

        assertCap(caps.get(0), "iron", 1.1F, 1, 0);
        assertCap(caps.get(1), "gold", 1.0F, 3, 1);
        assertCap(caps.get(2), "thaumium", 0.9F, 6, 2);
        assertCap(caps.get(3), "void", 0.8F, 9, 7);
        assertCap(caps.get(4), "copper", 1.1F, 2, 3);
        assertCap(caps.get(5), "silver", 1.0F, 4, 4);
    }

    private static void assertCap(final WandCap cap, final String tag, final float base,
                                  final int craftCost, final int meta) {
        assertEquals(tag, cap.tag());
        assertEquals(base, cap.baseCostModifier(), tag + " base cost modifier");
        assertEquals(craftCost, cap.craftCost(), tag + " craft cost");
        assertEquals(meta, cap.meta(), tag + " original damage value");
        assertEquals("CAP_" + tag, cap.research(), tag + " research key");
        assertEquals("thaumcraft:models/wand_cap_" + tag, cap.texture().toString());
    }

    /** {@code WAND_CAP_COPPER} / {@code WAND_CAP_SILVER} carry special-aspect modifiers. */
    @Test
    void onlyCopperAndSilverHaveSpecialCostModifiers() {
        assertEquals(List.of("ordo", "perditio"),
                WandComponents.cap("copper").specialCostModifierAspectTags());
        assertEquals(1.0F, WandComponents.cap("copper").specialCostModifier());
        assertEquals(List.of("aer", "terra", "ignis", "aqua"),
                WandComponents.cap("silver").specialCostModifierAspectTags());
        assertEquals(0.95F, WandComponents.cap("silver").specialCostModifier());
        for (final String tag : List.of("iron", "gold", "thaumium", "void")) {
            assertTrue(WandComponents.cap(tag).specialCostModifierAspectTags().isEmpty(),
                    tag + " has no special aspects in the original");
            // The original never assigned the field in that constructor.
            assertEquals(0.0F, WandComponents.cap(tag).specialCostModifier(), tag);
            assertFalse(WandComponents.cap(tag).hasSpecialCostModifiers());
        }
        assertTrue(WandComponents.cap("silver").hasSpecialCostModifiers());
    }

    // ---------------------------------------------------------------- rods

    @Test
    void rodTableMatchesTheOriginalRegistrationBlock() {
        final List<WandRod> rods = new ArrayList<>(WandComponents.rods());
        assertEquals(18, rods.size(), "9 wand rods + 9 staff cores");
        assertEquals(List.of("wood", "greatwood", "obsidian", "blaze", "ice", "quartz", "bone",
                        "reed", "silverwood", "greatwood_staff", "obsidian_staff", "blaze_staff",
                        "ice_staff", "quartz_staff", "bone_staff", "reed_staff", "silverwood_staff",
                        "primal_staff"),
                rods.stream().map(WandRod::tag).toList(),
                "ConfigItems.java:291-310 registration order");
    }

    /** tag, capacity, craftCost, meta, primal-aspect (null = none), staff, glowing, runes. */
    @Test
    void everyRodCarriesItsOriginalCapacityCostAndFlag() {
        assertRod("wood", 25, 1, -1, null, false, false, false);
        assertRod("greatwood", 50, 3, 0, null, false, false, false);
        assertRod("obsidian", 75, 6, 1, "terra", false, false, false);
        assertRod("blaze", 75, 6, 6, "ignis", false, true, false);
        assertRod("ice", 75, 6, 3, "aqua", false, false, false);
        assertRod("quartz", 75, 6, 4, "ordo", false, false, false);
        assertRod("bone", 75, 6, 7, "perditio", false, false, false);
        assertRod("reed", 75, 6, 5, "aer", false, false, false);
        assertRod("silverwood", 100, 9, 2, null, false, false, false);
        assertRod("greatwood_staff", 125, 8, 50, null, true, false, false);
        assertRod("obsidian_staff", 175, 14, 51, "terra", true, false, false);
        assertRod("blaze_staff", 175, 14, 56, "ignis", true, true, false);
        assertRod("ice_staff", 175, 14, 53, "aqua", true, false, false);
        assertRod("quartz_staff", 175, 14, 54, "ordo", true, false, false);
        assertRod("bone_staff", 175, 14, 57, "perditio", true, false, false);
        assertRod("reed_staff", 175, 14, 55, "aer", true, false, false);
        assertRod("silverwood_staff", 250, 24, 52, null, true, false, false);
        assertRod("primal_staff", 250, 32, 100, null, true, false, true);
    }

    private static void assertRod(final String tag, final int capacity, final int craftCost,
                                  final int meta, final String primalAspect, final boolean staff,
                                  final boolean glowing, final boolean runes) {
        final WandRod rod = WandComponents.rod(tag);
        assertNotNull(rod, tag + " is not registered");
        assertEquals(capacity, rod.capacity(), tag + " capacity (ConfigItems.java)");
        assertEquals(craftCost, rod.craftCost(), tag + " craft cost");
        assertEquals(staff, rod.staff(), tag + " staff flag");
        assertEquals(glowing, rod.glowing(), tag + " glow flag");
        assertEquals(runes, rod.runes(), tag + " runes flag (STAFF_ROD_PRIMAL.setRunes)");
        assertEquals(primalAspect, rod.primalAspectTag(), tag + " on-update primal");
        assertEquals("ROD_" + tag, rod.research(), tag + " research key");
        if (meta >= 0) {
            assertEquals(meta, rod.meta(), tag + " original damage value");
            assertEquals("thaumcraft:wandrod", rod.itemId().toString());
        } else {
            assertEquals("minecraft:stick", rod.itemId().toString(),
                    "WAND_ROD_WOOD is registered on a plain stick (ConfigItems.java:291)");
        }
    }

    /** {@code StaffRod} derives its key and display tag by suffixing/stripping {@code _staff}. */
    @Test
    void staffCoresUseTheBaseTagForDisplayAndTexture() {
        assertEquals("greatwood", WandComponents.rod("greatwood_staff").baseTag());
        assertEquals("primal", WandComponents.rod("primal_staff").baseTag());
        assertEquals("thaumcraft:models/wand_rod_greatwood",
                WandComponents.rod("greatwood_staff").texture().toString());
        assertEquals("thaumcraft:models/wand_rod_greatwood",
                WandComponents.rod("greatwood").texture().toString());
        assertEquals("wood", WandComponents.rod("wood").baseTag());
    }

    /** Only the elemental rods and the primal staff core carry an on-update (WandRodPrimalOnUpdate). */
    @Test
    void onlyTheOriginalRodsHaveOnUpdate() {
        final List<String> withOnUpdate = new ArrayList<>();
        for (final WandRod rod : WandComponents.rods()) {
            if (rod.hasOnUpdate()) {
                withOnUpdate.add(rod.tag());
            }
        }
        assertEquals(List.of("obsidian", "blaze", "ice", "quartz", "bone", "reed",
                        "obsidian_staff", "blaze_staff", "ice_staff", "quartz_staff", "bone_staff",
                        "reed_staff", "primal_staff"),
                withOnUpdate);
        assertFalse(WandComponents.rod("silverwood").hasOnUpdate());
        assertTrue(WandComponents.rod("primal_staff").primalTickAllPrimals());
        assertFalse(WandComponents.rod("obsidian").primalTickAllPrimals());
    }

    // ------------------------------------------------------------ registry

    /**
     * The 16 registry entries back the 24 {@code wand_component} rows of
     * docs/content-mapping.csv: a rod row pair that shares one tag collapses
     * into one entry (the CSV's documented shared-id group).
     */
    @Test
    void componentRegistryCoversEveryOriginalComponentRow() {
        final List<WandComponent> components = WandComponents.components();
        assertEquals(16, components.size());
        assertEquals(List.of("iron", "gold", "thaumium", "void", "copper", "silver", "wood",
                        "greatwood", "obsidian", "blaze", "ice", "quartz", "bone", "reed",
                        "silverwood", "primal"),
                components.stream().map(WandComponent::tag).toList(),
                "docs/content-mapping.csv row order (:382-405)");
        int rows = 0;
        for (final WandComponent component : components) {
            rows += component.componentRowCount();
        }
        assertEquals(24, rows, "the CSV inventories 24 wand_component rows");
    }

    @Test
    void defaultsAreIronCapAndWoodRod() {
        assertEquals("iron", WandComponents.defaultCap().tag(),
                "getCap defaults to ConfigItems.WAND_CAP_IRON");
        assertEquals("wood", WandComponents.defaultRod().tag(),
                "getRod defaults to ConfigItems.WAND_ROD_WOOD");
        assertNull(WandComponents.cap("wood"), "wood is a rod tag, not a cap tag");
        assertNull(WandComponents.rod("gold"), "gold is a cap tag, not a rod tag");
        assertEquals(6, WandComponents.WAND_CAP_COUNT);
        assertEquals(18, WandComponents.WAND_ROD_COUNT);
    }
}
