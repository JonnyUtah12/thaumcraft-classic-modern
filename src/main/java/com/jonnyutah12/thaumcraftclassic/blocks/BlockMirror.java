package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileMirror;
import com.jonnyutah12.thaumcraftclassic.tiles.TileMirrorEssentia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Magic mirror / essentia mirror (content-mapping.csv
 * {@code thaumcraft:blockmirror}, class=BlockMirror item_class=BlockMirrorItem).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockMirror}, one id with
 * metadata 0-5 (item mirror, meta = the face it is mounted on) and 6-11
 * (essentia mirror, same encoding + 6). The linking interaction is the hand
 * mirror item (TC-M6-04e, {@code items/ItemHandMirror}); this block only
 * mounts, passes dropped items to {@link TileMirror#transport}, and ticks.</p>
 */
public class BlockMirror extends BlockDeviceBase {

    public static final int MAX_VARIANT = 11;
    public static final int VARIANT_ESSENTIA_BASE = 6;

    // public (unlike the other device blocks): TileMirrorEssentia reads this field across packages
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, MAX_VARIANT);

    public BlockMirror() {
        super(VARIANT, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F)
                .noOcclusion());
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs). */
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /** Original metadata 0-5 = ForgeDirection ordinals; same in Direction. */
    public static Direction facingFor(final int meta) {
        return Direction.values()[meta % 6];
    }

    @Override
    public BlockState getStateForPlacement(final net.minecraft.world.item.context.BlockPlaceContext ctx) {
        final boolean essentia = variant(ctx.getLevel().getBlockState(ctx.getClickedPos())) >= VARIANT_ESSENTIA_BASE;
        final Direction face = ctx.getClickedFace();
        return withVariant(face.get3DDataValue() + (essentia ? VARIANT_ESSENTIA_BASE : 0));
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext ctx) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }

    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity item
                && level.getBlockEntity(pos) instanceof TileMirror mirror) {
            mirror.transport(item);
        }
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
}
