package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.tiles.TileLifter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * Arcane levitator (content-mapping.csv {@code thaumcraft:blocklifter}).
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockLifter} — a plain
 * block; the lift column logic lives in {@link TileLifter}.</p>
 *
 * <h2>The glow shell (BD-18)</h2>
 * The original's ISMR draws an {@code animatedglow} shell on five faces: the
 * bottom face (gated on the block above being solid) and the four sides (each
 * gated on that neighbour being solid), 0.01 out from the block surface, at
 * full brightness while the tile is unpowered —
 * {@code BlockLifterRenderer.java:50-89}: {@code bb = 180} when
 * {@code !tile.gettingPower()} (59-63), the bottom glow at 64-66 tinted
 * {@code BlockCustomOreItem.colors[4]} (57), the side glows at 69-83 tinted
 * {@code 0xDDDDDD} (68). The sprite is the block's own {@code iconGlow}
 * ({@code BlockLifter.java:39} → {@code thaumcraft:animatedglow}); the
 * levitator has no lit sprite and no top glow face.
 *
 * <p>In the port the shell is the {@code powered=false} model: its faces carry
 * {@code tintindex}, {@link DeviceBlockColors} supplies the two colours, and
 * {@code cullface} is the JSON spelling of the original's "only draw this glow
 * when that neighbour is solid" gate. Two deltas stay open and BD-18 records
 * them: the full-bright lightmap value ({@code bb = 180}) is not expressible
 * in a model, and the bottom glow's gate reads the block ABOVE, which a model
 * cannot query — so the port's bottom glow ships ungated.</p>
 *
 * <p>Power changes re-arm the tile's column recompute, as the original's
 * {@code onNeighborBlockChange} did through {@code updateLifterStack}
 * ({@code BlockLifter.java:85-91}).</p>
 */
public class BlockLifter extends Block implements EntityBlock, DeviceDynamicState {

    /**
     * BD-18: the appearance property. Vanilla's {@code powered} name is the
     * 1.21.1 spelling of the original's {@code TileLifter.gettingPower()}
     * query, and unpowered is the state that glows — the levitator pushes
     * while it has no signal.
     */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public BlockLifter() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F)
                .sound(net.minecraft.world.level.block.SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public Property<?> dynamicAppearanceProperty() {
        return POWERED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TileLifter(pos, state);
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

    /**
     * Original {@code onNeighborBlockChange} ({@code BlockLifter.java:85-91}):
     * a power change re-arms the tile's column recompute, which is where the
     * port re-publishes the {@code powered} appearance property too.
     */
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block neighborBlock, final BlockPos neighborPos,
                                final boolean movedByPiston) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof TileLifter tile
                && tile.gettingPower() != tile.lastPowerState) {
            tile.requiresUpdate = true;
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }
}
