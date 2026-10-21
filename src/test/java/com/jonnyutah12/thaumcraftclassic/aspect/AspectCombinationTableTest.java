package com.jonnyutah12.thaumcraftclassic.aspect;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TC-M4-01 acceptance: the combination table is exhaustive against the
 * original — every ordered pair of the 48 tags maps to the exact original
 * result (the compound whose declared components are that pair, either
 * order), and every other pair has no result.
 *
 * <p>The expected table is derived from {@link OriginalAspectTable} (the
 * frozen original component declarations), not from the port's own data.</p>
 */
class AspectCombinationTableTest {

    private static Map<String, String> expectedPairTable() {
        final Map<String, String> table = new TreeMap<>();
        for (final OriginalAspectTable.Row row : OriginalAspectTable.ROWS) {
            if (row.components().isEmpty()) {
                continue;
            }
            final String first = row.components().get(0);
            final String second = row.components().get(1);
            table.put(first + "+" + second, row.tag());
            table.put(second + "+" + first, row.tag());
        }
        return table;
    }

    @Test
    void combinationTableMatchesTheOriginalPairForPair() {
        final Map<String, String> expected = expectedPairTable();
        assertEquals(84, expected.size(), "42 compounds x 2 orders");
        final Map<String, String> actual = new TreeMap<>(AspectCombinations.combinationTable());
        assertEquals(expected, actual, "combination table differs from the original pair table");
    }

    @Test
    void everyPairOfThe48TagsResolvesExactlyAsTheOriginal() {
        final Map<String, String> expected = expectedPairTable();
        final List<String> tags = AspectDefinitions.all().stream()
                .map(AspectDefinitions.Definition::tag).toList();
        assertEquals(48, tags.size());

        int results = 0;
        for (final String a : tags) {
            for (final String b : tags) {
                final String want = expected.get(a + "+" + b);
                final String got = AspectCombinations.combinationResultTag(a, b);
                assertEquals(want, got, "pair " + a + "+" + b + " must resolve to " + want);
                if (want != null) {
                    results++;
                }
            }
        }
        assertEquals(84, results, "exactly the 84 declared ordered pairs may combine");
    }

    @Test
    void nullInputsHaveNoResult() {
        assertNull(AspectCombinations.combinationResultTag("aer", "unknown"));
    }

    // ------------------------------------------------- reduceToPrimals rules

    private static Map<String, Integer> map(String... kv) {
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put(kv[i], Integer.parseInt(kv[i + 1]));
        }
        return out;
    }

    @Test
    void reduceToPrimalsMatchesTheOriginalRecursiveExpansion() {
        // Original ResearchManager.reduceToPrimals: every compound contributes
        // both components (recursively), amounts add.
        assertEquals(map("aer", "3", "ignis", "3"),
                new TreeMap<>(AspectCombinations.reduceToPrimalsTags(map("lux", "3"), false)));
        // metallum = terra + vitreus; vitreus = terra + ordo -> terra twice.
        assertEquals(map("ordo", "1", "terra", "2"),
                new TreeMap<>(AspectCombinations.reduceToPrimalsTags(map("metallum", "1"), false)));
        // Deep chain: messis -> herba+humanus -> ... -> six primals, counts
        // from the original expansion (verified against the definition graph).
        assertEquals(map("aer", "2", "aqua", "8", "ignis", "2", "ordo", "2",
                        "perditio", "2", "terra", "10"),
                new TreeMap<>(AspectCombinations.reduceToPrimalsTags(map("messis", "2"), false)));
        // Primals pass through; mixed input accumulates.
        assertEquals(map("aer", "7", "ignis", "1", "perditio", "1"),
                new TreeMap<>(AspectCombinations.reduceToPrimalsTags(map("aer", "5", "tenebrae", "1"), false)));
    }

    @Test
    void reduceToPrimalsMergeVariantKeepsMaximums() {
        // Original reduceToPrimals(al, true): contributions merge by max.
        assertEquals(map("ordo", "1", "terra", "1"),
                new TreeMap<>(AspectCombinations.reduceToPrimalsTags(map("metallum", "1"), true)));
        assertEquals(map("aer", "5", "ignis", "1", "perditio", "1"),
                new TreeMap<>(AspectCombinations.reduceToPrimalsTags(map("aer", "5", "tenebrae", "1"), true)));
    }

    // ------------------------------------------------------ cullTags weights

    @Test
    void cullTagOrderAppliesTheOriginalWeighting() {
        // Original ThaumcraftApiHelper.cullTags: primal x0.9; per non-primal
        // component x1.1; per non-primal grandchild x1.05; drop the lowest
        // until 6 remain. Expected kept set computed from the original
        // arithmetic over the frozen table: aer/ignis (pure primal, 9.0) and
        // then the lowest-weighted compound go first.
        final Map<String, Integer> amounts = map("aer", "10", "ignis", "10", "lux", "10",
                "metallum", "10", "potentia", "10", "terra", "10", "ordo", "10", "vitreus", "10");
        final List<String> kept = AspectCombinations.cullTagOrder(amounts);
        assertEquals(6, kept.size());
        // Deterministic: insertion order minus the two dropped primals.
        assertEquals(List.of("lux", "metallum", "potentia", "terra", "ordo", "vitreus"), kept,
                "cullTags kept set differs from the original weighting");
    }
}
