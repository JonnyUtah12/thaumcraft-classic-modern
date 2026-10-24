package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nitor lamp (content-mapping.csv {@code thaumcraft:tilenitor}, hosted on
 * blockMetalDevice variant 10).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileNitor}. A permanent light
 * source that also emits a 10-tick redstone pulse when clicked, and burns
 * Fire essentia (pushed to it through essentia transport) while active. The
 * bore base draws its Fire through this tile's transport face.</p>
 */
public class TileNitor extends BlockEntity {

    public int redstoneSignal = 0;
    public int use = 0;
    private int age = 0;
    public AspectList essentia = new AspectList();

    public TileNitor(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_NITOR.get(), pos, state);
    }

    public void serverTick() {
        age++;
        if (age % 5 == 0) {
            essentiaTick();
        }
        if (redstoneSignal > 0) {
            redstoneSignal--;
            if (redstoneSignal == 0) {
                level.neighborChanged(worldPosition, level.getBlock(), worldPosition);
                level.neighborChanged(worldPosition.below(), level.getBlock(), worldPosition);
            }
        }
        if (use > 0) {
            if (!essentia.reduce(Aspect.getAspect("ignis"), 1)) {
                use = 0;
            }
        }
    }

    /** Pull Fire from adjacent providers (original essentiaTick). */
    private void essentiaTick() {
        final Aspect fire = Aspect.getAspect("ignis");
        for (final net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(face))
                    instanceof com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport other
                    && other.canOutputTo(face.getOpposite())) {
                final int take = other.takeEssentia(fire, 1, face.getOpposite());
                if (take > 0) {
                    essentia.add(fire, take);
                    other.setChanged();
                }
            }
        }
    }

    /** Original onBlockActivated: arm the pulse and the burn. */
    public void activate() {
        use = 10000;
        redstoneSignal = 10;
        level.neighborChanged(worldPosition, level.getBlock(), worldPosition);
        level.neighborChanged(worldPosition.below(), level.getBlock(), worldPosition);
        setChanged();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        essentia.writeToNBT(tag);
        tag.putInt("use", use);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essentia = new AspectList();
        essentia.readFromNBT(tag);
        use = tag.getInt("use");
    }
}
