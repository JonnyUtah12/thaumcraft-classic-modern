package com.jonnyutah12.thaumcraftclassic.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BOOK-4 (t_341117b8): the per-entry detail — aspect requirements, unlock
 * state, and the {@code parents} / {@code parentsHidden} / {@code siblings}
 * links — is resolved <b>out of the shipped research graph and the player's
 * own knowledge</b>, and out of nothing else.
 *
 * <p><b>The gate.</b> For all 201 entries the detail's link list must equal the
 * three reference lists of that entry in {@code entries.json}, its aspect list
 * must equal the entry's {@code tags}, and its state must be exactly what
 * {@link ResearchManager} says for the same knowledge. The expected sets are
 * read out of the JSON with a raw {@link JsonParser} at test time (D-019 rule
 * 3: the expected set comes from data, never from a list in the test), and the
 * state assertion is computed through {@code ResearchManager} and not through a
 * rule copied into this test — that is what pins "the unlock state comes from
 * the existing server-authoritative semantics" and not from a fourth store
 * this card could quietly invent.</p>
 *
 * <p><b>Why the model and not the screen.</b> {@code runGameTestServer} is a
 * dedicated server: no {@code Screen} is ever constructed (D-019 rule 2), so
 * the detail lives in {@link ResearchDetailModel}, which imports no Minecraft
 * type. The screen draws the lines; this test and
 * {@code ThaumonomiconBookGameTest} measure them.</p>
 *
 * <p>Line cites are to the original
 * {@code reference/decompiled/thaumcraft/thaumcraft/client/gui/GuiResearchBrowser.java}.</p>
 */
class ResearchDetailModelTest {

    /** The shipped graph, raw: the expected set the gate compares against. */
    private static final ShippedGraph SHIPPED = ShippedGraph.read();
    private static final ResearchDetailModel MODEL = ResearchDetailModel.forShippedGraph();

    /** Every entry's links are exactly its three reference lists, in the JSON's order. */
    @Test
    void everyEntryOfTheGraphResolvesADetailWhoseLinksAreTheGraphEdges() {
        assertEquals(201, SHIPPED.entryKeys.size(), "entries.json must carry 201 entries for this card's contract");
        int edges = 0;
        int withParents = 0;
        int withHidden = 0;
        int withSiblings = 0;
        for (final String key : SHIPPED.entryKeys) {
            final ResearchDetailModel.Detail detail =
                    MODEL.detail(key, PlayerKnowledgeData.empty());
            assertNotNull(detail, "the graph's own entry " + key + " must resolve a detail");
            assertEquals(key, detail.key(), "the detail is for the entry it was asked for");
            assertEquals(SHIPPED.category.get(key), detail.category(),
                    key + ": the detail's category is the graph's category field");

            final List<ResearchDetailModel.Link> links = detail.links();
            final List<String> expected = new ArrayList<>();
            for (final String kind : ResearchDetailModel.linkKinds()) {
                for (final String target : SHIPPED.references.get(key).get(kind)) {
                    expected.add(kind + " " + target);
                }
            }
            final List<String> actual = new ArrayList<>();
            for (final ResearchDetailModel.Link link : links) {
                actual.add(link.kind() + " " + link.key());
            }
            assertEquals(expected, actual,
                    key + ": the detail's links must be the entry's parents, parentsHidden and siblings from"
                            + " entries.json, in that order (GuiResearchBrowser.java:288-333 reads the same three"
                            + " arrays for the node's lines)");
            edges += links.size();
            if (!detail.links(ResearchDetailModel.KIND_PARENT).isEmpty()) {
                withParents++;
            }
            if (!detail.links(ResearchDetailModel.KIND_PARENTS_HIDDEN).isEmpty()) {
                withHidden++;
            }
            if (!detail.links(ResearchDetailModel.KIND_SIBLINGS).isEmpty()) {
                withSiblings++;
            }
        }
        // The sweep must have actually seen all three kinds, or this test proves
        // nothing about the two lists the tab strip never shows.
        assertEquals(SHIPPED.countWith(ResearchDetailModel.KIND_PARENT), withParents,
                "the detail panel must surface the parents edges the data carries");
        assertEquals(SHIPPED.countWith(ResearchDetailModel.KIND_PARENTS_HIDDEN), withHidden,
                "the detail panel must surface the parentsHidden edges the data carries");
        assertEquals(SHIPPED.countWith(ResearchDetailModel.KIND_SIBLINGS), withSiblings,
                "the detail panel must surface the siblings edges the data carries");
        assertTrue(SHIPPED.countWith(ResearchDetailModel.KIND_PARENTS_HIDDEN) > 0,
                "entries.json carries no parentsHidden edges — the graph's hidden parents are part of this gate");
        assertTrue(edges > 0, "the sweep resolved no edges at all");
    }

