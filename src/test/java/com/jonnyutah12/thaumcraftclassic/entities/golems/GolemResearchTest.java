package com.jonnyutah12.thaumcraftclassic.entities.golems;

import com.jonnyutah12.thaumcraftclassic.research.ResearchEntryData;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraph;
import com.jonnyutah12.thaumcraftclassic.research.ResearchGraphLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The GOLEMANCY gate: each of the eight materials maps to the graph entry the
 * shipped data already carries, and those entries really are in that category.
 *
 * <p>This is the "wire the gate to those entries, author nothing" half of the
 * card. It loads the shipped graph exactly as the mod does
 * ({@link ResearchGraphLoader#loadDefault()}) and pins both directions: the
 * code's key for a material exists, and every material's entry sits under
 * {@code GOLEMANCY} with the meta its icon names. The in-game half (the
 * crucible recipe rows that enforce the gate are live and refuse an unread
 * thrower) is {@code GolemContentGameTest}.</p>
 */
class GolemResearchTest {

    /** The eight original entry keys — the graph ships exactly these, and the port renames none. */
    private static final Map<EnumGolemType, String> KEYS = Map.of(
            EnumGolemType.STRAW, "GOLEMSTRAW",
            EnumGolemType.WOOD, "GOLEMWOOD",
            EnumGolemType.TALLOW, "GOLEMTALLOW",
            EnumGolemType.CLAY, "GOLEMCLAY",
            EnumGolemType.FLESH, "GOLEMFLESH",
            EnumGolemType.STONE, "GOLEMSTONE",
            EnumGolemType.IRON, "GOLEMIRON",
            EnumGolemType.THAUMIUM, "GOLEMTHAUMIUM");

    @Test
    void theKeyOfEveryMaterialIsGolemPlusTheName() {
        assertEquals(8, EnumGolemType.values().length);
        for (final Map.Entry<EnumGolemType, String> entry : KEYS.entrySet()) {
            assertEquals(entry.getValue(), GolemResearch.researchKey(entry.getKey()),
                    "STRAW -> GOLEMSTRAW, ... , THAUMIUM -> GOLEMTHAUMIUM (ConfigResearch.initializeResearch)");
            assertEquals("GOLEMANCY", GolemResearch.CATEGORY);
        }
        assertTrue(!GolemResearch.isNoteKey(EnumGolemType.STRAW),
                "none of the eight material keys is a @-prefixed note key");
    }

    /** Every material's entry is in the shipped graph, under GOLEMANCY. */
    @Test
    void everyMaterialEntryShipsInTheGolemancyCategory() {
        final ResearchGraph graph = ResearchGraphLoader.loadDefault();
        assertEquals(List.of(), graph.problems(), "the shipped graph must stay structurally clean");
        assertEquals(6, graph.categories().size(), "the graph ships 6 categories");
        assertEquals(201, graph.entries().size(), "the graph ships 201 entries");
        assertNotNull(graph.category("GOLEMANCY"), "the GOLEMANCY category the gate lives in is shipped");

        for (final Map.Entry<EnumGolemType, String> entry : KEYS.entrySet()) {
            final ResearchEntryData data = graph.entry(entry.getValue());
            assertNotNull(data, "the shipped graph carries " + entry.getValue());
            assertEquals("GOLEMANCY", data.category(), entry.getValue() + " is a GOLEMANCY entry");
            assertNotNull(data.icon(), entry.getValue() + " has an icon");
            assertEquals("item", data.icon().kind(), entry.getValue() + " icons an item");
            assertEquals(entry.getKey().ordinal(), data.icon().meta(),
                    "the placer icon of " + entry.getValue() + " uses the material's metadata");
        }
    }

    /** The GOLEMANCY category really is the golem home: it carries the eight materials plus their siblings. */
    @Test
    void theGolemancyCategoryCarriesTheGolemFamily() {
        final ResearchGraph graph = ResearchGraphLoader.loadDefault();
        final List<String> keys = graph.entries().stream()
                .filter(e -> "GOLEMANCY".equals(e.category()))
                .map(ResearchEntryData::key)
                .collect(Collectors.toList());
        assertTrue(keys.containsAll(KEYS.values()),
                "GOLEMANCY carries all eight material entries, found " + keys);
        assertEquals(39, keys.size(),
                "GOLEMANCY carries 39 entries in the shipped graph (8 materials + cores, upgrades, "
                        + "bell, decorations, advanced golems, fetter, hungry chest, trunk)");
    }
}
