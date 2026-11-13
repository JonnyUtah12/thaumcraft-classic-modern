package com.jonnyutah12.thaumcraftclassic.research;

import com.jonnyutah12.thaumcraftclassic.research.HexUtils.CubicHex;
import com.jonnyutah12.thaumcraftclassic.research.HexUtils.Hex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the TC-M3-04 transcription of the original
 * {@code thaumcraft.common.lib.utils.HexUtils} (TC 4.2.3.5): the direction
 * table read back out of the decompiled file, the ring walk order, the closed
 * form of {@code generateHexes}, the {@code "q:r"} map key, and the random draw
 * {@code distributeRingRandomly} performs but never uses.
 */
class HexUtilsTest {

    /** Original ring of radius 1, in the walk order {@code getRing} produces. */
    private static final List<Hex> RING_1 = List.of(
            new Hex(-1, 1), new Hex(0, 1), new Hex(1, 0), new Hex(1, -1), new Hex(0, -1), new Hex(-1, 0));

    @Test
    void neighbourTableMatchesTheDecompiledDirectionOrder() {
        final String original = ReferenceOrigins.read(
                "decompiled/thaumcraft/thaumcraft/common/lib/utils/HexUtils.java");
        final Matcher table = Pattern.compile("NEIGHBOURS = new int\\[\\]\\[\\]\\{(.*?)\\};").matcher(original);
        assertTrue(table.find(), "the decompiled NEIGHBOURS table moved or changed shape");
        final List<int[]> parsed = new ArrayList<>();
        final Matcher pair = Pattern.compile("\\{(-?\\d+),\\s*(-?\\d+)\\}").matcher(table.group(1));
        while (pair.find()) {
            parsed.add(new int[]{Integer.parseInt(pair.group(1)), Integer.parseInt(pair.group(2))});
        }
        assertEquals(6, parsed.size(), "the original table holds six directions");
        assertEquals(parsed.size(), HexUtils.NEIGHBOURS.length);
        for (int i = 0; i < parsed.size(); i++) {
            assertArrayEquals(parsed.get(i), HexUtils.NEIGHBOURS[i], "direction " + i);
        }
    }

    @Test
    void ringOfOneIsTheOriginalWalkOrder() {
        assertEquals(RING_1, HexUtils.getRing(1));
    }

    @Test
    void ringHoldsSixTimesRadiusCellsAllAtThatDistance() {
        for (int radius = 1; radius <= 4; radius++) {
            final List<Hex> ring = HexUtils.getRing(radius);
            assertEquals(6 * radius, ring.size(), "ring " + radius);
            assertEquals(6 * radius, ring.stream().distinct().count(), "ring " + radius + " repeats a cell");
            for (Hex hex : ring) {
                assertEquals(radius, HexUtils.getDistance(new Hex(0, 0), hex), "distance of " + hex);
            }
        }
    }

    @Test
    void generateHexesCoversTheDiscWithTheClosedFormCount() {
        for (int radius = 0; radius <= 4; radius++) {
            final var cells = HexUtils.generateHexes(radius);
            assertEquals(1 + 3 * radius * (radius + 1), cells.size(), "radius " + radius);
            for (Hex hex : cells.values()) {
                assertTrue(HexUtils.getDistance(new Hex(0, 0), hex) <= radius, hex + " outside radius " + radius);
            }
            for (int distance = 0; distance <= radius; distance++) {
                final int at = distance;
                assertTrue(cells.values().stream().anyMatch(h -> HexUtils.getDistance(new Hex(0, 0), h) == at),
                        "radius " + radius + " misses a cell at distance " + distance);
            }
        }
    }

    @Test
    void distanceIsSelfZeroAndSymmetric() {
        final Hex a = new Hex(2, -1);
        final Hex b = new Hex(-1, 3);
        assertEquals(0, HexUtils.getDistance(a, a));
        assertEquals(HexUtils.getDistance(a, b), HexUtils.getDistance(b, a));
        assertEquals(2, HexUtils.getDistance(new Hex(0, 0), new Hex(1, 1)));
        assertEquals(3, HexUtils.getDistance(new Hex(0, 0), new Hex(2, 1)));
        assertNotEquals(0, HexUtils.getDistance(a, b));
    }

    @Test
    void keyRoundTripsThroughTheOriginalQRForm() {
        for (Hex hex : HexUtils.generateHexes(3).values()) {
            assertEquals(hex.q() + ":" + hex.r(), hex.toString());
            assertEquals(hex, Hex.fromKey(hex.toString()));
        }
        assertEquals(new Hex(-2, 3), Hex.fromKey("-2:3"));
    }

    @Test
    void cubicAndPixelConversionsRoundTrip() {
        assertEquals(new CubicHex(2, -1, -1), new Hex(2, -1).toCubicHex());
        for (Hex hex : HexUtils.generateHexes(3).values()) {
            // The original's CubicHex.toHex() returns (x, z) while Hex.toCubicHex()
            // stores (q, r, -q-r), so the pair is NOT an identity — pin the real mapping.
            assertEquals(new Hex(hex.q(), -hex.q() - hex.r()), hex.toCubicHex().toHex(),
                    "CubicHex.toHex() is the original's (x, z) mapping");
            assertEquals(hex, hex.toPixel(20).toHex(20), "the pixel pair is an identity");
        }
    }

    @Test
    void distributeRingRandomlySpacesEntriesEvenlyFromRingIndexZero() {
        final List<Hex> ring = HexUtils.getRing(3);
        final ArrayList<Hex> placed = HexUtils.distributeRingRandomly(3, 4, new Random(7L));
        assertEquals(4, placed.size());
        final float spacing = (float) ring.size() / 4;
        for (int i = 0; i < 4; i++) {
            assertEquals(ring.get(Math.round(i * spacing)), placed.get(i),
                    "the original spaces positions from ring index 0, never from the drawn start");
        }
    }

    @Test
    void distributeRingRandomlyConsumesTheOriginalDiscardedStartDraw() {
        // The original drew `int start = random.nextInt(ring.size())` and never read
        // it. The draw must still happen, or every later consumer of the world
        // Random (the interior blank picks) would run on a different stream.
        final Random consumed = new Random(1234L);
        HexUtils.distributeRingRandomly(3, 5, consumed);
        final int afterPlacement = consumed.nextInt();

        final Random expected = new Random(1234L);
        expected.nextInt(18);
        assertEquals(expected.nextInt(), afterPlacement, "distributeRingRandomly must consume exactly one draw");
    }
}
