package com.jonnyutah12.thaumcraftclassic.casting;

import com.jonnyutah12.thaumcraftclassic.api.foci.Focus;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Everything one wand cast needs (TC-M6-02), handed to a
 * {@link FocusBehavior}. Ports the argument set the original passed to
 * {@code ItemFocusBasic.onFocusRightClick}
 * ({@code wandstack, world, player, movingobjectposition}) plus the pieces the
 * wand already resolved (the focus, its price and the resolved costs), so a
 * behaviour never re-derives them and can never disagree with what was paid.
 *
 * @param player      the caster (the original's {@code EntityPlayer})
 * @param level       the level the cast happens in; a cast only mutates state
 *                    when {@link #serverSide()} — the original split the same
 *                    way on {@code world.field_72995_K}
 * @param hand        the hand the wand is held in
 * @param wandStack   the caster's wand stack (the original's {@code wandstack})
 * @param wand        the wand item, already resolved
 * @param focus       the focus's table entry
 * @param focusStack  the focus stack the wand carries, upgrades included
 * @param baseCost    the focus's declared price ({@code ItemFocus.getVisCost})
 * @param cost        the price after the TC-M4-02 chain (what was checked and paid)
 */
public record CastContext(Player player, Level level, InteractionHand hand, ItemStack wandStack,
                          ItemWandCasting wand, Focus focus, ItemStack focusStack,
                          AspectList baseCost, AspectList cost) {

    /** True when world mutation is allowed — the original's {@code !world.isRemote} gate. */
    public boolean serverSide() {
        return !level.isClientSide;
    }
}
