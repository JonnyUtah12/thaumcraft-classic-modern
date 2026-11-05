package com.jonnyutah12.thaumcraftclassic.research.book;

import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraphLoader;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-03-s acceptance for the Thaumonomicon's presentation state machine.
 *
 * <p>Everything asserted here is a property of the real shipped research graph
 * (TC-M3-01 data, {@code data/thaumcraft/research_graph/}), not of a fixture:
 * the six categories in registration order, the placed entries of each at their
 * original browser coordinates, the drawn prerequisite lines, the hidden-entry
 * and concealed-page gates, and the original page lists the book pages through.
 * The counts below are the extracted graph's own numbers, so a hand-edit of the
 * JSON that changes them shows up here as well as in
 * {@code ResearchGraphDataTest}.</p>
 *
 * <p>What is deliberately NOT asserted: anything about drawing. The screen is
 * {@code client.ThaumonomiconScreen} and cannot be constructed off a client;
 * that it consumes exactly a {@link BookFrame} is pinned separately by
 * {@code ThaumonomiconScreenRegistrationTest}, and the open path in-game by
 * {@code ThaumonomiconGameTest}.</p>
 */
class ThaumonomiconBookTest {

    /** The six original categories, in registration order (categories.json). */
    private static final List<String> CATEGORY_ORDER =
            List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH");

    /** Placed (non-virtual) entries per category in that order — 199 of the 201. */
    private static final List<Integer> PLACED_ENTRIES = List.of(19, 40, 35, 50, 39, 16);

    /** Drawn prerequisite lines per category with full knowledge. */
    private static final List<Integer> DRAWN_LINES = List.of(10, 38, 33, 40, 37, 15);

    private static ResearchGraph graph() {
        return ResearchGraphLoader.loadDefault();
    }

    private static ThaumonomiconBook open() {
        return ThaumonomiconBook.open(graph(), ThaumonomiconKnowledge.all());
    }

    private static ThaumonomiconBook openKnowing(final Set<String> keys) {
        return ThaumonomiconBook.open(graph(), ThaumonomiconKnowledge.of(keys));
    }

    @Test
    void opensOnTheFirstRegisteredCategoryWithOneTabPerCategory() {
        final ThaumonomiconBook book = open();
        final BookFrame frame = book.frame();

        assertEquals(CATEGORY_ORDER, frame.tabs().stream().map(BookTab::categoryKey).toList(),
                "tabs must be the six original categories in registration order");
        assertEquals("BASICS", frame.categoryKey(),
                "the book opens on the first registered category, as the original browser did");
        assertEquals("tc.research_category.BASICS", frame.categoryNameKey(),
                "the heading is the category's original lang key");
        assertEquals(1, frame.tabs().stream().filter(BookTab::selected).count(),
                "exactly one tab is the active one");
        assertEquals("BASICS", frame.selectedTab().categoryKey(), "the active tab is the drawn category");

        // Tab order is carried too: the screen draws left to right from it.
        assertEquals(List.of(0, 1, 2, 3, 4, 5), frame.tabs().stream().map(BookTab::order).toList(),
                "tab order must be the original registration order");
        for (final BookTab tab : frame.tabs()) {
            assertEquals(graph().category(tab.categoryKey()).icon(), tab.icon(),
                    "the tab carries the original category icon location: " + tab.categoryKey());
        }
    }

    @Test
    void graphNodesAreThePlacedEntriesAtTheirOriginalBrowserPositions() {
        final ResearchGraph graph = graph();
        for (final String category : CATEGORY_ORDER) {
            final ThaumonomiconBook book = open();
            assertTrue(book.selectCategory(category), "a registered category must be selectable");

            final BookFrame frame = book.frame();
            final List<ResearchEntryData> placed = graph.entriesIn(category).stream()
                    .filter(entry -> entry.flags() == null || !entry.flags().virtual())
                    .toList();

            assertEquals(placed.size(), frame.nodes().size(),
                    "every placed entry of " + category + " must be a node");
            assertEquals(PLACED_ENTRIES.get(CATEGORY_ORDER.indexOf(category)), frame.nodes().size(),
                    "placed-entry count of " + category + " (extracted graph)");

            for (final BookNode node : frame.nodes()) {
                final ResearchEntryData entry = graph.entry(node.key());
                assertNotNull(entry, "a node must be a real entry: " + node.key());
                assertEquals(category, entry.category(), "a node belongs to the drawn category");
                assertEquals(entry.column(), node.column(), "original column of " + node.key());
                assertEquals(entry.row(), node.row(), "original row of " + node.key());
                assertEquals(entry.nameKey(), node.nameKey(), "original name key of " + node.key());
            }
        }
    }

