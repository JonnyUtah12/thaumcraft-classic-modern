package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-03-s acceptance: the twenty-one original {@code FocusUpgradeType}
 * entries, the ten per-rank upgrade trees, the four {@code canApplyUpgrade}
 * overrides and the original's rank rules for an applied upgrade list, all
 * against
 * {@code reference/decompiled/thaumcraft/thaumcraft/api/wands/FocusUpgradeType.java}
 * and the ten classes under {@code common/items/wands/foci/}.
 *
 * <p>Every literal below is transcribed from those lines, so a wrong id, lang
 * key, aspect cost or rank offer fails here rather than in game. The aspect
 * <em>tags</em> are the port's lowercased names for the original
 * {@code Aspect.WEAPON} … objects (docs/aspects.md); the test also asserts every
 * tag the upgrade table uses exists in the shipped aspect table, so a typo
 * cannot hide in a cost.</p>
 */
class FocusUpgradeTableTest {

    @Test
    void theUpgradeTableMatchesTheOriginal() {
        final List<FocusUpgradeType> all = FocusUpgrades.all();
        assertEquals(21, all.size(), "the original hands out ids 0..20 (dowsing grew types[20] to 21)");
        assertEquals(
                List.of("potency", "frugal", "treasure", "enlarge", "alchemistsfire", "alchemistsfrost",
                        "architect", "extend", "silktouch", "fireball", "firebeam", "scattershot",
                        "iceboulder", "batbombs", "devilbats", "nightshade", "seeker", "chainlightning",
                        "earthshock", "vampirebats", "dowsing"),
                all.stream().map(FocusUpgradeType::tag).toList(),
                "FocusUpgradeType.java:16-78 + the focus classes, in the original declaration order");

        for (final FocusUpgradeType type : all) {
            assertEquals(type.id(), all.indexOf(type), type.tag() + " must sit at its id's index");
            assertEquals("focus.upgrade." + type.tag() + ".name", type.nameKey());
            assertEquals("focus.upgrade." + type.tag() + ".text", type.textKey());
            assertEquals("focus.upgrade." + type.tag() + ".name", type.nameKey());
        }

        // The application prices (FocusUpgradeType.aspects), aspect tag -> amount.
        assertEquals(Map.of("telum", 1), costs("potency"));
        assertEquals(Map.of("fames", 1), costs("frugal"));
        assertEquals(Map.of("lucrum", 1), costs("treasure"));
        assertEquals(Map.of("iter", 1), costs("enlarge"));
        assertEquals(Map.of("potentia", 1, "limus", 1), costs("alchemistsfire"));
        assertEquals(Map.of("gelum", 1, "vinculum", 1), costs("alchemistsfrost"));
        assertEquals(Map.of("fabrico", 1), costs("architect"));
        assertEquals(Map.of("permutatio", 1), costs("extend"));
        assertEquals(Map.of("lucrum", 1), costs("silktouch"));
        assertEquals(Map.of("tenebrae", 1), costs("fireball"));
        assertEquals(Map.of("potentia", 1, "aer", 1), costs("firebeam"));
        assertEquals(Map.of("gelum", 1, "telum", 1), costs("scattershot"));
        assertEquals(Map.of("gelum", 1, "vitreus", 1), costs("iceboulder"));
        assertEquals(Map.of("potentia", 1, "vinculum", 1), costs("batbombs"));
        assertEquals(Map.of("tutamen", 1), costs("devilbats"));
        assertEquals(Map.of("victus", 1, "venenum", 1, "praecantatio", 1), costs("nightshade"));
        assertEquals(Map.of("sensus", 1, "cognitio", 1), costs("seeker"));
        assertEquals(Map.of("tempestas", 1), costs("chainlightning"));
        assertEquals(Map.of("tempestas", 1), costs("earthshock"));
        assertEquals(Map.of("fames", 1, "victus", 1), costs("vampirebats"));
        assertEquals(Map.of("perfodio", 1), costs("dowsing"));

        assertEquals(FocusUpgrades.VAMPIREBATS, FocusUpgrades.byTag("vampirebats"));
        assertEquals(FocusUpgrades.DOWSING, FocusUpgrades.byId(20));
        assertNull(FocusUpgrades.byId(21), "the original's types[] stops at 20");
        assertNull(FocusUpgrades.byTag("nope"));
    }

    @Test
    void everyUpgradeCostUsesAShippedAspectTag() {
        final Set<String> shipped = AspectDefinitions.all().stream()
                .map(AspectDefinitions.Definition::tag).collect(Collectors.toSet());
        for (final FocusUpgradeType type : FocusUpgrades.all()) {
            for (final Focus.Cost cost : type.aspectCost()) {
                assertTrue(shipped.contains(cost.aspectTag()),
                        type.tag() + " names aspect " + cost.aspectTag() + ", which the aspect table does not define");
            }
        }
    }

