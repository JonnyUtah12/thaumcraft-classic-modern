package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.book.BookNoteRequest;
import com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconKnowledge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-04-s-v unit guards for {@link BookNoteRequest} — the Thaumonomicon
 * node-click arms, read straight out of the decompiled browser
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchBrowser.java}):
 * the prerequisite gate {@code canUnlockResearch} ({@code :789-809}), the
 * purchase/primary split the click makes ({@code :688-727}), the aspect-pool
 * affordability loop ({@code :692-699}), the hint lines and their colours
 * ({@code :560}, {@code :614-618}, {@code :656-658}) and the popup window
 * ({@code :186}, {@code :722}).
 *
 * <p>The synthetic graph keeps the shape the rules read: parents, hidden
 * parents, tags and the secondary flag. The pool is a plain map, so every
 * assertion is about the rule and not about Minecraft.</p>
 */
class BookNoteRequestRulesTest {

    private static final ResearchGraph GRAPH = new ResearchGraph(List.of(), List.of(
            entry("PARENT", List.of(), List.of(), List.of(), false),
            entry("PRIMARY", List.of("PARENT"), List.of(),
                    List.of(new ResearchEntryData.AspectTag("aer", "AIR", 3)), false),
            entry("SECONDARY", List.of("PARENT"), List.of(),
                    List.of(new ResearchEntryData.AspectTag("ignis", "FIRE", 3),
                            new ResearchEntryData.AspectTag("aqua", "WATER", 6)), true),
            entry("UNTAGGED_SECONDARY", List.of("PARENT"), List.of(), List.of(), true),
            entry("HIDDEN_GATED", List.of(), List.of("PARENT"), List.of(), false),
            entry("GHOST_GATED", List.of("NOT_IN_GRAPH"), List.of(), List.of(), false)));

    private static final ToIntFunction<String> POOL = pool(Map.of("aer", 5, "ignis", 3, "aqua", 6));
    private static final ToIntFunction<String> NO_POOL = pool(Map.of());

    /** An aspect pool: the shape the screen hands the rules, zero for unknown. */
    private static ToIntFunction<String> pool(final Map<String, Integer> discovered) {
        return tag -> discovered.getOrDefault(tag, 0);
    }
    private static final int NORMAL_DIFFICULTY = 0;

    // ------------------------------------------------------------- prerequisites

