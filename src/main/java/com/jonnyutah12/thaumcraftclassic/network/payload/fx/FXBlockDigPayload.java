package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBlockDig}.
 * <p>Arcane bore digging particles on a face of a target block.</p>
<p>Numeric 1.7 block/item ids become a ResourceLocation.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBlockDigPayload(BlockPos targetPos, ResourceLocation blockOrItemId, int data, BlockPos digFromPos) implements CustomPacketPayload {

    public static final Type<FXBlockDigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_block_dig"));

    public static final StreamCodec<ByteBuf, FXBlockDigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXBlockDigPayload::targetPos,
            ResourceLocation.STREAM_CODEC, FXBlockDigPayload::blockOrItemId,
            ByteBufCodecs.VAR_INT, FXBlockDigPayload::data,
            BlockPos.STREAM_CODEC, FXBlockDigPayload::digFromPos,
            FXBlockDigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
