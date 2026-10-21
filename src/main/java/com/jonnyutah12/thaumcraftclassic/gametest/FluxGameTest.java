package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.world.FluxEvents;
import com.jonnyutah12.thaumcraftclassic.world.FluxModel;
import com.jonnyutah12.thaumcraftclassic.world.FluxTickHandler;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TC-M5-03 acceptance: flux generation from repeated magic use raises the
 * area's flux, the age pass decays it over time (with the original-derived
 * per-column rate and neighbour diffusion), and the values survive the real
 * save/reload path. Design record: docs/architecture/flux.md.
 *
 * <p>Each test keys off a distinct chunk position derived from the run's
 * randomized origin (the gametest server reuses run/world), and asserts only
 * about its own chunks.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluxGameTest {

    private FluxGameTest() {
    }

    @GameTest(template = "empty")
    public static void repeatedCastingRaisesFluxInArea(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos pos = chunkAt(helper, 16);
        final ChunkPos far = chunkAt(helper, 64);
        final BlockPos inside = new BlockPos(pos.x * 16 + 8, 64, pos.z * 16 + 8);

        // Own the fixture: zero the chunk (and the far control) so absolute
        // assertions hold regardless of what earlier runs left behind.
        WorldMagicData.get(level).setFlux(pos, 0, 0);
        WorldMagicData.get(level).setFlux(far, 0, 0);

        // Ten casts of a 5-vis spell: SPELL_CAST adds 1 air quantum per vis
        // unit (port design constant pinned in FluxModel + unit tests).
        for (int cast = 0; cast < 10; cast++) {
            FluxEvents.addFlux(level, inside, FluxModel.FluxSource.SPELL_CAST, 5);
        }

        final int air = FluxEvents.fluxAir(level, inside);
        helper.assertTrue(air == 50, "expected 50 air quanta after 10x5-vis casting, got " + air);
        helper.assertTrue(FluxEvents.levelAt(level, inside) == FluxModel.MAX_LEVEL,
                "representative level must cap at the original's meta-8 scale");
        helper.assertTrue(FluxEvents.fluxAir(level, new BlockPos(far.x * 16, 64, far.z * 16)) == 0,
                "casting must only pollute the containing chunk");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluxDecaysOverTime(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos pos = chunkAt(helper, 32);
        final BlockPos inside = new BlockPos(pos.x * 16 + 8, 64, pos.z * 16 + 8);

        final WorldMagicData data = WorldMagicData.get(level);
        // The age pass only ages loaded chunks (the original's random ticks
        // never touched unloaded ones) — load the fixture and its neighbours.
        for (final ChunkPos n : java.util.List.of(
                pos,
                new ChunkPos(pos.x + 1, pos.z), new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x, pos.z + 1), new ChunkPos(pos.x, pos.z - 1))) {
            level.getChunk(n.x, n.z);
            data.setFlux(n, 0, 0);
        }

        // Two infusion crashes: 7 ground quanta each (meta-7 goo columns,
        // TileInfusionMatrix.java:639-642).
        FluxEvents.addFlux(level, inside, FluxModel.FluxSource.INFUSION_FAILURE_GROUND, 1);
        FluxEvents.addFlux(level, inside, FluxModel.FluxSource.INFUSION_FAILURE_GROUND, 1);
        helper.assertTrue(FluxEvents.fluxGround(level, inside) == 14, "generation did not store 14 ground quanta");

        // First pass is fully deterministic except the 50% gas vent: 14 quanta
        // = 2 columns -> decay -2, and above one full column the chunk passes
        // 1 quantum to each of its four cardinal neighbours (-4).
        FluxTickHandler.runFluxPass(level);
        final int afterFirst = WorldMagicData.get(level).getFluxGround(pos);
        helper.assertTrue(afterFirst == 8, "first age pass expected 14-2-4=8, got " + afterFirst);
        final int neighbour = WorldMagicData.get(level).getFluxGround(new ChunkPos(pos.x + 1, pos.z));
        helper.assertTrue(neighbour == 1, "diffusion did not deliver one quantum to the east neighbour, got " + neighbour);
        final int vented = WorldMagicData.get(level).getFluxAir(pos);
        helper.assertTrue(vented >= 0 && vented <= 2, "gas vent out of range [0, columns=2]: " + vented);

        int previous = afterFirst;
        int passes = 1;
        while (WorldMagicData.get(level).getFluxGround(pos) > 0 && passes < 64) {
            FluxTickHandler.runFluxPass(level);
            final int ground = WorldMagicData.get(level).getFluxGround(pos);
            helper.assertTrue(ground < previous,
                    "age pass must reduce ground flux every pass (" + previous + " -> " + ground + ")");
            previous = ground;
            passes++;
        }
        helper.assertTrue(previous == 0, "ground flux did not decay to zero within " + passes + " passes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluxPersistsAcrossSaveReload(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos pos = chunkAt(helper, 48);
        final BlockPos inside = new BlockPos(pos.x * 16 + 8, 64, pos.z * 16 + 8);

        final WorldMagicData data = WorldMagicData.get(level);
        data.setFlux(pos, 0, 0);

        FluxEvents.addFlux(level, inside, FluxModel.FluxSource.SPELL_CAST, 7);
        FluxEvents.addFlux(level, inside, FluxModel.FluxSource.TAINT_DECAY, 1);
        helper.assertTrue(data.getFluxAir(pos) == 7 && data.getFluxGround(pos) == 8,
                "generation values not stored before save");

        // Same synchronous temp-dir round trip as PersistenceGameTest:
        // run/world is shared and must not be the fixture.
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tc-flux");
            final CompoundTag root = new CompoundTag();
            root.put("data", data.save(new CompoundTag(), level.registryAccess()));
            NbtUtils.addCurrentDataVersion(root);
            NbtIo.writeCompressed(root, tempDir.resolve(WorldMagicData.NAME + ".dat"));

            final MinecraftServer server = level.getServer();
            final DimensionDataStorage reloaded = new DimensionDataStorage(
                    tempDir.toFile(), server.getFixerUpper(), server.registryAccess());
            final WorldMagicData r = reloaded.computeIfAbsent(WorldMagicData.FACTORY, WorldMagicData.NAME);
            if (r == null) {
                helper.fail("world magic data did not reload from disk");
                return;
            }
            helper.assertTrue(r.getFluxAir(pos) == 7 && r.getFluxGround(pos) == 8,
                    "flux values not persisted across save/reload");
            helper.succeed();
        } catch (IOException e) {
            helper.fail("could not create/write temp data dir: " + e);
        } finally {
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir.resolve(WorldMagicData.NAME + ".dat"));
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    private static ChunkPos chunkAt(final GameTestHelper helper, final int chunkOffset) {
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new ChunkPos((origin.getX() >> 4) + chunkOffset, (origin.getZ() >> 4) + chunkOffset);
    }
}
