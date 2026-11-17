package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaContainerItem;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogResources;
import com.jonnyutah12.thaumcraftclassic.visnet.VisNetHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.awt.Color;

/**
 * The essentia crystallizer (content-mapping.csv {@code thaumcraft:tileessentiacrystalizer},
 * blockTube metadata 7) — the second machine the original hosted on the tube
 * block (TC-M4-07-m).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEssentiaCrystalizer} (a
 * {@code TileThaumcraft} implementing {@code IAspectContainer} +
 * {@code IEssentiaTransport}, {@code canUpdate()} true). It takes one unit of
 * <em>any</em> aspect through its {@code facing} face (suction 128 empty / 64
 * loaded, and it never outputs), then every fifth tick feeds it EARTH vis from
 * the vis network — {@code progress += 1 + drainVis * 2}, request
 * {@code min(20, max(1, (200 - progress) / 2))} — and at {@code progress >= 200}
 * ejects an {@code itemcrystalessence} carrying that aspect into a container
 * touching the face opposite the input, else as a world push
 * ({@code +0.65} out, {@code 0.04/tick} of motion), clearing the machine.</p>
 *
 * <p>The arithmetic is {@link CrystalizerLogic} (unit-tested); the EARTH drain
 * goes through the ported {@link VisNetHandler#drainVis} with the original's
 * arguments (the vis net landed with TC-M4-02; the tree's vis sources land with
 * their own cards, so a world without nodes advances at the free 1/gate —
 * proven in the probe with a registered test source). Redstone power stops the
 * machine (the original's {@code gettingPower} gate). All rules cite the
 * original; the tile persists under the original keys ({@code Aspect}/{@code face}).</p>
 *
 * <p>Out of scope, with cards: the renderer (the spinning crystal emitter,
 * TC-M7-01) and the fizz on eject / vent particles (TC-M7-05/TC-M7-01); the
 * client-half fields and their ramp ({@link #clientTick()}) are ported so the
 * renderer card drives real state.</p>
 */
public class TileEssentiaCrystalizer extends BlockEntity implements IEssentiaTransport {

    /** The one aspect-unit under crystallisation, or null when empty. */
    @Nullable
    public Aspect aspect;

    /**
     * Original {@code facing}: the one face this machine connects through,
     * {@code ForgeDirection.DOWN} default (the original is placed facing-down
     * and the port keeps that default verbatim).
     */
    public Direction facing = Direction.DOWN;

    /** Original {@code count}: advances every tick. */
    int count = 0;

    /** Original {@code progress}: the crystallisation meter, 0..200. */
    public int progress = 0;

    /** Original client fields: spin, tint lerp and the vent countdown (TC-M7-01). */
    public float spin = 0.0F;
    public float spinInc = 0.0F;
    float tr = 1.0F;
    float tg = 1.0F;
    float tb = 1.0F;
    public float cr = 1.0F;
    public float cg = 1.0F;
    public float cb = 1.0F;
    public int venting = 0;

