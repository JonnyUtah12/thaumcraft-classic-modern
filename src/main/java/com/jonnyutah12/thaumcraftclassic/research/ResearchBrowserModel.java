package com.jonnyutah12.thaumcraftclassic.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Thaumonomicon browser's listing: the category tabs and the entry rows of
 * the selected tab, computed <b>entirely out of the shipped research graph</b>
 * ({@code data/thaumcraft/research_graph/{categories,entries}.json}, 6
 * categories / 201 entries). There is no list of categories and no list of
 * entries in this class: change the JSON and the book changes.
 *
 * <p><b>Why a plain object.</b> The original browser builds its node list in
 * {@code GuiResearchBrowser.updateResearch()}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchBrowser.java:94-113}):
 * it clears a {@code LinkedList<ResearchItem>}, defaults the category to the
 * first key of {@code ResearchCategories.researchCategories} when none is
 * selected ({@code :96-99}), and fills the list from
 * {@code ResearchCategories.getResearchList(selectedCategory).research.values()}
 * ({@code :101-103}) — a list read out of the registry, exactly the shape this
 * class reproduces. In 1.7.10 that code sits inside a {@code GuiScreen} that
 * draws with {@code Tessellator}/{@code GL11}, so it can only be measured on
 * a client; {@code runGameTestServer} is a dedicated server and can never
 * construct a {@code Screen}. This class therefore imports <b>no</b> Minecraft
 * type, which is what makes the listing measurable: the game probes
 * ({@code ThaumonomiconBookGameTest}) build it on the server against the live
 * graph, and {@code ResearchBrowserGraphContentTest} gates it against the raw
 * JSON. Per D-018 the assertions target the PORT's classes.</p>
 *
 * <p><b>Rows, not a pannable map.</b> The original lays the same list out as a
 * pannable node grid (24 px pitch, {@code guiMapX/guiMapY}, {@code :105-113},
 * {@code :269-330}), a GL canvas this port does not have. The port lists the
 * identical rows in a paginated column of {@link #ROWS_PER_PAGE} rows and
 * keeps the original's tab strip as the category switch (24x24 tabs at
 * {@code paneLeft - 24}, {@code paneTop + i * 24}, {@code :480-530}; the click
 * test and switch are {@code :745-765}). The node art, the hover popups
 * ({@code GuiResearchPopup}) and the recipe pages ({@code GuiResearchRecipe})
 * stay BD-13; the row text is the graph's own display strings.</p>
 *
 * <p><b>All six tabs render.</b> The original hides the {@code ELDRITCH} tab
 * until the player has completed its gate entry
 * ({@code :491}, {@code :743}: the tab loop skips the category unless
 * {@code ResearchManager.isResearchComplete(player, "ELDRITCHMINOR")}). The
 * port shows every tab of the graph from the start: this card's contract is
 * that every entry of the graph is reachable as a row, and a tab the player
 * cannot see hides 16 entries. The research <em>unlock</em> gate is untouched
 * (it lives in {@code ResearchManager}); BD-13 records the display difference.</p>
 *
 * <p><b>Labels are data.</b> A row is labelled with the entry's own
 * {@code nameEnUs} from the JSON — the original en_US string, committed verbatim
 * as quotation under D-016 and shipped in the public jar — and falls back to
 * the research key for the entries the original left unnamed. The same rule
 * gives the tab labels ({@code nameEnUs} of the category) and the entry pane's
 * text ({@code textEnUs}). No research lang key is minted here: the original's
 * {@code tc.research_name.*} / {@code tc.research_text.*} keys are not in the
 * port's lang file, so per-locale translation of research text is a follow-up
 * card, stated in the docs and not silently skipped.</p>
 */
public final class ResearchBrowserModel {

    /**
     * Rows drawn per page. Port-authored: the original scrolls a node grid
     * instead of paginating, and the 1.21.1 book pane is 256x230
     * ({@code GuiResearchBrowser.java:46-47}), which fits a dozen 11 px rows
     * above the entry pane.
     */
    public static final int ROWS_PER_PAGE = 12;

    /**
     * One category tab: the graph's key, its display name, its registration
     * order and how many entries the graph has under it.
     */
    public record Tab(String key, String label, int order, int entryCount) {
    }

    /**
     * One rendered entry row: the graph's key, its display label, the category
     * tab it is listed under and its position in that tab's listing.
     */
    public record Row(String key, String label, String category, int index) {
    }

    private final ResearchGraph graph;
    private final List<Tab> tabs;
    private final Map<String, List<Row>> rowsByCategory;
    private final Map<String, Row> rowsByKey;

    private String selectedCategory;
    private String selectedEntry;
    private int page;

    public ResearchBrowserModel(final ResearchGraph graph) {
        this.graph = graph;

        // Tabs come from the graph's categories, in the registration order the
        // data carries (`order`), which is the order of the original's
        // ResearchCategories.researchCategories keySet (:480).
        final List<ResearchCategoryData> categories = new ArrayList<>(graph.categories());
        categories.sort(Comparator.comparingInt(ResearchCategoryData::order)
                .thenComparing(ResearchCategoryData::key));
        final List<Tab> builtTabs = new ArrayList<>();
        final Map<String, List<Row>> builtRows = new LinkedHashMap<>();
        final Map<String, Row> builtIndex = new LinkedHashMap<>();
        for (final ResearchCategoryData category : categories) {
            // The row list is the category's own list, in the graph's order —
            // updateResearch's `for (res : getResearchList(cat).research.values())`
            // (:101-103), no filter and no re-sort.
            final List<Row> rows = new ArrayList<>();
            for (final ResearchEntryData entry : graph.entriesIn(category.key())) {
                final Row row = new Row(entry.key(), display(entry.nameEnUs(), entry.key()),
                        category.key(), rows.size());
                rows.add(row);
                builtIndex.put(row.key(), row);
            }
            builtRows.put(category.key(), List.copyOf(rows));
            builtTabs.add(new Tab(category.key(), display(category.nameEnUs(), category.key()),
                    category.order(), rows.size()));
        }
        this.tabs = List.copyOf(builtTabs);
        this.rowsByCategory = Map.copyOf(builtRows);
        this.rowsByKey = Map.copyOf(builtIndex);
        this.selectedCategory = this.tabs.isEmpty() ? "" : this.tabs.get(0).key();
    }

