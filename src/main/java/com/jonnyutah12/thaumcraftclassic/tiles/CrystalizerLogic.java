package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * The essentia crystallizer's conversion rules (TC-M4-07-m), as pure functions
 * over ints and aspects — the {@code CentrifugeLogic} /
 * {@link TubeTransportLogic} pattern, so {@code CrystalizerLogicTest} pins them
 * value for value without a {@code Level}.
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEssentiaCrystalizer}
 * (blockTube metadata 7). The machine accepts one unit of <em>any</em> aspect
 * through its facing face (suction 128 while empty, 64 once loaded), then every
 * fifth tick feeds it EARTH vis from the vis network:
 * {@code progress += 1 + drainVis * 2} with the drain request
 * {@code min(20, max(1, (progMax - progress) / 2))}. At {@code progress >= 200}
 * it ejects a crystal essence carrying that aspect — into a container touching
 * the face <em>opposite</em> the input, else as a world drop — and resets.</p>
 *
 * <p>The vis drain itself is a {@code Level} call
 * ({@link com.jonnyutah12.thaumcraftclassic.visnet.VisNetHandler#drainVis});
 * this class carries everything around it: the request figure, the advance
 * arithmetic, the completion gate, the suction and the reservoir plan.</p>
 */
public final class CrystalizerLogic {

    /** Original {@code progMax}, and the literal in the {@code >=} completion gate. */
    public static final int PROG_MAX = 200;

    /** Original {@code updateEntity}: the fill/progress pass runs on {@code ++count % 5 == 0}. */
    public static final int PASS_INTERVAL_TICKS = 5;

    /** Original {@code getSuctionAmount}: suction on the input face while empty. */
    public static final int SUCTION_EMPTY = 128;

    /** Original {@code getSuctionAmount}: 64 on every other face and once loaded. */
    public static final int SUCTION_OFF = 64;

    /** Original progress gate: every vis drain is capped at 20. */
    public static final int MAX_VIS_REQUEST = 20;

    /** Original progress gate: the drain request floors at 1 (progress never stalls). */
    public static final int MIN_VIS_REQUEST = 1;

    /** Original {@code updateEntity}: the constant part of every progress step. */
    public static final int BASE_PROGRESS = 1;

    /** Original {@code updateEntity}: each drained vis centi-unit is worth 2 progress. */
    public static final int PROGRESS_PER_VIS = 2;

    private CrystalizerLogic() {
    }

    /**
     * Original {@code getSuctionAmount(loc)} (TileEssentiaCrystalizer.java:175-177):
     * {@code gettingPower() ? 0 : (loc == facing && aspect == null ? 128 : 64)}.
     * Redstone power stops the machine — and its suction — entirely; the face
     * gate uses the machine's stored {@code facing}.
     */
    public static int suction(final Direction loc, final Direction facing, final boolean powered,
                              final boolean hasAspect) {
        if (powered) {
            return 0;
        }
        return loc == facing && !hasAspect ? SUCTION_EMPTY : SUCTION_OFF;
    }

    /**
     * Original {@code updateEntity}'s drain request (line 215):
     * {@code Math.min(20, Math.max(1, (200 - progress) / 2))} — integer
     * division, so the last stretch asks for 1.
     */
    public static int visRequest(final int progress) {
        return Math.min(MAX_VIS_REQUEST, Math.max(MIN_VIS_REQUEST, (PROG_MAX - progress) / 2));
    }

    /**
     * Original {@code updateEntity}'s advance (lines 207-217):
     * {@code progress = progress + 1 + drainedVis * 2} — 1 free progress even
     * with no vis net in range, the vis on top doubles.
     */
    public static int advance(final int progress, final int drainedVis) {
        return progress + BASE_PROGRESS + drainedVis * PROGRESS_PER_VIS;
    }

    /**
     * Original {@code updateEntity}'s completion gate (line 221):
     * {@code aspect != null && progress >= 200} → eject, clear, reset.
     */
    public static boolean isComplete(@Nullable final Aspect aspect, final int progress) {
        return aspect != null && progress >= PROG_MAX;
    }

    /**
     * Original {@code addToContainer(tt, am)} (lines 82-95): an add of 0 is a
     * no-op; the container holds exactly one aspect-unit, so a full container
     * hands the whole amount back and an empty one takes one.
     */
    public static int addLeftover(@Nullable final Aspect stored, final int amount) {
        if (amount == 0) {
            return amount;
        }
        return stored == null ? amount - 1 : amount;
    }

    /**
     * Original {@code addEssentia(aspect, amount, face)} (line 196):
     * {@code canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0}.
     */
    public static boolean inputFace(final Direction face, final Direction facing) {
        return face == facing;
    }

    /**
     * Original {@code fillReservoir()}'s decision (lines 358-377), one
     * neighbour at a time — the centrifuge's shape <em>without</em> the primal
     * gate (the crystallizer crystallises anything, primals included):
     *
     * <pre>
     * te = getConnectableTile(facing); ic = (IEssentiaTransport) te;
     * if (!ic.canOutputTo(facing.getOpposite())) return;
     * if (ic.getEssentiaAmount(opp) &gt; 0
     *     &amp;&amp; ic.getSuctionAmount(opp) &lt; this.getSuctionAmount(facing)
     *     &amp;&amp; this.getSuctionAmount(facing) &gt;= ic.getMinimumSuction())
     *     ta = ic.getEssentiaType(opp);
     * if (ta != null &amp;&amp; ic.getSuctionAmount(opp) &lt; this.getSuctionAmount(facing))
     *     addToContainer(ta, ic.takeEssentia(ta, 1, opp));
     * </pre>
     *
     * @return the aspect to take (one unit, {@code takeEssentia(ta, 1,
     *         facing.getOpposite())}), or {@code null}
     */
    @Nullable
    public static Aspect planFill(final Direction facing, final int ownSuction,
                                  final TubeTransportLogic.Neighbour neighbour) {
        final IEssentiaTransport ic = neighbour.transport();
        if (ic == null) {
            return null;
        }
        final Direction back = facing.getOpposite();
        if (!ic.canOutputTo(back)) {
            return null;
        }
        Aspect ta = null;
        if (ic.getEssentiaAmount(back) > 0
                && ic.getSuctionAmount(back) < ownSuction
                && ownSuction >= ic.getMinimumSuction()) {
            ta = ic.getEssentiaType(back);
        }
        if (ta == null || ic.getSuctionAmount(back) >= ownSuction) {
            return null;
        }
        return ta;
    }
}