    @Test
    void everyDrawnAndHiddenParentMustBeCompleted() {
        assertTrue(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("PARENT"), all()));
        assertFalse(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("PRIMARY"), none()),
                "the drawn parent gate: PRIMARY needs PARENT");
        assertTrue(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("PRIMARY"), knows("PARENT")),
                "the drawn parent gate opens once PARENT is completed");
        assertFalse(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("HIDDEN_GATED"), none()),
                "a hidden parent gates the click exactly like a drawn one (:789-809)");
        assertTrue(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("HIDDEN_GATED"), knows("PARENT")),
                "and it opens on the hidden parent alone, with no drawn parent to satisfy");
        assertTrue(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("GHOST_GATED"), none()),
                "the original only failed on parents its registry resolved "
                        + "(getResearch(pt) != null && !completed.contains(pt))");
    }

    @Test
    void theNoteBasedKeyOfAParentDoesNotCount() {
        assertFalse(BookNoteRequest.canUnlock(GRAPH, GRAPH.entry("PRIMARY"), knows("@PARENT")),
                "the clue unlock writes '@key'; the gate tests the plain key");
    }

    // ------------------------------------------------------------ purchase split

    @Test
    void difficultyDecidesThePurchaseFamily() {
        assertTrue(BookNoteRequest.isPurchaseFamily(GRAPH.entry("SECONDARY"), NORMAL_DIFFICULTY),
                "difficulty 0: a secondary with tags is purchasable (:688-690)");
        assertFalse(BookNoteRequest.isPurchaseFamily(GRAPH.entry("PRIMARY"), NORMAL_DIFFICULTY),
                "difficulty 0: a primary with tags is note-based, not purchasable");
        assertTrue(BookNoteRequest.isPurchaseFamily(GRAPH.entry("PRIMARY"), -1),
                "difficulty -1 (easy): every tagged entry is purchasable");
        assertFalse(BookNoteRequest.isPurchaseFamily(GRAPH.entry("SECONDARY"), 1),
                "difficulty 1 (hard): nothing is purchasable");
        assertFalse(BookNoteRequest.isPurchaseFamily(GRAPH.entry("UNTAGGED_SECONDARY"), NORMAL_DIFFICULTY),
                "an entry with no tags has nothing to pay, so it is not in the family");
    }

    @Test
    void thePoolMustCoverEveryTag() {
        assertTrue(BookNoteRequest.hasAspects(GRAPH.entry("PRIMARY"), POOL), "5 aer covers a cost of 3");
        assertTrue(BookNoteRequest.hasAspects(GRAPH.entry("SECONDARY"), pool(Map.of("ignis", 3, "aqua", 6))),
                "a pool that exactly equals the cost is payable (the original's reduce check)");
        assertFalse(BookNoteRequest.hasAspects(GRAPH.entry("SECONDARY"), pool(Map.of("ignis", 4))),
                "an undiscovered aspect has an empty pool, so the cost is not covered (:622-640)");
        assertFalse(BookNoteRequest.hasAspects(GRAPH.entry("SECONDARY"), NO_POOL));
    }

    // ---------------------------------------------------------------- the arms

    @Test
    void aKnownEntryIsNeverARequest() {
        final BookNoteRequest.Request request =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("SECONDARY"), knows("SECONDARY"),
                        NORMAL_DIFFICULTY, true, false, POOL);
        assertEquals(BookNoteRequest.Arm.NONE, request.arm());
        assertNull(request.hintKey(),
                "the original's completed branch opens the entry view and draws no hint line");
        assertEquals("SECONDARY", request.key());
    }

    @Test
    void lockedEntriesAskForTheMissingResearchLine() {
        final BookNoteRequest.Request request =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("PRIMARY"), none(),
                        NORMAL_DIFFICULTY, true, false, POOL);
        assertEquals(BookNoteRequest.Arm.NONE, request.arm());
        assertEquals(BookNoteRequest.KEY_MISSING, request.hintKey());
        assertEquals(-9416624, BookNoteRequest.colorFor(BookNoteRequest.KEY_MISSING));
    }

    @Test
    void payableSecondariesAreThePurchaseArm() {
        final BookNoteRequest.Request request =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("SECONDARY"), knows("PARENT"),
                        NORMAL_DIFFICULTY, false, false, pool(Map.of("ignis", 3, "aqua", 6)));
        assertEquals(BookNoteRequest.Arm.PURCHASE, request.arm(),
                "the purchase arm ignores scribing ink entirely (:688-711)");
        assertEquals(BookNoteRequest.KEY_PURCHASE, request.hintKey());
        assertEquals(8900331, BookNoteRequest.colorFor(BookNoteRequest.KEY_PURCHASE));
    }

    @Test
    void unpayableSecondariesAskForAspectsNotInk() {
        final BookNoteRequest.Request request =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("SECONDARY"), knows("PARENT"),
                        NORMAL_DIFFICULTY, true, false, NO_POOL);
        assertEquals(BookNoteRequest.Arm.NONE, request.arm(),
                "the original deducts nothing when the pool is short (:692-711)");
        assertEquals(BookNoteRequest.KEY_SHORT, request.hintKey());
        assertEquals(14423100, BookNoteRequest.colorFor(BookNoteRequest.KEY_SHORT));
    }

    @Test
    void theNoteArmNeedsInkAndNoCarriedNote() {
        final BookNoteRequest.Request armed =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("PRIMARY"), knows("PARENT"),
                        NORMAL_DIFFICULTY, true, false, NO_POOL);
        assertEquals(BookNoteRequest.Arm.NOTE, armed.arm());
        assertEquals(BookNoteRequest.KEY_GET_PRIM, armed.hintKey());
        assertEquals(8900331, BookNoteRequest.colorFor(BookNoteRequest.KEY_GET_PRIM));

        final BookNoteRequest.Request dry =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("PRIMARY"), knows("PARENT"),
                        NORMAL_DIFFICULTY, false, false, NO_POOL);
        assertEquals(BookNoteRequest.Arm.NONE, dry.arm());
        assertEquals(BookNoteRequest.KEY_SHORT_PRIM, dry.hintKey(),
                "hasScribestuff fails: the original names the missing tools (:618)");
        assertEquals(14423100, BookNoteRequest.colorFor(BookNoteRequest.KEY_SHORT_PRIM));

        final BookNoteRequest.Request carried =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("PRIMARY"), knows("PARENT"),
                        NORMAL_DIFFICULTY, true, true, NO_POOL);
        assertEquals(BookNoteRequest.Arm.NONE, carried.arm());
        assertEquals(BookNoteRequest.KEY_HAS_NOTE, carried.hintKey(),
                "getResearchSlot != -1: the original refuses a second note (:614)");
        assertEquals(16753920, BookNoteRequest.colorFor(BookNoteRequest.KEY_HAS_NOTE));
    }

    @Test
    void anUntaggedSecondaryFallsToTheNoteArm() {
        final BookNoteRequest.Request request =
                BookNoteRequest.decide(GRAPH, GRAPH.entry("UNTAGGED_SECONDARY"), knows("PARENT"),
                        NORMAL_DIFFICULTY, true, false, NO_POOL);
        assertEquals(BookNoteRequest.Arm.NOTE, request.arm(),
                "the click's own branch order: not in the purchase family means the note arm (:712)");
    }

    // ------------------------------------------------------------------ popup

    @Test
    void thePopupWindowIsTheOriginalsThreeSeconds() {
        assertEquals(3000L, BookNoteRequest.POPUP_MILLIS,
                "popuptime = System.currentTimeMillis() + 3000L (:722), read at :186");
        assertEquals(-7302913, BookNoteRequest.COLOR_POPUP, "the popup text colour (:191)");
        assertEquals(150, BookNoteRequest.POPUP_WRAP, "the popup wraps at 150 px (:189)");
    }

    @Test
    void thePopupNamesTheEntryThroughItsLangKey() {
        // No language manager exists in a unit JVM, so assert on the component
        // the popup draws: the popup key with the entry's own name key as %1$s.
        final var contents = (TranslatableContents)
                BookNoteRequest.popupMessage(GRAPH.entry("PRIMARY")).getContents();
        assertEquals(BookNoteRequest.KEY_POPUP, contents.getKey());
        assertEquals(1, contents.getArgs().length);
        final var name = (TranslatableContents) ((Component) contents.getArgs()[0]).getContents();
        assertEquals("tc.research_name.PRIMARY", name.getKey());
    }

    @Test
    void everyHintKeyTheDecisionCanEmitHasTheOriginalColour() {
        for (final String key : List.of(BookNoteRequest.KEY_MISSING, BookNoteRequest.KEY_HAS_NOTE,
                BookNoteRequest.KEY_GET_PRIM, BookNoteRequest.KEY_SHORT_PRIM,
                BookNoteRequest.KEY_PURCHASE, BookNoteRequest.KEY_SHORT)) {
            assertTrue(BookNoteRequest.colorFor(key) != 0, "no colour for " + key);
        }
        assertEquals(0, BookNoteRequest.colorFor(null));
    }

    // ---------------------------------------------------------------- helpers

    private static ThaumonomiconKnowledge all() {
        return ThaumonomiconKnowledge.all();
    }

    private static ThaumonomiconKnowledge none() {
        return ThaumonomiconKnowledge.NONE;
    }

    private static ThaumonomiconKnowledge knows(final String... keys) {
        return ThaumonomiconKnowledge.of(List.of(keys));
    }

    private static ResearchEntryData entry(final String key,
                                           final List<String> parents,
                                           final List<String> parentsHidden,
                                           final List<ResearchEntryData.AspectTag> tags,
                                           final boolean secondary) {
        return new ResearchEntryData(key, "TEST", 0, 0, 1, null, tags,
                new ResearchEntryData.Icon("none", null, null, null, null, null),
                new ResearchEntryData.Flags(false, secondary, false, false, false, false,
                        false, false, false),
                parents, parentsHidden, List.of(),
                new ResearchEntryData.Triggers(List.of(), List.of(), List.of()),
                0, "tc.research_name." + key, key, "tc.research_text." + key, key, List.of());
    }
}
