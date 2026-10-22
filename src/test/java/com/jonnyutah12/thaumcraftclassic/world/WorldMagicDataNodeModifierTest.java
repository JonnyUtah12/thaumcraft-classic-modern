package com.jonnyutah12.thaumcraftclassic.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

/**
 * TC-M5-01 acceptance: node records (type + modifier + intensity + aspects)
 * survive save/reload through the M2-02 world store, and pre-M5 records
 * without a modifier field load unchanged (lenient codec).
 */
class WorldMagicDataNodeModifierTest {

    @Test
    void nodeModifierRoundTrips() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(3, -4);
        data.addNode(pos, new WorldMagicData.Node(50, 70, -50, "dark", "fading", 120,
                Map.of("mortuus", 40, "tenebrae", 80)));
        data.addNode(pos, new WorldMagicData.Node(51, 71, -51, "pure", null, 90, Map.of("ordo", 91)));

        final WorldMagicData reloaded = WorldMagicData.load(data.save(new CompoundTag(), null), null);
        final var nodes = reloaded.getNodes(pos);
        assertEquals(2, nodes.size());
        final WorldMagicData.Node dark = nodes.stream().filter(n -> n.x() == 50).findFirst().orElseThrow();
        assertEquals("dark", dark.type());
        assertEquals("fading", dark.modifier());
        assertEquals(120, dark.intensity());
        assertEquals(Map.of("mortuus", 40, "tenebrae", 80), dark.aspects());
        final WorldMagicData.Node pure = nodes.stream().filter(n -> n.x() == 51).findFirst().orElseThrow();
        assertNull(pure.modifier(), "modifier-less node must reload without one");
    }

    @Test
    void legacyNodeRecordsWithoutModifierStillLoad() {
        // A record written before TC-M5-01 has no "modifier" key — the codec
        // must read it as "no modifier", not fail.
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(0, 0);
        data.addNode(pos, new WorldMagicData.Node(1, 2, 3, "normal", 5));
        final WorldMagicData reloaded = WorldMagicData.load(data.save(new CompoundTag(), null), null);
        final WorldMagicData.Node node = reloaded.getNodes(pos).get(0);
        assertEquals("normal", node.type());
        assertNull(node.modifier());
        assertTrue(reloaded.getNodes(pos).size() == 1);
    }
}
