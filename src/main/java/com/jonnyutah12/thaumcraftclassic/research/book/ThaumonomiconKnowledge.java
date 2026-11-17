package com.jonnyutah12.thaumcraftclassic.research.book;

import java.util.Collection;
import java.util.Set;

/**
 * The knowledge the book screen may reveal, as seen by the presentation layer
 * (TC-M3-03-s).
 *
 * <p>The original {@code GuiResearchBrowser} asked
 * {@code ResearchManager.isResearchComplete(player, key)} for every entry and
 * page it drew, and separately for the note-based {@code @}-prefixed keys a
 * clue unlock leaves behind ({@code ResearchManager.java:460} passes
 * {@code "@" + key} to the same call). The presentation layer therefore only
 * needs one question answered — "does this player know this key" — and both the
 * server (the {@code PlayerKnowledgeData} attachment) and the client (the
 * {@code SyncResearchPayload} mirror) can answer it without the book knowing
 * where the answer comes from.</p>
 *
 * <p>{@link #ALL} is the browse-everything view: it is what the graph itself
 * looks like, and it is the default the game tests use so a probe never has to
 * build a player first.</p>
 */
@FunctionalInterface
public interface ThaumonomiconKnowledge {

    /** A player who has completed nothing (every hidden entry stays hidden). */
    ThaumonomiconKnowledge NONE = key -> false;

    /** A full-knowledge view of the graph: every entry and page is revealed. */
    ThaumonomiconKnowledge ALL = key -> true;

    /** Whether the raw key (which may be {@code @}-prefixed) is completed. */
    boolean knows(String key);

    /** A player who has completed nothing. */
    static ThaumonomiconKnowledge none() {
        return NONE;
    }

    /** The full-knowledge view; the graph is rendered as if every entry were unlocked. */
    static ThaumonomiconKnowledge all() {
        return ALL;
    }

    /** The completed-key set, as the attachments and the sync payload carry it. */
    static ThaumonomiconKnowledge of(final Collection<String> completedKeys) {
        final Set<String> keys = Set.copyOf(completedKeys);
        return keys::contains;
    }

    /**
     * The original's double test: an entry is known when its plain key is
     * completed, and a hidden entry is revealed when either the plain key or the
     * note-based {@code @}-key the clue unlock writes is completed.
     */
    default boolean knowsAny(final String key) {
        return knows(key) || knows("@" + key);
    }
}
