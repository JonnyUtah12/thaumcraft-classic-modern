package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData.HexEntry;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the TC-M3-04 note state to the original
 * {@code ResearchNoteData}/{@code ResearchManager.HexEntry} NBT layout
 * (TC 4.2.3.5): the {@code key}/{@code color}/{@code complete}/{@code copies}
 * keys, the {@code hexgrid} list of {@code {hexq, hexr, type, aspect}}
 * compounds with byte numerics, and lenient reading of widened numerics.
 */
class ResearchNoteDataTest {

    private static ResearchNoteData sample() {
        final Map<String, HexEntry> cells = new LinkedHashMap<>();
        cells.put("-1:1", new HexEntry(null, 0));
        cells.put("0:0", new HexEntry("ignis", 1));
        cells.put("1:0", new HexEntry("lux", 2));
        return new ResearchNoteData("FOCUSFIRE", 16734721, false, 3, cells);
    }

    private static CompoundTag encode(ResearchNoteData note) {
        return (CompoundTag) ResearchNoteData.CODEC.encodeStart(NbtOps.INSTANCE, note).getOrThrow();
    }

    @Test
    void codecWritesTheOriginalNbtKeyLayout() {
        final CompoundTag tag = encode(sample());
        assertEquals(Set.of("key", "color", "copies", "hexgrid"), tag.getAllKeys(),
                "complete equals the original default false, so the write leaves it out "
                        + "(the original's own reader takes an absent boolean as false)");
        assertEquals("FOCUSFIRE", tag.getString("key"));
        assertEquals(16734721, tag.getInt("color"));
        assertFalse(tag.getBoolean("complete"), "an absent complete reads as the original default false");
        assertEquals(3, tag.getInt("copies"));

        final CompoundTag complete = encode(new ResearchNoteData("FOCUSFIRE", 16734721, true, 0, Map.of()));
        assertEquals(Set.of("key", "color", "complete"), complete.getAllKeys(),
                "copies 0 is the original default and an empty grid adds no hexgrid key");
        assertTrue(complete.getBoolean("complete"));
    }

    @Test
    void hexGridRowsCarryTheOriginalByteNumericsAndTheQRKey() {
        final CompoundTag tag = encode(sample());
        final ListTag grid = tag.getList("hexgrid", Tag.TAG_COMPOUND);
        assertEquals(3, grid.size());
        boolean sawRequired = false;
        boolean sawPlaced = false;
        boolean sawEmpty = false;
        for (int i = 0; i < grid.size(); i++) {
            final CompoundTag row = grid.getCompound(i);
            assertTrue(Set.of("hexq", "hexr", "type", "aspect").containsAll(row.getAllKeys()),
                    "unexpected row keys " + row.getAllKeys());
            assertEquals(Tag.TAG_BYTE, row.get("hexq").getId(), "hexq is an NBT byte in the original");
            assertEquals(Tag.TAG_BYTE, row.get("hexr").getId(), "hexr is an NBT byte in the original");
            assertEquals(Tag.TAG_BYTE, row.get("type").getId(), "type is an NBT byte in the original");

            final Hex hex = new Hex((byte) row.getByte("hexq"), (byte) row.getByte("hexr"));
            final HexEntry cell = sample().entryAt(hex);
            assertNotNull(cell, "row " + hex + " is not a cell of the sample grid");
            assertEquals(cell.type(), row.getByte("type"), "type of " + hex);
            if (cell.aspectTag() == null) {
                assertFalse(row.contains("aspect"), "the original omitted the aspect key for an empty cell");
                sawEmpty = true;
            } else {
                assertEquals(Tag.TAG_STRING, row.get("aspect").getId(), "aspect is an NBT string in the original");
                assertEquals(cell.aspectTag(), row.getString("aspect"), "aspect of " + hex);
            }
            sawRequired |= cell.type() == 1;
            sawPlaced |= cell.type() == 2;
        }
        assertTrue(sawRequired && sawPlaced && sawEmpty,
                "the sample carries a required, a player-placed and an empty cell");
    }

    @Test
    void codecRoundTripsKeyColourFlagsAndEveryCell() {
        final ResearchNoteData decoded =
                ResearchNoteData.CODEC.parse(NbtOps.INSTANCE, encode(sample())).getOrThrow();
        assertEquals(sample(), decoded);
    }

    @Test
    void codecReadsLenientlyFromWidenedNumerics() {
        // The original wrote bytes but read with getByte()/getInt(); a legacy note
        // whose grid was widened (ints) must decode to the same state.
        final CompoundTag row = new CompoundTag();
        row.putInt("hexq", 1);
        row.putInt("hexr", -1);
        row.putInt("type", 1);
        row.putString("aspect", "aer");
        final ListTag grid = new ListTag();
        grid.add(row);
        final CompoundTag root = new CompoundTag();
        root.putString("key", "AER");
        root.putInt("color", 7);
        root.putBoolean("complete", true);
        root.putInt("copies", 2);
        root.put("hexgrid", grid);

        final ResearchNoteData decoded = ResearchNoteData.CODEC.parse(NbtOps.INSTANCE, root).getOrThrow();
        assertEquals("AER", decoded.key());
        assertEquals(7, decoded.color());
        assertTrue(decoded.complete());
        assertEquals(2, decoded.copies());
        assertEquals(new HexEntry("aer", 1), decoded.entryAt(new Hex(1, -1)));
    }

    @Test
    void absentOrNullGridNormalisesToAnEmptyMap() {
        final ResearchNoteData decoded = ResearchNoteData.CODEC
                .parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        assertEquals(Map.of(), decoded.hexEntries());
        assertEquals("", decoded.key());
        assertFalse(decoded.complete());
        assertEquals(0, decoded.copies());
        assertEquals(Map.of(), new ResearchNoteData("K", 0, false, 0, null).hexEntries());
        assertFalse(encode(new ResearchNoteData("K", 0, false, 0, Map.of())).contains("hexgrid"),
                "an empty grid writes no hexgrid key; a reader takes it as no cells");
    }

    @Test
    void mutatorsReturnNewInstancesAndLeaveTheOriginalAlone() {
        final ResearchNoteData note = sample();
        final ResearchNoteData withCell = note.withHexEntry(new Hex(2, 0), new HexEntry("ordo", 2));
        assertNotSame(note, withCell);
        assertEquals(3, note.hexEntries().size(), "withHexEntry must not mutate the receiver");
        assertEquals(4, withCell.hexEntries().size());

        final ResearchNoteData pruned = withCell.without(java.util.List.of("2:0", "-1:1"));
        assertNotSame(withCell, pruned);
        assertEquals(2, pruned.hexEntries().size());
        assertEquals(4, withCell.hexEntries().size(), "without must not mutate the receiver");

        assertTrue(note.withComplete(true).complete());
        assertFalse(note.complete());
        assertEquals(9, note.withCopies(9).copies());
        assertEquals(3, note.copies());
    }

    @Test
    void hexesAndEntryAtUseTheOriginalQRKey() {
        final ResearchNoteData note = sample();
        assertEquals(Set.of("-1:1", "0:0", "1:0"), note.hexes().keySet());
        assertEquals(new Hex(1, 0), note.hexes().get("1:0"));
        assertEquals(new HexEntry("ignis", 1), note.entryAt(new Hex(0, 0)));
        assertNull(note.entryAt(new Hex(9, 9)));
    }

    @Test
    void emptyCellReportsItselfEmpty() {
        assertTrue(new HexEntry(null, 0).empty());
        assertTrue(new HexEntry("", 0).empty());
        assertFalse(new HexEntry("ignis", 1).empty());
    }
}
