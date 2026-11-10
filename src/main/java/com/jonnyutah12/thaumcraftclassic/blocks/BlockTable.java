package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * Table family (content-mapping.csv {@code thaumcraft:blocktable}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockTable}, one id with
 * metadata 0-15: 0/1 plain table (orientation), 2-5 research-table parts
 * (TC-M2 research card), 6-13 table parts around a research table, 14
 * deconstruction table, 15 arcane workbench (crafting card). The block is
 * owned by this card; tiles are supplied per variant through
 * {@link DeviceTileFactory} (TileTable for 0/1/6-13, TileDeconstructionTable
 * for 14) — the research-table and workbench tiles register their suppliers
 * from their own cards.</p>
 *
 * <p>Deferred: the wand-right-click conversion of a plain table into an
 * arcane workbench (original {@code onWandRightClick}) needs the wand item
 * (TC-M6-01); the research-table auto-placement glue (original
 * neighborChanged for metas 2-9) lands with the research card.</p>
 */
public class BlockTable extends BlockDeviceBase {

    public static final int MAX_VARIANT = 15;
    public static final int VARIANT_TABLE = 0;
    public static final int VARIANT_DECONSTRUCTION = 14;
    public static final int VARIANT_ARCANE_WORKBENCH = 15;

    public BlockTable() {
        super(MAX_VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .noOcclusion());
    }

    /** Original onBlockPlaced: plain tables get orientation 0/1 from yaw. */
    @Override
    public BlockState getStateForPlacement(final net.minecraft.world.item.context.BlockPlaceContext ctx) {
        final int variant = variant(ctx.getLevel().getBlockState(ctx.getClickedPos()));
        if (variant < 14) {
            final int facing = Math.round((ctx.getRotation() - 180.0F) / 90.0F) & 1;
            return withVariant(facing);
        }
        return withVariant(variant);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            final BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof net.minecraft.world.WorldlyContainer inv) {
                Containers.dropContents(level, pos, inv);
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Right-click opening is NOT overridden here: BlockDeviceBase.useWithoutItem
    // already opens any MenuProvider tile (the deconstruction table and the
    // arcane workbench both are), and plain tables have no menu provider, so
    // the original's "md <= 1 returns false" branch falls out of the generic
    // seam unchanged.
}
