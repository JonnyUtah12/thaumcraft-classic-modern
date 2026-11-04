package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eldritch nothing (content-mapping.csv {@code thaumcraft:tileeldritchnothing},
 * hosted on blockEldritchNothing variant 1).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEldritchNothing} — a tile
 * entity with {@code canUpdate() == false}. The original gave the "exposed"
 * state (meta 1) a tile entity so the block's exposure toggle re-renders, and
 * nothing else. It exists for that parity only.</p>
 */
public class TileEldritchNothing extends BlockEntity {

    public TileEldritchNothing(final BlockPos pos, final BlockState state) {
        super(CatalogEldritchRealm.TILE_ELDRITCH_NOTHING.get(), pos, state);
    }
}