    @Test
    void theRankTreesMatchTheOriginal() {
        final Map<String, List<List<String>>> expected = new LinkedHashMap<>();
        expected.put("fire", List.of(
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "alchemistsfire"),
                List.of("frugal", "potency", "fireball", "firebeam"),
                List.of("frugal", "potency", "alchemistsfire"),
                List.of("frugal", "potency")));
        expected.put("shock", List.of(
                List.of("frugal", "potency"),
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "chainlightning", "earthshock"),
                List.of("frugal", "potency", "enlarge"),
                List.of("frugal", "potency", "enlarge")));
        expected.put("hellbat", List.of(
                List.of("frugal", "potency"),
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "batbombs", "devilbats"),
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "vampirebats")));
        expected.put("frost", List.of(
                List.of("frugal", "potency", "alchemistsfrost"),
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "scattershot", "iceboulder", "alchemistsfrost"),
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "alchemistsfrost")));
        expected.put("trade", List.of(
                List.of("frugal", "enlarge"),
                List.of("frugal", "enlarge"),
                List.of("frugal", "enlarge", "treasure", "architect"),
                List.of("frugal", "enlarge"),
                List.of("frugal", "enlarge", "silktouch")));
        expected.put("excavation", List.of(
                List.of("frugal", "potency", "treasure"),
                List.of("frugal", "potency", "enlarge"),
                List.of("frugal", "potency", "treasure", "dowsing"),
                List.of("frugal", "potency", "enlarge"),
                List.of("frugal", "potency", "treasure", "silktouch")));
        final List<String> hole = List.of("frugal", "enlarge", "extend");
        expected.put("portablehole", List.of(hole, hole, hole, hole, hole));
        expected.put("pech", List.of(
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "extend"),
                List.of("frugal", "potency"),
                List.of("frugal", "potency", "extend"),
                List.of("frugal", "potency", "nightshade")));
        expected.put("warding", List.of(
                List.of("frugal"),
                List.of("frugal", "architect"),
                List.of("frugal", "enlarge"),
                List.of("frugal", "enlarge"),
                List.of("frugal", "enlarge")));
        expected.put("primal", List.of(
                List.of("frugal"),
                List.of("frugal"),
                List.of("frugal", "seeker"),
                List.of("frugal"),
                List.of("frugal")));

        assertEquals(Focuses.tags().stream().sorted().toList(), expected.keySet().stream().sorted().toList());
        for (final Focus focus : Focuses.all()) {
            final List<List<String>> ranks = expected.get(focus.tag());
            for (int rank = 1; rank <= FocusUpgradeList.RANKS; rank++) {
                assertEquals(ranks.get(rank - 1),
                        FocusUpgradeTrees.byRank(focus.tag(), rank).stream()
                                .map(FocusUpgradeType::tag).toList(),
                        focus.tag() + " rank " + rank);
            }
        }
        assertNull(FocusUpgradeTrees.byRank("fire", 0), "the original answered null outside 1..5");
        assertNull(FocusUpgradeTrees.byRank("fire", 6));
        assertNull(FocusUpgradeTrees.byRank("nosuchfocus", 1));
        assertTrue(FocusUpgradeTrees.offers("frost", 3, FocusUpgrades.ALCHEMISTSFROST));
        assertFalse(FocusUpgradeTrees.offers("frost", 2, FocusUpgrades.ALCHEMISTSFROST));
    }

    @Test
    void anAppliedUpgradeListFollowsTheOriginalRankRules() {
        assertEquals(5, FocusUpgradeList.RANKS, "the original was a short[5]");
        assertEquals(5, FocusUpgradeList.NONE.ranks().size());
        assertTrue(FocusUpgradeList.NONE.applied().isEmpty());
        assertEquals(List.of(-1, -1, -1, -1, -1), FocusUpgradeList.NONE.ranks());
        assertTrue(FocusUpgradeList.NONE.rankFree(1));
        assertTrue(FocusUpgradeList.NONE.rankFree(5));
        assertFalse(FocusUpgradeList.NONE.rankFree(0), "ranks are 1-based");
        assertFalse(FocusUpgradeList.NONE.rankFree(6));

        // applyUpgrade: the type lands in its own rank's slot, only when free.
        final FocusUpgradeList one = FocusUpgradeList.NONE.withUpgrade(FocusUpgrades.FIREBALL, 3)
                .orElseThrow();
        assertEquals(3, one.applied().size() + 2, "rank 3 holds fireball");
        assertEquals(1, one.level(FocusUpgrades.FIREBALL));
        assertTrue(one.isUpgradedWith(FocusUpgrades.FIREBALL));
        assertEquals(FocusUpgrades.FIREBALL, one.at(3));
        assertNull(one.at(2), "rank 2 is still empty");
        assertEquals(List.of(FocusUpgrades.FIREBALL.id()), one.applied(),
                "applied() reports the upgrade id the original's types[] indexed, not the rank");
        assertTrue(one.withUpgrade(FocusUpgrades.POTENCY, 3).isEmpty(),
                "applyUpgrade refused an occupied rank (ItemFocusBasic.java:145)");
        assertTrue(one.withUpgrade(FocusUpgrades.POTENCY, 6).isEmpty(), "and a rank outside 1..5");
        assertTrue(one.withUpgrade(FocusUpgrades.POTENCY, 1).isPresent());

        // Two ranks of one upgrade: getUpgradeLevel counts them (frugal/potency trees allow it).
        final FocusUpgradeList two = one.withUpgrade(FocusUpgrades.FIREBALL, 1).orElseThrow();
        assertEquals(2, two.level(FocusUpgrades.FIREBALL));
        assertEquals(List.of(FocusUpgrades.FIREBALL.id(), FocusUpgrades.FIREBALL.id()), two.applied(),
                "applied() reports the upgrade ids the original's types[] indexed, in rank order");
        assertEquals(List.of(FocusUpgrades.FIREBALL, FocusUpgrades.FIREBALL), two.appliedTypes());
        assertEquals(1, two.cleared(1).level(FocusUpgrades.FIREBALL));
        // The original's pad/truncate shape (getAppliedUpgrades, ItemFocusBasic.java:129-141)
        assertEquals(List.of(7, -1, -1, -1, -1), FocusUpgradeList.of(List.of(7)).ranks());
        assertEquals(List.of(1, 2, 3, 4, 5), FocusUpgradeList.of(1, 2, 3, 4, 5, 6).applied(),
                "only the first five ids of a longer list land");
        assertEquals(List.of(9, 9, 9, 9, 8),
                FocusUpgradeList.of(9, 9, 9, 9).withUpgrade(FocusUpgrades.byId(8), 5).orElseThrow().ranks());
        assertThrowsForBadRanks();
    }

    private void assertThrowsForBadRanks() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FocusUpgradeList.of(List.of(-2, -1, -1, -1, -1)), "no id below -1");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FocusUpgradeList(List.of(1, 2, 3)), "the record is exactly five ranks");
    }

    @Test
    void canApplyUpgradeFollowsTheOriginalsFourOverrides() {
        final FocusUpgradeList none = FocusUpgradeList.NONE;

        // fire: alchemistsfire is refused once fireball and alchemistsfire are both applied.
        assertTrue(FocusUpgradeTrees.canApply("fire", none, FocusUpgrades.ALCHEMISTSFIRE, 2, false));
        final FocusUpgradeList fireballAndFire = FocusUpgradeList.NONE
                .withUpgrade(FocusUpgrades.FIREBALL, 3).orElseThrow()
                .withUpgrade(FocusUpgrades.ALCHEMISTSFIRE, 2).orElseThrow();
        assertFalse(FocusUpgradeTrees.canApply("fire", fireballAndFire, FocusUpgrades.ALCHEMISTSFIRE, 4, false));
        assertTrue(FocusUpgradeTrees.canApply("fire", fireballAndFire, FocusUpgrades.POTENCY, 1, false));

        // shock: enlarge needs chainlightning or earthshock.
        assertFalse(FocusUpgradeTrees.canApply("shock", none, FocusUpgrades.ENLARGE, 4, false));
        final FocusUpgradeList chained = FocusUpgradeList.NONE
                .withUpgrade(FocusUpgrades.CHAINLIGHTNING, 3).orElseThrow();
        assertTrue(FocusUpgradeTrees.canApply("shock", chained, FocusUpgrades.ENLARGE, 4, false));

        // warding: enlarge needs architect.
        assertFalse(FocusUpgradeTrees.canApply("warding", none, FocusUpgrades.ENLARGE, 3, false));
        final FocusUpgradeList architect = FocusUpgradeList.NONE
                .withUpgrade(FocusUpgrades.ARCHITECT, 2).orElseThrow();
        assertTrue(FocusUpgradeTrees.canApply("warding", architect, FocusUpgrades.ENLARGE, 3, false));

        // hellbat: vampirebats needs the VAMPBAT research entry.
        assertFalse(FocusUpgradeTrees.canApply("hellbat", none, FocusUpgrades.VAMPIREBATS, 5, false));
        assertTrue(FocusUpgradeTrees.canApply("hellbat", none, FocusUpgrades.VAMPIREBATS, 5, true));

        // Everything else inherits true, and the rank must offer the type at all.
        assertTrue(FocusUpgradeTrees.canApply("pech", none, FocusUpgrades.NIGHTSHADE, 5, false));
        assertFalse(FocusUpgradeTrees.canApply("pech", none, FocusUpgrades.NIGHTSHADE, 4, false));
        assertFalse(FocusUpgradeTrees.canApply("pech", none, null, 5, false));
    }

    private static Map<String, Integer> costs(final String tag) {
        final FocusUpgradeType type = FocusUpgrades.byTag(tag);
        assertNotNull(type, "the upgrade table has no '" + tag + "' entry");
        final Map<String, Integer> out = new LinkedHashMap<>();
        final List<String> tags = new ArrayList<>();
        for (final Focus.Cost cost : type.aspectCost()) {
            assertNull(out.put(cost.aspectTag(), cost.amount()), "duplicate aspect in " + tag);
            tags.add(cost.aspectTag());
        }
        assertEquals(tags.stream().distinct().count(), tags.size(), tag + " repeats an aspect");
        return out;
    }
}
