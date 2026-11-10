package com.jonnyutah12.thaumcraftclassic.research.book;

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
}
