package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBlockBubble}.
 * <p>Crucible-style bubble particles at a block.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBlockBubblePayload(BlockPos pos, int color) implements CustomPacketPayload {

    public static final Type<FXBlockBubblePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_block_bubble"));

    public static final StreamCodec<ByteBuf, FXBlockBubblePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXBlockBubblePayload::pos,
            ByteBufCodecs.INT, FXBlockBubblePayload::color,
            FXBlockBubblePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
