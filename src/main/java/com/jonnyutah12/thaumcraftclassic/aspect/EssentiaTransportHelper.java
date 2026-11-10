package com.jonnyutah12.thaumcraftclassic.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * Port of {@code ThaumcraftApiHelper.getConnectableTile} (TC-M4-06). The
 * original takes the querying tile's own coordinates plus the face it is
 * reaching toward, offsets by that face, and accepts the neighbour only if it
 * {@code isConnectable} back toward the caller ({@code face.getOpposite()}).
 * The original's null return is kept exactly.
 */
public final class EssentiaTransportHelper {

    private EssentiaTransportHelper() {
    }

    public static IEssentiaTransport getConnectableTile(final BlockGetter level, final BlockPos origin,
            final Direction face) {
        final Object be = level.getBlockEntity(origin.relative(face));
        if (be instanceof IEssentiaTransport iet && iet.isConnectable(face.getOpposite())) {
            return iet;
        }
        return null;
    }
}
