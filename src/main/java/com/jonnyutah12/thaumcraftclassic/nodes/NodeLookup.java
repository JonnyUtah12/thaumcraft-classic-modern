package com.jonnyutah12.thaumcraftclassic.nodes;

import com.jonnyutah12.thaumcraftclassic.world.WorldMagicData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Reads vis node records out of {@link WorldMagicData} by position (TC-M5-01).
 * This is the query hook the Thaumometer / Thaumic Infusion visuals (TC-M7)
 * and the node behaviour milestone (TC-M5-02) consume — the modern stand-in for
 * the original's {@code Utils.getSurroundingVis} / {@code TileVisNode} scans,
 * which walked loaded chunk tile entities.
 */
public final class NodeLookup {

    private NodeLookup() {
    }

    /** The node record exactly at this block position, if any. */
    public static Optional<WorldMagicData.Node> findNodeAt(final ServerLevel level, final BlockPos pos) {
        for (final WorldMagicData.Node node : WorldMagicData.get(level).getNodes(new ChunkPos(pos))) {
            if (node.x() == pos.getX() && node.y() == pos.getY() && node.z() == pos.getZ()) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /** All node records in this chunk. */
    public static List<WorldMagicData.Node> nodesInChunk(final ServerLevel level, final ChunkPos pos) {
        return WorldMagicData.get(level).getNodes(pos);
    }

    /**
     * All node records within {@code radius} blocks (Chebyshev) of the centre —
     * the scan shape {@code Utils.getSurroundingVis} used.
     */
    public static List<WorldMagicData.Node> nodesNear(final ServerLevel level, final BlockPos center, final int radius) {
        final List<WorldMagicData.Node> out = new ArrayList<>();
        final ChunkPos origin = new ChunkPos(center);
        final int chunkRadius = (radius >> 4) + 1;
        for (int cx = origin.x - chunkRadius; cx <= origin.x + chunkRadius; cx++) {
            for (int cz = origin.z - chunkRadius; cz <= origin.z + chunkRadius; cz++) {
                for (final WorldMagicData.Node node : WorldMagicData.get(level).getNodes(new ChunkPos(cx, cz))) {
                    if (Math.max(Math.abs(node.x() - center.getX()), Math.abs(node.z() - center.getZ())) <= radius) {
                        out.add(node);
                    }
                }
            }
        }
        return out;
    }
}
