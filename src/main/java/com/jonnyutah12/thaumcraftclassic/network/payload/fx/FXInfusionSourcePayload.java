package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXInfusionSource}.
 * <p>Infusion source particles from a block face.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXInfusionSourcePayload(BlockPos pos, BlockPos offset, int color) implements CustomPacketPayload {

    public static final Type<FXInfusionSourcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_infusion_source"));

    public static final StreamCodec<ByteBuf, FXInfusionSourcePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXInfusionSourcePayload::pos,
            BlockPos.STREAM_CODEC, FXInfusionSourcePayload::offset,
            ByteBufCodecs.INT, FXInfusionSourcePayload::color,
            FXInfusionSourcePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
