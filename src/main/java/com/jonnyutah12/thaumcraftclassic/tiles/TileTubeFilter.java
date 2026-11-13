package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The filtered essentia tube (content-mapping.csv
 * {@code thaumcraft:tiletubefilter}, blockTube variant 3).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTubeFilter extends TileTube}
 * — the whole behaviour is the suction filter: it overrides
 * {@code calculateSuction} to pass {@code this.aspectFilter} as the parent's
 * {@code filter} argument, which is what makes the tube refuse a line carrying
 * anything else (see {@link TubeTransportLogic#suctionFromNeighbour}). The
 * filter is set and cleared by the block's interactions from the original's
 * {@code itemResource} metadata-13 jar label, and it is persisted under the
 * original's {@code AspectFilter} key.</p>
 *
 * <p>The original also implements {@code IAspectContainer} for this tile,
 * returning {@code new AspectList().add(filter, -1)} — a negative-amount
 * marker no ported consumer reads (the label tooltip path the original used it
 * for reads the <em>item's</em> container). {@code IAspectContainer} is not a
 * ported interface, so the filter is exposed through {@link #aspectFilter}
 * instead and the resonator reads it through the transport contract.</p>
 */
public class TileTubeFilter extends TileTube {

    /** The original {@code aspectFilter}: null means "let everything through". */
    @Nullable
    public Aspect aspectFilter;

    public TileTubeFilter(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_TUBE_FILTER.get(), pos, state);
    }

    /** Original override: the tube's own filter replaces the caller's. */
    @Override
    void calculateSuction(@Nullable final Aspect filter, final boolean restrict, final boolean directional) {
        super.calculateSuction(this.aspectFilter, restrict, directional);
    }

    @Override
    public void readCustomNBT(final CompoundTag tag) {
        super.readCustomNBT(tag);
        this.aspectFilter = aspectOrNull(tag.getString("AspectFilter"));
    }

    @Override
    public void writeCustomNBT(final CompoundTag tag) {
        super.writeCustomNBT(tag);
        if (this.aspectFilter != null) {
            tag.putString("AspectFilter", this.aspectFilter.getTag());
        }
    }
}
