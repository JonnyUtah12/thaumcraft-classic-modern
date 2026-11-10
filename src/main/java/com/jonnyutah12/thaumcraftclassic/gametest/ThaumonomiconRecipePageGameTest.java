package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.items.ItemResearchNotes;
import com.jonnyutah12.thaumcraftclassic.items.ItemResource;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M3ResearchContent;
import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchManager;
import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;
import com.jonnyutah12.thaumcraftclassic.research.book.BookFrame;
import com.jonnyutah12.thaumcraftclassic.research.book.BookPageView;
import com.jonnyutah12.thaumcraftclassic.research.book.ResearchRecipeLinks;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconBook;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconHost;
import com.jonnyutah12.thaumcraftclassic.recipes.RecipePageDisplay;
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