    /** The model over the graph this jar ships. */
    public static ResearchBrowserModel forShippedGraph() {
        return new ResearchBrowserModel(ResearchManager.graph());
    }

    /** The graph this listing was built from. */
    public ResearchGraph graph() {
        return this.graph;
    }

    /** Every category tab, in the graph's registration order. */
    public List<Tab> tabs() {
        return this.tabs;
    }

    /** Every entry row of one category tab, in the graph's order. */
    public List<Row> rows(final String categoryKey) {
        final List<Row> rows = this.rowsByCategory.get(categoryKey);
        return rows == null ? List.of() : rows;
    }

    /** How many pages the tab's listing occupies. */
    public int pageCount(final String categoryKey) {
        final int rows = rows(categoryKey).size();
        return rows == 0 ? 0 : (rows + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
    }

    /** The rows of one page of a tab, clamped to the tab's own page count. */
    public List<Row> page(final String categoryKey, final int pageIndex) {
        final List<Row> rows = rows(categoryKey);
        final int pages = pageCount(categoryKey);
        if (rows.isEmpty() || pages == 0) {
            return List.of();
        }
        final int page = Math.max(0, Math.min(pageIndex, pages - 1));
        final int from = page * ROWS_PER_PAGE;
        return rows.subList(from, Math.min(from + ROWS_PER_PAGE, rows.size()));
    }

    /** The row for a research key, or null when the graph has no such entry. */
    public Row row(final String entryKey) {
        return entryKey == null ? null : this.rowsByKey.get(entryKey);
    }

    /** The rows the whole book renders: one per entry of the graph. */
    public int totalRows() {
        return this.rowsByKey.size();
    }

    public String selectedCategory() {
        return this.selectedCategory;
    }

    public String selectedEntry() {
        return this.selectedEntry;
    }

    /** The selected entry's row (null while nothing is selected). */
    public Row selectedRow() {
        return this.selectedEntry == null ? null : row(this.selectedEntry);
    }

    /** The page of the selected tab's listing currently on screen. */
    public int page() {
        return this.page;
    }

    /**
     * The text the entry pane shows: the entry's own hover text from the graph,
     * falling back to its first page's text, and blank when the graph carries
     * neither. The book never invents copy.
     */
    public String selectedText() {
        final ResearchEntryData entry = this.selectedEntry == null
                ? null : this.graph.entry(this.selectedEntry);
        if (entry == null) {
            return "";
        }
        if (entry.textEnUs() != null && !entry.textEnUs().isBlank()) {
            return entry.textEnUs();
        }
        for (final ResearchPageData pageData : entry.pages() == null
                ? List.<ResearchPageData>of() : entry.pages()) {
            if (pageData.textEnUs() != null && !pageData.textEnUs().isBlank()) {
                return pageData.textEnUs();
            }
        }
        return "";
    }

    /**
     * Switch to a category tab: the graph must have it. The original's tab
     * click is the same act ({@code :757-761} sets {@code selectedCategory} and
     * calls {@code updateResearch()}).
     *
     * @return false when the graph has no such category, leaving the book put
     */
    public boolean selectCategory(final String categoryKey) {
        if (categoryKey == null || this.graph.category(categoryKey) == null) {
            return false;
        }
        if (!categoryKey.equals(this.selectedCategory)) {
            this.selectedCategory = categoryKey;
            this.page = 0;
            this.selectedEntry = null;
        }
        return true;
    }

    /**
     * Open an entry row. The row set is exactly the graph's entries, so a key
     * the graph does not carry is refused here and can never render a row.
     *
     * @return false when the graph has no such entry, leaving the book put
     */
    public boolean selectEntry(final String entryKey) {
        final Row row = row(entryKey);
        if (row == null) {
            return false;
        }
        this.selectedEntry = row.key();
        if (!row.category().equals(this.selectedCategory)) {
            // Opening an entry the book is not looking at moves the listing to
            // its own tab, the same way the original's node lives on one map.
            this.selectedCategory = row.category();
            this.page = row.index() / ROWS_PER_PAGE;
        }
        return true;
    }

    /**
     * Turn to the next page of the selected tab.
     *
     * @return false on the last page (the listing has no more rows)
     */
    public boolean nextPage() {
        if (this.page + 1 >= pageCount(this.selectedCategory)) {
            return false;
        }
        this.page++;
        return true;
    }

    /**
     * Turn back.
     *
     * @return false on the first page
     */
    public boolean prevPage() {
        if (this.page <= 0) {
            return false;
        }
        this.page--;
        return true;
    }

    /**
     * Land on the page of the listing that shows an entry (used when the book
     * opens on a remembered entry's tab).
     */
    public void focusPageOf(final String entryKey) {
        final Row row = row(entryKey);
        if (row != null) {
            this.page = row.index() / ROWS_PER_PAGE;
        }
    }

    private static String display(final String fromGraph, final String key) {
        return fromGraph == null || fromGraph.isBlank() ? key : fromGraph;
    }
}
