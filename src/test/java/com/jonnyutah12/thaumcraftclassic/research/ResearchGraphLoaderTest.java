package com.jonnyutah12.thaumcraftclassic.research;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped research graph loads from mod resources through
 * {@link ResearchGraphLoader} and the registry answers lookups the way the
 * original ResearchCategories/ResearchManager shape does; spot checks pin
 * the extraction semantics (clamped complexity, hidden parents, triggers,
 * warp, computed icon metadata).
 */
class ResearchGraphLoaderTest {

    private static ResearchGraph graph() {
        return ResearchGraphLoader.loadDefault();
    }

    @Test
    void shippedGraphLoadsWithOriginalCounts() {
        ResearchGraph graph = graph();
        assertEquals(6, graph.categoryCount());
        assertEquals(201, graph.entryCount());
        assertEquals(List.of(), graph.problems());
        assertEquals(6, graph.categories().size());
    }

    @Test
    void lookupsResolveByCategoryAndKey() {
        ResearchGraph graph = graph();
        assertNotNull(graph.category("ELDRITCH"));
        assertNull(graph.category("NOPE"));
        assertNotNull(graph.entry("THAUMONOMICON"));
        assertNull(graph.entry("NOPE"));
        // category membership is derived from the owning category field
        long basics = graph.entriesIn("BASICS").size();
        long all = graph.entries().stream().filter(e -> e.category().equals("BASICS")).count();
        assertEquals(all, basics);
        assertTrue(basics > 10);
        assertEquals(201, graph.categories().stream()
                .mapToInt(c -> graph.entriesIn(c.key()).size()).sum());
    }

    @Test
    void discoveryTriggeredEntryKeepsHiddenParentsTriggersAndWarp() {
        ResearchGraph graph = graph();
        ResearchEntryData hellbat = graph.entry("FOCUSHELLBAT");
        assertTrue(hellbat.flags().hidden());
        assertEquals(List.of("FOCUSFIRE", "INFUSION"), hellbat.parentsHidden());
        assertEquals(List.of(), hellbat.parents());
        assertEquals(List.of("Thaumcraft.Firebat"), hellbat.triggers().entities());
        assertEquals(List.of("ignis"), hellbat.triggers().aspects());
        assertEquals(2, hellbat.warp());
        assertEquals("Thaumcraft:FocusHellbat", hellbat.icon().item());
        assertEquals(2, hellbat.complexity());
    }

    @Test
    void stubComplexityKeepsTheOriginalClampSemantics() {
        ResearchGraph graph = graph();
        ResearchEntryData jarLabel = graph.entry("JARLABEL");
        // The original constructor passed 0 and the constructor clamped it to 1.
        assertEquals(0, jarLabel.complexityRaw());
        assertEquals(1, jarLabel.complexity());
        assertTrue(jarLabel.flags().stub());
        assertTrue(jarLabel.flags().round());
        // the 48 jar-label variants are one array page in original order
        List<String> variants = jarLabel.pages().stream()
                .filter(p -> p.recipes() != null).flatMap(p -> p.recipes().stream()).toList();
        assertEquals(48, variants.size());
        assertEquals("JarLabel0", variants.get(0));
        assertEquals("JarLabel47", variants.get(47));
    }

    @Test
    void virtualStubsStayVirtualAndUnnamed() {
        ResearchGraph graph = graph();
        for (String key : List.of("CAP_iron", "ROD_wood")) {
            ResearchEntryData stub = graph.entry(key);
            assertTrue(stub.flags().virtual());
            assertTrue(stub.flags().autoUnlock());
            assertNull(stub.nameEnUs(), "UNKNOWN display must not be invented: " + key);
            assertNull(stub.textEnUs());
            assertNull(stub.complexity());
            assertEquals("none", stub.icon().kind());
            assertEquals(List.of(), stub.pages());
        }
    }

    @Test
    void computedIconMetadataMatchesTheOriginal() {
        ResearchGraph graph = graph();
        // SCEPTRE's icon is a wand ItemStack whose metadata is the computed
        // craft cost (int)(thaumium cap 6 * silverwood rod 9 * 1.5) = 81.
        ResearchEntryData sceptre = graph.entry("SCEPTRE");
        assertEquals("item", sceptre.icon().kind());
        assertEquals("Thaumcraft:WandCasting", sceptre.icon().item());
        assertEquals(81, sceptre.icon().meta());
        // wildcard metadata 32767 = "any" in the original triggers
        ResearchEntryData banners = graph.entry("BANNERS");
        assertEquals(8, banners.icon().meta());
        assertEquals(8, banners.triggers().items().get(0).meta());
        ResearchEntryData mirror = graph.entry("MIRROR");
        assertEquals("minecraft:ender_pearl", mirror.triggers().items().get(0).item());
        assertEquals(32767, mirror.triggers().items().get(1).meta());
    }

    @Test
    void pageTypesRoundTrip() {
        ResearchGraph graph = graph();
        ResearchEntryData crucible = graph.entry("CRUCIBLE");
        assertEquals(ResearchPageData.Type.TEXT, crucible.pages().get(0).type());
        assertTrue(crucible.pages().get(0).textEnUs().length() > 0);
        // the balanced-shard array page carries the six generated recipe keys
        List<String> shards = crucible.pages().stream()
                .filter(p -> p.recipes() != null).flatMap(p -> p.recipes().stream()).toList();
        assertEquals(List.of("BalancedShard_0", "BalancedShard_1", "BalancedShard_2",
                "BalancedShard_3", "BalancedShard_4", "BalancedShard_5"), shards);
        // the thaumonomicon compound page references the recipes-map list key
        ResearchEntryData book = graph.entry("THAUMONOMICON");
        assertTrue(book.pages().stream().anyMatch(p ->
                p.type() == ResearchPageData.Type.COMPOUND_CRAFTING && "Thaumonomicon".equals(p.recipeGroup())));
        // the single smelting page (balanced shard furnace display) lives on CRUCIBLE
        assertTrue(crucible.pages().stream().anyMatch(p ->
                p.type() == ResearchPageData.Type.SMELTING
                        && "Thaumcraft:ItemShard".equals(p.input().item()) && p.input().meta() == 6));
    }
}
