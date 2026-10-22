package com.jonnyutah12.thaumcraftclassic.accessory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * TC-M8-01: the frozen slot model. Capacities match Baubles 1.0.1.10 as used
 * by TC4 (amulet 1, ring 2, belt 3→1, plus the D-005 CHARM extension), the
 * fallback payload indices are fixed, and every lookup round-trips. These
 * values are a save-compatibility contract: changing any of them breaks
 * existing player saves.
 */
class AccessorySlotModelTest {

    @Test
    void capacitiesMatchBaublesSemantics() {
        assertEquals(2, AccessorySlot.RING.capacity());
        assertEquals(1, AccessorySlot.AMULET.capacity());
        assertEquals(1, AccessorySlot.BELT.capacity());
        assertEquals(1, AccessorySlot.CHARM.capacity());
        assertEquals(5, AccessorySlot.fallbackSlotCount());
    }

    @Test
    void fallbackIndicesAreFrozen() {
        assertEquals(0, AccessorySlot.RING.fallbackIndex(0));
        assertEquals(1, AccessorySlot.RING.fallbackIndex(1));
        assertEquals(2, AccessorySlot.AMULET.fallbackIndex(0));
        assertEquals(3, AccessorySlot.BELT.fallbackIndex(0));
        assertEquals(4, AccessorySlot.CHARM.fallbackIndex(0));
        assertEquals(-1, AccessorySlot.RING.fallbackIndex(2));
        assertEquals(-1, AccessorySlot.RING.fallbackIndex(-1));
        assertEquals(-1, AccessorySlot.AMULET.fallbackIndex(1));
    }

    @Test
    void indexAndIdLookupsRoundTrip() {
        for (final AccessorySlot slot : AccessorySlot.values()) {
            for (int i = 0; i < slot.capacity(); i++) {
                assertSame(slot, AccessorySlot.byFallbackIndex(slot.fallbackIndex(i)));
            }
            assertSame(slot, AccessorySlot.byId(slot.id()));
            assertSame(slot, AccessorySlot.byCuriosSlotId(slot.curiosSlotId()));
        }
        assertNull(AccessorySlot.byFallbackIndex(5));
        assertNull(AccessorySlot.byFallbackIndex(-1));
        assertNull(AccessorySlot.byId("Ring"));
        assertNull(AccessorySlot.byCuriosSlotId("ring"));
    }

    @Test
    void curiosSlotIdsAreNamespacedAndTagsMatch() {
        for (final AccessorySlot slot : AccessorySlot.values()) {
            assertEquals("thaumcraft." + slot.id(), slot.curiosSlotId());
            assertEquals(slot.curiosSlotId(), slot.curiosItemTag());
        }
    }
}
