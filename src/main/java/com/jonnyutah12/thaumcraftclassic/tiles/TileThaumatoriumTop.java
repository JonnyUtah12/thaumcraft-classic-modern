package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogThaumatorium;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Thaumatorium top tile (content-mapping.csv {@code thaumcraft:tilethaumatoriumtop},
 * original {@code thaumcraft.common.tiles.TileThaumatoriumTop}, hosted by
 * {@code blockmetaldevice} metadata 11 — the block placed above a thaumatorium).
 *
 * <p><b>Scope of this tile (TC-M7-04-m2c, gui_thaumatorium).</b> The window
 * seam, and only it: the original's {@code BlockMetalDevice.onBlockActivated}
 * for metadata 11 ({@code BlockMetalDevice.java:717-727}) opens the gui of the
 * {@code TileThaumatorium} in the block <i>below</i> the top when the player
 * is not sneaking, at that lower position. The port expresses that through the
 * {@link BlockDeviceBase.DeviceUseTile} seam — 1.21.1 calls
 * {@code useWithoutItem} on the empty-hand, non-sneaking path, which is the
 * original's gate — and opens the base tile's menu at the base position, so
 * the title, the position the menu binds and the {@code stillValid} distance
 * test all run against the base, exactly as the original's
 * {@code openGui(..., x, y - 1, z)} did.</p>
 *
 * <p>Everything else the original top tile does — the 1-slot input passthrough,
 * the {@code IAspectContainer}/{@code IEssentiaTransport} tube wire-up and its
 * server tick — belongs to the TileThaumatorium/TileThaumatoriumTop machine
 * rows of {@code docs/content-mapping.csv}, which this card does not flip
 * (BD-19).</p>
 */
public class TileThaumatoriumTop extends BlockEntity implements BlockDeviceBase.DeviceUseTile {

    public TileThaumatoriumTop(final BlockPos pos, final BlockState state) {
        super(CatalogThaumatorium.TILE_THAUMATORIUM_TOP.get(), pos, state);
    }

    /**
     * The original's {@code world.getTileEntity(x, y - 1, z)} instanceof
     * TileThaumatorium test: the base under this top, or null.
     */
    @Nullable
    public static TileThaumatorium baseThaumatorium(final net.minecraft.world.level.Level level,
                                                    final BlockPos topPos) {
        return level.getBlockEntity(topPos.below()) instanceof TileThaumatorium base
                ? base : null;
    }

    @Override
    public InteractionResult useWithoutItem(final ServerLevel level, final BlockPos pos,
                                            final ServerPlayer player) {
        final TileThaumatorium base = baseThaumatorium(level, pos);
        if (base == null) {
            // The original returns false with no base tile under the top.
            return InteractionResult.PASS;
        }
        // openGui(THAUM, world, x, y - 1, z): the menu binds the base position.
        player.openMenu(base, base.getBlockPos());
        return InteractionResult.CONSUME;
    }
}
