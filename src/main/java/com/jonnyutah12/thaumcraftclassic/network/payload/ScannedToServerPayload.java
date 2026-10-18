package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketScannedToServer}.
 * <p>The client scanned something with the Thaumaturgical Registry; the server validates and completes the scan.</p>
<p>Numeric 1.7 block/item ids become ResourceLocations; the sending player is taken from the payload context instead of an explicit id+dimension pair.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record ScannedToServerPayload(int scanType, ResourceLocation scannedId, int data, int targetEntityId, String phenomena, String prefix) implements CustomPacketPayload {

    public static final Type<ScannedToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "scanned_to_server"));

    public static final StreamCodec<ByteBuf, ScannedToServerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScannedToServerPayload::scanType,
            ResourceLocation.STREAM_CODEC, ScannedToServerPayload::scannedId,
            ByteBufCodecs.VAR_INT, ScannedToServerPayload::data,
            ByteBufCodecs.VAR_INT, ScannedToServerPayload::targetEntityId,
            ByteBufCodecs.STRING_UTF8, ScannedToServerPayload::phenomena,
            ByteBufCodecs.STRING_UTF8, ScannedToServerPayload::prefix,
            ScannedToServerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
