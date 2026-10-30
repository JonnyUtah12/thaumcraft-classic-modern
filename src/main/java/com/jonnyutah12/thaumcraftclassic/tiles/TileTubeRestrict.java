package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The restricted essentia tube (content-mapping.csv
 * {@code thaumcraft:tiletuberestrict}, blockTube variant 5).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTubeRestrict extends TileTube}
 * — a one-line override that passes {@code restrict = true} to
 * {@code calculateSuction}, so the tube advertises half of the strongest
 * neighbouring suction instead of {@code suction - 1}. That is what makes a
 * restricted tube sit <em>behind</em> a plain tube in the priority order: it
 * loses the pull unless it is much closer to the sink.</p>
 */
public class TileTubeRestrict extends TileTube {

    public TileTubeRestrict(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_TUBE_RESTRICT.get(), pos, state);
    }

    @Override
    void calculateSuction(@Nullable final Aspect filter, final boolean restrict, final boolean directional) {
        super.calculateSuction(filter, true, directional);
    }
}
