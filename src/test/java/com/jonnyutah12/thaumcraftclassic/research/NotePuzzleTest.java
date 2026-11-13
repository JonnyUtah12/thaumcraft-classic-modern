package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData.AspectTag;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData.Flags;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData.Triggers;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the TC-M3-04 note puzzle rules to the original
 * {@code ResearchManager.createNote} / {@code checkResearchCompletion} /
 * {@code checkConnections} (TC 4.2.3.5): the {@code 1 + min(3, complexity)}
 * radius, the required aspects on the outer ring in the entry's tag order, the
 * {@code complexity * 2} interior blanks behind the {@code cc < 2} guard, the
 * highest-amount primary tag (and its colour), and the completion rule — two
 * discovered aspects where one is a direct component of the other.
 */
class NotePuzzleTest {

    private static final String AER = "aer";
    private static final String TERRA = "terra";
    private static final String IGNIS = "ignis";
    private static final String LUX = "lux";
    private static final String VACUOS = "vacuos";
    private static final String TEMPESTAS = "tempestas";

    private static ResearchEntryData entry(String key, Integer complexity, String... aspectAmounts) {
        final List<AspectTag> tags = new ArrayList<>();
        for (String spec : aspectAmounts) {
            final String[] parts = spec.split(":");
            tags.add(new AspectTag(parts[0], parts[0], Integer.parseInt(parts[1])));
        }
        return new ResearchEntryData(key, "BASICS", 1, 1, complexity, null, tags, null,
                new Flags(false, false, false, false, false, false, false, false, false),
                List.of(), List.of(), List.of(), new Triggers(List.of(), List.of(), List.of()),
                0, "tc.research_name." + key, "name", "tc.research_text." + key, "text", List.of());
    }

    private static ResearchNoteData note(Map<String, HexEntry> cells) {
        return new ResearchNoteData("K", 0, false, 0, cells);
    }

    private static ResearchNoteData noteOf(Object... keyTypeAspect) {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        for (int i = 0; i < keyTypeAspect.length; i += 3) {
            cells.put((String) keyTypeAspect[i],
                    new HexEntry((String) keyTypeAspect[i + 2], (Integer) keyTypeAspect[i + 1]));
        }
        return note(cells);
    }

    private static Predicate<String> discoveredEverywhere() {
        return tag -> true;
    }

    // ------------------------------------------------------------------ primary tag

    @Test
    void primaryTagIsTheHighestPositiveAmountAndNullWhenNoneIsPositive() {
        assertEquals(AER, NotePuzzle.primaryTag(entry("K", 2, AER + ":3", IGNIS + ":2", TERRA + ":1")));
        assertEquals(AER, NotePuzzle.primaryTag(entry("K", 2, AER + ":3", IGNIS + ":3")), "first max wins a tie");
        assertNull(NotePuzzle.primaryTag(entry("K", 2, AER + ":0", IGNIS + ":0")));
        assertNull(NotePuzzle.primaryTag(entry("K", 2)));
        assertNull(NotePuzzle.primaryTag(null));
    }

    // ------------------------------------------------------------------ generation

    @Test
    void generateReturnsNullWithoutATaggablePrimary() {
        assertNull(NotePuzzle.generate(entry("K", 2), new Random(1L)));
        assertNull(NotePuzzle.generate(entry("K", 2, AER + ":0"), new Random(1L)),
                "the original returned null when getResearchPrimaryTag() was null");
        assertNull(NotePuzzle.generate(null, new Random(1L)));
    }

    @Test
    void radiusIsOnePlusMinThreeComplexity() {
        for (int complexity : new int[]{1, 2, 3, 4, 9}) {
            final int radius = 1 + Math.min(3, complexity);
            final int disc = 1 + 3 * radius * (radius + 1);
            final ResearchNoteData generated =
                    NotePuzzle.generate(entry("K", complexity, AER + ":3", IGNIS + ":1"), new Random(5L));
            assertNotNull(generated);
            final int blanks = complexity > 1 ? complexity * 2 : 0;
            assertEquals(disc - blanks, generated.hexEntries().size(),
                    "complexity " + complexity + " must fill the disc of radius " + radius + " minus its blanks");
        }
    }

    @Test
    void requiredAspectsSitOnTheOuterRingInTheEntryTagOrder() {
        final ResearchNoteData generated =
                NotePuzzle.generate(entry("K", 1, AER + ":3", IGNIS + ":2", TERRA + ":1"), new Random(3L));
        assertNotNull(generated);
        // complexity 1 -> radius 2 -> a 12-cell ring; three tags land at indices 0, 4, 8.
        final List<Hex> ring = HexUtils.getRing(2);
        assertEquals(AER, generated.entryAt(ring.get(0)).aspectTag());
        assertEquals(IGNIS, generated.entryAt(ring.get(4)).aspectTag());
        assertEquals(TERRA, generated.entryAt(ring.get(8)).aspectTag());

        int required = 0;
        for (Map.Entry<String, HexEntry> cell : generated.hexEntries().entrySet()) {
            if (cell.getValue().type() == 1) {
                required++;
                assertEquals(2, HexUtils.getDistance(new Hex(0, 0), Hex.fromKey(cell.getKey())),
                        "required aspect " + cell.getValue().aspectTag() + " is off the ring");
            } else {
                assertEquals(0, cell.getValue().type());
                assertNull(cell.getValue().aspectTag());
            }
        }
        assertEquals(3, required);
        assertEquals("K", generated.key());
        assertFalse(generated.complete());
        assertEquals(0, generated.copies());
        assertEquals(16777086, generated.color(), "the original stored the primary tag's colour (aer)");
    }

