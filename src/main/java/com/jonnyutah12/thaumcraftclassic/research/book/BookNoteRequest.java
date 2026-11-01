package com.jonnyutah12.thaumcraftclassic.research.book;

import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The Thaumonomicon's node-click arms (TC-M3-04-s-v): what clicking a node of
 * the research graph asks the server for, and what the book tells the player
 * about that before they click.
 *
 * <p>This is the whole decision of the original browser's
 * {@code GuiResearchBrowser.func_73864_a} — its {@code mouseClicked}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchBrowser.java:682-731})
 * — plus the three hover lines of its tooltip pass
 * ({@code :608-618}, {@code :651-659}) and the prerequisite gate
 * {@code canUnlockResearch} ({@code :789-809}). The original ran those tests
 * client-side and then re-ran its own gate server-side in
 * {@code PacketPlayerCompleteToServer.onMessage}
 * ({@code common/lib/network/playerdata/PacketPlayerCompleteToServer.java:52-92});
 * the port keeps that split. This class answers the client question, and the
 * server's answer stays where it always was:
 * {@link com.jonnyutah12.thaumcraftclassic.research.ResearchManager#handlePlayerCompleteRequest},
 * which is the only path that completes research (through
 * {@code ResearchManager.completeResearch} + {@code ResearchCompletePayload},
 * the same two calls the note item drives in
 * {@code ItemResearchNotes.completeWithSiblings}) or hands out a note (through
 * {@code ResearchManager.createResearchNoteForPlayer}). Nothing in the book
 * completes anything: the book sends a request, the server decides.</p>
 *
 * <p>The rules are a pure function of what the browser read in-process
 * (the player's completed-key set, the entry's tags and flags, the aspect pool,
 * the scribe-tool state, the {@code research_difficulty} config), so a headless
 * probe drives them with a real inventory and a real knowledge view. That is
 * also what keeps them honest: no client class is named here, so the same code
 * runs on a dedicated server.</p>
 *
 * <p>The arms, verbatim from the original click handler:</p>
 * <ul>
 *   <li><b>Known entry</b> — no request. The original opened the entry's
 *       recipe view instead ({@code :731-733}); in the port a left click opens
 *       the entry's page (TC-M3-03-s) and stays that behaviour.</li>
 *   <li><b>Unmet prerequisites</b> — no request. The original's
 *       {@code canUnlockResearch}: every drawn parent and every hidden parent
 *       must be completed.</li>
 *   <li><b>Purchase family</b> ({@code :688-690}: the entry carries tags and
 *       {@code research_difficulty} is {@code -1}, or is {@code 0} and the
 *       entry is a secondary) — pays the entry's tag amounts out of the aspect
 *       pool and completes the entry (request type 0), and only when the pool
 *       covers every tag ({@code :692-711}).</li>
 *   <li><b>Note arm</b> ({@code :712-727}) — the entry is not in the purchase
 *       family, the player carries the scribing kit (a scribe tool with ink
 *       left <em>and</em> paper, the browser's {@code hasScribestuff} at
 *       {@code :104-108}), and no note for this entry is already in the
 *       inventory ({@code ResearchManager.getResearchSlot},
 *       {@code ResearchManager.java:185-201}) → request type 1, which the
 *       server answers by spending the ink and handing over a fresh puzzle
 *       note. The player then solves the note; the note is what completes the
 *       research. The browser itself never completes a non-purchase entry.</li>
 * </ul>
 */
public final class BookNoteRequest {

    /** What a node click asks for. */
    public enum Arm {
        /** Nothing to do: known, locked, or not payable. */
        NONE,
        /** Request type 0 — pay the entry's tags out of the aspect pool. */
        PURCHASE,
        /** Request type 1 — spend scribing ink for a puzzle note. */
        NOTE
    }

    /**
     * The decision plus the line the original would have shown under the node.
     *
     * @param arm     what a click would send, {@link Arm#NONE} for a dead click
     * @param key     the entry the decision is about
     * @param hintKey the original's hover line for this state, verbatim key
     */
    public record Request(Arm arm, String key, String hintKey) {

        /** Would a click on this node do anything? */
        public boolean live() {
            return arm != Arm.NONE;
        }
    }

    // --------------------------------------------------- the original lang keys

    /** The browser's "Missing required research!" line ({@code :554-560}). */
    public static final String KEY_MISSING = "tc.researchmissing";
    /** "You already have this research note in your inventory" ({@code :614}). */
    public static final String KEY_HAS_NOTE = "tc.research.hasnote";
    /** "Click to get a research note for this research" ({@code :616}). */
    public static final String KEY_GET_PRIM = "tc.research.getprim";
    /** "You need scribing tools and paper to get this research note" ({@code :618}). */
    public static final String KEY_SHORT_PRIM = "tc.research.shortprim";
    /** "Click to purchase this research" ({@code :656}). */
    public static final String KEY_PURCHASE = "tc.research.purchase";
    /** "You do not have the required aspects to purchase this" ({@code :658}). */
    public static final String KEY_SHORT = "tc.research.short";
    /** The 3 s popup after a note arrived ({@code :717-726}). */
    public static final String KEY_POPUP = "tc.research.popup";

    /**
     * The popup window, in wall-clock milliseconds: the original set
     * {@code this.popuptime = System.currentTimeMillis() + 3000L} at the click
     * ({@code GuiResearchBrowser.java:722}) and drew the message while
     * {@code popuptime > System.currentTimeMillis()} ({@code :186}). Opening an
     * entry view clears it — {@code popuptime = now - 1} ({@code :683}).
     */
    public static final long POPUP_MILLIS = 3000L;

    /**
     * The text colours, verbatim from the original's draw calls: the has-note
     * line is yellow ({@code :614} {@code 16753920}), the clickable
     * purchase/get-note lines are sky blue ({@code :616}, {@code :656}
     * {@code 8900331}), the can't-pay / no-scribing-tools lines are crimson
     * ({@code :618}, {@code :658} {@code 14423100}), the missing-prerequisite
     * line is dark salmon ({@code :560} {@code -9416624}), and the popup text
     * is khaki ({@code :191} {@code -7302913}). The hint box behind the
     * missing-prerequisite line is translucent black ({@code :558}
     * {@code 0x80000000}).
     */
    public static final int COLOR_HAS_NOTE = 16753920;
    public static final int COLOR_LIVE = 8900331;
    public static final int COLOR_SHORT = 14423100;
    public static final int COLOR_MISSING = -9416624;
    public static final int COLOR_POPUP = -7302913;
    /** The popup text's wrap width — the original wrapped at 150 px ({@code :189}). */
    public static final int POPUP_WRAP = 150;

    private BookNoteRequest() {
    }

    // ------------------------------------------------------------------ rules

    /**
     * Original {@code GuiResearchBrowser.canUnlockResearch}
     * ({@code :789-809}): every drawn parent and every hidden parent must be
     * completed. The {@code @}-key of a parent does not count — the original
     * tested the plain key against its completed-research map.
     */
    public static boolean canUnlock(final ResearchGraph graph,
                                    final ResearchEntryData entry,
                                    final ThaumonomiconKnowledge knowledge) {
        if (entry == null) {
            return false;
        }
        for (final String parent : parentsOf(entry)) {
            if (knowledge.knows(parent)) {
                continue;
            }
            // The original resolved the parent through the research registry
            // and only failed the gate when that lookup succeeded
            // (getResearch(pt) != null && !completed.contains(pt)).
            if (graph.entry(parent) != null) {
                return false;
            }
        }
        for (final String parent : hiddenParentsOf(entry)) {
            if (knowledge.knows(parent)) {
                continue;
            }
            if (graph.entry(parent) != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * The original's {@code secondary} test at the click site
     * ({@code :688-690}) and in the tooltip pass ({@code :566-570}): the entry
     * carries tags, and the research difficulty is {@code -1} (easy — every
     * entry is purchasable), or {@code 0} and the entry is a secondary.
     */
    public static boolean isPurchaseFamily(final ResearchEntryData entry, final int researchDifficulty) {
        if (entry == null || entry.tags() == null || entry.tags().isEmpty()) {
            return false;
        }
        final boolean secondary = entry.flags() != null && entry.flags().secondary();
        return researchDifficulty == -1 || (researchDifficulty == 0 && secondary);
    }

    /**
     * The original's affordability loop at the click site
     * ({@code :692-699}): the aspect pool must cover every tag amount. An
     * undiscovered aspect has an empty pool, which is also the tooltip pass's
     * "not enough" case ({@code :622-640}).
     *
     * @param aspectPool tag tag name → the player's pool total for it
     */
    public static boolean hasAspects(final ResearchEntryData entry, final ToIntFunction<String> aspectPool) {
        if (entry == null || entry.tags() == null) {
            return true;
        }
        for (final ResearchEntryData.AspectTag tag : entry.tags()) {
            if (aspectPool.applyAsInt(tag.aspect()) < tag.amount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The click decision. The arguments are what the original read in-process
     * at {@code :683-727}: the completed-key view of the player, the effective
     * {@code research_difficulty}, the browser's {@code hasScribestuff}
     * ({@code :104-108} — a scribe tool with ink left and paper carried),
     * whether the inventory already holds a note for this entry
     * ({@code getResearchSlot != -1}), and the aspect pool.
     */
    public static Request decide(final ResearchGraph graph,
                                 final ResearchEntryData entry,
                                 final ThaumonomiconKnowledge knowledge,
                                 final int researchDifficulty,
                                 final boolean scribingInk,
                                 final boolean carriesNoteForEntry,
                                 final ToIntFunction<String> aspectPool) {
        if (entry == null) {
            return new Request(Arm.NONE, null, null);
        }
        final String key = entry.key();
        final boolean known = knowledge.knows(key);
        if (known) {
            // The original's completed branch: the click opens the entry view,
            // it never sends a request (and shows no hint line).
            return new Request(Arm.NONE, key, null);
        }
        if (!canUnlock(graph, entry, knowledge)) {
            return new Request(Arm.NONE, key, KEY_MISSING);
        }
        if (isPurchaseFamily(entry, researchDifficulty)) {
            final boolean enough = hasAspects(entry, aspectPool);
            return new Request(enough ? Arm.PURCHASE : Arm.NONE, key,
                    enough ? KEY_PURCHASE : KEY_SHORT);
        }
        // The primary family: the note arm. The click gate is
        // hasScribestuff && getResearchSlot == -1 (:712); the hint line names
        // the reason when the gate fails (:613-618).
        if (carriesNoteForEntry) {
            return new Request(Arm.NONE, key, KEY_HAS_NOTE);
        }
        if (!scribingInk) {
            return new Request(Arm.NONE, key, KEY_SHORT_PRIM);
        }
        return new Request(Arm.NOTE, key, KEY_GET_PRIM);
    }

    /** The colour the original drew this hint line in, or 0 for no hint. */
    public static int colorFor(final String hintKey) {
        if (hintKey == null) {
            return 0;
        }
        switch (hintKey) {
            case KEY_HAS_NOTE:
                return COLOR_HAS_NOTE;
            case KEY_PURCHASE:
            case KEY_GET_PRIM:
                return COLOR_LIVE;
            case KEY_SHORT:
            case KEY_SHORT_PRIM:
                return COLOR_SHORT;
            case KEY_MISSING:
                return COLOR_MISSING;
            default:
                return 0;
        }
    }

    /**
     * The {@code tc.research.popup} line for a note that just landed. The
     * original built {@code new ChatComponentTranslation(lang("tc.research.popup"),
     * "" + currentHighlight.getName())} ({@code :723-726}) — the formatted lang
     * string as the key with the entry name as its {@code %1$s} argument;
     * 1.21.1 expresses the same thing as a translatable with a translatable
     * argument, which resolves through the language system.
     */
    public static net.minecraft.network.chat.Component popupMessage(final ResearchEntryData entry) {
        final String name = entry == null ? "" : entry.nameKey();
        return net.minecraft.network.chat.Component
                .translatable(KEY_POPUP, net.minecraft.network.chat.Component.translatable(name));
    }

    // ---------------------------------------------------------------- helpers

    private static List<String> parentsOf(final ResearchEntryData entry) {
        return entry.parents() == null ? List.of() : entry.parents();
    }

    private static List<String> hiddenParentsOf(final ResearchEntryData entry) {
        return entry.parentsHidden() == null ? List.of() : entry.parentsHidden();
    }
}
