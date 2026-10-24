package com.jonnyutah12.thaumcraftclassic.worldgen;

/**
 * Seams where TC-M5-05b structure generation touches machinery owned by other
 * catalog cards. Defaults are inert (no maze overlap guard, no maze thread);
 * TC-M6-04f (Outer Lands dimension + MazeHandler/MazeThread) installs the
 * real implementations at mod construction.
 *
 * <p>Original counterparts: {@code MazeHandler.mazesInRange(chunkX, chunkZ,
 * width, height)} (WorldGenEldritchRing.java:67 — the ring refuses to
 * generate where it would overlap an existing Outer Lands maze) and
 * {@code new Thread(new MazeThread(chunkX, chunkZ, w, h, random.nextLong()))
 * .start()} (ThaumcraftWorldGenerator.java:474 — kicks off the maze the ring
 * marks the entrance to). The original's maze map is only populated for
 * loaded Outer Lands dimensions, so with no Outer Lands dimension yet the
 * default {@code false} reproduces observable behavior exactly.</p>
 */
public final class StructureHooks {

    /** Maze-overlap guard for the eldritch ring (original MazeHandler.mazesInRange). */
    public interface MazeGuard {
        boolean mazesInRange(int chunkX, int chunkZ, int width, int height);
    }

    /** Maze kick-off after a successful ring (original MazeThread). */
    public interface MazeKickoff {
        void start(int chunkX, int chunkZ, int width, int height, long seed);
    }

    private static volatile MazeGuard mazeGuard = (chunkX, chunkZ, width, height) -> false;
    private static volatile MazeKickoff mazeKickoff = (chunkX, chunkZ, width, height, seed) -> {
    };

    private StructureHooks() {
    }

    public static void setMazeGuard(final MazeGuard guard) {
        mazeGuard = guard == null ? (chunkX, chunkZ, width, height) -> false : guard;
    }

    public static void setMazeKickoff(final MazeKickoff kickoff) {
        mazeKickoff = kickoff == null ? (chunkX, chunkZ, width, height, seed) -> {
        } : kickoff;
    }

    static boolean mazesInRange(final int chunkX, final int chunkZ, final int width, final int height) {
        return mazeGuard.mazesInRange(chunkX, chunkZ, width, height);
    }

    static void startMaze(final int chunkX, final int chunkZ, final int width, final int height, final long seed) {
        mazeKickoff.start(chunkX, chunkZ, width, height, seed);
    }
}
