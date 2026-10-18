package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXSonic}.
 * <p>Sonic boom effect at an entity.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXSonicPayload(int sourceEntityId) implements CustomPacketPayload {

    public static final Type<FXSonicPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_sonic"));

    public static final StreamCodec<ByteBuf, FXSonicPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT
                    .map(FXSonicPayload::new, FXSonicPayload::sourceEntityId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