    /** A link is labelled with the target's own nameEnUs, and with its key when the graph names it nothing. */
    @Test
    void linkLabelsComeFromTheTargetEntryAndThePanelNeverInventsCopy() {
        for (final String key : SHIPPED.entryKeys) {
            final ResearchDetailModel.Detail detail =
                    MODEL.detail(key, PlayerKnowledgeData.empty());
            for (final ResearchDetailModel.Link link : detail.links()) {
                final String named = SHIPPED.nameEnUs.get(link.key());
                final String expected = named == null || named.isBlank() ? link.key() : named;
                assertEquals(expected, link.label(),
                        key + " -> " + link.key() + ": a link's label is the target's nameEnUs from the graph,"
                                + " falling back to the target's key (the BOOK-3 posture: render the key, do not fail)");
                assertTrue(link.inGraph(), "every target of the shipped graph resolves: " + link.key());
            }
            // The entry's own label follows the same rule.
            final String own = SHIPPED.nameEnUs.get(key);
            assertEquals(own == null || own.isBlank() ? key : own, detail.label(), key + ": panel label");
        }
    }

    /** The state is exactly ResearchManager's answer for the same knowledge — no third store. */
    @Test
    void unlockStateIsTheExistingServerAuthoritativeSemanticsForEveryEntry() {
        for (final PlayerKnowledgeData knowledge : List.of(
                PlayerKnowledgeData.empty(),
                PlayerKnowledgeData.empty().withResearchCompleted("ASPECTS"),
                PlayerKnowledgeData.empty().withResearchCompleted("BASICTHAUMATURGY"))) {
            for (final String key : SHIPPED.entryKeys) {
                final ResearchDetailModel.Detail detail = MODEL.detail(key, knowledge);
                final ResearchDetailModel.State expected =
                        ResearchManager.isResearchComplete(knowledge, key)
                                ? ResearchDetailModel.State.COMPLETED
                                : ResearchManager.doesPlayerHaveRequisites(knowledge, key)
                                        ? ResearchDetailModel.State.AVAILABLE
                                        : ResearchDetailModel.State.LOCKED;
                assertEquals(expected, detail.state(),
                        key + ": the panel's unlock state must be the research authority's own answer for this"
                                + " knowledge (isResearchComplete / doesPlayerHaveRequisites, TC-M3-01/02)");
            }
        }
    }

