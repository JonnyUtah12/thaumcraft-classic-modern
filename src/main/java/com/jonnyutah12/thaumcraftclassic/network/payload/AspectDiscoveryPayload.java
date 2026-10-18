package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketAspectDiscovery}.
 * <p>The player just discovered a new aspect.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record AspectDiscoveryPayload(String tag) implements CustomPacketPayload {

    public static final Type<AspectDiscoveryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "aspect_discovery"));

    public static final StreamCodec<ByteBuf, AspectDiscoveryPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8
                    .map(AspectDiscoveryPayload::new, AspectDiscoveryPayload::tag);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
