package com.jonnyutah12.thaumcraftclassic.research.book;

import java.util.List;

/**
 * The complete description of one book frame (TC-M3-03-s): everything the
 * screen needs to draw, and nothing it has to compute.
 *
 * <p>This is the seam the evidence rides on. The screen is a thin consumer of
 * this record, and the game test asserts on the record directly — a
 * headless dedicated server cannot instantiate a {@code Screen}, but it can
 * prove that opening the book produces a frame with the six category tabs, the
 * placed nodes and drawn prerequisite lines of the current category, and the
 * open entry's page.</p>
 *
 * @param tabs           one tab per registered category, in original order
 * @param categoryKey    the category currently drawn
 * @param categoryNameKey its original lang key (the browser heading)
 * @param nodes          placed nodes of that category (virtual entries excluded)
 * @param edges          drawn prerequisite lines among those nodes
 * @param page           the page being read, or null while on the graph view
 */
public record BookFrame(
        List<BookTab> tabs,
        String categoryKey,
        String categoryNameKey,
        List<BookNode> nodes,
        List<BookEdge> edges,
        BookPageView page) {

    /** True while the graph (not an entry's page) is showing. */
    public boolean onGraph() {
        return page == null;
    }

    /** The visible nodes, in draw order. */
    public List<BookNode> visibleNodes() {
        return nodes.stream().filter(BookNode::visible).toList();
    }

    /** The currently selected tab (the one the book is showing). */
    public BookTab selectedTab() {
        return tabs.stream().filter(BookTab::selected).findFirst().orElse(null);
    }
}
