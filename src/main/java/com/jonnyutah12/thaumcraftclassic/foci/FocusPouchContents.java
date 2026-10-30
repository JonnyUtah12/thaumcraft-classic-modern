package com.jonnyutah12.thaumcraftclassic.foci;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The focus pouch's stored inventory (TC-M6-02): the original
 * {@code ItemFocusPouch.getInventory}/{@code setInventory} 18-slot
 * {@code ItemStack[]} ({@code common/items/wands/ItemFocusPouch.java:63-93}).
 *
 * <p>The original wrote a sparse {@code "Inventory"} {@code NBTTagList} of
 * {@code {Slot: byte, ...item}} compounds, i.e. the slot index was part of the
 * data and an empty slot was simply absent. The port keeps that meaning in a
 * positional list — index <em>is</em> the slot, an empty slot is
 * {@link ItemStack#EMPTY} — so slot positions survive a save round-trip the
 * same way, and the list is bounded to the original 18 slots.</p>
 *
 * <p>Only wand foci belong in a pouch: the original's
 * {@code InventoryFocusPouch.isItemValidForSlot} accepted an
 * {@code ItemFocusBasic} and nothing else
 * ({@code common/container/InventoryFocusPouch.java:69-71}). The port's focus
 * item is {@code items/foci/ItemFocus}; whether a stack qualifies is
 * {@link ItemFocusPouch#isValidFocus(ItemStack)} (which also refuses a nested
 * pouch, keeping serialization finite).</p>
 *
 * @param slots exactly-positional slots, at most {@link #CAPACITY} entries;
 *              shorter lists mean the trailing slots are empty
 */
public record FocusPouchContents(List<ItemStack> slots) {

    /** The original pouch's slot count ({@code stackList = new ItemStack[18]}). */
    public static final int CAPACITY = 18;

    /** An empty pouch. */
    public static final FocusPouchContents EMPTY = new FocusPouchContents(List.of());

    /**
     * Persisted shape: a positional list of optional stacks, so an empty slot
     * encodes as an explicit absence and the indices of the filled slots are
     * preserved (the original's {@code "Slot"} byte).
     */
    public static final Codec<FocusPouchContents> CODEC =
            ItemStack.OPTIONAL_CODEC.listOf().xmap(FocusPouchContents::of, FocusPouchContents::slots);

    /** Network shape — same layout, so the client sees the slots it renders. */
    public static final StreamCodec<RegistryFriendlyByteBuf, FocusPouchContents> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(FocusPouchContents::of, FocusPouchContents::slots);

    /** Validates and freezes a slot list. */
    public static FocusPouchContents of(final List<ItemStack> slots) {
        if (slots.size() > CAPACITY) {
            throw new IllegalArgumentException(
                    "a focus pouch holds " + CAPACITY + " foci, got " + slots.size());
        }
        for (final ItemStack stack : slots) {
            if (stack == null) {
                throw new IllegalArgumentException("focus pouch slots must not be null");
            }
        }
        return new FocusPouchContents(List.copyOf(slots));
    }

    /** Compact-constructor guard so a hand-built record cannot exceed the pouch. */
    public FocusPouchContents {
        if (slots.size() > CAPACITY) {
            throw new IllegalArgumentException("a focus pouch holds " + CAPACITY + " foci");
        }
        slots = List.copyOf(slots);
    }

    /** The stack in {@code slot}, or {@link ItemStack#EMPTY} (the original's null slot). */
    public ItemStack get(final int slot) {
        return slot >= 0 && slot < slots.size() ? slots.get(slot) : ItemStack.EMPTY;
    }

    /** The slot index holding a focus with {@code tag}, or -1. */
    public int find(final String tag) {
        for (int i = 0; i < slots.size(); i++) {
            final ItemStack stack = slots.get(i);
            if (!stack.isEmpty() && com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus.focusOf(stack) != null
                    && tag.equals(com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus.focusOf(stack).tag())) {
                return i;
            }
        }
        return -1;
    }

    /** The lowest empty slot, or -1 when the pouch is full. */
    public int firstFreeSlot() {
        for (int i = 0; i < CAPACITY; i++) {
            if (get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** The number of stored foci. */
    public int size() {
        int count = 0;
        for (final ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** Every stored focus, in slot order. */
    public List<ItemStack> foci() {
        final List<ItemStack> out = new ArrayList<>();
        for (final ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return List.copyOf(out);
    }

    /** The first stored focus, i.e. the pouch's fixed sort order head; empty when the pouch is empty. */
    public ItemStack first() {
        for (final ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** This inventory with {@code slot} set to {@code stack} ({@code ItemStack.EMPTY} clears it). */
    public FocusPouchContents with(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= CAPACITY) {
            throw new IllegalArgumentException("pouch slot out of range: " + slot);
        }
        final List<ItemStack> out = new ArrayList<>(slots);
        while (out.size() <= slot) {
            out.add(ItemStack.EMPTY);
        }
        out.set(slot, stack == null ? ItemStack.EMPTY : stack);
        return of(out);
    }

    /** The stored focus stack with {@code tag}, if any (original {@code getInventory} lookup). */
    public Optional<ItemStack> byTag(final String tag) {
        final int slot = find(tag);
        return slot < 0 ? Optional.empty() : Optional.of(get(slot));
    }

    /** The contents as the original's fixed-length {@code ItemStack[18]}. */
    public ItemStack[] toArray() {
        final ItemStack[] out = new ItemStack[CAPACITY];
        for (int i = 0; i < CAPACITY; i++) {
            out[i] = get(i);
        }
        return out;
    }

    /** The inverse of {@link #toArray()} — the original {@code setInventory} accepts the array. */
    public static FocusPouchContents fromArray(final ItemStack[] array) {
        final List<ItemStack> out = new ArrayList<>(Math.min(array.length, CAPACITY));
        for (int i = 0; i < array.length && i < CAPACITY; i++) {
            out.add(array[i] == null ? ItemStack.EMPTY : array[i]);
        }
        return of(out);
    }
}
