package com.jonnyutah12.thaumcraftclassic.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codec round-trip and forward-tolerance contract for the world magic store
 * (TC-M2-02 acceptance). The provider argument is unused by this store, so null
 * is a valid stand-in outside a running game.
 */
class WorldMagicDataTest {

    private static CompoundTag save(final WorldMagicData data) {
        return data.save(new CompoundTag(), null);
    }

    private static WorldMagicData populate() {
        final WorldMagicData data = new WorldMagicData();
        final ChunkPos pos = new ChunkPos(7, -11);
        data.setMagicGenDone(pos, true);
        data.setAura(pos, 100, 25);
        data.setAuraAspect(pos, "aer", 3);
        data.setAuraAspect(pos, "ignis", 4);
        data.setFlux(pos, 12, 34);
        data.setTaint(pos, 9);
        data.addNode(pos, new WorldMagicData.Node(10, 64, -10, "pure", 2, Map.of("aquam", 1)));
        data.addNode(pos, new WorldMagicData.Node(11, 65, -9, "normal", 0));
        data.setWorldValue("auraCap", 77);
        return data;
    }

    @Test
    void roundTripPreservesEveryField() {
        final WorldMagicData original = populate();
        final WorldMagicData reloaded = WorldMagicData.load(save(original), null);

        assertEquals(WorldMagicData.CURRENT_VERSION, reloaded.getVersion());
        final ChunkPos pos = new ChunkPos(7, -11);
        assertTrue(reloaded.isMagicGenDone(pos));
        assertEquals(100, reloaded.getAuraDensity(pos));
        assertEquals(25, reloaded.getAuraPurity(pos));
        assertEquals(Map.of("aer", 3, "ignis", 4), reloaded.getAuraAspects(pos));
        assertEquals(12, reloaded.getFluxAir(pos));
        assertEquals(34, reloaded.getFluxGround(pos));
        assertEquals(9, reloaded.getTaint(pos));
        assertEquals(2, reloaded.getNodes(pos).size());
        final WorldMagicData.Node node = reloaded.getNodes(pos).get(0);
        assertEquals(10, node.x());
        assertEquals(64, node.y());
        assertEquals(-10, node.z());
        assertEquals("pure", node.type());
        assertEquals(2, node.intensity());
        assertEquals(Map.of("aquam", 1), node.aspects());
        assertEquals(WorldMagicData.Node.DEFAULT_TYPE, reloaded.getNodes(pos).get(1).type());
        assertEquals(77, reloaded.getWorldValue("auraCap"));
    }

    @Test
    void unknownKeysAndFieldsAreIgnored() {
        final CompoundTag tag = save(populate());
        // A future writer adds new keys at every level of the schema.
        tag.putInt("futureWorldField", 123);
        tag.put("futureCompound", new CompoundTag());
        final ListTag chunks = tag.getList("chunks", Tag.TAG_COMPOUND);
        final CompoundTag chunk = chunks.getCompound(0);
        chunk.putInt("futureChunkField", 5);
        final ListTag nodes = chunk.getList("nodes", Tag.TAG_COMPOUND);
        nodes.getCompound(0).putString("futureNodeField", "x");
        nodes.getCompound(0).put("futureList", new ListTag());

        final WorldMagicData reloaded = WorldMagicData.load(tag, null);
        final ChunkPos pos = new ChunkPos(7, -11);
        assertEquals(100, reloaded.getAuraDensity(pos));
        assertEquals(2, reloaded.getNodes(pos).size());
        assertEquals("pure", reloaded.getNodes(pos).get(0).type());
        assertEquals(77, reloaded.getWorldValue("auraCap"));
    }

    @Test
    void missingFieldsFallBackToDefaults() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("version", 1);
        final ListTag chunks = new ListTag();
        final CompoundTag chunk = new CompoundTag();
        chunk.putInt("x", 1);
        chunk.putInt("z", 2);
        chunks.add(chunk);
        tag.put("chunks", chunks);

        final WorldMagicData reloaded = WorldMagicData.load(tag, null);
        final ChunkPos pos = new ChunkPos(1, 2);
        assertFalse(reloaded.isMagicGenDone(pos));
        assertEquals(0, reloaded.getAuraDensity(pos));
        assertEquals(Map.of(), reloaded.getAuraAspects(pos));
        assertEquals(0, reloaded.getFluxAir(pos));
        assertEquals(0, reloaded.getTaint(pos));
        assertEquals(0, reloaded.getWorldValue("anything"));
        // A chunk entry without coordinates is identity-less and must be skipped,
        // not crash.
        final CompoundTag broken = new CompoundTag();
        broken.putInt("taint", 3);
        chunks.add(broken);
        final WorldMagicData tolerant = WorldMagicData.load(tag, null);
        assertNotNull(tolerant.getChunk(new ChunkPos(1, 2)));
    }

    @Test
    void newerSchemaVersionStillParsesKnownFields() {
        final CompoundTag tag = save(populate());
        tag.putInt("version", WorldMagicData.CURRENT_VERSION + 5);
        final WorldMagicData reloaded = WorldMagicData.load(tag, null);
        assertEquals(WorldMagicData.CURRENT_VERSION + 5, reloaded.getVersion());
        assertTrue(reloaded.isMagicGenDone(new ChunkPos(7, -11)));
        assertEquals(12, reloaded.getFluxAir(new ChunkPos(7, -11)));
    }

    @Test
    void mutationsMarkTheStoreDirty() {
        final WorldMagicData data = new WorldMagicData();
        assertFalse(data.isDirty());
        data.setTaint(new ChunkPos(0, 0), 1);
        assertTrue(data.isDirty());
    }

    @Test
    void absentChunkReadsAsNoRecord() {
        final WorldMagicData data = new WorldMagicData();
        assertNull(data.getChunk(new ChunkPos(4, 4)));
        assertEquals(0, data.getFluxAir(new ChunkPos(4, 4)));
        assertEquals(java.util.List.of(), data.getNodes(new ChunkPos(4, 4)));
    }

    @Test
    void zeroValuesAreNotWritten() {
        final WorldMagicData data = new WorldMagicData();
        data.setFlux(new ChunkPos(1, 1), 0, 0);
        final CompoundTag tag = save(data);
        final ListTag chunks = tag.getList("chunks", Tag.TAG_COMPOUND);
        assertEquals(1, chunks.size());
        assertFalse(chunks.getCompound(0).contains("fluxAir"));
        assertFalse(chunks.getCompound(0).contains("fluxGround"));
    }
}
