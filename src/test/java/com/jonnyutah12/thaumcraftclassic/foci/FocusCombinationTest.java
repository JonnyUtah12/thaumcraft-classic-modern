package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgradeType;
import com.jonnyutah12.thaumcraftclassic.api.foci.FocusUpgrades;
import com.jonnyutah12.thaumcraftclassic.casting.FocusSpellSpecs;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-03-s acceptance: the original combination effects — which applied
 * upgrade selects which price, cooldown and per-tick flag, in the original's
 * own <em>check order</em>.
 *
 * <p>Sources: the {@code getVisCost} / {@code getActivationCooldown} /
 * {@code onFocusRightClick} / {@code onUsingFocusTick} bodies of the ten classes
 * under {@code reference/decompiled/.../common/items/wands/foci/}, cited on each
 * expectation. The chain order is load-bearing (frost with both scattershot and
 * iceboulder prices as scattershot; excavation checks silktouch before dowsing),
 * so it is asserted directly.</p>
 */
class FocusCombinationTest {

    @Test
    void theCombinationPricesMatchTheOriginal() {
        // fire — ItemFocusFire.java:23-25,62 (beam checked first, then ball)
        assertEquals(Map.of("ignis", 10, "ordo", 3), cost("fire", FocusUpgrades.FIREBEAM));
        assertEquals(Map.of("ignis", 66, "perditio", 33), cost("fire", FocusUpgrades.FIREBALL));
        assertEquals(Map.of("ignis", 10), cost("fire", null));
        // shock — ItemFocusShock.java:35-37,73-74
        assertEquals(Map.of("aer", 40, "aqua", 10), cost("shock", FocusUpgrades.CHAINLIGHTNING));
        assertEquals(Map.of("aer", 75, "terra", 25), cost("shock", FocusUpgrades.EARTHSHOCK));
        assertEquals(Map.of("aer", 25), cost("shock", null));
        // hellbat — ItemFocusHellbat.java:29-31,144-145
        assertEquals(Map.of("ignis", 100, "perditio", 200, "aer", 100), cost("hellbat", FocusUpgrades.BATBOMBS));
        assertEquals(Map.of("ignis", 100, "perditio", 100, "aer", 100, "terra", 100),
                cost("hellbat", FocusUpgrades.DEVILBATS));
        assertEquals(Map.of("ignis", 200, "perditio", 100, "aer", 100), cost("hellbat", null));
        // frost — ItemFocusFrost.java:20-22,93-94
        assertEquals(Map.of("aqua", 20, "ignis", 2, "perditio", 2, "aer", 5),
                cost("frost", FocusUpgrades.SCATTERSHOT));
        assertEquals(Map.of("aqua", 20, "ignis", 2, "perditio", 2, "terra", 5),
                cost("frost", FocusUpgrades.ICEBOULDER));
        assertEquals(Map.of("aqua", 5, "ignis", 2, "perditio", 2), cost("frost", null));
        // trade — ItemFocusTrade.java:36,222-235: cost + one of every primal
        assertEquals(Map.of("perditio", 6, "terra", 6, "ordo", 6, "aer", 1, "ignis", 1, "aqua", 1),
                cost("trade", FocusUpgrades.SILKTOUCH));
        assertEquals(Map.of("perditio", 5, "terra", 5, "ordo", 5), cost("trade", null));
        // excavation — ItemFocusExcavation.java:36,72-95
        assertEquals(Map.of("terra", 16, "aer", 1, "ignis", 1, "aqua", 1, "ordo", 1, "perditio", 1),
                cost("excavation", FocusUpgrades.SILKTOUCH));
        assertEquals(Map.of("ignis", 2, "ordo", 2, "terra", 15), cost("excavation", FocusUpgrades.DOWSING));
        assertEquals(Map.of("terra", 15), cost("excavation", null));
        // portablehole / warding have no combination price (ItemFocusPortableHole.java:55, ItemFocusWarding.java:81)
        assertEquals(Map.of("perditio", 10, "aer", 10), cost("portablehole", FocusUpgrades.EXTEND));
        assertEquals(Map.of("perditio", 10, "aer", 10), cost("portablehole", null));
        assertEquals(Map.of("terra", 25, "ordo", 25, "aqua", 10), cost("warding", FocusUpgrades.ENLARGE));
        // pech — ItemFocusPech.java:22-29,84-85: nightshade prices all six primals at 10
        assertEquals(Map.of("aer", 10, "ignis", 10, "terra", 10, "ordo", 10, "perditio", 10, "aqua", 10),
                cost("pech", FocusUpgrades.NIGHTSHADE));
        assertEquals(Map.of("terra", 10, "perditio", 10, "aqua", 10), cost("pech", null));
        // primal rolls its price per cast (ItemFocusPrimal.java:75-84), so both combinations declare none
        assertEquals(Map.of(), cost("primal", FocusUpgrades.SEEKER));
        assertEquals(Map.of(), cost("primal", null));
    }

