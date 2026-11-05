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
 * @param entryKey    the entry the page belongs to
 * @param titleKey    the entry's name lang key, shown as the page heading
 * @param index       0-based index into the entry's original page list
 * @param count       size of the entry's original page list (0 = no pages)
 * @param kind        original page type; null only when the entry has no pages
 * @param textKey     original page text lang key (text/text_concealed/image)
 * @param requiresKey research key gating a concealed page
 * @param revealed    whether the player may read this page
 */
public record BookPageView(
        String entryKey,
        String titleKey,
        int index,
        int count,
        ResearchPageData.Type kind,
        String textKey,
        String requiresKey,
        boolean revealed) {
}
