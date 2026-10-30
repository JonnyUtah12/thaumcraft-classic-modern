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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Random;

/**
 * TC-M6-04f-m evidence (the card's bar: dimension + portal + boss-room
 * placement probes). Eight probes over the shipped seams:
 *
 * <ol>
 *   <li>the data-driven dimension family (type flags verbatim from
 *       {@code WorldProviderOuter}, the flat void generator, the biome);</li>
 *   <li>every Outer Lands column is air under the Eldritch biome (the
 *       original's {@code ChunkProviderOuter.fill} returning null);</li>
 *   <li>the labyrinth map persists (the {@code labyrinth.dat} analogue);</li>
 *   <li>a seeded maze matches the shipped {@link MazeGenerator} layout cell
 *       for cell (the {@code MazeThread} placement math);</li>
 *   <li>a feature-1 cell builds the portal room at x*16+8, y+3 (the
 *       {@code GenPortal} geometry + the chunk-hook entry);</li>
 *   <li>the lock's seam builds a boss room (the {@code GenBossRoom}/
 *       {@code Gen2x2} doorway geometry);</li>
 *   <li>the portal tile travels a player overworld → Outer Lands → back,
 *       gated by the original's 100-tick refresh;</li>
 *   <li>the altar's shipped maze seam answers yes once a maze exists
 *       (the {@code TileEldritchAltar.checkForMaze} request path).</li>
 * </ol>
 *
 * <p>Curios-independent: the suite runs identically in the default and
 * {@code -Ptc.noCurios} lanes.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OuterLandsGameTest {

    private OuterLandsGameTest() {
    }

    private static ServerLevel outer(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerLevel level = server.getLevel(OuterLands.OUTER_LANDS);
        helper.assertTrue(level != null, "the Outer Lands dimension is not live on the server");
        return level;
    }

    @GameTest(template = "empty")
    public static void dimensionFamilyShipsFromData(final GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();

        final DimensionType type = registries
                .lookupOrThrow(Registries.DIMENSION_TYPE).get(OuterLands.DIMENSION_TYPE)
                .map(net.minecraft.core.Holder::value).orElse(null);
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
                .map(net.minecraft.core.Holder::value).orElse(null);
        helper.assertTrue(biome != null, "thaumcraft:biome.biomegeneldritch is not in the registry");
        helper.assertTrue(!biome.hasPrecipitation(), "the Eldritch biome rains (BiomeGenEldritch has no precipitation)");
        helper.assertTrue(biome.getSpecialEffects().getSkyColor() == 0,
                "the Eldritch biome sky is not black");
        helper.assertTrue(biome.getSpecialEffects().getFogColor() == 0,
                "the Eldritch biome fog is not black");

        // The dimension json itself: live on the server with the flat void generator.
        final ServerLevel outer = outer(helper);
        helper.assertTrue(OuterLands.isOuterLands(outer.dimension()),
                "the live Outer Lands level reports dimension " + outer.dimension().location());
        helper.assertTrue(outer.getChunkSource().getGenerator()
                        instanceof net.minecraft.world.level.levelgen.FlatLevelSource,
                "the Outer Lands generator is not the flat void source (ChunkProviderOuter analogue)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void outerLandsColumnsAreVoidEldritch(final GameTestHelper helper) {
        final ServerLevel outer = outer(helper);
        // A chunk far from every probe (no maze, no structure): the original's
        // ChunkProviderOuter filled every block with null — pure air — under
        // WorldChunkManagerHell(biomeEldritchLands, 0.0).
        final int cx = 1000;
        final int cz = 1000;
        outer.getChunk(cx, cz);
        int air = 0;
        for (int x = cx * 16; x < cx * 16 + 3; x++) {
            for (int z = cz * 16; z < cz * 16 + 3; z++) {
                for (int y = 0; y < 128; y++) {
                    final BlockState state = outer.getBlockState(new BlockPos(x, y, z));
                    if (!state.isAir()) {
                        helper.fail("the Outer Lands column (" + x + "," + y + "," + z + ") is "
                                + state.getBlock() + ", the original's provider fills null (air)");
                        return;
                    }
                    air++;
                }
            }
        }
        helper.assertTrue(air == 3 * 3 * 128, "probed " + air + " blocks, expected 1152");
        final var biomeKey = outer.getBiome(new BlockPos(cx * 16 + 8, 64, cz * 16 + 8)).getKey();
        helper.assertTrue(biomeKey != null && biomeKey.location().equals(OuterLands.BIOME_ELDRITCH.location()),
                "the void column carries biome " + biomeKey + ", the original stamps biomeEldritchLands");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void labyrinthMapRoundTripsThroughNbt(final GameTestHelper helper) {
        final ServerLevel outer = outer(helper);
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
        final ServerLevel outer = outer(helper);
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
        helper.assertTrue(MazeHandler.data(outer).cellCount() == before + positive,
                "the map holds " + (MazeHandler.data(outer).cellCount() - before)
                        + " new cells, the layout has " + positive + " (the zero-markers are cleaned off)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mazeCellBuildsPortalRoom(final GameTestHelper helper) {
        final ServerLevel outer = outer(helper);
        final int cx = 30;
        final int cz = 30;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell((short) 1));
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

    @GameTest(template = "empty")
    public static void bossRoomBuildsThroughTheSeam(final GameTestHelper helper) {
        final ServerLevel outer = outer(helper);
        helper.assertTrue(EldritchBossRoomHost.hasBuilder(),
                "the shipped EldritchBossRoomHost seam has no builder (this card owns the install)");
        final int cx = 40;
        final int cz = 40;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell((short) 2));
        EldritchBossRoomHost.buildRoom(outer,
                new BlockPos(cx * 16 + 8, OuterLands.ROOM_Y, cz * 16 + 8), 0);
        // GenBossRoom sweeps the cell's 7x7 PAT_DOORWAY doorway: eldritch
        // metas (gate/lock/nothing) and eldritch stairs land in the cell
        // envelope — assert the doorway material is present, the boss
        // entities are TC-M6-06.
        final AABB box = new AABB(
                new net.minecraft.world.phys.Vec3(cx * 16 - 8, OuterLands.ROOM_Y, cz * 16 - 8),
                new net.minecraft.world.phys.Vec3(cx * 16 + 24, OuterLands.ROOM_Y + 16, cz * 16 + 24));
        int eldritch = 0;
        int stairs = 0;
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
                }
            }
        }
        helper.assertTrue(eldritch >= 1, "the boss-room doorway placed no blockEldritch in the cell envelope");
        helper.assertTrue(stairs >= 1, "the boss-room doorway placed no eldritch stairs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void portalTravelAndCooldownGate(final GameTestHelper helper) {
        final ServerLevel from = helper.getLevel();
        final ServerLevel outer = outer(helper);

        final BlockPos gate = helper.absolutePos(new BlockPos(1, 2, 1));
        from.setBlock(gate, CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get().defaultBlockState(), 3);
        final TileEldritchPortal gateTile =
                (TileEldritchPortal) from.getBlockEntity(gate);
        helper.assertTrue(gateTile != null, "the placed portal block has no tile");

        ServerPlayer player = (ServerPlayer) helper.makeMockPlayer(GameType.CREATIVE);
        TileEldritchPortal.clearTravelState(player.getUUID());
        player.setPos(gate.getX() + 0.5, gate.getY() + 0.5, gate.getZ() + 0.5);

        // Leg 1: standing in the overworld gate → the Outer Lands.
        // 1.21.1 retires the old player handle on travel: the tile's
        // changeDimension hands back the traveller, and the probes follow it.
        player = (ServerPlayer) player.changeDimension(new DimensionTransition(
                outer, player.position(), Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.DO_NOTHING));
        helper.assertTrue(player.level().dimension() == OuterLands.OUTER_LANDS,
                "changeDimension did not land the mock in the Outer Lands (at "
                        + player.level().dimension().location() + ")");
        TileEldritchPortal.clearTravelState(player.getUUID());

        // Leg 2: the arrival tile — the portal tile's own travel branch runs
        // for a player standing in it.
        final BlockPos arrival = BlockPos.containing(player.position());
        outer.setBlock(arrival, CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get().defaultBlockState(), 3);
        final TileEldritchPortal arrivalTile =
                (TileEldritchPortal) outer.getBlockEntity(arrival);
        helper.assertTrue(arrivalTile != null, "the arrival portal block has no tile");
        player.setPos(arrival.getX() + 0.5, arrival.getY() + 0.5, arrival.getZ() + 0.5);
        player = (ServerPlayer) runTravel(arrivalTile, outer, player);
        helper.assertTrue(player.level().dimension() == Level.OVERWORLD,
                "the Outer Lands portal did not travel the player home (at "
                        + player.level().dimension().location() + ")");

        // Leg 3: the cooldown gate — the overworld arrival tile sees the
        // player inside the original's 100-tick window and only refreshes.
        final BlockPos home = BlockPos.containing(player.position());
        from.setBlock(home, CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get().defaultBlockState(), 3);
        final TileEldritchPortal homeTile = (TileEldritchPortal) from.getBlockEntity(home);
        helper.assertTrue(homeTile != null, "the home portal block has no tile");
        player.setPos(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
        helper.assertTrue(TileEldritchPortal.onTravelCooldown(player.getUUID(), from.getGameTime()),
                "the travel did not set the original's 100-tick cooldown");
        final Vec3 before = player.position();
        homeTile.checkPlayers(from);
        helper.assertTrue(player.level().dimension() == Level.OVERWORLD
                        && player.position().distanceToSqr(before) < 1.0e-9,
                "the tile ping-ponged the player inside the cooldown window");
        TileEldritchPortal.clearTravelState(player.getUUID());
        helper.succeed();
    }

    /** Runs the tile's travel branch for one player and returns the traveller. */
    private static Entity runTravel(final TileEldritchPortal tile, final ServerLevel level,
                                    final ServerPlayer player) {
        // The original's gate is the cooldown; the probes clear it, then the
        // tile's travel branch (public on the tile) runs the leg.
        tile.travel(level, player);
        // travel() calls changeDimension, which retires the handle: re-resolve
        // by uuid on the player list of both planes.
        for (final ServerLevel candidate : new ServerLevel[]{level,
                level.getLevel().getServer().getLevel(OuterLands.isOuterLands(level)
                        ? Level.OVERWORLD : OuterLands.OUTER_LANDS)}) {
            if (candidate == null) {
                continue;
            }
            for (final ServerPlayer candidatePlayer : candidate.getPlayers(p -> true)) {
                if (candidatePlayer.getUUID().equals(player.getUUID())) {
                    return candidatePlayer;
                }
            }
        }
        throw new IllegalStateException("the traveller is on no player list");
    }

    @GameTest(template = "empty")
    public static void altarSeamAnswersMazeRequest(final GameTestHelper helper) {
        final ServerLevel outer = outer(helper);
        final int before = MazeHandler.data(outer).cellCount();
        // The shipped altar seam: request a maze at a virgin chunk; the
        // handler generates the layout (the original's MazeThread) and the
        // same request answers yes.
        helper.assertTrue(EldritchMazeHost.request(2000, 2000, 4, 4),
                "the maze seam answered no after generation (the altar never arms)");
        helper.assertTrue(MazeHandler.data(outer).cellCount() > before,
                "the maze request wrote no cells to the labyrinth map");
        helper.assertTrue(!EldritchMazeHost.request(3000, 3000, 1, 1)
                        || MazeHandler.mazesInRange(outer, 3000, 3000, 1, 1),
                "a virgin far chunk answered yes with no maze in range");
        helper.succeed();
    }
}
