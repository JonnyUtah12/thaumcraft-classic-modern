package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import com.jonnyutah12.thaumcraftclassic.taint.TaintRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;

/**
 * Taint fibres / grass / spore-stalk block (content-mapping.csv row
 * {@code thaumcraft:blockTaintFibres}, impl_class=BlockTaintFibres; the
 * conversion rule it owns lives in {@link TaintManager#fibresTick}).
 *
 * <p>Original {@code thaumcraft.common.blocks.BlockTaintFibres}: one id,
 * metadata 0 fibres, 1 grass-1, 2 grass-2, 3 spore stalk, 4 mature spore
 * stalk, carried verbatim in a {@code variant} property (D-001). Original
 * properties: hardness 5.0 ({@code func_149752_b(5.0F)}), resistance 1.0
 * ({@code func_149711_c(1.0F)}), light 8 for grass-2 (meta 2) and 10 for the mature
 * stalk (meta 4) from {@code getLightValue}, and no collision box
 * ({@code getCollisionBoundingBoxFromPool} returns null, {@code isSideSolid}
 * returns false).</p>
 *
 * <p>Behaviour ported: the spread/conversion tick ({@code func_149674_a},
 * lines 129-172 — the 2/3 crust and 3/4 soil conversion table, the fibre seed
 * ladder, and the spore-stalk ladder), the "dies when it only touches taint"
 * neighbour rule ({@code func_149695_a}, lines 121-127 →
 * {@link TaintManager#fibresNeighborCheck}), the taint-biome placement gate
 * ({@code func_149742_c} → {@link #canSurvive}), the collision poison
 * ({@code func_149670_a}: player 1/1000 for 80 ticks, other living 1/500 for
 * 160, all metas), the roots cue on a redstone pulse ({@code func_149696_a}
 * event 1), and the per-meta selection box of {@code setBlockBoundsBasedOnState}
 * (a 1/16 plate against the first solid neighbour face for fibres, a
 * 0.2-0.8 box for the grass and stalks).</p>
 *
 * <p>Deferred on owning rows: the mature stalk spawns the {@code EntityTaintSpore}
 * (TC-M6-06) through {@link TaintManager}'s guarded seam, and the stalk's
 * spore-puff particles are the TC-M5-07 asset row. The original drops nothing
 * for this block ({@code getItemDropped} → null), which is why
 * {@code loot_table/blocks/blocktaintfibres.json} declares no pools.</p>
 */
public class BlockTaintFibres extends Block implements VariantDevice {

    /** Original blockTaintFibres metadata, verbatim (D-001). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);

    public static final int META_FIBRES = 0;
    public static final int META_GRASS_1 = 1;
    public static final int META_GRASS_2 = 2;
    public static final int META_SPORE_STALK = 3;
    public static final int META_MATURE_SPORE_STALK = 4;

    /** The original's 0.2..0.8 box for the grass and stalk metas. */
    private static final VoxelShape BUSH = box(3.2, 0.0, 3.2, 12.8, 12.8, 12.8);
    /** The original's 1/16-thick plate metas, one per neighbour face. */
    private static final VoxelShape PLATE_DOWN = box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    private static final VoxelShape PLATE_UP = box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape PLATE_NORTH = box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape PLATE_SOUTH = box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final VoxelShape PLATE_WEST = box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
    private static final VoxelShape PLATE_EAST = box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public BlockTaintFibres() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F, 1.0F)
                .sound(SoundType.ROOTS)
                .noOcclusion()
                // See BlockTaint: the fibres conversion ladder is the original's
                // func_149674_a on the chunk-tick queue, so the state has to be
                // marked random-ticking for 1.21.1 to call randomTick at all.
                .randomTicks()
                .lightLevel(state -> switch (state.getValue(VARIANT)) {
                    case META_GRASS_2 -> 8;
                    case META_MATURE_SPORE_STALK -> 10;
                    default -> 0;
                }));
        registerDefaultState(stateDefinition.any().setValue(VARIANT, META_FIBRES));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public int variant(final BlockState state) {
        return state.getValue(VARIANT);
    }

    /** The tick engine, including the conversion table (original lines 129-172). */
    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                              final RandomSource rand) {
        TaintManager.fibresTick(level, pos, state, rand);
    }

    /** Original {@code func_149695_a}: the fibres die when only taint touches them. */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighborBlock, final BlockPos neighborPos,
                                   final boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockState(pos).getBlock() == this) {
            TaintManager.fibresNeighborCheck((ServerLevel) level, pos);
        }
        if (level.isClientSide && level.hasSignal(pos, Direction.UP)) {
            level.playLocalSound(pos, ModSounds.ROOTS.get(), SoundSource.BLOCKS,
                    TaintRules.ROOTS_VOLUME,
                    TaintRules.ROOTS_PITCH_BASE
                            + level.getRandom().nextFloat() * TaintRules.ROOTS_PITCH_SPREAD, false);
        }
    }

    /**
     * Original {@code func_149670_a} (onEntityCollidedWithBlock): the fibres
     * poison every meta, on much longer odds than the crust.
     */
    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos,
                                final Entity entity) {
        TaintManager.stepEffect(level, entity, true);
    }

    /**
     * Original {@code func_149742_c} (canPlaceBlockAt): the fibres only place
     * inside the Tainted Land biome. The biome tag is read through
     * {@link TaintManager#canPlaceFibresInBiome}.
     */
    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return TaintManager.canPlaceFibresInBiome(level, pos);
    }

    /**
     * Original {@code func_149719_a} (setBlockBoundsBasedOnState): fibres are a
     * 1/16 plate against the first solid neighbour face (bottom plate when
     * nothing is solid), the grass and stalks are the 0.2-0.8 box.
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (state.getValue(VARIANT) != META_FIBRES) {
            return BUSH;
        }
        // ForgeDirection ordinals and Direction.values() share the
        // DOWN, UP, NORTH, SOUTH, WEST, EAST order the original looped.
        for (final Direction side : Direction.values()) {
            final BlockPos np = pos.relative(side);
            if (level.getBlockState(np).isFaceSturdy(level, np, side.getOpposite())) {
                return switch (side) {
                    case DOWN -> PLATE_DOWN;
                    case UP -> PLATE_UP;
                    case NORTH -> PLATE_NORTH;
                    case SOUTH -> PLATE_SOUTH;
                    case WEST -> PLATE_WEST;
                    case EAST -> PLATE_EAST;
                };
            }
        }
        return PLATE_DOWN;
    }

    /** Original {@code func_149668_a} / {@code isSideSolid}: no collision, no solid sides. */
    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level,
                                           final BlockPos pos, final CollisionContext context) {
        return Shapes.empty();
    }
}
