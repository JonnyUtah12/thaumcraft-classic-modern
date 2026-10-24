package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * The research-note puzzle: hex-grid generation and completion checking
 * (TC-M3-04).
 *
 * <p>Ports {@code ResearchManager.createNote}'s grid generation and
 * {@code ResearchManager.checkResearchCompletion}/{@code checkConnections}
 * (TC 4.2.3.5). Pure Java — aspect identity is the tag string and the
 * component relation is read from {@link AspectDefinitions} — so the puzzle
 * rules are unit-testable without a game (docs/testing.md).</p>
 *
 * <p>One deliberate deviation: the original assigned the required aspects to
 * outer-ring positions in {@code AspectList} HashMap iteration order (an
 * accident of 1.7.10's map, not a rule); the port uses the entry's
 * {@code tags} order from the research graph JSON, which preserves the
 * original ConfigResearch insertion order and makes generation deterministic
 * for a given seed.</p>
 */
public final class NotePuzzle {

    /**
     * Generate the note state for one research entry, per original
     * {@code ResearchManager.createNote}: radius {@code 1 + min(3,
     * complexity)}, required aspects evenly spaced on the outer ring, and
     * {@code complexity * 2} interior blanks punched out for complexity &gt; 1
     * (a blank is only removed when every adjacent required hex keeps at
     * least two hex neighbours — the original's {@code cc < 2} guard).
     *
     * @param entry   the research entry (its tags are the required aspects)
     * @param random  the world random, consumed in the original's order
     * @return the note state, or null when the entry has no tags (the
     *         original returned null for a null primary tag)
     */
    public static ResearchNoteData generate(ResearchEntryData entry, Random random) {
        if (entry == null || entry.tags() == null || entry.tags().isEmpty()) {
            return null;
        }
        final int complexity = entry.complexity() == null ? 1 : entry.complexity();
        final String key = entry.key();
        final int color = primaryTag(entry) == null ? 0 : colorOf(primaryTag(entry));

        final int radius = 1 + Math.min(3, complexity);
        Map<String, Hex> hexLocs = HexUtils.generateHexes(radius);
        List<Hex> outerRing = HexUtils.distributeRingRandomly(radius, entry.tags().size(), random);

        Map<String, HexEntry> hexEntries = new LinkedHashMap<>();
        for (String hexKey : hexLocs.keySet()) {
            hexEntries.put(hexKey, new HexEntry(null, 0));
        }
        int count = 0;
        for (Hex hex : outerRing) {
            hexEntries.put(hex.toString(), new HexEntry(entry.tags().get(count).aspect(), 1));
            count++;
        }

        if (complexity > 1) {
            int blanks = complexity * 2;
            List<Hex> temp = new ArrayList<>(hexEntries.keySet().stream().map(Hex::fromKey).toList());
            while (blanks > 0) {
                int indx = random.nextInt(temp.size());
                HexEntry candidate = hexEntries.get(temp.get(indx).toString());
                if (candidate != null && candidate.type() == 0) {
                    boolean gtg = true;
                    for (int n = 0; n < 6; n++) {
                        Hex neighbour = temp.get(indx).getNeighbour(n);
                        HexEntry neighbourEntry = hexEntries.get(neighbour.toString());
                        if (neighbourEntry != null && neighbourEntry.type() == 1) {
                            int cc = 0;
                            for (int q = 0; q < 6; q++) {
                                if (hexEntries.containsKey(neighbour.getNeighbour(q).toString())) {
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
                        hexEntries.remove(temp.get(indx).toString());
                        temp = new ArrayList<>(hexEntries.keySet().stream().map(Hex::fromKey).toList());
                        blanks--;
                    }
                }
            }
        }

        return new ResearchNoteData(key, color, false, 0, hexEntries);
    }

    /** The original {@code ResearchItem.getResearchPrimaryTag}: highest-amount tag. */
    public static String primaryTag(ResearchEntryData entry) {
        if (entry == null || entry.tags() == null || entry.tags().isEmpty()) {
            return null;
        }
        String tag = null;
        int highest = 0;
        for (ResearchEntryData.AspectTag t : entry.tags()) {
            if (t.amount() > highest) {
                tag = t.aspect();
                highest = t.amount();
            }
        }
        return tag;
    }

    private static int colorOf(String tag) {
        for (AspectDefinitions.Definition d : AspectDefinitions.all()) {
            if (d.tag().equals(tag)) {
                return d.color();
            }
        }
        return 0;
    }

    /**
     * The original {@code ResearchManager.checkResearchCompletion}: walk the
     * required hexes through player-placed ones; a required aspect connects
     * when both aspects are discovered and one is a direct component of the
     * other. When every required hex is reachable, the note completes and
     * the unconnected filler hexes are pruned from the grid.
     *
     * @return the completed note state, or null when the puzzle is not yet
     *         solved (the original returned false and left the note alone)
     */
    public static ResearchNoteData checkCompletion(ResearchNoteData note, Predicate<String> aspectDiscovered) {
        if (note == null || note.hexEntries().isEmpty()) {
            return null;
        }
        List<String> checked = new ArrayList<>();
        List<String> main = new ArrayList<>();
        List<String> remains = new ArrayList<>();
        for (Map.Entry<String, HexEntry> e : note.hexEntries().entrySet()) {
            if (e.getValue().type() == 1) {
                main.add(e.getKey());
            }
        }
        for (String hexKey : note.hexEntries().keySet()) {
            if (note.hexEntries().get(hexKey).type() == 1) {
                main.remove(hexKey);
                checkConnections(note, Hex.fromKey(hexKey), checked, main, remains, aspectDiscovered);
                break;
            }
        }
        if (!main.isEmpty()) {
            return null;
        }
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, HexEntry> e : note.hexEntries().entrySet()) {
            if (e.getValue().type() != 1 && !remains.contains(e.getKey())) {
                remove.add(e.getKey());
            }
        }
        return note.without(remove).withComplete(true);
    }

    private static void checkConnections(
            ResearchNoteData note, Hex hex, List<String> checked, List<String> main, List<String> remains,
            Predicate<String> discovered) {
        checked.add(hex.toString());
        for (int a = 0; a < 6; a++) {
            Hex target = hex.getNeighbour(a);
            HexEntry targetEntry = note.hexEntries().get(target.toString());
            if (!checked.contains(target.toString()) && targetEntry != null && targetEntry.type() >= 1) {
                String aspect1 = note.entryAt(hex).aspectTag();
                String aspect2 = targetEntry.aspectTag();
                if (aspect1 != null && aspect2 != null
                        && discovered.test(aspect1) && discovered.test(aspect2)
                        && componentsMatch(aspect1, aspect2)) {
                    remains.add(target.toString());
                    if (targetEntry.type() == 1) {
                        main.remove(target.toString());
                    }
                    checkConnections(note, target, checked, main, remains, discovered);
                }
            }
        }
    }

    /**
     * The original connection rule: one aspect is a direct component of the
     * other (primal aspects have no components and can only be the child).
     */
    public static boolean componentsMatch(String tag1, String tag2) {
        AspectDefinitions.Definition d1 = definition(tag1);
        AspectDefinitions.Definition d2 = definition(tag2);
        if (d1 == null || d2 == null) {
            return false;
        }
        return (!d1.primal() && d1.components().contains(tag2))
                || (!d2.primal() && d2.components().contains(tag1));
    }

    private static AspectDefinitions.Definition definition(String tag) {
        for (AspectDefinitions.Definition d : AspectDefinitions.all()) {
            if (d.tag().equals(tag)) {
                return d;
            }
        }
        return null;
    }

    private NotePuzzle() {
    }
}
