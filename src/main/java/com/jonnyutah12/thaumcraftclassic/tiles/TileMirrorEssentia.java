package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockMirror;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604dFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Essentia mirror (content-mapping.csv {@code thaumcraft:tilemirroressentia},
 * blockMirror variants 6-11).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileMirrorEssentia} — same link
 * machinery as {@link TileMirror} (separate class in the original too), but
 * instead of teleporting items it acts as an {@link IAspectSource}: consumers
 * (thaumatorium, TC-M4-05) pull essentia through the link from the jar
 * sitting on the linked mirror. The jar block entity is TC-M4-06; the port
 * reaches it through the shared {@link IEssentiaTransport} contract, so the
 * delegation is inert until jars land.</p>
 */
public class TileMirrorEssentia extends BlockEntity implements IAspectSource, com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile {

    public boolean linked = false;
    public int linkX;
    public int linkY;
    public int linkZ;
    @Nullable
    public ResourceKey<Level> linkDim;
    int count = 0;
    int inc = 40;

    public TileMirrorEssentia(final BlockPos pos, final BlockState state) {
        super(M604dFurniture.TILE_MIRROR_ESSENTIA.get(), pos, state);
    }

    /** The face the mirror points at (its link passes essentia out that way). */
    private Direction linkedFacing() {
        return BlockMirror.facingFor(getBlockState().getValue(BlockMirror.VARIANT));
    }

    /** The transport at the linked mirror's position (the jar on top of it). */
    @Nullable
    private IEssentiaTransport linkedTransport() {
        if (!this.linked) {
            return null;
        }
        final ServerLevel world = TileMirror.levelOf(this.linkDim);
        if (world == null) {
            return null;
        }
        final BlockPos target = new BlockPos(this.linkX, this.linkY, this.linkZ);
        final BlockEntity at = world.getBlockEntity(target);
        if (at instanceof IEssentiaTransport transport) {
            return transport;
        }
        // Original: the jar sits on top of the linked mirror.
        final BlockEntity above = world.getBlockEntity(target.above());
        return above instanceof IEssentiaTransport transport ? transport : null;
    }

    public boolean isLinkValid() {
        if (!this.linked) {
            return false;
        }
        final ServerLevel world = TileMirror.levelOf(this.linkDim);
        if (world == null) {
            return false;
        }
        final BlockEntity te = world.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
        if (te instanceof TileMirrorEssentia tm && tm.linked
                && tm.linkX == this.worldPosition.getX() && tm.linkY == this.worldPosition.getY()
                && tm.linkZ == this.worldPosition.getZ() && tm.linkDim == this.level.dimension()) {
            return true;
        }
        this.linked = false;
        this.setChanged();
        return false;
    }

    public boolean isLinkValidSimple() {
        if (!this.linked) {
            return false;
        }
        final ServerLevel world = TileMirror.levelOf(this.linkDim);
        if (world == null) {
            return false;
        }
        final BlockEntity te = world.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
        return te instanceof TileMirrorEssentia tm && tm.linked
                && tm.linkX == this.worldPosition.getX() && tm.linkY == this.worldPosition.getY()
                && tm.linkZ == this.worldPosition.getZ() && tm.linkDim == this.level.dimension();
    }

    public void restoreLink() {
        final ServerLevel world = TileMirror.levelOf(this.linkDim);
        if (world == null) {
            return;
        }
        final BlockEntity te = world.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
        if (te instanceof TileMirrorEssentia tm && !tm.isLinkValid()) {
            tm.linked = true;
            tm.linkX = this.worldPosition.getX();
            tm.linkY = this.worldPosition.getY();
            tm.linkZ = this.worldPosition.getZ();
            tm.linkDim = this.level.dimension();
            this.linked = true;
            this.setChanged();
            tm.setChanged();
        }
    }

    public void invalidateLink() {
        final ServerLevel world = TileMirror.levelOf(this.linkDim);
        if (world != null) {
            final BlockEntity te = world.getBlockEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te instanceof TileMirrorEssentia tm) {
                tm.linked = false;
                this.setChanged();
                tm.setChanged();
            }
        }
    }

    public void serverTick() {
        if (this.count++ % this.inc == 0) {
            if (!isLinkValidSimple()) {
                if (this.inc < 600) {
                    this.inc += 20;
                }
                restoreLink();
            } else {
                this.inc = 40;
            }
        }
    }

    // --- IAspectSource: delegate through the link (original getJar path) ---

    @Override
    public int getAspectPoolSize() {
        final IEssentiaTransport jar = linkedTransport();
        if (jar == null) {
            return 0;
        }
        int total = 0;
        for (final Direction face : Direction.values()) {
            final Aspect type = jar.getEssentiaType(face);
            if (type != null) {
                total += jar.getEssentiaAmount(face);
            }
        }
        return total;
    }

    @Override
    public boolean doesContain(final Aspect aspect) {
        final IEssentiaTransport jar = linkedTransport();
        if (jar == null) {
            return false;
        }
        for (final Direction face : Direction.values()) {
            if (aspect.equals(jar.getEssentiaType(face))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int useAspect(final Aspect aspect, final int amount) {
        final IEssentiaTransport jar = linkedTransport();
        if (jar == null) {
            return 0;
        }
        for (final Direction face : Direction.values()) {
            if (aspect.equals(jar.getEssentiaType(face))) {
                final int taken = jar.takeEssentia(aspect, Math.min(amount, 8), face);
                if (taken > 0) {
                    ((BlockEntity) jar).setChanged();
                    return taken;
                }
            }
        }
        return 0;
    }

    /** Original takeFromContainer(Aspect, amount) over the linked jar. */
    public boolean takeFromContainer(final Aspect aspect, final int amount) {
        return useAspect(aspect, amount) > 0;
    }

    public AspectList getAspects() {
        return null;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", this.linked);
        tag.putInt("linkX", this.linkX);
        tag.putInt("linkY", this.linkY);
        tag.putInt("linkZ", this.linkZ);
        if (this.linkDim != null) {
            tag.putString("linkDim", this.linkDim.location().toString());
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linked = tag.getBoolean("linked");
        this.linkX = tag.getInt("linkX");
        this.linkY = tag.getInt("linkY");
        this.linkZ = tag.getInt("linkZ");
        this.linkDim = tag.contains("linkDim")
                ? ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(tag.getString("linkDim")))
                : null;
    }
}
