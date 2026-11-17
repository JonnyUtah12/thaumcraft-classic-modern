package com.jonnyutah12.thaumcraftclassic.foci;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-02 acceptance: the focus table against the original focus classes,
 * value for value, and against docs/content-mapping.csv.
 *
 * <p>Source of truth: the ten classes under
 * {@code reference/decompiled/thaumcraft/thaumcraft/common/items/wands/foci/}
 * of the decompiled Thaumcraft 4.2.3.5 tree, plus the registration block of
 * {@code common/config/ConfigItems.java:335-354}. Every literal below is
 * transcribed from those lines, so a wrong price, colour, cooldown or sorting
 * key fails here rather than in game.</p>
 *
 * <p>What is <em>not</em> here, and why: anything that needs the live aspect or
 * item registry. {@link Focus#cost()} resolves aspect tags through
 * {@code Aspect.getAspect}, which is empty before the mod's registries run, so
 * the declared price is asserted on {@link Focus#baseCost()} (tags + amounts)
 * here and resolved in {@code gametest/FociGameTest}. The upgrade-dependent
 * prices and cooldowns are TC-M6-03-s and are asserted <em>absent</em> by
 * pinning the no-upgrade values.</p>
 */
class FociParityTest {

    /** The focus tags in original registration order == CSV row order. */
    private static final List<String> CSV_ORDER =
            List.of("fire", "shock", "hellbat", "frost", "trade",
                    "excavation", "portablehole", "pech", "warding", "primal");

    @Test
    void focusTableMatchesTheOriginalFociClasses() {
        final List<Focus> foci = Focuses.all();
        assertEquals(10, foci.size(), "ConfigItems.java:335-354 registers ten focus items");
        assertEquals(CSV_ORDER, foci.stream().map(Focus::tag).toList());

        // ItemFocusFire.java:23,52,57,67,72 — costBase, "AF", 15028484, 0 (no fireball), true
        assertFocus("fire", "FOCUSFIRE", "AF", 15028484, 0, true,
                Map.of("ignis", 10));
        // ItemFocusShock.java:35,63,68,78 — costBase aer 25, "BL", 10466239, 250 (no upgrades)
        assertFocus("shock", "FOCUSSHOCK", "BL", 10466239, 250, false,
                Map.of("aer", 25));
        // ItemFocusHellbat.java:29,59,139,149 — costBase, "HH", 14431746, 1000
        assertFocus("hellbat", "FOCUSHELLBAT", "HH", 14431746, 1000, false,
                orderedCost("ignis", 200, "perditio", 100, "aer", 100));
        // ItemFocusFrost.java:20,43,88,98 — costBase aqua 5 ignis 2 perditio 2, "BF", 5204428, 200
        assertFocus("frost", "FOCUSFROST", "BF", 5204428, 200, false,
                orderedCost("aqua", 5, "ignis", 2, "perditio", 2));
        // ItemFocusTrade.java:36,45,217 — cost perditio 5 terra 5 ordo 5, "BT", 8747923, no cooldown
        assertFocus("trade", "FOCUSTRADE", "BT", 8747923, 0, false,
                orderedCost("perditio", 5, "terra", 5, "ordo", 5));
        // ItemFocusExcavation.java:36,62,67,99 — cost terra 15, "BE", 409606, true
        assertFocus("excavation", "FOCUSEXCAVATION", "BE", 409606, 0, true,
                Map.of("terra", 15));
        // ItemFocusPortableHole.java:27,34,50 — cost perditio 10 aer 10, "BPH", 594985
        assertFocus("portablehole", "FOCUSPORTABLEHOLE", "BPH", 594985, 0, false,
                orderedCost("perditio", 10, "aer", 10));
        // ItemFocusPech.java:22,43,59,79 — cost terra 10 perditio 10 aqua 10, "PP", 2267460, 250
        // (gated on the PECH research entry, not a FOCUSPECH one: the CSV has no FOCUSPECH row)
        assertFocus("pech", "PECH", "PP", 2267460, 250, false,
                orderedCost("terra", 10, "perditio", 10, "aqua", 10));
        // ItemFocusWarding.java:35,44,76 — cost terra 25 ordo 25 aqua 10, "BWA", 16771535
        assertFocus("warding", "FOCUSWARDING", "BWA", 16771535, 0, false,
                orderedCost("terra", 25, "ordo", 25, "aqua", 10));
        // ItemFocusPrimal.java:36,52,70 — "FP", 10854849, 500; its price is rolled (below)
        assertFocus("primal", "FOCUSPRIMAL", "FP", 10854849, 500, false, Map.of());
    }

    private static void assertFocus(final String tag, final String researchKey, final String sorting,
                                    final int color, final int cooldown, final boolean perTick,
                                    final Map<String, Integer> cost) {
        final Focus focus = Focuses.byTag(tag);
        assertNotNull(focus, "the focus table has no '" + tag + "' entry");
        assertEquals(researchKey, focus.researchKey(), tag + " research key");
        assertEquals(sorting, focus.sortingHelper(), tag + " sorting helper");
        assertEquals(color, focus.focusColor(), tag + " focus colour");
        assertEquals(cooldown, focus.activationCooldown(), tag + " no-upgrade cooldown (ms)");
        assertEquals(perTick, focus.visCostPerTick(), tag + " isVisCostPerTick");

        // Aspect prices are held as tags in table order — the original's AspectList order.
        final List<String> expectedTags = new ArrayList<>(cost.keySet());
        assertEquals(expectedTags, focus.baseCost().stream().map(Focus.Cost::aspectTag).toList(),
                tag + " base cost aspects, in the original AspectList order");
        for (final Focus.Cost entry : focus.baseCost()) {
            assertEquals(cost.get(entry.aspectTag()), entry.amount(), tag + " " + entry.aspectTag());
        }
    }

    private static Map<String, Integer> orderedCost(final Object... pairs) {
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return out;
    }

    /** The research graph that gates the foci must still carry every entry. */
    @Test
    void everyFocusIsGatedByAShippedResearchEntry() throws IOException {
        // The gradle test cwd is not the project dir, so read the shipped graph
        // off the classpath rather than from src/main/resources.
        final String json;
        try (InputStream in = FociParityTest.class.getResourceAsStream(
                "/data/thaumcraft/research_graph/entries.json")) {
            assertNotNull(in, "the shipped research graph must be on the test classpath");
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (final Focus focus : Focuses.all()) {
            assertTrue(json.contains("\"" + focus.researchKey() + "\""),
                    "focus " + focus.tag() + " is gated on research " + focus.researchKey()
                            + ", which the shipped research graph does not carry");
        }
    }

    /** Every focus row and the pouch row resolve in docs/content-mapping.csv. */
    @Test
    void everyFocusAndThePouchAreInventoryRows() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv), "content-mapping.csv not wired — run tests through ./gradlew test");

        final Map<String, ContentMappingCsv.Row> items = new LinkedHashMap<>();
        for (final ContentMappingCsv.Row row : ContentMappingCsv.read(csv)) {
            if ("item".equals(row.kind())) {
                items.put(row.newId(), row);
            }
        }

        for (final Focus focus : Focuses.all()) {
            final ContentMappingCsv.Row row = items.get("thaumcraft:focus" + focus.tag());
            assertNotNull(row, "no content-mapping.csv item row for focus '" + focus.tag() + "'");
            assertEquals(legacyId(focus.tag()), row.originalId(),
                    "the focus row's original id shape changed");
        }
        assertNotNull(items.get("thaumcraft:focuspouch"), "no content-mapping.csv row for the focus pouch");
        assertEquals(11, Focuses.all().size() + 1,
                "this card owns ten foci plus the pouch");
    }

    /** The original id of a focus row, rebuilt from the class name the original registered. */
    private static String legacyId(final String tag) {
        return "Thaumcraft:Focus" + switch (tag) {
            case "portablehole" -> "PortableHole";
            case "hellbat" -> "Hellbat";
            case "pech" -> "Pech";
            case "primal" -> "Primal";
            case "warding" -> "Warding";
            case "excavation" -> "Excavation";
            case "trade" -> "Trade";
            case "shock" -> "Shock";
            case "frost" -> "Frost";
            case "fire" -> "Fire";
            default -> throw new IllegalStateException("unknown focus tag " + tag);
        };
    }

    /** {@code ItemFocusPrimal.getVisCost}'s roll, and the clock-derived seed it used. */
    @Test
    void primalPriceFollowsTheOriginalRoll() {
        assertEquals(50, Focuses.primalRoll(0));
        assertEquals(250, Focuses.primalRoll(4));
        for (int roll = 0; roll < Focuses.PRIMAL_COST_ROLLS; roll++) {
            final int amount = Focuses.primalRoll(roll);
            assertTrue(amount >= 50 && amount <= 250 && amount % 50 == 0,
                    "roll " + roll + " produced " + amount);
        }
        assertThrows(IllegalArgumentException.class, () -> Focuses.primalRoll(5));
        assertThrows(IllegalArgumentException.class, () -> Focuses.primalRoll(-1));

        // new Random(System.currentTimeMillis() / 200)
        assertEquals(4_000_000L, Focuses.primalSeed(800_000_000L));
    }

    /** An unknown tag is absent, not a synthetic entry. */
    @Test
    void unknownTagsResolveToNothing() {
        assertNull(Focuses.byTag("nope"));
        assertNull(Focuses.byTag(null));
        assertNull(Focuses.researchKey("nope"));
        assertEquals(10, Focuses.tags().size());
        assertTrue(Focuses.baseCost("nope").size() == 0, "an unknown focus has no price");
    }
}
