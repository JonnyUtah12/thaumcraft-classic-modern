package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBeamPulseGolemBoss}.
 * <p>Beam pulse between a golem and its boss.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBeamPulseGolemBossPayload(int sourceEntityId, int targetEntityId) implements CustomPacketPayload {

    public static final Type<FXBeamPulseGolemBossPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_beam_pulse_golem_boss"));

    public static final StreamCodec<ByteBuf, FXBeamPulseGolemBossPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FXBeamPulseGolemBossPayload::sourceEntityId,
            ByteBufCodecs.VAR_INT, FXBeamPulseGolemBossPayload::targetEntityId,
            FXBeamPulseGolemBossPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
