package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.EntityBlock;

import javax.annotation.Nullable;

/**
 * Shared base for the multi-variant device blocks owned by TC-M6-04c
 * (blockStoneDevice / blockMetalDevice / blockWoodenDevice / blockArcaneFurnace
 * / blockAlchemyFurnace / blockAiry).
 *
 * <p>The original 1.7.10 blocks stored the sub-kind in block metadata; 1.21.1
 * has no block metadata, so each device block carries the original meta value
 * verbatim in an {@code variant} {@link IntegerProperty} (same convention as
 * {@link BlockCustomOre}, D-001: ids and meta values unchanged, only the
 * storage mechanism). The meta-to-tile mapping lives in
 * {@link DeviceTileFactory} so feature cards can supply their tiles without
 * editing these block classes (docs/catalog-triage.md hotspot rule).</p>
 */
public abstract class BlockDeviceBase extends Block implements VariantDevice, EntityBlock {

    private final IntegerProperty variantProperty;

    protected BlockDeviceBase(final int maxVariant, final BlockBehaviour.Properties props) {
        super(props);
        this.variantProperty = IntegerProperty.create("variant", 0, maxVariant);
        registerDefaultState(stateDefinition.any().setValue(variantProperty, 0));
    }

    /** The original-metadata range carried by this block's states. */
    public final IntegerProperty variantProperty() {
        return variantProperty;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(variantProperty);
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(variantProperty);
    }

    /** The state for one original-metadata value (worldgen, machines, tests). */
    public BlockState withVariant(final int variant) {
        return defaultBlockState().setValue(variantProperty, variant);
    }

    /**
     * The device blocks host a block entity only for the variants that have a
     * registered tile supplier; every other variant has none (matching the
     * original's {@code createTileEntity} returning null).
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return DeviceTileFactory.create(state, pos);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final net.minecraft.world.level.Level level, final BlockState state,
            final BlockEntityType<T> type) {
        // Ticking device tiles drive their own serverTick() through the
        // generic ticker below; the tile classes mirror the original's
        // canUpdate() and no-op when they never tick.
        return level.isClientSide ? null
                : (lvl, pos, st, be) -> {
                    if (be instanceof TickingDeviceTile tile) {
                        tile.serverTick();
                    }
                };
    }

    /** Implemented by device tiles that tick on the server (original canUpdate). */
    public interface TickingDeviceTile {
        void serverTick();
    }

    /**
     * Implemented by device tiles that handle an empty-hand use (the shared
     * equivalent of the original's per-block onBlockActivated/right-click
     * branches; TC-M4-04 runic matrix activation).
     */
    public interface DeviceUseTile {
        net.minecraft.world.InteractionResult useWithoutItem(
                net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                net.minecraft.server.level.ServerPlayer player);
    }

    /**
     * Implemented by device tiles that receive entities intersecting the
     * block (the shared equivalent of the original's per-block
     * {@code onEntityCollidedWithBlock} branches; TC-M4-05 crucible item
     * absorption).
     */
    public interface DeviceEntityInsideTile {
        void deviceEntityInside(net.minecraft.world.entity.Entity entity);
    }

    /**
     * Implemented by device tiles that handle a held-item right-click
     * (the shared equivalent of the original's per-block
     * {@code onBlockActivated} fluid-container branches; TC-M4-05 crucible
     * bucket fill).
     */
    public interface DeviceItemUseTile {
        net.minecraft.world.ItemInteractionResult useWithItem(
                net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                net.minecraft.server.level.ServerPlayer player,
                net.minecraft.world.InteractionHand hand);
    }

    /**
     * Implemented by device tiles that provide comparator power (the shared
     * equivalent of the original's per-block {@code isProvidingComparatorPower}
     * / {@code getComparatorInputSignal}; TC-M4-05 crucible vis level).
     */
    public interface DeviceAnalogTile {
        int getComparatorSignal();
    }

    /**
     * Implemented by device tiles that react to a neighbouring block change
     * (the shared equivalent of the original's per-block
     * {@code neighborChanged} branches; TC-M4-05 crucible bellows rescan).
     */
    public interface DeviceNeighborTile {
        void deviceNeighborChanged();
    }

    /**
     * Implemented by device tiles that emit ambient client particles/sounds
     * (the shared equivalent of the original's per-block
     * {@code randomDisplayTick}; TC-M4-05 crucible boiling lavapop).
     */
    public interface DeviceDisplayTickTile {
        void deviceDisplayTick(net.minecraft.util.RandomSource rand);
    }

    /**
     * Generic menu seam (TC-M6-04d): a device block opens its tile's menu
     * when the block entity is a {@link net.minecraft.world.MenuProvider} — the
     * shared equivalent of the original's per-block openGui calls, so
     * feature cards never edit the device block classes (hotspot rule).
     */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            final BlockState state, final net.minecraft.world.level.Level level,
            final BlockPos pos, final net.minecraft.world.entity.player.Player player,
            final net.minecraft.world.phys.BlockHitResult hitResult) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DeviceUseTile use && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            final net.minecraft.world.InteractionResult result = use.useWithoutItem(
                    (net.minecraft.server.level.ServerLevel) level, pos, sp);
            if (result.consumesAction()) {
                return result;
            }
        }
        if (be instanceof net.minecraft.world.MenuProvider provider) {
            player.openMenu(provider, pos);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected void entityInside(final BlockState state, final net.minecraft.world.level.Level level,
                                final BlockPos pos, final net.minecraft.world.entity.Entity entity) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DeviceEntityInsideTile tile) {
            tile.deviceEntityInside(entity);
        }
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(
            final net.minecraft.world.item.ItemStack stack, final BlockState state,
            final net.minecraft.world.level.Level level, final BlockPos pos,
            final net.minecraft.world.entity.player.Player player, final net.minecraft.world.InteractionHand hand,
            final net.minecraft.world.phys.BlockHitResult hitResult) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DeviceItemUseTile use && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            return use.useWithItem((net.minecraft.server.level.ServerLevel) level, pos, sp, hand);
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final net.minecraft.world.level.Level level,
                                        final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        return be instanceof DeviceAnalogTile tile ? tile.getComparatorSignal() : 0;
    }

    @Override
    protected void neighborChanged(final BlockState state, final net.minecraft.world.level.Level level,
                                   final BlockPos pos, final net.minecraft.world.level.block.Block neighborBlock,
                                   final BlockPos updatedPos, final boolean movedByPiston) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DeviceNeighborTile tile) {
            tile.deviceNeighborChanged();
        }
    }

    @Override
    protected void randomDisplayTick(final BlockState state, final net.minecraft.world.level.Level level,
                                     final BlockPos pos, final net.minecraft.util.RandomSource rand) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DeviceDisplayTickTile tile) {
            tile.deviceDisplayTick(rand);
        }
    }
}