    /** Completing the prerequisites through the knowledge store flips the entry from locked to available. */
    @Test
    void completingThePrerequisitesThroughTheKnowledgeStoreFlipsTheUnlockState() {
        final String key = SHIPPED.firstWith(ResearchDetailModel.KIND_PARENT);
        assertNotNull(key, "the shipped graph must have an entry with parents for this probe");
        final List<String> parents = SHIPPED.references.get(key).get(ResearchDetailModel.KIND_PARENT);
        final List<String> hidden = SHIPPED.references.get(key).get(ResearchDetailModel.KIND_PARENTS_HIDDEN);

        final PlayerKnowledgeData empty = PlayerKnowledgeData.empty();
        final ResearchDetailModel.Detail locked = MODEL.detail(key, empty);
        assertEquals(ResearchDetailModel.State.LOCKED, locked.state(),
                key + " has prerequisites and an empty knowledge store: " + ResearchDetailModel.STATE_LOCKED_TEXT);
        assertEquals(parents.size() + hidden.size(), locked.missingPrerequisites().size(),
                key + ": every prerequisite (hidden ones included) belongs on the missing list,"
                        + " the pair canUnlockResearch checks (GuiResearchBrowser.java:789-807)");
        assertTrue(locked.lines().contains(ResearchDetailModel.STATE_LOCKED_TEXT),
                "the locked panel shows the original's own card text");

        PlayerKnowledgeData armed = empty;
        for (final String parent : parents) {
            armed = armed.withResearchCompleted(parent);
        }
        for (final String parent : hidden) {
            armed = armed.withResearchCompleted(parent);
        }
        assertEquals(ResearchDetailModel.State.AVAILABLE, MODEL.detail(key, armed).state(),
                key + " must read available once its prerequisites are in the knowledge store");
        assertTrue(MODEL.detail(key, armed).missingPrerequisites().isEmpty(),
                "the missing list must be empty once the prerequisites are complete");

        // The panel links report the same flip: a completed parent is marked known.
        final ResearchDetailModel.Detail armedDetail = MODEL.detail(key, armed);
        for (final ResearchDetailModel.Link link : armedDetail.links(ResearchDetailModel.KIND_PARENT)) {
            assertTrue(link.completed(), link.key() + " is in the store, the link must say so");
        }

        // Completing the entry itself is the store's act, and the panel follows it.
        final PlayerKnowledgeData done = armed.withResearchCompleted(key);
        assertEquals(ResearchDetailModel.State.COMPLETED, MODEL.detail(key, done).state(),
                key + ": a completed entry reads known, not available");
        assertTrue(MODEL.detail(key, done).lines().contains(ResearchDetailModel.STATE_KNOWN_TEXT),
                "the panel's first line names the state");
    }

    /** Aspect requirements are the entry's tags plus the player's discovered pool, per the graph. */
    @Test
    void aspectRequirementsAreTheGraphTagsAndThePlayersAspectPool() {
        int withTags = 0;
        for (final String key : SHIPPED.entryKeys) {
            final List<JsonElement> tags = SHIPPED.tags.get(key);
            final ResearchDetailModel.Detail detail =
                    MODEL.detail(key, PlayerKnowledgeData.empty());
            assertEquals(tags.size(), detail.aspects().size(),
                    key + ": the panel's aspect requirements are exactly the entry's tags in entries.json"
                            + " (GuiResearchBrowser.java:611-640 draws the same list)");
            for (int i = 0; i < tags.size(); i++) {
                final JsonObject tag = tags.get(i).getAsJsonObject();
                final ResearchDetailModel.AspectRequirement requirement = detail.aspects().get(i);
                assertEquals(tag.get("aspect").getAsString(), requirement.aspect(), key + ": tag aspect");
                assertEquals(tag.get("amount").getAsInt(), requirement.amount(), key + ": tag amount");
                assertFalse(requirement.discovered(),
                        key + ": an empty store has discovered no aspect, and the original draws the"
                                + " _unknown.png stand-in for that case (:641-647)");
                assertFalse(requirement.affordable(), key + ": nothing is affordable from an empty store");
            }
            if (tags.isEmpty()) {
                continue;
            }
            withTags++;

            // The pool decides affordability, through the knowledge record's own rules.
            PlayerKnowledgeData full = PlayerKnowledgeData.empty();
            for (final JsonElement tag : tags) {
                full = full.withAspectDiscovered(tag.getAsJsonObject().get("aspect").getAsString(),
                        tag.getAsJsonObject().get("amount").getAsInt());
            }
            final ResearchDetailModel.Detail paid = MODEL.detail(key, full);
            assertTrue(paid.aspectsAffordable(),
                    key + ": a pool that covers every tag makes the entry affordable (tc.research.purchase, :653)");
            for (final ResearchDetailModel.AspectRequirement requirement : paid.aspects()) {
                assertTrue(requirement.discovered(), requirement.aspect() + " is in the store");
            }

            // One short of the amount is the pulsing "short" case (:625-630, tc.research.short at :655).
            final JsonElement first = tags.get(0);
            final int amount = first.getAsJsonObject().get("amount").getAsInt();
            if (amount > 0) {
                PlayerKnowledgeData short_ = PlayerKnowledgeData.empty()
                        .withAspectDiscovered(first.getAsJsonObject().get("aspect").getAsString(), amount - 1);
                for (int i = 1; i < tags.size(); i++) {
                    short_ = short_.withAspectDiscovered(tags.get(i).getAsJsonObject().get("aspect").getAsString(),
                            tags.get(i).getAsJsonObject().get("amount").getAsInt());
                }
                final ResearchDetailModel.Detail lacking = MODEL.detail(key, short_);
                assertFalse(lacking.aspectsAffordable(), key + ": a pool below the cost is not affordable");
                assertTrue(lacking.lines().contains(ResearchDetailModel.ASPECTS_SHORT_TEXT),
                        key + ": the panel shows the original's own short line, tc.research.short");
            }
        }
        assertEquals(SHIPPED.countWithTags(), withTags,
                "the sweep must have covered every entry that carries tags, and the data has "
                        + SHIPPED.countWithTags() + " of them");
    }

