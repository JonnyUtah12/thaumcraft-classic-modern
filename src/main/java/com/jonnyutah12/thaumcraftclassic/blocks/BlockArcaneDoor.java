package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneDoor;
import com.jonnyutah12.thaumcraftclassic.tiles.TileOwned;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * Arcane (warding) door (content-mapping.csv {@code thaumcraft:blockarcanedoor}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockArcaneDoor} — a two-block
 * tall wooden door; metadata 0-3 bottom facing, 4-7 top, bit 8 open. The
 * bottom half owns the tile ({@link TileArcaneDoor extends TileOwned});
 * opening requires the placer or someone on the access list. The 1.21.1
 * state carries the same information in FACING/OPEN/HALF properties.</p>
 */
public class BlockArcaneDoor extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Half> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public BlockArcaneDoor() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(5.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(HALF, Half.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HALF);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(HALF) == Half.BOTTOM ? new TileArcaneDoor(pos, state) : null;
    }

    /** Original onBlockPlacedBy: the bottom half stores the owner. */
    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            @Nullable final LivingEntity placer, final ItemStack stack) {
        if (placer instanceof Player player && state.getValue(HALF) == Half.BOTTOM
                && level.getBlockEntity(pos) instanceof TileOwned owned) {
            owned.owner = player.getGameProfile().getName();
            owned.setChanged();
        }
    }

    @Override
    public boolean useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (level.isClientSide) {
            return true;
        }
        final BlockPos bottom = state.getValue(HALF) == Half.TOP ? pos.below() : pos;
        final BlockState bottomState = level.getBlockState(bottom);
        if (!(bottomState.getBlock() instanceof BlockArcaneDoor)) {
            return false;
        }
        final BlockEntity be = level.getBlockEntity(bottom);
        if (be instanceof TileOwned owned && !owned.hasAccess(player.getGameProfile().getName())
                && !player.isCreative()) {
            return false;
        }
        final boolean open = bottomState.getValue(OPEN);
        for (final BlockPos p : new BlockPos[]{bottom, bottom.above()}) {
            final BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof BlockArcaneDoor) {
                level.setBlock(p, s.setValue(OPEN, !open), 2);
            }
        }
        level.playSound(null, bottom, net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // Break the whole door when either half goes (original behaviour).
            final BlockPos other = state.getValue(HALF) == Half.BOTTOM ? pos.above() : pos.below();
            if (level.getBlockState(other).getBlock() instanceof BlockArcaneDoor) {
                level.removeBlock(other, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
