package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arcane levitator (content-mapping.csv {@code thaumcraft:tilelifter}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileLifter}. A stack of lifters
 * raises the levitation range (10 per stacked, un-powered lifter below, cap
 * 10+10n); while unpowered it pushes items and living entities upward inside
 * its column. The original's "hold position" branch reads the client sneak
 * key through the common proxy — always false on a dedicated server — so the
 * port keeps only the push branch (faithful to server behaviour).</p>
 */
public class TileLifter extends BlockEntity implements com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase.TickingDeviceTile {

    private int counter = 0;
    public int rangeAbove = 0;
    public boolean requiresUpdate = true;
    public boolean lastPowerState = false;

    public TileLifter(final BlockPos pos, final BlockState state) {
        super(CatalogFurniture.TILE_LIFTER.get(), pos, state);
    }

    public void serverTick() {
        this.counter++;
        if (this.requiresUpdate || this.counter % 100 == 0) {
            this.lastPowerState = gettingPower();
            this.requiresUpdate = false;
            int max = 10;
            for (int count = 1;
                 this.level.getBlockState(this.worldPosition.below(count)).getBlock() == com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogFurniture.BLOCK_LIFTER.get()
                         && !this.level.hasNeighborSignal(this.worldPosition.below(count));
                 max += 10) {
                count++;
            }
            this.rangeAbove = 0;
            while (this.rangeAbove < max
                    // isRedstoneConductor is a BlockState (BlockBehaviour) query in 1.21.1,
                    // not a Block method — query the block state directly.
                    && !this.level.getBlockState(this.worldPosition.above(1 + this.rangeAbove))
                    .isRedstoneConductor(this.level, this.worldPosition.above(1 + this.rangeAbove))) {
                this.rangeAbove++;
            }
        }

        if (this.rangeAbove > 0 && !gettingPower()) {
            // MC 1.21.1 AABB takes Vec3 corners (the old BlockPos pair overload is gone);
            // atLowerCornerOf(y+2+range) is the top face of the last block in the column.
            final List<Entity> targets = this.level.getEntitiesOfClass(Entity.class,
                    new AABB(Vec3.atLowerCornerOf(this.worldPosition.above(1)),
                            Vec3.atLowerCornerOf(this.worldPosition.above(2 + this.rangeAbove))));
            for (final Entity e : targets) {
                if (e instanceof ItemEntity || e instanceof LivingEntity || e instanceof AbstractHorse) {
                    // Original: if (motionY < 0.35) motionY += 0.1; no clamp.
                    if (e.getDeltaMovement().y() < 0.35) {
                        e.setDeltaMovement(e.getDeltaMovement().x(), e.getDeltaMovement().y() + 0.1, e.getDeltaMovement().z());
                    }
                    e.resetFallDistance();
                }
            }
        }
    }

    public boolean gettingPower() {
        return this.level.hasNeighborSignal(this.worldPosition)
                || this.level.hasNeighborSignal(this.worldPosition.above());
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("rangeAbove", this.rangeAbove);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.rangeAbove = tag.getInt("rangeAbove");
    }
}
