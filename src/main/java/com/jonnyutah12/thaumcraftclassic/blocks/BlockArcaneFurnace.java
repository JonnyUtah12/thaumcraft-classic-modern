package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The infernal furnace block (content-mapping.csv
 * {@code thaumcraft:blockarcanefurnace}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockArcaneFurnace}, whose
 * metadata carried the whole 3×3×3 machine: 0 = the base plate, 1-9 = the
 * shell, 10 = the nozzle. This slice registers the two cells that carry
 * behaviour — {@code variant} 0 (base, {@link TileArcaneFurnace}) and
 * {@code variant} 1 (nozzle,
 * {@link com.jonnyutah12.thaumcraftclassic.tiles.TileArcaneFurnaceNozzle}) —
 * and leaves the shell to its own card (TC-M4-06-m: the ten variants, the
 * structure check with its {@code restoreBlocks} rollback, and the bound
 * blaze the original summons when a shell is completed).</p>
 *
 * <p>Properties are the original's: metal-class material, hardness 10,
 * resistance 500, light 13 for the base and the nozzle (the original lights
 * exactly those two metas). Shapes are the original's bounds: the base plate
 * is a quarter block, and the nozzle is a half block against the base it
 * faces. Behavior ported: the base's item suction and burning
 * ({@link TileArcaneFurnace#deviceEntityInside}), and the redstone the
 * original leaked — the original returned weak power on the UP face only,
 * which rides here as a signal of 1 into the block above.</p>
 *
 * <p>The original's {@code randomDisplayTick} smoke (client), the shell
 * metas' drops (nether brick, quartz, and a vanilla furnace for the nozzle)
 * and the block-pick sparkle on right-click are TC-M7-01 / the shell card.</p>
 */
public class BlockArcaneFurnace extends Block implements EntityBlock {

    /** 0 = base plate (original metadata 0), 1 = nozzle (metadata 10). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);
    public static final int VARIANT_BASE = 0;
    public static final int VARIANT_NOZZLE = 1;

    /** Original base bounds: 0.0 → 0.25. */
    private static final VoxelShape BASE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    private static final VoxelShape NOZZLE_WEST = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);
    private static final VoxelShape NOZZLE_EAST = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NOZZLE_NORTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    private static final VoxelShape NOZZLE_SOUTH = Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);

    public BlockArcaneFurnace() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .noOcclusion()
                .strength(10.0F, 500.0F)
                .lightLevel(state -> 13));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, VARIANT_BASE));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * Original {@code setBlockBoundsBasedOnState}: the nozzle is a half block
     * on the side of the base it is attached to (its metadata 10 is paired
     * with a metadata 0 neighbour), the base is the quarter-height plate.
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (state.getValue(VARIANT) == VARIANT_BASE) {
            return BASE_SHAPE;
        }
        if (isBaseAt(level, pos.west())) {
            return NOZZLE_WEST;
        }
        if (isBaseAt(level, pos.east())) {
            return NOZZLE_EAST;
        }
        if (isBaseAt(level, pos.north())) {
            return NOZZLE_NORTH;
        }
        if (isBaseAt(level, pos.south())) {
            return NOZZLE_SOUTH;
        }
        return FULL_SHAPE;
    }

    private static final VoxelShape FULL_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    private static boolean isBaseAt(final BlockGetter level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.is(CatalogEssentiaStorage.BLOCK_ARCANE_FURNACE.get())
                && state.getValue(VARIANT) == VARIANT_BASE;
    }

    /**
     * Original {@code isSideSolid}: the base plate is not solid, the nozzle
     * is. 1.21.1 has no {@code isSolidRender} hook on this base class, so the
     * nozzle keeps the block's own occlusion (it is registered with
     * {@code noOcclusion}, matching the base plate's see-through plate).
     */

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(VARIANT) == VARIANT_NOZZLE
                ? CatalogEssentiaStorage.createFurnaceNozzle(pos, state)
                : CatalogEssentiaStorage.createFurnace(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof BlockDeviceBase.TickingDeviceTile tile) {
                tile.serverTick();
            }
        };
    }

    /** Original {@code onEntityCollidedWithBlock} (metadata 0 only). */
    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (state.getValue(VARIANT) != VARIANT_BASE || level.isClientSide) {
            return;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileArcaneFurnace furnace) {
            furnace.deviceEntityInside(entity);
        }
    }

    /**
     * Original {@code isProvidingWeakPower}: true on the UP face only — a
     * signal of 1 into the block above the furnace.
     */
    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction direction) {
        return direction == Direction.UP ? 1 : 0;
    }

    /**
     * The alchemical furnace screen (original {@code gui_id} 9, TC-M7-04-m1):
     * a right-click on the base plate opens its tile's menu — the
     * {@link MenuProvider} seam the crate's furniture blocks ship
     * (BlockMagicBox:50). The original opened the same window from
     * {@code TileAlchemyFurnace}, the body this tile models; the nozzle (the
     * original's metadata 10) has no window, so the seam exists on the base
     * variant only.
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level,
                                               final BlockPos pos, final Player player,
                                               final BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
