package com.jonnyutah12.thaumcraftclassic.aspect;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.IAspectSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds and drains essentia for consumers that cannot hold a container
 * themselves — the runic matrix (TC-M4-04) is its only caller today; the
 * machines that pump essentia (TC-M4-07) and the alchemical furnace GUI
 * (TC-M7-04) come later.
 *
 * <p>Original: {@code thaumcraft.common.lib.events.EssentiaHandler}, which
 * keeps a per-consumer cache of {@code IAspectSource} tile positions and
 * drains through it. The scan geometry is ported unchanged: with
 * {@code ForgeDirection.UNKNOWN} the original widens the vertical axis to
 * {@code -range..range-1} while sweeping x and z over {@code -range..range},
 * skipping the consumer's own position; that asymmetry (the last layer at
 * {@code y = +range} is never visited) is preserved so the port and the
 * original agree on which sources are in reach.</p>
 *
 * <p>Two deviations, both behavioural no-ops: the original's
 * {@code sourcesDelay} (a 5 s wall-clock cooldown before a consumer re-scans
 * after its source list runs out) is not carried — the port simply re-scans
 * when the cached list is exhausted, so a source the player places late is
 * still found on the next drain; and {@code PacketFXEssentiaSource} is not
 * sent here, the client-side essentia streams being TC-M7-01's.</p>
 */
public final class EssentiaHandler {

    /** Per-consumer cache of in-reach {@link IAspectSource} positions. */
    private static final Map<BlockPos, List<BlockPos>> sources = new HashMap<>();

    /** Sources for {@code centre} within {@code range}, cached until exhausted. */
    public static void getSources(final Level level, final BlockPos centre, final int range) {
        final List<BlockPos> found = new ArrayList<>();
        for (int aa = -range; aa <= range; aa++) {
            for (int bb = -range; bb <= range; bb++) {
                for (int cc = -range; cc < range; cc++) {
                    if (aa == 0 && bb == 0 && cc == 0) {
                        continue;
                    }
                    final BlockEntity entity = level.getBlockEntity(
                            centre.offset(aa, cc, bb));
                    if (entity instanceof IAspectSource) {
                        found.add(centre.offset(aa, cc, bb));
                    }
                }
            }
        }
        if (!found.isEmpty()) {
            sources.put(centre, found);
        }
    }

    /** True when a source in reach holds one unit of the aspect. */
    public static boolean findEssentia(final Level level, final BlockPos centre,
                                      final Aspect aspect, final int range) {
        final List<BlockPos> cached = sources.get(centre);
        if (cached == null) {
            getSources(level, centre, range);
            return sources.containsKey(centre)
                    && findEssentia(level, centre, aspect, range);
        }
        for (final BlockPos pos : cached) {
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null || !(entity instanceof IAspectSource source)) {
                break; // the cached list has gone stale (original breaks here)
            }
            if (source.doesContain(aspect)) {
                return true;
            }
        }
        sources.remove(centre);
        return false;
    }

    /** Take one unit of the aspect from the first reachable source. */
    public static boolean drainEssentia(final Level level, final BlockPos centre,
                                       final Aspect aspect, final int range) {
        final List<BlockPos> cached = sources.get(centre);
        if (cached == null) {
            getSources(level, centre, range);
            return sources.containsKey(centre)
                    && drainEssentia(level, centre, aspect, range);
        }
        for (final BlockPos pos : cached) {
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null || !(entity instanceof IAspectSource source)) {
                break;
            }
            if (source.useAspect(aspect, 1) > 0) {
                return true;
            }
        }
        sources.remove(centre);
        return false;
    }

    /** Drop a consumer's cached source list (original refreshSources). */
    public static void refreshSources(final BlockPos centre) {
        sources.remove(centre);
    }

    /** Wipe every cache — the world changed or a test is starting. */
    public static void clearAllSources() {
        sources.clear();
    }

    private EssentiaHandler() {
    }
}
