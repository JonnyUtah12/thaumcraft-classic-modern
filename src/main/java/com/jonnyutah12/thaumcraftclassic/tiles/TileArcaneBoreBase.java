package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane bore base (content-mapping.csv {@code thaumcraft:tilearcaneborebase},
 * hosted on blockWoodenDevice variant 4).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneBoreBase} — the
 * stationary dock the bore head sits on. Stores the bore's orientation (set
 * by wand right-click on the side to bore toward; the wand itself is
 * TC-M6-01, so the setter is exposed here and wired from the block's use
 * path for now), and acts as an essentia intake: it sucks ENTROPY from any
 * adjacent transport face except the boring side, and hands it to the bore
 * head via {@link #drawEssentia()}.</p>
 */
public class TileArcaneBoreBase extends BlockEntity implements IEssentiaTransport {

    public Direction orientation = Direction.NORTH;

    public TileArcaneBoreBase(final BlockPos pos, final BlockState state) {
        super(CatalogDevices.TILE_ARCANE_BORE_BASE.get(), pos, state);
    }

    /** Original {@code canUpdate()=false}. */
    public void serverTick() {
        // no-op, matching the original
    }

    /** Original {@code onWandRightClick}: set the boring direction from the clicked face. */
    public void onWandRightClick(final Direction side) {
        this.orientation = side;
        setChanged();
    }

    /** Original {@code drawEssentia}: pull one ENTROPY from a willing neighbour. */
    boolean drawEssentia() {
        for (final Direction facing : Direction.values()) {
            final BlockEntity other = level.getBlockEntity(worldPosition.relative(facing));
            if (other instanceof IEssentiaTransport ic) {
                if (!ic.canOutputTo(facing.getOpposite())) {
                    continue;
                }
                if (ic.getSuctionAmount(facing.getOpposite()) < getSuctionAmount(facing)
                        && ic.takeEssentia(Aspect.getAspect("entropia"), 1, facing.getOpposite()) == 1) {
                    other.setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isConnectable(final Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return true;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return false;
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // original: no-op
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return Aspect.getAspect("entropia");
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return face != orientation ? 128 : 0;
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(final Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("orientation", (byte) orientation.ordinal());
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        orientation = Direction.from3DDataValue(Math.max(0, Math.min(5, tag.getByte("orientation"))));
    }
}
