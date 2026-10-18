package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketPlayerCompleteToServer}.
 * <p>The client asks the server to complete a research entry (or create the matching research note) for the sending player.</p>
<p>username+dimension dropped: the sender identity is the authenticated payload context player.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record PlayerCompleteToServerPayload(String key, int requestType) implements CustomPacketPayload {

    public static final Type<PlayerCompleteToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "player_complete_to_server"));

    public static final StreamCodec<ByteBuf, PlayerCompleteToServerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlayerCompleteToServerPayload::key,
            ByteBufCodecs.VAR_INT, PlayerCompleteToServerPayload::requestType,
            PlayerCompleteToServerPayload::new);
    /** Request kinds, matching the original PacketPlayerCompleteToServer type byte. */
    public static final int REQUEST_COMPLETE = 0;
    public static final int REQUEST_NOTE = 1;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
