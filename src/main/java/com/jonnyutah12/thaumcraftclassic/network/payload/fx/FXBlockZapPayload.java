package com.jonnyutah12.thaumcraftclassic.network.payload.fx;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.network.NetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.fx.PacketFXBlockZap}.
 * <p>Small zap burst at a position with a direction.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record FXBlockZapPayload(Vec3 pos, Vec3 direction) implements CustomPacketPayload {

    public static final Type<FXBlockZapPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "fx_block_zap"));

    public static final StreamCodec<ByteBuf, FXBlockZapPayload> STREAM_CODEC = StreamCodec.composite(
            NetworkCodecs.VEC3, FXBlockZapPayload::pos,
            NetworkCodecs.VEC3, FXBlockZapPayload::direction,
            FXBlockZapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
