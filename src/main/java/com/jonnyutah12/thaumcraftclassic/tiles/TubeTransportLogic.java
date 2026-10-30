package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The essentia tube network's suction and flow rules (TC-M4-07), as pure
 * functions over aspects and ints — the {@code JarStorageLogic} /
 * {@code CrucibleStateMachine} pattern, so {@link com.jonnyutah12.thaumcraftclassic.tubes.TubeTransportLogicTest}
 * pins them value for value without a {@code Level}.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileTube} (the
 * {@code calculateSuction} / {@code checkVenting} / {@code equalizeWithNeighbours}
 * trio), {@code TileTubeValve} (the flow gate), {@code TileTubeFilter} and
 * {@code TileTubeRestrict} / {@code TileTubeOneway} (the
 * {@code calculateSuction} overrides) and {@code TileTubeBuffer} (its own
 * {@code getSuctionAmount} / {@code takeEssentia} / {@code fillBuffer}
 * rules). The tiles are the thin world-facing shells that apply the results.</p>
 *
 * <h2>Side indexing</h2>
 * <p>1.7.10's {@code ForgeDirection.getOrientation(i)} and 1.21.1's
 * {@link Direction} share both the order and the meaning of the six face
 * ordinals — 0 DOWN, 1 UP, 2 NORTH (−Z), 3 SOUTH (+Z), 4 WEST (−X), 5 EAST
 * (+X) — so a loop over {@code Direction.values()} visits the faces in exactly
 * the original's order and the NBT {@code side} ordinal is unchanged (D-001).
 * The original's seventh value, {@code ForgeDirection.UNKNOWN}, has no 1.21.1
 * counterpart; every place it was used only as a "the face does not matter"
 * argument to {@code getEssentiaType}/{@code getEssentiaAmount} (both of which
 * ignore their face in every ported implementation, exactly as they did in the
 * original). {@link #ANY} is the stand-in for that argument, and the call
 * sites that need the <em>tube's own</em> suction read its fields directly
 * through {@code TileTube} rather than going through a face at all.</p>
 *
 * <h2>Aspect comparison</h2>
 * <p>The original compared aspects with {@code ==}. 1.21.1 aspects are
 * registry singletons (see {@link Aspect}), so identity is still the
 * semantics; {@link #same} and {@link #identical} reproduce it over tags, and
 * they differ on the one case the original's reference comparison was used
 * for: {@link #same} reports {@code false} when either side is {@code null}
 * (every call site using it has the original's explicit
 * {@code x == null ||} disjunct in front of it), while {@link #identical}
 * reports {@code null == null} as {@code true} (the vent-conflict check, which
 * compared two possibly-null references directly).</p>
 */
public final class TubeTransportLogic {

    /** The "face does not matter" argument that replaces {@code ForgeDirection.UNKNOWN}. */
    public static final Direction ANY = Direction.UP;

    /** Original {@code TileTube}: suction is recalculated every 2 ticks. */
    public static final int SUCTION_INTERVAL_TICKS = 2;

    /** Original {@code TileTube}: the equalisation pass runs on {@code count % 5 == 0}. */
    public static final int EQUALIZE_INTERVAL_TICKS = 5;

    /** Original {@code TileTube.checkVenting}: a conflict blanks the line for 40 ticks. */
    public static final int VENT_TICKS = 40;

    /** Original {@code TileTubeBuffer.MAXAMOUNT}. */
    public static final int BUFFER_MAX_AMOUNT = 8;

    /** Original {@code TileTubeBuffer}: the fill pass runs on {@code count % 5 == 0}. */
    public static final int BUFFER_FILL_INTERVAL_TICKS = 5;

    /** Original {@code TileTubeBuffer}: the bellows rescan runs on {@code count % 20 == 0}. */
    public static final int BUFFER_BELLOWS_RESCAN_TICKS = 20;

    /** Original {@code TileTubeBuffer.getSuctionAmount}: {@code bellows * 32}. */
    public static final int BUFFER_BELLOWS_SUCTION = 32;

    /** Original {@code TileTubeBuffer.getSuctionAmount}: the idle figure. */
    public static final int BUFFER_IDLE_SUCTION = 1;

    /** Original choke value that shuts a buffer face off entirely. */
    public static final int CHOKE_OFF = 2;

    /** Original choke value that ignores the bellows on a buffer face. */
    public static final int CHOKE_RESTRICTED = 1;

    /** One unit per transfer: the original moved essentia one at a time. */
    public static final int TRANSFER_PER_PASS = 1;

    /** A neighbour across one face: the transport there, or null when not connectable. */
    public record Neighbour(Direction side, @Nullable IEssentiaTransport transport) {
    }

    /** What a tube should advertise after looking at one neighbour. */
    public record Suction(@Nullable Aspect type, int amount) {
    }

    /** A decided transfer: the aspect to move and the neighbour to move it from. */
    public record Pull(Aspect aspect, Neighbour neighbour) {
    }

    /** A buffer's other reachable faces, for its "a better sink exists" rule. */
    public record BufferNeighbour(int neighbourSuction, @Nullable Aspect neighbourSuctionType, int ownSuction) {
    }

    /**
     * Original {@code this.suctionType == other} / {@code == ic.getEssentiaType(...)}
     * comparisons: true only when both are present and the same aspect.
     */
    public static boolean same(@Nullable final Aspect a, @Nullable final Aspect b) {
        return a != null && b != null && a.getTag().equals(b.getTag());
    }

    /**
     * Original {@code this.suctionType != ic.getSuctionType(...)}: the plain
     * reference comparison, so two absent aspects are <em>equal</em>.
     */
    public static boolean identical(@Nullable final Aspect a, @Nullable final Aspect b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getTag().equals(b.getTag());
    }

    /**
     * Original {@code TileTube.calculateSuction}, one neighbour at a time.
     *
     * <pre>
     * if ((!directional || facing == loc.getOpposite()) &amp;&amp; isConnectable(loc)) {
     *    if ((filter == null || theirSuctionType == null || theirSuctionType == filter)
     *        &amp;&amp; (filter != null || essentiaAmount &lt;= 0 || theirSuctionType == null
     *                                       || essentiaType == theirSuctionType)
     *        &amp;&amp; (filter == null || essentiaAmount &lt;= 0 || essentiaType == null
     *                             || theirSuctionType == null || essentiaType == theirSuctionType)) {
     *       int suck = ic.getSuctionAmount(loc.getOpposite());
     *       if (suck &gt; 0 &amp;&amp; suck &gt; this.suction + 1) {
     *          Aspect st = ic.getSuctionType(loc.getOpposite());
     *          if (st == null) st = filter;
     *          this.setSuction(st, restrict ? suck / 2 : suck - 1);
     *       }
     *    }
     * }
     * </pre>
     *
     * <p>The three {@code filter} terms are the original's: a plain tube
     * ({@code filter == null}) only adopts a neighbour's suction when it is
     * empty or already carrying that very aspect, and a filtered tube
     * ({@code filter != null}) picks the neighbour's suction type only when the
     * neighbour is untyped, otherwise it keeps its filter. Note the
     * {@code ==} in the directional gate here and the {@code !=} in
     * {@link #planPull}: the original's two passes really do use
     * <em>opposite</em> senses, and that asymmetry is preserved.</p>
     *
     * @param currentSuction the tube's suction field <em>as of this iteration</em>
     *                       (the original read {@code this.suction} after each
     *                       {@code setSuction}, so the loop is stateful)
     * @return the suction to apply, or {@code null} when this neighbour does
     *         not raise it
     */
    @Nullable
    public static Suction suctionFromNeighbour(@Nullable final Aspect filter, final boolean restrict,
                                               final boolean directional, final Direction facing,
                                               @Nullable final Aspect essentiaType, final int essentiaAmount,
                                               final int currentSuction, final Neighbour neighbour) {
        final IEssentiaTransport ic = neighbour.transport();
        if (ic == null) {
            return null;
        }
        final Direction side = neighbour.side();
        if (directional && facing != side.getOpposite()) {
            return null;
        }
        final Aspect theirSuctionType = ic.getSuctionType(side.getOpposite());
        final boolean passesFilter =
                (filter == null || theirSuctionType == null || same(theirSuctionType, filter))
                        && (filter != null || essentiaAmount <= 0 || theirSuctionType == null
                        || same(essentiaType, theirSuctionType))
                        && (filter == null || essentiaAmount <= 0 || essentiaType == null
                        || theirSuctionType == null || same(essentiaType, theirSuctionType));
        if (!passesFilter) {
            return null;
        }
        final int suck = ic.getSuctionAmount(side.getOpposite());
        if (suck <= 0 || suck <= currentSuction + 1) {
            return null;
        }
        Aspect st = theirSuctionType;
        if (st == null) {
            st = filter;
        }
        return new Suction(st, restrict ? suck / 2 : suck - 1);
    }

    /**
     * Original {@code TileTube.checkVenting}'s conflict condition: the line
     * vents when this tube's suction matches (or almost matches) a neighbour's
     * suction of a <em>different</em> type — two consumers arguing over what
     * the line should carry.
     */
    public static boolean ventConflict(final int suction, @Nullable final Aspect suctionType,
                                       final int neighbourSuction, @Nullable final Aspect neighbourSuctionType) {
        return suction > 0
                && (neighbourSuction == suction || neighbourSuction == suction - 1)
                && !identical(suctionType, neighbourSuctionType);
    }

    /**
     * Original {@code TileTube.equalizeWithNeighbours}, one neighbour at a time:
     *
     * <pre>
     * if ((!directional || facing != loc.getOpposite()) &amp;&amp; isConnectable(loc)
     *     &amp;&amp; ic.canOutputTo(loc.getOpposite())
     *     &amp;&amp; (suctionType == null || suctionType == ic.getEssentiaType(loc.getOpposite())
     *                              || ic.getEssentiaType(loc.getOpposite()) == null)
     *     &amp;&amp; suctionAmount &gt; ic.getSuctionAmount(loc.getOpposite())
     *     &amp;&amp; suctionAmount &gt;= ic.getMinimumSuction()) {
     *    Aspect a = suctionType;
     *    if (a == null) { a = ic.getEssentiaType(loc.getOpposite());
     *                     if (a == null) a = ic.getEssentiaType(UNKNOWN); }
     *    int am = addEssentia(a, ic.takeEssentia(a, 1, loc.getOpposite()), loc);
     *    if (am &gt; 0) return;
     * }
     * </pre>
     *
     * <p>The caller only runs this while it is empty (the original checks
     * {@code essentiaAmount <= 0} once, outside its loop).</p>
     *
     * @return the pull to attempt, or {@code null} when this neighbour is not a
     *         source this tube out-sucks
     */
    @Nullable
    public static Pull planPull(final boolean directional, final Direction facing,
                                @Nullable final Aspect suctionType, final int suctionAmount,
                                final Neighbour neighbour) {
        final IEssentiaTransport ic = neighbour.transport();
        if (ic == null) {
            return null;
        }
        final Direction side = neighbour.side();
        if (directional && facing == side.getOpposite()) {
            return null;
        }
        if (!ic.canOutputTo(side.getOpposite())) {
            return null;
        }
        final Aspect theirEssentia = ic.getEssentiaType(side.getOpposite());
        if (!(suctionType == null || same(suctionType, theirEssentia) || theirEssentia == null)) {
            return null;
        }
        if (suctionAmount <= ic.getSuctionAmount(side.getOpposite())) {
            return null;
        }
        if (suctionAmount < ic.getMinimumSuction()) {
            return null;
        }
        Aspect a = suctionType;
        if (a == null) {
            a = theirEssentia;
            if (a == null) {
                // The original's last resort, ic.getEssentiaType(ForgeDirection.UNKNOWN):
                // every ported transport returns its held aspect for any face.
                a = ic.getEssentiaType(ANY);
            }
        }
        return a == null ? null : new Pull(a, neighbour);
    }

    /**
     * Original {@code TileTubeBuffer.getSuctionAmount}:
     * {@code chokedSides[loc] == 2 ? 0 : (bellows > 0 &amp;&amp; chokedSides[loc] != 1 ? bellows * 32 : 1)}.
     */
    public static int bufferSuction(final int bellows, final int choke) {
        if (choke == CHOKE_OFF) {
            return 0;
        }
        return bellows > 0 && choke != CHOKE_RESTRICTED ? bellows * BUFFER_BELLOWS_SUCTION : BUFFER_IDLE_SUCTION;
    }

    /**
     * Original {@code TileTubeBuffer.takeEssentia}'s guard: refuse when another
     * open face reaches a sink with more suction, so a buffer does not drain a
     * line its own better neighbour is pulling on.
     *
     * <pre>
     * if ((su == aspect || su == null) &amp;&amp; faceSuction &lt; sa &amp;&amp; this.getSuctionAmount(dir) &lt; sa) return 0;
     * </pre>
     *
     * @param others every other open face that reaches a transport
     */
    public static boolean bufferTakeBlocked(@Nullable final Aspect aspect, final int faceSuction,
                                            final List<BufferNeighbour> others) {
        if (aspect == null) {
            return false;
        }
        for (final BufferNeighbour other : others) {
            if ((other.neighbourSuctionType() == null || same(other.neighbourSuctionType(), aspect))
                    && faceSuction < other.neighbourSuction()
                    && other.ownSuction() < other.neighbourSuction()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Original {@code TileTubeBuffer.addToContainer}: only a single unit is
     * accepted, and only while the pool has room.
     *
     * @return the units left over (the original's {@code am})
     */
    public static int bufferAddLeftover(final int visSize, final int amount, final int maxAmount) {
        if (amount != TRANSFER_PER_PASS) {
            return amount;
        }
        return visSize < maxAmount ? 0 : amount;
    }

    /**
     * Original {@code TileTubeBuffer.fillBuffer}'s condition: pull from the
     * first face whose neighbour holds essentia, advertises less suction than
     * the buffer's own face, and is satisfied by a suction at least as strong
     * as its minimum.
     */
    public static boolean bufferFillAllowed(final int neighbourEssentiaAmount, final int neighbourSuction,
                                            final int ownSuction, final int neighbourMinimumSuction) {
        return neighbourEssentiaAmount > 0
                && neighbourSuction < ownSuction
                && ownSuction >= neighbourMinimumSuction;
    }

    /**
     * Original {@code TileTubeBuffer} as read by {@code BlockTube.getComparatorInputOverride}:
     * {@code floor(visSize / 8 * 14) + (visSize > 0 ? 1 : 0)}.
     */
    public static int bufferSignalStrength(final int visSize) {
        return (int) Math.floor((float) visSize / (float) BUFFER_MAX_AMOUNT * 14.0F) + (visSize > 0 ? 1 : 0);
    }

    private TubeTransportLogic() {
    }
}
