package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectDefinitions;
import com.jonnyutah12.thaumcraftclassic.aspect.IEssentiaTransport;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M4-07 evidence: the essentia tube network's <b>suction and flow rules</b>,
 * pinned as the original's numbers, without a {@code Level}.
 *
 * <p>Reference: {@code reference/decompiled/thaumcraft/thaumcraft/common/tiles/}
 * — {@code TileTube.java} ({@code calculateSuction}, {@code checkVenting},
 * {@code equalizeWithNeighbours}), {@code TileTubeValve}, {@code TileTubeFilter},
 * {@code TileTubeRestrict}, {@code TileTubeOneway} and {@code TileTubeBuffer}.
 * Every assertion below cites the original expression it comes from; the rules
 * themselves live in {@link TubeTransportLogic}, which is what the tiles apply.</p>
 *
 * <p>Unit tests run without the game's {@code NewRegistryEvent}, so
 * {@code Aspect.getAspect} is null here; each aspect is built once from
 * {@link AspectDefinitions} (the {@code VisCostRulesTest}/{@code NodeBehaviorMathTest}
 * pattern). That also proves the port compares aspects by tag rather than by
 * object identity, which is what makes it robust across the registry.</p>
 */
class TubeTransportLogicTest {

    private static final Aspect AER = aspect("aer");
    private static final Aspect IGNIS = aspect("ignis");
    private static final Aspect TERRA = aspect("terra");

    private static Aspect aspect(final String tag) {
        return new Aspect(AspectDefinitions.all().stream()
                .filter(def -> def.tag().equals(tag))
                .findFirst()
                .orElseThrow());
    }

