package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBlockSparkle}.
 * <p>Sparkle particles at a block.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBlockSparklePayload(BlockPos pos, int color) implements CustomPacketPayload {

    public static final Type<FXBlockSparklePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_block_sparkle"));

    public static final StreamCodec<ByteBuf, FXBlockSparklePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXBlockSparklePayload::pos,
            ByteBufCodecs.INT, FXBlockSparklePayload::color,
            FXBlockSparklePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
