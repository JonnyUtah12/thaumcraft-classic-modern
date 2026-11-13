package com.jonnyutah12.thaumcraftclassic.research.book;

/**
 * One drawn prerequisite line of the book's graph view (TC-M3-03-s),
 * parent → child.
 *
 * <p>A line is drawn for each entry of the drawn node's {@code parents} list
 * (the visible prerequisites) and never for {@code parentsHidden} — the
 * original registration API kept those out of the browser on purpose
 * ({@code ResearchEntryData}). Lines are drawn only between nodes of the
 * category being shown: the browser displays one category at a time, so a
 * prerequisite living in another category has no position on this screen to
 * draw to. 47 of the extracted graph's prerequisite pairs cross categories
 * (INFUSION in ARTIFICE gates entries in THAUMATURGY, for instance) and are
 * therefore not drawn — recorded as BD-17.</p>
 *
 * @param parentKey the prerequisite entry (line start)
 * @param childKey  the entry it unlocks (line end)
 */
public record BookEdge(String parentKey, String childKey) {
}
