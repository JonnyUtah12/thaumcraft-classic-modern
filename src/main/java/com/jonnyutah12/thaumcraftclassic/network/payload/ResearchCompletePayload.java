package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — port of the original {@code thaumcraft.common.lib.network.playerdata.PacketResearchComplete}.
 * <p>The player just completed the research entry with this key.</p>

 * <p>Mapping and protocol notes: docs/architecture/networking.md.</p>
 */
public record ResearchCompletePayload(String key) implements CustomPacketPayload {

    public static final Type<ResearchCompletePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "research_complete"));

    public static final StreamCodec<ByteBuf, ResearchCompletePayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8
                    .map(ResearchCompletePayload::new, ResearchCompletePayload::key);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
