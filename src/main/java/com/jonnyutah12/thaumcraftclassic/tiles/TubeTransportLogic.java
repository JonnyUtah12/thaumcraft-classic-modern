package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntPredicate;

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
 * <p>TC-M4-07-f added the wand configuration's numbers to the same place: the
 * sub-hit box set of {@code TileTube}/{@code TileTubeBuffer.addTraceableCuboids}
 * ({@link #subHitSideBox(int)} / {@link #subHitCentreBox(boolean)}), the
 * centre-box face walk ({@link #nextFacing(int, IntPredicate)}) and the
 * buffer's choke cycle ({@link #nextChoke(int)}), so the click's geometry and
 * the figures it drives are pinned by the same unit test. The ray clipping
 * itself is {@link com.jonnyutah12.thaumcraftclassic.wands.WandRayTracer}.</p>
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

    // --- the wand sub-hit geometry (TC-M4-07-f) ---------------------------

    /** The sub-hit index of a tile's centre box: the original's {@code subHit == 6}. */
    public static final int SUBHIT_CENTRE = 6;

    /** Original {@code TileTube.addTraceableCuboids}: {@code float min = 0.42F}. */
    public static final double SUBHIT_SIDE_MIN = 0.42D;

    /** Original {@code TileTube.addTraceableCuboids}: {@code float max = 0.58F}. */
    public static final double SUBHIT_SIDE_MAX = 0.58D;

    /** Original {@code TileTube.addTraceableCuboids}: the centre box is {@code 0.34375 .. 0.65625}. */
    public static final double SUBHIT_TUBE_CENTRE_MIN = 0.34375D;

    /** Original {@code TileTube.addTraceableCuboids}: the centre box is {@code 0.34375 .. 0.65625}. */
    public static final double SUBHIT_TUBE_CENTRE_MAX = 0.65625D;

    /** Original {@code TileTubeBuffer.addTraceableCuboids}: the centre box is {@code 0.25 .. 0.75}. */
    public static final double SUBHIT_BUFFER_CENTRE_MIN = 0.25D;

    /** Original {@code TileTubeBuffer.addTraceableCuboids}: the centre box is {@code 0.25 .. 0.75}. */
    public static final double SUBHIT_BUFFER_CENTRE_MAX = 0.75D;

    /** Original {@code TileTube.onWandRightClick}: the tool click is 0.5 loud, 0.9 + rand·0.2. */
    public static final float SUBHIT_TOOL_VOLUME = 0.5F;

    /** Original {@code TileTube.onWandRightClick}: the tool click's pitch. */
    public static final float SUBHIT_TOOL_PITCH_BASE = 0.9F;

    /** Original {@code TileTubeBuffer.onWandRightClick}: the choke squeak is 0.6 loud, 1.1 + rand·0.2. */
    public static final float SUBHIT_SQUEAK_VOLUME = 0.6F;

    /** Original {@code TileTubeBuffer.onWandRightClick}: the choke squeak's pitch. */
    public static final float SUBHIT_SQUEAK_PITCH_BASE = 1.1F;

    /**
     * The original's per-side wand box, in <em>block-local</em> coordinates
     * (the tiles offset it by their own position when building the ray-trace
     * list, exactly as the original's {@code xTile + min} did). The index is a
     * face ordinal — 0 DOWN, 1 UP, 2 NORTH, 3 SOUTH, 4 WEST, 5 EAST — and each
     * box runs from the stub ({@code 0.42..0.58} across the other two axes) out
     * to the block edge on its own axis, so the ray can be aimed at the part of
     * the pipe that touches the neighbour.
     *
     * <p>Only the faces whose {@code canConnectSide} passed got a box in the
     * original, which is what makes the centre box reachable: aim at a face with
     * no transport neighbour and the ray has nothing to hit before the centre.</p>
     */
    public static AABB subHitSideBox(final int side) {
        final double min = SUBHIT_SIDE_MIN;
        final double max = SUBHIT_SIDE_MAX;
        return switch (side) {
            case 0 -> new AABB(min, 0.0D, min, max, 0.5D, max);
            case 1 -> new AABB(min, 0.5D, min, max, 1.0D, max);
            case 2 -> new AABB(min, min, 0.0D, max, max, 0.5D);
            case 3 -> new AABB(min, min, 0.5D, max, max, 1.0D);
            case 4 -> new AABB(0.0D, min, min, 0.5D, max, max);
            case 5 -> new AABB(0.5D, min, min, 1.0D, max, max);
            default -> throw new IllegalArgumentException("not a face ordinal: " + side);
        };
    }

    /**
     * The centre box every tube and buffer carries: the original's
     * {@code 0.34375 .. 0.65625} for a tube, {@code 0.25 .. 0.75} for a buffer.
     * Hitting it is sub-hit 6, which aims a directional tube / valve.
     */
    public static AABB subHitCentreBox(final boolean buffer) {
        final double min = buffer ? SUBHIT_BUFFER_CENTRE_MIN : SUBHIT_TUBE_CENTRE_MIN;
        final double max = buffer ? SUBHIT_BUFFER_CENTRE_MAX : SUBHIT_TUBE_CENTRE_MAX;
        return new AABB(min, min, min, max, max, max);
    }

    /**
     * Original {@code TileTube.onWandRightClick}'s centre-box walk, verbatim:
     *
     * <pre>
     * int a = this.facing.ordinal();
     * while (++a &lt; 20) {
     *    if (&lt;gate on ForgeDirection.getOrientation(a % 6)&gt;) { a %= 6; this.facing = ...; break; }
     * }
     * </pre>
     *
     * <p>The gate differs per tile — the base tube asks
     * {@code canConnectSide(candidate.getOpposite()) && isConnectable(candidate.getOpposite())},
     * the valve asks {@code !canConnectSide(candidate)} (its own override, with
     * no {@code getOpposite()} at all) — so the predicate is passed in and the
     * walk's shape stays in one place. The 20 bound is the original's: the walk
     * runs the ordinals past 5 (wrapping with {@code % 6}) and stops at 20, so
     * it inspects each face at most three times.</p>
     *
     * @param accept receives the <em>candidate</em> face ordinal (already
     *               {@code % 6}); the caller applies its own gate
     * @return the face to aim at, or {@code -1} when the walk found none (the
     *         original leaves {@code facing} alone in that case)
     */
    public static int nextFacing(final int fromOrdinal, final IntPredicate accept) {
        int a = fromOrdinal;
        while (++a < 20) {
            if (accept.test(a % 6)) {
                return a % 6;
            }
        }
        return -1;
    }

    /**
     * Original {@code TileTubeBuffer.onWandRightClick}'s choke cycle on a
     * sneak-use: {@code chokedSides[hit]++; if (chokedSides[hit] > 2)
     * chokedSides[hit] = 0;} — open (0) → bellows-ignored (1) → closed (2) → open.
     */
    public static int nextChoke(final int choke) {
        final int next = choke + 1;
        return next > CHOKE_OFF ? 0 : next;
    }

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
