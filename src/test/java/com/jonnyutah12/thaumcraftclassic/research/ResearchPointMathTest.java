package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-02 acceptance: the research-point economy against original values
 * (docs/balance-values.md §4, TC-M0-06).
 *
 * <p>In TC4 4.2.3.5 the per-player "research points" are the aspect pool
 * (the original {@code /thaumcraft aspect} help literally calls them "aspect
 * research points"): completing an entry through the Thaumonomicon deducts the
 * entry's tag amounts via {@code AspectList.reduce}, which is all-or-nothing.
 * The constructor "complexity" (RP cost) is transcribed into the graph and
 * drives research-note generation (TC-M3-04); there is no per-category point
 * store in this version. Both halves are pinned here: the pool arithmetic and
 * the full §4 cost table.</p>
 */
class ResearchPointMathTest {

    // ------------------------------------------------------- pool spend/refund

    @Test
    void spendDeductsExactlyTheTagCost() {
        PlayerKnowledgeData k = PlayerKnowledgeData.empty()
                .withAspectPoolAdded("ignis", 10)
                .withAspectPoolAdded("praecantatio", 10);
        k = k.withAspectPoolAdded("ignis", -3).withAspectPoolAdded("praecantatio", -3);
        assertEquals(7, k.aspectAmount("ignis"));
        assertEquals(7, k.aspectAmount("praecantatio"));
    }

    @Test
    void spendToExactlyZeroIsAllowed() {
        final PlayerKnowledgeData k = PlayerKnowledgeData.empty()
                .withAspectPoolAdded("ignis", 3)
                .withAspectPoolAdded("ignis", -3);
        assertEquals(0, k.aspectAmount("ignis"));
    }

    @Test
    void spendBelowTheCostChangesNothing() {
        // Original AspectList.reduce: only reduces when getAmount >= amount.
        // A pool of 2 paying a cost of 3 stays at 2 — never negative.
        PlayerKnowledgeData k = PlayerKnowledgeData.empty().withAspectPoolAdded("ignis", 2);
        final PlayerKnowledgeData after = k.withAspectPoolAdded("ignis", -3);
        assertEquals(2, after.aspectAmount("ignis"));
        assertSame(k, after, "no-change spend must return the same instance (original's false)");

        // An empty pool can't pay either.
        k = PlayerKnowledgeData.empty();
        assertSame(k, k.withAspectPoolAdded("ignis", -1));
    }

    @Test
    void refundAddsBack() {
        PlayerKnowledgeData k = PlayerKnowledgeData.empty()
                .withAspectPoolAdded("ignis", 10)
                .withAspectPoolAdded("ignis", -3)
                .withAspectPoolAdded("ignis", 3);
        assertEquals(10, k.aspectAmount("ignis"));
    }

    @Test
    void zeroAmountNeverChangesAnything() {
        final PlayerKnowledgeData k = PlayerKnowledgeData.empty();
        assertSame(k, k.withAspectPoolAdded("ignis", 0));
    }

    @Test
    void spendingNeverUndoesDiscovery() {
        // The original keeps the aspect in the discovered map at amount 0;
        // discovery and pool are distinguishable.
        PlayerKnowledgeData k = PlayerKnowledgeData.empty()
                .withAspectDiscovered("auram", 5)
                .withAspectPoolAdded("auram", -5);
        assertEquals(0, k.aspectAmount("auram"));
        assertTrue(k.hasAspectDiscovered("auram"));
    }

    // ------------------------------------------------- original cost values

    @Test
    void graphCostsMatchBalanceValuesSection4Table() throws IOException {
        final String file = System.getProperty("tc.balanceValuesFile");
        assertNotNull(file, "system property 'tc.balanceValuesFile' is not set — run tests through ./gradlew test");
        final Map<String, Integer> table = parseCostTable(Path.of(file));
        assertEquals(201, table.size(), "§4 must list all 201 research entries");

        // §4 records the raw constructor argument (the 6th ResearchItem arg).
        // Reconstructing it from the graph: complexityRaw when present (args
        // below the original's clamp), else complexity; the two 2-arg entries
        // (CAP_iron, ROD_wood) have no cost at all and are documented as 0.
        final Map<Integer, Integer> distribution = new HashMap<>();
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            final int rawCost = originalCostArgument(entry);
            distribution.merge(rawCost, 1, Integer::sum);
            final Integer documented = table.get(entry.key());
            assertNotNull(documented, "graph entry missing from §4: " + entry.key());
            assertEquals(documented, rawCost, "RP cost mismatch for " + entry.key());
        }

        // §4's stated distribution: 0×25, 1×75, 2×83, 3×16, plus the two
        // virtual/auto-unlock placeholders documented as "0 (virtual/auto)".
        assertEquals(Map.of(0, 27, 1, 75, 2, 83, 3, 16), distribution);
    }

    @Test
    void effectiveComplexityIsClampedToOneToThree() {
        // ResearchItem's constructor clamps: complexity < 1 → 1, > 3 → 3.
        // The graph stores that effective value in complexity; only the two
        // 2-arg virtual entries have none.
        for (final ResearchEntryData entry : ResearchManager.graph().entries()) {
            if (entry.complexity() == null) {
                assertTrue(entry.flags() != null && entry.flags().virtual(),
                        "only 2-arg virtual entries may lack a complexity: " + entry.key());
                continue;
            }
            assertTrue(entry.complexity() >= 1 && entry.complexity() <= 3,
                    "complexity outside the original clamp: " + entry.key());
        }
    }

    private static int originalCostArgument(final ResearchEntryData entry) {
        if (entry.complexityRaw() != null) {
            return entry.complexityRaw();
        }
        return entry.complexity() == null ? 0 : entry.complexity();
    }

    @Test
    void purchaseCostsAreTheEntryTags() {
        // The Thaumonomicon payment for FOCUSFIRE is its tags: FIRE 3 + MAGIC 3
        // (ConfigResearch.java:89 — tags and RP cost are distinct values; both
        // are pinned: tags here, RP cost in the §4 test).
        final ResearchEntryData focusfire = ResearchManager.graph().entry("FOCUSFIRE");
        assertNotNull(focusfire);
        assertEquals(2, focusfire.tags().size());
        assertEquals(3, tagAmount(focusfire, "ignis"));
        assertEquals(3, tagAmount(focusfire, "praecantatio"));
    }

    private static int tagAmount(final ResearchEntryData entry, final String aspect) {
        return entry.tags().stream().filter(t -> t.aspect().equals(aspect))
                .mapToInt(ResearchEntryData.AspectTag::amount).sum();
    }

    /** Parses the §4 markdown table: | key | category | RP cost | line |. */
    private static Map<String, Integer> parseCostTable(final Path file) throws IOException {
        final Map<String, Integer> table = new HashMap<>();
        boolean inSection = false;
        for (final String line : Files.readAllLines(file)) {
            if (line.startsWith("## 4. Research costs")) {
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("## ")) {
                break;
            }
            if (!inSection || !line.startsWith("|")) {
                continue;
            }
            final String[] cells = line.split("\\|");
            if (cells.length < 5) {
                continue;
            }
            final String key = cells[1].trim();
            final String cost = cells[3].trim().replaceAll("\\s*\\(.*\\)$", "");
            if (key.isEmpty() || key.equals("Research key") || key.equals("---")) {
                continue;
            }
            table.put(key, Integer.parseInt(cost));
        }
        return table;
    }
}
