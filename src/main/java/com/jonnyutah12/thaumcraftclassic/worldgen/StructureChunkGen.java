package com.jonnyutah12.thaumcraftclassic.worldgen;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.config.TcConfig;
import com.jonnyutah12.thaumcraftclassic.config.TcConfigCatalog;
import com.jonnyutah12.thaumcraftclassic.world.MagicChunkHooks;
import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.FlatLevelGenerator;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Random;

/**
 * Structure generation pass over the deferred chunk pipeline (TC-M5-05b):
 * golem totems, shrine mounds, eldritch stone rings and hilltop stones.
 * Replaces the original's {@code ThaumcraftWorldGenerator.generateSurface}
 * structure block and {@code generateTotem} (ThaumcraftWorldGenerator.java:
 * 445-487, 112-183).
 *
 * <p>Why this rides the chunk pipeline instead of data-driven biome features
 * (the TC-M5-05 ore machinery): the original's four structures are one
 * per-chunk sequence with shared state — a chained else-if roll (1/150 mound,
 * else 1/66 ring, else 1/40 hilltop: mutually exclusive, conditional
 * probabilities) and the totem's {@code !auraGen} suppression (skipped when
 * the chunk already produced an aura node or structure node). Independent
 * placed features with rarity filters cannot express either without changing
 * the frequencies this card must pin. The structures also create aura nodes
 * through {@link NodeGeneration}, which needs a {@link ServerLevel} and
 * writes {@link WorldMagicData} — impossible from a worldgen feature. The
 * TC-M5-01 design (docs/architecture/worldgen.md) already maps the original's
 * per-chunk entry points onto {@link MagicChunkHooks} and deferred exactly
 * this pass.</p>
 *
 * <p>Gate transcription from generateSurface: overworld only (the original's
 * {@code field_76574_g == 0}; the nether totem call site is dead code — its
 * dimension gate is literally {@code == 0 || == 1}, verified in the original
 * bytecode), dimension-blacklist {@code == -1}, {@code World_Generation.
 * generate_structures} (Config.genStructure), non-flat worldgen (the
 * original's generator-name {@code startsWith("flat")} check, here the
 * generator instance), and {@code newGen} is always true in the modern
 * once-per-chunk pipeline (the {@code World_Regeneration.*} options have no
 * effect here, as for aura nodes — worldgen.md note 7).</p>
 *
 * <p>Hook order: the totem's {@code !auraGen} check reads the nodes the node
 * pass created for this chunk, so this hook must run after
 * {@link AuraNodeChunkGen}; {@link AuraNodeChunkGen#ensureHookRegistered()}
 * pinned that order regardless of class-load order.</p>
 */
@EventBusSubscriber(modid = ThaumcraftClassic.MOD_ID)
public final class StructureChunkGen {

    static {
        // Registration order == execution order in MagicChunkHooks.HOOKS;
        // force the node pass to register first (the totem gate reads its result).
        AuraNodeChunkGen.ensureHookRegistered();
        MagicChunkHooks.registerChunkGenHook(StructureChunkGen::generateChunk);
    }

    private StructureChunkGen() {
    }

    /** One structure-generation pass for a chunk (public for the game test). */
    public static void generateChunk(final ServerLevel level, final ChunkPos pos, final WorldMagicData data) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return; // original: structures are overworld-only (nether call site dead)
        }
        if (NodeGeneration.isDimBlacklisted(level.dimension())) {
            return; // original: blacklist == -1 gate
        }
        if (!TcConfig.getBool(TcConfigCatalog.GENERATE_STRUCTURES)) {
            return; // Config.genStructure gate
        }
        if (level.getChunkSource().getGenerator() instanceof FlatLevelGenerator) {
            return; // original: generator name startsWith("flat")
        }
        final Random random = new Random(level.getSeed() ^ pos.asLong(pos.x, pos.z));
        generateStructures(level, pos, data, random);
    }

    /**
     * The original's structure block verbatim, including the chained
     * else-if roll and the totem call that follows it. Package-visible with an
     * injected random for the seeded parity test.
     */
    static void generateStructures(final ServerLevel level, final ChunkPos pos, final WorldMagicData data,
                                  final Random random) {
        final int randPosX = pos.x * 16 + random.nextInt(16);
        final int randPosZ = pos.z * 16 + random.nextInt(16);
        final int randPosY = level.getHeight(Heightmap.Types.WORLD_SURFACE, randPosX, randPosZ) - 9;

        // The original's auraGen flag at this point: the node pass (or the
        // scattered-feature check) already placed a node in this chunk.
        final boolean auraGen = !data.getNodes(pos).isEmpty();

        final boolean placed = rollStructures(random, randPosY, level.getMaxBuildHeight(),
                rand -> {
                    if (!WorldGenMoundGen.generate(level, rand, randPosX, randPosY, randPosZ)) {
                        return false;
                    }
                    rand.nextInt(200); // original computes value+400 and discards it; draw kept
                    NodeGeneration.createRandomNodeAt(level, randPosX + 9, randPosY + 8, randPosZ + 9,
                            rand, false, true, false);
                    return true;
                },
                rand -> {
                    final int ringY = randPosY + 8;
                    final int w = 11 + rand.nextInt(6) * 2;
                    final int h = 11 + rand.nextInt(6) * 2;
                    if (!WorldGenEldritchRingGen.generate(level, rand, randPosX, ringY, randPosZ, pos, w, h)) {
                        return false;
                    }
                    NodeGeneration.createRandomNodeAt(level, randPosX, ringY + 2, randPosZ,
                            rand, false, true, false);
                    // Original: new Thread(new MazeThread(chunkX, chunkZ, w, h, random.nextLong())).start()
                    StructureHooks.startMaze(pos.x, pos.z, w, h, rand.nextLong());
                    return true;
                },
                rand -> {
                    final int hilltopY = randPosY + 9;
                    if (!WorldGenHilltopStonesGen.generate(level, rand, randPosX, hilltopY, randPosZ)) {
                        return false;
                    }
                    NodeGeneration.createRandomNodeAt(level, randPosX, hilltopY + 5, randPosZ,
                            rand, false, true, false);
                    return true;
                });

        TotemGen.generate(level, pos, random, auraGen || placed);
    }

    /** One structure attempt; consumes random numbers exactly as the real generator does. */
    interface Attempt {
        boolean run(Random random);
    }

    /**
     * The original's chained else-if roll (ThaumcraftWorldGenerator.java:452-481):
     * 1/150 mound, else 1/66 ring, else 1/40 hilltop — mutually exclusive and
     * conditional: a failed mound attempt still suppresses the other two rolls
     * for this chunk. Extracted so the roll sequence is pinned by a seeded
     * unit test without a live level.
     */
    static boolean rollStructures(final Random random, final int randPosY, final int maxBuildHeight,
                                 final Attempt mound, final Attempt ring, final Attempt hilltop) {
        if (randPosY >= maxBuildHeight) { // original's world-height guard (vestigial)
            return false;
        }
        if (random.nextInt(150) == 0) {
            return mound.run(random);
        } else if (random.nextInt(66) == 0) {
            return ring.run(random);
        } else if (random.nextInt(40) == 0) {
            return hilltop.run(random);
        }
        return false;
    }
}
