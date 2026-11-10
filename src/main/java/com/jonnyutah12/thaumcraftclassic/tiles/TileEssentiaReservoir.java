package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The essentia reservoir (TC-M4-06): port of {@code TileEssentiaReservoir}.
 * Multi-aspect container (cap 256 total vis) with a single wand-set input
 * face (default DOWN) that pulls one unit per 5 ticks from the connectable
 * tile on that face. The original's client-side colour-interpolation fields
 * are renderer state and are not ported; the ambient creak sound is kept.
 *
 * <p>The wand seam ({@code onWandRightClick}) is exposed as
 * {@link #setFacingFromWand} for TC-M6-01's wand to call.</p>
 */
public class TileEssentiaReservoir extends BlockEntity implements IAspectSource, IEssentiaTransport {

    public AspectList essentia = new AspectList();
    public int maxAmount = 256;
    public Direction facing = Direction.DOWN;
    int count = 0;

    public TileEssentiaReservoir(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------- container

    public AspectList getAspects() {
        return this.essentia;
    }

    public void setAspects(final AspectList aspects) {
        this.essentia = aspects.copy();
    }

    public int addToContainer(final Aspect tt, final int am) {
        if (am == 0) {
            return am;
        }
        int am2 = am;
        final int space = this.maxAmount - this.essentia.visSize();
        if (space >= am2) {
            this.essentia.add(tt, am2);
            am2 = 0;
        } else {
            this.essentia.add(tt, space);
            am2 -= space;
        }
        if (space > 0) {
            DeviceSync.sync(this);
        }
        return am2;
    }

    public boolean takeFromContainer(final Aspect tt, final int am) {
        if (this.essentia.getAmount(tt) >= am) {
            this.essentia.remove(tt, am);
            DeviceSync.sync(this);
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(final AspectList ot) {
        return false;
    }

    public boolean doesContainerContainAmount(final Aspect tag, final int amt) {
        return this.essentia.getAmount(tag) >= amt;
    }

    public boolean doesContainerContain(final AspectList ot) {
        for (final Aspect tt : ot.getAspects()) {
            if (this.essentia.getAmount(tt) < ot.getAmount(tt)) {
                return false;
            }
        }
        return true;
    }

    public int containerContains(final Aspect tag) {
        return this.essentia.getAmount(tag);
    }

    public boolean doesContainerAccept(final Aspect tag) {
        return true;
    }

    // ------------------------------------------------------- aspect source

    @Override
    public int getAspectPoolSize() {
        return this.essentia.visSize();
    }

    @Override
    public boolean doesContain(final Aspect aspect) {
        return this.essentia.getAmount(aspect) > 0;
    }

    @Override
    public int useAspect(final Aspect aspect, final int amount) {
        return this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    // ------------------------------------------------------------- transport

    @Override
    public boolean isConnectable(final Direction face) {
        return face == this.facing;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return face == this.facing;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return face == this.facing;
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
        return 24;
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return this.essentia.visSize() < this.maxAmount ? 24 : 0;
    }

    @Override
    public Aspect getEssentiaType(final Direction face) {
        final var aspects = this.essentia.getAspects();
        return this.essentia.visSize() > 0 && face == Direction.NONE && !aspects.isEmpty() ? aspects.get(0) : null;
    }

    @Override
    public int getEssentiaAmount(final Direction face) {
        return this.essentia.visSize();
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final Direction face) {
        return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final Direction face) {
        return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
    }

    // ----------------------------------------------------------------- wand

    /**
     * Port of the original {@code onWandRightClick}: sneak-click points the
     * input face at the clicked side, normal-click at its opposite. Called by
     * TC-M6-01's wand once it exists.
     */
    public int setFacingFromWand(final Player player, final Direction side) {
        this.facing = player.isShiftKeyDown() ? side : side.getOpposite();
        DeviceSync.sync(this);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return 0;
    }

    // ----------------------------------------------------------------- tick

    public void tick() {
        final var level = getLevel();
        if (level == null) {
            return;
        }
        this.count++;
        if (!level.isClientSide && this.count % 5 == 0 && this.essentia.visSize() < this.maxAmount) {
            this.fillReservoir();
        }
        if (level.isClientSide) {
            final int vs = this.essentia.visSize();
            if (vs > 0 && level.getRandom().nextInt(500 - vs) == 0) {
                level.playSound(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D, ModSounds.CREAK.get(), SoundSource.BLOCKS, 1.0F,
                        1.4F + level.getRandom().nextFloat() * 0.2F);
            }
        }
    }

    void fillReservoir() {
        final var level = getLevel();
        if (level == null) {
            return;
        }
        final IEssentiaTransport ic = EssentiaTransportHelper.getConnectableTile(level, this.worldPosition,
                this.facing);
        if (ic != null) {
            if (!ic.canOutputTo(this.facing.getOpposite())) {
                return;
            }

            Aspect ta = null;
            if (ic.getEssentiaAmount(this.facing.getOpposite()) > 0
                    && ic.getSuctionAmount(this.facing.getOpposite()) < this.getSuctionAmount(this.facing)
                    && this.getSuctionAmount(this.facing) >= ic.getMinimumSuction()) {
                ta = ic.getEssentiaType(this.facing.getOpposite());
            }

            if (ta != null && ic.getSuctionAmount(this.facing.getOpposite()) < this.getSuctionAmount(this.facing)) {
                this.addToContainer(ta, ic.takeEssentia(ta, 1, this.facing.getOpposite()));
            }
        }
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeReservoirNbt(tag);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readReservoirNbt(tag);
    }

    /** Original readCustomNBT keys. */
    void readReservoirNbt(final CompoundTag nbt) {
        this.essentia.readFromNBT(nbt);
        if (this.essentia.visSize() > this.maxAmount) {
            this.essentia = new AspectList();
        }
        this.facing = Direction.from3DData(nbt.getByte("face"));
    }

    /** Original writeCustomNBT keys. */
    void writeReservoirNbt(final CompoundTag nbt) {
        this.essentia.writeToNBT(nbt);
        nbt.putByte("face", (byte) this.facing.get3DDataValue());
    }
}
