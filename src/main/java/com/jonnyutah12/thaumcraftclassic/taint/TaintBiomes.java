package com.jonnyutah12.thaumcraftclassic.taint;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.payload.BiomeChangePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * TC-M5-04: the Tainted Land biome (content-mapping.csv row
 * {@code biome.biomegentaint}, original {@code BiomeGenTaint}) and the
 * runtime biome-edit primitive the original exposed as
 * {@code Utils.setBiomeAt}.
 *
 * <p>In 1.7.10 biomes were a per-column byte array on the chunk, so
 * {@code setBiomeAt} wrote one byte. In 1.21.1 biomes live in a
 * {@code PalettedContainer<Holder<Biome>>} per 4x4x4 section
 * ({@code LevelChunkSection#getBiomes()}); the port writes every cell of the
 * column in every section of the chunk, which is the exact modern equivalent
 * of "set the biome of this chunk column".</p>
 *
 * <p>Client sync: the original sent {@code PacketBiomeChange}; the port's
 * 1:1 payload {@link BiomeChangePayload} already exists (TC-M7-05) and its
 * client handler applies the same palette write via
 * {@link #applyBiomeChange}. {@link #setBiomeAtAndSync} does both halves.</p>
 *
 * <p>The original biome id config ({@code Biomes/biome_taint}, default 192)
 * has no 1.21.1 counterpart: biomes are registry objects keyed by
 * {@code thaumcraft:biome.biomegentaint}; natural generation placement
 * ({@code taint_biome_weight}, {@code generate_taint}) is TC-M5-05's biome
 * modifier hook (docs/architecture/taint.md).</p>
 */
public final class TaintBiomes {

    /** Datapack biome id (content-mapping.csv new_id contract). */
    public static final ResourceKey<Biome> TAINT_BIOME =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "biome.biomegentaint"));

    /**
     * The Taint Poison effect (content-mapping.csv row {@code potion.fluxtaint},
     * original {@code Config.potionTaintPoisonID}). Registered by TC-M6-05;
     * every taint consumer looks this id up at call time and no-ops while the
     * effect does not exist yet.
     */
    public static final ResourceLocation TAINT_POISON_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "potion.fluxtaint");

    private TaintBiomes() {
    }

    public static boolean isTaintBiome(final Holder<Biome> biome) {
        return biome.is(TAINT_BIOME);
    }

    /**
     * Port of {@code Utils.setBiomeAt(World, x, z, BiomeGenBase)}: writes the
     * biome for the whole chunk column at (x, z), every section, every cell.
     * Server-side; marks the chunk dirty so the region file persists it.
     */
    public static void setBiomeAt(final Level level, final int x, final int z, final Holder<Biome> biome) {
        final ChunkAccess chunk = level.getChunk(BlockPos.containing(x, 0, z));
        for (final LevelChunkSection section : chunk.getSections()) {
            @SuppressWarnings("unchecked")
            final PalettedContainer<Holder<Biome>> biomes =
                    (PalettedContainer<Holder<Biome>>) section.getBiomes();
            // 16 section rows of 4; the container index is (x&3, y&3, z&3).
            for (int row = 0; row < 4; row++) {
                for (int wy = 0; wy < 4; wy++) {
                    biomes.set(x & 3, wy, z & 3, biome);
                }
            }
        }
        if (chunk instanceof LevelChunk levelChunk) {
            levelChunk.setUnsaved(true);
        }
    }

    /**
     * Server-side biome write plus the original {@code PacketBiomeChange}
     * half: sends {@link BiomeChangePayload} to players tracking the column
     * (the original broadcast to nearby players; 96 blocks covers the view
     * distance at which biome fog matters).
     */
    public static void setBiomeAtAndSync(final ServerLevel level, final int x, final int z, final Holder<Biome> biome) {
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

    /**
     * Client half of {@link BiomeChangePayload} (wired from
     * {@code ClientEffectHooks.biomeChange}): applies the palette write in the
     * client level so biome fog/sky/grass tint update at runtime.
     */
    public static void applyBiomeChange(final Level level, final int x, final int z, final ResourceLocation biomeId) {
        final var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        final Biome biome = registry.get(biomeId);
        if (biome == null) {
            return;
        }
        setBiomeAt(level, x, z, registry.wrapAsHolder(biome));
    }
}
