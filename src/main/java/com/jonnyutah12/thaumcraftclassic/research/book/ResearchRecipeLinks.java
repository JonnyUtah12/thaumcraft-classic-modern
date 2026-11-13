package com.jonnyutah12.thaumcraftclassic.research.book;

import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The research-page → vanilla-crafting-row join (TC-M3-04-s-iv-b, closes BD-19).
 *
 * <p>The original linked its recipe pages to its recipes in code. A crafting
 * page carries the key of the row in the {@code ConfigResearch.recipes} map —
 * {@code ConfigResearch.java:2661-2667} gives the KNOWFRAG entry a
 * {@code normal_crafting} page built from {@code recipes.get("KnowFrag")}, and
 * {@code ConfigRecipes.java:3860-3865} registers that same row as a plain
 * crafting-table recipe <b>and</b> puts it in the map. The shipped graph keeps
 * the page's half of that: {@code entries.json} carries
 * {@code {"type":"normal_crafting","recipe":"KnowFrag"}} for the KNOWFRAG entry
 * (and 45 pages overall name a recipes-map key).</p>
 *
 * <p>What 1.21.1 has lost is the other half: there is no recipes map, and a
 * {@code minecraft:crafting_*} data row has nowhere to carry a research key —
 * the port's own recipe types (arcane / infusion / crucible) have a
 * {@code research} field in their JSON, which is why their pages resolve a row
 * by themselves. So for the vanilla-crafting family the join is the
 * content-mapping ledger: the row {@code Thaumcraft:recipe.256.crafting_shaped}
 * carries {@code type=crafting_shaped research=KNOWFRAG} in its notes column,
 * and its JSON's {@code _source} field cites the registration
 * {@code ConfigResearch.recipes.put("KnowFrag", ...)} — which is the page's
 * recipes-map key.</p>
 *
 * <p>This class is that join, compiled in: the ledger is a {@code docs/} file
 * and is not shipped in the jar (gradle wires it into the test and game-test
 * JVMs through the {@code tc.contentMappingFile} system property only), so the
 * table lives here and {@code ThaumonomiconRecipePageTest
 * .theLedgerShipsExactlyThisVanillaCraftingJoin} re-derives it from the CSV,
 * from the shipped row JSONs' {@code _source} fields and from the shipped
 * {@code entries.json} pages on every build. A new linked row that is not
 * mirrored here fails the suite, and so does a link whose ledger row, row JSON
 * or graph page was removed. Nothing about a page's display can drift from what
 * the graph and the ledger say, and {@link com.jonnyutah12.thaumcraftclassic.recipes.RecipePageDisplay}
 * resolves the id against the live {@code RecipeManager}, so a page can only
 * ever draw a row that actually crafts.</p>
 *
 * <p>There is deliberately <b>no research gate</b> on these rows: the original's
 * KNOWFRAG entry is {@code setAutoUnlock} (BD-19 rejects a gate — it would add a
 * failure mode with no behaviour), the rows ship ungated, and the page body is
 * display only.</p>
 *
 * @param originalKey the original {@code ConfigResearch.recipes} map key a
 *     {@code normal_crafting} page names
 * @param researchKey the research key the ledger links the row to
 * @param recipeId    the port's row id, in the vanilla {@code RecipeManager}
 */
public record ResearchRecipeLinks(String originalKey, String researchKey, String recipeId) {

    /**
     * The vanilla-crafting rows the original put on a research page, in ledger
     * order. Today the original has exactly one: KNOWFRAG. The other 23 shipped
     * {@code .crafting_shaped/shapeless/special} rows carry {@code research=-}
     * in the ledger — the original never showed them on a page — and the 44
     * remaining {@code normal_crafting} pages name recipes-map keys whose rows
     * the ledger has not implemented yet, so they draw no body.
     */
    private static final Map<String, ResearchRecipeLinks> BY_ORIGINAL_KEY = buildTable();

    private static Map<String, ResearchRecipeLinks> buildTable() {
        final Map<String, ResearchRecipeLinks> links = new LinkedHashMap<>();
        // ConfigRecipes.java:3860-3865 (the row) + ConfigResearch.java:2661-2667
        // (the KNOWFRAG page that names it) + content-mapping.csv row
        // Thaumcraft:recipe.256.crafting_shaped, notes column
        // "type=crafting_shaped research=KNOWFRAG".
        links.put("KnowFrag", new ResearchRecipeLinks("KnowFrag", "KNOWFRAG",
                "thaumcraft:recipe.256.crafting_shaped"));
        return Map.copyOf(links);
    }

    /** The row a {@code normal_crafting} page names, or null if it is not shipped. */
    public static ResearchRecipeLinks byOriginalKey(final String originalKey) {
        return originalKey == null ? null : BY_ORIGINAL_KEY.get(originalKey);
    }

    /** The rows linked to a research key, in ledger order; empty for most entries. */
    public static List<String> rowsOfResearch(final String researchKey) {
        return BY_ORIGINAL_KEY.values().stream()
                .filter(link -> link.researchKey().equals(researchKey))
                .map(ResearchRecipeLinks::recipeId)
                .toList();
    }

    /** The whole join, keyed by the original recipes-map key. */
    public static Map<String, ResearchRecipeLinks> table() {
        return BY_ORIGINAL_KEY;
    }

    /**
     * The vanilla crafting row one page draws, or null. The page carries the
     * original's recipes-map key (the shipped graph kept that half of the pair —
     * {@code ConfigResearch.java:2661-2667} built the KNOWFRAG page from
     * {@code recipes.get("KnowFrag")}) and this join is the map from that key to
     * the port's row id.
     *
     * <p>Two guards, both from the ledger: the link must name <b>this</b> entry
     * (the row's {@code research=} column is the entry it belongs to, so a page
     * in one entry can never draw the row of another), and the page must be the
     * crafting kind the join covers — the port's own recipe types carry a
     * {@code research} field in their JSON and are drawn by their own cards
     * (BD-17's follow-ups), not by the vanilla-crafting path.</p>
     *
     * <p>This is the rule, in one place: the page pager
     * ({@link ThaumonomiconBook#frame()}) and the book screen's detail panel
     * (through the entry detail's {@code recipeId}) both ask it, so a page and a
     * panel cannot disagree about which row an entry draws.</p>
     */
    public static String vanillaCraftingRowId(final ResearchEntryData entry, final ResearchPageData page) {
        if (entry == null || page == null
                || page.type() != ResearchPageData.Type.NORMAL_CRAFTING) {
            return null;
        }
        final ResearchRecipeLinks link = byOriginalKey(page.recipe());
        if (link == null || !link.researchKey().equals(entry.key())) {
            return null;
        }
        return link.recipeId();
    }

    /**
     * The row an <b>entry</b> draws as its page body: the row of the first of
     * its pages the join links (BD-64 — the unified book screen lists entries
     * and shows one detail panel, so the panel asks the entry, not a page index).
     *
     * @return the row id, or null when no page of the entry names a shipped
     *     vanilla crafting row — which is the state of 44 of the graph's 45
     *     {@code normal_crafting} pages, whose recipes-map keys have no ledger
     *     row implemented yet.
     */
    public static String linkedRowOf(final ResearchEntryData entry) {
        if (entry == null || entry.pages() == null) {
            return null;
        }
        for (final ResearchPageData page : entry.pages()) {
            final String row = vanillaCraftingRowId(entry, page);
            if (row != null) {
                return row;
            }
        }
        return null;
    }
}
