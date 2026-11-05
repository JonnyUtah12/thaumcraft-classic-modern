package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the TC-M3-04 generation logic to the decompiled original rather than to
 * itself:
 *
 * <ul>
 *   <li>{@link #generatedGridsMatchADirectReplayOfTheDecompiledCreateNote} replays
 *   {@code ResearchManager.createNote} (TC 4.2.3.5) as its own transcription —
 *   including the {@code HashMap} of cells the original rebuilt its candidate
 *   array from, and the one-{@code nextInt}-per-attempt blank loop — and demands
 *   the port's grid be identical for every shipped entry across fixed seeds. The
 *   replay is what makes the blank-removal draw order observable: a port that
 *   reorders the candidate array (e.g. by iterating a {@code LinkedHashMap})
 *   removes different cells and fails here.</li>
 *   <li>the remaining assertions re-derive the transcribed rule constants from
 *   the decompiled text at test time, so a drift in either the port or the
 *   reference is caught.</li>
 * </ul>
 */
class NotePuzzleOriginalParityTest {

    private static final int[] SEEDS = {1, 2, 3, 5, 7, 11, 42, 1234};

    private static final String RESEARCH_MANAGER =
            "decompiled/thaumcraft/thaumcraft/common/lib/research/ResearchManager.java";
    private static final String HEX_UTILS = "decompiled/thaumcraft/thaumcraft/common/lib/utils/HexUtils.java";
    private static final String RESEARCH_ITEM = "decompiled/thaumcraft/thaumcraft/api/research/ResearchItem.java";
    private static final String ASPECT_LIST = "decompiled/thaumcraft/thaumcraft/api/aspects/AspectList.java";

    @Test
    void generatedGridsMatchADirectReplayOfTheDecompiledCreateNote() {
        final ResearchGraph graph = ResearchGraphLoader.loadDefault();
        int generated = 0;
        int nulls = 0;
        for (ResearchEntryData entry : graph.entries()) {
            for (int seed : SEEDS) {
                final Map<String, String> replay = replay(entry, new Random(seed));
                final ResearchNoteData port = NotePuzzle.generate(entry, new Random(seed));
                if (replay == null) {
                    assertEquals(null, port, entry.key() + " seed " + seed + ": the original returned null");
                    nulls++;
                } else {
                    assertEquals(replay, gridOf(port),
                            entry.key() + " seed " + seed + ": the port's grid differs from the original replay");
                    generated++;
                }
            }
        }
        assertEquals(201 * SEEDS.length, generated + nulls, "every shipped entry must be compared");
        assertTrue(generated > 0, "no entry produced a grid to compare");
    }

    @Test
    void transcribedConstantsMatchTheDecompiledOriginal() {
        final String manager = ReferenceOrigins.read(RESEARCH_MANAGER);
        assertTrue(manager.contains("int radius = 1 + Math.min(3, rr.getComplexity())"), "radius formula");
        assertTrue(manager.contains("int blanks = rr.getComplexity() * 2"), "blank count");
        assertTrue(manager.contains("if (cc < 2)"), "the neighbour guard");
        assertTrue(manager.contains("gt.func_74774_a(\"hexq\", (byte)hex.q)"), "the hexq byte write");
        assertTrue(manager.contains("gt.func_74774_a(\"hexr\", (byte)hex.r)"), "the hexr byte write");
        assertTrue(manager.contains("gt.func_74774_a(\"type\", (byte)hexEntries.get(hex.toString()).type)"),
                "the type byte write");
        assertTrue(manager.contains("note.hexEntries.get(target.toString()).type >= 1"),
                "the walk accepts required and player-placed cells");
        assertTrue(manager.contains("!aspect1.isPrimal() && (aspect1.getComponents()[0] == aspect2 "
                        + "|| aspect1.getComponents()[1] == aspect2)"),
                "the component connection rule");
        assertTrue(manager.contains("&& Thaumcraft.proxy.getPlayerKnowledge().hasDiscoveredAspect(username, aspect2)"),
                "both aspects must be discovered");

        final String hexUtils = ReferenceOrigins.read(HEX_UTILS);
        assertTrue(hexUtils.contains("float spacing = (float)ring.size() / entries"), "the ring spacing");
        assertTrue(hexUtils.contains("int start = random.nextInt(ring.size())"), "the discarded start draw");
        assertTrue(hexUtils.contains("results.add(ring.get(Math.round(pos)))"),
                "positions are spaced from ring index 0, never from the drawn start");

        final String researchItem = ReferenceOrigins.read(RESEARCH_ITEM);
        assertTrue(researchItem.contains("if (this.tags.getAmount(tag) > highest)"),
                "the primary tag is the highest positive amount, first max wins");
    }

    @Test
    void requiredAspectOrderIsTheOriginalAspectListOrder() {
        // The original assigned rr.tags.getAspects()[count] around the ring and
        // AspectList backs those with a LinkedHashMap, so getAspects() is the
        // insertion order ConfigResearch used - which is the order the TC-M3-01
        // extractor preserved in entries.json. The port using entry.tags() is
        // therefore faithful, not an approximation.
        final String aspectList = ReferenceOrigins.read(ASPECT_LIST);
        assertTrue(aspectList.contains("public LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();"),
                "AspectList.aspects must stay insertion-ordered for the tag order to be faithful");
        assertTrue(aspectList.contains("return this.aspects.keySet().toArray(q);"),
                "getAspects() must return the map's own order");
    }

    // ------------------------------------------------------------------ the replay

    /**
     * The grid as a comparable map: cell key -> "type|aspect".
     */
    private static Map<String, String> gridOf(ResearchNoteData note) {
        if (note == null) {
            return null;
        }
        final Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, HexEntry> cell : note.hexEntries().entrySet()) {
            out.put(cell.getKey(), cell.getValue().type() + "|" + cell.getValue().aspectTag());
        }
        return out;
    }

    /**
     * A direct transcription of {@code ResearchManager.createNote}
     * (TC 4.2.3.5), decompiled at {@code ResearchManager.java:300-370}, with the
     * original's two parallel {@code HashMaps} and its candidate-array rebuild.
     */
    private static Map<String, String> replay(ResearchEntryData entry, Random random) {
        if (entry.tags() == null || entry.tags().isEmpty()) {
            return null;
        }
        final String primary = NotePuzzle.primaryTag(entry);
        if (primary == null) {
            return null;
        }
        final int complexity = entry.complexity() == null ? 1 : entry.complexity();
        final int radius = 1 + Math.min(3, complexity);

        final HashMap<String, Hex> hexLocs = HexUtils.generateHexes(radius);
        final ArrayList<Hex> outerRing =
                HexUtils.distributeRingRandomly(radius, entry.tags().size(), random);
        final HashMap<String, Hex> hexes = new HashMap<>();
        final HashMap<String, String[]> hexEntries = new HashMap<>();

        for (Hex hex : hexLocs.values()) {
            hexes.put(hex.toString(), hex);
            hexEntries.put(hex.toString(), new String[]{null, "0"});
        }

        int count = 0;
        for (Hex hex : outerRing) {
            hexes.put(hex.toString(), hex);
            hexEntries.put(hex.toString(), new String[]{entry.tags().get(count).aspect(), "1"});
            count++;
        }

        if (complexity > 1) {
            int blanks = complexity * 2;
            Hex[] temp = hexes.values().toArray(new Hex[0]);
            while (blanks > 0) {
                final int indx = random.nextInt(temp.length);
                final String[] candidate = hexEntries.get(temp[indx].toString());
                if (candidate != null && "0".equals(candidate[1])) {
                    boolean gtg = true;
                    for (int n = 0; n < 6; n++) {
                        final Hex neighbour = temp[indx].getNeighbour(n);
                        if (hexes.containsKey(neighbour.toString())
                                && "1".equals(hexEntries.get(neighbour.toString())[1])) {
                            int cc = 0;
                            for (int q = 0; q < 6; q++) {
                                if (hexes.containsKey(hexes.get(neighbour.toString()).getNeighbour(q).toString())) {
                                    cc++;
                                }
                                if (cc >= 2) {
                                    break;
                                }
                            }
                            if (cc < 2) {
                                gtg = false;
                                break;
                            }
                        }
                    }
                    if (gtg) {
                        hexes.remove(temp[indx].toString());
                        hexEntries.remove(temp[indx].toString());
                        temp = hexes.values().toArray(new Hex[0]);
                        blanks--;
                    }
                }
            }
        }

        final Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String[]> cell : hexEntries.entrySet()) {
            out.put(cell.getKey(), cell.getValue()[1] + "|" + cell.getValue()[0]);
        }
        return out;
    }
}
