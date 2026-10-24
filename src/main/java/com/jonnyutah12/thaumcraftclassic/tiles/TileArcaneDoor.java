package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane door owner tile (content-mapping.csv {@code thaumcraft:tilearcanedoor}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneDoor extends TileOwned}
 * — no behaviour of its own; the warding door access check lives on the
 * block. The owner is stamped by {@code BlockArcaneDoor.setPlacedBy}.</p>
 */
public class TileArcaneDoor extends TileOwned {

    public TileArcaneDoor(final BlockPos pos, final BlockState state) {
        super(M604dFurniture.TILE_ARCANE_DOOR.get(), pos, state);
    }
}
