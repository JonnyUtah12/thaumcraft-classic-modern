package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXWispZap}.
 * <p>Wisp zap beam between two entities.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXWispZapPayload(int sourceEntityId, int targetEntityId) implements CustomPacketPayload {

    public static final Type<FXWispZapPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_wisp_zap"));

    public static final StreamCodec<ByteBuf, FXWispZapPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FXWispZapPayload::sourceEntityId,
            ByteBufCodecs.VAR_INT, FXWispZapPayload::targetEntityId,
            FXWispZapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