    @Test
    void virtualEntriesAreNeverPlacedInTheBrowser() {
        final ResearchGraph graph = graph();
        int placed = 0;
        for (final String category : CATEGORY_ORDER) {
            final ThaumonomiconBook book = open();
            book.selectCategory(category);
            placed += book.frame().nodes().size();
        }
        assertEquals(199, placed,
                "the browser holds every non-virtual entry of the 201-entry graph");
        assertEquals(graph.entryCount() - 2, placed,
                "exactly the graph's two virtual entries are outside the browser");

        // The original registered the two staff caps as virtual prerequisites.
        for (final String virtual : List.of("CAP_iron", "ROD_wood")) {
            assertTrue(graph.entry(virtual).flags().virtual(), virtual + " must stay virtual in the data");
            final ThaumonomiconBook book = open();
            book.selectCategory(graph.entry(virtual).category());
            assertTrue(book.frame().nodes().stream().noneMatch(node -> node.key().equals(virtual)),
                    "a virtual entry must never be given a browser position: " + virtual);
        }
    }

    @Test
    void edgesAreTheDrawnPrerequisiteLinesWithinTheCategory() {
        final ResearchGraph graph = graph();
        int total = 0;
        for (final String category : CATEGORY_ORDER) {
            final ThaumonomiconBook book = open();
            book.selectCategory(category);
            final BookFrame frame = book.frame();
            final Set<String> nodeKeys = frame.visibleNodes().stream().map(BookNode::key)
                    .collect(java.util.stream.Collectors.toSet());

            assertEquals(DRAWN_LINES.get(CATEGORY_ORDER.indexOf(category)), frame.edges().size(),
                    "drawn prerequisite lines of " + category);
            for (final BookEdge edge : frame.edges()) {
                assertTrue(nodeKeys.contains(edge.parentKey()),
                        "an edge must start at a drawn node of the category: " + edge);
                assertTrue(nodeKeys.contains(edge.childKey()),
                        "an edge must end at a drawn node of the category: " + edge);
                assertTrue(graph.entry(edge.childKey()).parents().contains(edge.parentKey()),
                        "an edge must be one of the child's drawn prerequisites: " + edge);
                assertTrue(graph.entry(edge.parentKey()).category().equals(category),
                        "the original draws no line across categories: " + edge);
            }
            total += frame.edges().size();
        }
        assertEquals(173, total, "the drawn-line set over the whole graph");

        // A concrete line: RESEARCH → THAUMONOMICON is the original's own
        // prerequisite pair and must be drawn in BASICS.
        final ThaumonomiconBook book = open();
        assertTrue(book.frame().edges().contains(new BookEdge("RESEARCH", "THAUMONOMICON")),
                "BASICS must draw the original RESEARCH -> THAUMONOMICON prerequisite");

        // …and the within-category rule is a real restriction, not a vacuous
        // one: a large share of the graph's prerequisites cross categories
        // (INFUSION in ARTIFICE gates entries in THAUMATURGY), so those pairs
        // have no on-screen line at all.
        final long crossCategory = graph.entries().stream()
                .filter(entry -> entry.flags() == null || !entry.flags().virtual())
                .flatMap(entry -> entry.parents().stream()
                        .filter(parent -> graph.entry(parent) != null
                                && !graph.entry(parent).category().equals(entry.category())))
                .count();
        assertEquals(47, crossCategory,
                "the graph carries 47 cross-category prerequisites, which are never drawn");
    }

    @Test
    void parentsHiddenAreNeverDrawnAsLines() {
        final ResearchGraph graph = graph();
        final List<ResearchEntryData> withHiddenParents = graph.entries().stream()
                .filter(entry -> entry.parentsHidden() != null && !entry.parentsHidden().isEmpty())
                .toList();
        assertFalse(withHiddenParents.isEmpty(),
                "the graph must exercise the hidden-parent path for this test to mean anything");
        assertEquals(16, withHiddenParents.size(),
                "the extracted graph carries 16 entries with hidden prerequisites");

        // FOCUSHELLBAT has *only* hidden prerequisites, so the original places it
        // in the browser with no incoming line at all.
        final ResearchEntryData shellbat = graph.entry("FOCUSHELLBAT");
        assertTrue(shellbat.parents().isEmpty() && !shellbat.parentsHidden().isEmpty(),
                "FOCUSHELLBAT keeps its two hidden-only prerequisites");
        final ThaumonomiconBook book = open();
        book.selectCategory("THAUMATURGY");
        assertTrue(book.frame().edges().stream().noneMatch(edge -> edge.childKey().equals("FOCUSHELLBAT")),
                "a hidden prerequisite must never be drawn as a line");
    }

