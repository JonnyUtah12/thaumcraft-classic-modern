package com.jonnyutah12.thaumcraftclassic.items;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockArcaneDoor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Arcane door item (content-mapping.csv {@code thaumcraft:itemarcanedoor}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.ItemArcaneDoor} — places the
 * two-block door as a unit (bottom at the clicked surface, top above when
 * free).</p>
 */
public class ItemArcaneDoor extends BlockItem {

    public ItemArcaneDoor(final BlockArcaneDoor door, final Item.Properties props) {
        super(door, props);
    }

    @Override
    public net.minecraft.world.InteractionResult place(final BlockPlaceContext ctx) {
        final Level level = ctx.getLevel();
        final BlockPos pos = ctx.getClickedPos();
        // one compile fix: 1.21.1 asks the state itself whether it can be replaced
        if (!level.getBlockState(pos).canBeReplaced(ctx)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (!level.isEmptyBlock(pos.above())) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        final BlockArcaneDoor door = (BlockArcaneDoor) getBlock();
        final BlockState bottom = door.defaultBlockState()
                .setValue(BlockArcaneDoor.FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(BlockArcaneDoor.HALF, Half.BOTTOM);
        final BlockState top = door.defaultBlockState()
                .setValue(BlockArcaneDoor.FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(BlockArcaneDoor.HALF, Half.TOP);
        // one compile fix: BlockPlaceContext.canPlace() takes no args; the block-fit
        // check (unobstructed at the clicked position) is BlockItem's canPlace(ctx, state).
        if (!this.canPlace(ctx, bottom)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        level.setBlock(pos, bottom, 11);
        level.setBlock(pos.above(), top, 11);
        if (!level.isClientSide) {
            final ItemStack stack = ctx.getItemInHand();
            if (!ctx.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
            // one compile fix: updateNeighbourForOutputSignal is a Level instance method
            // in 1.21.1, and BlockItem has no playSound helper - mirror vanilla BlockItem.place().
            level.updateNeighbourForOutputSignal(pos, bottom.getBlock());
            final net.minecraft.world.level.block.SoundType soundtype = bottom.getSoundType();
            level.playSound(ctx.getPlayer(), pos, this.getPlaceSound(bottom),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