    @Test
    void theCombinationCooldownsMatchTheOriginal() {
        // ItemFocusFire.java:66-69 (fireball 1000, otherwise 0), ItemFocusShock.java:77-80,
        // ItemFocusFrost.java:97-100 (200 until scattershot/iceboulder, then 500),
        // ItemFocusHellbat.java:148-151 (always 1000), the rest fixed.
        assertEquals(1000, cooldown("fire", FocusUpgrades.FIREBALL));
        assertEquals(0, cooldown("fire", FocusUpgrades.FIREBEAM));
        assertEquals(0, cooldown("fire", null));
        assertEquals(500, cooldown("shock", FocusUpgrades.CHAINLIGHTNING));
        assertEquals(1000, cooldown("shock", FocusUpgrades.EARTHSHOCK));
        assertEquals(250, cooldown("shock", null));
        assertEquals(500, cooldown("frost", FocusUpgrades.SCATTERSHOT));
        assertEquals(500, cooldown("frost", FocusUpgrades.ICEBOULDER));
        assertEquals(200, cooldown("frost", null));
        assertEquals(1000, cooldown("hellbat", FocusUpgrades.BATBOMBS));
        assertEquals(1000, cooldown("hellbat", null));
        assertEquals(250, cooldown("pech", FocusUpgrades.NIGHTSHADE));
        assertEquals(500, cooldown("primal", FocusUpgrades.SEEKER));
        assertEquals(0, cooldown("warding", FocusUpgrades.ENLARGE));
        assertEquals(0, cooldown("trade", FocusUpgrades.SILKTOUCH));
        assertEquals(0, cooldown("portablehole", FocusUpgrades.EXTEND));
        assertEquals(0, cooldown("excavation", FocusUpgrades.SILKTOUCH));
    }

    @Test
    void theUncombinedVariantKeepsTheParentSlicesValues() {
        // The parent slice's Focus table carries the original's no-upgrade values;
        // the uncombined combination must agree with it for every focus.
        for (final Focus focus : Focuses.all()) {
            final FocusCombinations.Combination base = FocusCombinations.resolve(focus.tag(), FocusUpgradeList.NONE);
            assertNotNull(base, focus.tag() + " has no uncombined variant");
            assertEquals(FocusCombinations.BASE, base.id());
            assertTrue(base.base());
            assertEquals(focus.activationCooldown(), base.activationCooldown(),
                    focus.tag() + " uncombined cooldown vs the parent table");
            assertTrue(base.selectedBy(FocusUpgradeList.NONE));
        }
    }

    @Test
    void theChainOrderIsTheOriginals() {
        // Both combinations applied: the original's first check wins.
        assertEquals("scattershot", variant("frost", FocusUpgrades.ICEBOULDER, FocusUpgrades.SCATTERSHOT));
        assertEquals("firebeam", variant("fire", FocusUpgrades.FIREBALL, FocusUpgrades.FIREBEAM));
        assertEquals("silktouch", variant("excavation", FocusUpgrades.DOWSING, FocusUpgrades.SILKTOUCH));
        assertEquals("chainlightning", variant("shock", FocusUpgrades.EARTHSHOCK, FocusUpgrades.CHAINLIGHTNING));
        assertEquals("batbombs", variant("hellbat", FocusUpgrades.DEVILBATS, FocusUpgrades.BATBOMBS));
        assertEquals("nightshade", variant("pech", FocusUpgrades.NIGHTSHADE));
        assertEquals("seeker", variant("primal", FocusUpgrades.SEEKER));
        assertEquals("iceboulder", variant("frost", FocusUpgrades.ICEBOULDER));
        // An upgrade that is not a combination for the focus does not change the variant.
        assertEquals("base", variant("warding", FocusUpgrades.ENLARGE));
        assertEquals("base", variant("primal", FocusUpgrades.FRUGAL));
        assertEquals("base", variant("fire", FocusUpgrades.FRUGAL, FocusUpgrades.POTENCY));
        assertNull(FocusCombinations.resolve("nosuchfocus", FocusUpgradeList.NONE));
        assertNull(FocusCombinations.variantId("nosuchfocus", FocusUpgradeList.NONE));
    }

