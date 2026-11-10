package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The essentia centrifuge (TC-M4-06): port of {@code TileCentrifuge}. Pulls
 * one compound (non-primal) aspect unit from the connectable tile below,
 * spins for 39 ticks, and hands one randomly chosen component aspect out the
 * top. Redstone power stalls it (suction 0, no processing) — the original's
 * inverted gate, kept exactly. The client-side rotor animation fields are
 * renderer state and are not ported.
 */
public class TileCentrifuge extends BlockEntity implements IEssentiaTransport {

    public Aspect aspectOut = null;
    public Aspect aspectIn = null;
    public Direction facing = Direction.NORTH;
    int count = 0;
    int process = 0;

    public TileCentrifuge(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------- container

    public AspectList getAspects() {
        final AspectList al = new AspectList();
        if (this.aspectOut != null) {
            al.add(this.aspectOut, 1);
        }
        return al;
    }

    public void setAspects(final AspectList aspects) {
        // Original: no-op.
    }

    public int addToContainer(final Aspect tt, final int am) {
        int am2 = am;
        if (am2 > 0 && this.aspectOut == null) {
            this.aspectOut = tt;
            DeviceSync.sync(this);
            am2--;
        }
        return am2;
    }

    public boolean takeFromContainer(final Aspect tt, final int am) {
        if (this.aspectOut != null && tt == this.aspectOut) {
            this.aspectOut = null;
            DeviceSync.sync(this);
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(final AspectList ot) {
        return false;
    }

    public boolean doesContainerContainAmount(final Aspect tag, final int amt) {
        return amt == 1 && tag == this.aspectOut;
    }

    public boolean doesContainerContain(final AspectList ot) {
        for (final Aspect tt : ot.getAspects()) {
            if (tt == this.aspectOut) {
                return true;
            }
        }
        return false;
    }

    public int containerContains(final Aspect tag) {
        return tag == this.aspectOut ? 1 : 0;
    }

    public boolean doesContainerAccept(final Aspect tag) {
        return true;
    }

    // ------------------------------------------------------------- transport

    @Override
    public boolean isConnectable(final Direction face) {
        return face == Direction.UP || face == Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // Original: no-op.
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return face == Direction.DOWN ? (this.gettingPower() ? 0 : (this.aspectIn == null ? 128 : 64)) : 0;
    }

    @Override
    public Aspect getEssentiaType(final Direction face) {
        return this.aspectOut;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return this.aspectOut != null ? 1 : 0;
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        if (this.aspectIn == null && !aspect.isPrimal()) {
            this.aspectIn = aspect;
            this.process = 39;
            DeviceSync.sync(this);
            return 1;
        }
        return 0;
    }

    // ----------------------------------------------------------------- tick

    public void tick() {
        final var level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!this.gettingPower()) {
            if (this.aspectOut == null && this.aspectIn == null && ++this.count % 5 == 0) {
                this.drawEssentia();
            }
            if (this.process > 0) {
                this.process--;
            }
            if (this.aspectOut == null && this.aspectIn != null && this.process == 0) {
                this.processEssentia();
            }
        }
    }

    void processEssentia() {
        final java.util.List<Aspect> comps = this.aspectIn.getComponents();
        final var level = getLevel();
        this.aspectOut = comps.get(level == null ? 0 : level.getRandom().nextInt(2));
        this.aspectIn = null;
        DeviceSync.sync(this);
    }

    void drawEssentia() {
        final var level = getLevel();
        if (level == null) {
            return;
        }
        final IEssentiaTransport ic = EssentiaTransportHelper.getConnectableTile(level, this.worldPosition,
                Direction.DOWN);
        if (ic != null) {
            if (!ic.canOutputTo(Direction.UP)) {
                return;
            }

            Aspect ta = null;
            if (ic.getEssentiaAmount(Direction.UP) > 0
                    && ic.getSuctionAmount(Direction.UP) < this.getSuctionAmount(Direction.DOWN)
                    && this.getSuctionAmount(Direction.DOWN) >= ic.getMinimumSuction()) {
                ta = ic.getEssentiaType(Direction.UP);
            }

            if (ta != null
                    && !ta.isPrimal()
                    && ic.getSuctionAmount(Direction.UP) < this.getSuctionAmount(Direction.DOWN)
                    && ic.takeEssentia(ta, 1, Direction.UP) == 1) {
                this.aspectIn = ta;
                this.process = 39;
                DeviceSync.sync(this);
            }
        }
    }

    public boolean gettingPower() {
        final var level = getLevel();
        return level != null && level.hasNeighborSignal(this.worldPosition);
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCentrifugeNbt(tag);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCentrifugeNbt(tag);
    }

    /** Original readCustomNBT keys. */
    void readCentrifugeNbt(final CompoundTag nbt) {
        this.aspectIn = Aspect.getAspect(nbt.getString("aspectIn"));
        this.aspectOut = Aspect.getAspect(nbt.getString("aspectOut"));
        this.facing = Direction.from3DData(nbt.getInt("facing"));
    }

    /** Original writeCustomNBT keys. */
    void writeCentrifugeNbt(final CompoundTag nbt) {
        if (this.aspectIn != null) {
            nbt.putString("aspectIn", this.aspectIn.getTag());
        }
        if (this.aspectOut != null) {
            nbt.putString("aspectOut", this.aspectOut.getTag());
        }
        nbt.putInt("facing", this.facing.get3DDataValue());
    }
}
