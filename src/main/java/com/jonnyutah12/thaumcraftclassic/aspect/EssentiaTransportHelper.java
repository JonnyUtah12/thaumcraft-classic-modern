package com.jonnyutah12.thaumcraftclassic.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * The original {@code ThaumcraftApiHelper.getConnectableTile} (TC-M4-07): the
 * one rule every piece of the essentia transport network uses to decide
 * whether a neighbour is reachable from a face.
 *
 * <p>Original, verbatim:</p>
 * <pre>
 * TileEntity te = world.getTileEntity(x + face.offsetX, y + face.offsetY, z + face.offsetZ);
 * return te instanceof IEssentiaTransport &amp;&amp; ((IEssentiaTransport) te).isConnectable(face.getOpposite())
 *         ? te : null;
 * </pre>
 *
 * <p>The interesting half is the {@code face.getOpposite()} argument: the
 * neighbour is asked whether <em>it</em> is connectable on the face pointing
 * back at us, so two tubes only link when both of them have that side open
 * (the tube network's whole routing rule). Reproduced with {@link Direction}
 * (D-001: only the platform type moves).</p>
 */
public final class EssentiaTransportHelper {

    private EssentiaTransportHelper() {
    }

    /**
     * The tile adjacent to {@code pos} across {@code face}, when it is an
     * {@link IEssentiaTransport} connectable on the side that faces back.
     *
     * @return the neighbouring tile, or {@code null} when there is none, it is
     *         not an essentia transport, or it is not connectable that way.
     */
    @Nullable
    public static BlockEntity getConnectableTile(final Level level, final BlockPos pos, final Direction face) {
        final BlockEntity te = level.getBlockEntity(pos.relative(face));
        if (te instanceof IEssentiaTransport transport && transport.isConnectable(face.getOpposite())) {
            return te;
        }
        return null;
    }

    /**
     * The tile at {@code neighbour} when it is connectable on
     * {@code asSeenFromIt}. Same contract as the original's second
     * {@code getConnectableTile} overload (the {@code IBlockAccess} one the
     * client renderer used), exposed so the shape/outline code can ask without
     * a {@code Level}.
     */
    @Nullable
    public static BlockEntity getConnectableAt(final Level level, final BlockPos neighbour,
                                               final Direction asSeenFromIt) {
        final BlockEntity te = level.getBlockEntity(neighbour);
        if (te instanceof IEssentiaTransport transport && transport.isConnectable(asSeenFromIt)) {
            return te;
        }
        return null;
    }
}
