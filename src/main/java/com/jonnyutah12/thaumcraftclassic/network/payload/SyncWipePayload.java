package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketSyncWipe}.
 * <p>Empty marker: the client must discard all cached player knowledge before the authoritative sync payloads that follow it.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record SyncWipePayload() implements CustomPacketPayload {

    public static final Type<SyncWipePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "sync_wipe"));

    public static final StreamCodec<ByteBuf, SyncWipePayload> STREAM_CODEC =
            StreamCodec.unit(new SyncWipePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
