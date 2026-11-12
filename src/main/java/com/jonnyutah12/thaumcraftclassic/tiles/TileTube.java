package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaTransportHelper;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import com.jonnyutah12.thaumcraftclassic.wands.IWandable;
import com.jonnyutah12.thaumcraftclassic.wands.WandInteractionFeedback;
import com.jonnyutah12.thaumcraftclassic.wands.WandRayTracer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The essentia tube (content-mapping.csv {@code thaumcraft:tiletube}, blockTube
 * variant 0) — the base of the whole transport network (TC-M4-07).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTube} (a
 * {@code TileThaumcraft} with {@code canUpdate()} true). It carries at most
 * <em>one</em> unit of <em>one</em> aspect, advertises a suction type/amount
 * read off its neighbours, and every other tick ({@code ++count % 2 == 0})
 * recalculates that suction and vents on a conflict while every fifth tick
 * ({@code count % 5 == 0}) it tries to pull one unit from a neighbour it
 * out-sucks. All of those rules live in {@link TubeTransportLogic}, so
 * {@code TubeTransportLogicTest} pins them without a {@code Level}; this class
 * only walks the six faces, calls the rules and persists the result under the
 * original NBT keys ({@code type}/{@code amount}/{@code side}/{@code open} in
 * the custom compound, {@code stype}/{@code samount} beside it).</p>
 *
 * <p>The {@code openSides} array <em>is</em> the network's routing rule: a face
 * is only reachable when {@link #isConnectable(Direction)} says so, and
 * {@link EssentiaTransportHelper} asks the neighbour the same question about
 * the side facing back. In the original that array is flipped by a wand
 * right-click on the pipe's face — a codechicken ray-trace <em>sub-hit</em>,
 * one box per connectable side plus a centre box — and that interaction is
 * ported here (TC-M4-07-f): {@link #onWandRightClick} retraces the click
 * through {@link WandRayTracer}, flips the aimed face, and hands the same value
 * to the neighbour across it so a side opens and closes <em>from both
 * ends</em>. Hitting the centre box walks {@link #facing} forward, which is how
 * a directional tube is aimed.</p>
 *
 * <p>Out of scope, with their cards: the client-side vent particles and the
 * {@code thaumcraft:creak}/fizz sounds (TC-M7-01/TC-M7-05 — the venting
 * <em>counter</em>, which does gate the transport pass, is ported), and the
 * tube's extended-run renderer (TC-M7-01).</p>
 */
public class TileTube extends BlockEntity implements IEssentiaTransport, IWandable, WandRayTracer.Traceable {

    /** Original {@code freq}: the equalisation cadence constant. */
    public static final int FREQ = 5;

    /** The one aspect unit this tube is carrying, or null when empty. */
    @Nullable
    public Aspect essentiaType;

    /** How many units the tube holds: 0 or 1 (the original never stored more). */
    public int essentiaAmount;

    /** The suction aspect this tube advertises, or null when untyped. */
    @Nullable
    public Aspect suctionType;

    /** The suction strength this tube advertises. */
    public int suction;

    /** Original {@code facing}: the directional tube's chosen face, {@code ForgeDirection.NORTH} default. */
    public Direction facing = Direction.NORTH;

    /**
     * Original {@code openSides}, one flag per face in
     * {@code ForgeDirection.getOrientation} order (== {@link Direction}
     * ordinals). All six start open.
     */
    public final boolean[] openSides = {true, true, true, true, true, true};

    /** Original {@code venting}: ticks left on a vent, during which suction is frozen. */
    public int venting;

    /** Original {@code count}: the tick counter driving the 2- and 5-tick cadences. */
    protected int count;

    public TileTube(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    // --- the transport pass (original updateEntity) -----------------------

    /**
     * Original {@code TileTube.updateEntity}, server half. The client half drew
     * the vent particles and is TC-M7-01.
     *
     * <pre>
     * if (venting &gt; 0) venting--;
     * if (count == 0) count = rand.nextInt(10);
     * if (!world.isRemote) {
     *    if (venting &lt;= 0) {
     *       if (++count % 2 == 0) { calculateSuction(null, false, false); checkVenting();
     *                               if (essentiaType != null &amp;&amp; essentiaAmount == 0) essentiaType = null; }
     *       if (count % 5 == 0 &amp;&amp; suction &gt; 0) equalizeWithNeighbours(false);
     *    }
     * }
     * </pre>
     *
     * <p>Note that {@code venting > 0} freezes the whole pass (the counter is
     * still decremented), which is the original's way of letting two consumers
     * that disagree about the suction type settle.</p>
     */
    public void serverTick() {
        if (venting > 0) {
            venting--;
        }
        if (count == 0) {
            count = level == null ? 0 : level.getRandom().nextInt(10);
        }
        if (venting > 0) {
            return;
        }
        if (++count % TubeTransportLogic.SUCTION_INTERVAL_TICKS == 0) {
            calculateSuction(null, false, false);
            checkVenting();
            if (essentiaType != null && essentiaAmount == 0) {
                essentiaType = null;
            }
        }
        if (count % TubeTransportLogic.EQUALIZE_INTERVAL_TICKS == 0 && suction > 0) {
            equalizeWithNeighbours(false);
        }
    }

    /**
     * Original {@code calculateSuction(Aspect filter, boolean restrict, boolean directional)}:
     * reset, then let each face raise the figure through {@link #setSuction}.
     * The loop is stateful in the original (it re-reads {@code this.suction}),
     * so it stays stateful here: a valve whose flow is closed refuses the
     * {@code setSuction} and therefore also refuses the comparison, exactly as
     * the original's field read did.
     */
    void calculateSuction(@Nullable final Aspect filter, final boolean restrict, final boolean directional) {
        this.suction = 0;
        this.suctionType = null;
        for (final Direction side : Direction.values()) {
            final TubeTransportLogic.Suction result = TubeTransportLogic.suctionFromNeighbour(
                    filter, restrict, directional, this.facing, this.essentiaType, this.essentiaAmount,
                    this.suction, neighbour(side));
            if (result != null) {
                this.setSuction(result.type(), result.amount());
            }
        }
    }

    /**
     * Original {@code checkVenting}: a neighbouring consumer whose suction
     * matches ours but whose type does not is a conflict, so the line vents for
     * {@link TubeTransportLogic#VENT_TICKS} ticks.
     */
    void checkVenting() {
        if (level == null) {
            return;
        }
        for (final Direction side : Direction.values()) {
            final BlockEntity te = EssentiaTransportHelper.getConnectableTile(level, worldPosition, side);
            if (te instanceof IEssentiaTransport ic
                    && TubeTransportLogic.ventConflict(suction, suctionType,
                    ic.getSuctionAmount(side.getOpposite()), ic.getSuctionType(side.getOpposite()))) {
                // The original also fires a block event here (the fizz sound and
                // the vent particle colour). Both are client effects: TC-M7-01
                // (particles) and TC-M7-05 (sounds).
                this.venting = TubeTransportLogic.VENT_TICKS;
            }
        }
    }

    /**
     * Original {@code equalizeWithNeighbours(boolean directional)}: while empty,
     * pull one unit from the first face whose neighbour can output and is
     * out-sucked by this tube.
     */
    void equalizeWithNeighbours(final boolean directional) {
        if (essentiaAmount > 0) {
            return;
        }
        for (final Direction side : Direction.values()) {
            final TubeTransportLogic.Pull pull = TubeTransportLogic.planPull(
                    directional, this.facing, this.suctionType, this.suction, neighbour(side));
            if (pull == null) {
                continue;
            }
            final IEssentiaTransport source = pull.neighbour().transport();
            final int taken = source.takeEssentia(pull.aspect(), TubeTransportLogic.TRANSFER_PER_PASS,
                    side.getOpposite());
            if (addEssentia(pull.aspect(), taken, side) > 0) {
                // The original rolls nextInt(100) == 0 here for the creak sound
                // (level event 0); the sound is TC-M7-05.
                return;
            }
        }
    }

    /** The neighbour across {@code side}, as the rules want to see it. */
    protected final TubeTransportLogic.Neighbour neighbour(final Direction side) {
        final BlockEntity te = level == null ? null : EssentiaTransportHelper.getConnectableTile(level, worldPosition, side);
        return new TubeTransportLogic.Neighbour(side, te instanceof IEssentiaTransport transport ? transport : null);
    }

    // --- the wand configuration (original onWandRightClick, TC-M4-07-f) ---

    /**
     * Original {@code TileTube.canConnectSide(int)}: a transport tile sits
     * across {@code side}. The original used it for two things at once — it is
     * why a side box exists in {@link #addTraceableCuboids} (only a connectable
     * face got a box, so a wand ray aimed at a bare face reaches the centre box
     * instead) and it is the centre walk's gate.
     */
    public boolean canConnectSide(final int side) {
        if (level == null || side < 0 || side > 5) {
            return false;
        }
        final BlockEntity te = level.getBlockEntity(worldPosition.relative(Direction.values()[side]));
        return te instanceof IEssentiaTransport;
    }

    /**
     * Original {@code TileTube.addTraceableCuboids}: the six side stubs
     * ({@link TubeTransportLogic#SUBHIT_SIDE_MIN}..{@link
     * TubeTransportLogic#SUBHIT_SIDE_MAX} across the other axes, out to the
     * block edge on their own) in face-ordinal order, each only when
     * {@link #canConnectSide(int)} passes, then the centre box
     * ({@link TubeTransportLogic#SUBHIT_TUBE_CENTRE_MIN}..{@link
     * TubeTransportLogic#SUBHIT_TUBE_CENTRE_MAX}) at index
     * {@link TubeTransportLogic#SUBHIT_CENTRE}. World-space, like the
     * original's {@code xTile + min} boxes — the order matters: the
     * ray-tracer keeps the first box on an exact tie.
     */
    @Override
    public void addTraceableCuboids(final List<WandRayTracer.IndexedBox> cuboids) {
        for (final Direction side : Direction.values()) {
            if (canConnectSide(side.ordinal())) {
                cuboids.add(new WandRayTracer.IndexedBox(side.ordinal(),
                        TubeTransportLogic.subHitSideBox(side.ordinal()).move(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())));
            }
        }
        cuboids.add(new WandRayTracer.IndexedBox(TubeTransportLogic.SUBHIT_CENTRE,
                TubeTransportLogic.subHitCentreBox(false).move(
                        worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())));
    }

    /**
     * Original {@code TileTube.onWandRightClick}: retrace the click into a
     * sub-hit and act on it.
     *
     * <ul>
     *   <li>sub-hit 0..5 — the tool click, then flip {@code openSides[subHit]}
     *       and <b>assign</b> the new value to the neighbouring {@link TileTube}
     *       across that face (its side pointing back), so the same face opens
     *       and closes from both ends. The original assigns rather than flips:
     *       two tubes that disagree about a shared face are pulled back into
     *       agreement by the next click from either side.</li>
     *   <li>sub-hit 6 (the centre box) — walk {@link #facing} forward through
     *       {@link TubeTransportLogic#nextFacing} with
     *       {@link #facingWalkAccepts(Direction)} as the gate.</li>
     *   <li>no box hit — nothing, the original's {@code hit == null} return.</li>
     * </ul>
     *
     * <p>1.21.1 splits the two sides: the state moves on the server only and
     * {@link #touch()} syncs it (the original flipped its client copy in place,
     * because the wand's {@code onItemUseFirst} ran on both sides).</p>
     */
    @Override
    public int onWandRightClick(final Level level, final ItemStack wandStack, final Player player,
                                final BlockPos pos, final Direction side, final int md) {
        final int hit = WandRayTracer.retraceBlock(level, player, worldPosition);
        if (hit < 0) {
            // The original's `if (hit == null) return 0;` — the tile answers the
            // wand (so the item consumes the click) and changes nothing.
            return 0;
        }
        if (hit < TubeTransportLogic.SUBHIT_CENTRE) {
            toolClick(level, player);
            touch();
            if (!level.isClientSide) {
                openSides[hit] = !openSides[hit];
            }
            final Direction dir = Direction.values()[hit];
            final BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
            if (te instanceof TileTube neighbour) {
                if (!level.isClientSide) {
                    neighbour.openSides[dir.getOpposite().ordinal()] = openSides[hit];
                    // The original: markBlockForUpdate on the neighbour + its markDirty.
                    neighbour.touch();
                }
            }
        }
        if (hit == TubeTransportLogic.SUBHIT_CENTRE) {
            toolClick(level, player);
            touch();
            final int next = TubeTransportLogic.nextFacing(facing.ordinal(),
                    candidate -> facingWalkAccepts(Direction.values()[candidate]));
            if (next >= 0 && !level.isClientSide) {
                facing = Direction.values()[next];
                touch();
            }
        }
        return 0;
    }

    /**
     * The centre walk's gate for this tile — the original's
     * {@code canConnectSide(getOrientation(a % 6).getOpposite().ordinal())
     * && isConnectable(getOrientation(a % 6).getOpposite())}, i.e. the walk
     * aims at {@code candidate} when the face <em>opposite</em> it is a live
     * connection. {@link TileTubeValve} replaces it with its own inverse gate.
     */
    protected boolean facingWalkAccepts(final Direction candidate) {
        final Direction towards = candidate.getOpposite();
        return canConnectSide(towards.ordinal()) && isConnectable(towards);
    }

    /** The original's {@code thaumcraft:tool} click: {@code 0.5F} / {@code 0.9F + rand·0.2F}. */
    protected void toolClick(final Level level, final Player player) {
        WandInteractionFeedback.toolClick(level, worldPosition, player);
    }

    @Override
    @Nullable
    public ItemStack onWandRightClick(final Level level, final ItemStack wandStack, final Player player) {
        // The original's drawing-the-wand hook: the tube family never replaces the wand.
        return null;
    }

    @Override
    public void onUsingWandTick(final ItemStack wandStack, final Player player, final int count) {
        // Original empty.
    }

    @Override
    public void onWandStoppedUsing(final ItemStack wandStack, final Level level, final Player player,
                                   final int count) {
        // Original empty.
    }

    // --- IEssentiaTransport (the contract the network speaks) --------------

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
        this.suctionType = aspect;
        this.suction = amount;
    }

    @Override
    @Nullable
    public Aspect getSuctionType(final Direction loc) {
        return this.suctionType;
    }

    @Override
    public int getSuctionAmount(final Direction loc) {
        return this.suction;
    }

    @Override
    @Nullable
    public Aspect getEssentiaType(final Direction loc) {
        return this.essentiaType;
    }

    @Override
    public int getEssentiaAmount(final Direction loc) {
        return this.essentiaAmount;
    }

    /**
     * Original {@code takeEssentia}: one unit, only for the exact aspect the
     * tube holds, only toward a face it can output on.
     */
    @Override
    public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        if (canOutputTo(face) && TubeTransportLogic.identical(this.essentiaType, aspect)
                && this.essentiaAmount > 0 && amount > 0) {
            this.essentiaAmount--;
            if (this.essentiaAmount <= 0) {
                this.essentiaType = null;
            }
            touch();
            return TubeTransportLogic.TRANSFER_PER_PASS;
        }
        return 0;
    }

    /**
     * Original {@code addEssentia}: one unit, only into an empty tube, only
     * through a face it can input from.
     */
    @Override
    public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
        if (canInputFrom(face) && this.essentiaAmount == 0 && amount > 0) {
            this.essentiaType = aspect;
            this.essentiaAmount++;
            touch();
            return TubeTransportLogic.TRANSFER_PER_PASS;
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

    /** Original {@code markDirtyForUpdate} + {@code markDirty}. */
    public void touch() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- persistence (the original NBT keys) ------------------------------

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCustomNBT(tag);
        if (this.suctionType != null) {
            tag.putString("stype", this.suctionType.getTag());
        }
        tag.putInt("samount", this.suction);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCustomNBT(tag);
        this.suctionType = aspectOrNull(tag.getString("stype"));
        this.suction = tag.getInt("samount");
    }

    /** Original {@code readCustomNBT}: the tube's content, facing and open faces. */
    public void readCustomNBT(final CompoundTag tag) {
        this.essentiaType = aspectOrNull(tag.getString("type"));
        this.essentiaAmount = tag.getInt("amount");
        this.facing = directionOr(tag.getInt("side"), Direction.NORTH);
        final byte[] sides = tag.getByteArray("open");
        if (sides.length == 6) {
            for (int a = 0; a < 6; a++) {
                this.openSides[a] = sides[a] == 1;
            }
        }
    }

    /** Original {@code writeCustomNBT}. */
    public void writeCustomNBT(final CompoundTag tag) {
        if (this.essentiaType != null) {
            tag.putString("type", this.essentiaType.getTag());
        }
        tag.putInt("amount", this.essentiaAmount);
        final byte[] sides = new byte[6];
        for (int a = 0; a < 6; a++) {
            sides[a] = (byte) (this.openSides[a] ? 1 : 0);
        }
        tag.putInt("side", this.facing.ordinal());
        tag.putByteArray("open", sides);
    }

    /** The original's {@code Aspect.getAspect(nbt.getString(...))}, absent-tag safe. */
    @Nullable
    static Aspect aspectOrNull(final String tag) {
        return tag == null || tag.isEmpty() ? null : Aspect.getAspect(tag);
    }

    /** The original stored the direction as {@code ForgeDirection.ordinal()}. */
    static Direction directionOr(final int ordinal, final Direction fallback) {
        final Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }
}
