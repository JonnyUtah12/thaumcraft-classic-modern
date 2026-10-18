package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketSyncWarp}.
 * <p>Authoritative value for one warp counter kind (see WARP_* constants).</p>
<p>byte kind widened to int (varint).</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record SyncWarpPayload(int value, int warpKind) implements CustomPacketPayload {

    public static final Type<SyncWarpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "sync_warp"));

    public static final StreamCodec<ByteBuf, SyncWarpPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncWarpPayload::value,
            ByteBufCodecs.VAR_INT, SyncWarpPayload::warpKind,
            SyncWarpPayload::new);
    /** Warp counter kinds, matching the original PacketSyncWarp type byte. */
    public static final int WARP_PERMANENT = 0;
    public static final int WARP_STICKY = 1;
    public static final int WARP_TEMPORARY = 2;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
