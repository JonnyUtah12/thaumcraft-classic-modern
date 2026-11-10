package com.jonnyutah12.thaumcraftclassic.research.book;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraphLoader;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageLayout;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-04-s-iv-b unit guards: the Thaumonomicon recipe-page display
 * (closes BD-19).
 *
 * <p>The in-game probes ({@code gametest.ThaumonomiconRecipePageGameTest}) show
 * that the KNOWFRAG page carries its row and that the row resolves out of the
 * live {@code RecipeManager}. These guards cover what is decidable from the
 * repository alone, and they are what keeps the compiled-in join
 * ({@link ResearchRecipeLinks}) from drifting away from its three sources:
 * the content-mapping ledger's {@code research=} column, the shipped row JSON's
 * {@code _source} citation of the original's recipes-map key, and the shipped
 * graph's {@code normal_crafting} pages. Plus the original's page geometry
 * (measured from {@code GuiResearchRecipe#drawCraftingPage}, lines 755-946) and
 * the two station label keys it printed.</p>
 */
class ThaumonomiconRecipePageTest {

    /** The row the ledger links to KNOWFRAG (content-mapping.csv). */
    private static final String ROW = "thaumcraft:recipe.256.crafting_shaped";
    /** The original's {@code ConfigResearch.recipes} key for that row. */
    private static final String MAP_KEY = "KnowFrag";
    private static final Pattern RESEARCH_LINK = Pattern.compile("\\bresearch=([A-Za-z0-9_@-]+)");
    private static final Pattern VANILLA_CRAFTING_TYPE =
            Pattern.compile("\\btype=crafting_(shaped|shapeless)\\b");

    // ------------------------------------------------------------- the join vs the ledger

    @Test
    void theLedgerShipsExactlyThisVanillaCraftingJoin() throws IOException {
        final Map<String, String> fromLedger = ledgerVanillaCraftingLinks();
        final Map<String, String> fromCode = new TreeMap<>();
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            fromCode.put(link.researchKey(), link.recipeId());
        }
        assertEquals(fromCode, fromLedger,
                "the compiled-in vanilla-crafting join must mirror the ledger rows that carry "
                        + "type=crafting_shaped|crafting_shapeless plus research=<KEY> (the "
                        + "content-mapping.csv notes column): ledger says " + fromLedger);
        assertEquals(ROW, fromLedger.get("KNOWFRAG"),
                "the KNOWFRAG row the ledger links is the shipped recipe.256.crafting_shaped");
    }

    @Test
    void theRowJsonCitesTheRecipesMapKeyThePageNames() {
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            final JsonObject json = shippedRow(link.recipeId());
            final String source = json.get("_source").getAsString();
            assertTrue(source.contains("recipes.put(\"" + link.originalKey() + "\""),
                    link.recipeId() + " must cite the ConfigResearch.recipes.put(\""
                            + link.originalKey() + "\", ...) registration it came from, got: " + source);
            final String type = json.get("type").getAsString();
            assertTrue("minecraft:crafting_shaped".equals(type)
                            || "minecraft:crafting_shapeless".equals(type),
                    link.recipeId() + " is " + type + ": the join covers the vanilla crafting "
                            + "family only, because the port's own recipe types carry a research "
                            + "field in their own JSON");
        }
    }

    @Test
    void theGraphPagesNameExactlyTheKeysThisJoinCovers() {
        final ResearchGraph graph = graph();
        int craftingPages = 0;
        final List<String> linkedPages = new ArrayList<>();
        for (final ResearchEntryData entry : graph.entries()) {
            for (final ResearchPageData page : pagesOf(entry)) {
                if (page.type() != ResearchPageData.Type.NORMAL_CRAFTING) {
                    continue;
                }
                craftingPages++;
                final ResearchRecipeLinks link = ResearchRecipeLinks.byOriginalKey(page.recipe());
                if (link == null) {
                    continue;
                }
                linkedPages.add(entry.key() + "/" + page.recipe());
                assertEquals(link.researchKey(), entry.key(),
                        "page " + entry.key() + " names recipes-map key " + page.recipe()
                                + ", which the ledger links to entry " + link.researchKey()
                                + " — a page may not draw the row of another entry");
            }
        }
        assertEquals(45, craftingPages,
                "the shipped graph has 45 normal_crafting pages; the join covers only the rows "
                        + "the ledger has implemented, so the count is the sweep's baseline");
        assertEquals(ResearchRecipeLinks.table().size(), linkedPages.size(),
                "every key the join covers must be named by a graph page (a key nobody renders "
                        + "is a dead link), got " + linkedPages);
        assertTrue(linkedPages.contains("KNOWFRAG/" + MAP_KEY),
                "the KNOWFRAG entry must carry the page that names KnowFrag, got " + linkedPages);
    }

    @Test
    void linkedEntriesAreAutoUnlockSoTheDisplayHasNoGate() {
        final ResearchGraph graph = graph();
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            final ResearchEntryData entry = graph.entry(link.researchKey());
            assertNotNull(entry, "the join names entry " + link.researchKey());
            assertNotNull(entry.flags(), "entry " + link.researchKey() + " carries no flags");
            assertTrue(entry.flags().autoUnlock(),
                    "BD-19 rejects a research gate on " + link.recipeId() + " because the "
                            + "original's " + link.researchKey() + " entry is setAutoUnlock: the "
                            + "page body is display only, so a gate would be a failure mode with "
                            + "no behaviour");
        }
    }

    @Test
    void theJoinCoversOnlyLinkedRowsAndNothingForEveryOtherEntry() {
        assertEquals(1, ResearchRecipeLinks.table().size(),
                "the original links exactly one vanilla crafting row to a research page "
                        + "(ConfigRecipes.java:3860-3865 + ConfigResearch.java:2661-2667)");
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            assertTrue(link.recipeId().startsWith("thaumcraft:"), link.recipeId());
            assertTrue(link.recipeId().endsWith(".crafting_shaped")
                            || link.recipeId().endsWith(".crafting_shapeless"),
                    link.recipeId() + " is not a vanilla crafting row id");
            assertSame(link, ResearchRecipeLinks.byOriginalKey(link.originalKey()));
            assertEquals(List.of(link.recipeId()), ResearchRecipeLinks.rowsOfResearch(link.researchKey()));
        }
        assertNull(ResearchRecipeLinks.byOriginalKey(null), "a page with no recipe names no row");
        assertNull(ResearchRecipeLinks.byOriginalKey("WandBasic"),
                "the BASICTHAUMATURGY crafting page's row is a ledger row the port has not "
                        + "implemented, so its page draws no body");
        assertEquals(List.of(), ResearchRecipeLinks.rowsOfResearch("BASICTHAUMATURGY"));
        assertEquals(List.of(), ResearchRecipeLinks.rowsOfResearch("THAUMONOMICON"));
    }

    // ------------------------------------------------------------------ page state shape

    @Test
    void aPageThatNamesNoRowCarriesNoBody() {
        final BookPageView text = new BookPageView("BASICTHAUMATURGY", "tc.research_name.X",
                0, 4, ResearchPageData.Type.TEXT, "tc.research_page.X.1", null, true);
        assertNull(text.recipeId(), "the eight-argument page constructor is the no-body shape");
        final BookPageView crafting = new BookPageView("KNOWFRAG", "tc.research_name.KNOWFRAG",
                1, 2, ResearchPageData.Type.NORMAL_CRAFTING, null, null, true, ROW);
        assertEquals(ROW, crafting.recipeId(), "the page state carries the row id, not a recipe object");
    }

    // ---------------------------------------------------------------- the row's grid shape

    @Test
    void aShapelessRowWalksTheOriginalsBox() {
        // GuiResearchRecipe.java:898-923: i over the ingredients capped at 9, cell (i % 3, i / 3).
        assertEquals(new RecipePageLayout.Shape(0, 0), RecipePageDisplay.shapelessShape(0));
        assertEquals(new RecipePageLayout.Shape(1, 1), RecipePageDisplay.shapelessShape(1));
        assertEquals(new RecipePageLayout.Shape(3, 1), RecipePageDisplay.shapelessShape(3));
        assertEquals(new RecipePageLayout.Shape(3, 2), RecipePageDisplay.shapelessShape(4));
        assertEquals(new RecipePageLayout.Shape(3, 2), RecipePageDisplay.shapelessShape(6));
        assertEquals(new RecipePageLayout.Shape(3, 3), RecipePageDisplay.shapelessShape(7));
        assertEquals(new RecipePageLayout.Shape(3, 3), RecipePageDisplay.shapelessShape(9));
        assertEquals(new RecipePageLayout.Shape(3, 3), RecipePageDisplay.shapelessShape(12),
                "the original draws at most 9 cells (i < 9), so a 12-ingredient row is capped");
        assertEquals(9, RecipePageDisplay.shapelessShape(9).cells());
    }

    // ---------------------------------------------------------------- the original's geometry

    @Test
    void theLayoutIsTheOriginalsCraftingPageGeometry() {
        // 52x52 backdrop blitted at scale 2 (GuiResearchRecipe.java:783-788).
        assertEquals(104, RecipePageLayout.BLOCK_WIDTH);
        assertEquals(2 * 52, RecipePageLayout.BLOCK_WIDTH);
        // grid (837-864 / 898-923), result (798-799, plate 789), label (821-823 / 887-890).
        assertEquals(16, RecipePageLayout.GRID_X);
        assertEquals(76, RecipePageLayout.GRID_Y);
        assertEquals(32, RecipePageLayout.PITCH);
        assertEquals(16, RecipePageLayout.ITEM);
        assertEquals(48, RecipePageLayout.RESULT_X);
        assertEquals(32, RecipePageLayout.RESULT_Y);
        assertEquals(56, RecipePageLayout.HEADER_CENTRE_X);
        assertEquals(18, RecipePageLayout.SLOT);
        assertEquals(3, RecipePageLayout.MAX_GRID);
        assertEquals(RecipePageLayout.GRID_Y + 2 * RecipePageLayout.PITCH + RecipePageLayout.ITEM,
                RecipePageLayout.BLOCK_HEIGHT,
                "the block reaches from the page top to the third grid row's item bottom");

        final RecipePageLayout full = new RecipePageLayout(10, 20, 1.0F);
        assertEquals(26, full.cellX(0), "x + GRID_X + 0 * PITCH");
        assertEquals(90, full.cellX(2), "x + 16 + 2 * 32");
        assertEquals(160, full.cellY(2), "y + 76 + 2 * 32");
        assertEquals(25, full.slotX(0), "the 18px vanilla slot square is centred on the 16px item");
        assertEquals(58, full.resultX());
        assertEquals(52, full.resultY());
        assertEquals(57, full.resultSlotX());
        assertEquals(56, full.headerX(20), "x + 56 - width / 2");
        assertEquals(18, full.slotSize());

        // The book's pane is smaller than the original's, so the block is scaled to fit.
        final RecipePageLayout fitted = RecipePageLayout.fit(0, 0, 110, 142);
        assertEquals(142 / 156.0F, fitted.scale(), 1e-6F, "the height is the binding axis");
        assertTrue(fitted.size(RecipePageLayout.BLOCK_WIDTH) <= 110);
        assertTrue(fitted.y(RecipePageLayout.BLOCK_HEIGHT) <= 142);
        assertEquals(1.0F, RecipePageLayout.fit(0, 0, 1000, 1000).scale(), 1e-6F,
                "the block is never drawn larger than the original drew it");
        assertEquals(RecipePageLayout.MIN_SCALE, RecipePageLayout.fit(0, 0, 0, 0).scale(), 1e-6F);
        assertEquals(RecipePageLayout.MIN_SCALE, RecipePageLayout.fit(0, 0, -8, 400).scale(), 1e-6F);
        assertEquals(10, RecipePageLayout.fit(10, 20, 400, 400).originX());
        assertEquals(20, RecipePageLayout.fit(10, 20, 400, 400).originY());
    }

    @Test
    void theStationLabelsAreTheOriginalsKeysAndShipTheOriginalStrings() throws IOException {
        assertEquals("recipe.type.workbench", RecipePageDisplay.Station.WORKBENCH.labelKey());
        assertEquals("recipe.type.workbenchshapeless",
                RecipePageDisplay.Station.WORKBENCH_SHAPELESS.labelKey());
        final JsonObject lang = lang();
        assertEquals("Workbench", lang.get(RecipePageDisplay.Station.WORKBENCH.labelKey()).getAsString());
        assertEquals("Workbench (Shapeless)",
                lang.get(RecipePageDisplay.Station.WORKBENCH_SHAPELESS.labelKey()).getAsString());

        final Path original = Path.of(System.getProperty("tc.originalLangFile", ""));
        Assumptions.assumeTrue(Files.isRegularFile(original),
                "the reference tree (reference/assets/thaumcraft/lang/en_US.lang) is not present");
        final Map<String, String> legacy = new LinkedHashMap<>();
        for (final String line : Files.readAllLines(original, StandardCharsets.UTF_8)) {
            final String trimmed = line.replace("\r", "");
            final int equals = trimmed.indexOf('=');
            if (equals > 0 && !trimmed.startsWith("#")) {
                legacy.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
        }
        for (final RecipePageDisplay.Station station : RecipePageDisplay.Station.values()) {
            assertEquals(legacy.get(station.labelKey()),
                    lang.get(station.labelKey()).getAsString(),
                    station + "'s label must be the original en_US.lang string verbatim");
        }
    }

    // ------------------------------------------------------------------------- sources

    /** The ledger's vanilla-crafting rows that carry a {@code research=<KEY>} link. */
    private static Map<String, String> ledgerVanillaCraftingLinks() throws IOException {
        final Path csv = Path.of(System.getProperty("tc.contentMappingFile", ""));
        assertTrue(Files.isRegularFile(csv),
                "content-mapping.csv not wired — run tests through ./gradlew test");
        final Map<String, String> links = new TreeMap<>();
        for (final ContentMappingCsv.Row row : ContentMappingCsv.read(csv)) {
            if (!"recipe".equals(row.kind())) {
                continue;
            }
            final Matcher type = VANILLA_CRAFTING_TYPE.matcher(row.notes());
            final Matcher research = RESEARCH_LINK.matcher(row.notes());
            if (!type.find() || !research.find() || "-".equals(research.group(1))) {
                continue;
            }
            final String previous = links.put(research.group(1), row.newId());
            assertNull(previous, "two vanilla crafting rows link to research " + research.group(1));
        }
        return links;
    }

    private static ResearchGraph graph() {
        final String path = System.getProperty("tc.researchDataDir");
        assertNotNull(path, "system property 'tc.researchDataDir' is not set — run tests through ./gradlew test");
        final Path dir = Path.of(path);
        assertTrue(Files.isDirectory(dir), "research data dir not found at " + dir);
        try (Reader categories = Files.newBufferedReader(dir.resolve("categories.json"));
             Reader entries = Files.newBufferedReader(dir.resolve("entries.json"))) {
            return ResearchGraphLoader.load(categories, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<ResearchPageData> pagesOf(final ResearchEntryData entry) {
        return entry.pages() == null ? List.of() : entry.pages();
    }

    /** The shipped row JSON behind a linked id, from the mod's own resources. */
    private static JsonObject shippedRow(final String recipeId) {
        final int colon = recipeId.indexOf(':');
        final String resource = "/data/thaumcraft/recipe/" + recipeId.substring(colon + 1) + ".json";
        try (InputStream in = ThaumonomiconRecipePageTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource + " is not on the classpath");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject lang() {
        try (InputStream in = ThaumonomiconRecipePageTest.class
                .getResourceAsStream("/assets/thaumcraft/lang/en_us.json")) {
            assertNotNull(in, "assets/thaumcraft/lang/en_us.json is not on the classpath");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
