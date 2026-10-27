package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane bellows (content-mapping.csv {@code thaumcraft:tilebellows},
 * hosted on blockWoodenDevice variant 0).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileBellows}. Furnaces count
 * adjacent bellows that face them ({@link #getBellows}) to shorten cook time
 * and raise smelting-bonus odds — that boost is this card's furnaces'
 * behavior. The original's vanilla-furnace boost branch ({@code
 * onVanillaFurnace}) is ported structurally but was dead code in the
 * original: nothing ever set the flag true, and 1.21.1's furnace burn clock
 * ({@code litTime}) is package-private, so the boost is not applied here.</p>
 *
 * <p>Deferred to owning cards: the inflation animation (client renderer,
 * TC-M7-01) and wand rotation of {@code orientation} (TC-M6-01 — in the
 * original the orientation was only ever changed by the wand).</p>
 */
public class TileBellows extends BlockEntity implements BlockDeviceBase.TickingDeviceTile {

    /** Direction the bellows points; original ForgeDirection ordinal, default DOWN. */
    public Direction orientation = Direction.DOWN;
    public boolean onVanillaFurnace = false;
    private int delay = 0;

    public TileBellows(final BlockPos pos, final BlockState state) {
        this(CatalogDevices.TILE_BELLOWS.get(), pos, state);
    }

    public TileBellows(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public void serverTick() {
        if (onVanillaFurnace && !gettingPower()) {
            delay++;
            if (delay >= 2) {
                delay = 0;
                final BlockEntity target = level.getBlockEntity(worldPosition.relative(orientation));
                // Original: advance the vanilla furnace burn clock. Unreachable
                // in the original (flag never set) and not writable in 1.21.1
                // (litTime is package-private) — kept for save parity only.
                if (target instanceof FurnaceBlockEntity) {
                    // no-op
                }
            }
        }
    }

    /** Original {@code gettingPower()}: the bellows itself is powered. */
    public boolean gettingPower() {
        return level.hasNeighborSignal(worldPosition);
    }

    /**
     * Original static helper: count bellows adjacent to {@code pos} along
     * {@code directions} that point back at it and are unpowered.
     */
    public static int getBellows(final net.minecraft.world.level.Level world, final BlockPos pos,
            final Direction[] directions) {
        int bellows = 0;
        for (final Direction dir : directions) {
            final BlockPos at = pos.relative(dir);
            if (world.getBlockEntity(at) instanceof TileBellows b
                    && b.orientation == dir.getOpposite()
                    && !world.hasNeighborSignal(at)) {
                bellows++;
            }
        }
        return bellows;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("orientation", (byte) orientation.ordinal());
        tag.putBoolean("onVanillaFurnace", onVanillaFurnace);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final Direction[] all = Direction.values();
        final int ordinal = tag.getByte("orientation");
        orientation = ordinal >= 0 && ordinal < all.length ? all[ordinal] : Direction.DOWN;
        onVanillaFurnace = tag.getBoolean("onVanillaFurnace");
    }
}
