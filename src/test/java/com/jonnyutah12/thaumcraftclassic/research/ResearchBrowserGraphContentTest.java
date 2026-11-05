package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BOOK-2 (t_10805b95): the book's category list and entry listing are rendered
 * <b>from the shipped research graph</b> — 6 categories / 201 entries — and
 * from nothing else.
 *
 * <p><b>The gate this test exists to fail.</b> Every entry in
 * {@code data/thaumcraft/research_graph/entries.json} must resolve to a
 * rendered row, and no row may render for an entry that is not in that file.
 * The expected set is therefore read out of the two JSON files at test time
 * with a raw {@link JsonParser} — never out of a list in this test and never
 * through {@code ResearchEntryData}, so the assertion is against the shipped
 * <em>data</em> and not against the loader that already parsed it. A
 * hardcoded id list here can never go red, and that trap is on record for this
 * repo (docs/porting-decisions.md D-018 §5: "the gate derives its expected
 * set from the registry", applied here to the graph, which is this feature's
 * registry).</p>
 *
 * <p><b>Why the model and not the screen.</b> {@code runGameTestServer} boots a
 * dedicated server: {@code Minecraft.getInstance()} is null and no
 * {@code Screen} is ever constructed, so the window cannot be measured there
 * (the same split {@code ThaumonomiconOpenPathContractTest} documents). The
 * rows, tabs, pages and selection are therefore computed in
 * {@link ResearchBrowserModel}, which imports <b>no</b> Minecraft type: the
 * listing is one plain-JVM object that the screen draws verbatim and the game
 * probes measure directly. Asserting against the PORT's classes is the posture
 * D-018 requires — 1.7.10's browser is immediate-mode GL ({@code Tessellator},
 * {@code GL11}) with no 1.21.1 equivalent to javap against.</p>
 *
 * <p>Line cites are to the original
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchBrowser.java}.</p>
 */
class ResearchBrowserGraphContentTest {

    /** The shipped data, as a resource (the jar's own bytes, on the test classpath). */
    private static final String CATEGORIES_RESOURCE = "/data/thaumcraft/research_graph/categories.json";
    private static final String ENTRIES_RESOURCE = "/data/thaumcraft/research_graph/entries.json";

    /** A key that cannot be in the graph: the graph's keys are upper-case research keys. */
    private static final String NOT_A_GRAPH_KEY = "t10805b95_not_in_the_graph";

    // ------------------------------------------------------------------ the tabs

    @Test
    void tabsAreTheGraphCategoriesInRegistrationOrder() {
        final ShippedGraph graph = ShippedGraph.read();
        final ResearchBrowserModel model = new ResearchBrowserModel(ResearchGraphLoader.loadDefault());

        final List<ResearchBrowserModel.Tab> tabs = model.tabs();
        assertEquals(graph.categoryKeys.size(), tabs.size(),
                "the tab strip must render one tab per category in the shipped categories.json, got "
                        + keys(tabs));

        final List<String> tabKeys = new ArrayList<>();
        for (final ResearchBrowserModel.Tab tab : tabs) {
            tabKeys.add(tab.key());
        }
        // categories.json is written in registration order and carries `order`;
        // both must agree, and the model must follow the data, not a list.
        assertEquals(graph.categoryKeys, tabKeys,
                "the tab strip order must be the categories.json order (the original iterates "
                        + "ResearchCategories.researchCategories.keySet(), GuiResearchBrowser:480-491)");

        for (final ResearchBrowserModel.Tab tab : tabs) {
            final int expectedOrder = graph.categoryOrder.get(tab.key());
            assertEquals(expectedOrder, tab.order(),
                    "tab " + tab.key() + " must carry the order field from categories.json");
            assertEquals(graph.categoryLabel.get(tab.key()), tab.label(),
                    "tab " + tab.key() + " must be labelled with the graph's own display name (D-016)");
            final int expectedEntries = graph.entriesOfCategory(tab.key()).size();
            assertEquals(expectedEntries, tab.entryCount(),
                    "tab " + tab.key() + " must count its own entries out of entries.json");
        }
    }

    // ------------------------------------------------------------------ the rows

