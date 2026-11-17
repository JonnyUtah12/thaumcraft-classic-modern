package com.jonnyutah12.thaumcraftclassic.foci;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-02 acceptance: the focus pouch's stored inventory, as pure data.
 *
 * <p>Source of truth: {@code ItemFocusPouch.getInventory}/{@code setInventory}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/items/wands/
 * ItemFocusPouch.java:63-93}) — an {@code ItemStack[18]} whose filled slots were
 * written as {@code {Slot: byte, ...item}} compounds and whose empty slots were
 * simply absent. {@link FocusPouchContents} keeps that meaning positionally.</p>
 *
 * <p>The component-backed half ({@code ItemFocusPouch.contents}/{@code insert}/
 * {@code extract} through {@code thaumcraft:focus_pouch_contents}) needs the
 * live data-component and item registries, so it runs in
 * {@code gametest/FociGameTest}. Here the record's own contract is pinned: the
 * original 18-slot bound, positional slots, the lowest-empty-slot rule and the
 * fixed-length array round-trip.</p>
 */
class FocusPouchContentsTest {

    @Test
    void capacityIsTheOriginalEighteenSlots() {
        assertEquals(18, FocusPouchContents.CAPACITY);
        final List<ItemStack> full = new ArrayList<>();
        for (int i = 0; i < FocusPouchContents.CAPACITY; i++) {
            full.add(ItemStack.EMPTY);
        }
        assertEquals(FocusPouchContents.CAPACITY, FocusPouchContents.of(full).slots().size());

        full.add(ItemStack.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> FocusPouchContents.of(full),
                "a 19th slot must be refused, as the original's [18] array was");
    }

    @Test
    void positionsAreSlotsAndEmptySlotsAreAbsent() {
        final FocusPouchContents empty = FocusPouchContents.EMPTY;
        assertEquals(0, empty.size(), "a fresh pouch is empty");
        assertTrue(empty.first().isEmpty(), "an empty pouch has no first focus");
        assertEquals(0, empty.firstFreeSlot(), "the first free slot of a fresh pouch is 0");
        assertTrue(empty.get(0).isEmpty());
        assertTrue(empty.get(17).isEmpty());
        assertTrue(empty.get(99).isEmpty(), "an out-of-range slot reads as empty, not an error");

        // The original wrote the slot index, so a stack in slot 5 stays in slot 5.
        final FocusPouchContents placed = empty.with(5, new ItemStack(net.minecraft.world.item.Items.STICK));
        assertEquals(1, placed.size());
        assertTrue(placed.get(0).isEmpty(), "slot 0 was never written");
        assertTrue(placed.get(4).isEmpty(), "slot 4 was never written");
        assertTrue(clone(placed.get(5)).is(net.minecraft.world.item.Items.STICK), "slot 5 carries the stack");
        assertEquals(0, placed.firstFreeSlot(), "slot 0 is still free");
        assertTrue(placed.first().is(net.minecraft.world.item.Items.STICK), "first() is slot order, not insertion order");

        // Writing ItemStack.EMPTY back clears the slot — the original removed the entry.
        final FocusPouchContents cleared = placed.with(5, ItemStack.EMPTY);
        assertEquals(0, cleared.size());
        assertTrue(cleared.slots().size() >= 6, "clearing keeps the slot domain, it does not shift the others");
    }

    @Test
    void lowestEmptySlotWinsAndAFullPouchHasNone() {
        FocusPouchContents contents = FocusPouchContents.EMPTY;
        for (int i = 0; i < FocusPouchContents.CAPACITY; i++) {
            assertEquals(i, contents.firstFreeSlot(), "slot " + i + " is the lowest free one");
            contents = contents.with(i, new ItemStack(net.minecraft.world.item.Items.STICK));
        }
        assertEquals(-1, contents.firstFreeSlot(), "a full pouch reports no free slot");
        assertEquals(FocusPouchContents.CAPACITY, contents.size());
        assertEquals(FocusPouchContents.CAPACITY, contents.foci().size());
    }

    /** The original's fixed-length {@code ItemStack[18]} survives the round-trip. */
    @Test
    void arrayRoundTripPreservesSlots() {
        final ItemStack[] slots = new ItemStack[FocusPouchContents.CAPACITY];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = ItemStack.EMPTY;
        }
        slots[2] = new ItemStack(net.minecraft.world.item.Items.STICK);
        slots[17] = new ItemStack(net.minecraft.world.item.Items.STONE);

        final FocusPouchContents contents = FocusPouchContents.fromArray(slots);
        assertEquals(2, contents.size());
        assertTrue(contents.get(2).is(net.minecraft.world.item.Items.STICK));
        assertTrue(contents.get(17).is(net.minecraft.world.item.Items.STONE));

        final ItemStack[] back = contents.toArray();
        assertEquals(FocusPouchContents.CAPACITY, back.length, "getInventory always yields 18 slots");
        assertTrue(back[2].is(net.minecraft.world.item.Items.STICK));
        assertTrue(back[17].is(net.minecraft.world.item.Items.STONE));
        for (int i = 0; i < back.length; i++) {
            if (i != 2 && i != 17) {
                assertTrue(back[i].isEmpty(), "slot " + i + " must read back empty");
            }
        }
    }

    /** A null slot is the original's null array entry, normalised to empty. */
    @Test
    void nullSlotsNormaliseToEmpty() {
        final FocusPouchContents contents = FocusPouchContents.fromArray(
                new ItemStack[] {new ItemStack(net.minecraft.world.item.Items.STICK), null});
        assertEquals(1, contents.size());
        assertTrue(contents.get(1).isEmpty());
    }

    @Test
    void findReportsAbsentOrTheSlot() {
        assertTrue(FocusPouchContents.EMPTY.byTag("fire").isEmpty());
        assertEquals(-1, FocusPouchContents.EMPTY.find("fire"));
    }

    private static ItemStack clone(final ItemStack stack) {
        return stack;
    }
}