    /** An entry the graph does not have resolves to nothing; a target it does not have renders its key. */
    @Test
    void aStrangerKeyResolvesNothingAndADanglingTargetRendersItsKey() {
        assertNull(MODEL.detail("t341117b8_not_in_the_graph", PlayerKnowledgeData.empty()),
                "a key the graph does not carry resolves no detail, so the panel can render nothing for it");
        assertNull(MODEL.detail(null, PlayerKnowledgeData.empty()), "no key, no detail");

        // A dangling target is not fatal: the link renders, labelled with the key,
        // and is marked not openable. Built in code because the shipped graph is
        // clean (ResearchGraphValidator reports no unknown references for it).
        final ResearchGraph synthetic = new ResearchGraph(
                List.of(new ResearchCategoryData("BASICS", 0, null, null,
                        "tc.research_category.BASICS", "Basics")),
                List.of(
                        new ResearchEntryData("GATE", "BASICS", 0, 0, 1, null, List.of(), null,
                                new ResearchEntryData.Flags(false, false, false, false, false, false,
                                        false, false, false),
                                List.of("MISSING_TARGET"), List.of("ALSO_MISSING"), List.of("NAMED_PEER"),
                                new ResearchEntryData.Triggers(List.of(), List.of(), List.of()), 0,
                                "tc.research_name.GATE", "The Gate", "tc.research_text.GATE", "hover", List.of()),
                        new ResearchEntryData("NAMED_PEER", "BASICS", 1, 0, 1, null, List.of(), null,
                                new ResearchEntryData.Flags(false, false, false, false, false, false,
                                        false, false, false),
                                List.of(), List.of(), List.of(),
                                new ResearchEntryData.Triggers(List.of(), List.of(), List.of()), 0,
                                "tc.research_name.NAMED_PEER", null, "tc.research_text.NAMED_PEER", null,
                                List.of())));

        final ResearchDetailModel.Detail detail =
                new ResearchDetailModel(synthetic).detail("GATE", PlayerKnowledgeData.empty());
        assertNotNull(detail, "the synthetic entry resolves");
        final List<ResearchDetailModel.Link> links = detail.links();
        assertEquals(3, links.size(), "every reference of the entry is offered, resolvable or not");
        final ResearchDetailModel.Link dangling = links.get(0);
        assertEquals("MISSING_TARGET", dangling.key(), "the first parent is the dangling one");
        assertFalse(dangling.inGraph(), "the graph does not contain the target");
        assertEquals("MISSING_TARGET", dangling.label(),
                "a target the graph does not have is labelled with its key — the panel renders the key, it does not fail");
        assertFalse(detail.openableLinks().contains(dangling),
                "a dangling link is not openable, so a click on it can reach nothing");
        // The unnamed peer renders its key too (CAP_iron / ROD_wood are the shipped cases).
        final ResearchDetailModel.Link peer = links.get(2);
        assertEquals("NAMED_PEER", peer.label(), "an entry the graph gives no name to is labelled with its key");
        assertTrue(detail.lines().stream().anyMatch(line -> line.startsWith("Requires: MISSING_TARGET")),
                "the dangling parent renders as its own line, labelled with the key: " + detail.lines());
    }

