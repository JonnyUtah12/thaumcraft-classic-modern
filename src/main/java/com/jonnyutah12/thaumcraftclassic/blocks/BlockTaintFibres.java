package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.taint.TaintManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fibrous Taint / Tainted Grass / Tainted Plant / Spore Stalk / Mature Spore
 * Stalk (content-mapping.csv row {@code thaumcraft:blocktaintfibres},
 * original {@code BlockTaintFibres}).
 *
 * <p>Original metadata 0-4 ride a {@code variant} property verbatim (D-001).
 * Physical values are the original's: hardness 1.0 / resistance 5.0, no
 * collision (original {@code getCollisionBox} returned null), the
 * gore/roots sound set, and light level 8 for meta 2 and 10 for meta 4
 * ({@code getLightValue}) via per-state {@code lightEmissive}.</p>
 *
 * <p>Meta 0 renders as a thin plate against the first solid neighbour face
 * (original {@code setBlockBoundsBasedOnState}); metas 1-4 use the 0.2..0.8
 * box. Drops: none, matching the original's null {@code getItemDropped}.</p>
 *
 * <p>Placement gate deviation: the original blocked placing fibres outside
 * the taint biome ({@code canPlaceBlockAt}); the port lets placement through
 * and the tick rule removes them again, which is the same observable outcome
 * one tick later (docs/architecture/taint.md).</p>
 */
public class BlockTaintFibres extends Block {

    /** Original blockTaintFibres metadata, verbatim 0-4. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);

    public static final int META_FIBRES = 0;
    public static final int META_GRASS_1 = 1;
    public static final int META_GRASS_2 = 2;
    public static final int META_SPORE_STALK = 3;
    public static final int META_MATURE_SPORE_STALK = 4;

    private static final VoxelShape BUSH_SHAPE = box(0.2 * 16, 0.0, 0.2 * 16, 0.8 * 16, 0.8 * 16, 0.8 * 16);
    private static final VoxelShape PLATE_Y = box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    private static final float PLATE = 1.0F; // 1/16 block, the original's 0.0625F

    public BlockTaintFibres() {
        super(Properties.of()
                .mapColor(MapColor.STONE_GRAY)
                .strength(1.0F, 5.0F)
                .sound(BlockTaint.TAINT_SOUNDS)
                .noCollission()
                .lightEmissive(state -> switch (state.getValue(VARIANT)) {
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

    public static BlockState stateFor(final int meta) {
        return com.jonnyutah12.thaumcraftclassic.registration.catalog.M504Taint.BLOCK_TAINT_FIBRES
                .get().defaultBlockState().setValue(VARIANT, meta);
    }

    @Override
    public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos,
                           final net.minecraft.util.RandomSource rand) {
        TaintManager.fibresTick(level, pos, state, rand);
    }

    /** Original {@code onEntityWalk}: rarer taint poison than crust/soil. */
    @Override
    public void stepOn(final Level level, final BlockPos pos, final BlockState state, final Entity entity) {
        TaintManager.stepEffect(level, entity, true);
    }

    /** Original getItemDropped returned null: fibres never drop anything. */
    @Override
    public List<ItemStack> getDrops(final BlockState state, final ServerLevel level, final BlockPos pos,
                                    final BlockState toolState, @Nullable final Entity breaker) {
        return List.of();
    }

    /** Meta-0 plate against the first solid face; metas 1-4 the bush box. */
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        if (state.getValue(VARIANT) != META_FIBRES) {
            return BUSH_SHAPE;
        }
        for (final Direction d : Direction.values()) {
            final BlockPos np = pos.relative(d);
            if (level.getBlockState(np).isFaceSturdy(level, np, d.getOpposite())) {
                return switch (d) {
                    case DOWN -> PLATE_Y;
                    case UP -> box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
                    case NORTH -> box(0.0, 0.0, 0.0, 16.0, 16.0, PLATE);
                    case SOUTH -> box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
                    case WEST -> box(0.0, 0.0, 0.0, PLATE, 16.0, 16.0);
                    case EAST -> box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
                };
            }
        }
        return PLATE_Y;
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                        final CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * Original {@code canPlaceBlockAt} required the taint biome; see the
     * class javadoc for the deviation note. Survival itself is unconditional
     * (the tick rule handles biome exit, exactly like the original).
     */
    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return true;
    }
}
