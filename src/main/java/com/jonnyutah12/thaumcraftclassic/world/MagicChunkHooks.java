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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chunk-level magic hooks: the TC-M2-02 substrate piece that replaces Thaumcraft 4's
 * {@code EventHandlerWorld.chunkSave/chunkLoad} pair (design record:
 * docs/architecture/persistence.md).
 *
 * <p>TC4 wrote a {@code Thaumcraft}/{@code regenKey} marker into raw chunk NBT and, on
 * load, queued any chunk without the marker for magic world regeneration. NeoForge 21.1
 * removed the raw chunk-data events, so the marker lives in {@link WorldMagicData}
 * keyed by chunk coordinates instead, and {@link ChunkEvent.Load} performs the same
 * "never generated? queue it" check. Generation itself is deferred to a tick context
 * (TC4's {@code ServerTickEventsFML.chunksToGenerate} parity): milestones register
 * {@link MagicChunkGenHook}s and call {@link #runPendingChunkGen(ServerLevel)}.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class MagicChunkHooks {

    /** Runs magic world generation for one chunk (node placement etc., added by M5). */
    @FunctionalInterface
    public interface MagicChunkGenHook {
        void generate(ServerLevel level, ChunkPos pos, WorldMagicData data);
    }

    private static final List<MagicChunkGenHook> HOOKS = new CopyOnWriteArrayList<>();
    private static final Map<ResourceKey<Level>, Deque<ChunkPos>> PENDING = new ConcurrentHashMap<>();

    private MagicChunkHooks() {
    }

    public static void registerChunkGenHook(final MagicChunkGenHook hook) {
        HOOKS.add(hook);
    }

    /** Read-only view of the pending queue for a level (never null). */
    public static Queue<ChunkPos> pendingChunks(final ResourceKey<Level> dimension) {
        return PENDING.computeIfAbsent(dimension, d -> new ArrayDeque<>());
    }

    public static void clearPending(final ResourceKey<Level> dimension) {
        PENDING.remove(dimension);
    }

    /**
     * Harness gate for the deferred block-writing pass (t_764d1e8e, docs/testing.md#deferred-chunkgen-gated-in-gametest):
     * {@code tc.noChunkGen} set as a system property makes {@link StructureChunkGen#generateChunk}
     * skip the chunk. The game-test server places its pads in chunks whose coordinates sit in the
     * millions, and that pass is what stamps those pad cells — eldritch rings write
     * {@code thaumcraft:blockeldritch} and carve air, the mound/hilltop/totem pass writes
     * {@code dirt}/{@code grass_block}/{@code cobblestone} — and a {@code setBlock} over a standing
     * cell leaves the cell's block entity in place, so the stamp lands under the probe's fixture
     * (docs/testing.md#scan-claim-covers-box-cells, docs/testing.md#orb-claim-covers-asserted-cells).
     * A probe that asserts its own geometry therefore draws this pass on every leg. The game-test
     * server has no use for millions-coordinate structures, so the harness gates the pass; the
     * property follows the {@code tc.*} precedent ({@code tc.noCurios}, {@code tc.contentMappingFile})
     * and {@code -Ptc.chunkGenOn} re-arms it for a measurement leg.
     *
     * <p>The gate sits on the STRUCTURE hook, not on the queue: measured on the first shape, a queue
     * clear in {@link #runPendingChunkGen} dropped every pending chunk and reddened
     * {@code loadedchunksflowthroughthetickdriver}, the lane that measures the driver draining the
     * queue ({@code data.isMagicGenDone(chunk)}). The aura pass writes no blocks — it records vis
     * density — so it stays live and the driver keeps its marking.</p>
     */
    public static final String NO_CHUNK_GEN_PROPERTY = "tc.noChunkGen";

    /** True while the harness gate is armed for this JVM (read per call, so a leg can flip it). */
    public static boolean deferredChunkGenGated() {
        return Boolean.getBoolean(NO_CHUNK_GEN_PROPERTY);
    }

    /**
     * Drains the pending queue for this level, runs every registered hook, and marks
     * each chunk generation-complete in the world store. Call from a server-tick
     * context, never from a chunk-load callback (the chunk may not be FULL yet).
     */
    public static void runPendingChunkGen(final ServerLevel level) {
        final Deque<ChunkPos> queue = PENDING.get(level.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        final WorldMagicData data = WorldMagicData.get(level);
        ChunkPos pos;
        while ((pos = queue.poll()) != null) {
            if (data.isMagicGenDone(pos)) {
                continue;
            }
            for (final MagicChunkGenHook hook : HOOKS) {
                hook.generate(level, pos, data);
            }
            data.setMagicGenDone(pos, true);
        }
    }

    // TC4 parity: EventHandlerWorld.chunkLoad — a chunk without the magic marker is
    // queued so magic world gen can catch up on worlds created before the mod existed.
    @SubscribeEvent
    static void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        final ChunkPos pos = event.getChunk().getPos();
        final WorldMagicData data = WorldMagicData.get(serverLevel);
        final WorldMagicData.ChunkMagic record = data.getChunk(pos);
        if (record == null || !record.isMagicGenDone()) {
            pendingChunks(serverLevel.dimension()).add(pos);
        }
    }

    // TC4 parity: EventHandlerWorld.worldUnload drops per-world state; the pending
    // queue is transient and must not outlive its level.
    @SubscribeEvent
    static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            clearPending(serverLevel.dimension());
        }
    }
}
