package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The alembic (TC-M4-06): port of {@code TileAlembic}. Sits on top of an
 * alchemical furnace (smelter) and receives essentia the smelter pushes into
 * it directly (the smelter special-cases this tile; the generic transport
 * contract here is input-inert — {@code canInputFrom} is false and
 * {@code addEssentia} returns 0, exactly as in the original). Output is
 * allowed on every face except the one the wand pointed and DOWN.
 *
 * <p>The original has no tick. The wand seam ({@code onWandRightClick}) is
 * exposed as {@link #setFacingFromWand}; the wand item itself arrives with
 * TC-M6-01 and will call it. {@code aboveAlembic}/{@code aboveFurnace} are
 * render-only connection flags the original recomputed from the block below;
 * the blocks they key off ({@code blockStoneDevice}, {@code blockMetalDevice})
 * belong to TC-M6-04c, so the lookup is by id and stays dormant until those
 * land.</p>
 */
public class TileAlembic extends BlockEntity implements IEssentiaTransport {

    public Aspect aspect = null;
    public Aspect aspectFilter = null;
    public int amount = 0;
    public int maxAmount = 32;
    public Direction facing = TileJarFillable.DEFAULT_FACING;
    public boolean aboveAlembic = false;
    public boolean aboveFurnace = false;

    public TileAlembic(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------- container

    public AspectList getAspects() {
        return this.aspect != null ? new AspectList().add(this.aspect, this.amount) : new AspectList();
    }

    public void setAspects(final AspectList aspects) {
        // Original: no-op.
    }

    public int addToContainer(final Aspect tt, final int am) {
        int am2 = am;
        if (this.amount < this.maxAmount && tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            final int added = Math.min(am2, this.maxAmount - this.amount);
            this.amount += added;
            am2 -= added;
        }
        DeviceSync.sync(this);
        return am2;
    }

    public boolean takeFromContainer(final Aspect tt, final int am) {
        if (this.amount == 0 || this.aspect == null) {
            this.aspect = null;
            this.amount = 0;
        }
        if (this.aspect != null && this.amount >= am && tt == this.aspect) {
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

    public boolean doesContainerContain(final AspectList ot) {
        return this.amount > 0 && this.aspect != null && ot.getAmount(this.aspect) > 0;
    }

    public boolean doesContainerContainAmount(final Aspect tt, final int am) {
        return this.amount >= am && tt == this.aspect;
    }

    public int containerContains(final Aspect tt) {
        return tt == this.aspect ? this.amount : 0;
    }

    public boolean doesContainerAccept(final Aspect tag) {
        return true;
    }

    public boolean takeFromContainer(final AspectList ot) {
        return false;
    }

    // ----------------------------------------------------------------- wand

    /**
     * Port of the original {@code onWandRightClick}: a horizontal side click
     * re-points the output face; vertical clicks are ignored. The wand item
     * (TC-M6-01) will call this; until then it is the documented seam.
     */
    public int setFacingFromWand(final Player player, final Direction side) {
        if (side == Direction.UP || side == Direction.DOWN) {
            return 0;
        }
        this.facing = side;
        DeviceSync.sync(this);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return 0;
    }

    // ------------------------------------------------------------- transport

    @Override
    public boolean isConnectable(final Direction face) {
        return face != this.facing && face != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        return face != this.facing && face != Direction.DOWN;
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // Original: no-op.
    }

    @Override
    public Aspect getSuctionType(final Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return 0;
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
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return true;
    }

    // ------------------------------------------------------- render linkage

    /**
     * Recomputes the render-only connection flags from the block below, as the
     * original did on load and on data-packet receipt. The stone/metal device
     * blocks are TC-M6-04c content; the lookup is by id so this stays correct
     * once they land and is inert until then.
     */
    public void getAppearance() {
        this.aboveAlembic = false;
        this.aboveFurnace = false;
        final var level = getLevel();
        if (level == null) {
            return;
        }
        final BlockState below = level.getBlockState(this.worldPosition.below());
        final var belowBlock = below.getBlock();
        final var stoneDevice = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockstonedevice"));
        final var metalDevice = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic.MOD_ID, "blockmetaldevice"));
        // The original additionally required metadata 0 on the stone device and
        // a metadata match on the metal device; the modern property names are
        // TC-M6-04c's to define, so the metadata gate is deferred to that card.
        if (stoneDevice != null && belowBlock == stoneDevice) {
            this.aboveFurnace = true;
        }
        if (metalDevice != null && belowBlock == metalDevice) {
            this.aboveAlembic = true;
        }
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeAlembicNbt(tag);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readAlembicNbt(tag);
        getAppearance();
    }

    /** Original readCustomNBT keys. */
    void readAlembicNbt(final CompoundTag nbt) {
        this.facing = Direction.from3DData(nbt.getByte("facing"));
        this.aspectFilter = Aspect.getAspect(nbt.getString("AspectFilter"));
        final String tag = nbt.getString("aspect");
        if (!tag.isEmpty()) {
            this.aspect = Aspect.getAspect(tag);
        }
        this.amount = nbt.getShort("amount");
    }

    /** Original writeCustomNBT keys. */
    void writeAlembicNbt(final CompoundTag nbt) {
        if (this.aspect != null) {
            nbt.putString("aspect", this.aspect.getTag());
        }
        if (this.aspectFilter != null) {
            nbt.putString("AspectFilter", this.aspectFilter.getTag());
        }
        nbt.putShort("amount", (short) this.amount);
        nbt.putByte("facing", (byte) this.facing.get3DDataValue());
    }
}
