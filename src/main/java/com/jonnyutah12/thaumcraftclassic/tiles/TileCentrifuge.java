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
import java.util.List;

/**
 * The alchemical centrifuge (content-mapping.csv {@code thaumcraft:tilecentrifuge},
 * blockTube metadata 2) — the machine the original hosted on the tube block
 * (TC-M4-07-m).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileCentrifuge} (a
 * {@code TileThaumcraft} implementing {@code IAspectContainer} +
 * {@code IEssentiaTransport}, {@code canUpdate()} true). It sucks one unit of
 * a <em>compound</em> aspect up out of the tile below (input face DOWN, output
 * face UP, TileCentrifuge.java:115-127), spins it for
 * {@value CentrifugeLogic#PROCESS_TICKS} ticks, then splits it into one of its
 * two components by coin-flip ({@code getComponents()[rand.nextInt(2)]},
 * line 217) and hands that unit out through the top. Redstone power stops the
 * whole machine (the original's {@code gettingPower} gate). Every rule lives in
 * {@link CentrifugeLogic}, so {@code CentrifugeLogicTest} pins the numbers
 * without a {@code Level}; this class walks the faces, applies the results and
 * persists under the original NBT keys ({@code aspectIn}/{@code aspectOut}/
 * {@code facing}).</p>
 *
 * <p>The container surface (the original's {@code IAspectContainer}) is the
 * port's plain-method convention (no standalone interface; the
 * {@code TileJarFillable} / {@code TileTubeBuffer} precedent), reproduced
 * term-for-term including the original's bare-reference aspect comparisons
 * ({@code tag == aspectOut} with {@code tag} null reports a hit on an empty
 * container — the {@link TubeTransportLogic#identical} reproduction).</p>
 *
 * <p>Out of scope, with cards: the renderer (the spinning top, TC-M7-01) and
 * the pump sound the original fires when {@code rotation} crosses 180°
 * (TC-M7-05). The client-half fields and their ramp
 * ({@link #clientTick()}) are ported so the renderer card drives the real
 * state.</p>
 */
public class TileCentrifuge extends BlockEntity implements IEssentiaTransport {

    /** The split unit waiting on the output face, or null when empty. */
    @Nullable
    public Aspect aspectOut;

    /** The compound aspect under spin-up, or null when idle. */
    @Nullable
    public Aspect aspectIn;

    /**
     * Original {@code facing}: the centrifuge's own orientation field. The
     * original never rotates it (no wand seam landed) — the draw pass always
     * uses DOWN — and persists it, so the port keeps it verbatim.
     */
    public Direction facing = Direction.NORTH;

    /** Original {@code count}: advances only while both slots are empty. */
    int count = 0;

    /** Original {@code process}: the spin-up countdown. */
    int process = 0;

    /** Original client fields: the renderer's animation state (TC-M7-01). */
    public float rotationSpeed = 0.0F;
    public float rotation = 0.0F;

