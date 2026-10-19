package com.jonnyutah12.thaumcraftclassic.world;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-level magic store: the TC-M2-02 persistence substrate for aura/node/flux/taint
 * state (design record: docs/architecture/persistence.md).
 *
 * <p>Attached to each {@link ServerLevel} through its {@code DimensionDataStorage}
 * under the fixed key {@link #NAME} (file {@code <level>/data/thaumcraft_world_magic.dat}),
 * and versioned via the {@code version} field so the schema can evolve without ambiguity
 * (TC-M5-01/02/03/04 will extend semantics behind this same key).</p>
 *
 * <p>Parity note: Thaumcraft 4.2.3.5 has no single world-magic class — nodes are tile
 * entities, flux/taint are blocks, and the only per-chunk persisted marker is the
 * {@code Thaumcraft}/{@code regenKey} compound written by {@code EventHandlerWorld}'s
 * {@code ChunkDataEvent} handlers. This store is the modern equivalent of that marker
 * plus the per-chunk value maps the milestone plan needs; the original list-of-compounds
 * NBT style ({@code {x, z, ...}} entries, {@code {key, amount}} aspect entries) is kept
 * deliberately, matching MazeHandler/ResearchManager serialization.</p>
 *
 * <p>Reading is lenient by contract: unknown keys are ignored, missing fields take their
 * defaults, and a newer {@code version} is parsed best-effort with a warning. Every
 * mutation marks the store dirty so vanilla saves it on the normal level-save pass.</p>
 */
public final class WorldMagicData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** DimensionDataStorage key; the on-disk file is {@code <name>.dat}. */
    public static final String NAME = "thaumcraft_world_magic";
    /** Schema version written into the NBT. Bump when the layout changes. */
    public static final int CURRENT_VERSION = 1;

    public static final SavedData.Factory<WorldMagicData> FACTORY =
            new SavedData.Factory<>(WorldMagicData::new, WorldMagicData::load);

    /** Fetch (or lazily create) this level's magic store. Server side only. */
    public static WorldMagicData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    // --- NBT keys (stable wire format; never rename without a version bump) -----
    static final String TAG_VERSION = "version";
    static final String TAG_VALUES = "values";
    static final String TAG_CHUNKS = "chunks";
    static final String TAG_X = "x";
    static final String TAG_Y = "y";
    static final String TAG_Z = "z";
    static final String TAG_MAGIC_GEN_DONE = "magicGenDone";
    static final String TAG_AURA_DENSITY = "auraDensity";
    static final String TAG_AURA_PURITY = "auraPurity";
    static final String TAG_AURA_ASPECTS = "auraAspects";
    static final String TAG_FLUX_AIR = "fluxAir";
    static final String TAG_FLUX_GROUND = "fluxGround";
    static final String TAG_TAINT = "taint";
    static final String TAG_NODES = "nodes";
    static final String TAG_TYPE = "type";
    static final String TAG_INTENSITY = "intensity";
    static final String TAG_ASPECTS = "aspects";
    static final String TAG_KEY = "key";
    static final String TAG_AMOUNT = "amount";

    private final int version;
    private final Map<ChunkPos, ChunkMagic> chunks = new HashMap<>();
    private final Map<String, Integer> worldValues = new HashMap<>();

    public WorldMagicData() {
        this.version = CURRENT_VERSION;
    }

    private WorldMagicData(final int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    // ------------------------------------------------------------------ loading

    /**
     * Lenient decoder: unknown keys are ignored, missing fields default, and a newer
     * version than {@link #CURRENT_VERSION} is still parsed best-effort.
     */
    public static WorldMagicData load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final int fileVersion = tag.contains(TAG_VERSION, Tag.TAG_ANY_NUMERIC) ? tag.getInt(TAG_VERSION) : CURRENT_VERSION;
        if (fileVersion > CURRENT_VERSION) {
            LOGGER.warn("[Thaumcraft] {} was written by a newer schema (version {} > {}); parsing known fields only.",
                    NAME, fileVersion, CURRENT_VERSION);
        }
        final WorldMagicData data = new WorldMagicData(fileVersion);

        if (tag.contains(TAG_VALUES, Tag.TAG_COMPOUND)) {
            final CompoundTag values = tag.getCompound(TAG_VALUES);
            for (String key : values.getAllKeys()) {
                if (values.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    data.worldValues.put(key, values.getInt(key));
                }
            }
        }

        if (tag.contains(TAG_CHUNKS, Tag.TAG_LIST)) {
            final ListTag chunkList = tag.getList(TAG_CHUNKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < chunkList.size(); i++) {
                final CompoundTag entry = chunkList.getCompound(i);
                // Chunk coordinates are the identity of an entry; everything else is optional.
                if (!entry.contains(TAG_X, Tag.TAG_ANY_NUMERIC) || !entry.contains(TAG_Z, Tag.TAG_ANY_NUMERIC)) {
                    continue;
                }
                final ChunkPos pos = new ChunkPos(entry.getInt(TAG_X), entry.getInt(TAG_Z));
                final ChunkMagic magic = data.chunks.computeIfAbsent(pos, p -> new ChunkMagic());
                magic.magicGenDone = entry.getBoolean(TAG_MAGIC_GEN_DONE);
                magic.auraDensity = entry.getInt(TAG_AURA_DENSITY);
                magic.auraPurity = entry.getInt(TAG_AURA_PURITY);
                magic.auraAspects.putAll(readAspects(entry, TAG_AURA_ASPECTS));
                magic.fluxAir = entry.getInt(TAG_FLUX_AIR);
                magic.fluxGround = entry.getInt(TAG_FLUX_GROUND);
                magic.taint = entry.getInt(TAG_TAINT);
                if (entry.contains(TAG_NODES, Tag.TAG_LIST)) {
                    final ListTag nodes = entry.getList(TAG_NODES, Tag.TAG_COMPOUND);
                    for (int n = 0; n < nodes.size(); n++) {
                        final CompoundTag node = nodes.getCompound(n);
                        if (!node.contains(TAG_X, Tag.TAG_ANY_NUMERIC)
                                || !node.contains(TAG_Y, Tag.TAG_ANY_NUMERIC)
                                || !node.contains(TAG_Z, Tag.TAG_ANY_NUMERIC)) {
                            continue;
                        }
                        final String type = node.contains(TAG_TYPE, Tag.TAG_STRING) ? node.getString(TAG_TYPE) : Node.DEFAULT_TYPE;
                        magic.nodes.add(new Node(node.getInt(TAG_X), node.getInt(TAG_Y), node.getInt(TAG_Z),
                                type, node.getInt(TAG_INTENSITY), readAspects(node, TAG_ASPECTS)));
                    }
                }
            }
        }
        return data;
    }

    private static Map<String, Integer> readAspects(final CompoundTag parent, final String key) {
        final Map<String, Integer> aspects = new LinkedHashMap<>();
        if (parent.contains(key, Tag.TAG_LIST)) {
            final ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final CompoundTag entry = list.getCompound(i);
                if (entry.contains(TAG_KEY, Tag.TAG_STRING) && entry.contains(TAG_AMOUNT, Tag.TAG_ANY_NUMERIC)) {
                    aspects.put(entry.getString(TAG_KEY), entry.getInt(TAG_AMOUNT));
                }
            }
        }
        return aspects;
    }

    // ------------------------------------------------------------------ writing

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putInt(TAG_VERSION, version);

        if (!worldValues.isEmpty()) {
            final CompoundTag values = new CompoundTag();
            worldValues.forEach(values::putInt);
            tag.put(TAG_VALUES, values);
        }

        if (!chunks.isEmpty()) {
            final ListTag chunkList = new ListTag();
            // Deterministic order keeps diffs and tests stable.
            final List<Map.Entry<ChunkPos, ChunkMagic>> ordered = new ArrayList<>(chunks.entrySet());
            ordered.sort((a, b) -> {
                final int byZ = Integer.compare(a.getKey().z, b.getKey().z);
                return byZ != 0 ? byZ : Integer.compare(a.getKey().x, b.getKey().x);
            });
            for (final Map.Entry<ChunkPos, ChunkMagic> entry : ordered) {
                final ChunkPos pos = entry.getKey();
                final ChunkMagic magic = entry.getValue();
                final CompoundTag chunk = new CompoundTag();
                chunk.putInt(TAG_X, pos.x);
                chunk.putInt(TAG_Z, pos.z);
                chunk.putBoolean(TAG_MAGIC_GEN_DONE, magic.magicGenDone);
                if (magic.auraDensity != 0) {
                    chunk.putInt(TAG_AURA_DENSITY, magic.auraDensity);
                }
                if (magic.auraPurity != 0) {
                    chunk.putInt(TAG_AURA_PURITY, magic.auraPurity);
                }
                if (!magic.auraAspects.isEmpty()) {
                    chunk.put(TAG_AURA_ASPECTS, writeAspects(magic.auraAspects));
                }
                if (magic.fluxAir != 0) {
                    chunk.putInt(TAG_FLUX_AIR, magic.fluxAir);
                }
                if (magic.fluxGround != 0) {
                    chunk.putInt(TAG_FLUX_GROUND, magic.fluxGround);
                }
                if (magic.taint != 0) {
                    chunk.putInt(TAG_TAINT, magic.taint);
                }
                if (!magic.nodes.isEmpty()) {
                    final ListTag nodes = new ListTag();
                    for (final Node node : magic.nodes) {
                        final CompoundTag nodeTag = new CompoundTag();
                        nodeTag.putInt(TAG_X, node.x());
                        nodeTag.putInt(TAG_Y, node.y());
                        nodeTag.putInt(TAG_Z, node.z());
                        nodeTag.putString(TAG_TYPE, node.type());
                        nodeTag.putInt(TAG_INTENSITY, node.intensity());
                        if (!node.aspects().isEmpty()) {
                            nodeTag.put(TAG_ASPECTS, writeAspects(node.aspects()));
                        }
                        nodes.add(nodeTag);
                    }
                    chunk.put(TAG_NODES, nodes);
                }
                chunkList.add(chunk);
            }
            tag.put(TAG_CHUNKS, chunkList);
        }
        return tag;
    }

    private static ListTag writeAspects(final Map<String, Integer> aspects) {
        final ListTag list = new ListTag();
        for (final Map.Entry<String, Integer> entry : aspects.entrySet()) {
            final CompoundTag aspect = new CompoundTag();
            aspect.putString(TAG_KEY, entry.getKey());
            aspect.putInt(TAG_AMOUNT, entry.getValue());
            list.add(aspect);
        }
        return list;
    }

    // ---------------------------------------------------------------- accessors

    /** World-level scalar (aura caps etc. defined by M5). Missing keys read as 0. */
    public int getWorldValue(final String key) {
        return worldValues.getOrDefault(key, 0);
    }

    public void setWorldValue(final String key, final int value) {
        worldValues.put(key, value);
        setDirty();
    }

    /** Existing record for a chunk, or {@code null} when the chunk has no magic state yet. */
    public ChunkMagic getChunk(final ChunkPos pos) {
        return chunks.get(pos);
    }

    /** Existing record or a fresh zeroed one (marked for save). */
    public ChunkMagic mutableChunk(final ChunkPos pos) {
        final ChunkMagic magic = chunks.computeIfAbsent(pos, p -> {
            setDirty();
            return new ChunkMagic();
        });
        return magic;
    }

    public boolean isMagicGenDone(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic != null && magic.magicGenDone;
    }

    /** Records that magic world generation has run for this chunk (TC4 regenKey parity). */
    public void setMagicGenDone(final ChunkPos pos, final boolean done) {
        mutableChunk(pos).magicGenDone = done;
        setDirty();
    }

    public int getAuraDensity(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? 0 : magic.auraDensity;
    }

    public int getAuraPurity(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? 0 : magic.auraPurity;
    }

    public void setAura(final ChunkPos pos, final int density, final int purity) {
        final ChunkMagic magic = mutableChunk(pos);
        magic.auraDensity = density;
        magic.auraPurity = purity;
        setDirty();
    }

    public Map<String, Integer> getAuraAspects(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? Map.of() : Collections.unmodifiableMap(magic.auraAspects);
    }

    public void setAuraAspect(final ChunkPos pos, final String aspectTag, final int amount) {
        final ChunkMagic magic = mutableChunk(pos);
        if (amount == 0) {
            magic.auraAspects.remove(aspectTag);
        } else {
            magic.auraAspects.put(aspectTag, amount);
        }
        setDirty();
    }

    public int getFluxAir(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? 0 : magic.fluxAir;
    }

    public int getFluxGround(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? 0 : magic.fluxGround;
    }

    public void setFlux(final ChunkPos pos, final int air, final int ground) {
        final ChunkMagic magic = mutableChunk(pos);
        magic.fluxAir = air;
        magic.fluxGround = ground;
        setDirty();
    }

    public int getTaint(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? 0 : magic.taint;
    }

    public void setTaint(final ChunkPos pos, final int taint) {
        mutableChunk(pos).taint = taint;
        setDirty();
    }

    public List<Node> getNodes(final ChunkPos pos) {
        final ChunkMagic magic = chunks.get(pos);
        return magic == null ? List.of() : List.copyOf(magic.nodes);
    }

    public void addNode(final ChunkPos pos, final Node node) {
        mutableChunk(pos).nodes.add(node);
        setDirty();
    }

    public boolean removeNode(final ChunkPos pos, final int x, final int y, final int z) {
        final ChunkMagic magic = chunks.get(pos);
        if (magic == null) {
            return false;
        }
        final boolean removed = magic.nodes.removeIf(n -> n.x() == x && n.y() == y && n.z() == z);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** A vis node record. Positions are block coordinates within the level. */
    public record Node(int x, int y, int z, String type, int intensity, Map<String, Integer> aspects) {
        public static final String DEFAULT_TYPE = "normal";

        public Node {
            type = type == null ? DEFAULT_TYPE : type;
            aspects = aspects == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
        }

        public Node(final int x, final int y, final int z, final String type, final int intensity) {
            this(x, y, z, type, intensity, Map.of());
        }
    }

    /** Mutable per-chunk magic record; only reachable through WorldMagicData mutators. */
    public static final class ChunkMagic {
        private boolean magicGenDone;
        private int auraDensity;
        private int auraPurity;
        private final Map<String, Integer> auraAspects = new LinkedHashMap<>();
        private int fluxAir;
        private int fluxGround;
        private int taint;
        private final List<Node> nodes = new ArrayList<>();

        public boolean isMagicGenDone() {
            return magicGenDone;
        }

        public int getAuraDensity() {
            return auraDensity;
        }

        public int getAuraPurity() {
            return auraPurity;
        }

        public Map<String, Integer> getAuraAspects() {
            return Collections.unmodifiableMap(auraAspects);
        }

        public int getFluxAir() {
            return fluxAir;
        }

        public int getFluxGround() {
            return fluxGround;
        }

        public int getTaint() {
            return taint;
        }

        public List<Node> getNodes() {
            return List.copyOf(nodes);
        }
    }
}
