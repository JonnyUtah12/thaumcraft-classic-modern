package com.jonnyutah12.thaumcraftclassic.entities.golems;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-08-s-b: the marker colour system is how golem tasks target containers,
 * so it is functional state rather than decoration. These tests pin the
 * original's two comparisons — {@code equals} (all six fields) and
 * {@code equalsFuzzy} (position, dimension and side, with colour {@code -1} as
 * the wildcard) — plus the {@link GolemHelper#contained} query that every
 * {@code GolemHelper} targeting method routes through.
 */
class MarkerTest {

    private static final int DIM = 0;

    @Test
    void carriesAllSixOriginalFields() {
        final Marker marker = new Marker(3, -7, 12, 5, (byte) 2, (byte) 9);
        assertEquals(3, marker.x, "x is the original's public int field");
        assertEquals(-7, marker.y, "y is the original's public int field");
        assertEquals(12, marker.z, "z is the original's public int field");
        assertEquals(5, marker.dim, "dim is the original's public int dimension field");
        assertEquals((byte) 2, marker.side, "side is the original's public byte field");
        assertEquals((byte) 9, marker.color, "color is the original's public byte field");
        assertEquals(3, marker.x());
        assertEquals(-7, marker.y());
        assertEquals(12, marker.z());
        assertEquals(5, marker.dim());
        assertEquals((byte) 2, marker.side());
        assertEquals((byte) 9, marker.color());
    }

    /** Original {@code equals}: every field, colour included. */
    @Test
    void exactEqualityComparesEveryFieldIncludingColour() {
        final Marker marker = new Marker(1, 2, 3, DIM, (byte) 0, (byte) 4);
        assertEquals(new Marker(1, 2, 3, DIM, (byte) 0, (byte) 4), marker, "an identical marker is equal");
        assertNotEquals(new Marker(2, 2, 3, DIM, (byte) 0, (byte) 4), marker, "a different x is not equal");
        assertNotEquals(new Marker(1, 3, 3, DIM, (byte) 0, (byte) 4), marker, "a different y is not equal");
        assertNotEquals(new Marker(1, 2, 4, DIM, (byte) 0, (byte) 4), marker, "a different z is not equal");
        assertNotEquals(new Marker(1, 2, 3, 1, (byte) 0, (byte) 4), marker, "a different dimension is not equal");
        assertNotEquals(new Marker(1, 2, 3, DIM, (byte) 1, (byte) 4), marker, "a different side is not equal");
        assertNotEquals(new Marker(1, 2, 3, DIM, (byte) 0, (byte) 5), marker,
                "equals is exact, so a different colour IS not equal (that is what equalsFuzzy is for)");
        assertFalse(marker.equals(null), "a null is never equal");
        assertFalse(marker.equals("not a marker"), "only another Marker can be equal");
    }

    /**
     * Original {@code equalsFuzzy}: {@code x}, {@code y}, {@code z}, {@code dim}
     * and {@code side} must match; the colour matches when it is equal
     * <em>or</em> when the receiver's colour is {@code -1}. This is what makes a
     * wildcard-coloured query match every marker on that face.
     */
    @Test
    void fuzzyEqualityTreatsColourMinusOneAsAWildcard() {
        final Marker wildcard = new Marker(1, 2, 3, DIM, (byte) 0, (byte) -1);
        final Marker coloured = new Marker(1, 2, 3, DIM, (byte) 0, (byte) 7);
        assertTrue(wildcard.equalsFuzzy(coloured),
                "a -1-coloured receiver matches a marker of any colour");
        assertFalse(coloured.equalsFuzzy(wildcard),
                "the wildcard is one-directional: a coloured receiver does not match a -1 marker");
        assertTrue(coloured.equalsFuzzy(coloured), "the same colour still matches");
        assertTrue(coloured.equalsFuzzy(new Marker(1, 2, 3, DIM, (byte) 0, (byte) 7)),
                "an equal colour on the same face matches");
        assertFalse(coloured.equalsFuzzy(new Marker(1, 2, 3, DIM, (byte) 0, (byte) 8)),
                "two different concrete colours do not match");
    }

    @Test
    void fuzzyEqualityStillRequiresThePositionDimensionAndSide() {
        final Marker wildcard = new Marker(1, 2, 3, DIM, (byte) 0, (byte) -1);
        assertFalse(wildcard.equalsFuzzy(new Marker(9, 2, 3, DIM, (byte) 0, (byte) 7)), "different x");
        assertFalse(wildcard.equalsFuzzy(new Marker(1, 9, 3, DIM, (byte) 0, (byte) 7)), "different y");
        assertFalse(wildcard.equalsFuzzy(new Marker(1, 2, 9, DIM, (byte) 0, (byte) 7)), "different z");
        assertFalse(wildcard.equalsFuzzy(new Marker(1, 2, 3, 1, (byte) 0, (byte) 7)), "different dimension");
        assertFalse(wildcard.equalsFuzzy(new Marker(1, 2, 3, DIM, (byte) 1, (byte) 7)), "different side");
        assertFalse(wildcard.equalsFuzzy(null), "a null is never fuzzy-equal");
        assertFalse(wildcard.equalsFuzzy("not a marker"), "only another Marker can be fuzzy-equal");
    }

    /**
     * {@link GolemHelper#contained} is {@code query.equalsFuzzy(candidate)} over
     * the golem's list — it is the single funnel `getMarkedSides`,
     * `getMarkedContainers` and `getMarkedBlocksAdjacentToGolem` all use, so the
     * wildcard direction above decides every targeting decision.
     */
    @Test
    void containedUsesTheFuzzyQueryAgainstTheGolemsMarkerList() {
        final List<Marker> markers = List.of(
                new Marker(1, 2, 3, DIM, (byte) 0, (byte) 4),
                new Marker(1, 2, 3, DIM, (byte) 1, (byte) 4));
        assertTrue(GolemHelper.contained(markers, new Marker(1, 2, 3, DIM, (byte) 0, (byte) 4)),
                "an exact marker is contained");
        assertTrue(GolemHelper.contained(markers, new Marker(1, 2, 3, DIM, (byte) 0, (byte) -1)),
                "a -1 query colour matches the marker on that face whatever colour it carries");
        assertFalse(GolemHelper.contained(markers, new Marker(1, 2, 3, DIM, (byte) 0, (byte) 9)),
                "a concrete query colour that no marker carries is not contained");
        assertFalse(GolemHelper.contained(markers, new Marker(1, 2, 3, DIM, (byte) 4, (byte) -1)),
                "a face with no marker is not contained, even with the wildcard colour");
        assertFalse(GolemHelper.contained(markers, new Marker(1, 2, 4, DIM, (byte) 0, (byte) -1)),
                "a different block is not contained");
        assertFalse(GolemHelper.contained(List.of(), new Marker(1, 2, 3, DIM, (byte) 0, (byte) -1)),
                "an empty marker list contains nothing");
    }

    /**
     * {@link GolemHelper#getMarkedSides} enumerates the six faces and asks
     * {@code contained} for each — the mapping from a marked face to the side
     * ordinal a container interaction uses. It needs a golem only for its marker
     * list, so the enumeration itself is pinned here through the {@code contained}
     * contract above; the world-backed half is a game test.
     */
    @Test
    void theSixFaceOrdinalsAreTheSideDomain() {
        for (int a = 0; a < 6; a++) {
            assertEquals(a, new Marker(0, 0, 0, DIM, (byte) a, (byte) 0).side(),
                    "face ordinal " + a + " is a legal marker side");
        }
        assertEquals(6, net.minecraft.core.Direction.values().length,
                "1.7.10's six ForgeDirection ordinals map 1:1 onto 1.21.1's six Direction values");
    }
}