    @Test
    void everyGraphEntryResolvesToARowAndNothingThatIsNotInTheGraphDoes() {
        final ShippedGraph graph = ShippedGraph.read();
        final ResearchBrowserModel model = new ResearchBrowserModel(ResearchGraphLoader.loadDefault());

        final List<String> rowKeys = new ArrayList<>();
        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            for (final ResearchBrowserModel.Row row : model.rows(tab.key())) {
                rowKeys.add(row.key());
                assertEquals(tab.key(), row.category(),
                        "row " + row.key() + " is listed under the wrong category tab");
                assertFalse(row.label().isBlank(),
                        "row " + row.key() + " renders no label at all");
            }
        }

        // Every entry in the JSON has a row.
        assertEquals(new LinkedHashSet<>(graph.entryKeys).size(), graph.entryKeys.size(),
                "entries.json carries duplicate keys; the data must be the source of truth");
        assertEquals(graph.entryKeys.size(), model.totalRows(),
                "the listing must show one row per entry in entries.json (" + graph.entryKeys.size()
                        + " rows), got " + model.totalRows());
        assertEquals(new HashSet<>(graph.entryKeys), new HashSet<>(rowKeys),
                "the rendered row set is not exactly the entry set of the shipped graph");
        assertEquals(graph.entryKeys.size(), rowKeys.size(),
                "a row rendered twice — the listing is not a bijection with entries.json");

