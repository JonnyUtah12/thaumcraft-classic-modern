package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * S2C payload — the ward hexagram (TC-M7-02-c-s-f4).
 *
 * <p>The original has no packet for this FX: {@code BlockWarded.addHitEffects}
 * (BlockWarded.java:64-70) and {@code BlockCosmeticOpaque.addHitEffects}
 * (BlockCosmeticOpaque.java:68-82) call {@code ClientProxy.blockWard(world, x,
 * y, z, side, f, f1, f2)} (ClientProxy.java:980-984) inside a
 * {@code Dist.CLIENT} branch, so the hit's block, face and in-block offset are
 * already on the client that draws them.</p>
 *
 * <p>1.21.1 moved the same hook to the client — the block's
 * {@code IClientBlockExtensions.addHitEffects} — and a ward block is placed
 * server-side, so a warded-glass hit reaches the drawing client only as data.
 * This payload carries exactly what {@code blockWard} takes: the block, the hit
 * face, and the {@code (hitVec - pos)} triple that becomes the mote's jitter.
 * It is the ward block's own S2C half, not a lane seam: the sender is
 * {@code TcWardHitEffects} on the client and this is the route a ward block
 * landed on the server uses to reach it.
 * {@code docs/architecture/networking.md#s2c-fx} carries the lane's
 * per-payload disposition.</p>
 */
public record FXWardBlockPayload(BlockPos pos, Direction side, Vec3 hit) implements CustomPacketPayload {

    public static final Type<FXWardBlockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_ward_block"));

    /** The hit face as its 3D data value, the same encoding the FX payloads use. */
    private static final StreamCodec<ByteBuf, Direction> SIDE =
            ByteBufCodecs.VAR_INT.map(
                    side -> ((Direction) side).get3DDataValue(),
                    Direction::from3DDataValue);

    /**
     * The hit point inside the block as three doubles: this mapping has no
     * {@code Vec3.STREAM_CODEC}, and {@code blockWard} takes the triple as
     * {@code f, f1, f2} (ClientProxy.java:980-984) — so the wire carries the
     * triple, and the record reassembles the point the plan jitters from.
     */
    public static final StreamCodec<ByteBuf, FXWardBlockPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXWardBlockPayload::pos,
            SIDE, FXWardBlockPayload::side,
            ByteBufCodecs.DOUBLE, p -> p.hit().x,
            ByteBufCodecs.DOUBLE, p -> p.hit().y,
            ByteBufCodecs.DOUBLE, p -> p.hit().z,
            (pos, side, x, y, z) -> new FXWardBlockPayload(pos, side, new Vec3(x, y, z)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
