package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crystal cluster orientation tile (content-mapping.csv
 * {@code thaumcraft:TileCrystal}, owned by TC-M6-04b).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileCrystal} — a
 * {@code TileThaumcraft} carrying only {@code short orientation} (a
 * ForgeDirection ordinal, default 1 = UP), persisted as NBT
 * {@code "orientation"}. The orientation records which face of the supporting
 * block the cluster is attached to; {@link
 * com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal} uses it to break the
 * cluster when that support is removed. ForgeDirection ordinals and
 * {@link Direction#get3DDataValue()} agree (DOWN 0, UP 1, NORTH 2, SOUTH 3,
 * WEST 4, EAST 5), so the stored value is kept verbatim.</p>
 *
 * <p>The original's {@code TileThaumcraft} base (vis/aura bookkeeping) is
 * TC-M4/M5 territory; this row's behavior is the orientation field alone, so
 * the tile extends plain {@link BlockEntity}.</p>
 */
public class TileCrystal extends BlockEntity {

    /** NBT key from the original {@code writeCustomNBT}/{@code readCustomNBT}. */
    private static final String TAG_ORIENTATION = "orientation";

    /** Original default: ForgeDirection.UP. */
    private Direction orientation = Direction.UP;

    public TileCrystal(final BlockPos pos, final BlockState state) {
        super(com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks.TILE_CRYSTAL.get(), pos, state);
    }

    public Direction getOrientation() {
        return orientation;
    }

    public void setOrientation(final Direction orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_ORIENTATION, orientation.get3DDataValue());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        orientation = Direction.from3DDataValue(tag.getInt(TAG_ORIENTATION));
    }
}
