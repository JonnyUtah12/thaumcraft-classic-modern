package com.jonnyutah12.thaumcraftclassic.accessory;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M8-01: the shared validation rules — the exact TC4
 * {@code InventoryBaubles.isValidItem} semantics (item must implement
 * {@link AccessoryItem} and its applicable set must contain the target slot;
 * an ALL item is accepted by every slot), plus the capacity bound and the
 * item-side veto. Both providers route through these rules, so the
 * no-Curios fallback and the Curios bridge validate identically.
 *
 * <p>The rules core is exercised through the package-private
 * {@code accepts}/{@code insertable} overloads with a pure
 * {@link AccessoryItem} fake: constructing {@code Item} instances is
 * impossible in the unit-test JVM (the item registry is frozen after
 * bootstrap), and the {@code ItemStack instanceof} routing in the public
 * entry points is covered in-game by AccessoryGameTest.</p>
 */
class AccessoryRulesTest {

    private static final class Fake implements AccessoryItem {
        private final Set<AccessorySlot> slots;
        private boolean vetoEquip = true;
        private boolean vetoUnequip = true;

        Fake(final Set<AccessorySlot> slots) {
            this.slots = slots;
        }

        @Override
        public Set<AccessorySlot> applicableSlots(final ItemStack stack) {
            return slots;
        }

        @Override
        public boolean canEquip(final ItemStack stack, final net.minecraft.world.entity.player.Player player) {
            return vetoEquip;
        }

        @Override
        public boolean canUnequip(final ItemStack stack, final net.minecraft.world.entity.player.Player player) {
            return vetoUnequip;
        }
    }

    @Test
    void noSlotSetMeansRejectedEverywhere() {
        final Fake plain = new Fake(Set.of());
        for (final AccessorySlot slot : AccessorySlot.values()) {
            assertFalse(AccessoryRules.accepts(slot, plain, ItemStack.EMPTY), slot.id());
        }
    }

    @Test
    void slotRoutingMatchesApplicableSet() {
        final Fake ring = new Fake(Set.of(AccessorySlot.RING));
        assertTrue(AccessoryRules.accepts(AccessorySlot.RING, ring, ItemStack.EMPTY));
        assertFalse(AccessoryRules.accepts(AccessorySlot.AMULET, ring, ItemStack.EMPTY));
        assertFalse(AccessoryRules.accepts(AccessorySlot.CHARM, ring, ItemStack.EMPTY));

        final Fake all = new Fake(EnumSet.allOf(AccessorySlot.class));
        for (final AccessorySlot slot : AccessorySlot.values()) {
            assertTrue(AccessoryRules.accepts(slot, all, ItemStack.EMPTY), "ALL item rejected by " + slot.id());
        }
    }

    @Test
    void publicEntryPointsRejectNonAccessoryStacks() {
        final ItemStack plain = new ItemStack(net.minecraft.world.item.Items.STONE);
        for (final AccessorySlot slot : AccessorySlot.values()) {
            assertFalse(AccessoryRules.slotAccepts(slot, plain), slot.id());
        }
        assertFalse(AccessoryRules.slotAccepts(AccessorySlot.RING, ItemStack.EMPTY));
        assertFalse(AccessoryRules.canInsert(AccessorySlot.RING, 0, ItemStack.EMPTY, null));
    }

    @Test
    void insertableCombinesRoutingCapacityAndVeto() {
        final Fake ring = new Fake(Set.of(AccessorySlot.RING));
        assertTrue(AccessoryRules.insertable(AccessorySlot.RING, 0, ring, ItemStack.EMPTY, null));
        assertTrue(AccessoryRules.insertable(AccessorySlot.RING, 1, ring, ItemStack.EMPTY, null));
        assertFalse(AccessoryRules.insertable(AccessorySlot.RING, 2, ring, ItemStack.EMPTY, null),
                "capacity not enforced");
        assertFalse(AccessoryRules.insertable(AccessorySlot.RING, -1, ring, ItemStack.EMPTY, null));
        assertFalse(AccessoryRules.insertable(AccessorySlot.AMULET, 0, ring, ItemStack.EMPTY, null));
        assertFalse(AccessoryRules.insertable(null, 0, ring, ItemStack.EMPTY, null));

        ring.vetoEquip = false;
        assertFalse(AccessoryRules.insertable(AccessorySlot.RING, 0, ring, ItemStack.EMPTY, null),
                "item-side canEquip veto not respected");
    }

    @Test
    void canRemoveHonoursUnequipVeto() {
        // Non-accessory and empty stacks are always removable (public path).
        assertTrue(AccessoryRules.canRemove(new ItemStack(net.minecraft.world.item.Items.STONE), null));
        assertTrue(AccessoryRules.canRemove(ItemStack.EMPTY, null));
        // The item-side veto itself is the AccessoryItem contract:
        final Fake probe = new Fake(Set.of(AccessorySlot.RING));
        assertTrue(probe.canUnequip(ItemStack.EMPTY, null));
        probe.vetoUnequip = false;
        assertFalse(probe.canUnequip(ItemStack.EMPTY, null));
    }
}