    public TileCentrifuge(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_CENTRIFUGE.get(), pos, state);
    }

    // --- the machine pass (original updateEntity, server half) -------------

    /**
     * Original {@code updateEntity} server branch (TileCentrifuge.java:183-196):
     *
     * <pre>
     * if (!gettingPower()) {
     *     if (aspectOut == null &amp;&amp; aspectIn == null &amp;&amp; ++count % 5 == 0) drawEssentia();
     *     if (process &gt; 0) process--;
     *     if (aspectOut == null &amp;&amp; aspectIn != null &amp;&amp; process == 0) processEssentia();
     * }
     * </pre>
     *
     * <p>Note the short-circuit: {@code ++count} only runs while the machine is
     * idle, reproduced exactly.</p>
     */
    public void serverTick() {
        if (gettingPower()) {
            return;
        }
        if (CentrifugeLogic.drawPassRuns(aspectIn != null, aspectOut != null, ++count)) {
            drawEssentia();
        }
        if (process > 0) {
            process--;
        }
        if (CentrifugeLogic.splitRuns(aspectIn != null, aspectOut != null, process)) {
            processEssentia();
        }
    }

    /**
     * Original {@code processEssentia()} (lines 215-221): the coin-flip split,
     * then the input is spent.
     */
    void processEssentia() {
        final List<Aspect> comps = aspectIn.getComponents();
        aspectOut = CentrifugeLogic.split(comps, level.getRandom().nextInt(2));
        aspectIn = null;
        touch();
    }

    /**
     * Original {@code drawEssentia()} (lines 223-250): ask the tile below
     * (DOWN), take one unit of the compound it offers if this machine
     * out-sucks it, and start the spin.
     */
    void drawEssentia() {
        if (level == null) {
            return;
        }
        final BlockEntity te = EssentiaTransportHelper.getConnectableTile(
                level, worldPosition, CentrifugeLogic.INPUT_FACE);
        if (!(te instanceof IEssentiaTransport ic)) {
            return;
        }
        final Aspect ta = CentrifugeLogic.planDraw(getSuctionAmount(CentrifugeLogic.INPUT_FACE),
                new TubeTransportLogic.Neighbour(CentrifugeLogic.INPUT_FACE, ic));
        if (ta != null && ic.takeEssentia(ta, TubeTransportLogic.TRANSFER_PER_PASS,
                CentrifugeLogic.INPUT_FACE_OPPPOSITE) == TubeTransportLogic.TRANSFER_PER_PASS) {
            aspectIn = ta;
            process = CentrifugeLogic.PROCESS_TICKS;
            touch();
        }
    }

    // --- IEssentiaTransport (the contract the network speaks) --------------

    @Override
    public boolean isConnectable(final Direction face) {
        // TileCentrifuge.java:115-117: only the vertical pair.
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
    public void setSuction(@Nullable final Aspect aspect, final int amount) {
        // Original no-op: the centrifuge never adopts a line's suction.
    }

    @Override
    @Nullable
    public Aspect getSuctionType(final Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final Direction face) {
        return CentrifugeLogic.suction(face, gettingPower(), aspectIn != null);
    }

    @Override
    @Nullable
    public Aspect getEssentiaType(final Direction loc) {
        return aspectOut;
    }

    @Override
    public int getEssentiaAmount(final Direction loc) {
        return aspectOut != null ? 1 : 0;
    }

    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        // TileCentrifuge.java:164-166: out through UP only, exact aspect only.
        return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        // TileCentrifuge.java:169-179: the compound gate; the face is NOT gated
        // in the original and stays ungated here.
        if (CentrifugeLogic.addEssentiaAllowed(aspectIn, aspect)) {
            aspectIn = aspect;
            process = CentrifugeLogic.PROCESS_TICKS;
            touch();
            return 1;
        }
        return 0;
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
        if (aspectOut != null) {
            al.add(aspectOut, 1);
        }
        return al;
    }

    public void setAspects(@Nullable final AspectList aspects) {
        // Original no-op.
    }

    /**
     * Original {@code addToContainer} (lines 60-69): an empty output slot takes
     * exactly one unit; returns the leftover.
     */
    public int addToContainer(@Nullable final Aspect tt, final int am) {
        if (am > 0 && aspectOut == null) {
            aspectOut = tt;
            touch();
            return am - 1;
        }
        return am;
    }

    /**
     * Original {@code takeFromContainer(Aspect, int)} (lines 72-81): exact
     * aspect (the bare reference comparison — {@link TubeTransportLogic#identical}),
     * amount ignored.
     */
    public boolean takeFromContainer(@Nullable final Aspect tt, final int am) {
        if (aspectOut != null && TubeTransportLogic.identical(tt, aspectOut)) {
            aspectOut = null;
            touch();
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(@Nullable final AspectList ot) {
        return false;
    }

    /**
     * Original {@code doesContainerContainAmount} (line 90): {@code amt == 1 &&
     * tag == aspectOut} — the reference comparison, so a null tag on an empty
     * container answers true (reproduced via {@code identical}).
     */
    public boolean doesContainerContainAmount(@Nullable final Aspect tag, final int amt) {
        return amt == 1 && TubeTransportLogic.identical(tag, aspectOut);
    }

    public boolean doesContainerContain(@Nullable final AspectList ot) {
        if (ot == null) {
            return false;
        }
        for (final Aspect tt : ot.getAspects()) {
            if (TubeTransportLogic.identical(tt, aspectOut)) {
                return true;
            }
        }
        return false;
    }

    public int containerContains(@Nullable final Aspect tag) {
        return TubeTransportLogic.identical(tag, aspectOut) ? 1 : 0;
    }

    public boolean doesContainerAccept(final Aspect tag) {
        return true;
    }

    // --- world seams --------------------------------------------------------

    /**
     * Original {@code gettingPower()}: redstone powering stops the machine.
     * 1.21.1's equivalent of the original's {@code isBlockGettingPowered} is
     * {@code hasNeighborSignal} (the {@code TileArcaneBore} precedent).
     */
    public boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /**
     * Original {@code markDirtyForUpdate} + {@code markDirty} (the
     * {@code func_70296_d} + {@code sendBlockUpdated} pair the tile fires on
     * every content change).
     */
    public void touch() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Original {@code updateEntity}'s client branch (lines 197-212): the spin
     * ramps at +2/tick while a spin is running (cap 20) and decays at
     * −0.5/tick; the renderer reads {@link #rotation}. The sound the original
     * plays on a 180° crossing is TC-M7-05 and the wiring into the client
     * block-entity ticker is TC-M7-01 (which owns this tile's renderer); the
     * rule is kept here so that card drives real state, not a re-derivation.
     */
    public void clientTick() {
        if (aspectIn != null && !gettingPower() && rotationSpeed < 20.0F) {
            rotationSpeed += 2.0F;
        }
        if ((aspectIn == null || gettingPower()) && rotationSpeed > 0.0F) {
            rotationSpeed -= 0.5F;
        }
        rotation = rotation + rotationSpeed;
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

    /** Original {@code readCustomNBT} (lines 30-33). */
    public void readCustomNBT(final CompoundTag tag) {
        aspectIn = TileTube.aspectOrNull(tag.getString("aspectIn"));
        aspectOut = TileTube.aspectOrNull(tag.getString("aspectOut"));
        facing = TileTube.directionOr(tag.getInt("facing"), Direction.NORTH);
    }

    /** Original {@code writeCustomNBT} (lines 37-47). */
    public void writeCustomNBT(final CompoundTag tag) {
        if (aspectIn != null) {
            tag.putString("aspectIn", aspectIn.getTag());
        }
        if (aspectOut != null) {
            tag.putString("aspectOut", aspectOut.getTag());
        }
        tag.putInt("facing", facing.ordinal());
    }
}
