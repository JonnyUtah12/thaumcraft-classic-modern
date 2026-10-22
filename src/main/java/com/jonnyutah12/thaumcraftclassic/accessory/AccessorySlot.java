package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.Locale;

/**
 * The canonical accessory slot model (TC-M8-01, docs/porting-decisions.md D-005).
 *
 * <p>Slot semantics match Baubles 1.0.1.10 as used by Thaumcraft 4.2.3.5:
 * {@code InventoryBaubles} is a 4-slot inventory (index 0 = AMULET, 1-2 = RING,
 * 3 = BELT) validated against {@code BaubleType} ({@code RING/AMULET/BELT};
 * see docs/architecture/accessories.md for the source citations). CHARM is the
 * one extension slot: TC4's own code never used a fifth slot, but the task
 * scope (and the Curios "charm" preset semantics) fixes it as the generic
 * thaumaturgical charm slot, capacity 1.</p>
 *
 * <p>The enum is provider-independent: {@link #curiosSlotId()} is the
 * data-driven Curios 9.5.1 slot type id used by the optional bridge,
 * {@link #fallbackIndex()} the fixed index inside the fallback attachment
 * payload. Both are frozen once shipped — saves reference them forever.</p>
 */
public enum AccessorySlot {

    /** Baubles slots 1-2 ({@code BaubleType.RING}); Curios slot size 2. */
    RING("ring", 2, "thaumcraft.ring", 0),

    /** Baubles slot 0 ({@code BaubleType.AMULET}). */
    AMULET("amulet", 1, "thaumcraft.amulet", 2),

    /** Baubles slot 3 ({@code BaubleType.BELT}); focus pouch baubles included. */
    BELT("belt", 1, "thaumcraft.belt", 3),

    /** Extension slot (no TC4 original; D-005 scope). */
    CHARM("charm", 1, "thaumcraft.charm", 4);

    private final String id;
    private final int capacity;
    private final String curiosSlotId;
    private final int firstFallbackIndex;

    AccessorySlot(final String id, final int capacity, final String curiosSlotId, final int firstFallbackIndex) {
        this.id = id;
        this.capacity = capacity;
        this.curiosSlotId = curiosSlotId;
        this.firstFallbackIndex = firstFallbackIndex;
    }

    /** Stable lowercase slot name, part of the frozen contract. */
    public String id() {
        return id;
    }

    /** How many items of this slot type a player may wear at once. */
    public int capacity() {
        return capacity;
    }

    /** Curios slot type id (data file {@code thaumcraft:curios/slots/<this>}). */
    public String curiosSlotId() {
        return curiosSlotId;
    }

    /** Item tag routing this slot's items under Curios ({@code curios:<this>}). */
    public String curiosItemTag() {
        return curiosSlotId;
    }

    /** Total slot count of the fallback payload (RING 2 + AMULET + BELT + CHARM). */
    public static int fallbackSlotCount() {
        return 5;
    }

    /** Attachment payload index for (slot, localIndex); -1 when out of range. */
    public int fallbackIndex(final int localIndex) {
        if (localIndex < 0 || localIndex >= capacity) {
            return -1;
        }
        return firstFallbackIndex + localIndex;
    }

    /** Reverse mapping from attachment payload index; null when out of range. */
    public static AccessorySlot byFallbackIndex(final int index) {
        for (final AccessorySlot slot : values()) {
            if (index >= slot.firstFallbackIndex && index < slot.firstFallbackIndex + slot.capacity) {
                return slot;
            }
        }
        return null;
    }

    /** Lookup by stable id ("ring", "amulet", ...); null when unknown. */
    public static AccessorySlot byId(final String id) {
        for (final AccessorySlot slot : values()) {
            if (slot.id.equals(id)) {
                return slot;
            }
        }
        return null;
    }

    /** Lookup by Curios slot type id; null when unknown. */
    public static AccessorySlot byCuriosSlotId(final String curiosId) {
        for (final AccessorySlot slot : values()) {
            if (slot.curiosSlotId.equals(curiosId)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
