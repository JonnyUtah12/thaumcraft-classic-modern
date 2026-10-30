package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The essentia buffer (content-mapping.csv {@code thaumcraft:tiletubebuffer},
 * blockTube variant 4) — the tube network's small tank.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTubeBuffer extends
 * TileThaumcraft}, and it is the one tube-family tile that is <em>not</em> a
 * {@link TileTube}: it holds an {@link AspectList} pool of up to
 * {@value TubeTransportLogic#BUFFER_MAX_AMOUNT} units (any mix of aspects),
 * advertises no suction <em>type</em> of its own, and instead of a suction
 * figure it uses the number of adjacent arcane bellows
 * ({@code bellows * 32}, or 1 with none) plus a per-face choke it cycles
 * through on a sneak-use (0 open, 1 bellows-ignored, 2 closed). It fills
 * itself from a neighbour it out-sucks every 5 ticks and refuses to hand
 * essentia to a face that is beating it to the same sink.</p>
 *
 * <p>Every rule is a {@link TubeTransportLogic} pure function
 * ({@code bufferSuction}, {@code bufferTakeBlocked}, {@code bufferFillAllowed},
 * {@code bufferAddLeftover}, {@code bufferSignalStrength}), so
 * {@code TubeTransportLogicTest} pins them without a world; this class walks
 * the faces, applies the results and persists the pool under the original NBT
 * keys ({@code Aspects} through {@link AspectList}, {@code open}, {@code choke}).</p>
 *
 * <p>The original's {@code addTraceableCuboids} (the wand ray-trace sub-hits
 * that toggle a face) rides on the wand/resonator interaction card, like the
 * base tube's. The client renderer and its aspect-coloured fill are TC-M7-01.</p>
 */
public class TileTubeBuffer extends BlockEntity implements IEssentiaTransport {

    /** Original {@code MAXAMOUNT}. */
    public static final int MAX_AMOUNT = TubeTransportLogic.BUFFER_MAX_AMOUNT;

    /** Original {@code aspects}: the shared pool, any aspect mix. */
    public final AspectList aspects = new AspectList();

    /** Original {@code openSides}, one flag per face. */
    public final boolean[] openSides = {true, true, true, true, true, true};

    /** Original {@code chokedSides}: 0 open, 1 ignore the bellows, 2 closed. */
    public final byte[] chokedSides = {0, 0, 0, 0, 0, 0};

    /** Original {@code bellows}: the cached count, -1 until the first scan. */
    public int bellows = -1;

    int count;

