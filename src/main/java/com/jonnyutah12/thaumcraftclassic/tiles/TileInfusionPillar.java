package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogInfusion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Infusion pillar (TC-M4-04; content-mapping.csv {@code tileinfusionpillar},
 * hosted on blockStoneDevice variants 3/4 — the two pillar orientations).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileInfusionPillar} — a
 * non-ticking marker tile; the runic matrix's {@code validLocation} requires
 * four of these at the corners of the central pedestal's layer. The
 * {@code orientation} byte is kept for render parity (M7-01).</p>
 */
public class TileInfusionPillar extends BlockEntity {

    public byte orientation = 0;

    public TileInfusionPillar(final BlockPos pos, final BlockState state) {
        super(CatalogInfusion.TILE_INFUSION_PILLAR.get(), pos, state);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("orientation", this.orientation);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.orientation = tag.getByte("orientation");
    }
}
