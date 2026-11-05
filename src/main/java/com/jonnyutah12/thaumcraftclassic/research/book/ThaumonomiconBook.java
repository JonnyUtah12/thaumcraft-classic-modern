package com.jonnyutah12.thaumcraftclassic.research.book;

import com.jonnyutah12.thaumcraftclassic.research.ResearchCategoryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Thaumonomicon's page state machine (TC-M3-03-s): which category tab is
 * showing, which entry is open, which of its pages is being read, and what the
 * screen should therefore draw.
 *
 * <p>This is the presentation counterpart of the original
 * {@code thaumcraft.client.gui.GuiResearchBrowser} (content-mapping.csv
 * {@code thaumcraft:gui_thaumonomicon}, gui_id 12). It is deliberately free of
 * any client class — no {@code Screen}, no {@code GuiGraphics}, no
 * {@code Minecraft} — so a dedicated server can drive it in a game test, and so
 * the render code stays a thin consumer of {@link BookFrame} instead of a
 * second place where the graph rules live.</p>
 *
 * <p>Rules reproduced from the original browser:</p>
 * <ul>
 *   <li><b>Tabs</b> are the registered categories in registration order
 *       ({@code order} in {@code categories.json}); the book opens on the first
 *       one.</li>
 *   <li><b>Nodes</b> are the category's placed entries at their original
 *       {@code column}/{@code row}. {@code virtual} entries are dropped: the
 *       original registered them as prerequisites only, and
 *       {@code ResearchCategories.addResearch} never gave them a browser
 *       position. A {@code hidden} entry is drawn only once the player has
 *       revealed it (plain key or the {@code @}-key a clue leaves behind).</li>
 *   <li><b>Edges</b> are the drawn prerequisite lines: one per entry of the
 *       open category's {@code parents} list whose other end is a visible node
 *       of the same category. {@code parentsHidden} is never drawn — the
 *       original kept those out of the browser on purpose — and neither is a
 *       prerequisite from another category, because the browser shows one
 *       category at a time (BD-17).</li>
 *   <li><b>Pages</b> are the entry's original page list in order, one at a
 *       time. A {@code text_concealed} page stays concealed until its
 *       {@code requires} prerequisite is completed.</li>
 * </ul>
 *
 * <p>Every mutator returns {@code false} when the requested move is not
 * available (unknown tab, unknown entry, no next page), so the screen can use
 * the return value to decide whether a control is live, and the game test can
 * assert on the edges of the navigation domain.</p>
 */
public final class ThaumonomiconBook {

    private final ResearchGraph graph;
    private final ThaumonomiconKnowledge knowledge;
    private final List<ResearchCategoryData> orderedCategories;

    private int categoryIndex;
    private String openEntryKey;
    private int pageIndex;

    private ThaumonomiconBook(final ResearchGraph graph, final ThaumonomiconKnowledge knowledge) {
        this.graph = graph;
        this.knowledge = knowledge;
        this.orderedCategories = graph.categories().stream()
                .sorted(Comparator.comparingInt(ResearchCategoryData::order)
                        .thenComparing(ResearchCategoryData::key))
                .toList();
        if (this.orderedCategories.isEmpty()) {
            throw new IllegalArgumentException("research graph has no categories to browse");
        }
        this.categoryIndex = 0;
    }

    /**
     * The book as the shipped mod opens it: the loaded research graph
     * ({@link ResearchManager#graph()}) and a full-knowledge view of it.
     */
    public static ThaumonomiconBook open() {
        return open(ResearchManager.graph(), ThaumonomiconKnowledge.all());
    }

    /**
     * The book over an explicit graph and knowledge view — what the screen
     * passes (the client's synced research set) and what the game test passes.
     */
    public static ThaumonomiconBook open(final ResearchGraph graph, final ThaumonomiconKnowledge knowledge) {
        return new ThaumonomiconBook(graph, knowledge);
    }

    // ------------------------------------------------------------------ frame

    /** Everything the screen draws this frame. */
    public BookFrame frame() {
        final ResearchCategoryData category = orderedCategories.get(categoryIndex);
        final List<BookNode> nodes = nodesOf(category);
        return new BookFrame(tabs(), category.key(), category.nameKey(),
                nodes, edgesOf(nodes), page());
    }

    /** One tab per registered category, in original order, the active one flagged. */
    public List<BookTab> tabs() {
        final List<BookTab> tabs = new ArrayList<>(orderedCategories.size());
        for (int i = 0; i < orderedCategories.size(); i++) {
            final ResearchCategoryData category = orderedCategories.get(i);
            tabs.add(new BookTab(category.key(), category.nameKey(), category.order(),
                    category.icon(), i == categoryIndex));
        }
        return List.copyOf(tabs);
    }

    private List<BookNode> nodesOf(final ResearchCategoryData category) {
        final List<BookNode> nodes = new ArrayList<>();
        for (final ResearchEntryData entry : graph.entriesIn(category.key())) {
            if (isVirtual(entry)) {
                continue;
            }
            final boolean hidden = isHidden(entry);
            final boolean known = knowledge.knowsAny(entry.key());
            nodes.add(new BookNode(entry.key(), entry.nameKey(), entry.column(), entry.row(),
                    kindOf(entry), known, !hidden || known));
        }
        nodes.sort(Comparator.comparingInt(BookNode::row)
                .thenComparingInt(BookNode::column)
                .thenComparing(BookNode::key));
        return List.copyOf(nodes);
    }

