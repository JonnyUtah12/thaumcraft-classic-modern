package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flux aging: the periodic decay + diffusion pass over chunks that carry flux
 * (TC-M5-03; design record: docs/architecture/flux.md).
 *
 * <p>Every {@link FluxModel#DECAY_PASS_TICKS} game ticks (the expected
 * per-column decay interval derived from the original's 1/30-per-random-tick
 * goo decay under 1.7.10's random-tick rate), each tracked chunk that is
 * currently loaded loses one level per flux column, ground decay can emit air
 * flux (the original's 50% gas vent), and chunks holding more than one full
 * column pass one quantum to each cardinal neighbour (chunk-scale analogue of
 * the original's fluid spreading).</p>
 *
 * <p>Chunks enter the tracked set when flux is added ({@link FluxEvents}) or
 * when a chunk carrying persisted flux loads (rebuild path after restart,
 * mirroring {@link MagicChunkHooks}' chunk-load pattern); they leave it when
 * their flux reaches zero. Unloaded chunks stay tracked but are skipped —
 * the original's random ticks only ever touched loaded chunks.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class FluxTickHandler {

    private static final Map<ResourceKey<Level>, Set<ChunkPos>> TRACKED = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Long> PASS_TICKS = new ConcurrentHashMap<>();

    private FluxTickHandler() {
    }

    /** Mark a chunk as flux-bearing so the age pass covers it. */
    static void track(final ServerLevel level, final ChunkPos pos) {
        TRACKED.computeIfAbsent(level.dimension(), d -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    /** Read-only view of the tracked set for a level (never null). */
    public static Set<ChunkPos> trackedChunks(final ResourceKey<Level> dimension) {
        return TRACKED.getOrDefault(dimension, Set.of());
    }

    /** Drop all per-level state (level unload; TC4 worldUnload parity). */
    public static void clear(final ResourceKey<Level> dimension) {
        TRACKED.remove(dimension);
        PASS_TICKS.remove(dimension);
    }

    @SubscribeEvent
    static void onServerTickPost(final ServerTickEvent.Post event) {
        for (final ServerLevel level : event.getServer().getAllLevels()) {
            final Set<ChunkPos> tracked = TRACKED.get(level.dimension());
            if (tracked == null || tracked.isEmpty()) {
                PASS_TICKS.put(level.dimension(), 0L);
                continue;
            }
            final long ticks = PASS_TICKS.merge(level.dimension(), 1L, Long::sum);
            if (ticks >= FluxModel.DECAY_PASS_TICKS) {
                PASS_TICKS.put(level.dimension(), 0L);
                runFluxPass(level);
            }
        }
    }

    /**
     * One age pass: decay + gas vent + diffusion, computed from a snapshot so
     * order cannot cascade within the pass. Public for the game test.
     */
    public static void runFluxPass(final ServerLevel level) {
        final Set<ChunkPos> tracked = TRACKED.get(level.dimension());
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        final WorldMagicData data = WorldMagicData.get(level);
        final FluxModel.Rand rand = FluxEvents.mcRand(level.getRandom());
        final List<ChunkPos> snapshot = new ArrayList<>(tracked);
        final Map<ChunkPos, int[]> pending = new ConcurrentHashMap<>(); // [airDelta, groundDelta]

        for (final ChunkPos pos : snapshot) {
            if (!level.hasChunk(pos.x, pos.z)) {
                continue; // loaded-only, like the original's random ticks
            }
            final int air = data.getFluxAir(pos);
            final int ground = data.getFluxGround(pos);
            if (air == 0 && ground == 0) {
                tracked.remove(pos);
                continue;
            }

            final int groundColumns = FluxModel.columns(ground);
            final int newGround = FluxModel.decayStep(ground);
            final int ventedAir = FluxModel.groundDecayAirEmission(groundColumns, rand);
            final int newAir = FluxModel.decayStep(air) + ventedAir;

            final int outAir = FluxModel.diffusionOut(air);
            final int outGround = FluxModel.diffusionOut(ground);

            accumulate(pending, pos, newAir - air, newGround - ground);
            for (final ChunkPos neighbour : cardinalNeighbours(pos)) {
                accumulate(pending, neighbour, outAir, outGround);
                accumulate(pending, pos, -outAir, -outGround);
            }
        }

        for (final Map.Entry<ChunkPos, int[]> entry : pending.entrySet()) {
            final ChunkPos pos = entry.getKey();
            final int[] delta = entry.getValue();
            final int air = Math.max(0, data.getFluxAir(pos) + delta[0]);
            final int ground = Math.max(0, data.getFluxGround(pos) + delta[1]);
            if (air == 0 && ground == 0) {
                data.setFlux(pos, 0, 0);
                tracked.remove(pos);
            } else {
                data.setFlux(pos, air, ground);
                track(level, pos);
            }
        }
    }

    private static List<ChunkPos> cardinalNeighbours(final ChunkPos pos) {
        return List.of(
                new ChunkPos(pos.x + 1, pos.z),
                new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x, pos.z + 1),
                new ChunkPos(pos.x, pos.z - 1));
    }

    private static void accumulate(final Map<ChunkPos, int[]> pending, final ChunkPos pos,
            final int airDelta, final int groundDelta) {
        final int[] delta = pending.computeIfAbsent(pos, p -> new int[2]);
        delta[0] += airDelta;
        delta[1] += groundDelta;
    }

    // Rebuild the tracked set after restarts: a chunk whose persisted record
    // carries flux re-enters aging when it loads (the store itself is only
    // reachable per-chunk; scanning every record is not possible without
    // touching the WorldMagicData hot file, and flux only matters near
    // players — i.e. near loaded chunks).
    @SubscribeEvent
    static void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        final ChunkPos pos = event.getChunk().getPos();
        final WorldMagicData data = WorldMagicData.get(serverLevel);
        if (data.getFluxAir(pos) != 0 || data.getFluxGround(pos) != 0) {
            track(serverLevel, pos);
        }
    }

    @SubscribeEvent
    static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            clear(serverLevel.dimension());
        }
    }
}
