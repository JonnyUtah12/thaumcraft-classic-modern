package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * TC-M3-03: the Minecraft-free view model behind the Thaumonomicon screens.
 *
 * <p>Every rule the original {@code GuiResearchBrowser} / {@code GuiResearchRecipe}
 * computed inline while drawing is extracted here as a pure function over the
 * {@link ResearchGraph} + {@link PlayerKnowledgeData}, so the book's state
 * correctness (category gating, entry reveal, locked/unlocked/complete states,
 * page filtering) is unit-testable without a client (docs/visual-parity.md).
 * The screens stay thin renderers over these results.</p>
 *
 * <p>Parity notes, reproduced exactly from the original:</p>
 * <ul>
 *   <li>The ELDRITCH tab is hidden until {@code ELDRITCHMINOR} is complete
 *       ({@code GuiResearchBrowser} hard-codes this for that one category).</li>
 *   <li>Reveal rules ({@code func_73863_a} node loop): lost entries are invisible
 *       until completed; hidden entries until their {@code @}-key; concealed
 *       entries are drawn dark until their prerequisites are met; virtual
 *       entries are never drawn.</li>
 *   <li>{@link #canUnlockForDisplay} is the original GUI's
 *       {@code canUnlockResearch}, NOT {@code ResearchManager.doesPlayerHaveRequisites}:
 *       an unknown parent key counts as satisfied, and the empty-completed-list
 *       quirk does not apply to the display gate.</li>
 *   <li>Node frames and the primary/secondary cost split follow the original's
 *       {@code Config.researchDifficulty} branches verbatim.</li>
 *   <li>{@link #bookPages} is the {@code GuiResearchRecipe} constructor:
 *       TEXT_CONCEALED pages are dropped until their {@code requires} entry is
 *       complete, and the ASPECTS entry appends live discovered-aspect pages,
 *       four aspects per page, exactly as the original generated them.</li>
 * </ul>
 */
public final class ThaumonomiconModel {

    /** Draw state of a browser node (original: white / pulsing / 0.3 grey). */
    public enum EntryState { COMPLETED, UNLOCKABLE, LOCKED }

    /** Cost display mode of an incomplete entry (original primary/secondary split). */
    public enum CostMode { NONE, PRIMARY, SECONDARY }

    /** A visible browser entry with its computed state. */
    public record EntryView(ResearchEntryData entry, EntryState state, CostMode costMode) {
    }

    /**
     * A connection line between two entries in the same category.
     * Kinds carry the original colors: DONE = dark grey (child complete),
     * GREEN = wiggling green (child visible, parent complete),
     * BLUE = wiggling blue (child visible, parent visible),
     * SIBLING_DONE = dark blue sibling link for a completed child.
     */
    public record LinkView(String from, String to, LinkKind kind) {
        public enum LinkKind { DONE, GREEN, BLUE, SIBLING_DONE }
    }

    /**
     * The node frame texture location inside {@code textures/gui/gui_research.png}
     * (26x26 at node position -2,-2), plus the special (gold) overlay when set.
     * Coordinates are the original's atlas offsets.
     */
    public record NodeFrame(int u, int v, boolean specialOverlay) {
        public static final int SPECIAL_U = 26;
        public static final int SPECIAL_V = 230;
    }

    /** One generated ASPECTS-page aspect: tag, pool amount, component tags (empty = primal). */
    public record AspectPageEntry(String tag, int amount, List<String> components) {
    }

    private ThaumonomiconModel() {
    }

    // ------------------------------------------------------------------ categories

    /**
     * Tabs in original registration order; ELDRITCH hidden until ELDRITCHMINOR
     * is complete ({@code GuiResearchBrowser} tab loop).
     */
    public static List<ResearchCategoryData> visibleCategories(final ResearchGraph graph,
                                                              final PlayerKnowledgeData knowledge) {
        final List<ResearchCategoryData> visible = new ArrayList<>();
        for (final ResearchCategoryData category : graph.categories()) {
            if (!"ELDRITCH".equals(category.key())
                    || ResearchManager.isResearchComplete(knowledge, "ELDRITCHMINOR")) {
                visible.add(category);
            }
        }
        return visible;
    }

    /**
     * The original {@code ResearchCategoryList} min/max display bounds, used to
     * clamp browser panning. Bounds cover every registered entry (the original
     * widened them in {@code addResearch} before any visibility filtering).
     */
    public record CategoryBounds(int minColumn, int minRow, int maxColumn, int maxRow) {
    }

    public static CategoryBounds categoryBounds(final ResearchGraph graph, final String category) {
        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int maxRow = Integer.MIN_VALUE;
        for (final ResearchEntryData entry : graph.entriesIn(category)) {
            minCol = Math.min(minCol, entry.column());
            minRow = Math.min(minRow, entry.row());
            maxCol = Math.max(maxCol, entry.column());
            maxRow = Math.max(maxRow, entry.row());
        }
        return new CategoryBounds(minCol, minRow, maxCol, maxRow);
    }

    // ---------------------------------------------------------------------- entries

    /** Original {@code GuiResearchBrowser.canUnlockResearch}: unknown parents count as satisfied. */
    public static boolean canUnlockForDisplay(final PlayerKnowledgeData knowledge, final ResearchEntryData entry) {
        final var completed = knowledge.researchKeys();
        for (final String parent : nonNull(entry.parents())) {
            final ResearchEntryData parentEntry = ResearchManager.graph().entry(parent);
            if (parentEntry != null && !completed.contains(parentEntry.key())) {
                return false;
            }
        }
        for (final String parent : nonNull(entry.parentsHidden())) {
            final ResearchEntryData parentEntry = ResearchManager.graph().entry(parent);
            if (parentEntry != null && !completed.contains(parentEntry.key())) {
                return false;
            }
        }
        return true;
    }

    /** The original node-loop visibility test (see class javadoc). */
    public static boolean isRevealed(final PlayerKnowledgeData knowledge, final ResearchEntryData entry) {
        final ResearchEntryData.Flags flags = entry.flags();
        if (flags != null && flags.virtual()) {
            return false;
        }
        final var completed = knowledge.researchKeys();
        if (completed.contains(entry.key())) {
            return true;
        }
        if (completed.contains("@" + entry.key())) {
            return true;
        }
        if (flags == null) {
            return true;
        }
        if (flags.lost() || flags.hidden()) {
            return false;
        }
        return !flags.concealed() || canUnlockForDisplay(knowledge, entry);
    }

    public static EntryState stateOf(final PlayerKnowledgeData knowledge, final ResearchEntryData entry) {
        if (knowledge.researchKeys().contains(entry.key())) {
            return EntryState.COMPLETED;
        }
        return canUnlockForDisplay(knowledge, entry) ? EntryState.UNLOCKABLE : EntryState.LOCKED;
    }

    /**
     * Original hover/click split:
     * {@code secondary = !complete && tags non-empty && (difficulty == -1 || (difficulty == 0 && isSecondary))},
     * {@code primary = !secondary && !complete}.
     */
    public static CostMode costMode(final PlayerKnowledgeData knowledge, final ResearchEntryData entry,
                                    final int difficulty) {
        if (knowledge.researchKeys().contains(entry.key())) {
            return CostMode.NONE;
        }
        final boolean hasTags = entry.tags() != null && !entry.tags().isEmpty();
        final boolean secondary = hasTags
                && (difficulty == -1 || (difficulty == 0 && entry.flags() != null && entry.flags().secondary()));
        return secondary ? CostMode.SECONDARY : CostMode.PRIMARY;
    }

    /** Every entry of a category the original would draw, with its state. */
    public static List<EntryView> visibleEntries(final ResearchGraph graph, final PlayerKnowledgeData knowledge,
                                                 final String category, final int difficulty) {
        final List<EntryView> views = new ArrayList<>();
        for (final ResearchEntryData entry : graph.entriesIn(category)) {
            if (!isRevealed(knowledge, entry)) {
                continue;
            }
            views.add(new EntryView(entry, stateOf(knowledge, entry), costMode(knowledge, entry, difficulty)));
        }
        return views;
    }

    /**
     * The original line-drawing pass: parent links (same category, non-virtual
     * parents) and sibling links (sibling must not already list the child as a
     * parent), with the original color selection.
     */
    public static List<LinkView> links(final ResearchGraph graph, final PlayerKnowledgeData knowledge,
                                       final String category) {
        final List<LinkView> links = new ArrayList<>();
        final var completed = knowledge.researchKeys();
        for (final ResearchEntryData entry : graph.entriesIn(category)) {
            if (entry.flags() != null && entry.flags().virtual()) {
                continue;
            }
            for (final String parentKey : nonNull(entry.parents())) {
                final ResearchEntryData parent = graph.entry(parentKey);
                if (parent == null || !parent.category().equals(category) || isVirtual(parent)) {
                    continue;
                }
                final LinkView link = linkFor(entry, parent, knowledge, completed, LinkKind.DONE);
                if (link != null) {
                    links.add(link);
                }
            }
            for (final String siblingKey : nonNull(entry.siblings())) {
                final ResearchEntryData sibling = graph.entry(siblingKey);
                if (sibling == null || !sibling.category().equals(category) || isVirtual(sibling)) {
                    continue;
                }
                if (nonNull(sibling.parents()).contains(entry.key())) {
                    continue;
                }
                final LinkView link = linkFor(entry, sibling, knowledge, completed, LinkKind.SIBLING_DONE);
                if (link != null) {
                    links.add(link);
                }
            }
        }
        return links;
    }

    private static LinkView linkFor(final ResearchEntryData child, final ResearchEntryData other,
                                    final PlayerKnowledgeData knowledge, final Set<String> completed,
                                    final LinkKind doneKind) {
        if (completed.contains(child.key())) {
            return new LinkView(child.key(), other.key(), doneKind);
        }
        if (!isDisplayable(child, knowledge, completed)) {
            return null;
        }
        if (completed.contains(other.key())) {
            return new LinkView(child.key(), other.key(), LinkKind.GREEN);
        }
        if (isDisplayable(other, knowledge, completed)) {
            return new LinkView(child.key(), other.key(), LinkKind.BLUE);
        }
        return null;
    }

    /** The original line pass's child/parent visibility gate (lost/hidden/concealed rules). */
    private static boolean isDisplayable(final ResearchEntryData entry, final PlayerKnowledgeData knowledge,
                                         final Set<String> completed) {
        final ResearchEntryData.Flags flags = entry.flags();
        if (flags == null) {
            return true;
        }
        if (flags.lost()) {
            return false;
        }
        if (flags.hidden() && !completed.contains("@" + entry.key())) {
            return false;
        }
        return !flags.concealed() || canUnlockForDisplay(knowledge, entry);
    }

    // ------------------------------------------------------------------------ frames

    /**
     * The original node-frame atlas selection (gui_research.png, 26x26):
     * round → (54,230); hidden → (86,230) or (230,230) under easy/secondary
     * difficulty; normal → (0,230) or (110,230); special adds the (26,230)
     * gold overlay.
     */
    public static NodeFrame nodeFrame(final ResearchEntryData entry, final int difficulty) {
        final ResearchEntryData.Flags flags = entry.flags();
        final boolean round = flags != null && flags.round();
        final boolean hidden = flags != null && flags.hidden();
        final boolean special = flags != null && flags.special();
        // Original: Config.researchDifficulty != -1 && (researchDifficulty != 0 || !isSecondary)
        // selects the plain frame; the easy/secondary frames are the far atlas slots.
        final boolean plainFrame = difficulty != -1 && (difficulty != 0 || !secondaryFlag(entry));
        final int u;
        final int v;
        if (round) {
            u = 54;
            v = 230;
        } else if (hidden) {
            u = plainFrame ? 86 : 230;
            v = 230;
        } else {
            u = plainFrame ? 0 : 110;
            v = 230;
        }
        return new NodeFrame(u, v, special);
    }

    private static boolean secondaryFlag(final ResearchEntryData entry) {
        return entry.flags() != null && entry.flags().secondary();
    }

    // ------------------------------------------------------------------- book pages

    /**
     * The {@code GuiResearchRecipe} page list: TEXT_CONCEALED pages are dropped
     * until their {@code requires} entry is complete; the ASPECTS entry appends
     * live discovered-aspect pages (four per page) exactly as the original
     * generated them from the player's aspect knowledge.
     */
    public static List<ResearchPageData> bookPages(final ResearchGraph graph, final PlayerKnowledgeData knowledge,
                                                   final String entryKey) {
        final ResearchEntryData entry = graph.entry(entryKey);
        if (entry == null) {
            return List.of();
        }
        final List<ResearchPageData> pages = new ArrayList<>();
        for (final ResearchPageData page : nonNull(entry.pages())) {
            if (page == null) {
                continue;
            }
            if (page.type() == ResearchPageData.Type.TEXT_CONCEALED
                    && page.requires() != null
                    && !ResearchManager.isResearchComplete(knowledge, page.requires())) {
                continue;
            }
            pages.add(page);
        }
        if ("ASPECTS".equals(entryKey)) {
            // The original rebuilt the ASPECTS entry from its static pages
            // (unfiltered) plus generated pages carrying up to four aspects
            // each (GuiResearchRecipe constructor, the `count == 4` grouping).
            final List<ResearchPageData> withAspects = new ArrayList<>(nonNull(entry.pages()));
            final List<AspectPageEntry> discovered = discoveredAspects(knowledge);
            final List<AspectPageEntry> group = new ArrayList<>();
            for (final AspectPageEntry aspect : discovered) {
                group.add(aspect);
                if (group.size() == 4) {
                    withAspects.add(aspectPage(group));
                    group.clear();
                }
            }
            if (!group.isEmpty()) {
                withAspects.add(aspectPage(group));
            }
            return withAspects;
        }
        return pages;
    }

    /** One generated ASPECTS page for a group of up to four aspects. */
    private static ResearchPageData aspectPage(final List<AspectPageEntry> aspects) {
        final List<ResearchEntryData.AspectTag> tags = new ArrayList<>();
        for (final AspectPageEntry aspect : aspects) {
            tags.add(new ResearchEntryData.AspectTag(aspect.tag(), aspect.tag().toUpperCase(), aspect.amount()));
        }
        return new ResearchPageData(ResearchPageData.Type.ASPECTS, null, null, null, null, null, null, null, tags);
    }

    /**
     * Discovered aspects with a positive pool, sorted by tag — the original
     * {@code getAspectsDiscovered} + {@code getAspectsSorted} pair (the
     * AspectList drops zero amounts; Aspect sorts by name).
     */
    public static List<AspectPageEntry> discoveredAspects(final PlayerKnowledgeData knowledge) {
        final List<AspectPageEntry> out = new ArrayList<>();
        for (final var entry : knowledge.aspectsDiscovered()) {
            if (entry.amount() <= 0) {
                continue;
            }
            final var definition = aspectDefinition(entry.key());
            out.add(new AspectPageEntry(entry.key(), entry.amount(),
                    definition == null ? List.of() : definition.components()));
        }
        out.sort(Comparator.comparing(AspectPageEntry::tag));
        return out;
    }

    private static com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions.Definition
    aspectDefinition(final String tag) {
        for (final var definition : com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions.all()) {
            if (definition.tag().equals(tag)) {
                return definition;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------- helpers

    private static List<String> nonNull(final List<String> list) {
        return list == null ? List.of() : list;
    }

    private static boolean isVirtual(final ResearchEntryData entry) {
        return entry.flags() != null && entry.flags().virtual();
    }
}
