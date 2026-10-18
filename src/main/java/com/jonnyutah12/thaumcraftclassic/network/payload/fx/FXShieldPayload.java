package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXShield}.
 * <p>Shield beam between two entities.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXShieldPayload(int sourceEntityId, int targetEntityId) implements CustomPacketPayload {

    public static final Type<FXShieldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_shield"));

    public static final StreamCodec<ByteBuf, FXShieldPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FXShieldPayload::sourceEntityId,
            ByteBufCodecs.VAR_INT, FXShieldPayload::targetEntityId,
            FXShieldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
