package com.jonnyutah12.thaumcraftclassic.accessory.curios;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryItem;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryProvider;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessoryRules;
import com.jonnyutah12.thaumcraftclassic.accessory.AccessorySlot;
import com.jonnyutah12.thaumcraftclassic.accessory.WornAccessory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Curios-backed {@link AccessoryProvider}: reads and writes the native
 * Curios inventory ({@code CuriosApi.getCuriosInventory}, an
 * {@code EntityCapability}) for our four slot types. Persistence, per-slot
 * sync, join sync, GUI, death drops and the equip/unequip lifecycle
 * callbacks are Curios-native — Curios' own update loop fires the
 * {@code ICurioItem} callbacks exactly once, so this provider deliberately
 * does NOT fire them itself (double-callback guard). {@link #tickWorn} is a
 * no-op for the same reason: Curios' {@code curioTick} drives
 * {@code onWornTick}.
 */
public final class CuriosProvider implements AccessoryProvider {

    public static final String PROVIDER_ID = "curios";

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public boolean canEquip(final Player player, final AccessorySlot slot, final int localIndex,
                           final ItemStack stack) {
        final ICurioStacksHandler handler = handler(player, slot);
        if (handler == null || localIndex < 0 || localIndex >= handler.getSlots()) {
            return false;
        }
        if (!AccessoryRules.canInsert(slot, localIndex, stack, player)) {
            return false;
        }
        return handler.getStacks().getStackInSlot(localIndex).isEmpty();
    }

    @Override
    public boolean equip(final Player player, final AccessorySlot slot, final int localIndex,
                        final ItemStack stack) {
        final ICurioStacksHandler handler = handler(player, slot);
        if (handler == null || localIndex < 0 || localIndex >= handler.getSlots()
                || !AccessoryRules.canInsert(slot, localIndex, stack, player)
                || !handler.getStacks().getStackInSlot(localIndex).isEmpty()) {
            return false;
        }
        // Curios' update loop fires onEquip via the adapter on its next tick.
        inventory(player).ifPresent(inv -> inv.setEquippedCurio(slot.curiosSlotId(), localIndex, stack.copy()));
        return true;
    }

    @Override
    public ItemStack unequip(final Player player, final AccessorySlot slot, final int localIndex) {
        final ICurioStacksHandler handler = handler(player, slot);
        if (handler == null || localIndex < 0 || localIndex >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        final ItemStack worn = handler.getStacks().getStackInSlot(localIndex);
        if (worn.isEmpty() || !AccessoryRules.canRemove(worn, player)) {
            return ItemStack.EMPTY;
        }
        // Curios' update loop fires onUnequip via the adapter on its next tick.
        inventory(player).ifPresent(inv -> inv.setEquippedCurio(slot.curiosSlotId(), localIndex, ItemStack.EMPTY));
        return worn;
    }

    @Override
    public List<WornAccessory> worn(final Player player) {
        final Optional<ICuriosItemHandler> inventory = inventory(player);
        if (inventory.isEmpty()) {
            return List.of();
        }
        final List<WornAccessory> worn = new ArrayList<>();
        final Map<String, ICurioStacksHandler> curios = inventory.get().getCurios();
        for (final AccessorySlot slot : AccessorySlot.values()) {
            final ICurioStacksHandler handler = curios.get(slot.curiosSlotId());
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < handler.getSlots(); i++) {
                final ItemStack stack = handler.getStacks().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    worn.add(new WornAccessory(slot, i, stack));
                }
            }
        }
        return worn;
    }

    @Override
    public void tickWorn(final Player player) {
        // No-op by design: Curios drives curioTick → onWornTick natively.
        // Calling it here would double-tick every worn accessory.
    }

    private static Optional<ICuriosItemHandler> inventory(final Player player) {
        return CuriosApi.getCuriosInventory(player);
    }

    private static ICurioStacksHandler handler(final Player player, final AccessorySlot slot) {
        if (slot == null) {
            return null;
        }
        return inventory(player).flatMap(inv -> inv.getStacksHandler(slot.curiosSlotId())).orElse(null);
    }
}
