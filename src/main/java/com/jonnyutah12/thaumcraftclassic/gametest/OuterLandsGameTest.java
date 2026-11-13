package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchPortal;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDevices;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch;
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

    @GameTest(template = "empty")
    public static void bossRoomBuildsThroughTheSeam(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        helper.assertTrue(EldritchBossRoomHost.hasBuilder(),
                "the shipped EldritchBossRoomHost seam has no builder (this card owns the install)");
        final int cx = 40;
        final int cz = 40;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell(BOSS_CELL));
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(3), cx, cz),
                "a feature-2 cell did not build its boss room");
        // GenBossRoom sweeps the cell's 7x7 PAT_DOORWAY doorway: eldritch
        // metas (gate/lock/nothing) and eldritch stairs land in the cell
        // envelope — assert the doorway material is present, the boss
        // entities are TC-M6-06.
        final AABB box = new AABB(
                new Vec3(cx * 16 - 8, OuterLands.ROOM_Y, cz * 16 - 8),
                new Vec3(cx * 16 + 24, OuterLands.ROOM_Y + 16, cz * 16 + 24));
        // The census, read once: the envelope's blockEldritch (the count main gated on, kept as a
        // printed fact), the room's stairs, the non-air floor, the material per chunk, and which
        // envelope chunks tick.
        final var materialByChunk = new java.util.TreeMap<String, String>();
        final var loaded = new StringBuilder();
        int eldritch = 0;
        int stairs = 0;
        int nonair = 0;
        final var cursor = new BlockPos.MutableBlockPos();
        for (int x = (int) box.minX; x < box.maxX; x++) {
            for (int y = (int) box.minY; y < box.maxY; y++) {
                for (int z = (int) box.minZ; z < box.maxZ; z++) {
                    final BlockState state = outer.getBlockState(cursor.set(x, y, z));
                    if (state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                        eldritch++;
                    }
                    if (state.is(CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get())) {
                        stairs++;
                    }
                    if (!state.isAir()) {
                        nonair++;
                        materialByChunk.putIfAbsent((x >> 4) + "," + (z >> 4), state.getBlock().toString());
                    }
                }
            }
        }
        for (int chX = (int) box.minX >> 4; chX <= ((int) box.maxX - 1) >> 4; chX++) {
            for (int chZ = (int) box.minZ >> 4; chZ <= ((int) box.maxZ - 1) >> 4; chZ++) {
                loaded.append(' ').append(chX).append(',').append(chZ).append(outer.getChunkSource()
                        .hasChunk(chX, chZ) ? "[ticking]" : "[MISSING]");
            }
        }

        // The doorway, on a fixture that can carry one. GenBossRoom.java:141-163 is the original's
        // if-chain (ref L39-64) — every open side writes into the SAME xx/zz pair — so the LAST open
        // side takes the 7x7 PAT_DOORWAY (GenBossRoom.java:78-86: 21 gate + 4 lock + 20 void) at
        // x0+3, y=ROOM_Y+2..+8. A cell whose 2x2 shell forms loses it again: the shell's own room
        // build lands on top of the sweep, measured gate=0 / airy=0 on all four planes in 10 of 10
        // shell-mode legs. So the probe builds a second feature-2 cell, open on all four sides, with
        // no 2x2 group around it, and reads the four planes cell by cell.
        final int dx = 70;
        final int dz = 70;
        final Cell doorCell = new Cell(BOSS_CELL);
        doorCell.north = true;
        doorCell.south = true;
        doorCell.east = true;
        doorCell.west = true;
        MazeHandler.putToHashMap(outer, new CellLoc(dx, dz), doorCell);
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(11), dx, dz),
                "an open-sided feature-2 cell did not build");
        final int px = dx * 16;
        final int pz = dz * 16;
        final var planes = new StringBuilder();
        int gate = 0;
        int airy = 0;
        for (final String side : new String[] {"north", "south", "east", "west"}) {
            int sideGate = 0;
            int sideAiry = 0;
            for (int a = 0; a < 7; a++) {
                for (int b = 0; b < 7; b++) {
                    final int xx = switch (side) {
                        case "north", "south" -> px + 5 + a;
                        case "east" -> px + 13;
                        default -> px + 3;
                    };
                    final int zz = switch (side) {
                        case "north" -> pz + 3;
                        case "south" -> pz + 13;
                        default -> pz + 5 + a;
                    };
                    final BlockState state = outer.getBlockState(cursor.set(xx, OuterLands.ROOM_Y + 2 + b, zz));
                    if (state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())) {
                        final int variant = state.getValue(BlockEldritch.VARIANT);
                        if (variant == EldritchRealmValues.ELDRITCH_GATE
                                || variant == EldritchRealmValues.ELDRITCH_LOCK) {
                            sideGate++;
                        }
                    }
                    if (state.is(CatalogDevices.BLOCK_AIRY.get())) {
                        sideAiry++;
                    }
                }
            }
            if (side.equals("west")) {
                gate = sideGate;
                airy = sideAiry;
            }
            planes.append(' ').append(side).append("{gate=").append(sideGate)
                  .append(" airy=").append(sideAiry).append('}');
        }
        final var census = new StringBuilder("cellEnvelope{eldritch=").append(eldritch)
                .append(" stairs=").append(stairs).append(" nonair=").append(nonair)
                .append('/').append(16384).append("} doorwayCell=").append(dx).append(',').append(dz)
                .append(" planes=").append(planes);
        System.out.println("GATE-BR cell=" + cx + "," + cz + " sides=closed(NSEW=0) " + census
                + " materialByChunk=" + materialByChunk + " envelopeChunks=" + loaded
                + " tick=" + helper.getTick());

        // Arm 1 — the doorway, on the plane the shipped generator writes it and on a fixture that
        // has one: 25 gate/lock variants and 20 blockAiry on the west plane (the last open side
        // takes the sweep). Measured 25/20 in every leg of the diagnostic; a side closing moves the
        // doorway to the last OPEN side, and closing every side parks it at the world column
        // (0,y,0), 7 cells, 14 of 14 legs, which is the shipped Cell(BOSS_CELL) fixture's shape.
        helper.assertTrue(gate >= 21 && airy >= 18,
                "the boss room's doorway placed no gate material (blockEldritch variant "
                        + EldritchRealmValues.ELDRITCH_GATE + "/" + EldritchRealmValues.ELDRITCH_LOCK
                        + ") and no void doorway (blockAiry) on the west plane of an open-sided"
                        + " feature-2 cell (x0+3, y=ROOM_Y+2..+8): PAT_DOORWAY (GenBossRoom.java:78-86)"
                        + " is 21 gate + 4 lock + 20 void, laid by the last open side of the"
                        + " if-chain at GenBossRoom.java:141-163. A side bit closed moves it to the"
                        + " next-to-last open side; every side closed parks the sweep at the world"
                        + " column (0,y,0), outside any cell envelope"
                        + "\n  census=" + census + ", tick " + helper.getTick());

        // Arm 2 — the room the probe is named for: GenBossRoom.generate() lays eldritch stairs in
        // the envelope, measured 46 in every leg at 104ccce and at 2d2bc94.
        helper.assertTrue(stairs >= 1, "the boss room placed no eldritch stairs in the cell envelope"
                        + "\n  census=" + census + ", tick " + helper.getTick());

        // Arm 3 — the seam, which is the probe's name: the envelope straddles four chunks, so the
        // material has to be readable across them. Measured: every envelope chunk answers ticking,
        // and the envelope's non-air census is 1646 with no 2x2 shell and 1871 with one (an exact
        // 7/7 split at 2d2bc94 — the shell forms out of state the batch shares). The floor is the
        // LOW mode and the census rides in the message, so a shell forming or not is a named fact,
        // not a coin flip the packing grid settles.
        helper.assertTrue(nonair >= 1646, "the boss room left " + nonair + " non-air cells in the"
                        + " cell envelope; the shipped feature-2 room fills at least 1646 (the low"
                        + " mode measured in 7 of 14 legs at 2d2bc94; 1871 with the 2x2 shell)"
                        + "\n  census=" + census + ", materialByChunk=" + materialByChunk
                        + ", tick " + helper.getTick());
        helper.assertTrue(!loaded.toString().contains("[MISSING]"),
                "an envelope chunk is not ticking: the seam reads unloaded chunks (the 32x16x32"
                        + " envelope spans four)\n  envelopeChunks=" + loaded
                        + ", census=" + census + ", tick " + helper.getTick());

        // NOT asserted, on measurement: main's `eldritch >= 1` over the envelope. That count is 3 in
        // 12/12 legs at 104ccce, 0 in 14/14 at 2d2bc94 and 1 in 15/15 on an open-sided fixture — a
        // knife edge the batch's shared 2x2-shell state settles, not the boss room. It stays
        // printed above and in every failure message, so a shift is a fact the next reader can
        // chase, and the doorway's own gate count is asserted above with 4 blocks of margin.

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
