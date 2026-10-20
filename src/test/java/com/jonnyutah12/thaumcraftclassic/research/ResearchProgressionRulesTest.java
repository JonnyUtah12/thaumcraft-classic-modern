package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.player.PlayerKnowledgeData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M3-02 acceptance: prerequisite evaluation and reveal-rule tests against the
 * original values (the graph is the mechanical extraction of ConfigResearch.java;
 * the rules mirror ResearchManager 4.2.3.5 exactly, quirks included).
 */
class ResearchProgressionRulesTest {

    /** The original starting-research set: every autoUnlock entry in ConfigResearch.java. */
    private static final List<String> ORIGINAL_STARTING_RESEARCH = List.of(
            "BASICTHAUMATURGY", "CAP_iron", "ROD_wood", "BASICARTIFACE", "ARCANESTONE", "GRATE",
            "TABLE", "ARCTABLE", "RESTABLE", "THAUMOMETER", "PHIAL", "CRUCIBLE", "ASPECTS", "PECH",
            "NODES", "WARP", "RESEARCH", "KNOWFRAG", "THAUMONOMICON", "ORE", "PLANTS", "ENCHANT");

    // ------------------------------------------------------------- starting set

    @Test
    void autoUnlockSetMatchesOriginalStartingResearch() {
        final List<String> auto = ResearchManager.graph().entries().stream()
                .filter(e -> e.flags() != null && e.flags().autoUnlock())
                .map(ResearchEntryData::key)
                .toList();
        assertEquals(ORIGINAL_STARTING_RESEARCH, auto);
    }

    // ------------------------------------------------------------ prerequisites

    @Test
    void prerequisitesGateOnCompletedParents() {
        final PlayerKnowledgeData empty = PlayerKnowledgeData.empty();
        assertFalse(ResearchManager.doesPlayerHaveRequisites(empty, "FOCUSFIRE"));

        final PlayerKnowledgeData withParent = empty.withResearchCompleted("BASICTHAUMATURGY");
        assertTrue(ResearchManager.doesPlayerHaveRequisites(withParent, "FOCUSFIRE"));

        // Unrelated completed research does not satisfy the parent.
        final PlayerKnowledgeData unrelated = empty.withResearchCompleted("ASPECTS");
        assertFalse(ResearchManager.doesPlayerHaveRequisites(unrelated, "FOCUSFIRE"));
    }

    @Test
    void hiddenParentsAreRequiredToo() {
        // FOCUSHELLBAT has parentsHidden = [FOCUSFIRE, INFUSION] and no visible parents.
        final PlayerKnowledgeData half = PlayerKnowledgeData.empty()
                .withResearchCompleted("FOCUSFIRE");
        assertFalse(ResearchManager.doesPlayerHaveRequisites(half, "FOCUSHELLBAT"));

        final PlayerKnowledgeData all = half.withResearchCompleted("INFUSION");
        assertTrue(ResearchManager.doesPlayerHaveRequisites(all, "FOCUSHELLBAT"));
    }

    @Test
    void emptyCompletedListNeverSatisfiesARequisitionList() {
        // The original's quirk: with a requisition list present and an empty
        // completed list the result is false — reproduced verbatim.
        assertFalse(ResearchManager.doesPlayerHaveRequisites(PlayerKnowledgeData.empty(), "FOCUSFIRE"));
        assertFalse(ResearchManager.doesPlayerHaveRequisites(PlayerKnowledgeData.empty(), "FOCUSHELLBAT"));
        // No requisition list at all → always satisfied.
        assertTrue(ResearchManager.doesPlayerHaveRequisites(PlayerKnowledgeData.empty(), "BASICTHAUMATURGY"));
    }

    @Test
    void unknownKeysAreNotCompleteAndHaveNoRequisites() {
        assertFalse(ResearchManager.isResearchComplete(PlayerKnowledgeData.empty(), "NOT_A_KEY"));
        assertFalse(ResearchManager.doesPlayerHaveRequisites(PlayerKnowledgeData.empty(), "NOT_A_KEY"));
    }

