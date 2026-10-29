package com.jonnyutah12.thaumcraftclassic.research.book;

/**
 * One category tab of the book's top bar (TC-M3-03-s).
 *
 * <p>The original's {@code GuiResearchBrowser} drew one tab per registered
 * {@code ResearchCategory} in registration order and highlighted the active
 * one; the six categories and their order come from
 * {@code data/thaumcraft/research_graph/categories.json} (TC-M3-01).</p>
 *
 * <p>{@code nameKey} is the original lang key
 * ({@code tc.research_category.<KEY>}) and is what the screen passes to
 * {@code Component.translatable}; {@code icon} is the original texture
 * location, resolved only when the All-Rights-Reserved reference overlay is
 * present (D-004), so the screen falls back to the label when it is not.</p>
 *
 * @param categoryKey original category key (BASICS, THAUMATURGY, ...)
 * @param nameKey     original lang key for the tab label
 * @param order       original registration order
 * @param icon        original category icon texture location
 * @param selected    whether this is the tab the book is showing
 */
public record BookTab(
        String categoryKey,
        String nameKey,
        int order,
        String icon,
        boolean selected) {
}
