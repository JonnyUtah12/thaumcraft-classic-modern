package com.jonnyutah12.thaumcraftclassic.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codec round-trip, forward tolerance, and original-format parity for the player
 * knowledge/warp attachments (TC-M2-02 acceptance). The hand-written NBT fixtures
 * use the exact tag layout of Thaumcraft 4.2.3.5 ResearchManager, which is the
 * parity contract for a future legacy-import path.
 */
class PlayerDataCodecTest {

    private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;

    private static <T> Tag encode(Codec<T> codec, T value) {
        return codec.encodeStart(OPS, value).getOrThrow();
    }

    private static <T> T decode(Codec<T> codec, Tag tag) {
        return codec.parse(OPS, tag).getOrThrow();
    }

    private static PlayerKnowledgeData sampleKnowledge() {
        return PlayerKnowledgeData.empty()
                .withResearchCompleted("THEORYcraft")
                .withResearchCompleted("ALCHIMIST")
                .withAspectDiscovered("aer", 15)
                .withAspectDiscovered("ignis", 20)
                .withScannedObject("thaumcraft:stone_amber")
                .withScannedEntity("thaumcraft:Taintacle")
                .withScannedPhenomena("TC.ENTRANCE.AIR");
    }

    private static PlayerWarpData sampleWarp() {
        return PlayerWarpData.empty()
                .addWarpPerm(4)
                .addWarpTemp(2)
                .addWarpSticky(1)
                .withWarpCounter(3)
                .withRunicCharge(50);
    }

    @Test
    void knowledgeRoundTripsThroughNbt() {
        final PlayerKnowledgeData value = sampleKnowledge();
        final Tag encoded = encode(PlayerKnowledgeData.CODEC, value);
        final PlayerKnowledgeData decoded = decode(PlayerKnowledgeData.CODEC, encoded);
        assertEquals(value, decoded);
        assertTrue(decoded.knowsResearch("THEORYcraft"));
        assertEquals(15, decoded.aspectAmount("aer"));
        assertTrue(decoded.scannedEntityKeys().contains("thaumcraft:Taintacle"));
    }

    @Test
    void warpRoundTripsThroughNbt() {
        final PlayerWarpData value = sampleWarp();
        final Tag encoded = encode(PlayerWarpData.CODEC, value);
        final PlayerWarpData decoded = decode(PlayerWarpData.CODEC, encoded);
        assertEquals(value, decoded);
        assertEquals(7, decoded.total());
    }

    @Test
    void knowledgeUsesOriginalThaumcraft4TagNames() {
        final Tag encoded = encode(PlayerKnowledgeData.CODEC, sampleKnowledge());
        final CompoundTag tag = (CompoundTag) encoded;
        // Exact ResearchManager constants — a legacy .thaum importer must be able
        // to reuse this codec unchanged.
        assertTrue(tag.contains(PlayerKnowledgeData.TAG_RESEARCH, Tag.TAG_LIST));
        assertTrue(tag.contains(PlayerKnowledgeData.TAG_ASPECTS, Tag.TAG_LIST));
        assertTrue(tag.contains(PlayerKnowledgeData.TAG_SCANNED_OBJECTS, Tag.TAG_LIST));
        assertTrue(tag.contains(PlayerKnowledgeData.TAG_SCANNED_ENTITIES, Tag.TAG_LIST));
        assertTrue(tag.contains(PlayerKnowledgeData.TAG_SCANNED_PHENOMENA, Tag.TAG_LIST));
        // Original entry shape: {key: "..."} and {key: aspectTag, amount: short}.
        final CompoundTag researchEntry = ((ListTag) tag.get(PlayerKnowledgeData.TAG_RESEARCH)).getCompound(0);
        assertTrue(researchEntry.contains("key", Tag.TAG_STRING));
        final CompoundTag aspectEntry = ((ListTag) tag.get(PlayerKnowledgeData.TAG_ASPECTS)).getCompound(0);
        assertTrue(aspectEntry.contains("key", Tag.TAG_STRING));
        assertTrue(aspectEntry.contains("amount", Tag.TAG_SHORT));
    }

    @Test
    void warpUsesOriginalThaumcraft4TagNames() {
        final CompoundTag tag = (CompoundTag) encode(PlayerWarpData.CODEC, sampleWarp());
        assertTrue(tag.contains(PlayerWarpData.TAG_WARP_PERM, Tag.TAG_ANY_NUMERIC));
        assertTrue(tag.contains(PlayerWarpData.TAG_WARP_TEMP, Tag.TAG_ANY_NUMERIC));
        assertTrue(tag.contains(PlayerWarpData.TAG_WARP_STICKY, Tag.TAG_ANY_NUMERIC));
        assertTrue(tag.contains(PlayerWarpData.TAG_WARP_COUNTER, Tag.TAG_ANY_NUMERIC));
        assertTrue(tag.contains(PlayerWarpData.TAG_RUNIC_CHARGE, Tag.TAG_ANY_NUMERIC));
    }

