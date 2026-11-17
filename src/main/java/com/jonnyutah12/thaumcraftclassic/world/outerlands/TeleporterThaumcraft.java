package com.jonnyutah12.thaumcraftclassic.world.outerlands;

import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogOuterLands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The eldritch teleporter placement (the original
 * {@code thaumcraft.common.lib.world.dim.TeleporterThaumcraft}).
 *
 * <p>1.7.10: {@code transferPlayerToDimension(player, dim, new
 * TeleporterThaumcraft(server.getWorldServer(dim)))} and the teleporter's
 * {@code placeInPortal} searched a 128-cell box around the player (full
 * height, top-down) for the nearest {@code blockEldritchPortal}, cached the
 * hit per {@code "chunkX:chunkZ:dim"} string hash with a 600-tick expiry
 * (pruned every 100 ticks), and placed the entity at
 * {@code (x + 0.5 ± 1, y, z + 0.5 ± 1)} with zeroed velocity; its
 * {@code makePortal} returned true without placing anything, so on a miss the
 * player simply stayed at their own coordinates in the new dimension.</p>
 *
 * <p>1.21.1 has no {@code transferPlayerToDimension}: travel goes through
 * {@code ServerPlayer.changeDimension(DimensionTransition)} with an explicit
 * position. The teleporter's search/cache/placement math is ported verbatim
 * here and answers that position; the "make no portal" behaviour is the
 * {@code Optional.empty()} fallback (the caller keeps the player's
 * coordinates), and the 600-tick cache prune runs on the tile's 100-tick
 * clock, same cadence as the original's {@code updateDestinationCoCache}.</p>
 */
public final class TeleporterThaumcraft {

    /** The original's search half-width in cells. */
    public static final int SEARCH_RADIUS = 128;
    /** The original's destination-cache expiry. */
    public static final long CACHE_TTL_TICKS = 600;
    /** The original's prune cadence. */
    public static final long CACHE_PRUNE_PERIOD = 100;

    private record Cached(BlockPos pos, long updatedGameTime) {
    }

    /** The original's static destinationCoordinateCache + key list. */
    private static final Map<Long, Cached> DESTINATION_CACHE = new ConcurrentHashMap<>();
    private static final List<Long> DESTINATION_KEYS = new ArrayList<>();

    private TeleporterThaumcraft() {
    }

    /**
     * The original's placeInPortal search: nearest {@code blockportaleldritch}
     * within ±128 in x/z, top-down over the full height, closest by squared
     * distance to the entity; cache keyed by the original's
     * {@code (chunkX + ":" + chunkZ + ":" + dimId).hashCode()} — the dim part
     * resolves through the level's own key hash (1.21.1 has no int dim ids).
     *
     * @return the placement position: portal centre + a random ±1 on each of
     *         x and z (the original's {@code nextBoolean() ? 1 : -1}), with
     *         zero velocity applied by the caller's transition
     */
    public static Optional<Vec3> findNearestPortal(final ServerLevel target, final Entity entity) {
        prune(target.getGameTime());
        final int lx = net.minecraft.util.Mth.floor(entity.getX());
        final int lz = net.minecraft.util.Mth.floor(entity.getZ());
        final long key = cacheKey(lx >> 4, lz >> 4, target);

        BlockPos hit = null;
        final Cached cached = DESTINATION_CACHE.get(key);
        if (cached != null) {
            hit = cached.pos();
            DESTINATION_CACHE.put(key, new Cached(hit, target.getGameTime()));
        } else {
            double bestDistance = -1.0;
            final var portal = CatalogOuterLands.BLOCK_ELDRITCH_PORTAL.get();
            for (int x = lx - SEARCH_RADIUS; x <= lx + SEARCH_RADIUS; x++) {
                for (int z = lz - SEARCH_RADIUS; z <= lz + SEARCH_RADIUS; z++) {
                    for (int y = target.getMaxBuildHeight() - 1; y >= target.getMinBuildHeight(); y--) {
                        if (target.getBlockState(new BlockPos(x, y, z)).is(portal)) {
                            final double dx = x + 0.5 - entity.getX();
                            final double dy = y + 0.5 - entity.getY();
                            final double dz = z + 0.5 - entity.getZ();
                            final double distance = dx * dx + dy * dy + dz * dz;
                            if (bestDistance < 0.0 || distance < bestDistance) {
                                bestDistance = distance;
                                hit = new BlockPos(x, y, z);
                            }
                        }
                    }
                }
            }
            if (hit != null) {
                DESTINATION_CACHE.put(key, new Cached(hit, target.getGameTime()));
                DESTINATION_KEYS.add(key);
            }
        }
        if (hit == null) {
            return Optional.empty();
        }
        final double px = hit.getX() + 0.5 + (target.getRandom().nextBoolean() ? 1 : -1);
        final double pz = hit.getZ() + 0.5 + (target.getRandom().nextBoolean() ? 1 : -1);
        return Optional.of(new Vec3(px, hit.getY(), pz));
    }

    /** The original's updateDestinationCoCache: prune stale entries every 100 ticks. */
    public static void prune(final long gameTime) {
        if (gameTime % CACHE_PRUNE_PERIOD != 0) {
            return;
        }
        final long cutoff = gameTime - CACHE_TTL_TICKS;
        final Iterator<Long> keys = DESTINATION_KEYS.iterator();
        while (keys.hasNext()) {
            final long key = keys.next();
            final Cached cached = DESTINATION_CACHE.get(key);
            if (cached == null || cached.updatedGameTime() < cutoff) {
                keys.remove();
                DESTINATION_CACHE.remove(key);
            }
        }
    }

    /** Test seam: the cache bookkeeping. */
    public static int cachedDestinations() {
        return DESTINATION_CACHE.size();
    }

    /** Test seam: empty the destination cache. */
    public static void clearCache() {
        DESTINATION_CACHE.clear();
        DESTINATION_KEYS.clear();
    }

    private static long cacheKey(final int chunkX, final int chunkZ, final ServerLevel level) {
        return (chunkX + ":" + chunkZ + ":" + level.dimension().location().hashCode()).hashCode();
    }
}
