package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.network.payload.fx.FXEssentiaSourcePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Essentia source lookup for consumers that cannot reach a jar directly
 * (TC-M4-04: the runic matrix drains through this).
 *
 * <p>Original: {@code thaumcraft.common.lib.events.EssentiaHandler}. Same
 * behaviour: per-consumer source lists cached in world, a failed drain
 * blacklists the consumer for 5000 ms, and a successful drain pushes the
 * source-beam FX. The original's directional scan is only ever used with
 * {@code ForgeDirection.UNKNOWN} by the matrix, so the port implements the
 * omnidirectional cube scan (range 12) and nothing else.</p>
 */
public final class EssentiaHandler {

    private static final long DELAY_MS = 5000L;

    private record Key(BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension) {
    }

    private static final Map<Key, List<BlockPos>> SOURCES = new HashMap<>();
    private static final Map<Key, Long> SOURCE_DELAY = new HashMap<>();

    /** Original {@code drainEssentia(tile, aspect, UNKNOWN, range)}. */
    public static boolean drainEssentia(final BlockEntity tile, final Aspect aspect, final int range) {
        final Level level = tile.getLevel();
        final Key key = new Key(tile.getBlockPos(), level.dimension());
        List<BlockPos> list = SOURCES.get(key);
        if (list == null) {
            getSources(level, tile.getBlockPos(), range, key);
            list = SOURCES.get(key);
            if (list == null) {
                return false;
            }
            return drainEssentia(tile, aspect, range);
        }
        for (final BlockPos source : list) {
            final BlockEntity sourceTile = level.getBlockEntity(source);
            if (!(sourceTile instanceof IAspectSource as)) {
                break;
            }
            if (as.useAspect(aspect, 1) >= 1) {
                final BlockPos offset = source.subtract(tile.getBlockPos());
                PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) level,
                        new ChunkPos(tile.getBlockPos()),
                        new FXEssentiaSourcePayload(tile.getBlockPos(), offset, aspect.getColor()));
                return true;
            }
        }
        SOURCES.remove(key);
        SOURCE_DELAY.put(key, System.currentTimeMillis() + DELAY_MS);
        return false;
    }

    /** Original {@code findEssentia}: same scan, no consumption. */
    public static boolean findEssentia(final BlockEntity tile, final Aspect aspect, final int range) {
        final Level level = tile.getLevel();
        final Key key = new Key(tile.getBlockPos(), level.dimension());
        List<BlockPos> list = SOURCES.get(key);
        if (list == null) {
            getSources(level, tile.getBlockPos(), range, key);
            list = SOURCES.get(key);
            if (list == null) {
                return false;
            }
            return findEssentia(tile, aspect, range);
        }
        for (final BlockPos source : list) {
            final BlockEntity sourceTile = level.getBlockEntity(source);
            if (!(sourceTile instanceof IAspectSource)) {
                break;
            }
            if (((IAspectSource) sourceTile).doesContain(aspect)) {
                return true;
            }
        }
        SOURCES.remove(key);
        SOURCE_DELAY.put(key, System.currentTimeMillis() + DELAY_MS);
        return false;
    }

    /** Drop a consumer's cached scan (world unload / test teardown). */
    public static void clearCache() {
        SOURCES.clear();
        SOURCE_DELAY.clear();
    }

    private static void getSources(final Level level, final BlockPos centre, final int range, final Key key) {
        final Long delay = SOURCE_DELAY.get(key);
        if (delay != null) {
            if (delay > System.currentTimeMillis()) {
                return;
            }
            SOURCE_DELAY.remove(key);
        }
        final List<BlockPos> found = new ArrayList<>();
        for (int xx = -range; xx <= range; xx++) {
            for (int yy = -range; yy <= range; yy++) {
                for (int zz = -range; zz <= range; zz++) {
                    if (xx == 0 && yy == 0 && zz == 0) {
                        continue;
                    }
                    final BlockPos pos = centre.offset(xx, yy, zz);
                    if (level.getBlockEntity(pos) instanceof IAspectSource) {
                        found.add(pos);
                    }
                }
            }
        }
        if (!found.isEmpty()) {
            SOURCES.put(key, found);
        }
    }

    private EssentiaHandler() {
    }
}
