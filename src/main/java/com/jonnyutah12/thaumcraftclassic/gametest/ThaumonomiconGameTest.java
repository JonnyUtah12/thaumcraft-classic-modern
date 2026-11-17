package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.book.BookEdge;
import com.jonnyutah12.thaumcraftclassic.research.book.BookFrame;
import com.jonnyutah12.thaumcraftclassic.research.book.BookNode;
import com.jonnyutah12.thaumcraftclassic.research.book.BookPageView;
import com.jonnyutah12.thaumcraftclassic.research.book.BookTab;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconBook;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconHost;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconKnowledge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

/**
 * TC-M3-03-s acceptance: the Thaumonomicon book screen opens and renders the
 * shipped research graph (TC-M3-01/02 data).
 *
 * <p>The game-test server is a dedicated server, so it cannot construct a
 * {@code Screen} — that is the point of the
 * {@link ThaumonomiconHost} seam. The probe installs a recording host, drives
 * the real open path the client uses, and asserts on the
 * {@link BookFrame} the host was handed: the frame is exactly what the client
 * screen draws, so "the screen opened with the graph on it" is proven without a
 * client in the loop.</p>
 *
 * <ul>
 *   <li>{@link #bookOpensAndRendersTheGraph}: the open seam fires, the frame
 *       carries one tab per registered category with the first selected, the
 *       placed nodes of that category at their original browser coordinates,
 *       and the drawn prerequisite lines among them.</li>
 *   <li>{@link #bookNavigationReadsTheShippedGraph}: tab switching, opening an
 *       entry, its original page list as page text, and back to the graph.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumonomiconGameTest {

    private static final List<String> CATEGORY_ORDER =
            List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH");

    private ThaumonomiconGameTest() {
    }

    @GameTest(template = "empty")
    public static void bookOpensAndRendersTheGraph(final GameTestHelper helper) {
        final RecordingHost host = new RecordingHost();
        ThaumonomiconHost.install(host);
        try {
            // Nothing can display the book before a client installs a host.
            helper.assertTrue(ThaumonomiconHost.installed() == host,
                    "the installed host must be the one the probe reads");

            final ThaumonomiconBook book = ThaumonomiconBook.open();
            helper.assertTrue(ThaumonomiconHost.openBook(book),
                    "opening the book through the host must report success");
            helper.assertTrue(host.opens == 1 && host.last == book,
                    "the host must be handed the book that was opened");

            final BookFrame frame = book.frame();
            helper.assertTrue(!frame.tabs().isEmpty(), "the book must open with category tabs");
            helper.assertTrue(frame.tabs().size() == CATEGORY_ORDER.size(),
                    "one tab per registered category, got " + frame.tabs().size());
            final List<String> tabKeys = frame.tabs().stream().map(BookTab::categoryKey).toList();
            helper.assertTrue(CATEGORY_ORDER.equals(tabKeys),
                    "tabs must follow the original registration order, got " + tabKeys);
            helper.assertTrue(frame.selectedTab() != null
                            && "BASICS".equals(frame.selectedTab().categoryKey()),
                    "the book must open on the first registered category");
            helper.assertTrue("BASICS".equals(frame.categoryKey()),
                    "the drawn category must be the selected tab, got " + frame.categoryKey());

            // The graph itself: the category's placed entries at their original
            // browser coordinates, and the drawn prerequisite lines between them.
            final var graph = ResearchManager.graph();
            final List<BookNode> nodes = frame.nodes();
            helper.assertTrue(nodes.size() == 19,
                    "BASICS carries 19 placed entries in the shipped graph, got " + nodes.size());
            for (final BookNode node : nodes) {
                final var entry = graph.entry(node.key());
                helper.assertTrue(entry != null,
                        "a drawn node must be a real research entry: " + node.key());
                helper.assertTrue(entry.column() == node.column() && entry.row() == node.row(),
                        "node " + node.key() + " must keep its original browser position");
                helper.assertTrue(node.nameKey() != null && node.nameKey().equals(entry.nameKey()),
                        "node " + node.key() + " must carry the original name key");
            }
            final Set<String> nodeKeys = nodes.stream().map(BookNode::key)
                    .collect(java.util.stream.Collectors.toSet());
            final List<BookEdge> edges = frame.edges();
            helper.assertTrue(edges.size() == 10,
                    "BASICS draws 10 prerequisite lines in the shipped graph, got " + edges.size());
            for (final BookEdge edge : edges) {
                helper.assertTrue(nodeKeys.contains(edge.parentKey()) && nodeKeys.contains(edge.childKey()),
                        "an edge must join two nodes of the drawn category: " + edge);
                helper.assertTrue(graph.entry(edge.childKey()).parents().contains(edge.parentKey()),
                        "an edge must be a drawn prerequisite of its child: " + edge);
            }
            helper.succeed();
        } finally {
            ThaumonomiconHost.uninstall();
        }
    }

    @GameTest(template = "empty")
    public static void bookNavigationReadsTheShippedGraph(final GameTestHelper helper) {
        final RecordingHost host = new RecordingHost();
        ThaumonomiconHost.install(host);
        try {
            // Browse the graph with full knowledge, then with none: hidden
            // entries may only be drawn once the player has revealed them.
            final ThaumonomiconBook book = ThaumonomiconBook.open();
            // Full knowledge: BASICS' single hidden entry is revealed along with
            // the rest, so all 19 placed entries are drawn.
            helper.assertTrue(book.frame().visibleNodes().size() == 19,
                    "BASICS draws all 19 of its placed entries with full knowledge, got "
                            + book.frame().visibleNodes().size());

            final ThaumonomiconBook blind = ThaumonomiconBook.open(
                    ResearchManager.graph(), ThaumonomiconKnowledge.none());
            helper.assertTrue(blind.frame().visibleNodes().size() == 18,
                    "a player who knows nothing still sees the category's unhidden entries");
            helper.assertTrue(blind.frame().nodes().stream().anyMatch(n -> !n.visible()),
                    "the category's hidden entry must not be drawn for a player who has not revealed it");
            final ThaumonomiconBook revealed = ThaumonomiconBook.open(ResearchManager.graph(),
                    ThaumonomiconKnowledge.of(Set.of("@CRIMSON")));
            helper.assertTrue(revealed.frame().nodes().stream().anyMatch(n -> n.key().equals("CRIMSON") && n.visible()),
                    "revealing a hidden entry's @-key must place it on the graph");

            // Tab switching draws the next category's own nodes.
            helper.assertTrue(book.nextCategory(), "the book must be able to turn to the next tab");
            helper.assertTrue("THAUMATURGY".equals(book.categoryKey()),
                    "the second tab is THAUMATURGY, got " + book.categoryKey());
            helper.assertTrue(book.frame().nodes().size() == 40,
                    "THAUMATURGY carries 40 placed entries, got " + book.frame().nodes().size());
            helper.assertTrue(book.frame().onGraph(), "switching tabs returns to the graph view");

            // Opening an entry shows its original page list as page text.
            helper.assertTrue(book.selectEntry("BASICTHAUMATURGY"),
                    "a placed entry of the drawn category must open");
            final BookFrame page = book.frame();
            helper.assertTrue(!page.onGraph() && page.page() != null, "an open entry must show a page");
            final BookPageView first = page.page();
            helper.assertTrue("tc.research_name.BASICTHAUMATURGY".equals(first.titleKey()),
                    "the page heading is the entry's original name key, got " + first.titleKey());
            helper.assertTrue("tc.research_page.BASICTHAUMATURGY.1".equals(first.textKey()),
                    "the first page holds the entry's original first page text, got " + first.textKey());
            helper.assertTrue(first.count() == 4 && first.index() == 0,
                    "the entry's original 4-page list starts at page 1, got index " + first.index()
                            + " of " + first.count());
            helper.assertTrue(page.page().revealed(), "a plain text page is readable");

            helper.assertTrue(book.nextPage() && book.pageIndex() == 1,
                    "the next-page move must advance one page");
            helper.assertTrue(book.back() && book.frame().onGraph(),
                    "back must return to the graph view");
            helper.assertTrue(!book.back(), "back from the graph view must report nothing to do");

            helper.assertTrue(host.opens == 0,
                    "navigating an already-open book must not re-open it");
            helper.succeed();
        } finally {
            ThaumonomiconHost.uninstall();
        }
    }

    /** A stand-in for the client's display: it records what it was asked to show. */
    private static final class RecordingHost implements ThaumonomiconHost {

        private int opens;
        private ThaumonomiconBook last;

        @Override
        public void open(final ThaumonomiconBook book) {
            this.opens++;
            this.last = book;
        }
    }
}