    public TileTubeBuffer(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_TUBE_BUFFER.get(), pos, state);
    }

    // --- the transport pass (original updateEntity) -----------------------

    /**
     * Original {@code TileTubeBuffer.updateEntity}: rescan the bellows on the
     * first tick and every 20 after, and top the pool up every 5 ticks while it
     * has room.
     */
    public void serverTick() {
        this.count++;
        if (this.bellows < 0 || this.count % TubeTransportLogic.BUFFER_BELLOWS_RESCAN_TICKS == 0) {
            getBellows();
        }
        if (level != null && !level.isClientSide
                && this.count % TubeTransportLogic.BUFFER_FILL_INTERVAL_TICKS == 0
                && this.aspects.visSize() < MAX_AMOUNT) {
            fillBuffer();
        }
    }

    /**
     * Original {@code fillBuffer}: take one unit from the first face whose
     * neighbour holds essentia, advertises less suction than this buffer's own
     * face and is satisfied by at least as much as its minimum suction.
     */
    void fillBuffer() {
        for (final Direction dir : Direction.values()) {
            final BlockEntity te = EssentiaTransportHelper.getConnectableTile(level, worldPosition, dir);
            if (te instanceof IEssentiaTransport ic) {
                final Direction theirSide = dir.getOpposite();
                if (TubeTransportLogic.bufferFillAllowed(ic.getEssentiaAmount(theirSide),
                        ic.getSuctionAmount(theirSide), getSuctionAmount(dir), ic.getMinimumSuction())) {
                    final Aspect ta = ic.getEssentiaType(theirSide);
                    addToContainer(ta, ic.takeEssentia(ta, TubeTransportLogic.TRANSFER_PER_PASS, theirSide));
                    return;
                }
            }
        }
    }

    /** Original {@code getBellows()}: count the arcane bellows pointing at this buffer. */
    public void getBellows() {
        if (level == null) {
            return;
        }
        this.bellows = TileBellows.getBellows(level, worldPosition, Direction.values());
    }

    // --- the pool (original IAspectContainer surface) ---------------------

    public AspectList getAspects() {
        return this.aspects;
    }

    public void setAspects(final AspectList aspects) {
        // Original setAspects is a no-op; the buffer fills through addToContainer.
    }

    /** Original {@code addToContainer}: one unit at a time, only while there is room. */
    public int addToContainer(@Nullable final Aspect aspect, final int amount) {
        final int leftover = TubeTransportLogic.bufferAddLeftover(this.aspects.visSize(), amount, MAX_AMOUNT);
        if (leftover == 0) {
            this.aspects.add(aspect, amount);
            touch();
        }
        return leftover;
    }

    /** Original {@code takeFromContainer(Aspect, int)}. */
    public boolean takeFromContainer(@Nullable final Aspect aspect, final int amount) {
        if (aspect != null && this.aspects.getAmount(aspect) >= amount) {
            this.aspects.remove(aspect, amount);
            touch();
            return true;
        }
        return false;
    }

    public boolean doesContainerContainAmount(@Nullable final Aspect aspect, final int amount) {
        return aspect != null && this.aspects.getAmount(aspect) >= amount;
    }

    public int containerContains(@Nullable final Aspect aspect) {
        return aspect == null ? 0 : this.aspects.getAmount(aspect);
    }

    public boolean doesContainerAccept(@Nullable final Aspect aspect) {
        return true;
    }

    // --- IEssentiaTransport ------------------------------------------------

    @Override
    public boolean isConnectable(final Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return face != null && openSides[face.ordinal()];
    }

    @Override
    public void setSuction(@Nullable final Aspect aspect, final int amount) {
        // Original no-op: the buffer's suction comes from its bellows, not a neighbour.
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
    @Nullable
    public Aspect getSuctionType(final Direction loc) {
        return null;
    }

    /** Original {@code getSuctionAmount}: choke-driven, bellows-scaled. */
    @Override
    public int getSuctionAmount(final Direction loc) {
        return TubeTransportLogic.bufferSuction(this.bellows, choke(loc));
    }

    /**
     * Original {@code getEssentiaType}: the pool is mixed, so it reports a
     * random member of it (the original drew from the world's random, which is
     * what the client renderer and the equalisation pass both read).
     */
    @Override
    @Nullable
    public Aspect getEssentiaType(final Direction loc) {
        final List<Aspect> held = this.aspects.getAspects();
        if (held.isEmpty()) {
            return null;
        }
        final int index = level == null ? 0 : level.getRandom().nextInt(held.size());
        return held.get(index);
    }

    @Override
    public int getEssentiaAmount(final Direction loc) {
        return this.aspects.visSize();
    }

    /**
     * Original {@code takeEssentia}: hand out the requested amount, but only
     * when no <em>other</em> open face reaches a stronger sink for the same
     * aspect — otherwise a buffer would strip a line its own better neighbour
     * is pulling on.
     */
    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        if (level == null || aspect == null || !canOutputTo(face)) {
            return 0;
        }
        final BlockEntity te = EssentiaTransportHelper.getConnectableTile(level, worldPosition, face);
        final int faceSuction = te instanceof IEssentiaTransport ic
                ? ic.getSuctionAmount(face.getOpposite()) : 0;
        final List<TubeTransportLogic.BufferNeighbour> others = new ArrayList<>();
        for (final Direction dir : Direction.values()) {
            if (dir == face || !canOutputTo(dir)) {
                continue;
            }
            final BlockEntity other = EssentiaTransportHelper.getConnectableTile(level, worldPosition, dir);
            if (other instanceof IEssentiaTransport ic) {
                others.add(new TubeTransportLogic.BufferNeighbour(
                        ic.getSuctionAmount(dir.getOpposite()),
                        ic.getSuctionType(dir.getOpposite()),
                        getSuctionAmount(dir)));
            }
        }
        if (TubeTransportLogic.bufferTakeBlocked(aspect, faceSuction, others)) {
            return 0;
        }
        int take = amount;
        if (take > this.aspects.getAmount(aspect)) {
            take = this.aspects.getAmount(aspect);
        }
        return takeFromContainer(aspect, take) ? take : 0;
    }

    /** Original {@code addEssentia}: {@code amount - addToContainer(aspect, amount)}. */
    @Override
    public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
    }

    /** Original {@code markDirtyForUpdate} + {@code markDirty}. */
    public void touch() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The choke value of a face; an absent face reads as open (the original indexed directly). */
    private int choke(final Direction face) {
        return face == null ? 0 : this.chokedSides[face.ordinal()];
    }

    // --- persistence (the original NBT keys) ------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.aspects.writeToNBT(tag);
        final byte[] sides = new byte[6];
        for (int a = 0; a < 6; a++) {
            sides[a] = (byte) (this.openSides[a] ? 1 : 0);
        }
        tag.putByteArray("open", sides);
        tag.putByteArray("choke", this.chokedSides);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.aspects.readFromNBT(tag);
        final byte[] sides = tag.getByteArray("open");
        if (sides.length == 6) {
            for (int a = 0; a < 6; a++) {
                this.openSides[a] = sides[a] == 1;
            }
        }
        final byte[] choke = tag.getByteArray("choke");
        if (choke.length >= 6) {
            System.arraycopy(choke, 0, this.chokedSides, 0, 6);
        } else {
            java.util.Arrays.fill(this.chokedSides, (byte) 0);
        }
    }
}
