package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.misc.PacketBiomeChange}.
 * <p>A chunk's biome changed at runtime; update the client's biome view.</p>
<p>Numeric biome id becomes a ResourceLocation.</p>
 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record BiomeChangePayload(int x, int z, ResourceLocation biome) implements CustomPacketPayload {

    public static final Type<BiomeChangePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "biome_change"));

    public static final StreamCodec<ByteBuf, BiomeChangePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BiomeChangePayload::x,
            ByteBufCodecs.VAR_INT, BiomeChangePayload::z,
            ResourceLocation.STREAM_CODEC, BiomeChangePayload::biome,
            BiomeChangePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
