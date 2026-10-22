package com.jonnyutah12.thaumcraftclassic.accessory;

import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Provider SPI for the accessory layer (TC-M8-01, D-005). Exactly one
 * implementation is active at runtime: the Curios bridge when Curios is
 * loaded, otherwise the built-in fallback. Core gameplay code goes through
 * {@link AccessoryService}, never through a provider directly.
 *
 * <p>Contracts every provider must satisfy (Baubles spec, verified by
 * AccessoryGameTest under both configurations):</p>
 * <ul>
 *   <li>server authority — mutations only on the server;</li>
 *   <li>{@link #canEquip} implements the full insertion check
 *       (routing + capacity + occupancy + item veto) without mutating;</li>
 *   <li>{@link #equip}/{@link #unequip} fire the {@link AccessoryItem}
 *       lifecycle callbacks exactly once per insert/remove — synchronously
 *       in the fallback, via Curios' own update loop under the bridge (the
 *       bridge must not fire them itself or every callback would double);</li>
 *   <li>{@link #worn} reflects the current worn set for effect queries;</li>
 *   <li>{@link #tickWorn} drives {@code onWornTick} exactly once per worn
 *       stack per tick — except the Curios bridge, where Curios' native
 *       {@code curioTick} drives it and this is a no-op (double-tick
 *       guard).</li>
 * </ul>
 */
public interface AccessoryProvider {

    /** Stable identifier: "curios" or "fallback". */
    String id();

    /** Full insertion check without mutation (see class contract). */
    boolean canEquip(Player player, AccessorySlot slot, int localIndex, ItemStack stack);

    /**
     * Places {@code stack} into the given slot (caller owns removing it from
     * wherever it came from). Returns true and fires {@code onEquipped} on
     * success; false when {@link #canEquip} fails.
     */
    boolean equip(Player player, AccessorySlot slot, int localIndex, ItemStack stack);

    /**
     * Removes the stack at the given slot, firing the {@code canUnequip}
     * veto and {@code onUnequipped}. Returns the removed stack, or EMPTY
     * when the slot was empty or removal was vetoed.
     */
    ItemStack unequip(Player player, AccessorySlot slot, int localIndex);

    /** Current worn set, ordered by slot then index. Never contains empties. */
    List<WornAccessory> worn(Player player);

    /** Drives {@code onWornTick} for the player's worn stacks (server only). */
    void tickWorn(Player player);
}
