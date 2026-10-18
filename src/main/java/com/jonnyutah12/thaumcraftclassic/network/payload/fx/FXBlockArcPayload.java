package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBlockArc}.
 * <p>Arcane arc particles at a block, sourced from an entity.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBlockArcPayload(BlockPos pos, int sourceEntityId) implements CustomPacketPayload {

    public static final Type<FXBlockArcPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_block_arc"));

    public static final StreamCodec<ByteBuf, FXBlockArcPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FXBlockArcPayload::pos,
            ByteBufCodecs.VAR_INT, FXBlockArcPayload::sourceEntityId,
            FXBlockArcPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
