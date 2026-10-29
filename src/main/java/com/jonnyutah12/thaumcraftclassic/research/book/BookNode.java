package com.jonnyutah12.thaumcraftclassic.research.book;

/**
 * One node of the book's research graph (TC-M3-03-s) — a placed entry of the
 * current category, at its original browser position.
 *
 * <p>{@code column} and {@code row} are the original {@code ResearchItem}
 * browser coordinates from {@code entries.json} (TC-M3-01), so the screen's
 * node placement is the original's layout, not a re-layout. {@code nameKey} is
 * the original {@code tc.research_name.<KEY>} the screen resolves through the
 * language system.</p>
 *
 * <p>{@code visible} is the original's own gate: a {@code hidden} entry is not
 * drawn until the player has revealed it (its {@code @}-key or its plain key).
 * Virtual entries are never nodes at all — the original registered them as
 * prerequisites, not browser positions, so {@link ThaumonomiconBook} drops
 * them.</p>
 *
 * @param key     original research key (the registry key)
 * @param nameKey original lang key for the node label
 * @param column  original browser grid column
 * @param row     original browser grid row
 * @param kind    structural kind (root, has drawn parents, hidden)
 * @param known   whether the player has completed this entry
 * @param visible whether the original would draw the node for this player
 */
public record BookNode(
        String key,
        String nameKey,
        int column,
        int row,
        Kind kind,
        boolean known,
        boolean visible) {

    /** Structural kind of a node, used by the screen for its frame styling. */
    public enum Kind {
        /** No drawn prerequisite: the original draws these as round roots. */
        ROOT,
        /** Reached by at least one drawn prerequisite line. */
        PARENTED,
        /** Carries the original {@code hidden} flag; drawn only once revealed. */
        HIDDEN
    }
}