    private List<BookEdge> edgesOf(final List<BookNode> nodes) {
        final Set<String> visible = new HashSet<>();
        for (final BookNode node : nodes) {
            if (node.visible()) {
                visible.add(node.key());
            }
        }
        final List<BookEdge> edges = new ArrayList<>();
        for (final BookNode child : nodes) {
            if (!child.visible()) {
                continue;
            }
            final ResearchEntryData entry = graph.entry(child.key());
            for (final String parent : nullSafe(entry.parents())) {
                if (visible.contains(parent)) {
                    edges.add(new BookEdge(parent, child.key()));
                }
            }
        }
        return List.copyOf(edges);
    }

    private BookPageView page() {
        if (openEntryKey == null) {
            return null;
        }
        final ResearchEntryData entry = graph.entry(openEntryKey);
        if (entry == null) {
            return null;
        }
        final List<ResearchPageData> pages = nullSafe(entry.pages());
        if (pages.isEmpty()) {
            return new BookPageView(entry.key(), entry.nameKey(), 0, 0, null, null, null, true);
        }
        final ResearchPageData page = pages.get(pageIndex);
        return new BookPageView(entry.key(), entry.nameKey(), pageIndex, pages.size(),
                page.type(), page.textKey(), page.requires(), isRevealed(page));
    }

    private boolean isRevealed(final ResearchPageData page) {
        if (page.type() != ResearchPageData.Type.TEXT_CONCEALED) {
            return true;
        }
        // The original reveals a concealed page only once its prerequisite is
        // completed; the page carries no other gate.
        return page.requires() != null && knowledge.knowsAny(page.requires());
    }

    // ------------------------------------------------------------- navigation

    /** Switch to a category tab by key; clears any open entry. */
    public boolean selectCategory(final String categoryKey) {
        for (int i = 0; i < orderedCategories.size(); i++) {
            if (orderedCategories.get(i).key().equals(categoryKey)) {
                return selectCategoryIndex(i);
            }
        }
        return false;
    }

    /** Switch to a category tab by its position in the tab bar. */
    public boolean selectCategoryIndex(final int index) {
        if (index < 0 || index >= orderedCategories.size()) {
            return false;
        }
        categoryIndex = index;
        openEntryKey = null;
        pageIndex = 0;
        return true;
    }

    /** Cycle to the next tab, wrapping at the end (the original's tab scroll). */
    public boolean nextCategory() {
        return selectCategoryIndex((categoryIndex + 1) % orderedCategories.size());
    }

    /** Cycle to the previous tab, wrapping at the start. */
    public boolean previousCategory() {
        return selectCategoryIndex((categoryIndex - 1 + orderedCategories.size()) % orderedCategories.size());
    }

    /**
     * Open an entry's page view. Any entry of the graph may be opened: clicking
     * a node is the usual route, and opening one from another category follows
     * the original's cross-category prerequisite jump by switching the tab.
     */
    public boolean selectEntry(final String key) {
        final ResearchEntryData entry = graph.entry(key);
        if (entry == null) {
            return false;
        }
        final int index = indexOfCategory(entry.category());
        if (index < 0) {
            return false;
        }
        categoryIndex = index;
        openEntryKey = key;
        pageIndex = 0;
        return true;
    }

    /** Turn to the next page of the open entry; false at the last page. */
    public boolean nextPage() {
        if (pageCount() == 0 || pageIndex + 1 >= pageCount()) {
            return false;
        }
        pageIndex++;
        return true;
    }

    /** Turn to the previous page of the open entry; false at the first page. */
    public boolean previousPage() {
        if (pageCount() == 0 || pageIndex == 0) {
            return false;
        }
        pageIndex--;
        return true;
    }

    /** Close the open entry and return to the graph view; false when already there. */
    public boolean back() {
        if (openEntryKey == null) {
            return false;
        }
        openEntryKey = null;
        pageIndex = 0;
        return true;
    }

    // -------------------------------------------------------------- accessors

    public ResearchGraph graph() {
        return graph;
    }

    public ThaumonomiconKnowledge knowledge() {
        return knowledge;
    }

    public int categoryIndex() {
        return categoryIndex;
    }

    /** The key of the category currently drawn. */
    public String categoryKey() {
        return orderedCategories.get(categoryIndex).key();
    }

    /** The entry whose pages are open, or null while on the graph view. */
    public String openEntryKey() {
        return openEntryKey;
    }

    /** The 0-based index of the page being read; 0 while on the graph view. */
    public int pageIndex() {
        return pageIndex;
    }

    /** Number of pages the open entry has (0 when no entry is open). */
    public int pageCount() {
        if (openEntryKey == null) {
            return 0;
        }
        final ResearchEntryData entry = graph.entry(openEntryKey);
        return entry == null ? 0 : nullSafe(entry.pages()).size();
    }

    // ---------------------------------------------------------------- helpers

    private int indexOfCategory(final String categoryKey) {
        for (int i = 0; i < orderedCategories.size(); i++) {
            if (orderedCategories.get(i).key().equals(categoryKey)) {
                return i;
            }
        }
        return -1;
    }

    private static BookNode.Kind kindOf(final ResearchEntryData entry) {
        if (isHidden(entry)) {
            return BookNode.Kind.HIDDEN;
        }
        return nullSafe(entry.parents()).isEmpty() ? BookNode.Kind.ROOT : BookNode.Kind.PARENTED;
    }

    private static boolean isVirtual(final ResearchEntryData entry) {
        return entry.flags() != null && entry.flags().virtual();
    }

    private static boolean isHidden(final ResearchEntryData entry) {
        return entry.flags() != null && entry.flags().hidden();
    }

    private static <T> List<T> nullSafe(final List<T> list) {
        return list == null ? List.of() : list;
    }
}