    @Test
    void hiddenEntriesStayHiddenUntilThePlayerRevealsThem() {
        // CRIMSON is BASICS' one hidden entry (flags.hidden in the data).
        assertTrue(graph().entry("CRIMSON").flags().hidden(), "CRIMSON must stay hidden in the data");

        final ThaumonomiconBook blind = ThaumonomiconBook.open(graph(), ThaumonomiconKnowledge.none());
        final BookNode hiddenForBlind = node(blind.frame(), "CRIMSON");
        assertFalse(hiddenForBlind.visible(), "an unrevealed hidden entry must not be drawn");
        assertFalse(hiddenForBlind.known(), "an unrevealed hidden entry is not known");

        // A clue unlock completes the @-key, which is what reveals the entry.
        final BookNode revealed = node(openKnowing(Set.of("@CRIMSON")).frame(), "CRIMSON");
        assertTrue(revealed.visible(), "the @-key reveal must draw the hidden entry");
        assertTrue(node(openKnowing(Set.of("CRIMSON")).frame(), "CRIMSON").visible(),
                "a plain completion also reveals the entry");

        // Revealing a hidden entry also draws the line it owes to its child in
        // the SAME category: THAUMATURGY's FOCUSHELLBAT is the sole prerequisite
        // of VAMPBAT, so that line appears exactly when the entry is revealed.
        final ThaumonomiconBook blindShelf = ThaumonomiconBook.open(graph(), ThaumonomiconKnowledge.none());
        blindShelf.selectCategory("THAUMATURGY");
        assertEquals(37, blindShelf.frame().edges().size(),
                "THAUMATURGY draws 37 lines while FOCUSHELLBAT is unrevealed");
        assertFalse(blindShelf.frame().edges().contains(new BookEdge("FOCUSHELLBAT", "VAMPBAT")),
                "an unrevealed hidden parent draws no line to its child");

        final ThaumonomiconBook revealedShelf = openKnowing(Set.of("@FOCUSHELLBAT"));
        revealedShelf.selectCategory("THAUMATURGY");
        assertTrue(revealedShelf.frame().edges().contains(new BookEdge("FOCUSHELLBAT", "VAMPBAT")),
                "a revealed hidden parent's line to its child must be drawn");
        assertEquals(38, revealedShelf.frame().edges().size(),
                "THAUMATURGY draws 38 lines once FOCUSHELLBAT is revealed");
    }

    @Test
    void concealedPagesNeedTheirPrerequisite() {
        // ARCANELAMP's third page is the original's text_concealed page, gated
        // on ARCANEBORE.
        final ThaumonomiconBook blind = ThaumonomiconBook.open(graph(), ThaumonomiconKnowledge.none());
        assertTrue(blind.selectEntry("ARCANELAMP"), "ARCANELAMP must open");
        assertTrue(blind.nextPage() && blind.nextPage(), "ARCANELAMP has three pages");
        final BookPageView concealed = blind.frame().page();
        assertEquals(ResearchPageData.Type.TEXT_CONCEALED, concealed.kind(),
                "the third page is the concealed one");
        assertEquals("ARCANEBORE", concealed.requiresKey(), "the page's original prerequisite");
        assertFalse(concealed.revealed(),
                "a concealed page must stay concealed until its prerequisite is known");

        final ThaumonomiconBook knowing = openKnowing(Set.of("ARCANEBORE"));
        knowing.selectEntry("ARCANELAMP");
        knowing.nextPage();
        knowing.nextPage();
        assertTrue(knowing.frame().page().revealed(),
                "completing the prerequisite reveals the concealed page");
    }

    @Test
    void pageNavigationWalksTheOriginalPageListAndStopsAtBothEnds() {
        final ThaumonomiconBook book = open();
        assertTrue(book.selectEntry("THAUMONOMICON"), "THAUMONOMICON must open from BASICS");

        assertEquals(2, book.pageCount(), "THAUMONOMICON keeps its original two pages");
        assertEquals(0, book.pageIndex());
        assertFalse(book.previousPage(), "the first page has nothing before it");

        final BookPageView first = book.frame().page();
        assertEquals("tc.research_page.THAUMONOMICON.1", first.textKey(),
                "the first page is the entry's original first page");
        assertEquals(0, first.index());
        assertEquals(2, first.count());
        assertTrue(first.revealed(), "a plain text page is always readable");

        assertTrue(book.nextPage(), "the second page must be reachable");
        assertEquals(1, book.pageIndex());
        assertFalse(book.nextPage(), "the last page has nothing after it");
        assertEquals(1, book.pageIndex(), "a refused move must not change the page");

        // RESEARCH holds the graph's longest BASICS page list.
        final ThaumonomiconBook longEntry = open();
        longEntry.selectEntry("RESEARCH");
        assertEquals(16, longEntry.pageCount(), "RESEARCH keeps its original 16 pages");
    }

