package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBeamPulse}.
 * <p>Colored beam pulse between two entities.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBeamPulsePayload(int sourceEntityId, int targetEntityId, int color) implements CustomPacketPayload {

    public static final Type<FXBeamPulsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_beam_pulse"));

    public static final StreamCodec<ByteBuf, FXBeamPulsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FXBeamPulsePayload::sourceEntityId,
            ByteBufCodecs.VAR_INT, FXBeamPulsePayload::targetEntityId,
            ByteBufCodecs.INT, FXBeamPulsePayload::color,
            FXBeamPulsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
