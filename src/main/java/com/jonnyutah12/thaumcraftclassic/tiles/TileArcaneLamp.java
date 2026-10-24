package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arcane lamp base (content-mapping.csv {@code thaumcraft:tilearanelamp};
 * hosted on blockMetalDevice variants 7/8/9 by the growth/fertility/nitor
 * subclasses).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileArcaneLamp}. Stores up to
 * 64 units of one suction aspect drawn through {@link IEssentiaTransport}
 * from adjacent essentia providers, and burns one unit of the target's
 * aspect cost per tick while its effect runs.</p>
 *
 * <p>Deferred: the vis-drain fallback (the original drained ambient vis when
 * essentia ran out) waits on the vis network (TC-M4-05); the light-level
 * boost with vis is likewise TC-M4-05. The block's light level is the static
 * original value.</p>
 */
public class TileArcaneLamp extends BlockEntity implements IEssentiaTransport {

    public AspectList essentia = new AspectList();
    public Aspect suction;
    public int suctionCap = 64;
    public int use = 0;
    protected int age = 0;

    public TileArcaneLamp(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public void serverTick() {
        age++;
        if (age % 5 == 0) {
            essentiaTick();
        }
        if (age % 20 == 0) {
            updateUse();
        }
        if (use > 0) {
            final AspectList target = target();
            if (target == null || target.size() == 0) {
                use = 0;
            } else {
                consumeEssentia(target);
            }
        }
    }

    /** The block this lamp is currently acting on, or null. */
    protected AspectList target() {
        return null;
    }

    protected void updateUse() {
        // subclasses re-arm `use` when a target is in range
    }

    /** Original {@code consumeEssentia}: one unit of each required aspect. */
    protected void consumeEssentia(final AspectList list) {
        for (final Aspect aspect : list.getAspects()) {
            if (essentia.getAmount(aspect) > 0) {
                essentia.reduce(aspect, 1);
            } else {
                use = 0;
            }
        }
    }

    /** Original {@code essentiaTick}: pull suction aspect from connected providers. */
    protected void essentiaTick() {
        if (suction == null) {
            return;
        }
        for (final Direction face : Direction.values()) {
            final BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(face));
            if (!(neighbor instanceof IEssentiaTransport other)
                    || !other.canOutputTo(face.getOpposite())
                    || !isConnectable(face)) {
                continue;
            }
            final Aspect type = other.getEssentiaType(face.getOpposite());
            final int amount = other.getEssentiaAmount(face.getOpposite());
            if (type != null && type == suction && amount > 0
                    && essentia.getAmount(suction) < suctionCap) {
                final int take = other.takeEssentia(suction, 1, face.getOpposite());
                if (take > 0) {
                    essentia.add(suction, take);
                    other.setChanged();
                }
            }
        }
        setChanged();
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
        suction = aspect;
        suctionCap = amount < 0 ? 64 : amount;
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return suction;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return suction == null ? 0 : suctionCap - essentia.getAmount(suction);
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
        if (suction != null && aspect == suction && essentia.getAmount(suction) < suctionCap) {
            essentia.add(aspect, amount);
            setChanged();
            return amount;
        }
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        essentia.writeToNBT(tag);
        tag.putInt("use", use);
        if (suction != null) {
            tag.putString("suction", suction.getTag());
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essentia = new AspectList();
        essentia.readFromNBT(tag);
        use = tag.getInt("use");
        final String s = tag.getString("suction");
        suction = s.isEmpty() ? null : Aspect.getAspect(s);
    }
}
