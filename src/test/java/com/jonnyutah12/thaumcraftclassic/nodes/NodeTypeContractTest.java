package com.jonnyutah12.thaumcraftclassic.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import com.jonnyutah12.thaumcraftclassic.registration.ContentMappingCsv;

/**
 * The node type / modifier enums must match the frozen id rows in
 * docs/content-mapping.csv (TC-M5-01): {@code thaumcraft:nodetype.*} ×6 and
 * {@code thaumcraft:nodemodifier.*} ×3, no more, no less. The in-game half
 * (the ids resolve to live registry objects) is asserted by
 * RegistrationGameTest.
 */
class NodeTypeContractTest {

    private static List<ContentMappingCsv.Row> rows(final String kind) throws IOException {
        final Path file = Path.of(System.getProperty("tc.contentMappingFile"));
        return ContentMappingCsv.read(file).stream()
                .filter(row -> kind.equals(row.kind()))
                .toList();
    }

    @Test
    void nodeTypeEnumMatchesCsvRows() throws IOException {
        final Set<String> csvIds = rows("node_type").stream()
                .map(row -> row.newId())
                .collect(Collectors.toSet());
        final Set<String> enumIds = java.util.Arrays.stream(NodeType.values())
                .map(t -> "thaumcraft:" + t.registryPath())
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "thaumcraft:nodetype.normal",
                "thaumcraft:nodetype.unstable",
                "thaumcraft:nodetype.dark",
                "thaumcraft:nodetype.tainted",
                "thaumcraft:nodetype.hungry",
                "thaumcraft:nodetype.pure"), enumIds);
        assertEquals(csvIds, enumIds, "enum set drifted from the CSV node_type rows");
    }

    @Test
    void nodeModifierEnumMatchesCsvRows() throws IOException {
        final Set<String> csvIds = rows("node_modifier").stream()
                .map(row -> row.newId())
                .collect(Collectors.toSet());
        final Set<String> enumIds = java.util.Arrays.stream(NodeModifier.values())
                .map(m -> "thaumcraft:" + m.registryPath())
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "thaumcraft:nodemodifier.bright",
                "thaumcraft:nodemodifier.pale",
                "thaumcraft:nodemodifier.fading"), enumIds);
        assertEquals(csvIds, enumIds, "enum set drifted from the CSV node_modifier rows");
    }

    @Test
    void shortIdsAreLowercaseNamesAndRoundTrip() {
        for (final NodeType type : NodeType.values()) {
            assertEquals(type.name().toLowerCase(), type.id());
            assertSame(type, NodeType.fromId(type.id()));
        }
        for (final NodeModifier modifier : NodeModifier.values()) {
            assertEquals(modifier.name().toLowerCase(), modifier.id());
            assertSame(modifier, NodeModifier.fromId(modifier.id()));
        }
    }

    @Test
    void lenientLookupMatchesStoreCodecBehaviour() {
        // Unknown/null type reads as NORMAL (WorldMagicData.Node codec parity);
        // unknown/null modifier reads as "no modifier" (null), as the original's
        // absent TileNode field does.
        assertSame(NodeType.NORMAL, NodeType.fromId(null));
        assertSame(NodeType.NORMAL, NodeType.fromId("nonsense"));
        assertNull(NodeModifier.fromId(null));
        assertNull(NodeModifier.fromId("nonsense"));
        assertTrue(NodeType.values().length == 6 && NodeModifier.values().length == 3);
    }
}
