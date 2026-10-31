package com.jonnyutah12.thaumcraftclassic.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The port of the ray-tracer the original's tube configuration rides on
 * (TC-M4-07-f): it splits a block into <em>sub-hits</em> — one index per
 * clickable box — so a wand right-click knows <em>which face</em> of a tube
 * the player aimed at, and the centre.
 *
 * <p>Original: {@code thaumcraft.codechicken.lib.raytracer.RayTracer}, bundled
 * into Thaumcraft 4.2. {@link #retrace} is its
 * {@code rayTraceCuboids(List<IndexedCuboid6>)} loop (every box is clipped
 * against the eye ray and the <em>nearest</em> hit wins, strictly closer, so an
 * earlier box in the list keeps a tie), {@link #retraceBlock} is its
 * {@code retraceBlock(World, EntityPlayer, x, y, z)} (eye position,
 * {@code getLook(1.0F)} — {@link Player#getLookAngle()} in 1.21.1 — scaled by
 * the player's block reach, then the block's own
 * {@code getMovingObjectPosition}, which in the original asked
 * {@code TileTube}/{@code TileTubeBuffer.addTraceableCuboids} for the boxes).
 * The face plane-intersection of {@code rayTraceCuboid} is the standard slab
 * clip, reproduced here against {@link AABB}; the sub-hit index is all the
 * callers read, so the per-face normal the original also recorded is not
 * carried.</p>
 *
 * <p>The box <em>sets</em> are the tiles' (the original's
 * {@code TileTube.addTraceableCuboids} / {@code TileTubeBuffer}
 * equivalents), built from the figures in
 * {@link com.jonnyutah12.thaumcraftclassic.tiles.TubeTransportLogic};
 * {@link Traceable} is the seam that hands them over, which is the original's
 * {@code instanceof TileTube || instanceof TileTubeBuffer} gate expressed as
 * an interface.</p>
 */
public final class WandRayTracer {

    /** The segment-parallel tolerance: a ray parallel to a slab axis. */
    private static final double PARALLEL = 1.0E-9D;

    /** A ray-traceable box and the sub-hit index it carries (the original's {@code IndexedCuboid6.data}). */
    public record IndexedBox(int index, AABB box) {
    }

    /** The tiles that carry sub-hit geometry — the original's {@code addTraceableCuboids} owners. */
    public interface Traceable {
        /** Original {@code addTraceableCuboids(List<IndexedCuboid6>)}: append <em>world-space</em> boxes. */
        void addTraceableCuboids(List<IndexedBox> cuboids);
    }

    /**
     * Original {@code RayTracer.retraceBlock(World, EntityPlayer, x, y, z)}
     * followed by {@code BlockTube.getMovingObjectPosition}: clip the player's
     * eye ray (eye position, look vector, block-reach length) against the
     * tile's own boxes.
     *
     * @return the sub-hit index of the nearest box the ray enters, or
     *         {@code -1} when the ray misses every box — the original's
     *         {@code hit == null}, which makes the tiles do nothing
     */
    public static int retraceBlock(final Level level, final Player player, final BlockPos pos) {
        final BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof Traceable traceable)) {
            return -1;
        }
        final List<IndexedBox> boxes = new ArrayList<>();
        traceable.addTraceableCuboids(boxes);
        if (boxes.isEmpty()) {
            return -1;
        }
        final Vec3 from = player.getEyePosition(1.0F);
        final Vec3 look = player.getLookAngle();
        // Original getBlockReachDistance(player): on the server the real
        // player's own reach (4.5 survival / 5.0 creative in both versions),
        // and 5.0 for anything that is not a server player entity. In 1.21.1 the
        // player's reach is blockInteractionRange(); a non-survival/non-creative
        // context answers 0, which is the original's "not a server player" case.
        final double own = player.blockInteractionRange();
        final double reach = own > 0.0D ? own : 5.0D;
        final Vec3 to = from.add(look.x * reach, look.y * reach, look.z * reach);
        return retrace(boxes, from, to);
    }

    /**
     * Original {@code RayTracer.rayTraceCuboids(start, end, cuboids)}: the
     * nearest box wins (strictly closer than the current best, so the first
     * box in the list keeps an exact tie).
     */
    public static int retrace(final List<IndexedBox> cuboids, final Vec3 from, final Vec3 to) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (final IndexedBox box : cuboids) {
            final double distance = entryDistance(from, to, box.box());
            if (distance >= 0.0D && distance < bestDistance) {
                bestDistance = distance;
                best = box.index();
            }
        }
        return best;
    }

    /**
     * Original {@code RayTracer.rayTraceCuboid}: the squared distance from the
     * ray's start to the point where the segment first enters {@code box}
     * (the original compared {@code hitVec} minus {@code start} magnitudes
     * squared), or {@code -1} when the segment never touches it.
     */
    static double entryDistance(final Vec3 from, final Vec3 to, final AABB box) {
        double near = 0.0D;
        double far = 1.0D;
        for (final Direction.Axis axis : Direction.Axis.values()) {
            final double origin = axis.choose(from.x, from.y, from.z);
            final double delta = axis.choose(to.x, to.y, to.z) - origin;
            final double low = axis.choose(box.minX, box.minY, box.minZ);
            final double high = axis.choose(box.maxX, box.maxY, box.maxZ);
            if (Math.abs(delta) < PARALLEL) {
                // Parallel to this slab: the ray only grazes the box when the
                // origin is inside the slab on this axis (the original's
                // between(min, hit, max) inclusive test).
                if (origin < low || origin > high) {
                    return -1.0D;
                }
                continue;
            }
            double t1 = (low - origin) / delta;
            double t2 = (high - origin) / delta;
            if (t1 > t2) {
                final double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            if (t1 > near) {
                near = t1;
            }
            if (t2 < far) {
                far = t2;
            }
            if (near > far) {
                return -1.0D;
            }
        }
        return from.distanceToSqr(from.lerp(to, near));
    }

    private WandRayTracer() {
    }
}