    @Test
    void atPrefixedNoteKeysBypassTheGraphCheck() {
        final PlayerKnowledgeData noteUnlocked = PlayerKnowledgeData.empty().withResearchCompleted("@BONEBOW");
        assertTrue(ResearchManager.isResearchComplete(noteUnlocked, "@BONEBOW"));
        // The plain key is still not complete — the note unlock is its own key.
        assertFalse(ResearchManager.isResearchComplete(noteUnlocked, "BONEBOW"));
        // An @-key is checked verbatim: present in the set → complete even though
        // it is not a graph entry; absent → not complete.
        final PlayerKnowledgeData odd = PlayerKnowledgeData.empty().withResearchCompleted("@NOT_A_KEY");
        assertTrue(ResearchManager.isResearchComplete(odd, "@NOT_A_KEY"));
        assertFalse(ResearchManager.isResearchComplete(noteUnlocked, "@NOT_A_KEY"));
        // A plain unknown key is never complete.
        assertFalse(ResearchManager.isResearchComplete(odd, "NOT_A_KEY"));
    }

    @Test
    void completionIsIdempotent() {
        final PlayerKnowledgeData k = PlayerKnowledgeData.empty().withResearchCompleted("FOCUSFIRE");
        assertEquals(k, ResearchManager.completeResearchUnsaved(k, "FOCUSFIRE"));
        assertTrue(ResearchManager.completeResearchUnsaved(k, "FOCUSFIRE") == k);
    }

    // -------------------------------------------------------------- trigger ids

    @Test
    void triggerIdsNormalizeToTheFrozenIdContract() {
        assertEquals("thaumcraft:focusfire", ResearchManager.normalizedItemId("Thaumcraft:FocusFire"));
        assertEquals("minecraft:bone", ResearchManager.normalizedItemId("minecraft:bone"));
        assertEquals("thaumcraft:brainyzombie", ResearchManager.normalizedEntityName("Thaumcraft.BrainyZombie"));
        assertEquals("minecraft:enderman", ResearchManager.normalizedEntityName("Enderman"));
    }

    @Test
    void itemTriggersMatchNormalizedIdsIgnoringMeta() {
        final PlayerKnowledgeData fresh = PlayerKnowledgeData.empty();
        // BONEBOW: hidden, tagged, item triggers minecraft:bow (wildcard meta) + minecraft:bone.
        assertEquals(List.of("BONEBOW"),
                ResearchManager.hiddenClueKeys(fresh, "minecraft:bow", null, List.of()));
        assertEquals(List.of("BONEBOW"),
                ResearchManager.hiddenClueKeys(fresh, "minecraft:bone", null, List.of()));
        // The original compares with ignoreDamage=true: metas never participate.
        // An unrelated item matches nothing hidden.
        assertTrue(ResearchManager.hiddenClueKeys(fresh, "minecraft:stone", null, List.of()).isEmpty());
    }

    @Test
    void entityAndAspectTriggersMatch() {
        final PlayerKnowledgeData fresh = PlayerKnowledgeData.empty();
        // JARBRAIN: entity triggers Thaumcraft.BrainyZombie / Thaumcraft.GiantBrainyZombie.
        assertTrue(ResearchManager.hiddenClueKeys(fresh, null, "thaumcraft:brainyzombie", List.of())
                .contains("JARBRAIN"));
        // MIRROR: entity trigger "Enderman" → minecraft:enderman.
        assertEquals(List.of("MIRROR"),
                ResearchManager.hiddenClueKeys(fresh, null, "minecraft:enderman", List.of()));
        // FOCUSHELLBAT: aspect trigger ignis.
        assertTrue(ResearchManager.hiddenClueKeys(fresh, null, null, List.of("ignis"))
                .contains("FOCUSHELLBAT"));
    }

    @Test
    void revealedEntriesDropOutOfFutureClues() {
        final PlayerKnowledgeData fresh = PlayerKnowledgeData.empty();
        assertEquals(List.of("BONEBOW"), ResearchManager.hiddenClueKeys(fresh, "minecraft:bow", null, List.of()));
        final PlayerKnowledgeData revealed = fresh.withResearchCompleted("@BONEBOW");
        assertTrue(ResearchManager.hiddenClueKeys(revealed, "minecraft:bow", null, List.of()).isEmpty());
    }

    // ------------------------------------------------------------- reveal rules

