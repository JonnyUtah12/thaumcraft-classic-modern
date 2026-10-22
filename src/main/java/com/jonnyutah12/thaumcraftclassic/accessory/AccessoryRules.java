package com.jonnyutah12.thaumcraftclassic.accessory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared validation used by every {@link AccessoryProvider}: slot-type
 * routing, capacity bounds, and the item-side {@code canEquip} veto.
 *
 * <p>TC4 parity: {@code InventoryBaubles.isValidItem} required the item to
 * implement {@code IBauble} and its {@code BaubleType} to match the target
 * slot index; {@code BaubleType.ALL} items were accepted by every slot.
 * {@link AccessoryItem#applicableSlots(ItemStack)} models that directly.</p>
 */
public final class AccessoryRules {

    /** True when the stack is an accessory whose slot set contains {@code slot}. */
    public static boolean slotAccepts(final AccessorySlot slot, final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AccessoryItem accessory)) {
            return false;
        }
        return accepts(slot, accessory, stack);
    }

    /**
     * Slot routing for a known accessory behavior (the {@link ItemStack}
     * instanceof routing above is a one-liner over this; split out so the
     * rules are unit-testable without constructing {@code Item}s, which the
     * frozen unit-test registry forbids).
     */
    static boolean accepts(final AccessorySlot slot, final AccessoryItem accessory, final ItemStack stack) {
        return accessory.applicableSlots(stack).contains(slot);
    }

    /**
     * Full insertion check: slot routing + payload capacity + item-side veto.
     * Does not check whether the target slot is occupied (provider-specific).
     */
    public static boolean canInsert(final AccessorySlot slot, final int localIndex,
                                   final ItemStack stack, final Player player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AccessoryItem accessory)) {
            return false;
        }
        return insertable(slot, localIndex, accessory, stack, player);
    }

    static boolean insertable(final AccessorySlot slot, final int localIndex,
                              final AccessoryItem accessory, final ItemStack stack, final Player player) {
        if (slot == null || localIndex < 0 || localIndex >= slot.capacity()) {
            return false;
        }
        if (!accepts(slot, accessory, stack)) {
            return false;
        }
        return accessory.canEquip(stack, player);
    }

    /** Item-side removal veto; true when the stack is not an accessory. */
    public static boolean canRemove(final ItemStack stack, final Player player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AccessoryItem accessory)) {
            return true;
        }
        return accessory.canUnequip(stack, player);
    }

    private AccessoryRules() {
    }
}
