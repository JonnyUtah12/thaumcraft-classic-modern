package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hanging banner (content-mapping.csv {@code thaumcraft:tilebanner}, hosted on
 * blockWoodenDevice variants 6/7 — TC-M6-04c's block).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileBanner}. Purely decorative
 * state: facing, dye colour, an optional aspect emblem and a wall-mounted
 * flag; the original never ticks. The aspect is drawn by the renderer
 * (TC-M7-01); the fields are the data contract it reads.</p>
 */
public class TileBanner extends BlockEntity {

    private byte facing = 0;
    private byte color = -1;
    private Aspect aspect = null;
    private boolean onWall = false;

    public TileBanner(final BlockPos pos, final BlockState state) {
        super(M604dFurniture.TILE_BANNER.get(), pos, state);
    }

    public byte getFacing() {
        return this.facing;
    }

    public void setFacing(final byte face) {
        this.facing = face;
        setChanged();
    }

    public boolean getWall() {
        return this.onWall;
    }

    public void setWall(final boolean wall) {
        this.onWall = wall;
        setChanged();
    }

    public Aspect getAspect() {
        return this.aspect;
    }

    public void setAspect(final Aspect aspect) {
        this.aspect = aspect;
    }

    public byte getColor() {
        return this.color;
    }

    public void setColor(final byte color) {
        this.color = color;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", this.facing);
        tag.putByte("color", this.getColor());
        tag.putString("aspect", this.getAspect() == null ? "" : this.getAspect().getTag());
        tag.putBoolean("wall", this.onWall);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.facing = tag.getByte("facing");
        this.setColor(tag.getByte("color"));
        final String as = tag.getString("aspect");
        this.aspect = as != null && !as.isEmpty() ? Aspect.getAspect(as) : null;
        this.onWall = tag.getBoolean("wall");
    }
}
