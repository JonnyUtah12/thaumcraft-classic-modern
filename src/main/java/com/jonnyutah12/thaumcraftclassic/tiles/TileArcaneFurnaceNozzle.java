package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.M604cBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane furnace nozzle (content-mapping.csv {@code thaumcraft:tilearcanefurnacenozzle},
 * blockArcaneFurnace variants 2/4/5/6/8).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneFurnaceNozzle}. Binds
 * to the adjacent furnace core and pulls Fire essentia from the tube network
 * on its facing side; each unit drawn buys the furnace 600 ticks of speed.</p>
 */
public class TileArcaneFurnaceNozzle extends BlockEntity implements IEssentiaTransport {

    public Direction facing = null;
    public TileArcaneFurnace furnace = null;
    private int drawDelay = 0;

    public TileArcaneFurnaceNozzle(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_ARCANE_FURNACE_NOZZLE.get(), pos, state);
    }

    public void serverTick() {
        if (facing == null && furnace == null) {
            for (final Direction dir : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(dir)) instanceof TileArcaneFurnace core) {
                    facing = dir.getOpposite();
                    furnace = core;
                    break;
                }
            }
        }
        if (furnace != null && furnace.speedyTime < 60 && drawEssentia()) {
            furnace.speedyTime += 600;
        }
    }

    private boolean drawEssentia() {
        if (++drawDelay % 5 != 0 || facing == null) {
            return false;
        }
        final BlockEntity te = level.getBlockEntity(worldPosition.relative(facing));
        if (te instanceof IEssentiaTransport ic && ic.canOutputTo(facing.getOpposite())) {
            if (ic.getSuctionAmount(facing.getOpposite()) < getSuctionAmount(facing)
                    && ic.takeEssentia(Aspect.getAspect("ignis"), 1, facing.getOpposite()) == 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isConnectable(final Direction face) {
        return facing != null;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return facing != null;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return false;
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // original: no suction of its own beyond the Fire pull below
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return Aspect.getAspect("ignis");
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        if (furnace != null && furnace.speedyTime < 40) {
            return 128;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
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
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (facing != null) {
            tag.putByte("facing", (byte) facing.ordinal());
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("facing")) {
            final Direction[] all = Direction.values();
            final int ordinal = tag.getByte("facing");
            facing = ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
        }
    }
}
