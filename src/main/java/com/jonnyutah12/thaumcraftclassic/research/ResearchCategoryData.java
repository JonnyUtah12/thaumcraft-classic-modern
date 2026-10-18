package com.jonnyutah12.thaumcraftclassic.research;

/**
 * One research category of the original Thaumcraft 4.2.3.5 research browser
 * (registered via {@code ResearchCategories.registerCategory} in the
 * decompiled ConfigResearch.java; see docs/research-graph-notes.md).
 *
 * <p>Data comes from {@code data/thaumcraft/research/categories.json},
 * extracted mechanically by scripts/extract_research_graph.py. The
 * {@code nameEnUs} value is the original en_US.lang string kept verbatim
 * as All Rights Reserved quotation (docs/licensing-and-attribution.md);
 * runtime display must resolve {@code nameKey} through the game language
 * system.</p>
 *
 * @param key        original category key (BASICS, THAUMATURGY, ...)
 * @param order      registration order in the original browser
 * @param icon       original category icon texture location
 * @param background original category background texture location
 * @param nameKey    original lang key ({@code tc.research_category.<key>})
 * @param nameEnUs   original en_US.lang display name, verbatim
 */
public record ResearchCategoryData(
        String key,
        int order,
        String icon,
        String background,
        String nameKey,
        String nameEnUs) {
}
