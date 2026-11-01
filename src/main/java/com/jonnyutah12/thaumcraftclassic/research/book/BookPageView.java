package com.jonnyutah12.thaumcraftclassic.research.book;

import com.jonnyutah12.thaumcraftclassic.research.ResearchPageData;

/**
 * The page the book is showing for the open entry (TC-M3-03-s) — one page of
 * the original entry's page list, at its original index.
 *
 * <p>The original {@code GuiResearchBrowser} showed one {@code ResearchPage}
 * at a time and paged through them with the arrow buttons; the page list comes
 * verbatim from {@code entries.json} (TC-M3-01). {@code kind} is the original
 * {@code PageType}, which decides what the screen draws: text pages render
 * {@code textKey}, crafting pages render their recipe (TC-M4-03/04) and aspect
 * pages their aspect list (TC-M4-01) — the presentation layer only carries the
 * type and the text key, so the crafting/aspect rendering stays on the cards
 * that own those systems.</p>
 *
 * <p>{@code revealed} is the original's {@code text_concealed} gate: such a
 * page stays hidden until its {@code requires} prerequisite is completed, which
 * is why the pair travels together.</p>
 *
 * <p>{@code recipeId} is the TC-M3-04-s-iv-b addition (closes BD-19). The
 * original's {@code ResearchPage} carried the recipe <i>object</i> for a
 * crafting page — {@code ConfigResearch.java:2661-2667} builds the KNOWFRAG
 * page from {@code recipes.get("KnowFrag")} — and its renderer drew that object
 * as the page body. The port carries the row's <b>id</b> instead, resolved from
 * the page's recipes-map key by
 * {@link ResearchRecipeLinks#byOriginalKey(String)} and drawn from the live
 * {@code RecipeManager} by
 * {@link com.jonnyutah12.thaumcraftclassic.recipes.RecipePageDisplay#resolve}, so the
 * book's page state stays off {@code net.minecraft.world.item.crafting} (a
 * dedicated server drives this record) and the picture can never drift from the
 * row that crafts. It is non-null only for a page the original drew as a
 * vanilla crafting grid; {@code null} means "this page has no researched
 * vanilla row", which is the ledger's no-display state for the other page kinds
 * and for {@code normal_crafting} pages whose row the ledger has not
 * implemented.</p>
 *
 * @param entryKey    the entry the page belongs to
 * @param titleKey    the entry's name lang key, shown as the page heading
 * @param index       0-based index into the entry's original page list
 * @param count       size of the entry's original page list (0 = no pages)
 * @param kind        original page type; null only when the entry has no pages
 * @param textKey     original page text lang key (text/text_concealed/image)
 * @param requiresKey research key gating a concealed page
 * @param revealed    whether the player may read this page
 * @param recipeId    the vanilla crafting row this page draws, or {@code null}
 */
public record BookPageView(
        String entryKey,
        String titleKey,
        int index,
        int count,
        ResearchPageData.Type kind,
        String textKey,
        String requiresKey,
        boolean revealed,
        String recipeId) {

    /** The constructor every page that draws no vanilla crafting row takes. */
    public BookPageView(final String entryKey, final String titleKey, final int index, final int count,
                        final ResearchPageData.Type kind, final String textKey, final String requiresKey,
                        final boolean revealed) {
        this(entryKey, titleKey, index, count, kind, textKey, requiresKey, revealed, null);
    }
}
