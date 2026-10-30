package com.jonnyutah12.thaumcraftclassic.items.golems;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The golem item families' pure mappings and their shipped labels: the six
 * upgrades in primal order, the twelve animation cores plus the blank core,
 * the eight accessories' decoration characters, and the per-meta names the
 * original's {@code en_US.lang} ships for all of them.
 *
 * <p>World-free on purpose: every fact here comes from a class constant or the
 * shipped {@code en_us.json}, so a wrong index or a renamed variant fails
 * without a game test server.</p>
 */
class GolemItemFactsTest {

    /** {@code icon[6]} in the original — Air, Earth, Fire, Water, Order, Entropy. */
    @Test
    void theSixUpgradesAreThePrimalOrder() {
        assertEquals(6, ItemGolemUpgrade.META_COUNT, "the original's icon array is icon[6]");
        assertEquals(
                List.of("Golem Upgrade: Air", "Golem Upgrade: Earth", "Golem Upgrade: Fire",
                        "Golem Upgrade: Water", "Golem Upgrade: Order", "Golem Upgrade: Entropy"),
                List.of(0, 1, 2, 3, 4, 5).stream().map(m -> lang().get("item.thaumcraft.itemgolemupgrade." + m).getAsString()).toList(),
                "the six upgrade names are the original item.ItemGolemUpgrade.<meta>.name strings, in primal order");
        assertEquals(
                List.of("Improves Speed", "Improves Strength", "Improves Damage, Capacity",
                        "Improves Senses, Range", "Improves Organization, Intellect",
                        "Improves Defenses, Reasoning"),
                List.of(0, 1, 2, 3, 4, 5).stream()
                        .map(m -> ItemGolemUpgrade.descriptionKey(m))
                        .map(k -> lang().get(k).getAsString()).toList(),
                "the description lines are the original item.ItemGolemUpgrade.<meta>.desc strings");
        assertEquals("item.thaumcraft.itemgolemupgrade.3.desc", ItemGolemUpgrade.descriptionKey(3));
    }

    /** {@code icon[12]} plus the blank core at meta 100. */
    @Test
    void theCoresAreTwelvePlusTheBlank() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 100),
                ItemGolemCore.VALID_METAS.stream().sorted().toList(),
                "the original's cores are metas 0..11 plus the blank core at 100");
        assertEquals(100, ItemGolemCore.BLANK_META);
        assertEquals(
                List.of("Fill", "Empty", "Gather", "Harvest", "Guard", "Decanting", "Alchemy", "Chop",
                        "Use", "Butcher", "Sorting", "Fishing"),
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).stream()
                        .map(m -> lang().get("item.thaumcraft.itemgolemcore." + m).getAsString()).toList(),
                "core names are the original item.ItemGolemCore.<meta>.name strings");
        assertEquals("Blank", lang().get("item.thaumcraft.itemgolemcore.100").getAsString());
    }

    /** The original's three static core predicates, as exact sets. */
    @Test
    void corePredicatesMatchTheOriginalSwitchSets() {
        assertEquals(List.of(0, 1, 2, 4, 5, 8, 10), trueFor(ItemGolemCore::hasGUI));
        assertEquals(List.of(0, 1, 2, 8, 10), trueFor(ItemGolemCore::canSort));
        assertEquals(List.of(0, 1, 2, 5, 8), trueFor(ItemGolemCore::hasInventory));
        // The negative side has to be real too: the blank core is never any of them.
        for (final int core : List.of(3, 6, 7, 9, 11, 100)) {
            assertTrue(!ItemGolemCore.hasGUI(core) && !ItemGolemCore.canSort(core)
                            && !ItemGolemCore.hasInventory(core),
                    "core " + core + " appears in none of the original's three switch sets");
        }
    }

    private static List<Integer> trueFor(final java.util.function.IntPredicate predicate) {
        return java.util.stream.IntStream.rangeClosed(0, 11).filter(predicate).boxed().toList();
    }

    /** {@code ItemGolemDecoration.getDecoChar} — H,G,B,F,R,V,P,M for metas 0..7. */
    @Test
    void decorationCharactersAreTheOriginalMapping() {
        assertEquals(List.of("H", "G", "B", "F", "R", "V", "P", "M"),
                List.of(0, 1, 2, 3, 4, 5, 6, 7).stream().map(ItemGolemDecoration::getDecoChar).toList(),
                "the original getDecoChar maps 0..7 to H,G,B,F,R,V,P,M");
        assertEquals("", ItemGolemDecoration.getDecoChar(8), "anything outside 0..7 has no character");
        assertEquals("", ItemGolemDecoration.getDecoChar(-1));
        // The two characters the load-bearing attribute bonuses read.
        assertEquals("H", ItemGolemDecoration.getDecoChar(0), "H is the top hat (+5 max health in setupGolem)");
        assertEquals("M", ItemGolemDecoration.getDecoChar(7), "M is the mace arm (+2 damage in setupGolem)");
    }

    /** Every per-meta name the four multi-meta golem items emit is shipped with the original wording. */
    @Test
    void everyPerMetaNameIsShippedWithTheOriginalWording() {
        final List<String> expectedPlacers = List.of("Straw Golem", "Wood Golem", "Tallow Golem", "Clay Golem",
                "Flesh Golem", "Stone Golem", "Iron Golem", "Thaumium Golem");
        assertEquals(8, ItemGolemPlacer.META_COUNT);
        for (int meta = 0; meta < 8; meta++) {
            assertEquals(expectedPlacers.get(meta),
                    lang().get("item.thaumcraft.itemgolemplacer." + meta).getAsString(),
                    "original item.ItemGolemPlacer." + meta + ".name");
        }
        assertEquals(List.of("Top Hat", "Spectacles", "Bowtie", "Fez", "Dart Launcher", "Visor",
                        "Iron Plating", "Mace Arm"),
                List.of(0, 1, 2, 3, 4, 5, 6, 7).stream()
                        .map(m -> lang().get("item.thaumcraft.itemgolemdecoration." + m).getAsString()).toList(),
                "original item.ItemGolemDecoration.<meta>.name");
        assertEquals(8, ItemGolemDecoration.META_COUNT);

        // Base keys: the family labels the original ships for the core's own
        // name and the accessory's prefix.
        assertEquals("Golem Animation Core", lang().get("item.thaumcraft.itemgolemcore").getAsString());
        assertEquals("Accessory", lang().get("item.thaumcraft.itemgolemdecoration").getAsString());
        assertEquals("Golemancer's Bell", lang().get("item.thaumcraft.golembell").getAsString());
        assertEquals("Golem", lang().get("entity.thaumcraft.golem").getAsString());
    }

    /**
     * The original's wildcard metadata {@code 32767} (the ADVANCEDGOLEM
     * research icon's {@code ItemGolemPlacer} stack) is not a placeable
     * variant — {@code EnumGolemType.getType} returns null for it, exactly as
     * the original does.
     */
    @Test
    void theWildcardMetaIsNotAPlaceableVariant() {
        assertEquals(32767, ItemGolemPlacer.META_ANY);
        assertTrue(com.jonnyutah12.thaumcraftclassic.entities.golems.EnumGolemType.getType(ItemGolemPlacer.META_ANY) == null);
    }

    private static JsonObject lang() {
        try (InputStream in = GolemItemFactsTest.class.getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
