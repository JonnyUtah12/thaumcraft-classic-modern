package com.jonnyutah12.thaumcraftclassic.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * TC4 parity: {@code ItemArmor.onArmorTick} — the original drove this from
 * the player tick for every equipped armor piece (server side, e.g.
 * {@code common/items/armor/ItemVoidArmor.java} onArmorTick). 1.21.1
 * removed the armor-slot tick, so {@link WornArmorTicker} restores the
 * driver and only pieces implementing this interface receive it.
 *
 * <p>Used by the void sets' self-repair (TC-M6-07). Kept player-scoped:
 * the original's {@code onArmorTick} was player-only.</p>
 */
public interface WornArmorTick {

    void onWornTick(ItemStack stack, Player wearer);
}
