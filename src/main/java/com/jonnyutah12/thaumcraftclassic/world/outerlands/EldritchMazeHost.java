package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The seam the eldritch structures use to ask for a maze (TC-M6-04f-s).
 *
 * <p>The original's altar and lock tiles asked {@code MazeHandler} for an
 * eldritch dungeon near the structure: the altar's {@code checkForMaze()}
 * asked whether a maze of {@code 15 + 2·rand(8)} cells already covered the
 * altar's chunk and, if not, started a {@code MazeThread} to build one; the
 * lock's boss spawn read the maze map of the 5×5 chunk neighbourhood to pick
 * the boss room's chunk and exit.</p>
 *
 * <p>The maze itself shipped as the pure generator in f252c58
 * ({@link MazeGenerator}, {@code Cell}, {@code CellLoc}); the per-world handler
 * that places generated cells into a dimension, keeps the map of built mazes
 * and answers "is one in range" is TC-M6-04f-m. Until that handler registers
 * a probe here, the answer is "no maze in range" — which is what the original
 * returns for a fresh world too — and each request is recorded so the
 * follow-up card (and the game test) can see that the structure asked.</p>
 */
public final class EldritchMazeHost {

    /** Answers "does a maze of at least this size cover this chunk". */
    @FunctionalInterface
    public interface Probe {
        boolean mazesInRange(int chunkX, int chunkZ, int width, int height);
    }

    private static Probe probe = (chunkX, chunkZ, width, height) -> false;

    private static int lastWidth;
    private static int lastHeight;
    private static int lastChunkX;
    private static int lastChunkZ;
    private static int requests;

    /** Installs the handler's answer (called by TC-M6-04f-m at mod start). */
    public static void setProbe(final Probe installed) {
        probe = installed;
    }

    /** Record a generation request and answer it through the probe. */
    public static boolean request(final int chunkX, final int chunkZ, final int width, final int height) {
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
        lastWidth = width;
        lastHeight = height;
        requests++;
        return probe.mazesInRange(chunkX, chunkZ, width, height);
    }

    /**
     * The original's maze size roll: {@code 15 + random.nextInt(8) * 2} — an
     * odd-cell-sided square between 15 and 29. Derived from the level seed's
     * hash so a structure's request is stable per world, as the original's
     * per-world random was.
     */
    public static int requestSize(final long seed, final int coordinate) {
        final int roll = Math.abs(Math.toIntExact(seed * 31L + coordinate * 7919L)) % 8;
        return 15 + roll * 2;
    }

    public static int lastWidth() {
        return lastWidth;
    }

    public static int lastHeight() {
        return lastHeight;
    }

    public static int lastChunkX() {
        return lastChunkX;
    }

    public static int lastChunkZ() {
        return lastChunkZ;
    }

    public static int requests() {
        return requests;
    }

    /** The lock's boss spawn reads the maze map around the lock (TC-M6-04f-m). */
    public static boolean mazeMapLoaded(final ServerLevel level, final BlockPos pos) {
        return probe.mazesInRange(pos.getX() >> 4, pos.getZ() >> 4, 15, 15);
    }

    private EldritchMazeHost() {
    }
}
