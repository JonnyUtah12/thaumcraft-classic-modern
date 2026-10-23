package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.nodes.NodeRuntime;
import com.jonnyutah12.thaumcraftclassic.worldgen.BiomeAura;
import com.jonnyutah12.thaumcraftclassic.worldgen.NodeGeneration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The node behaviour driver (TC-M5-02; design record:
 * docs/architecture/node-behavior.md). Sibling of {@link FluxTickHandler}:
 * every game tick, every node record in a loaded tracked chunk gets a
 * {@link NodeRuntime} ticked in the original {@code TileNode.updateEntity}
 * order, and changed state is written back through
 * {@link WorldMagicData#replaceNode}.
 *
 * <p>Unlike flux (which tracks only flux-bearing chunks), every loaded chunk
 * is tracked: the original node ticked whenever its chunk was loaded, and node
 * records can appear in any chunk at any time (generation, future device
 * placement), with no event on the record store to hang off. The per-tick cost
 * is one map lookup per loaded chunk.</p>
 *
 * <p>Transient runtime state (tick counters, lock, regen period) is held only
 * while the chunk is loaded — unloading a chunk drops its runtimes, exactly as
 * the original dropped tile entities, and they rebuild from the record on the
 * next load (including the offline catch-up check).</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class NodeTickDriver {

    /**
     * Stabilizer probe targets (TC-M5-02): the original read
     * {@code blockStoneDevice} meta 9/10 under the node (TileNode L949-957).
     * The M6 device blocks opt in by joining these tags; absent tags read as
     * "no stabilizer".
     */
    public static final TagKey<Block> STABILIZER_BASIC = blockTag("node_stabilizer_basic");
    public static final TagKey<Block> STABILIZER_ADVANCED = blockTag("node_stabilizer_advanced");

    /** Magical-log probe for the pure-node conversion (TileNode L846). */
    public static final TagKey<Block> MAGICAL_LOG = blockTag("magical_log");

    private static final Map<ResourceKey<Level>, Map<BlockPos, NodeRuntime>> RUNTIMES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> TRACKED = new ConcurrentHashMap<>();

    // Biome seams for node painting. Taint reuses BiomeAura's registry (M5-01);
    // eerie/magical-forest are node-behaviour consumers the biome cards will
    // register when they land (TC-M5-04+).
    private static final Set<ResourceKey<Biome>> EERIE_BIOMES = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceKey<Biome>> MAGICAL_FOREST_BIOMES = ConcurrentHashMap.newKeySet();

    private NodeTickDriver() {
    }

    private static TagKey<Block> blockTag(final String name) {
        return TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, name));
    }

    public static void registerEerieBiome(final ResourceKey<Biome> biome) {
        EERIE_BIOMES.add(biome);
    }

    public static void registerMagicalForestBiome(final ResourceKey<Biome> biome) {
        MAGICAL_FOREST_BIOMES.add(biome);
    }

    /** The live runtime for a position, or null (test/consumer hook). */
    public static NodeRuntime runtime(final ServerLevel level, final BlockPos pos) {
        final Map<BlockPos, NodeRuntime> nodes = RUNTIMES.get(level.dimension());
        return nodes == null ? null : nodes.get(pos);
    }

    /** Drop all per-level state (level unload; TC4 worldUnload parity). */
    public static void clear(final ResourceKey<Level> dimension) {
        TRACKED.remove(dimension);
        RUNTIMES.remove(dimension);
    }

    @SubscribeEvent
    static void onServerTickPost(final ServerTickEvent.Post event) {
        for (final ServerLevel level : event.getServer().getAllLevels()) {
            runNodePass(level);
        }
    }

    /**
     * One behaviour tick for every node in loaded tracked chunks. Public for
     * the game test (same contract as {@link FluxTickHandler#runFluxPass}).
     */
    public static void runNodePass(final ServerLevel level) {
        final Set<ChunkPos> tracked = TRACKED.get(level.dimension());
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        final WorldMagicData data = WorldMagicData.get(level);
        final Map<BlockPos, NodeRuntime> nodes =
                RUNTIMES.computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>());
        final WorldContext context = new WorldContext(level, nodes);
        // The original ticked against the level's shared java.util.Random
        // (world.rand, TileNode updateEntity); 1.21 exposes a RandomSource, so
        // bridge the two methods the engine consumes.
        final Random random = new LevelRandomBridge(level.getRandom());

        // Reconcile runtimes against records in loaded chunks.
        final Set<BlockPos> live = new HashSet<>();
        for (final ChunkPos chunk : new ArrayList<>(tracked)) {
            if (!level.hasChunk(chunk.x, chunk.z)) {
                continue; // loaded-only, like the original's tile entities
            }
            for (final WorldMagicData.Node record : data.getNodes(chunk)) {
                final BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
                live.add(pos);
                nodes.computeIfAbsent(pos, p -> NodeRuntime.fromRecord(record, Aspect::getAspect,
                        System::currentTimeMillis));
            }
        }
        nodes.keySet().retainAll(live);

        for (final NodeRuntime node : new ArrayList<>(nodes.values())) {
            // ChunkPos(int, int) takes CHUNK coordinates — shift the block
            // position first (the (BlockPos) ctor would do this implicitly).
            final ChunkPos chunk = new ChunkPos(node.x() >> 4, node.z() >> 4);
            node.tick(context, random, System::currentTimeMillis);
            if (node.isDead()) {
                // Original removed the tile and its block (TileNode L749-758).
                data.removeNode(chunk, node.x(), node.y(), node.z());
                nodes.remove(new BlockPos(node.x(), node.y(), node.z()));
            } else if (node.isDirty()) {
                data.replaceNode(chunk, node.x(), node.y(), node.z(), node.toRecord());
                node.clearDirty();
            }
        }
    }

    @SubscribeEvent
    static void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        TRACKED.computeIfAbsent(serverLevel.dimension(), d -> ConcurrentHashMap.newKeySet())
                .add(event.getChunk().getPos());
    }

    @SubscribeEvent
    static void onChunkUnload(final ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        final ChunkPos pos = event.getChunk().getPos();
        final Set<ChunkPos> tracked = TRACKED.get(serverLevel.dimension());
        if (tracked != null) {
            tracked.remove(pos);
        }
        // Transient state resets with the chunk, as the original's tile entities did.
        final Map<BlockPos, NodeRuntime> nodes = RUNTIMES.get(serverLevel.dimension());
        if (nodes != null) {
            nodes.keySet().removeIf(p -> new ChunkPos(p).equals(pos));
        }
    }

    @SubscribeEvent
    static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            clear(serverLevel.dimension());
        }
    }

    /** The world-backed {@link NodeRuntime.Context}; all seams resolve here. */
    private static final class WorldContext implements NodeRuntime.Context {

        private final ServerLevel level;
        private final Map<BlockPos, NodeRuntime> nodes;

        WorldContext(final ServerLevel level, final Map<BlockPos, NodeRuntime> nodes) {
            this.level = level;
            this.nodes = nodes;
        }

        @Override
        public NodeRuntime neighborAt(final int x, final int y, final int z) {
            return nodes.get(new BlockPos(x, y, z));
        }

        @Override
        public int stabilizerAt(final int x, final int y, final int z) {
            final BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.is(STABILIZER_ADVANCED)) {
                return 2;
            }
            if (state.is(STABILIZER_BASIC)) {
                return 1;
            }
            return 0;
        }

        @Override
        public boolean isTaintBiomeAt(final int x, final int z) {
            return BiomeAura.isTaintBiome(biomeAt(x, z));
        }

        @Override
        public boolean hasTaintBiome() {
            return BiomeAura.hasTaintBiome();
        }

        @Override
        public void paintTaintBiome(final int x, final int y, final int z) {
            setBiomeAt(x, y, z, BiomeAura.firstTaintBiome());
        }

        @Override
        public boolean isEerieBiomeAt(final int x, final int z) {
            final Holder<Biome> biome = biomeAt(x, z);
            return EERIE_BIOMES.contains(biome.unwrapKey().orElse(null));
        }

        @Override
        public boolean hasEerieBiome() {
            return !EERIE_BIOMES.isEmpty();
        }

        @Override
        public void paintEerieBiome(final int x, final int y, final int z) {
            setBiomeAt(x, y, z, first(EERIE_BIOMES));
        }

        @Override
        public boolean isMagicalForestBiomeAt(final int x, int z) {
            final Holder<Biome> biome = biomeAt(x, z);
            return MAGICAL_FOREST_BIOMES.contains(biome.unwrapKey().orElse(null));
        }

        @Override
        public boolean hasMagicalForestBiome() {
            return !MAGICAL_FOREST_BIOMES.isEmpty();
        }

        @Override
        public void paintMagicalForestBiome(final int x, final int y, final int z) {
            setBiomeAt(x, y, z, first(MAGICAL_FOREST_BIOMES));
        }

        @Override
        public boolean biomePaintingAllowed() {
            // Original gate: not The End (-1), not the Nether (1), dimension not
            // blacklisted (TileNode L831-835, L856-861). The biome-blacklist
            // config has no port-side registry yet, so it reads as allowed.
            return !level.dimension().equals(Level.NETHER)
                    && !level.dimension().equals(Level.END)
                    && !NodeGeneration.isDimBlacklisted(level.dimension());
        }

        @Override
        public boolean isMagicalLogAt(final int x, final int y, final int z) {
            return level.getBlockState(new BlockPos(x, y, z)).is(MAGICAL_LOG);
        }

        @Override
        public int surfaceHeight(final int x, final int z) {
            // Original world.getHeight(x,z) = top non-air block y; the modern
            // heightmap value is one above the top block.
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        }

        @Override
        public boolean isAir(final int x, final int y, final int z) {
            return level.isEmptyBlock(new BlockPos(x, y, z));
        }

        @Override
        public float blockHardness(final int x, final int y, final int z) {
            final BlockPos pos = new BlockPos(x, y, z);
            return level.getBlockState(pos).getDestroySpeed(level, pos);
        }

        @Override
        public int[] rayTraceHit(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
            // Original ThaumcraftApiHelper.rayTraceIgnoringSource: solids only,
            // no fluids, source block never collides (the port has no node block).
            final BlockHitResult hit = level.clip(new ClipContext(
                    new Vec3(x0 + 0.5, y0 + 0.5, z0 + 0.5),
                    new Vec3(x1 + 0.5, y1 + 0.5, z1 + 0.5),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    (Entity) null));
            if (hit == null || hit.getType() != BlockHitResult.Type.BLOCK) {
                return null;
            }
            final BlockPos pos = hit.getBlockPos();
            return new int[] {pos.getX(), pos.getY(), pos.getZ()};
        }

        @Override
        public void breakBlock(final int x, final int y, final int z) {
            level.destroyBlock(new BlockPos(x, y, z), true);
        }

        // Aspect orbs are entities; the unstable-node spawn is kept as a hook
        // until the M6 entity cards provide EntityAspectOrb (parity note in
        // NodeRuntime). The primal is still consumed from the node, as in the
        // original, which spawned the orb with it.

        private Holder<Biome> biomeAt(final int x, final int z) {
            return level.getBiome(new BlockPos(x, level.getMinBuildHeight() + 1, z));
        }

        private void setBiomeAt(final int x, final int y, final int z, final ResourceKey<Biome> key) {
            if (key == null) {
                return;
            }
            final RegistryAccess access = level.registryAccess();
            final Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);
            final Holder<Biome> holder = registry.getHolder(key).orElse(null);
            if (holder == null) {
                return;
            }
            final LevelChunk chunk = level.getChunkAt(new BlockPos(x, y, z));
            final int section = Math.floorDiv(y, 16) - chunk.getMinSection();
            if (section < 0 || section >= chunk.getSections().length) {
                return;
            }
            // 1.21.1 exposes the section biome palette read-only; the live
            // container is a PalettedContainer (the modded idiom).
            @SuppressWarnings("unchecked")
            final PalettedContainer<Holder<Biome>> biomes =
                    (PalettedContainer<Holder<Biome>>) chunk.getSections()[section].getBiomes();
            biomes.set(x & 3, y & 3, z & 3, holder);
            chunk.setUnsaved(true);
        }

        private static ResourceKey<Biome> first(final Set<ResourceKey<Biome>> keys) {
            return keys.isEmpty() ? null : keys.iterator().next();
        }
    }

    /** Delegates the engine's java.util.Random surface to the level's RandomSource. */
    private static final class LevelRandomBridge extends Random {
        private final RandomSource source;

        LevelRandomBridge(final RandomSource source) {
            this.source = source;
        }

        @Override
        public int nextInt() {
            return source.nextInt();
        }

        @Override
        public int nextInt(final int bound) {
            return source.nextInt(bound);
        }

        @Override
        public boolean nextBoolean() {
            return source.nextBoolean();
        }

        @Override
        public double nextDouble() {
            return source.nextDouble();
        }
    }
}
