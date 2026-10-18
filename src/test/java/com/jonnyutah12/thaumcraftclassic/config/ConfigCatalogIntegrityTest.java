package com.jonnyutah12.thaumcraftclassic.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural invariants of the config inventory itself: uniqueness, type
 * consistency, category coverage, the exact not-ported set, and the
 * read-time value corrections copied from the original config class.
 */
class ConfigCatalogIntegrityTest {

    @Test
    void optionPathsAreUniqueAndCategoriesAreCovered() {
        Set<String> paths = new HashSet<>();
        for (ConfigOption option : TcConfigCatalog.OPTIONS) {
            assertTrue(paths.add(option.path()), "duplicate option " + option.path());
            assertTrue(TcConfigCatalog.CATEGORY_ORDER.contains(option.category()),
                    "option " + option.path() + " uses a category outside CATEGORY_ORDER");
        }
        for (String category : TcConfigCatalog.CATEGORY_ORDER) {
            assertTrue(!TcConfigCatalog.optionsIn(category).isEmpty(),
                    "category " + category + " has no options — remove it or add the original's options");
        }
        assertEquals(TcConfigCatalog.OPTIONS.size(), paths.size());
    }

    @Test
    void defaultsMatchDeclaredTypesAndSourcesLookRight() {
        for (ConfigOption option : TcConfigCatalog.OPTIONS) {
            assertTrue(option.type().valueClass().isInstance(option.defaultValue()),
                    "default value for " + option.path() + " does not match its declared type");
            assertNotNull(option.comment(), "comment must be present (empty string when the original set none): " + option.path());
            assertTrue(option.source().matches("Config\\.java:\\d+"),
                    "source must reference the decompiled Config.java: " + option.path());
            if (option.ported()) {
                assertNull(option.notPortedReason(), "ported option must not carry a not-ported reason: " + option.path());
            } else {
                assertTrue(option.notPortedReason() != null && !option.notPortedReason().isBlank(),
                        "not-ported option must carry a reason: " + option.path());
            }
        }
    }

