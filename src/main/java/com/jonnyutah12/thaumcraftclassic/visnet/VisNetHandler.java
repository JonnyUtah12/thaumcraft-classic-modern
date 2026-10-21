package com.jonnyutah12.thaumcraftclassic.visnet;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Vis network graph (TC-M4-02): the port of {@code thaumcraft.api.visnet.
 * VisNetHandler} ({@code api/visnet/VisNetHandler.java}). The network is a
 * forest per dimension: vis node sources are roots, conduits (relays,
 * energized nodes) and devices (wand pedestal, etc.) attach to the closest
 * eligible member within their range, and vis consumption walks parent edges
 * up to the owning source — the node→conduit→device container model
 * (design record: docs/architecture/vis.md).
 *
 * <p>The selection algorithm is preserved term-for-term, including the
 * original's quirks, each cited to the decompiled line:
 * <ul>
 *   <li>root candidates are keyed {@code r - range*2} while descendant
 *       candidates use the raw squared distance (VisNetHandler.java:66-72) —
 *       mixed units, and it makes roots preferred inside a tree;</li>
 *   <li>range checks compare squared distance against {@code range²}
 *       ({@code inRange}, lines 128-131);</li>
 *   <li>attunement -1 matches anything (lines 84-86);</li>
 *   <li>the traversal guard caps at 512 visited positions and is cleared per
 *       root ({@code cache}, lines 74-78, 133-152);</li>
 *   <li>the drainer→nearest-node cache is dropped wholesale on any topology
 *       change ({@code nearbyNodes.clear()}, lines 47, 96, and the source
 *       registration path).</li>
 * </ul></p>
 *
 * <p>World hooks: line-of-sight is pluggable via {@link #setLineOfSight}
 * (the original ray-traced ignoring source blocks, lines 176-185 — the tile
 * layer installs that); the drainer-side particle beam
 * ({@code generateVisEffect}, lines 36-38) is client visuals and is left to
 * the M7 visual pass. Validity is explicit through {@link IVisNode#isRemoved()}
 * instead of weak-reference liveness.</p>
 */
public final class VisNetHandler {

    /** Per-dimension source registry (original {@code sources}, line 12). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, IVisNode>> SOURCES = new HashMap<>();

    /** Drainer position → nearest eligible node per tree (original {@code nearbyNodes}, line 14). */
    private static final Map<BlockPos, List<IVisNode>> NEARBY_NODES = new HashMap<>();

    /** Traversal guard of {@link #findClosestNodes} (original static {@code cache}, line 13). */
    private static final List<BlockPos> SEARCH_CACHE = new ArrayList<>();

    /** Pluggable line-of-sight test; default transparent (tile layer installs the ray trace). */
    @FunctionalInterface
    public interface LineOfSight {
        boolean canSee(IVisNode from, IVisNode to);
    }

    private static LineOfSight lineOfSight = (from, to) -> true;

    public static void setLineOfSight(final LineOfSight sight) {
        lineOfSight = sight == null ? (from, to) -> true : sight;
    }

    /** Original {@code isNodeValid} (lines 53-55): live and not removed. */
    public static boolean isNodeValid(final IVisNode node) {
        return node != null && !node.isRemoved();
    }

    /** Register a vis node source (original {@code addSource}, lines 40-49). */
    public static void addSource(final ResourceKey<Level> dimension, final IVisNode node) {
        SOURCES.computeIfAbsent(dimension, d -> new HashMap<>()).put(node.getLocation(), node);
        NEARBY_NODES.clear();
    }

    /**
     * Attach a non-source node to the closest eligible network member within
     * its range (original {@code addNode}, lines 57-101). Returns the parent
     * candidate — the caller sets the parent edge (the original's only caller,
     * {@code TileVisNode} update, does exactly that) — or null when nothing
     * qualifies.
     */
    public static IVisNode addNode(final ResourceKey<Level> dimension, final IVisNode target) {
        final Map<BlockPos, IVisNode> sourceList = SOURCES.get(dimension);
        if (sourceList == null) {
            return null;
        }

        final List<Object[]> nearby = new ArrayList<>();
        for (final IVisNode root : sourceList.values()) {
            if (isNodeValid(root)) {
                final float r = inRange(target.getLocation(), root.getLocation(), target.getRange());
                if (r > 0.0F) {
                    nearby.add(new Object[]{root, r - target.getRange() * 2.0F});
                }
                findClosestNodes(target, root, nearby);
                SEARCH_CACHE.clear();
            }
        }

        float dist = Float.MAX_VALUE;
        IVisNode closest = null;
        for (final Object[] entry : nearby) {
            final IVisNode candidate = (IVisNode) entry[0];
            final float key = (Float) entry[1];
            if (key < dist
                    && (target.getAttunement() == -1 || candidate.getAttunement() == -1
                        || target.getAttunement() == candidate.getAttunement())
                    && canNodeBeSeen(target, candidate)) {
                dist = key;
                closest = candidate;
            }
        }

        if (closest != null) {
            closest.getChildren().add(target);
            NEARBY_NODES.clear();
            return closest;
        }
        return null;
    }

    /** Original {@code findClosestNodes} (lines 103-121): walk the tree under {@code parent},
     * collecting descendants within {@code target}'s range keyed by raw squared distance. */
    public static List<Object[]> findClosestNodes(final IVisNode target, final IVisNode parent,
                                                  final List<Object[]> in) {
        if (SEARCH_CACHE.size() <= 512 && !SEARCH_CACHE.contains(parent.getLocation())) {
            SEARCH_CACHE.add(parent.getLocation());
            for (final IVisNode child : parent.getChildren()) {
                if (child != null && !child.equals(target) && !child.equals(parent)) {
                    final float r2 = inRange(child.getLocation(), target.getLocation(), target.getRange());
                    if (r2 > 0.0F) {
                        in.add(new Object[]{child, r2});
                    }
                    findClosestNodes(target, child, in);
                }
            }
        }
        return in;
    }

    /**
     * Drain vis at a device position (original {@code drainVis}, lines 20-39):
     * for each tree, the nearest eligible member within its own range is
     * consulted, consumption walks up its parent chain, and draining stops
     * once the amount is satisfied. Returns the amount actually drained.
     */
    public static int drainVis(final ResourceKey<Level> dimension, final BlockPos drainer,
                               final Aspect aspect, final int amount) {
        int drainedAmount = 0;
        if (!NEARBY_NODES.containsKey(drainer)) {
            calculateNearbyNodes(dimension, drainer);
        }

        final List<IVisNode> nodes = NEARBY_NODES.get(drainer);
        if (nodes != null && !nodes.isEmpty()) {
            int remaining = amount;
            for (final IVisNode node : nodes) {
                if (isNodeValid(node)) {
                    final int a = node.consumeVis(aspect, remaining);
                    drainedAmount += a;
                    remaining -= a;
                    // Original also fired generateVisEffect (particle beam,
                    // lines 36-38) — client visuals, M7 visual pass.
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }
        return drainedAmount;
    }

    /** Remove a source from the registry (the source branch of the original
     * {@code TileVisNode.removeThisNode}, lines 41-52). */
    public static void removeSource(final ResourceKey<Level> dimension, final IVisNode node) {
        final Map<BlockPos, IVisNode> sourceList = SOURCES.get(dimension);
        if (sourceList != null) {
            sourceList.remove(node.getLocation());
        }
        NEARBY_NODES.clear();
    }

    /** Drop all networks (server teardown / tests). */
    public static void clearNetworks() {
        SOURCES.clear();
        NEARBY_NODES.clear();
        SEARCH_CACHE.clear();
    }

    /** Original {@code canNodeBeSeen} (lines 176-185): ray trace ignoring source blocks;
     * delegated to the pluggable hook. */
    public static boolean canNodeBeSeen(final IVisNode from, final IVisNode to) {
        return lineOfSight.canSee(from, to);
    }

    // ------------------------------------------------------------- internals

    /** Original {@code inRange} (lines 128-131): squared distance vs range², -1 when out. */
    private static float inRange(final BlockPos a, final BlockPos b, final int range) {
        final double distance = a.distSqr(b);
        return distance > (double) range * range ? -1.0F : (float) distance;
    }

    /** Original {@code calculateNearbyNodes} (lines 133-170): for the drainer,
     * pick per tree the member (root or descendant) closest to the drainer,
     * each judged by its own range. */
    private static void calculateNearbyNodes(final ResourceKey<Level> dimension, final BlockPos drainer) {
        final Map<BlockPos, IVisNode> sourceList = SOURCES.get(dimension);
        if (sourceList == null) {
            return;
        }
        final List<IVisNode> cn = new ArrayList<>();
        for (final IVisNode root : sourceList.values()) {
            if (isNodeValid(root)) {
                IVisNode closest = null;
                float range = Float.MAX_VALUE;
                final float r = inRange(drainer, root.getLocation(), root.getRange());
                if (r > 0.0F) {
                    range = r;
                    closest = root;
                }
                for (final IVisNode n : getAllChildren(root)) {
                    if (n != null && !n.equals(root)) {
                        final float r2 = inRange(n.getLocation(), drainer, n.getRange());
                        if (r2 > 0.0F && r2 < range) {
                            range = r2;
                            closest = n;
                        }
                    }
                }
                if (closest != null) {
                    cn.add(closest);
                }
            }
        }
        NEARBY_NODES.put(drainer, cn);
    }

    /** Original {@code getAllChildren} (lines 172-181): live, chunk-loaded descendants only. */
    private static List<IVisNode> getAllChildren(final IVisNode source) {
        final List<IVisNode> list = new ArrayList<>();
        for (final IVisNode child : source.getChildren()) {
            if (isNodeValid(child) && child.isChunkLoaded()) {
                list.add(child);
                list.addAll(getAllChildren(child));
            }
        }
        return list;
    }

    private VisNetHandler() {
    }
}