        // And each row's label is that entry's own display string from the JSON
        // (the graph's verbatim en_US quotation, D-016), falling back to the key
        // for the 2 entries the original left unnamed.
        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            for (final ResearchBrowserModel.Row row : model.rows(tab.key())) {
                final String fromJson = graph.entryLabel.get(row.key());
                final String expected = fromJson == null || fromJson.isBlank() ? row.key() : fromJson;
                assertEquals(expected, row.label(),
                        "row " + row.key() + " must be labelled from entries.json, not from a list");
            }
        }
    }

    @Test
    void pagesCoverEveryRowOfTheirCategoryExactlyOnce() {
        final ShippedGraph graph = ShippedGraph.read();
        final ResearchBrowserModel model = new ResearchBrowserModel(ResearchGraphLoader.loadDefault());

        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            final List<ResearchBrowserModel.Row> all = model.rows(tab.key());
            final int pages = model.pageCount(tab.key());
            assertEquals((all.size() + ResearchBrowserModel.ROWS_PER_PAGE - 1) / ResearchBrowserModel.ROWS_PER_PAGE,
                    pages, "page count for " + tab.key() + " must follow the row count, not a fixed book depth");

            final List<String> paged = new ArrayList<>();
            for (int page = 0; page < pages; page++) {
                final List<ResearchBrowserModel.Row> window = model.page(tab.key(), page);
                assertTrue(window.size() <= ResearchBrowserModel.ROWS_PER_PAGE,
                        "page " + page + " of " + tab.key() + " overflows the row window");
                for (final ResearchBrowserModel.Row row : window) {
                    paged.add(row.key());
                }
            }
            assertEquals(keysOf(all), paged,
                    "paging " + tab.key() + " must cover every row exactly once, in listing order");
        }
    }

    // ------------------------------------------------------------- navigation

    @Test
    void navigationMovesBetweenCategoriesAndBetweenEntries() {
        final ShippedGraph graph = ShippedGraph.read();
        final ResearchBrowserModel model = new ResearchBrowserModel(ResearchGraphLoader.loadDefault());

        // Every category tab is reachable, in the data's own order.
        for (final String categoryKey : graph.categoryKeys) {
            assertTrue(model.selectCategory(categoryKey),
                    "the tab for " + categoryKey + " must switch the listing (GuiResearchBrowser:757-761)");
            assertEquals(categoryKey, model.selectedCategory(),
                    "the book must remember the category tab it switched to (:63)");
            assertFalse(model.rows(categoryKey).isEmpty(),
                    "the " + categoryKey + " tab renders no entries at all");
        }

        // Every entry row is selectable, and selecting it shows that entry's own text.
        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            model.selectCategory(tab.key());
            for (final ResearchBrowserModel.Row row : model.rows(tab.key())) {
                assertTrue(model.selectEntry(row.key()),
                        "the row " + row.key() + " must open the entry (the original opens "
                                + "GuiResearchRecipe for a completed node, :711)");
                assertEquals(row.key(), model.selectedEntry(), "the book must remember the selected entry");
                assertEquals(row.key(), model.selectedRow().key(), "selectedRow must be the selected entry");
                final String expectedText = graph.entryText.get(row.key());
                if (expectedText == null || expectedText.isBlank()) {
                    assertTrue(model.selectedText().isBlank(),
                            "the book invented text for " + row.key() + ", which the graph has none for");
                } else {
                    assertEquals(expectedText, model.selectedText(),
                            "the entry pane must show the text entries.json carries for " + row.key());
                }
            }
        }

        // Page navigation stays inside the category's own page count.
        final String busiest = graph.busiestCategory();
        model.selectCategory(busiest);
        final int pages = model.pageCount(busiest);
        assertTrue(pages > 1, "the busiest category of the shipped graph must paginate, got " + pages);
        int forward = 0;
        while (model.nextPage()) {
            forward++;
        }
        assertEquals(pages - 1, forward, "nextPage must stop at the last page of " + busiest);
        assertEquals(pages - 1, model.page());
        int back = 0;
        while (model.prevPage()) {
            back++;
        }
        assertEquals(pages - 1, back, "prevPage must stop at the first page of " + busiest);
        assertEquals(0, model.page());
    }

    @Test
    void aKeyAbsentFromTheGraphResolvesToNoRowAndNoSelection() {
        final ShippedGraph graph = ShippedGraph.read();
        final ResearchBrowserModel model = new ResearchBrowserModel(ResearchGraphLoader.loadDefault());
        assertFalse(graph.entryKeys.contains(NOT_A_GRAPH_KEY), "the probe key must not be in the shipped graph");

        final String firstCategory = graph.categoryKeys.get(0);
        model.selectCategory(firstCategory);
        final String realKey = model.rows(firstCategory).get(0).key();
        model.selectEntry(realKey);

        assertNull(model.row(NOT_A_GRAPH_KEY),
                "a research key that is not in the graph must not resolve to a rendered row");
        assertFalse(model.selectEntry(NOT_A_GRAPH_KEY),
                "selecting an entry the graph does not have must be refused, so no row can render for it");
        assertEquals(realKey, model.selectedEntry(), "the refused selection must not move the book");
        assertFalse(model.selectCategory(NOT_A_GRAPH_KEY),
                "the tab strip may not switch to a category the graph does not have");
        assertEquals(firstCategory, model.selectedCategory(), "the refused tab switch must not move the book");
        for (final ResearchBrowserModel.Tab tab : model.tabs()) {
            for (final ResearchBrowserModel.Row row : model.rows(tab.key())) {
                assertFalse(NOT_A_GRAPH_KEY.equals(row.key()), "a row rendered for an entry absent from the graph");
            }
        }
    }

    @Test
    void theModelReadsTheShippedGraphAndNotACopyOfIt() {
        final ShippedGraph graph = ShippedGraph.read();
        final ResearchBrowserModel model = ResearchBrowserModel.forShippedGraph();
        assertEquals(graph.categoryKeys.size(), model.tabs().size(),
                "the shipped-graph model must expose the shipped categories.json");
        assertEquals(graph.entryKeys.size(), model.totalRows(),
                "the shipped-graph model must expose every entry of the shipped graph");
        assertEquals(graph.categoryKeys, keys(model.tabs()),
                "the shipped-graph tab strip must be the categories.json order");
    }

    // ---------------------------------------------------- no hardcoded listings

    @Test
    void theBrowserSourcesCarryNoHardcodedGraphKeys() throws Exception {
        final ShippedGraph graph = ShippedGraph.read();
        final Map<String, String> sources = new LinkedHashMap<>();
        sources.put("research/ResearchBrowserModel.java",
                stripComments(readSource("research/ResearchBrowserModel.java")));
        sources.put("client/ThaumonomiconScreen.java",
                stripComments(readSource("client/ThaumonomiconScreen.java")));

        final List<String> keys = new ArrayList<>(graph.categoryKeys);
        keys.addAll(graph.entryKeys);
        for (final Map.Entry<String, String> source : sources.entrySet()) {
            for (final String key : keys) {
                assertFalse(source.getValue().contains("\"" + key + "\""),
                        source.getKey() + " hardcodes the graph key \"" + key
                                + "\"; the listing must come from the graph data");
            }
        }
    }

    // ---------------------------------------------------------------- plumbing

    private static List<String> keys(final List<ResearchBrowserModel.Tab> tabs) {
        final List<String> out = new ArrayList<>();
        for (final ResearchBrowserModel.Tab tab : tabs) {
            out.add(tab.key());
        }
        return out;
    }

    private static List<String> keysOf(final List<ResearchBrowserModel.Row> rows) {
        final List<String> out = new ArrayList<>();
        for (final ResearchBrowserModel.Row row : rows) {
            out.add(row.key());
        }
        return out;
    }

    /**
     * The shipped graph, read straight out of the two JSON files with a raw
     * parser: the expected set the gate compares against is the data, not the
     * loader's view of it.
     */
    private record ShippedGraph(List<String> categoryKeys,
                                Map<String, Integer> categoryOrder,
                                Map<String, String> categoryLabel,
                                List<String> entryKeys,
                                Map<String, String> entryCategory,
                                Map<String, String> entryLabel,
                                Map<String, String> entryText) {

        List<String> entriesOfCategory(final String category) {
            final List<String> out = new ArrayList<>();
            for (final String key : entryKeys) {
                if (category.equals(entryCategory.get(key))) {
                    out.add(key);
                }
            }
            return out;
        }

        String busiestCategory() {
            String best = categoryKeys.get(0);
            for (final String category : categoryKeys) {
                if (entriesOfCategory(category).size() > entriesOfCategory(best).size()) {
                    best = category;
                }
            }
            return best;
        }

        static ShippedGraph read() {
            final JsonObject categories = readResource(CATEGORIES_RESOURCE);
            final JsonObject entries = readResource(ENTRIES_RESOURCE);

            final List<String> categoryKeys = new ArrayList<>();
            final Map<String, Integer> categoryOrder = new LinkedHashMap<>();
            final Map<String, String> categoryLabel = new LinkedHashMap<>();
            for (final JsonElement element : categories.getAsJsonArray("categories")) {
                final JsonObject category = element.getAsJsonObject();
                final String key = category.get("key").getAsString();
                categoryKeys.add(key);
                categoryOrder.put(key, category.get("order").getAsInt());
                categoryLabel.put(key, category.get("nameEnUs").getAsString());
            }

            final List<String> entryKeys = new ArrayList<>();
            final Map<String, String> entryCategory = new LinkedHashMap<>();
            final Map<String, String> entryLabel = new LinkedHashMap<>();
            final Map<String, String> entryText = new LinkedHashMap<>();
            final JsonArray entryArray = entries.getAsJsonArray("entries");
            for (final JsonElement element : entryArray) {
                final JsonObject entry = element.getAsJsonObject();
                final String key = entry.get("key").getAsString();
                entryKeys.add(key);
                entryCategory.put(key, entry.get("category").getAsString());
                entryLabel.put(key, text(entry, "nameEnUs"));
                entryText.put(key, text(entry, "textEnUs"));
            }
            // The file's own declared count is part of the contract; a stale
            // count is a data bug the gate must not paper over.
            assertEquals(entries.get("entryCount").getAsInt(), entryKeys.size(),
                    "entries.json declares an entryCount that its array does not contain");

            return new ShippedGraph(categoryKeys, categoryOrder, categoryLabel,
                    entryKeys, entryCategory, entryLabel, entryText);
        }

        private static String text(final JsonObject entry, final String field) {
            final JsonElement value = entry.get(field);
            return value == null || value.isJsonNull() ? null : value.getAsString();
        }
    }

    private static JsonObject readResource(final String path) {
        try (Reader reader = new InputStreamReader(
                ResearchBrowserGraphContentTest.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            assertNotNull(reader, path + " must be on the test classpath (the shipped data)");
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    /**
     * The comments stripped out, so the literal scan below targets the
     * executable code. The javadoc of these classes quotes the original's own
     * source (the gate entry of the category the original hides until it is
     * unlocked, the lang keys research text is keyed by); a documentation
     * citation is not a hardcoded listing, and the docs are where that
     * provenance has to be stated.
     */
    private static String stripComments(final String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    /** The source of a shipped class, read from the repo the harness runs in. */
    private static String readSource(final String relativeUnderModPackage) throws Exception {
        final String suffix = "src/main/java/com/jonnyutah12/thaumcraftclassic/" + relativeUnderModPackage;
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        Path root = csv.getParent() == null ? Path.of(System.getProperty("user.dir")) : csv.getParent().getParent();
        Path source = root.resolve(suffix);
        if (!Files.isRegularFile(source)) {
            Path walk = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            for (int i = 0; i < 6 && !Files.isRegularFile(source); i++) {
                source = walk.resolve(suffix);
                walk = walk.getParent();
            }
        }
        assertTrue(Files.isRegularFile(source), "expected the source at " + source);
        return Files.readString(source, StandardCharsets.UTF_8);
    }
}