    public TileEssentiaCrystalizer(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_CRYSTALIZER.get(), pos, state);
    }

    // --- the machine pass (original updateEntity, server half) -------------

    /**
     * Original {@code updateEntity} server branch (TileEssentiaCrystalizer.java:201-227):
     *
     * <pre>
     * if (++count % 5 == 0 &amp;&amp; !gettingPower()) {
     *     if (aspect == null) { fillReservoir(); progress = 0; }
     *     else progress = progress + 1 + VisNetHandler.drainVis(world, pos, Aspect.EARTH,
     *                Math.min(20, Math.max(1, (200 - progress) / 2))) * 2;
     * }
     * if (aspect != null &amp;&amp; progress &gt;= 200) { eject(); aspect = null; progress = 0; markDirty...; }
     * </pre>
     *
     * <p>The counter advances every tick (unlike the centrifuge's idle-only
     * counter) — reproduced.</p>
     */
    public void serverTick() {
        if (++count % CrystalizerLogic.PASS_INTERVAL_TICKS == 0 && !gettingPower()) {
            if (aspect == null) {
                fillReservoir();
                progress = 0;
            } else {
                final int drained = level == null ? 0 : VisNetHandler.drainVis(
                        level.dimension(), worldPosition,
                        Aspect.getAspect("terra"), CrystalizerLogic.visRequest(progress));
                progress = CrystalizerLogic.advance(progress, drained);
            }
        }
        if (CrystalizerLogic.isComplete(aspect, progress)) {
            eject();
            aspect = null;
            progress = 0;
            touch();
        }
    }

    /**
     * Original {@code fillReservoir()} (lines 358-377): ask the tile across
     * {@code facing}, take one unit of whatever it offers (no primal gate —
     * the crystalliser crystallises anything).
     */
    void fillReservoir() {
        if (level == null) {
            return;
        }
        final BlockEntity te = EssentiaTransportHelper.getConnectableTile(level, worldPosition, facing);
        if (!(te instanceof IEssentiaTransport ic)) {
            return;
        }
        final Aspect ta = CrystalizerLogic.planFill(facing, getSuctionAmount(facing),
                new TubeTransportLogic.Neighbour(facing, ic));
        if (ta != null) {
            // The original: addToContainer(ta, ic.takeEssentia(ta, 1, opp)) —
            // a take of 0 leaves the container untouched (the am == 0 gate).
            addToContainer(ta, ic.takeEssentia(ta, TubeTransportLogic.TRANSFER_PER_PASS,
                    facing.getOpposite()));
        }
    }

    /**
     * Original {@code eject()} (lines 315-341): a crystal essence carrying the
     * stored aspect, pushed into the container across the face <em>opposite</em>
     * the input, else spawned as a world item at +0.65 out with 0.04/tick of
     * motion. The original's {@code random.fizz} is a client sound event
     * (TC-M7-05).
     */
    public void eject() {
        final ItemStack stack = new ItemStack(CatalogResources.ITEM_CRYSTAL_ESSENCE.get(), 1);
        ((IEssentiaContainerItem) stack.getItem()).setAspects(stack, new AspectList().add(aspect, 1));
        ItemStack remaining = stack;
        if (level != null) {
            final BlockEntity target = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
            if (target instanceof WorldlyContainer holder) {
                // Original InventoryUtils.placeItemStackIntoInventory(stack, inv,
                // facing.ordinal(), true) — the side passed is the machine's facing.
                remaining = insertInto(holder, stack, facing);
            }
            if (!remaining.isEmpty()) {
                spawnItem(remaining);
            }
        }
    }

    /**
     * Original {@code InventoryUtils.placeItemStackIntoInventory}: fill the
     * first accepting slot, return whatever does not fit (the
     * {@code TileArcaneBore.insertInto} shape).
     */
    private static ItemStack insertInto(final WorldlyContainer holder, final ItemStack stack,
                                        final Direction from) {
        final int[] slots = holder.getSlotsForFace(from);
        int slot = -1;
        for (final int candidate : slots) {
            if (holder.canPlaceItemThroughFace(candidate, stack, from)) {
                slot = candidate;
                break;
            }
        }
        if (slot < 0 || !holder.canPlaceItem(slot, stack)) {
            return stack;
        }
        final ItemStack remaining = stack.copy();
        final ItemStack existing = holder.getItem(slot);
        if (existing.isEmpty()) {
            holder.setItem(slot, remaining);
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(existing, remaining)) {
            final int space = existing.getMaxStackSize() - existing.getCount();
            final int moved = Math.min(space, remaining.getCount());
            if (moved > 0) {
                existing.grow(moved);
                remaining.shrink(moved);
                holder.setItem(slot, existing);
            }
        }
        return remaining;
    }

    /**
     * Original {@code spawnItem} (lines 343-356): the world push opposite the
     * input face.
     */
    public boolean spawnItem(final ItemStack stack) {
        final Direction out = facing.getOpposite();
        final ItemEntity item = new ItemEntity(level,
                worldPosition.getX() + 0.5D + out.getStepX() * 0.65D,
                worldPosition.getY() + 0.5D + out.getStepY() * 0.65D,
                worldPosition.getZ() + 0.5D + out.getStepZ() * 0.65D,
                stack);
        item.setDeltaMovement(out.getStepX() * 0.04D, out.getStepY() * 0.04D, out.getStepZ() * 0.04D);
        return level.addFreshEntity(item);
    }

    // --- IEssentiaTransport (the contract the network speaks) --------------

    @Override
    public boolean isConnectable(final Direction face) {
        return face == facing;
    }

    @Override
    public boolean canInputFrom(final Direction face) {
        return face == facing;
    }

    @Override
    public boolean canOutputTo(final Direction face) {
        // TileEssentiaCrystalizer.java:151-153: the machine never gives
        // essentia back to the line.
        return false;
    }

    @Override
    public void setSuction(@Nullable final Aspect aspect, final int amount) {
        // Original no-op.
    }

    @Override
    @Nullable
    public Aspect getSuctionType(final Direction loc) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction loc) {
        return CrystalizerLogic.suction(loc, facing, gettingPower(), aspect != null);
    }

    @Override
    @Nullable
    public Aspect getEssentiaType(final Direction loc) {
        return aspect;
    }

    @Override
    public int getEssentiaAmount(final Direction loc) {
        return aspect == null ? 0 : 1;
    }

    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        // TileEssentiaCrystalizer.java:190-192: once it is in, it does not come
        // back out through the line.
        return 0;
    }

    @Override
    public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        // TileEssentiaCrystalizer.java:195-197: face gate + container leftover.
        return CrystalizerLogic.inputFace(face, facing) ? amount - addToContainer(aspect, amount) : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // --- the container surface (the original's IAspectContainer) -----------

    public AspectList getAspects() {
        final AspectList al = new AspectList();
        if (aspect != null) {
            al.add(aspect, 1);
        }
        return al;
    }

    public void setAspects(@Nullable final AspectList aspects) {
        // Original no-op.
    }

    /**
     * Original {@code addToContainer(tt, am)} (lines 82-95): one unit into an
     * empty container; {@code am == 0} is a no-op. The stored aspect is not
     * checked (the original's shape — this container holds one unit, set once).
     */
    public int addToContainer(@Nullable final Aspect tt, final int am) {
        if (am == 0) {
            return am;
        }
        if (aspect == null) {
            aspect = tt;
            touch();
            return am - 1;
        }
        return am;
    }

    /**
     * Original {@code takeFromContainer(tt, am)} (lines 98-107): empties the
     * container for {@code am == 1} — the original does <em>not</em> compare
     * {@code tt} with the stored aspect (reproduced, quirk included).
     */
    public boolean takeFromContainer(@Nullable final Aspect tt, final int am) {
        if (aspect != null && am == 1) {
            aspect = null;
            touch();
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(@Nullable final AspectList ot) {
        return false;
    }

    /**
     * Original {@code doesContainerContainAmount} (lines 115-117): exactly one
     * of exactly the stored aspect (the reference comparison — {@code identical}).
     */
    public boolean doesContainerContainAmount(@Nullable final Aspect tag, final int amt) {
        return amt == 1 && aspect != null && TubeTransportLogic.identical(tag, aspect);
    }

    /**
     * Original {@code doesContainerContain(AspectList)} (lines 120-128): true
     * only when the list is exactly the stored single unit (every entry must
     * match and carry amount 1, and the container must be non-empty).
     */
    public boolean doesContainerContain(@Nullable final AspectList ot) {
        if (ot == null) {
            return false;
        }
        for (final Aspect tt : ot.getAspects()) {
            if (aspect == null || !TubeTransportLogic.identical(aspect, tt) || ot.getAmount(tt) != 1) {
                return false;
            }
        }
        return true;
    }

    public int containerContains(@Nullable final Aspect tag) {
        return aspect != null && TubeTransportLogic.identical(tag, aspect) ? 1 : 0;
    }

    public boolean doesContainerAccept(final Aspect tag) {
        return true;
    }

    // --- world seams --------------------------------------------------------

    /**
     * Original {@code gettingPower()}: redstone powering stops the machine
     * (the {@code TileArcaneBore} seam for the original's
     * {@code isBlockGettingPowered}).
     */
    public boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /**
     * Original {@code markDirtyForUpdate} + {@code markDirty} pair, fired on
     * every content change.
     */
    public void touch() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Original {@code onNeighborChange}-equivalent (lines 303-313): a neighbor
     * change on the client starts a 7-tick vent (the particle burst). The
     * particles are TC-M7-01; the counter is kept here so the renderer card
     * reads real state.
     */
    public void neighborChangedClient() {
        venting = 7;
    }

    /**
     * Original {@code updateEntity}'s client branch (lines 228-300): the tint
     * lerps toward the stored aspect's colour at 0.05/tick, the spin integrates
     * {@code spinInc} (ramp +0.1/tick while loaded and unpowered, cap 20;
     * decay −0.2/tick down to 0), and the vent countdown drives the vent
     * particles (TC-M7-01 draws them from {@link #venting}).
     */
    public void clientTick() {
        if (aspect == null) {
            tr = tg = tb = 1.0F;
        } else {
            final Color c = new Color(aspect.getColor());
            tr = c.getRed() / 220.0F;
            tg = c.getGreen() / 220.0F;
            tb = c.getBlue() / 220.0F;
        }
        if (cr < tr) {
            cr += 0.05F;
        }
        if (cr > tr) {
            cr -= 0.05F;
        }
        if (cg < tg) {
            cg += 0.05F;
        }
        if (cg > tg) {
            cg -= 0.05F;
        }
        if (cb < tb) {
            cb += 0.05F;
        }
        if (cb > tb) {
            cb -= 0.05F;
        }
        spin = spin + spinInc;
        if (spin > 360.0F) {
            spin -= 360.0F;
        }
        if (aspect != null && spinInc < 20.0F && !gettingPower()) {
            spinInc += 0.1F;
            if (spinInc > 20.0F) {
                spinInc = 20.0F;
            }
        } else if ((aspect == null || gettingPower()) && spinInc > 0.0F) {
            spinInc -= 0.2F;
            if (spinInc < 0.0F) {
                spinInc = 0.0F;
            }
        }
        if (venting > 0) {
            venting--;
            // The original draws the vent particles here (Thaumcraft.proxy.
            // drawVentParticles, lines 279-299) — client visuals, TC-M7-01.
        }
    }

    // --- persistence (the original NBT keys) --------------------------------

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

    /** Original {@code readCustomNBT} (lines 53-55): {@code Aspect} + {@code face}. */
    public void readCustomNBT(final CompoundTag tag) {
        aspect = TileTube.aspectOrNull(tag.getString("Aspect"));
        facing = TileTube.directionOr(tag.getByte("face"), Direction.DOWN);
    }

    /** Original {@code writeCustomNBT} (lines 59-64): {@code Aspect} + byte {@code face}. */
    public void writeCustomNBT(final CompoundTag tag) {
        if (aspect != null) {
            tag.putString("Aspect", aspect.getTag());
        }
        tag.putByte("face", (byte) facing.ordinal());
    }
}
