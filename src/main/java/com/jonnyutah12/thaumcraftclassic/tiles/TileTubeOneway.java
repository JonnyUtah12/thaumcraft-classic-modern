package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The directional (one-way) essentia tube (content-mapping.csv
 * {@code thaumcraft:tiletubeoneway}, blockTube variant 6).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTubeOneway extends TileTube}.
 * Two overrides, both meaning "keep the line one-way": {@code calculateSuction}
 * passes {@code directional = true} (so the tube only reads suction from the
 * face <em>opposite</em> its {@code facing}, i.e. it is pulled along its
 * direction) and {@code equalizeWithNeighbours} passes {@code directional =
 * true} as well (so it also only pulls from the face it faces away from).
 * Together with the {@code facing} property the block toggles, that is the
 * whole device.</p>
 */
public class TileTubeOneway extends TileTube {

    public TileTubeOneway(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_TUBE_ONEWAY.get(), pos, state);
    }

    @Override
    void calculateSuction(@Nullable final Aspect filter, final boolean restrict, final boolean directional) {
        super.calculateSuction(filter, restrict, true);
    }

    @Override
    void equalizeWithNeighbours(final boolean directional) {
        super.equalizeWithNeighbours(true);
    }
}
