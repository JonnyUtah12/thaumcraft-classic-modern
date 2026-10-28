package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileChestHungry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * Hungry chest (content-mapping.csv {@code thaumcraft:blockchesthungry},
 * GUI_CHEST_HUNGRY).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockChestHungry}. A chest
 * that sucks up dropped items landing on it (entityInside →
 * {@link TileChestHungry#absorb}) — in 4.2.3.5 it never feeds the player
 * (card-body mismatch noted in the handoff). The GUI is the vanilla chest
 * menu (the original gui row has no custom handler).</p>
 */
public class BlockChestHungry extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockChestHungry() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(final net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TileChestHungry(pos, state);
    }

    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity item
                && level.getBlockEntity(pos) instanceof TileChestHungry chest) {
            chest.absorb(item);
        }
    }

    /** One compile fix: 1.21.1's use seam returns InteractionResult (the old
     * boolean contract no longer exists); true maps to CONSUME, false to PASS. */
    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(final BlockState state, final Level level,
            final BlockPos pos, final Player player, final net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof TileChestHungry chest) {
            player.openMenu(chest, pos);
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof BlockDeviceBase.TickingDeviceTile tile) {
                tile.serverTick();
            }
        };
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            final BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileChestHungry chest) {
                Containers.dropContents(level, pos, chest);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