    @Test
    void notPortedSetIsExactlyTheNumericRegistryIdOptions() {
        // These nine exist only to assign numeric ids to biomes,
        // enchantments, villagers and the Outer Lands dimension — a concept
        // the target platform replaced with registry names. If anyone ports
        // or removes one, this guard forces a docs/config-reference.md update.
        Set<String> expected = Set.of(
                "general.thaumcraft_villager_id",
                "general.thaumcraft_banker_id",
                "Enchantments.ench_haste",
                "Enchantments.ench_repair",
                "Biomes.biome_taint",
                "Biomes.biome_magical_forest",
                "Biomes.biome_eerie",
                "Biomes.biome_eldritch",
                "Biomes.outer_lands_dim");
        Set<String> actual = new HashSet<>();
        for (ConfigOption option : TcConfigCatalog.OPTIONS) {
            if (!option.ported()) {
                actual.add(option.path());
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    void categoryCountsMatchTheOriginalConfigClass() {
        assertEquals(25, TcConfigCatalog.optionsIn("general").size());
        assertEquals(2, TcConfigCatalog.optionsIn("Enchantments").size());
        assertEquals(8, TcConfigCatalog.optionsIn("Monster_Spawning").size());
        assertEquals(2, TcConfigCatalog.optionsIn("Research").size());
        assertEquals(7, TcConfigCatalog.optionsIn("World_Generation").size());
        assertEquals(8, TcConfigCatalog.optionsIn("World_Regeneration").size());
        assertEquals(7, TcConfigCatalog.optionsIn("Biomes").size());
        assertEquals(3, TcConfigCatalog.optionsIn("Runic_Shielding").size());
        assertEquals(62, TcConfigCatalog.OPTIONS.size());
        assertEquals(53, TcConfigCatalog.portedOptions().size());
    }

    // ---- read-time corrections (original Config.java clamps) -----------------

    @Test
    void golemDelayClampsToOne() {
        assertEquals("golem_delay", TcConfigCatalog.GOLEM_DELAY.name());
        assertEquals(1, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_DELAY, 0));
        assertEquals(1, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_DELAY, -5));
        assertEquals(5, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_DELAY, 5));
        assertEquals(20, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_DELAY, 20));
    }

    @Test
    void golemIgnoreDelayClampsTo1000() {
        assertEquals(1000, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_IGNORE_DELAY, 999));
        assertEquals(1000, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_IGNORE_DELAY, 0));
        assertEquals(10000, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_IGNORE_DELAY, 10000));
    }

    @Test
    void golemLinkQualityBelowFourDisables() {
        // original: if (golemLinkQuality < 4) golemLinkQuality = 0;
        assertEquals(0, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_LINK_QUALITY, 3));
        assertEquals(0, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_LINK_QUALITY, -1));
        assertEquals(4, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_LINK_QUALITY, 4));
        assertEquals(16, ConfigNormalizers.normalizeInt(TcConfigCatalog.GOLEM_LINK_QUALITY, 16));
    }

    @Test
    void specialNodeRarityClampsToThree() {
        assertEquals(3, ConfigNormalizers.normalizeInt(TcConfigCatalog.SPECIAL_NODE_RARITY, 1));
        assertEquals(3, ConfigNormalizers.normalizeInt(TcConfigCatalog.SPECIAL_NODE_RARITY, 0));
        assertEquals(18, ConfigNormalizers.normalizeInt(TcConfigCatalog.SPECIAL_NODE_RARITY, 18));
    }

    @Test
    void runicShieldingClampsMatchMathMax() {
        assertEquals(500, ConfigNormalizers.normalizeInt(TcConfigCatalog.RUNIC_RECHARGE_SPEED, 499));
        assertEquals(2000, ConfigNormalizers.normalizeInt(TcConfigCatalog.RUNIC_RECHARGE_SPEED, 2000));
        assertEquals(0, ConfigNormalizers.normalizeInt(TcConfigCatalog.RUNIC_RECHARGE_DELAY, -10));
        assertEquals(80, ConfigNormalizers.normalizeInt(TcConfigCatalog.RUNIC_RECHARGE_DELAY, 80));
        assertEquals(0, ConfigNormalizers.normalizeInt(TcConfigCatalog.RUNIC_COST, -1));
        assertEquals(50, ConfigNormalizers.normalizeInt(TcConfigCatalog.RUNIC_COST, 50));
    }

    @Test
    void unclampedOptionsPassThrough() {
        assertEquals(-1, ConfigNormalizers.normalizeInt(TcConfigCatalog.RESEARCH_DIFFICULTY, -1));
        assertEquals(0, ConfigNormalizers.normalizeInt(TcConfigCatalog.BIOME_TAINT_SPREAD, 0));
        assertEquals(2, ConfigNormalizers.normalizeInt(TcConfigCatalog.TAINT_BIOME_WEIGHT, 2));
    }

    @Test
    void defaultsMatchTheOriginalConfigFile() {
        // Spot-check the values the original wrote into Thaumcraft.cfg on a
        // fresh install — these are the config-file defaults, not the Java
        // field initializers (syncConfigurable re-reads everything).
        assertEquals(true, TcConfigCatalog.ALLOW_MIRRORS.defaultValue());
        assertEquals(false, TcConfigCatalog.ALLOW_CHEAT_SHEET.defaultValue());
        assertEquals(false, TcConfigCatalog.DISPLAY_ASPECTS.defaultValue());
        assertEquals(true, TcConfigCatalog.SHADERS.defaultValue());
        assertEquals(5, TcConfigCatalog.GOLEM_DELAY.defaultValue());
        assertEquals(10000, TcConfigCatalog.GOLEM_IGNORE_DELAY.defaultValue());
        assertEquals(16, TcConfigCatalog.GOLEM_LINK_QUALITY.defaultValue());
        assertEquals(36, TcConfigCatalog.NODE_RARITY.defaultValue());
        assertEquals(18, TcConfigCatalog.SPECIAL_NODE_RARITY.defaultValue());
        assertEquals(5000, TcConfigCatalog.NOTIFICATION_DELAY.defaultValue());
        assertEquals(15, TcConfigCatalog.NOTIFICATION_MAX.defaultValue());
        assertEquals(200, TcConfigCatalog.BIOME_TAINT_SPREAD.defaultValue());
        assertEquals(2000, TcConfigCatalog.RUNIC_RECHARGE_SPEED.defaultValue());
        assertEquals(80, TcConfigCatalog.RUNIC_RECHARGE_DELAY.defaultValue());
        assertEquals(50, TcConfigCatalog.RUNIC_COST.defaultValue());
        assertEquals(0, TcConfigCatalog.RESEARCH_DIFFICULTY.defaultValue());
        assertEquals(100, TcConfigCatalog.ASPECT_TOTAL_CAP.defaultValue());
        assertEquals(2, TcConfigCatalog.TAINT_BIOME_WEIGHT.defaultValue());
        assertEquals(5, TcConfigCatalog.MAGICAL_FOREST_BIOME_WEIGHT.defaultValue());
        assertEquals("DEFAULT", TcConfigCatalog.REGEN_KEY.defaultValue());
        assertEquals("iron_door", TcConfigCatalog.PORTABLEHOLE_BLACKLIST.defaultValue());
        assertEquals(192, TcConfigCatalog.BIOME_TAINT.defaultValue());
        assertEquals(193, TcConfigCatalog.BIOME_MAGICAL_FOREST.defaultValue());
        assertEquals(194, TcConfigCatalog.BIOME_EERIE.defaultValue());
        assertEquals(195, TcConfigCatalog.BIOME_ELDRITCH.defaultValue());
        assertEquals(-42, TcConfigCatalog.OUTER_LANDS_DIM.defaultValue());
        assertEquals(150, TcConfigCatalog.ENCH_HASTE.defaultValue());
        assertEquals(151, TcConfigCatalog.ENCH_REPAIR.defaultValue());
        assertEquals(190, TcConfigCatalog.THAUMCRAFT_VILLAGER_ID.defaultValue());
        assertEquals(191, TcConfigCatalog.THAUMCRAFT_BANKER_ID.defaultValue());
    }

    @Test
    void everyCategorySectionOrderIsRespected() {
        List<ConfigOption> options = TcConfigCatalog.OPTIONS;
        int lastCategoryIndex = -1;
        for (ConfigOption option : options) {
            int categoryIndex = TcConfigCatalog.CATEGORY_ORDER.indexOf(option.category());
            assertTrue(categoryIndex >= lastCategoryIndex,
                    "OPTIONS must be ordered by CATEGORY_ORDER: " + option.path());
            lastCategoryIndex = categoryIndex;
        }
    }
}
