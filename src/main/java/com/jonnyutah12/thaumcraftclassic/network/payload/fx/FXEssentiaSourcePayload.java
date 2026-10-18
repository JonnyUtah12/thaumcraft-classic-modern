package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXEssentiaSource}.
 * <p>Essentia source particles from a block face.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXEssentiaSourcePayload(BlockPos pos, BlockPos offset, int color) implements CustomPacketPayload {

    public static final Type<FXEssentiaSourcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_essentia_source"));

    public static final StreamCodec<ByteBuf, FXEssentiaSourcePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXEssentiaSourcePayload::pos,
            BlockPos.STREAM_CODEC, FXEssentiaSourcePayload::offset,
            ByteBufCodecs.INT, FXEssentiaSourcePayload::color,
            FXEssentiaSourcePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