    /** The lines the panel draws: state, warp, essentia — the original's own words. */
    @Test
    void thePanelLinesCarryTheOriginalStringsForStateWarpAndEssentia() {
        // Warp: the original caps the displayed level at 5 (GuiResearchBrowser.java:637-639).
        final String tabooKey = SHIPPED.firstWithWarpAtLeast(6);
        final String moderateKey = SHIPPED.firstWithWarp(3);
        final String essentiaKey = SHIPPED.firstWithTags();
        assertNotNull(tabooKey, "the shipped graph has an entry with warp above 5 (the original caps the label at 5)");
        assertNotNull(moderateKey, "the shipped graph has a warp-3 entry");
        assertNotNull(essentiaKey, "the shipped graph has an entry with essentia tags");

        final ResearchDetailModel.Detail taboo = MODEL.detail(tabooKey, PlayerKnowledgeData.empty());
        assertTrue(taboo.lines().stream().anyMatch(line -> line.equals("Forbidden knowledge (Taboo)")),
                tabooKey + " has warp " + taboo.warp() + ", and the card caps the word at Taboo (en_US.lang:722,727): "
                        + taboo.lines());
        final ResearchDetailModel.Detail moderate = MODEL.detail(moderateKey, PlayerKnowledgeData.empty());
        assertTrue(moderate.lines().stream().anyMatch(line -> line.equals("Forbidden knowledge (Moderate)")),
                moderateKey + ": " + moderate.lines());
        final ResearchDetailModel.Detail noWarp = MODEL.detail(essentiaKey, PlayerKnowledgeData.empty());
        assertTrue(noWarp.warp() == 0
                        || noWarp.lines().stream().noneMatch(l -> l.startsWith("Forbidden knowledge")),
                "the warp line only shows when the entry carries warp (getWarp > 0, :645)");

        final List<String> lines = MODEL.detail(essentiaKey, PlayerKnowledgeData.empty()).lines();
        assertTrue(lines.stream().anyMatch(line -> line.startsWith(ResearchDetailModel.ASPECTS_LABEL)),
                essentiaKey + ": the essentia line names the graph's tags: " + lines);
        final String essentiaLine = lines.stream()
                .filter(line -> line.startsWith(ResearchDetailModel.ASPECTS_LABEL)).findFirst().orElseThrow();
        for (final ResearchDetailModel.AspectRequirement requirement
                : MODEL.detail(essentiaKey, PlayerKnowledgeData.empty()).aspects()) {
            assertTrue(essentiaLine.contains(requirement.token()),
                    "the essentia line carries the tag and its amount: " + essentiaLine);
        }
        assertTrue(lines.get(0).equals(ResearchDetailModel.STATE_LOCKED_TEXT)
                        || lines.get(0).startsWith("Available to research")
                        || lines.get(0).equals(ResearchDetailModel.STATE_KNOWN_TEXT),
                "the panel's first line is always the state: " + lines);

        // The link groups name themselves, and hidden parents are named apart
        // (the original keeps them off the drawn lines, :288-305).
        final String hiddenKey = SHIPPED.firstWith(ResearchDetailModel.KIND_PARENTS_HIDDEN);
        assertNotNull(hiddenKey, "the shipped graph has an entry with parentsHidden");
        final List<String> hiddenLines = MODEL.detail(hiddenKey, PlayerKnowledgeData.empty()).lines();
        assertTrue(hiddenLines.stream().anyMatch(line -> line.startsWith("Hidden requirement:")),
                hiddenKey + ": hidden prerequisites are labelled as the original treats them: " + hiddenLines);
        final String siblingKey = SHIPPED.firstWith(ResearchDetailModel.KIND_SIBLINGS);
        assertNotNull(siblingKey, "the shipped graph has an entry with siblings");
        assertTrue(MODEL.detail(siblingKey, PlayerKnowledgeData.empty()).lines().stream()
                        .anyMatch(line -> line.startsWith("Related:")),
                siblingKey + ": siblings render as the related links they are (:307-333)");
        assertEquals(ResearchDetailModel.Link.groupLabel(ResearchDetailModel.KIND_PARENT), "Requires:");
        assertEquals(ResearchDetailModel.Link.groupLabel(ResearchDetailModel.KIND_SIBLINGS), "Related:");
        assertEquals(ResearchDetailModel.Link.groupLabel(ResearchDetailModel.KIND_PARENTS_HIDDEN),
                "Hidden requirement:");
    }

