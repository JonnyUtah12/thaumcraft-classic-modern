package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * The research-note puzzle: hex-grid generation and completion checking
 * (TC-M3-04).
 *
 * <p>A line-for-line port of {@code ResearchManager.createNote}'s grid
 * generation and
 * {@code ResearchManager.checkResearchCompletion}/{@code checkConnections}
 * (TC 4.2.3.5). Pure Java — aspect identity is the tag string and the component
 * relation is read from {@link AspectDefinitions} — so the puzzle rules are
 * unit-testable without a game (docs/testing.md).</p>
 *
 * <p>Fidelity notes, all pinned by tests:</p>
 *
 * <ul>
 *   <li><b>Required-aspect order is the original's.</b> The original assigned
 *   {@code rr.tags.getAspects()[count]} around the ring, and
 *   {@code AspectList.aspects} is a {@code LinkedHashMap}, so
 *   {@code getAspects()} returns the insertion order in which
 *   {@code ConfigResearch} added them. That order IS the {@code tags} order the
 *   TC-M3-01 extractor preserved in {@code entries.json}, so the port is
 *   faithful here and uses {@code entry.tags()} directly.</li>
 *   <li><b>The interior blank picks stay on the original's random stream and in
 *   its map order.</b> {@code createNote} removed {@code complexity * 2}
 *   interior cells, rebuilding its candidate array from the {@code HashMap} of
 *   cells after every removal and drawing one {@code nextInt} per attempt. The
 *   port keeps the same {@code HashMap}-backed structures and the same rebuild,
 *   so the same seed yields the same removals
 *   ({@code NotePuzzleOriginalParityTest}).</li>
 *   <li>The port additionally tolerates a null aspect on a grid cell while the
 *   original would have thrown; a type&gt;=1 cell always carries one, so no
 *   reachable state differs.</li>
 * </ul>
 */
public final class NotePuzzle {

    /**
     * Generate the note state for one research entry, per original
     * {@code ResearchManager.createNote}: radius {@code 1 + min(3,
     * complexity)}, required aspects evenly spaced on the outer ring, and
     * {@code complexity * 2} interior blanks punched out for complexity &gt; 1
     * (a blank is only removed when every adjacent required hex keeps at least
     * two hex neighbours — the original's {@code cc < 2} guard).
     *
     * @param entry  the research entry (its tags are the required aspects)
     * @param random the world random, consumed in the original's order
     * @return the note state, or null when the entry has no primary tag (the
     *         original returned null when {@code getResearchPrimaryTag()} was
     *         null: no tags at all, or no tag with a positive amount)
     */
    public static ResearchNoteData generate(ResearchEntryData entry, Random random) {
        if (entry == null || entry.tags() == null || entry.tags().isEmpty()) {
            return null;
        }
        final String primary = primaryTag(entry);
        if (primary == null) {
            return null;
        }
        final int complexity = entry.complexity() == null ? 1 : entry.complexity();
        final int radius = 1 + Math.min(3, complexity);

        // The original kept two parallel HashMaps keyed by the "q:r" string; the
        // blank-removal loop rebuilds its candidate array from the cell map, so
        // the map type and the insertion sequence are behaviour, not style.
        final Map<String, Hex> hexLocs = HexUtils.generateHexes(radius);
        final List<Hex> outerRing = HexUtils.distributeRingRandomly(radius, entry.tags().size(), random);
        final Map<String, Hex> hexes = new HashMap<>();
        final Map<String, HexEntry> hexEntries = new HashMap<>();

        for (Hex hex : hexLocs.values()) {
            hexes.put(hex.toString(), hex);
            hexEntries.put(hex.toString(), new HexEntry(null, 0));
        }

        int count = 0;
        for (Hex hex : outerRing) {
            hexes.put(hex.toString(), hex);
            hexEntries.put(hex.toString(), new HexEntry(entry.tags().get(count).aspect(), 1));
            count++;
        }

        if (complexity > 1) {
            int blanks = complexity * 2;
            Hex[] temp = hexes.values().toArray(new Hex[0]);
            while (blanks > 0) {
                final int indx = random.nextInt(temp.length);
                final HexEntry candidate = hexEntries.get(temp[indx].toString());
                if (candidate != null && candidate.type() == 0) {
                    boolean gtg = true;
                    for (int n = 0; n < 6; n++) {
                        final Hex neighbour = temp[indx].getNeighbour(n);
                        if (hexes.containsKey(neighbour.toString())
                                && hexEntries.get(neighbour.toString()).type() == 1) {
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

        return new ResearchNoteData(entry.key(), colorOf(primary), false, 0, hexEntries);
    }

    /**
     * The original {@code ResearchItem.getResearchPrimaryTag}: the tag with the
     * highest amount, first one winning a tie, and null when no tag carries a
     * positive amount.
     */
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
     * required hexes through player-placed ones; a required aspect connects when
     * both aspects are discovered and one is a direct component of the other.
     * When every required hex is reachable, the note completes and the
     * unconnected filler hexes are pruned from the grid.
     *
     * <p>The walk starts at the first required cell of the grid map's iteration
     * order, as the original's {@code HashMap} iteration did; the result is
     * start-independent whenever the required cells form one connected chain,
     * which is the only completion the original ever saw.</p>
     *
     * @return the completed note state, or null when the puzzle is not yet
     *         solved (the original returned false and left the note alone)
     */
    public static ResearchNoteData checkCompletion(ResearchNoteData note, Predicate<String> aspectDiscovered) {
        if (note == null || note.hexEntries().isEmpty()) {
            return null;
        }
        final List<String> checked = new ArrayList<>();
        final List<String> main = new ArrayList<>();
        final List<String> remains = new ArrayList<>();

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

        final List<String> remove = new ArrayList<>();
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
            final Hex target = hex.getNeighbour(a);
            final HexEntry targetEntry = note.hexEntries().get(target.toString());
            if (!checked.contains(target.toString()) && targetEntry != null && targetEntry.type() >= 1) {
                final String aspect1 = note.entryAt(hex).aspectTag();
                final String aspect2 = targetEntry.aspectTag();
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
     * other (primal aspects have no components and can only be the parent, so
     * two primals never connect).
     */
    public static boolean componentsMatch(String tag1, String tag2) {
        final AspectDefinitions.Definition d1 = definition(tag1);
        final AspectDefinitions.Definition d2 = definition(tag2);
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
