package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils;
import com.jonnyutah12.thaumcraftclassic.research.NotePuzzle;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * TC-M3-04 acceptance on a real server: the note puzzle's generation and
 * completion rules against the shipped research graph and the TC-M3-02 player
 * knowledge record. No client, no screen, no world state - the puzzle is pure
 * logic, so a headless server can exercise it end to end.
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NotePuzzleGameTest {

    private NotePuzzleGameTest() {
    }

    /**
     * Every shipped entry generates a note whose required aspects are the
     * entry's tags placed on the outer ring of the {@code 1 + min(3, complexity)}
     * radius - and the whole thing is seed-reproducible.
     */
    @GameTest(template = "empty")
    public static void notePuzzleGeneratesFromTheShippedGraph(final GameTestHelper helper) {
        final ResearchGraph graph = ResearchManager.graph();
        helper.assertTrue(graph.entryCount() == 201,
                "the shipped research graph must carry 201 entries, got " + graph.entryCount());

        final ResearchEntryData fire = graph.entry("FOCUSFIRE");
        helper.assertTrue(fire != null, "FOCUSFIRE is missing from the shipped graph");
        final ResearchNoteData note = NotePuzzle.generate(fire, new Random(7L));
        helper.assertTrue(note != null, "note generation returned null for a tagged entry");
        helper.assertTrue(note.equals(NotePuzzle.generate(fire, new Random(7L))),
                "the same seed must reproduce the same note");
        helper.assertTrue("FOCUSFIRE".equals(note.key()), "the note must carry the entry key");

        int entries = 0;
        for (final ResearchEntryData entry : graph.entries()) {
            final int complexity = entry.complexity() == null ? 1 : entry.complexity();
            final int radius = 1 + Math.min(3, complexity);
            final ResearchNoteData generated = NotePuzzle.generate(entry, new Random(11L));
            if (entry.tags() == null || entry.tags().isEmpty() || NotePuzzle.primaryTag(entry) == null) {
                helper.assertTrue(generated == null,
                        entry.key() + ": the original returned null without a primary tag");
                continue;
            }
            helper.assertTrue(generated != null, entry.key() + ": generation returned null");
            int required = 0;
            for (final Map.Entry<String, HexEntry> cell : generated.hexEntries().entrySet()) {
                final HexUtils.Hex hex = HexUtils.Hex.fromKey(cell.getKey());
                if (cell.getValue().type() == 1) {
                    required++;
                    helper.assertTrue(HexUtils.getDistance(new HexUtils.Hex(0, 0), hex) == radius,
                            entry.key() + ": required aspect off the ring at " + cell.getKey());
                } else {
                    helper.assertTrue(cell.getValue().type() == 0 && cell.getValue().aspectTag() == null,
                            entry.key() + ": filler cells carry no aspect");
                }
            }
            helper.assertTrue(required == entry.tags().size(),
                    entry.key() + ": expected " + entry.tags().size() + " required cells, found " + required);
            entries++;
        }
        helper.assertTrue(entries > 100, "the graph yielded only " + entries + " generatable entries");
        helper.succeed();
    }

    /**
     * Completion is gated by the player's discovered aspects and by the
     * component relation, on a two-cell note - the same rule
     * {@code checkConnections} applied.
     */
    @GameTest(template = "empty")
    public static void notePuzzleCompletionFollowsDiscoveredAspects(final GameTestHelper helper) {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry("lux", 1));
        final ResearchNoteData puzzle = new ResearchNoteData("PROBE", 0, false, 0, cells);

        final PlayerKnowledgeData both = PlayerKnowledgeData.empty()
                .withAspectDiscovered("ignis", 1)
                .withAspectDiscovered("lux", 1);
        final Predicate<String> discovered = both::hasAspectDiscovered;
        final ResearchNoteData solved = NotePuzzle.checkCompletion(puzzle, discovered);
        helper.assertTrue(solved != null, "lux = aer + ignis must connect two adjacent required cells");
        helper.assertTrue(solved.complete(), "a solved note is marked complete");
        helper.assertTrue(solved.hexEntries().size() == 2, "the required pair survives the prune");

        final PlayerKnowledgeData partial = PlayerKnowledgeData.empty().withAspectDiscovered("ignis", 1);
        helper.assertTrue(NotePuzzle.checkCompletion(puzzle, partial::hasAspectDiscovered) == null,
                "an undiscovered aspect must block completion");

        final Map<String, HexEntry> primals = new LinkedHashMap<>();
        primals.put("0:0", new HexEntry("aer", 1));
        primals.put("1:0", new HexEntry("terra", 1));
        helper.assertTrue(
                NotePuzzle.checkCompletion(new ResearchNoteData("PROBE", 0, false, 0, primals), tag -> true) == null,
                "two primal aspects never connect");
        helper.succeed();
    }
}
