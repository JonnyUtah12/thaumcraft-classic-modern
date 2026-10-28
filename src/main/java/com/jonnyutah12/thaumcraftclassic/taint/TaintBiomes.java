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
 * ({@code LevelChunkSection#getBiomes()}, indexed by {@code (x&3, y&3, z&3)}),
 * so the port writes every cell of the column in every section of the chunk —
 * the exact modern equivalent of "this chunk column is the taint biome". The
 * chunk is marked unsaved so the region file persists it.</p>
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
     * Port of {@code Utils.setBiomeAt(World, x, z, BiomeGenBase)}: writes the
     * biome of the whole chunk column at (x, z), every section, every cell of
     * the column. Server-side.
     */
    public static void setBiomeAt(final Level level, final int x, final int z, final Holder<Biome> biome) {
        final ChunkAccess chunk = level.getChunk(BlockPos.containing(x, 0, z));
        for (final LevelChunkSection section : chunk.getSections()) {
            @SuppressWarnings("unchecked")
            final PalettedContainer<Holder<Biome>> biomes =
                    (PalettedContainer<Holder<Biome>>) section.getBiomes();
            // The container index is section-local (x&3, y&3, z&3); a column is
            // the 4 cells of one (x&3, z&3) pair in every section.
            for (int wy = 0; wy < 4; wy++) {
                biomes.set(x & 3, wy, z & 3, biome);
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
