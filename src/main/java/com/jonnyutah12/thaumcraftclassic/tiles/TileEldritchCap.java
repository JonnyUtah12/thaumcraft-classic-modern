package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eldritch capstone (content-mapping.csv {@code thaumcraft:tileeldritchcap},
 * hosted on blockEldritch variant 3).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEldritchCap} — a tile entity
 * with {@code canUpdate() == false} and no behaviour at all: it exists so the
 * capstone's 6-cell tile-entity range (the original's
 * {@code func_145833_n}/{@code getTileEntityDistance}) lets the eldritch
 * structures find the capstones of an obelisk column, and so the obelisk
 * spawner code can address the column. It is registered for exactly that
 * reason, and carries nothing else.</p>
 */
public class TileEldritchCap extends BlockEntity {

    public TileEldritchCap(final BlockPos pos, final BlockState state) {
        super(CatalogEldritchRealm.TILE_ELDRITCH_CAP.get(), pos, state);
    }
}
