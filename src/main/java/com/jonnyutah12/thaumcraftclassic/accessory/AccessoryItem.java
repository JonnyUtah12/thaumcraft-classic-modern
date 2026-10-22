package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.Set;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Item-side accessory behavior — the port of {@code baubles.api.IBauble}
 * (Thaumcraft API repo, MIT, adapted per docs/licensing-and-attribution.md).
 *
 * <p>Method-for-method equivalent of the original (getBaubleType →
 * {@link #applicableSlots(ItemStack)}, onWornTick/onEquipped/onUnequipped/
 * canEquip/canUnequip kept verbatim with {@code EntityLivingBase} →
 * {@code Player}, matching TC4 usage where baubles are only ever worn by
 * players). Content milestones (TC-M8-02, TC-M6-07) implement this on their
 * item classes; the accessory providers and the Curios bridge drive it, so
 * item code never references Curios types.</p>
 *
 * <p>Lifecycle contract (Baubles spec, enforced by both providers):
 * server-authoritative; {@code onEquipped}/{@code onUnequipped} fire exactly
 * once per insert/removal; {@code onWornTick} fires once per game tick per
 * worn stack on the server; {@code canEquip}/{@code canUnequip} are item-side
 * vetoes checked before any mutation. Death drops do NOT call
 * {@code onUnequipped} — matching {@code InventoryBaubles}, which drops
 * without lifecycle callbacks on death.</p>
 */
public interface AccessoryItem {

    /**
     * Which slot types this item may occupy. The original returned a single
     * {@code BaubleType}; TC4's {@code BaubleType.ALL} handling in
     * {@code InventoryBaubles.isValidItem} accepted any slot, which this set
     * models directly. An empty set means the item cannot be worn.
     */
    Set<AccessorySlot> applicableSlots(ItemStack stack);

    /** TC4 parity: {@code IBauble.onWornTick} — once per tick while worn (server). */
    default void onWornTick(final ItemStack stack, final Player wearer) {
    }

    /** TC4 parity: {@code IBauble.onEquipped}. */
    default void onEquipped(final ItemStack stack, final Player player) {
    }

    /** TC4 parity: {@code IBauble.onUnequipped}. */
    default void onUnequipped(final ItemStack stack, final Player player) {
    }

    /** TC4 parity: {@code IBauble.canEquip} — item-side veto on insertion. */
    default boolean canEquip(final ItemStack stack, final Player player) {
        return true;
    }

    /** TC4 parity: {@code IBauble.canUnequip} — item-side veto on removal. */
    default boolean canUnequip(final ItemStack stack, final Player player) {
        return true;
    }
}
