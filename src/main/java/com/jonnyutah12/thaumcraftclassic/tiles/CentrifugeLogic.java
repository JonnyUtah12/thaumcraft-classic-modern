package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The alchemical centrifuge's conversion rules (TC-M4-07-m), as pure functions
 * over aspects and ints — the {@code JarStorageLogic} / {@code CrucibleStateMachine}
 * / {@link TubeTransportLogic} pattern, so {@code CentrifugeLogicTest} pins them
 * value for value without a {@code Level}.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileCentrifuge} (blockTube
 * metadata 2). The machine sucks one unit of a <em>compound</em> aspect up out
 * of the tile below it (its only input face is DOWN, output is UP), spins it up
 * for {@value #PROCESS_TICKS} ticks, then splits it into <em>one</em> of its
 * two components chosen by {@code rand.nextInt(2)}, and advertises that
 * component on its output face. The tile is the thin world-facing shell that
 * applies these results.</p>
 *
 * <p>Face note: the original's draw pass reaches {@code ForgeDirection.DOWN}
 * and asks the neighbour back across {@code UP} (TileCentrifuge.java:224-247);
 * 1.7.10 and 1.21.1 agree on the DOWN ordinal (see
 * {@link TubeTransportLogic} "Side indexing"), so the pass is
 * {@code DOWN}/{@code DOWN.getOpposite()} here.</p>
 */
public final class CentrifugeLogic {

    /** Original {@code addEssentia}/{@code drawEssentia}: the spin-up window. */
    public static final int PROCESS_TICKS = 39;

    /** Original {@code updateEntity}: the draw pass runs on {@code ++count % 5 == 0}. */
    public static final int DRAW_INTERVAL_TICKS = 5;

    /** Original {@code getSuctionAmount}: suction while the input slot is free. */
    public static final int SUCTION_EMPTY = 128;

    /** Original {@code getSuctionAmount}: suction while a spin is in progress. */
    public static final int SUCTION_BUSY = 64;

    /** Suction on any face that is not the input face. */
    public static final int SUCTION_OFF_FACE = 0;

    /** The input face (the machine sucks essentia up out of the tile below). */
    public static final Direction INPUT_FACE = Direction.DOWN;

    /** The output face (the split component leaves through the top). */
    public static final Direction OUTPUT_FACE = Direction.UP;

    /** The input face as the neighbour sees it: {@code DOWN.getOpposite()}. */
    public static final Direction INPUT_FACE_OPPPOSITE = INPUT_FACE.getOpposite();

    private CentrifugeLogic() {
    }

    /**
     * Original {@code getSuctionAmount(face)}:
     * {@code face == DOWN ? (gettingPower() ? 0 : (aspectIn == null ? 128 : 64)) : 0}.
     * Redstone power stops the machine — and its suction — entirely.
     */
    public static int suction(final Direction face, final boolean powered, final boolean hasInput) {
        if (face != INPUT_FACE) {
            return SUCTION_OFF_FACE;
        }
        if (powered) {
            return 0;
        }
        return hasInput ? SUCTION_BUSY : SUCTION_EMPTY;
    }

    /**
     * Original {@code addEssentia(aspect, amount, face)}'s gate
     * (TileCentrifuge.java:169-179): the input slot must be free and the aspect
     * must be a compound — the centrifuge splits compounds, primals have
     * nothing to split. (The original does <em>not</em> gate the face here;
     * reproduced as-is.)
     */
    public static boolean addEssentiaAllowed(@Nullable final Aspect input, final Aspect aspect) {
        return input == null && aspect != null && !aspect.isPrimal();
    }

    /**
     * Original {@code processEssentia()} (TileCentrifuge.java:215-221):
     * {@code aspectOut = aspectIn.getComponents()[world.rand.nextInt(2)]} — the
     * split is a coin-flip between the compound's two components, in
     * declaration order.
     *
     * @param index the original's {@code rand.nextInt(2)} result (0 or 1)
     */
    public static Aspect split(final List<Aspect> components, final int index) {
        return components.get(index);
    }

    /**
     * Original {@code drawEssentia()}'s decision (TileCentrifuge.java:223-250),
     * one neighbour at a time:
     *
     * <pre>
     * te = getConnectableTile(DOWN); ic = (IEssentiaTransport) te;
     * if (!ic.canOutputTo(UP)) return;
     * if (ic.getEssentiaAmount(UP) &gt; 0
     *     &amp;&amp; ic.getSuctionAmount(UP) &lt; this.getSuctionAmount(DOWN)
     *     &amp;&amp; this.getSuctionAmount(DOWN) &gt;= ic.getMinimumSuction())
     *     ta = ic.getEssentiaType(UP);
     * if (ta != null &amp;&amp; !ta.isPrimal()
     *     &amp;&amp; ic.getSuctionAmount(UP) &lt; this.getSuctionAmount(DOWN)
     *     &amp;&amp; ic.takeEssentia(ta, 1, UP) == 1) { aspectIn = ta; process = 39; }
     * </pre>
     *
     * <p>The suction comparison appears twice in the original and both copies
     * are kept. The machine's own suction is passed in (the tile reads it via
     * {@link #suction}), keeping this function {@code Level}-free.</p>
     *
     * @return the aspect to draw (the tile then takes exactly one unit through
     *         {@code takeEssentia(ta, 1, UP)}), or {@code null} when this
     *         neighbour offers nothing the machine will accept
     */
    @Nullable
    public static Aspect planDraw(final int ownSuction, final TubeTransportLogic.Neighbour neighbour) {
        final IEssentiaTransport ic = neighbour.transport();
        if (ic == null) {
            return null;
        }
        final Direction back = INPUT_FACE_OPPPOSITE;
        if (!ic.canOutputTo(back)) {
            return null;
        }
        Aspect ta = null;
        if (ic.getEssentiaAmount(back) > 0
                && ic.getSuctionAmount(back) < ownSuction
                && ownSuction >= ic.getMinimumSuction()) {
            ta = ic.getEssentiaType(back);
        }
        if (ta == null || ta.isPrimal()) {
            return null;
        }
        if (ic.getSuctionAmount(back) >= ownSuction) {
            return null;
        }
        return ta;
    }

    /**
     * Original {@code updateEntity}'s draw cadence
     * (TileCentrifuge.java:185-187): the pass runs — and the counter moves —
     * only while <em>both</em> slots are empty ({@code aspectOut == null &&
     * aspectIn == null && ++count % 5 == 0}); a busy machine does not draw.
     */
    public static boolean drawPassRuns(final boolean hasInput, final boolean hasOutput, final int countAfterIncrement) {
        return !hasInput && !hasOutput && countAfterIncrement % DRAW_INTERVAL_TICKS == 0;
    }

    /**
     * Original {@code updateEntity}'s split gate (TileCentrifuge.java:189-195):
     * the spin counter counts down every tick, and the split fires on the tick
     * it reaches 0 with an input present and no output in the way.
     */
    public static boolean splitRuns(final boolean hasInput, final boolean hasOutput, final int process) {
        return !hasOutput && hasInput && process == 0;
    }
}
