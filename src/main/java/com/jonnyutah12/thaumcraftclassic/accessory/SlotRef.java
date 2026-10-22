package com.jonnyutah12.thaumcraftclassic.accessory;

/**
 * A concrete accessory slot address: slot type plus index within that type
 * (0..capacity-1). Used by equip/unequip flows and the fallback right-click
 * equip helper.
 */
public record SlotRef(AccessorySlot slot, int localIndex) {

    public int fallbackIndex() {
        return slot.fallbackIndex(localIndex);
    }
}