    @Test
    void hiddenResearchRevealOnlyPicksValidEntriesAndIsDeterministicPerDay() {
        final PlayerKnowledgeData fresh = PlayerKnowledgeData.empty();
        final String key = ResearchManager.findHiddenResearch(fresh, 0L);
        assertFalse("FAIL".equals(key), "fresh player must have revealable hidden research");

        final ResearchEntryData entry = ResearchManager.graph().entry(key);
        assertNotNull(entry);
        assertTrue(entry.flags().hidden(), "reveal must pick hidden entries");
        assertFalse(entry.tags() == null || entry.tags().isEmpty(), "reveal requires tags");
        final ResearchEntryData.Triggers t = entry.triggers();
        assertTrue(t != null && (!t.items().isEmpty() || !t.entities().isEmpty() || !t.aspects().isEmpty()),
                "reveal requires triggers");

        // The original seeds the pick from dayTime/10/5 — same day time, same pick.
        assertEquals(key, ResearchManager.findHiddenResearch(fresh, 47L));
    }

    @Test
    void aspectMatchRespectsFlagsRequisitesAndDifficulty() {
        final PlayerKnowledgeData joined = PlayerKnowledgeData.empty()
                .withResearchCompleted("BASICTHAUMATURGY");

        final String match = ResearchManager.findMatchingResearch(joined, "ignis", new Random(1L), 0);
        assertNotNull(match, "a joined player has ignis-tagged research available");
        final ResearchEntryData entry = ResearchManager.graph().entry(match);
        assertNotNull(entry);
        final ResearchEntryData.Flags f = entry.flags();
        assertFalse(f.hidden() || f.lost() || f.autoUnlock() || f.virtual() || f.stub(),
                "aspect match must skip hidden/lost/autoUnlock/virtual/stub entries");
        assertFalse(f.secondary(), "normal difficulty must skip secondary entries");
        assertTrue(entry.tags().stream().anyMatch(tag -> tag.aspect().equals("ignis") && tag.amount() > 0),
                "aspect match must carry the aspect");
        assertTrue(ResearchManager.doesPlayerHaveRequisites(joined, match), "aspect match requires requisites");

        // Easy mode (-1): the original's precedence makes every entry secondary → nothing matches.
        assertNull(ResearchManager.findMatchingResearch(joined, "ignis", new Random(1L), -1));
        // An aspect nobody is eligible for: null.
        assertNull(ResearchManager.findMatchingResearch(PlayerKnowledgeData.empty(), "nonexistent_aspect",
                new Random(1L), 0));
    }

    // -------------------------------------------------------------------- warp

    @Test
    void warpSplitMatchesCompleteResearch() {
        // Original: warp>1 → [warp - warp/2 perm, warp/2 sticky]; warp==1 → all perm.
        assertEquals(List.of(2, 1), List.of(ResearchManager.warpSplit(3)[0], ResearchManager.warpSplit(3)[1]));
        assertEquals(List.of(1, 0), List.of(ResearchManager.warpSplit(1)[0], ResearchManager.warpSplit(1)[1]));
        assertEquals(List.of(1, 1), List.of(ResearchManager.warpSplit(2)[0], ResearchManager.warpSplit(2)[1]));
        assertEquals(List.of(3, 2), List.of(ResearchManager.warpSplit(5)[0], ResearchManager.warpSplit(5)[1]));
        assertEquals(List.of(0, 0), List.of(ResearchManager.warpSplit(0)[0], ResearchManager.warpSplit(0)[1]));
    }

    // ------------------------------------------------------- crafting warp table

    @Test
    void craftingWarpTableMatchesTheAddWarpToItemSet() {
        final Set<String> keys = ResearchManager.itemWarpTable().stream()
                .map(e -> e.itemId() + ":" + e.meta())
                .collect(java.util.stream.Collectors.toSet());
        // Every ThaumcraftApi.addWarpToItem call in the decompiled ConfigResearch.java.
        assertEquals(Set.of(
                "thaumcraft:focushellbat:0",   // ConfigResearch.java:129
                "thaumcraft:itemcompassstone:0", // ConfigResearch.java:865
                "thaumcraft:blockjar:1",        // ConfigResearch.java:1414
                "thaumcraft:itembucketdeath:0", // ConfigResearch.java:1647
                "thaumcraft:itembottletaint:0", // ConfigResearch.java:1663
                "thaumcraft:itemgolemplacer:4", // ConfigResearch.java:2178
                "thaumcraft:focusprimal:0",     // ConfigResearch.java:3076
                "thaumcraft:wandrod:100"),      // ConfigResearch.java:3111
                keys);
    }
}
