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

import javax.annotation.Nullable;

/**
 * Advanced alchemical furnace nozzle (content-mapping.csv
 * {@code thaumcraft:tilealchemyfurnaceadvancednozzle}, hosted on
 * blockAlchemyFurnace variant 1).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedNozzle}
 * — a suction relay in front of the advanced alchemical furnace: it exposes
 * the furnace's aspect store to essentia tubes (suction 64 on its facing,
 * 16 elsewhere) and forwards take/add to the furnace behind it.</p>
 */
public class TileAlchemyFurnaceAdvancedNozzle extends BlockEntity implements IEssentiaTransport {

    public Direction facing = null;
    @Nullable
    public TileAlchemyFurnaceAdvanced furnace = null;

    public TileAlchemyFurnaceAdvancedNozzle(final BlockPos pos, final BlockState state) {
        super(M604cBlockEntities.TILE_ALCHEMY_FURNACE_ADVANCED_NOZZLE.get(), pos, state);
    }

    public void serverTick() {
        if (facing == null) {
            for (final Direction dir : Direction.values()) {
                final BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
                if (te instanceof TileAlchemyFurnaceAdvanced af) {
                    facing = dir;
                    furnace = af;
                }
            }
        }
    }

    @Override
    public boolean isConnectable(final Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return true;
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // original: no-op
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return face == facing ? 64 : 16;
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return furnace != null ? furnace.takeEssentia(aspect, amount, face) : 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        return furnace != null ? furnace.addEssentia(aspect, amount, face) : 0;
    }

    @Override
    public Aspect getEssentiaType(final Direction face) {
        return furnace != null ? furnace.getEssentiaType(face) : null;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return furnace != null ? furnace.getEssentiaAmount(face) : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
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
            facing = Direction.from3DDataValue(Math.max(0, Math.min(5, tag.getByte("facing"))));
        }
    }
}
