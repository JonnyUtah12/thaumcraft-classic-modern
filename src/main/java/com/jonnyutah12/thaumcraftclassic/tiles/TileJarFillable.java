package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The warded essentia jar (content-mapping.csv {@code thaumcraft:tilejar},
 * blockJar variant 0).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileJarFillable} (the original's
 * {@code TileJar} superclass carries nothing but the render texture and the
 * {@code canUpdate} contract, so the port collapses the hierarchy). It stores
 * one aspect, up to {@link JarStorageLogic#MAX_AMOUNT} units, and speaks both
 * sides of the essentia API: {@link IAspectSource} for consumers that scan for
 * sources — the runic matrix (TC-M4-04, live today) and the essentia mirror
 * (TC-M6-04d, which reaches a jar through {@link IEssentiaTransport}) — and
 * {@link IEssentiaTransport} for the tube network (TC-M4-07).</p>
 *
 * <p>Every rule (single-aspect matching, the label filter, the suction figures,
 * the redstone level, the 5-tick pull) is the original's and lives in
 * {@link JarStorageLogic} so JarStorageLogicTest pins it without a world; this
 * class only applies the results, persists them under the original NBT keys
 * ({@code Aspect}/{@code AspectFilter}/{@code Amount}/{@code facing}) and
 * touches the world.</p>
 *
 * <p>Client-side rendering (the liquid fill height and the glow), the jar's
 * placement sound and the spell particles stay on TC-M7-01.</p>
 */
public class TileJarFillable extends BlockEntity
        implements IAspectSource, IEssentiaTransport, BlockDeviceBase.TickingDeviceTile {

    @Nullable
    public Aspect aspect;
    @Nullable
    public Aspect aspectFilter;
    public int amount;
    /** Original {@code facing}: the labelled face, set from the placer's view. */
    public int facing = 2;

    int count = 0;

    public TileJarFillable(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaStorage.TILE_JAR.get(), pos, state);
    }

    protected TileJarFillable(final net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                              final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** The jar's capacity; the void jar shares it. */
    public int maxAmount() {
        return JarStorageLogic.MAX_AMOUNT;
    }

    /** True for the void jar, whose add rule clips instead of refusing. */
    public boolean voidJar() {
        return false;
    }

    // --- container side (original IAspectContainer surface) ---------------

    public AspectList getAspects() {
        final AspectList list = new AspectList();
        if (this.aspect != null && this.amount > 0) {
            list.add(this.aspect, this.amount);
        }
        return list;
    }

    public void setAspects(final AspectList aspects) {
        // Original setAspects is a no-op: the jar fills through addToContainer.
    }

    /** Original {@code addToContainer}: returns the units left over. */
    public int addToContainer(@Nullable final Aspect incoming, final int requested) {
        final JarStorageLogic.Add result = JarStorageLogic.addToContainer(
                tagOf(this.aspect), this.amount, maxAmount(), tagOf(incoming), requested, voidJar());
        if (result.aspectTag() != null) {
            this.aspect = Aspect.getAspect(result.aspectTag());
        }
        this.amount = result.amount();
        touch();
        return result.leftover();
    }

    /** Original {@code takeFromContainer(Aspect, int)}. */
    public boolean takeFromContainer(@Nullable final Aspect aspect, final int requested) {
        final JarStorageLogic.Take result = JarStorageLogic.takeFromContainer(
                tagOf(this.aspect), this.amount, tagOf(aspect), requested);
        this.aspect = result.aspectTag() == null ? null : Aspect.getAspect(result.aspectTag());
        this.amount = result.amount();
        if (result.taken()) {
            touch();
        }
        return result.taken();
    }

    public boolean doesContainerAccept(@Nullable final Aspect tag) {
        return JarStorageLogic.doesContainerAccept(tagOf(this.aspectFilter), tagOf(tag));
    }

    public boolean doesContainerContainAmount(@Nullable final Aspect tag, final int requested) {
        return JarStorageLogic.doesContainerContainAmount(tagOf(this.aspect), this.amount, tagOf(tag), requested);
    }

    public boolean doesContainerContain(final AspectList list) {
        if (list == null) {
            return false;
        }
        for (final Aspect aspect : list.getAspects()) {
            if (this.amount > 0 && aspect != null && aspect.equals(this.aspect)) {
                return true;
            }
        }
        return false;
    }

    /** Original {@code containerContains} always reported 0. */
    public int containerContains(@Nullable final Aspect tag) {
        return 0;
    }

    // --- IAspectSource: what EssentiaHandler drains (TC-M4-04 runic matrix) --

    @Override
    public int getAspectPoolSize() {
        return this.amount;
    }

    @Override
    public boolean doesContain(@Nullable final Aspect aspect) {
        return this.amount > 0 && aspect != null && aspect.equals(this.aspect);
    }

    @Override
    public int useAspect(@Nullable final Aspect aspect, final int requested) {
        return takeFromContainer(aspect, requested) ? requested : 0;
    }

    @Nullable
    public Aspect getAspect() {
        return this.aspect;
    }

    // --- IEssentiaTransport: the tube side (TC-M4-07 consumes this) --------

    /** The jar is connectable on its top face only (original). */
    @Override
    public boolean isConnectable(final Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void setSuction(@Nullable final Aspect aspect, final int amount) {
        // The jar never advertises a suction of its own (original no-op).
    }

    @Override
    public boolean renderExtendedTube() {
        return true;
    }

    @Override
    public int getMinimumSuction() {
        return JarStorageLogic.minimumSuction(this.aspectFilter != null);
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return this.aspectFilter != null ? this.aspectFilter : this.aspect;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return JarStorageLogic.suctionAmount(this.aspectFilter != null, this.amount, maxAmount());
    }

    @Override
    @Nullable
    public Aspect getEssentiaType(final Direction face) {
        return this.aspect;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return this.amount;
    }

    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
    }

    // --- ticking ----------------------------------------------------------

    @Override
    public void serverTick() {
        this.count++;
        if (JarStorageLogic.fillTick(this.count) && this.amount < maxAmount()) {
            fillJar();
        }
    }

    /**
     * Original {@code fillJar}: every 5 ticks pull one unit of the jar's own
     * (or filtered) aspect out of the connectable tile above it, when that
     * tile can output downward and advertises less suction than the jar.
     *
     * <p>The original resolves the connectable through
     * {@code ThaumcraftApiHelper.getConnectableTile}, which follows an
     * extended tube run across several cells; the tube network is TC-M4-07, so
     * the port reads the direct neighbour only and the multi-cell walk lands
     * with that card.</p>
     */
    void fillJar() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        final BlockEntity above = this.level.getBlockEntity(this.worldPosition.above());
        if (!(above instanceof IEssentiaTransport transport)) {
            return;
        }
        if (!transport.canOutputTo(Direction.DOWN)) {
            return;
        }
        Aspect target = null;
        if (this.aspectFilter != null) {
            target = this.aspectFilter;
        } else if (this.aspect != null && this.amount > 0) {
            target = this.aspect;
        } else if (transport.getEssentiaAmount(Direction.DOWN) > 0
                && transport.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP)
                && getSuctionAmount(Direction.UP) >= transport.getMinimumSuction()) {
            target = transport.getEssentiaType(Direction.DOWN);
        }
        if (target != null
                && transport.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP)) {
            final int taken = transport.takeEssentia(target, JarStorageLogic.FILL_PER_CYCLE, Direction.DOWN);
            if (taken > 0) {
                addToContainer(target, taken);
                if (above instanceof BlockEntity aboveBe) {
                    aboveBe.setChanged();
                }
            }
        }
    }

    /** Original {@code markDirtyForUpdate} + {@code markDirty}. */
    /** Public: the block-side interactions (label, dump) commit through this. */
    public void touch() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    static String tagOf(@Nullable final Aspect aspect) {
        return aspect == null ? null : aspect.getTag();
    }

    // --- persistence (original NBT keys) ----------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCustomNBT(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCustomNBT(tag);
    }

    /** Original {@code writeCustomNBT}: the four keys the original used. */
    public void writeCustomNBT(final CompoundTag tag) {
        if (this.aspect != null) {
            tag.putString("Aspect", this.aspect.getTag());
        }
        if (this.aspectFilter != null) {
            tag.putString("AspectFilter", this.aspectFilter.getTag());
        }
        tag.putShort("Amount", (short) this.amount);
        tag.putByte("facing", (byte) this.facing);
    }

    /** Original {@code readCustomNBT}. */
    public void readCustomNBT(final CompoundTag tag) {
        this.aspect = Aspect.getAspect(tag.getString("Aspect"));
        this.aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        this.amount = tag.getShort("Amount");
        this.facing = tag.getByte("facing");
    }
}
