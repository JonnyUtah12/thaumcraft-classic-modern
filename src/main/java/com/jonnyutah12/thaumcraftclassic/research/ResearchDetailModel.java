package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import com.jonnyutah12.thaumcraftclassic.research.book.BookNoteRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * The Thaumonomicon's per-entry detail: one entry's aspect requirements, its
 * unlock state, and its related-research links — {@code parents},
 * {@code parentsHidden} and {@code siblings} — resolved out of the shipped
 * research graph and the player's own knowledge, and out of nothing else.
 *
 * <p><b>Why a plain object (D-019).</b> The original composes this same
 * information inside a {@code GuiScreen}: {@code GuiResearchBrowser} draws the
 * hover card at {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/
 * GuiResearchBrowser.java:540-660} — the entry's name, its hover text, the
 * warp line, the "not unlocked" card ({@code tc.researchmissing}, {@code :553-562})
 * and the essentia tags with the pool check ({@code :611-660}) — and the graph
 * edges are read for the node links at {@code :288-333} (parents, then
 * siblings). 1.7.10 draws all of it with {@code GL11}/{@code Tessellator}, so
 * none of it is measurable on {@code runGameTestServer}, which is a dedicated
 * server that never constructs a {@code Screen}. This class therefore imports
 * <b>no</b> Minecraft type: the game probes
 * ({@code ThaumonomiconBookGameTest}) build it on the server against the live
 * graph and a real {@code ServerPlayer}'s knowledge, and
 * {@code ResearchDetailModelTest} builds it in a plain JVM against the raw
 * JSON. {@code ThaumonomiconScreen} draws the lines this class computes and
 * keeps no list of its own.</p>
 *
 * <p><b>Unlock state is the server's, not a new store.</b> The state comes
 * from the two research authorities the port already ships and that TC-M3-01/02
 * verified: {@link ResearchManager#isResearchComplete(PlayerKnowledgeData, String)}
 * (original {@code ResearchManager.isResearchComplete}) and
 * {@link ResearchManager#doesPlayerHaveRequisites(PlayerKnowledgeData, String)}
 * (original {@code doesPlayerHaveRequisites}, empty-completed-list quirk
 * included). The browser's own gate is the same pair — {@code canUnlockResearch}
 * ({@code GuiResearchBrowser.java:789-807}) walks {@code res.parents} then
 * {@code res.parentsHidden} against the completed list — and this class reads
 * the player's knowledge attachment through those methods, so the book can
 * never disagree with what the server will accept on a completion request.
 * Nothing here stores research state.</p>
 *
 * <p><b>Aspects are the graph's tags and the player's pool.</b> An entry's
 * essentia requirements are its {@code tags} list in
 * {@code data/thaumcraft/research_graph/entries.json} — 172 of the 201 entries
 * carry at least one — and the discovery/affordability half is the player's
 * own {@link PlayerKnowledgeData}, exactly the check the original's hover card
 * runs at {@code GuiResearchBrowser.java:620-640}
 * ({@code hasDiscoveredAspect} for the {@code _unknown.png} stand-in,
 * {@code getAspectPoolFor < amount} for the pulsing "short" alpha and the
 * {@code tc.research.short} line at {@code :655}).</p>
 *
 * <p><b>Labels are the graph's strings, and the key is the fallback.</b> A
 * link is labelled with the target entry's own {@code nameEnUs} (D-016), and
 * with the target's <em>key</em> when the graph gives it no name —
 * {@code CAP_iron} and {@code ROD_wood} are the two such entries of the 201.
 * A key the graph does not contain is never dropped and never fatal: the link
 * renders with {@code inGraph = false} and its key as its label, and the
 * browser refuses to open it. That is the BOOK-3 posture this card is told to
 * assume: the licensing decision on research names may land blocked, and the
 * panel must render the key rather than fail.</p>
 *
 * <p><b>Chrome strings are quotations.</b> The state and warp words are the
 * original's own en_US strings, committed verbatim as quotation under D-016:
 * {@code tc.researchmissing} ("Missing required research!",
 * {@code reference/assets/thaumcraft/lang/en_US.lang:731}),
 * {@code tc.research.short} ({@code :748}) and {@code tc.forbidden} with
 * {@code tc.forbidden.level.1..5} ({@code :722-727}); the original caps the
 * displayed level at 5 ({@code GuiResearchBrowser.java:637-639}). No new lang
 * key is minted, exactly as BOOK-2's row/tab text ships the graph's strings.</p>
 */
public final class ResearchDetailModel {

    /** {@code tc.researchmissing} (en_US.lang:731), verbatim (D-016). */
    public static final String STATE_LOCKED_TEXT = "Missing required research!";
    /** {@code tc.research.short} (en_US.lang:748), verbatim (D-016). */
    public static final String ASPECTS_SHORT_TEXT = "You do not have the required aspects to purchase this";
    /** Port-authored chrome: the panel has no original text for this line. */
    public static final String STATE_KNOWN_TEXT = "Research known";
    /** Port-authored chrome, naming the entry's own essentia tag list. */
    public static final String ASPECTS_LABEL = "Essentia required:";
    /** Port-authored chrome, naming the prerequisites the player is missing. */
    public static final String MISSING_LABEL = "Missing:";
    /** {@code tc.forbidden} without its § codes and with {@code %n} filled in (en_US.lang:722). */
    public static final String FORBIDDEN_TEMPLATE = "Forbidden knowledge (%n)";

    /** The graph field names, used as link kinds so the kind is the data's own. */
    public static final String KIND_PARENT = "parents";
    public static final String KIND_PARENTS_HIDDEN = "parentsHidden";
    public static final String KIND_SIBLINGS = "siblings";

    /**
     * The entry's state for one player, in the original's own terms: completed
     * (in the completed list), available (prerequisites satisfied — the
     * original's {@code canUnlockResearch} true, so its node draws at full
     * brightness and its card offers the purchase/note arm), or locked
     * (prerequisites missing — the card shows {@code tc.researchmissing}).
     */
    public enum State {
        COMPLETED,
        AVAILABLE,
        LOCKED
    }

    /**
     * One essentia requirement: the aspect tag and amount the graph carries,
     * plus whether this player has discovered the aspect and whether their pool
     * covers the amount.
     */
    public record AspectRequirement(String aspect, String constant, int amount,
                                    boolean discovered, boolean affordable) {

        /** The panel's token for this requirement. */
        public String token() {
            return aspect + " " + amount + (discovered ? (affordable ? "" : " (short)") : " (unknown)");
        }
    }

    /**
     * One related-research link: the graph field it came from, the target key,
     * the target's label (its {@code nameEnUs}, else its key), whether the
     * player has completed it, and whether the graph contains it at all.
     */
    public record Link(String kind, String key, String label, boolean completed, boolean inGraph) {

        /** The panel's line for this link. */
        public String text() {
            return groupLabel(kind) + " " + label + (completed ? " (known)" : "");
        }

        /** The panel's label for a link group, keyed off the graph's own field name. */
        public static String groupLabel(final String kind) {
            if (KIND_PARENTS_HIDDEN.equals(kind)) {
                // The original does not draw these as lines (:288-305 draws
                // parents; parentsHidden are the prerequisites it keeps off the
                // map), so the panel names them as the hidden requirements they are.
                return "Hidden requirement:";
            }
            if (KIND_SIBLINGS.equals(kind)) {
                // Display-linked peers, drawn as lines at :307-333 and completed
                // together with the entry (ItemThaumonomicon.java:56-64).
                return "Related:";
            }
            return "Requires:";
        }
    }

    /**
     * The whole detail for one entry. The lines the panel draws come from this
     * record ({@link #lines()}), so the panel's content is measurable without a
     * window.
     */
    public record Detail(String key, String label, String category, Integer pointCost, State state,
                         boolean autoUnlock, int warp, List<AspectRequirement> aspects,
                         List<Link> links, List<String> missingPrerequisites) {

        public Detail {
            aspects = aspects == null ? List.of() : List.copyOf(aspects);
            links = links == null ? List.of() : List.copyOf(links);
            missingPrerequisites = missingPrerequisites == null ? List.of()
                    : List.copyOf(missingPrerequisites);
        }

        /** The links of one kind, in the graph's order. */
        public List<Link> links(final String kind) {
            final List<Link> out = new ArrayList<>();
            for (final Link link : links) {
                if (link.kind().equals(kind)) {
                    out.add(link);
                }
            }
            return List.copyOf(out);
        }

        /** Every link the panel offers, in the graph's order: parents, hidden, siblings. */
        public List<Link> openableLinks() {
            final List<Link> out = new ArrayList<>();
            for (final Link link : links) {
                if (link.inGraph()) {
                    out.add(link);
                }
            }
            return List.copyOf(out);
        }

        /** True when the player's pool covers every aspect requirement. */
        public boolean aspectsAffordable() {
            for (final AspectRequirement requirement : aspects) {
                if (!requirement.affordable()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * One drawn line of the panel: its text, and the entry key it opens
         * when the player clicks it (null for a line that links nowhere). The
         * {@code Screen} draws these and hit-tests these; it adds no copy.
         */
        public record Line(String text, String linkKey) {
        }

        /**
         * The panel's lines with their click targets: state, missing
         * prerequisites, warp, essentia, then one line per related entry, in
         * the graph's order.
         */
        public List<Line> displayLines() {
            final List<Line> lines = new ArrayList<>();
            lines.add(new Line(switch (state) {
                case COMPLETED -> STATE_KNOWN_TEXT;
                case AVAILABLE -> "Available to research" + (pointCost == null ? "" : " (" + pointCost + " pts)");
                case LOCKED -> STATE_LOCKED_TEXT;
            }, null));
            if (!missingPrerequisites.isEmpty()) {
                lines.add(new Line(MISSING_LABEL + " " + String.join(", ", missingPrerequisites), null));
            }
            if (warp > 0) {
                lines.add(new Line(FORBIDDEN_TEMPLATE.replace("%n", warpLevel(warp)), null));
            }
            if (!aspects.isEmpty()) {
                final List<String> tokens = new ArrayList<>();
                for (final AspectRequirement requirement : aspects) {
                    tokens.add(requirement.token());
                }
                lines.add(new Line(ASPECTS_LABEL + " " + String.join(", ", tokens), null));
                if (!aspectsAffordable()) {
                    lines.add(new Line(ASPECTS_SHORT_TEXT, null));
                }
            }
            for (final Link link : links) {
                // A link the graph cannot resolve stays on the panel, labelled
                // with its key, and opens nothing: the panel renders the key
                // rather than failing (the BOOK-3 posture this card assumes).
                lines.add(new Line(link.text(), link.inGraph() ? link.key() : null));
            }
            return List.copyOf(lines);
        }

        /** The panel's plain text lines, in draw order. */
        public List<String> lines() {
            final List<String> out = new ArrayList<>();
            for (final Line line : displayLines()) {
                out.add(line.text());
            }
            return List.copyOf(out);
        }

        /** The original's warp words (en_US.lang:723-727), capped at 5 as the hover card caps it. */
        private static String warpLevel(final int warp) {
            return switch (Math.min(warp, 5)) {
                case 1 -> "Mostly Harmless";
                case 2 -> "Minor";
                case 3 -> "Moderate";
                case 4 -> "Dangerous";
                default -> "Taboo";
            };
        }
    }

    private final ResearchGraph graph;

    /**
     * What the clicking side knows about itself, as the original's click site
     * read it in-process ({@code GuiResearchBrowser.java:683-727}).
     *
     * @param researchDifficulty  the {@code research_difficulty} config value,
     *                            which decides the purchase family (easy -1 =
     *                            every tagged entry, 0 = only secondaries)
     * @param scribingInk         the browser's {@code hasScribestuff}
     *                            ({@code :104-108}): a scribe tool with ink left
     *                            AND paper carried
     * @param carriesNoteForEntry the inventory already holds a note for this
     *                            entry ({@code ResearchManager.getResearchSlot},
     *                            {@code :712} gate)
     * @param aspectPool          the player's pool total per aspect tag
     *                            ({@code :692-711} affordability)
     */
    public record AffordanceContext(int researchDifficulty, boolean scribingInk,
                                    boolean carriesNoteForEntry,
                                    java.util.function.ToIntFunction<String> aspectPool) {
    }

    /**
     * The book's click affordance for one entry: the arm a click sends and the
     * hint line the original drew beside the node (TC-M3-04-s-v, merged onto
     * this screen by INTEGRATE-BOOK-LANES / t_ee886c7e).
     *
     * <p>The rule is {@link BookNoteRequest}'s — this method only feeds it the
     * graph, the player's own completed-key set and the client's own
     * inventory/config/aspect-pool readings, so the answer is measurable on a
     * dedicated server (D-019 rule 2: this class imports no Minecraft type
     * beyond the record's own {@code ItemStack}-free shape).</p>
     *
     * <p><b>Two gates, both already shipped.</b> The panel's state line is
     * {@link ResearchManager}'s authority (the pair the server enforces with);
     * the hint line is the original's own click gate
     * ({@code canUnlockResearch} + the purchase/note tests). This merge adds no
     * third gate and no second completion path: an {@link BookNoteRequest.Arm}
     * is only ever a request, and
     * {@code ResearchManager.handlePlayerCompleteRequest} stays the sole path
     * that completes research (BD-13).</p>
     */
    public BookNoteRequest.Request affordance(final String entryKey,
                                              final PlayerKnowledgeData knowledge,
                                              final AffordanceContext context) {
        final ResearchEntryData entry = entryKey == null ? null : this.graph.entry(entryKey);
        if (entry == null || context == null) {
            return new BookNoteRequest.Request(BookNoteRequest.Arm.NONE, null, null);
        }
        final PlayerKnowledgeData known = knowledge == null ? PlayerKnowledgeData.empty() : knowledge;
        return BookNoteRequest.decide(this.graph, entry,
                com.jonnyutah12.thaumcraftclassic.research.book.ThaumonomiconKnowledge
                        .of(known.researchKeys()),
                context.researchDifficulty(), context.scribingInk(),
                context.carriesNoteForEntry(), context.aspectPool());
    }

    public ResearchDetailModel(final ResearchGraph graph) {
        this.graph = graph;
    }

    /** The model over the graph this jar ships. */
    public static ResearchDetailModel forShippedGraph() {
        return new ResearchDetailModel(ResearchManager.graph());
    }

    /** The graph this panel resolves out of. */
    public ResearchGraph graph() {
        return this.graph;
    }

    /**
     * Resolve one entry for one player.
     *
     * @return the detail, or null when the graph has no such entry (the panel
     *     then shows nothing; a link whose target is missing is not this case —
     *     it renders with its key)
     */
    public Detail detail(final String entryKey, final PlayerKnowledgeData knowledge) {
        final ResearchEntryData entry = entryKey == null ? null : this.graph.entry(entryKey);
        if (entry == null) {
            return null;
        }
        final PlayerKnowledgeData known = knowledge == null ? PlayerKnowledgeData.empty() : knowledge;

        // The state is the server's own pair of authorities — no third store.
        final State state = ResearchManager.isResearchComplete(known, entry.key())
                ? State.COMPLETED
                : (ResearchManager.doesPlayerHaveRequisites(known, entry.key())
                        ? State.AVAILABLE : State.LOCKED);

        final List<AspectRequirement> aspects = new ArrayList<>();
        for (final ResearchEntryData.AspectTag tag : nullSafe(entry.tags())) {
            final boolean discovered = known.hasAspectDiscovered(tag.aspect());
            aspects.add(new AspectRequirement(tag.aspect(), tag.constant(), tag.amount(),
                    discovered, discovered && known.aspectAmount(tag.aspect()) >= tag.amount()));
        }

        final List<Link> links = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        for (final String kind : List.of(KIND_PARENT, KIND_PARENTS_HIDDEN, KIND_SIBLINGS)) {
            for (final String target : references(entry, kind)) {
                final ResearchEntryData targetEntry = this.graph.entry(target);
                final boolean completed = ResearchManager.isResearchComplete(known, target);
                links.add(new Link(kind, target,
                        targetEntry == null ? target : display(targetEntry.nameEnUs(), target),
                        completed, targetEntry != null));
                if (!completed && !KIND_SIBLINGS.equals(kind)) {
                    // The card's "Missing required research!" list: the
                    // prerequisites, hidden ones included (canUnlockResearch
                    // checks both, GuiResearchBrowser.java:789-807). Siblings
                    // are not prerequisites and never gate the entry.
                    missing.add(targetEntry == null ? target : display(targetEntry.nameEnUs(), target));
                }
            }
        }

        return new Detail(entry.key(), display(entry.nameEnUs(), entry.key()), entry.category(),
                entry.complexity(), state, entry.flags() != null && entry.flags().autoUnlock(),
                entry.warp(), List.copyOf(aspects), List.copyOf(links), List.copyOf(missing));
    }

    /** The graph's own edge list for one link kind, in the JSON's order. */
    public static List<String> references(final ResearchEntryData entry, final String kind) {
        if (entry == null) {
            return List.of();
        }
        if (KIND_PARENTS_HIDDEN.equals(kind)) {
            return nullSafe(entry.parentsHidden());
        }
        if (KIND_SIBLINGS.equals(kind)) {
            return nullSafe(entry.siblings());
        }
        return nullSafe(entry.parents());
    }

    /** Every link kind this model resolves, in the panel's draw order. */
    public static List<String> linkKinds() {
        return List.of(KIND_PARENT, KIND_PARENTS_HIDDEN, KIND_SIBLINGS);
    }

    private static String display(final String fromGraph, final String key) {
        return fromGraph == null || fromGraph.isBlank() ? key : fromGraph;
    }

    private static <T> List<T> nullSafe(final List<T> list) {
        return list == null ? List.of() : list;
    }
}
