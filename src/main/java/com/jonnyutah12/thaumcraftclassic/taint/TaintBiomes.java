package com.jonnyutah12.thaumcraftclassic.taint;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.BiomeChangePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * TC-M5-04-s: the Tainted Land biome (content-mapping.csv row
 * {@code Thaumcraft:biome.BiomeGenTaint}, original {@code BiomeGenTaint}) and
 * the runtime biome-edit primitive the original exposed as
 * {@code Utils.setBiomeAt}.
 *
 * <p>In 1.7.10 a biome was one byte per chunk column, so
 * {@code Utils.setBiomeAt} wrote a byte. In 1.21.1 biomes live in a
 * {@code PalettedContainer<Holder<Biome>>} per 4×4×4 section
 * ({@code LevelChunkSection#getBiomes()}, indexed in quart space:
 * the cell of a block coordinate is {@code (coord >> 2) & 3}, which is how
 * {@code ChunkAccess#getNoiseBiome} reads it), so the port writes every
 * cell of the column in every section of the chunk — the exact modern
 * equivalent of "this chunk column is the taint biome". A biome cell is 4
 * blocks wide, so the write is 4-block granular: it taints the 4×4-block
 * footprint that contains the column (BD-15 in docs/architecture/taint.md).
 * The chunk is marked unsaved so the region file persists it.</p>
 *
 * <p>Client sync: the original broadcast {@code PacketBiomeChange}; the port's
 * 1:1 payload {@link BiomeChangePayload} is registered (TC-M7-05) and its
 * client handler applies the same palette write through
 * {@link #applyBiomeChange}. {@link #setBiomeAtAndSync} does both halves.</p>
 *
 * <p>The original's biome-id config ({@code Biomes/biome_taint}, default 192)
 * has no 1.21.1 counterpart — biomes are registry objects keyed by
 * {@code thaumcraft:biome.biomegentaint} (D-001 id contract, the CSV
 * {@code new_id} column). Natural placement of the biome at worldgen
 * ({@code Biomes/taint_biome_weight}, {@code World_Generation/generate_taint},
 * the {@code decorateSpecial} blob/fibre/big-tree passes) is TC-M5-05's biome
 * modifier hook, not this card: here the biome only spreads from taint blocks
 * and from the flux-goo seam, exactly as the spread rules do.</p>
 */
public final class TaintBiomes {

    /** Datapack biome id (the content-mapping.csv {@code new_id} contract). */
    public static final ResourceKey<Biome> TAINT_BIOME =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "biome.biomegentaint"));

    /**
     * The Taint Poison effect (content-mapping.csv row {@code potion.fluxtaint},
     * original {@code Config.potionTaintPoisonID}). TC-M6-05 registers it;
     * every taint consumer looks the id up at call time and no-ops while the
     * effect does not exist yet.
     */
    public static final ResourceLocation TAINT_POISON_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "potion.fluxtaint");

    /** Display name key of the biome (the original's {@code biome.Tainted Land} label). */
    public static final String BIOME_NAME_KEY = "biome." + ThaumcraftClassic.MOD_ID + ".biome.biomegentaint";

    /**
     * TC-M5-04-s-b: the biomes the Ethereal Bloom and the flux→taint restore
     * path are allowed to rewrite. The original lists exactly three ids in
     * {@code TileEtherealBloom.update} — {@code Config.biomeTaintID},
     * {@code Config.biomeEerieID}, {@code Config.biomeMagicalForestID} — so the
     * set is those three registry keys under the D-001 id contract
     * ({@code content-mapping.csv} {@code new_id} column, rows
     * {@code biome.BiomeGenTaint}, {@code biome.BiomeGenEerie},
     * {@code biome.BiomeGenMagicalForest}). Eerie and the Magical Forest are
     * other cards' rows and are not registered yet; the predicate simply never
     * matches a biome that does not exist, so the gate is the original's the
     * moment those cards land.
     */
    public static final java.util.Set<ResourceKey<Biome>> CLEANSABLE_BIOMES = java.util.Set.of(
            TAINT_BIOME,
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "biome.biomegeneerie")),
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "biome.biomegenmagicalforest")));

    /**
     * Chunk NBT key holding the biome a column had <em>before</em> taint
     * overwrote it (see {@link #generatedBiomeAt}).
     */
    public static final String BIOME_RECORD_KEY = "thaumcraft:taint_biome_record";

    private TaintBiomes() {
    }

    /**
     * The biome the world is actually storing at a position, read straight from
     * the chunk's biome palette.
     *
     * <p>1.7.10 reads the biome through {@code world.getBiomeGenForCoords(x, z)},
     * which reads the chunk's biome array, so a biome a taint tick has just
     * written is visible to the very next rule that asks. 1.21.1's
     * {@code Level#getBiome(BlockPos)} goes through {@code BiomeManager}, which
     * caches one biome per section cell for up to 600 game ticks, so that path
     * keeps reporting the <em>generated</em> biome for ~30 seconds after a
     * {@link #setBiomeAt} write — a spread gate, a decay gate and the cleansing
     * check would all read stale state on a freshly tainted column. Every taint
     * rule reads through this method instead.
     */
    public static Holder<Biome> biomeAt(final LevelReader level, final BlockPos pos) {
        return level.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ()));
    }

    public static boolean isTaintBiome(final Holder<Biome> biome) {
        return biome.is(TAINT_BIOME);
    }

    /**
     * TC-M5-04-s-b: is this biome one the cleansing path may rewrite? Reads the
     * registry key, so an unregistered biome (eerie, magical forest) can never
     * match — the gate is inert until those rows land, and exact once they do.
     */
    public static boolean isCleansableBiome(final Holder<Biome> biome) {
        for (final ResourceKey<Biome> key : CLEANSABLE_BIOMES) {
            if (biome.is(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * TC-M5-04-s-b: the biome a column would hold if taint had never touched
     * it — the modern stand-in for the original's
     * {@code WorldChunkManager#getBiomesToGenerateAt} (the chunk PRIMER's
     * generated biome array, which 1.7.10 kept separate from the editable
     * per-column byte the taint write overwrote).
     *
     * <p>1.21.1 has ONE biome palette per section: the biome the generator put
     * there and the biome a taint tick wrote there are the same stored value,
     * so the generated answer is not readable after a conversion. The port
     * therefore records the biome a column had at the moment taint overwrites
     * it (a chunk data attachment, persisted with the chunk) and answers from
     * that record. An untouched column has no record and answers from the
     * palette, which is its generated biome. Both cases give the original's
     * observable result: the biome that was there before the taint. See BD-16
     * in docs/architecture/taint.md.</p>
     */
    public static Holder<Biome> generatedBiomeAt(final LevelReader level, final int x, final int y,
                                                 final int z) {
        final Holder<Biome> recorded = readRecord(level, x, z, false);
        if (recorded != null) {
            return recorded;
        }
        return biomeAt(level, BlockPos.containing(x, y, z));
    }

    /**
     * TC-M5-04-s-b: the recorded pre-taint biome of a column, consuming the
     * record (a restore uses it up, so a second restore on the same column
     * falls back to the palette / plains rule). Returns null when the column
     * was never converted by taint.
     */
    public static Holder<Biome> takeGeneratedBiome(final LevelReader level, final int x, final int y,
                                                   final int z) {
        final Holder<Biome> recorded = readRecord(level, x, z, true);
        return recorded != null ? recorded : biomeAt(level, BlockPos.containing(x, y, z));
    }

    /** The chunk-local key of the 4×4 biome footprint containing a column. */
    private static String cellKey(final int x, final int z) {
        return ((x >> 2) & 3) + "_" + ((z >> 2) & 3);
    }

    private static Holder<Biome> readRecord(final LevelReader level, final int x, final int z,
                                            final boolean consume) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        final ChunkAccess chunk = serverLevel.getChunk(BlockPos.containing(x, 0, z));
        final java.util.Map<String, String> record = chunk.getExistingDataOrNull(
                com.jonnyutah12.thaumcraftclassic.player.ModAttachments.BIOME_RECORD.get());
        if (record == null || record.isEmpty()) {
            return null;
        }
        final String key = cellKey(x, z);
        final String id = record.get(key);
        if (id == null) {
            return null;
        }
        if (consume) {
            // A restore uses the cell up, so a second cleanse of the same
            // footprint falls back to the palette / plains rule.
            record.remove(key);
            chunk.setData(com.jonnyutah12.thaumcraftclassic.player.ModAttachments.BIOME_RECORD.get(), record);
        }
        final var registry = serverLevel.registryAccess().registryOrThrow(Registries.BIOME);
        final Biome biome = registry.get(ResourceLocation.parse(id));
        return biome == null ? null : registry.wrapAsHolder(biome);
    }

    /**
     * TC-M5-04-s-b: remember what a column's biome was, just before taint
     * overwrites it, so the cleansing path can put it back. Only the FIRST
     * conversion of a footprint records (a re-conversion of an already-tainted
     * column must not overwrite the original with taint); a column that was
     * already a cleansable biome keeps no record, which routes the restore
     * through the original's plains fallback.
     */
    private static void recordBeforeConversion(final ServerLevel level, final int x, final int z) {
        final ChunkAccess chunk = level.getChunk(BlockPos.containing(x, 0, z));
        final var type = com.jonnyutah12.thaumcraftclassic.player.ModAttachments.BIOME_RECORD.get();
        final java.util.Map<String, String> record = chunk.getExistingDataOrNull(type);
        final String key = cellKey(x, z);
        if (record != null && record.containsKey(key)) {
            return;
        }
        final Holder<Biome> previous = biomeAt(level,
                BlockPos.containing(x, level.getMinBuildHeight() + 1, z));
        if (isCleansableBiome(previous)) {
            return;
        }
        final ResourceLocation id = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(previous.value());
        if (id == null) {
            return;
        }
        final java.util.Map<String, String> updated = record == null
                ? new java.util.HashMap<>() : new java.util.HashMap<>(record);
        updated.put(key, id.toString());
        chunk.setData(type, updated);
    }

    /**
     * Port of {@code Utils.setBiomeAt(World, x, z, BiomeGenBase)}: writes the
     * biome of the whole chunk column at (x, z), every section, every cell of
     * the column. Server-side.
     */
    public static void setBiomeAt(final Level level, final int x, final int z, final Holder<Biome> biome) {
        final ChunkAccess chunk = level.getChunk(BlockPos.containing(x, 0, z));
        // The palette cell of a column, in the container's own 4x4x4 index
        // space. A cell is 4 blocks wide, so the cell of block x is
        // QuartPos.fromBlock(x) & 3 — exactly the index ChunkAccess.getNoiseBiome
        // reads with (it takes quart coordinates and does `sections[j]
        // .getNoiseBiome(x & 3, l & 3, z & 3)`). `x & 3` alone is the offset
        // inside the cell, not the cell: it lands in the right column only when
        // the block coordinate happens to agree with the cell coordinate
        // mod 4, so a 1:1 transcription of the 1.7.10 byte write is off by up to
        // three cells for every other column.
        final int cellX = (x >> 2) & 3;
        final int cellZ = (z >> 2) & 3;
        for (final LevelChunkSection section : chunk.getSections()) {
            @SuppressWarnings("unchecked")
            final PalettedContainer<Holder<Biome>> biomes =
                    (PalettedContainer<Holder<Biome>>) section.getBiomes();
            // A section is 16 blocks tall = 4 cells tall, so the column is the
            // 4 cells of one (cellX, cellZ) pair in every section.
            for (int wy = 0; wy < 4; wy++) {
                biomes.set(cellX, wy, cellZ, biome);
            }
        }
        if (chunk instanceof LevelChunk levelChunk) {
            levelChunk.setUnsaved(true);
        }
    }

    /**
     * Server-side biome write plus the original's {@code PacketBiomeChange}
     * half: sends {@link BiomeChangePayload} to the players tracking the
     * column (the original broadcast to nearby players; 96 blocks covers the
     * view distance at which biome fog/sky tint matters).
     */
    public static void setBiomeAtAndSync(final ServerLevel level, final int x, final int z,
                                         final Holder<Biome> biome) {
        // TC-M5-04-s-b: a write of a cleansable (taint-side) biome is a
        // conversion — remember what the column had first so the bloom /
        // restore path can put it back.
        if (isCleansableBiome(biome)) {
            recordBeforeConversion(level, x, z);
        }
        setBiomeAt(level, x, z, biome);
        final ResourceLocation id = level.registryAccess()
                .registryOrThrow(Registries.BIOME).getKey(biome.value());
        if (id == null) {
            return;
        }
        final BlockPos center = BlockPos.containing(x, level.getMinBuildHeight() + 1, z);
        for (final ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(center, 96.0D)) {
                player.connection.send(new BiomeChangePayload(x, z, id));
            }
        }
    }

    /** The registry holder of the taint biome, resolved against the level's registries. */
    public static Holder<Biome> taintBiomeHolder(final ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(TAINT_BIOME);
    }

    /**
     * Client half of {@link BiomeChangePayload} (wired from
     * {@code ClientEffectHooks.biomeChange}): applies the palette write in the
     * client level so biome fog/sky/grass tint update at runtime.
     */
    public static void applyBiomeChange(final Level level, final int x, final int z,
                                        final ResourceLocation biomeId) {
        final var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        final Biome biome = registry.get(biomeId);
        if (biome == null) {
            return;
        }
        setBiomeAt(level, x, z, registry.wrapAsHolder(biome));
    }
}