    // ------------------------------------------------------------------ plumbing

    /** The shipped graph, read straight out of entries.json with a raw parser. */
    private record ShippedGraph(List<String> entryKeys,
                                Map<String, String> category,
                                Map<String, String> nameEnUs,
                                Map<String, Map<String, List<String>>> references,
                                Map<String, List<JsonElement>> tags,
                                Map<String, Integer> warp) {

        static ShippedGraph read() {
            final List<String> entryKeys = new ArrayList<>();
            final Map<String, String> category = new LinkedHashMap<>();
            final Map<String, String> nameEnUs = new LinkedHashMap<>();
            final Map<String, Map<String, List<String>>> references = new LinkedHashMap<>();
            final Map<String, List<JsonElement>> tags = new LinkedHashMap<>();
            final Map<String, Integer> warp = new LinkedHashMap<>();
            try (Reader reader = new InputStreamReader(
                    ResearchDetailModelTest.class.getResourceAsStream(
                            "/data/thaumcraft/research_graph/entries.json"),
                    StandardCharsets.UTF_8)) {
                final JsonArray entries = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonArray("entries");
                for (final JsonElement element : entries) {
                    final JsonObject entry = element.getAsJsonObject();
                    final String key = entry.get("key").getAsString();
                    entryKeys.add(key);
                    category.put(key, entry.get("category").getAsString());
                    nameEnUs.put(key, entry.has("nameEnUs") && !entry.get("nameEnUs").isJsonNull()
                            ? entry.get("nameEnUs").getAsString() : null);
                    final Map<String, List<String>> byKind = new LinkedHashMap<>();
                    for (final String kind : ResearchDetailModel.linkKinds()) {
                        final List<String> list = new ArrayList<>();
                        for (final JsonElement ref : entry.getAsJsonArray(kind)) {
                            list.add(ref.getAsString());
                        }
                        byKind.put(kind, List.copyOf(list));
                    }
                    references.put(key, Map.copyOf(byKind));
                    final List<JsonElement> tagList = new ArrayList<>();
                    if (entry.has("tags")) {
                        for (final JsonElement tag : entry.getAsJsonArray("tags")) {
                            tagList.add(tag);
                        }
                    }
                    tags.put(key, List.copyOf(tagList));
                    warp.put(key, entry.has("warp") ? entry.get("warp").getAsInt() : 0);
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            // The graph's two unnamed entries (CAP_iron, ROD_wood) carry no nameEnUs,
            // and Map.copyOf rejects a null value; the maps must tolerate it.
            return new ShippedGraph(List.copyOf(entryKeys),
                    Collections.unmodifiableMap(new java.util.HashMap<>(category)),
                    Collections.unmodifiableMap(new java.util.HashMap<>(nameEnUs)),
                    Collections.unmodifiableMap(new java.util.HashMap<>(references)),
                    Collections.unmodifiableMap(new java.util.HashMap<>(tags)),
                    Collections.unmodifiableMap(new java.util.HashMap<>(warp)));
        }

        int countWith(final String kind) {
            int seen = 0;
            for (final String key : entryKeys) {
                if (!references.get(key).get(kind).isEmpty()) {
                    seen++;
                }
            }
            return seen;
        }

        int countWithTags() {
            int seen = 0;
            for (final String key : entryKeys) {
                if (!tags.get(key).isEmpty()) {
                    seen++;
                }
            }
            return seen;
        }

        String firstWith(final String kind) {
            for (final String key : entryKeys) {
                if (!references.get(key).get(kind).isEmpty()) {
                    return key;
                }
            }
            return null;
        }

        String firstWithTags() {
            for (final String key : entryKeys) {
                if (!tags.get(key).isEmpty()) {
                    return key;
                }
            }
            return null;
        }

        String firstWithWarp(final int value) {
            for (final String key : entryKeys) {
                if (warp.get(key) == value) {
                    return key;
                }
            }
            return null;
        }

        String firstWithWarpAtLeast(final int value) {
            for (final String key : entryKeys) {
                if (warp.get(key) >= value) {
                    return key;
                }
            }
            return null;
        }
    }
}
