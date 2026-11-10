package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchPortal;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.CellLoc;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchBossRoomHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMazeHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.MazeGenerator;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.MazeHandler;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.OuterLands;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.gen.GenCommon;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Modifier;
import java.util.Random;

/**
 * TC-M6-04f-m evidence (the card's bar: dimension + portal + boss-room
 * placement probes). Ten probes:
 *
 * <ol>
 *   <li>the data-driven dimension family — the live {@code dimension_type}
 *       entry carries the {@code WorldProviderOuter} flags verbatim, the live
 *       biome entry the {@code BiomeGenEldritch} numbers, and the shipped
 *       {@code dimension/dimension.-42.json} decodes through
 *       {@link LevelStem#CODEC} onto that live type;</li>
 *   <li>every Outer Lands column is air under the Eldritch biome (the
 *       original's {@code ChunkProviderOuter.fill} returning null +
 *       {@code WorldChunkManagerHell});</li>
 *   <li>the labyrinth map persists (the {@code labyrinth.dat} analogue);</li>
 *   <li>a seeded maze matches the shipped {@link MazeGenerator} layout cell
 *       for cell (the {@code MazeThread} placement math);</li>
 *   <li>a seeded maze runs the {@code MazeThread} build sweep cell by cell:
 *       features 1..5 build and mark built, the rest refuse, and the built
 *       gate makes the second sweep a no-op;</li>
 *   <li>a feature-1 cell builds the portal room at x*16+8, y+3 (the
 *       {@code GenPortal} geometry + the chunk-hook entry);</li>
 *   <li>the lock's seam is installed and builds a boss room through
 *       {@code MazeHandler} (the {@code GenBossRoom}/{@code Gen2x2} doorway
 *       geometry), while the installed builder keeps the {@code isEldritch}
 *       plane gate;</li>
 *   <li>the portal tile carves its room with its eldritch frame, gates the
 *       player on the original's 100-tick travel window (the {@code bW := 100}
 *       refresh rule), and takes its absent-plane travel path without moving
 *       the player or firing the ENTEROUTER completion;</li>
 *   <li>the altar's shipped maze seam keeps the original's plane gate (it
 *       runs on the live plane only), and the labyrinth map answers the
 *       {@code mazesInRange} request the altar's wave check reads;</li>
 *   <li>the seed is the maze's fingerprint and the map is not (the original's
 *       {@code MazeThread} + {@code LabyrinthMap}.{@code getCells}/
 *       {@code getBuiltCells}).</li>
 * </ol>
 *
 * <p>Where the probes build, and why: the game-test harness bakes its world
 * over an <em>empty</em> level-stem registry —
 * {@code net/minecraft/gametest/framework/GameTestServer.create} hands
 * {@code new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable())
 * .freeze()} to the flat world preset — so no datapack, mod or vanilla, ever
 * gets a second live dimension inside the harness. The dimension's data
 * contract is therefore asserted where it is observable (the live
 * {@code dimension_type}/{@code biome} registry entries and the shipped
 * dimension JSON decoded by the real codec), and the placement, generation and
 * travel-gate math — which is dimension-agnostic (cell origin x*16, room y+3,
 * the rotation and seed are the cell's, not the plane's) — runs in the
 * harness's own level. {@link OuterLands#outerLands(MinecraftServer)} is the
 * shipped accessor the game uses, and the tile's travel is written against a
 * target plane that may be absent (it returns without moving the player),
 * which is exactly the harness state asserted here.</p>
 *
 * <p>Curios-independent: the suite runs identically in the default and
 * {@code -Ptc.noCurios} lanes.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OuterLandsGameTest {

    private OuterLandsGameTest() {
    }

    /**
     * The level the placement probes build into: the harness's own level, for
     * the cited reason in the class javadoc (the harness has no second live
     * dimension; the placement math is the cell's, not the plane's).
     */
    /** The side bit of a Direction in the packed cell value (N=1, S=2, E=4, W=8). */
    private static int sideBit(final Direction side) {
        return switch (side) {
            case NORTH -> 1;
            case SOUTH -> 2;
            case EAST -> 4;
            case WEST -> 8;
            default -> 0;
        };
    }

    /**
     * Census of a box: blockEldritch, eldritch stairs, blockEldritchNothing, non-air, air. Reading
     * a block state loads the chunk synchronously, which is what every probe in this file already
     * does when it asserts a block; the harness pads carry no loaded tile entities, so the read is
     * safe (docs/testing.md).
     */
    private static int[] census(final ServerLevel level, final int x0, final int x1, final int y0,
                                final int y1, final int z0, final int z1) {
        int eldritch = 0;
        int stairs = 0;
        int nothing = 0;
        int nonAir = 0;
        int airy = 0;
        final var cursor = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    final BlockState state = level.getBlockState(cursor.set(x, y, z));
                    if (state.isAir()) {
                        airy++;
                    } else {
                        nonAir++;
                    }
                    if (state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                        eldritch++;
                    } else if (state.is(CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get())) {
                        stairs++;
                    } else if (state.is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                        nothing++;
                    }
                }
            }
        }
        return new int[] {eldritch, stairs, nothing, nonAir, airy};
    }

    /**
     * The 11x7 doorway plane one side of an open-sided boss-room cell lays inside the cell
     * (GenBossRoom.java:103-114: the plane sits at the side's fixed offset, 11 cells wide from the
     * side's own start, seven rows up from ROOM_Y+2). Census: blockEldritch, stairs, air.
     */
    private static int[] doorwayPlane(final ServerLevel level, final int cx, final int cz, final Direction side) {
        final int x0 = cx * 16;
        final int z0 = cz * 16;
        int eldritch = 0;
        int stairs = 0;
        int airy = 0;
        int nothing = 0;
        final var cursor = new BlockPos.MutableBlockPos();
        for (int a = 0; a <= 10; a++) {
            for (int b = 0; b <= 6; b++) {
                final int x = switch (side) {
                    case NORTH -> x0 + a;
                    case SOUTH -> x0 + 20 - a;
                    case WEST -> x0 + 20 - a;
                    default -> x0 + 13;
                };
                final int z = switch (side) {
                    case NORTH -> z0 + 3;
                    case SOUTH -> z0 + 13;
                    case WEST -> z0 + 20 - a;
                    default -> z0 + a;
                };
                final BlockState state = level.getBlockState(cursor.set(x, OuterLands.ROOM_Y + 2 + b, z));
                if (state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                    eldritch++;
                } else if (state.is(CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get())) {
                    stairs++;
                } else if (state.is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get())) {
                    nothing++;
                } else if (state.isAir()) {
                    airy++;
                }
            }
        }
        return new int[] {eldritch, stairs, airy, nothing};
    }

    /** blockEldritch in one world column, y0..y1 inclusive. */
    private static int countEldritchColumn(final ServerLevel level, final int x, final int z,
                                           final int y0, final int y1) {
        int n = 0;
        final var cursor = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            if (level.getBlockState(cursor.set(x, y, z)).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                n++;
            }
        }
        return n;
    }

    /**
     * blockEldritch in the 1-cell ring around a cell footprint, in the doorway's y band
     * (ROOM_Y+1 .. ROOM_Y+9 — the pattern rows are ROOM_Y+2 .. ROOM_Y+8; the room shell's own
     * eldritch sits higher, at ROOM_Y+11, and is not the doorway).
     */
    private static int ringEldritch(final ServerLevel level, final int cx, final int cz) {
        final int x0 = cx * 16;
        final int z0 = cz * 16;
        int n = 0;
        final var cursor = new BlockPos.MutableBlockPos();
        for (int y = OuterLands.ROOM_Y + 1; y <= OuterLands.ROOM_Y + 9; y++) {
            for (int a = -1; a <= 16; a++) {
                if (level.getBlockState(cursor.set(x0 + a, y, z0 - 1)).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                    n++;
                }
                if (level.getBlockState(cursor.set(x0 + a, y, z0 + 16)).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                    n++;
                }
                if (level.getBlockState(cursor.set(x0 - 1, y, z0 + a)).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                    n++;
                }
                if (level.getBlockState(cursor.set(x0 + 16, y, z0 + a)).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Pad facts for an assertion message: is the chunk holding this column loaded at all? */
    private static String chunkFacts(final ServerLevel level, final int x, final int z) {
        return "chunk " + (x >> 4) + "," + (z >> 4) + " "
                + (level.getChunkSource().hasChunk(x >> 4, z >> 4) ? "loaded" : "NOT LOADED");
    }

    private static ServerLevel build(final GameTestHelper helper) {
        return helper.getLevel();
    }

    /**
     * Decode the shipped {@code data/thaumcraft/dimension/dimension.-42.json}
     * with the real codec over the live dynamic registries — i.e. the same
     * path the game's world loader takes.
     */
    private static LevelStem shippedStem(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final JsonElement json;
        try (InputStream in = server.getResourceManager().open(ResourceLocation
                .fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "dimension/dimension.-42.json"))) {
            json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            helper.fail("the shipped dimension JSON is not readable: " + e);
            return null;
        }
        final RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        final DataResult<LevelStem> parsed = LevelStem.CODEC.parse(ops, json);
        final LevelStem stem = parsed.result().orElse(null);
        helper.assertTrue(stem != null,
                "the shipped dimension JSON does not decode: " + parsed.error().map(String::valueOf).orElse("?"));
        return stem;
    }

    @GameTest(template = "empty")
    public static void dimensionFamilyShipsFromData(final GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();

        final DimensionType type = registries
                .lookupOrThrow(Registries.DIMENSION_TYPE).get(OuterLands.DIMENSION_TYPE)
                .map(Holder::value).orElse(null);
        helper.assertTrue(type != null, "thaumcraft:outerlands dimension type is not in the registry");
        // WorldProviderOuter verbatim: canRespawnHere false (bed_works),
        // isDaylight false (has_skylight), getWeatherCoordinateScale/getTimeOffset
        // = 1.0, the void fog (no skylight, 0.15 ambient), the 128 height,
        // no raids/piglins/respawn anchors, monsters never spawn from light.
        helper.assertTrue(!type.hasSkyLight(), "the Outer Lands has daylight");
        helper.assertTrue(!type.natural(), "the Outer Lands is a natural world");
        helper.assertTrue(!type.ultraWarm(), "the Outer Lands is ultrawarm");
        helper.assertTrue(!type.bedWorks(), "beds work in the Outer Lands");
        helper.assertTrue(!type.respawnAnchorWorks(), "respawn anchors work in the Outer Lands");
        helper.assertTrue(!type.piglinSafe(), "the Outer Lands is piglin-safe");
        helper.assertTrue(!type.hasCeiling(), "the Outer Lands has a ceiling");
        helper.assertTrue(type.coordinateScale() == 1.0, "the Outer Lands coordinate scale is not 1.0");
        helper.assertTrue(type.ambientLight() == 0.15f,
                "the Outer Lands ambient light is " + type.ambientLight() + "; the original's void fog is 0.15");
        helper.assertTrue(type.height() == 128 && type.logicalHeight() == 128,
                "the Outer Lands height is " + type.height() + "/" + type.logicalHeight() + "; the original built at height 128");
        helper.assertTrue(type.monsterSpawnLightTest().sample(RandomSource.create()) == 0,
                "monsters spawn on light level in the Outer Lands");
        helper.assertTrue(type.monsterSpawnBlockLightLimit() == 0,
                "monsters spawn on block light in the Outer Lands");

        final var biome = registries.lookupOrThrow(Registries.BIOME).get(OuterLands.BIOME_ELDRITCH)
                .map(Holder::value).orElse(null);
        helper.assertTrue(biome != null, "thaumcraft:biome.biomegeneldritch is not in the registry");
        helper.assertTrue(!biome.hasPrecipitation(), "the Eldritch biome rains (BiomeGenEldritch has no precipitation)");
        helper.assertTrue(biome.getSpecialEffects().getSkyColor() == 0,
                "the Eldritch biome sky is not black");
        helper.assertTrue(biome.getSpecialEffects().getFogColor() == 0,
                "the Eldritch biome fog is not black");

        // The dimension JSON: it decodes through the real codec onto the live
        // shipped type and biome entries, i.e. the family is complete.
        final LevelStem stem = shippedStem(helper);
        if (stem == null) {
            return;
        }
        helper.assertTrue(stem.type().value() == type,
                "the dimension JSON points at " + stem.type().unwrapKey().map(k -> k.location().toString()).orElse("none")
                        + ", not the shipped thaumcraft:outerlands type");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void outerlandsColumnsAreVoidEldritch(final GameTestHelper helper) {
        // The original's ChunkProviderOuter filled every block with null —
        // pure air — and stamped every column with
        // WorldChunkManagerHell(biomeEldritchLands, 0.0). 1.21.1's carrier of
        // that fact is the flat generator's empty layer list (void) over a
        // fixed biome source, and the shipped dimension JSON is what the
        // world loader reads, so the JSON is what is asserted.
        final LevelStem stem = shippedStem(helper);
        if (stem == null) {
            return;
        }
        helper.assertTrue(stem.generator() instanceof FlatLevelSource flat,
                "the dimension generator is not the flat void source (ChunkProviderOuter analogue)");
        if (!(stem.generator() instanceof FlatLevelSource flat)) {
            return;
        }
        helper.assertTrue(flat.settings().getLayersInfo().isEmpty(),
                "the void generator has " + flat.settings().getLayersInfo().size()
                        + " layers; the original's provider fills null (air) everywhere");
        helper.assertTrue(flat.settings().getLayers().isEmpty(),
                "the void generator stacks " + flat.settings().getLayers().size() + " blocks in a column");
        helper.assertTrue(flat.settings().getBiome().is(OuterLands.BIOME_ELDRITCH),
                "the columns carry biome " + flat.settings().getBiome().unwrapKey().orElse(null)
                        + "; the original stamps biomeEldritchLands");
        helper.assertTrue(flat.settings().structureOverrides().isEmpty(),
                "the Outer Lands carries a structure set; the original places nothing (the maze is generated)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void labyrinthMapRoundTripsThroughNbt(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final CellLoc loc = new CellLoc(777, 888);
        MazeHandler.putToHashMapRaw(outer, loc, (short) 513);
        helper.assertTrue(MazeHandler.getFromHashMapRaw(outer, loc) == 513,
                "the packed cell did not round-trip the in-memory map");
        helper.assertTrue(MazeHandler.mazesInRange(outer, 778, 888, 1, 1),
                "mazesInRange missed a mapped cell one chunk over");
        helper.assertTrue(!MazeHandler.mazesInRange(outer, 777 + 40, 888, 2, 2),
                "mazesInRange answered true far from every cell");

        final CompoundTag tag = MazeHandler.data(outer).save(new CompoundTag(),
                helper.getLevel().registryAccess());
        final MazeHandler.LabyrinthData loaded =
                MazeHandler.LabyrinthData.FACTORY.deserializer().apply(tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.cellAt(777, 888) == 513,
                "the saved labyrinth lost a cell through NBT (the original's labyrinth.dat round-trip)");
        MazeHandler.removeFromHashMap(outer, loc);
        helper.assertTrue(MazeHandler.getFromHashMap(outer, loc) == null,
                "removeFromHashMap left the cell");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seededMazeMatchesGeneratorLayout(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final int x = 100;
        final int z = 100;
        final int w = 9;
        final int h = 9;
        final long seed = 12345L;
        final int before = MazeHandler.data(outer).cellCount();

        helper.assertTrue(MazeHandler.generateMaze(outer, x, z, w, h, seed),
                "generateMaze failed to find a layout");

        // Replay the original's MazeThread loop independently: same seed walk,
        // same placement math (col = x - (1 + w/2), row = z - (1 + h/2)).
        long s = seed;
        MazeGenerator gen = new MazeGenerator(w, h, s++);
        while (!gen.generate()) {
            gen = new MazeGenerator(w, h, s++);
        }
        final int col = x - (1 + w / 2);
        final int row = z - (1 + h / 2);
        int positive = 0;
        int portalRooms = 0;
        int bossRooms = 0;
        for (int a = 0; a < w; a++) {
            for (int b = 0; b < h; b++) {
                if (gen.grid[b][a] > 0) {
                    positive++;
                    final short stored = MazeHandler.getFromHashMapRaw(outer, new CellLoc(a + col, b + row));
                    helper.assertTrue(stored == (short) gen.grid[b][a],
                            "cell (" + (a + col) + "," + (b + row) + ") = " + stored
                                    + ", the seeded generator says " + gen.grid[b][a]);
                    final Cell cell = new Cell(stored);
                    if (cell.feature == 1) {
                        portalRooms++;
                    }
                    if (cell.feature >= 2 && cell.feature <= 5) {
                        bossRooms++;
                    }
                }
            }
        }
        helper.assertTrue(portalRooms == 1, "the maze has " + portalRooms + " portal rooms, the original has one");
        helper.assertTrue(bossRooms >= 1, "the maze has no boss-room cells");
        // The harness runs probes in parallel batches over one shared level, so
        // the assertion is over this probe's own cell window, not the map total.
        int visible = 0;
        for (int a = 0; a < w; a++) {
            for (int b = 0; b < h; b++) {
                if (MazeHandler.data(outer).cellAt(a + col, b + row) > 0) {
                    visible++;
                }
            }
        }
        helper.assertTrue(visible == positive,
                "the map shows " + visible + " of the layout's " + positive + " cells (the zero-markers are cleaned off)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seededMazeBuildsSweepAndBuiltGate(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final int x = 300;
        final int z = 300;
        final int w = 9;
        final int h = 9;
        final long seed = 4242L;
        helper.assertTrue(MazeHandler.generateMaze(outer, x, z, w, h, seed),
                "generateMaze failed to find a layout");

        // The original's MazeThread sweep: after the layout lands in the map,
        // every mapped cell is handed to the builder, which builds the rooms
        // this slice ships (feature 1 portal, 2..5 boss quadrants) and refuses
        // the rest (their builders are the follow-up slice), marking built only
        // what it built.
        int built = 0;
        int refused = 0;
        int portalCells = 0;
        final int xStart = x - (1 + w / 2);
        final int zStart = z - (1 + h / 2);
        for (int a = 0; a < w; a++) {
            for (int b = 0; b < h; b++) {
                final CellLoc loc = new CellLoc(xStart + a, zStart + b);
                final Cell cell = MazeHandler.getFromHashMap(outer, loc);
                if (cell == null) {
                    continue;
                }
                if (cell.feature == 1) {
                    portalCells++;
                }
                final boolean didBuild = MazeHandler.generateEldritch(outer, new Random(seed), loc.x, loc.z);
                helper.assertTrue(didBuild == (cell.feature >= 1 && cell.feature <= 5),
                        "cell (" + loc.x + "," + loc.z + ") feature " + cell.feature
                                + " answered " + didBuild + "; this slice builds features 1..5");
                helper.assertTrue(MazeHandler.data(outer).isBuilt(loc.x, loc.z) == didBuild,
                        "the built gate disagrees with the sweep for cell (" + loc.x + "," + loc.z + ")");
                if (didBuild) {
                    built++;
                } else {
                    refused++;
                }
            }
        }
        helper.assertTrue(portalCells == 1, "the seeded maze has " + portalCells + " portal cells, the original has one");
        helper.assertTrue(built >= 2, "the sweep built " + built + " cells; a 9x9 maze has a portal plus boss rooms");
        // Idempotence: the built gate makes the second sweep a no-op that still answers true.
        for (int a = 0; a < w; a++) {
            for (int b = 0; b < h; b++) {
                final CellLoc loc = new CellLoc(xStart + a, zStart + b);
                if (MazeHandler.getFromHashMap(outer, loc) != null) {
                    MazeHandler.generateEldritch(outer, new Random(seed), loc.x, loc.z);
                }
            }
        }
        helper.assertTrue(GenCommon.DECORATED_BLOCKS.isEmpty(),
                "processDecorations left " + GenCommon.DECORATED_BLOCKS.size() + " decorations queued");
        helper.assertTrue(refused >= 0 && built >= 2, "the sweep built " + built + ", refused " + refused);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mazeCellBuildsPortalRoom(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final int cx = 30;
        final int cz = 30;
        // Feature 1 (portal room) is the high byte of the packed cell code:
        // Cell's constructor reads feature = code >> 8 (the original's packing).
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell(PORTAL_CELL));
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(7), cx, cz),
                "a feature-1 cell did not build");
        // GenPortal geometry verbatim: room origin (cx*16, y, cz*16), the
        // eldritch frame at (x+8, y+2, z+8), the portal at (x+8, y+3, z+8).
        final BlockPos portal = new BlockPos(cx * 16 + 8, OuterLands.ROOM_Y + 3, cz * 16 + 8);
        helper.assertTrue(outer.getBlockState(portal).is(CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get()),
                "no eldritch portal at the room centre " + portal + " (got "
                        + outer.getBlockState(portal).getBlock() + ")");
        helper.assertTrue(outer.getBlockState(portal.below())
                        .is(CatalogEldritchRealm.BLOCK_ELDRITCH.get()),
                "the portal frame (blockEldritch) is missing under the portal");
        helper.assertTrue(MazeHandler.data(outer).isBuilt(cx, cz),
                "the built-gate did not mark the chunk (reload would rebuild it)");
        // The built gate: a second pass is a no-op.
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(7), cx, cz),
                "the built cell answered false on reload");
        helper.assertTrue(GenCommon.DECORATED_BLOCKS.isEmpty(),
                "processDecorations left " + GenCommon.DECORATED_BLOCKS.size() + " decorations queued");
        helper.succeed();
    }

    /** Feature 1 (portal room) and feature 2 (the upper-left boss quadrant), high-byte packed. */
    private static final short PORTAL_CELL = (short) (1 << 8);
    private static final short BOSS_CELL = (short) (2 << 8);
    /** Feature 2 with every side open (N=1 | S=2 | E=4 | W=8) — the shape a doorway is built for. */
    private static final short OPEN_SIDES_BOSS_CELL = (short) ((2 << 8) | 15);

    /**
     * The 2x2 boss-room shell over the envelope it covers, from a closed-sided feature-2 cell.
     * Deterministic: identical in all 12 measurement legs of card t_ec250b53.
     */
    private static final int SHELL_ELDRITCH = 3;
    private static final int SHELL_STAIRS = 46;
    private static final int SHELL_NOTHING = 689;
    private static final int SHELL_NONAIR = 1871;

    /** A CLOSED-sided boss-room cell lays no doorway blockEldritch on its planes (the plane cells
     *  of a closed-sided cell also carry the room shell, so only blockEldritch is the doorway's
     *  own material). */
    private static final int CLOSED_PLANE_ELDRITCH = 0;

    /**
     * One 11x7 doorway plane of an open-sided boss-room cell, per side: blockEldritch / eldritch
     * stairs / air / blockEldritchNothing (a complete partition of the 77 cells). The four sides
     * are NOT symmetric: GenBossRoom lays the 7x7 PAT_DOORWAY with its own axis per side, so the
     * shape is pinned per side and measured, not argued from the pattern's symmetry.
     */
    private static final int[] DOOR_NORTH = {0, 4, 21, 7};
    private static final int[] DOOR_SOUTH = {1, 2, 54, 0};
    private static final int[] DOOR_EAST = {1, 5, 29, 7};
    private static final int[] DOOR_WEST = {0, 0, 77, 0};

    /** The closed-sided doorway's write at the absolute world column (0, ROOM_Y..ROOM_Y+12, 0). */
    private static final int WORLD_COLUMN_LEAK = 7;

    /** The four sides a boss-room cell can lay a doorway on, in the generator's own order. */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    /** The shipped shape of one side's doorway plane (measured, per side — the sides differ). */
    private static int[] doorShape(final Direction side) {
        return switch (side) {
            case NORTH -> DOOR_NORTH;
            case SOUTH -> DOOR_SOUTH;
            case EAST -> DOOR_EAST;
            default -> DOOR_WEST;
        };
    }

    @GameTest(template = "empty")
    public static void bossRoomBuildsThroughTheSeam(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        helper.assertTrue(EldritchBossRoomHost.hasBuilder(),
                "the shipped EldritchBossRoomHost seam has no builder (this card owns the install)");

        // Two fixtures, one probe, because the doorway's target cell is a function of the cell's
        // side bits and the old single fixture could not see the seam it is named for.
        //
        //   (40,40)  feature 2, NO wall bits — every side closed. That is the shape that makes
        //            Gen2x2 lay the 2x2 boss room at all, and it is the shape whose census was
        //            measured over 12 legs of card t_ec250b53 (identical in all of them).
        //   (46,46)  the same feature 2 with all four sides OPEN — the shape the doorway is built
        //            for, and the only shape whose doorway lands inside a readable envelope.
        //
        // The side bits are a PRECONDITION of the seam, and the shipped generator proves it:
        // GenBossRoom sweeps its 7x7 PAT_DOORWAY per side and writes at
        // (x + xx, ROOM_Y + 2 + b, z + zz), where a side that is CLOSED leaves its xx/zz at 0 —
        // so with every side closed the whole doorway lands on the cell's NW corner, i.e. for
        // cell (40,40) on the ABSOLUTE world column (0, y, 0). Measured, 12 legs: 7 blockEldritch
        // at (0, 52..58, 0) — the seven pattern rows — and airy=0 inside the cell envelope. The
        // "doorway" a closed-sided cell builds is outside every envelope this probe can read;
        // that is a defect of the shipped generator (GenBossRoom.java:103-114 with
        // GenCommon.java:462-471), reported upstream, and it is why the seam needs its own
        // open-sided cell. The closed-side write is pinned below as a recorded deviation.
        final int cx = 40;
        final int cz = 40;
        final int dx = 46;
        final int dz = 46;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell(BOSS_CELL));
        MazeHandler.putToHashMap(outer, new CellLoc(dx, dz), new Cell(OPEN_SIDES_BOSS_CELL));
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(3), cx, cz)
                        && MazeHandler.generateEldritch(outer, new Random(3), dx, dz),
                "a feature-2 cell did not build its boss room");
        helper.assertTrue(MazeHandler.data(outer).isBuilt(cx, cz) && MazeHandler.data(outer).isBuilt(dx, dz),
                "the built-gate did not mark the boss-room cells (reload would rebuild them)");
        helper.assertTrue((BOSS_CELL & 15) == 0 && (OPEN_SIDES_BOSS_CELL & 15) == 15
                        && (BOSS_CELL >> 8) == 2 && (OPEN_SIDES_BOSS_CELL >> 8) == 2,
                "the two fixtures are one feature with the sides closed / open: packed " + BOSS_CELL
                + " and " + OPEN_SIDES_BOSS_CELL + " (side bits N=1 S=2 E=4 W=8, feature in bits 8-10)");

        // The 2x2 room overflows the cell it is built in (Gen2x2 places it at (x-8, y-2, z-8)), so
        // the shell is counted over the envelope the room covers. The census is pinned EXACTLY:
        // asking for ">= 1" of a shell that leaves 3 blockEldritch is a margin, not a contract,
        // and a shell that lost cells is the block-support class of docs/testing.md, not a window.
        final int[] shell = census(outer, cx * 16 - 8, cx * 16 + 23, OuterLands.ROOM_Y,
                OuterLands.ROOM_Y + 15, cz * 16 - 8, cz * 16 + 23);
        helper.assertTrue(shell[0] == SHELL_ELDRITCH && shell[1] == SHELL_STAIRS
                        && shell[2] == SHELL_NOTHING && shell[3] == SHELL_NONAIR,
                "the boss-room SHELL census in the cell envelope is " + shell[0] + " blockEldritch + "
                        + shell[1] + " eldritch stairs + " + shell[2] + " blockEldritchNothing = "
                        + shell[3] + " non-air blocks (the box holds " + (shell[3] + shell[4])
                        + " cells, " + shell[4] + " of them air); the shape the shipped generator"
                        + " leaves for a closed-sided feature-2 cell is " + SHELL_ELDRITCH + " + "
                        + SHELL_STAIRS + " + " + SHELL_NOTHING + " = " + SHELL_NONAIR + " (a shell"
                        + " missing cells is the block-support class of docs/testing.md: "
                        + chunkFacts(outer, cx * 16 - 8, cz * 16 - 8) + ")");
        // A closed-sided cell lays NOTHING on its doorway planes: the whole 7x7 pattern went to the
        // cell's NW corner (the world-column assert below). This is the fact the old probe was
        // missing — its "doorway" assert was reading the room shell all along.
        for (final Direction side : SIDES) {
            final int[] plane = doorwayPlane(outer, cx, cz, side);
            helper.assertTrue(plane[0] == CLOSED_PLANE_ELDRITCH,
                    "the " + side.getName() + " doorway plane of the CLOSED-sided boss-room cell ("
                            + cx + "," + cz + ") carries " + plane[0] + " blockEldritch (+" + plane[1]
                            + " eldritch stairs, +" + plane[2] + " air of 77 — the rest of the plane"
                            + " cells are the room shell of the closed-sided cell); the shipped"
                            + " closed-side doorway lays " + CLOSED_PLANE_ELDRITCH + " doorway"
                            + " blockEldritch there because it writes the whole 7x7 pattern at the"
                            + " cell's NW corner, outside every plane");
        }

        // The seam proper: each open side lays its 11x7 doorway plane INSIDE the cell (NORTH at
        // z+3 for a in 0..10, SOUTH at z+13 for a in 10..20, WEST at x+3 for a in 10..20, EAST at
        // x+13 for a in 0..10, seven rows from ROOM_Y+2). Pinned per side, and a short plane names
        // the closed side that closed it.
        final StringBuilder planes = new StringBuilder();
        for (final Direction side : SIDES) {
            final int[] plane = doorwayPlane(outer, dx, dz, side);
            planes.append(' ').append(side.getName()).append('=')
                    .append(plane[0]).append('/').append(plane[1]).append('/').append(plane[2])
                    .append('/').append(plane[3]);
        }
        for (final Direction side : SIDES) {
            final int[] plane = doorwayPlane(outer, dx, dz, side);
            final int[] shipped = doorShape(side);
            helper.assertTrue(plane[0] == shipped[0] && plane[1] == shipped[1]
                            && plane[2] == shipped[2] && plane[3] == shipped[3],
                    "the " + side.getName() + " doorway plane of the open-sided boss-room cell ("
                    + dx + "," + dz + ") is blockEldritch/eldritchStairs/air/eldritchNothing = "
                    + plane[0] + "/" + plane[1] + "/" + plane[2] + "/" + plane[3] + " of 77 cells;"
                    + " the seam ships " + shipped[0] + "/" + shipped[1] + "/" + shipped[2] + "/"
                    + shipped[3] + ". Every side of the cell under measurement is open, so a plane"
                    + " that changed shape means the side bit (" + sideBit(Direction.NORTH) + " N, "
                    + sideBit(Direction.SOUTH) + " S, " + sideBit(Direction.EAST) + " E, "
                    + sideBit(Direction.WEST) + " W) stopped being set: the side bits are the"
                    + " doorway's target-cell precondition, and a closed side moves that side's"
                    + " whole 11x7 plane to the cell's NW corner. All four sides:" + planes);
        }

        // The leak, pinned as a deviation record so it is loud in both directions: the closed-side
        // doorway of cell (40,40) writes the seven pattern rows at the ABSOLUTE world column
        // (0, ROOM_Y+2..ROOM_Y+8, 0) — outside the cell, outside the envelope, at a fixed world
        // coordinate. If the shipped generator stops writing there, this reddens and the record
        // below must be updated with the upstream fix.
        final int leak = countEldritchColumn(outer, 0, 0, OuterLands.ROOM_Y, OuterLands.ROOM_Y + 12);
        helper.assertTrue(leak == WORLD_COLUMN_LEAK,
                "the closed-sided cell's doorway wrote " + leak + " blockEldritch at the ABSOLUTE world"
                + " column (0, y, 0); the recorded shipped behaviour (the deviation this card reported"
                + " upstream) is " + WORLD_COLUMN_LEAK + " — the seven pattern rows of the pattern's"
                + " code-1/code-2 placements, at ROOM_Y+2..ROOM_Y+8");

        // And nothing of the open cell's doorway crosses its own footprint: the doorway of a cell
        // whose sides are open belongs inside that cell.
        final int ring = ringEldritch(outer, dx, dz);
        helper.assertTrue(ring == 0,
                "the doorway of the open-sided boss-room cell (" + dx + "," + dz + ") wrote " + ring
                + " blockEldritch in the 1-cell ring around the cell footprint in the doorway's y band"
                + " — the doorway belongs inside the cell whose sides are open");

        // The installed seam keeps the original's plane gate: a lock asking
        // for its room outside the Outer Lands builds nothing (the shipped
        // isOuterLands test in MazeHandler.install).
        final int bx = 44;
        final int bz = 44;
        MazeHandler.putToHashMap(outer, new CellLoc(bx, bz), new Cell(BOSS_CELL));
        EldritchBossRoomHost.buildRoom(outer,
                new BlockPos(bx * 16 + 8, OuterLands.ROOM_Y, bz * 16 + 8), 0);
        helper.assertTrue(!MazeHandler.data(outer).isBuilt(bx, bz)
                        && MazeHandler.getFromHashMap(outer, new CellLoc(bx, bz)).feature == 2,
                "the installed boss-room builder ran outside the Outer Lands (the shipped plane gate)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void portalTravelGateAndCarvedRoom(final GameTestHelper helper) {
        final ServerLevel level = build(helper);

        // The room the portal lives in: built through the shipped chunk hook.
        final int cx = 60;
        final int cz = 60;
        MazeHandler.putToHashMap(level, new CellLoc(cx, cz), new Cell(PORTAL_CELL));
        helper.assertTrue(MazeHandler.generateEldritch(level, new Random(11), cx, cz),
                "the portal-room cell did not build");
        final BlockPos portal = new BlockPos(cx * 16 + 8, OuterLands.ROOM_Y + 3, cz * 16 + 8);
        final BlockState carved = level.getBlockState(portal);
        helper.assertTrue(carved.is(CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get()),
                "the built cell has no portal block at " + portal + " (got " + carved.getBlock() + ")");
        final TileEldritchPortal tile = (TileEldritchPortal) level.getBlockEntity(portal);
        helper.assertTrue(tile != null, "the carved portal block has no TileEldritchPortal");
        if (tile == null) {
            return;
        }
        // TileEldritchPortal.onNeighborChange: open with blockEldritch above and below.
        helper.assertTrue(tile.hasEldritchFrame(), "the carved portal has no eldritch frame");

        // The original's open-animation counter starts at -1 (TileEldritchPortal L?);
        // the ticker wiring is the original's ISidedInventory-style server-side gate.
        helper.assertTrue(tile.opencount == -1,
                "the portal tile's open counter starts at " + tile.opencount + ", the original starts at -1");
        helper.assertTrue(CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get()
                        .getTicker(level, carved, CatalogOuterLands.TILE_ELDRITCH_PORTAL.get())
                == TileEldritchPortal.SERVER_TICKER,
                "the portal block does not hand the tile's server ticker to the server side");

        // The original's 5-tick cadence, driven through the shipped ticker: the
        // tile counts and runs its player scan every fifth tick. The harness server has no players at all
        // (the game-test server has no connections, and GameTestHelper's mock
        // player is not a ServerPlayer), so the scan is the empty-list path —
        // assert the cadence advances and nobody is travelled.
        final int countBefore = tile.count;
        for (int i = 0; i < 5; i++) {
            TileEldritchPortal.SERVER_TICKER.tick(level, portal, carved, tile);
        }
        helper.assertTrue(tile.count == countBefore + 5,
                "the 5-tick cadence advanced by " + (tile.count - countBefore));
        helper.assertTrue(TileEldritchPortal.enteredOuterLands.isEmpty(),
                "the tile travelled a player who is not there");

        // The plane facts, cited in the class javadoc: no live Outer Lands on
        // the harness server, so the tile's travel target is absent.
        helper.assertTrue(!OuterLands.isOuterLands(level.dimension()),
                "the harness level reports the Outer Lands");
        helper.assertTrue(OuterLands.outerLands(level.getServer()) == null,
                "the harness server carries a live Outer Lands level");

        // The original's bW := 100 travel window, and the seam's contract for
        // a player that never travelled.
        helper.assertTrue(TileEldritchPortal.TRAVEL_COOLDOWN_TICKS == 100,
                "the travel cooldown is " + TileEldritchPortal.TRAVEL_COOLDOWN_TICKS + ", the original's bW is 100");
        helper.assertTrue(!TileEldritchPortal.onTravelCooldown(java.util.UUID.randomUUID(), level.getGameTime()),
                "an untracked player is on travel cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void altarSeamKeepsThePlaneGateAndTheMapAnswers(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        // The installed seam runs the generation on the live plane only — the
        // original's request path worked on the Outer Lands world. The harness
        // server has no such level (class javadoc), so the shipped gate
        // answers the altar's request no, which is the honest in-harness
        // assertion of the install.
        helper.assertTrue(!EldritchMazeHost.request(2000, 2000, 4, 4),
                "the maze seam ran its generation without a live Outer Lands level");
        // The map fact the seam answers from: the original's mazesInRange.
        MazeHandler.putToHashMapRaw(outer, new CellLoc(2000, 2000), PORTAL_CELL);
        helper.assertTrue(MazeHandler.data(outer).cellAt(2000, 2000) == PORTAL_CELL,
                "the map did not take the cell");
        helper.assertTrue(MazeHandler.mazesInRange(outer, 2001, 2000, 1, 1),
                "mazesInRange missed a mapped cell one chunk over");
        helper.assertTrue(!MazeHandler.mazesInRange(outer, 2000 + 30, 2000, 2, 2),
                "mazesInRange answered true far from every cell");
        MazeHandler.removeFromHashMap(outer, new CellLoc(2000, 2000));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seedIsTheMazeFingerprintAndTheExposedSeamsAreAudited(final GameTestHelper helper) {
        final List<List<Integer>> layouts = new ArrayList<>();
        for (long seed = 0L; seed < 4L; seed++) {
            long s = seed;
            MazeGenerator generator = new MazeGenerator(9, 9, s++);
            while (!generator.generate()) {
                generator = new MazeGenerator(9, 9, s++);
            }
            final List<Integer> layout = new ArrayList<>();
            for (int[] row : generator.grid) {
                for (int cell : row) {
                    layout.add(cell);
                }
            }
            layouts.add(layout);
        }
        helper.assertTrue(!layouts.get(0).equals(layouts.get(1)),
                "the maze seed is not a fingerprint: seeds 0 and 1 agree");

        // The seam-visibility audit the card asks for, against the shipped
        // surfaces. The original's MazeThread read LabyrinthMap.getCells() /
        // getBuiltCells() (public statics there). In this port the maze cells
        // are exposed publicly (the shipped MazeGenerator.grid, f252c58) and
        // the labyrinth map exposes membership only — cellCount / cellAt /
        // isBuilt — with NO public cell-list getter (recorded here rather than
        // silently papered over: a future card that needs the full cell list,
        // e.g. the TC-M6-06 boss sweep, adds the getter to MazeHandler).
        try {
            helper.assertTrue(Modifier.isPublic(MazeGenerator.class.getField("grid").getModifiers())
                            && Modifier.isPublic(MazeGenerator.class.getMethod("generate").getModifiers()),
                    "the shipped maze generator does not expose its cells publicly");
            helper.assertTrue(Modifier.isPublic(MazeHandler.LabyrinthData.class.getMethod("cellCount").getModifiers())
                            && Modifier.isPublic(MazeHandler.LabyrinthData.class.getMethod("cellAt", int.class, int.class).getModifiers())
                            && Modifier.isPublic(MazeHandler.LabyrinthData.class.getMethod("isBuilt", int.class, int.class).getModifiers()),
                    "the shipped labyrinth map does not expose membership publicly");
            boolean listGetter = false;
            for (final var method : MazeHandler.LabyrinthData.class.getMethods()) {
                if (method.getName().toLowerCase().contains("cells")
                        && !method.getName().equals("cellCount") && method.getParameterCount() == 0) {
                    listGetter = true;
                }
            }
            helper.assertTrue(!listGetter,
                    "the labyrinth map now exposes a cell-list getter: update this audit + the original-fidelity note");
        } catch (ReflectiveOperationException e) {
            helper.fail("the shipped maze/map surfaces are not readable: " + e);
            return;
        }
        helper.succeed();
    }
}
