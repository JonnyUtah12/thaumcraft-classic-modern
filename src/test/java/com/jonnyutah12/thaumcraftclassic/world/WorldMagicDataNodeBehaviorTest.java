package com.jonnyutah12.thaumcraftclassic.world;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M5-02 acceptance: the node record's new persisted fields — the vis base
 * ({@code aspectsBase}, TileNode L159-160) and the catch-up anchor
 * ({@code lastActive}, TileNode L159) — round-trip through the store, stay
 * lenient when absent (generation writes neither), and the in-place
 * {@code replaceNode} write-back mutator behaves.
 */
class WorldMagicDataNodeBehaviorTest {

    private static CompoundTag save(final WorldMagicData data) {
        return data.save(new CompoundTag(), null);
    }

    @Test
    void roundTripCarriesAspectsBaseAndLastActive() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(3, 4);
        data.addNode(pos, new WorldMagicData.Node(10, 64, -10, "dark", "pale", 7,
                Map.of("aer", 2), Map.of("aer", 5, "ignis", 0), 123456789L));
        final WorldMagicData reloaded = WorldMagicData.load(save(data), null);
        final WorldMagicData.Node node = reloaded.getNodes(pos).get(0);
        assertEquals(Map.of("aer", 2), node.aspects());
        assertEquals(Map.of("aer", 5, "ignis", 0), node.aspectsBase());
        assertEquals(123456789L, node.lastActive());
        assertEquals("dark", node.type());
        assertEquals("pale", node.modifier());
    }

    @Test
    void generatedRecordsStayLenientWhenTheNewFieldsAreAbsent() {
        // Generation (TC-M5-01) writes neither field; old saves must still load,
        // with aspectsBase null = "same as aspects" at read time (TileNode L171-174).
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(-2, 8);
        data.addNode(pos, new WorldMagicData.Node(5, 60, 5, "normal", 3, Map.of("aer", 3)));
        final WorldMagicData reloaded = WorldMagicData.load(save(data), null);
        final WorldMagicData.Node node = reloaded.getNodes(pos).get(0);
        assertNull(node.aspectsBase());
        assertEquals(0L, node.lastActive());
        assertEquals(Map.of("aer", 3), node.aspects());
    }

    @Test
    void replaceNodeSwapsTheRecordInPlace() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(1, 1);
        data.addNode(pos, new WorldMagicData.Node(8, 64, 8, "normal", 3, Map.of("aer", 3)));
        data.addNode(pos, new WorldMagicData.Node(9, 64, 9, "normal", 3, Map.of("ignis", 3)));
        final WorldMagicData.Node updated = new WorldMagicData.Node(8, 64, 8, "normal", null, 3,
                Map.of("aer", 1), Map.of("aer", 3), 99L);
        assertTrue(data.replaceNode(pos, 8, 64, 8, updated));
        final var nodes = data.getNodes(pos);
        assertEquals(2, nodes.size());
        assertEquals(Map.of("aer", 1), nodes.get(0).aspects());
        assertEquals(99L, nodes.get(0).lastActive());
        assertEquals(Map.of("ignis", 3), nodes.get(1).aspects()); // untouched
        assertFalse(data.replaceNode(pos, 100, 100, 100, updated));
        assertFalse(data.replaceNode(new ChunkPos(50, 50), 8, 64, 8, updated));
        // The replacement persists through save/load.
        final WorldMagicData reloaded = WorldMagicData.load(save(data), null);
        assertEquals(Map.of("aer", 1), reloaded.getNodes(pos).get(0).aspects());
    }

    @Test
    void legacyShapeConstructorsDefaultTheNewFields() {
        final WorldMagicData.Node legacy = new WorldMagicData.Node(1, 2, 3, "normal", 1);
        assertNull(legacy.aspectsBase());
        assertEquals(0L, legacy.lastActive());
        final WorldMagicData.Node sevenArg = new WorldMagicData.Node(1, 2, 3, "normal", "bright", 1,
                Map.of("aer", 1));
        assertNull(sevenArg.aspectsBase());
        assertEquals(0L, sevenArg.lastActive());
    }
}
