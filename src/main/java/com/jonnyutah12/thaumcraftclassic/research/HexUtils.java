package com.jonnyutah12.thaumcraftclassic.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Axial/cubic hex-grid math for the research-note puzzle (TC-M3-04).
 *
 * <p>Port of {@code thaumcraft.common.lib.utils.HexUtils} (TC 4.2.3.5). The
 * original used mutable {@code Hex}/{@code CubicHex}/{@code Pixel} value
 * classes; the port keeps them as immutable records with the same
 * coordinate semantics, the same {@code NEIGHBOURS} direction order (parsed
 * back out of the decompiled file by
 * {@code NotePuzzleOriginalParityTest.neighbourTableMatchesTheDecompiledOriginal})
 * and the same {@code "q:r"} string key the original used as the map key
 * inside {@code ResearchNoteData} and the note {@code hexgrid} NBT. Pure
 * Java — no Minecraft types — so the puzzle logic is unit-testable
 * (docs/testing.md).</p>
 */
public final class HexUtils {

    /** Original direction table, verbatim: {@code HexUtils.NEIGHBOURS}. */
    static final int[][] NEIGHBOURS = new int[][]{{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

    public record Hex(int q, int r) {

        public CubicHex toCubicHex() {
            return new CubicHex(q, r, -q - r);
        }

        public Pixel toPixel(int size) {
            return new Pixel(size * 1.5 * q, size * Math.sqrt(3.0) * (r + q / 2.0));
        }

        public Hex getNeighbour(int direction) {
            int[] d = NEIGHBOURS[direction];
            return new Hex(q + d[0], r + d[1]);
        }

        /** The original map-key form; preserved verbatim for NBT parity. */
        @Override
        public String toString() {
            return q + ":" + r;
        }

        /** Parse an original "q:r" key. */
        public static Hex fromKey(String key) {
            String[] parts = key.split(":");
            return new Hex(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    public record CubicHex(int x, int y, int z) {
        public Hex toHex() {
            return new Hex(x, z);
        }
    }

    public record Pixel(double x, double y) {
        public Hex toHex(int size) {
            double qq = 0.6666666666666666 * x / size;
            double rr = (0.3333333333333333 * Math.sqrt(3.0) * -y - 0.3333333333333333 * x) / size;
            return getRoundedHex(qq, rr);
        }
    }

    public static int getDistance(Hex a1, Hex a2) {
        return (Math.abs(a1.q() - a2.q()) + Math.abs(a1.r() - a2.r())
                + Math.abs(a1.q() + a1.r() - a2.q() - a2.r())) / 2;
    }

    public static Hex getRoundedHex(double qq, double rr) {
        return getRoundedCubicHex(qq, rr, -qq - rr).toHex();
    }

    public static CubicHex getRoundedCubicHex(double xx, double yy, double zz) {
        int rx = (int) Math.round(xx);
        int ry = (int) Math.round(yy);
        int rz = (int) Math.round(zz);
        double xDiff = Math.abs(rx - xx);
        double yDiff = Math.abs(ry - yy);
        double zDiff = Math.abs(rz - zz);
        if (xDiff > yDiff && xDiff > zDiff) {
            rx = -ry - rz;
        } else if (yDiff > zDiff) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }
        return new CubicHex(rx, ry, rz);
    }

    /** The {@code 6 * radius} ring cells at that distance, in the original walk order. */
    public static ArrayList<Hex> getRing(int radius) {
        Hex h = new Hex(0, 0);
        for (int k = 0; k < radius; k++) {
            h = h.getNeighbour(4);
        }
        ArrayList<Hex> ring = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < radius; j++) {
                ring.add(h);
                h = h.getNeighbour(i);
            }
        }
        return ring;
    }

    /**
     * Evenly spaced {@code entries} positions on the outer ring, random start.
     *
     * <p>The original drew a random start into {@code start} and then never
     * read it (the positions are always spaced from ring index 0); the draw is
     * kept because it consumes one value of the world {@code Random}, and every
     * later draw (the interior blank picks) must stay on the same stream as
     * TC 4.2.3.5. Pinned by
     * {@code HexUtilsTest.distributeRingRandomlyConsumesTheOriginalDiscardedStartDraw}.</p>
     */
    public static ArrayList<Hex> distributeRingRandomly(int radius, int entries, Random random) {
        ArrayList<Hex> ring = getRing(radius);
        ArrayList<Hex> results = new ArrayList<>();
        float spacing = (float) ring.size() / entries;
        random.nextInt(ring.size());
        float pos = 0.0F;
        for (int i = 0; i < entries; i++) {
            results.add(ring.get(Math.round(pos)));
            pos += spacing;
        }
        return results;
    }

    /**
     * All hexes within {@code radius} (inclusive) — {@code 1 + 3r(r+1)} cells —
     * keyed by "q:r", in the original's insertion order.
     */
    public static HashMap<String, Hex> generateHexes(int radius) {
        HashMap<String, Hex> results = new HashMap<>();
        Hex h = new Hex(0, 0);
        results.put(h.toString(), h);
        for (int k = 0; k < radius; k++) {
            h = h.getNeighbour(4);
            Hex hd = new Hex(h.q(), h.r());
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j <= k; j++) {
                    results.put(hd.toString(), hd);
                    hd = hd.getNeighbour(i);
                }
            }
        }
        return results;
    }

    private HexUtils() {
    }
}