    @Test
    void channeledCombinationsAreThePerTickBodies() {
        // onUsingFocusTick vs onFocusRightClick: fire's base/beam stream and shock's bolt are
        // per-tick (ItemFocusFire.java:104, ItemFocusShock.java:136), their ball/orb
        // combinations are right-click (lines 82, 114), excavation is per-tick
        // (ItemFocusExcavation.java:109), and frost/pech/primal/hellbat/trade/warding/
        // portablehole all fire on the right-click.
        assertTrue(channeled("fire", null));
        assertTrue(channeled("fire", FocusUpgrades.FIREBEAM));
        assertFalse(channeled("fire", FocusUpgrades.FIREBALL));
        assertTrue(channeled("shock", null));
        assertTrue(channeled("shock", FocusUpgrades.CHAINLIGHTNING));
        assertFalse(channeled("shock", FocusUpgrades.EARTHSHOCK));
        assertTrue(channeled("excavation", null));
        assertTrue(channeled("excavation", FocusUpgrades.DOWSING));
        assertTrue(channeled("excavation", FocusUpgrades.SILKTOUCH));
        for (final String tag : List.of("frost", "pech", "primal", "hellbat", "trade", "warding", "portablehole")) {
            assertFalse(FocusCombinations.channeled(tag, FocusUpgradeList.NONE), tag + " is not a channel");
        }
        assertFalse(channeled("nosuchfocus", null));
    }

    @Test
    void everyCombinationHasItsPortEffect() {
        int combinations = 0;
        for (final String tag : FocusCombinations.tags()) {
            for (final FocusCombinations.Combination combination : FocusCombinations.variants(tag)) {
                assertTrue(FocusSpellSpecs.has(tag, combination.id()),
                        tag + "/" + combination.id() + " is priced but has no ported effect");
                combinations++;
            }
        }
        assertEquals(23, combinations, "10 uncombined variants + 13 combinations");
        for (final String tag : Focuses.tags()) {
            for (final String id : FocusSpellSpecs.variantIds(tag)) {
                assertTrue(FocusCombinations.variants(tag).stream().anyMatch(c -> c.id().equals(id)),
                        id + " has an effect but no combination");
            }
        }
        assertEquals(FocusCombinations.BASE,
                FocusCombinations.variants("warding").get(FocusCombinations.variants("warding").size() - 1).id(),
                "the uncombined variant is always last");
    }

    // ------------------------------------------------------------------ helpers

    private static String variant(final String tag, final FocusUpgradeType... applied) {
        return FocusCombinations.variantId(tag, list(applied));
    }

    private static boolean channeled(final String tag, final FocusUpgradeType type) {
        return FocusCombinations.channeled(tag, type == null ? FocusUpgradeList.NONE : list(type));
    }

    private static int cooldown(final String tag, final FocusUpgradeType type) {
        return FocusCombinations.cooldown(tag, type == null ? FocusUpgradeList.NONE : list(type));
    }

    private static Map<String, Integer> cost(final String tag, final FocusUpgradeType type) {
        final FocusCombinations.Combination combination = FocusCombinations.resolve(tag,
                type == null ? FocusUpgradeList.NONE : list(type));
        assertNotNull(combination, "no combination for " + tag);
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (final Focus.Cost cost : combination.cost()) {
            assertEquals(null, out.put(cost.aspectTag(), cost.amount()), "duplicate aspect in " + tag);
        }
        return out;
    }

    /** Puts each upgrade in its own rank slot, in tree order; a duplicate tag takes the next rank. */
    private static FocusUpgradeList list(final FocusUpgradeType... types) {
        FocusUpgradeList out = FocusUpgradeList.NONE;
        for (int rank = 1; rank <= types.length; rank++) {
            out = out.withUpgrade(types[rank - 1], rank).orElseThrow();
        }
        return out;
    }

    private static void assertNull(final Object value) {
        org.junit.jupiter.api.Assertions.assertNull(value);
    }
}
