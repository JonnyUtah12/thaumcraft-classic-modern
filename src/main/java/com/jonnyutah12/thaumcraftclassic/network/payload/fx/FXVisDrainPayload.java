package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXVisDrain}.
 * <p>Vis drain particles at a block.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXVisDrainPayload(BlockPos pos, BlockPos offset, int color) implements CustomPacketPayload {

    public static final Type<FXVisDrainPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_vis_drain"));

    public static final StreamCodec<ByteBuf, FXVisDrainPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXVisDrainPayload::pos,
            BlockPos.STREAM_CODEC, FXVisDrainPayload::offset,
            ByteBufCodecs.INT, FXVisDrainPayload::color,
            FXVisDrainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