    @Test
    void decodesHandWrittenThaumcraft4PlayerData() {
        // Verbatim TC4 layout from ResearchManager.savePlayerData.
        final CompoundTag tag = new CompoundTag();
        final ListTag research = new ListTag();
        final CompoundTag r = new CompoundTag();
        r.putString("key", "ARTHEAUMANCY");
        research.add(r);
        tag.put("THAUMCRAFT.RESEARCH", research);
        final ListTag aspects = new ListTag();
        final CompoundTag a = new CompoundTag();
        a.putString("key", "orbis");
        a.putShort("amount", (short) 21);
        aspects.add(a);
        tag.put("THAUMCRAFT.ASPECTS", aspects);
        tag.putInt("Thaumcraft.eldritch", 6);
        tag.putInt("Thaumcraft.eldritch.temp", 1);
        tag.putInt("Thaumcraft.eldritch.sticky", 2);
        tag.putInt("Thaumcraft.eldritch.counter", 4);
        tag.putInt("Thaumcraft.shielding", 100);

        final PlayerKnowledgeData knowledge = decode(PlayerKnowledgeData.CODEC, tag);
        assertTrue(knowledge.knowsResearch("ARTHEAUMANCY"));
        assertEquals(21, knowledge.aspectAmount("orbis"));

        final PlayerWarpData warp = decode(PlayerWarpData.CODEC, tag);
        assertEquals(6, warp.warpPerm());
        assertEquals(1, warp.warpTemp());
        assertEquals(2, warp.warpSticky());
        assertEquals(4, warp.warpCounter());
        assertEquals(100, warp.runicCharge());
        assertEquals(9, warp.total());
    }

    @Test
    void unknownFieldsAreIgnored() {
        final PlayerKnowledgeData value = sampleKnowledge();
        final CompoundTag tag = (CompoundTag) encode(PlayerKnowledgeData.CODEC, value);
        tag.putString("THAUMCRAFT.FUTURE_STUFF", "whatever");
        tag.put("neoforge:future", new CompoundTag());
        final ListTag research = (ListTag) tag.get(PlayerKnowledgeData.TAG_RESEARCH);
        ((CompoundTag) research.get(0)).putInt("futureEntryField", 1);
        assertEquals(value, decode(PlayerKnowledgeData.CODEC, tag));

        final CompoundTag warpTag = (CompoundTag) encode(PlayerWarpData.CODEC, sampleWarp());
        warpTag.putInt("Thaumcraft.eldritch.future", 9);
        assertEquals(sampleWarp(), decode(PlayerWarpData.CODEC, warpTag));
    }

    @Test
    void missingFieldsDefaultToEmpty() {
        assertEquals(PlayerKnowledgeData.empty(), decode(PlayerKnowledgeData.CODEC, new CompoundTag()));
        assertEquals(PlayerWarpData.empty(), decode(PlayerWarpData.CODEC, new CompoundTag()));
    }

    @Test
    void warpMathClampsAtZeroLikeTheOriginal() {
        // TC4 PlayerKnowledge.addWarp*: Math.max(0, current + amount).
        assertEquals(0, PlayerWarpData.empty().addWarpTemp(-5).warpTemp());
        assertEquals(1, sampleWarp().addWarpTemp(-1).warpTemp());
        assertEquals(0, sampleWarp().addWarpPerm(-100).warpPerm());
        assertEquals(0, sampleWarp().addWarpSticky(-100).warpSticky());
        assertEquals(0, PlayerWarpData.empty().withWarpPerm(-7).warpPerm());
    }

    @Test
    void aspectAmountsStayInShortRange() {
        final PlayerKnowledgeData big = PlayerKnowledgeData.empty()
                .withAspectDiscovered("potentia", 40000)
                .withAspectDiscovered("potentia", 40000);
        assertEquals(Short.MAX_VALUE, big.aspectAmount("potentia"));
    }

    @Test
    void duplicateResearchKeysAreNotAddedTwice() {
        final PlayerKnowledgeData once = PlayerKnowledgeData.empty().withResearchCompleted("X");
        assertEquals(once, once.withResearchCompleted("X"));
    }
}
