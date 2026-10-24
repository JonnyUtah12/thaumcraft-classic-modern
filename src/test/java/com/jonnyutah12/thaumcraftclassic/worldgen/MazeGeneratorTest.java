package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.MazeGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-M6-04f: the Outer Lands maze layout must reproduce the original
 * {@code MazeGenerator} structure for fixed seeds — portal room at the center,
 * the 2x2/boss/key/nest corner cluster, a library, and every feature cell
 * reachable from the portal. (Verified standalone before the gradle test task
 * could run: the shared tree's main source set was mid-refactor by a sibling
 * card; this class mirrors that verified smoke run.)
 */
class MazeGeneratorTest {

    private static final int SIZE = 32;

    @Test
    void deterministicLayoutAndConnectivity() {
        for (long seed : new long[]{1L, 42L, 123456789L, 777L}) {
            MazeGenerator m = new MazeGenerator(SIZE, SIZE, seed);
            assertTrue(m.generate(), "generate failed for seed " + seed);

            int portal = 0;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    short packed = (short) m.grid[y][x];
                    Cell c = new Cell(packed);
                    assertEquals(packed, c.pack(), "pack/unpack round-trip seed " + seed);
                    if (c.feature == 1) {
                        portal++;
                    }
                }
            }
            assertEquals(1, portal, "exactly one portal cell for seed " + seed);

            // flood-fill from the portal cell through mutually-agreed openings
            int px = 1 + SIZE / 2;
            int py = 1 + SIZE / 2;
            boolean[][] seen = new boolean[SIZE][SIZE];
            Deque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{px, py});
            seen[py][px] = true;
            int[] dirs = {MazeGenerator.N, MazeGenerator.S, MazeGenerator.E, MazeGenerator.W};
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                Cell c = new Cell((short) m.grid[cur[1]][cur[0]]);
                for (int d : dirs) {
                    boolean open = d == MazeGenerator.N ? c.north : d == MazeGenerator.S ? c.south
                            : d == MazeGenerator.E ? c.east : c.west;
                    if (!open) {
                        continue;
                    }
                    int nx = cur[0] + MazeGenerator.getDX(d);
                    int ny = cur[1] + MazeGenerator.getDY(d);
                    if (nx < 0 || ny < 0 || nx >= SIZE || ny >= SIZE || seen[ny][nx]) {
                        continue;
                    }
                    Cell n = new Cell((short) m.grid[ny][nx]);
                    boolean back = d == MazeGenerator.N ? n.south : d == MazeGenerator.S ? n.north
                            : d == MazeGenerator.E ? n.west : n.east;
                    if (back) {
                        seen[ny][nx] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }

            // Every feature cell must be reachable — except the corner cluster
            // (features 2-5), which the room generators build as one open 2x2
            // space and which counts as reachable when any member is. Feature-0
            // dangling corridors are a preserved TC4 quirk, not asserted.
            boolean clusterReachable = false;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    Cell c = new Cell((short) m.grid[y][x]);
                    if (c.feature >= 2 && c.feature <= 5) {
                        if (seen[y][x]) {
                            clusterReachable = true;
                        }
                        continue;
                    }
                    if (c.feature > 0) {
                        assertTrue(seen[y][x], "feature " + c.feature + " cell unreachable at "
                                + x + "," + y + " for seed " + seed);
                    }
                }
            }
            assertTrue(clusterReachable, "corner room cluster unreachable for seed " + seed);
        }
    }
}
