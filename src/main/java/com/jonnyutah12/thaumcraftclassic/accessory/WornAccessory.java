package com.jonnyutah12.thaumcraftclassic.accessory;

import net.minecraft.world.item.ItemStack;

/**
 * One worn accessory as reported by the active {@link AccessoryProvider}.
 * Effects (TC-M8-02 vis discount, TC-M6-07 runic shielding, ...) consume
 * these through {@link AccessoryService}; they never touch provider or Curios
 * types.
 *
 * @param slot  the logical slot type
 * @param index index within that slot type (0..capacity-1)
 * @param stack the worn stack (never empty)
 */
public record WornAccessory(AccessorySlot slot, int index, ItemStack stack) {
}
