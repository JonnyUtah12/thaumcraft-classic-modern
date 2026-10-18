package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketAspectPool}.
 * <p>Authoritative essentia pool total for one aspect, plus the amount just added (drives the client notification).</p>
<p>short amounts widened to int (varint).</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record AspectPoolPayload(String tag, int amount, int total) implements CustomPacketPayload {

    public static final Type<AspectPoolPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "aspect_pool"));

    public static final StreamCodec<ByteBuf, AspectPoolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AspectPoolPayload::tag,
            ByteBufCodecs.VAR_INT, AspectPoolPayload::amount,
            ByteBufCodecs.VAR_INT, AspectPoolPayload::total,
            AspectPoolPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
