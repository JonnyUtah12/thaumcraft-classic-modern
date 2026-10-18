package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketFlyToServer}.
 * <p>The client toggled hover (Icarus armor) flight.</p>
<p>playerid dropped: the sender identity is the authenticated payload context player.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FlyToServerPayload(boolean hover) implements CustomPacketPayload {

    public static final Type<FlyToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fly_to_server"));

    public static final StreamCodec<ByteBuf, FlyToServerPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL
                    .map(FlyToServerPayload::new, FlyToServerPayload::hover);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
