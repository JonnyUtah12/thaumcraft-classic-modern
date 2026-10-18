package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketRunicCharge}.
 * <p>Runic charging state for a player (used by the client runic table HUD).</p>
<p>short amounts widened to int (varint).</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record RunicChargePayload(int playerId, int charge, int maxCharge) implements CustomPacketPayload {

    public static final Type<RunicChargePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "runic_charge"));

    public static final StreamCodec<ByteBuf, RunicChargePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RunicChargePayload::playerId,
            ByteBufCodecs.VAR_INT, RunicChargePayload::charge,
            ByteBufCodecs.VAR_INT, RunicChargePayload::maxCharge,
            RunicChargePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
