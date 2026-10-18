package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketBoreDig}.
 * <p>An arcane bore advanced its dig; the client plays the dig event on the tile.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record BoreDigPayload(BlockPos pos, int digLocation) implements CustomPacketPayload {

    public static final Type<BoreDigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "bore_dig"));

    public static final StreamCodec<ByteBuf, BoreDigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BoreDigPayload::pos,
            ByteBufCodecs.VAR_INT, BoreDigPayload::digLocation,
            BoreDigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
