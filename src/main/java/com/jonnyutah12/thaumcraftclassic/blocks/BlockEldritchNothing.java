package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Block Eldritch Nothing (content-mapping.csv
 * {@code thaumcraft:blockeldritchnothing}) — the two-state eldritch shell: the
 * unbroken cover of an Outer Lands structure and the hollow "nothing" it
 * leaves when the structure is opened.
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockEldritchNothing} — air
 * material (light passes through, the shell is see-through glass-like),
 * hardness 0.2, blast resistance 6,000,000 (the shell outlives everything,
 * including the boss rooms), full cube, with the meta-1 tile entity. Its
 * {@code onNeighborBlockChange} is the structure's self-opening rule: a
 * neighbour that is neither an eldritch block nor eldritch nothing makes the
 * shell expose itself — it plays the break, becomes the hollow variant
 * (meta 1), drops the matching eldritch block (a capstone shell drops a
 * capstone, the shell over a hollow drops the nothing block) and refreshes
 * every air and eldritch cell within 2 cells.</p>
 *
 * <p>Registered as a 2-variant family; the hollow variant carries the
 * {@code tileeldritchnothing} tile entity, exactly where the original created
 * it. The 5-cell render box and the meta-0 sparkle trail are TC-M7-01/02.</p>
 */
public class BlockEldritchNothing extends Block implements VariantDevice, EntityBlock {

    /** The original's two metadata values, verbatim (D-001). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

    public BlockEldritchNothing() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(EldritchRealmValues.NOTHING_HARDNESS, EldritchRealmValues.NOTHING_RESISTANCE)
                .sound(SoundType.GLASS)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, EldritchRealmValues.NOTHING_VARIANT_HIDDEN));
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return DeviceTileFactory.create(state, pos);
    }

    /**
     * The original's {@code getCollidingBoundingBoxes}: the hollow variant
     * (meta 1) has no collision at all; the shell is a full cube.
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return state.getValue(VARIANT) == EldritchRealmValues.NOTHING_VARIANT_EXPOSED
                ? Shapes.empty() : super.getShape(state, level, pos, context);
    }

    /**
     * The original's {@code onNeighborBlockChange}: any neighbour that is not
     * eldritch (block or nothing) exposes the shell.
     */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbor, final BlockPos neighborPos, final boolean isMoving) {
        if (level.isClientSide || state.getValue(VARIANT) != EldritchRealmValues.NOTHING_VARIANT_HIDDEN) {
            return;
        }
        if (isEldritch(neighbor)) {
            return;
        }
        expose(level, pos, state);
    }

    private static boolean isEldritch(final Block block) {
        return block instanceof BlockEldritch || block instanceof BlockEldritchNothing;
    }

    /**
     * The original's exposure: play the break, flip to the hollow variant, drop
     * the matching eldritch block (a capstone shell drops a capstone, the shell
     * over a hollow drops the nothing block), and refresh the air/eldritch
     * cells within 2 cells.
     */
    public static void expose(final Level level, final BlockPos pos, final BlockState state) {
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5F,
                0.6F + level.getRandom().nextFloat() * 0.2F);
        level.setBlock(pos, state.setValue(VARIANT, EldritchRealmValues.NOTHING_VARIANT_EXPOSED), 3);

        popResource(level, pos, dropFor(level, pos));

        refreshNeighbours(level, pos);
    }

    /**
     * The original's drop choice: the cell under the shell decides — under an
     * obelisk column the shell drops the capstone (variant 3), over a hollow
     * it drops the nothing block itself (variant 1), and otherwise the plain
     * eldritch block (variant 0).
     */
    private static ItemStack dropFor(final Level level, final BlockPos pos) {
        final BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof BlockEldritch) {
            final int belowVariant = below.getValue(BlockEldritch.VARIANT);
            if (belowVariant == EldritchRealmValues.ELDRITCH_OBELISK
                    || belowVariant == EldritchRealmValues.ELDRITCH_OBELISK_TOP) {
                final ItemStack cap = new ItemStack(CatalogEldritchRealm.BLOCK_ELDRITCH.get(), 1);
                cap.set(ModDataComponents.ITEM_META.get(), EldritchRealmValues.ELDRITCH_CAP);
                return cap;
            }
        }
        final ItemStack nothing = new ItemStack(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get(), 1);
        nothing.set(ModDataComponents.ITEM_META.get(), EldritchRealmValues.NOTHING_VARIANT_EXPOSED);
        return nothing;
    }

    /** The original's 2-cell refresh of the air and eldritch cells around the shell. */
    private static void refreshNeighbours(final Level level, final BlockPos pos) {
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                for (int c = -2; c <= 2; c++) {
                    final BlockPos cell = pos.offset(a, b, c);
                    final BlockState cellState = level.getBlockState(cell);
                    if (isEldritch(cellState.getBlock()) || cellState.isAir()) {
                        cellState.updateNeighbourShapes(level, cell, 3);
                        level.sendBlockUpdated(cell, cellState, cellState, 3);
                    }
                }
            }
        }
    }
}
