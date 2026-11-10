package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fillable essentia jar (TC-M4-06): port of {@code TileJarFillable}.
 *
 * <p>Single-aspect container with an optional aspect filter; connects to the
 * essentia network only through its top face and pulls one unit per 5 ticks
 * from whatever connectable tile sits above it. The original's {@code TileJar}
 * base class carries only render state (texture, render bounding box) which
 * the modern renderer will own, so the port collapses the hierarchy into this
 * class — the content-mapping row {@code TileJar} names this class.</p>
 *
 * <p>Persistence keeps the original NBT keys ("Aspect", "AspectFilter",
 * "Amount", "facing"); the facing byte uses the same 0..5 ordinal order as
 * the original's ForgeDirection. The filter is additionally exposed as the
 * {@code thaumcraft:aspect_filter} item component so a broken jar's filter
 * survives on the dropped item (the original copied it through the jar item's
 * NBT).</p>
 */
public class TileJarFillable extends BlockEntity implements IAspectSource, IEssentiaTransport {

    /** Original default: ForgeDirection.NORTH (ordinal 2). */
    public static final Direction DEFAULT_FACING = Direction.NORTH;

    public Aspect aspect = null;
    public Aspect aspectFilter = null;
    public int amount = 0;
    public int maxAmount = 64;
    public Direction facing = DEFAULT_FACING;
    int count = 0;

    public TileJarFillable(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------- container

    public AspectList getAspects() {
        final AspectList al = new AspectList();
        if (this.aspect != null && this.amount > 0) {
            al.add(this.aspect, this.amount);
        }
        return al;
    }

    public void setAspects(final AspectList aspects) {
        // Original TileJarFillable.setAspects is a no-op.
    }

    public int addToContainer(final Aspect tt, final int am) {
        if (am == 0) {
            return am;
        }
        if (this.amount < this.maxAmount && tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            final int added = Math.min(am, this.maxAmount - this.amount);
            this.amount += added;
            DeviceSync.sync(this);
            return am - added;
        }
        DeviceSync.sync(this);
        return am;
    }

    public boolean takeFromContainer(final Aspect tt, final int am) {
        if (this.amount >= am && tt == this.aspect) {
            this.amount -= am;
            if (this.amount <= 0) {
                this.aspect = null;
                this.amount = 0;
            }
            DeviceSync.sync(this);
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(final AspectList ot) {
        return false;
    }

    public boolean doesContainerContainAmount(final Aspect tag, final int amt) {
        return this.amount >= amt && tag == this.aspect;
    }

    public boolean doesContainerContain(final AspectList ot) {
        for (final Aspect tt : ot.getAspects()) {
            if (this.amount > 0 && tt == this.aspect) {
                return true;
            }
        }
        return false;
    }

    public int containerContains(final Aspect tag) {
        return 0;
    }

    public boolean doesContainerAccept(final Aspect tag) {
        return this.aspectFilter != null ? tag.equals(this.aspectFilter) : true;
    }

    // ------------------------------------------------------- aspect source

    @Override
    public int getAspectPoolSize() {
        return this.amount;
    }

    @Override
    public boolean doesContain(final Aspect aspect) {
        return this.amount > 0 && this.aspect != null && this.aspect == aspect;
    }

    @Override
    public int useAspect(final Aspect aspect, final int amount) {
        return this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    // ------------------------------------------------------------- transport

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
    public void setSuction(final Aspect aspect, final int amount) {
        // Original: no-op.
    }

    @Override
    public boolean renderExtendedTube() {
        return true;
    }

    @Override
    public int getMinimumSuction() {
        return this.aspectFilter != null ? 64 : 32;
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return this.aspectFilter != null ? this.aspectFilter : this.aspect;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return this.amount < this.maxAmount ? (this.aspectFilter != null ? 64 : 32) : 0;
    }

    @Override
    public Aspect getEssentiaType(final Direction face) {
        return this.aspect;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return this.amount;
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
    }

    // ----------------------------------------------------------------- tick

    public void tick() {
        final var level = getLevel();
        if (level != null && !level.isClientSide && ++this.count % 5 == 0 && this.amount < this.maxAmount) {
            this.fillJar();
        }
    }

    void fillJar() {
        final var level = getLevel();
        if (level == null) {
            return;
        }
        final IEssentiaTransport ic = EssentiaTransportHelper.getConnectableTile(level, this.worldPosition,
                Direction.UP);
        if (ic != null) {
            if (!ic.canOutputTo(Direction.DOWN)) {
                return;
            }

            Aspect ta = null;
            if (this.aspectFilter != null) {
                ta = this.aspectFilter;
            } else if (this.aspect != null && this.amount > 0) {
                ta = this.aspect;
            } else if (ic.getEssentiaAmount(Direction.DOWN) > 0
                    && ic.getSuctionAmount(Direction.DOWN) < this.getSuctionAmount(Direction.UP)
                    && this.getSuctionAmount(Direction.UP) >= ic.getMinimumSuction()) {
                ta = ic.getEssentiaType(Direction.DOWN);
            }

            if (ta != null && ic.getSuctionAmount(Direction.DOWN) < this.getSuctionAmount(Direction.UP)) {
                this.addToContainer(ta, ic.takeEssentia(ta, 1, Direction.DOWN));
            }
        }
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeJarNbt(tag);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readJarNbt(tag);
    }

    /** Original readCustomNBT keys. */
    void readJarNbt(final CompoundTag nbt) {
        this.aspect = Aspect.getAspect(nbt.getString("Aspect"));
        this.aspectFilter = Aspect.getAspect(nbt.getString("AspectFilter"));
        this.amount = nbt.getShort("Amount");
        this.facing = Direction.from3DData(nbt.getByte("facing"));
    }

    /** Original writeCustomNBT keys. */
    void writeJarNbt(final CompoundTag nbt) {
        if (this.aspect != null) {
            nbt.putString("Aspect", this.aspect.getTag());
        }
        if (this.aspectFilter != null) {
            nbt.putString("AspectFilter", this.aspectFilter.getTag());
        }
        nbt.putShort("Amount", (short) this.amount);
        nbt.putByte("facing", (byte) this.facing.get3DDataValue());
    }

    // --------------------------------------------------- item <-> block sync

    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder builder) {
        if (this.aspect != null && this.amount > 0) {
            builder.set(ModDataComponents.ESSENTIA_CONTAINER.get(), EssentiaContainer.of(this.aspect, this.amount));
        }
        if (this.aspectFilter != null) {
            builder.set(ModDataComponents.ASPECT_FILTER.get(), this.aspectFilter.getTag());
        }
    }

    @Override
    protected void applyImplicitComponents(final BlockEntity.DataComponentInput input) {
        final EssentiaContainer container = input.get(ModDataComponents.ESSENTIA_CONTAINER.get());
        if (container != null && !container.isEmpty()) {
            final AspectList list = container.toAspectList();
            final var aspects = list.getAspects();
            if (!aspects.isEmpty()) {
                this.aspect = aspects.get(0);
                this.amount = Math.min(list.getAmount(this.aspect), this.maxAmount);
            }
        }
        final String filterTag = input.get(ModDataComponents.ASPECT_FILTER.get());
        if (filterTag != null) {
            this.aspectFilter = Aspect.getAspect(filterTag);
        }
    }

    /** Registry lookup used by drop/label code paths for cross-card items. */
    static net.minecraft.world.item.Item itemFromId(final String namespace, final String path) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
