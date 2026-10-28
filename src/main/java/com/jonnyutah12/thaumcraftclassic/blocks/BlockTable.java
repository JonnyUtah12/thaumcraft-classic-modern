package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileDeconstructionTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);
    public static final int VARIANT_TABLE = 0;
    public static final int VARIANT_DECONSTRUCTION = 14;
    public static final int VARIANT_ARCANE_WORKBENCH = 15;

    /**
     * Research-table direction encoding, same as the original: base variants
     * 2..5 encode the direction toward the top half (ForgeDirection ordinals
     * N, S, W, E); the top half carries 6..9. Only this small encoding is
     * duplicated here for the neighbour glue — the research-table tile class
     * itself belongs to the research card and is not referenced from here.
     */
    private static final Direction[] RESEARCH_DIRECTIONS =
            {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private static Direction topToBaseDirection(final int topVariant) {
        return RESEARCH_DIRECTIONS[topVariant - 6];
    }

    public BlockTable() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .noOcclusion());
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
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

    /**
     * Right-click on a top half (variants 6-9) opens the base's menu — the
     * original searched the four directions for the {@code TileResearchTable}
     * and opened its GUI. Right-clicks on bases (2-5) and plain tables go
     * through {@link BlockDeviceBase}'s MenuProvider seam, which reaches
     * {@code TileResearchTable} and {@code TileDeconstructionTable} alike.
     */
    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        final int variant = variant(state);
        if (variant < 6 || variant > 9) {
            return InteractionResult.PASS;
        }
        final BlockPos basePos = pos.relative(topToBaseDirection(variant));
        if (level.getBlockEntity(basePos) instanceof MenuProvider menuProvider) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(menuProvider, basePos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * Original {@code neighborChanged}: a top half (6-9) whose base is gone
     * dismantles itself back to a plain table and drops its items; a base
     * (2-5) whose top is gone resets to a plain table (the original also
     * reset the top's meta).
     */
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbor,
            final BlockPos fromPos, final boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        final int variant = variant(state);
        if (variant >= 6 && variant <= 9) {
            final BlockPos basePos = pos.relative(topToBaseDirection(variant));
            final BlockState base = level.getBlockState(basePos);
            if (!(base.getBlock() instanceof BlockTable) || variant(base) < 2 || variant(base) > 5) {
                final java.util.List<ItemStack> drops = new java.util.ArrayList<>();
                if (level.getBlockEntity(pos) instanceof net.minecraft.world.WorldlyContainer inv) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        final ItemStack stack = inv.removeItemNoUpdate(i);
                        // A container that has never been loaded may still hand
                        // back a null slot; treat it as empty rather than crash
                        // the dismantle path.
                        if (stack != null && !stack.isEmpty()) {
                            drops.add(stack);
                        }
                    }
                }
                level.removeBlockEntity(pos);
                // one compile fix: the base's withVariant is receiver-form (single int);
                // each of these resets names this block's own meta, as the original did.
                level.setBlockAndUpdate(pos, withVariant(0));
                for (final ItemStack drop : drops) {
                    // one compile fix: Block.popResource is 1.21.1's counterpart to the old popItem
                    Block.popResource(level, pos, drop);
                }
            }
        } else if (variant >= 2 && variant <= 5) {
            final BlockPos topPos = pos.relative(RESEARCH_DIRECTIONS[variant - 2]);
            final BlockState top = level.getBlockState(topPos);
            if (!(top.getBlock() instanceof BlockTable) || variant(top) < 6) {
                level.setBlockAndUpdate(pos, withVariant(0));
                if (top.getBlock() instanceof BlockTable) {
                    level.setBlockAndUpdate(topPos, withVariant(0));
                }
            }
        }
    }
}