    /** A stand-in neighbour: the transport contract's observable state. */
    private static IEssentiaTransport fake(@Nullable final Aspect suctionType, final int suctionAmount,
                                           @Nullable final Aspect essentiaType, final int essentiaAmount,
                                           final boolean canOutput, final int minimumSuction) {
        return new IEssentiaTransport() {
            @Override
            public boolean isConnectable(final Direction face) {
                return true;
            }

            @Override
            public boolean canInputFrom(final Direction face) {
                return true;
            }

            @Override
            public boolean canOutputTo(final Direction face) {
                return canOutput;
            }

            @Override
            public void setSuction(@Nullable final Aspect aspect, final int amount) {
            }

            @Override
            @Nullable
            public Aspect getSuctionType(final Direction face) {
                return suctionType;
            }

            @Override
            public int getSuctionAmount(final Direction face) {
                return suctionAmount;
            }

            @Override
            public int getMinimumSuction() {
                return minimumSuction;
            }

            @Override
            @Nullable
            public Aspect getEssentiaType(final Direction face) {
                return essentiaType;
            }

            @Override
            public int getEssentiaAmount(final Direction face) {
                return essentiaAmount;
            }

            @Override
            public int takeEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
                return 0;
            }

            @Override
            public int addEssentia(@Nullable final Aspect aspect, final int amount, final Direction face) {
                return 0;
            }

            @Override
            public boolean renderExtendedTube() {
                return false;
            }
        };
    }

    private static TubeTransportLogic.Neighbour neighbour(final Direction side,
                                                          @Nullable final IEssentiaTransport transport) {
        return new TubeTransportLogic.Neighbour(side, transport);
    }

    /** One empty tube face looking at a jar-like consumer: suction 32, no essentia. */
    private static TubeTransportLogic.Neighbour emptyJarBelow() {
        return neighbour(Direction.DOWN, fake(null, 32, null, 0, true, 32));
    }

    // --- calculateSuction (TileTube.calculateSuction) ----------------------

    @Test
    void aPlainTubeAdoptsTheStrongestNeighboursSuctionMinusOne() {
        final TubeTransportLogic.Suction result = TubeTransportLogic.suctionFromNeighbour(
                null, false, false, Direction.NORTH, null, 0, 0, emptyJarBelow());
        assertNotNull(result, "a neighbour pulling at 32 must raise an empty tube's suction");
        assertEquals(31, result.amount(),
                "TileTube.calculateSuction: setSuction(st, restrict ? suck / 2 : suck - 1)");
        assertNull(result.type(),
                "the jar advertises no suction type, and a plain tube has no filter, so the tube stays untyped");
    }

    @Test
    void aNeighbourThatDoesNotBeatUsIsIgnored() {
        // TileTube.calculateSuction: if (suck > 0 && suck > this.suction + 1).
        assertNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, false, Direction.NORTH, null, 0, 31, emptyJarBelow()),
                "32 does not beat 31 + 1, so the running figure stands");
        assertNotNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, false, Direction.NORTH, null, 0, 30, emptyJarBelow()),
                "but it does beat 30 + 1");
        assertNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, false, Direction.NORTH, null, 0, 0,
                        neighbour(Direction.DOWN, fake(null, 0, null, 0, true, 0))),
                "suck <= 0 never raises suction");
    }

    @Test
    void aRestrictedTubeAdvertisesHalfOfIt() {
        final TubeTransportLogic.Suction result = TubeTransportLogic.suctionFromNeighbour(
                null, true, false, Direction.NORTH, null, 0, 0, emptyJarBelow());
        assertNotNull(result);
        assertEquals(16, result.amount(),
                "TileTubeRestrict passes restrict = true: 32 / 2");
    }

    @Test
    void aFilteredTubeKeepsItsOwnSuiteOfFilterRules() {
        // TileTubeFilter.calculateSuction passes this.aspectFilter as the filter.
        // A typed neighbour whose type differs from the filter is refused outright.
        assertNull(TubeTransportLogic.suctionFromNeighbour(
                        AER, false, false, Direction.NORTH, null, 0, 0,
                        neighbour(Direction.DOWN, fake(IGNIS, 64, null, 0, true, 64))),
                "the filter's first term: filter != null && theirSuctionType != null && != filter");
        // A typed neighbour that matches the filter is taken, and the filter is kept.
        final TubeTransportLogic.Suction matching = TubeTransportLogic.suctionFromNeighbour(
                AER, false, false, Direction.NORTH, null, 0, 0,
                neighbour(Direction.DOWN, fake(AER, 64, null, 0, true, 64)));
        assertNotNull(matching);
        assertEquals(AER, matching.type(), "a matching neighbour keeps the filter as the suction type");
        assertEquals(63, matching.amount());
        // An untyped neighbour lets the filter become the advertised type.
        final TubeTransportLogic.Suction untyped = TubeTransportLogic.suctionFromNeighbour(
                AER, false, false, Direction.NORTH, null, 0, 0, emptyJarBelow());
        assertNotNull(untyped);
        assertEquals(AER, untyped.type(), "if (st == null) st = filter");
        assertEquals(31, untyped.amount());
    }

    @Test
    void aTubeCarryingEssentiaOnlyTakesSuctionThatMatchesIt() {
        // TileTube.calculateSuction's second term, for a plain tube:
        // filter != null || essentiaAmount <= 0 || theirSuctionType == null || essentiaType == theirSuctionType
        assertNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, false, Direction.NORTH, AER, 1, 0,
                        neighbour(Direction.DOWN, fake(IGNIS, 64, AER, 1, true, 64))),
                "a tube full of aer must not be pulled toward a line asking for ignis");
        assertNotNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, false, Direction.NORTH, AER, 1, 0,
                        neighbour(Direction.DOWN, fake(AER, 64, AER, 1, true, 64))),
                "the same aspect passes");
        assertNotNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, false, Direction.NORTH, AER, 1, 0, emptyJarBelow()),
                "and an untyped neighbour passes too");
    }

    @Test
    void aDirectionalTubeOnlyReadsTheFaceOppositeItsFacing() {
        // TileTube.calculateSuction: (!directional || facing == loc.getOpposite())
        assertNotNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, true, Direction.UP, null, 0, 0, emptyJarBelow()),
                "TileTubeOneway with facing = UP reads the jar below (DOWN.getOpposite() == UP)");
        assertNull(TubeTransportLogic.suctionFromNeighbour(
                        null, false, true, Direction.NORTH, null, 0, 0, emptyJarBelow()),
                "with facing = NORTH the jar below is not read at all");
    }

    // --- checkVenting ------------------------------------------------------

    @Test
    void twoConsumersDisagreeingAboutTheTypeVentTheLine() {
        // TileTube.checkVenting: suction > 0 && (suck == suction || suck == suction - 1)
        //                        && suctionType != ic.getSuctionType(...)
        assertTrue(TubeTransportLogic.ventConflict(31, AER, 31, IGNIS),
                "equal suction, different type: the original vents");
        assertTrue(TubeTransportLogic.ventConflict(31, AER, 30, IGNIS),
                "one below also vents (the neighbour's own figure is one lower down the line)");
        assertFalse(TubeTransportLogic.ventConflict(31, AER, 30, AER),
                "same type: no conflict");
        assertFalse(TubeTransportLogic.ventConflict(31, AER, 29, IGNIS),
                "a neighbour two below is a different line, not a conflict");
        assertFalse(TubeTransportLogic.ventConflict(0, AER, 0, IGNIS),
                "a tube with no suction cannot vent");
        assertFalse(TubeTransportLogic.ventConflict(31, null, 31, null),
                "the original compared the two possibly-null references directly, so two absent types agree");
    }

    // --- equalizeWithNeighbours -------------------------------------------

    @Test
    void aTubePullsFromANeighbourItOutSucks() {
        final TubeTransportLogic.Pull pull = TubeTransportLogic.planPull(
                false, Direction.NORTH, null, 31,
                neighbour(Direction.DOWN, fake(null, 0, AER, 8, true, 0)));
        assertNotNull(pull, "a full jar (suction 0) below a tube pulling at 31 is a source");
        assertEquals(AER, pull.aspect(),
                "the tube has no suction type of its own, so it takes the neighbour's essentia type");
    }

    @Test
    void aTubeDoesNotPullFromAJarItCannotOutSuckOrReach() {
        // suctionAmount > ic.getSuctionAmount(...)
        assertNull(TubeTransportLogic.planPull(false, Direction.NORTH, null, 31,
                        neighbour(Direction.DOWN, fake(null, 32, AER, 8, true, 32))),
                "a jar with room advertises 32, which beats the 31 a tube derived from it");
        // suctionAmount >= ic.getMinimumSuction(): a jar's minimum is 32, so 31 is not enough.
        assertNull(TubeTransportLogic.planPull(false, Direction.NORTH, null, 31,
                        neighbour(Direction.DOWN, fake(null, 0, AER, 64, true, 32))),
                "the original jar's getMinimumSuction is 32 (64 labelled), so a plain tube can never tap a jar");
        // ic.canOutputTo(...)
        assertNull(TubeTransportLogic.planPull(false, Direction.NORTH, null, 31,
                        neighbour(Direction.DOWN, fake(null, 0, AER, 8, false, 0))),
                "a neighbour that cannot output is not a source");
        // A buffer, unlike a jar, advertises minimum suction 0 — so a tube does tap it.
        assertNotNull(TubeTransportLogic.planPull(false, Direction.NORTH, null, 30,
                        neighbour(Direction.DOWN, fake(null, 1, AER, 8, true, 0))),
                "an essentia buffer's getMinimumSuction is 0, so the line can pull from it");
    }

    @Test
    void aTubeWithATypedSuctionOnlyPullsThatAspect() {
        assertNull(TubeTransportLogic.planPull(false, Direction.NORTH, AER, 31,
                        neighbour(Direction.DOWN, fake(null, 0, IGNIS, 8, true, 0))),
                "the tube's suction type and the neighbour's essentia disagree, so the pull is refused");
        assertNotNull(TubeTransportLogic.planPull(false, Direction.NORTH, AER, 31,
                        neighbour(Direction.DOWN, fake(null, 0, AER, 8, true, 0))));
    }

    @Test
    void thePullPassIsDirectionalInTheOppositeSenseToTheSuctionPass() {
        // TileTube.equalizeWithNeighbours: (!directional || facing != loc.getOpposite())
        // — note the != here against the == in calculateSuction above.
        assertNull(TubeTransportLogic.planPull(true, Direction.UP, null, 31,
                        neighbour(Direction.DOWN, fake(null, 0, AER, 8, true, 0))),
                "a directional tube does not pull from the face opposite its facing");
        assertNotNull(TubeTransportLogic.planPull(true, Direction.NORTH, null, 31,
                        neighbour(Direction.DOWN, fake(null, 0, AER, 8, true, 0))),
                "but the other five faces are still sources");
    }

    // --- identity semantics ------------------------------------------------

    @Test
    void theTwoAspectComparisonsReproduceTheOriginalsNullBehaviour() {
        assertTrue(TubeTransportLogic.same(AER, aspect("aer")),
                "same() compares tags, so two Aspect objects with the same tag agree");
        assertFalse(TubeTransportLogic.same(AER, IGNIS));
        assertFalse(TubeTransportLogic.same(null, null),
                "same() is only used behind the original's explicit `x == null ||` terms");
        assertTrue(TubeTransportLogic.identical(null, null),
                "identical() carries the original's bare reference comparisons, where null == null");
        assertFalse(TubeTransportLogic.identical(null, AER));
        assertTrue(TubeTransportLogic.identical(AER, aspect("aer")));
    }

    // --- the stateful suction loop (TileTube.calculateSuction) ------------

    @Test
    void theSuctionLoopAccumulatesExactlyLikeTheOriginal() {
        // The original re-reads this.suction after every setSuction, so the
        // figure walked up is the strongest neighbour's, minus one, and a weaker
        // later face is rejected against the already-raised figure.
        final List<TubeTransportLogic.Neighbour> faces = new ArrayList<>(List.of(
                neighbour(Direction.DOWN, fake(null, 8, null, 0, true, 8)),
                neighbour(Direction.UP, fake(null, 32, null, 0, true, 32)),
                neighbour(Direction.NORTH, fake(null, 20, null, 0, true, 20))));
        int suction = 0;
        for (final TubeTransportLogic.Neighbour face : faces) {
            final TubeTransportLogic.Suction step = TubeTransportLogic.suctionFromNeighbour(
                    null, false, false, Direction.NORTH, null, 0, suction, face);
            if (step != null) {
                suction = step.amount();
            }
        }
        assertEquals(31, suction,
                "DOWN sets 7, UP raises it to 31, and NORTH's 20 no longer beats 31 + 1");
    }

    // --- the buffer (TileTubeBuffer) --------------------------------------

    @Test
    void aBuffersSuctionComesFromItsBellowsAndItsChoke() {
        // TileTubeBuffer.getSuctionAmount
        assertEquals(1, TubeTransportLogic.bufferSuction(0, 0),
                "no bellows: the original's idle figure of 1");
        assertEquals(1, TubeTransportLogic.bufferSuction(-1, 0),
                "the -1 sentinel before the first scan reads as no bellows");
        assertEquals(32, TubeTransportLogic.bufferSuction(1, 0), "one bellows: 1 * 32");
        assertEquals(96, TubeTransportLogic.bufferSuction(3, 0), "three bellows: 3 * 32");
        assertEquals(1, TubeTransportLogic.bufferSuction(3, TubeTransportLogic.CHOKE_RESTRICTED),
                "choke 1 ignores the bellows entirely");
        assertEquals(0, TubeTransportLogic.bufferSuction(3, TubeTransportLogic.CHOKE_OFF),
                "choke 2 shuts the face off");
        assertEquals(0, TubeTransportLogic.bufferSuction(0, TubeTransportLogic.CHOKE_OFF));
    }

    @Test
    void aBufferOnlyStartsFillingWhenTheNeighbourHoldsEssentiaAndLosesTheSuctionFight() {
        // TileTubeBuffer.fillBuffer
        assertTrue(TubeTransportLogic.bufferFillAllowed(8, 0, 1, 0),
                "a full jar (suction 0, minimum 0) feeds a buffer pulling at 1");
        assertTrue(TubeTransportLogic.bufferFillAllowed(8, 0, 32, 32),
                "a bellows-fed buffer at 32 also reaches the jar's 32 minimum");
        assertFalse(TubeTransportLogic.bufferFillAllowed(0, 0, 32, 0),
                "no essentia to take");
        assertFalse(TubeTransportLogic.bufferFillAllowed(8, 32, 1, 32),
                "a neighbour pulling as hard as the buffer is not a source");
        assertFalse(TubeTransportLogic.bufferFillAllowed(8, 64, 32, 64),
                "nor one pulling harder still");
    }

    @Test
    void aBufferRefusesToStripALineANeighbourIsBetterPlacedToServe() {
        // TileTubeBuffer.takeEssentia's guard.
        final List<TubeTransportLogic.BufferNeighbour> better = List.of(
                new TubeTransportLogic.BufferNeighbour(64, null, 1));
        assertTrue(TubeTransportLogic.bufferTakeBlocked(AER, 32, better),
                "a second face reaching a stronger sink takes priority");
        assertFalse(TubeTransportLogic.bufferTakeBlocked(AER, 64, better),
                "but the face being asked is already the strongest, so it may serve");
        assertFalse(TubeTransportLogic.bufferTakeBlocked(AER, 32, List.of()),
                "with no other open face there is nothing to defer to");

        final List<TubeTransportLogic.BufferNeighbour> otherAspect = List.of(
                new TubeTransportLogic.BufferNeighbour(64, IGNIS, 1));
        assertFalse(TubeTransportLogic.bufferTakeBlocked(AER, 32, otherAspect),
                "a face whose sink wants a different aspect does not beat the caller");
        assertTrue(TubeTransportLogic.bufferTakeBlocked(AER, 32,
                        List.of(new TubeTransportLogic.BufferNeighbour(64, AER, 1))),
                "an untyped sink and a matching one both count");
        final List<TubeTransportLogic.BufferNeighbour> notEnoughPull = List.of(
                new TubeTransportLogic.BufferNeighbour(32, null, 32));
        assertFalse(TubeTransportLogic.bufferTakeBlocked(AER, 32, notEnoughPull),
                "this buffer's own figure must be below the neighbour's too "
                        + "(this.getSuctionAmount(dir) < sa)");
    }

    @Test
    void aBufferSwallowsOneUnitAtATimeAndOnlyWhileItHasRoom() {
        // TileTubeBuffer.addToContainer: if (am != 1) return am; if (visSize < 8) {... return 0;} return am;
        assertEquals(0, TubeTransportLogic.bufferAddLeftover(0, 1, 8), "an empty buffer takes the unit");
        assertEquals(0, TubeTransportLogic.bufferAddLeftover(7, 1, 8), "the eighth unit still fits");
        assertEquals(1, TubeTransportLogic.bufferAddLeftover(8, 1, 8),
                "a full buffer refuses and the caller gets the unit back");
        assertEquals(3, TubeTransportLogic.bufferAddLeftover(0, 3, 8),
                "a multi-unit request is refused outright (the original only ever moves one)");
    }

    @Test
    void aBuffersComparatorReadIsItsPoolLevel() {
        // BlockTube.getComparatorInputOverride: floor(visSize / 8 * 14) + (visSize > 0 ? 1 : 0)
        assertEquals(0, TubeTransportLogic.bufferSignalStrength(0), "an empty buffer gives no signal");
        assertEquals(2, TubeTransportLogic.bufferSignalStrength(1),
                "one unit: floor(1 / 8 * 14) + 1 = 2");
        assertEquals(8, TubeTransportLogic.bufferSignalStrength(4),
                "half full: floor(4 / 8 * 14) + 1 = 8");
        assertEquals(15, TubeTransportLogic.bufferSignalStrength(8), "full: floor(14) + 1");
    }

    // --- sub-hit geometry (TC-M4-07-f) ------------------------------------

    @Test
    void theSubHitBoxesAreTheOnesTheOriginalRegistered() {
        // TileTube.addTraceableCuboids / TileTubeBuffer.addTraceableCuboids — the
        // boxes the wand's ray is clipped against, so the click geometry is pinned
        // here and the probes only have to prove the aim lands on them.
        assertEquals(6, TubeTransportLogic.SUBHIT_CENTRE, "the centre box is sub-hit 6, above the six faces");
        final net.minecraft.world.phys.AABB up = TubeTransportLogic.subHitSideBox(1);
        assertEquals(0.42D, up.minX, "the side boxes are 0.42..0.58 across the two axes they do not point along");
        assertEquals(0.58D, up.maxX, "0.42..0.58 in x");
        assertEquals(0.42D, up.minZ, "0.42..0.58 in z");
        assertEquals(0.58D, up.maxZ, "0.42..0.58 in z");
        assertEquals(1.0D, up.maxY, "the UP stub (ForgeDirection.UP = 1) runs to the top face");
        assertEquals(0.5D, up.minY, "and starts at the centre");
        final net.minecraft.world.phys.AABB down = TubeTransportLogic.subHitSideBox(0);
        assertEquals(0.0D, down.minY, "the DOWN stub (index 0) starts at the bottom face");
        assertEquals(0.5D, down.maxY, "and stops at the centre");
        assertEquals(0.0D, TubeTransportLogic.subHitSideBox(2).minZ, "the NORTH stub (index 2) sits on the -Z edge");
        assertEquals(0.5D, TubeTransportLogic.subHitSideBox(2).maxZ, "and reaches the centre");
        assertEquals(0.5D, TubeTransportLogic.subHitSideBox(3).minZ, "the SOUTH stub (index 3) starts at the centre");
        assertEquals(1.0D, TubeTransportLogic.subHitSideBox(3).maxZ, "and reaches the +Z edge");
        assertEquals(0.0D, TubeTransportLogic.subHitSideBox(4).minX, "the WEST stub (index 4) sits on the -X edge");
        assertEquals(0.5D, TubeTransportLogic.subHitSideBox(4).maxX, "and reaches the centre");
        assertEquals(0.5D, TubeTransportLogic.subHitSideBox(5).minX, "the EAST stub (index 5) starts at the centre");
        assertEquals(1.0D, TubeTransportLogic.subHitSideBox(5).maxX, "and reaches the +X edge");
        assertEquals(0.34375D, TubeTransportLogic.subHitCentreBox(false).minX, "the tube's centre box is 9/32..21/32");
        assertEquals(0.65625D, TubeTransportLogic.subHitCentreBox(false).maxY, "the tube's centre box is 9/32..21/32");
        assertEquals(0.25D, TubeTransportLogic.subHitCentreBox(true).minX, "the buffer's centre box is 0.25..0.75");
        assertEquals(0.75D, TubeTransportLogic.subHitCentreBox(true).maxZ, "the buffer's centre box is 0.25..0.75");
    }

    @Test
    void theCentreBoxIsWiderThanTheStubBandSoTheNearSideRaceIsPredictable() {
        // The probes aim the centre box from a corner (where the centre must win
        // a race against the stubs) and from a mid-band side (where the stub must
        // win), so this pins the two orderings they rely on.
        assertTrue(TubeTransportLogic.SUBHIT_TUBE_CENTRE_MIN < TubeTransportLogic.SUBHIT_SIDE_MIN,
                "the centre box reaches further out than the stub band, which is why a diagonal "
                        + "aim on a live face lands on the centre and a mid-band aim lands on the stub");
    }

    @Test
    void theAimWalkTakesTheNextSideUnderItsOwnGate() {
        // TileTube.onWandRightClick: while (++a < 20) if (gate(a % 6)) { facing = a % 6; break; }
        // Every side accepted, from facing NORTH (2): the walk is SOUTH (3).
        assertEquals(3, TubeTransportLogic.nextFacing(2, side -> true),
                "the walk starts at facing + 1, not facing itself");
        // Accepting only DOWN (0): the ordinals run 3,4,5,6(->0), so the first
        // accepted side is 0 — the walk wraps modulo 6 past the ordinal bound.
        assertEquals(0, TubeTransportLogic.nextFacing(2, side -> side == 0),
                "the walk wraps modulo 6 and takes the first side its gate accepts");
        // A gate that accepts nothing walks its full 20 steps and reports nothing.
        assertEquals(-1, TubeTransportLogic.nextFacing(2, side -> false),
                "no accepted side: facing stays where it was (-1 = no change)");
        // The valve's inverse gate (its own override, no getOpposite()): from
        // NORTH in a cage that is live through WEST, the first bare face is EAST.
        assertEquals(5, TubeTransportLogic.nextFacing(2, side -> side == 5),
                "the valve walk skips the live faces and lands on the bare one");
        // The 20 bound is the original's, not "one lap": from facing 5 the walk
        // inspects 6..19, i.e. ordinals 0..5 twice and 0..1 a third time.
        assertEquals(1, TubeTransportLogic.nextFacing(5, side -> side == 1),
                "a side accepted on the second lap is still found (the bound is 20, not 6)");
        assertEquals(4, TubeTransportLogic.nextFacing(5, side -> side == 4),
                "the walk wraps past 5 twice before the bound: ordinal 4 is reached at a = 10, so a "
                        + "side on the far side of the wrap is still aimed (this is the original's "
                        + "20, not a single lap)");
        // From facing 5, a gate that accepts only side 5 is met at a = 12: the
        // walk can therefore land back on the face it started from.
        assertEquals(5, TubeTransportLogic.nextFacing(5, side -> side == 5),
                "the walk is allowed to return to the facing it started from");
    }

    @Test
    void theChokeCycleIsZeroOneTwoAndWraps() {
        // TileTubeBuffer.onWandRightClick: chokedSides[subHit]++; if (chokedSides[subHit] > 2) = 0
        assertEquals(1, TubeTransportLogic.nextChoke(0), "open -> bellows-ignored");
        assertEquals(2, TubeTransportLogic.nextChoke(1), "bellows-ignored -> closed");
        assertEquals(0, TubeTransportLogic.nextChoke(2), "closed wraps to open (the > 2 reset)");
        assertEquals(0, TubeTransportLogic.nextChoke(3), "the reset also catches any value above 2");
    }
}