    @Test
    void anEntryWithoutPagesReportsNoPages() {
        // CAP_iron is one of the graph's two page-less entries (and is virtual,
        // so it is not placed — but the original could still open it).
        final ThaumonomiconBook book = open();
        assertTrue(book.selectEntry("CAP_iron"), "a page-less entry still opens");
        assertEquals("THAUMATURGY", book.categoryKey(), "opening it follows its own category");
        assertEquals(0, book.pageCount(), "CAP_iron has no pages");
        final BookPageView page = book.frame().page();
        assertEquals(0, page.count());
        assertNull(page.kind(), "a page-less entry has no page type");
        assertFalse(book.nextPage(), "there is no page to turn to");
    }

    @Test
    void backReturnsToTheGraphView() {
        final ThaumonomiconBook book = open();
        assertFalse(book.back(), "there is nothing to go back to from the graph view");

        book.selectEntry("RESEARCH");
        assertFalse(book.frame().onGraph(), "an open entry is not the graph view");
        assertTrue(book.back(), "back must close the open entry");
        assertTrue(book.frame().onGraph());
        assertNull(book.frame().page(), "the graph view shows no page");
        assertEquals(0, book.pageCount(), "no entry is open any more");
    }

    @Test
    void selectingATabClearsTheOpenEntry() {
        final ThaumonomiconBook book = open();
        book.selectEntry("THAUMONOMICON");
        assertNotNull(book.openEntryKey());

        assertTrue(book.selectCategory("ALCHEMY"), "a registered category must be selectable");
        assertEquals("ALCHEMY", book.categoryKey());
        assertNull(book.openEntryKey(), "switching tabs leaves the entry's pages");
        assertEquals(0, book.pageIndex());
        assertTrue(book.frame().onGraph());
    }

    @Test
    void openingAnEntryFromAnotherCategoryFollowsItsTab() {
        final ThaumonomiconBook book = open();
        assertEquals("BASICS", book.categoryKey());

        assertTrue(book.selectEntry("FOCUSFIRE"), "a THAUMATURGY entry must open from BASICS");
        assertEquals("THAUMATURGY", book.categoryKey(), "opening it switches to its own tab");
        assertEquals("FOCUSFIRE", book.openEntryKey());
        assertEquals(3, book.pageCount(), "FOCUSFIRE keeps its original three pages");
    }

    @Test
    void unknownKeysAndOutOfRangeMovesAreRefused() {
        final ThaumonomiconBook book = open();

        assertFalse(book.selectCategory("NOSUCHCATEGORY"), "an unknown tab must be refused");
        assertEquals("BASICS", book.categoryKey(), "a refused tab move must not change the category");
        assertFalse(book.selectEntry("NOSUCHRESEARCH"), "an unknown entry must be refused");
        assertNull(book.openEntryKey(), "a refused entry move must not open anything");
        assertFalse(book.selectCategoryIndex(-1), "a negative tab index must be refused");
        assertFalse(book.selectCategoryIndex(CATEGORY_ORDER.size()), "an out-of-range tab index must be refused");
        assertTrue(book.selectCategoryIndex(CATEGORY_ORDER.size() - 1), "the last tab index is valid");
        assertEquals("ELDRITCH", book.categoryKey());
    }

    @Test
    void tabsCycleWithWrapAround() {
        final ThaumonomiconBook book = open();
        assertTrue(book.previousCategory(), "the tab scroll wraps backwards");
        assertEquals("ELDRITCH", book.categoryKey(), "from the first tab, back wraps to the last");

        assertTrue(book.nextCategory(), "the tab scroll wraps forwards");
        assertEquals("BASICS", book.categoryKey(), "from the last tab, forward wraps to the first");

        for (int i = 0; i < CATEGORY_ORDER.size(); i++) {
            assertTrue(book.nextCategory(), "each tab step is available");
        }
        assertEquals("BASICS", book.categoryKey(), "a full cycle returns to where it started");
    }

    @Test
    void openOnTheShippedGraphMatchesTheExtractedData() {
        final ResearchGraph graph = graph();
        assertEquals(6, graph.categoryCount(), "the shipped graph carries six categories");
        assertEquals(201, graph.entryCount(), "the shipped graph carries 201 entries");
        assertEquals(List.of(), graph.problems(), "the shipped graph is structurally clean");

        final ThaumonomiconBook book = open();
        final List<String> visited = new ArrayList<>();
        for (int i = 0; i < CATEGORY_ORDER.size(); i++) {
            book.selectCategoryIndex(i);
            visited.add(book.categoryKey());
        }
        assertEquals(CATEGORY_ORDER, visited, "every category is reachable through its tab");
    }

    private static BookNode node(final BookFrame frame, final String key) {
        return frame.nodes().stream().filter(node -> node.key().equals(key)).findFirst()
                .orElseThrow(() -> new AssertionError("no node " + key + " in the frame"));
    }
}
