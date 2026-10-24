package com.jonnyutah12.thaumcraftclassic.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.util.RandomSource;

/**
 * TC tallow candle (content-mapping.csv {@code thaumcraft:blockcandle}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockCandle} — one block id,
 * metadata 0-15 = the sixteen dye colours (white…black, matching the
 * {@code Utils.colors} map-color table), ported verbatim to a
 * {@code variant} property (D-001). Properties are the original's: hardness
 * 0.1, cloth step sound, light 14 (original 0.95 × 15), non-opaque, no
 * collision box, and the 6/16..10/16 × 8/16 × 6/16..10/16 hitbox. The
 * original's circuits-material resistance (10.0) is carried as the block
 * resistance.</p>
 *
 * <p>Behavior ported: needs a sturdy surface below (original
 * {@code canPlaceAt}/{@code canBlockStay}), drops when that support is
 * removed ({@code onNeighborBlockChange}), and the flame+smoke ambient
 * particles ({@code randomDisplayTick}). The original's
 * {@code IInfusionStabiliser} marker (TC-M4-04) has no ported interface yet;
 * TC-M4-04 should key off this block id. Per-variant map colour
 * ({@code Utils.colors[meta]}) is not state-addressable in 1.21.1 —
 * TC-M7-01 model work.</p>
 */
public class BlockCandle extends Block {

    /** Original blockCandle metadata = dye colour, verbatim 0-15. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    /** Original {@code setBlockBounds(0.375, 0, 0.375, 0.625, 0.5, 0.625)}. */
    protected static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 8.0, 10.0);

    public BlockCandle() {
        super(Properties.of()
                .mapColor(MapColor.SNOW)
                .strength(0.1F, 10.0F)
                .sound(SoundType.CANDLE)
                .lightLevel(state -> 14)
                .noCollission()
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
    }

    /** Original {@code getCollisionBox} returned null. */
    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return Shapes.empty();
    }

    /** Original {@code canPlaceAt}: solid top surface of the block below. */
    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** Original {@code onNeighborBlockChange}: drop when the support is gone. */
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (!level.isClientSide && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    /** Original {@code randomDisplayTick}: flame + smoke wisps above the wick. */
    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.7;
        final double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    /** Preserve the variant through drops / pick-block (see {@link VariantStateItems}). */
    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return VariantStateItems.withVariant(super.getCloneItemStack(level, pos, state), state);
    }
}
