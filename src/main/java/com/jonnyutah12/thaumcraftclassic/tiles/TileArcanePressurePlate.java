package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane pressure plate (content-mapping.csv {@code thaumcraft:tilearcanepressureplate},
 * hosted on blockWoodenDevice variants 2/3 — 2 unpowered, 3 powered, exactly
 * the original's metadata pair).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcanePressurePlate} — an
 * owned tile carrying only a {@code setting} byte: 0 triggers on everything,
 * 1 on everything except the owner (and guests), 2 on the owner (and guests)
 * only. The detection and power logic lives in the block
 * ({@code BlockWoodenDevice}), mirroring the original where the block, not
 * the tile, ran {@code setStateIfMobInteractsWithPlate}.</p>
 */
public class TileArcanePressurePlate extends TileOwned {

    public byte setting = 0;

    public TileArcanePressurePlate(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_ARCANE_PRESSURE_PLATE.get(), pos, state);
    }

    /** Original {@code canUpdate()=false}. */
    @Override
    public void serverTick() {
        // no-op, matching the original
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("setting", setting);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setting = tag.getByte("setting");
    }
}
