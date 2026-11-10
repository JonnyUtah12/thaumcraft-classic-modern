package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenBossRoom;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The per-world maze handler (TC-M6-04f-m; the original
 * {@code thaumcraft.common.lib.world.dim.MazeHandler} + the placement half of
 * {@code MazeThread} + the chunk-generation call site in
 * {@code ThaumcraftWorldGenerator.worldGeneration}).
 *
 * <p>The original kept {@code MazeHandler.labyrinth} — a
 * {@code ConcurrentHashMap<CellLoc, Short>} of packed {@link Cell}s — in a
 * static map, persisted to {@code labyrinth.dat} in the Outer Lands save
 * folder (its "world" in 1.7.10 = one dimension) and consulted by the Outer
 * Lands chunk generator for every chunk: a cell present in the map means
 * "this chunk is a maze room", and the feature id picks the builder —
 * 1 = the portal room, 2..5 = the four boss-room quadrants, 6 = library,
 * 7..9 = altar/obelisk/cap, 10..14 = wall features, 0 = passage. The altar
 * asked for a maze through a {@code MazeThread}: five zero-markers (centre +
 * the four corners), a randomized-DFS layout from {@link MazeGenerator}
 * retried on seed++, every positive grid entry written at
 * {@code (cellX + x - (1 + w/2), cellZ + z - (1 + h/2))}, then the zero
 * markers cleaned off.</p>
 *
 * <h2>1.21.1 shape</h2>
 * The map is a {@link SavedData} in the Outer Lands dimension's data storage
 * (the faithful analogue of "labyrinth.dat in the dimension's own save
 * folder", same as the shipped boss counter). Maze generation runs on the
 * server thread (the original spawned a raw {@code Thread} and raced the
 * chunk generator); the entry point is the shipped {@link EldritchMazeHost}
 * seam, so the altar's request starts the generation and gets the answer.
 * The chunk hook fires on every load, so a "built" set — the mark the
 * original's populate-once pipeline gave it for free — keeps a room from
 * being rebuilt (and its decoration rolls from re-running) on reload.
 *
 * <p>Builders shipping on this card: portal room ({@link GenPortal}) and the
 * four boss-room quadrants ({@link GenBossRoom} + {@code Gen2x2}) — the
 * card's named scope. The passage, key-, nest- and library-room builders and
 * the wall features are the follow-up slice (recorded in
 * {@code left_out_of_scope}); a cell whose builder has not landed stays
 * unbuilt, and its chunk stays the original's void. The boss entities
 * themselves (golem/warden/crimson/taint) are TC-M6-06.</p>
 */
public final class MazeHandler {

    /** The original's {@code labyrinth.dat} — 1.21.1: the Outer Lands SavedData. */
    public static final String DATA_NAME = "thaumcraft:labyrinth";

    /**
     * The live server (1.21.1 removed the 1.7.10-era
     * {@code MinecraftServer.getServer()} static; the shipped
     * {@code OuterLandsChunkHooks} captures it at server start for the
     * seam probes, which need the Outer Lands level from a tile tick that
     * carries no server handle).
     */
    private static volatile MinecraftServer server;

    public static void installServer(final MinecraftServer live) {
        server = live;
    }

    /** Per-level {@link Random}: the original's world.rand analogue (the Gen toolbox keeps java.util.Random). */
    private static final java.util.concurrent.ConcurrentHashMap<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Random> WORLD_RANDOM =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static Random worldRandom(final ServerLevel level) {
        return WORLD_RANDOM.computeIfAbsent(level.dimension(), k -> new Random(level.getSeed()));
    }

    // ------------------------------------------------------------------
    //  the map (original putToHashMap / getFromHashMap / ... family)
    // ------------------------------------------------------------------

    public static void putToHashMap(final ServerLevel outer, final CellLoc key, final Cell cell) {
        putToHashMapRaw(outer, key, cell.pack());
    }

    public static void putToHashMapRaw(final ServerLevel outer, final CellLoc key, final short cell) {
        data(outer).cells.put(pack(key.x, key.z), cell);
        data(outer).setDirty();
    }

    public static Cell getFromHashMap(final ServerLevel outer, final CellLoc key) {
        final Short value = data(outer).cells.get(pack(key.x, key.z));
        return value == null ? null : new Cell(value);
    }

    public static short getFromHashMapRaw(final ServerLevel outer, final CellLoc key) {
        final Short value = data(outer).cells.get(pack(key.x, key.z));
        return value == null ? 0 : value;
    }

    public static void removeFromHashMap(final ServerLevel outer, final CellLoc key) {
        data(outer).cells.remove(pack(key.x, key.z));
        data(outer).setDirty();
    }

    public static void clearHashMap(final ServerLevel outer) {
        data(outer).cells.clear();
        data(outer).built.clear();
        data(outer).setDirty();
    }

    public static int cellCount(final ServerLevel outer) {
        return data(outer).cells.size();
    }

    /**
     * The original's {@code mazesInRange}: any cell inside the
     * {@code [-w..w] × [-h..h]} chunk neighbourhood of the map answers yes.
     */
    public static boolean mazesInRange(final ServerLevel outer, final int chunkX, final int chunkZ,
                                       final int w, final int h) {
        final Map<Long, Short> cells = data(outer).cells;
        for (int x = -w; x <= w; x++) {
            for (int z = -h; z <= h; z++) {
                if (cells.containsKey(pack(chunkX + x, chunkZ + z))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  maze entry: the original's MazeThread.run, on the server thread
    // ------------------------------------------------------------------

    /**
     * Generate the maze layout the original's {@code MazeThread} built: the
     * five zero-markers, a {@link MazeGenerator} retried with seed++ until it
     * succeeds, every positive grid entry written at
     * {@code (a + x - (1 + w/2), b + z - (1 + h/2))}, the zero-markers
     * removed again. Returns false when no layout succeeded (the original's
     * thread just kept the markers then).
     */
    public static boolean generateMaze(final ServerLevel outer, final int x, final int z,
                                       final int w, final int h, long seed) {
        putToHashMapRaw(outer, new CellLoc(x, z), (short) 0);
        putToHashMapRaw(outer, new CellLoc(x - w, z - h), (short) 0);
        putToHashMapRaw(outer, new CellLoc(x + w, z + h), (short) 0);
        putToHashMapRaw(outer, new CellLoc(x - w, z + h), (short) 0);
        putToHashMapRaw(outer, new CellLoc(x + w, z - h), (short) 0);

        MazeGenerator gen = new MazeGenerator(w, h, seed++);
        while (!gen.generate()) {
            gen = new MazeGenerator(w, h, seed++);
            if (seed == 0) {
                break; // the original retried unbounded; a full seed walk terminates.
            }
        }

        final int col = x - (1 + w / 2);
        final int row = z - (1 + h / 2);
        for (int a = 0; a < w; a++) {
            for (int b = 0; b < h; b++) {
                if (gen.grid[b][a] > 0) {
                    putToHashMapRaw(outer, new CellLoc(a + col, b + row), (short) gen.grid[b][a]);
                }
            }
        }

        for (final CellLoc marker : new CellLoc[]{new CellLoc(x, z), new CellLoc(x - w, z - h),
                new CellLoc(x + w, z + h), new CellLoc(x - w, z + h), new CellLoc(x + w, z - h)}) {
            if (getFromHashMapRaw(outer, marker) == 0) {
                removeFromHashMap(outer, marker);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    //  chunk placement: the original's worldGeneration → generateEldritch
    // ------------------------------------------------------------------

    /**
     * The original's {@code MazeHandler.generateEldritch}: when a cell covers
     * the chunk, run its feature builder at y = 50 and flush the queued
     * decorations. This slice ships feature 1 (portal room) and features
     * 2..5 (the boss-room quadrants); other features return false and leave
     * the void (their builders are the follow-up slice, and the shipped room
     * is never rebuilt — see the "built" gate in the class javadoc).
     */
    public static boolean generateEldritch(final ServerLevel outer, final Random random,
                                           final int chunkX, final int chunkZ) {
        final long key = pack(chunkX, chunkZ);
        final LabyrinthData store = data(outer);
        final Short raw = store.cells.get(key);
        if (raw == null) {
            return false;
        }
        if (store.built.contains(key)) {
            return true;
        }
        final Cell cell = new Cell(raw);
        boolean built;
        switch (cell.feature) {
            case 1:
                GenPortal.generatePortal(outer, random, chunkX, chunkZ, OuterLands.ROOM_Y, cell);
                built = true;
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                // original arg list verbatim: (world, random, i, j, k, y, quadrant, cell);
                // quadrant = feature - 2 (2 = upper left ... 5 = lower right, MazeGenerator L109)
                GenBossRoom.generateRoom(outer, random, chunkX, chunkZ, 0, OuterLands.ROOM_Y,
                        cell.feature - 2, cell);
                built = true;
                break;
            default:
                built = false;
        }
        if (built) {
            GenCommon.processDecorations(outer);
            store.built.add(key);
            store.setDirty();
        }
        return built;
    }

    /** Test seam: mark every mapped cell built (idempotence checks). */
    public static void markAllBuilt(final ServerLevel outer) {
        final LabyrinthData store = data(outer);
        store.built.addAll(store.cells.keySet());
        store.setDirty();
    }

    // ------------------------------------------------------------------
    //  seam installs (mod start)
    // ------------------------------------------------------------------

    /**
     * Install the handler behind the shipped seams: the altar's maze request
     * ({@link EldritchMazeHost}) and the lock's boss-room build
     * ({@link EldritchBossRoomHost}).
     *
     * <p>The maze request runs the generation on the spot when the map has
     * nothing in range — the original started a {@code MazeThread} and
     * answered "no maze" for that first request; running synchronously
     * answers the *same* request yes, so the altar's next wave sees the
     * dungeon one eye-insertion earlier (documented in the FEATURE_MATRIX
     * row). The seed is the overworld's world random, as the original's
     * {@code world.rand.nextLong()} was.</p>
     */
    public static void install() {
        EldritchMazeHost.setProbe((chunkX, chunkZ, width, height) -> {
            final ServerLevel outer = OuterLands.outerLands(server);
            if (outer == null) {
                return false;
            }
            if (mazesInRange(outer, chunkX, chunkZ, width, height)) {
                return true;
            }
            final ServerLevel overworld = server != null ? server.getLevel(net.minecraft.world.level.Level.OVERWORLD) : null;
            generateMaze(outer, chunkX, chunkZ, width, height,
                    overworld != null ? worldRandom(overworld).nextLong() : outer.getSeed());
            return mazesInRange(outer, chunkX, chunkZ, width, height);
        });

        EldritchBossRoomHost.setBuilder((level, lockPos, exitFeature) -> {
            if (!OuterLands.isOuterLands(level)) {
                return;
            }
            // The original scans the 5x5 chunk neighbourhood of the maze map
            // for the boss room's cell (its feature 2..5 quadrant) and builds
            // the room at that cell, opening the door at the cell's exit;
            // the lock's own chunk answers on a world without a maze, which
            // is why the shipped tile hands the seam a 0 exit.
            final int cx = lockPos.getX() >> 4;
            final int cz = lockPos.getZ() >> 4;
            CellLoc room = null;
            outer:
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    final Cell cell = getFromHashMap(level, new CellLoc(cx + x, cz + z));
                    if (cell != null && cell.feature >= 2 && cell.feature <= 5) {
                        room = new CellLoc(cx + x, cz + z);
                        break outer;
                    }
                }
            }
            if (room == null) {
                return;
            }
            GenBossRoom.generateRoom(level, worldRandom(level), room.x, room.z, 0, OuterLands.ROOM_Y,
                    getFromHashMap(level, room).feature - 2, getFromHashMap(level, room));
            // The boss spawn (golem / warden / Crimson Portal / 4 taintacles)
            // is the lock's summon body: TC-M6-06 (the EldritchMobHost seam).
        });

        ThaumcraftClassic.LOGGER.info("[TC-M6-04f-m] Outer Lands maze handler + boss-room builder installed "
                + "(portal room f1, boss rooms f2-5; passages/key/nest/library on the follow-up slice)");
    }

    // ------------------------------------------------------------------
    //  persistence
    // ------------------------------------------------------------------

    public static LabyrinthData data(final ServerLevel outer) {
        return outer.getDataStorage().computeIfAbsent(LabyrinthData.FACTORY, DATA_NAME);
    }

    /** The {@code labyrinth.dat} map: packed CellLoc → packed Cell, + built cells. */
    public static final class LabyrinthData extends SavedData {

        public static final Factory<LabyrinthData> FACTORY = new Factory<>(LabyrinthData::new,
                (tag, registries) -> {
                    final LabyrinthData loaded = new LabyrinthData();
                    final ListTag cells = tag.getList("cells", Tag.TAG_COMPOUND);
                    for (int a = 0; a < cells.size(); a++) {
                        final CompoundTag cell = cells.getCompound(a);
                        loaded.cells.put(pack(cell.getInt("x"), cell.getInt("z")), cell.getShort("cell"));
                    }
                    final ListTag built = tag.getList("built", Tag.TAG_COMPOUND);
                    for (int a = 0; a < built.size(); a++) {
                        final CompoundTag cell = built.getCompound(a);
                        loaded.built.add(pack(cell.getInt("x"), cell.getInt("z")));
                    }
                    return loaded;
                });

        final Map<Long, Short> cells = new HashMap<>();
        final Set<Long> built = new HashSet<>();

        /** Probes/tests: the number of persisted cells. */
        public int cellCount() {
            return cells.size();
        }

        /** Probes/tests: the packed cell at a chunk, 0 when the chunk is no room. */
        public short cellAt(final int x, final int z) {
            final Short value = cells.get(pack(x, z));
            return value == null ? 0 : value;
        }

        /** Probes/tests: whether a chunk's room has been built. */
        public boolean isBuilt(final int x, final int z) {
            return built.contains(pack(x, z));
        }

        @Override
        public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final ListTag cellList = new ListTag();
            for (final Map.Entry<Long, Short> entry : cells.entrySet()) {
                // writeNBT kept only cells with a positive value
                if (entry.getValue() > 0) {
                    final CompoundTag cell = new CompoundTag();
                    cell.putInt("x", unpackX(entry.getKey()));
                    cell.putInt("z", unpackZ(entry.getKey()));
                    cell.putShort("cell", entry.getValue());
                    cellList.add(cell);
                }
            }
            tag.put("cells", cellList);
            final ListTag builtList = new ListTag();
            for (final long key : built) {
                final CompoundTag cell = new CompoundTag();
                cell.putInt("x", unpackX(key));
                cell.putInt("z", unpackZ(key));
                builtList.add(cell);
            }
            tag.put("built", builtList);
            return tag;
        }
    }

    private static long pack(final int x, final int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(final long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(final long key) {
        return (int) key;
    }

    private MazeHandler() {
    }
}
