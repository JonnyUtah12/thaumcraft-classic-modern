package com.jonnyutah12.thaumcraftclassic.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The wand seam of the port: a block entity (the original also allowed a
 * {@code Block} to answer) that reacts to a right-click with a wand — the
 * tube family's face configuration (TC-M4-07-f) is what lands on it first.
 *
 * <p>Original: {@code thaumcraft.api.wands.IWandable}, dispatched from
 * {@code ItemWandCasting.onItemUseFirst}: the block first, then the block
 * entity, and the item treats the click as handled when the return value is
 * {@code >= 0} (it returns {@code ret == 1}). The tube tiles answer
 * {@code 0}: the face was configured, and in 1.7.10 the wand's own
 * right-click then ran as well.</p>
 *
 * <p>1.21.1 realization: the engine hands the held stack to
 * {@code BlockBehaviour.useItemOn} for <em>any</em> item (verified in
 * {@code ServerPlayerGameMode.useItemOn}), which is where
 * {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockTube} picks up the
 * wand and dispatches here. The port consumes the click instead of reporting
 * it unhandled: a face toggle must not also fire an equipped focus and spend
 * vis, which is what the original's "return 0, keep going" did.</p>
 */
public interface IWandable {

    /**
     * Original {@code onWandRightClick(World, ItemStack, EntityPlayer, int x,
     * int y, int z, int side, int md)}: the right-click on this tile.
     *
     * @param side  the face the click landed on, the original's {@code side}
     * @param md    the block's metadata, the original's {@code md} — for the
     *              port that is the {@code variant} property value
     * @return the original's contract: {@code 0} handled-and-continue,
     *         {@code 1} handled-and-consumed, {@code -1} not handled
     */
    int onWandRightClick(Level level, ItemStack wandStack, Player player, BlockPos pos, Direction side, int md);

    /**
     * Original {@code onWandRightClick(World, ItemStack, EntityPlayer)}: the
     * right-click <em>while the wand is being drawn</em>; the returned stack
     * replaces the wand. Every ported implementer returns null, as the
     * original's tube family does.
     */
    @Nullable
    ItemStack onWandRightClick(Level level, ItemStack wandStack, Player player);

    /** Original {@code onUsingWandTick}: empty for the tube family. */
    void onUsingWandTick(ItemStack wandStack, Player player, int count);

    /** Original {@code onWandStoppedUsing}: empty for the tube family. */
    void onWandStoppedUsing(ItemStack wandStack, Level level, Player player, int count);
}
