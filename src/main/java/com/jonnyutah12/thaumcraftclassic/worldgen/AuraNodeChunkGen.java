package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.world.MagicChunkHooks;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Drives vis node generation from the chunk pipeline (TC-M5-01). Replaces the
 * original's {@code ThaumcraftWorldGenerator.generateSurface}/{@code
 * generateNether} entry points (worldgen IWorldGenerator), which map onto the
 * TC-M2-02 {@link MagicChunkHooks} deferred-chunk pipeline: one pass per chunk,
 * tracked by the {@code magicGenDone} marker (the modern regenKey equivalent).
 *
 * <p>Dimension gates are transcribed from the original: the End gets nothing,
 * the Nether gets wild nodes only (its totem check belongs to the structure
 * milestone), Outer Lands dimensions get their maze only (registered by
 * TC-M6-04f), and blacklisted dimensions skip the surface path. The
 * {@code World_Generation.generate_aura_nodes} option gates everything, as
 * {@code Config.genAura} did.</p>
 *
 * <p>Per-chunk aura values: the biome aura potential (BiomeHandler table) is
 * recorded as the chunk's vis density with purity 100 (pristine baseline;
 * flux/taint lower it via later milestones). The original had no per-chunk
 * ambient vis store — TC4 vis lived only in nodes — so the aspect map stays
 * empty at generation; TC-M5-03 (flux) writes ambient aspects.</p>
 *
 * <p>Deviations from the original, both deliberate: the per-chunk RNG is seeded
 * from the world seed + chunk coords (the original used the unsalted
 * {@code world.rand}, making node placement load-order dependent), and the
 * {@code World_Regeneration.*} options have no effect here because the modern
 * pipeline generates each chunk exactly once (docs/architecture/worldgen.md
 * note 7).</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class AuraNodeChunkGen {

    /** Baseline purity of freshly generated aura (100 = pristine; flux/taint reduce it). */
    public static final int PRISTINE_PURITY = 100;

    static {
        // Class-loaded by the @EventBusSubscriber scan at mod construction —
        // the right moment to join the chunk pipeline.
        MagicChunkHooks.registerChunkGenHook(AuraNodeChunkGen::generateChunk);
    }

    private AuraNodeChunkGen() {
    }

    /**
     * TC4 parity: EventHandlerWorld/ThaumcraftWorldGenerator ran magic gen from
     * a server-tick queue. This is the driver {@link MagicChunkHooks} was
     * designed for (M2-02 left it to M5).
     */
    @SubscribeEvent
    static void onServerTickPost(final ServerTickEvent.Post event) {
        for (final ServerLevel level : event.getServer().getAllLevels()) {
            MagicChunkHooks.runPendingChunkGen(level);
        }
    }

    /** One magic-generation pass for a chunk (public for the game test). */
    public static void generateChunk(final ServerLevel level, final ChunkPos pos, final WorldMagicData data) {
        final ResourceKey<Level> dimension = level.dimension();
        if (dimension.equals(Level.END)) {
            return; // original: End gets no TC features
        }
        if (NodeGeneration.isOuterLandsDimension(dimension)) {
            return; // Outer Lands gets the maze only (TC-M6-04f)
        }
        if (!TcConfig.getBool(com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog.GENERATE_AURA_NODES)) {
            return; // Config.genAura gate
        }

        final int nodeRarity = TcConfig.getInt(com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog.NODE_RARITY);
        final Random random = new Random(level.getSeed() ^ pos.asLong(pos.x, pos.z));

        if (dimension.equals(Level.NETHER)) {
            // Original generateNether: wild nodes only (totems belong to structures).
            NodeGeneration.generateWildNodes(level, pos, random, nodeRarity, false);
            recordAura(level, pos, data);
            return;
        }

        // Original generateSurface: the aura block is skipped for blacklisted
        // dimensions (its "blacklist == 0 || == 2" gate).
        if (NodeGeneration.isDimBlacklisted(dimension)) {
            return;
        }
        NodeGeneration.generateWildNodes(level, pos, random, nodeRarity, false);
        recordAura(level, pos, data);
    }

    /** Record the chunk's biome aura potential + pristine purity (TC-M4-02 AmbientVis contract). */
    private static void recordAura(final ServerLevel level, final ChunkPos pos, final WorldMagicData data) {
        final Holder<Biome> biome = level.getBiome(pos.getMiddleBlockPosition(0));
        data.setAura(pos, BiomeAura.getBiomeAura(biome), PRISTINE_PURITY);
    }
}
