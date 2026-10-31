package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The essentia valve (content-mapping.csv {@code thaumcraft:tiletubevalve},
 * blockTube variant 1).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTubeValve extends TileTube}.
 * Three rules, all ported:</p>
 * <ul>
 *   <li>the valve is <b>not</b> connectable on its {@code facing} side — the
 *       valve sits across the line it gates, and the impedance is geometric,
 *       not just a suction trick;</li>
 *   <li>{@link #setSuction} is refused while the valve is closed, so a closed
 *       valve advertises suction 0 (and, because {@code calculateSuction}
 *       resets the fields and then applies through {@code setSuction}, it
 *       blanks rather than accumulating);</li>
 *   <li>redstone: a rising edge closes an open valve, a falling edge opens a
 *       closed one, checked every 5 ticks, with the previous state carried in
 *       {@code wasPoweredLastTick} (persisted as {@code hadpower}) so a world
 *       reload does not re-trigger the edge.</li>
 * </ul>
 *
 * <p>The original's client-side {@code rotation} lerp (the valve wheel
 * animation) and the {@code thaumcraft:squeek} sounds are the client/effect
 * passes: TC-M7-01 (renderer) and TC-M7-05 (sounds). The flow state itself, the
 * per-variant item label, and the manual right-click toggle (<code>BlockTube
 * .onBlockActivated</code> metadata 1) are ported — see
 * {@link com.jonnyutah12.thaumcraftclassic.blocks.BlockTube}. The valve's wand
 * interaction is the base tile's, with one override: its centre-box aim walk
 * accepts a face that has <em>no</em> transport neighbour (see
 * {@link #facingWalkAccepts(Direction)}), which is the original's
 * {@code TileTubeValve.onWandRightClick} (TC-M4-07-f).</p>
 */
public class TileTubeValve extends TileTube {

    /** Original {@code allowFlow}: the valve's open state, persisted as {@code flow}. */
    public boolean allowFlow = true;

    /** Original {@code wasPoweredLastTick}: the edge detector, persisted as {@code hadpower}. */
    public boolean wasPoweredLastTick;

    public TileTubeValve(final BlockPos pos, final BlockState state) {
        super(CatalogEssentiaTransport.TILE_TUBE_VALVE.get(), pos, state);
    }

    /**
     * Original {@code TileTubeValve.updateEntity} (server half) then the parent
     * pass. The edge check reads {@code count} <em>before</em> the parent
     * increments it, exactly as the original did.
     */
    @Override
    public void serverTick() {
        if (level != null && !level.isClientSide && count % TubeTransportLogic.EQUALIZE_INTERVAL_TICKS == 0) {
            final boolean gettingPower = gettingPower();
            if (wasPoweredLastTick && !gettingPower && !allowFlow) {
                allowFlow = true;
                touch();
            }
            if (!wasPoweredLastTick && gettingPower && allowFlow) {
                allowFlow = false;
                touch();
            }
            wasPoweredLastTick = gettingPower;
        }
        super.serverTick();
    }

    /** Original {@code isConnectable}: never on the facing side. */
    @Override
    public boolean isConnectable(final Direction face) {
        return face != facing && super.isConnectable(face);
    }

    /**
     * The valve's own centre-walk gate from its {@code onWandRightClick}
     * override: {@code !canConnectSide(getOrientation(a % 6).ordinal())} — the
     * face is accepted when there is <b>no</b> transport neighbour across it,
     * with no {@code getOpposite()} and no {@code isConnectable} term. That is
     * the valve's geometry: the wheel blocks the line it gates, so aiming walks
     * to a side the valve can actually close off.
     */
    @Override
    protected boolean facingWalkAccepts(final Direction candidate) {
        return !canConnectSide(candidate.ordinal());
    }

    /** Original {@code setSuction}: a closed valve refuses to advertise suction. */
    @Override
    public void setSuction(@Nullable final Aspect aspect, final int amount) {
        if (allowFlow) {
            super.setSuction(aspect, amount);
        }
    }

    /** Original {@code gettingPower()}: the valve is itself powered. */
    public boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("flow", this.allowFlow);
        tag.putBoolean("hadpower", this.wasPoweredLastTick);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.allowFlow = tag.getBoolean("flow");
        this.wasPoweredLastTick = tag.getBoolean("hadpower");
    }
}