    @Test
    void interiorBlanksAreComplexityTimesTwo() {
        for (int complexity : new int[]{2, 3, 4}) {
            final int radius = 1 + Math.min(3, complexity);
            final int disc = 1 + 3 * radius * (radius + 1);
            final ResearchNoteData generated =
                    NotePuzzle.generate(entry("K", complexity, AER + ":3", IGNIS + ":2"), new Random(11L));
            assertNotNull(generated);
            assertEquals(disc - complexity * 2, generated.hexEntries().size(),
                    "the original removed exactly complexity * 2 interior cells");
        }
    }

    @Test
    void generateIsDeterministicForASeedAndIndependentOfOtherSeeds() {
        final ResearchEntryData e = entry("K", 3, AER + ":3", IGNIS + ":2", TERRA + ":1");
        final ResearchNoteData a = NotePuzzle.generate(e, new Random(42L));
        final ResearchNoteData b = NotePuzzle.generate(e, new Random(42L));
        assertEquals(a, b, "the same seed must reproduce the original's draw sequence exactly");

        final Set<Integer> sizes = Set.of(9, 42, 777, 31337).stream()
                .map(seed -> NotePuzzle.generate(e, new Random(seed)).hexEntries().size())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(61 - 6), sizes, "every seed removes exactly the six blanks");
    }

    // ------------------------------------------------------------------ connection rule

    @Test
    void connectionNeedsADirectComponentLink() {
        assertTrue(NotePuzzle.componentsMatch(IGNIS, LUX), "lux = aer + ignis");
        assertTrue(NotePuzzle.componentsMatch(AER, LUX));
        assertTrue(NotePuzzle.componentsMatch(LUX, IGNIS), "the rule is symmetric");
        assertFalse(NotePuzzle.componentsMatch(AER, TERRA), "two primals never connect");
        assertFalse(NotePuzzle.componentsMatch(LUX, VACUOS));
        assertFalse(NotePuzzle.componentsMatch(IGNIS, TEMPESTAS));
        assertFalse(NotePuzzle.componentsMatch("nope", LUX));
    }

    // ------------------------------------------------------------------ completion

    @Test
    void completionAcceptsAnAdjacentComponentPairAndPrunesTheFiller() {
        final ResearchNoteData puzzle = noteOf(
                "0:0", 1, IGNIS,
                "1:0", 1, LUX,
                "0:1", 0, null,
                "-1:0", 0, null);
        final ResearchNoteData solved = NotePuzzle.checkCompletion(puzzle, discoveredEverywhere());
        assertNotNull(solved, "ignis and lux are adjacent and lux = aer + ignis");
        assertTrue(solved.complete());
        assertEquals(Set.of("0:0", "1:0"), solved.hexEntries().keySet(),
                "unconnected filler cells are pruned, the required pair stays");
        assertNotSame(puzzle, solved);
        assertEquals(4, puzzle.hexEntries().size(), "the input note is untouched");
        assertFalse(puzzle.complete());
    }

    @Test
    void completionWalksThroughPlayerPlacedCells() {
        // ignis --- lux (placed) --- aer: lux bridges both primals.
        final ResearchNoteData puzzle = noteOf(
                "0:0", 1, IGNIS,
                "1:0", 2, LUX,
                "2:0", 1, AER,
                "1:1", 0, null);
        final ResearchNoteData solved = NotePuzzle.checkCompletion(puzzle, discoveredEverywhere());
        assertNotNull(solved, "a placed cell must carry the walk between two required cells");
        assertTrue(solved.complete());
        assertEquals(Set.of("0:0", "1:0", "2:0"), solved.hexEntries().keySet());
    }

    @Test
    void completionRejectsPairsWithoutAComponentLink() {
        assertNull(NotePuzzle.checkCompletion(noteOf("0:0", 1, AER, "1:0", 1, TERRA), discoveredEverywhere()),
                "two primal aspects never connect");
        assertNull(NotePuzzle.checkCompletion(noteOf("0:0", 1, IGNIS, "1:0", 1, TEMPESTAS), discoveredEverywhere()),
                "tempestas = aer + aqua never connects to ignis");
    }

    @Test
    void completionRequiresBothAspectsToBeDiscovered() {
        final ResearchNoteData puzzle = noteOf("0:0", 1, IGNIS, "1:0", 1, LUX);
        assertNull(NotePuzzle.checkCompletion(puzzle, tag -> false));
        assertNull(NotePuzzle.checkCompletion(puzzle, IGNIS::equals),
                "the original required hasDiscoveredAspect for both sides");
        assertNotNull(NotePuzzle.checkCompletion(puzzle, tag -> true));
    }

    @Test
    void completionReturnsNullWhenTheRequiredCellsAreDisconnected() {
        assertNull(NotePuzzle.checkCompletion(noteOf("0:0", 1, IGNIS, "3:0", 1, LUX), discoveredEverywhere()),
                "a required cell the walk never reaches keeps the note unsolved");
        assertNull(NotePuzzle.checkCompletion(note(Map.of()), discoveredEverywhere()));
        assertNull(NotePuzzle.checkCompletion(null, discoveredEverywhere()));
    }
}
