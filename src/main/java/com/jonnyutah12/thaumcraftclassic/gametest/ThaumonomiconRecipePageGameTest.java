package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.ResearchDetailModel;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import com.jonnyutah12.thaumcraftclassic.research.book.BookDetailPanelLayout;
import com.jonnyutah12.thaumcraftclassic.research.book.BookFrame;
import com.jonnyutah12.thaumcraftclassic.research.book.BookPageView;
import com.jonnyutah12.thaumcraftclassic.research.book.ResearchRecipeLinks;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconBook;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconHost;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageDisplay;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageLayout;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TC-M3-04-s-iv-b: the Thaumonomicon recipe-page display (closes BD-19).
 *
 * <p>The original's KnowFrag row lives in two places at once — a plain
 * crafting-table recipe <b>and</b> the value of
 * {@code ConfigResearch.recipes.put("KnowFrag", ...)}
 * ({@code ConfigRecipes.java:3860-3865}) — and its KNOWFRAG entry puts that map
 * value on a page ({@code ConfigResearch.java:2661-2667}), which is why the
 * entry's Thaumonomicon page draws the 3x3 as its body
 * ({@code GuiResearchRecipe#drawCraftingPage}, lines 755-946). The port ships
 * the row (TC-M3-04-s-iv, {@code recipe.256.crafting_shaped}) and KNOWFRAG is
 * {@code setAutoUnlock}, so acquisition is exact; these probes cover the page
 * half: the join that carries the row onto the page, and the picture read out
 * of the live {@link RecipeManager}.</p>
 *
 * <ul>
 *   <li>{@link #knowFragPageCarriesItsCraftingRow}: through the real
 *       {@link ThaumonomiconHost} open seam, the KNOWFRAG entry's second page is
 *       the {@code normal_crafting} page and carries the shipped row's id; the
 *       entry is auto-unlock, so the display has no gate (BD-19 rejects adding
 *       one).</li>
 *   <li>{@link #thePageRowResolvesFromTheLiveRecipeManager}: the id the page
 *       carries resolves in the manager the server is crafting with, into the
 *       original's picture — a 3x3 of meta-9 knowledge fragments beside a
 *       meta-42 blank discovery note, labelled {@code recipe.type.workbench};
 *       an id the page does not name resolves to nothing, so the screen can
 *       only ever draw this row.</li>
 *   <li>{@link #pagesWithoutALinkedRowDrawNoBody}: the whole shipped graph
 *       swept page by page — exactly the one page the ledger links carries a
 *       row, every other page (including the 44 {@code normal_crafting} pages
 *       whose recipes-map rows the ledger has not implemented) carries none, and
 *       no link in the join is dead.</li>
 *   <li>{@link #theEntryDetailCarriesTheRowItsPanelDraws} (TC-M3-04-s-iv-b-2,
 *       closes BD-64): the unified book screen's detail panel asks the
 *       <b>entry</b>, and the entry answers with the row of its own linked page —
 *       the same id the book's pager puts on that page, live in the manager the
 *       server crafts with, and exactly one entry in the graph has one.</li>
 *   <li>{@link #theBookPanelReservesARegionForTheRecipeBody}: the panel's box
 *       splits into kept text rows and a region at its bottom, and the original's
 *       page block — 3x3 grid, result, station label — fits inside the shipped
 *       panel's column, scaled down and never up.</li>
 *   <li>{@link #aPanelWithNothingToDrawShowsItsTextBody}: all three ways there is
 *       nothing to draw (no linked row, a row the live {@link RecipeManager} does
 *       not carry, no room for the block) leave the panel's text column full.</li>
 * </ul>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThaumonomiconRecipePageGameTest {

    /** The KNOWFRAG entry, and the row the ledger links to it. */
    private static final String ENTRY = "KNOWFRAG";
    private static final String ROW = "thaumcraft:recipe.256.crafting_shaped";

    private ThaumonomiconRecipePageGameTest() {
    }

    // ------------------------------------------------------- the page carries the row

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void knowFragPageCarriesItsCraftingRow(final GameTestHelper helper) {
        final RecordingHost host = new RecordingHost();
        ThaumonomiconHost.install(host);
        try {
            final ThaumonomiconBook book = ThaumonomiconBook.open();
            helper.assertTrue(ThaumonomiconHost.openBook(book),
                    "opening the book through the host must report success");
            helper.assertTrue(host.opens == 1 && host.last == book,
                    "the host must be handed the book that was opened");

            final ResearchGraph graph = ResearchManager.graph();
            final ResearchEntryData entry = graph.entry(ENTRY);
            helper.assertTrue(entry != null, "the shipped graph must carry the KNOWFRAG entry");
            helper.assertTrue(entry.flags() != null && entry.flags().autoUnlock(),
                    "the original's KNOWFRAG entry is setAutoUnlock: the page body may not be "
                            + "gated behind research, which is why BD-19 rejects a research gate "
                            + "on the row");
            helper.assertTrue(containsCraftingPage(entry),
                    "the original gives KNOWFRAG a normal_crafting page "
                            + "(ConfigResearch.java:2661-2667, recipes.get(\"KnowFrag\"))");

            helper.assertTrue(book.selectEntry(ENTRY), "the entry must open");
            final BookFrame first = book.frame();
            helper.assertTrue(!first.onGraph() && first.page() != null, "an open entry shows a page");
            final BookPageView text = first.page();
            helper.assertTrue(ENTRY.equals(text.entryKey()), "the page belongs to the open entry");
            helper.assertTrue(text.kind() == ResearchPageData.Type.TEXT,
                    "KNOWFRAG's first page is its text page, got " + text.kind());
            helper.assertTrue(text.recipeId() == null,
                    "a text page draws no recipe body, got " + text.recipeId());

            helper.assertTrue(book.nextPage(), "the entry has a second page");
            final BookPageView page = book.frame().page();
            helper.assertTrue(page.kind() == ResearchPageData.Type.NORMAL_CRAFTING,
                    "the second page is the crafting page the original built from "
                            + "recipes.get(\"KnowFrag\"), got " + page.kind());
            helper.assertTrue(ROW.equals(page.recipeId()),
                    "the page must carry the shipped KnowFrag row so the screen can draw it, got "
                            + page.recipeId());

            // The id the page carries is a row the server is actually crafting with.
            final RecipeManager recipes = helper.getLevel().getRecipeManager();
            helper.assertTrue(recipes.byKey(ResourceLocation.parse(page.recipeId())).isPresent(),
                    "the page names " + page.recipeId() + ", which the live RecipeManager does not "
                            + "have — a page may not display a row the pack cannot craft");

            ThaumcraftClassic.LOGGER.info(
                    "[TC-M3-04-s-iv-b] KNOWFRAG page {}/{} is a normal_crafting page carrying {} "
                            + "(entry autoUnlock, row live in the RecipeManager: {} of {} crafting rows)",
                    page.index() + 1, page.count(), page.recipeId(), true,
                    recipes.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).size());
            helper.succeed();
        } finally {
            ThaumonomiconHost.uninstall();
        }
    }

    // ------------------------------------------- the picture comes from the live manager

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void thePageRowResolvesFromTheLiveRecipeManager(final GameTestHelper helper) {
        final RecipeManager recipes = helper.getLevel().getRecipeManager();
        final var registries = helper.getLevel().registryAccess();
        final ResourceLocation id = ResourceLocation.parse(ROW);

        final Optional<RecipePageDisplay> resolved = RecipePageDisplay.resolve(id, recipes, registries);
        helper.assertTrue(resolved.isPresent(),
                "the page's row " + ROW + " must resolve out of the live RecipeManager — the "
                        + "display reads the manager the game crafts with, not the shipped JSON");
        final RecipePageDisplay row = resolved.get();

        helper.assertTrue(id.equals(row.id()), "the resolved row must be the page's row");
        helper.assertTrue(row.station() == RecipePageDisplay.Station.WORKBENCH
                        && "recipe.type.workbench".equals(row.station().labelKey()),
                "a shaped row is labelled with the original's recipe.type.workbench "
                        + "(GuiResearchRecipe.java:821-823), got " + row.station().labelKey());
        helper.assertTrue(row.width() == 3 && row.height() == 3,
                "the KnowFrag pattern KKK/KKK/KKK draws a 3x3 grid, got "
                        + row.width() + "x" + row.height());
        helper.assertTrue(row.grid().size() == 9, "a 3x3 box is nine cells, got " + row.grid().size());

        for (int cell = 0; cell < row.grid().size(); cell++) {
            final ItemStack stack = row.grid().get(cell);
            helper.assertTrue(!stack.isEmpty(),
                    "cell " + cell + " of the pattern is a K, so it must show an ingredient");
            helper.assertTrue(stack.is(M3ResearchContent.ITEM_RESEARCH_NOTES.get())
                            || stack.is(com.jonnyutah12.thaumcraftclassic.registration.catalog
                                    .CatalogResources.ITEM_RESOURCE.get()),
                    "cell " + cell + " shows " + stack.getItem() + ", the pattern's key is K");
            helper.assertTrue(stack.is(com.jonnyutah12.thaumcraftclassic.registration.catalog
                            .CatalogResources.ITEM_RESOURCE.get())
                            && ItemResource.storedMeta(stack) == ItemResource.META_KNOWLEDGE_FRAGMENT,
                    "cell " + cell + " shows " + stack + " (meta " + ItemResource.storedMeta(stack)
                            + "): the row's K is the neoforge:components ingredient pinned to "
                            + "thaumcraft:item_meta " + ItemResource.META_KNOWLEDGE_FRAGMENT
                            + ", and the page must show knowledge fragments, not another "
                            + "itemresource meta");
        }

        helper.assertTrue(row.result().is(M3ResearchContent.ITEM_RESEARCH_NOTES.get())
                        && row.result().getCount() == 1
                        && ItemResearchNotes.storedMeta(row.result())
                                == ItemResearchNotes.META_BLANK_DISCOVERY_B,
                "the item beside the grid is the row's output — the original's is meta "
                        + ItemResearchNotes.META_BLANK_DISCOVERY_B + " blank discovery note x1, got "
                        + row.result() + " (meta " + ItemResearchNotes.storedMeta(row.result()) + ")");

        // The screen draws what the page names: an id the page does not carry is
        // nothing to draw, so the picture cannot be invented from the JSON.
        helper.assertTrue(RecipePageDisplay.resolve(
                        ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                                "recipe.999.not_a_shipped_row"),
                        recipes, registries).isEmpty(),
                "an id the live manager does not have must resolve to no body");
        // What the screen may draw comes from the page, not from the manager: the
        // join carries exactly one vanilla crafting row, so no other live row —
        // not even a plain vanilla one — can ever be handed to the screen.
        final ResourceLocation vanilla = ResourceLocation.withDefaultNamespace("crafting_table");
        helper.assertTrue(recipes.byKey(vanilla).isPresent(),
                "minecraft:crafting_table is live in the same manager — the display's filter is "
                        + "the id the page carries, not the manager's contents");
        helper.assertTrue(ResearchRecipeLinks.table().size() == 1
                        && ROW.equals(ResearchRecipeLinks.byOriginalKey("KnowFrag").recipeId())
                        && ResearchRecipeLinks.table().values().stream()
                                .noneMatch(link -> vanilla.toString().equals(link.recipeId())),
                "the only vanilla crafting row any page can name is the row the ledger links "
                        + "(content-mapping research=KNOWFRAG), got " + ResearchRecipeLinks.table());

        int linked = 0;
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            if (RecipePageDisplay.resolve(ResourceLocation.parse(link.recipeId()), recipes, registries)
                    .isPresent()) {
                linked++;
            }
        }
        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv-b] recipe page body resolved from the live RecipeManager: {} is a "
                        + "{}x{} {} grid of itemresource meta {} -> {} x{} (meta {}), label {}; "
                        + "{} of {} linked rows are live (a gated-out row draws no body)",
                ROW, row.width(), row.height(), row.station(), ItemResource.META_KNOWLEDGE_FRAGMENT,
                row.result().getItem(), row.result().getCount(),
                ItemResearchNotes.storedMeta(row.result()), row.station().labelKey(),
                linked, ResearchRecipeLinks.table().size());
        helper.succeed();
    }

    // -------------------------------------------------- the join covers exactly the linked pages

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void pagesWithoutALinkedRowDrawNoBody(final GameTestHelper helper) {
        final ResearchGraph graph = ResearchManager.graph();
        final ThaumonomiconBook book = ThaumonomiconBook.open();
        final RecipeManager recipes = helper.getLevel().getRecipeManager();

        int pages = 0;
        int craftingPages = 0;
        int pagesWithBody = 0;
        final List<String> problems = new ArrayList<>();
        final List<String> linkedPages = new ArrayList<>();
        for (final ResearchEntryData entry : graph.entries()) {
            final List<ResearchPageData> entryPages =
                    entry.pages() == null ? List.of() : entry.pages();
            for (int index = 0; index < entryPages.size(); index++) {
                if (!book.selectEntry(entry.key())) {
                    problems.add("the book cannot open the shipped entry " + entry.key());
                    continue;
                }
                for (int turn = 0; turn < index; turn++) {
                    book.nextPage();
                }
                final BookPageView page = book.frame().page();
                final ResearchPageData data = entryPages.get(index);
                pages++;
                if (data.type() == ResearchPageData.Type.NORMAL_CRAFTING) {
                    craftingPages++;
                }
                final ResearchRecipeLinks link = ResearchRecipeLinks.byOriginalKey(data.recipe());
                final boolean shouldCarry = link != null
                        && data.type() == ResearchPageData.Type.NORMAL_CRAFTING
                        && link.researchKey().equals(entry.key());
                if (page.recipeId() != null) {
                    pagesWithBody++;
                    linkedPages.add(entry.key() + "#" + (index + 1) + "=" + page.recipeId());
                }
                if (shouldCarry && !page.recipeId().equals(link.recipeId())) {
                    problems.add("page " + entry.key() + "#" + (index + 1) + " names recipes-map key "
                            + data.recipe() + ", which the ledger links to " + link.recipeId()
                            + ", but the page carries " + page.recipeId());
                }
                if (!shouldCarry && page.recipeId() != null) {
                    problems.add("page " + entry.key() + "#" + (index + 1) + " (type " + data.type()
                            + ", recipe " + data.recipe() + ") carries a body the ledger does not link");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "the page bodies must follow the graph + ledger join: "
                + problems);
        helper.assertTrue(pages > 100, "the sweep must walk the shipped page list, got " + pages);
        helper.assertTrue(craftingPages == 45,
                "the shipped graph has 45 normal_crafting pages, got " + craftingPages);
        helper.assertTrue(pagesWithBody == ResearchRecipeLinks.table().size(),
                "exactly the linked pages carry a body (" + linkedPages + "), the join has "
                        + ResearchRecipeLinks.table().size() + " links");
        helper.assertTrue(linkedPages.contains(ENTRY + "#2=" + ROW),
                "the KNOWFRAG crafting page must carry the shipped row, got " + linkedPages);

        // No dead links: every row the join names is live, so a linked page is
        // never a page that draws nothing because the id went stale.
        final List<String> dead = new ArrayList<>();
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            final Optional<RecipePageDisplay> row =
                    RecipePageDisplay.resolve(ResourceLocation.parse(link.recipeId()), recipes,
                            helper.getLevel().registryAccess());
            if (row.isEmpty()) {
                dead.add(link.originalKey() + " -> " + link.recipeId());
            } else if (row.get().width() > 3 || row.get().height() > 3) {
                dead.add(link.recipeId() + " draws a " + row.get().width() + "x" + row.get().height()
                        + " grid, wider than the original's capped 3x3 page grid");
            }
        }
        helper.assertTrue(dead.isEmpty(), "every linked row must be live and page-sized: " + dead);

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv-b] swept {} pages of {} entries: {} are normal_crafting pages, "
                        + "{} carry a recipe body ({}), {} of them resolve in the live "
                        + "RecipeManager, the rest draw the page-kind placeholder",
                pages, graph.entryCount(), craftingPages, pagesWithBody, linkedPages,
                ResearchRecipeLinks.table().size());
        helper.succeed();
    }

    // ------------------------- the unified book screen's panel body (TC-M3-04-s-iv-b-2, BD-64)

    /**
     * <b>The panel asks the entry, and the entry answers with its own row.</b> The
     * unified book screen ({@code client.ThaumonomiconScreen}) lists entries and
     * shows one detail panel, so the page body is resolved from the <b>entry</b>
     * detail, not from a page index. This probe measures that on the live graph:
     * KNOWFRAG's detail carries the shipped row, the id the panel carries is the id
     * the book's own pager puts on the page, every entry that carries a row is the
     * entry the ledger's {@code research=} column names, and every id carried is
     * live in the {@link RecipeManager} the server is crafting with — so a panel
     * never names a row the pack cannot craft.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theEntryDetailCarriesTheRowItsPanelDraws(final GameTestHelper helper) {
        final ResearchGraph graph = ResearchManager.graph();
        final ResearchDetailModel details = new ResearchDetailModel(graph);
        final RecipeManager recipes = helper.getLevel().getRecipeManager();

        final ResearchDetailModel.Detail detail = details.detail(ENTRY, null);
        helper.assertTrue(detail != null, ENTRY + " is a shipped entry");
        helper.assertTrue(ROW.equals(detail.recipeId()),
                "the panel of " + ENTRY + " must carry the joined row " + ROW + ", got "
                        + detail.recipeId());
        helper.assertTrue(RecipePageDisplay.resolve(ResourceLocation.parse(detail.recipeId()), recipes,
                        helper.getLevel().registryAccess()).isPresent(),
                "the row the panel carries must resolve out of the live RecipeManager");

        // The page pager and the panel ask one rule: same entry, same row.
        final ThaumonomiconBook book = ThaumonomiconBook.open();
        helper.assertTrue(book.selectEntry(ENTRY), "the book must open " + ENTRY);
        helper.assertTrue(book.nextPage(), ENTRY + " has the crafting page the original built");
        final BookPageView page = book.frame().page();
        helper.assertTrue(detail.recipeId().equals(page.recipeId()),
                "the panel carries " + detail.recipeId() + " and the page carries " + page.recipeId()
                        + " — page and panel may not name different rows");

        int withBody = 0;
        final List<String> dead = new ArrayList<>();
        final List<String> carried = new ArrayList<>();
        for (final ResearchEntryData entry : graph.entries()) {
            final ResearchDetailModel.Detail each = details.detail(entry.key(), null);
            helper.assertTrue(each != null, "every shipped entry must build a panel detail");
            if (each.recipeId() == null) {
                continue;
            }
            withBody++;
            carried.add(entry.key() + "=" + each.recipeId());
            helper.assertTrue(entry.key().equals(ENTRY),
                    "only the entry the ledger links has a page body, and " + entry.key() + " has one");
            helper.assertTrue(ResearchRecipeLinks.linkedRowOf(entry).equals(each.recipeId()),
                    "the panel's row must be the join's row for the entry");
            if (RecipePageDisplay.resolve(ResourceLocation.parse(each.recipeId()), recipes,
                    helper.getLevel().registryAccess()).isEmpty()) {
                dead.add(entry.key() + " -> " + each.recipeId());
            }
        }
        helper.assertTrue(1 == withBody, "one ledger row carries a research= key, so exactly one entry's "
                + "panel has a recipe body, got " + carried);
        helper.assertTrue(dead.isEmpty(), "no panel may name a row the live RecipeManager does not carry: "
                + dead);

        // The negative control, on the live graph: an entry with its own
        // normal_crafting page whose ledger row is not implemented draws text.
        final ResearchDetailModel.Detail plain = details.detail("BASICTHAUMATURGY", null);
        helper.assertTrue(plain != null && plain.recipeId() == null,
                "BASICTHAUMATURGY's page names a recipes-map key the ledger has not implemented, so its "
                        + "panel has no body, got " + (plain == null ? null : plain.recipeId()));

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv-b-2] swept {} entries of the live graph: {} panel carries {}, {} entries "
                        + "carry a row ({}) and every one resolves in the live RecipeManager; the rest "
                        + "of the panels draw their text body (BD-64 closed)",
                graph.entryCount(), ENTRY, detail.recipeId(), withBody, carried);
        helper.succeed();
    }

    /**
     * <b>The region the picture draws in.</b> The panel's box and the block's fit
     * are computed by {@code BookDetailPanelLayout} — a record that imports no
     * Minecraft type, so this probe measures the geometry the window will use
     * (D-019 rule 2). The numbers are the book panel's column:
     * {@code ThaumonomiconScreen.DETAIL_WIDTH} wide (100), from
     * {@code ROWS_TOP - 2} (38) to {@code PANE_HEIGHT - 4} (226), at
     * {@code DETAIL_PITCH} (9); the client-side split is asserted in
     * {@code client.ThaumonomiconDetailPanelContractTest} against the constants
     * themselves.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theBookPanelReservesARegionForTheRecipeBody(final GameTestHelper helper) {
        final BookDetailPanelLayout panel = new BookDetailPanelLayout(148, 38, 100, 226, 9);
        final int right = panel.left() + panel.width();

        helper.assertTrue(panel.bodyTop() > panel.top() + 4 * 9 - 1,
                "the body region starts below the kept text rows, got " + panel.bodyTop());
        helper.assertTrue(panel.bodyHeight() > 0, "the shipped panel has a body region, got "
                + panel.bodyHeight());
        helper.assertTrue(panel.bodyFits(),
                "the original's " + RecipePageLayout.BLOCK_WIDTH + "x" + RecipePageLayout.BLOCK_HEIGHT
                        + " block must fit the book's " + panel.width() + "x" + panel.bodyHeight()
                        + " region");
        final RecipePageLayout layout = panel.bodyLayout();
        helper.assertTrue(layout.scale() > RecipePageLayout.MIN_SCALE && layout.scale() <= 1.0F,
                "the block fits scaled down, never enlarged, got " + layout.scale());
        helper.assertTrue(layout.x(0) >= panel.left()
                        && layout.x(0) + layout.size(RecipePageLayout.BLOCK_WIDTH) <= right + 1,
                "the block's frame is inside the panel column");
        helper.assertTrue(layout.y(0) >= panel.bodyTop()
                        && layout.y(0) + layout.size(RecipePageLayout.BLOCK_HEIGHT) <= panel.bottom(),
                "the block's frame is inside the region");

        // The KNOWFRAG body the screen draws for this entry: a 3x3 grid, the result
        // beside it, the station label above — all inside the panel.
        for (int col = 0; col < RecipePageLayout.MAX_GRID; col++) {
            helper.assertTrue(layout.cellX(col) >= panel.left()
                            && layout.cellX(col) + RecipePageLayout.ITEM <= right,
                    "grid column " + col + " draws outside the panel");
        }
        for (int row = 0; row < RecipePageLayout.MAX_GRID; row++) {
            helper.assertTrue(layout.cellY(row) >= panel.bodyTop()
                            && layout.cellY(row) + RecipePageLayout.ITEM <= panel.bottom() + 1,
                    "grid row " + row + " draws outside the region");
        }
        helper.assertTrue(layout.resultX() + RecipePageLayout.ITEM <= right
                        && layout.resultY() >= panel.bodyTop()
                        && layout.resultY() + RecipePageLayout.ITEM <= panel.bottom(),
                "the result beside the grid draws outside the panel");
        helper.assertTrue(layout.headerX(50) >= panel.left() && layout.headerX(50) + 50 <= right,
                "the station label draws outside the panel");
        helper.assertTrue(panel.textBottomWithBody() == panel.bodyTop()
                        && panel.textBottom() == panel.bottom(),
                "the text column is the panel above the body, and the whole panel with no body");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv-b-2] book panel {}x{} splits at y={}: the {}x{} page block fits at "
                        + "scale {} (grid rows {}..{}, result at {},{}), the text keeps {} rows above it",
                panel.width(), panel.bodyHeight(), panel.bodyTop(),
                layout.size(RecipePageLayout.BLOCK_WIDTH), layout.size(RecipePageLayout.BLOCK_HEIGHT),
                layout.scale(), layout.cellY(0), layout.cellY(2), layout.resultX(), layout.resultY(),
                BookDetailPanelLayout.TEXT_ROWS_KEPT);
        helper.succeed();
    }

    /**
     * <b>Every way there is nothing to draw, the panel degrades to its text
     * body</b> — the card's required probe, measured on the live graph:
     *
     * <ol>
     *   <li>an entry with no row the ledger links (44 of the graph's 45 crafting
     *       pages, and every other entry) carries no id, so there is nothing to
     *       resolve;</li>
     *   <li>a row id the live {@link RecipeManager} does not carry — the row gated
     *       out of the pack, a datapack that removed it, a stale join — resolves to
     *       no picture, and the screen draws no grid;</li>
     *   <li>a panel with no room for the block at its floor scale reports no room,
     *       so the window never paints a clipped grid over entry text.</li>
     * </ol>
     *
     * <p>In all three the text column is the whole panel again
     * ({@code textBottom() == bottom()}): the page's words are never shortened by
     * a body that is not there.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aPanelWithNothingToDrawShowsItsTextBody(final GameTestHelper helper) {
        final RecipeManager recipes = helper.getLevel().getRecipeManager();
        final var registries = helper.getLevel().registryAccess();

        // 1. No linked row: the join names one, so every other entry has nothing.
        final ResearchGraph graph = ResearchManager.graph();
        final ResearchDetailModel details = new ResearchDetailModel(graph);
        int textOnly = 0;
        for (final ResearchEntryData entry : graph.entries()) {
            if (details.detail(entry.key(), null).recipeId() == null) {
                textOnly++;
            }
        }
        helper.assertTrue(textOnly == graph.entryCount() - 1,
                "every entry but the linked one is text-only, got " + textOnly + " of "
                        + graph.entryCount());

        // 2. A row the live manager does not carry resolves to no picture.
        helper.assertTrue(RecipePageDisplay.resolve(ResourceLocation.parse(ROW), recipes, registries)
                .isPresent(), "the shipped row is live, so the linked panel has a body to draw");
        helper.assertTrue(RecipePageDisplay.resolve(
                        ResourceLocation.parse(ROW + ".removed_by_a_pack"), recipes, registries).isEmpty(),
                "an id the live RecipeManager does not carry must resolve to no body");
        final List<String> gatedOut = new ArrayList<>();
        for (final ResearchRecipeLinks link : ResearchRecipeLinks.table().values()) {
            final ResourceLocation id = ResourceLocation.parse(link.recipeId() + ".absent");
            if (RecipePageDisplay.resolve(id, recipes, registries).isPresent()) {
                gatedOut.add(id.toString());
            }
        }
        helper.assertTrue(gatedOut.isEmpty(), "no absent id may resolve: " + gatedOut);

        // 3. No room for the block: the region gives, not the picture.
        final BookDetailPanelLayout cramped = new BookDetailPanelLayout(0, 0, 20, 40, 9);
        helper.assertTrue(!cramped.bodyFits(),
                "a " + cramped.width() + "x" + cramped.bodyHeight() + " region cannot hold the block at "
                        + "its floor scale, and the panel must fall back to its text body");
        helper.assertTrue(cramped.textBottom() == cramped.bottom(),
                "with no body the text column is the whole panel");
        final BookDetailPanelLayout shipped = new BookDetailPanelLayout(148, 38, 100, 226, 9);
        helper.assertTrue(shipped.bodyFits() && shipped.textBottom() == shipped.bottom()
                        && shipped.textBottomWithBody() == shipped.bodyTop(),
                "the shipped panel has room, so its text gives only when a body is drawn");

        ThaumcraftClassic.LOGGER.info(
                "[TC-M3-04-s-iv-b-2] degradation measured: {} of {} entries carry no row id, an id the "
                        + "live RecipeManager lacks resolves to nothing, and a {} px region with no room "
                        + "for the block leaves the text column full",
                textOnly, graph.entryCount(), cramped.bodyHeight());
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    private static boolean containsCraftingPage(final ResearchEntryData entry) {
        return entry.pages() != null && entry.pages().stream()
                .anyMatch(page -> page.type() == ResearchPageData.Type.NORMAL_CRAFTING);
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
