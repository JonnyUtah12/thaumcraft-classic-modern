package com.jonnyutah12.thaumcraftclassic.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory registry of the original research graph: categories and entries
 * indexed by key, with the structural invariants of the original
 * {@code ResearchCategories} registration checked up front.
 *
 * <p>{@link #problems()} lists every structural violation found (unknown
 * references, cycles, duplicate browser positions, unparsed page/icon data).
 * The shipped graph has none; the unit tests assert that and would surface
 * any hand-edit that breaks parity with the original.</p>
 */
public final class ResearchGraph {

    private final LinkedHashMap<String, ResearchCategoryData> categories = new LinkedHashMap<>();
    private final LinkedHashMap<String, ResearchEntryData> entries = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public ResearchGraph(List<ResearchCategoryData> categoryList, List<ResearchEntryData> entryList) {
        for (ResearchCategoryData category : categoryList) {
            if (categories.putIfAbsent(category.key(), category) != null) {
                problems.add("duplicate category key: " + category.key());
            }
        }
        for (ResearchEntryData entry : entryList) {
            if (entries.putIfAbsent(entry.key(), entry) != null) {
                problems.add("duplicate entry key: " + entry.key());
            }
        }
        validate();
    }

    private void validate() {
        for (ResearchEntryData entry : entries.values()) {
            if (!categories.containsKey(entry.category())) {
                problems.add(entry.key() + ": unknown category " + entry.category());
            }
            for (String parent : concat(entry.parents(), entry.parentsHidden(), entry.siblings())) {
                if (!entries.containsKey(parent)) {
                    problems.add(entry.key() + ": references unknown research key " + parent);
                }
            }
            if (entry.icon() != null && !Set.of("item", "texture", "none").contains(entry.icon().kind())) {
                problems.add(entry.key() + ": unresolved icon " + entry.icon());
            }
            for (ResearchPageData page : nullSafe(entry.pages())) {
                if (page.type() == null) {
                    problems.add(entry.key() + ": page with unknown type " + page);
                }
                if (page.textKey() != null && page.textEnUs() == null) {
                    problems.add(entry.key() + ": page text missing for lang key " + page.textKey());
                }
                if (page.requires() != null && !entries.containsKey(page.requires())) {
                    problems.add(entry.key() + ": page requires unknown key " + page.requires());
                }
            }
        }
        detectCycles();
        detectPositionCollisions();
    }

    /**
     * The prerequisite graph (parents + parentsHidden edges, as the original
     * ResearchManager gates them) must be acyclic.
     */
    private void detectCycles() {
        Map<String, Integer> state = new LinkedHashMap<>();
        for (String key : entries.keySet()) {
            if (state.getOrDefault(key, 0) == 0) {
                walk(key, state, new ArrayList<>());
            }
        }
    }

    private void walk(String key, Map<String, Integer> state, List<String> stack) {
        state.put(key, 1);
        stack.add(key);
        ResearchEntryData entry = entries.get(key);
        for (String parent : concat(entry.parents(), entry.parentsHidden())) {
            if (!entries.containsKey(parent)) {
                continue;
            }
            int mark = state.getOrDefault(parent, 0);
            if (mark == 1) {
                problems.add("cycle in research graph: " + String.join(" -> ", stack) + " -> " + parent);
            } else if (mark == 0) {
                walk(parent, state, stack);
            }
        }
        stack.remove(stack.size() - 1);
        state.put(key, 2);
    }

    /**
     * The original {@code ResearchCategories.addResearch} silently drops a
     * non-virtual entry whose browser position overlaps an existing one;
     * the extracted graph must not contain such an overlap.
     */
    private void detectPositionCollisions() {
        Map<String, String> placed = new LinkedHashMap<>();
        for (ResearchEntryData entry : entries.values()) {
            if (entry.flags() != null && entry.flags().virtual()) {
                continue;
            }
            String pos = entry.category() + ":" + entry.column() + "," + entry.row();
            String previous = placed.putIfAbsent(pos, entry.key());
            if (previous != null) {
                problems.add("position collision " + pos + ": " + previous + " vs " + entry.key());
            }
        }
    }

    public ResearchCategoryData category(String key) {
        return categories.get(key);
    }

    public ResearchEntryData entry(String key) {
        return entries.get(key);
    }

    public Collection<ResearchCategoryData> categories() {
        return categories.values();
    }

    public Collection<ResearchEntryData> entries() {
        return entries.values();
    }

    public List<ResearchEntryData> entriesIn(String category) {
        return entries.values().stream().filter(e -> e.category().equals(category)).toList();
    }

    public int categoryCount() {
        return categories.size();
    }

    public int entryCount() {
        return entries.size();
    }

    public List<String> problems() {
        return List.copyOf(problems);
    }

    private static List<String> concat(List<String>... lists) {
        List<String> out = new ArrayList<>();
        for (List<String> list : lists) {
            out.addAll(nullSafe(list));
        }
        return out;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
