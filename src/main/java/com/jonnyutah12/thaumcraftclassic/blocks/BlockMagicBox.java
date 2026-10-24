package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileMagicBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * Magic box (content-mapping.csv {@code thaumcraft:blockmagicbox},
 * GUI_MAGIC_BOX).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockMagicBox} — a plain
 * block; all inventory/adjacency logic lives in {@link TileMagicBox}.
 * Right-click opens the magic-box menu.</p>
 */
public class BlockMagicBox extends Block {

    public BlockMagicBox() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TileMagicBox(pos, state);
    }

    @Override
    public boolean useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (level.isClientSide) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof TileMagicBox box) {
            player.openMenu(box, pos);
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
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
            if (be instanceof TileMagicBox box) {
                Containers.dropContents(level, pos, box);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
