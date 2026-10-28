package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plain table surface (content-mapping.csv {@code thaumcraft:tiletable}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTable} — an empty tile whose
 * only purpose is to mark blockTable variants 0/1 and 6-13 as table surfaces
 * (the research-table pairing logic in BlockTable keys off tile identity).
 * The original's canUpdate()=false: this tile never ticks.</p>
 */
public class TileTable extends BlockEntity {

    public TileTable(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_TABLE.get(), pos, state);
    }
}
