package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketAspectPlaceToServer}.
 * <p>The client placed an aspect onto the research table grid at (q, r).</p>
<p>The original wrote the literal string "null" for an absent aspect; this uses an optional field. The sending player comes from the payload context.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record AspectPlaceToServerPayload(BlockPos tablePos, Optional<String> aspectTag, int q, int r) implements CustomPacketPayload {

    public static final Type<AspectPlaceToServerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "aspect_place_to_server"));

    public static final StreamCodec<ByteBuf, AspectPlaceToServerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AspectPlaceToServerPayload::tablePos,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), AspectPlaceToServerPayload::aspectTag,
            ByteBufCodecs.VAR_INT, AspectPlaceToServerPayload::q,
            ByteBufCodecs.VAR_INT, AspectPlaceToServerPayload::r,
            AspectPlaceToServerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
