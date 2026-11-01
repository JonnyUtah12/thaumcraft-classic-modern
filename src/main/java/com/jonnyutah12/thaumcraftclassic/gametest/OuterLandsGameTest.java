package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticSolid;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCosmeticStoneSlab;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockCrystal;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockEldritch;
import com.jonnyutah12.thaumcraftclassic.blocks.BlockLoot;
import com.jonnyutah12.thaumcraftclassic.items.ItemEldritchObject;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogDecorativeBlocks;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogTrinketsLoot;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchAltar;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchPortal;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.Cell;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.CellLoc;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchBossRoomHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMazeHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchMobHost;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
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
import java.util.function.Predicate;

/**
 * TC-M6-04f-m + TC-M6-04f-m2 evidence (the parent's bar: dimension + portal +
 * boss-room placement probes; this card's bar: the maze features 6..14 the
 * parent refused, plus the altar's {@code checkForMaze} caller leg). Fifteen
 * probes:
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
 *       every feature the layout draws builds and marks built (TC-M6-04f-m
 *       built only 1..5 and refused the rest), and the built gate makes the
 *       second sweep a no-op;</li>
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
 *   <li>a feature-6 cell builds the key room: the shell that starts one above
 *       the floor, the void lining, the purple-stair case, the eldritch cap
 *       pedestal, the Runed Tablet (eldritch object meta 2) that stands in for
 *       the unregistered {@code EntityPermanentItem} — meta, spot and the
 *       never-despawn rule — and the difficulty-counted guardian escort through
 *       the {@code EldritchMobHost} seam (the {@code GenKeyRoom} geometry, ref
 *       L21-116);</li>
 *   <li>a feature-7 cell builds the nest room: the floor-level shell, the void
 *       floor under the crusted one, the crusted spawner platform up the centre
 *       column and the upper platform, the ceiling crystals (blockCrystal meta
 *       7) and the crate/urn loot ring (the {@code GenNestRoom} geometry, ref
 *       L20-84);</li>
 *   <li>a feature-8 cell builds the library room: the twelve-deep shell, the
 *       ancient-stone pedestal (meta 15) / glyphed eldritch (meta 5) / eldritch
 *       slab ({@code blockSlabStone} meta 1) / double slab (meta 9) stack at
 *       the corners and in the centre column, and the stair runs (the
 *       {@code GenLibraryRoom} geometry, ref L21-99);</li>
 *   <li>features 0 and 9..14 each build their passage through the fall-through
 *       leg (reference {@code MazeHandler.java:161-180}), and the features
 *       GenCommon decorates off {@code cell.feature} are pinned: 11 the
 *       interior bedrock (code 20), 12 the crusted shell (code 22), 13 the
 *       taint-biome leg (the shipped {@code BiomeAura} seam, a no-op while no
 *       taint biome is registered), 14 the mob spawner at (x+8,y+4,z+8) with
 *       its cobweb box (ref L240-276);</li>
 *   <li>the altar tile's own {@code checkForMaze} leg reaches the maze seam
 *       with the request the original draws: the altar's chunk, an odd
 *       15..29 per side ({@code 15 + random.nextInt(8) * 2}) and a seed, drawn
 *       per call, while the shipped install keeps answering no without a live
 *       Outer Lands level (reference {@code TileEldritchAltar.java:224-236}).</li>
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
        // every mapped cell is handed to the builder. TC-M6-04f-m built only
        // feature 1 (portal) and 2..5 (boss quadrants) and refused the rest;
        // TC-M6-04f-m2 lands the remaining legs of the GenMaze switch (6 key,
        // 7 nest, 8 library, 9..14 passages — reference MazeHandler.java:
        // 142-168), so every feature the layout draws now builds, and the
        // built gate marks exactly what was built.
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
                helper.assertTrue(didBuild,
                        "cell (" + loc.x + "," + loc.z + ") feature " + cell.feature
                                + " answered " + didBuild + "; the maze builds every feature the layout draws");
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

    // ------------------------------------------------------------------
    // TC-M6-04f-m2 — the maze features the parent slice refused.
    //
    // The harness has no live Outer Lands ServerLevel (its level-stem registry
    // is empty when the world is baked — GameTestServer.create), so each probe
    // below puts its cell in the harness level and reads what landed there, as
    // mazeCellBuildsPortalRoom and bossRoomBuildsThroughTheSeam do. Every probe
    // owns a 16x16 cell window of its own and counts only inside it: the
    // harness runs probes in parallel batches on ONE shared level and one
    // shared labyrinth map, so a whole-map count would race with siblings.
    // ------------------------------------------------------------------

    /** Features 6 (key), 7 (nest) and 8 (library), high-byte packed (Cell: feature = code >> 8). */
    private static final short KEY_ROOM_CELL = (short) (6 << 8);
    private static final short NEST_ROOM_CELL = (short) (7 << 8);
    private static final short LIBRARY_ROOM_CELL = (short) (8 << 8);

    @GameTest(template = "empty")
    public static void mazeCellBuildsKeyRoom(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final int cx = 70;
        final int cz = 70;
        final int x = cx * 16;
        final int z = cz * 16;
        final int y = OuterLands.ROOM_Y;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell(KEY_ROOM_CELL));

        // GenKeyRoom's escort runs through the EldritchMobHost seam (the
        // guardians are TC-M6-06). The probe claims the seam for the duration
        // and records only spots inside its own cell window, so a sibling probe
        // summoning a guardian at its own structure cannot land in this count;
        // the seam goes back to the unregistered no-op afterwards.
        final List<BlockPos> guardians = new ArrayList<>();
        EldritchMobHost.register(EldritchMobHost.GUARDIAN, (level, pos) -> {
            if (pos.getX() >= x && pos.getX() < x + 16 && pos.getZ() >= z && pos.getZ() < z + 16) {
                guardians.add(pos.immutable());
            }
            return false;
        }, () -> true);
        final boolean built;
        try {
            built = MazeHandler.generateEldritch(outer, new Random(11), cx, cz);
        } finally {
            EldritchMobHost.register(EldritchMobHost.GUARDIAN, (level, pos) -> false, () -> false);
        }
        helper.assertTrue(built, "a feature-6 cell did not build");
        helper.assertTrue(MazeHandler.data(outer).isBuilt(cx, cz), "the built gate did not mark the key-room cell");

        // The bedrock shell: ref L21-24 runs c over 1..10 and places while
        // c < 10, so the shell's lowest block sits one above the room floor
        // (the nest room's shell starts at the floor — the two rooms differ
        // here, and the probe pins each). The interior is the eldritch void
        // (code 8, ref L25-27) and the (3,*,3) corner is the purple stair
        // (code 3, ref L35).
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 1, y + 1, z + 1)).is(Blocks.BEDROCK),
                "the key room's bedrock shell is missing at its lowest corner");
        helper.assertTrue(isVoid(outer.getBlockState(new BlockPos(x + 2, y + 1, z + 2))),
                "the key room's eldritch void lining (code 8) is missing");
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 3, y + 1, z + 3))
                        .is(CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get()),
                "the key room's purple stair (code 3) is missing at (x+3,y+1,z+3)");
        // The floor (ref L51-53) is code 2 (cosmetic solid, stone case); the
        // 1-in-25 decoration queue may have turned it into crusted stone or an
        // eldritch pedestal (GenCommon code 2 case), so both are the reference.
        helper.assertTrue(isFloor(outer.getBlockState(new BlockPos(x + 8, y + 1, z + 8))),
                "the key room's floor under the pedestal is missing (got "
                        + outer.getBlockState(new BlockPos(x + 8, y + 1, z + 8)).getBlock() + ")");

        // The cap pedestal: ref L95 places blockEldritch meta 3 at (x+8,y+2,z+8).
        final BlockPos pedestal = new BlockPos(x + 8, y + 2, z + 8);
        helper.assertTrue(outer.getBlockState(pedestal).is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())
                        && outer.getBlockState(pedestal).getValue(BlockEldritch.VARIANT)
                                == EldritchRealmValues.ELDRITCH_CAP,
                "the eldritch cap (meta 3) is not on the pedestal " + pedestal + " (got "
                        + outer.getBlockState(pedestal) + ")");
        // Ref L65-70: the room's core above the pedestal is void (code 8).
        helper.assertTrue(isVoid(outer.getBlockState(new BlockPos(x + 7, y + 5, z + 7))),
                "the key room's inner void (ref L65-70) is missing above the pedestal");

        // The Runed Tablet (ref L96-100): eldritch object meta 2, on top of the
        // cap, never despawning — the vanilla stand-in for EntityPermanentItem.
        // The harness level is shared across the parallel probe batches, so the
        // census filters to the item the room drops (an eldritch object) and
        // reports the rest, rather than trusting a raw item count.
        final List<ItemEntity> all = outer.getEntitiesOfClass(ItemEntity.class, cellBox(cx, cz));
        final List<ItemEntity> drops = all.stream()
                .filter(e -> e.getItem().getItem() instanceof ItemEldritchObject).toList();
        helper.assertTrue(drops.size() == 1,
                "the key room's cell holds " + drops.size() + " eldritch objects (" + all.size()
                        + " item entities in the window); ref L97 drops one tablet");
        if (drops.isEmpty()) {
            helper.fail("no tablet on the pedestal");
            return;
        }
        final ItemEntity tablet = drops.get(0);
        helper.assertTrue(ItemEldritchObject.storedMeta(tablet.getItem()) == EldritchRealmValues.OBJECT_TABLET,
                "the pedestal item is not the Runed Tablet (meta 2, ref L98)");
        helper.assertTrue(tablet.blockPosition().equals(pedestal.above()),
                "the tablet is not on top of the cap (ref L97: y + 3.5)");
        helper.assertTrue(tablet.getAge() < 0,
                "the tablet may despawn; the original's EntityPermanentItem never does (L23-25)");

        // The escort (ref L101-116): zz = 2 + (hard ? 2 : normal ? 1 : 0)
        // guardians, each at x + 8.5 + [1..3]*[-1..1] whole blocks, z likewise,
        // y + 2 exactly.
        final int escort = outer.getDifficulty() == Difficulty.HARD ? 4
                : outer.getDifficulty() == Difficulty.NORMAL ? 3 : 2;
        helper.assertTrue(guardians.size() == escort,
                "the key room summoned " + guardians.size() + " guardians; ref L101 counts " + escort
                        + " on " + outer.getDifficulty());
        for (final BlockPos g : guardians) {
            helper.assertTrue(g.getX() >= x + 5 && g.getX() <= x + 11 && g.getY() == y + 2
                            && g.getZ() >= z + 5 && g.getZ() <= z + 11,
                    "the guardian spot " + g + " is outside ref L107-108's 8.5 ± [1,3] box");
        }
        helper.assertTrue(GenCommon.DECORATED_BLOCKS.isEmpty(),
                "processDecorations left " + GenCommon.DECORATED_BLOCKS.size() + " decorations queued");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mazeCellBuildsNestRoom(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final int cx = 71;
        final int cz = 70;
        final int x = cx * 16;
        final int z = cz * 16;
        final int y = OuterLands.ROOM_Y;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell(NEST_ROOM_CELL));
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(13), cx, cz),
                "a feature-7 cell did not build");
        helper.assertTrue(MazeHandler.data(outer).isBuilt(cx, cz), "the built gate did not mark the nest-room cell");

        // The shell starts AT the floor here (ref L20-23: c runs 0..10), and the
        // floor is the eldritch void (code 8) with the crusted shell (code 22)
        // above it — the room's signature is the crust.
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 1, y, z + 1)).is(Blocks.BEDROCK),
                "the nest room's bedrock shell is missing at the floor corner");
        helper.assertTrue(isVoid(outer.getBlockState(new BlockPos(x + 8, y, z + 8))),
                "the nest room's void floor (code 8) is missing");
        helper.assertTrue(isCrust(outer.getBlockState(new BlockPos(x + 8, y + 1, z + 8))),
                "the nest room's crusted floor (code 22) is missing");
        // The spawner platform: ref L41-47 stacks the crust up the room's
        // centre column, and ref L48-56 the upper platform at y+8.
        for (int c = 2; c <= 4; c++) {
            helper.assertTrue(isCrust(outer.getBlockState(new BlockPos(x + 8, y + c, z + 8))),
                    "the nest room's spawner platform is missing at (x+8,y+" + c + ",z+8)");
        }
        helper.assertTrue(isCrust(outer.getBlockState(new BlockPos(x + 8, y + 8, z + 8))),
                "the nest room's upper platform is missing at (x+8,y+8,z+8)");
        helper.assertTrue(countInCell(outer, cx, cz, y, y + 14, OuterLandsGameTest::isCrust) >= 20,
                "the nest room's crusted shell is thin in its own cell");

        // The ceiling crystal (ref L61-65: blockCrystal meta 7, hung downward)
        // is a 1-in-N roll, so the probe counts the whole cell and asks for at
        // least one over the generator's own draw — with 25 rolls in the room a
        // zero is not reachable for the seed the probe pins.
        final int crystals = countInCell(outer, cx, cz, y, y + 14,
                state -> state.is(CatalogDecorativeBlocks.BLOCK_CRYSTAL.get()));
        helper.assertTrue(crystals >= 1, "the nest room placed no ceiling crystal (ref L61-65)");

        // The loot (ref L66-84): crates and urns on the floor ring. The count is
        // random-driven, so the probe asks for at least one landing in the cell
        // and pins the y the reference places them at.
        final int loot = countInCell(outer, cx, cz, y + 2, y + 2, state ->
                state.is(CatalogTrinketsLoot.BLOCK_LOOT_CRATE.get())
                        || state.is(CatalogTrinketsLoot.BLOCK_LOOT_URN.get()));
        helper.assertTrue(loot >= 1, "the nest room placed no crate/urn loot in its cell (ref L66-84)");

        // The crab spawners run through GenCommon's decoration queue (code 26);
        // the queue must be drained by the handler's processDecorations call.
        helper.assertTrue(GenCommon.DECORATED_BLOCKS.isEmpty(),
                "processDecorations left " + GenCommon.DECORATED_BLOCKS.size() + " decorations queued");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mazeCellBuildsLibraryRoom(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final int cx = 72;
        final int cz = 70;
        final int x = cx * 16;
        final int z = cz * 16;
        final int y = OuterLands.ROOM_Y;
        MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell(LIBRARY_ROOM_CELL));
        helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(17), cx, cz),
                "a feature-8 cell did not build");
        helper.assertTrue(MazeHandler.data(outer).isBuilt(cx, cz), "the built gate did not mark the library-room cell");

        // The library is the tall room: its shell runs c over 0..12 (ref L21-24),
        // twelve blocks up from the floor — two above the key room's nine.
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 1, y + 12, z + 1)).is(Blocks.BEDROCK),
                "the library's bedrock shell does not reach y+12 (ref L21-24)");
        helper.assertTrue(!outer.getBlockState(new BlockPos(x + 1, y + 13, z + 1)).is(Blocks.BEDROCK),
                "the library's shell is taller than ref L21-24's twelve");

        // The corner pedestals (ref L62-63: blockCosmeticSolid meta 15, the
        // ancient pedestal) carry the glyphed eldritch block (meta 5) under a
        // slab, a double slab, and the glyphed block again (ref L74-89).
        final BlockPos corner = new BlockPos(x + 5, y + 3, z + 5);
        helper.assertTrue(outer.getBlockState(corner).is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get())
                        && outer.getBlockState(corner).getValue(BlockCosmeticSolid.VARIANT)
                                == BlockCosmeticSolid.VARIANT_ANCIENT_PEDESTAL,
                "the library's corner pedestal (meta 15) is missing at " + corner + " (got "
                        + outer.getBlockState(corner) + ")");
        helper.assertTrue(isGlyphed(outer.getBlockState(corner.above())),
                "the glyphed eldritch block (meta 5) is missing over the corner pedestal");
        final BlockPos slab = new BlockPos(x + 5, y + 5, z + 5);
        helper.assertTrue(outer.getBlockState(slab).is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SLAB_STONE.get())
                        && outer.getBlockState(slab).getValue(BlockCosmeticStoneSlab.VARIANT)
                                == BlockCosmeticStoneSlab.VARIANT_ELDRITCH
                        && outer.getBlockState(slab).getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "the library's eldritch slab (blockSlabStone meta 1) is missing at " + slab + " (got "
                        + outer.getBlockState(slab) + ")");
        final BlockPos doubleSlab = new BlockPos(x + 5, y + 6, z + 5);
        helper.assertTrue(outer.getBlockState(doubleSlab).is(CatalogDecorativeBlocks.BLOCK_COSMETIC_DOUBLE_SLAB_STONE.get())
                        && outer.getBlockState(doubleSlab).getValue(SlabBlock.TYPE) == SlabType.DOUBLE,
                "the library's double slab (blockDoubleSlabStone meta 1) is missing at " + doubleSlab
                        + " (got " + outer.getBlockState(doubleSlab) + ")");
        helper.assertTrue(isGlyphed(outer.getBlockState(doubleSlab.above())),
                "the glyphed block is missing over the library's double slab");

        // The centre column (ref L96-99): pedestal, glyphed, slab, ... the
        // double slab at y+7, glyphed at y+8, pedestal at y+9.
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 8, y + 2, z + 8))
                        .is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get()),
                "the library's centre pedestal is missing");
        helper.assertTrue(isGlyphed(outer.getBlockState(new BlockPos(x + 8, y + 3, z + 8))),
                "the library's centre column has no glyphed block at y+3");
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 8, y + 7, z + 8))
                        .is(CatalogDecorativeBlocks.BLOCK_COSMETIC_DOUBLE_SLAB_STONE.get()),
                "the library's centre column has no double slab at y+7");
        helper.assertTrue(outer.getBlockState(new BlockPos(x + 8, y + 9, z + 8))
                        .is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get()),
                "the library's centre column has no pedestal at y+9");

        // The stair runs (ref L90-95): the room's five cases place purple
        // stairs along the walls; the probe pins a floor under the count rather
        // than the exact cells, which the generator's own draws choose.
        helper.assertTrue(countInCell(outer, cx, cz, y, y + 14,
                        state -> state.is(CatalogDecorativeBlocks.BLOCK_STAIRS_ELDRITCH.get())) >= 20,
                "the library's eldritch stair runs are missing from its cell");
        helper.assertTrue(GenCommon.DECORATED_BLOCKS.isEmpty(),
                "processDecorations left " + GenCommon.DECORATED_BLOCKS.size() + " decorations queued");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void passageFeaturesBuildPassages(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        // The original's GenMaze has no case for features 9..14: they fall
        // through to the private generatePassage wrapper (reference
        // MazeHandler.java:161-180), whose body is the default passage. Feature
        // 0 (the empty cell) takes the same leg. Each feature gets its own cell.
        final int[] features = {0, 9, 10, 11, 12, 13, 14};
        for (int i = 0; i < features.length; i++) {
            final int feature = features[i];
            final int cx = 73 + i;
            final int cz = 70;
            final int x = cx * 16;
            final int z = cz * 16;
            final int y = OuterLands.ROOM_Y;
            MazeHandler.putToHashMap(outer, new CellLoc(cx, cz), new Cell((short) (feature << 8)));
            helper.assertTrue(MazeHandler.generateEldritch(outer, new Random(23 + i), cx, cz),
                    "feature " + feature + " did not build its passage");
            helper.assertTrue(MazeHandler.data(outer).isBuilt(cx, cz),
                    "the built gate did not mark the feature-" + feature + " passage cell");
            // GenPassage builds its own shell: the room is carved out of
            // cosmetic solid (code 2) with bedrock at the corners (code 1).
            helper.assertTrue(countInCell(outer, cx, cz, y, y + 10, state ->
                            state.is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get())
                                    || state.is(Blocks.BEDROCK)) >= 40,
                    "feature " + feature + " left no passage shell in its cell");

            if (feature == 11) {
                // Ref L61, L103, L145, L187 (the four stone-site legs): feature
                // 11 swaps one in three of the passage's stone sites for the
                // trapped stone — code 20 (STONE_TRAPPED) -> blockEldritch meta
                // 10 (GenCommon's STONE_TRAPPED case, ref L223-228). The roll is
                // the pinned maze random (feature == 11 && random.nextInt(3) == 0
                // ? 20 : 2), so the count this probe pins is deterministic.
                helper.assertTrue(countInCell(outer, cx, cz, y, y + 10, OuterLandsGameTest::isTrap) >= 1,
                        "feature 11 (the trapped passage, code 20) left no trapped stone (meta 10)");
            }
            if (feature == 12) {
                // Ref L246-250: feature 12 crusts the room (code 22).
                helper.assertTrue(countInCell(outer, cx, cz, y, y + 10, OuterLandsGameTest::isCrust) >= 1,
                        "feature 12 (the crusted passage) left no crusted stone");
            }
            if (feature == 13) {
                // Ref L251-255: feature 13 writes the taint biome into the
                // column through the shipped BiomeAura seam. No taint biome is
                // registered on this card, so the write is a no-op (the seam's
                // own contract) and the passage is the assertion.
                helper.assertTrue(com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura.firstTaintBiome() == null,
                        "feature 13's taint-biome leg needs its assertion re-measured: a taint biome is now registered");
            }
            if (feature == 14) {
                // Ref L260-276: the spawner box — the mob spawner at the centre
                // (x+8, y+4, z+8) and cobweb in the air cells around it.
                final BlockPos centre = new BlockPos(x + 8, y + 4, z + 8);
                helper.assertTrue(outer.getBlockState(centre).is(Blocks.SPAWNER),
                        "feature 14 placed no mob spawner at " + centre + " (ref L268)");
                helper.assertTrue(countInCell(outer, cx, cz, y, y + 10, state -> state.is(Blocks.COBWEB)) >= 1,
                        "feature 14 placed no cobweb in its spawner box (ref L265)");
            }
        }
        helper.assertTrue(GenCommon.DECORATED_BLOCKS.isEmpty(),
                "processDecorations left " + GenCommon.DECORATED_BLOCKS.size() + " decorations queued");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void altarTileRunsTheMazeRequestLeg(final GameTestHelper helper) {
        final ServerLevel outer = build(helper);
        final BlockPos altarPos = new BlockPos(1400, OuterLands.ROOM_Y, 1400);
        outer.setBlock(altarPos, CatalogEldritchRealm.BLOCK_ELDRITCH.get().defaultBlockState()
                .setValue(BlockEldritch.VARIANT, EldritchRealmValues.ELDRITCH_ALTAR), 3);
        final TileEldritchAltar altar = (TileEldritchAltar) outer.getBlockEntity(altarPos);
        if (altar == null) {
            helper.fail("no eldritch altar tile at " + altarPos);
            return;
        }

        // The probe claims the maze seam for the duration and records the
        // request this tile makes, so the shape it draws is measured and not a
        // sibling probe's; the shipped install goes back at the end.
        final List<long[]> seen = new ArrayList<>();
        EldritchMazeHost.setProbe((chunkX, chunkZ, width, height, seed) -> {
            seen.add(new long[]{chunkX, chunkZ, width, height, seed});
            return false;
        });
        final boolean answer;
        final boolean second;
        try {
            answer = altar.checkForMaze();
            second = altar.checkForMaze();
        } finally {
            MazeHandler.install();
        }
        helper.assertTrue(seen.size() >= 2,
                "the altar's checkForMaze reached the maze seam " + seen.size() + " times in two calls");
        final long[] first = seen.get(seen.size() - 2);
        final long[] next = seen.get(seen.size() - 1);
        for (final long[] request : new long[][]{first, next}) {
            // Ref TileEldritchAltar.java:227-231: the request is the altar's own
            // chunk, and the size is 15 + random.nextInt(8) * 2 per side — odd,
            // 15..29 — drawn from the level random, per call.
            helper.assertTrue(request[0] == (altarPos.getX() >> 4) && request[1] == (altarPos.getZ() >> 4),
                    "the altar's maze request carried chunk (" + request[0] + "," + request[1]
                            + "), not the altar's own chunk");
            helper.assertTrue(request[2] >= 15 && request[2] <= 29 && (request[2] & 1L) == 1L,
                    "the maze width " + request[2] + " is outside ref L228's 15 + nextInt(8)*2");
            helper.assertTrue(request[3] >= 15 && request[3] <= 29 && (request[3] & 1L) == 1L,
                    "the maze height " + request[3] + " is outside ref L228's 15 + nextInt(8)*2");
            // Ref L231: the seed is the level's own random draw, not a constant.
            helper.assertTrue(request[4] != 0L, "the maze request carried no seed (ref L231)");
        }
        helper.assertTrue(first[4] != next[4] || first[2] != next[2] || first[3] != next[3],
                "two checkForMaze calls drew the identical request; the original draws per call");
        // The plane gate: the harness has no live Outer Lands level, so the
        // shipped install answers "no maze" (reference TileEldritchAltar.java:
        // 224-236 keeps the isEldritch gate; the parent's probe pins the gate,
        // this one pins that the tile's caller leg reaches the seam at all).
        helper.assertTrue(!answer && !second,
                "the maze request answered yes with no live Outer Lands level in the harness");
        helper.succeed();
    }

    /** The original's eldritch void (blockEldritchnothing, maze code 8). */
    private static boolean isVoid(final BlockState state) {
        return state.is(CatalogEldritchRealm.BLOCK_ELDRITCH_NOTHING.get());
    }

    /** The trapped stone (blockEldritch meta 10, maze code 20 / STONE_TRAPPED). */
    private static boolean isTrap(final BlockState state) {
        return state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())
                && state.getValue(BlockEldritch.VARIANT) == EldritchRealmValues.ELDRITCH_TRAP;
    }

    /** The glyphed eldritch block (blockEldritch meta 5). */
    private static boolean isGlyphed(final BlockState state) {
        return state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())
                && state.getValue(BlockEldritch.VARIANT) == EldritchRealmValues.ELDRITCH_GLYPHED;
    }

    /** The crusted shell (blockCosmeticSolid meta 14) and its code-22 eldritch twin. */
    private static boolean isCrust(final BlockState state) {
        if (state.is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get())
                && state.getValue(BlockCosmeticSolid.VARIANT) == BlockCosmeticSolid.VARIANT_CRUSTED_STONE) {
            return true;
        }
        return state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())
                && state.getValue(BlockEldritch.VARIANT) == EldritchRealmValues.ELDRITCH_CRUSTED;
    }

    /** A maze floor: the plain cosmetic solid (code 2) or a decoration the queue put there. */
    private static boolean isFloor(final BlockState state) {
        return state.is(CatalogDecorativeBlocks.BLOCK_COSMETIC_SOLID.get())
                || state.is(CatalogEldritchRealm.BLOCK_ELDRITCH.get())
                || state.is(Blocks.BEDROCK);
    }

    /** The probe's own cell window — the harness shares one level across probes. */
    private static AABB cellBox(final int cx, final int cz) {
        return new AABB(cx * 16, OuterLands.ROOM_Y - 8, cz * 16, cx * 16 + 16, OuterLands.ROOM_Y + 24, cz * 16 + 16);
    }

    private static int countInCell(final ServerLevel level, final int cx, final int cz, final int yFrom,
                                   final int yTo, final Predicate<BlockState> test) {
        int n = 0;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = cx * 16; x < cx * 16 + 16; x++) {
            for (int y = yFrom; y <= yTo; y++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    if (test.test(level.getBlockState(at.set(x, y, z)))) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

}
