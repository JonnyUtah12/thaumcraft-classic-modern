package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Built-in accessory backend (D-005 fallback): the 5-slot layout lives in the
 * {@link AccessoryAttachments#ACCESSORIES} player attachment. Always
 * loadable, references no accessory mod, and is inert whenever the Curios
 * bridge is active (exactly-one-provider rule, see {@link AccessoryService}).
 *
 * <p>Equipping without Curios is right-click driven
 * ({@link AccessoryLifecycle}); there is deliberately no fallback GUI —
 * the fallback is a compatibility shim, not a second framework.</p>
 */
public final class FallbackProvider implements AccessoryProvider {

    public static final String PROVIDER_ID = "fallback";

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public boolean canEquip(final Player player, final AccessorySlot slot, final int localIndex,
                           final ItemStack stack) {
        if (slot == null || !AccessoryRules.canInsert(slot, localIndex, stack, player)) {
            return false;
        }
        final int index = slot.fallbackIndex(localIndex);
        return index >= 0 && data(player).get(index).isEmpty();
    }

    @Override
    public boolean equip(final Player player, final AccessorySlot slot, final int localIndex,
                        final ItemStack stack) {
        if (!canEquip(player, slot, localIndex, stack)) {
            return false;
        }
        player.setData(AccessoryAttachments.ACCESSORIES,
                data(player).with(slot.fallbackIndex(localIndex), stack.copy()));
        ((AccessoryItem) stack.getItem()).onEquipped(stack, player);
        return true;
    }

    @Override
    public ItemStack unequip(final Player player, final AccessorySlot slot, final int localIndex) {
        if (slot == null) {
            return ItemStack.EMPTY;
        }
        final int index = slot.fallbackIndex(localIndex);
        final ItemStack worn = data(player).get(index);
        if (worn.isEmpty() || !AccessoryRules.canRemove(worn, player)) {
            return ItemStack.EMPTY;
        }
        player.setData(AccessoryAttachments.ACCESSORIES, data(player).with(index, ItemStack.EMPTY));
        ((AccessoryItem) worn.getItem()).onUnequipped(worn, player);
        return worn;
    }

    @Override
    public List<WornAccessory> worn(final Player player) {
        final AccessoryData data = data(player);
        final List<WornAccessory> worn = new ArrayList<>();
        for (int index = 0; index < AccessoryData.SIZE; index++) {
            final ItemStack stack = data.get(index);
            if (!stack.isEmpty()) {
                final AccessorySlot slot = AccessorySlot.byFallbackIndex(index);
                worn.add(new WornAccessory(slot, index - slot.fallbackIndex(0), stack));
            }
        }
        return worn;
    }

    @Override
    public void tickWorn(final Player player) {
        for (final WornAccessory worn : worn(player)) {
            if (worn.stack().getItem() instanceof AccessoryItem accessory) {
                accessory.onWornTick(worn.stack(), player);
            }
        }
    }

    /** First valid empty slot for the stack, or null when none. */
    public SlotRef firstFreeSlot(final Player player, final ItemStack stack) {
        for (final AccessorySlot slot : AccessorySlot.values()) {
            for (int i = 0; i < slot.capacity(); i++) {
                if (canEquip(player, slot, i, stack)) {
                    return new SlotRef(slot, i);
                }
            }
        }
        return null;
    }

    private static AccessoryData data(final Player player) {
        return player.getData(AccessoryAttachments.ACCESSORIES);
    }
}
